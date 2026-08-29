package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun VybIntentCard(
    payload: VybIntentPayload,
    modifier: Modifier = Modifier,
    onFollowUpClick: (String) -> Unit = {},
    onAddShoppingItem: ((String, String, String, Double) -> Unit)? = null,
    onAddTaskItem: ((String, String, String, String) -> Unit)? = null
) {
    var showDebugContext by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when {
            payload.briefingPlan != null -> DailyBriefingCard(payload.title, payload.briefingPlan)
            payload.stylePlan != null -> StyleCard(payload.title, payload.stylePlan)
            payload.decisionPlan != null -> DecisionCard(payload.title, payload.decisionPlan)
            payload.projectPlan != null -> ProjectCard(payload.title, payload.projectPlan)
            payload.workCommunication != null -> WorkCommunicationCard(payload.title, payload.workCommunication)
            payload.ridePlan != null -> RideBookingCard(payload.title, payload.ridePlan)
            payload.dietPlan != null -> DietCard(payload.title, payload.dietPlan)
            payload.shoppingList != null -> ShoppingCard(payload.title, payload.shoppingList)
            payload.travelPlan != null -> TravelCard(payload.title, payload.travelPlan)
            payload.taskList != null -> TaskCard(payload.title, payload.taskList)
            payload.financePlan != null -> FinanceCard(payload.title, payload.financePlan)
            payload.emailDraft != null -> EmailDraftCard(payload.title, payload.emailDraft)
            else -> GeneralIntentCard(payload.title, payload.summary)
        }

        // Optional debug context drawer
        payload.diagnostics?.let { diag ->
            if (showDebugContext) {
                RealDiagnosticsCard(diag)
            }
        }

        // Suggested Follow-Up Prompts
        if (payload.suggestedFollowUps.isNotEmpty()) {
            Text(
                text = "SUGGESTED NEXT STEPS",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                payload.suggestedFollowUps.take(2).forEach { followUp ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Slate850,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onFollowUpClick(followUp) }
                            .testTag("follow_up_chip_${followUp.take(10)}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SubdirectoryArrowRight,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = followUp,
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate300,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

// 1. Diet Plan Card
@Composable
fun DietCard(title: String, diet: DietPlanPayload) {
    VybGlowCard(
        borderColor = EmeraldNeon.copy(alpha = 0.5f),
        glowColor = EmeraldGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EmeraldNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.ifBlank { "Nutrition & Diet Plan" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${diet.targetCalories} kcal",
                        color = EmeraldNeon,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Macros Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroPill("Protein", "${diet.proteinGrams}g", CyanNeon, Modifier.weight(1f))
                MacroPill("Carbs", "${diet.carbsGrams}g", AmberNeon, Modifier.weight(1f))
                MacroPill("Fats", "${diet.fatGrams}g", RoseNeon, Modifier.weight(1f))
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            // Meals List
            Text(
                text = "DAILY MEALS",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            diet.meals.forEach { meal ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate850,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${meal.mealType}: ${meal.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate100,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${meal.calories} kcal • ${meal.protein}g protein",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldNeon
                            )
                        }
                        if (meal.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = meal.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                        if (meal.ingredients.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                meal.ingredients.take(4).forEach { ing ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Slate800
                                    ) {
                                        Text(
                                            text = ing,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate300,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (diet.tips.isNotEmpty()) {
                Text(
                    text = "💡 " + diet.tips.first(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate300
                )
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Slate850,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Slate400)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

// 2. Shopping List Card
@Composable
fun ShoppingCard(title: String, shopping: ShoppingListPayload) {
    var itemsState by remember(shopping) { mutableStateOf(shopping.items) }

    VybGlowCard(
        borderColor = CyanNeon.copy(alpha = 0.5f),
        glowColor = CyanGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.ifBlank { "Grocery & Shopping" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (shopping.estimatedTotal > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate800,
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "~$${String.format("%.2f", shopping.estimatedTotal)}",
                            color = CyanNeon,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsState.forEachIndexed { index, item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (item.isChecked) Slate900 else Slate850,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                itemsState = itemsState.toMutableList().also {
                                    it[index] = item.copy(isChecked = !item.isChecked)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { checked ->
                                        itemsState = itemsState.toMutableList().also {
                                            it[index] = item.copy(isChecked = checked)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = CyanNeon,
                                        uncheckedColor = Slate600,
                                        checkmarkColor = Slate950
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (item.isChecked) Slate600 else Slate100,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400
                                        )
                                        if (item.quantity.isNotBlank()) {
                                            Text(
                                                text = "• ${item.quantity}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate400
                                            )
                                        }
                                    }
                                }
                            }
                            if (item.estimatedCost > 0) {
                                Text(
                                    text = "$${String.format("%.2f", item.estimatedCost)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (item.isChecked) Slate600 else CyanNeon,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. Travel Itinerary Card
@Composable
fun TravelCard(title: String, travel: TravelPlanPayload) {
    var selectedDay by remember { mutableIntStateOf(1) }

    VybGlowCard(
        borderColor = VioletNeon.copy(alpha = 0.5f),
        glowColor = VioletGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(VioletNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flight,
                            contentDescription = null,
                            tint = VioletNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = travel.destination.ifBlank { title },
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VioletNeon.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${travel.durationDays} Days",
                        color = VioletNeon,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Day Selector Tabs
            if (travel.days.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    travel.days.forEach { day ->
                        val isSelected = day.dayNumber == selectedDay
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) VioletNeon else Slate850,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) VioletNeon else CardBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDay = day.dayNumber }
                        ) {
                            Text(
                                text = "Day ${day.dayNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Slate950 else Slate300,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                // Active Day Timeline
                val currentDay = travel.days.find { it.dayNumber == selectedDay } ?: travel.days.first()
                Text(
                    text = currentDay.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyanNeon,
                    fontWeight = FontWeight.SemiBold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    currentDay.activities.forEach { act ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Slate800,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = act.time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate300,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = act.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Slate100,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (act.location.isNotBlank()) {
                                    Text(
                                        text = "📍 ${act.location}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                                if (act.notes.isNotBlank()) {
                                    Text(
                                        text = act.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Slate300
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (travel.packingChecklist.isNotEmpty()) {
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
                Text(
                    text = "PACKING ESSENTIALS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    travel.packingChecklist.take(3).forEach { pack ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Slate850
                        ) {
                            Text(
                                text = "✓ $pack",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate300,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 4. Task Plan Card
@Composable
fun TaskCard(title: String, taskList: TaskListPayload) {
    var tasksState by remember(taskList) { mutableStateOf(taskList.tasks) }

    VybGlowCard(
        borderColor = AmberNeon.copy(alpha = 0.5f),
        glowColor = AmberGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AmberNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.ifBlank { "Action Items & Tasks" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                }
                val completedCount = tasksState.count { it.isCompleted }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$completedCount / ${tasksState.size}",
                        color = AmberNeon,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tasksState.forEachIndexed { index, task ->
                    val priorityColor = when (task.priority.uppercase()) {
                        "HIGH" -> RoseNeon
                        "LOW" -> CyanNeon
                        else -> AmberNeon
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (task.isCompleted) Slate900 else Slate850,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tasksState = tasksState.toMutableList().also {
                                    it[index] = task.copy(isCompleted = !task.isCompleted)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = task.isCompleted,
                                    onCheckedChange = { checked ->
                                        tasksState = tasksState.toMutableList().also {
                                            it[index] = task.copy(isCompleted = checked)
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = AmberNeon,
                                        uncheckedColor = Slate600,
                                        checkmarkColor = Slate950
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (task.isCompleted) Slate600 else Slate100,
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = task.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400
                                        )
                                        if (task.dueDate.isNotBlank()) {
                                            Text(
                                                text = "• ${task.dueDate}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Slate400
                                            )
                                        }
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = priorityColor.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, priorityColor)
                            ) {
                                Text(
                                    text = task.priority,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = priorityColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. Finance & Budget Card
@Composable
fun FinanceCard(title: String, finance: FinancePlanPayload) {
    VybGlowCard(
        borderColor = CyanNeon.copy(alpha = 0.5f),
        glowColor = CyanGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.ifBlank { "Budget & Finance" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "$${String.format("%.0f", finance.totalBudget)} Budget",
                        color = CyanNeon,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Categories Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                finance.categories.forEach { cat ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate300
                            )
                            Text(
                                text = "$${String.format("%.0f", cat.allocatedAmount)} (${cat.percentage}%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanNeon,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (cat.percentage / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = CyanNeon,
                            trackColor = Slate800
                        )
                    }
                }
            }

            if (finance.tips.isNotEmpty()) {
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
                Text(
                    text = "💡 " + finance.tips.first(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate300
                )
            }
        }
    }
}

// 6. Email Draft Card
@Composable
fun EmailDraftCard(title: String, email: EmailDraftPayload) {
    val context = LocalContext.current

    VybGlowCard(
        borderColor = VioletNeon.copy(alpha = 0.5f),
        glowColor = VioletGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(VioletNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mail,
                            contentDescription = null,
                            tint = VioletNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.ifBlank { "Email Draft" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800
                ) {
                    Text(
                        text = email.tone,
                        color = VioletNeon,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate850,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (email.recipient.isNotBlank()) {
                        Text(
                            text = "To: ${email.recipient}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Slate400
                        )
                    }
                    Text(
                        text = "Subject: ${email.subject}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyanNeon,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = email.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Slate100,
                        lineHeight = 20.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Email Body", email.body)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Email copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Slate800,
                        contentColor = Slate100
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, email.subject)
                            putExtra(Intent.EXTRA_TEXT, email.body)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Draft"))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VioletNeon,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

// 7. General Intent Card
@Composable
fun GeneralIntentCard(title: String, summary: String) {
    if (summary.isNotBlank()) {
        VybGlowCard(
            borderColor = CardBorderGlow,
            glowColor = CyanGlow
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = CyanNeon,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate300
                    )
                }
            }
        }
    }
}

// 8. Ride Hailing & Cab Booking Card
@Composable
fun RideBookingCard(title: String, ridePlan: RidePlanPayload) {
    val context = LocalContext.current
    var selectedOptionForAuth by remember { mutableStateOf<RideOptionPayload?>(null) }

    if (selectedOptionForAuth != null) {
        val opt = selectedOptionForAuth!!
        OwnerAuthorizationDialog(
            actionTitle = "Open ${opt.provider}",
            targetService = opt.provider,
            actionDescription = "Launch ${opt.provider} for ${ridePlan.destination} from ${ridePlan.pickupLocation}",
            recipientOrDestination = ridePlan.destination,
            estimatedCost = opt.estimatedFare,
            riskLevel = "LEVEL 0 • OPEN RIDE PROVIDER",
            consequenceSummary = "Opens the ${opt.provider} application on your mobile device to view live pricing and book your trip.",
            ownerName = "",
            onDismiss = { selectedOptionForAuth = null },
            onAuthorizeAndExecute = {
                selectedOptionForAuth = null
                val result = com.example.util.DeviceAppExecutionBridge.executeRideBooking(
                    context = context,
                    provider = opt.provider,
                    destination = ridePlan.destination,
                    pickup = ridePlan.pickupLocation
                )
                Toast.makeText(context, "${result.actionDescription} • ${result.details}", Toast.LENGTH_LONG).show()
            }
        )
    }

    VybGlowCard(
        borderColor = AmberNeon.copy(alpha = 0.6f),
        glowColor = AmberGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AmberNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalTaxi,
                            contentDescription = null,
                            tint = AmberNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title.ifBlank { "Ride Providers & Navigation" },
                        style = MaterialTheme.typography.titleMedium,
                        color = Slate100,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon.copy(alpha = 0.4f))
                ) {
                    val statusLabel = if (ridePlan.estimatedDistance != null && ridePlan.estimatedDuration != null) {
                        "${ridePlan.estimatedDistance} • ${ridePlan.estimatedDuration}"
                    } else {
                        "App Launch Ready"
                    }
                    Text(
                        text = statusLabel,
                        color = AmberNeon,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Route Points
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Slate900)
                    .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(EmeraldNeon)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pickup: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ridePlan.pickupLocation,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate100,
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RoseNeon)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Drop: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ridePlan.destination.ifBlank { "Destination" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate100,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            // Ride Options List
            Text(
                text = "AVAILABLE RIDE PROVIDERS • LAUNCH IN APP",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            ridePlan.rideOptions.forEach { option ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (option.isRecommended) Slate850 else Slate900,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (option.isRecommended) AmberNeon.copy(alpha = 0.6f) else CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (option.provider.lowercase().trim()) {
                                            "uber" -> Color.Black
                                            "ola" -> Color(0xFF00C853)
                                            "rapido" -> Color(0xFFFFC107)
                                            "yatri sathi", "yatrisathi" -> Color(0xFF0288D1)
                                            "indrive", "indriver", "in drive" -> Color(0xFF22C55E)
                                            else -> Slate700
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = null,
                                    tint = if (option.provider.lowercase().contains("rapido")) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = option.provider,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate100,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (option.isRecommended) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AmberNeon.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "RECOMMENDED",
                                                color = AmberNeon,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                val subLabel = if (option.etaMinutes != null) {
                                    "${option.vehicleType} • ${option.etaMinutes} min • ${option.capacity}"
                                } else {
                                    "${option.vehicleType} • ${option.capacity}"
                                }
                                Text(
                                    text = subLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (option.estimatedFare != null) "₹${option.estimatedFare.toInt()}" else "Live in App",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (option.estimatedFare != null) AmberNeon else Slate300,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    selectedOptionForAuth = option
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (option.isRecommended) AmberNeon else Slate700,
                                    contentColor = if (option.isRecommended) Slate950 else Slate100
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("book_ride_btn_${option.provider.lowercase().replace(" ", "_")}")
                            ) {
                                Text(
                                    text = "Open App",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 8. Daily Briefing Card (VYB TODAY)
@Composable
fun DailyBriefingCard(title: String, briefing: DailyBriefingPayload) {
    val context = LocalContext.current
    VybGlowCard(
        borderColor = CyanNeon.copy(alpha = 0.6f),
        glowColor = CyanGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "VYB TODAY • DAILY LIFE BRIEFING",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = briefing.greeting,
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = briefing.weatherSummary,
                        color = Slate200,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Transport & Morning Routine Block (if present)
            if (briefing.sonSchoolTransportTime.isNotBlank() || briefing.transportStatus.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberNeon.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Transport Routine", color = AmberNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            if (briefing.sonSchoolTransportTime.isNotBlank()) {
                                Text(briefing.sonSchoolTransportTime, color = Slate300, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (briefing.transportStatus.isNotBlank()) {
                            Text(text = briefing.transportStatus, color = Slate300, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Work Priorities & Discretionary Budget
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Work
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Work Priorities", color = BlueNeon, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        if (briefing.workPriorities.isNotEmpty()) {
                            briefing.workPriorities.take(2).forEach { item ->
                                Text("• $item", color = Slate300, fontSize = 11.sp, maxLines = 1)
                            }
                        } else {
                            Text("No pending priority tasks", color = Slate400, fontSize = 11.sp)
                        }
                    }
                }

                // Money
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Discretionary Budget", color = EmeraldNeon, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        val budgetVal = briefing.discretionaryBudgetInr
                        if (budgetVal != null) {
                            Text("₹${budgetVal.toInt()}", color = Slate100, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Active monthly buffer", color = Slate400, fontSize = 10.sp)
                        } else {
                            Text("Unconfigured", color = Slate400, fontWeight = FontWeight.Normal, fontSize = 13.sp)
                            Text("No budget set", color = Slate500, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Personal Goals & Proactive Suggestion
            if (briefing.personalGoalsSummary.isNotBlank() || briefing.proactiveVybSuggestion.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Slate850,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (briefing.personalGoalsSummary.isNotBlank()) {
                            Text(text = "Goals: ${briefing.personalGoalsSummary}", color = Slate300, fontSize = 11.sp)
                        }
                        if (briefing.proactiveVybSuggestion.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = briefing.proactiveVybSuggestion, color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 9. Style & Wardrobe Card (VYB STYLE)
@Composable
fun StyleCard(title: String, style: StylePlanPayload) {
    VybGlowCard(
        borderColor = VioletNeon.copy(alpha = 0.6f),
        glowColor = VioletGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(VioletNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = null,
                            tint = VioletNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "VYB STYLE // WARDROBE INTELLIGENCE",
                            style = MaterialTheme.typography.labelSmall,
                            color = VioletNeon,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = style.primaryOutfitName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VioletNeon.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = style.occasion,
                        color = VioletNeon,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Weather Context & Coordination
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Weather: ${style.weatherContext}", color = Slate300, fontSize = 11.sp)
                    Text(text = style.colorCoordination, color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Outfit Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutfitItemRow("Top Garment", style.topGarment, Icons.Default.DryCleaning)
                OutfitItemRow("Bottom Garment", style.bottomGarment, Icons.Default.Straighten)
                OutfitItemRow("Footwear", style.footwear, Icons.Default.Hiking)
                if (style.accessories.isNotEmpty()) {
                    OutfitItemRow("Accessories", style.accessories.joinToString(", "), Icons.Default.Watch)
                }
            }

            // Style Tip & Budget Note
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate850,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "💡 Style Tip: ${style.styleTip}", color = Slate300, fontSize = 11.sp)
                    Text(text = "✨ Wardrobe Check: ${style.budgetOrWardrobeNote}", color = EmeraldNeon, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun OutfitItemRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Slate900,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Slate400, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$label: ", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(text = value, color = Slate100, fontSize = 11.sp, fontWeight = FontWeight.Normal)
        }
    }
}

// 10. Decision & Affordability Card (HELP ME DECIDE)
@Composable
fun DecisionCard(title: String, decision: DecisionPlanPayload) {
    val verdictColor = when (decision.affordabilityVerdictColor.uppercase()) {
        "EMERALD" -> EmeraldNeon
        "ROSE", "CRIMSON" -> CrimsonNeon
        else -> AmberNeon
    }

    VybGlowCard(
        borderColor = verdictColor.copy(alpha = 0.6f),
        glowColor = verdictColor.copy(alpha = 0.2f)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(verdictColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Balance,
                            contentDescription = null,
                            tint = verdictColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "HELP ME DECIDE // DECISION ADVISOR",
                            style = MaterialTheme.typography.labelSmall,
                            color = verdictColor,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = decision.decisionSubject,
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (decision.itemPriceInr > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Slate800
                    ) {
                        Text(
                            text = "₹${decision.itemPriceInr.toInt()}",
                            color = AmberNeon,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Verdict Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = verdictColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, verdictColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (decision.affordabilityVerdictColor == "EMERALD") Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = verdictColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "VERDICT: ${decision.verdict}", color = verdictColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(text = decision.financialImpactAnalysis, color = Slate200, fontSize = 11.sp)
                    }
                }
            }

            // Discretionary and Savings status
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Post-Purchase Discretionary", color = Slate400, fontSize = 10.sp)
                        val remaining = decision.monthlyDiscretionaryRemaining
                        if (remaining != null) {
                            Text("₹${remaining.toInt()}", color = EmeraldNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Text("Unconfigured", color = Slate400, fontSize = 12.sp)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Monthly Savings Status", color = Slate400, fontSize = 10.sp)
                        Text(if (decision.savingsGoalProtected) "Protected" else "Impacted", color = if (decision.savingsGoalProtected) EmeraldNeon else CrimsonNeon, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Key Factors & Alternatives
            if (decision.keyFactors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Key Evaluation Factors:", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    decision.keyFactors.forEach { factor ->
                        Text("• $factor", color = Slate300, fontSize = 11.sp)
                    }
                }
            }

            // Final Recommendation
            if (decision.finalRecommendation.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate850,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Recommendation: ${decision.finalRecommendation}",
                        color = Slate100,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

// 11. Project Plan Card (Technology & Home Projects)
@Composable
fun ProjectCard(title: String, project: ProjectPlanPayload) {
    VybGlowCard(
        borderColor = CyanNeon.copy(alpha = 0.5f),
        glowColor = CyanGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PROJECT CONTEXT // ${project.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanNeon,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = project.projectName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate800
                ) {
                    Text(
                        text = project.currentPhase,
                        color = CyanNeon,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Context Summary
            if (project.keyContextSummary.isNotBlank()) {
                Text(text = project.keyContextSummary, color = Slate300, fontSize = 12.sp)
            }

            // Tools / Hardware / Specs
            if (project.hardwareOrTools.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Specs/Tools: ", color = Slate400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(project.hardwareOrTools.joinToString(" • "), color = Slate200, fontSize = 11.sp)
                    }
                }
            }

            // Next Action Step
            if (project.nextActionStep.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate850,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Next Immediate Step:", color = CyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(project.nextActionStep, color = Slate100, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// 12. Work & Supplier Communication Card
@Composable
fun WorkCommunicationCard(title: String, comm: WorkCommunicationPayload) {
    val context = LocalContext.current
    var showAuthDialog by remember { mutableStateOf(false) }

    if (showAuthDialog) {
        OwnerAuthorizationDialog(
            actionTitle = "Dispatch ${comm.channel} Message",
            targetService = comm.channel,
            actionDescription = "Send drafted message to ${comm.recipientNameOrRole} regarding \"${comm.draftSubject}\"",
            recipientOrDestination = comm.recipientNameOrRole,
            riskLevel = "LEVEL 2 • EXTERNAL COMMUNICATION",
            consequenceSummary = "Requires owner authorization before dispatching external work correspondence.",
            ownerName = "Tanmoy",
            onDismiss = { showAuthDialog = false },
            onAuthorizeAndExecute = {
                showAuthDialog = false
                if (comm.channel.contains("WhatsApp", ignoreCase = true)) {
                    val res = com.example.util.DeviceAppExecutionBridge.executeWhatsAppMessage(
                        context = context,
                        phoneNumber = null,
                        messageText = "*${comm.draftSubject}*\n\n${comm.draftMessage}"
                    )
                    Toast.makeText(context, "${res.actionDescription} • ${res.details}", Toast.LENGTH_SHORT).show()
                } else {
                    val sendIntent: Intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_SUBJECT, comm.draftSubject)
                        putExtra(Intent.EXTRA_TEXT, comm.draftMessage)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Send via ${comm.channel}")
                    shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(shareIntent)
                    Toast.makeText(context, "Dispatched ${comm.channel} intent.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    VybGlowCard(
        borderColor = BlueNeon.copy(alpha = 0.6f),
        glowColor = BlueGlow
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BlueNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BusinessCenter,
                            contentDescription = null,
                            tint = BlueNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PROFESSIONAL COMMUNICATION // ${comm.channel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BlueNeon,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.1.sp
                        )
                        Text(
                            text = comm.recipientNameOrRole,
                            style = MaterialTheme.typography.titleMedium,
                            color = Slate100,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate800
                ) {
                    Text(
                        text = comm.tone,
                        color = Slate300,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Subject & Draft
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Subject: ${comm.draftSubject}", color = Slate100, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    HorizontalDivider(color = Slate800, thickness = 1.dp)
                    Text(text = comm.draftMessage, color = Slate200, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }

            // Safety note & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔒 Requires your confirmation before sending",
                    color = Slate400,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("VYB Draft", comm.draftMessage)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied response to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BlueNeon),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showAuthDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BlueNeon, contentColor = Slate950),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("dispatch_work_comm_btn")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Authorize & Dispatch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun RealDiagnosticsCard(diagnostics: DiagnosticsPayload) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate950,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderGlow),
        modifier = Modifier.fillMaxWidth().testTag("real_diagnostics_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(CyanNeon.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = CyanNeon,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VYB ORCHESTRATOR DIAGNOSTICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanNeon,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    )
                }

                val statusColor = when (diagnostics.actionState) {
                    "WAITING_FOR_CONFIRMATION" -> AmberNeon
                    "FAILED" -> RoseNeon
                    "WAITING_FOR_PERMISSION" -> BlueNeon
                    else -> EmeraldNeon
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = diagnostics.actionState,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate800, thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Intent:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontSize = 10.sp
                )
                Text(
                    text = diagnostics.intent,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate200,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            if (diagnostics.contextSummary.isNotEmpty()) {
                Text(
                    text = "Context Retained:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontSize = 10.sp
                )
                diagnostics.contextSummary.take(2).forEach { ctx ->
                    Text(
                        text = "• $ctx",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate300,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }

            if (diagnostics.toolSelected != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Selected Tool:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontSize = 10.sp
                    )
                    Text(
                        text = diagnostics.toolSelected,
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            if (diagnostics.nextAction.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Next Action:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        fontSize = 10.sp
                    )
                    Text(
                        text = diagnostics.nextAction,
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberNeon,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

