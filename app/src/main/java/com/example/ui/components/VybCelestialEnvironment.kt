package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.temporal.VybTimeOfDay
import kotlin.math.*

/**
 * Procedural star definition for the night and twilight sky.
 */
private data class CelestialStar(
    val xRatio: Float,
    val yRatio: Float,
    val radius: Float,
    val baseAlpha: Float,
    val twinklePeriodMs: Int,
    val phaseOffset: Float,
    val hasCrossFlare: Boolean = false
)

/**
 * Fixed celestial stars for stable, persistent constellation aesthetics.
 */
private val FixedStars: List<CelestialStar> = listOf(
    CelestialStar(0.12f, 0.08f, 1.8f, 0.85f, 2900, 0.1f, true),
    CelestialStar(0.28f, 0.14f, 1.2f, 0.70f, 3400, 1.2f),
    CelestialStar(0.82f, 0.09f, 2.0f, 0.95f, 3100, 2.4f, true),
    CelestialStar(0.70f, 0.18f, 1.4f, 0.75f, 2600, 0.7f),
    CelestialStar(0.48f, 0.06f, 1.1f, 0.65f, 4100, 1.8f),
    CelestialStar(0.91f, 0.22f, 1.5f, 0.80f, 3300, 3.1f),
    CelestialStar(0.08f, 0.25f, 1.3f, 0.65f, 3700, 2.0f),
    CelestialStar(0.35f, 0.28f, 1.0f, 0.55f, 2800, 0.5f),
    CelestialStar(0.62f, 0.26f, 1.6f, 0.75f, 3900, 1.9f),
    CelestialStar(0.85f, 0.35f, 1.1f, 0.60f, 3000, 2.7f),
    CelestialStar(0.19f, 0.38f, 1.4f, 0.70f, 3500, 1.1f),
    CelestialStar(0.53f, 0.36f, 1.0f, 0.50f, 4200, 0.3f),
    CelestialStar(0.76f, 0.42f, 1.3f, 0.65f, 3100, 2.9f),
    CelestialStar(0.24f, 0.48f, 1.2f, 0.55f, 3600, 1.7f),
    CelestialStar(0.92f, 0.50f, 1.0f, 0.50f, 4000, 0.8f),
    CelestialStar(0.15f, 0.58f, 1.2f, 0.60f, 2900, 2.2f),
    CelestialStar(0.40f, 0.54f, 0.9f, 0.45f, 3800, 1.4f),
    CelestialStar(0.68f, 0.52f, 1.5f, 0.70f, 3200, 0.9f),
    CelestialStar(0.84f, 0.62f, 1.1f, 0.50f, 4400, 2.5f),
    CelestialStar(0.31f, 0.66f, 1.0f, 0.45f, 3700, 1.6f),
    CelestialStar(0.58f, 0.68f, 1.2f, 0.55f, 3300, 0.4f),
    CelestialStar(0.09f, 0.72f, 1.1f, 0.50f, 3500, 2.8f),
    CelestialStar(0.88f, 0.75f, 1.3f, 0.60f, 3900, 1.3f),
    CelestialStar(0.22f, 0.80f, 1.0f, 0.40f, 4100, 0.6f),
    CelestialStar(0.72f, 0.82f, 1.4f, 0.65f, 3000, 2.1f),
    CelestialStar(0.45f, 0.85f, 0.9f, 0.45f, 3600, 3.3f)
)

/**
 * Atmospheric background that transitions smoothly across time of day.
 * Renders high-end dark glass gradients and real twinkling night stars.
 */
@Composable
fun VybAtmosphereBackground(
    timeOfDay: VybTimeOfDay,
    modifier: Modifier = Modifier
) {
    val transitionSpec = tween<Color>(durationMillis = 1400, easing = FastOutSlowInEasing)

    // Palette stops for the 5 celestial time periods
    val (targetColorTop, targetColorMid, targetColorLow, targetColorBottom) = when (timeOfDay) {
        VybTimeOfDay.DAWN -> Tuple4(
            Color(0xFF090D1E),
            Color(0xFF191228),
            Color(0xFF2E172B),
            Color(0xFF070B14)
        )
        VybTimeOfDay.MORNING -> Tuple4(
            Color(0xFF071426),
            Color(0xFF0C2038),
            Color(0xFF102844),
            Color(0xFF070B14)
        )
        VybTimeOfDay.DAY -> Tuple4(
            Color(0xFF071830),
            Color(0xFF0B2446),
            Color(0xFF0F2D52),
            Color(0xFF070B14)
        )
        VybTimeOfDay.EVENING -> Tuple4(
            Color(0xFF0B0D1E),
            Color(0xFF1D142E),
            Color(0xFF33162C),
            Color(0xFF070B14)
        )
        VybTimeOfDay.NIGHT -> Tuple4(
            Color(0xFF030611),
            Color(0xFF060B1C),
            Color(0xFF0A122C),
            Color(0xFF050814)
        )
    }

    val animatedColorTop by animateColorAsState(targetColorTop, transitionSpec, label = "bgTop")
    val animatedColorMid by animateColorAsState(targetColorMid, transitionSpec, label = "bgMid")
    val animatedColorLow by animateColorAsState(targetColorLow, transitionSpec, label = "bgLow")
    val animatedColorBottom by animateColorAsState(targetColorBottom, transitionSpec, label = "bgBottom")

    // Stars opacity animated smoothly between day and night
    val starsTargetAlpha = timeOfDay.starsIntensity
    val animatedStarsAlpha by animateFloatAsState(
        targetValue = starsTargetAlpha,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "starsAlpha"
    )

    // Infinite transition for star twinkling and gentle atmospheric haze drift
    val infiniteTransition = rememberInfiniteTransition(label = "AtmosphereEffects")
    val twinkleTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinkleTime"
    )

    val hazeOffset by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hazeOffset"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Draw smooth 4-stop vertical atmospheric gradient
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to animatedColorTop,
                0.35f to animatedColorMid,
                0.70f to animatedColorLow,
                1.0f to animatedColorBottom
            )
        )

        // 2. Draw soft ambient atmospheric haze
        val hazeCenter = Offset(width * 0.5f + hazeOffset, height * 0.25f)
        val hazeRadius = width * 0.75f
        val hazeColor = when (timeOfDay) {
            VybTimeOfDay.DAWN -> Color(0xFFF472B6).copy(alpha = 0.08f)
            VybTimeOfDay.MORNING -> Color(0xFF38BDF8).copy(alpha = 0.09f)
            VybTimeOfDay.DAY -> Color(0xFF00E5FF).copy(alpha = 0.08f)
            VybTimeOfDay.EVENING -> Color(0xFFFB923C).copy(alpha = 0.10f)
            VybTimeOfDay.NIGHT -> Color(0xFF818CF8).copy(alpha = 0.07f)
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(hazeColor, Color.Transparent),
                center = hazeCenter,
                radius = hazeRadius
            ),
            center = hazeCenter,
            radius = hazeRadius
        )

        // 3. Draw Stars if night / twilight
        if (animatedStarsAlpha > 0.01f) {
            for (star in FixedStars) {
                val starX = star.xRatio * width
                val starY = star.yRatio * height
                val starCenter = Offset(starX, starY)

                val localPhase = twinkleTime + star.phaseOffset
                val twinkleFactor = 0.65f + 0.35f * sin(localPhase)
                val finalAlpha = (star.baseAlpha * twinkleFactor * animatedStarsAlpha).coerceIn(0f, 1f)

                if (finalAlpha > 0.02f) {
                    drawCircle(
                        color = Color.White.copy(alpha = finalAlpha),
                        radius = star.radius,
                        center = starCenter
                    )

                    // Subtle cross flare on primary prominent stars
                    if (star.hasCrossFlare && finalAlpha > 0.4f) {
                        val flareLength = star.radius * 3.5f
                        val flareAlpha = finalAlpha * 0.45f
                        drawLine(
                            color = Color(0xFFE0F2FE).copy(alpha = flareAlpha),
                            start = Offset(starX - flareLength, starY),
                            end = Offset(starX + flareLength, starY),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0xFFE0F2FE).copy(alpha = flareAlpha),
                            start = Offset(starX, starY - flareLength),
                            end = Offset(starX, starY + flareLength),
                            strokeWidth = 1f
                        )
                    }
                }
            }
        }
    }
}

/**
 * Living Celestial Entity (Sun / Moon) positioned behind and framing the VYB Intelligence Core.
 * Recreates a cinematic, futuristic celestial presence with translucent light rays, coronal glow,
 * organic breathing motion, and smooth day <-> night transitions.
 */
@Composable
fun VybCelestialBody(
    timeOfDay: VybTimeOfDay,
    visualState: VybVisualState = VybVisualState.IDLE,
    size: Dp = 290.dp,
    modifier: Modifier = Modifier
) {
    // Smooth transition weights between sun and moon
    val animatedSunAlpha by animateFloatAsState(
        targetValue = timeOfDay.solarIntensity,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "celestialSunAlpha"
    )
    val animatedMoonAlpha by animateFloatAsState(
        targetValue = timeOfDay.lunarIntensity,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "celestialMoonAlpha"
    )
    val animatedWarmth by animateFloatAsState(
        targetValue = timeOfDay.warmthFactor,
        animationSpec = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
        label = "celestialWarmth"
    )

    // Continuous celestial kinetics
    val infiniteTransition = rememberInfiniteTransition(label = "CelestialLoop")

    // Slow solar ray rotation (42 seconds per revolution)
    val solarRayRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(42000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "solarRayRotation"
    )

    // Gentle solar coronal breathing (7.5 second breathing cycle)
    val solarBreathScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "solarBreathScale"
    )

    // Subtle moon orbital drift / phase shimmer (12 seconds)
    val moonGlowPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "moonGlowPulse"
    )

    // Floating starlight / solar dust particles
    val particlePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhase"
    )

    Canvas(
        modifier = modifier
            .size(size)
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxRadius = min(this.size.width, this.size.height) / 2f

        // ==============================================================
        // 1. SUN ENVIRONMENT (DAWN / MORNING / DAY / EVENING TRANSITION)
        // ==============================================================
        if (animatedSunAlpha > 0.01f) {
            val sunRadius = maxRadius * 0.88f * solarBreathScale

            // A. Coronal Radial Glow Palette based on warmthFactor
            val sunCenterColor = when {
                animatedWarmth > 0.7f -> Color(0xFFFEF08A) // warm golden sunset
                animatedWarmth > 0.4f -> Color(0xFFFFFBEB) // morning crisp white-gold
                else -> Color(0xFFF0FDF4) // bright midday pure solar
            }.copy(alpha = 0.70f * animatedSunAlpha)

            val sunCoronaInner = when {
                animatedWarmth > 0.7f -> Color(0xFFF97316) // sunset amber-orange
                animatedWarmth > 0.4f -> Color(0xFFFBBF24) // morning bright gold
                else -> Color(0xFF38BDF8) // day subtle cyan-gold corona
            }.copy(alpha = 0.40f * animatedSunAlpha)

            val sunCoronaOuter = when {
                animatedWarmth > 0.7f -> Color(0xFFE11D48) // evening twilight crimson
                else -> Color(0xFF0284C7) // sky sapphire boundary
            }.copy(alpha = 0.0f)

            // Draw multi-layered soft coronal aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(sunCenterColor, sunCoronaInner, sunCoronaOuter),
                    center = center,
                    radius = sunRadius * 1.35f
                ),
                center = center,
                radius = sunRadius * 1.35f
            )

            // B. Translucent Radiant Light Rays (8 elegant light beams radiating outward)
            rotate(degrees = solarRayRotation, pivot = center) {
                val rayCount = 10
                val angleStep = 360f / rayCount
                for (i in 0 until rayCount) {
                    val angleDeg = i * angleStep
                    val angleRad = (angleDeg * PI / 180f).toFloat()

                    val rayLength = sunRadius * 1.45f
                    val rayWidth = 24.dp.toPx()

                    // Rotate drawScope to ray angle
                    rotate(degrees = angleDeg, pivot = center) {
                        val rayBrush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.3f to sunCoronaInner.copy(alpha = 0.22f * animatedSunAlpha),
                            0.7f to sunCoronaInner.copy(alpha = 0.12f * animatedSunAlpha),
                            1.0f to Color.Transparent,
                            startY = center.y,
                            endY = center.y + rayLength
                        )

                        val path = Path().apply {
                            moveTo(center.x - rayWidth * 0.2f, center.y)
                            lineTo(center.x + rayWidth * 0.2f, center.y)
                            lineTo(center.x + rayWidth * 0.6f, center.y + rayLength)
                            lineTo(center.x - rayWidth * 0.6f, center.y + rayLength)
                            close()
                        }
                        drawPath(path, brush = rayBrush)
                    }
                }
            }

            // C. Inner Sun Sphere - Soft feathered disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        sunCenterColor,
                        sunCoronaInner.copy(alpha = 0.35f * animatedSunAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = sunRadius * 0.60f
                ),
                center = center,
                radius = sunRadius * 0.60f
            )

            // D. Subtle Floating Solar Particles / Dust Motes
            for (p in 0 until 8) {
                val pAngle = p * (PI / 4.0).toFloat() + particlePhase * 0.4f
                val pDist = sunRadius * (0.6f + 0.35f * sin(particlePhase + p))
                val px = center.x + cos(pAngle) * pDist
                val py = center.y + sin(pAngle) * pDist
                val pAlpha = (0.35f + 0.25f * sin(particlePhase * 1.5f + p)) * animatedSunAlpha

                drawCircle(
                    color = sunCenterColor.copy(alpha = pAlpha.coerceIn(0f, 1f)),
                    radius = 2.0.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }

        // ==============================================================
        // 2. MOON ENVIRONMENT (NIGHT / EVENING & DAWN TRANSITION)
        // ==============================================================
        if (animatedMoonAlpha > 0.01f) {
            val moonRadius = maxRadius * 0.44f * moonGlowPulse
            // Position moon slightly elevated and offset in the upper-right quadrant of the core
            val moonCenter = Offset(center.x + maxRadius * 0.28f, center.y - maxRadius * 0.26f)

            // A. Outer Ethereal Lunar Aura
            val moonAuraColor = Color(0xFFE0F2FE).copy(alpha = 0.38f * animatedMoonAlpha)
            val moonAuraMid = Color(0xFF38BDF8).copy(alpha = 0.16f * animatedMoonAlpha)
            val moonAuraEdge = Color(0xFF0369A1).copy(alpha = 0.0f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(moonAuraColor, moonAuraMid, moonAuraEdge),
                    center = moonCenter,
                    radius = moonRadius * 2.8f
                ),
                center = moonCenter,
                radius = moonRadius * 2.8f
            )

            // B. Cinematic Moon Sphere Base
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC).copy(alpha = 0.85f * animatedMoonAlpha),
                        Color(0xFFCBD5E1).copy(alpha = 0.70f * animatedMoonAlpha),
                        Color(0xFF64748B).copy(alpha = 0.50f * animatedMoonAlpha)
                    ),
                    center = Offset(moonCenter.x - moonRadius * 0.25f, moonCenter.y - moonRadius * 0.25f),
                    radius = moonRadius
                ),
                center = moonCenter,
                radius = moonRadius
            )

            // C. Realistic Lunar Craters / Maria (subtle soft maria textures)
            val craterColor = Color(0xFF475569).copy(alpha = 0.22f * animatedMoonAlpha)
            drawCircle(craterColor, radius = moonRadius * 0.22f, center = Offset(moonCenter.x - moonRadius * 0.25f, moonCenter.y - moonRadius * 0.15f))
            drawCircle(craterColor, radius = moonRadius * 0.16f, center = Offset(moonCenter.x + moonRadius * 0.20f, moonCenter.y - moonRadius * 0.28f))
            drawCircle(craterColor, radius = moonRadius * 0.28f, center = Offset(moonCenter.x + moonRadius * 0.15f, moonCenter.y + moonRadius * 0.20f))
            drawCircle(craterColor, radius = moonRadius * 0.12f, center = Offset(moonCenter.x - moonRadius * 0.32f, moonCenter.y + moonRadius * 0.25f))

            // D. Soft Crescent / Gibbous Shadow Shading for high-end depth
            val shadowColor = Color(0xFF070B14).copy(alpha = 0.65f * animatedMoonAlpha)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, shadowColor),
                    center = Offset(moonCenter.x - moonRadius * 0.4f, moonCenter.y - moonRadius * 0.3f),
                    radius = moonRadius * 1.1f
                ),
                center = moonCenter,
                radius = moonRadius
            )

            // E. Fine Lunar Rim Light (soft specular crescent highlight)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.80f * animatedMoonAlpha),
                        Color.Transparent
                    ),
                    center = Offset(moonCenter.x + moonRadius * 0.35f, moonCenter.y - moonRadius * 0.35f),
                    radius = moonRadius * 0.6f
                ),
                center = Offset(moonCenter.x + moonRadius * 0.35f, moonCenter.y - moonRadius * 0.35f),
                radius = moonRadius * 0.6f
            )

            // F. Subtle Lunar Haze Band
            val hazeWidth = moonRadius * 3.2f
            val hazeHeight = moonRadius * 0.6f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.12f * animatedMoonAlpha),
                        Color.Transparent
                    ),
                    center = moonCenter,
                    radius = hazeWidth / 2f
                ),
                topLeft = Offset(moonCenter.x - hazeWidth / 2f, moonCenter.y - hazeHeight / 2f),
                size = Size(hazeWidth, hazeHeight)
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
