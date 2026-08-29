package com.example.domain.actions

import android.Manifest

/**
 * Authorization levels defined in the VYB Core Engineering Specification.
 */
enum class AuthorizationLevel {
    READ_ONLY,     // May execute automatically without asking
    LOW_RISK,      // May execute automatically or notify
    SENSITIVE,     // Must ask user confirmation
    FINANCIAL,     // Must always require explicit authorization
    COMMUNICATION, // Must require authorization before sending
    BOOKING        // Must require authorization before final booking/payment
}

/**
 * Action Definition describing requirements and capabilities for an action.
 */
data class ActionDefinition(
    val actionType: String,
    val name: String,
    val description: String,
    val requiredParameters: List<String>,
    val optionalParameters: List<String> = emptyList(),
    val requiredPermissions: List<String> = emptyList(),
    val authorizationLevel: AuthorizationLevel = AuthorizationLevel.READ_ONLY
)

/**
 * Central registry of all supported actions.
 */
object ActionRegistry {

    val ACTION_OPEN_APP = ActionDefinition(
        actionType = "OPEN_APP",
        name = "Open Application",
        description = "Launches an installed application via Android package manager",
        requiredParameters = listOf("app_name"),
        authorizationLevel = AuthorizationLevel.READ_ONLY
    )

    val ACTION_OPEN_URL = ActionDefinition(
        actionType = "OPEN_URL",
        name = "Open Web URL",
        description = "Opens a web link in the default browser",
        requiredParameters = listOf("url"),
        authorizationLevel = AuthorizationLevel.READ_ONLY
    )

    val ACTION_WEB_SEARCH = ActionDefinition(
        actionType = "WEB_SEARCH",
        name = "Web Search",
        description = "Executes an Android system web search",
        requiredParameters = listOf("query"),
        authorizationLevel = AuthorizationLevel.READ_ONLY
    )

    val ACTION_MAKE_CALL = ActionDefinition(
        actionType = "MAKE_CALL",
        name = "Make Phone Call",
        description = "Initiates a phone call via system dialer",
        requiredParameters = listOf("target"),
        requiredPermissions = listOf(Manifest.permission.CALL_PHONE),
        authorizationLevel = AuthorizationLevel.COMMUNICATION
    )

    val ACTION_SEND_SMS = ActionDefinition(
        actionType = "SEND_SMS",
        name = "Send SMS Message",
        description = "Composes and sends an SMS to a specified recipient",
        requiredParameters = listOf("recipient", "message"),
        requiredPermissions = listOf(Manifest.permission.SEND_SMS),
        authorizationLevel = AuthorizationLevel.COMMUNICATION
    )

    val ACTION_OPEN_WHATSAPP = ActionDefinition(
        actionType = "OPEN_WHATSAPP",
        name = "WhatsApp Message",
        description = "Opens WhatsApp composer with pre-filled message text",
        requiredParameters = listOf("message"),
        authorizationLevel = AuthorizationLevel.COMMUNICATION
    )

    val ACTION_CREATE_REMINDER = ActionDefinition(
        actionType = "CREATE_REMINDER",
        name = "Create Reminder",
        description = "Schedules an alarm/reminder on the device and local database",
        requiredParameters = listOf("title", "time"),
        authorizationLevel = AuthorizationLevel.LOW_RISK
    )

    val ACTION_CREATE_CALENDAR_EVENT = ActionDefinition(
        actionType = "CREATE_CALENDAR_EVENT",
        name = "Create Calendar Event",
        description = "Inserts a calendar event into the system calendar",
        requiredParameters = listOf("title", "startTime"),
        authorizationLevel = AuthorizationLevel.LOW_RISK
    )

    val ACTION_SET_ALARM = ActionDefinition(
        actionType = "SET_ALARM",
        name = "Set Alarm",
        description = "Configures a device clock alarm for a specific time",
        requiredParameters = listOf("time"),
        authorizationLevel = AuthorizationLevel.LOW_RISK
    )

    val ACTION_OPEN_MAPS = ActionDefinition(
        actionType = "OPEN_MAPS",
        name = "Open Maps",
        description = "Launches Google Maps with search location",
        requiredParameters = listOf("query"),
        authorizationLevel = AuthorizationLevel.READ_ONLY
    )

    val ACTION_NAVIGATE = ActionDefinition(
        actionType = "NAVIGATE",
        name = "Turn-by-turn Navigation",
        description = "Launches Google Maps turn-by-turn navigation to destination",
        requiredParameters = listOf("destination"),
        authorizationLevel = AuthorizationLevel.READ_ONLY
    )

    val ACTION_SAVE_MEMORY = ActionDefinition(
        actionType = "SAVE_MEMORY",
        name = "Save Memory",
        description = "Persists a verified fact or preference in Room Database",
        requiredParameters = listOf("key", "value"),
        authorizationLevel = AuthorizationLevel.READ_ONLY
    )

    val ACTION_RETRIEVE_MEMORY = ActionDefinition(
        actionType = "RETRIEVE_MEMORY",
        name = "Retrieve Memory",
        description = "Queries stored facts and preferences from Room Database",
        requiredParameters = listOf("query"),
        authorizationLevel = AuthorizationLevel.READ_ONLY
    )

    val ACTION_OPEN_RIDE_PROVIDER = ActionDefinition(
        actionType = "OPEN_RIDE_PROVIDER",
        name = "Open Ride Provider",
        description = "Launches an installed ride hailing app on the device to view live options and complete trip booking",
        requiredParameters = listOf("destination"),
        authorizationLevel = AuthorizationLevel.READ_ONLY
    )

    val ACTION_BOOK_RIDE = ActionDefinition(
        actionType = "BOOK_RIDE",
        name = "Book Ride",
        description = "Direct programmatic ride booking via live provider API",
        requiredParameters = listOf("destination", "provider"),
        authorizationLevel = AuthorizationLevel.BOOKING
    )

    val ACTION_BOOK_TRANSPORT = ActionDefinition(
        actionType = "BOOK_TRANSPORT",
        name = "Book Ride / Transport",
        description = "Prepares and dispatches transport requests",
        requiredParameters = listOf("destination"),
        authorizationLevel = AuthorizationLevel.BOOKING
    )

    val allActions = listOf(
        ACTION_OPEN_APP,
        ACTION_OPEN_URL,
        ACTION_WEB_SEARCH,
        ACTION_MAKE_CALL,
        ACTION_SEND_SMS,
        ACTION_OPEN_WHATSAPP,
        ACTION_CREATE_REMINDER,
        ACTION_CREATE_CALENDAR_EVENT,
        ACTION_SET_ALARM,
        ACTION_OPEN_MAPS,
        ACTION_NAVIGATE,
        ACTION_SAVE_MEMORY,
        ACTION_RETRIEVE_MEMORY,
        ACTION_OPEN_RIDE_PROVIDER,
        ACTION_BOOK_RIDE,
        ACTION_BOOK_TRANSPORT
    )

    fun find(actionType: String): ActionDefinition? {
        return allActions.find { it.actionType.equals(actionType, ignoreCase = true) }
    }
}
