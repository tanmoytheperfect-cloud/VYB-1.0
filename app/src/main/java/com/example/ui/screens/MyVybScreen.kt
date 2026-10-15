package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.genesis.*
import com.example.ui.components.VybGlowCard
import com.example.ui.components.VybTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

enum class MyVybTab(val title: String) {
    PROFILE("Profile"),
    GRAPH("Context Graph"),
    MEMORY("Memory"),
    ROUTINES("Routines & Patterns"),
    PEOPLE("People & Places"),
    GOALS("Goals & Projects"),
    PERMISSIONS("Permissions & Sources"),
    DATA("Data & Privacy"),
    MIGRATION("Migration & Sync")
}

@Composable
fun MyVybScreen(
    viewModel: VybViewModel,
    onNavigateBack: () -> Unit = {},
    onTriggerGenesisScan: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableStateOf(MyVybTab.PROFILE) }

    val profile by viewModel.personalProfile.collectAsStateWithLifecycle()
    val contextGraph by viewModel.contextGraph.collectAsStateWithLifecycle()
    val memories by viewModel.allMemories.collectAsStateWithLifecycle()
    val patterns by viewModel.discoveredPatterns.collectAsStateWithLifecycle()
    val routines by viewModel.allRoutines.collectAsStateWithLifecycle()
    val familyList by viewModel.allFamilyMembers.collectAsStateWithLifecycle()
    val goalList by viewModel.allGoals.collectAsStateWithLifecycle()
    val projectList by viewModel.allProjects.collectAsStateWithLifecycle()
    val sourceSpecs by viewModel.genesisSourceSpecs.collectAsStateWithLifecycle()
    val digitalTwin by viewModel.personalDigitalTwin.collectAsStateWithLifecycle()
    val metadata by viewModel.universalInstanceMetadata.collectAsStateWithLifecycle()
    val continuousStage by viewModel.continuousPersonalizationStage.collectAsStateWithLifecycle()

    var showExportDialog by remember { mutableStateOf(false) }
    var exportArchiveText by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }
    var showWipeConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VybTopBar(
                title = "MY VYB",
                subtitle = "Owner Control • Intelligence Layer & Digital Twin",
                showBackButton = true,
                onBackClick = onNavigateBack
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Horizontal Tab Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MyVybTab.values()) { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) CyanNeon else Slate900,
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) CyanNeon else Slate700
                        ),
                        modifier = Modifier
                            .clickable { selectedTab = tab }
                            .testTag("my_vyb_tab_${tab.name}")
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) Slate950 else Slate300,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTab) {
                    MyVybTab.PROFILE -> MyVybProfilePane(
                        profile = profile,
                        onUpdate = { viewModel.updatePersonalProfile(it) },
                        onRerunGenesis = onTriggerGenesisScan
                    )
                    MyVybTab.GRAPH -> MyVybGraphPane(graph = contextGraph)
                    MyVybTab.MEMORY -> MyVybMemoryPane(
                        memories = memories,
                        onDeleteMemory = { viewModel.deleteMemory(it) },
                        onAddMemory = { cat, key, value ->
                            viewModel.addMemory(cat, key, value, "User Stated")
                        }
                    )
                    MyVybTab.ROUTINES -> MyVybRoutinesPatternsPane(
                        patterns = patterns,
                        routines = routines,
                        onConfirmPattern = { id, saveAsRoutine ->
                            viewModel.confirmPattern(id, saveAsRoutine)
                            Toast.makeText(context, if (saveAsRoutine) "Routine saved to schedule" else "Preference saved", Toast.LENGTH_SHORT).show()
                        },
                        onDismissPattern = { id -> viewModel.dismissPattern(id) },
                        onDeleteRoutine = { viewModel.deleteRoutine(it) }
                    )
                    MyVybTab.PEOPLE -> MyVybPeoplePane(
                        familyList = familyList,
                        profile = profile,
                        onAddFamily = { name, rel, age, school ->
                            viewModel.addFamilyMember(name, rel, age, school)
                        },
                        onDeleteFamily = { viewModel.deleteFamilyMember(it) }
                    )
                    MyVybTab.GOALS -> MyVybGoalsPane(
                        goals = goalList,
                        projects = projectList,
                        onAddGoal = { title, cat, target, deadline ->
                            viewModel.addGoal(title, cat, target, "", deadline)
                        },
                        onAddProject = { title, desc, cat, deadline ->
                            viewModel.addProject(title, desc, cat, deadline)
                        }
                    )
                    MyVybTab.PERMISSIONS -> MyVybPermissionsPane(
                        sourceSpecs = sourceSpecs,
                        onToggleSource = { type, granted ->
                            viewModel.toggleGenesisSource(type, granted)
                        }
                    )
                    MyVybTab.DATA -> MyVybDataPrivacyPane(
                        metadata = metadata,
                        onWipeClick = { showWipeConfirmDialog = true },
                        onRerunGenesis = onTriggerGenesisScan
                    )
                    MyVybTab.MIGRATION -> MyVybMigrationPane(
                        metadata = metadata,
                        onExportClick = {
                            exportArchiveText = viewModel.exportPersonalIntelligenceArchive()
                            showExportDialog = true
                        },
                        onImportClick = {
                            showImportDialog = true
                        }
                    )
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = Slate900,
            title = {
                Text("Export Personal Intelligence", color = CyanNeon, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Your complete encrypted digital twin and memories packaged for transfer:",
                        color = Slate300,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = exportArchiveText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate700
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportArchiveText))
                        Toast.makeText(context, "Copied archive to clipboard", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
                ) {
                    Text("Copy Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = Slate400)
                }
            }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = Slate900,
            title = {
                Text("Import Personal Intelligence", color = CyanNeon, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste an exported VYB migration JSON package:", color = Slate300, fontSize = 12.sp)
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        placeholder = { Text("{\"version\": 1, ...}", color = Slate500) },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate200,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate700
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.importPersonalIntelligenceArchive(importInputText)
                        if (success) {
                            Toast.makeText(context, "Successfully imported VYB context!", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                            importInputText = ""
                        } else {
                            Toast.makeText(context, "Invalid archive format", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
                ) {
                    Text("Import Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }

    // Wipe Confirmation Dialog
    if (showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmDialog = false },
            containerColor = Slate900,
            title = {
                Text("Erase All Personal Intelligence?", color = Rose500, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This immediately deletes all local memories, routines, personal profile data, and life vault facts. You will re-enter the Personal Genesis flow.",
                    color = Slate300,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.wipeAllPersonalIntelligence()
                        showWipeConfirmDialog = false
                        onTriggerGenesisScan()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Slate100)
                ) {
                    Text("Factory Erase")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

/* =====================================================================
   TAB 1: PROFILE PANE (16 Sections)
   ===================================================================== */
@Composable
private fun MyVybProfilePane(
    profile: VybPersonalProfile,
    onUpdate: (VybPersonalProfile) -> Unit,
    onRerunGenesis: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            VybGlowCard(borderColor = CyanNeon, glowColor = CyanGlow) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = profile.name.ifBlank { "Unconfigured Owner" },
                                color = Slate100,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${profile.callingTitle} • ${profile.jobTitle.ifBlank { "Executive" }} @ ${profile.companyName.ifBlank { "Enterprise" }}",
                                color = CyanNeon,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = onRerunGenesis,
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Re-run Genesis", fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = "\"${profile.personalCreed}\"",
                        color = Slate400,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        item {
            ProfileSummaryItem("Languages", "${profile.primaryLanguage} (Primary), ${profile.secondaryLanguages.joinToString(", ")}")
            ProfileSummaryItem("Communication Style", profile.communicationStyle.title)
            ProfileSummaryItem("Preferences", "Diet: ${profile.dietaryPreference} • Commute: ${profile.commutePreference}")
            ProfileSummaryItem("Workplace Node", profile.officeAddress.ifBlank { "Not configured" })
            ProfileSummaryItem("Home Node", profile.homeAddress.ifBlank { "Not configured" })
            ProfileSummaryItem("Monthly Inflow", "${profile.currencySymbol}${profile.monthlyIncome.toInt()}")
            ProfileSummaryItem("Fixed Commitments", "${profile.currencySymbol}${profile.fixedCommitments.toInt()}")
            ProfileSummaryItem("Savings Target", "${profile.currencySymbol}${profile.savingsTarget.toInt()}")
            ProfileSummaryItem("Active Projects", profile.activeProjects.joinToString(" • "))
            ProfileSummaryItem("Top Priorities", profile.personalPriorities.joinToString("\n• ", prefix = "• "))
        }
    }
}

@Composable
private fun ProfileSummaryItem(title: String, value: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, color = Slate200, fontSize = 13.sp)
        }
    }
}

/* =====================================================================
   TAB 2: CONTEXT GRAPH PANE
   ===================================================================== */
@Composable
private fun MyVybGraphPane(graph: PersonalContextGraph) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "PERSONAL CONTEXT GRAPH: RELATIONAL REASONING",
                color = CyanNeon,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "${graph.nodes.size} Nodes • ${graph.edges.size} Directed Relational Edges",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        items(graph.nodes) { node ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = node.label, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (node.details.isNotBlank()) {
                            Text(text = node.details, color = Slate400, fontSize = 11.sp)
                        }
                    }
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
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/* =====================================================================
   TAB 3: MEMORY PANE (Rich AI Memory with Source, Confidence, Privacy)
   ===================================================================== */
@Composable
private fun MyVybMemoryPane(
    memories: List<com.example.data.local.entity.MemoryItemEntity>,
    onDeleteMemory: (Long) -> Unit,
    onAddMemory: (String, String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var cat by remember { mutableStateOf("PREFERENCE") }
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "PERSONAL AI MEMORY", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(text = "${memories.size} Verified Memory Items", color = Slate400, fontSize = 12.sp)
                }
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Memory", fontSize = 12.sp)
                }
            }
        }

        if (memories.isEmpty()) {
            item {
                Text("No verified memories stored. Talk with VYB to store memories.", color = Slate500, fontSize = 13.sp)
            }
        }

        items(memories) { mem ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = mem.key, color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Slate800
                            ) {
                                Text(
                                    text = mem.category,
                                    color = Slate300,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(text = mem.value, color = Slate100, fontSize = 13.sp)
                        Text(
                            text = "Source: ${mem.source} • Confidence: ${(mem.confidence * 100).toInt()}% • On-Device Encrypted",
                            color = Slate500,
                            fontSize = 10.sp
                        )
                    }
                    IconButton(onClick = { onDeleteMemory(mem.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose500, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Slate900,
            title = { Text("Add Memory Item", color = CyanNeon, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Key (e.g. coffee)") })
                    OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Value (e.g. Black with no sugar)") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (key.isNotBlank()) {
                            onAddMemory(cat, key, value)
                            key = ""
                            value = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Slate400) }
            }
        )
    }
}

/* =====================================================================
   TAB 4: ROUTINES & PATTERN DISCOVERY PANE
   ===================================================================== */
@Composable
private fun MyVybRoutinesPatternsPane(
    patterns: List<PatternDiscoveryItem>,
    routines: List<com.example.data.local.entity.RoutineEntity>,
    onConfirmPattern: (String, Boolean) -> Unit,
    onDismissPattern: (String) -> Unit,
    onDeleteRoutine: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "PATTERN DISCOVERY (\"I NOTICED A PATTERN...\")",
                color = CyanNeon,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
            Text(
                text = "Observations are not facts until confirmed by the owner.",
                color = Slate400,
                fontSize = 12.sp
            )
        }

        items(patterns.filter { !it.isDismissed }) { pattern ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (pattern.isConfirmed) Slate900 else Slate850,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (pattern.isConfirmed) Emerald500.copy(alpha = 0.5f) else Amber500.copy(alpha = 0.7f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pattern.category,
                            color = if (pattern.isConfirmed) Emerald500 else Amber500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Confidence: ${(pattern.confidence * 100).toInt()}% (${pattern.evidenceDaysCount} events)",
                            color = Slate500,
                            fontSize = 10.sp
                        )
                    }

                    Text(text = pattern.observation, color = Slate100, fontSize = 13.sp)

                    if (!pattern.isConfirmed) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onDismissPattern(pattern.id) }) {
                                Text("Dismiss", color = Slate400, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { onConfirmPattern(pattern.id, pattern.suggestedType == "SAVE_AS_ROUTINE") },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (pattern.suggestedType == "SAVE_AS_ROUTINE") "Save as Routine" else "Save as Preference",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        Text(text = "✓ Confirmed by owner", color = Emerald500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ACTIVE CONFIRMED ROUTINES (${routines.size})",
                color = CyanNeon,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }

        items(routines) { r ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = r.title, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "${r.timeOfDay} • ${r.daysOfWeek}", color = CyanNeon, fontSize = 11.sp)
                        Text(text = r.description, color = Slate400, fontSize = 12.sp)
                    }
                    IconButton(onClick = { onDeleteRoutine(r.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose500, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/* =====================================================================
   TAB 5: PEOPLE & PLACES PANE
   ===================================================================== */
@Composable
private fun MyVybPeoplePane(
    familyList: List<com.example.data.local.entity.FamilyMemberEntity>,
    profile: VybPersonalProfile,
    onAddFamily: (String, String, Int, String) -> Unit,
    onDeleteFamily: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var school by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "IMPORTANT PEOPLE & FAMILY", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 12.sp)
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Person", fontSize = 12.sp)
                }
            }
        }

        items(familyList) { f ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = f.name, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "${f.relation} • Age ${f.age}", color = CyanNeon, fontSize = 11.sp)
                        if (f.occupationOrSchool.isNotBlank()) {
                            Text(text = "School / Work: ${f.occupationOrSchool}", color = Slate300, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = { onDeleteFamily(f.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose500, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "IMPORTANT PLACES", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 12.sp)
            ProfileSummaryItem("Home Node", profile.homeAddress.ifBlank { "Not set" })
            ProfileSummaryItem("Office Node", profile.officeAddress.ifBlank { "Not set" })
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Slate900,
            title = { Text("Add Important Person", color = CyanNeon, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relation (Spouse, Child, Colleague)") })
                    OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") })
                    OutlinedTextField(value = school, onValueChange = { school = it }, label = { Text("School / Company") })
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddFamily(name, relation, age.toIntOrNull() ?: 0, school)
                            name = ""
                            relation = ""
                            age = ""
                            school = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = Slate400) }
            }
        )
    }
}

/* =====================================================================
   TAB 6: GOALS & PROJECTS PANE
   ===================================================================== */
@Composable
private fun MyVybGoalsPane(
    goals: List<com.example.data.local.entity.GoalEntity>,
    projects: List<com.example.data.local.entity.ProjectEntity>,
    onAddGoal: (String, String, String, String) -> Unit,
    onAddProject: (String, String, String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(text = "ACTIVE GOALS (${goals.size})", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        items(goals) { g ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = g.title, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Target: ${g.targetDeadline} • Progress: ${g.progressPercent}%", color = CyanNeon, fontSize = 11.sp)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "ACTIVE PROJECTS (${projects.size})", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
        items(projects) { p ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = p.title, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Milestone: ${p.deadline} • ${p.category}", color = Slate400, fontSize = 11.sp)
                }
            }
        }
    }
}

/* =====================================================================
   TAB 7: PERMISSIONS & SOURCES PANE
   ===================================================================== */
@Composable
private fun MyVybPermissionsPane(
    sourceSpecs: List<GenesisSourceSpec>,
    onToggleSource: (GenesisDataSource, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(text = "AUTHORIZATION MATRIX (14 DATA SOURCES)", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(text = "Toggle permissions on or off at any moment. Zero silent data collection.", color = Slate400, fontSize = 12.sp)
        }

        items(sourceSpecs) { spec ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = spec.title, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = spec.status.label, color = if (spec.isAuthorized) Emerald500 else Amber500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = spec.isAuthorized,
                        onCheckedChange = { onToggleSource(spec.type, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate950,
                            checkedTrackColor = CyanNeon
                        )
                    )
                }
            }
        }
    }
}

/* =====================================================================
   TAB 8: DATA & PRIVACY PANE
   ===================================================================== */
@Composable
private fun MyVybDataPrivacyPane(
    metadata: UniversalInstanceMetadata,
    onWipeClick: () -> Unit,
    onRerunGenesis: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            VybGlowCard(borderColor = Emerald500.copy(alpha = 0.6f), glowColor = Emerald500.copy(alpha = 0.15f)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "DATA MINIMIZATION ARCHITECTURE", color = Emerald500, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(
                        text = "• All personal facts are stored exclusively in the local Android SQLite/Room database.\n• Data is encrypted with ${metadata.encryptionAlgorithm}.\n• Never sold, never shared with third parties, never used for advertising.\n• Full auditability: You can view, export, or permanently wipe every single byte.",
                        color = Slate300,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, Rose500.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "DANGER ZONE: FACTORY WIPE", color = Rose500, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Erase all personal intelligence, context graph relations, and learned routines.", color = Slate400, fontSize = 12.sp)
                    Button(
                        onClick = onWipeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Slate100),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wipe All Personal Intelligence")
                    }
                }
            }
        }
    }
}

/* =====================================================================
   TAB 9: MIGRATION & MULTI-DEVICE SYNC PANE
   ===================================================================== */
@Composable
private fun MyVybMigrationPane(
    metadata: UniversalInstanceMetadata,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            VybGlowCard(borderColor = CyanNeon, glowColor = CyanGlow) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "PERSONAL AI MIGRATION ARCHITECTURE", color = CyanNeon, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(
                        text = "Phone → Tablet → Laptop → Car → Wearable → Future Hardware\n\nYour device is replaceable. Your VYB identity and personal intelligence layer are persistent.",
                        color = Slate200,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Universal Isolated Context", color = Slate100, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Instance ID: ${metadata.instanceId}", color = Slate400, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    Text(text = "Fingerprint: ${metadata.ownerFingerprint}", color = Slate400, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onExportClick,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Context")
                        }
                        Button(
                            onClick = onImportClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Context")
                        }
                    }
                }
            }
        }
    }
}
