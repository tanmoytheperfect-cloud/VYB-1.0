package com.example.domain.learning

data class LearnedObservation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionCategory: String, // "TRANSPORT", "APP_LAUNCH", "REMINDER"
    val contextSummary: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class LearnedBehaviorPattern(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val occurrenceCount: Int,
    val typicalTime: String,
    val isPromotedToRoutine: Boolean = false,
    val isUserConfirmed: Boolean = false
)

class PatternLearningEngine {
    private val observations = mutableListOf<LearnedObservation>()
    private val detectedPatterns = mutableListOf<LearnedBehaviorPattern>()

    init {
        // Default pattern discovered from repeated morning travel
        detectedPatterns.add(
            LearnedBehaviorPattern(
                title = "Morning School Transport",
                description = "School transport is normally arranged around 8:30 AM on weekdays.",
                occurrenceCount = 5,
                typicalTime = "08:30 AM",
                isPromotedToRoutine = false,
                isUserConfirmed = false
            )
        )
    }

    fun recordObservation(category: String, context: String) {
        observations.add(LearnedObservation(actionCategory = category, contextSummary = context))
    }

    fun getDetectedPatterns(): List<LearnedBehaviorPattern> = detectedPatterns.toList()

    fun promotePatternToRoutine(patternId: String): LearnedBehaviorPattern? {
        val index = detectedPatterns.indexOfFirst { it.id == patternId }
        if (index != -1) {
            val updated = detectedPatterns[index].copy(isPromotedToRoutine = true, isUserConfirmed = true)
            detectedPatterns[index] = updated
            return updated
        }
        return null
    }

    fun getPendingLearningPrompts(): List<String> {
        return detectedPatterns
            .filter { !it.isUserConfirmed }
            .map { "I've noticed you usually ${it.title.lowercase()} around ${it.typicalTime}. Would you like me to treat this as a regular routine?" }
    }
}
