package com.example.domain.finance

import com.example.domain.brain.FinancialLifeContext
import com.example.domain.brain.PersonalLifeModel

data class FinancialAdvice(
    val query: String,
    val facts: List<String>,
    val assumptions: List<String>,
    val recommendation: String,
    val isAffordableImmediately: Boolean,
    val impactOnSavingsTarget: String
)

class FinancialIntelligenceEngine {

    /**
     * Answers queries like "Can I afford a Classic 350?", "Can I afford ₹50,000?", etc.
     */
    fun evaluateAffordability(
        query: String,
        amountInr: Double?,
        lifeModel: PersonalLifeModel
    ): FinancialAdvice {
        val fin = lifeModel.financialContext.value
        val lower = query.lowercase()

        // Known bike query or amount detection
        val effectiveAmount = amountInr ?: when {
            lower.contains("classic 350") || lower.contains("royal enfield") || lower.contains("bike") -> 220000.0
            lower.contains("iphone") || lower.contains("phone") -> 80000.0
            lower.contains("laptop") -> 95000.0
            else -> 25000.0
        }

        val currency = fin.currencySymbol
        val monthlyDiscretionary = fin.safeDiscretionaryBuffer
        val currentReserve = fin.activeReserve
        val targetSavings = fin.monthlySavingsTarget

        val facts = listOf(
            "Monthly income: $currency${fin.monthlyIncome.toInt()}",
            "Fixed commitments: $currency${fin.fixedCommitments.toInt()}",
            "Monthly savings target: $currency${targetSavings.toInt()}",
            "Safe discretionary buffer this month: $currency${monthlyDiscretionary.toInt()}",
            "Estimated item cost: $currency${effectiveAmount.toInt()}"
        )

        val assumptions = listOf(
            "Emergency reserve of at least 3 months fixed expenses is preserved.",
            "No unexpected medical or family liabilities arise this billing cycle."
        )

        val isAffordableImmediately = effectiveAmount <= monthlyDiscretionary

        val recommendation = if (isAffordableImmediately) {
            "Yes, you can safely afford this purchase of $currency${effectiveAmount.toInt()}. It fits cleanly within your monthly discretionary buffer ($currency${monthlyDiscretionary.toInt()}) without impacting your monthly savings target of $currency${targetSavings.toInt()}."
        } else if (effectiveAmount <= currentReserve) {
            "You have the cash in reserve, but I would recommend deferring or financing over a few months. Paying $currency${effectiveAmount.toInt()} upfront immediately exhausts your active discretionary buffer and would reduce your planned savings below your monthly target ($currency${targetSavings.toInt()})."
        } else {
            "I would recommend waiting. An outlay of $currency${effectiveAmount.toInt()} exceeds both your safe monthly discretionary buffer ($currency${monthlyDiscretionary.toInt()}) and current liquid reserve. You previously decided to prioritize building your emergency fund to ₹1,50,000 first."
        }

        val impact = if (isAffordableImmediately) {
            "Zero negative impact on your ₹${targetSavings.toInt()} savings target."
        } else {
            "Would breach your ₹${targetSavings.toInt()} savings goal by approximately ₹${(effectiveAmount - monthlyDiscretionary).toInt()}."
        }

        return FinancialAdvice(
            query = query,
            facts = facts,
            assumptions = assumptions,
            recommendation = recommendation,
            isAffordableImmediately = isAffordableImmediately,
            impactOnSavingsTarget = impact
        )
    }

    /**
     * Answers queries like "How much can I safely spend this month?"
     */
    fun calculateSafeDiscretionarySpend(lifeModel: PersonalLifeModel): String {
        val fin = lifeModel.financialContext.value
        val safeAmount = fin.safeDiscretionaryBuffer
        val sym = fin.currencySymbol

        return """
            Based on your financial baseline:
            • Monthly Income: $sym${fin.monthlyIncome.toInt()}
            • Fixed Commitments: $sym${fin.fixedCommitments.toInt()}
            • Monthly Savings Target: $sym${fin.monthlySavingsTarget.toInt()}
            
            Your safe discretionary spending limit for this month is $sym${safeAmount.toInt()}. Spending up to this threshold protects your core emergency reserves and savings discipline.
        """.trimIndent()
    }

    /**
     * Answers queries like "I have ₹20,000 extra. What should I do with it?"
     */
    fun adviseSurplusCapital(amountInr: Double, lifeModel: PersonalLifeModel): String {
        val fin = lifeModel.financialContext.value
        val sym = fin.currencySymbol

        val currentReserve = fin.activeReserve
        val targetReserve = 150000.0

        return if (currentReserve < targetReserve) {
            val needed = (targetReserve - currentReserve).toInt()
            """
                Recommendation for extra $sym${amountInr.toInt()}:
                1. Allocate $sym${(amountInr * 0.7).toInt()} toward your emergency liquid reserve (currently at $sym${currentReserve.toInt()}, target $sym${targetReserve.toInt()}).
                2. Allocate $sym${(amountInr * 0.3).toInt()} toward your planned family vacation to Darjeeling next month.
                
                This strengthens your financial safety net while still advancing your personal travel goals.
            """.trimIndent()
        } else {
            """
                Recommendation for extra $sym${amountInr.toInt()}:
                Your liquid emergency reserve is already healthy. I recommend:
                1. 50% ($sym${(amountInr * 0.5).toInt()}) into index mutual funds / SIP.
                2. 50% ($sym${(amountInr * 0.5).toInt()}) toward personal lifestyle or upcoming vacation planning.
            """.trimIndent()
        }
    }
}
