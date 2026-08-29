package com.example.domain.context

import android.content.Context
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.TaskDao
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.TaskItemEntity
import com.example.domain.capability.CapabilityEngine
import kotlinx.coroutines.flow.firstOrNull

class ContextEngine(
    private val memoryDao: MemoryDao? = null,
    private val taskDao: TaskDao? = null
) {
    suspend fun buildSnapshot(appContext: Context?): VybContextSnapshot {
        val memories: List<MemoryItemEntity> = try {
            memoryDao?.getAllMemories()?.firstOrNull() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val tasks: List<TaskItemEntity> = try {
            taskDao?.getAllTasks()?.firstOrNull()?.filter { !it.isCompleted } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val capabilities = CapabilityEngine.getAllCapabilities(appContext)

        val preferences = memories
            .filter { it.category.equals("PREFERENCE", ignoreCase = true) || it.category.equals("PREFERENCES", ignoreCase = true) }
            .map { "${it.key}: ${it.value}" }

        return VybContextSnapshot(
            storedPreferences = preferences,
            storedMemories = memories,
            activeTasks = tasks,
            checkedCapabilities = capabilities
        )
    }

    fun getRelevantContextForIntent(intentType: String, snapshot: VybContextSnapshot): List<String> {
        val relevant = mutableListOf<String>()
        relevant.add("Time/Date: ${snapshot.currentFormattedTime}")
        if (snapshot.cityLocation.isNotBlank() || snapshot.ownerName.isNotBlank()) {
            relevant.add("Location: ${snapshot.cityLocation} (Owner: ${snapshot.ownerName})")
        }

        when (intentType) {
            "TRACK_ORDER" -> {
                val amazonCap = snapshot.checkedCapabilities.find { it.type.id == "amazon" }
                relevant.add("Amazon Account Status: ${amazonCap?.status?.label ?: "Not Connected"}")
                relevant.add("Pending package queries must be checked against real connected account")
            }
            "BOOK_TRANSPORT", "RIDE" -> {
                if (snapshot.sonContext.name.isNotBlank()) {
                    relevant.add("Family Member: ${snapshot.sonContext.name} (${snapshot.sonContext.age} yrs), School: ${snapshot.sonContext.schoolName}")
                }
                val pricePref = snapshot.storedPreferences.find { it.contains("cab", true) || it.contains("cheap", true) || it.contains("transport", true) }
                if (pricePref != null) {
                    relevant.add("Memory Preference: $pricePref")
                }
                relevant.add("Supported transport providers: Rapido, Uber, Ola, Yatri Sathi, inDrive (Can launch installed app on device)")
            }
            "AFFORDABILITY_ANALYSIS", "FINANCE" -> {
                if (snapshot.financeContext.monthlyIncomeInr > 0) {
                    relevant.add("Monthly Income: ₹${snapshot.financeContext.monthlyIncomeInr.toInt()}")
                    relevant.add("Monthly Savings Target: ₹${snapshot.financeContext.monthlySavingsTargetInr.toInt()}")
                    relevant.add("Current Discretionary Buffer: ₹${snapshot.financeContext.activeDiscretionaryBufferInr.toInt()}")
                } else {
                    relevant.add("Financial Profile: Unconfigured")
                }
            }
            "DAILY_BRIEFING", "BRIEFING" -> {
                if (snapshot.activeTasks.isNotEmpty()) {
                    relevant.add("Active Pending Tasks (${snapshot.activeTasks.size}): ${snapshot.activeTasks.take(2).joinToString { it.title }}")
                }
                if (snapshot.financeContext.activeDiscretionaryBufferInr > 0) {
                    relevant.add("Discretionary Budget Buffer: ₹${snapshot.financeContext.activeDiscretionaryBufferInr.toInt()}")
                }
            }
            "RESUME_PROJECT", "PROJECT" -> {
                snapshot.activeProjects.forEach {
                    relevant.add("Project '${it.name}': Phase '${it.phase}', Milestone: '${it.currentMilestone}', Next: '${it.nextStep}'")
                }
            }
            "STYLE_RECOMMENDATION", "STYLE" -> {
                if (snapshot.cityLocation.isNotBlank()) {
                    relevant.add("City Location: ${snapshot.cityLocation}")
                }
            }
            "STORE_PREFERENCE" -> {
                relevant.add("Action: Store user stated preference in permanent memory vault")
            }
            "QUERY_MEMORY" -> {
                relevant.add("Total Memories Stored: ${snapshot.storedMemories.size}")
                relevant.add("Stored categories: Facts, Preferences, Habits, Projects, Decisions, Goals")
            }
            else -> {
                if (snapshot.financeContext.activeDiscretionaryBufferInr > 0) {
                    relevant.add("Discretionary Buffer: ₹${snapshot.financeContext.activeDiscretionaryBufferInr.toInt()}")
                }
                if (snapshot.activeProjects.isNotEmpty()) {
                    relevant.add("Active Projects: ${snapshot.activeProjects.joinToString { it.name }}")
                }
            }
        }
        return relevant
    }
}
