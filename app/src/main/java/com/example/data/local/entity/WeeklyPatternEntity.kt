package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 7-Day Routine Shift and Weekly Pattern Analysis Entity.
 * Distinguishes weekday, Saturday, and Sunday behavior models with evidence.
 */
@Entity(tableName = "weekly_patterns")
data class WeeklyPatternEntity(
    @PrimaryKey
    val weekIdentifier: String, // e.g. "2026-W35"
    val startDateString: String,
    val endDateString: String,
    val stableRoutinesSummary: String = "",
    val changingRoutinesSummary: String = "",
    val weekdayPatternSummary: String = "",
    val saturdayPatternSummary: String = "",
    val sundayPatternSummary: String = "",
    val unusualDaysSummary: String = "",
    val brokenRoutinesSummary: String = "",
    val weeklyInsightText: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
