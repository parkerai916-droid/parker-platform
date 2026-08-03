package parker.core.interfaces

/**
 * Evidence Intelligence, Implementation Unit 1 ("Input/Output Shape
 * Foundation"), reimplemented against the amended governance that
 * resolved Governance Contradiction Report GCR-EI-UNIT1-001. Governed in
 * full by the frozen, now-amended Evidence Intelligence documentation
 * stack: `docs/decisions/CDR-007_CONSTITUTIONAL_CLASSIFICATION_OF_EVIDENCE_INTELLIGENCE.md`
 * ("CDR-007"); `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md`
 * ("the Contract Design" -- Accepted, Canonical, as amended);
 * `docs/architecture/EVIDENCE_INTELLIGENCE_SCOPE_LOCK.md` ("the Scope
 * Lock" -- Accepted, Canonical, Implementation Frozen, as amended); and
 * `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan" -- Accepted, Canonical, Implementation
 * Authorised, as amended), whose own Unit 1 this file implements exactly,
 * and only. Follows the "one file per subsystem" precedent [MemoryCore]
 * and [EvidenceCustodian] already established -- this file will grow
 * across later Units within itself, exactly as `EvidenceCustodian.kt`
 * already did across its own three Units.
 *
 * ## What this Unit implements
 *
 * The three new public shapes the Contract Design's own §3 (as amended)
 * authorises, and nothing else: [EvidenceAnalysisRequest] (§4),
 * [EvidenceAnalysisResult] (§5), and [CandidateMemoryCoreRecord] -- the
 * closed, two-case, behaviour-free payload selector for
 * [EvidenceAnalysisResult.CandidateRecordProduced], authorised by the
 * governance remediation that resolved GCR-EI-UNIT1-001
 * (`docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_1_GOVERNANCE_REMEDIATION_PLAN.md`).
 *
 * ## Corrected from the original Unit 1 attempt
 *
 * The original attempt at this Unit additionally introduced
 * `AnalyticalClaim`, a public type representing one claim within a
 * `TransientOutput`. Independent review found this exceeded the Scope
 * Lock's own "exactly three new public types" cap (as it then stood) --
 * `AnalyticalClaim` was never authorised by the Contract Design, and
 * "minimal supporting type" was not a governance exemption. `AnalyticalClaim`
 * has been removed entirely, without replacement. Claim-level traceability
 * (Contract Design §5) is instead achieved by a change of usage, not a new
 * type: each `TransientOutput` value now represents exactly one material
 * analytical claim directly, and a multi-claim analysis is represented by
 * returning multiple `TransientOutput` values in one invocation's result
 * list (see [EvidenceAnalysisResult.TransientOutput]'s own KDoc). The
 * second unauthorised type from that attempt, a wrapper for
 * [EvidenceAnalysisResult.CandidateRecordProduced]'s own payload, has
 * since been expressly authorised by the amended Contract Design and
 * Scope Lock and survives, under the same name, as [CandidateMemoryCoreRecord]
 * below.
 *
 * ## What this Unit deliberately does not implement
 *
 * - **No `EvidenceIntelligence` interface.** The Implementation Plan's own
 *   Unit 1 KDoc states the three shapes below are "the sole new public
 *   surface besides the operation itself" -- the operation is assembled
 *   later, by Unit 5, behind Units 1-4. No interface, method signature, or
 *   operation of any kind is declared anywhere in this file.
 * - **No retrieval.** Nothing here calls, or could call,
 *   `EvidenceCustodian.retrieve` or `MemoryRetrieval` -- both remain Unit
 *   2's own responsibility. [EvidenceAnalysisRequest] carries references
 *   only; resolving them is a later Unit's concern entirely.
 * - **No reasoning or orchestration.** Nothing here invokes a
 *   [ReasoningProvider] -- that is Unit 3's own responsibility.
 * - **No acceptance coordinator, no Permission Engine gating, no runtime
 *   wiring.** None of Units 6, 7, or 8 exist in this file in any form.
 * - **No Memory Core acceptance, no Knowledge submission.** Nothing here
 *   calls `MemoryCore`'s write interface, `EvidenceCustodian.accept`, or
 *   Knowledge Memory's Knowledge Submission interface. The types below
 *   carry candidates only -- they never submit themselves anywhere.
 * - **No analysis logic, transcription, OCR, provenance processing, or
 *   contradiction analysis of any kind.** This file contains plain data
 *   shapes and their own construction-time structural validation only --
 *   no method beyond what Kotlin's `data class`/`sealed class` machinery
 *   generates, and no behaviour that inspects, compares, or interprets the
 *   content any of these shapes carries.
 * - **No confidence field, no evidential-state field, anywhere.**
 *   Structurally impossible by omission, not by runtime check (Contract
 *   Design §11's own "structural prevention, not a runtime check" -- for
 *   exactly the reason [KnowledgeCandidate] itself already carries neither
 *   field).
 * - **No raw evidence.** [EvidenceAnalysisRequest] carries only
 *   [EvidenceArtifactId] values (an existing identifier, never content)
 *   and [RelationshipEndpoint] values (an existing identifier pair, never
 *   a copy of record content) -- no `ByteArray`, no raw content field of
 *   any kind, anywhere in this file.
 *
 * ## Why `RelationshipEndpoint`, not `MemoryCoreRecordReference`, for
 * `EvidenceAnalysisRequest`'s Memory Core references
 *
 * Contract Design §4 states the Memory Core reference list uses "the same
 * (record kind, record identifier) pair shape `Relationship`'s own
 * endpoints already use (Memory Core Contract Design §8)" -- naming
 * [RelationshipEndpoint] exactly, not [MemoryCoreRecordReference] (the
 * narrower, four-case-exhaustive type `MemoryCore.transitionStatus` uses).
 * [RelationshipEndpoint] is reused here entirely unmodified, exactly as
 * every other type in this file is.
 *
 * ## Why no rejection of an evidence-less, reference-less request is
 * enforced here
 *
 * Contract Design §11 requires a concrete implementation of the
 * Evidence Intelligence *operation* to reject a request naming no
 * retrievable evidence and no resolvable Memory Core reference "before
 * invoking any analytical or Reasoning Provider step" -- an operational
 * behaviour of Unit 5's own operation, not a constructor-time invariant of
 * this shape. Contract Design §4 itself confirms each individual list
 * "may be empty, for an analysis that operates only on already-registered
 * Memory Core records" -- so neither list is rejected alone, and this Unit
 * has no operation yet to reject anything from. Enforcing the combined
 * check here would anticipate Unit 2/Unit 5's own responsibility, which
 * this Unit's own governing instruction forbids.
 *
 * ## On the name `CandidateMemoryCoreRecord`
 *
 * No governing document assigns a Kotlin name to the payload selector --
 * the Contract Design and Scope Lock both state explicitly that its name
 * is left to this Unit. `CandidateMemoryCoreRecord` is chosen because it
 * mirrors this repository's own existing [MemoryCoreRecord] precedent
 * exactly (a sealed wrapper of exactly the Memory-Core-owned record kinds
 * relevant to one already-governed operation, one nested case per kind,
 * named `OfEntity`/`OfDocument`/`OfAssertion`/`OfRelationship`), narrowed
 * to the two candidate kinds Contract Design §5's own "Candidate record
 * produced" row authorises. This is an implementation-level naming
 * choice under the amended governance, not itself a constitutional
 * decision -- a future revision could rename this type without any
 * governance amendment, provided its frozen shape and properties are
 * preserved.
 */

/**
 * The single input shape for one Evidence Intelligence analysis
 * invocation (Contract Design §3, §4). Every field is either a reference
 * to an existing, unmodified type, or an open classification string --
 * never a copy of governed content, and never a caller-declared
 * confidence or evidential-state value of any kind (Contract Design §4's
 * own "never accepted as input" list).
 *
 * @param analysisKind A non-blank, open, non-enumerated classification of
 *   what analysis is being requested (Contract Design §4) -- mirroring
 *   [Entity.entityType]'s and [Document.documentType]'s own "open, not
 *   closed" convention. Naming a new value here never creates, expands,
 *   or implies any new authority (Contract Design §4's own explicit
 *   statement).
 * @param requestingPrincipalId Reused unchanged, for audit purposes only
 *   -- never a filter or authorisation decision this shape, or anything
 *   in this file, applies on its own (Contract Design §4).
 * @param evidenceArtifactIds Custodied evidence, by reference only
 *   (Contract Design §4). Resolved only through `EvidenceCustodian.retrieve`
 *   by a later Unit; may be empty for an analysis operating only on
 *   already-registered Memory Core records. Left empty alongside an empty
 *   [memoryCoreReferences], this shape is still validly constructed --
 *   rejecting a request with nothing to analyse is an operational
 *   responsibility of a later Unit's operation, never this constructor's
 *   (Contract Design §11).
 * @param memoryCoreReferences Memory Core records, by reference only,
 *   using [RelationshipEndpoint]'s existing (record kind, record
 *   identifier) pair shape (Contract Design §4). Resolved only through
 *   `MemoryRetrieval` by a later Unit; may be empty for an analysis
 *   operating only on custodied evidence.
 * @param reasoningContext An optional, already-assembled [ReasoningContext]
 *   (Contract Design §4), supplied only when this analysis internally
 *   invokes one or more [ReasoningProvider]s -- a later Unit's own
 *   responsibility to invoke, not this shape's to interpret.
 */
data class EvidenceAnalysisRequest(
    val analysisKind: String,
    val requestingPrincipalId: PrincipalId,
    val evidenceArtifactIds: List<EvidenceArtifactId> = emptyList(),
    val memoryCoreReferences: List<RelationshipEndpoint> = emptyList(),
    val reasoningContext: ReasoningContext? = null,
) {
    init {
        require(analysisKind.isNotBlank()) { "EvidenceAnalysisRequest.analysisKind must not be blank" }
    }
}

/**
 * The one selector type the amended Contract Design (§3) and Scope Lock
 * (§4, §5) authorise, letting [EvidenceAnalysisResult.CandidateRecordProduced]
 * carry exactly one of the two record kinds Contract Design §5's own
 * "Candidate record produced" row names -- an existing, unmodified
 * [CandidateAssertion] or [CandidateRelationship], including
 * `SUPPORTS`/`CONTRADICTS` relationships -- never both, never neither, and
 * never [CandidateEntity] or [CandidateDocument] (not named by that row).
 * See this file's own header KDoc, "On the name `CandidateMemoryCoreRecord`,"
 * for why this name and shape were chosen.
 *
 * Frozen properties (Contract Design §3, as amended; Scope Lock §4, §5,
 * as amended), none of which any future revision may weaken:
 *
 * - Behaviour-free -- no method beyond what Kotlin's `sealed class`/
 *   `data class` machinery generates.
 * - Closed to exactly two cases -- [OfAssertion], [OfRelationship] --
 *   never a third without a further Contract Design amendment.
 * - Owns no data beyond the one selected candidate value.
 * - Not itself a candidate output category -- one of exactly four
 *   [EvidenceAnalysisResult] variants remains "Candidate record produced";
 *   this type only supplies the shape that variant needs to compile,
 *   since Kotlin has no union type.
 * - Grants no acceptance, persistence, retrieval, reasoning, confidence,
 *   evidential-state, provenance, or ownership authority of its own -- a
 *   pure selection mechanism.
 * - Does not modify [CandidateAssertion] or [CandidateRelationship] --
 *   both remain reused, unmodified; this type only references them.
 * - Not a generic union abstraction -- closed to these two named,
 *   concrete existing types only, never a type-parameterised mechanism
 *   reusable for any other pair of types.
 * - Creates no independent dependency entitlement for any other
 *   subsystem -- owned exclusively by Evidence Intelligence. Legitimate
 *   `EvidenceAnalysisResult` consumers (including the separate acceptance
 *   coordinator, a later Unit) may inspect a value of this type solely to
 *   determine which existing candidate it carries, in order to dispatch
 *   that candidate through the frozen acceptance sequence -- that
 *   consumption grants no ownership, authority, extension right, or
 *   independent dependency entitlement over this type itself.
 */
sealed class CandidateMemoryCoreRecord {
    data class OfAssertion(val candidateAssertion: CandidateAssertion) : CandidateMemoryCoreRecord()
    data class OfRelationship(val candidateRelationship: CandidateRelationship) : CandidateMemoryCoreRecord()
}

/**
 * The sealed output shape for one Evidence Intelligence analysis
 * invocation -- exactly four variants, one per CDR-007 §6 permitted-
 * analytical-artefact category, no more, no fewer (Contract Design §5).
 * Each variant carries only an existing, unmodified candidate type, or --
 * for [TransientOutput] -- plain prose together with references to
 * existing, already-governed identifiers; never a new record shape, never
 * a new identifier or provenance type, never a caller-declared confidence
 * or evidential-state value (Contract Design §3, §5, §11).
 *
 * No fifth variant, no wrapper type, no partial-result type, and no
 * alternate output taxonomy exist here or anywhere else in this file
 * (Scope Lock §3, as amended, restating Contract Design §11's own "no
 * fourth failure variant" instruction, applied identically here to a
 * would-be fifth output variant). Partial completion across many
 * referenced inputs within one invocation is represented, per Contract
 * Design §11, by an ordinary, non-empty list of the four variants below
 * for the inputs that succeeded -- never by a variant, wrapper, or field
 * this file defines; a later Unit's own responsibility to assemble that
 * list, not this Unit's to represent differently.
 *
 * No operation, method, or persistence/acceptance behaviour of any kind
 * is declared on this type or any variant below -- every value here is an
 * inert, disposable proposal until some later, separate responsibility
 * (never Evidence Intelligence itself, Contract Design §5's own "Evidence
 * Intelligence performs only the first" statement) accepts it.
 */
sealed class EvidenceAnalysisResult {

    /**
     * A plain, disposable prose result representing exactly one material
     * analytical claim (a single observation within a working chronology,
     * a single point within a draft comparison) -- never accepted or
     * submitted anywhere, discarded or regenerated freely (Contract Design
     * §5). Carries only its own prose and the specific governed
     * reference(s) supporting that one claim -- never a copy of their
     * content, and never a new provenance-carrying type (Contract Design
     * §8's own "this same governed-reference mechanism, not a parallel
     * provenance type").
     *
     * **Claim-level traceability without a claim type.** An earlier
     * attempt at this Unit introduced a separate `AnalyticalClaim` public
     * type, and represented a multi-claim analysis as one `TransientOutput`
     * carrying a list of such claims. That type was unauthorised and has
     * been removed (see this file's own header KDoc, "Corrected from the
     * original Unit 1 attempt"). Instead, each `TransientOutput` value
     * represents exactly one claim directly, and a multi-claim analysis is
     * represented by returning multiple `TransientOutput` values in one
     * invocation's own result list (Contract Design §5's own "one
     * invocation may produce many outputs... rather than assuming exactly
     * one output per analysis" mechanism) -- never one `TransientOutput`
     * bundling several claims, and never one reference list pooled across
     * several claims.
     *
     * At least one of [evidenceArtifactReferences] or
     * [memoryCoreReferences] must be non-empty -- a claim traceable to
     * nothing is not a governed claim at all (Contract Design §5's own
     * "each such claim must itself be traceable back to the specific
     * governed evidence reference(s) that support it").
     */
    data class TransientOutput(
        val text: String,
        val evidenceArtifactReferences: List<EvidenceArtifactId> = emptyList(),
        val memoryCoreReferences: List<RelationshipEndpoint> = emptyList(),
    ) : EvidenceAnalysisResult() {
        init {
            require(text.isNotBlank()) { "TransientOutput.text must not be blank" }
            require(evidenceArtifactReferences.isNotEmpty() || memoryCoreReferences.isNotEmpty()) {
                "TransientOutput must be traceable to at least one governed evidence reference " +
                    "(evidenceArtifactReferences or memoryCoreReferences)"
            }
        }
    }

    /**
     * An existing, unmodified [CandidateEvidenceArtifact] (an OCR
     * transcription, an extraction, a translation) -- owned by Evidence
     * Intelligence until accepted into Evidence Custodian via its
     * existing `accept` path, never by this shape (Contract Design §5).
     */
    data class CandidateArtifactProduced(
        val candidateEvidenceArtifact: CandidateEvidenceArtifact,
    ) : EvidenceAnalysisResult()

    /**
     * An existing, unmodified [CandidateAssertion] or [CandidateRelationship]
     * -- owned by Evidence Intelligence until written into Memory Core via
     * its existing public write interface, never by this shape (Contract
     * Design §5). See [CandidateMemoryCoreRecord] for why this variant
     * carries that selector rather than either candidate type directly.
     */
    data class CandidateRecordProduced(
        val candidateRecord: CandidateMemoryCoreRecord,
    ) : EvidenceAnalysisResult()

    /**
     * An existing, unmodified [KnowledgeCandidate], referencing
     * already-accepted Memory Core evidence, carrying no confidence or
     * evidential-state field of its own (Contract Design §5) -- owned by
     * Evidence Intelligence until submitted to Knowledge Memory via its
     * existing Knowledge Submission interface, never by this shape.
     * [KnowledgeCandidate] itself already structurally forbids naming a
     * not-yet-accepted record (Contract Design §5's own "may reference
     * only already-accepted Memory Core evidence" rule) -- enforcing that
     * ordering is a later Unit's own responsibility, not this shape's.
     */
    data class CandidateKnowledgeProduced(
        val knowledgeCandidate: KnowledgeCandidate,
    ) : EvidenceAnalysisResult()
}
