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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NotificationRecord
import java.text.SimpleDateFormat
import java.util.*
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Refresh
import com.example.ui.components.NotificationSimpleCard
import com.example.util.IconProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAppDetails: (String, String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val timeFilter by viewModel.timeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val swipeLeftAction by viewModel.swipeLeftAction.collectAsStateWithLifecycle()
    val swipeRightAction by viewModel.swipeRightAction.collectAsStateWithLifecycle()
    val isSelectionModeActive by viewModel.isSelectionModeActive.collectAsStateWithLifecycle()

    
    val haptic = LocalHapticFeedback.current
    val selectedCategory = "All"
    
    val baseNotifications = notifications.filter { !it.isArchived && !it.isSpam }
    val pinnedNotifications = baseNotifications.filter { it.isPinned }
    val otherNotifications = baseNotifications.filter { !it.isPinned }
    
    val groupedByApp = otherNotifications.groupBy { it.packageName }
    
    var expandedApps by remember { mutableStateOf(setOf<String>()) }
    var isSearchActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
 
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
 
    // Stable alphabetical sorting order of groupings so parent views never shift during swipes
    val filteredMap = remember(groupedByApp) {
        groupedByApp.toList()
            .sortedBy { it.second.firstOrNull()?.appName?.lowercase() ?: "" }
            .toMap()
    }
    
    val filteredNotifications = baseNotifications
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode = isSelectionModeActive



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

    val mostActiveAppInfo = remember(baseNotifications) {
        if (baseNotifications.isEmpty()) null
        else {
            val counts = baseNotifications.groupBy { it.appName }
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

    LaunchedEffect(Unit) {
        viewModel.updateTimeFilter(TimeFilter.TODAY)
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll(filteredNotifications.map { it.id }) }) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.starSelected() }) {
                            Icon(Icons.Rounded.Star, contentDescription = "Star Selected")
                        }
                        IconButton(onClick = { viewModel.archiveSelected() }) {
                            Icon(Icons.Rounded.Archive, contentDescription = "Archive Selected")
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Selected")
                        }
                        IconButton(onClick = { viewModel.pinSelected() }) {
                            Icon(Icons.Rounded.PushPin, contentDescription = "Pin Selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Notify Center", fontWeight = FontWeight.ExtraBold)
                            if (baseNotifications.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text("${baseNotifications.size}")
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search, contentDescription = "Toggle Search")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(bottom = 16.dp)
        ) {
            // Static header container for Search and Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            placeholder = { Text("Guided AI Search (try 'from gmail in may' or 'Sami')") },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            singleLine = true,
                            shape = RoundedCornerShape(100),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.6f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.4f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        // Suggestion chips related to guided AI search scenarions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Guided AI: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            val suggestions = listOf("from Gmail in May", "Sami", "Urgent News", "Urgent on Whatsapp", "Starred Notifications")
                            suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.updateSearchQuery(suggestion)
                                    },
                                    label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    shape = CircleShape
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Only actual scrollable list content is inside the LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (pinnedNotifications.isNotEmpty() && !isSearchActive && selectedCategory == "All") {
                    item(key = "pinned_center") {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                                Icon(Icons.Rounded.PushPin, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pinned Center", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pinnedNotifications.forEach { record ->
                                    Card(
                                        onClick = { viewModel.recordInteraction(record.id) },
                                        modifier = Modifier
                                            .width(260.dp)
                                            .heightIn(min = 100.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.4f)),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha=0.1f)), contentAlignment = Alignment.Center) {
                                                    Text(record.appName.take(1).uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(record.appName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, maxLines = 1)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(record.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Text(record.content, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 1. Most active app intelligence indicator
                if (mostActiveAppInfo != null && !isSearchActive) {
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



            if (notifications.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            Box(
                                modifier = Modifier.size(120.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Clear Skies Ahead", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Your distraction-free timeline is ready. We'll capture notifications securely while you focus.", 
                                style = MaterialTheme.typography.bodyMedium, 
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                if (filteredMap.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.FilterListOff, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No matches for '$selectedCategory'", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Try changing your filter or search query.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                filteredMap.forEach { (packageName, records) ->
                    val appName = records.first().appName
                    val isExpanded = expandedApps.contains(packageName)
                    
                    item(key = packageName) {
                        Box(modifier = Modifier.animateItem()) {
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
                    }
                    
                    if (isExpanded) {
                        items(records, key = { it.id }) { record ->
                            Box(modifier = Modifier.padding(horizontal = 4.dp).animateItem()) {
                                NotificationSimpleCard(
                                    record = record,
                                    isSelected = selectedIds.contains(record.id),
                                    isSelectionModeActive = isSelectionMode,
                                    swipeLeftAction = swipeLeftAction,
                                    swipeRightAction = swipeRightAction,
                                    onClick = { 
                                        if (isSelectionMode) {
                                            viewModel.toggleSelection(record.id)
                                        } else {
                                            viewModel.recordInteraction(record.id)
                                            onNavigateToAppDetails(packageName, appName)
                                        }
                                    },
                                    onLongClick = { viewModel.toggleSelection(record.id) },
                                    onDelete = { viewModel.deleteNotification(record.id) },
                                    onArchive = { viewModel.archiveNotification(record.id) },
                                    onStar = { viewModel.toggleStar(record.id) },
                                    onBlock = { viewModel.blockApp(record.packageName) },
                                    onPin = { viewModel.togglePin(record.id) },
                                    onImportant = { viewModel.toggleImportant(record.id) },
                                    onRemind = { hours -> viewModel.setReminder(record.id, hours) }
                                )
                            }
                        }
                        item {
                            Button(
                                onClick = { onNavigateToAppDetails(packageName, appName) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enter Full Preview Room")
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }


}
}

@Composable
fun AppGroupCard(
appName: String, count: Int, isExpanded: Boolean, latestMsg: String, onToggle: () -> Unit, packageName: String? = null, onBlock: () -> Unit = {}, onDelete: () -> Unit = {}) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val appIcon = remember(packageName) {
        if (packageName != null) {
            IconProvider.getAppIcon(context, packageName)
        } else null
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.animateContentSize(animationSpec = tween(300)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
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
