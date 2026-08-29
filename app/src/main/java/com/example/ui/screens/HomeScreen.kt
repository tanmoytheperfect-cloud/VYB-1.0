package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.example.data.local.entity.MessageEntity
import com.example.data.model.IntentType
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: VybViewModel,
    onNavigateToLife: () -> Unit = {},
    onNavigateToActions: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var showVoiceDialog by remember { mutableStateOf(false) }

    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val currentConv by viewModel.currentConversation.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val todayEvents by viewModel.todayTimelineEvents.collectAsStateWithLifecycle()
    val learnedRoutines by viewModel.allLearnedRoutines.collectAsStateWithLifecycle()
    val allMemories by viewModel.allMemories.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Real-Time Live Clock State (updates every 5 seconds)
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(5000)
        }
    }

    val liveCalendar = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }
    val currentHour = liveCalendar.get(Calendar.HOUR_OF_DAY)
    val liveTimeStr = remember(currentTimeMillis) {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(currentTimeMillis))
    }
    val liveDateStr = remember(currentTimeMillis) {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(currentTimeMillis))
    }

    val greeting = when (currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    val observationsTodayCount = todayEvents.size
    val patternsLearningCount = learnedRoutines.count { it.status == "OBSERVATION" || it.status == "POSSIBLE_PATTERN" || it.status == "CANDIDATE_ROUTINE" }
    val learnedRoutinesCount = learnedRoutines.count { it.status == "LEARNED_ROUTINE" }
    val hasPersonalConfig = ownerName.isNotBlank() || allMemories.isNotEmpty()

    // Ensure there is an active conversation session on launch
    LaunchedEffect(conversations) {
        if (currentConv == null && conversations.isNotEmpty()) {
            viewModel.selectConversation(conversations.first().id)
        }
    }

    if (showVoiceDialog) {
        VoiceInputDialog(
            onDismiss = { showVoiceDialog = false },
            onResult = { voiceText ->
                inputText = voiceText
                if (currentConv != null) {
                    viewModel.sendMessage(voiceText)
                } else {
                    viewModel.startNewConversation(voiceText) {}
                }
                inputText = ""
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = Slate950,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(androidx.compose.foundation.BorderStroke(1.dp, Brush.verticalGradient(listOf(CardBorder, Color.Transparent))))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Futuristic Breathing Hologram Core
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(CyanNeon, VioletNeon, Slate900)
                                    )
                                )
                                .border(1.5.dp, CyanNeon.copy(alpha = 0.8f), CircleShape)
                                .clickable {
                                    viewModel.speakJarvisGreeting()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.GraphicEq else Icons.Default.Psychology,
                                contentDescription = "VYB Core",
                                tint = Slate100,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "VYB",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Slate100,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isLoading) AmberNeon else EmeraldNeon)
                                )
                            }
                            Text(
                                text = if (isLoading) "REASONING & PLANNING..." else if (isSpeaking) "SPEAKING // JARVIS AUDIO" else "PERSONAL AI OPERATING SYSTEM",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLoading) AmberNeon else CyanNeon,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Top right live time HUD & Voice trigger
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = liveTimeStr,
                                    color = Slate100,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Voice trigger
                        IconButton(
                            onClick = {
                                if (isSpeaking) viewModel.stopSpeaking() else viewModel.speakJarvisGreeting()
                            },
                            modifier = Modifier.testTag("tts_voice_toggle")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Toggle Audio Voice",
                                tint = if (isSpeaking) AmberNeon else Slate400,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // New Dialogue Session
                        IconButton(
                            onClick = {
                                viewModel.startNewConversation("VYB, good morning.") {}
                            },
                            modifier = Modifier.testTag("new_dialogue_session")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddComment,
                                contentDescription = "New Session",
                                tint = CyanNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Slate950,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .border(androidx.compose.foundation.BorderStroke(1.dp, Brush.verticalGradient(listOf(CardBorder, Slate950))))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    VybAiInputBox(
                        value = inputText,
                        onValueChange = { inputText = it },
                        onSend = { prompt ->
                            if (currentConv != null) {
                                viewModel.sendMessage(prompt)
                            } else {
                                viewModel.startNewConversation(prompt) {}
                            }
                            inputText = ""
                        },
                        onVoiceClick = { showVoiceDialog = true },
                        isLoading = isLoading,
                        placeholder = "Talk to VYB... e.g. What routines have you observed?",
                        minHeight = 54.dp
                    )
                }
            }
        },
        containerColor = Slate950
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))

                // Honest Real-Time Learning State Header
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
                                        .background(CyanNeon.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (ownerName.isNotBlank()) "$greeting, $ownerName." else "$greeting.",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Slate100,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$liveTimeStr • $liveDateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (learnedRoutinesCount > 0) EmeraldNeon.copy(alpha = 0.15f) else AmberNeon.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (learnedRoutinesCount > 0) EmeraldNeon.copy(alpha = 0.4f) else AmberNeon.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (learnedRoutinesCount > 0) "$learnedRoutinesCount ROUTINES LEARNED" else "LEARNING IN PROGRESS",
                                    color = if (learnedRoutinesCount > 0) EmeraldNeon else AmberNeon,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = if (observationsTodayCount == 0 && learnedRoutinesCount == 0) {
                                "VYB is observing device activity to learn your recurring daily patterns. All observations stay strictly on your device."
                            } else {
                                "Recorded $observationsTodayCount observations today. $patternsLearningCount candidate patterns currently evaluating."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate200,
                            lineHeight = 20.sp
                        )

                        // Real metrics breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Slate900,
                                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("OBSERVATIONS", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("$observationsTodayCount", color = CyanNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Slate900,
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("PATTERNS", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("$patternsLearningCount", color = AmberNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = Slate900,
                                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("ROUTINES", color = Slate400, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    Text("$learnedRoutinesCount", color = EmeraldNeon, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Authentic Contextual Prompt Pills
            item {
                val contextualPills = listOf(
                    Triple("📊 What do you know about me?", "What information and habits have you observed about me so far?", CyanNeon),
                    Triple("⏱️ Today's Timeline", "Show me my recorded device activity and timeline for today.", AmberNeon),
                    Triple("🧠 Learned Routines", "What recurring daily routines have you learned from my device signals?", EmeraldNeon),
                    Triple("📋 Organize Daily Tasks", "Help me organize and review my tasks for today.", BlueNeon),
                    Triple("🔒 Privacy & Diagnostics", "Provide system diagnostics on learning engine, observed events, and privacy status.", VioletNeon)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(contextualPills) { (label, prompt, color) ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .clickable {
                                    if (currentConv != null) {
                                        viewModel.sendMessage(prompt)
                                    } else {
                                        viewModel.startNewConversation(prompt) {}
                                    }
                                }
                                .testTag("pill_${label.take(10)}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    color = Slate100,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Conversation Messages Stream
            items(messages, key = { it.id }) { message ->
                MessageBubbleItem(
                    message = message,
                    viewModel = viewModel,
                    onFollowUpClick = { followUpPrompt ->
                        viewModel.sendMessage(followUpPrompt)
                    }
                )
            }

            // Thinking & Reasoning Indicator
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CyanNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = CyanNeon,
                                strokeWidth = 2.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "VYB is evaluating personal context, memories & tools...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
