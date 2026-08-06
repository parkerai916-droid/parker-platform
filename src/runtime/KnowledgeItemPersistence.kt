package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem

/**
 * Programme 3, Knowledge Memory, Implementation Unit 8 (Knowledge
 * Submission). The minimal internal durable-storage seam
 * [DefaultKnowledgeSubmission] requires to persist a successfully
 * promoted [KnowledgeItem] -- see
 * `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md`
 * ("the Unit 8 Clarification") §10 for the constitutional reasoning.
 *
 * Deliberately `internal`, never added to `src/interfaces/` -- the Unit 8
 * Clarification's own governing task requires this seam to remain
 * internal and never become a public contract. No caller outside
 * `parker.core.runtime` may construct, depend upon, or observe this type;
 * it exists solely so [DefaultKnowledgeSubmission] has somewhere to write
 * a promoted [KnowledgeItem], never as a second, competing public storage
 * contract alongside the legacy [parker.core.interfaces.KnowledgeStore] or
 * a future Unit 9 retrieval surface, neither of which this Unit touches.
 *
 * Two operations only, both deliberately minimal:
 *
 * - [store] persists exactly one [KnowledgeItem], keyed by its own
 *   [KnowledgeItem.knowledgeId]. This Unit does not decide, and this
 *   interface does not need to decide, what happens if an identical
 *   [KnowledgeId] is ever stored twice -- that question belongs to
 *   lifecycle work ([KnowledgeRevisionEvaluator]/
 *   [parker.core.interfaces.KnowledgeRetirementEvaluator], already
 *   implemented elsewhere as pure functions this Unit does not touch or
 *   persist the output of). [InMemoryKnowledgeItemPersistence]'s own
 *   implementation simply replaces any prior value for that key, since
 *   Unit 8 itself only ever calls [store] once per successful promotion,
 *   for a [KnowledgeId] [DefaultKnowledgeCandidateEvaluator] has already
 *   guaranteed is deterministically derived from the candidate's own
 *   evidence reference.
 * - [find] exists so this Unit's own tests can observe whether persistence
 *   occurred, and so a later unit may reuse this seam without inventing a
 *   second one. On its own, it was not a retrieval capability of any kind
 *   in the constitutional sense -- Unit 9's own Knowledge Query/Knowledge
 *   Result surface was, at the time this Unit was written, entirely
 *   separate, unauthorised, and unbegun.
 * - [findAll], added by Programme 3, Knowledge Memory, Implementation
 *   Unit 9.2 (Deterministic Retrieval Engine, `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md`
 *   §4), is the minimal enumeration capability that Unit's own retrieval
 *   engine requires and this seam did not previously expose -- structural
 *   query execution against Knowledge Memory's own held state (the Unit 9
 *   Contract Design §2) is impossible without some way to iterate every
 *   currently stored [KnowledgeItem]. This remains a purely internal,
 *   non-constitutional addition to an already-internal seam: it adds no
 *   new public contract, does not appear in `src/interfaces/`, and does
 *   not alter [store] or [find]'s own existing behaviour in any way.
 *   Returned in the same insertion order [InMemoryKnowledgeItemPersistence]'s
 *   own backing map already, natively preserves -- this is the one
 *   disclosed, deterministic ordering guarantee Unit 9.2's own retrieval
 *   engine relies upon (Unit 9 Contract Design §8), not an ordering this
 *   method computes or sorts by itself.
 *
 * No transaction technology, database choice, or file format is decided
 * here (Unit 8 Clarification §10) -- this Unit selects only an in-memory
 * implementation, mirroring every other Programme 2/3 Version-1
 * precedent ([InMemoryKnowledgeStore], [InMemoryMemoryCore]).
 */
internal interface KnowledgeItemPersistence {
    suspend fun store(item: KnowledgeItem): KnowledgeItem
    suspend fun find(knowledgeId: KnowledgeId): KnowledgeItem?
    suspend fun findAll(): List<KnowledgeItem>
}

/**
 * The sole implementation of [KnowledgeItemPersistence] this Unit adds --
 * an in-memory, mutex-guarded map, mirroring [InMemoryKnowledgeStore]'s
 * and [InMemoryMemoryCore]'s own identical concurrency-safety pattern
 * exactly (a single [Mutex] guarding a single [MutableMap], never a
 * concurrent collection relied upon for its own, unaudited safety
 * guarantees).
 */
internal class InMemoryKnowledgeItemPersistence : KnowledgeItemPersistence {

    private val mutex = Mutex()
    private val items = mutableMapOf<KnowledgeId, KnowledgeItem>()

    override suspend fun store(item: KnowledgeItem): KnowledgeItem = mutex.withLock {
        items[item.knowledgeId] = item
        item
    }

    override suspend fun find(knowledgeId: KnowledgeId): KnowledgeItem? = mutex.withLock {
        items[knowledgeId]
    }

    override suspend fun findAll(): List<KnowledgeItem> = mutex.withLock {
        items.values.toList()
    }
}
