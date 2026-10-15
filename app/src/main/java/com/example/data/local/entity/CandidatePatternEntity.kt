package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Candidate and Established Pattern Entity.
 * Represents hypotheses and discovered recurring behaviors with evidence-backed confidence.
 */
@Entity(tableName = "candidate_patterns")
data class CandidatePatternEntity(
    @PrimaryKey
    val patternId: String,
    val ownerId: String = "primary_owner",
    val domain: String = "GENERAL",
    val subject: String = "",
    val title: String,
    val description: String,
    val evidenceCount: Int = 0,
    val supportingObservationIdsJson: String = "[]",
    val contradictoryObservationIdsJson: String = "[]",
    val confidence: Float = 0.0f,
    val firstObserved: Long = System.currentTimeMillis(),
    val lastObserved: Long = System.currentTimeMillis(),
    val recurrence: String = "WEEKDAY", // "WEEKDAY", "WEEKEND", "DAILY", "CUSTOM", "NONE"
    val expectedDaysJson: String = "[2,3,4,5,6]", // Calendar.MONDAY..FRIDAY
    val expectedTimeRange: String = "08:30 AM",
    val expectedContext: String = "",
    val exceptionsJson: String = "[]",
    val status: String = "CANDIDATE", // CANDIDATE, EMERGING, ESTABLISHED, CONTRADICTED, STALE, RETIRED
    val explanation: String = "",
    val truthState: String = "LEARNED", // LEARNED, INFERRED, USER_CONFIRMED
    val isUserConfirmed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
