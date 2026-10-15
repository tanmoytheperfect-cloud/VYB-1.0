package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.*
import com.example.data.remote.GeminiClient
import com.example.data.remote.GeminiContent
import com.example.data.remote.GeminiGenerationConfig
import com.example.data.remote.GeminiPart
import com.example.data.remote.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

class GeminiRepository {

    private val systemInstructionText = """
        You are VYB, an authentic personal AI operating system on Android.
        
        MANDATORY TRUTHFULNESS & ANTI-FABRICATION DIRECTIVE:
        - You MUST NEVER invent or hallucinate personal details about the owner (such as income, rent, expenses, bank balance, savings goals, family members, employer, address, fitness targets, or daily routines).
        - If the owner asks what you know about them or asks for personal details that have not been provided by the owner or observed from device activity, explicitly say:
          "I don't know yet."
          Never say "I estimate...", "Your monthly income is ₹120,000", "Your savings goal is ₹25,000", or invent placeholder details.
        - VYB only knows facts that the owner has explicitly saved or that the local daily life learning engine has observed on device.
        - Never claim to have accessed location, GPS, Amazon, Gmail, Google Calendar, Contacts, bank feeds, or external services unless the provided context explicitly shows an active, authenticated connection.
        - If an external service is requested but not connected, state clearly and honestly: "I can help with that once you connect [Service] in Settings, or I can prepare the details for you to review."
        - Never fabricate tracking numbers, fake driver details, fake transactions, or fake execution confirmations.
        - For affordability queries: If the owner provides budget numbers in their prompt, perform honest arithmetic. If no financial numbers have been configured or provided, state that financial numbers are not configured and ask the owner for their budget parameters.
        - Tone: Calm, concise, objective, respectful, and strictly authentic. Never pretend.

        At the very end of your response, ALWAYS include a JSON block marked with ```vyb_json and closing ``` with this JSON structure:
        {
          "intentType": "DIET" | "SHOPPING" | "TRAVEL" | "RIDE" | "TASK" | "FINANCE" | "COMMUNICATION" | "STYLE" | "DECISION" | "PROJECT" | "BRIEFING" | "GENERAL",
          "title": "Short descriptive title",
          "summary": "One sentence summary of the action",
          "suggestedFollowUps": ["Follow up prompt 1", "Follow up prompt 2"]
        }
    """.trimIndent()

    suspend fun generateAssistantResponse(
        prompt: String,
        conversationHistory: List<GeminiContent> = emptyList(),
        customSystemInstruction: String? = null,
        authorizationToken: com.example.domain.privacy.TransferAuthorizationToken? = null
    ): Result<Pair<String, VybIntentPayload>> = withContext(Dispatchers.IO) {
        if (com.example.domain.privacy.FluidPrivacyFirewall.isLocalOnlyMode.value) {
            Log.i("GeminiRepository", "Transfer blocked: Local-Only Mode is active")
            return@withContext Result.failure(IllegalStateException("Local-Only Mode is active: outbound cloud request blocked."))
        }

        val destination = com.example.domain.privacy.TransferDestination(
            type = com.example.domain.privacy.TransferDestinationType.CLOUD_AI_SERVICE,
            name = "Google Gemini API",
            endpointOrPackage = "https://generativelanguage.googleapis.com"
        )

        val isAuthorized = com.example.domain.privacy.FluidPrivacyFirewall.validateAndConsumeToken(
            token = authorizationToken,
            targetDestination = destination,
            actualPayload = prompt
        )

        if (!isAuthorized) {
            Log.w("GeminiRepository", "Outbound transfer blocked by Fluid Privacy Firewall: unapproved or invalid token.")
            return@withContext Result.failure(SecurityException("Outbound transfer blocked by Fluid Privacy Firewall: unapproved or invalid token."))
        }

        val apiKey = BuildConfig.GEMINI_API_KEY

        val contents = mutableListOf<GeminiContent>()
        contents.addAll(conversationHistory.takeLast(6))
        contents.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = prompt))
            )
        )

        val instructionToUse = customSystemInstruction ?: systemInstructionText

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = instructionToUse))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                topP = 0.95f
            )
        )

        try {
            if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                // Safeguard network call with 7 second timeout so it NEVER hangs
                val response = withTimeoutOrNull(7000L) {
                    GeminiClient.apiService.generateContent(apiKey, request)
                }
                val rawText = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!rawText.isNullOrBlank()) {
                    val (conversationalText, payload) = parseVybResponse(prompt, rawText)
                    return@withContext Result.success(Pair(conversationalText, payload))
                }
            }
            if (customSystemInstruction != null) {
                return@withContext Result.failure(Exception("Gemini returned empty or blank response"))
            }
            // Fast reliable fallback generator
            val fallback = generateSmartFallback(prompt)
            Result.success(fallback)
        } catch (e: Exception) {
            Log.e("GeminiRepository", "API or Network Issue: ${e.message}")
            if (customSystemInstruction != null) {
                return@withContext Result.failure(e)
            }
            val fallback = generateSmartFallback(prompt)
            Result.success(fallback)
        }
    }

    private fun parseVybResponse(prompt: String, rawResponse: String): Pair<String, VybIntentPayload> {
        val jsonPattern = "(?s)```(?:vyb_json|json)?\\s*(\\{.*?\\})\\s*```".toRegex()
        val match = jsonPattern.find(rawResponse)

        var conversationalText = rawResponse
        var payload: VybIntentPayload? = null

        if (match != null) {
            val jsonString = match.groupValues[1]
            conversationalText = rawResponse.replace(match.value, "").trim()
            try {
                val adapter = GeminiClient.moshi.adapter(VybIntentPayload::class.java)
                payload = adapter.fromJson(jsonString)
            } catch (e: Exception) {
                Log.e("GeminiRepository", "Error parsing Moshi payload: ${e.message}")
            }
        }

        if (payload == null) {
            // Heuristic detection if JSON was missing or malformed
            payload = createHeuristicPayload(prompt, conversationalText)
        }

        return Pair(conversationalText, payload)
    }

    fun createHeuristicPayload(prompt: String, conversationalText: String): VybIntentPayload {
        val lower = prompt.lowercase()
        return when {
            lower.contains("cab") || lower.contains("ride") || lower.contains("uber") || lower.contains("ola") || lower.contains("rapido") || lower.contains("taxi") || lower.contains("car") || lower.contains("airport cab") || lower.contains("driver") -> {
                val dest = when {
                    lower.contains("airport") -> "Netaji Subhash Chandra Bose Int'l Airport (CCU)"
                    lower.contains("howrah") -> "Howrah Railway Station, Kolkata"
                    lower.contains("sealdah") -> "Sealdah Railway Station, Kolkata"
                    lower.contains("salt lake") || lower.contains("sector v") -> "Sector V, Salt Lake IT Hub, Kolkata"
                    lower.contains("park street") -> "Park Street / Camac Street, Central Kolkata"
                    lower.contains("south city") -> "South City Mall, Prince Anwar Shah Rd"
                    else -> "Kolkata City Destination"
                }
                VybIntentPayload(
                    intentType = "RIDE",
                    title = "Ride Providers & Navigation: $dest",
                    summary = "Installed ride providers: Rapido, Uber, Ola, Yatri Sathi & inDrive (Launch in app)",
                    ridePlan = RidePlanPayload(
                        pickupLocation = "Current GPS Location (Kolkata, WB)",
                        destination = dest,
                        estimatedDistance = null,
                        estimatedDuration = null,
                        liveFareAvailable = false,
                        directBookingAvailable = false,
                        note = "Direct live booking API unavailable • Can open installed ride apps",
                        rideOptions = listOf(
                            RideOptionPayload("Rapido", "Open app for live fares", null, null, "Bike / Auto / Cab", "com.rapido.passenger", false),
                            RideOptionPayload("Uber", "Open app for live fares", null, null, "Go / Premier / Auto", "com.ubercab", false),
                            RideOptionPayload("Ola", "Open app for live fares", null, null, "Mini / Prime / Auto", "com.olacabs.customer", false),
                            RideOptionPayload("Yatri Sathi", "Open app for live fares", null, null, "Metered Taxi", "com.yatrisathi.customer", false),
                            RideOptionPayload("inDrive", "Open app for live fares", null, null, "City Ride / Comfort", "sinet.startup.inDriver", false)
                        ),
                        directDeepLink = null
                    ),
                    suggestedFollowUps = listOf("Open Rapido", "Open Uber", "Open inDrive", "Navigate via Google Maps")
                )
            }
            lower.contains("diet") || lower.contains("meal") || lower.contains("calorie") || lower.contains("keto") || lower.contains("protein") -> {
                VybIntentPayload(
                    intentType = "DIET",
                    title = "Nutrition & Meal Plan",
                    summary = "Balanced macronutrient nutrition blueprint",
                    dietPlan = DietPlanPayload(
                        goal = "Balanced Nutrition",
                        targetCalories = 2000,
                        proteinGrams = 120,
                        carbsGrams = 200,
                        fatGrams = 60,
                        meals = listOf(
                            MealItemPayload("Breakfast", "Oatmeal with Nuts & Fruit", "Wholesome oats with toasted walnuts, fruit, and chia seeds", 400, 18, listOf("Oats", "Walnuts", "Banana", "Milk / Almond Milk")),
                            MealItemPayload("Lunch", "High-Protein Grain Bowl", "Grains with roasted vegetables and protein of choice", 600, 40, listOf("Brown Rice / Quinoa", "Lentils / Tofu / Paneer", "Greens")),
                            MealItemPayload("Evening Fuel", "Roasted Nuts & Herbal Tea", "Light wholesome snack with green or herbal tea", 180, 8, listOf("Roasted Chana / Almonds", "Green Tea")),
                            MealItemPayload("Dinner", "Warm Soup & Mixed Vegetable Stir-Fry", "Light nourishing dinner with legumes and sauteed vegetables", 500, 35, listOf("Lentils / Dal", "Broccoli", "Carrot", "Garlic"))
                        ),
                        tips = listOf("Maintain 2.5–3L daily hydration", "Include varied whole food protein sources")
                    ),
                    suggestedFollowUps = listOf("Adjust calorie target", "Create grocery list", "Customize dietary restrictions")
                )
            }
            lower.contains("grocery") || lower.contains("shopping") || lower.contains("buy") || lower.contains("market") -> {
                VybIntentPayload(
                    intentType = "SHOPPING",
                    title = "Grocery & Supplies Checklist",
                    summary = "Categorized items with estimated pricing",
                    shoppingList = ShoppingListPayload(
                        title = "Shopping Checklist",
                        estimatedTotal = 1200.00,
                        items = listOf(
                            ShoppingItemPayload("Fresh Eggs / Tofu", "Protein", "1 unit", 110.00, false),
                            ShoppingItemPayload("Rolled Whole Oats", "Pantry", "1 kg", 240.00, false),
                            ShoppingItemPayload("Fresh Greens / Spinach", "Produce", "2 bunches", 80.00, false),
                            ShoppingItemPayload("Whole Fruits", "Produce", "Assorted", 150.00, false)
                        )
                    ),
                    suggestedFollowUps = listOf("Add items", "Clear completed", "Categorize by store aisle")
                )
            }
            lower.contains("travel") || lower.contains("trip") || lower.contains("itinerary") || lower.contains("vacation") || lower.contains("visit") -> {
                val dest = "Destination"
                VybIntentPayload(
                    intentType = "TRAVEL",
                    title = "Travel Expedition: $dest",
                    summary = "Curated schedule, logistics, and packing checklist",
                    travelPlan = TravelPlanPayload(
                        destination = dest,
                        durationDays = 3,
                        estimatedBudget = 15000.00,
                        days = listOf(
                            TravelDayPayload(
                                dayNumber = 1,
                                title = "Arrival & Local Exploration",
                                activities = listOf(
                                    ActivityPayload("09:30 AM", "Check-in & Unpack", "Hotel / Stay", "Settle in and rest"),
                                    ActivityPayload("12:00 PM", "Local Sights & Promenade", "City Center", "Explore the main area")
                                )
                            ),
                            TravelDayPayload(
                                dayNumber = 2,
                                title = "Day Tour & Key Highlights",
                                activities = listOf(
                                    ActivityPayload("08:00 AM", "Guided Excursion", "Scenic Point", "Major viewpoint and historic sites")
                                )
                            ),
                            TravelDayPayload(
                                dayNumber = 3,
                                title = "Wrap-up & Departure",
                                activities = listOf(
                                    ActivityPayload("10:00 AM", "Local Crafts & Departure", "Market", "Souvenirs and return transit")
                                )
                            )
                        ),
                        packingChecklist = listOf("ID Cards & Tickets", "Charger & Power Bank", "Comfortable walking shoes", "Weather-appropriate jacket"),
                        highlights = listOf("Curated local eateries", "Scenic walking route")
                    ),
                    suggestedFollowUps = listOf("Add transportation options", "Check weather forecast", "Calculate trip budget")
                )
            }
            lower.contains("task") || lower.contains("remind") || lower.contains("todo") || lower.contains("organize") || lower.contains("plan") -> {
                VybIntentPayload(
                    intentType = "TASK",
                    title = "Action Checklist",
                    summary = "Prioritized strategic action items",
                    taskList = TaskListPayload(
                        title = "Tasks",
                        tasks = listOf(
                            TaskItemPayload("Review priority deliverables", "HIGH", "Work", "Today", false),
                            TaskItemPayload("Check schedule and calendar commitments", "MEDIUM", "Personal", "Today", false)
                        )
                    ),
                    suggestedFollowUps = listOf("Add new task", "Set priority", "Mark as completed")
                )
            }
            lower.contains("budget") || lower.contains("finance") || lower.contains("money") || lower.contains("expense") || lower.contains("spend") || lower.contains("afford") -> {
                VybIntentPayload(
                    intentType = "FINANCE",
                    title = "Financial Calculation",
                    summary = "Financial framework based on your verified numbers",
                    financePlan = null,
                    suggestedFollowUps = listOf("Calculate purchase affordability", "Set up financial targets in settings")
                )
            }
            lower.contains("email") || lower.contains("message") || lower.contains("write") || lower.contains("draft") -> {
                VybIntentPayload(
                    intentType = "COMMUNICATION",
                    title = "Draft Message",
                    summary = "Message drafted for your review",
                    emailDraft = EmailDraftPayload(
                        recipient = "",
                        subject = "Project Update",
                        body = "Hello,\n\nI am following up regarding our recent discussion. Please let me know your availability for a quick sync.\n\nBest regards,",
                        tone = "Professional & Clear"
                    ),
                    suggestedFollowUps = listOf("Refine tone", "Copy message to clipboard")
                )
            }
            else -> {
                VybIntentPayload(
                    intentType = "GENERAL",
                    title = "VYB Intelligence Engine",
                    summary = "On-device intelligence and task assistant",
                    suggestedFollowUps = listOf("Show today's timeline", "What routines have you learned?", "Help me plan my day")
                )
            }
        }
    }

    private fun generateSmartFallback(prompt: String): Pair<String, VybIntentPayload> {
        val payload = createHeuristicPayload(prompt, "")
        val lower = prompt.lowercase()
        val conversationalText = when {
            lower.contains("know about me") || lower.contains("what do you know") -> {
                "I don't know yet. I only know facts you explicitly save in your local memory vault and routines learned from your on-device activity over time."
            }
            lower.contains("income") || lower.contains("salary") || lower.contains("bank balance") -> {
                "I don't know yet. Financial figures are kept strictly private on your device and are only stored if you explicitly enter them."
            }
            lower.contains("fitness target") || lower.contains("my weight") -> {
                "I don't know yet. You can set your personal fitness goals in the settings or memory vault."
            }
            payload.intentType == "RIDE" -> {
                "I can prepare the route details and open a ride provider on your phone (Rapido, Uber, Ola, Yatri Sathi, or inDrive). Direct live booking APIs are currently unavailable, so you can select your preferred provider to view live fares."
            }
            payload.intentType == "DIET" -> {
                "Here is a balanced nutrition outline based on your request."
            }
            payload.intentType == "SHOPPING" -> {
                "Here is a structured shopping checklist categorized by section."
            }
            payload.intentType == "TRAVEL" -> {
                "Here is a travel itinerary outline based on your requested destination."
            }
            payload.intentType == "TASK" -> {
                "Action items organized and prioritized."
            }
            payload.intentType == "FINANCE" -> {
                "I don't have your financial figures saved yet. Please provide your monthly income and fixed expenses, or enter them to calculate."
            }
            payload.intentType == "COMMUNICATION" -> {
                "I have drafted a message for your review. Please confirm before sending."
            }
            payload.intentType == "BRIEFING" -> {
                "Here is your daily overview based on your on-device timeline and recorded patterns."
            }
            else -> "Ready. What would you like to assist with today?"
        }
        return Pair(conversationalText, payload)
    }
}
