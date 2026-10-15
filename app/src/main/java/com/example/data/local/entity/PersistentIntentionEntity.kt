package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistent Intention Entity (Room Database).
 * Represents a durable personal intention that survives process death and phone reboot.
 * Room is the single Source of Truth; Android scheduler is the execution mechanism.
 */
@Entity(tableName = "persistent_intentions")
data class PersistentIntentionEntity(
    @PrimaryKey
    val id: String, // e.g. "VYB-INT-2026-XXXX"
    val ownerId: String = "primary_owner",
    val rawRequest: String, // Natural language request: e.g. "Wake me tomorrow at 7:30 AM"
    val normalizedIntent: String, // "ALARM", "REMINDER", "ROUTINE", "CAB", "FINANCE", "COMMUNICATION", "PROACTIVE"
    val scheduledTimestamp: Long, // Target epoch millis when Android should wake VYB
    val recurrenceRule: String = "NONE", // "NONE", "DAILY", "WEEKDAYS", "WEEKENDS", "WEEKLY", "SPLIT_WEEKDAY_WEEKEND"
    val timeOfDay: String = "07:30", // Target time HH:mm
    val timezone: String = "Asia/Kolkata",
    val createdAt: Long = System.currentTimeMillis(),
    val executionState: String = "SCHEDULED", // "PENDING", "SCHEDULED", "EXECUTING", "COMPLETED", "FAILED", "MISSED", "CANCELLED"
    val autonomyLevel: Int = 3,
    val requiredCapability: String = "REMINDER",
    val requiredPermission: String? = null,
    val isConfirmed: Boolean = true,
    val actionPlanJson: String? = null,
    val lastExecutionTimestamp: Long? = null,
    val lastExecutionResult: String? = null,
    val lastVerificationProof: String? = null,
    val staleThresholdMinutes: Int = 120, // Threshold after which a missed wake is considered stale
    val isStale: Boolean = false,
    val retryCount: Int = 0,
    val notificationTitle: String = "VYB Reminder",
    val notificationBody: String = "Scheduled execution ready",
    val lastEventId: String? = null,
    val destination: String? = null,
    val serviceTarget: String? = null
)
