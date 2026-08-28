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
    val transcriptionProfileId: String = HISTORICAL_TRANSCRIPTION_PROFILE_ID,
    val instructionSha256: String? = null,
    val structuredSchemaSha256: String? = null,
    val processingProfileIdentity: String = BYTE_EXACT_PROCESSING_PROFILE_ID,
    val acceptanceState: ExternalTranscriptionAcceptanceState = ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING,
    val reasoningEffort: String = "NOT_CONFIGURED_HISTORICAL",
    val pdfDetail: String = "NOT_CONFIGURED_HISTORICAL",
    val imageDetail: String = "high",
)

enum class ExternalTranscriptionAcceptanceState {
    DISABLED,
    CONFIGURATION_READY,
    ACCEPTANCE_PENDING,
    ACCEPTED,
    SUSPENDED,
}

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
        transcriptionProfileId = if (p.required("schemaVersion") == "1") HISTORICAL_TRANSCRIPTION_PROFILE_ID else p.required("transcriptionProfileId"),
        instructionSha256 = if (p.required("schemaVersion") == "1") null else p.required("instructionSha256"),
        structuredSchemaSha256 = if (p.required("schemaVersion") == "1") null else p.required("structuredSchemaSha256"),
        processingProfileIdentity = if (p.required("schemaVersion") == "1") BYTE_EXACT_PROCESSING_PROFILE_ID else p.required("processingProfileIdentity"),
        acceptanceState = if (p.required("schemaVersion") == "1") ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING
        else try { enumValueOf<ExternalTranscriptionAcceptanceState>(p.required("acceptanceState")) }
        catch (_: IllegalArgumentException) { invalidValue("acceptanceState is invalid") },
        reasoningEffort = if (p.required("schemaVersion") == "3") p.required("reasoningEffort") else "NOT_CONFIGURED_HISTORICAL",
        pdfDetail = if (p.required("schemaVersion") == "3") p.required("pdfDetail") else "NOT_CONFIGURED_HISTORICAL",
        imageDetail = if (p.required("schemaVersion") == "3") p.required("imageDetail") else "high",
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
        if (p.schemaVersion !in setOf("1", "2", "3")) return "Unsupported provider profile schemaVersion"
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
        if (p.transcriptionProfileId.isBlank() || p.processingProfileIdentity.isBlank()) return "Transcription configuration identities must be present"
        if (p.schemaVersion == "1") {
            if (p.transcriptionProfileId != HISTORICAL_TRANSCRIPTION_PROFILE_ID || p.instructionSha256 != null ||
                p.structuredSchemaSha256 != null || p.processingProfileIdentity != BYTE_EXACT_PROCESSING_PROFILE_ID ||
                p.acceptanceState != ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING
            ) return "Historical profile state is inconsistent"
        } else if (p.schemaVersion == "2") {
            if (p.transcriptionProfileId != LITERAL_V2_TRANSCRIPTION_PROFILE_ID) return "Profile schema v2 requires literal-v2 identity"
            if (!SHA256.matches(p.instructionSha256.orEmpty()) || !SHA256.matches(p.structuredSchemaSha256.orEmpty())) {
                return "Profile schema v2 requires valid configuration SHA-256 digests"
            }
            if (p.processingProfileIdentity != BYTE_EXACT_PROCESSING_PROFILE_ID) return "Profile schema v2 requires byte-exact processing"
        } else {
            if (p.transcriptionProfileId != FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID) return "Profile schema v3 requires fidelity-first identity"
            if (!SHA256.matches(p.instructionSha256.orEmpty()) || !SHA256.matches(p.structuredSchemaSha256.orEmpty())) {
                return "Profile schema v3 requires valid configuration SHA-256 digests"
            }
            if (p.processingProfileIdentity != DIRECT_AUTHORITATIVE_PROCESSING_PROFILE_ID) return "Profile schema v3 requires direct-authoritative-byte processing"
            if (p.modelSelectionRule != "gpt-5.6-sol") return "Profile schema v3 requires gpt-5.6-sol"
            if (p.reasoningEffort != "none") return "Profile schema v3 requires reasoning effort none"
            if (p.pdfDetail != "high") return "Profile schema v3 requires PDF detail high"
            if (p.imageDetail != "original") return "Profile schema v3 requires image detail original"
        }
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
            "transcriptionProfileId", "instructionSha256", "structuredSchemaSha256", "processingProfileIdentity", "acceptanceState",
            "reasoningEffort", "pdfDetail", "imageDetail",
        )
        val SHA256 = Regex("^[0-9a-f]{64}$")
    }
}

const val HISTORICAL_TRANSCRIPTION_PROFILE_ID = "openai-faithful-page-transcription-v1"
const val LITERAL_V2_TRANSCRIPTION_PROFILE_ID = "openai-literal-page-transcription-v2"
const val BYTE_EXACT_PROCESSING_PROFILE_ID = "external-transcription.direct-byte-exact-v1"
const val FIDELITY_FIRST_TRANSCRIPTION_PROFILE_ID = "openai-fidelity-first-transcription-v1"
const val DIRECT_AUTHORITATIVE_PROCESSING_PROFILE_ID = "external-transcription.direct-authoritative-byte-v1"
