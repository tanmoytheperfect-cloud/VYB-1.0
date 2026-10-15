package com.example.domain.cognitive.engine

import com.example.domain.cognitive.models.CandidatePattern
import com.example.domain.cognitive.models.CognitiveObservation
import com.example.domain.cognitive.models.PatternStatus
import com.example.domain.cognitive.models.TruthState

/**
 * Forms initial behavioral hypotheses from early observations (1-2 signals).
 * Sets test conditions to be verified by future incoming observations.
 */
class HypothesisEngine {

    fun formHypothesis(observations: List<CognitiveObservation>): CandidatePattern? {
        if (observations.isEmpty()) return null
        val sample = observations.first()
        val distinctDays = observations.map { it.dateString }.distinct().size

        return CandidatePattern(
            patternId = "hypo_${sample.subject.lowercase().replace(" ", "_")}_${sample.eventType.lowercase()}",
            ownerId = sample.ownerId,
            domain = sample.domain,
            subject = sample.subject,
            title = "Hypothesis: ${sample.subject} ${sample.domain} Pattern",
            description = "Tentative pattern observed on $distinctDays day(s). Awaiting recurring evidence.",
            evidenceCount = observations.size,
            supportingObservationIds = observations.map { it.observationId },
            confidence = (distinctDays * 0.18f).coerceAtMost(0.35f),
            firstObserved = observations.minOf { it.timestamp },
            lastObserved = observations.maxOf { it.timestamp },
            recurrence = if (observations.all { it.dayOfWeek in 2..6 }) "WEEKDAY" else "UNKNOWN",
            status = PatternStatus.CANDIDATE,
            explanation = "Early hypothesis formed from $distinctDays observation(s); not yet established.",
            truthState = TruthState.INFERRED
        )
    }
}
