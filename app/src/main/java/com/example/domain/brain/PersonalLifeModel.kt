package com.example.domain.brain

import android.content.SharedPreferences
import com.example.data.local.entity.*
import java.util.Calendar
import java.util.Locale

/**
 * Knowledge State for facts and inferences inside the Personal Life Model.
 * Distinguishes verified facts from inferences, unknown, and permission-gated data.
 */
enum class KnowledgeState {
    KNOWN,                 // Verified fact with high confidence
    INFERRED,              // Deducted from patterns or context; not yet verified as fact
    UNCERTAIN,             // Conflicting or low-confidence information
    UNKNOWN,               // No data available
    OUTDATED,              // Previously known but past freshness threshold
    REQUIRES_PERMISSION    // Requires explicit user authorization/permission
}

/**
 * Permission Scope required to access or modify a life facet.
 */
enum class PermissionScope {
    PUBLIC_READ,
    LOCAL_PRIVATE,
    SENSITIVE_FINANCIAL,
    SENSITIVE_FAMILY,
    DEVICE_HARDWARE,
    EXTERNAL_ACCOUNT
}

/**
 * Wrapper for every piece of information in the Personal Life Model.
 * Guarantees provenance, timestamp, confidence, permission scope, and verification tracking.
 */
data class LifeFact<T>(
    val value: T,
    val source: String = "SYSTEM_DEFAULT",
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f,
    val permissionScope: PermissionScope = PermissionScope.LOCAL_PRIVATE,
    val lastVerifiedTime: Long = System.currentTimeMillis(),
    val knowledgeState: KnowledgeState = KnowledgeState.KNOWN
)

/**
 * The Persistent Personal Life Model.
 * Represents the owner's entire digital and real-world context:
 * OWNER, FAMILY, HOME, WORK, FINANCE, ROUTINES, HABITS, PREFERENCES,
 * GOALS, PROJECTS, SHOPPING, TRAVEL, DEVICES, ACCOUNTS, SERVICES,
 * COMMITMENTS, DECISIONS, MEMORIES, CONVERSATIONS, RECENT EVENTS,
 * FUTURE EVENTS, LEARNED BEHAVIOURS.
 */
data class PersonalLifeModel(
    // 1. OWNER
    val ownerProfile: LifeFact<OwnerProfile> = LifeFact(OwnerProfile()),

    // 2. FAMILY
    val familyMembers: LifeFact<List<FamilyMemberInfo>> = LifeFact(emptyList()),

    // 3. HOME
    val homeDetails: LifeFact<HomeDetails> = LifeFact(HomeDetails()),

    // 4. WORK
    val workContext: LifeFact<WorkProfile> = LifeFact(WorkProfile()),

    // 5. FINANCE
    val financialContext: LifeFact<FinancialLifeContext> = LifeFact(FinancialLifeContext(), permissionScope = PermissionScope.SENSITIVE_FINANCIAL),

    // 6. ROUTINES
    val dailyRoutines: LifeFact<List<LifeRoutine>> = LifeFact(emptyList()),

    // 7. HABITS
    val observedHabits: LifeFact<List<LifeHabit>> = LifeFact(emptyList(), knowledgeState = KnowledgeState.INFERRED),

    // 8. PREFERENCES
    val userPreferences: LifeFact<Map<String, String>> = LifeFact(emptyMap()),

    // 9. GOALS
    val activeGoals: LifeFact<List<LifeGoal>> = LifeFact(emptyList()),

    // 10. PROJECTS
    val activeProjects: LifeFact<List<LifeProject>> = LifeFact(emptyList()),

    // 11. SHOPPING
    val shoppingContext: LifeFact<ShoppingLifeContext> = LifeFact(ShoppingLifeContext()),

    // 12. TRAVEL
    val travelContext: LifeFact<TravelLifeContext> = LifeFact(TravelLifeContext()),

    // 13. DEVICES
    val connectedDevices: LifeFact<List<DeviceInfo>> = LifeFact(listOf(DeviceInfo("Current Android Phone", "Pixel / Android Device", isPrimary = true))),

    // 14. ACCOUNTS
    val connectedAccounts: LifeFact<List<AccountStatus>> = LifeFact(emptyList(), permissionScope = PermissionScope.EXTERNAL_ACCOUNT),

    // 15. SERVICES
    val integratedServices: LifeFact<Map<String, ServiceStatusInfo>> = LifeFact(emptyMap()),

    // 16. COMMITMENTS
    val upcomingCommitments: LifeFact<List<LifeCommitment>> = LifeFact(emptyList()),

    // 17. DECISIONS
    val recordedDecisions: LifeFact<List<LifeDecision>> = LifeFact(emptyList()),

    // 18. MEMORIES
    val memoryVaultItems: LifeFact<List<LifeVaultItemEntity>> = LifeFact(emptyList()),

    // 19. CONVERSATIONS
    val recentConversationSummaries: LifeFact<List<String>> = LifeFact(emptyList()),

    // 20. RECENT EVENTS
    val recentTimelineEvents: LifeFact<List<TimelineEventEntity>> = LifeFact(emptyList()),

    // 21. FUTURE EVENTS
    val futureEvents: LifeFact<List<FutureLifeEvent>> = LifeFact(emptyList()),

    // 22. LEARNED BEHAVIOURS
    val learnedBehaviours: LifeFact<List<LearnedBehaviour>> = LifeFact(emptyList(), knowledgeState = KnowledgeState.INFERRED)
) {
    companion object {
        fun createDefault(): PersonalLifeModel = fromUserData(null)

        fun fromUserData(
            prefs: SharedPreferences?,
            familyList: List<FamilyMemberEntity> = emptyList(),
            routinesList: List<RoutineEntity> = emptyList(),
            goalsList: List<GoalEntity> = emptyList(),
            projectsList: List<com.example.data.local.entity.ProjectEntity> = emptyList(),
            memoriesList: List<MemoryItemEntity> = emptyList(),
            accountsList: List<OwnerAccountEntity> = emptyList(),
            vaultList: List<LifeVaultItemEntity> = emptyList(),
            learnedRoutines: List<LearnedRoutineEntity> = emptyList(),
            timelineEvents: List<TimelineEventEntity> = emptyList()
        ): PersonalLifeModel {
            val name = prefs?.getString("owner_name", "")?.takeIf { it.isNotBlank() } ?: "Tanmoy"
            val title = prefs?.getString("owner_title", "")?.takeIf { it.isNotBlank() } ?: "Sir Tanmoy"
            val city = prefs?.getString("owner_city", "")?.takeIf { it.isNotBlank() } ?: "Kolkata"
            val homeAddress = prefs?.getString("owner_home_address", "")?.takeIf { it.isNotBlank() } ?: "Salt Lake, Kolkata"
            val company = prefs?.getString("owner_company", "")?.takeIf { it.isNotBlank() } ?: "Tech Studio"
            val jobTitle = prefs?.getString("owner_job_title", "")?.takeIf { it.isNotBlank() } ?: "Director"

            val income = (prefs?.getFloat("monthly_income", 120000f) ?: 120000f).toDouble()
            val fixed = (prefs?.getFloat("fixed_commitments", 45000f) ?: 45000f).toDouble()
            val savings = (prefs?.getFloat("monthly_savings_target", 35000f) ?: 35000f).toDouble()
            val discretionary = (income - fixed - savings).coerceAtLeast(0.0)

            // Family members
            val family = if (familyList.isNotEmpty()) {
                familyList.map { FamilyMemberInfo(it.name, it.relation, it.routineSummary, it.occupationOrSchool) }
            } else {
                listOf(FamilyMemberInfo(name = "Vivaan", relation = "Son", routineNotes = "School pickup at 10:30 AM / 2:30 PM", schoolOrWorkPlace = "Delhi Public School"))
            }

            // Routines
            val routines = if (routinesList.isNotEmpty()) {
                routinesList.map { LifeRoutine(it.title, it.timeOfDay, it.description, it.daysOfWeek) }
            } else {
                listOf(
                    LifeRoutine("Son's School Commute", "10:30 AM", "Arrange cab pickup for Vivaan", "Mon, Tue, Wed, Thu, Fri"),
                    LifeRoutine("Morning Planning Briefing", "08:00 AM", "Review daily priorities and financial status", "Everyday"),
                    LifeRoutine("Evening Review", "08:30 PM", "Review tasks and prepare for tomorrow", "Everyday")
                )
            }

            // Decisions
            val decisions = listOf(
                LifeDecision(
                    topic = "Royal Enfield Classic 350",
                    decisionSummary = "Postponed purchase to next quarter until savings target reaches ₹1,50,000 buffer.",
                    timestamp = System.currentTimeMillis() - 7 * 86400000L,
                    rationale = "Protects core family emergency fund."
                )
            )

            // Preferences
            val prefsMap = mutableMapOf(
                "CAB_PROVIDER" to "Yatri Sathi / Rapido",
                "BUDGET_DISCIPLINE" to "High",
                "COMMUNICATION_TONE" to "Executive Chief of Staff"
            )

            return PersonalLifeModel(
                ownerProfile = LifeFact(OwnerProfile(name = name, title = title, city = city, company = company, jobTitle = jobTitle)),
                familyMembers = LifeFact(family, permissionScope = PermissionScope.SENSITIVE_FAMILY),
                homeDetails = LifeFact(HomeDetails(city = city, address = homeAddress)),
                workContext = LifeFact(WorkProfile(company = company, role = jobTitle)),
                financialContext = LifeFact(
                    FinancialLifeContext(
                        monthlyIncome = income,
                        fixedCommitments = fixed,
                        monthlySavingsTarget = savings,
                        safeDiscretionaryBuffer = discretionary,
                        activeReserve = 65000.0
                    ),
                    permissionScope = PermissionScope.SENSITIVE_FINANCIAL
                ),
                dailyRoutines = LifeFact(routines),
                userPreferences = LifeFact(prefsMap),
                recordedDecisions = LifeFact(decisions),
                memoryVaultItems = LifeFact(vaultList),
                recentTimelineEvents = LifeFact(timelineEvents.takeLast(20)),
                futureEvents = LifeFact(
                    listOf(
                        FutureLifeEvent(
                            title = "Family Vacation to Darjeeling",
                            scheduledDate = "Next Month (15th)",
                            category = "TRAVEL",
                            estimatedCostInr = 35000.0
                        )
                    )
                )
            )
        }
    }
}

data class OwnerProfile(
    val name: String = "Tanmoy",
    val title: String = "Sir Tanmoy",
    val email: String = "tanmoy@example.com",
    val phone: String = "+91 98300 XXXXX",
    val city: String = "Kolkata",
    val company: String = "Tech Studio",
    val jobTitle: String = "Director"
)

data class FamilyMemberInfo(
    val name: String,
    val relation: String,
    val routineNotes: String = "",
    val schoolOrWorkPlace: String = ""
)

data class HomeDetails(
    val city: String = "Kolkata",
    val address: String = "Salt Lake, Kolkata",
    val smartDevicesCount: Int = 0
)

data class WorkProfile(
    val company: String = "Tech Studio",
    val role: String = "Director",
    val workHours: String = "10:00 AM - 6:30 PM"
)

data class FinancialLifeContext(
    val monthlyIncome: Double = 120000.0,
    val fixedCommitments: Double = 45000.0,
    val monthlySavingsTarget: Double = 35000.0,
    val safeDiscretionaryBuffer: Double = 40000.0,
    val activeReserve: Double = 65000.0,
    val currencySymbol: String = "₹"
)

data class LifeRoutine(
    val title: String,
    val timeFormatted: String,
    val actionSummary: String,
    val recurrence: String
)

data class LifeHabit(
    val name: String,
    val frequency: String,
    val context: String,
    val confidence: Float
)

data class LifeGoal(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val category: String,
    val targetDate: String? = null,
    val progressPercent: Int = 0
)

data class LifeProject(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val phase: String,
    val nextAction: String
)

data class ShoppingLifeContext(
    val pendingItems: List<String> = emptyList(),
    val preferredPlatforms: List<String> = listOf("Amazon", "Blinkit", "Flipkart"),
    val amazonConnectionStatus: KnowledgeState = KnowledgeState.UNKNOWN
)

data class TravelLifeContext(
    val upcomingTrips: List<String> = listOf("Darjeeling Vacation next month"),
    val preferredAirlines: List<String> = emptyList(),
    val preferredCabProviders: List<String> = listOf("Yatri Sathi", "Rapido", "Uber")
)

data class DeviceInfo(
    val name: String,
    val type: String,
    val isPrimary: Boolean = true
)

data class AccountStatus(
    val provider: String,
    val accountId: String,
    val isConnected: Boolean,
    val lastSyncTime: Long? = null
)

data class ServiceStatusInfo(
    val serviceId: String,
    val displayName: String,
    val isAvailable: Boolean,
    val connectionType: String // "NATIVE_INTENT", "OAUTH_API", "APP_DEEPLINK"
)

data class LifeCommitment(
    val title: String,
    val dueDateTime: String,
    val category: String,
    val priority: String = "HIGH"
)

data class LifeDecision(
    val id: String = java.util.UUID.randomUUID().toString(),
    val topic: String,
    val decisionSummary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rationale: String = ""
)

data class FutureLifeEvent(
    val title: String,
    val scheduledDate: String,
    val category: String,
    val estimatedCostInr: Double? = null
)

data class LearnedBehaviour(
    val patternDescription: String,
    val observationCount: Int,
    val confidence: Float,
    val isActive: Boolean = true
)
