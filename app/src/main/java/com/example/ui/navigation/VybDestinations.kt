package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Ask : Screen("ask", "Search", Icons.Default.ChatBubbleOutline)
    object CenterVyb : Screen("center_vyb", "VYB", Icons.Default.AutoAwesome)
    object Memory : Screen("memory", "Memory", Icons.Default.Psychology)
    object You : Screen("you", "You", Icons.Default.PersonOutline)

    // Sub-screens
    object Life : Screen("life", "Life", Icons.Default.AccountTree)
    object Actions : Screen("actions", "Actions", Icons.Default.Bolt)
    object Control : Screen("control", "Control", Icons.Default.Tune)
}
