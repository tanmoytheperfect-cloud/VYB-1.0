package com.example.domain.cognitive.engine

import com.example.data.local.dao.ObservationDao
import com.example.data.local.entity.ObservationEntity
import com.example.domain.cognitive.models.CognitiveObservation
import com.example.domain.cognitive.models.TruthState
import java.text.SimpleDateFormat
import java.util.*

class ObservationEngine(
    private val observationDao: ObservationDao
) {

    private val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun recordObservation(
        domain: String,
        subject: String,
        eventType: String,
        action: String = "",
        context: String = "",
        entities: Map<String, String> = emptyMap(),
        relatedPerson: String? = null,
        relatedRoutine: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        timezone: String = TimeZone.getDefault().id
    ): CognitiveObservation {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(timezone)).apply { timeInMillis = timestamp }
        val dateString = sdfDate.format(Date(timestamp))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val observationId = "obs_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"

        val entitiesJson = if (entities.isEmpty()) "{}" else entities.entries.joinToString(prefix = "{", postfix = "}") {
            "\"${it.key}\":\"${it.value.replace("\"", "\\\"")}\""
        }

        val entity = ObservationEntity(
            observationId = observationId,
            ownerId = "primary_owner",
            timestamp = timestamp,
            timezone = timezone,
            dateString = dateString,
            dayOfWeek = dayOfWeek,
            source = "SYSTEM_OBSERVATION",
            domain = domain,
            subject = subject,
            eventType = eventType,
            action = action,
            context = context,
            entitiesJson = entitiesJson,
            location = "",
            relatedRoutine = relatedRoutine,
            relatedPerson = relatedPerson,
            confidence = 1.0f,
            truthState = TruthState.OBSERVED.name,
            observedValue = action.ifBlank { eventType },
            metadataJson = "{}"
        )

        observationDao.insertObservation(entity)

        return CognitiveObservation(
            observationId = observationId,
            ownerId = "primary_owner",
            timestamp = timestamp,
            timezone = timezone,
            dateString = dateString,
            dayOfWeek = dayOfWeek,
            source = "SYSTEM_OBSERVATION",
            domain = domain,
            subject = subject,
            eventType = eventType,
            action = action,
            context = context,
            entities = entities,
            location = "",
            relatedRoutine = relatedRoutine,
            relatedPerson = relatedPerson,
            confidence = 1.0f,
            truthState = TruthState.OBSERVED,
            observedValue = action.ifBlank { eventType }
        )
    }

    suspend fun getObservationsForSubject(subject: String): List<CognitiveObservation> {
        return observationDao.getObservationsForSubject(subject).map { it.toModel() }
    }

    suspend fun getAllObservations(): List<CognitiveObservation> {
        return observationDao.getAllObservations().map { it.toModel() }
    }

    private fun ObservationEntity.toModel(): CognitiveObservation {
        return CognitiveObservation(
            observationId = this.observationId,
            ownerId = this.ownerId,
            timestamp = this.timestamp,
            timezone = this.timezone,
            dateString = this.dateString,
            dayOfWeek = this.dayOfWeek,
            source = this.source,
            domain = this.domain,
            subject = this.subject,
            eventType = this.eventType,
            action = this.action,
            context = this.context,
            entities = emptyMap(),
            location = this.location,
            relatedRoutine = this.relatedRoutine,
            relatedPerson = this.relatedPerson,
            confidence = this.confidence,
            truthState = try { TruthState.valueOf(this.truthState) } catch (e: Exception) { TruthState.OBSERVED },
            observedValue = this.observedValue
        )
    }
}
