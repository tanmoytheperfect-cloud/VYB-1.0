package com.example.domain.intelligence

import com.example.domain.genesis.*

/**
 * Outcome Learning Engine.
 * Observes the outcome of executed actions and user confirmations,
 * distilling verified facts and preferences into the Personal Context Graph
 * and Personal Profile.
 * Never silently turns unverified guesses into facts.
 */
class OutcomeLearningEngine {

    fun learnFromActionOutcome(
        intent: UniversalIntent,
        context: ResolvedPersonalContext,
        verification: VerificationResult,
        existingProfile: VybPersonalProfile,
        existingGraph: PersonalContextGraph
    ): Pair<VybPersonalProfile, PersonalContextGraph> {
        if (!verification.isVerified) {
            return Pair(existingProfile, existingGraph)
        }

        var updatedProfile = existingProfile
        val updatedNodes = existingGraph.nodes.toMutableList()
        val updatedEdges = existingGraph.edges.toMutableList()

        when (intent.intentType) {
            UniversalIntentType.BOOK_TRANSPORT -> {
                val service = context.contextualService
                val dest = context.contextualDestination
                if (!service.isNullOrBlank()) {
                    // Update commute preference if user confirmed
                    if (intent.status == IntentStatus.COMPLETED) {
                        updatedProfile = updatedProfile.copy(
                            commutePreference = "$service / Transit"
                        )
                    }
                    // Add/update node in context graph
                    val serviceNodeId = "service_${service.lowercase().replace(" ", "_")}"
                    if (updatedNodes.none { it.id == serviceNodeId }) {
                        updatedNodes.add(
                            ContextGraphNode(
                                id = serviceNodeId,
                                label = service,
                                type = GraphNodeType.SERVICE,
                                classification = InformationClassification.CONFIRMED,
                                source = "OUTCOME_LEARNING_VERIFIED",
                                confidence = 0.95f,
                                details = "Mobility dispatch provider verified through action execution"
                            )
                        )
                        updatedEdges.add(
                            ContextGraphEdge(
                                fromId = "owner_root",
                                toId = serviceNodeId,
                                relation = "USES_TRANSPORT_SERVICE",
                                classification = InformationClassification.CONFIRMED
                            )
                        )
                    }
                }

                if (!dest.isNullOrBlank()) {
                    val destNodeId = "dest_${dest.lowercase().replace(" ", "_").take(20)}"
                    if (updatedNodes.none { it.id == destNodeId }) {
                        updatedNodes.add(
                            ContextGraphNode(
                                id = destNodeId,
                                label = dest,
                                type = GraphNodeType.PLACE,
                                classification = InformationClassification.CONFIRMED,
                                source = "OUTCOME_LEARNING_VERIFIED",
                                confidence = 0.9f,
                                details = "Frequent destination verified through travel action"
                            )
                        )
                        updatedEdges.add(
                            ContextGraphEdge(
                                fromId = "owner_root",
                                toId = destNodeId,
                                relation = "FREQUENT_DESTINATION",
                                classification = InformationClassification.CONFIRMED
                            )
                        )
                    }
                }
            }

            UniversalIntentType.COMMUNICATION_DISPATCH -> {
                val contactName = context.relatedPeople.firstOrNull()
                if (!contactName.isNullOrBlank()) {
                    val contactNodeId = "person_${contactName.lowercase().replace(" ", "_")}"
                    if (updatedNodes.none { it.id == contactNodeId }) {
                        updatedNodes.add(
                            ContextGraphNode(
                                id = contactNodeId,
                                label = contactName,
                                type = GraphNodeType.PERSON,
                                classification = InformationClassification.CONFIRMED,
                                source = "OUTCOME_LEARNING_VERIFIED",
                                confidence = 0.95f,
                                details = "Verified communication partner"
                            )
                        )
                        updatedEdges.add(
                            ContextGraphEdge(
                                fromId = "owner_root",
                                toId = contactNodeId,
                                relation = "COMMUNICATES_WITH",
                                classification = InformationClassification.CONFIRMED
                            )
                        )
                    }
                }
            }

            else -> {
                // Keep existing graph intact
            }
        }

        return Pair(updatedProfile, PersonalContextGraph(updatedNodes, updatedEdges))
    }
}
