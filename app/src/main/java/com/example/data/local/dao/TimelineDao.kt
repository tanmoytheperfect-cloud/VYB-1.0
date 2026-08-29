package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.TimelineEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimelineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TimelineEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<TimelineEventEntity>)

    @Query("SELECT * FROM timeline_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEventsFlow(limit: Int = 100): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM timeline_events WHERE dateString = :dateString ORDER BY timestamp ASC")
    fun getEventsByDateFlow(dateString: String): Flow<List<TimelineEventEntity>>

    @Query("SELECT * FROM timeline_events WHERE dateString = :dateString ORDER BY timestamp ASC")
    suspend fun getEventsByDate(dateString: String): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getEventsSince(sinceTimestamp: Long): List<TimelineEventEntity>

    @Query("SELECT * FROM timeline_events WHERE eventType = :type ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getEventsByType(type: String, limit: Int = 50): List<TimelineEventEntity>

    @Query("SELECT COUNT(*) FROM timeline_events WHERE dateString = :dateString")
    suspend fun getEventCountForDate(dateString: String): Int

    @Query("DELETE FROM timeline_events WHERE dateString = :dateString")
    suspend fun clearEventsForDate(dateString: String)

    @Query("DELETE FROM timeline_events")
    suspend fun clearAllEvents()
}
