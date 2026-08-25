package parker.core.interfaces

import java.time.Instant

/**
 * OCR Mechanism, Implementation Unit 1 ("Provider-Neutral OCR Capability
 * Contract"). Governed in full by
 * `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract
 * Design") Sections 1-8, 12-13; by
 * `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope Lock")
 * Sections 3, 5, 6, 10, 13; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 1. Mirrors [ReasoningProvider]'s and
 * [EvidenceExtractor]'s own "narrow, dependency-free interface plus its
 * own sealed response type, one file" shape exactly (Implementation Plan
 * Unit 1; Contract Design Section 3).
 *
 * ## What this Unit implements
 *
 * [OcrMechanism] -- one operation, given already-retrieved image content
 * and the fixed set of already-established context facts a caller
 * supplies alongside it, return an [OcrRecognitionOutcome]. Declares no
 * constructor, no property, and no dependency of any kind -- mirroring
 * [ReasoningProvider]'s own "pure callee, calls nothing" shape exactly
 * (Contract Design Section 3, Section 12; Scope Lock Section 3, Section
 * 13). No concrete OCR engine, library, or service -- OCRmyPDF,
 * Tesseract, EasyOCR, PaddleOCR, or any other -- is named, selected, or
 * implied anywhere in this file (Scope Lock Section 14).
 *
 * ## No implementation, no adapter, no orchestration in this Unit
 *
 * This file defines the capability's own contract only. It contains no
 * concrete implementation of [OcrMechanism], no provider adapter
 * abstraction (Implementation Plan Unit 2), no execution sequencing
 * (Unit 3), and no runtime composition of any kind (Unit 12, structurally
 * blocked). It introduces no dependency on `EvidenceCustodian`,
 * `MemoryCore`, Knowledge Memory's submission interface, the Permission
 * Engine, `EvidenceIntelligence`'s own result handling, any runtime
 * conversation component, any reporting mechanism, or Docling -- at any
 * depth (Scope Lock Section 13).
 *
 * ## Why `recognise` is `suspend`
 *
 * For consistency with every other Parker interface a coordinator calls
 * ([EvidenceExtractor.extract], [ReasoningProvider.reason]), even though
 * this Unit performs no genuinely asynchronous work itself -- a shape
 * decision, not a behavioural one (Implementation Plan Unit 1).
 *
 * ## `EvidenceArtifactId` is referenced, not depended upon
 *
 * [OcrRecognitionRequest.sourceEvidenceId] reuses Evidence Custodian's
 * own, existing, unmodified [EvidenceArtifactId] identifier type exactly
 * as the Contract Design's own Repository Reuse section already
 * authorises ("Reused, unmodified: `EvidenceArtifactId`... the OCR
 * mechanism never constructs these directly"). Referencing an existing
 * identifier *value* is not a dependency on the `EvidenceCustodian`
 * *interface* -- the same distinction [ReasoningProvider]'s own
 * `ReasoningSubject.OfEvidenceAnalysisRequest` case already relies on
 * for referencing `EvidenceAnalysisRequest` without thereby depending on
 * Evidence Intelligence. This file holds no reference of any kind to the
 * [EvidenceCustodian] interface itself.
 */
interface OcrMechanism {
    suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome
}

/**
 * Every input the [OcrMechanism] may consume, exactly the categories
 * Scope Lock Section 5 permits -- no requesting principal (audit context
 * remains at Evidence Intelligence's own tier, Scope Lock Section 5), no
 * caller-declared confidence or evidential-state value (Contract Design
 * Section 4), and no provenance context as a distinct field (provenance
 * is an output obligation, Section 7 there, never an input here).
 *
 * **"Processing context" is not a separate field.** Scope Lock Section 5
 * describes "processing context" as "the fixed set of facts Evidence
 * Intelligence supplies alongside the content itself" -- exactly the
 * fields already present below ([sourceEvidenceId], [mediaType],
 * [pageCount]), taken together. No further, generic context field is
 * introduced; doing so before a concrete need is identified would be
 * exactly the kind of speculative scaffolding the Implementation Plan's
 * own "no scaffolding for future units" discipline forbids.
 *
 * Not a `data class`: a Kotlin `data class`'s auto-generated `equals`/
 * `hashCode` compare a `ByteArray` property by reference, not by
 * content, which would silently misrepresent equality for this type
 * (the same reasoning [CandidateEvidenceArtifact]'s own KDoc already
 * documents). [equals], [hashCode], and [toString] are overridden here
 * to compare [content] structurally ([ByteArray.contentEquals]/
 * [ByteArray.contentHashCode]) instead.
 *
 * @param sourceEvidenceId The original evidence artefact's own,
 *   existing, unmodified identifier -- reused, never redefined (Contract
 *   Design Section 13). Sufficient to resolve the eventual recognition
 *   back to the original it was produced from (Scope Lock Section 9).
 * @param content Already-retrieved image content -- never fetched by
 *   this mechanism itself; obtained by the caller through its own,
 *   already-existing `EvidenceCustodian.retrieve` dependency before this
 *   operation is ever invoked (Scope Lock Section 5, Section 13).
 *   Immutable in effect: no operation defined anywhere in this file ever
 *   writes to it.
 * @param mediaType The detected media type, passed through unchanged
 *   from whatever upstream detection produced it -- never re-derived or
 *   second-guessed by this mechanism (Contract Design Section 4).
 * @param pageCount The source's own page count, where known -- `null`
 *   only when genuinely unknown, never a placeholder for zero (mirroring
 *   [ExtractionOutcome.RequiresOcr]'s own identical convention).
 */
class OcrRecognitionRequest(
    val sourceEvidenceId: EvidenceArtifactId,
    val content: ByteArray,
    val mediaType: String,
    val pageCount: Int? = null,
) {
    init {
        require(mediaType.isNotBlank()) { "OcrRecognitionRequest.mediaType must not be blank" }
    }

    override fun equals(other: Any?): Boolean =
        other is OcrRecognitionRequest &&
            sourceEvidenceId == other.sourceEvidenceId &&
            content.contentEquals(other.content) &&
            mediaType == other.mediaType &&
            pageCount == other.pageCount

    override fun hashCode(): Int {
        var result = sourceEvidenceId.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + mediaType.hashCode()
        result = 31 * result + (pageCount ?: 0)
        return result
    }

    override fun toString(): String =
        "OcrRecognitionRequest(sourceEvidenceId=$sourceEvidenceId, content=<${content.size} bytes>, " +
            "mediaType=$mediaType, pageCount=$pageCount)"
}

/**
 * The recognised text's fidelity to the original image content -- exactly
 * the three categories `EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` Section
 * 5 already freezes for any transcription-produced candidate, realised
 * here in Kotlin for the first time because no earlier unit had reason
 * to. Owned, in substance, by the Evidence Intelligence Contract Design;
 * this file only gives its already-frozen prose a closed Kotlin shape,
 * introducing no fourth category (Implementation Plan Unit 1).
 */
enum class TranscriptionFidelity {
    /** Reproduces the original's exact characters, spelling, and layout as read. */
    VERBATIM,

    /** Corrects or standardises what was read, at the cost of exact reproduction. */
    NORMALISED,

    /** Fills a gap the original does not clearly support with the mechanism's own best judgement. */
    INFERRED_RECONSTRUCTION,
}

/**
 * What configuration produced a given recognition -- one structured
 * record, not independently-worded prose, mirroring
 * [ExtractionIdentity]'s own "one structured record, not three
 * independently-worded sentences" discipline (Evidence Processing
 * Boundary Clarification Section 4; Scope Lock Section 9). Never names a
 * concrete engine (Scope Lock Section 14).
 *
 * @param mechanismIdentity The specific recognition mechanism that
 *   handled this content -- a provider-neutral label, never a concrete
 *   engine name.
 * @param configurationProfile A named tag identifying the specific
 *   configuration in force for this recognition -- a different
 *   configuration could produce different output from the same
 *   mechanism.
 * @param mechanismVersion The mechanism's own version, where available;
 *   `null` only when genuinely unknown (Scope Lock Section 9: "where
 *   available").
 * @param modelIdentity Names the specific underlying model artifact this
 *   recognition's text actually ran through -- distinct from
 *   [mechanismIdentity] (the framework, e.g. `"docling"`) and
 *   [configurationProfile] (the profile string). `null` unless a
 *   provider's own bridge has cryptographically verified, against a
 *   trusted manifest, that the bytes hashed for [modelVersion] are the
 *   exact bytes the model actually ran on for this invocation (Tier B OCR
 *   Truthful Mandatory Provenance Implementation Plan, Section 5/13:
 *   "verified" branch only -- never populated by the "fallback" branch).
 *   Never a filesystem path, never a semantic-version guess, never the
 *   literal string `"unknown"`.
 * @param modelVersion A content-addressed revision identifier of the
 *   exact bytes [modelIdentity] names -- always exactly `sha256:` followed
 *   by 64 lowercase hexadecimal digits when present, never a semantic
 *   version, never a manifest version, never a filename. Present if, and
 *   only if, [modelIdentity] is also present.
 */
data class OcrRecognitionIdentity(
    val mechanismIdentity: String,
    val configurationProfile: String,
    val mechanismVersion: String? = null,
    val modelIdentity: String? = null,
    val modelVersion: String? = null,
) {
    init {
        require(mechanismIdentity.isNotBlank()) { "OcrRecognitionIdentity.mechanismIdentity must not be blank" }
        require(configurationProfile.isNotBlank()) { "OcrRecognitionIdentity.configurationProfile must not be blank" }
        require((modelIdentity == null) == (modelVersion == null)) {
            "OcrRecognitionIdentity.modelIdentity and modelVersion must be both present or both absent -- never one alone"
        }
        require(modelIdentity == null || modelIdentity.isNotBlank()) {
            "OcrRecognitionIdentity.modelIdentity must not be blank when present"
        }
        require(modelIdentity == null || !modelIdentity.equals("unknown", ignoreCase = true)) {
            "OcrRecognitionIdentity.modelIdentity must never be the literal 'unknown' -- absent (null) is the honest representation of unverified provenance"
        }
        require(modelVersion == null || MODEL_VERSION_PATTERN.matches(modelVersion)) {
            "OcrRecognitionIdentity.modelVersion must exactly match sha256:<64 lowercase hex digits> when present"
        }
    }

    private companion object {
        val MODEL_VERSION_PATTERN = Regex("^sha256:[0-9a-f]{64}$")
    }
}

/**
 * One portion of a recognition's own [OcrRecognitionResult.recognisedText]
 * -- the finer granularity Implementation Plan Unit 6 adds to Unit 1's
 * shape, authorised exactly, and only, by two already-frozen elaborations
 * of existing Contract Design Section 5 categories, never a new output
 * category: "recognised text, optionally organised at page-aligned
 * granularity... an elaboration of 'recognised text,' not a new category"
 * (Scope Lock Section 6); and "or, where more than one [fidelity]
 * applies within a single recognition, which portions are which" (Contract
 * Design Section 5), itself restating Evidence Intelligence Contract
 * Design Section 5's own "the output must make which of the three it is --
 * or which portions are which, where more than one applies within the
 * same artefact -- apparent."
 *
 * **Document-level versus portion-level text, without duplication
 * ambiguity.** [OcrRecognitionResult.recognisedText] always remains the
 * complete, document-level text on its own, exactly as Unit 1 fixed it --
 * present and non-blank regardless of whether any segment exists.
 * [OcrRecognitionResult.segments], when non-empty, is this Unit's own
 * additional, finer-grained view of that same recognition, for a reader
 * that needs page or per-portion fidelity detail; it is never a second,
 * independent source of text a reader must reconcile against
 * [OcrRecognitionResult.recognisedText] to avoid double-counting. Byte-
 * for-byte concatenation consistency between the two is a construction-
 * time concern for whatever produces a real [OcrRecognitionResult] --
 * this Unit fixes only the shape, not that judgement.
 *
 * **What this type is not.** No bounding box, coordinate, heading, table,
 * or other document-structure concept is represented here, and none may
 * be added under this Unit -- that layout-semantic territory belongs
 * exclusively to a future, separately governed structured-document
 * capability, never to the OCR mechanism (Implementation Plan Section 14,
 * "do not introduce layout semantics belonging to Docling or a future
 * structured-document model").
 *
 * @param text This portion's own recognised text. Never blank.
 * @param fidelity This portion's own fidelity -- may differ from a
 *   neighbouring segment's, and from [OcrRecognitionResult.fidelity]'s own
 *   document-level value, exactly the "portions... where more than one
 *   applies within a single recognition" case this Unit exists to make
 *   discoverable.
 * @param pageNumber The one-based page this portion belongs to, where
 *   known. `null` when this portion is not tied to a specific page (for
 *   example, fidelity-level segmentation without page-level detail from a
 *   future concrete provider). Never zero or negative.
 */
data class OcrRecognitionSegment(
    val text: String,
    val fidelity: TranscriptionFidelity,
    val pageNumber: Int? = null,
) {
    init {
        require(text.isNotBlank()) { "OcrRecognitionSegment.text must not be blank" }
        pageNumber?.let {
            require(it >= 1) { "OcrRecognitionSegment.pageNumber must be at least 1 when present -- page numbering is one-based" }
        }
    }
}

/**
 * A successful recognition's own disclosure -- exactly the four
 * categories Contract Design Section 5 fixes (recognised text, fidelity
 * disclosure, identity disclosure, working confidence signal), never a
 * fifth. **Not itself a governed record.** This type is never a
 * `CandidateEvidenceArtifact`, an `EvidenceAnalysisResult`, or any other
 * accepted artefact (Contract Design Section 5; Scope Lock Section 6,
 * Section 8) -- it is the raw material a future Evidence-Intelligence-side
 * caller uses to decide whether, and how, to produce one.
 *
 * @param recognisedText The text a human reader would recognise in the
 *   supplied image content -- the complete, document-level text, present
 *   and non-blank regardless of whether [segments] is populated.
 * @param fidelity Which of [TranscriptionFidelity]'s three categories this
 *   recognition represents as a whole. Remains a single, required value
 *   exactly as Unit 1 fixed it -- when [segments] carries differing
 *   per-portion fidelity values, each segment's own [OcrRecognitionSegment.fidelity]
 *   is authoritative for its own portion, and this field is understood to
 *   describe the recognition's own dominant or primary fidelity, a
 *   construction-time judgement this shape does not itself compute.
 * @param identity The structured, reproducibility record for this
 *   recognition.
 * @param confidence A working, transient confidence signal, where
 *   genuinely available -- never durable, never written to
 *   `CandidateAssertion.confidence` or any other durable field (Contract
 *   Design Section 9; Scope Lock Section 6). Must fall within the closed
 *   unit interval when present; `null` when genuinely unavailable, never
 *   fabricated.
 * @param recognisedAt When this recognition was performed.
 * @param warnings Any non-fatal condition observed during recognition.
 *   An empty list means genuinely no warnings, mirroring
 *   [ExtractionResult.warnings]'s own "explicit, never silently omitted"
 *   convention. Order is preserved, never sorted or deduplicated.
 * @param segments This recognition's own optional, finer-grained
 *   breakdown -- page-aligned, per-portion-fidelity, or both, exactly the
 *   granularity the request's own page scope and the recognition's own
 *   fidelity mix support. Empty when no finer granularity is available or
 *   needed; [recognisedText] and [fidelity] alone remain fully meaningful
 *   in that case, exactly as they were before this Unit (Implementation
 *   Plan Unit 6: "optionally page-aligned"; Scope Lock Section 6: "an
 *   elaboration... not a new category"). Where any segment carries a
 *   [OcrRecognitionSegment.pageNumber], page numbers must appear in
 *   non-decreasing order across the list -- multiple segments may share
 *   one page (for example, two differently-fidelity portions of the same
 *   page), but no later segment may name an earlier page than one already
 *   seen, preserving page order and preventing invalid page numbering
 *   (Implementation Plan Unit 6's own page-alignment requirements).
 */
data class OcrRecognitionResult(
    val recognisedText: String,
    val fidelity: TranscriptionFidelity,
    val identity: OcrRecognitionIdentity,
    val confidence: Double? = null,
    val recognisedAt: Instant,
    val warnings: List<String> = emptyList(),
    val segments: List<OcrRecognitionSegment> = emptyList(),
) {
    init {
        require(recognisedText.isNotBlank()) { "OcrRecognitionResult.recognisedText must not be blank" }
        confidence?.let {
            require(it in 0.0..1.0) { "OcrRecognitionResult.confidence must fall within 0.0..1.0" }
        }
        val pageNumbers = segments.mapNotNull { it.pageNumber }
        require(pageNumbers == pageNumbers.sorted()) {
            "OcrRecognitionResult.segments must carry non-decreasing page numbers across the list -- found: $pageNumbers"
        }
    }
}

/**
 * The sealed outcome of one [OcrMechanism.recognise] call -- success
 * ([Recognised]), the original Unit 1 generic failure shape ([Failed],
 * kept unchanged as a compatibility wrapper), and, as of Implementation
 * Plan Unit 7, one concrete subclass per each of Scope Lock Section 10's
 * seven non-collapsible constitutional distinctions.
 *
 * **Unit 1's own history, preserved for context.** This class originally
 * carried only [Recognised] and [Failed] -- the smallest lawful shape
 * Scope Lock Section 10 permitted before a future unit claimed the
 * concrete-taxonomy responsibility by name. An earlier revision of this
 * file replaced seven bespoke sealed subclasses with a seven-value enum,
 * reasoning that reusing the Scope Lock's own labels avoided "inventing"
 * a taxonomy -- that reasoning was itself mistaken: an enum is,
 * definitionally, exactly the "enum-like... list of failure categories"
 * Scope Lock Section 10 refused to freeze at that tier, regardless of
 * whose label vocabulary it borrowed. [Failed] itself never discharged
 * Section 10's non-collapse requirement -- every [Failed] value looked
 * identical regardless of which distinction actually occurred.
 *
 * **Implementation Plan Unit 7 now claims that responsibility.** Its own
 * Purpose is explicit: "Design the concrete representation of the Scope
 * Lock's seven non-collapsible failure distinctions -- the first tier at
 * which Scope Lock Section 10 permits this." The seven sibling subclasses
 * below -- [NotAuthorised], [UnsupportedOrInaccessibleInput],
 * [NoRecognisableContent], [PartialOrDegradedOutput],
 * [ValidationRejection], [ProcessingOrDependencyFailure], and
 * [GenuineImplementationFault] -- are that representation, one type per
 * distinction, none collapsed into another or into [Failed].
 *
 * **[Failed] remains, unmodified, as a compatibility wrapper.** Unit 7's
 * own Files-expected-to-change field authorises extending Unit 1's own
 * failure shape, not replacing it -- and every Unit 1-6 call site
 * constructing `OcrRecognitionOutcome.Failed(reason)` directly, or
 * matching `is Failed`/reading `.reason`, must keep compiling and
 * passing unchanged (Implementation Plan Unit 7's own "whether the change
 * can remain source-compatible with Units 1-6" question, answered here:
 * yes, by addition only). [Failed] therefore continues to mean exactly
 * what it always meant -- an honest, non-blank technical explanation,
 * undifferentiated among the seven distinctions -- for any caller that
 * has not adopted the more specific Unit 7 types below.
 *
 * **Responsibility tiers, restated from Scope Lock Section 10.** Three of
 * the seven distinctions belong to the OCR mechanism's own operational
 * implementation and are freely producible by a provider adapter or the
 * sequencer: [UnsupportedOrInaccessibleInput], [ProcessingOrDependencyFailure],
 * [GenuineImplementationFault]. Two are the mechanism's own honest
 * technical disclosure, also producible now, but never itself a judgement
 * about whether the disclosed recognition is worth using downstream --
 * that judgement remains Evidence Intelligence's own, later, exclusively
 * (Scope Lock Section 11): [NoRecognisableContent], [PartialOrDegradedOutput].
 * Two are reserved, included here only "for completeness of the taxonomy"
 * (Implementation Plan Unit 7's own Constitutional constraints) and are
 * never constructed by any code path in Units 1-7: [NotAuthorised] (an
 * orchestration-layer outcome -- the OCR mechanism holds no Permission
 * Engine reference and never itself evaluates or reports it) and
 * [ValidationRejection] (Parker-owned output-quality judgement, Scope
 * Lock Section 11 -- this Unit implements no validation policy, threshold,
 * or mechanism of any kind).
 */
sealed class OcrRecognitionOutcome {

    /** A successful recognition of the supplied image content. */
    data class Recognised(val result: OcrRecognitionResult) : OcrRecognitionOutcome()

    /**
     * A failed recognition attempt, carrying only an honest, non-blank
     * technical explanation of what went wrong -- no failure kind, code,
     * or category of any shape. Preserved unchanged from Unit 1 as a
     * compatibility wrapper (see this sealed class's own KDoc); a caller
     * with no need to distinguish among Scope Lock Section 10's seven
     * distinctions may still use this shape exactly as before. It does
     * not, itself, satisfy Section 10's non-collapse requirement --
     * `Failed` values remain indistinguishable from one another by
     * distinction, by design, since undifferentiated disclosure is
     * precisely what this variant is for.
     */
    data class Failed(val reason: String) : OcrRecognitionOutcome() {
        init {
            require(reason.isNotBlank()) { "OcrRecognitionOutcome.Failed.reason must not be blank" }
        }
    }

    /**
     * Scope Lock Section 10's first constitutional distinction: "an
     * orchestration outcome, never reached by the mechanism itself...
     * denial stops before any dependency is invoked; the OCR mechanism
     * holds no Permission Engine reference of its own... and never itself
     * evaluates or reports this outcome." Represented here only "for
     * completeness of the taxonomy" (Implementation Plan Unit 7's own
     * Constitutional constraints) -- **no code path in Units 1-7 ever
     * constructs this value.** It exists so a future orchestration layer
     * has a lawful, already-reviewed shape to report denial with, without
     * this Unit adding a Permission Engine dependency, wiring a permission
     * check, or having any adapter manufacture a denial of its own.
     */
    data class NotAuthorised(val reason: String) : OcrRecognitionOutcome() {
        init {
            require(reason.isNotBlank()) { "OcrRecognitionOutcome.NotAuthorised.reason must not be blank" }
        }
    }

    /**
     * Scope Lock Section 10's second constitutional distinction: "the OCR
     * mechanism's own operational concern (Contract Design Section 6,
     * 'operational/mechanical failure'), disclosed honestly, never
     * silently substituted with a fabricated result." Produced by a
     * provider adapter when the supplied content itself cannot be
     * processed -- for example, an undecodable or unrecognised media
     * type -- distinct from every other operational or disclosure
     * condition below.
     */
    data class UnsupportedOrInaccessibleInput(val reason: String) : OcrRecognitionOutcome() {
        init {
            require(reason.isNotBlank()) { "OcrRecognitionOutcome.UnsupportedOrInaccessibleInput.reason must not be blank" }
        }
    }

    /**
     * Scope Lock Section 10's third constitutional distinction: "disclosed
     * by the OCR mechanism honestly (Contract Design Section 6); whether
     * that disclosure means the recognition is worth producing as a
     * candidate at all is Evidence Intelligence's own analytical
     * judgement, never the mechanism's own decision." Produced when
     * recognition completed without error but found nothing recognisable
     * in the supplied content -- an honest technical fact, not itself a
     * decision that the input was worthless.
     */
    data class NoRecognisableContent(val reason: String) : OcrRecognitionOutcome() {
        init {
            require(reason.isNotBlank()) { "OcrRecognitionOutcome.NoRecognisableContent.reason must not be blank" }
        }
    }

    /**
     * Scope Lock Section 10's fourth constitutional distinction: "the
     * same disclosure/judgement split as [NoRecognisableContent]: the
     * mechanism discloses what it could and could not recognise;
     * Evidence Intelligence's own analysis decides what that means for
     * the result it produces." Carries the actual, usable [partialResult]
     * exactly as recognised -- never discarded, and never silently
     * promoted to an ordinary [Recognised] as though nothing were
     * degraded (Implementation Plan Unit 7: "do not silently discard
     * usable partial recognition," "do not silently promote degraded
     * output to fully successful recognition"). Structurally distinct
     * from plain success precisely so a caller can tell, without
     * inspecting [OcrRecognitionResult.warnings] prose, that this
     * recognition needs more scrutiny than an ordinary [Recognised].
     *
     * @param partialResult The partial or degraded recognition actually
     *   produced, in full -- the same shape a clean [Recognised] would
     *   carry, preserved rather than discarded.
     * @param reason An honest, non-blank technical description of what
     *   is missing or degraded.
     */
    data class PartialOrDegradedOutput(val partialResult: OcrRecognitionResult, val reason: String) : OcrRecognitionOutcome() {
        init {
            require(reason.isNotBlank()) { "OcrRecognitionOutcome.PartialOrDegradedOutput.reason must not be blank" }
        }
    }

    /**
     * Scope Lock Section 10's fifth constitutional distinction:
     * "Parker-owned output-quality judgement (Section 11, above), never
     * the mechanism's own determination and never an orchestration-layer
     * authorisation failure; a distinct outcome from 'not authorised.'"
     * Represented here only "for completeness of the taxonomy"
     * (Implementation Plan Unit 7's own Constitutional constraints) --
     * **no code path in Units 1-7 ever constructs this value.** This Unit
     * implements no validation policy, threshold, or mechanism of any
     * kind (Scope Lock Section 11); this shape exists only so a future,
     * separately governed Parker-owned validation step has an
     * already-reviewed, structurally distinct place to report rejection,
     * never confusable with [ProcessingOrDependencyFailure] or any other
     * operational condition the OCR mechanism itself produces.
     */
    data class ValidationRejection(val reason: String) : OcrRecognitionOutcome() {
        init {
            require(reason.isNotBlank()) { "OcrRecognitionOutcome.ValidationRejection.reason must not be blank" }
        }
    }

    /**
     * Scope Lock Section 10's sixth constitutional distinction: "the OCR
     * mechanism's own operational concern (Contract Design Section 6),
     * covering conditions such as a resource limit being exceeded (Scope
     * Lock Section 15) or a required processing step being unavailable,
     * disclosed rather than silently retried or masked." Distinct from
     * [GenuineImplementationFault]: this is an anticipated operational
     * limitation, not an unexpected defect.
     */
    data class ProcessingOrDependencyFailure(val reason: String) : OcrRecognitionOutcome() {
        init {
            require(reason.isNotBlank()) { "OcrRecognitionOutcome.ProcessingOrDependencyFailure.reason must not be blank" }
        }
    }

    /**
     * Scope Lock Section 10's seventh constitutional distinction: "the OCR
     * mechanism's own operational concern (Contract Design Section 6),
     * distinct from every expected operational condition above." For a
     * provider adapter that chooses to represent a fault as a disclosed
     * outcome rather than letting it propagate as a thrown exception --
     * this variant does not change, and is never required by, Unit 3's
     * own existing fault-propagation discipline (no `try`/`catch` in
     * [parker.core.runtime.OcrExecutionSequencer]; a genuine, unexpected
     * fault an adapter throws still propagates unchanged). This shape is
     * for a fault an adapter has already caught and chosen to disclose,
     * never a substitute for propagation.
     */
    data class GenuineImplementationFault(val reason: String) : OcrRecognitionOutcome() {
        init {
            require(reason.isNotBlank()) { "OcrRecognitionOutcome.GenuineImplementationFault.reason must not be blank" }
        }
    }
}
