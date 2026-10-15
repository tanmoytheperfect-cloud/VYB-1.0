package com.example.domain.intelligence

import com.example.domain.permission.VybPermissionType
import java.util.Locale
import java.util.UUID

/**
 * Universal Intent Engine (Part 8, 9, 41, 45).
 * Interprets user natural language semantically, never naively keyword-matching.
 * Distinguishes Personal Knowledge from Schedule, App Launch from Action Completion,
 * and maintains active conversational continuity.
 */
class UniversalIntentEngine {

    fun parseIntent(input: String, activeState: ActiveConversationState? = null): UniversalIntent {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)
            .replace(Regex("[?!.,]"), "")
            .trim()

        // 1. Task Correction (Part 20 & 45)
        // "you only opened the app", "you didn't set the destination", "that's incomplete", "that didn't work"
        if (lower.contains("you only opened") || lower.contains("only opened the app") ||
            lower.contains("didn't set the destination") || lower.contains("did not set the destination") ||
            lower.contains("you only opened it") || lower.contains("thats incomplete") ||
            lower.contains("that's incomplete") || lower.contains("that didn't work") ||
            lower.contains("that didnt work") || lower.contains("thats not what i asked") ||
            lower.contains("you missed that") || lower.contains("not set")) {

            val activeTask = activeState?.activeTaskState
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.ACTION_CORRECTION,
                entities = mapOf(
                    "correction" to trimmed,
                    "targetTaskId" to (activeTask?.taskId ?: "active_task"),
                    "service" to (activeTask?.service ?: "external_app")
                ),
                requiredCapabilities = listOf("TRANSPORT", "DEVICE_CONTROL"),
                riskLevel = RiskLevel.MEDIUM,
                confirmationRequired = false
            )
        }

        // 2. Task Continuation (Part 20 & 45)
        // "continue", "finish it", "do it again", "keep going"
        if (lower == "continue" || lower == "finish it" || lower == "do it again" ||
            lower == "keep going" || lower == "proceed" || lower == "finish the task") {

            val activeTask = activeState?.activeTaskState
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.ACTION_CONTINUATION,
                entities = mapOf(
                    "targetTaskId" to (activeTask?.taskId ?: "active_task"),
                    "service" to (activeTask?.service ?: "external_app")
                ),
                requiredCapabilities = listOf("TRANSPORT", "DEVICE_CONTROL"),
                riskLevel = RiskLevel.MEDIUM,
                confirmationRequired = false
            )
        }

        // 3. Conversational Follow-up Turns (Part 10)
        if (lower == "what about my work" || lower == "and my work" || lower == "what about work") {
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.PROJECT_QUERY,
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }
        if (lower == "and my family" || lower == "what about my family" || lower == "what about family") {
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.FAMILY_QUERY,
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }
        if (lower == "what about my goals" || lower == "and my goals" || lower == "what about goals") {
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.GOAL_QUERY,
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 4. Personal Knowledge & Identity (Part 8, 11, 45)
        // "what do you know about me?", "tell me about myself", "what have you learned about me?", "who am I to you?"
        if (lower == "what do you know about me" || lower == "what you know about me" ||
            lower == "tell me about myself" || lower == "what have you learned about me" ||
            lower == "what information do you have about me" || lower == "who am i to you" ||
            lower == "who am i" || lower == "tell me who i am" || lower == "what do you know about me") {

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.PERSONAL_KNOWLEDGE_QUERY,
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW,
                confirmationRequired = false
            )
        }

        // 5. Memory Write & Deletion (Part 12 & 45)
        // "remember that I prefer concise answers", "forget that preference"
        if (lower.startsWith("forget that") || lower.startsWith("delete that memory") || lower.startsWith("forget memory")) {
            val key = trimmed.replaceFirst("(?i)^(forget\\s*that|delete\\s*that\\s*memory|forget\\s*memory)\\s*".toRegex(), "").trim()
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.MEMORY_DELETE,
                entities = mapOf("target" to key),
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }

        if (lower.startsWith("remember that") || lower.startsWith("note that") || lower.startsWith("save to memory") || lower.startsWith("keep in mind that")) {
            val content = trimmed.replaceFirst("(?i)^(remember\\s*that|note\\s*that|save\\s*to\\s*memory|keep\\s*in\\s*mind\\s*that)\\s*".toRegex(), "").trim()
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.MEMORY_WRITE,
                entities = mapOf("content" to content, "key" to content.take(30)),
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 6a. Conversation Context Recency ("what did I just tell you?", "what did I say earlier?")
        if (lower.contains("just tell") || lower.contains("last tell") || lower.contains("tell you earlier") ||
            lower.startsWith("what did i just tell you") || lower.startsWith("what did i tell you") ||
            lower.startsWith("what did i say") || lower.contains("did i just say") || lower.contains("what was the last thing")
        ) {
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.MEMORY_QUERY,
                entities = mapOf(
                    "query" to "recent_conversation",
                    "scope" to "CONVERSATION_CONTEXT"
                ),
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 6b. Memory Query / Personal Recall ("what else do you remember?", "what do you remember?", "where is my office?", "do you remember...", "what do you recall?", etc.)
        if (lower.contains("remember") || lower.contains("recall") ||
            lower == "what else do you know" || lower == "what do you know" ||
            lower.contains("what else do you remember") || lower.contains("do you remember") ||
            lower.contains("what do you remember") || lower.contains("anything else you remember") ||
            lower.contains("tell me what you remember") || lower.contains("what have you remembered") ||
            lower.startsWith("what do you recall") || lower.startsWith("what is my") ||
            lower.startsWith("where is my") || lower.startsWith("where do i") || lower.startsWith("where is") ||
            lower.startsWith("what time does") || lower.startsWith("when does") ||
            (lower.contains("school") && (lower.contains("time") || lower.contains("start")))
        ) {
            val isGeneralRecall = lower == "what else do you remember" || lower == "what do you remember" ||
                lower == "what do you remember about me" || lower.contains("what else do you remember") ||
                lower.contains("do you remember anything else") || lower == "tell me what you remember" ||
                lower == "what have you remembered" || lower == "what do you recall" ||
                lower == "what else do you know" || lower == "what do you know" || lower == "what do you recall"

            val query = if (isGeneralRecall) "ALL_CATEGORIES" else trimmed

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.MEMORY_QUERY,
                entities = mapOf(
                    "query" to query,
                    "isGeneralRecall" to isGeneralRecall.toString()
                ),
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 7. Goals Query (Part 45)
        // "what are my goals?", "show my goals"
        if (lower.contains("what are my goals") || lower.contains("my goals") || lower == "what goals do i have") {
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.GOAL_QUERY,
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 8. Projects & Work Query (Part 45)
        // "what am I working on?", "what are my projects?"
        if (lower.contains("what am i working on") || lower.contains("what are my projects") ||
            lower.contains("my active projects") || lower.contains("my projects") || lower == "what is my work") {

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.PROJECT_QUERY,
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 9. Family Query (Part 45)
        // "tell me about my family", "who is in my family?"
        if (lower.contains("about my family") || lower.contains("who is in my family") ||
            lower.contains("my family members") || lower == "my family") {

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.FAMILY_QUERY,
                requiredCapabilities = listOf("MEMORY"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 10. Routines Query (Part 45)
        // "what are my routines?", "show my routines"
        if (lower.contains("what are my routines") || lower.contains("show my routines") ||
            lower.contains("my daily routines") || lower.contains("what routines do i have") || lower == "my routines") {

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.ROUTINE_QUERY,
                requiredCapabilities = listOf("AUTOMATION"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 11. Calendar & Schedule Query (Part 45)
        // "what is on my calendar today?", "what do I have today?", "calendar today"
        if (lower.contains("calendar today") || lower.contains("on my calendar today") ||
            lower.contains("what do i have today") || lower.contains("what is on my calendar") ||
            lower == "my schedule today" || lower == "what is my schedule" || lower == "agenda today") {

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.CALENDAR_QUERY,
                entities = mapOf("date" to "today"),
                requiredCapabilities = listOf("CALENDAR"),
                requiredPermissions = listOf(VybPermissionType.CALENDAR),
                riskLevel = RiskLevel.LOW
            )
        }

        // 12. Device Telemetry & Permissions (Part 29 & 45)
        // "what is my battery?", "is phone charging?", "what permissions do you have?", "what can you actually do on this phone?"
        if (lower.contains("battery") || lower.contains("is phone charging") || lower.contains("charging status") ||
            lower == "what is my battery" || lower == "battery level") {

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.DEVICE_QUERY,
                entities = mapOf("metric" to "BATTERY"),
                requiredCapabilities = listOf("DEVICE_CONTROL"),
                riskLevel = RiskLevel.LOW
            )
        }

        if (lower.contains("what permissions do you have") || lower.contains("permissions do you have") ||
            lower.contains("check permissions") || lower.contains("show permissions") ||
            lower.contains("what can you actually do on this phone") || lower.contains("what can you do on this phone")) {

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.PERMISSION_QUERY,
                requiredCapabilities = listOf("DEVICE_CONTROL"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 13. App Launch (Pure launcher without automation task) (Part 45)
        // "open Rapido", "open Uber", "open WhatsApp", "open Chrome"
        if (lower == "open rapido" || lower == "open uber" || lower == "open whatsapp" ||
            lower == "open maps" || lower == "open chrome" || lower == "open youtube") {

            val app = trimmed.replaceFirst("(?i)^open\\s*".toRegex(), "").trim()
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.APP_LAUNCH,
                entities = mapOf("app_name" to app),
                requiredCapabilities = listOf("EXTERNAL_APP_ACTION"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 14. Transport / Ride Request with destination (Part 23 & 45)
        // "open Rapido and set my office as destination", "book my usual cab to office", "book cab to work", "take me to my office"
        if (lower.contains("and set") && (lower.contains("destination") || lower.contains("office") || lower.contains("home")) ||
            lower.startsWith("take me to") || lower.contains("usual cab") ||
            (lower.startsWith("book") && (lower.contains("cab") || lower.contains("ride") || lower.contains("uber") || lower.contains("ola") || lower.contains("rapido")))) {

            val service = when {
                lower.contains("rapido") -> "Rapido"
                lower.contains("uber") -> "Uber"
                lower.contains("ola") -> "Ola"
                lower.contains("yatri") -> "Yatri Sathi"
                else -> null
            }

            val destination = when {
                lower.contains("office") || lower.contains("work") -> "Office"
                lower.contains("home") -> "Home"
                lower.contains("airport") -> "Airport"
                lower.contains("school") -> "School"
                lower.contains("destination to ") -> trimmed.substringAfter("destination to ").trim()
                lower.contains("set ") && lower.contains(" as destination") -> trimmed.substringAfter("set ").substringBefore(" as destination").trim()
                lower.contains("to ") -> trimmed.substringAfter("to ").substringBefore(" by").substringBefore(" in").trim()
                else -> null
            }

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.BOOK_TRANSPORT,
                entities = mapOf(
                    "service" to (service ?: "Preferred Provider"),
                    "destination" to (destination ?: "Office")
                ),
                requiredCapabilities = listOf("LOCATION", "TRANSPORT"),
                requiredPermissions = listOf(VybPermissionType.LOCATION),
                riskLevel = RiskLevel.HIGH,
                confirmationRequired = true
            )
        }

        // 15. Reminder / Alarm Request (Part 45)
        // "remind me tomorrow to call Rahul", "set alarm for 7am"
        if (lower.startsWith("remind me") || lower.startsWith("create reminder") || lower.startsWith("set alarm")) {
            val task = trimmed
                .replaceFirst("(?i)^(remind\\s*me\\s*(to)?|create\\s*reminder\\s*(for)?|set\\s*alarm\\s*(for)?)\\s*".toRegex(), "")
                .trim()

            val time = when {
                lower.contains("tomorrow") -> "Tomorrow"
                lower.contains("tonight") -> "Tonight"
                else -> "Today"
            }

            val entities = mutableMapOf<String, Any?>("task" to task, "time" to time)
            if (task.contains("call ", ignoreCase = true)) {
                val name = task.substringAfter("call ").trim().split(" ").firstOrNull()
                name?.let { entities["contact_name"] = it }
            }

            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.CREATE_REMINDER,
                entities = entities,
                requiredCapabilities = listOf("REMINDER"),
                requiredPermissions = listOf(VybPermissionType.ALARM),
                riskLevel = RiskLevel.LOW
            )
        }

        // 16. Communication Dispatch
        // "call Rahul", "send WhatsApp to Sarah", "send a message to Rahul"
        if (lower.startsWith("call ") || lower.startsWith("send whatsapp") || lower.startsWith("whatsapp ") ||
            lower.startsWith("send message") || lower.startsWith("send a message") || lower.startsWith("message ")) {
            val target = when {
                lower.startsWith("call ") -> trimmed.substringAfter("call ").trim()
                lower.startsWith("send a message to ") -> trimmed.substringAfter("send a message to ").substringBefore(" regarding").substringBefore(" that").substringBefore(" saying").trim()
                lower.startsWith("send message to ") -> trimmed.substringAfter("send message to ").substringBefore(" regarding").substringBefore(" that").substringBefore(" saying").trim()
                lower.startsWith("send whatsapp to ") -> trimmed.substringAfter("send whatsapp to ").substringBefore(" that").substringBefore(" saying").trim()
                lower.startsWith("whatsapp ") -> trimmed.substringAfter("whatsapp ").substringBefore(" that").substringBefore(" saying").trim()
                lower.startsWith("message ") -> trimmed.substringAfter("message ").substringBefore(" regarding").substringBefore(" that").substringBefore(" saying").trim()
                else -> "Contact"
            }
            val msg = when {
                lower.contains("regarding ") -> trimmed.substringAfter("regarding ").trim()
                lower.contains("saying ") -> trimmed.substringAfter("saying ").trim()
                lower.contains("that ") -> trimmed.substringAfter("that ").trim()
                else -> "Hello via VYB"
            }
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.MESSAGE_REQUEST,
                entities = mapOf("contact_name" to target, "message" to msg),
                requiredCapabilities = listOf("MESSAGING", "CONTACTS"),
                requiredPermissions = listOf(VybPermissionType.CONTACTS),
                riskLevel = RiskLevel.HIGH,
                confirmationRequired = true
            )
        }

        // 17. Finance Query
        if (lower.contains("afford") || lower.contains("budget") || lower.contains("expenses increase") || lower.contains("compare these expenses") || lower.contains("how much did i spend")) {
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.FINANCE_ANALYSIS,
                entities = mapOf("query" to trimmed),
                requiredCapabilities = listOf("FINANCE_ANALYSIS"),
                riskLevel = RiskLevel.MEDIUM
            )
        }

        // 18. Shopping Query
        if (lower.startsWith("buy ") || lower.startsWith("order ") || lower.contains("make a shopping list") || lower.startsWith("what should i buy")) {
            val item = trimmed.replaceFirst("(?i)^(buy|order|what\\s*should\\s*i\\s*buy\\s*for|make\\s*a\\s*shopping\\s*list\\s*for)\\s*".toRegex(), "").trim()
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.SHOPPING_QUERY,
                entities = mapOf("item" to item),
                requiredCapabilities = listOf("SHOPPING"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 19. Research Query
        if (lower.startsWith("research ") || lower.startsWith("deep dive on") || lower.startsWith("investigate ")) {
            val topic = trimmed.replaceFirst("(?i)^(research|deep\\s*dive\\s*on|investigate)\\s*".toRegex(), "").trim()
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.RESEARCH_QUERY,
                entities = mapOf("topic" to topic),
                requiredCapabilities = listOf("WEB_RESEARCH"),
                riskLevel = RiskLevel.LOW
            )
        }

        // 20. Ambiguous / Unknown Intent (Part 42 & 45)
        // "do something", "test", single words that cannot be resolved safely
        if (lower == "do something" || lower == "something" || lower == "action" || lower.length < 3) {
            return UniversalIntent(
                intentId = UUID.randomUUID().toString(),
                rawInput = trimmed,
                intentType = UniversalIntentType.UNKNOWN_INTENT,
                clarificationQuestion = "I'm not completely sure what you mean. Do you want:\n1. What I remember about you\n2. Your schedule today\n3. Your routines\n4. Something else?",
                riskLevel = RiskLevel.LOW
            )
        }

        // 21. General Conversation
        return UniversalIntent(
            intentId = UUID.randomUUID().toString(),
            rawInput = trimmed,
            intentType = UniversalIntentType.GENERAL_CONVERSATION,
            entities = mapOf("prompt" to trimmed),
            requiredCapabilities = listOf("CHAT"),
            riskLevel = RiskLevel.LOW
        )
    }
}
