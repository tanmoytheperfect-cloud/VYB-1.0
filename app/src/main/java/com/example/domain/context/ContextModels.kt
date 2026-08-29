package com.example.domain.context

import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.TaskItemEntity
import com.example.domain.capability.CapabilityState
import java.text.SimpleDateFormat
import java.util.*

data class FamilySonContext(
    val name: String = "",
    val age: Int = 0,
    val schoolName: String = "",
    val schoolLocation: String = "",
    val morningDropoffTime: String = "",
    val morningPickupTime: String = "",
    val transportPreference: String = ""
)

data class FinanceContext(
    val monthlyIncomeInr: Double = 0.0,
    val fixedRentCommitmentsInr: Double = 0.0,
    val familyExpensesInr: Double = 0.0,
    val transportBudgetInr: Double = 0.0,
    val foodBudgetInr: Double = 0.0,
    val monthlySavingsTargetInr: Double = 0.0,
    val activeDiscretionaryBufferInr: Double = 0.0,
    val recentLoggedSpendingInr: Double = 0.0
)

data class FitnessContext(
    val targetWeightKg: Double = 0.0,
    val currentWeightKg: Double = 0.0,
    val dailyProteinTargetGrams: Int = 0,
    val preferredFoods: List<String> = emptyList(),
    val weeklyGroceryBudgetEstimateInr: Double = 0.0
)

data class ActiveProjectContext(
    val name: String,
    val phase: String,
    val currentMilestone: String,
    val nextStep: String
)

data class VybContextSnapshot(
    val ownerName: String = "",
    val ownerEmail: String = "",
    val cityLocation: String = "",
    val currentFormattedTime: String = SimpleDateFormat("EEEE, h:mm a", Locale.getDefault()).format(Date()),
    val isSchoolDay: Boolean = false,
    val sonContext: FamilySonContext = FamilySonContext(),
    val financeContext: FinanceContext = FinanceContext(),
    val fitnessContext: FitnessContext = FitnessContext(),
    val activeProjects: List<ActiveProjectContext> = emptyList(),
    val storedPreferences: List<String> = emptyList(),
    val storedMemories: List<MemoryItemEntity> = emptyList(),
    val activeTasks: List<TaskItemEntity> = emptyList(),
    val checkedCapabilities: List<CapabilityState> = emptyList()
)
