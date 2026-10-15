package com.example.domain.cognitive.engine

import com.example.domain.cognitive.models.CandidatePattern
import com.example.domain.cognitive.models.PatternStatus
import java.util.Calendar

data class CognitivePrediction(
    val patternId: String,
    val predictedSubject: String,
    val predictedDomain: String,
    val anticipatedTime: String,
    val confidence: Float,
    val reason: String
)

class PredictionEngine {

    fun predictUpcomingNeeds(
        currentDayOfWeek: Int,
        patterns: List<CandidatePattern>
    ): List<CognitivePrediction> {
        val established = patterns.filter { it.status == PatternStatus.ESTABLISHED }
        val predictions = mutableListOf<CognitivePrediction>()

        for (p in established) {
            if (currentDayOfWeek in p.expectedDays) {
                predictions.add(
                    CognitivePrediction(
                        patternId = p.patternId,
                        predictedSubject = p.subject,
                        predictedDomain = p.domain,
                        anticipatedTime = p.expectedTimeRange,
                        confidence = p.confidence,
                        reason = p.explanation
                    )
                )
            }
        }
        return predictions
    }
}
