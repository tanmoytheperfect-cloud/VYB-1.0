package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Legitimate Observed Timeline Event in VYB Personal AI OS.
 * Records only authorized, verified Android signals and user interactions.
 */
@Entity(tableName = "timeline_events")
data class TimelineEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // DEVICE_UNLOCK, APP_OPENED, APP_CLOSED, NOTIFICATION_RECEIVED, CALENDAR_EVENT, ALARM_TRIGGERED, LOCATION_CONTEXT, VYB_INTERACTION, SLEEP_INACTIVITY_PERIOD, ROUTINE_EXECUTED
    val source: String, // SystemBroadcast, UsageStatsManager, VybUserSession, CalendarContract, BatteryLifecycle
    val confidence: Float = 1.0f,
    val durationMs: Long = 0L,
    val contextData: String = "", // JSON or descriptive string (e.g. packageName, label, location area)
    val privacyClassification: String = "LOCAL_DEVICE_ONLY", // LOCAL_DEVICE_ONLY, AGGREGATE_ONLY, CONFIDENTIAL
    val dayOfWeek: Int = 1, // java.util.Calendar.SUNDAY..SATURDAY
    val dateString: String = "" // "YYYY-MM-DD"
)
