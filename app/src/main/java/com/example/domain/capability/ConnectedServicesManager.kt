package com.example.domain.capability

import android.content.Context
import com.example.domain.brain.KnowledgeState

enum class DigitalService(
    val id: String,
    val displayName: String,
    val defaultAndroidPackage: String?,
    val isExternalApi: Boolean
) {
    AMAZON("amazon", "Amazon Shopping & Deliveries", "com.amazon.mShop.android.shopping", true),
    UBER("uber", "Uber", "com.ubercab", true),
    OLA("ola", "Ola Cabs", "com.olacabs.customer", true),
    RAPIDO("rapido", "Rapido Auto & Bike", "com.rapido.passenger", true),
    INDRIVE("indrive", "inDrive Fare Negotiation", "sinet.startup.inDriver", true),
    YATRI_SATHI("yatri_sathi", "Yatri Sathi Metered Taxi", "com.yatrisathi.customer", false),
    GOOGLE_CALENDAR("calendar", "Google Calendar", "com.google.android.calendar", true),
    GMAIL("gmail", "Gmail", "com.google.android.gm", true),
    GOOGLE_MAPS("maps", "Google Maps", "com.google.android.apps.maps", false),
    BANKING_FEED("banking", "Direct Bank Account Feeds", null, true)
}

data class ServiceConnectivityReport(
    val service: DigitalService,
    val isConnectedViaApi: Boolean,
    val isAppInstalledOnDevice: Boolean,
    val statusSummary: String,
    val recommendedAction: String
)

class ConnectedServicesManager(private val context: Context?) {

    // Set of officially connected API accounts (kept in secure preferences)
    private val connectedApis = mutableSetOf<String>()

    fun setServiceConnected(service: DigitalService, connected: Boolean) {
        if (connected) connectedApis.add(service.id)
        else connectedApis.remove(service.id)
    }

    fun isServiceConnected(service: DigitalService): Boolean {
        return connectedApis.contains(service.id)
    }

    fun getServiceReport(service: DigitalService): ServiceConnectivityReport {
        val hasApi = isServiceConnected(service)
        val hasApp = service.defaultAndroidPackage?.let { pkg ->
            context?.let { ctx ->
                try {
                    ctx.packageManager.getLaunchIntentForPackage(pkg) != null
                } catch (_: Exception) {
                    false
                }
            } ?: false
        } ?: false

        val status = when {
            hasApi -> "Direct API linked securely."
            hasApp -> "Official app is installed on your device."
            else -> "Not connected and app is not installed."
        }

        val rec = when {
            hasApi -> "Ready for direct queries."
            hasApp -> "Can open installed app directly to view live data."
            else -> "Link account in Settings or install app from Google Play."
        }

        return ServiceConnectivityReport(
            service = service,
            isConnectedViaApi = hasApi,
            isAppInstalledOnDevice = hasApp,
            statusSummary = status,
            recommendedAction = rec
        )
    }
}
