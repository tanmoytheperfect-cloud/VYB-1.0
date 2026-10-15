package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.observer.AwarenessState
import com.example.domain.observer.BackgroundAwarenessReport
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackgroundAwarenessSection(
    report: BackgroundAwarenessReport,
    onTriggerReconciliation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showDeveloperTraces by remember { mutableStateOf(false) }

    val stateColor = Color(report.awarenessState.colorHex)
    val timeFormat = remember { SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault()) }

    val lastCollectionStr = if (report.lastCollectionTimeMs > 0) {
        timeFormat.format(Date(report.lastCollectionTimeMs))
    } else {
        "Never"
    }

    val watermarkStr = if (report.lastCommittedWatermarkMs > 0) {
        timeFormat.format(Date(report.lastCommittedWatermarkMs))
    } else {
        "None (First Run)"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBackgroundElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("background_awareness_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Title & Awareness Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Background Awareness",
                            tint = CyanNeon,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Background Awareness",
                            color = Slate100,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Honest Usage Ledger & Pattern Discovery",
                            color = Slate400,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }

                // Awareness State Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = stateColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, stateColor.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("awareness_state_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                        Text(
                            text = report.awarenessState.label.uppercase(),
                            color = stateColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate800, thickness = 0.5.dp)

            // Sources Enabled & Permission Indicators
            Text(
                text = "SIGNALS & PERMISSION TRUTH",
                color = Slate400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Usage Stats Source Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (report.isUsageAccessGranted) EmeraldNeon.copy(alpha = 0.1f) else AmberNeon.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (report.isUsageAccessGranted) EmeraldNeon.copy(alpha = 0.4f) else AmberNeon.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (!report.isUsageAccessGranted) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        .testTag("source_usage_chip")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (report.isUsageAccessGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (report.isUsageAccessGranted) EmeraldNeon else AmberNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Usage Stats",
                                color = Slate200,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (report.isUsageAccessGranted) "Granted" else "Tap to Grant",
                            color = if (report.isUsageAccessGranted) EmeraldNeon else AmberNeon,
                            fontSize = 10.sp
                        )
                    }
                }

                // Calendar Source Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (report.isCalendarAccessGranted) EmeraldNeon.copy(alpha = 0.1f) else Slate800,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (report.isCalendarAccessGranted) EmeraldNeon.copy(alpha = 0.4f) else Slate700
                    ),
                    modifier = Modifier.weight(1f).testTag("source_calendar_chip")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (report.isCalendarAccessGranted) Icons.Default.CheckCircle else Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (report.isCalendarAccessGranted) EmeraldNeon else Slate400,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Calendar",
                                color = Slate200,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (report.isCalendarAccessGranted) "Active" else "Optional",
                            color = if (report.isCalendarAccessGranted) EmeraldNeon else Slate400,
                            fontSize = 10.sp
                        )
                    }
                }

                // System Broadcasts Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EmeraldNeon.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f).testTag("source_broadcast_chip")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Broadcasts",
                                color = Slate200,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Unlocks & Power",
                            color = EmeraldNeon,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Collection Watermark & Timestamp Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(InputBackground)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Last Collection:", color = Slate400, fontSize = 11.sp)
                    Text(lastCollectionStr, color = Slate200, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.testTag("diag_last_collection"))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Committed Watermark:", color = Slate400, fontSize = 11.sp)
                    Text(watermarkStr, color = CyanNeon, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag("diag_watermark"))
                }
            }

            // Pipeline Throughput Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(InputBackground)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Collected", color = Slate400, fontSize = 10.sp)
                    Text("${report.latestCollectedCount}", color = Slate100, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.testTag("metric_collected_count"))
                    Text("raw events", color = Slate500, fontSize = 9.sp)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(InputBackground)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Persisted", color = Slate400, fontSize = 10.sp)
                    Text("${report.latestPersistedCount}", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.testTag("metric_persisted_count"))
                    Text("to ledger", color = Slate500, fontSize = 9.sp)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(InputBackground)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Processed", color = Slate400, fontSize = 10.sp)
                    Text("${report.latestProcessedCount}", color = EmeraldNeon, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.testTag("metric_processed_count"))
                    Text("by brain", color = Slate500, fontSize = 9.sp)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(InputBackground)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Patterns", color = Slate400, fontSize = 10.sp)
                    Text("${report.candidatePatternCount + report.establishedPatternCount}", color = VioletNeon, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.testTag("metric_patterns_count"))
                    Text("${report.establishedPatternCount} estab.", color = Slate500, fontSize = 9.sp)
                }
            }

            // Error Display (if any)
            if (report.lastError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CrimsonNeon.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("awareness_error_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = CrimsonNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = report.lastError,
                            color = CrimsonNeon,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Action Buttons: Reconcile Now & Developer Traces Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onTriggerReconciliation,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("reconcile_now_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reconcile Now", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                TextButton(
                    onClick = { showDeveloperTraces = !showDeveloperTraces },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("toggle_developer_traces_button")
                ) {
                    Text(
                        text = if (showDeveloperTraces) "Hide Traces" else "Diagnostic Traces",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }

            // Developer Diagnostics View: Traces from collection -> persistence -> cognitive processing
            AnimatedVisibility(visible = showDeveloperTraces) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate900)
                        .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "END-TO-END PIPELINE TRACE (LATEST 10)",
                            color = CyanNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Total DB: ${report.totalObservationCount}",
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }

                    if (report.recentTraces.isEmpty()) {
                        Text(
                            text = "No observations recorded yet. Grant Usage Access and use device apps, or tap Reconcile Now.",
                            color = Slate500,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        report.recentTraces.forEach { trace ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate950,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, Slate800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = trace.subject,
                                            color = Slate100,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = timeFormat.format(Date(trace.timestamp)),
                                            color = Slate400,
                                            fontSize = 9.sp
                                        )
                                    }

                                    Text(
                                        text = "ID: ${trace.observationId}",
                                        color = Slate500,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Src: ${trace.source}", color = Slate400, fontSize = 9.sp)
                                        Text("•", color = Slate600, fontSize = 9.sp)
                                        Text("Dom: ${trace.domain}", color = Slate400, fontSize = 9.sp)
                                        Spacer(modifier = Modifier.weight(1f))

                                        // Persisted indicator
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EmeraldNeon.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, EmeraldNeon)
                                        ) {
                                            Text(
                                                text = "PERSISTED",
                                                color = EmeraldNeon,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                        // Processed indicator
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = CyanNeon.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, CyanNeon)
                                        ) {
                                            Text(
                                                text = "PROCESSED",
                                                color = CyanNeon,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
