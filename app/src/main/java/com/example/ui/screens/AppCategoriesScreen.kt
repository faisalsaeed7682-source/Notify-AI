package com.example.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.rounded.ArrowBack

data class AppCategoryItem(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable,
    val currentCategory: String
)

class AppCategoriesViewModel(private val repository: com.example.data.repository.NotificationRepository, private val context: Context) : ViewModel() {
    private val _allApps = MutableStateFlow<List<AppCategoryItem>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val apps: StateFlow<List<AppCategoryItem>> = _allApps
        .combine(_searchQuery) { apps, query ->
            if (query.isBlank()) apps else apps.filter { it.appName.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val rules = repository.getAllCategoryRules().first().associateBy { it.packageName }
            
            val appList = packages.mapNotNull { appInfo ->
                if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    val rule = rules[appInfo.packageName]
                    AppCategoryItem(appInfo.packageName, appName, icon, rule?.category ?: "other")
                } else null
            }.sortedBy { it.appName }

            _allApps.value = appList
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategory(packageName: String, category: String) {
        viewModelScope.launch {
            repository.insertAppCategoryRule(com.example.data.local.AppCategoryRule(packageName, category))
            repository.updateCategoryForApp(packageName, category)
            // Local update instead of complete reload
            _allApps.value = _allApps.value.map { 
                if (it.packageName == packageName) it.copy(currentCategory = category) else it 
            }
        }
    }

    fun blockApp(packageName: String) {
        viewModelScope.launch {
            repository.blockApp(packageName)
        }
    }

    fun deleteAppNotifications(packageName: String) {
        viewModelScope.launch {
            repository.deleteApp(packageName)
        }
    }
}

class AppCategoriesViewModelFactory(private val repository: com.example.data.repository.NotificationRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppCategoriesViewModel(repository, context) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCategoriesScreen(viewModel: AppCategoriesViewModel, onBack: () -> Unit) {
    val apps by viewModel.apps.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedApp by remember { mutableStateOf<AppCategoryItem?>(null) }
    
    val categories = listOf("msg", "social", "promo", "sys", "spam", "other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Categories") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search apps...") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { selectedApp = app },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = app.icon.toBitmap().asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.appName, fontWeight = FontWeight.Bold)
                            Text("Category: ${app.currentCategory}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        
        if (selectedApp != null) {
            AlertDialog(
                onDismissRequest = { selectedApp = null },
                title = { Text("Manage App: ${selectedApp?.appName}", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Category Selection", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.updateCategory(selectedApp!!.packageName, cat)
                                        selectedApp = null
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedApp?.currentCategory == cat, onClick = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(cat, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text("App Actions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.blockApp(selectedApp!!.packageName)
                                    selectedApp = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Block App")
                            }
                            Button(
                                onClick = {
                                    viewModel.deleteAppNotifications(selectedApp!!.packageName)
                                    selectedApp = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Delete Data")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedApp = null }) { Text("Cancel") }
                }
            )
        }
        }
    }
}
