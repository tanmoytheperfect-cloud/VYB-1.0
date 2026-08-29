package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // BUSINESS, TRAVEL, APP, PORTFOLIO, FITNESS
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, PAUSED
    val progressPercent: Int = 0,
    val deadline: String = "",
    val activeTasksCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
