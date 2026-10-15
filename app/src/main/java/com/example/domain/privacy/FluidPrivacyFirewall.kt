package com.example.domain.privacy

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Data classification categories for sensitive personal information.
 */
enum class DataCategory(val displayName: String, val description: String) {
    PERSONAL_IDENTITY("Personal Identity", "Name, title, account identifiers"),
    PERSONAL_LIFE_CONTEXT("Personal Life Context", "Routines, schedule, calendar, personal memory vault"),
    FINANCIAL_DATA("Financial Data", "Income, expenses, budget, commitments"),
    FAMILY_AND_RELATIONSHIPS("Family & Relationships", "Family members, relationships, ages"),
    DEVICE_TELEMETRY("Device Telemetry", "Battery, charging, network type"),
    LOCATION_COORDINATES("Location Coordinates", "GPS latitude and longitude"),
    EXTERNAL_COMMUNICATION("External Communication", "Message text, phone numbers, search terms"),
    GENERAL_QUERY("General Query", "User prompt or instructions")
}

/**
 * Type of destination for outbound transfers.
 */
enum class TransferDestinationType {
    CLOUD_AI_SERVICE,
    ENVIRONMENTAL_TELEMETRY_SERVICE,
    EXTERNAL_APP_DEEP_LINK,
    EXTERNAL_WEB_SEARCH,
    UNKNOWN
}

data class TransferDestination(
    val type: TransferDestinationType,
    val name: String,
    val endpointOrPackage: String
)

/**
 * Structured proposal for an external transfer requiring user approval.
 */
data class OutboundTransferProposal(
    val transferId: String = UUID.randomUUID().toString(),
    val destination: TransferDestination,
    val purpose: String,
    val categories: Set<DataCategory>,
    val payloadPreview: String,
    val payloadHash: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val originalPayload: String = "",
    val candidatePayload: String = "",
    val initialCategories: Set<DataCategory> = categories,
    val redactedCategories: Set<DataCategory> = emptySet()
)

/**
 * Cryptographic single-use token issued upon explicit user approval.
 */
data class TransferAuthorizationToken(
    val transferId: String,
    val approvedDestination: TransferDestination,
    val approvedPayloadHash: String,
    val authorizedAtEpochMs: Long,
    val expiresAtEpochMs: Long = authorizedAtEpochMs + 60_000, // Valid for 60 seconds
    val authorizedPayload: String? = null
)

/**
 * Authorization evaluation outcome.
 */
sealed class TransferAuthorizationResult {
    data class Approved(val token: TransferAuthorizationToken) : TransferAuthorizationResult()
    data class Denied(val reason: String) : TransferAuthorizationResult()
    object BlockedByLocalOnlyMode : TransferAuthorizationResult()
    data class Cancelled(val reason: String) : TransferAuthorizationResult()
}

/**
 * Audit log outcome status.
 */
enum class AuditOutcome {
    AUTHORIZED_AND_CONSUMED,
    DENIED_BY_USER,
    CANCELLED_BY_USER,
    BLOCKED_LOCAL_ONLY_MODE,
    BLOCKED_PAYLOAD_TAMPERED,
    BLOCKED_DESTINATION_MISMATCH,
    BLOCKED_TOKEN_INVALID_OR_EXPIRED,
    BLOCKED_UNAUTHORIZED
}

/**
 * Lightweight, privacy-preserving audit record.
 * Crucially, raw sensitive payloads are NOT stored; only the cryptographic SHA-256 hash is recorded.
 */
data class PrivacyAuditRecord(
    val id: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis(),
    val transferId: String,
    val destinationName: String,
    val destinationEndpoint: String,
    val purpose: String,
    val categoryNames: List<String>,
    val payloadSha256: String,
    val outcome: AuditOutcome,
    val note: String = ""
)

/**
 * Centralized Fluid Privacy Firewall.
 * Enforces:
 * 1. Local-Only Mode master boundary.
 * 2. Request-specific, explicit approval for every outbound transfer of personal context.
 * 3. Cryptographic payload hash binding (prevents modifying approved payloads).
 * 4. Single-use token consumption with expiration (replay protection).
 * 5. Local audit logging without persisting private payloads.
 */
object FluidPrivacyFirewall {

    private const val TAG = "FluidPrivacyFirewall"
    private const val PREFS_NAME = "fluid_privacy_preferences"
    private const val KEY_LOCAL_ONLY_MODE = "is_local_only_mode"

    private var appContext: Context? = null

    // Persistent Local-Only Mode state
    private val _isLocalOnlyMode = MutableStateFlow(false)
    val isLocalOnlyMode: StateFlow<Boolean> = _isLocalOnlyMode.asStateFlow()

    // Current pending proposal awaiting user review in UI
    private val _pendingProposal = MutableStateFlow<OutboundTransferProposal?>(null)
    val pendingProposal: StateFlow<OutboundTransferProposal?> = _pendingProposal.asStateFlow()

    // Active unconsumed authorization tokens (transferId -> Token)
    private val activeTokens = ConcurrentHashMap<String, TransferAuthorizationToken>()

    // Local in-memory audit ledger (most recent 200 events)
    private val _auditLog = MutableStateFlow<List<PrivacyAuditRecord>>(emptyList())
    val auditLog: StateFlow<List<PrivacyAuditRecord>> = _auditLog.asStateFlow()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLocalOnly = prefs?.getBoolean(KEY_LOCAL_ONLY_MODE, false) ?: false
        _isLocalOnlyMode.value = savedLocalOnly
        Log.i(TAG, "FluidPrivacyFirewall initialized. Local-Only Mode = $savedLocalOnly")
    }

    /**
     * Toggles Local-Only Mode. When enabled, all outbound transfers are strictly rejected.
     */
    fun setLocalOnlyMode(enabled: Boolean) {
        _isLocalOnlyMode.value = enabled
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_LOCAL_ONLY_MODE, enabled)
            ?.apply()

        Log.i(TAG, "Local-Only Mode updated: $enabled")
        if (enabled) {
            // Cancel any pending transfer proposals immediately
            cancelPendingProposal("Local-Only Mode enabled")
            activeTokens.clear()
        }
    }

    /**
     * Creates an outbound transfer proposal and computes the SHA-256 hash of the intended payload.
     */
    fun createProposal(
        destination: TransferDestination,
        purpose: String,
        categories: Set<DataCategory>,
        payload: String,
        previewSummary: String? = null
    ): OutboundTransferProposal {
        val hash = computeSha256(payload)
        val preview = previewSummary ?: if (payload.length > 300) payload.take(300) + "..." else payload
        return OutboundTransferProposal(
            destination = destination,
            purpose = purpose,
            categories = categories,
            payloadPreview = preview,
            payloadHash = hash,
            originalPayload = payload,
            candidatePayload = payload,
            initialCategories = categories,
            redactedCategories = emptySet()
        )
    }

    /**
     * Constructs the final candidate payload after all selected redactions have been applied.
     * The firewall, not merely the UI, establishes the authorized payload and destination.
     * Recomputes the candidate payload's cryptographic hash so that explicit user approval
     * binds directly to the final transmitted bytes.
     */
    fun applyRedactions(
        proposalId: String,
        categoriesToRedact: Set<DataCategory>,
        customTermsToRedact: Set<String> = emptySet()
    ): OutboundTransferProposal? {
        val current = _pendingProposal.value ?: return null
        if (current.transferId != proposalId) {
            Log.w(TAG, "Cannot apply redactions: proposal $proposalId does not match pending proposal")
            return null
        }
        if (_isLocalOnlyMode.value) {
            return null
        }

        var candidate = current.originalPayload
        val remainingCategories = current.initialCategories.toMutableSet()

        for (cat in categoriesToRedact) {
            remainingCategories.remove(cat)
            when (cat) {
                DataCategory.FINANCIAL_DATA -> {
                    candidate = candidate
                        .replace(Regex("(?i)Finance:.*"), "Finance: [REDACTED BY USER]")
                        .replace(Regex("(?i)Income:.*"), "Income: [REDACTED BY USER]")
                        .replace(Regex("(?i)Expenses:.*"), "Expenses: [REDACTED BY USER]")
                        .replace(Regex("(?i)Budget:.*"), "Budget: [REDACTED BY USER]")
                }
                DataCategory.FAMILY_AND_RELATIONSHIPS -> {
                    candidate = candidate
                        .replace(Regex("(?i)Family:.*"), "Family: [REDACTED BY USER]")
                }
                DataCategory.PERSONAL_LIFE_CONTEXT -> {
                    candidate = candidate
                        .replace(Regex("(?i)Personal Life Vault.*"), "Personal Life Vault: [REDACTED BY USER]")
                        .replace(Regex("(?i)Routines:.*"), "Routines: [REDACTED BY USER]")
                }
                DataCategory.LOCATION_COORDINATES -> {
                    candidate = candidate
                        .replace(Regex("(?i)lat=[0-9.-]+&lon=[0-9.-]+"), "lat=[REDACTED]&lon=[REDACTED]")
                }
                DataCategory.DEVICE_TELEMETRY -> {
                    candidate = candidate
                        .replace(Regex("(?i)Device State:.*"), "Device State: [REDACTED BY USER]")
                }
                else -> {
                    // Custom category
                }
            }
        }

        for (term in customTermsToRedact) {
            if (term.isNotBlank()) {
                candidate = candidate.replace(term, "[REDACTED]")
            }
        }

        val newHash = computeSha256(candidate)
        val newPreview = if (candidate.length > 300) candidate.take(300) + "..." else candidate
        val updated = current.copy(
            candidatePayload = candidate,
            categories = remainingCategories,
            redactedCategories = categoriesToRedact,
            payloadPreview = newPreview,
            payloadHash = newHash
        )
        _pendingProposal.value = updated
        Log.d(TAG, "Redactions applied for proposal $proposalId. New candidate hash: ${newHash.take(12)}... (redacted=${categoriesToRedact.map { it.displayName }})")
        return updated
    }

    /**
     * Submits a proposal for UI review if not in Local-Only Mode.
     */
    fun submitProposalForUserApproval(proposal: OutboundTransferProposal): Boolean {
        if (_isLocalOnlyMode.value) {
            recordAudit(
                transferId = proposal.transferId,
                destination = proposal.destination,
                purpose = proposal.purpose,
                categories = proposal.categories,
                payloadHash = proposal.payloadHash,
                outcome = AuditOutcome.BLOCKED_LOCAL_ONLY_MODE,
                note = "Transfer blocked: Local-Only Mode is active."
            )
            return false
        }

        _pendingProposal.value = proposal
        return true
    }

    /**
     * Approves the specified transfer proposal, generating a cryptographically bound, single-use token.
     * The token is explicitly bound to the SHA-256 hash of the final candidate payload established
     * by the firewall and the approved destination descriptor.
     */
    fun approveProposal(proposalId: String): TransferAuthorizationToken? {
        val current = _pendingProposal.value
        if (current == null || current.transferId != proposalId) {
            Log.w(TAG, "Cannot approve: proposal $proposalId does not match pending proposal")
            return null
        }

        if (_isLocalOnlyMode.value) {
            _pendingProposal.value = null
            recordAudit(
                transferId = current.transferId,
                destination = current.destination,
                purpose = current.purpose,
                categories = current.categories,
                payloadHash = current.payloadHash,
                outcome = AuditOutcome.BLOCKED_LOCAL_ONLY_MODE,
                note = "Approval rejected because Local-Only Mode was turned on."
            )
            return null
        }

        val token = TransferAuthorizationToken(
            transferId = current.transferId,
            approvedDestination = current.destination,
            approvedPayloadHash = current.payloadHash,
            authorizedAtEpochMs = System.currentTimeMillis(),
            authorizedPayload = current.candidatePayload
        )

        activeTokens[token.transferId] = token
        _pendingProposal.value = null

        Log.i(TAG, "Proposal ${current.transferId} approved by user. Token issued bound to hash ${token.approvedPayloadHash.take(12)}... destination: ${token.approvedDestination.name}")
        return token
    }

    /**
     * Denies the current proposal.
     */
    fun denyProposal(proposalId: String, reason: String = "Declined by user") {
        val current = _pendingProposal.value
        if (current != null && current.transferId == proposalId) {
            _pendingProposal.value = null
            recordAudit(
                transferId = current.transferId,
                destination = current.destination,
                purpose = current.purpose,
                categories = current.categories,
                payloadHash = current.payloadHash,
                outcome = AuditOutcome.DENIED_BY_USER,
                note = reason
            )
            Log.i(TAG, "Proposal $proposalId denied by user: $reason")
        }
    }

    /**
     * Cancels the current proposal.
     */
    fun cancelPendingProposal(reason: String = "Cancelled by user") {
        val current = _pendingProposal.value
        if (current != null) {
            _pendingProposal.value = null
            recordAudit(
                transferId = current.transferId,
                destination = current.destination,
                purpose = current.purpose,
                categories = current.categories,
                payloadHash = current.payloadHash,
                outcome = AuditOutcome.CANCELLED_BY_USER,
                note = reason
            )
        }
    }

    /**
     * Validates and consumes the authorization token right before network transmission.
     * Enforces:
     * - Local-Only Mode check
     * - Token existence & expiration
     * - Destination matching
     * - Payload cryptographic hash matching (prevents tampering)
     * - Single-use consumption (token is removed immediately)
     */
    fun validateAndConsumeToken(
        token: TransferAuthorizationToken?,
        targetDestination: TransferDestination,
        actualPayload: String
    ): Boolean {
        if (_isLocalOnlyMode.value) {
            recordAudit(
                transferId = token?.transferId ?: "UNKNOWN",
                destination = targetDestination,
                purpose = "Execution blocked",
                categories = emptySet(),
                payloadHash = computeSha256(actualPayload),
                outcome = AuditOutcome.BLOCKED_LOCAL_ONLY_MODE,
                note = "Outbound transfer blocked: Local-Only Mode is active."
            )
            return false
        }

        if (token == null) {
            recordAudit(
                transferId = "NO_TOKEN",
                destination = targetDestination,
                purpose = "Execution blocked",
                categories = emptySet(),
                payloadHash = computeSha256(actualPayload),
                outcome = AuditOutcome.BLOCKED_UNAUTHORIZED,
                note = "Outbound transfer blocked: Missing authorization token."
            )
            return false
        }

        val storedToken = activeTokens.remove(token.transferId)
        if (storedToken == null) {
            recordAudit(
                transferId = token.transferId,
                destination = targetDestination,
                purpose = "Execution blocked",
                categories = emptySet(),
                payloadHash = computeSha256(actualPayload),
                outcome = AuditOutcome.BLOCKED_TOKEN_INVALID_OR_EXPIRED,
                note = "Token is invalid, already consumed, or expired."
            )
            return false
        }

        val now = System.currentTimeMillis()
        if (now > storedToken.expiresAtEpochMs) {
            recordAudit(
                transferId = token.transferId,
                destination = targetDestination,
                purpose = "Execution blocked",
                categories = emptySet(),
                payloadHash = computeSha256(actualPayload),
                outcome = AuditOutcome.BLOCKED_TOKEN_INVALID_OR_EXPIRED,
                note = "Authorization token expired."
            )
            return false
        }

        if (storedToken.approvedDestination != targetDestination) {
            recordAudit(
                transferId = token.transferId,
                destination = targetDestination,
                purpose = "Execution blocked",
                categories = emptySet(),
                payloadHash = computeSha256(actualPayload),
                outcome = AuditOutcome.BLOCKED_DESTINATION_MISMATCH,
                note = "Approved destination (${storedToken.approvedDestination.endpointOrPackage}) did not match actual (${targetDestination.endpointOrPackage})."
            )
            return false
        }

        val currentPayloadHash = computeSha256(actualPayload)
        if (storedToken.approvedPayloadHash != currentPayloadHash) {
            recordAudit(
                transferId = token.transferId,
                destination = targetDestination,
                purpose = "Execution blocked",
                categories = emptySet(),
                payloadHash = currentPayloadHash,
                outcome = AuditOutcome.BLOCKED_PAYLOAD_TAMPERED,
                note = "Payload was modified after approval. Hash mismatch."
            )
            return false
        }

        // Successfully authorized and consumed!
        recordAudit(
            transferId = token.transferId,
            destination = targetDestination,
            purpose = "Authorized transfer executed",
            categories = emptySet(),
            payloadHash = currentPayloadHash,
            outcome = AuditOutcome.AUTHORIZED_AND_CONSUMED,
            note = "Successfully authorized and consumed."
        )
        return true
    }

    /**
     * Instant evaluation for programmatic or non-interactive outbound checks.
     */
    fun evaluateEgress(
        destination: TransferDestination,
        purpose: String,
        categories: Set<DataCategory>,
        payload: String
    ): TransferAuthorizationResult {
        if (_isLocalOnlyMode.value) {
            recordAudit(
                transferId = "PROG_${UUID.randomUUID().toString().take(8)}",
                destination = destination,
                purpose = purpose,
                categories = categories,
                payloadHash = computeSha256(payload),
                outcome = AuditOutcome.BLOCKED_LOCAL_ONLY_MODE,
                note = "Programmatic egress blocked by Local-Only Mode."
            )
            return TransferAuthorizationResult.BlockedByLocalOnlyMode
        }

        return TransferAuthorizationResult.Denied("Implicit egress disallowed without explicit user authorization token.")
    }

    /**
     * Checks and audits atmospheric weather & air quality telemetry egress (Open-Meteo).
     * Rejects call if Local-Only Mode is active.
     */
    fun checkTelemetryEgress(lat: Double, lon: Double): Boolean {
        val destination = TransferDestination(
            type = TransferDestinationType.ENVIRONMENTAL_TELEMETRY_SERVICE,
            name = "Open-Meteo Environmental Telemetry",
            endpointOrPackage = "https://api.open-meteo.com"
        )
        val payload = "lat=$lat&lon=$lon"
        val payloadHash = computeSha256(payload)

        if (_isLocalOnlyMode.value) {
            recordAudit(
                transferId = "ENV_${System.currentTimeMillis()}",
                destination = destination,
                purpose = "Query atmospheric weather & air quality telemetry",
                categories = setOf(DataCategory.LOCATION_COORDINATES),
                payloadHash = payloadHash,
                outcome = AuditOutcome.BLOCKED_LOCAL_ONLY_MODE,
                note = "Environmental telemetry network egress blocked: Local-Only Mode is active."
            )
            return false
        }

        recordAudit(
            transferId = "ENV_${System.currentTimeMillis()}",
            destination = destination,
            purpose = "Query atmospheric weather & air quality telemetry",
            categories = setOf(DataCategory.LOCATION_COORDINATES),
            payloadHash = payloadHash,
            outcome = AuditOutcome.AUTHORIZED_AND_CONSUMED,
            note = "Environmental telemetry query executed to open meteorological endpoint."
        )
        return true
    }

    /**
     * Audits an external intent dispatch (ride booking, WhatsApp, SMS, Navigation, Phone, Shopping search).
     */
    fun recordExternalIntentDispatch(
        targetName: String,
        destinationPackageOrUri: String,
        purpose: String,
        categories: Set<DataCategory>,
        payload: String
    ) {
        val destination = TransferDestination(
            type = TransferDestinationType.EXTERNAL_APP_DEEP_LINK,
            name = targetName,
            endpointOrPackage = destinationPackageOrUri
        )
        recordAudit(
            transferId = "INTENT_${System.currentTimeMillis()}",
            destination = destination,
            purpose = purpose,
            categories = categories,
            payloadHash = computeSha256(payload),
            outcome = AuditOutcome.AUTHORIZED_AND_CONSUMED,
            note = "External application intent dispatched upon user authorization."
        )
    }

    fun computeSha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input.hashCode().toString()
        }
    }

    private fun recordAudit(
        transferId: String,
        destination: TransferDestination,
        purpose: String,
        categories: Set<DataCategory>,
        payloadHash: String,
        outcome: AuditOutcome,
        note: String
    ) {
        val record = PrivacyAuditRecord(
            transferId = transferId,
            destinationName = destination.name,
            destinationEndpoint = destination.endpointOrPackage,
            purpose = purpose,
            categoryNames = categories.map { it.displayName },
            payloadSha256 = payloadHash,
            outcome = outcome,
            note = note
        )

        val updated = (listOf(record) + _auditLog.value).take(200)
        _auditLog.value = updated
        Log.d(TAG, "[AUDIT] $outcome -> ${destination.name} ($purpose) [Hash: ${payloadHash.take(12)}...]")
    }

    /**
     * Clears in-memory audit log and active tokens (for tests/reset).
     */
    fun resetForTesting() {
        activeTokens.clear()
        _pendingProposal.value = null
        _auditLog.value = emptyList()
        _isLocalOnlyMode.value = false
    }
}
