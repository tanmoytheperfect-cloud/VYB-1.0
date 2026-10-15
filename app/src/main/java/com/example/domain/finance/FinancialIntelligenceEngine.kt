package com.example.domain.finance

import com.example.data.local.entity.ExpenseEntity
import com.example.domain.genesis.VybPersonalProfile
import java.text.NumberFormat
import java.util.Locale

enum class AffordabilityVerdict(val label: String, val isViable: Boolean) {
    AFFORDABLE("Comfortably Affordable", true),
    TIGHT_BUT_VIABLE("Viable with Budget Discipline", true),
    EXCEEDS_DISCRETIONARY("Exceeds Monthly Discretionary Buffer", false),
    UNSUSTAINABLE("Financial Target Risk", false)
}

data class FinancialHealthSnapshot(
    val monthlyIncome: Double,
    val fixedCommitments: Double,
    val monthlySavingsTarget: Double,
    val totalDiscretionaryBudget: Double,
    val totalSpentThisMonth: Double,
    val remainingDiscretionaryBuffer: Double,
    val savingsRatePercent: Double,
    val currencySymbol: String = "₹"
)

data class AffordabilityAssessment(
    val itemName: String,
    val itemCost: Double,
    val verdict: AffordabilityVerdict,
    val snapshot: FinancialHealthSnapshot,
    val impactExplanation: String,
    val recommendation: String
)

/**
 * Financial Intelligence Engine:
 * Implements strict, verifiable mathematical budget analysis and affordability checks.
 * Ensures zero fictitious estimates, respecting the owner's true income and savings targets.
 */
class FinancialIntelligenceEngine {

    fun calculateFinancialSnapshot(
        profile: VybPersonalProfile,
        expensesThisMonth: List<ExpenseEntity>
    ): FinancialHealthSnapshot {
        val income = profile.monthlyIncome.coerceAtLeast(0.0)
        val fixed = profile.fixedCommitments.coerceAtLeast(0.0)
        val savingsTarget = profile.savingsTarget.coerceAtLeast(0.0)
        val symbol = profile.currencySymbol.ifBlank { "₹" }

        val discretionaryBudget = (income - fixed - savingsTarget).coerceAtLeast(0.0)
        val spent = expensesThisMonth.sumOf { it.amount }
        val remaining = discretionaryBudget - spent

        val savingsRate = if (income > 0) (savingsTarget / income) * 100.0 else 0.0

        return FinancialHealthSnapshot(
            monthlyIncome = income,
            fixedCommitments = fixed,
            monthlySavingsTarget = savingsTarget,
            totalDiscretionaryBudget = discretionaryBudget,
            totalSpentThisMonth = spent,
            remainingDiscretionaryBuffer = remaining,
            savingsRatePercent = savingsRate,
            currencySymbol = symbol
        )
    }

    fun assessAffordability(
        itemName: String,
        cost: Double,
        profile: VybPersonalProfile,
        expensesThisMonth: List<ExpenseEntity>
    ): AffordabilityAssessment {
        val snapshot = calculateFinancialSnapshot(profile, expensesThisMonth)
        val cur = snapshot.currencySymbol

        if (snapshot.monthlyIncome <= 0.0) {
            return AffordabilityAssessment(
                itemName = itemName,
                itemCost = cost,
                verdict = AffordabilityVerdict.EXCEEDS_DISCRETIONARY,
                snapshot = snapshot,
                impactExplanation = "Your monthly income is not yet configured in your personal profile.",
                recommendation = "Please configure your monthly income and budget in Profile settings before assessing affordability."
            )
        }

        val remaining = snapshot.remainingDiscretionaryBuffer
        val (verdict, explanation, recommendation) = when {
            cost <= remaining * 0.50 -> {
                Triple(
                    AffordabilityVerdict.AFFORDABLE,
                    "The cost of $cur${cost.toInt()} leaves a safe remaining buffer of $cur${(remaining - cost).toInt()} for the rest of the month.",
                    "You can comfortably proceed with this purchase without impacting your $cur${snapshot.monthlySavingsTarget.toInt()} savings goal."
                )
            }
            cost <= remaining -> {
                Triple(
                    AffordabilityVerdict.TIGHT_BUT_VIABLE,
                    "This purchase consumes ${((cost / remaining) * 100).toInt()}% of your remaining discretionary buffer ($cur${remaining.toInt()}).",
                    "Viable, but you will need to limit further non-essential spending for the remainder of this cycle."
                )
            }
            cost <= remaining + snapshot.monthlySavingsTarget -> {
                val deficit = cost - remaining
                Triple(
                    AffordabilityVerdict.EXCEEDS_DISCRETIONARY,
                    "This purchase exceeds your remaining monthly discretionary buffer by $cur${deficit.toInt()}.",
                    "To purchase this now, you would need to reduce this month's savings deposit from $cur${snapshot.monthlySavingsTarget.toInt()} down to $cur${(snapshot.monthlySavingsTarget - deficit).toInt()}."
                )
            }
            else -> {
                Triple(
                    AffordabilityVerdict.UNSUSTAINABLE,
                    "The cost ($cur${cost.toInt()}) exceeds both your monthly discretionary budget and full monthly savings target.",
                    "Not recommended as an out-of-pocket monthly purchase. Consider setting up a multi-month sinking fund or capital expense goal."
                )
            }
        }

        return AffordabilityAssessment(
            itemName = itemName,
            itemCost = cost,
            verdict = verdict,
            snapshot = snapshot,
            impactExplanation = explanation,
            recommendation = recommendation
        )
    }

    fun formatAffordabilityBriefing(assessment: AffordabilityAssessment): String {
        val s = assessment.snapshot
        val cur = s.currencySymbol
        val sb = StringBuilder()

        sb.appendLine("📊 **Financial Affordability Analysis: ${assessment.itemName}**\n")
        sb.appendLine("• **Estimated Cost**: $cur${assessment.itemCost.toInt()}")
        sb.appendLine("• **Verdict**: **${assessment.verdict.label}**\n")

        sb.appendLine("### 💰 Monthly Budget Breakdown")
        sb.appendLine("• **Monthly Net Income**: $cur${s.monthlyIncome.toInt()}")
        sb.appendLine("• **Fixed Commitments (Rent, EMIs, Bills)**: -$cur${s.fixedCommitments.toInt()}")
        sb.appendLine("• **Monthly Savings Target (${s.savingsRatePercent.toInt()}%)**: -$cur${s.monthlySavingsTarget.toInt()}")
        sb.appendLine("• **Discretionary Spending Pool**: $cur${s.totalDiscretionaryBudget.toInt()}")
        sb.appendLine("• **Logged Spend to Date**: -$cur${s.totalSpentThisMonth.toInt()}")
        sb.appendLine("• **Current Discretionary Buffer**: **$cur${s.remainingDiscretionaryBuffer.toInt()}**\n")

        sb.appendLine("### 💡 Guidance & Impact")
        sb.appendLine(assessment.impactExplanation)
        sb.appendLine(assessment.recommendation)

        return sb.toString().trim()
    }
}
