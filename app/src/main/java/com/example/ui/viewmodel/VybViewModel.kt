package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.model.IntentType
import com.example.data.model.VybIntentPayload
import com.example.data.remote.GeminiClient
import com.example.data.repository.VybRepository
import com.example.domain.brain.PersonalWorldModel
import com.example.domain.brain.VybBrainStatus
import com.example.domain.brain.WhatMattersNowSummary
import com.example.domain.brain.VybRecommendedAction
import com.example.domain.brain.LifeBriefing
import com.example.domain.context.*
import com.example.domain.genesis.*
import com.example.domain.intelligence.*
import com.example.domain.learning.*
import com.example.domain.permission.*
import com.example.domain.proactive.*
import com.example.domain.observer.VybDeviceObserver
import com.example.util.DeviceAppExecutionBridge
import com.example.util.JarvisCoreManager
import com.example.util.JarvisLocationTelemetry
import com.example.util.JarvisSystemTelemetry
import com.example.domain.cognitive.VybCognitiveEngine
import com.example.domain.cognitive.models.*
import com.example.domain.environment.VybLivingEnvironmentEngine
import com.example.domain.environment.model.*
import com.example.domain.persistence.*
import com.example.domain.state.AskVybState
import com.example.domain.temporal.VybTimeOfDay
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VybViewModel(application: Application) : AndroidViewModel(application) {

    val viewModelIdentity = "VybViewModel@${Integer.toHexString(System.identityHashCode(this))}"
    val viewModelCreationTime = System.currentTimeMillis()

    private val database = AppDatabase.getDatabase(application)
    val repository = VybRepository(database)
    private val prefs = application.getSharedPreferences("vyb_owner_prefs", Context.MODE_PRIVATE)

    // Durable Cloud & Persistence Layer
    val persistenceEngine = VybCloudPersistenceEngine(application)
    val startupCoordinator = VybStartupCoordinator(application, database, persistenceEngine)
    val startupStatus: StateFlow<StartupStatusReport> = startupCoordinator.status

    // Permission Manager & Observer
    val permissionManager = VybPermissionManager(application)
    val deviceObserver = VybDeviceObserver(
        context = application,
        database = database,
        cognitiveEngine = repository.cognitiveEngine,
        permissionManager = permissionManager
    )
    val backgroundAwarenessReport: StateFlow<com.example.domain.observer.BackgroundAwarenessReport> = deviceObserver.awarenessReport

    fun triggerUsageReconciliation() {
        viewModelScope.launch {
            deviceObserver.reconcileUsageEvents()
            repository.cognitiveEngine.lifeContextEngine.refreshState()
        }
    }

    // Living Environment & Activity Telemetry Engine
    val livingEnvironmentEngine = VybLivingEnvironmentEngine(application, repository.cognitiveEngine)
    val livingSignals: StateFlow<LivingSignals> = livingEnvironmentEngine.signals

    // Canonical Current Life State (Unified Reality: Telemetry + Context + Brain)
    val currentLifeState: StateFlow<CurrentLifeState> = repository.cognitiveEngine.lifeContextEngine.currentLifeState

    val allCandidatePatterns = database.candidatePatternDao().getAllPatternsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteCandidatePattern(patternId: String) {
        viewModelScope.launch {
            database.candidatePatternDao().deletePattern(patternId)
            repository.cognitiveEngine.lifeContextEngine.refreshState()
        }
    }

    fun correctCandidatePattern(patternId: String, correction: String, previousBelief: String) {
        viewModelScope.launch {
            repository.cognitiveEngine.correctionEngine.recordCorrection(
                targetPatternOrFact = patternId,
                userCorrectionStatement = correction,
                previousBelief = previousBelief
            )
            repository.cognitiveEngine.lifeContextEngine.refreshState()
        }
    }

    fun refreshLivingSignals() {
        livingEnvironmentEngine.refreshAllSignals()
        viewModelScope.launch {
            repository.cognitiveEngine.lifeContextEngine.refreshState()
        }
    }

    fun startLiveJourney(destination: String, travelMode: MobilityState = MobilityState.DRIVING) {
        repository.cognitiveEngine.liveJourneyEngine.startJourney(destination = destination, travelMode = travelMode)
        viewModelScope.launch {
            repository.cognitiveEngine.lifeContextEngine.refreshState()
        }
    }

    fun updateLiveJourneyProgress(speedKmh: Double, distanceDeltaKm: Double, currentRoad: String = "") {
        repository.cognitiveEngine.liveJourneyEngine.updateProgress(speedKmh, distanceDeltaKm, currentRoad)
        viewModelScope.launch {
            repository.cognitiveEngine.lifeContextEngine.refreshState()
        }
    }

    fun recordNavigationHandoff(destination: String) {
        repository.cognitiveEngine.liveJourneyEngine.recordNavigationHandoff(destination)
        viewModelScope.launch {
            repository.cognitiveEngine.lifeContextEngine.refreshState()
        }
    }

    fun completeLiveJourney() {
        repository.cognitiveEngine.liveJourneyEngine.completeJourney()
        viewModelScope.launch {
            repository.cognitiveEngine.lifeContextEngine.refreshState()
        }
    }

    fun applyPatternCorrection(target: String, correction: String, previous: String) {
        viewModelScope.launch {
            repository.cognitiveEngine.applyUserCorrection(target, correction, previous)
        }
    }

    fun auditSamsungS23Sensors(): SamsungS23SensorAudit {
        return livingEnvironmentEngine.auditSamsungS23Sensors()
    }

    val currentContextEngine = CurrentContextEngine(
        timelineDao = database.timelineDao(),
        learnedRoutineDao = database.learnedRoutineDao()
    )
    val routineLearningEngine = RoutineLearningEngine(
        timelineDao = database.timelineDao(),
        learnedRoutineDao = database.learnedRoutineDao()
    )
    val dailyReflectionEngine = DailyReflectionEngine(
        timelineDao = database.timelineDao(),
        dailyReflectionDao = database.dailyReflectionDao(),
        routineLearningEngine = routineLearningEngine
    )
    val weeklyAnalysisEngine = WeeklyRoutineAnalysisEngine(
        dailyReflectionDao = database.dailyReflectionDao(),
        weeklyPatternDao = database.weeklyPatternDao()
    )
    val proactiveDecisionEngine = ProactiveDecisionEngine()
    val anomalyDetectionEngine = AnomalyDetectionEngine()

    // Personal AI OS State Flows
    val recentTimelineEvents: StateFlow<List<TimelineEventEntity>> = repository.recentTimelineEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayDateString: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val todayTimelineEvents: StateFlow<List<TimelineEventEntity>> = repository.getTodayEventsFlow(todayDateString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLearnedRoutines: StateFlow<List<LearnedRoutineEntity>> = repository.allLearnedRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDailyReflections: StateFlow<List<DailyReflectionEntity>> = repository.allDailyReflections
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWeeklyPatterns: StateFlow<List<WeeklyPatternEntity>> = repository.allWeeklyPatterns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val learningSettings: StateFlow<LearningSettingEntity?> = repository.learningSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _currentContext = MutableStateFlow(VybCurrentContext())
    val currentContext: StateFlow<VybCurrentContext> = _currentContext.asStateFlow()

    private val _proactiveDecision = MutableStateFlow(
        ProactiveDecision(
            level = ProactiveActionLevel.INFORM,
            title = "Personal OS Initializing",
            description = "Observing legitimate device signals to assist your daily life.",
            priority = NotificationPriority.LOW
        )
    )
    val proactiveDecision: StateFlow<ProactiveDecision> = _proactiveDecision.asStateFlow()

    private val _permissionReports = MutableStateFlow<List<PermissionReport>>(emptyList())
    val permissionReports: StateFlow<List<PermissionReport>> = _permissionReports.asStateFlow()

    // Master VybBrain Intelligence Layer
    val vybBrain = repository.orchestrator.vybBrain
    val brainStatus: StateFlow<VybBrainStatus> = vybBrain.brainStatus

    private val _whatMattersNow = MutableStateFlow(vybBrain.getWhatMattersNow())
    val whatMattersNow: StateFlow<WhatMattersNowSummary> = _whatMattersNow.asStateFlow()

    private val _currentBriefing = MutableStateFlow(vybBrain.getMorningBriefing())
    val currentBriefing: StateFlow<LifeBriefing> = _currentBriefing.asStateFlow()

    private val _recommendedActions = MutableStateFlow(vybBrain.getRecommendedActions())
    val recommendedActions: StateFlow<List<VybRecommendedAction>> = _recommendedActions.asStateFlow()

    fun refreshBrainIntelligence() {
        _whatMattersNow.value = vybBrain.getWhatMattersNow()
        _currentBriefing.value = vybBrain.getMorningBriefing()
        _recommendedActions.value = vybBrain.getRecommendedActions()
    }

    // JARVIS Core Engine
    val jarvisCoreManager = JarvisCoreManager(application)
    val locationTelemetry: StateFlow<JarvisLocationTelemetry> = jarvisCoreManager.locationTelemetry
    val systemTelemetry: StateFlow<JarvisSystemTelemetry> = jarvisCoreManager.systemTelemetry
    val isSpeaking: StateFlow<Boolean> = jarvisCoreManager.isSpeaking

    // VYB Personal Genesis Engine & Universal Intelligence Layer
    val genesisEngine = PersonalGenesisEngine(application)

    private val _authorizedGenesisSources = MutableStateFlow<Set<GenesisDataSource>>(
        setOf(
            GenesisDataSource.IDENTITY,
            GenesisDataSource.DEVICE,
            GenesisDataSource.SUPPORTED_FINANCIAL_DATA,
            GenesisDataSource.FILES,
            GenesisDataSource.PHOTOS,
            GenesisDataSource.SUPPORTED_SERVICES
        )
    )
    val authorizedGenesisSources: StateFlow<Set<GenesisDataSource>> = _authorizedGenesisSources.asStateFlow()

    private val _genesisSourceSpecs = MutableStateFlow<List<GenesisSourceSpec>>(emptyList())
    val genesisSourceSpecs: StateFlow<List<GenesisSourceSpec>> = _genesisSourceSpecs.asStateFlow()

    private val _personalProfile = MutableStateFlow(
        run {
            val owner = VybOwnerIdentityManager.getOwnerIdentity(application)
            val name = prefs.getString("owner_name", "")?.ifBlank { owner.ownerName } ?: owner.ownerName
            val email = prefs.getString("owner_email", "")?.ifBlank { owner.ownerEmail } ?: owner.ownerEmail
            val company = prefs.getString("owner_company", "")?.ifBlank { "Metropoli Fashions" } ?: "Metropoli Fashions"
            val jobTitle = prefs.getString("owner_job_title", "")?.ifBlank { "Managing Director" } ?: "Managing Director"
            val homeAddress = prefs.getString("owner_home_address", "")?.ifBlank { "Salt Lake, Kolkata, West Bengal, India" } ?: "Salt Lake, Kolkata, West Bengal, India"
            val officeAddress = prefs.getString("owner_office_address", "")?.ifBlank { "Sector V, Salt Lake, Kolkata, West Bengal, India" } ?: "Sector V, Salt Lake, Kolkata, West Bengal, India"
            val monthlyIncome = prefs.getFloat("monthly_income", 0f).toDouble().let { if (it > 0) it else 250000.0 }
            val fixedCommitments = prefs.getFloat("fixed_commitments", 0f).toDouble().let { if (it > 0) it else 65000.0 }
            val savingsTarget = prefs.getFloat("monthly_savings_target", 0f).toDouble().let { if (it > 0) it else 50000.0 }
            val currencySymbol = prefs.getString("currency_symbol", "₹") ?: "₹"
            val diet = prefs.getString("diet_pref", "High-Protein Balanced") ?: "High-Protein Balanced"

            VybPersonalProfile(
                name = name,
                callingTitle = prefs.getString("owner_title", "Sir") ?: "Sir",
                companyName = company,
                jobTitle = jobTitle,
                homeAddress = homeAddress,
                officeAddress = officeAddress,
                monthlyIncome = monthlyIncome,
                fixedCommitments = fixedCommitments,
                savingsTarget = savingsTarget,
                currencySymbol = currencySymbol,
                dietaryPreference = diet
            )
        }
    )
    val personalProfile: StateFlow<VybPersonalProfile> = _personalProfile.asStateFlow()

    private val _contextGraph = MutableStateFlow(PersonalContextGraph())
    val contextGraph: StateFlow<PersonalContextGraph> = _contextGraph.asStateFlow()

    private val _discoveredPatterns = MutableStateFlow<List<PatternDiscoveryItem>>(emptyList())
    val discoveredPatterns: StateFlow<List<PatternDiscoveryItem>> = _discoveredPatterns.asStateFlow()

    private val _personalDigitalTwin = MutableStateFlow<PersonalDigitalTwin?>(null)
    val personalDigitalTwin: StateFlow<PersonalDigitalTwin?> = _personalDigitalTwin.asStateFlow()

    private val _universalInstanceMetadata = MutableStateFlow(
        genesisEngine.generateUniversalInstanceMetadata(prefs.getString("owner_name", "") ?: "Owner")
    )
    val universalInstanceMetadata: StateFlow<UniversalInstanceMetadata> = _universalInstanceMetadata.asStateFlow()

    private val _continuousPersonalizationStage = MutableStateFlow("OBSERVE")
    val continuousPersonalizationStage: StateFlow<String> = _continuousPersonalizationStage.asStateFlow()

    // VYB Personal Intelligence & Action Engine State
    val intelligenceOrchestrator: VybIntelligenceOrchestrator = repository.intelligenceOrchestrator

    private val _lastIntelligenceResult = MutableStateFlow<IntelligenceOrchestrationResult?>(null)
    val lastIntelligenceResult: StateFlow<IntelligenceOrchestrationResult?> = _lastIntelligenceResult.asStateFlow()

    private val _activeActionPlans = MutableStateFlow<List<ActionPlan>>(emptyList())
    val activeActionPlans: StateFlow<List<ActionPlan>> = _activeActionPlans.asStateFlow()

    private val _allCapabilities = MutableStateFlow<List<VybCapabilityDeclaration>>(emptyList())
    val allCapabilities: StateFlow<List<VybCapabilityDeclaration>> = _allCapabilities.asStateFlow()

    private val _pendingConfirmationPrompt = MutableStateFlow<HumanConfirmationPrompt?>(null)
    val pendingConfirmationPrompt: StateFlow<HumanConfirmationPrompt?> = _pendingConfirmationPrompt.asStateFlow()

    private val _activeTaskState = MutableStateFlow<VybTaskExecutionState?>(null)
    val activeTaskState: StateFlow<VybTaskExecutionState?> = _activeTaskState.asStateFlow()

    private val _actionLedger = MutableStateFlow<List<ActionLedgerEntry>>(emptyList())
    val actionLedger: StateFlow<List<ActionLedgerEntry>> = _actionLedger.asStateFlow()

    private val _autonomyLevel = MutableStateFlow(AutonomyLevel.LEVEL_3_ACT_WITH_CONFIRMATION)
    val autonomyLevel: StateFlow<AutonomyLevel> = _autonomyLevel.asStateFlow()

    // Onboarding State
    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("is_onboarding_completed", true))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    // Arrival animation flag (plays only once on cold start)
    var hasCompletedArrivalAnimation: Boolean = false

    // Owner Profile State - Anchored to PersonalProfile
    private val _ownerName = MutableStateFlow(_personalProfile.value.name)
    val ownerName: StateFlow<String> = _ownerName.asStateFlow()

    private val _ownerTitle = MutableStateFlow(_personalProfile.value.callingTitle)
    val ownerTitle: StateFlow<String> = _ownerTitle.asStateFlow()

    private val _ownerEmail = MutableStateFlow(
        prefs.getString("owner_email", "")?.ifBlank { VybOwnerIdentityManager.getOwnerIdentity(application).ownerEmail }
            ?: VybOwnerIdentityManager.getOwnerIdentity(application).ownerEmail
    )
    val ownerEmail: StateFlow<String> = _ownerEmail.asStateFlow()

    private val _ownerPhone = MutableStateFlow(prefs.getString("owner_phone", "") ?: "")
    val ownerPhone: StateFlow<String> = _ownerPhone.asStateFlow()

    private val _ownerHomeAddress = MutableStateFlow(_personalProfile.value.homeAddress)
    val ownerHomeAddress: StateFlow<String> = _ownerHomeAddress.asStateFlow()

    private val _ownerOfficeAddress = MutableStateFlow(_personalProfile.value.officeAddress)
    val ownerOfficeAddress: StateFlow<String> = _ownerOfficeAddress.asStateFlow()

    private val _ownerJobTitle = MutableStateFlow(_personalProfile.value.jobTitle)
    val ownerJobTitle: StateFlow<String> = _ownerJobTitle.asStateFlow()

    private val _ownerCompanyName = MutableStateFlow(_personalProfile.value.companyName)
    val ownerCompanyName: StateFlow<String> = _ownerCompanyName.asStateFlow()

    private val _ownerCity = MutableStateFlow(prefs.getString("owner_city", "Kolkata") ?: "Kolkata")
    val ownerCity: StateFlow<String> = _ownerCity.asStateFlow()

    // Financial Planning Context
    private val _monthlyIncome = MutableStateFlow(_personalProfile.value.monthlyIncome)
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    private val _fixedCommitments = MutableStateFlow(_personalProfile.value.fixedCommitments)
    val fixedCommitments: StateFlow<Double> = _fixedCommitments.asStateFlow()

    private val _monthlySavingsTarget = MutableStateFlow(_personalProfile.value.savingsTarget)
    val monthlySavingsTarget: StateFlow<Double> = _monthlySavingsTarget.asStateFlow()

    private val _currencySymbol = MutableStateFlow(_personalProfile.value.currencySymbol)
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()
    val currency: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _aiTone = MutableStateFlow(prefs.getString("ai_tone", "EXECUTIVE_PA") ?: "EXECUTIVE_PA")
    val aiTone: StateFlow<String> = _aiTone.asStateFlow()

    private val _dietPreference = MutableStateFlow(_personalProfile.value.dietaryPreference)
    val dietPreference: StateFlow<String> = _dietPreference.asStateFlow()

    // Security Verification / Lock State
    private val _isSecurityLockEnabled = MutableStateFlow(prefs.getBoolean("security_lock_enabled", false))
    val isSecurityLockEnabled: StateFlow<Boolean> = _isSecurityLockEnabled.asStateFlow()

    private val _securityPin = MutableStateFlow(prefs.getString("security_pin", "1234") ?: "1234")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Fluid Privacy Firewall State & Policy Controls
    val isLocalOnlyMode: StateFlow<Boolean> = com.example.domain.privacy.FluidPrivacyFirewall.isLocalOnlyMode
    val pendingTransferProposal: StateFlow<com.example.domain.privacy.OutboundTransferProposal?> = com.example.domain.privacy.FluidPrivacyFirewall.pendingProposal
    val privacyAuditLog: StateFlow<List<com.example.domain.privacy.PrivacyAuditRecord>> = com.example.domain.privacy.FluidPrivacyFirewall.auditLog

    fun setLocalOnlyMode(enabled: Boolean) {
        com.example.domain.privacy.FluidPrivacyFirewall.setLocalOnlyMode(enabled)
    }

    fun approveTransferProposal(proposalId: String): com.example.domain.privacy.TransferAuthorizationToken? {
        return com.example.domain.privacy.FluidPrivacyFirewall.approveProposal(proposalId)
    }

    fun denyTransferProposal(proposalId: String, reason: String = "Declined by user") {
        com.example.domain.privacy.FluidPrivacyFirewall.denyProposal(proposalId, reason)
    }

    fun cancelTransferProposal(reason: String = "Cancelled by user") {
        com.example.domain.privacy.FluidPrivacyFirewall.cancelPendingProposal(reason)
    }

    fun requestCloudReasoningWithFirewall(prompt: String, context: Context? = null) {
        if (com.example.domain.privacy.FluidPrivacyFirewall.isLocalOnlyMode.value) {
            processUserCommand(prompt, context)
            return
        }

        val destination = com.example.domain.privacy.TransferDestination(
            type = com.example.domain.privacy.TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )
        val categories = setOf(
            com.example.domain.privacy.DataCategory.GENERAL_QUERY,
            com.example.domain.privacy.DataCategory.PERSONAL_LIFE_CONTEXT,
            com.example.domain.privacy.DataCategory.FINANCIAL_DATA,
            com.example.domain.privacy.DataCategory.FAMILY_AND_RELATIONSHIPS
        )
        val proposal = com.example.domain.privacy.FluidPrivacyFirewall.createProposal(
            destination = destination,
            purpose = "Cloud AI Reasoning with on-device context synthesis",
            categories = categories,
            payload = prompt
        )
        com.example.domain.privacy.FluidPrivacyFirewall.submitProposalForUserApproval(proposal)
    }

    fun onProposalAuthorized(
        proposalId: String,
        token: com.example.domain.privacy.TransferAuthorizationToken,
        finalPayload: String,
        context: Context? = null
    ) {
        processUserCommand(
            prompt = finalPayload,
            context = context,
            authorizationToken = token
        )
    }

    // Time-Aware Celestial Environment State & Developer/Test Override
    private val _timeOfDayOverride = MutableStateFlow<VybTimeOfDay?>(null)
    val timeOfDayOverride: StateFlow<VybTimeOfDay?> = _timeOfDayOverride.asStateFlow()

    fun setTimeOfDayOverride(override: VybTimeOfDay?) {
        _timeOfDayOverride.value = override
    }

    fun cycleTimeOfDayOverride() {
        val current = _timeOfDayOverride.value
        val next = when (current) {
            null -> VybTimeOfDay.DAWN
            VybTimeOfDay.DAWN -> VybTimeOfDay.MORNING
            VybTimeOfDay.MORNING -> VybTimeOfDay.DAY
            VybTimeOfDay.DAY -> VybTimeOfDay.EVENING
            VybTimeOfDay.EVENING -> VybTimeOfDay.NIGHT
            VybTimeOfDay.NIGHT -> null // back to real-time auto
        }
        _timeOfDayOverride.value = next
    }

    // Owner Accounts Vault
    val allOwnerAccounts: StateFlow<List<OwnerAccountEntity>> = repository.allOwnerAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val authorizedAiAccounts: StateFlow<List<OwnerAccountEntity>> = repository.authorizedAiAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State Flows
    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<GoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRoutines: StateFlow<List<RoutineEntity>> = repository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActions: StateFlow<List<ActionRecordEntity>> = repository.allActions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingConfirmationActions: StateFlow<List<ActionRecordEntity>> = repository.pendingConfirmationActions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemories: StateFlow<List<MemoryItemEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allVaultItems: StateFlow<List<LifeVaultItemEntity>> = repository.allVaultItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingVaultConflicts: StateFlow<List<LifeVaultItemEntity>> = repository.pendingVaultConflicts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFamilyMembers: StateFlow<List<FamilyMemberEntity>> = repository.allFamilyMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTasks: StateFlow<List<TaskItemEntity>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allShoppingItems: StateFlow<List<ShoppingItemEntity>> = repository.allShoppingItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalExpenseAmount: StateFlow<Double> = repository.totalExpenseAmount
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allPersistentIntentions: StateFlow<List<PersistentIntentionEntity>> = database.persistentIntentionDao().getAllIntentionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cognitiveEngine = repository.cognitiveEngine

    private val _currentNextUp = MutableStateFlow<com.example.domain.cognitive.models.NextUpItem?>(null)
    val currentNextUp: StateFlow<com.example.domain.cognitive.models.NextUpItem?> = _currentNextUp.asStateFlow()

    fun refreshNextUp(nowMillis: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            _currentNextUp.value = cognitiveEngine.resolveNextUpForHome(nowMillis)
        }
    }

    init {
        viewModelScope.launch {
            // Stage 1-12: Execute startup & first-launch restoration pipeline
            startupCoordinator.executeStartupPipeline(
                onProfileRestored = { restored ->
                    _personalProfile.value = restored
                    _ownerName.value = restored.name
                    _ownerTitle.value = restored.callingTitle
                    _ownerCompanyName.value = restored.companyName
                    _ownerJobTitle.value = restored.jobTitle
                    _ownerHomeAddress.value = restored.homeAddress
                    _ownerOfficeAddress.value = restored.officeAddress
                    _monthlyIncome.value = restored.monthlyIncome
                    _fixedCommitments.value = restored.fixedCommitments
                    _monthlySavingsTarget.value = restored.savingsTarget
                    _currencySymbol.value = restored.currencySymbol
                    _dietPreference.value = restored.dietaryPreference
                },
                onGraphRehydrated = { graph ->
                    _contextGraph.value = graph
                },
                intelligenceOrchestrator = intelligenceOrchestrator
            )

            // Ensure verified profile facts are synced to Life Vault
            val owner = VybOwnerIdentityManager.getOwnerIdentity(application)
            _ownerEmail.value = owner.ownerEmail
            _ownerPhone.value = prefs.getString("owner_phone", "") ?: ""
            repository.syncProfileFactsToVault(_personalProfile.value, _ownerEmail.value, _ownerPhone.value)

            // Save durable snapshot of initial state
            val initialSnapshot = persistenceEngine.buildSnapshotFromDatabase(database, _personalProfile.value, owner.ownerId, _ownerEmail.value, _ownerPhone.value, _aiTone.value)
            persistenceEngine.saveAndSyncSnapshot(initialSnapshot)

            repository.seedInitialOperatingLayerDataIfEmpty()
            seedInitialTimelineAndSettingsIfEmpty()
            syncWorldModelToBrain()
            deviceObserver.startObserving()
            refreshPermissionReports()
            refreshGenesisState()
            refreshCapabilities()
            refreshCurrentContext()
            runRoutineLearning()
            try {
                ensureActiveConversation()
            } catch (e: Exception) {
                Log.e("VYB_TRACE", "Error in init ensureActiveConversation", e)
            }
        }
        Log.d("VYB_TRACE", "VIEWMODEL_INIT identity=$viewModelIdentity time=$viewModelCreationTime")
    }

    private suspend fun seedInitialTimelineAndSettingsIfEmpty() {
        if (repository.learningSettingsFlow.firstOrNull() == null) {
            repository.insertLearningSettings(
                LearningSettingEntity(
                    id = 1,
                    isLearningPaused = false,
                    autoReflectAtMidnight = true,
                    proactiveInterruptionLevel = "BALANCED",
                    allowUsageStatsObservation = true,
                    allowCalendarObservation = true,
                    allowLocationObservation = true,
                    allowNotificationObservation = true,
                    allowDeviceLifecycleObservation = true
                )
            )
        }
    }

    fun refreshPermissionReports() {
        _permissionReports.value = permissionManager.getAllPermissionReports()
    }

    fun refreshCurrentContext() {
        viewModelScope.launch {
            val calendarEvents = deviceObserver.scanCalendarIfPermitted()
            val ctx = currentContextEngine.evaluateCurrentContext(calendarEvents)
            _currentContext.value = ctx

            val anomaly = anomalyDetectionEngine.detectAnomalies(ctx)
            val decision = proactiveDecisionEngine.evaluateProactiveNeed(ctx, anomaly)
            _proactiveDecision.value = decision
        }
    }

    fun runRoutineLearning() {
        viewModelScope.launch {
            val settings = repository.learningSettingsFlow.firstOrNull()
            if (settings?.isLearningPaused == true) return@launch

            routineLearningEngine.analyzeAndLearnRoutines()
            refreshCurrentContext()
        }
    }

    fun triggerDailyReflection(targetDate: String? = null) {
        viewModelScope.launch {
            dailyReflectionEngine.performDailyReflection(targetDate)
            weeklyAnalysisEngine.performWeeklyAnalysis()
            refreshCurrentContext()
        }
    }

    fun setLearningPaused(isPaused: Boolean) {
        viewModelScope.launch {
            repository.setLearningPaused(isPaused)
        }
    }

    fun clearTodayTimeline() {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.clearTodayTimeline(todayStr)
            refreshCurrentContext()
        }
    }

    fun clearAllTimeline() {
        viewModelScope.launch {
            repository.clearAllTimeline()
            refreshCurrentContext()
        }
    }

    fun deleteLearnedRoutine(routineId: String) {
        viewModelScope.launch {
            repository.deleteLearnedRoutine(routineId)
            refreshCurrentContext()
        }
    }

    fun clearAllLearnedRoutines() {
        viewModelScope.launch {
            repository.clearAllLearnedRoutines()
            refreshCurrentContext()
        }
    }

    fun recordVybInteraction(summary: String, category: String = "CONVERSATION") {
        deviceObserver.recordVybInteraction(summary, category)
        refreshCurrentContext()
    }

    private fun syncWorldModelToBrain() {
        viewModelScope.launch {
            val family = allFamilyMembers.value
            val routines = allRoutines.value
            val goals = allGoals.value
            val projects = allProjects.value
            val memories = allMemories.value
            val accounts = allOwnerAccounts.value
            val vaultItems = allVaultItems.value

            val model = PersonalWorldModel.fromUserData(
                prefs = prefs,
                familyList = family,
                routinesList = routines,
                goalsList = goals,
                projectsList = projects,
                memoriesList = memories,
                accountsList = accounts,
                vaultList = vaultItems
            )
            repository.orchestrator.cognitiveBrain.updateWorldModel(model)

            val lifeModel = com.example.domain.brain.PersonalLifeModel.fromUserData(
                prefs = prefs,
                familyList = family,
                routinesList = routines,
                goalsList = goals,
                projectsList = projects,
                memoriesList = memories,
                accountsList = accounts,
                vaultList = vaultItems,
                learnedRoutines = allLearnedRoutines.value,
                timelineEvents = recentTimelineEvents.value
            )
            repository.orchestrator.vybBrain.updateLifeModel(lifeModel)
        }
    }

    private val _currentConversationId = MutableStateFlow<Long?>(null)
    val currentConversationId: StateFlow<Long?> = _currentConversationId.asStateFlow()

    private val _currentConversation = MutableStateFlow<ConversationEntity?>(null)
    val currentConversation: StateFlow<ConversationEntity?> = _currentConversation.asStateFlow()

    val currentMessages: StateFlow<List<MessageEntity>> = _currentConversationId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesForConversation(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Real-Time Runtime Trace Pipeline State (S23 Verification)
    private val traceCounter = java.util.concurrent.atomic.AtomicInteger(1)
    fun generateNextTraceId(): String {
        return String.format(Locale.US, "ASK_TRACE_%03d", traceCounter.getAndIncrement())
    }

    private val _latestResponseText = MutableStateFlow("")
    val latestResponseText: StateFlow<String> = _latestResponseText.asStateFlow()

    private val _latestTraceStatus = MutableStateFlow("IDLE")
    val latestTraceStatus: StateFlow<String> = _latestTraceStatus.asStateFlow()

    private val _latestTraceId = MutableStateFlow("ASK_TRACE_000")
    val latestTraceId: StateFlow<String> = _latestTraceId.asStateFlow()

    private val _askVybState = MutableStateFlow(AskVybState.IDLE)
    val askVybState: StateFlow<AskVybState> = _askVybState.asStateFlow()

    // User Customization / Profile state
    private val _assistantTone = MutableStateFlow(prefs.getString("assistant_tone", "Executive Chief of Staff") ?: "Executive Chief of Staff")
    val assistantTone: StateFlow<String> = _assistantTone.asStateFlow()

    private val _userDietPreference = MutableStateFlow(prefs.getString("diet_pref", "High-Protein Balanced") ?: "High-Protein Balanced")
    val userDietPreference: StateFlow<String> = _userDietPreference.asStateFlow()

    fun completeOnboarding() {
        _isOnboardingCompleted.value = true
        prefs.edit().putBoolean("is_onboarding_completed", true).apply()
        syncWorldModelToBrain()
    }

    fun resetOnboarding() {
        _isOnboardingCompleted.value = false
        prefs.edit().putBoolean("is_onboarding_completed", false).apply()
    }

    fun saveComprehensiveProfile(
        name: String,
        title: String,
        email: String,
        phone: String,
        homeAddress: String,
        officeAddress: String,
        jobTitle: String,
        company: String,
        city: String,
        monthlyIncome: Double,
        fixedCommitments: Double,
        savingsTarget: Double,
        currency: String
    ) {
        _ownerName.value = name
        _ownerTitle.value = title
        _ownerEmail.value = email
        _ownerPhone.value = phone
        _ownerHomeAddress.value = homeAddress
        _ownerOfficeAddress.value = officeAddress
        _ownerJobTitle.value = jobTitle
        _ownerCompanyName.value = company
        _ownerCity.value = city
        _monthlyIncome.value = monthlyIncome
        _fixedCommitments.value = fixedCommitments
        _monthlySavingsTarget.value = savingsTarget
        _currencySymbol.value = currency

        prefs.edit()
            .putString("owner_name", name)
            .putString("owner_title", title)
            .putString("owner_email", email)
            .putString("owner_phone", phone)
            .putString("owner_home_address", homeAddress)
            .putString("owner_office_address", officeAddress)
            .putString("owner_job_title", jobTitle)
            .putString("owner_company", company)
            .putString("owner_city", city)
            .putFloat("monthly_income", monthlyIncome.toFloat())
            .putFloat("fixed_commitments", fixedCommitments.toFloat())
            .putFloat("monthly_savings_target", savingsTarget.toFloat())
            .putString("currency_symbol", currency)
            .apply()

        _personalProfile.value = VybPersonalProfile(
            name = name,
            callingTitle = title,
            homeAddress = homeAddress,
            officeAddress = officeAddress,
            jobTitle = jobTitle,
            companyName = company,
            monthlyIncome = monthlyIncome,
            fixedCommitments = fixedCommitments,
            savingsTarget = savingsTarget,
            currencySymbol = currency,
            dietaryPreference = _dietPreference.value
        )

        val ownerId = VybOwnerIdentityManager.computeStableOwnerId(email.ifBlank { name })
        VybOwnerIdentityManager.setOwnerIdentity(
            getApplication(),
            VybOwnerIdentity(
                ownerId = ownerId,
                ownerEmail = email,
                ownerName = name
            )
        )

        viewModelScope.launch {
            repository.syncProfileFactsToVault(_personalProfile.value, email, phone)
            val snap = persistenceEngine.buildSnapshotFromDatabase(database, _personalProfile.value, ownerId, email, phone, _aiTone.value)
            persistenceEngine.saveAndSyncSnapshot(snap)
        }

        syncWorldModelToBrain()
        refreshGenesisState()
    }

    fun refreshGenesisState() {
        viewModelScope.launch {
            val hasFin = _monthlyIncome.value > 0
            val hasAcc = allOwnerAccounts.value.isNotEmpty()
            val specs = genesisEngine.getInitialSourceSpecs(
                ownerName = _ownerName.value,
                hasFinancialData = hasFin,
                hasConnectedAccounts = hasAcc,
                authorizedTypes = _authorizedGenesisSources.value
            )
            _genesisSourceSpecs.value = specs

            val graph = genesisEngine.buildContextGraph(
                profile = _personalProfile.value,
                familyList = allFamilyMembers.value,
                taskList = allTasks.value,
                routineList = allRoutines.value,
                goalList = allGoals.value
            )
            _contextGraph.value = graph

            val patterns = genesisEngine.discoverPatterns(
                timelineEvents = recentTimelineEvents.value,
                existingRoutines = allRoutines.value
            )
            _discoveredPatterns.value = patterns

            _personalDigitalTwin.value = genesisEngine.buildPersonalDigitalTwin(
                profile = _personalProfile.value,
                authorizedSources = _authorizedGenesisSources.value.toList()
            )
        }
    }

    fun toggleGenesisSource(type: GenesisDataSource, isAuthorized: Boolean) {
        val current = _authorizedGenesisSources.value.toMutableSet()
        if (isAuthorized) current.add(type) else current.remove(type)
        _authorizedGenesisSources.value = current
        refreshGenesisState()
    }

    fun updatePersonalProfile(profile: VybPersonalProfile) {
        _personalProfile.value = profile
        saveComprehensiveProfile(
            name = profile.name,
            title = profile.callingTitle,
            email = _ownerEmail.value,
            phone = _ownerPhone.value,
            homeAddress = profile.homeAddress,
            officeAddress = profile.officeAddress,
            jobTitle = profile.jobTitle,
            company = profile.companyName,
            city = _ownerCity.value,
            monthlyIncome = profile.monthlyIncome,
            fixedCommitments = profile.fixedCommitments,
            savingsTarget = profile.savingsTarget,
            currency = profile.currencySymbol
        )
        refreshGenesisState()
    }

    fun confirmPattern(patternId: String, saveAsRoutine: Boolean) {
        val list = _discoveredPatterns.value.map {
            if (it.id == patternId) it.copy(isConfirmed = true) else it
        }
        _discoveredPatterns.value = list
        if (saveAsRoutine) {
            val item = list.firstOrNull { it.id == patternId }
            if (item != null) {
                addRoutine(
                    title = item.category,
                    timeOfDay = "07:30 AM",
                    daysOfWeek = "Mon, Tue, Wed, Thu, Fri",
                    description = item.observation,
                    prompt = "Initiate routine for ${item.category}",
                    category = "LEARNED_ROUTINE"
                )
            }
        }
        _continuousPersonalizationStage.value = "REMEMBER"
    }

    fun dismissPattern(patternId: String) {
        _discoveredPatterns.value = _discoveredPatterns.value.map {
            if (it.id == patternId) it.copy(isDismissed = true) else it
        }
    }

    fun setPersonalityStyle(style: PersonalityStyle) {
        val updated = _personalProfile.value.copy(communicationStyle = style)
        _personalProfile.value = updated
        _assistantTone.value = style.title
        prefs.edit().putString("assistant_tone", style.title).apply()
        refreshGenesisState()
    }

    fun exportPersonalIntelligenceArchive(): String {
        val pkg = genesisEngine.createMigrationArchive(
            profile = _personalProfile.value,
            memories = allMemories.value,
            instanceMetadata = _universalInstanceMetadata.value
        )
        return pkg.payloadJson
    }

    fun importPersonalIntelligenceArchive(jsonStr: String): Boolean {
        return try {
            val root = org.json.JSONObject(jsonStr)
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                val importedProfile = _personalProfile.value.copy(
                    name = p.optString("name", _personalProfile.value.name),
                    callingTitle = p.optString("title", _personalProfile.value.callingTitle),
                    companyName = p.optString("company", _personalProfile.value.companyName),
                    jobTitle = p.optString("jobTitle", _personalProfile.value.jobTitle),
                    monthlyIncome = p.optDouble("income", _personalProfile.value.monthlyIncome),
                    fixedCommitments = p.optDouble("commitments", _personalProfile.value.fixedCommitments),
                    savingsTarget = p.optDouble("savingsTarget", _personalProfile.value.savingsTarget),
                    currencySymbol = p.optString("currency", _personalProfile.value.currencySymbol)
                )
                updatePersonalProfile(importedProfile)
            }
            refreshGenesisState()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun wipeAllPersonalIntelligence() {
        resetVybEntirely()
        _isOnboardingCompleted.value = false
        prefs.edit().putBoolean("is_onboarding_completed", false).apply()
        _personalProfile.value = VybPersonalProfile()
        _authorizedGenesisSources.value = setOf(
            GenesisDataSource.IDENTITY,
            GenesisDataSource.DEVICE
        )
        refreshGenesisState()
    }

    fun selectConversation(id: Long) {
        _currentConversationId.value = id
        viewModelScope.launch {
            _currentConversation.value = repository.getConversationById(id)
        }
    }

    fun startNewConversation(initialPrompt: String, onStarted: (Long) -> Unit) {
        if (initialPrompt.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                syncWorldModelToBrain()
                val conversationId = repository.createConversation(initialPrompt)
                _currentConversationId.value = conversationId
                _currentConversation.value = repository.getConversationById(conversationId)
                onStarted(conversationId)

                val result = repository.sendUserMessage(conversationId, initialPrompt, getApplication<Application>())
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to generate response"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to start conversation"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun ensureActiveConversation(): Long {
        val current = _currentConversationId.value
        if (current != null) return current

        val latest = repository.getLatestConversation()
        val convId = if (latest != null) {
            latest.id
        } else {
            repository.createConversation("Ask VYB")
        }
        _currentConversationId.value = convId
        _currentConversation.value = repository.getConversationById(convId)
        Log.d("VYB_TRACE", "Active conversation ensured id=$convId")
        return convId
    }

    /**
     * Canonical conversational command processor for Ask VYB pipeline (Parts 3, 7, 8, 12, 14).
     * Connects UI directly to VybIntelligenceOrchestrator and VybRepository.
     */
    fun processUserCommand(
        prompt: String,
        context: Context? = null,
        traceId: String = generateNextTraceId(),
        authorizationToken: com.example.domain.privacy.TransferAuthorizationToken? = null
    ): Job {
        val cleanPrompt = prompt.trim()
        Log.d("VYB_TRACE", "[$traceId] PROCESS_USER_COMMAND_ENTERED (chars=${cleanPrompt.length})")
        _latestTraceId.value = traceId
        _latestTraceStatus.value = "[$traceId] PROCESS_USER_COMMAND_ENTERED"
        _askVybState.value = AskVybState.SUBMITTING

        return viewModelScope.launch {
            Log.d("VYB_TRACE", "[$traceId] COROUTINE_STARTED")
            Log.d("VYB_TRACE", "[$traceId] VIEWMODEL_PROCESS_STARTED")
            _isLoading.value = true
            _askVybState.value = AskVybState.THINKING
            _errorMessage.value = null

            try {
                val convId = ensureActiveConversation()
                syncWorldModelToBrain()

                Log.d("VYB_TRACE", "[$traceId] ORCHESTRATOR_CALLED")
                _latestTraceStatus.value = "[$traceId] ORCHESTRATOR_CALLED"

                val result = repository.sendUserMessage(
                    conversationId = convId,
                    userPrompt = cleanPrompt,
                    appContext = context ?: getApplication<Application>(),
                    traceId = traceId,
                    authorizationToken = authorizationToken
                )

                if (result.isSuccess) {
                    val message = result.getOrThrow()
                    Log.d("VYB_TRACE", "[$traceId] RESPONSE_STATE_UPDATED (chars=${message.content.length})")
                    _latestResponseText.value = message.content
                    Log.d("VYB_TRACE", "[$traceId] VIEWMODEL_STATE_UPDATED")
                    _latestTraceStatus.value = "[$traceId] VIEWMODEL_STATE_UPDATED"
                    _currentConversation.value = repository.getConversationById(convId)
                    _askVybState.value = AskVybState.SUCCESS
                } else {
                    val ex = result.exceptionOrNull()
                    val errorMsg = ex?.message ?: "Failed to generate response"
                    Log.e("VYB_TRACE", "[$traceId] REPOSITORY_RESULT_FAILURE: class=${ex?.javaClass?.name} msg=$errorMsg", ex)
                    _errorMessage.value = errorMsg
                    _latestResponseText.value = "Error: $errorMsg"
                    _latestTraceStatus.value = "[$traceId] ERROR: $errorMsg"
                    _askVybState.value = AskVybState.ERROR
                }
            } catch (t: Throwable) {
                Log.e("VYB_TRACE", "[$traceId] VIEWMODEL_COROUTINE_EXCEPTION: class=${t.javaClass.name} msg=${t.message}", t)
                val errorMsg = t.message ?: "Unexpected error"
                _errorMessage.value = errorMsg
                _latestResponseText.value = "Exception: $errorMsg"
                _latestTraceStatus.value = "[$traceId] EXCEPTION: $errorMsg"
                _askVybState.value = AskVybState.ERROR
            } finally {
                _isLoading.value = false
                if (_askVybState.value == AskVybState.THINKING || _askVybState.value == AskVybState.SUBMITTING) {
                    _askVybState.value = AskVybState.IDLE
                }
            }
        }
    }

    fun sendMessage(prompt: String) {
        processUserCommand(prompt)
    }

    fun retryLastMessage() {
        val lastUserMsg = currentMessages.value.lastOrNull { it.sender == "USER" } ?: return
        processUserCommand(lastUserMsg.content)
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_currentConversationId.value == id) {
                _currentConversationId.value = null
                _currentConversation.value = null
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAllConversations()
            _currentConversationId.value = null
            _currentConversation.value = null
        }
    }

    // App Execution & Safety Approvals
    fun executeAndApproveAction(context: Context, action: ActionRecordEntity) {
        viewModelScope.launch {
            // 1. Dispatch real device intent
            val res = when (action.actionType.uppercase()) {
                "TRANSPORT_DISPATCH", "RIDE_DISPATCH", "OPEN_RIDE_PROVIDER" -> {
                    val providerName = when {
                        action.title.contains("Yatri", ignoreCase = true) || action.consequenceSummary.contains("Yatri", ignoreCase = true) -> "Yatri Sathi"
                        action.title.contains("Rapido", ignoreCase = true) || action.consequenceSummary.contains("Rapido", ignoreCase = true) -> "Rapido"
                        action.title.contains("Ola", ignoreCase = true) || action.consequenceSummary.contains("Ola", ignoreCase = true) -> "Ola"
                        action.title.contains("inDrive", ignoreCase = true) || action.consequenceSummary.contains("inDrive", ignoreCase = true) -> "inDrive"
                        else -> "Uber"
                    }
                    DeviceAppExecutionBridge.executeRideBooking(
                        context = context,
                        provider = providerName,
                        destination = action.description
                    )
                }
                "OPEN_APP" -> {
                    if (action.title.contains("Amazon", ignoreCase = true) || action.description.contains("Amazon", ignoreCase = true)) {
                        DeviceAppExecutionBridge.executeShoppingSearch(
                            context = context,
                            platform = "Amazon",
                            searchQuery = ""
                        )
                    } else {
                        DeviceAppExecutionBridge.executeShoppingSearch(
                            context = context,
                            platform = "Amazon",
                            searchQuery = action.description
                        )
                    }
                }
                "COMMUNICATION_DISPATCH", "WHATSAPP" -> {
                    DeviceAppExecutionBridge.executeWhatsAppMessage(
                        context = context,
                        phoneNumber = null,
                        messageText = action.description
                    )
                }
                "NAVIGATION_LAUNCH", "MAPS" -> {
                    DeviceAppExecutionBridge.executeMapNavigation(
                        context = context,
                        destinationAddress = action.description
                    )
                }
                "PHONE_CALL", "CALL" -> {
                    DeviceAppExecutionBridge.executePhoneCall(
                        context = context,
                        phoneNumber = action.description
                    )
                }
                "SHOPPING_SEARCH", "AMAZON" -> {
                    DeviceAppExecutionBridge.executeShoppingSearch(
                        context = context,
                        platform = "Amazon",
                        searchQuery = action.description
                    )
                }
                else -> {
                    DeviceAppExecutionBridge.executeShoppingSearch(
                        context = context,
                        platform = "Amazon",
                        searchQuery = action.description
                    )
                }
            }

            // 2. Update status in Database
            repository.updateActionStatus(action.id, "COMPLETED")
            Toast.makeText(context, "${res.appName}: ${res.actionDescription}", Toast.LENGTH_SHORT).show()
        }
    }

    fun approveAction(actionId: Long) {
        viewModelScope.launch {
            repository.updateActionStatus(actionId, "COMPLETED")
        }
    }

    fun cancelAction(actionId: Long) {
        viewModelScope.launch {
            repository.updateActionStatus(actionId, "CANCELLED")
        }
    }

    fun deleteAction(actionId: Long) {
        viewModelScope.launch {
            repository.deleteAction(actionId)
        }
    }

    // Hub Tasks
    fun toggleTask(id: Long, completed: Boolean) {
        viewModelScope.launch { repository.toggleTask(id, completed) }
    }

    fun addTask(title: String, priority: String, category: String, dueDate: String) {
        viewModelScope.launch { repository.addTask(title, priority, category, dueDate) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repository.deleteTask(id) }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch { repository.clearCompletedTasks() }
    }

    // Hub Shopping
    fun toggleShoppingItem(id: Long, isChecked: Boolean) {
        viewModelScope.launch { repository.toggleShoppingItem(id, isChecked) }
    }

    fun addShoppingItem(name: String, category: String, quantity: String, estimatedCost: Double) {
        viewModelScope.launch { repository.addShoppingItem(name, category, quantity, estimatedCost) }
    }

    fun deleteShoppingItem(id: Long) {
        viewModelScope.launch { repository.deleteShoppingItem(id) }
    }

    fun clearCheckedShoppingItems() {
        viewModelScope.launch { repository.clearCheckedShoppingItems() }
    }

    // Hub Expenses
    fun addExpense(title: String, amount: Double, category: String, date: String) {
        viewModelScope.launch { repository.addExpense(title, amount, category, date) }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch { repository.deleteExpense(id) }
    }

    // Family Methods
    fun addFamilyMember(
        name: String,
        relation: String,
        age: Int = 0,
        occupationOrSchool: String = "",
        notes: String = "",
        routineSummary: String = "",
        dates: String = ""
    ) {
        viewModelScope.launch {
            database.familyDao().insertFamilyMember(
                FamilyMemberEntity(
                    name = name,
                    relation = relation,
                    age = age,
                    occupationOrSchool = occupationOrSchool,
                    notes = notes,
                    routineSummary = routineSummary,
                    importantDates = dates
                )
            )
            syncWorldModelToBrain()
        }
    }

    fun addFamilyMember(
        name: String,
        relation: String,
        notes: String,
        routineSummary: String,
        dates: String
    ) {
        addFamilyMember(
            name = name,
            relation = relation,
            age = 0,
            occupationOrSchool = "",
            notes = notes,
            routineSummary = routineSummary,
            dates = dates
        )
    }

    fun updateOwnerProfile(name: String, email: String) {
        _ownerName.value = name
        _ownerEmail.value = email
        prefs.edit().putString("owner_name", name).putString("owner_email", email).apply()
        _personalProfile.value = _personalProfile.value.copy(name = name)
        val ownerId = VybOwnerIdentityManager.computeStableOwnerId(email.ifBlank { name })
        VybOwnerIdentityManager.setOwnerIdentity(
            getApplication(),
            VybOwnerIdentity(
                ownerId = ownerId,
                ownerEmail = email,
                ownerName = name
            )
        )
        viewModelScope.launch {
            repository.syncProfileFactsToVault(_personalProfile.value, email, _ownerPhone.value)
            val snap = persistenceEngine.buildSnapshotFromDatabase(database, _personalProfile.value, ownerId, email, _ownerPhone.value, _aiTone.value)
            persistenceEngine.saveAndSyncSnapshot(snap)
        }
        syncWorldModelToBrain()
    }

    fun updateMonthlyBudget(income: Double, savingsTarget: Double) {
        _monthlyIncome.value = income
        _monthlySavingsTarget.value = savingsTarget
        prefs.edit().putFloat("monthly_income", income.toFloat()).putFloat("monthly_savings_target", savingsTarget.toFloat()).apply()
        _personalProfile.value = _personalProfile.value.copy(monthlyIncome = income, savingsTarget = savingsTarget)
        viewModelScope.launch {
            syncSnapshotToDurableVault()
        }
        syncWorldModelToBrain()
    }

    fun updatePreferences(tone: String, diet: String, currency: String) {
        _aiTone.value = tone
        _dietPreference.value = diet
        _currencySymbol.value = currency
        prefs.edit().putString("ai_tone", tone).putString("diet_preference", diet).putString("currency_symbol", currency).apply()
        _personalProfile.value = _personalProfile.value.copy(dietaryPreference = diet, currencySymbol = currency)
        viewModelScope.launch {
            syncSnapshotToDurableVault()
        }
        syncWorldModelToBrain()
    }

    fun updateFamilyMember(member: FamilyMemberEntity) {
        viewModelScope.launch {
            repository.updateFamilyMember(member)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun deleteFamilyMember(id: Long) {
        viewModelScope.launch {
            repository.deleteFamilyMember(id)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    // Routine Methods
    fun addRoutine(title: String, timeOfDay: String, daysOfWeek: String, description: String, prompt: String, cost: Double = 0.0, category: String = "TRANSPORT") {
        viewModelScope.launch {
            repository.addRoutine(title, timeOfDay, daysOfWeek, description, prompt, cost, category)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun toggleRoutine(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRoutine(id, isEnabled)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch {
            repository.deleteRoutine(id)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    // Goal Methods
    fun addGoal(title: String, category: String, targetValue: String, currentValue: String, targetDeadline: String, progressPercent: Int = 0, stepsSummary: String = "") {
        viewModelScope.launch {
            repository.addGoal(title, category, targetValue, currentValue, targetDeadline, progressPercent, stepsSummary)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun updateGoalProgress(id: Long, progress: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateGoalProgress(id, progress, isCompleted)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    // Project Methods
    fun addProject(title: String, description: String, category: String, deadline: String, progress: Int = 0) {
        viewModelScope.launch {
            repository.addProject(title, description, category, deadline, progress)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun updateProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.updateProject(project)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    // Memory Methods
    fun addMemory(category: String, key: String, value: String, source: String = "User Profile") {
        viewModelScope.launch {
            repository.addMemory(category, key, value, source)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun updateMemory(memory: MemoryItemEntity) {
        viewModelScope.launch {
            repository.updateMemory(memory)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    // Personal Life Vault Operations
    fun addVaultFact(
        category: String,
        key: String,
        value: String,
        source: String = com.example.domain.vault.VaultSource.USER_PROVIDED,
        sourceDetail: String? = null,
        numericValue: Double? = null,
        isConfirmed: Boolean = true
    ) {
        viewModelScope.launch {
            repository.addVaultItem(category, key, value, source, sourceDetail, numericValue, isConfirmed)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun updateVaultFact(item: LifeVaultItemEntity) {
        viewModelScope.launch {
            repository.updateVaultItem(item)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun deleteVaultFact(id: Long) {
        viewModelScope.launch {
            repository.deleteVaultItem(id)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun resolveVaultConflict(itemId: Long, acceptNew: Boolean) {
        viewModelScope.launch {
            repository.resolveVaultConflict(itemId, acceptNew)
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    fun clearAllVaultFacts() {
        viewModelScope.launch {
            repository.clearAllVaultItems()
            syncSnapshotToDurableVault()
            syncWorldModelToBrain()
        }
    }

    suspend fun syncSnapshotToDurableVault() {
        val owner = VybOwnerIdentityManager.getOwnerIdentity(getApplication())
        val snap = persistenceEngine.buildSnapshotFromDatabase(database, _personalProfile.value, owner.ownerId, _ownerEmail.value, _ownerPhone.value, _aiTone.value)
        persistenceEngine.saveAndSyncSnapshot(snap)
    }

    suspend fun getPersistenceDiagnostics(): PersistenceDiagnosticSnapshot {
        val owner = VybOwnerIdentityManager.getOwnerIdentity(getApplication())
        return persistenceEngine.getDiagnosticSnapshot(database, owner.ownerId)
    }

    // Security & Pin
    fun unlockApp(pin: String): Boolean {
        if (!_isSecurityLockEnabled.value || pin == _securityPin.value) {
            _isAppLocked.value = false
            return true
        }
        return false
    }

    fun bypassBiometricUnlock() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        if (_isSecurityLockEnabled.value) {
            _isAppLocked.value = true
        }
    }

    fun setSecurityLockEnabled(enabled: Boolean) {
        _isSecurityLockEnabled.value = enabled
        prefs.edit().putBoolean("security_lock_enabled", enabled).apply()
        if (!enabled) {
            _isAppLocked.value = false
        }
    }

    fun setSecurityPin(newPin: String) {
        if (newPin.length == 4 && newPin.all { it.isDigit() }) {
            _securityPin.value = newPin
            prefs.edit().putString("security_pin", newPin).apply()
        }
    }

    // Accounts
    fun addOwnerAccount(serviceName: String, category: String, accountIdentifier: String, passwordOrSecret: String, isAiMemoryAllowed: Boolean = true, iconKey: String = "key", notes: String = "") {
        viewModelScope.launch {
            repository.addOwnerAccount(serviceName, category, accountIdentifier, passwordOrSecret, isAiMemoryAllowed, iconKey, notes)
        }
    }

    fun updateOwnerAccount(account: OwnerAccountEntity) {
        viewModelScope.launch { repository.updateOwnerAccount(account) }
    }

    fun deleteOwnerAccount(account: OwnerAccountEntity) {
        viewModelScope.launch { repository.deleteOwnerAccount(account) }
    }

    fun toggleAccountAiMemory(id: Long, allowed: Boolean) {
        viewModelScope.launch { repository.toggleAccountAiMemory(id, allowed) }
    }

    fun speakText(text: String) { jarvisCoreManager.speak(text) }
    fun stopSpeaking() { jarvisCoreManager.stopSpeaking() }
    fun speakJarvisGreeting() { jarvisCoreManager.speakJarvisGreeting(_ownerName.value) }
    fun refreshLocation() { jarvisCoreManager.refreshLocation(); jarvisCoreManager.refreshTelemetry() }

    fun resetVybEntirely() {
        viewModelScope.launch {
            repository.resetAllData()
            prefs.edit().clear().apply()
            _ownerName.value = ""
            _ownerTitle.value = ""
            _ownerEmail.value = ""
            _ownerPhone.value = ""
            _ownerHomeAddress.value = ""
            _ownerOfficeAddress.value = ""
            _ownerJobTitle.value = ""
            _ownerCompanyName.value = ""
            _ownerCity.value = ""
            _monthlyIncome.value = 0.0
            _fixedCommitments.value = 0.0
            _monthlySavingsTarget.value = 0.0
            _currencySymbol.value = "₹"
            syncWorldModelToBrain()
            refreshCurrentContext()
        }
    }

    fun refreshCapabilities() {
        val caps = intelligenceOrchestrator.capabilityRegistry.getAllCapabilities(
            getApplication<Application>(),
            permissionManager
        )
        _allCapabilities.value = caps
    }

    fun processIntelligenceCommand(
        input: String,
        onComplete: ((IntelligenceOrchestrationResult) -> Unit)? = null
    ) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = intelligenceOrchestrator.processRequest(
                    input = input,
                    profile = _personalProfile.value,
                    contextGraph = _contextGraph.value,
                    authorizedSources = _authorizedGenesisSources.value,
                    appContext = getApplication<Application>(),
                    permissionManager = permissionManager
                )
                _lastIntelligenceResult.value = result

                result.updatedProfile?.let { updatePersonalProfile(it) }
                result.updatedContextGraph?.let { _contextGraph.value = it }

                result.actionPlan?.let { plan ->
                    _activeActionPlans.value = listOf(plan) + _activeActionPlans.value.filter { it.planId != plan.planId }
                }

                if (result.confirmationPrompt != null) {
                    _pendingConfirmationPrompt.value = result.confirmationPrompt
                }

                refreshCapabilities()
                onComplete?.invoke(result)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to orchestrate action"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun confirmPendingIntelligenceAction(prompt: HumanConfirmationPrompt, context: Context) {
        viewModelScope.launch {
            _pendingConfirmationPrompt.value = null
            val plan = _activeActionPlans.value.firstOrNull { it.status == PlanStatus.AWAITING_CONFIRMATION }
            if (plan != null) {
                val assignedAgent = intelligenceOrchestrator.agentRegistry.findAgentForIntent(
                    _lastIntelligenceResult.value?.universalIntent ?: return@launch
                ) ?: return@launch

                val updatedPlan = intelligenceOrchestrator.actionPlanEngine.executeFullPlan(
                    plan.copy(status = PlanStatus.IN_PROGRESS),
                    _lastIntelligenceResult.value!!.universalIntent,
                    assignedAgent,
                    _lastIntelligenceResult.value!!.resolvedContext,
                    context,
                    permissionManager
                )
                _activeActionPlans.value = _activeActionPlans.value.map { if (it.planId == plan.planId) updatedPlan else it }
                Toast.makeText(context, "Action confirmed and executed: ${plan.goal}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun cancelPendingIntelligenceAction(prompt: HumanConfirmationPrompt) {
        _pendingConfirmationPrompt.value = null
        val plan = _activeActionPlans.value.firstOrNull { it.status == PlanStatus.AWAITING_CONFIRMATION }
        if (plan != null) {
            _activeActionPlans.value = _activeActionPlans.value.map {
                if (it.planId == plan.planId) it.copy(status = PlanStatus.CANCELLED) else it
            }
        }
    }

    fun confirmPendingAction(approved: Boolean, context: Context? = null) {
        val prompt = _pendingConfirmationPrompt.value
        if (prompt != null) {
            if (approved) {
                confirmPendingIntelligenceAction(prompt, context ?: getApplication<Application>())
            } else {
                cancelPendingIntelligenceAction(prompt)
            }
        }
    }

    fun startNewSession() {
        clearAllConversations()
    }

    override fun onCleared() {
        super.onCleared()
        jarvisCoreManager.destroy()
        livingEnvironmentEngine.unregisterSensors()
    }

    fun parsePayload(json: String?): VybIntentPayload? {
        if (json.isNullOrBlank()) return null
        return try {
            val adapter = GeminiClient.moshi.adapter(VybIntentPayload::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
