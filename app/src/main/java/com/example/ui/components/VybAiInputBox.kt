package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun VybAiInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Ask VYB anything... diet, trip, tasks, shopping, budget",
    isLoading: Boolean = false,
    minHeight: Dp = 64.dp
) {
    val isFilled = value.isNotBlank()

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = CyanGlow,
                spotColor = VioletGlow
            )
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Slate900,
                        Slate850
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = if (isFilled) {
                        listOf(CyanNeon, VioletNeon)
                    } else {
                        listOf(CardBorderGlow, CardBorder)
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight - 24.dp)
        ) {
            // Glow Sparkle Icon
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Slate800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Sparkle",
                    tint = if (isFilled) CyanNeon else Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Text Field
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            color = Slate400,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 20.sp
                        ),
                        maxLines = 2
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = Slate100,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    ),
                    cursorBrush = SolidColor(CyanNeon),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (isFilled && !isLoading) {
                                onSend(value)
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_input_text_field")
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Voice Mic Button
            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Slate800.copy(alpha = 0.8f))
                    .testTag("mic_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send / Action Button
            IconButton(
                onClick = {
                    if (isFilled && !isLoading) {
                        onSend(value)
                    }
                },
                enabled = isFilled && !isLoading,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isFilled && !isLoading) {
                            Brush.linearGradient(listOf(CyanNeon, VioletNeon))
                        } else {
                            SolidColor(Slate800)
                        }
                    )
                    .testTag("send_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = CyanNeon
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Request",
                        tint = if (isFilled) Slate950 else Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
