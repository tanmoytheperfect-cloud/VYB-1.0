package com.example.domain.orchestrator

import android.content.Context
import com.example.data.local.dao.LifeVaultDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.TaskDao
import com.example.data.model.*
import com.example.data.repository.GeminiRepository
import com.example.domain.actions.*
import com.example.domain.agent.AgentEngine
import com.example.domain.agent.AgentIntentCategory
import com.example.domain.brain.VybCognitiveBrain
import com.example.domain.capability.CapabilityEngine
import com.example.domain.capability.CapabilityState
import com.example.domain.capability.CapabilityType
import com.example.domain.context.ContextEngine
import com.example.domain.intent.IntentParser
import com.example.domain.intent.StructuredIntent
import com.example.domain.state.VybOperatingState
import com.example.domain.vault.PersonalLifeVaultEngine
import com.example.domain.vault.VaultOperationResult

data class VybDiagnostics(
    val intent: String,
    val contextSummary: List<String>,
    val capabilitiesChecked: List<CapabilityState>,
    val toolSelected: String?,
    val actionState: ActionStatus,
    val nextAction: String,
    val isVerified: Boolean = true,
    val structuredIntent: StructuredIntent? = null,
    val executionResult: ActionResult? = null
)

data class OrchestratorResult(
    val conversationalResponse: String,
    val intentPayload: VybIntentPayload,
    val diagnostics: VybDiagnostics,
    val action: VybAction? = null,
    val executionResult: ActionResult? = null,
    val operatingState: VybOperatingState = VybOperatingState.SUCCESS
)

class VybOrchestrator(
    private val memoryDao: MemoryDao? = null,
    private val taskDao: TaskDao? = null,
    private val geminiRepository: GeminiRepository = GeminiRepository(),
    private val lifeVaultDao: LifeVaultDao? = null
) {
    val cognitiveBrain = VybCognitiveBrain()
    val vybBrain = com.example.domain.brain.VybBrain(memoryDao, taskDao, lifeVaultDao, geminiRepository = geminiRepository)
    private val contextEngine = ContextEngine(memoryDao, taskDao)
    val actionEngine = ActionEngine(memoryDao, taskDao)
    val personalLifeVaultEngine = lifeVaultDao?.let { PersonalLifeVaultEngine(it) }
    val agentEngine = AgentEngine(memoryDao, taskDao, lifeVaultDao, geminiRepository = geminiRepository)

    suspend fun processMessage(input: String, appContext: Context?): OrchestratorResult {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        // Handle active pending conflict resolution from conversation
        if (personalLifeVaultEngine != null && lifeVaultDao != null) {
            val pendingConflicts = lifeVaultDao.getAllVaultItems().filter { it.isPendingConflict }
            if (pendingConflicts.isNotEmpty()) {
                val targetConflict = pendingConflicts.first()
                if (lower.contains("replace") || lower.contains("update") || lower == "yes" || lower == "yes please" || lower == "use new") {
                    val resolved = personalLifeVaultEngine.resolveConflict(targetConflict.id, acceptNew = true)
                    val reply = "Replaced previous ${targetConflict.key} (\"${resolved?.previousValue}\") with updated value: \"${resolved?.value}\" [Source: USER_CONFIRMED]."
                    val payload = VybIntentPayload(
                        intentType = "TASK",
                        title = "Personal Life Vault Updated",
                        summary = "${targetConflict.key}: ${resolved?.value}",
                        suggestedFollowUps = listOf("View Personal Life Vault", "What else do you know about me?", "Today's briefing")
                    )
                    return OrchestratorResult(
                        conversationalResponse = reply,
                        intentPayload = payload,
                        diagnostics = VybDiagnostics(
                            intent = "VAULT_CONFLICT_RESOLVED",
                            contextSummary = listOf("Updated: ${targetConflict.key}"),
                            capabilitiesChecked = emptyList(),
                            toolSelected = "Personal Life Vault Engine",
                            actionState = ActionStatus.COMPLETED,
                            nextAction = "Resolved and persisted"
                        ),
                        operatingState = VybOperatingState.SUCCESS
                    )
                } else if (lower.contains("keep") || lower == "no" || lower == "cancel" || lower.contains("don't replace")) {
                    val resolved = personalLifeVaultEngine.resolveConflict(targetConflict.id, acceptNew = false)
                    val reply = "Kept existing ${targetConflict.key}: \"${resolved?.value}\". Cancelled pending change."
                    val payload = VybIntentPayload(
                        intentType = "TASK",
                        title = "Vault Maintained",
                        summary = "Kept ${targetConflict.key}: ${resolved?.value}",
                        suggestedFollowUps = listOf("View Personal Life Vault", "Add another fact", "Today's briefing")
                    )
                    return OrchestratorResult(
                        conversationalResponse = reply,
                        intentPayload = payload,
                        diagnostics = VybDiagnostics(
                            intent = "VAULT_CONFLICT_CANCELLED",
                            contextSummary = listOf("Kept: ${targetConflict.key}"),
                            capabilitiesChecked = emptyList(),
                            toolSelected = "Personal Life Vault Engine",
                            actionState = ActionStatus.COMPLETED,
                            nextAction = "Preserved original fact"
                        ),
                        operatingState = VybOperatingState.SUCCESS
                    )
                }
            }

            // Check if input is a vault fact statement, modification, or forget command
            val vaultResult = personalLifeVaultEngine.processConversationalMemory(trimmed)
            when (vaultResult) {
                is VaultOperationResult.Stored -> {
                    val payload = VybIntentPayload(
                        intentType = "TASK",
                        title = "Personal Life Vault Saved",
                        summary = "${vaultResult.item.key}: ${vaultResult.item.value}",
                        suggestedFollowUps = listOf("View Personal Life Vault", "What else do you remember?", "Today's briefing")
                    )
                    return OrchestratorResult(
                        conversationalResponse = vaultResult.message,
                        intentPayload = payload,
                        diagnostics = VybDiagnostics(
                            intent = "VAULT_FACT_STORED",
                            contextSummary = listOf("${vaultResult.item.category}: ${vaultResult.item.key}"),
                            capabilitiesChecked = emptyList(),
                            toolSelected = "Personal Life Vault",
                            actionState = ActionStatus.COMPLETED,
                            nextAction = "Persisted to Room Database"
                        ),
                        operatingState = VybOperatingState.SUCCESS
                    )
                }
                is VaultOperationResult.ConflictDetected -> {
                    val payload = VybIntentPayload(
                        intentType = "DECISION",
                        title = "Vault Conflict: ${vaultResult.proposedKey}",
                        summary = "Existing: ${vaultResult.existingItem.value} vs New: ${vaultResult.proposedValue}",
                        suggestedFollowUps = listOf("Replace with ${vaultResult.proposedValue}", "Keep ${vaultResult.existingItem.value}")
                    )
                    return OrchestratorResult(
                        conversationalResponse = "${vaultResult.promptText}\n\nPlease let me know if you want to replace it or keep the current one.",
                        intentPayload = payload,
                        diagnostics = VybDiagnostics(
                            intent = "VAULT_CONFLICT_DETECTED",
                            contextSummary = listOf("Existing: ${vaultResult.existingItem.value}", "Proposed: ${vaultResult.proposedValue}"),
                            capabilitiesChecked = emptyList(),
                            toolSelected = "Personal Life Vault",
                            actionState = ActionStatus.WAITING_FOR_CONFIRMATION,
                            nextAction = "Awaiting user confirmation"
                        ),
                        operatingState = VybOperatingState.WAITING_FOR_AUTHORIZATION
                    )
                }
                is VaultOperationResult.Deleted -> {
                    val payload = VybIntentPayload(
                        intentType = "TASK",
                        title = "Vault Fact Removed",
                        summary = vaultResult.message,
                        suggestedFollowUps = listOf("View Personal Life Vault", "Add new fact", "Today's briefing")
                    )
                    return OrchestratorResult(
                        conversationalResponse = vaultResult.message,
                        intentPayload = payload,
                        diagnostics = VybDiagnostics(
                            intent = "VAULT_FACT_DELETED",
                            contextSummary = listOf("Deleted: ${vaultResult.targetKey}"),
                            capabilitiesChecked = emptyList(),
                            toolSelected = "Personal Life Vault",
                            actionState = ActionStatus.COMPLETED,
                            nextAction = "Removed from Room Database"
                        ),
                        operatingState = VybOperatingState.SUCCESS
                    )
                }
                is VaultOperationResult.AskConfirmation -> {
                    val payload = VybIntentPayload(
                        intentType = "DECISION",
                        title = "Confirm Vault Entry",
                        summary = "${vaultResult.proposedKey}: ${vaultResult.proposedValue}",
                        suggestedFollowUps = listOf("Yes, save to vault", "No, ignore")
                    )
                    return OrchestratorResult(
                        conversationalResponse = vaultResult.promptText,
                        intentPayload = payload,
                        diagnostics = VybDiagnostics(
                            intent = "VAULT_CONFIRMATION_REQUIRED",
                            contextSummary = listOf("${vaultResult.proposedCategory}: ${vaultResult.proposedKey}"),
                            capabilitiesChecked = emptyList(),
                            toolSelected = "Personal Life Vault",
                            actionState = ActionStatus.WAITING_FOR_CONFIRMATION,
                            nextAction = "Awaiting user confirmation"
                        ),
                        operatingState = VybOperatingState.WAITING_FOR_AUTHORIZATION
                    )
                }
                is VaultOperationResult.NoOperation -> {
                    // Continue with normal orchestration pipeline
                }
            }
        }

        // STEP 0: VYB BRAIN INTENT & INTELLIGENCE REASONING
        val isBrainQuery = lower.contains("priorities today") ||
                lower.contains("what should i do now") ||
                lower.contains("what am i forgetting") ||
                lower.contains("morning briefing") ||
                lower.contains("good morning") ||
                lower.contains("evening review") ||
                lower.contains("good night") ||
                lower.contains("what did i decide") ||
                lower.contains("should i buy") ||
                lower.contains("what should i wear") ||
                lower.contains("travel next month") ||
                lower.contains("cheapest") ||
                lower.contains("book it") ||
                lower.startsWith("remember that") ||
                lower.startsWith("forget") ||
                (lower.contains("amazon") && (lower.contains("delivery") || lower.contains("track") || lower.contains("order") || lower.contains("where") || lower.contains("arrive"))) ||
                (lower.contains("cab") && (lower.contains("son") || lower.contains("school") || lower.contains("vivaan") || lower.contains("cheapest") || lower.contains("pickup"))) ||
                lower.contains("can i afford") ||
                lower.contains("safely spend") ||
                (lower.contains("extra") && (lower.contains("₹") || lower.contains("rupee") || lower.contains("money") || lower.contains("20,000") || lower.contains("20000")))

        if (isBrainQuery) {
            val prefs = appContext?.getSharedPreferences("vyb_user_preferences", Context.MODE_PRIVATE)
            val brainRes = vybBrain.processQuery(input, prefs)

            return OrchestratorResult(
                conversationalResponse = brainRes.replyText,
                intentPayload = brainRes.intentPayload,
                diagnostics = brainRes.diagnostics,
                action = brainRes.action,
                operatingState = if (brainRes.action != null) VybOperatingState.WAITING_FOR_AUTHORIZATION else VybOperatingState.SUCCESS
            )
        }

        // STEP 1: PARSE STRUCTURED INTENT
        val structuredIntent = IntentParser.parse(input)

        // STEP 2: CHECK FOR MISSING INFORMATION
        if (structuredIntent.hasMissingRequiredInfo()) {
            val missingText = structuredIntent.clarificationQuestion
                ?: "I need a bit more information to complete that. Please specify the missing details."

            val payload = VybIntentPayload(
                intentType = "GENERAL",
                title = "Information Needed",
                summary = "Missing: ${structuredIntent.missingParameters.joinToString(", ")}",
                suggestedFollowUps = listOf("Provide missing details", "Cancel request")
            )

            val diag = VybDiagnostics(
                intent = structuredIntent.intent,
                contextSummary = listOf("Missing Parameters: ${structuredIntent.missingParameters.joinToString(", ")}"),
                capabilitiesChecked = emptyList(),
                toolSelected = null,
                actionState = ActionStatus.WAITING_FOR_CONFIRMATION,
                nextAction = "Awaiting user clarification",
                isVerified = true,
                structuredIntent = structuredIntent
            )

            return OrchestratorResult(
                conversationalResponse = missingText,
                intentPayload = payload,
                diagnostics = diag,
                operatingState = VybOperatingState.NEEDS_INFORMATION
            )
        }

        // STEP 3: EXECUTE OR ROUTE ACTIONS
        // Handle direct memory retrieval
        if (structuredIntent.intent == StructuredIntent.INTENT_RETRIEVE_MEMORY) {
            val result = actionEngine.execute(structuredIntent, appContext)
            val reply = if (result.message.isNotBlank() && !result.message.contains("no stored records matched")) {
                "According to your stored memory:\n• ${result.message}"
            } else {
                "I searched my memory for \"${structuredIntent.entities["query"]}\", but didn't find a matching record. You can tell me \"Remember that...\" at any time."
            }

            val payload = VybIntentPayload(
                intentType = "TASK",
                title = "Memory Retrieval",
                summary = result.message,
                suggestedFollowUps = listOf("What else do you remember?", "Add new memory", "Today's briefing")
            )

            val diag = VybDiagnostics(
                intent = structuredIntent.intent,
                contextSummary = listOf("Memory Query: ${structuredIntent.entities["query"]}"),
                capabilitiesChecked = emptyList(),
                toolSelected = "Memory Engine Query",
                actionState = ActionStatus.COMPLETED,
                nextAction = "Memory lookup complete",
                isVerified = true,
                structuredIntent = structuredIntent,
                executionResult = result
            )

            return OrchestratorResult(
                conversationalResponse = reply,
                intentPayload = payload,
                diagnostics = diag,
                executionResult = result,
                operatingState = VybOperatingState.SUCCESS
            )
        }

        // Handle direct memory storage
        if (structuredIntent.intent == StructuredIntent.INTENT_SAVE_MEMORY) {
            val result = actionEngine.execute(structuredIntent, appContext)
            val valStr = structuredIntent.entities["value"] ?: input
            val keyStr = structuredIntent.entities["key"] ?: "Memory"

            val reply = "I have stored that in your long-term memory:\n\"$keyStr: $valStr\".\n\nI will remember this whenever you ask."

            val payload = VybIntentPayload(
                intentType = "TASK",
                title = "Memory Stored",
                summary = "$keyStr: $valStr",
                suggestedFollowUps = listOf("What time does my son's school start?", "View all memories", "Today's plan")
            )

            val diag = VybDiagnostics(
                intent = structuredIntent.intent,
                contextSummary = listOf("Stored: $keyStr"),
                capabilitiesChecked = emptyList(),
                toolSelected = "Memory Persistence Engine",
                actionState = ActionStatus.COMPLETED,
                nextAction = "Persisted to Room Database",
                isVerified = true,
                structuredIntent = structuredIntent,
                executionResult = result
            )

            return OrchestratorResult(
                conversationalResponse = reply,
                intentPayload = payload,
                diagnostics = diag,
                executionResult = result,
                operatingState = VybOperatingState.SUCCESS
            )
        }

        // Handle real app launching, maps, navigation, and urls directly
        if (structuredIntent.intent == StructuredIntent.INTENT_OPEN_APP ||
            structuredIntent.intent == StructuredIntent.INTENT_OPEN_MAPS ||
            structuredIntent.intent == StructuredIntent.INTENT_NAVIGATE ||
            structuredIntent.intent == StructuredIntent.INTENT_OPEN_URL ||
            structuredIntent.intent == StructuredIntent.INTENT_WEB_SEARCH
        ) {
            val result = actionEngine.execute(structuredIntent, appContext)
            val reply = result.message

            val payload = VybIntentPayload(
                intentType = "TASK",
                title = "Device Action Execution",
                summary = result.message,
                suggestedFollowUps = listOf("Return to Home", "Today's briefing")
            )

            val diag = VybDiagnostics(
                intent = structuredIntent.intent,
                contextSummary = listOf("Action: ${structuredIntent.intent}"),
                capabilitiesChecked = emptyList(),
                toolSelected = "Android System Intent Execution",
                actionState = if (result.success) ActionStatus.COMPLETED else ActionStatus.CANCELLED,
                nextAction = if (result.success) "Executed on device" else "Execution Failed",
                isVerified = true,
                structuredIntent = structuredIntent,
                executionResult = result
            )

            return OrchestratorResult(
                conversationalResponse = reply,
                intentPayload = payload,
                diagnostics = diag,
                executionResult = result,
                operatingState = if (result.success) VybOperatingState.SUCCESS else VybOperatingState.FAILED
            )
        }

        // Handle reminder creation directly
        if (structuredIntent.intent == StructuredIntent.INTENT_CREATE_REMINDER || structuredIntent.intent == StructuredIntent.INTENT_SET_ALARM) {
            val result = actionEngine.execute(structuredIntent, appContext)
            val title = structuredIntent.entities["title"] ?: "Reminder"
            val time = structuredIntent.entities["time"] ?: ""

            val reply = "${result.message}\n\nI have scheduled this on your device and recorded it in your priority tasks."

            val payload = VybIntentPayload(
                intentType = "TASK",
                title = "Reminder Scheduled",
                summary = "$title at $time",
                suggestedFollowUps = listOf("View today's tasks", "Set another reminder", "Back to briefing")
            )

            val diag = VybDiagnostics(
                intent = structuredIntent.intent,
                contextSummary = listOf("Reminder: $title at $time"),
                capabilitiesChecked = emptyList(),
                toolSelected = "AlarmClock / TaskEngine",
                actionState = ActionStatus.COMPLETED,
                nextAction = "Scheduled on device",
                isVerified = true,
                structuredIntent = structuredIntent,
                executionResult = result
            )

            return OrchestratorResult(
                conversationalResponse = reply,
                intentPayload = payload,
                diagnostics = diag,
                executionResult = result,
                operatingState = VybOperatingState.SUCCESS
            )
        }

        // STEP 4: COGNITIVE BRAIN REASONING FOR COMPLEX QUERIES (Transport, Affordability, Briefing, Communication)
        val cognitiveResult = cognitiveBrain.processCognitiveInput(input)
        val snapshot = contextEngine.buildSnapshot(appContext)
        val capabilities = snapshot.checkedCapabilities

        val actionState = when {
            cognitiveResult.pendingAction != null -> ActionStatus.WAITING_FOR_CONFIRMATION
            cognitiveResult.understoodGoal == "CONNECT_ACCOUNT_HELP" || cognitiveResult.understoodGoal == "TRACK_ORDER_QUERY" -> ActionStatus.COMPLETED
            else -> ActionStatus.COMPLETED
        }

        val nextAction = when {
            cognitiveResult.pendingAction != null -> "Awaiting user confirmation before dispatch"
            cognitiveResult.understoodGoal == "CONNECT_ACCOUNT_HELP" -> "Review official OAuth2 capability when available"
            else -> "Ready"
        }

        val diagnostics = VybDiagnostics(
            intent = cognitiveResult.understoodGoal,
            contextSummary = cognitiveResult.retainedContext,
            capabilitiesChecked = capabilities.filter {
                when (cognitiveResult.understoodGoal) {
                    "CONNECT_ACCOUNT_HELP", "TRACK_ORDER_QUERY" -> it.type == CapabilityType.AMAZON
                    "TRANSPORT_REQUEST", "HOLD_TRANSPORT_BOOKING" -> it.type == CapabilityType.LOCATION || it.type == CapabilityType.TRANSPORT_SERVICE
                    else -> true
                }
            },
            toolSelected = cognitiveResult.pendingAction?.title,
            actionState = actionState,
            nextAction = nextAction,
            isVerified = true,
            structuredIntent = structuredIntent
        )

        val diagnosticsPayload = DiagnosticsPayload(
            intent = cognitiveResult.understoodGoal,
            contextSummary = cognitiveResult.retainedContext,
            capabilitiesChecked = diagnostics.capabilitiesChecked.map { "${it.type.displayName}: ${it.status.name}" },
            toolSelected = cognitiveResult.pendingAction?.title,
            actionState = actionState.name,
            nextAction = nextAction
        )

        val enrichedPayload = cognitiveResult.intentPayload.copy(diagnostics = diagnosticsPayload)

        return OrchestratorResult(
            conversationalResponse = cognitiveResult.executiveReply,
            intentPayload = enrichedPayload,
            diagnostics = diagnostics,
            action = cognitiveResult.pendingAction,
            operatingState = if (cognitiveResult.pendingAction != null) VybOperatingState.WAITING_FOR_AUTHORIZATION else VybOperatingState.SUCCESS
        )
    }
}


