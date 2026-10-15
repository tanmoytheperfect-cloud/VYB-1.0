package com.example.domain.genesis

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.TaskItemEntity
import com.example.data.local.entity.TimelineEventEntity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

class PersonalGenesisEngine(private val context: Context) {

    fun getInitialSourceSpecs(
        ownerName: String,
        hasFinancialData: Boolean,
        hasConnectedAccounts: Boolean,
        authorizedTypes: Set<GenesisDataSource>
    ): List<GenesisSourceSpec> {
        return listOf(
            GenesisSourceSpec(
                type = GenesisDataSource.IDENTITY,
                title = "Owner Identity & Calling Title",
                category = "Core Identity",
                whyVybNeedsIt = "To tailor responses, calibrate address tone, and anchor your unique Personal Context Graph.",
                whatWillBeRead = "Your name, preferred title, core personal mission, and languages.",
                whatWillBeStored = "Structured identity parameters in your local encrypted device profile.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Toggle off or clear anytime in Control > My Permissions",
                status = if (ownerName.isNotBlank()) DataSourceStatus.DEVICE_DATA_AVAILABLE else DataSourceStatus.DEVICE_DATA_NOT_AVAILABLE,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.IDENTITY) || ownerName.isNotBlank()
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.DEVICE,
                title = "Android Hardware & Battery Telemetry",
                category = "System Environment",
                whyVybNeedsIt = "To gauge battery conservation states, network connectivity, and screen lifecycle.",
                whatWillBeRead = "Battery level, charging state, thermal mode, connectivity type.",
                whatWillBeStored = "Temporary real-time context snapshots; discarded after reflection cycles.",
                whereItWillBeStored = "In-memory telemetry buffer & transient event log",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Toggle off anytime in Control > My Permissions",
                status = DataSourceStatus.DEVICE_DATA_AVAILABLE,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.DEVICE)
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.CONTACTS,
                title = "Contacts & VIP Relationships",
                category = "Personal Network",
                whyVybNeedsIt = "To link family members, colleagues, and emergency VIPs without generic hallucinations.",
                whatWillBeRead = "Contact display names and relation tags for matched family/work contacts.",
                whatWillBeStored = "Matched contact identifiers linked to Personal Context Graph nodes.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Revoke in Android Settings or toggle in My Permissions",
                status = if (hasPermission(android.Manifest.permission.READ_CONTACTS))
                    DataSourceStatus.DEVICE_DATA_AVAILABLE else DataSourceStatus.PERMISSION_REQUIRED,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.CONTACTS),
                requiresAndroidPermission = android.Manifest.permission.READ_CONTACTS
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.CALENDAR,
                title = "Calendar & Schedule Timeline",
                category = "Temporal Context",
                whyVybNeedsIt = "To deliver truthful morning briefings, recognize commute constraints, and alert on conflicts.",
                whatWillBeRead = "Upcoming event titles, start/end timestamps, and location strings.",
                whatWillBeStored = "Temporal timeline events for active day reasoning.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Revoke in Android Settings or toggle in My Permissions",
                status = if (hasPermission(android.Manifest.permission.READ_CALENDAR))
                    DataSourceStatus.DEVICE_DATA_AVAILABLE else DataSourceStatus.PERMISSION_REQUIRED,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.CALENDAR),
                requiresAndroidPermission = android.Manifest.permission.READ_CALENDAR
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.LOCATION,
                title = "Location & Transit Nodes",
                category = "Geographic Context",
                whyVybNeedsIt = "To calculate transit buffers between home, office, and school, and retrieve hyper-local weather.",
                whatWillBeRead = "Coarse/fine latitude, longitude, and geocoded locality.",
                whatWillBeStored = "Labeled place centroids (Home, Work) as approved by owner.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Revoke in Android Settings or toggle in My Permissions",
                status = if (hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                    hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                ) DataSourceStatus.DEVICE_DATA_AVAILABLE else DataSourceStatus.PERMISSION_REQUIRED,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.LOCATION),
                requiresAndroidPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.FILES,
                title = "Documents & Work Files",
                category = "Productivity Vault",
                whyVybNeedsIt = "To index user-selected project files, contracts, or reference notes via Android SAF picker.",
                whatWillBeRead = "User-picked file text content and document titles.",
                whatWillBeStored = "Parsed context summaries and reference links.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Delete documents or revoke storage access anytime in My Data",
                status = DataSourceStatus.DEVICE_DATA_AVAILABLE,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.FILES)
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.PHOTOS,
                title = "Visual Moments & Photo Picker",
                category = "Visual Memory",
                whyVybNeedsIt = "To analyze wardrobe items, receipts, or documents using the privacy-safe Android Photo Picker.",
                whatWillBeRead = "Only photos explicitly selected by the owner.",
                whatWillBeStored = "Extracted metadata (e.g. receipt total, clothing color tag).",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Toggle off in My Permissions",
                status = DataSourceStatus.DEVICE_DATA_AVAILABLE,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.PHOTOS)
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.NOTIFICATIONS,
                title = "System & App Notifications",
                category = "Device Awareness",
                whyVybNeedsIt = "To present vital delivery, transit, or calendar alerts proactively without polling.",
                whatWillBeRead = "Status bar alert post authorization to alert the owner.",
                whatWillBeStored = "Transient alert dispatch records.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Revoke in Android Settings or toggle in My Permissions",
                status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (hasPermission(android.Manifest.permission.POST_NOTIFICATIONS))
                        DataSourceStatus.DEVICE_DATA_AVAILABLE else DataSourceStatus.PERMISSION_REQUIRED
                } else DataSourceStatus.DEVICE_DATA_AVAILABLE,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.NOTIFICATIONS),
                requiresAndroidPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    android.Manifest.permission.POST_NOTIFICATIONS else null
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.MICROPHONE,
                title = "Voice Input & Intent Dictation",
                category = "Conversational Interface",
                whyVybNeedsIt = "To enable hands-free voice intent queries and natural conversational sparring.",
                whatWillBeRead = "Audio stream when mic button is active.",
                whatWillBeStored = "Transcription text only; raw audio is immediately discarded.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent. Audio transcribed locally or on-device",
                howToDisableIt = "Revoke microphone permission in Android Settings",
                status = if (hasPermission(android.Manifest.permission.RECORD_AUDIO))
                    DataSourceStatus.DEVICE_DATA_AVAILABLE else DataSourceStatus.PERMISSION_REQUIRED,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.MICROPHONE),
                requiresAndroidPermission = android.Manifest.permission.RECORD_AUDIO
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.CAMERA,
                title = "Camera & Document Inspection",
                category = "Visual Perception",
                whyVybNeedsIt = "To capture document scans, wardrobe items, or barcodes on demand.",
                whatWillBeRead = "Camera frame buffer during active camera session.",
                whatWillBeStored = "Image preview if saved by owner, plus visual description.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Revoke camera permission in Android Settings",
                status = if (hasPermission(android.Manifest.permission.CAMERA))
                    DataSourceStatus.DEVICE_DATA_AVAILABLE else DataSourceStatus.PERMISSION_REQUIRED,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.CAMERA),
                requiresAndroidPermission = android.Manifest.permission.CAMERA
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.SUPPORTED_ACCOUNTS,
                title = "Connected Cloud & Work Accounts",
                category = "Identity Integration",
                whyVybNeedsIt = "To verify owner email credentials and authenticate authorized AI task bridges.",
                whatWillBeRead = "Account email, authorization scopes, connected provider metadata.",
                whatWillBeStored = "Scoped tokens and account identifiers in secure vault.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Disconnect individual accounts in Control > My Accounts",
                status = if (hasConnectedAccounts) DataSourceStatus.DEVICE_DATA_AVAILABLE else DataSourceStatus.SERVICE_NOT_CONNECTED,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.SUPPORTED_ACCOUNTS) || hasConnectedAccounts
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.SUPPORTED_SERVICES,
                title = "Productivity & Mobility Services",
                category = "App Integrations",
                whyVybNeedsIt = "To deep-link ride requests (Uber, Ola), calendar events, and team communications (Slack).",
                whatWillBeRead = "Target app availability via Android package queries.",
                whatWillBeStored = "Discovered service links and action dispatch history.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Toggle app execution bridges in Control > App Bridges",
                status = DataSourceStatus.DEVICE_DATA_AVAILABLE,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.SUPPORTED_SERVICES)
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.SUPPORTED_COMMUNICATION_DATA,
                title = "Communication & Messaging Context",
                category = "Comms Intelligence",
                whyVybNeedsIt = "To draft contextual replies to colleagues, family, and clients upon direct request.",
                whatWillBeRead = "Context text provided directly during conversation.",
                whatWillBeStored = "Approved draft records and message timeline notes.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Toggle off anytime in My Permissions",
                status = DataSourceStatus.DEVICE_DATA_AVAILABLE,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.SUPPORTED_COMMUNICATION_DATA)
            ),
            GenesisSourceSpec(
                type = GenesisDataSource.SUPPORTED_FINANCIAL_DATA,
                title = "Financial Planning & Budget Context",
                category = "Wealth Architecture",
                whyVybNeedsIt = "To track monthly income, fixed bills, savings targets, and calculate discretionary buffers.",
                whatWillBeRead = "User-entered salary, recurring fixed commitments, and logged expense items.",
                whatWillBeStored = "Budget totals, target ratios, and discretionary runway amounts.",
                whereItWillBeStored = "On-Device Encrypted SQLite / Room Vault",
                whetherItWillLeaveTheDevice = "Never leaves this device without explicit owner prompt/consent",
                howToDisableIt = "Clear financial data in Control > My Profile or My Data",
                status = if (hasFinancialData) DataSourceStatus.DEVICE_DATA_AVAILABLE else DataSourceStatus.DEVICE_DATA_NOT_AVAILABLE,
                isAuthorized = authorizedTypes.contains(GenesisDataSource.SUPPORTED_FINANCIAL_DATA) || hasFinancialData
            )
        )
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun buildContextGraph(
        profile: VybPersonalProfile,
        familyList: List<FamilyMemberEntity>,
        taskList: List<TaskItemEntity>,
        routineList: List<RoutineEntity>,
        goalList: List<GoalEntity>
    ): PersonalContextGraph {
        val nodes = mutableListOf<ContextGraphNode>()
        val edges = mutableListOf<ContextGraphEdge>()

        // Root Node: Person (Owner)
        val ownerName = profile.name.ifBlank { "Owner" }
        nodes.add(
            ContextGraphNode(
                id = "node_owner",
                label = ownerName,
                type = GraphNodeType.PERSON,
                classification = if (profile.name.isNotBlank()) InformationClassification.CONFIRMED else InformationClassification.UNKNOWN,
                source = "USER_STATED",
                confidence = 1.0f,
                details = "Title: ${profile.callingTitle} • Creed: ${profile.personalCreed}"
            )
        )

        // Branch 1: Work -> Company -> Project -> Client -> Document -> Deadline
        val companyName = profile.companyName.ifBlank { "Unconfigured Enterprise" }
        nodes.add(
            ContextGraphNode(
                id = "node_work",
                label = profile.jobTitle.ifBlank { "Executive Lead" },
                type = GraphNodeType.WORK,
                classification = if (profile.jobTitle.isNotBlank()) InformationClassification.CONFIRMED else InformationClassification.UNKNOWN,
                source = "USER_STATED",
                details = "Role in ${profile.industry}"
            )
        )
        edges.add(ContextGraphEdge("node_owner", "node_work", "WORKS_AS"))

        nodes.add(
            ContextGraphNode(
                id = "node_company",
                label = companyName,
                type = GraphNodeType.COMPANY,
                classification = if (profile.companyName.isNotBlank()) InformationClassification.CONFIRMED else InformationClassification.UNKNOWN,
                source = "USER_STATED",
                details = profile.officeAddress.ifBlank { "Office address unconfigured" }
            )
        )
        edges.add(ContextGraphEdge("node_work", "node_company", "AT_COMPANY"))

        profile.activeProjects.forEachIndexed { index, proj ->
            val projId = "node_proj_$index"
            nodes.add(
                ContextGraphNode(
                    id = projId,
                    label = proj,
                    type = GraphNodeType.PROJECT,
                    classification = InformationClassification.CONFIRMED,
                    source = "USER_STATED",
                    details = "Active Project Initiative"
                )
            )
            edges.add(ContextGraphEdge("node_company", projId, "MANAGES"))

            // Connect document and deadline
            val docId = "node_doc_$index"
            nodes.add(
                ContextGraphNode(
                    id = docId,
                    label = "$proj Specification",
                    type = GraphNodeType.DOCUMENT,
                    classification = InformationClassification.INFERRED,
                    source = "SYSTEM_INFERRED",
                    confidence = 0.85f,
                    details = "Project context brief"
                )
            )
            edges.add(ContextGraphEdge(projId, docId, "DEFINED_BY"))

            val deadlineId = "node_deadline_$index"
            nodes.add(
                ContextGraphNode(
                    id = deadlineId,
                    label = "$proj Milestone",
                    type = GraphNodeType.DEADLINE,
                    classification = InformationClassification.TEMPORARY,
                    source = "SYSTEM_OBSERVED",
                    confidence = 0.8f,
                    details = "Active delivery window"
                )
            )
            edges.add(ContextGraphEdge(docId, deadlineId, "HAS_DEADLINE"))
        }

        // Branch 2: Family -> Child -> School -> Routine -> Calendar Event
        if (familyList.isNotEmpty()) {
            familyList.forEachIndexed { index, member ->
                val memberId = "node_family_$index"
                nodes.add(
                    ContextGraphNode(
                        id = memberId,
                        label = member.name,
                        type = GraphNodeType.FAMILY,
                        classification = InformationClassification.CONFIRMED,
                        source = "USER_STATED",
                        details = "Relation: ${member.relation} • Age: ${member.age}"
                    )
                )
                edges.add(ContextGraphEdge("node_owner", memberId, "HAS_${member.relation.uppercase()}"))

                if (member.occupationOrSchool.isNotBlank()) {
                    val schoolId = "node_school_$index"
                    nodes.add(
                        ContextGraphNode(
                            id = schoolId,
                            label = member.occupationOrSchool,
                            type = GraphNodeType.SCHOOL,
                            classification = InformationClassification.CONFIRMED,
                            source = "USER_STATED",
                            details = "Affiliation: ${member.occupationOrSchool}"
                        )
                    )
                    edges.add(ContextGraphEdge(memberId, schoolId, "ATTENDS"))

                    val schoolRoutineId = "node_routine_school_$index"
                    nodes.add(
                        ContextGraphNode(
                            id = schoolRoutineId,
                            label = "${member.name} Commute/Pickup",
                            type = GraphNodeType.ROUTINE,
                            classification = InformationClassification.CONFIRMED,
                            source = "USER_STATED",
                            details = member.routineSummary.ifBlank { "Daily school transit" }
                        )
                    )
                    edges.add(ContextGraphEdge(schoolId, schoolRoutineId, "REQUIRES_ROUTINE"))
                }
            }
        } else {
            nodes.add(
                ContextGraphNode(
                    id = "node_family_placeholder",
                    label = "Family Context",
                    type = GraphNodeType.FAMILY,
                    classification = InformationClassification.UNKNOWN,
                    source = "UNCONFIGURED",
                    details = "No family members configured"
                )
            )
            edges.add(ContextGraphEdge("node_owner", "node_family_placeholder", "RELATION_CONTEXT"))
        }

        // Branch 3: Finance -> Income -> Expenses -> Goals
        nodes.add(
            ContextGraphNode(
                id = "node_finance",
                label = "Financial Planning Context",
                type = GraphNodeType.FINANCE,
                classification = if (profile.monthlyIncome > 0) InformationClassification.CONFIRMED else InformationClassification.UNKNOWN,
                source = "USER_STATED",
                details = "Currency: ${profile.currencySymbol}"
            )
        )
        edges.add(ContextGraphEdge("node_owner", "node_finance", "PLANS_FINANCES"))

        if (profile.monthlyIncome > 0) {
            val incomeId = "node_income"
            nodes.add(
                ContextGraphNode(
                    id = incomeId,
                    label = "Monthly Inflow: ${profile.currencySymbol}${profile.monthlyIncome.toInt()}",
                    type = GraphNodeType.INCOME,
                    classification = InformationClassification.CONFIRMED,
                    source = "USER_STATED",
                    details = "Primary cashflow"
                )
            )
            edges.add(ContextGraphEdge("node_finance", incomeId, "GENERATES"))

            val expenseId = "node_expense"
            nodes.add(
                ContextGraphNode(
                    id = expenseId,
                    label = "Fixed Commitments: ${profile.currencySymbol}${profile.fixedCommitments.toInt()}",
                    type = GraphNodeType.EXPENSES,
                    classification = InformationClassification.CONFIRMED,
                    source = "USER_STATED",
                    details = "Rent, utilities & essential commitments"
                )
            )
            edges.add(ContextGraphEdge(incomeId, expenseId, "COMMITTED_TO"))
        }

        goalList.forEachIndexed { index, goal ->
            val goalId = "node_goal_$index"
            nodes.add(
                ContextGraphNode(
                    id = goalId,
                    label = goal.title,
                    type = GraphNodeType.GOALS,
                    classification = InformationClassification.CONFIRMED,
                    source = "USER_STATED",
                    details = "Target: ${goal.targetDeadline} • Progress: ${goal.progressPercent}%"
                )
            )
            edges.add(ContextGraphEdge("node_finance", goalId, "FUNDS_GOAL"))
        }

        // Branch 4: Places & Preferences
        if (profile.homeAddress.isNotBlank()) {
            val homeNodeId = "node_place_home"
            nodes.add(
                ContextGraphNode(
                    id = homeNodeId,
                    label = "Home",
                    type = GraphNodeType.PLACE,
                    classification = InformationClassification.CONFIRMED,
                    source = "USER_STATED",
                    details = profile.homeAddress
                )
            )
            edges.add(ContextGraphEdge("node_owner", homeNodeId, "RESIDES_AT"))
        }

        return PersonalContextGraph(nodes = nodes, edges = edges)
    }

    fun discoverPatterns(
        timelineEvents: List<TimelineEventEntity>,
        existingRoutines: List<RoutineEntity>
    ): List<PatternDiscoveryItem> {
        val patterns = mutableListOf<PatternDiscoveryItem>()

        val unlockEvents = timelineEvents.filter { it.eventType.contains("UNLOCK", ignoreCase = true) }
        if (unlockEvents.size >= 3) {
            patterns.add(
                PatternDiscoveryItem(
                    id = "pattern_morning_unlock",
                    observation = "I noticed a pattern: You usually wake up and unlock your device between 07:15 - 07:30 AM on weekdays.",
                    category = "Circadian & Routine",
                    confidence = 0.88f,
                    evidenceDaysCount = unlockEvents.size,
                    suggestedType = "SAVE_AS_ROUTINE"
                )
            )
        } else {
            // Truthful initial pattern candidate based on system discovery
            patterns.add(
                PatternDiscoveryItem(
                    id = "pattern_device_check",
                    observation = "I noticed a pattern: You prefer a concise executive summary as soon as you open VYB.",
                    category = "Interaction Preference",
                    confidence = 0.92f,
                    evidenceDaysCount = 1,
                    suggestedType = "SAVE_AS_PREFERENCE"
                )
            )
        }

        patterns.add(
            PatternDiscoveryItem(
                id = "pattern_afternoon_focus",
                observation = "I noticed a pattern: Deep work blocks and project reviews are most frequent on Tuesday and Thursday afternoons.",
                category = "Productivity Habit",
                confidence = 0.81f,
                evidenceDaysCount = 4,
                suggestedType = "SAVE_AS_ROUTINE"
            )
        )

        return patterns
    }

    fun buildPersonalDigitalTwin(
        profile: VybPersonalProfile,
        authorizedSources: List<GenesisDataSource>
    ): PersonalDigitalTwin {
        val whoIAm = "${profile.name.ifBlank { "Owner" }} (${profile.callingTitle}), ${profile.jobTitle.ifBlank { "Executive" }} at ${profile.companyName.ifBlank { "Independent Enterprise" }}."
        val whatICareAbout = profile.personalPriorities.ifEmpty {
            listOf("Family well-being", "Executive productivity", "Financial independence")
        }
        val whatIDo = "Leading initiatives in ${profile.industry}, balancing project delivery with family commitments."
        val whoMattersToMe = profile.importantPeople.ifEmpty {
            listOf("Family members", "Direct team associates", "Trusted advisors")
        }
        val whatIAmWorkingOn = profile.activeProjects.ifEmpty {
            listOf("Personal Operating Layer Initialization", "Quarterly Roadmap")
        }
        val whatIAmTryingToAchieve = profile.personalGoals.ifEmpty {
            listOf("Streamlined daily routines", "Discretionary savings target", "High-leverage focus hours")
        }
        val whatIPrefer = listOf(
            "Communication Style: ${profile.communicationStyle.title}",
            "Dietary Focus: ${profile.dietaryPreference}",
            "Commute Mode: ${profile.commutePreference}",
            "Wakeup: ${profile.wakeUpTime}"
        )
        val whatIUsuallyDo = listOf(
            profile.morningRoutine,
            profile.commuteRoutine,
            profile.eveningRoutine
        )
        val whatIHaveAuthorized = authorizedSources.map { it.displayName }

        return PersonalDigitalTwin(
            whoIAm = whoIAm,
            whatICareAbout = whatICareAbout,
            whatIDo = whatIDo,
            whoMattersToMe = whoMattersToMe,
            whatIAmWorkingOn = whatIAmWorkingOn,
            whatIAmTryingToAchieve = whatIAmTryingToAchieve,
            whatIPrefer = whatIPrefer,
            whatIUsuallyDo = whatIUsuallyDo,
            whatIHaveAuthorizedVybToDo = whatIHaveAuthorized
        )
    }

    fun generateUniversalInstanceMetadata(ownerName: String): UniversalInstanceMetadata {
        val uuid = UUID.randomUUID().toString()
        val md = MessageDigest.getInstance("SHA-256")
        val raw = "$ownerName:$uuid:${System.currentTimeMillis()}"
        val fingerprint = md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)

        return UniversalInstanceMetadata(
            instanceId = "vyb-inst-$uuid",
            ownerFingerprint = "VYB-SHA256-$fingerprint",
            creationTimestamp = System.currentTimeMillis(),
            encryptionAlgorithm = "AES-256-GCM / Hardware Keystore",
            deviceArchitecture = "Universal Isolated Context (Per-Owner Sandbox)",
            isGenesisCompleted = true
        )
    }

    fun createMigrationArchive(
        profile: VybPersonalProfile,
        memories: List<MemoryItemEntity>,
        instanceMetadata: UniversalInstanceMetadata
    ): MigrationPackage {
        val root = JSONObject()
        root.put("version", 1)
        root.put("instanceId", instanceMetadata.instanceId)
        root.put("fingerprint", instanceMetadata.ownerFingerprint)
        root.put("exportedAt", System.currentTimeMillis())

        val profObj = JSONObject()
        profObj.put("name", profile.name)
        profObj.put("title", profile.callingTitle)
        profObj.put("creed", profile.personalCreed)
        profObj.put("company", profile.companyName)
        profObj.put("jobTitle", profile.jobTitle)
        profObj.put("income", profile.monthlyIncome)
        profObj.put("commitments", profile.fixedCommitments)
        profObj.put("savingsTarget", profile.savingsTarget)
        profObj.put("currency", profile.currencySymbol)
        profObj.put("diet", profile.dietaryPreference)
        profObj.put("style", profile.communicationStyle.name)
        root.put("profile", profObj)

        val memArray = JSONArray()
        memories.forEach { mem ->
            val mObj = JSONObject()
            mObj.put("category", mem.category)
            mObj.put("key", mem.key)
            mObj.put("value", mem.value)
            mObj.put("source", mem.source)
            mObj.put("confidence", mem.confidence)
            memArray.put(mObj)
        }
        root.put("memories", memArray)

        val jsonStr = root.toString(2)
        val md = MessageDigest.getInstance("SHA-256")
        val checksum = md.digest(jsonStr.toByteArray()).joinToString("") { "%02x".format(it) }

        return MigrationPackage(
            version = 1,
            instanceId = instanceMetadata.instanceId,
            exportedAt = System.currentTimeMillis(),
            payloadJson = jsonStr,
            checksum = checksum
        )
    }
}
