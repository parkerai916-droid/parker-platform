package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import parker.composition.ExternalTranscriptionAcceptanceState
import parker.composition.OpenAiExternalTranscriptionReadiness
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.ExternalTranscriptionOwnerInvocationOutcome

internal object UnitOLiteralV2CleanLock {
    const val STAGE = "UNIT_O4_LITERAL_V2_CLEAN_REACCEPTANCE"
    const val ALLOCATION = "CLEAN_LITERAL_V2_REACCEPTANCE"
    const val EVIDENCE_ID = "evidence-6d9eae30-5cc5-4ba9-b70f-c7f680848296"
    const val SOURCE_SHA256 = "42d9b8e91c3a8df442d28ef7bb4199b7b77c5d6f6b6403dc2d22b551ab793ddd"
    const val SOURCE_BYTES = 810037L
    const val MEDIA_TYPE = "application/pdf"
    const val PROVIDER = "OpenAI"
    const val MODEL_RULE = "gpt-4.1-mini"
    const val PROFILE = "openai-literal-page-transcription-v2"
    const val INSTRUCTION_SHA256 = "c721e63b29e56f9242ee24dd8f13ddcab5d4468d3d17e9e3b9b1d66a68cb2000"
    const val SCHEMA_SHA256 = "3fe8a26be40a06f047b493094d06c52e1df056162583b8e0b81564f55de265b2"
    const val PROCESSING_PROFILE = "external-transcription.direct-byte-exact-v1"
    const val ENDPOINT = "/v1/responses"
    const val ADAPTER_VERSION = "1.1.0"
    const val REQUEST_ORDINAL = 2
}

internal data class UnitOLiteralV2AcceptanceAuthorization(
    val stage: String,
    val allocation: String,
    val evidenceArtifactId: String,
    val provider: String,
    val modelRule: String,
    val profileId: String,
    val instructionSha256: String,
    val schemaSha256: String,
    val processingProfile: String,
    val endpoint: String,
    val store: Boolean,
    val adapterVersion: String,
    val requestOrdinal: Int,
    val repositoryCommit: String,
    val issuedBy: String,
    val issuedAt: String,
)

internal data class UnitOLiteralV2AcceptancePreflightInput(
    val optIn: Boolean,
    val authorization: UnitOLiteralV2AcceptanceAuthorization?,
    val requestedEvidenceArtifactId: EvidenceArtifactId,
    val repositoryCommit: String,
    val profileReadiness: OpenAiExternalTranscriptionReadiness,
    val credentialReady: Boolean,
    val resultPath: Path?,
)

internal data class UnitOLiteralV2AcceptancePreflight(
    val problems: List<String>,
    val metadata: UnitOAuthoritativeManifestFacts?,
) { val ready: Boolean get() = problems.isEmpty() }

/** Detached Unit O boundary. It is test/integration-only and is never composed into ParkerRuntime. */
internal class UnitOLiteralV2CleanAcceptanceBoundary(
    private val manifestReader: UnitOManifestMetadataReader,
    private val invokeGovernedProductionFlow: suspend (EvidenceArtifactId) -> ExternalTranscriptionOwnerInvocationOutcome,
) {
    private var providerAttempts = 0
    val requestCount: Int get() = providerAttempts

    suspend fun preflight(input: UnitOLiteralV2AcceptancePreflightInput): UnitOLiteralV2AcceptancePreflight {
        val problems = mutableListOf<String>()
        if (!input.optIn) problems += "OPT_IN_MISSING"
        val a = input.authorization
        if (a == null) problems += "AUTHORIZATION_RECORD_MISSING"
        if (input.requestedEvidenceArtifactId.value != UnitOLiteralV2CleanLock.EVIDENCE_ID) problems += "EVIDENCE_ID_MISMATCH"
        if (a != null) {
            if (a.stage != UnitOLiteralV2CleanLock.STAGE) problems += "STAGE_MISMATCH"
            if (a.allocation != UnitOLiteralV2CleanLock.ALLOCATION) problems += "ALLOCATION_MISMATCH"
            if (a.evidenceArtifactId != UnitOLiteralV2CleanLock.EVIDENCE_ID) problems += "AUTHORIZATION_EVIDENCE_MISMATCH"
            if (a.provider != UnitOLiteralV2CleanLock.PROVIDER || a.modelRule != UnitOLiteralV2CleanLock.MODEL_RULE ||
                a.profileId != UnitOLiteralV2CleanLock.PROFILE || a.instructionSha256 != UnitOLiteralV2CleanLock.INSTRUCTION_SHA256 ||
                a.schemaSha256 != UnitOLiteralV2CleanLock.SCHEMA_SHA256 || a.processingProfile != UnitOLiteralV2CleanLock.PROCESSING_PROFILE ||
                a.endpoint != UnitOLiteralV2CleanLock.ENDPOINT || a.store || a.adapterVersion != UnitOLiteralV2CleanLock.ADAPTER_VERSION
            ) problems += "AUTHORIZATION_TUPLE_MISMATCH"
            if (a.requestOrdinal != UnitOLiteralV2CleanLock.REQUEST_ORDINAL) problems += "REQUEST_ORDINAL_MISMATCH"
            if (a.repositoryCommit != input.repositoryCommit) problems += "COMMIT_BINDING_MISMATCH"
            if (a.issuedBy.isBlank() || a.issuedAt.isBlank()) problems += "AUTHORIZATION_ISSUANCE_MISSING"
        }
        val ready = input.profileReadiness as? OpenAiExternalTranscriptionReadiness.Ready
        if (ready == null) problems += "PROFILE_NOT_READY" else {
            val p = ready.profile
            if (p.acceptanceState != ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING) problems += "STATE_NOT_ACCEPTANCE_PENDING"
            if (p.providerIdentity != UnitOLiteralV2CleanLock.PROVIDER || p.modelSelectionRule != UnitOLiteralV2CleanLock.MODEL_RULE ||
                p.transcriptionProfileId != UnitOLiteralV2CleanLock.PROFILE || p.instructionSha256 != UnitOLiteralV2CleanLock.INSTRUCTION_SHA256 ||
                p.structuredSchemaSha256 != UnitOLiteralV2CleanLock.SCHEMA_SHA256 || p.processingProfileIdentity != UnitOLiteralV2CleanLock.PROCESSING_PROFILE ||
                p.apiProductPath != UnitOLiteralV2CleanLock.ENDPOINT || p.store
            ) problems += "PROFILE_TUPLE_MISMATCH"
        }
        if (!input.credentialReady) problems += "CREDENTIAL_NOT_READY"
        if (input.resultPath == null || !Files.isRegularFile(input.resultPath) || !Files.isWritable(input.resultPath)) problems += "RESULT_PATH_NOT_READY"
        if (providerAttempts != 0) problems += "ALLOCATION_ALREADY_ATTEMPTED"

        var metadata: UnitOAuthoritativeManifestFacts? = null
        if (input.requestedEvidenceArtifactId.value == UnitOLiteralV2CleanLock.EVIDENCE_ID) {
            metadata = manifestReader.read(input.requestedEvidenceArtifactId)
            if (metadata == null) problems += "MANIFEST_MISSING" else {
                if (metadata.evidenceArtifactId != input.requestedEvidenceArtifactId) problems += "MANIFEST_ID_MISMATCH"
                if (metadata.sha256 != UnitOLiteralV2CleanLock.SOURCE_SHA256) problems += "SOURCE_SHA256_MISMATCH"
                if (metadata.byteLength != UnitOLiteralV2CleanLock.SOURCE_BYTES) problems += "SOURCE_LENGTH_MISMATCH"
                if (metadata.declaredMediaType != UnitOLiteralV2CleanLock.MEDIA_TYPE) problems += "SOURCE_MEDIA_TYPE_MISMATCH"
            }
        }
        return UnitOLiteralV2AcceptancePreflight(problems.distinct(), metadata)
    }

    suspend fun execute(input: UnitOLiteralV2AcceptancePreflightInput): ExternalTranscriptionOwnerInvocationOutcome {
        val preflight = preflight(input)
        require(preflight.ready) { "acceptance preflight failed: ${preflight.problems.joinToString(",")}" }
        check(providerAttempts == 0) { "CLEAN literal-v2 allocation already attempted" }
        providerAttempts++
        return invokeGovernedProductionFlow(input.requestedEvidenceArtifactId)
    }
}

internal fun UnitOLiteralV2AcceptanceAuthorization.render(): String = listOf(
    "stage=$stage", "allocation=$allocation", "evidenceArtifactId=$evidenceArtifactId", "provider=$provider",
    "modelRule=$modelRule", "profileId=$profileId", "instructionSha256=$instructionSha256",
    "schemaSha256=$schemaSha256", "processingProfile=$processingProfile", "endpoint=$endpoint",
    "store=$store", "adapterVersion=$adapterVersion", "requestOrdinal=$requestOrdinal",
    "repositoryCommit=$repositoryCommit", "issuedBy=$issuedBy", "issuedAt=$issuedAt",
).joinToString("\n", postfix = "\n")

internal fun readUnitOLiteralV2Authorization(path: Path): UnitOLiteralV2AcceptanceAuthorization {
    require(Files.isRegularFile(path) && Files.isReadable(path) && Files.size(path) <= 64L * 1024) { "authorization record is unavailable or unbounded" }
    val fields = Files.readAllLines(path).filter { it.isNotBlank() }.associate { line ->
        val separator = line.indexOf('='); require(separator in 1 until line.lastIndex) { "authorization record is malformed" }
        line.substring(0, separator) to line.substring(separator + 1)
    }
    val expected = setOf("stage", "allocation", "evidenceArtifactId", "provider", "modelRule", "profileId", "instructionSha256",
        "schemaSha256", "processingProfile", "endpoint", "store", "adapterVersion", "requestOrdinal", "repositoryCommit", "issuedBy", "issuedAt")
    require(fields.keys == expected) { "authorization record fields are not exact" }
    fun value(name: String) = fields.getValue(name)
    return UnitOLiteralV2AcceptanceAuthorization(value("stage"), value("allocation"), value("evidenceArtifactId"), value("provider"),
        value("modelRule"), value("profileId"), value("instructionSha256"), value("schemaSha256"), value("processingProfile"),
        value("endpoint"), value("store").toBooleanStrict(), value("adapterVersion"), value("requestOrdinal").toInt(),
        value("repositoryCommit"), value("issuedBy"), value("issuedAt"))
}

internal data class UnitOLiteralV2AcceptanceResult(
    val status: String, val commit: String, val returnedModel: String?, val responseId: String?, val generationId: String?,
    val requestedPages: String?, val returnedPages: String?, val fidelity: String?, val completeness: String?,
    val requestCount: Int, val retryCount: Int = 0, val fallbackCount: Int = 0, val modelSwitchCount: Int = 0,
    val analysisCount: Int = 0,
) {
    fun render(): String = listOf(
        "status=$status", "commit=$commit", "stage=${UnitOLiteralV2CleanLock.STAGE}", "allocation=${UnitOLiteralV2CleanLock.ALLOCATION}",
        "evidenceArtifactId=${UnitOLiteralV2CleanLock.EVIDENCE_ID}", "sourceSha256=${UnitOLiteralV2CleanLock.SOURCE_SHA256}",
        "sourceByteLength=${UnitOLiteralV2CleanLock.SOURCE_BYTES}", "mediaType=${UnitOLiteralV2CleanLock.MEDIA_TYPE}",
        "profileId=${UnitOLiteralV2CleanLock.PROFILE}", "instructionSha256=${UnitOLiteralV2CleanLock.INSTRUCTION_SHA256}",
        "schemaSha256=${UnitOLiteralV2CleanLock.SCHEMA_SHA256}", "processingProfile=${UnitOLiteralV2CleanLock.PROCESSING_PROFILE}",
        "provider=${UnitOLiteralV2CleanLock.PROVIDER}", "returnedModel=${returnedModel ?: "NOT_AVAILABLE"}",
        "responseId=${responseId ?: "NOT_AVAILABLE"}", "derivativeGenerationId=${generationId ?: "NOT_AVAILABLE"}",
        "requestedPages=${requestedPages ?: "NOT_AVAILABLE"}", "returnedPages=${returnedPages ?: "NOT_AVAILABLE"}",
        "fidelity=${fidelity ?: "NOT_AVAILABLE"}", "completeness=${completeness ?: "NOT_AVAILABLE"}",
        "requestCount=$requestCount", "retryCount=$retryCount", "fallbackCount=$fallbackCount", "modelSwitchCount=$modelSwitchCount",
        "analysisCount=$analysisCount",
    ).joinToString("\n", postfix = "\n")
}
