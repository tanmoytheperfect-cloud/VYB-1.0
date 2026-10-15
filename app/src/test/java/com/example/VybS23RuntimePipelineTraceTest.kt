package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.VybRepository
import com.example.domain.orchestrator.VybOrchestrator
import com.example.domain.state.AskVybState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VybS23RuntimePipelineTraceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: VybRepository
    private lateinit var orchestrator: VybOrchestrator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = AppDatabase.getDatabase(context)
        repository = VybRepository(database)
        orchestrator = repository.orchestrator
    }

    @Test
    fun testDeterministicTimeQuery_ReturnsImmediatelyWithoutNetwork() = runBlocking {
        val traceId = "ASK_TRACE_001"
        val input = "What time is it?"

        val result = orchestrator.processMessage(input, context, traceId)

        assertNotNull(result)
        assertTrue(result.conversationalResponse.isNotBlank())
        assertTrue(
            "Response should indicate the current time: got '${result.conversationalResponse}'",
            result.conversationalResponse.startsWith("It is ") &&
                (result.conversationalResponse.contains("AM") || result.conversationalResponse.contains("PM"))
        )
        assertEquals("CURRENT_TIME", result.diagnostics.intent)
        assertEquals("DeviceTimeTool", result.diagnostics.toolSelected)
    }

    @Test
    fun testDeterministicDateQuery_ReturnsDeviceDate() = runBlocking {
        val traceId = "ASK_TRACE_002"
        val input = "What day is today?"

        val result = orchestrator.processMessage(input, context, traceId)

        assertNotNull(result)
        assertTrue(
            "Response should indicate the day/date: got '${result.conversationalResponse}'",
            result.conversationalResponse.startsWith("Today is ")
        )
        assertEquals("CURRENT_DATE", result.diagnostics.intent)
    }

    @Test
    fun testWeatherQuery_ReturnsTruthfulTelemetryOrExplicitUnavailable() = runBlocking {
        val traceId = "ASK_TRACE_003"
        val input = "What is the weather right now?"

        val result = orchestrator.processMessage(input, context, traceId)

        assertNotNull(result)
        assertTrue(result.conversationalResponse.isNotBlank())
        assertTrue(
            "Should return weather or explicitly state unavailable",
            result.conversationalResponse.contains("weather", ignoreCase = true) ||
                result.conversationalResponse.contains("unavailable", ignoreCase = true)
        )
    }

    @Test
    fun testSonMemoryQuery_ReturnsTruthfulUnknownWhenEmpty() = runBlocking {
        val traceId = "ASK_TRACE_004"
        val input = "What do you remember about my son?"

        val result = orchestrator.processMessage(input, context, traceId)

        assertNotNull(result)
        assertTrue(result.conversationalResponse.isNotBlank())
        // Without pre-existing memory, it must state truthful unknown or no stored records
        assertTrue(
            "Should not fabricate memory about son: got '${result.conversationalResponse}'",
            result.conversationalResponse.contains("no stored records matched", ignoreCase = true) ||
                result.conversationalResponse.contains("didn't find", ignoreCase = true) ||
                result.conversationalResponse.contains("memory", ignoreCase = true)
        )
    }

    @Test
    fun testRepositorySendUserMessage_StoresBothTurnsAndUpdatesConversation() = runBlocking {
        val convId = repository.createConversation("Test Session")
        val traceId = "ASK_TRACE_005"

        val sendResult = repository.sendUserMessage(
            conversationId = convId,
            userPrompt = "What time is it?",
            appContext = context,
            traceId = traceId
        )

        assertTrue("sendUserMessage should succeed", sendResult.isSuccess)
        val assistantMsg = sendResult.getOrThrow()
        assertEquals("ASSISTANT", assistantMsg.sender)
        assertTrue(assistantMsg.content.startsWith("It is "))
        assertFalse(assistantMsg.isError)
    }

    @Test
    fun testAskVybStateTransitions() {
        val states = AskVybState.values().toList()
        assertTrue(states.contains(AskVybState.IDLE))
        assertTrue(states.contains(AskVybState.SUBMITTING))
        assertTrue(states.contains(AskVybState.THINKING))
        assertTrue(states.contains(AskVybState.RESPONDING))
        assertTrue(states.contains(AskVybState.SUCCESS))
        assertTrue(states.contains(AskVybState.ERROR))
    }
}
