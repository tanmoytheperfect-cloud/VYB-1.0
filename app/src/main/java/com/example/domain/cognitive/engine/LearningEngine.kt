package com.example.domain.cognitive.engine

import com.example.data.local.dao.CandidatePatternDao
import com.example.data.local.dao.ObservationDao
import com.example.domain.cognitive.models.CandidatePattern
import com.example.domain.cognitive.models.PatternStatus
import com.example.domain.cognitive.models.TruthState

/**
 * Coordinates automated learning across observations, updates patterns, and processes corrections.
 */
class LearningEngine(
    private val observationDao: ObservationDao,
    private val candidatePatternDao: CandidatePatternDao,
    private val patternDiscoveryEngine: PatternDiscoveryEngine,
    private val lifeContextEngine: LifeContextEngine
) {

    /**
     * Executes the observation -> pattern discovery -> confidence update loop.
     */
    suspend fun executeLearningCycle(): List<CandidatePattern> {
        val observations = observationDao.getAllObservations()
        if (observations.isEmpty()) return emptyList()

        val cognitiveObservations = observations.map { entity ->
            com.example.domain.cognitive.models.CognitiveObservation(
                observationId = entity.observationId,
                ownerId = entity.ownerId,
                timestamp = entity.timestamp,
                timezone = entity.timezone,
                dateString = entity.dateString,
                dayOfWeek = entity.dayOfWeek,
                source = entity.source,
                domain = entity.domain,
                subject = entity.subject,
                eventType = entity.eventType,
                action = entity.action,
                context = entity.context,
                entities = emptyMap(),
                location = entity.location,
                relatedRoutine = entity.relatedRoutine,
                relatedPerson = entity.relatedPerson,
                confidence = entity.confidence,
                truthState = try { TruthState.valueOf(entity.truthState) } catch (e: Exception) { TruthState.OBSERVED },
                observedValue = entity.observedValue
            )
        }

        val discoveredPatterns = patternDiscoveryEngine.discoverPatterns(cognitiveObservations)

        // Attach discovered patterns to PersonalLifeGraph
        for (pattern in discoveredPatterns.filter { it.status == PatternStatus.ESTABLISHED || it.status == PatternStatus.EMERGING }) {
            val entityNode = lifeContextEngine.getLifeGraph().findEntityByName(pattern.subject)
            val parentId = entityNode?.id ?: "node_routines"
            lifeContextEngine.getLifeGraph().attachRoutineToEntity(
                entityId = parentId,
                routineTitle = pattern.title,
                timeWindow = pattern.expectedTimeRange,
                recurrence = pattern.recurrence
            )
        }

        lifeContextEngine.refreshState()
        return discoveredPatterns
    }

    /**
     * Explicit user correction takes precedence over inferred or learned patterns.
     */
    suspend fun applyCorrection(
        patternId: String,
        correctedTimeWindow: String,
        correctedRecurrence: String
    ) {
        val existing = candidatePatternDao.getPatternById(patternId) ?: return
        val updated = existing.copy(
            expectedTimeRange = correctedTimeWindow,
            recurrence = correctedRecurrence,
            status = PatternStatus.ESTABLISHED.name,
            truthState = TruthState.USER_CONFIRMED.name,
            isUserConfirmed = true,
            explanation = "User explicitly confirmed and corrected schedule to $correctedTimeWindow ($correctedRecurrence).",
            confidence = 1.0f,
            updatedAt = System.currentTimeMillis()
        )
        candidatePatternDao.insertOrUpdate(updated)
        lifeContextEngine.refreshState()
    }
}
