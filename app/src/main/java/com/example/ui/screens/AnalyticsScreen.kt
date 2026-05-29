package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.simpleVerticalScrollbar
import com.example.data.local.NotificationRecord
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.geometry.Offset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: DashboardViewModel
) {
    val notifications by viewModel.allNotificationsOverall.collectAsStateWithLifecycle()

    val activeNotifications = remember(notifications) {
        notifications.filter { !it.isArchived && !it.isSpam && !it.isTrash }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var currentTrendFilter by remember { mutableStateOf("Weekly") }

    val todayCount = remember(activeNotifications) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        activeNotifications.count { it.timestamp >= todayStart }
    }

    val chartData = remember(activeNotifications, currentTrendFilter) {
        val list = mutableListOf<DayAlertsValue>()
        when (currentTrendFilter) {
            "Weekly" -> {
                val sdf = SimpleDateFormat("EEE", Locale.getDefault())
                for (i in 6 downTo 0) {
                    val dCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                    val name = sdf.format(dCal.time)
                    dCal.set(Calendar.HOUR_OF_DAY, 0)
                    dCal.set(Calendar.MINUTE, 0)
                    dCal.set(Calendar.SECOND, 0)
                    dCal.set(Calendar.MILLISECOND, 0)
                    val ds = dCal.timeInMillis
                    val de = ds + 24 * 60 * 60 * 1000L - 1
                    list.add(DayAlertsValue(name, activeNotifications.count { it.timestamp in ds..de }))
                }
            }
            "Monthly" -> {
                for (i in 3 downTo 0) {
                    val dCal = Calendar.getInstance()
                    dCal.add(Calendar.WEEK_OF_YEAR, -i)
                    dCal.set(Calendar.DAY_OF_WEEK, dCal.firstDayOfWeek)
                    val ds = dCal.timeInMillis
                    dCal.add(Calendar.DAY_OF_YEAR, 6)
                    val de = dCal.timeInMillis + 24 * 60 * 60 * 1000L - 1
                    list.add(DayAlertsValue("W${4-i}", activeNotifications.count { it.timestamp in ds..de }))
                }
            }
            "All Time" -> {
                val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
                for (i in 5 downTo 0) {
                    val dCal = Calendar.getInstance().apply { add(Calendar.YEAR, -i) }
                    val name = sdf.format(dCal.time)
                    dCal.set(Calendar.DAY_OF_YEAR, 1)
                    dCal.set(Calendar.HOUR_OF_DAY, 0)
                    dCal.set(Calendar.MINUTE, 0)
                    dCal.set(Calendar.SECOND, 0)
                    dCal.set(Calendar.MILLISECOND, 0)
                    val ds = dCal.timeInMillis
                    dCal.add(Calendar.YEAR, 1)
                    val de = dCal.timeInMillis - 1
                    list.add(DayAlertsValue(name, activeNotifications.count { it.timestamp in ds..de }))
                }
            }
        }
        list
    }

    val topSenderData = remember(activeNotifications) {
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val weeklyNotes = activeNotifications.filter { it.timestamp >= weekAgo }
        val senderGroup = weeklyNotes.groupBy { it.appName }
        senderGroup.maxByOrNull { it.value.size }
    }

    val appAnalytics = remember(activeNotifications) {
        activeNotifications.groupBy { it.packageName }.map { (pkg, notes) ->
            AppInflowData(pkg, notes.firstOrNull()?.appName ?: pkg, notes.size)
        }.sortedByDescending { it.count }
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
        LearningPatternInfo(
            patternName = when (peakHr) {
                in 0..5 -> "Late Night Nocturnal"
                in 6..11 -> "Morning Peak Influx"
                in 12..17 -> "Afternoon Clutter Spike"
                else -> "Evening Wind-down Inflow"
            },
            peakHour = peakHr,
            peakCount = peakVal,
            recommendation = when (peakHr) {
                in 0..5 -> "Most alerts show up between 12 AM and 6 AM. Consider setting auto-DND schedules to improve sleep hygiene."
                in 6..11 -> "Incoming alerts peak from 6 AM to 12 PM. Use focus blocks to avoid shattered morning productivity."
                in 12..17 -> "High volume peaks between 12 PM and 6 PM. Mid-day alerts interrupt tasks. Use focus blocks to batch these securely."
                else -> "Notifications peak in the evening. Keep your personal wind-down time clear of app pings to seamlessly disconnect."
            }
        )
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Segmented Style Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(100))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Trends", "Insider").forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    val haptic = LocalHapticFeedback.current
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedTabIndex = index
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().weight(1f).simpleVerticalScrollbar(listState),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (selectedTabIndex == 0) {
                    item(key = "trends_period_filter") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            listOf("Weekly", "Monthly", "All Time").forEach { opt ->
                                FilterChip(
                                    selected = currentTrendFilter == opt,
                                    onClick = { currentTrendFilter = opt },
                                    label = { Text(opt, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    border = null
                                )
                            }
                        }
                    }

                    item(key = "general_metrics") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            InsightMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Today's Volume",
                                value = todayCount.toString(),
                                icon = Icons.Rounded.NotificationsActive,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            InsightMetricCard(
                                modifier = Modifier.weight(1f),
                                title = "Total Lifetime",
                                value = activeNotifications.size.toString(),
                                icon = Icons.Rounded.AllInclusive,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    item(key = "beautiful_chart") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text("$currentTrendFilter Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("A visual curve of your notification volume", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                val maxVal = (chartData.maxOfOrNull { it.count }?.toFloat() ?: 1f).coerceAtLeast(1f)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(180.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    chartData.forEach { dataPoint ->
                                        val barHeightFraction = (dataPoint.count / maxVal).coerceIn(0f, 1f)
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f).fillMaxHeight()
                                        ) {
                                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.BottomCenter) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                                    Text(
                                                        text = if (dataPoint.count > 0) dataPoint.count.toString() else "",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(0.6f)
                                                            .fillMaxHeight(barHeightFraction.coerceAtLeast(0.02f))
                                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = dataPoint.dayName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (topSenderData != null) {
                        item(key = "busiest_sender_card") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text("Top Distractor", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Text("${topSenderData.key} with ${topSenderData.value.size} alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedTabIndex == 1) {
                    item(key = "insider_ai_block") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Pattern Intelligence", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(16.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(hourlySuggestions.patternName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(100)
                                            ) {
                                                Text(
                                                    text = "Peak: ${hourlySuggestions.peakHour}:00",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(hourlySuggestions.recommendation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    item(key = "insider_breakdown_label") {
                        Text(
                            text = "Top Application Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (appAnalytics.isEmpty()) {
                        item(key = "empty_apps") {
                            Text("No notifications recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        items(appAnalytics.take(10), key = { "app_${it.packageName}" }) { data ->
                            AppAnalyticsItemRow(data)
                        }
                    }
                    
                    item(key = "wellbeing_tip_bottom") {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Wellbeing Snapshot", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Unplugging 1 hour before bed enhances REM sleep directly. Try keeping your device out of your bedroom tonight.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsightMetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.8f))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = contentColor)
        }
    }
}

@Composable
fun AppAnalyticsItemRow(data: AppInflowData) {
    val maxCountLimit = 50.0f
    val ratio = (data.count / maxCountLimit).coerceIn(0.1f, 1.0f)
    val color = when {
        data.count > 30 -> Color(0xFFE57373)
        data.count > 10 -> Color(0xFFFFD54F)
        else -> Color(0xFF81C784)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(data.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Surface(color = color.copy(alpha = 0.2f), shape = RoundedCornerShape(100)) {
                    Text(
                        text = "${data.count} Alerts",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(modifier = Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(CircleShape).background(color))
            }
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
