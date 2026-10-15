package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.domain.genesis.GenesisDataSource
import com.example.domain.genesis.PersonalContextGraph
import com.example.domain.genesis.VybPersonalProfile
import com.example.domain.intelligence.UniversalIntentType
import com.example.domain.intelligence.VybIntelligenceOrchestrator
import com.example.domain.permission.VybPermissionManager
import com.example.domain.persistence.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VybOwnerPersistenceReinstallTest {

    private lateinit var context: Context
    private val testOwnerEmail = "test_owner_persistence@vybos.internal"
    private lateinit var stableOwnerId: String

    private val testProfile = VybPersonalProfile(
        name = "Tanmoy Mukherjee",
        callingTitle = "Sir",
        companyName = "Metropoli Fashions",
        jobTitle = "Chief Executive",
        homeAddress = "Salt Lake City, Block AB, Kolkata",
        officeAddress = "Sector V, Tech Hub, Kolkata",
        monthlyIncome = 250000.0,
        fixedCommitments = 65000.0,
        savingsTarget = 50000.0,
        currencySymbol = "₹"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        stableOwnerId = VybOwnerIdentityManager.computeStableOwnerId(testOwnerEmail)
        VybOwnerIdentityManager.setOwnerIdentity(
            context,
            VybOwnerIdentity(
                ownerId = stableOwnerId,
                ownerEmail = testOwnerEmail,
                ownerName = "Tanmoy Mukherjee"
            )
        )
    }

    @After
    fun tearDown() {
        // Clean up test vault file if exists
        val vaultDir = File(context.filesDir, "vyb_durable_vault")
        if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
        }
    }

    private fun createDb(): AppDatabase {
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Test
    fun `acceptance test - owner intelligence survives uninstall and reinstall with first-launch restoration`() = runBlocking {
        // ==========================================
        // PHASE A: INITIAL SESSION & DATA PERSISTENCE
        // ==========================================
        val db1 = createDb()
        val persistenceEngine = VybCloudPersistenceEngine(context)

        // 1. Store 3 Owner Facts (TEST DATA)
        db1.lifeVaultDao().insertVaultItem(
            LifeVaultItemEntity(
                category = "ME",
                key = "Full Name",
                value = "Tanmoy Mukherjee",
                source = "USER_PROVIDED",
                isConfirmed = true
            )
        )
        db1.lifeVaultDao().insertVaultItem(
            LifeVaultItemEntity(
                category = "WORK",
                key = "Company Name",
                value = "Metropoli Fashions",
                source = "USER_PROVIDED",
                isConfirmed = true
            )
        )
        db1.lifeVaultDao().insertVaultItem(
            LifeVaultItemEntity(
                category = "PLACES",
                key = "Office Address",
                value = "Sector V, Tech Hub, Kolkata",
                source = "USER_PROVIDED",
                isConfirmed = true
            )
        )

        // 2. Store 2 Preferences (TEST DATA)
        db1.memoryDao().insertMemory(
            MemoryItemEntity(
                category = "PREFERENCE",
                key = "Preferred Transit Mode",
                value = "Uber Premier Cabs",
                source = "User Conversation"
            )
        )
        db1.memoryDao().insertMemory(
            MemoryItemEntity(
                category = "PREFERENCE",
                key = "Diet Target",
                value = "140 grams daily protein",
                source = "User Conversation"
            )
        )

        // 3. Store 2 Routines (TEST DATA)
        db1.routineDao().insertRoutine(
            RoutineEntity(
                title = "Morning Commute",
                timeOfDay = "08:45 AM",
                daysOfWeek = "Mon-Fri",
                description = "Sector V Commute",
                actionPrompt = "Book ride",
                category = "COMMUTE"
            )
        )
        db1.routineDao().insertRoutine(
            RoutineEntity(
                title = "Evening Reflection",
                timeOfDay = "10:00 PM",
                daysOfWeek = "Daily",
                description = "Day summary",
                actionPrompt = "Run reflection",
                category = "LEARNING"
            )
        )

        // 4. Store 1 Goal (TEST DATA)
        db1.goalDao().insertGoal(
            GoalEntity(
                title = "Monthly Discretionary Savings",
                category = "FINANCE",
                targetValue = "50000",
                currentValue = "35000",
                targetDeadline = "End of Month"
            )
        )

        // 5. Store 1 Family Context Item (TEST DATA)
        db1.familyDao().insertFamilyMember(
            FamilyMemberEntity(
                name = "Aarav",
                relation = "Son",
                notes = "Enjoys chess and robotics",
                routineSummary = "School pickup at 3:15 PM"
            )
        )

        // 6. Store 1 Persistent Intention (TEST DATA)
        db1.persistentIntentionDao().insertIntention(
            PersistentIntentionEntity(
                id = "VYB-INT-TEST-001",
                ownerId = stableOwnerId,
                rawRequest = "Wake me up tomorrow at 7:30 AM",
                normalizedIntent = "ALARM",
                scheduledTimestamp = System.currentTimeMillis() + 86400000L,
                recurrenceRule = "NONE",
                executionState = "SCHEDULED",
                notificationTitle = "Morning Wakeup Call",
                notificationBody = "Scheduled morning execution"
            )
        )

        // 7. Store 1 Fact that gets corrected (Phase G Preparation)
        db1.lifeVaultDao().insertVaultItem(
            LifeVaultItemEntity(
                category = "ME",
                key = "Office Departure Time",
                value = "9:00 AM", // User corrected from 8:45 AM
                source = "USER_PROVIDED",
                isConfirmed = true,
                updatedAt = System.currentTimeMillis() + 1000L
            )
        )

        // ==========================================
        // PHASE B: PERSIST SNAPSHOT TO DURABLE VAULT
        // ==========================================
        val snapshot = persistenceEngine.buildSnapshotFromDatabase(
            database = db1,
            profile = testProfile,
            ownerId = stableOwnerId,
            email = testOwnerEmail,
            phone = "+91 98765 43210"
        )
        val pushResult = persistenceEngine.saveAndSyncSnapshot(snapshot)
        assertTrue("Snapshot save must succeed", pushResult.success)
        assertTrue("Integrity checksum must validate", snapshot.validateIntegrity())

        // Verify local state counts before wipe
        assertEquals(4, db1.lifeVaultDao().getVaultFactCount())
        assertEquals(2, db1.memoryDao().getMemoryCountDirect())
        assertEquals(2, db1.routineDao().getAllRoutinesDirect().size)
        assertEquals(1, db1.goalDao().getAllGoalsDirect().size)
        assertEquals(1, db1.familyDao().getAllFamilyMembersDirect().size)
        assertEquals(1, db1.persistentIntentionDao().getAllIntentionsDirect().size)

        // ==========================================
        // PHASE C: SIMULATE UNINSTALL (COMPLETE LOCAL WIPE)
        // ==========================================
        db1.close()

        // Fresh database simulation representing reinstall
        val db2 = createDb()
        assertEquals("Reinstalled database must be completely empty", 0, db2.lifeVaultDao().getVaultFactCount())
        assertEquals("Reinstalled memories must be zero", 0, db2.memoryDao().getMemoryCountDirect())
        assertEquals("Reinstalled routines must be zero", 0, db2.routineDao().getAllRoutinesDirect().size)
        assertEquals("Reinstalled intentions must be zero", 0, db2.persistentIntentionDao().getAllIntentionsDirect().size)

        // ==========================================
        // PHASE D & E: AUTHENTICATE & FIRST-LAUNCH RESTORATION
        // ==========================================
        val startupCoordinator = VybStartupCoordinator(context, db2, persistenceEngine)
        var restoredProfile: VybPersonalProfile? = null

        val startupReport = startupCoordinator.executeStartupPipeline(
            onProfileRestored = { restoredProfile = it }
        )

        assertTrue("Startup coordinator must report ready", startupReport.isReady)
        assertEquals(StartupState.READY, startupReport.state)
        assertEquals("Personal Life Context active", startupReport.message)
        assertTrue("Restored facts count must match stored facts", startupReport.restoredFactsCount >= 4)

        // ==========================================
        // PHASE F: VERIFY RESTORED INTELLIGENCE STATE
        // ==========================================
        // 1. Facts survived
        val restoredVaultFacts = db2.lifeVaultDao().getAllVaultItems()
        assertEquals(4, restoredVaultFacts.size)
        assertTrue(restoredVaultFacts.any { it.key == "Full Name" && it.value == "Tanmoy Mukherjee" })
        assertTrue(restoredVaultFacts.any { it.key == "Company Name" && it.value == "Metropoli Fashions" })
        assertTrue(restoredVaultFacts.any { it.key == "Office Address" && it.value.contains("Sector V") })

        // 2. Correction survived (9:00 AM)
        val departureFact = restoredVaultFacts.find { it.key == "Office Departure Time" }
        assertNotNull(departureFact)
        assertEquals("9:00 AM", departureFact?.value)

        // 3. Memories survived
        val restoredMemories = db2.memoryDao().getAllMemoriesDirect()
        assertEquals(2, restoredMemories.size)
        assertTrue(restoredMemories.any { it.value.contains("Uber Premier") })
        assertTrue(restoredMemories.any { it.value.contains("140 grams") })

        // 4. Routines & Goals survived
        assertEquals(2, db2.routineDao().getAllRoutinesDirect().size)
        assertEquals(1, db2.goalDao().getAllGoalsDirect().size)

        // 5. Family context survived
        val familyList = db2.familyDao().getAllFamilyMembersDirect()
        assertEquals(1, familyList.size)
        assertEquals("Aarav", familyList.first().name)
        assertEquals("Son", familyList.first().relation)

        // 6. Persistent Intention survived
        val intentions = db2.persistentIntentionDao().getAllIntentionsDirect()
        assertEquals(1, intentions.size)
        assertEquals("VYB-INT-TEST-001", intentions.first().id)

        // 7. Profile restored
        assertNotNull(restoredProfile)
        assertEquals("Tanmoy Mukherjee", restoredProfile?.name)
        assertEquals("Metropoli Fashions", restoredProfile?.companyName)

        // ==========================================
        // PHASE G: QUERY RESTORED REASONING ENGINE
        // ==========================================
        val orchestrator = VybIntelligenceOrchestrator(
            memoryDao = db2.memoryDao(),
            taskDao = db2.taskDao(),
            lifeVaultDao = db2.lifeVaultDao(),
            expenseDao = db2.expenseDao(),
            shoppingDao = db2.shoppingDao(),
            routineDao = db2.routineDao(),
            goalDao = db2.goalDao(),
            projectDao = db2.projectDao(),
            familyDao = db2.familyDao()
        )

        val queryResult = orchestrator.processRequest(
            input = "What is my preferred office departure time?",
            profile = restoredProfile ?: testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = VybPermissionManager(context)
        )

        assertNotNull(queryResult)
        assertEquals(UniversalIntentType.MEMORY_QUERY, queryResult.universalIntent.intentType)
        assertTrue("Must retrieve 9:00 AM from restored database",
            queryResult.conversationalResponse.contains("9:00 AM", ignoreCase = true))
        assertFalse("Must NOT retrieve stale 8:45 AM",
            queryResult.conversationalResponse.contains("8:45 AM", ignoreCase = true))

        db2.close()
    }

    @Test
    fun `test schema integrity validation prevents corrupted state restoration`() = runBlocking {
        val state = DurablePersonalIntelligenceState(
            ownerId = stableOwnerId,
            stateVersion = 1,
            profile = DurableOwnerProfile(ownerId = stableOwnerId, name = "Valid Owner"),
            checksum = "invalid_hash_signature"
        )
        assertFalse("Corrupted checksum must fail validation", state.validateIntegrity())
    }

    @Test
    fun `acceptance test - blue tiger 742 test phrase survives uninstall and reinstall`() = runBlocking {
        val db1 = createDb()
        val persistenceEngine = VybCloudPersistenceEngine(context)

        // Store BLUE TIGER 742 test phrase
        db1.lifeVaultDao().insertVaultItem(
            LifeVaultItemEntity(
                category = "ME",
                key = "VYB persistence test phrase",
                value = "BLUE TIGER 742",
                source = "USER_PROVIDED",
                isConfirmed = true
            )
        )

        val snapshot = persistenceEngine.buildSnapshotFromDatabase(
            database = db1,
            profile = testProfile,
            ownerId = stableOwnerId,
            email = testOwnerEmail,
            phone = "+91 98765 43210"
        )
        val pushResult = persistenceEngine.saveAndSyncSnapshot(snapshot)
        assertTrue("Snapshot save must succeed", pushResult.success)
        assertTrue("Integrity checksum must validate", snapshot.validateIntegrity())

        db1.close()

        // Simulate reinstall with clean DB
        val db2 = createDb()
        assertEquals(0, db2.lifeVaultDao().getVaultFactCount())

        val startupCoordinator = VybStartupCoordinator(context, db2, persistenceEngine)
        val startupReport = startupCoordinator.executeStartupPipeline()
        assertTrue("Startup coordinator must be ready", startupReport.isReady)

        val restoredFacts = db2.lifeVaultDao().getAllVaultItems()
        val testPhraseFact = restoredFacts.find { it.key.contains("test phrase", ignoreCase = true) }
        assertNotNull("Restored test phrase fact must exist", testPhraseFact)
        assertEquals("BLUE TIGER 742", testPhraseFact?.value)

        // Query reasoning engine
        val orchestrator = VybIntelligenceOrchestrator(
            memoryDao = db2.memoryDao(),
            taskDao = db2.taskDao(),
            lifeVaultDao = db2.lifeVaultDao(),
            expenseDao = db2.expenseDao(),
            shoppingDao = db2.shoppingDao(),
            routineDao = db2.routineDao(),
            goalDao = db2.goalDao(),
            projectDao = db2.projectDao(),
            familyDao = db2.familyDao()
        )
        val queryResult = orchestrator.processRequest(
            input = "What is my VYB persistence test phrase?",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = VybPermissionManager(context)
        )

        assertNotNull(queryResult)
        assertTrue("Must retrieve BLUE TIGER 742 from restored durable state",
            queryResult.conversationalResponse.contains("BLUE TIGER 742", ignoreCase = true))

        db2.close()
    }

    @Test
    fun `acceptance test - real data correction update to 9-15 AM survives reinstall`() = runBlocking {
        val db1 = createDb()
        val persistenceEngine = VybCloudPersistenceEngine(context)

        // Initial fact: 9:00 AM
        val initialId = db1.lifeVaultDao().insertVaultItem(
            LifeVaultItemEntity(
                category = "ME",
                key = "Office Departure Time",
                value = "9:00 AM",
                source = "USER_PROVIDED",
                isConfirmed = true
            )
        )

        // Verify initial state
        assertEquals("9:00 AM", db1.lifeVaultDao().findVaultItemByKey("Office Departure Time")?.value)

        // Correction update: 9:15 AM
        db1.lifeVaultDao().updateVaultItem(
            LifeVaultItemEntity(
                id = initialId,
                category = "ME",
                key = "Office Departure Time",
                value = "9:15 AM",
                previousValue = "9:00 AM",
                source = "USER_CONFIRMED",
                isConfirmed = true,
                updatedAt = System.currentTimeMillis() + 5000L
            )
        )

        // Persist corrected state
        val snapshot = persistenceEngine.buildSnapshotFromDatabase(
            database = db1,
            profile = testProfile,
            ownerId = stableOwnerId,
            email = testOwnerEmail,
            phone = "+91 98765 43210"
        )
        persistenceEngine.saveAndSyncSnapshot(snapshot)

        db1.close()

        // Simulate reinstall (complete wipe)
        val db2 = createDb()
        val startupCoordinator = VybStartupCoordinator(context, db2, persistenceEngine)
        startupCoordinator.executeStartupPipeline()

        // Query reasoning engine
        val orchestrator = VybIntelligenceOrchestrator(
            memoryDao = db2.memoryDao(),
            taskDao = db2.taskDao(),
            lifeVaultDao = db2.lifeVaultDao(),
            expenseDao = db2.expenseDao(),
            shoppingDao = db2.shoppingDao(),
            routineDao = db2.routineDao(),
            goalDao = db2.goalDao(),
            projectDao = db2.projectDao(),
            familyDao = db2.familyDao()
        )
        val queryResult = orchestrator.processRequest(
            input = "What is my preferred office departure time?",
            profile = testProfile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE),
            appContext = context,
            permissionManager = VybPermissionManager(context)
        )

        assertNotNull(queryResult)
        assertTrue("Must retrieve corrected 9:15 AM",
            queryResult.conversationalResponse.contains("9:15 AM", ignoreCase = true))
        assertFalse("Must NOT retrieve old 9:00 AM",
            queryResult.conversationalResponse.contains("9:00 AM", ignoreCase = true))

        db2.close()
    }
}
