package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

@Composable
fun AppLockScreen(
    viewModel: VybViewModel,
    ownerName: String,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val locationTelemetry by viewModel.locationTelemetry.collectAsStateWithLifecycle()
    val systemTelemetry by viewModel.systemTelemetry.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_reactor")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )
    val reverseRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reverse_rotation"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    fun handleDigit(digit: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            isError = false
            if (newPin.length == 4) {
                val success = viewModel.unlockApp(newPin)
                if (success) {
                    viewModel.speakJarvisGreeting()
                    onUnlocked()
                } else {
                    isError = true
                    errorMessage = "ACCESS DENIED • Invalid PIN (Default is 1234)"
                    enteredPin = ""
                }
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isError = false
        }
    }

    fun handleClear() {
        enteredPin = ""
        isError = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stark Industries / JARVIS Header Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate900.copy(alpha = 0.8f))
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CyanNeon)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "STARK OS // JARVIS PROTOCOL",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                }
                Text(
                    text = "${locationTelemetry.city.uppercase()}, ${locationTelemetry.countryCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberNeon,
                    fontWeight = FontWeight.Bold
                )
            }

            // Animated Iron Man Arc Reactor Core
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(rotationAngle)
                ) {
                    drawCircle(
                        color = CyanNeon.copy(alpha = 0.2f),
                        radius = size.minDimension / 2.1f,
                        style = Stroke(width = 3f)
                    )
                    for (i in 0 until 6) {
                        val angle = Math.toRadians((i * 60).toDouble())
                        val r = size.minDimension / 2.3f
                        val x = center.x + (r * Math.cos(angle)).toFloat()
                        val y = center.y + (r * Math.sin(angle)).toFloat()
                        drawCircle(
                            color = CyanNeon,
                            radius = 4f,
                            center = Offset(x, y)
                        )
                    }
                }

                Canvas(
                    modifier = Modifier
                        .size(80.dp)
                        .rotate(reverseRotation)
                ) {
                    drawCircle(
                        color = VioletNeon.copy(alpha = 0.35f),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 2.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    CyanNeon.copy(alpha = 0.5f * pulseAlpha),
                                    Slate900
                                )
                            )
                        )
                        .border(2.dp, CyanNeon, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Arc Reactor Shield",
                        tint = CyanNeon,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Identification Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "OWNER VERIFICATION",
                    style = MaterialTheme.typography.titleLarge,
                    color = Slate100,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Welcome back, Sir $ownerName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyanNeon,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Arc Reactor Core: ${systemTelemetry.arcReactorCorePower}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontFamily = FontFamily.Monospace
                )
            }

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isError -> CrimsonNeon
                                    isFilled -> CyanNeon
                                    else -> Slate850
                                }
                            )
                            .border(
                                1.5.dp,
                                when {
                                    isError -> CrimsonNeon
                                    isFilled -> CyanNeon
                                    else -> Slate700
                                },
                                CircleShape
                            )
                    )
                }
            }

            // Error Message
            AnimatedVisibility(visible = isError) {
                Text(
                    text = errorMessage,
                    color = CrimsonNeon,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Numeric Keypad
            Column(
                modifier = Modifier.widthIn(max = 290.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL")
                )

                keypadRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "C" -> {
                                    KeypadButton(
                                        text = "C",
                                        onClick = { handleClear() },
                                        isAction = true,
                                        testTag = "keypad_clear"
                                    )
                                }
                                "DEL" -> {
                                    KeypadIconButton(
                                        icon = Icons.Default.Backspace,
                                        onClick = { handleBackspace() },
                                        testTag = "keypad_backspace"
                                    )
                                }
                                else -> {
                                    KeypadButton(
                                        text = key,
                                        onClick = { handleDigit(key) },
                                        testTag = "keypad_num_$key"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Biometric Iron Man Arc Scanner
            Button(
                onClick = {
                    viewModel.bypassBiometricUnlock()
                    viewModel.speakJarvisGreeting()
                    onUnlocked()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Slate900
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(CyanNeon, VioletNeon))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("biometric_unlock_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Bypass",
                    tint = CyanNeon,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "BIOMETRIC PALM / FINGERPRINT SCAN",
                    color = Slate100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp
                )
            }

            // Telemetry Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEFAULT PIN: 1234",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "GPS: ${locationTelemetry.formattedCoordinates}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    isAction: Boolean = false,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isAction) Slate850 else Slate900,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAction) CardBorder else Slate700
        ),
        modifier = Modifier
            .size(60.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = if (isAction) AmberNeon else Slate100,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Slate850,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .size(60.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Keypad Action",
                tint = Slate300,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
