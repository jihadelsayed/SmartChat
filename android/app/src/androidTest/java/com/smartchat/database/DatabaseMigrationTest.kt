package com.smartchat.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartchat.core.database.MIGRATION_2_3
import com.smartchat.core.database.MIGRATION_3_4
import com.smartchat.core.database.SmartChatDatabase
import com.smartchat.core.database.entity.MessageDeliveryState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SmartChatDatabase::class.java
    )

    @Test
    fun migrationTwoToThreePreservesMessagesAndMapsDeliveryState() {
        helper.createDatabase(DATABASE_NAME, 2).apply {
            insertConversation()
            execSQL(
                """
                INSERT INTO messages(
                    id, conversationId, sender, content, createdAt, syncState, lastError
                ) VALUES(
                    'message', 'conversation', 'USER', 'Retry me', 2, 'FAILED', 'Offline'
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            DATABASE_NAME,
            3,
            true,
            MIGRATION_2_3
        ).use { database ->
            database.query(
                """
                SELECT syncState, attemptStartedAt, nextAttemptAt, backendMessageId
                FROM messages
                WHERE id = 'message'
                """.trimIndent()
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(MessageDeliveryState.FAILED_RETRYABLE, cursor.getString(0))
                assertEquals(true, cursor.isNull(1))
                assertEquals(true, cursor.isNull(2))
                assertEquals(true, cursor.isNull(3))
            }
        }
    }

    @Test
    fun migrationThreeToFourPreservesAttachmentsAndAddsUploadState() {
        helper.createDatabase("${DATABASE_NAME}-attachments", 3).apply {
            insertConversation()
            execSQL(
                """
                INSERT INTO messages(
                    id, conversationId, sender, content, createdAt, syncState,
                    lastError, attemptStartedAt, nextAttemptAt, backendMessageId
                ) VALUES(
                    'message', 'conversation', 'USER', 'Image', 2, 'SENT',
                    NULL, NULL, NULL, NULL
                )
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO attachments(
                    id, messageId, contentUri, fileName, mimeType, sizeBytes,
                    backendUrl, syncState, createdAt
                ) VALUES(
                    'attachment', 'message', NULL, 'image.png', 'image/png', 8,
                    '/uploads/image', 'SYNCED', 3
                )
                """.trimIndent()
            )
            close()
        }

        helper.runMigrationsAndValidate(
            "${DATABASE_NAME}-attachments",
            4,
            true,
            MIGRATION_3_4
        ).use { database ->
            database.query(
                "SELECT syncState, backendAttachmentId FROM attachments"
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals("UPLOADED", cursor.getString(0))
                assertEquals("attachment", cursor.getString(1))
            }
        }
    }

    private fun SupportSQLiteDatabase.insertConversation() {
        execSQL(
            """
            INSERT INTO conversations(id, title, createdAt, updatedAt)
            VALUES('conversation', 'Title', 1, 1)
            """.trimIndent()
        )
    }

    private companion object {
        const val DATABASE_NAME = "message-delivery-migration-test"
    }
}
