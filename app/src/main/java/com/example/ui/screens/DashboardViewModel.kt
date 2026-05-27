package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NotificationRecord
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

enum class TimeFilter(val displayName: String) {
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month"),
    ALL("All Time")
}

class DashboardViewModel(private val repository: NotificationRepository, private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _timeFilter = MutableStateFlow(TimeFilter.ALL)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _briefingText = MutableStateFlow<String?>(null)
    val briefingText: StateFlow<String?> = _briefingText.asStateFlow()

    private val _isGeneratingBriefing = MutableStateFlow(false)
    val isGeneratingBriefing: StateFlow<Boolean> = _isGeneratingBriefing.asStateFlow()

    private val _briefingTone = MutableStateFlow("Friendly")
    val briefingTone: StateFlow<String> = _briefingTone.asStateFlow()

    val notifications: StateFlow<List<NotificationRecord>> = combine(
        repository.allNotifications,
        _timeFilter,
        _searchQuery
    ) { notes, filter, query ->
        val now = System.currentTimeMillis()
        val cutoff = when (filter) {
            TimeFilter.TODAY -> now - 24L * 60 * 60 * 1000L
            TimeFilter.WEEK -> now - 7L * 24 * 60 * 60 * 1000L
            TimeFilter.MONTH -> now - 30L * 24 * 60 * 60 * 1000L
            TimeFilter.ALL -> 0L
        }
        notes.filter { it.timestamp >= cutoff }.filter {
            if (query.isBlank()) true else {
                it.appName.contains(query, ignoreCase = true) || 
                it.title.contains(query, ignoreCase = true) || 
                it.content.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun updateTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setBriefingTone(tone: String) {
        _briefingTone.value = tone
    }

    fun generateBriefing(recentNotifications: List<NotificationRecord>) {
        _isGeneratingBriefing.value = true
        val model = com.example.ai.LocalLLMManager.currentModel.displayName
        val engine = com.example.ai.LocalLLMManager.currentEngine.name
        _briefingText.value = "[Warm-booting $engine Pipeline...]\n[Loading $model context into GGUF Runtime...]\n\n"
        viewModelScope.launch {
            val fullText = com.example.ai.LocalLLMManager.generateBriefing(recentNotifications, _briefingTone.value)
            
            var currentText = ""
            val streamTokens = sequence {
                val words = fullText.split(" ")
                for(word in words) {
                    yield("$word ")
                }
            }
            for (token in streamTokens) {
                currentText += token
                _briefingText.value = currentText
                kotlinx.coroutines.delay((5..15).random().toLong())
            }
            _isGeneratingBriefing.value = false
        }
    }
        
    fun archiveNotification(id: Int) {
        viewModelScope.launch {
            repository.archive(id)
        }
    }
    
    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }
    
    fun restoreAllFromTrash() {
        viewModelScope.launch {
            repository.restoreAllFromTrash()
        }
    }
    
    fun restoreNotification(id: Int) {
        viewModelScope.launch {
            repository.restoreNotification(id)
        }
    }
    
    fun deleteNotification(id: Int) {
        viewModelScope.launch {
            repository.archive(id)
        }
    }
    
    fun toggleStar(id: Int) {
        viewModelScope.launch {
            repository.toggleStar(id)
        }
    }
    
    fun updateCategory(id: Int, category: String) {
        viewModelScope.launch {
            repository.updateCategory(id, category)
        }
    }
    
    fun markSpam(id: Int) {
        viewModelScope.launch {
            repository.markSpam(id)
        }
    }

    fun recordInteraction(id: Int) {
        viewModelScope.launch {
            repository.recordInteraction(id)
        }
    }
    
    fun blockApp(packageName: String) {
        viewModelScope.launch {
            repository.blockApp(packageName)
        }
    }
    
    fun deleteApp(packageName: String) {
        viewModelScope.launch {
            repository.deleteApp(packageName)
        }
    }
}
