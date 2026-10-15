package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import java.net.URLEncoder

/**
 * Real Android Device App Execution Bridge.
 * Allows VYB to interact with external mobile apps on the owner's device with explicit owner permission.
 */
object DeviceAppExecutionBridge {

    data class ExecutionResult(
        val success: Boolean,
        val appName: String,
        val actionDescription: String,
        val details: String
    )

    /**
     * Launch Cab / Ride Booking App with destination
     */
    fun executeRideBooking(
        context: Context,
        provider: String,
        destination: String,
        pickup: String = "Current Location"
    ): ExecutionResult {
        val encodedDest = URLEncoder.encode(destination, "UTF-8")
        val pkg = when (provider.lowercase().trim()) {
            "uber" -> "com.ubercab"
            "ola" -> "com.olacabs.customer"
            "rapido" -> "com.rapido.passenger"
            "yatri sathi", "yatrisathi" -> "com.yatrisathi.customer"
            "indrive", "indriver", "in drive" -> "sinet.startup.inDriver"
            else -> null
        }

        com.example.domain.privacy.FluidPrivacyFirewall.recordExternalIntentDispatch(
            targetName = provider,
            destinationPackageOrUri = pkg ?: "ride_provider",
            purpose = "Ride booking dispatch to $destination",
            categories = setOf(
                com.example.domain.privacy.DataCategory.LOCATION_COORDINATES,
                com.example.domain.privacy.DataCategory.EXTERNAL_COMMUNICATION
            ),
            payload = "destination=$destination,pickup=$pickup"
        )

        try {
            // 1. Try launching installed app via PackageManager
            if (pkg != null) {
                var launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent == null && (pkg == "sinet.startup.inDriver")) {
                    launchIntent = context.packageManager.getLaunchIntentForPackage("com.sinoptik.client")
                }
                if (launchIntent == null && (pkg == "com.yatrisathi.customer")) {
                    launchIntent = context.packageManager.getLaunchIntentForPackage("com.yatrisathi")
                }
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ExecutionResult(
                        success = true,
                        appName = provider,
                        actionDescription = "Opened $provider for $destination",
                        details = "Opened $provider app on your phone so you can view live rates and complete booking."
                    )
                }
            }

            // 2. Try URI Scheme Deep Links
            val deepLinkUri = when (provider.lowercase().trim()) {
                "uber" -> Uri.parse("uber://?action=setPickup&pickup=my_location&dropoff[formatted_address]=$encodedDest")
                "ola" -> Uri.parse("olacabs://app/launch")
                "rapido" -> Uri.parse("rapido://")
                "indrive", "indriver", "in drive" -> Uri.parse("indriver://")
                else -> null
            }

            if (deepLinkUri != null) {
                val deepLinkIntent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (deepLinkIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(deepLinkIntent)
                    return ExecutionResult(
                        success = true,
                        appName = provider,
                        actionDescription = "Launched $provider deep-link for $destination",
                        details = "Opened $provider app on device."
                    )
                }
            }

            // 3. Fallback to Official Mobile Web App or Google Maps Ride Navigation
            if (com.example.domain.privacy.FluidPrivacyFirewall.isLocalOnlyMode.value) {
                return ExecutionResult(
                    success = false,
                    appName = provider,
                    actionDescription = "External web redirection blocked",
                    details = "Local-Only Mode is active: external web URL redirection blocked by Fluid Privacy Firewall."
                )
            }

            val (webUri, actionLabel) = when (provider.lowercase().trim()) {
                "uber" -> Pair(Uri.parse("https://m.uber.com/looking?drop[0]=$encodedDest"), "Uber Web")
                "ola" -> Pair(Uri.parse("https://book.olacabs.com"), "Ola Web")
                "rapido" -> Pair(Uri.parse("https://www.rapido.bike"), "Rapido Web")
                "yatri sathi", "yatrisathi" -> Pair(Uri.parse("https://yatrisathi.wb.gov.in"), "Yatri Sathi Web")
                "indrive", "indriver", "in drive" -> Pair(Uri.parse("https://indrive.com"), "inDrive Web")
                else -> Pair(Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$encodedDest"), "Google Maps")
            }

            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)

            return ExecutionResult(
                success = true,
                appName = "$provider ($actionLabel)",
                actionDescription = "Opened $provider for $destination",
                details = "Opened provider interface in browser/navigation."
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Final fallback: Google Maps search
            if (com.example.domain.privacy.FluidPrivacyFirewall.isLocalOnlyMode.value) {
                return ExecutionResult(
                    success = false,
                    appName = provider,
                    actionDescription = "External web redirection blocked",
                    details = "Local-Only Mode is active: external web search blocked by Fluid Privacy Firewall."
                )
            }
            return try {
                val mapUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedDest")
                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(mapIntent)
                ExecutionResult(
                    success = true,
                    appName = "Google Maps",
                    actionDescription = "Navigated to $destination",
                    details = "Fallback navigation opened."
                )
            } catch (err: Exception) {
                ExecutionResult(
                    success = false,
                    appName = provider,
                    actionDescription = "Failed to launch $provider",
                    details = e.localizedMessage ?: "App not available on device"
                )
            }
        }
    }

    /**
     * Send WhatsApp message or open chat with pre-filled text
     */
    fun executeWhatsAppMessage(
        context: Context,
        phoneNumber: String?,
        messageText: String
    ): ExecutionResult {
        val encodedMsg = URLEncoder.encode(messageText, "UTF-8")
        val cleanPhone = phoneNumber?.replace("[^0-9+]".toRegex(), "") ?: ""
        
        com.example.domain.privacy.FluidPrivacyFirewall.recordExternalIntentDispatch(
            targetName = "WhatsApp",
            destinationPackageOrUri = "com.whatsapp",
            purpose = "Send WhatsApp communication",
            categories = setOf(com.example.domain.privacy.DataCategory.EXTERNAL_COMMUNICATION),
            payload = "recipient=${cleanPhone.ifBlank { "selected_contact" }},message=$messageText"
        )

        try {
            // 1. Try WhatsApp direct URI
            val whatsappUri = if (cleanPhone.isNotBlank()) {
                Uri.parse("whatsapp://send?phone=$cleanPhone&text=$encodedMsg")
            } else {
                Uri.parse("whatsapp://send?text=$encodedMsg")
            }
            val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsappIntent)
                return ExecutionResult(
                    success = true,
                    appName = "WhatsApp",
                    actionDescription = "Opened WhatsApp chat draft",
                    details = "Drafted: \"${messageText.take(50)}...\""
                )
            }

            // 2. Try web api link with WhatsApp package
            val webUrl = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMsg"
            }
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Try WhatsApp or WhatsApp Business packages
            for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
                val pkgIntent = Intent(webIntent).apply { setPackage(pkg) }
                if (pkgIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(pkgIntent)
                    return ExecutionResult(
                        success = true,
                        appName = "WhatsApp",
                        actionDescription = "Opened WhatsApp message draft",
                        details = "Drafted: \"${messageText.take(50)}...\""
                    )
                }
            }

            // 3. Fallback to browser or SMS
            if (com.example.domain.privacy.FluidPrivacyFirewall.isLocalOnlyMode.value) {
                // If Local-Only Mode is active, fallback to device SMS to avoid outbound web link
                return executeSendSms(context, cleanPhone, messageText)
            }

            context.startActivity(webIntent)
            return ExecutionResult(
                success = true,
                appName = "WhatsApp Web",
                actionDescription = "Opened WhatsApp link",
                details = "Opened WhatsApp dispatch link."
            )
        } catch (e: Exception) {
            // Fallback to SMS
            return executeSendSms(context, cleanPhone, messageText)
        }
    }

    /**
     * Send SMS / Messages
     */
    fun executeSendSms(
        context: Context,
        phoneNumber: String?,
        messageText: String
    ): ExecutionResult {
        com.example.domain.privacy.FluidPrivacyFirewall.recordExternalIntentDispatch(
            targetName = "Messages / SMS",
            destinationPackageOrUri = "smsto",
            purpose = "Send SMS text message",
            categories = setOf(com.example.domain.privacy.DataCategory.EXTERNAL_COMMUNICATION),
            payload = "recipient=${phoneNumber ?: "selected_contact"},message=$messageText"
        )

        try {
            val uri = if (!phoneNumber.isNullOrBlank()) Uri.parse("smsto:$phoneNumber") else Uri.parse("smsto:")
            val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            return ExecutionResult(
                success = true,
                appName = "Messages / SMS",
                actionDescription = "Opened SMS composer",
                details = "Drafted SMS to ${phoneNumber ?: "contact"}"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return ExecutionResult(
                success = false,
                appName = "Messages",
                actionDescription = "Failed to launch SMS",
                details = e.localizedMessage ?: "No messaging app found"
            )
        }
    }

    /**
     * Launch Turn-by-Turn Map Navigation to Address / Location
     */
    fun executeMapNavigation(
        context: Context,
        destinationAddress: String
    ): ExecutionResult {
        com.example.domain.privacy.FluidPrivacyFirewall.recordExternalIntentDispatch(
            targetName = "Google Maps",
            destinationPackageOrUri = "com.google.android.apps.maps",
            purpose = "Turn-by-turn navigation route to $destinationAddress",
            categories = setOf(com.example.domain.privacy.DataCategory.LOCATION_COORDINATES),
            payload = "destination=$destinationAddress"
        )

        try {
            val encoded = URLEncoder.encode(destinationAddress, "UTF-8")
            val uri = Uri.parse("google.navigation:q=$encoded")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                if (com.example.domain.privacy.FluidPrivacyFirewall.isLocalOnlyMode.value) {
                    return ExecutionResult(
                        success = false,
                        appName = "Google Maps",
                        actionDescription = "Web maps redirection blocked",
                        details = "Local-Only Mode is active: external web map URL redirection blocked by Fluid Privacy Firewall."
                    )
                }
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            }
            return ExecutionResult(
                success = true,
                appName = "Google Maps",
                actionDescription = "Launched navigation to $destinationAddress",
                details = "Navigation route initialized."
            )
        } catch (e: Exception) {
            return ExecutionResult(
                success = false,
                appName = "Google Maps",
                actionDescription = "Failed to open Maps",
                details = e.localizedMessage ?: "Error opening navigation"
            )
        }
    }

    /**
     * Dial Phone Number
     */
    fun executePhoneCall(
        context: Context,
        phoneNumber: String
    ): ExecutionResult {
        val clean = phoneNumber.replace("[^0-9+]".toRegex(), "")
        com.example.domain.privacy.FluidPrivacyFirewall.recordExternalIntentDispatch(
            targetName = "Phone Dialer",
            destinationPackageOrUri = "tel",
            purpose = "Place outbound phone call to $clean",
            categories = setOf(com.example.domain.privacy.DataCategory.EXTERNAL_COMMUNICATION),
            payload = "number=$clean"
        )
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ExecutionResult(
                success = true,
                appName = "Phone Dialer",
                actionDescription = "Opened dialer for $clean",
                details = "Ready for call."
            )
        } catch (e: Exception) {
            return ExecutionResult(
                success = false,
                appName = "Phone Dialer",
                actionDescription = "Failed to open dialer",
                details = e.localizedMessage ?: "Dialer unavailable"
            )
        }
    }

    /**
     * Draft Email in Gmail / Default Mail App
     */
    fun executeEmailDraft(
        context: Context,
        recipientEmail: String,
        subject: String,
        bodyText: String
    ): ExecutionResult {
        com.example.domain.privacy.FluidPrivacyFirewall.recordExternalIntentDispatch(
            targetName = "Email / Gmail",
            destinationPackageOrUri = "mailto",
            purpose = "Draft email to $recipientEmail",
            categories = setOf(com.example.domain.privacy.DataCategory.EXTERNAL_COMMUNICATION),
            payload = "recipient=$recipientEmail,subject=$subject"
        )
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$recipientEmail")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, bodyText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ExecutionResult(
                success = true,
                appName = "Email / Gmail",
                actionDescription = "Drafted email to $recipientEmail",
                details = "Subject: $subject"
            )
        } catch (e: Exception) {
            return ExecutionResult(
                success = false,
                appName = "Email",
                actionDescription = "Failed to open email app",
                details = e.localizedMessage ?: "No email app found"
            )
        }
    }

    /**
     * Search E-Commerce / Quick Commerce (Amazon, Flipkart, Blinkit, etc.)
     */
    fun executeShoppingSearch(
        context: Context,
        platform: String,
        searchQuery: String
    ): ExecutionResult {
        val encoded = URLEncoder.encode(searchQuery, "UTF-8")
        val (pkg, webUrl) = when (platform.lowercase()) {
            "amazon" -> Pair("com.amazon.mShop.android.shopping", "https://www.amazon.in/s?k=$encoded")
            "flipkart" -> Pair("com.flipkart.android", "https://www.flipkart.com/search?q=$encoded")
            "blinkit" -> Pair("com.grofers.customerapp", "https://blinkit.com/s/?q=$encoded")
            "zepto" -> Pair("com.zeptonow.android", "https://www.zeptonow.com/search?q=$encoded")
            else -> Pair(null, "https://www.google.com/search?q=$encoded")
        }

        com.example.domain.privacy.FluidPrivacyFirewall.recordExternalIntentDispatch(
            targetName = platform,
            destinationPackageOrUri = pkg ?: "web_search",
            purpose = "Search products on $platform for \"$searchQuery\"",
            categories = setOf(com.example.domain.privacy.DataCategory.EXTERNAL_COMMUNICATION),
            payload = "query=$searchQuery,platform=$platform"
        )

        try {
            if (pkg != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ExecutionResult(
                        success = true,
                        appName = platform,
                        actionDescription = "Launched $platform for \"$searchQuery\"",
                        details = "Opened $platform app to search products."
                    )
                }
            }

            if (com.example.domain.privacy.FluidPrivacyFirewall.isLocalOnlyMode.value) {
                return ExecutionResult(
                    success = false,
                    appName = platform,
                    actionDescription = "Web search redirection blocked",
                    details = "Local-Only Mode is active: external web search blocked by Fluid Privacy Firewall."
                )
            }

            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            return ExecutionResult(
                success = true,
                appName = "$platform (Web)",
                actionDescription = "Searched $platform for \"$searchQuery\"",
                details = "Opened search results in browser."
            )
        } catch (e: Exception) {
            return ExecutionResult(
                success = false,
                appName = platform,
                actionDescription = "Failed to search $platform",
                details = e.localizedMessage ?: "App or web browser unavailable"
            )
        }
    }

    /**
     * Add Event to Google Calendar
     */
    fun executeCalendarEvent(
        context: Context,
        title: String,
        description: String,
        location: String = "",
        startTimeMillis: Long = System.currentTimeMillis() + 3600000,
        endTimeMillis: Long = System.currentTimeMillis() + 7200000
    ): ExecutionResult {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description)
                putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeMillis)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ExecutionResult(
                success = true,
                appName = "Google Calendar",
                actionDescription = "Added calendar event: $title",
                details = "Calendar entry created."
            )
        } catch (e: Exception) {
            return ExecutionResult(
                success = false,
                appName = "Calendar",
                actionDescription = "Failed to add calendar event",
                details = e.localizedMessage ?: "Calendar app unavailable"
            )
        }
    }

    /**
     * Set System Alarm or Reminder
     */
    fun executeSetAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        message: String
    ): ExecutionResult {
        try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ExecutionResult(
                success = true,
                appName = "Clock & Alarms",
                actionDescription = "Set alarm for $hour:$minute ($message)",
                details = "System alarm scheduled."
            )
        } catch (e: Exception) {
            return ExecutionResult(
                success = false,
                appName = "Clock",
                actionDescription = "Failed to set alarm",
                details = e.localizedMessage ?: "Clock app unavailable"
            )
        }
    }
}
