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
