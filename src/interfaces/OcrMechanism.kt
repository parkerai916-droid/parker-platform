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
 */
data class OcrRecognitionIdentity(
    val mechanismIdentity: String,
    val configurationProfile: String,
    val mechanismVersion: String? = null,
) {
    init {
        require(mechanismIdentity.isNotBlank()) { "OcrRecognitionIdentity.mechanismIdentity must not be blank" }
        require(configurationProfile.isNotBlank()) { "OcrRecognitionIdentity.configurationProfile must not be blank" }
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
 *   supplied image content. Page-aligned representation, where the
 *   request's own page scope supports it, is deferred to Implementation
 *   Plan Unit 6's own refinement of this shape -- this Unit fixes only
 *   that the recognised text itself is present and non-blank.
 * @param fidelity Which of [TranscriptionFidelity]'s three categories
 *   this recognition represents.
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
 *   convention.
 */
data class OcrRecognitionResult(
    val recognisedText: String,
    val fidelity: TranscriptionFidelity,
    val identity: OcrRecognitionIdentity,
    val confidence: Double? = null,
    val recognisedAt: Instant,
    val warnings: List<String> = emptyList(),
) {
    init {
        require(recognisedText.isNotBlank()) { "OcrRecognitionResult.recognisedText must not be blank" }
        confidence?.let {
            require(it in 0.0..1.0) { "OcrRecognitionResult.confidence must fall within 0.0..1.0" }
        }
    }
}

/**
 * The sealed outcome of one [OcrMechanism.recognise] call -- exactly two
 * variants, success or failure, distinguishable from one another and
 * from a thrown exception (mirroring [ExtractionOutcome]'s own "no
 * default/empty result standing in for a genuine failure" discipline).
 *
 * **This is the smallest lawful Unit 1 shape, not a finished failure
 * model.** Scope Lock Section 10 is explicit and binding: "it freezes no
 * exhaustive, named, coded, or enum-like list of failure categories,"
 * and "the concrete taxonomy, naming, and representation of these
 * distinctions remain future governance or implementation work -- a
 * future Implementation Plan's own responsibility." Implementation Plan
 * Unit 7's own Purpose claims that responsibility by name: "the first
 * tier at which Scope Lock Section 10 permits" concrete representation.
 * An earlier revision of this file replaced seven bespoke sealed
 * subclasses with a seven-value enum, reasoning that reusing the Scope
 * Lock's own labels avoided "inventing" a taxonomy -- that reasoning was
 * itself mistaken: an enum is, definitionally, exactly the "enum-like...
 * list of failure categories" Scope Lock Section 10 refuses to freeze,
 * regardless of whose label vocabulary it borrows. This Unit therefore
 * introduces no enum, no code, no category-name vocabulary, and no
 * closed list of failure kinds of any shape -- only that a recognition
 * either succeeds ([Recognised]) or fails ([Failed]), carrying nothing
 * more than an honest, non-blank technical explanation.
 *
 * **[Failed] does not itself satisfy Scope Lock Section 10's
 * non-collapse requirement, and must never be treated as though it
 * does.** Scope Lock Section 10 remains binding that a future
 * implementation "must not collapse" its seven named constitutional
 * distinctions (not authorised; unsupported or inaccessible input; no
 * recognisable content; partial or technically degraded output;
 * validation rejection; processing or dependency failure; genuine
 * implementation fault) into one another. That obligation is not
 * discharged by this Unit's own [Failed] shape -- every [Failed] value
 * looks identical regardless of which of the seven distinctions actually
 * occurred, and no caller, test, or future unit may treat that
 * indistinguishability as evidence the non-collapse requirement is
 * already met. Implementation Plan Unit 7 remains the first, and only,
 * tier authorised to give those seven distinctions their concrete
 * representation; no downstream code written under this Unit may assume,
 * encode, or rely upon any particular one of them existing yet.
 */
sealed class OcrRecognitionOutcome {

    /** A successful recognition of the supplied image content. */
    data class Recognised(val result: OcrRecognitionResult) : OcrRecognitionOutcome()

    /**
     * A failed recognition attempt, carrying only an honest, non-blank
     * technical explanation of what went wrong -- no failure kind, code,
     * or category of any shape. Implementation Plan Unit 7 owns replacing
     * or extending this shape with the first concrete representation of
     * Scope Lock Section 10's own seven, still-binding, non-collapsible
     * distinctions.
     */
    data class Failed(val reason: String) : OcrRecognitionOutcome() {
        init {
            require(reason.isNotBlank()) { "OcrRecognitionOutcome.Failed.reason must not be blank" }
        }
    }
}
