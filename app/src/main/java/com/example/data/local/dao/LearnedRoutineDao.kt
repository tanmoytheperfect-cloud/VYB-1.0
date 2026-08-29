package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.LearnedRoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnedRoutineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(routine: LearnedRoutineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routines: List<LearnedRoutineEntity>)

    @Query("SELECT * FROM learned_routines ORDER BY confidenceScore DESC, evidenceCount DESC")
    fun getAllLearnedRoutinesFlow(): Flow<List<LearnedRoutineEntity>>

    @Query("SELECT * FROM learned_routines WHERE isActive = 1 ORDER BY startMinuteOfDay ASC")
    fun getActiveRoutinesFlow(): Flow<List<LearnedRoutineEntity>>

    @Query("SELECT * FROM learned_routines WHERE status = 'LEARNED_ROUTINE' AND isActive = 1")
    suspend fun getVerifiedLearnedRoutines(): List<LearnedRoutineEntity>

    @Query("SELECT * FROM learned_routines WHERE routineId = :id")
    suspend fun getRoutineById(id: String): LearnedRoutineEntity?

    @Query("SELECT * FROM learned_routines WHERE daysOfWeek LIKE '%' || :day || '%' OR daysOfWeek = 'WEEKDAY' OR daysOfWeek = 'WEEKEND' OR daysOfWeek = 'ANY'")
    suspend fun getRoutinesForDay(day: String): List<LearnedRoutineEntity>

    @Query("DELETE FROM learned_routines WHERE routineId = :id")
    suspend fun deleteRoutine(id: String)

    @Query("DELETE FROM learned_routines")
    suspend fun clearAllLearnedRoutines()
}
