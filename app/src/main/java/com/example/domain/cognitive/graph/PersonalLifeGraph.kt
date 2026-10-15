package com.example.domain.cognitive.graph

import com.example.domain.cognitive.models.TruthState

data class LifeGraphNode(
    val id: String,
    val category: String, // OWNER, FAMILY, WORK, HOME, FINANCE, TRANSPORT, GOALS, ROUTINES, EDUCATION
    val label: String,
    val properties: Map<String, String> = emptyMap(),
    val truthState: TruthState = TruthState.KNOWN
)

data class LifeGraphEdge(
    val sourceId: String,
    val targetId: String,
    val relation: String, // PARENT_OF, CHILD_OF, SPOUSE_OF, ATTENDS, EMPLOYED_AT, LOCATED_AT, FOLLOWS_ROUTINE, HAS_GOAL, HAS_EXPENSE, HAS_MEMBER
    val confidence: Float = 1.0f,
    val truthState: TruthState = TruthState.KNOWN
)

/**
 * Canonical Personal Life Graph.
 * Holds typed hierarchical and relational knowledge about the owner, family, routines, and life contexts.
 */
class PersonalLifeGraph {

    private val nodes = mutableMapOf<String, LifeGraphNode>()
    private val edges = mutableListOf<LifeGraphEdge>()

    init {
        // Initialize root life categories
        addNode(LifeGraphNode("node_owner", "OWNER", "Owner", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_family", "FAMILY", "Family", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_work", "WORK", "Work", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_home", "HOME", "Home", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_finance", "FINANCE", "Finance", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_transport", "TRANSPORT", "Transport", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_goals", "GOALS", "Goals", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_routines", "ROUTINES", "Routines", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_activity", "ACTIVITY", "Activity", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_places", "PLACES", "Places", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_environment", "ENVIRONMENT", "Environment", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_health", "HEALTH", "Health", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_travel", "TRAVEL", "Travel", truthState = TruthState.KNOWN))
        addNode(LifeGraphNode("node_baselines", "BASELINES", "Personal Baselines", truthState = TruthState.KNOWN))

        // Connect root domains to owner
        listOf(
            "node_family", "node_work", "node_home", "node_finance",
            "node_transport", "node_goals", "node_routines", "node_activity",
            "node_places", "node_environment", "node_health", "node_travel", "node_baselines"
        ).forEach {
            addEdge(LifeGraphEdge("node_owner", it, "HAS_DOMAIN"))
        }
    }

    @Synchronized
    fun addNode(node: LifeGraphNode) {
        nodes[node.id] = node
    }

    @Synchronized
    fun getNode(id: String): LifeGraphNode? = nodes[id]

    @Synchronized
    fun getAllNodes(): List<LifeGraphNode> = nodes.values.toList()

    @Synchronized
    fun addEdge(edge: LifeGraphEdge) {
        // Remove existing edge with same source, target, and relation if present
        edges.removeAll { it.sourceId == edge.sourceId && it.targetId == edge.targetId && it.relation == edge.relation }
        edges.add(edge)
    }

    @Synchronized
    fun getAllEdges(): List<LifeGraphEdge> = edges.toList()

    @Synchronized
    fun getEdgesFrom(sourceId: String): List<LifeGraphEdge> = edges.filter { it.sourceId == sourceId }

    @Synchronized
    fun getEdgesTo(targetId: String): List<LifeGraphEdge> = edges.filter { it.targetId == targetId }

    @Synchronized
    fun addFamilyMember(name: String, relation: String, occupationOrSchool: String = "", truthState: TruthState = TruthState.USER_CONFIRMED): String {
        val memberId = "family_${name.lowercase().trim().replace(" ", "_")}"
        addNode(
            LifeGraphNode(
                id = memberId,
                category = "FAMILY",
                label = name,
                properties = mapOf(
                    "relation" to relation,
                    "occupationOrSchool" to occupationOrSchool
                ),
                truthState = truthState
            )
        )
        addEdge(LifeGraphEdge("node_family", memberId, "HAS_MEMBER", truthState = truthState))
        addEdge(LifeGraphEdge("node_owner", memberId, relation.uppercase(), truthState = truthState))

        if (occupationOrSchool.isNotBlank()) {
            val schoolId = "school_${occupationOrSchool.lowercase().trim().replace(" ", "_")}"
            addNode(
                LifeGraphNode(
                    id = schoolId,
                    category = "EDUCATION",
                    label = occupationOrSchool,
                    truthState = truthState
                )
            )
            addEdge(LifeGraphEdge(memberId, schoolId, "ATTENDS", truthState = truthState))
        }
        return memberId
    }

    @Synchronized
    fun attachRoutineToEntity(entityId: String, routineTitle: String, timeWindow: String, recurrence: String): String {
        val routineId = "routine_${routineTitle.lowercase().trim().replace(" ", "_")}"
        addNode(
            LifeGraphNode(
                id = routineId,
                category = "ROUTINES",
                label = routineTitle,
                properties = mapOf(
                    "timeWindow" to timeWindow,
                    "recurrence" to recurrence
                ),
                truthState = TruthState.LEARNED
            )
        )
        addEdge(LifeGraphEdge(entityId, routineId, "FOLLOWS_ROUTINE", truthState = TruthState.LEARNED))
        addEdge(LifeGraphEdge("node_routines", routineId, "CONTAINS_ROUTINE", truthState = TruthState.LEARNED))
        return routineId
    }

    @Synchronized
    fun getFamilyMembers(): List<LifeGraphNode> {
        val memberIds = getEdgesFrom("node_family").map { it.targetId }.toSet()
        return nodes.values.filter { it.id in memberIds }
    }

    @Synchronized
    fun getRoutinesForEntity(entityId: String): List<LifeGraphNode> {
        val routineIds = getEdgesFrom(entityId).filter { it.relation == "FOLLOWS_ROUTINE" }.map { it.targetId }.toSet()
        return nodes.values.filter { it.id in routineIds }
    }

    @Synchronized
    fun findEntityByName(name: String): LifeGraphNode? {
        val target = name.lowercase().trim()
        return nodes.values.firstOrNull { it.label.lowercase().trim() == target || it.id.contains(target) }
    }

    @Synchronized
    fun addActivityEvidence(
        activityType: String,
        speedKmh: Double = 0.0,
        steps: Int = 0,
        truthState: TruthState = TruthState.DETECTED
    ): String {
        val actId = "activity_${activityType.lowercase().trim()}"
        addNode(
            LifeGraphNode(
                id = actId,
                category = "ACTIVITY",
                label = activityType,
                properties = mapOf(
                    "speedKmh" to speedKmh.toString(),
                    "steps" to steps.toString(),
                    "timestamp" to System.currentTimeMillis().toString()
                ),
                truthState = truthState
            )
        )
        addEdge(LifeGraphEdge("node_activity", actId, "HAS_ACTIVITY", truthState = truthState))
        addEdge(LifeGraphEdge("node_owner", actId, "CURRENTLY_DOING", truthState = truthState))
        return actId
    }

    @Synchronized
    fun addPlaceEvidence(
        placeType: String,
        label: String,
        truthState: TruthState = TruthState.KNOWN
    ): String {
        val placeId = "place_${placeType.lowercase().trim()}"
        addNode(
            LifeGraphNode(
                id = placeId,
                category = "PLACES",
                label = label,
                properties = mapOf("placeType" to placeType),
                truthState = truthState
            )
        )
        addEdge(LifeGraphEdge("node_places", placeId, "KNOWN_PLACE", truthState = truthState))
        addEdge(LifeGraphEdge("node_owner", placeId, "CURRENTLY_AT", truthState = truthState))
        return placeId
    }

    @Synchronized
    fun addTravelEvidence(
        origin: String,
        destination: String,
        mode: String,
        speedKmh: Double,
        truthState: TruthState = TruthState.KNOWN
    ): String {
        val travelId = "travel_${System.currentTimeMillis()}"
        addNode(
            LifeGraphNode(
                id = travelId,
                category = "TRAVEL",
                label = "Trip to $destination",
                properties = mapOf(
                    "origin" to origin,
                    "destination" to destination,
                    "mode" to mode,
                    "speedKmh" to speedKmh.toString()
                ),
                truthState = truthState
            )
        )
        addEdge(LifeGraphEdge("node_travel", travelId, "HAS_TRIP", truthState = truthState))
        addEdge(LifeGraphEdge("node_owner", travelId, "EN_ROUTE", truthState = truthState))
        return travelId
    }

    @Synchronized
    fun addEnvironmentEvidence(
        aqi: Int,
        aqiLevel: String,
        tempC: Double,
        truthState: TruthState = TruthState.KNOWN
    ): String {
        val envId = "env_current"
        addNode(
            LifeGraphNode(
                id = envId,
                category = "ENVIRONMENT",
                label = "Air & Atmosphere",
                properties = mapOf(
                    "aqi" to aqi.toString(),
                    "level" to aqiLevel,
                    "tempC" to tempC.toString()
                ),
                truthState = truthState
            )
        )
        addEdge(LifeGraphEdge("node_environment", envId, "CURRENT_CONDITION", truthState = truthState))
        return envId
    }

    @Synchronized
    fun addBaselineEvidence(
        baselineType: String,
        expectedValue: String,
        confidence: Float
    ): String {
        val baseId = "baseline_${baselineType.lowercase().trim().replace(" ", "_")}"
        addNode(
            LifeGraphNode(
                id = baseId,
                category = "BASELINES",
                label = baselineType,
                properties = mapOf("expected" to expectedValue, "confidence" to confidence.toString()),
                truthState = TruthState.LEARNED
            )
        )
        addEdge(LifeGraphEdge("node_baselines", baseId, "LEARNED_BASELINE", confidence = confidence, truthState = TruthState.LEARNED))
        addEdge(LifeGraphEdge("node_owner", baseId, "PERSONAL_NORM", confidence = confidence, truthState = TruthState.LEARNED))
        return baseId
    }

    @Synchronized
    fun getActivities(): List<LifeGraphNode> {
        val actIds = getEdgesFrom("node_activity").map { it.targetId }.toSet()
        return nodes.values.filter { it.id in actIds }
    }

    @Synchronized
    fun getPlaces(): List<LifeGraphNode> {
        val placeIds = getEdgesFrom("node_places").map { it.targetId }.toSet()
        return nodes.values.filter { it.id in placeIds }
    }

    @Synchronized
    fun getBaselines(): List<LifeGraphNode> {
        val baseIds = getEdgesFrom("node_baselines").map { it.targetId }.toSet()
        return nodes.values.filter { it.id in baseIds }
    }

    @Synchronized
    fun clearDynamicEntities() {
        nodes.entries.removeIf { it.key.startsWith("family_") || it.key.startsWith("school_") || it.key.startsWith("routine_") || it.key.startsWith("activity_") || it.key.startsWith("travel_") }
        edges.removeIf { it.sourceId.startsWith("family_") || it.targetId.startsWith("family_") || it.sourceId.startsWith("school_") || it.targetId.startsWith("school_") || it.sourceId.startsWith("routine_") || it.targetId.startsWith("routine_") || it.sourceId.startsWith("activity_") || it.targetId.startsWith("activity_") || it.sourceId.startsWith("travel_") || it.targetId.startsWith("travel_") }
    }

    @Synchronized
    fun addRoutine(title: String, timeWindow: String, recurrence: String, truthState: TruthState = TruthState.USER_CONFIRMED): String {
        val routineId = "routine_${title.lowercase().trim().replace(' ', '_')}"
        addNode(
            LifeGraphNode(
                id = routineId,
                category = "ROUTINES",
                label = title,
                properties = mapOf(
                    "timeWindow" to timeWindow,
                    "recurrence" to recurrence
                ),
                truthState = truthState
            )
        )
        addEdge(LifeGraphEdge("node_routines", routineId, "CONTAINS_ROUTINE", truthState = truthState))
        return routineId
    }

    @Synchronized
    fun addGoal(title: String, targetProgress: Float = 1.0f, truthState: TruthState = TruthState.USER_CONFIRMED): String {
        val goalId = "goal_${title.lowercase().trim().replace(' ', '_')}"
        addNode(
            LifeGraphNode(
                id = goalId,
                category = "GOALS",
                label = title,
                properties = mapOf(
                    "progress" to targetProgress.toString()
                ),
                truthState = truthState
            )
        )
        addEdge(LifeGraphEdge("node_goals", goalId, "CONTAINS_GOAL", truthState = truthState))
        return goalId
    }

}
