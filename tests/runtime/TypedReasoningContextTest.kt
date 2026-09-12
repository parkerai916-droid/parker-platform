package parker.core.runtime

import parker.core.interfaces.AssertionId
import parker.core.interfaces.ConversationId
import parker.core.interfaces.CorrelationId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.InboundOwnerMessage
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.ReasoningProviderRequest
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject
import parker.core.interfaces.Turn
import parker.core.interfaces.TurnId
import parker.core.interfaces.ModuleId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TypedReasoningContextTest {

    private fun turn(text: String = "summarise the supplied material") = Turn(
        turnId = TurnId("turn-typed-context"),
        conversationId = ConversationId("conversation-typed-context"),
        message = InboundOwnerMessage(
            channelId = ModuleId("channel.typed-context"),
            senderPrincipalId = parker.core.interfaces.PrincipalId("owner-typed-context"),
            text = text,
            timestamp = Instant.EPOCH,
            correlationId = CorrelationId("correlation-typed-context"),
        ),
        receivedAt = Instant.EPOCH,
    )

    @Test
    fun `governed evidence identity survives typed context construction`() {
        val artifact = EvidenceArtifactId("artifact-typed-context")
        val entry = ReasoningContextEntry.GovernedEvidence(
            text = "Evidence content supplied for analysis",
            evidenceArtifactId = artifact,
        )

        val context = ReasoningContext.fromTypedEntries(listOf(entry))

        val governed = assertIs<ReasoningContextEntry.GovernedEvidence>(context.suppliedGovernedEntries.single())
        assertEquals(artifact, governed.evidenceArtifactId)
    }

    @Test
    fun `Knowledge and Memory Core identity plus provenance survive typed context construction`() {
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-typed-context"))
        val provenance = ProvenanceReference(ProvenanceId("provenance-typed-context"))
        val entry = ReasoningContextEntry.GovernedKnowledge(
            text = "Knowledge content supplied for analysis",
            knowledgeId = KnowledgeId("knowledge-typed-context"),
            evidenceReference = evidenceReference,
            provenanceReference = provenance,
        )

        val context = ReasoningContext.fromTypedEntries(listOf(entry))

        val governed = assertIs<ReasoningContextEntry.GovernedKnowledge>(context.suppliedGovernedEntries.single())
        assertEquals(evidenceReference, governed.evidenceReference)
        assertEquals(provenance, governed.provenanceReference)
        assertEquals(KnowledgeId("knowledge-typed-context"), governed.knowledgeId)
    }

    @Test
    fun `ordinary conversational text remains source compatible and renders unchanged`() {
        val context = ReasoningContext(listOf("ordinary conversational context"))

        assertEquals(listOf("ordinary conversational context"), context.entries)
        assertIs<ReasoningContextEntry.PlainText>(context.typedEntries.single())
        assertTrue(DefaultReasoningPromptBuilder().buildPrompt(turn(), context).contains("ordinary conversational context"))
    }

    @Test
    fun `provider rendering uses text while Parker retains the structural supplied set`() {
        val context = ReasoningContext.fromTypedEntries(
            listOf(
                ReasoningContextEntry.PlainText("plain context"),
                ReasoningContextEntry.GovernedEvidence(
                    text = "governed evidence text",
                    evidenceArtifactId = EvidenceArtifactId("artifact-rendered"),
                ),
            ),
        )
        val prompt = DefaultReasoningPromptBuilder().buildPrompt(turn(), context)

        assertTrue(prompt.contains("plain context"))
        assertTrue(prompt.contains("governed evidence text"))
        assertEquals(1, context.suppliedGovernedEntries.size)
        assertEquals(EvidenceArtifactId("artifact-rendered"), (context.suppliedGovernedEntries.single() as ReasoningContextEntry.GovernedEvidence).evidenceArtifactId)
    }

    @Test
    fun `provider output cannot enlarge the supplied context set`() {
        val context = ReasoningContext.fromTypedEntries(
            listOf(
                ReasoningContextEntry.GovernedEvidence(
                    text = "only supplied evidence",
                    evidenceArtifactId = EvidenceArtifactId("artifact-only"),
                ),
            ),
        )
        val request = ReasoningProviderRequest(ReasoningSubject.OfTurn(turn()), context)
        val response = ReasoningProviderResponse.Reply("provider mentions artifact-fabricated")

        assertIs<ReasoningProviderResponse.Reply>(response)
        assertEquals(1, request.reasoningContext.suppliedGovernedEntries.size)
        assertEquals(
            EvidenceArtifactId("artifact-only"),
            (request.reasoningContext.suppliedGovernedEntries.single() as ReasoningContextEntry.GovernedEvidence).evidenceArtifactId,
        )
    }

    @Test
    fun `malformed typed context fails closed instead of flattening identity`() {
        val entry = ReasoningContextEntry.GovernedEvidence(
            text = "actual rendered content",
            evidenceArtifactId = EvidenceArtifactId("artifact-malformed"),
        )

        assertFailsWith<IllegalArgumentException> {
            ReasoningContext(
                entries = listOf("different rendered content"),
                typedEntries = listOf(entry),
            )
        }
    }
}
