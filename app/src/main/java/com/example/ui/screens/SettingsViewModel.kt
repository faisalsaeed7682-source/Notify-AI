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
        
    val useCloudAi: StateFlow<Boolean> = settingsRepository.useCloudAi
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
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
    
    fun toggleUseCloudAi(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setUseCloudAi(enabled)
        }
    }

    val themeMode: StateFlow<Int> = settingsRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun setThemeMode(mode: Int) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    val swipeLeftAction = settingsRepository.swipeLeftAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val swipeRightAction = settingsRepository.swipeRightAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    fun setSwipeLeftAction(action: Int) {
        viewModelScope.launch { settingsRepository.setSwipeLeftAction(action) }
    }

    fun setSwipeRightAction(action: Int) {
        viewModelScope.launch { settingsRepository.setSwipeRightAction(action) }
    }

    fun setAutoDeleteDays(days: Int) {
        viewModelScope.launch {
            settingsRepository.setAutoDeleteDays(days)
        }
    }

    val forceInAppGmail: StateFlow<Boolean> = settingsRepository.forceInAppGmail
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val forceInAppWhatsapp: StateFlow<Boolean> = settingsRepository.forceInAppWhatsapp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val forceInAppOutlook: StateFlow<Boolean> = settingsRepository.forceInAppOutlook
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleForceInAppGmail(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setForceInAppGmail(enabled) }
    }

    fun toggleForceInAppWhatsapp(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setForceInAppWhatsapp(enabled) }
    }

    fun toggleForceInAppOutlook(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setForceInAppOutlook(enabled) }
    }
}
