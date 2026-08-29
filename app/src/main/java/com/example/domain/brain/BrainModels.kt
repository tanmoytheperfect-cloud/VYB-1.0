package com.example.domain.brain

import com.example.domain.actions.ActionLevel
import com.example.domain.actions.VybAction
import com.example.domain.tools.ToolRegistry
import com.example.domain.tools.VybTool

data class PlanStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val requiredToolId: String? = null,
    val actionLevel: ActionLevel = ActionLevel.LEVEL_0_INFORMATIONAL,
    val isCompleted: Boolean = false
)

data class GoalPlan(
    val goalId: String = java.util.UUID.randomUUID().toString(),
    val goalTitle: String,
    val objectiveSummary: String,
    val category: String, // HEALTH, FINANCE, TRAVEL, CAREER, SHOPPING, FAMILY
    val targetValue: String? = null,
    val currentValue: String? = null,
    val targetDeadline: String? = null,
    val estimatedBudgetInr: Double? = null,
    val steps: List<PlanStep> = emptyList(),
    val whyRecommended: String? = null,
    val requiredTools: List<VybTool> = emptyList(),
    val proposedActions: List<VybAction> = emptyList()
)

data class DecisionOption(
    val name: String,
    val description: String,
    val costInr: Double,
    val pros: List<String>,
    val cons: List<String>,
    val rating: Double = 4.5,
    val isRecommended: Boolean = false,
    val recommendationReason: String? = null
)

data class DecisionAnalysis(
    val query: String,
    val criteria: List<String>, // Price, Convenience, Quality, Speed
    val options: List<DecisionOption>,
    val bestOverallOption: String,
    val whyVybRecommends: String
)
