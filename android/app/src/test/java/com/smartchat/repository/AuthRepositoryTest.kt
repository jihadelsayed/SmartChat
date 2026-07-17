package com.smartchat.repository

import com.smartchat.core.datastore.SessionState
import com.smartchat.core.datastore.SessionStore
import com.smartchat.core.network.ApiEnvelope
import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.AuthApi
import com.smartchat.core.network.AuthData
import com.smartchat.core.network.LoginRequest
import com.smartchat.core.network.PublicUser
import com.smartchat.core.network.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthRepositoryTest {
    private val user = PublicUser(
        id = "user-1",
        email = "student@example.com",
        displayName = "Student",
        profileImageUrl = null
    )

    @Test
    fun loginSavesReturnedSession() = runTest {
        val api = FakeAuthApi(AuthData("jwt-token", user))
        val sessionStore = RecordingSessionStore()
        val repository = AuthRepositoryImpl(api, sessionStore)

        val result = repository.login(" Student@Example.com ", "Password1")

        assertTrue(result is ApiResult.Success)
        assertEquals("student@example.com", api.loginRequest?.email)
        assertEquals(user, sessionStore.savedUser)
        assertEquals("jwt-token", sessionStore.savedToken)
    }

    @Test
    fun registerSendsDisplayNameEmailAndPassword() = runTest {
        val api = FakeAuthApi(AuthData("jwt-token", user))
        val repository = AuthRepositoryImpl(api, RecordingSessionStore())

        repository.register(" Student ", " Student@Example.com ", "Password1")

        assertEquals(
            RegisterRequest("student@example.com", "Password1", "Student"),
            api.registerRequest
        )
    }

    private class FakeAuthApi(private val authData: AuthData) : AuthApi {
        var loginRequest: LoginRequest? = null
        var registerRequest: RegisterRequest? = null

        override suspend fun login(request: LoginRequest): Response<ApiEnvelope<AuthData>> {
            loginRequest = request
            return Response.success(ApiEnvelope(success = true, data = authData))
        }

        override suspend fun register(request: RegisterRequest): Response<ApiEnvelope<AuthData>> {
            registerRequest = request
            return Response.success(ApiEnvelope(success = true, data = authData))
        }
    }

    private class RecordingSessionStore : SessionStore {
        private val tokenFlow = MutableStateFlow<String?>(null)
        override val accessToken: Flow<String?> = tokenFlow
        override val sessionState: Flow<SessionState> = MutableStateFlow(SessionState.SignedOut)
        var savedUser: PublicUser? = null
        var savedToken: String? = null

        override suspend fun saveSession(user: PublicUser, accessToken: String) {
            savedUser = user
            savedToken = accessToken
            tokenFlow.value = accessToken
        }

        override suspend fun clearSession() {
            tokenFlow.value = null
        }
    }
}
