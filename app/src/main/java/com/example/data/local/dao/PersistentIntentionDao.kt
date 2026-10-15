package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.PersistentIntentionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersistentIntentionDao {

    @Query("SELECT * FROM persistent_intentions ORDER BY scheduledTimestamp ASC")
    fun getAllIntentionsFlow(): Flow<List<PersistentIntentionEntity>>

    @Query("SELECT * FROM persistent_intentions ORDER BY scheduledTimestamp ASC")
    suspend fun getAllIntentionsDirect(): List<PersistentIntentionEntity>

    @Query("SELECT * FROM persistent_intentions WHERE id = :id LIMIT 1")
    suspend fun getIntentionById(id: String): PersistentIntentionEntity?

    @Query("SELECT * FROM persistent_intentions WHERE executionState IN ('SCHEDULED', 'PENDING') ORDER BY scheduledTimestamp ASC")
    suspend fun getPendingOrScheduledIntentions(): List<PersistentIntentionEntity>

    @Query("SELECT * FROM persistent_intentions WHERE executionState IN ('SCHEDULED', 'PENDING') ORDER BY scheduledTimestamp ASC LIMIT 1")
    suspend fun getNextScheduledIntention(): PersistentIntentionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntention(intention: PersistentIntentionEntity)

    @Update
    suspend fun updateIntention(intention: PersistentIntentionEntity)

    @Query("UPDATE persistent_intentions SET executionState = :state, lastExecutionTimestamp = :lastExecTime, lastExecutionResult = :result, lastVerificationProof = :proof, lastEventId = :eventId WHERE id = :id")
    suspend fun updateExecutionOutcome(
        id: String,
        state: String,
        lastExecTime: Long,
        result: String?,
        proof: String?,
        eventId: String?
    )

    @Query("UPDATE persistent_intentions SET scheduledTimestamp = :newScheduledTime, executionState = 'SCHEDULED' WHERE id = :id")
    suspend fun rescheduleIntention(id: String, newScheduledTime: Long)

    @Query("UPDATE persistent_intentions SET executionState = 'CANCELLED' WHERE id = :id")
    suspend fun cancelIntention(id: String)

    @Query("DELETE FROM persistent_intentions WHERE id = :id")
    suspend fun deleteIntention(id: String)

    @Query("SELECT COUNT(*) FROM persistent_intentions WHERE executionState IN ('SCHEDULED', 'PENDING')")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM persistent_intentions WHERE executionState = 'COMPLETED'")
    suspend fun getCompletedCount(): Int

    @Query("SELECT COUNT(*) FROM persistent_intentions WHERE executionState = 'MISSED'")
    suspend fun getMissedCount(): Int
}
