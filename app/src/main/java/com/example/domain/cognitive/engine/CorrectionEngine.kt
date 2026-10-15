package com.example.domain.cognitive.engine

import com.example.data.local.dao.CandidatePatternDao
import com.example.domain.cognitive.models.CandidatePattern
import com.example.domain.cognitive.models.CognitiveCorrection
import com.example.domain.cognitive.models.PatternStatus
import com.example.domain.cognitive.models.TruthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Handles explicit user corrections to learned inferences and predictions.
 * Accepts the correction, records it, lowers pattern confidence without destroying evidence,
 * and prevents recurring false hypotheses.
 */
class CorrectionEngine(
    private val candidatePatternDao: CandidatePatternDao
) {

    private val _corrections = MutableStateFlow<List<CognitiveCorrection>>(emptyList())
    val corrections: StateFlow<List<CognitiveCorrection>> = _corrections.asStateFlow()

    suspend fun recordCorrection(
        targetPatternOrFact: String,
        userCorrectionStatement: String,
        previousBelief: String
    ): CognitiveCorrection {
        val correctionId = "corr_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val correction = CognitiveCorrection(
            correctionId = correctionId,
            targetPatternOrFact = targetPatternOrFact,
            userCorrection = userCorrectionStatement,
            previousBelief = previousBelief,
            timestamp = System.currentTimeMillis()
        )

        _corrections.value = _corrections.value + correction

        // Find relevant pattern and penalize confidence while preserving evidence history
        val patterns = candidatePatternDao.getAllPatterns()
        val matching = patterns.firstOrNull {
            it.title.contains(targetPatternOrFact, ignoreCase = true) ||
            it.subject.contains(targetPatternOrFact, ignoreCase = true) ||
            targetPatternOrFact.contains(it.subject, ignoreCase = true)
        }

        if (matching != null) {
            val penalizedConfidence = (matching.confidence - 0.35f).coerceAtLeast(0.15f)
            val updated = matching.copy(
                confidence = penalizedConfidence,
                status = if (penalizedConfidence < 0.40f) PatternStatus.CANDIDATE.name else matching.status,
                explanation = "Adjusted following user correction: \"$userCorrectionStatement\" (previous belief: \"$previousBelief\").",
                updatedAt = System.currentTimeMillis()
            )
            candidatePatternDao.insertOrUpdate(updated)
        }

        return correction
    }

    fun getAllCorrections(): List<CognitiveCorrection> = _corrections.value
}
