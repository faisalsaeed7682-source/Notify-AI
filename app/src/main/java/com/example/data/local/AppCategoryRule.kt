package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_category_rules")
data class AppCategoryRule(
    @PrimaryKey val packageName: String,
    val category: String
)
