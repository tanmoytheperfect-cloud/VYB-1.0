package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Screen
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

@Composable
fun VybApp(
    viewModel: VybViewModel = viewModel()
) {
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isSecurityLockEnabled by viewModel.isSecurityLockEnabled.collectAsStateWithLifecycle()
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val pendingTransferProposal by viewModel.pendingTransferProposal.collectAsStateWithLifecycle()

    if (pendingTransferProposal != null) {
        com.example.ui.components.FluidPrivacyApprovalDialog(
            proposal = pendingTransferProposal!!,
            onAuthorize = { token, finalPayload ->
                viewModel.onProposalAuthorized(pendingTransferProposal!!.transferId, token, finalPayload)
            },
            onDeny = { reason ->
                viewModel.denyTransferProposal(pendingTransferProposal!!.transferId, reason)
            },
            onCancel = {
                viewModel.cancelTransferProposal()
            },
            onEnableLocalOnlyMode = {
                viewModel.setLocalOnlyMode(true)
            }
        )
    }

    if (!isOnboardingCompleted) {
        PersonalGenesisScreen(
            viewModel = viewModel,
            onGenesisComplete = {
                viewModel.completeOnboarding()
            }
        )
        return
    }

    if (isAppLocked && isSecurityLockEnabled) {
        AppLockScreen(
            viewModel = viewModel,
            ownerName = ownerName,
            onUnlocked = {
                // Handled in ViewModel unlockApp() / bypassBiometricUnlock()
            }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            // Premium custom bottom navigation bar matching the visual reference design
            Surface(
                color = Color(0xFF070B14).copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(68.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(Color(0x2238BDF8), Color(0xFF070B14))
                        ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. HOME TAB
                    val isHomeSelected = currentRoute == Screen.Home.route
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isHomeSelected) {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            .testTag("nav_item_home")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (isHomeSelected) CyanNeon else Slate400,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Home",
                            color = if (isHomeSelected) CyanNeon else Slate400,
                            fontSize = 11.sp,
                            fontWeight = if (isHomeSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // 2. SEARCH / ASK TAB
                    val isAskSelected = currentRoute == Screen.Ask.route
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isAskSelected) {
                                        navController.navigate(Screen.Ask.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            .testTag("nav_item_search")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Search",
                            tint = if (isAskSelected) CyanNeon else Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Search",
                            color = if (isAskSelected) CyanNeon else Slate400,
                            fontSize = 11.sp,
                            fontWeight = if (isAskSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // 3. CENTER PROMINENT LUMINOUS VYB BUTTON
                    Box(
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF38BDF8),
                                            Color(0xFF818CF8),
                                            Color(0xFF1E1B4B)
                                        )
                                    )
                                )
                                .border(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        listOf(Color.White.copy(alpha = 0.8f), CyanNeon, VioletNeon)
                                    ),
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        navController.navigate(Screen.Ask.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                                .testTag("nav_item_center_vyb"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "VYB",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // 4. MEMORY TAB
                    val isMemorySelected = currentRoute == Screen.Memory.route
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isMemorySelected) {
                                        navController.navigate(Screen.Memory.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            .testTag("nav_item_memory")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Memory",
                            tint = if (isMemorySelected) CyanNeon else Slate400,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Memory",
                            color = if (isMemorySelected) CyanNeon else Slate400,
                            fontSize = 11.sp,
                            fontWeight = if (isMemorySelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    // 5. YOU TAB (Profile)
                    val isYouSelected = currentRoute == Screen.You.route
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isYouSelected) {
                                        navController.navigate(Screen.You.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            .testTag("nav_item_you")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = "You",
                            tint = if (isYouSelected) CyanNeon else Slate400,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "You",
                            color = if (isYouSelected) CyanNeon else Slate400,
                            fontSize = 11.sp,
                            fontWeight = if (isYouSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFF070B14)
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAsk = {
                        navController.navigate(Screen.Ask.route)
                    },
                    onNavigateToLife = {
                        navController.navigate(Screen.Life.route)
                    },
                    onNavigateToActions = {
                        navController.navigate(Screen.Actions.route)
                    },
                    onNavigateToMemory = {
                        navController.navigate(Screen.Memory.route)
                    }
                )
            }

            composable(Screen.Ask.route) {
                AskVybScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Memory.route) {
                MemoryScreen(
                    viewModel = viewModel
                )
            }

            composable(Screen.You.route) {
                ProfileScreen(
                    viewModel = viewModel
                )
            }

            composable(Screen.Life.route) {
                LifeScreen(
                    viewModel = viewModel,
                    onOpenPromptInHome = { prompt ->
                        viewModel.startNewConversation(prompt) {}
                        navController.navigate(Screen.Ask.route)
                    }
                )
            }

            composable(Screen.Actions.route) {
                ActionsScreen(
                    viewModel = viewModel,
                    onExecuteActionPrompt = { prompt ->
                        viewModel.startNewConversation(prompt) {}
                        navController.navigate(Screen.Ask.route)
                    }
                )
            }
        }
    }
}
