package com.example.data.model

import com.squareup.moshi.JsonClass

enum class IntentType(val displayName: String, val categoryIcon: String) {
    DIET("Diet & Nutrition", "restaurant"),
    SHOPPING("Shopping & Groceries", "shopping_cart"),
    TRAVEL("Travel & Itinerary", "flight"),
    RIDE("Rides & Cab Booking", "local_taxi"),
    TASK("Tasks & Organization", "check_circle"),
    FINANCE("Finance & Budget", "account_balance_wallet"),
    COMMUNICATION("Email & Messaging", "mail"),
    DOCUMENT("Document Analysis", "description"),
    STYLE("Style & Wardrobe", "checkroom"),
    DECISION("Decision Advisor", "balance"),
    PROJECT("Projects & Context", "terminal"),
    BRIEFING("Daily Briefing", "wb_sunny"),
    GENERAL("General Assistant", "smart_toy");

    companion object {
        fun fromString(value: String?): IntentType {
            return when (value?.uppercase()) {
                "DIET", "NUTRITION", "MEAL_PLAN" -> DIET
                "SHOPPING", "GROCERY", "GROCERIES", "PRODUCT" -> SHOPPING
                "TRAVEL", "TRIP", "ITINERARY", "VACATION" -> TRAVEL
                "RIDE", "CAB", "UBER", "OLA", "RAPIDO", "TAXI", "CAB_BOOKING" -> RIDE
                "TASK", "TASKS", "REMINDER", "TODO" -> TASK
                "FINANCE", "BUDGET", "EXPENSE", "MONEY" -> FINANCE
                "COMMUNICATION", "EMAIL", "MESSAGE", "LETTER", "SUPPLIER" -> COMMUNICATION
                "DOCUMENT", "DOC", "ANALYSIS" -> DOCUMENT
                "STYLE", "WARDROBE", "FASHION", "OUTFIT", "CLOTHING" -> STYLE
                "DECISION", "AFFORD", "AFFORDABILITY", "CHOICE", "DECIDE" -> DECISION
                "PROJECT", "PROJECTS", "TECH", "PLANT", "DEVELOPMENT" -> PROJECT
                "BRIEFING", "TODAY", "MORNING", "ROUTINE", "SUMMARY" -> BRIEFING
                else -> GENERAL
            }
        }
    }
}
