package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.domain.temporal.VybTimeOfDay
import com.example.ui.components.VybVisualState
import com.example.ui.viewmodel.VybViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VybDynamicDayNightEnvironmentTest {

    private lateinit var app: Application
    private lateinit var viewModel: VybViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = VybViewModel(app)
    }

    @Test
    fun test_all_24_hours_map_to_correct_celestial_ranges() {
        // DAWN: 05:00 - 06:59
        assertEquals(VybTimeOfDay.DAWN, VybTimeOfDay.fromHour(5))
        assertEquals(VybTimeOfDay.DAWN, VybTimeOfDay.fromHour(6))

        // MORNING: 07:00 - 11:59
        for (h in 7..11) {
            assertEquals("Hour $h should be MORNING", VybTimeOfDay.MORNING, VybTimeOfDay.fromHour(h))
        }

        // DAY: 12:00 - 16:59
        for (h in 12..16) {
            assertEquals("Hour $h should be DAY", VybTimeOfDay.DAY, VybTimeOfDay.fromHour(h))
        }

        // EVENING: 17:00 - 19:59
        for (h in 17..19) {
            assertEquals("Hour $h should be EVENING", VybTimeOfDay.EVENING, VybTimeOfDay.fromHour(h))
        }

        // NIGHT: 20:00 - 04:59
        for (h in listOf(20, 21, 22, 23, 0, 1, 2, 3, 4)) {
            assertEquals("Hour $h should be NIGHT", VybTimeOfDay.NIGHT, VybTimeOfDay.fromHour(h))
        }
    }

    @Test
    fun test_morning_and_day_sun_environment() {
        val morning = VybTimeOfDay.MORNING
        assertTrue("Morning must be sun environment", morning.isSunEnvironment)
        assertEquals(1.0f, morning.solarIntensity, 0.001f)
        assertEquals(0.0f, morning.lunarIntensity, 0.001f)
        assertEquals(0.0f, morning.starsIntensity, 0.001f)
        assertEquals("Good morning", morning.greetingPrefix)

        val day = VybTimeOfDay.DAY
        assertTrue("Day must be sun environment", day.isSunEnvironment)
        assertEquals(1.0f, day.solarIntensity, 0.001f)
        assertEquals(0.0f, day.lunarIntensity, 0.001f)
        assertEquals("Good afternoon", day.greetingPrefix)
    }

    @Test
    fun test_evening_sunset_transition() {
        val evening = VybTimeOfDay.EVENING
        assertFalse("Evening transitions out of pure day sun", evening.isSunEnvironment)
        assertTrue("Evening retains softened sun rays", evening.solarIntensity > 0f)
        assertTrue("Evening introduces emerging moon", evening.lunarIntensity > 0f)
        assertTrue("Evening awakens first stars", evening.starsIntensity > 0f)
        assertTrue("Evening has warm sunset factor", evening.warmthFactor >= 0.8f)
        assertEquals("Good evening", evening.greetingPrefix)
    }

    @Test
    fun test_night_moon_and_stars_environment() {
        val night = VybTimeOfDay.NIGHT
        assertFalse("Night must not be sun environment", night.isSunEnvironment)
        assertEquals(0.0f, night.solarIntensity, 0.001f)
        assertEquals(1.0f, night.lunarIntensity, 0.001f)
        assertEquals(1.0f, night.starsIntensity, 0.001f)
        assertEquals("Good night", night.greetingPrefix)
    }

    @Test
    fun test_dawn_awakening_environment() {
        val dawn = VybTimeOfDay.DAWN
        assertTrue("Dawn includes emerging sun", dawn.isSunEnvironment)
        assertTrue("Dawn has solar presence", dawn.solarIntensity > 0.5f)
        assertTrue("Dawn has fading lunar aura", dawn.lunarIntensity > 0f)
        assertEquals("Good morning", dawn.greetingPrefix)
    }

    @Test
    fun test_greetings_align_dynamically() {
        assertEquals("Good morning", VybTimeOfDay.DAWN.greetingPrefix)
        assertEquals("Good morning", VybTimeOfDay.MORNING.greetingPrefix)
        assertEquals("Good afternoon", VybTimeOfDay.DAY.greetingPrefix)
        assertEquals("Good evening", VybTimeOfDay.EVENING.greetingPrefix)
        assertEquals("Good night", VybTimeOfDay.NIGHT.greetingPrefix)
    }

    @Test
    fun test_viewmodel_time_override_and_cycling() {
        // Initially null (auto device local time)
        assertNull(viewModel.timeOfDayOverride.value)

        // Cycle through all states
        viewModel.cycleTimeOfDayOverride()
        assertEquals(VybTimeOfDay.DAWN, viewModel.timeOfDayOverride.value)

        viewModel.cycleTimeOfDayOverride()
        assertEquals(VybTimeOfDay.MORNING, viewModel.timeOfDayOverride.value)

        viewModel.cycleTimeOfDayOverride()
        assertEquals(VybTimeOfDay.DAY, viewModel.timeOfDayOverride.value)

        viewModel.cycleTimeOfDayOverride()
        assertEquals(VybTimeOfDay.EVENING, viewModel.timeOfDayOverride.value)

        viewModel.cycleTimeOfDayOverride()
        assertEquals(VybTimeOfDay.NIGHT, viewModel.timeOfDayOverride.value)

        viewModel.cycleTimeOfDayOverride()
        assertNull("Cycling after NIGHT returns to null (auto real time)", viewModel.timeOfDayOverride.value)

        // Explicit set
        viewModel.setTimeOfDayOverride(VybTimeOfDay.DAY)
        assertEquals(VybTimeOfDay.DAY, viewModel.timeOfDayOverride.value)
    }

    @Test
    fun test_real_calendar_resolution() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 8)
        assertEquals(VybTimeOfDay.MORNING, VybTimeOfDay.fromCalendar(cal))

        cal.set(Calendar.HOUR_OF_DAY, 14)
        assertEquals(VybTimeOfDay.DAY, VybTimeOfDay.fromCalendar(cal))

        cal.set(Calendar.HOUR_OF_DAY, 18)
        assertEquals(VybTimeOfDay.EVENING, VybTimeOfDay.fromCalendar(cal))

        cal.set(Calendar.HOUR_OF_DAY, 23)
        assertEquals(VybTimeOfDay.NIGHT, VybTimeOfDay.fromCalendar(cal))

        cal.set(Calendar.HOUR_OF_DAY, 6)
        assertEquals(VybTimeOfDay.DAWN, VybTimeOfDay.fromCalendar(cal))
    }
}
