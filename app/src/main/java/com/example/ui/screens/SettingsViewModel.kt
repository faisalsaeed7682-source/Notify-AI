package com.example.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = settingsRepository.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
        
    val isSecure: StateFlow<Boolean> = settingsRepository.isSecure
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val autoDeleteDays: StateFlow<Int> = settingsRepository.autoDeleteDays
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(enabled)
        }
    }
    
    fun toggleSecure(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSecure(enabled)
        }
    }

    fun setAutoDeleteDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setAutoDeleteDays(days)
        }
    }
}
