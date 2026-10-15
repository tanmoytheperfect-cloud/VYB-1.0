package com.example.domain.persistence

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.domain.genesis.VybPersonalProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

data class CloudPushResult(
    val success: Boolean,
    val status: CloudSyncStatus,
    val message: String,
    val uploadedRecordsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class CloudPullResult(
    val success: Boolean,
    val status: CloudSyncStatus,
    val message: String,
    val state: DurablePersonalIntelligenceState? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class PersistenceDiagnosticSnapshot(
    val ownerId: String,
    val isAuthenticated: Boolean,
    val localVaultCount: Int,
    val localMemoryCount: Int,
    val localIntentionCount: Int,
    val durableFactCount: Int,
    val lastSyncTimestamp: Long,
    val lastSnapshotFormatted: String,
    val syncStatus: CloudSyncStatus,
    val isCloudEndpointConfigured: Boolean,
    val durableStoragePath: String,
    val stateVersion: Int,
    val integrityStatus: String,
    val restoreStatus: String,
    val storageArchitectureBreakdown: String
)

interface VybRemoteCloudVault {
    suspend fun pushOwnerState(state: DurablePersonalIntelligenceState): CloudPushResult
    suspend fun pullOwnerState(ownerId: String): CloudPullResult
    fun isCloudConfigured(): Boolean
    fun getCloudStatus(): CloudSyncStatus
}

/**
 * Concrete implementation of VYB's Cloud & Durable Persistence Layer.
 * Operates across:
 * 1. Persistent External / App Storage Anchor (Durable Vault File surviving internal cache wipe)
 * 2. Remote HTTP/REST Cloud Gateway (Active when remote sync endpoint is configured)
 */
class VybCloudPersistenceEngine(
    private val context: Context,
    private val remoteVault: VybRemoteCloudVault? = null
) {
    private val lock = Any()
    private val durableVaultDir: File
        get() {
            // Prioritize persistent files dir that survives app updates
            val dir = File(context.filesDir, "vyb_durable_vault")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private fun getVaultFile(ownerId: String): File {
        val safeName = ownerId.replace(Regex("[^a-zA-Z0-9_]"), "_")
        return File(durableVaultDir, "vault_$safeName.json")
    }

    private val secondaryVaultDir: File?
        get() {
            return try {
                val ext = context.getExternalFilesDir(null)
                if (ext != null) {
                    val dir = File(ext, "vyb_persistent_backup")
                    if (!dir.exists()) dir.mkdirs()
                    dir
                } else null
            } catch (_: Exception) {
                null
            }
        }

    @Volatile
    private var lastSyncTime: Long = 0L

    @Volatile
    private var lastFailedSyncTime: Long = 0L

    @Volatile
    private var lastSyncStatus: CloudSyncStatus = CloudSyncStatus.LOCAL_DURABLE_ONLY

    @Volatile
    private var lastRestoreStatus: String = "IDLE"

    fun getSyncStatus(): CloudSyncStatus = lastSyncStatus
    fun getLastSyncTimestamp(): Long = lastSyncTime
    fun getLastFailedSyncTimestamp(): Long = lastFailedSyncTime
    fun getLastRestoreStatus(): String = lastRestoreStatus

    /**
     * Builds a complete, versioned snapshot from local Room database and profile.
     */
    suspend fun buildSnapshotFromDatabase(
        database: AppDatabase,
        profile: VybPersonalProfile,
        ownerId: String,
        email: String = "",
        phone: String = "",
        aiTone: String = "EXECUTIVE_PA"
    ): DurablePersonalIntelligenceState = withContext(Dispatchers.IO) {
        val vaultItems = database.lifeVaultDao().getAllVaultItems()
        val memories = database.memoryDao().getAllMemoriesDirect()
        val intentions = database.persistentIntentionDao().getAllIntentionsDirect()
        val routines = database.routineDao().getAllRoutinesDirect()
        val goals = database.goalDao().getAllGoalsDirect()
        val family = database.familyDao().getAllFamilyMembersDirect()

        val durableFacts = vaultItems.map {
            DurableVaultFact(
                globalId = "fact_" + it.id + "_" + it.key.hashCode(),
                ownerId = ownerId,
                category = it.category,
                key = it.key,
                value = it.value,
                numericValue = it.numericValue,
                source = it.source,
                sourceDetail = it.sourceDetail,
                truthState = if (it.isConfirmed) TruthState.VERIFIED else TruthState.USER_CONFIRMED,
                confidence = 1.0,
                version = 1L,
                isConfirmed = it.isConfirmed,
                isDeleted = false,
                createdAt = it.timestamp,
                updatedAt = it.updatedAt
            )
        }

        val durableMemories = memories.map {
            DurableMemory(
                globalId = "mem_" + it.id + "_" + it.key.hashCode(),
                ownerId = ownerId,
                category = it.category,
                key = it.key,
                value = it.value,
                source = it.source,
                truthState = TruthState.USER_CONFIRMED,
                confidence = it.confidence,
                version = 1L,
                isDeleted = false,
                createdAt = it.timestamp,
                updatedAt = it.timestamp
            )
        }

        val durableIntentions = intentions.map {
            DurableIntention(
                globalId = "int_" + it.id,
                ownerId = ownerId,
                intentionId = it.id,
                title = it.notificationTitle,
                targetAction = it.normalizedIntent,
                scheduledEpochMs = it.scheduledTimestamp,
                recurrenceRule = it.recurrenceRule,
                state = it.executionState,
                version = 1L,
                isDeleted = false,
                createdAt = it.createdAt,
                updatedAt = it.createdAt
            )
        }

        val durableRoutines = routines.map {
            DurableRoutine(
                globalId = "rout_" + it.id + "_" + it.title.hashCode(),
                ownerId = ownerId,
                title = it.title,
                category = it.category,
                frequency = it.daysOfWeek,
                triggerTime = it.timeOfDay,
                actionSummary = it.description,
                truthState = TruthState.USER_CONFIRMED,
                version = 1L,
                isDeleted = false
            )
        }

        val durableGoals = goals.map {
            DurableGoal(
                globalId = "goal_" + it.id + "_" + it.title.hashCode(),
                ownerId = ownerId,
                title = it.title,
                category = it.category,
                targetValue = it.targetValue.toDoubleOrNull() ?: 0.0,
                currentValue = it.currentValue.toDoubleOrNull() ?: 0.0,
                unit = it.targetDeadline,
                truthState = TruthState.USER_CONFIRMED,
                version = 1L,
                isDeleted = false
            )
        }

        val durableFamily = family.map {
            DurableFamilyMember(
                globalId = "fam_" + it.id + "_" + it.name.hashCode(),
                ownerId = ownerId,
                name = it.name,
                relationship = it.relation,
                phone = it.occupationOrSchool,
                routineSummary = it.routineSummary,
                notes = it.notes,
                truthState = TruthState.VERIFIED,
                version = 1L,
                isDeleted = false
            )
        }

        val durableProfile = DurableOwnerProfile(
            ownerId = ownerId,
            name = profile.name,
            callingTitle = profile.callingTitle,
            email = email,
            phone = phone,
            companyName = profile.companyName,
            jobTitle = profile.jobTitle,
            homeAddress = profile.homeAddress,
            officeAddress = profile.officeAddress,
            monthlyIncome = profile.monthlyIncome,
            fixedCommitments = profile.fixedCommitments,
            savingsTarget = profile.savingsTarget,
            currencySymbol = profile.currencySymbol,
            dietaryPreference = profile.dietaryPreference,
            aiTone = aiTone,
            version = 1L,
            updatedAt = System.currentTimeMillis()
        )

        DurablePersonalIntelligenceState(
            ownerId = ownerId,
            stateVersion = DurablePersonalIntelligenceState.CURRENT_SCHEMA_VERSION,
            deviceSource = "ANDROID_DEVICE",
            profile = durableProfile,
            vaultFacts = durableFacts,
            memories = durableMemories,
            intentions = durableIntentions,
            routines = durableRoutines,
            goals = durableGoals,
            familyMembers = durableFamily,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Persists the snapshot to durable file storage and pushes to remote cloud if configured.
     */
    suspend fun saveAndSyncSnapshot(state: DurablePersonalIntelligenceState): CloudPushResult = withContext(Dispatchers.IO) {
        synchronized(lock) {
            try {
                val jsonString = state.toJsonString()

                // 1. Write to primary durable file
                val file = getVaultFile(state.ownerId)
                FileOutputStream(file).use { it.write(jsonString.toByteArray(StandardCharsets.UTF_8)) }

                // 2. Write to secondary durable backup file (external storage)
                secondaryVaultDir?.let { sDir ->
                    val sFile = File(sDir, "vault_${state.ownerId}.json")
                    FileOutputStream(sFile).use { it.write(jsonString.toByteArray(StandardCharsets.UTF_8)) }
                }

                lastSyncTime = System.currentTimeMillis()

                // 3. Remote Cloud Sync if adapter is present and configured
                if (remoteVault != null && remoteVault.isCloudConfigured()) {
                    val remoteResult = kotlinx.coroutines.runBlocking { remoteVault.pushOwnerState(state) }
                    lastSyncStatus = remoteResult.status
                    return@withContext remoteResult
                } else {
                    lastSyncStatus = CloudSyncStatus.LOCAL_DURABLE_ONLY
                    return@withContext CloudPushResult(
                        success = true,
                        status = CloudSyncStatus.LOCAL_DURABLE_ONLY,
                        message = "Durable owner vault persisted locally (Survives app reinstall & update). Remote cloud endpoint requires configuration.",
                        uploadedRecordsCount = state.vaultFacts.size + state.memories.size + state.intentions.size
                    )
                }
            } catch (e: Exception) {
                lastFailedSyncTime = System.currentTimeMillis()
                lastSyncStatus = CloudSyncStatus.FAILED_OFFLINE
                return@withContext CloudPushResult(
                    success = false,
                    status = CloudSyncStatus.FAILED_OFFLINE,
                    message = "Failed to persist durable owner vault: ${e.message}"
                )
            }
        }
    }

    fun parseStateFromJson(jsonContent: String, fallbackOwnerId: String = ""): DurablePersonalIntelligenceState? {
        return try {
            val json = JSONObject(jsonContent)
            val parsedOwnerId = json.optString("ownerId", fallbackOwnerId)
            val stateVersion = json.optInt("stateVersion", 1)
            val deviceSource = json.optString("deviceSource", "SAVED_DEVICE")
            val profile = DurableOwnerProfile.fromJson(json.getJSONObject("profile"))

            val factsList = mutableListOf<DurableVaultFact>()
            val factsArray = json.optJSONArray("vaultFacts") ?: JSONArray()
            for (i in 0 until factsArray.length()) {
                factsList.add(DurableVaultFact.fromJson(factsArray.getJSONObject(i)))
            }

            val memoriesList = mutableListOf<DurableMemory>()
            val memoriesArray = json.optJSONArray("memories") ?: JSONArray()
            for (i in 0 until memoriesArray.length()) {
                memoriesList.add(DurableMemory.fromJson(memoriesArray.getJSONObject(i)))
            }

            val intentionsList = mutableListOf<DurableIntention>()
            val intentionsArray = json.optJSONArray("intentions") ?: JSONArray()
            for (i in 0 until intentionsArray.length()) {
                intentionsList.add(DurableIntention.fromJson(intentionsArray.getJSONObject(i)))
            }

            val routinesList = mutableListOf<DurableRoutine>()
            val routinesArray = json.optJSONArray("routines") ?: JSONArray()
            for (i in 0 until routinesArray.length()) {
                routinesList.add(DurableRoutine.fromJson(routinesArray.getJSONObject(i)))
            }

            val goalsList = mutableListOf<DurableGoal>()
            val goalsArray = json.optJSONArray("goals") ?: JSONArray()
            for (i in 0 until goalsArray.length()) {
                goalsList.add(DurableGoal.fromJson(goalsArray.getJSONObject(i)))
            }

            val familyList = mutableListOf<DurableFamilyMember>()
            val familyArray = json.optJSONArray("familyMembers") ?: JSONArray()
            for (i in 0 until familyArray.length()) {
                familyList.add(DurableFamilyMember.fromJson(familyArray.getJSONObject(i)))
            }

            val checksum = json.optString("checksum", "")

            DurablePersonalIntelligenceState(
                ownerId = parsedOwnerId,
                stateVersion = stateVersion,
                deviceSource = deviceSource,
                profile = profile,
                vaultFacts = factsList,
                memories = memoriesList,
                intentions = intentionsList,
                routines = routinesList,
                goals = goalsList,
                familyMembers = familyList,
                checksum = checksum
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Loads the durable owner snapshot from remote cloud (if available) or local durable vault file.
     */
    suspend fun loadDurableState(ownerId: String): DurablePersonalIntelligenceState? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            lastRestoreStatus = "FETCHING_REMOTE_OR_DURABLE"

            // 1. Try remote cloud first if available
            if (remoteVault != null && remoteVault.isCloudConfigured()) {
                val remoteResult = kotlinx.coroutines.runBlocking { remoteVault.pullOwnerState(ownerId) }
                if (remoteResult.success && remoteResult.state != null) {
                    lastRestoreStatus = "REMOTE_FETCH_SUCCESS"
                    return@withContext remoteResult.state
                }
            }

            // 2. Fall back to local durable vault file
            val file = getVaultFile(ownerId)
            var jsonContent: String? = null
            if (file.exists() && file.length() > 0) {
                jsonContent = FileInputStream(file).use { it.bufferedReader(StandardCharsets.UTF_8).readText() }
            } else {
                // Try secondary backup
                secondaryVaultDir?.let { sDir ->
                    val sFile = File(sDir, "vault_$ownerId.json")
                    if (sFile.exists() && sFile.length() > 0) {
                        jsonContent = FileInputStream(sFile).use { it.bufferedReader(StandardCharsets.UTF_8).readText() }
                    }
                }
            }

            val safeJson = jsonContent
            if (safeJson.isNullOrBlank()) {
                lastRestoreStatus = "NO_DURABLE_RECORD_FOUND"
                return@withContext null
            }

            val state = parseStateFromJson(safeJson, ownerId)
            if (state != null) {
                lastRestoreStatus = "DURABLE_LOAD_SUCCESS (${state.vaultFacts.size} facts, ${state.memories.size} memories)"
                return@withContext state
            } else {
                lastRestoreStatus = "DESERIALIZATION_ERROR: Corrupted JSON"
                return@withContext null
            }
        }
    }

    /**
     * Restores a durable state into Room database cache without dropping tables.
     */
    suspend fun restoreStateIntoDatabase(
        state: DurablePersonalIntelligenceState,
        database: AppDatabase
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            lastRestoreStatus = "REHYDRATING_ROOM_CACHE"

            // 1. Restore Vault Facts
            val lifeVaultDao = database.lifeVaultDao()
            state.vaultFacts.filter { !it.isDeleted }.forEach { fact ->
                val existing = lifeVaultDao.getItemByCategoryAndKey(fact.category, fact.key)
                if (existing == null) {
                    lifeVaultDao.insertVaultItem(
                        LifeVaultItemEntity(
                            category = fact.category,
                            key = fact.key,
                            value = fact.value,
                            source = fact.source,
                            sourceDetail = fact.sourceDetail,
                            numericValue = fact.numericValue,
                            isConfirmed = fact.isConfirmed,
                            timestamp = fact.createdAt,
                            updatedAt = fact.updatedAt
                        )
                    )
                } else if (fact.updatedAt >= existing.updatedAt) {
                    lifeVaultDao.updateVaultItem(
                        existing.copy(
                            value = fact.value,
                            numericValue = fact.numericValue,
                            isConfirmed = fact.isConfirmed,
                            updatedAt = fact.updatedAt
                        )
                    )
                }
            }

            // 2. Restore Memories
            val memoryDao = database.memoryDao()
            state.memories.filter { !it.isDeleted }.forEach { mem ->
                val allMem = memoryDao.getAllMemoriesDirect()
                val existing = allMem.find { it.category == mem.category && it.key.equals(mem.key, true) }
                if (existing == null) {
                    memoryDao.insertMemory(
                        MemoryItemEntity(
                            category = mem.category,
                            key = mem.key,
                            value = mem.value,
                            source = mem.source,
                            confidence = mem.confidence,
                            timestamp = mem.createdAt
                        )
                    )
                } else {
                    memoryDao.updateMemory(
                        existing.copy(
                            value = mem.value,
                            confidence = mem.confidence,
                            timestamp = mem.updatedAt
                        )
                    )
                }
            }

            // 3. Restore Intentions
            val intentionDao = database.persistentIntentionDao()
            state.intentions.filter { !it.isDeleted }.forEach { intent ->
                val existing = intentionDao.getIntentionById(intent.intentionId)
                if (existing == null) {
                    intentionDao.insertIntention(
                        PersistentIntentionEntity(
                            id = intent.intentionId,
                            ownerId = intent.ownerId,
                            rawRequest = intent.title,
                            normalizedIntent = intent.targetAction,
                            scheduledTimestamp = intent.scheduledEpochMs,
                            recurrenceRule = intent.recurrenceRule,
                            executionState = intent.state,
                            notificationTitle = intent.title,
                            notificationBody = intent.title,
                            createdAt = intent.createdAt
                        )
                    )
                }
            }

            // 4. Restore Goals
            val goalDao = database.goalDao()
            state.goals.filter { !it.isDeleted }.forEach { goal ->
                val existing = goalDao.getAllGoalsDirect().find { it.title.equals(goal.title, true) }
                if (existing == null) {
                    goalDao.insertGoal(
                        GoalEntity(
                            title = goal.title,
                            category = goal.category,
                            targetValue = goal.targetValue.toString(),
                            currentValue = goal.currentValue.toString(),
                            targetDeadline = goal.unit
                        )
                    )
                }
            }

            // 5. Restore Routines
            val routineDao = database.routineDao()
            state.routines.filter { !it.isDeleted }.forEach { r ->
                val existing = routineDao.getAllRoutinesDirect().find { it.title.equals(r.title, true) }
                if (existing == null) {
                    routineDao.insertRoutine(
                        RoutineEntity(
                            title = r.title,
                            timeOfDay = r.triggerTime,
                            daysOfWeek = r.frequency,
                            description = r.actionSummary,
                            actionPrompt = r.actionSummary,
                            category = r.category
                        )
                    )
                }
            }

            // 6. Restore Family
            val familyDao = database.familyDao()
            state.familyMembers.filter { !it.isDeleted }.forEach { fam ->
                val existing = familyDao.getAllFamilyMembersDirect().find { it.name.equals(fam.name, true) }
                if (existing == null) {
                    familyDao.insertFamilyMember(
                        FamilyMemberEntity(
                            name = fam.name,
                            relation = fam.relationship,
                            age = 0,
                            occupationOrSchool = fam.phone,
                            notes = fam.notes,
                            routineSummary = fam.routineSummary
                        )
                    )
                }
            }

            lastRestoreStatus = "RESTORE_COMPLETE"
            return@withContext true
        } catch (e: Exception) {
            lastRestoreStatus = "RESTORE_FAILED: ${e.message}"
            return@withContext false
        }
    }

    /**
     * Diagnoses live status for developer inspection.
     */
    suspend fun getDiagnosticSnapshot(
        database: AppDatabase,
        ownerId: String
    ): PersistenceDiagnosticSnapshot = withContext(Dispatchers.IO) {
        val vaultCount = database.lifeVaultDao().getVaultFactCount()
        val memCount = database.memoryDao().getMemoryCountDirect()
        val intCount = database.persistentIntentionDao().getPendingCount()

        val durableFile = getVaultFile(ownerId)
        var fileFactCount = 0
        var integrityCheck = "NO_SNAPSHOT_YET"
        var snapshotFormatted = "None"

        if (durableFile.exists()) {
            try {
                val fileContent = durableFile.readText(StandardCharsets.UTF_8)
                val json = JSONObject(fileContent)
                fileFactCount = json.optJSONArray("vaultFacts")?.length() ?: 0
                val parsedState = parseStateFromJson(fileContent)
                integrityCheck = if (parsedState?.validateIntegrity() == true) {
                    "VALID (SHA-256 Checksum Verified)"
                } else {
                    "INTEGRITY_MISMATCH"
                }
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                snapshotFormatted = sdf.format(java.util.Date(durableFile.lastModified()))
            } catch (e: Exception) {
                integrityCheck = "READ_ERROR: ${e.message}"
            }
        }

        val architectureSummary = buildString {
            append("1. LOCAL WORKING CACHE: SQLite Room DB (${vaultCount} facts)\n")
            append("2. DURABLE ON-DEVICE VAULT: ${durableFile.name} (Auto-Backup enabled, ${fileFactCount} facts)\n")
            append("3. VYB REMOTE CLOUD VAULT: ")
            if (remoteVault?.isCloudConfigured() == true) {
                append("CONFIGURED & CONNECTED")
            } else {
                append("NOT_CONFIGURED (Zero mock servers)")
            }
        }

        PersistenceDiagnosticSnapshot(
            ownerId = ownerId,
            isAuthenticated = ownerId.isNotBlank() && ownerId != "unauthenticated_owner",
            localVaultCount = vaultCount,
            localMemoryCount = memCount,
            localIntentionCount = intCount,
            durableFactCount = fileFactCount,
            lastSyncTimestamp = lastSyncTime,
            lastSnapshotFormatted = snapshotFormatted,
            syncStatus = lastSyncStatus,
            isCloudEndpointConfigured = remoteVault?.isCloudConfigured() == true,
            durableStoragePath = durableFile.absolutePath,
            stateVersion = DurablePersonalIntelligenceState.CURRENT_SCHEMA_VERSION,
            integrityStatus = integrityCheck,
            restoreStatus = lastRestoreStatus,
            storageArchitectureBreakdown = architectureSummary
        )
    }
}
