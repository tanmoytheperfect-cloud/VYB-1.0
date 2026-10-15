package com.example.domain.intelligence

import com.example.data.local.AppDatabase
import com.example.domain.genesis.VybPersonalProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Developer diagnostic model reflecting live on-device personal context state.
 */
data class PersonalContextDiagnosticSnapshot(
    val persistedMemoryCount: Int,
    val retrievableMemoryCount: Int,
    val activeIntentionsCount: Int,
    val activeRoutinesCount: Int,
    val confirmedFactsCount: Int,
    val inferredFactsCount: Int,
    val conversationContextAvailable: Boolean,
    val ownerContextAvailable: Boolean,
    val lastSuccessfulMemoryRetrieval: String?,
    val lastRetrievalResultSummary: String,
    val fallbackReason: String?
)

/**
 * Developer Diagnostic Inspector for Personal Context & Intelligence.
 */
object PersonalContextDiagnostics {

    @Volatile
    var lastSuccessfulMemoryRetrievalTime: Long? = null

    @Volatile
    var lastRetrievalResultSummary: String = "No retrieval performed yet."

    @Volatile
    var lastFallbackReason: String? = null

    fun recordRetrievalSuccess(summary: String) {
        lastSuccessfulMemoryRetrievalTime = System.currentTimeMillis()
        lastRetrievalResultSummary = summary
        lastFallbackReason = null
    }

    fun recordFallback(reason: String) {
        lastFallbackReason = reason
    }

    suspend fun inspectPersonalContext(
        database: AppDatabase,
        profile: VybPersonalProfile,
        conversationContextEngine: ConversationContextEngine
    ): PersonalContextDiagnosticSnapshot = withContext(Dispatchers.IO) {
        val memories = database.memoryDao().getAllMemoriesDirect()
        val vaultItems = database.lifeVaultDao().getAllVaultItems()
        val routines = database.routineDao().getAllRoutinesDirect()
        val family = database.familyDao().getAllFamilyMembersDirect()
        val goals = database.goalDao().getAllGoalsDirect()
        val projects = database.projectDao().getAllProjectsDirect()

        val confirmedCount = vaultItems.count { !it.isPendingConflict } +
                memories.count { it.confidence >= 0.95 } +
                (if (profile.name.isNotBlank()) 1 else 0) +
                family.size

        val inferredCount = memories.count { it.confidence in 0.5..0.94 }

        val activeIntentions = routines.count { it.isEnabled } + goals.count { !it.isCompleted }

        val conversationAvailable = conversationContextEngine.getAllRecentStatements().isNotEmpty()
        val ownerAvailable = profile.name.isNotBlank() || profile.callingTitle.isNotBlank() || profile.companyName.isNotBlank()

        val timeStr = lastSuccessfulMemoryRetrievalTime?.let {
            java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault()).format(java.util.Date(it))
        }

        PersonalContextDiagnosticSnapshot(
            persistedMemoryCount = memories.size + vaultItems.size,
            retrievableMemoryCount = memories.size + vaultItems.size + family.size + routines.size + goals.size + projects.size,
            activeIntentionsCount = activeIntentions,
            activeRoutinesCount = routines.size,
            confirmedFactsCount = confirmedCount,
            inferredFactsCount = inferredCount,
            conversationContextAvailable = conversationAvailable,
            ownerContextAvailable = ownerAvailable,
            lastSuccessfulMemoryRetrieval = timeStr,
            lastRetrievalResultSummary = lastRetrievalResultSummary,
            fallbackReason = lastFallbackReason
        )
    }
}
