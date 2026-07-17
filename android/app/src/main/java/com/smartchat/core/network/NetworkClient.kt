package com.smartchat.core.network

import com.smartchat.BuildConfig
import com.smartchat.core.datastore.SessionStore
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkClient {
    fun create(sessionStore: SessionStore): SmartChatApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionStore))
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SmartChatApi::class.java)
    }
}
