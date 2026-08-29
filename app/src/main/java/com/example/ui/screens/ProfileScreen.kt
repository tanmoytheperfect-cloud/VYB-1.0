package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.domain.permission.PermissionState
import com.example.ui.components.VybGlowCard
import com.example.ui.components.VybTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

@Composable
fun ProfileScreen(
    viewModel: VybViewModel
) {
    val context = LocalContext.current
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val ownerEmail by viewModel.ownerEmail.collectAsStateWithLifecycle()
    val isLockEnabled by viewModel.isSecurityLockEnabled.collectAsStateWithLifecycle()
    val currentPin by viewModel.securityPin.collectAsStateWithLifecycle()
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()

    val assistantTone by viewModel.assistantTone.collectAsStateWithLifecycle()
    val dietPref by viewModel.userDietPreference.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val locationTelemetry by viewModel.locationTelemetry.collectAsStateWithLifecycle()
    val systemTelemetry by viewModel.systemTelemetry.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val permissionReports by viewModel.permissionReports.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    val toneOptions = listOf(
        "JARVIS (Stark AI Precision)",
        "Concise & Action-Oriented",
        "Detailed & Analytical",
        "Executive Brief"
    )
    val dietOptions = listOf("High Protein", "Balanced", "Keto / Low Carb", "Plant-Based / Vegan")
    val currencyOptions = listOf("₹ (INR)", "$ (USD)", "€ (EUR)", "£ (GBP)")

    val hasCustomKey = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    Scaffold(
        topBar = {
            VybTopBar(
                title = "Owner & AI Profile",
                subtitle = "Security, memory & personalization"
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Owner Profile Card Header
            item {
                VybGlowCard(
                    borderColor = CyanNeon.copy(alpha = 0.6f),
                    glowColor = CyanGlow
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(Slate850, Slate800)))
                                        .border(1.5.dp, Brush.linearGradient(listOf(CyanNeon, VioletNeon)), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ownerName.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = CyanNeon,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = ownerName,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Slate100,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldNeon.copy(alpha = 0.2f),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, EmeraldNeon)
                                        ) {
                                            Text(
                                                text = "OWNER",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldNeon,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = ownerEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.testTag("edit_owner_profile_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Owner Profile",
                                    tint = CyanNeon
                                )
                            }
                        }

                        HorizontalDivider(color = Slate800, thickness = 1.dp)

                        Button(
                            onClick = { viewModel.resetOnboarding() },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanNeon),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("restart_induction_btn")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Re-run 4-Step Executive Induction Setup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Security & App Verification Section
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "OWNER SECURITY & VERIFICATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Verification on Open Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (isLockEnabled) CyanNeon else Slate500,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Verification on App Open",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Slate100,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Prompt 4-digit PIN / Biometric when launching VYB",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = isLockEnabled,
                                    onCheckedChange = { viewModel.setSecurityLockEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Slate950,
                                        checkedTrackColor = CyanNeon
                                    ),
                                    modifier = Modifier.testTag("app_lock_switch")
                                )
                            }

                            HorizontalDivider(color = Slate800, thickness = 1.dp)

                            // Change PIN & Lock Now actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showChangePinDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700),
                                    modifier = Modifier.weight(1f).testTag("change_pin_btn")
                                ) {
                                    Icon(Icons.Default.Pin, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Change PIN", color = Slate200, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }

                                Button(
                                    onClick = { viewModel.lockApp() },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                                    modifier = Modifier.weight(1f).testTag("lock_now_btn")
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Lock App Now", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Transparent Android Permissions & Observation Telemetry
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ANDROID PERMISSIONS & OBSERVATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = { viewModel.refreshPermissionReports() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh permissions",
                                tint = CyanNeon,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "VYB only observes authorized Android signals. No fake data, no hidden services, and no unauthorized background tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate300,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )

                            permissionReports.forEach { report ->
                                val (statusColor, statusText) = when (report.state) {
                                    PermissionState.AVAILABLE -> Pair(EmeraldNeon, "AUTHORIZED")
                                    PermissionState.NOT_GRANTED -> Pair(AmberNeon, "NOT GRANTED")
                                    PermissionState.NOT_REQUIRED -> Pair(Slate400, "BUILT-IN")
                                    PermissionState.NOT_SUPPORTED -> Pair(CrimsonNeon, "UNSUPPORTED")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = report.type.displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Slate100,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = statusColor.copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor)
                                            ) {
                                                Text(
                                                    text = statusText,
                                                    color = statusColor,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = report.type.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400,
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp
                                        )
                                    }

                                    if (report.state == PermissionState.NOT_GRANTED) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val intent = if (report.type == com.example.domain.permission.VybPermissionType.USAGE_STATS) {
                                                        viewModel.permissionManager.createUsageStatsSettingsIntent()
                                                    } else {
                                                        viewModel.permissionManager.createAppSettingsIntent()
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    val appSettingsIntent = viewModel.permissionManager.createAppSettingsIntent()
                                                    context.startActivity(appSettingsIntent)
                                                }
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f))
                                        ) {
                                            Text("Grant", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                HorizontalDivider(color = Slate800, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // JARVIS Core Telemetry & Location Diagnostics
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JARVIS LOCATION & TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "ONLINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldNeon,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CapabilityStatusRow(
                                label = "Detected City",
                                value = "${locationTelemetry.city}, ${locationTelemetry.state}",
                                color = CyanNeon
                            )
                            CapabilityStatusRow(
                                label = "Country & Region",
                                value = "${locationTelemetry.country} (${locationTelemetry.countryCode})",
                                color = Slate100
                            )
                            CapabilityStatusRow(
                                label = "GPS Coordinates",
                                value = locationTelemetry.formattedCoordinates,
                                color = AmberNeon
                            )
                            CapabilityStatusRow(
                                label = "System Currency",
                                value = "${locationTelemetry.currencySymbol} (${locationTelemetry.currencyCode})",
                                color = EmeraldNeon
                            )
                            CapabilityStatusRow(
                                label = "Arc Reactor Core",
                                value = systemTelemetry.arcReactorCorePower,
                                color = CyanNeon
                            )
                            CapabilityStatusRow(
                                label = "Diagnostic Status",
                                value = systemTelemetry.systemStatus,
                                color = EmeraldNeon
                            )

                            HorizontalDivider(color = Slate800, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.refreshLocation() },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).testTag("refresh_location_btn")
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Refresh GPS", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (isSpeaking) {
                                            viewModel.stopSpeaking()
                                        } else {
                                            viewModel.speakJarvisGreeting()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSpeaking) CrimsonNeon.copy(alpha = 0.3f) else VioletNeon.copy(alpha = 0.3f)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSpeaking) CrimsonNeon else VioletNeon
                                    ),
                                    modifier = Modifier.weight(1f).testTag("test_jarvis_voice_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = if (isSpeaking) CrimsonNeon else VioletNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isSpeaking) "Stop Voice" else "JARVIS Voice",
                                        color = Slate100,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Memory Vault Summary Card
            item {
                VybGlowCard(
                    borderColor = VioletNeon.copy(alpha = 0.4f),
                    glowColor = VioletGlow
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(VioletNeon.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = "Vault",
                                    tint = VioletNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Owner Memory Vault",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Slate100,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${ownerAccounts.size} Accounts • Amazon, Flipkart, WhatsApp, Mails",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate800
                        ) {
                            Text(
                                text = "Hub Tab 4",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeon,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Proactivity Level Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AI PROACTIVITY LEVEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "LEVEL 4 // JARVIS",
                            color = CyanNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val proactivityLevels = listOf(
                        Triple(1, "1. Quiet", "Responds only when explicitly spoken to"),
                        Triple(2, "2. Helpful", "Gentle deadline & critical calendar nudges"),
                        Triple(3, "3. Proactive", "Suggests school cab, grocery restock & discretionary buffer"),
                        Triple(4, "4. JARVIS", "Full autonomous life OS: proactive briefings, drafts & situational intelligence")
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            proactivityLevels.forEach { (lvl, title, desc) ->
                                val isSelected = lvl == 4 // JARVIS default
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Slate800 else Color.Transparent,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)) else null,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isSelected) CyanNeon else Slate300,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                text = desc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Slate400,
                                                fontSize = 11.sp
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = CyanNeon,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Assistant Response Tone Selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AI RESPONSE STYLE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            toneOptions.forEach { tone ->
                                val isSelected = assistantTone == tone
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Slate800 else Color.Transparent,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updatePreferences(tone, dietPref, currency) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tone,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) CyanNeon else Slate300,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = CyanNeon,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Nutrition & Lifestyle Preference
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DEFAULT NUTRITION PREFERENCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            dietOptions.forEach { diet ->
                                val isSelected = dietPref == diet
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Slate800 else Color.Transparent,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.5f)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.updatePreferences(assistantTone, diet, currency) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = diet,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSelected) EmeraldNeon else Slate300,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = EmeraldNeon,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI Model & Architecture Status
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AI ENGINE & CAPABILITIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CapabilityStatusRow("AI Model", "Gemini 3.5 Flash", CyanNeon)
                            CapabilityStatusRow("Intent Router", "Real-time Multi-Domain Classifier", VioletNeon)
                            CapabilityStatusRow("Local Persistence", "Room SQLite Database", EmeraldNeon)
                            CapabilityStatusRow("Voice Recognition", "Android Speech Recognizer", AmberNeon)
                            CapabilityStatusRow("Active Modules", "Diet, Shopping, Travel, Tasks, Finance, Email", Slate300)
                        }
                    }
                }
            }

            // API Key Status Guide
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (hasCustomKey) EmeraldNeon else AmberNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (hasCustomKey) "Gemini API Key Active" else "AI Studio Environment Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = Slate100,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "VYB runs seamlessly in AI Studio with built-in intent parsing and fallback smart generation. Configure GEMINI_API_KEY in the Secrets panel for live cloud API keys.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showEditProfileDialog) {
        var newName by remember { mutableStateOf(ownerName) }
        var newEmail by remember { mutableStateOf(ownerEmail) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = Slate900,
            title = {
                Text("Edit Owner Profile", color = Slate100, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Owner Name", color = Slate400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Owner Email", color = Slate400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateOwnerProfile(newName, newEmail)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
                ) {
                    Text("Save", color = Slate950, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }

    if (showChangePinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            containerColor = Slate900,
            title = {
                Text("Change 4-Digit Security PIN", color = Slate100, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Current PIN: $currentPin", style = MaterialTheme.typography.bodySmall, color = Slate400)
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        label = { Text("New 4-Digit PIN", color = Slate400) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate700
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text("Please enter a valid 4-digit PIN", color = CrimsonNeon, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            viewModel.setSecurityPin(pinInput)
                            showChangePinDialog = false
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberNeon)
                ) {
                    Text("Update PIN", color = Slate950, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

@Composable
private fun CapabilityStatusRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Slate400)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
    }
}
