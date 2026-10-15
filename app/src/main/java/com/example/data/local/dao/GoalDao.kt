package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, createdAt DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY isCompleted ASC, createdAt DESC")
    suspend fun getAllGoalsDirect(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE goals SET isCompleted = :isCompleted, progressPercent = :progress WHERE id = :id")
    suspend fun updateGoalProgress(id: Long, progress: Int, isCompleted: Boolean)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)
}
