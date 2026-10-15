package com.example.domain.temporal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

data class NotificationDispatchResult(
    val isPosted: Boolean,
    val notificationId: Int,
    val reason: String
)

/**
 * Manages notification channels, permission checking, and verified notification dispatch.
 */
object VybNotificationHelper {

    const val CHANNEL_INTENTIONS_ID = "vyb_intentions_channel"
    const val CHANNEL_DIAGNOSTICS_ID = "vyb_diagnostics_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. High-importance Channel for Alarms & Critical Intentions
            val intentionChannel = NotificationChannel(
                CHANNEL_INTENTIONS_ID,
                "VYB Scheduled Intentions & Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Delivers timely scheduled personal intentions, alarms, routines, and wakeups"
                enableVibration(true)
                enableLights(true)
            }

            // 2. Default-importance Channel for Diagnostics & Background Sync
            val diagChannel = NotificationChannel(
                CHANNEL_DIAGNOSTICS_ID,
                "VYB Persistence Diagnostics",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Audit trail and diagnostics for persistent background wake events"
            }

            notificationManager.createNotificationChannel(intentionChannel)
            notificationManager.createNotificationChannel(diagChannel)
        }
    }

    fun isNotificationPermitted(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun postIntentionNotification(
        context: Context,
        intentionId: String,
        title: String,
        bodyText: String,
        eventId: String = "VYB-EVENT-${System.currentTimeMillis()}"
    ): NotificationDispatchResult {
        if (!isNotificationPermitted(context)) {
            return NotificationDispatchResult(
                isPosted = false,
                notificationId = intentionId.hashCode(),
                reason = "Notification permission not granted in Android OS settings."
            )
        }

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_INTENTION_ID", intentionId)
            putExtra("EVENT_ID", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            intentionId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_INTENTIONS_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSubText("VYB Personal Intelligence")
            .build()

        val notificationId = intentionId.hashCode()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            return NotificationDispatchResult(
                isPosted = true,
                notificationId = notificationId,
                reason = "Notification posted successfully via $CHANNEL_INTENTIONS_ID."
            )
        } catch (e: SecurityException) {
            return NotificationDispatchResult(
                isPosted = false,
                notificationId = notificationId,
                reason = "SecurityException: POST_NOTIFICATIONS missing or denied."
            )
        } catch (e: Exception) {
            return NotificationDispatchResult(
                isPosted = false,
                notificationId = notificationId,
                reason = e.localizedMessage ?: "Failed to post notification."
            )
        }
    }
}
