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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.CloudDownload
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    val preloadedMessages = listOf(
        "Summarize today's alerts",
        "Show my most important messages",
        "Any OTP or codes?",
        "Did I miss any emails?"
    )

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
                Text(
                    "Chat History",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
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
        Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notify AI", fontWeight = FontWeight.Bold) },
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
                        viewModel.startNewChat()
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "New Chat")
                    }
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

            // Preloaded chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                preloadedMessages.forEach { msg ->
                    SuggestionChip(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.sendMessage(msg)
                        },
                        label = { Text(msg) }
                    )
                }
            }

            // Inline GGUF Downloader Panel
            val selectedModel = currentModel
            val isModelDownloaded = downloadedModels.contains(selectedModel.name)
            val isModelDownloading = isDownloading[selectedModel.name] == true
            val progress = downloadProgress[selectedModel.name] ?: 0.0f

            if (!isModelDownloaded) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Local AI weights not downloaded",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Text(
                            text = "To run true offline token summarization on this device, you need the GGUF layers for ${selectedModel.displayName} (${selectedModel.size}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.9f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        
                        if (isModelDownloading) {
                            Column(modifier = Modifier.padding(top = 4.dp)) {
                                LinearProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Downloading layers... ${(progress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        } else {
                            Button(
                                onClick = { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    com.example.ai.LocalLLMManager.startModelWeightsDownload(context, selectedModel) 
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                modifier = Modifier.padding(top = 4.dp).height(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDownload,
                                    contentDescription = "Download",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download ${selectedModel.displayName} (${selectedModel.size})", style = MaterialTheme.typography.labelSmall)
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
