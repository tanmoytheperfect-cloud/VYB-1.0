package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * End-of-Day Local Reflection summary.
 * Aggregates actual observed timeline events without leaking raw data.
 */
@Entity(tableName = "daily_reflections")
data class DailyReflectionEntity(
    @PrimaryKey
    val dateString: String, // "YYYY-MM-DD"
    val dayOfWeek: String, // "Monday", "Tuesday", etc.
    val dayType: String = "WORKDAY", // WORKDAY, WEEKEND, HOLIDAY, LEAVE_DAY
    val firstUnlockTime: String? = null,
    val lastActivityTime: String? = null,
    val activePeriodsSummary: String = "",
    val longInactivitySummary: String = "",
    val mostUsedAppsSummary: String = "",
    val calendarEventsCount: Int = 0,
    val vybInteractionsCount: Int = 0,
    val repeatedPatternsIdentified: Int = 0,
    val newCandidateRoutinesCount: Int = 0,
    val confidenceUpdatesSummary: String = "",
    val reflectionSummaryText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
