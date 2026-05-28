package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String,
    val groupKey: String?,
    val isArchived: Boolean = false,
    val isSpam: Boolean = false,
    val isBlocked: Boolean = false,
    val isTrash: Boolean = false,
    val isPinned: Boolean = false,
    val isImportant: Boolean = false,
    val imagePath: String? = null,
    val reminderTimestamp: Long? = null,
    val labelId: String? = null,
    val notificationKey: String? = null,
    val hasReply: Boolean = false,
    val isStarred: Boolean = false,
    val priority: Float = 0.5f,
    val aiCategory: String = "other",
    val primaryReasoning: String = "",
    val interactionCount: Int = 0,
    val importanceScore: Float = 0f
)
