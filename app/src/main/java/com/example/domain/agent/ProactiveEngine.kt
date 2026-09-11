package com.example.domain.agent

import java.util.Calendar
import java.util.Locale

data class ProactivePriorityItem(
    val priorityRank: Int,
    val category: String, // FAMILY, WORK, FINANCE, PERSONAL
    val title: String,
    val description: String,
    val isUrgent: Boolean,
    val actionSuggestion: String? = null
)

data class ProactiveBriefing(
    val greeting: String,
    val timeFormatted: String,
    val dayOfWeek: String,
    val primaryFocus: String,
    val priorities: List<ProactivePriorityItem>,
    val executiveSummary: String,
    val suggestedFollowUps: List<String>
)

class ProactiveEngine {

    fun generateBriefing(context: AgentContext): ProactiveBriefing {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeFormatted = String.format(Locale.US, "%02d:%02d", hour, minute)
        val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Today"

        val timeOfDay = when (hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..21 -> "Evening"
            else -> "Night"
        }

        val greeting = "Good $timeOfDay, ${context.ownerName}."
        val priorities = mutableListOf<ProactivePriorityItem>()
        var rank = 1

        // 1. Family / School Commute
        val sonSchoolTime = context.sonSchoolCommuteTime ?: "08:30 AM"
        if (hour < 9) {
            priorities.add(
                ProactivePriorityItem(
                    priorityRank = rank++,
                    category = "FAMILY",
                    title = "Son's School Commute",
                    description = "Scheduled for $sonSchoolTime. Transport needs confirmation.",
                    isUrgent = true,
                    actionSuggestion = "Coordinate School Cab"
                )
            )
        }

        // 2. Active Work / Priority Tasks
        if (context.activeTasksCount > 0) {
            val taskListSummary = context.activeTasksSummaries.take(2).joinToString("; ")
            priorities.add(
                ProactivePriorityItem(
                    priorityRank = rank++,
                    category = "WORK",
                    title = "Active Work Priorities",
                    description = "$taskListSummary (${context.activeTasksCount} tasks pending).",
                    isUrgent = hour in 9..17,
                    actionSuggestion = "Review Active Tasks"
                )
            )
        } else {
            priorities.add(
                ProactivePriorityItem(
                    priorityRank = rank++,
                    category = "WORK",
                    title = "Work Priorities",
                    description = "No urgent overdue deadlines logged on device.",
                    isUrgent = false
                )
            )
        }

        // 3. Financial Health
        val discretionary = context.activeDiscretionaryBuffer
        val savings = context.monthlySavingsTarget
        if (discretionary > 0) {
            priorities.add(
                ProactivePriorityItem(
                    priorityRank = rank++,
                    category = "FINANCE",
                    title = "Monthly Discretionary Buffer",
                    description = "₹${discretionary.toInt()} available for discretionary spending. Monthly savings target of ₹${savings.toInt()} is 100% safeguarded.",
                    isUrgent = false,
                    actionSuggestion = "Review Cashflow"
                )
            )
        }

        // 4. Memory & Lifestyle
        val memoryCount = context.retrievedMemories.size
        if (memoryCount > 0) {
            val firstPref = context.retrievedMemories.firstOrNull { it.category == "PREFERENCE" }
            val prefText = firstPref?.let { "Factored preference: \"${it.content}\"." } ?: "$memoryCount active items in memory vault."
            priorities.add(
                ProactivePriorityItem(
                    priorityRank = rank++,
                    category = "PERSONAL",
                    title = "Personal World Model",
                    description = prefText,
                    isUrgent = false
                )
            )
        }

        val primaryFocus = when {
            hour < 9 -> "School transport coordination for your son ($sonSchoolTime)"
            hour in 9..17 -> if (context.activeTasksCount > 0) "Executing priority work tasks" else "Maintaining work schedule"
            else -> "Evening wrap-up, family routines, and next-day preparation"
        }

        val summary = buildString {
            append("$greeting ($timeFormatted • $dayOfWeek)\n\n")
            append("Here is what matters most today:\n\n")
            priorities.forEach { item ->
                append("${item.priorityRank}. **${item.category}**: ${item.title}\n")
                append("   ${item.description}\n\n")
            }
            append("• **Immediate Priority**: $primaryFocus.")
        }

        val followUps = mutableListOf<String>()
        if (hour < 9) followUps.add("Handle my son's school cab")
        followUps.add("What should I do now?")
        followUps.add("Can I afford ₹5,000?")

        return ProactiveBriefing(
            greeting = greeting,
            timeFormatted = timeFormatted,
            dayOfWeek = dayOfWeek,
            primaryFocus = primaryFocus,
            priorities = priorities,
            executiveSummary = summary,
            suggestedFollowUps = followUps
        )
    }

    fun evaluateWhatShouldIDoNow(context: AgentContext): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeFormatted = String.format(Locale.US, "%02d:%02d", hour, minute)

        val sonSchoolTime = context.sonSchoolCommuteTime ?: "08:30 AM"

        return when {
            hour < 9 -> {
                "Right now ($timeFormatted), your top priority is **your son's school transport** ($sonSchoolTime).\n\n" +
                        "1. Confirm transport / cab to school.\n" +
                        "2. Review morning schedule before work.\n" +
                        "3. Your monthly financial buffer is healthy (₹${context.activeDiscretionaryBuffer.toInt()} available)."
            }
            hour in 9..12 -> {
                val tasks = if (context.activeTasksCount > 0) "${context.activeTasksSummaries.firstOrNull() ?: "Work deliverables"}" else "Review planned milestones"
                "Right now ($timeFormatted), you are in your **peak morning productivity window**.\n\n" +
                        "1. Focus on: $tasks.\n" +
                        "2. Keep interruptions minimal.\n" +
                        "3. Next routine checkpoint: Midday review."
            }
            hour in 12..14 -> {
                "Right now ($timeFormatted), it is **midday lunch & transition time**.\n\n" +
                        "1. Take a break and recharge.\n" +
                        "2. Check if any afternoon errands or school pickup arrangements are required.\n" +
                        "3. Prepare for afternoon task commitments."
            }
            hour in 14..18 -> {
                val tasks = if (context.activeTasksCount > 0) "${context.activeTasksCount} active tasks remaining" else "Wrap up client communication"
                "Right now ($timeFormatted), you are in your **afternoon execution block**.\n\n" +
                        "1. Wrap up remaining work tasks: $tasks.\n" +
                        "2. Verify family return travel arrangements if required."
            }
            else -> {
                "Right now ($timeFormatted), you are in your **evening wind-down & family time**.\n\n" +
                        "1. Review today's completed goals.\n" +
                        "2. Verify tomorrow's morning schedule.\n" +
                        "3. All systems in your personal vault are synchronized."
            }
        }
    }
}
