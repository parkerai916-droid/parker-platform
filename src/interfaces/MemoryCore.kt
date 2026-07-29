package parker.core.interfaces

import java.time.Instant

/**
 * Programme 2, Memory Core, Implementation Unit 1 (Shared identifiers and
 * enumerations). Opens `src/interfaces/MemoryCore.kt`, following exactly
 * the "one file per subsystem" precedent [KnowledgeStore] and [WorldModel]
 * already established -- every Memory Core contract
 * `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` (the Contract
 * Design) approved, and every rule
 * `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` (the Scope Lock) froze,
 * is added to this one file across this Programme's nine implementation
 * units (`docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md`, the
 * Implementation Plan, Section 4).
 *
 * This Unit adds only what every later Unit in this file depends on:
 * five identifier types (one per Memory Core record kind) and two
 * enumerations (the shared record lifecycle status, and Provenance's own
 * content-nature classification) -- nothing else. No record type, no
 * interface, and no field beyond these is added by this Unit; those
 * begin with Unit 2 (`Provenance`).
 */

/**
 * Identifies one `Entity` record across its entire lifecycle. Assigned
 * once, by Memory Core, at creation; never reassigned or reused --
 * including for an Entity later transitioned to
 * [MemoryCoreRecordStatus.DELETED], so an audit trail can still name it
 * (Contract Design Section 4; Scope Lock Section 11's own "stable
 * identifiers" performance expectation). Follows the same established
 * identifier pattern every other long-lived Parker object already uses
 * ([PrincipalId], [ResourceId], [KnowledgeId], [ConversationId],
 * [AgentRunId]): a single, blank-rejecting `String` value, validated at
 * construction, with no behaviour beyond identity.
 */
@JvmInline
value class EntityId(val value: String) {
    init {
        require(value.isNotBlank()) { "EntityId must not be blank" }
    }
}

/**
 * Identifies one `Document` registration record across its entire
 * lifecycle. Same shape and same assignment discipline as [EntityId].
 */
@JvmInline
value class DocumentId(val value: String) {
    init {
        require(value.isNotBlank()) { "DocumentId must not be blank" }
    }
}

/**
 * Identifies one `Assertion` record across its entire lifecycle. Same
 * shape and same assignment discipline as [EntityId].
 */
@JvmInline
value class AssertionId(val value: String) {
    init {
        require(value.isNotBlank()) { "AssertionId must not be blank" }
    }
}

/**
 * Identifies one `Provenance` record across its entire lifecycle. Same
 * shape and same assignment discipline as [EntityId]. Every `Entity`,
 * `Document`, `Assertion`, and `Relationship` record carries a
 * mandatory, non-optional reference of this type (Contract Design
 * Section 3's cross-cutting decision; Scope Lock Section 7) -- no Memory
 * Core record of any of those four kinds may exist without one.
 */
@JvmInline
value class ProvenanceId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProvenanceId must not be blank" }
    }
}

/**
 * Identifies one `Relationship` record across its entire lifecycle. Same
 * shape and same assignment discipline as [EntityId].
 */
@JvmInline
value class RelationshipId(val value: String) {
    init {
        require(value.isNotBlank()) { "RelationshipId must not be blank" }
    }
}

/**
 * The lifecycle status shared identically by every Memory Core record
 * type (`Entity`, `Document`, `Assertion`, `Relationship`) -- one shared
 * type, not four near-duplicate ones (Contract Design Section 11;
 * frozen, without change, by Scope Lock Section 8).
 *
 * Valid transitions, enforced by `MemoryCore`'s own implementation
 * (Implementation Unit 8), never by this enum itself:
 *
 * ```
 * ACTIVE <-> DISPUTED -> SUPERSEDED
 * ACTIVE, DISPUTED, and SUPERSEDED may each transition to ARCHIVED.
 * ARCHIVED may transition back to whichever status preceded it
 *   (archival is reversible).
 * Any status may transition to DELETED. DELETED is terminal --
 *   no transition out of it exists.
 * ```
 *
 * [DELETED] is reserved exclusively for the owner-requested erasure path
 * (Scope Lock Section 6, `PermissionAction.DELETE`) -- no ordinary
 * correction, dispute, or supersession act ever produces it.
 */
enum class MemoryCoreRecordStatus {
    ACTIVE,
    DISPUTED,
    SUPERSEDED,
    ARCHIVED,
    DELETED,
}

/**
 * `Provenance`'s own required classification of what kind of content a
 * record's payload represents (Contract Design Section 7). Five values,
 * not four -- [UNKNOWN] is an explicit, first-class, valid value, never
 * a placeholder to avoid: a submission that genuinely cannot say which
 * of the first four applies states [UNKNOWN] outright. This is the
 * concrete mechanism satisfying the Scope Lock's own "unknown values
 * must remain explicit" requirement (Section 7) -- [UNKNOWN] must never
 * be silently defaulted to [ORIGINAL], and no future implementation unit
 * may introduce a default value for this field to fill a gap in a
 * submission.
 */
enum class ContentNature {
    ORIGINAL,
    EXTRACTED,
    SUMMARISED,
    INFERRED,
    UNKNOWN,
}

/**
 * Programme 2, Memory Core, Implementation Unit 2. The mandatory,
 * first-class origin record every other Memory Core record type must
 * reference (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, the
 * Contract Design, Section 7; frozen without change by
 * `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, the Scope Lock,
 * Section 7). This Unit implements [Provenance] and its own
 * construction-time validation only -- no `Entity`, `Document`,
 * `Assertion`, or `Relationship` record type exists yet (each of those
 * carries a mandatory [ProvenanceId] reference to an instance of this
 * type, added by later Units); no `MemoryCore`/`MemoryRetrieval`
 * interface, runtime implementation, event publication, or Runtime
 * composition exists yet either.
 *
 * ## Mandatory fields
 *
 * Never null; a construction attempt omitting any of these fails
 * outright (Scope Lock Section 7, "Mandatory Provenance fields"):
 * [provenanceId], [sourceIdentifier], [sourceType], [acquisitionTime],
 * [ingestionTime], [contentNature].
 *
 * ## Fields that may be unknown
 *
 * Genuinely nullable; a `null` value means "not known," never
 * "not applicable" and never a placeholder standing in for a value a
 * caller forgot to supply (Scope Lock Section 7, "Fields that may be
 * unknown"): [creator], [creatorPrincipalId], [claimedCreationTime],
 * [extractedFrom], [integrityInformation], [confidence], [sensitivity].
 * [derivedFrom] and [processingHistory] default to an empty list rather
 * than `null`, since an empty list already, unambiguously, expresses
 * "nothing to reference/record yet" without needing a second, redundant
 * absence state.
 *
 * ## `contentNature` carries no default value, deliberately
 *
 * [contentNature] is a required constructor parameter with **no Kotlin
 * default** -- not even [ContentNature.UNKNOWN]. This is a stronger
 * reading of Scope Lock Section 7's "unknown values must remain
 * explicit" than defaulting the parameter to `UNKNOWN` would be: a
 * Kotlin default is easy for a caller to rely on without a conscious
 * choice, whereas a mandatory parameter forces every caller, every
 * time, to decide -- including consciously writing
 * [ContentNature.UNKNOWN] when that is genuinely the caller's own
 * state. No future implementation unit may add a default value to this
 * parameter (Scope Lock Section 7's own explicit prohibition).
 *
 * ## Sensitivity
 *
 * [sensitivity] reuses [ResourceSensitivity]
 * (`src/contracts/Resource.kt`'s existing nine-value classification,
 * already governing every other Resource in this repository) rather
 * than a Memory-Core-specific scheme, per the Contract Design's own
 * Section 7 recommendation -- directly resolving the sensitivity-model
 * mismatch the Governance Review left open. Left nullable: `null` means
 * "not yet classified," and must be treated at least as protectively as
 * the most sensitive defined value by any future Permission Engine
 * integration, never as equivalent to [ResourceSensitivity.PUBLIC].
 * Whether [ResourceSensitivity] should itself gain an explicit
 * `UNCLASSIFIED` value remains an open question the Contract Design
 * (Section 19) already deferred to a future implementation or Scope
 * Lock decision -- not resolved, and not addressed, by this Unit.
 *
 * ## What this Unit does not implement
 *
 * No persistence, no database, no document parsing, no OCR, and no
 * hidden behaviour of any kind -- [Provenance] is a plain, immutable
 * data holder with construction-time validation only. It is never
 * constructed, stored, or retrieved by anything in this file yet; that
 * begins with the `MemoryCore`/`InMemoryMemoryCore` Units still to come.
 */
data class Provenance(
    val provenanceId: ProvenanceId,
    val sourceIdentifier: String,
    val sourceType: String,
    val acquisitionTime: Instant,
    val ingestionTime: Instant,
    val contentNature: ContentNature,
    val creator: String? = null,
    val creatorPrincipalId: PrincipalId? = null,
    val claimedCreationTime: Instant? = null,
    val derivedFrom: List<ProvenanceId> = emptyList(),
    val extractedFrom: DocumentId? = null,
    val processingHistory: List<String> = emptyList(),
    val integrityInformation: String? = null,
    val confidence: Double? = null,
    val sensitivity: ResourceSensitivity? = null,
) {
    init {
        require(sourceIdentifier.isNotBlank()) { "Provenance.sourceIdentifier must not be blank" }
        require(sourceType.isNotBlank()) { "Provenance.sourceType must not be blank" }
        require(creator == null || creator.isNotBlank()) {
            "Provenance.creator must not be blank if present -- omit it (null) instead to express an unknown creator"
        }
        require(integrityInformation == null || integrityInformation.isNotBlank()) {
            "Provenance.integrityInformation must not be blank if present"
        }
        if (confidence != null) {
            require(confidence in 0.0..1.0) {
                "Provenance.confidence must be between 0.0 and 1.0, was $confidence"
            }
        }
    }
}

/**
 * Programme 2, Memory Core, Implementation Unit 3. Identifies a durable
 * "who/what" Parker has encountered -- the addressable subject a
 * `Document`, `Assertion`, or `Relationship` can refer to (Contract
 * Design Section 4). This Unit implements [Entity] and its own
 * construction-time validation only -- no `Document`, `Assertion`, or
 * `Relationship` record type exists yet; no `MemoryCore`/`MemoryRetrieval`
 * interface, runtime implementation, event publication, or Runtime
 * composition exists yet either.
 *
 * [Entity] carries no belief about the entity beyond its own bare
 * identity and classification -- whatever Parker has learned *about* an
 * Entity lives in `Assertion`s and `Relationship`s that reference it
 * (added by later Units), never as a field appended directly here.
 *
 * ## What this type intentionally does not carry
 *
 * Per Contract Design Section 4's own closing list, checked here against
 * the actual implementation, not merely asserted: no relationship to
 * another record (`Relationship`, Unit 6, owns that); no sensitivity
 * classification of its own (inherited from [provenanceId]'s own
 * [Provenance.sensitivity], per Section 3's cross-cutting decision); no
 * belief, confidence, or truth-status field (that is `Assertion`'s job,
 * never `Entity`'s); no embeddings or ranking signal.
 *
 * ## Aliases are additive-only -- a future write-path guarantee, not a
 * property of this immutable type today
 *
 * [aliases] is a plain, immutable `List<String>` here, exactly like
 * every other field on this data class. "Additive-only" (an existing
 * alias is never removed or rewritten, Contract Design Section 12) is a
 * behavioural guarantee `MemoryCore`'s future amendment operations
 * (Unit 8) must uphold when producing a *new* `Entity` value from an
 * old one -- it is not, and cannot be, a property this Unit's plain data
 * type enforces on its own, since a `data class` has no notion of "the
 * previous version of this same record" to compare itself against.
 *
 * ## Status
 *
 * [status] defaults to [MemoryCoreRecordStatus.ACTIVE], reflecting
 * Contract Design Section 11's own "the initial resting state, entered
 * immediately at creation" rule for a genuinely new record -- this is a
 * single, deterministic, always-correct default for that one case, not
 * a fabricated-uncertainty default the way a defaulted [ContentNature]
 * would have been on [Provenance]. Every other [MemoryCoreRecordStatus]
 * value remains directly constructible, since this same type is also
 * what a future `MemoryRetrieval` read returns for an already-existing
 * record in any status.
 *
 * @param createdAt Deliberately carries no default value (unlike
 *   [status]), consistent with [Provenance]'s own identical treatment of
 *   its timestamps and with Scope Lock Section 11's determinism
 *   requirement -- a data class must not silently read the system clock.
 */
data class Entity(
    val entityId: EntityId,
    val entityType: String,
    val primaryLabel: String,
    val provenanceId: ProvenanceId,
    val createdAt: Instant,
    val aliases: List<String> = emptyList(),
    val relatedPrincipalId: PrincipalId? = null,
    val status: MemoryCoreRecordStatus = MemoryCoreRecordStatus.ACTIVE,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(entityType.isNotBlank()) { "Entity.entityType must not be blank" }
        require(primaryLabel.isNotBlank()) { "Entity.primaryLabel must not be blank" }
        require(aliases.all { it.isNotBlank() }) { "Entity.aliases must not contain a blank alias" }
    }
}

/**
 * Whether anything external to Memory Core has processed a [Document]'s
 * own contents (Contract Design Section 5). Distinct from, and varies
 * independently of, [Document.status] (the shared record lifecycle,
 * Section 11) -- a Document can be [MemoryCoreRecordStatus.SUPERSEDED]
 * as a *record* while its processing status remains whatever it was,
 * and vice versa. Memory Core only ever records which of these five
 * values is currently true; it never performs, triggers, or interprets
 * the processing itself (Document Handling's own, later, separate
 * Programme boundary).
 */
enum class DocumentProcessingStatus {
    REGISTERED,
    PROCESSING_REQUESTED,
    PROCESSED_EXTERNALLY,
    PROCESSING_FAILED,
    NOT_APPLICABLE,
}

/**
 * Programme 2, Memory Core, Implementation Unit 4. Registers that a
 * source document exists, where it can be found, and what Memory Core
 * currently knows about its provenance and processing state --
 * **registration only** (Contract Design Section 5). This Unit
 * implements [Document] and its own construction-time validation only
 * -- no `Assertion` or `Relationship` record type exists yet; no
 * `MemoryCore`/`MemoryRetrieval` interface, runtime implementation,
 * event publication, or Runtime composition exists yet either.
 *
 * ## Registration only -- never parsed contents
 *
 * This type never represents a document's parsed contents, extracted
 * text, page structure, or any interpretation of what the document
 * says. Memory Core never fetches, opens, or validates
 * [locationReference]'s own reachability -- it is recorded, not
 * resolved. Nothing about this type's shape anticipates or reserves
 * space for document parsing, OCR, or image analysis beyond
 * [processingStatus], which only ever records *whether* something else
 * has acted on this Document, never *what* it found.
 *
 * ## Relationships, satisfied without an embedded field
 *
 * This Unit's own Contract Design brief lists "relationships" among
 * Document's required fields (Contract Design Section 5's own field
 * list). This is satisfied entirely through `Relationship` records
 * (Unit 6, not yet implemented) that name this Document as an endpoint
 * -- never through a field embedded on [Document] itself. Repeating an
 * embedded, untyped relationship list here would reintroduce exactly
 * the weakness the Governance Review found and criticised in today's
 * `KnowledgeRecord.relatedMemoryIds`, which `Relationship` was designed to
 * remove. Deliberately, no such field exists on this data class, and
 * `DocumentTest.kt`'s own structural test confirms it, rather than
 * merely asserting it in this comment.
 *
 * ## What this type intentionally does not carry
 *
 * No sensitivity classification of its own (inherited from
 * [provenanceId]'s own `Provenance.sensitivity`, per Section 3's
 * cross-cutting decision); no belief, confidence, or truth-status field
 * (`Assertion`'s job, never `Document`'s); no embeddings or ranking
 * signal; no parsed content of any kind.
 *
 * @param registeredAt Deliberately carries no default value, consistent
 *   with [Entity.createdAt]'s and [Provenance]'s own identical treatment
 *   of their timestamps -- a data class must not silently read the
 *   system clock (Scope Lock Section 11's determinism requirement).
 * @param processingStatus Defaults to [DocumentProcessingStatus.REGISTERED],
 *   the single, deterministic, always-correct state for a genuinely new
 *   registration -- mirroring [Entity.status]'s identical "safe default
 *   for the one real initial case" reasoning, not a fabricated-
 *   uncertainty default.
 */
data class Document(
    val documentId: DocumentId,
    val documentType: String,
    val locationReference: String,
    val provenanceId: ProvenanceId,
    val registeredAt: Instant,
    val integrityHash: String? = null,
    val processingStatus: DocumentProcessingStatus = DocumentProcessingStatus.REGISTERED,
    val status: MemoryCoreRecordStatus = MemoryCoreRecordStatus.ACTIVE,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(documentType.isNotBlank()) { "Document.documentType must not be blank" }
        require(locationReference.isNotBlank()) { "Document.locationReference must not be blank" }
        require(integrityHash == null || integrityHash.isNotBlank()) {
            "Document.integrityHash must not be blank if present"
        }
    }
}

/**
 * Programme 2, Memory Core, Implementation Unit 5. Records a claim.
 * **An Assertion is never automatically truth, and its own existence
 * never causes any other record's status to change** (Contract Design
 * Section 6) -- this is the one guarantee this type exists specifically
 * to protect, and `AssertionTest.kt`'s own tests check it directly, not
 * merely by asserting this comment is true. This Unit implements
 * [Assertion] and its own construction-time validation only -- no
 * `Relationship` record type exists yet; no
 * `MemoryCore`/`MemoryRetrieval` interface, runtime implementation,
 * event publication, or Runtime composition exists yet either.
 *
 * ## No separate `source` field
 *
 * Per Section 3's cross-cutting decision (restated here because this is
 * the type it most directly resolves against this Unit's own Contract
 * Design brief's literal field list): [Assertion] carries no `source`
 * field of its own. [provenanceId] already answers "where did this
 * claim come from" more completely (source identifier, source type,
 * creator, three distinct timestamps) than a single bare `source` field
 * ever could -- adding a second, parallel origin field here would
 * reintroduce exactly the scattered-provenance-fields weakness the
 * Governance Review already found and criticised in today's
 * `KnowledgeRecord`.
 *
 * ## Supporting and contradicting references, satisfied without an
 * embedded field
 *
 * This Unit's own Contract Design brief lists "supporting references"
 * and "contradicting references" among Assertion's required fields.
 * Per Section 3's cross-cutting decision, both are satisfied by typed
 * `Relationship` records (Unit 6, not yet implemented --
 * `SUPPORTS`/`CONTRADICTS` relationship types) connecting an Assertion
 * to a Document, an Entity, or another Assertion -- never by two
 * parallel embedded list fields here. Deliberately, no such field
 * exists on this data class, and `AssertionTest.kt`'s own structural
 * test confirms it.
 *
 * ## No independent creation timestamp, unlike [Entity]/[Document]
 *
 * Unlike [Entity.createdAt] and [Document.registeredAt], [Assertion]
 * carries no timestamp field of its own. This is not an oversight --
 * Contract Design Section 6's own required-field list never named one,
 * and [provenanceId]'s own `Provenance.ingestionTime` already answers
 * "when was this recorded" without this type duplicating it. Adding one
 * "for consistency" with [Entity]/[Document] would be introducing a
 * field beyond what the Contract Design actually approved -- exactly
 * what this Unit's own instructions forbid.
 *
 * ## How this type keeps its own central promise
 *
 * Nothing in this shape gives an [Assertion] any path to mark another
 * record `DISPUTED`, `SUPERSEDED`, or otherwise altered as a side
 * effect of its own creation. Marking a *different* record disputed or
 * superseded because of an Assertion is always a second, separate,
 * explicit act on that other record (added by later Units) -- never
 * something constructing this type triggers on its own. An [Assertion]
 * existing, by itself, changes nothing about anything else Memory Core
 * holds.
 */
data class Assertion(
    val assertionId: AssertionId,
    val statement: String,
    val provenanceId: ProvenanceId,
    val confidence: Double? = null,
    val status: MemoryCoreRecordStatus = MemoryCoreRecordStatus.ACTIVE,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(statement.isNotBlank()) { "Assertion.statement must not be blank" }
        if (confidence != null) {
            require(confidence in 0.0..1.0) {
                "Assertion.confidence must be between 0.0 and 1.0, was $confidence"
            }
        }
    }
}

/**
 * One endpoint of a [Relationship] (Contract Design Section 8): a pair
 * of a [recordKind] tag and a [recordId] value, deliberately **not** a
 * structural reference to any specific record type. This is what lets
 * [Relationship] connect an [Entity], a [Document], an [Assertion], a
 * Conversation Turn, or a future Knowledge Memory record without
 * [Relationship] itself ever holding a compile-time dependency on any of
 * them -- in particular, never on Knowledge Memory's own, not-yet-designed
 * types, preserving the one-directional dependency the Reconciliation and
 * Contract Design Section 2 both require. An endpoint of a kind Memory
 * Core does not own (`conversation-turn`, `knowledge-record`) is accepted
 * and stored by identifier and kind tag alone -- the same
 * loose-coupling-by-identifier pattern `correlationId` already uses
 * throughout this repository -- never verified against that external
 * subsystem's own state.
 *
 * [recordId] deliberately carries a plain `String`, not one of this
 * file's own identifier value classes ([EntityId], [DocumentId], ...):
 * an endpoint may reference a record kind this file does not, and must
 * not, know the identifier shape of (a Conversation `TurnId`, a future
 * Knowledge Memory identifier).
 */
data class RelationshipEndpoint(
    val recordKind: String,
    val recordId: String,
) {
    init {
        require(recordKind.isNotBlank()) { "RelationshipEndpoint.recordKind must not be blank" }
        require(recordId.isNotBlank()) { "RelationshipEndpoint.recordId must not be blank" }
    }

    /**
     * The record kinds Contract Design Section 8 names -- `entity`,
     * `document`, `assertion`, `relationship`, `conversation-turn`,
     * `knowledge-record`. Named constants only, for discoverability and
     * to reduce typo risk; [recordKind] itself remains a plain, open
     * `String` field, never a closed enumeration (Contract Design
     * Section 8's own "extensible to future kinds" requirement) -- a
     * caller remains free to supply a kind not named here.
     */
    companion object {
        const val ENTITY = "entity"
        const val DOCUMENT = "document"
        const val ASSERTION = "assertion"
        const val RELATIONSHIP = "relationship"
        const val CONVERSATION_TURN = "conversation-turn"
        const val KNOWLEDGE_RECORD = "knowledge-record"
    }
}

/**
 * Programme 2, Memory Core, Implementation Unit 6. The one, single
 * mechanism connecting any two Memory-Core-addressable records -- and,
 * by identifier only, records outside Memory Core entirely (Contract
 * Design Section 8). This Unit implements [Relationship] and
 * [RelationshipEndpoint]'s own construction-time validation only -- no
 * `MemoryCore`/`MemoryRetrieval` interface, runtime implementation,
 * event publication, or Runtime composition exists yet.
 *
 * No other contract in this file carries its own embedded reference to
 * another record ([Entity.aliases] excepted, which references nothing
 * beyond itself) -- every connection Memory Core needs (`Document`'s own
 * "relationships," `Assertion`'s own "supporting"/"contradicting"
 * references) is expressed here instead, per Section 3's cross-cutting
 * decision, already stated on [Entity]/[Document]/[Assertion]'s own
 * KDoc and confirmed here from the connecting type's own side.
 *
 * ## Referential integrity -- deferred to a future Unit, not enforced here
 *
 * Contract Design Section 8 requires referential integrity for endpoint
 * kinds Memory Core itself owns and can verify ([RelationshipEndpoint.ENTITY],
 * [RelationshipEndpoint.DOCUMENT], [RelationshipEndpoint.ASSERTION],
 * [RelationshipEndpoint.RELATIONSHIP]) -- a Relationship naming a
 * nonexistent record of one of those four kinds must be rejected at
 * creation. **This Unit's plain data type cannot perform that check on
 * its own** -- it has no access to a store to verify existence against,
 * exactly the same situation [Entity]'s own "additive-only aliases" and
 * "assigned by Memory Core" KDoc already disclosed. That verification is
 * `InMemoryMemoryCore`'s own responsibility (a future Unit), not this
 * one. What this Unit **can**, and does, verify at construction -- since
 * it needs no store to check -- is that [fromEndpoint] and [toEndpoint]
 * are not identical (Contract Design Section 14, "invalid relationships
 * ... an endpoint referencing itself").
 *
 * ## Recognised relationship types
 *
 * [relationshipType] is a plain, open `String`, **not** a closed
 * enumeration (Contract Design Section 8's own explicit instruction:
 * "Design for extensibility"). A small set of **recognised** values is
 * named as companion constants below, since Memory Core's own future
 * retrieval and lifecycle logic treats them specially; any other,
 * caller-defined value is accepted and stored, but carries no special
 * interpretation.
 */
data class Relationship(
    val relationshipId: RelationshipId,
    val relationshipType: String,
    val fromEndpoint: RelationshipEndpoint,
    val toEndpoint: RelationshipEndpoint,
    val directional: Boolean,
    val provenanceId: ProvenanceId,
    val createdAt: Instant,
    val status: MemoryCoreRecordStatus = MemoryCoreRecordStatus.ACTIVE,
) {
    init {
        require(relationshipType.isNotBlank()) { "Relationship.relationshipType must not be blank" }
        require(fromEndpoint != toEndpoint) {
            "Relationship must not connect an endpoint to itself (fromEndpoint == toEndpoint == $fromEndpoint)"
        }
    }

    /**
     * The seven recognised relationship-type names Contract Design
     * Section 8 lists (eight distinct string values across seven named
     * bullet points -- `SUPPORTS`/`CONTRADICTS` share one bullet but are
     * two separate values). Named constants only, for discoverability
     * and to reduce typo risk; [relationshipType] itself remains open,
     * never closed, per this type's own KDoc above.
     */
    companion object {
        const val SUPPORTS = "supports"
        const val CONTRADICTS = "contradicts"
        const val AMENDS = "amends"
        const val SUPERSEDES = "supersedes"
        const val DISPUTES = "disputes"
        const val SAME_AS = "same_as"
        const val EXTRACTED_FROM = "extracted_from"
        const val REFERENCES = "references"
    }
}

/**
 * Programme 2, Memory Core, Implementation Unit 7. Adds the two public
 * interfaces every earlier Unit's record types existed to serve --
 * [MemoryCore] (the single write surface) and [MemoryRetrieval] (the
 * single structural read surface) -- plus the supporting candidate and
 * query/result types those two interfaces directly require. This Unit
 * implements interface shape only: no `InMemoryMemoryCore`, no
 * persistence, no referential-integrity enforcement, no event
 * publication, no permission decorator, and no runtime composition exist
 * yet (each is a later Unit's own responsibility).
 *
 * ## Candidate types exist because of Contract Design Errata 002
 *
 * `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_002.md` resolved
 * a genuine contradiction this Unit exposed: every stored record type
 * above ([Entity], [Document], [Assertion], [Relationship], and
 * [Provenance] itself) requires its own identifier as a mandatory,
 * non-defaulted constructor parameter, while the Contract Design and
 * Implementation Plan both require Memory Core to mint every one of its
 * five record kinds' identifiers internally, "never accepted from a
 * caller." [CandidateProvenance], [CandidateEntity], [CandidateDocument],
 * [CandidateAssertion], and [CandidateRelationship] are the errata's
 * resolution: a caller submits one of these five ID-less shapes; the
 * corresponding [MemoryCore] operation mints the identifier and any other
 * Memory-Core-owned field internally, and returns the completed, stored
 * record. This mirrors the repository's own existing
 * `CandidateKnowledge -> KnowledgeStore.remember -> KnowledgeRecord` precedent
 * (`src/interfaces/KnowledgeStore.kt`), extended here to Memory Core's five
 * record kinds.
 *
 * Each candidate type carries exactly its stored counterpart's
 * caller-supplied fields -- never a Memory-Core-owned field (the
 * record's own identifier; any Memory-Core-assigned timestamp such as
 * `createdAt`/`registeredAt`/`ingestionTime`; any Memory-Core-assigned
 * initial lifecycle `status`), and never a convenience field the stored
 * type does not itself carry. Errata 002's own field-by-field tables are
 * this Unit's authoritative source; this file's KDoc on each candidate
 * type restates only what that document already decided.
 *
 * ## `transitionStatus`, added by a same-Unit amendment
 *
 * Unit 7 as originally scoped specified the write flow as exactly five
 * candidate-to-record creation operations, deliberately omitting a
 * status-transition operation rather than inferring one silently. That
 * gap was then resolved, explicitly, by
 * `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_003.md` --
 * [MemoryCore.transitionStatus] and [MemoryCoreRecordReference] are its
 * approved resolution, added here as a same-Unit amendment before Unit 8
 * began, precisely so `InMemoryMemoryCore` has a complete interface to
 * implement rather than one already known to be missing a Scope-Lock-
 * required capability.
 *
 * ## Retrieval: seven modes, ten methods
 *
 * [MemoryRetrieval] realises Contract Design Section 9's seven approved
 * structural retrieval modes -- identifier lookup, entity lookup,
 * document lookup, relationship traversal, chronological lookup,
 * metadata filtering, provenance-aware lookup -- as ten methods, since
 * identifier lookup is realised as four separate, type-safe methods
 * ([MemoryRetrieval.getEntity], [MemoryRetrieval.getDocument],
 * [MemoryRetrieval.getAssertion], [MemoryRetrieval.getRelationship])
 * rather than one method returning a common supertype. No semantic,
 * ranked, embedding-based, or inferred retrieval exists anywhere in this
 * interface, and no method accepts or evaluates a permission decision --
 * [requestingPrincipalId] is carried on every query for auditability
 * only, mirroring [KnowledgeQuery.requestingPrincipalId]'s own identical,
 * already-approved treatment; permission evaluation remains entirely
 * external to Memory Core (Contract Design Section 9's own disclosed
 * correction against today's `KnowledgeStore.retrieve` precedent).
 */

/**
 * One of Memory Core's own record kinds, wrapped so a single
 * [MemoryRetrieval] method can return a genuinely heterogeneous result
 * set for the three retrieval modes that are not restricted to one
 * record kind by their own query shape
 * ([MemoryRetrieval.findByTimeRange], [MemoryRetrieval.findByMetadata],
 * [MemoryRetrieval.findByProvenance]) -- without forcing [Entity],
 * [Document], [Assertion], and [Relationship] into an artificial shared
 * supertype of their own. [Provenance] deliberately has no case here:
 * none of the seven approved retrieval modes returns a bare [Provenance]
 * value directly (Contract Design Section 9) -- a [Provenance] is always
 * reached by dereferencing the `provenanceId` already held on one of the
 * four record kinds this type does wrap.
 */
sealed class MemoryCoreRecord {
    data class OfEntity(val entity: Entity) : MemoryCoreRecord()
    data class OfDocument(val document: Document) : MemoryCoreRecord()
    data class OfAssertion(val assertion: Assertion) : MemoryCoreRecord()
    data class OfRelationship(val relationship: Relationship) : MemoryCoreRecord()
}

/**
 * What a caller submits to [MemoryCore.createProvenance] -- every
 * caller-supplied field [Provenance] itself carries, and nothing else
 * (Contract Design Errata 002, Section 7). Excludes exactly
 * [Provenance.provenanceId] (Memory-Core-owned identity) and
 * [Provenance.ingestionTime] (Memory-Core-owned: the moment Memory Core
 * itself ingests this submission, structurally the same role as
 * [Entity.createdAt]/[Document.registeredAt] elsewhere -- a caller
 * cannot know this timestamp in advance of submitting, only Memory Core
 * can). Validation mirrors [Provenance]'s own constructor exactly, since
 * every validated field here is carried onto the stored record
 * unchanged.
 */
data class CandidateProvenance(
    val sourceIdentifier: String,
    val sourceType: String,
    val acquisitionTime: Instant,
    val contentNature: ContentNature,
    val creator: String? = null,
    val creatorPrincipalId: PrincipalId? = null,
    val claimedCreationTime: Instant? = null,
    val derivedFrom: List<ProvenanceId> = emptyList(),
    val extractedFrom: DocumentId? = null,
    val processingHistory: List<String> = emptyList(),
    val integrityInformation: String? = null,
    val confidence: Double? = null,
    val sensitivity: ResourceSensitivity? = null,
) {
    init {
        require(sourceIdentifier.isNotBlank()) { "CandidateProvenance.sourceIdentifier must not be blank" }
        require(sourceType.isNotBlank()) { "CandidateProvenance.sourceType must not be blank" }
        require(creator == null || creator.isNotBlank()) {
            "CandidateProvenance.creator must not be blank if present -- omit it (null) instead to express an unknown creator"
        }
        require(integrityInformation == null || integrityInformation.isNotBlank()) {
            "CandidateProvenance.integrityInformation must not be blank if present"
        }
        if (confidence != null) {
            require(confidence in 0.0..1.0) {
                "CandidateProvenance.confidence must be between 0.0 and 1.0, was $confidence"
            }
        }
    }
}

/**
 * What a caller submits to [MemoryCore.createEntity] -- every
 * caller-supplied field [Entity] itself carries, and nothing else
 * (Contract Design Errata 002, Section 4). Excludes exactly
 * [Entity.entityId] (Memory-Core-owned identity), [Entity.createdAt]
 * (Memory-Core-owned timestamp), and [Entity.status] (Memory-Core-owned:
 * every newly created Entity begins [MemoryCoreRecordStatus.ACTIVE]).
 */
data class CandidateEntity(
    val entityType: String,
    val primaryLabel: String,
    val provenanceId: ProvenanceId,
    val aliases: List<String> = emptyList(),
    val relatedPrincipalId: PrincipalId? = null,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(entityType.isNotBlank()) { "CandidateEntity.entityType must not be blank" }
        require(primaryLabel.isNotBlank()) { "CandidateEntity.primaryLabel must not be blank" }
        require(aliases.all { it.isNotBlank() }) { "CandidateEntity.aliases must not contain a blank alias" }
    }
}

/**
 * What a caller submits to [MemoryCore.registerDocument] -- every
 * caller-supplied field [Document] itself carries, and nothing else
 * (Contract Design Errata 002, Section 5). Excludes exactly
 * [Document.documentId] (Memory-Core-owned identity),
 * [Document.registeredAt] (Memory-Core-owned timestamp), and
 * [Document.status] (Memory-Core-owned: every newly registered Document
 * begins [MemoryCoreRecordStatus.ACTIVE]). [processingStatus] remains
 * caller-supplied here, per Errata 002's own disclosed reasoning: it
 * expresses the submitter's own knowledge or intent about external
 * processing, never the record's lifecycle -- a genuinely different
 * concept from [status], which is the one Memory Core alone governs.
 */
data class CandidateDocument(
    val documentType: String,
    val locationReference: String,
    val provenanceId: ProvenanceId,
    val integrityHash: String? = null,
    val processingStatus: DocumentProcessingStatus = DocumentProcessingStatus.REGISTERED,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(documentType.isNotBlank()) { "CandidateDocument.documentType must not be blank" }
        require(locationReference.isNotBlank()) { "CandidateDocument.locationReference must not be blank" }
        require(integrityHash == null || integrityHash.isNotBlank()) {
            "CandidateDocument.integrityHash must not be blank if present"
        }
    }
}

/**
 * What a caller submits to [MemoryCore.createAssertion] -- every
 * caller-supplied field [Assertion] itself carries, and nothing else
 * (Contract Design Errata 002, Section 6). Excludes exactly
 * [Assertion.assertionId] (Memory-Core-owned identity) and
 * [Assertion.status] (Memory-Core-owned: every newly created Assertion
 * begins [MemoryCoreRecordStatus.ACTIVE]). [Assertion] itself carries no
 * independent creation timestamp (Unit 5's own deliberate decision), so
 * none is excluded here on that basis either.
 */
data class CandidateAssertion(
    val statement: String,
    val provenanceId: ProvenanceId,
    val confidence: Double? = null,
    val metadata: Map<String, String> = emptyMap(),
) {
    init {
        require(statement.isNotBlank()) { "CandidateAssertion.statement must not be blank" }
        if (confidence != null) {
            require(confidence in 0.0..1.0) {
                "CandidateAssertion.confidence must be between 0.0 and 1.0, was $confidence"
            }
        }
    }
}

/**
 * What a caller submits to [MemoryCore.createRelationship] -- every
 * caller-supplied field [Relationship] itself carries, and nothing else
 * (Contract Design Errata 002, Section "CandidateRelationship").
 * Excludes exactly [Relationship.relationshipId] (Memory-Core-owned
 * identity), [Relationship.createdAt] (Memory-Core-owned timestamp), and
 * [Relationship.status] (Memory-Core-owned: every newly created
 * Relationship begins [MemoryCoreRecordStatus.ACTIVE]). [Relationship]
 * carries no metadata field (Errata 001), so none is excluded or carried
 * here either. The self-reference check
 * ([Relationship]'s own `fromEndpoint != toEndpoint` rule) is repeated
 * here, at submission time, since it needs no store access and there is
 * no reason to defer a check this type can already perform on its own to
 * [MemoryCore.createRelationship] instead.
 */
data class CandidateRelationship(
    val relationshipType: String,
    val fromEndpoint: RelationshipEndpoint,
    val toEndpoint: RelationshipEndpoint,
    val directional: Boolean,
    val provenanceId: ProvenanceId,
) {
    init {
        require(relationshipType.isNotBlank()) { "CandidateRelationship.relationshipType must not be blank" }
        require(fromEndpoint != toEndpoint) {
            "CandidateRelationship must not connect an endpoint to itself (fromEndpoint == toEndpoint == $fromEndpoint)"
        }
    }
}

/**
 * Programme 2, Memory Core, Implementation Unit 7 Amendment
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_003.md`).
 * Identifies the target of [MemoryCore.transitionStatus] -- a sealed,
 * compile-time-exhaustive wrapper over exactly the four record kinds
 * that carry [MemoryCoreRecordStatus] ([EntityId], [DocumentId],
 * [AssertionId], [RelationshipId]), deliberately **not** a loosely-typed
 * `(recordKind: String, recordId: String)` pair the way
 * [RelationshipEndpoint] is.
 *
 * [RelationshipEndpoint]'s loose, string-typed coupling exists for a
 * specific reason that does not apply here: a [Relationship] must be
 * able to name endpoints Memory Core does not own and has no compile-
 * time dependency on ([RelationshipEndpoint.CONVERSATION_TURN],
 * [RelationshipEndpoint.KNOWLEDGE_RECORD]). A lifecycle transition has
 * no such need -- it can only ever target one of Memory Core's own four
 * lifecycle-bearing record kinds, every one of which already has a
 * proper, validated identifier type. Reusing [RelationshipEndpoint]'s
 * loose typing here would discard type safety already available for no
 * benefit (Errata 003, Section 2.5).
 *
 * [ProvenanceId] deliberately has no case here: [Provenance] carries no
 * [MemoryCoreRecordStatus] field at all (Errata 003's own opening
 * correction) -- a lifecycle transition has nothing to do to a
 * [Provenance] record, and this type's exhaustiveness makes attempting
 * one a compile error rather than a runtime concern.
 */
sealed class MemoryCoreRecordReference {
    data class ToEntity(val entityId: EntityId) : MemoryCoreRecordReference()
    data class ToDocument(val documentId: DocumentId) : MemoryCoreRecordReference()
    data class ToAssertion(val assertionId: AssertionId) : MemoryCoreRecordReference()
    data class ToRelationship(val relationshipId: RelationshipId) : MemoryCoreRecordReference()
}

/**
 * Memory Core's single public write surface (Contract Design Section 3;
 * Scope Lock Section 4). Six operations: five create/register
 * operations, one per record kind, each accepting the ID-less candidate
 * shape that kind's own KDoc documents and returning the completed,
 * stored, identified record (Errata 002); and one lifecycle-transition
 * operation, [transitionStatus] (Errata 003). No operation here accepts
 * or evaluates a permission decision -- Memory Core never authorises
 * itself; permission evaluation is entirely external, applied by a
 * future decorator (`PermissionGatedMemoryCore`, not implemented by this
 * Unit) composed around an implementation of this interface, never
 * inside one.
 *
 * `registerDocument` deliberately keeps a distinct verb from the other
 * four `create*` operations: a [Document] records the *registration* of
 * an external artefact Memory Core did not itself originate, never the
 * *creation* of that artefact, mirroring [Document]'s own field name,
 * `registeredAt`, as opposed to `createdAt` on [Entity] and
 * [Relationship].
 *
 * ## `transitionStatus`
 *
 * One generic operation for every lifecycle change Scope Lock Section 8
 * requires (`ACTIVE<->DISPUTED->SUPERSEDED`; `ARCHIVED` reversible from
 * `ACTIVE`, `DISPUTED`, and `SUPERSEDED`; `DELETED` terminal, reachable
 * from any status) -- not four separate `dispute`/`archive`/
 * `supersede`/`delete` methods, per Errata 003's own analysis: the
 * frozen lifecycle is a closed, already-fully-specified table of valid
 * `(fromStatus, targetStatus)` pairs, and no transition in that table
 * needs information a single generic operation could not carry. The
 * *reason* for a transition (which `Assertion` disputes a record, which
 * new record supersedes it) is never a parameter here -- it is already
 * captured separately, by a `Relationship` record naming the
 * transitioned record as an endpoint, per Contract Design Section 3's
 * "no embedded relationship/source fields" decision.
 *
 * [targetStatus] carries no restriction of its own to which
 * [MemoryCoreRecordStatus] values are valid from a given record's
 * current status -- [MemoryCoreRecordStatus]'s own KDoc already states
 * transition validity is "enforced by MemoryCore's own implementation,
 * never by this enum itself." This interface's signature does not, and
 * must not, attempt to encode the transition graph in types; that
 * remains `InMemoryMemoryCore`'s own responsibility (a future Unit), an
 * invalid transition rejected there as a thrown `IllegalStateException`
 * (Errata 003, Section 2.7), never as a sealed result alongside a
 * legitimate outcome, since a state-precondition violation is not a
 * competing business decision.
 *
 * `DELETED` carries no special case here either -- "owner-erasure-only"
 * governs *who* may authorise a transition to `DELETED`, never *how* it
 * is structurally represented (Errata 003, Section 2.4). That
 * authorisation is entirely a Runtime concern, enforced before this
 * operation is ever invoked; from this interface's own vantage point,
 * `DELETED` is structurally just another valid target status.
 *
 * This operation never performs an amendment and never creates a
 * replacement record as a side effect -- Contract Design Section 12's
 * amendment process remains a caller-orchestrated sequence of
 * operations this interface already provides (create the replacement,
 * record an `AMENDS`-typed `Relationship`, and, where appropriate, call
 * this operation to transition the original to `SUPERSEDED`), never
 * something this operation performs on its own initiative.
 *
 * No implementation of this interface exists yet -- `InMemoryMemoryCore`
 * is a future Unit's own responsibility, including the referential-
 * integrity checks (Contract Design Section 8) and lifecycle-transition
 * rules (Contract Design Section 11, Errata 003) that a plain interface
 * has no body to enforce.
 *
 * ## `requestingPrincipalId`, added by Errata 004
 *
 * Every operation's leading parameter, added by
 * `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` to
 * resolve a genuine gap Unit 10 exposed: none of the five candidate
 * types, and no bare `MemoryCoreRecordReference`/`MemoryCoreRecordStatus`
 * pair, carries caller identity (deliberately -- Errata 002/003 both
 * keep those types content-only). A permission-gating decorator
 * (`PermissionGatedMemoryCore`, still not implemented by this amendment)
 * cannot ask "may this Principal perform this action" without a
 * Principal to name. [InMemoryMemoryCore] accepts and threads this
 * parameter through unchanged -- it does not evaluate permission,
 * filter, persist, or otherwise behave differently for one
 * [PrincipalId] versus another; it remains exactly as permission-
 * *neutral* as before, just no longer permission-*blind* (Errata 004,
 * Section 4).
 */
interface MemoryCore {
    suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance
    suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity
    suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document
    suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion
    suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship
    suspend fun transitionStatus(
        requestingPrincipalId: PrincipalId,
        reference: MemoryCoreRecordReference,
        targetStatus: MemoryCoreRecordStatus,
    ): MemoryCoreRecord
}

/**
 * Which direction [MemoryRetrieval.traverseRelationships] follows from a
 * [RelationshipTraversalQuery.startingEndpoint]: only relationships where
 * the starting endpoint is [Relationship.fromEndpoint] ([FORWARD]), only
 * where it is [Relationship.toEndpoint] ([REVERSE]), or both ([BOTH]).
 * This is a traversal-time concept, distinct from
 * [Relationship.directional] itself (which describes whether one
 * particular, already-stored relationship is inherently directional or
 * symmetric) -- [BOTH] is a genuine, always-safe default here (the
 * inclusive, no-restriction case), not a fabricated-uncertainty default
 * of the kind [ContentNature] deliberately avoids.
 */
enum class RelationshipTraversalDirection {
    FORWARD,
    REVERSE,
    BOTH,
}

/**
 * What a caller is asking [MemoryRetrieval.findEntities] to find
 * (Contract Design Section 9, "entity lookup"). Every filter field is
 * optional and additive -- omitting all of them (aside from the
 * mandatory [requestingPrincipalId] and [maximumResults]) is a valid
 * request meaning "any Entity," not an error. [maximumResults] mirrors
 * [KnowledgeQuery.maximumResults]'s own already-approved required, positive
 * bound.
 */
data class EntityLookupQuery(
    val requestingPrincipalId: PrincipalId,
    val maximumResults: Int,
    val labelOrAliasMatch: String? = null,
    val entityType: String? = null,
    val status: MemoryCoreRecordStatus? = null,
) {
    init {
        require(maximumResults >= 1) { "EntityLookupQuery.maximumResults must be at least 1, was $maximumResults" }
        require(labelOrAliasMatch == null || labelOrAliasMatch.isNotBlank()) {
            "EntityLookupQuery.labelOrAliasMatch must not be blank if present"
        }
        require(entityType == null || entityType.isNotBlank()) {
            "EntityLookupQuery.entityType must not be blank if present"
        }
    }
}

/**
 * What a caller is asking [MemoryRetrieval.findDocuments] to find
 * (Contract Design Section 9, "document lookup"). Same "every filter is
 * optional and additive" treatment as [EntityLookupQuery].
 */
data class DocumentLookupQuery(
    val requestingPrincipalId: PrincipalId,
    val maximumResults: Int,
    val documentType: String? = null,
    val locationReferenceMatch: String? = null,
    val processingStatus: DocumentProcessingStatus? = null,
) {
    init {
        require(maximumResults >= 1) { "DocumentLookupQuery.maximumResults must be at least 1, was $maximumResults" }
        require(documentType == null || documentType.isNotBlank()) {
            "DocumentLookupQuery.documentType must not be blank if present"
        }
        require(locationReferenceMatch == null || locationReferenceMatch.isNotBlank()) {
            "DocumentLookupQuery.locationReferenceMatch must not be blank if present"
        }
    }
}

/**
 * What a caller is asking [MemoryRetrieval.traverseRelationships] to
 * find (Contract Design Section 9, "relationship traversal"): every
 * [Relationship] connected to [startingEndpoint], optionally narrowed by
 * [relationshipType] and [direction].
 */
data class RelationshipTraversalQuery(
    val requestingPrincipalId: PrincipalId,
    val maximumResults: Int,
    val startingEndpoint: RelationshipEndpoint,
    val relationshipType: String? = null,
    val direction: RelationshipTraversalDirection = RelationshipTraversalDirection.BOTH,
) {
    init {
        require(maximumResults >= 1) {
            "RelationshipTraversalQuery.maximumResults must be at least 1, was $maximumResults"
        }
        require(relationshipType == null || relationshipType.isNotBlank()) {
            "RelationshipTraversalQuery.relationshipType must not be blank if present"
        }
    }
}

/**
 * What a caller is asking [MemoryRetrieval.findByTimeRange] to find
 * (Contract Design Section 9, "chronological lookup"): every record
 * created within [from]..[to], optionally narrowed to one
 * [recordKind] (one of [RelationshipEndpoint]'s own Memory-Core-owned
 * kind constants -- `entity`, `document`, `assertion`, `relationship`).
 * Leaving [recordKind] `null` spans all four kinds at once, which is why
 * [MemoryRetrieval.findByTimeRange] returns the heterogeneous
 * [MemoryCoreRecord] wrapper rather than one concrete record type.
 */
data class ChronologicalLookupQuery(
    val requestingPrincipalId: PrincipalId,
    val maximumResults: Int,
    val from: Instant,
    val to: Instant,
    val recordKind: String? = null,
) {
    init {
        require(maximumResults >= 1) {
            "ChronologicalLookupQuery.maximumResults must be at least 1, was $maximumResults"
        }
        require(!from.isAfter(to)) { "ChronologicalLookupQuery.from must not be after to (from=$from, to=$to)" }
        require(recordKind == null || recordKind.isNotBlank()) {
            "ChronologicalLookupQuery.recordKind must not be blank if present"
        }
    }
}

/**
 * What a caller is asking [MemoryRetrieval.findByMetadata] to find
 * (Contract Design Section 9, "metadata filtering"): every record of
 * [recordKind] whose `metadata` map matches every entry in
 * [metadataFilter]. Unlike [ChronologicalLookupQuery], [recordKind] here
 * is mandatory, not optional -- Contract Design Section 9 describes this
 * mode as returning "records of a specified kind," not records of any
 * kind at once.
 */
data class MetadataLookupQuery(
    val requestingPrincipalId: PrincipalId,
    val maximumResults: Int,
    val recordKind: String,
    val metadataFilter: Map<String, String>,
) {
    init {
        require(maximumResults >= 1) { "MetadataLookupQuery.maximumResults must be at least 1, was $maximumResults" }
        require(recordKind.isNotBlank()) { "MetadataLookupQuery.recordKind must not be blank" }
        require(metadataFilter.isNotEmpty()) { "MetadataLookupQuery.metadataFilter must not be empty" }
    }
}

/**
 * What a caller is asking [MemoryRetrieval.findByProvenance] to find
 * (Contract Design Section 9, "provenance-aware lookup"): every record
 * of [recordKind] whose referenced [Provenance] matches every supplied
 * criterion. [recordKind] is mandatory here for the same reason it is
 * mandatory on [MetadataLookupQuery] -- keeping this mode's return shape
 * consistent with metadata filtering's own already-decided "one
 * specified kind" scoping, rather than leaving one of the two
 * kind-scoped filtering modes ambiguous. At least one of [sourceType],
 * [creator], [contentNature], or [sensitivity] must be supplied -- a
 * provenance-aware lookup naming zero criteria is not a filter at all.
 */
data class ProvenanceLookupQuery(
    val requestingPrincipalId: PrincipalId,
    val maximumResults: Int,
    val recordKind: String,
    val sourceType: String? = null,
    val creator: String? = null,
    val contentNature: ContentNature? = null,
    val sensitivity: ResourceSensitivity? = null,
) {
    init {
        require(maximumResults >= 1) { "ProvenanceLookupQuery.maximumResults must be at least 1, was $maximumResults" }
        require(recordKind.isNotBlank()) { "ProvenanceLookupQuery.recordKind must not be blank" }
        require(sourceType == null || sourceType.isNotBlank()) {
            "ProvenanceLookupQuery.sourceType must not be blank if present"
        }
        require(creator == null || creator.isNotBlank()) {
            "ProvenanceLookupQuery.creator must not be blank if present"
        }
        require(sourceType != null || creator != null || contentNature != null || sensitivity != null) {
            "ProvenanceLookupQuery must specify at least one of sourceType, creator, contentNature, or sensitivity"
        }
    }
}

/**
 * Memory Core's single public structural read surface (Contract Design
 * Section 9; Scope Lock Section 9). Realises exactly the seven approved
 * retrieval modes -- identifier lookup (as four type-safe methods),
 * entity lookup, document lookup, relationship traversal, chronological
 * lookup, metadata filtering, provenance-aware lookup -- and nothing
 * else. No semantic, ranked, embedding-based, or inferred retrieval
 * exists anywhere on this interface, and no method performs
 * principal-based filtering internally: [PrincipalId] parameters here
 * exist for auditability only, exactly as [KnowledgeQuery]'s own already-
 * approved [KnowledgeQuery.requestingPrincipalId] does. A caller receiving
 * more than it should ever see is prevented entirely externally, by a
 * future permission-filtering decorator
 * (`PermissionFilteredMemoryRetrieval`, not implemented by this Unit)
 * composed around an implementation of this interface, never inside one.
 *
 * No implementation of this interface exists yet -- `InMemoryMemoryCore`
 * is a future Unit's own responsibility.
 *
 * ## The four direct-lookup methods, amended by Errata 004
 *
 * [getEntity], [getDocument], [getAssertion], and [getRelationship] each
 * gained a leading `requestingPrincipalId: PrincipalId` parameter
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`) -- the
 * six query-based methods below needed no change, since their own query
 * types already carried one. Every [PrincipalId] parameter across this
 * whole interface remains for auditability only, and now additionally
 * for [PermissionFilteredMemoryRetrieval]-implemented filtering -- never
 * evaluated or filtered on by [InMemoryMemoryCore] itself.
 */
interface MemoryRetrieval {
    suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity?
    suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document?
    suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion?
    suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship?

    suspend fun findEntities(query: EntityLookupQuery): List<Entity>
    suspend fun findDocuments(query: DocumentLookupQuery): List<Document>
    suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship>
    suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord>
    suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord>
    suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord>
}
