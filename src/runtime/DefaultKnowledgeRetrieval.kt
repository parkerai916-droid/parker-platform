package parker.core.runtime

import java.time.Clock
import java.time.Duration
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeResultEntry
import parker.core.interfaces.KnowledgeRetrieval
import parker.core.interfaces.KnowledgeRetrievalDisposition
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.KnowledgeRetrievalResult
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.StalenessDisclosure

/**
 * Programme 3, Knowledge Memory, Implementation Units 9.2 (Deterministic
 * Retrieval Engine), 9.3 (Staleness Disclosure), and 9.4 (Retirement and
 * Supersession Retrieval-Shape Decision). The sole implementation of
 * [parker.core.interfaces.KnowledgeRetrieval] -- see
 * `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md`
 * §4, Units 9.2 through 9.4, and `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`
 * ("the Unit 9 Contract Design") for the constitutional reasoning this
 * class implements exactly and nothing more.
 *
 * This class implements query execution, filtering (structural matching
 * and lifecycle-status shaping), one disclosed, consistently-applied
 * ordering rule (Contract Design §8), one disclosed staleness-disclosure
 * heuristic (Contract Design V2 §3, Amendment 7; Unit 9 Contract Design
 * §2), and one disclosed retired-item default-inclusion policy (Unit 9
 * Contract Design §6). It does not implement permission enforcement (Unit
 * 9.5) or runtime composition (Unit 9.6) -- each remains a later,
 * separately authorised Unit's own responsibility, exactly as the
 * Implementation Plan's own ordering fixes.
 *
 * ## Read source: [persistence], never Memory Core
 *
 * This class holds exactly one dependency -- [persistence] -- and,
 * deliberately, no [parker.core.interfaces.MemoryRetrieval] or
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
 * -- filtering with [List.filter] and bounding with [List.take] both
 * preserve the input list's own relative order (Kotlin's own documented
 * guarantee for both), so the one ordering rule Contract Design §8
 * requires to "exist and be applied consistently" is satisfied by
 * construction, never by an ordering algorithm this class implements.
 * The same query against unchanged persisted state therefore returns an
 * identical result, in identical order, on every call.
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
 * not distinguish as unusually old.
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
 * retrievable"). [retrieve] forwards each matched [KnowledgeItem]
 * unchanged -- it never truncates, filters, or re-projects
 * [KnowledgeItem.history] -- so multi-hop retrievability, and the
 * current-versus-superseded distinction Unit 9 Contract Design §6's own
 * "Superseded" paragraph fixes ("the item's current classification is the
 * most recent entry in its own single, non-forking history, and any
 * earlier, superseded entry remains part of that same history rather than
 * being presented as though it were current"), are both satisfied by
 * construction, not by anything this Unit adds. This is also why
 * [KnowledgeResultEntry] itself needed no widening for supersession: the
 * distinguishing information a caller needs -- which entry is current,
 * which are historical -- is already present on the unchanged
 * [KnowledgeItem] every entry already carries.
 *
 * **No "latest only" selection exists anywhere in this class.** Unit 9
 * Contract Design §6 states plainly that "nothing here selects a 'latest
 * only' retrieval policy," since Contract Design V2 §3's own multi-hop
 * retrievability requirement forecloses it as the sole behaviour. This
 * class discloses relationships -- a matched item's full, ordered
 * [KnowledgeItem.history] -- and leaves any "which entry matters most"
 * judgment entirely to the caller; it computes no "latest" projection, no
 * summary, and no collapse of the chain into a single representative
 * entry anywhere in [retrieve].
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
 * permission decision" -- applies to this new field exactly as it already
 * applies to [KnowledgeItem.status] itself. A caller setting
 * [KnowledgeRetrievalQuery.includeRetired] to `true` requests that retired
 * items be considered for structural matching; it grants no permission,
 * bypasses no future gate, and is evaluated identically by whatever
 * mechanism Unit 9.5 eventually wires, exactly as every other matched
 * item is.
 *
 * ## Permission -- accepted, never consulted
 *
 * [requestingPrincipalId] is accepted because [KnowledgeRetrieval.retrieve]'s
 * own fixed signature (Unit 9.1) requires it -- this class does not read,
 * filter by, or otherwise consult its value for any decision. No
 * permission evaluation of any kind occurs here; every call is treated as
 * authorised, since gating this act is Unit 9.5's own, separately
 * authorised, not-yet-begun responsibility (Unit 9 Contract Design §5).
 * [KnowledgeRetrievalDisposition.NotAuthorised] is never returned by this
 * class -- only [KnowledgeRetrievalDisposition.Retrieved].
 *
 * ## Determinism, disclosed precisely
 *
 * For unchanged persisted state *and* a fixed instant in time, the same
 * [KnowledgeRetrievalQuery] yields the same matched items, in the same
 * order, bounded to the same count, with the same staleness disclosures,
 * every time -- no randomisation and no load-dependent reordering exists
 * anywhere in [retrieve] (Unit 9 Contract Design §8; `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`
 * §7). [isRetrievable] is a pure function of [KnowledgeItem.status] and
 * [KnowledgeRetrievalQuery.includeRetired] alone -- no wall-clock read,
 * no randomisation, applied by the same single [List.filter] step as
 * [matches] -- so lifecycle shaping is exactly as deterministic as
 * structural matching itself, uniformly, never varying by query shape or
 * code path (mirroring Scope Lock §8's own concurrent-revision-ordering
 * uniformity discipline). One disclosed exception, unchanged from Unit
 * 9.3: [disclosureFor]'s own elapsed-time computation is, necessarily,
 * wall-clock-derived, and two calls genuinely separated in real time by
 * more than [POSSIBLY_STALE_AFTER] may honestly differ in which entries
 * they disclose as [StalenessDisclosure.POSSIBLY_STALE] -- mirroring
 * [DefaultKnowledgeCandidateEvaluator]'s own identical, already-disclosed
 * treatment of [parker.core.interfaces.KnowledgePromotion.occurredAt]
 * ("timestamps may legitimately differ across repeated evaluations...
 * does not weaken, and is entirely separate from, the deterministic
 * identity and classification guarantees"). Matching, lifecycle shaping,
 * ordering, and bounding all remain fully deterministic regardless; only
 * the staleness disclosure is time-relative, exactly as a genuinely
 * time-based signal must be.
 *
 * @param persistence The sole read source for already-promoted
 *   [KnowledgeItem] values. Read only -- this class never calls
 *   [KnowledgeItemPersistence.store].
 * @param clock The time source [disclosureFor] reads "now" from. Defaults
 *   to the real system clock in production; tests supply a fixed
 *   [Clock] so staleness assertions remain deterministic and instant,
 *   never dependent on real elapsed wall-clock time or a sleeping test.
 */
internal class DefaultKnowledgeRetrieval(
    private val persistence: KnowledgeItemPersistence,
    private val clock: Clock = Clock.systemUTC(),
) : KnowledgeRetrieval {

    override suspend fun retrieve(
        requestingPrincipalId: PrincipalId,
        query: KnowledgeRetrievalQuery,
    ): KnowledgeRetrievalDisposition {
        val matched = persistence.findAll()
            .filter { item -> matches(item, query.relevance) && isRetrievable(item, query) }
            .take(query.maximumResults)
            .map { item -> KnowledgeResultEntry(item = item, staleness = disclosureFor(item)) }

        return KnowledgeRetrievalDisposition.Retrieved(KnowledgeRetrievalResult(matched))
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

    companion object {
        /**
         * See this file's own "Staleness" KDoc, above, for the full
         * disclosed reasoning. Thirty days -- an implementation-defined,
         * not architecturally significant bound; may be changed freely
         * without a Contract Design revision.
         */
        private val POSSIBLY_STALE_AFTER: Duration = Duration.ofDays(30)
    }
}
