package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.privacy.DataCategory
import com.example.domain.privacy.FluidPrivacyFirewall
import com.example.domain.privacy.OutboundTransferProposal
import com.example.domain.privacy.TransferAuthorizationToken
import com.example.ui.theme.*

/**
 * Explicit Privacy Firewall Approval Dialog.
 *
 * Mandatory requirement: Fresh, explicit human review & approval before any external transfer
 * of personal information. Shows destination, purpose, data categories, and actual proposed
 * payload with granular redaction support, denial, cancellation, or activating Local-Only Mode.
 */
@Composable
fun FluidPrivacyApprovalDialog(
    proposal: OutboundTransferProposal,
    onAuthorize: (token: TransferAuthorizationToken, finalPayload: String) -> Unit,
    onDeny: (reason: String) -> Unit,
    onCancel: () -> Unit,
    onEnableLocalOnlyMode: () -> Unit
) {
    var redactFinancial by remember { mutableStateOf(proposal.redactedCategories.contains(DataCategory.FINANCIAL_DATA)) }
    var redactFamily by remember { mutableStateOf(proposal.redactedCategories.contains(DataCategory.FAMILY_AND_RELATIONSHIPS)) }
    var redactVault by remember { mutableStateOf(proposal.redactedCategories.contains(DataCategory.PERSONAL_LIFE_CONTEXT)) }

    fun updateFirewallRedactions(fin: Boolean, fam: Boolean, vlt: Boolean) {
        val cats = mutableSetOf<DataCategory>()
        if (fin) cats.add(DataCategory.FINANCIAL_DATA)
        if (fam) cats.add(DataCategory.FAMILY_AND_RELATIONSHIPS)
        if (vlt) cats.add(DataCategory.PERSONAL_LIFE_CONTEXT)
        FluidPrivacyFirewall.applyRedactions(proposal.transferId, cats)
    }

    val pendingProposalState by FluidPrivacyFirewall.pendingProposal.collectAsState()
    val effectiveProposal = if (pendingProposalState?.transferId == proposal.transferId) pendingProposalState!! else proposal

    val activePayload = effectiveProposal.candidatePayload.ifEmpty { effectiveProposal.payloadPreview }
    val currentHash = effectiveProposal.payloadHash

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Slate900,
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(AmberNeon, CyanNeon))),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp)
                .testTag("fluid_privacy_approval_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AmberNeon.copy(alpha = 0.2f))
                                .border(1.dp, AmberNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = AmberNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "FLUID PRIVACY FIREWALL",
                                color = AmberNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Explicit Outbound Egress Review",
                                color = Slate300,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.testTag("firewall_cancel_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Slate400)
                    }
                }

                HorizontalDivider(color = Slate800)

                // Destination Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DESTINATION SERVICE",
                                color = CyanNeon,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = proposal.destination.name,
                            color = Slate100,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = proposal.destination.endpointOrPackage,
                            color = Slate400,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Purpose: ${proposal.purpose}",
                            color = Slate300,
                            fontSize = 11.sp
                        )
                    }
                }

                // Data Categories Detected
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DETECTED DATA CATEGORIES",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        effectiveProposal.categories.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AmberNeon.copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, AmberNeon.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = cat.displayName,
                                    color = AmberNeon,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Granular Redaction Controls
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate950,
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "PRIVACY REDACTION TOGGLES",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Redact Financial Data (Budget / Spending)", color = Slate200, fontSize = 11.sp)
                            Switch(
                                checked = redactFinancial,
                                onCheckedChange = {
                                    redactFinancial = it
                                    updateFirewallRedactions(it, redactFamily, redactVault)
                                },
                                modifier = Modifier.testTag("redact_financial_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Redact Family & Contacts", color = Slate200, fontSize = 11.sp)
                            Switch(
                                checked = redactFamily,
                                onCheckedChange = {
                                    redactFamily = it
                                    updateFirewallRedactions(redactFinancial, it, redactVault)
                                },
                                modifier = Modifier.testTag("redact_family_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Redact Personal Life Vault", color = Slate200, fontSize = 11.sp)
                            Switch(
                                checked = redactVault,
                                onCheckedChange = {
                                    redactVault = it
                                    updateFirewallRedactions(redactFinancial, redactFamily, it)
                                },
                                modifier = Modifier.testTag("redact_vault_switch")
                            )
                        }
                    }
                }

                // Payload Review
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PAYLOAD REVIEWABLE PREVIEW",
                            color = Slate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "SHA: ${currentHash.take(10)}...",
                            color = CyanNeon,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate950,
                        border = BorderStroke(0.5.dp, Slate800),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp)
                    ) {
                        Text(
                            text = activePayload,
                            color = Slate300,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp,
                            modifier = Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }

                HorizontalDivider(color = Slate800)

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val token = FluidPrivacyFirewall.approveProposal(effectiveProposal.transferId)
                            if (token != null) {
                                onAuthorize(token, token.authorizedPayload ?: activePayload)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("firewall_authorize_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Authorize & Send Payload", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onDeny("Declined by owner in Firewall dialog") },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonNeon),
                            border = BorderStroke(1.dp, CrimsonNeon.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("firewall_deny_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Deny Transfer", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onEnableLocalOnlyMode()
                                onDeny("Switched to Local-Only Mode")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberNeon),
                            border = BorderStroke(1.dp, AmberNeon.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("firewall_local_only_toggle"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Local-Only Mode", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
