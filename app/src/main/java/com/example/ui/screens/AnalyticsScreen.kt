package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NotificationRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: DashboardViewModel
) {
    val notifications by viewModel.allNotificationsOverall.collectAsStateWithLifecycle()

    // Highly optimized memoization of analytics metrics to prevent unnecessary layout recalculations
    val activeNotifications = remember(notifications) {
        notifications.filter { !it.isArchived && !it.isSpam && !it.isTrash }
    }

    // Top sender of the week calculation
    val topSenderWeek = remember(activeNotifications) {
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val weeklyNotes = activeNotifications.filter { it.timestamp >= weekAgo }
        val senderGroup = weeklyNotes.groupBy { it.appName }
        val topSender = senderGroup.maxByOrNull { it.value.size }
        if (topSender != null) {
            "${topSender.key} (${topSender.value.size} Alerts)"
        } else {
            "No alerts recorded"
        }
    }

    var selectedTrendDay by remember { mutableStateOf<DayAlertsValue?>(null) }
    
    // Compute peak hours for selected day dynamically
    val selectedDayPeakHours = remember(selectedTrendDay, notifications) {
        if (selectedTrendDay == null) emptyList<Pair<Int, Int>>() else {
            val dCal = Calendar.getInstance()
            val sdf = SimpleDateFormat("EEE", Locale.getDefault())
            var targetMsStart = 0L
            var targetMsEnd = 0L
            for (i in 6 downTo 0) {
                val testCal = Calendar.getInstance()
                testCal.add(Calendar.DAY_OF_YEAR, -i)
                if (sdf.format(testCal.time) == selectedTrendDay?.dayName) {
                    testCal.set(Calendar.HOUR_OF_DAY, 0)
                    testCal.set(Calendar.MINUTE, 0)
                    testCal.set(Calendar.SECOND, 0)
                    testCal.set(Calendar.MILLISECOND, 0)
                    targetMsStart = testCal.timeInMillis
                    targetMsEnd = targetMsStart + 24 * 60 * 60 * 1000L - 1
                    break
                }
            }
            if (targetMsStart == 0L) emptyList() else {
                val dayRecords = notifications.filter { it.timestamp in targetMsStart..targetMsEnd }
                val hrBuckets = IntArray(24) { 0 }
                dayRecords.forEach {
                    val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    val h = c.get(Calendar.HOUR_OF_DAY)
                    if (h in 0..23) hrBuckets[h]++
                }
                hrBuckets.indices
                    .map { it to hrBuckets[it] }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
            }
        }
    }

    val appAnalytics = remember(activeNotifications) {
        activeNotifications.groupBy { it.packageName }.map { (pkg, notes) ->
            val appName = notes.firstOrNull()?.appName ?: pkg
            val count = notes.size
            AppInflowData(pkg, appName, count)
        }.sortedByDescending { it.count }
    }

    val todayCount = remember(activeNotifications) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        activeNotifications.count { it.timestamp >= todayStart }
    }

    val weeklyData = remember(activeNotifications) {
        val list = mutableListOf<DayAlertsValue>()
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        
        for (i in 6 downTo 0) {
            val dCal = Calendar.getInstance()
            dCal.add(Calendar.DAY_OF_YEAR, -i)
            val name = sdf.format(dCal.time)
            
            dCal.set(Calendar.HOUR_OF_DAY, 0)
            dCal.set(Calendar.MINUTE, 0)
            dCal.set(Calendar.SECOND, 0)
            dCal.set(Calendar.MILLISECOND, 0)
            val ds = dCal.timeInMillis
            val de = ds + 24 * 60 * 60 * 1000L - 1
            
            val count = activeNotifications.count { it.timestamp in ds..de }
            list.add(DayAlertsValue(name, count))
        }
        list
    }

    val hourlySuggestions = remember(activeNotifications) {
        val hours = IntArray(24) { 0 }
        activeNotifications.forEach {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val hr = c.get(Calendar.HOUR_OF_DAY)
            if (hr in 0..23) hours[hr]++
        }
        val peakHr = hours.indices.maxByOrNull { hours[it] } ?: 12
        val peakVal = hours[peakHr]
        
        val patternTitle = when (peakHr) {
            in 0..5 -> "Late Night Nocturnal"
            in 6..11 -> "Morning Peak Influx"
            in 12..17 -> "Afternoon Clutter Spike"
            else -> "Evening Wind-down Inflow"
        }
        
        val description = when (peakHr) {
            in 0..5 -> "Most alerts show up between 12 AM and 6 AM. This sleep-time focus leakage directly disrupts sleep hygiene. Consider setting auto-DND schedules!"
            in 6..11 -> "Your peak distraction period is from 6 AM to 12 PM. Incoming alerts can shatter morning deep-work. Schedule 1-hour batch digests."
            in 12..17 -> "High volume peaks between 12 PM and 6 PM. Mid-day alerts interrupt tasks. Use focus blocks to batch these alerts securely."
            else -> "Notifications peak in the evening (6 PM to 12 AM). Your personal wind-down time is carry-over territory of app pings. Unwind offline."
        }

        LearningPatternInfo(patternTitle, peakHr, peakVal, description)
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Weekly", "Insider")
    val haptic = LocalHapticFeedback.current

    val mostActiveAppInfo = remember(activeNotifications) {
        if (activeNotifications.isEmpty()) null
        else {
            val counts = activeNotifications.groupBy { it.appName }
            val entry = counts.maxByOrNull { it.value.size }
            if (entry != null) {
                val appName = entry.key
                val count = entry.value.size
                val latestTime = entry.value.maxOfOrNull { it.timestamp } ?: 0L
                val timeStr = if (latestTime > 0L) {
                    java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(latestTime))
                } else ""
                Triple(appName, count, timeStr)
            } else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights Center", fontWeight = FontWeight.Black, fontSize = 24.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Styled TabRow with premium tactile look
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selectedTabIndex = index
                        },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.ExtraBold else FontWeight.Medium,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTabIndex == 0) {
                    // --- WEEKLY TRENDS TAB ---
            // General metrics overview
            item(key = "general_metrics") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Captured Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(todayCount.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.AllInclusive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Total Registered", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(notifications.size.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            }

            if (selectedTabIndex == 0) {
            // Beautiful minimal weekly bar chart
            item(key = "weekly_chart") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(28.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.InsertChartOutlined, contentDescription = "Weekly Chart", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Weekly Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Usage counts over the last 7 days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ContactSupport, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Busiest Sender: $topSenderWeek",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        Text(
                            text = "💡 Tip: Click any day column to view its hourly peak congestion times!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        Spacer(Modifier.height(20.dp))

                        val maxAlerts = weeklyData.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            weeklyData.forEach { dayAlert ->
                                val barRatio = dayAlert.count.toFloat() / maxAlerts.toFloat()
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedTrendDay = dayAlert
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = dayAlert.count.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dayAlert.count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(14.dp)
                                            .fillMaxHeight(barRatio.coerceAtLeast(0.06f))
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (dayAlert.count == maxAlerts) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            )
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = dayAlert.dayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } // End of Weekly tab content

            if (selectedTabIndex == 1) {
                // --- INSIDER TAB CONTENT ---

                // High Activity Detector
                if (mostActiveAppInfo != null) {
                    item(key = "most_active_stat_widget") {
                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("High Activity Detector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Most active application is ${mostActiveAppInfo.first} with ${mostActiveAppInfo.second} captured notifications. Latest activity was at ${mostActiveAppInfo.third}.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTabIndex == 1) {
                // AI Learning Pattern
            item(key = "learning_pattern") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(28.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = "Intelligence", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AI Focus Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = hourlySuggestions.patternName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(100)
                                    ) {
                                        Text(
                                            text = "Peak: ${hourlySuggestions.peakHour}:00",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                
                                Text(
                                    text = hourlySuggestions.recommendation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
            }

            if (selectedTabIndex == 1) {
            // Distracting Apps Headers
            item(key = "most_active_title") {
                Text(
                    text = "Most Active Applications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            if (appAnalytics.isEmpty()) {
                item(key = "empty_analytics") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No notifications capture stats recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(appAnalytics.take(10), key = { "app_analytic_${it.packageName}" }) { data ->
                    AppAnalyticsItem(data)
                }
            }
        }
    }
}

        if (selectedTrendDay != null) {
            AlertDialog(
                onDismissRequest = { selectedTrendDay = null },
                title = { Text("📊 Hourly Peak Inflow: ${selectedTrendDay?.dayName}", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Hourly notification density breakdown for ${selectedTrendDay?.dayName}:", style = MaterialTheme.typography.bodySmall)
                        
                        if (selectedDayPeakHours.isEmpty()) {
                            Text("No notifications recorded on this day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedDayPeakHours) { (hour, count) ->
                                    val hrLabel = if (hour == 0) "12 AM" else if (hour < 12) "$hour AM" else if (hour == 12) "12 PM" else "${hour - 12} PM"
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(hrLabel, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text("$count alerts", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTrendDay = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

data class DayAlertsValue(val dayName: String, val count: Int)

data class LearningPatternInfo(
    val patternName: String,
    val peakHour: Int,
    val peakCount: Int,
    val recommendation: String
)

data class AppInflowData(val packageName: String, val appName: String, val count: Int)

@Composable
fun AppAnalyticsItem(data: AppInflowData) {
    val maxCountLimit = 50.0f
    val ratio = (data.count / maxCountLimit).coerceIn(0.12f, 1.0f)

    val progressBarColor = when {
        data.count > 30 -> Color(0xFFE57373) // soft red
        data.count > 10 -> Color(0xFFFFD54F) // soft yellow
        else -> Color(0xFF81C784) // soft green
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(data.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Surface(
                    color = progressBarColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(100)
                ) {
                    Text(
                        text = "${data.count} Alerts",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = progressBarColor
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(progressBarColor)
                )
            }
        }
    }
}
