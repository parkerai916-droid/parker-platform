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
 * What a caller submits to [EvidenceCustodian.accept] -- content, plus
 * two optional, purely declarative facts about that content's original
 * submission. Carries no [EvidenceArtifactId] (minted only by a
 * successful `accept` call), no acceptance timestamp, no classification,
 * no owner, and no provenance reference. See this file's own Unit 2
 * KDoc, above, for why each of those is deliberately absent.
 *
 * ## [receivedMediaType] and [originalFileName] -- Authoritative Source
 * Manifest Foundation Implementation
 *
 * Governed by `docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 25, which names widening this exact type as
 * one of two authorized shapes for capturing a caller-declared received
 * media type at admission time -- the only moment such a fact can ever
 * be truthfully captured (Scope Lock Section 9: "the original ingress
 * channel's own declaration ... never Parker's own later inspection of
 * the bytes"). Both fields are optional, `null`-defaulted, purely
 * pass-through declarations this class neither computes, infers, nor
 * validates against [content] -- exactly the same "caller supplies,
 * Evidence Custodian never asserts" discipline [content] itself already
 * has. This is not a classification judgment (the kind of field this
 * type's own Correction history, above, already excludes) -- it is a
 * literal, externally-declared fact recorded, never evaluated.
 *
 * Not a `data class`: a Kotlin `data class`'s auto-generated `equals`/
 * `hashCode` compare a `ByteArray` property by reference, not by content,
 * which would silently misrepresent equality for this type. [equals] and
 * [hashCode] are overridden here to compare [content] structurally
 * ([ByteArray.contentEquals]/[ByteArray.contentHashCode]) instead.
 */
class CandidateEvidenceArtifact(
    val content: ByteArray,
    val receivedMediaType: String? = null,
    val originalFileName: String? = null,
) {
    init {
        require(receivedMediaType == null || receivedMediaType.isNotBlank()) {
            "CandidateEvidenceArtifact.receivedMediaType must not be blank if present -- omit it (null) " +
                "instead to express a genuinely undeclared media type"
        }
        require(originalFileName == null || originalFileName.isNotBlank()) {
            "CandidateEvidenceArtifact.originalFileName must not be blank if present -- omit it (null) " +
                "instead to express a genuinely undeclared filename"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is CandidateEvidenceArtifact &&
            content.contentEquals(other.content) &&
            receivedMediaType == other.receivedMediaType &&
            originalFileName == other.originalFileName

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + (receivedMediaType?.hashCode() ?: 0)
        result = 31 * result + (originalFileName?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "CandidateEvidenceArtifact(content=<${content.size} bytes>, receivedMediaType=$receivedMediaType, " +
            "originalFileName=$originalFileName)"
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
 * Evidence Custodian, Implementation Plan Phase 4 ("Retrieval interface
 * behaviour"), Unit 3. Adds exactly what Unit 2's own closing KDoc
 * deferred to this phase: [EvidenceRetrievalResult] and
 * [EvidenceCustodian.retrieve] -- an authorised, observational read
 * operation, gated the same way [EvidenceCustodian.accept] is gated
 * (Scope Lock Section 7's own "Authorised read access confers no write,
 * mutation, or replacement capability").
 *
 * ## What this Unit implements
 *
 * A second operation on the same [EvidenceCustodian] interface Unit 2
 * introduced: given an already-existing [EvidenceArtifactId] and a
 * requesting [PrincipalId], return the exact bytes [EvidenceCustodian.accept]
 * persisted for that identifier -- or an explicit, non-exceptional
 * indication that the request was not authorised, or that no artefact
 * exists under that identifier. Reuses [EvidenceArtifactId] (Unit 1),
 * `EvidenceArtifactStorage.read` (Unit 1, Phase 2, unmodified), and the
 * existing [PermissionEngine] interface unmodified -- no new storage
 * primitive, no new identity type, and no new Permission Engine contract
 * is introduced anywhere in this Unit.
 *
 * ## Why no new storage abstraction is required
 *
 * `EvidenceArtifactStorage.read` already returns exactly what this
 * operation needs to return -- the exact bytes written for an identifier,
 * or `null` if none exist -- and already guarantees a freshly-allocated
 * `ByteArray` on every call (Unit 1's own documented byte-ownership
 * guarantee, verified by that Unit's own test suite). That method's KDoc
 * disclaims only that it is *itself* "not the governed, caller-facing
 * retrieval API" -- language identifying exactly the gap this Unit closes,
 * not a gap in the read primitive's own behaviour. This Unit therefore adds
 * a governed *wrapper* around an already-correct, already-tested read
 * primitive, never a second way of reading bytes off disk.
 *
 * ## Why no Memory Core or provenance work is required
 *
 * The Implementation Plan's own Traceability table (Section 8) lists
 * Provenance integration as Phase 5, sequenced strictly after this Phase
 * (Phase 4) -- retrieval's own dependency row names only the Constitution's
 * no-bypass principle and Contract Design Section 6.6/Scope Lock Section 7,
 * neither of which mentions Memory Core. Contract Design Section 6.2 keeps
 * Provenance exclusively Memory Core's own contract regardless; reading a
 * custodied artefact's bytes back does not touch, require, or imply
 * anything about whatever Provenance record Memory Core may separately
 * hold for it.
 *
 * ## Why retrieval is observational only
 *
 * [parker.core.runtime.DefaultEvidenceCustodian.retrieve] calls exactly two
 * methods: [PermissionEngine.evaluate] (itself read-only against Identity
 * Service/policy state) and `EvidenceArtifactStorage.read` (Unit 1's own
 * read-only primitive). No code path in this Unit calls
 * `EvidenceArtifactStorage.write`; no identifier is minted -- the caller
 * supplies an already-existing [EvidenceArtifactId], exactly as retrieval
 * requires one to already exist rather than creating one; no field of
 * [AcceptedEvidenceArtifact] or any other acceptance-side state is ever
 * read, written, or referenced by this Unit's own retrieval path; and no
 * Memory Core, Knowledge Memory, or provenance call appears anywhere in
 * this Unit's implementation. [EvidenceRetrievalResult.Found] carries only
 * the identifier the caller already supplied and the bytes
 * `EvidenceArtifactStorage.read` already returned -- it asserts no new
 * fact this Unit did not already have on hand from its own two calls.
 *
 * ## `EvidenceRetrievalResult`, mirroring `EvidenceAcceptanceResult`'s own
 * non-exceptional-outcome shape, with one addition: a genuine storage
 * fault still propagates as a thrown exception
 *
 * A denied Permission Engine decision remains an ordinary, expected
 * outcome -- not a fault -- exactly as Unit 2 already established, so it
 * is returned as [EvidenceRetrievalResult.Rejected], never thrown. A
 * missing artefact ("authorised, but nothing exists under this identifier")
 * is equally an ordinary, expected outcome, not a fault, so it is returned
 * as [EvidenceRetrievalResult.NotFound], never thrown or conflated with
 * [Rejected]. A genuine storage fault (`EvidenceArtifactStorageException`,
 * any subtype -- for example `StorageIOFailure` or an `UnsafeIdentifier`
 * raised by a corrupted or hand-edited identifier reaching storage) is a
 * different kind of outcome, exactly as Unit 2 already distinguished for
 * `accept`, and is deliberately not folded into this same result type: it
 * propagates unchanged, uncaught, per this Unit's own "do not hide genuine
 * storage failures as `NotFound`" instruction. No document reviewed for
 * this Unit requires a different behaviour.
 *
 * ## One disclosed convention this Unit's own `retrieve` implementation
 * depends on, without resolving how it gets wired in production
 *
 * [parker.core.runtime.DefaultEvidenceCustodian] constructs an
 * `ExecutionRequest` naming a second fixed, well-known `ResourceId`
 * (`"evidence-custodian-retrieval"`) and a second fixed proposed-action
 * name (`"evidence.retrieve"`) -- distinct literal values from Unit 2's
 * own acceptance conventions, defined once as named constants and never
 * duplicated as a second literal anywhere else in this Unit's own code or
 * tests. Neither is registered anywhere by this Unit -- no call to
 * `ResourceRegistry.register` or an `ActionVocabulary` registration exists
 * in this Unit's own code, since registering either is Implementation Plan
 * Phase 10 ("Runtime integration") work, explicitly excluded from this
 * Unit. A production [PermissionEngine] (`DefaultPermissionEngine` backed
 * by `DefaultPermissionPolicy`) will resolve neither name to anything until
 * that future Phase 10 Unit registers a Resource under this identifier and
 * an `ActionVocabulary` entry mapping this action name to a resolvable
 * `(PermissionAction, ResourceType)` pair -- until then, a real
 * `DefaultPermissionPolicy` would deny every retrieval request through this
 * path (its own documented "Unknown Action"/"Unknown Resource" ->
 * `DENIED` behaviour), exactly the same safe, conservative failure mode
 * Unit 2's own acceptance path is already in today, not a new or different
 * risk this Unit introduces. This Unit's own tests exercise the
 * *orchestration* (`FakePermissionEngine`, exactly as Unit 2's own tests
 * already do) rather than a real policy, for the same reason.
 */

/**
 * What [EvidenceCustodian.retrieve] returns -- an explicit, non-exceptional
 * outcome for the two ordinary cases (denied, not found), with a genuine
 * storage fault deliberately left outside this type and instead thrown
 * (this file's own Unit 3 KDoc explains why).
 */
sealed class EvidenceRetrievalResult {

    /**
     * Retrieval was authorised and an artefact exists under
     * [evidenceArtifactId]. [content] is exactly what
     * [parker.core.runtime.DefaultEvidenceCustodian] received back from
     * `EvidenceArtifactStorage.read` for this identifier -- no
     * transformation, re-encoding, or modification of any kind.
     *
     * Not a `data class`, for the same reason [CandidateEvidenceArtifact]
     * is not: a Kotlin `data class`'s auto-generated `equals`/`hashCode`
     * would compare [content] by reference, not by value. [equals] and
     * [hashCode] are overridden here to compare [content] structurally
     * instead.
     */
    class Found(val evidenceArtifactId: EvidenceArtifactId, val content: ByteArray) : EvidenceRetrievalResult() {

        override fun equals(other: Any?): Boolean =
            other is Found && evidenceArtifactId == other.evidenceArtifactId && content.contentEquals(other.content)

        override fun hashCode(): Int = 31 * evidenceArtifactId.hashCode() + content.contentHashCode()

        override fun toString(): String =
            "Found(evidenceArtifactId=$evidenceArtifactId, content=<${content.size} bytes>)"
    }

    /**
     * Retrieval was authorised, but no artefact exists under
     * [evidenceArtifactId] -- distinct from [Rejected] (an authorisation
     * outcome) and from a thrown `EvidenceArtifactStorageException` (a
     * genuine fault). Carries the identifier that was requested so a
     * caller can correlate this result with its own request without this
     * type needing to echo anything else about it.
     */
    data class NotFound(val evidenceArtifactId: EvidenceArtifactId) : EvidenceRetrievalResult()

    /**
     * Retrieval was not authorised. Carries the identifier that was
     * requested -- exactly as [Found] and [NotFound] already do -- so a
     * denied outcome remains individually identifiable and externally
     * disclosable alongside any other outcome from the same or a
     * different invocation, per Evidence Artifact Contract Design §6.3's
     * own "every retrieval outcome preserves the identity of the artefact
     * requested, including a denied one" requirement. [evidenceArtifactId]
     * echoes only the identifier the requesting caller already supplied to
     * make this request -- it grants no access to the artefact, discloses
     * no evidence content, and creates no custody, retrieval, deletion,
     * acceptance, storage, or ownership authority of any kind (Contract
     * Design §6.3). [reason] is a plain-language explanation, never a
     * caller-facing policy justification this Unit has no basis to
     * construct -- see [parker.core.runtime.DefaultEvidenceCustodian]'s
     * own KDoc for exactly what it contains.
     */
    data class Rejected(val evidenceArtifactId: EvidenceArtifactId, val reason: String) : EvidenceRetrievalResult() {
        init {
            require(reason.isNotBlank()) { "EvidenceRetrievalResult.Rejected.reason must not be blank" }
        }
    }
}

/**
 * The governed acceptance and retrieval boundary itself. Two operations on
 * this interface so far -- [accept] (Unit 2, Phase 3) and [retrieve] (Unit
 * 3, Phase 4) -- with [parker.core.runtime.DefaultEvidenceCustodian] as the
 * sole implementation of both. No implementation of this interface may
 * accept evidence, or grant read access to it, implicitly, silently, or as
 * a side effect of any other operation (Contract Design Section 4's own
 * "Accepting evidence into custody"/"Supporting authorised read access"
 * requirements, restated here as this interface's own contract).
 */
interface EvidenceCustodian {
    suspend fun accept(
        requestingPrincipalId: PrincipalId,
        candidate: CandidateEvidenceArtifact,
    ): EvidenceAcceptanceResult

    /**
     * Authorised, observational read access to a previously accepted
     * artefact. Never mints or reassigns an [EvidenceArtifactId]; never
     * alters acceptance state; never writes, replaces, deletes, promotes,
     * classifies, hashes, or annotates anything (this file's own Unit 3
     * KDoc, "Why retrieval is observational only").
     */
    suspend fun retrieve(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): EvidenceRetrievalResult

    /**
     * Document Ingestion, Authoritative Source Manifest Foundation
     * Implementation. Authorised, observational read access to the
     * Authoritative Evidence Source Manifest [EvidenceArtifactId] names,
     * governed by
     * `docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
     * ("the Scope Lock") Section 14, option B -- a second, narrow,
     * read-only operation added directly to this interface, mirroring
     * how [accept] and [retrieve] were themselves incrementally added
     * across prior Units, rather than as a new interface: no
     * authority-narrowing reason (the kind that justified splitting
     * [OwnerEvidenceDeletionAuthority] out entirely) applies to a
     * read-only manifest fact ordinary retrieval callers may also
     * safely hold.
     *
     * Never mints, mutates, deletes, or rewrites a manifest; never lists,
     * searches, or returns "the latest" of anything; never retrieves
     * source bytes or derivative data as a side effect (Scope Lock
     * Section 12/14). A legacy artefact admitted before this
     * capability existed, or missing bytes entirely, truthfully returns
     * [EvidenceManifestRetrievalResult.NotFound] -- never a fabricated
     * or inferred manifest (Scope Lock Section 13).
     */
    suspend fun retrieveManifest(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): EvidenceManifestRetrievalResult
}

/**
 * Evidence Custodian, Implementation Plan Phase 7 ("Deletion workflow").
 * Governed in full by
 * `docs/architecture/EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification"), which this Unit implements exactly --
 * no responsibility, exclusion, or ordering rule beyond what that
 * document already fixed. See the Boundary Clarification's own Sections
 * 3-7 for the full rationale; this file's KDoc restates only what is
 * specific to the types themselves.
 *
 * ## Deliberately not part of [EvidenceCustodian] (Boundary Clarification
 * Section 3, Determination 3)
 *
 * [EvidenceCustodian] is unchanged by this Unit -- it still declares
 * exactly `accept` and `retrieve`. Deletion is not "an ordinary
 * operation available through the general Evidence Custodian capability
 * used by routine consumers": anything holding only an
 * [EvidenceCustodian]-typed reference (a retrieval caller, a future
 * acceptance caller) has no way to reach deletion, because the type
 * itself does not offer it. [OwnerEvidenceDeletionAuthority] is a wholly
 * separate interface, implemented by a separate class
 * ([parker.core.runtime.DefaultOwnerEvidenceDeletionAuthority]), never
 * by [parker.core.runtime.DefaultEvidenceCustodian].
 */
sealed class EvidenceDeletionResult {

    /**
     * Deletion was authorised, physically completed, and durably audited
     * -- returned only once both a durable `AUTHORISED` and a durable
     * `COMPLETED` [parker.core.interfaces.EvidenceDeletionAuditRecord]
     * exist (Boundary Clarification Section 6). A caller can never
     * observe [Deleted] for an attempt whose completion was not itself
     * durably recorded.
     */
    data class Deleted(val evidenceArtifactId: EvidenceArtifactId) : EvidenceDeletionResult()

    /**
     * Deletion was authorised, but nothing was present under
     * [evidenceArtifactId] -- covers both "never existed" and
     * "already deleted" identically (Determination 2: no tombstone
     * distinguishes the two).
     */
    data class NotFound(val evidenceArtifactId: EvidenceArtifactId) : EvidenceDeletionResult()

    /**
     * Deletion was not authorised. [reason] is a plain-language
     * explanation, mirroring [EvidenceAcceptanceResult.Rejected]'s own
     * convention exactly.
     */
    data class Rejected(val reason: String) : EvidenceDeletionResult() {
        init {
            require(reason.isNotBlank()) { "EvidenceDeletionResult.Rejected.reason must not be blank" }
        }
    }
}

/**
 * The governed, structurally owner-only deletion boundary. Declares
 * exactly one operation, and that operation carries no `reason`/
 * `justification` parameter of any kind -- there is no code path by
 * which a caller can label a deletion "optimisation," "cleanup," or
 * "storage pressure" and have it processed differently from an ordinary
 * authorised request (Boundary Clarification Section 4, "no `reason`
 * parameter anywhere in `deleteAsOwner`'s own signature"). Which
 * component in a future production runtime is ever given a reference to
 * an implementation of this interface is deliberately not decided here
 * -- that is Implementation Plan Phase 10 ("Runtime integration") work
 * (Boundary Clarification Section 5); this Unit's own obligation is only
 * to make the type narrow enough that Phase 10 has no choice but to wire
 * it narrowly.
 */
interface OwnerEvidenceDeletionAuthority {
    suspend fun deleteAsOwner(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): EvidenceDeletionResult
}
