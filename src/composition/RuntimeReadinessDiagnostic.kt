package parker.composition

import java.time.LocalDate
import java.util.jar.Manifest

/**
 * The individual, non-secret predicates used by the governed region-execution
 * composition guard. This is diagnostic data only; [overallReady] grants no
 * execution authority.
 */
data class RuntimeReadinessDiagnostic(
    val acceptanceStoragePresent: Boolean,
    val acceptanceStorageComplete: Boolean,
    val providerStateStoragePresent: Boolean,
    val buildIdentityPresent: Boolean,
    val buildIdentityMatches: Boolean,
    val providerProfilePresent: Boolean,
    val providerProfileStructurallyValid: Boolean,
    val providerProfileAccepted: Boolean,
    val providerProfileNonStale: Boolean,
    val providerProfileReady: Boolean,
    val credentialPresent: Boolean,
    val credentialStructurallyValid: Boolean,
    val overallReady: Boolean,
    val reasons: Map<String, String>,
) {
    companion object {
        /** Evaluates the same profile/configuration inputs used by production composition. */
        fun evaluate(config: ParkerRuntimeConfig, embeddedCommit: String?): RuntimeReadinessDiagnostic {
            val profilePath = config.openAiExternalTranscriptionProviderProfilePath
            val profileReadiness = OpenAiExternalTranscriptionProviderReadinessEvaluator()
                .evaluate(config.openAiExternalTranscriptionEnabled, profilePath)
            return fromEvaluated(config, embeddedCommit, profileReadiness)
        }

        /** Builds diagnostics from the already-evaluated production readiness value. */
        fun fromEvaluated(
            config: ParkerRuntimeConfig,
            embeddedCommit: String?,
            profileReadiness: OpenAiExternalTranscriptionReadiness,
        ): RuntimeReadinessDiagnostic {
            val authorityPresent = !config.fidelityFirstAcceptanceAuthorityStorageRootPath.isNullOrBlank()
            val attemptPresent = !config.fidelityFirstAttemptStorageRootPath.isNullOrBlank()
            val providerStatePresent = !config.regionProviderStateStorageRootPath.isNullOrBlank()
            val acceptanceComplete = authorityPresent && attemptPresent
            val buildPresent = !config.productionCommit.isNullOrBlank()
            val buildMatches = buildPresent && config.productionCommit == embeddedCommit
            val profilePresent = !config.openAiExternalTranscriptionProviderProfilePath.isNullOrBlank()
            val profileValid = profileReadiness is OpenAiExternalTranscriptionReadiness.Ready
            val profile = (profileReadiness as? OpenAiExternalTranscriptionReadiness.Ready)?.profile
            val accepted = profile?.acceptanceState == ExternalTranscriptionAcceptanceState.ACCEPTED
            val nonStale = profileReadiness !is OpenAiExternalTranscriptionReadiness.StaleProfile &&
                profileReadiness !is OpenAiExternalTranscriptionReadiness.InvalidProfile
            val credentialPresent = config.openAiApiCredential != null
            val credentialValid = credentialPresent
            // Startup composes the separately governed acceptance lane while a profile is
            // ACCEPTANCE_PENDING; lifecycle acceptance gates execution, not composition.
            val providerReady = profileValid && nonStale && credentialValid
            val regionExecutionConfigured = !config.regionProviderStateStorageRootPath.isNullOrBlank()
            val overall = !regionExecutionConfigured || (acceptanceComplete && providerStatePresent && buildMatches && providerReady)
            val reasons = linkedMapOf<String, String>()
            if (!authorityPresent) reasons["acceptanceStoragePresent"] = "ACCEPTANCE_AUTHORITY_ROOT_MISSING"
            if (!attemptPresent) reasons["acceptanceStorageComplete"] = "ATTEMPT_LEDGER_ROOT_MISSING"
            if (!providerStatePresent) reasons["providerStateStoragePresent"] = "PROVIDER_STATE_ROOT_MISSING"
            if (!buildPresent) reasons["buildIdentityPresent"] = "PRODUCTION_COMMIT_MISSING"
            else if (!buildMatches) reasons["buildIdentityMatches"] = "BUILD_IDENTITY_MISMATCH"
            if (!profilePresent) reasons["providerProfilePresent"] = "PROFILE_PATH_MISSING"
            when (profileReadiness) {
                is OpenAiExternalTranscriptionReadiness.InvalidProfile -> {
                    reasons["providerProfileStructurallyValid"] = "INVALID_PROFILE"
                    reasons["providerProfileReady"] = profileReadiness.reason
                }
                is OpenAiExternalTranscriptionReadiness.StaleProfile -> {
                    reasons["providerProfileNonStale"] = "PROFILE_REVIEW_EXPIRED"
                    reasons["providerProfileReady"] = "PROFILE_NOT_READY"
                }
                OpenAiExternalTranscriptionReadiness.Disabled -> reasons["providerProfileReady"] = "PROFILE_DISABLED"
                is OpenAiExternalTranscriptionReadiness.Ready -> Unit
            }
            if (!accepted) reasons["providerProfileAccepted"] = "PROFILE_NOT_ACCEPTED"
            if (!credentialPresent) reasons["credentialPresent"] = "CREDENTIAL_MISSING"
            return RuntimeReadinessDiagnostic(
                authorityPresent, acceptanceComplete, providerStatePresent, buildPresent, buildMatches,
                profilePresent, profileValid, accepted, nonStale, providerReady,
                credentialPresent, credentialValid, overall, reasons,
            )
        }
    }
}

/** Shared manifest identity lookup used by startup and the diagnostic entry point. */
fun discoverRuntimeEmbeddedSourceCommit(classLoader: ClassLoader = ParkerRuntime::class.java.classLoader): String? =
    classLoader.getResources("META-INF/MANIFEST.MF").toList()
        .mapNotNull { resource -> runCatching { resource.openStream().use { Manifest(it).mainAttributes.getValue("Parker-Source-Commit") } }.getOrNull() }
        .singleOrNull { Regex("^[0-9a-f]{40}$").matches(it) }

/** Bounded, non-egress diagnostic entry point; it never starts Parker or a provider. */
object RuntimeReadinessDiagnosticCli {
    @JvmStatic
    fun main(args: Array<String>) {
        val config = ParkerRuntimeConfigLoader.load(System.getenv())
        val embeddedCommit = args.firstOrNull()?.takeIf { it.matches(Regex("^[0-9a-f]{40}$")) }
            ?: discoverRuntimeEmbeddedSourceCommit()
        println(RuntimeReadinessDiagnostic.evaluate(config, embeddedCommit))
    }
}
