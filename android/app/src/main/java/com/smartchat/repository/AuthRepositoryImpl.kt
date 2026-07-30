package com.smartchat.repository

import com.smartchat.core.datastore.SessionStore
import com.smartchat.core.network.ApiEnvelope
import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.AuthApi
import com.smartchat.core.network.AuthData
import com.smartchat.core.network.LoginRequest
import com.smartchat.core.network.PublicUser
import com.smartchat.core.network.RegisterRequest
import com.smartchat.core.network.apiRequest
import retrofit2.Response

class AuthRepositoryImpl(
    private val api: AuthApi,
    private val sessionStore: SessionStore,
    private val onAuthenticated: suspend () -> Unit = {}
) : AuthRepository {
    override suspend fun login(email: String, password: String): ApiResult<PublicUser> {
        return authenticate {
            api.login(LoginRequest(email = email.trim().lowercase(), password = password))
        }
    }

    override suspend fun register(
        displayName: String,
        email: String,
        password: String
    ): ApiResult<PublicUser> {
        return authenticate {
            api.register(
                RegisterRequest(
                    email = email.trim().lowercase(),
                    password = password,
                    displayName = displayName.trim()
                )
            )
        }
    }

    private suspend fun authenticate(
        request: suspend () -> Response<ApiEnvelope<AuthData>>
    ): ApiResult<PublicUser> = when (val result = apiRequest(request)) {
        is ApiResult.Success -> {
            onAuthenticated()
            sessionStore.saveSession(result.value.user, result.value.accessToken)
            ApiResult.Success(result.value.user)
        }
        is ApiResult.Error -> result
    }
}
