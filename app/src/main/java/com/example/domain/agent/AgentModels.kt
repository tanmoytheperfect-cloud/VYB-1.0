package com.example.domain.agent

import com.example.domain.actions.ActionLevel
import com.example.domain.actions.ActionStatus
import com.example.domain.actions.VybAction

/**
 * Core Agent Confidence / Knowledge Status
 */
enum class KnowledgeStatus {
    KNOWN,
    INFERRED,
    UNKNOWN,
    REQUIRES_PERMISSION,
    UNAVAILABLE
}

/**
 * Agent Operating State
 */
enum class AgentState {
    IDLE,
    UNDERSTANDING_INTENT,
    RETRIEVING_CONTEXT,
    RETRIEVING_MEMORY,
    DISCOVERING_TOOLS,
    REASONING_AND_PLANNING,
    CHECKING_PERMISSIONS,
    AWAITING_USER_AUTHORIZATION,
    EXECUTING_ACTION,
    VERIFYING_RESULT,
    UPDATING_MEMORY,
    COMPLETED,
    FAILED
}

/**
 * Action Safety Risk Levels
 */
enum class SafetyRiskLevel(val code: Int, val title: String, val requiresConfirmation: Boolean) {
    LOW(0, "Low (Informational / Read / Launch)", false),
    MEDIUM(1, "Medium (Draft / Local Change / Reminder)", false),
    HIGH(2, "High (Booking / Payment / Data Deletion)", true)
}

/**
 * Intent Category recognized by the Agent Loop
 */
enum class AgentIntentCategory {
    DAILY_PRIORITIES,
    WHAT_SHOULD_I_DO_NOW,
    TRANSPORT_COORDINATION,
    DELIVERY_TRACKING,
    FINANCIAL_AFFORDABILITY,
    SHOPPING_RESEARCH,
    REMINDER_AND_TASK,
    APP_LAUNCH,
    MEMORY_STORE,
    MEMORY_RETRIEVE,
    MEMORY_FORGET,
    ACCOUNT_CONNECTION_HELP,
    CALENDAR_SCHEDULE,
    DEVICE_ALARM,
    GENERAL_QUERY
}

/**
 * Structured Agent Intent
 */
data class AgentIntent(
    val category: AgentIntentCategory,
    val summary: String,
    val targetSubject: String? = null,
    val targetPerson: String? = null,
    val targetAmountInr: Double? = null,
    val targetDateOrTime: String? = null,
    val rawPrompt: String = "",
    val confidence: Float = 1.0f,
    val missingParameters: List<String> = emptyList(),
    val clarificationNeeded: String? = null
)

/**
 * Comprehensive Agent Context
 */
data class AgentContext(
    val ownerName: String = "Owner",
    val city: String = "Kolkata",
    val currentTimeFormatted: String = "",
    val currentDateFormatted: String = "",
    val dayOfWeek: String = "",
    val timeOfDay: String = "Day",
    val hourOfDay: Int = 12,
    val minuteOfHour: Int = 0,
    val isMorning: Boolean = true,
    val monthlyIncome: Double = 0.0,
    val monthlySavingsTarget: Double = 0.0,
    val activeDiscretionaryBuffer: Double = 0.0,
    val familyMembers: List<String> = emptyList(),
    val sonSchoolCommuteTime: String? = null,
    val sonSchoolName: String? = null,
    val activeTasksCount: Int = 0,
    val activeTasksSummaries: List<String> = emptyList(),
    val retrievedMemories: List<AgentMemoryItem> = emptyList(),
    val availableTools: List<String> = emptyList(),
    val isAmazonConnected: Boolean = false,
    val isRideApiConnected: Boolean = false
)

/**
 * Structured Memory Item for the Agent
 */
data class AgentMemoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val category: String, // FACT, PREFERENCE, ROUTINE, PERSON, GOAL, FINANCIAL_CONTEXT, DECISION
    val key: String,
    val content: String,
    val source: String = "User Conversation",
    val confidence: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val importance: Int = 1,
    val isUserEditable: Boolean = true
)

/**
 * Plan Step formulated by the Agent
 */
data class AgentStep(
    val stepNumber: Int,
    val title: String,
    val toolId: String?,
    val expectedOutcome: String,
    val riskLevel: SafetyRiskLevel,
    var status: ActionStatus = ActionStatus.PENDING,
    var resultSummary: String? = null
)

/**
 * Execution Plan formulated by the Agent
 */
data class AgentPlan(
    val planId: String = java.util.UUID.randomUUID().toString(),
    val intent: AgentIntent,
    val rationale: String,
    val steps: List<AgentStep> = emptyList(),
    val requiredAuthorizations: List<String> = emptyList(),
    val overallRiskLevel: SafetyRiskLevel = SafetyRiskLevel.LOW
)

/**
 * Unified Agent Response returned to the user
 */
data class AgentResponse(
    val conversationalReply: String,
    val intent: AgentIntent,
    val plan: AgentPlan,
    val state: AgentState,
    val knowledgeStatus: KnowledgeStatus,
    val pendingAction: VybAction? = null,
    val executionResult: String? = null,
    val diagnosticsSummary: Map<String, String> = emptyMap(),
    val suggestedActions: List<String> = emptyList()
)
