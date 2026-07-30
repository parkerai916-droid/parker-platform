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
 * Programme 3, Knowledge Memory, Implementation Unit 4. The disclosed,
 * read-only record documenting the basis for one classification event --
 * an initial promotion, or a subsequent revision -- of exactly one
 * [KnowledgeItem]
 * (`docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`
 * §12: "One such record exists per promotion and per subsequent
 * revision -- never only at initial promotion; each is attached to
 * exactly one classification event and never independently altered
 * afterward"). This type is a plain data holder only -- it is never
 * caller-invocable, carries no `execute` method or algorithm of any
 * kind, and makes no permission or truth determination. Nothing in this
 * Unit constructs a [KnowledgePromotion] value; that begins with Unit 6
 * (initial promotion) and Unit 7 (subsequent revision).
 *
 * [knowledgeId] identifies the [KnowledgeItem] this disclosure belongs
 * to. [evidenceReference] names the specific Memory Core record whose
 * evidence justified this particular classification event -- see this
 * file's Unit 4 header KDoc, "Why the evidence reference is
 * `MemoryCoreRecordReference`" -- deliberately its own field, separate
 * from [KnowledgeItem.evidenceReference], since a later revision may
 * cite different or additional evidence than the promotion that preceded
 * it (Contract Design Version 2 §3, Revision). [resultingState] is the
 * [EvidentialState] this event produced. [occurredAt] carries no default
 * value, consistent with every other event timestamp in this file and in
 * `MemoryCore.kt` (a data class must not silently read the system
 * clock). [basis] is the disclosed, non-blank, free-text explanation of
 * why this classification was assigned -- the same free-text-over-new-
 * type choice this file's own [KnowledgePromotionDecision.Reject.reason]
 * already makes, for the same reason: a promotion or revision's basis
 * weighs several named factors together (Contract Design Version 2 §5),
 * not a single structural rule a closed enum could represent without
 * misrepresenting it as one.
 */
data class KnowledgePromotion(
    val knowledgeId: KnowledgeId,
    val evidenceReference: MemoryCoreRecordReference,
    val resultingState: EvidentialState,
    val occurredAt: Instant,
    val basis: String,
) {
    init {
        require(basis.isNotBlank()) { "KnowledgePromotion.basis must not be blank" }
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
    val history: List<KnowledgePromotion> = emptyList(),
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
