package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.environment.model.*
import com.example.domain.temporal.VybTimeOfDay
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Compact Living Signals surface framing the owner's immediate world:
 * TIME + WEATHER + AIR QUALITY + STEPS + ACTIVITY + LIFE CONTEXT.
 * Prioritizes relevance based on time of day and truth state.
 */
@Composable
fun VybLivingSignalsSection(
    signals: LivingSignals,
    timeOfDay: VybTimeOfDay,
    onRefresh: () -> Unit = {},
    onRequestLocationPermission: () -> Unit = {},
    onRequestActivityPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val air = signals.airQuality
    val weather = signals.weather
    val steps = signals.steps
    val activity = signals.activity
    val bloodOxygen = signals.bloodOxygen

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_living_signals_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==============================================================
        // 1. PRIMARY ENVIRONMENTAL CARD: AIR QUALITY & TEMPERATURE
        // ==============================================================
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.70f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2238BDF8)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("living_signals_air_weather_card")
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRefresh
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    when (air?.level) {
                                        AirQualityLevel.GOOD -> Color(0xFF10B981)
                                        AirQualityLevel.MODERATE -> Color(0xFFFBBF24)
                                        AirQualityLevel.POOR -> Color(0xFFF97316)
                                        AirQualityLevel.VERY_POOR -> Color(0xFFEF4444)
                                        AirQualityLevel.HAZARDOUS -> Color(0xFF8B5CF6)
                                        null -> Color(0xFF64748B)
                                    }
                                )
                        )
                        Text(
                            text = "AIR & ENVIRONMENT",
                            color = Slate300,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                            fontSize = 11.sp
                        )
                    }

                    // Freshness indicator
                    val freshnessText = when {
                        !signals.isOnline -> "Offline"
                        air != null && air.truthState == MetricTruthState.KNOWN -> signals.getFreshnessString(air.timestamp)
                        weather != null && weather.truthState == MetricTruthState.KNOWN -> signals.getFreshnessString(weather.timestamp)
                        else -> "Standby"
                    }
                    Text(
                        text = freshnessText,
                        color = Slate400,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Temperature and Weather condition
                    Column {
                        if (weather != null && weather.truthState == MetricTruthState.KNOWN) {
                            Text(
                                text = "${weather.tempC.toInt()}°C",
                                color = Slate100,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Feels like ${weather.feelsLikeC.toInt()}°C • ${weather.conditionText}",
                                color = Slate300,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp
                            )
                        } else if (signals.location.truthState == MetricTruthState.PERMISSION_REQUIRED) {
                            Text(
                                text = "Weather Standby",
                                color = Slate300,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Location permission needed",
                                color = CyanNeon,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable { onRequestLocationPermission() }
                            )
                        } else {
                            Text(
                                text = "--°C",
                                color = Slate400,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Connecting to environmental sensors...",
                                color = Slate400,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Right Column: Air Quality & Interpretation
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        if (air != null && air.truthState == MetricTruthState.KNOWN) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "AQI",
                                    color = Slate400,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${air.aqi}",
                                    color = when (air.level) {
                                        AirQualityLevel.GOOD -> Color(0xFF10B981)
                                        AirQualityLevel.MODERATE -> Color(0xFFFBBF24)
                                        AirQualityLevel.POOR -> Color(0xFFF97316)
                                        AirQualityLevel.VERY_POOR, AirQualityLevel.HAZARDOUS -> Color(0xFFEF4444)
                                    },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = air.level.label,
                                color = Slate200,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        } else if (signals.location.truthState == MetricTruthState.PERMISSION_REQUIRED) {
                            Text(
                                text = "AQI --",
                                color = Slate400,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Location required",
                                color = Slate400,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "AQI --",
                                color = Slate400,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Awaiting live telemetry",
                                color = Slate400,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // ==============================================================
        // 2. TWIN METRICS: STEPS TODAY & CURRENT ACTIVITY STATE
        // ==============================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Steps Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2238BDF8)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("living_signals_steps_card")
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRequestActivityPermission
                    )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STEPS TODAY",
                            color = Slate400,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                            contentDescription = "Steps",
                            tint = CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (steps.truthState == MetricTruthState.KNOWN) {
                        Text(
                            text = NumberFormat.getNumberInstance(Locale.US).format(steps.steps),
                            color = Slate100,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${steps.distanceKm} km • ${steps.activeMinutes} min active",
                            color = Slate300,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    } else if (steps.truthState == MetricTruthState.PERMISSION_REQUIRED) {
                        Text(
                            text = "--",
                            color = Slate400,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Activity permission needed",
                            color = CyanNeon,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp
                        )
                    } else {
                        Text(
                            text = "0",
                            color = Slate200,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Hardware sensor standby",
                            color = Slate400,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Activity State Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.65f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x228B5CF6)),
                modifier = Modifier
                    .weight(1f)
                    .testTag("living_signals_activity_card")
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVITY",
                            color = Slate400,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                        Icon(
                            imageVector = when (activity.state) {
                                ActivityState.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
                                ActivityState.RUNNING -> Icons.AutoMirrored.Filled.DirectionsRun
                                ActivityState.CYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
                                ActivityState.DRIVING -> Icons.Default.DirectionsCar
                                else -> Icons.Default.SelfImprovement
                            },
                            contentDescription = "Activity State",
                            tint = VioletNeon,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = activity.state.label,
                        color = Slate100,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${activity.truthState.name} • ${activity.source.take(18)}",
                        color = Slate400,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // ==============================================================
        // 3. AUXILIARY / ENVIRONMENT BAR (HUMIDITY • UV • BLOOD OXYGEN STATUS)
        // ==============================================================
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0F172A).copy(alpha = 0.50f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("living_signals_auxiliary_bar")
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 9.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (weather != null && weather.truthState == MetricTruthState.KNOWN) {
                        Text(
                            text = "Humidity ${weather.humidity}%",
                            color = Slate300,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "•",
                            color = Slate500,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "UV ${weather.uvIndex.toInt()}",
                            color = Slate300,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "Local Sensors Active",
                            color = Slate300,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp
                        )
                    }
                }

                // Truthful Blood Oxygen SpO2 indicator (no fake software estimation)
                if (bloodOxygen.spo2Percentage != null) {
                    Text(
                        text = "SpO₂ ${bloodOxygen.spo2Percentage}%",
                        color = EmeraldNeon,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "SpO2",
                            tint = Slate400,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "SpO₂: Sensor required",
                            color = Slate400,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
