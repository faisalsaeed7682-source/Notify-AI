package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NotificationRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.historyNotifications.collectAsStateWithLifecycle()
    val timeFilter by viewModel.timeFilter.collectAsStateWithLifecycle()
    val historyAppFilter by viewModel.historyAppFilter.collectAsStateWithLifecycle()
    val historyDateRange by viewModel.historyDateRange.collectAsStateWithLifecycle()
    val allNotificationsOverall by viewModel.allNotificationsOverall.collectAsStateWithLifecycle()

    val uniqueApps = remember(allNotificationsOverall) {
        allNotificationsOverall.map { it.appName }.distinct().sorted()
    }

    var showAppMenu by remember { mutableStateOf(false) }
    var showDateRangeDialog by remember { mutableStateOf(false) }

    val groupedByDate = notifications.groupBy { 
        SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date(it.timestamp))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Timeline Filter Tabs
            ScrollableTabRow(
                selectedTabIndex = TimeFilter.values().filter { it != TimeFilter.RANGE }.indexOf(timeFilter).coerceAtLeast(0),
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {}
            ) {
                TimeFilter.values().filter { it != TimeFilter.RANGE }.forEach { filter ->
                    Tab(
                        selected = timeFilter == filter,
                        onClick = { viewModel.updateTimeFilter(filter) },
                        text = { Text(filter.displayName) }
                    )
                }
            }

            // Quick Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Filter Pill
                Box {
                    AssistChip(
                        onClick = { showAppMenu = true },
                        label = { Text(historyAppFilter ?: "All Apps") },
                        leadingIcon = { Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenu(
                        expanded = showAppMenu,
                        onDismissRequest = { showAppMenu = false },
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Apps (Reset)") },
                            onClick = {
                                viewModel.updateHistoryAppFilter(null)
                                showAppMenu = false
                            }
                        )
                        uniqueApps.forEach { appName ->
                            DropdownMenuItem(
                                text = { Text(appName) },
                                onClick = {
                                    viewModel.updateHistoryAppFilter(appName)
                                    showAppMenu = false
                                }
                            )
                        }
                    }
                }

                // Custom Date Range Picker Pill
                AssistChip(
                    onClick = { 
                        viewModel.updateTimeFilter(TimeFilter.RANGE)
                        showDateRangeDialog = true 
                    },
                    label = { 
                        val range = historyDateRange
                        if (timeFilter == TimeFilter.RANGE && (range.first != null || range.second != null)) {
                            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
                            val startStr = range.first?.let { sdf.format(Date(it)) } ?: "?"
                            val endStr = range.second?.let { sdf.format(Date(it)) } ?: "?"
                            Text("$startStr - $endStr")
                        } else {
                            Text("Date Range")
                        }
                    },
                    leadingIcon = { Icon(Icons.Rounded.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (timeFilter == TimeFilter.RANGE) MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.4f) else MaterialTheme.colorScheme.surface
                    )
                )
            }

            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No notifications found for this selection.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedByDate.forEach { (date, records) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(records) { record ->
                            TimelineItemCard(record)
                        }
                    }
                }
            }
        }

        if (showDateRangeDialog) {
            AlertDialog(
                onDismissRequest = { showDateRangeDialog = false },
                title = { Text("Filter History by Period", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Select a quick date preset to filter notifications:")
                        
                        val presets = listOf(
                            "Last 24 Hours" to (24 * 3600 * 1000L),
                            "Last 3 Days" to (3 * 24 * 3600 * 1000L),
                            "Last 7 Days" to (7 * 24 * 3600 * 1000L),
                            "Last 30 Days" to (30 * 24 * 3600 * 1000L)
                        )
                        
                        presets.forEach { (label, duration) ->
                            OutlinedButton(
                                onClick = {
                                    val now = System.currentTimeMillis()
                                    viewModel.updateHistoryDateRange(now - duration, now)
                                    viewModel.updateTimeFilter(TimeFilter.RANGE)
                                    showDateRangeDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { 
                        viewModel.updateHistoryDateRange(null, null)
                        viewModel.updateTimeFilter(TimeFilter.ALL)
                        showDateRangeDialog = false 
                    }) {
                        Text("Reset Date Filters")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TimelineItemCard(record: NotificationRecord) {
    val context = LocalContext.current
    val appIcon = remember(record.packageName) {
        com.example.util.IconProvider.getAppIcon(context, record.packageName)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (appIcon != null) {
                androidx.compose.foundation.Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(record.appName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(record.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
                Text(record.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(
                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(record.timestamp)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
