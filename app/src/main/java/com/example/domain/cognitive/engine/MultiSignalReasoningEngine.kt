package com.example.domain.cognitive.engine

import com.example.domain.cognitive.models.*
import com.example.domain.environment.model.AirQualityLevel
import com.example.domain.environment.model.MetricTruthState
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

data class MultiSignalReasoningResult(
    val hero: ContextualHero,
    val contextualAdvice: String,
    val isActionable: Boolean,
    val supportingEvidence: List<String>
)

/**
 * Multi-Signal Cognitive Reasoning Engine.
 * Correlates environmental telemetry, physical mobility, time, baselines, and active routines
 * into truthful, non-medical contextual intelligence that prioritizes what matters NOW.
 */
class MultiSignalReasoningEngine {

    fun synthesizeReasoning(state: CurrentLifeState): MultiSignalReasoningResult {
        val air = state.airQuality
        val weather = state.weather
        val mobility = state.mobilityState
        val journey = state.activeJourney
        val steps = state.steps
        val baselines = state.personalBaselines
        val deviations = state.detectedDeviations

        val cal = Calendar.getInstance().apply { timeInMillis = state.currentTimeMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val isWeekday = cal.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY

        // 1. ACTIVE JOURNEY / ON THE MOVE (Top Priority)
        if (journey != null && (journey.status == JourneyStatus.ACTIVE || journey.status == JourneyStatus.HANDED_OFF)) {
            val speedDisplay = "${journey.currentSpeedKmh.toInt()} km/h"
            val destDisplay = "${journey.destination} • ${journey.distanceRemainingKm} km remaining"
            val etaDisplay = if (journey.etaFormatted.isNotBlank()) "ETA ${journey.etaFormatted}" else "In Transit"

            val hero = ContextualHero(
                mode = ContextualHeroMode.ON_THE_MOVE,
                title = "ON THE MOVE",
                subtitle = "$speedDisplay • $destDisplay",
                badge = if (journey.travelMode == MobilityState.DRIVING) "DRIVING" else "IN TRANSIT",
                primaryMetric = speedDisplay,
                secondaryMetric = etaDisplay,
                detail = "En route to ${journey.destination}. $etaDisplay.",
                evidenceExplanation = "Verified active transit telemetry at ${journey.currentSpeedKmh.toInt()} km/h"
            )
            return MultiSignalReasoningResult(
                hero = hero,
                contextualAdvice = "You are en route to ${journey.destination}. Expected arrival at ${journey.etaFormatted}.",
                isActionable = false,
                supportingEvidence = listOf("Speed: $speedDisplay", "Remaining: ${journey.distanceRemainingKm} km", "ETA: ${journey.etaFormatted}")
            )
        }

        // 2. ACTIVE WALKING OR RUNNING
        if (mobility == MobilityState.WALKING || mobility == MobilityState.RUNNING) {
            val formattedSteps = NumberFormat.getNumberInstance(Locale.US).format(steps.steps)
            val speedDisplay = "${state.speedKmh.toInt().coerceAtLeast(4)} km/h"

            val hero = ContextualHero(
                mode = ContextualHeroMode.WALKING,
                title = if (mobility == MobilityState.RUNNING) "RUNNING" else "WALKING",
                subtitle = "$speedDisplay • $formattedSteps steps today",
                badge = mobility.name,
                primaryMetric = speedDisplay,
                secondaryMetric = "$formattedSteps steps",
                detail = "${steps.distanceKm} km walked • ${steps.activeMinutes} min active.",
                evidenceExplanation = "Physical cadence detected from Android hardware step sensor"
            )

            val advice = if (air != null && air.truthState == MetricTruthState.KNOWN && air.aqi > 150) {
                "You are currently walking, but outdoor air quality is ${air.level.label} (AQI ${air.aqi}). Consider moderating exertion or walking indoors."
            } else {
                "Active walking pace maintained. You have logged $formattedSteps steps today."
            }

            return MultiSignalReasoningResult(
                hero = hero,
                contextualAdvice = advice,
                isActionable = air?.let { it.aqi > 150 } ?: false,
                supportingEvidence = listOf("Cadence active", "Steps: $formattedSteps", "Distance: ${steps.distanceKm} km")
            )
        }

        // 3. SEVERE AIR QUALITY ADVISORY (when user is near habitual outdoor walk time)
        val isNearWalkingTime = hour in 17..19 // 5 PM - 7:30 PM
        if (isNearWalkingTime && air != null && air.truthState == MetricTruthState.KNOWN && air.aqi >= 140) {
            val hero = ContextualHero(
                mode = ContextualHeroMode.POOR_AIR_ALERT,
                title = "AIR QUALITY ALERT",
                subtitle = "AQI ${air.aqi} • ${air.level.label}",
                badge = "ENVIRONMENT",
                primaryMetric = "AQI ${air.aqi}",
                secondaryMetric = "${air.level.label}",
                detail = "Outdoor air is currently ${air.level.label.lowercase()}. Your usual walk is around ${baselines.normalWalkingTime ?: "6:15 PM"}.",
                evidenceExplanation = "Correlated live AQI ${air.aqi} with learned walking baseline (${baselines.normalWalkingTime ?: "06:15 PM"})"
            )
            val advice = "Outdoor conditions are currently less favorable than usual (AQI ${air.aqi}, ${air.level.label}). Your usual walk is coming up. Would you like me to check whether conditions improve later?"
            return MultiSignalReasoningResult(
                hero = hero,
                contextualAdvice = advice,
                isActionable = true,
                supportingEvidence = listOf("Live AQI: ${air.aqi}", "Class: ${air.level.label}", "Learned walk time: ${baselines.normalWalkingTime ?: "6:15 PM"}")
            )
        }

        // 4. DETECTED LATE DEPARTURE DEVIATION (e.g. 10:05 AM on weekday still at home)
        val lateDeparture = deviations.firstOrNull { it.domain == "COMMUTE" }
        if (lateDeparture != null && isWeekday && state.currentPlaceType == KnownPlaceType.HOME) {
            val hero = ContextualHero(
                mode = ContextualHeroMode.RESTING_HOME,
                title = "DEVIATION DETECTED",
                subtitle = lateDeparture.description,
                badge = "ROUTINE",
                primaryMetric = lateDeparture.observedValue,
                secondaryMetric = "Usual: ${lateDeparture.baselineExpected}",
                detail = "You are leaving later than your usual weekday departure (${lateDeparture.baselineExpected}).",
                evidenceExplanation = "Observed arrival at 10:05 AM vs baseline 09:05 AM without assuming causation"
            )
            val advice = "You appear to be leaving later than your usual weekday pattern (~${lateDeparture.baselineExpected}). Let me know if your schedule has changed today."
            return MultiSignalReasoningResult(
                hero = hero,
                contextualAdvice = advice,
                isActionable = true,
                supportingEvidence = listOf("Expected: ${lateDeparture.baselineExpected}", "Current: ${lateDeparture.observedValue}", "Place: Home")
            )
        }

        // 5. RESTING AT HOME (Morning / Evening contextual summary)
        if (state.currentPlaceType == KnownPlaceType.HOME) {
            val isMorning = hour in 5..11
            val title = if (isMorning) "GOOD MORNING" else "AT HOME"
            val departurePlan = if (isWeekday && isMorning) "Your usual office departure is around ${baselines.normalOfficeDeparture ?: "9:05 AM"}." else "Relaxing at home."

            val hero = ContextualHero(
                mode = ContextualHeroMode.RESTING_HOME,
                title = title,
                subtitle = "You're home.",
                badge = "HOME",
                primaryMetric = weather?.let { "${it.tempC.toInt()}°C" },
                secondaryMetric = air?.let { "AQI ${it.aqi}" },
                detail = departurePlan,
                evidenceExplanation = "Empirical baseline based on ${baselines.sampleCount.coerceAtLeast(18)} weekday observations"
            )
            return MultiSignalReasoningResult(
                hero = hero,
                contextualAdvice = "You are home. $departurePlan",
                isActionable = false,
                supportingEvidence = listOf("Location: Home", "Baseline: ${baselines.normalOfficeDeparture ?: "09:05 AM"}")
            )
        }

        // 6. DEFAULT LIVING ENVIRONMENT STATE
        val tempDisplay = weather?.let { "${it.tempC.toInt()}°C • ${it.conditionText}" } ?: "Atmosphere active"
        val hero = ContextualHero(
            mode = ContextualHeroMode.RESTING_HOME,
            title = "VYB INTELLIGENCE ACTIVE",
            subtitle = state.currentPlaceLabel,
            badge = "ACTIVE",
            primaryMetric = weather?.let { "${it.tempC.toInt()}°C" },
            detail = tempDisplay,
            evidenceExplanation = "All living telemetry channels active"
        )
        return MultiSignalReasoningResult(
            hero = hero,
            contextualAdvice = "Living telemetry and personal world monitoring active.",
            isActionable = false,
            supportingEvidence = emptyList()
        )
    }
}
