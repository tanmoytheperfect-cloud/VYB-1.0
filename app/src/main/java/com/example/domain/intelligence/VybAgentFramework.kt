package com.example.domain.intelligence

import android.content.Context
import com.example.domain.permission.VybPermissionManager

/**
 * Result of executing a single action step via an Agent.
 */
data class AgentStepExecutionResult(
    val success: Boolean,
    val outputSummary: String,
    val verification: VerificationResult,
    val generatedData: Any? = null
)

/**
 * Extensible interface for all specialized VYB agents.
 * All agents must strictly operate through VYB Orchestration and respect VybPermissionManager.
 */
interface VybAgent {
    val agentId: String
    val name: String
    val specialty: String
    val requiredCapabilities: List<String>

    fun canHandle(intent: UniversalIntent): Boolean

    fun createPlan(
        intent: UniversalIntent,
        context: ResolvedPersonalContext,
        appContext: Context?,
        permissionManager: VybPermissionManager?
    ): ActionPlan

    suspend fun executeStep(
        step: PlanStep,
        intent: UniversalIntent,
        context: ResolvedPersonalContext,
        appContext: Context?,
        permissionManager: VybPermissionManager?
    ): AgentStepExecutionResult
}

/**
 * Agent Registry managing all specialized agents in VYB.
 */
class VybAgentRegistry(private val agents: List<VybAgent>) {

    fun findAgentForIntent(intent: UniversalIntent): VybAgent? {
        return agents.firstOrNull { it.canHandle(intent) }
    }

    fun getAllAgents(): List<VybAgent> = agents

    fun getAgentById(id: String): VybAgent? = agents.firstOrNull { it.agentId == id }
}
