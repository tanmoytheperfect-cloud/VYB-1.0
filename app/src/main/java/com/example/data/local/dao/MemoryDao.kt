package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.MemoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    suspend fun getAllMemoriesDirect(): List<MemoryItemEntity>

    @Query("SELECT * FROM memories WHERE `key` LIKE '%' || :query || '%' OR `value` LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchMemoriesDirect(query: String): List<MemoryItemEntity>

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY timestamp DESC")
    fun getMemoriesByCategory(category: String): Flow<List<MemoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryItemEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryItemEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemory(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()
}
