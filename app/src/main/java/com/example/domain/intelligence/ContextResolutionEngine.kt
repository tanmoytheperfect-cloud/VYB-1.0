package com.example.domain.intelligence

import com.example.domain.genesis.GraphNodeType
import com.example.domain.genesis.PersonalContextGraph
import com.example.domain.genesis.VybPersonalProfile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Context Resolution Engine.
 * Resolves implicit user phrasing ("my usual cab", "call Rahul", "prepare for school")
 * into explicit, high-confidence contextual parameters.
 * If confidence is insufficient, flags clarificationQuestion instead of guessing.
 */
class ContextResolutionEngine {

    fun resolveContext(
        rawInput: String,
        intentType: UniversalIntentType,
        extractedEntities: Map<String, Any?>,
        profile: VybPersonalProfile,
        contextGraph: PersonalContextGraph? = null
    ): ResolvedPersonalContext {
        val lower = rawInput.lowercase(Locale.ROOT)
        val notes = mutableListOf<String>()
        var confidence = 1.0f
        var isSufficient = true
        var clarification: String? = null

        // 1. Resolve WHO (The Owner)
        val ownerName = profile.name.ifBlank { "Owner" }
        val callingTitle = profile.callingTitle.ifBlank { "Sir" }
        notes.add("Owner identified as $ownerName ($callingTitle)")

        // 2. Resolve HABITUAL PREFERENCES
        val habitualPrefs = mutableMapOf<String, String>()
        habitualPrefs["commute"] = profile.commutePreference
        habitualPrefs["diet"] = profile.dietaryPreference
        habitualPrefs["workplace"] = profile.officeAddress
        habitualPrefs["home"] = profile.homeAddress
        habitualPrefs["wakeTime"] = profile.wakeUpTime

        var destination: String? = extractedEntities["destination"] as? String
        var targetTime: String? = extractedEntities["time"] as? String
        var service: String? = extractedEntities["service"] as? String
        val budget: Double? = profile.monthlyIncome.takeIf { it > 0.0 }
        val relatedPeople = mutableListOf<String>()

        when (intentType) {
            UniversalIntentType.BOOK_TRANSPORT -> {
                // Check if user says "usual cab" or "to work" or "to home"
                if (lower.contains("usual") || lower.contains("to work") || lower.contains("to office")) {
                    if (profile.officeAddress.isNotBlank()) {
                        destination = profile.officeAddress
                        notes.add("Resolved destination to registered office address: $destination")
                    } else {
                        confidence *= 0.5f
                        isSufficient = false
                        clarification = "I don't have your office or usual destination address in your profile. Where would you like to travel to?"
                    }
                } else if (lower.contains("to home") || lower.contains("back home")) {
                    if (profile.homeAddress.isNotBlank()) {
                        destination = profile.homeAddress
                        notes.add("Resolved destination to registered home address: $destination")
                    } else {
                        confidence *= 0.5f
                        isSufficient = false
                        clarification = "Your home address is not set in your Personal Profile. What is your destination?"
                    }
                } else if (destination.isNullOrBlank()) {
                    confidence *= 0.4f
                    isSufficient = false
                    clarification = "Where would you like to book the cab to?"
                }

                // Service resolution
                if (service.isNullOrBlank()) {
                    service = when {
                        lower.contains("yatri") -> "Yatri Sathi"
                        lower.contains("ola") -> "Ola"
                        lower.contains("uber") -> "Uber"
                        profile.commutePreference.contains("Yatri", ignoreCase = true) -> "Yatri Sathi"
                        else -> "Uber" // default fallback
                    }
                    notes.add("Resolved preferred mobility provider: $service")
                }

                // Time resolution
                if (targetTime.isNullOrBlank()) {
                    val cal = Calendar.getInstance()
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    targetTime = "Immediate (${sdf.format(cal.time)})"
                    notes.add("Resolved departure time: $targetTime")
                }
            }

            UniversalIntentType.COMMUNICATION_DISPATCH -> {
                val targetName = extractedEntities["contact_name"] as? String
                if (!targetName.isNullOrBlank()) {
                    relatedPeople.add(targetName)
                    notes.add("Target recipient identified: $targetName")
                } else {
                    // Check if query implies a person like "Rahul" or "wife" or "spouse"
                    val personNode = contextGraph?.nodes?.firstOrNull {
                        it.type == GraphNodeType.PERSON && lower.contains(it.label.lowercase(Locale.ROOT))
                    }
                    if (personNode != null) {
                        relatedPeople.add(personNode.label)
                        notes.add("Resolved recipient from Context Graph: ${personNode.label}")
                    } else {
                        confidence *= 0.5f
                        isSufficient = false
                        clarification = "Who would you like me to contact?"
                    }
                }
            }

            UniversalIntentType.CREATE_REMINDER -> {
                if (targetTime.isNullOrBlank()) {
                    if (lower.contains("tomorrow")) {
                        targetTime = "Tomorrow morning (09:00 AM)"
                    } else if (lower.contains("tonight")) {
                        targetTime = "Tonight (08:00 PM)"
                    } else {
                        targetTime = "Today"
                    }
                    notes.add("Resolved reminder schedule: $targetTime")
                }
            }

            UniversalIntentType.FINANCE_ANALYSIS -> {
                notes.add("Financial context: Income=${profile.currencySymbol}${profile.monthlyIncome}, Commitments=${profile.currencySymbol}${profile.fixedCommitments}")
            }

            else -> {
                // General query resolution
            }
        }

        return ResolvedPersonalContext(
            ownerName = ownerName,
            callingTitle = callingTitle,
            habitualPreferences = habitualPrefs,
            contextualDestination = destination,
            contextualTime = targetTime,
            contextualService = service,
            contextualBudget = budget,
            relatedPeople = relatedPeople,
            confidenceScore = confidence,
            isSufficient = isSufficient,
            clarificationQuestion = clarification,
            resolutionNotes = notes
        )
    }
}
