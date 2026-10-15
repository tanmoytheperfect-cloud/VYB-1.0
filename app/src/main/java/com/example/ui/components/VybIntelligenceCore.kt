package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.temporal.VybTimeOfDay
import com.example.ui.theme.*
import kotlin.math.*

/**
 * Visual operational states representing VYB's real-time cognitive lifecycle.
 */
enum class VybVisualState(val label: String) {
    IDLE("VYB is observing"),
    READY("VYB is ready"),
    LISTENING("VYB is listening"),
    THINKING("VYB is thinking"),
    PLANNING("VYB is planning"),
    ACTING("VYB is acting"),
    VERIFYING("VYB is verifying"),
    SUCCESS("VYB completed action"),
    WAITING("VYB is waiting"),
    NEEDS_ATTENTION("VYB needs your attention"),
    OFFLINE("VYB is offline")
}

/**
 * Living VYB Intelligence Core.
 * Multi-layered organic intelligence entity created using native Compose graphics,
 * fluid sine-wave morphing, independent rotational layers, and atmospheric depth.
 */
@Composable
fun VybIntelligenceCore(
    modifier: Modifier = Modifier,
    visualState: VybVisualState = VybVisualState.IDLE,
    timeOfDay: VybTimeOfDay = VybTimeOfDay.DAY,
    size: Dp = 190.dp,
    hasCompletedArrival: Boolean = true,
    onArrivalFinished: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    // Session arrival animation controller (0.0 to 1.0)
    val arrivalProgress = remember { Animatable(if (hasCompletedArrival) 1f else 0f) }

    LaunchedEffect(hasCompletedArrival) {
        if (!hasCompletedArrival) {
            arrivalProgress.snapTo(0f)
            arrivalProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 1400,
                    easing = FastOutSlowInEasing
                )
            )
            onArrivalFinished()
        }
    }

    // Infinite transitions for independent non-synchronized layer motion
    val infiniteTransition = rememberInfiniteTransition(label = "VybCoreLivingLoop")

    // Combine visualState with timeOfDay for harmonious cognitive & celestial presence
    val (primaryGlowColor, secondaryGlowColor, coreSpeedMultiplier) = when (visualState) {
        VybVisualState.IDLE -> when (timeOfDay) {
            VybTimeOfDay.DAWN -> Triple(Color(0xFF00E5FF), Color(0xFFC084FC), 0.95f) // gentle morning awakening
            VybTimeOfDay.MORNING -> Triple(Color(0xFF00E5FF), Color(0xFF8B5CF6), 1.05f) // fresh morning vitality
            VybTimeOfDay.DAY -> Triple(Color(0xFF00E5FF), Color(0xFF6366F1), 1.0f) // clear day observing
            VybTimeOfDay.EVENING -> Triple(Color(0xFF38BDF8), Color(0xFFA855F7), 0.90f) // sunset calm transition
            VybTimeOfDay.NIGHT -> Triple(Color(0xFF38BDF8), Color(0xFF818CF8), 0.78f) // restful night breathing
        }
        VybVisualState.READY -> Triple(Color(0xFF10B981), Color(0xFF00E5FF), 1.0f)
        VybVisualState.LISTENING -> when (timeOfDay) {
            VybTimeOfDay.NIGHT -> Triple(Color(0xFF38BDF8), Color(0xFFC084FC), 1.35f) // night receptive focus
            else -> Triple(Color(0xFF38BDF8), Color(0xFF818CF8), 1.3f)
        }
        VybVisualState.THINKING -> Triple(Color(0xFF8B5CF6), Color(0xFFEC4899), 1.8f) // active internal energy/orbits
        VybVisualState.PLANNING -> Triple(Color(0xFF6366F1), Color(0xFF06B6D4), 1.4f)
        VybVisualState.ACTING -> Triple(Color(0xFF00E5FF), Color(0xFF3B82F6), 2.0f) // outward radiant energy
        VybVisualState.VERIFYING -> Triple(Color(0xFF10B981), Color(0xFF6366F1), 1.2f) // focused stable core
        VybVisualState.SUCCESS -> Triple(Color(0xFF10B981), Color(0xFF34D399), 1.0f)
        VybVisualState.WAITING -> Triple(Color(0xFF818CF8), Color(0xFF38BDF8), 0.7f)
        VybVisualState.NEEDS_ATTENTION -> Triple(Color(0xFFF59E0B), Color(0xFFEF4444), 1.5f)
        VybVisualState.OFFLINE -> Triple(Color(0xFF64748B), Color(0xFF334155), 0.3f)
    }

    // Dynamic breathing cycle duration: slower and restful at night, more active when thinking/acting
    val breathCycleDuration = when {
        visualState == VybVisualState.THINKING || visualState == VybVisualState.ACTING -> 3200
        timeOfDay == VybTimeOfDay.NIGHT && visualState == VybVisualState.IDLE -> 6200
        else -> 4800
    }

    val maxBreathScale = when {
        visualState == VybVisualState.LISTENING -> 1.09f // expands subtly when listening
        timeOfDay == VybTimeOfDay.NIGHT && visualState == VybVisualState.IDLE -> 1.04f // slow restful breathing
        else -> 1.06f
    }

    // Layer 1: Breathing cycle
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = maxBreathScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = breathCycleDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    // Layer 2: Independent rotation Layer A (18 seconds, clockwise)
    val rotationA by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationA"
    )

    // Layer 3: Independent rotation Layer B (27 seconds, counter-clockwise)
    val rotationB by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 27000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationB"
    )

    // Layer 4: Independent rotation Layer C (12 seconds, clockwise)
    val rotationC by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationC"
    )

    // Layer 5: Fluid shape phase shift for organic morphing
    val fluidPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fluidPhase"
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = arrivalProgress.value
            if (progress <= 0.01f) return@Canvas

            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (min(this.size.width, this.size.height) / 2.25f) * progress * breathScale

            // ==========================================
            // LAYER 1: OUTER ATMOSPHERIC HALO GLOW
            // ==========================================
            val haloRadius = baseRadius * 1.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlowColor.copy(alpha = 0.22f * progress),
                        secondaryGlowColor.copy(alpha = 0.10f * progress),
                        Color.Transparent
                    ),
                    center = center,
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = center
            )

            // ==========================================
            // LAYER 2: TRANSLUCENT OUTER FLUID SHAPE (Morphing)
            // ==========================================
            rotate(rotationA * coreSpeedMultiplier, pivot = center) {
                drawFluidMorphBlob(
                    center = center,
                    radius = baseRadius * 1.08f,
                    phase = fluidPhase,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlowColor.copy(alpha = 0.28f * progress),
                            secondaryGlowColor.copy(alpha = 0.12f * progress),
                            Color.Transparent
                        ),
                        center = center.copy(
                            x = center.x + sin(fluidPhase) * 8f,
                            y = center.y + cos(fluidPhase) * 8f
                        ),
                        radius = baseRadius * 1.15f
                    )
                )
            }

            // ==========================================
            // LAYER 3: SECONDARY ROTATING FLUID LAYER (Layer B)
            // ==========================================
            rotate(rotationB * coreSpeedMultiplier, pivot = center) {
                drawFluidMorphBlob(
                    center = center,
                    radius = baseRadius * 0.94f,
                    phase = fluidPhase + PI.toFloat(),
                    brush = Brush.linearGradient(
                        colors = listOf(
                            secondaryGlowColor.copy(alpha = 0.40f * progress),
                            primaryGlowColor.copy(alpha = 0.25f * progress),
                            Color(0x1000E5FF)
                        ),
                        start = Offset(center.x - baseRadius, center.y - baseRadius),
                        end = Offset(center.x + baseRadius, center.y + baseRadius)
                    )
                )
            }

            // ==========================================
            // LAYER 4: INNER HIGH-DENSITY ORGANIC CORE
            // ==========================================
            rotate(rotationC * coreSpeedMultiplier, pivot = center) {
                scale(scaleX = 1f + sin(fluidPhase * 1.5f) * 0.04f, scaleY = 1f + cos(fluidPhase * 1.5f) * 0.04f, pivot = center) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0F172A),
                                Color(0xFF0B1020),
                                secondaryGlowColor.copy(alpha = 0.45f * progress),
                                primaryGlowColor.copy(alpha = 0.65f * progress)
                            ),
                            center = center.copy(y = center.y - baseRadius * 0.15f),
                            radius = baseRadius * 0.76f
                        ),
                        radius = baseRadius * 0.76f,
                        center = center
                    )
                }
            }

            // ==========================================
            // LAYER 5: LUMINOUS INTERNAL REFRACTION RINGS
            // ==========================================
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryGlowColor.copy(alpha = 0.85f * progress),
                        Color.Transparent,
                        secondaryGlowColor.copy(alpha = 0.90f * progress),
                        Color.Transparent,
                        primaryGlowColor.copy(alpha = 0.85f * progress)
                    ),
                    center = center
                ),
                radius = baseRadius * 0.72f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.2.dp.toPx()
                )
            )

            // ==========================================
            // LAYER 6: INNER NUCLEUS HIGHLIGHT & SHADOW
            // ==========================================
            val highlightCenter = center.copy(
                x = center.x - baseRadius * 0.22f,
                y = center.y - baseRadius * 0.25f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f * progress),
                        primaryGlowColor.copy(alpha = 0.25f * progress),
                        Color.Transparent
                    ),
                    center = highlightCenter,
                    radius = baseRadius * 0.45f
                ),
                radius = baseRadius * 0.45f,
                center = highlightCenter
            )

            // Micro Center Dot (Intelligence Core Eye)
            drawCircle(
                color = Color.White.copy(alpha = 0.85f * progress),
                radius = 3.dp.toPx() * (1f + sin(fluidPhase * 2f) * 0.2f),
                center = center
            )

            // ==========================================
            // LAYER 7: CENTRAL WINGED "V" / IRIS EMBEDDED MEMBRANE
            // (Signature translucent glowing fluid crest from the reference target)
            // ==========================================
            val vWidth = baseRadius * 0.50f
            val vHeight = baseRadius * 0.46f
            val bottomY = center.y + vHeight * 0.35f + sin(fluidPhase) * 2.5f
            val topY = center.y - vHeight * 0.40f + cos(fluidPhase) * 2f

            val vPath = Path().apply {
                moveTo(center.x, bottomY)
                // Left wing curve
                cubicTo(
                    center.x - vWidth * 0.45f, bottomY - vHeight * 0.2f,
                    center.x - vWidth * 0.70f, topY + vHeight * 0.35f,
                    center.x - vWidth * 0.38f, topY
                )
                // Left inner curve back to center cleft
                cubicTo(
                    center.x - vWidth * 0.22f, topY + vHeight * 0.35f,
                    center.x - vWidth * 0.08f, bottomY - vHeight * 0.15f,
                    center.x, bottomY - vHeight * 0.18f
                )
                // Right inner curve to right wing tip
                cubicTo(
                    center.x + vWidth * 0.08f, bottomY - vHeight * 0.15f,
                    center.x + vWidth * 0.22f, topY + vHeight * 0.35f,
                    center.x + vWidth * 0.38f, topY
                )
                // Right wing curve back to bottom
                cubicTo(
                    center.x + vWidth * 0.70f, topY + vHeight * 0.35f,
                    center.x + vWidth * 0.45f, bottomY - vHeight * 0.2f,
                    center.x, bottomY
                )
                close()
            }
            drawPath(
                path = vPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.88f * progress),
                        Color(0xFF2DD4BF).copy(alpha = 0.75f * progress),
                        Color(0xFFA78BFA).copy(alpha = 0.55f * progress)
                    ),
                    startY = topY,
                    endY = bottomY
                )
            )

            // Outer glass bubble rim with subtle specular highlight
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.40f * progress),
                        Color(0xFF38BDF8).copy(alpha = 0.28f * progress),
                        Color(0xFFA855F7).copy(alpha = 0.22f * progress),
                        Color.Transparent,
                        Color(0xFF2DD4BF).copy(alpha = 0.32f * progress),
                        Color.White.copy(alpha = 0.40f * progress)
                    ),
                    center = center
                ),
                radius = baseRadius * 1.05f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 1.2.dp.toPx()
                )
            )
        }
    }
}

/**
 * Draws a smoothly undulating organic blob using multi-point cubic sine deformation.
 */
private fun DrawScope.drawFluidMorphBlob(
    center: Offset,
    radius: Float,
    phase: Float,
    brush: Brush
) {
    val points = 8
    val path = Path()
    val angleStep = (2 * PI / points).toFloat()

    val pathPoints = mutableListOf<Offset>()
    for (i in 0 until points) {
        val angle = i * angleStep
        // Modulate radius with harmonic wave
        val wave = sin(phase + i * 1.3f) * 0.08f + cos(phase * 0.7f + i * 2.1f) * 0.04f
        val r = radius * (1f + wave)
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        pathPoints.add(Offset(x, y))
    }

    // Connect points with smooth cubic beziers
    path.moveTo(pathPoints[0].x, pathPoints[0].y)
    for (i in 0 until points) {
        val p1 = pathPoints[i]
        val p2 = pathPoints[(i + 1) % points]
        val midX = (p1.x + p2.x) / 2f
        val midY = (p1.y + p2.y) / 2f
        path.quadraticBezierTo(p1.x, p1.y, midX, midY)
    }
    path.close()

    drawPath(path = path, brush = brush)
}
