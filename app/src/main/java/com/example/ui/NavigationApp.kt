package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.navigation.AppDetailsRoute
import com.example.ui.navigation.BlockedAppsRoute
import com.example.ui.navigation.ChatRoute
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.screens.*

@Composable
fun NavigationApp(
    dashboardViewModel: DashboardViewModel,
    chatViewModel: ChatViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentDestination?.route?.contains("DashboardRoute") == true,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(DashboardRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.ChatBubble, contentDescription = "AI Chat") },
                    label = { Text("AI Ask") },
                    selected = currentDestination?.route?.contains("ChatRoute") == true,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(ChatRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentDestination?.route?.contains("SettingsRoute") == true,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navController.navigate(SettingsRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<DashboardRoute> {
                DashboardScreen(dashboardViewModel, onNavigateToAppDetails = { pkg, name ->
                    navController.navigate(AppDetailsRoute(pkg, name))
                })
            }
            composable<ChatRoute> {
                ChatScreen(chatViewModel)
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    settingsViewModel, 
                    onNavigateToBlockedApps = {
                        navController.navigate(BlockedAppsRoute)
                    },
                    onNavigateToAppCategories = {
                        navController.navigate(com.example.ui.navigation.AppCategoriesRoute)
                    },
                    onNavigateToTrash = {
                        navController.navigate(com.example.ui.navigation.TrashRoute)
                    }
                )
            }
            composable<com.example.ui.navigation.TrashRoute> {
                TrashScreen(
                    dashboardViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<BlockedAppsRoute> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val repository = DiHelper.getRepository(context)
                val factory = BlockedAppsViewModelFactory(repository, context)
                val blockedAppsViewModel: BlockedAppsViewModel = viewModel(factory = factory)
                BlockedAppsScreen(viewModel = blockedAppsViewModel, onBack = { navController.popBackStack() })
            }
            composable<com.example.ui.navigation.AppCategoriesRoute> {
                val context = androidx.compose.ui.platform.LocalContext.current
                val repository = DiHelper.getRepository(context)
                val factory = AppCategoriesViewModelFactory(repository, context)
                val viewModel: AppCategoriesViewModel = viewModel(factory = factory)
                AppCategoriesScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
            composable<AppDetailsRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<AppDetailsRoute>()
                val factory = AppDetailsViewModelFactory(DiHelper.getRepository(context), route.packageName)
                val viewModel: AppDetailsViewModel = viewModel(factory = factory)
                AppDetailsScreen(
                    appName = route.appName,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
