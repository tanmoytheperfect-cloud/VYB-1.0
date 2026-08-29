package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long? = null,
    val title: String,
    val amount: Double,
    val category: String = "General",
    val date: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
