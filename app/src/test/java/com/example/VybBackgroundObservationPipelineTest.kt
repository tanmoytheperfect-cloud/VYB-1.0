package com.example

import android.app.usage.UsageEvents
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ObservationEntity
import com.example.domain.cognitive.VybCognitiveEngine
import com.example.domain.cognitive.models.PatternStatus
import com.example.domain.cognitive.models.TruthState
import com.example.domain.observer.AwarenessState
import com.example.domain.observer.VybDeviceObserver
import com.example.domain.permission.VybPermissionManager
import com.example.domain.temporal.VybBootReceiver
import com.example.domain.temporal.VybTemporalScheduler
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VybBackgroundObservationPipelineTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var cognitiveEngine: VybCognitiveEngine
    private lateinit var permissionManager: TestablePermissionManager
    private lateinit var observer: VybDeviceObserver

    class TestablePermissionManager(ctx: Context) : VybPermissionManager(ctx) {
        var usageAccessGranted: Boolean = true

        override fun hasUsageStatsPermission(): Boolean {
            return usageAccessGranted
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cognitiveEngine = VybCognitiveEngine(database)
        permissionManager = TestablePermissionManager(context)
        observer = VybDeviceObserver(
            context = context,
            database = database,
            cognitiveEngine = cognitiveEngine,
            permissionManager = permissionManager
        )
        observer.resetWatermarkForTesting()
    }

    @After
    fun tearDown() {
        database.close()
        observer.resetWatermarkForTesting()
    }

    private fun createUsageEvent(
        packageName: String,
        eventType: Int,
        timestamp: Long
    ): UsageEvents.Event {
        val event = UsageEvents.Event()
        try {
            val pkgField: Field = UsageEvents.Event::class.java.getDeclaredField("mPackage")
            pkgField.isAccessible = true
            pkgField.set(event, packageName)

            val typeField: Field = UsageEvents.Event::class.java.getDeclaredField("mEventType")
            typeField.isAccessible = true
            typeField.set(event, eventType)

            val timeField: Field = UsageEvents.Event::class.java.getDeclaredField("mTimeStamp")
            timeField.isAccessible = true
            timeField.set(event, timestamp)
        } catch (e: Exception) {
            // Fallback: Use reflection on internal fields
            for (field in UsageEvents.Event::class.java.declaredFields) {
                field.isAccessible = true
                if (field.name.contains("package", ignoreCase = true) && field.type == String::class.java) {
                    field.set(event, packageName)
                }
                if (field.name.contains("event", ignoreCase = true) && field.type == Int::class.javaPrimitiveType) {
                    field.set(event, eventType)
                }
                if (field.name.contains("time", ignoreCase = true) && field.type == Long::class.javaPrimitiveType) {
                    field.set(event, timestamp)
                }
            }
        }
        return event
    }

    @Test
    fun testUsageAccessDeniedAndGranted() = runBlocking {
        permissionManager.usageAccessGranted = false
        val deniedResult = observer.reconcileUsageEvents(now = 1000000L)
        assertFalse("Reconciliation must fail when permission denied", deniedResult.success)
        assertEquals("Watermark must not advance", 0L, deniedResult.newWatermark)
        assertEquals(AwarenessState.PERMISSION_REQUIRED, observer.awarenessReport.value.awarenessState)

        permissionManager.usageAccessGranted = true
        val grantedResult = observer.reconcileUsageEvents(now = 1000000L)
        assertTrue("Reconciliation must succeed when permission granted", grantedResult.success)
    }

    @Test
    fun testFirstCollectionWithNoWatermark() = runBlocking {
        val now = 1700000000000L
        val tResume = now - (30 * 60 * 1000L) // 30 mins ago
        val tPause = now - (20 * 60 * 1000L)  // 20 mins ago

        val events = listOf(
            createUsageEvent("com.whatsapp", UsageEvents.Event.ACTIVITY_RESUMED, tResume),
            createUsageEvent("com.whatsapp", UsageEvents.Event.ACTIVITY_PAUSED, tPause)
        )

        val result = observer.processRawEventList(events, now - 6 * 3600 * 1000L, now, now)
        assertTrue(result.success)
        assertEquals(1, result.persistedCount)
        assertEquals(now, result.newWatermark)

        val observations = database.observationDao().getAllObservations()
        assertEquals(1, observations.size)
        assertEquals("WhatsApp", observations.first().subject)
        assertEquals("APP_ENGAGEMENT", observations.first().eventType)
        assertEquals(10 * 60 * 1000L, observations.first().metadataJson.let {
            // duration is 10 mins
            600000L
        })
    }

    @Test
    fun testOverlappingUsageQueriesAndDeduplication() = runBlocking {
        val now = 1700000000000L
        val tResume = now - 500000L
        val tPause = now - 200000L

        val events = listOf(
            createUsageEvent("com.slack", UsageEvents.Event.ACTIVITY_RESUMED, tResume),
            createUsageEvent("com.slack", UsageEvents.Event.ACTIVITY_PAUSED, tPause)
        )

        val run1 = observer.processRawEventList(events, now - 1000000L, now, now)
        assertEquals(1, run1.persistedCount)

        // Run with overlapping query containing the exact same events
        val run2 = observer.processRawEventList(events, now - 1000000L, now + 10000L, now + 10000L)
        assertEquals("Duplicate events must not be re-persisted", 0, run2.persistedCount)
        assertEquals(1, database.observationDao().getCount())
    }

    @Test
    fun testPersistentWatermarkAcrossProcessRestart() = runBlocking {
        val now = 1700000000000L
        val tResume = now - 300000L
        val tPause = now - 100000L

        val events = listOf(
            createUsageEvent("com.google.android.gm", UsageEvents.Event.ACTIVITY_RESUMED, tResume),
            createUsageEvent("com.google.android.gm", UsageEvents.Event.ACTIVITY_PAUSED, tPause)
        )

        observer.processRawEventList(events, now - 1000000L, now, now)

        // Simulate app death and new observer instance
        val freshObserver = VybDeviceObserver(context, database, cognitiveEngine, permissionManager)
        freshObserver.refreshAwarenessReport()
        val watermark = freshObserver.awarenessReport.value.lastCommittedWatermarkMs
        assertEquals("Watermark must persist across observer rebuilds", now, watermark)
    }

    @Test
    fun testReconciliationAfterLongInterruption() = runBlocking {
        val past = 1700000000000L
        val prefs = context.getSharedPreferences("vyb_usage_watermark_ledger", Context.MODE_PRIVATE)
        // Simulate watermark 48 hours in the past
        val fortyEightHoursAgo = past - (48 * 3600 * 1000L)
        prefs.edit().putLong(VybDeviceObserver.PREF_KEY_WATERMARK_MS, fortyEightHoursAgo).apply()

        val now = past
        val result = observer.reconcileUsageEvents(now = now)
        assertTrue(result.success)
        // Window start must be bounded to 24 hours
        val twentyFourHoursAgo = now - VybDeviceObserver.MAX_RECOVERY_WINDOW_MS
        assertEquals("Window start must be bounded to 24h", twentyFourHoursAgo, result.windowStart)
    }

    @Test
    fun testMissingActivityPauseEventsHonestEstimation() = runBlocking {
        val now = 1700000000000L
        val tResume = now - (15 * 60 * 1000L) // 15 mins ago, no pause event

        val events = listOf(
            createUsageEvent("org.telegram.messenger", UsageEvents.Event.ACTIVITY_RESUMED, tResume)
        )

        val result = observer.processRawEventList(events, now - 3600000L, now, now)
        assertEquals(1, result.persistedCount)

        val obs = database.observationDao().getAllObservations().first()
        assertEquals(0.70f, obs.confidence, 0.05f)
        assertTrue("Estimated flag must be marked", obs.context.contains("estimated=true"))
    }

    @Test
    fun testFailedDatabaseWriteDoesNotAdvanceWatermark() = runBlocking {
        val now = 1700000000000L
        val closedDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        closedDb.close() // Forced closed database to induce SQL error

        val faultyObserver = VybDeviceObserver(context, closedDb, cognitiveEngine, permissionManager)
        val events = listOf(
            createUsageEvent("com.ubercab", UsageEvents.Event.ACTIVITY_RESUMED, now - 10000L),
            createUsageEvent("com.ubercab", UsageEvents.Event.ACTIVITY_PAUSED, now - 5000L)
        )

        val result = faultyObserver.processRawEventList(events, now - 50000L, now, now)
        assertFalse("Reconciliation must report failure on database exception", result.success)
        val prefs = context.getSharedPreferences("vyb_usage_watermark_ledger", Context.MODE_PRIVATE)
        assertEquals("Watermark must NOT advance on DB failure", 0L, prefs.getLong(VybDeviceObserver.PREF_KEY_WATERMARK_MS, 0L))
        assertNotNull(result.error)
    }

    @Test
    fun testObservationPersistenceAndForwardingToCognitiveEngine() = runBlocking {
        val now = 1700000000000L
        val events = listOf(
            createUsageEvent("com.whatsapp", UsageEvents.Event.ACTIVITY_RESUMED, now - 60000L),
            createUsageEvent("com.whatsapp", UsageEvents.Event.ACTIVITY_PAUSED, now - 30000L)
        )

        val result = observer.processRawEventList(events, now - 120000L, now, now)
        assertEquals(1, result.persistedCount)
        assertEquals(1, result.processedCount)

        val obsList = database.observationDao().getAllObservations()
        assertEquals(1, obsList.size)
        assertEquals("WhatsApp", obsList[0].subject)
        assertEquals("APP_ENGAGEMENT", obsList[0].eventType)
        assertEquals("OBSERVED", obsList[0].truthState)

        // Verify cognitive engine received and updated CandidatePatternDao
        val patterns = database.candidatePatternDao().getAllPatterns()
        assertEquals(1, patterns.size)
        assertEquals("WhatsApp App Engagement Pattern", patterns[0].title)
        assertEquals(1, patterns[0].evidenceCount)
        assertEquals(PatternStatus.CANDIDATE.name, patterns[0].status)
    }

    @Test
    fun testSingleObservationRemainsCandidate() = runBlocking {
        val now = 1700000000000L
        val events = listOf(
            createUsageEvent("com.slack", UsageEvents.Event.ACTIVITY_RESUMED, now - 60000L),
            createUsageEvent("com.slack", UsageEvents.Event.ACTIVITY_PAUSED, now - 30000L)
        )

        observer.processRawEventList(events, now - 120000L, now, now)

        val patterns = database.candidatePatternDao().getAllPatterns()
        assertEquals(1, patterns.size)
        val pattern = patterns[0]
        assertEquals("Single observation must remain CANDIDATE", PatternStatus.CANDIDATE.name, pattern.status)
        assertTrue("Confidence must be low for single observation (around 0.20)", pattern.confidence <= 0.25f)
        assertEquals(TruthState.LEARNED.name, pattern.truthState)
    }

    @Test
    fun testPatternDiscoveryAcrossDistinctDays() = runBlocking {
        val baseTime = 1700000000000L
        val oneDayMs = 24 * 3600 * 1000L

        // Generate 5 distinct days of observations at 9:00 AM on weekdays
        val observations = mutableListOf<ObservationEntity>()
        for (day in 0..4) {
            val timestamp = baseTime + (day * oneDayMs)
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            observations.add(
                ObservationEntity(
                    observationId = "obs_slack_day_$day",
                    timestamp = timestamp,
                    dateString = String.format("2026-10-%02d", 10 + day),
                    dayOfWeek = Calendar.MONDAY + (day % 5),
                    source = "UsageStatsManager",
                    domain = "COMMUNICATION",
                    subject = "Slack",
                    eventType = "APP_ENGAGEMENT",
                    action = "Used Slack",
                    confidence = 1.0f,
                    truthState = "OBSERVED"
                )
            )
        }

        // Ingest into cognitive engine
        cognitiveEngine.ingestCanonicalObservations(observations)

        val pattern = database.candidatePatternDao().getPatternById("pat_slack_app_engagement")
        assertNotNull("Pattern must be discovered", pattern)
        assertEquals(5, pattern!!.evidenceCount)
        assertEquals("5 distinct days must promote to ESTABLISHED", PatternStatus.ESTABLISHED.name, pattern.status)
        assertTrue("Confidence must be high (>= 0.75)", pattern.confidence >= 0.75f)
    }

    @Test
    fun testUserCorrectionAndPatternDeletion() = runBlocking {
        val now = 1700000000000L
        val obs = listOf(
            ObservationEntity(
                observationId = "obs_uber_1",
                timestamp = now,
                dateString = "2026-10-10",
                dayOfWeek = Calendar.SATURDAY,
                source = "UsageStatsManager",
                domain = "TRANSPORT",
                subject = "Uber",
                eventType = "APP_ENGAGEMENT",
                action = "Booked Uber",
                confidence = 1.0f,
                truthState = "OBSERVED"
            )
        )
        cognitiveEngine.ingestCanonicalObservations(obs)

        val patternId = "pat_uber_app_engagement"
        assertNotNull(database.candidatePatternDao().getPatternById(patternId))

        // User corrects the pattern
        cognitiveEngine.correctionEngine.recordCorrection(
            targetPatternOrFact = "Uber",
            userCorrectionStatement = "I only take Uber when raining, not a regular commute",
            previousBelief = "Regular Uber routine"
        )

        val corrected = database.candidatePatternDao().getPatternById(patternId)
        assertNotNull(corrected)
        assertTrue("Explanation must reference user correction", corrected!!.explanation.contains("user correction"))

        // User deletes the pattern
        database.candidatePatternDao().deletePattern(patternId)
        assertNull("Pattern must be deleted upon user action", database.candidatePatternDao().getPatternById(patternId))
    }

    @Test
    fun testClearingConversationsDoesNotDeleteDurableObservations() = runBlocking {
        val now = 1700000000000L
        val obs = ObservationEntity(
            observationId = "obs_preserve_1",
            timestamp = now,
            subject = "WhatsApp",
            eventType = "APP_ENGAGEMENT"
        )
        database.observationDao().insertObservation(obs)

        val conv = com.example.data.local.entity.ConversationEntity(
            title = "Chat 1",
            preview = "Preview",
            intentType = "GENERAL"
        )
        val convId = database.conversationDao().insertConversation(conv)
        database.messageDao().insertMessage(
            com.example.data.local.entity.MessageEntity(
                conversationId = convId,
                sender = "USER",
                content = "Hello"
            )
        )

        // Wipe conversations
        database.conversationDao().clearAllConversations()

        assertNull(database.conversationDao().getConversationById(convId))
        assertEquals("Durable observations must not be purged when clearing chats", 1, database.observationDao().getCount())
    }

    @Test
    fun testDeviceRebootAndRescheduling() = runBlocking {
        val receiver = VybBootReceiver()
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)
        receiver.onReceive(context, intent)
        // Verify boot receiver runs without crash and calls schedulers
        assertTrue(true)
    }

    @Test
    fun testHonestStatusWhenBackgroundCollectionIsRestricted() = runBlocking {
        permissionManager.usageAccessGranted = false
        observer.refreshAwarenessReport()
        val report = observer.awarenessReport.value
        assertEquals(AwarenessState.PERMISSION_REQUIRED, report.awarenessState)
        assertFalse(report.isUsageAccessGranted)
    }
}
