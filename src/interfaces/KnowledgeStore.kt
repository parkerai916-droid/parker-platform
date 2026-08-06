package parker.core.interfaces

import java.time.Instant

/**
 * Sprint 4, Track A, Unit A3 (Memory Runtime Implementation). Field-level
 * Kotlin for every contract `docs/architecture/MEMORY_CONTRACT_DESIGN.md`
 * (Unit A2) approved as required: [KnowledgeId], [CandidateKnowledge],
 * [KnowledgeRecord], [KnowledgeCategory], [KnowledgePromotionDecision],
 * [KnowledgePromotionPolicy], [KnowledgeQuery], [KnowledgeStore]. Nothing else is
 * added here -- the four excluded candidates (`CandidateMemoryId`,
 * `MemoryQueryResult`, `MemoryRuntime`, `MemoryObservation`) and the two
 * deferred seams (`MemoryRetrievalPolicy`, a combined retention/
 * consolidation seam) are not implemented, per Unit A2's own
 * determination and this Unit's explicit instruction to stop and report
 * rather than implement a deferred seam without justification.
 *
 * This file replaces the original four-operation, named-only stub this
 * repository previously carried. Three changes from that stub, each
 * required by Unit A2 and explained here rather than applied silently:
 *
 * 1. The promoted-record type is named [KnowledgeRecord], not `Memory`
 *    (`MEMORY_CONTRACT_DESIGN.md` §3's naming clarification -- "Memory"
 *    already names the subsystem itself throughout this architecture;
 *    reusing the same bare word as a concrete type name invited exactly
 *    the ambiguity a reader would otherwise have to resolve from context
 *    every time it appeared).
 * 2. `promote(memoryId: KnowledgeId): Memory` no longer exists as a public,
 *    caller-facing operation (`MEMORY_CONTRACT_DESIGN.md` §9's
 *    architectural decision: "External callers never invoke promotion.
 *    Memory owns evaluation and promotion internally, end to end").
 *    [KnowledgeStore.remember] now performs submission, Evaluation, and
 *    (where [KnowledgePromotionPolicy] decides in the submission's favour)
 *    Promotion in one call, returning a [KnowledgePromotionDecision]
 *    capable of expressing either outcome -- exactly the shape Unit A2
 *    required ("the public contract must express the result of
 *    submission/evaluation/promotion in a way that can represent both
 *    promoted and rejected outcomes").
 * 3. `forget(memoryId: KnowledgeId): ForgetResult` no longer names a
 *    `ForgetResult` type. `ForgetResult` was never one of Unit A2's eight
 *    approved required contracts -- it was a leftover, unauthorised name
 *    from the original stub, not a decision Unit A2 actually made.
 *    Rather than shaping a ninth, unapproved contract now,
 *    [KnowledgeStore.forget] returns a plain `Boolean`: `true` if a record
 *    existed and was forgotten, `false` if [KnowledgeId] named nothing (a
 *    missing record is a normal, safely-handled outcome, never an
 *    exception). This mirrors this Unit's own instruction not to invent
 *    unauthorised method or type names, applied to a type the original
 *    stub named but Unit A2 never approved. See
 *    `docs/reviews/SPRINT_4_TRACK_A_UNIT_A3_POST_IMPLEMENTATION_REVIEW.md`
 *    for the full account of this change.
 */

/**
 * A Memory record's identifier, across its entire lifecycle -- from the
 * moment a [CandidateKnowledge] is submitted through however long the
 * resulting [KnowledgeRecord] remains retrievable
 * (`MEMORY_CONTRACT_DESIGN.md` §1). Follows the same established
 * identifier pattern as every other long-lived object Parker owns
 * ([PrincipalId], [ResourceId], [PlanCandidateId], [TaskProposalId],
 * [TaskId], [PlanningSessionId]): a single, blank-rejecting `String`
 * value, assigned once (by [KnowledgeStore], at submission) and never
 * reassigned or recycled -- including for a forgotten record, so an
 * audit trail can still name it. One identifier space, not two: Unit A2
 * §1 ("Why not `CandidateMemoryId` as well?") found the existing stub's
 * own `addCandidate(candidate: CandidateKnowledge): KnowledgeId` signature
 * already committed to a single identifier space, assigned at submission
 * and carried unchanged through Promotion, since Promotion is a state
 * change of one record Memory owns throughout -- not the construction of
 * a different object owned by a different subsystem, the way a
 * `PlanCandidate` becoming a `TaskProposal` is.
 */
@JvmInline
value class KnowledgeId(val value: String) {
    init {
        require(value.isNotBlank()) { "KnowledgeId must not be blank" }
    }
}

/**
 * The architectural categories of Memory content
 * (`docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md` §3;
 * `MEMORY_CONTRACT_DESIGN.md` §4). A single closed enum, deliberately not
 * five separate types: these categories classify content only -- they do
 * not change ownership, lifecycle, or constitutional boundaries -- and no
 * category-specific field requirement has ever been identified. A sixth
 * value could be added additively if a genuinely new kind of knowledge
 * is identified later.
 */
enum class KnowledgeCategory {
    EPISODIC,
    SEMANTIC,
    PROCEDURAL,
    USER_PREFERENCES,
    RELATIONSHIPS,
}

/**
 * What a subsystem submits when it observes something that might be
 * worth remembering -- the shape of a proposal for retention, before
 * Evaluation (`MEMORY_CONTRACT_DESIGN.md` §2). Owned by Memory from the
 * instant it is submitted via [KnowledgeStore.remember]
 * (`docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md`'s explicit
 * ownership clarification), not before.
 *
 * Field-by-field justification, per `MEMORY_CONTRACT_DESIGN.md` §2:
 *
 * - [knowledgePayload]: the content actually being proposed for
 *   retention. Required, non-blank. This type does not shape the
 *   payload's internal structure -- that is storage-format territory,
 *   explicitly out of scope for this Unit -- only that one must be
 *   present.
 * - [proposedCategory]: the submitting subsystem's own best
 *   classification. Proposed, not final -- it carries no authority; a
 *   submitter cannot force its own classification to stick (cognition
 *   proposes; Memory's own policy decides).
 * - [sourceSubsystem]: free-text provenance naming where this submission
 *   came from. Deliberately a `String`, not a closed enum --
 *   `MEMORY_RUNTIME_ARCHITECTURE.md` §6 names known sources (the Planner
 *   Runtime, the Agent Runtime, the World Model, a direct user
 *   instruction, a plugin, a workflow, a future reasoning provider) as
 *   "illustrative... not closed by architectural necessity."
 * - [correlationId]: ties this submission back to the task or session
 *   that produced it, mirroring [TaskProposal.correlationId] and
 *   [PlanningRequest.correlationId]'s own identical, required, non-blank
 *   treatment.
 * - [originatingPrincipalId]: the Principal this observation relates to,
 *   where one exists. Nullable -- not every submission has one.
 * - [confidence]: optional (`0.0`..`1.0`), since not every submitter has
 *   a figure to offer; one of `33-memory-consolidation.md`'s six
 *   promotion factors.
 * - [explicitlyRequested]: `33-memory-consolidation.md`'s "explicit
 *   request" factor, named as its own distinct signal, separate from
 *   confidence or repetition -- a user directly asking Parker to
 *   remember something is different evidence than Parker noticing a
 *   pattern on its own.
 * - [sensitive]: carried forward unchanged onto the resulting
 *   [KnowledgeRecord] if promoted -- enough for the Permission Engine to
 *   evaluate a disclosure decision against later, per
 *   `docs/specifications/volume-03-core-interfaces/KnowledgeStore.md`'s
 *   "Sensitive memories MUST require appropriate permission." Memory
 *   never evaluates this itself (constitutional boundary: Memory never
 *   authorises); it only carries the flag.
 *
 * Deliberately absent, per `MEMORY_CONTRACT_DESIGN.md` §2's "what it
 * intentionally does not carry": any promotion decision; any
 * [KnowledgeRecord]-only field (a retention hint, consolidation history);
 * any authority of any kind; any self-reported repetition/frequency
 * figure (that comparison is [KnowledgePromotionPolicy]'s job, performed
 * against Memory's own existing records, not something the submitter can
 * assert about itself); and any ranking or relevance score (a
 * retrieval-time concept, not a submission-time one).
 */
data class CandidateKnowledge(
    val knowledgePayload: String,
    val proposedCategory: KnowledgeCategory,
    val sourceSubsystem: String,
    val correlationId: String,
    val originatingPrincipalId: PrincipalId? = null,
    val confidence: Double? = null,
    val explicitlyRequested: Boolean = false,
    val sensitive: Boolean = false,
) {
    init {
        require(knowledgePayload.isNotBlank()) { "CandidateKnowledge.knowledgePayload must not be blank" }
        require(sourceSubsystem.isNotBlank()) { "CandidateKnowledge.sourceSubsystem must not be blank" }
        require(correlationId.isNotBlank()) { "CandidateKnowledge.correlationId must not be blank" }
        if (confidence != null) {
            require(confidence in 0.0..1.0) {
                "CandidateKnowledge.confidence must be between 0.0 and 1.0, was $confidence"
            }
        }
    }
}

/**
 * The durable, promoted representation of a Long-term Memory
 * (`docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md` §4;
 * `MEMORY_CONTRACT_DESIGN.md` §3). Constructed by [KnowledgeStore] at the
 * moment of Promotion; the type [KnowledgeStore.retrieve] returns.
 *
 * Field groups, exactly as `MEMORY_CONTRACT_DESIGN.md` §3 separates them:
 * required identity ([memoryId]); required metadata ([category],
 * [sourceSubsystem]/[correlationId]/[originatingPrincipalId] as
 * provenance, [promotedAt], [sensitive]); optional metadata
 * ([confidence], [relatedMemoryIds]); knowledge payload
 * ([knowledgePayload]); and history ([history]).
 *
 * [history] is deliberately a plain `List<String>` of human-readable
 * audit entries, not a new named event type -- the same free-text-over-
 * new-type choice already made for [PlanRejection.detail] and
 * [PlanningSessionResult.Failed.reason], consistent with this Unit's
 * instruction not to introduce unauthorised public contracts. Per Unit
 * A2's own revision, the audit trail must be able to express at least
 * four kinds of event -- promoted, consolidated, forgotten, superseded --
 * though this Unit's implementation records only "promoted" (on the
 * record itself; "forgotten" is recorded by [InMemoryKnowledgeStore]
 * separately, since a forgotten record's [KnowledgeRecord] is removed from
 * retrieval). Consolidation and Retention/supersession are deferred
 * seams this Unit does not implement, per its own explicit scope.
 */
data class KnowledgeRecord(
    val memoryId: KnowledgeId,
    val category: KnowledgeCategory,
    val sourceSubsystem: String,
    val correlationId: String,
    val promotedAt: Instant,
    val knowledgePayload: String,
    val originatingPrincipalId: PrincipalId? = null,
    val confidence: Double? = null,
    val sensitive: Boolean = false,
    val relatedMemoryIds: List<KnowledgeId> = emptyList(),
    val history: List<String> = emptyList(),
) {
    init {
        require(knowledgePayload.isNotBlank()) { "KnowledgeRecord.knowledgePayload must not be blank" }
        require(sourceSubsystem.isNotBlank()) { "KnowledgeRecord.sourceSubsystem must not be blank" }
        require(correlationId.isNotBlank()) { "KnowledgeRecord.correlationId must not be blank" }
        if (confidence != null) {
            require(confidence in 0.0..1.0) {
                "KnowledgeRecord.confidence must be between 0.0 and 1.0, was $confidence"
            }
        }
    }
}

/**
 * The outcome of one [KnowledgePromotionPolicy] evaluation of one
 * [CandidateKnowledge] (`MEMORY_CONTRACT_DESIGN.md` §5) -- also the return
 * type of [KnowledgeStore.remember] itself, per `MEMORY_CONTRACT_DESIGN.md`
 * §9's architectural decision that submission and its outcome are one
 * caller-facing step.
 *
 * Deliberately two variants, not three: unlike `PlanDecisionResult`,
 * which evaluates a whole batch of Plan Candidates together and must
 * express "no candidate in this batch was viable" as its own case,
 * Memory's Evaluation judges one [CandidateKnowledge] at a time, with no
 * other candidate competing against it in the same call -- there is no
 * batch, so there is no such case to express.
 *
 * [Reject.reason] is deliberately a free-text, non-blank `String`, not a
 * closed enum like `PlanRejectionReason` -- a Memory promotion decision
 * weighs several named factors together
 * (`docs/architecture/33-memory-consolidation.md`: repetition, user
 * importance, goal relevance, frequency, confidence, explicit request),
 * not a single structural pass/fail rule the way `PlanRejection`'s four
 * reasons are; collapsing that multi-factor judgement into one enum
 * value would misrepresent it as a discrete rule, which it is not.
 */
sealed class KnowledgePromotionDecision {
    abstract val memoryId: KnowledgeId

    data class Promote(
        override val memoryId: KnowledgeId,
        val category: KnowledgeCategory,
    ) : KnowledgePromotionDecision()

    data class Reject(
        override val memoryId: KnowledgeId,
        val reason: String,
    ) : KnowledgePromotionDecision() {
        init {
            require(reason.isNotBlank()) { "KnowledgePromotionDecision.Reject.reason must not be blank" }
        }
    }
}

/**
 * The seam by which Memory decides whether a submitted [CandidateKnowledge]
 * is promoted (`MEMORY_CONTRACT_DESIGN.md` §6). Structurally identical to
 * `PlanDecision` and `AgentStepSource` -- not `AgentPolicy`, which is a
 * bounded-configuration record, not a decision seam
 * (`MEMORY_CONTRACT_DESIGN.md` §6, "Comparing the three policy seams").
 *
 * `KnowledgePromotionPolicy` SHALL determine whether a [CandidateKnowledge]
 * becomes a [KnowledgeRecord] -- stated without hedging, per Unit A2's own
 * revision. This is a Memory-internal policy decision, never invoked
 * directly by an external caller of Memory: a [KnowledgeStore]
 * implementation consults it internally as part of handling
 * [KnowledgeStore.remember] (`MEMORY_CONTRACT_DESIGN.md` §9's architectural
 * decision) -- an external caller submits a [CandidateKnowledge] and learns
 * the outcome; it never calls this interface itself, sees its reasoning
 * before the decision is final, or overrides, appeals, or bypasses it.
 *
 * `suspend`, for the same reason `PlanDecision.decide` is: a future
 * policy implementation may need to compare a submission against a
 * large or externally-stored population of existing records (weighing
 * `33-memory-consolidation.md`'s "repetition"/"frequency" factors), and
 * must not be foreclosed by a non-suspending signature decided before
 * any real implementation exists.
 *
 * [memoryId] is passed in, already assigned, rather than returned:
 * [KnowledgeId] is minted by [KnowledgeStore] at submission (never by this
 * policy, and never by a caller), so by the time this seam is consulted,
 * the identifier already exists -- this operation only decides what
 * happens to the record that identifier names.
 */
interface KnowledgePromotionPolicy {
    suspend fun evaluate(candidate: CandidateKnowledge, memoryId: KnowledgeId): KnowledgePromotionDecision
}

/**
 * What a caller is asking Memory to retrieve
 * (`MEMORY_CONTRACT_DESIGN.md` §7). A request shape only --
 * [KnowledgeStore.retrieve] is the operation that acts on it; this type
 * defines no ranking or retrieval algorithm (`MemoryRetrievalPolicy`
 * remains a deferred seam this Unit does not implement).
 *
 * [maximumResults] is a required, positive bound, per Unit A2's own
 * revision: Memory retrieval must not imply "return everything
 * matching." The caller decides how many records it wants back; Memory
 * decides which ones, among the matches, are most relevant to return
 * first.
 */
data class KnowledgeQuery(
    val requestingPrincipalId: PrincipalId,
    val relevance: String,
    val correlationId: String,
    val maximumResults: Int,
    val category: KnowledgeCategory? = null,
) {
    init {
        require(relevance.isNotBlank()) { "KnowledgeQuery.relevance must not be blank" }
        require(correlationId.isNotBlank()) { "KnowledgeQuery.correlationId must not be blank" }
        require(maximumResults >= 1) { "KnowledgeQuery.maximumResults must be at least 1, was $maximumResults" }
    }
}

/**
 * Memory's one public interface (`MEMORY_CONTRACT_DESIGN.md` §9). No
 * separate `MemoryRuntime` interface exists -- Unit A2's own central
 * minimalism decision was that this interface already suffices, exactly
 * as [IdentityService]/[ToolRegistry] are each the one public interface
 * for their own subsystem.
 *
 * Three operations, not four: [remember] replaces the original stub's
 * separate `addCandidate`/`promote` pair with one submit-and-learn-the-
 * outcome operation (`MEMORY_CONTRACT_DESIGN.md` §9's architectural
 * decision -- external callers never invoke promotion directly);
 * [retrieve] and [forget] are unchanged in intent from the original
 * stub, aside from [forget]'s corrected, unauthorised-type-free return
 * shape (see this file's own header KDoc).
 */
interface KnowledgeStore {
    suspend fun remember(candidate: CandidateKnowledge): KnowledgePromotionDecision
    suspend fun retrieve(query: KnowledgeQuery): List<KnowledgeRecord>
    suspend fun forget(memoryId: KnowledgeId): Boolean
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 4 (Knowledge Item,
 * Knowledge Promotion, Knowledge Reference). Additively extends this
 * file (`docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md`
 * §4's own repository-impact table names this file, not a new one, as
 * Unit 4's location) with the three public model types
 * `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
 * §12 (the Contract Inventory) requires, plus the one minimal supporting
 * value type ([KnowledgeItemStatus]) that Knowledge Item's own approved
 * "current lifecycle status" field requires to compile. Every existing
 * declaration above this comment -- [KnowledgeId], [KnowledgeCategory],
 * [CandidateKnowledge], [KnowledgeRecord], [KnowledgePromotionDecision],
 * [KnowledgePromotionPolicy], [KnowledgeQuery], [KnowledgeStore] -- is
 * unchanged by this Unit; nothing below alters an existing field,
 * method, or behaviour.
 *
 * ## Why this Unit adds to this file rather than a new one
 *
 * Unlike Units 2 and 3 ([EvidentialState], [ProvenanceReference]), which
 * the Implementation Plan's own repository-impact table places in "a new
 * value-type location," that same table places Unit 4 in this file
 * specifically: "`src/interfaces/MemoryStore.kt` | 1, 4, 5 | Renamed
 * (Unit 1); additively extended (Units 4, 5)." The Migration Strategy's
 * "avoid duplicate sources of truth" principle (Section 5) reinforces
 * this: Knowledge Item, Knowledge Promotion, and Knowledge Reference are
 * built as additive extensions of the renamed record shape this file
 * already holds, sharing its identifier space ([KnowledgeId]) rather
 * than existing as an unrelated, independently addressed type family in
 * a different file.
 *
 * ## No duplicated field -- the actual test this Unit applies
 *
 * "Additive extension... never a second, parallel type carrying its own
 * independent copy of what the renamed shape already holds" (Migration
 * Strategy, Section 5) is satisfied concretely, not merely asserted:
 * [KnowledgeItem] carries none of [KnowledgeRecord]'s own fields
 * ([KnowledgeRecord.knowledgePayload], [KnowledgeRecord.category],
 * [KnowledgeRecord.sourceSubsystem], [KnowledgeRecord.correlationId],
 * [KnowledgeRecord.promotedAt], [KnowledgeRecord.originatingPrincipalId],
 * [KnowledgeRecord.sensitive], [KnowledgeRecord.relatedMemoryIds]) -- it
 * shares only [KnowledgeId], the identifier space, and adds exclusively
 * the four things Contract Design Version 2 §12's own Knowledge Item row
 * names: a Memory Core evidence reference, an evidential-state
 * classification, a provenance reference, and an ordered history -- plus
 * the current-status field that row also names. This is a disclosed,
 * reasoned reading of "additive extension," not a literal in-place field
 * merge into [KnowledgeRecord] itself -- Contract Design Version 2 §12
 * lists Knowledge Item as its own, separately named contract, and this
 * task's own instruction is to "introduce the approved public Knowledge
 * Memory model types," which only makes sense read as introducing
 * genuinely new, distinctly named types.
 *
 * [KnowledgeItem.history] and [KnowledgeRecord.history] remain two
 * differently-shaped fields sharing the word "history" -- this was
 * identified by the Unit 4 Governance Reconciliation as a genuine
 * duplicate-source-of-truth risk, not a merely apparent one, and is now
 * resolved by
 * `docs/governance/PROGRAMME_3_UNIT_4_SCOPE_LOCK_CLARIFICATION.md` §1:
 * [KnowledgeItem.history] is the sole constitutionally authoritative
 * structured history for the new public model; [KnowledgeRecord.history]
 * remains legacy migration state only, never independently authoritative
 * for the same promoted knowledge, and neither is synchronised, copied,
 * merged, or dual-written by this or any unit before Unit 10. See that
 * clarification document for the full reasoning and for which later unit
 * (Unit 10) owns retiring or adapting the legacy field.
 *
 * ## Why the evidence reference is `MemoryCoreRecordReference`, not `AssertionId`
 *
 * This Unit originally typed [KnowledgeItem.evidenceReference] and
 * [KnowledgePromotion.evidenceReference] as [AssertionId], reasoning from
 * Contract Design Version 2's own illustrative examples (Section 3: "a
 * referenced Assertion's own recorded confidence"; "the underlying
 * Assertion" being marked superseded). The Unit 4 Governance
 * Reconciliation identified this as an unauthorised over-narrowing:
 * every *general* statement in the frozen documents describing this
 * field is deliberately generic ("a Memory Core record," Contract Design
 * Version 2 §5; "a reference to Memory Core content," §7; "Memory Core
 * evidence reference," §2, §12), and
 * `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §3 is
 * explicit that the submission path must be adapted to reference real
 * Memory Core "Provenance, Entity, Document, and Evidence records" --
 * not Assertion alone. [MemoryCoreRecordReference]
 * (`MemoryCore.kt`, Programme 2 Unit 7 Amendment) is the existing,
 * already-authoritative sealed type covering exactly this breadth
 * ([EntityId], [DocumentId], [AssertionId], [RelationshipId]), reused
 * here unchanged rather than inventing a new reference hierarchy, per
 * this task's own repeated instruction not to invent a duplicate type
 * where an authoritative one already exists.
 *
 * ## Why [KnowledgeItemStatus] exists, and why it is exactly two values
 *
 * Contract Design Version 2 §12 states Knowledge Item carries "its own
 * current lifecycle status" but does not itself enumerate the value set
 * -- this task's own instruction, anticipating exactly this gap,
 * authorises introducing "only that minimal approved representation"
 * where the contract requires a lifecycle value type for these models to
 * compile coherently, provided it is explained rather than improvised.
 * Section 3's own lifecycle prose names exactly two states a Knowledge
 * Item's *current* status can honestly be: "current" (the state entered
 * at promotion and preserved through revision) and "no longer current"
 * (Retirement's own defining language). Restoration does not introduce a
 * third current-status value -- Section 3 is explicit that restoration
 * is a new, visible history *event*, appended alongside promotion,
 * revision, and retirement, not a status value of its own; a restored
 * Knowledge Item's current status returns to [KnowledgeItemStatus.ACTIVE],
 * with the fact that it was once retired preserved permanently in
 * [KnowledgeItem.history], never in the current-status field. No
 * transition method, validation, or enforcement of legal transitions
 * exists on this enum, mirroring [MemoryCoreRecordStatus]'s own identical
 * precedent ("enforced by MemoryCore's own implementation, never by this
 * enum itself") -- that enforcement is Unit 7's own, later, separately
 * authorised responsibility.
 *
 * ## What this Unit does not implement
 *
 * No promotion decision, no promotion algorithm, no revision logic, no
 * supersession logic, no contradiction handling, no retirement or
 * restoration logic, no ordering rule, no persistence, no retrieval, and
 * no permission evaluation exist anywhere in this Unit's additions --
 * every type below is a plain, immutable data holder, exactly like every
 * other record in this file and in `MemoryCore.kt`. [KnowledgeItem.history]
 * is declared as an ordered `List` (Kotlin's own read-only collection
 * interface, matching every other collection field in this file and in
 * `MemoryCore.kt`), but nothing in this Unit populates it, sorts it, or
 * enforces the non-forking guarantee Contract Design Version 2 §3
 * describes -- exactly as [Entity.aliases]'s own "additive-only" KDoc
 * already discloses for a structurally identical reason, that guarantee
 * is a future write-path behaviour (Unit 7's own responsibility), not a
 * property a plain data class can enforce on itself.
 */

/**
 * The minimal, closed representation of a [KnowledgeItem]'s own current
 * lifecycle status -- see this file's Unit 4 header KDoc, "Why
 * [KnowledgeItemStatus] exists, and why it is exactly two values," for
 * the full reasoning. [ACTIVE] is the state a newly promoted
 * [KnowledgeItem] enters and remains in through ordinary revision;
 * [RETIRED] is entered only through Unit 7's own, not-yet-implemented
 * retirement act. No other value exists, and no transition between them
 * is validated by this enum itself.
 */
enum class KnowledgeItemStatus {
    ACTIVE,
    RETIRED,
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.1 (Lifecycle Event
 * Type Foundation). Introduces the closed, sealed lifecycle-event
 * contract `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md`
 * §5 requires, widens [KnowledgeItem.history] to hold it, and adds the
 * two new event variants (§5) that Contract Design Version 2 §3 names but
 * Unit 4 deliberately left unimplemented ("that guarantee is a future
 * write-path behaviour (Unit 7's own responsibility)"). This Unit adds
 * only type structure -- no lifecycle operation (revision evaluation,
 * supersession evaluation, retirement, restoration, compare-and-append,
 * or any ordering/concurrency mechanism) is implemented here; those
 * remain later Unit 7 sub-units' own, separately authorised
 * responsibility. Nothing below alters Unit 6's promotion behaviour,
 * promotion-result semantics, promotion factors, corroboration handling,
 * contradiction handling, or confidence handling.
 *
 * ## [KnowledgeLifecycleEvent] -- exactly the three fields genuinely common to every variant
 *
 * Scope Lock Clarification §5 authorises exactly three lifecycle-event
 * variants and no fourth: [KnowledgePromotion] (reused, unchanged in
 * meaning, for both initial promotion and subsequent revision),
 * [KnowledgeRetirement], and [KnowledgeRestoration]. The common sealed
 * contract exposes only [KnowledgeLifecycleEvent.knowledgeId],
 * [KnowledgeLifecycleEvent.occurredAt], and
 * [KnowledgeLifecycleEvent.basis] -- the three fields every one of
 * Scope Lock Clarification §5's own "carrying at minimum..." clauses
 * names for all three variants. An evidence or provenance reference is
 * deliberately **not** hoisted onto the common interface:
 * [KnowledgeRetirement] carries no such reference at all (§5: retirement
 * is a status change, never an evidential classification, and needs no
 * evidence to justify it), so forcing one onto every variant would
 * fabricate a field retirement's own governance never requires --
 * exactly the "speculative abstraction... to make the hierarchy visually
 * symmetrical" this Unit's own task instructs against. [resultingState]
 * is similarly not common: only [KnowledgePromotion] ever produces an
 * [EvidentialState] classification.
 *
 * ## Why append-only history, not this type, enforces immutability
 *
 * [KnowledgeLifecycleEvent] is a closed, sealed interface over three
 * immutable `data class` variants; it declares no method and enforces no
 * behaviour of its own. Scope Lock Clarification §6's append-only,
 * never-edited-in-place guarantee is a property of how
 * [KnowledgeItem.history] is populated by a future write path, not
 * something this type can enforce on itself -- mirroring Unit 4's own,
 * identical disclosure for [KnowledgeItem.history] before this Unit
 * existed.
 */
sealed interface KnowledgeLifecycleEvent {
    /** The [KnowledgeItem] this lifecycle event belongs to. */
    val knowledgeId: KnowledgeId

    /**
     * The wall-clock instant this event occurred. Not deterministic and
     * not required to be -- see [KnowledgePromotion.occurredAt]'s own,
     * unchanged KDoc and
     * `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md`
     * §12. This Unit does not implement, and this field does not itself
     * define, any ordering rule over multiple events -- that remains a
     * later Unit 7 sub-unit's own responsibility (Scope Lock
     * Clarification §10).
     */
    val occurredAt: Instant

    /**
     * The disclosed, non-blank, free-text explanation for this event --
     * the same free-text-over-new-type choice this file already makes
     * for [KnowledgePromotionDecision.Reject.reason] and
     * [KnowledgeCandidateEvaluation.Reject.basis], for the same reason.
     */
    val basis: String
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 4 (original type),
 * reused unchanged by Unit 7.1 as one [KnowledgeLifecycleEvent] variant.
 * The disclosed, read-only record documenting the basis for one
 * classification event -- an initial promotion, or a subsequent revision
 * -- of exactly one [KnowledgeItem]
 * (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
 * §12: "One such record exists per promotion and per subsequent
 * revision -- never only at initial promotion; each is attached to
 * exactly one classification event and never independently altered
 * afterward"; `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md`
 * §5: "`KnowledgePromotion` **is** a lifecycle event and **must not** be
 * duplicated by a separate 'revision' type"). This type is a plain data
 * holder only -- it is never caller-invocable, carries no `execute`
 * method or algorithm of any kind, and makes no permission or truth
 * determination. This Unit does not implement revision evaluation --
 * only Unit 6 (initial promotion) presently constructs a
 * [KnowledgePromotion] value; a later Unit 7 sub-unit (revision
 * evaluation) is the only other authorised constructor of one.
 *
 * [knowledgeId] identifies the [KnowledgeItem] this disclosure belongs
 * to. [evidenceReference] names the specific Memory Core record whose
 * evidence justified this particular classification event -- see this
 * file's Unit 4 header KDoc, "Why the evidence reference is
 * `MemoryCoreRecordReference`" -- deliberately its own field, separate
 * from [KnowledgeItem.evidenceReference], since a later revision may
 * cite different or additional evidence than the promotion that preceded
 * it (Contract Design Version 2 §3, Revision). [resultingState] is the
 * [EvidentialState] this event produced -- deliberately not part of the
 * common [KnowledgeLifecycleEvent] contract, since neither
 * [KnowledgeRetirement] nor [KnowledgeRestoration] produces one.
 * [occurredAt] carries no default value, consistent with every other
 * event timestamp in this file and in `MemoryCore.kt` (a data class must
 * not silently read the system clock). [basis] is the disclosed,
 * non-blank, free-text explanation of why this classification was
 * assigned -- the same free-text-over-new-type choice this file's own
 * [KnowledgePromotionDecision.Reject.reason] already makes, for the same
 * reason: a promotion or revision's basis weighs several named factors
 * together (Contract Design Version 2 §5), not a single structural rule
 * a closed enum could represent without misrepresenting it as one.
 */
data class KnowledgePromotion(
    override val knowledgeId: KnowledgeId,
    val evidenceReference: MemoryCoreRecordReference,
    val resultingState: EvidentialState,
    override val occurredAt: Instant,
    override val basis: String,
) : KnowledgeLifecycleEvent {
    init {
        require(basis.isNotBlank()) { "KnowledgePromotion.basis must not be blank" }
    }
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.1. The
 * [KnowledgeLifecycleEvent] variant recording that a [KnowledgeItem] was
 * retired (`docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md`
 * §5, §8). This type is a plain data holder only, exactly like
 * [KnowledgePromotion] -- it defines no `execute` method, performs no
 * transition itself, and this Unit does not implement the `ACTIVE` to
 * `RETIRED` operation that would construct one; that operation is a
 * later Unit 7 sub-unit's own, separately authorised responsibility.
 *
 * Deliberately carries **no** [resultingState] and **no** evidence or
 * provenance reference: Scope Lock Clarification §5 and §8 are explicit
 * that retirement "is a status change, never an evidential
 * classification," and requires no evidence to justify it -- only a
 * disclosed [basis]. Retirement does not delete, remove, or render
 * inaccessible the retired [KnowledgeItem], its prior history, or its
 * provenance (§8); this event is additive only, appended to
 * [KnowledgeItem.history] alongside, never in place of, every event that
 * preceded it (§6). It never implies historical erasure of any kind.
 *
 * [knowledgeId] identifies the retired [KnowledgeItem]. [occurredAt]
 * carries no default value, for the same reason [KnowledgePromotion.occurredAt]
 * does not. [basis] is the disclosed, non-blank, free-text explanation
 * required by §8, mirroring every other disclosed basis in this file.
 */
data class KnowledgeRetirement(
    override val knowledgeId: KnowledgeId,
    override val occurredAt: Instant,
    override val basis: String,
) : KnowledgeLifecycleEvent {
    init {
        require(basis.isNotBlank()) { "KnowledgeRetirement.basis must not be blank" }
    }
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.1. The
 * [KnowledgeLifecycleEvent] variant recording that a retired
 * [KnowledgeItem] was restored
 * (`docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §5,
 * §9). This type is a plain data holder only, exactly like
 * [KnowledgePromotion] and [KnowledgeRetirement] -- it defines no
 * `execute` method and performs no transition itself. This Unit does
 * not implement restoration eligibility, evidence resolution, the
 * qualifying-relationship requirement, or the non-resolution safety
 * boundary Scope Lock Clarification §9 describes; those remain a later
 * Unit 7 sub-unit's own, separately authorised responsibility. This type
 * defines only the event shape those future rules will construct.
 *
 * [knowledgeId] identifies the restored [KnowledgeItem]. [evidenceReference]
 * names the Memory Core evidence that re-establishes support, mirroring
 * [KnowledgePromotion.evidenceReference]'s own already-established shape
 * (§5: "a reference to the Memory Core evidence that re-establishes
 * support"). [occurredAt] carries no default value, for the same reason
 * every other event timestamp in this file does not. [basis] is the
 * disclosed, non-blank, free-text explanation required by §9.
 *
 * This event does not, and cannot by itself, erase, alter, or reverse
 * the [KnowledgeRetirement] event that preceded it -- that event remains
 * permanently visible in [KnowledgeItem.history] (§6, §9); restoration is
 * represented **only** by this new, appended event, never by removing or
 * rewriting the retirement it follows.
 */
data class KnowledgeRestoration(
    override val knowledgeId: KnowledgeId,
    val evidenceReference: MemoryCoreRecordReference,
    override val occurredAt: Instant,
    override val basis: String,
) : KnowledgeLifecycleEvent {
    init {
        require(basis.isNotBlank()) { "KnowledgeRestoration.basis must not be blank" }
    }
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 4. Represents a
 * single piece of promoted, durable knowledge
 * (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
 * §12). See this file's own Unit 4 header KDoc for the full reasoning
 * behind this type's shape, its shared identifier space with
 * [KnowledgeRecord], and why it duplicates none of that type's own
 * fields.
 *
 * [knowledgeId] reuses [KnowledgeId] unchanged -- this task's own
 * instruction not to invent a duplicate identifier type where an
 * authoritative one already exists, and the Migration Strategy's own
 * "avoid duplicate sources of truth" principle (Implementation Plan
 * Section 5), applied directly. [evidenceReference] names the Memory
 * Core record currently supporting this Knowledge Item -- see this
 * file's Unit 4 header KDoc, "Why the evidence reference is
 * `MemoryCoreRecordReference`." [provenanceReference] is the immutable,
 * identifier-only pointer (Unit 3) to that evidence's own originating
 * [Provenance] record -- carried directly on [KnowledgeItem] so a
 * caller can reach provenance detail without a second Memory Core hop
 * through [evidenceReference] first, per Contract Design Version 2 §6's
 * own "retrieve provenance references" capability. [evidentialState] is
 * the current Article IV classification (Unit 2) -- never a truth
 * determination (Contract Design Version 2 §1, §3). [status] defaults to
 * [KnowledgeItemStatus.ACTIVE], mirroring [Entity.status]'s own identical
 * "single, deterministic, always-correct default for a genuinely new
 * record" reasoning -- every promoted Knowledge Item begins current, by
 * definition. [history] defaults to an empty list, matching this file's
 * own [KnowledgeRecord.history] and `MemoryCore.kt`'s own collection-field
 * convention; nothing in this Unit populates it (see this file's Unit 4
 * header KDoc, "What this Unit does not implement"). [history] is the
 * sole constitutionally authoritative structured history for this
 * record -- [KnowledgeRecord.history] is a separate, legacy field
 * belonging to the still-production-wired legacy path and is never
 * synchronised with this one
 * (`docs/governance/PROGRAMME_3_UNIT_4_SCOPE_LOCK_CLARIFICATION.md` §1).
 *
 * **[history]'s element type, widened by Unit 7.1.** Originally
 * `List<KnowledgePromotion>`; widened to a `List` of [KnowledgeLifecycleEvent]
 * per `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §5,
 * which exercises exactly the latitude this Unit's own original KDoc
 * already reserved to Unit 7 ("that guarantee is a future write-path
 * behaviour (Unit 7's own responsibility)"). [history] remains a plain,
 * read-only `List` -- ordered, but not itself append-only, mutation-
 * preventing, or historically-preservative by any enforcement mechanism
 * of its own; it can hold any mix, in any order, of [KnowledgePromotion],
 * [KnowledgeRetirement], and [KnowledgeRestoration] values a caller
 * constructs, since this Unit adds only the type shape, not a write path
 * or an ordering rule (Scope Lock Clarification §§6, 10, both a later
 * Unit 7 sub-unit's own responsibility). No lifecycle version is stored
 * on this or any other field here -- Scope Lock Clarification §11 fixes
 * the lifecycle version as `history`'s own length, derived, never
 * independently stored.
 *
 * No `init` block validates [evidenceReference], [provenanceReference],
 * [evidentialState], or [status] beyond their own types' construction-
 * time checks -- consistent with this task's own instruction that this
 * Unit's types contain no validation beyond ordinary type safety.
 */
data class KnowledgeItem(
    val knowledgeId: KnowledgeId,
    val evidenceReference: MemoryCoreRecordReference,
    val provenanceReference: ProvenanceReference,
    val evidentialState: EvidentialState,
    val status: KnowledgeItemStatus = KnowledgeItemStatus.ACTIVE,
    val history: List<KnowledgeLifecycleEvent> = emptyList(),
)

/**
 * Programme 3, Knowledge Memory, Implementation Unit 4. A lightweight,
 * read-oriented handle to exactly one [KnowledgeItem]
 * (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
 * §12: "Lightweight identifier and summary for a Knowledge Item...
 * carries no independent state"). [knowledgeId] is the [KnowledgeItem]
 * this reference points to -- the only field this type carries.
 *
 * This type originally also carried a stored `summary: String` field.
 * The Unit 4 Governance Reconciliation found that a stored, captured-at-
 * construction field is in tension with "carries no independent state"
 * -- a value captured once and possibly held by a caller after the
 * underlying [KnowledgeItem] changes is structurally exactly the
 * "independent state" that phrase forbids, and neither Contract Design
 * version specifies a concrete field shape for "summary detail" beyond
 * prose. `docs/governance/PROGRAMME_3_UNIT_4_SCOPE_LOCK_CLARIFICATION.md`
 * §2 resolved this: [KnowledgeReference] is identifier-only for
 * Programme 3; any summary or task-scoped display content is a
 * retrieval-time projection belonging to Unit 9's own Knowledge Result
 * contract, never stored state owned by this type.
 *
 * Deliberately absent, per Contract Design Version 2 §12's own "carries
 * no independent state": [evidentialState], [provenanceReference],
 * [status], and [history] all remain exclusively on [KnowledgeItem]
 * itself -- a caller needing any of them must follow [knowledgeId] to
 * the full record, never rely on a copy embedded here, so that this type
 * can never drift out of sync with the record it points to (this row's
 * own "no silent rewriting" constitutional obligation).
 */
data class KnowledgeReference(
    val knowledgeId: KnowledgeId,
)

/**
 * Programme 3, Knowledge Memory, Implementation Unit 5. The caller-facing
 * proposal that existing Memory Core evidence be evaluated for promotion
 * (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
 * §2, §7, §12). This type is distinct from, and not a rename, adapter,
 * or evolution in place of, legacy [CandidateKnowledge] -- see
 * `docs/governance/PROGRAMME_3_UNIT_5_SCOPE_LOCK_CLARIFICATION.md` §1 for
 * the full reasoning. It represents nothing durable: a candidate that is
 * not promoted produces no [KnowledgeItem] and no Knowledge Memory record
 * of any kind (Contract Design Version 2 §3).
 *
 * [evidenceReference] is the only field this type carries, per Contract
 * Design Version 2's own three independent, consistent statements of this
 * contract's shape (§2: "a reference to existing Memory Core evidence
 * and nothing else evidential"; §7: "a reference to Memory Core content
 * -- never a copy"; §12: "a Memory Core evidence reference only"). It is
 * typed [MemoryCoreRecordReference], not [AssertionId], for the same
 * reason [KnowledgeItem.evidenceReference] and
 * [KnowledgePromotion.evidenceReference] are (see this file's Unit 4
 * header KDoc).
 *
 * No confidence field and no [EvidentialState] field exist on this type,
 * by design, not by omission -- `docs/governance/PROGRAMME_3_UNIT_5_SCOPE_LOCK_CLARIFICATION.md`
 * §4 confirms this makes a caller-declared confidence or evidential-state
 * value structurally impossible to construct through this contract at
 * all, a compile-time guarantee stronger than a runtime-rejected
 * malformed instance, and the intended satisfaction of Contract Design
 * Version 2 §5's Amendment 2 ("a Knowledge Candidate carrying either is
 * malformed and must be rejected on that basis").
 *
 * ## `explicitlyRequested` (Contract Design Version 2 §16, Phase 1
 * Amendment 5, and its extension, §§16.9-16.13)
 *
 * The one authorised, non-evidential submission-context field this type
 * carries beyond [evidenceReference]. It records whether this submission
 * arose in direct response to an explicit request -- a fact about the
 * circumstances of submission, never a claim about the underlying
 * proposition's truth, confidence, provenance, corroboration,
 * contradiction, or importance (§16.9's own enumerated prohibitions). It
 * does not permit any reasoning provider or other module to self-certify
 * the truth or evidential status of its own output (§16.9; Article XV,
 * applied here as an extension of that Article's separation principle,
 * not a literal application of its own reasoning-provider-specific
 * text).
 *
 * Nullable, and deliberately so: `true` and `false` are both honest,
 * caller-reported facts; `null` means the constructing subsystem
 * genuinely could not determine this at submission time. §16.3's
 * Guarantee 4 forbids fabricating either direction -- this field must
 * never default an unknown submission to `true`, and an implementation
 * must never coerce `null` to `false` merely for convenience. The only
 * authorised consumer of this field is Knowledge Memory's own promotion
 * evaluator (§16.5); it never by itself determines promotion (§16.3's
 * Guarantee 3; Section 5's multi-factor discipline applies unchanged),
 * and its contribution must be disclosed in the promotion basis whenever
 * relied upon (§16.5).
 *
 * No `init` block exists on this type -- there is no field beyond
 * ordinary type safety left to validate.
 */
data class KnowledgeCandidate(
    val evidenceReference: MemoryCoreRecordReference,
    val explicitlyRequested: Boolean? = null,
)

/**
 * Programme 3, Knowledge Memory, Implementation Unit 6 (Constitutional
 * Knowledge Promotion Pipeline). The outcome of one
 * [KnowledgeCandidateEvaluator] evaluation of one [KnowledgeCandidate] --
 * see
 * `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md` §3 for
 * why this type exists rather than reusing [KnowledgePromotionDecision]:
 * that type's own `Promote` case is hard-wired to [KnowledgeCategory], a
 * concept [KnowledgeItem] does not carry, making it structurally
 * incapable of representing this evaluation's result. This is not a
 * second legacy promotion hierarchy -- it belongs exclusively to the
 * constitutional [KnowledgeCandidate] path.
 *
 * [Promote] carries the proposed, not-yet-persisted [KnowledgeItem] and
 * its accompanying [KnowledgePromotion] disclosure record -- construction
 * only, per the Unit 6 Clarification §4; nothing implementing this type
 * writes either value anywhere. [Reject] carries a disclosed,
 * constitutionally required [basis], mirroring
 * [KnowledgePromotionDecision.Reject.reason]'s own identical free-text
 * treatment, for the same reason: multi-factor evidential weighing is not
 * a single structural rule a closed enum could represent without
 * misrepresenting it as one.
 */
sealed interface KnowledgeCandidateEvaluation {
    data class Promote(
        val item: KnowledgeItem,
        val promotion: KnowledgePromotion,
    ) : KnowledgeCandidateEvaluation

    data class Reject(
        val basis: String,
    ) : KnowledgeCandidateEvaluation {
        init {
            require(basis.isNotBlank()) { "KnowledgeCandidateEvaluation.Reject.basis must not be blank" }
        }
    }
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 6. The seam by which
 * Knowledge Memory evaluates a submitted [KnowledgeCandidate] against the
 * constitutional multi-factor promotion requirements (Contract Design
 * Version 2 §5, Amendments 1 and 2; Article XI). Structurally analogous
 * to [KnowledgePromotionPolicy], but operating on the new,
 * constitutionally governed model rather than the legacy one -- the two
 * are independent seams, never adapted into one another (Unit 6
 * Clarification §1).
 *
 * This interface performs no persistence, no lifecycle transition, and
 * no permission decision of its own -- see
 * [DefaultKnowledgeCandidateEvaluator]'s own KDoc for the concrete
 * factors one implementation considers and the structural prerequisites
 * it checks first. Deliberately not `suspend`, per this Unit's own
 * authorised signature -- [DefaultKnowledgeCandidateEvaluator] bridges to
 * [MemoryRetrieval]'s `suspend` methods internally (see its own KDoc)
 * rather than this interface propagating suspension outward; a future
 * implementation is free to introduce its own, separate suspending seam
 * without changing this one, exactly as [KnowledgePromotionPolicy]'s own
 * precedent left room for parallel seams rather than retrofitting a
 * shared one.
 */
interface KnowledgeCandidateEvaluator {
    fun evaluate(candidate: KnowledgeCandidate): KnowledgeCandidateEvaluation
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.2 (Knowledge
 * Revision and Supersession Evaluation). The outcome of one
 * [KnowledgeRevisionEvaluator] evaluation of one proposed revision of an
 * existing [KnowledgeItem]
 * (`docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §7,
 * §15). A distinct, independent contract from [KnowledgeCandidateEvaluation]
 * -- initial promotion (Unit 6) and revision (Unit 7.2) are separate,
 * governed seams, never adapted into one another, exactly as the Unit 6
 * Clarification already established for [KnowledgeCandidateEvaluation]
 * relative to the legacy [KnowledgePromotionDecision].
 *
 * Represents only the three result outcomes this Unit is authorised to
 * implement, out of Scope Lock Clarification §15's full six-outcome
 * closed set: [Applied], [NotQualifyingRevision], and
 * [StructurallyUnresolvable]. Stale-version conflict, restoration
 * outcomes, and retirement outcomes are deliberately absent -- they
 * depend on lifecycle sequence tracking, retirement, and restoration,
 * none of which this Unit implements; a later Unit 7 sub-unit introduces
 * them without altering the three cases here.
 */
sealed interface KnowledgeRevisionEvaluation {
    /**
     * The submitted evidence qualified as a revision (Section 7's
     * Revision Qualification Gate) and was evaluated. [item] is the new,
     * proposed [KnowledgeItem] value -- [KnowledgeItem.history] carries
     * exactly one more entry than the [KnowledgeItem] this evaluation
     * started from, and every prior entry is preserved unchanged, per
     * Scope Lock Clarification §6's append-only, never-rewritten
     * guarantee. [promotion] is the same, newly appended
     * [KnowledgePromotion] value already present as the final element of
     * [item]'s own [KnowledgeItem.history], surfaced here directly for a
     * caller's convenience, mirroring [KnowledgeCandidateEvaluation.Promote]'s
     * own identical shape. Construction only -- nothing implementing this
     * type writes [item] or [promotion] anywhere; persistence remains a
     * later, separately authorised responsibility.
     */
    data class Applied(
        val item: KnowledgeItem,
        val promotion: KnowledgePromotion,
    ) : KnowledgeRevisionEvaluation

    /**
     * The submitted evidence resolved structurally, but no relationship
     * or status transition authorised by Section 7's Revision
     * Qualification Gate links it to the target [KnowledgeItem]'s
     * already-cited evidence. No lifecycle event is appended and the
     * existing [KnowledgeItem] is returned unchanged by the caller (this
     * type carries no [KnowledgeItem] value, since none was produced).
     * This is a structural qualification failure, never an evidential-
     * sufficiency judgement -- Section 7 expressly disclaims any
     * numerical materiality threshold, and this outcome must never be
     * described as, or treated as, evidential insufficiency.
     */
    data class NotQualifyingRevision(
        val basis: String,
    ) : KnowledgeRevisionEvaluation {
        init {
            require(basis.isNotBlank()) { "KnowledgeRevisionEvaluation.NotQualifyingRevision.basis must not be blank" }
        }
    }

    /**
     * Either the target [KnowledgeItem]'s own already-cited evidence
     * reference, or the newly submitted evidence reference, could not be
     * resolved through [MemoryRetrieval] -- mirroring
     * [KnowledgeCandidateEvaluation.Reject]'s own identical structural-
     * prerequisite category. This evaluator does not, and structurally
     * cannot, distinguish non-existence from an unauthorised access
     * decision here, for the same reason [DefaultKnowledgeCandidateEvaluator]
     * does not (Contract Design Version 2 Section 7).
     */
    data class StructurallyUnresolvable(
        val basis: String,
    ) : KnowledgeRevisionEvaluation {
        init {
            require(basis.isNotBlank()) { "KnowledgeRevisionEvaluation.StructurallyUnresolvable.basis must not be blank" }
        }
    }
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.2. The seam by
 * which Knowledge Memory decides whether newly submitted Memory Core
 * evidence qualifies as, and how it affects, a revision of an existing,
 * already-promoted [KnowledgeItem]
 * (`docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §7).
 * Structurally analogous to [KnowledgeCandidateEvaluator], but a distinct,
 * independent seam -- initial promotion and revision are never adapted
 * into one another.
 *
 * This interface performs no persistence, no compare-and-append, no
 * lifecycle-sequence enforcement, and no retirement or restoration
 * decision of its own -- see [DefaultKnowledgeRevisionEvaluator]'s own
 * KDoc for the concrete qualification and classification rules one
 * implementation applies. [occurredAt] is supplied by the caller, not
 * read internally from the system clock, so that qualification and
 * classification remain deterministic, testable pure functions of their
 * own inputs -- a disclosed difference from [DefaultKnowledgeCandidateEvaluator],
 * which does read the clock internally, since that class (unlike this
 * one) is itself the sole authorised constructor of its own
 * [KnowledgePromotion.occurredAt] value.
 */
interface KnowledgeRevisionEvaluator {
    fun evaluate(
        item: KnowledgeItem,
        newEvidenceReference: MemoryCoreRecordReference,
        occurredAt: Instant,
    ): KnowledgeRevisionEvaluation
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.3 (Knowledge
 * Retirement Evaluation). The outcome of one [KnowledgeRetirementEvaluator]
 * evaluation of one proposed retirement of an existing [KnowledgeItem]
 * (`docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §8,
 * §15). A distinct, independent contract from [KnowledgeRevisionEvaluation]
 * -- revision (Unit 7.2) and retirement (Unit 7.3) are separate, governed
 * seams, never adapted into one another.
 *
 * Represents only the two result outcomes this Unit is authorised to
 * implement, per the Unit 7.3 Targeted Retirement Boundary Review's own
 * confirmed conclusions: [Applied] and [NotPermittedFromCurrentStatus].
 * `StructurallyUnresolvable` is deliberately absent -- Section 15 names
 * that outcome only for "evidence a revision or restoration cites";
 * retirement cites no evidence and resolves nothing through
 * [MemoryRetrieval], so no failure mode exists for it to express.
 * Stale-version conflict, `NotQualifyingRevision`, and restoration
 * outcomes are also absent, for the same reason they are absent from
 * [KnowledgeRevisionEvaluation] -- they belong to lifecycle-sequence
 * enforcement, revision qualification, and restoration, none of which
 * this Unit implements.
 */
sealed interface KnowledgeRetirementEvaluation {
    /**
     * The submitted [KnowledgeItem] was [KnowledgeItemStatus.ACTIVE] and
     * was retired. [item] is the new, proposed [KnowledgeItem] value --
     * [KnowledgeItem.status] is [KnowledgeItemStatus.RETIRED] and
     * [KnowledgeItem.history] carries exactly one more entry than the
     * [KnowledgeItem] this evaluation started from, with every prior
     * entry preserved unchanged (Scope Lock Clarification §6, §8).
     * [KnowledgeItem.evidenceReference], [KnowledgeItem.provenanceReference],
     * and [KnowledgeItem.evidentialState] are all carried over unchanged --
     * retirement is a status change, never an evidential classification
     * (§5, §8). [retirement] is the same, newly appended
     * [KnowledgeRetirement] value already present as the final element of
     * [item]'s own [KnowledgeItem.history], surfaced here directly for a
     * caller's convenience, mirroring [KnowledgeRevisionEvaluation.Applied]'s
     * own identical shape. Construction only -- nothing implementing this
     * type writes [item] or [retirement] anywhere; persistence remains a
     * later, separately authorised responsibility.
     */
    data class Applied(
        val item: KnowledgeItem,
        val retirement: KnowledgeRetirement,
    ) : KnowledgeRetirementEvaluation

    /**
     * The submitted [KnowledgeItem] was not [KnowledgeItemStatus.ACTIVE]
     * at the time retirement was attempted -- retirement is defined,
     * exhaustively, as the `ACTIVE`-to-`RETIRED` transition (§8), so a
     * submission against an item already [KnowledgeItemStatus.RETIRED]
     * names no transition this document defines at all. No lifecycle
     * event is appended and the existing [KnowledgeItem] is returned
     * unchanged by the caller (this type carries no [KnowledgeItem]
     * value, since none was produced). This is rejected, never treated as
     * a disclosed no-op and never treated as a new permitted event (§8),
     * mirroring Memory Core Scope Lock §8's own governing convention that
     * every transition it does not define is refused, not quietly
     * absorbed.
     */
    data class NotPermittedFromCurrentStatus(
        val basis: String,
    ) : KnowledgeRetirementEvaluation {
        init {
            require(basis.isNotBlank()) { "KnowledgeRetirementEvaluation.NotPermittedFromCurrentStatus.basis must not be blank" }
        }
    }
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.3. The seam by
 * which Knowledge Memory decides whether an already-promoted
 * [KnowledgeItem] may be retired
 * (`docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §8).
 * Structurally analogous to [KnowledgeRevisionEvaluator], but a distinct,
 * independent seam -- revision and retirement are never adapted into one
 * another.
 *
 * This interface performs no persistence, no compare-and-append, no
 * lifecycle-sequence enforcement, no restoration decision, and consults
 * no [MemoryRetrieval] dependency of any kind -- see
 * [DefaultKnowledgeRetirementEvaluator]'s own KDoc, and the Unit 7.3
 * Targeted Retirement Boundary Review's own confirmed conclusion that
 * retirement's qualification depends only on [KnowledgeItem.status] and
 * the caller-supplied [basis], never on Memory Core evidence. [occurredAt]
 * is supplied by the caller, not read internally from the system clock,
 * mirroring [KnowledgeRevisionEvaluator]'s own identical, disclosed
 * choice, for the same reason: qualification and outcome remain
 * deterministic, testable pure functions of their own inputs.
 */
interface KnowledgeRetirementEvaluator {
    fun evaluate(
        item: KnowledgeItem,
        basis: String,
        occurredAt: Instant,
    ): KnowledgeRetirementEvaluation
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 8 (Knowledge
 * Submission). The outcome of one [KnowledgeSubmission.submit] call --
 * see `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`
 * ("the Unit 8 Clarification"), §9, for the full constitutional reasoning
 * this type implements exactly and nothing more.
 *
 * Three outcomes, not two and not four, per the Unit 8 Clarification's own
 * binding determination:
 *
 * - [NotAuthorised] -- Evaluation B (the submission-act permission check,
 *   `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
 *   §7, Amendment 8) did not approve. This is never, and must never be
 *   read as, a judgement on the candidate's own evidential merit -- it
 *   says nothing about whether the candidate would have been promoted,
 *   only that the submission act itself was not authorised (Unit 8
 *   Clarification §9: "denial reflects nothing about the candidate's
 *   evidential merit").
 * - [Declined] -- Evaluation B approved, [KnowledgeCandidateEvaluator]
 *   was invoked, and it returned [KnowledgeCandidateEvaluation.Reject].
 *   [Declined.basis] carries that rejection's own
 *   [KnowledgeCandidateEvaluation.Reject.basis] unchanged -- this type
 *   never re-authors, re-classifies, or supplements the evaluator's own
 *   disclosed reasoning.
 * - [Promoted] -- Evaluation B approved, the evaluator returned
 *   [KnowledgeCandidateEvaluation.Promote], and the resulting
 *   [KnowledgeItem] (whose own [KnowledgeItem.history] already carries
 *   [promotion] as its sole entry at this point) was durably persisted.
 *   [item] and [promotion] are exactly the evaluator's own two `Promote`
 *   fields, surfaced unchanged -- this type constructs neither.
 *
 * Deliberately not a superset of, rename of, or replacement for
 * [KnowledgeCandidateEvaluation] -- that type remains Unit 6's own,
 * closed, two-outcome contract, unmodified and unreopened by this Unit
 * (Unit 8 Clarification §9: "does not authorise altering
 * `KnowledgeCandidateEvaluation`... to accommodate [a permission
 * denial]"). [KnowledgeSubmissionDisposition] is a distinct,
 * narrower-scoped, caller-facing contract belonging exclusively to the
 * Knowledge Submission boundary; [KnowledgeCandidateEvaluator] itself
 * never constructs, returns, or references it.
 */
sealed interface KnowledgeSubmissionDisposition {

    data class Promoted(
        val item: KnowledgeItem,
        val promotion: KnowledgePromotion,
    ) : KnowledgeSubmissionDisposition

    data class Declined(
        val basis: String,
    ) : KnowledgeSubmissionDisposition {
        init {
            require(basis.isNotBlank()) { "KnowledgeSubmissionDisposition.Declined.basis must not be blank" }
        }
    }

    data class NotAuthorised(
        val reason: String,
    ) : KnowledgeSubmissionDisposition {
        init {
            require(reason.isNotBlank()) { "KnowledgeSubmissionDisposition.NotAuthorised.reason must not be blank" }
        }
    }
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 8. Knowledge
 * Memory's single public write boundary for the constitutional
 * [KnowledgeCandidate] path -- see
 * `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`, in
 * full, for the constitutional reasoning this interface's one operation
 * implements. Not a rename, adapter, or evolution of legacy
 * [KnowledgeStore.remember] -- the two remain separate, non-overlapping
 * seams (`docs/governance/PROGRAMME_3_UNIT_5_SCOPE_LOCK_CLARIFICATION.md`
 * §1, §3), and this interface neither calls nor depends on [KnowledgeStore]
 * in any way.
 *
 * One operation only. [submit] performs Evaluation B, invokes
 * [KnowledgeCandidateEvaluator] exactly once on approval, and persists
 * only a successful promotion -- see the default implementation
 * (`src/runtime/DefaultKnowledgeSubmission.kt`) for the concrete
 * sequencing. This interface itself expresses no evaluation policy, no
 * promotion criteria, no lifecycle operation, no retrieval capability,
 * and no storage mechanic of any kind -- every one of those remains
 * exactly where existing, closed governance already placed it.
 *
 * [requestingPrincipalId] is a required, explicit parameter -- never
 * carried on [KnowledgeCandidate] itself, and never assumed from ambient
 * context (Unit 8 Clarification §6) -- mirroring [MemoryCore]'s,
 * [MemoryRetrieval]'s, and [EvidenceCustodian]'s own identical,
 * already-established treatment of caller identity.
 */
interface KnowledgeSubmission {
    suspend fun submit(requestingPrincipalId: PrincipalId, candidate: KnowledgeCandidate): KnowledgeSubmissionDisposition
}

/**
 * Programme 3, Knowledge Memory, Implementation Unit 9.1 (Knowledge Query
 * and Knowledge Result Contracts). Declares the public request and
 * response shapes `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`
 * ("the Unit 9 Contract Design") §4 fixes at the properties level, and
 * the [KnowledgeRetrieval] interface itself -- see
 * `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md`
 * §4, Unit 9.1, for this Unit's own objective, dependencies, and
 * verification requirements. This Unit implements no query execution, no
 * filtering, no ordering rule, no staleness-detection mechanism, no
 * permission enforcement, and no runtime composition -- every type below
 * is a plain, immutable data holder or an unimplemented interface
 * declaration, exactly like every other contract-tier Unit in this file
 * before its own concrete implementation existed ([KnowledgeSubmission]
 * before `DefaultKnowledgeSubmission`, [KnowledgeCandidateEvaluator]
 * before `DefaultKnowledgeCandidateEvaluator`).
 *
 * ## Why [KnowledgeRetrievalQuery], not a reuse of legacy [KnowledgeQuery]
 *
 * Contract Design Version 2 §2 lists "Knowledge Query" as "Belongs...
 * Unchanged from Version 1" -- read, as [KnowledgeItem] and
 * [KnowledgeCandidate] both already establish for their own, identically
 * worded table rows, as the *concept* belonging unchanged, not as a
 * literal reuse of the existing Kotlin declaration. Reusing legacy
 * [KnowledgeQuery] here is structurally impossible in any case: its own
 * return path ([KnowledgeStore.retrieve]: `List<KnowledgeRecord>`) answers
 * with the legacy, flat [KnowledgeRecord] shape, never the V2
 * evidential-state/provenance/staleness-bearing [KnowledgeItem] shape the
 * Unit 9 Contract Design's own Knowledge Result requires -- the same
 * reasoning `docs/governance/PROGRAMME_3_UNIT_5_SCOPE_LOCK_CLARIFICATION.md`
 * §1 already gives for [KnowledgeCandidate] being distinct from legacy
 * [CandidateKnowledge].
 *
 * [requestingPrincipalId] is deliberately **not** a field on
 * [KnowledgeRetrievalQuery] -- the Unit 9 Contract Design §4's own
 * "retrieval interface" bullet fixes the requesting principal as a
 * separate parameter to the retrieval operation itself, mirroring
 * [KnowledgeSubmission.submit]'s own identical, already-established
 * treatment (Errata 004; the Unit 8 Clarification §6), never carried on
 * the payload type. [correlationId] is required per the Unit 9 Contract
 * Design §4's own explicit addition: "a Knowledge Query must additionally
 * be capable of carrying an explicit correlation identifier... explicit,
 * never ambient." [relevance] and [maximumResults] mirror legacy
 * [KnowledgeQuery]'s own already-proven, structural-matching-only field
 * shape -- reused here as the minimal, precedented representation of
 * "caller-supplied structural matching criteria," per the Unit 9 Contract
 * Design §4's own "nothing else" limit; no ranking instruction, semantic
 * hint, or permission assertion field is added. [category] is
 * deliberately omitted -- Contract Design Version 2 never names it as a
 * V2-tier retrieval concept, and this Unit adds no field beyond what the
 * Contract Design's own text requires.
 *
 * **Widened by Programme 3, Knowledge Memory, Implementation Unit 9.4
 * (Retirement and Supersession Retrieval-Shape Decision) -- the smallest
 * additive widening the Unit 9 Contract Design §6 itself already named as
 * one of three lawful outcomes.** [includeRetired] is added because Unit
 * 9.4's own planning determination
 * (`docs/reviews/PROGRAMME_3_UNIT_9_4_RETIREMENT_SUPERSESSION_SHAPING_COMPLETION_REVIEW.md`)
 * concluded that an ordinary Knowledge Query must exclude
 * [KnowledgeItemStatus.RETIRED] items by default (an ordinary,
 * task-scoped request for "relevant, already-promoted knowledge" -- Unit
 * 9 Contract Design §1 -- is naturally a request for knowledge Knowledge
 * Memory still holds as current, not knowledge it has already marked "no
 * longer current"), while Contract Design Version 2 §3's own "retirement
 * never implies deletion... remain retained and retrievable" guarantee
 * forecloses a permanent, non-overridable exclusion, since Knowledge
 * Retrieval is "the sole public path through which anything outside
 * Knowledge Memory may observe promoted knowledge" (Unit 9 Contract
 * Design §1) and a retired item unreachable through it, ever, would be
 * retrievable in name only. The Unit 9 Contract Design §6 itself already
 * names exactly this resolution as one of three lawful outcomes --
 * "included only under an explicit caller criterion" -- so this widening
 * exercises authority the adopted Contract Design already disclosed,
 * never a newly invented one. [includeRetired] is a **structural**
 * criterion about which of an already-permitted caller's own matched
 * items to include, never a permission signal -- Unit 9 Contract Design
 * §6's own closing paragraph ("lifecycle status is never a substitute
 * for, or determinant of, a permission decision") applies to this field
 * exactly as it applies to [KnowledgeItem.status] itself: a retired item
 * a caller requests via `includeRetired = true` remains subject to
 * whatever permission classification Unit 9.5 eventually wires, never
 * exempted from it by virtue of this field. Defaults to `false` --
 * callers who construct a [KnowledgeRetrievalQuery] without naming this
 * parameter receive the new, deliberate default (excluded), not the old,
 * disclosed-as-provisional "unconditionally included" placeholder
 * behaviour Units 9.2 and 9.3 left in place pending this Unit's own
 * decision. [KnowledgeResultEntry] itself required no corresponding
 * widening: a retired item, once included, already discloses its own
 * retired status honestly via the unchanged [KnowledgeItem.status] field
 * it already carries -- see [DefaultKnowledgeRetrieval]'s own "Lifecycle
 * shaping" KDoc for the full reasoning, including why supersession
 * required no widening or new field at all.
 */
data class KnowledgeRetrievalQuery(
    val relevance: String,
    val correlationId: String,
    val maximumResults: Int,
    val includeRetired: Boolean = false,
) {
    init {
        require(relevance.isNotBlank()) { "KnowledgeRetrievalQuery.relevance must not be blank" }
        require(correlationId.isNotBlank()) { "KnowledgeRetrievalQuery.correlationId must not be blank" }
        require(maximumResults >= 1) { "KnowledgeRetrievalQuery.maximumResults must be at least 1, was $maximumResults" }
    }
}

/**
 * One [KnowledgeItem] disclosed by a [KnowledgeRetrievalResult], paired
 * with its own mandatory staleness disclosure (Contract Design Version 2
 * §3, §6, Amendment 7; the Unit 9 Contract Design §2's own "never
 * optional, never inferred from its own absence" requirement). [staleness]
 * is non-nullable specifically because a nullable representation would
 * itself violate that requirement -- an absent value is not a
 * representable state of this field at all.
 *
 * **[staleness]'s element type, widened by Unit 9.3.** Originally
 * `stale: Boolean` (Unit 9.1's own original shape, declared before Unit
 * 9.3's own staleness-detection mechanism existed to compute it) -- a
 * non-nullable `Boolean` for exactly the same "an absent value is not
 * representable" reasoning above, and at the time thought sufficient,
 * since Unit 9.1's own scope declared only the shape and implemented no
 * staleness-detection mechanism of its own (Unit 9 Implementation Plan,
 * Unit 9.3). Widened to [staleness] ([StalenessDisclosure]) below,
 * because Unit 9.3's own Independent
 * Constitutional Review (`docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`)
 * found a binary signal genuinely insufficient: Contract Design Version 2
 * §3 (Amendment 7) defines staleness as a *confirmed* condition ("the
 * underlying evidence's status changes... before Knowledge Memory has
 * re-evaluated"), never an inferred one, and no mechanism available to
 * Knowledge Retrieval -- forbidden from querying Memory Core (Unit 9
 * Contract Design §3) -- can confirm that condition either way for any
 * item. A `Boolean` forces every item into a confident-looking `true` or
 * `false` regardless, which for the common case (a recently classified
 * item whose evidence may already have changed) silently discloses
 * `false` -- an unconfirmed claim of freshness presented with the same
 * certainty a genuine confirmation would carry, precisely the "an unknown
 * value is never fabricated, defaulted, or inferred without a disclosed
 * inference step" failure this repository's own governing principle
 * (restated for Knowledge Memory's own durable layer by the Unit 9
 * Contract Design's own Context section) exists to prevent, and precisely
 * what Article XIII's "uncertainty... must never be concealed" forbids.
 * [StalenessDisclosure] replaces that false confidence with an honestly
 * bounded signal instead. See [StalenessDisclosure]'s own KDoc for the
 * full account of what each of its four values means and who may assign
 * it.
 */
data class KnowledgeResultEntry(
    val item: KnowledgeItem,
    val staleness: StalenessDisclosure,
)

/**
 * Programme 3, Knowledge Memory, Implementation Unit 9.3 (Staleness
 * Disclosure). What a [KnowledgeResultEntry] discloses about whether its
 * own [KnowledgeItem]'s classification remains current, per Contract
 * Design Version 2 §3 (Amendment 7)'s own governing definition: staleness
 * is *confirmed* only by comparing the underlying Memory Core evidence's
 * current status against its status when the classification was last
 * computed -- a comparison no mechanism available to Knowledge Retrieval
 * can lawfully perform, since doing so would require exactly the Memory
 * Core query Knowledge Retrieval is structurally forbidden from making
 * (Unit 9 Contract Design §3).
 *
 * Four values, in two disclosed groups:
 *
 * - [CONFIRMED_CURRENT] and [CONFIRMED_STALE] represent the two outcomes
 *   Contract Design Version 2 §3 itself actually defines. **Neither is
 *   ever assigned by any mechanism this Programme has built as of Unit
 *   9.3** -- both are reserved for a future, genuinely Memory-Core-aware
 *   mechanism (for example, one subscribing to Memory Core's own
 *   published events, the "continuous monitoring" direction the Unit 9
 *   Implementation Plan's own Unit 9.3 entry names but does not require),
 *   should one ever be authorised. Reserving them now means that future
 *   mechanism can be introduced without a second breaking change to this
 *   type.
 * - [POSSIBLY_STALE] and [INDETERMINATE] are the two values Unit 9.3's
 *   own age-based mechanism (`DefaultKnowledgeRetrieval`) actually
 *   assigns, and are explicitly, honestly distinct from the two values
 *   above: neither claims to confirm anything about the underlying
 *   evidence's own status. [POSSIBLY_STALE] discloses only that an
 *   unusually long time has elapsed since the classification was last
 *   computed, without re-confirmation -- a true, honestly-held fact about
 *   elapsed time, offered as a reason a caller might choose to re-verify,
 *   never as a claim that the evidence actually changed.
 *   [INDETERMINATE] discloses that this class has no basis, in either
 *   direction, to say anything about the item's own currency -- the
 *   honest default for every item its own age-based signal does not
 *   distinguish as [POSSIBLY_STALE]. Critically, [INDETERMINATE] is
 *   deliberately *not* a claim of freshness -- it replaces what an
 *   age-based `Boolean` `false` would have silently implied, without
 *   implying anything in its place.
 */
enum class StalenessDisclosure {
    /**
     * Reserved. A future, genuinely Memory-Core-aware mechanism has
     * confirmed the underlying evidence's own status has not changed
     * since this classification was last computed. Never assigned by any
     * mechanism this Programme has built as of Unit 9.3.
     */
    CONFIRMED_CURRENT,

    /**
     * Reserved. A future, genuinely Memory-Core-aware mechanism has
     * confirmed the underlying evidence's own status has changed since
     * this classification was last computed, per Contract Design Version
     * 2 §3's own governing definition of staleness. Never assigned by any
     * mechanism this Programme has built as of Unit 9.3.
     */
    CONFIRMED_STALE,

    /**
     * An unusually long time has elapsed since this classification was
     * last computed, with no re-evaluation since -- Unit 9.3's own
     * disclosed, honest, age-based signal. Not a claim that the
     * underlying evidence's own status has changed; only that this
     * classification has gone unusually long without re-confirmation, and
     * may warrant it.
     */
    POSSIBLY_STALE,

    /**
     * This class has no basis, in either direction, to disclose whether
     * the item remains current. The honest default for any item Unit
     * 9.3's own age-based signal does not distinguish as
     * [POSSIBLY_STALE] -- deliberately not a claim of freshness.
     */
    INDETERMINATE,
}

/**
 * What [KnowledgeRetrieval.retrieve] returns on a successful, authorised
 * query -- the Unit 9 Contract Design §4's own "Knowledge Result" bundle:
 * zero or more [KnowledgeResultEntry] values, each already carrying its
 * own [KnowledgeItem] (whose own [KnowledgeItem.evidentialState] and
 * [KnowledgeItem.provenanceReference] fields already satisfy the Contract
 * Design's own "evidential-state disclosures... provenance references"
 * requirement without this type duplicating either) and its own mandatory
 * staleness disclosure. An empty [entries] list is a valid, successful
 * result (Contract Design §4: "a call that finds nothing matching returns
 * an empty, valid Knowledge Result -- never an error") -- never confused
 * with [KnowledgeRetrievalDisposition.NotAuthorised], since the two are
 * distinct sealed variants (see [KnowledgeRetrievalDisposition]), not
 * distinguished by list emptiness. [entries] is a plain, ordinary `List`
 * -- this Unit implements no ordering rule, deduplication, or truncation
 * logic of its own (Unit 9 Implementation Plan, Unit 9.2); whatever order
 * a future retrieval engine produces is preserved by this type exactly as
 * received, never re-ordered by construction.
 */
data class KnowledgeRetrievalResult(
    val entries: List<KnowledgeResultEntry>,
)

/**
 * The outcome of one [KnowledgeRetrieval.retrieve] call. Two variants,
 * not five: the Unit 9 Contract Design §9's own Error Model names five
 * distinguishable outcomes, but only two require a sealed-type variant
 * here, mirroring exactly how `DefaultKnowledgeSubmission` and
 * `InMemoryMemoryCore` already express the other three:
 *
 * - **Invalid query** is prevented by [KnowledgeRetrievalQuery]'s own
 *   construction-time validation -- a malformed query is already
 *   impossible to construct through the public contract, a compile-time
 *   guarantee, mirroring [KnowledgeSubmission]'s own identical structural
 *   admissibility reasoning (the Unit 8 Clarification §8, step 2). No
 *   runtime "invalid query" case exists for this type to represent.
 * - **Unavailable data** and **implementation failure** are genuine
 *   infrastructure faults, never returned values -- they propagate as
 *   thrown exceptions, mirroring `InMemoryMemoryCore`'s and
 *   `DefaultKnowledgeSubmission`'s own shared "no `try`/`catch`, faults
 *   propagate unchanged" discipline (Unit 9 Contract Design §9). Adding a
 *   variant for either here would misrepresent a genuine fault as an
 *   ordinary, expected outcome.
 * - [NotAuthorised] and [Retrieved] are the two remaining outcomes, and
 *   the only two this type declares -- **empty** and **non-empty**
 *   results are both [Retrieved], distinguished only by
 *   [KnowledgeRetrievalResult.entries]'s own list contents, never by a
 *   separate disposition variant (Contract Design §4: an empty result is
 *   "never confusable with a permission denial").
 *
 * This is a distinct, narrower-scoped contract belonging exclusively to
 * the Knowledge Retrieval boundary -- it is never a superset of, rename
 * of, or replacement for [KnowledgeSubmissionDisposition], mirroring that
 * type's own identical relationship to [KnowledgeCandidateEvaluation].
 *
 * **Contract status of this two-variant shape.** This is a deliberate,
 * authorised Unit 9.1 representation, reasoned above -- it is not
 * declared constitutionally immutable, and this Unit does not claim it
 * is the only shape the Unit 9 Contract Design's own Error Model could
 * ever be represented by. Any future addition of a third variant, or any
 * other change to this type's own outcome representation, requires its
 * own explicit, disclosed contract amendment or later authorised
 * governance step -- never a silent extension introduced merely because
 * Kotlin's own sealed-interface mechanism permits another subclass to be
 * added. No later Unit may treat that technical possibility as
 * governance authority to do so.
 */
sealed interface KnowledgeRetrievalDisposition {

    data class Retrieved(
        val result: KnowledgeRetrievalResult,
    ) : KnowledgeRetrievalDisposition

    data class NotAuthorised(
        val reason: String,
    ) : KnowledgeRetrievalDisposition {
        init {
            require(reason.isNotBlank()) { "KnowledgeRetrievalDisposition.NotAuthorised.reason must not be blank" }
        }
    }
}

/**
 * Knowledge Memory's single public read boundary for the constitutional
 * [KnowledgeItem] model -- see the Unit 9 Contract Design and the adopted
 * `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` for the
 * full constitutional reasoning this interface's one operation will
 * eventually be gated by. This Unit declares the contract only; no class
 * implements it yet -- query execution, filtering, ordering, staleness
 * computation, and permission enforcement are each a later, separately
 * authorised Unit (Unit 9 Implementation Plan, Units 9.2 through 9.5),
 * mirroring exactly how [KnowledgeSubmission] existed as a pure contract
 * before `DefaultKnowledgeSubmission` was built.
 *
 * [requestingPrincipalId] is a required, explicit parameter -- never
 * carried on [KnowledgeRetrievalQuery] itself, and never assumed from
 * ambient context, mirroring [KnowledgeSubmission.submit]'s own identical
 * treatment (Unit 9 Contract Design §5).
 *
 * This interface performs no write of any kind, no Memory Core query of
 * its own, no lifecycle transition, and no Reasoning Context composition
 * -- every one of those remains exactly where existing, closed governance
 * already placed it (Unit 9 Contract Design §1, §3).
 */
interface KnowledgeRetrieval {
    suspend fun retrieve(requestingPrincipalId: PrincipalId, query: KnowledgeRetrievalQuery): KnowledgeRetrievalDisposition
}
