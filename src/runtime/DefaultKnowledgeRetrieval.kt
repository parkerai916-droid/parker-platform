package parker.core.runtime

import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeResultEntry
import parker.core.interfaces.KnowledgeRetrieval
import parker.core.interfaces.KnowledgeRetrievalDisposition
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.KnowledgeRetrievalResult
import parker.core.interfaces.PrincipalId

/**
 * Programme 3, Knowledge Memory, Implementation Unit 9.2 (Deterministic
 * Retrieval Engine). The sole implementation of
 * [parker.core.interfaces.KnowledgeRetrieval] -- see
 * `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md`
 * §4, Unit 9.2, and `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`
 * ("the Unit 9 Contract Design") for the constitutional reasoning this
 * class implements exactly and nothing more.
 *
 * This class implements query execution, filtering, and one disclosed,
 * consistently-applied ordering rule (Contract Design §8). It does not
 * implement staleness detection (Unit 9.3), retirement/supersession
 * default-inclusion policy (Unit 9.4), permission enforcement (Unit 9.5),
 * or runtime composition (Unit 9.6) -- each remains a later, separately
 * authorised Unit's own responsibility, exactly as the Implementation
 * Plan's own ordering fixes.
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
 * ## Staleness -- a disclosed, conservative placeholder, never a computation
 *
 * [KnowledgeResultEntry.stale] is fixed `true` for every entry this class
 * returns -- not a genuine staleness determination (this class performs
 * none; Unit 9.3, Unit 9 Implementation Plan, owns that), but the
 * disclosed, deliberately conservative placeholder [KnowledgeResultEntry.stale]'s
 * own KDoc authorises pending a real mechanism. `true` is chosen over
 * `false` because this class has no basis whatsoever to assert freshness
 * -- verifying that would require exactly the Memory Core query this
 * class is structurally forbidden from making -- and asserting `false`
 * without evidence would be the fabrication the Unit 9 Contract Design's
 * own governing principle forbids ("an unknown value is never fabricated,
 * defaulted, or inferred without a disclosed inference step"), mirroring
 * `docs/architecture/10-permission-engine.md`'s own fail-closed discipline
 * ("uncertainty... defaults to inaction," applied here to disclosure
 * rather than authorisation: uncertain freshness discloses as potentially
 * stale, never silently as fresh). This is not a claim that every
 * returned item is actually stale -- it is a disclosed refusal to claim
 * the opposite without evidence.
 *
 * **This placeholder is never observable by any production caller before
 * Unit 9.3 replaces it.** No class in this codebase constructs
 * [DefaultKnowledgeRetrieval] outside this Unit's own tests -- runtime
 * composition (wiring any [KnowledgeRetrieval] implementation into
 * `ParkerRuntime.kt`) is Unit 9.6's own, separately authorised
 * responsibility, and the Unit 9 Implementation Plan's own ordering fixes
 * Unit 9.6 as strictly last, gated on Units 9.1 through 9.5 all being
 * complete -- including Unit 9.3. This placeholder's own practical safety
 * therefore does not depend on this class never being relied upon; it
 * depends on this class never being reachable by a real caller at all
 * until a genuine staleness mechanism has already replaced it.
 *
 * ## Lifecycle status -- currently unfiltered, an absence of policy, not a policy
 *
 * [retrieve] applies no filtering by [KnowledgeItem.status] of any kind --
 * an item whose status is [parker.core.interfaces.KnowledgeItemStatus.RETIRED]
 * is matched, ordered, and bounded identically to an
 * [parker.core.interfaces.KnowledgeItemStatus.ACTIVE] one, provided its
 * own most recent history entry's [parker.core.interfaces.KnowledgeLifecycleEvent.basis]
 * satisfies [matches]. **This is not a considered "include retired items
 * by default" policy decision.** It is the simple absence of any
 * lifecycle-aware filtering logic in this Unit at all. The Unit 9
 * Contract Design §6 explicitly reserves the actual default-inclusion
 * question -- included by default, excluded by default, or included only
 * under an explicit caller criterion -- to a later retrieval-shape
 * decision (Unit 9 Implementation Plan, Unit 9.4). This Unit's own
 * current, unconditional-inclusion behaviour must not be read as having
 * already answered that question, must not be relied upon as stable, and
 * remains subject to change, in either direction, once Unit 9.4 actually
 * decides it.
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
 * For unchanged persisted state, the same [KnowledgeRetrievalQuery]
 * yields the same matched items, in the same order, bounded to the same
 * count, every time -- no randomisation, no time-based variation, and no
 * load-dependent reordering exists anywhere in [retrieve] (Unit 9
 * Contract Design §8; `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`
 * §7).
 *
 * @param persistence The sole read source for already-promoted
 *   [KnowledgeItem] values. Read only -- this class never calls
 *   [KnowledgeItemPersistence.store].
 */
internal class DefaultKnowledgeRetrieval(
    private val persistence: KnowledgeItemPersistence,
) : KnowledgeRetrieval {

    override suspend fun retrieve(
        requestingPrincipalId: PrincipalId,
        query: KnowledgeRetrievalQuery,
    ): KnowledgeRetrievalDisposition {
        val matched = persistence.findAll()
            .filter { item -> matches(item, query.relevance) }
            .take(query.maximumResults)
            .map { item -> KnowledgeResultEntry(item = item, stale = true) }

        return KnowledgeRetrievalDisposition.Retrieved(KnowledgeRetrievalResult(matched))
    }

    private fun matches(item: KnowledgeItem, relevance: String): Boolean {
        val currentBasis = item.history.lastOrNull()?.basis ?: return false
        return currentBasis.contains(relevance, ignoreCase = true)
    }
}
