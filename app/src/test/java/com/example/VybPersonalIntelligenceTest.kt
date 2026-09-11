package com.example

import com.example.domain.brain.DecisionOption
import com.example.domain.brain.VybBrain
import com.example.domain.capability.DigitalService
import com.example.domain.context.FollowUpResolution
import com.example.domain.memory.MemoryTier
import com.example.domain.orchestrator.VybOrchestrator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VybPersonalIntelligenceTest {

    private lateinit var brain: VybBrain
    private lateinit var orchestrator: VybOrchestrator

    @Before
    fun setUp() {
        brain = VybBrain()
        orchestrator = VybOrchestrator()
    }

    @Test
    fun testPersonalLifeModelStructure() {
        val model = brain.lifeModel
        assertNotNull(model.ownerProfile)
        assertEquals("Tanmoy", model.ownerProfile.value.name)
        assertTrue(model.dailyRoutines.value.isNotEmpty())
        assertTrue(model.familyMembers.value.isNotEmpty())
        assertTrue(model.financialContext.value.safeDiscretionaryBuffer > 0)
    }

    @Test
    fun testFinancialIntelligenceAffordabilityReasoning() {
        val financeEngine = brain.financialEngine

        // Reasonable purchase within discretionary pool
        val coffeeAnalysis = financeEngine.evaluateAffordability(
            query = "Specialty coffee",
            amountInr = 350.0,
            lifeModel = brain.lifeModel
        )
        assertTrue(coffeeAnalysis.isAffordableImmediately)
        assertTrue(
            coffeeAnalysis.recommendation.contains("discretionary", ignoreCase = true) ||
            coffeeAnalysis.recommendation.contains("safely", ignoreCase = true)
        )

        // Large purchase that strains discretionary funds
        val vacationAnalysis = financeEngine.evaluateAffordability(
            query = "Luxury Vacation Outlay",
            amountInr = 180000.0,
            lifeModel = brain.lifeModel
        )
        assertFalse(vacationAnalysis.isAffordableImmediately)
    }

    @Test
    fun testDecisionRetrievalForBicycle() = runBlocking {
        val response = orchestrator.processMessage("What did I decide about the bike?", null)

        assertNotNull(response.conversationalResponse)
        assertTrue(
            response.conversationalResponse.contains("bike", ignoreCase = true) ||
            response.conversationalResponse.contains("Classic 350", ignoreCase = true) ||
            response.conversationalResponse.contains("postpone", ignoreCase = true) ||
            response.conversationalResponse.contains("defer", ignoreCase = true)
        )
    }

    @Test
    fun testCanIAffordQuery() = runBlocking {
        val response = orchestrator.processMessage("Can I afford a Classic 350 right now?", null)

        assertNotNull(response.conversationalResponse)
        assertTrue(
            response.conversationalResponse.contains("₹") ||
            response.conversationalResponse.contains("afford", ignoreCase = true) ||
            response.conversationalResponse.contains("discretionary", ignoreCase = true)
        )
    }

    @Test
    fun testMultiTurnContextTrackingForCabs() = runBlocking {
        val convContext = brain.conversationContext

        // Turn 1: Options presented
        val options = listOf(
            DecisionOption("Yatri Sathi", "Regulated metered taxi", 110.0, emptyList(), emptyList()),
            DecisionOption("inDrive", "Negotiated fare", 120.0, emptyList(), emptyList())
        )
        convContext.updateContext(
            domain = "CAB_TRANSPORT",
            subject = "Son's School Cab to Delhi Public School",
            options = options,
            selected = options.first(),
            pendingAction = "OPEN_RIDE_PROVIDER"
        )

        // Turn 2: User asks for cheapest
        val resolved = convContext.resolveFollowUp("cheapest")
        assertTrue(resolved is FollowUpResolution.OptionSelected)
        assertEquals("Yatri Sathi", (resolved as FollowUpResolution.OptionSelected).selectedOption.name)
    }

    @Test
    fun testOwnerLearningCorrection() = runBlocking {
        val memoryEngine = brain.memoryEngine

        // User corrects a preference
        memoryEngine.learnFromCorrection("I always take Yatri Sathi for school commute")

        val retrieved = memoryEngine.getMemoriesByTier(MemoryTier.PREFERENCE)
        assertTrue(retrieved.any { it.content.contains("Yatri Sathi", ignoreCase = true) })
    }

    @Test
    fun testTemporalMorningBriefingGeneration() {
        val briefing = brain.getMorningBriefing()

        assertNotNull(briefing.headline)
        assertTrue(briefing.summaryBullets.isNotEmpty())
        assertNotNull(briefing.priorityAction)
        assertNotNull(briefing.financialSnapshot)
    }

    @Test
    fun testWhatMattersNowSummary() {
        val matters = brain.getWhatMattersNow()

        assertNotNull(matters.priorityTitle)
        assertNotNull(matters.priorityDescription)
        assertNotNull(matters.financialDiscretionaryRemaining)
        assertNotNull(matters.recommendedNextStep)
    }

    @Test
    fun testRecommendedActionsAvailability() {
        val actions = brain.getRecommendedActions()

        assertTrue(actions.isNotEmpty())
        assertTrue(actions.any { it.category == "TRANSPORT" })
        assertTrue(actions.any { it.category == "FINANCE" })
    }

    @Test
    fun testZeroHallucinationDeviceIntegrity() {
        val connectedServices = brain.connectedServicesManager

        // Amazon account is not connected via API
        assertFalse(connectedServices.isServiceConnected(DigitalService.AMAZON))
        val report = connectedServices.getServiceReport(DigitalService.AMAZON)
        assertTrue(report.statusSummary.isNotEmpty())
    }
}
