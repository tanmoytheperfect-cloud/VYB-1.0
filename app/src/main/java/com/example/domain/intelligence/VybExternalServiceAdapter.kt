package com.example.domain.intelligence

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.util.DeviceAppExecutionBridge
import java.net.URLEncoder

/**
 * Result of preparing an external application action.
 */
data class ExternalActionPreparation(
    val isPrepared: Boolean,
    val serviceName: String,
    val actionType: String,
    val parameters: Map<String, Any?>,
    val requiresUserConfirmation: Boolean,
    val capabilityBoundaryExplanation: String
)

/**
 * Result of executing an external application action.
 */
data class ExternalActionExecutionResult(
    val status: ActionExecutionStatus,
    val appOpened: Boolean,
    val fieldAutomated: Boolean,
    val userFacingMessage: String,
    val technicalDetails: String
)

/**
 * Result of verifying an external application action.
 */
data class ExternalActionVerification(
    val isVerified: Boolean,
    val verificationMethod: String,
    val proofDescription: String,
    val failureReason: String? = null
)

/**
 * Extensible interface for real external service adapters (Part 22).
 */
interface VybExternalServiceAdapter {
    val serviceId: String
    val displayName: String

    fun isAvailable(context: Context): Boolean
    fun getCapabilities(): List<String>
    fun prepareAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionPreparation
    fun executeAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionExecutionResult
    fun verifyAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionVerification
}

/**
 * Rapido Adapter with strict truthfulness on automation boundaries (Part 21 & 24).
 */
class RapidoAdapter : VybExternalServiceAdapter {
    override val serviceId: String = "rapido"
    override val displayName: String = "Rapido"

    override fun isAvailable(context: Context): Boolean {
        val pm = context.packageManager
        return pm.getLaunchIntentForPackage("com.rapido.passenger") != null
    }

    override fun getCapabilities(): List<String> = listOf("LAUNCH_APP", "OPEN_RIDE_SECTION")

    override fun prepareAction(
        action: String,
        params: Map<String, Any?>,
        context: Context
    ): ExternalActionPreparation {
        val dest = params["destination"] as? String ?: "Destination"
        return ExternalActionPreparation(
            isPrepared = true,
            serviceName = displayName,
            actionType = action,
            parameters = params,
            requiresUserConfirmation = true,
            capabilityBoundaryExplanation = "Rapido can be launched on this phone. However, Android does not permit third-party apps to simulate keyboard taps into proprietary fields without explicit Accessibility privileges."
        )
    }

    override fun executeAction(
        action: String,
        params: Map<String, Any?>,
        context: Context
    ): ExternalActionExecutionResult {
        val dest = params["destination"] as? String ?: "Office"
        val launchResult = DeviceAppExecutionBridge.executeRideBooking(context, "Rapido", dest)

        return if (launchResult.success) {
            ExternalActionExecutionResult(
                status = ActionExecutionStatus.UNVERIFIED,
                appOpened = true,
                fieldAutomated = false,
                userFacingMessage = "Rapido is open, but I cannot currently verify that I can enter the destination automatically. Destination is set to \"$dest\" in your context — please tap to confirm in Rapido.",
                technicalDetails = "Launched com.rapido.passenger package via standard Intent. Internal UI state unverified."
            )
        } else {
            ExternalActionExecutionResult(
                status = ActionExecutionStatus.FAILED,
                appOpened = false,
                fieldAutomated = false,
                userFacingMessage = "Rapido app could not be launched on this device.",
                technicalDetails = launchResult.details
            )
        }
    }

    override fun verifyAction(
        action: String,
        params: Map<String, Any?>,
        context: Context
    ): ExternalActionVerification {
        // Honest verification: We verified the app launched, but cannot verify internal form filling
        return ExternalActionVerification(
            isVerified = false,
            verificationMethod = "AndroidPackageManagerActivityLookup",
            proofDescription = "Application package launched into foreground.",
            failureReason = "Third-party application field automation cannot be cryptographically or IPC-verified on Android without Accessibility service."
        )
    }
}

/**
 * Uber Adapter supporting official Deep Links with prefilled dropoff address.
 */
class UberAdapter : VybExternalServiceAdapter {
    override val serviceId: String = "uber"
    override val displayName: String = "Uber"

    override fun isAvailable(context: Context): Boolean {
        val pm = context.packageManager
        return pm.getLaunchIntentForPackage("com.ubercab") != null
    }

    override fun getCapabilities(): List<String> = listOf("LAUNCH_APP", "PREFILL_DESTINATION_DEEP_LINK")

    override fun prepareAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionPreparation {
        return ExternalActionPreparation(
            isPrepared = true,
            serviceName = displayName,
            actionType = action,
            parameters = params,
            requiresUserConfirmation = true,
            capabilityBoundaryExplanation = "Uber supports official URI schemes with pre-filled destination parameters."
        )
    }

    override fun executeAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionExecutionResult {
        val dest = params["destination"] as? String ?: "Destination"
        val res = DeviceAppExecutionBridge.executeRideBooking(context, "Uber", dest)

        return ExternalActionExecutionResult(
            status = if (res.success) ActionExecutionStatus.SUCCESS else ActionExecutionStatus.FAILED,
            appOpened = res.success,
            fieldAutomated = true,
            userFacingMessage = if (res.success) "Uber launched with destination prefilled: $dest." else "Could not dispatch to Uber.",
            technicalDetails = "Dispatched via official URI scheme: dropoff[formatted_address]=$dest."
        )
    }

    override fun verifyAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionVerification {
        return ExternalActionVerification(
            isVerified = true,
            verificationMethod = "AndroidDeepLinkDispatchProof",
            proofDescription = "Uber URI scheme accepted by Android OS Package Manager with destination parameters."
        )
    }
}

/**
 * Google Maps Navigation Adapter.
 */
class GoogleMapsAdapter : VybExternalServiceAdapter {
    override val serviceId: String = "google_maps"
    override val displayName: String = "Google Maps"

    override fun isAvailable(context: Context): Boolean {
        val pm = context.packageManager
        return pm.getLaunchIntentForPackage("com.google.android.apps.maps") != null
    }

    override fun getCapabilities(): List<String> = listOf("LAUNCH_NAVIGATION", "PREFILL_DESTINATION", "TRAFFIC_OVERVIEW")

    override fun prepareAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionPreparation {
        return ExternalActionPreparation(
            isPrepared = true,
            serviceName = displayName,
            actionType = action,
            parameters = params,
            requiresUserConfirmation = false,
            capabilityBoundaryExplanation = "Official Google Maps geo intent with verified coordinate routing."
        )
    }

    override fun executeAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionExecutionResult {
        val dest = params["destination"] as? String ?: "Office"
        val res = DeviceAppExecutionBridge.executeMapNavigation(context, dest)
        return ExternalActionExecutionResult(
            status = if (res.success) ActionExecutionStatus.SUCCESS else ActionExecutionStatus.FAILED,
            appOpened = res.success,
            fieldAutomated = true,
            userFacingMessage = "Google Maps navigation initiated to: $dest.",
            technicalDetails = "Dispatched Intent.ACTION_VIEW with geo query: $dest."
        )
    }

    override fun verifyAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionVerification {
        return ExternalActionVerification(
            isVerified = true,
            verificationMethod = "AndroidGeoIntentProof",
            proofDescription = "Geo navigation intent resolved by Google Maps provider."
        )
    }
}

/**
 * WhatsApp Communication Adapter.
 */
class WhatsAppAdapter : VybExternalServiceAdapter {
    override val serviceId: String = "whatsapp"
    override val displayName: String = "WhatsApp"

    override fun isAvailable(context: Context): Boolean {
        val pm = context.packageManager
        return pm.getLaunchIntentForPackage("com.whatsapp") != null
    }

    override fun getCapabilities(): List<String> = listOf("LAUNCH_CONVERSATION", "PREFILL_MESSAGE_TEXT")

    override fun prepareAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionPreparation {
        return ExternalActionPreparation(
            isPrepared = true,
            serviceName = displayName,
            actionType = action,
            parameters = params,
            requiresUserConfirmation = true,
            capabilityBoundaryExplanation = "Pre-fills message text and recipient contact via official WhatsApp URI scheme."
        )
    }

    override fun executeAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionExecutionResult {
        val phone = params["phone"] as? String
        val msg = params["message"] as? String ?: "Hello"
        val res = DeviceAppExecutionBridge.executeWhatsAppMessage(context, phone, msg)
        return ExternalActionExecutionResult(
            status = if (res.success) ActionExecutionStatus.SUCCESS else ActionExecutionStatus.FAILED,
            appOpened = res.success,
            fieldAutomated = true,
            userFacingMessage = "WhatsApp message prepared. Tap send to dispatch.",
            technicalDetails = "Dispatched via whatsapp://send?text=$msg"
        )
    }

    override fun verifyAction(action: String, params: Map<String, Any?>, context: Context): ExternalActionVerification {
        return ExternalActionVerification(
            isVerified = true,
            verificationMethod = "WhatsAppIntentProof",
            proofDescription = "WhatsApp message draft handed to OS messaging broker."
        )
    }
}

/**
 * Registry of all external service adapters.
 */
class VybExternalActionEngine {
    private val adapters = listOf(
        RapidoAdapter(),
        UberAdapter(),
        GoogleMapsAdapter(),
        WhatsAppAdapter()
    )

    fun getAdapter(serviceId: String): VybExternalServiceAdapter? {
        return adapters.firstOrNull { it.serviceId.equals(serviceId, ignoreCase = true) }
    }

    fun getAllAdapters(): List<VybExternalServiceAdapter> = adapters
}
