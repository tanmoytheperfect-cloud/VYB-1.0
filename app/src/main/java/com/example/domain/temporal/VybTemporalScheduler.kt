package com.example.domain.temporal

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PersistenceLedgerEntity
import com.example.data.local.entity.PersistentIntentionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class SchedulingMechanism(val displayName: String) {
    ALARM_CLOCK_EXACT("AlarmManager.setAlarmClock (Exact & Visual Tray)"),
    EXACT_ALLOW_WHILE_IDLE("AlarmManager.setExactAndAllowWhileIdle"),
    INEXACT_ALLOW_WHILE_IDLE("AlarmManager.setAndAllowWhileIdle (Fallback)"),
    UNAVAILABLE("Scheduler Unavailable")
}

data class SchedulingResult(
    val isScheduled: Boolean,
    val intentionId: String,
    val mechanismUsed: SchedulingMechanism,
    val triggerTimestampMillis: Long,
    val formattedTime: String,
    val details: String
)

/**
 * Universal Android Scheduler for VYB Intentions.
 * Bridges Room database intentions to Android AlarmManager with exact alarm capability checks.
 */
object VybTemporalScheduler {

    const val ACTION_EXECUTE_INTENTION = "com.example.vyb.ACTION_EXECUTE_INTENTION"
    const val EXTRA_INTENTION_ID = "EXTRA_INTENTION_ID"

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedules a persistent intention with the Android OS AlarmManager.
     */
    fun scheduleWithAlarmManager(
        context: Context,
        intention: PersistentIntentionEntity
    ): SchedulingResult {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return SchedulingResult(
                isScheduled = false,
                intentionId = intention.id,
                mechanismUsed = SchedulingMechanism.UNAVAILABLE,
                triggerTimestampMillis = intention.scheduledTimestamp,
                formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(intention.scheduledTimestamp)),
                details = "AlarmManager system service is unavailable."
            )

        val intent = Intent(context, VybIntentionBroadcastReceiver::class.java).apply {
            action = ACTION_EXECUTE_INTENTION
            putExtra(EXTRA_INTENTION_ID, intention.id)
        }

        val requestCode = intention.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = intention.scheduledTimestamp
        val formattedTime = SimpleDateFormat("EEE, MMM d, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(triggerTime))

        val hasExact = canScheduleExact(context)

        return try {
            if (hasExact) {
                // If it's an alarm or user wakeup intention, use setAlarmClock for maximum priority & clock icon
                if (intention.normalizedIntent == "ALARM" || intention.rawRequest.contains("wake", ignoreCase = true)) {
                    val infoIntent = PendingIntent.getActivity(
                        context,
                        requestCode,
                        Intent(context, com.example.MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, infoIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                    SchedulingResult(
                        isScheduled = true,
                        intentionId = intention.id,
                        mechanismUsed = SchedulingMechanism.ALARM_CLOCK_EXACT,
                        triggerTimestampMillis = triggerTime,
                        formattedTime = formattedTime,
                        details = "Registered with Android AlarmClock (Exact priority with system tray icon)."
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    SchedulingResult(
                        isScheduled = true,
                        intentionId = intention.id,
                        mechanismUsed = SchedulingMechanism.EXACT_ALLOW_WHILE_IDLE,
                        triggerTimestampMillis = triggerTime,
                        formattedTime = formattedTime,
                        details = "Registered with AlarmManager.setExactAndAllowWhileIdle."
                    )
                }
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                SchedulingResult(
                    isScheduled = true,
                    intentionId = intention.id,
                    mechanismUsed = SchedulingMechanism.INEXACT_ALLOW_WHILE_IDLE,
                    triggerTimestampMillis = triggerTime,
                    formattedTime = formattedTime,
                    details = "Registered with AlarmManager.setAndAllowWhileIdle (Exact alarm permission not granted in OS)."
                )
            }
        } catch (e: SecurityException) {
            // In case exact alarm was rejected at runtime
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            SchedulingResult(
                isScheduled = true,
                intentionId = intention.id,
                mechanismUsed = SchedulingMechanism.INEXACT_ALLOW_WHILE_IDLE,
                triggerTimestampMillis = triggerTime,
                formattedTime = formattedTime,
                details = "Fell back to inexact alarm: ${e.localizedMessage}"
            )
        }
    }

    /**
     * Cancels any pending alarm for the given intention ID.
     */
    fun cancelAlarm(context: Context, intentionId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, VybIntentionBroadcastReceiver::class.java).apply {
            action = ACTION_EXECUTE_INTENTION
            putExtra(EXTRA_INTENTION_ID, intentionId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            intentionId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Re-registers all active SCHEDULED intentions from the Room database.
     * Called on device boot or application startup to ensure persistent synchronization.
     */
    suspend fun restoreAllActiveIntentions(context: Context): Int = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val pendingIntentions = database.persistentIntentionDao().getPendingOrScheduledIntentions()
        val now = System.currentTimeMillis()
        var restoredCount = 0

        pendingIntentions.forEach { intention ->
            // Check freshness
            if (intention.scheduledTimestamp > now) {
                scheduleWithAlarmManager(context, intention)
                restoredCount++
            } else {
                // If it passed while device was off, check freshness
                val freshness = VybRecurrenceEvaluator.evaluateFreshness(
                    intention.scheduledTimestamp,
                    intention.staleThresholdMinutes,
                    now
                )
                if (freshness.isStale) {
                    if (intention.recurrenceRule != "NONE") {
                        // Calculate next future occurrence
                        val next = VybRecurrenceEvaluator.calculateNextOccurrence(
                            recurrenceRule = intention.recurrenceRule,
                            timeOfDay = intention.timeOfDay,
                            timezoneId = intention.timezone,
                            nowMillis = now
                        )
                        database.persistentIntentionDao().rescheduleIntention(intention.id, next.nextTimestampMillis)
                        val updated = intention.copy(scheduledTimestamp = next.nextTimestampMillis)
                        scheduleWithAlarmManager(context, updated)
                        restoredCount++
                    } else {
                        database.persistentIntentionDao().updateExecutionOutcome(
                            id = intention.id,
                            state = "MISSED",
                            lastExecTime = now,
                            result = "Missed: ${freshness.reason}",
                            proof = "Device off during scheduled window.",
                            eventId = "VYB-BOOT-MISSED-${System.currentTimeMillis()}"
                        )
                    }
                } else {
                    // Within threshold, execute or schedule immediate wake
                    scheduleWithAlarmManager(context, intention)
                    restoredCount++
                }
            }
        }

        database.persistenceLedgerDao().insertLedgerEntry(
            PersistenceLedgerEntity(
                eventId = "VYB-RESTORE-${System.currentTimeMillis()}",
                intentionId = "ALL_PENDING",
                eventType = "BOOT_RESTORE",
                details = "Restored $restoredCount pending intentions from Room database into AlarmManager.",
                isSuccess = true
            )
        )

        restoredCount
    }

    /**
     * Schedules deferrable periodic background reconciliation using Android WorkManager.
     * Respects Android background battery & execution limits without continuously polling.
     */
    fun schedulePeriodicUsageReconciliation(context: Context) {
        try {
            if (!androidx.work.WorkManager.isInitialized()) {
                try {
                    val config = androidx.work.Configuration.Builder().build()
                    androidx.work.WorkManager.initialize(context, config)
                } catch (e: Exception) {
                    // Ignore if already initialized
                }
            }

            val constraints = androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.domain.observer.VybUsageReconciliationWorker>(
                15,
                java.util.concurrent.TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                com.example.domain.observer.VybUsageReconciliationWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            android.util.Log.w("VybScheduler", "Periodic usage reconciliation WorkManager schedule note: ${e.message}")
        }
    }
}
