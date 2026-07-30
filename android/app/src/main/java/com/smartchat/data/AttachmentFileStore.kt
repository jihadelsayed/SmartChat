package com.smartchat.data

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.smartchat.core.database.entity.PendingAttachmentEntity
import com.smartchat.core.network.ApiResult
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class AttachmentFileStore(
    private val contentResolver: ContentResolver,
    filesDirectory: File,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val attachmentDirectory = File(filesDirectory, "attachments")

    fun copyToOwnedStorage(
        contentUri: String,
        conversationKey: String,
        localMessageId: String
    ): ApiResult<PendingAttachmentEntity> {
        val uri = Uri.parse(contentUri)
        val mimeType = contentResolver.getType(uri)?.lowercase()
        if (mimeType !in SUPPORTED_MIME_TYPES) {
            return ApiResult.Error(
                "Unsupported image type. Choose JPEG, PNG, or WebP.",
                retryable = false
            )
        }
        val validatedMimeType = requireNotNull(mimeType)
        val metadata = readMetadata(uri)
        if (metadata.sizeBytes > MAX_SIZE_BYTES) {
            return ApiResult.Error("Images must be 10 MB or smaller.", retryable = false)
        }

        attachmentDirectory.mkdirs()
        val id = UUID.randomUUID().toString()
        val destination = File(attachmentDirectory, id)
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            contentResolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > MAX_SIZE_BYTES) {
                            throw AttachmentTooLargeException()
                        }
                        digest.update(buffer, 0, read)
                        output.write(buffer, 0, read)
                    }
                }
            } ?: return ApiResult.Error("The selected image is unavailable.")

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                destination.delete()
                return ApiResult.Error(
                    "The selected file is not a valid image.",
                    retryable = false
                )
            }
            ApiResult.Success(
                PendingAttachmentEntity(
                    id = id,
                    localMessageId = localMessageId,
                    conversationKey = conversationKey,
                    fileName = metadata.fileName,
                    mimeType = validatedMimeType,
                    localFilePath = destination.absolutePath,
                    sizeBytes = total,
                    contentHash = digest.digest().joinToString("") { "%02x".format(it) },
                    createdAt = clock()
                )
            )
        } catch (_: AttachmentTooLargeException) {
            destination.delete()
            ApiResult.Error("Images must be 10 MB or smaller.", retryable = false)
        } catch (_: Exception) {
            destination.delete()
            ApiResult.Error("SmartChat could not copy the selected image.")
        }
    }

    fun delete(localFilePath: String?) {
        localFilePath?.let { File(it).takeIf(File::exists)?.delete() }
    }

    private fun readMetadata(uri: Uri): FileMetadata {
        var name = uri.lastPathSegment ?: "image"
        var size = -1L
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    .takeIf { it >= 0 }
                    ?.let { name = cursor.getString(it) ?: name }
                cursor.getColumnIndex(OpenableColumns.SIZE)
                    .takeIf { it >= 0 && !cursor.isNull(it) }
                    ?.let { size = cursor.getLong(it) }
            }
        }
        return FileMetadata(name.takeLast(255), size)
    }

    private data class FileMetadata(val fileName: String, val sizeBytes: Long)
    private class AttachmentTooLargeException : Exception()

    companion object {
        const val MAX_ATTACHMENTS = 4
        const val MAX_SIZE_BYTES = 10L * 1024L * 1024L
        val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}
