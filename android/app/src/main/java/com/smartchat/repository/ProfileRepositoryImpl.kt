package com.smartchat.repository

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.PublicUser
import com.smartchat.core.network.SmartChatApi
import com.smartchat.core.network.apiRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileRepositoryImpl(
    private val api: SmartChatApi,
    private val contentResolver: ContentResolver
) : ProfileRepository {

    override suspend fun loadCurrentUser(): ApiResult<PublicUser> =
        apiRequest(api::currentUser)

    override suspend fun uploadProfileImage(
        uri: Uri
    ): ApiResult<PublicUser> {
        return try {
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val fileName = getFileName(uri) ?: "profile-image.jpg"

            val bytes = contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: return ApiResult.Error(
                    message = "Could not read the selected profile picture.",
                    retryable = false
                )

            val requestBody =
                bytes.toRequestBody(mimeType.toMediaTypeOrNull())

            val part = MultipartBody.Part.createFormData(
                "file",
                fileName,
                requestBody
            )

            apiRequest {
                api.uploadProfileImage(part)
            }
        } catch (_: Exception) {
            ApiResult.Error(
                message = "Could not prepare the selected profile picture.",
                retryable = false
            )
        }
    }

    private fun getFileName(uri: Uri): String? {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) {
                return@use null
            }

            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

            if (index >= 0) {
                cursor.getString(index)
            } else {
                null
            }
        }
    }
}