package com.example.domain.capability

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class HardwareCapability(
    val key: String,
    val displayName: String,
    val requiredPermission: String?,
    val explanation: String
) {
    DEVICE_TIME("time", "Device Clock & Calendar", null, "Accessible directly from Android system clock."),
    NOTIFICATIONS(
        "notifications",
        "System Notifications",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null,
        "Required to send timely proactive briefings and routine alerts."
    ),
    LOCATION(
        "location",
        "GPS Location Context",
        Manifest.permission.ACCESS_FINE_LOCATION,
        "Required to calculate travel times and local city context."
    ),
    CALENDAR(
        "calendar",
        "Device Calendar",
        Manifest.permission.READ_CALENDAR,
        "Required to read scheduled meetings and family commitments."
    ),
    CONTACTS(
        "contacts",
        "Device Contacts",
        Manifest.permission.READ_CONTACTS,
        "Required to lookup family phone numbers and contact details."
    ),
    CAMERA(
        "camera",
        "Camera",
        Manifest.permission.CAMERA,
        "Required to scan physical documents or bills."
    ),
    MICROPHONE(
        "microphone",
        "Voice Input",
        Manifest.permission.RECORD_AUDIO,
        "Required for hands-free voice commands."
    ),
    PHONE(
        "phone",
        "Phone Calls",
        Manifest.permission.CALL_PHONE,
        "Required to initiate authorized calls to school or family."
    ),
    SMS(
        "sms",
        "SMS Dispatch",
        Manifest.permission.SEND_SMS,
        "Required to send authorized emergency SMS notifications."
    ),
    APP_LAUNCH(
        "app_launch",
        "Application Launch & Deep-linking",
        null,
        "Standard Android intent dispatch to launch installed apps."
    ),
    BATTERY(
        "battery",
        "Battery & Telemetry",
        null,
        "System battery and charging telemetry."
    ),
    NETWORK_STATE(
        "network",
        "Network Connectivity",
        Manifest.permission.ACCESS_NETWORK_STATE,
        "Detects online status for syncing."
    )
}

enum class CapabilityAvailability(val label: String, val isOperable: Boolean) {
    AVAILABLE("Available", true),
    REQUIRES_PERMISSION("Requires Permission", false),
    DENIED("Permission Denied", false),
    UNAVAILABLE("Unavailable on this device", false)
}

data class CapabilityCheckResult(
    val capability: HardwareCapability,
    val availability: CapabilityAvailability,
    val statusExplanation: String,
    val resolutionPrompt: String? = null
)

class DeviceCapabilityManager(private val context: Context?) {

    fun checkCapability(capability: HardwareCapability): CapabilityCheckResult {
        if (context == null) {
            return CapabilityCheckResult(
                capability = capability,
                availability = CapabilityAvailability.AVAILABLE,
                statusExplanation = "${capability.displayName} operates in simulated environment."
            )
        }

        val perm = capability.requiredPermission
        if (perm == null) {
            return CapabilityCheckResult(
                capability = capability,
                availability = CapabilityAvailability.AVAILABLE,
                statusExplanation = "${capability.displayName} is ready and operational."
            )
        }

        val granted = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        return if (granted) {
            CapabilityCheckResult(
                capability = capability,
                availability = CapabilityAvailability.AVAILABLE,
                statusExplanation = "Permission granted for ${capability.displayName}."
            )
        } else {
            CapabilityCheckResult(
                capability = capability,
                availability = CapabilityAvailability.REQUIRES_PERMISSION,
                statusExplanation = "To access ${capability.displayName}, VYB requires Android permission.",
                resolutionPrompt = capability.explanation
            )
        }
    }

    fun isAppInstalled(packageName: String): Boolean {
        if (context == null) return true
        return try {
            context.packageManager.getLaunchIntentForPackage(packageName) != null
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if a specific permission string is currently granted.
     */
    fun hasPermission(permission: String): Boolean {
        if (context == null) return true
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Inspect all core hardware capabilities.
     */
    fun inspectAllCapabilities(): List<CapabilityCheckResult> {
        return HardwareCapability.values().map { checkCapability(it) }
    }
}
