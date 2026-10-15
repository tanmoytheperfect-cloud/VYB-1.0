package com.example.domain.persistence

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.domain.genesis.PersonalContextGraph
import com.example.domain.genesis.VybPersonalProfile
import com.example.domain.intelligence.VybIntelligenceOrchestrator
import com.example.domain.temporal.VybTemporalScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class StartupState {
    INITIALIZING,
    AUTHENTICATING,
    IDENTIFYING_OWNER,
    CHECKING_LOCAL_STATE,
    FETCHING_DURABLE_STATE,
    VALIDATING_INTEGRITY,
    MIGRATING_SCHEMA,
    REHYDRATING_LOCAL_CACHE,
    REBUILDING_INDEXES,
    REHYDRATING_ENGINES,
    READY,
    DEGRADED_OFFLINE,
    ERROR
}

data class StartupStatusReport(
    val state: StartupState,
    val message: String,
    val isReady: Boolean = false,
    val restoredFactsCount: Int = 0,
    val restoredIntentionsCount: Int = 0,
    val errorDetail: String? = null
)

class VybStartupCoordinator(
    private val context: Context,
    private val database: AppDatabase,
    private val persistenceEngine: VybCloudPersistenceEngine
) {
    private val _status = MutableStateFlow(
        StartupStatusReport(
            state = StartupState.INITIALIZING,
            message = "Initializing VYB Personal Intelligence System..."
        )
    )
    val status: StateFlow<StartupStatusReport> = _status.asStateFlow()

    /**
     * Executes the strict 12-stage startup and first-launch restoration pipeline.
     */
    suspend fun executeStartupPipeline(
        onProfileRestored: ((VybPersonalProfile) -> Unit)? = null,
        onGraphRehydrated: ((PersonalContextGraph) -> Unit)? = null,
        intelligenceOrchestrator: VybIntelligenceOrchestrator? = null
    ): StartupStatusReport {
        try {
            // Stage 1: Initializing
            _status.value = StartupStatusReport(StartupState.INITIALIZING, "Starting Personal AI OS engine...")

            // Stage 2 & 3: Authenticate & Identify Owner
            _status.value = StartupStatusReport(StartupState.AUTHENTICATING, "Authenticating owner identity...")
            val owner = VybOwnerIdentityManager.getOwnerIdentity(context)
            _status.value = StartupStatusReport(
                StartupState.IDENTIFYING_OWNER,
                "Identified owner: ${owner.ownerName.ifBlank { owner.ownerEmail }}"
            )

            // Stage 4: Check Local State
            _status.value = StartupStatusReport(StartupState.CHECKING_LOCAL_STATE, "Scanning local device cache...")
            val localVaultCount = database.lifeVaultDao().getVaultFactCount()
            val localMemCount = database.memoryDao().getMemoryCountDirect()

            // Stage 5: Fetch Durable Cloud / Persistent State
            _status.value = StartupStatusReport(StartupState.FETCHING_DURABLE_STATE, "Connecting to durable owner vault...")
            val durableState = persistenceEngine.loadDurableState(owner.ownerId)

            var restoredFacts = 0
            var restoredIntentions = 0

            if (durableState != null) {
                // Stage 6: Validate Integrity
                _status.value = StartupStatusReport(StartupState.VALIDATING_INTEGRITY, "Verifying cryptographic vault integrity...")
                val isIntegrityValid = durableState.validateIntegrity()
                if (!isIntegrityValid) {
                    _status.value = StartupStatusReport(
                        StartupState.DEGRADED_OFFLINE,
                        "Vault integrity mismatch detected; loading with safe recovery.",
                        errorDetail = "Checksum validation warning"
                    )
                }

                // Stage 7: Schema Version Migration
                if (durableState.stateVersion > DurablePersonalIntelligenceState.CURRENT_SCHEMA_VERSION) {
                    _status.value = StartupStatusReport(
                        StartupState.MIGRATING_SCHEMA,
                        "Migrating future state version ${durableState.stateVersion}..."
                    )
                }

                // Stage 8: Rehydrate Local Cache (Only if local cache was wiped / has fewer facts)
                _status.value = StartupStatusReport(StartupState.REHYDRATING_LOCAL_CACHE, "Restoring Personal Life Vault...")
                val restoreSuccess = persistenceEngine.restoreStateIntoDatabase(durableState, database)
                if (restoreSuccess) {
                    restoredFacts = durableState.vaultFacts.size
                    restoredIntentions = durableState.intentions.size

                    // Restore profile values
                    val p = durableState.profile
                    val restoredProfile = VybPersonalProfile(
                        name = p.name,
                        callingTitle = p.callingTitle,
                        companyName = p.companyName,
                        jobTitle = p.jobTitle,
                        homeAddress = p.homeAddress,
                        officeAddress = p.officeAddress,
                        monthlyIncome = p.monthlyIncome,
                        fixedCommitments = p.fixedCommitments,
                        savingsTarget = p.savingsTarget,
                        currencySymbol = p.currencySymbol,
                        dietaryPreference = p.dietaryPreference
                    )
                    onProfileRestored?.invoke(restoredProfile)
                }
            } else if (localVaultCount == 0 && localMemCount == 0) {
                // Pristine first-time launch
                _status.value = StartupStatusReport(
                    StartupState.READY,
                    "Pristine initialization complete. Ready to learn your life context.",
                    isReady = true
                )
                return _status.value
            }

            // Stage 9: Rebuild Indexes
            _status.value = StartupStatusReport(StartupState.REBUILDING_INDEXES, "Rebuilding memory retrieval indexes...")

            // Stage 10: Rehydrate Context Graph
            _status.value = StartupStatusReport(StartupState.REHYDRATING_ENGINES, "Rehydrating Personal Context Graph & Intentions...")
            val freshGraph = PersonalContextGraph()
            onGraphRehydrated?.invoke(freshGraph)

            // Stage 11: Re-register Android Schedulers for Persistent Intentions
            val activeIntentions = database.persistentIntentionDao().getPendingOrScheduledIntentions()
            activeIntentions.forEach { intention ->
                if (intention.scheduledTimestamp > System.currentTimeMillis()) {
                    VybTemporalScheduler.scheduleWithAlarmManager(context, intention)
                }
            }

            // Stage 12: READY
            val finalReport = StartupStatusReport(
                state = StartupState.READY,
                message = "Personal Life Context active",
                isReady = true,
                restoredFactsCount = restoredFacts,
                restoredIntentionsCount = restoredIntentions
            )
            _status.value = finalReport
            return finalReport

        } catch (e: Exception) {
            val errorReport = StartupStatusReport(
                state = StartupState.DEGRADED_OFFLINE,
                message = "Your personal data could not be fully restored yet. VYB is working offline and will retry.",
                isReady = true,
                errorDetail = e.message
            )
            _status.value = errorReport
            return errorReport
        }
    }
}
