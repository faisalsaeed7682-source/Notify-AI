package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Check
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.exportChatToPdfAndShare
import com.example.util.simpleVerticalScrollbar
import com.example.data.local.NotificationRecord
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.DateRange

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    // Check model weights states
    val currentModel by com.example.ai.LocalLLMManager.currentModelFlow.collectAsStateWithLifecycle()
    val downloadedModels by com.example.ai.LocalLLMManager.downloadedModels.collectAsStateWithLifecycle()
    val isDownloading by com.example.ai.LocalLLMManager.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by com.example.ai.LocalLLMManager.downloadProgress.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

        val selectedApps by viewModel.selectedApps.collectAsStateWithLifecycle()
        val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()
        val allUniqueApps by viewModel.allUniqueApps.collectAsStateWithLifecycle()
        var showAppPopup by remember { mutableStateOf(false) }
        var popupSearchQuery by remember { mutableStateOf("") }
        var showFilterDialog by remember { mutableStateOf(false) }

        // Wizard states
        var isSearchExpanded by remember { mutableStateOf(true) }
        var wizardSelectedApp by remember { mutableStateOf("") }
        var wizardMonth by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }
        var wizardYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
        var wizardKeyword by remember { mutableStateOf("") }
        var showWizardAppDropdown by remember { mutableStateOf(false) }

        // Select and delete states
        var showSelectDeleteDialog by remember { mutableStateOf(false) }
        var selectedToClearIds by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text("AI Finder", fontWeight = FontWeight.ExtraBold)
                        Text("Analyze & Search History", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Context Filters") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Limit intelligence search to specific apps or time periods.", style = MaterialTheme.typography.bodySmall)
                        
                        OutlinedButton(onClick = { /* Date picker logic */ }) {
                            Icon(Icons.Rounded.DateRange, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (dateRange.first != null) "Time Range Set" else "Select Date Range")
                        }
                        
                        Text("Apps Captured", style = MaterialTheme.typography.labelMedium)
                        // This would ideally be a Flow of apps from repository
                        val apps = listOf("WhatsApp", "Instagram", "Gmail", "System")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            apps.forEach { app ->
                                FilterChip(
                                    selected = app in selectedApps,
                                    onClick = {
                                        val new = if (app in selectedApps) selectedApps - app else selectedApps + app
                                        viewModel.updateSelectedApps(new)
                                    },
                                    label = { Text(app) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFilterDialog = false }) { Text("Apply") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.updateSelectedApps(emptySet())
                        viewModel.updateDateRange(null, null)
                        showFilterDialog = false
                    }) { Text("Reset Filters") }
                }
            )
        }



        val activeNotifications by viewModel.activeNotifications.collectAsStateWithLifecycle()
        if (showSelectDeleteDialog) {
            val filteredActive = activeNotifications.filter { !it.isArchived && !it.isSpam && !it.isTrash }
            AlertDialog(
                onDismissRequest = { showSelectDeleteDialog = false },
                title = { Text("Select & Delete Captured List", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Select which notifications to move into Trash feed:", style = MaterialTheme.typography.bodySmall)
                        if (filteredActive.isEmpty()) {
                            Text("No active notifications found to select.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { selectedToClearIds = filteredActive.map { it.id }.toSet() }) {
                                    Text("Select All")
                                }
                                TextButton(onClick = { selectedToClearIds = emptySet() }) {
                                    Text("Clear Selection")
                                }
                            }
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredActive, key = { it.id }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedToClearIds = if (selectedToClearIds.contains(item.id)) {
                                                    selectedToClearIds - item.id
                                                } else {
                                                    selectedToClearIds + item.id
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = selectedToClearIds.contains(item.id),
                                            onCheckedChange = { isChecked ->
                                                selectedToClearIds = if (isChecked == true) {
                                                    selectedToClearIds + item.id
                                                } else {
                                                    selectedToClearIds - item.id
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(item.title, fontWeight = FontWeight.Bold, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                                            Text(item.content, maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSelectedNotifications(selectedToClearIds.toList())
                            selectedToClearIds = emptySet()
                            showSelectDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        enabled = selectedToClearIds.isNotEmpty()
                    ) {
                        Text("Trash Selected (${selectedToClearIds.size})")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSelectDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            val msgListState = androidx.compose.foundation.lazy.rememberLazyListState()
            LazyColumn(
                state = msgListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .simpleVerticalScrollbar(msgListState),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                reverseLayout = true
            ) {
                if (messages.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillParentMaxWidth()
                                .height(300.dp)
                                .animateContentSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text("Welcome to AI Finder!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(8.dp))
                            Text("Ask questions about your captured notifications. AI will help you filter, find, and summarize alerts intelligently.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(messages.reversed()) { msg ->
                        ChatBubbleItem(msg)
                    }
                }
            }

            if (isSearchExpanded) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .heightIn(max = 600.dp)
                        .animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    border = BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column {
                        Column(modifier = Modifier.weight(1f, fill = false).padding(14.dp).verticalScroll(rememberScrollState())) {
                            Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.FilterList,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "AI Finder Filters",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 1. Target Application Picker
                        Text("1. Target Application", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)) {
                            var appDropdownExpanded by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = { appDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Apps, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (wizardSelectedApp.isEmpty()) "All Captured Apps" else wizardSelectedApp,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = appDropdownExpanded,
                                onDismissRequest = { appDropdownExpanded = false },
                                modifier = Modifier.heightIn(max = 220.dp)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Captured Apps") },
                                    onClick = {
                                        wizardSelectedApp = ""
                                        appDropdownExpanded = false
                                    }
                                )
                                allUniqueApps.forEach { appName ->
                                    DropdownMenuItem(
                                        text = { Text(appName) },
                                        onClick = {
                                            wizardSelectedApp = appName
                                            appDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Chronological Period Picker
                        Text("2. Chronological Period", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            var showMonthDropdown by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showMonthDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[wizardMonth - 1],
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = showMonthDropdown,
                                    onDismissRequest = { showMonthDropdown = false }
                                ) {
                                    (1..12).forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[m - 1]) },
                                            onClick = {
                                                wizardMonth = m
                                                showMonthDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            var showYearDropdown by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showYearDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = wizardYear.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                                DropdownMenu(
                                    expanded = showYearDropdown,
                                    onDismissRequest = { showYearDropdown = false }
                                ) {
                                    listOf(2024, 2025, 2026, 2027).forEach { y ->
                                        DropdownMenuItem(
                                            text = { Text(y.toString()) },
                                            onClick = {
                                                wizardYear = y
                                                showYearDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Keyword / Query Filter Input
                        Text("3. Target Keyword Filter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = wizardKeyword,
                            onValueChange = { wizardKeyword = it },
                            placeholder = { Text("e.g. key details, urgent notification...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        )
                        
                        val realTimePreview = remember(wizardSelectedApp, wizardKeyword, activeNotifications) {
                            if (wizardSelectedApp.isNotEmpty() && wizardKeyword.isNotBlank()) {
                                activeNotifications.filter { it.appName == wizardSelectedApp || it.packageName == wizardSelectedApp }
                                    .filter { it.title.contains(wizardKeyword, ignoreCase = true) || it.content.contains(wizardKeyword, ignoreCase = true) }
                                    .take(5) // Limit to 5 previews
                            } else {
                                emptyList()
                            }
                        }

                        androidx.compose.animation.AnimatedVisibility(visible = realTimePreview.isNotEmpty()) {
                            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                Text("Real-time Preview from History (${realTimePreview.size} matches)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    realTimePreview.forEach { record ->
                                        ChatNotificationCardItem(record, highlightKeyword = wizardKeyword)
                                    }
                                }
                            }
                        }

                        }

                        // 4. Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    wizardSelectedApp = ""
                                    wizardKeyword = ""
                                    wizardMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
                                    wizardYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset")
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.queryGuidedAsk(
                                        appName = wizardSelectedApp,
                                        month = wizardMonth,
                                        year = wizardYear,
                                        keyword = wizardKeyword
                                    )
                                    isSearchExpanded = false
                                },
                                enabled = wizardSelectedApp.isNotEmpty() && wizardKeyword.isNotBlank(),
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("AI Scan History", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

        }
    }
    
    if (showAppPopup) {
        AlertDialog(
            onDismissRequest = { showAppPopup = false },
            title = { Text("Which app is it?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = popupSearchQuery,
                        onValueChange = { popupSearchQuery = it },
                        placeholder = { Text("Search apps...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    val filteredApps = allUniqueApps.filter { 
                        it.contains(popupSearchQuery, ignoreCase = true) 
                    }
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (filteredApps.isEmpty()) {
                            item {
                                Text("No captured apps match.", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            items(filteredApps) { appName ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showAppPopup = false
                                            viewModel.updateSelectedApps(setOf(appName))
                                            inputText = "Prompt for $appName: "
                                        }
                                        .padding(vertical = 2.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(appName, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAppPopup = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ChatBubbleItem(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var copied by remember { mutableStateOf(false) }
    
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier.widthIn(max = 320.dp),
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 20.dp
                    ))
                    .background(bgColor)
                    .padding(16.dp)
            ) {
                Text(text = message.text, color = textColor, style = MaterialTheme.typography.bodyLarge)
            }
            
            if (!message.isUser && !message.matchedNotifications.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    message.matchedNotifications.forEach { record ->
                        ChatNotificationCardItem(record = record)
                    }
                }
            }

            if (!message.isUser && !message.isLoading) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        clipboardManager.setText(AnnotatedString(message.text))
                        copied = true
                    },
                    modifier = Modifier.size(32.dp).padding(top = 4.dp)
                ) {
                    Icon(
                        if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy, 
                        contentDescription = "Copy", 
                        modifier = Modifier.size(16.dp), 
                        tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatNotificationCardItem(record: NotificationRecord, highlightKeyword: String? = null) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }

    val appIcon = remember(record.packageName) {
        com.example.util.IconProvider.getAppIcon(context, record.packageName)
    }
    val timeStr = remember(record.timestamp) {
        java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp))
    }

    // Helper for highlighting text
    @Composable
    fun getHighlightedString(text: String, keyword: String?): AnnotatedString {
        if (keyword.isNullOrBlank() || !text.contains(keyword, ignoreCase = true)) {
            return buildAnnotatedString { append(text) }
        }
        return buildAnnotatedString {
            val startIndex = text.indexOf(keyword, ignoreCase = true)
            append(text.substring(0, startIndex))
            withStyle(style = androidx.compose.ui.text.SpanStyle(background = MaterialTheme.colorScheme.primaryContainer, color = MaterialTheme.colorScheme.onPrimaryContainer)) {
                append(text.substring(startIndex, startIndex + keyword.length))
            }
            append(text.substring(startIndex + keyword.length))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboardManager.setText(AnnotatedString("${record.title}: ${record.content}"))
                copied = true
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (appIcon != null) {
                    androidx.compose.foundation.Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.appName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                if (copied) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Rounded.Check, contentDescription = "Copied", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = getHighlightedString(record.title, highlightKeyword),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = getHighlightedString(record.content, highlightKeyword),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
