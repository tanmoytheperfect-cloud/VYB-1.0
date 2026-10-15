package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * Premium translucent dark glassmorphism card for the VYB Life OS.
 */
@Composable
fun VybGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    borderColor: Color = Color(0x3338BDF8),
    borderGradient: Brush? = null,
    backgroundColor: Color = Color(0xFF0F172A).copy(alpha = 0.55f),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val appliedBorder = borderGradient?.let {
        Modifier.border(1.dp, it, shape)
    } ?: Modifier.border(1.dp, borderColor, shape)

    val clickableMod = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Surface(
        modifier = modifier
            .clip(shape)
            .then(appliedBorder)
            .background(backgroundColor)
            .then(clickableMod),
        color = Color.Transparent,
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
