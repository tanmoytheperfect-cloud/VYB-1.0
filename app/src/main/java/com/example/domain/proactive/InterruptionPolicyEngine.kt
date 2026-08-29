package com.example.domain.proactive

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

class InterruptionPolicyEngine {

    private val dismissedCategoryCounts = mutableMapOf<String, Int>()

    fun recordDismissal(category: String) {
        val current = dismissedCategoryCounts.getOrDefault(category, 0)
        dismissedCategoryCounts[category] = current + 1
    }

    fun recordEngagement(category: String) {
        val current = dismissedCategoryCounts.getOrDefault(category, 0)
        if (current > 0) {
            dismissedCategoryCounts[category] = current - 1
        }
    }

    fun shouldDeliverProactiveAlert(
        category: String,
        basePriority: NotificationPriority,
        isNightOrResting: Boolean
    ): Boolean {
        if (isNightOrResting && basePriority != NotificationPriority.URGENT) {
            return false
        }

        val dismissals = dismissedCategoryCounts.getOrDefault(category, 0)
        if (dismissals >= 3 && basePriority == NotificationPriority.LOW) {
            // Adaptive suppression
            return false
        }

        return true
    }
}
