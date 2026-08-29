package com.example.ui.components

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.entity.OwnerAccountEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

@Composable
fun OwnerVaultSection(
    viewModel: VybViewModel,
    accounts: List<OwnerAccountEntity>,
    onAddAccountClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var accountToEdit by remember { mutableStateOf<OwnerAccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<OwnerAccountEntity?>(null) }

    // Dynamic Permission States
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var audioPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var galleryPermissionGranted by remember {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        )
    }

    var notificationPermissionGranted by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
        }
    }

    // Permission Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        Toast.makeText(
            context,
            if (granted) "Camera permission granted to VYB" else "Camera permission denied",
            Toast.LENGTH_SHORT
        ).show()
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        audioPermissionGranted = granted
        Toast.makeText(
            context,
            if (granted) "Microphone permission granted" else "Microphone permission denied",
            Toast.LENGTH_SHORT
        ).show()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        galleryPermissionGranted = granted
        Toast.makeText(
            context,
            if (granted) "Gallery access granted to VYB" else "Gallery access denied",
            Toast.LENGTH_SHORT
        ).show()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted = granted
    }

    val categories = listOf("All", "E-Commerce", "Social", "Messaging", "Mail", "Device & Media", "Finance")

    val filteredAccounts = remember(accounts, selectedCategory, searchQuery) {
        accounts.filter { account ->
            val matchesCategory = selectedCategory == "All" || account.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    account.serviceName.contains(searchQuery, ignoreCase = true) ||
                    account.accountIdentifier.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Vault Security Banner
        VybGlowCard(
            borderColor = CyanNeon.copy(alpha = 0.5f),
            glowColor = CyanGlow
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate800)
                        .border(1.dp, CyanNeon, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "Key Vault",
                        tint = CyanNeon,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Owner Memory & Account Vault",
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Store passwords & grant memory permissions for Amazon, Flipkart, WhatsApp, Mails & Sensors",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }
        }

        // Hardware & Media Permissions Hub
        VybGlowCard(
            borderColor = VioletNeon.copy(alpha = 0.4f),
            glowColor = VioletGlow
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PermDeviceInformation,
                            contentDescription = "Device Permissions",
                            tint = VioletNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hardware & Media Permissions",
                            style = MaterialTheme.typography.titleSmall,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Owner Controlled",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldNeon,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "Grant VYB hardware access to analyze camera photos, invoices, and voice prompts seamlessly:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )

                // Permission Rows
                PermissionItemRow(
                    title = "Camera Access",
                    subtitle = "Scan receipts, documents & visual tasks",
                    icon = Icons.Default.PhotoCamera,
                    isGranted = cameraPermissionGranted,
                    onRequest = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                    testTag = "perm_camera_btn"
                )

                PermissionItemRow(
                    title = "Gallery & Photos",
                    subtitle = "Access media memories & documents",
                    icon = Icons.Default.Collections,
                    isGranted = galleryPermissionGranted,
                    onRequest = {
                        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        galleryLauncher.launch(perm)
                    },
                    testTag = "perm_gallery_btn"
                )

                PermissionItemRow(
                    title = "Microphone & Voice",
                    subtitle = "AI voice commands & speech-to-text",
                    icon = Icons.Default.Mic,
                    isGranted = audioPermissionGranted,
                    onRequest = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    testTag = "perm_mic_btn"
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionItemRow(
                        title = "Notifications",
                        subtitle = "Task reminders & AI execution alerts",
                        icon = Icons.Default.Notifications,
                        isGranted = notificationPermissionGranted,
                        onRequest = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        testTag = "perm_notif_btn"
                    )
                }
            }
        }

        // Search Bar & Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connected Owner Accounts (${accounts.size})",
                style = MaterialTheme.typography.titleMedium,
                color = Slate100,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onAddAccountClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_account_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Account",
                    tint = Slate950,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add Account",
                    color = Slate950,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Search Input Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search Amazon, Flipkart, WhatsApp, Mail...", color = Slate500, fontSize = 13.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Slate400, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(18.dp))
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Slate900,
                unfocusedContainerColor = Slate900,
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = Slate800,
                focusedTextColor = Slate100,
                unfocusedTextColor = Slate200
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_search_field")
        )

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanNeon.copy(alpha = 0.2f),
                        selectedLabelColor = CyanNeon,
                        containerColor = Slate900,
                        labelColor = Slate400
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) CyanNeon else Slate800
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Accounts List
        if (filteredAccounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "No Accounts",
                        tint = Slate600,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No accounts match '$searchQuery'" else "No accounts in this category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate400
                    )
                }
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                filteredAccounts.forEach { account ->
                    OwnerAccountCard(
                        account = account,
                        onToggleMemory = { allowed ->
                            viewModel.toggleAccountAiMemory(account.id, allowed)
                        },
                        onEdit = { accountToEdit = account },
                        onDelete = { accountToDelete = account }
                    )
                }
            }
        }
    }

    // Edit Account Dialog
    if (accountToEdit != null) {
        EditAccountDialog(
            account = accountToEdit!!,
            onDismiss = { accountToEdit = null },
            onSave = { updated ->
                viewModel.updateOwnerAccount(updated)
                accountToEdit = null
                Toast.makeText(context, "Account updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    if (accountToDelete != null) {
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            containerColor = Slate900,
            title = {
                Text("Remove Account?", color = Slate100, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to remove ${accountToDelete?.serviceName} from your owner memory vault?",
                    color = Slate300
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        accountToDelete?.let { viewModel.deleteOwnerAccount(it) }
                        accountToDelete = null
                        Toast.makeText(context, "Account removed from vault", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonNeon)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Cancel", color = Slate400)
                }
            }
        )
    }
}

@Composable
private fun PermissionItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequest: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Slate900)
            .border(1.dp, if (isGranted) EmeraldNeon.copy(alpha = 0.3f) else Slate800, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isGranted) EmeraldNeon.copy(alpha = 0.15f) else AmberNeon.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isGranted) EmeraldNeon else AmberNeon,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Slate100,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                fontSize = 11.sp
            )
        }
        if (isGranted) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = EmeraldNeon.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmeraldNeon)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Granted",
                        color = EmeraldNeon,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            Button(
                onClick = onRequest,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmberNeon),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(
                    text = "Grant",
                    color = Slate950,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun OwnerAccountCard(
    account: OwnerAccountEntity,
    onToggleMemory: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var isPasswordVisible by remember { mutableStateOf(false) }

    fun copyToClipboard(text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    val iconVector = when (account.iconKey.lowercase()) {
        "amazon" -> Icons.Default.ShoppingCart
        "flipkart" -> Icons.Default.ShoppingBag
        "whatsapp" -> Icons.Default.Chat
        "facebook" -> Icons.Default.Public
        "mail" -> Icons.Default.Email
        "camera" -> Icons.Default.PhotoCamera
        "gallery" -> Icons.Default.Collections
        else -> Icons.Default.VpnKey
    }

    val iconColor = when (account.category.lowercase()) {
        "e-commerce" -> AmberNeon
        "messaging" -> EmeraldNeon
        "social" -> CyanNeon
        "mail" -> VioletNeon
        "device & media" -> CyanNeon
        else -> Slate300
    }

    VybGlowCard(
        borderColor = if (account.isAiMemoryAllowed) CyanNeon.copy(alpha = 0.35f) else Slate800,
        glowColor = if (account.isAiMemoryAllowed) CyanGlow else Color.Transparent
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Service Icon, Name, Category & Edit/Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconColor.copy(alpha = 0.15f))
                            .border(1.dp, iconColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = account.serviceName,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = account.serviceName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Slate800,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = account.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Account",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Account",
                            tint = CrimsonNeon.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = Slate800, thickness = 1.dp)

            // Account Identifier Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Account Identifier / Email",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontSize = 11.sp
                    )
                    Text(
                        text = account.accountIdentifier,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate200,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(
                    onClick = { copyToClipboard(account.accountIdentifier, "Account Identifier") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Identifier",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Password / Secret Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate900)
                    .border(1.dp, Slate800, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Security Key / Password",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate500,
                        fontSize = 10.sp
                    )
                    Text(
                        text = if (isPasswordVisible) account.passwordOrSecret else "••••••••••••",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPasswordVisible) CyanNeon else Slate300,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isPasswordVisible = !isPasswordVisible },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                            tint = if (isPasswordVisible) CyanNeon else Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { copyToClipboard(account.passwordOrSecret, "Password") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Password",
                            tint = Slate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (account.notes.isNotBlank()) {
                Text(
                    text = "Note: ${account.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400,
                    fontSize = 11.sp
                )
            }

            // AI Memory Permission Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate900.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Memory",
                        tint = if (account.isAiMemoryAllowed) CyanNeon else Slate500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Allow VYB AI Memory Access",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (account.isAiMemoryAllowed) Slate100 else Slate400,
                        fontWeight = FontWeight.Medium
                    )
                }
                Switch(
                    checked = account.isAiMemoryAllowed,
                    onCheckedChange = onToggleMemory,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Slate950,
                        checkedTrackColor = CyanNeon,
                        uncheckedThumbColor = Slate400,
                        uncheckedTrackColor = Slate800
                    ),
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
    }
}

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.padding(0.dp) // layout scale placeholder
)

@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (OwnerAccountEntity) -> Unit
) {
    val presets = listOf(
        "Amazon" to "E-Commerce",
        "Flipkart" to "E-Commerce",
        "WhatsApp" to "Messaging",
        "Facebook" to "Social",
        "Gmail / Mail" to "Mail",
        "Gallery Access" to "Device & Media",
        "Camera Access" to "Device & Media",
        "Custom" to "Other"
    )

    var serviceName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("E-Commerce") }
    var identifier by remember { mutableStateOf("tanmoytheperfect@gmail.com") }
    var password by remember { mutableStateOf("") }
    var isAiAllowed by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Owner Account", color = Slate100, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Preset quick chips
                Text("Quick Preset:", style = MaterialTheme.typography.labelSmall, color = Slate400)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets) { (presetName, presetCategory) ->
                        Surface(
                            onClick = {
                                serviceName = if (presetName == "Custom") "" else presetName
                                category = presetCategory
                            },
                            shape = RoundedCornerShape(6.dp),
                            color = if (serviceName == presetName) CyanNeon.copy(alpha = 0.2f) else Slate800,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (serviceName == presetName) CyanNeon else Slate700
                            )
                        ) {
                            Text(
                                text = presetName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (serviceName == presetName) CyanNeon else Slate300,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Service Name (e.g. Amazon, Flipkart)", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Account Identifier (Email / Username / Phone)", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password / PIN / Access Key", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant AI Memory Permission", style = MaterialTheme.typography.bodySmall, color = Slate200)
                    Switch(
                        checked = isAiAllowed,
                        onCheckedChange = { isAiAllowed = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate950,
                            checkedTrackColor = CyanNeon
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (serviceName.isNotBlank()) {
                        val iconKey = when {
                            serviceName.contains("amazon", ignoreCase = true) -> "amazon"
                            serviceName.contains("flipkart", ignoreCase = true) -> "flipkart"
                            serviceName.contains("whatsapp", ignoreCase = true) -> "whatsapp"
                            serviceName.contains("facebook", ignoreCase = true) -> "facebook"
                            serviceName.contains("mail", ignoreCase = true) || serviceName.contains("gmail", ignoreCase = true) -> "mail"
                            serviceName.contains("camera", ignoreCase = true) -> "camera"
                            serviceName.contains("gallery", ignoreCase = true) -> "gallery"
                            else -> "key"
                        }
                        onAdd(
                            OwnerAccountEntity(
                                serviceName = serviceName.trim(),
                                category = category,
                                accountIdentifier = identifier.trim(),
                                passwordOrSecret = password.trim(),
                                isAiMemoryAllowed = isAiAllowed,
                                iconKey = iconKey,
                                notes = notes.trim()
                            )
                        )
                    }
                },
                enabled = serviceName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
            ) {
                Text("Save Account", color = Slate950, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}

@Composable
fun EditAccountDialog(
    account: OwnerAccountEntity,
    onDismiss: () -> Unit,
    onSave: (OwnerAccountEntity) -> Unit
) {
    var serviceName by remember { mutableStateOf(account.serviceName) }
    var category by remember { mutableStateOf(account.category) }
    var identifier by remember { mutableStateOf(account.accountIdentifier) }
    var password by remember { mutableStateOf(account.passwordOrSecret) }
    var isAiAllowed by remember { mutableStateOf(account.isAiMemoryAllowed) }
    var notes by remember { mutableStateOf(account.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = {
            Text("Edit Owner Account", color = Slate100, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it },
                    label = { Text("Service Name", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Account Identifier / Email", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password / Secret", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate200,
                        focusedBorderColor = CyanNeon,
                        unfocusedBorderColor = Slate700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Allow VYB AI Memory Access", style = MaterialTheme.typography.bodySmall, color = Slate200)
                    Switch(
                        checked = isAiAllowed,
                        onCheckedChange = { isAiAllowed = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate950,
                            checkedTrackColor = CyanNeon
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (serviceName.isNotBlank()) {
                        onSave(
                            account.copy(
                                serviceName = serviceName.trim(),
                                category = category,
                                accountIdentifier = identifier.trim(),
                                passwordOrSecret = password.trim(),
                                isAiMemoryAllowed = isAiAllowed,
                                notes = notes.trim()
                            )
                        )
                    }
                },
                enabled = serviceName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
            ) {
                Text("Save Changes", color = Slate950, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Slate400)
            }
        }
    )
}
