package parker.core.runtime

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFailsWith
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.GroundedProposition
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.GroundedReviewReason
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.RelationshipEndpoint

class GroundedReplyValidatorTest {

    private val bytes = "governed source".toByteArray()
    private val evidenceId = EvidenceArtifactId("artifact-r1-current")
    private val evidence = ReasoningContextEntry.GovernedEvidence(
        text = "governed source",
        evidenceArtifactId = evidenceId,
        sourceSha256 = sha256(bytes),
    )
    private val evidenceContext = ReasoningContext.fromTypedEntries(listOf(evidence))

    @Test
    fun `current resolved evidence support passes`() {
        val reply = reply(GroundedProposition(
            text = "the source says governed source",
            classification = GroundedPropositionClassification.SUPPORTED_FACT,
            supportReferences = listOf(evidence),
        ), evidenceContext)

        assertIs<GroundedReplyValidationOutcome.Valid>(validate(reply, evidenceContext, foundEvidence()))
    }

    @Test
    fun `current resolved knowledge support passes`() {
        val provenanceId = ProvenanceId("provenance-r1")
        val reference = MemoryCoreRecordReference.ToEntity(EntityId("entity-r1"))
        val entry = ReasoningContextEntry.GovernedKnowledge(
            text = "known entity",
            knowledgeId = KnowledgeId("knowledge-r1"),
            evidenceReference = reference,
            provenanceReference = ProvenanceReference(provenanceId),
        )
        val context = ReasoningContext.fromTypedEntries(listOf(entry))
        val endpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-r1")
        val record = MemoryCoreRecord.OfEntity(
            Entity(EntityId("entity-r1"), "person", "A person", provenanceId, Instant.parse("2025-01-01T00:00:00Z")),
        )
        val reply = reply(GroundedProposition(
            text = "the entity is known",
            classification = GroundedPropositionClassification.SUPPORTED_FACT,
            supportReferences = listOf(entry),
        ), context)

        assertIs<GroundedReplyValidationOutcome.Valid>(
            validate(reply, context, emptyList(), listOf(endpoint to record)),
        )
    }

    @Test
    fun `fabricated knowledge support fails`() {
        val entry = ReasoningContextEntry.GovernedKnowledge(
            text = "fabricated knowledge",
            knowledgeId = KnowledgeId("knowledge-fabricated"),
            evidenceReference = MemoryCoreRecordReference.ToEntity(EntityId("entity-fabricated")),
            provenanceReference = ProvenanceReference(ProvenanceId("provenance-fabricated")),
        )
        val context = ReasoningContext.fromTypedEntries(listOf(entry))
        val result = GroundedReplyValidator.validate(
            reply(supported(entry), context),
            context,
            emptyList(),
            emptyList(),
        )

        assertEquals(
            GroundedReplyValidationFailure.UNRESOLVED_MEMORY_CORE_REFERENCE,
            assertIs<GroundedReplyValidationOutcome.Invalid>(result).reason,
        )
    }

    @Test
    fun `fabricated, foreign, and unresolved evidence references fail`() {
        val fabricated = evidence.copy(evidenceArtifactId = EvidenceArtifactId("artifact-fabricated"))
        val fabricatedContext = ReasoningContext.fromTypedEntries(listOf(fabricated))
        val fabricatedReply = reply(supported(fabricated), fabricatedContext)
        assertEquals(
            GroundedReplyValidationFailure.UNRESOLVED_EVIDENCE,
            assertIs<GroundedReplyValidationOutcome.Invalid>(validate(fabricatedReply, fabricatedContext, foundEvidence())).reason,
        )

        val foreignReply = reply(supported(evidence), evidenceContext)
        assertEquals(
            GroundedReplyValidationFailure.UNRESOLVED_EVIDENCE,
            assertIs<GroundedReplyValidationOutcome.Invalid>(
                validate(foreignReply, evidenceContext, listOf(EvidenceRetrievalResult.Found(EvidenceArtifactId("other"), bytes))),
            ).reason,
        )
    }

    @Test
    fun `inconsistent provenance fails closed`() {
        val mismatched = evidence.copy(sourceSha256 = "0".repeat(64))
        val context = ReasoningContext.fromTypedEntries(listOf(mismatched))
        val reply = reply(supported(mismatched), context)
        assertEquals(
            GroundedReplyValidationFailure.INCONSISTENT_EVIDENCE_PROVENANCE,
            assertIs<GroundedReplyValidationOutcome.Invalid>(validate(reply, context, foundEvidence())).reason,
        )
    }

    @Test
    fun `inference basis passes without becoming a supported fact`() {
        val proposition = GroundedProposition(
            text = "this may imply a conclusion",
            classification = GroundedPropositionClassification.INFERENCE,
            basisReferences = listOf(evidence),
        )
        val result = assertIs<GroundedReplyValidationOutcome.Valid>(validate(reply(proposition, evidenceContext), evidenceContext, foundEvidence()))
        assertEquals(GroundedPropositionClassification.INFERENCE, result.reply.propositions.single().classification)
    }

    @Test
    fun `not established has no support and conflict remains review required`() {
        val notEstablished = GroundedProposition("not established", GroundedPropositionClassification.NOT_ESTABLISHED)
        assertIs<GroundedReplyValidationOutcome.Valid>(validate(reply(notEstablished, evidenceContext), evidenceContext, foundEvidence()))

        val review = GroundedProposition(
            "conflicting dates",
            GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED,
            supportReferences = listOf(evidence),
            reviewReason = GroundedReviewReason.CONFLICT,
        )
        val validated = assertIs<GroundedReplyValidationOutcome.Valid>(validate(reply(review, evidenceContext), evidenceContext, foundEvidence()))
        assertEquals(GroundedReviewReason.CONFLICT, validated.reply.propositions.single().reviewReason)
    }

    @Test
    fun `malformed grounded output and invalid result cannot proceed as grounded delivery`() {
        val parser = GroundedReplyParser()
        assertFailsWith<IllegalArgumentException> {
            parser.parse("GROUNDED_REPLY_R0\nP\tSUPPORTED_FACT\tY2xhaW0=\t-\t-\t-", evidenceContext)
        }

        assertIs<GroundedReplyValidationOutcome.Invalid>(
            validate(reply(supported(evidence), evidenceContext), evidenceContext, emptyList()),
        )
    }

    private fun supported(entry: ReasoningContextEntry) = GroundedProposition(
        "supported claim",
        GroundedPropositionClassification.SUPPORTED_FACT,
        supportReferences = listOf(entry),
    )

    private fun reply(proposition: GroundedProposition, context: ReasoningContext) =
        GroundedReply.fromContext(context, listOf(proposition))

    private fun validate(
        reply: GroundedReply,
        context: ReasoningContext,
        evidenceResults: List<EvidenceRetrievalResult>,
        memoryResults: List<Pair<RelationshipEndpoint, MemoryCoreRecord?>> = emptyList(),
    ) = GroundedReplyValidator.validate(reply, context, evidenceResults, memoryResults)

    private fun foundEvidence() = listOf(EvidenceRetrievalResult.Found(evidenceId, bytes))

    private companion object {
        fun sha256(value: ByteArray): String = java.security.MessageDigest.getInstance("SHA-256")
            .digest(value).joinToString("") { "%02x".format(it) }
    }
}
