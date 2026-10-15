package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.domain.intent.IntentParser
import com.example.domain.intent.StructuredIntent
import com.example.domain.orchestrator.VybOrchestrator
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
class VybRuntimeInvestigationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var orchestrator: VybOrchestrator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        orchestrator = VybOrchestrator(
            memoryDao = database.memoryDao(),
            taskDao = database.taskDao(),
            lifeVaultDao = database.lifeVaultDao(),
            routineDao = database.routineDao(),
            learnedRoutineDao = database.learnedRoutineDao(),
            familyDao = database.familyDao(),
            goalDao = database.goalDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWhatMattersToday_NoGenericFallback() = runBlocking {
        val input = "good morning vyb, what matters today"
        val result = orchestrator.processMessage(input, context)

        // Verify it NEVER produces the generic fallback
        val genericFallback = "Understood, Owner. How can I assist you with your schedule, transport, finances, family routines, or mobile apps today?"
        assertNotEquals(genericFallback, result.conversationalResponse)
        assertFalse(result.conversationalResponse.contains("How can I assist you with your schedule, transport, finances"))

        // Verify it provides truthful context regarding schedule and device
        assertTrue(
            result.conversationalResponse.contains("schedule") ||
            result.conversationalResponse.contains("Calendar") ||
            result.conversationalResponse.contains("Good morning") ||
            result.conversationalResponse.contains("matters today")
        )
    }

    @Test
    fun testWhatShouldIDoNow() = runBlocking {
        val input = "what should I do now?"
        val result = orchestrator.processMessage(input, context)

        assertFalse(result.conversationalResponse.contains("Understood, Owner"))
        assertTrue(
            result.conversationalResponse.contains("tasks") ||
            result.conversationalResponse.contains("Current time") ||
            result.conversationalResponse.contains("schedule")
        )
    }

    @Test
    fun testWhatDoYouKnowAboutMyDay() = runBlocking {
        val input = "what do you know about my day?"
        val result = orchestrator.processMessage(input, context)

        assertTrue(result.conversationalResponse.contains("Calendar"))
        assertTrue(result.conversationalResponse.contains("Routines"))
        assertTrue(result.conversationalResponse.contains("Tasks"))
    }

    @Test
    fun testNextImportantThing() = runBlocking {
        val input = "what is my next important thing?"
        val result = orchestrator.processMessage(input, context)

        assertFalse(result.conversationalResponse.contains("Understood, Owner"))
        assertTrue(
            result.conversationalResponse.contains("upcoming tasks", ignoreCase = true) ||
            result.conversationalResponse.contains("priority", ignoreCase = true) ||
            result.conversationalResponse.contains("scheduled", ignoreCase = true) ||
            result.conversationalResponse.contains("tasks", ignoreCase = true)
        )
    }

    @Test
    fun testRememberPreferenceAndRecall() = runBlocking {
        // Step 1: Tell VYB a preference
        val storeInput = "remember that I prefer the cheaper option"
        val storeResult = orchestrator.processMessage(storeInput, context)

        assertTrue(
            storeResult.conversationalResponse.contains("stored that in your long-term memory", ignoreCase = true) ||
            storeResult.conversationalResponse.contains("Personal Life Vault", ignoreCase = true) ||
            storeResult.conversationalResponse.contains("Saved", ignoreCase = true)
        )

        // Step 2: Query what was just told
        val recallInput = "what did I just tell you?"
        val recallResult = orchestrator.processMessage(recallInput, context)

        assertTrue(
            recallResult.conversationalResponse.contains("cheaper", ignoreCase = true) ||
            recallResult.conversationalResponse.contains("prefer", ignoreCase = true)
        )
    }

    @Test
    fun testUpdateHomeScreenIntents() = runBlocking {
        val input1 = "update home screen"
        val result1 = orchestrator.processMessage(input1, context)
        assertEquals(StructuredIntent.INTENT_UPDATE_HOME_SCREEN, result1.diagnostics.intent)
        assertTrue(result1.conversationalResponse.contains("home screen", ignoreCase = true))

        val input2 = "change VYB's home screen"
        val result2 = orchestrator.processMessage(input2, context)
        assertEquals(StructuredIntent.INTENT_UPDATE_HOME_SCREEN, result2.diagnostics.intent)
    }

    @Test
    fun testOpenAmazon() = runBlocking {
        val input = "open Amazon"
        val intent = IntentParser.parse(input)
        assertEquals(StructuredIntent.INTENT_OPEN_APP, intent.intent)
        assertEquals("Amazon", intent.entities["app_name"])
    }

    @Test
    fun testSetAlarmFor7() = runBlocking {
        val input = "set an alarm for 7"
        val result = orchestrator.processMessage(input, context)
        assertEquals(StructuredIntent.INTENT_SET_ALARM, result.diagnostics.intent)
        assertTrue(result.conversationalResponse.contains("Alarm") || result.conversationalResponse.contains("07:00"))
    }
}
