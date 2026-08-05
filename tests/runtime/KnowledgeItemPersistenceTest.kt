package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Programme 3, Knowledge Memory, Implementation Unit 8. Behavioural tests
 * of [InMemoryKnowledgeItemPersistence] -- the internal, non-public
 * durable-storage seam [DefaultKnowledgeSubmission] writes to. See
 * `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md` §10 for
 * the constitutional reasoning; this suite is pure storage-behaviour
 * verification, mirroring `InMemoryEvidenceArtifactStorageTest.kt`'s own
 * scope for its own storage seam.
 */
class KnowledgeItemPersistenceTest {

    private fun item(
        knowledgeId: KnowledgeId = KnowledgeId("knowledge-1"),
        status: KnowledgeItemStatus = KnowledgeItemStatus.ACTIVE,
    ): KnowledgeItem {
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(
            parker.core.interfaces.AssertionId("assertion-1"),
        )
        val promotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = EvidentialState.UNKNOWN,
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            basis = "test fixture promotion",
        )
        return KnowledgeItem(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            provenanceReference = ProvenanceReference(ProvenanceId("provenance-1")),
            evidentialState = EvidentialState.UNKNOWN,
            status = status,
            history = listOf(promotion),
        )
    }

    @Test
    fun `store then find round-trips the same KnowledgeItem`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val value = item()

        persistence.store(value)

        assertEquals(value, persistence.find(value.knowledgeId))
    }

    @Test
    fun `find for a KnowledgeId nothing was ever stored under returns null`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()

        assertNull(persistence.find(KnowledgeId("never-stored")))
    }

    @Test
    fun `store returns exactly the value it was given`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val value = item()

        val stored = persistence.store(value)

        assertEquals(value, stored)
    }

    @Test
    fun `two distinct KnowledgeId values are stored and found independently`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        val first = item(KnowledgeId("knowledge-1"))
        val second = item(KnowledgeId("knowledge-2"))

        persistence.store(first)
        persistence.store(second)

        assertEquals(first, persistence.find(first.knowledgeId))
        assertEquals(second, persistence.find(second.knowledgeId))
    }

    @Test
    fun `KnowledgeItemPersistence is an internal seam, not part of the public interfaces package`() {
        assertEquals(
            "parker.core.runtime",
            KnowledgeItemPersistence::class.java.packageName,
            "the persistence seam must never live in the public parker.core.interfaces package",
        )
    }
}
