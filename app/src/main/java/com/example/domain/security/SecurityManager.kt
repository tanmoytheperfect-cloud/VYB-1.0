package com.example.domain.security

enum class ActionAuthorizationLevel(val levelName: String, val requiresExplicitConfirmation: Boolean) {
    LOW("Low Risk", false),       // read time, local routine, stored preference
    MEDIUM("Medium Risk", false),  // open app, create reminder, prepare booking draft
    HIGH("High Risk", true)       // book transport, send money, financial transaction, delete data
}

data class SecurityAuditEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val actionName: String,
    val level: ActionAuthorizationLevel,
    val isConfirmedByUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val outcome: String
)

class SecurityManager {

    private val auditLog = mutableListOf<SecurityAuditEntry>()
    var isProactiveEnabled: Boolean = true
    var isLearningEnabled: Boolean = true

    fun evaluateAuthorizationRequirement(actionType: String): ActionAuthorizationLevel {
        val upper = actionType.uppercase()
        return when {
            upper.contains("PAY") || upper.contains("TRANSFER") || upper.contains("TRANSACTION") ||
            upper.contains("BOOK") || upper.contains("DELETE") || upper.contains("PURCHASE") ||
            upper.contains("SEND_SMS") || upper.contains("CALL") -> ActionAuthorizationLevel.HIGH

            upper.contains("OPEN_APP") || upper.contains("REMINDER") || upper.contains("DRAFT") ||
            upper.contains("NOTIFY") || upper.contains("PREPARE") -> ActionAuthorizationLevel.MEDIUM

            else -> ActionAuthorizationLevel.LOW
        }
    }

    fun recordAudit(actionName: String, level: ActionAuthorizationLevel, confirmed: Boolean, outcome: String) {
        auditLog.add(
            SecurityAuditEntry(
                actionName = actionName,
                level = level,
                isConfirmedByUser = confirmed,
                outcome = outcome
            )
        )
    }

    fun getAuditLog(): List<SecurityAuditEntry> = auditLog.toList()
}
