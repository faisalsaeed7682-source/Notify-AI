package com.example

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import com.example.ui.NavigationApp
import com.example.ui.screens.DashboardViewModelFactory
import com.example.ui.screens.ChatViewModelFactory
import com.example.ui.screens.DashboardViewModel
import com.example.ui.screens.ChatViewModel
import com.example.ui.screens.SettingsRepository
import com.example.ui.screens.SettingsViewModel
import com.example.ui.screens.DiHelper
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Local AI model weights persistence from storage
        com.example.ai.LocalLLMManager.initialize(this)

        val repository = DiHelper.getRepository(this)
        val settingsRepository = DiHelper.getSettingsRepository(this)
        val settingsViewModel = SettingsViewModel(settingsRepository)
        val dashboardViewModel = ViewModelProvider(this, DashboardViewModelFactory(repository, settingsRepository))[DashboardViewModel::class.java]
        val chatViewModel = ViewModelProvider(this, ChatViewModelFactory(repository, settingsRepository))[ChatViewModel::class.java]

        setContent {
            val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
            
            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    NavigationApp(
                        dashboardViewModel = dashboardViewModel,
                        chatViewModel = chatViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
                
                LaunchedEffect(Unit) {
                    com.example.service.ClassificationWorker.enqueue(this@MainActivity)
                    checkNotificationPermission()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            }
        }
    }
    
    private fun checkNotificationPermission() {
        val cn = ComponentName(this, com.example.service.ClearNotificationListenerService::class.java)
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val isEnabled = enabledListeners?.contains(cn.flattenToString()) == true
        if (!isEnabled) {
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }
}
