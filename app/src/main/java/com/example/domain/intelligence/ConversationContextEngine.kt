package com.example.domain.intelligence

/**
 * Tracks multi-turn conversational context across user requests (Part 10).
 */
data class ActiveConversationState(
    val lastIntentType: UniversalIntentType? = null,
    val lastTopic: String? = null,
    val lastMentionedPerson: String? = null,
    val lastMentionedPlace: String? = null,
    val activeTaskState: VybTaskExecutionState? = null,
    val turnCount: Int = 0,
    val latestUserStatement: String? = null,
    val recentStatements: List<String> = emptyList()
)

/**
 * Conversation Context Engine:
 * Resolves follow-up queries like "What about my work?", "And my family?", "What about my goals?",
 * distinguishes current conversation context from persistent memory,
 * and maintains recent conversational utterances for recency queries like "What did I just tell you?".
 */
class ConversationContextEngine {

    private var state = ActiveConversationState()
    private val statementHistory = mutableListOf<String>()

    fun getActiveState(): ActiveConversationState = state

    fun recordUserTurn(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isNotBlank()) {
            statementHistory.add(trimmed)
            state = state.copy(
                latestUserStatement = trimmed,
                recentStatements = statementHistory.takeLast(10)
            )
        }
    }

    fun getLatestUserStatement(excludingLastN: Int = 1): String? {
        if (statementHistory.size <= excludingLastN) {
            return statementHistory.lastOrNull()
        }
        return statementHistory[statementHistory.size - 1 - excludingLastN]
    }

    fun getAllRecentStatements(): List<String> = statementHistory.toList()

    fun setActiveTask(taskState: VybTaskExecutionState?) {
        state = state.copy(activeTaskState = taskState)
    }

    fun updateContext(
        intent: UniversalIntent,
        resolvedContext: ResolvedPersonalContext,
        newTaskState: VybTaskExecutionState? = state.activeTaskState
    ) {
        val topic = when (intent.intentType) {
            UniversalIntentType.PERSONAL_KNOWLEDGE_QUERY -> "PERSONAL_KNOWLEDGE"
            UniversalIntentType.PROFILE_QUERY -> "PROFILE"
            UniversalIntentType.PROJECT_QUERY -> "WORK"
            UniversalIntentType.FAMILY_QUERY -> "FAMILY"
            UniversalIntentType.GOAL_QUERY -> "GOALS"
            UniversalIntentType.ROUTINE_QUERY -> "ROUTINES"
            UniversalIntentType.BOOK_TRANSPORT, UniversalIntentType.TRANSPORT_REQUEST -> "TRANSPORT"
            UniversalIntentType.MEMORY_QUERY -> "MEMORY"
            else -> state.lastTopic
        }

        val person = resolvedContext.relatedPeople.firstOrNull() ?: state.lastMentionedPerson
        val place = resolvedContext.contextualDestination ?: state.lastMentionedPlace

        state = state.copy(
            lastIntentType = intent.intentType,
            lastTopic = topic,
            lastMentionedPerson = person,
            lastMentionedPlace = place,
            activeTaskState = newTaskState,
            turnCount = state.turnCount + 1
        )
    }

    companion object {
        val WORK_QUERY_ALIAS = UniversalIntentType.PROJECT_QUERY
    }
}
