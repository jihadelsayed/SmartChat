package com.smartchat.core.network

import com.google.gson.Gson
import java.io.IOException
import retrofit2.Response

private val errorParser = Gson()

suspend fun <T> apiRequest(call: suspend () -> Response<ApiEnvelope<T>>): ApiResult<T> = try {
    val response = call()
    val envelope = response.body()
    val data = envelope?.data
    if (response.isSuccessful && envelope?.success == true && data != null) {
        ApiResult.Success(data)
    } else {
        ApiResult.Error(
            message = envelope?.error?.message ?: parseError(response) ?: defaultHttpMessage(response.code()),
            statusCode = response.code()
        )
    }
} catch (_: IOException) {
    ApiResult.Error("Cannot reach the SmartChat backend. Check your connection and try again.")
} catch (_: Exception) {
    ApiResult.Error("The backend returned an unexpected response.")
}

suspend fun apiUnitRequest(call: suspend () -> Response<Unit>): ApiResult<Unit> = try {
    val response = call()
    if (response.isSuccessful) {
        ApiResult.Success(Unit)
    } else {
        ApiResult.Error(parseError(response) ?: defaultHttpMessage(response.code()), response.code())
    }
} catch (_: IOException) {
    ApiResult.Error("Cannot reach the SmartChat backend. Check your connection and try again.")
} catch (_: Exception) {
    ApiResult.Error("The backend returned an unexpected response.")
}

private fun parseError(response: Response<*>): String? = runCatching {
    val body = response.errorBody()?.string().orEmpty()
    errorParser.fromJson(body, ErrorEnvelope::class.java)?.error?.message
}.getOrNull()

private fun defaultHttpMessage(statusCode: Int): String = when (statusCode) {
    400 -> "The request was not accepted. Check the entered information."
    401 -> "Your session has expired. Please sign in again."
    404 -> "The requested item no longer exists."
    409 -> "An account with this email already exists."
    else -> "The server returned HTTP $statusCode."
}

private data class ErrorEnvelope(val error: ApiError?)
