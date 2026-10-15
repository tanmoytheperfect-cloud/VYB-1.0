package com.example.domain.persistence

import android.content.Context
import java.security.MessageDigest

data class VybOwnerIdentity(
    val ownerId: String,
    val ownerEmail: String,
    val ownerName: String = "",
    val authProvider: String = "VYB_AUTHENTICATED_OWNER",
    val isAuthenticated: Boolean = true,
    val authenticatedAt: Long = System.currentTimeMillis()
)

object VybOwnerIdentityManager {

    private const val PREFS_NAME = "vyb_secure_owner_identity"
    private const val KEY_OWNER_ID = "authenticated_owner_id"
    private const val KEY_OWNER_EMAIL = "authenticated_owner_email"
    private const val KEY_OWNER_NAME = "authenticated_owner_name"
    private const val KEY_AUTH_PROVIDER = "auth_provider"
    private const val KEY_IS_AUTHENTICATED = "is_authenticated"
    private const val KEY_AUTH_TIMESTAMP = "auth_timestamp"

    /**
     * Computes a deterministic, globally unique, privacy-preserving owner ID from an authenticated identifier.
     * E.g. sha256(lowercase(email)).
     */
    fun computeStableOwnerId(identifier: String): String {
        val clean = identifier.trim().lowercase()
        if (clean.isBlank()) return "owner_default_vyb"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(clean.toByteArray(Charsets.UTF_8))
        return "vyb_owner_" + hash.take(16).joinToString("") { "%02x".format(it) }
    }

    fun getOwnerIdentity(context: Context): VybOwnerIdentity {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacyPrefs = context.getSharedPreferences("vyb_owner_prefs", Context.MODE_PRIVATE)

        var email = prefs.getString(KEY_OWNER_EMAIL, null)
        var name = prefs.getString(KEY_OWNER_NAME, null)

        // Check fallback to existing legacy prefs if not yet initialized in secure identity
        if (email.isNullOrBlank()) {
            email = legacyPrefs.getString("owner_email", "") ?: ""
        }
        if (name.isNullOrBlank()) {
            name = legacyPrefs.getString("owner_name", "") ?: ""
        }

        val resolvedEmail = email ?: ""
        val resolvedName = name ?: ""

        val storedOwnerId = prefs.getString(KEY_OWNER_ID, null)
        val hasExplicitCredentials = resolvedEmail.isNotBlank() || !storedOwnerId.isNullOrBlank()

        val effectiveOwnerId = if (!storedOwnerId.isNullOrBlank()) {
            storedOwnerId
        } else if (resolvedEmail.isNotBlank()) {
            computeStableOwnerId(resolvedEmail)
        } else {
            "unauthenticated_owner"
        }

        val isAuthenticated = prefs.getBoolean(KEY_IS_AUTHENTICATED, hasExplicitCredentials)
        val authProvider = prefs.getString(KEY_AUTH_PROVIDER, "VYB_AUTHENTICATED_OWNER") ?: "VYB_AUTHENTICATED_OWNER"
        val authTimestamp = prefs.getLong(KEY_AUTH_TIMESTAMP, System.currentTimeMillis())

        return VybOwnerIdentity(
            ownerId = effectiveOwnerId,
            ownerEmail = resolvedEmail,
            ownerName = resolvedName,
            authProvider = authProvider,
            isAuthenticated = isAuthenticated,
            authenticatedAt = authTimestamp
        )
    }

    fun setOwnerIdentity(context: Context, identity: VybOwnerIdentity) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_OWNER_ID, identity.ownerId)
            .putString(KEY_OWNER_EMAIL, identity.ownerEmail)
            .putString(KEY_OWNER_NAME, identity.ownerName)
            .putString(KEY_AUTH_PROVIDER, identity.authProvider)
            .putBoolean(KEY_IS_AUTHENTICATED, identity.isAuthenticated)
            .putLong(KEY_AUTH_TIMESTAMP, identity.authenticatedAt)
            .apply()

        // Also sync to legacy prefs for backward compatibility
        context.getSharedPreferences("vyb_owner_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("owner_email", identity.ownerEmail)
            .putString("owner_name", identity.ownerName)
            .apply()
    }
}
