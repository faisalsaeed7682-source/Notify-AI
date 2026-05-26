package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE packageName NOT IN (SELECT packageName FROM blocked_apps) AND NOT isSpam AND NOT isArchived ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationRecord>>
    
    @Query("SELECT * FROM notifications WHERE packageName = :packageName AND packageName NOT IN (SELECT packageName FROM blocked_apps) AND NOT isSpam AND NOT isArchived ORDER BY timestamp ASC")
    fun getNotificationsForApp(packageName: String): Flow<List<NotificationRecord>>

    @Query("SELECT * FROM notifications WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getRecentNotifications(since: Long): Flow<List<NotificationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationRecord)

    @Query("UPDATE notifications SET isArchived = 1 WHERE id = :id")
    suspend fun archiveNotification(id: Int)
    
    @Query("UPDATE notifications SET isArchived = 1 WHERE packageName = :packageName")
    suspend fun archiveAllForApp(packageName: String)

    @Query("UPDATE notifications SET isSpam = 1 WHERE id = :id")
    suspend fun markAsSpam(id: Int)

    @Query("UPDATE notifications SET isSpam = 1, isBlocked = 1 WHERE packageName = :packageName")
    suspend fun blockApp(packageName: String)

    @Query("SELECT COUNT(*) FROM blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun isAppBlocked(packageName: String): Int
    
    @Query("UPDATE notifications SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Int, category: String)
    
    @Query("UPDATE notifications SET isSpam = 0, isBlocked = 0 WHERE packageName = :packageName")
    suspend fun unblockApp(packageName: String)
    
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApp(blockedApp: BlockedApp)
    
    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun removeBlockedApp(packageName: String)
    
    @Query("SELECT packageName FROM blocked_apps")
    fun getBlockedApps(): kotlinx.coroutines.flow.Flow<List<String>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppCategoryRule(rule: AppCategoryRule)

    @Query("DELETE FROM app_category_rules WHERE packageName = :packageName")
    suspend fun deleteAppCategoryRule(packageName: String)

    @Query("SELECT category FROM app_category_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getCategoryRule(packageName: String): String?
    
    @Query("SELECT * FROM app_category_rules")
    fun getAllCategoryRules(): kotlinx.coroutines.flow.Flow<List<AppCategoryRule>>

    @Query("UPDATE notifications SET category = :category WHERE packageName = :packageName")
    suspend fun updateCategoryForApp(packageName: String, category: String)

    @Query("DELETE FROM notifications WHERE packageName = :packageName")
    suspend fun deleteAppNotifications(packageName: String)
    
    @Query("UPDATE notifications SET isStarred = CASE WHEN isStarred = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleStar(id: Int)
    
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: Int)
    
    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("DELETE FROM notifications WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
    
    @Query("DELETE FROM notifications WHERE isArchived = 1 OR isSpam = 1 OR category = 'spam'")
    suspend fun emptyTrash()

    @Query("UPDATE notifications SET isArchived = 0, isSpam = 0 WHERE isArchived = 1 OR isSpam = 1 OR category = 'spam'")
    suspend fun restoreAllFromTrash()

    @Query("UPDATE notifications SET isArchived = 0, isSpam = 0 WHERE id = :id")
    suspend fun restoreNotification(id: Int)
}
