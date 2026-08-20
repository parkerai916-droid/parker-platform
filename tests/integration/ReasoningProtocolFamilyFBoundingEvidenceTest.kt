package parker.integration

/**
 * Reasoning Protocol Live-Model Conformance, Unit 3-BF: Family F Bounding
 * Evidence offline tooling.
 *
 * Governed by:
 * docs/decisions/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_IMPLEMENTATION_AUTHORIZATION_DECISION.md
 * docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md
 *
 * This file is one of exactly two files the accepted Decision authorizes
 * (the other is build.gradle.kts). It implements WP-A (the offline,
 * schedule-derived request-body estimator), honest WP-B/C/D evidence-gap
 * handling for the case -- the only case reachable under this offline
 * implementation task -- where no primary source has been supplied, WP-E
 * artifact/integrity/report generation, the append-only progress ledger,
 * copied-directory verification, mutually exclusive terminal markers, and
 * the double-gated evidence-producing entry point, plus this file's own
 * offline test suite.
 *
 * No file under src/ is modified. tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt
 * and tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt
 * remain byte-unchanged; tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt
 * carries only the narrow, separately authorized two-line FamilyFRole
 * correction (below) and is otherwise unchanged. Every model call this
 * estimator constructs is built by the unmodified production chain
 * (DefaultReasoningPromptBuilder, defaultOllamaRequestBody) and the
 * unmodified frozen schedule (FamilyFCampaignDefinition.allTrials), never
 * a second handwritten copy.
 *
 * Model roles: the FamilyFRole enum (ReasoningProtocolFamilyFDiagnosticTest.kt,
 * corrected under the accepted FamilyFRole Source Correction Amendment and
 * its accepted Independent Constitutional Review) now maps SUBJECT ->
 * llama3.2:3b / CONTROL -> qwen2.5-coder:7b, matching the governance table
 * Decision Section 10 and the Family F Model Role and Research Question
 * Scope Lock both freeze. WP-A's role/modelName fields remain a direct,
 * unmodified pass-through of that frozen schedule's own FamilyFRole
 * assignment, with no independent interpretation or second mapping of its
 * own -- consistent with Decision Section 10 and Plan Section 12's
 * prohibition on a substituted model name or a second handwritten schedule
 * copy.
 *
 * No model is acquired, loaded, started, stopped, or contacted by any
 * test in this file. No provider, network, or Docker call is made. The
 * one real evidence-producing entry point is reachable only through the
 * double gate below, and remains inert under every ordinary Gradle task
 * (test, check, build, assemble).
 */

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertFailsWith
import parker.core.interfaces.ReasoningSubject
import parker.core.runtime.DefaultReasoningPromptBuilder
import parker.core.runtime.defaultOllamaRequestBody
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import java.util.Comparator

// ---------------------------------------------------------------------
// Frozen constants (Decision Sections 6, 8; Plan Section 6)
// ---------------------------------------------------------------------

const val FAMILY_F_BOUNDING_EVIDENCE_PROPERTY = "parker.reasoning.familyf.boundingEvidence.enabled"
const val FAMILY_F_BOUNDING_EVIDENCE_APPROVED_ENV = "PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_APPROVED"
const val FAMILY_F_BOUNDING_EVIDENCE_ROOT_ENV = "PARKER_REASONING_FAMILY_F_BOUNDING_EVIDENCE_ROOT"
const val FAMILY_F_BOUNDING_EVIDENCE_SCHEMA_VERSION = 1

private fun boundingEvidenceSha256Bytes(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

private fun checkedAdd(a: Long, b: Long): Long = Math.addExact(a, b)

private fun boundingEvidenceJsonEscape(value: String): String = buildString {
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character.code < 0x20) append("\\u%04x".format(character.code)) else append(character)
        }
    }
}

private fun List<String>.toJsonArray(): String =
    joinToString(prefix = "[", postfix = "]") { "\"${boundingEvidenceJsonEscape(it)}\"" }

// ---------------------------------------------------------------------
// WP-A: offline, schedule-derived request-body estimator (Plan Sections
// 12-14). Derives every record only from FamilyFCampaignDefinition.allTrials
// and the unmodified production formatter/serializer; maintains no second
// fixture list or schedule copy.
// ---------------------------------------------------------------------

data class FamilyFBoundingRequestRecord(
    val schemaVersion: Int,
    val sequence: Int,
    val trialId: String,
    val blockId: String,
    val blockOrder: Int,
    val warmup: Boolean,
    val role: String,
    val modelName: String,
    val fixtureId: String,
    val profileId: String,
    val repetition: Int,
    val promptUtf8ByteCount: Long,
    val promptSha256: String,
    val requestBodyUtf8ByteCount: Long,
    val requestBodySha256: String,
    val requestBodyBase64: String,
)

fun FamilyFBoundingRequestRecord.toJsonLine(): String = buildString {
    append('{')
    append("\"schemaVersion\":").append(schemaVersion).append(',')
    append("\"sequence\":").append(sequence).append(',')
    append("\"trialId\":\"").append(boundingEvidenceJsonEscape(trialId)).append("\",")
    append("\"blockId\":\"").append(boundingEvidenceJsonEscape(blockId)).append("\",")
    append("\"blockOrder\":").append(blockOrder).append(',')
    append("\"warmup\":").append(warmup).append(',')
    append("\"role\":\"").append(boundingEvidenceJsonEscape(role)).append("\",")
    append("\"modelName\":\"").append(boundingEvidenceJsonEscape(modelName)).append("\",")
    append("\"fixtureId\":\"").append(boundingEvidenceJsonEscape(fixtureId)).append("\",")
    append("\"profileId\":\"").append(boundingEvidenceJsonEscape(profileId)).append("\",")
    append("\"repetition\":").append(repetition).append(',')
    append("\"promptUtf8ByteCount\":").append(promptUtf8ByteCount).append(',')
    append("\"promptSha256\":\"").append(promptSha256).append("\",")
    append("\"requestBodyUtf8ByteCount\":").append(requestBodyUtf8ByteCount).append(',')
    append("\"requestBodySha256\":\"").append(requestBodySha256).append("\",")
    append("\"requestBodyBase64\":\"").append(requestBodyBase64).append('"')
    append('}')
}

object FamilyFBoundingEvidenceRequestEstimator {
    /**
     * WP-A. Iterates the accepted, byte-unchanged FamilyFCampaignDefinition.allTrials
     * schedule exactly once, in order, deriving each request body from the
     * unmodified production chain (SyntheticContextProfiles.construct,
     * DefaultReasoningPromptBuilder, defaultOllamaRequestBody). Produces
     * exactly 392 records. Applies no independent model-role interpretation:
     * each record's role/modelName is a direct pass-through of the frozen
     * trial's own FamilyFRole.
     */
    fun estimate(): List<FamilyFBoundingRequestRecord> {
        val trials = FamilyFCampaignDefinition.allTrials
        val blockOrderCounters = mutableMapOf<Pair<Int, FamilyFRole>, Int>()
        val records = trials.mapIndexed { index, trial ->
            val key = trial.repetition to trial.role
            val order = (blockOrderCounters[key] ?: 0) + 1
            blockOrderCounters[key] = order

            val profileId = trial.profileId ?: ContextProfileId.MINIMAL_PRODUCTION_CONTEXT
            val input = SyntheticContextProfiles.construct(trial.fixture, profileId)
            val turn = (input.request.subject as ReasoningSubject.OfTurn).turn
            val prompt = DefaultReasoningPromptBuilder().buildPrompt(turn, input.request.reasoningContext)
            val modelName = trial.role.modelName
            val requestBody = defaultOllamaRequestBody(prompt, modelName)

            val promptBytes = prompt.toByteArray(StandardCharsets.UTF_8)
            val requestBytes = requestBody.toByteArray(StandardCharsets.UTF_8)

            FamilyFBoundingRequestRecord(
                schemaVersion = FAMILY_F_BOUNDING_EVIDENCE_SCHEMA_VERSION,
                sequence = index,
                trialId = trial.id,
                blockId = "r${trial.repetition.toString().padStart(2, '0')}-${trial.role.name.lowercase()}",
                blockOrder = order,
                warmup = trial.kind == FamilyFTrialKind.WARMUP,
                role = trial.role.name,
                modelName = modelName,
                fixtureId = trial.fixture.id,
                profileId = profileId.externalId,
                repetition = trial.repetition,
                promptUtf8ByteCount = promptBytes.size.toLong(),
                promptSha256 = boundingEvidenceSha256Bytes(promptBytes),
                requestBodyUtf8ByteCount = requestBytes.size.toLong(),
                requestBodySha256 = boundingEvidenceSha256Bytes(requestBytes),
                requestBodyBase64 = Base64.getEncoder().encodeToString(requestBytes),
            )
        }
        check(records.size == 392) { "WP-A must derive exactly 392 records from the frozen schedule; got ${records.size}" }
        return records
    }
}

data class FamilyFBoundingRequestSummary(
    val expectedRecordCount: Int,
    val observedRecordCount: Int,
    val scoredCount: Int,
    val warmupCount: Int,
    val globalMaxByteCount: Long,
    val globalMaxTrialIds: List<String>,
    val uniqueBodyCount: Int,
    val sortedByteLengthDistribution: List<Long>,
    val status: String,
)

fun FamilyFBoundingRequestSummary.toJson(): String = buildString {
    append('{')
    append("\"expectedRecordCount\":").append(expectedRecordCount).append(',')
    append("\"observedRecordCount\":").append(observedRecordCount).append(',')
    append("\"scoredCount\":").append(scoredCount).append(',')
    append("\"warmupCount\":").append(warmupCount).append(',')
    append("\"globalMaxByteCount\":").append(globalMaxByteCount).append(',')
    append("\"globalMaxTrialIds\":").append(globalMaxTrialIds.toJsonArray()).append(',')
    append("\"uniqueBodyCount\":").append(uniqueBodyCount).append(',')
    append("\"sortedByteLengthDistribution\":[").append(sortedByteLengthDistribution.joinToString(",")).append("],")
    append("\"PROPOSED_MAX_REQUEST_BOUND\":").append(globalMaxByteCount).append(',')
    append("\"PROPOSED_MAX_REQUEST_BOUND_NOTE\":\"evidence result only; not an accepted bound\",")
    append("\"status\":\"").append(boundingEvidenceJsonEscape(status)).append('"')
    append('}')
}

object FamilyFBoundingEvidenceRequestSummarizer {
    /**
     * Plan Section 13. Never selects or approves a bound -- reports only
     * the observed maximum and every tied trial ID as an evidence result.
     */
    fun summarize(records: List<FamilyFBoundingRequestRecord>): FamilyFBoundingRequestSummary {
        require(records.isNotEmpty()) { "cannot summarize an empty record set" }
        val scored = records.count { !it.warmup }
        val warmup = records.count { it.warmup }
        val maxLen = records.maxOf { it.requestBodyUtf8ByteCount }
        val tied = records.filter { it.requestBodyUtf8ByteCount == maxLen }.map { it.trialId }.sorted()
        val uniqueBodies = records.map { it.requestBodySha256 }.toSet().size
        val distribution = records.map { it.requestBodyUtf8ByteCount }.sorted()
        val status = if (records.size == 392) "RESOLVED_WITH_PROPOSED_VALUE" else "SCHEDULE_MISMATCH"
        return FamilyFBoundingRequestSummary(
            expectedRecordCount = 392,
            observedRecordCount = records.size,
            scoredCount = scored,
            warmupCount = warmup,
            globalMaxByteCount = maxLen,
            globalMaxTrialIds = tied,
            uniqueBodyCount = uniqueBodies,
            sortedByteLengthDistribution = distribution,
            status = status,
        )
    }
}

// ---------------------------------------------------------------------
// WP-B/C/D: honest evidence-gap handling (Plan Sections 15-19). No
// collection mechanism is authorized under this offline implementation
// task, so every work package below is invoked with an empty supplied-
// source list and honestly returns an unresolved status plus a complete
// evidence-gap record -- never a fabricated or inferred value.
// ---------------------------------------------------------------------

data class FamilyFEvidenceGapEntry(
    val workPackage: String,
    val question: String,
    val admissibleSourceClasses: List<String>,
    val sourcesSearched: List<String>,
    val reason: String,
    val remainingMissingFact: String,
    val futureGovernanceNeeded: String,
)

fun FamilyFEvidenceGapEntry.toJson(): String = buildString {
    append('{')
    append("\"workPackage\":\"").append(boundingEvidenceJsonEscape(workPackage)).append("\",")
    append("\"question\":\"").append(boundingEvidenceJsonEscape(question)).append("\",")
    append("\"admissibleSourceClasses\":").append(admissibleSourceClasses.toJsonArray()).append(',')
    append("\"sourcesSearched\":").append(sourcesSearched.toJsonArray()).append(',')
    append("\"reason\":\"").append(boundingEvidenceJsonEscape(reason)).append("\",")
    append("\"remainingMissingFact\":\"").append(boundingEvidenceJsonEscape(remainingMissingFact)).append("\",")
    append("\"futureGovernanceNeeded\":\"").append(boundingEvidenceJsonEscape(futureGovernanceNeeded)).append('"')
    append('}')
}

fun List<FamilyFEvidenceGapEntry>.toJson(): String = joinToString(prefix = "[", postfix = "]") { it.toJson() }

object FamilyFBoundingEvidenceGapRegister {
    fun forNoSuppliedSources(
        workPackage: String,
        question: String,
        admissibleSourceClasses: List<String>,
    ): FamilyFEvidenceGapEntry = FamilyFEvidenceGapEntry(
        workPackage = workPackage,
        question = question,
        admissibleSourceClasses = admissibleSourceClasses,
        sourcesSearched = emptyList(),
        reason = "No primary source was supplied under this offline-only implementation and verification task; no collection mechanism is authorized here (Plan Section 20).",
        remainingMissingFact = question,
        futureGovernanceNeeded = "A separately authorized Family F Bounding Evidence Production Authorization Decision must enumerate an admissible collection mechanism before any source may be supplied.",
    )
}

// Plan Section 15's full required source-record schema (WP-B, and reused
// by WP-D since Plan Section 18 requires the same primary-provenance
// discipline). Admissible source categories are the exact four Plan
// Section 15 names; every other value is a real, testable rejection
// condition, not a documentation aspiration.
val FAMILY_F_WP_B_ADMISSIBLE_SOURCE_CATEGORIES = setOf(
    "official provider documentation",
    "official provider source or release artifact",
    "official tokenizer or model documentation",
    "accepted programme-enforced generation-limit specification",
)

data class FamilyFPrimaryEvidenceSource(
    val sourceId: String,
    val sourceCategory: String,
    val publisher: String,
    val title: String,
    val canonicalLocator: String,
    val retrievedAt: String,
    val applicableProviderVersionOrDigest: String,
    val applicableModelDigest: String?,
    val contentSha256: String,
    val localCapturePath: String,
    val relevantClaim: String,
    val exactLocationWithinSource: String,
)

enum class FamilyFEvidenceAdmissibility { ADMISSIBLE, REJECTED }

data class FamilyFEvidenceAdmissibilityResult(
    val sourceId: String,
    val admissibilityStatus: FamilyFEvidenceAdmissibility,
    val rejectionReason: String?,
)

enum class FamilyFResponseEvidenceStatus {
    RESOLVED_WITH_PROPOSED_VALUE,
    UNRESOLVED_NO_APPLICABLE_PRIMARY_SOURCE,
    UNRESOLVED_INCOMPLETE_SERIALIZATION_BOUND,
    NOT_ADMISSIBLE,
}

enum class FamilyFRuntimeEvidenceStatus {
    RESOLVED_BY_COMPLETE_PROVIDER_DOCUMENTATION,
    UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE,
    NOT_ADMISSIBLE,
}

object FamilyFBoundingResponseEvidenceInventory {
    // WP-B. Real, deterministic, offline admissibility checking (Plan
    // Section 15): a source is admissible only if its sourceCategory is
    // one of the four Plan-named admissible classes AND every required
    // provenance field (Plan Section 15's own record schema) is non-blank.
    // "Third-party summaries, forum posts, search-result snippets,
    // observed response sizes, averages, and uncited recollections are
    // inadmissible" (Plan Section 15) -- any sourceCategory outside the
    // four named classes is rejected exactly on that basis. This
    // validator never retrieves, queries, or contacts a provider/network
    // endpoint of its own, and never resolves a numeric bound: Plan
    // Section 16's full worksheet (HARD_GENERATION_TOKEN_LIMIT, envelope
    // accounting, etc.) is a separate, later-governed derivation this
    // validator does not attempt.
    fun assessAdmissibility(source: FamilyFPrimaryEvidenceSource): FamilyFEvidenceAdmissibilityResult {
        if (source.sourceCategory !in FAMILY_F_WP_B_ADMISSIBLE_SOURCE_CATEGORIES) {
            return FamilyFEvidenceAdmissibilityResult(
                source.sourceId, FamilyFEvidenceAdmissibility.REJECTED,
                "sourceCategory '${source.sourceCategory}' is not one of the four Plan Section 15 admissible categories",
            )
        }
        val missing = buildList {
            if (source.sourceId.isBlank()) add("sourceId")
            if (source.canonicalLocator.isBlank()) add("canonicalLocator")
            if (source.contentSha256.isBlank()) add("contentSha256")
            if (source.applicableProviderVersionOrDigest.isBlank()) add("applicableProviderVersionOrDigest")
            if (source.relevantClaim.isBlank()) add("relevantClaim")
            if (source.exactLocationWithinSource.isBlank()) add("exactLocationWithinSource")
        }
        if (missing.isNotEmpty()) {
            return FamilyFEvidenceAdmissibilityResult(
                source.sourceId, FamilyFEvidenceAdmissibility.REJECTED,
                "missing required Plan Section 15 provenance field(s): ${missing.joinToString(", ")}",
            )
        }
        return FamilyFEvidenceAdmissibilityResult(source.sourceId, FamilyFEvidenceAdmissibility.ADMISSIBLE, null)
    }

    fun evaluate(preSuppliedSources: List<FamilyFPrimaryEvidenceSource>): Pair<FamilyFResponseEvidenceStatus, FamilyFEvidenceGapEntry?> {
        if (preSuppliedSources.isEmpty()) {
            return FamilyFResponseEvidenceStatus.UNRESOLVED_NO_APPLICABLE_PRIMARY_SOURCE to
                FamilyFBoundingEvidenceGapRegister.forNoSuppliedSources(
                    workPackage = "WP-B",
                    question = "What is Parker's actual MAX_RESPONSE_BOUND for the frozen subject/control models?",
                    admissibleSourceClasses = FAMILY_F_WP_B_ADMISSIBLE_SOURCE_CATEGORIES.toList(),
                )
        }
        val admissible = preSuppliedSources.map(::assessAdmissibility).filter { it.admissibilityStatus == FamilyFEvidenceAdmissibility.ADMISSIBLE }
        if (admissible.isEmpty()) {
            return FamilyFResponseEvidenceStatus.NOT_ADMISSIBLE to null
        }
        // Admissible sources exist, but Plan Section 16's full worksheet
        // (a separate, later-governed derivation) is required before any
        // value could be proposed; this validator establishes
        // admissibility only, exactly as Plan Section 16 itself states:
        // "This Plan predicts no status and selects no value."
        return FamilyFResponseEvidenceStatus.UNRESOLVED_INCOMPLETE_SERIALIZATION_BOUND to null
    }
}

// Plan Section 17: MAX_HEADER_COUNT is an application-level iteration
// limit; MAX_AGGREGATE_HEADER_BYTES is a durable encoded-size limit.
// Treating evidence of one kind as resolving the other -- or treating a
// parser wire limit or bare provider-produced behaviour as resolving
// either -- is exactly the conflation Plan Section 17 forbids ("must not
// conflate: parser wire limits; application-level iteration limits;
// durable encoded-size limits; or provider-produced header behavior").
enum class FamilyFHeaderLimitKind { PARSER_WIRE_LIMIT, APPLICATION_ITERATION_LIMIT, DURABLE_ENCODED_SIZE_LIMIT, PROVIDER_PRODUCED_BEHAVIOR }
enum class FamilyFHeaderBoundTarget { MAX_HEADER_COUNT, MAX_AGGREGATE_HEADER_BYTES }

data class FamilyFHeaderEvidenceSource(
    val sourceId: String,
    val boundTarget: FamilyFHeaderBoundTarget,
    val limitKind: FamilyFHeaderLimitKind,
    val canonicalLocator: String,
    val contentSha256: String,
)

object FamilyFBoundingHeaderEvidenceInventory {
    private val requiredKindForTarget = mapOf(
        FamilyFHeaderBoundTarget.MAX_HEADER_COUNT to FamilyFHeaderLimitKind.APPLICATION_ITERATION_LIMIT,
        FamilyFHeaderBoundTarget.MAX_AGGREGATE_HEADER_BYTES to FamilyFHeaderLimitKind.DURABLE_ENCODED_SIZE_LIMIT,
    )

    // WP-C. Real conflation check: a source is admissible for its declared
    // boundTarget only if its limitKind is the kind that target actually
    // requires; any mismatch (including a parser wire limit or bare
    // provider-produced-behaviour claim asked to resolve either bound) is
    // rejected. Never contacts a provider/network endpoint; never resolves
    // a numeric bound of its own.
    fun isAdmissible(source: FamilyFHeaderEvidenceSource): Boolean =
        source.limitKind == requiredKindForTarget.getValue(source.boundTarget)

    fun evaluate(preSuppliedSources: List<FamilyFHeaderEvidenceSource>): Pair<String, FamilyFEvidenceGapEntry?> {
        if (preSuppliedSources.isEmpty()) {
            return "UNRESOLVED" to FamilyFBoundingEvidenceGapRegister.forNoSuppliedSources(
                workPackage = "WP-C",
                question = "What are Parker's actual MAX_HEADER_COUNT and MAX_AGGREGATE_HEADER_BYTES?",
                admissibleSourceClasses = listOf("JDK/HTTP implementation documentation for the exact estimator/diagnostic commit"),
            )
        }
        val conflated = preSuppliedSources.filterNot(::isAdmissible)
        if (conflated.size == preSuppliedSources.size) {
            return "NOT_ADMISSIBLE" to null
        }
        return "UNRESOLVED" to null
    }
}

object FamilyFBoundingRuntimeEvidenceInventory {
    // WP-D. Documentation-only (Plan Section 18): admissible only if the
    // source is official provider documentation AND was not derived from
    // an actual launch, benchmark, model request, unload, crash, restart,
    // or filesystem-mutation observation -- Plan Section 18's own explicit
    // prohibition list. `basedOnObservation=true` is exactly that
    // prohibited condition, made real and testable rather than assumed.
    fun isAdmissible(source: FamilyFPrimaryEvidenceSource, basedOnObservation: Boolean): Boolean =
        source.sourceCategory == "official provider documentation" && !basedOnObservation

    fun evaluate(
        preSuppliedSources: List<FamilyFPrimaryEvidenceSource>,
        basedOnObservation: Map<String, Boolean> = emptyMap(),
    ): Pair<FamilyFRuntimeEvidenceStatus, FamilyFEvidenceGapEntry?> {
        if (preSuppliedSources.isEmpty()) {
            return FamilyFRuntimeEvidenceStatus.UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE to
                FamilyFBoundingEvidenceGapRegister.forNoSuppliedSources(
                    workPackage = "WP-D",
                    question = "What is Parker's actual writable dedicated-runtime growth (R) for the frozen subject/control models?",
                    admissibleSourceClasses = listOf("official provider documentation describing writable runtime behaviour for the exact provider version"),
                )
        }
        val admissible = preSuppliedSources.filter { isAdmissible(it, basedOnObservation[it.sourceId] ?: false) }
        if (admissible.isEmpty()) {
            return FamilyFRuntimeEvidenceStatus.NOT_ADMISSIBLE to null
        }
        return FamilyFRuntimeEvidenceStatus.UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE to null
    }
}

// ---------------------------------------------------------------------
// Repository-input preservation (Plan Section 11). Read-only hashing of
// the protected files this Plan/Decision cite; no network, no mutation.
// ---------------------------------------------------------------------

object FamilyFBoundingEvidenceRepositoryInputs {
    val protectedRelativePaths: List<String> = listOf(
        "build.gradle.kts",
        "tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt",
        "tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt",
        "tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt",
        "tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt",
        "src/runtime/ReasoningPromptBuilder.kt",
        "src/runtime/ModelInferenceClient.kt",
        "docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_CAPTURE_PROXY_RESPONSE_REQUEST_SIZE_AND_DEDICATED_RUNTIME_GROWTH_BOUNDING_SCOPE_LOCK.md",
        "docs/implementation/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_3BF_FAMILY_F_BOUNDING_EVIDENCE_ACQUISITION_AND_OFFLINE_ESTIMATOR_PLAN.md",
    )

    fun currentHashes(repositoryRoot: Path): Map<String, String> =
        protectedRelativePaths.associateWith { relative ->
            val file = repositoryRoot.resolve(relative)
            if (Files.exists(file)) boundingEvidenceSha256Bytes(Files.readAllBytes(file)) else "MISSING"
        }
}

class FamilyFBoundingEvidenceProtectedInputDriftException(message: String) : RuntimeException(message)

fun checkProtectedInputDrift(actual: Map<String, String>, expected: Map<String, String>) {
    expected.forEach { (path, expectedHash) ->
        val actualHash = actual[path]
            ?: throw FamilyFBoundingEvidenceProtectedInputDriftException("protected input missing: $path")
        if (actualHash != expectedHash) {
            throw FamilyFBoundingEvidenceProtectedInputDriftException("protected input drift detected: $path")
        }
    }
}

// ---------------------------------------------------------------------
// Progress ledger (Plan Section 22): deterministic step IDs, append-only
// until finalized, WP_E_VALIDATION is always the final recorded step.
// ---------------------------------------------------------------------

enum class FamilyFBoundingEvidenceStep {
    PREFLIGHT, WP_A_ESTIMATOR, WP_B_RESPONSE_EVIDENCE, WP_C_HEADER_EVIDENCE, WP_D_RUNTIME_EVIDENCE, WP_E_VALIDATION,
}

class FamilyFBoundingEvidenceLedgerFinalizedException(message: String) : RuntimeException(message)

class FamilyFBoundingEvidenceLedger(private val ledgerPath: Path) {
    private var finalized = false

    fun recordStep(step: FamilyFBoundingEvidenceStep, status: String, detail: String) {
        if (finalized) {
            throw FamilyFBoundingEvidenceLedgerFinalizedException("cannot append after ledger finalization: $step")
        }
        val line = "{\"step\":\"${step.name}\",\"status\":\"${boundingEvidenceJsonEscape(status)}\"," +
            "\"detail\":\"${boundingEvidenceJsonEscape(detail)}\",\"recordedAt\":\"${Instant.now()}\"}"
        Files.writeString(
            ledgerPath,
            line + "\n",
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }

    fun finalizeLedger() {
        finalized = true
    }
}

object FamilyFBoundingEvidenceLedgerReader {
    private val recordPattern = Regex(
        """^\{"step":"([^"\\]*)","status":"((?:\\(?:["\\/bfnrt]|u[0-9A-Fa-f]{4})|[^"\\\u0000-\u001F])*)","detail":"((?:\\(?:["\\/bfnrt]|u[0-9A-Fa-f]{4})|[^"\\\u0000-\u001F])*)","recordedAt":"([^"\\\u0000-\u001F]+)"\}$""",
    )

    private val requiredStatus = mapOf(
        FamilyFBoundingEvidenceStep.PREFLIGHT to "OK",
        FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR to "OK",
        FamilyFBoundingEvidenceStep.WP_B_RESPONSE_EVIDENCE to "UNRESOLVED_NO_APPLICABLE_PRIMARY_SOURCE",
        FamilyFBoundingEvidenceStep.WP_C_HEADER_EVIDENCE to "UNRESOLVED",
        FamilyFBoundingEvidenceStep.WP_D_RUNTIME_EVIDENCE to "UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE",
        FamilyFBoundingEvidenceStep.WP_E_VALIDATION to "OK",
    )

    // Read-only. Used only to decide, on resume, which ledger-governed
    // steps are already durably recorded (Plan Section 22) -- never
    // mutates the ledger.
    fun readCompletedSteps(ledgerPath: Path): Set<FamilyFBoundingEvidenceStep> {
        if (!Files.exists(ledgerPath)) return emptySet()
        val rawLedger = Files.readString(ledgerPath, StandardCharsets.UTF_8)
        if (rawLedger.isEmpty() || !rawLedger.endsWith("\n")) {
            throw FamilyFBoundingEvidenceLedgerMalformedException("malformed ledger: empty or unterminated JSONL content")
        }
        val lines = rawLedger.dropLast(1).split('\n')
        val completed = linkedSetOf<FamilyFBoundingEvidenceStep>()
        lines.forEachIndexed { index, line ->
            if (line.isBlank()) {
                throw FamilyFBoundingEvidenceLedgerMalformedException("malformed ledger record at line ${index + 1}: blank record")
            }
            val match = recordPattern.matchEntire(line)
                ?: throw FamilyFBoundingEvidenceLedgerMalformedException("malformed ledger record at line ${index + 1}: invalid or unterminated JSONL record")
            val step = try {
                FamilyFBoundingEvidenceStep.valueOf(match.groupValues[1])
            } catch (_: IllegalArgumentException) {
                throw FamilyFBoundingEvidenceLedgerMalformedException("malformed ledger record at line ${index + 1}: unknown step '${match.groupValues[1]}'")
            }
            if (match.groupValues[2].isBlank() || match.groupValues[3].isBlank()) {
                throw FamilyFBoundingEvidenceLedgerMalformedException("malformed ledger record at line ${index + 1}: status and detail are required")
            }
            val governedStatus = requiredStatus.getValue(step)
            if (match.groupValues[2] != governedStatus) {
                throw FamilyFBoundingEvidenceLedgerMalformedException(
                    "semantically invalid ledger metadata at line ${index + 1} for ${step.name}: " +
                        "expected governed status '$governedStatus'",
                )
            }
            try {
                Instant.parse(match.groupValues[4])
            } catch (_: RuntimeException) {
                throw FamilyFBoundingEvidenceLedgerMalformedException("malformed ledger record at line ${index + 1}: invalid recordedAt")
            }
            if (!completed.add(step)) {
                throw FamilyFBoundingEvidenceLedgerMalformedException("duplicate ledger completion at line ${index + 1}: ${step.name}")
            }
            val expectedStep = FamilyFBoundingEvidenceStep.entries[index]
            if (step != expectedStep) {
                throw FamilyFBoundingEvidenceLedgerMalformedException(
                    "structurally impossible ledger order at line ${index + 1}: expected ${expectedStep.name}, found ${step.name}",
                )
            }
        }
        return completed
    }
}

class FamilyFBoundingEvidenceLedgerMalformedException(message: String) : RuntimeException(message)

// ---------------------------------------------------------------------
// Manifest, copied-directory verification, and mutually exclusive
// terminal markers (Plan Sections 8, 21).
// ---------------------------------------------------------------------

object FamilyFBoundingEvidenceIntegrity {
    val manifestCoveredFiles: Set<String> = setOf(
        "evidence-identity.json",
        "repository-inputs.json",
        "progress-ledger.jsonl",
        "request-estimator-records.jsonl",
        "request-estimator-summary.json",
        "response-primary-evidence-index.json",
        "header-primary-evidence-index.json",
        "runtime-primary-evidence-index.json",
        "evidence-gap-register.json",
        "bounding-evidence-report.md",
    )
    val governedFiles: Set<String> = manifestCoveredFiles + setOf("SHA256SUMS.txt", "evidence.complete", "evidence.failed")
    val governedDirectories: Set<String> = setOf(
        "response-primary-evidence",
        "header-primary-evidence",
        "runtime-primary-evidence",
    )

    private fun hasSymbolicLink(root: Path, target: Path): Boolean {
        if (Files.isSymbolicLink(root)) return true
        var current = root
        for (name in root.relativize(target)) {
            current = current.resolve(name)
            if (Files.isSymbolicLink(current)) return true
        }
        return false
    }

    private fun isRealPathConfined(root: Path, target: Path): Boolean {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS) ||
            !Files.exists(target, LinkOption.NOFOLLOW_LINKS) ||
            hasSymbolicLink(root, target)
        ) return false
        return try {
            val rootReal = root.toRealPath()
            val targetReal = target.toRealPath()
            targetReal != rootReal && targetReal.startsWith(rootReal)
        } catch (_: RuntimeException) {
            false
        }
    }

    fun validateCampaignSurface(root: Path): String? {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) return null
        if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            return "campaign root must be a real directory, not a symbolic link"
        }
        Files.walk(root).use { stream ->
            val candidates = stream.iterator()
            while (candidates.hasNext()) {
                val candidate = candidates.next()
                if (candidate == root) continue
                val relative = root.relativize(candidate).toString().replace('\\', '/')
                if (Files.isSymbolicLink(candidate)) return "symbolic links are forbidden in campaign surface: $relative"
                if (!isRealPathConfined(root, candidate)) return "campaign path escapes real root: $relative"
                val allowed = if (Files.isDirectory(candidate, LinkOption.NOFOLLOW_LINKS)) {
                    relative in governedDirectories
                } else {
                    Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS) && relative in governedFiles
                }
                if (!allowed) return "unknown campaign path: $relative"
            }
        }
        return null
    }

    fun writeManifest(root: Path, mandatoryFiles: List<Path>): Path {
        check(validateCampaignSurface(root) == null) { "campaign surface is not safe for manifest hashing" }
        mandatoryFiles.forEach { file ->
            check(Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && isRealPathConfined(root, file)) {
                "manifest member is not a confined regular file: $file"
            }
        }
        val manifestPath = root.resolve("SHA256SUMS.txt")
        val lines = mandatoryFiles.sortedBy { it.toString() }.map { file ->
            val hash = boundingEvidenceSha256Bytes(Files.readAllBytes(file))
            "$hash  ${root.relativize(file)}"
        }
        Files.writeString(manifestPath, lines.joinToString("\n", postfix = "\n"), StandardCharsets.UTF_8)
        return manifestPath
    }

    fun verifyManifest(root: Path, manifestPath: Path): Boolean {
        val normalizedRoot = root.toAbsolutePath().normalize()
        val expectedManifest = normalizedRoot.resolve("SHA256SUMS.txt")
        val suppliedManifest = manifestPath.toAbsolutePath()
        // Exact lexical identity is established before caller-supplied content
        // can be opened. Traversal-equivalent spellings intentionally reject.
        if (suppliedManifest != expectedManifest) return false
        if (validateCampaignSurface(normalizedRoot) != null ||
            Files.isSymbolicLink(suppliedManifest) ||
            !Files.isRegularFile(suppliedManifest, LinkOption.NOFOLLOW_LINKS) ||
            !isRealPathConfined(normalizedRoot, suppliedManifest)
        ) return false
        if (Files.isSymbolicLink(normalizedRoot)) return false
        val realRoot = try {
            normalizedRoot.toRealPath()
        } catch (_: RuntimeException) {
            return false
        }
        val manifestReal = try {
            suppliedManifest.toRealPath()
        } catch (_: RuntimeException) {
            return false
        }
        if (!manifestReal.startsWith(realRoot) || manifestReal == realRoot) return false
        val lines = Files.readAllLines(suppliedManifest, StandardCharsets.UTF_8)
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) return false
        val seen = mutableSetOf<String>()
        val valid = lines.all { line ->
            val parts = line.split("  ", limit = 2)
            if (parts.size != 2) return@all false
            val (expectedHash, relative) = parts
            if (!expectedHash.matches(Regex("[0-9a-f]{64}"))) return@all false
            if (relative.isBlank() || '\\' in relative || Regex("^[A-Za-z]:[/\\\\]").containsMatchIn(relative)) return@all false
            val relativePath = try {
                Path.of(relative)
            } catch (_: RuntimeException) {
                return@all false
            }
            if (relativePath.isAbsolute || relativePath.normalize().startsWith("..")) return@all false
            val target = normalizedRoot.resolve(relativePath).normalize()
            if (!target.startsWith(normalizedRoot) || target == normalizedRoot) return@all false
            val normalizedRelative = normalizedRoot.relativize(target).toString().replace('\\', '/')
            if (normalizedRelative !in manifestCoveredFiles || !seen.add(normalizedRelative)) return@all false
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS) ||
                hasSymbolicLink(normalizedRoot, target)
            ) return@all false
            val targetReal = try {
                target.toRealPath()
            } catch (_: RuntimeException) {
                return@all false
            }
            targetReal.startsWith(realRoot) && targetReal != realRoot &&
                boundingEvidenceSha256Bytes(Files.readAllBytes(target)) == expectedHash
        }
        return valid && seen == manifestCoveredFiles
    }

    private fun deleteTree(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    /** Read-only, safely repeatable: copies to a fresh sibling directory, re-verifies, and always removes the copy. */
    fun verifyFromFreshCopy(root: Path, tempParent: Path): Boolean {
        if (validateCampaignSurface(root) != null) return false
        val copyRoot = Files.createTempDirectory(tempParent, "bounding-evidence-copy-")
        var primaryFailure: Throwable? = null
        try {
            Files.walk(root).use { stream ->
                stream.sorted().forEach { source ->
                    val relative = root.relativize(source)
                    if (relative.toString().isEmpty()) return@forEach
                    val target = copyRoot.resolve(relative.toString())
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target)
                    } else {
                        Files.createDirectories(target.parent)
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
            return verifyManifest(copyRoot, copyRoot.resolve("SHA256SUMS.txt"))
        } catch (failure: Throwable) {
            primaryFailure = failure
            throw failure
        } finally {
            try {
                deleteTree(copyRoot)
            } catch (cleanupFailure: Throwable) {
                if (primaryFailure != null) primaryFailure.addSuppressed(cleanupFailure) else throw cleanupFailure
            }
        }
    }
}

object FamilyFBoundingEvidenceTerminal {
    fun writeComplete(root: Path) {
        check(!Files.exists(root.resolve("evidence.failed"))) {
            "cannot write evidence.complete: evidence.failed already present"
        }
        Files.writeString(root.resolve("evidence.complete"), "COMPLETE ${Instant.now()}\n", StandardCharsets.UTF_8)
    }

    fun writeFailed(root: Path, reason: String) {
        check(!Files.exists(root.resolve("evidence.complete"))) {
            "cannot write evidence.failed: evidence.complete already present"
        }
        Files.writeString(root.resolve("evidence.failed"), "FAILED ${Instant.now()} ${boundingEvidenceJsonEscape(reason)}\n", StandardCharsets.UTF_8)
    }
}

// ---------------------------------------------------------------------
// WP-A through WP-E orchestration, entirely offline and entirely
// in-process, against a caller-supplied output root. Never contacts a
// provider, model, or network endpoint; never sets or reads the
// evidence-production approval environment value itself; produces no
// result eligible for Evidence Completion Review or Bound Selection
// (Decision Section 9). Exists so offline tests can exercise the
// complete artifact/integrity/ledger flow without ever invoking the
// double-gated live entry point below.
// ---------------------------------------------------------------------

data class FamilyFBoundingEvidenceProductionResult(
    val outputRoot: Path,
    val records: List<FamilyFBoundingRequestRecord>,
    val summary: FamilyFBoundingRequestSummary,
    val evidenceGaps: List<FamilyFEvidenceGapEntry>,
    val terminal: String,
)

// Recovery/resume outcomes (Plan Section 22). A resumed run never re-fetches
// or replaces already-finalized evidence, never duplicates a completed
// ledger step, and never guesses at ambiguous state -- it rejects and
// requires fresh governance instead.
sealed class FamilyFBoundingEvidenceResumeOutcome {
    data class AlreadyComplete(val outputRoot: Path) : FamilyFBoundingEvidenceResumeOutcome()
    data class Produced(val result: FamilyFBoundingEvidenceProductionResult, val resumedSteps: Set<FamilyFBoundingEvidenceStep>) : FamilyFBoundingEvidenceResumeOutcome()
    data class Rejected(val reason: String) : FamilyFBoundingEvidenceResumeOutcome()
}

private fun buildBoundingEvidenceReport(
    summary: FamilyFBoundingRequestSummary,
    gaps: List<FamilyFEvidenceGapEntry>,
): String = buildString {
    appendLine("# Family F Bounding Evidence Report (offline verification only)")
    appendLine()
    appendLine("This report was produced by offline verification of the accepted two-file")
    appendLine("implementation. It is NOT a governed evidence package: it was not run under a")
    appendLine("separately authorized Evidence Production Authorization Decision, and no value")
    appendLine("in this report is an accepted bound.")
    appendLine()
    appendLine("## WP-A request-bound evidence")
    appendLine()
    appendLine("- Observed record count: ${summary.observedRecordCount} (expected ${summary.expectedRecordCount})")
    appendLine("- Scored records: ${summary.scoredCount}; warm-up records: ${summary.warmupCount}")
    appendLine("- Global maximum request-body byte count observed: ${summary.globalMaxByteCount}")
    appendLine("- Trial IDs tied at that maximum: ${summary.globalMaxTrialIds.joinToString(", ")}")
    appendLine("- Unique request-body count: ${summary.uniqueBodyCount}")
    appendLine("- Status: ${summary.status}")
    appendLine("- PROPOSED_MAX_REQUEST_BOUND=${summary.globalMaxByteCount} bytes -- an EVIDENCE RESULT ONLY, not an accepted bound.")
    appendLine()
    appendLine("## WP-B/C/D evidence gaps")
    appendLine()
    if (gaps.isEmpty()) {
        appendLine("No evidence gaps recorded.")
    } else {
        gaps.forEach { gap ->
            appendLine("- [${gap.workPackage}] ${gap.question}")
            appendLine("  Reason: ${gap.reason}")
        }
    }
}

object FamilyFBoundingEvidenceProducer {

    private fun repositoryInputsJson(repositoryRoot: Path): String =
        FamilyFBoundingEvidenceRepositoryInputs.currentHashes(repositoryRoot).entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ",",
        ) { (path, hash) ->
            "\"${boundingEvidenceJsonEscape(path)}\":\"${boundingEvidenceJsonEscape(hash)}\""
        }

    private fun verifyIdentityArtifact(path: Path): String? {
        if (!Files.isRegularFile(path)) return "WP_A_ESTIMATOR completed but evidence-identity.json is missing"
        val identityPattern = Regex(
            """^\{"schemaVersion":1,"evidenceCampaignId":"offline-verification-only","startedAt":"([^"]+)"\}$""",
        )
        val match = identityPattern.matchEntire(Files.readString(path, StandardCharsets.UTF_8))
            ?: return "WP_A_ESTIMATOR completed but evidence-identity.json is malformed or changed"
        return try {
            Instant.parse(match.groupValues[1])
            null
        } catch (_: RuntimeException) {
            "WP_A_ESTIMATOR completed but evidence-identity.json contains invalid startedAt metadata"
        }
    }

    private fun verifyExactArtifact(path: Path, expected: String, step: FamilyFBoundingEvidenceStep): String? {
        if (!Files.exists(path)) return "progress-ledger.jsonl records ${step.name} complete but ${path.fileName} is missing"
        if (!Files.isRegularFile(path)) return "progress-ledger.jsonl records ${step.name} complete but ${path.fileName} is not a regular file"
        if (Files.readString(path, StandardCharsets.UTF_8) != expected) {
            return "progress-ledger.jsonl records ${step.name} complete but ${path.fileName} is malformed or changed"
        }
        return null
    }

    private fun verifyEmptyEvidenceDirectory(path: Path, step: FamilyFBoundingEvidenceStep): String? {
        if (!Files.isDirectory(path)) return "progress-ledger.jsonl records ${step.name} complete but ${path.fileName} is missing"
        Files.list(path).use { entries ->
            if (entries.findAny().isPresent) return "progress-ledger.jsonl records ${step.name} complete but ${path.fileName} is not empty"
        }
        return null
    }

    private fun rejectUnexpectedFinalStateForIncompleteStep(
        outputRoot: Path,
        step: FamilyFBoundingEvidenceStep,
        relativePaths: List<String>,
    ): String? {
        val existing = relativePaths.firstOrNull { Files.exists(outputRoot.resolve(it)) } ?: return null
        return "final artifact $existing exists but progress-ledger.jsonl does not record ${step.name} complete"
    }

    private fun validateResumeArtifacts(
        outputRoot: Path,
        completed: Set<FamilyFBoundingEvidenceStep>,
        repositoryRoot: Path,
    ): String? {
        val records = FamilyFBoundingEvidenceRequestEstimator.estimate()
        val expectedRecords = records.joinToString("\n", postfix = "\n") { it.toJsonLine() }
        val expectedSummary = FamilyFBoundingEvidenceRequestSummarizer.summarize(records).toJson()

        if (FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR in completed) {
            verifyIdentityArtifact(outputRoot.resolve("evidence-identity.json"))?.let { return it }
            verifyExactArtifact(
                outputRoot.resolve("repository-inputs.json"),
                repositoryInputsJson(repositoryRoot),
                FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR,
            )?.let { return it }
            verifyExactArtifact(outputRoot.resolve("request-estimator-records.jsonl"), expectedRecords, FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR)?.let { return it }
            verifyExactArtifact(outputRoot.resolve("request-estimator-summary.json"), expectedSummary, FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR)?.let { return it }
        } else {
            rejectUnexpectedFinalStateForIncompleteStep(
                outputRoot,
                FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR,
                listOf("evidence-identity.json", "repository-inputs.json", "request-estimator-records.jsonl", "request-estimator-summary.json"),
            )?.let { return it }
        }

        val packageArtifacts = listOf(
            Triple(
                FamilyFBoundingEvidenceStep.WP_B_RESPONSE_EVIDENCE,
                "response-primary-evidence-index.json",
                "{\"SOURCE_COUNT\":0,\"STATUS\":\"UNRESOLVED_NO_APPLICABLE_PRIMARY_SOURCE\"}",
            ) to "response-primary-evidence",
            Triple(
                FamilyFBoundingEvidenceStep.WP_C_HEADER_EVIDENCE,
                "header-primary-evidence-index.json",
                "{\"SOURCE_COUNT\":0,\"STATUS\":\"UNRESOLVED\"}",
            ) to "header-primary-evidence",
            Triple(
                FamilyFBoundingEvidenceStep.WP_D_RUNTIME_EVIDENCE,
                "runtime-primary-evidence-index.json",
                "{\"SOURCE_COUNT\":0,\"STATUS\":\"UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE\"}",
            ) to "runtime-primary-evidence",
        )
        packageArtifacts.forEach { (artifact, directoryName) ->
            val (step, fileName, expected) = artifact
            if (step in completed) {
                verifyExactArtifact(outputRoot.resolve(fileName), expected, step)?.let { return it }
                verifyEmptyEvidenceDirectory(outputRoot.resolve(directoryName), step)?.let { return it }
            } else {
                rejectUnexpectedFinalStateForIncompleteStep(outputRoot, step, listOf(fileName, directoryName))?.let { return it }
            }
        }
        return null
    }

    /**
     * Shared step execution, used by both produce() (always a fresh run,
     * alreadyCompleted always empty) and produceOrResume() (alreadyCompleted
     * reflects what the ledger already durably records). A step already in
     * alreadyCompleted is never re-recorded in the ledger -- Plan Section
     * 22's append-only, no-duplicate-record requirement -- but its mandatory
     * artifacts are still re-derived deterministically in memory (WP-A is
     * pure and side-effect-free to recompute) so downstream steps and the
     * final manifest always see consistent, complete data.
     */
    private fun executeSteps(
        outputRoot: Path,
        repositoryRoot: Path,
        ledger: FamilyFBoundingEvidenceLedger,
        alreadyCompleted: Set<FamilyFBoundingEvidenceStep>,
    ): FamilyFBoundingEvidenceProductionResult {
        val mandatoryArtifacts: MutableList<Path> = mutableListOf()
        val ledgerPath = outputRoot.resolve("progress-ledger.jsonl")

        if (FamilyFBoundingEvidenceStep.PREFLIGHT !in alreadyCompleted) {
            ledger.recordStep(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "offline evidence production preflight")
        }

        val identityPath = outputRoot.resolve("evidence-identity.json")
        if (!Files.exists(identityPath)) {
            Files.writeString(
                identityPath,
                "{\"schemaVersion\":1,\"evidenceCampaignId\":\"offline-verification-only\"," +
                    "\"startedAt\":\"${Instant.now()}\"}",
                StandardCharsets.UTF_8,
            )
        }
        mandatoryArtifacts.add(identityPath)

        val repositoryInputsPath = outputRoot.resolve("repository-inputs.json")
        if (!Files.exists(repositoryInputsPath)) {
            Files.writeString(repositoryInputsPath, repositoryInputsJson(repositoryRoot), StandardCharsets.UTF_8)
        }
        mandatoryArtifacts.add(repositoryInputsPath)

        val records = FamilyFBoundingEvidenceRequestEstimator.estimate()
        val requestRecordsPath = outputRoot.resolve("request-estimator-records.jsonl")
        val summary = FamilyFBoundingEvidenceRequestSummarizer.summarize(records)
        val summaryPath = outputRoot.resolve("request-estimator-summary.json")
        if (FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR !in alreadyCompleted) {
            Files.writeString(requestRecordsPath, records.joinToString("\n", postfix = "\n") { it.toJsonLine() }, StandardCharsets.UTF_8)
            Files.writeString(summaryPath, summary.toJson(), StandardCharsets.UTF_8)
            ledger.recordStep(FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR, "OK", "produced ${records.size} records")
        }
        mandatoryArtifacts.add(requestRecordsPath)
        mandatoryArtifacts.add(summaryPath)

        val gaps = mutableListOf<FamilyFEvidenceGapEntry>()

        val (responseStatus, responseGap) = FamilyFBoundingResponseEvidenceInventory.evaluate(emptyList())
        responseGap?.let { gaps += it }
        val responseIndexPath = outputRoot.resolve("response-primary-evidence-index.json")
        if (!Files.exists(responseIndexPath)) {
            Files.writeString(responseIndexPath, "{\"SOURCE_COUNT\":0,\"STATUS\":\"${responseStatus.name}\"}", StandardCharsets.UTF_8)
        }
        mandatoryArtifacts.add(responseIndexPath)
        Files.createDirectories(outputRoot.resolve("response-primary-evidence"))
        if (FamilyFBoundingEvidenceStep.WP_B_RESPONSE_EVIDENCE !in alreadyCompleted) {
            ledger.recordStep(FamilyFBoundingEvidenceStep.WP_B_RESPONSE_EVIDENCE, responseStatus.name, "no pre-supplied sources")
        }

        val (headerStatus, headerGap) = FamilyFBoundingHeaderEvidenceInventory.evaluate(emptyList())
        headerGap?.let { gaps += it }
        val headerIndexPath = outputRoot.resolve("header-primary-evidence-index.json")
        if (!Files.exists(headerIndexPath)) {
            Files.writeString(headerIndexPath, "{\"SOURCE_COUNT\":0,\"STATUS\":\"$headerStatus\"}", StandardCharsets.UTF_8)
        }
        mandatoryArtifacts.add(headerIndexPath)
        Files.createDirectories(outputRoot.resolve("header-primary-evidence"))
        if (FamilyFBoundingEvidenceStep.WP_C_HEADER_EVIDENCE !in alreadyCompleted) {
            ledger.recordStep(FamilyFBoundingEvidenceStep.WP_C_HEADER_EVIDENCE, headerStatus, "no pre-supplied sources")
        }

        val (runtimeStatus, runtimeGap) = FamilyFBoundingRuntimeEvidenceInventory.evaluate(emptyList())
        runtimeGap?.let { gaps += it }
        val runtimeIndexPath = outputRoot.resolve("runtime-primary-evidence-index.json")
        if (!Files.exists(runtimeIndexPath)) {
            Files.writeString(runtimeIndexPath, "{\"SOURCE_COUNT\":0,\"STATUS\":\"${runtimeStatus.name}\"}", StandardCharsets.UTF_8)
        }
        mandatoryArtifacts.add(runtimeIndexPath)
        Files.createDirectories(outputRoot.resolve("runtime-primary-evidence"))
        if (FamilyFBoundingEvidenceStep.WP_D_RUNTIME_EVIDENCE !in alreadyCompleted) {
            ledger.recordStep(FamilyFBoundingEvidenceStep.WP_D_RUNTIME_EVIDENCE, runtimeStatus.name, "no pre-supplied sources")
        }

        val gapRegisterPath = outputRoot.resolve("evidence-gap-register.json")
        Files.writeString(gapRegisterPath, gaps.toJson(), StandardCharsets.UTF_8)
        mandatoryArtifacts.add(gapRegisterPath)

        val reportPath = outputRoot.resolve("bounding-evidence-report.md")
        Files.writeString(reportPath, buildBoundingEvidenceReport(summary, gaps), StandardCharsets.UTF_8)
        mandatoryArtifacts.add(reportPath)

        ledger.recordStep(FamilyFBoundingEvidenceStep.WP_E_VALIDATION, "OK", "validating artifacts and finalizing ledger")
        ledger.finalizeLedger()
        mandatoryArtifacts.add(ledgerPath)

        FamilyFBoundingEvidenceIntegrity.writeManifest(outputRoot, mandatoryArtifacts)
        val verified = FamilyFBoundingEvidenceIntegrity.verifyFromFreshCopy(outputRoot, outputRoot.parent ?: outputRoot)

        val terminal = if (verified) {
            FamilyFBoundingEvidenceTerminal.writeComplete(outputRoot)
            "COMPLETE"
        } else {
            FamilyFBoundingEvidenceTerminal.writeFailed(outputRoot, "copied-directory verification failed")
            "FAILED"
        }

        return FamilyFBoundingEvidenceProductionResult(outputRoot, records, summary, gaps, terminal)
    }

    fun produce(outputRoot: Path, repositoryRoot: Path = Path.of(".")): FamilyFBoundingEvidenceProductionResult {
        if (Files.exists(outputRoot)) {
            Files.list(outputRoot).use { entries ->
                check(!entries.findAny().isPresent) { "fresh evidence output root must be absent or empty" }
            }
        }
        Files.createDirectories(outputRoot)
        val ledger = FamilyFBoundingEvidenceLedger(outputRoot.resolve("progress-ledger.jsonl"))
        return executeSteps(outputRoot, repositoryRoot, ledger, emptySet())
    }

    /**
     * Resume-aware entry (Plan Section 22). Safe to call repeatedly
     * against the same output root:
     *
     * - a pre-existing evidence.complete is verified, never re-derived;
     * - a pre-existing evidence.failed halts resumption -- a failed
     *   terminal state requires fresh governance, not a silent retry;
     * - a ledger already carrying WP_E_VALIDATION but no terminal marker
     *   is a conflicting/torn state and is rejected, not guessed at;
     * - any already-completed ledger step is skipped -- never re-recorded,
     *   never duplicated -- only after independently re-deriving that
     *   step's artifacts and confirming they are byte-identical to what is
     *   already on disk; a mismatch halts rather than silently overwriting;
     * - otherwise this is a fresh, clean run, identical to produce().
     */
    fun produceOrResume(outputRoot: Path, repositoryRoot: Path = Path.of(".")): FamilyFBoundingEvidenceResumeOutcome {
        val completeMarker = outputRoot.resolve("evidence.complete")
        val failedMarker = outputRoot.resolve("evidence.failed")
        val manifestPath = outputRoot.resolve("SHA256SUMS.txt")
        val ledgerPath = outputRoot.resolve("progress-ledger.jsonl")

        FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(outputRoot)?.let {
            return FamilyFBoundingEvidenceResumeOutcome.Rejected(it)
        }

        if (Files.exists(completeMarker) && Files.exists(failedMarker)) {
            return FamilyFBoundingEvidenceResumeOutcome.Rejected(
                "conflicting terminal state: both evidence.complete and evidence.failed present",
            )
        }
        if (Files.exists(completeMarker)) {
            val completed = try {
                FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(ledgerPath)
            } catch (failure: FamilyFBoundingEvidenceLedgerMalformedException) {
                return FamilyFBoundingEvidenceResumeOutcome.Rejected(failure.message ?: "malformed progress ledger")
            }
            return if (
                completed == FamilyFBoundingEvidenceStep.entries.toSet() &&
                FamilyFBoundingEvidenceIntegrity.verifyManifest(outputRoot, manifestPath)
            ) {
                FamilyFBoundingEvidenceResumeOutcome.AlreadyComplete(outputRoot)
            } else {
                FamilyFBoundingEvidenceResumeOutcome.Rejected(
                    "evidence.complete present but finalized ledger or manifest verification failed",
                )
            }
        }
        if (Files.exists(failedMarker)) {
            return FamilyFBoundingEvidenceResumeOutcome.Rejected(
                "evidence.failed present -- a failed terminal state requires fresh governance, not resumption",
            )
        }

        val alreadyCompleted = try {
            FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(ledgerPath)
        } catch (failure: FamilyFBoundingEvidenceLedgerMalformedException) {
            return FamilyFBoundingEvidenceResumeOutcome.Rejected(failure.message ?: "malformed progress ledger")
        }
        if (FamilyFBoundingEvidenceStep.WP_E_VALIDATION in alreadyCompleted) {
            return FamilyFBoundingEvidenceResumeOutcome.Rejected(
                "progress-ledger.jsonl already records WP_E_VALIDATION but no terminal marker exists -- " +
                    "conflicting/torn state; requires fresh governance, not silent resumption",
            )
        }

        validateResumeArtifacts(outputRoot, alreadyCompleted, repositoryRoot)?.let {
            return FamilyFBoundingEvidenceResumeOutcome.Rejected(it)
        }

        Files.createDirectories(outputRoot)
        val ledger = FamilyFBoundingEvidenceLedger(ledgerPath)
        val result = executeSteps(outputRoot, repositoryRoot, ledger, alreadyCompleted)
        return FamilyFBoundingEvidenceResumeOutcome.Produced(result, alreadyCompleted)
    }
}

// ---------------------------------------------------------------------
// Double-gated evidence-producing entry point (Decision Sections 6-9;
// Plan Section 6). Checks the live-execution refusal gate first, then
// both positive gates, before resolving any output root, constructing
// any writer, or touching the filesystem. Invokes the real evidence
// entry point (produce) exactly once, only when both positive gates hold
// and the negative gate is absent.
// ---------------------------------------------------------------------

fun familyFBoundingEvidenceEntryPoint(
    systemProperty: String?,
    approvalEnv: String?,
    executionApprovedEnv: String?,
    outputRootEnv: String?,
    produce: (Path) -> FamilyFBoundingEvidenceProductionResult = { FamilyFBoundingEvidenceProducer.produce(it) },
): FamilyFBoundingEvidenceProductionResult? {
    if (executionApprovedEnv != null) return null
    if (systemProperty != "true") return null
    if (approvalEnv != "true") return null
    val root = outputRootEnv ?: return null
    return produce(Path.of(root))
}

// ---------------------------------------------------------------------
// Source-inspection isolation (mirrors requireFamilyFDownstreamIsolated
// in ReasoningProtocolFamilyFDiagnosticTest.kt).
// ---------------------------------------------------------------------

// FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SCAN_EXCLUDE_START -- this declaration
// necessarily contains the literal forbidden strings; excluded from its own
// scan the same way the established FAMILY_F_FORBIDDEN_SYMBOLS precedent
// (ReasoningProtocolFamilyFDiagnosticTest.kt) excludes itself.
val FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SYMBOLS = listOf(
    "java.net.Socket",
    "java.net.ServerSocket",
    "java.net.http.HttpClient",
    "java.net.HttpURLConnection",
    "com.sun.net.httpserver",
    "ProcessBuilder",
    "Runtime.getRuntime()",
    "DockerClient",
    "docker compose",
    "docker-compose",
    "FamilyFRealModelCaller",
    "FamilyFRealModelUnloadCommand",
    "FamilyFCaptureProxy",
    "FamilyFLiveEntryPoint",
    "ConversationReplyCoordinator",
    "MemoryAdmissionCoordinator",
    "MemoryCore",
    "ParkerRuntime",
    "parker.composition.Main",
)
// FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SCAN_EXCLUDE_END

private fun familyFBoundingEvidenceStripExcludedScanBlocks(text: String): String {
    val startMarker = "// FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SCAN_EXCLUDE_START"
    val endMarker = "// FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SCAN_EXCLUDE_END"
    val builder = StringBuilder()
    var index = 0
    while (true) {
        val start = text.indexOf(startMarker, index)
        if (start < 0) {
            builder.append(text.substring(index))
            break
        }
        builder.append(text.substring(index, start))
        val end = text.indexOf(endMarker, start)
        require(end >= 0) { "unterminated forbidden-scan-exclude block" }
        index = end + endMarker.length
    }
    return builder.toString()
}

fun familyFBoundingEvidenceScanSafeSource(path: Path): String =
    familyFBoundingEvidenceStripExcludedScanBlocks(Files.readString(path))

/**
 * Pure, string-in/string-out check usable directly by tests against
 * synthetic source text, so the exclusion-region behaviour itself can be
 * proven without ever writing a real forbidden symbol into this file.
 */
fun familyFBoundingEvidenceForbiddenSymbolsFound(
    text: String,
    symbols: List<String> = FAMILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SYMBOLS,
): List<String> {
    val scanned = familyFBoundingEvidenceStripExcludedScanBlocks(text)
    return symbols.filter { it in scanned }
}

fun requireFamilyFBoundingEvidenceIsolated() {
    val source = familyFBoundingEvidenceScanSafeSource(Path.of("tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt"))
    val found = familyFBoundingEvidenceForbiddenSymbolsFound(source)
    check(found.isEmpty()) { "Family F bounding-evidence source unexpectedly references forbidden symbol(s): $found" }
}

// ---------------------------------------------------------------------
// Offline test suite.
// ---------------------------------------------------------------------

class ReasoningProtocolFamilyFBoundingEvidenceTest {

    private fun snapshotRegularFiles(root: Path): Map<String, List<Byte>> {
        val snapshot = linkedMapOf<String, List<Byte>>()
        Files.walk(root).use { stream ->
            stream.filter { Files.isRegularFile(it) }.forEach {
                snapshot[root.relativize(it).toString()] = Files.readAllBytes(it).toList()
            }
        }
        return snapshot
    }

    private fun countVerificationCopies(parent: Path): Long =
        Files.list(parent).use { entries ->
            entries.filter { it.fileName.toString().startsWith("bounding-evidence-copy-") }.count()
        }

    private fun createValidPartialCampaign(root: Path, through: FamilyFBoundingEvidenceStep) {
        Files.createDirectories(root)
        val ledger = FamilyFBoundingEvidenceLedger(root.resolve("progress-ledger.jsonl"))
        val ordered = FamilyFBoundingEvidenceStep.entries.take(through.ordinal + 1)
        ordered.forEach { step ->
            when (step) {
                FamilyFBoundingEvidenceStep.PREFLIGHT -> ledger.recordStep(step, "OK", "offline evidence production preflight")
                FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR -> {
                    val records = FamilyFBoundingEvidenceRequestEstimator.estimate()
                    Files.writeString(
                        root.resolve("evidence-identity.json"),
                        "{\"schemaVersion\":1,\"evidenceCampaignId\":\"offline-verification-only\",\"startedAt\":\"${Instant.now()}\"}",
                        StandardCharsets.UTF_8,
                    )
                    val repositoryInputs = FamilyFBoundingEvidenceRepositoryInputs.currentHashes(Path.of(".")).entries.joinToString(
                        prefix = "{", postfix = "}", separator = ",",
                    ) { (path, hash) -> "\"${boundingEvidenceJsonEscape(path)}\":\"${boundingEvidenceJsonEscape(hash)}\"" }
                    Files.writeString(root.resolve("repository-inputs.json"), repositoryInputs, StandardCharsets.UTF_8)
                    Files.writeString(
                        root.resolve("request-estimator-records.jsonl"),
                        records.joinToString("\n", postfix = "\n") { it.toJsonLine() },
                        StandardCharsets.UTF_8,
                    )
                    Files.writeString(
                        root.resolve("request-estimator-summary.json"),
                        FamilyFBoundingEvidenceRequestSummarizer.summarize(records).toJson(),
                        StandardCharsets.UTF_8,
                    )
                    ledger.recordStep(step, "OK", "produced ${records.size} records")
                }
                FamilyFBoundingEvidenceStep.WP_B_RESPONSE_EVIDENCE -> {
                    Files.writeString(
                        root.resolve("response-primary-evidence-index.json"),
                        "{\"SOURCE_COUNT\":0,\"STATUS\":\"UNRESOLVED_NO_APPLICABLE_PRIMARY_SOURCE\"}",
                        StandardCharsets.UTF_8,
                    )
                    Files.createDirectories(root.resolve("response-primary-evidence"))
                    ledger.recordStep(step, "UNRESOLVED_NO_APPLICABLE_PRIMARY_SOURCE", "no pre-supplied sources")
                }
                FamilyFBoundingEvidenceStep.WP_C_HEADER_EVIDENCE -> {
                    Files.writeString(
                        root.resolve("header-primary-evidence-index.json"),
                        "{\"SOURCE_COUNT\":0,\"STATUS\":\"UNRESOLVED\"}",
                        StandardCharsets.UTF_8,
                    )
                    Files.createDirectories(root.resolve("header-primary-evidence"))
                    ledger.recordStep(step, "UNRESOLVED", "no pre-supplied sources")
                }
                FamilyFBoundingEvidenceStep.WP_D_RUNTIME_EVIDENCE -> {
                    Files.writeString(
                        root.resolve("runtime-primary-evidence-index.json"),
                        "{\"SOURCE_COUNT\":0,\"STATUS\":\"UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE\"}",
                        StandardCharsets.UTF_8,
                    )
                    Files.createDirectories(root.resolve("runtime-primary-evidence"))
                    ledger.recordStep(step, "UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE", "no pre-supplied sources")
                }
                FamilyFBoundingEvidenceStep.WP_E_VALIDATION -> ledger.recordStep(step, "OK", "validating artifacts and finalizing ledger")
            }
        }
    }

    private fun assertRejectedWithoutMutation(root: Path, expectedReasonFragment: String) {
        val before = snapshotRegularFiles(root)
        val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
        val rejected = outcome as? FamilyFBoundingEvidenceResumeOutcome.Rejected
        assertTrue(rejected != null, "expected Rejected, got $outcome")
        assertTrue(rejected!!.reason.contains(expectedReasonFragment), "unexpected rejection reason: ${rejected.reason}")
        val after = snapshotRegularFiles(root)
        assertEquals(before, after, "failed resume must leave campaign state byte-unchanged")
    }

    // ---- Double gate / self-skip ----

    // Real, genuine invocations of familyFBoundingEvidenceEntryPoint for all
    // four gate combinations (Decision Section 7 item 1; Section 8). The
    // `produce` argument is a counting wrapper that still delegates to the
    // real FamilyFBoundingEvidenceProducer.produce -- it preserves the real
    // entry path end to end (including, in case D, a real, fully offline
    // production run against a @TempDir) while letting each case assert the
    // exact invocation count. No case ever sets or reads the real
    // evidence-production approval environment value in this JVM; every
    // gate value below is passed as an explicit function argument, never
    // read from the live environment.

    @Test
    fun `A - both gates false - real entry path does not reach the produce operation`(@TempDir tempDir: Path) {
        var invocationCount = 0
        val spy: (Path) -> FamilyFBoundingEvidenceProductionResult = { invocationCount++; FamilyFBoundingEvidenceProducer.produce(it) }
        val result = familyFBoundingEvidenceEntryPoint(
            systemProperty = null,
            approvalEnv = null,
            executionApprovedEnv = null,
            outputRootEnv = tempDir.resolve("case-a").toString(),
            produce = spy,
        )
        assertNull(result)
        assertEquals(0, invocationCount)
    }

    @Test
    fun `B - gate 1 true, gate 2 false - real entry path does not reach the produce operation`(@TempDir tempDir: Path) {
        var invocationCount = 0
        val spy: (Path) -> FamilyFBoundingEvidenceProductionResult = { invocationCount++; FamilyFBoundingEvidenceProducer.produce(it) }
        val result = familyFBoundingEvidenceEntryPoint(
            systemProperty = "true",
            approvalEnv = null,
            executionApprovedEnv = null,
            outputRootEnv = tempDir.resolve("case-b").toString(),
            produce = spy,
        )
        assertNull(result)
        assertEquals(0, invocationCount)
    }

    @Test
    fun `C - gate 1 false, gate 2 true - real entry path does not reach the produce operation`(@TempDir tempDir: Path) {
        var invocationCount = 0
        val spy: (Path) -> FamilyFBoundingEvidenceProductionResult = { invocationCount++; FamilyFBoundingEvidenceProducer.produce(it) }
        val result = familyFBoundingEvidenceEntryPoint(
            systemProperty = null,
            approvalEnv = "true",
            executionApprovedEnv = null,
            outputRootEnv = tempDir.resolve("case-c").toString(),
            produce = spy,
        )
        assertNull(result)
        assertEquals(0, invocationCount)
    }

    @Test
    fun `D - both gates true - real entry path reaches the real produce operation exactly once`(@TempDir tempDir: Path) {
        var invocationCount = 0
        val spy: (Path) -> FamilyFBoundingEvidenceProductionResult = { invocationCount++; FamilyFBoundingEvidenceProducer.produce(it) }
        val root = tempDir.resolve("case-d")
        val result = familyFBoundingEvidenceEntryPoint(
            systemProperty = "true",
            approvalEnv = "true",
            executionApprovedEnv = null,
            outputRootEnv = root.toString(),
            produce = spy,
        )
        assertNotNull(result)
        assertEquals(1, invocationCount)
        assertEquals("COMPLETE", result!!.terminal)
        assertEquals(392, result.records.size)
    }

    @Test
    fun `bounding-evidence entry point refuses when the live-execution approval variable is present, regardless of value, even with both other gates true`(@TempDir tempDir: Path) {
        var invocationCount = 0
        val spy: (Path) -> FamilyFBoundingEvidenceProductionResult = { invocationCount++; FamilyFBoundingEvidenceProducer.produce(it) }
        val result = familyFBoundingEvidenceEntryPoint(
            systemProperty = "true",
            approvalEnv = "true",
            executionApprovedEnv = "true",
            outputRootEnv = tempDir.resolve("case-refused").toString(),
            produce = spy,
        )
        assertNull(result)
        assertEquals(0, invocationCount)
    }

    @Test
    fun `bounding-evidence entry point self-skips under this JVM's own real environment, unless a real evidence run was genuinely authorized`() {
        val realResult = familyFBoundingEvidenceEntryPoint(
            systemProperty = System.getProperty(FAMILY_F_BOUNDING_EVIDENCE_PROPERTY),
            approvalEnv = System.getenv(FAMILY_F_BOUNDING_EVIDENCE_APPROVED_ENV),
            executionApprovedEnv = System.getenv(FAMILY_F_EXECUTION_APPROVED_ENV),
            outputRootEnv = System.getenv(FAMILY_F_BOUNDING_EVIDENCE_ROOT_ENV),
        )
        assumeTrue(
            System.getProperty(FAMILY_F_BOUNDING_EVIDENCE_PROPERTY) != "true" ||
                System.getenv(FAMILY_F_BOUNDING_EVIDENCE_APPROVED_ENV) != "true" ||
                System.getenv(FAMILY_F_EXECUTION_APPROVED_ENV) != null,
            "ordinary/offline verification: at least one gate condition must prevent a real run",
        )
        assertNull(realResult, "entry point must self-skip under ordinary/offline verification's real environment")
    }

    @Test
    fun `bounding-evidence entry point checks the negative gate, then both positive gates, before resolving output or invoking the real entry point exactly once`() {
        val source = familyFBoundingEvidenceScanSafeSource(Path.of("tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt"))
        val marker = "fun familyFBoundingEvidenceEntryPoint("
        val start = source.indexOf(marker)
        assertTrue(start >= 0, "entry point function must exist in this file's own source")
        val signatureEnd = "): FamilyFBoundingEvidenceProductionResult? {"
        val signatureEndIndex = source.indexOf(signatureEnd, start)
        assertTrue(signatureEndIndex >= 0, "entry point function signature must end exactly as expected")
        val bodyStart = signatureEndIndex + signatureEnd.length - 1
        val bodyEnd = source.indexOf("\n}\n", bodyStart)
        val body = source.substring(bodyStart, bodyEnd)

        val negativeGateIndex = body.indexOf("executionApprovedEnv != null")
        val propertyGateIndex = body.indexOf("systemProperty != \"true\"")
        val approvalGateIndex = body.indexOf("approvalEnv != \"true\"")
        val outputRootIndex = body.indexOf("outputRootEnv ?:")
        val produceCallIndex = body.indexOf("produce(")

        assertTrue(negativeGateIndex >= 0 && propertyGateIndex >= 0 && approvalGateIndex >= 0 && outputRootIndex >= 0 && produceCallIndex >= 0)
        assertTrue(negativeGateIndex < propertyGateIndex, "the negative (live-execution) gate must be checked first")
        assertTrue(propertyGateIndex < approvalGateIndex, "the enabled-property gate must be checked before the approval gate")
        assertTrue(approvalGateIndex < outputRootIndex, "output-root resolution must not occur before both positive gates")
        assertTrue(outputRootIndex < produceCallIndex, "the real entry point must not be invoked before output-root resolution")
        assertEquals(1, Regex(Regex.escape("produce(")).findAll(body).count(), "the real evidence entry point must be invoked exactly once")
    }

    // ---- Source inspection ----

    @Test
    fun `source contains no forbidden network, process, Docker, or production-live-diagnostic symbol`() {
        requireFamilyFBoundingEvidenceIsolated()
    }

    // ---- WP-A estimator correctness ----

    @Test
    fun `estimator output is deterministic and byte-identical across two independent runs`() {
        val first = FamilyFBoundingEvidenceRequestEstimator.estimate()
        val second = FamilyFBoundingEvidenceRequestEstimator.estimate()
        assertEquals(first.map { it.toJsonLine() }, second.map { it.toJsonLine() })
    }

    @Test
    fun `estimator produces exactly 392 records with trial IDs identical to the frozen schedule, no missing or duplicate`() {
        val records = FamilyFBoundingEvidenceRequestEstimator.estimate()
        assertEquals(392, records.size)
        val expectedIds = FamilyFCampaignDefinition.allTrials.map { it.id }
        val actualIds = records.map { it.trialId }
        assertEquals(expectedIds, actualIds)
        assertEquals(actualIds.size, actualIds.distinct().size)
    }

    @Test
    fun `estimator covers both frozen model names, both profiles, every fixture for both roles, and all 24 warm-up records`() {
        val records = FamilyFBoundingEvidenceRequestEstimator.estimate()

        val modelNames = records.map { it.modelName }.toSet()
        assertEquals(setOf(FAMILY_F_SUBJECT_MODEL_NAME, FAMILY_F_CONTROL_MODEL_NAME), modelNames)

        val scored = records.filterNot { it.warmup }
        assertEquals(368, scored.size)
        val profileIds = scored.map { it.profileId }.toSet()
        assertEquals(FamilyFCorpus.profiles.map { it.externalId }.toSet(), profileIds)

        FamilyFRole.values().forEach { role ->
            FamilyFCorpus.profiles.forEach { profile ->
                val cell = scored.filter { it.role == role.name && it.profileId == profile.externalId }
                assertEquals(FamilyFCorpus.fixtures.size * 4, cell.size, "role=$role profile=$profile must contain 23 fixtures x 4 repetitions")
                val fixtureIds = cell.map { it.fixtureId }.toSet()
                assertEquals(FamilyFCorpus.fixtures.map { it.id }.toSet(), fixtureIds, "role=$role profile=$profile must cover every fixture")
            }
        }

        val warmups = records.filter { it.warmup }
        assertEquals(24, warmups.size)
        assertTrue(warmups.all { it.fixtureId == FamilyFCorpus.warmupFixture.id })
    }

    @Test
    fun `UTF-8 byte count differs correctly from character count for the adversarial Unicode fixture`() {
        val records = FamilyFBoundingEvidenceRequestEstimator.estimate()
        val unicodeRecords = records.filter { it.fixtureId == "n02-unicode-whitespace" }
        assertTrue(unicodeRecords.isNotEmpty())
        unicodeRecords.forEach { record ->
            val decodedBytes = Base64.getDecoder().decode(record.requestBodyBase64)
            val decodedText = String(decodedBytes, StandardCharsets.UTF_8)
            assertEquals(decodedBytes.size.toLong(), record.requestBodyUtf8ByteCount)
            assertTrue(decodedBytes.size > decodedText.length, "multi-byte UTF-8 content must make byte count exceed character count")
        }
    }

    @Test
    fun `Base64 request body and prompt hashes round-trip exactly`() {
        val records = FamilyFBoundingEvidenceRequestEstimator.estimate()
        records.take(10).forEach { record ->
            val decoded = Base64.getDecoder().decode(record.requestBodyBase64)
            assertEquals(record.requestBodyUtf8ByteCount, decoded.size.toLong())
            assertEquals(record.requestBodySha256, boundingEvidenceSha256Bytes(decoded))

            val line = record.toJsonLine()
            assertTrue(line.contains("\"trialId\":\"${record.trialId}\""))
            assertTrue(line.contains("\"requestBodySha256\":\"${record.requestBodySha256}\""))
            assertFalse(line.contains("\n"))
        }
    }

    @Test
    fun `summarizer reports every tied maximum on a synthetic input with a deliberate tie`() {
        fun sample(sequence: Int, trialId: String, bytes: Long) = FamilyFBoundingRequestRecord(
            schemaVersion = 1,
            sequence = sequence,
            trialId = trialId,
            blockId = "block",
            blockOrder = sequence,
            warmup = false,
            role = "SUBJECT",
            modelName = "irrelevant",
            fixtureId = "fixture",
            profileId = "profile",
            repetition = 1,
            promptUtf8ByteCount = bytes,
            promptSha256 = "hash",
            requestBodyUtf8ByteCount = bytes,
            requestBodySha256 = "hash-$sequence",
            requestBodyBase64 = "AA==",
        )
        val a = sample(0, "t-a", 100L)
        val b = sample(1, "t-b", 100L)
        val c = sample(2, "t-c", 50L)
        val summary = FamilyFBoundingEvidenceRequestSummarizer.summarize(listOf(a, b, c))
        assertEquals(100L, summary.globalMaxByteCount)
        assertEquals(listOf("t-a", "t-b"), summary.globalMaxTrialIds)
    }

    @Test
    fun `summarizer maximum matches independent recomputation over the real estimator output`() {
        val records = FamilyFBoundingEvidenceRequestEstimator.estimate()
        val summary = FamilyFBoundingEvidenceRequestSummarizer.summarize(records)
        val recomputedMax = records.maxOf { it.requestBodyUtf8ByteCount }
        assertEquals(recomputedMax, summary.globalMaxByteCount)
        val recomputedTied = records.filter { it.requestBodyUtf8ByteCount == recomputedMax }.map { it.trialId }.sorted()
        assertEquals(recomputedTied, summary.globalMaxTrialIds)
        assertTrue(summary.globalMaxTrialIds.isNotEmpty())
    }

    @Test
    fun `checked arithmetic rejects overflow`() {
        assertFailsWith<ArithmeticException> { checkedAdd(Long.MAX_VALUE, 1L) }
        assertEquals(5L, checkedAdd(2L, 3L))
    }

    @Test
    fun `protected-input drift check halts on any hash mismatch or missing input`() {
        val expected = mapOf("a.kt" to "hash-a", "b.kt" to "hash-b")
        checkProtectedInputDrift(mapOf("a.kt" to "hash-a", "b.kt" to "hash-b"), expected)
        assertFailsWith<FamilyFBoundingEvidenceProtectedInputDriftException> {
            checkProtectedInputDrift(mapOf("a.kt" to "hash-a", "b.kt" to "DIFFERENT"), expected)
        }
        assertFailsWith<FamilyFBoundingEvidenceProtectedInputDriftException> {
            checkProtectedInputDrift(mapOf("a.kt" to "hash-a"), expected)
        }
    }

    @Test
    fun `repository-input hashes resolve for every protected path from the project root`() {
        val hashes = FamilyFBoundingEvidenceRepositoryInputs.currentHashes(Path.of("."))
        hashes.forEach { (path, hash) ->
            assertTrue(hash != "MISSING", "protected input not found at expected path: $path")
            assertEquals(64, hash.length)
        }
    }

    // ---- WP-B/C/D honest evidence-gap handling ----

    @Test
    fun `WP-B, WP-C, and WP-D honestly report unresolved status and a complete evidence-gap record when no source is supplied`() {
        val (responseStatus, responseGap) = FamilyFBoundingResponseEvidenceInventory.evaluate(emptyList())
        assertEquals(FamilyFResponseEvidenceStatus.UNRESOLVED_NO_APPLICABLE_PRIMARY_SOURCE, responseStatus)
        assertTrue(responseGap != null && responseGap.sourcesSearched.isEmpty())

        val (headerStatus, headerGap) = FamilyFBoundingHeaderEvidenceInventory.evaluate(emptyList())
        assertEquals("UNRESOLVED", headerStatus)
        assertTrue(headerGap != null && headerGap.sourcesSearched.isEmpty())

        val (runtimeStatus, runtimeGap) = FamilyFBoundingRuntimeEvidenceInventory.evaluate(emptyList())
        assertEquals(FamilyFRuntimeEvidenceStatus.UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE, runtimeStatus)
        assertTrue(runtimeGap != null && runtimeGap.sourcesSearched.isEmpty())
    }

    // ---- Fake-driven / temporary-directory offline production tests (WP-E, ledger, integrity, terminal) ----

    @Test
    fun `offline production writes exactly the mandatory artifact set and a COMPLETE terminal marker, verified from a fresh copy`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("evidence-root")
        val result = FamilyFBoundingEvidenceProducer.produce(root)

        assertEquals("COMPLETE", result.terminal)
        assertTrue(Files.exists(root.resolve("evidence.complete")))
        assertFalse(Files.exists(root.resolve("evidence.failed")))
        assertEquals(392, result.records.size)

        val mandatory = listOf(
            "evidence-identity.json", "repository-inputs.json", "progress-ledger.jsonl",
            "request-estimator-records.jsonl", "request-estimator-summary.json",
            "response-primary-evidence-index.json", "header-primary-evidence-index.json",
            "runtime-primary-evidence-index.json", "evidence-gap-register.json",
            "bounding-evidence-report.md", "SHA256SUMS.txt", "evidence.complete",
        )
        mandatory.forEach { name -> assertTrue(Files.exists(root.resolve(name)), "missing mandatory artifact: $name") }
        listOf("response-primary-evidence", "header-primary-evidence", "runtime-primary-evidence").forEach { dir ->
            assertTrue(Files.isDirectory(root.resolve(dir)))
        }

        assertTrue(FamilyFBoundingEvidenceIntegrity.verifyManifest(root, root.resolve("SHA256SUMS.txt")))
        assertEquals(3, result.evidenceGaps.size)
    }

    @Test
    fun `ledger rejects any append after finalization`(@TempDir tempDir: Path) {
        val ledgerPath = tempDir.resolve("progress-ledger.jsonl")
        val ledger = FamilyFBoundingEvidenceLedger(ledgerPath)
        ledger.recordStep(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "start")
        ledger.finalizeLedger()
        assertFailsWith<FamilyFBoundingEvidenceLedgerFinalizedException> {
            ledger.recordStep(FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR, "OK", "should fail")
        }
    }

    @Test
    fun `manifest verification detects tampering after production`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("evidence-root-tamper")
        FamilyFBoundingEvidenceProducer.produce(root)
        Files.writeString(root.resolve("request-estimator-summary.json"), "{\"tampered\":true}", StandardCharsets.UTF_8)
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(root, root.resolve("SHA256SUMS.txt")))
    }

    @Test
    fun `copied-directory verification succeeds over an untampered production`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("evidence-root-copy")
        FamilyFBoundingEvidenceProducer.produce(root)
        val campaignBefore = snapshotRegularFiles(root)
        val copiesBefore = countVerificationCopies(tempDir)
        assertTrue(FamilyFBoundingEvidenceIntegrity.verifyFromFreshCopy(root, tempDir))
        assertEquals(copiesBefore, countVerificationCopies(tempDir), "fresh verification copy must be deleted after success")
        val campaignAfter = snapshotRegularFiles(root)
        assertEquals(campaignBefore, campaignAfter, "copy verification must not modify the original campaign")
    }

    @Test
    fun `copied-directory verification deletes its copy after verification failure`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("evidence-root-copy-failure")
        FamilyFBoundingEvidenceProducer.produce(root)
        Files.writeString(root.resolve("request-estimator-summary.json"), "tampered", StandardCharsets.UTF_8)
        val copiesBefore = countVerificationCopies(tempDir)
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyFromFreshCopy(root, tempDir))
        assertEquals(copiesBefore, countVerificationCopies(tempDir))
        assertEquals("tampered", Files.readString(root.resolve("request-estimator-summary.json"), StandardCharsets.UTF_8))
    }

    @Test
    fun `copied-directory verification deletes its copy after an exception`(@TempDir tempDir: Path) {
        val missingRoot = tempDir.resolve("missing-campaign")
        val copiesBefore = countVerificationCopies(tempDir)
        assertFailsWith<Exception> { FamilyFBoundingEvidenceIntegrity.verifyFromFreshCopy(missingRoot, tempDir) }
        assertEquals(copiesBefore, countVerificationCopies(tempDir))
        assertFalse(Files.exists(missingRoot))
    }

    @Test
    fun `terminal marker is mutually exclusive`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("evidence-root-terminal")
        Files.createDirectories(root)
        FamilyFBoundingEvidenceTerminal.writeComplete(root)
        assertFailsWith<IllegalStateException> { FamilyFBoundingEvidenceTerminal.writeFailed(root, "should not be reachable") }
    }

    // ---- Recovery / resume (Plan Section 22) ----

    @Test
    fun `resume from clean initial state performs a fresh run with no steps skipped`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("resume-clean")
        val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
        val produced = outcome as? FamilyFBoundingEvidenceResumeOutcome.Produced
        assertTrue(produced != null, "expected Produced, got $outcome")
        assertTrue(produced!!.resumedSteps.isEmpty())
        assertEquals("COMPLETE", produced.result.terminal)
        assertEquals(392, produced.result.records.size)
    }

    @Test
    fun `resume from a partial valid ledger skips already-completed steps without duplicating their ledger records`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("resume-partial")
        // Simulate an interrupted first attempt with PREFLIGHT and WP-A,
        // including every artifact written before WP-B begins.
        createValidPartialCampaign(root, FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR)
        val ledgerPath = root.resolve("progress-ledger.jsonl")

        val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
        val produced = outcome as? FamilyFBoundingEvidenceResumeOutcome.Produced
        assertTrue(produced != null, "expected Produced, got $outcome")
        assertEquals(setOf(FamilyFBoundingEvidenceStep.PREFLIGHT, FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR), produced!!.resumedSteps)
        assertEquals("COMPLETE", produced.result.terminal)

        val finalLedgerLines = Files.readAllLines(ledgerPath, StandardCharsets.UTF_8).filter { it.isNotBlank() }
        val stepCounts = finalLedgerLines.groupingBy { line ->
            val marker = "\"step\":\""
            val start = line.indexOf(marker) + marker.length
            line.substring(start, line.indexOf('"', start))
        }.eachCount()
        assertEquals(1, stepCounts[FamilyFBoundingEvidenceStep.PREFLIGHT.name], "PREFLIGHT must not be duplicated on resume")
        assertEquals(1, stepCounts[FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR.name], "WP_A_ESTIMATOR must not be duplicated on resume")
        assertEquals(1, stepCounts[FamilyFBoundingEvidenceStep.WP_B_RESPONSE_EVIDENCE.name], "the next lawful step must still be recorded exactly once")
        assertEquals(1, stepCounts[FamilyFBoundingEvidenceStep.WP_C_HEADER_EVIDENCE.name])
        assertEquals(1, stepCounts[FamilyFBoundingEvidenceStep.WP_D_RUNTIME_EVIDENCE.name])
        assertEquals(1, stepCounts[FamilyFBoundingEvidenceStep.WP_E_VALIDATION.name])
    }

    @Test
    fun `resume rejects a ledger claiming WP_A_ESTIMATOR complete when the recorded artifact does not match a fresh recomputation`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("resume-inconsistent")
        Files.createDirectories(root)
        val ledger = FamilyFBoundingEvidenceLedger(root.resolve("progress-ledger.jsonl"))
        ledger.recordStep(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "offline evidence production preflight")
        // Corrupt/inconsistent state: WP_A_ESTIMATOR marked complete but its
        // mandatory artifact was never actually written.
        ledger.recordStep(FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR, "OK", "produced 392 records")

        val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
        val rejected = outcome as? FamilyFBoundingEvidenceResumeOutcome.Rejected
        assertTrue(rejected != null, "expected Rejected, got $outcome")
        assertTrue(rejected!!.reason.contains("is missing"))
    }

    @Test
    fun `resume rejects when WP_E_VALIDATION is recorded but no terminal marker exists`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("resume-torn")
        createValidPartialCampaign(root, FamilyFBoundingEvidenceStep.WP_E_VALIDATION)

        val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
        val rejected = outcome as? FamilyFBoundingEvidenceResumeOutcome.Rejected
        assertTrue(rejected != null, "expected Rejected, got $outcome")
        assertTrue(rejected!!.reason.contains("conflicting/torn state"))
    }

    @Test
    fun `resume respects an already-complete terminal state and does not re-run`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("resume-already-complete")
        val first = FamilyFBoundingEvidenceProducer.produce(root)
        assertEquals("COMPLETE", first.terminal)

        val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
        assertTrue(outcome is FamilyFBoundingEvidenceResumeOutcome.AlreadyComplete)
    }

    @Test
    fun `resume respects an already-failed terminal state and requires fresh governance rather than retrying`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("resume-already-failed")
        Files.createDirectories(root)
        FamilyFBoundingEvidenceTerminal.writeFailed(root, "simulated prior failure")

        val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
        val rejected = outcome as? FamilyFBoundingEvidenceResumeOutcome.Rejected
        assertTrue(rejected != null, "expected Rejected, got $outcome")
        assertTrue(rejected!!.reason.contains("fresh governance"))
    }

    @Test
    fun `resume rejects conflicting terminal state where both markers are present`(@TempDir tempDir: Path) {
        val root = tempDir.resolve("resume-conflicting-terminal")
        Files.createDirectories(root)
        Files.writeString(root.resolve("evidence.complete"), "COMPLETE", StandardCharsets.UTF_8)
        Files.writeString(root.resolve("evidence.failed"), "FAILED", StandardCharsets.UTF_8)

        val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
        val rejected = outcome as? FamilyFBoundingEvidenceResumeOutcome.Rejected
        assertTrue(rejected != null, "expected Rejected, got $outcome")
        assertTrue(rejected!!.reason.contains("conflicting terminal state"))
    }

    @Test
    fun `ledger reader returns completed steps without mutating the ledger`(@TempDir tempDir: Path) {
        val ledgerPath = tempDir.resolve("progress-ledger.jsonl")
        assertEquals(emptySet<FamilyFBoundingEvidenceStep>(), FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(ledgerPath))
        val ledger = FamilyFBoundingEvidenceLedger(ledgerPath)
        ledger.recordStep(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "offline evidence production preflight")
        ledger.recordStep(FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR, "OK", "produced 392 records")
        val before = Files.readString(ledgerPath, StandardCharsets.UTF_8)
        val completed = FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(ledgerPath)
        val after = Files.readString(ledgerPath, StandardCharsets.UTF_8)
        assertEquals(setOf(FamilyFBoundingEvidenceStep.PREFLIGHT, FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR), completed)
        assertEquals(before, after, "reading completed steps must not mutate the ledger")
    }

    @Test
    fun `malformed, truncated, missing-field, unknown-step, invalid-metadata, and impossible-order ledger records all fail closed without mutation`(@TempDir tempDir: Path) {
        val cases = listOf(
            "malformed-syntax" to "not-json",
            "truncated" to "{\"step\":\"PREFLIGHT\"",
            "missing-fields" to "{\"step\":\"PREFLIGHT\",\"status\":\"OK\"}",
            "unknown-step" to "{\"step\":\"WP_UNKNOWN\",\"status\":\"OK\",\"detail\":\"x\",\"recordedAt\":\"2026-08-20T00:00:00Z\"}",
            "invalid-json-escape" to "{\"step\":\"PREFLIGHT\",\"status\":\"OK\",\"detail\":\"bad\\qescape\",\"recordedAt\":\"2026-08-20T00:00:00Z\"}",
            "invalid-metadata" to "{\"step\":\"PREFLIGHT\",\"status\":\"OK\",\"detail\":\"x\",\"recordedAt\":\"not-an-instant\"}",
            "impossible-order" to "{\"step\":\"WP_B_RESPONSE_EVIDENCE\",\"status\":\"OK\",\"detail\":\"x\",\"recordedAt\":\"2026-08-20T00:00:00Z\"}",
        )
        cases.forEach { (name, record) ->
            val root = tempDir.resolve(name)
            Files.createDirectories(root)
            val ledgerPath = root.resolve("progress-ledger.jsonl")
            Files.writeString(ledgerPath, "$record\n", StandardCharsets.UTF_8)
            val before = Files.readAllBytes(ledgerPath).toList()
            val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
            assertTrue(outcome is FamilyFBoundingEvidenceResumeOutcome.Rejected, "$name must reject, got $outcome")
            assertEquals(before, Files.readAllBytes(ledgerPath).toList(), "$name must leave ledger unchanged")
            assertEquals(setOf("progress-ledger.jsonl"), Files.list(root).use { it.map { path -> path.fileName.toString() }.toList().toSet() })
        }

        val unterminatedRoot = tempDir.resolve("missing-jsonl-newline")
        Files.createDirectories(unterminatedRoot)
        val unterminatedLedger = unterminatedRoot.resolve("progress-ledger.jsonl")
        Files.writeString(
            unterminatedLedger,
            "{\"step\":\"PREFLIGHT\",\"status\":\"OK\",\"detail\":\"x\",\"recordedAt\":\"2026-08-20T00:00:00Z\"}",
            StandardCharsets.UTF_8,
        )
        assertRejectedWithoutMutation(unterminatedRoot, "unterminated JSONL")
    }

    @Test
    fun `ledger enforces governed status while accepting nonblank informational detail`(@TempDir tempDir: Path) {
        val legalMetadata = listOf(
            Triple(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "offline evidence production preflight"),
            Triple(FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR, "OK", "produced 392 records"),
            Triple(
                FamilyFBoundingEvidenceStep.WP_B_RESPONSE_EVIDENCE,
                "UNRESOLVED_NO_APPLICABLE_PRIMARY_SOURCE",
                "no pre-supplied sources",
            ),
            Triple(FamilyFBoundingEvidenceStep.WP_C_HEADER_EVIDENCE, "UNRESOLVED", "no pre-supplied sources"),
            Triple(
                FamilyFBoundingEvidenceStep.WP_D_RUNTIME_EVIDENCE,
                "UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE",
                "no pre-supplied sources",
            ),
            Triple(
                FamilyFBoundingEvidenceStep.WP_E_VALIDATION,
                "OK",
                "validating artifacts and finalizing ledger",
            ),
        )

        val validLedger = tempDir.resolve("all-legal.jsonl")
        val validWriter = FamilyFBoundingEvidenceLedger(validLedger)
        legalMetadata.forEach { (step, status, detail) -> validWriter.recordStep(step, status, detail) }
        assertEquals(FamilyFBoundingEvidenceStep.entries.toSet(), FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(validLedger))

        legalMetadata.forEachIndexed { index, (step, status, detail) ->
            val wrongStatusPath = tempDir.resolve("wrong-status-$index.jsonl")
            val wrongStatusWriter = FamilyFBoundingEvidenceLedger(wrongStatusPath)
            legalMetadata.take(index).forEach { (priorStep, priorStatus, priorDetail) ->
                wrongStatusWriter.recordStep(priorStep, priorStatus, priorDetail)
            }
            wrongStatusWriter.recordStep(step, "WRONG_$status", detail)
            assertFailsWith<FamilyFBoundingEvidenceLedgerMalformedException> {
                FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(wrongStatusPath)
            }

            val informationalDetailPath = tempDir.resolve("informational-detail-$index.jsonl")
            val informationalDetailWriter = FamilyFBoundingEvidenceLedger(informationalDetailPath)
            legalMetadata.take(index).forEach { (priorStep, priorStatus, priorDetail) ->
                informationalDetailWriter.recordStep(priorStep, priorStatus, priorDetail)
            }
            informationalDetailWriter.recordStep(step, status, "alternate nonblank informational detail")
            assertEquals(
                legalMetadata.take(index + 1).map { it.first }.toSet(),
                FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(informationalDetailPath),
            )
        }

        val crossDetail = tempDir.resolve("cross-detail.jsonl")
        FamilyFBoundingEvidenceLedger(crossDetail).recordStep(
            FamilyFBoundingEvidenceStep.PREFLIGHT,
            "OK",
            "validating artifacts and finalizing ledger",
        )
        assertEquals(
            setOf(FamilyFBoundingEvidenceStep.PREFLIGHT),
            FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(crossDetail),
        )

        val blankDetail = tempDir.resolve("blank-detail.jsonl")
        FamilyFBoundingEvidenceLedger(blankDetail).recordStep(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "")
        assertFailsWith<FamilyFBoundingEvidenceLedgerMalformedException> {
            FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(blankDetail)
        }

        val extraMetadata = tempDir.resolve("extra-metadata.jsonl")
        Files.writeString(
            extraMetadata,
            "{\"step\":\"PREFLIGHT\",\"status\":\"OK\",\"detail\":\"offline evidence production preflight\"," +
                "\"recordedAt\":\"2026-08-20T00:00:00Z\",\"unexpected\":true}\n",
            StandardCharsets.UTF_8,
        )
        assertFailsWith<FamilyFBoundingEvidenceLedgerMalformedException> {
            FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(extraMetadata)
        }
    }

    @Test
    fun `one completion is valid while duplicate completions in any position fail closed and cannot alter resume position`(@TempDir tempDir: Path) {
        val validLedger = tempDir.resolve("valid-ledger.jsonl")
        FamilyFBoundingEvidenceLedger(validLedger).recordStep(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "offline evidence production preflight")
        assertEquals(setOf(FamilyFBoundingEvidenceStep.PREFLIGHT), FamilyFBoundingEvidenceLedgerReader.readCompletedSteps(validLedger))

        val immediateRoot = tempDir.resolve("duplicate-immediate")
        Files.createDirectories(immediateRoot)
        val immediate = FamilyFBoundingEvidenceLedger(immediateRoot.resolve("progress-ledger.jsonl"))
        immediate.recordStep(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "offline evidence production preflight")
        immediate.recordStep(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "offline evidence production preflight")
        assertRejectedWithoutMutation(immediateRoot, "duplicate ledger completion")

        val laterRoot = tempDir.resolve("duplicate-later")
        createValidPartialCampaign(laterRoot, FamilyFBoundingEvidenceStep.WP_A_ESTIMATOR)
        FamilyFBoundingEvidenceLedger(laterRoot.resolve("progress-ledger.jsonl"))
            .recordStep(FamilyFBoundingEvidenceStep.PREFLIGHT, "OK", "offline evidence production preflight")
        assertRejectedWithoutMutation(laterRoot, "duplicate ledger completion")
    }

    @Test
    fun `completed WP-B WP-C and WP-D artifacts are verified and safely skipped when valid`(@TempDir tempDir: Path) {
        listOf(
            FamilyFBoundingEvidenceStep.WP_B_RESPONSE_EVIDENCE,
            FamilyFBoundingEvidenceStep.WP_C_HEADER_EVIDENCE,
            FamilyFBoundingEvidenceStep.WP_D_RUNTIME_EVIDENCE,
        ).forEach { step ->
            val root = tempDir.resolve("valid-${step.name}")
            createValidPartialCampaign(root, step)
            val beforeLedger = Files.readAllLines(root.resolve("progress-ledger.jsonl"), StandardCharsets.UTF_8)
            val outcome = FamilyFBoundingEvidenceProducer.produceOrResume(root)
            val produced = outcome as? FamilyFBoundingEvidenceResumeOutcome.Produced
            assertTrue(produced != null, "valid $step recovery must produce, got $outcome")
            assertTrue(step in produced!!.resumedSteps)
            val afterLedger = Files.readAllLines(root.resolve("progress-ledger.jsonl"), StandardCharsets.UTF_8)
            assertEquals(1, afterLedger.count { it.contains("\"step\":\"${step.name}\"") })
            assertEquals(beforeLedger.size + (FamilyFBoundingEvidenceStep.entries.size - beforeLedger.size), afterLedger.size)
        }
    }

    @Test
    fun `completed WP-B WP-C and WP-D each reject missing changed and malformed final artifacts without rewriting`(@TempDir tempDir: Path) {
        val packages = listOf(
            Triple(FamilyFBoundingEvidenceStep.WP_B_RESPONSE_EVIDENCE, "response-primary-evidence-index.json", "response-primary-evidence"),
            Triple(FamilyFBoundingEvidenceStep.WP_C_HEADER_EVIDENCE, "header-primary-evidence-index.json", "header-primary-evidence"),
            Triple(FamilyFBoundingEvidenceStep.WP_D_RUNTIME_EVIDENCE, "runtime-primary-evidence-index.json", "runtime-primary-evidence"),
        )
        packages.forEach { (step, fileName, directoryName) ->
            val missingRoot = tempDir.resolve("missing-${step.name}")
            createValidPartialCampaign(missingRoot, step)
            Files.delete(missingRoot.resolve(fileName))
            assertRejectedWithoutMutation(missingRoot, "is missing")

            val changedRoot = tempDir.resolve("changed-${step.name}")
            createValidPartialCampaign(changedRoot, step)
            Files.writeString(changedRoot.resolve(fileName), "{\"changed\":true}", StandardCharsets.UTF_8)
            assertRejectedWithoutMutation(changedRoot, "malformed or changed")

            val malformedRoot = tempDir.resolve("malformed-${step.name}")
            createValidPartialCampaign(malformedRoot, step)
            Files.writeString(malformedRoot.resolve(fileName), "not-json", StandardCharsets.UTF_8)
            assertRejectedWithoutMutation(malformedRoot, "malformed or changed")

            val nonEmptyDirectoryRoot = tempDir.resolve("non-empty-${step.name}")
            createValidPartialCampaign(nonEmptyDirectoryRoot, step)
            Files.writeString(nonEmptyDirectoryRoot.resolve(directoryName).resolve("unexpected.capture"), "x", StandardCharsets.UTF_8)
            assertRejectedWithoutMutation(nonEmptyDirectoryRoot, "unknown campaign path")
        }
    }

    @Test
    fun `unknown top-level nested and stale temporary campaign files fail closed while the exact governed surface passes`(@TempDir tempDir: Path) {
        val validRoot = tempDir.resolve("valid-surface")
        FamilyFBoundingEvidenceProducer.produce(validRoot)
        assertNull(FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(validRoot))

        val topLevel = tempDir.resolve("unknown-top-level")
        Files.createDirectories(topLevel)
        Files.writeString(topLevel.resolve("surprise.bin"), "x", StandardCharsets.UTF_8)
        assertRejectedWithoutMutation(topLevel, "unknown campaign path")

        val nested = tempDir.resolve("unknown-nested")
        Files.createDirectories(nested.resolve("response-primary-evidence"))
        Files.writeString(nested.resolve("response-primary-evidence").resolve("surprise.bin"), "x", StandardCharsets.UTF_8)
        assertRejectedWithoutMutation(nested, "unknown campaign path")

        val staleTemporary = tempDir.resolve("stale-temporary")
        Files.createDirectories(staleTemporary)
        Files.writeString(staleTemporary.resolve("request-estimator-summary.json.tmp"), "x", StandardCharsets.UTF_8)
        assertRejectedWithoutMutation(staleTemporary, "unknown campaign path")

        val freshProducerRoot = tempDir.resolve("fresh-producer-non-empty")
        Files.createDirectories(freshProducerRoot)
        Files.writeString(freshProducerRoot.resolve("surprise.bin"), "do-not-overwrite", StandardCharsets.UTF_8)
        assertFailsWith<IllegalStateException> { FamilyFBoundingEvidenceProducer.produce(freshProducerRoot) }
        assertEquals(setOf("surprise.bin"), Files.list(freshProducerRoot).use { it.map { path -> path.fileName.toString() }.toList().toSet() })
    }

    @Test
    fun `campaign and manifest reject every symbolic link including outside nested chained and broken escapes`(@TempDir tempDir: Path) {
        val outsideFile = tempDir.resolve("outside-file.txt")
        val outsideDirectory = tempDir.resolve("outside-directory")
        Files.writeString(outsideFile, "test-owned outside bytes", StandardCharsets.UTF_8)
        Files.createDirectories(outsideDirectory)

        val normalRoot = tempDir.resolve("normal")
        FamilyFBoundingEvidenceProducer.produce(normalRoot)
        assertNull(FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(normalRoot))
        assertTrue(
            FamilyFBoundingEvidenceIntegrity.verifyManifest(
                normalRoot,
                normalRoot.resolve("SHA256SUMS.txt"),
            ),
        )

        val outsideFileRoot = tempDir.resolve("outside-file-root")
        FamilyFBoundingEvidenceProducer.produce(outsideFileRoot)
        val governedFile = outsideFileRoot.resolve("request-estimator-summary.json")
        Files.delete(governedFile)
        Files.createSymbolicLink(governedFile, outsideFile)
        assertTrue(FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(outsideFileRoot)!!.contains("symbolic"))
        assertFalse(
            FamilyFBoundingEvidenceIntegrity.verifyManifest(
                outsideFileRoot,
                outsideFileRoot.resolve("SHA256SUMS.txt"),
            ),
        )
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyFromFreshCopy(outsideFileRoot, tempDir))

        val outsideDirectoryRoot = tempDir.resolve("outside-directory-root")
        Files.createDirectories(outsideDirectoryRoot)
        Files.createSymbolicLink(
            outsideDirectoryRoot.resolve("response-primary-evidence"),
            outsideDirectory,
        )
        assertTrue(FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(outsideDirectoryRoot)!!.contains("symbolic"))

        val nestedRoot = tempDir.resolve("nested-root")
        Files.createDirectories(nestedRoot.resolve("response-primary-evidence"))
        Files.createSymbolicLink(
            nestedRoot.resolve("response-primary-evidence/escape"),
            outsideFile,
        )
        assertTrue(FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(nestedRoot)!!.contains("symbolic"))

        val chainTarget = tempDir.resolve("chain-target")
        Files.createSymbolicLink(chainTarget, outsideFile)
        val chainRoot = tempDir.resolve("chain-root")
        Files.createDirectories(chainRoot)
        Files.createSymbolicLink(chainRoot.resolve("request-estimator-summary.json"), chainTarget)
        assertTrue(FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(chainRoot)!!.contains("symbolic"))

        val brokenRoot = tempDir.resolve("broken-root")
        Files.createDirectories(brokenRoot)
        Files.createSymbolicLink(
            brokenRoot.resolve("request-estimator-summary.json"),
            tempDir.resolve("does-not-exist"),
        )
        assertTrue(FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(brokenRoot)!!.contains("symbolic"))

        val unknownRoot = tempDir.resolve("unknown-link-root")
        Files.createDirectories(unknownRoot)
        Files.createSymbolicLink(unknownRoot.resolve("unknown-link"), outsideFile)
        assertTrue(FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(unknownRoot)!!.contains("symbolic"))

        val rootLink = tempDir.resolve("campaign-root-link")
        Files.createSymbolicLink(rootLink, normalRoot)
        assertTrue(FamilyFBoundingEvidenceIntegrity.validateCampaignSurface(rootLink)!!.contains("symbolic"))
    }

    @Test
    fun `manifest verification confines paths to the exact governed campaign-root member set`(@TempDir tempDir: Path) {
        fun freshManifestRoot(name: String): Pair<Path, Path> {
            val root = tempDir.resolve(name)
            Files.createDirectories(root)
            val files = FamilyFBoundingEvidenceIntegrity.manifestCoveredFiles.sorted().map { relative ->
                root.resolve(relative).also { Files.writeString(it, relative, StandardCharsets.UTF_8) }
            }
            return root to FamilyFBoundingEvidenceIntegrity.writeManifest(root, files)
        }

        val (validRoot, validManifest) = freshManifestRoot("manifest-valid")
        assertTrue(FamilyFBoundingEvidenceIntegrity.verifyManifest(validRoot, validManifest))

        listOf(
            "../outside.txt",
            "../../outside.txt",
            "/tmp/outside.txt",
            "C:\\outside.txt",
            "safe/../../outside.txt",
        ).forEachIndexed { index, escape ->
            val (root, manifest) = freshManifestRoot("manifest-escape-$index")
            val first = Files.readAllLines(manifest, StandardCharsets.UTF_8).first()
            val hash = first.substringBefore("  ")
            Files.writeString(manifest, "$hash  $escape\n", StandardCharsets.UTF_8)
            assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(root, manifest), "manifest escape must reject: $escape")
        }

        val (duplicateRoot, duplicateManifest) = freshManifestRoot("manifest-duplicate")
        val duplicateLines = Files.readAllLines(duplicateManifest, StandardCharsets.UTF_8)
        Files.writeString(duplicateManifest, (duplicateLines + duplicateLines.first()).joinToString("\n", postfix = "\n"), StandardCharsets.UTF_8)
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(duplicateRoot, duplicateManifest))

        // The external file deliberately contains invalid UTF-8. A false result
        // rather than a decoding exception proves rejection occurs before read.
        val externalManifest = tempDir.resolve("external-SHA256SUMS.txt")
        Files.write(externalManifest, byteArrayOf(0xff.toByte(), 0xfe.toByte()))
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(validRoot, externalManifest))
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(validRoot, externalManifest.toAbsolutePath()))

        val siblingManifest = validRoot.parent.resolve("SHA256SUMS.txt")
        Files.write(siblingManifest, byteArrayOf(0xff.toByte()))
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(validRoot, siblingManifest))

        val alternateDirectory = tempDir.resolve("alternate-manifest-directory")
        Files.createDirectories(alternateDirectory)
        val alternateManifest = alternateDirectory.resolve("SHA256SUMS.txt")
        Files.write(alternateManifest, byteArrayOf(0xff.toByte()))
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(validRoot, alternateManifest))

        val traversalEquivalent = validRoot.resolve("nested/../SHA256SUMS.txt")
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(validRoot, traversalEquivalent))

        val (outsideLinkRoot, outsideLinkManifest) = freshManifestRoot("manifest-link-outside")
        Files.delete(outsideLinkManifest)
        Files.createSymbolicLink(outsideLinkManifest, externalManifest)
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(outsideLinkRoot, outsideLinkManifest))

        val (insideLinkRoot, insideLinkManifest) = freshManifestRoot("manifest-link-inside")
        val insideTarget = insideLinkRoot.resolve("inside-manifest-target")
        Files.move(insideLinkManifest, insideTarget)
        Files.createSymbolicLink(insideLinkManifest, insideTarget)
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(insideLinkRoot, insideLinkManifest))

        val (brokenLinkRoot, brokenLinkManifest) = freshManifestRoot("manifest-link-broken")
        Files.delete(brokenLinkManifest)
        Files.createSymbolicLink(brokenLinkManifest, tempDir.resolve("missing-manifest-target"))
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(brokenLinkRoot, brokenLinkManifest))

        val normalizedTraversal = validRoot.resolve("nested").resolve("..").resolve("SHA256SUMS.txt")
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(validRoot, normalizedTraversal))

        val absoluteExternal = tempDir.resolve("absolute-external-manifest").toAbsolutePath()
        Files.write(absoluteExternal, byteArrayOf(0xff.toByte()))
        assertFalse(FamilyFBoundingEvidenceIntegrity.verifyManifest(validRoot, absoluteExternal))
    }

    // ---- WP-B/C/D real validator behaviour ----

    @Test
    fun `WP-B admits a source of an admissible category with complete provenance, and rejects an inadmissible category or missing field`() {
        val admissible = FamilyFPrimaryEvidenceSource(
            sourceId = "src-1", sourceCategory = "official provider documentation", publisher = "Ollama",
            title = "API reference", canonicalLocator = "https://example.invalid/docs", retrievedAt = "2026-01-01T00:00:00Z",
            applicableProviderVersionOrDigest = "v1.2.3", applicableModelDigest = null, contentSha256 = "a".repeat(64),
            localCapturePath = "/tmp/capture", relevantClaim = "response size claim", exactLocationWithinSource = "section 4",
        )
        assertEquals(FamilyFEvidenceAdmissibility.ADMISSIBLE, FamilyFBoundingResponseEvidenceInventory.assessAdmissibility(admissible).admissibilityStatus)

        val wrongCategory = admissible.copy(sourceId = "src-2", sourceCategory = "forum post")
        val wrongCategoryResult = FamilyFBoundingResponseEvidenceInventory.assessAdmissibility(wrongCategory)
        assertEquals(FamilyFEvidenceAdmissibility.REJECTED, wrongCategoryResult.admissibilityStatus)
        assertTrue(wrongCategoryResult.rejectionReason!!.contains("not one of the four"))

        val missingField = admissible.copy(sourceId = "src-3", canonicalLocator = "")
        val missingFieldResult = FamilyFBoundingResponseEvidenceInventory.assessAdmissibility(missingField)
        assertEquals(FamilyFEvidenceAdmissibility.REJECTED, missingFieldResult.admissibilityStatus)
        assertTrue(missingFieldResult.rejectionReason!!.contains("canonicalLocator"))

        assertEquals(FamilyFResponseEvidenceStatus.NOT_ADMISSIBLE, FamilyFBoundingResponseEvidenceInventory.evaluate(listOf(wrongCategory)).first)
        assertEquals(FamilyFResponseEvidenceStatus.UNRESOLVED_INCOMPLETE_SERIALIZATION_BOUND, FamilyFBoundingResponseEvidenceInventory.evaluate(listOf(admissible)).first)
    }

    @Test
    fun `WP-C admits a source only when its limit kind matches its declared bound target, rejecting conflation`() {
        val correctForCount = FamilyFHeaderEvidenceSource(
            "hdr-1", FamilyFHeaderBoundTarget.MAX_HEADER_COUNT, FamilyFHeaderLimitKind.APPLICATION_ITERATION_LIMIT,
            "https://example.invalid/jdk-docs", "b".repeat(64),
        )
        assertTrue(FamilyFBoundingHeaderEvidenceInventory.isAdmissible(correctForCount))

        val conflated = FamilyFHeaderEvidenceSource(
            "hdr-2", FamilyFHeaderBoundTarget.MAX_AGGREGATE_HEADER_BYTES, FamilyFHeaderLimitKind.PARSER_WIRE_LIMIT,
            "https://example.invalid/parser-docs", "c".repeat(64),
        )
        assertFalse(FamilyFBoundingHeaderEvidenceInventory.isAdmissible(conflated))

        assertEquals("NOT_ADMISSIBLE", FamilyFBoundingHeaderEvidenceInventory.evaluate(listOf(conflated)).first)
        assertEquals("UNRESOLVED", FamilyFBoundingHeaderEvidenceInventory.evaluate(listOf(correctForCount)).first)
        assertEquals("UNRESOLVED", FamilyFBoundingHeaderEvidenceInventory.evaluate(emptyList()).first)
    }

    @Test
    fun `WP-D admits only official provider documentation not based on an actual observation, rejecting benchmark-derived claims`() {
        val documentationOnly = FamilyFPrimaryEvidenceSource(
            sourceId = "rt-1", sourceCategory = "official provider documentation", publisher = "Ollama",
            title = "Runtime writable paths", canonicalLocator = "https://example.invalid/runtime-docs",
            retrievedAt = "2026-01-01T00:00:00Z", applicableProviderVersionOrDigest = "v1.2.3", applicableModelDigest = null,
            contentSha256 = "d".repeat(64), localCapturePath = "/tmp/capture2", relevantClaim = "writable path claim",
            exactLocationWithinSource = "section 2",
        )
        assertTrue(FamilyFBoundingRuntimeEvidenceInventory.isAdmissible(documentationOnly, basedOnObservation = false))
        assertFalse(FamilyFBoundingRuntimeEvidenceInventory.isAdmissible(documentationOnly, basedOnObservation = true))
        assertFalse(FamilyFBoundingRuntimeEvidenceInventory.isAdmissible(documentationOnly.copy(sourceCategory = "internal benchmark report"), basedOnObservation = false))

        assertEquals(
            FamilyFRuntimeEvidenceStatus.NOT_ADMISSIBLE,
            FamilyFBoundingRuntimeEvidenceInventory.evaluate(listOf(documentationOnly), mapOf("rt-1" to true)).first,
        )
        assertEquals(
            FamilyFRuntimeEvidenceStatus.UNRESOLVED_PROVIDER_DOCUMENTATION_INCOMPLETE,
            FamilyFBoundingRuntimeEvidenceInventory.evaluate(listOf(documentationOnly), mapOf("rt-1" to false)).first,
        )
    }

    // ---- Source-inspection exclusion-region precision ----
    //
    // These synthetic marker/symbol strings are deliberately assembled from
    // fragments (never a contiguous literal) so that this file's own raw
    // source text never contains a real marker or a real forbidden symbol
    // outside the one legitimate declared exclusion region -- avoiding
    // exactly the self-interference these tests exist to guard against.

    private val syntheticExcludeStartMarker = listOf("// FAM", "ILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SCAN_EX", "CLUDE_START").joinToString("")
    private val syntheticExcludeEndMarker = listOf("// FAM", "ILY_F_BOUNDING_EVIDENCE_FORBIDDEN_SCAN_EX", "CLUDE_END").joinToString("")
    private val syntheticForbiddenSymbol = listOf("Synthetic", "Forbidden", "MarkerXYZ").joinToString("")

    @Test
    fun `forbidden-symbol declaration does not detect itself`() {
        val synthetic = "$syntheticExcludeStartMarker\n" +
            "val X = listOf(\"$syntheticForbiddenSymbol\")\n" +
            "$syntheticExcludeEndMarker\n"
        assertTrue(familyFBoundingEvidenceForbiddenSymbolsFound(synthetic, listOf(syntheticForbiddenSymbol)).isEmpty())
    }

    @Test
    fun `a forbidden symbol immediately before the legitimate exclusion region is detected`() {
        val synthetic = "val leaked = \"$syntheticForbiddenSymbol\"\n" +
            "$syntheticExcludeStartMarker\n" +
            "val X = listOf(\"$syntheticForbiddenSymbol\")\n" +
            "$syntheticExcludeEndMarker\n"
        assertEquals(listOf(syntheticForbiddenSymbol), familyFBoundingEvidenceForbiddenSymbolsFound(synthetic, listOf(syntheticForbiddenSymbol)))
    }

    @Test
    fun `a forbidden symbol immediately after the legitimate exclusion region is detected`() {
        val synthetic = "$syntheticExcludeStartMarker\n" +
            "val X = listOf(\"$syntheticForbiddenSymbol\")\n" +
            "$syntheticExcludeEndMarker\n" +
            "val leaked = \"$syntheticForbiddenSymbol\"\n"
        assertEquals(listOf(syntheticForbiddenSymbol), familyFBoundingEvidenceForbiddenSymbolsFound(synthetic, listOf(syntheticForbiddenSymbol)))
    }

    @Test
    fun `ordinary allowed source containing none of the forbidden symbols is accepted`() {
        val synthetic = "val ok = defaultOllamaRequestBody(prompt, modelName)\nval path: java.nio.file.Path = tempDir\n"
        assertTrue(familyFBoundingEvidenceForbiddenSymbolsFound(synthetic, listOf(syntheticForbiddenSymbol)).isEmpty())
    }

    @Test
    fun `exclusion region wraps only the forbidden-symbol declaration, not the import block or substantive implementation code`() {
        val source = familyFBoundingEvidenceScanSafeSource(Path.of("tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt"))
        val rawSource = Files.readString(Path.of("tests/integration/ReasoningProtocolFamilyFBoundingEvidenceTest.kt"))
        // The scanned (exclusion-stripped) text must still contain ordinary
        // import statements and estimator implementation code -- proving the
        // exclusion region did not swallow anything beyond the declaration
        // it exists to cover.
        assertTrue(source.contains("import parker.core.runtime.defaultOllamaRequestBody"))
        assertTrue(source.contains("object FamilyFBoundingEvidenceRequestEstimator"))
        assertTrue(source.contains("fun estimate(): List<FamilyFBoundingRequestRecord>"))
        assertTrue(rawSource.length - source.length < 1500, "excluded region must be small (only the forbidden-symbol list), not the whole file")
    }

}
