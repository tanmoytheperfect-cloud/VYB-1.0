package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun AssistantScreen(
    viewModel: VybViewModel,
    onBackClick: () -> Unit
) {
    val messages by viewModel.currentMessages.collectAsStateWithLifecycle()
    val currentConv by viewModel.currentConversation.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    var showVoiceDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showVoiceDialog) {
        VoiceInputDialog(
            onDismiss = { showVoiceDialog = false },
            onResult = { voiceText ->
                inputText = voiceText
                viewModel.sendMessage(voiceText)
                inputText = ""
            }
        )
    }

    Scaffold(
        topBar = {
            VybTopBar(
                title = currentConv?.title ?: "VYB AI",
                subtitle = if (currentConv != null) IntentType.fromString(currentConv?.intentType).displayName else "AI Life Engine // Sir $ownerName",
                showBackButton = true,
                onBackClick = onBackClick,
                actions = {
                    if (currentConv != null) {
                        IconButton(
                            onClick = {
                                viewModel.deleteConversation(currentConv!!.id)
                                onBackClick()
                            },
                            modifier = Modifier.testTag("delete_conversation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Conversation",
                                tint = Slate400
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = Slate950,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
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
                        placeholder = "Ask VYB...",
                        minHeight = 56.dp
                    )
                }
            }
        },
        containerColor = Slate950
    ) { innerPadding ->
        if (messages.isEmpty() && !isLoading) {
            // Empty state helper
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Slate850, Slate800)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Start a Request",
                        style = MaterialTheme.typography.titleLarge,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Ask for a meal plan, grocery list, travel itinerary, budget breakdown, or task schedule.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubbleItem(
                        message = msg,
                        viewModel = viewModel,
                        onFollowUpClick = { followUpPrompt ->
                            viewModel.sendMessage(followUpPrompt)
                        }
                    )
                }

                if (isLoading) {
                    item {
                        AssistantTypingIndicator()
                    }
                }

                if (errorMessage != null) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate900,
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoseNeon.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = RoseNeon,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = errorMessage ?: "Request failed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate200
                                    )
                                }
                                TextButton(onClick = { viewModel.retryLastMessage() }) {
                                    Text("Retry", color = CyanNeon, fontWeight = FontWeight.Bold)
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
fun MessageBubbleItem(
    message: MessageEntity,
    viewModel: VybViewModel,
    onFollowUpClick: (String) -> Unit
) {
    val isUser = message.sender == "USER"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isUser) {
            Surface(
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp),
                color = Slate800,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate100,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    lineHeight = 22.sp
                )
            }
        } else {
            // Assistant Message
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header badge with Speak Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(CyanNeon, VioletNeon))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Slate950,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VYB AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.speakText(message.content) },
                        modifier = Modifier.size(28.dp).testTag("speak_message_btn_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Read Aloud",
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Conversational Text Bubble
                if (message.content.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                        color = Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate100,
                            modifier = Modifier.padding(14.dp),
                            lineHeight = 22.sp
                        )
                    }
                }

                // Interactive Intent Cards (if present)
                val payload = remember(message.intentJson) {
                    viewModel.parsePayload(message.intentJson)
                }

                if (payload != null) {
                    VybIntentCard(
                        payload = payload,
                        onFollowUpClick = onFollowUpClick
                    )
                }
            }
        }
    }
}

@Composable
fun AssistantTypingIndicator() {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(CyanNeon, VioletNeon))),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = Slate950
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "VYB is thinking and organizing...",
            style = MaterialTheme.typography.bodySmall,
            color = CyanNeon,
            fontWeight = FontWeight.Medium
        )
    }
}
