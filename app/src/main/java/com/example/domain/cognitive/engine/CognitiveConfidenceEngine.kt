package com.example.domain.cognitive.engine

import com.example.domain.cognitive.models.PatternStatus
import kotlin.math.min

/**
 * Calculates empirical confidence strictly from evidence count, consistency, and distinct observation days.
 * Never establishes rules from single observations.
 */
class CognitiveConfidenceEngine {

    fun computeConfidence(
        evidenceCount: Int,
        distinctDays: Int,
        contradictionCount: Int = 0
    ): Float {
        if (evidenceCount <= 0 || distinctDays <= 0) return 0.0f

        // Base confidence derived from distinct days of supporting observation
        val baseScore = when {
            distinctDays == 1 -> 0.20f
            distinctDays == 2 -> 0.35f
            distinctDays == 3 -> 0.50f
            distinctDays == 4 -> 0.65f
            distinctDays in 5..9 -> 0.85f
            else -> min(0.98f, 0.85f + (distinctDays - 5) * 0.02f)
        }

        // Additional evidence reinforcement (same day multi-signal consistency)
        val reinforcement = min(0.05f, (evidenceCount - distinctDays) * 0.01f)

        // Contradiction penalty
        val penalty = contradictionCount * 0.15f

        return (baseScore + reinforcement - penalty).coerceIn(0.05f, 1.0f)
    }

    fun deriveStatus(confidence: Float, contradictionCount: Int = 0): PatternStatus {
        return when {
            contradictionCount >= 3 && confidence < 0.40f -> PatternStatus.CONTRADICTED
            confidence >= 0.75f -> PatternStatus.ESTABLISHED
            confidence >= 0.40f -> PatternStatus.EMERGING
            else -> PatternStatus.CANDIDATE
        }
    }
}
