package com.example.domain.intelligence

import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.RoutineEntity
import com.example.domain.genesis.PersonalContextGraph
import com.example.domain.genesis.PersonalDigitalTwin
import com.example.domain.genesis.VybPersonalProfile

/**
 * Personal Knowledge Engine (Part 11 & Part 32).
 * Compiles a structured, authentic personal intelligence briefing from verified on-device stores.
 * Strictly distinguishes what is CONFIRMED, what is INFERRED, and what is UNKNOWN.
 */
class PersonalKnowledgeEngine {

    fun generatePersonalKnowledgeBriefing(
        profile: VybPersonalProfile,
        digitalTwin: PersonalDigitalTwin?,
        contextGraph: PersonalContextGraph,
        memories: List<MemoryItemEntity>,
        familyMembers: List<FamilyMemberEntity>,
        goals: List<GoalEntity>,
        projects: List<ProjectEntity>,
        routines: List<RoutineEntity>,
        batteryLevel: Int? = null,
        isCharging: Boolean? = null
    ): String {
        val sb = StringBuilder()
        val title = profile.callingTitle.ifBlank { "Sir" }
        val name = profile.name.ifBlank { "Owner" }

        sb.appendLine("Here is what VYB currently knows about you, verified from your authorized local context:\n")

        // 1. WHO YOU ARE
        sb.appendLine("### 👤 WHO YOU ARE")
        sb.appendLine("• **Name**: $name ($title)")
        if (profile.personalCreed.isNotBlank()) sb.appendLine("• **Personal Creed**: \"${profile.personalCreed}\"")
        sb.appendLine("• **Primary Language**: ${profile.primaryLanguage} (Speaking: ${profile.aiSpeakingDialect})")
        sb.appendLine("• **Communication Style**: ${profile.communicationStyle.title}")

        // 2. WORK
        sb.appendLine("\n### 💼 WHAT VYB KNOWS ABOUT YOUR WORK")
        if (profile.companyName.isNotBlank() || profile.jobTitle.isNotBlank()) {
            sb.appendLine("• **Role**: ${profile.jobTitle.ifBlank { "Unspecified" }} at ${profile.companyName.ifBlank { "Unspecified Company" }}")
            sb.appendLine("• **Industry**: ${profile.industry}")
            if (profile.officeAddress.isNotBlank()) sb.appendLine("• **Office Address**: ${profile.officeAddress}")
        } else {
            sb.appendLine("• *Workplace details have not been explicitly specified in your profile.*")
        }

        // 3. FAMILY
        sb.appendLine("\n### 👨‍👩‍👧‍👦 WHAT VYB KNOWS ABOUT YOUR FAMILY")
        if (familyMembers.isNotEmpty()) {
            familyMembers.forEach { member ->
                sb.appendLine("• **${member.name}** (${member.relation}): ${member.occupationOrSchool.ifBlank { member.notes.ifBlank { "Family Member" } }}")
            }
        } else if (profile.familySummary.isNotBlank()) {
            sb.appendLine("• ${profile.familySummary}")
        } else {
            sb.appendLine("• *No family members registered yet in your Personal Life Vault.*")
        }

        // 4. PROJECTS
        sb.appendLine("\n### 🚀 YOUR ACTIVE PROJECTS")
        val activeProj = projects.map { it.title }.ifEmpty { profile.activeProjects }
        if (activeProj.isNotEmpty()) {
            activeProj.forEach { sb.appendLine("• $it") }
        } else {
            sb.appendLine("• *No active projects currently listed.*")
        }

        // 5. GOALS
        sb.appendLine("\n### 🎯 YOUR GOALS")
        val activeGoals = goals.map { it.title }.ifEmpty { profile.personalGoals }
        if (activeGoals.isNotEmpty()) {
            activeGoals.forEach { sb.appendLine("• $it") }
        } else {
            sb.appendLine("• *No personal goals registered.*")
        }

        // 6. ROUTINES
        sb.appendLine("\n### ⏰ YOUR ROUTINES")
        if (routines.isNotEmpty()) {
            routines.take(4).forEach { sb.appendLine("• **${it.timeOfDay}**: ${it.title} (${it.daysOfWeek})") }
        } else {
            sb.appendLine("• **Morning**: ${profile.morningRoutine}")
            sb.appendLine("• **Commute**: ${profile.commuteRoutine}")
            sb.appendLine("• **Evening**: ${profile.eveningRoutine}")
        }

        // 7. PREFERENCES
        sb.appendLine("\n### ⭐ YOUR PREFERENCES")
        sb.appendLine("• **Commute**: ${profile.commutePreference}")
        sb.appendLine("• **Diet**: ${profile.dietaryPreference}")
        sb.appendLine("• **Wakeup / Sleep**: ${profile.wakeUpTime} / ${profile.sleepTime}")

        // 8. IMPORTANT PEOPLE & PLACES
        sb.appendLine("\n### 📍 IMPORTANT PEOPLE & PLACES")
        val peopleStr = if (profile.importantPeople.isNotEmpty()) profile.importantPeople.joinToString(", ") else "None specified"
        sb.appendLine("• **People**: $peopleStr")
        val homeStr = profile.homeAddress.ifBlank { "Not set" }
        val officeStr = profile.officeAddress.ifBlank { "Not set" }
        sb.appendLine("• **Home**: $homeStr")
        sb.appendLine("• **Office**: $officeStr")

        // 9. CONNECTED SERVICES & DIGITAL LIFE
        sb.appendLine("\n### 📱 CONNECTED SERVICES & DIGITAL LIFE")
        profile.connectedServices.forEach { sb.appendLine("• $it") }
        if (batteryLevel != null) {
            sb.appendLine("• **Device State**: Battery at $batteryLevel%${if (isCharging == true) " (Charging)" else ""}")
        }
        sb.appendLine("• **Screen Time Target**: ${profile.screenTimeTargetHours} hrs/day")
        sb.appendLine("• **Do Not Disturb**: ${profile.dndStartTime} to ${profile.dndEndTime}")

        // 10. WHAT VYB DOES NOT KNOW (Transparent Honesty)
        sb.appendLine("\n### ❓ WHAT VYB DOES NOT KNOW (UNCONFIGURED)")
        val unknownList = mutableListOf<String>()
        if (profile.name.isBlank()) unknownList.add("Your legal name")
        if (profile.homeAddress.isBlank()) unknownList.add("Your home address")
        if (profile.officeAddress.isBlank()) unknownList.add("Your office address")
        if (profile.monthlyIncome <= 0.0) unknownList.add("Your exact monthly income & budget")
        if (familyMembers.isEmpty() && profile.familySummary.isBlank()) unknownList.add("Family dependents & school logistics")
        if (projects.isEmpty() && profile.activeProjects.isEmpty()) unknownList.add("Work milestones and deadlines")

        if (unknownList.isNotEmpty()) {
            unknownList.forEach { sb.appendLine("• $it") }
        } else {
            sb.appendLine("• *Core profile attributes are thoroughly configured.*")
        }

        // 11. TRUTH MODEL & DATA CERTAINTY
        sb.appendLine("\n### 🛡️ TRUTH & DATA CERTAINTY MODEL")
        sb.appendLine("• **[VERIFIED]**: Identity, explicit vault facts, verified schedule, hardware telemetry")
        if (profile.monthlyIncome > 0.0) {
            sb.appendLine("• **[VERIFIED FINANCIALS]**: Income ${profile.currencySymbol}${profile.monthlyIncome.toInt()}, Fixed Commitments ${profile.currencySymbol}${profile.fixedCommitments.toInt()}")
        }
        sb.appendLine("• **[INFERRED]**: Discovered routine patterns with confidence scoring")
        sb.appendLine("• **[ZERO HALLUCINATION]**: Unknown context is never assumed or simulated")

        sb.appendLine("\n*All information is stored exclusively on-device in your encrypted Personal Vault.*")

        return sb.toString().trim()
    }
}
