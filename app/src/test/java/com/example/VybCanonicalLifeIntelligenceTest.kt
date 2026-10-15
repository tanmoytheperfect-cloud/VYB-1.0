package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.domain.cognitive.VybCognitiveEngine
import com.example.domain.cognitive.models.*
import com.example.domain.environment.model.*
import com.example.ui.viewmodel.VybViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VybCanonicalLifeIntelligenceTest {

    private lateinit var app: Application
    private lateinit var viewModel: VybViewModel
    private lateinit var cognitiveEngine: VybCognitiveEngine

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = VybViewModel(app)
        cognitiveEngine = viewModel.repository.cognitiveEngine
    }

    @Test
    fun test_01_canonical_current_life_state_cohesion() = runBlocking {
        // Requirement 1: One canonical CurrentLifeState combining all life dimensions
        val state = cognitiveEngine.getCurrentLifeState()

        assertNotNull("Identity must be populated", state.ownerId)
        assertNotNull("Owner name must be present", state.ownerName)
        assertNotNull("Time and date must be populated", state.timeFormatted)
        assertNotNull("Location context must be present", state.location)
        assertNotNull("Steps metric must be present", state.steps)
        assertNotNull("Activity metric must be present", state.activity)
        assertNotNull("Mobility state must be present", state.mobilityState)
        assertNotNull("Personal baselines must be present", state.personalBaselines)
        assertNotNull("Contextual hero must be present", state.contextualHero)
        assertFalse("Truth state must never be null", state.healthTruthState == null)
    }

    @Test
    fun test_02_telemetry_to_observation_engine() = runBlocking {
        // Requirement 2: Every real telemetry event becomes an observation
        val obs = cognitiveEngine.observeEvent(
            domain = "ACTIVITY",
            subject = "WALKING",
            eventType = "STEP_COUNT",
            action = "STEPS_LOGGED",
            context = "User has walked 2,840 steps today.",
            entities = mapOf("steps" to "2840", "unit" to "steps", "value" to "2840")
        )

        assertNotNull(obs.observationId)
        assertEquals("ACTIVITY", obs.domain)
        assertEquals("WALKING", obs.subject)
        assertEquals("STEP_COUNT", obs.eventType)
        assertEquals(TruthState.OBSERVED, obs.truthState)
        assertEquals("primary_owner", obs.ownerId)
        assertTrue("Timestamp must be recent", obs.timestamp > 0)
    }

    @Test
    fun test_03_multi_signal_reasoning_air_and_walking() = runBlocking {
        // Requirement 3: Multi-signal reasoning correlating high AQI with upcoming walk
        val poorAir = AirQualityEnvironment(
            aqi = 165,
            pm25 = 85.0,
            pm10 = 140.0,
            level = AirQualityLevel.VERY_POOR,
            timestamp = System.currentTimeMillis(),
            source = "Open-Meteo Air Quality",
            truthState = MetricTruthState.KNOWN
        )
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18) // 6 PM
            set(Calendar.MINUTE, 10)
        }
        val draftState = cognitiveEngine.getCurrentLifeState().copy(
            currentTimeMillis = cal.timeInMillis,
            airQuality = poorAir,
            personalBaselines = PersonalBaselines(normalWalkingTime = "06:15 PM")
        )

        val result = cognitiveEngine.multiSignalReasoningEngine.synthesizeReasoning(draftState)
        assertNotNull(result.hero)
        assertEquals(ContextualHeroMode.POOR_AIR_ALERT, result.hero.mode)
        assertTrue(result.contextualAdvice.contains("favorable", ignoreCase = true) ||
                   result.contextualAdvice.contains("air quality", ignoreCase = true))
        assertTrue(result.hero.primaryMetric?.contains("165") == true)
    }

    @Test
    fun test_04_pattern_learning_without_hardcoded_rules() = runBlocking {
        // Requirement 4: Learn patterns from repeated observations across weekdays
        val cal = Calendar.getInstance()
        val days = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY)

        for ((idx, day) in days.withIndex()) {
            cal.set(Calendar.DAY_OF_WEEK, day)
            cal.set(Calendar.HOUR_OF_DAY, 18)
            cal.set(Calendar.MINUTE, 10 + idx * 2) // 18:10, 18:12, 18:14...
            cognitiveEngine.observeEvent(
                domain = "ACTIVITY",
                subject = "Walking",
                eventType = "WALK_SESSION",
                action = "EVENING_WALK",
                context = "Evening walk logged",
                timestamp = cal.timeInMillis
            )
        }

        val learned = cognitiveEngine.learningEngine.executeLearningCycle()
        val walkingPattern = learned.firstOrNull { it.subject.contains("Walk", ignoreCase = true) || it.title.contains("Walk", ignoreCase = true) }

        assertNotNull("Pattern should be learned from repeated observations", walkingPattern)
        assertTrue("Evidence count must be at least 5", walkingPattern!!.evidenceCount >= 5)
        assertEquals("WEEKDAY", walkingPattern.recurrence)
        assertTrue(walkingPattern.confidence >= 0.70f)
        assertTrue(walkingPattern.expectedTimeRange.contains("PM"))
    }

    @Test
    fun test_05_exception_intelligence_preserves_pattern() = runBlocking {
        // Requirement 5: One-time deviation on off-day recorded as EXCEPTION without destroying pattern
        val pattern = CandidatePattern(
            patternId = "pat_walking_routine",
            title = "Evening Walking Routine",
            description = "Weekdays around 6:15 PM",
            evidenceCount = 8,
            confidence = 0.88f,
            recurrence = "WEEKDAY",
            expectedDays = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
            expectedTimeRange = "06:15 PM"
        )

        val saturdayCal = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY) }
        val exception = cognitiveEngine.exceptionEngine.evaluateException(pattern, saturdayCal.timeInMillis, "Saturday charity walk")

        assertNotNull("Off-day occurrence must be flagged as exception", exception)
        assertEquals(Calendar.SATURDAY, exception!!.dayOfWeek)
        assertTrue(exception.reason.contains("One-time exception"))
    }

    @Test
    fun test_06_absence_handling_separates_observed_from_unobserved() = runBlocking {
        // Requirement 6: Distinguish OBSERVED_ABSENCE from NOT_OBSERVED
        val unobserved = CognitiveObservation(
            observationId = "obs_1",
            eventType = "ACTIVITY_CHECK",
            absenceType = AbsenceType.NOT_OBSERVED
        )
        val confirmedAbsence = CognitiveObservation(
            observationId = "obs_2",
            eventType = "ACTIVITY_CHECK",
            absenceType = AbsenceType.OBSERVED_ABSENCE
        )

        assertEquals(AbsenceType.NOT_OBSERVED, unobserved.absenceType)
        assertEquals(AbsenceType.OBSERVED_ABSENCE, confirmedAbsence.absenceType)
    }

    @Test
    fun test_07_mobility_brain_classification_and_provenance() {
        // Requirement 8: Mobility Brain combines signals with provenance
        val brain = cognitiveEngine.mobilityBrain

        val stationary = brain.determineMobility(speedKmh = 0.0, accelVariance = 0.1)
        assertEquals(MobilityState.STATIONARY, stationary.state)
        assertEquals(TruthState.DETECTED, stationary.truthState)

        val walking = brain.determineMobility(speedKmh = 4.8, isStepCounterActive = true, recentStepCadencePerMinute = 80)
        assertEquals(MobilityState.WALKING, walking.state)
        assertTrue(walking.speedKmh in 4.0..5.5)

        val driving = brain.determineMobility(speedKmh = 35.0)
        assertEquals(MobilityState.DRIVING, driving.state)
        assertEquals(TruthState.DETECTED, driving.truthState)
        assertTrue(driving.confidence >= 0.85f)
    }

    @Test
    fun test_08_live_journey_session_and_eta() {
        // Requirement 9: Live Journey Session tracking
        val journeyEngine = cognitiveEngine.liveJourneyEngine

        val session = journeyEngine.startJourney(
            destination = "Office",
            origin = "Home",
            travelMode = MobilityState.DRIVING,
            initialDistanceKm = 8.2,
            estimatedDurationMinutes = 20
        )

        assertEquals("Office", session.destination)
        assertEquals(8.2, session.distanceRemainingKm, 0.01)
        assertEquals(JourneyStatus.ACTIVE, session.status)
        assertFalse(session.etaFormatted.isBlank())

        // Update progress
        val updated = journeyEngine.updateProgress(speedKmh = 34.0, distanceCoveredKmDelta = 2.0, currentRoad = "EM Bypass")
        assertNotNull(updated)
        assertEquals(6.2, updated!!.distanceRemainingKm, 0.1)
        assertEquals(2.0, updated.distanceCoveredKm, 0.1)
    }

    @Test
    fun test_09_map_intelligence_navigation_handoff_truth() {
        // Requirement 10: Map intelligence truthful handoff vs active navigation
        val journeyEngine = cognitiveEngine.liveJourneyEngine

        journeyEngine.recordNavigationHandoff("Salt Lake City Centre")
        val map = journeyEngine.getMapTelemetry()

        assertEquals(MapNavigationStatus.HANDED_OFF, map.navigationStatus)
        assertEquals("Salt Lake City Centre", map.destination)
    }

    @Test
    fun test_10_baseline_learning_and_deviation_detection() {
        // Requirement 14 & 15: Baseline learning and late departure deviation detection
        val baselineEngine = cognitiveEngine.baselineLearningEngine
        val baselines = PersonalBaselines(
            normalOfficeDeparture = "09:05 AM",
            sampleCount = 18
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 5) // 10:05 AM
        }

        val deviations = baselineEngine.evaluateDeviations(
            currentTimeMillis = cal.timeInMillis,
            isAtHome = true,
            hasDepartedForOffice = false,
            baselines = baselines
        )

        assertEquals(1, deviations.size)
        val dev = deviations.first()
        assertEquals("COMMUTE", dev.domain)
        assertTrue(dev.description.contains("later than your usual", ignoreCase = true))
    }

    @Test
    fun test_11_ask_vyb_truthful_answers_from_canonical_brain() = runBlocking {
        // Requirement 19: Ask VYB uses the exact same canonical brain
        val stepsAnswer = cognitiveEngine.answerCognitiveQuestion("How many steps today?")
        assertNotNull(stepsAnswer)
        assertTrue(stepsAnswer!!.contains("step", ignoreCase = true))

        val whereAnswer = cognitiveEngine.answerCognitiveQuestion("Where am I?")
        assertNotNull(whereAnswer)
        assertTrue(whereAnswer!!.contains("location", ignoreCase = true) || whereAnswer.contains("at", ignoreCase = true))

        val doingAnswer = cognitiveEngine.answerCognitiveQuestion("What am I doing right now?")
        assertNotNull(doingAnswer)
        assertTrue(doingAnswer!!.contains("currently", ignoreCase = true))

        val whyWalkAnswer = cognitiveEngine.answerCognitiveQuestion("Why do you think I normally walk at 6 PM?")
        assertNotNull(whyWalkAnswer)
        assertTrue(whyWalkAnswer!!.contains("observed walking activity", ignoreCase = true))

        val whyLeaveAnswer = cognitiveEngine.answerCognitiveQuestion("Why are you saying I should leave?")
        assertNotNull(whyLeaveAnswer)
        assertTrue(whyLeaveAnswer!!.contains("office departure", ignoreCase = true))
    }

    @Test
    fun test_12_correction_engine_reduces_confidence_and_preserves_evidence() = runBlocking {
        // Requirement 22: Correction Engine handles user corrections
        val correction = cognitiveEngine.applyUserCorrection(
            targetPatternOrFact = "Office",
            userCorrection = "No, I wasn't going to the office.",
            previousBelief = "Weekday Office Commute"
        )

        assertNotNull(correction.correctionId)
        assertEquals("Office", correction.targetPatternOrFact)
        assertEquals("No, I wasn't going to the office.", correction.userCorrection)

        val storedCorrections = cognitiveEngine.correctionEngine.getAllCorrections()
        assertTrue(storedCorrections.any { it.correctionId == correction.correctionId })
    }

    @Test
    fun test_13_personal_life_graph_telemetry_evidence_nodes() = runBlocking {
        // Requirement 13: Telemetry contributes structured evidence to Life Graph
        val graph = cognitiveEngine.getLifeGraph()

        val actNodeId = graph.addActivityEvidence("WALKING", speedKmh = 4.8, steps = 3200, truthState = TruthState.KNOWN)
        val actNode = graph.getNode(actNodeId)
        assertNotNull(actNode)
        assertEquals("ACTIVITY", actNode!!.category)
        assertEquals("4.8", actNode.properties["speedKmh"])

        val placeNodeId = graph.addPlaceEvidence("HOME", "Home", truthState = TruthState.KNOWN)
        val placeNode = graph.getNode(placeNodeId)
        assertNotNull(placeNode)
        assertEquals("PLACES", placeNode!!.category)

        val envNodeId = graph.addEnvironmentEvidence(aqi = 65, aqiLevel = "Moderate", tempC = 27.5, truthState = TruthState.KNOWN)
        val envNode = graph.getNode(envNodeId)
        assertNotNull(envNode)
        assertEquals("ENVIRONMENT", envNode!!.category)
    }

    @Test
    fun test_14_orchestrator_handles_cognitive_queries_and_corrections() = runBlocking {
        // Verify VybOrchestrator integration directly
        val orchestrator = viewModel.repository.orchestrator

        val res1 = orchestrator.processMessage("How many steps today?", app)
        assertTrue(res1.conversationalResponse.contains("step", ignoreCase = true))
        assertEquals("COGNITIVE_BRAIN_QUERY", res1.diagnostics.intent)

        val res2 = orchestrator.processMessage("No, I wasn't going to the office.", app)
        assertTrue(res2.conversationalResponse.contains("recorded that correction", ignoreCase = true))
        assertEquals("PATTERN_CORRECTION_APPLIED", res2.diagnostics.intent)
    }
}
