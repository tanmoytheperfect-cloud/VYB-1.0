package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.LifeVaultItemEntity
import com.example.data.local.entity.TimelineEventEntity
import com.example.domain.brain.PersonalWorldModel
import com.example.domain.learning.RoutineLearningEngine
import com.example.domain.vault.PersonalLifeVaultEngine
import com.example.domain.vault.VaultCategory
import com.example.domain.vault.VaultOperationResult
import com.example.domain.vault.VaultSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MilestoneOneVerificationTest {

    private lateinit var db: AppDatabase
    private lateinit var routineEngine: RoutineLearningEngine
    private lateinit var vaultEngine: PersonalLifeVaultEngine

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        routineEngine = RoutineLearningEngine(db.timelineDao(), db.learnedRoutineDao())
        vaultEngine = PersonalLifeVaultEngine(db.lifeVaultDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `test 1 fresh installation starts with zero fake routines and zero vault facts`() = runBlocking {
        assertEquals(0, db.learnedRoutineDao().getVerifiedLearnedRoutines().size)
        assertEquals(0, db.timelineDao().getEventsSince(0L).size)
        assertEquals(0, db.lifeVaultDao().getAllVaultItems().size)
        assertEquals(0, db.familyDao().getAllFamilyMembers().first().size)
        assertEquals(0, db.expenseDao().getAllExpenses().first().size)
    }

    @Test
    fun `test 2 world model contains zero hardcoded personal figures`() {
        val defaultModel = PersonalWorldModel.createDefault()
        assertEquals(0.0, defaultModel.finance.monthlyIncome, 0.001)
        assertEquals(0.0, defaultModel.finance.fixedCommitments, 0.001)
        assertEquals(0.0, defaultModel.finance.monthlySavingsTarget, 0.001)
        assertEquals(0.0, defaultModel.finance.activeDiscretionaryBuffer, 0.001)
        assertTrue(defaultModel.family.members.isEmpty())
        assertTrue(defaultModel.routines.isEmpty())
    }

    @Test
    fun `test 3 life vault handles user provided facts and generates honest gemini context`() = runBlocking {
        // Add facts to vault
        db.lifeVaultDao().insertVaultItem(
            LifeVaultItemEntity(
                category = VaultCategory.ME,
                key = "name",
                value = "Aarav",
                source = VaultSource.USER_PROVIDED
            )
        )
        db.lifeVaultDao().insertVaultItem(
            LifeVaultItemEntity(
                category = VaultCategory.PLACES,
                key = "office address",
                value = "Salt Lake Sector V, Kolkata",
                source = VaultSource.USER_PROVIDED
            )
        )

        val items = db.lifeVaultDao().getAllVaultItems()
        assertEquals(2, items.size)

        // Verify Gemini context includes verified facts and explicitly labels unconfigured fields as UNKNOWN
        val contextPrompt = vaultEngine.buildGeminiContextPrompt()
        assertTrue(contextPrompt.contains("Aarav"))
        assertTrue(contextPrompt.contains("Salt Lake Sector V"))
        assertTrue(contextPrompt.contains("UNKNOWN") || contextPrompt.contains("UNCONFIGURED"))
    }

    @Test
    fun `test 4 life vault conflict detection and resolution`() = runBlocking {
        // Initial fact
        db.lifeVaultDao().insertVaultItem(
            LifeVaultItemEntity(
                category = VaultCategory.PLACES,
                key = "office",
                value = "Sector V",
                source = VaultSource.USER_PROVIDED
            )
        )

        // Process natural language change: "My office is now New Town Action Area II"
        val result = vaultEngine.processConversationalMemory("My office is now New Town Action Area II")
        assertTrue(result is VaultOperationResult.ConflictDetected)

        val conflict = result as VaultOperationResult.ConflictDetected
        assertEquals("Sector V", conflict.existingItem.value)
        assertEquals("New Town Action Area II", conflict.proposedValue)

        // Pending conflicts count in database should be 1
        val pending = db.lifeVaultDao().getPendingConflicts()
        assertEquals(1, pending.size)

        // Resolve conflict by accepting new
        val resolved = vaultEngine.resolveConflict(conflict.existingItem.id, acceptNew = true)
        assertNotNull(resolved)
        assertEquals("New Town Action Area II", resolved?.value)
        assertEquals("Sector V", resolved?.previousValue)
        assertEquals(VaultSource.USER_CONFIRMED, resolved?.source)
        assertFalse(resolved?.isPendingConflict ?: true)
    }

    @Test
    fun `test 5 real android timeline events are logged and analyzed honestly`() = runBlocking {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        cal.set(Calendar.HOUR_OF_DAY, 7)
        cal.set(Calendar.MINUTE, 30)
        val timestamp1 = cal.timeInMillis

        val eventUnlock = TimelineEventEntity(
            timestamp = timestamp1,
            eventType = "DEVICE_UNLOCK",
            source = "SYSTEM_RECEIVER",
            contextData = "Device screen unlocked",
            dayOfWeek = Calendar.TUESDAY,
            dateString = "2026-08-28"
        )

        db.timelineDao().insertEvent(eventUnlock)

        val recorded = db.timelineDao().getEventsSince(0L)
        assertEquals(1, recorded.size)

        // Analyze timeline
        val routines = routineEngine.analyzeAndLearnRoutines()
        assertEquals(1, routines.size)
        val morningRoutine = routines.first()
        assertEquals("learned_morning_unlock_weekday", morningRoutine.routineId)
        assertEquals("OBSERVATION", morningRoutine.status)
        assertEquals(1, morningRoutine.evidenceCount)
        assertTrue(morningRoutine.confidenceScore <= 0.35f)
    }
}
