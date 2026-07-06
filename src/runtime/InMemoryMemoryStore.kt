package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.CandidateMemory
import parker.core.interfaces.MemoryId
import parker.core.interfaces.MemoryPromotionDecision
import parker.core.interfaces.MemoryPromotionPolicy
import parker.core.interfaces.MemoryQuery
import parker.core.interfaces.MemoryRecord
import parker.core.interfaces.MemoryStore

/**
 * Sprint 4, Track A, Unit A3. The first in-memory implementation of
 * [MemoryStore] (`docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md`,
 * `docs/architecture/MEMORY_CONTRACT_DESIGN.md`). Implements
 * [MemoryStore] directly -- no separate `MemoryRuntime` interface exists
 * to implement instead, per `MEMORY_CONTRACT_DESIGN.md` §9's own
 * determination -- mirroring [InMemoryIdentityService]/
 * [InMemoryToolRegistry]'s identical "one interface, one implementing
 * class" precedent.
 *
 * ## Boundary this class enforces
 *
 * [remember] performs submission, Evaluation (via the injected
 * [MemoryPromotionPolicy]), and Promotion in one call, exactly as
 * `MEMORY_CONTRACT_DESIGN.md` §9 requires: an external caller submits a
 * [CandidateMemory] and learns the outcome; there is no separate,
 * caller-facing "now promote this" operation for it to invoke instead,
 * and [MemoryPromotionPolicy] is never reachable from outside this
 * class.
 *
 * ## What this class does not do
 *
 * Per this Unit's explicit scope, this class does not implement:
 * embeddings, a vector database, a storage engine, persistence,
 * Android APIs, LLM prompts, a retrieval algorithm beyond the minimal,
 * deterministic one described on [retrieve], autonomous background
 * consolidation, autonomous retention sweeps, any Memory-to-World-Model
 * mutation, any World-Model-to-Memory automatic copying, or any
 * dependency on the Planner Runtime, the Agent Runtime, or the
 * Permission Engine. Its constructor takes only a [MemoryPromotionPolicy]
 * (defaulted to [DefaultMemoryPromotionPolicy]) -- no `EventBus`,
 * `IdentityService`, or `PermissionEngine` dependency exists, since
 * nothing in the approved architecture requires one and Memory never
 * reacts to, or triggers, any of those systems.
 */
class InMemoryMemoryStore(
    private val promotionPolicy: MemoryPromotionPolicy = DefaultMemoryPromotionPolicy(),
) : MemoryStore {

    private val mutex = Mutex()
    private val records = mutableMapOf<MemoryId, MemoryRecord>()

    /**
     * Every [MemoryId] this store has ever forgotten, retained here (not
     * in [records]) purely as an audit trail -- per
     * `docs/specifications/volume-03-core-interfaces/MemoryStore.md`'s
     * "Forgetting MUST be auditable" and `MEMORY_CONTRACT_DESIGN.md` §1's
     * "a forgotten record's `MemoryId` is not recycled." No content is
     * retained here, only the fact that this identifier once existed and
     * was forgotten -- inspectable via [wasForgotten].
     */
    private val forgottenIds = mutableSetOf<MemoryId>()

    private var nextSequence = 1L

    override suspend fun remember(candidate: CandidateMemory): MemoryPromotionDecision = mutex.withLock {
        val memoryId = MemoryId("memory-${nextSequence++}")
        when (val decision = promotionPolicy.evaluate(candidate, memoryId)) {
            is MemoryPromotionDecision.Promote -> {
                val promotedAt = Instant.now()
                records[memoryId] = MemoryRecord(
                    memoryId = memoryId,
                    category = decision.category,
                    sourceSubsystem = candidate.sourceSubsystem,
                    correlationId = candidate.correlationId,
                    promotedAt = promotedAt,
                    knowledgePayload = candidate.knowledgePayload,
                    originatingPrincipalId = candidate.originatingPrincipalId,
                    confidence = candidate.confidence,
                    sensitive = candidate.sensitive,
                    history = listOf("promoted at $promotedAt"),
                )
                decision
            }

            is MemoryPromotionDecision.Reject -> decision
        }
    }

    /**
     * The minimal, deterministic retrieval this Unit is scoped to
     * implement (`MemoryRetrievalPolicy`, the pluggable ranking seam
     * `MEMORY_CONTRACT_DESIGN.md` §8 reserves, is a deferred seam this
     * Unit does not implement). Matching is: scoped to [MemoryQuery.requestingPrincipalId]
     * (a record with no [MemoryRecord.originatingPrincipalId] is treated
     * as Principal-agnostic and visible to every requester; a record with
     * one is visible only to that same Principal); narrowed by
     * [MemoryQuery.category] if supplied; and filtered by a
     * case-insensitive substring match of [MemoryQuery.relevance] against
     * [MemoryRecord.knowledgePayload]. Results are ordered most-recently-
     * promoted first, then truncated to [MemoryQuery.maximumResults].
     *
     * "Most-recently-promoted first" is implemented via [records]' own
     * insertion order (a `LinkedHashMap`, per `mutableMapOf`'s default,
     * and every promotion is a fresh key, inserted under [mutex], so
     * insertion order is exactly promotion order) reversed, rather than
     * by comparing [MemoryRecord.promotedAt] values directly -- two
     * promotions completing within the same clock tick would otherwise
     * tie, and a tie-breaking rule based on wall-clock time is not
     * genuinely deterministic. Ordering by structure, not by a
     * potentially-colliding timestamp, is what keeps this a "fixed,
     * stable, repeatable rule," not a scored ranking.
     */
    override suspend fun retrieve(query: MemoryQuery): List<MemoryRecord> = mutex.withLock {
        val matches = records.values.filter { record ->
            (record.originatingPrincipalId == null || record.originatingPrincipalId == query.requestingPrincipalId) &&
                (query.category == null || record.category == query.category) &&
                record.knowledgePayload.contains(query.relevance, ignoreCase = true)
        }
        matches.asReversed().take(query.maximumResults)
    }

    override suspend fun forget(memoryId: MemoryId): Boolean = mutex.withLock {
        val removed = records.remove(memoryId)
        if (removed != null) {
            forgottenIds += memoryId
            true
        } else {
            false
        }
    }

    /**
     * Class-specific inspection method, not part of [MemoryStore] --
     * mirrors `InMemoryPlannerRuntime.getSessionStatus`/
     * `InMemoryTaskManagerRuntime.getTask`'s identical precedent of an
     * observability method sitting outside the formal interface. Exists
     * so a forgotten [MemoryId] can be confirmed, auditably, to have once
     * existed and been forgotten, proving [forget]'s auditability without
     * adding a public `MemoryStore` member Unit A2 never authorised.
     */
    fun wasForgotten(memoryId: MemoryId): Boolean = memoryId in forgottenIds
}
