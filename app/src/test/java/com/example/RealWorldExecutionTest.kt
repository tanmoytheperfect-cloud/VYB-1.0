package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.actions.ActionEngine
import com.example.domain.connectors.*
import com.example.domain.intent.StructuredIntent
import com.example.domain.security.ActionAuthorizationLevel
import com.example.domain.security.SecurityManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RealWorldExecutionTest {

    private lateinit var context: Context
    private lateinit var actionEngine: ActionEngine
    private lateinit var securityManager: SecurityManager
    private lateinit var connectorRegistry: ConnectorRegistry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        actionEngine = ActionEngine()
        securityManager = SecurityManager()
        connectorRegistry = ConnectorRegistry()
    }

    @Test
    fun testAmazonConnectorZeroSimulation() {
        val amazon = connectorRegistry.getAmazonConnector()
        assertNotNull(amazon)

        // Default state must be NOT_CONNECTED
        assertEquals(ConnectorState.NOT_CONNECTED, amazon.getConnectionState(context))

        // Check delivery status when not connected
        val result = amazon.checkDeliveryStatus(context)
        assertFalse(result.isSuccess)
        assertEquals(ConnectorErrorType.NOT_CONNECTED, result.errorType)
        // Must be truthful: explains that account is not connected and never invents fake tracking codes
        assertTrue(result.userFacingExplanation.contains("Amazon isn't connected", ignoreCase = true))
        assertFalse(result.userFacingExplanation.contains("TRACK"))
    }

    @Test
    fun testTransportConnectorFareOptions() {
        val transport = connectorRegistry.getTransportConnector()
        val fares = transport.getFareComparison("Home", "The Heritage School")

        assertTrue(fares.isNotEmpty())
        assertEquals(5, fares.size)

        // Lowest fare option
        val cheapest = fares.minByOrNull { it.estimatedFareInr }
        assertNotNull(cheapest)
        assertEquals("yatri_sathi", cheapest?.providerId)
        assertEquals(125.0, cheapest?.estimatedFareInr ?: 0.0, 0.01)

        // All fares must require explicit confirmation (no silent dispatch)
        assertTrue(fares.all { it.requiresExplicitConfirmation })
    }

    @Test
    fun testSecurityAuthorizationLevels() {
        // Financial or booking transactions must be HIGH risk
        val rideRisk = securityManager.evaluateAuthorizationRequirement("BOOK_RIDE")
        assertEquals(ActionAuthorizationLevel.HIGH, rideRisk)
        assertTrue(rideRisk.requiresExplicitConfirmation)

        val payRisk = securityManager.evaluateAuthorizationRequirement("SEND_MONEY_TRANSFER")
        assertEquals(ActionAuthorizationLevel.HIGH, payRisk)
        assertTrue(payRisk.requiresExplicitConfirmation)

        // Opening app or reminders are MEDIUM risk
        val appRisk = securityManager.evaluateAuthorizationRequirement("OPEN_APP")
        assertEquals(ActionAuthorizationLevel.MEDIUM, appRisk)

        // Reading time or local preferences are LOW risk
        val readRisk = securityManager.evaluateAuthorizationRequirement("READ_DEVICE_TIME")
        assertEquals(ActionAuthorizationLevel.LOW, readRisk)
        assertFalse(readRisk.requiresExplicitConfirmation)
    }

    @Test
    fun testActionEngineDirectBookingRejection() = runBlocking {
        // Direct automated booking without provider API must be rejected honestly
        val bookRideIntent = StructuredIntent(
            intent = StructuredIntent.INTENT_BOOK_RIDE,
            entities = mapOf("provider" to "Uber", "destination" to "School")
        )

        val result = actionEngine.execute(bookRideIntent, context)
        assertFalse(result.success)
        assertEquals("NO_LIVE_BOOKING_API", result.error)
        assertTrue(result.message.contains("unavailable without authenticated provider API", ignoreCase = true))
    }

    @Test
    fun testActionEngineReminderCreation() = runBlocking {
        val reminderIntent = StructuredIntent(
            intent = StructuredIntent.INTENT_CREATE_REMINDER,
            entities = mapOf("title" to "Pick up Vivaan from school", "time" to "10:30 AM")
        )

        val result = actionEngine.execute(reminderIntent, context)
        assertTrue(result.success)
        assertTrue(result.message.contains("Reminder", ignoreCase = true) || result.message.contains("scheduled", ignoreCase = true))
    }
}
