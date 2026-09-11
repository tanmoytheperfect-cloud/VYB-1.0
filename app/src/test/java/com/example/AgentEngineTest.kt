package com.example

import com.example.domain.agent.*
import com.example.domain.orchestrator.VybOrchestrator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AgentEngineTest {

    private lateinit var orchestrator: VybOrchestrator
    private lateinit var agentEngine: AgentEngine

    @Before
    fun setUp() {
        orchestrator = VybOrchestrator()
        agentEngine = AgentEngine()
    }

    @Test
    fun testDailyPrioritiesReasoning() = runBlocking {
        val response = orchestrator.processMessage("What are my priorities today?", null)

        assertNotNull(response.conversationalResponse)
        assertTrue(response.conversationalResponse.contains("priority", ignoreCase = true) ||
                response.conversationalResponse.contains("priorities", ignoreCase = true))
        assertEquals("DAILY_PRIORITIES", response.diagnostics.intent)
        assertTrue(response.intentPayload.suggestedFollowUps.isNotEmpty())
    }

    @Test
    fun testWhatShouldIDoNowReasoning() = runBlocking {
        val response = orchestrator.processMessage("What should I do now?", null)

        assertNotNull(response.conversationalResponse)
        assertTrue(response.conversationalResponse.contains("priority", ignoreCase = true) ||
                response.conversationalResponse.contains("focus", ignoreCase = true) ||
                response.conversationalResponse.contains("Right now", ignoreCase = true))
        assertEquals("WHAT_SHOULD_I_DO_NOW", response.diagnostics.intent)
    }

    @Test
    fun testZeroSimulationAmazonDelivery() = runBlocking {
        // Zero fabrication: Amazon is not connected, so VYB must not invent tracking numbers or fake delivery trucks
        val response = orchestrator.processMessage("Where is my Amazon delivery?", null)

        assertNotNull(response.conversationalResponse)
        assertTrue(
            response.conversationalResponse.contains("not linked", ignoreCase = true) ||
            response.conversationalResponse.contains("not connected", ignoreCase = true) ||
            response.conversationalResponse.contains("Amazon app", ignoreCase = true)
        )
        // Must NOT contain hallucinated tracking numbers like "TRACK12345"
        assertFalse(response.conversationalResponse.contains("TRACK12345"))
        assertEquals("DELIVERY_TRACKING", response.diagnostics.intent)
    }

    @Test
    fun testTransportCoordinationRequiresConfirmation() = runBlocking {
        // Booking a cab for son Vivaan must recognize family routine and request confirmation
        val response = orchestrator.processMessage("Handle my son's school cab", null)

        assertNotNull(response.conversationalResponse)
        assertTrue(response.conversationalResponse.contains("ride", ignoreCase = true) ||
                response.conversationalResponse.contains("cab", ignoreCase = true) ||
                response.conversationalResponse.contains("transport", ignoreCase = true))

        assertNotNull(response.action)
        assertEquals("OPEN_RIDE_PROVIDER", response.action?.actionType)
        assertEquals("TRANSPORT_COORDINATION", response.diagnostics.intent)
    }

    @Test
    fun testFinancialAffordabilityMath() = runBlocking {
        // High amount exceeds discretionary buffer
        val expensiveResponse = orchestrator.processMessage("Can I afford ₹80,000?", null)
        assertTrue(expensiveResponse.conversationalResponse.contains("exceeds", ignoreCase = true) ||
                expensiveResponse.conversationalResponse.contains("defer", ignoreCase = true) ||
                expensiveResponse.conversationalResponse.contains("hold", ignoreCase = true))

        // Reasonable amount within buffer
        val affordableResponse = orchestrator.processMessage("Can I afford ₹2,000?", null)
        assertTrue(affordableResponse.conversationalResponse.contains("affordable", ignoreCase = true) ||
                affordableResponse.conversationalResponse.contains("safe", ignoreCase = true))
    }

    @Test
    fun testAgentMemoryStoreAndForget() = runBlocking {
        val storeResponse = agentEngine.processRequest("Remember that I prefer Yatri Sathi over Uber", null, null)
        assertTrue(storeResponse.conversationalReply.contains("stored that in your personal memory vault", ignoreCase = true))

        val forgetResponse = agentEngine.processRequest("Forget that preference", null, null)
        assertTrue(forgetResponse.conversationalReply.contains("removed", ignoreCase = true))
    }

    @Test
    fun testDeviceAlarmToolSelection() = runBlocking {
        val response = agentEngine.processRequest("Set an alarm for 7:00 AM", null, null)
        assertEquals(AgentIntentCategory.DEVICE_ALARM, response.intent.category)
        assertTrue(response.conversationalReply.contains("alarm", ignoreCase = true))
    }

    @Test
    fun testDeviceCalendarScheduleSelection() = runBlocking {
        val response = agentEngine.processRequest("What's on my calendar?", null, null)
        assertEquals(AgentIntentCategory.CALENDAR_SCHEDULE, response.intent.category)
        assertTrue(response.conversationalReply.contains("schedule", ignoreCase = true) ||
                response.conversationalReply.contains("calendar", ignoreCase = true))
    }
}
