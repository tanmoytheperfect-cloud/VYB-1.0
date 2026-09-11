package com.example.domain.connectors

import android.content.Context

class ConnectorRegistry {
    private val connectors = mutableMapOf<String, ServiceConnector>()

    init {
        register(AmazonConnector())
        register(GoogleCalendarConnector())
        register(GmailConnector())
        register(TransportConnector())
        register(ShoppingConnector())
        register(WeatherConnector())
    }

    fun register(connector: ServiceConnector) {
        connectors[connector.serviceId] = connector
    }

    fun getConnector(serviceId: String): ServiceConnector? = connectors[serviceId]

    fun getAllConnectors(): List<ServiceConnector> = connectors.values.toList()

    fun getAmazonConnector(): AmazonConnector = connectors["amazon"] as AmazonConnector

    fun getTransportConnector(): TransportConnector = connectors["transport"] as TransportConnector

    fun getGoogleCalendarConnector(): GoogleCalendarConnector = connectors["google_calendar"] as GoogleCalendarConnector

    fun getGmailConnector(): GmailConnector = connectors["gmail"] as GmailConnector
}
