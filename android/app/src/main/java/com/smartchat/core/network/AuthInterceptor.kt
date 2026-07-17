package com.smartchat.core.network

import com.smartchat.core.datastore.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val sessionStore: SessionStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val isAuthenticationRequest = original.url.encodedPath.contains("/auth/")
        val token = if (isAuthenticationRequest) null else runBlocking {
            sessionStore.accessToken.first()
        }
        val authenticatedRequest = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }

        val response = chain.proceed(authenticatedRequest)
        if (response.code == 401 && !token.isNullOrBlank()) {
            runBlocking { sessionStore.clearSession() }
        }
        return response
    }
}
