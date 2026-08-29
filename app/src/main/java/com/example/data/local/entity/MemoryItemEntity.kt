package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // PREFERENCE, PROFILE, HABIT, FINANCIAL, FAMILY, TRAVEL, STYLE
    val key: String,
    val value: String,
    val source: String = "User Conversation",
    val confidence: Double = 0.95,
    val isEditable: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
