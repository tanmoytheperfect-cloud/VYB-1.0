package com.example.domain.genesis

enum class GenesisDataSource(val displayName: String) {
    IDENTITY("Owner Identity & Creed"),
    DEVICE("Android Device Hardware & Telemetry"),
    CONTACTS("Contacts & VIP Associates"),
    CALENDAR("Calendar & Agenda Timeline"),
    LOCATION("Location & Transit Nodes"),
    FILES("Documents & Work Files"),
    PHOTOS("Visual Moments & Artifacts"),
    NOTIFICATIONS("System & App Notifications"),
    MICROPHONE("Voice Intent & Dictation"),
    CAMERA("Camera & Visual Scanning"),
    SUPPORTED_ACCOUNTS("Connected Cloud & Work Accounts"),
    SUPPORTED_SERVICES("Productivity & Mobility Services"),
    SUPPORTED_COMMUNICATION_DATA("Communication & Messaging Context"),
    SUPPORTED_FINANCIAL_DATA("Financial Planning & Budget Context")
}

enum class DataSourceStatus(val label: String) {
    DEVICE_DATA_AVAILABLE("DEVICE DATA AVAILABLE"),
    DEVICE_DATA_NOT_AVAILABLE("DEVICE DATA NOT AVAILABLE"),
    PERMISSION_REQUIRED("PERMISSION REQUIRED"),
    SERVICE_NOT_CONNECTED("SERVICE NOT CONNECTED")
}

data class GenesisSourceSpec(
    val type: GenesisDataSource,
    val title: String,
    val category: String,
    val whyVybNeedsIt: String,
    val whatWillBeRead: String,
    val whatWillBeStored: String,
    val whereItWillBeStored: String = "On-Device Encrypted SQLite / Room Vault",
    val whetherItWillLeaveTheDevice: String = "Never leaves this device without explicit owner prompt/consent",
    val howToDisableIt: String = "Toggle off anytime in Control > My Permissions",
    val status: DataSourceStatus,
    val isAuthorized: Boolean = false,
    val requiresAndroidPermission: String? = null
)

enum class InformationClassification {
    CONFIRMED,
    INFERRED,
    TEMPORARY,
    UNKNOWN
}

enum class GraphNodeType {
    PERSON,
    WORK,
    COMPANY,
    PROJECT,
    CLIENT,
    DOCUMENT,
    DEADLINE,
    FAMILY,
    CHILD,
    SCHOOL,
    ROUTINE,
    CALENDAR_EVENT,
    FINANCE,
    INCOME,
    EXPENSES,
    GOALS,
    PLACE,
    SERVICE,
    PREFERENCE
}

data class ContextGraphNode(
    val id: String,
    val label: String,
    val type: GraphNodeType,
    val classification: InformationClassification,
    val source: String,
    val confidence: Float = 1.0f,
    val details: String = ""
)

data class ContextGraphEdge(
    val fromId: String,
    val toId: String,
    val relation: String,
    val classification: InformationClassification = InformationClassification.CONFIRMED
)

data class PersonalContextGraph(
    val nodes: List<ContextGraphNode> = emptyList(),
    val edges: List<ContextGraphEdge> = emptyList()
)

data class PatternDiscoveryItem(
    val id: String,
    val observation: String,
    val category: String,
    val confidence: Float,
    val evidenceDaysCount: Int,
    val suggestedType: String, // "SAVE_AS_ROUTINE" or "SAVE_AS_PREFERENCE"
    val isConfirmed: Boolean = false,
    val isDismissed: Boolean = false
)

enum class MemorySourceType {
    USER_STATED,
    SYSTEM_OBSERVED,
    SYSTEM_INFERRED
}

enum class MemoryPrivacyLevel {
    ON_DEVICE_ONLY,
    ENCRYPTED_VAULT,
    CONFIDENTIAL
}

enum class MemoryConfirmationState {
    CONFIRMED,
    PENDING_REVIEW,
    REJECTED
}

data class PersonalAiMemoryItem(
    val id: Long,
    val content: String,
    val source: MemorySourceType,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float,
    val category: String,
    val privacyLevel: MemoryPrivacyLevel = MemoryPrivacyLevel.ON_DEVICE_ONLY,
    val confirmationState: MemoryConfirmationState = MemoryConfirmationState.CONFIRMED
)

enum class PersonalityStyle(val title: String, val description: String) {
    CONCISE_DIRECT(
        "Concise & Direct",
        "Brief, high-density responses focused purely on executive actions and decisions."
    ),
    DETAILED_ANALYTICAL(
        "Detailed & Analytical",
        "In-depth reasoning, statistical context, and thorough step-by-step breakdowns."
    ),
    VOICE_FIRST(
        "Voice-First Companion",
        "Optimized for conversational acoustics, natural speech cadence, and audio feedback."
    ),
    WORK_FOCUSED(
        "Work & Enterprise Focused",
        "Prioritizes company deadlines, client deliverables, project velocity, and calendar."
    ),
    FAMILY_FOCUSED(
        "Family & Household Focused",
        "Highlights school logistics, family routines, domestic harmony, and shared calendars."
    ),
    CREATIVE_WORKSPACE(
        "Creative & Strategic Sparring",
        "Expansive brainstorms, idea synthesis, lateral thinking, and conceptual diagrams."
    )
}

data class VybPersonalProfile(
    // 1. Identity
    val name: String = "",
    val callingTitle: String = "Sir",
    val personalCreed: String = "Autonomy through proactive intelligence.",
    val pronouns: String = "They/Them",

    // 2. Languages
    val primaryLanguage: String = "English",
    val secondaryLanguages: List<String> = listOf("Hindi", "Spanish"),
    val aiSpeakingDialect: String = "Executive English (En-US / En-IN)",

    // 3. Communication Style
    val communicationStyle: PersonalityStyle = PersonalityStyle.CONCISE_DIRECT,
    val responseVerbosity: String = "Balanced",

    // 4. Preferences
    val dietaryPreference: String = "High-Protein Balanced",
    val wakeUpTime: String = "07:00 AM",
    val sleepTime: String = "11:30 PM",
    val commutePreference: String = "Metro / Ride-hailing",

    // 5. Family Context
    val familySummary: String = "",
    val dependentsNotes: String = "",
    val schoolRoutines: String = "",

    // 6. Work Context
    val companyName: String = "",
    val jobTitle: String = "",
    val industry: String = "Technology & Operations",
    val officeAddress: String = "",

    // 7. Projects
    val activeProjects: List<String> = listOf("VYB OS Genesis", "Q4 Strategy Roadmap"),

    // 8. Goals
    val personalGoals: List<String> = listOf("Achieve 20% discretionary savings", "Daily 45-min deep work"),

    // 9. Routines
    val morningRoutine: String = "07:15 Wakeup -> Device Check -> Daily Briefing",
    val commuteRoutine: String = "09:00 Transit to Office -> Audio News Summary",
    val eveningRoutine: String = "22:00 Daily Reflection -> Task Triage",

    // 10. Important People
    val importantPeople: List<String> = listOf("Spouse", "Family", "Key Associate"),

    // 11. Important Places
    val homeAddress: String = "",
    val workPlaceAddress: String = "",
    val gymOrRecreation: String = "",

    // 12. Services
    val connectedServices: List<String> = listOf("Google Workspace", "Local Telemetry", "Encrypted Vault"),

    // 13. Interests
    val interests: List<String> = listOf("Artificial Intelligence", "Personal Productivity", "Financial Architecture"),

    // 14. Digital Habits
    val screenTimeTargetHours: Int = 4,
    val dndStartTime: String = "23:00",
    val dndEndTime: String = "07:00",

    // 15. Financial Planning Context
    val monthlyIncome: Double = 0.0,
    val fixedCommitments: Double = 0.0,
    val savingsTarget: Double = 0.0,
    val currencySymbol: String = "₹",

    // 16. Personal Priorities
    val personalPriorities: List<String> = listOf(
        "Maintain deep focus during mornings",
        "Ensure family schedule commitments are never missed",
        "Streamline digital life via on-device AI"
    )
)

data class PersonalDigitalTwin(
    val whoIAm: String,
    val whatICareAbout: List<String>,
    val whatIDo: String,
    val whoMattersToMe: List<String>,
    val whatIAmWorkingOn: List<String>,
    val whatIAmTryingToAchieve: List<String>,
    val whatIPrefer: List<String>,
    val whatIUsuallyDo: List<String>,
    val whatIHaveAuthorizedVybToDo: List<String>
)

data class UniversalInstanceMetadata(
    val instanceId: String,
    val ownerFingerprint: String,
    val creationTimestamp: Long = System.currentTimeMillis(),
    val encryptionAlgorithm: String = "AES-256-GCM / Android KeyStore",
    val deviceArchitecture: String = "Local Isolated Instance",
    val isGenesisCompleted: Boolean = false
)

data class MigrationPackage(
    val version: Int = 1,
    val instanceId: String,
    val exportedAt: Long = System.currentTimeMillis(),
    val payloadJson: String,
    val checksum: String
)
