package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Explicit Security & Owner Authorization Dialog.
 * Enforces the core personal assistant principle:
 * "The assistant asks its owner permission every time before executing tasks on the device."
 */
@Composable
fun OwnerAuthorizationDialog(
    actionTitle: String,
    targetService: String,
    actionDescription: String,
    recipientOrDestination: String? = null,
    estimatedCost: Double? = null,
    riskLevel: String = "LEVEL 3 • DIRECT DEVICE EXECUTION",
    consequenceSummary: String = "This action will launch the external mobile app on your phone with prepared parameters.",
    ownerName: String = "",
    onDismiss: () -> Unit,
    onAuthorizeAndExecute: () -> Unit
) {
    val currentTime = remember {
        val sdf = SimpleDateFormat("hh:mm a • d MMM yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    var isVerifying by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = {
            if (!isVerifying) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isVerifying,
            dismissOnClickOutside = !isVerifying,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Slate900,
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(AmberNeon, CyanNeon))),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(16.dp)
                .testTag("owner_authorization_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberNeon.copy(alpha = 0.2f))
                                .border(1.dp, AmberNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AmberNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "OWNER AUTHORIZATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = AmberNeon,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Personal Assistant Security Gate",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate800,
                        border = BorderStroke(1.dp, Slate700)
                    ) {
                        Text(
                            text = currentTime,
                            color = Slate300,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                HorizontalDivider(color = CardBorder, thickness = 1.dp)

                // Action Overview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate850,
                    border = BorderStroke(1.dp, CardBorder),
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
                            Text(
                                text = actionTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = Slate100,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanNeon.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = targetService,
                                    color = CyanNeon,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = actionDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate300,
                            lineHeight = 20.sp
                        )

                        if (!recipientOrDestination.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = AmberNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Target: $recipientOrDestination",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate200,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (estimatedCost != null && estimatedCost > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CurrencyRupee,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Estimated Fare / Spend: ₹${estimatedCost.toInt()} (Non-Surge)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldNeon,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Security & Consequence Info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Verified Owner: Sir $ownerName",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = consequenceSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        fontSize = 11.sp
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate700),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Slate300
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("auth_deny_button")
                    ) {
                        Text(
                            text = "Deny / Cancel",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            isVerifying = true
                            onAuthorizeAndExecute()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberNeon,
                            contentColor = Slate950
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("auth_confirm_button")
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Slate950
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Executing...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Authorize & Run",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
