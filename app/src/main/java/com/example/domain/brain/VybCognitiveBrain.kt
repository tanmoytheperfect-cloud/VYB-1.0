package com.example.domain.brain

import com.example.data.model.*
import com.example.domain.actions.ActionLevel
import com.example.domain.actions.ActionStatus
import com.example.domain.actions.VybAction

data class CognitiveCognitionResult(
    val understoodGoal: String,
    val executiveReply: String,
    val intentPayload: VybIntentPayload,
    val pendingAction: VybAction? = null,
    val learnedPreference: LearnedPreference? = null,
    val retainedContext: List<String> = emptyList()
)

/**
 * The Central Cognitive Brain of VYB.
 * Processes natural language conversations by evaluating meaning, goals, connected world models,
 * conversation history, reference resolution, authentic capabilities, and device app execution.
 */
class VybCognitiveBrain(
    var worldModel: PersonalWorldModel = PersonalWorldModel.createDefault(),
    var memoryEngine: CognitiveMemoryEngine = CognitiveMemoryEngine(worldModel)
) {

    fun updateWorldModel(newModel: PersonalWorldModel) {
        this.worldModel = newModel
        this.memoryEngine = CognitiveMemoryEngine(newModel)
    }

    /**
     * Core Cognitive Pipeline:
     * 1. UNDERSTAND MEANING & GOAL (Meaning > Keywords)
     * 2. RESOLVE PRONOUNS & CONTEXT REFERENCES (it, this, that, one, him, her)
     * 3. QUERY CONNECTED WORLD MODEL (Family, Work, Finance, Routines, Goals)
     * 4. EVALUATE CAPABILITIES & APPS (WhatsApp, Cabs, Maps, Amazon, Gmail, Dialer)
     * 5. FORMULATE CALM, PRACTICAL, HONEST PA ACTION & RESPONSE WITH EXPLICIT OWNER PERMISSION
     * 6. UPDATE COGNITIVE MEMORY & RECORD TURNS
     */
    fun processCognitiveInput(userInput: String): CognitiveCognitionResult {
        val trimmed = userInput.trim()
        val lower = trimmed.lowercase()

        // 1. Resolve references & temporal context
        val ref = memoryEngine.resolveReferences(trimmed)
        val temporal = memoryEngine.resolveTemporalContext(trimmed)
        val turns = memoryEngine.getRecentTurns(6)

        // 2. Classify semantic cognitive goal
        val goal = identifyCognitiveGoal(trimmed, lower, ref, turns)

        // 3. Process based on Goal with World Model reasoning
        val result = when (goal) {
            CognitiveGoal.CONNECT_ACCOUNT_HELP -> handleConnectAccountHelp(trimmed, lower)
            CognitiveGoal.TRACK_ORDER_QUERY -> handleTrackOrderQuery(trimmed, lower)
            CognitiveGoal.DAILY_PRIORITIES_BRIEFING -> handleDailyBriefing(trimmed, temporal)
            CognitiveGoal.AFFORDABILITY_DECISION -> handleAffordabilityDecision(trimmed, ref)
            CognitiveGoal.TRANSPORT_REQUEST -> handleTransportRequest(trimmed, ref, temporal)
            CognitiveGoal.HOLD_TRANSPORT_BOOKING -> handleHoldTransportBooking(ref)
            CognitiveGoal.STORE_EXPLICIT_PREFERENCE -> handleStorePreference(trimmed, lower)
            CognitiveGoal.VAGUE_QUERY_DISAMBIGUATION -> handleVagueQuery(trimmed)
            CognitiveGoal.PRODUCT_SEARCH_FOLLOWUP -> handleProductSearch(trimmed, ref)
            CognitiveGoal.APP_EXECUTION_WHATSAPP -> handleWhatsAppExecution(trimmed, lower)
            CognitiveGoal.APP_EXECUTION_NAVIGATION -> handleNavigationExecution(trimmed, lower)
            CognitiveGoal.APP_EXECUTION_CALL -> handleCallExecution(trimmed, lower)
            CognitiveGoal.GENERAL_CONVERSATION -> handleGeneralConversation(trimmed, ref)
        }

        // 4. Update memory engine turn history
        memoryEngine.recordTurn(
            role = "USER",
            message = trimmed,
            detectedGoal = goal.name,
            entity = ref.resolvedSubject
        )
        memoryEngine.recordTurn(
            role = "ASSISTANT",
            message = result.executiveReply,
            detectedGoal = goal.name,
            entity = ref.resolvedSubject
        )

        // 5. If new preference was learned, store it
        result.learnedPreference?.let {
            memoryEngine.learnPreference(it.category, it.preference, it.source)
        }

        return result
    }

    private enum class CognitiveGoal {
        CONNECT_ACCOUNT_HELP,
        TRACK_ORDER_QUERY,
        DAILY_PRIORITIES_BRIEFING,
        AFFORDABILITY_DECISION,
        TRANSPORT_REQUEST,
        HOLD_TRANSPORT_BOOKING,
        STORE_EXPLICIT_PREFERENCE,
        VAGUE_QUERY_DISAMBIGUATION,
        PRODUCT_SEARCH_FOLLOWUP,
        APP_EXECUTION_WHATSAPP,
        APP_EXECUTION_NAVIGATION,
        APP_EXECUTION_CALL,
        GENERAL_CONVERSATION
    }

    private fun identifyCognitiveGoal(
        raw: String,
        lower: String,
        ref: ResolvedReferenceResult,
        turns: List<CognitiveMemoryEngine.ConversationTurn>
    ): CognitiveGoal {
        // Direct WhatsApp / messaging intent
        if (lower.startsWith("send whatsapp") || lower.startsWith("whatsapp to") || lower.contains("message on whatsapp") || lower.contains("send message to")) {
            return CognitiveGoal.APP_EXECUTION_WHATSAPP
        }

        // Direct Maps / Navigation intent
        if (lower.startsWith("navigate to") || lower.startsWith("take me to") || lower.startsWith("directions to") || lower.contains("open maps for")) {
            return CognitiveGoal.APP_EXECUTION_NAVIGATION
        }

        // Direct Phone / Call intent
        if (lower.startsWith("call ") || lower.startsWith("dial ") || lower.contains("phone call to")) {
            return CognitiveGoal.APP_EXECUTION_CALL
        }

        // Goal 1: Explicit preference statement
        if (lower.startsWith("remember") || lower.startsWith("note that") || lower.contains("save preference") || lower.contains("my preference is")) {
            return CognitiveGoal.STORE_EXPLICIT_PREFERENCE
        }

        // Goal 2: Hold/Cancel an in-progress booking
        if (lower.contains("don't book yet") || lower.contains("dont book yet") || lower.contains("do not book yet") || lower.contains("hold on, don't book")) {
            return CognitiveGoal.HOLD_TRANSPORT_BOOKING
        }

        // Goal 3: Account access / Connection query
        if ((lower.contains("give access") || lower.contains("connect") || lower.contains("how do i add") || lower.contains("link account") || lower.contains("authorize")) &&
            (lower.contains("amazon") || lower.contains("gmail") || lower.contains("bank") || lower.contains("account"))
        ) {
            return CognitiveGoal.CONNECT_ACCOUNT_HELP
        }

        // Goal 4: Order tracking
        if ((lower.contains("where is my") || lower.contains("track") || lower.contains("status of")) &&
            (lower.contains("delivery") || lower.contains("package") || lower.contains("order") || lower.contains("shipment"))
        ) {
            return CognitiveGoal.TRACK_ORDER_QUERY
        }

        // Goal 5: Daily Priorities / Briefing / Life Context
        if (lower.contains("what matters today") || lower.contains("what matters now") || lower.contains("what matters") ||
            lower.contains("what should i do") || lower.contains("what do you know about my day") ||
            lower.contains("what is my next important thing") || lower.contains("next important thing") ||
            lower.contains("priorities today") || lower.contains("plan for today") || lower.contains("briefing") ||
            lower.contains("good morning") || lower.contains("morning briefing") || lower.contains("what do i have")) {
            return CognitiveGoal.DAILY_PRIORITIES_BRIEFING
        }

        // Goal 6: Affordability / Purchase evaluation
        if (lower.contains("how much can i spend") || lower.contains("should i buy") || lower.contains("can i afford") || lower.contains("is it in budget") || lower.contains("afford")) {
            return CognitiveGoal.AFFORDABILITY_DECISION
        }

        // Goal 7: Cab / Transport request
        if (lower.contains("cab") || lower.contains("ride") || lower.contains("transport") || lower.contains("taxi") || ref.isSonReference) {
            return CognitiveGoal.TRANSPORT_REQUEST
        }

        // Goal 8: Vague request without context
        if (lower == "find me something" || lower == "find me something." || lower == "get me something" || lower == "look for something") {
            return CognitiveGoal.VAGUE_QUERY_DISAMBIGUATION
        }

        // Goal 9: Followup on previous product/item
        if ((lower.contains("find me one") || lower.contains("find one") || lower.contains("show me options")) && ref.resolvedSubject != null) {
            return CognitiveGoal.PRODUCT_SEARCH_FOLLOWUP
        }

        // Goal 10: General context-aware conversation
        return CognitiveGoal.GENERAL_CONVERSATION
    }

    // ==========================================
    // HANDLERS FOR AUTHENTIC PA INTELLIGENCE
    // ==========================================

    private fun handleConnectAccountHelp(raw: String, lower: String): CognitiveCognitionResult {
        val serviceName = if (lower.contains("amazon")) "Amazon" else if (lower.contains("gmail")) "Gmail" else if (lower.contains("bank")) "Banking" else "External Account"

        val reply = "VYB executes actions on your device through verified Android system intents and official deep links.\n\n" +
                "For your security, I never ask for or store your raw $serviceName password. When you authorize an action, I launch the installed app directly on your phone with your pre-filled task parameters."

        val payload = VybIntentPayload(
            intentType = "GENERAL",
            title = "Device Security & Integration: $serviceName",
            summary = "Direct device integration for $serviceName",
            suggestedFollowUps = listOf("View Connected Mobile Apps", "What capabilities can you execute?", "Return to conversation")
        )

        return CognitiveCognitionResult(
            understoodGoal = "CONNECT_ACCOUNT_HELP",
            executiveReply = reply,
            intentPayload = payload,
            retainedContext = listOf("Security Protocol: Zero password storage", "$serviceName: Device Intent Ready")
        )
    }

    private fun handleTrackOrderQuery(raw: String, lower: String): CognitiveCognitionResult {
        val reply = "To track your shipment safely, I can open your installed Amazon or delivery app directly.\n\n" +
                "Please confirm if you would like me to launch Amazon Order Tracking."

        val action = VybAction(
            title = "Launch Amazon Orders",
            description = "Opens Amazon app on your phone to track shipments",
            actionType = "APP_LAUNCH",
            level = ActionLevel.LEVEL_0_INFORMATIONAL,
            status = ActionStatus.WAITING_FOR_CONFIRMATION,
            consequenceSummary = "Opens Amazon application on device."
        )

        val payload = VybIntentPayload(
            intentType = "TASK",
            title = "Amazon Order Tracking",
            summary = "Ready to launch Amazon app",
            suggestedFollowUps = listOf("Authorize & Open Amazon", "Check local tasks instead", "Today's briefing")
        )

        return CognitiveCognitionResult(
            understoodGoal = "TRACK_ORDER_QUERY",
            executiveReply = reply,
            intentPayload = payload,
            pendingAction = action,
            retainedContext = listOf("Action: Open Amazon Orders")
        )
    }

    private fun handleDailyBriefing(raw: String, temporal: TemporalContext): CognitiveCognitionResult {
        val owner = worldModel.owner
        val familyMembers = worldModel.family.members
        val fc = worldModel.finance
        val dayTitle = temporal.targetDayLabel
        val cur = fc.currencySymbol
        val currentTime = temporal.currentTimeFormatted
        val timeOfDay = temporal.timeOfDay

        val familySummary = if (familyMembers.isNotEmpty()) {
            val first = familyMembers.first()
            val commute = if (first.morningCommuteTime.isNotBlank()) "(${first.morningCommuteTime})" else ""
            "${first.relation} ${first.name}: ${first.occupationOrSchool} $commute"
        } else {
            "No family routines scheduled"
        }

        val workSummary = if (worldModel.work.companyName.isNotBlank()) {
            "${worldModel.work.companyName} (${worldModel.work.role}) — ${worldModel.work.officeAddress}"
        } else {
            "Not configured in Personal Life Vault"
        }

        val financeSummary = if (fc.monthlyIncome > 0) {
            "Monthly income is $cur${fc.monthlyIncome.toInt()}, with $cur${fc.monthlySavingsTarget.toInt()} dedicated to your savings target and $cur${fc.activeDiscretionaryBuffer.toInt()} in active discretionary buffer."
        } else {
            "Financial baseline not configured (figures are kept strictly private on-device)."
        }

        val reply = "Good $timeOfDay, ${owner.title}. Current time is $currentTime.\n\n" +
                "Here is your executive overview for $dayTitle:\n\n" +
                "1. Family & Routine: $familySummary.\n" +
                "2. Business / Workplace: $workSummary.\n" +
                "3. Financial Health: $financeSummary\n" +
                "4. Device App Execution: I require your explicit authorization before launching any connected app or dispatching tasks."

        val payload = VybIntentPayload(
            intentType = "BRIEFING",
            title = "Executive Overview: $dayTitle",
            summary = "Personalized schedule, family, and financial overview ($currentTime)",
            briefingPlan = DailyBriefingPayload(
                greeting = "Good $timeOfDay, ${owner.title} ($currentTime)",
                dateString = dayTitle,
                weatherSummary = "${owner.city} • Clear & Active • $currentTime",
                sonSchoolTransportTime = familySummary,
                transportStatus = "Cab dispatch ready",
                workPriorities = if (worldModel.work.companyName.isNotBlank()) listOf("${worldModel.work.companyName} review", "Coordinate tasks & approvals") else listOf("Coordinate daily tasks & approvals"),
                discretionaryBudgetInr = fc.activeDiscretionaryBuffer,
                pendingShoppingCount = 0,
                personalGoalsSummary = if (fc.monthlySavingsTarget > 0) "Savings Target: $cur${fc.monthlySavingsTarget.toInt()} protected" else "No active savings target set",
                proactiveVybSuggestion = "Review upcoming schedule and approve pending dispatches."
            ),
            suggestedFollowUps = listOf("Book cab for family", "Send WhatsApp update", "How much can I spend this month?")
        )

        return CognitiveCognitionResult(
            understoodGoal = "DAILY_PRIORITIES_BRIEFING",
            executiveReply = reply,
            intentPayload = payload,
            retainedContext = listOf("Owner: ${owner.name}", "Discretionary: $cur${fc.activeDiscretionaryBuffer.toInt()}")
        )
    }

    private fun handleAffordabilityDecision(raw: String, ref: ResolvedReferenceResult): CognitiveCognitionResult {
        val fc = worldModel.finance
        val cur = fc.currencySymbol
        val amount = ref.budgetInr ?: 8000.0
        val subject = ref.resolvedSubject ?: "Purchase Item"
        val remaining = fc.activeDiscretionaryBuffer - amount

        val reply = if (remaining >= 0) {
            "You have $cur${fc.activeDiscretionaryBuffer.toInt()} available in your monthly discretionary pool (calculated from your $cur${fc.monthlyIncome.toInt()} income, minus $cur${fc.fixedCommitments.toInt()} fixed commitments and your $cur${fc.monthlySavingsTarget.toInt()} savings target).\n\n" +
                    "A spend of $cur${amount.toInt()} on $subject is safe and affordable. It leaves $cur${remaining.toInt()} in discretionary buffer while keeping your $cur${fc.monthlySavingsTarget.toInt()} savings goal 100% intact."
        } else {
            val deficit = -remaining
            "This spend of $cur${amount.toInt()} exceeds your active discretionary pool ($cur${fc.activeDiscretionaryBuffer.toInt()}) by $cur${deficit.toInt()}.\n\n" +
                    "To safeguard your $cur${fc.monthlySavingsTarget.toInt()} monthly savings target without dipping into fixed commitments, I recommend deferring this purchase or selecting an alternative."
        }

        val payload = VybIntentPayload(
            intentType = "DECISION",
            title = "Affordability: $subject ($cur${amount.toInt()})",
            summary = if (remaining >= 0) "Affordable within discretionary pool" else "Exceeds discretionary pool",
            decisionPlan = DecisionPlanPayload(
                decisionSubject = "$subject ($cur${amount.toInt()})",
                itemPriceInr = amount,
                verdict = if (remaining >= 0) "AFFORDABLE & SAFE" else "EXCEEDS DISCRETIONARY BUFFER",
                affordabilityVerdictColor = if (remaining >= 0) "EMERALD" else "ROSE",
                financialImpactAnalysis = if (remaining >= 0) "Reduces discretionary pool to $cur${remaining.toInt()}. Savings target of $cur${fc.monthlySavingsTarget.toInt()} is protected." else "Would dip into $cur${fc.monthlySavingsTarget.toInt()} savings allocation.",
                monthlyDiscretionaryRemaining = remaining.coerceAtLeast(0.0),
                savingsGoalProtected = remaining >= 0,
                keyFactors = listOf("Monthly Income: $cur${fc.monthlyIncome.toInt()}", "Fixed Expenses: $cur${fc.fixedCommitments.toInt()}", "Discretionary Pool: $cur${fc.activeDiscretionaryBuffer.toInt()}", "Target Savings: $cur${fc.monthlySavingsTarget.toInt()}"),
                alternativesConsidered = listOf(
                    DecisionAlternativePayload("Proceed with purchase", amount, "Full utility", remaining >= 0),
                    DecisionAlternativePayload("Hold in liquid savings", 0.0, "Maximizes safety reserve", remaining < 0)
                ),
                finalRecommendation = if (remaining >= 0) "Safe to proceed from discretionary buffer." else "Wait or select a value alternative."
            ),
            suggestedFollowUps = listOf("Log as planned expense", "Check monthly budget", "Back to daily plan")
        )

        return CognitiveCognitionResult(
            understoodGoal = "AFFORDABILITY_DECISION",
            executiveReply = reply,
            intentPayload = payload,
            retainedContext = listOf("Income: $cur${fc.monthlyIncome.toInt()}", "Discretionary: $cur${fc.activeDiscretionaryBuffer.toInt()}")
        )
    }

    private fun handleTransportRequest(
        raw: String,
        ref: ResolvedReferenceResult,
        temporal: TemporalContext
    ): CognitiveCognitionResult {
        // 1. Resolve the passenger from actual VYB memory/world model
        val member = worldModel.family.members.firstOrNull { it.relation.contains("son", ignoreCase = true) || it.name.contains("vivaan", ignoreCase = true) }
            ?: worldModel.family.members.firstOrNull()
        val memberName = if (raw.contains("son", ignoreCase = true) || ref.isSonReference) {
            member?.name?.ifBlank { "Son" } ?: "Son"
        } else {
            "Self"
        }

        // 2. Resolve destination from actual stored context
        val destination = when {
            raw.contains("airport", ignoreCase = true) -> "Airport"
            raw.contains("son", ignoreCase = true) || ref.isSonReference -> member?.occupationOrSchool?.ifBlank { "School / Activity" } ?: "School / Activity"
            raw.contains("office", ignoreCase = true) || raw.contains("work", ignoreCase = true) -> worldModel.work.officeAddress.ifBlank { "Office" }
            else -> member?.occupationOrSchool?.ifBlank { "Destination" } ?: "Destination"
        }

        // 3. Resolve date/time from actual temporal context
        val dayLabel = temporal.targetDayLabel
        val currentTime = temporal.currentTimeFormatted
        val timeOfDay = temporal.timeOfDay

        val timeNote = if (temporal.passedMorningNote != null) {
            "${temporal.passedMorningNote}\n\n"
        } else {
            "Current time is $currentTime ($timeOfDay).\n\n"
        }

        // 4 & 5. Check real ride-provider integration and live fare/ETA availability
        val hasLiveRideIntegration = false
        val hasLiveFareData = false

        val reply = if (!hasLiveRideIntegration) {
            "${timeNote}I can prepare the route details and open a ride provider on your phone for $memberName to $destination ($dayLabel), but I don't currently have live ride-provider access to compare fares or complete a booking.\n\n" +
                    "Opening a ride app is not the same as booking a ride. You can select an installed provider below (Rapido, Uber, Ola, Yatri Sathi, or inDrive) or launch Google Maps navigation."
        } else {
            "${timeNote}Preparing route for $memberName to $destination."
        }

        val action = VybAction(
            title = "Open Ride Provider for $destination",
            description = "Launch ride app for trip to $destination ($memberName)",
            actionType = "OPEN_RIDE_PROVIDER",
            level = ActionLevel.LEVEL_0_INFORMATIONAL,
            status = ActionStatus.WAITING_FOR_CONFIRMATION,
            consequenceSummary = "Opens the selected ride application on your device so you can view live pricing and book your trip.",
            parameters = mapOf(
                "destination" to destination,
                "passenger" to memberName,
                "pickup" to worldModel.owner.homeAddress.ifBlank { "Home (${worldModel.owner.city})" }
            )
        )

        val payload = VybIntentPayload(
            intentType = "RIDE",
            title = "Transport: $memberName to $destination",
            summary = "Live fare API unavailable • Can open installed ride apps or navigation",
            ridePlan = RidePlanPayload(
                pickupLocation = worldModel.owner.homeAddress.ifBlank { "Home (${worldModel.owner.city})" },
                destination = destination,
                estimatedDistance = null,
                estimatedDuration = null,
                liveFareAvailable = hasLiveFareData,
                directBookingAvailable = hasLiveRideIntegration,
                note = "Live fare comparison and booking APIs are unavailable. Opening an app allows you to view live rates directly.",
                rideOptions = listOf(
                    RideOptionPayload(
                        provider = "Rapido",
                        vehicleType = "Open app for live fares",
                        estimatedFare = null,
                        etaMinutes = null,
                        capacity = "Bike / Auto / Cab",
                        appPackage = "com.rapido.passenger",
                        isRecommended = false
                    ),
                    RideOptionPayload(
                        provider = "Uber",
                        vehicleType = "Open app for live fares",
                        estimatedFare = null,
                        etaMinutes = null,
                        capacity = "Go / Premier / Auto",
                        appPackage = "com.ubercab",
                        isRecommended = false
                    ),
                    RideOptionPayload(
                        provider = "Ola",
                        vehicleType = "Open app for live fares",
                        estimatedFare = null,
                        etaMinutes = null,
                        capacity = "Mini / Prime / Auto",
                        appPackage = "com.olacabs.customer",
                        isRecommended = false
                    ),
                    RideOptionPayload(
                        provider = "Yatri Sathi",
                        vehicleType = "Open app for live fares",
                        estimatedFare = null,
                        etaMinutes = null,
                        capacity = "Metered Taxi",
                        appPackage = "com.yatrisathi.customer",
                        isRecommended = false
                    ),
                    RideOptionPayload(
                        provider = "inDrive",
                        vehicleType = "Open app for live fares",
                        estimatedFare = null,
                        etaMinutes = null,
                        capacity = "City Ride / Comfort",
                        appPackage = "sinet.startup.inDriver",
                        isRecommended = false
                    )
                )
            ),
            suggestedFollowUps = listOf("Open Rapido", "Open Uber", "Open inDrive", "Navigate via Google Maps", "Cancel")
        )

        return CognitiveCognitionResult(
            understoodGoal = "TRANSPORT_REQUEST",
            executiveReply = reply,
            intentPayload = payload,
            pendingAction = action,
            retainedContext = listOf(
                "Passenger: $memberName",
                "Destination: $destination",
                "Live Ride API: Unavailable",
                "Action Mode: Open Provider App"
            )
        )
    }

    private fun handleWhatsAppExecution(raw: String, lower: String): CognitiveCognitionResult {
        val cleanMsg = raw.replace("send whatsapp", "", ignoreCase = true)
            .replace("whatsapp to", "", ignoreCase = true)
            .replace("message on whatsapp", "", ignoreCase = true)
            .replace("send message to", "", ignoreCase = true)
            .trim()

        val recipient = if (cleanMsg.contains("supplier", ignoreCase = true)) "Supplier" else if (cleanMsg.contains("wife", ignoreCase = true)) "Spouse" else "Contact"

        val reply = "I have drafted your WhatsApp message to $recipient:\n\n" +
                "\"${cleanMsg.ifBlank { "Hello, following up on our recent conversation." }}\"\n\n" +
                "Please tap **Authorize & Open WhatsApp** to send it via WhatsApp on your phone."

        val action = VybAction(
            title = "WhatsApp: $recipient",
            description = cleanMsg.ifBlank { "Follow-up message" },
            actionType = "COMMUNICATION_DISPATCH",
            level = ActionLevel.LEVEL_2_COMMUNICATION,
            status = ActionStatus.WAITING_FOR_CONFIRMATION,
            consequenceSummary = "Opens WhatsApp composer with pre-filled message."
        )

        val payload = VybIntentPayload(
            intentType = "TASK",
            title = "WhatsApp Draft: $recipient",
            summary = cleanMsg.ifBlank { "Message ready for dispatch" },
            suggestedFollowUps = listOf("Authorize & Open WhatsApp", "Edit Message Draft", "Cancel")
        )

        return CognitiveCognitionResult(
            understoodGoal = "APP_EXECUTION_WHATSAPP",
            executiveReply = reply,
            intentPayload = payload,
            pendingAction = action,
            retainedContext = listOf("App: WhatsApp", "Recipient: $recipient")
        )
    }

    private fun handleNavigationExecution(raw: String, lower: String): CognitiveCognitionResult {
        val dest = raw.replace("navigate to", "", ignoreCase = true)
            .replace("take me to", "", ignoreCase = true)
            .replace("directions to", "", ignoreCase = true)
            .replace("open maps for", "", ignoreCase = true)
            .trim()
            .ifBlank { worldModel.work.officeAddress.ifBlank { "Office" } }

        val reply = "Preparing turn-by-turn navigation to **$dest**.\n\n" +
                "Tap **Authorize & Launch Navigation** to open Google Maps on your phone."

        val action = VybAction(
            title = "Navigate to $dest",
            description = "Google Maps route directions",
            actionType = "NAVIGATION_LAUNCH",
            level = ActionLevel.LEVEL_0_INFORMATIONAL,
            status = ActionStatus.WAITING_FOR_CONFIRMATION,
            consequenceSummary = "Opens Google Maps turn-by-turn directions to $dest."
        )

        val payload = VybIntentPayload(
            intentType = "TASK",
            title = "Map Navigation: $dest",
            summary = "Turn-by-turn route prepared",
            suggestedFollowUps = listOf("Authorize & Launch Navigation", "Find parking nearby", "Cancel")
        )

        return CognitiveCognitionResult(
            understoodGoal = "APP_EXECUTION_NAVIGATION",
            executiveReply = reply,
            intentPayload = payload,
            pendingAction = action,
            retainedContext = listOf("App: Google Maps", "Destination: $dest")
        )
    }

    private fun handleCallExecution(raw: String, lower: String): CognitiveCognitionResult {
        val target = raw.replace("call", "", ignoreCase = true)
            .replace("dial", "", ignoreCase = true)
            .trim()
            .ifBlank { "Contact" }

        val reply = "Ready to dial **$target**.\n\n" +
                "Tap **Authorize & Call** to open your phone dialer."

        val action = VybAction(
            title = "Call $target",
            description = "Initiate phone call via device dialer",
            actionType = "PHONE_CALL",
            level = ActionLevel.LEVEL_2_COMMUNICATION,
            status = ActionStatus.WAITING_FOR_CONFIRMATION,
            consequenceSummary = "Opens phone dialer for $target."
        )

        val payload = VybIntentPayload(
            intentType = "TASK",
            title = "Phone Call: $target",
            summary = "Dialer ready",
            suggestedFollowUps = listOf("Authorize & Call", "Send SMS instead", "Cancel")
        )

        return CognitiveCognitionResult(
            understoodGoal = "APP_EXECUTION_CALL",
            executiveReply = reply,
            intentPayload = payload,
            pendingAction = action,
            retainedContext = listOf("App: Phone Dialer", "Target: $target")
        )
    }

    private fun handleHoldTransportBooking(ref: ResolvedReferenceResult): CognitiveCognitionResult {
        val reply = "Understood, ${worldModel.owner.title}. I have held the action. Nothing has been booked or charged. Let me know whenever you're ready."

        val payload = VybIntentPayload(
            intentType = "GENERAL",
            title = "Action Held",
            summary = "Execution paused per your instruction",
            suggestedFollowUps = listOf("Resume action", "Today's briefing", "Check financial buffer")
        )

        return CognitiveCognitionResult(
            understoodGoal = "HOLD_TRANSPORT_BOOKING",
            executiveReply = reply,
            intentPayload = payload,
            retainedContext = listOf("Action Status: Held / Paused")
        )
    }

    private fun handleStorePreference(raw: String, lower: String): CognitiveCognitionResult {
        val category = when {
            lower.contains("cheap") || lower.contains("cost") || lower.contains("price") || lower.contains("value") -> "FINANCE"
            lower.contains("cab") || lower.contains("travel") || lower.contains("transport") -> "TRANSPORT"
            lower.contains("food") || lower.contains("diet") || lower.contains("protein") -> "HEALTH"
            else -> "GENERAL"
        }

        val learnedPref = LearnedPreference(
            category = category,
            preference = raw.replace("remember", "", ignoreCase = true).replace("note that", "", ignoreCase = true).trim(),
            source = "Explicit User Statement",
            confidence = 1.0
        )

        val reply = "I have saved that to your Personal World Model: \"${learnedPref.preference}\". I will naturally factor this into future calculations and actions."

        val payload = VybIntentPayload(
            intentType = "TASK",
            title = "Preference Saved to Memory",
            summary = learnedPref.preference,
            suggestedFollowUps = listOf("What do you remember about me?", "How much can I spend this month?", "Daily briefing")
        )

        return CognitiveCognitionResult(
            understoodGoal = "STORE_EXPLICIT_PREFERENCE",
            executiveReply = reply,
            intentPayload = payload,
            learnedPreference = learnedPref,
            retainedContext = listOf("New Preference: ${learnedPref.preference}")
        )
    }

    private fun handleVagueQuery(raw: String): CognitiveCognitionResult {
        val reply = "I know you're looking for an item, but could you specify what you'd like me to find? (For example: a phone, laptop, groceries on Blinkit, or household item?)"

        val payload = VybIntentPayload(
            intentType = "GENERAL",
            title = "Specify Product Request",
            summary = "Awaiting item category and parameters",
            suggestedFollowUps = listOf("Find a smartphone under ₹30,000", "Order groceries on Blinkit", "Check shopping list")
        )

        return CognitiveCognitionResult(
            understoodGoal = "VAGUE_QUERY_DISAMBIGUATION",
            executiveReply = reply,
            intentPayload = payload,
            retainedContext = listOf("State: Awaiting user specification")
        )
    }

    private fun handleProductSearch(raw: String, ref: ResolvedReferenceResult): CognitiveCognitionResult {
        val cur = worldModel.finance.currencySymbol
        val subject = ref.resolvedSubject ?: "Item"
        val budget = ref.budgetInr

        if (budget != null) {
            memoryEngine.setActiveFocus("SHOPPING_ITEM", subject, mapOf("budget" to "$cur${budget.toInt()}"))
        } else {
            memoryEngine.setActiveFocus("SHOPPING_ITEM", subject, emptyMap())
        }

        val budgetText = if (budget != null) " around $cur${budget.toInt()}" else ""
        val bufferText = if (worldModel.finance.activeDiscretionaryBuffer > 0) " (Discretionary buffer: $cur${worldModel.finance.activeDiscretionaryBuffer.toInt()})" else ""

        val reply = if (budget != null) {
            "Understood. Searching for $subject around $cur${budget.toInt()}.$bufferText\n\n" +
                    "Tap below to search directly on shopping apps."
        } else {
            "Understood. Searching for $subject.$bufferText\n\n" +
                    "Tap below to search directly on shopping apps."
        }

        val searchLabel = if (budget != null) "$subject ($cur${budget.toInt()})" else subject
        val action = VybAction(
            title = "Search $subject on Shopping Apps",
            description = "Search for $searchLabel",
            actionType = "SHOPPING_SEARCH",
            level = ActionLevel.LEVEL_0_INFORMATIONAL,
            status = ActionStatus.WAITING_FOR_CONFIRMATION,
            consequenceSummary = "Opens shopping app with query \"$subject\"."
        )

        val payload = VybIntentPayload(
            intentType = "SHOPPING",
            title = "Product Search: $searchLabel",
            summary = "Options for $searchLabel",
            shoppingList = ShoppingListPayload(
                title = "$subject Search",
                estimatedTotal = budget ?: 0.0,
                items = listOf(
                    ShoppingItemPayload(
                        name = subject,
                        category = "General",
                        quantity = "1 Unit",
                        estimatedCost = budget ?: 0.0
                    )
                )
            ),
            suggestedFollowUps = listOf("Open shopping app", "Hold for now")
        )

        val budgetContext = if (budget != null) "$cur${budget.toInt()}" else "Unset"
        return CognitiveCognitionResult(
            understoodGoal = "PRODUCT_SEARCH_FOLLOWUP",
            executiveReply = reply,
            intentPayload = payload,
            pendingAction = action,
            retainedContext = listOf("Item: $subject", "Budget: $budgetContext")
        )
    }

    private fun handleGeneralConversation(raw: String, ref: ResolvedReferenceResult): CognitiveCognitionResult {
        if (raw.lowercase().contains("buy a new phone") || raw.lowercase().contains("buy a phone")) {
            memoryEngine.setActiveFocus("SHOPPING_ITEM", "Smartphone")
            val reply = "What budget should I stay within for the new phone?"
            val payload = VybIntentPayload(
                intentType = "SHOPPING",
                title = "Smartphone Planning",
                summary = "Setting budget and parameters",
                suggestedFollowUps = listOf("Around 30,000", "Under 20,000", "Flagship above 50,000")
            )
            return CognitiveCognitionResult(
                understoodGoal = "GENERAL_CONVERSATION",
                executiveReply = reply,
                intentPayload = payload,
                retainedContext = listOf("Subject: Smartphone (Awaiting budget)")
            )
        }

        val owner = worldModel.owner
        val learnedPrefs = worldModel.learnedPreferences
        val family = worldModel.family.members
        val routines = worldModel.routines
        val projects = worldModel.activeProjects
        val finance = worldModel.finance
        val turns = memoryEngine.getRecentTurns()

        val lower = raw.lowercase()
        val isRecallQuery = lower.contains("remember") || lower.contains("recall") ||
                lower.contains("what do you know") || lower.contains("tell me what you know") ||
                lower.contains("what do you have") || lower.contains("what did i tell you")

        val categorySections = mutableListOf<String>()
        var totalRecords = 0

        // 1. IDENTITY
        if (owner.name.isNotBlank() && owner.name != "Owner") {
            val roleStr = if (owner.jobTitle.isNotBlank() || owner.companyName.isNotBlank()) " — ${owner.jobTitle.ifBlank { "Executive" }} at ${owner.companyName.ifBlank { "Company" }}" else ""
            categorySections.add("### 👤 IDENTITY\n• Name: ${owner.name}$roleStr")
            totalRecords++
        }

        // 2. FAMILY
        if (family.isNotEmpty()) {
            val lines = family.map { "• ${it.name} (${it.relation}): ${it.notes.ifBlank { "Family Member" }}" }
            categorySections.add("### 👨‍👩‍👧‍👦 FAMILY\n" + lines.joinToString("\n"))
            totalRecords += family.size
        }

        // 3. ROUTINES
        if (routines.isNotEmpty()) {
            val lines = routines.take(4).map { "• ${it.timeOfDay}: ${it.title} (${it.days})" }
            categorySections.add("### ⏰ ROUTINES\n" + lines.joinToString("\n"))
            totalRecords += routines.size
        }

        // 4. PROJECTS
        if (projects.isNotEmpty()) {
            val lines = projects.take(4).map { "• ${it.name} [${it.currentPhase}]" }
            categorySections.add("### 🚀 PROJECTS & WORK\n" + lines.joinToString("\n"))
            totalRecords += projects.size
        }

        // 5. FINANCE
        if (finance.monthlyIncome > 0) {
            categorySections.add("### 💰 FINANCE\n• Monthly Income: ${finance.currencySymbol}${finance.monthlyIncome.toInt()} | Savings Target: ${finance.currencySymbol}${finance.monthlySavingsTarget.toInt()}")
            totalRecords++
        }

        // 6. PREFERENCES & MEMORY
        if (learnedPrefs.isNotEmpty()) {
            val lines = learnedPrefs.take(6).map { "• ${it.preference} *[${it.category}]*" }
            categorySections.add("### ⭐ PREFERENCES & MEMORIES\n" + lines.joinToString("\n"))
            totalRecords += learnedPrefs.size
        }

        val hasData = totalRecords > 0
        val reply: String
        val cardSummary: String

        if (lower.contains("just tell") || lower.contains("last tell")) {
            val latestTurn = turns.lastOrNull { it.role == "USER" }
            if (latestTurn != null) {
                reply = "In our current conversation, you recently said: \"${latestTurn.message}\" [Conversation Context - Current Session]"
                cardSummary = "Recent conversational context retrieved"
            } else {
                reply = "I don't have any recent statements recorded in our current conversation session."
                cardSummary = "No recent conversation statements"
            }
        } else if (hasData) {
            val sb = StringBuilder("Here is what I have stored in your Personal Memory:\n\n")
            sb.append(categorySections.joinToString("\n\n"))
            sb.append("\n\n*[Storage: LOCAL ON-DEVICE ONLY • Verified via Room Database]*")
            reply = sb.toString().trim()
            cardSummary = "$totalRecords records available across ${categorySections.size} categories"
        } else {
            reply = "I don't currently have any saved personal memories I can confidently recall. No personal vault facts, routines, or profile details are stored yet. You can teach me by saying 'Remember that...' or configuring your profile."
            cardSummary = "Unconfigured (0 memories stored)"
        }

        val payload = VybIntentPayload(
            intentType = "GENERAL",
            title = "Personal Life Context",
            summary = cardSummary,
            suggestedFollowUps = if (hasData) listOf("What matters today?", "What else do you remember?", "Add a task") else listOf("Remember that...", "Set up profile", "Add a task")
        )
        return CognitiveCognitionResult(
            understoodGoal = if (isRecallQuery) "MEMORY_RECALL" else "GENERAL_CONVERSATION",
            executiveReply = reply,
            intentPayload = payload,
            retainedContext = listOf("Records Available: $totalRecords")
        )
    }
}
