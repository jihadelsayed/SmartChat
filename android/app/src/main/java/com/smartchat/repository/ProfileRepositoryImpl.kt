package com.smartchat.repository

import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.PublicUser
import com.smartchat.core.network.SmartChatApi
import com.smartchat.core.network.apiRequest

class ProfileRepositoryImpl(
    private val api: SmartChatApi
) : ProfileRepository {
    override suspend fun loadCurrentUser(): ApiResult<PublicUser> = apiRequest(api::currentUser)
}
