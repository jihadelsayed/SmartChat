package com.smartchat.repository

import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.PublicUser

interface AuthRepository {
    suspend fun login(email: String, password: String): ApiResult<PublicUser>
    suspend fun register(displayName: String, email: String, password: String): ApiResult<PublicUser>
}
