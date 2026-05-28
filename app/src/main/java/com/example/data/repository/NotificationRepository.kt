package com.example.data.repository

import com.example.data.local.NotificationDao
import com.example.data.local.NotificationRecord
import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {
    val allNotifications: Flow<List<NotificationRecord>> = dao.getAllNotifications()
    val trashNotifications: Flow<List<NotificationRecord>> = dao.getTrashNotifications()
    val archivedNotifications: Flow<List<NotificationRecord>> = dao.getArchivedNotifications()
    val starredNotifications: Flow<List<NotificationRecord>> = dao.getStarredNotifications()
    val importantNotifications: Flow<List<NotificationRecord>> = dao.getImportantNotifications()
    val allLabels: Flow<List<com.example.data.local.Label>> = dao.getAllLabels()
    val fullHistoryStream: Flow<List<NotificationRecord>> = dao.getFullHistoryStream()
    
    fun getRecentNotifications(since: Long): Flow<List<NotificationRecord>> = dao.getRecentNotifications(since)

    suspend fun insertLabel(label: com.example.data.local.Label) {
        dao.insertLabel(label)
    }

    suspend fun deleteLabel(id: Int) {
        dao.deleteLabel(id)
    }

    suspend fun setNotificationLabel(id: Int, labelId: String?) {
        dao.setNotificationLabel(id, labelId)
    }

    suspend fun insert(notification: NotificationRecord) {
        dao.insertNotification(notification)
    }

    suspend fun archive(id: Int) {
        dao.archiveNotification(id)
    }

    suspend fun unarchive(id: Int) {
        dao.unarchiveNotification(id)
    }

    fun getNotificationsForApp(packageName: String): Flow<List<NotificationRecord>> = dao.getNotificationsForApp(packageName)

    suspend fun moveToTrash(id: Int) {
        dao.moveToTrash(id)
    }

    suspend fun togglePin(id: Int) {
        dao.togglePin(id)
    }

    suspend fun toggleImportant(id: Int) {
        dao.toggleImportant(id)
    }

    suspend fun setReminder(id: Int, timestamp: Long?) {
        dao.setReminder(id, timestamp)
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
        dao.moveAppNotificationsToTrash(packageName)
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

    suspend fun deleteMocks() {
        dao.deleteMocks()
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

    suspend fun recordInteraction(id: Int) {
        dao.incrementInteraction(id)
    }

    suspend fun updateImportance(id: Int, score: Float) {
        dao.updateImportance(id, score)
    }
}
