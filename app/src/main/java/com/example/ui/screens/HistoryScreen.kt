package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ConversationEntity
import com.example.data.model.IntentType
import com.example.ui.components.VybTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: VybViewModel,
    onOpenConversation: (Long) -> Unit
) {
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>("ALL") }

    val categories = listOf("ALL", "DIET", "SHOPPING", "TRAVEL", "TASK", "FINANCE", "COMMUNICATION")

    val filteredList = remember(conversations, searchQuery, selectedCategory) {
        conversations.filter { conv ->
            val matchesCategory = if (selectedCategory == "ALL" || selectedCategory == null) true
            else conv.intentType.equals(selectedCategory, ignoreCase = true)

            val matchesSearch = if (searchQuery.isBlank()) true
            else conv.title.contains(searchQuery, ignoreCase = true) || conv.preview.contains(searchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
    }

    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VybTopBar(
                title = "Request History",
                subtitle = "${conversations.size} logged requests",
                actions = {
                    if (conversations.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier.testTag("clear_all_history")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = Slate400
                            )
                        }
                    }
                }
            )
        },
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search past requests...", color = Slate500) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyanNeon)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null, tint = Slate400)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = CardBorder,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_search_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    val display = if (cat == "ALL") "All Intents" else IntentType.fromString(cat).displayName
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) CyanNeon else Slate900,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) CyanNeon else CardBorder),
                        modifier = Modifier
                            .clickable { selectedCategory = cat }
                            .testTag("filter_chip_$cat")
                    ) {
                        Text(
                            text = display,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Slate950 else Slate300,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Slate600,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No history found",
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate400
                        )
                        Text(
                            text = "Conversations and AI actions will appear here",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { conv ->
                        HistoryCardItem(
                            conversation = conv,
                            onClick = { onOpenConversation(conv.id) },
                            onDelete = { viewModel.deleteConversation(conv.id) }
                        )
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = Slate900,
            title = { Text("Clear All History", color = Slate100, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete all saved conversations and requests?", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllConversations()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseNeon, contentColor = Color.White)
                ) {
                    Text("Clear All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

@Composable
fun HistoryCardItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(conversation.updatedAt) {
        SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()).format(Date(conversation.updatedAt))
    }

    val intent = remember(conversation.intentType) {
        IntentType.fromString(conversation.intentType)
    }

    val accentColor = when (intent) {
        IntentType.DIET -> EmeraldNeon
        IntentType.SHOPPING -> CyanNeon
        IntentType.TRAVEL -> VioletNeon
        IntentType.TASK -> AmberNeon
        IntentType.FINANCE -> CyanNeon
        IntentType.COMMUNICATION -> BlueNeon
        else -> CyanNeon
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("history_item_${conversation.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, accentColor)
                ) {
                    Text(
                        text = intent.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Slate600,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = conversation.title,
                style = MaterialTheme.typography.titleMedium,
                color = Slate100,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = conversation.preview,
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                maxLines = 2,
                lineHeight = 18.sp
            )
        }
    }
}
