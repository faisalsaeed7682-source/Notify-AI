package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NotificationRecord
import java.text.SimpleDateFormat
import java.util.*

import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import com.example.service.ReplyCache

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.ui.components.NotificationSimpleCard
import com.example.ui.components.ActivityGraph

import com.example.util.IconProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsScreen(
    appName: String,
    viewModel: AppDetailsViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val appIcon = remember(notifications) {
        val pkg = notifications.firstOrNull()?.packageName
        if (pkg != null) {
            IconProvider.getAppIcon(context, pkg)
        } else null
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (appIcon != null) {
                            Image(bitmap = appIcon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        Column {
                            Text(appName, fontWeight = FontWeight.Black)
                            Text("${notifications.size} Messages", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Clear All App Alerts", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.clearAllForApp()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Block App Notifications", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.blockApp()
                                onBack()
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.2f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Session Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        ActivityGraph(notifications = notifications)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${notifications.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text("Alerts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val latest = notifications.firstOrNull()?.timestamp ?: 0L
                                Text(if (latest > 0) SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(latest)) else "--:--", 
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                                Text("Latest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { 
                Text("Message Timeline", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
            items(notifications, key = { it.id }) { record ->
                NotificationSimpleCard(
                    record = record,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        try {
                            val intent = context.packageManager.getLaunchIntentForPackage(record.packageName)
                            if (intent != null) context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    onDelete = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.deleteNotification(record.id) 
                    },
                    onArchive = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.archiveNotification(record.id) 
                    },
                    onStar = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleStar(record.id) 
                    },
                    onBlock = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.blockApp() 
                    },
                    onPin = { /* Not in this screen */ },
                    onImportant = { 
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.updateCategory(record.id, "important") 
                    },
                    onRemind = { /* Not in this screen */ }
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun ChatMessageItem(
    record: NotificationRecord,
    onDelete: (Int) -> Unit,
    onArchive: (Int) -> Unit,
    onSpam: (Int) -> Unit,
    onBlock: () -> Unit,
    onStar: (Int) -> Unit,
    onUpdateCategory: (Int, String) -> Unit = { _, _ -> }
) {
    val formatter = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
    val timeString = formatter.format(Date(record.timestamp))
    var showMenu by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(record.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    Row {
                        if (record.isStarred) {
                            Icon(Icons.Rounded.Star, contentDescription = "Starred", tint = androidx.compose.ui.graphics.Color(0xFFFFD700), modifier = Modifier.size(24.dp).padding(end = 4.dp))
                        }
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Options", modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (record.isStarred) "Unstar" else "Star") },
                            onClick = {
                                showMenu = false
                                onStar(record.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Chat") },
                            onClick = {
                                showMenu = false
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${record.title}: ${record.content}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                context.startActivity(shareIntent)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Archive") },
                            onClick = {
                                showMenu = false
                                onArchive(record.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mark as Spam") },
                            onClick = {
                                showMenu = false
                                onSpam(record.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move to Trash") },
                            onClick = {
                                showMenu = false
                                onDelete(record.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Set as Important") },
                            onClick = {
                                showMenu = false
                                onUpdateCategory(record.id, "important")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Set as Social") },
                            onClick = {
                                showMenu = false
                                onUpdateCategory(record.id, "social")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Block Notifications", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onBlock()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                
                if (record.imagePath != null) {
                    AsyncImage(
                        model = java.io.File(record.imagePath),
                        contentDescription = "Notification Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(record.content, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(timeString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                if (record.hasReply && record.notificationKey != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Reply inline...") },
                        trailingIcon = {
                            IconButton(onClick = { 
                                if (replyText.isNotBlank()) {
                                    ReplyCache.sendReply(context, record.notificationKey, replyText)
                                    replyText = ""
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send Reply")
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true
                    )
                }
            }
        }
    }
}
