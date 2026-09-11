package com.example.domain.learning

import com.example.data.local.dao.LearnedRoutineDao
import com.example.data.local.dao.TimelineDao
import com.example.data.local.entity.LearnedRoutineEntity
import com.example.data.local.entity.TimelineEventEntity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class RoutineLearningEngine(
    private val timelineDao: TimelineDao,
    private val learnedRoutineDao: LearnedRoutineDao
) {

    /**
     * Data structure representing circular statistical metrics for a cluster of timestamps.
     */
    data class CircularTimeStats(
        val meanMinuteOfDay: Int,
        val standardDeviationMinutes: Float,
        val consistencyScore: Float, // 0.0 to 1.0
        val windowStartMinute: Int,
        val windowEndMinute: Int,
        val distinctDates: List<String>,
        val dayCounts: Map<Int, Int>, // Calendar.DAY_OF_WEEK -> count
        val firstTimestamp: Long,
        val lastTimestamp: Long
    )

    /**
     * Executes routine pattern extraction across timeline history.
     * Updates confidence, evidence count, and status based strictly on genuine evidence.
     */
    suspend fun analyzeAndLearnRoutines(): List<LearnedRoutineEntity> {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        val allEvents = timelineDao.getEventsSince(thirtyDaysAgo)

        if (allEvents.isEmpty()) {
            learnedRoutineDao.clearAllLearnedRoutines()
            return emptyList()
        }

        val learnedList = mutableListOf<LearnedRoutineEntity>()

        // 1. FIRST_PHONE_USE / SCREEN_ACTIVITY: Morning Unlocks (Separately for Weekdays vs Weekends)
        val unlockEvents = allEvents.filter { it.eventType == "DEVICE_UNLOCK" }
        
        // Weekday unlocks (Monday to Friday)
        val weekdayUnlocks = unlockEvents.filter { it.dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY }
        if (weekdayUnlocks.isNotEmpty()) {
            val stats = computeCircularStats(weekdayUnlocks)
            val evidenceCount = weekdayUnlocks.size
            val distinctDays = stats.distinctDates.size
            val (status, confidence) = computeStatusAndConfidence(evidenceCount, distinctDays, stats.consistencyScore)

            val startWindow = formatMinuteOfDay(stats.windowStartMinute)
            val endWindow = formatMinuteOfDay(stats.windowEndMinute)
            val meanFormatted = formatMinuteOfDay(stats.meanMinuteOfDay)

            val routine = LearnedRoutineEntity(
                routineId = "learned_morning_unlock_weekday",
                title = "Weekday Device Activity",
                description = "Recurring device unlock activity observed around $meanFormatted ($startWindow – $endWindow) on weekdays.",
                daysOfWeek = "WEEKDAY",
                dayTypeCategory = "WORKDAY",
                expectedTimeWindow = "$startWindow – $endWindow",
                startMinuteOfDay = stats.windowStartMinute,
                endMinuteOfDay = stats.windowEndMinute,
                meanMinuteOfDay = stats.meanMinuteOfDay,
                timeVarianceMinutes = stats.standardDeviationMinutes,
                confidenceScore = confidence,
                evidenceCount = evidenceCount,
                distinctObservationDays = distinctDays,
                status = status,
                patternCategory = "FIRST_PHONE_USE",
                source = "DEVICE_OBSERVATION",
                firstObservedTimestamp = stats.firstTimestamp,
                lastObservedTimestamp = stats.lastTimestamp,
                observedDatesSummary = stats.distinctDates.takeLast(10).joinToString(", "),
                dayDistributionSummary = formatDayDistribution(stats.dayCounts),
                sourceEventsSummary = "$evidenceCount unlock events recorded across $distinctDays distinct weekday(s)",
                proactiveActionType = "INFORM"
            )
            learnedRoutineDao.insertOrUpdate(routine)
            learnedList.add(routine)
        }

        // Weekend unlocks (Saturday and Sunday)
        val weekendUnlocks = unlockEvents.filter { it.dayOfWeek == Calendar.SATURDAY || it.dayOfWeek == Calendar.SUNDAY }
        val morningWeekendUnlocks = weekendUnlocks.filter {
            val min = getMinuteOfDay(it.timestamp)
            min in 360..750 // 06:00 to 12:30
        }
        if (morningWeekendUnlocks.isNotEmpty()) {
            val stats = computeCircularStats(morningWeekendUnlocks)
            val evidenceCount = morningWeekendUnlocks.size
            val distinctDays = stats.distinctDates.size
            val (status, confidence) = computeStatusAndConfidence(evidenceCount, distinctDays, stats.consistencyScore)

            val startWindow = formatMinuteOfDay(stats.windowStartMinute)
            val endWindow = formatMinuteOfDay(stats.windowEndMinute)
            val meanFormatted = formatMinuteOfDay(stats.meanMinuteOfDay)

            val routine = LearnedRoutineEntity(
                routineId = "learned_morning_unlock_weekend",
                title = "Weekend Morning Device Activity",
                description = "Recurring device unlock activity observed around $meanFormatted ($startWindow – $endWindow) on weekends.",
                daysOfWeek = "WEEKEND",
                dayTypeCategory = "WEEKEND",
                expectedTimeWindow = "$startWindow – $endWindow",
                startMinuteOfDay = stats.windowStartMinute,
                endMinuteOfDay = stats.windowEndMinute,
                meanMinuteOfDay = stats.meanMinuteOfDay,
                timeVarianceMinutes = stats.standardDeviationMinutes,
                confidenceScore = confidence,
                evidenceCount = evidenceCount,
                distinctObservationDays = distinctDays,
                status = status,
                patternCategory = "FIRST_PHONE_USE",
                source = "DEVICE_OBSERVATION",
                firstObservedTimestamp = stats.firstTimestamp,
                lastObservedTimestamp = stats.lastTimestamp,
                observedDatesSummary = stats.distinctDates.takeLast(10).joinToString(", "),
                dayDistributionSummary = formatDayDistribution(stats.dayCounts),
                sourceEventsSummary = "$evidenceCount unlock events recorded across $distinctDays distinct weekend day(s)",
                proactiveActionType = "INFORM"
            )
            learnedRoutineDao.insertOrUpdate(routine)
            learnedList.add(routine)
        }

        // 2. APP_USAGE: App Sessions Clustering
        val appEvents = allEvents.filter { it.eventType == "APP_OPENED" }
        val appGroups = appEvents.groupBy { it.contextData.substringBefore(" (").trim() }

        for ((appName, events) in appGroups) {
            if (events.isNotEmpty()) {
                val stats = computeCircularStats(events)
                val evidenceCount = events.size
                val distinctDays = stats.distinctDates.size
                val (status, confidence) = computeStatusAndConfidence(evidenceCount, distinctDays, stats.consistencyScore)

                val startWindow = formatMinuteOfDay(stats.windowStartMinute)
                val endWindow = formatMinuteOfDay(stats.windowEndMinute)
                val meanFormatted = formatMinuteOfDay(stats.meanMinuteOfDay)
                val safeId = appName.lowercase().replace(Regex("[^a-z0-9]"), "_")

                val routine = LearnedRoutineEntity(
                    routineId = "learned_app_pattern_$safeId",
                    title = "$appName Activity Cluster",
                    description = "Repeated usage of $appName observed around $meanFormatted ($startWindow – $endWindow).",
                    daysOfWeek = if (events.all { it.dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY }) "WEEKDAY" else if (events.all { it.dayOfWeek in listOf(Calendar.SATURDAY, Calendar.SUNDAY) }) "WEEKEND" else "ANY",
                    dayTypeCategory = "ANY",
                    expectedTimeWindow = "$startWindow – $endWindow",
                    startMinuteOfDay = stats.windowStartMinute,
                    endMinuteOfDay = stats.windowEndMinute,
                    meanMinuteOfDay = stats.meanMinuteOfDay,
                    timeVarianceMinutes = stats.standardDeviationMinutes,
                    confidenceScore = confidence,
                    evidenceCount = evidenceCount,
                    distinctObservationDays = distinctDays,
                    status = status,
                    patternCategory = "APP_USAGE",
                    source = "DEVICE_OBSERVATION",
                    firstObservedTimestamp = stats.firstTimestamp,
                    lastObservedTimestamp = stats.lastTimestamp,
                    observedDatesSummary = stats.distinctDates.takeLast(10).joinToString(", "),
                    dayDistributionSummary = formatDayDistribution(stats.dayCounts),
                    sourceEventsSummary = "$evidenceCount app sessions observed across $distinctDays distinct day(s)",
                    proactiveActionType = "INFORM"
                )
                learnedRoutineDao.insertOrUpdate(routine)
                learnedList.add(routine)
            }
        }

        // 3. SLEEP_INACTIVITY: Night Inactivity & Rest Onset
        val nightEvents = allEvents.filter {
            (it.eventType == "SLEEP_INACTIVITY_PERIOD" || it.eventType == "SCREEN_OFF") &&
            (getHourOfDay(it.timestamp) in 21..23 || getHourOfDay(it.timestamp) in 0..4)
        }
        if (nightEvents.isNotEmpty()) {
            val stats = computeCircularStats(nightEvents)
            val evidenceCount = nightEvents.size
            val distinctDays = stats.distinctDates.size
            val (status, confidence) = computeStatusAndConfidence(evidenceCount, distinctDays, stats.consistencyScore)

            val startWindow = formatMinuteOfDay(stats.windowStartMinute)
            val endWindow = formatMinuteOfDay(stats.windowEndMinute)
            val meanFormatted = formatMinuteOfDay(stats.meanMinuteOfDay)

            val routine = LearnedRoutineEntity(
                routineId = "learned_night_inactivity_sleep",
                title = "Night Inactivity Onset",
                description = "Device inactivity onset observed consistently around $meanFormatted ($startWindow – $endWindow).",
                daysOfWeek = "ANY",
                dayTypeCategory = "ANY",
                expectedTimeWindow = "$startWindow – $endWindow",
                startMinuteOfDay = stats.windowStartMinute,
                endMinuteOfDay = stats.windowEndMinute,
                meanMinuteOfDay = stats.meanMinuteOfDay,
                timeVarianceMinutes = stats.standardDeviationMinutes,
                confidenceScore = confidence,
                evidenceCount = evidenceCount,
                distinctObservationDays = distinctDays,
                status = status,
                patternCategory = "SLEEP_INACTIVITY",
                source = "DEVICE_OBSERVATION",
                firstObservedTimestamp = stats.firstTimestamp,
                lastObservedTimestamp = stats.lastTimestamp,
                observedDatesSummary = stats.distinctDates.takeLast(10).joinToString(", "),
                dayDistributionSummary = formatDayDistribution(stats.dayCounts),
                sourceEventsSummary = "$evidenceCount night inactivity transitions across $distinctDays day(s)",
                proactiveActionType = "INFORM"
            )
            learnedRoutineDao.insertOrUpdate(routine)
            learnedList.add(routine)
        }

        // 4. CHARGING: Power Connection Clustering
        val chargingEvents = allEvents.filter { it.eventType == "POWER_CONNECTED" || it.eventType == "CHARGING_STARTED" }
        if (chargingEvents.isNotEmpty()) {
            val stats = computeCircularStats(chargingEvents)
            val evidenceCount = chargingEvents.size
            val distinctDays = stats.distinctDates.size
            val (status, confidence) = computeStatusAndConfidence(evidenceCount, distinctDays, stats.consistencyScore)

            val startWindow = formatMinuteOfDay(stats.windowStartMinute)
            val endWindow = formatMinuteOfDay(stats.windowEndMinute)
            val meanFormatted = formatMinuteOfDay(stats.meanMinuteOfDay)

            val routine = LearnedRoutineEntity(
                routineId = "learned_charging_pattern",
                title = "Power Connection Window",
                description = "Device is repeatedly connected to power around $meanFormatted ($startWindow – $endWindow).",
                daysOfWeek = "ANY",
                dayTypeCategory = "ANY",
                expectedTimeWindow = "$startWindow – $endWindow",
                startMinuteOfDay = stats.windowStartMinute,
                endMinuteOfDay = stats.windowEndMinute,
                meanMinuteOfDay = stats.meanMinuteOfDay,
                timeVarianceMinutes = stats.standardDeviationMinutes,
                confidenceScore = confidence,
                evidenceCount = evidenceCount,
                distinctObservationDays = distinctDays,
                status = status,
                patternCategory = "CHARGING",
                source = "DEVICE_OBSERVATION",
                firstObservedTimestamp = stats.firstTimestamp,
                lastObservedTimestamp = stats.lastTimestamp,
                observedDatesSummary = stats.distinctDates.takeLast(10).joinToString(", "),
                dayDistributionSummary = formatDayDistribution(stats.dayCounts),
                sourceEventsSummary = "$evidenceCount charging events across $distinctDays day(s)",
                proactiveActionType = "INFORM"
            )
            learnedRoutineDao.insertOrUpdate(routine)
            learnedList.add(routine)
        }

        // 5. CALENDAR_PATTERN: Recurring Scheduled Calendar Events (when permission granted)
        val calendarEvents = allEvents.filter { it.eventType == "CALENDAR_EVENT" }
        if (calendarEvents.isNotEmpty()) {
            val stats = computeCircularStats(calendarEvents)
            val evidenceCount = calendarEvents.size
            val distinctDays = stats.distinctDates.size
            val (status, confidence) = computeStatusAndConfidence(evidenceCount, distinctDays, stats.consistencyScore)

            val startWindow = formatMinuteOfDay(stats.windowStartMinute)
            val endWindow = formatMinuteOfDay(stats.windowEndMinute)
            val meanFormatted = formatMinuteOfDay(stats.meanMinuteOfDay)

            val routine = LearnedRoutineEntity(
                routineId = "learned_calendar_pattern",
                title = "Scheduled Calendar Block",
                description = "Authorized calendar events regularly scheduled around $meanFormatted ($startWindow – $endWindow).",
                daysOfWeek = "ANY",
                dayTypeCategory = "ANY",
                expectedTimeWindow = "$startWindow – $endWindow",
                startMinuteOfDay = stats.windowStartMinute,
                endMinuteOfDay = stats.windowEndMinute,
                meanMinuteOfDay = stats.meanMinuteOfDay,
                timeVarianceMinutes = stats.standardDeviationMinutes,
                confidenceScore = confidence,
                evidenceCount = evidenceCount,
                distinctObservationDays = distinctDays,
                status = status,
                patternCategory = "CALENDAR_PATTERN",
                source = "DEVICE_OBSERVATION",
                firstObservedTimestamp = stats.firstTimestamp,
                lastObservedTimestamp = stats.lastTimestamp,
                observedDatesSummary = stats.distinctDates.takeLast(10).joinToString(", "),
                dayDistributionSummary = formatDayDistribution(stats.dayCounts),
                sourceEventsSummary = "$evidenceCount calendar events logged across $distinctDays day(s)",
                proactiveActionType = "INFORM"
            )
            learnedRoutineDao.insertOrUpdate(routine)
            learnedList.add(routine)
        }

        return learnedList
    }

    /**
     * Circular time clustering: maps 24h minutes [0..1440) to angles on a unit circle
     * to accurately handle midnight-crossing boundaries.
     */
    fun computeCircularStats(events: List<TimelineEventEntity>): CircularTimeStats {
        if (events.isEmpty()) {
            return CircularTimeStats(0, 0f, 0f, 0, 0, emptyList(), emptyMap(), 0L, 0L)
        }

        var sumCos = 0.0
        var sumSin = 0.0
        val distinctDates = events.map { it.dateString.ifBlank { formatDate(it.timestamp) } }.distinct().sorted()
        val dayCounts = events.groupingBy { it.dayOfWeek }.eachCount()
        val firstTimestamp = events.minOf { it.timestamp }
        val lastTimestamp = events.maxOf { it.timestamp }

        for (event in events) {
            val min = getMinuteOfDay(event.timestamp)
            val angle = (min.toDouble() / 1440.0) * 2.0 * Math.PI
            sumCos += cos(angle)
            sumSin += sin(angle)
        }

        val n = events.size.toDouble()
        val meanCos = sumCos / n
        val meanSin = sumSin / n
        var meanAngle = atan2(meanSin, meanCos)
        if (meanAngle < 0) {
            meanAngle += 2.0 * Math.PI
        }

        val meanMinute = ((meanAngle / (2.0 * Math.PI)) * 1440.0).roundToInt() % 1440
        val resultantLength = sqrt(meanCos * meanCos + meanSin * meanSin).coerceIn(0.0001, 1.0)
        
        // Circular standard deviation in minutes
        val circularStdDevMinutes = (sqrt(-2.0 * ln(resultantLength)) * (1440.0 / (2.0 * Math.PI))).toFloat().coerceIn(5f, 180f)
        val consistencyScore = (1.0f - (circularStdDevMinutes / 120.0f)).coerceIn(0.2f, 1.0f)

        // Cluster margin around the mean (proportional to standard deviation, clamped between 15m and 45m)
        val margin = (circularStdDevMinutes * 0.8f).roundToInt().coerceIn(15, 45)
        val startMin = (meanMinute - margin + 1440) % 1440
        val endMin = (meanMinute + margin) % 1440

        return CircularTimeStats(
            meanMinuteOfDay = meanMinute,
            standardDeviationMinutes = circularStdDevMinutes,
            consistencyScore = consistencyScore,
            windowStartMinute = startMin,
            windowEndMinute = endMin,
            distinctDates = distinctDates,
            dayCounts = dayCounts,
            firstTimestamp = firstTimestamp,
            lastTimestamp = lastTimestamp
        )
    }

    /**
     * Mathematical evidence progression formula:
     * Progression: OBSERVATION -> POSSIBLE_PATTERN -> CANDIDATE_ROUTINE -> LEARNED_ROUTINE
     * 
     * Factors:
     * 1. Distinct observation days factor (Weight: 55%)
     * 2. Temporal circular consistency (Weight: 35%)
     * 3. Total observation volume factor (Weight: 10%)
     */
    fun computeStatusAndConfidence(
        evidenceCount: Int,
        distinctDays: Int,
        consistencyScore: Float
    ): Pair<String, Float> {
        val daysFactor = (distinctDays.toFloat() / 14.0f).coerceIn(0.05f, 1.0f)
        val countFactor = (evidenceCount.toFloat() / 20.0f).coerceIn(0.05f, 1.0f)
        
        val rawConfidence = (0.55f * daysFactor) + (0.35f * consistencyScore) + (0.10f * countFactor)
        
        return when {
            distinctDays <= 1 || evidenceCount <= 1 -> {
                val conf = minOf(0.35f, rawConfidence.coerceAtLeast(0.20f))
                "OBSERVATION" to (round(conf * 100f) / 100f)
            }
            distinctDays in 2..4 -> {
                val conf = rawConfidence.coerceIn(0.36f, 0.65f)
                "POSSIBLE_PATTERN" to (round(conf * 100f) / 100f)
            }
            distinctDays in 5..9 -> {
                val conf = rawConfidence.coerceIn(0.66f, 0.84f)
                "CANDIDATE_ROUTINE" to (round(conf * 100f) / 100f)
            }
            else -> {
                // distinctDays >= 10 and sufficient evidence
                val conf = if (consistencyScore >= 0.65f && evidenceCount >= 10) {
                    rawConfidence.coerceIn(0.85f, 0.98f)
                } else {
                    rawConfidence.coerceIn(0.70f, 0.84f)
                }
                val status = if (conf >= 0.85f) "LEARNED_ROUTINE" else "CANDIDATE_ROUTINE"
                status to (round(conf * 100f) / 100f)
            }
        }
    }

    private fun getMinuteOfDay(timestamp: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun getHourOfDay(timestamp: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    fun formatMinuteOfDay(totalMinutes: Int): String {
        val m = (totalMinutes % 1440 + 1440) % 1440
        val hour24 = m / 60
        val minute = m % 60
        val hour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
        val amPm = if (hour24 < 12) "AM" else "PM"
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatDayDistribution(dayCounts: Map<Int, Int>): String {
        val dayNames = mapOf(
            Calendar.MONDAY to "Mon",
            Calendar.TUESDAY to "Tue",
            Calendar.WEDNESDAY to "Wed",
            Calendar.THURSDAY to "Thu",
            Calendar.FRIDAY to "Fri",
            Calendar.SATURDAY to "Sat",
            Calendar.SUNDAY to "Sun"
        )
        return dayNames.mapNotNull { (calDay, name) ->
            val count = dayCounts[calDay] ?: 0
            if (count > 0) "$name: $count" else null
        }.joinToString(", ")
    }
}
