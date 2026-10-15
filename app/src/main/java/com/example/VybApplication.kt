package com.example

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.domain.temporal.VybNotificationHelper
import com.example.domain.temporal.VybPersistenceDiagnostics
import com.example.domain.temporal.VybTemporalScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom Application class for VYB.
 * Sets up notification channels, tracks foreground/background states,
 * and restores active intentions on application initialization.
 */
class VybApplication : Application(), androidx.work.Configuration.Provider {

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize high-priority notification channels
        VybNotificationHelper.createNotificationChannels(this)

        // Initialize Fluid Privacy Firewall for centralized transfer & Local-Only policy
        com.example.domain.privacy.FluidPrivacyFirewall.initialize(this)

        // 2. Track process lifecycle for persistence diagnostics via ActivityLifecycleCallbacks
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                if (++activityReferences == 1 && !isActivityChangingConfigurations) {
                    // App enters foreground
                    VybPersistenceDiagnostics.markProcessForeground()
                }
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                isActivityChangingConfigurations = activity.isChangingConfigurations
                if (--activityReferences == 0 && !isActivityChangingConfigurations) {
                    // App enters background
                    VybPersistenceDiagnostics.markProcessBackground()
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })

        // 3. Restore all pending scheduled intentions from Room database into AlarmManager
        CoroutineScope(Dispatchers.IO).launch {
            try {
                VybTemporalScheduler.restoreAllActiveIntentions(this@VybApplication)
                VybTemporalScheduler.schedulePeriodicUsageReconciliation(this@VybApplication)
            } catch (e: Exception) {
                android.util.Log.w("VybApp", "Failed to restore scheduled intentions: ${e.message}", e)
            }
        }
    }
}
