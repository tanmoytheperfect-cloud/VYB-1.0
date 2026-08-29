package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timeOfDay: String, // e.g. "08:30 AM"
    val daysOfWeek: String, // e.g. "Mon-Fri"
    val description: String,
    val actionPrompt: String,
    val estimatedCostInr: Double = 0.0,
    val isEnabled: Boolean = true,
    val isAutoBookingAllowed: Boolean = false,
    val category: String = "TRANSPORT" // TRANSPORT, SCHOOL, WORK, HEALTH, FINANCE
)
