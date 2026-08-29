package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.ui.components.AddAccountDialog
import com.example.ui.components.OwnerVaultSection
import com.example.ui.components.VybGlowCard
import com.example.ui.components.VybTopBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.VybViewModel

@Composable
fun HubScreen(
    viewModel: VybViewModel
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Goals", "Routines", "Finance", "Tasks", "Memory")

    val goals by viewModel.allGoals.collectAsStateWithLifecycle()
    val projects by viewModel.allProjects.collectAsStateWithLifecycle()
    val routines by viewModel.allRoutines.collectAsStateWithLifecycle()
    val memories by viewModel.allMemories.collectAsStateWithLifecycle()
    val familyMembers by viewModel.allFamilyMembers.collectAsStateWithLifecycle()
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.allShoppingItems.collectAsStateWithLifecycle()
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpenseAmount.collectAsStateWithLifecycle()
    val monthlyIncome by viewModel.monthlyIncome.collectAsStateWithLifecycle()
    val monthlySavingsTarget by viewModel.monthlySavingsTarget.collectAsStateWithLifecycle()
    val ownerAccounts by viewModel.allOwnerAccounts.collectAsStateWithLifecycle()
    val currency by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddRoutineDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddShoppingDialog by remember { mutableStateOf(false) }
    var showAddMemoryDialog by remember { mutableStateOf(false) }
    var showAddFamilyDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VybTopBar(
                title = "VYB Life Operating Hub",
                subtitle = "Goals, automated routines, wealth & context graph"
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showAddGoalDialog = true
                        1 -> showAddRoutineDialog = true
                        2 -> showAddExpenseDialog = true
                        3 -> showAddTaskDialog = true
                        4 -> showAddMemoryDialog = true
                    }
                },
                containerColor = CyanNeon,
                contentColor = Slate950,
                modifier = Modifier.testTag("hub_fab_add")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        },
        containerColor = Slate950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Tabs
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEachIndexed { index, tabName ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) CyanNeon.copy(alpha = 0.2f) else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) CyanNeon else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabName,
                                color = if (isSelected) CyanNeon else Slate400,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tab Content
            when (selectedTab) {
                0 -> GoalsTab(
                    goals = goals,
                    projects = projects,
                    onUpdateProgress = { id, prog, comp -> viewModel.updateGoalProgress(id, prog, comp) },
                    onDeleteGoal = { viewModel.deleteGoal(it) },
                    onAddGoal = { showAddGoalDialog = true }
                )
                1 -> RoutinesTab(
                    routines = routines,
                    onToggleRoutine = { id, enabled -> viewModel.toggleRoutine(id, enabled) },
                    onDeleteRoutine = { viewModel.deleteRoutine(it) },
                    onAddRoutine = { showAddRoutineDialog = true }
                )
                2 -> FinanceTab(
                    expenses = expenses,
                    totalExpense = totalExpense,
                    income = monthlyIncome,
                    savingsTarget = monthlySavingsTarget,
                    currency = currency,
                    onAddExpense = { showAddExpenseDialog = true },
                    onDeleteExpense = { viewModel.deleteExpense(it) },
                    onUpdateBudget = { inc, sav -> viewModel.updateMonthlyBudget(inc, sav) }
                )
                3 -> TasksAndListsTab(
                    tasks = tasks,
                    shoppingItems = shoppingItems,
                    currency = currency,
                    onToggleTask = { id, comp -> viewModel.toggleTask(id, comp) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onToggleShopping = { id, chk -> viewModel.toggleShoppingItem(id, chk) },
                    onDeleteShopping = { viewModel.deleteShoppingItem(it) },
                    onAddTask = { showAddTaskDialog = true },
                    onAddShopping = { showAddShoppingDialog = true }
                )
                4 -> MemoryAndGraphTab(
                    memories = memories,
                    familyMembers = familyMembers,
                    accounts = ownerAccounts,
                    onDeleteMemory = { viewModel.deleteMemory(it) },
                    onToggleAccountAi = { id, allowed -> viewModel.toggleAccountAiMemory(id, allowed) },
                    onDeleteFamily = { viewModel.deleteFamilyMember(it) },
                    onAddMemory = { showAddMemoryDialog = true },
                    onAddFamily = { showAddFamilyDialog = true },
                    onAddAccount = { showAddAccountDialog = true }
                )
            }
        }
    }

    // Dialogs
    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, cat, target, current, deadline ->
                viewModel.addGoal(title, cat, target, current, deadline)
                showAddGoalDialog = false
            }
        )
    }

    if (showAddRoutineDialog) {
        AddRoutineDialog(
            onDismiss = { showAddRoutineDialog = false },
            onConfirm = { title, time, days, desc, prompt, cost, cat ->
                viewModel.addRoutine(title, time, days, desc, prompt, cost, cat)
                showAddRoutineDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            currency = currency,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { title, amount, category, date ->
                viewModel.addExpense(title, amount, category, date)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, priority, category, dueDate ->
                viewModel.addTask(title, priority, category, dueDate)
                showAddTaskDialog = false
            }
        )
    }

    if (showAddShoppingDialog) {
        AddShoppingDialog(
            currency = currency,
            onDismiss = { showAddShoppingDialog = false },
            onConfirm = { name, category, quantity, cost ->
                viewModel.addShoppingItem(name, category, quantity, cost)
                showAddShoppingDialog = false
            }
        )
    }

    if (showAddMemoryDialog) {
        AddMemoryDialog(
            onDismiss = { showAddMemoryDialog = false },
            onConfirm = { cat, key, value ->
                viewModel.addMemory(cat, key, value)
                showAddMemoryDialog = false
            }
        )
    }

    if (showAddFamilyDialog) {
        AddFamilyDialog(
            onDismiss = { showAddFamilyDialog = false },
            onConfirm = { name, rel, notes, routine, dates ->
                viewModel.addFamilyMember(name = name, relation = rel, notes = notes, routineSummary = routine, dates = dates)
                showAddFamilyDialog = false
            }
        )
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onAdd = { account ->
                viewModel.addOwnerAccount(
                    serviceName = account.serviceName,
                    category = account.category,
                    accountIdentifier = account.accountIdentifier,
                    passwordOrSecret = account.passwordOrSecret,
                    isAiMemoryAllowed = account.isAiMemoryAllowed,
                    iconKey = account.iconKey,
                    notes = account.notes
                )
                showAddAccountDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// 1. GOALS & PROJECTS TAB
// -------------------------------------------------------------
@Composable
fun GoalsTab(
    goals: List<GoalEntity>,
    projects: List<ProjectEntity>,
    onUpdateProgress: (Long, Int, Boolean) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onAddGoal: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACTIVE GOALS (${goals.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }

        items(goals) { goal ->
            VybGlowCard(
                borderColor = CyanNeon.copy(alpha = 0.4f),
                glowColor = CyanGlow
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = Slate100,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Category: ${goal.category} • Target: ${goal.targetValue}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                        IconButton(onClick = { onDeleteGoal(goal.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Goal", tint = Slate500, modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Progress: ${goal.progressPercent}%", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(text = "Target Deadline: ${goal.targetDeadline}", color = Slate400, fontSize = 11.sp)
                    }

                    LinearProgressIndicator(
                        progress = { goal.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyanNeon,
                        trackColor = Slate800
                    )

                    // Quick Progress Updater
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 50, 75, 100).forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (goal.progressPercent >= p) CyanNeon.copy(alpha = 0.2f) else Slate800,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (goal.progressPercent >= p) CyanNeon else Slate700),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onUpdateProgress(goal.id, p, p == 100) }
                                    .padding(vertical = 4.dp),
                            ) {
                                Text(
                                    text = "$p%",
                                    color = if (goal.progressPercent >= p) CyanNeon else Slate400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. ROUTINES TAB
// -------------------------------------------------------------
@Composable
fun RoutinesTab(
    routines: List<RoutineEntity>,
    onToggleRoutine: (Long, Boolean) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onAddRoutine: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "AUTOMATED LIFE ROUTINES (${routines.size})",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }

        items(routines) { routine ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (routine.isEnabled) AmberNeon.copy(alpha = 0.4f) else Slate800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberNeon.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Alarm, contentDescription = null, tint = AmberNeon, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = routine.title, style = MaterialTheme.typography.titleMedium, color = Slate100, fontWeight = FontWeight.Bold)
                                Text(text = "${routine.timeOfDay} • ${routine.daysOfWeek}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            }
                        }

                        Switch(
                            checked = routine.isEnabled,
                            onCheckedChange = { onToggleRoutine(routine.id, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Slate950,
                                checkedTrackColor = AmberNeon,
                                uncheckedTrackColor = Slate800
                            )
                        )
                    }

                    Text(text = routine.description, style = MaterialTheme.typography.bodySmall, color = Slate300)

                    if (routine.estimatedCostInr > 0) {
                        Text(text = "Estimated Cost: ₹${routine.estimatedCostInr.toInt()} per run", color = EmeraldNeon, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onDeleteRoutine(routine.id) }) {
                            Text("Remove", color = CrimsonNeon, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. FINANCE & BUDGET TAB
// -------------------------------------------------------------
@Composable
fun FinanceTab(
    expenses: List<ExpenseEntity>,
    totalExpense: Double,
    income: Double,
    savingsTarget: Double,
    currency: String,
    onAddExpense: () -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onUpdateBudget: (Double, Double) -> Unit
) {
    var checkCostText by remember { mutableStateOf("") }
    var affordabilityResult by remember { mutableStateOf<String?>(null) }

    val remainingSafeSpending = income - savingsTarget - totalExpense

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            VybGlowCard(
                borderColor = EmeraldNeon.copy(alpha = 0.5f),
                glowColor = EmeraldGlow
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "MONTHLY FINANCIAL OVERVIEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldNeon,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Monthly Income", color = Slate400, fontSize = 11.sp)
                            Text("$currency${income.toInt()}", color = Slate100, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column {
                            Text("Savings Target", color = Slate400, fontSize = 11.sp)
                            Text("$currency${savingsTarget.toInt()}", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column {
                            Text("Spent This Month", color = Slate400, fontSize = 11.sp)
                            Text("$currency${totalExpense.toInt()}", color = AmberNeon, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Divider(color = Slate800)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Safe Discretionary Left:", color = Slate300, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            text = "$currency${remainingSafeSpending.toInt()}",
                            color = if (remainingSafeSpending > 0) EmeraldNeon else CrimsonNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // "Can I Afford This?" Calculator
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "DECISION ENGINE: CAN I AFFORD THIS?",
                        style = MaterialTheme.typography.labelSmall,
                        color = AmberNeon,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = checkCostText,
                            onValueChange = { checkCostText = it },
                            placeholder = { Text("Item cost in ₹", color = Slate500, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Slate100,
                                unfocusedTextColor = Slate100,
                                focusedBorderColor = AmberNeon,
                                unfocusedBorderColor = Slate700
                            )
                        )
                        Button(
                            onClick = {
                                val cost = checkCostText.toDoubleOrNull() ?: 0.0
                                affordabilityResult = if (cost <= 0) {
                                    "Please enter a valid amount."
                                } else if (cost <= remainingSafeSpending) {
                                    "✅ APPROVED: ₹${cost.toInt()} fits comfortably in your discretionary buffer (₹${remainingSafeSpending.toInt()} remaining). Your ₹${savingsTarget.toInt()} savings target remains protected."
                                } else {
                                    "⚠️ WARNING: ₹${cost.toInt()} exceeds current monthly discretionary headroom (₹${remainingSafeSpending.toInt()}). It will dip into your ₹${savingsTarget.toInt()} monthly savings goal."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberNeon, contentColor = Slate950),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Evaluate", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (affordabilityResult != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Slate850,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = affordabilityResult!!,
                                color = Slate200,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "LOGGED EXPENSES (${expenses.size})",
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }

        items(expenses) { expense ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = expense.title, color = Slate100, fontWeight = FontWeight.SemiBold)
                        Text(text = "${expense.category} • ${expense.date}", color = Slate400, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "$currency${expense.amount.toInt()}", color = EmeraldNeon, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onDeleteExpense(expense.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. TASKS & LISTS TAB
// -------------------------------------------------------------
@Composable
fun TasksAndListsTab(
    tasks: List<TaskItemEntity>,
    shoppingItems: List<ShoppingItemEntity>,
    currency: String,
    onToggleTask: (Long, Boolean) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onToggleShopping: (Long, Boolean) -> Unit,
    onDeleteShopping: (Long) -> Unit,
    onAddTask: () -> Unit,
    onAddShopping: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRIORITY TASKS (${tasks.count { !it.isCompleted }} pending)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                TextButton(onClick = onAddTask) {
                    Text("+ Add Task", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(tasks) { task ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { onToggleTask(task.id, it) },
                        colors = CheckboxDefaults.colors(checkedColor = CyanNeon, checkmarkColor = Slate950)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            color = if (task.isCompleted) Slate500 else Slate100,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                        )
                        Text(
                            text = "Priority: ${task.priority} • Due: ${task.dueDate}",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { onDeleteTask(task.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHOPPING & GROCERY LIST",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                TextButton(onClick = onAddShopping) {
                    Text("+ Add Item", color = EmeraldNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(shoppingItems) { item ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = item.isChecked,
                        onCheckedChange = { onToggleShopping(item.id, it) },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldNeon, checkmarkColor = Slate950)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            color = if (item.isChecked) Slate500 else Slate100,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
                        )
                        Text(
                            text = "${item.quantity} • ${item.category}",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                    }
                    if (item.estimatedCost > 0) {
                        Text(
                            text = "$currency${item.estimatedCost.toInt()}",
                            color = EmeraldNeon,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { onDeleteShopping(item.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. MEMORY & CONTEXT GRAPH TAB
// -------------------------------------------------------------
@Composable
fun MemoryAndGraphTab(
    memories: List<MemoryItemEntity>,
    familyMembers: List<FamilyMemberEntity>,
    accounts: List<OwnerAccountEntity>,
    onDeleteMemory: (Long) -> Unit,
    onToggleAccountAi: (Long, Boolean) -> Unit,
    onDeleteFamily: (Long) -> Unit,
    onAddMemory: () -> Unit,
    onAddFamily: () -> Unit,
    onAddAccount: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Family Graph
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FAMILY & HOUSEHOLD GRAPH (${familyMembers.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = VioletNeon,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                TextButton(onClick = onAddFamily) {
                    Text("+ Add Member", color = VioletNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(familyMembers) { member ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, VioletNeon.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${member.name} (${member.relation})", color = Slate100, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onDeleteFamily(member.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                        }
                    }
                    if (member.routineSummary.isNotBlank()) {
                        Text(text = "Routine: ${member.routineSummary}", color = Slate300, fontSize = 11.sp)
                    }
                    if (member.importantDates.isNotBlank()) {
                        Text(text = "Dates: ${member.importantDates}", color = Slate400, fontSize = 11.sp)
                    }
                }
            }
        }

        // Long-Term Memory Graph
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LONG-TERM MEMORY STORE (${memories.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanNeon,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                TextButton(onClick = onAddMemory) {
                    Text("+ Add Memory", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(memories) { mem ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = mem.key, color = Slate100, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = mem.value, color = Slate300, fontSize = 12.sp)
                        Text(text = "Category: ${mem.category} • Source: ${mem.source}", color = Slate500, fontSize = 10.sp)
                    }
                    IconButton(onClick = { onDeleteMemory(mem.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Slate500, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Authorized Accounts & Integrations Vault
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONNECTED SERVICE VAULT (${accounts.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldNeon,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                TextButton(onClick = onAddAccount) {
                    Text("+ Connect", color = EmeraldNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(accounts) { account ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate900,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = account.serviceName, color = Slate100, fontWeight = FontWeight.Bold)
                        Text(text = account.accountIdentifier, color = Slate400, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (account.isAiMemoryAllowed) "AI Active" else "AI Paused",
                            color = if (account.isAiMemoryAllowed) EmeraldNeon else Slate500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = account.isAiMemoryAllowed,
                            onCheckedChange = { onToggleAccountAi(account.id, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Slate950, checkedTrackColor = EmeraldNeon, uncheckedTrackColor = Slate800)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOG HELPERS
// -------------------------------------------------------------
@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("FINANCE") }
    var targetValue by remember { mutableStateOf("") }
    var currentValue by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Life Goal", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Title") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (e.g. FINANCE, HEALTH, TRAVEL)") })
                OutlinedTextField(value = targetValue, onValueChange = { targetValue = it }, label = { Text("Target Value (e.g. ₹50,000, 70 kg)") })
                OutlinedTextField(value = currentValue, onValueChange = { currentValue = it }, label = { Text("Current Value") })
                OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Target Deadline (e.g. In 6 Months)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, category, targetValue, currentValue, deadline)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
            ) { Text("Save Goal") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Slate400) }
        },
        containerColor = Slate900
    )
}

@Composable
fun AddRoutineDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, Double, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var timeOfDay by remember { mutableStateOf("") }
    var daysOfWeek by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var actionPrompt by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf("0") }
    var category by remember { mutableStateOf("GENERAL") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Automated Routine", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Routine Title") })
                OutlinedTextField(value = timeOfDay, onValueChange = { timeOfDay = it }, label = { Text("Time of Day") })
                OutlinedTextField(value = daysOfWeek, onValueChange = { daysOfWeek = it }, label = { Text("Days (e.g. Mon-Fri)") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
                OutlinedTextField(value = actionPrompt, onValueChange = { actionPrompt = it }, label = { Text("VYB Action Prompt") })
                OutlinedTextField(value = costText, onValueChange = { costText = it }, label = { Text("Estimated Cost in ₹") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, timeOfDay, daysOfWeek, description, actionPrompt, costText.toDoubleOrNull() ?: 0.0, category)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AmberNeon, contentColor = Slate950)
            ) { Text("Save Routine") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate400) } },
        containerColor = Slate900
    )
}

@Composable
fun AddExpenseDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Expense", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title") })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount ($currency)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0) {
                        onConfirm(title, amt, category, "Today")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Slate950)
            ) { Text("Add Expense") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate400) } },
        containerColor = Slate900
    )
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("HIGH") }
    var category by remember { mutableStateOf("GENERAL") }
    var dueDate by remember { mutableStateOf("Today") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") })
                OutlinedTextField(value = priority, onValueChange = { priority = it }, label = { Text("Priority (HIGH/MEDIUM/LOW)") })
                OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title, priority, category, dueDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
            ) { Text("Save Task") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate400) } },
        containerColor = Slate900
    )
}

@Composable
fun AddShoppingDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var category by remember { mutableStateOf("Grocery") }
    var cost by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Shopping Item", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") })
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Estimated Cost ($currency)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name, category, quantity, cost.toDoubleOrNull() ?: 0.0)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Slate950)
            ) { Text("Add Item") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate400) } },
        containerColor = Slate900
    )
}

@Composable
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var category by remember { mutableStateOf("PREFERENCE") }
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Long-Term Memory", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("Fact / Key (e.g. Diet, City, Morning Routine)") })
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Value / Description") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (key.isNotBlank() && value.isNotBlank()) onConfirm(category, key, value)
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
            ) { Text("Remember Fact") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate400) } },
        containerColor = Slate900
    )
}

@Composable
fun AddFamilyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var routine by remember { mutableStateOf("") }
    var dates by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Family Member", color = Slate100, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Relationship") })
                OutlinedTextField(value = routine, onValueChange = { routine = it }, label = { Text("Routine or schedule note") })
                OutlinedTextField(value = dates, onValueChange = { dates = it }, label = { Text("Important Dates (e.g. Birthday)") })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name, relation, notes, routine, dates)
                },
                colors = ButtonDefaults.buttonColors(containerColor = VioletNeon, contentColor = Slate950)
            ) { Text("Save Member") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Slate400) } },
        containerColor = Slate900
    )
}
