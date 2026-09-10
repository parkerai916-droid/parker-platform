package parker.core.interfaces

/**
 * Hermes pre-ingestion processing result -- the missing "did Hermes's own processing of this raw
 * source meet the bar for Parker governed ingestion" fact identified by the 2026-09-10 Parker +
 * Hermes baseline audit. R0: type, semantics, and invariants only -- no HTTP endpoint, no Agent
 * Gateway wiring, no PermissionEngine call, and no governed-ingestion side effect of any kind.
 * Wiring this contract into the Hermes -> Parker submission path is a separate, later unit.
 *
 * ## Lifecycle boundary: distinct from [parker.core.runtime.SteveReviewQueueStatus]
 *
 * [HermesProcessingStatus] deliberately reuses the exact PASS / REVIEW_REQUIRED / FAILED
 * vocabulary `SteveReviewQueueStatus` already uses -- that vocabulary is the correct one -- but
 * the two types describe two different, non-overlapping moments in the evidence lifecycle:
 *
 * - [HermesProcessingStatus] = pre-ingestion processing quality. It describes whether Hermes's
 *   own processing of a raw source, before that source necessarily has any canonical custodied
 *   bytes or [EvidenceSourceManifest] under an [EvidenceArtifactId] at all, met the bar for
 *   governed ingestion to proceed.
 * - `SteveReviewQueueStatus` (`src/runtime/SteveReviewQueueProjection.kt`) = post-ingestion Parker
 *   fidelity/review projection. It is a read-only projection of OCR/Human-Fidelity-Review outcomes
 *   for evidence *already* registered with the Evidence Custodian -- a later, unrelated pipeline
 *   stage.
 *
 * Nothing in this file mutates, depends upon, or is derived from `SteveReviewQueueStatus`,
 * [DerivativeReviewState], or [HumanFidelityReviewState], and none of those may be derived from
 * this file's types either. Constructing or holding a [HermesProcessingResult] confers no
 * governed-evidence fact of any kind -- it exists entirely on the Hermes side of the
 * "RAW SOURCE -> HERMES PROCESSING -> HERMES PROCESSING RESULT" boundary, never inside
 * "PARKER GOVERNED INGESTION".
 *
 * ## Case / batch relationship
 *
 * [HermesProcessingResult.batchId] is Parker's existing, server-minted, opaque ingestion batch
 * identity -- the same plain `String` shape `BulkIngestionBindingCoordinator` already mints and
 * Hermes already receives from `GET /agent/ingestion-batches`. This file introduces no new case
 * or batch model, and no Hermes-side [CaseId]: Hermes never asserts case membership, and the
 * batch -> case relationship remains exclusively Parker's own.
 *
 * ## Source identity
 *
 * [HermesProcessingResult.sourceSha256] is the authoritative correlation key -- the same bare,
 * regex-validated SHA-256 hex string shape [EvidenceSourceManifest.sha256] and
 * `EvidenceSourceIdentityIndex.createOrGet` already use for a source before governed registration
 * necessarily completes, rather than [OcrSha256Digest] (a value class scoped to the later
 * OCR/Human-Fidelity-Review pipeline this file's types are explicitly *not* part of).
 * [HermesProcessingResult.proposedEvidenceArtifactId] mirrors `createOrGet`'s own
 * `proposedEvidenceArtifactId` naming exactly: an optional, non-authoritative identity Hermes may
 * already hold (for example, because it already submitted bytes and received one back), never a
 * guarantee of final identity -- Parker's own hash-based dedup, unchanged by this file, remains
 * the sole authority for which [EvidenceArtifactId] a given hash ultimately resolves to.
 *
 * ## Why no [SourceRegionId] or [PageRepresentationId] field exists here
 *
 * Both are Parker-canonical, pixel-render-derived digests computed only by Parker's own
 * downstream page-rendering/region-derivation pipeline, after governed ingestion. Hermes cannot
 * truthfully compute either before that pipeline ever runs, so no field here asserts one --
 * exactly the same "never fabricate a fact a type's producer cannot truthfully establish"
 * discipline already governing, for example, [OcrModelSnapshot] and
 * [OcrProviderProvenance.providerReportedModelIdentifier]. [HermesProcessingIssueLocation]
 * carries only a page number, an optional Hermes-extracted-text-relative offset range (the same
 * shape and disclaimer [OcrUncertaintySpan] already uses), and an optional bounded free-text
 * region hint -- never a second page/region coordinate system.
 */
enum class HermesProcessingStatus {
    /**
     * Hermes processed the source with sufficient fidelity, traceability, and processing
     * certainty to proceed to Parker governed ingestion. Strictly a processing-quality decision:
     * it asserts nothing about legal relevance, admissibility, factual truth, substantive
     * reliability, or evidential persuasiveness.
     */
    PASS,

    /** Hermes completed enough processing to describe the problem, but one or more uncertainties require Steve's review before governed ingestion proceeds. */
    REVIEW_REQUIRED,

    /** Hermes could not produce a processing result suitable for governed ingestion or meaningful human acceptance without reprocessing or intervention. */
    FAILED,
}

/**
 * How Hermes produced a result. Multiple methods may apply to one source (for example, direct
 * text extraction for most pages plus OCR for a scanned page within the same document). Only
 * values justified by the current Hermes/Parker programme -- not an open or speculative taxonomy.
 */
enum class HermesProcessingMethod {
    DIRECT_TEXT_EXTRACTION,
    OCR,
    VISION,
    TRANSCRIPTION,
    STRUCTURED_DOCUMENT_EXTRACTION,
}

/**
 * Where a [HermesProcessingIssue] applies, if known. A sealed type -- not a flat nullable
 * `pageNumber` on [HermesProcessingIssue] itself -- so a future unit adding time-range
 * provenance for audio/video sources can add a sibling variant here without altering
 * [DocumentPage] or any of its existing callers.
 */
sealed interface HermesProcessingIssueLocation {

    /**
     * A document page, optionally narrowed to a character range within whatever text Hermes
     * itself extracted for that page. [startOffsetInclusive]/[endOffsetExclusive] address
     * Hermes's own extracted text only -- the same disclaimer [OcrUncertaintySpan] already
     * carries -- and assert no pixel/spatial location. [regionDescription] is an optional,
     * bounded, human-readable hint (for example, "table", "header column 2") -- deliberately not
     * a [SourceRegionId], which Hermes cannot truthfully compute at this stage (see this file's
     * top-level KDoc).
     */
    data class DocumentPage(
        val pageNumber: Int,
        val startOffsetInclusive: Int? = null,
        val endOffsetExclusive: Int? = null,
        val regionDescription: String? = null,
    ) : HermesProcessingIssueLocation {
        init {
            require(pageNumber >= 1) {
                "HermesProcessingIssueLocation.DocumentPage.pageNumber must be one-based and positive"
            }
            require((startOffsetInclusive == null) == (endOffsetExclusive == null)) {
                "HermesProcessingIssueLocation.DocumentPage offsets must be both present or both absent"
            }
            if (startOffsetInclusive != null && endOffsetExclusive != null) {
                require(startOffsetInclusive >= 0 && endOffsetExclusive > startOffsetInclusive) {
                    "HermesProcessingIssueLocation.DocumentPage offsets must form a non-empty half-open range"
                }
            }
            require(regionDescription == null || (regionDescription.isNotBlank() && regionDescription.length <= MAX_HERMES_TEXT_CHARACTERS)) {
                "HermesProcessingIssueLocation.DocumentPage.regionDescription must be absent or contain 1..$MAX_HERMES_TEXT_CHARACTERS characters"
            }
        }
    }
}

/**
 * A machine-readable reason a source needs review or could not be processed. Only values
 * justified by the current Hermes/Parker programme's own worked examples -- not an enormous or
 * speculative taxonomy.
 */
enum class HermesProcessingIssueKind {
    OCR_UNCERTAINTY,
    UNREADABLE_REGION,
    TABLE_STRUCTURE_AMBIGUITY,
    PAGE_SEGMENTATION_UNCERTAINTY,
    MISSING_CONTENT,
    CONFLICTING_EXTRACTION_OUTPUTS,
    UNSUPPORTED_CONTENT,
    LOW_FIDELITY_TRANSCRIPTION,
    PROCESSING_EXCEPTION,
}

/**
 * One reason a source needs review, usable directly by a future Steve review screen: [kind] is
 * machine-readable, [explanation] is the human-readable statement of the problem (and, where
 * applicable, why review is required), [location] is the affected part of the source if known,
 * and [hermesInterpretation] is Hermes's own uncertain reading of the affected content, if it
 * produced one (for example, "Gross earnings appears to be $42,871."). [transcriptionFidelity]
 * reuses Parker's own existing fidelity vocabulary ([TranscriptionFidelity]) and may be supplied
 * only alongside [HermesProcessingIssueKind.LOW_FIDELITY_TRANSCRIPTION] -- it is not forced onto
 * issue kinds it does not actually describe.
 */
data class HermesProcessingIssue(
    val kind: HermesProcessingIssueKind,
    val explanation: String,
    val location: HermesProcessingIssueLocation? = null,
    val hermesInterpretation: String? = null,
    val transcriptionFidelity: TranscriptionFidelity? = null,
) {
    init {
        require(explanation.isNotBlank() && explanation.length <= MAX_HERMES_TEXT_CHARACTERS) {
            "HermesProcessingIssue.explanation must contain 1..$MAX_HERMES_TEXT_CHARACTERS characters"
        }
        require(hermesInterpretation == null || (hermesInterpretation.isNotBlank() && hermesInterpretation.length <= MAX_HERMES_TEXT_CHARACTERS)) {
            "HermesProcessingIssue.hermesInterpretation must be absent or contain 1..$MAX_HERMES_TEXT_CHARACTERS characters"
        }
        require(transcriptionFidelity == null || kind == HermesProcessingIssueKind.LOW_FIDELITY_TRANSCRIPTION) {
            "HermesProcessingIssue.transcriptionFidelity may be supplied only for a LOW_FIDELITY_TRANSCRIPTION issue"
        }
    }
}

/**
 * A machine-readable reason Hermes could not produce a processing result suitable for governed
 * ingestion or meaningful human acceptance. Only values justified by the current Hermes/Parker
 * programme's own worked examples. Does not model operational retry policy.
 */
enum class HermesProcessingFailureKind {
    UNSUPPORTED_FILE_FORMAT,
    ENCRYPTED_SOURCE,
    NO_READABLE_CONTENT,
    PROCESSOR_FAILURE,
    CORRUPT_SOURCE,
    PROCESSING_TIMEOUT,
    REQUIRED_PROCESSOR_UNAVAILABLE,
}

/** [detail] is an optional, bounded, free-text elaboration -- never a substitute for [kind]. */
data class HermesProcessingFailure(
    val kind: HermesProcessingFailureKind,
    val detail: String? = null,
) {
    init {
        require(detail == null || (detail.isNotBlank() && detail.length <= MAX_HERMES_TEXT_CHARACTERS)) {
            "HermesProcessingFailure.detail must be absent or contain 1..$MAX_HERMES_TEXT_CHARACTERS characters"
        }
    }
}

/**
 * Hermes's result for one raw source, before that source necessarily exists as governed Parker
 * evidence. See this file's own top-level KDoc for the full lifecycle-boundary rationale and for
 * why [sourceSha256]/[proposedEvidenceArtifactId] and [HermesProcessingIssueLocation] are shaped
 * the way they are. Follows the same flat-data-class-with-per-status-invariant shape
 * [GroundedProposition] already established in this repository, rather than a sealed hierarchy of
 * per-status subtypes, since the fields shared across all three statuses (source identity, batch,
 * methods) dominate over the small, status-specific differences.
 */
data class HermesProcessingResult(
    val sourceSha256: String,
    val batchId: String,
    val status: HermesProcessingStatus,
    val methods: Set<HermesProcessingMethod>,
    val proposedEvidenceArtifactId: EvidenceArtifactId? = null,
    val issues: List<HermesProcessingIssue> = emptyList(),
    val failure: HermesProcessingFailure? = null,
) {
    init {
        require(sourceSha256.matches(HERMES_SHA256_PATTERN)) {
            "HermesProcessingResult.sourceSha256 must be 64 lowercase hexadecimal characters"
        }
        require(batchId.isNotBlank()) { "HermesProcessingResult.batchId must not be blank" }
        require(methods.isNotEmpty()) { "HermesProcessingResult.methods must name at least one processing method" }
        require(issues.size <= MAX_HERMES_ISSUES) { "HermesProcessingResult.issues must contain at most $MAX_HERMES_ISSUES entries" }
        when (status) {
            HermesProcessingStatus.PASS -> {
                require(failure == null) { "PASS must not carry a failure reason" }
            }
            HermesProcessingStatus.REVIEW_REQUIRED -> {
                require(failure == null) { "REVIEW_REQUIRED must not carry a fatal failure reason -- use FAILED instead" }
                require(issues.isNotEmpty()) { "REVIEW_REQUIRED requires at least one processing issue" }
            }
            HermesProcessingStatus.FAILED -> {
                require(failure != null) { "FAILED requires a failure reason" }
            }
        }
    }
}

private const val MAX_HERMES_TEXT_CHARACTERS = 4_096
private const val MAX_HERMES_ISSUES = 1_000
private val HERMES_SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
