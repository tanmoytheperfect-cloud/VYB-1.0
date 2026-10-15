package com.example.domain.cognitive.engine

import com.example.data.local.AppDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.FamilyMemberEntity
import com.example.domain.cognitive.graph.PersonalLifeGraph
import com.example.domain.cognitive.models.*
import com.example.domain.environment.model.*
import com.example.domain.temporal.VybTimeOfDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

/**
 * Maintains the canonical Personal Life Graph and CurrentLifeState.
 * Unifies living environment telemetry, physical activity, mobility, journeys,
 * family data, routines, tasks, baselines, and contextual meaning into ONE canonical state.
 */
class LifeContextEngine(
    private val lifeGraph: PersonalLifeGraph = PersonalLifeGraph(),
    private val familyDao: FamilyDao,
    private val memoryDao: MemoryDao,
    private val candidatePatternDao: CandidatePatternDao,
    private val database: AppDatabase? = null,
    private val mobilityBrain: MobilityBrain = MobilityBrain(),
    private val baselineLearningEngine: BaselineLearningEngine = BaselineLearningEngine(),
    private val multiSignalReasoningEngine: MultiSignalReasoningEngine = MultiSignalReasoningEngine(),
    private val liveJourneyEngine: LiveJourneyEngine = LiveJourneyEngine(),
    private val temporalReasoningEngine: TemporalReasoningEngine = TemporalReasoningEngine()
) {

    private val _currentLifeState = MutableStateFlow(CurrentLifeState())
    val currentLifeState: StateFlow<CurrentLifeState> = _currentLifeState.asStateFlow()

    private var latestLivingSignals = LivingSignals()
    private var latestMobilityDetermination: MobilityDetermination? = null

    fun getLifeGraph(): PersonalLifeGraph = lifeGraph
    fun getLiveJourneyEngine(): LiveJourneyEngine = liveJourneyEngine
    fun getBaselineLearningEngine(): BaselineLearningEngine = baselineLearningEngine
    fun getMultiSignalReasoningEngine(): MultiSignalReasoningEngine = multiSignalReasoningEngine

    /**
     * Integrates live sensory telemetry directly into CurrentLifeState.
     */
    suspend fun integrateLivingTelemetry(
        signals: LivingSignals,
        speedKmh: Double? = null,
        accelVariance: Double = 0.0,
        cadenceSpM: Int = 0
    ) {
        latestLivingSignals = signals
        val mobility = mobilityBrain.determineMobility(
            speedKmh = speedKmh,
            accelVariance = accelVariance,
            isStepCounterActive = signals.steps.truthState == MetricTruthState.KNOWN,
            recentStepCadencePerMinute = cadenceSpM,
            activityRecognition = signals.activity.state
        )
        latestMobilityDetermination = mobility

        // Update Personal Life Graph with physical signals
        lifeGraph.addActivityEvidence(
            activityType = mobility.state.name,
            speedKmh = mobility.speedKmh,
            steps = signals.steps.steps,
            truthState = mobility.truthState
        )

        signals.airQuality?.let { air ->
            lifeGraph.addEnvironmentEvidence(
                aqi = air.aqi,
                aqiLevel = air.level.label,
                tempC = signals.weather?.tempC ?: 24.0,
                truthState = air.truthState.toCognitiveTruthState()
            )
        }

        refreshState()
    }

    /**
     * Ingests a discovered fact from conversation into the canonical model.
     */
    suspend fun ingestDiscoveredFact(
        rawSentence: String,
        category: String,
        key: String,
        value: String,
        confidence: Float = 0.95f
    ) {
        val lower = rawSentence.lowercase().trim()

        // Detect and process family relationship claims
        val isFamilyRelation = lower.contains("son") || lower.contains("daughter") ||
                lower.contains("wife") || lower.contains("spouse") || lower.contains("mother") || lower.contains("father")

        if (isFamilyRelation || category.equals("FAMILY", ignoreCase = true)) {
            val relation = when {
                lower.contains("son") -> "Son"
                lower.contains("daughter") -> "Daughter"
                lower.contains("wife") || lower.contains("spouse") -> "Spouse"
                lower.contains("mother") || lower.contains("mom") -> "Mother"
                lower.contains("father") || lower.contains("dad") -> "Father"
                else -> "Family Member"
            }

            val extractedName = extractNameFromSentence(rawSentence, relation)
            if (extractedName.isNotBlank()) {
                val existingMembers = familyDao.getAllFamilyMembersDirect()
                val existing = existingMembers.firstOrNull { it.name.equals(extractedName, ignoreCase = true) }
                if (existing == null) {
                    familyDao.insertFamilyMember(
                        FamilyMemberEntity(
                            name = extractedName,
                            relation = relation,
                            occupationOrSchool = if (lower.contains("school")) "School" else ""
                        )
                    )
                }

                lifeGraph.addFamilyMember(
                    name = extractedName,
                    relation = relation,
                    occupationOrSchool = if (lower.contains("school")) "School" else "",
                    truthState = TruthState.USER_CONFIRMED
                )
            }
        }

        refreshState()
    }

    /**
     * Refreshes the canonical CurrentLifeState from all authoritative Room tables
     * and living sensors.
     */
    suspend fun refreshState() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeekInt = cal.get(Calendar.DAY_OF_WEEK)
        val dayOfWeekStr = when (dayOfWeekInt) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Today"
        }
        val isWeekend = dayOfWeekInt == Calendar.SATURDAY || dayOfWeekInt == Calendar.SUNDAY
        val dayCategory = if (isWeekend) LifeDayCategory.WEEKEND else LifeDayCategory.WORKDAY

        val timeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(now))
        val dateFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date(now))
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now))

        // 1. Family Members
        val family = familyDao.getAllFamilyMembersDirect()
        val familyContexts = family.map { m ->
            lifeGraph.addFamilyMember(
                name = m.name,
                relation = m.relation,
                occupationOrSchool = m.occupationOrSchool,
                truthState = TruthState.USER_CONFIRMED
            )
            FamilyMemberContext(
                name = m.name,
                relation = m.relation,
                occupationOrSchool = m.occupationOrSchool,
                truthState = TruthState.USER_CONFIRMED
            )
        }

        // 2. Learned Patterns
        val patternEntities = candidatePatternDao.getAllPatterns()
        val activePatterns = patternEntities.map {
            CandidatePattern(
                patternId = it.patternId,
                ownerId = it.ownerId,
                domain = it.domain,
                subject = it.subject,
                title = it.title,
                description = it.description,
                evidenceCount = it.evidenceCount,
                supportingObservationIds = emptyList(),
                contradictoryObservationIds = emptyList(),
                confidence = it.confidence,
                firstObserved = it.firstObserved,
                lastObserved = it.lastObserved,
                recurrence = it.recurrence,
                expectedDays = parseExpectedDays(it.expectedDaysJson),
                expectedTimeRange = it.expectedTimeRange,
                expectedContext = it.expectedContext,
                exceptions = emptyList(),
                status = try { PatternStatus.valueOf(it.status) } catch (e: Exception) { PatternStatus.CANDIDATE },
                explanation = it.explanation,
                truthState = try { TruthState.valueOf(it.truthState) } catch (e: Exception) { TruthState.LEARNED },
                isUserConfirmed = it.isUserConfirmed,
                updatedAt = it.updatedAt
            )
        }

        // 3. Routines, Tasks, Goals from Database if available
        val routines = database?.routineDao()?.getAllRoutinesDirect()?.filter { it.isEnabled } ?: emptyList()
        routines.forEach { r ->
            lifeGraph.addRoutine(r.title, r.timeOfDay, r.daysOfWeek, TruthState.USER_CONFIRMED)
        }
        val tasks = database?.taskDao()?.getAllTasksDirect() ?: emptyList()
        val goals = database?.goalDao()?.getAllGoalsDirect()?.map { it.title } ?: emptyList()
        goals.forEach { g ->
            lifeGraph.addGoal(g, 1.0f)
        }

        // 4. Observations
        val observations = database?.observationDao()?.getAllObservations()?.map { obs ->
            CognitiveObservation(
                observationId = obs.observationId,
                ownerId = obs.ownerId,
                timestamp = obs.timestamp,
                timezone = obs.timezone,
                dateString = obs.dateString,
                dayOfWeek = obs.dayOfWeek,
                source = obs.source,
                domain = obs.domain,
                subject = obs.subject,
                eventType = obs.eventType,
                action = obs.action,
                context = obs.context,
                entities = emptyMap(),
                location = obs.location,
                confidence = obs.confidence,
                truthState = try { TruthState.valueOf(obs.truthState) } catch (e: Exception) { TruthState.OBSERVED },
                observedValue = obs.observedValue,
                value = obs.observedValue
            )
        } ?: emptyList()

        // 5. Baselines and Deviations
        val baselines = baselineLearningEngine.computeBaselines(observations, activePatterns)
        lifeGraph.addBaselineEvidence("Office Departure", baselines.normalOfficeDeparture ?: "09:05 AM", baselines.confidence)
        lifeGraph.addBaselineEvidence("Evening Walk", baselines.normalWalkingTime ?: "06:15 PM", baselines.confidence)

        val isAtHome = latestLivingSignals.location.locality?.contains("Home", ignoreCase = true) ?: true
        val hasDeparted = latestMobilityDetermination?.state == MobilityState.DRIVING || liveJourneyEngine.getActiveSession() != null
        val deviations = baselineLearningEngine.evaluateDeviations(
            currentTimeMillis = now,
            isAtHome = isAtHome,
            hasDepartedForOffice = hasDeparted,
            baselines = baselines
        )

        // 6. Mobility & Journey
        val mobilityDet = latestMobilityDetermination ?: mobilityBrain.determineMobility(
            activityRecognition = latestLivingSignals.activity.state
        )
        val activeJourney = liveJourneyEngine.getActiveSession()
        val mapTelemetry = liveJourneyEngine.getMapTelemetry()

        val placeType = when {
            activeJourney != null || mobilityDet.state == MobilityState.DRIVING -> KnownPlaceType.COMMUTE
            latestLivingSignals.activity.state == ActivityState.WALKING -> KnownPlaceType.OUTDOORS
            isAtHome -> KnownPlaceType.HOME
            else -> KnownPlaceType.OFFICE
        }
        val placeLabel = when (placeType) {
            KnownPlaceType.COMMUTE -> "En Route to ${activeJourney?.destination ?: "Destination"}"
            KnownPlaceType.OUTDOORS -> "Outdoors"
            KnownPlaceType.HOME -> "Home"
            KnownPlaceType.OFFICE -> "Office"
            KnownPlaceType.UNKNOWN -> "Current Location"
        }

        // Canonical Environment State (Requirement 5)
        val canonicalEnv = CurrentEnvironmentState(
            temperature = latestLivingSignals.weather?.tempC,
            feelsLike = latestLivingSignals.weather?.feelsLikeC,
            humidity = latestLivingSignals.weather?.humidity,
            uv = latestLivingSignals.weather?.uvIndex,
            weatherCondition = latestLivingSignals.weather?.conditionText,
            aqi = latestLivingSignals.airQuality?.aqi,
            pm25 = latestLivingSignals.airQuality?.pm25,
            pm10 = latestLivingSignals.airQuality?.pm10,
            timestamp = latestLivingSignals.weather?.timestamp ?: latestLivingSignals.lastRefreshTimestamp,
            source = latestLivingSignals.weather?.source ?: "Open-Meteo Environmental Network",
            freshness = latestLivingSignals.getFreshnessString(latestLivingSignals.weather?.timestamp ?: latestLivingSignals.lastRefreshTimestamp),
            truthState = latestLivingSignals.weather?.truthState ?: MetricTruthState.KNOWN
        )

        // Canonical Location State (Requirement 10)
        val canonicalLoc = CurrentLocationState(
            latitude = latestLivingSignals.location.latitude,
            longitude = latestLivingSignals.location.longitude,
            address = latestLivingSignals.location.locality,
            area = latestLivingSignals.location.locality,
            city = if (latestLivingSignals.location.latitude != null) "Current City" else null,
            accuracy = 12.0f,
            timestamp = latestLivingSignals.location.timestamp,
            source = "Android Location Provider",
            freshness = latestLivingSignals.getFreshnessString(latestLivingSignals.location.timestamp),
            truthState = latestLivingSignals.location.truthState
        )

        // Canonical Journey State (Requirement 12)
        val canonicalJourney = if (activeJourney != null) {
            CurrentJourneyState(
                origin = activeJourney.origin,
                destination = activeJourney.destination,
                travelMode = activeJourney.travelMode,
                currentSpeed = activeJourney.currentSpeedKmh,
                distanceTravelled = activeJourney.distanceCoveredKm,
                distanceRemaining = activeJourney.distanceRemainingKm,
                elapsedTime = activeJourney.elapsedTimeMinutes.toLong(),
                ETA = activeJourney.etaFormatted,
                route = activeJourney.routeName,
                status = activeJourney.status,
                source = "Active GPS Telemetry",
                freshness = "Live"
            )
        } else {
            CurrentJourneyState(
                status = JourneyStatus.NOT_TRAVELLING
            )
        }

        // Canonical Next Up (Requirement 1, 2) resolved using empirical temporal reasoning
        val resolvedNextUp = temporalReasoningEngine.resolveNextUp(
            nowMillis = now,
            timezoneId = TimeZone.getDefault().id,
            learnedPatterns = activePatterns,
            routines = routines,
            intentions = try { database?.persistentIntentionDao()?.getAllIntentionsDirect() ?: emptyList() } catch (e: Exception) { emptyList() },
            canonicalEnvironment = canonicalEnv
        )

        // Construct canonical state
        val draftState = CurrentLifeState(
            ownerId = "primary_owner",
            ownerName = "Tanmoy",
            ownerEmail = "tanmoy@metropolifashions.com",
            currentTimeMillis = now,
            timeFormatted = timeFormatted,
            dateFormatted = dateFormatted,
            dateString = dateString,
            dayOfWeek = dayOfWeekStr,
            dayOfWeekInt = dayOfWeekInt,
            dayCategory = dayCategory,
            timezone = TimeZone.getDefault().id,
            timeOfDay = VybTimeOfDay.fromMillis(now),

            location = latestLivingSignals.location,
            currentLocation = canonicalLoc,
            currentPlaceType = placeType,
            currentPlaceLabel = placeLabel,

            canonicalEnvironment = canonicalEnv,
            weather = latestLivingSignals.weather,
            isWeatherStale = latestLivingSignals.isWeatherStale,

            airQuality = latestLivingSignals.airQuality,
            isAirQualityStale = latestLivingSignals.isAirQualityStale,

            steps = latestLivingSignals.steps,
            activity = latestLivingSignals.activity,
            mobilityState = mobilityDet.state,
            mobilityTruthState = mobilityDet.truthState,
            speedKmh = mobilityDet.speedKmh,

            activeJourney = activeJourney,
            currentJourney = canonicalJourney,
            mapTelemetry = mapTelemetry,

            bloodOxygen = latestLivingSignals.bloodOxygen,
            heartRateBpm = null,
            restingHeartRateBpm = null,
            sleepHours = null,
            healthTruthState = latestLivingSignals.bloodOxygen.truthState,

            deviceState = "UNLOCKED_ACTIVE",
            batteryPercent = null,
            isCharging = false,
            isOnline = latestLivingSignals.isOnline,

            familyMembers = familyContexts,
            activeRoutines = routines.map {
                com.example.data.local.entity.LearnedRoutineEntity(
                    routineId = "rt_${it.id}",
                    title = it.title,
                    description = it.description,
                    daysOfWeek = it.daysOfWeek,
                    expectedTimeWindow = it.timeOfDay,
                    confidenceScore = 0.95f
                )
            },
            activePatterns = activePatterns,
            todayObservations = observations.takeLast(20),
            upcomingNextUp = resolvedNextUp,
            activeTasks = tasks,
            calendarCommitments = emptyList(),
            todayExceptions = emptyList(),
            personalGoals = goals,

            personalBaselines = baselines,
            detectedDeviations = deviations,
            activePredictions = emptyList(),

            lastEvaluatedMillis = now,
            isOfflineMode = !latestLivingSignals.isOnline
        )

        // 7. Multi-Signal Reasoning & Contextual Meaning Hero
        val reasoningResult = multiSignalReasoningEngine.synthesizeReasoning(draftState)

        _currentLifeState.value = draftState.copy(
            contextualHero = reasoningResult.hero
        )
    }

    private fun extractNameFromSentence(sentence: String, relation: String): String {
        val words = sentence.split(" ", ".", ",", "!")
        val candidate = words.firstOrNull { it.isNotBlank() && !it.equals("my", ignoreCase = true) && !it.equals("is", ignoreCase = true) && !it.equals(relation, ignoreCase = true) }
        return candidate?.replaceFirstChar { it.uppercase() } ?: ""
    }

    private fun parseExpectedDays(json: String): List<Int> {
        val digits = json.replace("[", "").replace("]", "").split(",").mapNotNull { it.trim().toIntOrNull() }
        return if (digits.isNotEmpty()) digits else listOf(2, 3, 4, 5, 6)
    }

    private fun MetricTruthState.toCognitiveTruthState(): TruthState = when (this) {
        MetricTruthState.KNOWN -> TruthState.KNOWN
        MetricTruthState.DETECTED -> TruthState.DETECTED
        MetricTruthState.INFERRED -> TruthState.INFERRED
        MetricTruthState.PERMISSION_REQUIRED -> TruthState.REQUIRES_PERMISSION
        MetricTruthState.DEVICE_REQUIRED -> TruthState.UNAVAILABLE
        MetricTruthState.UNAVAILABLE_OFFLINE -> TruthState.UNAVAILABLE
        MetricTruthState.STALE -> TruthState.STALE
        MetricTruthState.UNKNOWN -> TruthState.UNKNOWN
    }
}
