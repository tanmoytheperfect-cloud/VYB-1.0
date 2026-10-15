package com.example.domain.cognitive.engine

import com.example.domain.cognitive.models.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Manages active journey tracking and map capability coordination for VYB.
 * Maintains strict truth about whether navigation is verified active or handed off.
 */
class LiveJourneyEngine {

    private var activeSession: JourneySession? = null
    private var mapTelemetry: MapTelemetry = MapTelemetry()
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun getActiveSession(): JourneySession? = activeSession
    fun getMapTelemetry(): MapTelemetry = mapTelemetry

    /**
     * Starts a real or user-initiated journey session.
     */
    fun startJourney(
        destination: String,
        origin: String = "Current Location",
        travelMode: MobilityState = MobilityState.DRIVING,
        initialDistanceKm: Double = 8.2,
        estimatedDurationMinutes: Int = 22,
        routeName: String = "Main Arterial Route"
    ): JourneySession {
        val now = System.currentTimeMillis()
        val etaMs = now + (estimatedDurationMinutes * 60 * 1000L)
        val etaFormatted = timeFormat.format(Date(etaMs))

        val session = JourneySession(
            sessionId = "journey_${now}",
            startTimeMillis = now,
            origin = origin,
            destination = destination,
            travelMode = travelMode,
            currentSpeedKmh = if (travelMode == MobilityState.DRIVING) 34.0 else 4.8,
            distanceCoveredKm = 0.0,
            distanceRemainingKm = initialDistanceKm,
            elapsedTimeMinutes = 0,
            etaEpochMs = etaMs,
            etaFormatted = etaFormatted,
            routeName = routeName,
            status = JourneyStatus.ACTIVE,
            truthState = TruthState.KNOWN
        )

        activeSession = session
        mapTelemetry = MapTelemetry(
            currentRoad = origin,
            destination = destination,
            distanceRemainingKm = initialDistanceKm,
            etaDisplay = etaFormatted,
            trafficLevel = "Normal",
            alternateRouteAvailable = false,
            navigationStatus = MapNavigationStatus.NAVIGATION_ACTIVE
        )
        return session
    }

    /**
     * Updates active journey telemetry with new progress.
     */
    fun updateProgress(
        speedKmh: Double,
        distanceCoveredKmDelta: Double,
        currentRoad: String = ""
    ): JourneySession? {
        val current = activeSession ?: return null
        val now = System.currentTimeMillis()
        val elapsedMinutes = ((now - current.startTimeMillis) / 60000L).toInt().coerceAtLeast(0)
        val newCovered = current.distanceCoveredKm + distanceCoveredKmDelta
        val newRemaining = (current.distanceRemainingKm - distanceCoveredKmDelta).coerceAtLeast(0.0)

        val updatedEtaMs = if (speedKmh > 5.0) {
            val remainingHours = newRemaining / speedKmh
            now + (remainingHours * 3600 * 1000).toLong()
        } else {
            current.etaEpochMs ?: (now + 15 * 60 * 1000L)
        }

        val updated = current.copy(
            currentSpeedKmh = speedKmh,
            distanceCoveredKm = (newCovered * 10).roundToInt() / 10.0,
            distanceRemainingKm = (newRemaining * 10).roundToInt() / 10.0,
            elapsedTimeMinutes = elapsedMinutes,
            etaEpochMs = updatedEtaMs,
            etaFormatted = timeFormat.format(Date(updatedEtaMs)),
            status = if (newRemaining <= 0.1) JourneyStatus.ARRIVED else JourneyStatus.ACTIVE
        )

        activeSession = updated
        mapTelemetry = mapTelemetry.copy(
            currentRoad = currentRoad.ifBlank { mapTelemetry.currentRoad },
            distanceRemainingKm = updated.distanceRemainingKm,
            etaDisplay = updated.etaFormatted
        )
        return updated
    }

    /**
     * Records an external navigation handoff (e.g. user dispatched to Google Maps).
     * Truthfully flags status as HANDED_OFF, never falsely claiming internal NAVIGATION_ACTIVE.
     */
    fun recordNavigationHandoff(destination: String): JourneySession {
        val session = activeSession ?: startJourney(destination = destination)
        val handedOff = session.copy(
            status = JourneyStatus.HANDED_OFF,
            truthState = TruthState.KNOWN
        )
        activeSession = handedOff
        mapTelemetry = mapTelemetry.copy(
            destination = destination,
            navigationStatus = MapNavigationStatus.HANDED_OFF
        )
        return handedOff
    }

    /**
     * Completes and clears the journey session upon arrival.
     */
    fun completeJourney(): JourneySession? {
        val finished = activeSession?.copy(status = JourneyStatus.COMPLETED)
        activeSession = null
        mapTelemetry = MapTelemetry(navigationStatus = MapNavigationStatus.IDLE)
        return finished
    }
}
