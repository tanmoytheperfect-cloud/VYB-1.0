package com.example.ui.screens

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

@Composable
fun LifeScreen(
    viewModel: VybViewModel,
    onOpenPromptInHome: (String) -> Unit = {}
) {
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val familyMembers by viewModel.allFamilyMembers.collectAsStateWithLifecycle()
    val routines by viewModel.allRoutines.collectAsStateWithLifecycle()
    val goals by viewModel.allGoals.collectAsStateWithLifecycle()
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpenseAmount.collectAsStateWithLifecycle()
    val monthlyIncome by viewModel.monthlyIncome.collectAsStateWithLifecycle()
    val savingsTarget by viewModel.monthlySavingsTarget.collectAsStateWithLifecycle()

    var selectedSection by remember { mutableStateOf("ALL") }

    val discretionaryRemaining = (monthlyIncome - savingsTarget - totalExpense).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            VybTopBar(
                title = "PERSONAL CONTEXT",
                subtitle = if (ownerName.isNotBlank()) "Unified Life Model // $ownerName" else "Personal Context // Unconfigured"
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))

                // Unified Context Overview Banner
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
                            Text(
                                text = "AUTHENTIC PERSONAL CONTEXT",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanNeon,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (ownerName.isNotBlank() || familyMembers.isNotEmpty()) EmeraldNeon.copy(alpha = 0.15f) else Slate800,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (ownerName.isNotBlank() || familyMembers.isNotEmpty()) EmeraldNeon.copy(alpha = 0.4f) else Slate700)
                            ) {
                                Text(
                                    text = if (ownerName.isNotBlank()) "CONFIGURED" else "ZERO FABRICATION",
                                    color = if (ownerName.isNotBlank()) EmeraldNeon else Slate400,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = if (ownerName.isNotBlank()) {
                                "VYB only reasons on user-provided profile facts and verified on-device observations."
                            } else {
                                "No personal context configured. VYB will only learn from verified Android device observations or user-provided configuration."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate300,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Quick Category Filter Pills
            item {
                val filters = listOf("ALL", "FAMILY", "FINANCES", "PROJECTS", "ROUTINES")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filters) { filter ->
                        val isSelected = selectedSection == filter
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyanNeon else Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanNeon else CardBorder),
                            modifier = Modifier
                                .clickable { selectedSection = filter }
                                .testTag("life_filter_$filter")
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) Slate950 else Slate300,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 1. Family Setup
            if (selectedSection == "ALL" || selectedSection == "FAMILY") {
                item {
                    VybGlowCard(
                        borderColor = AmberNeon.copy(alpha = 0.5f),
                        glowColor = AmberGlow
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FamilyRestroom, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Family & Household", style = MaterialTheme.typography.titleMedium, color = Slate100, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (familyMembers.isEmpty()) {
                                Text("No family members configured yet.", color = Slate400, fontSize = 12.sp)
                            } else {
                                familyMembers.forEach { member ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Slate900,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("${member.name} (${member.relation})", color = Slate100, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            if (member.occupationOrSchool.isNotBlank()) {
                                                Text(member.occupationOrSchool, color = Slate300, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Personal Finances
            if (selectedSection == "ALL" || selectedSection == "FINANCES") {
                item {
                    VybGlowCard(
                        borderColor = EmeraldNeon.copy(alpha = 0.5f),
                        glowColor = EmeraldGlow
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Financial Baseline", style = MaterialTheme.typography.titleMedium, color = Slate100, fontWeight = FontWeight.Bold)
                                }
                                Text(if (monthlyIncome > 0) "₹${monthlyIncome.toInt()}/mo" else "Not set", color = EmeraldNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            if (monthlyIncome <= 0 && savingsTarget <= 0) {
                                Text("No financial parameters configured.", color = Slate400, fontSize = 12.sp)
                            } else {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Slate900,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Discretionary", color = Slate400, fontSize = 10.sp)
                                            Text("₹${discretionaryRemaining.toInt()}", color = EmeraldNeon, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Slate900,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text("Savings Target", color = Slate400, fontSize = 10.sp)
                                            Text("₹${savingsTarget.toInt()}", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Routines
            if (selectedSection == "ALL" || selectedSection == "ROUTINES") {
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Configured Routines", style = MaterialTheme.typography.titleMedium, color = Slate100, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (routines.isEmpty()) {
                                Text("No user routines configured. Genuine device routines will appear under Timeline learning.", color = Slate400, fontSize = 12.sp)
                            } else {
                                routines.forEach { r ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Slate900,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(r.title, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("${r.timeOfDay} • ${r.daysOfWeek}", color = VioletNeon, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Long-Term Projects
            if (selectedSection == "ALL" || selectedSection == "PROJECTS") {
                item {
                    VybGlowCard(
                        borderColor = BlueNeon.copy(alpha = 0.5f),
                        glowColor = BlueGlow
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Terminal, contentDescription = null, tint = BlueNeon, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("User Projects", style = MaterialTheme.typography.titleMedium, color = Slate100, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (projects.isEmpty()) {
                                Text("No active user projects recorded.", color = Slate400, fontSize = 12.sp)
                            } else {
                                projects.forEach { p ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Slate900,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(p.title, color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(p.description, color = Slate300, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
