package com.smartchat.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE messages ADD COLUMN syncState TEXT NOT NULL DEFAULT 'SYNCED'"
        )
        db.execSQL("ALTER TABLE messages ADD COLUMN lastError TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS attachments (
                id TEXT NOT NULL PRIMARY KEY,
                messageId TEXT NOT NULL,
                contentUri TEXT,
                fileName TEXT NOT NULL,
                mimeType TEXT NOT NULL,
                sizeBytes INTEGER NOT NULL,
                backendUrl TEXT,
                syncState TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(messageId) REFERENCES messages(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_attachments_messageId ON attachments(messageId)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE messages ADD COLUMN attemptStartedAt INTEGER")
        db.execSQL("ALTER TABLE messages ADD COLUMN nextAttemptAt INTEGER")
        db.execSQL("ALTER TABLE messages ADD COLUMN backendMessageId TEXT")
        db.execSQL(
            """
            UPDATE messages
            SET syncState = CASE
                WHEN syncState = 'SYNCED' THEN 'SENT'
                WHEN syncState = 'FAILED' THEN 'FAILED_RETRYABLE'
                ELSE syncState
            END
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_messages_backendMessageId
            ON messages(backendMessageId)
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE attachments ADD COLUMN localFilePath TEXT")
        db.execSQL("ALTER TABLE attachments ADD COLUMN backendAttachmentId TEXT")
        db.execSQL("ALTER TABLE attachments ADD COLUMN failureReason TEXT")
        db.execSQL("ALTER TABLE attachments ADD COLUMN attemptStartedAt INTEGER")
        db.execSQL("ALTER TABLE attachments ADD COLUMN contentHash TEXT")
        db.execSQL(
            """
            UPDATE attachments
            SET syncState = CASE
                WHEN syncState = 'SYNCED' THEN 'UPLOADED'
                WHEN syncState = 'FAILED' THEN 'FAILED_RETRYABLE'
                WHEN syncState = 'PENDING' THEN 'PENDING_UPLOAD'
                ELSE syncState
            END,
            backendAttachmentId = CASE
                WHEN backendUrl IS NOT NULL THEN id
                ELSE NULL
            END
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS index_attachments_backendAttachmentId
            ON attachments(backendAttachmentId)
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS pending_attachments (
                id TEXT NOT NULL PRIMARY KEY,
                localMessageId TEXT NOT NULL,
                conversationKey TEXT NOT NULL,
                fileName TEXT NOT NULL,
                mimeType TEXT NOT NULL,
                localFilePath TEXT NOT NULL,
                sizeBytes INTEGER NOT NULL,
                contentHash TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_pending_attachments_localMessageId " +
                "ON pending_attachments(localMessageId)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_pending_attachments_conversationKey " +
                "ON pending_attachments(conversationKey)"
        )
    }
}
