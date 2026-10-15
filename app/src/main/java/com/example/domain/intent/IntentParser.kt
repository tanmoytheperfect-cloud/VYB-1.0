package com.example.domain.intent

import java.util.Locale

/**
 * Natural language parser converting user input into StructuredIntent objects,
 * identifying missing information and mapping to real device actions.
 */
object IntentParser {

    fun parse(input: String): StructuredIntent {
        val trimmed = input.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // 0. Check for Personal Knowledge Query ("what do you know about me", "tell me about myself", etc.)
        if (lower.contains("about me") || lower.contains("about myself") || lower.contains("who am i") ||
            lower == "what do you know" || lower.startsWith("what do you know about me")) {
            return StructuredIntent(
                intent = StructuredIntent.INTENT_RETRIEVE_MEMORY,
                entities = mapOf("query" to "personal_knowledge", "category" to "ALL"),
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 0b. Check for What Matters Today / Priorities / Daily Reasoning Intent
        if (lower.contains("what matters today") || lower.contains("what matters now") || lower.contains("what matters") ||
            lower.contains("what should i do") || lower.contains("what do you know about my day") ||
            lower.contains("what is my next important thing") || lower.contains("next important thing") ||
            lower.contains("what's my next") || lower.contains("what are my priorities") ||
            lower.contains("priorities today") || lower.contains("plan for today") || lower.contains("daily briefing") ||
            lower.contains("good morning") || lower.contains("morning briefing") || lower.contains("what do i have")) {
            return StructuredIntent(
                intent = StructuredIntent.INTENT_WHAT_MATTERS_TODAY,
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 1. Check for Memory Storage Intent ("Remember that...", "Note that...", "Save fact...")
        if (lower.startsWith("remember") || lower.startsWith("note that") || lower.startsWith("save to memory") || lower.startsWith("keep in mind")) {
            val content = trimmed
                .replaceFirst("(?i)^remember\\s*(that\\s*)?".toRegex(), "")
                .replaceFirst("(?i)^note\\s*(that\\s*)?".toRegex(), "")
                .replaceFirst("(?i)^save\\s*(to\\s*memory\\s*)?".toRegex(), "")
                .replaceFirst("(?i)^keep\\s*in\\s*mind\\s*(that\\s*)?".toRegex(), "")
                .trim()

            if (content.isBlank()) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_SAVE_MEMORY,
                    entities = emptyMap(),
                    rawPrompt = trimmed,
                    missingParameters = listOf("memory_content"),
                    clarificationQuestion = "What would you like me to remember?"
                )
            }

            val category = when {
                lower.contains("school") || lower.contains("son") || lower.contains("family") || lower.contains("wife") -> "FAMILY"
                lower.contains("budget") || lower.contains("salary") || lower.contains("inr") || lower.contains("cost") || lower.contains("cheap") -> "FINANCIAL"
                lower.contains("diet") || lower.contains("protein") || lower.contains("calorie") || lower.contains("food") -> "HEALTH"
                lower.contains("prefer") || lower.contains("like") || lower.contains("always") -> "PREFERENCES"
                else -> "FACTS"
            }

            val key = when {
                lower.contains("school") -> "School Schedule"
                lower.contains("office") || lower.contains("work") -> "Workplace Information"
                lower.contains("cab") || lower.contains("transport") -> "Transport Preference"
                else -> "Note: ${content.take(30)}"
            }

            return StructuredIntent(
                intent = StructuredIntent.INTENT_SAVE_MEMORY,
                entities = mapOf(
                    "key" to key,
                    "value" to content,
                    "category" to category
                ),
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 2. Check for Memory Retrieval Intent ("What did I just tell you?", "What else do you remember?", "What time does my son's school start?", "Where do I work?", "What do you remember...")
        if (lower.contains("just tell") || lower.contains("last tell") || lower.contains("tell you") ||
            lower.contains("remember") || lower.contains("recall") ||
            lower.startsWith("what time does") || lower.startsWith("when does") ||
            lower.startsWith("what is my") || lower.startsWith("what did i say") ||
            lower.contains("what do you know") || lower.contains("tell me what you know") ||
            (lower.contains("school") && (lower.contains("time") || lower.contains("start")))) {

            val isGeneralRecall = lower == "what else do you remember" || lower == "what do you remember" ||
                    lower == "what do you remember about me" || lower.contains("what else do you remember") ||
                    lower.contains("do you remember anything else") || lower == "tell me what you remember" ||
                    lower == "what have you remembered" || lower == "what do you recall" ||
                    lower == "what else do you know" || lower == "what do you know"

            val query = when {
                lower.contains("just tell") || lower.contains("last tell") || lower.contains("tell you") -> "recent"
                isGeneralRecall -> "ALL_CATEGORIES"
                lower.contains("school") -> "school"
                lower.contains("son") -> "son"
                lower.contains("work") || lower.contains("office") -> "office"
                lower.contains("budget") || lower.contains("savings") -> "savings"
                else -> trimmed.replaceFirst("(?i)^what\\s*(is|time|do|did|else)\\s*".toRegex(), "").trim()
            }

            return StructuredIntent(
                intent = StructuredIntent.INTENT_RETRIEVE_MEMORY,
                entities = mapOf(
                    "query" to query,
                    "rawQuery" to trimmed,
                    "isGeneralRecall" to isGeneralRecall.toString()
                ),
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 3. Check for Reminder / Alarm Creation ("Remind me to...", "Set alarm for...", "Create reminder...")
        if (lower.startsWith("remind me to") || lower.startsWith("create reminder") || lower.startsWith("remind me") || lower.startsWith("set a reminder") || lower.startsWith("set reminder")) {
            val reminderBody = trimmed
                .replaceFirst("(?i)^remind\\s*me\\s*to\\s*".toRegex(), "")
                .replaceFirst("(?i)^create\\s*reminder\\s*(for|to)?\\s*".toRegex(), "")
                .replaceFirst("(?i)^set\\s*a?\\s*reminder\\s*(for|to)?\\s*".toRegex(), "")
                .replaceFirst("(?i)^remind\\s*me\\s*".toRegex(), "")
                .trim()

            if (reminderBody.isBlank() || reminderBody.equals("tomorrow", ignoreCase = true) || reminderBody.equals("today", ignoreCase = true)) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_CREATE_REMINDER,
                    entities = emptyMap(),
                    rawPrompt = trimmed,
                    missingParameters = listOf("title"),
                    clarificationQuestion = if (reminderBody.isNotBlank()) "What would you like me to remind you about $reminderBody?" else "What would you like me to remind you about, and at what time?"
                )
            }

            // Extract time (e.g. "at 6 PM", "at 8:30 AM", "in 30 minutes")
            val timeMatch = "(?i)\\bat\\s*(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)\\b".toRegex().find(reminderBody)
            val parsedTime = timeMatch?.groupValues?.get(1)?.trim()
            val cleanTitle = if (timeMatch != null) {
                reminderBody.removeRange(timeMatch.range).trim()
            } else {
                reminderBody
            }

            val hourMinute = parseHourMinute(parsedTime ?: "")

            if (cleanTitle.isBlank() || cleanTitle.equals("tomorrow", ignoreCase = true) || cleanTitle.equals("today", ignoreCase = true)) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_CREATE_REMINDER,
                    entities = mapOf("time" to (parsedTime ?: "")),
                    rawPrompt = trimmed,
                    missingParameters = listOf("title"),
                    clarificationQuestion = "What would you like me to remind you about${if (parsedTime != null) " at $parsedTime" else ""}?"
                )
            }

            return if (parsedTime == null && !lower.contains("today") && !lower.contains("tomorrow")) {
                StructuredIntent(
                    intent = StructuredIntent.INTENT_CREATE_REMINDER,
                    entities = mapOf("title" to cleanTitle),
                    rawPrompt = trimmed,
                    missingParameters = listOf("time"),
                    clarificationQuestion = "What time should I set the reminder for \"$cleanTitle\"?"
                )
            } else {
                StructuredIntent(
                    intent = StructuredIntent.INTENT_CREATE_REMINDER,
                    entities = mapOf(
                        "title" to cleanTitle,
                        "time" to (parsedTime ?: "6:00 PM"),
                        "hour" to (hourMinute?.first ?: 18),
                        "minute" to (hourMinute?.second ?: 0)
                    ),
                    rawPrompt = trimmed,
                    requiresAuthorization = false
                )
            }
        }

        // 4. Check for Alarm ("Set an alarm for 7 AM", "Alarm at 8:30")
        if (lower.startsWith("set alarm") || lower.startsWith("set an alarm") || lower.startsWith("wake me up at") || lower.startsWith("wake me up")) {
            val timeMatch = "(?i)(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)".toRegex().find(trimmed)
            val timeStr = timeMatch?.groupValues?.get(1)

            if (timeStr == null) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_SET_ALARM,
                    entities = emptyMap(),
                    rawPrompt = trimmed,
                    missingParameters = listOf("time"),
                    clarificationQuestion = "What time should I set the alarm for?"
                )
            }

            val hourMinute = parseHourMinute(timeStr)
            return StructuredIntent(
                intent = StructuredIntent.INTENT_SET_ALARM,
                entities = mapOf(
                    "time" to timeStr,
                    "hour" to (hourMinute?.first ?: 7),
                    "minute" to (hourMinute?.second ?: 0)
                ),
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 5. Check for Navigation / Maps Intent ("Open Google Maps", "Navigate to...", "Directions to...")
        if (lower.startsWith("navigate to") || lower.startsWith("take me to") || lower.startsWith("directions to")) {
            val destination = trimmed
                .replaceFirst("(?i)^navigate\\s*to\\s*".toRegex(), "")
                .replaceFirst("(?i)^take\\s*me\\s*to\\s*".toRegex(), "")
                .replaceFirst("(?i)^directions\\s*to\\s*".toRegex(), "")
                .trim()

            if (destination.isBlank()) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_NAVIGATE,
                    entities = emptyMap(),
                    rawPrompt = trimmed,
                    missingParameters = listOf("destination"),
                    clarificationQuestion = "Where would you like to navigate to?"
                )
            }

            return StructuredIntent(
                intent = StructuredIntent.INTENT_NAVIGATE,
                entities = mapOf("destination" to destination),
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        if (lower == "open google maps" || lower == "open maps" || lower.startsWith("open maps for") || lower == "show map" || lower == "maps") {
            val query = trimmed.replaceFirst("(?i)^open\\s*(google\\s*)?maps(\\s*for)?\\s*".toRegex(), "").trim()
            return StructuredIntent(
                intent = StructuredIntent.INTENT_OPEN_MAPS,
                entities = mapOf("query" to query.ifBlank { "Kolkata" }),
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 6. Check for URL Opening
        if (lower.startsWith("open http://") || lower.startsWith("open https://") || lower.startsWith("go to http") || lower.startsWith("visit http") || (lower.startsWith("open ") && (lower.endsWith(".com") || lower.endsWith(".org") || lower.endsWith(".io") || lower.endsWith(".in")))) {
            val url = trimmed.replaceFirst("(?i)^(open|go\\s*to|visit)\\s+".toRegex(), "").trim()
            if (url.isBlank()) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_OPEN_URL,
                    entities = emptyMap(),
                    rawPrompt = trimmed,
                    missingParameters = listOf("url"),
                    clarificationQuestion = "Which website URL would you like me to open?"
                )
            }
            return StructuredIntent(
                intent = StructuredIntent.INTENT_OPEN_URL,
                entities = mapOf("url" to url),
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 7. Check for Application Launch ("Open [App Name]", "Launch [App Name]")
        if (lower.startsWith("open ") || lower.startsWith("launch ")) {
            val appName = trimmed.replaceFirst("(?i)^(open|launch)\\s+".toRegex(), "").trim()
            if (appName.isBlank()) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_OPEN_APP,
                    entities = emptyMap(),
                    rawPrompt = trimmed,
                    missingParameters = listOf("app_name"),
                    clarificationQuestion = "Which application would you like me to open?"
                )
            }
            return StructuredIntent(
                intent = StructuredIntent.INTENT_OPEN_APP,
                entities = mapOf("app_name" to appName),
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 8. Check for Calendar Event ("Create calendar event...", "Schedule meeting...")
        if (lower.startsWith("create calendar event") || lower.startsWith("schedule event") || lower.startsWith("add to calendar") || lower.startsWith("create event")) {
            val eventDetails = trimmed
                .replaceFirst("(?i)^(create\\s*calendar\\s*event|schedule\\s*event|add\\s*to\\s*calendar|create\\s*event)\\s*(for|to)?\\s*".toRegex(), "")
                .trim()

            if (eventDetails.isBlank()) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_CREATE_CALENDAR_EVENT,
                    entities = emptyMap(),
                    rawPrompt = trimmed,
                    missingParameters = listOf("title"),
                    clarificationQuestion = "What is the title and time of the event you would like to schedule?"
                )
            }

            return StructuredIntent(
                intent = StructuredIntent.INTENT_CREATE_CALENDAR_EVENT,
                entities = mapOf(
                    "title" to eventDetails,
                    "startTime" to "Scheduled time"
                ),
                rawPrompt = trimmed,
                requiresAuthorization = true
            )
        }

        // 9. Check for Phone Call ("Call [Person]", "Dial [Number]")
        if (lower.startsWith("call ") || lower.startsWith("dial ")) {
            val target = trimmed.replaceFirst("(?i)^(call|dial)\\s+".toRegex(), "").trim()
            if (target.isBlank()) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_MAKE_CALL,
                    entities = emptyMap(),
                    rawPrompt = trimmed,
                    missingParameters = listOf("target"),
                    clarificationQuestion = "Who or which number would you like me to call?"
                )
            }

            val digitsOnly = target.filter { it.isDigit() || it == '+' }
            // If it's a contact name without phone digits, mark phone_number as missing so we ask for clarification
            if (digitsOnly.length < 3) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_MAKE_CALL,
                    entities = mapOf("target" to target),
                    rawPrompt = trimmed,
                    missingParameters = listOf("phone_number"),
                    clarificationQuestion = "I don't have a stored phone number for $target. What phone number would you like me to call?"
                )
            }

            return StructuredIntent(
                intent = StructuredIntent.INTENT_MAKE_CALL,
                entities = mapOf("target" to target),
                rawPrompt = trimmed,
                requiresAuthorization = true
            )
        }

        // 10. Check for SMS ("Send SMS to...", "Text [Person]...")
        if (lower.startsWith("send sms to") || lower.startsWith("send sms") || lower.startsWith("text ")) {
            val clean = trimmed.replaceFirst("(?i)^(send\\s*sms\\s*to|send\\s*sms|text)\\s*".toRegex(), "").trim()
            val parts = clean.split(" saying ", " that ", " message: ", ": ", limit = 2)
            val recipient = parts.getOrNull(0)?.trim() ?: ""
            val message = parts.getOrNull(1)?.trim() ?: ""

            if (recipient.isBlank()) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_SEND_SMS,
                    entities = emptyMap(),
                    rawPrompt = trimmed,
                    missingParameters = listOf("recipient", "message"),
                    clarificationQuestion = "Who would you like to text, and what is your message?"
                )
            }

            val digitsOnly = recipient.filter { it.isDigit() || it == '+' }
            if (digitsOnly.length < 3) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_SEND_SMS,
                    entities = mapOf("recipient" to recipient, "message" to message),
                    rawPrompt = trimmed,
                    missingParameters = if (message.isBlank()) listOf("phone_number", "message") else listOf("phone_number"),
                    clarificationQuestion = "I don't have a phone number for $recipient. What phone number should I send this SMS to?"
                )
            }

            if (message.isBlank()) {
                return StructuredIntent(
                    intent = StructuredIntent.INTENT_SEND_SMS,
                    entities = mapOf("recipient" to recipient),
                    rawPrompt = trimmed,
                    missingParameters = listOf("message"),
                    clarificationQuestion = "What message would you like me to send to $recipient?"
                )
            }

            return StructuredIntent(
                intent = StructuredIntent.INTENT_SEND_SMS,
                entities = mapOf("recipient" to recipient, "message" to message),
                rawPrompt = trimmed,
                requiresAuthorization = true
            )
        }

        // 11. Check for WhatsApp Message ("Send WhatsApp to...", "WhatsApp message...")
        if (lower.contains("whatsapp")) {
            val clean = trimmed
                .replaceFirst("(?i)^send\\s*(a\\s*)?whatsapp(\\s*message)?\\s*(to)?\\s*".toRegex(), "")
                .replaceFirst("(?i)^whatsapp\\s*(to)?\\s*".toRegex(), "")
                .trim()

            return StructuredIntent(
                intent = StructuredIntent.INTENT_OPEN_WHATSAPP,
                entities = mapOf("message" to clean.ifBlank { "Hello" }),
                rawPrompt = trimmed,
                requiresAuthorization = true
            )
        }

        // 12. Check for Transport / Cab ("Book a cab...", "Book ride...")
        if (lower.contains("cab") || lower.contains("ride") || lower.contains("taxi")) {
            val destination = if (lower.contains("airport")) {
                "Airport"
            } else {
                "Destination"
            }
            val passenger = if (lower.contains("family") || lower.contains("son") || lower.contains("wife")) "Family" else "Self"
            return StructuredIntent(
                intent = StructuredIntent.INTENT_BOOK_TRANSPORT,
                entities = mapOf(
                    "destination" to destination,
                    "passenger" to passenger
                ),
                rawPrompt = trimmed,
                requiresAuthorization = true
            )
        }

        // 13. Check for Affordability ("Can I afford...", "Should I buy...")
        if (lower.contains("can i afford") || lower.contains("should i buy") || lower.contains("in budget")) {
            return StructuredIntent(
                intent = StructuredIntent.INTENT_CHECK_AFFORDABILITY,
                entities = mapOf("query" to trimmed),
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 14. Check for Home Screen Update / Customization
        if (lower.contains("update home screen") || lower.contains("change home screen") || lower.contains("change vyb's home screen") || lower.contains("refresh home screen")) {
            return StructuredIntent(
                intent = StructuredIntent.INTENT_UPDATE_HOME_SCREEN,
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // 15. Check for What Matters Today / Life Context / Reasoning Intent
        if (lower.contains("what matters today") || lower.contains("what matters now") || lower.contains("what matters") ||
            lower.contains("what should i do") || lower.contains("what do you know about my day") ||
            lower.contains("what is my next important thing") || lower.contains("next important thing") ||
            lower.contains("what's my next") || lower.contains("what are my priorities") ||
            lower.contains("priorities today") || lower.contains("plan for today") || lower.contains("daily briefing") ||
            lower.contains("good morning") || lower.contains("morning briefing") || lower.contains("what do i have")) {
            return StructuredIntent(
                intent = StructuredIntent.INTENT_WHAT_MATTERS_TODAY,
                rawPrompt = trimmed,
                requiresAuthorization = false
            )
        }

        // Fallback: General conversation
        return StructuredIntent(
            intent = StructuredIntent.INTENT_GENERAL_CONVERSATION,
            rawPrompt = trimmed,
            requiresAuthorization = false
        )
    }

    private fun parseHourMinute(timeStr: String): Pair<Int, Int>? {
        if (timeStr.isBlank()) return null
        val lower = timeStr.lowercase(Locale.ROOT)
        val isPm = lower.contains("pm")
        val isAm = lower.contains("am")

        val digitsOnly = lower.replace("am", "").replace("pm", "").trim()
        val parts = digitsOnly.split(":")
        val rawHour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0

        var hour = rawHour
        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0

        return Pair(hour, minute)
    }
}

