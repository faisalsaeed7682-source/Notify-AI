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
    ALL("All Time"),
    RANGE("Date Range")
}

class DashboardViewModel(private val repository: NotificationRepository, private val settingsRepository: SettingsRepository) : ViewModel() {
    val allNotificationsOverall: StateFlow<List<NotificationRecord>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _timeFilter = MutableStateFlow(TimeFilter.TODAY)
    val timeFilter: StateFlow<TimeFilter> = _timeFilter.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _historyAppFilter = MutableStateFlow<String?>(null)
    val historyAppFilter = _historyAppFilter.asStateFlow()

    private val _historyDateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    val historyDateRange = _historyDateRange.asStateFlow()

    init {
        viewModelScope.launch {
            repository.deleteMocks()
        }
    }

    fun updateHistoryAppFilter(app: String?) {
        _historyAppFilter.value = app
    }

    fun updateHistoryDateRange(start: Long?, end: Long?) {
        _historyDateRange.value = start to end
    }

    val historyNotifications: StateFlow<List<NotificationRecord>> = combine(
        repository.allNotifications,
        _timeFilter,
        _historyAppFilter,
        _historyDateRange
    ) { notes, filter, appFilter, range ->
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        
        notes.filter { note ->
            when (filter) {
                TimeFilter.TODAY -> {
                    calendar.timeInMillis = now
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    note.timestamp >= calendar.timeInMillis
                }
                TimeFilter.ALL -> true
                TimeFilter.RANGE -> {
                    val (start, end) = range
                    val afterStart = start == null || note.timestamp >= start
                    val beforeEnd = end == null || note.timestamp <= end
                    afterStart && beforeEnd
                }
            }
        }.filter { note ->
            appFilter == null || note.appName.equals(appFilter, ignoreCase = true) || note.packageName.contains(appFilter, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
 
    private val _isSelectionModeActive = MutableStateFlow(false)
    val isSelectionModeActive: StateFlow<Boolean> = _isSelectionModeActive.asStateFlow()

    private fun parseAndPerformAiSearch(query: String, note: NotificationRecord): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        
        // 1. App search parsing
        var appMatch = true
        val appKeywords = listOf("gmail", "whatsapp", "slack", "outlook", "telegram", "facebook", "instagram", "system")
        for (app in appKeywords) {
            if (q.contains("from $app") || q.contains("app: $app") || q.contains("by $app")) {
                appMatch = note.appName.lowercase().contains(app) || note.packageName.lowercase().contains(app)
                break
            }
        }
        
        // 2. Month parsing
        var monthMatch = true
        val months = mapOf(
            "january" to 0, "jan" to 0,
            "february" to 1, "feb" to 1,
            "march" to 2, "mar" to 2,
            "april" to 3, "apr" to 3,
            "may" to 4,
            "june" to 5, "jun" to 5,
            "july" to 6, "jul" to 6,
            "august" to 7, "aug" to 7,
            "september" to 8, "sep" to 8,
            "october" to 9, "oct" to 9,
            "november" to 10, "nov" to 10,
            "december" to 11, "dec" to 11
        )
        
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = note.timestamp
        val noteMonth = cal.get(java.util.Calendar.MONTH)
        
        for ((mName, mIdx) in months) {
            if (q.contains("in $mName") || q.contains("month: $mName") || (q.contains(mName) && !appKeywords.any { q.contains("from $it") })) {
                monthMatch = (noteMonth == mIdx)
                break
            }
        }
        
        // 3. Flags/Keywords parsing
        var flagMatch = true
        if (q.contains("pinned") || q.contains("pin")) {
            flagMatch = note.isPinned
        } else if (q.contains("starred") || q.contains("star")) {
            flagMatch = note.isStarred
        } else if (q.contains("important") || q.contains("urgent")) {
            flagMatch = note.isImportant || note.priority >= 3
        }
        
        val standardSubstr = note.appName.contains(q, ignoreCase = true) || 
                             note.title.contains(q, ignoreCase = true) || 
                             note.content.contains(q, ignoreCase = true) ||
                             note.category.contains(q, ignoreCase = true)
                             
        val hasSpecificGuiding = q.contains("from ") || q.contains("app:") || q.contains("in ") || q.contains("month:") || q.contains("pinned") || q.contains("starred") || q.contains("important") || q.contains("urgent")
        
        return if (hasSpecificGuiding) {
            appMatch && monthMatch && flagMatch
        } else {
            standardSubstr
        }
    }

    val notifications: StateFlow<List<NotificationRecord>> = combine(
        repository.allNotifications,
        _timeFilter,
        _searchQuery
    ) { notes, filter, query ->
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        
        val filtered = notes.filter {
            val cutoff = when (filter) {
                TimeFilter.TODAY -> {
                    calendar.timeInMillis = now
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    calendar.timeInMillis
                }
                TimeFilter.ALL -> 0L
                TimeFilter.RANGE -> 0L
            }
            it.timestamp >= cutoff
        }.filter {
            parseAndPerformAiSearch(query, it)
        }
        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val trashNotifications = repository.trashNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotifications = repository.archivedNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val swipeLeftAction = settingsRepository.swipeLeftAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val swipeRightAction = settingsRepository.swipeRightAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val starredNotifications = repository.starredNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val importantNotifications = repository.importantNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun updateTimeFilter(filter: TimeFilter) {
        _timeFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun unarchiveNotification(id: Int) {
        viewModelScope.launch {
            repository.unarchive(id)
        }
    }

    fun insertMockNotification(appName: String, packageName: String, title: String, content: String, category: String = "Social") {
        viewModelScope.launch {
            val record = NotificationRecord(
                packageName = packageName,
                appName = appName,
                title = title,
                content = content,
                category = category,
                groupKey = "mock_group",
                notificationKey = "mock_key_${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis()
            )
            repository.insert(record)
        }
    }

    fun archiveNotification(id: Int) {
        viewModelScope.launch {
            repository.archive(id)
        }
    }

    fun togglePin(id: Int) {
        viewModelScope.launch {
            repository.togglePin(id)
        }
    }

    fun toggleImportant(id: Int) {
        viewModelScope.launch {
            repository.toggleImportant(id)
        }
    }

    fun setReminder(id: Int, hours: Int) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis() + hours * 60 * 60 * 1000L
            repository.setReminder(id, timestamp)
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

    fun deletePermanently(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
    
    fun deleteNotification(id: Int) {
        viewModelScope.launch {
            repository.moveToTrash(id)
        }
    }

    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIds: StateFlow<Set<Int>> = _selectedIds.asStateFlow()

    fun toggleSelection(id: Int) {
        _selectedIds.value = if (_selectedIds.value.contains(id)) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
        _isSelectionModeActive.value = true
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
        _isSelectionModeActive.value = false
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { repository.moveToTrash(it) }
            clearSelection()
        }
    }

    fun archiveSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { repository.archive(it) }
            clearSelection()
        }
    }

    fun starSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { repository.toggleStar(it) }
            clearSelection()
        }
    }

    fun selectAll(ids: List<Int>) {
        if (ids.isNotEmpty() && _selectedIds.value.containsAll(ids)) {
            _selectedIds.value = emptySet()
        } else {
            _selectedIds.value = ids.toSet()
        }
        _isSelectionModeActive.value = true
    }

    fun restoreSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { repository.restoreNotification(it) }
            clearSelection()
        }
    }

    fun deletePermanentlySelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { repository.deleteById(it) }
            clearSelection()
        }
    }

    fun unarchiveSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { repository.unarchive(it) }
            clearSelection()
        }
    }

    fun pinSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { repository.togglePin(it) }
            clearSelection()
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

    val forceInAppGmail = settingsRepository.forceInAppGmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val forceInAppWhatsapp = settingsRepository.forceInAppWhatsapp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val forceInAppOutlook = settingsRepository.forceInAppOutlook
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}
