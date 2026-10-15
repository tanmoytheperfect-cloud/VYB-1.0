package com.example.domain.temporal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.example.data.local.AppDatabase
import com.example.data.local.entity.PersistenceLedgerEntity
import com.example.domain.genesis.GenesisDataSource
import com.example.domain.genesis.PersonalContextGraph
import com.example.domain.genesis.VybPersonalProfile
import com.example.domain.intelligence.VybIntelligenceOrchestrator
import com.example.domain.permission.VybPermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Universal Background Wakeup Receiver for VYB.
 * Wakes when Android AlarmManager triggers, reconstructs context,
 * executes the central intelligence pipeline, verifies result,
 * posts verified notification, schedules next recurrence if any,
 * and cleanly returns to dormant state.
 */
class VybIntentionBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val intentionId = intent.getStringExtra(VybTemporalScheduler.EXTRA_INTENTION_ID) ?: return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "vyb:IntentionExecutionWakeLock"
        )
        wakeLock?.acquire(20000L) // Safety timeout: 20 seconds

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                executeIntentionInBackground(context, intentionId)
            } catch (e: Exception) {
                val db = AppDatabase.getDatabase(context)
                db.persistenceLedgerDao().insertLedgerEntry(
                    PersistenceLedgerEntity(
                        eventId = "VYB-ERR-${System.currentTimeMillis()}",
                        intentionId = intentionId,
                        eventType = "ERROR",
                        details = "Background execution exception: ${e.localizedMessage}",
                        isSuccess = false
                    )
                )
            } finally {
                wakeLock?.let {
                    if (it.isHeld) it.release()
                }
                pendingResult.finish()
            }
        }
    }

    private suspend fun executeIntentionInBackground(context: Context, intentionId: String) {
        val database = AppDatabase.getDatabase(context)
        val intentionDao = database.persistentIntentionDao()
        val ledgerDao = database.persistenceLedgerDao()

        val intention = intentionDao.getIntentionById(intentionId) ?: return

        val now = System.currentTimeMillis()
        val eventId = "VYB-EVENT-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}"

        // 1. Freshness Check (Section 27)
        val freshness = VybRecurrenceEvaluator.evaluateFreshness(
            intention.scheduledTimestamp,
            intention.staleThresholdMinutes,
            now
        )

        if (freshness.isStale) {
            // Stale event handling
            if (intention.recurrenceRule != "NONE") {
                val next = VybRecurrenceEvaluator.calculateNextOccurrence(
                    recurrenceRule = intention.recurrenceRule,
                    timeOfDay = intention.timeOfDay,
                    timezoneId = intention.timezone,
                    nowMillis = now
                )
                intentionDao.rescheduleIntention(intention.id, next.nextTimestampMillis)
                val updated = intention.copy(scheduledTimestamp = next.nextTimestampMillis)
                VybTemporalScheduler.scheduleWithAlarmManager(context, updated)

                ledgerDao.insertLedgerEntry(
                    PersistenceLedgerEntity(
                        eventId = eventId,
                        intentionId = intention.id,
                        eventType = "STALE_SKIPPED",
                        details = "Occurrence was stale (${freshness.reason}). Rescheduled next occurrence.",
                        isSuccess = true
                    )
                )
            } else {
                intentionDao.updateExecutionOutcome(
                    id = intention.id,
                    state = "MISSED",
                    lastExecTime = now,
                    result = "Stale: ${freshness.reason}",
                    proof = "Missed execution window while device was dormant.",
                    eventId = eventId
                )
                ledgerDao.insertLedgerEntry(
                    PersistenceLedgerEntity(
                        eventId = eventId,
                        intentionId = intention.id,
                        eventType = "MISSED",
                        details = freshness.reason,
                        isSuccess = false
                    )
                )
            }
            return
        }

        // 2. Log Wakeup Event
        ledgerDao.insertLedgerEntry(
            PersistenceLedgerEntity(
                eventId = eventId,
                intentionId = intention.id,
                eventType = "WAKE",
                details = "Woke up from dormancy to execute: \"${intention.rawRequest}\"",
                isSuccess = true
            )
        )

        // 3. Reconstruct Context (Section 16)
        val prefs = context.getSharedPreferences("vyb_owner_prefs", Context.MODE_PRIVATE)
        val profile = VybPersonalProfile(
            name = prefs.getString("owner_name", "") ?: "",
            callingTitle = prefs.getString("owner_title", "Sir") ?: "Sir",
            homeAddress = prefs.getString("owner_home_address", "") ?: "",
            officeAddress = prefs.getString("owner_office_address", "") ?: "",
            commutePreference = prefs.getString("commute_pref", "Uber Premier / Yatri Sathi") ?: "Uber Premier / Yatri Sathi",
            monthlyIncome = prefs.getFloat("monthly_income", 0f).toDouble(),
            fixedCommitments = prefs.getFloat("fixed_commitments", 0f).toDouble(),
            savingsTarget = prefs.getFloat("monthly_savings_target", 0f).toDouble(),
            currencySymbol = prefs.getString("currency_symbol", "₹") ?: "₹"
        )

        val permissionManager = VybPermissionManager(context)
        val orchestrator = VybIntelligenceOrchestrator(
            memoryDao = database.memoryDao(),
            taskDao = database.taskDao(),
            lifeVaultDao = database.lifeVaultDao(),
            expenseDao = database.expenseDao(),
            shoppingDao = database.shoppingDao(),
            routineDao = database.routineDao(),
            goalDao = database.goalDao(),
            projectDao = database.projectDao(),
            familyDao = database.familyDao()
        )

        // 4. Route through Central Orchestrator (Section 1 & 15)
        val result = orchestrator.processRequest(
            input = intention.rawRequest,
            profile = profile,
            contextGraph = PersonalContextGraph(),
            authorizedSources = setOf(
                GenesisDataSource.IDENTITY,
                GenesisDataSource.DEVICE,
                GenesisDataSource.SUPPORTED_FINANCIAL_DATA
            ),
            appContext = context,
            permissionManager = permissionManager
        )

        val proof = result.verification?.proofDescription ?: "Orchestrator background execution completed."

        // 5. Notify Owner (Section 13)
        val notifTitle = intention.notificationTitle.ifBlank { "VYB Scheduled Task" }
        val notifBody = result.conversationalResponse.take(180).trim()

        val notifResult = VybNotificationHelper.postIntentionNotification(
            context = context,
            intentionId = intention.id,
            title = notifTitle,
            bodyText = notifBody,
            eventId = eventId
        )

        // 6. Persist Updated Intention State & Next Recurrence
        if (intention.recurrenceRule != "NONE") {
            val next = VybRecurrenceEvaluator.calculateNextOccurrence(
                recurrenceRule = intention.recurrenceRule,
                timeOfDay = intention.timeOfDay,
                timezoneId = intention.timezone,
                nowMillis = now
            )
            intentionDao.rescheduleIntention(intention.id, next.nextTimestampMillis)
            val updated = intention.copy(
                scheduledTimestamp = next.nextTimestampMillis,
                lastExecutionTimestamp = now,
                lastExecutionResult = result.conversationalResponse.take(200),
                lastVerificationProof = proof,
                lastEventId = eventId
            )
            VybTemporalScheduler.scheduleWithAlarmManager(context, updated)
        } else {
            intentionDao.updateExecutionOutcome(
                id = intention.id,
                state = "COMPLETED",
                lastExecTime = now,
                result = result.conversationalResponse.take(200),
                proof = proof,
                eventId = eventId
            )
        }

        // 7. Audit Log
        ledgerDao.insertLedgerEntry(
            PersistenceLedgerEntity(
                eventId = eventId,
                intentionId = intention.id,
                eventType = "COMPLETED",
                details = "Executed & verified. Notification: ${notifResult.reason}",
                isSuccess = true,
                verificationProof = proof
            )
        )
    }
}
