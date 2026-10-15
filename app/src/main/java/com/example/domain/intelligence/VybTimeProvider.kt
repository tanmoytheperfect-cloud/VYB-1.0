package com.example.domain.intelligence

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * VybTimeProvider:
 * Truth-first time provider reading from the real Android runtime and device system clock (Part 30).
 * Never hardcodes current date or allows AI hallucination of current time.
 */
object VybTimeProvider {

    fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    fun getCurrentDate(): Date = Date()

    fun getTimeZone(): TimeZone = TimeZone.getDefault()

    fun getCurrentDateString(pattern: String = "yyyy-MM-dd"): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(getCurrentDate())

    fun getCurrentTimeString(pattern: String = "hh:mm a"): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(getCurrentDate())

    fun getFormattedDateTimeString(): String =
        SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' hh:mm a (z)", Locale.getDefault()).format(getCurrentDate())

    fun getDayOfWeek(): String =
        SimpleDateFormat("EEEE", Locale.getDefault()).format(getCurrentDate())

    fun getCalendar(): Calendar = Calendar.getInstance(getTimeZone())
}
