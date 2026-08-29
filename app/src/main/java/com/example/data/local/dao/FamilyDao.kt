package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.FamilyMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {
    @Query("SELECT * FROM family_members ORDER BY id ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity): Long

    @Update
    suspend fun updateFamilyMember(member: FamilyMemberEntity)

    @Query("DELETE FROM family_members WHERE id = :id")
    suspend fun deleteFamilyMember(id: Long)
}
