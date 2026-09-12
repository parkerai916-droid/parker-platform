package parker.core.runtime

import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.GroundedProposition
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.GroundedReviewReason
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroundedReplyTest {

    private val evidence = ReasoningContextEntry.GovernedEvidence(
        text = "The supplied document states amount 10",
        evidenceArtifactId = EvidenceArtifactId("artifact-grounded-reply"),
    )
    private val context = ReasoningContext.fromTypedEntries(listOf(evidence))

    @Test
    fun `SUPPORTED_FACT requires a governed support reference`() {
        assertFailsWith<IllegalArgumentException> {
            GroundedProposition(
                text = "amount 10",
                classification = GroundedPropositionClassification.SUPPORTED_FACT,
            )
        }
    }

    @Test
    fun `valid SUPPORTED_FACT binds to supplied governed evidence`() {
        val proposition = GroundedProposition(
            text = "amount 10",
            classification = GroundedPropositionClassification.SUPPORTED_FACT,
            supportReferences = listOf(evidence),
        )

        val reply = GroundedReply.fromContext(context, listOf(proposition))

        assertEquals(evidence, reply.propositions.single().supportReferences.single())
    }

    @Test
    fun `INFERENCE remains distinct and identifies its evidential basis`() {
        val proposition = GroundedProposition(
            text = "the transaction may be complete",
            classification = GroundedPropositionClassification.INFERENCE,
            basisReferences = listOf(evidence),
            reasoning = "derived from the supplied amount statement",
        )

        val reply = GroundedReply.fromContext(context, listOf(proposition))

        assertEquals(GroundedPropositionClassification.INFERENCE, reply.propositions.single().classification)
        assertEquals(listOf(evidence), reply.propositions.single().basisReferences)
    }

    @Test
    fun `NOT_ESTABLISHED has no fabricated support`() {
        val proposition = GroundedProposition(
            text = "the transaction was paid",
            classification = GroundedPropositionClassification.NOT_ESTABLISHED,
        )

        val reply = GroundedReply.fromContext(context, listOf(proposition))

        assertEquals(emptyList(), reply.propositions.single().supportReferences)
    }

    @Test
    fun `HUMAN_REVIEW_REQUIRED retains conflict reason`() {
        val proposition = GroundedProposition(
            text = "the meeting date is unresolved",
            classification = GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED,
            supportReferences = listOf(evidence),
            reviewReason = GroundedReviewReason.CONFLICT,
        )

        val reply = GroundedReply.fromContext(context, listOf(proposition))

        assertEquals(GroundedReviewReason.CONFLICT, reply.propositions.single().reviewReason)
    }

    @Test
    fun `malformed supported fact fails closed and cannot be downgraded`() {
        assertFailsWith<IllegalArgumentException> {
            GroundedProposition(
                text = "unsupported claim",
                classification = GroundedPropositionClassification.SUPPORTED_FACT,
                basisReferences = listOf(evidence),
            )
        }
    }

    @Test
    fun `provider cannot add a governed support entry absent from supplied context`() {
        val fabricated = ReasoningContextEntry.GovernedEvidence(
            text = "provider-fabricated evidence",
            evidenceArtifactId = EvidenceArtifactId("artifact-provider-fabricated"),
        )
        val proposition = GroundedProposition(
            text = "fabricated claim",
            classification = GroundedPropositionClassification.SUPPORTED_FACT,
            supportReferences = listOf(fabricated),
        )

        assertFailsWith<IllegalArgumentException> {
            GroundedReply.fromContext(context, listOf(proposition))
        }
    }

    @Test
    fun `structured grounded output round trips deterministically`() {
        val parser = GroundedReplyParser()
        val reply = GroundedReply.fromContext(
            context,
            listOf(
                GroundedProposition(
                    text = "amount 10",
                    classification = GroundedPropositionClassification.SUPPORTED_FACT,
                    supportReferences = listOf(evidence),
                ),
            ),
        )

        val encoded = parser.encode(reply, context)

        assertEquals(reply, parser.parse(encoded, context))
        assertEquals(encoded, parser.encode(parser.parse(encoded, context), context))
    }

    @Test
    fun `parser rejects an out of supplied context support index`() {
        val parser = GroundedReplyParser()
        val encoded = "GROUNDED_REPLY_R0\nP\tSUPPORTED_FACT\tY2xhaW0=\t99\t-\t-"

        assertFailsWith<IllegalArgumentException> { parser.parse(encoded, context) }
    }
}
