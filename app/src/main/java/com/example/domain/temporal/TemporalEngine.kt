package com.example.domain.temporal

import com.example.domain.brain.LifeRoutine
import com.example.domain.brain.PersonalLifeModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class TimeOfDayCategory(val displayName: String) {
    EARLY_MORNING("Early Morning"),
    MORNING("Morning"),
    MIDDAY("Midday"),
    AFTERNOON("Afternoon"),
    EVENING("Evening"),
    NIGHT("Night"),
    LATE_NIGHT("Late Night")
}

data class TemporalContext(
    val currentTimeMillis: Long,
    val formattedTime: String,
    val formattedDate: String,
    val dayOfWeek: String,
    val isWeekend: Boolean,
    val timeOfDay: TimeOfDayCategory,
    val currentHour: Int,
    val currentMinute: Int,
    val todayDateString: String,
    val tomorrowDateString: String
)

data class RoutineProactiveAssessment(
    val approachingRoutine: LifeRoutine?,
    val minutesUntilRoutine: Int,
    val isActionRequired: Boolean,
    val recommendedAction: String?,
    val reasoningChain: List<String>
)

/**
 * Temporal Engine - Time is a first-class citizen of VYB's brain.
 */
class TemporalEngine {

    fun getCurrentTemporalContext(timestamp: Long = System.currentTimeMillis()): TemporalContext {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val dayOfWeekInt = cal.get(Calendar.DAY_OF_WEEK)

        val dayOfWeek = when (dayOfWeekInt) {
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

        val timeCategory = when (hour) {
            in 5..7 -> TimeOfDayCategory.EARLY_MORNING
            in 8..11 -> TimeOfDayCategory.MORNING
            in 12..13 -> TimeOfDayCategory.MIDDAY
            in 14..17 -> TimeOfDayCategory.AFTERNOON
            in 18..21 -> TimeOfDayCategory.EVENING
            in 22..23 -> TimeOfDayCategory.NIGHT
            else -> TimeOfDayCategory.LATE_NIGHT
        }

        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
        val dateFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))

        val calTomorrow = Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val tomorrowStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calTomorrow.time)

        return TemporalContext(
            currentTimeMillis = timestamp,
            formattedTime = timeFmt,
            formattedDate = dateFmt,
            dayOfWeek = dayOfWeek,
            isWeekend = isWeekend,
            timeOfDay = timeCategory,
            currentHour = hour,
            currentMinute = minute,
            todayDateString = todayStr,
            tomorrowDateString = tomorrowStr
        )
    }

    /**
     * Evaluates whether an important recurring routine is approaching and requires action.
     * E.g. Son's school commute approaching at 10:30 AM or 8:30 AM.
     */
    fun evaluateApproachingRoutines(
        lifeModel: PersonalLifeModel,
        currentTimestamp: Long = System.currentTimeMillis()
    ): RoutineProactiveAssessment {
        val temporal = getCurrentTemporalContext(currentTimestamp)
        val currentMinutesOfDay = temporal.currentHour * 60 + temporal.currentMinute

        val routines = lifeModel.dailyRoutines.value
        val reasoning = mutableListOf<String>()

        // Check each routine
        for (routine in routines) {
            val routineMinutes = parseRoutineTimeToMinutes(routine.timeFormatted)
            if (routineMinutes != null) {
                val diffMinutes = routineMinutes - currentMinutesOfDay

                // If routine is within the next 2 hours and hasn't passed
                if (diffMinutes in 5..120) {
                    reasoning.add("Current time is ${temporal.formattedTime} on ${temporal.dayOfWeek}")
                    reasoning.add("Approaching routine: '${routine.title}' scheduled for ${routine.timeFormatted} (in $diffMinutes mins)")

                    val actionRequired = routine.title.contains("Commute", ignoreCase = true) ||
                            routine.title.contains("Cab", ignoreCase = true) ||
                            routine.title.contains("School", ignoreCase = true)

                    if (actionRequired) {
                        reasoning.add("Action requirement: Transport needs to be coordinated before departure")
                        reasoning.add("No external cab booking confirmation detected yet")
                        reasoning.add("Recommendation: Compare available ride providers (Yatri Sathi / Rapido / Uber) and prepare dispatch")
                    } else {
                        reasoning.add("Routine requires review of planned actions")
                    }

                    return RoutineProactiveAssessment(
                        approachingRoutine = routine,
                        minutesUntilRoutine = diffMinutes,
                        isActionRequired = actionRequired,
                        recommendedAction = if (actionRequired) "Coordinate ${routine.title}" else "Review ${routine.title}",
                        reasoningChain = reasoning
                    )
                }
            }
        }

        // Default calm state
        return RoutineProactiveAssessment(
            approachingRoutine = null,
            minutesUntilRoutine = -1,
            isActionRequired = false,
            recommendedAction = null,
            reasoningChain = listOf("Current time: ${temporal.formattedTime}. No imminent routines requiring intervention.")
        )
    }

    private fun parseRoutineTimeToMinutes(timeStr: String): Int? {
        val clean = timeStr.trim().uppercase(Locale.ROOT)
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")
        val digits = clean.replace("AM", "").replace("PM", "").trim()
        val parts = digits.split(":")
        if (parts.isEmpty()) return null

        var hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0

        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0

        return hour * 60 + minute
    }
}
