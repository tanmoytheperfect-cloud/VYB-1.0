package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.TaskItemEntity
import com.example.domain.intelligence.PlanStatus
import com.example.domain.temporal.VybTimeOfDay
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Living Personal AI Operating System Home Screen.
 * Faithful native recreation of the VYB visual intelligence design.
 */
@Composable
fun HomeScreen(
    viewModel: VybViewModel,
    onNavigateToAsk: () -> Unit = {},
    onNavigateToLife: () -> Unit = {},
    onNavigateToActions: () -> Unit = {},
    onNavigateToMemory: () -> Unit = {}
) {
    // ----------------------------------------------------
    // Observable Live Data from VYB Architecture
    // ----------------------------------------------------
    val ownerName by viewModel.ownerName.collectAsStateWithLifecycle()
    val ownerTitle by viewModel.ownerTitle.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmationPrompt.collectAsStateWithLifecycle()
    val activePlans by viewModel.activeActionPlans.collectAsStateWithLifecycle()
    val allRoutines by viewModel.allRoutines.collectAsStateWithLifecycle()
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpenseAmount.collectAsStateWithLifecycle()
    val monthlyIncome by viewModel.monthlyIncome.collectAsStateWithLifecycle()
    val monthlySavingsTarget by viewModel.monthlySavingsTarget.collectAsStateWithLifecycle()
    val allFamilyMembers by viewModel.allFamilyMembers.collectAsStateWithLifecycle()
    val allIntentions by viewModel.allPersistentIntentions.collectAsStateWithLifecycle()
    val livingSignals by viewModel.livingSignals.collectAsStateWithLifecycle()
    val currentLifeState by viewModel.currentLifeState.collectAsStateWithLifecycle()
    val awarenessReport by viewModel.backgroundAwarenessReport.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.any { it }) {
            viewModel.refreshLivingSignals()
        }
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.livingEnvironmentEngine.initializeSensors()
            viewModel.refreshLivingSignals()
        }
    }

    // ----------------------------------------------------
    // Real-Time Live Clock State & Lifecycle Resume Handler
    // ----------------------------------------------------
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentTimeMillis = System.currentTimeMillis()
                viewModel.triggerUsageReconciliation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(2000)
        }
    }

    val liveCalendar = remember(currentTimeMillis) {
        Calendar.getInstance().apply { timeInMillis = currentTimeMillis }
    }
    val liveTimeStr = remember(currentTimeMillis) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(currentTimeMillis))
    }

    // Time-Aware Celestial Environment State (Real Local Time with Optional Dev/Test Override)
    val timeOverride by viewModel.timeOfDayOverride.collectAsStateWithLifecycle()
    val currentTimeOfDay = remember(currentTimeMillis, timeOverride) {
        timeOverride ?: VybTimeOfDay.fromMillis(currentTimeMillis)
    }

    // Dynamic greeting based on time of day
    val greetingWord = currentTimeOfDay.greetingPrefix

    // Formatted owner greeting matching: “Good morning, Sir Tanmoy” / “Good night, Sir Tanmoy”
    val formattedOwnerGreeting = remember(greetingWord, ownerTitle, ownerName) {
        val titlePart = if (ownerTitle.isNotBlank() && ownerTitle != "Owner") "$ownerTitle " else ""
        val namePart = ownerName.ifBlank { "Tanmoy" }
        "“$greetingWord, $titlePart$namePart”"
    }

    // ----------------------------------------------------
    // Cold-Start Arrival Animation
    // ----------------------------------------------------
    val arrivalProgress = remember { Animatable(if (viewModel.hasCompletedArrivalAnimation) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!viewModel.hasCompletedArrivalAnimation) {
            arrivalProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing)
            )
            viewModel.hasCompletedArrivalAnimation = true
        }
    }

    // ----------------------------------------------------
    // True Cognitive State of the VYB Intelligence Core
    // ----------------------------------------------------
    val currentVisualState = remember(isLoading, isSpeaking, pendingConfirmation, activePlans) {
        when {
            pendingConfirmation != null -> VybVisualState.NEEDS_ATTENTION
            isLoading -> VybVisualState.THINKING
            isSpeaking -> VybVisualState.ACTING
            activePlans.any { it.status == PlanStatus.IN_PROGRESS } -> VybVisualState.ACTING
            activePlans.any { it.status == PlanStatus.AWAITING_CONFIRMATION } -> VybVisualState.WAITING
            else -> VybVisualState.IDLE
        }
    }

    // Dynamic Time-Aware Atmospheric Background (Day / Night / Dawn / Evening)
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        VybAtmosphereBackground(
            timeOfDay = currentTimeOfDay,
            modifier = Modifier.fillMaxSize()
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)
        ) {
            // ==========================================
            // TOP HUD / STATUS BAR
            // ==========================================
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VYB",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = Slate100,
                        letterSpacing = 2.sp,
                        modifier = Modifier.testTag("home_brand_logo")
                    )

                    // Interactive Celestial Time Chip (Tapping cycles DAWN -> MORNING -> DAY -> EVENING -> NIGHT -> AUTO)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8)),
                        modifier = Modifier
                            .testTag("home_time_override_chip")
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.cycleTimeOfDayOverride() }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (currentTimeOfDay.isSunEnvironment) Icons.Default.WbSunny else Icons.Default.Nightlight,
                                contentDescription = "Celestial Indicator",
                                tint = if (currentTimeOfDay.isSunEnvironment) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = liveTimeStr,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Slate200,
                                fontSize = 12.sp,
                                modifier = Modifier.testTag("home_time_display")
                            )
                            if (timeOverride != null) {
                                Text(
                                    text = "• ${currentTimeOfDay.displayName.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanNeon,
                                    fontSize = 10.sp,
                                    modifier = Modifier.testTag("home_celestial_tag")
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = "Signal",
                            tint = Slate400,
                            modifier = Modifier.size(15.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = "Battery",
                            tint = Slate400,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // ==========================================
            // HERO SECTION: LIVING VYB INTELLIGENCE CORE
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(arrivalProgress.value),
                    contentAlignment = Alignment.Center
                ) {
                    // Celestial Sun / Moon framing and radiating behind the core
                    VybCelestialBody(
                        timeOfDay = currentTimeOfDay,
                        visualState = currentVisualState,
                        size = 280.dp,
                        modifier = Modifier.testTag("home_celestial_body")
                    )

                    // Living VYB Intelligence Core floating in front
                    VybIntelligenceCore(
                        size = 230.dp,
                        visualState = currentVisualState,
                        timeOfDay = currentTimeOfDay,
                        hasCompletedArrival = viewModel.hasCompletedArrivalAnimation,
                        onClick = {
                            if (isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakJarvisGreeting()
                            }
                        },
                        modifier = Modifier.testTag("home_living_vyb_core")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Capsule Status Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.75f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8)),
                    modifier = Modifier
                        .alpha(arrivalProgress.value)
                        .testTag("home_status_pill")
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateToAsk
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    when (currentVisualState) {
                                        VybVisualState.THINKING -> VioletNeon
                                        VybVisualState.ACTING -> CyanNeon
                                        VybVisualState.NEEDS_ATTENTION -> AmberNeon
                                        else -> Color(0xFF2DD4BF)
                                    }
                                )
                        )
                        Text(
                            text = currentVisualState.label,
                            color = Slate200,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Personal Greeting & Invitation
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.alpha(arrivalProgress.value)
                ) {
                    Text(
                        text = formattedOwnerGreeting,
                        color = Slate100,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                        modifier = Modifier.testTag("home_personal_greeting")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentTimeOfDay.contextualSummary,
                        color = Slate400,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        letterSpacing = 0.2.sp,
                        modifier = Modifier.testTag("home_celestial_summary")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // ==========================================
            // CONTEXTUAL MEANING HERO (WHAT MATTERS NOW)
            // ==========================================
            item {
                VybContextualHeroCard(
                    hero = currentLifeState.contextualHero,
                    modifier = Modifier
                        .alpha(arrivalProgress.value)
                        .padding(horizontal = 2.dp)
                )
            }

            // ==========================================
            // COMPACT LIVING SIGNALS (TIME + WEATHER + AIR QUALITY + STEPS + ACTIVITY)
            // ==========================================
            item {
                VybLivingSignalsSection(
                    signals = livingSignals,
                    timeOfDay = currentTimeOfDay,
                    onRefresh = { viewModel.refreshLivingSignals() },
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    onRequestActivityPermission = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            activityPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            viewModel.refreshLivingSignals()
                        }
                    },
                    modifier = Modifier
                        .alpha(arrivalProgress.value)
                        .padding(horizontal = 2.dp)
                )
            }

            // ==========================================
            // PRIMARY CONTEXTUAL CARDS (2x2 GRID)
            // ==========================================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(arrivalProgress.value),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ROW 1: NEXT UP & TODAY
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Card 1: Next Up
                        Box(modifier = Modifier.weight(1f)) {
                            NextUpCard(
                                nextUpItem = currentLifeState.upcomingNextUp,
                                environment = currentLifeState.canonicalEnvironment,
                                onClick = onNavigateToLife
                            )
                        }

                        // Card 2: Today
                        Box(modifier = Modifier.weight(1f)) {
                            TodayTasksCard(
                                tasks = allTasks,
                                onToggleTask = { task ->
                                    viewModel.toggleTask(task.id, !task.isCompleted)
                                },
                                onCardClick = onNavigateToLife
                            )
                        }
                    }

                    // ROW 2: MONEY & FAMILY
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Card 3: Money
                        Box(modifier = Modifier.weight(1f)) {
                            MoneyInsightCard(
                                totalExpense = totalExpense,
                                monthlyIncome = monthlyIncome,
                                savingsTarget = monthlySavingsTarget,
                                onClick = onNavigateToLife
                            )
                        }

                        // Card 4: Family
                        Box(modifier = Modifier.weight(1f)) {
                            FamilyContextCard(
                                familyMembers = allFamilyMembers,
                                onClick = onNavigateToLife
                            )
                        }
                    }
                }
            }

            // ==========================================
            // BACKGROUND AWARENESS & LEARNING STATUS (DIAGNOSTICS)
            // ==========================================
            item {
                BackgroundAwarenessSection(
                    report = awarenessReport,
                    onTriggerReconciliation = { viewModel.triggerUsageReconciliation() },
                    modifier = Modifier
                        .alpha(arrivalProgress.value)
                        .padding(horizontal = 2.dp)
                )
            }
        }
    }
}

// =========================================================================
// PREMIUM GLASSMORPHIC CARD COMPONENTS
// =========================================================================

/**
 * Next Up Card: Displays upcoming real intention/routine with commute & weather context.
 * Strictly driven by canonical CurrentLifeState (Requirement 1, 2, 4, 5).
 * Never fabricates values, routines, or temperatures.
 */
@Composable
private fun NextUpCard(
    nextUpItem: com.example.domain.cognitive.models.NextUpItem?,
    environment: com.example.domain.cognitive.models.CurrentEnvironmentState,
    onClick: () -> Unit
) {
    val titleText = nextUpItem?.title ?: "Next up"
    val subtitleText = nextUpItem?.subtitle ?: "No routines pending"
    val timeText = nextUpItem?.timeDisplay ?: "All clear"
    val weatherText = environment.temperature?.let { "${it.toInt()}°C" } ?: nextUpItem?.weatherDisplay ?: ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clickable(onClick = onClick)
            .testTag("home_card_next_up"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2E38BDF8))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x1A00E5FF),
                            Color.Transparent
                        ),
                        radius = 280f
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (nextUpItem != null) "Next up" else "Schedule",
                        color = Slate100,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (nextUpItem != null) titleText else subtitleText,
                        color = Slate200,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (nextUpItem != null) "$timeText • $subtitleText" else timeText,
                        color = Slate400,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Footer context icons (Commute, transit, real canonical temperature)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val travelIcon = when (nextUpItem?.commuteIcon) {
                            "WALK" -> Icons.AutoMirrored.Filled.DirectionsWalk
                            "TRANSIT" -> Icons.Default.DirectionsTransit
                            else -> Icons.Default.DirectionsCar
                        }
                        Icon(
                            imageVector = travelIcon,
                            contentDescription = "Travel Mode",
                            tint = Slate400,
                            modifier = Modifier.size(15.dp)
                        )
                        if (nextUpItem?.isException == true) {
                            Text(
                                text = "EXCEPTION",
                                color = AmberNeon,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = "Sensors active",
                                tint = CyanNeon.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    if (weatherText.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Weather",
                                tint = AmberNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = weatherText,
                                color = Slate300,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Today Card: Displays intelligently selected tasks with functional circular checkboxes.
 */
@Composable
private fun TodayTasksCard(
    tasks: List<TaskItemEntity>,
    onToggleTask: (TaskItemEntity) -> Unit,
    onCardClick: () -> Unit
) {
    val displayedTasks = remember(tasks) {
        if (tasks.isNotEmpty()) {
            tasks.take(3)
        } else {
            emptyList()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clickable(onClick = onCardClick)
            .testTag("home_card_today"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2E818CF8))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x188B5CF6),
                            Color.Transparent
                        ),
                        radius = 280f
                    )
                )
                .padding(14.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Today",
                    color = Slate100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = if (displayedTasks.isNotEmpty()) "${displayedTasks.size} intelligently selected tasks" else "No pending tasks",
                    color = Slate400,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (displayedTasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Nothing needs your attention right now.",
                            color = Slate400,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        displayedTasks.forEach { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleTask(task) },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = task.title,
                                    color = if (task.isCompleted) Slate500 else Slate200,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Toggle completion",
                                    tint = if (task.isCompleted) CyanNeon else Slate500,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Money Card: Displays real financial insight and monthly budget progress.
 */
@Composable
private fun MoneyInsightCard(
    totalExpense: Double,
    monthlyIncome: Double,
    savingsTarget: Double,
    onClick: () -> Unit
) {
    val calculatedBudget = remember(monthlyIncome, savingsTarget) {
        monthlyIncome - savingsTarget
    }
    val isBudgetAvailable = calculatedBudget > 0
    val spent = totalExpense
    val progress = if (isBudgetAvailable) {
        (spent / calculatedBudget).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick)
            .testTag("home_card_money"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2E2DD4BF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Money",
                        color = Slate100,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Monthly spending insight",
                        color = Slate400,
                        fontSize = 10.sp
                    )
                }

                Column {
                    if (isBudgetAvailable) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "₹${String.format(Locale.getDefault(), "%,.0f", spent)} ",
                                color = Slate100,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "/ ₹${String.format(Locale.getDefault(), "%,.0f", calculatedBudget)} budget",
                                color = Slate400,
                                fontSize = 10.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Dual-tone CyanNeon to VioletNeon gradient progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1E293B))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(CyanNeon, VioletNeon)
                                        )
                                    )
                            )
                        }
                    } else {
                        Text(
                            text = "₹${String.format(Locale.getDefault(), "%,.0f", spent)} spent",
                            color = Slate100,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Budget not set",
                            color = Slate400,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Family Card: Displays relevant family context and quick connection actions.
 */
@Composable
private fun FamilyContextCard(
    familyMembers: List<com.example.data.local.entity.FamilyMemberEntity>,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick)
            .testTag("home_card_family"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2EA78BFA))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Family",
                    color = Slate100,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (familyMembers.isNotEmpty()) {
                        val primaryMember = familyMembers.first()
                        val primaryText = if (primaryMember.relation.isNotBlank()) {
                            "${primaryMember.name} (${primaryMember.relation})"
                        } else {
                            primaryMember.name
                        }
                        Text(
                            text = primaryText,
                            color = Slate200,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitleText = if (familyMembers.size > 1) {
                            val secondary = familyMembers[1]
                            val secondaryText = if (secondary.relation.isNotBlank()) {
                                "${secondary.name} (${secondary.relation})"
                            } else {
                                secondary.name
                            }
                            if (familyMembers.size > 2) "$secondaryText +${familyMembers.size - 2} more" else secondaryText
                        } else {
                            "1 family member saved"
                        }
                        Text(
                            text = subtitleText,
                            color = Slate400,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "No family members added",
                            color = Slate200,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Open Life to manage family context.",
                            color = Slate400,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
