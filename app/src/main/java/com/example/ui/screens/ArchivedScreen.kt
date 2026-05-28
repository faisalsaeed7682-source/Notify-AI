package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NotificationRecord

import com.example.ui.components.NotificationSimpleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivedScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val archivedNotes by viewModel.archivedNotifications.collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val isSelectionMode = selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll(archivedNotes.map { it.id }) }) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.unarchiveSelected() }) {
                            Icon(Icons.Rounded.Unarchive, contentDescription = "Unarchive Selected")
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
                    title = { Text("Archived", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        if (archivedNotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Archive, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
                    Spacer(Modifier.height(16.dp))
                    Text("Archive is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(archivedNotes, key = { it.id }) { record ->
                    Box(modifier = Modifier.animateItem()) {
                        NotificationSimpleCard(
                            record = record,
                            isSelected = selectedIds.contains(record.id),
                            onClick = {
                                if (isSelectionMode) viewModel.toggleSelection(record.id)
                            },
                            onLongClick = { viewModel.toggleSelection(record.id) },
                            onDelete = { viewModel.deleteNotification(record.id) },
                            onArchive = { viewModel.unarchiveNotification(record.id) },
                            onStar = { viewModel.toggleStar(record.id) },
                            onBlock = { viewModel.blockApp(record.packageName) },
                            onPin = { viewModel.togglePin(record.id) },
                            onImportant = { viewModel.toggleImportant(record.id) },
                            onRemind = { viewModel.setReminder(record.id, it) }
                        )
                    }
                }
            }
        }
    }
}
