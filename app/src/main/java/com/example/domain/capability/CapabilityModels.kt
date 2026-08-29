package com.example.domain.capability

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

enum class CapabilityType(val id: String, val displayName: String, val iconName: String) {
    GOOGLE_CALENDAR("calendar", "Google Calendar", "calendar_month"),
    GMAIL("gmail", "Gmail & Email Dispatch", "mail"),
    AMAZON("amazon", "Amazon Order & Delivery", "shopping_bag"),
    MAPS("maps", "Maps & Geo Navigation", "map"),
    LOCATION("location", "GPS Location", "location_on"),
    NOTIFICATIONS("notifications", "System Notifications", "notifications"),
    MICROPHONE("microphone", "Voice & Microphone", "mic"),
    CONTACTS("contacts", "Device Contacts", "contacts"),
    TRANSPORT_SERVICE("transport", "Cab & Transit Dispatch", "local_taxi"),
    BANKING("banking", "Direct Banking Feed", "account_balance")
}

enum class CapabilityStatus(val label: String, val isOperable: Boolean) {
    CONNECTED("Connected", true),
    NOT_CONNECTED("Not Connected", false),
    GRANTED("Permission Granted", true),
    DENIED("Permission Denied", false),
    AVAILABLE("Available (App/Intent)", true),
    UNAVAILABLE("Unavailable", false)
}

data class CapabilityState(
    val type: CapabilityType,
    val status: CapabilityStatus,
    val description: String,
    val requiredActionExplanation: String,
    val canConnect: Boolean = true,
    val isExternalService: Boolean = false
)

object CapabilityEngine {

    // Stored mock connection states for external accounts (can be toggled in settings)
    private val connectedServices = mutableSetOf<CapabilityType>()

    fun setServiceConnected(type: CapabilityType, isConnected: Boolean) {
        if (isConnected) {
            connectedServices.add(type)
        } else {
            connectedServices.remove(type)
        }
    }

    fun isServiceConnected(type: CapabilityType): Boolean {
        return connectedServices.contains(type)
    }

    fun evaluateCapability(type: CapabilityType, context: Context?): CapabilityState {
        return when (type) {
            CapabilityType.LOCATION -> {
                val granted = context?.let {
                    ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                } ?: false
                CapabilityState(
                    type = type,
                    status = if (granted) CapabilityStatus.GRANTED else CapabilityStatus.DENIED,
                    description = "Accesses live coordinates on device for hyperlocal routing.",
                    requiredActionExplanation = "Grant Android Location permission in OS settings."
                )
            }
            CapabilityType.MICROPHONE -> {
                val granted = context?.let {
                    ContextCompat.checkSelfPermission(it, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                } ?: false
                CapabilityState(
                    type = type,
                    status = if (granted) CapabilityStatus.GRANTED else CapabilityStatus.DENIED,
                    description = "Enables real-time voice speech input & JARVIS dialogue.",
                    requiredActionExplanation = "Grant Microphone permission for voice queries."
                )
            }
            CapabilityType.NOTIFICATIONS -> {
                val granted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context?.let {
                        ContextCompat.checkSelfPermission(it, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } ?: true
                } else true
                CapabilityState(
                    type = type,
                    status = if (granted) CapabilityStatus.GRANTED else CapabilityStatus.DENIED,
                    description = "Sends critical routine alerts and timely proactive reminders.",
                    requiredActionExplanation = "Enable notifications for proactive alarms."
                )
            }
            CapabilityType.CONTACTS -> {
                val granted = context?.let {
                    ContextCompat.checkSelfPermission(it, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                } ?: false
                CapabilityState(
                    type = type,
                    status = if (granted) CapabilityStatus.GRANTED else CapabilityStatus.DENIED,
                    description = "Accesses recipient contacts for supplier and family messaging.",
                    requiredActionExplanation = "Grant Contacts permission to lookup phone numbers."
                )
            }
            CapabilityType.GOOGLE_CALENDAR -> {
                val isConn = connectedServices.contains(CapabilityType.GOOGLE_CALENDAR)
                CapabilityState(
                    type = type,
                    status = if (isConn) CapabilityStatus.CONNECTED else CapabilityStatus.NOT_CONNECTED,
                    description = "Syncs meetings and deadlines with Google Calendar.",
                    requiredActionExplanation = "Authenticate your Google Calendar account in VYB Control.",
                    isExternalService = true
                )
            }
            CapabilityType.GMAIL -> {
                val isConn = connectedServices.contains(CapabilityType.GMAIL)
                CapabilityState(
                    type = type,
                    status = if (isConn) CapabilityStatus.CONNECTED else CapabilityStatus.NOT_CONNECTED,
                    description = "Drafts and sends emails directly through Gmail API.",
                    requiredActionExplanation = "Connect your Google / Gmail account in VYB Control.",
                    isExternalService = true
                )
            }
            CapabilityType.AMAZON -> {
                val isConn = connectedServices.contains(CapabilityType.AMAZON)
                CapabilityState(
                    type = type,
                    status = if (isConn) CapabilityStatus.CONNECTED else CapabilityStatus.NOT_CONNECTED,
                    description = "Tracks live delivery status and order history from Amazon.",
                    requiredActionExplanation = "Connect your Amazon account in VYB Control to check order tracking.",
                    isExternalService = true
                )
            }
            CapabilityType.MAPS -> {
                CapabilityState(
                    type = type,
                    status = CapabilityStatus.AVAILABLE,
                    description = "Open routes and view locations via Google Maps.",
                    requiredActionExplanation = "Standard Android Map intent is ready."
                )
            }
            CapabilityType.TRANSPORT_SERVICE -> {
                val isConn = connectedServices.contains(CapabilityType.TRANSPORT_SERVICE)
                CapabilityState(
                    type = type,
                    status = if (isConn) CapabilityStatus.CONNECTED else CapabilityStatus.AVAILABLE,
                    description = "Prepares ride comparisons for Yatri Sathi, Rapido, Uber & Ola.",
                    requiredActionExplanation = "Connect direct booking API or launch app intents."
                )
            }
            CapabilityType.BANKING -> {
                val isConn = connectedServices.contains(CapabilityType.BANKING)
                CapabilityState(
                    type = type,
                    status = if (isConn) CapabilityStatus.CONNECTED else CapabilityStatus.NOT_CONNECTED,
                    description = "Direct read-only banking feed (Account Aggregator).",
                    requiredActionExplanation = "Connect Account Aggregator for automated transaction logging.",
                    isExternalService = true
                )
            }
        }
    }

    fun getAllCapabilities(context: Context?): List<CapabilityState> {
        return CapabilityType.values().map { evaluateCapability(it, context) }
    }
}
