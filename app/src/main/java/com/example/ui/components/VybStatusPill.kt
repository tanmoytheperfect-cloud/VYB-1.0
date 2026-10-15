package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Truthful status pill displaying VYB's real-time observing and cognitive status.
 */
@Composable
fun VybStatusPill(
    visualState: VybVisualState,
    modifier: Modifier = Modifier,
    customText: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "StatusDotPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val (dotColor, textColor, borderColor) = when (visualState) {
        VybVisualState.IDLE -> Triple(Color(0xFF00E5FF), Slate200, Color(0x3300E5FF))
        VybVisualState.READY -> Triple(Color(0xFF10B981), Slate100, Color(0x3310B981))
        VybVisualState.LISTENING -> Triple(Color(0xFF38BDF8), Slate100, Color(0x4438BDF8))
        VybVisualState.THINKING -> Triple(Color(0xFF8B5CF6), Color(0xFFE9D5FF), Color(0x448B5CF6))
        VybVisualState.PLANNING -> Triple(Color(0xFF6366F1), Color(0xFFE0E7FF), Color(0x446366F1))
        VybVisualState.ACTING -> Triple(Color(0xFF00E5FF), Slate100, Color(0x5500E5FF))
        VybVisualState.VERIFYING -> Triple(Color(0xFF10B981), Slate100, Color(0x4410B981))
        VybVisualState.SUCCESS -> Triple(Color(0xFF10B981), Slate100, Color(0x4410B981))
        VybVisualState.WAITING -> Triple(Color(0xFF818CF8), Slate200, Color(0x33818CF8))
        VybVisualState.NEEDS_ATTENTION -> Triple(Color(0xFFF59E0B), Color(0xFFFEF3C7), Color(0x44F59E0B))
        VybVisualState.OFFLINE -> Triple(Color(0xFF64748B), Slate400, Color(0x2264748B))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0xFF0B1120).copy(alpha = 0.75f))
            .border(1.dp, borderColor, RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = dotAlpha))
            )

            Text(
                text = customText ?: visualState.label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp
            )
        }
    }
}
