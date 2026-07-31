package parker.core.interfaces

import java.time.Instant

/**
 * Evidence Custodian -- Implementation Unit 1 (Foundational Identity),
 * corrected. Opens `src/interfaces/EvidenceCustodian.kt`, following the
 * "one file per subsystem" precedent [MemoryCore] and [WorldModel] already
 * established. Governed by the frozen Evidence Custodian documentation
 * stack: `docs/architecture/parker-constitution.md`;
 * `docs/architecture/epistemic-integrity.md` (Article IX, as amended);
 * `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
 * ("CDR-006"); `docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`
 * ("the Contract Design"); `docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`
 * ("the Scope Lock"); and `docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan").
 *
 * ## Correction history
 *
 * This Unit originally also introduced an `EvidenceArtifact` data class
 * carrying `artifactType` and `acceptedAt` fields. Architectural review
 * found both fields exceeded what Phase 1 may truthfully represent:
 * `acceptedAt` asserted a governed acceptance fact with no acceptance
 * mechanism (Permission Engine gating, Implementation Plan Phase 3) yet in
 * existence to produce it, and nothing prevented arbitrary direct
 * construction of a value falsely claiming an artefact had been accepted;
 * `artifactType` had no frozen Phase 1 requirement, only a design
 * inference from the Contract Design's descriptive examples (Section 2).
 * `EvidenceArtifact` has been removed entirely, without replacement, per
 * that review. This Unit now implements exactly one thing: [EvidenceArtifactId].
 *
 * ## What this Unit implements
 *
 * A single, non-vacuous identity value ([EvidenceArtifactId]) and the
 * foundational terminology later phases will build on. Nothing else.
 *
 * ## What this Unit deliberately does not implement, and does not claim
 *
 * - **No custody record, artefact shape, or acceptance fact of any kind.**
 *   This file contains no type representing "an artefact in custody" --
 *   that concept is not yet safe to represent, since the governed
 *   acceptance path (Implementation Plan Section 4, Phase 3) that alone
 *   may produce it does not exist yet.
 * - **No storage, persistence, or location mechanism.** No field anywhere
 *   in this file names a file path, byte content, object-store key, or
 *   database reference (Scope Lock Section 9).
 * - **No hashing, integrity-verification, or encryption scheme.** (Scope
 *   Lock Section 9; Implementation Plan Section 6.)
 * - **No lifecycle status field or enum.** The Contract Design's own
 *   Section 7 states its seven conceptual lifecycle stages are "not
 *   necessarily exclusive or strictly sequential" and explicitly that "no
 *   implementation state, Kotlin enum, or status field is defined or
 *   implied by the stages above." Nothing here represents any of them.
 * - **No interface, method, or API of any kind.** An `EvidenceCustodian`
 *   interface -- the surface a future acceptance/retrieval/deletion
 *   operation will live on -- is not introduced by this Unit
 *   (Implementation Plan Section 4, Phases 3, 4, 7).
 * - **No Permission Engine integration.** No type in this file is ever
 *   constructed, accepted, or authorised by anything yet.
 * - **No ownership, provenance, or derivative-link field.** (CDR-006,
 *   Contract Design Section 5; Contract Design Section 6.2.)
 * - **No uniqueness or non-reassignment guarantee.** [EvidenceArtifactId]
 *   is a value type only; nothing in this file tracks which identifiers
 *   have already been issued or prevents two different constructions from
 *   reusing the same identifier. Scope Lock Section 6's "assigned once, at
 *   acceptance, and never reassigned" guarantee is a property of a future
 *   governed system over time, not a property this bare value type can, or
 *   does, establish on its own.
 *
 * ## The accurate Phase 1 claim
 *
 * Phase 1 establishes the value-level representation of an evidence
 * artefact identifier. It does not yet establish issuance, uniqueness,
 * acceptance, custody, storage, or constitutional immutability
 * enforcement.
 */

/**
 * A non-vacuous identity value within the Evidence Custodian domain.
 * Rejects blank content at construction and compares by value, exactly
 * like every other identifier value class already established in this
 * repository ([EntityId], [DocumentId], [ProvenanceId], [RelationshipId]).
 *
 * This type identifies an artefact within the Evidence Custodian domain
 * and nothing more. Holding, or being able to construct, an
 * [EvidenceArtifactId] value proves none of the following:
 *
 * - that any artefact has been accepted into custody;
 * - that custody exists, or ever existed, for this value;
 * - that this value is unique -- nothing here prevents two different,
 *   unrelated constructions from using the same string;
 * - that this value, once used, will never be reassigned or reused by a
 *   future system;
 * - that any storage, provenance, or integrity guarantee attaches to it.
 *
 * Governed acceptance (Implementation Plan Section 4, Phase 3), storage
 * (Phase 2), and any guarantee of stable, non-reassigned, unique issuance
 * are future phases' responsibility, not this type's. This type supplies
 * only the shared vocabulary -- "an identity of this shape exists" -- that
 * those later phases will need in order to be built at all.
 */
@JvmInline
value class EvidenceArtifactId(val value: String) {
    init {
        require(value.isNotBlank()) { "EvidenceArtifactId must not be blank" }
    }
}

/**
 * Evidence Custodian, Implementation Plan Phase 3 ("Governed acceptance
 * path"), Unit 2. Adds exactly what Unit 1's own closing KDoc deferred to
 * this phase: [CandidateEvidenceArtifact], [AcceptedEvidenceArtifact],
 * [EvidenceAcceptanceResult], and the [EvidenceCustodian] interface itself
 * -- the surface Unit 1 explicitly did not introduce ("An `EvidenceCustodian`
 * interface ... is not introduced by this Unit (Implementation Plan
 * Section 4, Phases 3, 4, 7)"). This Unit implements Phase 3 only --
 * `accept` is the sole operation; retrieval (Phase 4) and deletion (Phase 7)
 * remain future additions to this same interface, exactly as Memory Core's
 * own `MemoryCore` interface grew across its own Units within one file.
 *
 * ## What this Unit implements
 *
 * The governed acceptance boundary: candidate evidence in, a
 * Permission-Engine-authorised, persisted, accepted result out, or an
 * explicit, non-exceptional rejection. Reuses [EvidenceArtifactId] (Unit
 * 1) and the existing [PermissionEngine] interface unmodified; the
 * concrete implementation ([parker.core.runtime.DefaultEvidenceCustodian])
 * additionally reuses `EvidenceArtifactStorage` (Unit 1, Phase 2)
 * unmodified.
 *
 * ## Candidate / Accepted split, mirroring Memory Core's own Errata 002
 * pattern
 *
 * [CandidateEvidenceArtifact] carries only what a caller can actually
 * supply -- raw content, nothing else. It carries no identity (minted
 * only by a successful, governed [EvidenceCustodian.accept] call) and no
 * acceptance timestamp (assigned only there too) -- precisely the
 * discipline Unit 1's own "Correction history" section found missing from
 * its first, since-removed draft of an `EvidenceArtifact` type: nothing
 * about constructing a [CandidateEvidenceArtifact] can be mistaken for, or
 * used to fabricate, a claim that acceptance occurred. [AcceptedEvidenceArtifact]
 * is the completed record -- identity plus acceptance timestamp -- and is
 * never constructed by this file directly; only [EvidenceCustodian.accept]
 * produces one, exactly mirroring `MemoryCore`'s own
 * `CandidateEntity -> MemoryCore.createEntity -> Entity` shape.
 *
 * [CandidateEvidenceArtifact] and [AcceptedEvidenceArtifact] both carry no
 * `artifactType`/classification field, no owner field, and no provenance
 * or storage-location field -- document classification, ownership
 * determination, and provenance are each explicitly excluded from this
 * Unit's own scope (this Unit's own governing instruction; CDR-006;
 * Contract Design Section 5).
 *
 * ## Empty content is permitted, consistent with Unit 1's own decision
 *
 * [CandidateEvidenceArtifact.content] carries no non-empty requirement --
 * `EvidenceArtifactStorage`'s own Unit 1 test suite already established
 * that a zero-length artefact is accepted, not rejected, at the storage
 * layer ("a zero-byte artefact is a legitimate, if unusual, artefact").
 * This Unit does not silently introduce a stricter rule storage itself
 * does not have.
 *
 * ## `EvidenceAcceptanceResult`, not a thrown rejection
 *
 * A denied Permission Engine decision is an ordinary, expected outcome --
 * not a fault -- so [EvidenceCustodian.accept] returns
 * [EvidenceAcceptanceResult.Rejected] rather than throwing, mirroring
 * [WorldModel]'s own [ObservationResult] precedent (a sealed result type
 * defined alongside its owning interface, in `src/interfaces`) rather than
 * `src/runtime`'s own `GatedOutcome` -- reusing `GatedOutcome` here would
 * have required this `src/interfaces` file to depend on a `src/runtime`
 * type, inverting this repository's own existing interfaces/runtime
 * layering. A storage failure (a genuine fault -- disk error, an
 * unexpected duplicate identifier) is a different kind of outcome and is
 * deliberately not folded into this same result type: it propagates as
 * the thrown `EvidenceArtifactStorageException` Unit 1 already defined,
 * unchanged, per this Unit's own "storage and acceptance remain separate
 * responsibilities" instruction.
 *
 * ## What this Unit deliberately does not implement
 *
 * No Evidence Intelligence; no evidence search or retrieval API (Phase 4,
 * not this Unit); no provenance graph or Memory Core registration (a
 * separate, later concern -- this Unit's own candidate/accepted types
 * carry no provenance field for exactly that reason); no hashing, OCR, or
 * document classification; no duplicate-content detection or semantic
 * comparison (this Unit only ever prevents *identifier* collision, via
 * `EvidenceArtifactStorage`'s own existing guarantee -- never compares two
 * different identifiers' content); no chain-of-custody reporting; no
 * change to Memory Core; no new storage implementation (Unit 1's own two
 * implementations are reused, unmodified); no runtime composition (wiring
 * a real, production [PermissionEngine] with the specific
 * `ResourceRegistry`/`ActionMapper` entries this Unit's own well-known
 * conventions below assume is a future Unit's own responsibility, not
 * this one's).
 *
 * ## Two disclosed conventions this Unit's own `accept` implementation
 * depends on, without resolving how they get wired in production
 *
 * [parker.core.runtime.DefaultEvidenceCustodian] constructs an
 * `ExecutionRequest` naming a fixed, well-known [PrincipalId]-independent
 * `ResourceId` (`"evidence-custodian-intake"`) and a fixed proposed-action
 * name (`"evidence.accept"`). Neither is registered anywhere by this Unit
 * -- no call to `ResourceRegistry.register` or an `ActionVocabulary`
 * registration exists in this Unit's own code, since registering either
 * is runtime-composition work this Unit is explicitly excluded from. A
 * production [PermissionEngine] (`DefaultPermissionEngine` backed by
 * `DefaultPermissionPolicy`) will resolve neither name to anything until a
 * future Unit registers a `DOCUMENT`-typed Resource under that identifier
 * and an `ActionVocabulary` entry mapping that action name to
 * `(PermissionAction.WRITE, ResourceType.DOCUMENT)` -- until then, a real
 * `DefaultPermissionPolicy` would deny every request through this path
 * (its own documented "Unknown Action"/"Unknown Resource" -> `DENIED`
 * behaviour), which is the safe, conservative failure mode, not a
 * misbehaviour. This Unit's own tests exercise the *orchestration*
 * (`FakePermissionEngine`, exactly as `DefaultExecutionPipelineTest`
 * already does) rather than a real policy, for the same reason
 * `DefaultPermissionPolicy`'s own tests do not invent policy content that
 * is not yet specified.
 */

/**
 * What a caller submits to [EvidenceCustodian.accept] -- content only.
 * Carries no [EvidenceArtifactId] (minted only by a successful `accept`
 * call), no acceptance timestamp, no classification, no owner, and no
 * provenance reference. See this file's own Unit 2 KDoc, above, for why
 * each of those is deliberately absent.
 *
 * Not a `data class`: a Kotlin `data class`'s auto-generated `equals`/
 * `hashCode` compare a `ByteArray` property by reference, not by content,
 * which would silently misrepresent equality for this type. [equals] and
 * [hashCode] are overridden here to compare [content] structurally
 * ([ByteArray.contentEquals]/[ByteArray.contentHashCode]) instead.
 */
class CandidateEvidenceArtifact(val content: ByteArray) {

    override fun equals(other: Any?): Boolean =
        other is CandidateEvidenceArtifact && content.contentEquals(other.content)

    override fun hashCode(): Int = content.contentHashCode()

    override fun toString(): String = "CandidateEvidenceArtifact(content=<${content.size} bytes>)"
}

/**
 * The result of a successful, governed acceptance transition -- returned
 * only by [EvidenceCustodian.accept], never constructed directly by any
 * caller. [evidenceArtifactId] is minted, once, inside that governed call;
 * [acceptedAt] is read, once, at the same moment -- neither is
 * caller-supplied, mirroring [Entity.createdAt]/[Document.registeredAt]'s
 * own identical "Memory-Core-owned timestamp" treatment.
 *
 * Carries no content of its own -- the accepted bytes live only in
 * `EvidenceArtifactStorage`, addressable by [evidenceArtifactId]. Carries
 * no classification, owner, or provenance field, for the same reasons
 * [CandidateEvidenceArtifact] does not (this file's own Unit 2 KDoc).
 * Retrieving the actual stored content back is explicitly out of this
 * Unit's own scope -- Implementation Plan Phase 4, not Phase 3.
 */
data class AcceptedEvidenceArtifact(
    val evidenceArtifactId: EvidenceArtifactId,
    val acceptedAt: Instant,
)

/**
 * What [EvidenceCustodian.accept] returns -- an explicit, non-exceptional
 * outcome, since a denied Permission Engine decision is an ordinary,
 * expected result of a governed proposal, not a fault (this file's own
 * Unit 2 KDoc explains why this is a sealed result type here, in
 * `src/interfaces`, rather than a reuse of `src/runtime`'s `GatedOutcome`).
 */
sealed class EvidenceAcceptanceResult {

    /** Acceptance was authorised and the content was durably persisted. */
    data class Accepted(val acceptedEvidenceArtifact: AcceptedEvidenceArtifact) : EvidenceAcceptanceResult()

    /**
     * Acceptance was not authorised. [reason] is a plain-language
     * explanation, never a caller-facing policy justification this Unit
     * has no basis to construct -- see [parker.core.runtime.DefaultEvidenceCustodian]'s
     * own KDoc for exactly what it contains.
     */
    data class Rejected(val reason: String) : EvidenceAcceptanceResult() {
        init {
            require(reason.isNotBlank()) { "EvidenceAcceptanceResult.Rejected.reason must not be blank" }
        }
    }
}

/**
 * The governed acceptance boundary itself. A single operation for this
 * Unit -- [accept] -- with [parker.core.runtime.DefaultEvidenceCustodian]
 * as its sole implementation. No implementation of this interface may
 * accept evidence implicitly, silently, or as a side effect of any other
 * operation (Contract Design Section 4's own "Accepting evidence into
 * custody" requirement, restated here as this interface's own contract).
 */
interface EvidenceCustodian {
    suspend fun accept(
        requestingPrincipalId: PrincipalId,
        candidate: CandidateEvidenceArtifact,
    ): EvidenceAcceptanceResult
}
