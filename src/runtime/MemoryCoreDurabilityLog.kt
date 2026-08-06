package parker.core.runtime

/**
 * Memory Core Durability, Implementation Unit 2 (Durable Storage
 * Interface). The internal-only persistence seam through which durable
 * Memory Core records will later be appended and read
 * (`docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`, "the
 * Contract Design," Section 12: "durable storage remains strictly below
 * `MemoryCore`"; `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md`,
 * "the Scope Lock," Section 14). This Unit fixes the storage *contract*
 * only -- no filesystem persistence, no serialization, and no replay
 * exists anywhere below.
 *
 * ## A disclosed, deliberate departure from the Implementation Plan's own
 * Unit 2 text
 *
 * `docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md`'s
 * own Unit 2 description anticipated "a single concrete class,
 * `FileSystemMemoryCoreDurabilityLog` -- deliberately not split into an
 * interface plus one implementation the way `EvidenceArtifactStorage`/
 * `FileSystemEvidenceArtifactStorage` are." This governing task's own
 * explicit instruction supersedes that specific reasoning for this
 * session: it asks for "the internal durability storage interface,"
 * explicitly instructing that a caller "do not create a production
 * implementation yet," using a test-only fake in its place. This is a
 * genuine redirection, not a mere narrowing (unlike
 * Unit 1's own disclosed scope refinement) -- it introduces exactly the
 * interface-plus-implementation split the Implementation Plan's own text
 * reasoned against. It is honoured here because it is this session's own
 * explicit, current, and more specific instruction, and because nothing
 * about it conflicts with either governing document's own fixed
 * authority: the Contract Design's own Section 5 already establishes
 * that "this Contract Design fixes required properties; it does not
 * select a mechanism," and an interface-first sequencing is one lawful
 * way to build toward a mechanism without selecting one prematurely, not
 * a new architectural decision requiring a Scope Lock revision. Disclosed
 * here, and in full in this Unit's own Completion Review, rather than
 * silently reconciled.
 *
 * ## Exactly two operations -- the exact shape the Implementation Plan's
 * own Unit 2 already authorised
 *
 * [append] and [readAll], matching
 * `suspend fun append(entry: DurableMemoryCoreEntry)` and
 * `suspend fun readAll(): List<DurableMemoryCoreEntry>` precisely, as the
 * Implementation Plan's own Unit 2 text already fixed them -- no batch
 * write, no delete, update, replace, truncate, clear, or compact
 * operation exists, and none may be added without a Scope Lock or
 * Implementation Plan revision authorising it (Scope Lock Section 6's own
 * mechanism-boundary discipline, applied here to the interface shape
 * itself).
 *
 * ## Failure behaviour, fixed now, deliberately minimal
 *
 * Neither operation returns a value capable of representing failure --
 * [append] returns `Unit`, never a `Boolean` success flag; [readAll]
 * returns a non-nullable `List<DurableMemoryCoreEntry>`, never a
 * nullable or sealed-result-wrapped type. **A failure of either
 * operation is a thrown exception, propagated to the caller, never a
 * silently substituted empty list or a silently discarded write** --
 * mirroring [EvidenceArtifactStorage]'s own exception hierarchy, "sealed
 * and thrown -- not returned as a sealed result type," and the Contract
 * Design's own Section 11 requirement, "**No new caller-facing public
 * result type is invented.**" This Unit deliberately does **not** define a dedicated
 * exception type for this interface: no real implementation exists yet
 * to have a real failure mode to name, and inventing one in advance would
 * risk fixing the wrong shape before a genuine failure case (Unit 3's own
 * responsibility) is known. An implementation of this interface remains
 * free to let an underlying, implementation-specific exception propagate
 * unchanged, or to wrap it in a purpose-named exception of its own --
 * either satisfies this interface's own contract, which fixes only that
 * failure is never silent and never indistinguishable from emptiness.
 *
 * **An implementation must never make an unavailable or corrupt store
 * indistinguishable from a genuinely empty one.** If [readAll] cannot
 * determine what was durably stored, it must throw, not return an empty
 * list -- an empty list from [readAll] must mean, and only ever mean,
 * "nothing has been durably appended yet." Corruption *classification*
 * (partial write versus genuine corruption) is explicitly not this
 * Unit's own responsibility (a future Unit's own, Contract-Design-Section-7-governed
 * concern) -- this Unit fixes only that the *emptiness* and
 * *unavailability* cases must never be confused with one another.
 *
 * ## What this Unit deliberately does not implement
 *
 * File I/O; append-only log encoding; atomic flush; replay; startup
 * recovery; restore functions; identifier restoration; lifecycle replay;
 * runtime composition; Docker volume wiring; backup or replication;
 * SQLite; caching; indexing; corruption recovery of any kind. Each is
 * either a later Implementation Plan unit's own responsibility, or
 * (backup/replication, SQLite, caching, indexing) permanently deferred
 * per the Scope Lock's own Out-of-Scope Register.
 *
 * ## Dependencies -- exactly one
 *
 * This interface depends on [DurableMemoryCoreEntry] (Unit 1) and
 * standard Kotlin collection types only. No dependency on
 * `InMemoryMemoryCore`, `PermissionEngine`, Knowledge Memory,
 * `EvidenceCustodian`, `EventBus`, or `ParkerRuntime` exists anywhere in
 * this file, confirmed both by this file's own import list (empty --
 * [DurableMemoryCoreEntry] lives in this same package) and by a dedicated
 * structural test.
 *
 * ## Internal, not public
 *
 * [MemoryCoreDurabilityLog] is `internal`, mirroring
 * [DurableMemoryCoreEntry]'s and `MemoryCoreLifecycleTransitions`'s own
 * identical precedent: never reachable through
 * `src/interfaces/MemoryCore.kt`'s own public `MemoryCore`/
 * `MemoryRetrieval` contracts, never referenced by either, and never
 * itself part of any public, caller-facing contract.
 */
internal interface MemoryCoreDurabilityLog {

    /**
     * Durably appends [entry]. Accepts exactly one [DurableMemoryCoreEntry]
     * per call -- there is no batch-append operation, and none is
     * authorised. A call that returns normally has durably committed
     * [entry]; a call that cannot durably commit it throws, never returns
     * normally having silently discarded it.
     */
    suspend fun append(entry: DurableMemoryCoreEntry)

    /**
     * Reads back every entry [append] has durably committed, **in the
     * exact order [append] was called** -- the ordering guarantee this
     * whole seam exists to preserve (Contract Design Section 10's own
     * total-write-order requirement, which a future recovery unit depends
     * on). An empty result means, and only ever means, that nothing has
     * been durably appended yet; an implementation that cannot determine
     * this throws instead of returning an empty list.
     */
    suspend fun readAll(): List<DurableMemoryCoreEntry>
}
