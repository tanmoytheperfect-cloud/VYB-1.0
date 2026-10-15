package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.cognitive.models.ContextualHero
import com.example.domain.cognitive.models.ContextualHeroMode
import com.example.ui.theme.*

/**
 * Contextual Meaning Hero Card for Home Screen (Requirement 18).
 * Selects and showcases what matters NOW in the owner's life rather than overloading
 * the screen with raw telemetry dashboards.
 */
@Composable
fun VybContextualHeroCard(
    hero: ContextualHero,
    modifier: Modifier = Modifier
) {
    val borderColor = when (hero.mode) {
        ContextualHeroMode.ON_THE_MOVE -> Color(0x3338BDF8) // Cyan
        ContextualHeroMode.WALKING -> Color(0x3310B981)    // Emerald
        ContextualHeroMode.POOR_AIR_ALERT -> Color(0x44F97316) // Orange/Amber
        ContextualHeroMode.OFFICE_WORK -> Color(0x338B5CF6)  // Violet
        else -> Color(0x2238BDF8)
    }

    val badgeColor = when (hero.mode) {
        ContextualHeroMode.ON_THE_MOVE -> CyanNeon
        ContextualHeroMode.WALKING -> EmeraldNeon
        ContextualHeroMode.POOR_AIR_ALERT -> AmberNeon
        ContextualHeroMode.OFFICE_WORK -> VioletNeon
        else -> CyanNeon
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.78f),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_contextual_meaning_hero")
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Status badge and mode tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Text(
                        text = hero.badge,
                        color = badgeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        fontSize = 11.sp
                    )
                }

                // Meaning icon
                Icon(
                    imageVector = when (hero.mode) {
                        ContextualHeroMode.ON_THE_MOVE -> Icons.Default.DirectionsCar
                        ContextualHeroMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
                        ContextualHeroMode.POOR_AIR_ALERT -> Icons.Default.Warning
                        ContextualHeroMode.OFFICE_WORK -> Icons.Default.Work
                        else -> Icons.Default.Home
                    },
                    contentDescription = hero.title,
                    tint = badgeColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle: Title & Primary Metric (if any)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hero.title,
                        color = Slate100,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = hero.subtitle,
                        color = Slate300,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp
                    )
                }

                if (hero.primaryMetric != null) {
                    Text(
                        text = hero.primaryMetric,
                        color = badgeColor,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom: Explanatory Context / Learned Evidence
            if (hero.detail.isNotBlank() || hero.evidenceExplanation != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color(0x18FFFFFF)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (hero.detail.isNotBlank()) {
                    Text(
                        text = hero.detail,
                        color = Slate300,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                if (hero.evidenceExplanation != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• ${hero.evidenceExplanation}",
                        color = Slate500,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
