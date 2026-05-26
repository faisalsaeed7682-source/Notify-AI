package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.local.AppDatabase
import com.example.data.local.NotificationRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class ClearNotificationListenerService : NotificationListenerService() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onListenerConnected() {
        super.onListenerConnected()
        showPersistentNotification()
        
        scope.launch {
            try {
                val active = activeNotifications
                if (active != null) {
                    for (sbn in active) {
                        onNotificationPosted(sbn)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotifyListener", "Error syncing active", e)
            }
        }
    }

    private fun showPersistentNotification() {
        val channelId = "clearnotify_status"
        val channel = NotificationChannel(channelId, "ClearNotify Active", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows that ClearNotify is active in the background"
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ClearNotify is Active")
            .setContentText("Listening and securely storing notifications.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
            
        manager.notify(1001, notification)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val flags = sbn.notification.flags
        
        // Ignore accidental music/media, ongoing events (like USB connected), and system UI
        val isMedia = sbn.notification.extras.getString("android.template")?.contains("MediaStyle") == true
        val isOngoing = (flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        val category = sbn.notification.category ?: "other"
        
        if (isMedia || isOngoing || category == "media" || category == "transport" || category == "progress" || packageName.contains("spotify") || packageName == "android" || packageName == "com.android.systemui") {
            Log.d("NotifyListener", "Ignoring non-message notification from $packageName")
            return
        }

        val appName = applicationContext.packageManager.getApplicationInfo(packageName, 0)
            .loadLabel(applicationContext.packageManager).toString()

        val title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: ""
        val content = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""
        
        if (title.isBlank() && content.isBlank()) return

        val groupKey = sbn.groupKey
        val notificationKey = sbn.key
        
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val isBlocked = db.notificationDao().isAppBlocked(packageName) > 0
                if (isBlocked) {
                    Log.d("NotifyListener", "Ignoring blocked app: $packageName")
                    return@launch
                }
                
                var hasReply = false
        sbn.notification.actions?.forEach { action ->
            if (action.remoteInputs?.isNotEmpty() == true) {
                hasReply = true
                ReplyCache.replies[sbn.key] = action
            }
        }
        
        var imagePath: String? = null
        val extras = sbn.notification.extras
        val bitmap = extras.getParcelable("android.picture") as? android.graphics.Bitmap 
            ?: extras.getParcelable("android.largeIcon") as? android.graphics.Bitmap
            
        if (bitmap != null) {
            try {
                val file = java.io.File(cacheDir, "notif_images")
                if (!file.exists()) file.mkdirs()
                val imgFile = java.io.File(file, "img_${System.currentTimeMillis()}.png")
                java.io.FileOutputStream(imgFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                imagePath = imgFile.absolutePath
            } catch (e: Exception) {
                Log.e("NotifyListener", "Failed to save image", e)
            }
        }
        
                val manualCategory = db.notificationDao().getCategoryRule(packageName)
                val aiCategory = if (manualCategory == null) {
                    com.example.ai.LocalLLMManager.categorizeNotification(appName, title, content)
                } else { "" }

                val finalCategory = manualCategory ?: if (aiCategory == "other" || aiCategory.isEmpty()) category else aiCategory
                val isSpamFlag = aiCategory == "spam"

                val record = NotificationRecord(
                    packageName = packageName,
                    appName = appName,
                    title = title,
                    content = content,
                    category = finalCategory,
                    groupKey = groupKey,
                    imagePath = imagePath,
                    notificationKey = notificationKey,
                    hasReply = hasReply,
                    isSpam = isSpamFlag,
                    isArchived = isSpamFlag // Automatically archive if it's spam
                )
                db.notificationDao().insertNotification(record)
                Log.d("NotifyListener", "Saved notification: $appName - $title (Category: ${record.category})")
                
                // Cleanup old notifications
                try {
                    val settingsRepo = com.example.ui.screens.SettingsRepository(this@ClearNotificationListenerService)
                    val days = settingsRepo.autoDeleteDays.first()
                    if (days > 0) {
                        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000L
                        db.notificationDao().deleteOlderThan(cutoff)
                    }
                } catch (e: Exception) {
                    Log.e("NotifyListener", "Failed cleanup", e)
                }

            } catch (e: Exception) {
                Log.e("NotifyListener", "Failed to save notification", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
