package com.example.domain.permission

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class PermissionState {
    AVAILABLE,
    NOT_GRANTED,
    NOT_SUPPORTED,
    NOT_REQUIRED
}

enum class VybPermissionType(
    val displayName: String,
    val description: String,
    val isCriticalForOS: Boolean,
    val androidPermissions: List<String>
) {
    USAGE_STATS(
        displayName = "Usage Stats Access",
        description = "Allows observing legitimate app usage sequences and screen active intervals.",
        isCriticalForOS = false,
        androidPermissions = emptyList() // Requires AppOps special access
    ),
    CALENDAR(
        displayName = "Calendar Access",
        description = "Allows observing upcoming schedule commitments to provide contextual morning briefings.",
        isCriticalForOS = false,
        androidPermissions = listOf(android.Manifest.permission.READ_CALENDAR)
    ),
    NOTIFICATIONS(
        displayName = "Notification Permission",
        description = "Allows VYB to deliver timely, non-intrusive proactive alerts.",
        isCriticalForOS = false,
        androidPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(android.Manifest.permission.POST_NOTIFICATIONS)
        } else emptyList()
    ),
    LOCATION(
        displayName = "Location Context",
        description = "Provides city/area context for weather and travel suggestions.",
        isCriticalForOS = false,
        androidPermissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ),
    ALARM(
        displayName = "Exact Alarms & Reminders",
        description = "Allows scheduling timely routine checks and morning briefings.",
        isCriticalForOS = false,
        androidPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
        } else emptyList()
    ),
    MICROPHONE(
        displayName = "Voice Input (Microphone)",
        description = "Allows hands-free conversational voice commands.",
        isCriticalForOS = false,
        androidPermissions = listOf(android.Manifest.permission.RECORD_AUDIO)
    ),
    CONTACTS(
        displayName = "Contacts Access",
        description = "Allows matching supplier or family names directly.",
        isCriticalForOS = false,
        androidPermissions = listOf(android.Manifest.permission.READ_CONTACTS)
    ),
    CAMERA(
        displayName = "Camera / Visual AI",
        description = "Allows capturing outfit or document photos for visual analysis.",
        isCriticalForOS = false,
        androidPermissions = listOf(android.Manifest.permission.CAMERA)
    )
}

data class PermissionReport(
    val type: VybPermissionType,
    val state: PermissionState,
    val canRequestInApp: Boolean
)

open class VybPermissionManager(private val context: Context) {

    fun checkPermission(type: VybPermissionType): PermissionState {
        return when (type) {
            VybPermissionType.USAGE_STATS -> {
                if (hasUsageStatsPermission()) PermissionState.AVAILABLE else PermissionState.NOT_GRANTED
            }
            VybPermissionType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (isGranted(android.Manifest.permission.POST_NOTIFICATIONS)) {
                        PermissionState.AVAILABLE
                    } else {
                        PermissionState.NOT_GRANTED
                    }
                } else {
                    PermissionState.AVAILABLE // Granted by default on older Android
                }
            }
            VybPermissionType.CALENDAR -> {
                if (isGranted(android.Manifest.permission.READ_CALENDAR)) {
                    PermissionState.AVAILABLE
                } else {
                    PermissionState.NOT_GRANTED
                }
            }
            VybPermissionType.LOCATION -> {
                val hasFine = isGranted(android.Manifest.permission.ACCESS_FINE_LOCATION)
                val hasCoarse = isGranted(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                if (hasFine || hasCoarse) PermissionState.AVAILABLE else PermissionState.NOT_GRANTED
            }
            VybPermissionType.ALARM -> {
                PermissionState.AVAILABLE
            }
            VybPermissionType.MICROPHONE -> {
                if (isGranted(android.Manifest.permission.RECORD_AUDIO)) PermissionState.AVAILABLE else PermissionState.NOT_GRANTED
            }
            VybPermissionType.CONTACTS -> {
                if (isGranted(android.Manifest.permission.READ_CONTACTS)) PermissionState.AVAILABLE else PermissionState.NOT_GRANTED
            }
            VybPermissionType.CAMERA -> {
                if (isGranted(android.Manifest.permission.CAMERA)) PermissionState.AVAILABLE else PermissionState.NOT_GRANTED
            }
        }
    }

    fun getAllPermissionReports(): List<PermissionReport> {
        return VybPermissionType.values().map { type ->
            val state = checkPermission(type)
            val canRequestInApp = type.androidPermissions.isNotEmpty()
            PermissionReport(type, state, canRequestInApp)
        }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    open fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun createUsageStatsSettingsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun createAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
}
