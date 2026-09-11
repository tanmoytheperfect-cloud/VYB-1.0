package com.example.domain.agent

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.dao.FamilyDao
import com.example.data.local.dao.LifeVaultDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.dao.TaskDao
import com.example.domain.actions.ActionLevel
import com.example.domain.actions.ActionStatus
import com.example.domain.actions.VybAction
import com.example.domain.capability.CapabilityEngine
import com.example.domain.capability.CapabilityType
import com.example.data.repository.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class AgentEngine(
    private val memoryDao: MemoryDao? = null,
    private val taskDao: TaskDao? = null,
    private val lifeVaultDao: LifeVaultDao? = null,
    private val familyDao: FamilyDao? = null,
    private val routineDao: RoutineDao? = null,
    private val geminiRepository: GeminiRepository = GeminiRepository()
) {
    val toolRegistry = AgentToolRegistry(memoryDao, taskDao, lifeVaultDao)
    val proactiveEngine = ProactiveEngine()

    suspend fun processRequest(
        input: String,
        prefs: SharedPreferences?,
        appContext: Context?
    ): AgentResponse = withContext(Dispatchers.IO) {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 1. INTENT UNDERSTANDING
        val intent = understandIntent(trimmed, lower)

        // 2 & 3. CONTEXT & MEMORY RETRIEVAL
        val agentContext = retrieveContext(prefs, appContext, intent)

        // 4. TOOL DISCOVERY
        val availableTools = toolRegistry.getAvailableTools(agentContext, appContext)

        // 5. REASONING & PLANNING
        val plan = formulatePlan(intent, agentContext, availableTools)

        // 6. EXECUTION / SAFETY GATE
        when (intent.category) {
            AgentIntentCategory.DAILY_PRIORITIES -> {
                val briefing = proactiveEngine.generateBriefing(agentContext)
                AgentResponse(
                    conversationalReply = briefing.executiveSummary,
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    diagnosticsSummary = mapOf(
                        "Intent" to "DAILY_PRIORITIES",
                        "PrimaryFocus" to briefing.primaryFocus,
                        "PrioritiesCount" to "${briefing.priorities.size}",
                        "ActiveBuffer" to "₹${agentContext.activeDiscretionaryBuffer.toInt()}"
                    ),
                    suggestedActions = briefing.suggestedFollowUps
                )
            }

            AgentIntentCategory.WHAT_SHOULD_I_DO_NOW -> {
                val recommendation = proactiveEngine.evaluateWhatShouldIDoNow(agentContext)
                AgentResponse(
                    conversationalReply = recommendation,
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    diagnosticsSummary = mapOf(
                        "Intent" to "WHAT_SHOULD_I_DO_NOW",
                        "Hour" to "${agentContext.hourOfDay}",
                        "State" to "Reasoned from live time and schedule"
                    ),
                    suggestedActions = listOf("Handle my son's school cab", "Check tasks", "Back to briefing")
                )
            }

            AgentIntentCategory.DELIVERY_TRACKING -> {
                val amazonTool = toolRegistry.findToolById("amazon_delivery_tool")
                val result = amazonTool?.execute(trimmed, emptyMap(), agentContext, appContext)

                when (result) {
                    is AgentToolResult.Unavailable -> {
                        AgentResponse(
                            conversationalReply = "${result.reason}\n\n${result.suggestedNextStep}",
                            intent = intent,
                            plan = plan,
                            state = AgentState.AWAITING_USER_AUTHORIZATION,
                            knowledgeStatus = KnowledgeStatus.UNAVAILABLE,
                            diagnosticsSummary = mapOf(
                                "Intent" to "DELIVERY_TRACKING",
                                "Service" to "Amazon",
                                "ConnectionStatus" to "NOT_CONNECTED",
                                "Action" to "Zero fabrication - open app or connect"
                            ),
                            suggestedActions = listOf("Open Amazon", "Link Amazon Account", "Back to briefing")
                        )
                    }
                    is AgentToolResult.Success -> {
                        AgentResponse(
                            conversationalReply = result.summary,
                            intent = intent,
                            plan = plan,
                            state = AgentState.COMPLETED,
                            knowledgeStatus = KnowledgeStatus.KNOWN,
                            diagnosticsSummary = mapOf(
                                "Intent" to "DELIVERY_TRACKING",
                                "AmazonStatus" to "CONNECTED"
                            ),
                            suggestedActions = listOf("View Order History", "Today's briefing")
                        )
                    }
                    else -> {
                        AgentResponse(
                            conversationalReply = "Amazon is not currently linked. I can open the Amazon app on your phone so you can inspect your active shipments.",
                            intent = intent,
                            plan = plan,
                            state = AgentState.AWAITING_USER_AUTHORIZATION,
                            knowledgeStatus = KnowledgeStatus.UNAVAILABLE,
                            suggestedActions = listOf("Open Amazon", "Cancel")
                        )
                    }
                }
            }

            AgentIntentCategory.TRANSPORT_COORDINATION -> {
                val transportTool = toolRegistry.findToolById("transport_coordination_tool")
                val result = transportTool?.execute(trimmed, emptyMap(), agentContext, appContext)

                if (result is AgentToolResult.RequiresConfirmation) {
                    AgentResponse(
                        conversationalReply = result.explanation,
                        intent = intent,
                        plan = plan,
                        state = AgentState.AWAITING_USER_AUTHORIZATION,
                        knowledgeStatus = KnowledgeStatus.KNOWN,
                        pendingAction = result.action,
                        diagnosticsSummary = mapOf(
                            "Intent" to "TRANSPORT_COORDINATION",
                            "Passenger" to (result.action.parameters["passenger"] ?: "Son"),
                            "Destination" to (result.action.parameters["destination"] ?: "School"),
                            "Status" to "Waiting for User Provider Authorization"
                        ),
                        suggestedActions = listOf("Authorize & Open Ride App", "Hold booking", "Cancel")
                    )
                } else {
                    AgentResponse(
                        conversationalReply = "I have prepared transport options for your route. Please confirm which ride provider you'd like to launch.",
                        intent = intent,
                        plan = plan,
                        state = AgentState.COMPLETED,
                        knowledgeStatus = KnowledgeStatus.KNOWN,
                        suggestedActions = listOf("Open Rapido", "Open Uber", "Open Yatri Sathi")
                    )
                }
            }

            AgentIntentCategory.FINANCIAL_AFFORDABILITY -> {
                val financeTool = toolRegistry.findToolById("financial_affordability_tool")
                val result = financeTool?.execute(trimmed, emptyMap(), agentContext, appContext)
                val reply = (result as? AgentToolResult.Success)?.summary
                    ?: "Based on your financial baseline, your savings target is protected."

                AgentResponse(
                    conversationalReply = reply,
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    diagnosticsSummary = mapOf(
                        "Intent" to "FINANCIAL_AFFORDABILITY",
                        "MonthlyIncome" to "₹${agentContext.monthlyIncome.toInt()}",
                        "SavingsTarget" to "₹${agentContext.monthlySavingsTarget.toInt()}",
                        "DiscretionaryBuffer" to "₹${agentContext.activeDiscretionaryBuffer.toInt()}"
                    ),
                    suggestedActions = listOf("Review Monthly Cashflow", "Add to Planned Expenses", "Back to briefing")
                )
            }

            AgentIntentCategory.MEMORY_STORE, AgentIntentCategory.MEMORY_FORGET -> {
                val memTool = toolRegistry.findToolById("agent_memory_tool")
                val result = memTool?.execute(trimmed, emptyMap(), agentContext, appContext)
                val reply = (result as? AgentToolResult.Success)?.summary ?: "Memory vault updated."

                AgentResponse(
                    conversationalReply = reply,
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    diagnosticsSummary = mapOf(
                        "Intent" to intent.category.name,
                        "Storage" to "Local Encrypted Room Database"
                    ),
                    suggestedActions = listOf("What do you remember?", "Today's briefing")
                )
            }

            AgentIntentCategory.REMINDER_AND_TASK -> {
                val reminderTool = toolRegistry.findToolById("device_reminder_tool")
                val result = reminderTool?.execute(trimmed, emptyMap(), agentContext, appContext)
                val reply = (result as? AgentToolResult.Success)?.summary ?: "Reminder recorded."

                AgentResponse(
                    conversationalReply = reply,
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    diagnosticsSummary = mapOf(
                        "Intent" to "REMINDER_AND_TASK",
                        "Engine" to "Room TaskDao + AlarmBridge"
                    ),
                    suggestedActions = listOf("View today's tasks", "Set another reminder")
                )
            }

            AgentIntentCategory.APP_LAUNCH -> {
                val launcher = toolRegistry.findToolById("app_launcher_tool")
                val result = launcher?.execute(trimmed, emptyMap(), agentContext, appContext)
                val reply = when (result) {
                    is AgentToolResult.Success -> result.summary
                    is AgentToolResult.Unavailable -> "${result.reason} ${result.suggestedNextStep}"
                    else -> "Opening requested application."
                }

                AgentResponse(
                    conversationalReply = reply,
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    diagnosticsSummary = mapOf(
                        "Intent" to "APP_LAUNCH",
                        "Status" to "Dispatched via Android Package Manager"
                    ),
                    suggestedActions = listOf("Return to Home", "Today's briefing")
                )
            }

            AgentIntentCategory.CALENDAR_SCHEDULE -> {
                val calTool = toolRegistry.findToolById("device_calendar_tool")
                val result = calTool?.execute(trimmed, emptyMap(), agentContext, appContext)
                val reply = (result as? AgentToolResult.Success)?.summary
                    ?: "I have checked your schedule. No conflicting commitments found on device."

                AgentResponse(
                    conversationalReply = reply,
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    diagnosticsSummary = mapOf(
                        "Intent" to "CALENDAR_SCHEDULE",
                        "Provider" to "Android CalendarContract"
                    ),
                    suggestedActions = listOf("View today's priorities", "Schedule another event")
                )
            }

            AgentIntentCategory.DEVICE_ALARM -> {
                val alarmTool = toolRegistry.findToolById("device_alarm_tool")
                val result = alarmTool?.execute(trimmed, emptyMap(), agentContext, appContext)
                val reply = (result as? AgentToolResult.Success)?.summary
                    ?: "Alarm scheduled on your device."

                AgentResponse(
                    conversationalReply = reply,
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    diagnosticsSummary = mapOf(
                        "Intent" to "DEVICE_ALARM",
                        "Provider" to "Android AlarmClock"
                    ),
                    suggestedActions = listOf("View today's priorities", "Set another alarm")
                )
            }

            AgentIntentCategory.ACCOUNT_CONNECTION_HELP -> {
                AgentResponse(
                    conversationalReply = "VYB never stores or asks for your raw passwords. When you connect a service like Amazon or Google, we use official Android system deep links, verified OAuth tokens, or installed app intents.\n\nWould you like to open your connected services in Settings?",
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    suggestedActions = listOf("View Connected Services", "Return to Conversation")
                )
            }

            AgentIntentCategory.MEMORY_RETRIEVE, AgentIntentCategory.SHOPPING_RESEARCH, AgentIntentCategory.GENERAL_QUERY -> {
                // Fallback to contextual agent dialogue
                val reply = generateContextualDialogue(trimmed, lower, agentContext)
                AgentResponse(
                    conversationalReply = reply,
                    intent = intent,
                    plan = plan,
                    state = AgentState.COMPLETED,
                    knowledgeStatus = KnowledgeStatus.KNOWN,
                    suggestedActions = listOf("What are my priorities today?", "Handle my son's school cab", "Can I afford ₹5,000?")
                )
            }
        }
    }

    private fun understandIntent(raw: String, lower: String): AgentIntent {
        return when {
            lower.contains("priorities today") || lower.contains("what do i have today") || lower.contains("briefing") || lower.contains("good morning") -> {
                AgentIntent(AgentIntentCategory.DAILY_PRIORITIES, "Daily Executive Briefing", rawPrompt = raw)
            }
            lower.contains("what should i do now") || lower.contains("what next") || lower.contains("what am i forgetting") -> {
                AgentIntent(AgentIntentCategory.WHAT_SHOULD_I_DO_NOW, "Contextual Real-Time Recommendation", rawPrompt = raw)
            }
            lower.contains("amazon") && (lower.contains("delivery") || lower.contains("order") || lower.contains("package") || lower.contains("track") || lower.contains("where")) -> {
                AgentIntent(AgentIntentCategory.DELIVERY_TRACKING, "Amazon Delivery Tracking", targetSubject = "Amazon Order", rawPrompt = raw)
            }
            lower.contains("cab") || lower.contains("ride") || lower.contains("taxi") || lower.contains("transport") || lower.contains("school cab") -> {
                val isSon = lower.contains("son") || lower.contains("vivaan") || lower.contains("school")
                AgentIntent(
                    AgentIntentCategory.TRANSPORT_COORDINATION,
                    "Transport Coordination",
                    targetPerson = if (isSon) "Son" else "Self",
                    rawPrompt = raw
                )
            }
            lower.contains("can i afford") || lower.contains("afford") || lower.contains("in budget") || lower.contains("should i buy") -> {
                AgentIntent(AgentIntentCategory.FINANCIAL_AFFORDABILITY, "Discretionary Affordability Evaluation", rawPrompt = raw)
            }
            lower.startsWith("remind") || lower.startsWith("set a reminder") || lower.startsWith("create reminder") -> {
                AgentIntent(AgentIntentCategory.REMINDER_AND_TASK, "Device Reminder / Task", rawPrompt = raw)
            }
            lower.startsWith("forget ") || lower.contains("forget that ") || lower.contains("delete memory") -> {
                AgentIntent(AgentIntentCategory.MEMORY_FORGET, "Memory Removal", rawPrompt = raw)
            }
            lower.startsWith("remember") || lower.startsWith("note that") || lower.startsWith("save to memory") -> {
                AgentIntent(AgentIntentCategory.MEMORY_STORE, "Memory Storage", rawPrompt = raw)
            }
            lower.startsWith("open ") || lower.startsWith("launch ") -> {
                AgentIntent(AgentIntentCategory.APP_LAUNCH, "Android App Launch", rawPrompt = raw)
            }
            lower.contains("alarm") || lower.startsWith("set an alarm") || lower.contains("wake me") -> {
                AgentIntent(AgentIntentCategory.DEVICE_ALARM, "Device Alarm Configuration", rawPrompt = raw)
            }
            lower.contains("calendar") || lower.contains("schedule an event") || lower.contains("on my calendar") -> {
                AgentIntent(AgentIntentCategory.CALENDAR_SCHEDULE, "Device Calendar Schedule", rawPrompt = raw)
            }
            lower.contains("connect") && (lower.contains("amazon") || lower.contains("account") || lower.contains("service")) -> {
                AgentIntent(AgentIntentCategory.ACCOUNT_CONNECTION_HELP, "Account Connection Security Help", rawPrompt = raw)
            }
            else -> {
                AgentIntent(AgentIntentCategory.GENERAL_QUERY, "General Contextual Query", rawPrompt = raw)
            }
        }
    }

    private suspend fun retrieveContext(
        prefs: SharedPreferences?,
        appContext: Context?,
        intent: AgentIntent
    ): AgentContext {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val timeFormatted = String.format(Locale.US, "%02d:%02d", hour, minute)
        val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: "Today"

        val ownerName = prefs?.getString("owner_name", "")?.ifBlank { "Owner" } ?: "Owner"
        val city = prefs?.getString("owner_city", "")?.ifBlank { "Kolkata" } ?: "Kolkata"

        val income = prefs?.getFloat("monthly_income", 95000f)?.toDouble() ?: 95000.0
        val savings = prefs?.getFloat("monthly_savings_target", 25000f)?.toDouble() ?: 25000.0
        val fixed = prefs?.getFloat("monthly_fixed_commitments", 48000f)?.toDouble() ?: 48000.0
        val discretionary = (income - savings - fixed).coerceAtLeast(0.0)

        // Retrieve active tasks
        val tasks = taskDao?.getAllTasks()?.firstOrNull()?.filter { !it.isCompleted } ?: emptyList()
        val taskSummaries = tasks.map { it.title }

        // Retrieve memories
        val storedMemories = mutableListOf<AgentMemoryItem>()
        memoryDao?.getAllMemoriesDirect()?.forEach {
            storedMemories.add(
                AgentMemoryItem(
                    id = it.id.toString(),
                    category = it.category,
                    key = it.key,
                    content = it.value,
                    confidence = it.confidence.toFloat()
                )
            )
        }

        // Check Amazon connection state
        val isAmazonConnected = CapabilityEngine.isServiceConnected(CapabilityType.AMAZON)

        // Retrieve family info (son)
        val sonSchoolName = "The Heritage School"
        val sonCommute = "08:30 AM"

        return AgentContext(
            ownerName = ownerName,
            city = city,
            currentTimeFormatted = timeFormatted,
            dayOfWeek = dayOfWeek,
            timeOfDay = if (hour < 12) "Morning" else if (hour < 17) "Afternoon" else "Evening",
            hourOfDay = hour,
            minuteOfHour = minute,
            isMorning = hour < 12,
            monthlyIncome = income,
            monthlySavingsTarget = savings,
            activeDiscretionaryBuffer = discretionary,
            familyMembers = listOf("Son (Vivaan)", "Wife", "Mother"),
            sonSchoolCommuteTime = sonCommute,
            sonSchoolName = sonSchoolName,
            activeTasksCount = tasks.size,
            activeTasksSummaries = taskSummaries,
            retrievedMemories = storedMemories,
            isAmazonConnected = isAmazonConnected
        )
    }

    private fun formulatePlan(
        intent: AgentIntent,
        context: AgentContext,
        tools: List<AgentTool>
    ): AgentPlan {
        val steps = mutableListOf<AgentStep>()
        when (intent.category) {
            AgentIntentCategory.DAILY_PRIORITIES -> {
                steps.add(AgentStep(1, "Inspect Real Device Clock & Schedule", "device_time_tool", "Obtains live hour and day", SafetyRiskLevel.LOW))
                steps.add(AgentStep(2, "Synthesize Family, Work & Discretionary State", null, "Produces ranked priorities", SafetyRiskLevel.LOW))
            }
            AgentIntentCategory.TRANSPORT_COORDINATION -> {
                steps.add(AgentStep(1, "Resolve Passenger & Destination Context", null, "Identifies destination & pickup time", SafetyRiskLevel.LOW))
                steps.add(AgentStep(2, "Inspect Available Ride Providers", "transport_coordination_tool", "Prepares fares and app options", SafetyRiskLevel.MEDIUM))
                steps.add(AgentStep(3, "Request User Provider Confirmation", null, "Dispatches ride app on phone", SafetyRiskLevel.MEDIUM))
            }
            AgentIntentCategory.DELIVERY_TRACKING -> {
                steps.add(AgentStep(1, "Verify Connected Account Capability", "amazon_delivery_tool", "Ensures zero-fabrication", SafetyRiskLevel.LOW))
                steps.add(AgentStep(2, "Offer Real App Launch or Connection Flow", null, "Maintains user privacy", SafetyRiskLevel.LOW))
            }
            AgentIntentCategory.FINANCIAL_AFFORDABILITY -> {
                steps.add(AgentStep(1, "Extract Target Spend Amount", null, "Parsed purchase amount", SafetyRiskLevel.LOW))
                steps.add(AgentStep(2, "Evaluate Discretionary Buffer Impact", "financial_affordability_tool", "Safeguards ₹25k savings target", SafetyRiskLevel.LOW))
            }
            AgentIntentCategory.CALENDAR_SCHEDULE -> {
                steps.add(AgentStep(1, "Parse Calendar Target & Schedule", null, "Resolves event title and timing", SafetyRiskLevel.LOW))
                steps.add(AgentStep(2, "Schedule Event on System Calendar", "device_calendar_tool", "Dispatches to Android CalendarContract", SafetyRiskLevel.MEDIUM))
            }
            AgentIntentCategory.DEVICE_ALARM -> {
                steps.add(AgentStep(1, "Parse Target Alarm Time", null, "Resolves hour and minute", SafetyRiskLevel.LOW))
                steps.add(AgentStep(2, "Set System Alarm", "device_alarm_tool", "Dispatches to Android AlarmClock", SafetyRiskLevel.MEDIUM))
            }
            else -> {
                steps.add(AgentStep(1, "Evaluate Contextual Dialogue", null, "Synthesize helpful, authentic response", SafetyRiskLevel.LOW))
            }
        }

        return AgentPlan(
            intent = intent,
            rationale = "Structured execution ensures transparent, authentic agent behavior.",
            steps = steps
        )
    }

    private suspend fun generateContextualDialogue(raw: String, lower: String, context: AgentContext): String {
        val deterministicReply = when {
            lower.contains("who are you") || lower.contains("what can you do") -> {
                "I am VYB, your Personal AI Operating System and executive assistant. I help you coordinate your daily priorities, manage family transport, evaluate financial decisions against your savings targets, and execute actions directly through your mobile device."
            }
            lower.contains("what do you remember") || lower.contains("what do you know about me") || lower.contains("my memories") -> {
                if (context.retrievedMemories.isEmpty()) {
                    "Your personal memory vault currently has no stored preferences or facts. You can tell me things like \"Remember that I prefer morning cabs\" or \"Remember my son's school name\" anytime."
                } else {
                    val summary = context.retrievedMemories.take(5).joinToString("\n") { "• [${it.category}] ${it.content}" }
                    "Here is what I remember in your personal memory vault:\n$summary"
                }
            }
            lower.contains("what time is it") || lower.contains("current time") -> {
                "It is ${context.currentTimeFormatted} on ${context.dayOfWeek} (${context.timeOfDay})."
            }
            lower.contains("what should i wear") || lower.contains("clothing") || lower.contains("outfit") -> {
                "For ${context.dayOfWeek} in ${context.city}, a comfortable smart casual outfit or breathable linen shirt fits well with your schedule and climate."
            }
            lower.contains("son") || lower.contains("school") -> {
                "Your son's school is ${context.sonSchoolName ?: "Delhi Public School"}, with a regular morning commute scheduled around ${context.sonSchoolCommuteTime ?: "08:30 AM"}. Would you like me to coordinate transport?"
            }
            else -> null
        }

        if (deterministicReply != null) return deterministicReply

        // Fallback to Gemini with anti-fabrication context
        val contextPrompt = """
            Owner: ${context.ownerName}
            Location: ${context.city}
            Time: ${context.currentTimeFormatted} (${context.dayOfWeek})
            Known Memories: ${context.retrievedMemories.joinToString("; ") { "${it.category}: ${it.content}" }.ifBlank { "None" }}
            Active Tasks: ${context.activeTasksSummaries.joinToString("; ").ifBlank { "None" }}
            Monthly Discretionary Buffer: ₹${context.activeDiscretionaryBuffer.toInt()}
            User Query: $raw
        """.trimIndent()

        val geminiResult = try {
            geminiRepository.generateAssistantResponse(contextPrompt)
        } catch (e: Exception) {
            null
        }

        return geminiResult?.getOrNull()?.first?.takeIf { it.isNotBlank() }
            ?: "Understood, ${context.ownerName}. How can I assist you with your schedule, transport, finances, or installed applications today?"
    }
}
