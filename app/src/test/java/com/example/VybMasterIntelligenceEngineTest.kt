package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ExpenseEntity
import com.example.domain.communication.CommunicationChannel
import com.example.domain.communication.CommunicationHandoffEngine
import com.example.domain.finance.AffordabilityVerdict
import com.example.domain.finance.FinancialIntelligenceEngine
import com.example.domain.genesis.GenesisDataSource
import com.example.domain.genesis.PersonalContextGraph
import com.example.domain.genesis.PersonalityStyle
import com.example.domain.genesis.VybPersonalProfile
import com.example.domain.intelligence.RiskLevel
import com.example.domain.intelligence.UniversalIntentType
import com.example.domain.intelligence.VybIntelligenceOrchestrator
import com.example.domain.permission.VybPermissionManager
import com.example.domain.transport.RideVehicleType
import com.example.domain.transport.TransportChoiceEngine
import com.example.domain.transport.TransportProvider
import com.example.domain.truth.TruthConfidenceLevel
import com.example.domain.truth.TruthFact
import com.example.domain.truth.VybTruthEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VybMasterIntelligenceEngineTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var permissionManager: VybPermissionManager
    private lateinit var orchestrator: VybIntelligenceOrchestrator

    private val masterProfile = VybPersonalProfile(
        name = "Tanmoy",
        callingTitle = "Sir",
        jobTitle = "Founder & Chief Executive",
        companyName = "Metropoli Fashions",
        officeAddress = "Sector V, Tech Hub, Kolkata",
        homeAddress = "Salt Lake City, Block AB, Kolkata",
        commutePreference = "Uber Premier / Yatri Sathi",
        monthlyIncome = 250000.0,
        fixedCommitments = 65000.0,
        savingsTarget = 50000.0,
        currencySymbol = "₹",
        communicationStyle = PersonalityStyle.WORK_FOCUSED
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        permissionManager = VybPermissionManager(context)
        orchestrator = VybIntelligenceOrchestrator(
            memoryDao = database.memoryDao(),
            taskDao = database.taskDao(),
            lifeVaultDao = database.lifeVaultDao(),
            expenseDao = database.expenseDao(),
            shoppingDao = database.shoppingDao(),
            routineDao = database.routineDao(),
            goalDao = database.goalDao(),
            projectDao = database.projectDao(),
            familyDao = database.familyDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test 1 Truth Model evaluates verified, inferred, and stale data confidence`() {
        val truthEngine = VybTruthEngine()

        // 1. Explicit user stated fact -> VERIFIED (1.0)
        val verifiedFact = truthEngine.evaluateTruth(
            category = "IDENTITY",
            key = "Owner Name",
            value = "Tanmoy",
            statedConfidence = 1.0f,
            source = "USER_EXPLICIT"
        )
        assertEquals(TruthConfidenceLevel.VERIFIED, verifiedFact.confidenceLevel)
        assertEquals(1.0f, verifiedFact.confidenceScore)
        assertTrue(verifiedFact.isUserConfirmed)

        // 2. High confidence hardware GPS -> VERIFIED
        val sensorFact = truthEngine.evaluateTruth(
            category = "LOCATION",
            key = "GPS",
            value = "22.5726, 88.3639",
            statedConfidence = 0.95f,
            source = "GPS_HARDWARE"
        )
        assertEquals(TruthConfidenceLevel.VERIFIED, sensorFact.confidenceLevel)

        // 3. Routine pattern with medium confidence -> INFERRED
        val inferredFact = truthEngine.evaluateTruth(
            category = "ROUTINE",
            key = "Evening Gym",
            value = "Gym at 7 PM on weekdays",
            statedConfidence = 0.75f,
            source = "OBSERVED_ROUTINE"
        )
        assertEquals(TruthConfidenceLevel.INFERRED, inferredFact.confidenceLevel)
        assertEquals(0.75f, inferredFact.confidenceScore)

        // 4. Stale fact older than freshness window -> STALE
        val oldTimestamp = System.currentTimeMillis() - (48 * 60 * 60 * 1000L) // 48 hrs ago
        val staleFact = truthEngine.evaluateTruth(
            category = "TELEMETRY",
            key = "Battery",
            value = "85%",
            statedConfidence = 0.9f,
            source = "BATTERY_SENSOR",
            timestamp = oldTimestamp,
            freshnessWindowMs = TruthFact.TELEMETRY_FRESHNESS_MS
        )
        assertEquals(TruthConfidenceLevel.STALE, staleFact.confidenceLevel)
        assertTrue(staleFact.confidenceScore < 0.50f)

        // 5. Truth Audit formatting
        val auditReport = truthEngine.formatTruthAudit(listOf(verifiedFact, inferredFact, staleFact))
        assertTrue(auditReport.contains("TRUTH AUDIT SUMMARY"))
        assertTrue(auditReport.contains("Verified Facts (High Certainty)"))
        assertTrue(auditReport.contains("Inferred Facts (Pattern/Observation)"))
        assertTrue(auditReport.contains("Stale Facts (Needs Re-validation)"))
    }

    @Test
    fun `test 2 Transport Choice Engine supports ONDC, fare comparison, and preserves preferences`() {
        val transportEngine = TransportChoiceEngine()

        val comparison = transportEngine.compareTransportOptions(
            destination = "Sector V, Tech Hub, Kolkata",
            pickup = "Salt Lake City, Block AB",
            profile = masterProfile
        )

        assertEquals("Sector V, Tech Hub, Kolkata", comparison.destination)
        assertTrue("Must include multiple transport options", comparison.options.size >= 4)

        // Verify ONDC option is present
        val ondcOption = comparison.options.firstOrNull { it.provider.isOpenNetwork }
        assertNotNull("Must include ONDC provider (Yatri Sathi or Namma Yatri)", ondcOption)
        assertTrue(ondcOption!!.provider == TransportProvider.ONDC_YATRI_SATHI || ondcOption.provider == TransportProvider.ONDC_NAMMA_YATRI)

        // Verify user preference match (Profile specifies "Uber Premier / Yatri Sathi")
        val preferredOption = comparison.options.firstOrNull { it.isPreferredByUser }
        assertNotNull("Must flag preferred user match", preferredOption)

        // Verify recommendation
        assertNotNull(comparison.recommendedOption)
        assertTrue(comparison.recommendedOption!!.isPreferredByUser || comparison.recommendedOption!!.isCheapest)

        // Formatted briefing
        val briefing = transportEngine.formatComparisonBriefing(comparison)
        assertTrue(briefing.contains("Transport Comparison to Sector V"))
        assertTrue(briefing.contains("Yatri Sathi (ONDC)"))
        assertTrue(briefing.contains("Uber"))
        assertTrue(briefing.contains("Recommended Dispatch"))
    }

    @Test
    fun `test 3 Financial Intelligence Engine calculates exact discretionary buffer and evaluates affordability`() = runBlocking {
        val financeEngine = FinancialIntelligenceEngine()

        // Monthly Income: 250,000; Fixed: 65,000; Savings: 50,000
        // Expected Discretionary = 250,000 - 65,000 - 50,000 = 135,000
        val expenses = listOf(
            ExpenseEntity(id = 1, title = "Groceries", amount = 15000.0, category = "FOOD", date = "2026-09-10"),
            ExpenseEntity(id = 2, title = "Electricity", amount = 8000.0, category = "UTILITIES", date = "2026-09-15")
        )
        // Total spent: 23,000; Remaining buffer: 135,000 - 23,000 = 112,000

        val snapshot = financeEngine.calculateFinancialSnapshot(masterProfile, expenses)
        assertEquals(250000.0, snapshot.monthlyIncome, 0.01)
        assertEquals(65000.0, snapshot.fixedCommitments, 0.01)
        assertEquals(50000.0, snapshot.monthlySavingsTarget, 0.01)
        assertEquals(135000.0, snapshot.totalDiscretionaryBudget, 0.01)
        assertEquals(23000.0, snapshot.totalSpentThisMonth, 0.01)
        assertEquals(112000.0, snapshot.remainingDiscretionaryBuffer, 0.01)
        assertEquals(20.0, snapshot.savingsRatePercent, 0.1)

        // Assessment 1: Affordable Item (₹45,000 monitor within ₹112,000 buffer)
        val affordable = financeEngine.assessAffordability("4K Monitor", 45000.0, masterProfile, expenses)
        assertEquals(AffordabilityVerdict.AFFORDABLE, affordable.verdict)
        assertTrue(affordable.verdict.isViable)

        // Assessment 2: High cost exceeding discretionary buffer (₹150,000 laptop > ₹112,000 buffer)
        val exceeds = financeEngine.assessAffordability("MacBook Pro", 150000.0, masterProfile, expenses)
        assertEquals(AffordabilityVerdict.EXCEEDS_DISCRETIONARY, exceeds.verdict)
        assertFalse(exceeds.verdict.isViable)
        assertTrue(exceeds.impactExplanation.contains("exceeds your remaining monthly discretionary buffer"))

        // Assessment 3: Formatted briefing output
        val formatted = financeEngine.formatAffordabilityBriefing(affordable)
        assertTrue(formatted.contains("Financial Affordability Analysis"))
        assertTrue(formatted.contains("Monthly Net Income"))
        assertTrue(formatted.contains("250000"))
        assertTrue(formatted.contains("Current Discretionary Buffer"))
        assertTrue(formatted.contains("112000"))
    }

    @Test
    fun `test 4 Communication Handoff Engine drafts tone-aligned message and prepares intent preview`() {
        val handoffEngine = CommunicationHandoffEngine()

        val draft = handoffEngine.draftMessage(
            channel = CommunicationChannel.WHATSAPP,
            recipientName = "Rahul",
            recipientContact = "+919876543210",
            topicOrPrompt = "Q3 quarterly production numbers",
            profile = masterProfile
        )

        assertEquals("Rahul", draft.recipientName)
        assertEquals("+919876543210", draft.recipientContact)
        assertEquals(CommunicationChannel.WHATSAPP, draft.channel)
        assertTrue("Message must reflect executive tone", draft.draftedMessage.contains("Dear Rahul") || draft.draftedMessage.contains("Q3 quarterly production numbers"))
        assertTrue("Draft must require confirmation", draft.requiresConfirmation)

        // Execute handoff
        val execution = handoffEngine.executeHandoff(context, draft)
        assertTrue(execution.isDispatched)
        assertEquals(CommunicationChannel.WHATSAPP, execution.channel)
        assertTrue(execution.message.contains("Prepared WhatsApp message to Rahul"))
    }

    @Test
    fun `test 5 Full Orchestrator handles Financial Affordability Analysis intent seamlessly`() = runBlocking {
        database.expenseDao().insertExpense(
            ExpenseEntity(title = "Office Supplies", amount = 12000.0, category = "OFFICE", date = "2026-09-20")
        )

        val result = orchestrator.processRequest(
            input = "Can I afford a new laptop within my monthly budget?",
            profile = masterProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.SUPPORTED_FINANCIAL_DATA),
            appContext = context,
            permissionManager = permissionManager
        )

        assertNotNull(result)
        assertEquals(UniversalIntentType.FINANCE_ANALYSIS, result.universalIntent.intentType)
        assertNotNull(result.verification)
        assertTrue(result.verification!!.isVerified)
        assertTrue(result.conversationalResponse.contains("Financial Affordability Analysis"))
        assertTrue(result.conversationalResponse.contains("Monthly Net Income"))
        assertTrue(result.conversationalResponse.contains("250000"))
        assertTrue(result.conversationalResponse.contains("Discretionary Spending Pool"))
    }

    @Test
    fun `test 6 Full Orchestrator handles Communication Dispatch with Draft Preview`() = runBlocking {
        val result = orchestrator.processRequest(
            input = "Send a message to Rahul regarding the contract agreement",
            profile = masterProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.CONTACTS),
            appContext = context,
            permissionManager = permissionManager
        )

        assertNotNull(result)
        assertEquals(UniversalIntentType.MESSAGE_REQUEST, result.universalIntent.intentType)
        assertEquals(com.example.domain.intelligence.IntentStatus.AWAITING_CONFIRMATION, result.universalIntent.status)
        assertNotNull(result.confirmationPrompt)
        assertTrue(result.conversationalResponse.contains("Rahul"))
        assertTrue(result.conversationalResponse.contains("Please confirm to proceed"))

        // Verify draft engine generates verified handoff payload
        val draft = orchestrator.communicationHandoffEngine.draftMessage(
            channel = CommunicationChannel.WHATSAPP,
            recipientName = "Rahul",
            recipientContact = "+919876543210",
            topicOrPrompt = "contract agreement",
            profile = masterProfile
        )
        assertNotNull(draft)
        assertTrue(draft.draftedMessage.contains("contract agreement"))
    }

    @Test
    fun `test 7 Full Orchestrator Personal Knowledge briefing includes Truth Model certainty`() = runBlocking {
        val result = orchestrator.processRequest(
            input = "What do you know about me?",
            profile = masterProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        assertNotNull(result)
        assertEquals(UniversalIntentType.PERSONAL_KNOWLEDGE_QUERY, result.universalIntent.intentType)
        assertTrue(result.conversationalResponse.contains("TRUTH & DATA CERTAINTY MODEL"))
        assertTrue(result.conversationalResponse.contains("[VERIFIED]"))
        assertTrue(result.conversationalResponse.contains("[ZERO HALLUCINATION]"))
        assertTrue(result.conversationalResponse.contains("Metropoli Fashions"))
    }
}
