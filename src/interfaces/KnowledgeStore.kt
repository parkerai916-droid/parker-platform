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
