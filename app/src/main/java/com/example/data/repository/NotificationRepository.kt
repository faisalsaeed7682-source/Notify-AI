package com.example.data.repository

import com.example.data.local.NotificationDao
import com.example.data.local.NotificationRecord
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {
    val allNotifications: Flow<List<NotificationRecord>> = dao.getAllNotifications()
    
    fun getNotificationsForApp(packageName: String): Flow<List<NotificationRecord>> = dao.getNotificationsForApp(packageName)

    fun getRecentNotifications(since: Long): Flow<List<NotificationRecord>> = dao.getRecentNotifications(since)

    suspend fun insert(notification: NotificationRecord) {
        dao.insertNotification(notification)
    }

    suspend fun archive(id: Int) {
        dao.archiveNotification(id)
    }

    suspend fun archiveApp(packageName: String) {
        dao.archiveAllForApp(packageName)
    }

    suspend fun blockApp(packageName: String) {
        dao.insertBlockedApp(com.example.data.local.BlockedApp(packageName))
        dao.blockApp(packageName)
    }

    suspend fun unblockApp(packageName: String) {
        dao.removeBlockedApp(packageName)
        dao.unblockApp(packageName)
    }
    
    suspend fun deleteApp(packageName: String) {
        dao.deleteAppNotifications(packageName)
    }

    fun getBlockedApps(): Flow<List<String>> = dao.getBlockedApps()

    suspend fun markSpam(id: Int) {
        dao.markAsSpam(id)
    }

    suspend fun toggleStar(id: Int) {
        dao.toggleStar(id)
    }

    suspend fun updateCategory(id: Int, category: String) {
        dao.updateCategory(id, category)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteNotificationById(id)
    }
    
    suspend fun clearAll() {
        dao.clearAll()
    }

    suspend fun emptyTrash() {
        dao.emptyTrash()
    }
    
    suspend fun restoreAllFromTrash() {
        dao.restoreAllFromTrash()
    }
    
    suspend fun restoreNotification(id: Int) {
        dao.restoreNotification(id)
    }

    suspend fun deleteOlderThan(cutoff: Long) {
        dao.deleteOlderThan(cutoff)
    }

    suspend fun insertAppCategoryRule(rule: com.example.data.local.AppCategoryRule) {
        dao.insertAppCategoryRule(rule)
    }

    suspend fun updateCategoryForApp(packageName: String, category: String) {
        dao.updateCategoryForApp(packageName, category)
    }

    fun getAllCategoryRules(): Flow<List<com.example.data.local.AppCategoryRule>> = dao.getAllCategoryRules()
}
