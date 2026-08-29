package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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

    if (!isOnboardingCompleted) {
        ExecutiveOnboardingScreen(
            viewModel = viewModel,
            onOnboardingComplete = {
                // Handled in ViewModel
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

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Life,
        Screen.Actions,
        Screen.Memory,
        Screen.Control
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Slate950,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            listOf(CardBorder, Slate950)
                        ),
                        shape = androidx.compose.ui.graphics.RectangleShape
                    )
            ) {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label,
                                tint = if (isSelected) CyanNeon else Slate400,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.label,
                                color = if (isSelected) CyanNeon else Slate400,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Slate850
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        },
        containerColor = Slate950
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
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

            composable(Screen.Life.route) {
                LifeScreen(
                    viewModel = viewModel,
                    onOpenPromptInHome = { prompt ->
                        viewModel.startNewConversation(prompt) {}
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

            composable(Screen.Actions.route) {
                ActionsScreen(
                    viewModel = viewModel,
                    onExecuteActionPrompt = { prompt ->
                        viewModel.startNewConversation(prompt) {}
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

            composable(Screen.Control.route) {
                ProfileScreen(
                    viewModel = viewModel
                )
            }
        }
    }
}
