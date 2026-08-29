package com.example.domain.context

import com.example.data.local.entity.LearnedRoutineEntity
import com.example.data.local.entity.TimelineEventEntity

enum class DayCategory {
    WORKDAY,
    WEEKEND,
    SCHOOL_DAY,
    HOLIDAY,
    LEAVE_DAY
}

enum class HomeScreenMode {
    IDLE,
    MORNING,
    WORK,
    TRAVEL,
    EVENING,
    NIGHT,
    UNKNOWN
}

enum class DeviceState {
    UNLOCKED_ACTIVE,
    INACTIVE_RESTING,
    RECENTLY_UNLOCKED,
    SCREEN_ON
}

enum class InactivityState {
    ACTIVE,
    SHORT_IDLE,
    PROBABLE_SLEEP_RESTING
}

data class VybCurrentContext(
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val timeFormatted: String = "",
    val dateFormatted: String = "",
    val dayOfWeek: String = "",
    val dayCategory: DayCategory = DayCategory.WORKDAY,
    val operatingMode: HomeScreenMode = HomeScreenMode.MORNING,
    val deviceState: DeviceState = DeviceState.UNLOCKED_ACTIVE,
    val inactivityState: InactivityState = InactivityState.ACTIVE,
    val activeRoutine: LearnedRoutineEntity? = null,
    val recentEvents: List<TimelineEventEntity> = emptyList(),
    val recentApps: List<String> = emptyList(),
    val calendarCommitments: List<String> = emptyList(),
    val confidenceScore: Float = 0.9f,
    val contextSummary: String = "",
    val proactiveBriefingSummary: String = ""
)
