package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_records")
data class ActionRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val actionType: String,
    val levelCode: Int = 1, // 0 = Informational, 1 = Personal, 2 = Communication, 3 = Financial
    val status: String = "PENDING", // PENDING, RUNNING, WAITING_FOR_CONFIRMATION, COMPLETED, FAILED, CANCELLED
    val costInr: Double? = null,
    val consequenceSummary: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
