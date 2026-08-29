package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import com.example.domain.capability.CapabilityEngine
import com.example.domain.capability.CapabilityState
import com.example.domain.capability.CapabilityStatus
import com.example.domain.tools.ToolRegistry
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

@Composable
fun ActionsScreen(
    viewModel: VybViewModel,
    onExecuteActionPrompt: (String) -> Unit = {}
) {
    val pendingActions by viewModel.pendingConfirmationActions.collectAsStateWithLifecycle()
    val allActions by viewModel.allActions.collectAsStateWithLifecycle()
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var capabilitiesList by remember { mutableStateOf(CapabilityEngine.getAllCapabilities(context)) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Capabilities, 1 = Action Tools, 2 = Pending Approvals, 3 = History

    Scaffold(
        topBar = {
            VybTopBar(
                title = "ACTION CENTER",
                subtitle = "Autonomous Tool Execution // Sir $ownerName"
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

                // Action Center Header
                VybGlowCard(
                    borderColor = BlueNeon.copy(alpha = 0.5f),
                    glowColor = BlueGlow
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "THINGS VYB CAN DO",
                                style = MaterialTheme.typography.labelSmall,
                                color = BlueNeon,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            if (pendingActions.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AmberNeon.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon)
                                ) {
                                    Text(
                                        text = "${pendingActions.size} PENDING APPROVAL",
                                        color = AmberNeon,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "VYB orchestrates intelligent tools across transit, finance, commerce, style, and professional communication with strict confirmation gates for financial or consequential actions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate300,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Tab Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf("Capabilities", "Tools", "Approvals (${pendingActions.size})", "History")
                    tabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedTab == index
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) BlueNeon else Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) BlueNeon else CardBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = index }
                                .testTag("actions_tab_$index")
                        ) {
                            Text(
                                text = tabTitle,
                                color = if (isSelected) Slate950 else Slate300,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // TAB 0: Capabilities
            if (selectedTab == 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "AUTHENTIC CAPABILITY REGISTRY",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "VYB only accesses verified APIs and system permissions. Zero simulated telemetry.",
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }
                }

                items(capabilitiesList) { cap ->
                    val statusColor = when (cap.status) {
                        CapabilityStatus.CONNECTED, CapabilityStatus.GRANTED, CapabilityStatus.AVAILABLE -> EmeraldNeon
                        CapabilityStatus.NOT_CONNECTED, CapabilityStatus.DENIED -> RoseNeon
                        CapabilityStatus.UNAVAILABLE -> Slate400
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth().testTag("cap_card_${cap.type.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cap.type.displayName,
                                        color = Slate100,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = statusColor.copy(alpha = 0.15f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = cap.status.label,
                                            color = statusColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = cap.description,
                                    color = Slate400,
                                    fontSize = 11.sp
                                )
                                if (cap.requiredActionExplanation.isNotBlank()) {
                                    Text(
                                        text = cap.requiredActionExplanation,
                                        color = Slate500,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (cap.status == CapabilityStatus.NOT_CONNECTED && cap.isExternalService) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        CapabilityEngine.setServiceConnected(cap.type, true)
                                        capabilitiesList = CapabilityEngine.getAllCapabilities(context)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Connect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (cap.status == CapabilityStatus.CONNECTED && cap.isExternalService) {
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        CapabilityEngine.setServiceConnected(cap.type, false)
                                        capabilitiesList = CapabilityEngine.getAllCapabilities(context)
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Disconnect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // TAB 1: Action Tools Quick Launchers
            if (selectedTab == 1) {
                val actionTools = listOf(
                    Triple("⏱️ Timeline Diagnostics", "Show today's device timeline observations", "Review real-time Android events logged on device"),
                    Triple("🧠 Pattern Learning Status", "What device patterns have you observed?", "Check current candidate routines and observation evidence"),
                    Triple("🔒 Memory Vault Audit", "Show what facts VYB knows and what is unknown", "Verify stored personal memories and unconfigured attributes"),
                    Triple("📝 Task Organizer", "Add a new task: Review project requirements tomorrow", "Create and schedule local actionable tasks"),
                    Triple("🛡️ System Permissions", "Check active Android permissions", "Inspect Usage Access, Calendar, and Notification permissions")
                )

                items(actionTools) { (title, prompt, description) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onExecuteActionPrompt(prompt) }
                            .testTag("action_tool_${title.take(8)}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = title, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = description, color = Slate400, fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onExecuteActionPrompt(prompt) },
                                colors = ButtonDefaults.buttonColors(containerColor = BlueNeon, contentColor = Slate950),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Launch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("REGISTERED SYSTEM TOOLS (${ToolRegistry.allTools.size})", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                items(ToolRegistry.allTools) { tool ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = tool.name, color = Slate200, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = tool.description, color = Slate400, fontSize = 10.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (tool.requiresConfirmation) AmberNeon.copy(alpha = 0.2f) else EmeraldNeon.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (tool.requiresConfirmation) "Confirmation Req" else "Autonomous",
                                    color = if (tool.requiresConfirmation) AmberNeon else EmeraldNeon,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // TAB 2: Pending Approvals
            if (selectedTab == 2) {
                if (pendingActions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(40.dp))
                                Text("No pending confirmation actions", color = Slate300, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("All tool executions are up to date.", color = Slate400, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    items(pendingActions) { action ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(action.title, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    if (action.costInr != null && action.costInr > 0) {
                                        Text("₹${action.costInr.toInt()}", color = AmberNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                Text(action.description, color = Slate300, fontSize = 11.sp)
                                Text("🔒 Level 2/3 Safety Protocol: Explicit user approval required.", color = AmberNeon, fontSize = 10.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.cancelAction(action.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonNeon),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Cancel", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.executeAndApproveAction(context, action) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Slate950),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Approve & Execute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 3: History
            if (selectedTab == 3) {
                items(allActions) { action ->
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
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(action.title, color = Slate100, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(action.description, color = Slate400, fontSize = 10.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (action.status == "COMPLETED") EmeraldNeon.copy(alpha = 0.2f) else if (action.status == "CANCELLED") CrimsonNeon.copy(alpha = 0.2f) else AmberNeon.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = action.status,
                                    color = if (action.status == "COMPLETED") EmeraldNeon else if (action.status == "CANCELLED") CrimsonNeon else AmberNeon,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
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
