package com.example.domain.cognitive.engine

import com.example.data.local.dao.CandidatePatternDao
import com.example.data.local.entity.CandidatePatternEntity
import com.example.domain.cognitive.models.CandidatePattern
import com.example.domain.cognitive.models.CognitiveObservation
import com.example.domain.cognitive.models.PatternStatus
import com.example.domain.cognitive.models.TruthState
import java.text.SimpleDateFormat
import java.util.*

class PatternDiscoveryEngine(
    private val candidatePatternDao: CandidatePatternDao,
    private val confidenceEngine: CognitiveConfidenceEngine = CognitiveConfidenceEngine()
) {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.US)

    /**
     * Discovers and updates patterns by analyzing clusters of observations for a given subject/domain.
     * Incorporates positive occurrence clustering and evidence-aware absence reasoning.
     */
    suspend fun discoverPatterns(observations: List<CognitiveObservation>): List<CandidatePattern> {
        if (observations.isEmpty()) return emptyList()

        // Group observations by subject + eventType/domain
        val groups = observations.groupBy { "${it.subject.lowercase().trim()}::${it.domain}::${it.eventType}" }
        val results = mutableListOf<CandidatePattern>()

        for ((key, obsList) in groups) {
            val sample = obsList.first()
            val subject = sample.subject
            val domain = sample.domain
            val eventType = sample.eventType

            val distinctDates = obsList.map { it.dateString }.distinct()
            val distinctDaysOfWeek = obsList.map { it.dayOfWeek }.distinct()
            val evidenceCount = obsList.size
            val distinctDayCount = maxOf(distinctDates.size, distinctDaysOfWeek.size)

            // Compute weekday vs weekend distribution
            val weekdayObs = obsList.filter { it.dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY }
            val weekendObs = obsList.filter { it.dayOfWeek == Calendar.SATURDAY || it.dayOfWeek == Calendar.SUNDAY }

            // Determine recurrence structure strictly from evidence
            val recurrence: String
            val expectedDays: List<Int>
            val absenceEvidenceSummary: String

            when {
                // Strong weekday pattern: All observed days are weekdays, no weekend activity observed
                weekdayObs.isNotEmpty() && weekendObs.isEmpty() -> {
                    recurrence = "WEEKDAY"
                    expectedDays = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)
                    absenceEvidenceSummary = if (distinctDayCount >= 3) " and no weekend activity" else ""
                }
                // Weekend pattern
                weekendObs.isNotEmpty() && weekdayObs.isEmpty() -> {
                    recurrence = "WEEKEND"
                    expectedDays = listOf(Calendar.SATURDAY, Calendar.SUNDAY)
                    absenceEvidenceSummary = " and no weekday activity"
                }
                // Daily pattern across both weekdays and weekends
                weekdayObs.isNotEmpty() && weekendObs.isNotEmpty() && distinctDayCount >= 5 -> {
                    recurrence = "DAILY"
                    expectedDays = listOf(Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)
                    absenceEvidenceSummary = ""
                }
                else -> {
                    recurrence = "CUSTOM"
                    expectedDays = distinctDaysOfWeek
                    absenceEvidenceSummary = ""
                }
            }

            // Cluster timestamps to find representative time window
            val representativeTime = computeAverageTime(obsList.map { it.timestamp })
            val timeWindowDisplay = representativeTime.ifBlank { "08:30 AM" }

            // Compute confidence and status strictly from evidence
            val confidence = confidenceEngine.computeConfidence(
                evidenceCount = evidenceCount,
                distinctDays = distinctDayCount,
                contradictionCount = 0
            )
            val status = confidenceEngine.deriveStatus(confidence)

            // Construct truthful explanation of learning
            val explanation = when (recurrence) {
                "WEEKDAY" -> "VYB learned this from $distinctDayCount weekday observation(s)$absenceEvidenceSummary."
                "WEEKEND" -> "VYB learned this from $distinctDayCount weekend observation(s)$absenceEvidenceSummary."
                "DAILY" -> "VYB learned this from $distinctDayCount daily observation(s) across both weekdays and weekends."
                else -> "VYB observed this on $distinctDayCount day(s)."
            }

            val patternTitle = when {
                domain.equals("APP_USAGE", ignoreCase = true) || eventType.contains("APP", ignoreCase = true) ->
                    "$subject App Engagement Pattern"
                subject.isNotBlank() && eventType.contains("SCHOOL", ignoreCase = true) ->
                    "$subject's School Routine"
                eventType.contains("WALK", ignoreCase = true) ->
                    if (timeWindowDisplay.contains("PM")) "Evening Walking Routine" else "Daily Walking Routine"
                eventType.contains("ACTIVITY", ignoreCase = true) || domain.equals("ACTIVITY", ignoreCase = true) ->
                    "Personal Physical Activity Pattern"
                subject.isNotBlank() ->
                    "$subject Recurring $domain Routine"
                else ->
                    "Recurring $domain Routine"
            }

            val patternDescription = "$patternTitle appears to normally occur around $timeWindowDisplay (${recurrence.lowercase()}-bound)."

            val cleanSubject = subject.lowercase().replace(Regex("[^a-z0-9_]"), "_").trim('_').ifBlank { "unknown" }
            val cleanEvent = eventType.lowercase().replace(Regex("[^a-z0-9_]"), "_").trim('_').ifBlank { "event" }
            val patternId = "pat_${cleanSubject}_${cleanEvent}"

            // Check if existing pattern has recorded exceptions
            val existingEntity = candidatePatternDao.getPatternById(patternId)
            val exceptionsJson = existingEntity?.exceptionsJson ?: "[]"
            val isUserConfirmed = existingEntity?.isUserConfirmed ?: false

            val pattern = CandidatePattern(
                patternId = patternId,
                ownerId = "primary_owner",
                domain = domain,
                subject = subject,
                title = patternTitle,
                description = patternDescription,
                evidenceCount = evidenceCount,
                supportingObservationIds = obsList.map { it.observationId },
                contradictoryObservationIds = emptyList(),
                confidence = confidence,
                firstObserved = obsList.minOf { it.timestamp },
                lastObserved = obsList.maxOf { it.timestamp },
                recurrence = recurrence,
                expectedDays = expectedDays,
                expectedTimeRange = timeWindowDisplay,
                expectedContext = "Identified via repeated behavioral observations",
                exceptions = emptyList(),
                status = status,
                explanation = explanation,
                truthState = if (isUserConfirmed) TruthState.USER_CONFIRMED else TruthState.LEARNED,
                isUserConfirmed = isUserConfirmed,
                updatedAt = System.currentTimeMillis()
            )

            // Persist to Room candidate_patterns
            val entity = CandidatePatternEntity(
                patternId = pattern.patternId,
                ownerId = pattern.ownerId,
                domain = pattern.domain,
                subject = pattern.subject,
                title = pattern.title,
                description = pattern.description,
                evidenceCount = pattern.evidenceCount,
                supportingObservationIdsJson = pattern.supportingObservationIds.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]"),
                contradictoryObservationIdsJson = "[]",
                confidence = pattern.confidence,
                firstObserved = pattern.firstObserved,
                lastObserved = pattern.lastObserved,
                recurrence = pattern.recurrence,
                expectedDaysJson = pattern.expectedDays.joinToString(prefix = "[", separator = ",", postfix = "]"),
                expectedTimeRange = pattern.expectedTimeRange,
                expectedContext = pattern.expectedContext,
                exceptionsJson = exceptionsJson,
                status = pattern.status.name,
                explanation = pattern.explanation,
                truthState = pattern.truthState.name,
                isUserConfirmed = pattern.isUserConfirmed,
                updatedAt = pattern.updatedAt
            )
            candidatePatternDao.insertOrUpdate(entity)
            results.add(pattern)
        }

        return results
    }

    private fun computeAverageTime(timestamps: List<Long>): String {
        if (timestamps.isEmpty()) return ""
        val minutesOfDay = timestamps.map {
            val cal = Calendar.getInstance().apply { timeInMillis = it }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        }
        val avgMinute = minutesOfDay.average().toInt()
        val hour = avgMinute / 60
        val minute = avgMinute % 60
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return timeFormat.format(cal.time)
    }
}
