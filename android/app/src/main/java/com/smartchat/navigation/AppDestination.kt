package com.smartchat.navigation

sealed class AppDestination(val route: String, val label: String) {
    data object Login : AppDestination("login", "Login")
    data object Register : AppDestination("register", "Register")
    data object Chat : AppDestination("chat?conversationId={conversationId}", "Chat") {
        fun route(conversationId: String? = null): String =
            if (conversationId == null) "chat" else "chat?conversationId=${android.net.Uri.encode(conversationId)}"
    }
    data object History : AppDestination("history", "History")
    data object Profile : AppDestination("profile", "Profile")
    data object Settings : AppDestination("settings", "Settings")
}
