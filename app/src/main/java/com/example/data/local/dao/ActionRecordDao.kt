package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ActionRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionRecordDao {
    @Query("SELECT * FROM action_records ORDER BY timestamp DESC")
    fun getAllActions(): Flow<List<ActionRecordEntity>>

    @Query("SELECT * FROM action_records WHERE status = 'WAITING_FOR_CONFIRMATION' ORDER BY timestamp DESC")
    fun getPendingConfirmationActions(): Flow<List<ActionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: ActionRecordEntity): Long

    @Query("UPDATE action_records SET status = :status WHERE id = :id")
    suspend fun updateActionStatus(id: Long, status: String)

    @Query("DELETE FROM action_records WHERE id = :id")
    suspend fun deleteAction(id: Long)
}
