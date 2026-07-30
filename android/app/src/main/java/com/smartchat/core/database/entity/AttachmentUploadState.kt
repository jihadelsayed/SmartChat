package com.smartchat.core.database.entity

object AttachmentUploadState {
    const val LOCAL = "LOCAL"
    const val PENDING_UPLOAD = "PENDING_UPLOAD"
    const val UPLOADING = "UPLOADING"
    const val UPLOADED = "UPLOADED"
    const val FAILED_RETRYABLE = "FAILED_RETRYABLE"
    const val FAILED_PERMANENT = "FAILED_PERMANENT"
}
