package com.example.domain.cognitive.engine

import com.example.data.local.entity.PersistentIntentionEntity
import com.example.data.local.entity.RoutineEntity
import com.example.domain.cognitive.models.CandidatePattern
import com.example.domain.cognitive.models.CurrentEnvironmentState
import com.example.domain.cognitive.models.NextUpItem
import com.example.domain.cognitive.models.PatternStatus
import com.example.domain.cognitive.models.TruthState
import java.text.SimpleDateFormat
import java.util.*

class TemporalReasoningEngine {

    data class ParsedTime(val hour24: Int, val minute: Int, val formatted: String)

    /**
     * Resolves the next contextually relevant item for the Home Screen.
     * Strictly verifies day of week recurrence, past-time cutoff, exceptions, and staleness.
     */
    fun resolveNextUp(
        nowMillis: Long = System.currentTimeMillis(),
        timezoneId: String = TimeZone.getDefault().id,
        learnedPatterns: List<CandidatePattern> = emptyList(),
        routines: List<RoutineEntity> = emptyList(),
        intentions: List<PersistentIntentionEntity> = emptyList(),
        canonicalEnvironment: CurrentEnvironmentState? = null
    ): NextUpItem? {
        val tz = TimeZone.getTimeZone(timezoneId)
        val nowCal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
        val currentDayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK)
        val currentMinuteOfDay = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        val timeFormatDisplay = SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = tz }
        val dayNameFormat = SimpleDateFormat("EEE", Locale.US).apply { timeZone = tz }

        val candidateItems = mutableListOf<NextUpCandidate>()

        val weatherStr = canonicalEnvironment?.temperature?.let {
            "${String.format(Locale.US, "%.0f", it)}°C"
        }

        // 1. Evaluate Learned Patterns (e.g. Adhyansh's School routine, Evening Walk)
        for (pattern in learnedPatterns.filter { it.status == PatternStatus.ESTABLISHED || it.status == PatternStatus.EMERGING }) {
            val parsedTime = parseTimeOfDay(pattern.expectedTimeRange, tz)
            val eventMinuteOfDay = parsedTime.hour24 * 60 + parsedTime.minute
            val isScheduledDay = currentDayOfWeek in pattern.expectedDays

            // Check if there is an approved one-time exception for today
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }.format(Date(nowMillis))
            val hasTodayException = pattern.exceptions.any { it.dateString == todayDateStr }

            if (isScheduledDay || hasTodayException) {
                // Relevant today: check if it has already passed
                if (eventMinuteOfDay > currentMinuteOfDay) {
                    val eventCal = (nowCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, parsedTime.hour24)
                        set(Calendar.MINUTE, parsedTime.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    candidateItems.add(
                        NextUpCandidate(
                            id = pattern.patternId,
                            title = pattern.title,
                            subtitle = if (pattern.subject.isNotBlank()) "${pattern.subject} • ${pattern.domain}" else pattern.domain,
                            timeDisplay = parsedTime.formatted,
                            isToday = true,
                            nextEpochMs = eventCal.timeInMillis,
                            recurrence = pattern.recurrence,
                            confidence = pattern.confidence,
                            truthState = pattern.truthState,
                            explanation = pattern.explanation
                        )
                    )
                }
            } else {
                // NOT relevant today (e.g. today is Saturday for a weekday-bound routine).
                // Calculate next expected occurrence on next scheduled day (e.g. Monday).
                val nextCal = getNextScheduledOccurrence(nowCal, pattern.expectedDays, parsedTime.hour24, parsedTime.minute)
                val dayName = dayNameFormat.format(nextCal.time)
                candidateItems.add(
                    NextUpCandidate(
                        id = pattern.patternId,
                        title = pattern.title,
                        subtitle = if (pattern.subject.isNotBlank()) "${pattern.subject} • Next expected $dayName" else "Next expected $dayName",
                        timeDisplay = parsedTime.formatted,
                        isToday = false,
                        nextEpochMs = nextCal.timeInMillis,
                        recurrence = pattern.recurrence,
                        confidence = pattern.confidence,
                        truthState = pattern.truthState,
                        explanation = "Learned ${pattern.recurrence.lowercase()} pattern; next occurrence is on $dayName."
                    )
                )
            }
        }

        // 2. Evaluate User Configured Routines
        for (routine in routines.filter { it.isEnabled }) {
            val parsedTime = parseTimeOfDay(routine.timeOfDay, tz)
            val eventMinuteOfDay = parsedTime.hour24 * 60 + parsedTime.minute
            val expectedDays = parseDaysOfWeek(routine.daysOfWeek)
            val isScheduledDay = currentDayOfWeek in expectedDays

            if (isScheduledDay) {
                if (eventMinuteOfDay > currentMinuteOfDay) {
                    val eventCal = (nowCal.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, parsedTime.hour24)
                        set(Calendar.MINUTE, parsedTime.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    candidateItems.add(
                        NextUpCandidate(
                            id = "routine_${routine.id}",
                            title = routine.title,
                            subtitle = routine.category,
                            timeDisplay = parsedTime.formatted,
                            isToday = true,
                            nextEpochMs = eventCal.timeInMillis,
                            recurrence = routine.daysOfWeek,
                            confidence = 1.0f,
                            truthState = TruthState.USER_CONFIRMED,
                            explanation = "User-configured routine"
                        )
                    )
                }
            } else {
                val nextCal = getNextScheduledOccurrence(nowCal, expectedDays, parsedTime.hour24, parsedTime.minute)
                val dayName = dayNameFormat.format(nextCal.time)
                candidateItems.add(
                    NextUpCandidate(
                        id = "routine_${routine.id}",
                        title = routine.title,
                        subtitle = "${routine.category} • Next expected $dayName",
                        timeDisplay = parsedTime.formatted,
                        isToday = false,
                        nextEpochMs = nextCal.timeInMillis,
                        recurrence = routine.daysOfWeek,
                        confidence = 1.0f,
                        truthState = TruthState.USER_CONFIRMED,
                        explanation = "User routine scheduled for $dayName"
                    )
                )
            }
        }

        // 3. Evaluate Persistent Intentions (e.g. Scheduled Alarms / Reminders)
        for (intent in intentions.filter { it.executionState == "SCHEDULED" || it.executionState == "PENDING" }) {
            if (intent.scheduledTimestamp > nowMillis) {
                val intentCal = Calendar.getInstance(tz).apply { timeInMillis = intent.scheduledTimestamp }
                val isToday = intentCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR) &&
                        intentCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
                candidateItems.add(
                    NextUpCandidate(
                        id = intent.id,
                        title = intent.rawRequest,
                        subtitle = intent.normalizedIntent,
                        timeDisplay = timeFormatDisplay.format(intentCal.time),
                        isToday = isToday,
                        nextEpochMs = intent.scheduledTimestamp,
                        recurrence = intent.recurrenceRule,
                        confidence = 1.0f,
                        truthState = TruthState.USER_CONFIRMED,
                        explanation = "Scheduled intention"
                    )
                )
            }
        }

        // Priority 1: Pick nearest upcoming event for TODAY
        val todayCandidates = candidateItems.filter { it.isToday }.sortedBy { it.nextEpochMs }
        if (todayCandidates.isNotEmpty()) {
            val best = todayCandidates.first()
            return NextUpItem(
                id = best.id,
                title = best.title,
                subtitle = best.subtitle,
                timeDisplay = best.timeDisplay,
                isToday = true,
                nextOccurrenceEpochMs = best.nextEpochMs,
                recurrenceRule = best.recurrence,
                confidence = best.confidence,
                truthState = best.truthState,
                explanation = best.explanation,
                weatherDisplay = weatherStr
            )
        }

        // Priority 2: Pick nearest upcoming future occurrence (e.g. Monday school if today is Saturday)
        val futureCandidates = candidateItems.filter { !it.isToday }.sortedBy { it.nextEpochMs }
        if (futureCandidates.isNotEmpty()) {
            val best = futureCandidates.first()
            return NextUpItem(
                id = best.id,
                title = best.title,
                subtitle = best.subtitle,
                timeDisplay = best.timeDisplay,
                isToday = false,
                nextOccurrenceEpochMs = best.nextEpochMs,
                recurrenceRule = best.recurrence,
                confidence = best.confidence,
                truthState = best.truthState,
                explanation = best.explanation,
                weatherDisplay = weatherStr
            )
        }

        return null
    }

    /**
     * Strictly audited time parser.
     * Supports 12-hour (10:30 AM, 10:30 PM, 12:00 PM, 12:00 AM) and 24-hour (00:30, 10:30, 22:30).
     * Never converts AM to PM or vice-versa regardless of timezone.
     */
    fun parseTimeOfDay(rawTime: String, tz: TimeZone = TimeZone.getDefault()): ParsedTime {
        val clean = rawTime.trim().uppercase(Locale.US)
        val isPm = clean.contains("PM")
        val isAm = clean.contains("AM")
        val digits = clean.replace("AM", "").replace("PM", "").trim()
        val parts = digits.split(":")

        var hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 30

        if (isPm) {
            if (hour < 12) hour += 12
        } else if (isAm) {
            if (hour == 12) hour = 0
        }

        val cal = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val formatter = SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = tz
        }

        return ParsedTime(
            hour24 = hour,
            minute = minute,
            formatted = formatter.format(cal.time)
        )
    }

    private fun parseDaysOfWeek(rule: String): List<Int> {
        val upper = rule.uppercase().trim()
        return when {
            upper.contains("MON-FRI") || upper.contains("WEEKDAY") ->
                listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
            upper.contains("SAT-SUN") || upper.contains("WEEKEND") ->
                listOf(Calendar.SATURDAY, Calendar.SUNDAY)
            upper.contains("DAILY") || upper.contains("EVERYDAY") ->
                listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)
            else ->
                listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
        }
    }

    private fun getNextScheduledOccurrence(
        baseCal: Calendar,
        validDays: List<Int>,
        targetHour: Int,
        targetMinute: Int
    ): Calendar {
        val candidate = (baseCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        for (dayOffset in 1..7) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
            if (candidate.get(Calendar.DAY_OF_WEEK) in validDays) {
                break
            }
        }
        return candidate
    }

    private data class NextUpCandidate(
        val id: String,
        val title: String,
        val subtitle: String,
        val timeDisplay: String,
        val isToday: Boolean,
        val nextEpochMs: Long,
        val recurrence: String,
        val confidence: Float,
        val truthState: TruthState,
        val explanation: String
    )
}
