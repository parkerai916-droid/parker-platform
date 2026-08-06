package parker.core.runtime

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeResultEntry
import parker.core.interfaces.KnowledgeRetrieval
import parker.core.interfaces.KnowledgeRetrievalDisposition
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.KnowledgeRetrievalResult
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceId
import parker.core.interfaces.StalenessDisclosure

/**
 * Programme 3, Knowledge Memory, Implementation Units 9.2 (Deterministic
 * Retrieval Engine), 9.3 (Staleness Disclosure), 9.4 (Retirement and
 * Supersession Retrieval-Shape Decision), and 9.5 (Permission Enforcement
 * Wiring). The sole implementation of
 * [parker.core.interfaces.KnowledgeRetrieval] -- see
 * `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md`
 * §4, Units 9.2 through 9.5, `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`
 * ("the Unit 9 Contract Design"), and
 * `docs/governance/PROGRAMME_3_UNIT_9_PERMISSION_ENFORCEMENT_MECHANISM_CLARIFICATION.md`
 * ("the Clarification", Adopted) for the constitutional reasoning this
 * class implements exactly and nothing more.
 *
 * This class implements query execution, filtering (structural matching
 * and lifecycle-status shaping), one disclosed, consistently-applied
 * ordering rule (Contract Design §8), one disclosed staleness-disclosure
 * heuristic (Contract Design V2 §3, Amendment 7; Unit 9 Contract Design
 * §2), one disclosed retired-item default-inclusion policy (Unit 9
 * Contract Design §6), and the Clarification's own two-tier permission
 * gate. It does not implement runtime composition (Unit 9.6) -- that
 * remains a later, separately authorised Unit's own responsibility,
 * exactly as the Implementation Plan's own ordering fixes.
 *
 * ## Permission enforcement (Unit 9.5) -- the Clarification's own two-tier gate, implemented exactly
 *
 * The Clarification's Section 6.2 freezes two, and only two, evaluation
 * granularities, both required (neither alone satisfies the Unit 9
 * Clarification's own per-item disclosure reasoning and Contract Design
 * §9's own binding "empty result implies permission was granted"
 * requirement simultaneously):
 *
 * 1. **Act-level gate.** [retrieve] evaluates [permissionEngine] exactly
 *    once per call, using [buildExecutionRequest] with
 *    [ACT_LEVEL_INTENT], before [persistence] is read at all. On any
 *    outcome other than [PermissionDecisionOutcome.APPROVED] or
 *    [PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION],
 *    [KnowledgeRetrievalDisposition.NotAuthorised] is returned
 *    immediately -- no persistence read, no matching, no lifecycle
 *    shaping, and no staleness computation occurs on this path
 *    (Clarification §6.2 item 1, §8 steps 2-3).
 * 2. **Item-level gate.** Where the act-level gate approves, every
 *    candidate [KnowledgeItem] surviving [matches] and [isRetrievable] --
 *    unchanged Unit 9.2/9.4 behaviour -- receives its own, separate
 *    evaluation, in the same order [persistence] returned them, **before**
 *    [KnowledgeRetrievalQuery.maximumResults] bounding is applied
 *    (Clarification §8 step 6, whose own disclosed reasoning this class
 *    relies on unchanged: bounding after permission filtering, not
 *    before, so a caller who receives fewer than [KnowledgeRetrievalQuery.maximumResults]
 *    entries can trust this reflects genuinely fewer *visible* items, not
 *    an artefact of a bound applied before visibility was known). An item
 *    whose evaluation is not `APPROVED`/`APPROVED_WITH_CONFIRMATION` is
 *    silently excluded from the result -- never surfaced as a
 *    distinguishable per-item denial, indistinguishable at the type level
 *    from an item that never matched at all (Clarification §10, mirroring
 *    `PermissionFilteredMemoryRetrieval`'s own established per-record
 *    filtering precedent).
 *
 * These are evaluations of two genuinely different questions -- "may this
 * principal use Knowledge Retrieval at all" and "may this principal see
 * this specific item" -- never a redundant, duplicate evaluation of the
 * same proposal (Clarification §6.2). A query therefore evaluates
 * [PermissionEngine.evaluate] exactly `1` time when the act-level gate
 * denies, or exactly `1 + N` times when it approves, where `N` is the
 * number of items surviving [matches] and [isRetrievable] before bounding
 * (Clarification §8's own disclosed "Verification consequence").
 *
 * ## One fixed resource/action pair, reused at both granularities
 *
 * [KNOWLEDGE_RETRIEVAL_RESOURCE_ID] and [RETRIEVE_ACTION_NAME] are the
 * single, fixed, disclosed-but-unregistered pair the Clarification §7
 * names -- never a per-item identifier. Both granularities' own
 * [ExecutionRequest] share the identical `targetResources`/
 * `proposedActions` pair; only [ExecutionRequest.intent] varies (naming
 * the specific item under evaluation, for the item-level gate), and only
 * *how many times*, and *against which principal*, the pair is evaluated
 * varies (Clarification §7: "only *how many times*, and *against which
 * principal*, the same fixed pair is evaluated varies").
 *
 * ## Correlation identifier -- propagated unchanged, never freshly minted
 *
 * Unlike [DefaultKnowledgeSubmission] (which mints a fresh correlation
 * identifier, since `KnowledgeCandidate` carries none of its own),
 * [KnowledgeRetrievalQuery.correlationId] already exists precisely so it
 * can be propagated (Unit 9 Contract Design §4). Every [ExecutionRequest]
 * this class constructs, at either granularity, carries
 * [KnowledgeRetrievalQuery.correlationId] unchanged as its own
 * [ExecutionRequest.correlationId] -- never a freshly minted value
 * (Clarification §9). [ExecutionRequest.requestId] is still freshly
 * minted per evaluation (an address for one specific evaluation, a
 * different concern from correlating every evaluation belonging to the
 * same query).
 *
 * ## Read source: [persistence], never Memory Core
 *
 * This class holds no [parker.core.interfaces.MemoryRetrieval] or
 * [parker.core.interfaces.MemoryCore] reference of any kind. It is
 * therefore structurally incapable of reading Memory Core content at any
 * point in [retrieve], mirroring [DefaultKnowledgeSubmission]'s own
 * identical "no dependency capable of reading Memory Core" structural
 * guarantee (Unit 9 Contract Design §3: "Knowledge Retrieval never
 * performs a Memory Core query of its own"). Every [KnowledgeItem] this
 * class returns already carries its own provenance reference
 * ([KnowledgeItem.provenanceReference]); this class forwards it unchanged,
 * never resolving, dereferencing, or duplicating it.
 *
 * ## Structural matching target -- a disclosed, minimal, non-inventive choice
 *
 * The Unit 9 Contract Design deliberately does not fix what "caller-
 * supplied structural matching criteria" (Contract Design §2) are matched
 * against -- only that matching is structural, never semantic. Unlike
 * legacy `KnowledgeRecord`, [KnowledgeItem] carries no free-text "payload"
 * field of its own to match against (by design: Knowledge Memory never
 * copies or duplicates Memory Core content). The only free-text field
 * [KnowledgeItem] genuinely carries is each history event's own disclosed
 * [parker.core.interfaces.KnowledgeLifecycleEvent.basis] -- the explanation
 * for why the item's current classification is what it is. [matches]
 * therefore performs a case-insensitive substring match of
 * [KnowledgeRetrievalQuery.relevance] against the *most recent* history
 * event's own [parker.core.interfaces.KnowledgeLifecycleEvent.basis] --
 * the text explaining the item's own, current state -- mirroring legacy
 * `KnowledgeQuery.relevance`'s own already-proven, already-tested
 * case-insensitive substring convention (`InMemoryKnowledgeStore`), never
 * a new algorithm. An item with no history (structurally impossible for
 * any item this Unit's own precedent construction path produces, since
 * every promoted [KnowledgeItem] carries at least its own
 * [parker.core.interfaces.KnowledgePromotion] as history's first entry) is
 * treated as never matching, rather than this class fabricating a basis
 * that does not exist.
 *
 * ## Ordering -- insertion order, disclosed and relied upon, not computed here
 *
 * [KnowledgeItemPersistence.findAll] already returns items in the order
 * they were stored (its own KDoc). This class relies on that existing
 * guarantee rather than sorting, ranking, or re-ordering anything itself
 * -- filtering, item-level permission evaluation (a sequential, in-order
 * loop, never a re-ordering operation), and bounding with [List.take] all
 * preserve the input list's own relative order, so the one ordering rule
 * Contract Design §8 requires to "exist and be applied consistently" is
 * satisfied by construction, never by an ordering algorithm this class
 * implements. The same query against unchanged persisted state and an
 * unchanged permission policy therefore returns an identical result, in
 * identical order, on every call.
 *
 * ## Staleness -- a disclosed, honest, age-based signal, never a claim of the governed condition
 *
 * Contract Design V2 §3 (Amendment 7) states the governed condition
 * precisely and it is quoted here in full, without softening: "Where the
 * underlying evidence's status changes afterward (for example, becomes
 * disputed) before Knowledge Memory has re-evaluated and produced a new
 * classification, the Knowledge Item is stale, and this must never be
 * silently concealed." **Staleness, as governed, is an evidence-status-
 * change condition. It is never an elapsed-time condition.** Time appears
 * in Contract Design V2 §3 only as a reference point -- "the state of its
 * underlying Memory Core evidence at the moment that classification was
 * computed" -- never as the trigger itself. **No governing document
 * authorises age, by itself, as a staleness signal.** Only the detection
 * *mechanism* is left open by Unit 9 Implementation Plan §4, Unit 9.3 --
 * "continuous monitoring, checked at query time, or otherwise" -- and that
 * latitude is about *how* evidence-status change is detected, not license
 * to substitute a different, unauthorised condition (elapsed time) for the
 * governed one.
 *
 * This class cannot detect the governed condition: doing so would require
 * exactly the Memory Core query it is structurally forbidden from making
 * (see "Read source," above; Unit 9 Contract Design §3). Rather than
 * fabricate a disclosure it has no basis for, or silently repurpose the
 * old `stale: Boolean` field to mean something governance never defined,
 * this class discloses staleness through [StalenessDisclosure]
 * (`src/interfaces/KnowledgeStore.kt`) -- a widened representation, per
 * Unit 9.1's own KDoc, which explicitly pre-authorised Unit 9.3 "to widen
 * or replace [`stale: Boolean`]... if its own drafting finds a binary
 * signal insufficient to satisfy the Unit 9 Contract Design's own
 * staleness-disclosure guarantee." [disclosureFor] never returns
 * [StalenessDisclosure.CONFIRMED_CURRENT] or
 * [StalenessDisclosure.CONFIRMED_STALE] -- both require the Memory Core
 * comparison this class cannot perform, and remain reserved for a future,
 * genuinely evidence-aware mechanism. It returns
 * [StalenessDisclosure.POSSIBLY_STALE] when more than
 * [POSSIBLY_STALE_AFTER] has elapsed, per [clock], since the item's own
 * most recent [KnowledgePromotion] history entry's
 * [parker.core.interfaces.KnowledgeLifecycleEvent.occurredAt] -- the
 * moment its current classification was last computed -- and
 * [StalenessDisclosure.INDETERMINATE] otherwise: the honest default,
 * deliberately not a claim of freshness, for every item this signal does
 * not distinguish as unusually old. Unit 9.5 computes this disclosure
 * only for the final, permission-approved, bounded set (Clarification §8
 * step 8) -- an unchanged computation, applied to a possibly smaller set.
 *
 * **The false-negative direction is the more serious of this proxy's two
 * failure modes, not a symmetric limitation.** A freshly-classified item
 * discloses [StalenessDisclosure.INDETERMINATE] -- honestly, since this
 * class has no basis to claim otherwise -- but that item's own Memory
 * Core evidence could, in principle, already have changed status moments
 * after classification, and this class would have no way to know. A
 * `Boolean` `false` at that same moment would have asserted "not stale" --
 * a claim of freshness this class was never entitled to make and Article
 * XIII forbids concealing the uncertainty behind. [StalenessDisclosure]
 * corrects this precisely: [StalenessDisclosure.INDETERMINATE] makes no
 * freshness claim at all, so the false-negative risk inherent in this
 * proxy is disclosed by the type itself, not concealed by it. An old,
 * unrevisited item disclosing [StalenessDisclosure.POSSIBLY_STALE] is the
 * comparatively minor failure mode -- a plausibly-still-current item
 * flagged for reconsideration it may not have needed -- never a
 * concealment of uncertainty in the other direction.
 *
 * **Continuous monitoring was considered and is not implemented here.**
 * A genuinely event-driven mechanism (subscribing to Memory Core's own
 * published events to detect an evidence status change as it happens)
 * would come closer to the governed condition, but requires a new
 * dependency (an event subscription) and new durable state (tracking
 * which Knowledge Items reference which Memory Core records) that neither
 * the Unit 9 Contract Design nor the Unit 9 Implementation Plan's own
 * Unit 9.3 entry authorises for this narrow sub-unit -- its own only
 * named dependency is Unit 9.1. Building it here would be scope creep
 * this task's own governing discipline forbids, not a permitted
 * elaboration of "or otherwise."
 *
 * [POSSIBLY_STALE_AFTER] (thirty days) is a disclosed,
 * implementation-defined bound, not architecturally significant and
 * changeable freely without a Contract Design revision -- mirroring
 * `DefaultReasoningContextAssembler.MEMORY_QUERY_MAXIMUM_RESULTS`'s own
 * identical treatment elsewhere in this codebase, and
 * `DefaultWorldModelUpdatePolicy.DEFAULT_STALE_AFTER`'s own identical
 * naming convention -- though that World Model policy governs a
 * genuinely different, legitimately age-based epistemic model (belief
 * transience, per ADR-024), never this Unit's own evidence-status-change
 * condition; the naming parallel is convention only, not a claim of
 * conceptual equivalence. An item with no [KnowledgePromotion] history
 * entry (structurally impossible via this Unit's own precedent
 * construction path, per "Structural matching target," above) discloses
 * [StalenessDisclosure.INDETERMINATE] -- this class has no reference
 * timestamp to measure elapsed time against, and therefore no basis for
 * [StalenessDisclosure.POSSIBLY_STALE] either; only
 * [parker.core.interfaces.KnowledgeRetirement] and
 * [parker.core.interfaces.KnowledgeRestoration] history entries are
 * excluded from the reference-timestamp search, since neither represents
 * a genuine reclassification -- Unit 7's own established precedent holds
 * that a retirement or restoration "is a status change, never an
 * evidential classification," and only a genuine [KnowledgePromotion]
 * carries a newly-computed classification worth measuring elapsed time
 * from.
 *
 * ## Lifecycle shaping (Unit 9.4) -- a considered policy, not an absence of one
 *
 * **The planning determination, stated once, here, and applied
 * consistently by [isRetrievable].** Unit 9 Contract Design §6 named
 * three lawful outcomes for whether a [KnowledgeItemStatus.RETIRED] item
 * appears in an ordinary [KnowledgeRetrievalQuery]'s own result set --
 * included by default, excluded by default, or included only under an
 * explicit caller criterion -- and left the choice to this Unit. This
 * Unit chooses **excluded by default, included only when the caller sets
 * [KnowledgeRetrievalQuery.includeRetired] to `true`.** Two governing
 * texts drive this, in tension, both satisfied only by this combination:
 * Unit 9 Contract Design §1 defines an ordinary query as a "task-scoped
 * request for relevant, **already-promoted** knowledge" -- naturally a
 * request for what Knowledge Memory still holds as current, since a
 * retired item is, by definition (Contract Design V2 §3), "no longer
 * current"; but Contract Design V2 §3's own "retirement never implies
 * deletion... remain retained and retrievable" guarantee forecloses a
 * *permanent*, non-overridable exclusion, since this class is "the sole
 * public path through which anything outside Knowledge Memory may
 * observe promoted knowledge" (Unit 9 Contract Design §1) -- a retired
 * item excluded from it with no override would be retrievable in name
 * only. Excluding by default, with an explicit, disclosed opt-in,
 * satisfies both: ordinary queries return current knowledge; a caller who
 * genuinely needs a retired item can still reach it through the one
 * lawful path. This default is a considered decision, not the earlier,
 * disclosed-as-provisional absence of one Units 9.2 and 9.3 left in
 * place -- seeded from [KnowledgeRetrievalQuery.includeRetired]'s own
 * `false` default, so an existing caller who does not name that parameter
 * receives the new, deliberate exclusion, not silent continuation of the
 * old placeholder behaviour.
 *
 * **[isRetrievable] filters on [KnowledgeItem.status] alone, deliberately
 * never on whether [KnowledgeItem.history] contains a
 * [parker.core.interfaces.KnowledgeRetirement] event.** This distinction
 * matters for restoration: Unit 9 Contract Design §6 fixes that
 * "restoration... returns its status to active" and that "a restored item
 * is retrievable exactly as any other active item once restored."
 * Filtering on current [KnowledgeItem.status] gives restoration this
 * treatment automatically and without any special-case code -- a restored
 * item's [KnowledgeItem.status] is [KnowledgeItemStatus.ACTIVE], so
 * [isRetrievable] admits it exactly as it would any other active item,
 * regardless of the [parker.core.interfaces.KnowledgeRetirement] event
 * still, correctly, visible earlier in its own [KnowledgeItem.history].
 * Filtering on history-contains-a-retirement instead would have
 * incorrectly re-excluded every restored item forever -- exactly the
 * "silently collapse... with no trace" failure Unit 9 Contract Design §6
 * warns against, inverted into a different, equally dishonest failure.
 *
 * **Superseded classifications required no filtering decision, and no
 * contract widening, at all.** Contract Design V2 §3 fixes that
 * supersession "is re-evaluated against the superseding evidence, exactly
 * as an ordinary revision" -- it is not a status, not a fourth lifecycle
 * event kind, and never forks a [KnowledgeItem] into a separate "current"
 * and "superseded" record. A single [KnowledgeItem]'s own
 * [KnowledgeItem.evidentialState] already holds exactly its current
 * classification, and [KnowledgeItem.history] already holds every earlier
 * one, including every earlier hop of an arbitrarily long supersession
 * chain (Contract Design V2 §3: "the full chain... remains transitively
 * retrievable"). [retrieve] forwards each item-level-approved
 * [KnowledgeItem] unchanged -- it never truncates, filters, or re-projects
 * [KnowledgeItem.history] -- so multi-hop retrievability, and the
 * current-versus-superseded distinction Unit 9 Contract Design §6's own
 * "Superseded" paragraph fixes, are both satisfied by construction, not by
 * anything this Unit adds. This is also why [KnowledgeResultEntry] itself
 * needed no widening for supersession, and why Unit 9.5's own item-level
 * permission gate needed none either: the distinguishing information a
 * caller needs -- which entry is current, which are historical -- is
 * already present on the unchanged [KnowledgeItem] every approved entry
 * already carries.
 *
 * **No "latest only" selection exists anywhere in this class.** Unit 9
 * Contract Design §6 states plainly that "nothing here selects a 'latest
 * only' retrieval policy," since Contract Design V2 §3's own multi-hop
 * retrievability requirement forecloses it as the sole behaviour. This
 * class discloses relationships -- a matched, approved item's full,
 * ordered [KnowledgeItem.history] -- and leaves any "which entry matters
 * most" judgment entirely to the caller; it computes no "latest"
 * projection, no summary, and no collapse of the chain into a single
 * representative entry anywhere in [retrieve].
 *
 * **[KnowledgeItemStatus] alone is sufficient to represent every retrieval-
 * shaping decision this Unit makes.** No additional, derived
 * classification was added to distinguish "superseded" as its own kind,
 * because supersession is not a status in the constitutional model this
 * class is bound to (Contract Design V2 §3) -- it is a revision-kind
 * history event, already fully disclosed by forwarding
 * [KnowledgeItem.history] unchanged, as explained above. The one genuine
 * status-shaped decision -- whether [KnowledgeItemStatus.RETIRED] items
 * appear by default -- is fully expressed by the two-value status model
 * this file already uses; inventing a third value, or a parallel
 * classification, would violate the Implementation Plan's own
 * cross-cutting "no new lifecycle state or event kind" boundary (§3) for
 * no disclosed benefit.
 *
 * **[includeRetired] is a structural criterion, never a permission
 * signal.** Unit 9 Contract Design §6's own closing paragraph --
 * "lifecycle status is never a substitute for, or determinant of, a
 * permission decision" -- applies to this field exactly as it already
 * applies to [KnowledgeItem.status] itself, and exactly as it applies to
 * Unit 9.5's own permission gate: a caller setting
 * [KnowledgeRetrievalQuery.includeRetired] to `true` requests that retired
 * items be considered for structural matching; it grants no permission
 * and is evaluated identically, by the same item-level gate, as every
 * other matched item.
 *
 * ## Determinism, disclosed precisely
 *
 * For unchanged persisted state, an unchanged permission policy, *and* a
 * fixed instant in time, the same [KnowledgeRetrievalQuery] yields the
 * same disposition, the same matched items, in the same order, bounded to
 * the same count, with the same staleness disclosures, every time -- no
 * randomisation and no load-dependent reordering exists anywhere in
 * [retrieve] (Unit 9 Contract Design §8; `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`
 * §7). [isRetrievable] is a pure function of [KnowledgeItem.status] and
 * [KnowledgeRetrievalQuery.includeRetired] alone -- no wall-clock read, no
 * randomisation, applied by the same single filtering step as [matches]
 * -- so lifecycle shaping is exactly as deterministic as structural
 * matching itself, uniformly, never varying by query shape or code path
 * (mirroring Scope Lock §8's own concurrent-revision-ordering uniformity
 * discipline). [isAuthorised] is likewise a pure function of its own
 * [PermissionDecision] argument alone.
 *
 * **Two disclosed exceptions, not one.** [disclosureFor]'s own
 * elapsed-time computation is, necessarily, wall-clock-derived, and two
 * calls genuinely separated in real time by more than
 * [POSSIBLY_STALE_AFTER] may honestly differ in which entries they
 * disclose as [StalenessDisclosure.POSSIBLY_STALE] -- mirroring
 * [DefaultKnowledgeCandidateEvaluator]'s own identical, already-disclosed
 * treatment of [parker.core.interfaces.KnowledgePromotion.occurredAt]
 * ("timestamps may legitimately differ across repeated evaluations...
 * does not weaken, and is entirely separate from, the deterministic
 * identity and classification guarantees"). Unit 9.5 adds a second,
 * genuinely new exception: this class's own overall determinism claim now
 * additionally depends on [permissionEngine] itself returning a stable
 * decision for repeated evaluations of the same [ExecutionRequest]
 * content against the same principal and unchanged policy -- a property
 * this class assumes, consistent with the general "no ranking or
 * scoring" and "no random or load-dependent behaviour" discipline every
 * prior Unit here already relies on for its own collaborators, but does
 * not, and cannot, itself enforce, since [PermissionEngine] is a Trust
 * Framework-owned dependency this class invokes rather than implements.
 * Matching, lifecycle shaping, ordering, and bounding all remain fully
 * deterministic regardless of either exception; only the staleness
 * disclosure and the permission decisions themselves are the two
 * time/policy-relative signals, exactly as a genuinely time-based signal
 * and an externally-owned trust decision must honestly be.
 *
 * @param persistence The sole read source for already-promoted
 *   [KnowledgeItem] values. Read only -- this class never calls
 *   [KnowledgeItemPersistence.store]. Never read before the act-level
 *   permission gate approves (Clarification §8 step 2's own "must never
 *   read `KnowledgeItemPersistence` before it completes" requirement).
 * @param permissionEngine Evaluated at least once, and never zero times,
 *   per [retrieve] call -- exactly once for the act-level gate, plus once
 *   per candidate item surviving structural matching and lifecycle
 *   shaping, for the item-level gate (Clarification §6.2, §8).
 * @param clock The time source [disclosureFor] reads "now" from. Defaults
 *   to the real system clock in production; tests supply a fixed
 *   [Clock] so staleness assertions remain deterministic and instant,
 *   never dependent on real elapsed wall-clock time or a sleeping test.
 */
internal class DefaultKnowledgeRetrieval(
    private val persistence: KnowledgeItemPersistence,
    private val permissionEngine: PermissionEngine,
    private val clock: Clock = Clock.systemUTC(),
) : KnowledgeRetrieval {

    override suspend fun retrieve(
        requestingPrincipalId: PrincipalId,
        query: KnowledgeRetrievalQuery,
    ): KnowledgeRetrievalDisposition {
        val actLevelDecision = permissionEngine.evaluate(
            buildExecutionRequest(
                requestingPrincipalId = requestingPrincipalId,
                correlationId = query.correlationId,
                intent = ACT_LEVEL_INTENT,
            ),
        )

        if (!isAuthorised(actLevelDecision)) {
            return KnowledgeRetrievalDisposition.NotAuthorised(
                "Permission Engine did not authorise Knowledge Retrieval for principal " +
                    "'${requestingPrincipalId.value}' (decision=${actLevelDecision.decision})",
            )
        }

        val candidates = persistence.findAll()
            .filter { item -> matches(item, query.relevance) && isRetrievable(item, query) }

        val approved = mutableListOf<KnowledgeItem>()
        for (item in candidates) {
            val itemDecision = permissionEngine.evaluate(
                buildExecutionRequest(
                    requestingPrincipalId = requestingPrincipalId,
                    correlationId = query.correlationId,
                    intent = itemLevelIntent(item),
                ),
            )
            if (isAuthorised(itemDecision)) {
                approved += item
            }
        }

        val entries = approved
            .take(query.maximumResults)
            .map { item -> KnowledgeResultEntry(item = item, staleness = disclosureFor(item)) }

        return KnowledgeRetrievalDisposition.Retrieved(KnowledgeRetrievalResult(entries))
    }

    private fun isAuthorised(decision: PermissionDecision): Boolean {
        return decision.decision == PermissionDecisionOutcome.APPROVED ||
            decision.decision == PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
    }

    private fun matches(item: KnowledgeItem, relevance: String): Boolean {
        val currentBasis = item.history.lastOrNull()?.basis ?: return false
        return currentBasis.contains(relevance, ignoreCase = true)
    }

    private fun isRetrievable(item: KnowledgeItem, query: KnowledgeRetrievalQuery): Boolean {
        return item.status == KnowledgeItemStatus.ACTIVE || query.includeRetired
    }

    private fun disclosureFor(item: KnowledgeItem): StalenessDisclosure {
        val lastClassifiedAt = item.history.filterIsInstance<KnowledgePromotion>()
            .lastOrNull()?.occurredAt ?: return StalenessDisclosure.INDETERMINATE
        val elapsed = Duration.between(lastClassifiedAt, clock.instant())
        return if (elapsed > POSSIBLY_STALE_AFTER) {
            StalenessDisclosure.POSSIBLY_STALE
        } else {
            StalenessDisclosure.INDETERMINATE
        }
    }

    private fun itemLevelIntent(item: KnowledgeItem): String =
        "Disclose Knowledge Item '${item.knowledgeId.value}' in a Knowledge Retrieval result"

    /**
     * Shared [ExecutionRequest] construction for both the act-level and
     * item-level gates -- the two granularities differ only in [intent]
     * (Clarification §6.2, §7), never in [KNOWLEDGE_RETRIEVAL_RESOURCE_ID],
     * [RETRIEVE_ACTION_NAME], or how [correlationId] is sourced.
     * [correlationId] is always the caller-supplied
     * [KnowledgeRetrievalQuery.correlationId], never freshly minted
     * (Clarification §9) -- unlike [ExecutionRequest.requestId], which is
     * freshly minted per evaluation, mirroring
     * [DefaultKnowledgeSubmission.buildExecutionRequest]'s own identical
     * per-call `requestId` minting.
     */
    private fun buildExecutionRequest(
        requestingPrincipalId: PrincipalId,
        correlationId: String,
        intent: String,
    ): ExecutionRequest {
        return ExecutionRequest(
            requestId = RequestId("knowledge-retrieval-${UUID.randomUUID()}"),
            principalId = requestingPrincipalId,
            origin = RequestOrigin.REMOTE_INTERFACE,
            intent = intent,
            targetResources = listOf(KNOWLEDGE_RETRIEVAL_RESOURCE_ID),
            proposedActions = listOf(RETRIEVE_ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = correlationId,
        )
    }

    companion object {
        /**
         * See this file's own "Staleness" KDoc, above, for the full
         * disclosed reasoning. Thirty days -- an implementation-defined,
         * not architecturally significant bound; may be changed freely
         * without a Contract Design revision.
         */
        private val POSSIBLY_STALE_AFTER: Duration = Duration.ofDays(30)

        /**
         * A fixed, well-known [ResourceId] both of Unit 9.5's own gates
         * always name as their target -- not a per-item identity
         * (Clarification §7: "a single, fixed resource identity
         * representing the Knowledge Retrieval boundary itself... not a
         * per-item resource"), mirroring
         * [DefaultKnowledgeSubmission.KNOWLEDGE_SUBMISSION_RESOURCE_ID]'s
         * own exact naming and treatment. Not registered anywhere by this
         * Unit.
         */
        val KNOWLEDGE_RETRIEVAL_RESOURCE_ID: ResourceId = ResourceId("knowledge-memory-retrieval")

        /**
         * A fixed proposed-action name both of Unit 9.5's own gates always
         * name, following this repository's own established
         * dotted-namespace convention (Clarification §7). A future
         * `ActionVocabulary` registration is expected to map it to a
         * resolvable `(PermissionAction, ResourceType)` pair; not
         * registered anywhere by this Unit.
         */
        const val RETRIEVE_ACTION_NAME: String = "knowledge.retrieve"

        /**
         * The [ExecutionRequest.intent] the act-level gate always supplies
         * -- fixed, not caller- or query-specific, since the act-level
         * gate asks only "may this principal use Knowledge Retrieval at
         * all," never anything about a specific item (Clarification §6.2
         * item 1).
         */
        const val ACT_LEVEL_INTENT: String = "Authorise Knowledge Retrieval for a requesting principal"
    }
}
