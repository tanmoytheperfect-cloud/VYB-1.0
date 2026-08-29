package com.example.domain.tools

import android.content.Context
import com.example.domain.actions.ActionLevel
import com.example.domain.actions.ActionStatus
import com.example.domain.actions.VybAction
import com.example.domain.capability.CapabilityEngine
import com.example.domain.capability.CapabilityStatus
import com.example.domain.capability.CapabilityType
import com.example.domain.context.VybContextSnapshot

sealed class ToolExecutionResult {
    data class Success(
        val toolId: String,
        val summary: String,
        val nextAction: String,
        val data: Any? = null,
        val actionRecord: VybAction? = null
    ) : ToolExecutionResult()

    data class WaitingForConfirmation(
        val toolId: String,
        val action: VybAction,
        val summary: String,
        val estimatedCostInr: Double? = null
    ) : ToolExecutionResult()

    data class CapabilityMissing(
        val toolId: String,
        val capabilityType: CapabilityType,
        val message: String
    ) : ToolExecutionResult()

    data class PermissionRequired(
        val toolId: String,
        val permission: String,
        val explanation: String
    ) : ToolExecutionResult()

    data class Error(
        val toolId: String,
        val message: String
    ) : ToolExecutionResult()
}

interface VybToolContract {
    val id: String
    val name: String
    val description: String
    val category: ToolCategory
    val requiredCapability: CapabilityType?
    val requiredPermissions: List<String>
    val riskLevel: Int // 0: safe, 1: personal write, 2: external comms, 3: financial/booking

    fun isAvailable(appContext: Context?, snapshot: VybContextSnapshot): Boolean {
        val cap = requiredCapability?.let { CapabilityEngine.evaluateCapability(it, appContext) }
        return cap == null || cap.status.isOperable
    }

    suspend fun execute(
        input: String,
        params: Map<String, Any> = emptyMap(),
        snapshot: VybContextSnapshot,
        appContext: Context?
    ): ToolExecutionResult
}
