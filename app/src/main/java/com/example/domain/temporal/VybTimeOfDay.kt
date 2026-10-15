package com.example.domain.temporal

import java.util.Calendar

/**
 * High-fidelity time-aware environment state for VYB.
 * Tracks the real local celestial period throughout the owner's day:
 * DAWN: 05:00–06:59
 * MORNING: 07:00–11:59
 * DAY: 12:00–16:59
 * EVENING: 17:00–19:59
 * NIGHT: 20:00–04:59
 */
enum class VybTimeOfDay(
    val displayName: String,
    val greetingPrefix: String,
    val isSunEnvironment: Boolean,
    val solarIntensity: Float,
    val lunarIntensity: Float,
    val starsIntensity: Float,
    val warmthFactor: Float,
    val statusTag: String,
    val contextualSummary: String
) {
    DAWN(
        displayName = "Dawn",
        greetingPrefix = "Good morning",
        isSunEnvironment = true,
        solarIntensity = 0.65f,
        lunarIntensity = 0.15f,
        starsIntensity = 0.25f,
        warmthFactor = 0.75f,
        statusTag = "DAWN AWAKENING",
        contextualSummary = "First light • Systems awakening • Morning routine preparing"
    ),
    MORNING(
        displayName = "Morning",
        greetingPrefix = "Good morning",
        isSunEnvironment = true,
        solarIntensity = 1.0f,
        lunarIntensity = 0.0f,
        starsIntensity = 0.0f,
        warmthFactor = 0.50f,
        statusTag = "MORNING ACTIVE",
        contextualSummary = "Fresh solar atmosphere • Active life tracking • Prime focus window"
    ),
    DAY(
        displayName = "Day",
        greetingPrefix = "Good afternoon",
        isSunEnvironment = true,
        solarIntensity = 1.0f,
        lunarIntensity = 0.0f,
        starsIntensity = 0.0f,
        warmthFactor = 0.35f,
        statusTag = "MIDDAY OBSERVING",
        contextualSummary = "Luminous solar presence • Quiet observation • Cognitive flow active"
    ),
    EVENING(
        displayName = "Evening",
        greetingPrefix = "Good evening",
        isSunEnvironment = false,
        solarIntensity = 0.40f,
        lunarIntensity = 0.45f,
        starsIntensity = 0.45f,
        warmthFactor = 0.85f,
        statusTag = "EVENING TWILIGHT",
        contextualSummary = "Sunset transition • Day wind-down • Reflection and calm"
    ),
    NIGHT(
        displayName = "Night",
        greetingPrefix = "Good night",
        isSunEnvironment = false,
        solarIntensity = 0.0f,
        lunarIntensity = 1.0f,
        starsIntensity = 1.0f,
        warmthFactor = 0.0f,
        statusTag = "NIGHT RESTFUL",
        contextualSummary = "Moonlit atmosphere • Deep restful state • Quiet background guard"
    );

    companion object {
        /**
         * Resolves the environment state from hour and minute in the device's local timezone.
         */
        fun fromHour(hour: Int): VybTimeOfDay {
            return when (hour) {
                in 5..6 -> DAWN
                in 7..11 -> MORNING
                in 12..16 -> DAY
                in 17..19 -> EVENING
                else -> NIGHT
            }
        }

        /**
         * Resolves the real-world VybTimeOfDay from the device's current Calendar instance.
         */
        fun fromCalendar(calendar: Calendar = Calendar.getInstance()): VybTimeOfDay {
            return fromHour(calendar.get(Calendar.HOUR_OF_DAY))
        }

        /**
         * Resolves the environment state from epoch timestamp.
         */
        fun fromMillis(millis: Long = System.currentTimeMillis()): VybTimeOfDay {
            val cal = Calendar.getInstance().apply { timeInMillis = millis }
            return fromCalendar(cal)
        }
    }
}
