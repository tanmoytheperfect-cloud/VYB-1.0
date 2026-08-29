package com.example.domain.brain

import com.example.data.model.IntentType
import com.example.domain.actions.ActionLevel
import com.example.domain.actions.VybAction
import com.example.domain.tools.ToolRegistry

object IntentEngine {
    fun resolveIntent(input: String): IntentType {
        val lower = input.lowercase().trim()
        return when {
            lower.contains("cab") || lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("taxi") || lower.contains("ride") || lower.contains("transport") -> IntentType.RIDE
            lower.contains("diet") || lower.contains("calorie") || lower.contains("protein") || lower.contains("meal plan") || lower.contains("weight") || lower.contains("nutrition") -> IntentType.DIET
            lower.contains("shop") || lower.contains("buy") || lower.contains("grocery") || lower.contains("groceries") || lower.contains("bazaar") || lower.contains("amazon") || lower.contains("flipkart") || lower.contains("gift") || lower.contains("purchase") -> IntentType.SHOPPING
            lower.contains("travel") || lower.contains("trip") || lower.contains("darjeeling") || lower.contains("hotel") || lower.contains("flight") || lower.contains("vacation") || lower.contains("itinerary") || lower.contains("visit") -> IntentType.TRAVEL
            lower.contains("invest") || lower.contains("expense") || lower.contains("budget") || lower.contains("salary") || lower.contains("sip") || lower.contains("rupee") || lower.contains("₹") || lower.contains("money") || lower.contains("afford") || lower.contains("save") -> IntentType.FINANCE
            lower.contains("task") || lower.contains("todo") || lower.contains("remind") || lower.contains("deadline") || lower.contains("priority") -> IntentType.TASK
            lower.contains("mail") || lower.contains("email") || lower.contains("message") || lower.contains("whatsapp") -> IntentType.COMMUNICATION
            lower.contains("doc") || lower.contains("document") || lower.contains("invoice") || lower.contains("pdf") -> IntentType.DOCUMENT
            else -> IntentType.GENERAL
        }
    }
}

object GoalEngine {
    fun formulateGoal(input: String, ownerName: String): GoalPlan {
        val displayName = if (ownerName.isNotBlank()) ownerName else "User"
        return GoalPlan(
            goalTitle = "Goal: ${input.take(40)}",
            objectiveSummary = "Execution protocol formulated for $displayName based on provided requirements.",
            category = "GENERAL",
            steps = listOf(
                PlanStep(1, "Clarify Objectives & Constraints", "Define clear milestones and parameters", "web_search", ActionLevel.LEVEL_0_INFORMATIONAL),
                PlanStep(2, "Execute Core Phase", "Coordinate required actions and verify steps", "task_manager", ActionLevel.LEVEL_1_ORGANIZATIONAL),
                PlanStep(3, "Verify Outcome", "Ensure goal satisfaction and review results", "task_manager", ActionLevel.LEVEL_1_ORGANIZATIONAL)
            ),
            whyRecommended = "Structured decomposition ensures highest likelihood of goal achievement."
        )
    }
}

object DecisionEngine {
    fun evaluateOptions(query: String): DecisionAnalysis {
        val lower = query.lowercase()
        return if (lower.contains("cab") || lower.contains("airport") || lower.contains("transport")) {
            DecisionAnalysis(
                query = query,
                criteria = listOf("App Availability", "App Launch Support", "Fleet Type", "Kolkata Coverage"),
                options = listOf(
                    DecisionOption("Rapido", "Bike, Auto & Economy Cabs", 0.0, listOf("Supported provider", "Quick city trips"), listOf("Check app for live fare"), 4.6),
                    DecisionOption("Uber", "Uber Go & Premier Sedans", 0.0, listOf("Supported provider", "Airport & citywide"), listOf("Check app for live fare"), 4.8),
                    DecisionOption("Ola", "Prime & Mini Cabs", 0.0, listOf("Supported provider", "Wide local availability"), listOf("Check app for live fare"), 4.6),
                    DecisionOption("Yatri Sathi", "WB Govt Metered Taxi Network", 0.0, listOf("Supported provider", "Regulated non-surge fares"), listOf("Check app for live fare"), 4.5, isRecommended = true, recommendationReason = "Regulated local taxi network in Kolkata. Open app to view real-time metered options."),
                    DecisionOption("inDrive", "Passenger Fare Choice & Bidding", 0.0, listOf("Supported provider", "Fare negotiation"), listOf("Check app for live fare"), 4.4)
                ),
                bestOverallOption = "Yatri Sathi / Rapido / Uber",
                whyVybRecommends = "Opening installed ride apps allows you to view real-time pricing and driver availability on your device."
            )
        } else {
            DecisionAnalysis(
                query = query,
                criteria = listOf("Budget / Cost", "Quality", "Convenience", "Durability"),
                options = listOf(
                    DecisionOption("Premium Option", "Top brand with 2-year warranty", 4500.0, listOf("Exceptional build", "Long life"), listOf("Higher initial cost"), 4.8),
                    DecisionOption("Balanced Value Option", "Bestseller with 4.7★ from 12,000+ users", 2800.0, listOf("Great price-to-performance", "Same-day delivery in Kolkata"), listOf("Standard warranty"), 4.7, isRecommended = true, recommendationReason = "Optimal balance of verified customer reviews and 38% price savings."),
                    DecisionOption("Budget Option", "Economical entry model", 1400.0, listOf("Lowest upfront cost"), listOf("Basic materials"), 4.2)
                ),
                bestOverallOption = "Balanced Value Option (₹2,800)",
                whyVybRecommends = "Offers 95% of premium features at ₹2,800, saving ₹1,700 while maintaining top reliability."
            )
        }
    }
}
