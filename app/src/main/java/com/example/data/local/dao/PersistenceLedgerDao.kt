package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.PersistenceLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersistenceLedgerDao {

    @Query("SELECT * FROM persistence_ledger ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLedgerEntriesFlow(limit: Int = 50): Flow<List<PersistenceLedgerEntity>>

    @Query("SELECT * FROM persistence_ledger ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLedgerEntriesDirect(limit: Int = 50): List<PersistenceLedgerEntity>

    @Query("SELECT * FROM persistence_ledger WHERE intentionId = :intentionId ORDER BY timestamp DESC")
    suspend fun getEntriesForIntention(intentionId: String): List<PersistenceLedgerEntity>

    @Query("SELECT * FROM persistence_ledger ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEntry(): PersistenceLedgerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: PersistenceLedgerEntity): Long

    @Query("DELETE FROM persistence_ledger")
    suspend fun clearLedger()
}
