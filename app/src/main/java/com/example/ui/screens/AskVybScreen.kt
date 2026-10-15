package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.BuildConfig
import com.example.data.local.entity.MessageEntity
import com.example.domain.state.AskVybState
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel
import android.util.Log
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskVybScreen(
    viewModel: VybViewModel,
    initialPrompt: String = "",
    onNavigateBack: () -> Unit = {}
) {
    BackHandler {
        onNavigateBack()
    }

    var inputText by remember { mutableStateOf(initialPrompt) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var selectedMediaUri by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmationPrompt.collectAsStateWithLifecycle()

    val latestResponse by viewModel.latestResponseText.collectAsStateWithLifecycle()
    val latestTraceStatus by viewModel.latestTraceStatus.collectAsStateWithLifecycle()
    val latestTraceId by viewModel.latestTraceId.collectAsStateWithLifecycle()
    val askVybState by viewModel.askVybState.collectAsStateWithLifecycle()
    val isLocalOnlyMode by viewModel.isLocalOnlyMode.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Log.d("VYB_TRACE", "ASK_VYB_SCREEN_COMPOSED vm=${viewModel.viewModelIdentity} creationTime=${viewModel.viewModelCreationTime}")
    }

    LaunchedEffect(latestResponse, latestTraceStatus) {
        if (latestResponse.isNotBlank()) {
            Log.d("VYB_TRACE", "[$latestTraceId] COMPOSE_STATE_OBSERVED: \"$latestResponse\"")
        }
    }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Photo picker (Android zero-permission picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri.toString()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showVoiceDialog) {
        VoiceInputDialog(
            onDismiss = { showVoiceDialog = false },
            onResult = { text ->
                showVoiceDialog = false
                if (text.isNotBlank()) {
                    viewModel.sendMessage(text)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VybIntelligenceCore(
                            size = 32.dp,
                            visualState = if (isLoading) VybVisualState.THINKING else VybVisualState.IDLE
                        )
                        Column {
                            Text(
                                text = "ASK VYB",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Slate100,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isLoading) {
                                    "Reasoning across Personal Life Model..."
                                } else if (isLocalOnlyMode) {
                                    "🛡️ Local-Only Mode (Zero Cloud Transfers)"
                                } else {
                                    "🔒 Fluid Privacy Firewall Gated"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLoading) CyanNeon else if (isLocalOnlyMode) AmberNeon else Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ask_vyb_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Home",
                            tint = Slate200
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startNewSession() },
                        modifier = Modifier.testTag("ask_vyb_new_session_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = "New Session",
                            tint = CyanNeon
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950
                )
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Confirmation Card if an action requires explicit human confirmation
            if (pendingConfirmation != null) {
                val prompt = pendingConfirmation!!
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1B4B).copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VioletNeon)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = prompt.title.uppercase(),
                                color = VioletNeon,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(prompt.whatWillHappen, color = Slate200, fontSize = 12.sp, lineHeight = 18.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.confirmPendingAction(true) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                                modifier = Modifier.weight(1f).testTag("action_confirm_button")
                            ) {
                                Text("Approve & Execute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.confirmPendingAction(false) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate300),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Slate600),
                                modifier = Modifier.weight(1f).testTag("action_reject_button")
                            ) {
                                Text("Decline", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // S23 RUNTIME TRACE & BUILD DIAGNOSTICS CARD (PART 1, PART 3, PART 13)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0B1120).copy(alpha = 0.95f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("s23_debug_diagnostics_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ S23 BUILD & RUNTIME TRACE",
                            color = CyanNeon,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "STATE: ${askVybState.name}",
                            color = if (askVybState == AskVybState.ERROR) CrimsonNeon else CyanNeon,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "App: ${BuildConfig.APPLICATION_ID} | v${BuildConfig.VERSION_NAME} (code ${BuildConfig.VERSION_CODE})\nBuilt: ${BuildConfig.BUILD_TIME_PRETTY}\nVM: ${viewModel.viewModelIdentity}",
                        color = Slate400,
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Slate800)
                    Text(
                        text = "TRACE: ${latestTraceStatus.ifBlank { "Ready" }}",
                        color = Slate300,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (latestResponse.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "DEBUG RESPONSE:\n$latestResponse",
                            color = Slate100,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Message History or Invitation State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    // Empty Session State with Quick Suggested Prompts
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        VybIntelligenceCore(
                            size = 110.dp,
                            visualState = VybVisualState.IDLE,
                            onClick = {}
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "What do you want VYB to do?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask deterministic questions, check weather/device time, or query your Personal Life Vault.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        // Suggested query pills (including deterministic tests from user specification)
                        val suggestedPrompts = listOf(
                            "What time is it?",
                            "What day is today?",
                            "What is the weather right now?",
                            "What do you remember about my son?",
                            "What matters today?"
                        )

                        suggestedPrompts.forEach { prompt ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        val traceId = viewModel.generateNextTraceId()
                                        Log.d("VYB_TRACE", "[$traceId] INPUT_RECEIVED: \"$prompt\"")
                                        Log.d("VYB_TRACE", "[$traceId] SEND_CLICKED")
                                        viewModel.processUserCommand(prompt, context, traceId)
                                    }
                                    .testTag("suggested_prompt_${prompt.take(15)}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = CyanNeon,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(prompt, color = Slate200, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            AskVybMessageBubble(
                                message = msg,
                                ownerName = ownerName,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            // Input Bar at the Bottom
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                color = Slate950,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (selectedMediaUri != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attachment attached", color = CyanNeon, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { selectedMediaUri = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Slate400, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Media attachment button
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Slate900)
                                .border(1.dp, CardBorder, CircleShape)
                                .testTag("attach_media_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Attach Media",
                                tint = Slate300,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Voice input button
                        IconButton(
                            onClick = { showVoiceDialog = true },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Slate900)
                                .border(1.dp, CardBorder, CircleShape)
                                .testTag("voice_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = Slate300,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Text Field
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text("What do you want VYB to do?", color = Slate500, fontSize = 13.sp)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.8f),
                                unfocusedContainerColor = Color(0xFF0F172A).copy(alpha = 0.8f),
                                focusedBorderColor = CyanNeon,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate200
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ask_vyb_input_field")
                        )

                        // Cloud AI Firewall Review Button (when not in local-only mode)
                        if (!isLocalOnlyMode && inputText.isNotBlank() && !isLoading) {
                            IconButton(
                                onClick = {
                                    val textToSend = inputText.trim()
                                    inputText = ""
                                    viewModel.requestCloudReasoningWithFirewall(textToSend, context)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AmberNeon.copy(alpha = 0.2f))
                                    .border(1.dp, AmberNeon, CircleShape)
                                    .testTag("cloud_reasoning_review_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Cloud AI Reasoning (Firewall Review)",
                                    tint = AmberNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Send Button (On-device reasoning default)
                        IconButton(
                            onClick = {
                                val currentText = inputText
                                Log.d("VYB_TRACE", "INPUT_TEXT = \"$currentText\"")
                                if (currentText.isNotBlank() && !isLoading) {
                                    val textToSend = currentText.trim()
                                    val traceId = viewModel.generateNextTraceId()
                                    Log.d("VYB_TRACE", "[$traceId] INPUT_RECEIVED: \"$textToSend\"")
                                    Log.d("VYB_TRACE", "[$traceId] SEND_CLICKED")
                                    inputText = ""
                                    selectedMediaUri = null
                                    viewModel.processUserCommand(textToSend, context, traceId)
                                } else {
                                    Log.w("VYB_TRACE", "SEND_CLICKED blocked: isBlank=${currentText.isBlank()} isLoading=$isLoading")
                                }
                            },
                            enabled = inputText.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputText.isNotBlank() && !isLoading) CyanNeon else Slate800
                                )
                                .testTag("ask_vyb_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank() && !isLoading) Slate950 else Slate500,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AskVybMessageBubble(
    message: MessageEntity,
    ownerName: String,
    viewModel: VybViewModel
) {
    val isUser = message.sender == "USER"
    val traceId by viewModel.latestTraceId.collectAsStateWithLifecycle()

    SideEffect {
        if (!isUser) {
            Log.d("VYB_TRACE", "[$traceId] RESPONSE_RENDERED: \"${message.content.take(60).replace("\n", " ")}\"")
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                text = if (isUser) ownerName.ifBlank { "You" } else "VYB",
                color = if (isUser) Slate400 else CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) {
                Color(0xFF1E293B)
            } else {
                Color(0xFF0F172A).copy(alpha = 0.9f)
            },
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUser) Color(0x3338BDF8) else Color(0x4406B6D4)
            ),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = message.content,
                    color = Slate100,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )

                // If intent JSON exists, render structured cards
                if (!message.intentJson.isNullOrBlank() && !isUser) {
                    val payload = remember(message.intentJson) {
                        viewModel.parsePayload(message.intentJson)
                    }
                    if (payload != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        VybIntentCard(payload = payload)
                    }
                }
            }
        }
    }
}
