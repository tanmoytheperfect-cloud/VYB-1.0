package com.example.domain.cognitive.engine

import com.example.domain.cognitive.models.MobilityState
import com.example.domain.cognitive.models.TruthState
import com.example.domain.environment.model.ActivityState

data class MobilityDetermination(
    val state: MobilityState,
    val truthState: TruthState,
    val confidence: Float,
    val speedKmh: Double,
    val rationale: String
)

/**
 * Mobility Brain combining GPS speed, accelerometer dynamics, hardware step detector,
 * and Android activity recognition into honest, provenance-tracked mobility states.
 */
class MobilityBrain {

    fun determineMobility(
        speedKmh: Double? = null,
        accelVariance: Double = 0.0,
        isStepCounterActive: Boolean = false,
        recentStepCadencePerMinute: Int = 0,
        activityRecognition: ActivityState? = null,
        isNearRoadOrCommuteRoute: Boolean = false
    ): MobilityDetermination {
        val speed = speedKmh ?: 0.0
        val hasGpsSpeed = speedKmh != null && speedKmh >= 0.0

        // 1. High-speed road transit -> DRIVING
        if (hasGpsSpeed && speed >= 22.0) {
            val isKnownSpeed = speed > 25.0
            return MobilityDetermination(
                state = MobilityState.DRIVING,
                truthState = if (isKnownSpeed) TruthState.DETECTED else TruthState.INFERRED,
                confidence = if (speed >= 30.0) 0.95f else 0.80f,
                speedKmh = speed,
                rationale = "Vehicle transit detected via GPS speed of ${speed.toInt()} km/h"
            )
        }

        // 2. Cycling velocity window (13 to 22 km/h) with low step cadence
        if (hasGpsSpeed && speed in 13.0..22.0 && recentStepCadencePerMinute < 60) {
            return MobilityDetermination(
                state = MobilityState.CYCLING,
                truthState = TruthState.INFERRED,
                confidence = 0.75f,
                speedKmh = speed,
                rationale = "Cycling inferred from speed of ${speed.toInt()} km/h with low step cadence"
            )
        }

        // 3. Running cadence and velocity (8 to 16 km/h or high cadence > 140)
        if ((hasGpsSpeed && speed in 8.0..16.0 && recentStepCadencePerMinute > 120) ||
            (recentStepCadencePerMinute >= 145 && accelVariance > 4.0)) {
            return MobilityDetermination(
                state = MobilityState.RUNNING,
                truthState = TruthState.DETECTED,
                confidence = 0.88f,
                speedKmh = if (speed > 0) speed else 9.5,
                rationale = "Running detected via high step cadence ($recentStepCadencePerMinute spm) and movement dynamics"
            )
        }

        // 4. Walking: Step detector pulses + cadence or pedestrian speed
        if (isStepCounterActive && (recentStepCadencePerMinute > 30 || activityRecognition == ActivityState.WALKING)) {
            val pedestrianSpeed = if (speed in 1.5..7.5) speed else 4.8
            return MobilityDetermination(
                state = MobilityState.WALKING,
                truthState = if (activityRecognition == ActivityState.WALKING || recentStepCadencePerMinute > 50) TruthState.KNOWN else TruthState.DETECTED,
                confidence = 0.92f,
                speedKmh = pedestrianSpeed,
                rationale = "Active walking detected from hardware step cadence ($recentStepCadencePerMinute spm)"
            )
        }

        // Pedestrian speed without step counter
        if (hasGpsSpeed && speed in 2.0..7.0 && accelVariance > 0.8) {
            return MobilityDetermination(
                state = MobilityState.WALKING,
                truthState = TruthState.DETECTED,
                confidence = 0.85f,
                speedKmh = speed,
                rationale = "Walking movement detected via ground speed ($speed km/h) and acceleration variance"
            )
        }

        // 5. Stationary
        if ((hasGpsSpeed && speed < 1.5 && accelVariance < 0.5) ||
            (!hasGpsSpeed && accelVariance < 0.3 && recentStepCadencePerMinute == 0)) {
            return MobilityDetermination(
                state = MobilityState.STATIONARY,
                truthState = TruthState.DETECTED,
                confidence = 0.95f,
                speedKmh = 0.0,
                rationale = "Stationary state verified by zero ground speed and resting accelerometer telemetry"
            )
        }

        // 6. Fallback based on Activity Recognition if provided
        return when (activityRecognition) {
            ActivityState.WALKING -> MobilityDetermination(
                state = MobilityState.WALKING,
                truthState = TruthState.DETECTED,
                confidence = 0.80f,
                speedKmh = 4.5,
                rationale = "Walking reported by Android activity recognition"
            )
            ActivityState.RUNNING -> MobilityDetermination(
                state = MobilityState.RUNNING,
                truthState = TruthState.DETECTED,
                confidence = 0.85f,
                speedKmh = 9.0,
                rationale = "Running reported by Android activity recognition"
            )
            ActivityState.CYCLING -> MobilityDetermination(
                state = MobilityState.CYCLING,
                truthState = TruthState.DETECTED,
                confidence = 0.80f,
                speedKmh = 16.0,
                rationale = "Cycling reported by Android activity recognition"
            )
            ActivityState.DRIVING -> MobilityDetermination(
                state = MobilityState.DRIVING,
                truthState = TruthState.DETECTED,
                confidence = 0.85f,
                speedKmh = 35.0,
                rationale = "Automotive transit reported by Android activity recognition"
            )
            ActivityState.STATIONARY -> MobilityDetermination(
                state = MobilityState.STATIONARY,
                truthState = TruthState.DETECTED,
                confidence = 0.90f,
                speedKmh = 0.0,
                rationale = "Stationary posture confirmed"
            )
            else -> MobilityDetermination(
                state = MobilityState.UNKNOWN,
                truthState = TruthState.UNKNOWN,
                confidence = 0.20f,
                speedKmh = speed,
                rationale = "Insufficient sensor telemetry to classify mobility posture"
            )
        }
    }
}
