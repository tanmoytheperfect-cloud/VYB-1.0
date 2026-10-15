package com.example.domain.intelligence

/**
 * Confirmation Preview Payload presented to the user before high-risk execution.
 */
data class HumanConfirmationPrompt(
    val title: String,
    val riskLevel: RiskLevel,
    val whatWillHappen: String,
    val recipientOrService: String,
    val dataShared: String,
    val estimatedConsequence: String,
    val intentId: String
)

/**
 * Risk Manager.
 * Evaluates intents and execution plans for real-world impact.
 * Gates dangerous or privacy-sensitive actions behind mandatory user confirmation.
 */
class RiskManager {

    fun evaluateRisk(intent: UniversalIntent, context: ResolvedPersonalContext): RiskLevel {
        return when (intent.intentType) {
            UniversalIntentType.BOOK_TRANSPORT, UniversalIntentType.TRANSPORT_REQUEST -> RiskLevel.HIGH
            UniversalIntentType.COMMUNICATION_DISPATCH, UniversalIntentType.MESSAGE_REQUEST, UniversalIntentType.EMAIL_REQUEST -> RiskLevel.HIGH
            UniversalIntentType.FINANCE_ANALYSIS -> if (intent.rawInput.contains("transfer", ignoreCase = true) || intent.rawInput.contains("spend", ignoreCase = true)) RiskLevel.CRITICAL else RiskLevel.MEDIUM
            UniversalIntentType.DEVICE_CONTROL -> RiskLevel.MEDIUM
            UniversalIntentType.CALENDAR_SCHEDULE -> RiskLevel.MEDIUM
            UniversalIntentType.AUTOMATION_ROUTINE -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    fun buildConfirmationPrompt(intent: UniversalIntent, context: ResolvedPersonalContext): HumanConfirmationPrompt {
        val level = evaluateRisk(intent, context)

        return when (intent.intentType) {
            UniversalIntentType.BOOK_TRANSPORT, UniversalIntentType.TRANSPORT_REQUEST -> {
                val service = context.contextualService ?: "Uber / Ride Provider"
                val dest = context.contextualDestination ?: "Destination"
                HumanConfirmationPrompt(
                    title = "Confirm Ride Booking Dispatch",
                    riskLevel = level,
                    whatWillHappen = "Dispatch ride-hailing request directly to $service",
                    recipientOrService = service,
                    dataShared = "Target destination address ($dest) and current GPS location coordinates",
                    estimatedConsequence = "May initiate a driver request and incur monetary fare charge in $service app",
                    intentId = intent.intentId
                )
            }

            UniversalIntentType.COMMUNICATION_DISPATCH, UniversalIntentType.MESSAGE_REQUEST, UniversalIntentType.EMAIL_REQUEST -> {
                val recipient = intent.entities["contact_name"] as? String ?: intent.entities["recipient"] as? String ?: context.relatedPeople.firstOrNull() ?: "Contact"
                val msg = intent.entities["message"] as? String ?: intent.rawInput
                HumanConfirmationPrompt(
                    title = "Confirm Communication Dispatch",
                    riskLevel = level,
                    whatWillHappen = "Send outbound message to $recipient",
                    recipientOrService = recipient,
                    dataShared = "Draft message text: \"$msg\"",
                    estimatedConsequence = "Message will be delivered to $recipient from your device identity",
                    intentId = intent.intentId
                )
            }

            UniversalIntentType.CALENDAR_SCHEDULE -> {
                val title = intent.entities["title"] as? String ?: intent.rawInput
                val time = context.contextualTime ?: "Specified time"
                HumanConfirmationPrompt(
                    title = "Confirm Calendar Modification",
                    riskLevel = level,
                    whatWillHappen = "Create calendar event \"$title\"",
                    recipientOrService = "Device Calendar Provider",
                    dataShared = "Event title, timestamp ($time), and attendance notes",
                    estimatedConsequence = "Adds event to your official device schedule timeline",
                    intentId = intent.intentId
                )
            }

            else -> {
                HumanConfirmationPrompt(
                    title = "Confirm Action Execution",
                    riskLevel = level,
                    whatWillHappen = "Execute: ${intent.rawInput}",
                    recipientOrService = "System OS",
                    dataShared = "Necessary entity parameters",
                    estimatedConsequence = "Executes requested device mutation",
                    intentId = intent.intentId
                )
            }
        }
    }
}
