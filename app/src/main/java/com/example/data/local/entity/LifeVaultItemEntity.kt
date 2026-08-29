package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Personal Life Vault Item Entity.
 * Represents a persistent, source-tracked fact or rule in VYB's Personal Life Vault.
 *
 * Source types:
 * - USER_PROVIDED
 * - USER_CONFIRMED
 * - CONNECTED
 * - LEARNED
 * - UNKNOWN
 *
 * Categories:
 * - ME
 * - FAMILY
 * - PLACES
 * - WORK
 * - FINANCE
 * - HEALTH_FITNESS
 * - IMPORTANT_DATES
 * - MEMORIES
 */
@Entity(tableName = "life_vault_items")
data class LifeVaultItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val key: String,
    val value: String,
    val source: String = "USER_PROVIDED",
    val sourceDetail: String? = null,
    val numericValue: Double? = null,
    val isConfirmed: Boolean = true,
    val isPendingConflict: Boolean = false,
    val conflictingValue: String? = null,
    val previousValue: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
