package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.domain.genesis.GenesisDataSource
import com.example.domain.genesis.PersonalContextGraph
import com.example.domain.genesis.VybPersonalProfile
import com.example.domain.intelligence.PersonalContextDiagnostics
import com.example.domain.intelligence.UniversalIntentType
import com.example.domain.intelligence.VybIntelligenceOrchestrator
import com.example.domain.orchestrator.VybOrchestrator
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
class VybPersonalIntelligenceRealityTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var permissionManager: VybPermissionManager

    private val testProfile = VybPersonalProfile(
        name = "Tanmoy",
        callingTitle = "Sir",
        jobTitle = "Founder & Chief Executive",
        companyName = "Metropoli Fashions",
        officeAddress = "Sector V, Tech Hub, Kolkata",
        homeAddress = "Salt Lake City, Block AB, Kolkata",
        monthlyIncome = 250000.0,
        fixedCommitments = 65000.0,
        savingsTarget = 50000.0,
        currencySymbol = "₹"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        permissionManager = VybPermissionManager(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createIntelligenceOrchestrator(): VybIntelligenceOrchestrator {
        return VybIntelligenceOrchestrator(
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

    private fun createVybOrchestrator(): VybOrchestrator {
        return VybOrchestrator(
            memoryDao = database.memoryDao(),
            taskDao = database.taskDao(),
            lifeVaultDao = database.lifeVaultDao(),
            routineDao = database.routineDao(),
            learnedRoutineDao = database.learnedRoutineDao(),
            familyDao = database.familyDao(),
            goalDao = database.goalDao()
        )
    }

    @Test
    fun `test 1 Store memory, simulate process restart, and verify memory survives and is recalled`() = runBlocking {
        // Step 1: Store memory in initial session
        var orchestrator = createIntelligenceOrchestrator()
        val storeResult = orchestrator.processRequest(
            input = "Remember that my son's school is St. Xavier's High School",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )
        assertNotNull(storeResult)
        assertEquals(UniversalIntentType.MEMORY_WRITE, storeResult.universalIntent.intentType)
        assertTrue(storeResult.conversationalResponse.contains("St. Xavier's High School", ignoreCase = true))

        // Step 2: Simulate process death and app restart by recreating the orchestrator with fresh in-memory instance
        orchestrator = createIntelligenceOrchestrator()

        // Step 3: Query the memory
        val recallResult = orchestrator.processRequest(
            input = "What is my son's school?",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        assertNotNull(recallResult)
        assertEquals(UniversalIntentType.MEMORY_QUERY, recallResult.universalIntent.intentType)
        assertTrue("Memory must be retrieved from database across process restart",
            recallResult.conversationalResponse.contains("St. Xavier's High School", ignoreCase = true))
        assertFalse(recallResult.conversationalResponse.contains("I don't have enough schedule or routine details yet"))
    }

    @Test
    fun `test 2 Store multiple memories and retrieve only the relevant memory`() = runBlocking {
        val orchestrator = createIntelligenceOrchestrator()

        orchestrator.processRequest(
            input = "Remember that my office is in Sector V, Tech Hub, Kolkata",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        orchestrator.processRequest(
            input = "Remember that my daily protein goal is 140 grams",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        orchestrator.processRequest(
            input = "Remember that I prefer Uber Premier cabs",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        // Query only office
        val officeResult = orchestrator.processRequest(
            input = "Where is my office?",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )
        assertTrue(officeResult.conversationalResponse.contains("Sector V", ignoreCase = true))
        assertFalse("Must not dump unrelated protein memory", officeResult.conversationalResponse.contains("protein", ignoreCase = true))

        // Query only protein
        val proteinResult = orchestrator.processRequest(
            input = "What is my protein goal?",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )
        assertTrue(proteinResult.conversationalResponse.contains("140 grams", ignoreCase = true))
        assertFalse("Must not dump unrelated office memory", proteinResult.conversationalResponse.contains("Sector V", ignoreCase = true))
    }

    @Test
    fun `test 3 What else do you remember returns structured summary of persisted categories with no generic fallback`() = runBlocking {
        val orchestrator = createIntelligenceOrchestrator()

        orchestrator.processRequest(
            input = "Remember that my son's school is St. Xavier's High School",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        val result = orchestrator.processRequest(
            input = "What else do you remember?",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        assertNotNull(result)
        assertEquals(UniversalIntentType.MEMORY_QUERY, result.universalIntent.intentType)
        // Verify real persisted categories are returned
        assertTrue(result.conversationalResponse.contains("IDENTITY", ignoreCase = true))
        assertTrue(result.conversationalResponse.contains("Tanmoy", ignoreCase = true))
        assertTrue(result.conversationalResponse.contains("St. Xavier's High School", ignoreCase = true))
        // Verify generic fallback is NOT present
        assertFalse(result.conversationalResponse.contains("I am tracking your on-device context. I don't have enough schedule"))
        assertFalse(result.conversationalResponse.contains("ready to help organize your calendar, tasks, transport"))
    }

    @Test
    fun `test 4 Ask about unknown fact and verify VYB truthfully reports absence of record without hallucination`() = runBlocking {
        val orchestrator = createIntelligenceOrchestrator()

        val result = orchestrator.processRequest(
            input = "What is my brother's shoe size?",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        assertNotNull(result)
        assertTrue(
            result.conversationalResponse.contains("don't have a record", ignoreCase = true) ||
            result.conversationalResponse.contains("didn't find", ignoreCase = true) ||
            result.conversationalResponse.contains("no stored records", ignoreCase = true)
        )
        assertFalse("Must not invent a shoe size", result.conversationalResponse.contains("size 9", ignoreCase = true))
        assertFalse("Must not invent a shoe size", result.conversationalResponse.contains("size 10", ignoreCase = true))
    }

    @Test
    fun `test 5 User correction takes precedence over older memory`() = runBlocking {
        val orchestrator = createIntelligenceOrchestrator()

        // 1. Initial statement
        orchestrator.processRequest(
            input = "Remember that my son's school starts at 8:00 AM",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        // 2. Correction
        orchestrator.processRequest(
            input = "Remember that my son's school starts at 8:30 AM",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        // 3. Query
        val recallResult = orchestrator.processRequest(
            input = "What time does my son's school start?",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        assertTrue("Newest correction (8:30 AM) must take precedence",
            recallResult.conversationalResponse.contains("8:30 AM", ignoreCase = true))
    }

    @Test
    fun `test 6 Distinguish current conversation context from persistent memory`() = runBlocking {
        val orchestrator = createIntelligenceOrchestrator()

        // 1. User says something in current session without "remember that"
        orchestrator.processRequest(
            input = "My son has school tomorrow",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        // 2. User asks what was just said
        val result = orchestrator.processRequest(
            input = "What did I just tell you?",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        assertTrue("Must retrieve recent conversation turn",
            result.conversationalResponse.contains("My son has school tomorrow", ignoreCase = true))
        assertTrue("Must tag with CONVERSATION_CONTEXT",
            result.conversationalResponse.contains("CONVERSATION_CONTEXT", ignoreCase = true))
    }

    @Test
    fun `test 7 Genuinely empty database returns truthful absence of memories with no generic fallback`() = runBlocking {
        val orchestrator = createIntelligenceOrchestrator()
        val emptyProfile = VybPersonalProfile()

        val result = orchestrator.processRequest(
            input = "What else do you remember?",
            profile = emptyProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        assertTrue(result.conversationalResponse.contains("I don't currently have any saved personal memories", ignoreCase = true))
        assertFalse(result.conversationalResponse.contains("I am tracking your on-device context. I don't have enough schedule"))
    }

    @Test
    fun `test 8 VybOrchestrator handles What else do you remember with real data and no generic fallback`() = runBlocking {
        val vybOrch = createVybOrchestrator()

        // Store a memory through VybOrchestrator
        vybOrch.processMessage("remember that I prefer the cheaper option", context)

        // Query what else do you remember
        val result = vybOrch.processMessage("What else do you remember?", context)

        assertFalse(result.conversationalResponse.contains("I am tracking your on-device context. I don't have enough schedule"))
        assertTrue(
            result.conversationalResponse.contains("cheaper", ignoreCase = true) ||
            result.conversationalResponse.contains("Personal Memory", ignoreCase = true) ||
            result.conversationalResponse.contains("PREFERENCES", ignoreCase = true)
        )
    }

    @Test
    fun `test 9 PersonalContextDiagnostics reflects accurate live on-device context counts`() = runBlocking {
        val orchestrator = createIntelligenceOrchestrator()

        orchestrator.processRequest(
            input = "Remember that my office is in Sector V, Tech Hub, Kolkata",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = permissionManager
        )

        val diag = PersonalContextDiagnostics.inspectPersonalContext(
            database = database,
            profile = testProfile,
            conversationContextEngine = orchestrator.conversationContextEngine
        )

        assertTrue("Persisted count must be >= 1", diag.persistedMemoryCount >= 1)
        assertTrue(diag.ownerContextAvailable)
        assertTrue(diag.conversationContextAvailable)
        assertNotNull(diag.lastSuccessfulMemoryRetrieval)
    }
}
