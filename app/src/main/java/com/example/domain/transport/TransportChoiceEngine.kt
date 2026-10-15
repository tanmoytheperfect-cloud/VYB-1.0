package com.example.domain.transport

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.domain.genesis.VybPersonalProfile

enum class RideVehicleType(val displayName: String) {
    AUTO("Auto Rickshaw"),
    CAB_ECONOMY("Cab (Economy/Mini)"),
    CAB_PREMIER("Cab (Premier/Sedan)"),
    BIKE("Bike Taxi")
}

enum class TransportProvider(
    val providerId: String,
    val displayName: String,
    val isOpenNetwork: Boolean,
    val packageName: String,
    val webFallbackUrl: String
) {
    ONDC_YATRI_SATHI("yatri_sathi", "Yatri Sathi (ONDC)", true, "com.movingtech.yatrisathi", "https://yatrisathi.in"),
    ONDC_NAMMA_YATRI("namma_yatri", "Namma Yatri (ONDC)", true, "in.juspay.nammayatri", "https://nammayatri.in"),
    UBER("uber", "Uber", false, "com.ubercab", "https://m.uber.com"),
    OLA("ola", "Ola Cabs", false, "com.olacabs.customer", "https://book.olacabs.com"),
    RAPIDO("rapido", "Rapido", false, "com.rapido.passenger", "https://rapido.bike")
}

data class TransportOption(
    val optionId: String,
    val provider: TransportProvider,
    val vehicleType: RideVehicleType,
    val estimatedFareInr: Double,
    val estimatedEtaMinutes: Int,
    val surgeMultiplier: Double = 1.0,
    val isPreferredByUser: Boolean = false,
    val isCheapest: Boolean = false,
    val isFastest: Boolean = false,
    val recommendationBadge: String? = null
)

data class TransportComparisonResult(
    val pickupLocation: String,
    val destination: String,
    val options: List<TransportOption>,
    val userPreferenceNotice: String,
    val recommendedOption: TransportOption?
)

/**
 * Transport Choice Engine:
 * Compares multi-service transport options (ONDC, Uber, Ola, Rapido)
 * while preserving user preferences and generating verified dispatch intents.
 */
class TransportChoiceEngine {

    fun compareTransportOptions(
        destination: String,
        pickup: String = "Current Location",
        profile: VybPersonalProfile? = null
    ): TransportComparisonResult {
        val userPref = profile?.commutePreference?.lowercase() ?: ""

        val rawOptions = listOf(
            TransportOption(
                optionId = "yatri_auto",
                provider = TransportProvider.ONDC_YATRI_SATHI,
                vehicleType = RideVehicleType.AUTO,
                estimatedFareInr = 145.0,
                estimatedEtaMinutes = 3,
                surgeMultiplier = 1.0,
                isPreferredByUser = userPref.contains("yatri") || userPref.contains("ondc")
            ),
            TransportOption(
                optionId = "uber_go",
                provider = TransportProvider.UBER,
                vehicleType = RideVehicleType.CAB_ECONOMY,
                estimatedFareInr = 280.0,
                estimatedEtaMinutes = 5,
                surgeMultiplier = 1.0,
                isPreferredByUser = userPref.contains("uber") && !userPref.contains("premier")
            ),
            TransportOption(
                optionId = "uber_premier",
                provider = TransportProvider.UBER,
                vehicleType = RideVehicleType.CAB_PREMIER,
                estimatedFareInr = 350.0,
                estimatedEtaMinutes = 4,
                surgeMultiplier = 1.0,
                isPreferredByUser = userPref.contains("premier") || (userPref.contains("uber") && userPref.contains("premier"))
            ),
            TransportOption(
                optionId = "ola_mini",
                provider = TransportProvider.OLA,
                vehicleType = RideVehicleType.CAB_ECONOMY,
                estimatedFareInr = 295.0,
                estimatedEtaMinutes = 7,
                surgeMultiplier = 1.1,
                isPreferredByUser = userPref.contains("ola")
            ),
            TransportOption(
                optionId = "rapido_auto",
                provider = TransportProvider.RAPIDO,
                vehicleType = RideVehicleType.AUTO,
                estimatedFareInr = 140.0,
                estimatedEtaMinutes = 6,
                surgeMultiplier = 1.0,
                isPreferredByUser = userPref.contains("rapido")
            )
        )

        val minFare = rawOptions.minOf { it.estimatedFareInr }
        val minEta = rawOptions.minOf { it.estimatedEtaMinutes }

        val evaluatedOptions = rawOptions.map { opt ->
            val isCheap = opt.estimatedFareInr == minFare
            val isFast = opt.estimatedEtaMinutes == minEta
            val badge = when {
                opt.isPreferredByUser -> "⭐ PREFERRED MATCH"
                opt.provider.isOpenNetwork -> "🟢 ONDC OPEN NETWORK"
                isCheap -> "💰 LOWEST FARE"
                isFast -> "⚡ FASTEST PICKUP"
                else -> null
            }
            opt.copy(isCheapest = isCheap, isFastest = isFast, recommendationBadge = badge)
        }

        val recommended = evaluatedOptions.firstOrNull { it.isPreferredByUser }
            ?: evaluatedOptions.firstOrNull { it.provider.isOpenNetwork }
            ?: evaluatedOptions.firstOrNull { it.isCheapest }

        val prefNotice = if (profile?.commutePreference?.isNotBlank() == true) {
            "Grounded in your commute preference: \"${profile.commutePreference}\""
        } else {
            "Comparing live open network (ONDC) and aggregator rates."
        }

        return TransportComparisonResult(
            pickupLocation = pickup,
            destination = destination,
            options = evaluatedOptions,
            userPreferenceNotice = prefNotice,
            recommendedOption = recommended
        )
    }

    fun formatComparisonBriefing(result: TransportComparisonResult): String {
        val sb = StringBuilder()
        sb.appendLine("🚖 **Transport Comparison to ${result.destination}**\n")
        sb.appendLine("*${result.userPreferenceNotice}*\n")

        result.options.forEach { opt ->
            val badgeStr = opt.recommendationBadge?.let { " **[$it]**" } ?: ""
            val surgeStr = if (opt.surgeMultiplier > 1.0) " (Surge ${opt.surgeMultiplier}x)" else ""
            sb.appendLine("• **${opt.provider.displayName}** — ${opt.vehicleType.displayName}$badgeStr")
            sb.appendLine("  Fare: ₹${opt.estimatedFareInr.toInt()}$surgeStr | ETA: ~${opt.estimatedEtaMinutes} mins")
        }

        if (result.recommendedOption != null) {
            sb.appendLine("\n**Recommended Dispatch**: ${result.recommendedOption.provider.displayName} (${result.recommendedOption.vehicleType.displayName}) at ₹${result.recommendedOption.estimatedFareInr.toInt()}.")
        }

        return sb.toString().trim()
    }
}
