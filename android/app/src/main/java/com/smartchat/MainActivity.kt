package com.smartchat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.smartchat.navigation.SmartChatApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = application as SmartChatApplication

        setContent {
            SmartChatApp(
                chatRepository = application.chatRepository,
                settingsRepository = application.settingsRepository,
                authRepository = application.authRepository,
                profileRepository = application.profileRepository
            )
        }
    }
}
