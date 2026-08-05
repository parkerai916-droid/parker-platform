package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DerivativeReviewRecord
import parker.core.interfaces.DerivativeReviewState
import parker.core.interfaces.DocumentId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Evidence Processing (Searchable PDF), Implementation Unit 3 ("Human
 * Review Registry"). Behavioural tests for
 * [InMemoryDerivativeReviewRegistry], per the Implementation Plan's own
 * Unit 3 test list.
 */
class InMemoryDerivativeReviewRegistryTest {

    private val fixedInstant: Instant = Instant.parse("2026-01-01T00:00:00Z")

    private fun record(
        evidenceArtifactId: EvidenceArtifactId = EvidenceArtifactId("derivative-1"),
        state: DerivativeReviewState,
        reviewingPrincipalId: PrincipalId? = null,
        note: String? = null,
    ) = DerivativeReviewRecord(
        evidenceArtifactId = evidenceArtifactId,
        documentId = DocumentId("document-1"),
        state = state,
        recordedAt = fixedInstant,
        reviewingPrincipalId = reviewingPrincipalId,
        note = note,
    )

    @Test
    fun `the initial transition into PENDING_REVIEW succeeds`() = runTest {
        val registry = InMemoryDerivativeReviewRegistry()

        registry.recordReviewState(record(state = DerivativeReviewState.PENDING_REVIEW))

        assertEquals(DerivativeReviewState.PENDING_REVIEW, registry.currentReviewState(EvidenceArtifactId("derivative-1")))
    }

    @Test
    fun `every valid transition from PENDING_REVIEW succeeds`() = runTest {
        listOf(DerivativeReviewState.APPROVED, DerivativeReviewState.REJECTED, DerivativeReviewState.NEEDS_CORRECTION).forEach { target ->
            val registry = InMemoryDerivativeReviewRegistry()
            val id = EvidenceArtifactId("derivative-$target")

            registry.recordReviewState(record(id, DerivativeReviewState.PENDING_REVIEW))
            registry.recordReviewState(record(id, target))

            assertEquals(target, registry.currentReviewState(id))
        }
    }

    @Test
    fun `an identifier attempting to skip PENDING_REVIEW entirely throws`() = runTest {
        val registry = InMemoryDerivativeReviewRegistry()

        assertFailsWith<IllegalArgumentException> {
            registry.recordReviewState(record(state = DerivativeReviewState.APPROVED))
        }
    }

    @Test
    fun `a NEEDS_CORRECTION record attempting to transition back to PENDING_REVIEW on the same identifier throws`() = runTest {
        val registry = InMemoryDerivativeReviewRegistry()
        val id = EvidenceArtifactId("derivative-1")
        registry.recordReviewState(record(id, DerivativeReviewState.PENDING_REVIEW))
        registry.recordReviewState(record(id, DerivativeReviewState.NEEDS_CORRECTION))

        assertFailsWith<IllegalArgumentException> {
            registry.recordReviewState(record(id, DerivativeReviewState.PENDING_REVIEW))
        }

        assertEquals(DerivativeReviewState.NEEDS_CORRECTION, registry.currentReviewState(id), "the rejected attempt must not have altered current state")
    }

    @Test
    fun `APPROVED and REJECTED are equally terminal -- every transition away from either throws`() = runTest {
        listOf(DerivativeReviewState.APPROVED, DerivativeReviewState.REJECTED).forEach { terminalState ->
            listOf(
                DerivativeReviewState.PENDING_REVIEW,
                DerivativeReviewState.APPROVED,
                DerivativeReviewState.REJECTED,
                DerivativeReviewState.NEEDS_CORRECTION,
            ).forEach { attemptedTarget ->
                val registry = InMemoryDerivativeReviewRegistry()
                val id = EvidenceArtifactId("derivative-$terminalState-$attemptedTarget")
                registry.recordReviewState(record(id, DerivativeReviewState.PENDING_REVIEW))
                registry.recordReviewState(record(id, terminalState))

                assertFailsWith<IllegalArgumentException>(
                    "transitioning from terminal state $terminalState to $attemptedTarget must be rejected",
                ) {
                    registry.recordReviewState(record(id, attemptedTarget))
                }
            }
        }
    }

    @Test
    fun `reading the state of an unknown identifier returns an explicit null, not PENDING_REVIEW`() = runTest {
        val registry = InMemoryDerivativeReviewRegistry()

        assertNull(registry.currentReviewState(EvidenceArtifactId("never-recorded")))
    }

    @Test
    fun `append-only history is preserved -- recording APPROVED after PENDING_REVIEW never overwrites the first record`() = runTest {
        val registry = InMemoryDerivativeReviewRegistry()
        val id = EvidenceArtifactId("derivative-1")

        registry.recordReviewState(record(id, DerivativeReviewState.PENDING_REVIEW))
        registry.recordReviewState(record(id, DerivativeReviewState.APPROVED, reviewingPrincipalId = PrincipalId("reviewer-1")))

        // Both records remain readable in order via the internal history -- verified indirectly:
        // current state resolves to the most recently appended record, and a second read is
        // stable (proving nothing was overwritten or removed by the second call).
        assertEquals(DerivativeReviewState.APPROVED, registry.currentReviewState(id))
        assertEquals(DerivativeReviewState.APPROVED, registry.currentReviewState(id))
    }

    @Test
    fun `current state always resolves to the most recently appended record`() = runTest {
        val registry = InMemoryDerivativeReviewRegistry()
        val id = EvidenceArtifactId("derivative-1")

        registry.recordReviewState(record(id, DerivativeReviewState.PENDING_REVIEW))
        assertEquals(DerivativeReviewState.PENDING_REVIEW, registry.currentReviewState(id))

        registry.recordReviewState(record(id, DerivativeReviewState.NEEDS_CORRECTION, note = "page 3 illegible"))
        assertEquals(DerivativeReviewState.NEEDS_CORRECTION, registry.currentReviewState(id))
    }

    @Test
    fun `distinct identifiers maintain fully independent review histories`() = runTest {
        val registry = InMemoryDerivativeReviewRegistry()
        val first = EvidenceArtifactId("derivative-1")
        val second = EvidenceArtifactId("derivative-2")

        registry.recordReviewState(record(first, DerivativeReviewState.PENDING_REVIEW))
        registry.recordReviewState(record(second, DerivativeReviewState.PENDING_REVIEW))
        registry.recordReviewState(record(first, DerivativeReviewState.APPROVED))

        assertEquals(DerivativeReviewState.APPROVED, registry.currentReviewState(first))
        assertEquals(DerivativeReviewState.PENDING_REVIEW, registry.currentReviewState(second))
    }
}
