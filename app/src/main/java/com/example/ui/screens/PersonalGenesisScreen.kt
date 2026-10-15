package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.genesis.*
import com.example.ui.components.VybGlowCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

@Composable
fun PersonalGenesisScreen(
    viewModel: VybViewModel,
    onGenesisComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) } // 1..6

    val sourceSpecs by viewModel.genesisSourceSpecs.collectAsStateWithLifecycle()
    val personalProfile by viewModel.personalProfile.collectAsStateWithLifecycle()
    val contextGraph by viewModel.contextGraph.collectAsStateWithLifecycle()
    val digitalTwin by viewModel.personalDigitalTwin.collectAsStateWithLifecycle()
    val universalMetadata by viewModel.universalInstanceMetadata.collectAsStateWithLifecycle()
    val personalityStyle = personalProfile.communicationStyle

    // Permission launcher for runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        viewModel.refreshGenesisState()
    }

    Scaffold(
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Stepper Header
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VYB PERSONAL GENESIS",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.6.sp
                        )
                        val stepTitle = when (currentStep) {
                            1 -> "Welcome to VYB"
                            2 -> "Personal Life Scan"
                            3 -> "Personal Profile Engine"
                            4 -> "Personal Context Graph"
                            5 -> "Personality Adaptation"
                            else -> "Universal Digital Twin"
                        }
                        Text(
                            text = stepTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(
                            text = "STAGE $currentStep OF 6",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                // Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..6) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (i <= currentStep) CyanNeon else Slate800)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body Content
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentStep,
                    label = "genesis_screen_step"
                ) { step ->
                    when (step) {
                        1 -> GenesisStageWelcome(
                            onStartScan = { currentStep = 2 }
                        )
                        2 -> GenesisStageLifeScan(
                            sourceSpecs = sourceSpecs,
                            onToggleSource = { type, granted ->
                                viewModel.toggleGenesisSource(type, granted)
                            },
                            onRequestSystemPermission = { perm ->
                                permissionLauncher.launch(arrayOf(perm))
                            }
                        )
                        3 -> GenesisStageProfileEngine(
                            profile = personalProfile,
                            onUpdateProfile = { updated ->
                                viewModel.updatePersonalProfile(updated)
                            },
                            onAddFamilyMember = { name, relation, age, school ->
                                viewModel.addFamilyMember(
                                    name = name,
                                    relation = relation,
                                    age = age,
                                    occupationOrSchool = school
                                )
                            }
                        )
                        4 -> GenesisStageContextGraph(
                            graph = contextGraph
                        )
                        5 -> GenesisStagePersonality(
                            currentStyle = personalityStyle,
                            onSelectStyle = { style ->
                                viewModel.setPersonalityStyle(style)
                            }
                        )
                        6 -> GenesisStageDigitalTwinSeal(
                            twin = digitalTwin,
                            metadata = universalMetadata,
                            profile = personalProfile
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Navigation Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < 6) {
                            currentStep++
                        } else {
                            viewModel.completeOnboarding()
                            onGenesisComplete()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("genesis_continue_button")
                ) {
                    Text(
                        text = if (currentStep < 6) "Continue" else "Seal Genesis & Activate VYB",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (currentStep < 6) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/* =====================================================================
   STAGE 1: WELCOME TO VYB (First-Run Experience)
   ===================================================================== */
@Composable
private fun GenesisStageWelcome(
    onStartScan: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vyb_genesis_hero),
                    contentDescription = "VYB Genesis Hero Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Slate950.copy(alpha = 0.85f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "WELCOME TO VYB",
                        color = CyanNeon,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Your Universal Personal Intelligence Layer",
                        color = Slate200,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            VybGlowCard(
                borderColor = CyanNeon.copy(alpha = 0.7f),
                glowColor = CyanGlow
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "\"VYB is not another app.\nVYB becomes your personal intelligence layer.\"",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate100,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp
                        )
                    }

                    HorizontalDivider(color = Slate800, thickness = 1.dp)

                    Text(
                        text = "VYB can understand your digital life only through information and permissions that YOU choose to provide.\n\nWe never imply that VYB automatically has access to everything on your device. Every signal is explicitly authorized, inspectable, and auditable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate300,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        item {
            VybGlowCard(
                borderColor = Emerald500.copy(alpha = 0.5f),
                glowColor = Emerald500.copy(alpha = 0.15f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Emerald500, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ABSOLUTE OPERATING RULE: TRUTH > APPEARANCE",
                            color = Emerald500,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "• NEVER create the illusion of access that does not exist.\n• NEVER claim to scan the phone without authorization.\n• NEVER fabricate memories or simulate omniscient data.\n• All personal data stays encrypted on your local Android device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate300,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Button(
                onClick = onStartScan,
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("begin_genesis_scan_button")
            ) {
                Icon(Icons.Default.Biotech, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Begin Personal Genesis Life Scan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/* =====================================================================
   STAGE 2: PERSONAL LIFE SCAN ("BUILD MY VYB")
   ===================================================================== */
@Composable
private fun GenesisStageLifeScan(
    sourceSpecs: List<GenesisSourceSpec>,
    onToggleSource: (GenesisDataSource, Boolean) -> Unit,
    onRequestSystemPermission: (String) -> Unit
) {
    var expandedSourceType by remember { mutableStateOf<GenesisDataSource?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            VybGlowCard(
                borderColor = Slate700,
                glowColor = Color.Transparent
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "BUILD MY VYB: GUIDED LIFE SCAN",
                        color = CyanNeon,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Inspect and authorize only the data sources you choose. Tap any source to review Why VYB Needs It, What Will Be Read, Where Stored, and How to Disable.",
                        color = Slate300,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        items(sourceSpecs) { spec ->
            val isExpanded = expandedSourceType == spec.type
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (spec.isAuthorized) CyanNeon.copy(alpha = 0.7f) else CardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = spec.title,
                                    color = Slate100,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Status Pill
                            val statusColor = when (spec.status) {
                                DataSourceStatus.DEVICE_DATA_AVAILABLE -> Emerald500
                                DataSourceStatus.DEVICE_DATA_NOT_AVAILABLE -> Slate400
                                DataSourceStatus.PERMISSION_REQUIRED -> Amber500
                                DataSourceStatus.SERVICE_NOT_CONNECTED -> Rose500
                            }
                            Text(
                                text = spec.status.label,
                                color = statusColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = spec.isAuthorized,
                                onCheckedChange = { granted ->
                                    if (granted && spec.requiresAndroidPermission != null &&
                                        spec.status == DataSourceStatus.PERMISSION_REQUIRED
                                    ) {
                                        onRequestSystemPermission(spec.requiresAndroidPermission)
                                    }
                                    onToggleSource(spec.type, granted)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Slate950,
                                    checkedTrackColor = CyanNeon,
                                    uncheckedThumbColor = Slate400,
                                    uncheckedTrackColor = Slate800
                                ),
                                modifier = Modifier.testTag("toggle_${spec.type.name}")
                            )

                            IconButton(
                                onClick = {
                                    expandedSourceType = if (isExpanded) null else spec.type
                                }
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand details",
                                    tint = Slate400
                                )
                            }
                        }
                    }

                    // Deep Transparency Section (Mandatory 6 attributes)
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .background(Slate950, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TransparencyItem(label = "WHY VYB NEEDS IT", value = spec.whyVybNeedsIt, color = CyanNeon)
                            TransparencyItem(label = "WHAT WILL BE READ", value = spec.whatWillBeRead, color = Slate200)
                            TransparencyItem(label = "WHAT WILL BE STORED", value = spec.whatWillBeStored, color = Slate200)
                            TransparencyItem(label = "WHERE IT WILL BE STORED", value = spec.whereItWillBeStored, color = Slate300)
                            TransparencyItem(label = "WHETHER IT WILL LEAVE THE DEVICE", value = spec.whetherItWillLeaveTheDevice, color = Emerald500)
                            TransparencyItem(label = "HOW TO DISABLE IT", value = spec.howToDisableIt, color = Amber500)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransparencyItem(label: String, value: String, color: Color) {
    Column {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 0.8.sp
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Slate300,
            lineHeight = 16.sp
        )
    }
}

/* =====================================================================
   STAGE 3: PERSONAL PROFILE ENGINE (16 Core Dimensions)
   ===================================================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenesisStageProfileEngine(
    profile: VybPersonalProfile,
    onUpdateProfile: (VybPersonalProfile) -> Unit,
    onAddFamilyMember: (String, String, Int, String) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var title by remember { mutableStateOf(profile.callingTitle) }
    var creed by remember { mutableStateOf(profile.personalCreed) }
    var company by remember { mutableStateOf(profile.companyName) }
    var jobTitle by remember { mutableStateOf(profile.jobTitle) }
    var industry by remember { mutableStateOf(profile.industry) }
    var homeAddress by remember { mutableStateOf(profile.homeAddress) }
    var officeAddress by remember { mutableStateOf(profile.officeAddress) }
    var income by remember { mutableStateOf(if (profile.monthlyIncome > 0) profile.monthlyIncome.toInt().toString() else "") }
    var commitments by remember { mutableStateOf(if (profile.fixedCommitments > 0) profile.fixedCommitments.toInt().toString() else "") }
    var savings by remember { mutableStateOf(if (profile.savingsTarget > 0) profile.savingsTarget.toInt().toString() else "") }
    var currency by remember { mutableStateOf(profile.currencySymbol) }
    var diet by remember { mutableStateOf(profile.dietaryPreference) }
    var commute by remember { mutableStateOf(profile.commutePreference) }

    // Family Member Quick Add
    var famName by remember { mutableStateOf("") }
    var famRelation by remember { mutableStateOf("") }
    var famAge by remember { mutableStateOf("") }
    var famSchool by remember { mutableStateOf("") }

    // Save changes back to profile
    fun emitChanges() {
        val updated = profile.copy(
            name = name,
            callingTitle = title,
            personalCreed = creed,
            companyName = company,
            jobTitle = jobTitle,
            industry = industry,
            homeAddress = homeAddress,
            officeAddress = officeAddress,
            monthlyIncome = income.toDoubleOrNull() ?: 0.0,
            fixedCommitments = commitments.toDoubleOrNull() ?: 0.0,
            savingsTarget = savings.toDoubleOrNull() ?: 0.0,
            currencySymbol = currency,
            dietaryPreference = diet,
            commutePreference = commute
        )
        onUpdateProfile(updated)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "VYB PERSONAL PROFILE ENGINE (16 SECTIONS)",
                color = CyanNeon,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "These structured dimensions form your permanent digital context model. You can inspect and modify them anytime in Owner Control.",
                color = Slate300,
                fontSize = 13.sp
            )
        }

        // Section 1: Identity & Creed
        item {
            ProfileSectionCard(title = "1. Identity & Calling Title") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; emitChanges() },
                    label = { Text("Your Full Name") },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    colors = outlinedFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; emitChanges() },
                        label = { Text("Calling Title (e.g. Sir / Lead)") },
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it; emitChanges() },
                        label = { Text("Currency (₹, $, €)") },
                        modifier = Modifier.weight(0.6f),
                        colors = outlinedFieldColors()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = creed,
                    onValueChange = { creed = it; emitChanges() },
                    label = { Text("Personal Creed / Operating Mission") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )
            }
        }

        // Section 2: Work & Enterprise Context
        item {
            ProfileSectionCard(title = "2. Work & Enterprise Context") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it; emitChanges() },
                        label = { Text("Company / Enterprise") },
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = jobTitle,
                        onValueChange = { jobTitle = it; emitChanges() },
                        label = { Text("Job Title / Role") },
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = officeAddress,
                    onValueChange = { officeAddress = it; emitChanges() },
                    label = { Text("Office / Workspace Node") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )
            }
        }

        // Section 3: Family Context & Dependents
        item {
            ProfileSectionCard(title = "3. Family Context & Dependents") {
                Text(
                    text = "Add family members (child, spouse, parents) to connect school & home routines.",
                    color = Slate400,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = famName,
                        onValueChange = { famName = it },
                        label = { Text("Name") },
                        modifier = Modifier.weight(1.2f),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = famRelation,
                        onValueChange = { famRelation = it },
                        label = { Text("Relation") },
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = famAge,
                        onValueChange = { famAge = it },
                        label = { Text("Age") },
                        modifier = Modifier.weight(0.7f),
                        colors = outlinedFieldColors()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = famSchool,
                        onValueChange = { famSchool = it },
                        label = { Text("School / Occupation") },
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                    Button(
                        onClick = {
                            if (famName.isNotBlank()) {
                                onAddFamilyMember(famName, famRelation, famAge.toIntOrNull() ?: 0, famSchool)
                                famName = ""
                                famRelation = ""
                                famAge = ""
                                famSchool = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanNeon),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        // Section 4: Financial Planning Context
        item {
            ProfileSectionCard(title = "4. Financial Planning Context") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = income,
                        onValueChange = { income = it; emitChanges() },
                        label = { Text("Monthly Inflow ($currency)") },
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = commitments,
                        onValueChange = { commitments = it; emitChanges() },
                        label = { Text("Fixed Commitments") },
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = savings,
                    onValueChange = { savings = it; emitChanges() },
                    label = { Text("Target Discretionary Savings ($currency)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )
            }
        }

        // Section 5: Preferences & Places
        item {
            ProfileSectionCard(title = "5. Preferences & Core Locations") {
                OutlinedTextField(
                    value = homeAddress,
                    onValueChange = { homeAddress = it; emitChanges() },
                    label = { Text("Home Locality / Centroid") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = diet,
                        onValueChange = { diet = it; emitChanges() },
                        label = { Text("Dietary Focus") },
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                    OutlinedTextField(
                        value = commute,
                        onValueChange = { commute = it; emitChanges() },
                        label = { Text("Commute Preference") },
                        modifier = Modifier.weight(1f),
                        colors = outlinedFieldColors()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = Slate100,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanNeon,
    unfocusedBorderColor = Slate700,
    focusedLabelColor = CyanNeon,
    unfocusedLabelColor = Slate400,
    focusedTextColor = Slate100,
    unfocusedTextColor = Slate200,
    cursorColor = CyanNeon
)

/* =====================================================================
   STAGE 4: DIGITAL LIFE DISCOVERY & PERSONAL CONTEXT GRAPH
   ===================================================================== */
@Composable
private fun GenesisStageContextGraph(
    graph: PersonalContextGraph
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredNodes = remember(graph, selectedFilter) {
        when (selectedFilter) {
            "WORK" -> graph.nodes.filter { it.type in listOf(GraphNodeType.WORK, GraphNodeType.COMPANY, GraphNodeType.PROJECT, GraphNodeType.DOCUMENT, GraphNodeType.DEADLINE) }
            "FAMILY" -> graph.nodes.filter { it.type in listOf(GraphNodeType.FAMILY, GraphNodeType.CHILD, GraphNodeType.SCHOOL, GraphNodeType.ROUTINE) }
            "FINANCE" -> graph.nodes.filter { it.type in listOf(GraphNodeType.FINANCE, GraphNodeType.INCOME, GraphNodeType.EXPENSES, GraphNodeType.GOALS) }
            else -> graph.nodes
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            VybGlowCard(
                borderColor = CyanNeon.copy(alpha = 0.6f),
                glowColor = CyanGlow
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PERSONAL CONTEXT GRAPH",
                        color = CyanNeon,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "VYB reasons through relational nodes rather than static keywords. Information is classified with verified certainty.",
                        color = Slate300,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClassificationLegend(label = "CONFIRMED", color = CyanNeon)
                        ClassificationLegend(label = "INFERRED", color = PurpleAccent)
                        ClassificationLegend(label = "TEMPORARY", color = Amber500)
                        ClassificationLegend(label = "UNKNOWN", color = Slate500)
                    }
                }
            }
        }

        item {
            // Domain Filter Tabs
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ALL", "WORK", "FAMILY", "FINANCE")) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) CyanNeon else Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanNeon else Slate700),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Slate950 else Slate300,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Relational Reasoning Chains (${filteredNodes.size} Active Nodes, ${graph.edges.size} Edges)",
                color = Slate400,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(filteredNodes) { node ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = node.label,
                                color = Slate100,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val badgeColor = when (node.classification) {
                                InformationClassification.CONFIRMED -> CyanNeon
                                InformationClassification.INFERRED -> PurpleAccent
                                InformationClassification.TEMPORARY -> Amber500
                                InformationClassification.UNKNOWN -> Slate500
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeColor)
                            ) {
                                Text(
                                    text = node.classification.name,
                                    color = badgeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (node.details.isNotBlank()) {
                            Text(
                                text = node.details,
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = node.type.name,
                        color = Slate500,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassificationLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/* =====================================================================
   STAGE 5: PERSONALITY ADAPTATION & INTELLIGENCE CALIBRATION
   ===================================================================== */
@Composable
private fun GenesisStagePersonality(
    currentStyle: PersonalityStyle,
    onSelectStyle: (PersonalityStyle) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            VybGlowCard(
                borderColor = PurpleAccent.copy(alpha = 0.6f),
                glowColor = PurpleAccent.copy(alpha = 0.15f)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "VYB PERSONALITY ADAPTATION",
                        color = PurpleAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "VYB adapts its interaction style around you—whether you want concise executive bullet points, in-depth analytical reasoning, or voice-first conversations.",
                        color = Slate300,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Boundaries Guarantee: Personality adaptation never changes safety, truthfulness, or permission boundaries.",
                        color = Emerald500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        items(PersonalityStyle.values()) { style ->
            val isSelected = currentStyle == style
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) Slate850 else Slate900,
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) CyanNeon else CardBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectStyle(style) }
                    .testTag("personality_${style.name}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = style.title,
                            color = if (isSelected) CyanNeon else Slate100,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = style.description,
                            color = Slate400,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = CyanNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        item {
            VybGlowCard(
                borderColor = Slate700,
                glowColor = Color.Transparent
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "CONTINUOUS PERSONALIZATION LOOP",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "OBSERVE → UNDERSTAND → ASK → CONFIRM → REMEMBER → APPLY",
                        color = Slate300,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Observations are never silently converted into permanent memory without user confirmation.",
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

/* =====================================================================
   STAGE 6: PERSONAL DIGITAL TWIN ACTIVATION
   ===================================================================== */
@Composable
private fun GenesisStageDigitalTwinSeal(
    twin: PersonalDigitalTwin?,
    metadata: UniversalInstanceMetadata,
    profile: VybPersonalProfile
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            VybGlowCard(
                borderColor = CyanNeon,
                glowColor = CyanGlow
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "UNIVERSAL PERSONAL INSTANCE ACTIVATION",
                            color = CyanNeon,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.2.sp
                        )
                    }
                    Text(
                        text = "Instance ID: ${metadata.instanceId}",
                        color = Slate200,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Fingerprint: ${metadata.ownerFingerprint}",
                        color = Slate400,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Encryption: ${metadata.encryptionAlgorithm} • Isolated Sandbox",
                        color = Emerald500,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        item {
            Text(
                text = "PERSONAL DIGITAL TWIN MODEL",
                color = Slate300,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (twin != null) {
            item { TwinFacetCard(title = "WHO I AM", content = twin.whoIAm) }
            item { TwinFacetCard(title = "WHAT I DO", content = twin.whatIDo) }
            item { TwinFacetCard(title = "WHAT I CARE ABOUT", items = twin.whatICareAbout) }
            item { TwinFacetCard(title = "WHO MATTERS TO ME", items = twin.whoMattersToMe) }
            item { TwinFacetCard(title = "WHAT I AM WORKING ON", items = twin.whatIAmWorkingOn) }
            item { TwinFacetCard(title = "WHAT I AM TRYING TO ACHIEVE", items = twin.whatIAmTryingToAchieve) }
            item { TwinFacetCard(title = "WHAT I PREFER", items = twin.whatIPrefer) }
            item { TwinFacetCard(title = "WHAT I USUALLY DO", items = twin.whatIUsuallyDo) }
            item { TwinFacetCard(title = "WHAT I HAVE AUTHORIZED VYB TO DO", items = twin.whatIHaveAuthorizedVybToDo) }
        }

        item {
            VybGlowCard(
                borderColor = Emerald500.copy(alpha = 0.6f),
                glowColor = Emerald500.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Genesis Ready. Tap below to seal your personal intelligence layer and enter the VYB Operating System.",
                    color = Slate200,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TwinFacetCard(title: String, content: String? = null, items: List<String>? = null) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (content != null) {
                Text(text = content, color = Slate200, fontSize = 13.sp)
            }
            items?.forEach { itm ->
                Text(text = "• $itm", color = Slate300, fontSize = 12.sp)
            }
        }
    }
}
