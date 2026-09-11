package com.example

import com.example.data.local.dao.DailyReflectionDao
import com.example.data.local.dao.LearnedRoutineDao
import com.example.data.local.dao.TimelineDao
import com.example.data.local.entity.DailyReflectionEntity
import com.example.data.local.entity.LearnedRoutineEntity
import com.example.data.local.entity.TimelineEventEntity
import com.example.domain.learning.DailyReflectionEngine
import com.example.domain.learning.RoutineLearningEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

class MilestoneThreeLearningTest {

    private class FakeTimelineDao : TimelineDao {
        val events = mutableListOf<TimelineEventEntity>()
        private var nextId = 1L

        override suspend fun insertEvent(event: TimelineEventEntity): Long {
            val withId = event.copy(id = nextId++)
            events.add(withId)
            return withId.id
        }

        override suspend fun insertEvents(events: List<TimelineEventEntity>) {
            events.forEach { insertEvent(it) }
        }

        override fun getRecentEventsFlow(limit: Int): Flow<List<TimelineEventEntity>> =
            MutableStateFlow(events.sortedByDescending { it.timestamp }.take(limit))

        override fun getEventsByDateFlow(dateString: String): Flow<List<TimelineEventEntity>> =
            MutableStateFlow(events.filter { it.dateString == dateString }.sortedBy { it.timestamp })

        override suspend fun getEventsByDate(dateString: String): List<TimelineEventEntity> {
            return events.filter { it.dateString == dateString }.sortedBy { it.timestamp }
        }

        override suspend fun getEventsSince(sinceTimestamp: Long): List<TimelineEventEntity> {
            return events.filter { it.timestamp >= sinceTimestamp }.sortedBy { it.timestamp }
        }

        override suspend fun getEventsByType(type: String, limit: Int): List<TimelineEventEntity> {
            return events.filter { it.eventType == type }.sortedByDescending { it.timestamp }.take(limit)
        }

        override suspend fun getEventCountForDate(dateString: String): Int =
            events.count { it.dateString == dateString }

        override suspend fun clearEventsForDate(dateString: String) {
            events.removeAll { it.dateString == dateString }
        }

        override suspend fun clearAllEvents() {
            events.clear()
        }

        suspend fun deleteAllEvents() {
            clearAllEvents()
        }
    }

    private class FakeLearnedRoutineDao : LearnedRoutineDao {
        val routines = mutableMapOf<String, LearnedRoutineEntity>()

        override suspend fun insertOrUpdate(routine: LearnedRoutineEntity) {
            routines[routine.routineId] = routine
        }

        override suspend fun insertAll(routines: List<LearnedRoutineEntity>) {
            routines.forEach { this.routines[it.routineId] = it }
        }

        override fun getAllLearnedRoutinesFlow(): Flow<List<LearnedRoutineEntity>> =
            MutableStateFlow(routines.values.toList())

        override fun getActiveRoutinesFlow(): Flow<List<LearnedRoutineEntity>> =
            MutableStateFlow(routines.values.filter { it.isActive })

        override suspend fun getVerifiedLearnedRoutines(): List<LearnedRoutineEntity> {
            return routines.values.filter { it.status == "LEARNED_ROUTINE" && it.isActive }
        }

        override suspend fun getRoutineById(id: String): LearnedRoutineEntity? = routines[id]

        override suspend fun getRoutinesForDay(day: String): List<LearnedRoutineEntity> {
            return routines.values.filter {
                it.daysOfWeek.contains(day) || it.daysOfWeek == "WEEKDAY" || it.daysOfWeek == "WEEKEND" || it.daysOfWeek == "ANY"
            }
        }

        override suspend fun deleteRoutine(id: String) {
            routines.remove(id)
        }

        override suspend fun clearAllLearnedRoutines() {
            routines.clear()
        }
    }

    private class FakeDailyReflectionDao : DailyReflectionDao {
        val reflections = mutableListOf<DailyReflectionEntity>()

        override suspend fun insertReflection(reflection: DailyReflectionEntity) {
            reflections.add(reflection)
        }

        override fun getAllReflectionsFlow(): Flow<List<DailyReflectionEntity>> =
            MutableStateFlow(reflections.sortedByDescending { it.timestamp })

        override suspend fun getReflectionByDate(dateString: String): DailyReflectionEntity? {
            return reflections.find { it.dateString == dateString }
        }

        override suspend fun getLatestReflection(): DailyReflectionEntity? {
            return reflections.maxByOrNull { it.timestamp }
        }

        override suspend fun getRecentReflections(limit: Int): List<DailyReflectionEntity> {
            return reflections.sortedByDescending { it.timestamp }.take(limit)
        }

        override suspend fun clearAllReflections() {
            reflections.clear()
        }
    }

    private lateinit var timelineDao: FakeTimelineDao
    private lateinit var learnedRoutineDao: FakeLearnedRoutineDao
    private lateinit var dailyReflectionDao: FakeDailyReflectionDao
    private lateinit var routineLearningEngine: RoutineLearningEngine
    private lateinit var dailyReflectionEngine: DailyReflectionEngine

    @Before
    fun setup() {
        timelineDao = FakeTimelineDao()
        learnedRoutineDao = FakeLearnedRoutineDao()
        dailyReflectionDao = FakeDailyReflectionDao()
        routineLearningEngine = RoutineLearningEngine(timelineDao, learnedRoutineDao)
        dailyReflectionEngine = DailyReflectionEngine(timelineDao, dailyReflectionDao, routineLearningEngine)
    }

    @Test
    fun test1_freshDatabaseHasNoRoutines() = runBlocking {
        val routines = routineLearningEngine.analyzeAndLearnRoutines()
        assertTrue("Fresh database must have 0 learned routines", routines.isEmpty())
    }

    @Test
    fun test2_singleObservationDoesNotCreateLearnedRoutine() = runBlocking {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 15)
        }

        timelineDao.insertEvent(
            TimelineEventEntity(
                timestamp = cal.timeInMillis,
                eventType = "DEVICE_UNLOCK",
                source = "SYSTEM_RECEIVER",
                contextData = "Screen turned on and unlocked",
                dayOfWeek = Calendar.MONDAY,
                dateString = "2026-08-03"
            )
        )

        val routines = routineLearningEngine.analyzeAndLearnRoutines()
        assertEquals(1, routines.size)
        val routine = routines.first()

        assertEquals("learned_morning_unlock_weekday", routine.routineId)
        assertEquals("OBSERVATION", routine.status)
        assertEquals(1, routine.evidenceCount)
        assertEquals(1, routine.distinctObservationDays)
        assertTrue("Confidence for single observation must be <= 0.35", routine.confidenceScore <= 0.35f)
        assertEquals("DEVICE_OBSERVATION", routine.source)
        assertFalse(routine.description.contains("wake up", ignoreCase = true))
    }

    @Test
    fun test3_repeatedObservationsAcrossMultipleDaysIncreasesEvidence() = runBlocking {
        // Log morning unlocks across 6 distinct weekdays around 07:15 - 07:25
        val baseCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 20)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var addedDays = 0
        var offsetDays = 1
        while (addedDays < 6) {
            val cal = (baseCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -offsetDays)
            }
            offsetDays++
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY) {
                cal.add(Calendar.MINUTE, addedDays)
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(cal.timeInMillis))
                timelineDao.insertEvent(
                    TimelineEventEntity(
                        timestamp = cal.timeInMillis,
                        eventType = "DEVICE_UNLOCK",
                        source = "SYSTEM_RECEIVER",
                        contextData = "Device unlocked",
                        dayOfWeek = dayOfWeek,
                        dateString = dateStr
                    )
                )
                addedDays++
            }
        }

        val routines = routineLearningEngine.analyzeAndLearnRoutines()
        assertEquals(1, routines.size)
        val routine = routines.first()

        assertEquals(6, routine.evidenceCount)
        assertEquals(6, routine.distinctObservationDays)
        assertEquals("CANDIDATE_ROUTINE", routine.status)
        assertTrue("Confidence for 6 days should be between 0.66 and 0.84", routine.confidenceScore in 0.66f..0.84f)
    }

    @Test
    fun test4_weekdayAndWeekendModelsRemainStrictlySeparate() = runBlocking {
        // Add Monday unlocks
        for (i in 0 until 3) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 10)
            }
            timelineDao.insertEvent(
                TimelineEventEntity(
                    timestamp = cal.timeInMillis - (i * 7 * 24 * 60 * 60 * 1000L),
                    eventType = "DEVICE_UNLOCK",
                    source = "SYSTEM_RECEIVER",
                    contextData = "Unlock",
                    dayOfWeek = Calendar.MONDAY,
                    dateString = "2026-08-0$i"
                )
            )
        }

        // Add Saturday unlocks at 09:30 AM
        for (i in 0 until 4) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 30)
            }
            timelineDao.insertEvent(
                TimelineEventEntity(
                    timestamp = cal.timeInMillis - (i * 7 * 24 * 60 * 60 * 1000L),
                    eventType = "DEVICE_UNLOCK",
                    source = "SYSTEM_RECEIVER",
                    contextData = "Unlock",
                    dayOfWeek = Calendar.SATURDAY,
                    dateString = "2026-08-1$i"
                )
            )
        }

        val routines = routineLearningEngine.analyzeAndLearnRoutines()
        assertEquals(2, routines.size)

        val weekdayRoutine = routines.find { it.daysOfWeek == "WEEKDAY" }
        val weekendRoutine = routines.find { it.daysOfWeek == "WEEKEND" }

        assertNotNull("Weekday routine must exist", weekdayRoutine)
        assertNotNull("Weekend routine must exist", weekendRoutine)

        assertEquals("learned_morning_unlock_weekday", weekdayRoutine?.routineId)
        assertEquals(3, weekdayRoutine?.evidenceCount)

        assertEquals("learned_morning_unlock_weekend", weekendRoutine?.routineId)
        assertEquals(4, weekendRoutine?.evidenceCount)
        assertTrue(weekendRoutine?.expectedTimeWindow?.contains("09:") == true || weekendRoutine?.description?.contains("09:") == true)
    }

    @Test
    fun test5_highEvidenceConsistencyGraduatesToLearnedRoutine() = runBlocking {
        // 14 distinct days of consistent 07:15 AM unlocks
        for (day in 1..14) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
                set(Calendar.HOUR_OF_DAY, 7)
                set(Calendar.MINUTE, 15)
            }
            timelineDao.insertEvent(
                TimelineEventEntity(
                    timestamp = cal.timeInMillis - (day * 24 * 60 * 60 * 1000L),
                    eventType = "DEVICE_UNLOCK",
                    source = "SYSTEM_RECEIVER",
                    contextData = "Unlock",
                    dayOfWeek = Calendar.TUESDAY,
                    dateString = "2026-07-$day"
                )
            )
        }

        val routines = routineLearningEngine.analyzeAndLearnRoutines()
        val routine = routines.first()

        assertEquals("LEARNED_ROUTINE", routine.status)
        assertTrue("Confidence for 14 consistent days must be >= 0.85", routine.confidenceScore >= 0.85f)
        assertEquals(14, routine.distinctObservationDays)
    }

    @Test
    fun test6_dailyReflectionGeneratesFactualCountsWithoutHallucination() = runBlocking {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cal = Calendar.getInstance()

        // Insert 3 unlock events and 2 app events today
        timelineDao.insertEvent(
            TimelineEventEntity(
                timestamp = cal.timeInMillis,
                eventType = "DEVICE_UNLOCK",
                source = "SYSTEM_RECEIVER",
                contextData = "Screen unlocked",
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                dateString = todayStr
            )
        )
        timelineDao.insertEvent(
            TimelineEventEntity(
                timestamp = cal.timeInMillis + 5000,
                eventType = "APP_OPENED",
                source = "USAGE_STATS_MANAGER",
                contextData = "Chrome (Browser)",
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                dateString = todayStr
            )
        )

        val reflection = dailyReflectionEngine.performDailyReflection(todayStr)
        assertNotNull(reflection)
        assertEquals(todayStr, reflection.dateString)
        assertTrue(reflection.reflectionSummaryText.contains("2 device events observed"))
        assertTrue(reflection.reflectionSummaryText.contains("Chrome"))
        assertFalse(reflection.reflectionSummaryText.contains("busy morning", ignoreCase = true))
        assertFalse(reflection.reflectionSummaryText.contains("woke up", ignoreCase = true))
    }

    @Test
    fun test7_deletingObservationsRecalculatesRoutines() = runBlocking {
        timelineDao.insertEvent(
            TimelineEventEntity(
                timestamp = System.currentTimeMillis(),
                eventType = "DEVICE_UNLOCK",
                source = "SYSTEM_RECEIVER",
                contextData = "Unlock",
                dayOfWeek = Calendar.WEDNESDAY,
                dateString = "2026-08-28"
            )
        )

        val initialRoutines = routineLearningEngine.analyzeAndLearnRoutines()
        assertEquals(1, initialRoutines.size)

        // Clear all timeline events
        timelineDao.deleteAllEvents()
        val emptyRoutines = routineLearningEngine.analyzeAndLearnRoutines()
        assertTrue("After timeline deletion, analyzed routines must be empty", emptyRoutines.isEmpty())
    }
}
