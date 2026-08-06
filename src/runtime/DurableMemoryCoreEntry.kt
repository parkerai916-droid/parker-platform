package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.Assertion
import parker.core.interfaces.Document
import parker.core.interfaces.Entity
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.Provenance
import parker.core.interfaces.Relationship

/**
 * Memory Core Durability, Implementation Unit 1 (Durable Record Format
 * Boundary). The internal, in-memory representation of one durable act --
 * one [DurableMemoryCoreEntry] per [InMemoryMemoryCore] write operation
 * (`docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`, "the
 * Contract Design," Section 4: "one `MemoryCore` contract operation is
 * one atomic durable act"). This file establishes the durable *model*
 * only, exactly as `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 1 requires.
 *
 * ## What this Unit deliberately does not do -- a disclosed, narrower
 * boundary than the Implementation Plan's own Unit 1 text anticipated
 *
 * The Implementation Plan's own Unit 1 description anticipated "a
 * deterministic, round-trip-safe line-oriented text encoding for each
 * case" living in this same file. This governing task's own explicit
 * instruction narrows that, requiring this Unit to "expose no
 * serialization technology," and separately stating that "no JSON
 * library, SQLite library, serializer, parser, or storage implementation
 * is authorised in this unit." This file honours the narrower
 * instruction -- no `encode`/`decode` function, no text format,
 * no file I/O, and no import beyond the JDK's own [Instant] and this
 * repository's already-existing Memory Core domain types exists anywhere
 * below. The text encoding the Implementation Plan's own Unit 1
 * anticipated is deferred to a future unit's own "internal-only
 * persistence seam" work, where the concrete storage mechanism's own
 * format concerns actually belong -- this only moves *where* an
 * already-planned capability is implemented, not *whether* it is
 * planned; see `docs/reviews/MEMORY_CORE_DURABILITY_UNIT_1_RECORD_FORMAT_COMPLETION_REVIEW.md`
 * for the full disclosure.
 *
 * No persistence, append log, replay, startup recovery, restore method,
 * runtime composition, Docker integration, `PermissionEngine` reference,
 * or Knowledge Memory reference exists anywhere in this file either --
 * each is a later, separate Implementation Plan unit's own responsibility.
 *
 * ## Six cases, one per write operation -- matching the Contract Design's
 * own durable record scope exactly
 *
 * [ProvenanceCreated], [EntityCreated], [DocumentRegistered],
 * [AssertionCreated], and [RelationshipCreated] each wrap the
 * corresponding, already-frozen Memory Core domain record type
 * ([Provenance], [Entity], [Document], [Assertion], [Relationship])
 * unchanged -- no field is added, removed, or renamed (Contract Design
 * Section 3: "no field added, removed, or renamed by this document").
 * Wrapping the existing, canonical record type, rather than duplicating
 * its own field list a second time here, is what makes "preserve
 * immutable identifiers," "preserve timestamps," and "preserve
 * provenance links" true by construction: whatever those five record
 * types already carry, an entry wrapping one carries too, with no
 * possibility of the two silently drifting apart. [StatusTransitioned]
 * represents the sixth write operation, [InMemoryMemoryCore.transitionStatus] --
 * the one write operation with no corresponding stored record type of
 * its own to wrap, since a lifecycle transition is a fact *about* an
 * existing record, not a new record (Contract Design Section 9:
 * "Lifecycle transitions are appended, not rewritten").
 * `DocumentRegistered`, not `DocumentCreated`, deliberately mirrors
 * [Document]'s own established verb distinction (`registeredAt`, not
 * `createdAt`) -- a Document is registered, never created, by Memory
 * Core (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` Section 5).
 *
 * ## `schemaVersion`, explicit on every case
 *
 * Every case carries its own [schemaVersion], defaulted to
 * [CURRENT_SCHEMA_VERSION] -- a single source of truth for "what version
 * does code written today produce," never hardcoded separately per case
 * (Contract Design Section 8: "every durably stored record carries an
 * explicit schema-version tag"). The default exists for the same reason
 * [Entity.status] defaults to `ACTIVE` rather than [Provenance.contentNature]
 * carrying no default at all: there is exactly one correct value for the
 * common case (code written against today's schema always produces
 * today's version), not a genuine caller uncertainty a default would
 * paper over. The field remains present, explicit, and inspectable on
 * every entry regardless -- a future decode step (not implemented here)
 * is what gives this field its real work to do, rejecting a version
 * newer than the reading code understands rather than silently
 * interpreting it (Contract Design Section 8).
 *
 * ## `StatusTransitioned`'s own fields, distinct from the record types
 * above
 *
 * [StatusTransitioned] does not wrap an existing domain type, since none
 * exists for a transition fact -- it fixes its own minimal shape
 * instead: [reference] (which record, using the same
 * [MemoryCoreRecordReference] sealed type [InMemoryMemoryCore.transitionStatus]
 * itself already accepts, never a loosely-typed kind/id pair); both
 * [priorStatus] and [targetStatus], since a transition *is* a
 * (prior, target) pair and recording only the target would lose exactly
 * the "what actually changed" information "preserve lifecycle
 * information" requires; and [transitionedAt], a timestamp this entry
 * itself owns -- distinct from any field on [Entity], [Document],
 * [Assertion], or [Relationship], none of which carries its own
 * "last transitioned at" field today, and none of which this file adds
 * one to.
 *
 * ## What this file deliberately omits, and why
 *
 * No entry carries a `requestingPrincipalId`. Every `MemoryCore` write
 * operation accepts one, but "only for auditability (recording who
 * asked) -- never as a filter" (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`
 * Section 9), added to every operation's own signature by Errata 004 --
 * this governing task's own Unit 1 requirement list names
 * "preserve immutable identifiers," "preserve timestamps," "preserve
 * provenance links," and "preserve lifecycle information," and does not
 * name "preserve requesting principal." Adding a field the task did not
 * ask for, however plausible, would be exactly the kind of unjustified
 * scope expansion `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md`'s
 * own Scope Lock Principle prohibits ("the burden of proof favours
 * exclusion, not inclusion"). Disclosed here, not silently decided.
 *
 * ## Internal, not public -- mirroring an established precedent exactly
 *
 * [DurableMemoryCoreEntry] and every one of its six cases are `internal`,
 * never reachable through `src/interfaces/MemoryCore.kt`'s own public
 * `MemoryCore`/`MemoryRetrieval` contracts, and never referenced by
 * either. This mirrors [MemoryCoreLifecycleTransitions]'s own existing
 * precedent exactly: kept `internal`, not `private`, specifically so
 * code elsewhere in this same module -- a future durability
 * implementation; this Unit's own test file -- can exercise it directly,
 * without it ever becoming part of any public, caller-facing contract.
 */
internal sealed class DurableMemoryCoreEntry {

    abstract val schemaVersion: Int

    internal data class ProvenanceCreated(
        override val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val provenance: Provenance,
    ) : DurableMemoryCoreEntry()

    internal data class EntityCreated(
        override val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val entity: Entity,
    ) : DurableMemoryCoreEntry()

    internal data class DocumentRegistered(
        override val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val document: Document,
    ) : DurableMemoryCoreEntry()

    internal data class AssertionCreated(
        override val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val assertion: Assertion,
    ) : DurableMemoryCoreEntry()

    internal data class RelationshipCreated(
        override val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val relationship: Relationship,
    ) : DurableMemoryCoreEntry()

    internal data class StatusTransitioned(
        override val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
        val reference: MemoryCoreRecordReference,
        val priorStatus: MemoryCoreRecordStatus,
        val targetStatus: MemoryCoreRecordStatus,
        val transitionedAt: Instant,
    ) : DurableMemoryCoreEntry()

    internal companion object {
        /**
         * The schema version every entry constructed by code in this
         * repository today carries. A future, genuinely breaking schema
         * change requires its own dedicated governance review before this
         * constant changes (Contract Design Section 8; this repository's
         * own Errata precedent) -- it is not incremented speculatively
         * here.
         */
        const val CURRENT_SCHEMA_VERSION: Int = 1
    }
}
