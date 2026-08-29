package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long? = null,
    val name: String,
    val category: String = "General",
    val quantity: String = "1",
    val estimatedCost: Double = 0.0,
    val isChecked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
