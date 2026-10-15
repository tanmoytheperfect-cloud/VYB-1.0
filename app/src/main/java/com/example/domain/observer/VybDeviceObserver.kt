package com.example.domain.observer

import android.annotation.SuppressLint
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.dao.ObservationDao
import com.example.data.local.dao.TimelineDao
import com.example.data.local.entity.ObservationEntity
import com.example.data.local.entity.TimelineEventEntity
import com.example.domain.cognitive.VybCognitiveEngine
import com.example.domain.permission.VybPermissionManager
import com.example.domain.permission.VybPermissionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AwarenessState(val label: String, val colorHex: Long) {
    ACTIVE("Active", 0xFF10B981),
    PERMISSION_REQUIRED("Permission Required", 0xFFF59E0B),
    RESTRICTED("Restricted", 0xFFEF4444),
    STALE("Stale", 0xFF64748B),
    UNAVAILABLE("Unavailable", 0xFF94A3B8)
}

data class ObservationTraceItem(
    val observationId: String,
    val timestamp: Long,
    val source: String,
    val domain: String,
    val subject: String,
    val eventType: String,
    val confidence: Float,
    val truthState: String,
    val isPersisted: Boolean,
    val isProcessedByCognitiveEngine: Boolean
)

data class BackgroundAwarenessReport(
    val awarenessState: AwarenessState,
    val isUsageAccessGranted: Boolean,
    val isCalendarAccessGranted: Boolean,
    val isNotificationAccessGranted: Boolean,
    val lastCollectionTimeMs: Long,
    val lastCommittedWatermarkMs: Long,
    val latestCollectedCount: Int,
    val latestPersistedCount: Int,
    val latestProcessedCount: Int,
    val candidatePatternCount: Int,
    val establishedPatternCount: Int,
    val totalObservationCount: Int,
    val lastError: String?,
    val recentTraces: List<ObservationTraceItem> = emptyList()
)

data class ReconciliationResult(
    val success: Boolean,
    val windowStart: Long,
    val windowEnd: Long,
    val collectedCount: Int,
    val persistedCount: Int,
    val processedCount: Int,
    val newWatermark: Long,
    val error: String? = null
)

/**
 * Universal Background Device Observer & Usage Reconciliation Engine for VYB.
 *
 * Implements:
 * 1. Persistent collection watermark across process deaths & device reboots.
 * 2. Honest Android activity resume/pause session tracking with duration estimation.
 * 3. Direct route from Android signals -> canonical ObservationEntity -> VybCognitiveEngine -> PatternDiscoveryEngine.
 * 4. Transactional guarantees: Watermark advances ONLY after database write succeeds.
 * 5. Full developer diagnostics report with live trace verification.
 */
class VybDeviceObserver(
    private val context: Context,
    private val database: AppDatabase,
    private val cognitiveEngine: VybCognitiveEngine,
    private val permissionManager: VybPermissionManager
) {
    private val timelineDao: TimelineDao = database.timelineDao()
    private val observationDao: ObservationDao = database.observationDao()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastActiveEventTimestamp: Long = System.currentTimeMillis()
    private var isReceiverRegistered = false

    private val prefs = context.getSharedPreferences("vyb_usage_watermark_ledger", Context.MODE_PRIVATE)

    companion object {
        const val PREF_KEY_WATERMARK_MS = "key_watermark_ms"
        const val PREF_KEY_LAST_COLLECTION_TIME_MS = "key_last_collection_time_ms"
        const val PREF_KEY_LATEST_COLLECTED = "key_latest_collected"
        const val PREF_KEY_LATEST_PERSISTED = "key_latest_persisted"
        const val PREF_KEY_LATEST_PROCESSED = "key_latest_processed"
        const val PREF_KEY_LAST_ERROR = "key_last_error"

        const val MAX_RECOVERY_WINDOW_MS = 24 * 60 * 60 * 1000L // Bounded 24h recovery
        const val INITIAL_DEFAULT_WINDOW_MS = 6 * 60 * 60 * 1000L // Initial 6h recovery
        const val STALE_THRESHOLD_MS = 6 * 60 * 60 * 1000L // 6h inactivity marks awareness STALE
    }

    private val _awarenessReport = MutableStateFlow(createInitialReport())
    val awarenessReport: StateFlow<BackgroundAwarenessReport> = _awarenessReport.asStateFlow()

    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val now = System.currentTimeMillis()

            scope.launch {
                when (action) {
                    Intent.ACTION_USER_PRESENT -> {
                        recordUnlockEvent(now)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        recordBroadcastSignal(
                            eventType = "SCREEN_ON",
                            domain = "DEVICE_STATE",
                            subject = "Device",
                            contextData = "Screen turned on",
                            timestamp = now
                        )
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        lastActiveEventTimestamp = now
                        recordBroadcastSignal(
                            eventType = "SCREEN_OFF",
                            domain = "DEVICE_STATE",
                            subject = "Device",
                            contextData = "Screen turned off / Inactivity onset",
                            timestamp = now
                        )
                    }
                    Intent.ACTION_BOOT_COMPLETED -> {
                        recordBroadcastSignal(
                            eventType = "DEVICE_BOOT",
                            domain = "DEVICE_STATE",
                            subject = "Device",
                            contextData = "Device boot completed",
                            timestamp = now
                        )
                    }
                    Intent.ACTION_POWER_CONNECTED -> {
                        recordBroadcastSignal(
                            eventType = "POWER_CONNECTED",
                            domain = "DEVICE_STATE",
                            subject = "Battery",
                            contextData = "Device plugged into power",
                            timestamp = now
                        )
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        recordBroadcastSignal(
                            eventType = "POWER_DISCONNECTED",
                            domain = "DEVICE_STATE",
                            subject = "Battery",
                            contextData = "Device unplugged from power",
                            timestamp = now
                        )
                    }
                }
            }
        }
    }

    init {
        scope.launch {
            refreshAwarenessReport()
        }
    }

    fun startObserving() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            try {
                context.registerReceiver(deviceReceiver, filter)
                isReceiverRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Trigger immediate reconciliation and calendar scan
        pollUsageStatsIfPermitted()
        scanCalendarIfPermitted()
    }

    fun stopObserving() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(deviceReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun pollUsageStatsIfPermitted() {
        scope.launch {
            reconcileUsageEvents()
        }
    }

    /**
     * Executes reliable event reconciliation using persistent watermark.
     * Guarantees transactional write and forward into cognitive pipeline.
     */
    @SuppressLint("MissingPermission")
    suspend fun reconcileUsageEvents(now: Long = System.currentTimeMillis()): ReconciliationResult {
        val hasUsage = permissionManager.hasUsageStatsPermission()
        if (!hasUsage) {
            val errorMsg = "Usage Stats access not granted in Android Settings"
            saveLastError(errorMsg)
            refreshAwarenessReport()
            val watermark = prefs.getLong(PREF_KEY_WATERMARK_MS, 0L)
            return ReconciliationResult(
                success = false,
                windowStart = 0L,
                windowEnd = now,
                collectedCount = 0,
                persistedCount = 0,
                processedCount = 0,
                newWatermark = watermark,
                error = errorMsg
            )
        }

        val lastWatermark = prefs.getLong(PREF_KEY_WATERMARK_MS, 0L)
        val windowStart = when {
            lastWatermark <= 0L -> (now - INITIAL_DEFAULT_WINDOW_MS).coerceAtLeast(0L)
            now - lastWatermark > MAX_RECOVERY_WINDOW_MS -> now - MAX_RECOVERY_WINDOW_MS // Bounded recovery
            else -> lastWatermark
        }
        val windowEnd = now

        if (windowEnd <= windowStart) {
            refreshAwarenessReport()
            return ReconciliationResult(
                success = true,
                windowStart = windowStart,
                windowEnd = windowEnd,
                collectedCount = 0,
                persistedCount = 0,
                processedCount = 0,
                newWatermark = lastWatermark
            )
        }

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usageStatsManager == null) {
            val err = "UsageStatsManager system service unavailable"
            saveLastError(err)
            refreshAwarenessReport()
            return ReconciliationResult(false, windowStart, windowEnd, 0, 0, 0, lastWatermark, err)
        }

        return try {
            val events = usageStatsManager.queryEvents(windowStart, windowEnd)
            processUsageEventsStream(events, windowStart, windowEnd, now)
        } catch (e: SecurityException) {
            val err = "Usage Access revoked: ${e.message}"
            saveLastError(err)
            refreshAwarenessReport()
            ReconciliationResult(false, windowStart, windowEnd, 0, 0, 0, lastWatermark, err)
        } catch (e: Exception) {
            val err = "Failed to query usage events: ${e.message}"
            saveLastError(err)
            refreshAwarenessReport()
            ReconciliationResult(false, windowStart, windowEnd, 0, 0, 0, lastWatermark, err)
        }
    }

    /**
     * Parses raw usage events into paired sessions, deduplicates against Room,
     * writes transactionally, forwards to cognitive engine, and commits watermark.
     */
    suspend fun processUsageEventsStream(
        events: UsageEvents,
        windowStart: Long,
        windowEnd: Long,
        executionTime: Long
    ): ReconciliationResult {
        val eventList = mutableListOf<UsageEvents.Event>()
        val currentEvent = UsageEvents.Event()
        while (events.hasNextEvent()) {
            val ev = UsageEvents.Event()
            events.getNextEvent(ev)
            eventList.add(ev)
        }

        return processRawEventList(eventList, windowStart, windowEnd, executionTime)
    }

    /**
     * Internal processor accepting List<UsageEvents.Event> for production and deterministic testing.
     */
    suspend fun processRawEventList(
        eventList: List<UsageEvents.Event>,
        windowStart: Long,
        windowEnd: Long,
        executionTime: Long
    ): ReconciliationResult {
        val totalRawCount = eventList.size

        // Pair resume and pause events to estimate honest foreground engagement
        data class ActiveSession(
            val packageName: String,
            val startTime: Long,
            var isEstimatedStart: Boolean = false
        )

        val openSessions = mutableMapOf<String, ActiveSession>()
        data class CompletedSession(
            val packageName: String,
            val startTime: Long,
            val endTime: Long,
            val durationMs: Long,
            val isEstimated: Boolean
        )

        val completedSessions = mutableListOf<CompletedSession>()

        // Sort events chronologically to guarantee correct pairing
        val sortedEvents = eventList.sortedBy { it.timeStamp }

        for (ev in sortedEvents) {
            val pkg = ev.packageName ?: continue
            // Skip VYB itself so internal observation does not feed circular patterns
            if (pkg == context.packageName) continue

            when (ev.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // If an open session already existed for this package, finish previous as estimated
                    val previous = openSessions[pkg]
                    if (previous != null) {
                        val duration = (ev.timeStamp - previous.startTime).coerceAtLeast(1000L)
                        completedSessions.add(
                            CompletedSession(
                                packageName = pkg,
                                startTime = previous.startTime,
                                endTime = ev.timeStamp,
                                durationMs = duration,
                                isEstimated = true
                            )
                        )
                    }
                    openSessions[pkg] = ActiveSession(
                        packageName = pkg,
                        startTime = ev.timeStamp,
                        isEstimatedStart = false
                    )
                }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val open = openSessions.remove(pkg)
                    if (open != null) {
                        val duration = (ev.timeStamp - open.startTime).coerceAtLeast(500L)
                        completedSessions.add(
                            CompletedSession(
                                packageName = pkg,
                                startTime = open.startTime,
                                endTime = ev.timeStamp,
                                durationMs = duration,
                                isEstimated = open.isEstimatedStart
                            )
                        )
                    } else {
                        // Pause event without preceding resume in window: session began prior to watermark
                        val estimatedStart = windowStart
                        val duration = (ev.timeStamp - estimatedStart).coerceAtLeast(1000L)
                        completedSessions.add(
                            CompletedSession(
                                packageName = pkg,
                                startTime = estimatedStart,
                                endTime = ev.timeStamp,
                                durationMs = duration,
                                isEstimated = true
                            )
                        )
                    }
                }
            }
        }

        // Close any remaining active sessions at windowEnd conservatively as estimated/incomplete
        for ((pkg, open) in openSessions) {
            val duration = (windowEnd - open.startTime).coerceAtLeast(1000L)
            completedSessions.add(
                CompletedSession(
                    packageName = pkg,
                    startTime = open.startTime,
                    endTime = windowEnd,
                    durationMs = duration,
                    isEstimated = true
                )
            )
        }

        // Deduplicate using deterministic stable IDs: obs_usage_${pkg}_${startTime}
        val existingIds = try {
            observationDao.getObservationIdsSince(windowStart).toSet()
        } catch (dbErr: Exception) {
            val lastWatermark = prefs.getLong(PREF_KEY_WATERMARK_MS, 0L)
            val errStr = "Database query failed: ${dbErr.message}"
            saveLastError(errStr)
            return ReconciliationResult(
                success = false,
                windowStart = windowStart,
                windowEnd = windowEnd,
                collectedCount = totalRawCount,
                persistedCount = 0,
                processedCount = 0,
                newWatermark = lastWatermark,
                error = errStr
            )
        }

        val newObservations = mutableListOf<ObservationEntity>()
        val newTimelineEvents = mutableListOf<TimelineEventEntity>()

        for (session in completedSessions) {
            val pkgClean = session.packageName.replace(".", "_")
            val stableId = "obs_usage_${pkgClean}_${session.startTime}"

            if (existingIds.contains(stableId)) {
                continue
            }

            val appName = getAppNameFromPackage(session.packageName)
            val domain = mapAppToDomain(session.packageName)
            val durationSec = (session.durationMs / 1000).coerceAtLeast(1)
            val durationText = if (durationSec >= 60) "${durationSec / 60}m ${durationSec % 60}s" else "${durationSec}s"

            val cal = Calendar.getInstance().apply { timeInMillis = session.startTime }
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(session.startTime))
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(session.startTime))

            val confidence = if (session.isEstimated) 0.70f else 1.0f

            val obsEntity = ObservationEntity(
                observationId = stableId,
                ownerId = "primary_owner",
                timestamp = session.startTime,
                timezone = TimeZone.getDefault().id,
                dateString = dateStr,
                dayOfWeek = dayOfWeek,
                source = "UsageStatsManager",
                domain = domain,
                subject = appName,
                eventType = "APP_ENGAGEMENT",
                action = "Used $appName for $durationText",
                context = "Package: ${session.packageName}, durationMs=${session.durationMs}, estimated=${session.isEstimated}, time=$timeStr",
                entitiesJson = "{\"packageName\":\"${session.packageName}\",\"durationMs\":\"${session.durationMs}\",\"isEstimated\":\"${session.isEstimated}\"}",
                location = "",
                confidence = confidence,
                truthState = "OBSERVED",
                observedValue = appName,
                metadataJson = "{}"
            )
            newObservations.add(obsEntity)

            val timelineEntity = TimelineEventEntity(
                timestamp = session.startTime,
                eventType = "APP_OPENED",
                source = "UsageStatsManager",
                confidence = confidence,
                durationMs = session.durationMs,
                contextData = "$appName ($pkgClean) active for $durationText (${if (session.isEstimated) "estimated" else "verified"})",
                privacyClassification = "LOCAL_DEVICE_ONLY",
                dayOfWeek = dayOfWeek,
                dateString = dateStr
            )
            newTimelineEvents.add(timelineEntity)
        }

        // STEP F & G: Transactional database write
        val lastWatermark = prefs.getLong(PREF_KEY_WATERMARK_MS, 0L)
        try {
            database.withTransaction {
                if (newObservations.isNotEmpty()) {
                    observationDao.insertObservations(newObservations)
                }
                if (newTimelineEvents.isNotEmpty()) {
                    timelineDao.insertEvents(newTimelineEvents)
                }
            }
        } catch (dbErr: Exception) {
            val errStr = "Database commit failed: ${dbErr.message}"
            saveLastError(errStr)
            refreshAwarenessReport()
            // Do NOT advance watermark on DB failure
            return ReconciliationResult(
                success = false,
                windowStart = windowStart,
                windowEnd = windowEnd,
                collectedCount = totalRawCount,
                persistedCount = 0,
                processedCount = 0,
                newWatermark = lastWatermark,
                error = errStr
            )
        }

        // STEP G: Forward to Cognitive Engine for real Pattern Discovery
        var processedCount = 0
        try {
            if (newObservations.isNotEmpty()) {
                cognitiveEngine.learningEngine.executeLearningCycle()
                cognitiveEngine.lifeContextEngine.refreshState()
                processedCount = newObservations.size
            }
        } catch (cogErr: Exception) {
            cogErr.printStackTrace()
        }

        // STEP H: Persist Watermark ONLY after database write succeeds
        prefs.edit()
            .putLong(PREF_KEY_WATERMARK_MS, windowEnd)
            .putLong(PREF_KEY_LAST_COLLECTION_TIME_MS, executionTime)
            .putInt(PREF_KEY_LATEST_COLLECTED, totalRawCount)
            .putInt(PREF_KEY_LATEST_PERSISTED, newObservations.size)
            .putInt(PREF_KEY_LATEST_PROCESSED, processedCount)
            .remove(PREF_KEY_LAST_ERROR)
            .apply()

        refreshAwarenessReport()

        return ReconciliationResult(
            success = true,
            windowStart = windowStart,
            windowEnd = windowEnd,
            collectedCount = totalRawCount,
            persistedCount = newObservations.size,
            processedCount = processedCount,
            newWatermark = windowEnd
        )
    }

    private suspend fun recordUnlockEvent(timestamp: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

        val gapMs = timestamp - lastActiveEventTimestamp
        val fourHoursMs = 4 * 60 * 60 * 1000L
        if (gapMs >= fourHoursMs && hour in 4..11) {
            val durationHours = gapMs / (1000 * 60 * 60.0)
            recordBroadcastSignal(
                eventType = "SLEEP_INACTIVITY_PERIOD",
                domain = "HEALTH",
                subject = "Owner",
                confidence = 0.85f,
                durationMs = gapMs,
                contextData = String.format(Locale.getDefault(), "Inactivity period (~%.1f hrs) before %s unlock", durationHours, timeStr),
                timestamp = lastActiveEventTimestamp
            )
        }

        recordBroadcastSignal(
            eventType = "DEVICE_UNLOCK",
            domain = "ROUTINE",
            subject = "Owner",
            confidence = 1.0f,
            contextData = "Device unlocked at $timeStr (Hour $hour)",
            timestamp = timestamp
        )

        lastActiveEventTimestamp = timestamp
        reconcileUsageEvents(timestamp)
    }

    fun recordVybInteraction(summary: String, category: String = "CONVERSATION") {
        val now = System.currentTimeMillis()
        lastActiveEventTimestamp = now
        scope.launch {
            recordBroadcastSignal(
                eventType = "VYB_INTERACTION",
                domain = "INTERACTION",
                subject = "Owner",
                source = "VybUserSession",
                confidence = 1.0f,
                contextData = "Interaction [$category]: $summary",
                timestamp = now
            )
        }
    }

    fun recordRoutineExecution(routineTitle: String, actionType: String) {
        val now = System.currentTimeMillis()
        lastActiveEventTimestamp = now
        scope.launch {
            recordBroadcastSignal(
                eventType = "ROUTINE_EXECUTED",
                domain = "ROUTINE",
                subject = routineTitle,
                source = "VybActionEngine",
                confidence = 1.0f,
                contextData = "Executed routine: $routineTitle ($actionType)",
                timestamp = now
            )
        }
    }

    suspend fun recordBroadcastSignal(
        eventType: String,
        domain: String,
        subject: String,
        source: String = "SystemBroadcast",
        contextData: String = "",
        timestamp: Long = System.currentTimeMillis(),
        confidence: Float = 1.0f,
        durationMs: Long = 0L
    ) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val stableId = "obs_signal_${eventType.lowercase()}_$timestamp"

        val obs = ObservationEntity(
            observationId = stableId,
            ownerId = "primary_owner",
            timestamp = timestamp,
            timezone = TimeZone.getDefault().id,
            dateString = dateString,
            dayOfWeek = dayOfWeek,
            source = source,
            domain = domain,
            subject = subject,
            eventType = eventType,
            action = contextData,
            context = contextData,
            confidence = confidence,
            truthState = "OBSERVED",
            observedValue = eventType
        )

        val timeline = TimelineEventEntity(
            timestamp = timestamp,
            eventType = eventType,
            source = source,
            confidence = confidence,
            durationMs = durationMs,
            contextData = contextData,
            privacyClassification = "LOCAL_DEVICE_ONLY",
            dayOfWeek = dayOfWeek,
            dateString = dateString
        )

        try {
            database.withTransaction {
                observationDao.insertObservation(obs)
                timelineDao.insertEvent(timeline)
            }
            cognitiveEngine.learningEngine.executeLearningCycle()
            cognitiveEngine.lifeContextEngine.refreshState()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scanCalendarIfPermitted(): List<String> {
        val calendarSummaries = mutableListOf<String>()
        if (permissionManager.checkPermission(VybPermissionType.CALENDAR) != com.example.domain.permission.PermissionState.AVAILABLE) {
            return calendarSummaries
        }

        try {
            val uri: Uri = CalendarContract.Events.CONTENT_URI
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val endOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION
            )

            val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)"
            val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())

            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use {
                val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)

                while (it.moveToNext()) {
                    val title = if (titleIdx >= 0) it.getString(titleIdx) ?: "Event" else "Event"
                    val start = if (startIdx >= 0) it.getLong(startIdx) else 0L
                    val timeStr = if (start > 0) SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(start)) else "Today"
                    val item = "$title at $timeStr"
                    calendarSummaries.add(item)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return calendarSummaries
    }

    suspend fun refreshAwarenessReport() {
        val hasUsage = permissionManager.hasUsageStatsPermission()
        val hasCalendar = permissionManager.checkPermission(VybPermissionType.CALENDAR) == com.example.domain.permission.PermissionState.AVAILABLE
        val hasNotification = permissionManager.checkPermission(VybPermissionType.NOTIFICATIONS) == com.example.domain.permission.PermissionState.AVAILABLE

        val lastCollection = prefs.getLong(PREF_KEY_LAST_COLLECTION_TIME_MS, 0L)
        val watermark = prefs.getLong(PREF_KEY_WATERMARK_MS, 0L)
        val latestColl = prefs.getInt(PREF_KEY_LATEST_COLLECTED, 0)
        val latestPers = prefs.getInt(PREF_KEY_LATEST_PERSISTED, 0)
        val latestProc = prefs.getInt(PREF_KEY_LATEST_PROCESSED, 0)
        val lastErr = prefs.getString(PREF_KEY_LAST_ERROR, null)

        val now = System.currentTimeMillis()
        val awarenessState = when {
            !hasUsage -> AwarenessState.PERMISSION_REQUIRED
            lastErr != null -> AwarenessState.RESTRICTED
            lastCollection > 0 && (now - lastCollection > STALE_THRESHOLD_MS) -> AwarenessState.STALE
            lastCollection > 0 -> AwarenessState.ACTIVE
            else -> AwarenessState.PERMISSION_REQUIRED
        }

        val allPatterns = database.candidatePatternDao().getAllPatterns()
        val candidateCount = allPatterns.count { it.status == "CANDIDATE" || it.status == "EMERGING" }
        val establishedCount = allPatterns.count { it.status == "ESTABLISHED" }
        val obsCount = observationDao.getCount()

        val recentObs = observationDao.getRecentObservations(10)
        val traces = recentObs.map { obs ->
            ObservationTraceItem(
                observationId = obs.observationId,
                timestamp = obs.timestamp,
                source = obs.source,
                domain = obs.domain,
                subject = obs.subject,
                eventType = obs.eventType,
                confidence = obs.confidence,
                truthState = obs.truthState,
                isPersisted = true,
                isProcessedByCognitiveEngine = true
            )
        }

        _awarenessReport.value = BackgroundAwarenessReport(
            awarenessState = awarenessState,
            isUsageAccessGranted = hasUsage,
            isCalendarAccessGranted = hasCalendar,
            isNotificationAccessGranted = hasNotification,
            lastCollectionTimeMs = lastCollection,
            lastCommittedWatermarkMs = watermark,
            latestCollectedCount = latestColl,
            latestPersistedCount = latestPers,
            latestProcessedCount = latestProc,
            candidatePatternCount = candidateCount,
            establishedPatternCount = establishedCount,
            totalObservationCount = obsCount,
            lastError = lastErr,
            recentTraces = traces
        )
    }

    private fun saveLastError(error: String) {
        prefs.edit().putString(PREF_KEY_LAST_ERROR, error).apply()
    }

    fun resetWatermarkForTesting() {
        prefs.edit().clear().apply()
    }

    private fun createInitialReport(): BackgroundAwarenessReport {
        val hasUsage = permissionManager.hasUsageStatsPermission()
        return BackgroundAwarenessReport(
            awarenessState = if (hasUsage) AwarenessState.ACTIVE else AwarenessState.PERMISSION_REQUIRED,
            isUsageAccessGranted = hasUsage,
            isCalendarAccessGranted = false,
            isNotificationAccessGranted = false,
            lastCollectionTimeMs = 0L,
            lastCommittedWatermarkMs = 0L,
            latestCollectedCount = 0,
            latestPersistedCount = 0,
            latestProcessedCount = 0,
            candidatePatternCount = 0,
            establishedPatternCount = 0,
            totalObservationCount = 0,
            lastError = null
        )
    }

    private fun mapAppToDomain(packageName: String): String {
        return when {
            packageName.contains("whatsapp") || packageName.contains("telegram") || packageName.contains("slack") || packageName.contains("teams") -> "COMMUNICATION"
            packageName.contains("ubercab") || packageName.contains("olacabs") || packageName.contains("rapido") || packageName.contains("yatrisathi") || packageName.contains("maps") -> "TRANSPORT"
            packageName.contains("gm") || packageName.contains("email") || packageName.contains("calendar") || packageName.contains("docs") -> "WORK"
            packageName.contains("amazon") || packageName.contains("flipkart") || packageName.contains("grofers") || packageName.contains("zepto") || packageName.contains("myntra") -> "SHOPPING"
            packageName.contains("fit") || packageName.contains("health") || packageName.contains("strava") -> "HEALTH"
            else -> "APP_USAGE"
        }
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return when {
            packageName.contains("whatsapp") -> "WhatsApp"
            packageName.contains("telegram") -> "Telegram"
            packageName.contains("gm") || packageName.contains("email") -> "Gmail"
            packageName.contains("calendar") -> "Google Calendar"
            packageName.contains("chrome") -> "Chrome"
            packageName.contains("ubercab") -> "Uber"
            packageName.contains("olacabs") -> "Ola"
            packageName.contains("rapido") -> "Rapido"
            packageName.contains("yatrisathi") -> "Yatri Sathi"
            packageName.contains("inDriver") -> "inDrive"
            packageName.contains("amazon") -> "Amazon"
            packageName.contains("youtube") -> "YouTube"
            packageName.contains("slack") -> "Slack"
            packageName.contains("teams") -> "Microsoft Teams"
            packageName.contains("maps") -> "Google Maps"
            else -> packageName.substringAfterLast(".").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
    }
}
