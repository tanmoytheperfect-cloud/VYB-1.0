package com.example.domain.brain

import java.text.SimpleDateFormat
import java.util.*

/**
 * Multi-Tier Cognitive Memory Engine with Reference & Pronoun Resolution.
 */
class CognitiveMemoryEngine(
    val worldModel: PersonalWorldModel = PersonalWorldModel()
) {
    // Conversational turn history for resolution
    private val conversationHistory = mutableListOf<ConversationTurn>()
    
    // Track active focus subject (e.g. "phone", "cab for son", "Amazon account connection", "jacket")
    var activeFocusEntity: FocusEntity? = null
        private set

    data class ConversationTurn(
        val role: String, // "USER" or "ASSISTANT"
        val message: String,
        val detectedGoal: String? = null,
        val referencedEntity: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class FocusEntity(
        val category: String, // "SHOPPING_ITEM", "CAB_REQUEST", "ACCOUNT_AUTH", "FINANCIAL_QUERY", "ROUTINE", "PROJECT"
        val subject: String,
        val attributes: MutableMap<String, String> = mutableMapOf(),
        val lastMentionedTimestamp: Long = System.currentTimeMillis()
    )

    fun recordTurn(role: String, message: String, detectedGoal: String? = null, entity: String? = null) {
        conversationHistory.add(ConversationTurn(role, message, detectedGoal, entity))
        if (entity != null) {
            // Update active focus entity
            val current = activeFocusEntity
            if (current != null && current.subject.equals(entity, ignoreCase = true)) {
                // Keep and refresh
                activeFocusEntity = current.copy(lastMentionedTimestamp = System.currentTimeMillis())
            }
        }
    }

    fun getRecentTurns(count: Int = 8): List<ConversationTurn> = conversationHistory.takeLast(count)

    /**
     * Pronoun & Reference Resolver.
     * Resolves references like "it", "one", "this", "that", "him", "the cheaper one", "my usual", "the school"
     * against previous turns and the Personal World Model.
     */
    fun resolveReferences(rawInput: String): ResolvedReferenceResult {
        val lower = rawInput.lowercase().trim()
        val recentUserTurns = conversationHistory.filter { it.role == "USER" }.takeLast(4)
        val lastUserMessage = recentUserTurns.lastOrNull()?.message?.lowercase() ?: ""
        
        var resolvedSubject: String? = activeFocusEntity?.subject
        var inferredGoal: String? = null
        val extractedBudget = extractBudget(lower)

        // 1. Check if referring to family/contacts
        val isSonReference = lower.contains("son") || lower.contains("child") || lower.contains("kid")
        if (isSonReference) {
            resolvedSubject = activeFocusEntity?.subject ?: "Family Member"
            inferredGoal = "FAMILY_TRANSPORT"
        }

        // 2. Check if referring to a previously mentioned item/product ("one", "it", "this", "that", "the phone", "find me one")
        if (lower.contains("find me one") || lower.contains("one around") || lower.contains("buy one") || lower.contains("find one")) {
            // Find what "one" refers to from recent history
            val previousItem = findSubjectInText(lastUserMessage) ?: activeFocusEntity?.subject
            if (previousItem != null) {
                resolvedSubject = previousItem
                inferredGoal = "PRODUCT_SEARCH"
            }
        } else if (lower == "should i buy this?" || lower == "should i buy this" || lower.startsWith("can i afford this") || lower.contains("buy this")) {
            val previousItem = activeFocusEntity?.subject ?: findSubjectInText(lastUserMessage)
            resolvedSubject = previousItem ?: "Inquired Purchase"
            inferredGoal = "AFFORDABILITY_DECISION"
        }

        // 3. Check for modifiers on current active subject ("make sure it's cheap", "don't book yet", "switch to sedan")
        if (lower.contains("make sure it's cheap") || lower.contains("cheaper one") || lower.contains("keep it budget")) {
            inferredGoal = "APPLY_BUDGET_PREFERENCE"
        }
        if (lower.contains("don't book yet") || lower.contains("dont book yet") || lower.contains("hold on") || lower.contains("cancel booking")) {
            inferredGoal = "HOLD_EXECUTION"
        }

        return ResolvedReferenceResult(
            rawInput = rawInput,
            resolvedSubject = resolvedSubject,
            inferredGoal = inferredGoal,
            budgetInr = extractedBudget,
            isSonReference = isSonReference,
            focusEntity = activeFocusEntity
        )
    }

    fun setActiveFocus(category: String, subject: String, attributes: Map<String, String> = emptyMap()) {
        activeFocusEntity = FocusEntity(
            category = category,
            subject = subject,
            attributes = attributes.toMutableMap()
        )
    }

    fun learnPreference(category: String, preferenceText: String, source: String = "User Conversation") {
        val existing = worldModel.learnedPreferences.find { it.preference.equals(preferenceText, ignoreCase = true) }
        if (existing == null) {
            worldModel.learnedPreferences.add(
                LearnedPreference(
                    category = category,
                    preference = preferenceText,
                    source = source,
                    confidence = 1.0
                )
            )
        }
    }

    /**
     * Resolves temporal phrases like "today", "tomorrow", "this morning", "next week", "later"
     * with live, accurate device clock awareness.
     */
    fun resolveTemporalContext(input: String): TemporalContext {
        val lower = input.lowercase()
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        val dateSdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.ENGLISH)
        val timeSdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val shortDateSdf = SimpleDateFormat("d MMM yyyy", Locale.ENGLISH)

        val formattedCurrentTime = timeSdf.format(calendar.time)
        val formattedCurrentDate = dateSdf.format(calendar.time)
        val shortDate = shortDateSdf.format(calendar.time)

        val timeOfDay = when (currentHour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }

        val isMorningPassedToday = currentHour >= 12

        return when {
            lower.contains("tomorrow") -> {
                val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                TemporalContext(
                    targetDayLabel = "Tomorrow (${dateSdf.format(tomorrowCal.time)})",
                    isTomorrow = true,
                    isToday = false,
                    currentTimeFormatted = formattedCurrentTime,
                    currentDateFormatted = formattedCurrentDate,
                    currentHour = currentHour,
                    currentMinute = currentMinute,
                    timeOfDay = timeOfDay,
                    isMorningPassedToday = isMorningPassedToday,
                    isMorning = lower.contains("morning")
                )
            }
            lower.contains("this morning") || lower.contains("morning") -> {
                if (isMorningPassedToday) {
                    // Current time is afternoon/evening/night. Morning has passed for today!
                    val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                    TemporalContext(
                        targetDayLabel = "Tomorrow Morning (${dateSdf.format(tomorrowCal.time)})",
                        isTomorrow = true,
                        isToday = false,
                        isMorning = true,
                        currentTimeFormatted = formattedCurrentTime,
                        currentDateFormatted = formattedCurrentDate,
                        currentHour = currentHour,
                        currentMinute = currentMinute,
                        timeOfDay = timeOfDay,
                        isMorningPassedToday = true,
                        passedMorningNote = "Current time is $formattedCurrentTime ($timeOfDay). Morning window for today has passed. Scheduling for Tomorrow Morning."
                    )
                } else {
                    TemporalContext(
                        targetDayLabel = "This Morning ($shortDate, $formattedCurrentTime)",
                        isMorning = true,
                        isToday = true,
                        currentTimeFormatted = formattedCurrentTime,
                        currentDateFormatted = formattedCurrentDate,
                        currentHour = currentHour,
                        currentMinute = currentMinute,
                        timeOfDay = timeOfDay,
                        isMorningPassedToday = false
                    )
                }
            }
            lower.contains("next week") -> {
                val nextWeekCal = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, 1) }
                TemporalContext(
                    targetDayLabel = "Next Week (${dateSdf.format(nextWeekCal.time)})",
                    isNextWeek = true,
                    isToday = false,
                    currentTimeFormatted = formattedCurrentTime,
                    currentDateFormatted = formattedCurrentDate,
                    currentHour = currentHour,
                    currentMinute = currentMinute,
                    timeOfDay = timeOfDay,
                    isMorningPassedToday = isMorningPassedToday
                )
            }
            else -> {
                TemporalContext(
                    targetDayLabel = "Today ($formattedCurrentDate • $formattedCurrentTime)",
                    isToday = true,
                    currentTimeFormatted = formattedCurrentTime,
                    currentDateFormatted = formattedCurrentDate,
                    currentHour = currentHour,
                    currentMinute = currentMinute,
                    timeOfDay = timeOfDay,
                    isMorningPassedToday = isMorningPassedToday
                )
            }
        }
    }

    private fun extractBudget(input: String): Double? {
        // e.g. "30k", "30,000", "₹30000", "around 30k", "under 25000", "8000"
        val kPattern = "(\\d+(?:\\.\\d+)?)\\s*k\\b".toRegex()
        val kMatch = kPattern.find(input)
        if (kMatch != null) {
            return (kMatch.groupValues[1].toDoubleOrNull() ?: 0.0) * 1000.0
        }

        val rupeePattern = "(?:₹|rs\\.?|inr)?\\s*(\\d{1,3}(?:,\\d{3})*|\\d{4,7})".toRegex()
        val matches = rupeePattern.findAll(input)
        for (m in matches) {
            val rawNum = m.groupValues[1].replace(",", "")
            val num = rawNum.toDoubleOrNull()
            if (num != null && num >= 100) {
                return num
            }
        }
        return null
    }

    private fun findSubjectInText(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("phone") || lower.contains("smartphone") || lower.contains("mobile") -> "Smartphone"
            lower.contains("laptop") || lower.contains("macbook") -> "Laptop"
            lower.contains("jacket") || lower.contains("coat") -> "Jacket"
            lower.contains("shoes") || lower.contains("sneakers") -> "Shoes"
            lower.contains("cab") || lower.contains("ride") || lower.contains("taxi") -> "Cab Transport"
            else -> null
        }
    }
}

data class ResolvedReferenceResult(
    val rawInput: String,
    val resolvedSubject: String?,
    val inferredGoal: String?,
    val budgetInr: Double?,
    val isSonReference: Boolean,
    val focusEntity: CognitiveMemoryEngine.FocusEntity?
)

data class TemporalContext(
    val targetDayLabel: String,
    val isToday: Boolean = true,
    val isTomorrow: Boolean = false,
    val isMorning: Boolean = false,
    val isNextWeek: Boolean = false,
    val currentTimeFormatted: String = "",
    val currentDateFormatted: String = "",
    val currentHour: Int = 12,
    val currentMinute: Int = 0,
    val timeOfDay: String = "Day",
    val isMorningPassedToday: Boolean = false,
    val passedMorningNote: String? = null
)
