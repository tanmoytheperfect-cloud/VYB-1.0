package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.model.IntentType
import com.example.data.model.VybIntentPayload
import com.example.data.remote.GeminiClient
import com.example.domain.orchestrator.VybOrchestrator
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VybRepository(
    private val database: AppDatabase,
    private val geminiRepository: GeminiRepository = GeminiRepository()
) {
    private val conversationDao = database.conversationDao()
    private val messageDao = database.messageDao()
    private val taskDao = database.taskDao()
    private val shoppingDao = database.shoppingDao()
    private val expenseDao = database.expenseDao()
    private val ownerAccountDao = database.ownerAccountDao()
    private val goalDao = database.goalDao()
    private val projectDao = database.projectDao()
    private val routineDao = database.routineDao()
    private val actionRecordDao = database.actionRecordDao()
    private val memoryDao = database.memoryDao()
    private val familyDao = database.familyDao()
    private val timelineDao = database.timelineDao()
    private val learnedRoutineDao = database.learnedRoutineDao()
    private val dailyReflectionDao = database.dailyReflectionDao()
    private val weeklyPatternDao = database.weeklyPatternDao()
    private val learningSettingDao = database.learningSettingDao()
    private val lifeVaultDao = database.lifeVaultDao()

    val personalLifeVaultEngine = com.example.domain.vault.PersonalLifeVaultEngine(lifeVaultDao)

    val orchestrator = VybOrchestrator(
        memoryDao = memoryDao,
        taskDao = taskDao,
        geminiRepository = geminiRepository,
        lifeVaultDao = lifeVaultDao
    )

    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val allTasks: Flow<List<TaskItemEntity>> = taskDao.getAllTasks()
    val allShoppingItems: Flow<List<ShoppingItemEntity>> = shoppingDao.getAllShoppingItems()
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val totalExpenseAmount: Flow<Double?> = expenseDao.getTotalExpenseAmount()
    val allOwnerAccounts: Flow<List<OwnerAccountEntity>> = ownerAccountDao.getAllAccounts()
    val authorizedAiAccounts: Flow<List<OwnerAccountEntity>> = ownerAccountDao.getAccountsWithAiMemoryPermission()
    
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allRoutines: Flow<List<RoutineEntity>> = routineDao.getAllRoutines()
    val allActions: Flow<List<ActionRecordEntity>> = actionRecordDao.getAllActions()
    val pendingConfirmationActions: Flow<List<ActionRecordEntity>> = actionRecordDao.getPendingConfirmationActions()
    val allMemories: Flow<List<MemoryItemEntity>> = memoryDao.getAllMemories()
    val allFamilyMembers: Flow<List<FamilyMemberEntity>> = familyDao.getAllFamilyMembers()
    val allVaultItems: Flow<List<LifeVaultItemEntity>> = lifeVaultDao.getAllVaultItemsFlow()
    val pendingVaultConflicts: Flow<List<LifeVaultItemEntity>> = lifeVaultDao.getPendingConflictsFlow()

    // Timeline & Personal AI OS Flows
    val recentTimelineEvents: Flow<List<TimelineEventEntity>> = timelineDao.getRecentEventsFlow(100)
    val allLearnedRoutines: Flow<List<LearnedRoutineEntity>> = learnedRoutineDao.getAllLearnedRoutinesFlow()
    val allDailyReflections: Flow<List<DailyReflectionEntity>> = dailyReflectionDao.getAllReflectionsFlow()
    val allWeeklyPatterns: Flow<List<WeeklyPatternEntity>> = weeklyPatternDao.getAllPatternsFlow()
    val learningSettingsFlow: Flow<LearningSettingEntity?> = learningSettingDao.getSettingsFlow()

    fun getTodayEventsFlow(dateString: String): Flow<List<TimelineEventEntity>> {
        return timelineDao.getEventsByDateFlow(dateString)
    }

    suspend fun getTimelineDao(): com.example.data.local.dao.TimelineDao = timelineDao
    suspend fun getLearnedRoutineDao(): com.example.data.local.dao.LearnedRoutineDao = learnedRoutineDao
    suspend fun getDailyReflectionDao(): com.example.data.local.dao.DailyReflectionDao = dailyReflectionDao
    suspend fun getWeeklyPatternDao(): com.example.data.local.dao.WeeklyPatternDao = weeklyPatternDao
    suspend fun getLearningSettingDao(): com.example.data.local.dao.LearningSettingDao = learningSettingDao

    suspend fun insertTimelineEvent(event: TimelineEventEntity): Long = timelineDao.insertEvent(event)
    suspend fun insertLearnedRoutine(routine: LearnedRoutineEntity) = learnedRoutineDao.insertOrUpdate(routine)
    suspend fun deleteLearnedRoutine(routineId: String) = learnedRoutineDao.deleteRoutine(routineId)
    suspend fun clearAllLearnedRoutines() = learnedRoutineDao.clearAllLearnedRoutines()
    suspend fun clearTodayTimeline(dateString: String) = timelineDao.clearEventsForDate(dateString)
    suspend fun clearAllTimeline() = timelineDao.clearAllEvents()
    suspend fun setLearningPaused(isPaused: Boolean) = learningSettingDao.setLearningPaused(isPaused)
    suspend fun insertLearningSettings(settings: LearningSettingEntity) = learningSettingDao.insertOrUpdate(settings)

    fun getMessagesForConversation(conversationId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesForConversation(conversationId)
    }

    suspend fun getConversationById(id: Long): ConversationEntity? {
        return conversationDao.getConversationById(id)
    }

    suspend fun createConversation(prompt: String): Long {
        val title = if (prompt.length > 35) prompt.take(35) + "..." else prompt
        val conversation = ConversationEntity(
            title = title,
            preview = prompt,
            intentType = IntentType.GENERAL.name
        )
        return conversationDao.insertConversation(conversation)
    }

    suspend fun sendUserMessage(
        conversationId: Long,
        userPrompt: String,
        appContext: android.content.Context? = null
    ): Result<MessageEntity> {
        // Insert user message
        val userMessage = MessageEntity(
            conversationId = conversationId,
            sender = "USER",
            content = userPrompt,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userMessage)

        return try {
            val orchestratorResult = orchestrator.processMessage(userPrompt, appContext)
            val conversationalText = orchestratorResult.conversationalResponse
            val payload = orchestratorResult.intentPayload

            // If an action was generated, save into action record table
            orchestratorResult.action?.let { action ->
                actionRecordDao.insertAction(
                    ActionRecordEntity(
                        title = action.title,
                        description = action.description,
                        actionType = action.actionType,
                        levelCode = action.level.ordinal,
                        status = action.status.name,
                        costInr = action.expectedCostInr,
                        consequenceSummary = action.consequenceSummary,
                        timestamp = System.currentTimeMillis()
                    )
                )
            }

            val adapter = GeminiClient.moshi.adapter(VybIntentPayload::class.java)
            val intentJson = adapter.toJson(payload)

            val assistantMessage = MessageEntity(
                conversationId = conversationId,
                sender = "ASSISTANT",
                content = conversationalText,
                intentJson = intentJson,
                timestamp = System.currentTimeMillis(),
                isError = false
            )
            messageDao.insertMessage(assistantMessage)

            // Update conversation metadata
            val conversation = conversationDao.getConversationById(conversationId)
            if (conversation != null) {
                val updatedTitle = if (payload.title.isNotBlank()) payload.title else conversation.title
                conversationDao.updateConversation(
                    conversation.copy(
                        title = updatedTitle,
                        preview = payload.summary.ifBlank { conversationalText.take(60) },
                        intentType = payload.intentType,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Automatically sync structured entities into local Hub if appropriate
            syncPayloadToHub(conversationId, payload)

            Result.success(assistantMessage)
        } catch (e: Exception) {
            val errorMessage = MessageEntity(
                conversationId = conversationId,
                sender = "ASSISTANT",
                content = "I encountered an issue processing your request: ${e.message ?: "Please tap retry."}",
                timestamp = System.currentTimeMillis(),
                isError = true
            )
            messageDao.insertMessage(errorMessage)
            Result.failure(e)
        }
    }

    private suspend fun syncPayloadToHub(conversationId: Long, payload: VybIntentPayload) {
        // Sync tasks
        payload.taskList?.tasks?.forEach { task ->
            taskDao.insertTask(
                TaskItemEntity(
                    conversationId = conversationId,
                    title = task.title,
                    priority = task.priority,
                    category = task.category,
                    dueDate = task.dueDate,
                    isCompleted = false
                )
            )
        }

        // Sync shopping items
        payload.shoppingList?.items?.forEach { item ->
            shoppingDao.insertShoppingItem(
                ShoppingItemEntity(
                    conversationId = conversationId,
                    name = item.name,
                    category = item.category,
                    quantity = item.quantity,
                    estimatedCost = item.estimatedCost,
                    isChecked = false
                )
            )
        }

        // Sync finance budget category estimates if provided
        val today = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
        payload.financePlan?.categories?.forEach { cat ->
            if (cat.allocatedAmount > 0) {
                expenseDao.insertExpense(
                    ExpenseEntity(
                        conversationId = conversationId,
                        title = "Budget: ${cat.name}",
                        amount = cat.allocatedAmount,
                        category = cat.name,
                        date = today
                    )
                )
            }
        }
    }

    suspend fun deleteConversation(id: Long) {
        messageDao.deleteMessagesForConversation(id)
        conversationDao.deleteConversationById(id)
    }

    suspend fun clearAllConversations() {
        conversationDao.clearAllConversations()
    }

    // Task Operations
    suspend fun toggleTask(id: Long, completed: Boolean) {
        taskDao.setTaskCompleted(id, completed)
    }

    suspend fun addTask(title: String, priority: String, category: String, dueDate: String): Long {
        return taskDao.insertTask(
            TaskItemEntity(
                title = title,
                priority = priority,
                category = category,
                dueDate = dueDate,
                isCompleted = false
            )
        )
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteTaskById(id)
    }

    suspend fun clearCompletedTasks() {
        taskDao.deleteCompletedTasks()
    }

    // Shopping Operations
    suspend fun toggleShoppingItem(id: Long, checked: Boolean) {
        shoppingDao.setItemChecked(id, checked)
    }

    suspend fun addShoppingItem(name: String, category: String, quantity: String, estimatedCost: Double): Long {
        return shoppingDao.insertShoppingItem(
            ShoppingItemEntity(
                name = name,
                category = category,
                quantity = quantity,
                estimatedCost = estimatedCost,
                isChecked = false
            )
        )
    }

    suspend fun deleteShoppingItem(id: Long) {
        shoppingDao.deleteShoppingItemById(id)
    }

    suspend fun clearCheckedShoppingItems() {
        shoppingDao.deleteCheckedItems()
    }

    // Expense Operations
    suspend fun addExpense(title: String, amount: Double, category: String, date: String): Long {
        return expenseDao.insertExpense(
            ExpenseEntity(
                title = title,
                amount = amount,
                category = category,
                date = date
            )
        )
    }

    suspend fun deleteExpense(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    // Owner Account Operations
    suspend fun addOwnerAccount(
        serviceName: String,
        category: String,
        accountIdentifier: String,
        passwordOrSecret: String,
        isAiMemoryAllowed: Boolean = true,
        iconKey: String = "key",
        notes: String = ""
    ): Long {
        return ownerAccountDao.insertAccount(
            OwnerAccountEntity(
                serviceName = serviceName,
                category = category,
                accountIdentifier = accountIdentifier,
                passwordOrSecret = passwordOrSecret,
                isAiMemoryAllowed = isAiMemoryAllowed,
                iconKey = iconKey,
                notes = notes,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateOwnerAccount(account: OwnerAccountEntity) {
        ownerAccountDao.updateAccount(account.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteOwnerAccount(account: OwnerAccountEntity) {
        ownerAccountDao.deleteAccount(account)
    }

    suspend fun toggleAccountAiMemory(id: Long, allowed: Boolean) {
        ownerAccountDao.updateAiMemoryPermission(id, allowed)
    }

    suspend fun seedDefaultOwnerAccountsIfEmpty() {
        // Milestone 1 - No seeded accounts or passwords
    }

    // Goal Operations
    suspend fun addGoal(
        title: String,
        category: String,
        targetValue: String,
        currentValue: String,
        targetDeadline: String,
        progressPercent: Int = 0,
        stepsSummary: String = ""
    ): Long {
        return goalDao.insertGoal(
            GoalEntity(
                title = title,
                category = category,
                targetValue = targetValue,
                currentValue = currentValue,
                targetDeadline = targetDeadline,
                progressPercent = progressPercent,
                stepsSummary = stepsSummary
            )
        )
    }

    suspend fun updateGoalProgress(id: Long, progress: Int, isCompleted: Boolean) {
        goalDao.updateGoalProgress(id, progress, isCompleted)
    }

    suspend fun deleteGoal(id: Long) {
        goalDao.deleteGoal(id)
    }

    // Project Operations
    suspend fun addProject(
        title: String,
        description: String,
        category: String,
        deadline: String,
        progressPercent: Int = 0
    ): Long {
        return projectDao.insertProject(
            ProjectEntity(
                title = title,
                description = description,
                category = category,
                deadline = deadline,
                progressPercent = progressPercent
            )
        )
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project)
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProject(id)
    }

    // Routine Operations
    suspend fun addRoutine(
        title: String,
        timeOfDay: String,
        daysOfWeek: String,
        description: String,
        actionPrompt: String,
        estimatedCostInr: Double = 0.0,
        category: String = "TRANSPORT"
    ): Long {
        return routineDao.insertRoutine(
            RoutineEntity(
                title = title,
                timeOfDay = timeOfDay,
                daysOfWeek = daysOfWeek,
                description = description,
                actionPrompt = actionPrompt,
                estimatedCostInr = estimatedCostInr,
                category = category
            )
        )
    }

    suspend fun toggleRoutine(id: Long, isEnabled: Boolean) {
        routineDao.toggleRoutine(id, isEnabled)
    }

    suspend fun deleteRoutine(id: Long) {
        routineDao.deleteRoutine(id)
    }

    // Action Record Operations
    suspend fun recordAction(
        title: String,
        description: String,
        actionType: String,
        levelCode: Int = 1,
        status: String = "PENDING",
        costInr: Double? = null,
        consequenceSummary: String = ""
    ): Long {
        return actionRecordDao.insertAction(
            ActionRecordEntity(
                title = title,
                description = description,
                actionType = actionType,
                levelCode = levelCode,
                status = status,
                costInr = costInr,
                consequenceSummary = consequenceSummary
            )
        )
    }

    suspend fun updateActionStatus(id: Long, status: String) {
        actionRecordDao.updateActionStatus(id, status)
    }

    suspend fun deleteAction(id: Long) {
        actionRecordDao.deleteAction(id)
    }

    // Memory Operations
    suspend fun addMemory(
        category: String,
        key: String,
        value: String,
        source: String = "User Conversation",
        confidence: Double = 0.95
    ): Long {
        return memoryDao.insertMemory(
            MemoryItemEntity(
                category = category,
                key = key,
                value = value,
                source = source,
                confidence = confidence
            )
        )
    }

    suspend fun updateMemory(memory: MemoryItemEntity) {
        memoryDao.updateMemory(memory)
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemory(id)
    }

    suspend fun clearAllMemories() {
        memoryDao.clearAllMemories()
    }

    // Family Operations
    suspend fun addFamilyMember(
        name: String,
        relation: String,
        notes: String = "",
        routineSummary: String = "",
        importantDates: String = ""
    ): Long {
        return familyDao.insertFamilyMember(
            FamilyMemberEntity(
                name = name,
                relation = relation,
                notes = notes,
                routineSummary = routineSummary,
                importantDates = importantDates
            )
        )
    }

    suspend fun updateFamilyMember(member: FamilyMemberEntity) {
        familyDao.updateFamilyMember(member)
    }

    suspend fun deleteFamilyMember(id: Long) {
        familyDao.deleteFamilyMember(id)
    }

    // Life Vault Operations
    suspend fun addVaultItem(
        category: String,
        key: String,
        value: String,
        source: String = com.example.domain.vault.VaultSource.USER_PROVIDED,
        sourceDetail: String? = null,
        numericValue: Double? = null,
        isConfirmed: Boolean = true
    ): Long {
        return lifeVaultDao.insertVaultItem(
            LifeVaultItemEntity(
                category = category,
                key = key,
                value = value,
                source = source,
                sourceDetail = sourceDetail,
                numericValue = numericValue,
                isConfirmed = isConfirmed,
                timestamp = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateVaultItem(item: LifeVaultItemEntity) {
        lifeVaultDao.updateVaultItem(item.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteVaultItem(id: Long) {
        lifeVaultDao.deleteVaultItemById(id)
    }

    suspend fun resolveVaultConflict(itemId: Long, acceptNew: Boolean): LifeVaultItemEntity? {
        return personalLifeVaultEngine.resolveConflict(itemId, acceptNew)
    }

    suspend fun clearAllVaultItems() {
        lifeVaultDao.clearAllVaultItems()
    }

    fun getVaultItemsByCategoryFlow(category: String): Flow<List<LifeVaultItemEntity>> {
        return lifeVaultDao.getVaultItemsByCategoryFlow(category)
    }

    suspend fun seedInitialOperatingLayerDataIfEmpty() {
        // Milestone 1 - Pristine empty initialization: No fabricated or seeded personal data.
        // VYB learns genuinely from device observation.
    }

    suspend fun resetAllData() {
        database.clearAllTables()
    }
}
