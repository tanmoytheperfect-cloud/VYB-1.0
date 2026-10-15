package com.example.domain.cognitive.engine

import com.example.data.local.dao.CandidatePatternDao
import com.example.domain.cognitive.models.CandidatePattern
import com.example.domain.cognitive.models.PatternException
import java.text.SimpleDateFormat
import java.util.*

class ExceptionEngine(
    private val candidatePatternDao: CandidatePatternDao
) {

    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Checks if a proposed action or schedule deviates from an established pattern.
     * If it deviates on a day outside the pattern's recurrence (e.g. Saturday for a weekday routine),
     * flags the deviation as a one-time exception without destroying the established pattern.
     */
    fun evaluateException(
        pattern: CandidatePattern,
        targetTimestamp: Long,
        reason: String
    ): PatternException? {
        val cal = Calendar.getInstance().apply { timeInMillis = targetTimestamp }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dateStr = sdfDate.format(Date(targetTimestamp))

        // If day of week is NOT in the pattern's expected recurrence days
        if (dayOfWeek !in pattern.expectedDays) {
            return PatternException(
                exceptionId = "exc_${pattern.patternId}_${dateStr}",
                patternId = pattern.patternId,
                dateString = dateStr,
                dayOfWeek = dayOfWeek,
                reason = "One-time exception on $dateStr: $reason (deviates from regular ${pattern.recurrence.lowercase()} pattern)",
                wasExplicitlyRequested = true,
                timestamp = System.currentTimeMillis()
            )
        }
        return null
    }

    /**
     * Records a one-time exception against an established pattern in Room.
     * Preserves the established pattern and evidence count intact.
     */
    suspend fun recordException(patternId: String, exception: PatternException) {
        val entity = candidatePatternDao.getPatternById(patternId) ?: return
        val currentJson = entity.exceptionsJson
        val newEntry = "{\"date\":\"${exception.dateString}\",\"reason\":\"${exception.reason.replace("\"", "\\\"")}\"}"
        val updatedJson = if (currentJson == "[]" || currentJson.isBlank()) {
            "[$newEntry]"
        } else {
            "${currentJson.dropLast(1)},$newEntry]"
        }
        candidatePatternDao.insertOrUpdate(entity.copy(exceptionsJson = updatedJson, updatedAt = System.currentTimeMillis()))
    }
}
