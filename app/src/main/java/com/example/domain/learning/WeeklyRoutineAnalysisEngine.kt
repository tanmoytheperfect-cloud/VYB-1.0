package com.example.domain.learning

import com.example.data.local.dao.DailyReflectionDao
import com.example.data.local.dao.WeeklyPatternDao
import com.example.data.local.entity.WeeklyPatternEntity
import java.text.SimpleDateFormat
import java.util.*

class WeeklyRoutineAnalysisEngine(
    private val dailyReflectionDao: DailyReflectionDao,
    private val weeklyPatternDao: WeeklyPatternDao
) {

    suspend fun performWeeklyAnalysis(): WeeklyPatternEntity {
        val reflections = dailyReflectionDao.getRecentReflections(14)
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        val weekId = "$year-W$weekNumber"

        if (reflections.isEmpty()) {
            val emptyPattern = WeeklyPatternEntity(
                weekIdentifier = weekId,
                startDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now - 7 * 86400000L)),
                endDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now)),
                stableRoutinesSummary = "Insufficient evidence.",
                changingRoutinesSummary = "Insufficient evidence.",
                weekdayPatternSummary = "Insufficient evidence.",
                saturdayPatternSummary = "Insufficient evidence.",
                sundayPatternSummary = "Insufficient evidence.",
                unusualDaysSummary = "Insufficient evidence.",
                brokenRoutinesSummary = "Insufficient evidence.",
                weeklyInsightText = "Insufficient evidence.",
                timestamp = now
            )
            weeklyPatternDao.insertPattern(emptyPattern)
            return emptyPattern
        }

        val weekdayReflections = reflections.filter { it.dayType == "WORKDAY" }
        val weekendReflections = reflections.filter { it.dayType == "WEEKEND" }

        val weekdayAvgUnlock = weekdayReflections.mapNotNull { it.firstUnlockTime }.takeIf { it.isNotEmpty() }?.joinToString(", ")
        val weekendAvgUnlock = weekendReflections.mapNotNull { it.firstUnlockTime }.takeIf { it.isNotEmpty() }?.joinToString(", ")

        val weekdaySummary = if (weekdayAvgUnlock != null) "Weekday activity observed with unlocks: $weekdayAvgUnlock." else "Insufficient workday reflections."
        val weekendSummary = if (weekendAvgUnlock != null) "Weekend activity observed with unlocks: $weekendAvgUnlock." else "Insufficient weekend reflections."

        val insightText = if (weekdayAvgUnlock != null && weekendAvgUnlock != null) {
            "Weekday wake onset observed at $weekdayAvgUnlock compared to weekend onset at $weekendAvgUnlock."
        } else if (weekdayAvgUnlock != null) {
            "Consistent workday activity recorded ($weekdayAvgUnlock)."
        } else {
            "Insufficient evidence."
        }

        val pattern = WeeklyPatternEntity(
            weekIdentifier = weekId,
            startDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now - 7 * 86400000L)),
            endDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(now)),
            stableRoutinesSummary = if (weekdayAvgUnlock != null) "Morning unlock ($weekdayAvgUnlock)" else "Insufficient evidence.",
            changingRoutinesSummary = "None detected.",
            weekdayPatternSummary = weekdaySummary,
            saturdayPatternSummary = weekendSummary,
            sundayPatternSummary = weekendSummary,
            unusualDaysSummary = "None observed.",
            brokenRoutinesSummary = "None detected.",
            weeklyInsightText = insightText,
            timestamp = now
        )

        weeklyPatternDao.insertPattern(pattern)
        return pattern
    }
}
