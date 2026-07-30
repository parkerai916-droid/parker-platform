package parker.core.interfaces

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.1 (Lifecycle Event
 * Type Foundation). Pure data-shape validation for [KnowledgeLifecycleEvent],
 * [KnowledgePromotion], [KnowledgeRetirement], [KnowledgeRestoration], and
 * the widened [KnowledgeItem.history] field -- mirroring
 * `KnowledgeContractsTest.kt`'s own "pure data-shape validation" scope.
 * This suite proves only the type contract `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md`
 * §5 requires; it does not test revision evaluation, supersession
 * evaluation, retirement or restoration operations, compare-and-append,
 * or any ordering/concurrency mechanism -- none of that is implemented by
 * Unit 7.1, and none of it is exercised here.
 */
class KnowledgeLifecycleEventTest {

    private val knowledgeId = KnowledgeId("item-1")
    private val occurredAt: Instant = Instant.parse("2026-01-01T00:00:00Z")
    private val evidenceReference: MemoryCoreRecordReference =
        MemoryCoreRecordReference.ToEntity(EntityId("entity-1"))
    private val provenanceReference = ProvenanceReference(ProvenanceId("prov-1"))

    private fun promotion(
        knowledgeId: KnowledgeId = this.knowledgeId,
        basis: String = "initial promotion basis",
    ) = KnowledgePromotion(
        knowledgeId = knowledgeId,
        evidenceReference = evidenceReference,
        resultingState = EvidentialState.UNKNOWN,
        occurredAt = occurredAt,
        basis = basis,
    )

    private fun retirement(
        knowledgeId: KnowledgeId = this.knowledgeId,
        basis: String = "no longer supported",
    ) = KnowledgeRetirement(
        knowledgeId = knowledgeId,
        occurredAt = occurredAt,
        basis = basis,
    )

    private fun restoration(
        knowledgeId: KnowledgeId = this.knowledgeId,
        basis: String = "support re-established",
    ) = KnowledgeRestoration(
        knowledgeId = knowledgeId,
        evidenceReference = evidenceReference,
        occurredAt = occurredAt,
        basis = basis,
    )

    private fun item(history: List<KnowledgeLifecycleEvent> = emptyList()) = KnowledgeItem(
        knowledgeId = knowledgeId,
        evidenceReference = evidenceReference,
        provenanceReference = provenanceReference,
        evidentialState = EvidentialState.UNKNOWN,
        history = history,
    )

    // --- Lifecycle Contract: each variant is a KnowledgeLifecycleEvent ---

    @Test
    fun `KnowledgePromotion is a KnowledgeLifecycleEvent`() {
        val event: KnowledgeLifecycleEvent = promotion()
        assertIs<KnowledgePromotion>(event)
        assertEquals(knowledgeId, event.knowledgeId)
        assertEquals(occurredAt, event.occurredAt)
    }

    @Test
    fun `KnowledgeRetirement is a KnowledgeLifecycleEvent`() {
        val event: KnowledgeLifecycleEvent = retirement()
        assertIs<KnowledgeRetirement>(event)
        assertEquals(knowledgeId, event.knowledgeId)
        assertEquals(occurredAt, event.occurredAt)
    }

    @Test
    fun `KnowledgeRestoration is a KnowledgeLifecycleEvent`() {
        val event: KnowledgeLifecycleEvent = restoration()
        assertIs<KnowledgeRestoration>(event)
        assertEquals(knowledgeId, event.knowledgeId)
        assertEquals(occurredAt, event.occurredAt)
    }

    @Test
    fun `the lifecycle-event hierarchy is closed to exactly the three authorised variants`() {
        // Exhaustive `when` over a sealed interface is itself the source-level
        // closure proof: this would fail to compile (without an `else`
        // branch) were a fourth variant ever added, satisfying "closed to
        // the authorised variants at source level as far as practical in
        // Kotlin" without relying on reflection.
        val events: List<KnowledgeLifecycleEvent> = listOf(promotion(), retirement(), restoration())
        events.forEach { event ->
            val describedKind = when (event) {
                is KnowledgePromotion -> "promotion"
                is KnowledgeRetirement -> "retirement"
                is KnowledgeRestoration -> "restoration"
            }
            assertTrue(describedKind.isNotBlank())
        }
    }

    // --- History Compatibility ---

    @Test
    fun `a KnowledgeItem can contain only a promotion event`() {
        val promotion = promotion()
        val subject = item(history = listOf(promotion))

        assertEquals(listOf<KnowledgeLifecycleEvent>(promotion), subject.history)
    }

    @Test
    fun `a KnowledgeItem can contain promotion followed by retirement`() {
        val promotion = promotion()
        val retirement = retirement()
        val subject = item(history = listOf(promotion, retirement))

        assertEquals(listOf<KnowledgeLifecycleEvent>(promotion, retirement), subject.history)
    }

    @Test
    fun `a KnowledgeItem can contain promotion, retirement, and restoration in chronological list order`() {
        val promotion = promotion()
        val retirement = retirement()
        val restoration = restoration()
        val subject = item(history = listOf(promotion, retirement, restoration))

        assertEquals(3, subject.history.size)
        assertEquals(promotion, subject.history[0])
        assertEquals(retirement, subject.history[1])
        assertEquals(restoration, subject.history[2])
    }

    @Test
    fun `existing Unit 6 style construction -- history equal to listOf a single promotion -- remains valid`() {
        val promotion = promotion()
        val subject = KnowledgeItem(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            provenanceReference = provenanceReference,
            evidentialState = EvidentialState.UNKNOWN,
            status = KnowledgeItemStatus.ACTIVE,
            history = listOf(promotion),
        )

        assertEquals(KnowledgeItemStatus.ACTIVE, subject.status)
        assertEquals(listOf<KnowledgeLifecycleEvent>(promotion), subject.history)
    }

    @Test
    fun `history remains an ordinary read-only List from the public contract perspective`() {
        val subject = item(history = listOf(promotion()))

        // KnowledgeItem.history is declared List<KnowledgeLifecycleEvent>, Kotlin's
        // own read-only collection interface -- no mutating method (add,
        // remove, set) is exposed by the declared type, so none is called
        // here; this test documents that the declared contract itself
        // carries no such method, mirroring Unit 4's own original History
        // Compatibility discipline. A star-projected check is used rather
        // than a checked-generic one, since Kotlin cannot check an erased
        // type argument at runtime.
        assertTrue(subject.history is List<*>)
    }

    // --- Field Semantics ---

    @Test
    fun `KnowledgeRetirement carries its disclosed basis and occurredAt`() {
        val subject = retirement(basis = "evidence no longer supports this item")

        assertEquals("evidence no longer supports this item", subject.basis)
        assertEquals(occurredAt, subject.occurredAt)
    }

    @Test
    fun `KnowledgeRetirement exposes no resultingState -- retirement is a status change, never a classification`() {
        // KnowledgeRetirement has no resultingState property at all -- a
        // compile-time guarantee, not a runtime check. This test documents
        // that guarantee: were resultingState ever added to this type, this
        // file's own KDoc contradiction would need correcting, not merely
        // this test.
        val subject = retirement()
        assertIs<KnowledgeLifecycleEvent>(subject)
    }

    @Test
    fun `a blank basis is rejected at KnowledgeRetirement construction`() {
        assertFailsWith<IllegalArgumentException> { retirement(basis = "") }
    }

    @Test
    fun `KnowledgeRestoration carries its disclosed basis, occurredAt, and supporting evidence reference`() {
        val subject = restoration(basis = "new corroborating evidence resolved")

        assertEquals("new corroborating evidence resolved", subject.basis)
        assertEquals(occurredAt, subject.occurredAt)
        assertEquals(evidenceReference, subject.evidenceReference)
    }

    @Test
    fun `a blank basis is rejected at KnowledgeRestoration construction`() {
        assertFailsWith<IllegalArgumentException> { restoration(basis = "") }
    }

    @Test
    fun `KnowledgePromotion retains its existing fields and semantics unchanged by Unit 7-1`() {
        val subject = promotion(basis = "confidence and explicit request both reachable")

        assertEquals(knowledgeId, subject.knowledgeId)
        assertEquals(evidenceReference, subject.evidenceReference)
        assertEquals(EvidentialState.UNKNOWN, subject.resultingState)
        assertEquals(occurredAt, subject.occurredAt)
        assertEquals("confidence and explicit request both reachable", subject.basis)
    }

    @Test
    fun `a blank basis is rejected at KnowledgePromotion construction, unchanged from Unit 4`() {
        assertFailsWith<IllegalArgumentException> { promotion(basis = "") }
    }

    // --- Regression ---

    @Test
    fun `a KnowledgeItem with no history events still constructs, mirroring the original Unit 4 default`() {
        val subject = item()
        assertEquals(emptyList<KnowledgeLifecycleEvent>(), subject.history)
        assertEquals(KnowledgeItemStatus.ACTIVE, subject.status)
    }
}
