package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.NotificationRecord
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppDetailsViewModel(
    private val repository: NotificationRepository,
    private val packageName: String
) : ViewModel() {

    val notifications: StateFlow<List<NotificationRecord>> = repository.getNotificationsForApp(packageName)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(500),
            initialValue = emptyList()
        )
        
    fun archiveNotification(id: Int) {
        viewModelScope.launch {
            repository.archive(id)
        }
    }
    
    fun deleteNotification(id: Int) {
        viewModelScope.launch {
            repository.archive(id)
        }
    }
    
    fun markSpam(id: Int) {
        viewModelScope.launch {
            repository.markSpam(id)
        }
    }
    
    fun blockApp() {
        viewModelScope.launch {
            repository.blockApp(packageName)
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
}

class AppDetailsViewModelFactory(
    private val repository: NotificationRepository,
    private val packageName: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AppDetailsViewModel(repository, packageName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
