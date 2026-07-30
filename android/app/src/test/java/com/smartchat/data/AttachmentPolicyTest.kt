package com.smartchat.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentPolicyTest {
    @Test
    fun supportedPolicyMatchesBackendAndRejectsHeicGif() {
        assertTrue("image/jpeg" in AttachmentFileStore.SUPPORTED_MIME_TYPES)
        assertTrue("image/png" in AttachmentFileStore.SUPPORTED_MIME_TYPES)
        assertTrue("image/webp" in AttachmentFileStore.SUPPORTED_MIME_TYPES)
        assertFalse("image/gif" in AttachmentFileStore.SUPPORTED_MIME_TYPES)
        assertFalse("image/heic" in AttachmentFileStore.SUPPORTED_MIME_TYPES)
        assertFalse("image/heif" in AttachmentFileStore.SUPPORTED_MIME_TYPES)
    }
}
