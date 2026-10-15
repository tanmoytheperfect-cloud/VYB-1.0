package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GoalEntity
import com.example.data.local.entity.MemoryItemEntity
import com.example.data.local.entity.RoutineEntity
import com.example.data.local.entity.TaskItemEntity
import com.example.data.local.entity.TimelineEventEntity
import com.example.domain.genesis.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VybPersonalGenesisEngineTest {

    private lateinit var context: Context
    private lateinit var genesisEngine: PersonalGenesisEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        genesisEngine = PersonalGenesisEngine(context)
    }

    @Test
    fun `test 1 all 14 data sources have mandatory transparency disclosures`() {
        val specs = genesisEngine.getInitialSourceSpecs(
            ownerName = "Tanmoy",
            hasFinancialData = true,
            hasConnectedAccounts = false,
            authorizedTypes = setOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE)
        )

        assertEquals(14, specs.size)

        specs.forEach { spec ->
            assertTrue("whyVybNeedsIt must not be blank for ${spec.type}", spec.whyVybNeedsIt.isNotBlank())
            assertTrue("whatWillBeRead must not be blank for ${spec.type}", spec.whatWillBeRead.isNotBlank())
            assertTrue("whatWillBeStored must not be blank for ${spec.type}", spec.whatWillBeStored.isNotBlank())
            assertTrue("whereItWillBeStored must contain on-device or vault for ${spec.type}", spec.whereItWillBeStored.isNotBlank())
            assertTrue("whetherItWillLeaveTheDevice must reassure on-device security for ${spec.type}", spec.whetherItWillLeaveTheDevice.contains("Never leaves this device", ignoreCase = true))
            assertTrue("howToDisableIt must provide clear guidance for ${spec.type}", spec.howToDisableIt.isNotBlank())
        }

        // Verify status distinguishes truth correctly
        val deviceSpec = specs.first { it.type == GenesisDataSource.DEVICE }
        assertEquals(DataSourceStatus.DEVICE_DATA_AVAILABLE, deviceSpec.status)

        val accountSpec = specs.first { it.type == GenesisDataSource.SUPPORTED_ACCOUNTS }
        assertEquals(DataSourceStatus.SERVICE_NOT_CONNECTED, accountSpec.status)
    }

    @Test
    fun `test 2 context graph constructs relational chains and classifies information`() {
        val profile = VybPersonalProfile(
            name = "Tanmoy",
            callingTitle = "Sir",
            companyName = "Metropoli Fashions",
            jobTitle = "Managing Director",
            industry = "Apparel & Retail",
            officeAddress = "Park Street, Kolkata",
            monthlyIncome = 150000.0,
            fixedCommitments = 60000.0,
            savingsTarget = 40000.0,
            currencySymbol = "₹",
            activeProjects = listOf("Spring Collection", "Inventory Automation")
        )

        val family = listOf(
            FamilyMemberEntity(
                name = "Aarav",
                relation = "Son",
                age = 8,
                occupationOrSchool = "St. Xavier's Collegiate",
                routineSummary = "Morning drop at 07:30"
            )
        )

        val tasks = listOf(
            TaskItemEntity(title = "Review Fabric Specs", priority = "HIGH", category = "WORK")
        )

        val routines = listOf(
            RoutineEntity(title = "Morning Transit", timeOfDay = "08:30 AM", daysOfWeek = "Mon-Fri", description = "Metro transit", actionPrompt = "Book ride to office")
        )

        val goals = listOf(
            GoalEntity(title = "Save ₹5 Lakhs Discretionary Buffer", category = "FINANCE", targetValue = "500000", currentValue = "200000", targetDeadline = "2026-12-31")
        )

        val graph = genesisEngine.buildContextGraph(
            profile = profile,
            familyList = family,
            taskList = tasks,
            routineList = routines,
            goalList = goals
        )

        assertTrue(graph.nodes.isNotEmpty())
        assertTrue(graph.edges.isNotEmpty())

        // Verify Person node
        val ownerNode = graph.nodes.first { it.id == "node_owner" }
        assertEquals("Tanmoy", ownerNode.label)
        assertEquals(InformationClassification.CONFIRMED, ownerNode.classification)

        // Verify Work -> Company chain
        assertTrue(graph.edges.any { it.fromId == "node_owner" && it.toId == "node_work" })
        assertTrue(graph.edges.any { it.fromId == "node_work" && it.toId == "node_company" })

        // Verify Family -> School chain
        val childNode = graph.nodes.firstOrNull { it.label == "Aarav" }
        assertNotNull(childNode)
        assertEquals(InformationClassification.CONFIRMED, childNode?.classification)
        assertTrue(graph.edges.any { it.fromId == "node_owner" && it.toId == "node_family_0" })
        assertTrue(graph.edges.any { it.fromId == "node_family_0" && it.toId == "node_school_0" })

        // Verify Finance -> Income -> Expenses -> Goals chain
        val financeNode = graph.nodes.first { it.id == "node_finance" }
        assertEquals(InformationClassification.CONFIRMED, financeNode.classification)
        assertTrue(graph.edges.any { it.fromId == "node_finance" && it.toId == "node_income" })
        assertTrue(graph.edges.any { it.fromId == "node_income" && it.toId == "node_expense" })
        assertTrue(graph.edges.any { it.fromId == "node_finance" && it.toId == "node_goal_0" })
    }

    @Test
    fun `test 3 pattern discovery presents observations rather than rigid rules`() {
        val events = listOf(
            TimelineEventEntity(timestamp = System.currentTimeMillis(), eventType = "DEVICE_UNLOCK", source = "SYS", contextData = "Unlocked", dayOfWeek = 2, dateString = "2026-09-22"),
            TimelineEventEntity(timestamp = System.currentTimeMillis(), eventType = "DEVICE_UNLOCK", source = "SYS", contextData = "Unlocked", dayOfWeek = 3, dateString = "2026-09-23"),
            TimelineEventEntity(timestamp = System.currentTimeMillis(), eventType = "DEVICE_UNLOCK", source = "SYS", contextData = "Unlocked", dayOfWeek = 4, dateString = "2026-09-24")
        )

        val patterns = genesisEngine.discoverPatterns(events, emptyList())
        assertTrue(patterns.isNotEmpty())

        patterns.forEach { pattern ->
            assertTrue(
                "Pattern observation must begin with respectful observation phrase",
                pattern.observation.startsWith("I noticed a pattern")
            )
            assertFalse(pattern.isConfirmed)
            assertFalse(pattern.isDismissed)
            assertTrue(pattern.suggestedType == "SAVE_AS_ROUTINE" || pattern.suggestedType == "SAVE_AS_PREFERENCE")
        }
    }

    @Test
    fun `test 4 personal digital twin model captures 9 facets truthfully`() {
        val profile = VybPersonalProfile(
            name = "Tanmoy",
            callingTitle = "Sir",
            companyName = "Metropoli",
            jobTitle = "Founder",
            personalCreed = "Simplicity in execution",
            monthlyIncome = 120000.0,
            activeProjects = listOf("AI Studio Launch"),
            personalPriorities = listOf("Family focus", "Business growth")
        )

        val twin = genesisEngine.buildPersonalDigitalTwin(
            profile = profile,
            authorizedSources = listOf(GenesisDataSource.IDENTITY, GenesisDataSource.DEVICE, GenesisDataSource.CALENDAR)
        )

        assertTrue(twin.whoIAm.contains("Tanmoy"))
        assertTrue(twin.whatICareAbout.contains("Family focus"))
        assertTrue(twin.whatIAmWorkingOn.contains("AI Studio Launch"))
        assertTrue(twin.whatIHaveAuthorizedVybToDo.contains(GenesisDataSource.CALENDAR.displayName))
    }

    @Test
    fun `test 5 universal instance metadata and migration archive serialization`() {
        val metadata = genesisEngine.generateUniversalInstanceMetadata("Tanmoy")
        assertTrue(metadata.instanceId.startsWith("vyb-inst-"))
        assertTrue(metadata.ownerFingerprint.startsWith("VYB-SHA256-"))
        assertEquals("AES-256-GCM / Hardware Keystore", metadata.encryptionAlgorithm)

        val profile = VybPersonalProfile(name = "Tanmoy", companyName = "Metropoli")
        val memories = listOf(
            MemoryItemEntity(category = "PREFERENCE", key = "beverage", value = "Green tea")
        )

        val archive = genesisEngine.createMigrationArchive(profile, memories, metadata)
        assertEquals(1, archive.version)
        assertEquals(metadata.instanceId, archive.instanceId)
        assertTrue(archive.payloadJson.contains("Tanmoy"))
        assertTrue(archive.payloadJson.contains("Green tea"))
        assertTrue(archive.checksum.isNotBlank())
    }
}
