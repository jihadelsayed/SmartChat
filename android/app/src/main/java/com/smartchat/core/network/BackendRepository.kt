package com.smartchat.core.network

import com.google.gson.Gson
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import retrofit2.Response

private val errorParser = Gson()

suspend fun <T> apiRequest(call: suspend () -> Response<ApiEnvelope<T>>): ApiResult<T> = try {
    val response = call()
    val envelope = response.body()
    val data = envelope?.data
    if (response.isSuccessful && envelope?.success == true && data != null) {
        ApiResult.Success(data)
    } else {
        val parsedError = envelope?.error ?: parseError(response)
        ApiResult.Error(
            message = parsedError?.message ?: defaultHttpMessage(response.code()),
            statusCode = response.code(),
            code = parsedError?.code,
            retryable = parsedError?.retryable ?: defaultRetryable(response.code()),
            requestId = parsedError?.requestId,
            retryAfterMillis = parseRetryAfterMillis(response)
        )
    }
} catch (_: IOException) {
    ApiResult.Error(
        message = "Cannot reach the SmartChat backend. Check your connection and try again.",
        retryable = true
    )
} catch (_: Exception) {
    ApiResult.Error(
        message = "The backend returned an unexpected response.",
        retryable = true
    )
}

suspend fun apiUnitRequest(call: suspend () -> Response<Unit>): ApiResult<Unit> = try {
    val response = call()
    if (response.isSuccessful) {
        ApiResult.Success(Unit)
    } else {
        val parsedError = parseError(response)
        ApiResult.Error(
            message = parsedError?.message ?: defaultHttpMessage(response.code()),
            statusCode = response.code(),
            code = parsedError?.code,
            retryable = parsedError?.retryable ?: defaultRetryable(response.code()),
            requestId = parsedError?.requestId,
            retryAfterMillis = parseRetryAfterMillis(response)
        )
    }
} catch (_: IOException) {
    ApiResult.Error(
        message = "Cannot reach the SmartChat backend. Check your connection and try again.",
        retryable = true
    )
} catch (_: Exception) {
    ApiResult.Error(
        message = "The backend returned an unexpected response.",
        retryable = true
    )
}

private fun parseError(response: Response<*>): ApiError? = runCatching {
    val body = response.errorBody()?.string().orEmpty()
    errorParser.fromJson(body, ErrorEnvelope::class.java)?.error
}.getOrNull()

private fun defaultRetryable(statusCode: Int): Boolean =
    statusCode == 429 || statusCode >= 500

private fun parseRetryAfterMillis(response: Response<*>): Long? {
    val value = response.headers()["Retry-After"]?.trim().orEmpty()
    if (value.isEmpty()) return null
    value.toLongOrNull()?.let { seconds ->
        if (seconds >= 0) return seconds * 1_000L
    }
    val retryAt = runCatching {
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }.parse(value)?.time
    }.getOrNull() ?: return null
    return (retryAt - System.currentTimeMillis()).coerceAtLeast(0L)
}

private fun defaultHttpMessage(statusCode: Int): String = when (statusCode) {
    400 -> "The request was not accepted. Check the entered information."
    401 -> "Your session has expired. Please sign in again."
    404 -> "The requested item no longer exists."
    409 -> "An account with this email already exists."
    else -> "The server returned HTTP $statusCode."
}

private data class ErrorEnvelope(val error: ApiError?)
