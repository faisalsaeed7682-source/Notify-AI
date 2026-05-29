package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
object ChatRoute

@Serializable
object SettingsRoute

@Serializable
object BlockedAppsRoute

@Serializable
object AppCategoriesRoute

@Serializable
object TrashRoute

@Serializable
object TimelineRoute

@Serializable
object StarredRoute

@Serializable
object ArchivedRoute

@Serializable
object ImportantRoute

@Serializable
object AnalyticsRoute

@Serializable
data class AppDetailsRoute(val packageName: String, val appName: String)
