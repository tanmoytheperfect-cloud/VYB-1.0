package com.example.domain.temporal

import java.text.SimpleDateFormat
import java.util.*

data class NextOccurrenceResult(
    val nextTimestampMillis: Long,
    val formattedNextTime: String,
    val isRecurring: Boolean,
    val ruleDescription: String
)

data class FreshnessEvaluation(
    val isStale: Boolean,
    val delayMinutes: Long,
    val reason: String
)

/**
 * Evaluates recurring schedules, temporal rules, and event freshness
 * with zero reliance on constant AI loops or continuous background polling.
 */
object VybRecurrenceEvaluator {

    /**
     * Calculates the next occurrence in milliseconds from `nowMillis`.
     * If `targetTimeOfDay` is "07:30", calculates the next calendar match.
     */
    fun calculateNextOccurrence(
        recurrenceRule: String,
        timeOfDay: String = "07:30",
        timezoneId: String = TimeZone.getDefault().id,
        nowMillis: Long = System.currentTimeMillis()
    ): NextOccurrenceResult {
        val tz = TimeZone.getTimeZone(timezoneId)
        val cal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }

        val parts = timeOfDay.split(":")
        val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val targetMinute = parts.getOrNull(1)?.toIntOrNull() ?: 30

        return when (recurrenceRule.uppercase().trim()) {
            "DAILY" -> {
                val nextCal = getNextCalendarForTime(cal, targetHour, targetMinute)
                NextOccurrenceResult(
                    nextTimestampMillis = nextCal.timeInMillis,
                    formattedNextTime = formatCalendar(nextCal),
                    isRecurring = true,
                    ruleDescription = "Daily at $timeOfDay"
                )
            }
            "WEEKDAYS" -> {
                var nextCal = getNextCalendarForTime(cal, targetHour, targetMinute)
                while (nextCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    nextCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                NextOccurrenceResult(
                    nextTimestampMillis = nextCal.timeInMillis,
                    formattedNextTime = formatCalendar(nextCal),
                    isRecurring = true,
                    ruleDescription = "Weekdays (Mon-Fri) at $timeOfDay"
                )
            }
            "WEEKENDS" -> {
                var nextCal = getNextCalendarForTime(cal, targetHour, targetMinute)
                while (nextCal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY &&
                    nextCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    nextCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                NextOccurrenceResult(
                    nextTimestampMillis = nextCal.timeInMillis,
                    formattedNextTime = formatCalendar(nextCal),
                    isRecurring = true,
                    ruleDescription = "Weekends (Sat-Sun) at $timeOfDay"
                )
            }
            "SPLIT_WEEKDAY_WEEKEND" -> {
                // e.g. 07:30 on weekdays, 08:30 on weekends
                val candidateCal = Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
                val weekdayHour = targetHour
                val weekdayMinute = targetMinute
                val weekendHour = targetHour + 1
                val weekendMinute = targetMinute

                // Check today and up to next 7 days to find nearest valid future slot
                var bestCal: Calendar? = null
                for (dayOffset in 0..7) {
                    val testCal = Calendar.getInstance(tz).apply {
                        timeInMillis = nowMillis
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                        val isWeekend = get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                        set(Calendar.HOUR_OF_DAY, if (isWeekend) weekendHour else weekdayHour)
                        set(Calendar.MINUTE, if (isWeekend) weekendMinute else weekdayMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (testCal.timeInMillis > nowMillis) {
                        bestCal = testCal
                        break
                    }
                }
                val targetCal = bestCal ?: getNextCalendarForTime(cal, targetHour, targetMinute)
                NextOccurrenceResult(
                    nextTimestampMillis = targetCal.timeInMillis,
                    formattedNextTime = formatCalendar(targetCal),
                    isRecurring = true,
                    ruleDescription = "Split: $weekdayHour:${String.format("%02d", weekdayMinute)} on weekdays, $weekendHour:${String.format("%02d", weekendMinute)} on weekends"
                )
            }
            else -> {
                // "NONE" or one-time
                val nextCal = getNextCalendarForTime(cal, targetHour, targetMinute)
                NextOccurrenceResult(
                    nextTimestampMillis = nextCal.timeInMillis,
                    formattedNextTime = formatCalendar(nextCal),
                    isRecurring = false,
                    ruleDescription = "One-time at $timeOfDay"
                )
            }
        }
    }

    private fun getNextCalendarForTime(base: Calendar, targetHour: Int, targetMinute: Int): Calendar {
        val target = (base.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= base.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target
    }

    private fun formatCalendar(cal: Calendar): String {
        val sdf = SimpleDateFormat("EEE, MMM d, yyyy 'at' hh:mm a", Locale.getDefault())
        sdf.timeZone = cal.timeZone
        return sdf.format(cal.time)
    }

    /**
     * Evaluates if a scheduled event timestamp that passed in the past is stale.
     * e.g. Phone was switched off or asleep.
     */
    fun evaluateFreshness(
        scheduledTimestamp: Long,
        staleThresholdMinutes: Int = 120,
        nowMillis: Long = System.currentTimeMillis()
    ): FreshnessEvaluation {
        if (scheduledTimestamp > nowMillis) {
            return FreshnessEvaluation(isStale = false, delayMinutes = 0, reason = "Event is in the future")
        }

        val elapsedMs = nowMillis - scheduledTimestamp
        val elapsedMinutes = elapsedMs / (60 * 1000L)

        return if (elapsedMinutes > staleThresholdMinutes) {
            FreshnessEvaluation(
                isStale = true,
                delayMinutes = elapsedMinutes,
                reason = "Event passed $elapsedMinutes minutes ago (threshold: $staleThresholdMinutes min). Execution would be stale."
            )
        } else {
            FreshnessEvaluation(
                isStale = false,
                delayMinutes = elapsedMinutes,
                reason = "Within acceptable delay window ($elapsedMinutes min < $staleThresholdMinutes min)."
            )
        }
    }

    /**
     * Generates a stable idempotency key for an intention occurrence.
     */
    fun generateOccurrenceKey(intentionId: String, timestampMillis: Long): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        return "${intentionId}_${sdf.format(Date(timestampMillis))}"
    }
}
