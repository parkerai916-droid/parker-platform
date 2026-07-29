package parker.core.interfaces

/**
 * World Model Source contract, Sprint 11 Unit 8, per
 * `docs/architecture/WORLD_MODEL_SOURCE_CONTRACT_DESIGN.md` ("the Contract
 * Design") Section 2.1, freezing
 * `docs/implementation/WORLD_MODEL_SOURCE_INTEGRATION_SCOPE_LOCK.md` ("the
 * Scope Lock") Section 1's single responsibility: **given a [WorldQuery],
 * retrieve the already-current [WorldBelief]s matching it.** Nothing more.
 *
 * **One operation, deliberately.** [recall] reuses [WorldQuery] and
 * [WorldBelief] unchanged -- no new query type, no new result type, no new
 * field. `recall` is named distinctly from [WorldModel.query] so a caller
 * holding only a [WorldModelSource] reference is never confused for one
 * holding a full [WorldModel] reference, even though the underlying
 * behaviour is identical -- mirroring exactly why [KnowledgeSource.recall] is
 * not named `retrieve`.
 *
 * **`query`, not `current`, is the operation this interface exposes**
 * (Contract Design Section 2.2). `WorldModel.query`'s own capability is a
 * strict superset of `current`'s -- a caller wanting one exact subject's
 * belief can supply that subject as `WorldQuery.subjectMatch` with
 * `maximumResults = 1` and receive the identical answer. Exposing only
 * `recall(query: WorldQuery)` keeps this interface to the smallest surface
 * that still lets a caller express either question, without adding a
 * second method whose capability the first already subsumes.
 *
 * **Expresses no retrieval algorithm of its own.** [WorldQuery] states what
 * the caller is asking for; it is a request shape, not an algorithm. How
 * `subjectMatch` is matched, how `minimumConfidence` is applied, how
 * staleness is excluded, and in what order results are ultimately returned
 * within the caller's stated `maximumResults` is owned entirely by the
 * World Model implementation behind [WorldModel] (Contract Design Section
 * 3) -- this interface does not define, name, or presuppose ranking,
 * scoring, semantic search, heuristics, embeddings, reconciliation, or
 * contradiction handling. Those remain `WorldModelUpdatePolicy`'s own,
 * already-implemented, internal concern, or a future, separately-scoped
 * retrieval-ranking seam symmetric to Memory's own deferred
 * `MemoryRetrievalPolicy` -- never this interface's.
 *
 * **Passive read boundary, never authority.** [WorldModelSource] exposes
 * current world-model state. It does not determine what becomes world
 * state, does not create world state, does not modify world state, does
 * not reconcile world state, and does not forget world state -- those
 * responsibilities remain [WorldModel]'s own, sole, unchanged
 * responsibility, invoked only through `observe`, an operation this
 * interface cannot even express (Contract Design's own Constitutional
 * Boundary section).
 *
 * **Read-only, never throws for "no matches."** [recall] returns an empty
 * list -- never an exception -- when nothing matches [WorldQuery], the
 * same convention [WorldModel.query] itself already establishes and
 * [ConversationHistorySource.history]/[KnowledgeSource.recall] already share.
 *
 * **No ordering guarantee.** Unlike [KnowledgeSource] (which inherits
 * `InMemoryKnowledgeStore.retrieve`'s explicit, deterministic
 * most-recently-promoted-first guarantee), [WorldModel.query]'s own KDoc
 * makes no ordering guarantee at all -- results are returned in whatever
 * order the underlying implementation produces them. [WorldModelSource]
 * inherits this absence of a guarantee unchanged; it does not invent one.
 * A caller must not depend on any particular order beyond whatever
 * `recall` happens to return.
 *
 * **Never a second source of truth.** Every implementation of this
 * interface must be a pure projection of [WorldModel]'s own owned state
 * (Scope Lock Section 3, Governing Principle) -- it must never construct a
 * [WorldBelief] itself, never mutate one, and never call anything
 * equivalent to [WorldModel.observe]. This interface is declared
 * separately from [WorldModel] specifically so a caller holding only a
 * [WorldModelSource] reference is structurally unable to reach `observe`
 * -- capability narrowing enforced by the type system, not by convention
 * alone, mirroring exactly why [KnowledgeSource] is declared separately from
 * [KnowledgeStore] and [ConversationHistorySource] from [ConversationEngine].
 *
 * **Not decided by this interface (Contract Design Section 3):** exactly
 * how a caller constructs the `WorldQuery` it passes to [recall] --
 * `subjectMatch`'s optional nullability is a property of [WorldQuery]
 * itself (`docs/architecture/WORLD_QUERY_OPTIONAL_SUBJECT_CONTRACT_REVISION.md`),
 * not of this interface, and the concrete `maximumResults` a caller
 * supplies on any given call is implementation policy, not architecture,
 * deliberately left unfixed here -- mirroring [KnowledgeSource]'s identical
 * treatment of its own `maximumResults`.
 */
fun interface WorldModelSource {
    suspend fun recall(query: WorldQuery): List<WorldBelief>
}
