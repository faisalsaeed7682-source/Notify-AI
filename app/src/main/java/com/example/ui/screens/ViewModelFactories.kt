package com.example.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.NotificationRepository

object DiHelper {
    private var repository: NotificationRepository? = null
    private var settingsRepository: SettingsRepository? = null

    fun getRepository(context: Context): NotificationRepository {
        return repository ?: synchronized(this) {
            val db = AppDatabase.getDatabase(context.applicationContext)
            val repo = NotificationRepository(db.notificationDao())
            repository = repo
            repo
        }
    }
    
    fun getSettingsRepository(context: Context): SettingsRepository {
        return settingsRepository ?: synchronized(this) {
            val repo = SettingsRepository(context.applicationContext)
            settingsRepository = repo
            repo
        }
    }
}

class DashboardViewModelFactory(private val repository: NotificationRepository, private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class BlockedAppsViewModelFactory(private val repository: NotificationRepository, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BlockedAppsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BlockedAppsViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ChatViewModelFactory(private val repository: NotificationRepository, private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
