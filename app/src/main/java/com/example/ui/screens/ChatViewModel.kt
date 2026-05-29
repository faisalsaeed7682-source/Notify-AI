package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.LocalLLMManager
import com.example.data.repository.NotificationRepository
import com.example.data.local.NotificationRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.first

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false,
    val matchedNotifications: List<NotificationRecord>? = null
)

data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    var name: String,
    val messages: List<ChatMessage>
)

class ChatViewModel(private val repository: NotificationRepository, private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    private val _chatHistory = MutableStateFlow<List<ChatSession>>(emptyList())
    val chatHistory: StateFlow<List<ChatSession>> = _chatHistory.asStateFlow()

    private var currentSessionId: String = java.util.UUID.randomUUID().toString()

    init {
        startNewChat()
    }
    
    fun startNewChat() {
        saveCurrentSession()
        currentSessionId = java.util.UUID.randomUUID().toString()
        _messages.value = emptyList()
    }

    private fun saveCurrentSession() {
        if (_messages.value.size > 1 && !_messages.value.all { !it.isUser }) {
            val preview = _messages.value.find { it.isUser }?.text?.take(20)?.plus("...") ?: "New Chat"
            val existing = _chatHistory.value.find { it.id == currentSessionId }
            if (existing != null) {
                _chatHistory.value = _chatHistory.value.map { if (it.id == currentSessionId) it.copy(messages = _messages.value) else it }
            } else {
                _chatHistory.value = listOf(ChatSession(id = currentSessionId, name = preview, messages = _messages.value)) + _chatHistory.value
            }
        }
    }

    fun loadHistorySession(session: ChatSession) {
        saveCurrentSession()
        currentSessionId = session.id
        _messages.value = session.messages
    }

    fun renameSession(id: String, newName: String) {
        _chatHistory.value = _chatHistory.value.map { if (it.id == id) it.copy(name = newName) else it }
    }

    fun deleteSession(id: String) {
        _chatHistory.value = _chatHistory.value.filter { it.id != id }
        if (currentSessionId == id) {
            startNewChat()
        }
    }

    fun deleteHistory() {
        _chatHistory.value = emptyList()
        startNewChat()
    }

    private val _selectedApps = MutableStateFlow<Set<String>>(emptySet())
    val selectedApps: StateFlow<Set<String>> = _selectedApps.asStateFlow()

    val allUniqueApps: StateFlow<List<String>> = repository.allNotifications
        .map { notes -> 
            val dbApps = notes.map { it.appName }.distinct()
            val defaults = listOf("WhatsApp", "Instagram", "Gmail", "System")
            (dbApps + defaults).distinct().sorted()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("WhatsApp", "Instagram", "Gmail", "System"))

    private val _dateRange = MutableStateFlow<Pair<Long?, Long?>>(null to null)
    val dateRange: StateFlow<Pair<Long?, Long?>> = _dateRange.asStateFlow()

    fun updateSelectedApps(apps: Set<String>) {
        _selectedApps.value = apps
    }

    fun updateDateRange(start: Long?, end: Long?) {
        _dateRange.value = start to end
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            val userMsg = ChatMessage(text = text, isUser = true)
            val loadingMsg = ChatMessage(text = "...", isUser = false, isLoading = true)
            
            _messages.value = _messages.value + userMsg + loadingMsg
            
            // Build Context with Filters
            var recentNotifications = repository.allNotifications.firstOrNull() ?: emptyList()
            
            // Apply Filters
            if (_selectedApps.value.isNotEmpty()) {
                recentNotifications = recentNotifications.filter { it.appName in _selectedApps.value }
            }
            
            val (start, end) = _dateRange.value
            if (start != null) {
                recentNotifications = recentNotifications.filter { it.timestamp >= start }
            }
            if (end != null) {
                recentNotifications = recentNotifications.filter { it.timestamp <= end }
            }

            val useCloud = settingsRepository.useCloudAi.first()
            val streamFlow = LocalLLMManager.generateTextStream(text, recentNotifications.take(100), useCloudAi = useCloud)
            
            var partialResponse = ""
            val modelMsgId = java.util.UUID.randomUUID().toString()
            
            val updatedMessages = _messages.value.filter { !it.isLoading }.toMutableList()
            updatedMessages.add(ChatMessage(id = modelMsgId, text = partialResponse, isUser = false))
            _messages.value = updatedMessages
            
            streamFlow.collect { token ->
                partialResponse += token
                val currentMessages = _messages.value.toMutableList()
                val index = currentMessages.indexOfFirst { it.id == modelMsgId }
                if(index != -1) {
                    currentMessages[index] = currentMessages[index].copy(text = partialResponse)
                    _messages.value = currentMessages
                }
            }
            saveCurrentSession()
        }
    }
    
    val activeNotifications: StateFlow<List<NotificationRecord>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreAllNotifications() {
        viewModelScope.launch {
            repository.restoreAllFromTrash()
            val allNotes = repository.allNotifications.first()
            allNotes.filter { it.isArchived }.forEach { repository.unarchive(it.id) }
            _messages.value = _messages.value + ChatMessage(text = "♻️ Restored all notifications from trash and archives back to active history!", isUser = false)
            saveCurrentSession()
        }
    }

    fun deleteAllActiveNotifications() {
        viewModelScope.launch {
            val allActive = repository.allNotifications.first()
            allActive.forEach { repository.moveToTrash(it.id) }
            _messages.value = _messages.value + ChatMessage(text = "🗑️ Cleared all notifications from active feed (moved to Trash).", isUser = false)
            saveCurrentSession()
        }
    }

    fun deleteSelectedNotifications(ids: List<Int>) {
        viewModelScope.launch {
            ids.forEach { repository.moveToTrash(it) }
            _messages.value = _messages.value + ChatMessage(text = "🗑️ Cleared ${ids.size} selected notifications.", isUser = false)
            saveCurrentSession()
        }
    }

    fun queryGuidedAsk(appName: String, month: Int, year: Int, keyword: String) {
        viewModelScope.launch {
            val userMsgText = "AI Guided Search:\n• App: $appName\n• Period: $month/$year\n• Keyword: \"$keyword\""
            val userMsg = ChatMessage(text = userMsgText, isUser = true)
            val loadingMsg = ChatMessage(text = "Retrieving matching notifications from history...", isUser = false, isLoading = true)
            
            _messages.value = _messages.value + userMsg + loadingMsg
            
            val allNotes = repository.fullHistoryStream.first()
            val calendar = java.util.Calendar.getInstance()
            
            val filtered = allNotes.filter { note ->
                val appMatches = note.appName.equals(appName, ignoreCase = true) || note.packageName.contains(appName, ignoreCase = true)
                
                calendar.timeInMillis = note.timestamp
                val noteMonth = calendar.get(java.util.Calendar.MONTH) + 1 // MONTH is 0-indexed
                val noteYear = calendar.get(java.util.Calendar.YEAR)
                val dateMatches = noteMonth == month && noteYear == year
                
                val keywordMatches = note.title.contains(keyword, ignoreCase = true) || 
                                     note.content.contains(keyword, ignoreCase = true) ||
                                     note.appName.contains(keyword, ignoreCase = true) ||
                                     note.category.contains(keyword, ignoreCase = true)
                                     
                appMatches && dateMatches && keywordMatches
            }
            
            val responseText = if (filtered.isEmpty()) {
                "No notifications found in history for $appName in $month/$year containing keyword \"$keyword\"."
            } else {
                "Found ${filtered.size} matching notifications from history for package \"$appName\":"
            }
            
            val updatedMessages = _messages.value.filter { !it.isLoading }.toMutableList()
            updatedMessages.add(ChatMessage(text = responseText, isUser = false, matchedNotifications = filtered))
            _messages.value = updatedMessages
            saveCurrentSession()
        }
    }
    
    fun generateDailySummary() {
        viewModelScope.launch {
            val userMsg = ChatMessage(text = "Summarize my day", isUser = true)
            val loadingMsg = ChatMessage(text = "Analyzing notifications...", isUser = false, isLoading = true)
            
            _messages.value = _messages.value + userMsg + loadingMsg
            
            val recentNotifications = repository.allNotifications.firstOrNull() ?: emptyList()
            val useCloud = settingsRepository.useCloudAi.first()
            val fullResponse = LocalLLMManager.generateBriefing(recentNotifications, useCloudAi = useCloud)
            
            var partialResponse = ""
            val modelMsgId = java.util.UUID.randomUUID().toString()
            val updatedMessages = _messages.value.filter { !it.isLoading }.toMutableList()
            updatedMessages.add(ChatMessage(id = modelMsgId, text = partialResponse, isUser = false))
            _messages.value = updatedMessages
            
            kotlinx.coroutines.delay(400)
            val streamTokens = sequence {
                yield("Initializing local sequence generation...\n\n")
                val words = fullResponse.split(" ")
                for(word in words) {
                    yield("$word ")
                }
            }
            
            for (token in streamTokens) {
                partialResponse += token
                val currentMessages = _messages.value.toMutableList()
                val index = currentMessages.indexOfFirst { it.id == modelMsgId }
                if(index != -1) {
                    currentMessages[index] = currentMessages[index].copy(text = partialResponse)
                    _messages.value = currentMessages
                }
                kotlinx.coroutines.delay((5..15).random().toLong())
            }
            saveCurrentSession()
        }
    }
}
