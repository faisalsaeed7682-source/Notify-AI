package com.example.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppItem(
    val packageName: String,
    val appName: String,
    val isBlocked: Boolean
)

enum class AppFilter {
    ALL, BLOCKED, ENABLED, MOST_ACTIVE
}

class BlockedAppsViewModel(
    private val repository: NotificationRepository,
    context: Context
) : ViewModel() {
    private val packageManager = context.packageManager
    
    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _currentFilter = MutableStateFlow(AppFilter.ALL)
    val currentFilter: StateFlow<AppFilter> = _currentFilter.asStateFlow()
    
    private val blockedAppsFlow = repository.getBlockedApps()
    
    val appsList = combine(_installedApps, blockedAppsFlow, _searchQuery, _currentFilter) { apps, blocked, query, filter ->
        var filteredList = apps.map { app ->
            app.copy(isBlocked = blocked.contains(app.packageName))
        }
        
        filteredList = when (filter) {
            AppFilter.BLOCKED -> filteredList.filter { it.isBlocked }
            AppFilter.ENABLED -> filteredList.filter { !it.isBlocked }
            AppFilter.MOST_ACTIVE -> filteredList.sortedByDescending { it.appName } // Ideally sorted by notification count, but here we just show by name as a functional placeholder unless we add a count query
            else -> filteredList
        }
        
        filteredList.filter {
            query.isEmpty() || it.appName.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }.sortedBy { it.appName }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    init {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .mapNotNull { info ->
                    if (packageManager.getLaunchIntentForPackage(info.packageName) != null) {
                        AppItem(
                            packageName = info.packageName,
                            appName = info.loadLabel(packageManager).toString(),
                            isBlocked = false
                        )
                    } else null
                }.distinctBy { it.packageName }
            _installedApps.value = apps
        }
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun updateFilter(filter: AppFilter) {
        _currentFilter.value = filter
    }
    
    fun toggleBlock(appItem: AppItem) {
        viewModelScope.launch {
            if (appItem.isBlocked) {
                repository.unblockApp(appItem.packageName)
            } else {
                repository.blockApp(appItem.packageName)
            }
        }
    }
}
