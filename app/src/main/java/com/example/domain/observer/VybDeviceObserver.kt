package com.example.domain.observer

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import com.example.data.local.dao.TimelineDao
import com.example.data.local.entity.TimelineEventEntity
import com.example.domain.permission.VybPermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VybDeviceObserver(
    private val context: Context,
    private val timelineDao: TimelineDao,
    private val permissionManager: VybPermissionManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastActiveEventTimestamp: Long = System.currentTimeMillis()
    private var isReceiverRegistered = false

    private val deviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            val now = System.currentTimeMillis()

            scope.launch {
                when (action) {
                    Intent.ACTION_USER_PRESENT -> {
                        // Legitimate morning / day phone unlock
                        recordUnlockEvent(now)
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        recordEvent(
                            eventType = "SCREEN_ON",
                            source = "SystemBroadcast",
                            contextData = "Screen turned on",
                            timestamp = now
                        )
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        lastActiveEventTimestamp = now
                        recordEvent(
                            eventType = "SCREEN_OFF",
                            source = "SystemBroadcast",
                            contextData = "Screen turned off / Inactivity onset",
                            timestamp = now
                        )
                    }
                    Intent.ACTION_BOOT_COMPLETED -> {
                        recordEvent(
                            eventType = "DEVICE_BOOT",
                            source = "SystemBroadcast",
                            contextData = "Device boot completed",
                            timestamp = now
                        )
                    }
                    Intent.ACTION_POWER_CONNECTED -> {
                        recordEvent(
                            eventType = "POWER_CONNECTED",
                            source = "SystemBroadcast",
                            contextData = "Device plugged in / charging",
                            timestamp = now
                        )
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        recordEvent(
                            eventType = "POWER_DISCONNECTED",
                            source = "SystemBroadcast",
                            contextData = "Device unplugged",
                            timestamp = now
                        )
                    }
                }
            }
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
        // Initial scan of legitimate usage stats & calendar if granted
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

    private suspend fun recordUnlockEvent(timestamp: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

        // Check if there was a preceding long inactivity period (e.g. > 4 hours gap during night)
        val gapMs = timestamp - lastActiveEventTimestamp
        val fourHoursMs = 4 * 60 * 60 * 1000L
        if (gapMs >= fourHoursMs && hour in 4..11) {
            val durationHours = gapMs / (1000 * 60 * 60.0)
            recordEvent(
                eventType = "SLEEP_INACTIVITY_PERIOD",
                source = "InactivityDetectionEngine",
                confidence = 0.85f,
                durationMs = gapMs,
                contextData = String.format(Locale.getDefault(), "Long inactivity period (~%.1f hrs) before %s unlock", durationHours, timeStr),
                timestamp = lastActiveEventTimestamp
            )
        }

        recordEvent(
            eventType = "DEVICE_UNLOCK",
            source = "SystemBroadcast",
            confidence = 1.0f,
            contextData = "Device unlocked at $timeStr (Hour $hour)",
            timestamp = timestamp
        )

        lastActiveEventTimestamp = timestamp

        // Also check if user opened other apps shortly after unlock
        pollUsageStatsIfPermitted()
    }

    fun recordVybInteraction(summary: String, category: String = "CONVERSATION") {
        val now = System.currentTimeMillis()
        lastActiveEventTimestamp = now
        scope.launch {
            recordEvent(
                eventType = "VYB_INTERACTION",
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
            recordEvent(
                eventType = "ROUTINE_EXECUTED",
                source = "VybActionEngine",
                confidence = 1.0f,
                contextData = "Executed routine: $routineTitle ($actionType)",
                timestamp = now
            )
        }
    }

    fun pollUsageStatsIfPermitted() {
        if (!permissionManager.hasUsageStatsPermission()) return

        scope.launch {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return@launch
                val endTime = System.currentTimeMillis()
                val startTime = endTime - (30 * 60 * 1000L) // Past 30 mins

                val events = usageStatsManager.queryEvents(startTime, endTime)
                val event = UsageEvents.Event()

                val recordedApps = mutableSetOf<String>()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        val pkg = event.packageName
                        if (pkg != context.packageName && !recordedApps.contains(pkg)) {
                            recordedApps.add(pkg)
                            val appName = getAppNameFromPackage(pkg)
                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(event.timeStamp))
                            recordEvent(
                                eventType = "APP_OPENED",
                                source = "UsageStatsManager",
                                confidence = 1.0f,
                                contextData = "$appName ($pkg) active at $timeStr",
                                timestamp = event.timeStamp
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun scanCalendarIfPermitted(): List<String> {
        val calendarSummaries = mutableListOf<String>()
        if (permissionManager.checkPermission(com.example.domain.permission.VybPermissionType.CALENDAR) != com.example.domain.permission.PermissionState.AVAILABLE) {
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

    private suspend fun recordEvent(
        eventType: String,
        source: String,
        contextData: String,
        timestamp: Long,
        confidence: Float = 1.0f,
        durationMs: Long = 0L
    ) {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        val entity = TimelineEventEntity(
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
        timelineDao.insertEvent(entity)
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return when {
            packageName.contains("whatsapp") -> "WhatsApp"
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
            else -> packageName.substringAfterLast(".").capitalize(Locale.getDefault())
        }
    }
}
