package com.example.domain.brain

enum class GraphNodeType {
    PERSON,
    FAMILY,
    WORK,
    HOME,
    ROUTINES,
    PLACES,
    SERVICES,
    ACCOUNTS,
    FINANCES,
    TASKS,
    GOALS,
    PREFERENCES,
    DECISIONS,
    HABITS,
    EVENTS,
    PURCHASES,
    TRAVEL,
    HEALTH_LIFESTYLE
}

data class LifeGraphNode(
    val id: String,
    val type: GraphNodeType,
    val name: String,
    val properties: Map<String, Any?> = emptyMap(),
    val confidence: Double = 1.0,
    val isUserVerified: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class LifeGraphEdge(
    val fromNodeId: String,
    val toNodeId: String,
    val relationship: String, // e.g. "HAS_CHILD", "WORKS_AT", "LOCATED_IN", "PREFERS"
    val weight: Double = 1.0
)

/**
 * Transparent, editable, private Personal Life Graph.
 */
class PersonalLifeGraph {
    private val nodes = mutableMapOf<String, LifeGraphNode>()
    private val edges = mutableListOf<LifeGraphEdge>()

    init {
        initializeTanmoyGraph()
    }

    private fun initializeTanmoyGraph() {
        // Root Person
        addNode(LifeGraphNode("person_tanmoy", GraphNodeType.PERSON, "Tanmoy", mapOf("role" to "Primary Owner")))

        // Family
        addNode(LifeGraphNode("family_root", GraphNodeType.FAMILY, "Family", emptyMap()))
        addEdge("person_tanmoy", "family_root", "HAS_FAMILY")

        addNode(LifeGraphNode("family_vivaan", GraphNodeType.FAMILY, "Vivaan", mapOf(
            "relation" to "Son",
            "school" to "The Heritage School",
            "school_start" to "08:30 AM",
            "school_pickup" to "10:30 AM"
        )))
        addEdge("family_root", "family_vivaan", "CHILD")

        addNode(LifeGraphNode("family_wife", GraphNodeType.FAMILY, "Wife", mapOf("relation" to "Spouse")))
        addEdge("family_root", "family_wife", "SPOUSE")

        // Work
        addNode(LifeGraphNode("work_metropoli", GraphNodeType.WORK, "Metropoli Fashions", mapOf(
            "role" to "Owner / Entrepreneur",
            "commute_time" to "10:00 AM"
        )))
        addEdge("person_tanmoy", "work_metropoli", "WORKS_AT")

        // Finances
        addNode(LifeGraphNode("finances_root", GraphNodeType.FINANCES, "Personal & Business Finances", mapOf(
            "monthly_income" to 85000.0,
            "savings_target" to 25000.0,
            "discretionary_buffer" to 20000.0,
            "fixed_expenses" to 40000.0
        )))
        addEdge("person_tanmoy", "finances_root", "MANAGES_FINANCES")

        // Preferences
        addNode(LifeGraphNode("pref_clothing", GraphNodeType.PREFERENCES, "Clothing Preferences", mapOf(
            "fabric" to "100% Linen / Organic Cotton",
            "style" to "Minimalist tailored, earth tones and navy"
        )))
        addEdge("person_tanmoy", "pref_clothing", "PREFERS")

        addNode(LifeGraphNode("pref_transport", GraphNodeType.PREFERENCES, "Transport Preferences", mapOf(
            "priority" to "Cheapest reliable option",
            "top_choices" to listOf("Yatri Sathi", "Rapido", "Uber")
        )))
        addEdge("person_tanmoy", "pref_transport", "PREFERS")

        // Routines
        addNode(LifeGraphNode("routine_school_drop", GraphNodeType.ROUTINES, "Morning School Transport", mapOf(
            "time" to "08:15 AM - 08:30 AM",
            "passenger" to "Vivaan",
            "destination" to "The Heritage School"
        )))
        addEdge("family_vivaan", "routine_school_drop", "DAILY_ROUTINE")

        addNode(LifeGraphNode("routine_school_pickup", GraphNodeType.ROUTINES, "Morning School Pickup", mapOf(
            "time" to "10:30 AM",
            "passenger" to "Vivaan"
        )))
        addEdge("family_vivaan", "routine_school_pickup", "DAILY_ROUTINE")

        // Services
        addNode(LifeGraphNode("service_amazon", GraphNodeType.SERVICES, "Amazon", mapOf("connected" to false)))
        addEdge("person_tanmoy", "service_amazon", "USES_SERVICE")
    }

    fun addNode(node: LifeGraphNode) {
        nodes[node.id] = node
    }

    fun addEdge(fromId: String, toId: String, relation: String) {
        edges.add(LifeGraphEdge(fromId, toId, relation))
    }

    fun getNode(id: String): LifeGraphNode? = nodes[id]

    fun getAllNodes(): List<LifeGraphNode> = nodes.values.toList()

    fun getEdgesFrom(nodeId: String): List<LifeGraphEdge> = edges.filter { it.fromNodeId == nodeId }

    fun removeNode(id: String) {
        nodes.remove(id)
        edges.removeAll { it.fromNodeId == id || it.toNodeId == id }
    }
}
