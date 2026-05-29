package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.simpleVerticalScrollbar

import android.content.Context
import android.os.PowerManager
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel, 
    onNavigateToBlockedApps: () -> Unit,
    onNavigateToAppCategories: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToStarred: () -> Unit,
    onNavigateToArchived: () -> Unit,
    onNavigateToImportant: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val useCloudAi by viewModel.useCloudAi.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var isIgnoringBattery by remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSwipeDialog by remember { mutableStateOf(false) }
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsStateWithLifecycle()
    val swipeRightAction by viewModel.swipeRightAction.collectAsStateWithLifecycle()
    val themeOptions = listOf(
        0 to "System Default",
        1 to "Light Mode",
        2 to "Dark Mode",
        4 to "Glass Ambient"
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("Settings", fontWeight = FontWeight.ExtraBold)
                        Text("Preferences & System Tools", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .simpleVerticalScrollbar(scrollState)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            // General Section
            SettingsSectionHeader("General")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
            ) {
                Column {
                    SettingsItemAction(
                        icon = Icons.Rounded.Palette,
                        title = "App Theme",
                        subtitle = when(themeMode) {
                            0 -> "System Default"
                            1 -> "Light Mode"
                            2 -> "Dark Mode"
                            4 -> "Glass Ambient"
                            else -> "Dark Mode"
                        },
                        onClick = { showThemeDialog = true }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                    SettingsItemAction(
                        icon = Icons.Rounded.History,
                        title = "Notification History",
                        subtitle = "Timeline of all captured alerts",
                        onClick = onNavigateToHistory
                    )
                }
            }

            // Notifications Section
            SettingsSectionHeader("Smart Management")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
            ) {
                Column {
                    SettingsItemAction(
                        icon = Icons.Rounded.Star,
                        title = "Starred Notifications",
                        subtitle = "Quick access to tagged alerts",
                        onClick = onNavigateToStarred
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                    SettingsItemAction(
                        icon = Icons.Rounded.Archive,
                        title = "Archived Notifications",
                        subtitle = "Hidden but not forgotten",
                        onClick = onNavigateToArchived
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                    SettingsItemAction(
                        icon = Icons.Rounded.PriorityHigh,
                        title = "Important Notifications",
                        subtitle = "AI-flagged priority alerts",
                        onClick = onNavigateToImportant
                    )
                }
            }

            // Cleanup & Privacy
            SettingsSectionHeader("Cleanup & Privacy")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
            ) {
                Column {
                    SettingsItemAction(
                        icon = Icons.Rounded.Block,
                        title = "Blocked Apps",
                        subtitle = "Muted from intelligence pipeline",
                        onClick = onNavigateToBlockedApps
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                    SettingsItemAction(
                        icon = Icons.Rounded.SwapHoriz,
                        title = "Swipe Gestures",
                        subtitle = "Customize left/right actions",
                        onClick = { showSwipeDialog = true }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                    SettingsItemAction(
                        icon = Icons.Rounded.Delete,
                        title = "Trash Bin",
                        subtitle = "Recover or purge recently deleted",
                        onClick = onNavigateToTrash
                    )
                }
            }

            // System Diagnostics
            SettingsSectionHeader("System & About")
            Card(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
            ) {
                Column {
                    if (!isIgnoringBattery) {
                        SettingsItemAction(
                            icon = Icons.Rounded.BatteryAlert,
                            title = "Battery Optimization",
                            subtitle = "Action required for reliability",
                            onClick = {
                                val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                intent.data = Uri.parse("package:${context.packageName}")
                                context.startActivity(intent)
                                isIgnoringBattery = true
                            }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.1f))
                    }
                    SettingsItemAction(
                        icon = Icons.Rounded.Info,
                        title = "App Version",
                        subtitle = "Version 1.2.0 (Build 302)",
                        onClick = {}
                    )
                }
            }

            // Changelog
            Card(
                modifier = Modifier.padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Updates", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("• Premium Glassmorphism & Ambient Glow\n• Advanced Guided AI Search (App, Month, Keyword)\n• Filterable Timeline History & Date Presets\n• Added notification pinning & selector toggles", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // Dialogs

        if (showSwipeDialog) {
            AlertDialog(
                onDismissRequest = { showSwipeDialog = false },
                title = { Text("Swipe Gestures") },
                text = {
                    Column {
                        Text("Left Swipe", style = MaterialTheme.typography.labelLarge)
                        listOf(0 to "Delete", 1 to "Archive", 2 to "Star").forEach { (id, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.setSwipeLeftAction(id) }.padding(8.dp)
                            ) {
                                RadioButton(selected = swipeLeftAction == id, onClick = { viewModel.setSwipeLeftAction(id) })
                                Text(label)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Right Swipe", style = MaterialTheme.typography.labelLarge)
                        listOf(0 to "Delete", 1 to "Archive", 2 to "Star").forEach { (id, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.setSwipeRightAction(id) }.padding(8.dp)
                            ) {
                                RadioButton(selected = swipeRightAction == id, onClick = { viewModel.setSwipeRightAction(id) })
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSwipeDialog = false }) { Text("Done") }
                }
            )
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Select Theme") },
                text = {
                    Column {
                        themeOptions.forEach { (mode, label) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    viewModel.setThemeMode(mode); showThemeDialog = false 
                                }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = themeMode == mode, onClick = null)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SettingsItemToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsItemAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
    }
}
