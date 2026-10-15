package com.example.domain.cognitive.engine

import com.example.domain.cognitive.models.CandidatePattern
import com.example.domain.cognitive.models.PatternStatus

data class DecisionAdvice(
    val recommendedOption: String,
    val confidence: Float,
    val learnedPreferenceSummary: String
)

class DecisionEngine {

    fun evaluateChoice(
        options: List<String>,
        learnedPatterns: List<CandidatePattern>
    ): DecisionAdvice? {
        val behavioralPatterns = learnedPatterns.filter {
            it.status == PatternStatus.ESTABLISHED &&
                    (it.domain.contains("FINANCE", ignoreCase = true) || it.domain.contains("BEHAVIOR", ignoreCase = true))
        }

        if (behavioralPatterns.isEmpty()) return null
        val topPattern = behavioralPatterns.maxByOrNull { it.confidence } ?: return null

        return DecisionAdvice(
            recommendedOption = options.firstOrNull() ?: "",
            confidence = topPattern.confidence,
            learnedPreferenceSummary = topPattern.description
        )
    }
}
