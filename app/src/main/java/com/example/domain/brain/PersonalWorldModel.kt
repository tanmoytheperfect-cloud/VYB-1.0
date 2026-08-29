package com.example.domain.brain

import android.content.SharedPreferences
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.LifeVaultItemEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.OwnerAccountEntity
import com.example.data.local.entity.ProjectEntity as RoomProjectEntity
import com.example.data.local.entity.RoutineEntity as RoomRoutineEntity
import java.util.*

/**
 * Universal Connected Personal World Model.
 * Dynamically hydrated for ANY user who installs VYB from Google Play.
 * Learns Owner Profile, Family Members, Life Schedule, Work, Financial Baseline,
 * and Mobile Device App Execution connectors.
 */
data class PersonalWorldModel(
    val owner: OwnerEntity = OwnerEntity(),
    val family: FamilyStructure = FamilyStructure(),
    val work: WorkContext = WorkContext(),
    val finance: FinanceContext = FinanceContext(),
    val health: HealthLifestyleContext = HealthLifestyleContext(),
    val shopping: ShoppingContext = ShoppingContext(),
    val travel: TravelContext = TravelContext(),
    val activeProjects: List<ProjectEntity> = emptyList(),
    val routines: List<RoutineModel> = emptyList(),
    val learnedPreferences: MutableList<LearnedPreference> = mutableListOf(),
    val services: MutableMap<String, ServiceIntegrationStatus> = mutableMapOf(),
    val recentDecisions: MutableList<DecisionRecord> = mutableListOf()
) {
    companion object {
        fun createDefault(): PersonalWorldModel {
            return PersonalWorldModel(
                owner = OwnerEntity(
                    name = "Owner",
                    title = "Owner",
                    email = "",
                    city = "",
                    state = "",
                    country = "",
                    jobTitle = "",
                    companyName = "",
                    homeAddress = "",
                    officeAddress = ""
                ),
                family = FamilyStructure(members = emptyList()),
                work = WorkContext(),
                finance = FinanceContext(
                    monthlyIncome = 0.0,
                    fixedCommitments = 0.0,
                    monthlySavingsTarget = 0.0,
                    activeDiscretionaryBuffer = 0.0,
                    currencySymbol = "₹"
                ),
                health = HealthLifestyleContext(
                    targetWeightKg = 0.0,
                    currentWeightKg = 0.0,
                    dailyProteinTargetGrams = 0
                ),
                activeProjects = emptyList(),
                routines = emptyList(),
                learnedPreferences = mutableListOf(),
                services = mutableMapOf(
                    "whatsapp" to ServiceIntegrationStatus("whatsapp", "WhatsApp Direct Dispatch", true, true, listOf("Draft message", "Send to contacts"), "Android Intent"),
                    "cabs" to ServiceIntegrationStatus("cabs", "Cab Dispatch (Yatri Sathi / Uber / Rapido)", true, true, listOf("Compare fares", "Launch booking"), "Android Intent"),
                    "maps" to ServiceIntegrationStatus("maps", "Google Maps & Navigation", true, true, listOf("Turn-by-turn navigation", "Search locations"), "Google Maps Intent"),
                    "amazon" to ServiceIntegrationStatus("amazon", "Amazon Shopping", true, true, listOf("Search products", "Launch product page"), "Amazon App / Web Intent"),
                    "gmail" to ServiceIntegrationStatus("gmail", "Gmail & Email Dispatch", true, true, listOf("Draft email", "Send to suppliers"), "Android Intent"),
                    "dialer" to ServiceIntegrationStatus("dialer", "Phone Dialer & Calls", true, true, listOf("Prepare call", "Dial contact"), "Android Dialer Intent")
                )
            )
        }

        /**
         * Hydrates dynamic World Model from User's SharedPreferences & Room Database
         */
        fun fromUserData(
            prefs: SharedPreferences,
            familyList: List<FamilyMemberEntity>,
            routinesList: List<RoomRoutineEntity>,
            goalsList: List<GoalEntity>,
            projectsList: List<RoomProjectEntity>,
            memoriesList: List<MemoryItemEntity>,
            accountsList: List<OwnerAccountEntity>,
            vaultList: List<LifeVaultItemEntity> = emptyList()
        ): PersonalWorldModel {
            val ownerName = prefs.getString("owner_name", "") ?: ""
            val ownerTitle = prefs.getString("owner_title", if (ownerName.isNotBlank()) "Sir $ownerName" else "Owner") ?: "Owner"
            val ownerEmail = prefs.getString("owner_email", "") ?: ""
            val ownerPhone = prefs.getString("owner_phone", "") ?: ""
            val homeAddress = prefs.getString("owner_home_address", "") ?: ""
            val officeAddress = prefs.getString("owner_office_address", "") ?: ""
            val jobTitle = prefs.getString("owner_job_title", "") ?: ""
            val companyName = prefs.getString("owner_company", "") ?: ""
            val city = prefs.getString("owner_city", "") ?: ""
            val currencySymbol = prefs.getString("currency_symbol", "₹") ?: "₹"

            val income = prefs.getFloat("monthly_income", 0f).toDouble()
            val fixed = prefs.getFloat("fixed_commitments", 0f).toDouble()
            val savingsTarget = prefs.getFloat("monthly_savings_target", 0f).toDouble()
            val discretionary = if (income > 0) (income - fixed - savingsTarget).coerceAtLeast(0.0) else 0.0

            val parsedFamily = familyList.map { f ->
                FamilyMemberModel(
                    name = f.name,
                    relation = f.relation,
                    age = 0,
                    occupationOrSchool = f.notes,
                    morningCommuteTime = "",
                    pickupTime = "",
                    notes = f.routineSummary
                )
            }

            val parsedRoutines = routinesList.map { r ->
                RoutineModel(
                    id = r.id.toString(),
                    title = r.title,
                    timeOfDay = r.timeOfDay,
                    days = r.daysOfWeek,
                    personInvolved = ownerName,
                    destination = r.description,
                    typicalAction = r.actionPrompt,
                    isCritical = r.isEnabled
                )
            }

            val parsedProjects = projectsList.map { p ->
                ProjectEntity(
                    id = p.id.toString(),
                    name = p.title,
                    category = p.category,
                    objective = p.description,
                    currentPhase = "${p.progressPercent}% Completed",
                    status = "ACTIVE"
                )
            }

            val parsedLearnedPrefs = memoriesList.map { m ->
                LearnedPreference(
                    category = m.category,
                    preference = "${m.key}: ${m.value}",
                    source = m.source,
                    confidence = 1.0
                )
            }.toMutableList()

            vaultList.forEach { v ->
                parsedLearnedPrefs.add(
                    LearnedPreference(
                        category = v.category,
                        preference = "${v.key}: ${v.value}",
                        source = "Vault (${v.source})",
                        confidence = 1.0
                    )
                )
            }

            val servicesMap = mutableMapOf(
                "whatsapp" to ServiceIntegrationStatus("whatsapp", "WhatsApp Direct Dispatch", true, true, listOf("Draft message", "Send to contacts"), "Android Intent"),
                "cabs" to ServiceIntegrationStatus("cabs", "Cab Dispatch (Yatri Sathi / Uber / Rapido)", true, true, listOf("Compare fares", "Launch booking"), "Android Intent"),
                "maps" to ServiceIntegrationStatus("maps", "Google Maps & Navigation", true, true, listOf("Turn-by-turn navigation", "Search locations"), "Google Maps Intent"),
                "amazon" to ServiceIntegrationStatus("amazon", "Amazon Shopping", true, true, listOf("Search products", "Launch product page"), "Amazon App / Web Intent"),
                "gmail" to ServiceIntegrationStatus("gmail", "Gmail & Email Dispatch", true, true, listOf("Draft email", "Send to suppliers"), "Android Intent"),
                "dialer" to ServiceIntegrationStatus("dialer", "Phone Dialer & Calls", true, true, listOf("Prepare call", "Dial contact"), "Android Dialer Intent")
            )

            return PersonalWorldModel(
                owner = OwnerEntity(
                    name = ownerName,
                    title = ownerTitle,
                    email = ownerEmail,
                    phone = ownerPhone,
                    city = city,
                    jobTitle = jobTitle,
                    companyName = companyName,
                    homeAddress = homeAddress,
                    officeAddress = officeAddress
                ),
                family = FamilyStructure(members = parsedFamily),
                work = WorkContext(
                    companyName = companyName,
                    role = jobTitle,
                    officeAddress = officeAddress
                ),
                finance = FinanceContext(
                    monthlyIncome = income,
                    fixedCommitments = fixed,
                    monthlySavingsTarget = savingsTarget,
                    activeDiscretionaryBuffer = discretionary,
                    currencySymbol = currencySymbol
                ),
                activeProjects = parsedProjects,
                routines = parsedRoutines,
                learnedPreferences = parsedLearnedPrefs,
                services = servicesMap
            )
        }
    }
}

data class OwnerEntity(
    val name: String = "",
    val title: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val homeAddress: String = "",
    val officeAddress: String = ""
)

data class FamilyStructure(
    val members: List<FamilyMemberModel> = emptyList()
) {
    val son: FamilyMemberModel? get() = members.find { it.relation.equals("son", ignoreCase = true) }
    val spouse: FamilyMemberModel? get() = members.find { it.relation.equals("spouse", ignoreCase = true) || it.relation.equals("wife", ignoreCase = true) }
}

data class FamilyMemberModel(
    val name: String,
    val relation: String,
    val age: Int = 0,
    val occupationOrSchool: String = "",
    val morningCommuteTime: String = "",
    val pickupTime: String = "",
    val notes: String = ""
)

data class WorkContext(
    val companyName: String = "",
    val role: String = "",
    val officeAddress: String = "",
    val activeIssues: List<String> = emptyList()
)

data class FinanceContext(
    val monthlyIncome: Double = 0.0,
    val fixedCommitments: Double = 0.0,
    val monthlySavingsTarget: Double = 0.0,
    val activeDiscretionaryBuffer: Double = 0.0,
    val currencySymbol: String = "₹"
)

data class HealthLifestyleContext(
    val targetWeightKg: Double = 0.0,
    val currentWeightKg: Double = 0.0,
    val dailyProteinTargetGrams: Int = 0,
    val dailyStepGoal: Int = 0
)

data class ShoppingContext(
    val recentInquiries: List<String> = emptyList(),
    val savedSizes: Map<String, String> = emptyMap()
)

data class TravelContext(
    val upcomingTrips: List<String> = emptyList()
)

data class ProjectEntity(
    val id: String,
    val name: String,
    val category: String,
    val objective: String,
    val currentPhase: String,
    val status: String
)

data class RoutineModel(
    val id: String,
    val title: String,
    val timeOfDay: String,
    val days: String,
    val personInvolved: String,
    val destination: String,
    val typicalAction: String,
    val isCritical: Boolean
)

data class LearnedPreference(
    val category: String,
    val preference: String,
    val source: String,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class ServiceIntegrationStatus(
    val serviceId: String,
    val displayName: String,
    val isConnected: Boolean,
    val canDirectlyExecute: Boolean,
    val supportedActions: List<String>,
    val authMethodDescription: String
)

data class DecisionRecord(
    val subject: String,
    val optionsConsidered: List<String>,
    val chosenOption: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
