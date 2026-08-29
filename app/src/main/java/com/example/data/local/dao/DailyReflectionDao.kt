package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.DailyReflectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReflectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: DailyReflectionEntity)

    @Query("SELECT * FROM daily_reflections ORDER BY timestamp DESC")
    fun getAllReflectionsFlow(): Flow<List<DailyReflectionEntity>>

    @Query("SELECT * FROM daily_reflections WHERE dateString = :dateString")
    suspend fun getReflectionByDate(dateString: String): DailyReflectionEntity?

    @Query("SELECT * FROM daily_reflections ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReflection(): DailyReflectionEntity?

    @Query("SELECT * FROM daily_reflections ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentReflections(limit: Int = 14): List<DailyReflectionEntity>

    @Query("DELETE FROM daily_reflections")
    suspend fun clearAllReflections()
}
