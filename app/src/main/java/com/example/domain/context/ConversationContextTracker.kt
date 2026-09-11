package com.example.domain.context

import com.example.domain.brain.DecisionOption

data class ConversationContextState(
    val activeDomain: String? = null, // "CAB_TRANSPORT", "AMAZON_SHOPPING", "AFFORDABILITY", "TASK"
    val activeSubject: String? = null,
    val candidateOptions: List<DecisionOption> = emptyList(),
    val selectedOption: DecisionOption? = null,
    val pendingActionType: String? = null,
    val lastAssistantReply: String? = null,
    val lastUserQuery: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class ConversationContextTracker {

    private var currentState: ConversationContextState = ConversationContextState()

    fun updateContext(
        domain: String?,
        subject: String?,
        options: List<DecisionOption> = emptyList(),
        selected: DecisionOption? = null,
        pendingAction: String? = null,
        assistantReply: String? = null,
        userQuery: String? = null
    ) {
        currentState = ConversationContextState(
            activeDomain = domain ?: currentState.activeDomain,
            activeSubject = subject ?: currentState.activeSubject,
            candidateOptions = if (options.isNotEmpty()) options else currentState.candidateOptions,
            selectedOption = selected ?: currentState.selectedOption,
            pendingActionType = pendingAction ?: currentState.pendingActionType,
            lastAssistantReply = assistantReply ?: currentState.lastAssistantReply,
            lastUserQuery = userQuery ?: currentState.lastUserQuery,
            timestamp = System.currentTimeMillis()
        )
    }

    fun getActiveState(): ConversationContextState = currentState

    /**
     * Resolves follow-up queries like "cheapest", "book it", "second one", "cancel", "proceed".
     */
    fun resolveFollowUp(userInput: String): FollowUpResolution {
        val lower = userInput.lowercase().trim()

        if (lower == "cheapest" || lower.contains("find cheapest") || lower.contains("cheapest option")) {
            val options = currentState.candidateOptions
            if (options.isNotEmpty()) {
                val cheapest = options.minByOrNull { it.costInr } ?: options.first()
                currentState = currentState.copy(selectedOption = cheapest)
                return FollowUpResolution.OptionSelected(
                    domain = currentState.activeDomain ?: "TRANSPORT",
                    selectedOption = cheapest,
                    explanation = "Selected ${cheapest.name} as the lowest-fare option."
                )
            }
        }

        if (lower == "book it" || lower == "proceed" || lower == "confirm" || lower == "book the cab" || lower == "yes book it") {
            val target = currentState.selectedOption ?: currentState.candidateOptions.firstOrNull()
            return if (target != null) {
                FollowUpResolution.ProceedWithAction(
                    domain = currentState.activeDomain ?: "TRANSPORT",
                    targetOption = target,
                    actionType = "OPEN_RIDE_PROVIDER"
                )
            } else {
                FollowUpResolution.NoContext("I don't have an active booking draft to proceed with. Would you like me to find options first?")
            }
        }

        if (lower.contains("hold") || lower.contains("wait") || lower.contains("not yet") || lower.contains("cancel")) {
            currentState = currentState.copy(pendingActionType = null)
            return FollowUpResolution.HoldOrCancel("Understood, holding the action. I won't proceed.")
        }

        return FollowUpResolution.NotAFollowUp
    }

    fun clear() {
        currentState = ConversationContextState()
    }
}

sealed class FollowUpResolution {
    data class OptionSelected(val domain: String, val selectedOption: DecisionOption, val explanation: String) : FollowUpResolution()
    data class ProceedWithAction(val domain: String, val targetOption: DecisionOption, val actionType: String) : FollowUpResolution()
    data class HoldOrCancel(val message: String) : FollowUpResolution()
    data class NoContext(val message: String) : FollowUpResolution()
    object NotAFollowUp : FollowUpResolution()
}
