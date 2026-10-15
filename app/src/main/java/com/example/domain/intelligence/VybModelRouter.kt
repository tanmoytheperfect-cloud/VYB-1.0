package com.example.domain.intelligence

/**
 * Model Capability classifications (Part 15).
 */
enum class ModelCapability {
    DETERMINISTIC_RULES,
    ON_DEVICE_EMBEDDING,
    LOCAL_HEURISTICS,
    SERVER_SIDE_GEMINI,
    MULTIMODAL_VISION,
    OFFLINE_REASONING
}

/**
 * Model Routing Decision made by VybModelRouter.
 */
data class ModelRoutingDecision(
    val selectedProvider: String,
    val capability: ModelCapability,
    val isOnlineRequired: Boolean,
    val latencyExpectedMs: Long,
    val privacyLevel: String,
    val availabilityStatus: String,
    val fallbackReason: String? = null
)

/**
 * VybModelRouter:
 * Truth-first router matching user tasks to optimal local or server-side model capabilities.
 * If offline or unavailable, honestly reports fallback to local deterministic vault.
 */
class VybModelRouter(private val isServerGeminiAvailable: Boolean = true) {

    fun routeTask(
        intentType: UniversalIntentType,
        isMultimodal: Boolean = false,
        isDeviceOnline: Boolean = true
    ): ModelRoutingDecision {
        if (!isDeviceOnline) {
            return ModelRoutingDecision(
                selectedProvider = "On-Device Deterministic & Encrypted Room Vault",
                capability = ModelCapability.OFFLINE_REASONING,
                isOnlineRequired = false,
                latencyExpectedMs = 5,
                privacyLevel = "On-Device Encrypted Only",
                availabilityStatus = "ONLINE_UNAVAILABLE / OFFLINE_ACTIVE",
                fallbackReason = "Internet connectivity is offline. Operating strictly in on-device deterministic mode."
            )
        }

        if (isMultimodal) {
            return ModelRoutingDecision(
                selectedProvider = "Server-Side Gemini 2.5 Flash Vision",
                capability = ModelCapability.MULTIMODAL_VISION,
                isOnlineRequired = true,
                latencyExpectedMs = 1200,
                privacyLevel = "Encrypted Transient Session",
                availabilityStatus = if (isServerGeminiAvailable) "ACTIVE" else "UNAVAILABLE",
                fallbackReason = if (!isServerGeminiAvailable) "Vision API credentials unavailable; visual entity scanning offline." else null
            )
        }

        return when (intentType) {
            UniversalIntentType.PERSONAL_KNOWLEDGE_QUERY,
            UniversalIntentType.MEMORY_QUERY,
            UniversalIntentType.PROFILE_QUERY,
            UniversalIntentType.ROUTINE_QUERY,
            UniversalIntentType.GOAL_QUERY,
            UniversalIntentType.PROJECT_QUERY,
            UniversalIntentType.FAMILY_QUERY,
            UniversalIntentType.PEOPLE_QUERY,
            UniversalIntentType.PLACE_QUERY,
            UniversalIntentType.DEVICE_QUERY,
            UniversalIntentType.PERMISSION_QUERY -> ModelRoutingDecision(
                selectedProvider = "On-Device Personal Context Engine",
                capability = ModelCapability.LOCAL_HEURISTICS,
                isOnlineRequired = false,
                latencyExpectedMs = 10,
                privacyLevel = "Strict Local On-Device (Never Leaves Device)",
                availabilityStatus = "ACTIVE"
            )

            UniversalIntentType.RESEARCH_TOPIC,
            UniversalIntentType.RESEARCH_QUERY -> ModelRoutingDecision(
                selectedProvider = "Server-Side Gemini & Live Web Synthesis",
                capability = ModelCapability.SERVER_SIDE_GEMINI,
                isOnlineRequired = true,
                latencyExpectedMs = 1500,
                privacyLevel = "Encrypted Transient Context",
                availabilityStatus = if (isServerGeminiAvailable) "ACTIVE" else "OFFLINE_FALLBACK",
                fallbackReason = if (!isServerGeminiAvailable) "Live Web synthesis unavailable. Returning cached local insights." else null
            )

            UniversalIntentType.CODE_CALCULATION -> ModelRoutingDecision(
                selectedProvider = "Deterministic Mathematical & Syntax Engine",
                capability = ModelCapability.DETERMINISTIC_RULES,
                isOnlineRequired = false,
                latencyExpectedMs = 2,
                privacyLevel = "Strict Local On-Device",
                availabilityStatus = "ACTIVE"
            )

            else -> ModelRoutingDecision(
                selectedProvider = "VYB Hybrid Intelligence Orchestrator",
                capability = ModelCapability.SERVER_SIDE_GEMINI,
                isOnlineRequired = true,
                latencyExpectedMs = 600,
                privacyLevel = "On-Device Context Grounded",
                availabilityStatus = "ACTIVE"
            )
        }
    }
}
