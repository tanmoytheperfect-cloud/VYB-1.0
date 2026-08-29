package com.example.domain.context

import com.example.data.local.dao.LearnedRoutineDao
import com.example.data.local.dao.TimelineDao
import com.example.data.local.entity.LearnedRoutineEntity
import com.example.data.local.entity.TimelineEventEntity
import java.text.SimpleDateFormat
import java.util.*

class CurrentContextEngine(
    private val timelineDao: TimelineDao,
    private val learnedRoutineDao: LearnedRoutineDao
) {

    suspend fun evaluateCurrentContext(
        calendarEvents: List<String> = emptyList(),
        currentTimestamp: Long = System.currentTimeMillis()
    ): VybCurrentContext {
        val cal = Calendar.getInstance().apply { timeInMillis = currentTimestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val currentMinuteOfDay = hour * 60 + minute
        val dayOfWeekInt = cal.get(Calendar.DAY_OF_WEEK)

        val dayOfWeekStr = when (dayOfWeekInt) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Today"
        }

        val isWeekend = dayOfWeekInt == Calendar.SATURDAY || dayOfWeekInt == Calendar.SUNDAY
        val dayCategory = if (isWeekend) DayCategory.WEEKEND else DayCategory.WORKDAY

        val timeFormatted = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(currentTimestamp))
        val dateFormatted = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(currentTimestamp))
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(currentTimestamp))

        // Query today's recent timeline events
        val todayEvents = timelineDao.getEventsByDate(dateString)
        val recentEvents = todayEvents.takeLast(10).reversed()

        // Extract recent apps
        val recentApps = todayEvents
            .filter { it.eventType == "APP_OPENED" }
            .map { it.contextData.substringBefore(" (") }
            .distinct()
            .takeLast(4)

        // Find active matching routine for current minute of day
        val dayTag = when (dayOfWeekInt) {
            Calendar.SATURDAY -> "SATURDAY"
            Calendar.SUNDAY -> "SUNDAY"
            else -> "WEEKDAY"
        }
        val candidateRoutines = learnedRoutineDao.getRoutinesForDay(dayTag)
        val activeRoutine = candidateRoutines.firstOrNull { routine ->
            routine.isActive && currentMinuteOfDay in (routine.startMinuteOfDay - 15)..(routine.endMinuteOfDay + 15)
        }

        // Determine Operating Mode
        val operatingMode = when {
            hour in 5..10 -> HomeScreenMode.MORNING
            hour in 11..17 && !isWeekend -> HomeScreenMode.WORK
            hour in 17..19 -> HomeScreenMode.TRAVEL
            hour in 20..22 -> HomeScreenMode.EVENING
            hour in 23..24 || hour in 0..4 -> HomeScreenMode.NIGHT
            else -> HomeScreenMode.IDLE
        }

        // Inactivity / sleep state
        val lastEvent = todayEvents.lastOrNull()
        val inactivityState = when {
            lastEvent == null && hour in 23..24 || hour in 0..5 -> InactivityState.PROBABLE_SLEEP_RESTING
            lastEvent != null && (currentTimestamp - lastEvent.timestamp) > 3 * 3600 * 1000L && hour in 22..24 || hour in 0..6 -> InactivityState.PROBABLE_SLEEP_RESTING
            lastEvent != null && (currentTimestamp - lastEvent.timestamp) > 30 * 60 * 1000L -> InactivityState.SHORT_IDLE
            else -> InactivityState.ACTIVE
        }

        // Proactive Briefing summary formulation
        val proactiveSummary = buildProactiveSummary(
            hour = hour,
            dayOfWeekStr = dayOfWeekStr,
            isWeekend = isWeekend,
            activeRoutine = activeRoutine,
            calendarEvents = calendarEvents,
            todayEventCount = todayEvents.size
        )

        val contextSummary = "$dayOfWeekStr $timeFormatted • Mode: $operatingMode • Day Type: $dayCategory"

        return VybCurrentContext(
            currentTimeMillis = currentTimestamp,
            timeFormatted = timeFormatted,
            dateFormatted = dateFormatted,
            dayOfWeek = dayOfWeekStr,
            dayCategory = dayCategory,
            operatingMode = operatingMode,
            deviceState = DeviceState.UNLOCKED_ACTIVE,
            inactivityState = inactivityState,
            activeRoutine = activeRoutine,
            recentEvents = recentEvents,
            recentApps = recentApps,
            calendarCommitments = calendarEvents,
            confidenceScore = 0.92f,
            contextSummary = contextSummary,
            proactiveBriefingSummary = proactiveSummary
        )
    }

    private fun buildProactiveSummary(
        hour: Int,
        dayOfWeekStr: String,
        isWeekend: Boolean,
        activeRoutine: LearnedRoutineEntity?,
        calendarEvents: List<String>,
        todayEventCount: Int
    ): String {
        return when {
            hour in 5..10 -> {
                if (!isWeekend) {
                    val routineText = activeRoutine?.let { "Active routine: ${it.title}." } ?: "Morning sequence active."
                    val calText = if (calendarEvents.isNotEmpty()) "Calendar: ${calendarEvents.size} event(s) today." else "Calendar: No early conflicting meetings."
                    "$dayOfWeekStr Morning. $routineText $calText"
                } else {
                    "$dayOfWeekStr Morning. Weekend routine active. No work commitments observed."
                }
            }
            hour in 11..17 -> {
                if (!isWeekend) {
                    val calText = if (calendarEvents.isNotEmpty()) "${calendarEvents.first()}." else "Focused work block."
                    "Workday context active. $calText"
                } else {
                    "Weekend afternoon personal context active."
                }
            }
            hour in 18..21 -> {
                "Evening wind-down & family context. Day observations: $todayEventCount signals logged."
            }
            else -> {
                "Night quiet mode. Low interruption threshold."
            }
        }
    }
}
