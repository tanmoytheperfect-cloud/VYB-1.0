package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.WeeklyPatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyPatternDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: WeeklyPatternEntity)

    @Query("SELECT * FROM weekly_patterns ORDER BY timestamp DESC")
    fun getAllPatternsFlow(): Flow<List<WeeklyPatternEntity>>

    @Query("SELECT * FROM weekly_patterns WHERE weekIdentifier = :weekId")
    suspend fun getPatternByWeek(weekId: String): WeeklyPatternEntity?

    @Query("SELECT * FROM weekly_patterns ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestPattern(): WeeklyPatternEntity?

    @Query("DELETE FROM weekly_patterns")
    suspend fun clearAllPatterns()
}
