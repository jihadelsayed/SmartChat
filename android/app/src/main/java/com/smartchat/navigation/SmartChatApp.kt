package com.smartchat.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartchat.core.datastore.SessionState
import com.smartchat.core.datastore.SettingsRepository
import com.smartchat.data.ChatRepository
import com.smartchat.feature.auth.login.LoginScreen
import com.smartchat.feature.auth.login.LoginViewModel
import com.smartchat.feature.auth.register.RegisterScreen
import com.smartchat.feature.auth.register.RegisterViewModel
import com.smartchat.feature.chat.ChatScreen
import com.smartchat.feature.history.HistoryScreen
import com.smartchat.feature.profile.ProfileScreen
import com.smartchat.feature.settings.SettingsScreen
import com.smartchat.repository.AuthRepository
import com.smartchat.repository.ProfileRepository
import com.smartchat.ui.theme.SmartChatTheme

@Composable
fun SmartChatApp(
    chatRepository: ChatRepository,
    settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository
) {
    val preferences by settingsRepository.preferences.collectAsStateWithLifecycle(
        initialValue = com.smartchat.core.datastore.SettingsPreferences()
    )
    val sessionState by settingsRepository.sessionState.collectAsStateWithLifecycle(
        initialValue = SessionState.Loading
    )

    SmartChatTheme(darkTheme = preferences.darkTheme) {
        if (sessionState == SessionState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            key(sessionState) {
                SessionNavigation(
                    sessionState = sessionState,
                    chatRepository = chatRepository,
                    settingsRepository = settingsRepository,
                    authRepository = authRepository,
                    profileRepository = profileRepository
                )
            }
        }
    }
}

@Composable
private fun SessionNavigation(
    sessionState: SessionState,
    chatRepository: ChatRepository,
    settingsRepository: SettingsRepository,
    authRepository: AuthRepository,
    profileRepository: ProfileRepository
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isSignedIn = sessionState == SessionState.SignedIn
    val authenticatedRoutes = listOf("chat", "history", "profile", "settings")
    val showBottomBar = isSignedIn && authenticatedRoutes.any { route ->
        currentRoute?.startsWith(route) == true
    }
    val bottomDestinations = listOf(
        AppDestination.Chat,
        AppDestination.History,
        AppDestination.Profile,
        AppDestination.Settings
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        val baseRoute = destination.route.substringBefore("?")
                        NavigationBarItem(
                            selected = currentRoute?.startsWith(baseRoute) == true,
                            onClick = {
                                val route = if (destination is AppDestination.Chat) {
                                    destination.route()
                                } else {
                                    destination.route
                                }
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(destination.label.take(1)) },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (isSignedIn) AppDestination.Chat.route() else AppDestination.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(AppDestination.Login.route) {
                val loginViewModel: LoginViewModel = viewModel(
                    factory = LoginViewModel.Factory(authRepository)
                )
                LoginScreen(
                    viewModel = loginViewModel,
                    onRegister = { navController.navigate(AppDestination.Register.route) }
                )
            }
            composable(AppDestination.Register.route) {
                val registerViewModel: RegisterViewModel = viewModel(
                    factory = RegisterViewModel.Factory(authRepository)
                )
                RegisterScreen(
                    viewModel = registerViewModel,
                    onBack = navController::navigateUp
                )
            }
            composable(
                route = AppDestination.Chat.route,
                arguments = listOf(
                    navArgument("conversationId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                ChatScreen(entry.arguments?.getString("conversationId"), chatRepository)
            }
            composable(AppDestination.History.route) {
                HistoryScreen(
                    chatRepository = chatRepository,
                    onOpenConversation = { conversationId ->
                        navController.navigate(AppDestination.Chat.route(conversationId))
                    },
                    onNewConversation = {
                        navController.navigate(AppDestination.Chat.route())
                    }
                )
            }
            composable(AppDestination.Profile.route) {
                ProfileScreen(profileRepository)
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(settingsRepository, chatRepository)
            }
        }
    }
}
