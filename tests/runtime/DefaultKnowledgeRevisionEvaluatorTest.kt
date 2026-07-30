package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRevisionEvaluation
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Unit 7.2. Unit tests of
 * [DefaultKnowledgeRevisionEvaluator]'s own revision-qualification and
 * revision-evaluation logic, stated in full in that class's KDoc and in
 * `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §7,
 * §15. Kept entirely separate from [DefaultKnowledgeCandidateEvaluatorTest]
 * -- initial promotion (Unit 6) and revision (Unit 7.2) are independent,
 * separately governed seams.
 *
 * Uses a real [InMemoryMemoryCore] (both [parker.core.interfaces.MemoryCore]
 * and [parker.core.interfaces.MemoryRetrieval]) as the evaluator's read
 * dependency, mirroring [DefaultKnowledgeCandidateEvaluatorTest]'s own
 * choice not to invent new test infrastructure.
 */
class DefaultKnowledgeRevisionEvaluatorTest {

    private val writerPrincipal = PrincipalId("test.writer")
    private val existingKnowledgeId = KnowledgeId("item-1")

    private fun evaluator(core: InMemoryMemoryCore) = DefaultKnowledgeRevisionEvaluator(core)

    private suspend fun newAssertion(
        core: InMemoryMemoryCore,
        statement: String,
        confidence: Double? = null,
    ) = core.createAssertion(
        writerPrincipal,
        CandidateAssertion(
            statement = statement,
            provenanceId = core.createProvenance(
                writerPrincipal,
                CandidateProvenance(
                    sourceIdentifier = "test-source",
                    sourceType = "test-harness",
                    acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                    contentNature = ContentNature.ORIGINAL,
                ),
            ).provenanceId,
            confidence = confidence,
        ),
    )

    /** An already-promoted [KnowledgeItem] whose sole history entry cites [evidenceReference]. */
    private fun existingItem(
        evidenceReference: MemoryCoreRecordReference,
        provenanceId: parker.core.interfaces.ProvenanceId,
        evidentialState: EvidentialState = EvidentialState.UNKNOWN,
    ): KnowledgeItem {
        val initialPromotion = KnowledgePromotion(
            knowledgeId = existingKnowledgeId,
            evidenceReference = evidenceReference,
            resultingState = evidentialState,
            occurredAt = Instant.parse("2026-01-01T00:00:00Z"),
            basis = "initial promotion, fixed for test setup",
        )
        return KnowledgeItem(
            knowledgeId = existingKnowledgeId,
            evidenceReference = evidenceReference,
            provenanceReference = ProvenanceReference(provenanceId),
            evidentialState = evidentialState,
            history = listOf(initialPromotion),
        )
    }

    private suspend fun relate(
        core: InMemoryMemoryCore,
        type: String,
        from: parker.core.interfaces.AssertionId,
        to: parker.core.interfaces.AssertionId,
        provenanceId: parker.core.interfaces.ProvenanceId,
    ) = core.createRelationship(
        writerPrincipal,
        CandidateRelationship(
            relationshipType = type,
            fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, from.value),
            toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, to.value),
            directional = false,
            provenanceId = provenanceId,
        ),
    )

    private val revisionOccurredAt: Instant = Instant.parse("2026-06-01T00:00:00Z")

    // --- Qualification: each of the four relationship types qualifies ---

    @Test
    fun `an AMENDS relationship qualifies a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the meeting is at 3pm", confidence = 0.5)
        val newEvidence = newAssertion(core, "the meeting is at 3pm, confirmed by calendar invite")
        relate(core, Relationship.AMENDS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.Applied>(result)
    }

    @Test
    fun `a SUPERSEDES relationship qualifies a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 14 Elm St")
        relate(core, Relationship.SUPERSEDES, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.Applied>(result)
    }

    @Test
    fun `a CONTRADICTS relationship qualifies a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the package weighs 2kg")
        val newEvidence = newAssertion(core, "the package weighs 3kg")
        relate(core, Relationship.CONTRADICTS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.Applied>(result)
    }

    @Test
    fun `a DISPUTES relationship qualifies a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the invoice was paid on the 3rd")
        val newEvidence = newAssertion(core, "the invoice was paid on the 5th")
        relate(core, Relationship.DISPUTES, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.Applied>(result)
    }

    @Test
    fun `a Memory Core status transition on the already-cited evidence qualifies a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the flight departs at 9am")
        val newEvidence = newAssertion(core, "the flight departs at 10am")
        // No relationship at all links the two -- only the already-cited evidence's own status
        // transition (Section 7's second qualifying case) is exercised here.
        core.transitionStatus(writerPrincipal, MemoryCoreRecordReference.ToAssertion(existing.assertionId), MemoryCoreRecordStatus.DISPUTED)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.Applied>(result)
    }

    // --- Qualification: rejection cases ---

    @Test
    fun `entirely unrelated evidence, with no relationship and no status transition, does not qualify`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the user prefers window seats")
        val unrelated = newAssertion(core, "the weather in Paris is mild in April")
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(unrelated.assertionId), revisionOccurredAt)

        val notQualifying = assertIs<KnowledgeRevisionEvaluation.NotQualifyingRevision>(result)
        assertTrue(notQualifying.basis.isNotBlank())
    }

    @Test
    fun `a SUPPORTS relationship alone does not qualify a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the flight departs at 9am")
        val corroborating = newAssertion(core, "confirmed, 9am departure")
        relate(core, Relationship.SUPPORTS, corroborating.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(corroborating.assertionId), revisionOccurredAt)

        val notQualifying = assertIs<KnowledgeRevisionEvaluation.NotQualifyingRevision>(result)
        assertTrue(notQualifying.basis.contains("SUPPORTS"), "the detected but non-qualifying SUPPORTS relationship must still be disclosed")
    }

    // --- Revision evaluation: state change, no-change, history growth, prior-event preservation ---

    @Test
    fun `a CONTRADICTS-qualified revision changes an UNKNOWN item to COMPETING_EXPLANATIONS`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the meeting is on Tuesday")
        val newEvidence = newAssertion(core, "the meeting is on Wednesday")
        relate(core, Relationship.CONTRADICTS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId, EvidentialState.UNKNOWN)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, applied.item.evidentialState)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, applied.promotion.resultingState)
    }

    @Test
    fun `an AMENDS-qualified revision with no contradiction still appends an event even though the classification does not change`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 12 Elm St, unit 4")
        relate(core, Relationship.AMENDS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId, EvidentialState.UNKNOWN)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(EvidentialState.UNKNOWN, applied.item.evidentialState, "no contradiction is present, so the classification honestly remains UNKNOWN")
        assertEquals(2, applied.item.history.size, "a qualifying no-change revision must still append a new event")
    }

    @Test
    fun `history length increases by exactly one and the previous event is preserved unchanged`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 14 Elm St")
        relate(core, Relationship.SUPERSEDES, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)
        val originalFirstEvent = item.history.first()

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(item.history.size + 1, applied.item.history.size)
        assertEquals(originalFirstEvent, applied.item.history[0], "the original promotion event must remain unchanged, in place")
        assertEquals(applied.promotion, applied.item.history[1])
    }

    // --- Supersession: both qualifying triggers produce an ordinary revision, not a distinct outcome ---

    @Test
    fun `SUPERSEDES produces an ordinary Applied revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the phone number is 555-0100")
        val newEvidence = newAssertion(core, "the phone number is 555-0199")
        relate(core, Relationship.SUPERSEDES, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(newEvidence.assertionId.value, (applied.item.evidenceReference as MemoryCoreRecordReference.ToAssertion).assertionId.value)
    }

    @Test
    fun `a SUPERSEDED status transition produces an ordinary Applied revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the phone number is 555-0100")
        val newEvidence = newAssertion(core, "the phone number is 555-0199")
        core.transitionStatus(writerPrincipal, MemoryCoreRecordReference.ToAssertion(existing.assertionId), MemoryCoreRecordStatus.SUPERSEDED)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.Applied>(result)
    }

    // --- Result contract shape ---

    @Test
    fun `Applied carries an updated KnowledgeItem and a KnowledgePromotion referencing the new evidence`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the meeting is on Tuesday")
        val newEvidence = newAssertion(core, "the meeting is on Wednesday")
        relate(core, Relationship.CONTRADICTS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(item.knowledgeId, applied.item.knowledgeId, "KnowledgeId is stable across revision")
        assertEquals(MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), applied.promotion.evidenceReference)
        assertEquals(revisionOccurredAt, applied.promotion.occurredAt)
        assertEquals(applied.promotion, applied.item.history.last())
    }

    @Test
    fun `NotQualifyingRevision carries a disclosed, non-blank basis and no KnowledgeItem`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the user prefers window seats")
        val unrelated = newAssertion(core, "the coffee shop opens at 7am")
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(unrelated.assertionId), revisionOccurredAt)

        val notQualifying = assertIs<KnowledgeRevisionEvaluation.NotQualifyingRevision>(result)
        assertTrue(notQualifying.basis.contains("Unit 6"), "the disclosed basis should point toward the alternative Unit 6 promotion path")
    }

    @Test
    fun `StructurallyUnresolvable is returned when the already-cited evidence no longer resolves`() = runTest {
        val core = InMemoryMemoryCore()
        val newEvidence = newAssertion(core, "the meeting is on Wednesday")
        val item = existingItem(
            MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-does-not-exist")),
            newEvidence.provenanceId,
        )

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.StructurallyUnresolvable>(result)
    }

    @Test
    fun `StructurallyUnresolvable is returned when the newly submitted evidence does not resolve`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the meeting is on Tuesday")
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(
            item,
            MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-does-not-exist")),
            revisionOccurredAt,
        )

        assertIs<KnowledgeRevisionEvaluation.StructurallyUnresolvable>(result)
    }

    // --- Regression: no retirement or restoration behaviour leaks in ---

    @Test
    fun `a qualifying revision never changes KnowledgeItemStatus and never appends a retirement or restoration event`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 14 Elm St")
        relate(core, Relationship.SUPERSEDES, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(parker.core.interfaces.KnowledgeItemStatus.ACTIVE, applied.item.status)
        applied.item.history.forEach { event -> assertIs<KnowledgePromotion>(event) }
    }

    // --- Determinism ---

    @Test
    fun `evaluating the same revision twice yields the same resulting EvidentialState and appended content`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the meeting is on Tuesday")
        val newEvidence = newAssertion(core, "the meeting is on Wednesday")
        relate(core, Relationship.CONTRADICTS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val first = assertIs<KnowledgeRevisionEvaluation.Applied>(
            evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt),
        )
        val second = assertIs<KnowledgeRevisionEvaluation.Applied>(
            evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt),
        )

        assertEquals(first.item.evidentialState, second.item.evidentialState)
        assertEquals(first.promotion.resultingState, second.promotion.resultingState)
        assertEquals(first.promotion.basis, second.promotion.basis)
    }

    // --- Relationship direction: AMENDS/SUPERSEDES qualify only new -> existing ---

    @Test
    fun `a backwards-oriented AMENDS relationship does not qualify a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 12 Elm St, unit 4")
        // Backwards: existing AMENDS newEvidence, not newEvidence AMENDS existing.
        relate(core, Relationship.AMENDS, existing.assertionId, newEvidence.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val notQualifying = assertIs<KnowledgeRevisionEvaluation.NotQualifyingRevision>(result)
        assertTrue(notQualifying.basis.contains("wrong direction"), "the misdirected relationship must be disclosed, not silently ignored")
    }

    @Test
    fun `a backwards-oriented SUPERSEDES relationship does not qualify a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the phone number is 555-0100")
        val newEvidence = newAssertion(core, "the phone number is 555-0199")
        // Backwards: existing SUPERSEDES newEvidence, not newEvidence SUPERSEDES existing.
        relate(core, Relationship.SUPERSEDES, existing.assertionId, newEvidence.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.NotQualifyingRevision>(result)
    }

    @Test
    fun `a backwards-oriented CONTRADICTS relationship still qualifies -- CONTRADICTS is direction-agnostic`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the meeting is on Tuesday")
        val newEvidence = newAssertion(core, "the meeting is on Wednesday")
        // Backwards relative to the AMENDS/SUPERSEDES convention -- CONTRADICTS does not care.
        relate(core, Relationship.CONTRADICTS, existing.assertionId, newEvidence.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, applied.item.evidentialState)
    }

    @Test
    fun `a backwards-oriented DISPUTES relationship still qualifies -- DISPUTES is direction-agnostic`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the invoice was paid on the 3rd")
        val newEvidence = newAssertion(core, "the invoice was paid on the 5th")
        relate(core, Relationship.DISPUTES, existing.assertionId, newEvidence.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, applied.item.evidentialState)
    }

    // --- Status qualification: exactly DISPUTED and SUPERSEDED, nothing else ---

    @Test
    fun `DISPUTED status alone qualifies a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the flight departs at 9am")
        val newEvidence = newAssertion(core, "the flight departs at 10am")
        core.transitionStatus(writerPrincipal, MemoryCoreRecordReference.ToAssertion(existing.assertionId), MemoryCoreRecordStatus.DISPUTED)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.Applied>(result)
    }

    @Test
    fun `SUPERSEDED status alone qualifies a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the phone number is 555-0100")
        val newEvidence = newAssertion(core, "the phone number is 555-0199")
        core.transitionStatus(writerPrincipal, MemoryCoreRecordReference.ToAssertion(existing.assertionId), MemoryCoreRecordStatus.SUPERSEDED)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.Applied>(result)
    }

    @Test
    fun `ACTIVE status alone does not qualify a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the user prefers window seats")
        val unrelated = newAssertion(core, "the weather in Paris is mild in April")
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        // existing's status remains ACTIVE (the default) throughout -- no relationship links the two either.
        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(unrelated.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.NotQualifyingRevision>(result)
    }

    @Test
    fun `ARCHIVED status alone does not qualify a revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the user prefers window seats")
        val unrelated = newAssertion(core, "the weather in Paris is mild in April")
        core.transitionStatus(writerPrincipal, MemoryCoreRecordReference.ToAssertion(existing.assertionId), MemoryCoreRecordStatus.ARCHIVED)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(unrelated.assertionId), revisionOccurredAt)

        val notQualifying = assertIs<KnowledgeRevisionEvaluation.NotQualifyingRevision>(result)
        assertTrue(notQualifying.basis.contains("ARCHIVED"), "the non-qualifying current status should still be disclosed honestly")
    }

    @Test
    fun `DELETED status alone does not qualify a revision, even though the record remains structurally resolvable here`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the user prefers window seats")
        val unrelated = newAssertion(core, "the weather in Paris is mild in April")
        core.transitionStatus(writerPrincipal, MemoryCoreRecordReference.ToAssertion(existing.assertionId), MemoryCoreRecordStatus.DELETED)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        // This in-memory implementation's getAssertion is a plain map lookup, independent of status, so the
        // DELETED record remains resolvable here -- this test asserts the correct outcome for that concrete
        // behaviour (NotQualifyingRevision) rather than assuming the general MemoryRetrieval interface
        // guarantees resolvability for a DELETED record, which it does not.
        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(unrelated.assertionId), revisionOccurredAt)

        assertIs<KnowledgeRevisionEvaluation.NotQualifyingRevision>(result)
    }

    // --- Multiple and duplicate relationships ---

    @Test
    fun `AMENDS plus CONTRADICTS together still produce COMPETING_EXPLANATIONS`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the package weighs 2kg")
        val newEvidence = newAssertion(core, "the package weighs 3kg")
        relate(core, Relationship.AMENDS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        relate(core, Relationship.CONTRADICTS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, applied.item.evidentialState)
    }

    @Test
    fun `SUPERSEDES plus DISPUTES together still produce COMPETING_EXPLANATIONS`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 14 Elm St")
        relate(core, Relationship.SUPERSEDES, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        relate(core, Relationship.DISPUTES, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, applied.item.evidentialState)
    }

    @Test
    fun `duplicate AMENDS relationships do not change the qualification or classification outcome`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 12 Elm St, unit 4")
        relate(core, Relationship.AMENDS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        relate(core, Relationship.AMENDS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(EvidentialState.UNKNOWN, applied.item.evidentialState)
        assertEquals(2, applied.item.history.size, "duplicate relationships must not append more than one event")
    }

    @Test
    fun `SUPPORTS plus CONTRADICTS still qualifies and classifies through CONTRADICTS`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the meeting is on Tuesday")
        val supporting = newAssertion(core, "yes, Tuesday")
        val newEvidence = newAssertion(core, "no, Wednesday")
        relate(core, Relationship.SUPPORTS, supporting.assertionId, existing.assertionId, existing.provenanceId)
        relate(core, Relationship.CONTRADICTS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, applied.item.evidentialState)
    }

    // --- Existing COMPETING_EXPLANATIONS state and provenance ---

    @Test
    fun `an existing COMPETING_EXPLANATIONS item remains COMPETING_EXPLANATIONS after a non-contradicting qualifying revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 12 Elm St, unit 4")
        relate(core, Relationship.AMENDS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(
            MemoryCoreRecordReference.ToAssertion(existing.assertionId),
            existing.provenanceId,
            EvidentialState.COMPETING_EXPLANATIONS,
        )

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(
            EvidentialState.COMPETING_EXPLANATIONS,
            applied.item.evidentialState,
            "a non-contradicting revision must never silently resolve an existing, disclosed contradiction",
        )
        assertTrue(applied.promotion.basis.contains("preserved"), "the sticky-state reasoning should be disclosed")
    }

    @Test
    fun `provenanceReference updates to the newly qualified evidence's own provenance`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 14 Elm St")
        relate(core, Relationship.SUPERSEDES, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(newEvidence.provenanceId, applied.item.provenanceReference.provenanceId)
    }

    @Test
    fun `prior history retains earlier evidence and provenance after a subsequent revision`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the address is 12 Elm St")
        val newEvidence = newAssertion(core, "the address is 14 Elm St")
        relate(core, Relationship.SUPERSEDES, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        val firstEvent = assertIs<KnowledgePromotion>(applied.item.history[0])
        assertEquals(MemoryCoreRecordReference.ToAssertion(existing.assertionId), firstEvent.evidenceReference)
    }

    // --- Record-kind coverage: revision qualification is not Assertion-specific ---

    @Test
    fun `a qualifying revision evaluates identically for Entity-kind evidence`() = runTest {
        val core = InMemoryMemoryCore()
        val provenanceId = core.createProvenance(
            writerPrincipal,
            CandidateProvenance(
                sourceIdentifier = "test-source",
                sourceType = "test-harness",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                contentNature = ContentNature.ORIGINAL,
            ),
        ).provenanceId
        val existing = core.createEntity(
            writerPrincipal,
            parker.core.interfaces.CandidateEntity(entityType = "person", primaryLabel = "Alex", provenanceId = provenanceId),
        )
        val newEvidence = core.createEntity(
            writerPrincipal,
            parker.core.interfaces.CandidateEntity(entityType = "person", primaryLabel = "A different Alex", provenanceId = provenanceId),
        )
        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.CONTRADICTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, newEvidence.entityId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, existing.entityId.value),
                directional = false,
                provenanceId = provenanceId,
            ),
        )
        val item = existingItem(MemoryCoreRecordReference.ToEntity(existing.entityId), provenanceId)

        val result = evaluator(core).evaluate(item, MemoryCoreRecordReference.ToEntity(newEvidence.entityId), revisionOccurredAt)

        val applied = assertIs<KnowledgeRevisionEvaluation.Applied>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, applied.item.evidentialState)
    }

    // --- No persistence ---

    @Test
    fun `evaluation performs no persistence -- an unrelated KnowledgeStore remains empty throughout`() = runTest {
        val core = InMemoryMemoryCore()
        val existing = newAssertion(core, "the meeting is on Tuesday")
        val newEvidence = newAssertion(core, "the meeting is on Wednesday")
        relate(core, Relationship.CONTRADICTS, newEvidence.assertionId, existing.assertionId, existing.provenanceId)
        val item = existingItem(MemoryCoreRecordReference.ToAssertion(existing.assertionId), existing.provenanceId)
        val store = InMemoryKnowledgeStore()
        val principal = PrincipalId("test.reader")

        repeat(3) {
            evaluator(core).evaluate(item, MemoryCoreRecordReference.ToAssertion(newEvidence.assertionId), revisionOccurredAt)
        }

        val allRecords = store.retrieve(
            parker.core.interfaces.KnowledgeQuery(
                requestingPrincipalId = principal,
                relevance = "anything",
                correlationId = "corr-1",
                maximumResults = 100,
            ),
        )
        assertTrue(allRecords.isEmpty(), "no KnowledgeRecord should exist -- evaluation must never persist")
    }
}
