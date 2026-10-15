package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.privacy.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Stage 1 — Privacy Foundation Unit Tests.
 * Validates FluidPrivacyFirewall policies, cryptographic hash bindings, Local-Only Mode,
 * replay protection, and audit ledger integrity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FluidPrivacyFirewallTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        FluidPrivacyFirewall.initialize(context)
        FluidPrivacyFirewall.resetForTesting()
    }

    @Test
    fun testProposalCreation_computesAccurateSha256() {
        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val payload = "User query: What is my budget?"
        val expectedHash = FluidPrivacyFirewall.computeSha256(payload)

        val proposal = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Financial context query reasoning",
            categories = setOf(DataCategory.FINANCIAL_DATA, DataCategory.GENERAL_QUERY),
            payload = payload
        )

        assertEquals(expectedHash, proposal.payloadHash)
        assertEquals(destination, proposal.destination)
        assertTrue(proposal.categories.contains(DataCategory.FINANCIAL_DATA))
    }

    @Test
    fun testLocalOnlyMode_blocksProposalSubmissionAndRecordsAudit() {
        FluidPrivacyFirewall.setLocalOnlyMode(true)
        assertTrue(FluidPrivacyFirewall.isLocalOnlyMode.value)

        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val proposal = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Query reasoning",
            categories = setOf(DataCategory.GENERAL_QUERY),
            payload = "Hello"
        )

        val submitted = FluidPrivacyFirewall.submitProposalForUserApproval(proposal)
        assertFalse("Proposal submission must be blocked when Local-Only Mode is active", submitted)
        assertNull("Pending proposal must remain null", FluidPrivacyFirewall.pendingProposal.value)

        val audits = FluidPrivacyFirewall.auditLog.value
        assertEquals(1, audits.size)
        assertEquals(AuditOutcome.BLOCKED_LOCAL_ONLY_MODE, audits.first().outcome)
    }

    @Test
    fun testApprovalWorkflow_issuesSingleUseToken() {
        FluidPrivacyFirewall.setLocalOnlyMode(false)

        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val payload = "Analyze my day"
        val proposal = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Daily schedule reasoning",
            categories = setOf(DataCategory.PERSONAL_LIFE_CONTEXT),
            payload = payload
        )

        val submitted = FluidPrivacyFirewall.submitProposalForUserApproval(proposal)
        assertTrue(submitted)
        assertEquals(proposal, FluidPrivacyFirewall.pendingProposal.value)

        // Approve
        val token = FluidPrivacyFirewall.approveProposal(proposal.transferId)
        assertNotNull(token)
        assertEquals(proposal.transferId, token!!.transferId)
        assertEquals(proposal.payloadHash, token.approvedPayloadHash)
        assertNull("Pending proposal should be cleared after approval", FluidPrivacyFirewall.pendingProposal.value)

        // Consume token validly
        val consumed = FluidPrivacyFirewall.validateAndConsumeToken(
            token = token,
            targetDestination = destination,
            actualPayload = payload
        )
        assertTrue("Token must be successfully validated and consumed", consumed)

        // Replay protection: token cannot be consumed a second time
        val replayed = FluidPrivacyFirewall.validateAndConsumeToken(
            token = token,
            targetDestination = destination,
            actualPayload = payload
        )
        assertFalse("Replaying an already consumed token must be blocked", replayed)
    }

    @Test
    fun testPayloadTampering_isBlockedByFirewall() {
        FluidPrivacyFirewall.setLocalOnlyMode(false)

        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val originalPayload = "Clean prompt without sensitive facts"
        val proposal = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "General reasoning",
            categories = setOf(DataCategory.GENERAL_QUERY),
            payload = originalPayload
        )

        FluidPrivacyFirewall.submitProposalForUserApproval(proposal)
        val token = FluidPrivacyFirewall.approveProposal(proposal.transferId)
        assertNotNull(token)

        // Attempt to send a modified payload that does not match the approved hash
        val tamperedPayload = "Clean prompt without sensitive facts + ADDED PRIVATE SECRET"
        val allowed = FluidPrivacyFirewall.validateAndConsumeToken(
            token = token,
            targetDestination = destination,
            actualPayload = tamperedPayload
        )

        assertFalse("Modifying payload after approval must be blocked due to hash mismatch", allowed)
        val latestAudit = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.BLOCKED_PAYLOAD_TAMPERED, latestAudit.outcome)
    }

    @Test
    fun testDestinationMismatch_isBlockedByFirewall() {
        FluidPrivacyFirewall.setLocalOnlyMode(false)

        val approvedDestination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val rogueDestination = TransferDestination(
            type = TransferDestinationType.UNKNOWN,
            name = "Rogue Third Party API",
            endpointOrPackage = "https://untrusted-endpoint.com"
        )
        val payload = "Test payload"
        val proposal = FluidPrivacyFirewall.createProposal(
            destination = approvedDestination,
            purpose = "Reasoning",
            categories = setOf(DataCategory.GENERAL_QUERY),
            payload = payload
        )

        FluidPrivacyFirewall.submitProposalForUserApproval(proposal)
        val token = FluidPrivacyFirewall.approveProposal(proposal.transferId)
        assertNotNull(token)

        val allowed = FluidPrivacyFirewall.validateAndConsumeToken(
            token = token,
            targetDestination = rogueDestination,
            actualPayload = payload
        )

        assertFalse("Destination mismatch must be blocked", allowed)
        val latestAudit = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.BLOCKED_DESTINATION_MISMATCH, latestAudit.outcome)
    }

    @Test
    fun testEnvironmentalTelemetryEgress_respectsFirewallPolicy() {
        // 1. When Local-Only Mode is ACTIVE -> telemetry is blocked and logged
        FluidPrivacyFirewall.setLocalOnlyMode(true)
        val blocked = FluidPrivacyFirewall.checkTelemetryEgress(lat = 22.5726, lon = 88.3639)
        assertFalse(blocked)

        val auditBlocked = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.BLOCKED_LOCAL_ONLY_MODE, auditBlocked.outcome)
        assertEquals("Open-Meteo Environmental Telemetry", auditBlocked.destinationName)

        // 2. When Local-Only Mode is DISABLED -> telemetry is allowed and audited
        FluidPrivacyFirewall.setLocalOnlyMode(false)
        val allowed = FluidPrivacyFirewall.checkTelemetryEgress(lat = 22.5726, lon = 88.3639)
        assertTrue(allowed)

        val auditAllowed = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.AUTHORIZED_AND_CONSUMED, auditAllowed.outcome)
        assertTrue(auditAllowed.categoryNames.contains("Location Coordinates"))
    }

    @Test
    fun testExternalIntentDispatch_isAuditedWithoutStoringRawPayload() {
        FluidPrivacyFirewall.recordExternalIntentDispatch(
            targetName = "Uber",
            destinationPackageOrUri = "com.ubercab",
            purpose = "Ride to Airport",
            categories = setOf(DataCategory.LOCATION_COORDINATES, DataCategory.EXTERNAL_COMMUNICATION),
            payload = "destination=Airport Terminal 2"
        )

        val audits = FluidPrivacyFirewall.auditLog.value
        assertEquals(1, audits.size)
        val record = audits.first()
        assertEquals("Uber", record.destinationName)
        assertEquals("com.ubercab", record.destinationEndpoint)
        assertEquals(AuditOutcome.AUTHORIZED_AND_CONSUMED, record.outcome)
        assertFalse("Raw payload should not be present in record", record.payloadSha256.contains("Airport Terminal 2"))
        assertEquals(64, record.payloadSha256.length) // SHA-256 hex string is 64 characters
    }

    @Test
    fun testUnredactedPayloadApprovalAndDispatch() {
        FluidPrivacyFirewall.setLocalOnlyMode(false)
        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val originalPayload = "User Query: What meetings do I have tomorrow?"
        val proposal = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Calendar schedule reasoning",
            categories = setOf(DataCategory.PERSONAL_LIFE_CONTEXT, DataCategory.GENERAL_QUERY),
            payload = originalPayload
        )

        assertTrue(FluidPrivacyFirewall.submitProposalForUserApproval(proposal))
        assertEquals(originalPayload, FluidPrivacyFirewall.pendingProposal.value?.candidatePayload)

        val token = FluidPrivacyFirewall.approveProposal(proposal.transferId)
        assertNotNull(token)
        assertEquals(FluidPrivacyFirewall.computeSha256(originalPayload), token!!.approvedPayloadHash)
        assertEquals(originalPayload, token.authorizedPayload)

        val dispatched = FluidPrivacyFirewall.validateAndConsumeToken(
            token = token,
            targetDestination = destination,
            actualPayload = originalPayload
        )
        assertTrue("Unredacted payload must be approved and dispatched cleanly", dispatched)

        val latestAudit = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.AUTHORIZED_AND_CONSUMED, latestAudit.outcome)
        assertEquals(token.approvedPayloadHash, latestAudit.payloadSha256)
    }

    @Test
    fun testRedactedPayloadApprovalAndDispatch() {
        FluidPrivacyFirewall.setLocalOnlyMode(false)
        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val rawSensitivePayload = """
            === VERIFIED ON-DEVICE CONTEXT ===
            Finance: Monthly income 50000, Expenses: 35000, Budget: 15000
            Family: Spouse Alice, Son Bob
            Personal Life Vault / Memory: Top secret anniversary note
            === END CONTEXT ===
            User Query: "How much did I save?"
        """.trimIndent()

        val proposal = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Financial context reasoning",
            categories = setOf(DataCategory.FINANCIAL_DATA, DataCategory.FAMILY_AND_RELATIONSHIPS, DataCategory.PERSONAL_LIFE_CONTEXT),
            payload = rawSensitivePayload
        )

        FluidPrivacyFirewall.submitProposalForUserApproval(proposal)

        // User redacts Financial Data and Family
        val updatedProposal = FluidPrivacyFirewall.applyRedactions(
            proposalId = proposal.transferId,
            categoriesToRedact = setOf(DataCategory.FINANCIAL_DATA, DataCategory.FAMILY_AND_RELATIONSHIPS)
        )
        assertNotNull(updatedProposal)
        assertTrue(updatedProposal!!.candidatePayload.contains("Finance: [REDACTED BY USER]"))
        assertTrue(updatedProposal.candidatePayload.contains("Family: [REDACTED BY USER]"))
        assertFalse("Original unredacted numbers must not be present in candidate payload", updatedProposal.candidatePayload.contains("50000"))
        assertFalse(updatedProposal.categories.contains(DataCategory.FINANCIAL_DATA))
        assertFalse(updatedProposal.categories.contains(DataCategory.FAMILY_AND_RELATIONSHIPS))

        // Approve redacted proposal
        val token = FluidPrivacyFirewall.approveProposal(proposal.transferId)
        assertNotNull(token)
        assertEquals(updatedProposal.payloadHash, token!!.approvedPayloadHash)
        assertEquals(updatedProposal.candidatePayload, token.authorizedPayload)

        // Dispatch exact redacted payload
        val dispatched = FluidPrivacyFirewall.validateAndConsumeToken(
            token = token,
            targetDestination = destination,
            actualPayload = updatedProposal.candidatePayload
        )
        assertTrue("Redacted payload matching token hash must be successfully dispatched", dispatched)

        val latestAudit = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.AUTHORIZED_AND_CONSUMED, latestAudit.outcome)
        assertEquals(updatedProposal.payloadHash, latestAudit.payloadSha256)
    }

    @Test
    fun testRedactedPayload_isExactlyAuthorizedPayload_andOriginalRejected() {
        FluidPrivacyFirewall.setLocalOnlyMode(false)
        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val rawSensitivePayload = "Finance: Balance is 10000. User Query: Help me plan."
        val proposal = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Budget planning",
            categories = setOf(DataCategory.FINANCIAL_DATA),
            payload = rawSensitivePayload
        )

        FluidPrivacyFirewall.submitProposalForUserApproval(proposal)
        val updatedProposal = FluidPrivacyFirewall.applyRedactions(
            proposalId = proposal.transferId,
            categoriesToRedact = setOf(DataCategory.FINANCIAL_DATA)
        )
        assertNotNull(updatedProposal)

        val token = FluidPrivacyFirewall.approveProposal(proposal.transferId)
        assertNotNull(token)

        // 1. Attempting to dispatch the original UNREDACTED payload must be rejected
        val dispatchOriginal = FluidPrivacyFirewall.validateAndConsumeToken(
            token = token,
            targetDestination = destination,
            actualPayload = rawSensitivePayload
        )
        assertFalse("Dispatching unredacted payload when redacted was approved must be blocked", dispatchOriginal)

        val latestAudit = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.BLOCKED_PAYLOAD_TAMPERED, latestAudit.outcome)
    }

    @Test
    fun testDenialAndCancellation_produceNoDispatch() {
        FluidPrivacyFirewall.setLocalOnlyMode(false)
        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val payload = "Confidential context payload"

        // 1. Denial test
        val proposal1 = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Reasoning",
            categories = setOf(DataCategory.PERSONAL_LIFE_CONTEXT),
            payload = payload
        )
        FluidPrivacyFirewall.submitProposalForUserApproval(proposal1)
        FluidPrivacyFirewall.denyProposal(proposal1.transferId, "Explicitly rejected by user")
        assertNull("Pending proposal must be cleared after denial", FluidPrivacyFirewall.pendingProposal.value)

        val auditDenial = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.DENIED_BY_USER, auditDenial.outcome)

        // Attempting to dispatch without valid token fails closed
        val dispatchBlocked = FluidPrivacyFirewall.validateAndConsumeToken(
            token = null,
            targetDestination = destination,
            actualPayload = payload
        )
        assertFalse(dispatchBlocked)

        // 2. Cancellation test
        val proposal2 = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Reasoning 2",
            categories = setOf(DataCategory.PERSONAL_LIFE_CONTEXT),
            payload = payload
        )
        FluidPrivacyFirewall.submitProposalForUserApproval(proposal2)
        FluidPrivacyFirewall.cancelPendingProposal("User dismissed dialog")
        assertNull("Pending proposal must be cleared after cancellation", FluidPrivacyFirewall.pendingProposal.value)

        val auditCancel = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.CANCELLED_BY_USER, auditCancel.outcome)
    }

    @Test
    fun testExpiredToken_isRejected() {
        FluidPrivacyFirewall.setLocalOnlyMode(false)
        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val payload = "Test expired payload"
        val proposal = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Reasoning",
            categories = setOf(DataCategory.GENERAL_QUERY),
            payload = payload
        )
        FluidPrivacyFirewall.submitProposalForUserApproval(proposal)
        val token = FluidPrivacyFirewall.approveProposal(proposal.transferId)
        assertNotNull(token)

        // Construct expired token (validity was 60 seconds, create one expired 100 seconds ago)
        val expiredToken = token!!.copy(
            authorizedAtEpochMs = System.currentTimeMillis() - 120_000,
            expiresAtEpochMs = System.currentTimeMillis() - 60_000
        )
        // Inject expired token into activeTokens
        val tokenField = FluidPrivacyFirewall.javaClass.getDeclaredField("activeTokens")
        tokenField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val map = tokenField.get(FluidPrivacyFirewall) as java.util.concurrent.ConcurrentHashMap<String, TransferAuthorizationToken>
        map[expiredToken.transferId] = expiredToken

        val allowed = FluidPrivacyFirewall.validateAndConsumeToken(
            token = expiredToken,
            targetDestination = destination,
            actualPayload = payload
        )
        assertFalse("Expired token must be rejected", allowed)
        val audit = FluidPrivacyFirewall.auditLog.value.first()
        assertEquals(AuditOutcome.BLOCKED_TOKEN_INVALID_OR_EXPIRED, audit.outcome)
    }

    @Test
    fun testConcurrentRequests_cannotConsumeOrSubstituteTokens() {
        FluidPrivacyFirewall.setLocalOnlyMode(false)
        val destination = TransferDestination(
            type = TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val payloadA = "Request payload A - Topic Alpha"
        val payloadB = "Request payload B - Topic Beta"

        // Create and approve Proposal A
        val proposalA = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Reasoning A",
            categories = setOf(DataCategory.GENERAL_QUERY),
            payload = payloadA
        )
        FluidPrivacyFirewall.submitProposalForUserApproval(proposalA)
        val tokenA = FluidPrivacyFirewall.approveProposal(proposalA.transferId)
        assertNotNull(tokenA)

        // Create and approve Proposal B
        val proposalB = FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Reasoning B",
            categories = setOf(DataCategory.GENERAL_QUERY),
            payload = payloadB
        )
        FluidPrivacyFirewall.submitProposalForUserApproval(proposalB)
        val tokenB = FluidPrivacyFirewall.approveProposal(proposalB.transferId)
        assertNotNull(tokenB)

        // Attempt Cross-Substitution 1: Token A used with Payload B -> Rejected
        val cross1 = FluidPrivacyFirewall.validateAndConsumeToken(
            token = tokenA,
            targetDestination = destination,
            actualPayload = payloadB
        )
        assertFalse("Attempting to use Token A with Payload B must be blocked", cross1)

        // Attempt Cross-Substitution 2: Token B used with Payload A -> Rejected
        val cross2 = FluidPrivacyFirewall.validateAndConsumeToken(
            token = tokenB,
            targetDestination = destination,
            actualPayload = payloadA
        )
        assertFalse("Attempting to use Token B with Payload A must be blocked", cross2)

        // Verify tokens are single-use and cannot be used after failed tampered consumption
        val retryTokenA = FluidPrivacyFirewall.validateAndConsumeToken(
            token = tokenA,
            targetDestination = destination,
            actualPayload = payloadA
        )
        assertFalse("Token A was consumed/invalidated during the tampered attempt and cannot be replayed", retryTokenA)
    }
}
