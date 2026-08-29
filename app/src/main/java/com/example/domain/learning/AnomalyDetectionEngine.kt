package com.example.domain.learning

import com.example.domain.context.DayCategory
import com.example.domain.context.VybCurrentContext
import java.util.Calendar

data class DetectedAnomaly(
    val title: String,
    val description: String,
    val confidence: Float,
    val suggestedQuestion: String? = null,
    val severity: String = "INFO" // INFO, ATTENTION, URGENT
)

class AnomalyDetectionEngine {

    fun detectAnomalies(context: VybCurrentContext): DetectedAnomaly? {
        val cal = Calendar.getInstance().apply { timeInMillis = context.currentTimeMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayCategory = context.dayCategory

        // Example: Workday morning past 10:00 AM with no activity or unusual silence
        if (dayCategory == DayCategory.WORKDAY && hour >= 10 && context.recentEvents.size < 2 && context.confidenceScore > 0.7f) {
            return DetectedAnomaly(
                title = "Workday Routine Shift",
                description = "Your regular workday schedule appears quiet today.",
                confidence = 0.65f,
                suggestedQuestion = "Your usual workday routine looks different today. Is today a leave or travel day?",
                severity = "INFO"
            )
        }

        return null
    }
}
