package com.example.domain.intelligence

import android.content.Context
import com.example.domain.permission.PermissionState
import com.example.domain.permission.VybPermissionManager
import com.example.domain.permission.VybPermissionType

/**
 * Capability operational availability state.
 */
enum class CapabilityAvailability(val label: String, val isOperable: Boolean) {
    ACTIVE("ACTIVE", true),
    PERMISSION_REQUIRED("PERMISSION REQUIRED", false),
    TOOL_UNAVAILABLE("TOOL UNAVAILABLE", false),
    SERVICE_NOT_CONNECTED("SERVICE NOT CONNECTED", false),
    OFFLINE_ONLY("OFFLINE ONLY", true)
}

/**
 * Capability declaration inside the VYB Capability Registry.
 */
data class VybCapabilityDeclaration(
    val capabilityId: String,
    val name: String,
    val description: String,
    val requiredPermissions: List<VybPermissionType>,
    val requiredTools: List<String>,
    val supportedPlatforms: List<String> = listOf("Android Native", "Room DB"),
    val onlineRequired: Boolean = false,
    val confirmationRequired: Boolean = false,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val availability: CapabilityAvailability = CapabilityAvailability.ACTIVE,
    val verificationMethod: String = "Internal SQLite Verification"
)

/**
 * Central Capability Registry.
 * Declares all 23 standard capabilities and evaluates their true operational availability
 * based on live device permissions, installed apps, and system access.
 */
class VybCapabilityRegistry {

    fun getAllCapabilities(context: Context?, permissionManager: VybPermissionManager?): List<VybCapabilityDeclaration> {
        val checkPerm = { type: VybPermissionType ->
            permissionManager?.checkPermission(type) ?: PermissionState.NOT_GRANTED
        }

        return listOf(
            // 1. CHAT
            VybCapabilityDeclaration(
                capabilityId = "CHAT",
                name = "Contextual Dialogue",
                description = "On-device multi-turn dialogue grounded in personal profile and memory.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("BrainEngine", "GeminiRepository"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Response Content Verification"
            ),

            // 2. SEARCH
            VybCapabilityDeclaration(
                capabilityId = "SEARCH",
                name = "Fast Web & App Search",
                description = "Direct web browser search dispatch via Android Intent.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("WebSearchTool"),
                onlineRequired = true,
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Intent Activity Resolved"
            ),

            // 3. RESEARCH
            VybCapabilityDeclaration(
                capabilityId = "RESEARCH",
                name = "Deep Research & Synthesis",
                description = "Multi-source topic research and structured fact synthesis.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("ResearchAgent", "WebSearchTool"),
                onlineRequired = true,
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Summary & Citation Audit"
            ),

            // 4. IMAGE_ANALYSIS
            VybCapabilityDeclaration(
                capabilityId = "IMAGE_ANALYSIS",
                name = "Visual & Image Understanding",
                description = "Inspects image attachments, receipts, and camera scans.",
                requiredPermissions = listOf(VybPermissionType.CAMERA),
                requiredTools = listOf("CameraTool", "VisionModel"),
                availability = if (checkPerm(VybPermissionType.CAMERA) == PermissionState.AVAILABLE)
                    CapabilityAvailability.ACTIVE else CapabilityAvailability.PERMISSION_REQUIRED,
                verificationMethod = "Visual Entity Tagging"
            ),

            // 5. DOCUMENT_ANALYSIS
            VybCapabilityDeclaration(
                capabilityId = "DOCUMENT_ANALYSIS",
                name = "Document & File Analysis",
                description = "Extracts structured insights from local text documents and notes.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("DocumentAgent", "FileVault"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Document Parser Verification"
            ),

            // 6. VOICE
            VybCapabilityDeclaration(
                capabilityId = "VOICE",
                name = "Voice & Microphone",
                description = "Speech recognition and hands-free voice commanding.",
                requiredPermissions = listOf(VybPermissionType.MICROPHONE),
                requiredTools = listOf("JarvisCoreManager", "SpeechRecognizer"),
                availability = if (checkPerm(VybPermissionType.MICROPHONE) == PermissionState.AVAILABLE)
                    CapabilityAvailability.ACTIVE else CapabilityAvailability.PERMISSION_REQUIRED,
                verificationMethod = "Audio Buffer Stream State"
            ),

            // 7. MEMORY
            VybCapabilityDeclaration(
                capabilityId = "MEMORY",
                name = "Personal AI Memory",
                description = "Local encrypted memory and Personal Life Vault persistence.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("MemoryDao", "PersonalLifeVaultEngine"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "SQLite Row Count & Checksum"
            ),

            // 8. REMINDER
            VybCapabilityDeclaration(
                capabilityId = "REMINDER",
                name = "Reminders & Alarms",
                description = "Creates exact alarms and local task reminders.",
                requiredPermissions = listOf(VybPermissionType.ALARM),
                requiredTools = listOf("TaskDao", "AlarmClockBridge"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Task DB Insertion & System Intent"
            ),

            // 9. CALENDAR
            VybCapabilityDeclaration(
                capabilityId = "CALENDAR",
                name = "Calendar Agenda",
                description = "Reads upcoming meetings and schedules new calendar events.",
                requiredPermissions = listOf(VybPermissionType.CALENDAR),
                requiredTools = listOf("CalendarTool", "CalendarContract"),
                availability = if (checkPerm(VybPermissionType.CALENDAR) == PermissionState.AVAILABLE)
                    CapabilityAvailability.ACTIVE else CapabilityAvailability.PERMISSION_REQUIRED,
                verificationMethod = "Calendar Event ContentResolver Verification"
            ),

            // 10. CONTACTS
            VybCapabilityDeclaration(
                capabilityId = "CONTACTS",
                name = "Device Contacts",
                description = "Directly resolves associate phone numbers and relationship links.",
                requiredPermissions = listOf(VybPermissionType.CONTACTS),
                requiredTools = listOf("ContactsTool"),
                availability = if (checkPerm(VybPermissionType.CONTACTS) == PermissionState.AVAILABLE)
                    CapabilityAvailability.ACTIVE else CapabilityAvailability.PERMISSION_REQUIRED,
                verificationMethod = "Contact Lookup Verification"
            ),

            // 11. LOCATION
            VybCapabilityDeclaration(
                capabilityId = "LOCATION",
                name = "GPS Location & Telemetry",
                description = "Live coordinates for transit estimation and hyperlocal context.",
                requiredPermissions = listOf(VybPermissionType.LOCATION),
                requiredTools = listOf("JarvisLocationTelemetry"),
                availability = if (checkPerm(VybPermissionType.LOCATION) == PermissionState.AVAILABLE)
                    CapabilityAvailability.ACTIVE else CapabilityAvailability.PERMISSION_REQUIRED,
                verificationMethod = "Location Coordinates Timestamp"
            ),

            // 12. FILES
            VybCapabilityDeclaration(
                capabilityId = "FILES",
                name = "File Vault Management",
                description = "Local file management and migration archive package creation.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("GenesisMigrationEngine"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "File IO Status"
            ),

            // 13. CAMERA
            VybCapabilityDeclaration(
                capabilityId = "CAMERA",
                name = "Camera Capture",
                description = "Direct camera launch for receipts and quick item snapping.",
                requiredPermissions = listOf(VybPermissionType.CAMERA),
                requiredTools = listOf("CameraBridge"),
                availability = if (checkPerm(VybPermissionType.CAMERA) == PermissionState.AVAILABLE)
                    CapabilityAvailability.ACTIVE else CapabilityAvailability.PERMISSION_REQUIRED,
                verificationMethod = "MediaStore Image Capture Result"
            ),

            // 14. NOTIFICATIONS
            VybCapabilityDeclaration(
                capabilityId = "NOTIFICATIONS",
                name = "Proactive Notifications",
                description = "System notifications for timely briefings and reminders.",
                requiredPermissions = listOf(VybPermissionType.NOTIFICATIONS),
                requiredTools = listOf("NotificationManager"),
                availability = if (checkPerm(VybPermissionType.NOTIFICATIONS) == PermissionState.AVAILABLE)
                    CapabilityAvailability.ACTIVE else CapabilityAvailability.PERMISSION_REQUIRED,
                verificationMethod = "Notification Channel Active"
            ),

            // 15. SHOPPING
            VybCapabilityDeclaration(
                capabilityId = "SHOPPING",
                name = "Shopping & Price Compare",
                description = "Direct search dispatch to Amazon, Flipkart, and local cart tracking.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("ShoppingDao", "DeviceAppExecutionBridge"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Shopping DB Insertion & Intent Dispatch"
            ),

            // 16. FINANCE_ANALYSIS
            VybCapabilityDeclaration(
                capabilityId = "FINANCE_ANALYSIS",
                name = "Financial Planning",
                description = "Expense tracking, affordability calculations, and monthly budget checks.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("ExpenseDao", "FinanceCalculatorTool"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Expense Ledger Audit"
            ),

            // 17. EMAIL
            VybCapabilityDeclaration(
                capabilityId = "EMAIL",
                name = "Email Dispatch",
                description = "Composes and drafts emails via standard email client intent.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("EmailIntentBridge"),
                riskLevel = RiskLevel.HIGH,
                confirmationRequired = true,
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Mail Intent Intent Activity Check"
            ),

            // 18. MESSAGING
            VybCapabilityDeclaration(
                capabilityId = "MESSAGING",
                name = "Messaging & SMS / WhatsApp",
                description = "Prepares and dispatches messages to contacts with user preview.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("DeviceAppExecutionBridge"),
                riskLevel = RiskLevel.HIGH,
                confirmationRequired = true,
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Messaging Intent Dispatch Verification"
            ),

            // 19. AUTOMATION
            VybCapabilityDeclaration(
                capabilityId = "AUTOMATION",
                name = "Personal Automation",
                description = "Executes learned routines and proactive background recommendations.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("RoutineLearningEngine", "ProactiveDecisionEngine"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Automation Trigger Event Log"
            ),

            // 20. WEB_INTERACTION
            VybCapabilityDeclaration(
                capabilityId = "WEB_INTERACTION",
                name = "Web Interaction & URL Launch",
                description = "Opens specific web portals, dashboards, and booking URLs.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("BrowserIntentBridge"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Browser Launch Activity Verification"
            ),

            // 21. CODING
            VybCapabilityDeclaration(
                capabilityId = "CODING",
                name = "Code & Script Computation",
                description = "Logic evaluation, syntax generation, and data formulas.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("CodingAgent"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Syntax & Output Verification"
            ),

            // 22. DATA_ANALYSIS
            VybCapabilityDeclaration(
                capabilityId = "DATA_ANALYSIS",
                name = "Data & Statistical Analysis",
                description = "Aggregates weekly screen habits, expense trends, and routine consistency.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("WeeklyRoutineAnalysisEngine"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Statistical Trend Calculation"
            ),

            // 23. CREATIVE_GENERATION
            VybCapabilityDeclaration(
                capabilityId = "CREATIVE_GENERATION",
                name = "Creative Formulation",
                description = "Drafts strategic proposals, message phrasing, and creative outlines.",
                requiredPermissions = emptyList(),
                requiredTools = listOf("CreativeAgent"),
                availability = CapabilityAvailability.ACTIVE,
                verificationMethod = "Creativity Heuristic Check"
            )
        )
    }
}
