package com.example.domain.temporal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android Boot Receiver for VYB.
 * Wakes upon Android system boot completion (ACTION_BOOT_COMPLETED)
 * to reconstruct and restore all active scheduled intentions from Room database into AlarmManager.
 */
class VybBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    VybTemporalScheduler.restoreAllActiveIntentions(context)
                    VybTemporalScheduler.schedulePeriodicUsageReconciliation(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
