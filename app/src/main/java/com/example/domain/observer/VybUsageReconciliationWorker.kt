package com.example.domain.observer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.domain.cognitive.VybCognitiveEngine
import com.example.domain.permission.VybPermissionManager

/**
 * Deferrable background periodic reconciliation worker using Android WorkManager.
 * Runs non-intrusively according to Android battery, idle, and execution constraints.
 */
class VybUsageReconciliationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val permissionManager = VybPermissionManager(applicationContext)
        if (!permissionManager.hasUsageStatsPermission()) {
            return Result.success()
        }

        return try {
            val database = AppDatabase.getDatabase(applicationContext)
            val cognitiveEngine = VybCognitiveEngine(database)
            val observer = VybDeviceObserver(
                context = applicationContext,
                database = database,
                cognitiveEngine = cognitiveEngine,
                permissionManager = permissionManager
            )

            val result = observer.reconcileUsageEvents()
            if (result.success) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "vyb_periodic_usage_reconciliation"
    }
}
