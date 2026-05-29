package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NotificationRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

import com.example.util.IconProvider

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationSimpleCard(
    record: NotificationRecord, 
    isSelected: Boolean = false,
    isSelectionModeActive: Boolean = false,
    swipeLeftAction: Int = 1, // Default to Archive
    swipeRightAction: Int = 2, // Default to Star
    onClick: () -> Unit, 
    onLongClick: () -> Unit = {},
    onDelete: () -> Unit, 
    onArchive: () -> Unit,
    onStar: () -> Unit, 
    onBlock: () -> Unit,
    onPin: () -> Unit,
    onImportant: () -> Unit,
    onRemind: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    
    val scope = rememberCoroutineScope()
    var showMenuDialog by remember { mutableStateOf(false) }

    if (showMenuDialog) {
        ModalBottomSheet(onDismissRequest = { showMenuDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Manage Notification",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(record.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(record.content, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                val options = listOf(
                    Triple(
                        if (record.isStarred) "Remove Star" else "Star Notification",
                        Icons.Rounded.Star,
                        onStar
                    ),
                    Triple(
                        if (record.isPinned) "Unpin Notification" else "Pin Notification",
                        Icons.Rounded.PushPin,
                        onPin
                    ),
                    Triple(
                        "Archive Notification",
                        Icons.Rounded.Archive,
                        onArchive
                    ),
                    Triple(
                        "Move to Trash",
                        Icons.Rounded.Delete,
                        onDelete
                    ),
                    Triple(
                        "Select and Batch Action...",
                        Icons.Rounded.CheckCircle,
                        { onLongClick() }
                    ),
                    Triple(
                        "Block ${record.appName}",
                        Icons.Rounded.Block,
                        onBlock
                    )
                )

                options.forEach { (label, icon, action) ->
                    ListItem(
                        headlineContent = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                        leadingContent = { 
                            Icon(
                                imageVector = icon, 
                                contentDescription = null, 
                                tint = if (label == "Move to Trash" || label.startsWith("Block")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ) 
                        },
                        colors = ListItemDefaults.colors(
                            headlineColor = if (label == "Move to Trash" || label.startsWith("Block")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            containerColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showMenuDialog = false
                                action()
                            }
                    )
                }
            }
        }
    }
    
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val action = if (value == SwipeToDismissBoxValue.EndToStart) swipeLeftAction else swipeRightAction
                scope.launch {
                    when(action) {
                        0 -> onDelete()
                        1 -> onArchive()
                        2 -> onStar()
                    }
                }
                // Return true to allow swiping away if the item will be deleted or archived
                if (action == 0 || action == 1) return@rememberSwipeToDismissBoxState true
            }
            false // always snap back cleanly if not deleted
        }
    )

    val appIcon = remember(record.packageName) {
        IconProvider.getAppIcon(context, record.packageName)
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = true,
        enableDismissFromStartToEnd = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val progress = dismissState.progress
            if (direction != null && progress > 0.01f) {
                val action = if (direction == SwipeToDismissBoxValue.StartToEnd) swipeRightAction else swipeLeftAction
                
                // Extremely clean, gorgeous visual hierarchy matching modern mobile app standards
                val color = when (action) {
                    0 -> Color(0xFFEF4444) // Vibrant, rich Red for Deletion action
                    1 -> Color(0xFF10B981) // Vibrant, rich Emerald Green for Archive action
                    2 -> Color(0xFFF59E0B) // Vibrant, rich Gold/Amber Yellow for Star action
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                
                val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                val icon = when (action) {
                    0 -> Icons.Rounded.Delete
                    1 -> Icons.Rounded.Archive
                    2 -> Icons.Rounded.Star
                    else -> Icons.Rounded.Notifications
                }
                
                // Animated dynamic feedback based on swipe progress/percentage
                val scale = if (progress > 0.02f) (progress * 1.25f).coerceIn(0.65f, 1.35f) else 0.5f
                val alpha = if (progress > 0.02f) progress.coerceIn(0f, 1.0f) else 0.0f
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = alpha)) // Also fade background opacity with swipe progress
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                            .size(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
            }
        },
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (isSelectionModeActive) onClick() else showMenuDialog = true
                    }
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
            ),
            shape = RoundedCornerShape(14.dp),
            border = if (isSelected) 
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelectionModeActive) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { _ -> onClick() },
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (record.isPinned) {
                                Icon(Icons.Rounded.PushPin, contentDescription = "Pinned", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(11.dp).padding(end = 4.dp))
                            }
                            if (record.isStarred) {
                                Icon(Icons.Rounded.Star, contentDescription = "Starred", tint = Color(0xFFFFD700), modifier = Modifier.size(11.dp).padding(end = 4.dp))
                            }
                            Text(record.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        HighlightedText(
                            text = record.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                        
                        if (record.imagePath != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            AsyncImage(
                                model = record.imagePath,
                                contentDescription = "Notification Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 110.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        if (record.isImportant) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Icon(Icons.Rounded.PriorityHigh, contentDescription = "Important", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        val category = if (record.appName.contains("Mail") || record.appName.contains("Gmail") || record.appName.contains("Teams")) "Work"
                        else if (record.appName.contains("Insta") || record.appName.contains("WhatsApp") || record.appName.contains("Snap")) "Social"
                        else "General"
                        
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.4f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = category,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
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
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val barWidth = 10.dp.toPx()
            val spacing = 6.dp.toPx()
            val maxBars = (size.width / (barWidth + spacing)).toInt()
            
            val buckets = IntArray(maxBars) { 0 }
            if (notifications.isNotEmpty()) {
                val now = System.currentTimeMillis()
                val minTime = notifications.minOf { it.timestamp }
                val range = (now - minTime).coerceAtLeast(1)
                
                notifications.forEach {
                    val idx = (((it.timestamp - minTime).toFloat() / range) * (maxBars - 1)).toInt()
                    if (idx in 0 until maxBars) buckets[idx]++
                }
                
                val maxVal = buckets.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
                
                for (i in 0 until maxBars) {
                    val h = (buckets[i] / maxVal) * size.height
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.5f),
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
fun HighlightedText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = 4
) {
    val annotatedString = buildAnnotatedString {
        val urgentPattern = "(?i)(urgent|meeting|late|important|deadline)".toRegex()
        
        var lastIndex = 0
        
        // Find all matches for urgent pattern
        val matches = urgentPattern.findAll(text).sortedBy { it.range.first }
        
        matches.forEach { match ->
            // Text before match
            append(text.substring(lastIndex, match.range.first))
            
            // Highlighted match
            withStyle(style = SpanStyle(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary,
                background = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
            )) {
                append(match.value)
            }
            
            lastIndex = match.range.last + 1
        }
        
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
    
    Text(
        text = annotatedString,
        style = style,
        color = color,
        maxLines = maxLines
    )
}
