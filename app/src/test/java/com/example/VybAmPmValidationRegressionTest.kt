package com.example

import com.example.domain.cognitive.engine.TemporalReasoningEngine
import com.example.domain.cognitive.models.CandidatePattern
import com.example.domain.cognitive.models.PatternStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * AM/PM VALIDATION REGRESSION TEST SUITE (Requirement 3)
 *
 * A source time of 10:30 AM must never become 10:30 PM because of:
 * - timezone conversion
 * - LocalDateTime conversion
 * - Instant conversion
 * - formatter
 * - 12-hour/24-hour conversion
 * - database serialization
 *
 * Tests specifically for:
 * - 10:30 AM
 * - 10:30 PM
 * - 00:30
 * - 12:00 PM
 * - 12:00 AM
 * across multiple timezones (UTC, Asia/Kolkata, America/New_York, Europe/London, Asia/Tokyo).
 */
class VybAmPmValidationRegressionTest {

    private val temporalEngine = TemporalReasoningEngine()
    private val testTimezones = listOf(
        TimeZone.getTimeZone("UTC"),
        TimeZone.getTimeZone("Asia/Kolkata"),
        TimeZone.getTimeZone("America/New_York"),
        TimeZone.getTimeZone("Europe/London"),
        TimeZone.getTimeZone("Asia/Tokyo")
    )

    @Test
    fun test_10_30_AM_never_becomes_PM() {
        for (tz in testTimezones) {
            val parsed = temporalEngine.parseTimeOfDay("10:30 AM", tz)
            assertEquals("10:30 AM must have hour24 = 10 in tz ${tz.id}", 10, parsed.hour24)
            assertEquals("10:30 AM must have minute = 30 in tz ${tz.id}", 30, parsed.minute)
            assertEquals("10:30 AM formatted display must be '10:30 AM' in tz ${tz.id}", "10:30 AM", parsed.formatted)
        }
    }

    @Test
    fun test_10_30_PM_remains_PM() {
        for (tz in testTimezones) {
            val parsed = temporalEngine.parseTimeOfDay("10:30 PM", tz)
            assertEquals("10:30 PM must have hour24 = 22 in tz ${tz.id}", 22, parsed.hour24)
            assertEquals("10:30 PM must have minute = 30 in tz ${tz.id}", 30, parsed.minute)
            assertEquals("10:30 PM formatted display must be '10:30 PM' in tz ${tz.id}", "10:30 PM", parsed.formatted)
        }
    }

    @Test
    fun test_00_30_is_12_30_AM() {
        for (tz in testTimezones) {
            val parsed = temporalEngine.parseTimeOfDay("00:30", tz)
            assertEquals("00:30 must have hour24 = 0 in tz ${tz.id}", 0, parsed.hour24)
            assertEquals("00:30 must have minute = 30 in tz ${tz.id}", 30, parsed.minute)
            assertEquals("00:30 formatted display must be '12:30 AM' in tz ${tz.id}", "12:30 AM", parsed.formatted)
        }
    }

    @Test
    fun test_12_00_PM_is_Noon() {
        for (tz in testTimezones) {
            val parsed = temporalEngine.parseTimeOfDay("12:00 PM", tz)
            assertEquals("12:00 PM must have hour24 = 12 in tz ${tz.id}", 12, parsed.hour24)
            assertEquals("12:00 PM must have minute = 0 in tz ${tz.id}", 0, parsed.minute)
            assertEquals("12:00 PM formatted display must be '12:00 PM' in tz ${tz.id}", "12:00 PM", parsed.formatted)
        }
    }

    @Test
    fun test_12_00_AM_is_Midnight() {
        for (tz in testTimezones) {
            val parsed = temporalEngine.parseTimeOfDay("12:00 AM", tz)
            assertEquals("12:00 AM must have hour24 = 0 in tz ${tz.id}", 0, parsed.hour24)
            assertEquals("12:00 AM must have minute = 0 in tz ${tz.id}", 0, parsed.minute)
            assertEquals("12:00 AM formatted display must be '12:00 AM' in tz ${tz.id}", "12:00 AM", parsed.formatted)
        }
    }

    @Test
    fun test_24_hour_format_roundtrips() {
        // 22:30 is 10:30 PM
        val p2230 = temporalEngine.parseTimeOfDay("22:30")
        assertEquals(22, p2230.hour24)
        assertEquals(30, p2230.minute)
        assertEquals("10:30 PM", p2230.formatted)

        // 10:30 without suffix is 10:30 AM
        val p1030 = temporalEngine.parseTimeOfDay("10:30")
        assertEquals(10, p1030.hour24)
        assertEquals(30, p1030.minute)
        assertEquals("10:30 AM", p1030.formatted)
    }

    private fun formatMinuteOfDay(totalMinutes: Int): String {
        val m = (totalMinutes % 1440 + 1440) % 1440
        val hour24 = m / 60
        val minute = m % 60
        val hour12 = if (hour24 == 0) 12 else if (hour24 > 12) hour24 - 12 else hour24
        val amPm = if (hour24 < 12) "AM" else "PM"
        return String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
    }

    @Test
    fun test_minute_formatting_never_converts_am_to_pm() {
        // 10:30 AM is 10 * 60 + 30 = 630
        val str1030AM = formatMinuteOfDay(630)
        assertEquals("10:30 AM", str1030AM)

        // 10:30 PM is 22 * 60 + 30 = 1350
        val str1030PM = formatMinuteOfDay(1350)
        assertEquals("10:30 PM", str1030PM)

        // 00:30 is 30
        val str0030 = formatMinuteOfDay(30)
        assertEquals("12:30 AM", str0030)

        // 12:00 PM is 720
        val str1200PM = formatMinuteOfDay(720)
        assertEquals("12:00 PM", str1200PM)

        // 12:00 AM is 0
        val str1200AM = formatMinuteOfDay(0)
        assertEquals("12:00 AM", str1200AM)
    }

    @Test
    fun test_temporal_reasoning_next_up_weekday_school_on_saturday() {
        val calSaturday = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata")).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }

        val schoolPattern = CandidatePattern(
            patternId = "pat_adhyansh_school",
            subject = "Adhyansh",
            domain = "School",
            title = "Adhyansh's School Routine",
            description = "Adhyansh school routine on weekdays at 08:30 AM",
            expectedDays = listOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            ),
            expectedTimeRange = "08:30 AM",
            recurrence = "WEEKDAY",
            status = PatternStatus.ESTABLISHED,
            confidence = 0.95f
        )

        val nextUp = temporalEngine.resolveNextUp(
            nowMillis = calSaturday.timeInMillis,
            timezoneId = "Asia/Kolkata",
            learnedPatterns = listOf(schoolPattern),
            routines = emptyList(),
            intentions = emptyList(),
            canonicalEnvironment = null
        )

        assertNotNull("Must resolve next up candidate", nextUp)
        assertFalse("On Saturday, weekday school must NOT be marked as today", nextUp!!.isToday)
        assertTrue("Subtitle must indicate next expected day (Mon)", nextUp.subtitle.contains("Mon", ignoreCase = true))
        assertEquals("8:30 AM", nextUp.timeDisplay)
    }
}
