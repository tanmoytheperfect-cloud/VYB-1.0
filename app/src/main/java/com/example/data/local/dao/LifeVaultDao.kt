package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.LifeVaultItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeVaultDao {
    @Query("SELECT * FROM life_vault_items ORDER BY updatedAt DESC")
    fun getAllVaultItemsFlow(): Flow<List<LifeVaultItemEntity>>

    @Query("SELECT * FROM life_vault_items ORDER BY updatedAt DESC")
    suspend fun getAllVaultItems(): List<LifeVaultItemEntity>

    @Query("SELECT * FROM life_vault_items WHERE category = :category ORDER BY updatedAt DESC")
    fun getVaultItemsByCategoryFlow(category: String): Flow<List<LifeVaultItemEntity>>

    @Query("SELECT * FROM life_vault_items WHERE category = :category ORDER BY updatedAt DESC")
    suspend fun getVaultItemsByCategory(category: String): List<LifeVaultItemEntity>

    @Query("SELECT * FROM life_vault_items WHERE `key` = :key LIMIT 1")
    suspend fun findVaultItemByKey(key: String): LifeVaultItemEntity?

    @Query("SELECT * FROM life_vault_items WHERE category = :category AND `key` = :key LIMIT 1")
    suspend fun findVaultItemByCategoryAndKey(category: String, key: String): LifeVaultItemEntity?

    @Query("SELECT * FROM life_vault_items WHERE `key` LIKE '%' || :query || '%' OR `value` LIKE '%' || :query || '%'")
    suspend fun searchVaultItems(query: String): List<LifeVaultItemEntity>

    @Query("SELECT * FROM life_vault_items WHERE isPendingConflict = 1")
    fun getPendingConflictsFlow(): Flow<List<LifeVaultItemEntity>>

    @Query("SELECT * FROM life_vault_items WHERE isPendingConflict = 1")
    suspend fun getPendingConflicts(): List<LifeVaultItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: LifeVaultItemEntity): Long

    @Update
    suspend fun updateVaultItem(item: LifeVaultItemEntity)

    @Query("DELETE FROM life_vault_items WHERE id = :id")
    suspend fun deleteVaultItemById(id: Long)

    @Query("DELETE FROM life_vault_items WHERE `key` = :key")
    suspend fun deleteVaultItemByKey(key: String)

    @Query("DELETE FROM life_vault_items WHERE category = :category AND `key` = :key")
    suspend fun deleteVaultItemByCategoryAndKey(category: String, key: String)

    @Query("DELETE FROM life_vault_items")
    suspend fun clearAllVaultItems()
}
