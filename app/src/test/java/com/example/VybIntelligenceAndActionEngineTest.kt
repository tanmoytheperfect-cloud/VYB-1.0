package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LifeVaultItemEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.domain.genesis.*
import com.example.domain.intelligence.*
import com.example.domain.permission.VybPermissionManager
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
class VybIntelligenceAndActionEngineTest {

    private lateinit var context: Context
    private lateinit var orchestrator: VybIntelligenceOrchestrator
    private lateinit var database: AppDatabase
    private lateinit var permissionManager: VybPermissionManager

    private val sampleProfile = VybPersonalProfile(
        name = "Tanmoy",
        callingTitle = "Sir",
        officeAddress = "Sector V, Tech Hub, Kolkata",
        homeAddress = "Salt Lake City, Block AB",
        commutePreference = "Uber / Yatri Sathi",
        monthlyIncome = 150000.0,
        fixedCommitments = 45000.0,
        savingsTarget = 30000.0,
        currencySymbol = "₹"
    )

    @Before
    fun setup() {
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
            routineDao = database.routineDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test 1 Universal Intent Engine parses intents with entities, capabilities and risk`() {
        val intentEngine = UniversalIntentEngine()

        // 1. Reminder Intent
        val reminderIntent = intentEngine.parseIntent("Remind me tomorrow to call Rahul")
        assertEquals(UniversalIntentType.CREATE_REMINDER, reminderIntent.intentType)
        assertEquals("Tomorrow", reminderIntent.entities["time"])
        assertEquals("Rahul", reminderIntent.entities["contact_name"])
        assertEquals(RiskLevel.LOW, reminderIntent.riskLevel)
        assertFalse(reminderIntent.confirmationRequired)

        // 2. Cab Booking Intent
        val cabIntent = intentEngine.parseIntent("Book my usual cab to office")
        assertEquals(UniversalIntentType.BOOK_TRANSPORT, cabIntent.intentType)
        assertEquals("Office", cabIntent.entities["destination"])
        assertEquals(RiskLevel.HIGH, cabIntent.riskLevel)
        assertTrue(cabIntent.confirmationRequired)

        // 3. Shopping Query
        val shopIntent = intentEngine.parseIntent("What should I buy for my son's school?")
        assertEquals(UniversalIntentType.SHOPPING_QUERY, shopIntent.intentType)
        assertEquals(RiskLevel.LOW, shopIntent.riskLevel)

        // 4. Finance Query
        val financeIntent = intentEngine.parseIntent("Can I afford a new laptop within my monthly budget?")
        assertEquals(UniversalIntentType.FINANCE_ANALYSIS, financeIntent.intentType)
        assertEquals(RiskLevel.MEDIUM, financeIntent.riskLevel)
    }

    @Test
    fun `test 2 Context Resolution Engine resolves implicit preferences or asks user`() {
        val resolver = ContextResolutionEngine()

        // Test with known profile office address
        val resolvedWithContext = resolver.resolveContext(
            rawInput = "Book my usual cab",
            intentType = UniversalIntentType.BOOK_TRANSPORT,
            extractedEntities = emptyMap(),
            profile = sampleProfile
        )

        assertTrue(resolvedWithContext.isSufficient)
        assertEquals("Sector V, Tech Hub, Kolkata", resolvedWithContext.contextualDestination)
        assertNotNull(resolvedWithContext.contextualService)
        assertEquals(1.0f, resolvedWithContext.confidenceScore, 0.01f)
        assertNull(resolvedWithContext.clarificationQuestion)

        // Test with empty destination profile -> MUST ask user, NEVER guess!
        val emptyProfile = VybPersonalProfile(name = "User", callingTitle = "Sir")
        val resolvedInsufficient = resolver.resolveContext(
            rawInput = "Book my usual cab",
            intentType = UniversalIntentType.BOOK_TRANSPORT,
            extractedEntities = emptyMap(),
            profile = emptyProfile
        )

        assertFalse(resolvedInsufficient.isSufficient)
        assertNotNull(resolvedInsufficient.clarificationQuestion)
        assertTrue(resolvedInsufficient.clarificationQuestion!!.contains("office or usual destination address"))
    }

    @Test
    fun `test 3 Contextual Memory Retrieval applies relevance, privacy, permission, confidence filters`() {
        val retrievalEngine = ContextualMemoryRetrievalEngine()

        val memories = listOf(
            MemoryItemEntity(id = 1, category = "FAMILY", key = "Son's School", value = "Heritage Academy starts at 8:30 AM", confidence = 0.95),
            MemoryItemEntity(id = 2, category = "FINANCIAL", key = "Salary", value = "Monthly salary credit is ₹150,000", confidence = 0.98),
            MemoryItemEntity(id = 3, category = "PREFERENCES", key = "Commute", value = "Prefers Yatri Sathi or Uber Premier", confidence = 0.90),
            MemoryItemEntity(id = 4, category = "HABIT", key = "Low confidence guess", value = "Might like tea", confidence = 0.30)
        )

        val vaultItems = listOf(
            LifeVaultItemEntity(id = 1, category = "FAMILY", key = "Son Uniform", value = "Navy Blue blazer", source = "USER"),
            LifeVaultItemEntity(id = 2, category = "FINANCIAL", key = "Secret Account", value = "Account #9988", source = "USER", isPendingConflict = true)
        )

        // Query: "What should I buy for my son's school?" with FAMILY authorized, FINANCIAL NOT authorized
        val retrieved = retrievalEngine.retrieveRelevantContext(
            query = "What should I buy for my son's school?",
            intentType = UniversalIntentType.SHOPPING_QUERY,
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.SUPPORTED_COMMUNICATION_DATA),
            allMemories = memories,
            allVaultItems = vaultItems
        )

        // Must retrieve Son's school & uniform
        assertTrue("Must include son's school memory", retrieved.relevantMemories.any { it.key == "Son's School" })
        assertTrue("Must include son uniform vault item", retrieved.relevantVaultItems.any { it.key == "Son Uniform" })

        // Must NOT leak financial salary or secret account (unauthorized category and conflict)
        assertFalse("Must NOT leak salary", retrieved.relevantMemories.any { it.key == "Salary" })
        assertFalse("Must NOT leak secret account", retrieved.relevantVaultItems.any { it.key == "Secret Account" })

        // Must filter out low confidence guess (< 0.5)
        assertFalse("Must filter low confidence", retrieved.relevantMemories.any { it.confidence < 0.5 })
    }

    @Test
    fun `test 4 Capability Registry declares 23 capabilities and verifies truth`() {
        val registry = VybCapabilityRegistry()
        val allCaps = registry.getAllCapabilities(context, permissionManager)

        assertEquals("Must declare all 23 standard capabilities", 23, allCaps.size)

        val chatCap = allCaps.first { it.capabilityId == "CHAT" }
        assertEquals(CapabilityAvailability.ACTIVE, chatCap.availability)

        val memoryCap = allCaps.first { it.capabilityId == "MEMORY" }
        assertEquals(CapabilityAvailability.ACTIVE, memoryCap.availability)

        val shoppingCap = allCaps.first { it.capabilityId == "SHOPPING" }
        assertEquals(CapabilityAvailability.ACTIVE, shoppingCap.availability)

        val codingCap = allCaps.first { it.capabilityId == "CODING" }
        assertEquals(CapabilityAvailability.ACTIVE, codingCap.availability)

        // Each capability must have a declared verification method
        allCaps.forEach { cap ->
            assertTrue("Capability ${cap.capabilityId} must have verificationMethod", cap.verificationMethod.isNotBlank())
        }
    }

    @Test
    fun `test 5 Agent Framework and 10-step Action Plan for Travel Agent`() = runBlocking {
        val agent = TravelAgent()
        val intent = UniversalIntent(
            rawInput = "Find the cheapest suitable cab for me and prepare the booking",
            intentType = UniversalIntentType.BOOK_TRANSPORT,
            entities = mapOf("destination" to "Sector V, Tech Hub")
        )

        val resolvedContext = ResolvedPersonalContext(
            ownerName = "Tanmoy",
            callingTitle = "Sir",
            contextualDestination = "Sector V, Tech Hub",
            contextualService = "Yatri Sathi"
        )

        val plan = agent.createPlan(intent, resolvedContext, context, permissionManager)

        assertEquals(10, plan.steps.size)
        assertEquals(RiskLevel.HIGH, plan.riskLevel)
        assertTrue("Step 7 must require confirmation", plan.steps[7].requiresConfirmation)
        assertTrue("Step 8 must require confirmation", plan.steps[8].requiresConfirmation)
    }

    @Test
    fun `test 6 Full 11-stage Intelligence Orchestrator pipeline execution and verification`() = runBlocking {
        val result = orchestrator.processRequest(
            input = "Remind me tomorrow to call Rahul",
            profile = sampleProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        assertNotNull(result)
        assertEquals(UniversalIntentType.CREATE_REMINDER, result.universalIntent.intentType)
        assertNotNull(result.verification)
        assertTrue("Verification must pass", result.verification!!.isVerified)
        assertTrue("Must have 11-stage pipeline trace", result.pipelineTrace.size >= 10)
        assertTrue(result.conversationalResponse.contains("[✓ Verified:"))
    }

    @Test
    fun `test 7 High-risk action triggers confirmation prompt with clear preview`() = runBlocking {
        val result = orchestrator.processRequest(
            input = "Book my usual cab to office",
            profile = sampleProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE, GenesisDataSource.LOCATION),
            appContext = context,
            permissionManager = permissionManager
        )

        assertNotNull(result.confirmationPrompt)
        val prompt = result.confirmationPrompt!!
        assertEquals(RiskLevel.HIGH, prompt.riskLevel)
        assertTrue(prompt.whatWillHappen.isNotBlank())
        assertTrue(prompt.recipientOrService.isNotBlank())
        assertTrue(prompt.dataShared.contains("destination"))
        assertTrue(prompt.estimatedConsequence.contains("fare charge") || prompt.estimatedConsequence.contains("driver request"))
        assertEquals(IntentStatus.AWAITING_CONFIRMATION, result.universalIntent.status)
    }

    @Test
    fun `test 8 Outcome learning updates Personal Context Graph and Profile`() {
        val outcomeEngine = OutcomeLearningEngine()
        val intent = UniversalIntent(
            rawInput = "Book my usual cab to Sector V",
            intentType = UniversalIntentType.BOOK_TRANSPORT,
            status = IntentStatus.COMPLETED
        )

        val context = ResolvedPersonalContext(
            contextualService = "Yatri Sathi",
            contextualDestination = "Sector V Tech Hub"
        )

        val verification = VerificationResult(
            isVerified = true,
            method = "AndroidIntentDispatchProof",
            proofDescription = "Dispatched with verified GPS coordinates."
        )

        val (updatedProfile, updatedGraph) = outcomeEngine.learnFromActionOutcome(
            intent = intent,
            context = context,
            verification = verification,
            existingProfile = sampleProfile,
            existingGraph = PersonalContextGraph()
        )

        assertTrue("Profile commute preference updated", updatedProfile.commutePreference.contains("Yatri Sathi"))
        assertTrue("Context graph has service node", updatedGraph.nodes.any { it.label == "Yatri Sathi" && it.classification == InformationClassification.CONFIRMED })
        assertTrue("Context graph has destination node", updatedGraph.nodes.any { it.label == "Sector V Tech Hub" && it.classification == InformationClassification.CONFIRMED })
    }
}
