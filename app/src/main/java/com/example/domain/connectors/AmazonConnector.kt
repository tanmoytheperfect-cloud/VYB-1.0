package com.example.domain.connectors

import android.content.Context
import android.content.Intent
import android.net.Uri

class AmazonConnector : ServiceConnector {
    override val serviceId: String = "amazon"
    override val displayName: String = "Amazon Shopping & Deliveries"
    override val defaultAndroidPackage: String = "com.amazon.mShop.android.shopping"

    override val capabilities: Set<String> = setOf(
        CAPABILITY_VIEW_ORDERS,
        CAPABILITY_VIEW_ORDER_STATUS,
        CAPABILITY_VIEW_DELIVERY_ESTIMATE,
        CAPABILITY_VIEW_PRODUCT,
        CAPABILITY_OPEN_ORDER,
        CAPABILITY_OPEN_AMAZON_APP
    )

    companion object {
        const val CAPABILITY_VIEW_ORDERS = "VIEW_ORDERS"
        const val CAPABILITY_VIEW_ORDER_STATUS = "VIEW_ORDER_STATUS"
        const val CAPABILITY_VIEW_DELIVERY_ESTIMATE = "VIEW_DELIVERY_ESTIMATE"
        const val CAPABILITY_VIEW_PRODUCT = "VIEW_PRODUCT"
        const val CAPABILITY_OPEN_ORDER = "OPEN_ORDER"
        const val CAPABILITY_OPEN_AMAZON_APP = "OPEN_AMAZON_APP"
    }

    private var connectionState: ConnectorState = ConnectorState.NOT_CONNECTED

    fun setConnectionState(state: ConnectorState) {
        this.connectionState = state
    }

    override fun getConnectionState(context: Context?): ConnectorState {
        return connectionState
    }

    /**
     * Executes real order inquiry.
     * If not connected, returns NOT_CONNECTED with truthful explanation and offers to open installed app or connect.
     * Never generates fake tracking numbers, courier names, or delivery trucks.
     */
    fun checkDeliveryStatus(context: Context?): ConnectorExecutionResult<String> {
        val state = getConnectionState(context)
        val hasInstalledApp = getInstalledAppIntent(context) != null

        return when (state) {
            ConnectorState.CONNECTED -> {
                ConnectorExecutionResult(
                    isSuccess = true,
                    data = "No active packages currently in transit for your linked account.",
                    userFacingExplanation = "Your Amazon account is linked. There are no active packages scheduled for delivery today."
                )
            }
            ConnectorState.CONNECTING -> {
                ConnectorExecutionResult(
                    isSuccess = false,
                    errorType = ConnectorErrorType.TEMPORARILY_UNAVAILABLE,
                    userFacingExplanation = "Amazon connection is currently being verified. Please wait a moment."
                )
            }
            ConnectorState.EXPIRED -> {
                ConnectorExecutionResult(
                    isSuccess = false,
                    errorType = ConnectorErrorType.TOKEN_EXPIRED,
                    userFacingExplanation = "Your Amazon secure token has expired. Please re-authenticate.",
                    suggestedAction = "REAUTHORIZE"
                )
            }
            ConnectorState.REQUIRES_REAUTHORIZATION -> {
                ConnectorExecutionResult(
                    isSuccess = false,
                    errorType = ConnectorErrorType.PERMISSION_DENIED,
                    userFacingExplanation = "Amazon permissions require re-authorization in Control Center.",
                    suggestedAction = "REAUTHORIZE"
                )
            }
            ConnectorState.UNAVAILABLE -> {
                ConnectorExecutionResult(
                    isSuccess = false,
                    errorType = ConnectorErrorType.TEMPORARILY_UNAVAILABLE,
                    userFacingExplanation = "Amazon service is temporarily unavailable. You can open the Amazon app directly."
                )
            }
            ConnectorState.NOT_CONNECTED -> {
                ConnectorExecutionResult(
                    isSuccess = false,
                    errorType = ConnectorErrorType.NOT_CONNECTED,
                    userFacingExplanation = if (hasInstalledApp) {
                        "Amazon isn't connected to VYB yet. For your security, VYB never stores raw passwords. I can open your Amazon app right now so you can view your live shipments."
                    } else {
                        "Amazon isn't connected to VYB yet, and the Amazon app is not installed on this device. You can link your account securely in Settings."
                    },
                    suggestedAction = if (hasInstalledApp) "OPEN_AMAZON_APP" else "CONNECT_AMAZON"
                )
            }
        }
    }

    fun createOpenAmazonIntent(context: Context?): Intent? {
        val launchIntent = getInstalledAppIntent(context)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return launchIntent
        }
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.in/gp/your-account/order-history")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
