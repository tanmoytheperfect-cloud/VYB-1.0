package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Genuine Learned Routine model in VYB Personal AI OS.
 * Developed progressively from repeated observations over multiple days.
 */
@Entity(tableName = "learned_routines")
data class LearnedRoutineEntity(
    @PrimaryKey
    val routineId: String, // e.g. "learned_morning_unlock", "learned_whatsapp_after_unlock", "learned_work_start"
    val title: String,
    val description: String,
    val daysOfWeek: String, // "WEEKDAY", "WEEKEND", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    val dayTypeCategory: String = "WORKDAY", // WORKDAY, SCHOOL_DAY, WEEKEND, HOLIDAY, LEAVE_DAY, ANY
    val expectedTimeWindow: String, // e.g. "06:30 AM - 07:15 AM"
    val startMinuteOfDay: Int = 0, // Minutes from midnight (e.g. 390 = 6:30 AM)
    val endMinuteOfDay: Int = 0,
    val confidenceScore: Float = 0.0f, // 0.0 to 1.0
    val evidenceCount: Int = 0, // Number of observed days providing evidence
    val status: String = "OBSERVATION", // OBSERVATION (1), POSSIBLE_PATTERN (2-3), CANDIDATE_ROUTINE (4-7), LEARNED_ROUTINE (8+)
    val lastObservedTimestamp: Long = System.currentTimeMillis(),
    val firstObservedTimestamp: Long = System.currentTimeMillis(),
    val patternCategory: String = "SCREEN_ACTIVITY", // FIRST_PHONE_USE, SCREEN_ACTIVITY, APP_USAGE, CHARGING, SLEEP_INACTIVITY, LOCATION_PATTERN, COMMUTE_PATTERN, CALENDAR_PATTERN, INTERACTION_PATTERN
    val distinctObservationDays: Int = 0,
    val source: String = "DEVICE_OBSERVATION", // Strictly DEVICE_OBSERVATION, never USER_PROVIDED
    val observedDatesSummary: String = "",
    val dayDistributionSummary: String = "",
    val meanMinuteOfDay: Int = 0,
    val timeVarianceMinutes: Float = 0.0f,
    val sourceEventsSummary: String = "",
    val proactiveActionType: String = "INFORM", // DO_NOTHING, INFORM, SUGGEST, PREPARE, ASK_FOR_AUTHORIZATION, EXECUTE_AUTOMATICALLY
    val isActive: Boolean = true,
    val isUserOverridden: Boolean = false
)
