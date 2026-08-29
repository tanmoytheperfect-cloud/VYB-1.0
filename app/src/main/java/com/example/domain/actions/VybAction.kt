package com.example.domain.actions

enum class ActionLevel(val code: Int, val displayName: String, val requiresExplicitConfirmation: Boolean) {
    LEVEL_0_INFORMATIONAL(0, "Informational", false),
    LEVEL_1_ORGANIZATIONAL(1, "Personal Organization", false),
    LEVEL_2_COMMUNICATION(2, "External Communication", true),
    LEVEL_3_CONSEQUENTIAL(3, "Financial or Consequential", true)
}

enum class ActionStatus {
    PENDING,
    RUNNING,
    WAITING_FOR_CONFIRMATION,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class VybAction(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val actionType: String,
    val level: ActionLevel,
    val status: ActionStatus = if (level.requiresExplicitConfirmation) ActionStatus.WAITING_FOR_CONFIRMATION else ActionStatus.PENDING,
    val expectedCostInr: Double? = null,
    val consequenceSummary: String = "",
    val parameters: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val resultSummary: String? = null
)
