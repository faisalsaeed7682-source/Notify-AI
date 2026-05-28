package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.Label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    // We should observe labels from DashboardViewModel (which uses the repository)
    // val labels by viewModel.allLabels.collectAsStateWithLifecycle(initialValue = emptyList())
    // For now, I'll assume they are in DashboardViewModel
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newLabelName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.Blue) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Labels", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Label")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text("Create custom labels to organize your notification history. You can assign labels to any captured alert.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))
                
                // Mocking list for now as allLabels flow needs to be connected
                val mockLabels = listOf(
                    Label(1, "Work", Color.Red.toArgb()),
                    Label(2, "Personal", Color.Green.toArgb()),
                    Label(3, "Finance", Color.Yellow.toArgb())
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(mockLabels) { label ->
                        LabelItem(label, onDelete = { /* TODO */ })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Label") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newLabelName,
                        onValueChange = { newLabelName = it },
                        label = { Text("Label Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Select Color", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta, Color.Cyan).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, CircleShape)
                                    .clickable { selectedColor = color }
                                    .let { if (selectedColor == color) it.background(color.copy(alpha=0.3f), CircleShape) else it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    // viewModel.insertLabel(Label(name = newLabelName, color = selectedColor.toArgb()))
                    showAddDialog = false 
                }) { Text("Create") }
            }
        )
    }
}

@Composable
fun LabelItem(label: Label, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(12.dp).background(Color(label.color), CircleShape))
            Spacer(Modifier.width(16.dp))
            Text(label.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha=0.6f))
            }
        }
    }
}
