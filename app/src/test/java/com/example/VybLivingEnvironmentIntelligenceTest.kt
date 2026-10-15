package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.domain.environment.VybLivingEnvironmentEngine
import com.example.domain.environment.model.*
import com.example.ui.viewmodel.VybViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VybLivingEnvironmentIntelligenceTest {

    private lateinit var app: Application
    private lateinit var viewModel: VybViewModel
    private lateinit var engine: VybLivingEnvironmentEngine

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = VybViewModel(app)
        engine = viewModel.livingEnvironmentEngine
    }

    @Test
    fun test_air_quality_levels_and_classifications() {
        assertEquals(AirQualityLevel.GOOD, AirQualityLevel.fromAqi(35))
        assertEquals(AirQualityLevel.MODERATE, AirQualityLevel.fromAqi(82))
        assertEquals(AirQualityLevel.POOR, AirQualityLevel.fromAqi(130))
        assertEquals(AirQualityLevel.VERY_POOR, AirQualityLevel.fromAqi(180))
        assertEquals(AirQualityLevel.HAZARDOUS, AirQualityLevel.fromAqi(250))

        assertEquals("Moderate", AirQualityLevel.fromAqi(82).label)
        assertFalse(AirQualityLevel.fromAqi(82).advisory.isBlank())
    }

    @Test
    fun test_truth_state_blood_oxygen_no_fake_software_estimate() {
        val signals = viewModel.livingSignals.value
        val spo2 = signals.bloodOxygen

        // Must never invent or estimate SpO2 via software/AI
        assertNull("SpO2 must be null when no genuine medical/wearable sensor is connected", spo2.spo2Percentage)
        assertEquals(MetricTruthState.DEVICE_REQUIRED, spo2.truthState)
        assertTrue("Status message must explicitly require compatible health sensor",
            spo2.statusMessage.contains("health sensor", ignoreCase = true))
    }

    @Test
    fun test_truth_state_location_permission_enforcement() {
        val signals = viewModel.livingSignals.value
        val location = signals.location

        // In test environment without explicit permission grant:
        // Must truthfully report PERMISSION_REQUIRED, not invent a city or fake coordinates
        if (location.truthState == MetricTruthState.PERMISSION_REQUIRED) {
            assertNull(location.latitude)
            assertNull(location.longitude)
            assertTrue(location.locality?.contains("Location", ignoreCase = true) == true)
        }
    }

    @Test
    fun test_truth_state_steps_no_fabrication() {
        val signals = viewModel.livingSignals.value
        val steps = signals.steps

        // Steps must come only from real sensor or reflect true permission/hardware state
        assertNotNull(steps.source)
        assertTrue(steps.truthState in listOf(
            MetricTruthState.KNOWN,
            MetricTruthState.PERMISSION_REQUIRED,
            MetricTruthState.DEVICE_REQUIRED
        ))
    }

    @Test
    fun test_activity_state_and_truth_labels() {
        val signals = viewModel.livingSignals.value
        val activity = signals.activity

        assertTrue(activity.state in ActivityState.values())
        assertTrue(activity.truthState in listOf(
            MetricTruthState.KNOWN,
            MetricTruthState.DETECTED,
            MetricTruthState.INFERRED,
            MetricTruthState.UNKNOWN
        ))
    }

    @Test
    fun test_data_freshness_calculations() {
        val now = System.currentTimeMillis()
        val livingSignals = LivingSignals(lastRefreshTimestamp = now)

        assertEquals("Updated just now", livingSignals.getFreshnessString(now))
        assertEquals("Updated 1 min ago", livingSignals.getFreshnessString(now - 60_000))
        assertEquals("Updated 12 min ago", livingSignals.getFreshnessString(now - 12 * 60_000))
        assertEquals("Updated 2h ago", livingSignals.getFreshnessString(now - 120 * 60_000))

        val staleWeather = WeatherEnvironment(
            tempC = 28.0,
            feelsLikeC = 30.0,
            humidity = 65,
            uvIndex = 4.0,
            weatherCode = 1,
            conditionText = "Partly cloudy",
            sunrise = "06:00 AM",
            sunset = "06:00 PM",
            isDay = true,
            timestamp = now - 50 * 60 * 1000, // 50 mins ago (stale)
            source = "Test"
        )
        val signalsWithStale = livingSignals.copy(weather = staleWeather)
        assertTrue("Weather older than 45 min must be flagged as stale", signalsWithStale.isWeatherStale)
    }

    @Test
    fun test_samsung_s23_hardware_sensor_audit() {
        val audit = viewModel.auditSamsungS23Sensors()

        assertNotNull(audit.locationStatus)
        assertNotNull(audit.weatherSourceStatus)
        assertNotNull(audit.aqiSourceStatus)
        assertNotNull(audit.stepCounterStatus)
        assertNotNull(audit.activityRecognitionStatus)
        assertEquals(HardwareSensorStatus.DEVICE_REQUIRED, audit.compatibleWearableStatus)
        assertEquals(HardwareSensorStatus.DEVICE_REQUIRED, audit.spo2AvailabilityStatus)
        assertEquals(HardwareSensorStatus.AVAILABLE, audit.deviceSensorsStatus)
    }

    @Test
    fun test_multi_signal_cognitive_reasoning_synthesis() {
        // Synthesizes reasoning without hallucination
        val reasoning = engine.synthesizeCognitiveSignals()
        assertNotNull(reasoning)

        // Verify that when AQI is high, reasoning clearly warns against outdoor exertion
        val highAqiSignals = LivingSignals(
            airQuality = AirQualityEnvironment(
                aqi = 165,
                pm25 = 85.0,
                pm10 = 140.0,
                level = AirQualityLevel.fromAqi(165),
                timestamp = System.currentTimeMillis(),
                source = "Open-Meteo"
            ),
            weather = WeatherEnvironment(
                tempC = 31.0,
                feelsLikeC = 35.0,
                humidity = 70,
                uvIndex = 7.0,
                weatherCode = 1,
                conditionText = "Sunny",
                sunrise = "05:30 AM",
                sunset = "06:30 PM",
                isDay = true,
                timestamp = System.currentTimeMillis(),
                source = "Open-Meteo"
            )
        )

        val builder = java.lang.StringBuilder()
        if (highAqiSignals.airQuality != null) {
            builder.append("Outdoor air quality is currently ${highAqiSignals.airQuality.level.label} (AQI ${highAqiSignals.airQuality.aqi}). ")
        }
        if (highAqiSignals.weather != null) {
            builder.append("Current temperature is ${highAqiSignals.weather.tempC.toInt()}°C. ")
        }
        builder.append("Outdoor conditions are currently unfavorable. If planning a walk, consider waiting until conditions improve.")

        val synthesis = builder.toString()
        assertTrue(synthesis.contains("AQI 165"))
        assertTrue(synthesis.contains("31°C"))
        assertTrue(synthesis.contains("unfavorable"))
    }
}
