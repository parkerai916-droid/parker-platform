package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgeRetirement
import parker.core.interfaces.KnowledgeRetirementEvaluation
import parker.core.interfaces.KnowledgeRetirementEvaluator

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.3 (Knowledge
 * Retirement Evaluation). The concrete [KnowledgeRetirementEvaluator] this
 * Unit supplies.
 *
 * ## Scope -- what this class does and does not do
 *
 * This class implements exactly: the retirement qualification check
 * (`item.status == ACTIVE`), constructing exactly one [KnowledgeRetirement]
 * event, and returning a copied [KnowledgeItem] with
 * [KnowledgeItemStatus.RETIRED] and that event appended to
 * [KnowledgeItem.history]. It does **not** implement revision, supersession,
 * restoration, persistence, compare-and-append, lifecycle-sequence
 * enforcement, optimistic concurrency, any Memory Core mutation, or
 * Permission Engine wiring (Unit 8) -- all remain later units' own,
 * separately authorised responsibility
 * (`docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §4, §8).
 *
 * ## No [parker.core.interfaces.MemoryRetrieval] dependency, by design
 *
 * Unlike [DefaultKnowledgeRevisionEvaluator], this class takes no
 * constructor dependency of any kind and consults no Memory Core evidence,
 * relationship, or status. This is not an oversight: Scope Lock
 * Clarification §8 defines retirement exhaustively as the `ACTIVE`-to-
 * `RETIRED` transition, gated solely by the item's own current [status]
 * and a disclosed, non-blank [basis] -- it names no relationship or
 * status-transition gate analogous to §7's Revision Qualification Gate,
 * in deliberate contrast to §9's Restoration Qualification Rule, which
 * does explicitly require [parker.core.interfaces.MemoryRetrieval]
 * resolution. This boundary was independently re-confirmed by the Unit
 * 7.3 Targeted Retirement Boundary Review before this class was written.
 *
 * ## Qualification (Scope Lock Clarification §8)
 *
 * Retirement qualifies only where [item].[KnowledgeItem.status] is
 * currently [KnowledgeItemStatus.ACTIVE]. Where it is already
 * [KnowledgeItemStatus.RETIRED], the request names no transition this
 * document defines at all and **must** be rejected --
 * [KnowledgeRetirementEvaluation.NotPermittedFromCurrentStatus] -- never
 * treated as a silent no-op and never treated as a new permitted event,
 * mirroring Memory Core Scope Lock §8's own governing convention that
 * every transition it does not define is refused, not quietly absorbed.
 *
 * ## The updated [KnowledgeItem]
 *
 * Where retirement is applied, the returned [KnowledgeItem] carries:
 * [KnowledgeItem.status] changed to [KnowledgeItemStatus.RETIRED];
 * [KnowledgeItem.history] equal to the prior value's own history with
 * **exactly one** new [KnowledgeRetirement] appended -- no prior entry is
 * ever rewritten, reordered, or removed (§6); [KnowledgeItem.knowledgeId],
 * [KnowledgeItem.evidenceReference], [KnowledgeItem.provenanceReference],
 * and [KnowledgeItem.evidentialState] all carried over unchanged --
 * retirement is a status change, never an evidential classification (§5,
 * §8). This class performs no persistence of any kind: the returned
 * [KnowledgeItem] and [KnowledgeRetirement] are proposed, constructed
 * values only, exactly as [DefaultKnowledgeRevisionEvaluator]'s own
 * `Applied` outcome already is.
 *
 * ## Determinism
 *
 * Qualification and the appended [KnowledgeRetirement]'s content are
 * deterministic for identical inputs (the same [item], [basis], and
 * [occurredAt]). [occurredAt] is supplied by the caller rather than read
 * from the system clock internally, mirroring
 * [DefaultKnowledgeRevisionEvaluator]'s own identical, disclosed choice.
 * This class implements no ordering rule and no lifecycle-sequence value
 * of any kind; both remain a later Unit 7 sub-unit's own, separately
 * authorised responsibility (Scope Lock Clarification §10, §11).
 */
class DefaultKnowledgeRetirementEvaluator : KnowledgeRetirementEvaluator {

    override fun evaluate(
        item: KnowledgeItem,
        basis: String,
        occurredAt: Instant,
    ): KnowledgeRetirementEvaluation {
        if (item.status != KnowledgeItemStatus.ACTIVE) {
            return KnowledgeRetirementEvaluation.NotPermittedFromCurrentStatus(
                "this KnowledgeItem's current status is ${item.status}, not ACTIVE -- Unit 7 Scope Lock " +
                    "Clarification Section 8 defines retirement exhaustively as the ACTIVE-to-RETIRED transition, " +
                    "so a retirement request against an item that is not currently ACTIVE names no transition " +
                    "this document defines and must be rejected, never treated as a silent no-op or a new " +
                    "permitted event",
            )
        }

        val retirement = KnowledgeRetirement(
            knowledgeId = item.knowledgeId,
            occurredAt = occurredAt,
            basis = basis,
        )

        val updatedItem = item.copy(
            status = KnowledgeItemStatus.RETIRED,
            history = item.history + retirement,
        )

        return KnowledgeRetirementEvaluation.Applied(updatedItem, retirement)
    }
}
