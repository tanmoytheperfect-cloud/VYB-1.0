package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val relation: String, // Son, Spouse, Daughter, Mother, Father, Partner, etc.
    val age: Int = 0,
    val occupationOrSchool: String = "",
    val notes: String = "",
    val routineSummary: String = "",
    val importantDates: String = ""
)
