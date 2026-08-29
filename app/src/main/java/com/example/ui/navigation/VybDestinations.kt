package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "VYB", Icons.Default.Psychology)
    object Life : Screen("life", "Life", Icons.Default.AccountTree)
    object Actions : Screen("actions", "Actions", Icons.Default.Bolt)
    object Memory : Screen("memory", "Memory", Icons.Default.Memory)
    object Control : Screen("control", "Control", Icons.Default.Tune)
}
