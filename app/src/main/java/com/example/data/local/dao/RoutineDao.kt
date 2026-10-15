package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.RoutineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY timeOfDay ASC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines ORDER BY timeOfDay ASC")
    suspend fun getAllRoutinesDirect(): List<RoutineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("UPDATE routines SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleRoutine(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)
}
