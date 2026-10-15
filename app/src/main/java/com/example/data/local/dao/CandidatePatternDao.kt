package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.CandidatePatternEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CandidatePatternDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(pattern: CandidatePatternEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(patterns: List<CandidatePatternEntity>)

    @Query("SELECT * FROM candidate_patterns ORDER BY confidence DESC")
    fun getAllPatternsFlow(): Flow<List<CandidatePatternEntity>>

    @Query("SELECT * FROM candidate_patterns ORDER BY confidence DESC")
    suspend fun getAllPatterns(): List<CandidatePatternEntity>

    @Query("SELECT * FROM candidate_patterns WHERE status = 'ESTABLISHED' ORDER BY confidence DESC")
    suspend fun getEstablishedPatterns(): List<CandidatePatternEntity>

    @Query("SELECT * FROM candidate_patterns WHERE patternId = :patternId LIMIT 1")
    suspend fun getPatternById(patternId: String): CandidatePatternEntity?

    @Query("SELECT * FROM candidate_patterns WHERE subject = :subject ORDER BY confidence DESC")
    suspend fun getPatternsForSubject(subject: String): List<CandidatePatternEntity>

    @Query("DELETE FROM candidate_patterns WHERE patternId = :patternId")
    suspend fun deletePattern(patternId: String)

    @Query("DELETE FROM candidate_patterns")
    suspend fun clearAll()
}
