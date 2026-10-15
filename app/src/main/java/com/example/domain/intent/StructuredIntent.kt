package com.example.domain.intent

/**
 * Structured Intent schema matching the VYB Core Engineering Specification.
 */
data class StructuredIntent(
    val intent: String,
    val entities: Map<String, Any?> = emptyMap(),
    val preference: String? = null,
    val requiresAuthorization: Boolean = false,
    val rawPrompt: String = "",
    val missingParameters: List<String> = emptyList(),
    val clarificationQuestion: String? = null
) {
    companion object {
        const val INTENT_OPEN_APP = "OPEN_APP"
        const val INTENT_OPEN_URL = "OPEN_URL"
        const val INTENT_WEB_SEARCH = "WEB_SEARCH"
        const val INTENT_MAKE_CALL = "MAKE_CALL"
        const val INTENT_SEND_SMS = "SEND_SMS"
        const val INTENT_OPEN_WHATSAPP = "OPEN_WHATSAPP"
        const val INTENT_CREATE_REMINDER = "CREATE_REMINDER"
        const val INTENT_CREATE_CALENDAR_EVENT = "CREATE_CALENDAR_EVENT"
        const val INTENT_SET_ALARM = "SET_ALARM"
        const val INTENT_OPEN_MAPS = "OPEN_MAPS"
        const val INTENT_NAVIGATE = "NAVIGATE"
        const val INTENT_SAVE_MEMORY = "SAVE_MEMORY"
        const val INTENT_RETRIEVE_MEMORY = "RETRIEVE_MEMORY"
        const val INTENT_OPEN_RIDE_PROVIDER = "OPEN_RIDE_PROVIDER"
        const val INTENT_BOOK_RIDE = "BOOK_RIDE"
        const val INTENT_BOOK_TRANSPORT = "BOOK_TRANSPORT"
        const val INTENT_CHECK_AFFORDABILITY = "CHECK_AFFORDABILITY"
        const val INTENT_DAILY_BRIEFING = "DAILY_BRIEFING"
        const val INTENT_WHAT_MATTERS_TODAY = "WHAT_MATTERS_TODAY"
        const val INTENT_UPDATE_HOME_SCREEN = "UPDATE_HOME_SCREEN"
        const val INTENT_GENERAL_CONVERSATION = "GENERAL_CONVERSATION"
    }

    fun hasMissingRequiredInfo(): Boolean = missingParameters.isNotEmpty()
}
