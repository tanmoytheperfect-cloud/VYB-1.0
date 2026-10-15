package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.LearnedRoutineEntity
import com.example.data.local.entity.LifeVaultItemEntity
import com.example.data.local.entity.TimelineEventEntity
import com.example.domain.vault.VaultCategory
import com.example.domain.vault.VaultSource
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    viewModel: VybViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val vaultItems by viewModel.allVaultItems.collectAsStateWithLifecycle()
    val pendingConflicts by viewModel.pendingVaultConflicts.collectAsStateWithLifecycle()
    val learnedRoutines by viewModel.allLearnedRoutines.collectAsStateWithLifecycle()
    val candidatePatterns by viewModel.allCandidatePatterns.collectAsStateWithLifecycle()
    val timelineEvents by viewModel.recentTimelineEvents.collectAsStateWithLifecycle()
    val dailyReflections by viewModel.allDailyReflections.collectAsStateWithLifecycle()
    val weeklyPatterns by viewModel.allWeeklyPatterns.collectAsStateWithLifecycle()
    val currentCtx by viewModel.currentContext.collectAsStateWithLifecycle()
    val learningSettings by viewModel.learningSettings.collectAsStateWithLifecycle()
    val isLocalOnlyMode by viewModel.isLocalOnlyMode.collectAsStateWithLifecycle()
    val privacyAuditLog by viewModel.privacyAuditLog.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Personal Life Vault", "Learned Routines", "Daily Timeline", "Reflections & Shifts", "Privacy & Control")

    var selectedVaultCategory by remember { mutableStateOf("ALL") }
    val vaultCategories = listOf("ALL", VaultCategory.ME, VaultCategory.FAMILY, VaultCategory.PLACES, VaultCategory.WORK, VaultCategory.FINANCE, VaultCategory.HEALTH_FITNESS, VaultCategory.IMPORTANT_DATES, VaultCategory.MEMORIES)

    var selectedRoutineFilter by remember { mutableStateOf("ALL") }
    val routineFilters = listOf("ALL", "WEEKDAY", "WEEKEND", "FIRST_PHONE_USE", "APP_USAGE", "SLEEP_INACTIVITY", "CHARGING")

    var showAddVaultDialog by remember { mutableStateOf(false) }
    var editingVaultItem by remember { mutableStateOf<LifeVaultItemEntity?>(null) }
    var showForgetEverythingDialog by remember { mutableStateOf(false) }
    var showClearTimelineDialog by remember { mutableStateOf(false) }
    var showResetRoutinesDialog by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var diagnosticsData by remember { mutableStateOf<com.example.domain.persistence.PersistenceDiagnosticSnapshot?>(null) }

    // Dialog: Persistence Diagnostics
    if (showDiagnosticsDialog && diagnosticsData != null) {
        val diag = diagnosticsData!!
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = { Text("Owner Persistence Diagnostics", color = CyanNeon, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AUTHENTICATED OWNER: ${if (diag.isAuthenticated) "AUTHENTICATED" else "UNAUTHENTICATED / GUEST"}", color = if (diag.isAuthenticated) EmeraldNeon else CrimsonNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("STABLE OWNER ID: ${diag.ownerId}", color = Slate100, fontSize = 11.sp)
                    Text("LOCAL FACT COUNT: ${diag.localVaultCount} facts", color = Slate300, fontSize = 11.sp)
                    Text("DURABLE FACT COUNT: ${diag.durableFactCount} facts", color = Slate300, fontSize = 11.sp)
                    Text("LAST DURABLE SNAPSHOT: ${diag.lastSnapshotFormatted}", color = Slate300, fontSize = 11.sp)
                    Text("SCHEMA VERSION: v${diag.stateVersion}", color = Slate300, fontSize = 11.sp)
                    Text("INTEGRITY STATUS: ${diag.integrityStatus}", color = if (diag.integrityStatus.startsWith("VALID")) EmeraldNeon else AmberNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("SYNC / STORAGE STATUS: ${diag.syncStatus.name}", color = CyanNeon, fontSize = 11.sp)
                    Text("RESTORE STATUS: ${diag.restoreStatus}", color = Slate300, fontSize = 11.sp)
                    HorizontalDivider(color = Slate700)
                    Text("ARCHITECTURE LAYER TRUTH:\n${diag.storageArchitectureBreakdown}", color = Slate400, fontSize = 10.sp, lineHeight = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDiagnosticsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
                ) {
                    Text("Dismiss")
                }
            },
            containerColor = Slate900
        )
    }

    // Dialog: Factory Reset
    if (showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = { showFactoryResetDialog = false },
            title = { Text("Factory Reset & Purge All Data?", color = CrimsonNeon, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will completely wipe all timelines, learned routines, Personal Life Vault facts, weekly reflections, and SharedPreferences. VYB will be returned to a pure unconfigured zero state.",
                    color = Slate300
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetVybEntirely()
                        showFactoryResetDialog = false
                        Toast.makeText(context, "VYB completely reset to zero state", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonNeon, contentColor = Slate100),
                    modifier = Modifier.testTag("confirm_factory_reset_button")
                ) {
                    Text("Purge Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFactoryResetDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    // Dialog: Wipe Vault
    if (showForgetEverythingDialog) {
        AlertDialog(
            onDismissRequest = { showForgetEverythingDialog = false },
            title = { Text("Erase All Personal Life Vault Facts?", color = CrimsonNeon, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will completely erase all stored facts, family members, addresses, financial figures, and rules from VYB's Personal Life Vault. This action is irreversible.",
                    color = Slate300
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllVaultFacts()
                        showForgetEverythingDialog = false
                        Toast.makeText(context, "Personal Life Vault cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonNeon, contentColor = Slate100)
                ) {
                    Text("Erase Vault")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgetEverythingDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    if (showClearTimelineDialog) {
        AlertDialog(
            onDismissRequest = { showClearTimelineDialog = false },
            title = { Text("Clear Today's Timeline?", color = AmberNeon, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will remove today's recorded device events from the local database. Historical learned routines will be preserved.",
                    color = Slate300
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearTodayTimeline()
                        showClearTimelineDialog = false
                        Toast.makeText(context, "Today's timeline cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberNeon, contentColor = Slate950)
                ) {
                    Text("Clear Today")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTimelineDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    if (showResetRoutinesDialog) {
        AlertDialog(
            onDismissRequest = { showResetRoutinesDialog = false },
            title = { Text("Reset Learned Routines?", color = CrimsonNeon, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will erase all learned routine models and restart learning from freshly observed device evidence.",
                    color = Slate300
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllLearnedRoutines()
                        showResetRoutinesDialog = false
                        Toast.makeText(context, "Routines reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonNeon, contentColor = Slate100)
                ) {
                    Text("Reset Routines")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetRoutinesDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    // Dialog: Add Fact to Vault
    if (showAddVaultDialog) {
        var newCat by remember { mutableStateOf(if (selectedVaultCategory != "ALL") selectedVaultCategory else VaultCategory.ME) }
        var newKey by remember { mutableStateOf("") }
        var newValue by remember { mutableStateOf("") }
        var newSource by remember { mutableStateOf(VaultSource.USER_PROVIDED) }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddVaultDialog = false },
            title = { Text("Add Fact to Life Vault", color = CyanNeon, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Provide explicit, authentic information about your life:", color = Slate300, fontSize = 12.sp)

                    // Category Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate100),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Text("Category: $newCat")
                        }
                        DropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            VaultCategory.ALL_CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        newCat = cat
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it },
                        label = { Text("Key / Title") },
                        placeholder = { Text(when (newCat) {
                            VaultCategory.PLACES -> "e.g. Office Address / Son's School"
                            VaultCategory.FINANCE -> "e.g. Monthly Salary / Rent"
                            VaultCategory.FAMILY -> "e.g. Wife / Son"
                            VaultCategory.WORK -> "e.g. Company / Role"
                            VaultCategory.HEALTH_FITNESS -> "e.g. Diet Preference / Allergies"
                            else -> "e.g. Home Location / Nickname"
                        }) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("vault_key_input")
                    )

                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text("Value / Details") },
                        placeholder = { Text(when (newCat) {
                            VaultCategory.FINANCE -> "e.g. ₹1,50,000"
                            VaultCategory.PLACES -> "e.g. Salt Lake Sector V, Kolkata"
                            else -> "e.g. Specific authentic detail"
                        }) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("vault_value_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newKey.isNotBlank() && newValue.isNotBlank()) {
                            viewModel.addVaultFact(
                                category = newCat,
                                key = newKey.trim(),
                                value = newValue.trim(),
                                source = newSource,
                                sourceDetail = "Owner Manual UI Entry"
                            )
                            showAddVaultDialog = false
                            Toast.makeText(context, "Added to Personal Life Vault", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                    modifier = Modifier.testTag("save_vault_button")
                ) {
                    Text("Save Fact", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVaultDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    // Dialog: Edit Fact in Vault
    if (editingVaultItem != null) {
        val item = editingVaultItem!!
        var editKey by remember { mutableStateOf(item.key) }
        var editVal by remember { mutableStateOf(item.value) }

        AlertDialog(
            onDismissRequest = { editingVaultItem = null },
            title = { Text("Edit Life Vault Fact", color = CyanNeon, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Category: ${item.category} • Source: ${item.source}", color = Slate400, fontSize = 11.sp)

                    OutlinedTextField(
                        value = editKey,
                        onValueChange = { editKey = it },
                        label = { Text("Key / Title") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editVal,
                        onValueChange = { editVal = it },
                        label = { Text("Value / Details") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editKey.isNotBlank() && editVal.isNotBlank()) {
                            viewModel.updateVaultFact(
                                item.copy(
                                    key = editKey.trim(),
                                    value = editVal.trim(),
                                    source = VaultSource.USER_CONFIRMED,
                                    previousValue = item.value,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            editingVaultItem = null
                            Toast.makeText(context, "Fact updated", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
                ) {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingVaultItem = null }) {
                    Text("Cancel", color = Slate400)
                }
            },
            containerColor = Slate900
        )
    }

    Scaffold(
        topBar = {
            VybTopBar(
                title = "What VYB Knows",
                subtitle = "Personal Life Vault & Local Observed Patterns"
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            // Live Device State Header
            item {
                VybGlowCard(
                    borderColor = CyanNeon.copy(alpha = 0.5f),
                    glowColor = CyanGlow
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CyanNeon.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "AUTHENTIC KNOWLEDGE BOUNDARIES",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanNeon,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "Strict Separation: User Vault vs Observed Device Patterns",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Slate100,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        diagnosticsData = viewModel.getPersistenceDiagnostics()
                                        showDiagnosticsDialog = true
                                    }
                                },
                                modifier = Modifier.size(28.dp).testTag("persistence_diagnostics_button")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Persistence Diagnostics", tint = CyanNeon, modifier = Modifier.size(16.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Vault Facts", color = Slate400, fontSize = 10.sp)
                                Text("${vaultItems.size} Verified", color = Slate200, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text("Learned Routines", color = Slate400, fontSize = 10.sp)
                                Text("${learnedRoutines.size} Discovered", color = Slate200, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text("Timeline Signals", color = Slate400, fontSize = 10.sp)
                                Text("${timelineEvents.size} Events", color = Slate200, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column {
                                Text("Fabricated Data", color = Slate400, fontSize = 10.sp)
                                Text("0 (Strictly Blocked)", color = EmeraldNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Navigation Tabs
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Slate900,
                    contentColor = CyanNeon,
                    edgePadding = 0.dp,
                    divider = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == index) CyanNeon else Slate400
                                )
                            },
                            modifier = Modifier.testTag("tab_${title.lowercase().replace(" ", "_")}")
                        )
                    }
                }
            }

            // Tab Contents
            when (selectedTab) {
                0 -> {
                    // TAB 0: PERSONAL LIFE VAULT
                    // Pending Conflicts Banner
                    if (pendingConflicts.isNotEmpty()) {
                        item {
                            val conflict = pendingConflicts.first()
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AmberNeon.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "CONFLICT DETECTED: ${conflict.key.uppercase()}",
                                            color = AmberNeon,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "Current saved value: \"${conflict.value}\"\nProposed new value: \"${conflict.conflictingValue}\"",
                                        color = Slate200,
                                        style = MaterialTheme.typography.bodySmall,
                                        lineHeight = 18.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.resolveVaultConflict(conflict.id, acceptNew = true)
                                                Toast.makeText(context, "Replaced with new value", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberNeon, contentColor = Slate950),
                                            modifier = Modifier.weight(1f).testTag("resolve_conflict_replace")
                                        ) {
                                            Text("Replace with New", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.resolveVaultConflict(conflict.id, acceptNew = false)
                                                Toast.makeText(context, "Kept existing value", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                                            modifier = Modifier.weight(1f).testTag("resolve_conflict_keep")
                                        ) {
                                            Text("Keep Existing", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Vault Category Filter Chips
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(vaultCategories) { cat ->
                                val isSelected = selectedVaultCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedVaultCategory = cat },
                                    label = { Text(cat, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                                        selectedLabelColor = CyanNeon,
                                        containerColor = Slate900,
                                        labelColor = Slate400
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) CyanNeon else CardBorder
                                    ),
                                    modifier = Modifier.testTag("filter_chip_$cat")
                                )
                            }
                        }
                    }

                    // Header row with count and Add button
                    item {
                        val filteredCount = if (selectedVaultCategory == "ALL") vaultItems.size else vaultItems.count { it.category == selectedVaultCategory }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PERSONAL LIFE VAULT ($filteredCount FACTS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Button(
                                onClick = { showAddVaultDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("add_fact_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Fact", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    val displayItems = if (selectedVaultCategory == "ALL") vaultItems else vaultItems.filter { it.category == selectedVaultCategory }

                    if (displayItems.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate900,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (selectedVaultCategory == "ALL") "Personal Life Vault is Empty" else "No facts stored for $selectedVaultCategory",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Slate100,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "VYB does not fabricate personal information. Every fact in your Personal Life Vault must come from you directly, or from an authorized connection. When asked about unconfigured details, VYB will honestly reply: \"I don't know yet.\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        // Explicitly unconfigured categories overview
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate900.copy(alpha = 0.6f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "EXPLICITLY UNCONFIGURED ATTRIBUTES",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberNeon,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    val unknowns = listOf(
                                        "ME: Name, DOB, Contact details (UNCONFIGURED)",
                                        "FAMILY: Wife, Son, Parents, Relatives (UNKNOWN)",
                                        "PLACES: Home, Office, Son's School (UNKNOWN)",
                                        "WORK: Employer, Role, Hours (UNKNOWN)",
                                        "FINANCE: Income, Rent, Expenses, Savings Target (UNKNOWN / NULL)",
                                        "HEALTH/FITNESS: Weight, Goals, Nutrition Targets (UNCONFIGURED)",
                                        "IMPORTANT DATES: Birthdays, Anniversaries (NONE CONFIGURED)"
                                    )
                                    unknowns.forEach { itemText ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(AmberNeon)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(itemText, color = Slate400, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(displayItems, key = { it.id }) { item ->
                            LifeVaultCardItem(
                                item = item,
                                onEdit = { editingVaultItem = item },
                                onDelete = { viewModel.deleteVaultFact(item.id) }
                            )
                        }
                    }
                }

                1 -> {
                    // TAB 1: LEARNED ROUTINES
                    val displayRoutines = when (selectedRoutineFilter) {
                        "ALL" -> learnedRoutines
                        "WEEKDAY" -> learnedRoutines.filter { it.daysOfWeek == "WEEKDAY" }
                        "WEEKEND" -> learnedRoutines.filter { it.daysOfWeek == "WEEKEND" }
                        "FIRST_PHONE_USE" -> learnedRoutines.filter { it.patternCategory == "FIRST_PHONE_USE" || it.patternCategory == "SCREEN_ACTIVITY" }
                        "APP_USAGE" -> learnedRoutines.filter { it.patternCategory == "APP_USAGE" }
                        "SLEEP_INACTIVITY" -> learnedRoutines.filter { it.patternCategory == "SLEEP_INACTIVITY" }
                        "CHARGING" -> learnedRoutines.filter { it.patternCategory == "CHARGING" }
                        else -> learnedRoutines
                    }

                    // Routine Category Filter Chips
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(routineFilters) { filter ->
                                val isSelected = selectedRoutineFilter == filter
                                val filterLabel = filter.replace("_", " ")
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedRoutineFilter = filter },
                                    label = { Text(filterLabel, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                                        selectedLabelColor = CyanNeon,
                                        containerColor = Slate900,
                                        labelColor = Slate400
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) CyanNeon else CardBorder
                                    ),
                                    modifier = Modifier.testTag("routine_filter_${filter.lowercase()}")
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EVIDENCE-BASED ROUTINES (${displayRoutines.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Button(
                                onClick = {
                                    viewModel.runRoutineLearning()
                                    Toast.makeText(context, "Routine engine analyzed timeline", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Analyze", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (displayRoutines.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate900,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (selectedRoutineFilter == "ALL") "No Routines Learned Yet" else "No routines for $selectedRoutineFilter",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Slate100,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "VYB builds routines purely from real device evidence (unlocks, app usage, sleep periods). Use your phone normally over several days for genuine habits to emerge.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayRoutines, key = { it.routineId }) { routine ->
                            LearnedRoutineCardItem(
                                routine = routine,
                                onDelete = { viewModel.deleteLearnedRoutine(routine.routineId) }
                            )
                        }
                    }

                    if (candidatePatterns.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "COGNITIVE CANDIDATE PATTERNS (${candidatePatterns.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeon,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        items(candidatePatterns, key = { it.patternId }) { pattern ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate900,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("candidate_pattern_${pattern.patternId}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pattern.title,
                                                color = Slate100,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${pattern.subject} • ${pattern.domain}",
                                                color = Slate400,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            val statusColor = when (pattern.status) {
                                                "ESTABLISHED" -> EmeraldNeon
                                                "EMERGING" -> CyanNeon
                                                else -> VioletNeon
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = statusColor.copy(alpha = 0.15f),
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, statusColor)
                                            ) {
                                                Text(
                                                    text = pattern.status,
                                                    color = statusColor,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (pattern.isUserConfirmed) EmeraldNeon.copy(alpha = 0.15f) else Slate800,
                                                border = androidx.compose.foundation.BorderStroke(0.5.dp, if (pattern.isUserConfirmed) EmeraldNeon else Slate700)
                                            ) {
                                                Text(
                                                    text = if (pattern.isUserConfirmed) "CONFIRMED" else "LEARNED",
                                                    color = if (pattern.isUserConfirmed) EmeraldNeon else Slate300,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = pattern.description,
                                        color = Slate300,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )

                                    Text(
                                        text = pattern.explanation,
                                        color = Slate400,
                                        fontSize = 10.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Evidence: ${pattern.evidenceCount} observations (${(pattern.confidence * 100).toInt()}% conf)",
                                            color = Slate400,
                                            fontSize = 10.sp
                                        )

                                        IconButton(
                                            onClick = {
                                                viewModel.deleteCandidatePattern(pattern.patternId)
                                                Toast.makeText(context, "Pattern deleted", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp).testTag("delete_pattern_${pattern.patternId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Pattern",
                                                tint = CrimsonNeon,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: DAILY TIMELINE
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECENT TIMELINE EVENTS (${timelineEvents.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            IconButton(onClick = { viewModel.refreshCurrentContext() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CyanNeon)
                            }
                        }
                    }

                    if (timelineEvents.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate900,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Timeline Empty",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Slate100,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Device events will be logged here in real-time as you unlock your device, switch apps, or interact with VYB.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate400
                                    )
                                }
                            }
                        }
                    } else {
                        items(timelineEvents, key = { it.id }) { event ->
                            TimelineEventRowItem(event = event)
                        }
                    }
                }

                3 -> {
                    // TAB 3: REFLECTIONS & SHIFTS
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAILY REFLECTIONS & SHIFT INSIGHTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Button(
                                onClick = {
                                    viewModel.triggerDailyReflection()
                                    Toast.makeText(context, "Generated daily reflection", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reflect Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (weeklyPatterns.isNotEmpty()) {
                        item {
                            val wp = weeklyPatterns.first()
                            VybGlowCard(
                                borderColor = VioletNeon.copy(alpha = 0.5f),
                                glowColor = VioletGlow
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "7-DAY ROUTINE SHIFT ANALYSIS (${wp.weekIdentifier})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VioletNeon,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "${wp.startDateString} to ${wp.endDateString}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Text(
                                        text = wp.weeklyInsightText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate100,
                                        lineHeight = 18.sp
                                    )
                                    Divider(color = Slate800, thickness = 1.dp)
                                    Text(text = "• ${wp.weekdayPatternSummary}", style = MaterialTheme.typography.bodySmall, color = Slate300, fontSize = 11.sp)
                                    Text(text = "• ${wp.saturdayPatternSummary}", style = MaterialTheme.typography.bodySmall, color = Slate300, fontSize = 11.sp)
                                    Text(text = "• ${wp.sundayPatternSummary}", style = MaterialTheme.typography.bodySmall, color = Slate300, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    items(dailyReflections, key = { it.dateString }) { reflection ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${reflection.dayOfWeek} • ${reflection.dateString}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Slate100,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = CyanNeon.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = reflection.dayType,
                                            color = CyanNeon,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = reflection.reflectionSummaryText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate300,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                4 -> {
                    // TAB 4: PRIVACY & CONTROL

                    // 1. FLUID OS LOCAL-ONLY MODE CARD
                    item {
                        VybGlowCard(
                            borderColor = if (isLocalOnlyMode) AmberNeon else CyanNeon.copy(alpha = 0.5f),
                            glowColor = if (isLocalOnlyMode) AmberGlow else CyanGlow
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            imageVector = if (isLocalOnlyMode) Icons.Default.Shield else Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = if (isLocalOnlyMode) AmberNeon else CyanNeon,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "LOCAL-ONLY MODE (ZERO CLOUD EGRESS)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isLocalOnlyMode) AmberNeon else CyanNeon,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Switch(
                                        checked = isLocalOnlyMode,
                                        onCheckedChange = { enabled ->
                                            viewModel.setLocalOnlyMode(enabled)
                                        },
                                        modifier = Modifier.testTag("local_only_mode_switch"),
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AmberNeon,
                                            checkedTrackColor = AmberNeon.copy(alpha = 0.3f),
                                            uncheckedThumbColor = Slate400,
                                            uncheckedTrackColor = Slate800
                                        )
                                    )
                                }

                                Text(
                                    text = if (isLocalOnlyMode) {
                                        "ACTIVE: Outbound network transfers are strictly blocked by Fluid Privacy Firewall. Gemini cloud reasoning and external API calls are disabled. Fluid OS operates 100% locally on-device. Note: Cellular and Wi-Fi remain available for your other installed Android apps."
                                    } else {
                                        "INACTIVE (HYBRID MODE): Fluid OS requires fresh, explicit human approval before any outbound transfer of personal context. When a cloud reasoning request is proposed, you can inspect, redact, or reject the transfer."
                                    },
                                    color = Slate300,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isLocalOnlyMode) AmberNeon.copy(alpha = 0.1f) else Slate950,
                                    border = BorderStroke(1.dp, if (isLocalOnlyMode) AmberNeon.copy(alpha = 0.4f) else Slate800),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isLocalOnlyMode) AmberNeon else EmeraldNeon)
                                        )
                                        Text(
                                            text = if (isLocalOnlyMode) "Policy: STRICT ON-DEVICE ONLY" else "Policy: FIREWALL GATED TRANSFERS",
                                            color = if (isLocalOnlyMode) AmberNeon else EmeraldNeon,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. FLUID PRIVACY FIREWALL OUTBOUND AUDIT LEDGER
                    item {
                        VybGlowCard(
                            borderColor = VioletNeon.copy(alpha = 0.5f),
                            glowColor = VioletGlow
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.HistoryEdu, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(18.dp))
                                        Text(
                                            text = "OUTBOUND AUDIT LEDGER (${privacyAuditLog.size})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VioletNeon,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                    Text(
                                        text = "SHA-256 HASH VERIFIED",
                                        color = Slate400,
                                        fontSize = 9.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = "Every transfer attempt is logged below with its cryptographic SHA-256 hash. Raw sensitive payloads are never retained in the ledger.",
                                    color = Slate400,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                if (privacyAuditLog.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Slate950,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "No outbound transfers recorded yet. The firewall has prevented unapproved network egress.",
                                            color = Slate400,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        privacyAuditLog.take(10).forEach { record ->
                                            val outcomeColor = when (record.outcome) {
                                                com.example.domain.privacy.AuditOutcome.AUTHORIZED_AND_CONSUMED -> EmeraldNeon
                                                com.example.domain.privacy.AuditOutcome.BLOCKED_LOCAL_ONLY_MODE -> AmberNeon
                                                com.example.domain.privacy.AuditOutcome.DENIED_BY_USER,
                                                com.example.domain.privacy.AuditOutcome.CANCELLED_BY_USER -> Slate400
                                                else -> CrimsonNeon
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Slate950,
                                                border = BorderStroke(0.5.dp, CardBorder),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = record.outcome.name,
                                                            color = outcomeColor,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = record.destinationName,
                                                            color = Slate200,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                    Text(
                                                        text = "Purpose: ${record.purpose}",
                                                        color = Slate300,
                                                        fontSize = 11.sp
                                                    )
                                                    Text(
                                                        text = "SHA: ${record.payloadSha256.take(16)}... • ${record.destinationEndpoint}",
                                                        color = Slate400,
                                                        fontSize = 9.sp,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                    )
                                                    if (record.note.isNotBlank()) {
                                                        Text(
                                                            text = "Note: ${record.note}",
                                                            color = Slate400,
                                                            fontSize = 10.sp
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

                    // 3. OBSERVATION & LEARNING CONTROLS
                    item {
                        VybGlowCard(
                            borderColor = EmeraldNeon.copy(alpha = 0.5f),
                            glowColor = EmeraldGlow
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "PRIVACY & OBSERVATION CONTROLS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldNeon,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Pause Routine Learning",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Slate100,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Temporarily stop observing device signals and creating new routines.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = learningSettings?.isLearningPaused == true,
                                        onCheckedChange = { isPaused ->
                                            viewModel.setLearningPaused(isPaused)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = AmberNeon,
                                            checkedTrackColor = AmberNeon.copy(alpha = 0.3f),
                                            uncheckedThumbColor = Slate400,
                                            uncheckedTrackColor = Slate800
                                        )
                                    )
                                }

                                Divider(color = Slate800)

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { showClearTimelineDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberNeon),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Clear Today's Timeline")
                                    }

                                    OutlinedButton(
                                        onClick = { showResetRoutinesDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonNeon),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reset All Learned Routines")
                                    }

                                    OutlinedButton(
                                        onClick = { showForgetEverythingDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonNeon),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Wipe Personal Life Vault Facts")
                                    }

                                    Button(
                                        onClick = { showFactoryResetDialog = true },
                                        modifier = Modifier.fillMaxWidth().testTag("factory_reset_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonNeon.copy(alpha = 0.2f), contentColor = CrimsonNeon),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Default.Dangerous, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Factory Reset & Purge All Data", fontWeight = FontWeight.Bold)
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

@Composable
private fun LifeVaultCardItem(
    item: LifeVaultItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val sourceColor = when (item.source) {
        VaultSource.USER_PROVIDED -> CyanNeon
        VaultSource.USER_CONFIRMED -> EmeraldNeon
        VaultSource.CONNECTED -> VioletNeon
        VaultSource.LEARNED -> AmberNeon
        else -> Slate400
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().testTag("vault_card_${item.key.lowercase().replace(" ", "_")}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Slate800
                    ) {
                        Text(
                            text = item.category,
                            color = CyanNeon,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.key,
                        style = MaterialTheme.typography.titleSmall,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = item.value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate200,
                    fontWeight = FontWeight.Medium
                )

                if (item.previousValue != null) {
                    Text(
                        text = "Previously: \"${item.previousValue}\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontSize = 10.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = sourceColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = item.source,
                            color = sourceColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    if (item.sourceDetail != null) {
                        Text(
                            text = "• ${item.sourceDetail}",
                            color = Slate500,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Fact",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Fact",
                        tint = Slate500,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LearnedRoutineCardItem(
    routine: LearnedRoutineEntity,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    val statusColor = when (routine.status) {
        "LEARNED_ROUTINE" -> EmeraldNeon
        "CANDIDATE_ROUTINE" -> CyanNeon
        "POSSIBLE_PATTERN" -> AmberNeon
        else -> Slate400
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = routine.status.replace("_", " "),
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = routine.expectedTimeWindow,
                        color = Slate300,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete routine",
                        tint = Slate500,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = routine.title,
                style = MaterialTheme.typography.titleSmall,
                color = Slate100,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = routine.description,
                style = MaterialTheme.typography.bodySmall,
                color = Slate300,
                lineHeight = 18.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Confidence: ${(routine.confidenceScore * 100).toInt()}% • Evidence: ${routine.evidenceCount} observation(s)",
                        color = Slate400,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Days: ${routine.daysOfWeek}",
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }
                LinearProgressIndicator(
                    progress = routine.confidenceScore.coerceIn(0.05f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = statusColor,
                    trackColor = Slate800
                )
            }

            // Evidence & Explainability Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CyanNeon,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "Hide Evidence Breakdown" else "Why does VYB consider this a pattern?",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isExpanded) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate950,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "OBSERVATION METRICS & PROVENANCE",
                            color = Slate400,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Distinct Observation Days:", color = Slate400, fontSize = 10.sp)
                            Text("${routine.distinctObservationDays} calendar day(s)", color = Slate200, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        if (routine.dayDistributionSummary.isNotBlank()) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Day Distribution:", color = Slate400, fontSize = 10.sp)
                                Text(routine.dayDistributionSummary, color = Slate200, fontSize = 10.sp)
                            }
                        }
                        if (routine.timeVarianceMinutes > 0f) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Time Variation (Std Dev):", color = Slate400, fontSize = 10.sp)
                                Text("±${routine.timeVarianceMinutes.toInt()} min", color = Slate200, fontSize = 10.sp)
                            }
                        }
                        if (routine.observedDatesSummary.isNotBlank()) {
                            Column {
                                Text("Observed Dates:", color = Slate400, fontSize = 10.sp)
                                Text(routine.observedDatesSummary, color = Slate300, fontSize = 9.sp)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Signal Source:", color = Slate400, fontSize = 10.sp)
                            Text(routine.source, color = EmeraldNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Discovered purely through authentic local Android signals. Never user-configured or fabricated.",
                            color = Slate500,
                            fontSize = 9.sp,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineEventRowItem(event: TimelineEventEntity) {
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(event.timestamp))

    val icon = when (event.eventType) {
        "DEVICE_UNLOCK" -> Icons.Default.LockOpen
        "APP_OPENED" -> Icons.Default.Apps
        "SCREEN_OFF" -> Icons.Default.Smartphone
        "SCREEN_ON" -> Icons.Default.StayCurrentPortrait
        "SLEEP_INACTIVITY_PERIOD" -> Icons.Default.Bedtime
        "VYB_INTERACTION" -> Icons.Default.ChatBubbleOutline
        "CALENDAR_EVENT" -> Icons.Default.Event
        "ROUTINE_EXECUTED" -> Icons.Default.CheckCircleOutline
        else -> Icons.Default.FiberManualRecord
    }

    val badgeColor = when (event.eventType) {
        "DEVICE_UNLOCK" -> CyanNeon
        "APP_OPENED" -> EmeraldNeon
        "SLEEP_INACTIVITY_PERIOD" -> VioletNeon
        "VYB_INTERACTION" -> AmberNeon
        "ROUTINE_EXECUTED" -> EmeraldNeon
        else -> Slate400
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.eventType.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = event.contextData,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate200,
                    fontSize = 12.sp
                )
            }
        }
    }
}
