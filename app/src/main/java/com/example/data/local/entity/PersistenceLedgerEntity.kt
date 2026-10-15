package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persistence Audit Ledger Entity (Room Database).
 * Every background execution, wakeup, boot restore, or verification records an entry
 * with a traceable event ID for end-to-end auditability.
 */
@Entity(tableName = "persistence_ledger")
data class PersistenceLedgerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String, // e.g. "VYB-EVENT-2026-XXXX"
    val intentionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // "SCHEDULED", "WAKE", "ORCHESTRATOR_EXECUTE", "COMPLETED", "BOOT_RESTORE", "STALE_SKIPPED", "NOTIFICATION_POSTED", "MISSED"
    val details: String,
    val isSuccess: Boolean = true,
    val verificationProof: String? = null
)
