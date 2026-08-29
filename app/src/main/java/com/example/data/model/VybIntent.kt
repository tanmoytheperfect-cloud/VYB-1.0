package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VybIntentPayload(
    val intentType: String = "GENERAL",
    val title: String = "",
    val summary: String = "",
    val dietPlan: DietPlanPayload? = null,
    val shoppingList: ShoppingListPayload? = null,
    val travelPlan: TravelPlanPayload? = null,
    val taskList: TaskListPayload? = null,
    val financePlan: FinancePlanPayload? = null,
    val emailDraft: EmailDraftPayload? = null,
    val ridePlan: RidePlanPayload? = null,
    val stylePlan: StylePlanPayload? = null,
    val decisionPlan: DecisionPlanPayload? = null,
    val projectPlan: ProjectPlanPayload? = null,
    val briefingPlan: DailyBriefingPayload? = null,
    val workCommunication: WorkCommunicationPayload? = null,
    val diagnostics: DiagnosticsPayload? = null,
    val suggestedFollowUps: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DiagnosticsPayload(
    val intent: String = "",
    val contextSummary: List<String> = emptyList(),
    val capabilitiesChecked: List<String> = emptyList(),
    val toolSelected: String? = null,
    val actionState: String = "READY",
    val nextAction: String = ""
)

@JsonClass(generateAdapter = true)
data class DietPlanPayload(
    val goal: String = "",
    val targetCalories: Int = 2000,
    val proteinGrams: Int = 140,
    val carbsGrams: Int = 200,
    val fatGrams: Int = 65,
    val meals: List<MealItemPayload> = emptyList(),
    val tips: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MealItemPayload(
    val mealType: String = "Meal", // Breakfast, Lunch, Dinner, Snack
    val name: String = "",
    val description: String = "",
    val calories: Int = 0,
    val protein: Int = 0,
    val ingredients: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ShoppingListPayload(
    val title: String = "Shopping List",
    val estimatedTotal: Double = 0.0,
    val items: List<ShoppingItemPayload> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ShoppingItemPayload(
    val name: String = "",
    val category: String = "General", // Produce, Dairy, Protein, Pantry, Household
    val quantity: String = "1",
    val estimatedCost: Double = 0.0,
    val isChecked: Boolean = false
)

@JsonClass(generateAdapter = true)
data class TravelPlanPayload(
    val destination: String = "",
    val durationDays: Int = 3,
    val estimatedBudget: Double = 0.0,
    val days: List<TravelDayPayload> = emptyList(),
    val packingChecklist: List<String> = emptyList(),
    val highlights: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TravelDayPayload(
    val dayNumber: Int = 1,
    val title: String = "",
    val activities: List<ActivityPayload> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ActivityPayload(
    val time: String = "",
    val title: String = "",
    val location: String = "",
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class TaskListPayload(
    val title: String = "Action Items",
    val tasks: List<TaskItemPayload> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TaskItemPayload(
    val title: String = "",
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val category: String = "General",
    val dueDate: String = "",
    val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class FinancePlanPayload(
    val title: String = "Budget Overview",
    val totalBudget: Double = 0.0,
    val savingsTarget: Double = 0.0,
    val categories: List<BudgetCategoryPayload> = emptyList(),
    val tips: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BudgetCategoryPayload(
    val name: String = "",
    val allocatedAmount: Double = 0.0,
    val percentage: Int = 0
)

@JsonClass(generateAdapter = true)
data class EmailDraftPayload(
    val recipient: String = "",
    val subject: String = "",
    val body: String = "",
    val tone: String = "Professional"
)

@JsonClass(generateAdapter = true)
data class RidePlanPayload(
    val pickupLocation: String = "Current Location",
    val destination: String = "",
    val estimatedDistance: String? = null,
    val estimatedDuration: String? = null,
    val rideOptions: List<RideOptionPayload> = emptyList(),
    val directDeepLink: String? = null,
    val liveFareAvailable: Boolean = false,
    val directBookingAvailable: Boolean = false,
    val note: String = "Direct live booking API unavailable • Can open installed ride apps or navigation"
)

@JsonClass(generateAdapter = true)
data class RideOptionPayload(
    val provider: String = "Uber", // Rapido, Uber, Ola, Yatri Sathi, inDrive
    val vehicleType: String = "Open app to view options",
    val estimatedFare: Double? = null,
    val etaMinutes: Int? = null,
    val capacity: String = "Standard",
    val appPackage: String = "com.ubercab",
    val isRecommended: Boolean = false,
    val isLiveQuote: Boolean = false,
    val isInstalled: Boolean = true
)

@JsonClass(generateAdapter = true)
data class StylePlanPayload(
    val occasion: String = "Meeting",
    val weatherContext: String = "",
    val primaryOutfitName: String = "Outfit Recommendation",
    val topGarment: String = "",
    val bottomGarment: String = "",
    val footwear: String = "",
    val accessories: List<String> = emptyList(),
    val colorCoordination: String = "",
    val styleTip: String = "",
    val budgetOrWardrobeNote: String = ""
)

@JsonClass(generateAdapter = true)
data class DecisionPlanPayload(
    val decisionSubject: String = "Purchase / Action Evaluation",
    val itemPriceInr: Double = 0.0,
    val verdict: String = "EVALUATING", // AFFORDABLE & SAFE, CAUTION / POSTPONE, NOT RECOMMENDED
    val affordabilityVerdictColor: String = "AMBER", // EMERALD, AMBER, ROSE
    val financialImpactAnalysis: String = "",
    val monthlyDiscretionaryRemaining: Double? = null,
    val savingsGoalProtected: Boolean = true,
    val keyFactors: List<String> = emptyList(),
    val alternativesConsidered: List<DecisionAlternativePayload> = emptyList(),
    val finalRecommendation: String = ""
)

@JsonClass(generateAdapter = true)
data class DecisionAlternativePayload(
    val name: String = "",
    val priceInr: Double = 0.0,
    val pro: String = "",
    val isRecommended: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ProjectPlanPayload(
    val projectName: String = "Active Project",
    val category: String = "GENERAL",
    val currentPhase: String = "Planning",
    val objective: String = "",
    val keyContextSummary: String = "",
    val hardwareOrTools: List<String> = emptyList(),
    val activeMilestones: List<String> = emptyList(),
    val nextActionStep: String = "",
    val notes: String = ""
)

@JsonClass(generateAdapter = true)
data class DailyBriefingPayload(
    val greeting: String = "Good day",
    val dateString: String = "Today",
    val weatherSummary: String = "",
    val sonSchoolTransportTime: String = "",
    val transportStatus: String = "",
    val workPriorities: List<String> = emptyList(),
    val discretionaryBudgetInr: Double? = null,
    val pendingShoppingCount: Int = 0,
    val personalGoalsSummary: String = "",
    val proactiveVybSuggestion: String = ""
)

@JsonClass(generateAdapter = true)
data class WorkCommunicationPayload(
    val recipientNameOrRole: String = "Recipient",
    val channel: String = "Message / Email",
    val objective: String = "Communication",
    val keyConstraint: String = "",
    val draftSubject: String = "",
    val draftMessage: String = "",
    val tone: String = "Professional",
    val safetyNote: String = "VYB will not send without your manual confirmation. Tap below to copy or dispatch."
)

