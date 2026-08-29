package com.example.domain.proactive

import com.example.domain.context.HomeScreenMode
import com.example.domain.context.InactivityState
import com.example.domain.context.VybCurrentContext
import com.example.domain.learning.DetectedAnomaly

enum class ProactiveActionLevel {
    DO_NOTHING,
    INFORM,
    SUGGEST,
    PREPARE,
    ASK_FOR_AUTHORIZATION,
    EXECUTE_AUTOMATICALLY
}

data class ProactiveDecision(
    val level: ProactiveActionLevel,
    val title: String,
    val description: String,
    val suggestedActionLabel: String? = null,
    val actionPayload: Map<String, String> = emptyMap(),
    val confidence: Float = 0.8f,
    val priority: NotificationPriority = NotificationPriority.NORMAL
)

class ProactiveDecisionEngine(
    private val interruptionPolicy: InterruptionPolicyEngine = InterruptionPolicyEngine()
) {

    fun evaluateProactiveNeed(
        context: VybCurrentContext,
        anomaly: DetectedAnomaly? = null
    ): ProactiveDecision {
        // 1. If user is in sleep/inactivity state, DO NOTHING
        if (context.inactivityState == InactivityState.PROBABLE_SLEEP_RESTING || context.operatingMode == HomeScreenMode.NIGHT) {
            return ProactiveDecision(
                level = ProactiveActionLevel.DO_NOTHING,
                title = "Quiet Mode Active",
                description = "Suppressing proactive interruptions during resting hours.",
                priority = NotificationPriority.LOW
            )
        }

        // 2. If an anomaly is detected with sufficient confidence
        if (anomaly != null && anomaly.confidence >= 0.6f) {
            return ProactiveDecision(
                level = ProactiveActionLevel.INFORM,
                title = anomaly.title,
                description = anomaly.description,
                suggestedActionLabel = anomaly.suggestedQuestion,
                priority = NotificationPriority.NORMAL
            )
        }

        // 3. Active routine match
        val routine = context.activeRoutine
        if (routine != null && routine.confidenceScore >= 0.70f) {
            return when (routine.proactiveActionType) {
                "PREPARE" -> ProactiveDecision(
                    level = ProactiveActionLevel.PREPARE,
                    title = routine.title,
                    description = "${routine.description} Ready for quick access.",
                    suggestedActionLabel = "Open ${routine.title}",
                    actionPayload = mapOf("routineId" to routine.routineId),
                    confidence = routine.confidenceScore,
                    priority = NotificationPriority.NORMAL
                )
                "SURFACE_BRIEFING" -> ProactiveDecision(
                    level = ProactiveActionLevel.INFORM,
                    title = "Morning Routine Active",
                    description = context.proactiveBriefingSummary,
                    suggestedActionLabel = "View Morning Plan",
                    confidence = routine.confidenceScore,
                    priority = NotificationPriority.NORMAL
                )
                else -> ProactiveDecision(
                    level = ProactiveActionLevel.INFORM,
                    title = routine.title,
                    description = routine.description,
                    confidence = routine.confidenceScore,
                    priority = NotificationPriority.LOW
                )
            }
        }

        // 4. Calendar upcoming commitment
        if (context.calendarCommitments.isNotEmpty()) {
            val nextMeeting = context.calendarCommitments.first()
            return ProactiveDecision(
                level = ProactiveActionLevel.INFORM,
                title = "Upcoming Calendar Item",
                description = nextMeeting,
                suggestedActionLabel = "View Calendar",
                priority = NotificationPriority.NORMAL
            )
        }

        // Default state
        return ProactiveDecision(
            level = ProactiveActionLevel.INFORM,
            title = "Personal OS Ready",
            description = context.proactiveBriefingSummary,
            suggestedActionLabel = "Ask VYB",
            priority = NotificationPriority.LOW
        )
    }
}
