package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.LearningSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningSettingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: LearningSettingEntity)

    @Query("SELECT * FROM learning_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<LearningSettingEntity?>

    @Query("SELECT * FROM learning_settings WHERE id = 1")
    suspend fun getSettings(): LearningSettingEntity?

    @Query("UPDATE learning_settings SET isLearningPaused = :isPaused WHERE id = 1")
    suspend fun setLearningPaused(isPaused: Boolean)
}
