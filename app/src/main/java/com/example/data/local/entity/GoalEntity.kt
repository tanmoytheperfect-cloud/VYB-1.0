package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // HEALTH, FINANCE, TRAVEL, CAREER, SHOPPING, FAMILY
    val targetValue: String,
    val currentValue: String,
    val targetDeadline: String,
    val progressPercent: Int = 0,
    val stepsSummary: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
