package com.smartchat.repository

import android.net.Uri
import com.smartchat.core.network.ApiResult
import com.smartchat.core.network.PublicUser

interface ProfileRepository {
    suspend fun loadCurrentUser(): ApiResult<PublicUser>

    suspend fun uploadProfileImage(
        uri: Uri
    ): ApiResult<PublicUser>
}