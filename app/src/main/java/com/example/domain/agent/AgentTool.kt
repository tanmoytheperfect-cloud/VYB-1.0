package com.example.domain.agent

import android.content.Context
import com.example.domain.actions.ActionLevel
import com.example.domain.actions.ActionStatus
import com.example.domain.actions.VybAction
import com.example.domain.tools.ToolCategory
import com.example.util.DeviceAppExecutionBridge

sealed class ToolPrerequisiteResult {
    data object Satisfied : ToolPrerequisiteResult()
    data class MissingAccountConnection(val serviceName: String, val message: String, val connectAction: String) : ToolPrerequisiteResult()
    data class MissingPermission(val permission: String, val rationale: String) : ToolPrerequisiteResult()
    data class AppNotInstalled(val appName: String, val packageName: String?, val storeUrl: String) : ToolPrerequisiteResult()
}

sealed class AgentToolResult {
    data class Success(
        val toolId: String,
        val summary: String,
        val data: Map<String, Any?> = emptyMap(),
        val actionRecord: VybAction? = null
    ) : AgentToolResult()

    data class RequiresConfirmation(
        val toolId: String,
        val action: VybAction,
        val explanation: String
    ) : AgentToolResult()

    data class Unavailable(
        val toolId: String,
        val reason: String,
        val suggestedNextStep: String
    ) : AgentToolResult()

    data class Failure(
        val toolId: String,
        val errorMessage: String
    ) : AgentToolResult()
}

interface AgentTool {
    val id: String
    val name: String
    val description: String
    val category: ToolCategory
    val riskLevel: SafetyRiskLevel
    val requiredPermissions: List<String> get() = emptyList()

    fun isAvailable(agentContext: AgentContext, appContext: Context?): Boolean = true

    fun checkPrerequisites(agentContext: AgentContext, appContext: Context?): ToolPrerequisiteResult =
        ToolPrerequisiteResult.Satisfied

    suspend fun execute(
        input: String,
        params: Map<String, Any?>,
        agentContext: AgentContext,
        appContext: Context?
    ): AgentToolResult
}
