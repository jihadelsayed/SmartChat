package com.smartchat.core.network

import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class BackendRepositoryTest {
    @Test
    fun structuredErrorPreservesRetryabilityAndRequestId() = runTest {
        val response = errorResponse<HealthData>(
            status = 429,
            body = """
                {
                  "error": {
                    "code": "AI_RATE_LIMITED",
                    "message": "Please wait.",
                    "retryable": true,
                    "requestId": "request-123"
                  }
                }
            """.trimIndent(),
            retryAfter = "30"
        )

        val result = apiRequest { response } as ApiResult.Error

        assertEquals("AI_RATE_LIMITED", result.code)
        assertEquals("request-123", result.requestId)
        assertEquals(true, result.retryable)
        assertEquals(30_000L, result.retryAfterMillis)
    }

    @Test
    fun unknownClientErrorDefaultsToPermanent() = runTest {
        val result = apiRequest {
            errorResponse<HealthData>(
                status = 422,
                body = """{"error":{"code":"INVALID_INPUT","message":"Invalid request"}}"""
            )
        } as ApiResult.Error

        assertFalse(result.retryable!!)
    }

    @Test
    fun unstructuredServerErrorDefaultsToRetryableWithoutLeakingBody() = runTest {
        val result = apiRequest {
            errorResponse<HealthData>(
                status = 503,
                body = "provider stack trace and secret"
            )
        } as ApiResult.Error

        assertTrue(result.retryable!!)
        assertFalse(result.message.contains("provider stack trace"))
        assertFalse(result.message.contains("secret"))
    }

    private fun <T> errorResponse(
        status: Int,
        body: String,
        retryAfter: String? = null
    ): Response<ApiEnvelope<T>> {
        val rawBuilder = okhttp3.Response.Builder()
            .request(Request.Builder().url("https://smartchat.test/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message("HTTP $status")
        retryAfter?.let { rawBuilder.header("Retry-After", it) }
        return Response.error(body.toResponseBody(), rawBuilder.build())
    }
}
