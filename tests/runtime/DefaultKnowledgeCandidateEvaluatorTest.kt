package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeCandidate
import parker.core.interfaces.KnowledgeCandidateEvaluation
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Unit 6. Unit tests of
 * [DefaultKnowledgeCandidateEvaluator]'s own multi-factor evaluation,
 * stated in full in that class's KDoc and in
 * `docs/governance/PROGRAMME_3_UNIT_6_SCOPE_LOCK_CLARIFICATION.md`. Kept
 * entirely separate from [DefaultKnowledgePromotionPolicyTest] -- the
 * legacy suite -- per the Unit 6 Clarification's own instruction that the
 * two paths are independent and neither test suite is rewritten to
 * accommodate the other.
 *
 * **Corrected against frozen Contract Design Version 2 §16.** The
 * previous version of this suite tested an unconditional-promotion
 * model, now removed. These tests establish the genuine, narrower,
 * disclosed two-factor gate (confidence + explicit request) that
 * replaced it, and confirm corroboration is detected but never relied
 * upon absent a common-origin capability that does not yet exist.
 *
 * Uses a real [InMemoryMemoryCore] (both [parker.core.interfaces.MemoryCore]
 * and [parker.core.interfaces.MemoryRetrieval]) as the evaluator's read
 * dependency, rather than a hand-written fake -- this is genuine,
 * already-tested production code, not new test infrastructure, per this
 * Unit's own instruction not to invent production infrastructure beyond
 * Unit 6.
 */
class DefaultKnowledgeCandidateEvaluatorTest {

    private val writerPrincipal = PrincipalId("test.writer")

    private fun evaluator(core: InMemoryMemoryCore) = DefaultKnowledgeCandidateEvaluator(core)

    private suspend fun newAssertion(
        core: InMemoryMemoryCore,
        statement: String = "the user prefers window seats",
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

    // --- 1. structural prerequisite: missing record ---

    @Test
    fun `a reference to a nonexistent Assertion is rejected`() = runTest {
        val core = InMemoryMemoryCore()
        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(parker.core.interfaces.AssertionId("assertion-does-not-exist"))),
        )

        val reject = assertIs<KnowledgeCandidateEvaluation.Reject>(result)
        assertTrue(reject.basis.isNotBlank())
    }

    // --- 2. structural resolvability alone does not cause promotion ---

    @Test
    fun `a resolvable Assertion with no reachable promotion factor is rejected, not promoted`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = null)

        val result = evaluator(core).evaluate(KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId)))

        val reject = assertIs<KnowledgeCandidateEvaluation.Reject>(result)
        assertTrue(reject.basis.contains("insufficient promotion basis"))
    }

    // --- 3. explicit request alone does not cause promotion ---

    @Test
    fun `explicit request alone, with no reachable confidence, is rejected`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = null)

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true),
        )

        val reject = assertIs<KnowledgeCandidateEvaluation.Reject>(result)
        assertTrue(reject.basis.contains("explicit request"))
    }

    // --- 4. confidence alone does not cause promotion ---

    @Test
    fun `confidence alone, with no explicit request, is rejected`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = 0.9)

        val result = evaluator(core).evaluate(KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId)))

        assertIs<KnowledgeCandidateEvaluation.Reject>(result)
    }

    // --- 5. missing confidence is handled honestly, never as zero ---

    @Test
    fun `absent confidence is disclosed honestly in a rejection basis, never reported as zero`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = null)

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = false),
        )

        val reject = assertIs<KnowledgeCandidateEvaluation.Reject>(result)
        assertTrue(!reject.basis.contains("confidence: 0.0"), "absent confidence must never be reported as zero")
        assertTrue(reject.basis.contains("null"), "absent confidence must be disclosed honestly")
    }

    // --- 6 & 7. a valid, authorised multi-factor case promotes, with explicit request disclosed ---

    @Test
    fun `confidence and explicit request together satisfy the gate and promote as UNKNOWN`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = 0.4)

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true),
        )

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.UNKNOWN, promote.item.evidentialState)
        assertTrue(promote.promotion.basis.contains("explicit request"), "explicit request's contribution must be disclosed in the basis")
        assertTrue(promote.promotion.basis.contains("0.4"), "confidence's contribution must be disclosed in the basis")
    }

    @Test
    fun `a low recorded confidence value, combined with explicit request, still promotes -- no invented threshold`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = 0.02)

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true),
        )

        // No numeric threshold exists in this evaluator -- a low confidence value, jointly
        // weighed with explicit request, promotes exactly as a high one would.
        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.UNKNOWN, promote.item.evidentialState)
    }

    // --- 9. SUPPORTS alone does not produce CORROBORATED_EVIDENCE ---

    @Test
    fun `a corroborated Assertion with no other reachable factor is rejected, never assigned CORROBORATED_EVIDENCE`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, statement = "the flight departs at 9am", confidence = null)
        val supporting = newAssertion(core, statement = "confirmed, 9am departure")
        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.SUPPORTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, supporting.assertionId.value),
                directional = false,
                provenanceId = subject.provenanceId,
            ),
        )

        val result = evaluator(core).evaluate(KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId)))

        val reject = assertIs<KnowledgeCandidateEvaluation.Reject>(result)
        assertTrue(reject.basis.contains("SUPPORTS"), "the detected but unrelied-upon corroboration signal must still be disclosed")
    }

    // --- 10. common-origin uncertainty prevents unauthorised corroboration reliance, even when the gate is otherwise satisfied ---

    @Test
    fun `corroboration present alongside a satisfied gate still yields UNKNOWN, never CORROBORATED_EVIDENCE`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, statement = "the flight departs at 9am", confidence = 0.6)
        val supporting = newAssertion(core, statement = "confirmed, 9am departure")
        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.SUPPORTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, supporting.assertionId.value),
                directional = false,
                provenanceId = subject.provenanceId,
            ),
        )

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true),
        )

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(
            EvidentialState.UNKNOWN,
            promote.item.evidentialState,
            "no common-origin determination capability exists, so corroboration must never upgrade the classification",
        )
        assertTrue(promote.promotion.basis.contains("SUPPORTS"), "the unrelied-upon corroboration signal must still be disclosed")
    }

    // --- 11 & 12. contradiction is its own, independent express exception ---

    @Test
    fun `a contradicted Assertion is promoted with COMPETING_EXPLANATIONS via its own exception, even with no other reachable factor`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, statement = "the meeting is on Tuesday", confidence = null)
        val contradicting = newAssertion(core, statement = "the meeting is on Wednesday")
        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.CONTRADICTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, contradicting.assertionId.value),
                directional = false,
                provenanceId = subject.provenanceId,
            ),
        )

        // No confidence, no explicit request -- the general two-factor gate would reject this
        // candidate. Contradiction promotes it anyway, via its own, separate, express exception,
        // never because the general gate was satisfied.
        val result = evaluator(core).evaluate(KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId)))

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, promote.item.evidentialState)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, promote.promotion.resultingState)
    }

    @Test
    fun `contradiction is never silently overridden by simultaneous corroboration`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, statement = "the meeting is on Tuesday")
        val supporting = newAssertion(core, statement = "yes, Tuesday")
        val contradicting = newAssertion(core, statement = "no, Wednesday")

        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.SUPPORTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, supporting.assertionId.value),
                directional = false,
                provenanceId = subject.provenanceId,
            ),
        )
        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.DISPUTES,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, contradicting.assertionId.value),
                directional = false,
                provenanceId = subject.provenanceId,
            ),
        )

        val result = evaluator(core).evaluate(KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId)))

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, promote.item.evidentialState)
    }

    // --- construction shape: both KnowledgeItem and KnowledgePromotion present, no persistence ---

    @Test
    fun `a promoted result carries both a KnowledgeItem and a KnowledgePromotion referencing the same evidence`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = 0.5)

        val candidate = KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true)
        val result = evaluator(core).evaluate(candidate)

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(candidate.evidenceReference, promote.item.evidenceReference)
        assertEquals(candidate.evidenceReference, promote.promotion.evidenceReference)
        assertEquals(promote.item.knowledgeId, promote.promotion.knowledgeId)
        assertEquals(listOf(promote.promotion), promote.item.history)
        assertEquals(subject.provenanceId, promote.item.provenanceReference.provenanceId)
    }

    // --- 14. determinism ---

    @Test
    fun `evaluating the same candidate twice yields the same KnowledgeId`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = 0.5)
        val candidate = KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true)

        val first = assertIs<KnowledgeCandidateEvaluation.Promote>(evaluator(core).evaluate(candidate))
        val second = assertIs<KnowledgeCandidateEvaluation.Promote>(evaluator(core).evaluate(candidate))

        assertEquals(first.item.knowledgeId, second.item.knowledgeId)
        assertEquals(first.item.evidentialState, second.item.evidentialState)
    }

    // --- 13. no persistence ---

    @Test
    fun `evaluation performs no persistence -- an unrelated KnowledgeStore remains empty throughout`() = runTest {
        // DefaultKnowledgeCandidateEvaluator's only constructor dependency is a MemoryRetrieval
        // (read-only) -- it holds no KnowledgeStore reference at all and is never given one, so
        // it structurally cannot write to it. This test confirms, behaviourally rather than by
        // construction alone, that an entirely separate, real InMemoryKnowledgeStore remains
        // empty before and after several evaluations run alongside it.
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = 0.5)
        val store = InMemoryKnowledgeStore()
        val principal = PrincipalId("test.reader")

        repeat(3) {
            evaluator(core).evaluate(
                KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true),
            )
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

    // --- non-Assertion evidence: confidence structurally unreachable ---

    @Test
    fun `a resolvable Entity with explicit request but no reachable confidence is rejected`() = runTest {
        val core = InMemoryMemoryCore()
        val entity = core.createEntity(
            writerPrincipal,
            parker.core.interfaces.CandidateEntity(
                entityType = "person",
                primaryLabel = "Alex",
                provenanceId = core.createProvenance(
                    writerPrincipal,
                    CandidateProvenance(
                        sourceIdentifier = "test-source",
                        sourceType = "test-harness",
                        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                        contentNature = ContentNature.ORIGINAL,
                    ),
                ).provenanceId,
            ),
        )

        // Explicit request alone is one factor; Entity-kind evidence carries no confidence
        // field at all, so the second, currently-authorised factor is structurally unreachable.
        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToEntity(entity.entityId), explicitlyRequested = true),
        )

        val reject = assertIs<KnowledgeCandidateEvaluation.Reject>(result)
        assertTrue(reject.basis.contains("insufficient promotion basis"))
    }

    // --- non-Assertion evidence: contradiction exception still applies ---

    @Test
    fun `a contradicted Entity is promoted with COMPETING_EXPLANATIONS despite having no reachable confidence`() = runTest {
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
        val entity = core.createEntity(
            writerPrincipal,
            parker.core.interfaces.CandidateEntity(entityType = "person", primaryLabel = "Alex", provenanceId = provenanceId),
        )
        val contradictingEntity = core.createEntity(
            writerPrincipal,
            parker.core.interfaces.CandidateEntity(entityType = "person", primaryLabel = "A different Alex", provenanceId = provenanceId),
        )
        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.CONTRADICTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, entity.entityId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, contradictingEntity.entityId.value),
                directional = false,
                provenanceId = provenanceId,
            ),
        )

        val result = evaluator(core).evaluate(KnowledgeCandidate(MemoryCoreRecordReference.ToEntity(entity.entityId)))

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, promote.item.evidentialState)
    }

    // --- DISPUTES receives the same treatment as CONTRADICTS, proven independently ---

    @Test
    fun `a DISPUTES relationship triggers the same contradiction exception as CONTRADICTS`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, statement = "the invoice was paid on the 3rd", confidence = null)
        val disputing = newAssertion(core, statement = "the invoice was paid on the 5th")
        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.DISPUTES,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, disputing.assertionId.value),
                directional = false,
                provenanceId = subject.provenanceId,
            ),
        )

        val result = evaluator(core).evaluate(KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId)))

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, promote.item.evidentialState)
    }

    // --- confidence = 0.0 boundary: presence, not magnitude, is what counts ---

    @Test
    fun `confidence of exactly zero, combined with explicit request, promotes -- proving no numeric threshold`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = 0.0)

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true),
        )

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.UNKNOWN, promote.item.evidentialState)
        assertTrue(promote.promotion.basis.contains("0.0"), "the zero confidence value must be disclosed, not treated as absence")
    }

    // --- contradiction is independent of the gate even when exactly one ordinary factor is present ---

    @Test
    fun `a contradicted Assertion with confidence present but no explicit request still promotes via the contradiction exception`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, statement = "the package weighs 2kg", confidence = 0.7)
        val contradicting = newAssertion(core, statement = "the package weighs 3kg")
        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.CONTRADICTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, contradicting.assertionId.value),
                directional = false,
                provenanceId = subject.provenanceId,
            ),
        )

        // explicitlyRequested left at its default (null) -- only one of the two ordinary
        // factors (confidence) is present, which would be insufficient under the general gate.
        val result = evaluator(core).evaluate(KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId)))

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.COMPETING_EXPLANATIONS, promote.item.evidentialState)
    }

    // --- multiple/duplicate relationships: ordering and repetition do not change the outcome ---

    @Test
    fun `multiple SUPPORTS relationships and a duplicate CONTRADICTS relationship do not change the classification`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, statement = "the server restarted at midnight", confidence = 0.5)
        val supportingA = newAssertion(core, statement = "confirmed, midnight restart")
        val supportingB = newAssertion(core, statement = "restart logged at 00:00")
        val contradictingA = newAssertion(core, statement = "the server restarted at noon")
        val contradictingB = newAssertion(core, statement = "restart logged at 12:00")

        // Two SUPPORTS relationships, and two separate CONTRADICTS relationships -- neither
        // repetition nor traversal order should alter the outcome: contradiction still wins,
        // via its own independent exception, exactly as with a single relationship of each kind.
        listOf(supportingA, supportingB).forEach { supporting ->
            core.createRelationship(
                writerPrincipal,
                CandidateRelationship(
                    relationshipType = Relationship.SUPPORTS,
                    fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                    toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, supporting.assertionId.value),
                    directional = false,
                    provenanceId = subject.provenanceId,
                ),
            )
        }
        listOf(contradictingA, contradictingB).forEach { contradicting ->
            core.createRelationship(
                writerPrincipal,
                CandidateRelationship(
                    relationshipType = Relationship.CONTRADICTS,
                    fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                    toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, contradicting.assertionId.value),
                    directional = false,
                    provenanceId = subject.provenanceId,
                ),
            )
        }

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true),
        )

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(
            EvidentialState.COMPETING_EXPLANATIONS,
            promote.item.evidentialState,
            "repeated relationships of the same kind must not change the classification",
        )
    }

    // --- Parker Conversational Memory Bridge, Admission Unit: the new, express single-factor
    // --- exception (docs/governance/PROGRAMME_3_EXPLICIT_OWNER_INSTRUCTION_PROMOTION_EXCEPTION_SCOPE_LOCK_CLARIFICATION.md) ---

    @Test
    fun `soleBasisIsExplicitInstruction alone promotes an Assertion with no confidence and no explicitlyRequested`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = null)

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), soleBasisIsExplicitInstruction = true),
        )

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.UNKNOWN, promote.item.evidentialState, "the exception must never assign a stronger state than the ordinary two-factor gate itself ever produces")
        assertTrue(
            promote.promotion.basis.contains("explicit, deterministic owner instruction"),
            "the basis must honestly disclose that promotion rests solely on the instruction, with no independent evidential weight",
        )
    }

    @Test
    fun `soleBasisIsExplicitInstruction promotes an Entity, which carries no confidence field at all`() = runTest {
        val core = InMemoryMemoryCore()
        val entity = core.createEntity(
            writerPrincipal,
            parker.core.interfaces.CandidateEntity(
                entityType = "person",
                primaryLabel = "Stellar",
                provenanceId = core.createProvenance(
                    writerPrincipal,
                    CandidateProvenance(
                        sourceIdentifier = "test-source",
                        sourceType = "test-harness",
                        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                        contentNature = ContentNature.ORIGINAL,
                    ),
                ).provenanceId,
            ),
        )

        // The ordinary two-factor gate structurally cannot promote Entity-kind evidence at all
        // (confidence is unreachable for it) -- this exception is the one, narrow way an
        // Entity-kind candidate can be promoted absent Contradiction.
        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToEntity(entity.entityId), soleBasisIsExplicitInstruction = true),
        )

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(EvidentialState.UNKNOWN, promote.item.evidentialState)
    }

    @Test
    fun `soleBasisIsExplicitInstruction false has no effect -- the ordinary gate still applies`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = null)

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), soleBasisIsExplicitInstruction = false),
        )

        val reject = assertIs<KnowledgeCandidateEvaluation.Reject>(result)
        assertTrue(reject.basis.contains("insufficient promotion basis"))
    }

    @Test
    fun `explicitlyRequested true, without soleBasisIsExplicitInstruction, does not trigger the new exception -- the two fields are independent`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, confidence = null)

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), explicitlyRequested = true),
        )

        // Identical outcome to the pre-existing "explicit request alone... is rejected" test --
        // restated here specifically to prove the new field's absence, not merely
        // explicitlyRequested's own already-established insufficiency.
        val reject = assertIs<KnowledgeCandidateEvaluation.Reject>(result)
        assertTrue(reject.basis.contains("insufficient promotion basis"))
    }

    @Test
    fun `contradiction still takes priority over soleBasisIsExplicitInstruction -- COMPETING_EXPLANATIONS, not the plain instruction-only basis`() = runTest {
        val core = InMemoryMemoryCore()
        val subject = newAssertion(core, statement = "the mug is black")
        val contradicting = newAssertion(core, statement = "the mug is white")
        core.createRelationship(
            writerPrincipal,
            CandidateRelationship(
                relationshipType = Relationship.CONTRADICTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, subject.assertionId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, contradicting.assertionId.value),
                directional = false,
                provenanceId = subject.provenanceId,
            ),
        )

        val result = evaluator(core).evaluate(
            KnowledgeCandidate(MemoryCoreRecordReference.ToAssertion(subject.assertionId), soleBasisIsExplicitInstruction = true),
        )

        val promote = assertIs<KnowledgeCandidateEvaluation.Promote>(result)
        assertEquals(
            EvidentialState.COMPETING_EXPLANATIONS,
            promote.item.evidentialState,
            "an unresolved contradiction must still be honestly disclosed even for an explicit-instruction candidate",
        )
    }
}
