package parker.core.runtime

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.imageio.ImageIO
import parker.core.interfaces.*

const val ORDINARY_REGION_CAPABILITY_ID = "ordinary-external-region-transcription-v5"
const val ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORE_ID = "ordinary-region-capability-acceptance-v1"
const val ORDINARY_REGION_AUTHORIZATION_STORE_ID = "ordinary-region-owner-authorization-v1"
const val R69_AUTHORITY_ID = "authority-fa-9.4p-a1e-r6.8c1"
const val R69_AUTHORITY_RECORD_ID = "5497f12e65d6e7a4d795cfec22ee3aa99c40eb00a8fc2e9f76835b5cfb2d23c9"
const val R69_EXECUTION_ID = "execution-fa-9.4p-a1e-r6.8c1"
const val R69_REQUEST_DIGEST = "1a691388478370add9bae4e920fb1071369efa543057403727b422e9000a3d36"
const val R69_PROVIDER_RESPONSE_ID = "resp_0007d6aa81587b3e016a92f716feb087d0ae9e005456676627"
const val R69_PROVIDER_STATE_ID = "31b997b2a5208ab120fa483778bca9f1ec270c994b7937b3e5dd765db2bfabcd"
const val R69_RAW_RESPONSE_DIGEST = "500863d65c7f9ca69a66b2ffef3ef8a42b7033903cf1b5a5bd774d9f0decd87f"
const val R69_STRUCTURED_STATE_DIGEST = "7031179aa4267fdc12a50a429eef184e4ecfb2efb3ae993b6a5527ecf9f4c476"
const val R69_PROVIDER_RECORD_DIGEST = "ad2542015546250bfe0640e5c31636bb6401a20d537d95db04248c81883ad135"
const val R69_ASSESSMENT_DIGEST = "39fbc01c7cf831ebf5fc0751cfcca73310bc1a1a1846508ff46d64c61bd09da7"
const val R69_INPUT_TOKENS = 19364
const val R69_OUTPUT_TOKENS = 4803
const val R69_TOTAL_TOKENS = 24167
const val R69A_COMMIT = "07b5b07769e57f5e066b680599143a2de6082ad8"
const val R69A_REPORT_DIGEST = "6e3260effc4862bc4eed0a0b2e05aac5a118524f85ae16e4afec1d24e8d93200"
const val R69B_COMMIT = "ac6c49115a756d4b5b88ff69e1789b36f54b03e0"
const val R69B_REPORT_DIGEST = "2295ea42f7b447806e66d3aac2bec9a1a7875040c0d23aa16cee6d47d013a2dc"
const val R69C_COMMIT = "b9a964a98edf9803791a046d189a61c2353a44a3"
const val R69C_REPORT_DIGEST = "3245780663d86c486bf8b42d4dded31beb0cf8b00cb6641f8d596ce077fc6d4e"
const val ORDINARY_REGION_MAX_REQUEST_BODY_BYTES: Int = 16 * 1024 * 1024
const val ORDINARY_REGION_CAPABILITY_ACCEPTANCE_CONTAINER_ROOT = "/data/region-transcription-capability-acceptances"
const val ORDINARY_REGION_OWNER_AUTHORIZATION_CONTAINER_ROOT = "/data/external-region-owner-authorizations"

data class OrdinaryRegionCapabilityIdentity(
    val capabilityId: String = ORDINARY_REGION_CAPABILITY_ID,
    val provider: String = "OpenAI",
    val endpointOperation: String = "POST /v1/responses",
    val model: String = OPENAI_REGION_MODEL,
    val reasoning: String = "none",
    val store: Boolean = false,
    val imageDetail: String = OPENAI_REGION_IMAGE_DETAIL,
    val adapterId: String = OPENAI_REGION_ADAPTER_ID,
    val adapterVersion: String = OPENAI_REGION_ADAPTER_VERSION,
    val providerProfile: String = OPENAI_REGION_PROFILE_ID,
    val wireVersion: Int = REGION_TRANSCRIPTION_WIRE_VERSION,
    val schemaSha256: String = REGION_TRANSCRIPTION_SCHEMA_SHA256,
    val instructionSha256: String = OPENAI_REGION_INSTRUCTION_SHA256,
    val processingProfile: String = REGION_TRANSCRIPTION_PROCESSING_PROFILE,
    val rendererIdentity: String = "${DeterministicSourcePageRenderer.PDFBOX_ID}:${DeterministicSourcePageRenderer.PDFBOX_VERSION}:300dpi",
    val mediaType: String = "application/pdf",
    val maximumRegions: Int = 32,
    val aggregateRequestBodyMaximumBytes: Int = ORDINARY_REGION_MAX_REQUEST_BODY_BYTES,
    val batching: Boolean = false,
) {
    init {
        require(capabilityId == ORDINARY_REGION_CAPABILITY_ID && provider == "OpenAI")
        require(endpointOperation == "POST /v1/responses" && model == "gpt-5.6-sol")
        require(reasoning == "none" && !store && imageDetail == "original")
        require(adapterId == "openai-responses-region-transcription-adapter" && adapterVersion == "4.0.0")
        require(providerProfile == "openai-region-anchored-transcription-v2" && wireVersion == 5)
        require(schemaSha256 == REGION_TRANSCRIPTION_SCHEMA_SHA256 && instructionSha256 == OPENAI_REGION_INSTRUCTION_SHA256)
        require(processingProfile == REGION_TRANSCRIPTION_PROCESSING_PROFILE)
        require(rendererIdentity == "apache-pdfbox:3.0.7:300dpi")
        require(mediaType == "application/pdf" && maximumRegions == 32)
        require(aggregateRequestBodyMaximumBytes == 16_777_216 && !batching)
    }

    fun digest(): String = ordinaryRegionDigest(*fields().toTypedArray())
    internal fun fields() = listOf(capabilityId, provider, endpointOperation, model, reasoning, store.toString(),
        imageDetail, adapterId, adapterVersion, providerProfile, wireVersion.toString(), schemaSha256,
        instructionSha256, processingProfile, rendererIdentity, mediaType, maximumRegions.toString(),
        aggregateRequestBodyMaximumBytes.toString(), batching.toString())
}

enum class OrdinaryRegionAcceptanceEvidenceRole {
    R6_9_LIVE_PROVIDER_RESULT, R6_9A_FORENSIC_ANALYSIS, R6_9B_POINT_ANCHOR_SEMANTICS, R6_9C_FIDELITY_REVIEW,
}
enum class OrdinaryRegionFidelityClassification { PASS_FIDELITY, FAIL_FIDELITY }

data class OrdinaryRegionLiveR69Evidence(
    val role: OrdinaryRegionAcceptanceEvidenceRole,
    val authorityId: String, val executionId: String, val requestDigest: String,
    val providerResponseId: String, val providerStateRecordId: String,
    val rawResponseDigest: String, val structuredStateDigest: String,
    val providerStateRecordDigest: String, val assessmentDigest: String,
)
data class OrdinaryRegionR69AEvidence(val role: OrdinaryRegionAcceptanceEvidenceRole, val commit: String, val reportDigest: String)
data class OrdinaryRegionR69BEvidence(val role: OrdinaryRegionAcceptanceEvidenceRole, val commit: String, val reportDigest: String, val wireVersion: Int)
data class OrdinaryRegionR69CEvidence(
    val role: OrdinaryRegionAcceptanceEvidenceRole, val commit: String, val reportDigest: String,
    val classification: OrdinaryRegionFidelityClassification, val reviewedRegions: Int,
    val exactRegions: Int, val nonExactDiscrepancies: Int,
)

data class RegionTranscriptionCapabilityAcceptanceEvidenceV1(
    val liveResult: OrdinaryRegionLiveR69Evidence,
    val forensicAnalysis: OrdinaryRegionR69AEvidence,
    val pointAnchorSemantics: OrdinaryRegionR69BEvidence,
    val fidelityReview: OrdinaryRegionR69CEvidence,
) {
    init {
        val roles = listOf(liveResult.role, forensicAnalysis.role, pointAnchorSemantics.role, fidelityReview.role)
        require(roles.toSet().size == 4 && roles.toSet() == OrdinaryRegionAcceptanceEvidenceRole.entries.toSet())
        require(liveResult.role == OrdinaryRegionAcceptanceEvidenceRole.R6_9_LIVE_PROVIDER_RESULT)
        require(liveResult.authorityId == R69_AUTHORITY_ID && liveResult.executionId == R69_EXECUTION_ID)
        require(liveResult.requestDigest == R69_REQUEST_DIGEST && liveResult.providerResponseId == R69_PROVIDER_RESPONSE_ID)
        require(liveResult.providerStateRecordId == R69_PROVIDER_STATE_ID)
        require(liveResult.rawResponseDigest == R69_RAW_RESPONSE_DIGEST)
        require(liveResult.structuredStateDigest == R69_STRUCTURED_STATE_DIGEST)
        require(liveResult.providerStateRecordDigest == R69_PROVIDER_RECORD_DIGEST)
        require(liveResult.assessmentDigest == R69_ASSESSMENT_DIGEST)
        require(forensicAnalysis == OrdinaryRegionR69AEvidence(OrdinaryRegionAcceptanceEvidenceRole.R6_9A_FORENSIC_ANALYSIS, R69A_COMMIT, R69A_REPORT_DIGEST))
        require(pointAnchorSemantics == OrdinaryRegionR69BEvidence(OrdinaryRegionAcceptanceEvidenceRole.R6_9B_POINT_ANCHOR_SEMANTICS, R69B_COMMIT, R69B_REPORT_DIGEST, 5))
        require(fidelityReview == OrdinaryRegionR69CEvidence(OrdinaryRegionAcceptanceEvidenceRole.R6_9C_FIDELITY_REVIEW,
            R69C_COMMIT, R69C_REPORT_DIGEST, OrdinaryRegionFidelityClassification.PASS_FIDELITY, 24, 24, 0))
    }
    internal fun fields(): List<String> = listOf(
        liveResult.role.name, liveResult.authorityId, liveResult.executionId, liveResult.requestDigest,
        liveResult.providerResponseId, liveResult.providerStateRecordId, liveResult.rawResponseDigest,
        liveResult.structuredStateDigest, liveResult.providerStateRecordDigest, liveResult.assessmentDigest,
        forensicAnalysis.role.name, forensicAnalysis.commit, forensicAnalysis.reportDigest,
        pointAnchorSemantics.role.name, pointAnchorSemantics.commit, pointAnchorSemantics.reportDigest, pointAnchorSemantics.wireVersion.toString(),
        fidelityReview.role.name, fidelityReview.commit, fidelityReview.reportDigest, fidelityReview.classification.name,
        fidelityReview.reviewedRegions.toString(), fidelityReview.exactRegions.toString(), fidelityReview.nonExactDiscrepancies.toString(),
    )
    companion object {
        fun governed(live: OrdinaryRegionLiveR69Evidence) = RegionTranscriptionCapabilityAcceptanceEvidenceV1(
            live,
            OrdinaryRegionR69AEvidence(OrdinaryRegionAcceptanceEvidenceRole.R6_9A_FORENSIC_ANALYSIS, R69A_COMMIT, R69A_REPORT_DIGEST),
            OrdinaryRegionR69BEvidence(OrdinaryRegionAcceptanceEvidenceRole.R6_9B_POINT_ANCHOR_SEMANTICS, R69B_COMMIT, R69B_REPORT_DIGEST, 5),
            OrdinaryRegionR69CEvidence(OrdinaryRegionAcceptanceEvidenceRole.R6_9C_FIDELITY_REVIEW, R69C_COMMIT,
                R69C_REPORT_DIGEST, OrdinaryRegionFidelityClassification.PASS_FIDELITY, 24, 24, 0),
        )
    }
}

data class OrdinaryRegionCapabilityAcceptanceRecord(
    val recordId: String,
    val capabilityDigest: String,
    val evidence: RegionTranscriptionCapabilityAcceptanceEvidenceV1,
    val promotingBuildCommit: String,
    val acceptedBy: String,
    val acceptedAt: Instant,
) {
    init {
        require(recordId.matches(SHA256) && capabilityDigest.matches(SHA256))
        require(promotingBuildCommit.matches(COMMIT))
        require(acceptedBy.isNotBlank() && acceptedBy.length <= 256)
        require(recordId == identity(capabilityDigest, evidence, promotingBuildCommit, acceptedBy, acceptedAt))
    }
    companion object {
        private val SHA256 = Regex("^[0-9a-f]{64}$"); private val COMMIT = Regex("^[0-9a-f]{40}$")
        fun create(capability: OrdinaryRegionCapabilityIdentity, evidence: RegionTranscriptionCapabilityAcceptanceEvidenceV1, commit: String,
            acceptedBy: String, acceptedAt: Instant) = OrdinaryRegionCapabilityAcceptanceRecord(
            identity(capability.digest(), evidence, commit, acceptedBy, acceptedAt), capability.digest(),
            evidence, commit, acceptedBy, acceptedAt)
        private fun identity(capability: String, evidence: RegionTranscriptionCapabilityAcceptanceEvidenceV1, commit: String, by: String, at: Instant) =
            ordinaryRegionDigest("parker.region-capability-acceptance.v2", capability, *evidence.fields().toTypedArray(), commit, by, at.toString())
    }
}

class FileSystemOrdinaryRegionCapabilityAcceptanceStore(storageRoot: Path) {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root)) }

    fun admit(record: OrdinaryRegionCapabilityAcceptanceRecord) {
        val existing = findExact(OrdinaryRegionCapabilityIdentity(), record.promotingBuildCommit)
        if (existing != null) {
            require(existing == record) { "capability acceptance conflict" }
            return
        }
        val target = root.resolve("${record.recordId}.region-capability-acceptance-v2")
        createOnce(target, encode(record), "capability acceptance conflict")
    }

    fun findExact(capability: OrdinaryRegionCapabilityIdentity, runtimeCommit: String): OrdinaryRegionCapabilityAcceptanceRecord? {
        require(runtimeCommit.matches(Regex("^[0-9a-f]{40}$")))
        val records = try {
            Files.list(root).use { stream -> stream.filter { it.fileName.toString().endsWith(".region-capability-acceptance-v2") }
                .map { decode(Files.readString(it)) }.toList() }
        } catch (e: Exception) { throw IllegalStateException("capability acceptance store unavailable or corrupt", e) }
        return records.filter { it.capabilityDigest == capability.digest() && it.promotingBuildCommit == runtimeCommit }
            .singleOrNull().also { if (records.count { r -> r.capabilityDigest == capability.digest() && r.promotingBuildCommit == runtimeCommit } > 1)
                error("ambiguous capability acceptance") }
    }

    private fun encode(r: OrdinaryRegionCapabilityAcceptanceRecord): String {
        val fields = listOf(r.recordId, r.capabilityDigest) + r.evidence.fields() + listOf(r.promotingBuildCommit,
            b64(r.acceptedBy), r.acceptedAt.toString())
        val payload = fields.joinToString("\t"); return "$payload\t${ordinaryRegionDigest(payload)}\n"
    }
    private fun decode(text: String): OrdinaryRegionCapabilityAcceptanceRecord {
        require(text.endsWith('\n') && text.count { it == '\n' } == 1)
        val p = text.trimEnd().split('\t'); require(p.size == 30)
        val payload = p.take(29).joinToString("\t"); require(p[29] == ordinaryRegionDigest(payload))
        fun role(i: Int) = OrdinaryRegionAcceptanceEvidenceRole.valueOf(p[i])
        val evidence = RegionTranscriptionCapabilityAcceptanceEvidenceV1(
            OrdinaryRegionLiveR69Evidence(role(2), p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], p[11]),
            OrdinaryRegionR69AEvidence(role(12), p[13], p[14]),
            OrdinaryRegionR69BEvidence(role(15), p[16], p[17], p[18].toInt()),
            OrdinaryRegionR69CEvidence(role(19), p[20], p[21], OrdinaryRegionFidelityClassification.valueOf(p[22]),
                p[23].toInt(), p[24].toInt(), p[25].toInt()),
        )
        return OrdinaryRegionCapabilityAcceptanceRecord(p[0], p[1], evidence, p[26], unb64(p[27]), Instant.parse(p[28]))
    }
}

sealed interface OrdinaryRegionCapabilityAcceptanceEvaluation {
    data class Accepted(val record: OrdinaryRegionCapabilityAcceptanceRecord) : OrdinaryRegionCapabilityAcceptanceEvaluation
    data object NotAccepted : OrdinaryRegionCapabilityAcceptanceEvaluation
}

/** Performs a fresh durable lookup on every call; no accepted-state cache exists. */
class OrdinaryRegionCapabilityAcceptanceEvaluator(
    private val store: FileSystemOrdinaryRegionCapabilityAcceptanceStore,
    private val capability: OrdinaryRegionCapabilityIdentity,
    private val runtimeEmbeddedParkerSourceCommit: () -> String?,
) {
    fun evaluate(): OrdinaryRegionCapabilityAcceptanceEvaluation {
        val commit = runtimeEmbeddedParkerSourceCommit() ?: return OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted
        if (!commit.matches(Regex("^[0-9a-f]{40}$"))) return OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted
        return try { store.findExact(capability, commit)?.let(OrdinaryRegionCapabilityAcceptanceEvaluation::Accepted)
            ?: OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted }
        catch (_: Exception) { OrdinaryRegionCapabilityAcceptanceEvaluation.NotAccepted }
    }
}

data class OrdinaryRegionCapabilityPromotionRequest(val capabilityId: String, val promotingBuildCommit: String)
sealed interface OrdinaryRegionCapabilityPromotionOutcome {
    data class Created(val record: OrdinaryRegionCapabilityAcceptanceRecord) : OrdinaryRegionCapabilityPromotionOutcome
    data class Existing(val record: OrdinaryRegionCapabilityAcceptanceRecord) : OrdinaryRegionCapabilityPromotionOutcome
    data class V8Created(val record: OrdinaryRequestRegionV8CapabilityAcceptanceRecord) : OrdinaryRegionCapabilityPromotionOutcome
    data class V8Existing(val record: OrdinaryRequestRegionV8CapabilityAcceptanceRecord) : OrdinaryRegionCapabilityPromotionOutcome
    data class Blocked(val reason: String) : OrdinaryRegionCapabilityPromotionOutcome
}
fun interface OrdinaryRegionCapabilityPromotionPort { fun create(request:OrdinaryRegionCapabilityPromotionRequest):OrdinaryRegionCapabilityPromotionOutcome }

/** Reconstructs the fixed accepted R6.9 chain from durable provider state; never performs transport. */
fun interface OrdinaryRegionR69EvidenceLoader { fun load(): OrdinaryRegionLiveR69Evidence }

class DurableOrdinaryRegionR69EvidenceLoader(
    private val providerState: FileSystemRegionProviderStateStore,
    private val authorities: FileSystemRegionAcceptanceAuthorityStorageV2,
    private val attempts: FileSystemFidelityFirstAttemptLedger,
) : OrdinaryRegionR69EvidenceLoader {
    override fun load(): OrdinaryRegionLiveR69Evidence {
        val authority = requireNotNull(authorities.load(R69_AUTHORITY_ID)) { "R6.9 authority missing" }
        require(authority.authorityId == R69_AUTHORITY_ID && authority.executionId == R69_EXECUTION_ID)
        require(authority.recordId == R69_AUTHORITY_RECORD_ID) { "R6.9 authority identity mismatch" }
        require(authority.maximumProviderAttempts == 1 && authority.purpose.code == RegionAcceptancePurposeCode.CONTROLLED_LIVE_FIDELITY_ACCEPTANCE)
        val facts = authority.manifest.facts.associate { it.name to it.value }
        require(facts.getValue("request.correlation_id") == "correlation-fa-9-4p-a1e-r6-8c1")
        require(facts.getValue("request.schema_version") == "4")
        require(facts.getValue("provider.wire_version") == "4")
        require(facts.getValue("provider.adapter_version") == "3.0.0")
        require(facts.getValue("provider.profile_id") == "openai-region-anchored-transcription-v1")
        val expectedAttempt = FidelityFirstExecutionIdentity(
            authority.executionId, R69_REQUEST_DIGEST, facts.getValue("request.correlation_id"),
            facts.getValue("source.evidence_artifact_id"), facts.getValue("source.sha256"),
            facts.getValue("source.byte_length").toLong(), facts.getValue("source.media_type"),
            facts.getValue("deployment.runtime_commit"), facts.getValue("provider.name"),
            facts.getValue("provider.model"), facts.getValue("provider.profile_id"),
            facts.getValue("adapter.provider_instruction_sha256"), facts.getValue("request.schema_sha256"),
            facts.getValue("request.processing_profile"), facts.getValue("provider.adapter_version"),
        )
        val attempt = requireNotNull(attempts.readExisting(R69_EXECUTION_ID)) { "R6.9 attempt missing" }
        require(attempt.identity == expectedAttempt) { "R6.9 authority-to-attempt linkage mismatch" }
        require(attempt.stages.lastOrNull() == FidelityFirstAttemptStage.PROVIDER_RESPONSE_RECEIVED)
        require(attempt.stages.count { it == FidelityFirstAttemptStage.PROVIDER_ATTEMPT_STARTED } == 1)
        require(attempt.stages.count { it == FidelityFirstAttemptStage.PROVIDER_RESPONSE_RECEIVED } == 1)

        val recovered = providerState.read(R69_PROVIDER_STATE_ID)
        require(recovered.requestDigest == attempt.identity.requestId)
        require(recovered.recordId == R69_PROVIDER_STATE_ID)
        require(recovered.rawDigest == R69_RAW_RESPONSE_DIGEST)
        require(recovered.recordDigest == R69_PROVIDER_RECORD_DIGEST)
        require(recovered.structuredDigest == R69_STRUCTURED_STATE_DIGEST)
        require(recovered.assessmentDigest == R69_ASSESSMENT_DIGEST)
        require(recovered.outcomeCode == "VALIDATION_MALFORMED_SCHEMA")
        require(!recovered.downstreamProcessingPending)
        @Suppress("UNCHECKED_CAST")
        val provenance = recovered.exactStructuredState?.get("provider_provenance") as? Map<String, Any?>
            ?: error("R6.9 provider result incomplete")
        require(provenance["provider"] == "OpenAI")
        require(provenance["provider_response_id"] == null) // historical v4 projection; identity remains in verified raw envelope
        @Suppress("UNCHECKED_CAST")
        val raw = RegionJson.parse(String(recovered.rawBytes, StandardCharsets.UTF_8)) as? Map<String, Any?>
            ?: error("R6.9 raw provider response malformed")
        val responseId = raw["id"] as? String ?: error("R6.9 provider response identity missing")
        require(responseId == R69_PROVIDER_RESPONSE_ID)
        require(raw["model"] == "gpt-5.6-sol" && raw["status"] == "completed" && raw["error"] == null)
        @Suppress("UNCHECKED_CAST")
        val usage = raw["usage"] as? Map<String, Any?> ?: error("R6.9 provider usage missing")
        require((usage["input_tokens"] as? Number)?.toInt() == R69_INPUT_TOKENS)
        require((usage["output_tokens"] as? Number)?.toInt() == R69_OUTPUT_TOKENS)
        require((usage["total_tokens"] as? Number)?.toInt() == R69_TOTAL_TOKENS)
        return OrdinaryRegionLiveR69Evidence(
            OrdinaryRegionAcceptanceEvidenceRole.R6_9_LIVE_PROVIDER_RESULT,
            R69_AUTHORITY_ID, R69_EXECUTION_ID, recovered.requestDigest, responseId, recovered.recordId,
            recovered.rawDigest, requireNotNull(recovered.structuredDigest), recovered.recordDigest,
            requireNotNull(recovered.assessmentDigest),
        )
    }
}

class OrdinaryRegionCapabilityAcceptanceCoordinator(
    private val store: FileSystemOrdinaryRegionCapabilityAcceptanceStore,
    private val evidenceLoader: OrdinaryRegionR69EvidenceLoader,
    private val runtimeEmbeddedParkerSourceCommit: () -> String?,
    private val acceptedBy: String,
    private val now: () -> Instant = Instant::now,
) : OrdinaryRegionCapabilityPromotionPort {
    override fun create(request: OrdinaryRegionCapabilityPromotionRequest): OrdinaryRegionCapabilityPromotionOutcome {
        if (request.capabilityId != ORDINARY_REGION_CAPABILITY_ID) return blocked("CAPABILITY_ID_MISMATCH")
        if (!request.promotingBuildCommit.matches(Regex("^[0-9a-f]{40}$"))) return blocked("BUILD_COMMIT_MALFORMED")
        if (runtimeEmbeddedParkerSourceCommit() != request.promotingBuildCommit) return blocked("BUILD_COMMIT_MISMATCH")
        val capability = OrdinaryRegionCapabilityIdentity()
        val existing = runCatching { store.findExact(capability, request.promotingBuildCommit) }
            .getOrElse { return blocked("ACCEPTANCE_STORE_UNAVAILABLE_OR_CORRUPT") }
        if (existing != null) return OrdinaryRegionCapabilityPromotionOutcome.Existing(existing)
        val live = runCatching { evidenceLoader.load() }
            .getOrElse { return blocked("R6_9_PROVIDER_EVIDENCE_UNAVAILABLE_OR_CORRUPT") }
        val evidence = runCatching { RegionTranscriptionCapabilityAcceptanceEvidenceV1.governed(live) }
            .getOrElse { return blocked("R6_9_EVIDENCE_CHAIN_MISMATCH") }
        val record = runCatching { OrdinaryRegionCapabilityAcceptanceRecord.create(
            capability, evidence, request.promotingBuildCommit, acceptedBy, now())
        }.getOrElse { return blocked("ACCEPTANCE_RECORD_INVALID") }
        return try {
            store.admit(record)
            OrdinaryRegionCapabilityPromotionOutcome.Created(record)
        } catch (_: Exception) { blocked("ACCEPTANCE_CREATE_CONFLICT") }
    }
    private fun blocked(reason: String) = OrdinaryRegionCapabilityPromotionOutcome.Blocked(reason)
}

enum class OrdinaryRegionAuthorizationState { AVAILABLE, RESERVED_FOR_EXECUTION, CONSUMED_BY_PROVIDER_ATTEMPT }

data class OrdinaryRegionOwnerAuthorization(
    val authorizationId: String,
    val evidenceArtifactId: String,
    val sourceSha256: String,
    val capabilityDigest: String,
    val provider: String,
    val purpose: String,
    val transmittedScope: String,
    val disclosure: String,
    val approvedBy: String,
    val approvedAt: Instant,
    val expiresAt: Instant,
) {
    init {
        require(authorizationId.matches(Regex("^[A-Za-z0-9_.-]{1,120}$")))
        require(listOf(sourceSha256, capabilityDigest).all { it.matches(Regex("^[0-9a-f]{64}$")) })
        require(provider == "OpenAI" && purpose.isNotBlank() && transmittedScope.isNotBlank())
        require(disclosure.contains("OpenAI") && expiresAt.isAfter(approvedAt))
    }
}

data class OrdinaryRegionAuthorizationSnapshot(
    val grant: OrdinaryRegionOwnerAuthorization,
    val state: OrdinaryRegionAuthorizationState,
    val executionId: String? = null,
    val reservedAt: Instant? = null,
    val revokedAt: Instant? = null,
    val revocationPostAttempt: Boolean = false,
)

class OrdinaryRegionAuthorizationGuard(storageRoot: Path) {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isWritable(root)) }
    fun <T> locked(authorizationId: String, action: () -> T): T {
        val safe = ordinaryRegionDigest("authorization-guard", authorizationId)
        val path = root.resolve(".$safe.authorization.lock")
        val local = LOCAL_LOCKS.computeIfAbsent(path) { java.util.concurrent.locks.ReentrantLock() }
        local.lock()
        return try {
            FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                .use { channel -> channel.lock().use { action() } }
        } finally { local.unlock() }
    }
    private companion object { val LOCAL_LOCKS = java.util.concurrent.ConcurrentHashMap<Path, java.util.concurrent.locks.ReentrantLock>() }
}

class FileSystemOrdinaryRegionAuthorizationStore(storageRoot: Path) {
    private val root = storageRoot.toAbsolutePath().normalize()
    init { require(Files.isDirectory(root) && Files.isReadable(root) && Files.isWritable(root)) }

    fun create(grant: OrdinaryRegionOwnerAuthorization) = createOnce(base(grant.authorizationId), encodeGrant(grant), "authorization identity conflict")

    fun loadIfPresent(id: String): OrdinaryRegionAuthorizationSnapshot? =
        if (Files.exists(base(id))) load(id) else null

    fun load(id: String, providerAttemptStarted: Boolean = false): OrdinaryRegionAuthorizationSnapshot {
        val grant = decodeGrant(Files.readString(base(id)))
        val events = event(id).let { if (Files.exists(it)) Files.readAllLines(it) else emptyList() }.map(::decodeEvent)
        val reservation = events.filter { it[0] == "RESERVED" }.singleOrNull()
        val revocation = events.filter { it[0] == "REVOKED" }.singleOrNull()
        return OrdinaryRegionAuthorizationSnapshot(grant,
            if (providerAttemptStarted) OrdinaryRegionAuthorizationState.CONSUMED_BY_PROVIDER_ATTEMPT
            else if (reservation != null) OrdinaryRegionAuthorizationState.RESERVED_FOR_EXECUTION else OrdinaryRegionAuthorizationState.AVAILABLE,
            reservation?.get(1), reservation?.get(2)?.let(Instant::parse), revocation?.get(1)?.let(Instant::parse),
            revocation?.get(2)?.toBooleanStrictOrNull() ?: false)
    }

    fun reserve(id: String, executionId: String, now: Instant): OrdinaryRegionAuthorizationSnapshot {
        val current = load(id)
        require(current.revokedAt == null) { "OWNER_AUTHORIZATION_REVOKED" }
        if (current.executionId != null) {
            require(current.executionId == executionId) { "authorization reserved to different execution" }
            return current
        }
        require(now.isBefore(current.grant.expiresAt)) { "OWNER_AUTHORIZATION_EXPIRED_BEFORE_RESERVATION" }
        appendEvent(id, listOf("RESERVED", executionId, now.toString()))
        return load(id)
    }

    fun revoke(id: String, now: Instant, providerAttemptStarted: Boolean): OrdinaryRegionAuthorizationSnapshot {
        val current = load(id, providerAttemptStarted)
        if (current.revokedAt != null) return current
        appendEvent(id, listOf("REVOKED", now.toString(), providerAttemptStarted.toString()))
        return load(id, providerAttemptStarted)
    }

    private fun appendEvent(id: String, fields: List<String>) {
        val payload = fields.joinToString("\t"); val bytes = "$payload\t${ordinaryRegionDigest(payload)}\n".toByteArray()
        FileChannel.open(event(id), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND).use {
            val b = ByteBuffer.wrap(bytes); while (b.hasRemaining()) it.write(b); it.force(true)
        }
        forceDirectory(root)
    }
    private fun decodeEvent(line: String): List<String> { val p = line.split('\t'); require(p.size >= 3); val body = p.dropLast(1).joinToString("\t"); require(p.last() == ordinaryRegionDigest(body)); return p.dropLast(1) }
    private fun encodeGrant(g: OrdinaryRegionOwnerAuthorization): String {
        val f = listOf(g.authorizationId, g.evidenceArtifactId, g.sourceSha256, g.capabilityDigest, g.provider,
            b64(g.purpose), b64(g.transmittedScope), b64(g.disclosure), b64(g.approvedBy), g.approvedAt.toString(), g.expiresAt.toString())
        val p = f.joinToString("\t"); return "$p\t${ordinaryRegionDigest(p)}\n"
    }
    private fun decodeGrant(text: String): OrdinaryRegionOwnerAuthorization {
        val p = text.trimEnd().split('\t'); require(p.size == 12); val body = p.take(11).joinToString("\t"); require(p.last() == ordinaryRegionDigest(body))
        return OrdinaryRegionOwnerAuthorization(p[0], p[1], p[2], p[3], p[4], unb64(p[5]), unb64(p[6]), unb64(p[7]), unb64(p[8]), Instant.parse(p[9]), Instant.parse(p[10]))
    }
    private fun base(id: String) = root.resolve("$id.region-owner-authorization-v1").normalize().also { require(it.parent == root) }
    private fun event(id: String) = root.resolve("$id.region-owner-authorization-events-v1").normalize().also { require(it.parent == root) }
}

enum class OrdinaryRegionDisposition {
    UNSUPPORTED_MEDIA, NO_TRANSCRIBABLE_REGIONS, REQUEST_BOUNDS_EXCEEDED,
    SOURCE_ORDER_REVIEW_REQUIRED, SOURCE_ORDER_NOT_SUPPORTED, CAPABILITY_NOT_ACCEPTED,
    OWNER_AUTHORIZATION_REQUIRED, OWNER_AUTHORIZATION_REVOKED,
    OWNER_AUTHORIZATION_EXPIRED_BEFORE_RESERVATION, PROVIDER_OUTCOME_UNKNOWN,
    PROVIDER_RESPONSE_AVAILABLE, VALIDATION_FAILED, ADMISSION_CONFLICT, ADMITTED, REVIEW_REQUIRED,
    SOURCE_UNAVAILABLE, EXECUTION_CONFLICT,
}

data class OrdinaryRegionProposal(
    val evidenceArtifactId: String,
    val capabilityId: String,
    val provider: String = "OpenAI",
    val disclosure: String = "Selected authoritative PDF evidence crops will be transmitted to OpenAI for literal transcription.",
    val executing: Boolean = false,
    val capabilityStatus: OrdinaryRegionCapabilityStatus,
)

enum class OrdinaryRegionOwnerAuthorizationDisposition { NOT_AUTHORISED, AUTHORISED, UNAVAILABLE }

data class OrdinaryRegionOwnerAuthorizationView(
    val disposition: OrdinaryRegionOwnerAuthorizationDisposition,
    val evidenceArtifactId: String,
    val provider: String = "OpenAI",
    val disclosure: String = "Selected authoritative PDF evidence crops will be transmitted to OpenAI for literal transcription.",
    val authorizationId: String? = null,
    val approvedAt: Instant? = null,
    val expiresAt: Instant? = null,
    val detail: String? = null,
    val executionState: String = "NOT_STARTED",
)

sealed interface OrdinaryRegionOwnerAuthorizationOutcome {
    data class Created(val view: OrdinaryRegionOwnerAuthorizationView) : OrdinaryRegionOwnerAuthorizationOutcome
    data class Existing(val view: OrdinaryRegionOwnerAuthorizationView) : OrdinaryRegionOwnerAuthorizationOutcome
    data class Blocked(val view: OrdinaryRegionOwnerAuthorizationView) : OrdinaryRegionOwnerAuthorizationOutcome
}

enum class OrdinaryRegionCapabilityDisposition { CAPABILITY_NOT_ACCEPTED, ACCEPTED, NOT_CONFIGURED }
data class OrdinaryRegionCapabilityStatus(
    val capabilityId: String, val provider: String, val endpointOperation: String, val model: String,
    val adapterId: String, val adapterVersion: String, val providerProfile: String, val wireVersion: Int,
    val mediaType: String, val maximumRegions: Int, val aggregateRequestBodyMaximumBytes: Int,
    val batching: Boolean, val disposition: OrdinaryRegionCapabilityDisposition,
    val runtimeEmbeddedBuildCommit: String?, val acceptedPromotingBuildCommit: String?,
)

fun evaluateOrdinaryRegionCapabilityStatus(
    capability: OrdinaryRegionCapabilityIdentity,
    acceptance: OrdinaryRegionCapabilityAcceptanceEvaluator,
    runtimeCommit: () -> String,
): OrdinaryRegionCapabilityStatus {
    val accepted = acceptance.evaluate() as? OrdinaryRegionCapabilityAcceptanceEvaluation.Accepted
    return OrdinaryRegionCapabilityStatus(
        capability.capabilityId, capability.provider, capability.endpointOperation, capability.model,
        capability.adapterId, capability.adapterVersion, capability.providerProfile, capability.wireVersion,
        capability.mediaType, capability.maximumRegions, capability.aggregateRequestBodyMaximumBytes,
        capability.batching,
        if (accepted == null) OrdinaryRegionCapabilityDisposition.CAPABILITY_NOT_ACCEPTED else OrdinaryRegionCapabilityDisposition.ACCEPTED,
        runtimeCommit(), accepted?.record?.promotingBuildCommit,
    )
}

data class OrdinaryRegionPreparedRequest(
    val binding: GovernedRegionExecutionBinding,
    val pages: List<AuthoritativePageRepresentation>,
    val graphs: List<SourceRegionOrderGraph>,
    val exactRequestBodyBytes: Int,
)

sealed interface OrdinaryRegionPreparationOutcome {
    data class Prepared(val value: OrdinaryRegionPreparedRequest) : OrdinaryRegionPreparationOutcome
    data class Blocked(val disposition: OrdinaryRegionDisposition, val detail: String) : OrdinaryRegionPreparationOutcome
}

class OrdinaryRegionRequestPreparer(
    private val renderer: DeterministicSourcePageRenderer = DeterministicSourcePageRenderer(),
    private val deriver: SourceRegionDeriver = DeterministicSourceRegionDeriver(),
    private val bodyEncoder: (RegionTranscriptionRequest) -> String,
    private val requestDigest: (RegionTranscriptionRequest) -> String,
    private val maximumRequestBodyBytes: Int = ORDINARY_REGION_MAX_REQUEST_BODY_BYTES,
) {
    init { require(maximumRequestBodyBytes > 0) }
    internal fun prepare(source: AuthoritativeAcquisitionInput, executionId: String, attemptId: String,
        repositoryCommit: String): OrdinaryRegionPreparationOutcome {
        if (source.mediaType != "application/pdf") return blocked(OrdinaryRegionDisposition.UNSUPPORTED_MEDIA, "ordinary region-v5 accepts application/pdf only")
        val bytes = source.bytes()
        val profile = PageRenderProfile("authoritative-page-region-raster-v1", 1, 300)
        val first = renderer.render(SourcePageRenderRequest(source.evidenceArtifactId, source.sha256, "application/pdf", bytes, 1, profile))
        val firstPage = (first as? SourcePageRepresentationOutcome.Created)?.representation
            ?: return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED, "PDF rendering failed or exceeded governed source/page bounds")
        val pages = mutableListOf(firstPage)
        for (number in 2..firstPage.provenance.declaredPageCount) {
            val rendered = renderer.render(SourcePageRenderRequest(source.evidenceArtifactId, source.sha256, "application/pdf", bytes, number, profile))
            pages += (rendered as? SourcePageRepresentationOutcome.Created)?.representation
                ?: return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED, "PDF rendering failed or exceeded governed source/page bounds")
        }
        val graphs = pages.map { page ->
            when (val derived = deriver.derive(page)) {
                is SourceRegionDerivationOutcome.Derived -> derived.graph
                SourceRegionDerivationOutcome.ExcessiveRegions -> return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED, "region derivation exceeded its bound")
                else -> return blocked(OrdinaryRegionDisposition.REVIEW_REQUIRED, "deterministic region derivation failed")
            }
        }
        graphs.firstOrNull { it.ambiguityState == SourceRegionAmbiguityState.HUMAN_ORDER_REQUIRED }?.let {
            return blocked(OrdinaryRegionDisposition.SOURCE_ORDER_REVIEW_REQUIRED, it.reason ?: "human source-order review required")
        }
        graphs.firstOrNull { it.ambiguityState == SourceRegionAmbiguityState.NOT_YET_SUPPORTED }?.let {
            return blocked(OrdinaryRegionDisposition.SOURCE_ORDER_NOT_SUPPORTED, it.reason ?: "source order not supported")
        }
        val targets = pages.zip(graphs).flatMap { (page, graph) -> graph.regions.map { target(source, page, it) } }
        if (targets.isEmpty()) return blocked(OrdinaryRegionDisposition.NO_TRANSCRIBABLE_REGIONS, "Parker derived no transcribable regions")
        if (targets.size > RegionTranscriptionRequest.MAX_REGIONS_PER_REQUEST) return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED, "complete region set exceeds 32")
        val request = RegionTranscriptionRequest(attemptId, REGION_TRANSCRIPTION_PROFILE_ID, REGION_TRANSCRIPTION_SCHEMA_ID,
            REGION_TRANSCRIPTION_WIRE_VERSION, REGION_TRANSCRIPTION_SCHEMA_SHA256, REGION_TRANSCRIPTION_PROCESSING_PROFILE,
            REGION_LITERAL_TRANSCRIPTION_INSTRUCTION, targets)
        val bodySize = bodyEncoder(request).toByteArray(StandardCharsets.UTF_8).size
        if (bodySize > maximumRequestBodyBytes) return blocked(OrdinaryRegionDisposition.REQUEST_BOUNDS_EXCEEDED,
            "exact UTF-8 request body exceeds $maximumRequestBodyBytes bytes")
        val identity = FidelityFirstExecutionIdentity(executionId, requestDigest(request), attemptId,
            source.evidenceArtifactId.value, source.sha256, source.byteLength, "application/pdf", repositoryCommit,
            "OpenAI", OPENAI_REGION_MODEL, OPENAI_REGION_PROFILE_ID, OPENAI_REGION_INSTRUCTION_SHA256,
            REGION_TRANSCRIPTION_SCHEMA_SHA256, REGION_TRANSCRIPTION_PROCESSING_PROFILE, OPENAI_REGION_ADAPTER_VERSION)
        val order = RegionSourceOrderReconstructor().order(graphs).getOrElse { return blocked(OrdinaryRegionDisposition.REVIEW_REQUIRED, it.message ?: "invalid source-order graph") }
        return OrdinaryRegionPreparationOutcome.Prepared(OrdinaryRegionPreparedRequest(
            GovernedRegionExecutionBinding(identity, request, order), pages, graphs, bodySize))
    }

    private fun target(source: AuthoritativeAcquisitionInput, page: AuthoritativePageRepresentation, region: SourceRegion): RegionTranscriptionTarget {
        val crop = renderer.crop(page, region.bounds); val encoded = png(crop.dimensions, crop.canonicalPixels())
        return RegionTranscriptionTarget(source.evidenceArtifactId, source.sha256, page.id, page.provenance.pageNumber,
            page.provenance.pixelDimensions, region.id, region.bounds, region.cropDigest, region.structuralClass,
            region.provenance.derivationProfileId, region.provenance.derivationProfileVersion,
            RegionTranscriptionImage(page.id, region.bounds, region.cropDigest, "image/png", RegionTranscriptionImage.sha256(encoded), encoded), null)
    }
    private fun png(d: PagePixelDimensions, p: ByteArray): ByteArray {
        val image = BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB); var i = 0
        for (y in 0 until d.height) for (x in 0 until d.width) image.setRGB(x, y,
            ((p[i++].toInt() and 255) shl 16) or ((p[i++].toInt() and 255) shl 8) or (p[i++].toInt() and 255))
        return ByteArrayOutputStream().use { out -> check(ImageIO.write(image, "png", out)); out.toByteArray() }
    }
    private fun blocked(d: OrdinaryRegionDisposition, detail: String) = OrdinaryRegionPreparationOutcome.Blocked(d, detail)
}

class RegionSourceOrderReconstructor {
    fun order(graphs: List<SourceRegionOrderGraph>): Result<List<SourceRegionId>> = runCatching {
        graphs.sortedBy { it.regions.firstOrNull()?.provenance?.pageNumber ?: Int.MAX_VALUE }.flatMap(::orderPage)
    }
    private fun orderPage(graph: SourceRegionOrderGraph): List<SourceRegionId> {
        require(graph.ambiguityState == SourceRegionAmbiguityState.UNAMBIGUOUS)
        val regions = graph.regions.associateBy { it.id }; require(regions.size == graph.regions.size)
        val outgoing = regions.keys.associateWith { mutableSetOf<SourceRegionId>() }.toMutableMap()
        val indegree = regions.keys.associateWith { 0 }.toMutableMap()
        graph.edges.forEach { edge -> if (outgoing.getValue(edge.from).add(edge.to)) indegree[edge.to] = indegree.getValue(edge.to) + 1 }
        val comparator = compareBy<SourceRegionId>({ regions.getValue(it).bounds.top }, { regions.getValue(it).bounds.left }, { it.value })
        val ready = java.util.PriorityQueue(comparator); indegree.filterValues { it == 0 }.keys.forEach(ready::add)
        val result = mutableListOf<SourceRegionId>()
        while (ready.isNotEmpty()) { val id = ready.remove(); result += id; outgoing.getValue(id).forEach { next ->
            indegree[next] = indegree.getValue(next) - 1; if (indegree.getValue(next) == 0) ready += next } }
        require(result.size == regions.size) { "source-order graph contains a cycle or inconsistency" }
        return result
    }
    fun reconstruct(result: RegionTranscriptionResult, graphs: List<SourceRegionOrderGraph>): Result<List<RegionTranscriptionBlock>> = runCatching {
        val ordered = order(graphs).getOrThrow(); val blocks = result.blocksInProviderOrder
        require(blocks.map { it.sourceRegionId }.distinct().size == blocks.size) { "duplicate provider region" }
        require(blocks.map { it.sourceRegionId }.toSet() == ordered.toSet()) { "missing or unknown provider region" }
        val byId = blocks.associateBy { it.sourceRegionId }; ordered.map(byId::getValue)
    }
}

data class OrdinaryRegionReconstruction(
    val orderedBlocks: List<RegionTranscriptionBlock>,
    val contentDigest: String,
)

fun reconstructOrdinaryRegion(result: RegionTranscriptionResult, graphs: List<SourceRegionOrderGraph>): Result<OrdinaryRegionReconstruction> =
    RegionSourceOrderReconstructor().reconstruct(result, graphs).map { blocks ->
        val fields = blocks.flatMap { block -> listOf(block.sourceRegionId.value, block.pageNumber.toString(), block.literalText ?: "<null>",
            block.status.name, block.providerReturnedOrdinal.toString(),
            block.uncertainties.joinToString("|") { "${it.startCodePoint}:${it.endCodePointExclusive}:${it.exactSubstring}:${it.category}" },
            block.visualObservations.joinToString("|") { "${it.kind}:${it.startCodePoint}:${it.endCodePointExclusive}" }) }
        OrdinaryRegionReconstruction(blocks, ordinaryRegionDigest("parker.region-transcription.reconstructed.v1", *fields.toTypedArray()))
    }

sealed interface OrdinaryRegionAdmissionOutcome {
    data class Admitted(val record: DerivativeGenerationRecord, val recovered: Boolean) : OrdinaryRegionAdmissionOutcome
    data class Conflict(val reason: String) : OrdinaryRegionAdmissionOutcome
}

class OrdinaryRegionDerivativeAdmission(
    private val generationStorage: DerivativeGenerationStorage,
    private val contentStorage: DerivativeContentStorage,
    private val audit: DocumentIngestionAudit,
    private val now: () -> Instant = Instant::now,
) {
    suspend fun admit(payload: OrdinaryRegionTranscriptionDerivative, principal: PrincipalId): OrdinaryRegionAdmissionOutcome {
        val id = DerivativeGenerationId("region-${payload.canonicalGenerationKeyDigest}")
        val entry = DerivativeContentEntry(id, EvidenceArtifactId(payload.evidenceArtifactId), TierADerivativePayload.RegionTranscription(payload))
        val existingContent = runCatching { contentStorage.retrieve(id) }.getOrElse { return conflict("content state unreadable") }
        if (existingContent != null && existingContent != entry) return conflict("content identity conflict")
        val existingGeneration = runCatching { generationStorage.retrieve(id) }.getOrElse { return conflict("generation state unreadable") }
        if (existingGeneration != null) {
            if (existingContent == null || !matches(existingGeneration, payload)) return conflict("generation provenance conflict")
            return OrdinaryRegionAdmissionOutcome.Admitted(existingGeneration, true)
        }
        if (existingContent == null) try { contentStorage.prepare(entry); contentStorage.publishPrepared(id) }
        catch (_: Exception) { if (runCatching { contentStorage.retrieve(id) }.getOrNull() != entry) return conflict("content publication conflict") }
        val record = record(id, payload)
        try {
            audit.record(audit(payload, principal, id, DocumentIngestionAuditStage.ADMISSION_AUTHORISED))
            generationStorage.prepare(record); generationStorage.publishPrepared(id)
            audit.record(audit(payload, principal, id, DocumentIngestionAuditStage.ADMITTED))
        } catch (_: Exception) {
            val recovered = runCatching { generationStorage.retrieve(id) }.getOrNull()
            if (recovered == null || !matches(recovered, payload)) return conflict("generation publication conflict")
            return OrdinaryRegionAdmissionOutcome.Admitted(recovered, true)
        }
        return OrdinaryRegionAdmissionOutcome.Admitted(record, false)
    }
    private fun record(id: DerivativeGenerationId, p: OrdinaryRegionTranscriptionDerivative) = DerivativeGenerationRecord(id,
        EvidenceArtifactId(p.evidenceArtifactId), listOf(DerivativeParentReference.RootEvidenceArtifact(EvidenceArtifactId(p.evidenceArtifactId))),
        if(p.capabilityId==ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID)"ordinary request-region-v8 transcription" else "ordinary region-v5 transcription", DerivativeProducerIdentity("parker-region-transcription", if(p.representationVersion==2)"2.0.0" else "1.0.0",
            p.providerProfile, p.adapterId, p.adapterVersion, p.model, p.model),
        listOf(DerivativeTransformation.PAGE_RENDERING, DerivativeTransformation.MODEL_INFERENCE,
            DerivativeTransformation.READING_ORDER_INFERENCE), now(), DerivativeContentIdentity.Digest("SHA-256", p.reconstructedContentDigest),
        DerivativeCompletenessState.ACCOUNTED_FOR, DerivativeOperationalOutcome.USABLE)
    private fun matches(r: DerivativeGenerationRecord, p: OrdinaryRegionTranscriptionDerivative) =
        r.derivativeGenerationId.value == "region-${p.canonicalGenerationKeyDigest}" &&
            r.rootSourceEvidenceArtifactId.value == p.evidenceArtifactId &&
            (r.contentIdentity as? DerivativeContentIdentity.Digest)?.digest == p.reconstructedContentDigest
    private fun audit(p: OrdinaryRegionTranscriptionDerivative, principal: PrincipalId, id: DerivativeGenerationId, stage: DocumentIngestionAuditStage) =
        DocumentIngestionAuditRecord(p.executionIdentity, EvidenceArtifactId(p.evidenceArtifactId), principal,
            if(p.capabilityId==ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID)"ordinary request-region-v8 transcription" else "ordinary region-v5 transcription", now(), id, stage)
    private fun conflict(reason: String) = OrdinaryRegionAdmissionOutcome.Conflict(reason)
}

fun ordinaryRegionGenerationKey(executionId: String, evidenceId: String, sourceSha256: String,
    capabilityAcceptanceId: String, providerStateId: String, contentDigest: String): String =
    ordinaryRegionDigest("parker.region-transcription.generation.v1", executionId, evidenceId, sourceSha256,
        capabilityAcceptanceId, providerStateId, contentDigest)

data class OrdinaryRegionOwnerResult(
    val disposition: OrdinaryRegionDisposition,
    val detail: String,
    val derivativeGenerationId: String? = null,
    val providerCalls: Int? = null,
)

interface OrdinaryRegionOwnerWorkflowPort {
    fun capabilityStatus():OrdinaryRegionCapabilityStatus
    suspend fun proposal(evidenceId:EvidenceArtifactId):OrdinaryRegionProposal?
    suspend fun authorizationStatus(evidenceId:EvidenceArtifactId):OrdinaryRegionOwnerAuthorizationView
    suspend fun authorize(evidenceId:EvidenceArtifactId):OrdinaryRegionOwnerAuthorizationOutcome
    fun createAuthorization(grant:OrdinaryRegionOwnerAuthorization)
    fun reserve(authorizationId:String,executionId:String):OrdinaryRegionAuthorizationSnapshot
    fun revoke(authorizationId:String):OrdinaryRegionAuthorizationSnapshot
    suspend fun execute(evidenceId:EvidenceArtifactId,authorizationId:String,executionId:String,attemptId:String):OrdinaryRegionOwnerResult
}

/** Owner-facing ordinary API. Proposal, authorization and reservation are explicitly non-executing. */
class OrdinaryRegionIngestionWorkflow(
    private val ownerPrincipalId: PrincipalId,
    evidenceCustodian: EvidenceCustodian,
    private val capability: OrdinaryRegionCapabilityIdentity,
    private val acceptance: OrdinaryRegionCapabilityAcceptanceEvaluator,
    private val authorizations: FileSystemOrdinaryRegionAuthorizationStore,
    private val guard: OrdinaryRegionAuthorizationGuard,
    private val ledger: FileSystemFidelityFirstAttemptLedger,
    private val preparer: OrdinaryRegionRequestPreparer,
    private val execution: GovernedRegionTranscriptionExecutionCoordinator,
    private val providerState: FileSystemRegionProviderStateStore,
    private val admission: OrdinaryRegionDerivativeAdmission,
    private val runtimeCommit: () -> String,
    private val now: () -> Instant = Instant::now,
) : OrdinaryRegionOwnerWorkflowPort {
    private val resolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)

    override fun capabilityStatus(): OrdinaryRegionCapabilityStatus {
        return evaluateOrdinaryRegionCapabilityStatus(capability, acceptance, runtimeCommit)
    }

    override suspend fun proposal(evidenceId: EvidenceArtifactId): OrdinaryRegionProposal? =
        when (val source = resolver.resolve(ownerPrincipalId, evidenceId)) {
            is AuthoritativeAcquisitionResolution.Verified -> if (source.input.mediaType == "application/pdf")
                OrdinaryRegionProposal(evidenceId.value, capability.capabilityId, capabilityStatus = capabilityStatus()) else null
            else -> null
        }

    /** Fresh canonical status for the exact evidence/source/build binding; never creates state. */
    override suspend fun authorizationStatus(evidenceId: EvidenceArtifactId): OrdinaryRegionOwnerAuthorizationView {
        if (acceptance.evaluate() !is OrdinaryRegionCapabilityAcceptanceEvaluation.Accepted)
            return authorizationUnavailable(evidenceId, "CAPABILITY_NOT_ACCEPTED")
        val source = verifiedPdf(evidenceId) ?: return authorizationUnavailable(evidenceId, "SOURCE_UNAVAILABLE_OR_UNSUPPORTED")
        val id = authorizationIdentity(source)
        val snapshot = try { authorizations.loadIfPresent(id)?.let { current ->
            val started = current.executionId?.let { ledger.providerAttemptStartedForExecution(it) } ?: false
            authorizations.load(id, started)
        } }
        catch (_: Exception) { return authorizationUnavailable(evidenceId, "AUTHORIZATION_STORE_UNAVAILABLE_OR_CORRUPT") }
        return snapshot?.let(::authorizationView) ?: OrdinaryRegionOwnerAuthorizationView(
            OrdinaryRegionOwnerAuthorizationDisposition.NOT_AUTHORISED, evidenceId.value)
    }

    /** Explicit authorization only. Governed binding fields are reconstructed server-side; no execution occurs. */
    override suspend fun authorize(evidenceId: EvidenceArtifactId): OrdinaryRegionOwnerAuthorizationOutcome {
        if (acceptance.evaluate() !is OrdinaryRegionCapabilityAcceptanceEvaluation.Accepted)
            return OrdinaryRegionOwnerAuthorizationOutcome.Blocked(authorizationUnavailable(evidenceId, "CAPABILITY_NOT_ACCEPTED"))
        val source = verifiedPdf(evidenceId) ?: return OrdinaryRegionOwnerAuthorizationOutcome.Blocked(
            authorizationUnavailable(evidenceId, "SOURCE_UNAVAILABLE_OR_UNSUPPORTED"))
        val id = authorizationIdentity(source)
        return try {
            guard.locked(id) {
                authorizations.loadIfPresent(id)?.let {
                    return@locked OrdinaryRegionOwnerAuthorizationOutcome.Existing(authorizationView(it))
                }
                val approvedAt = now()
                authorizations.create(OrdinaryRegionOwnerAuthorization(
                    authorizationId = id,
                    evidenceArtifactId = evidenceId.value,
                    sourceSha256 = source.sha256,
                    capabilityDigest = capability.digest(),
                    provider = "OpenAI",
                    purpose = "literal transcription",
                    transmittedScope = "Selected authoritative PDF evidence crops",
                    disclosure = "Selected authoritative PDF evidence crops will be transmitted to OpenAI for literal transcription.",
                    approvedBy = ownerPrincipalId.value,
                    approvedAt = approvedAt,
                    expiresAt = approvedAt.plusSeconds(86_400),
                ))
                OrdinaryRegionOwnerAuthorizationOutcome.Created(authorizationView(authorizations.load(id)))
            }
        } catch (_: Exception) {
            OrdinaryRegionOwnerAuthorizationOutcome.Blocked(authorizationUnavailable(evidenceId, "AUTHORIZATION_CREATE_FAILED"))
        }
    }

    override fun createAuthorization(grant: OrdinaryRegionOwnerAuthorization) { require(grant.capabilityDigest == capability.digest()); authorizations.create(grant) }
    override fun reserve(authorizationId: String, executionId: String) = guard.locked(authorizationId) { authorizations.reserve(authorizationId, executionId, now()) }
    override fun revoke(authorizationId: String): OrdinaryRegionAuthorizationSnapshot = guard.locked(authorizationId) {
        val executionId = authorizations.load(authorizationId).executionId
        val started = executionId?.let { runCatching { ledger.providerAttemptStartedForExecution(it) }.getOrDefault(false) } ?: false
        authorizations.revoke(authorizationId, now(), started)
    }

    private suspend fun verifiedPdf(evidenceId: EvidenceArtifactId): AuthoritativeAcquisitionInput? =
        ((resolver.resolve(ownerPrincipalId, evidenceId) as? AuthoritativeAcquisitionResolution.Verified)?.input)
            ?.takeIf { it.mediaType == "application/pdf" }

    private fun authorizationIdentity(source: AuthoritativeAcquisitionInput): String = "ordinary-auth-" + ordinaryRegionDigest(
        "parker.ordinary-region-owner-authorization.ui.v1", source.evidenceArtifactId.value, source.sha256,
        capability.digest(), runtimeCommit(), ownerPrincipalId.value)

    private fun authorizationView(snapshot: OrdinaryRegionAuthorizationSnapshot): OrdinaryRegionOwnerAuthorizationView {
        val detail = when {
            snapshot.revokedAt != null -> "OWNER_AUTHORIZATION_REVOKED"
            !now().isBefore(snapshot.grant.expiresAt) -> "OWNER_AUTHORIZATION_EXPIRED"
            snapshot.state == OrdinaryRegionAuthorizationState.RESERVED_FOR_EXECUTION -> "OWNER_AUTHORIZATION_RESERVED"
            snapshot.state == OrdinaryRegionAuthorizationState.CONSUMED_BY_PROVIDER_ATTEMPT -> "PROVIDER_ATTEMPT_STARTED"
            else -> null
        }
        return OrdinaryRegionOwnerAuthorizationView(
        if (snapshot.revokedAt == null && now().isBefore(snapshot.grant.expiresAt)) OrdinaryRegionOwnerAuthorizationDisposition.AUTHORISED
        else OrdinaryRegionOwnerAuthorizationDisposition.UNAVAILABLE,
        snapshot.grant.evidenceArtifactId,
        snapshot.grant.provider, snapshot.grant.disclosure, snapshot.grant.authorizationId,
        snapshot.grant.approvedAt, snapshot.grant.expiresAt, detail,
        when (snapshot.state) {
            OrdinaryRegionAuthorizationState.AVAILABLE -> "NOT_STARTED"
            OrdinaryRegionAuthorizationState.RESERVED_FOR_EXECUTION -> "RESERVED"
            OrdinaryRegionAuthorizationState.CONSUMED_BY_PROVIDER_ATTEMPT -> "ATTEMPT_STARTED"
        },
    )
    }

    private fun authorizationUnavailable(evidenceId: EvidenceArtifactId, detail: String) =
        OrdinaryRegionOwnerAuthorizationView(OrdinaryRegionOwnerAuthorizationDisposition.UNAVAILABLE, evidenceId.value, detail = detail)

    override suspend fun execute(evidenceId: EvidenceArtifactId, authorizationId: String, executionId: String,
        attemptId: String): OrdinaryRegionOwnerResult {
        val accepted = acceptance.evaluate() as? OrdinaryRegionCapabilityAcceptanceEvaluation.Accepted
            ?: return result(OrdinaryRegionDisposition.CAPABILITY_NOT_ACCEPTED)
        val source = (resolver.resolve(ownerPrincipalId, evidenceId) as? AuthoritativeAcquisitionResolution.Verified)?.input
            ?: return result(OrdinaryRegionDisposition.SOURCE_UNAVAILABLE)
        val existing = try { guard.locked(authorizationId) {
            authorizations.load(authorizationId).also { snapshot ->
                require(snapshot.revokedAt == null) { "OWNER_AUTHORIZATION_REVOKED" }
                require(snapshot.executionId == null || snapshot.executionId == executionId) {
                    "authorization reserved to different execution"
                }
                if (snapshot.executionId == null) require(now().isBefore(snapshot.grant.expiresAt)) {
                    "OWNER_AUTHORIZATION_EXPIRED_BEFORE_RESERVATION"
                }
            }
        } } catch (e: Exception) { return authorizationFailure(e) }
        if (!bindingMatches(existing.grant, source)) return result(OrdinaryRegionDisposition.EXECUTION_CONFLICT, "authorization binding mismatch")
        val prepared = when (val p = preparer.prepare(source, executionId, attemptId, runtimeCommit())) {
            is OrdinaryRegionPreparationOutcome.Blocked -> return result(p.disposition, p.detail)
            is OrdinaryRegionPreparationOutcome.Prepared -> p.value
        }
        try { guard.locked(authorizationId) { authorizations.reserve(authorizationId, executionId, now()) } }
        catch (e: Exception) { return authorizationFailure(e) }
        val recoveredBeforeStart = execution.prepareForGuardedAttempt(prepared.binding)
        val providerOutcome = if (recoveredBeforeStart != null) {
            if (recoveredBeforeStart is GovernedRegionExecutionOutcome.Blocked) return executionFailure(recoveredBeforeStart)
            recoveredBeforeStart
        } else {
            val startFailure = guard.locked(authorizationId) {
                val snapshot = authorizations.load(authorizationId)
                if (snapshot.revokedAt != null && !snapshot.revocationPostAttempt) "OWNER_AUTHORIZATION_REVOKED"
                else if (snapshot.executionId != executionId || !bindingMatches(snapshot.grant, source)) "EXECUTION_CONFLICT"
                else execution.durablyStartProviderAttempt(prepared.binding)?.let(::outcomeReason)
            }
            if (startFailure != null) return result(if (startFailure == "OWNER_AUTHORIZATION_REVOKED")
                OrdinaryRegionDisposition.OWNER_AUTHORIZATION_REVOKED else OrdinaryRegionDisposition.EXECUTION_CONFLICT, startFailure)
            execution.transportAfterGuardRelease(prepared.binding)
        }
        val recovered = when (providerOutcome) {
            is GovernedRegionExecutionOutcome.FirstAttemptCompleted -> providerOutcome.recovered
            is GovernedRegionExecutionOutcome.Recovered -> providerOutcome.providerState
            is GovernedRegionExecutionOutcome.Blocked -> return result(OrdinaryRegionDisposition.PROVIDER_OUTCOME_UNKNOWN, providerOutcome.reason)
        } ?: return result(OrdinaryRegionDisposition.PROVIDER_OUTCOME_UNKNOWN)
        val validated = when (providerOutcome) {
            is GovernedRegionExecutionOutcome.FirstAttemptCompleted -> providerOutcome.validated
            is GovernedRegionExecutionOutcome.Recovered -> providerOutcome.validated
            else -> null
        } ?: return result(if (recovered.downstreamProcessingPending) OrdinaryRegionDisposition.PROVIDER_RESPONSE_AVAILABLE else OrdinaryRegionDisposition.VALIDATION_FAILED)
        val reconstruction = reconstructOrdinaryRegion(validated, prepared.graphs).getOrElse {
            return result(OrdinaryRegionDisposition.REVIEW_REQUIRED, it.message ?: "source-order reconstruction failed") }
        val generationKey = ordinaryRegionGenerationKey(executionId, evidenceId.value, source.sha256,
            accepted.record.recordId, recovered.recordId, reconstruction.contentDigest)
        val payload = derivative(source, prepared, validated, reconstruction, recovered, accepted.record, authorizationId, generationKey)
        return when (val admitted = admission.admit(payload, ownerPrincipalId)) {
            is OrdinaryRegionAdmissionOutcome.Conflict -> result(OrdinaryRegionDisposition.ADMISSION_CONFLICT, admitted.reason)
            is OrdinaryRegionAdmissionOutcome.Admitted -> {
                val snapshot = ledger.open(prepared.binding.identity)
                if (snapshot.stages.last().ordinal < FidelityFirstAttemptStage.GENERATION_ADMITTED.ordinal)
                    runCatching { ledger.transition(prepared.binding.identity, FidelityFirstAttemptStage.GENERATION_ADMITTED) }
                val after = ledger.open(prepared.binding.identity)
                if (after.stages.last().ordinal < FidelityFirstAttemptStage.TERMINAL_SUCCESS.ordinal)
                    runCatching { ledger.transition(prepared.binding.identity, FidelityFirstAttemptStage.TERMINAL_SUCCESS,
                        listOf("generationId" to admitted.record.derivativeGenerationId.value)) }
                result(OrdinaryRegionDisposition.ADMITTED, if (admitted.recovered) "recovered existing admission" else "admitted",
                    admitted.record.derivativeGenerationId.value)
            }
        }
    }

    private fun derivative(source: AuthoritativeAcquisitionInput, prepared: OrdinaryRegionPreparedRequest,
        validated: RegionTranscriptionResult, reconstruction: OrdinaryRegionReconstruction,
        state: RecoveredRegionProviderState, accepted: OrdinaryRegionCapabilityAcceptanceRecord,
        authorizationId: String, key: String): OrdinaryRegionTranscriptionDerivative {
        val targets = prepared.binding.request.targets
        return OrdinaryRegionTranscriptionDerivative(evidenceArtifactId = source.evidenceArtifactId.value, sourceSha256 = source.sha256,
            pageBindings = prepared.pages.map { "${it.id.value}|${it.provenance.canonicalPixelDigest.value}|${it.provenance.encodedRepresentationSha256}" },
            regionBindings = targets.map { "${it.sourceRegionId.value}|${it.cropDigest.value}|${it.regionImage.encodedSha256}" },
            transcriptionBlocks = reconstruction.orderedBlocks.map { blockString(it) },
            providerReturnedOrder = validated.blocksInProviderOrder.map { it.sourceRegionId.value },
            parkerSourceOrder = reconstruction.orderedBlocks.map { it.sourceRegionId.value }, provider = "OpenAI", model = OPENAI_REGION_MODEL,
            adapterId = OPENAI_REGION_ADAPTER_ID, adapterVersion = OPENAI_REGION_ADAPTER_VERSION,
            providerProfile = OPENAI_REGION_PROFILE_ID, wireVersion = REGION_TRANSCRIPTION_WIRE_VERSION,
            schemaSha256 = REGION_TRANSCRIPTION_SCHEMA_SHA256, instructionSha256 = OPENAI_REGION_INSTRUCTION_SHA256,
            processingProfile = REGION_TRANSCRIPTION_PROCESSING_PROFILE, requestIdentity = prepared.binding.identity.requestId,
            requestDigest = state.requestDigest, responseIdentity = state.rawDigest, providerStateRecordIdentity = state.recordId,
            capabilityAcceptanceRecordIdentity = accepted.recordId, ownerAuthorizationIdentity = authorizationId,
            executionIdentity = prepared.binding.identity.executionId, attemptIdentity = prepared.binding.identity.attemptId,
            reconstructedContentDigest = reconstruction.contentDigest, canonicalGenerationKeyDigest = key,
            admissionProvenance = "${ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORE_ID}|${ORDINARY_REGION_AUTHORIZATION_STORE_ID}|${REGION_PROVIDER_STATE_STORE_ID}")
    }
    private fun blockString(b: RegionTranscriptionBlock) = listOf(b.sourceRegionId.value, b.pageNumber.toString(), b.literalText ?: "<null>",
        b.status.name, b.providerReturnedOrdinal.toString(), b.uncertainties.joinToString("|") { "${it.startCodePoint}:${it.endCodePointExclusive}:${it.exactSubstring}:${it.category}" },
        b.visualObservations.joinToString("|") { "${it.kind}:${it.startCodePoint}:${it.endCodePointExclusive}" }).joinToString("\u001f")
    private fun bindingMatches(g: OrdinaryRegionOwnerAuthorization, s: AuthoritativeAcquisitionInput) =
        g.evidenceArtifactId == s.evidenceArtifactId.value && g.sourceSha256 == s.sha256 &&
            g.capabilityDigest == capability.digest() && g.provider == "OpenAI"
    private fun authorizationFailure(e: Exception): OrdinaryRegionOwnerResult = when {
        e.message?.contains("EXPIRED") == true -> result(OrdinaryRegionDisposition.OWNER_AUTHORIZATION_EXPIRED_BEFORE_RESERVATION)
        e.message?.contains("REVOKED") == true -> result(OrdinaryRegionDisposition.OWNER_AUTHORIZATION_REVOKED)
        else -> result(OrdinaryRegionDisposition.OWNER_AUTHORIZATION_REQUIRED)
    }
    private fun executionFailure(o: GovernedRegionExecutionOutcome) = result(OrdinaryRegionDisposition.PROVIDER_OUTCOME_UNKNOWN, outcomeReason(o))
    private fun outcomeReason(o: GovernedRegionExecutionOutcome) = (o as? GovernedRegionExecutionOutcome.Blocked)?.reason ?: o.state.name
    private fun result(d: OrdinaryRegionDisposition, detail: String = d.name, id: String? = null) = OrdinaryRegionOwnerResult(d, detail, id)
}

data class OrdinaryRegionIngestionConfiguration(
    val enabled: Boolean = false,
    val capabilityAcceptanceRoot: Path? = null,
    val ownerAuthorizationRoot: Path? = null,
) {
    fun validate() {
        if (!enabled) return
        listOfNotNull(capabilityAcceptanceRoot, ownerAuthorizationRoot).also { require(it.size == 2) }
            .forEach { require(Files.isDirectory(it) && Files.isReadable(it) && Files.isWritable(it)) }
    }
}

private fun ordinaryRegionDigest(vararg fields: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    fields.forEach { field -> val bytes = field.toByteArray(StandardCharsets.UTF_8); md.update(ByteBuffer.allocate(4).putInt(bytes.size).array()); md.update(bytes) }
    return md.digest().joinToString("") { "%02x".format(it) }
}
private fun b64(value: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))
private fun unb64(value: String) = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
private fun createOnce(path: Path, text: String, conflict: String) {
    val bytes = text.toByteArray(StandardCharsets.UTF_8)
    try { FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { c -> val b = ByteBuffer.wrap(bytes); while (b.hasRemaining()) c.write(b); c.force(true) }; forceDirectory(path.parent) }
    catch (e: java.nio.file.FileAlreadyExistsException) { require(Files.readAllBytes(path).contentEquals(bytes)) { conflict } }
}
private fun forceDirectory(path: Path) { if (!System.getProperty("os.name").startsWith("Windows", true)) FileChannel.open(path, StandardOpenOption.READ).use { it.force(true) } }
