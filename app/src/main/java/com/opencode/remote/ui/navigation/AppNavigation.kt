package com.opencode.remote.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.opencode.remote.ui.sessions.SessionsScreen
import com.opencode.remote.ui.sessions.SessionsViewModel
import com.opencode.remote.ui.detail.DetailScreen
import com.opencode.remote.ui.detail.DetailViewModel
import com.opencode.remote.ui.settings.SettingsScreen
import com.opencode.remote.ui.settings.SettingsViewModel
import com.opencode.remote.ui.help.HelpScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Sessions : Screen("sessions", "Sessions", Icons.Default.Forum)
    data object Detail : Screen("detail/{sessionId}", "Detail", Icons.Default.Chat) {
        fun createRoute(sessionId: String) = "detail/$sessionId"
    }
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Help : Screen("help", "Help", Icons.Default.Info)
}

val bottomNavItems = listOf(
    Screen.Sessions,
    Screen.Detail,
    Screen.Settings,
    Screen.Help
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Track the last visited session ID so Detail tab can navigate back to it
    var lastSessionId by remember { mutableStateOf<String?>(null) }

    // Shared ViewModels
    val sessionsViewModel: SessionsViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = NavigationBarDefaults.Elevation
            ) {
                bottomNavItems.forEachIndexed { index, screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    val isDetailTab = screen is Screen.Detail
                    val hasSessionSelected = lastSessionId != null

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selected,
                        enabled = !(isDetailTab && !hasSessionSelected && !selected),
                        onClick = {
                            if (isDetailTab && hasSessionSelected) {
                                navController.navigate(Screen.Detail.createRoute(lastSessionId!!)) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            } else if (!isDetailTab) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Sessions.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Sessions.route) {
                SessionsScreen(
                    viewModel = sessionsViewModel,
                    onSessionClick = { sessionId ->
                        lastSessionId = sessionId
                        navController.navigate(Screen.Detail.createRoute(sessionId))
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                lastSessionId = sessionId
                val detailViewModel: DetailViewModel = hiltViewModel()
                DetailScreen(
                    viewModel = detailViewModel,
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onConfigChanged = {
                        sessionsViewModel.refresh()
                    }
                )
            }

            composable(Screen.Help.route) {
                HelpScreen()
            }
        }
    }
}
