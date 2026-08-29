package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.OwnerAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnerAccountDao {
    @Query("SELECT * FROM owner_accounts ORDER BY updatedAt DESC")
    fun getAllAccounts(): Flow<List<OwnerAccountEntity>>

    @Query("SELECT * FROM owner_accounts WHERE isAiMemoryAllowed = 1 ORDER BY updatedAt DESC")
    fun getAccountsWithAiMemoryPermission(): Flow<List<OwnerAccountEntity>>

    @Query("SELECT * FROM owner_accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): OwnerAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: OwnerAccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<OwnerAccountEntity>)

    @Update
    suspend fun updateAccount(account: OwnerAccountEntity)

    @Delete
    suspend fun deleteAccount(account: OwnerAccountEntity)

    @Query("UPDATE owner_accounts SET isAiMemoryAllowed = :allowed WHERE id = :id")
    suspend fun updateAiMemoryPermission(id: Long, allowed: Boolean)

    @Query("SELECT COUNT(*) FROM owner_accounts")
    suspend fun getAccountCount(): Int
}
