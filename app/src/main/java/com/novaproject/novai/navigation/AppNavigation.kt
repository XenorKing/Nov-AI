package com.novaproject.novai.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.novaproject.novai.ui.screens.*
import com.novaproject.novai.ui.theme.NovAITheme
import com.novaproject.novai.viewmodel.AuthViewModel
import com.novaproject.novai.viewmodel.ChatViewModel
import com.novaproject.novai.viewmodel.SettingsViewModel

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object Conversations : Screen("conversations")
    object Settings : Screen("settings")
    object Profile : Screen("profile")
    object PrivacyPolicy : Screen("privacy_policy")
    object Stats : Screen("stats")
    object Chat : Screen("chat/{convId}") {
        fun route(id: String) = "chat/$id"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()
    // Fix #2: hoist SettingsViewModel to activity scope so it is shared with SettingsScreen
    // and the accent color updates live without restarting the app.
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.state.collectAsState()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("novai_prefs", Context.MODE_PRIVATE) }

    val username = authState.user?.displayName?.takeIf { it.isNotBlank() } ?: ""

    // Wrap everything in NovAITheme here so that accent color changes are applied live
    NovAITheme(accentColorKey = settingsState.settings.accentColor) {
        NavHost(navController = navController, startDestination = Screen.Splash.route) {

            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigate = {
                        val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
                        val dest = when {
                            isFirstLaunch -> Screen.Onboarding.route
                            authState.user != null -> Screen.Conversations.route
                            else -> Screen.Login.route
                        }
                        navController.navigate(dest) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        prefs.edit().putBoolean("is_first_launch", false).apply()
                        val dest = if (authState.user != null) Screen.Conversations.route else Screen.Login.route
                        navController.navigate(dest) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onLoginSuccess = {
                        navController.navigate(Screen.Conversations.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(Screen.Conversations.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) }
                )
            }

            composable(Screen.Conversations.route) {
                ConversationsScreen(
                    viewModel = chatViewModel,
                    onOpenChat = { convId -> navController.navigate(Screen.Chat.route(convId)) },
                    onSettings = { navController.navigate(Screen.Settings.route) },
                    onProfile = { navController.navigate(Screen.Profile.route) },
                    onStats = { navController.navigate(Screen.Stats.route) },
                    onSignOut = {
                        authViewModel.signOut()
                        chatViewModel.resetChat()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Conversations.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("convId") { type = NavType.StringType })
            ) { back ->
                val convId = back.arguments?.getString("convId") ?: return@composable
                ChatScreen(
                    convId = convId,
                    username = username,
                    viewModel = chatViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                // Pass the shared settingsViewModel so saves are reflected immediately
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                    viewModel = settingsViewModel
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Conversations.route) { inclusive = true }
                        }
                    },
                    onPrivacyPolicy = { navController.navigate(Screen.PrivacyPolicy.route) },
                    onDeleteAccount = {
                        chatViewModel.deleteAccount {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Conversations.route) { inclusive = true }
                            }
                        }
                    },
                    viewModel = authViewModel
                )
            }

            composable(Screen.PrivacyPolicy.route) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Stats.route) {
                StatsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
