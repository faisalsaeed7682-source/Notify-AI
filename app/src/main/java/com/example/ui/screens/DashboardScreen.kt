package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.foundation.shape.CircleShape
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NotificationRecord
import java.text.SimpleDateFormat
import java.util.*
import android.speech.tts.TextToSpeech
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Refresh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAppDetails: (String, String) -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val timeFilter by viewModel.timeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val briefingText by viewModel.briefingText.collectAsStateWithLifecycle()
    val isGeneratingBriefing by viewModel.isGeneratingBriefing.collectAsStateWithLifecycle()
    val briefingTone by viewModel.briefingTone.collectAsStateWithLifecycle()
    
    val haptic = LocalHapticFeedback.current
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "msg", "social", "sys", "promo", "other")
    
    val baseNotifications = notifications.filter { !it.isArchived && !it.isSpam }
    val groupedByApp = baseNotifications.groupBy { it.packageName }
    
    var expandedApps by remember { mutableStateOf(setOf<String>()) }
    var isSearchActive by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    val filteredMap = if (selectedCategory == "All" || selectedCategory == "trash") {
        groupedByApp
    } else if (selectedCategory == "sys") {
        groupedByApp.filter { it.value.first().category == "sys" || it.key == "android" || it.key == "com.android.systemui" }
    } else {
        groupedByApp.filter { it.value.any { n -> n.category.contains(selectedCategory, ignoreCase = true) } }
    }
    
    val filteredNotifications = filteredMap.values.flatten()
    DisposableEffect(context) {
        val listener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = java.util.Locale.getDefault()
            }
        }
        val textToSpeech = TextToSpeech(context, listener)
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notify Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = "Toggle Search")
                        if (!isSearchActive && searchQuery.isNotEmpty()) {
                            viewModel.updateSearchQuery("")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        placeholder = { Text("Search notifications...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(100)
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeFilter.values().forEach { filter ->
                        val selected = timeFilter == filter
                        Surface(
                            onClick = { viewModel.updateTimeFilter(filter) },
                            shape = RoundedCornerShape(100),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = filter.displayName,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            item {
                OtpExtractorCard(baseNotifications)
            }
            item {
                DashboardInsightsCard(
                    notifications = filteredNotifications,
                    briefingText = briefingText,
                    isGeneratingBriefing = isGeneratingBriefing,
                    briefingTone = briefingTone,
                    onGenerate = { 
                        val yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
                        val recent = baseNotifications.filter { it.timestamp >= yesterday }
                        viewModel.generateBriefing(recent) 
                    },
                    onToneChange = { viewModel.setBriefingTone(it) },
                    tts = tts,
                    isPlaying = isPlaying,
                    onPlayToggle = {
                        if (isPlaying) {
                            tts?.stop()
                            isPlaying = false
                        } else if (briefingText != null) {
                            tts?.speak(briefingText, TextToSpeech.QUEUE_FLUSH, null, "briefing")
                            isPlaying = true
                        }
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = (selectedCategory == category),
                            onClick = { 
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedCategory = category 
                            },
                            label = { Text(if(category == "sys") "System" else category.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }
            
            if (notifications.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No Notifications Captured Yet", style = MaterialTheme.typography.titleMedium)
                            Text("We're listening securely in the background.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                if (filteredMap.isEmpty()) {
                    item {
                        Text("No notifications match this category.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                filteredMap.forEach { (packageName, records) ->
                    val appName = records.first().appName
                    val isExpanded = expandedApps.contains(packageName)
                    
                    item(key = packageName) {
                        AppGroupCard(
                            appName = appName,
                            count = records.size,
                            isExpanded = isExpanded,
                            latestMsg = records.first().title,
                            onToggle = { 
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                expandedApps = if (isExpanded) expandedApps - packageName else expandedApps + packageName
                            },
                            packageName = packageName,
                            onBlock = { viewModel.blockApp(packageName) },
                            onDelete = { viewModel.deleteApp(packageName) }
                        )
                    }
                    
                    if (isExpanded) {
                        items(records.take(3)) { record ->
                            val context = LocalContext.current
                            NotificationSimpleCard(
                                record = record,
                                onClick = { 
                                    try {
                                        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                                        if (intent != null) {
                                            context.startActivity(intent)
                                        } else {
                                            onNavigateToAppDetails(packageName, appName)
                                        }
                                    } catch (e: Exception) {
                                        onNavigateToAppDetails(packageName, appName)
                                    }
                                },
                                onDelete = { viewModel.deleteNotification(record.id) },
                                onSpam = { viewModel.markSpam(record.id) },
                                onStar = { viewModel.toggleStar(record.id) },
                                onBlock = { viewModel.blockApp(record.packageName) },
                                onUpdateCategory = { viewModel.updateCategory(record.id, it) }
                            )
                        }
                        if (records.size > 3) {
                            item {
                                TextButton(
                                    onClick = { onNavigateToAppDetails(packageName, appName) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("View all ${records.size} messages")
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun OtpExtractorCard(notifications: List<NotificationRecord>) {
    val recentOtps = remember(notifications) {
        val tenMinsAgo = System.currentTimeMillis() - 10 * 60 * 1000L
        val regex = Regex("(?i)\\b([A-Z0-9]{1,4}-[A-Z0-9]{3,8})\\b|\\b(\\d{4,8})\\b")
        notifications.filter { it.timestamp >= tenMinsAgo && (it.category == "msg" || it.title.contains("code", true) || it.content.contains("code", true)) }
            .mapNotNull { notif ->
                val code = regex.find(notif.content)?.value ?: regex.find(notif.title)?.value
                if (code != null && !code.equals("code", true)) notif to code else null
            }
    }

    if (recentOtps.isNotEmpty()) {
        val (notif, code) = recentOtps.first()
        val clipboardManager: ClipboardManager = LocalClipboardManager.current
        val context = LocalContext.current

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("OTP Detected: ${notif.appName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(code, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        android.widget.Toast.makeText(context, "Copied: $code", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.tertiary, CircleShape)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy OTP", tint = MaterialTheme.colorScheme.onTertiary)
                }
            }
        }
    }
}

@Composable
fun DashboardInsightsCard(
    notifications: List<NotificationRecord>,
    briefingText: String?,
    isGeneratingBriefing: Boolean,
    briefingTone: String,
    onGenerate: () -> Unit,
    onToneChange: (String) -> Unit,
    tts: TextToSpeech?,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Analytics, contentDescription = "AI", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Distraction Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(notifications.size.toString(), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Text("Total Alerts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
                }
                
                val mostActive = notifications.groupBy { it.appName }.maxByOrNull { it.value.size }
                Column(horizontalAlignment = Alignment.End) {
                    Text(mostActive?.key ?: "None", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Most Distracting", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val tones = listOf("Friendly", "Professional", "Humorous", "Concise")
            
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tones.forEach { tone ->
                    FilterChip(
                        selected = briefingTone == tone,
                        onClick = { onToneChange(tone) },
                        label = { Text(tone, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (briefingText != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column {
                        Text(briefingText, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onPlayToggle) {
                                Icon(if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = "TTS")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isPlaying) "Stop" else "Play")
                            }
                            TextButton(onClick = onGenerate, enabled = !isGeneratingBriefing) {
                                if (isGeneratingBriefing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Regenerate")
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(if (isGeneratingBriefing) "Wait..." else "Regenerate")
                            }
                        }
                    }
                }
            } else {
                Button(
                    onClick = onGenerate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isGeneratingBriefing,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    if (isGeneratingBriefing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onSecondary, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading GGUF Pipeline...")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run Local Offline Briefing")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            
            Text("Top 5 Distracting Apps", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(16.dp))
            
            val topApps = notifications.groupBy { it.appName }.map { it.key to it.value.size }.sortedByDescending { it.second }.take(5)
            if (topApps.isNotEmpty()) {
                val maxCount = topApps.maxOf { it.second }.coerceAtLeast(1)
                
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    topApps.forEach { (appName, count) ->
                        val ratio = count.toFloat() / maxCount.toFloat()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = appName.take(12),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.width(80.dp),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ratio)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Text("Not enough data to graph.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
fun ActivityGraph(notifications: List<NotificationRecord>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = 12.dp.toPx()
            val spacing = 8.dp.toPx()
            val maxBars = (size.width / (barWidth + spacing)).toInt()
            
            val buckets = IntArray(maxBars) { 0 }
            if (notifications.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val minTime = notifications.minOf { it.timestamp }
                val range = (now - minTime).coerceAtLeast(1)
                
                notifications.forEach {
                    val idx = (((it.timestamp - minTime).toFloat() / range) * (maxBars - 1)).toInt()
                    buckets[idx]++
                }
                
                val maxVal = buckets.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
                
                for (i in 0 until maxBars) {
                    val h = (buckets[i] / maxVal) * size.height
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.7f),
                        topLeft = Offset(i * (barWidth + spacing), size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun AppGroupCard(appName: String, count: Int, isExpanded: Boolean, latestMsg: String, onToggle: () -> Unit, packageName: String? = null, onBlock: () -> Unit = {}, onDelete: () -> Unit = {}) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val appIcon = remember(packageName) {
        if (packageName != null) {
            try {
                context.packageManager.getApplicationIcon(packageName).toBitmap(width = 96, height = 96).asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.animateContentSize(animationSpec = tween(300)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha=0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(appName.take(1).uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                if (!isExpanded) {
                    Text(latestMsg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Expand"
            )
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("Move All to Trash", color = MaterialTheme.colorScheme.error) }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = false
                        onDelete()
                    })
                    DropdownMenuItem(text = { Text("Block App", color = MaterialTheme.colorScheme.error) }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = false
                        onBlock()
                    })
                }
            }
        }
    }
}

@Composable
fun NotificationSimpleCard(record: NotificationRecord, onClick: () -> Unit, onDelete: () -> Unit = {}, onSpam: () -> Unit = {}, onStar: () -> Unit = {}, onBlock: () -> Unit = {}, onUpdateCategory: (String) -> Unit = {}) {
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp).clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (record.isStarred) {
                        Icon(Icons.Rounded.Star, contentDescription = "Starred", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp).padding(end = 4.dp))
                    }
                    Text(record.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Text(record.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text(if (record.isStarred) "Unstar" else "Star") }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = false
                        onStar()
                    })
                    DropdownMenuItem(text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = false
                        onDelete()
                    })
                    DropdownMenuItem(text = { Text("Block App", color = MaterialTheme.colorScheme.error) }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = false
                        onBlock()
                    })
                    DropdownMenuItem(text = { Text("Mark Spam", color = MaterialTheme.colorScheme.error) }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = false
                        onSpam()
                    })
                    DropdownMenuItem(text = { Text("Set as Important") }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = false
                        onUpdateCategory("important")
                    })
                    DropdownMenuItem(text = { Text("Set as Social") }, onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showMenu = false
                        onUpdateCategory("social")
                    })
                }
            }
        }
    }
}
