package com.smartchat.repository

import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.PublicUser

interface ProfileRepository {
    suspend fun loadCurrentUser(): ApiResult<PublicUser>
}
