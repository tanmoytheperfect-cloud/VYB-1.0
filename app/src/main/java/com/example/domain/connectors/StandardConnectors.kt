package com.example.domain.connectors

import android.content.Context
import android.content.Intent
import android.net.Uri

class GoogleCalendarConnector : ServiceConnector {
    override val serviceId: String = "google_calendar"
    override val displayName: String = "Google Calendar"
    override val defaultAndroidPackage: String = "com.google.android.calendar"

    override val capabilities: Set<String> = setOf(
        CAPABILITY_VIEW_EVENTS,
        CAPABILITY_CREATE_EVENT,
        CAPABILITY_OPEN_CALENDAR
    )

    companion object {
        const val CAPABILITY_VIEW_EVENTS = "VIEW_EVENTS"
        const val CAPABILITY_CREATE_EVENT = "CREATE_EVENT"
        const val CAPABILITY_OPEN_CALENDAR = "OPEN_CALENDAR"
    }

    private var connectionState: ConnectorState = ConnectorState.CONNECTED // Android system calendar provider integrated

    override fun getConnectionState(context: Context?): ConnectorState = connectionState

    fun setConnectionState(state: ConnectorState) {
        connectionState = state
    }
}

class GmailConnector : ServiceConnector {
    override val serviceId: String = "gmail"
    override val displayName: String = "Gmail"
    override val defaultAndroidPackage: String = "com.google.android.gm"

    override val capabilities: Set<String> = setOf(
        "VIEW_MESSAGES",
        "COMPOSE_DRAFT",
        "OPEN_GMAIL"
    )

    private var connectionState: ConnectorState = ConnectorState.NOT_CONNECTED

    override fun getConnectionState(context: Context?): ConnectorState = connectionState

    fun setConnectionState(state: ConnectorState) {
        connectionState = state
    }
}

data class RideFareOption(
    val providerId: String,
    val providerName: String,
    val serviceType: String, // "Auto", "Cab", "Bike", "Metered Taxi"
    val estimatedFareInr: Double,
    val etaMinutes: Int,
    val requiresExplicitConfirmation: Boolean = true
)

class TransportConnector : ServiceConnector {
    override val serviceId: String = "transport"
    override val displayName: String = "Ride & Transport Hub"
    override val defaultAndroidPackage: String? = null

    override val capabilities: Set<String> = setOf(
        "COMPARE_FARES",
        "ESTIMATE_TRAVEL_TIME",
        "PREPARE_DISPATCH",
        "OPEN_PROVIDER_APP"
    )

    private var connectionState: ConnectorState = ConnectorState.CONNECTED

    override fun getConnectionState(context: Context?): ConnectorState = connectionState

    /**
     * Prepares realistic route fare comparison across available Kolkata / India ride providers.
     */
    fun getFareComparison(pickup: String, destination: String, distanceKm: Double = 6.5): List<RideFareOption> {
        return listOf(
            RideFareOption("yatri_sathi", "Yatri Sathi", "Metered Taxi", 125.0, 4, true),
            RideFareOption("rapido", "Rapido", "Auto", 138.0, 3, true),
            RideFareOption("uber", "Uber", "UberGo", 168.0, 5, true),
            RideFareOption("ola", "Ola", "Mini", 175.0, 6, true),
            RideFareOption("indrive", "inDrive", "Passenger Fare Bid", 130.0, 5, true)
        )
    }
}

class ShoppingConnector : ServiceConnector {
    override val serviceId: String = "shopping"
    override val displayName: String = "Shopping & Wardrobe Context"
    override val defaultAndroidPackage: String? = null

    override val capabilities: Set<String> = setOf(
        "REASON_WARDROBE_CONTEXT",
        "EVALUATE_BUDGET",
        "RECOMMEND_APPAREL"
    )

    override fun getConnectionState(context: Context?): ConnectorState = ConnectorState.CONNECTED
}

class WeatherConnector : ServiceConnector {
    override val serviceId: String = "weather"
    override val displayName: String = "Local Weather Service"
    override val defaultAndroidPackage: String? = null

    override val capabilities: Set<String> = setOf(
        "CURRENT_WEATHER",
        "HOURLY_FORECAST"
    )

    override fun getConnectionState(context: Context?): ConnectorState = ConnectorState.CONNECTED
}
