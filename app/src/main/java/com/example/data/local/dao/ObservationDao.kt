package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ObservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObservationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: ObservationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservations(observations: List<ObservationEntity>)

    @Query("SELECT * FROM observations ORDER BY timestamp DESC")
    fun getAllObservationsFlow(): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM observations ORDER BY timestamp ASC")
    suspend fun getAllObservations(): List<ObservationEntity>

    @Query("SELECT * FROM observations WHERE subject = :subject ORDER BY timestamp ASC")
    suspend fun getObservationsForSubject(subject: String): List<ObservationEntity>

    @Query("SELECT * FROM observations WHERE domain = :domain ORDER BY timestamp ASC")
    suspend fun getObservationsForDomain(domain: String): List<ObservationEntity>

    @Query("SELECT * FROM observations WHERE eventType = :eventType ORDER BY timestamp ASC")
    suspend fun getObservationsForEventType(eventType: String): List<ObservationEntity>

    @Query("SELECT * FROM observations WHERE timestamp >= :sinceEpochMs ORDER BY timestamp ASC")
    suspend fun getObservationsSince(sinceEpochMs: Long): List<ObservationEntity>

    @Query("SELECT COUNT(*) FROM observations")
    suspend fun getCount(): Int

    @Query("SELECT * FROM observations WHERE observationId = :observationId LIMIT 1")
    suspend fun getObservationById(observationId: String): ObservationEntity?

    @Query("SELECT observationId FROM observations WHERE timestamp >= :sinceEpochMs")
    suspend fun getObservationIdsSince(sinceEpochMs: Long): List<String>

    @Query("SELECT * FROM observations ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentObservations(limit: Int): List<ObservationEntity>

    @Query("DELETE FROM observations")
    suspend fun clearAll()
}
