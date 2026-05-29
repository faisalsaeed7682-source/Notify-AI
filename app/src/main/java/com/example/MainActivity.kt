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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import android.widget.Toast
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import com.example.ui.NavigationApp
import com.example.ui.screens.DashboardViewModelFactory
import com.example.ui.screens.ChatViewModelFactory
import com.example.ui.screens.DashboardViewModel
import com.example.ui.screens.ChatViewModel
import com.example.ui.screens.SettingsRepository
import com.example.ui.screens.SettingsViewModel
import com.example.ui.screens.DiHelper
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : FragmentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle result
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = DiHelper.getRepository(this)
        val settingsRepository = DiHelper.getSettingsRepository(this)
        val settingsViewModel = SettingsViewModel(settingsRepository)
        val dashboardViewModel = ViewModelProvider(this, DashboardViewModelFactory(repository, settingsRepository))[DashboardViewModel::class.java]
        val chatViewModel = ViewModelProvider(this, ChatViewModelFactory(repository, settingsRepository))[ChatViewModel::class.java]

        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

            val isDarkTheme = when(themeMode) {
                1 -> false
                2 -> true
                4 -> true
                else -> isSystemInDarkTheme()
            }
            
            LaunchedEffect(isDarkTheme) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = if (isDarkTheme) {
                    androidx.activity.SystemBarStyle.dark(transparent)
                } else {
                    androidx.activity.SystemBarStyle.light(transparent, transparent)
                }
                enableEdgeToEdge(
                    statusBarStyle = style,
                    navigationBarStyle = style
                )
            }
            
            MyApplicationTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
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
