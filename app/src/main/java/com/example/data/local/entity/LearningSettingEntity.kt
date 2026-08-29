package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Learning and Privacy Controls in VYB OS.
 * Allows the owner to pause learning, toggle observation sources, and configure interruption levels.
 */
@Entity(tableName = "learning_settings")
data class LearningSettingEntity(
    @PrimaryKey
    val id: Int = 1,
    val isLearningPaused: Boolean = false,
    val autoReflectAtMidnight: Boolean = true,
    val proactiveInterruptionLevel: String = "BALANCED", // MINIMAL, BALANCED, PROACTIVE
    val allowUsageStatsObservation: Boolean = true,
    val allowCalendarObservation: Boolean = true,
    val allowLocationObservation: Boolean = true,
    val allowNotificationObservation: Boolean = true,
    val allowDeviceLifecycleObservation: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)
