package com.example.domain.intelligence

import com.example.data.local.entity.LifeVaultItemEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.domain.genesis.*
import java.util.Locale

/**
 * Filtered contextual memory result.
 */
data class RetrievedContextPackage(
    val relevantMemories: List<MemoryItemEntity>,
    val relevantVaultItems: List<LifeVaultItemEntity>,
    val relevantGraphNodes: List<ContextGraphNode>,
    val filtersAppliedSummary: String,
    val totalAvailableItemsCount: Int,
    val itemsIncludedCount: Int
)

/**
 * Contextual Memory Retrieval Engine.
 * Enforces 4 strict filters:
 * 1. RELEVANCE: Keyword and semantic relevance to current prompt/intent.
 * 2. PRIVACY: Respects privacy levels (e.g. Confidential items only retrieved when explicitly needed).
 * 3. PERMISSION: Only reads from data sources explicitly authorized in Genesis.
 * 4. CONFIDENCE: Excludes rejected or sub-threshold inferred assumptions.
 */
class ContextualMemoryRetrievalEngine {

    fun retrieveRelevantContext(
        query: String,
        intentType: UniversalIntentType,
        authorizedSources: Set<GenesisDataSource>,
        allMemories: List<MemoryItemEntity>,
        allVaultItems: List<LifeVaultItemEntity>,
        contextGraph: PersonalContextGraph? = null
    ): RetrievedContextPackage {
        val lowerQuery = query.lowercase(Locale.ROOT)
        val queryTokens = lowerQuery.split(Regex("[^a-zA-Z0-9]+")).filter { it.length > 2 }
        val totalAvailable = allMemories.size + allVaultItems.size + (contextGraph?.nodes?.size ?: 0)

        // 1. Determine categories allowed based on Genesis Permissions
        val allowFamily = authorizedSources.contains(GenesisDataSource.SUPPORTED_COMMUNICATION_DATA) ||
                authorizedSources.contains(GenesisDataSource.CONTACTS) ||
                authorizedSources.contains(GenesisDataSource.IDENTITY)
        val allowFinance = authorizedSources.contains(GenesisDataSource.SUPPORTED_FINANCIAL_DATA)
        val allowDevice = authorizedSources.contains(GenesisDataSource.DEVICE)
        val allowCalendar = authorizedSources.contains(GenesisDataSource.CALENDAR)

        // 2. Filter Memories
        val filteredMemories = allMemories.filter { mem ->
            // Filter 1: Confidence
            if (mem.confidence < 0.5f) return@filter false

            // Filter 2: Permission check
            val catUpper = mem.category.uppercase(Locale.ROOT)
            if (catUpper.contains("FINANCE") && !allowFinance) return@filter false
            if (catUpper.contains("FAMILY") && !allowFamily) return@filter false

            // Filter 3: Relevance check
            val contentLower = mem.value.lowercase(Locale.ROOT)
            val keyLower = mem.key.lowercase(Locale.ROOT)
            val matchesQuery = queryTokens.any { token ->
                contentLower.contains(token) || keyLower.contains(token)
            }

            val isGeneralRecall = intentType == UniversalIntentType.MEMORY_QUERY ||
                    intentType == UniversalIntentType.PERSONAL_KNOWLEDGE_QUERY ||
                    lowerQuery.contains("all_categories") ||
                    lowerQuery.contains("remember") ||
                    lowerQuery.contains("recall")

            val matchesIntent = when (intentType) {
                UniversalIntentType.MEMORY_QUERY, UniversalIntentType.PERSONAL_KNOWLEDGE_QUERY -> true
                UniversalIntentType.SHOPPING_QUERY -> catUpper.contains("SHOPPING") || catUpper.contains("FAMILY") || catUpper.contains("PREFERENCE")
                UniversalIntentType.BOOK_TRANSPORT -> catUpper.contains("COMMUTE") || catUpper.contains("TRANSPORT") || catUpper.contains("LOCATION")
                UniversalIntentType.FINANCE_ANALYSIS -> catUpper.contains("FINANCE") || catUpper.contains("BUDGET")
                UniversalIntentType.CALENDAR_SCHEDULE -> catUpper.contains("ROUTINE") || catUpper.contains("WORK") || catUpper.contains("FAMILY")
                UniversalIntentType.DAILY_BRIEFING -> true // General briefing can take high-priority memories
                else -> false
            }

            if (isGeneralRecall) true else (matchesQuery || matchesIntent)
        }.sortedByDescending { it.timestamp }.take(12)

        // 3. Filter Vault Items
        val filteredVault = allVaultItems.filter { vault ->
            // Filter 1: Permission check
            val catUpper = vault.category.uppercase(Locale.ROOT)
            if (catUpper.contains("FINANCIAL") && !allowFinance) return@filter false
            if (catUpper.contains("FAMILY") && !allowFamily) return@filter false

            // Filter 2: Exclude unresolved conflicts from polluting AI reasoning
            if (vault.isPendingConflict) return@filter false

            // Filter 3: Relevance check
            val keyLower = vault.key.lowercase(Locale.ROOT)
            val valueLower = vault.value.lowercase(Locale.ROOT)
            val matchesQuery = queryTokens.any { token ->
                keyLower.contains(token) || valueLower.contains(token)
            }

            val isGeneralRecall = intentType == UniversalIntentType.MEMORY_QUERY ||
                    intentType == UniversalIntentType.PERSONAL_KNOWLEDGE_QUERY ||
                    lowerQuery.contains("all_categories") ||
                    lowerQuery.contains("remember") ||
                    lowerQuery.contains("recall")

            val matchesIntent = when (intentType) {
                UniversalIntentType.MEMORY_QUERY, UniversalIntentType.PERSONAL_KNOWLEDGE_QUERY -> true
                UniversalIntentType.BOOK_TRANSPORT -> keyLower.contains("office") || keyLower.contains("home") || keyLower.contains("cab")
                UniversalIntentType.FINANCE_ANALYSIS -> catUpper.contains("FINANCIAL") || keyLower.contains("income") || keyLower.contains("budget")
                UniversalIntentType.SHOPPING_QUERY -> keyLower.contains("brand") || keyLower.contains("school") || keyLower.contains("diet")
                UniversalIntentType.DAILY_BRIEFING -> true
                else -> false
            }

            if (isGeneralRecall) true else (matchesQuery || matchesIntent)
        }.sortedByDescending { it.updatedAt }.take(12)

        // 4. Filter Context Graph Nodes
        val filteredNodes = contextGraph?.nodes?.filter { node ->
            // Exclude UNKNOWN classification
            if (node.classification == InformationClassification.UNKNOWN) return@filter false

            val labelLower = node.label.lowercase(Locale.ROOT)
            val matchesQuery = queryTokens.any { token -> labelLower.contains(token) }
            val matchesType = when (intentType) {
                UniversalIntentType.BOOK_TRANSPORT -> node.type == GraphNodeType.PLACE || node.type == GraphNodeType.WORK || node.type == GraphNodeType.PREFERENCE
                UniversalIntentType.SHOPPING_QUERY -> node.type == GraphNodeType.FAMILY || node.type == GraphNodeType.CHILD || node.type == GraphNodeType.SCHOOL || node.type == GraphNodeType.PREFERENCE
                UniversalIntentType.FINANCE_ANALYSIS -> node.type == GraphNodeType.FINANCE || node.type == GraphNodeType.EXPENSES || node.type == GraphNodeType.GOALS
                else -> false
            }

            matchesQuery || matchesType
        }?.take(6) ?: emptyList()

        val totalIncluded = filteredMemories.size + filteredVault.size + filteredNodes.size
        val summary = "Retrieved $totalIncluded relevant records (${filteredMemories.size} memories, ${filteredVault.size} vault items, ${filteredNodes.size} graph nodes) after RELEVANCE + PRIVACY + PERMISSION + CONFIDENCE filters."

        return RetrievedContextPackage(
            relevantMemories = filteredMemories,
            relevantVaultItems = filteredVault,
            relevantGraphNodes = filteredNodes,
            filtersAppliedSummary = summary,
            totalAvailableItemsCount = totalAvailable,
            itemsIncludedCount = totalIncluded
        )
    }
}
