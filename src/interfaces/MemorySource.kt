package parker.core.interfaces

/**
 * Memory Source contract, Sprint 11 Unit 7, per
 * `docs/architecture/MEMORY_SOURCE_CONTRACT_DESIGN.md` ("the Contract
 * Design") Section 4.1, freezing
 * `docs/implementation/MEMORY_SOURCE_INTEGRATION_SCOPE_LOCK.md` ("the
 * Scope Lock") Section 1's single responsibility: **given a [MemoryQuery],
 * retrieve the already-promoted, already-existing [MemoryRecord]s
 * matching it.** Nothing more.
 *
 * **One operation, deliberately.** [recall] reuses [MemoryQuery] and
 * [MemoryRecord] unchanged -- no new query type, no new result type, no
 * new field. `recall` is named distinctly from [MemoryStore.retrieve] so
 * a caller holding only a [MemorySource] reference is never confused for
 * one holding a full [MemoryStore] reference, even though the underlying
 * behaviour is identical.
 *
 * **Expresses no retrieval algorithm of its own.** [MemoryQuery] states
 * what the caller is asking for; it is a request shape, not an algorithm.
 * How `relevance` is matched, how candidates are ranked, how ties are
 * broken, and how many are ultimately returned within the caller's stated
 * `maximumResults` is owned entirely by the Memory implementation behind
 * [MemoryStore] (Contract Design Section 4.1) -- this interface does not
 * define, name, or presuppose substring search, semantic search,
 * embeddings, a ranking algorithm, a scoring function, or any other
 * retrieval heuristic. Those remain `MemoryRetrievalPolicy`'s deferred
 * territory (`MEMORY_CONTRACT_DESIGN.md` Section 8), never this
 * interface's.
 *
 * **Passive read boundary, never authority.** [MemorySource] exposes
 * retained memories. It does not determine what becomes memory, does not
 * promote information into memory, does not modify memory, and does not
 * forget memory -- memory creation, promotion, modification, and
 * forgetting remain [MemoryStore]'s own, sole, unchanged responsibility,
 * invoked only through `remember`/`forget`, operations this interface
 * cannot even express (Contract Design's own Constitutional Boundary
 * section).
 *
 * **Read-only, never throws for "no matches."** [recall] returns an empty
 * list -- never an exception -- when nothing matches [MemoryQuery], the
 * same convention [MemoryStore.retrieve] itself already establishes and
 * [ConversationHistorySource.history]/[IdentityService.resolve] already
 * share.
 *
 * **Never a second source of truth.** Every implementation of this
 * interface must be a pure projection of [MemoryStore]'s own owned state
 * (Scope Lock Section 3, Governing Principle) -- it must never construct
 * a [MemoryRecord] itself, never mutate a record, and never call
 * anything equivalent to [MemoryStore.remember] or [MemoryStore.forget].
 * This interface is declared separately from [MemoryStore] specifically
 * so a caller holding only a [MemorySource] reference is structurally
 * unable to reach either -- capability narrowing enforced by the type
 * system, not by convention alone, mirroring exactly why
 * [ConversationHistorySource] is declared separately from
 * [ConversationEngine].
 *
 * **Not decided by this interface (Contract Design Section 5):** the
 * concrete `maximumResults` a caller supplies on any given call --
 * implementation policy, not architecture, deliberately left unfixed
 * here.
 */
fun interface MemorySource {
    suspend fun recall(query: MemoryQuery): List<MemoryRecord>
}
