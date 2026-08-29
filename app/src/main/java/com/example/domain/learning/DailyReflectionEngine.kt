package com.example.domain.learning

import com.example.data.local.dao.DailyReflectionDao
import com.example.data.local.dao.TimelineDao
import com.example.data.local.entity.DailyReflectionEntity
import java.text.SimpleDateFormat
import java.util.*

class DailyReflectionEngine(
    private val timelineDao: TimelineDao,
    private val dailyReflectionDao: DailyReflectionDao,
    private val routineLearningEngine: RoutineLearningEngine
) {

    suspend fun performDailyReflection(targetDateString: String? = null): DailyReflectionEntity {
        val dateString = targetDateString ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val todayEvents = timelineDao.getEventsByDate(dateString)

        val cal = Calendar.getInstance()
        val dayOfWeekInt = cal.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
        val isWeekend = dayOfWeekInt == Calendar.SATURDAY || dayOfWeekInt == Calendar.SUNDAY
        val dayType = if (isWeekend) "WEEKEND" else "WORKDAY"

        // Analyze unlock events
        val unlockEvents = todayEvents.filter { it.eventType == "DEVICE_UNLOCK" }
        val firstUnlockTime = unlockEvents.firstOrNull()?.let {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it.timestamp))
        }

        // Analyze last activity
        val lastActivityTime = todayEvents.lastOrNull()?.let {
            SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it.timestamp))
        }

        // App activity
        val appEvents = todayEvents.filter { it.eventType == "APP_OPENED" }
        val topApps = appEvents
            .map { it.contextData.substringBefore(" (").trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key} (${it.value} session${if (it.value > 1) "s" else ""})" }

        val chargingCount = todayEvents.count { it.eventType == "POWER_CONNECTED" || it.eventType == "CHARGING_STARTED" }
        val vybInteractions = todayEvents.count { it.eventType == "VYB_INTERACTION" || it.eventType == "ROUTINE_EXECUTED" }
        val calendarCount = todayEvents.count { it.eventType == "CALENDAR_EVENT" }

        // Inactivity summary
        val sleepEvents = todayEvents.filter { it.eventType == "SLEEP_INACTIVITY_PERIOD" }
        val inactivitySummary = if (sleepEvents.isNotEmpty()) {
            sleepEvents.first().contextData
        } else {
            "Continuous observation active."
        }

        // Trigger routine learning pass to update routines
        val updatedRoutines = routineLearningEngine.analyzeAndLearnRoutines()
        val candidateCount = updatedRoutines.count { it.status == "CANDIDATE_ROUTINE" || it.status == "POSSIBLE_PATTERN" }
        val learnedCount = updatedRoutines.count { it.status == "LEARNED_ROUTINE" }

        val totalEvents = todayEvents.size
        val reflectionText = buildString {
            append("$totalEvents device events observed on $dayOfWeekStr ($dateString). ")
            if (firstUnlockTime != null) {
                append("First device unlock recorded at $firstUnlockTime. ")
            }
            if (unlockEvents.isNotEmpty()) {
                append("${unlockEvents.size} total unlock events logged. ")
            }
            if (topApps.isNotEmpty()) {
                append("Active app sessions: $topApps. ")
            }
            if (chargingCount > 0) {
                append("$chargingCount charging connection event(s) recorded. ")
            }
            append("Learned routines active: $learnedCount ($candidateCount candidates evaluating).")
        }

        val reflection = DailyReflectionEntity(
            dateString = dateString,
            dayOfWeek = dayOfWeekStr,
            dayType = dayType,
            firstUnlockTime = firstUnlockTime,
            lastActivityTime = lastActivityTime,
            activePeriodsSummary = "$totalEvents genuine signals logged across the day",
            longInactivitySummary = inactivitySummary,
            mostUsedAppsSummary = if (topApps.isNotEmpty()) topApps else "No distinct foreground app sessions logged",
            calendarEventsCount = calendarCount,
            vybInteractionsCount = vybInteractions,
            repeatedPatternsIdentified = updatedRoutines.size,
            newCandidateRoutinesCount = candidateCount,
            confidenceUpdatesSummary = "$learnedCount verified routines, $candidateCount candidate patterns updated",
            reflectionSummaryText = reflectionText,
            timestamp = System.currentTimeMillis()
        )

        dailyReflectionDao.insertReflection(reflection)
        return reflection
    }
}
