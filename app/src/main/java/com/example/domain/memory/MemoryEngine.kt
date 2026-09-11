package com.example.domain.memory

import com.example.data.local.dao.LifeVaultDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.entity.LifeVaultItemEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.domain.brain.KnowledgeState
import com.example.domain.brain.PersonalLifeModel

/**
 * The 9 Core Memory Tiers in VYB.
 */
enum class MemoryTier(val displayName: String, val description: String) {
    EPISODIC("Episodic Memory", "What happened? Chronological events, actions, and interactions"),
    SEMANTIC("Semantic Memory", "What is true about the owner? Verified facts about life, family, and home"),
    PREFERENCE("Preference Memory", "What does the owner like/dislike? Choices, styles, and providers"),
    ROUTINE("Routine Memory", "What normally happens? Recurring schedules and typical daily rhythm"),
    DECISION("Decision Memory", "What did the owner decide? Past resolutions, agreements, and deferred choices"),
    RELATIONSHIP("Relationship Memory", "Who is important and why? Family connections, colleagues, and contacts"),
    GOAL("Goal Memory", "What is the owner trying to achieve? Target milestones, financial buffers, projects"),
    CONTEXT("Context Memory", "What is happening currently? Active topic, recent messages, and present hour"),
    PATTERN("Pattern Memory", "What does VYB observe repeatedly? Inferred behavioural tendencies")
}

/**
 * Intelligent Memory Unit with strict verification, provenance, and permission tracking.
 */
data class IntelligentMemoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val tier: MemoryTier,
    val subject: String,
    val content: String,
    val knowledgeState: KnowledgeState = KnowledgeState.KNOWN,
    val confidence: Float = 1.0f,
    val source: String = "USER_CONVERSATION",
    val timestamp: Long = System.currentTimeMillis(),
    val lastVerifiedTime: Long = System.currentTimeMillis(),
    val isUserEditable: Boolean = true,
    val isPaused: Boolean = false
)

/**
 * Intelligent Memory Engine.
 * Manages the 9 memory tiers, transparent learning, corrections, forgetting, and recall.
 */
class MemoryEngine(
    private val memoryDao: MemoryDao? = null,
    private val lifeVaultDao: LifeVaultDao? = null,
    private var lifeModel: PersonalLifeModel = PersonalLifeModel.createDefault()
) {
    // In-memory active stores for fast contextual recall
    private val inMemoryItems = mutableListOf<IntelligentMemoryItem>()

    init {
        seedDefaultMemoryBase()
    }

    fun updateLifeModel(newModel: PersonalLifeModel) {
        this.lifeModel = newModel
    }

    private fun seedDefaultMemoryBase() {
        // SEMANTIC
        inMemoryItems.add(
            IntelligentMemoryItem(
                tier = MemoryTier.SEMANTIC,
                subject = "Residence",
                content = "Tanmoy lives in Salt Lake, Kolkata.",
                knowledgeState = KnowledgeState.KNOWN,
                confidence = 1.0f,
                source = "USER_PROFILE"
            )
        )
        inMemoryItems.add(
            IntelligentMemoryItem(
                tier = MemoryTier.SEMANTIC,
                subject = "Role",
                content = "Tanmoy works as Director at Tech Studio.",
                knowledgeState = KnowledgeState.KNOWN,
                confidence = 1.0f,
                source = "USER_PROFILE"
            )
        )

        // RELATIONSHIP
        inMemoryItems.add(
            IntelligentMemoryItem(
                tier = MemoryTier.RELATIONSHIP,
                subject = "Son",
                content = "Son Vivaan attends Delhi Public School. Daily school routine requires transport coordination.",
                knowledgeState = KnowledgeState.KNOWN,
                confidence = 1.0f,
                source = "USER_PROVIDED"
            )
        )

        // PREFERENCE
        inMemoryItems.add(
            IntelligentMemoryItem(
                tier = MemoryTier.PREFERENCE,
                subject = "Cab Provider",
                content = "Prefers Yatri Sathi (regulated metered network) and Rapido/Uber for city and school travel.",
                knowledgeState = KnowledgeState.KNOWN,
                confidence = 0.95f,
                source = "USER_PROVIDED"
            )
        )

        // ROUTINE
        inMemoryItems.add(
            IntelligentMemoryItem(
                tier = MemoryTier.ROUTINE,
                subject = "School Commute Routine",
                content = "School pickup routine at 10:30 AM / 2:30 PM on weekdays.",
                knowledgeState = KnowledgeState.KNOWN,
                confidence = 1.0f,
                source = "OBSERVED_ROUTINE"
            )
        )

        // DECISION
        inMemoryItems.add(
            IntelligentMemoryItem(
                tier = MemoryTier.DECISION,
                subject = "Royal Enfield Classic 350",
                content = "Decided to postpone purchasing the Classic 350 to next quarter to protect the ₹1,50,000 savings buffer.",
                knowledgeState = KnowledgeState.KNOWN,
                confidence = 1.0f,
                source = "EXPLICIT_DECISION"
            )
        )

        // GOAL
        inMemoryItems.add(
            IntelligentMemoryItem(
                tier = MemoryTier.GOAL,
                subject = "Emergency Savings Buffer",
                content = "Targeting ₹1,50,000 emergency liquid savings buffer; monthly savings target is ₹35,000.",
                knowledgeState = KnowledgeState.KNOWN,
                confidence = 1.0f,
                source = "FINANCIAL_PLAN"
            )
        )
        inMemoryItems.add(
            IntelligentMemoryItem(
                tier = MemoryTier.GOAL,
                subject = "Family Vacation",
                content = "Planning family vacation to Darjeeling next month with estimated budget ₹35,000.",
                knowledgeState = KnowledgeState.KNOWN,
                confidence = 0.9f,
                source = "USER_STATED"
            )
        )

        // PATTERN
        inMemoryItems.add(
            IntelligentMemoryItem(
                tier = MemoryTier.PATTERN,
                subject = "Morning Check Rhythm",
                content = "Frequently checks priorities and commute options around 8:00 AM - 8:30 AM.",
                knowledgeState = KnowledgeState.INFERRED,
                confidence = 0.85f,
                source = "PATTERN_OBSERVER"
            )
        )
    }

    /**
     * Retrieves memories filtered by tier.
     */
    fun getMemoriesByTier(tier: MemoryTier): List<IntelligentMemoryItem> {
        return inMemoryItems.filter { it.tier == tier && !it.isPaused }
    }

    /**
     * Semantic query across all memory tiers.
     */
    fun queryMemories(query: String, maxResults: Int = 6): List<IntelligentMemoryItem> {
        val tokens = query.lowercase().split(" ", ",", "?", ".", "!").filter { it.length > 2 }
        if (tokens.isEmpty()) return inMemoryItems.take(maxResults)

        return inMemoryItems
            .filter { !it.isPaused }
            .map { item ->
                val text = (item.subject + " " + item.content).lowercase()
                val score = tokens.count { token -> text.contains(token) }
                Pair(item, score)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(maxResults)
    }

    /**
     * Stores an explicit memory item or updates existing if matching subject.
     */
    suspend fun storeMemory(
        tier: MemoryTier,
        subject: String,
        content: String,
        source: String = "USER_CONVERSATION",
        confidence: Float = 1.0f,
        knowledgeState: KnowledgeState = KnowledgeState.KNOWN
    ): IntelligentMemoryItem {
        // Remove existing duplicate subject in tier if present
        inMemoryItems.removeAll { it.tier == tier && it.subject.equals(subject, ignoreCase = true) }

        val item = IntelligentMemoryItem(
            tier = tier,
            subject = subject,
            content = content,
            knowledgeState = knowledgeState,
            confidence = confidence,
            source = source
        )
        inMemoryItems.add(item)

        // Persist to Room LifeVaultDao if available
        try {
            lifeVaultDao?.insertVaultItem(
                LifeVaultItemEntity(
                    category = tier.name,
                    key = subject,
                    value = content,
                    source = source,
                    isConfirmed = knowledgeState == KnowledgeState.KNOWN
                )
            )
        } catch (_: Exception) {}

        return item
    }

    /**
     * Forgets or deletes a memory item (for user transparency and control).
     */
    suspend fun forgetMemory(subjectOrContent: String): Boolean {
        val target = inMemoryItems.firstOrNull {
            it.subject.contains(subjectOrContent, ignoreCase = true) ||
            it.content.contains(subjectOrContent, ignoreCase = true)
        }
        return if (target != null) {
            inMemoryItems.remove(target)
            try {
                lifeVaultDao?.deleteVaultItemByKey(target.subject)
            } catch (_: Exception) {}
            true
        } else {
            false
        }
    }

    /**
     * Learns from a user correction.
     * E.g. "No, I don't like that", "I always take Yatri Sathi", "Don't remind me about this"
     */
    suspend fun learnFromCorrection(correction: String): String {
        val lower = correction.lowercase()
        return when {
            lower.contains("don't like") || lower.contains("dont like") || lower.contains("no, i don't") -> {
                val disliked = correction.replace(Regex("(?i)no,? i don't like|dont like"), "").trim()
                storeMemory(
                    tier = MemoryTier.PREFERENCE,
                    subject = "Disliked Preference",
                    content = "Avoids or dislikes: $disliked",
                    source = "USER_CORRECTION"
                )
                "Understood Tanmoy. I've noted that you dislike $disliked and will avoid suggesting it."
            }
            lower.contains("always") || lower.contains("prefer") -> {
                val pref = correction.replace(Regex("(?i)i always|i prefer"), "").trim()
                storeMemory(
                    tier = MemoryTier.PREFERENCE,
                    subject = "Strong Preference",
                    content = "Always prefers: $pref",
                    source = "USER_CORRECTION"
                )
                "Recorded your preference for $pref. I'll prioritize this in future recommendations."
            }
            lower.contains("don't remind me") || lower.contains("dont remind me") -> {
                storeMemory(
                    tier = MemoryTier.PREFERENCE,
                    subject = "Notification Preference",
                    content = "Do not remind about: ${correction.takeLast(40)}",
                    source = "USER_CORRECTION"
                )
                "I have updated your notification preferences and won't prompt you on this again."
            }
            else -> {
                storeMemory(
                    tier = MemoryTier.SEMANTIC,
                    subject = "User Correction",
                    content = correction,
                    source = "USER_CORRECTION"
                )
                "Learned and updated in your personal knowledge vault."
            }
        }
    }

    /**
     * Returns "What VYB has learned" for full user transparency and inspection.
     */
    fun getWhatVybHasLearned(): List<IntelligentMemoryItem> {
        return inMemoryItems.toList()
    }

    fun getAllLearnedMemories(): List<IntelligentMemoryItem> = inMemoryItems.toList()

    private var learningPaused: Boolean = false

    fun isLearningPaused(): Boolean = learningPaused

    fun setLearningPaused(paused: Boolean) {
        learningPaused = paused
    }

    fun getAllItems(): List<IntelligentMemoryItem> = inMemoryItems.toList()
}
