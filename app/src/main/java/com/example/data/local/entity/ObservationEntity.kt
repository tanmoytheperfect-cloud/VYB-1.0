package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Structured Observation Model in VYB Personal Cognitive Architecture.
 * Captures real-world authorized behaviors, events, and interactions.
 */
@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey
    val observationId: String,
    val ownerId: String = "primary_owner",
    val timestamp: Long = System.currentTimeMillis(),
    val timezone: String = "Asia/Kolkata",
    val dateString: String = "", // "YYYY-MM-DD"
    val dayOfWeek: Int = 1, // java.util.Calendar.SUNDAY..SATURDAY
    val source: String = "SYSTEM", // SENSOR, USER_ACTION, CONVERSATION, SYSTEM, APP_EVENT
    val domain: String = "GENERAL", // TRANSPORT, EDUCATION, WORK, FINANCE, FAMILY, HABIT, HEALTH
    val subject: String = "", // e.g. "Adhyansh", "Owner", "School Commute"
    val eventType: String = "", // TRANSPORT_BOOKED, SCHOOL_ACTIVITY, CALENDAR_EVENT, WAKE_TIME, LEAVE_HOME, etc.
    val action: String = "",
    val context: String = "",
    val entitiesJson: String = "{}",
    val location: String = "",
    val relatedRoutine: String? = null,
    val relatedPerson: String? = null,
    val confidence: Float = 1.0f,
    val truthState: String = "OBSERVED",
    val observedValue: String = "",
    val metadataJson: String = "{}"
)
