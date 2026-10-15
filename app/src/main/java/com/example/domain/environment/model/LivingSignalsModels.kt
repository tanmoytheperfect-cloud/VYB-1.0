package com.example.domain.environment.model

/**
 * Air Quality severity classifications following national and international AQI standards.
 */
enum class AirQualityLevel(val label: String, val advisory: String) {
    GOOD("Good", "Air quality is satisfactory and poses little or no risk"),
    MODERATE("Moderate", "Air quality is acceptable; unusually sensitive people should consider limiting prolonged outdoor exertion"),
    POOR("Poor", "Members of sensitive groups may experience health effects; general public is less likely to be affected"),
    VERY_POOR("Very Poor", "Health alert: everyone may begin to experience health effects; avoid prolonged outdoor exertion"),
    HAZARDOUS("Hazardous", "Health warnings of emergency conditions: everyone should avoid outdoor physical activities");

    companion object {
        fun fromAqi(aqi: Int): AirQualityLevel = when {
            aqi <= 50 -> GOOD
            aqi <= 100 -> MODERATE
            aqi <= 150 -> POOR
            aqi <= 200 -> VERY_POOR
            else -> HAZARDOUS
        }
    }
}

/**
 * Truth states for every metric in VYB to guarantee TRUTH > APPEARANCE.
 */
enum class MetricTruthState {
    KNOWN,                  // Directly measured from active hardware / authoritative API
    DETECTED,               // Detected from genuine physical sensor patterns (accelerometer / step detector)
    INFERRED,               // Deduced through cognitive multi-signal reasoning
    UNKNOWN,                // Currently undetermined
    PERMISSION_REQUIRED,    // Requires explicit user permission grant
    DEVICE_REQUIRED,        // Requires dedicated compatible external medical/health hardware
    UNAVAILABLE_OFFLINE,    // Network data source cannot be reached while offline
    STALE                   // Cached data whose freshness threshold has expired
}

/**
 * Sensor audit status for physical device reporting (Samsung S23).
 */
enum class HardwareSensorStatus(val displayLabel: String) {
    AVAILABLE("AVAILABLE"),
    CONNECTED("CONNECTED"),
    PERMISSION_REQUIRED("PERMISSION REQUIRED"),
    DEVICE_REQUIRED("DEVICE REQUIRED"),
    API_UNAVAILABLE("API UNAVAILABLE"),
    NOT_IMPLEMENTED("NOT IMPLEMENTED")
}

data class WeatherEnvironment(
    val tempC: Double,
    val feelsLikeC: Double,
    val humidity: Int,
    val uvIndex: Double,
    val weatherCode: Int,
    val conditionText: String,
    val sunrise: String,
    val sunset: String,
    val isDay: Boolean,
    val timestamp: Long,
    val source: String,
    val truthState: MetricTruthState = MetricTruthState.KNOWN
)

data class AirQualityEnvironment(
    val aqi: Int,
    val pm25: Double,
    val pm10: Double,
    val no2: Double? = null,
    val o3: Double? = null,
    val level: AirQualityLevel,
    val timestamp: Long,
    val source: String,
    val truthState: MetricTruthState = MetricTruthState.KNOWN
)

enum class ActivityState(val label: String) {
    STATIONARY("Stationary"),
    WALKING("Walking"),
    RUNNING("Running"),
    CYCLING("Cycling"),
    DRIVING("Driving"),
    TRAVELLING("Travelling"),
    UNKNOWN("Unknown")
}

data class StepMetric(
    val steps: Int,
    val distanceKm: Double,
    val activeMinutes: Int,
    val truthState: MetricTruthState,
    val timestamp: Long,
    val source: String
)

data class ActivityMetric(
    val state: ActivityState,
    val truthState: MetricTruthState,
    val confidence: Float,
    val timestamp: Long,
    val source: String
)

data class BloodOxygenMetric(
    val spo2Percentage: Int?,
    val truthState: MetricTruthState,
    val timestamp: Long?,
    val source: String?,
    val statusMessage: String
)

data class LocationTelemetry(
    val latitude: Double?,
    val longitude: Double?,
    val locality: String?,
    val truthState: MetricTruthState,
    val timestamp: Long
)

data class SamsungS23SensorAudit(
    val locationStatus: HardwareSensorStatus,
    val weatherSourceStatus: HardwareSensorStatus,
    val aqiSourceStatus: HardwareSensorStatus,
    val stepCounterStatus: HardwareSensorStatus,
    val activityRecognitionStatus: HardwareSensorStatus,
    val compatibleWearableStatus: HardwareSensorStatus,
    val spo2AvailabilityStatus: HardwareSensorStatus,
    val deviceSensorsStatus: HardwareSensorStatus
)

/**
 * Living signals composite holding all current sensory inputs and freshness indicators.
 */
data class LivingSignals(
    val weather: WeatherEnvironment? = null,
    val airQuality: AirQualityEnvironment? = null,
    val steps: StepMetric = StepMetric(
        steps = 0,
        distanceKm = 0.0,
        activeMinutes = 0,
        truthState = MetricTruthState.PERMISSION_REQUIRED,
        timestamp = System.currentTimeMillis(),
        source = "Android Hardware Step Counter"
    ),
    val activity: ActivityMetric = ActivityMetric(
        state = ActivityState.STATIONARY,
        truthState = MetricTruthState.DETECTED,
        confidence = 0.85f,
        timestamp = System.currentTimeMillis(),
        source = "Sensor Telemetry"
    ),
    val bloodOxygen: BloodOxygenMetric = BloodOxygenMetric(
        spo2Percentage = null,
        truthState = MetricTruthState.DEVICE_REQUIRED,
        timestamp = null,
        source = null,
        statusMessage = "Requires compatible health sensor (Galaxy Watch / Health Connect)"
    ),
    val location: LocationContext = LocationContext(
        latitude = null,
        longitude = null,
        locality = null,
        truthState = MetricTruthState.PERMISSION_REQUIRED,
        timestamp = System.currentTimeMillis()
    ),
    val isOnline: Boolean = true,
    val lastRefreshTimestamp: Long = System.currentTimeMillis()
) {
    /**
     * Human-readable data freshness calculation.
     */
    fun getFreshnessString(timestamp: Long): String {
        val diffMs = System.currentTimeMillis() - timestamp
        val diffMins = (diffMs / 60000).coerceAtLeast(0)
        return when {
            diffMins < 1 -> "Updated just now"
            diffMins == 1L -> "Updated 1 min ago"
            diffMins < 60 -> "Updated $diffMins min ago"
            else -> "Updated ${diffMins / 60}h ago"
        }
    }

    /**
     * Determines whether environmental data is stale (older than 45 minutes).
     */
    val isWeatherStale: Boolean
        get() = weather != null && (System.currentTimeMillis() - weather.timestamp) > 45 * 60 * 1000

    val isAirQualityStale: Boolean
        get() = airQuality != null && (System.currentTimeMillis() - airQuality.timestamp) > 45 * 60 * 1000
}

data class LocationContext(
    val latitude: Double?,
    val longitude: Double?,
    val locality: String?,
    val truthState: MetricTruthState,
    val timestamp: Long
)
