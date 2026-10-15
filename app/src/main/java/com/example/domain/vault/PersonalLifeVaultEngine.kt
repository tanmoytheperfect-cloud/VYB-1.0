package com.example.domain.vault

import com.example.data.local.dao.LifeVaultDao
import com.example.data.local.entity.LifeVaultItemEntity

/**
 * Personal Life Vault Categories
 */
object VaultCategory {
    const val ME = "ME"
    const val FAMILY = "FAMILY"
    const val PLACES = "PLACES"
    const val WORK = "WORK"
    const val FINANCE = "FINANCE"
    const val HEALTH_FITNESS = "HEALTH_FITNESS"
    const val IMPORTANT_DATES = "IMPORTANT_DATES"
    const val MEMORIES = "MEMORIES"

    val ALL_CATEGORIES = listOf(
        ME,
        FAMILY,
        PLACES,
        WORK,
        FINANCE,
        HEALTH_FITNESS,
        IMPORTANT_DATES,
        MEMORIES
    )
}

/**
 * Personal Life Vault Sources
 */
object VaultSource {
    const val USER_PROVIDED = "USER_PROVIDED"
    const val USER_CONFIRMED = "USER_CONFIRMED"
    const val CONNECTED = "CONNECTED"
    const val LEARNED = "LEARNED"
    const val UNKNOWN = "UNKNOWN"
}

/**
 * Result of attempting to parse and store or update a vault fact from conversation.
 */
sealed class VaultOperationResult {
    data class Stored(val item: LifeVaultItemEntity, val isNew: Boolean, val message: String) : VaultOperationResult()
    data class ConflictDetected(
        val existingItem: LifeVaultItemEntity,
        val proposedValue: String,
        val proposedCategory: String,
        val proposedKey: String,
        val promptText: String
    ) : VaultOperationResult()
    data class Deleted(val deletedCount: Int, val targetKey: String, val message: String) : VaultOperationResult()
    data class AskConfirmation(val proposedCategory: String, val proposedKey: String, val proposedValue: String, val promptText: String) : VaultOperationResult()
    data object NoOperation : VaultOperationResult()
}

/**
 * Structured snapshot for Gemini & AI Brain Context
 */
data class VaultContextSnapshot(
    val meFacts: List<LifeVaultItemEntity> = emptyList(),
    val familyFacts: List<LifeVaultItemEntity> = emptyList(),
    val placesFacts: List<LifeVaultItemEntity> = emptyList(),
    val workFacts: List<LifeVaultItemEntity> = emptyList(),
    val financeFacts: List<LifeVaultItemEntity> = emptyList(),
    val healthFacts: List<LifeVaultItemEntity> = emptyList(),
    val datesFacts: List<LifeVaultItemEntity> = emptyList(),
    val memoryFacts: List<LifeVaultItemEntity> = emptyList(),
    val totalFactCount: Int = 0
)

class PersonalLifeVaultEngine(
    private val lifeVaultDao: LifeVaultDao
) {

    /**
     * Parses natural language input to detect if the user is providing, modifying, or forgetting a personal fact.
     */
    suspend fun processConversationalMemory(input: String): VaultOperationResult {
        val trimmed = input.trim()
        val lower = trimmed.lowercase()

        // 1. Check for "Forget" commands
        if (lower.startsWith("forget ") || lower.contains("forget my ") || lower.contains("delete from memory") || lower.contains("remove from vault")) {
            return handleForgetCommand(trimmed, lower)
        }

        // 2. Check for explicit or natural "Remember" / personal fact statements
        val isExplicitRemember = lower.startsWith("remember that ") || lower.startsWith("remember ") || lower.startsWith("save to memory ") || lower.startsWith("save to vault ")
        val isPersonalStatement = lower.startsWith("my ") || lower.startsWith("i work at ") || lower.startsWith("i live in ") || lower.startsWith("my son's ") || lower.startsWith("my wife's ") || lower.startsWith("my salary is ") || lower.startsWith("my income is ") || lower.startsWith("my rent is ") || lower.startsWith("my budget is ")

        if (!isExplicitRemember && !isPersonalStatement) {
            return VaultOperationResult.NoOperation
        }

        val extracted = extractFactFromText(trimmed, isExplicitRemember)
            ?: return VaultOperationResult.NoOperation

        // Check for conflicts with existing keys in the vault (case-insensitive and alias-aware)
        val allItems = lifeVaultDao.getAllVaultItems()
        val existing = allItems.find { it.category.equals(extracted.category, true) && (it.key.equals(extracted.key, true) || it.key.contains(extracted.key, true) || extracted.key.contains(it.key, true)) }
            ?: allItems.find { it.key.equals(extracted.key, true) || it.key.contains(extracted.key, true) || extracted.key.contains(it.key, true) }

        if (existing != null) {
            // If identical value, nothing to change
            if (existing.value.equals(extracted.value, ignoreCase = true)) {
                return VaultOperationResult.Stored(existing, isNew = false, message = "I already have this recorded in your Personal Life Vault.")
            }

            // Conflict detected!
            val prompt = "I already have ${existing.key} saved as \"${existing.value}\". Do you want me to replace it with \"${extracted.value}\"?"
            // Mark existing as pending conflict
            val updatedExisting = existing.copy(
                isPendingConflict = true,
                conflictingValue = extracted.value,
                updatedAt = System.currentTimeMillis()
            )
            lifeVaultDao.updateVaultItem(updatedExisting)

            return VaultOperationResult.ConflictDetected(
                existingItem = existing,
                proposedValue = extracted.value,
                proposedCategory = extracted.category,
                proposedKey = extracted.key,
                promptText = prompt
            )
        }

        // If not explicit "remember", but a clear personal fact:
        val itemToInsert = LifeVaultItemEntity(
            category = extracted.category,
            key = extracted.key,
            value = extracted.value,
            source = VaultSource.USER_PROVIDED,
            sourceDetail = "Natural conversation: \"$trimmed\"",
            numericValue = extracted.numericAmount,
            isConfirmed = true,
            isPendingConflict = false,
            timestamp = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val id = lifeVaultDao.insertVaultItem(itemToInsert)
        val saved = itemToInsert.copy(id = id)
        return VaultOperationResult.Stored(
            item = saved,
            isNew = true,
            message = "Saved to Personal Life Vault [${extracted.category}]: ${extracted.key} = ${extracted.value} (Source: USER_PROVIDED)"
        )
    }

    /**
     * Handles forgetting / deleting facts.
     */
    private suspend fun handleForgetCommand(raw: String, lower: String): VaultOperationResult {
        val targetQuery = when {
            lower.contains("forget my old office address") || lower.contains("forget my office address") || lower.contains("forget office") -> "Office Address"
            lower.contains("forget my home address") || lower.contains("forget home") -> "Home Address"
            lower.contains("forget my salary") || lower.contains("forget my income") -> "Monthly Income"
            lower.contains("forget my son's school") || lower.contains("forget school") -> "Son's School"
            lower.contains("forget my gym") -> "Gym / Fitness"
            lower.contains("forget my diet") -> "Diet Preference"
            lower.startsWith("forget that ") -> raw.substringAfter("forget that ").trim()
            lower.startsWith("forget my ") -> raw.substringAfter("forget my ").trim()
            lower.startsWith("forget ") -> raw.substringAfter("forget ").trim()
            else -> raw
        }

        val allItems = lifeVaultDao.getAllVaultItems()
        val matched = allItems.filter {
            it.key.contains(targetQuery, ignoreCase = true) ||
            it.value.contains(targetQuery, ignoreCase = true) ||
            targetQuery.contains(it.key, ignoreCase = true)
        }

        if (matched.isNotEmpty()) {
            matched.forEach { lifeVaultDao.deleteVaultItemById(it.id) }
            return VaultOperationResult.Deleted(
                deletedCount = matched.size,
                targetKey = targetQuery,
                message = "Removed ${matched.size} fact(s) matching \"$targetQuery\" from your Personal Life Vault."
            )
        }

        return VaultOperationResult.Deleted(
            deletedCount = 0,
            targetKey = targetQuery,
            message = "No matching facts found in your Personal Life Vault for \"$targetQuery\"."
        )
    }

    /**
     * Resolves an existing conflict.
     */
    suspend fun resolveConflict(itemId: Long, acceptNew: Boolean): LifeVaultItemEntity? {
        val all = lifeVaultDao.getAllVaultItems()
        val existing = all.find { it.id == itemId } ?: return null

        return if (acceptNew && existing.conflictingValue != null) {
            val updated = existing.copy(
                previousValue = existing.value,
                value = existing.conflictingValue,
                source = VaultSource.USER_CONFIRMED,
                sourceDetail = "Updated by user confirmation after conflict resolution",
                isPendingConflict = false,
                conflictingValue = null,
                isConfirmed = true,
                updatedAt = System.currentTimeMillis()
            )
            lifeVaultDao.updateVaultItem(updated)
            updated
        } else {
            val updated = existing.copy(
                isPendingConflict = false,
                conflictingValue = null,
                updatedAt = System.currentTimeMillis()
            )
            lifeVaultDao.updateVaultItem(updated)
            updated
        }
    }

    /**
     * Compiles a genuine snapshot of everything the vault currently knows.
     * Missing values are NEVER fabricated.
     */
    suspend fun getVaultSnapshot(): VaultContextSnapshot {
        val all = lifeVaultDao.getAllVaultItems()
        return VaultContextSnapshot(
            meFacts = all.filter { it.category == VaultCategory.ME },
            familyFacts = all.filter { it.category == VaultCategory.FAMILY },
            placesFacts = all.filter { it.category == VaultCategory.PLACES },
            workFacts = all.filter { it.category == VaultCategory.WORK },
            financeFacts = all.filter { it.category == VaultCategory.FINANCE },
            healthFacts = all.filter { it.category == VaultCategory.HEALTH_FITNESS },
            datesFacts = all.filter { it.category == VaultCategory.IMPORTANT_DATES },
            memoryFacts = all.filter { it.category == VaultCategory.MEMORIES },
            totalFactCount = all.size
        )
    }

    /**
     * Generates honest contextual text for Gemini prompt injection.
     * Explictly marks empty sections as UNKNOWN / UNCONFIGURED.
     */
    suspend fun buildGeminiContextPrompt(): String {
        val snapshot = getVaultSnapshot()
        val sb = StringBuilder()
        sb.appendLine("=== VYB PERSONAL LIFE VAULT (SOURCE-VERIFIED CONTEXT) ===")

        // ME
        if (snapshot.meFacts.isNotEmpty()) {
            sb.appendLine("ME (Owner Profile):")
            snapshot.meFacts.forEach { sb.appendLine("  - ${it.key}: ${it.value} [Source: ${it.source}]") }
        } else {
            sb.appendLine("ME: UNCONFIGURED (Owner has not provided profile details)")
        }

        // FAMILY
        if (snapshot.familyFacts.isNotEmpty()) {
            sb.appendLine("FAMILY:")
            snapshot.familyFacts.forEach { sb.appendLine("  - ${it.key}: ${it.value} [Source: ${it.source}]") }
        } else {
            sb.appendLine("FAMILY: UNKNOWN (No family members configured)")
        }

        // PLACES
        if (snapshot.placesFacts.isNotEmpty()) {
            sb.appendLine("PLACES:")
            snapshot.placesFacts.forEach { sb.appendLine("  - ${it.key}: ${it.value} [Source: ${it.source}]") }
        } else {
            sb.appendLine("PLACES: UNKNOWN (No home, office, or school locations saved)")
        }

        // WORK
        if (snapshot.workFacts.isNotEmpty()) {
            sb.appendLine("WORK:")
            snapshot.workFacts.forEach { sb.appendLine("  - ${it.key}: ${it.value} [Source: ${it.source}]") }
        } else {
            sb.appendLine("WORK: UNKNOWN (No employer or role configured)")
        }

        // FINANCE
        if (snapshot.financeFacts.isNotEmpty()) {
            sb.appendLine("FINANCE:")
            snapshot.financeFacts.forEach {
                val numStr = if (it.numericValue != null) " (Amount: ₹${it.numericValue.toInt()})" else ""
                sb.appendLine("  - ${it.key}: ${it.value}$numStr [Source: ${it.source}]")
            }
        } else {
            sb.appendLine("FINANCE: UNKNOWN (No income, expenses, rent, or savings goals provided by owner. Treat as UNKNOWN, never assume ₹0)")
        }

        // HEALTH/FITNESS
        if (snapshot.healthFacts.isNotEmpty()) {
            sb.appendLine("HEALTH & FITNESS:")
            snapshot.healthFacts.forEach { sb.appendLine("  - ${it.key}: ${it.value} [Source: ${it.source}]") }
        } else {
            sb.appendLine("HEALTH & FITNESS: UNCONFIGURED (No weight, goals, calorie, or protein targets configured. Do NOT invent defaults)")
        }

        // IMPORTANT DATES
        if (snapshot.datesFacts.isNotEmpty()) {
            sb.appendLine("IMPORTANT DATES:")
            snapshot.datesFacts.forEach { sb.appendLine("  - ${it.key}: ${it.value} [Source: ${it.source}]") }
        } else {
            sb.appendLine("IMPORTANT DATES: None configured")
        }

        // MEMORIES
        if (snapshot.memoryFacts.isNotEmpty()) {
            sb.appendLine("MEMORIES & PREFERENCES:")
            snapshot.memoryFacts.forEach { sb.appendLine("  - ${it.key}: ${it.value} [Source: ${it.source}]") }
        }

        sb.appendLine("=== END LIFE VAULT CONTEXT ===")
        return sb.toString()
    }

    private data class ExtractedFact(
        val category: String,
        val key: String,
        val value: String,
        val numericAmount: Double? = null
    )

    private fun extractFactFromText(raw: String, isExplicitRemember: Boolean): ExtractedFact? {
        val clean = if (isExplicitRemember) {
            raw.replaceFirst("(?i)^remember\\s+that\\s+".toRegex(), "")
                .replaceFirst("(?i)^remember\\s+".toRegex(), "")
                .replaceFirst("(?i)^save\\s+to\\s+memory\\s+".toRegex(), "")
                .replaceFirst("(?i)^save\\s+to\\s+vault\\s+".toRegex(), "")
                .trim()
        } else {
            raw.trim()
        }
        val lower = clean.lowercase()

        // 1. Office Location
        if (lower.contains("office is in ") || lower.contains("office address is ") || lower.contains("office is at ") || lower.contains("my office is ") || lower.contains("office is now ")) {
            val value = clean.substringAfter("is now ", clean.substringAfter("is at ", clean.substringAfter("is in ", clean.substringAfter("is ", "")))).trim().removeSuffix(".")
            if (value.isNotBlank()) {
                return ExtractedFact(VaultCategory.PLACES, "Office Address", value)
            }
        }

        // 2. Home Location
        if (lower.contains("home is in ") || lower.contains("home address is ") || lower.contains("home is at ") || lower.contains("my home is ") || lower.contains("home is now ")) {
            val value = clean.substringAfter("is now ", clean.substringAfter("is at ", clean.substringAfter("is in ", clean.substringAfter("is ", "")))).trim().removeSuffix(".")
            if (value.isNotBlank()) {
                return ExtractedFact(VaultCategory.PLACES, "Home Address", value)
            }
        }

        // 3. Son's School
        if (lower.contains("son's school is ") || lower.contains("son school is ") || lower.contains("son goes to ")) {
            val value = clean.substringAfter("is ", clean.substringAfter("goes to ", "")).trim().removeSuffix(".")
            if (value.isNotBlank()) {
                return ExtractedFact(VaultCategory.PLACES, "Son's School", value)
            }
        }

        // 4. Financial - Salary / Income
        if (lower.contains("salary is ") || lower.contains("income is ") || lower.contains("earn ") || lower.contains("monthly salary is ")) {
            val value = clean.substringAfter("is ", clean.substringAfter("earn ", "")).trim().removeSuffix(".")
            val num = extractNumericAmount(value)
            return ExtractedFact(VaultCategory.FINANCE, "Monthly Salary", value, num)
        }

        // 5. Financial - Rent
        if (lower.contains("rent is ") || lower.contains("house rent is ")) {
            val value = clean.substringAfter("is ", "").trim().removeSuffix(".")
            val num = extractNumericAmount(value)
            return ExtractedFact(VaultCategory.FINANCE, "Monthly Rent", value, num)
        }

        // 6. Financial - Savings Goal
        if (lower.contains("savings target is ") || lower.contains("savings goal is ") || lower.contains("save ₹") || lower.contains("save ")) {
            val value = clean.substringAfter("is ", clean.substringAfter("save ", "")).trim().removeSuffix(".")
            val num = extractNumericAmount(value)
            return ExtractedFact(VaultCategory.FINANCE, "Monthly Savings Target", value, num)
        }

        // 7. Work / Role / Company
        if (lower.contains("i work at ") || lower.contains("company is ") || lower.contains("employer is ")) {
            val value = clean.substringAfter("work at ", clean.substringAfter("is ", "")).trim().removeSuffix(".")
            return ExtractedFact(VaultCategory.WORK, "Company", value)
        }
        if (lower.contains("job role is ") || lower.contains("role is ") || lower.contains("designation is ") || lower.contains("i work as ")) {
            val value = clean.substringAfter("as ", clean.substringAfter("is ", "")).trim().removeSuffix(".")
            return ExtractedFact(VaultCategory.WORK, "Job Role", value)
        }

        // 8. Family
        if (lower.contains("wife's name is ") || lower.contains("wife is ")) {
            val value = clean.substringAfter("is ", "").trim().removeSuffix(".")
            return ExtractedFact(VaultCategory.FAMILY, "Wife", value)
        }
        if (lower.contains("son's name is ") || lower.contains("son is ")) {
            val value = clean.substringAfter("is ", "").trim().removeSuffix(".")
            return ExtractedFact(VaultCategory.FAMILY, "Son", value)
        }

        // 9. Important Dates / Birthdays / Anniversaries
        if (lower.contains("birthday is on ") || lower.contains("birthday is ")) {
            val subject = when {
                lower.contains("wife's birthday") -> "Wife's Birthday"
                lower.contains("son's birthday") -> "Son's Birthday"
                lower.contains("my birthday") -> "My Birthday"
                else -> "Birthday"
            }
            val value = clean.substringAfter("is on ", clean.substringAfter("is ", "")).trim().removeSuffix(".")
            return ExtractedFact(VaultCategory.IMPORTANT_DATES, subject, value)
        }
        if (lower.contains("anniversary is on ") || lower.contains("anniversary is ")) {
            val value = clean.substringAfter("is on ", clean.substringAfter("is ", "")).trim().removeSuffix(".")
            return ExtractedFact(VaultCategory.IMPORTANT_DATES, "Anniversary", value)
        }

        // 10. Health / Fitness (Only explicit)
        if (lower.contains("diet is ") || lower.contains("diet preference is ") || lower.contains("allergic to ") || lower.contains("allergy is ")) {
            val key = if (lower.contains("allergic") || lower.contains("allergy")) "Allergies" else "Diet Preference"
            val value = clean.substringAfter("to ", clean.substringAfter("is ", "")).trim().removeSuffix(".")
            return ExtractedFact(VaultCategory.HEALTH_FITNESS, key, value)
        }

        // 11. Generic explicit remember
        if (isExplicitRemember) {
            val category = when {
                lower.contains("prefer") || lower.contains("like") || lower.contains("cheap") || lower.contains("budget") -> VaultCategory.MEMORIES
                lower.contains("office") || lower.contains("work") -> VaultCategory.WORK
                lower.contains("school") || lower.contains("son") || lower.contains("wife") || lower.contains("family") -> VaultCategory.FAMILY
                else -> VaultCategory.MEMORIES
            }
            val key = when {
                lower.contains("cheap") || lower.contains("cheaper") -> "Transport & Spend Preference"
                lower.contains("prefer") -> "User Preference"
                clean.contains(" is ") -> clean.substringBefore(" is ").trim()
                else -> "Note: ${clean.take(30)}"
            }
            val value = if (clean.contains(" is ")) clean.substringAfter(" is ").trim().removeSuffix(".") else clean
            return ExtractedFact(category, key, value)
        }

        return null
    }

    private fun extractNumericAmount(text: String): Double? {
        val clean = text.replace(",", "").replace("₹", "").replace("rs", "", true).trim()
        val match = "([0-9]+(?:\\.[0-9]+)?)".toRegex().find(clean)
        val base = match?.groupValues?.get(1)?.toDoubleOrNull() ?: return null

        return when {
            text.contains("lakh", true) || text.contains("lac", true) || text.contains(" l", true) -> base * 100000.0
            text.contains("k", true) -> base * 1000.0
            text.contains("crore", true) || text.contains("cr", true) -> base * 10000000.0
            else -> base
        }
    }
}
