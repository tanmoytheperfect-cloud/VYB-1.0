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
import com.example.domain.learning.*
import com.example.domain.permission.*
import com.example.domain.proactive.*
import com.example.domain.observer.VybDeviceObserver
import com.example.util.DeviceAppExecutionBridge
import com.example.util.JarvisCoreManager
import com.example.util.JarvisLocationTelemetry
import com.example.util.JarvisSystemTelemetry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class VybViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val repository = VybRepository(database)
    private val prefs = application.getSharedPreferences("vyb_owner_prefs", Context.MODE_PRIVATE)

    // Permission Manager & Observer
    val permissionManager = VybPermissionManager(application)
    val deviceObserver = VybDeviceObserver(
        context = application,
        timelineDao = database.timelineDao(),
        permissionManager = permissionManager
    )

    // Personal AI OS Engines
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

    // Onboarding State
    private val _isOnboardingCompleted = MutableStateFlow(prefs.getBoolean("is_onboarding_completed", true))
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    // Owner Profile State - Unconfigured / Empty by default
    private val _ownerName = MutableStateFlow(prefs.getString("owner_name", "") ?: "")
    val ownerName: StateFlow<String> = _ownerName.asStateFlow()

    private val _ownerTitle = MutableStateFlow(prefs.getString("owner_title", "") ?: "")
    val ownerTitle: StateFlow<String> = _ownerTitle.asStateFlow()

    private val _ownerEmail = MutableStateFlow(prefs.getString("owner_email", "") ?: "")
    val ownerEmail: StateFlow<String> = _ownerEmail.asStateFlow()

    private val _ownerPhone = MutableStateFlow(prefs.getString("owner_phone", "") ?: "")
    val ownerPhone: StateFlow<String> = _ownerPhone.asStateFlow()

    private val _ownerHomeAddress = MutableStateFlow(prefs.getString("owner_home_address", "") ?: "")
    val ownerHomeAddress: StateFlow<String> = _ownerHomeAddress.asStateFlow()

    private val _ownerOfficeAddress = MutableStateFlow(prefs.getString("owner_office_address", "") ?: "")
    val ownerOfficeAddress: StateFlow<String> = _ownerOfficeAddress.asStateFlow()

    private val _ownerJobTitle = MutableStateFlow(prefs.getString("owner_job_title", "") ?: "")
    val ownerJobTitle: StateFlow<String> = _ownerJobTitle.asStateFlow()

    private val _ownerCompanyName = MutableStateFlow(prefs.getString("owner_company", "") ?: "")
    val ownerCompanyName: StateFlow<String> = _ownerCompanyName.asStateFlow()

    private val _ownerCity = MutableStateFlow(prefs.getString("owner_city", "") ?: "")
    val ownerCity: StateFlow<String> = _ownerCity.asStateFlow()

    // Financial Planning Context - 0.0 by default unless set by user
    private val _monthlyIncome = MutableStateFlow(prefs.getFloat("monthly_income", 0f).toDouble())
    val monthlyIncome: StateFlow<Double> = _monthlyIncome.asStateFlow()

    private val _fixedCommitments = MutableStateFlow(prefs.getFloat("fixed_commitments", 0f).toDouble())
    val fixedCommitments: StateFlow<Double> = _fixedCommitments.asStateFlow()

    private val _monthlySavingsTarget = MutableStateFlow(prefs.getFloat("monthly_savings_target", 0f).toDouble())
    val monthlySavingsTarget: StateFlow<Double> = _monthlySavingsTarget.asStateFlow()

    private val _currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "₹") ?: "₹")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()
    val currency: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _aiTone = MutableStateFlow(prefs.getString("ai_tone", "EXECUTIVE_PA") ?: "EXECUTIVE_PA")
    val aiTone: StateFlow<String> = _aiTone.asStateFlow()

    private val _dietPreference = MutableStateFlow(prefs.getString("diet_preference", "") ?: "")
    val dietPreference: StateFlow<String> = _dietPreference.asStateFlow()

    // Security Verification / Lock State
    private val _isSecurityLockEnabled = MutableStateFlow(prefs.getBoolean("security_lock_enabled", false))
    val isSecurityLockEnabled: StateFlow<Boolean> = _isSecurityLockEnabled.asStateFlow()

    private val _securityPin = MutableStateFlow(prefs.getString("security_pin", "1234") ?: "1234")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

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

    init {
        viewModelScope.launch {
            repository.seedInitialOperatingLayerDataIfEmpty()
            seedInitialTimelineAndSettingsIfEmpty()
            syncWorldModelToBrain()
            deviceObserver.startObserving()
            refreshPermissionReports()
            refreshCurrentContext()
            runRoutineLearning()
        }
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

        syncWorldModelToBrain()
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

    fun sendMessage(prompt: String) {
        val convId = _currentConversationId.value ?: return
        if (prompt.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                syncWorldModelToBrain()
                val result = repository.sendUserMessage(convId, prompt, getApplication<Application>())
                if (result.isFailure) {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to generate response"
                } else {
                    _currentConversation.value = repository.getConversationById(convId)
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send message"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun retryLastMessage() {
        val convId = _currentConversationId.value ?: return
        val lastUserMsg = currentMessages.value.lastOrNull { it.sender == "USER" } ?: return
        sendMessage(lastUserMsg.content)
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
        syncWorldModelToBrain()
    }

    fun updateMonthlyBudget(income: Double, savingsTarget: Double) {
        _monthlyIncome.value = income
        _monthlySavingsTarget.value = savingsTarget
        prefs.edit().putFloat("monthly_income", income.toFloat()).putFloat("monthly_savings_target", savingsTarget.toFloat()).apply()
        syncWorldModelToBrain()
    }

    fun updatePreferences(tone: String, diet: String, currency: String) {
        _aiTone.value = tone
        _dietPreference.value = diet
        _currencySymbol.value = currency
        prefs.edit().putString("ai_tone", tone).putString("diet_preference", diet).putString("currency_symbol", currency).apply()
        syncWorldModelToBrain()
    }

    fun updateFamilyMember(member: FamilyMemberEntity) {
        viewModelScope.launch {
            repository.updateFamilyMember(member)
            syncWorldModelToBrain()
        }
    }

    fun deleteFamilyMember(id: Long) {
        viewModelScope.launch {
            repository.deleteFamilyMember(id)
            syncWorldModelToBrain()
        }
    }

    // Routine Methods
    fun addRoutine(title: String, timeOfDay: String, daysOfWeek: String, description: String, prompt: String, cost: Double = 0.0, category: String = "TRANSPORT") {
        viewModelScope.launch {
            repository.addRoutine(title, timeOfDay, daysOfWeek, description, prompt, cost, category)
            syncWorldModelToBrain()
        }
    }

    fun toggleRoutine(id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleRoutine(id, isEnabled)
            syncWorldModelToBrain()
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch {
            repository.deleteRoutine(id)
            syncWorldModelToBrain()
        }
    }

    // Goal Methods
    fun addGoal(title: String, category: String, targetValue: String, currentValue: String, targetDeadline: String, progressPercent: Int = 0, stepsSummary: String = "") {
        viewModelScope.launch {
            repository.addGoal(title, category, targetValue, currentValue, targetDeadline, progressPercent, stepsSummary)
            syncWorldModelToBrain()
        }
    }

    fun updateGoalProgress(id: Long, progress: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateGoalProgress(id, progress, isCompleted)
            syncWorldModelToBrain()
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
            syncWorldModelToBrain()
        }
    }

    // Project Methods
    fun addProject(title: String, description: String, category: String, deadline: String, progress: Int = 0) {
        viewModelScope.launch {
            repository.addProject(title, description, category, deadline, progress)
            syncWorldModelToBrain()
        }
    }

    fun updateProject(project: ProjectEntity) {
        viewModelScope.launch {
            repository.updateProject(project)
            syncWorldModelToBrain()
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            repository.deleteProject(id)
            syncWorldModelToBrain()
        }
    }

    // Memory Methods
    fun addMemory(category: String, key: String, value: String, source: String = "User Profile") {
        viewModelScope.launch {
            repository.addMemory(category, key, value, source)
            syncWorldModelToBrain()
        }
    }

    fun updateMemory(memory: MemoryItemEntity) {
        viewModelScope.launch {
            repository.updateMemory(memory)
            syncWorldModelToBrain()
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
            syncWorldModelToBrain()
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
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
            syncWorldModelToBrain()
        }
    }

    fun updateVaultFact(item: LifeVaultItemEntity) {
        viewModelScope.launch {
            repository.updateVaultItem(item)
            syncWorldModelToBrain()
        }
    }

    fun deleteVaultFact(id: Long) {
        viewModelScope.launch {
            repository.deleteVaultItem(id)
            syncWorldModelToBrain()
        }
    }

    fun resolveVaultConflict(itemId: Long, acceptNew: Boolean) {
        viewModelScope.launch {
            repository.resolveVaultConflict(itemId, acceptNew)
            syncWorldModelToBrain()
        }
    }

    fun clearAllVaultFacts() {
        viewModelScope.launch {
            repository.clearAllVaultItems()
            syncWorldModelToBrain()
        }
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

    override fun onCleared() {
        super.onCleared()
        jarvisCoreManager.destroy()
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
