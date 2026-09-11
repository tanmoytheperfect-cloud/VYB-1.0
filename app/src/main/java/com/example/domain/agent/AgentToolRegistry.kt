package com.example.domain.agent

import android.content.Context
import com.example.data.local.dao.LifeVaultDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.TaskDao
import com.example.domain.tools.ToolCategory

class AgentToolRegistry(
    memoryDao: MemoryDao? = null,
    taskDao: TaskDao? = null,
    lifeVaultDao: LifeVaultDao? = null
) {
    private val tools = mutableListOf<AgentTool>()

    init {
        tools.add(DeviceTimeTool())
        tools.add(AppLauncherTool())
        tools.add(AmazonDeliveryTool())
        tools.add(TransportCoordinationTool())
        tools.add(FinancialAffordabilityTool())
        tools.add(DeviceReminderTool(taskDao))
        tools.add(AgentMemoryTool(memoryDao, lifeVaultDao))
        tools.add(CalendarTool())
        tools.add(AlarmTool())
    }

    fun getAllTools(): List<AgentTool> = tools.toList()

    fun findToolById(id: String): AgentTool? = tools.find { it.id == id }

    fun getAvailableTools(agentContext: AgentContext, appContext: Context?): List<AgentTool> {
        return tools.filter { it.isAvailable(agentContext, appContext) }
    }

    fun getToolsByCategory(category: ToolCategory): List<AgentTool> {
        return tools.filter { it.category == category }
    }
}
