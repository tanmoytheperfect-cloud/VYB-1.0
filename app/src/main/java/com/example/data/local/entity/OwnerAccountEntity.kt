package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "owner_accounts")
data class OwnerAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serviceName: String,
    val category: String, // E-Commerce, Social, Messaging, Mail, Media, Finance, Other
    val accountIdentifier: String, // username or email or phone
    val passwordOrSecret: String,
    val isAiMemoryAllowed: Boolean = true,
    val iconKey: String = "key", // amazon, flipkart, whatsapp, facebook, mail, camera, gallery, key
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
