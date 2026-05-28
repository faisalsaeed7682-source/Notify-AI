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
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.util.exportChatToPdfAndShare
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showPdfDialog by remember { mutableStateOf(false) }
    var pdfName by remember { mutableStateOf("Ask_Chat_History") }



    if (showPdfDialog) {
        AlertDialog(
            onDismissRequest = { showPdfDialog = false },
            title = { Text("Export Chat as PDF") },
            text = {
                OutlinedTextField(
                    value = pdfName,
                    onValueChange = { pdfName = it },
                    label = { Text("File Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPdfDialog = false
                    exportChatToPdfAndShare(context, messages, pdfName)
                }) {
                    Text("Export & Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfDialog = false }) { Text("Cancel") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Chat History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = {
                        viewModel.startNewChat()
                        scope.launch { drawerState.close() }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "New Chat", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    label = { Text("New Chat") },
                    selected = false,
                    onClick = {
                        viewModel.startNewChat()
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                HorizontalDivider()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (chatHistory.isEmpty()) {
                        item { Text("No past chats.", modifier = Modifier.padding(16.dp)) }
                    }
                    items(chatHistory, key = { it.id }) { session ->
                        var showRenameDialog by remember { mutableStateOf(false) }
                        var newName by remember { mutableStateOf(session.name) }
                        
                        if (showRenameDialog) {
                            AlertDialog(
                                onDismissRequest = { showRenameDialog = false },
                                title = { Text("Rename Chat") },
                                text = {
                                    OutlinedTextField(
                                        value = newName,
                                        onValueChange = { newName = it },
                                        singleLine = true
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.renameSession(session.id, newName)
                                        showRenameDialog = false
                                    }) { Text("Save") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
                                }
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.loadHistorySession(session)
                                    scope.launch { drawerState.close() }
                                }
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = session.name, maxLines = 1, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { showRenameDialog = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Rename", modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.deleteSession(session.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {
        val selectedApps by viewModel.selectedApps.collectAsStateWithLifecycle()
        val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()
        val allUniqueApps by viewModel.allUniqueApps.collectAsStateWithLifecycle()
        var showAppPopup by remember { mutableStateOf(false) }
        var popupSearchQuery by remember { mutableStateOf("") }
        var showFilterDialog by remember { mutableStateOf(false) }

        // Wizard states
        var isSearchExpanded by remember { mutableStateOf(false) }
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
            TopAppBar(
                title = { Text("AI Ask Intelligence", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch { drawerState.open() }
                    }) {
                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showPdfDialog = true
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share Chat as PDF")
                    }
                }
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

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { msg ->
                    ChatBubbleItem(msg)
                }
            }

            if (isSearchExpanded) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    border = BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
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
                                    text = "AI Search Guider",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isSearchExpanded = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Collapse filters",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
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
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        )

                        // 4. Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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

            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        popupSearchQuery = ""
                        showAppPopup = true 
                    }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Select App Popup", tint = MaterialTheme.colorScheme.primary)
                    }
                    
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about notifications...") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        )
                    )

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSearchExpanded = !isSearchExpanded
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "AI Search Guider",
                            tint = if (isSearchExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
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
}

@Composable
fun ChatBubbleItem(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

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
                    },
                    modifier = Modifier.size(32.dp).padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Rounded.ContentCopy, 
                        contentDescription = "Copy", 
                        modifier = Modifier.size(16.dp), 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatNotificationCardItem(record: NotificationRecord) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val appIcon = remember(record.packageName) {
        com.example.util.IconProvider.getAppIcon(context, record.packageName)
    }
    val timeStr = remember(record.timestamp) {
        java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboardManager.setText(AnnotatedString("${record.title}: ${record.content}"))
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = record.title,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = record.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
