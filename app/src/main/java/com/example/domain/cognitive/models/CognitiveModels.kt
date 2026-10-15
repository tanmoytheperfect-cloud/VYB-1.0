package com.example.domain.cognitive.models

import com.example.data.local.entity.LearnedRoutineEntity
import com.example.data.local.entity.TaskItemEntity
import com.example.domain.environment.model.*
import com.example.domain.temporal.VybTimeOfDay
import java.util.Calendar
import java.util.TimeZone

/**
 * Strict Truth Model distinguishing empirical ground truth from inferred hypotheses.
 */
enum class TruthState {
    KNOWN,
    VERIFIED,
    USER_CONFIRMED,
    EXTRACTED,
    OBSERVED,
    DETECTED,
    INFERRED,
    LEARNED,
    ESTIMATED,
    STALE,
    UNKNOWN,
    UNAVAILABLE,
    REQUIRES_PERMISSION,
    MOCK,
    UNSUPPORTED
}

/**
 * Absence distinction to prevent falsely equating lack of sensor coverage with non-occurrence.
 */
enum class AbsenceType {
    NOT_OBSERVED,       // No observation / sensor coverage during the period
    OBSERVED_ABSENCE    // Active coverage confirmed the user did NOT perform the activity
}

/**
 * Lifecycle status of candidate and established patterns.
 */
enum class PatternStatus {
    CANDIDATE,       // 1-2 observations: hypothesis formed, low confidence
    EMERGING,        // 3-4 observations: recurring pattern emerging
    ESTABLISHED,     // 5+ observations: proven recurring behavior, high confidence
    CONTRADICTED,    // Recurring contradictions detected
    STALE,           // Has not been observed within expected lifecycle
    RETIRED          // Explicitly replaced or corrected by user
}

/**
 * Structured observation recording real authorized activities and telemetry.
 */
data class CognitiveObservation(
    val observationId: String,
    val ownerId: String = "primary_owner",
    val timestamp: Long = System.currentTimeMillis(),
    val timezone: String = TimeZone.getDefault().id,
    val dateString: String = "", // "YYYY-MM-DD"
    val dayOfWeek: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
    val source: String = "SYSTEM",
    val domain: String = "GENERAL", // TRANSPORT, EDUCATION, WORK, FINANCE, FAMILY, HABIT, HEALTH, ENVIRONMENT, MOBILITY, ACTIVITY
    val subject: String = "",       // e.g. "Adhyansh", "Owner", "School Commute", "Walking", "Air Quality"
    val eventType: String = "",     // "SCHOOL_ACTIVITY", "TRANSPORT_BOOKED", "WAKE_TIME", "STEP_COUNT", "AQI_READING", "MOBILITY_STATE", etc.
    val action: String = "",
    val context: String = "",
    val entities: Map<String, String> = emptyMap(),
    val location: String = "",
    val relatedRoutine: String? = null,
    val relatedPerson: String? = null,
    val confidence: Float = 1.0f,
    val truthState: TruthState = TruthState.OBSERVED,
    val observedValue: String = "",
    val value: String = observedValue,
    val unit: String = "",
    val freshnessMs: Long = 0L,
    val absenceType: AbsenceType = AbsenceType.NOT_OBSERVED,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Record of an approved one-time deviation from an established routine.
 */
data class PatternException(
    val exceptionId: String,
    val patternId: String,
    val dateString: String,
    val dayOfWeek: Int,
    val reason: String,
    val wasExplicitlyRequested: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Candidate or established learned pattern.
 */
data class CandidatePattern(
    val patternId: String,
    val ownerId: String = "primary_owner",
    val domain: String = "GENERAL",
    val subject: String = "",
    val title: String,
    val description: String,
    val evidenceCount: Int = 0,
    val supportingObservationIds: List<String> = emptyList(),
    val contradictoryObservationIds: List<String> = emptyList(),
    val confidence: Float = 0.0f,
    val firstObserved: Long = System.currentTimeMillis(),
    val lastObserved: Long = System.currentTimeMillis(),
    val recurrence: String = "WEEKDAY", // "WEEKDAY", "WEEKEND", "DAILY", "CUSTOM", "NONE"
    val expectedDays: List<Int> = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY),
    val expectedTimeRange: String = "08:30 AM",
    val expectedContext: String = "",
    val exceptions: List<PatternException> = emptyList(),
    val status: PatternStatus = PatternStatus.CANDIDATE,
    val explanation: String = "",
    val truthState: TruthState = TruthState.LEARNED,
    val isUserConfirmed: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Canonical Next Up item resolved by TemporalReasoningEngine for Home Screen presentation.
 */
data class NextUpItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val timeDisplay: String, // e.g. "08:30 AM"
    val isToday: Boolean,
    val nextOccurrenceEpochMs: Long,
    val recurrenceRule: String = "WEEKDAY",
    val isException: Boolean = false,
    val confidence: Float = 1.0f,
    val truthState: TruthState = TruthState.LEARNED,
    val explanation: String = "",
    val commuteIcon: String = "CAR", // "CAR", "WALK", "TRANSIT"
    val weatherDisplay: String? = null
)

/**
 * Physical mobility classification.
 */
enum class MobilityState(val displayLabel: String) {
    STATIONARY("Stationary"),
    WALKING("Walking"),
    RUNNING("Running"),
    CYCLING("Cycling"),
    DRIVING("Driving"),
    UNKNOWN("Unknown")
}

/**
 * Real place classification for owner's world.
 */
enum class KnownPlaceType(val displayLabel: String) {
    HOME("Home"),
    OFFICE("Office"),
    COMMUTE("In Transit"),
    OUTDOORS("Outdoors"),
    UNKNOWN("Unknown Location")
}

/**
 * Active Live Journey session state.
 */
enum class JourneyStatus {
    NOT_TRAVELLING,
    DETECTED,
    ACTIVE,
    PAUSED,
    ARRIVING,
    COMPLETED,
    HANDED_OFF,
    IDLE,
    ARRIVED
}

/**
 * Canonical Environment State (Requirement 5).
 * Guarantees ONE single source of truth across all Home screen cards.
 */
data class CurrentEnvironmentState(
    val temperature: Double? = null,
    val feelsLike: Double? = null,
    val humidity: Int? = null,
    val uv: Double? = null,
    val weatherCondition: String? = null,
    val aqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "Open-Meteo",
    val freshness: String = "Live",
    val truthState: MetricTruthState = MetricTruthState.KNOWN
)

/**
 * Canonical Location State (Requirement 10).
 */
data class CurrentLocationState(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String? = null,
    val area: String? = null,
    val city: String? = null,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "GPS",
    val freshness: String = "Live",
    val truthState: MetricTruthState = MetricTruthState.PERMISSION_REQUIRED
)

/**
 * Canonical Journey State (Requirement 12).
 */
data class CurrentJourneyState(
    val origin: String? = null,
    val destination: String? = null,
    val travelMode: MobilityState = MobilityState.STATIONARY,
    val currentSpeed: Double? = null,
    val distanceTravelled: Double? = null,
    val distanceRemaining: Double? = null,
    val elapsedTime: Long? = null,
    val ETA: String? = null,
    val route: String? = null,
    val status: JourneyStatus = JourneyStatus.NOT_TRAVELLING,
    val source: String = "Telemetry",
    val freshness: String = "Live"
)

data class JourneySession(
    val sessionId: String,
    val startTimeMillis: Long,
    val origin: String = "Current Location",
    val destination: String,
    val travelMode: MobilityState = MobilityState.DRIVING,
    val currentSpeedKmh: Double = 0.0,
    val distanceCoveredKm: Double = 0.0,
    val distanceRemainingKm: Double = 0.0,
    val elapsedTimeMinutes: Int = 0,
    val etaEpochMs: Long? = null,
    val etaFormatted: String = "",
    val stops: List<String> = emptyList(),
    val routeName: String = "",
    val status: JourneyStatus = JourneyStatus.ACTIVE,
    val truthState: TruthState = TruthState.KNOWN
)

/**
 * Map intelligence telemetry.
 */
enum class MapNavigationStatus {
    IDLE,
    NAVIGATION_ACTIVE,
    HANDED_OFF
}

data class MapTelemetry(
    val currentRoad: String = "",
    val destination: String = "",
    val distanceRemainingKm: Double = 0.0,
    val etaDisplay: String = "",
    val trafficLevel: String = "Normal",
    val alternateRouteAvailable: Boolean = false,
    val navigationStatus: MapNavigationStatus = MapNavigationStatus.IDLE
)

/**
 * Structured family member context in life state.
 */
data class FamilyMemberContext(
    val name: String,
    val relation: String,
    val occupationOrSchool: String = "",
    val truthState: TruthState = TruthState.USER_CONFIRMED
)

/**
 * Learned owner baseline ("What is normal for me").
 */
data class PersonalBaselines(
    val normalWakeTime: String? = "07:00 AM",
    val normalOfficeDeparture: String? = "09:05 AM",
    val normalOfficeCommuteMinutes: Int? = 25,
    val normalWalkingTime: String? = "06:15 PM",
    val normalStepRange: String? = "5,000–7,000 steps",
    val normalActivityPeriods: List<String> = listOf("Morning Commute (08:50–09:20 AM)", "Evening Walk (06:00–06:45 PM)"),
    val sampleCount: Int = 0,
    val varianceMinutes: Int = 15,
    val confidence: Float = 0.85f,
    val recentDeviation: String? = null,
    val historicalRange: String = "8:55 AM – 9:15 AM departure on weekdays"
)

/**
 * Deviation detected against learned baseline.
 */
data class DetectedDeviation(
    val id: String,
    val domain: String, // "COMMUTE", "ACTIVITY", "SCHEDULE"
    val description: String,
    val baselineExpected: String,
    val observedValue: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isExceptionRecorded: Boolean = false
)

/**
 * Cognitive prediction produced from learned patterns.
 */
data class CognitivePrediction(
    val patternId: String,
    val predictedSubject: String,
    val predictedDomain: String,
    val anticipatedTime: String,
    val confidence: Float,
    val reason: String
)

/**
 * User-provided correction to an inferred belief or routine.
 */
data class CognitiveCorrection(
    val correctionId: String,
    val targetPatternOrFact: String,
    val userCorrection: String,
    val previousBelief: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Contextual Meaning Hero mode selecting what matters NOW on Home Screen.
 */
enum class ContextualHeroMode {
    ON_THE_MOVE,    // Driving / in transit
    WALKING,        // Active walk
    RESTING_HOME,   // At home relaxing or preparing for day
    OFFICE_WORK,    // At office working
    POOR_AIR_ALERT, // Severe AQI alert
    EVENING_WIND_DOWN
}

data class ContextualHero(
    val mode: ContextualHeroMode = ContextualHeroMode.RESTING_HOME,
    val title: String = "GOOD MORNING",
    val subtitle: String = "You're home.",
    val badge: String = "HOME",
    val primaryMetric: String? = null,
    val secondaryMetric: String? = null,
    val detail: String = "Your usual office departure is around 9:05 AM.",
    val evidenceExplanation: String? = "Based on 18 weekday departure observations"
)

/**
 * Day classification for current life state.
 */
enum class LifeDayCategory {
    WORKDAY,
    WEEKEND,
    SCHOOL_DAY,
    HOLIDAY,
    LEAVE_DAY
}

/**
 * CANONICAL CURRENT LIFE STATE FOR VYB.
 * Combines identity, time, environment, air quality, activity, health,
 * mobility, journey, family, routines, tasks, baselines, and contextual hero.
 */
data class CurrentLifeState(
    // 1. IDENTITY
    val ownerId: String = "primary_owner",
    val ownerName: String = "Tanmoy",
    val ownerEmail: String = "tanmoy@metropolifashions.com",

    // 2. TEMPORAL / TIME / DATE
    val currentTimeMillis: Long = System.currentTimeMillis(),
    val timeFormatted: String = "",
    val dateFormatted: String = "",
    val dateString: String = "", // "YYYY-MM-DD"
    val dayOfWeek: String = "",
    val dayOfWeekInt: Int = Calendar.getInstance().get(Calendar.DAY_OF_WEEK),
    val dayCategory: LifeDayCategory = LifeDayCategory.WORKDAY,
    val timezone: String = TimeZone.getDefault().id,
    val timeOfDay: VybTimeOfDay = VybTimeOfDay.DAY,

    // 3. LOCATION & PLACES
    val location: LocationContext = LocationContext(null, null, null, MetricTruthState.PERMISSION_REQUIRED, System.currentTimeMillis()),
    val currentLocation: CurrentLocationState = CurrentLocationState(),
    val currentPlaceType: KnownPlaceType = KnownPlaceType.HOME,
    val currentPlaceLabel: String = "Home",

    // 4. ENVIRONMENT & WEATHER (CANONICAL STATE)
    val canonicalEnvironment: CurrentEnvironmentState = CurrentEnvironmentState(),
    val weather: WeatherEnvironment? = null,
    val isWeatherStale: Boolean = false,

    // 5. AIR QUALITY
    val airQuality: AirQualityEnvironment? = null,
    val isAirQualityStale: Boolean = false,

    // 6. ACTIVITY & STEPS
    val steps: StepMetric = StepMetric(0, 0.0, 0, MetricTruthState.PERMISSION_REQUIRED, System.currentTimeMillis(), "Android Hardware Step Counter"),
    val activity: ActivityMetric = ActivityMetric(ActivityState.STATIONARY, MetricTruthState.DETECTED, 0.85f, System.currentTimeMillis(), "Sensor Telemetry"),
    val mobilityState: MobilityState = MobilityState.STATIONARY,
    val mobilityTruthState: TruthState = TruthState.DETECTED,
    val speedKmh: Double = 0.0,

    // 7. MOBILITY & LIVE JOURNEY
    val activeJourney: JourneySession? = null,
    val currentJourney: CurrentJourneyState = CurrentJourneyState(),
    val mapTelemetry: MapTelemetry = MapTelemetry(),

    // 8. HEALTH DATA (STRICT TRUTH, NO SOFTWARE ESTIMATIONS)
    val bloodOxygen: BloodOxygenMetric = BloodOxygenMetric(null, MetricTruthState.DEVICE_REQUIRED, null, null, "Requires compatible health sensor"),
    val heartRateBpm: Int? = null,
    val restingHeartRateBpm: Int? = null,
    val sleepHours: Double? = null,
    val healthTruthState: MetricTruthState = MetricTruthState.DEVICE_REQUIRED,

    // 9. DEVICE STATE & CONNECTIVITY
    val deviceState: String = "UNLOCKED_ACTIVE",
    val batteryPercent: Int? = null,
    val isCharging: Boolean = false,
    val isOnline: Boolean = true,

    // 10. LIFE CONTEXT: FAMILY, TASKS, CALENDAR, ROUTINES, GOALS
    val familyMembers: List<FamilyMemberContext> = emptyList(),
    val activeRoutines: List<LearnedRoutineEntity> = emptyList(),
    val activePatterns: List<CandidatePattern> = emptyList(),
    val todayObservations: List<CognitiveObservation> = emptyList(),
    val upcomingNextUp: NextUpItem? = null,
    val activeTasks: List<TaskItemEntity> = emptyList(),
    val calendarCommitments: List<String> = emptyList(),
    val todayExceptions: List<PatternException> = emptyList(),
    val personalGoals: List<String> = emptyList(),

    // 11. LEARNED BASELINES ("WHAT IS NORMAL FOR ME")
    val personalBaselines: PersonalBaselines = PersonalBaselines(),

    // 12. DETECTED DEVIATIONS
    val detectedDeviations: List<DetectedDeviation> = emptyList(),

    // 13. PREDICTIONS
    val activePredictions: List<CognitivePrediction> = emptyList(),

    // 14. CONTEXTUAL MEANING HERO (FOR HOME SCREEN)
    val contextualHero: ContextualHero = ContextualHero(
        mode = ContextualHeroMode.RESTING_HOME,
        title = "GOOD MORNING",
        subtitle = "You're home.",
        badge = "HOME",
        primaryMetric = null,
        detail = "Your usual office departure is around 9:05 AM.",
        evidenceExplanation = "Based on 18 weekday departure observations"
    ),

    // 15. PROVENANCE & FRESHNESS
    val lastEvaluatedMillis: Long = System.currentTimeMillis(),
    val isOfflineMode: Boolean = false
)
