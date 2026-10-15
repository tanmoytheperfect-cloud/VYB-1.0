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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.intelligence.*
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
    val capabilities by viewModel.allCapabilities.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastIntelligenceResult.collectAsStateWithLifecycle()
    val activePlans by viewModel.activeActionPlans.collectAsStateWithLifecycle()
    val pendingPrompt by viewModel.pendingConfirmationPrompt.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) } // 0 = Capabilities (23), 1 = Agents & Plans, 2 = Approvals, 3 = Pipeline Trace & History
    var commandInput by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VybTopBar(
                title = "ACTION & INTELLIGENCE",
                subtitle = "Orchestration & Verification // Sir $ownerName"
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

                // Action Center Header & Interactive Command Input
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
                            Text(
                                text = "PERSONAL INTELLIGENCE ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = BlueNeon,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            if (pendingPrompt != null || pendingActions.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AmberNeon.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon)
                                ) {
                                    Text(
                                        text = "CONFIRMATION REQUIRED",
                                        color = AmberNeon,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Every user command executes strictly through: Input → Intent Detection → Context Resolution → Memory Retrieval → Permission Check → Capability Discovery → Plan → Execute → Verify → Respond → Learn.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate300,
                            lineHeight = 18.sp
                        )

                        // Command Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = commandInput,
                                onValueChange = { commandInput = it },
                                placeholder = { Text("e.g. Book my usual cab / Remind me tomorrow", color = Slate500, fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("action_command_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BlueNeon,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate200,
                                    focusedContainerColor = Slate900,
                                    unfocusedContainerColor = Slate900
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (commandInput.isNotBlank()) {
                                        val cmd = commandInput
                                        commandInput = ""
                                        isExecuting = true
                                        viewModel.processIntelligenceCommand(cmd) {
                                            isExecuting = false
                                        }
                                    }
                                },
                                enabled = commandInput.isNotBlank() && !isExecuting,
                                colors = ButtonDefaults.buttonColors(containerColor = BlueNeon, contentColor = Slate950),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.height(48.dp).testTag("action_execute_btn")
                            ) {
                                if (isExecuting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Slate950, strokeWidth = 2.dp)
                                } else {
                                    Text("Orchestrate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Execution Prompts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quicks = listOf(
                        "Book my usual cab",
                        "Remind me to call Rahul",
                        "What to buy for son's school"
                    )
                    quicks.forEach { q ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .clickable {
                                    commandInput = q
                                }
                        ) {
                            Text(
                                text = q,
                                color = CyanNeon,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Pipeline Inspector Card (if last result exists)
            lastResult?.let { res ->
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth().testTag("pipeline_inspector_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "11-STAGE PIPELINE TRACE",
                                    color = CyanNeon,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldNeon.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon)
                                ) {
                                    Text(
                                        text = res.universalIntent.status.name,
                                        color = EmeraldNeon,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Intent: ${res.universalIntent.intentType.displayName} | Risk: ${res.universalIntent.riskLevel.label} | Confidence: ${(res.resolvedContext.confidenceScore * 100).toInt()}%",
                                color = Slate300,
                                fontSize = 11.sp
                            )

                            // Verification details
                            res.verification?.let { v ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (v.isVerified) EmeraldNeon.copy(alpha = 0.1f) else RoseNeon.copy(alpha = 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (v.isVerified) EmeraldNeon.copy(alpha = 0.4f) else RoseNeon.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (v.isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (v.isVerified) EmeraldNeon else RoseNeon,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "[${v.method}] ${v.proofDescription}",
                                            color = if (v.isVerified) EmeraldNeon else RoseNeon,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }

                            // Flow Trace Summary
                            Text(
                                text = "Pipeline Stages:",
                                color = Slate400,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            res.pipelineTrace.forEachIndexed { idx, stage ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (stage.isSuccess) EmeraldNeon else RoseNeon)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${idx + 1}. ${stage.stageName}: ",
                                        color = Slate300,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = stage.description,
                                        color = Slate400,
                                        fontSize = 10.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        "Capabilities (${capabilities.size})",
                        "Agents & Plans (${activePlans.size})",
                        "Approvals (${if (pendingPrompt != null) pendingActions.size + 1 else pendingActions.size})",
                        "History"
                    )
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

            // TAB 0: 23 System Capabilities
            if (selectedTab == 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CAPABILITY REGISTRY (23 CAPABILITIES)",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Real Android sensors, platform intents & encrypted databases. Zero simulated telemetry.",
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }
                }

                items(capabilities) { cap ->
                    val statusColor = when (cap.availability) {
                        CapabilityAvailability.ACTIVE, CapabilityAvailability.OFFLINE_ONLY -> EmeraldNeon
                        CapabilityAvailability.PERMISSION_REQUIRED -> AmberNeon
                        CapabilityAvailability.SERVICE_NOT_CONNECTED, CapabilityAvailability.TOOL_UNAVAILABLE -> RoseNeon
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth().testTag("cap_card_${cap.capabilityId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cap.name,
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
                                            text = cap.availability.label,
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
                                Text(
                                    text = "Verification: ${cap.verificationMethod} | Risk: ${cap.riskLevel.label}",
                                    color = Slate500,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }
            }

            // TAB 1: 12 Specialized Agents & Multi-Step Action Plans
            if (selectedTab == 1) {
                item {
                    Text(
                        text = "ACTIVE ACTION PLANS (${activePlans.size})",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                if (activePlans.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No active multi-step plans running. Use the orchestrator bar above to plan a task.",
                                color = Slate400,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(activePlans) { plan ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (plan.status == PlanStatus.COMPLETED) EmeraldNeon.copy(alpha = 0.4f) else BlueNeon.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(plan.goal, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (plan.status == PlanStatus.COMPLETED) EmeraldNeon.copy(alpha = 0.2f) else BlueNeon.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = plan.status.name,
                                            color = if (plan.status == PlanStatus.COMPLETED) EmeraldNeon else BlueNeon,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text("Progress: Step ${plan.currentStepIndex}/${plan.steps.size}", color = Slate400, fontSize = 10.sp)

                                plan.steps.forEach { step ->
                                    val isCurrent = step.stepIndex == plan.currentStepIndex
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = when (step.status) {
                                                StepStatus.COMPLETED -> Icons.Default.CheckCircle
                                                StepStatus.RUNNING -> Icons.Default.Sync
                                                StepStatus.CONFIRMATION_REQUIRED -> Icons.Default.Lock
                                                StepStatus.FAILED -> Icons.Default.Cancel
                                                else -> Icons.Default.RadioButtonUnchecked
                                            },
                                            contentDescription = null,
                                            tint = when (step.status) {
                                                StepStatus.COMPLETED -> EmeraldNeon
                                                StepStatus.RUNNING -> CyanNeon
                                                StepStatus.CONFIRMATION_REQUIRED -> AmberNeon
                                                StepStatus.FAILED -> RoseNeon
                                                else -> Slate500
                                            },
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${step.stepIndex + 1}. ${step.title}",
                                            color = if (isCurrent) Slate100 else Slate400,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "12 SPECIALIZED AGENTS",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                val allAgents = viewModel.intelligenceOrchestrator.agentRegistry.getAllAgents()
                items(allAgents) { agent ->
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
                                Text(agent.name, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(agent.specialty, color = Slate400, fontSize = 10.sp)
                                Text("Capabilities: ${agent.requiredCapabilities.joinToString()}", color = Slate500, fontSize = 9.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BlueNeon.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "READY",
                                    color = BlueNeon,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // TAB 2: Pending Approvals & Risk Confirmation
            if (selectedTab == 2) {
                // High Risk Prompt from Intelligence Orchestrator
                pendingPrompt?.let { prompt ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon),
                            modifier = Modifier.fillMaxWidth().testTag("pending_intelligence_prompt_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(prompt.title, color = AmberNeon, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AmberNeon.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = prompt.riskLevel.label,
                                            color = AmberNeon,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text("• What will happen: ${prompt.whatWillHappen}", color = Slate200, fontSize = 11.sp)
                                Text("• Service / Target: ${prompt.recipientOrService}", color = Slate300, fontSize = 11.sp)
                                Text("• Data Shared: ${prompt.dataShared}", color = Slate300, fontSize = 11.sp)
                                Text("• Consequence: ${prompt.estimatedConsequence}", color = AmberNeon, fontSize = 10.sp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.cancelPendingIntelligenceAction(prompt) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonNeon),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp).testTag("cancel_prompt_btn")
                                    ) {
                                        Text("Reject", fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.confirmPendingIntelligenceAction(prompt, context) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Slate950),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp).testTag("confirm_prompt_btn")
                                    ) {
                                        Text("Confirm & Execute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                if (pendingPrompt == null && pendingActions.isEmpty()) {
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
                                Text("All tool executions and high-risk gates are clear.", color = Slate400, fontSize = 11.sp)
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

            // TAB 3: History & Trace
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
