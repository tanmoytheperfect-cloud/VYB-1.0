package com.example.domain.connectors

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Real Connection States as defined in Core Architecture Specification.
 */
enum class ConnectorState(val displayName: String, val isOperable: Boolean) {
    CONNECTED("Connected", true),
    NOT_CONNECTED("Not Connected", false),
    CONNECTING("Connecting", false),
    EXPIRED("Token Expired", false),
    REQUIRES_REAUTHORIZATION("Requires Re-authorization", false),
    UNAVAILABLE("Service Unavailable", false)
}

/**
 * Strict Error Classification for Connector Operations.
 * VYB must know the difference between:
 * - "I don't know" (DATA_NOT_FOUND)
 * - "I don't have permission" (PERMISSION_DENIED)
 * - "The service doesn't provide this capability" (CAPABILITY_UNSUPPORTED)
 * - "The service is temporarily unavailable" (TEMPORARILY_UNAVAILABLE)
 */
enum class ConnectorErrorType {
    NONE,
    DATA_NOT_FOUND,
    PERMISSION_DENIED,
    NOT_CONNECTED,
    TOKEN_EXPIRED,
    CAPABILITY_UNSUPPORTED,
    TEMPORARILY_UNAVAILABLE,
    NETWORK_OFFLINE
}

data class ConnectorExecutionResult<T>(
    val isSuccess: Boolean,
    val data: T? = null,
    val errorType: ConnectorErrorType = ConnectorErrorType.NONE,
    val userFacingExplanation: String,
    val suggestedAction: String? = null
)

/**
 * Base Service Connector Interface.
 * Every connector exposes its real capabilities and connection state.
 * Never fakes API access or invents dummy data.
 */
interface ServiceConnector {
    val serviceId: String
    val displayName: String
    val defaultAndroidPackage: String?
    val capabilities: Set<String>
    
    fun getConnectionState(context: Context?): ConnectorState
    fun isCapabilitySupported(capability: String): Boolean = capabilities.contains(capability)
    fun getInstalledAppIntent(context: Context?): Intent? {
        val pkg = defaultAndroidPackage ?: return null
        return context?.packageManager?.getLaunchIntentForPackage(pkg)
    }
}
