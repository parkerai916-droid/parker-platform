package parker.composition

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.util.Properties
import parker.core.interfaces.ExternalTranscriptionRequest

data class OpenAiExternalTranscriptionProviderProfile(
    val schemaVersion: String,
    val providerIdentity: String,
    val apiProductPath: String,
    val store: Boolean,
    val modelSelectionRule: String,
    val modelSnapshotPolicy: String,
    val maximumPdfBytes: Long,
    val maximumImageBytes: Long,
    val maximumOutputBytes: Long,
    val timeoutMillis: Long,
    val allowedNetworkDestination: String,
    val retentionTreatment: String,
    val dataUseTrainingTreatment: String,
    val zdrMamStatus: String,
    val projectAccountStatus: String,
    val projectAccountControls: String,
    val authenticationMechanism: String,
    val requestLoggingConsiderations: String,
    val regionalStorageConsiderations: String,
    val verifiedOn: LocalDate,
    val approvingOwnerReference: String,
    val nextReviewDate: LocalDate,
    val verificationReferences: List<String>,
    val reverificationTriggers: List<String>,
)

data class OpenAiExternalTranscriptionEffectiveLimits(
    val maximumPdfBytes: Long,
    val maximumImageBytes: Long,
    val maximumOutputBytes: Long,
    val timeoutMillis: Long,
)

sealed interface OpenAiExternalTranscriptionReadiness {
    data object Disabled : OpenAiExternalTranscriptionReadiness
    data class InvalidProfile(val reason: String) : OpenAiExternalTranscriptionReadiness
    data class StaleProfile(val nextReviewDate: LocalDate) : OpenAiExternalTranscriptionReadiness
    data class Ready(
        val profile: OpenAiExternalTranscriptionProviderProfile,
        val effectiveLimits: OpenAiExternalTranscriptionEffectiveLimits,
    ) : OpenAiExternalTranscriptionReadiness
}

/** Credential-free, file-local profile loading and deterministic readiness evaluation. */
class OpenAiExternalTranscriptionProviderReadinessEvaluator(
    private val today: () -> LocalDate = LocalDate::now,
) {
    fun evaluate(enabled: Boolean, profilePath: String?): OpenAiExternalTranscriptionReadiness {
        if (!enabled) return OpenAiExternalTranscriptionReadiness.Disabled
        if (profilePath.isNullOrBlank()) return invalid("Enabled external transcription requires a provider profile path")
        val properties = try {
            Properties().apply { Files.newInputStream(Path.of(profilePath)).use(::load) }
        } catch (_: Exception) {
            return invalid("Provider profile is missing or malformed")
        }
        val profile = try {
            val unexpected = properties.stringPropertyNames() - PROFILE_KEYS
            require(unexpected.isEmpty()) { "Provider profile contains unsupported fields: ${unexpected.sorted()}" }
            parse(properties)
        } catch (e: IllegalArgumentException) {
            return invalid(e.message ?: "Provider profile is invalid")
        }
        validate(profile)?.let { return invalid(it) }
        if (today().isAfter(profile.nextReviewDate)) {
            return OpenAiExternalTranscriptionReadiness.StaleProfile(profile.nextReviewDate)
        }
        return OpenAiExternalTranscriptionReadiness.Ready(
            profile,
            OpenAiExternalTranscriptionEffectiveLimits(
                maximumPdfBytes = minOf(profile.maximumPdfBytes, ExternalTranscriptionRequest.MAX_SOURCE_BYTES),
                maximumImageBytes = minOf(profile.maximumImageBytes, ExternalTranscriptionRequest.MAX_SOURCE_BYTES),
                maximumOutputBytes = minOf(profile.maximumOutputBytes, PARKER_MAXIMUM_OUTPUT_BYTES),
                timeoutMillis = minOf(profile.timeoutMillis, PARKER_MAXIMUM_TIMEOUT_MILLIS),
            ),
        )
    }

    private fun parse(p: Properties) = OpenAiExternalTranscriptionProviderProfile(
        schemaVersion = p.required("schemaVersion"),
        providerIdentity = p.required("providerIdentity"),
        apiProductPath = p.required("apiProductPath"),
        store = p.required("store").toBooleanStrictOrNull() ?: invalidValue("store must be true or false"),
        modelSelectionRule = p.required("modelSelectionRule"),
        modelSnapshotPolicy = p.required("modelSnapshotPolicy"),
        maximumPdfBytes = p.positiveLong("maximumPdfBytes"),
        maximumImageBytes = p.positiveLong("maximumImageBytes"),
        maximumOutputBytes = p.positiveLong("maximumOutputBytes"),
        timeoutMillis = p.positiveLong("timeoutMillis"),
        allowedNetworkDestination = p.required("allowedNetworkDestination"),
        retentionTreatment = p.required("retentionTreatment"),
        dataUseTrainingTreatment = p.required("dataUseTrainingTreatment"),
        zdrMamStatus = p.required("zdrMamStatus"),
        projectAccountStatus = p.required("projectAccountStatus"),
        projectAccountControls = p.required("projectAccountControls"),
        authenticationMechanism = p.required("authenticationMechanism"),
        requestLoggingConsiderations = p.required("requestLoggingConsiderations"),
        regionalStorageConsiderations = p.required("regionalStorageConsiderations"),
        verifiedOn = p.date("verifiedOn"),
        approvingOwnerReference = p.required("approvingOwnerReference"),
        nextReviewDate = p.date("nextReviewDate"),
        verificationReferences = p.list("verificationReferences"),
        reverificationTriggers = p.list("reverificationTriggers"),
    )

    private fun validate(p: OpenAiExternalTranscriptionProviderProfile): String? {
        val bounded = listOf(
            p.schemaVersion, p.providerIdentity, p.apiProductPath, p.modelSelectionRule,
            p.modelSnapshotPolicy, p.allowedNetworkDestination, p.retentionTreatment,
            p.dataUseTrainingTreatment, p.zdrMamStatus, p.projectAccountStatus,
            p.projectAccountControls, p.authenticationMechanism, p.requestLoggingConsiderations,
            p.regionalStorageConsiderations, p.approvingOwnerReference,
        ) + p.verificationReferences + p.reverificationTriggers
        if (bounded.any { it.length !in 1..MAX_FIELD_CHARACTERS }) return "Profile fields must be bounded"
        if (p.schemaVersion != "1") return "Unsupported provider profile schemaVersion"
        if (p.providerIdentity != "OpenAI") return "Provider identity must be OpenAI"
        if (p.apiProductPath != "/v1/responses") return "API product path must be /v1/responses"
        if (p.store) return "Provider profile must require store=false"
        if (p.modelSelectionRule.equals("unknown", true)) return "Model selection rule must not be unknown"
        if (p.modelSnapshotPolicy.equals("unknown", true)) return "Model snapshot policy must be explicit"
        if (p.authenticationMechanism != "BEARER_API_CREDENTIAL") return "Authentication mechanism must be a description only"
        if (p.verifiedOn.isAfter(today())) return "verifiedOn must not be in the future"
        if (p.nextReviewDate.isBefore(p.verifiedOn)) return "nextReviewDate must not precede verifiedOn"
        if (p.verificationReferences.isEmpty()) return "Verification references must be present"
        if (p.reverificationTriggers.isEmpty()) return "Re-verification triggers must be present"
        val destination = try { URI(p.allowedNetworkDestination) } catch (_: Exception) { return "Network destination is invalid" }
        if (destination.scheme != "https" || destination.host != "api.openai.com" ||
            destination.userInfo != null || destination.port != -1 ||
            (destination.path.isNotEmpty() && destination.path != "/") || destination.query != null || destination.fragment != null
        ) return "Network destination must be credential-free https://api.openai.com"
        return null
    }

    private fun Properties.required(key: String): String = getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
        ?: invalidValue("Missing mandatory profile field: $key")
    private fun Properties.positiveLong(key: String): Long = required(key).toLongOrNull()?.takeIf { it > 0 }
        ?: invalidValue("$key must be a positive integer")
    private fun Properties.date(key: String): LocalDate = try { LocalDate.parse(required(key)) }
        catch (_: Exception) { invalidValue("$key must be an ISO date") }
    private fun Properties.list(key: String): List<String> = required(key).split('|').map(String::trim).filter(String::isNotEmpty)
    private fun invalid(reason: String) = OpenAiExternalTranscriptionReadiness.InvalidProfile(reason)
    private fun invalidValue(reason: String): Nothing = throw IllegalArgumentException(reason)

    private companion object {
        const val MAX_FIELD_CHARACTERS = 4_096
        const val PARKER_MAXIMUM_OUTPUT_BYTES = 20L * 1024L * 1024L
        const val PARKER_MAXIMUM_TIMEOUT_MILLIS = 900_000L
        val PROFILE_KEYS = setOf(
            "schemaVersion", "providerIdentity", "apiProductPath", "store", "modelSelectionRule",
            "modelSnapshotPolicy", "maximumPdfBytes", "maximumImageBytes", "maximumOutputBytes",
            "timeoutMillis", "allowedNetworkDestination", "retentionTreatment", "dataUseTrainingTreatment",
            "zdrMamStatus", "projectAccountStatus", "projectAccountControls", "authenticationMechanism",
            "requestLoggingConsiderations", "regionalStorageConsiderations", "verifiedOn",
            "approvingOwnerReference", "nextReviewDate", "verificationReferences", "reverificationTriggers",
        )
    }
}
