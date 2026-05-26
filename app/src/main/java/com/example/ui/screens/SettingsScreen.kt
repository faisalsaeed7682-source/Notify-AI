package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.content.Context
import android.os.PowerManager
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel, 
    onNavigateToBlockedApps: () -> Unit,
    onNavigateToAppCategories: () -> Unit,
    onNavigateToTrash: () -> Unit
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isSecure by viewModel.isSecure.collectAsStateWithLifecycle()
    val autoDeleteDays by viewModel.autoDeleteDays.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var isIgnoringBattery by remember { mutableStateOf(powerManager.isIgnoringBatteryOptimizations(context.packageName)) }

    var showAutoDeleteDialog by remember { mutableStateOf(false) }
    val autoDeleteOptions = listOf(0 to "Never", 1 to "1 Day", 7 to "1 Week", 30 to "1 Month", 365 to "1 Year")

    // Local AI Settings States
    val currentModel by com.example.ai.LocalLLMManager.currentModelFlow.collectAsStateWithLifecycle()
    val downloadedModels by com.example.ai.LocalLLMManager.downloadedModels.collectAsStateWithLifecycle()
    val downloadProgress by com.example.ai.LocalLLMManager.downloadProgress.collectAsStateWithLifecycle()
    val isDownloading by com.example.ai.LocalLLMManager.isDownloading.collectAsStateWithLifecycle()
    val consoleLogs by com.example.ai.LocalLLMManager.consoleLogs.collectAsStateWithLifecycle()
    val deviceTemp by com.example.ai.LocalLLMManager.deviceTemperature.collectAsStateWithLifecycle()
    val ramAvailable by com.example.ai.LocalLLMManager.ramAvailable.collectAsStateWithLifecycle()

    var showAiControlCenter by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsItemToggle(
                title = "Dark Mode",
                subtitle = "Toggle dark theme application wide.",
                checked = isDarkMode,
                onCheckedChange = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleDarkMode(it) 
                }
            )
            
            HorizontalDivider()

            val currentAutoDeleteLabel = autoDeleteOptions.find { it.first == autoDeleteDays }?.second ?: "Never"
            Surface(
                onClick = { showAutoDeleteDialog = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Auto Delete Notifications", style = MaterialTheme.typography.titleMedium)
                    Text("Older than: $currentAutoDeleteLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // GGUF Control Center Button (Expandable)
            Surface(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAiControlCenter = !showAiControlCenter 
                },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            Spacer(Modifier.width(8.dp))
                            Text("On-Device GGUF Control Center", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                        }
                        Icon(
                            imageVector = if (showAiControlCenter) Icons.Rounded.Close else Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Configure true on-device llama.cpp hardware execution layers & parameters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
                    
                    if (!showAiControlCenter) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Model: ${currentModel.displayName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("Temp: ${"%.1f".format(deviceTemp)}°C", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("Memory: ${currentModel.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Expanded AI Control Panel
            if (showAiControlCenter) {
                Card(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        // 1. Diagnostics Board
                        Row(
                            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f), RoundedCornerShape(12.dp)).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("DEVICE TEMP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("${"%.1f".format(deviceTemp)}°C", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("AVAILABLE RAM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("${"%.2f".format(ramAvailable)} GB", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("CORES ALLOC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("${com.example.ai.LocalLLMManager.threadsCount.collectAsState().value} / ${com.example.ai.LocalLLMManager.threadAllocationMax} Cores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 2. Hardware acceleration selection
                        Text("1. Hardware Acceleration engine", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        var tempEngine by remember { mutableStateOf(com.example.ai.LocalLLMManager.currentEngine) }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            com.example.ai.LocalLLMManager.EngineType.values().forEach { engine ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            tempEngine = engine
                                            com.example.ai.LocalLLMManager.currentEngine = engine
                                            com.example.ai.LocalLLMManager.addLog("Switching JNI runtime to ${engine.displayName}")
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = tempEngine == engine, onClick = null)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(engine.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("Maps API pointers via JNI native bridge directly into ${engine.apiLib}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // 3. Models weights manager (Download System)
                        Text("2. GGUF weights & models manager", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        com.example.ai.LocalLLMManager.LocalModel.values().forEach { model ->
                            val downloaded = downloadedModels.contains(model.name)
                            val downloading = isDownloading[model.name] == true
                            val progress = downloadProgress[model.name] ?: 0.0f
                            val memoryAlert = !com.example.ai.LocalLLMManager.checkRamCompatibility(model)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (downloaded) {
                                            com.example.ai.LocalLLMManager.currentModel = model
                                            com.example.ai.LocalLLMManager.addLog("Loaded active model pointers: ${model.displayName}")
                                        } else if (!downloading) {
                                            com.example.ai.LocalLLMManager.startModelWeightsDownload(context, model)
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentModel == model) 
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = BorderStroke(
                                    width = if (currentModel == model) 2.dp else 1.dp,
                                    color = if (currentModel == model) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = currentModel == model,
                                                    onClick = {
                                                        if (downloaded) {
                                                            com.example.ai.LocalLLMManager.currentModel = model
                                                            com.example.ai.LocalLLMManager.addLog("Loaded active model pointers: ${model.displayName}")
                                                        }
                                                    },
                                                    enabled = downloaded
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(model.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Text("${model.params} | Size: ${model.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        // Action Button
                                        if (downloaded) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Downloaded", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Ready", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        } else if (downloading) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Button(
                                                onClick = { 
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    com.example.ai.LocalLLMManager.startModelWeightsDownload(context, model) 
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Rounded.CloudDownload, contentDescription = "Download", modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Get GGUF", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    if (downloading) {
                                        Spacer(Modifier.height(8.dp))
                                        Column {
                                            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(4.dp))
                                            Text("Downloading weights layers... ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    if (memoryAlert) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Low RAM (Model requires ${model.minRamRequiredGb}GB minimum)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // 4. Tokenizer & decoding parameters
                        Text("3. Pre-Tuned Tensor Parameters", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        
                        // Threads selection
                        val curThreads by com.example.ai.LocalLLMManager.threadsCount.collectAsStateWithLifecycle()
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Inference Thread Allocation", style = MaterialTheme.typography.bodySmall)
                                Text("$curThreads Cores", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = curThreads.toFloat(),
                                onValueChange = { com.example.ai.LocalLLMManager.threadsCount.value = it.toInt() },
                                valueRange = 1f..com.example.ai.LocalLLMManager.threadAllocationMax.toFloat(),
                                steps = if (com.example.ai.LocalLLMManager.threadAllocationMax > 2) com.example.ai.LocalLLMManager.threadAllocationMax - 2 else 0
                            )
                        }

                        // Temp selection
                        val tempVal by com.example.ai.LocalLLMManager.temperature.collectAsStateWithLifecycle()
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Temperature (Sampling variability)", style = MaterialTheme.typography.bodySmall)
                                Text("Temp: ${"%.2f".format(tempVal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = tempVal,
                                onValueChange = { com.example.ai.LocalLLMManager.temperature.value = it },
                                valueRange = 0.1f..1.5f
                            )
                        }

                        // Top-P Selection
                        val topPVal by com.example.ai.LocalLLMManager.topP.collectAsStateWithLifecycle()
                        Column {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Top-P (Nucleus cutoff value)", style = MaterialTheme.typography.bodySmall)
                                Text("Top-P: ${"%.2f".format(topPVal)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = topPVal,
                                onValueChange = { com.example.ai.LocalLLMManager.topP.value = it },
                                valueRange = 0.1f..1.0f
                            )
                        }

                        // Sliding window context selection
                        val ctxSize by com.example.ai.LocalLLMManager.customContextSize.collectAsStateWithLifecycle()
                        Column {
                            Text("Context Window Limit Size", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                listOf(2048, 4096, 8192).forEach { size ->
                                    FilterChip(
                                        selected = ctxSize == size,
                                        onClick = { com.example.ai.LocalLLMManager.customContextSize.value = size },
                                        label = { Text("$size tokens", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider()

                        // 5. Native Log Diagnostic stdout console
                        Text("4. Local stdout telemetry console", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(Color.Black, RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            val listScrollState = rememberScrollState()
                            LaunchedEffect(consoleLogs.size) {
                                listScrollState.animateScrollTo(listScrollState.maxValue)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(listScrollState),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                consoleLogs.forEach { log ->
                                    Text(
                                        text = log,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (log.contains("error", true)) Color.Red else Color.Green,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showAutoDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showAutoDeleteDialog = false },
                    title = { Text("Auto-Delete Trash") },
                    text = {
                        Column {
                            autoDeleteOptions.forEach { (days, label) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.setAutoDeleteDays(days)
                                            showAutoDeleteDialog = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = autoDeleteDays == days, onClick = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAutoDeleteDialog = false }) { Text("Close") }
                    }
                )
            }
            
            if (!isIgnoringBattery) {
                Surface(
                    onClick = { 
                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        intent.data = Uri.parse("package:${context.packageName}")
                        context.startActivity(intent)
                        isIgnoringBattery = true
                    },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Disable Battery Optimization", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Android may kill the background listener. Tap to allow this app to run reliably.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha=0.8f))
                    }
                }
            }
            
            Surface(
                onClick = { onNavigateToBlockedApps() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Blocked Apps", style = MaterialTheme.typography.titleMedium)
                    Text("Manage apps whose notifications are ignored.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Surface(
                onClick = { onNavigateToAppCategories() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("App Categories", style = MaterialTheme.typography.titleMedium)
                    Text("Manually override categories for installed apps.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Surface(
                onClick = { onNavigateToTrash() },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Trash Bin", style = MaterialTheme.typography.titleMedium)
                    Text("View and restore deleted notifications.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            HorizontalDivider()
            Text("Suggestions & Improvements", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text("This app continuously listens for standard OS broadcasts securely in the background. It groups alerts intelligently and uses an optimized local LLM integration completely on-device to create summaries. Your data never leaves this phone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SettingsItemToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
