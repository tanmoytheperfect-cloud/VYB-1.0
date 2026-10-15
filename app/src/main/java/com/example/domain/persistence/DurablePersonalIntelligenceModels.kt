package com.example.domain.persistence

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/**
 * Explicit Truth States for every recalled fact and personal intelligence node.
 */
object TruthState {
    const val VERIFIED = "VERIFIED"
    const val USER_CONFIRMED = "USER_CONFIRMED"
    const val INFERRED = "INFERRED"
    const val OBSERVED = "OBSERVED"
    const val STALE = "STALE"
    const val UNKNOWN = "UNKNOWN"
    const val UNAVAILABLE = "UNAVAILABLE"
    const val REQUIRES_PERMISSION = "REQUIRES_PERMISSION"
    const val LOW_CONFIDENCE = "LOW_CONFIDENCE"
}

/**
 * Cloud Synchronization States.
 */
enum class CloudSyncStatus {
    UNCONFIGURED,
    SYNCED,
    LOCAL_DURABLE_ONLY,
    PENDING_UPLOAD,
    PENDING_DOWNLOAD,
    SYNCING,
    RESTORE_IN_PROGRESS,
    FAILED_OFFLINE,
    CONFLICT_RESOLVED
}

/**
 * Globally Stable Vault Fact.
 * Does NOT depend on SQLite auto-increment primary keys.
 */
data class DurableVaultFact(
    val globalId: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val category: String,
    val key: String,
    val value: String,
    val numericValue: Double? = null,
    val source: String = "USER_PROVIDED",
    val sourceDetail: String? = null,
    val truthState: String = TruthState.VERIFIED,
    val confidence: Double = 1.0,
    val version: Long = 1L,
    val isConfirmed: Boolean = true,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("globalId", globalId)
        put("ownerId", ownerId)
        put("category", category)
        put("key", key)
        put("value", value)
        if (numericValue != null) put("numericValue", numericValue)
        put("source", source)
        if (sourceDetail != null) put("sourceDetail", sourceDetail)
        put("truthState", truthState)
        put("confidence", confidence)
        put("version", version)
        put("isConfirmed", isConfirmed)
        put("isDeleted", isDeleted)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): DurableVaultFact = DurableVaultFact(
            globalId = json.optString("globalId", UUID.randomUUID().toString()),
            ownerId = json.optString("ownerId", ""),
            category = json.optString("category", "ME"),
            key = json.optString("key", ""),
            value = json.optString("value", ""),
            numericValue = if (json.has("numericValue")) json.optDouble("numericValue") else null,
            source = json.optString("source", "USER_PROVIDED"),
            sourceDetail = if (json.has("sourceDetail")) json.optString("sourceDetail") else null,
            truthState = json.optString("truthState", TruthState.VERIFIED),
            confidence = json.optDouble("confidence", 1.0),
            version = json.optLong("version", 1L),
            isConfirmed = json.optBoolean("isConfirmed", true),
            isDeleted = json.optBoolean("isDeleted", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

/**
 * Globally Stable Conversation-Derived Memory.
 */
data class DurableMemory(
    val globalId: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val category: String,
    val key: String,
    val value: String,
    val source: String = "User Conversation",
    val truthState: String = TruthState.USER_CONFIRMED,
    val confidence: Double = 0.95,
    val version: Long = 1L,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("globalId", globalId)
        put("ownerId", ownerId)
        put("category", category)
        put("key", key)
        put("value", value)
        put("source", source)
        put("truthState", truthState)
        put("confidence", confidence)
        put("version", version)
        put("isDeleted", isDeleted)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): DurableMemory = DurableMemory(
            globalId = json.optString("globalId", UUID.randomUUID().toString()),
            ownerId = json.optString("ownerId", ""),
            category = json.optString("category", "PREFERENCE"),
            key = json.optString("key", ""),
            value = json.optString("value", ""),
            source = json.optString("source", "User Conversation"),
            truthState = json.optString("truthState", TruthState.USER_CONFIRMED),
            confidence = json.optDouble("confidence", 0.95),
            version = json.optLong("version", 1L),
            isDeleted = json.optBoolean("isDeleted", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

/**
 * Globally Stable Persistent Intention.
 */
data class DurableIntention(
    val globalId: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val intentionId: String,
    val title: String,
    val targetAction: String,
    val scheduledEpochMs: Long,
    val recurrenceRule: String,
    val truthState: String = TruthState.VERIFIED,
    val state: String = "SCHEDULED",
    val version: Long = 1L,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("globalId", globalId)
        put("ownerId", ownerId)
        put("intentionId", intentionId)
        put("title", title)
        put("targetAction", targetAction)
        put("scheduledEpochMs", scheduledEpochMs)
        put("recurrenceRule", recurrenceRule)
        put("truthState", truthState)
        put("state", state)
        put("version", version)
        put("isDeleted", isDeleted)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): DurableIntention = DurableIntention(
            globalId = json.optString("globalId", UUID.randomUUID().toString()),
            ownerId = json.optString("ownerId", ""),
            intentionId = json.optString("intentionId", ""),
            title = json.optString("title", ""),
            targetAction = json.optString("targetAction", ""),
            scheduledEpochMs = json.optLong("scheduledEpochMs", 0L),
            recurrenceRule = json.optString("recurrenceRule", "NONE"),
            truthState = json.optString("truthState", TruthState.VERIFIED),
            state = json.optString("state", "SCHEDULED"),
            version = json.optLong("version", 1L),
            isDeleted = json.optBoolean("isDeleted", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

/**
 * Globally Stable Routine.
 */
data class DurableRoutine(
    val globalId: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val title: String,
    val category: String,
    val frequency: String,
    val triggerTime: String,
    val actionSummary: String,
    val truthState: String = TruthState.USER_CONFIRMED,
    val version: Long = 1L,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("globalId", globalId)
        put("ownerId", ownerId)
        put("title", title)
        put("category", category)
        put("frequency", frequency)
        put("triggerTime", triggerTime)
        put("actionSummary", actionSummary)
        put("truthState", truthState)
        put("version", version)
        put("isDeleted", isDeleted)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): DurableRoutine = DurableRoutine(
            globalId = json.optString("globalId", UUID.randomUUID().toString()),
            ownerId = json.optString("ownerId", ""),
            title = json.optString("title", ""),
            category = json.optString("category", "COMMUTE"),
            frequency = json.optString("frequency", "DAILY"),
            triggerTime = json.optString("triggerTime", "08:30"),
            actionSummary = json.optString("actionSummary", ""),
            truthState = json.optString("truthState", TruthState.USER_CONFIRMED),
            version = json.optLong("version", 1L),
            isDeleted = json.optBoolean("isDeleted", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

/**
 * Globally Stable Goal.
 */
data class DurableGoal(
    val globalId: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val title: String,
    val category: String,
    val targetValue: Double,
    val currentValue: Double,
    val unit: String,
    val truthState: String = TruthState.USER_CONFIRMED,
    val version: Long = 1L,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("globalId", globalId)
        put("ownerId", ownerId)
        put("title", title)
        put("category", category)
        put("targetValue", targetValue)
        put("currentValue", currentValue)
        put("unit", unit)
        put("truthState", truthState)
        put("version", version)
        put("isDeleted", isDeleted)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): DurableGoal = DurableGoal(
            globalId = json.optString("globalId", UUID.randomUUID().toString()),
            ownerId = json.optString("ownerId", ""),
            title = json.optString("title", ""),
            category = json.optString("category", "FITNESS"),
            targetValue = json.optDouble("targetValue", 0.0),
            currentValue = json.optDouble("currentValue", 0.0),
            unit = json.optString("unit", ""),
            truthState = json.optString("truthState", TruthState.USER_CONFIRMED),
            version = json.optLong("version", 1L),
            isDeleted = json.optBoolean("isDeleted", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

/**
 * Globally Stable Family Member.
 */
data class DurableFamilyMember(
    val globalId: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val name: String,
    val relationship: String,
    val phone: String = "",
    val routineSummary: String = "",
    val notes: String = "",
    val truthState: String = TruthState.VERIFIED,
    val version: Long = 1L,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("globalId", globalId)
        put("ownerId", ownerId)
        put("name", name)
        put("relationship", relationship)
        put("phone", phone)
        put("routineSummary", routineSummary)
        put("notes", notes)
        put("truthState", truthState)
        put("version", version)
        put("isDeleted", isDeleted)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): DurableFamilyMember = DurableFamilyMember(
            globalId = json.optString("globalId", UUID.randomUUID().toString()),
            ownerId = json.optString("ownerId", ""),
            name = json.optString("name", ""),
            relationship = json.optString("relationship", ""),
            phone = json.optString("phone", ""),
            routineSummary = json.optString("routineSummary", ""),
            notes = json.optString("notes", ""),
            truthState = json.optString("truthState", TruthState.VERIFIED),
            version = json.optLong("version", 1L),
            isDeleted = json.optBoolean("isDeleted", false),
            createdAt = json.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

/**
 * Globally Stable Owner Profile State.
 */
data class DurableOwnerProfile(
    val ownerId: String,
    val name: String = "",
    val callingTitle: String = "Sir",
    val email: String = "",
    val phone: String = "",
    val companyName: String = "",
    val jobTitle: String = "",
    val homeAddress: String = "",
    val officeAddress: String = "",
    val monthlyIncome: Double = 0.0,
    val fixedCommitments: Double = 0.0,
    val savingsTarget: Double = 0.0,
    val currencySymbol: String = "₹",
    val dietaryPreference: String = "High-Protein Balanced",
    val aiTone: String = "EXECUTIVE_PA",
    val version: Long = 1L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("ownerId", ownerId)
        put("name", name)
        put("callingTitle", callingTitle)
        put("email", email)
        put("phone", phone)
        put("companyName", companyName)
        put("jobTitle", jobTitle)
        put("homeAddress", homeAddress)
        put("officeAddress", officeAddress)
        put("monthlyIncome", monthlyIncome)
        put("fixedCommitments", fixedCommitments)
        put("savingsTarget", savingsTarget)
        put("currencySymbol", currencySymbol)
        put("dietaryPreference", dietaryPreference)
        put("aiTone", aiTone)
        put("version", version)
        put("updatedAt", updatedAt)
    }

    companion object {
        fun fromJson(json: JSONObject): DurableOwnerProfile = DurableOwnerProfile(
            ownerId = json.optString("ownerId", ""),
            name = json.optString("name", ""),
            callingTitle = json.optString("callingTitle", "Sir"),
            email = json.optString("email", ""),
            phone = json.optString("phone", ""),
            companyName = json.optString("companyName", ""),
            jobTitle = json.optString("jobTitle", ""),
            homeAddress = json.optString("homeAddress", ""),
            officeAddress = json.optString("officeAddress", ""),
            monthlyIncome = json.optDouble("monthlyIncome", 0.0),
            fixedCommitments = json.optDouble("fixedCommitments", 0.0),
            savingsTarget = json.optDouble("savingsTarget", 0.0),
            currencySymbol = json.optString("currencySymbol", "₹"),
            dietaryPreference = json.optString("dietaryPreference", "High-Protein Balanced"),
            aiTone = json.optString("aiTone", "EXECUTIVE_PA"),
            version = json.optLong("version", 1L),
            updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
        )
    }
}

/**
 * Versioned, Durable Personal Intelligence State.
 * The Canonical Owner Vault Snapshot that survives reinstalls, updates, and device replacements.
 */
data class DurablePersonalIntelligenceState(
    val ownerId: String,
    val stateVersion: Int = CURRENT_SCHEMA_VERSION,
    val deviceSource: String = "ANDROID_PRIMARY",
    val profile: DurableOwnerProfile,
    val vaultFacts: List<DurableVaultFact> = emptyList(),
    val memories: List<DurableMemory> = emptyList(),
    val intentions: List<DurableIntention> = emptyList(),
    val routines: List<DurableRoutine> = emptyList(),
    val goals: List<DurableGoal> = emptyList(),
    val familyMembers: List<DurableFamilyMember> = emptyList(),
    val checksum: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun calculateChecksum(content: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(content.toByteArray(Charsets.UTF_8))
            return hash.joinToString("") { "%02x".format(it) }
        }
    }

    fun toJsonString(): String {
        val json = JSONObject().apply {
            put("ownerId", ownerId)
            put("stateVersion", stateVersion)
            put("deviceSource", deviceSource)
            put("profile", profile.toJson())

            val factsArray = JSONArray()
            vaultFacts.forEach { factsArray.put(it.toJson()) }
            put("vaultFacts", factsArray)

            val memoriesArray = JSONArray()
            memories.forEach { memoriesArray.put(it.toJson()) }
            put("memories", memoriesArray)

            val intentionsArray = JSONArray()
            intentions.forEach { intentionsArray.put(it.toJson()) }
            put("intentions", intentionsArray)

            val routinesArray = JSONArray()
            routines.forEach { routinesArray.put(it.toJson()) }
            put("routines", routinesArray)

            val goalsArray = JSONArray()
            goals.forEach { goalsArray.put(it.toJson()) }
            put("goals", goalsArray)

            val familyArray = JSONArray()
            familyMembers.forEach { familyArray.put(it.toJson()) }
            put("familyMembers", familyArray)

            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
        val raw = json.toString()
        val calculatedCheck = calculateChecksum(raw)
        json.put("checksum", calculatedCheck)
        return json.toString()
    }

    fun validateIntegrity(): Boolean {
        if (ownerId.isBlank()) return false
        if (checksum.isBlank()) return true // unchecksummed
        val clone = JSONObject(toJsonString())
        clone.remove("checksum")
        val recomputed = calculateChecksum(clone.toString())
        return checksum.equals(recomputed, ignoreCase = true)
    }
}
