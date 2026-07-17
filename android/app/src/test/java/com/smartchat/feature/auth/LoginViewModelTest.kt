package com.smartchat.feature.auth

import com.smartchat.MainDispatcherRule
import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.PublicUser
import com.smartchat.feature.auth.login.LoginViewModel
import com.smartchat.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun validCredentialsAuthenticateAndClearLoading() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)
        viewModel.updateEmail("student@example.com")
        viewModel.updatePassword("Password1")

        viewModel.login()
        advanceUntilIdle()

        assertEquals("student@example.com", repository.loginEmail)
        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.authenticationSucceeded)
    }

    @Test
    fun invalidEmailIsRejectedWithoutCallingRepository() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository)
        viewModel.updateEmail("invalid")
        viewModel.updatePassword("Password1")

        viewModel.login()

        assertEquals(null, repository.loginEmail)
        assertEquals("Enter a valid email address.", viewModel.state.value.errorMessage)
    }

    private class FakeAuthRepository : AuthRepository {
        var loginEmail: String? = null

        override suspend fun login(email: String, password: String): ApiResult<PublicUser> {
            loginEmail = email
            return ApiResult.Success(USER)
        }

        override suspend fun register(
            displayName: String,
            email: String,
            password: String
        ): ApiResult<PublicUser> = ApiResult.Success(USER)
    }

    private companion object {
        val USER = PublicUser("id", "student@example.com", "Student", null)
    }
}
