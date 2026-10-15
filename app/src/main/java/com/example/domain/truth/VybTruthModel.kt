package com.example.domain.truth

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Truth Model & Confidence Hierarchy for the VYB Intelligence Layer.
 * Implements strict distinction between verified facts, inferred patterns,
 * and stale or unknown data. Never assumes or hallucinate.
 */
enum class TruthConfidenceLevel(val label: String, val minConfidence: Float, val maxConfidence: Float) {
    VERIFIED("VERIFIED", 0.90f, 1.0f),
    INFERRED("INFERRED", 0.50f, 0.89f),
    STALE("STALE", 0.10f, 0.49f),
    UNKNOWN("UNKNOWN", 0.0f, 0.09f),
    CONFLICTING("CONFLICTING", 0.0f, 1.0f)
}

/**
 * Source of truth for a piece of personal intelligence.
 */
enum class TruthSourceType(val displayName: String) {
    USER_EXPLICIT("User Explicit Stated"),
    HARDWARE_SENSOR("Hardware Sensor / GPS Telemetry"),
    SYSTEM_DATABASE("On-Device Encrypted Database"),
    OBSERVED_ROUTINE("Machine Learned Routine Pattern"),
    THIRD_PARTY_SERVICE("Authenticated Service API"),
    HEURISTIC_INFERENCE("Heuristic Context Inference")
}

/**
 * Encapsulates a verified or inferred fact within the Truth Model.
 */
data class TruthFact(
    val factId: String,
    val category: String,
    val key: String,
    val value: String,
    val confidenceLevel: TruthConfidenceLevel,
    val confidenceScore: Float,
    val sourceType: TruthSourceType,
    val sourceDetail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val freshnessWindowMillis: Long = DEFAULT_FRESHNESS_WINDOW_MS,
    val isUserConfirmed: Boolean = false,
    val verificationProof: String? = null
) {
    val isStale: Boolean
        get() = (System.currentTimeMillis() - timestamp) > freshnessWindowMillis && !isPermanent

    val isPermanent: Boolean
        get() = category in setOf("IDENTITY", "FAMILY", "PREFERENCES") && isUserConfirmed

    companion object {
        const val DEFAULT_FRESHNESS_WINDOW_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val TELEMETRY_FRESHNESS_MS = 15 * 60 * 1000L // 15 mins for GPS/battery
        const val FARE_FRESHNESS_MS = 10 * 60 * 1000L // 10 mins for ride fares
    }
}

/**
 * Truth Model Engine:
 * Validates, ranks, and audits truth certainty across the personal context graph,
 * memories, life vault, and live telemetry.
 */
class VybTruthEngine {

    fun evaluateTruth(
        category: String,
        key: String,
        value: String,
        statedConfidence: Float,
        source: String,
        timestamp: Long = System.currentTimeMillis(),
        freshnessWindowMs: Long = TruthFact.DEFAULT_FRESHNESS_WINDOW_MS
    ): TruthFact {
        val now = System.currentTimeMillis()
        val age = now - timestamp

        val (level, score) = when {
            source.contains("USER", ignoreCase = true) || source.contains("EXPLICIT", ignoreCase = true) -> {
                TruthConfidenceLevel.VERIFIED to 1.0f
            }
            age > freshnessWindowMs -> {
                TruthConfidenceLevel.STALE to (statedConfidence * 0.4f).coerceAtMost(0.49f)
            }
            statedConfidence >= 0.90f -> {
                TruthConfidenceLevel.VERIFIED to statedConfidence
            }
            statedConfidence >= 0.50f -> {
                TruthConfidenceLevel.INFERRED to statedConfidence
            }
            else -> {
                TruthConfidenceLevel.UNKNOWN to statedConfidence
            }
        }

        val sourceType = when {
            source.contains("USER", ignoreCase = true) -> TruthSourceType.USER_EXPLICIT
            source.contains("GPS", ignoreCase = true) || source.contains("BATTERY", ignoreCase = true) -> TruthSourceType.HARDWARE_SENSOR
            source.contains("DB", ignoreCase = true) || source.contains("ROOM", ignoreCase = true) -> TruthSourceType.SYSTEM_DATABASE
            source.contains("ROUTINE", ignoreCase = true) -> TruthSourceType.OBSERVED_ROUTINE
            else -> TruthSourceType.HEURISTIC_INFERENCE
        }

        return TruthFact(
            factId = "${category}_${key}_${timestamp}",
            category = category,
            key = key,
            value = value,
            confidenceLevel = level,
            confidenceScore = score,
            sourceType = sourceType,
            sourceDetail = source,
            timestamp = timestamp,
            freshnessWindowMillis = freshnessWindowMs,
            isUserConfirmed = level == TruthConfidenceLevel.VERIFIED,
            verificationProof = "Verified via ${sourceType.displayName} ($source)"
        )
    }

    fun formatTruthAudit(facts: List<TruthFact>): String {
        val verified = facts.filter { it.confidenceLevel == TruthConfidenceLevel.VERIFIED }
        val inferred = facts.filter { it.confidenceLevel == TruthConfidenceLevel.INFERRED }
        val stale = facts.filter { it.confidenceLevel == TruthConfidenceLevel.STALE }
        val unknown = facts.filter { it.confidenceLevel == TruthConfidenceLevel.UNKNOWN }

        val sb = StringBuilder("### 🛡️ TRUTH AUDIT SUMMARY\n")
        sb.appendLine("• **Verified Facts (High Certainty)**: ${verified.size}")
        verified.take(3).forEach { sb.appendLine("  ✓ ${it.key}: ${it.value} [${it.sourceDetail}]") }

        if (inferred.isNotEmpty()) {
            sb.appendLine("• **Inferred Facts (Pattern/Observation)**: ${inferred.size}")
            inferred.take(2).forEach { sb.appendLine("  ~ ${it.key}: ${it.value} (${(it.confidenceScore * 100).toInt()}% conf)") }
        }

        if (stale.isNotEmpty()) {
            sb.appendLine("• **Stale Facts (Needs Re-validation)**: ${stale.size}")
            stale.take(2).forEach { sb.appendLine("  ! ${it.key}: Needs update") }
        }

        if (unknown.isNotEmpty()) {
            sb.appendLine("• **Unknown / Unset Attributes**: ${unknown.size}")
        }

        return sb.toString().trim()
    }
}
