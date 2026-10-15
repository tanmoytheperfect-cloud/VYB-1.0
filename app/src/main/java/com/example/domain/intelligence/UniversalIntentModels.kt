package com.example.domain.intelligence

import com.example.domain.capability.CapabilityType
import com.example.domain.genesis.InformationClassification
import com.example.domain.permission.VybPermissionType
import java.util.UUID

/**
 * Risk classification for intents and action execution.
 */
enum class RiskLevel(val label: String, val requiresStrictConfirmation: Boolean) {
    LOW("Low Risk", false),
    MEDIUM("Medium Risk", false),
    HIGH("High Risk", true),
    CRITICAL("Critical Risk", true)
}

/**
 * Autonomy levels configured by the owner (Part 27).
 */
enum class AutonomyLevel(val level: Int, val title: String, val description: String) {
    LEVEL_0_CHAT_ONLY(0, "Level 0: Chat Only", "No device mutations or external app launches."),
    LEVEL_1_SUGGEST(1, "Level 1: Suggest", "Suggests actions and drafts without executing."),
    LEVEL_2_PREPARE(2, "Level 2: Prepare", "Prepares actions, opens deep links, fills parameters where supported."),
    LEVEL_3_ACT_WITH_CONFIRMATION(3, "Level 3: Act with Confirmation", "Default for sensitive and external operations; requires explicit owner prompt."),
    LEVEL_4_LIMITED_AUTONOMY(4, "Level 4: Limited Autonomy", "Executes verified routines autonomously within predefined safety boundaries.")
}

/**
 * Universal Intent Type categories recognized by the Intelligence Orchestrator (Part 9 & Part 41).
 */
enum class UniversalIntentType(val id: String, val displayName: String, val defaultRisk: RiskLevel) {
    // 1. General & Dialogue
    GENERAL_CONVERSATION("GENERAL_CONVERSATION", "General Conversation", RiskLevel.LOW),
    UNKNOWN_INTENT("UNKNOWN_INTENT", "Unknown / Ambiguous Intent", RiskLevel.LOW),

    // 2. Personal Identity & Profile Knowledge (Part 8 & 11)
    PERSONAL_KNOWLEDGE_QUERY("PERSONAL_KNOWLEDGE_QUERY", "Personal Knowledge Query", RiskLevel.LOW),
    PROFILE_QUERY("PROFILE_QUERY", "Profile Inspection", RiskLevel.LOW),
    PROFILE_UPDATE("PROFILE_UPDATE", "Profile Update", RiskLevel.LOW),
    CONTEXT_QUERY("CONTEXT_QUERY", "Context Query", RiskLevel.LOW),

    // 3. Memory Lifecycle (Part 12)
    MEMORY_QUERY("MEMORY_QUERY", "Memory Query", RiskLevel.LOW),
    MEMORY_WRITE("MEMORY_WRITE", "Memory Write", RiskLevel.LOW),
    MEMORY_DELETE("MEMORY_DELETE", "Memory Deletion", RiskLevel.LOW),

    // 4. Life Dimensions
    ROUTINE_QUERY("ROUTINE_QUERY", "Routine & Habit Query", RiskLevel.LOW),
    TASK_QUERY("TASK_QUERY", "Task & Priority Query", RiskLevel.LOW),
    CALENDAR_QUERY("CALENDAR_QUERY", "Calendar & Schedule Query", RiskLevel.LOW),
    GOAL_QUERY("GOAL_QUERY", "Personal Goal Query", RiskLevel.LOW),
    PROJECT_QUERY("PROJECT_QUERY", "Project Query", RiskLevel.LOW),
    FAMILY_QUERY("FAMILY_QUERY", "Family Context Query", RiskLevel.LOW),
    PEOPLE_QUERY("PEOPLE_QUERY", "Important People Query", RiskLevel.LOW),
    PLACE_QUERY("PLACE_QUERY", "Important Places Query", RiskLevel.LOW),
    FINANCE_QUERY("FINANCE_QUERY", "Financial Context Query", RiskLevel.MEDIUM),
    SHOPPING_QUERY("SHOPPING_QUERY", "Shopping Query", RiskLevel.LOW),
    RESEARCH_QUERY("RESEARCH_QUERY", "Research Query", RiskLevel.LOW),
    DOCUMENT_QUERY("DOCUMENT_QUERY", "Document Query", RiskLevel.LOW),
    IMAGE_QUERY("IMAGE_QUERY", "Visual & Image Query", RiskLevel.LOW),
    VOICE_QUERY("VOICE_QUERY", "Voice & Microphone Query", RiskLevel.LOW),

    // 5. Device & Telemetry (Part 29)
    DEVICE_QUERY("DEVICE_QUERY", "Device Telemetry & Status", RiskLevel.LOW),
    PERMISSION_QUERY("PERMISSION_QUERY", "Permissions & Access Query", RiskLevel.LOW),

    // 6. Action Requests & Execution
    APP_LAUNCH("APP_LAUNCH", "Launch Application", RiskLevel.LOW),
    TRANSPORT_REQUEST("TRANSPORT_REQUEST", "Cab / Ride Dispatch", RiskLevel.HIGH),
    MESSAGE_REQUEST("MESSAGE_REQUEST", "Send Message / SMS", RiskLevel.HIGH),
    EMAIL_REQUEST("EMAIL_REQUEST", "Email Dispatch", RiskLevel.HIGH),
    REMINDER_REQUEST("REMINDER_REQUEST", "Create Reminder / Alarm", RiskLevel.LOW),
    PLANNING_REQUEST("PLANNING_REQUEST", "Multi-Step Planning", RiskLevel.LOW),
    ACTION_REQUEST("ACTION_REQUEST", "Execute Action", RiskLevel.MEDIUM),
    AUTOMATION_REQUEST("AUTOMATION_REQUEST", "Automation Trigger", RiskLevel.MEDIUM),

    // 7. Active Task Control (Part 19 & 20)
    ACTION_CONTINUATION("ACTION_CONTINUATION", "Continue Active Task", RiskLevel.MEDIUM),
    ACTION_CORRECTION("ACTION_CORRECTION", "User Correction to Active Task", RiskLevel.MEDIUM),
    ACTION_STATUS_QUERY("ACTION_STATUS_QUERY", "Active Task Status", RiskLevel.LOW),

    // Backward-compatibility aliases
    CREATE_REMINDER("CREATE_REMINDER", "Create Reminder / Alarm", RiskLevel.LOW),
    CALENDAR_SCHEDULE("CALENDAR_SCHEDULE", "Schedule Calendar Event", RiskLevel.MEDIUM),
    BOOK_TRANSPORT("BOOK_TRANSPORT", "Book Cab / Transit Dispatch", RiskLevel.HIGH),
    COMMUNICATION_DISPATCH("COMMUNICATION_DISPATCH", "Send Message / Initiate Call", RiskLevel.HIGH),
    RESEARCH_TOPIC("RESEARCH_TOPIC", "Research & Deep Synthesis", RiskLevel.LOW),
    FINANCE_ANALYSIS("FINANCE_ANALYSIS", "Financial Planning & Affordability", RiskLevel.MEDIUM),
    DEVICE_CONTROL("DEVICE_CONTROL", "Device Hardware & App Settings", RiskLevel.MEDIUM),
    PERSONAL_RETRIEVAL("PERSONAL_RETRIEVAL", "Retrieve Personal Memory / Profile", RiskLevel.LOW),
    SAVE_MEMORY("SAVE_MEMORY", "Save Personal Memory / Vault Item", RiskLevel.LOW),
    DAILY_BRIEFING("DAILY_BRIEFING", "Daily Briefing & Priorities", RiskLevel.LOW),
    TASK_MANAGEMENT("TASK_MANAGEMENT", "Task & Goal Orchestration", RiskLevel.LOW),
    AUTOMATION_ROUTINE("AUTOMATION_ROUTINE", "Automation & Routine Trigger", RiskLevel.MEDIUM),
    CODE_CALCULATION("CODE_CALCULATION", "Computation & Code Generation", RiskLevel.LOW),
    GENERAL_REASONING("GENERAL_REASONING", "Contextual Dialogue & Reasoning", RiskLevel.LOW)
}

/**
 * Action Execution States (Part 25).
 * Never converts OPENED into COMPLETED unless the intended action was only opening.
 */
enum class ActionExecutionStatus {
    PENDING,
    ATTEMPTING,
    SUCCESS,
    FAILED,
    BLOCKED,
    REQUIRES_USER,
    UNVERIFIED
}

/**
 * Active Task Execution State (Part 19 & 20).
 * Persists active multi-step workflows across conversational turns and receives user corrections.
 */
data class VybTaskExecutionState(
    val taskId: String = UUID.randomUUID().toString(),
    val intent: UniversalIntentType,
    val service: String? = null,
    val currentStep: String = "",
    val completedSteps: List<String> = emptyList(),
    val pendingSteps: List<String> = emptyList(),
    val failedSteps: List<String> = emptyList(),
    val entities: Map<String, Any?> = emptyMap(),
    val origin: String? = null,
    val destination: String? = null,
    val time: String? = null,
    val confirmationState: String = "NOT_REQUIRED",
    val executionStatus: ActionExecutionStatus = ActionExecutionStatus.PENDING,
    val verificationStatus: String = "UNVERIFIED",
    val lastAction: String = "",
    val lastFailure: String? = null,
    val userCorrection: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Action Ledger Entry (Part 28).
 */
data class ActionLedgerEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val intent: String,
    val task: String,
    val plan: String? = null,
    val agent: String? = null,
    val tool: String? = null,
    val permission: String? = null,
    val confirmation: String = "AUTONOMOUS",
    val action: String,
    val result: String,
    val verification: String,
    val error: String? = null
)

/**
 * Lifecycle state of a Universal Intent as it traverses the Intelligence Pipeline.
 */
enum class IntentStatus {
    PARSED,
    CONTEXT_RESOLVED,
    MEMORY_RETRIEVED,
    CAPABILITY_DISCOVERED,
    PLANNING,
    AWAITING_CONFIRMATION,
    CONFIRMED,
    EXECUTING,
    VERIFYING,
    COMPLETED,
    FAILED,
    REJECTED
}

/**
 * Resolved personal context package before execution.
 */
data class ResolvedPersonalContext(
    val ownerName: String = "",
    val callingTitle: String = "Sir",
    val habitualPreferences: Map<String, String> = emptyMap(),
    val contextualDestination: String? = null,
    val contextualTime: String? = null,
    val contextualService: String? = null,
    val contextualBudget: Double? = null,
    val relatedPeople: List<String> = emptyList(),
    val confidenceScore: Float = 1.0f,
    val isSufficient: Boolean = true,
    val clarificationQuestion: String? = null,
    val resolutionNotes: List<String> = emptyList()
)

/**
 * Verification details after tool or plan step execution.
 */
data class VerificationResult(
    val isVerified: Boolean,
    val method: String,
    val proofDescription: String,
    val verifiedAt: Long = System.currentTimeMillis(),
    val recoverySuggestion: String? = null
)

/**
 * Universal Structured Intent representing any user command or proactive prompt.
 */
data class UniversalIntent(
    val intentId: String = UUID.randomUUID().toString(),
    val rawInput: String,
    val intentType: UniversalIntentType,
    val entities: Map<String, Any?> = emptyMap(),
    val resolvedContext: ResolvedPersonalContext = ResolvedPersonalContext(),
    val requiredCapabilities: List<String> = emptyList(),
    val requiredPermissions: List<VybPermissionType> = emptyList(),
    val riskLevel: RiskLevel = intentType.defaultRisk,
    val confirmationRequired: Boolean = riskLevel.requiresStrictConfirmation,
    val assignedAgentId: String? = null,
    val executionPlan: ActionPlan? = null,
    val status: IntentStatus = IntentStatus.PARSED,
    val result: Any? = null,
    val verification: VerificationResult? = null,
    val clarificationQuestion: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Status of individual plan step.
 */
enum class StepStatus {
    PENDING,
    RUNNING,
    CONFIRMATION_REQUIRED,
    COMPLETED,
    FAILED,
    SKIPPED
}

/**
 * Single step inside an Action Plan.
 */
data class PlanStep(
    val stepIndex: Int,
    val title: String,
    val toolName: String,
    val parameters: Map<String, Any?> = emptyMap(),
    val status: StepStatus = StepStatus.PENDING,
    val requiresConfirmation: Boolean = false,
    val output: String? = null,
    val verification: VerificationResult? = null,
    val canRollback: Boolean = false,
    val rollbackAction: String? = null
)

/**
 * Status of multi-step Action Plan.
 */
enum class PlanStatus {
    DRAFT,
    AWAITING_CONFIRMATION,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Multi-step Action Plan produced by Agents for complex intents.
 */
data class ActionPlan(
    val planId: String = UUID.randomUUID().toString(),
    val goal: String,
    val steps: List<PlanStep>,
    val currentStepIndex: Int = 0,
    val status: PlanStatus = PlanStatus.DRAFT,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val createdAt: Long = System.currentTimeMillis()
)
