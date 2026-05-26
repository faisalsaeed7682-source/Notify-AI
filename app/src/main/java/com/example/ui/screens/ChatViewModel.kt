package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.LocalLLMManager
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.first

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isLoading: Boolean = false
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
        _messages.value = listOf(
            ChatMessage(text = "Hello! I am your Notification AI. I can formally analyze your alerts and provide concise summaries. How can I assist you today?", isUser = false)
        )
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

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            val userMsg = ChatMessage(text = text, isUser = true)
            val loadingMsg = ChatMessage(text = "...", isUser = false, isLoading = true)
            
            _messages.value = _messages.value + userMsg + loadingMsg
            
            // Build Context
            val recentNotifications = repository.allNotifications.firstOrNull()?.take(50) ?: emptyList()
            
            val streamFlow = LocalLLMManager.generateTextStream(text, recentNotifications)
            
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
    
    fun generateDailySummary() {
        viewModelScope.launch {
            val userMsg = ChatMessage(text = "Summarize my day", isUser = true)
            val loadingMsg = ChatMessage(text = "Analyzing notifications...", isUser = false, isLoading = true)
            
            _messages.value = _messages.value + userMsg + loadingMsg
            
            val recentNotifications = repository.allNotifications.firstOrNull() ?: emptyList()
            val fullResponse = LocalLLMManager.generateBriefing(recentNotifications)
            
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
