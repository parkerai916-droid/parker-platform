package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.GroundedProposition
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.RelationshipEndpoint

/** Deterministic reasons for rejecting a structured grounded reply. */
enum class GroundedReplyValidationFailure {
    UNSUPPLIED_CONTEXT_REFERENCE,
    UNRESOLVED_EVIDENCE,
    UNRESOLVED_MEMORY_CORE_REFERENCE,
    INCONSISTENT_EVIDENCE_PROVENANCE,
    INCONSISTENT_MEMORY_PROVENANCE,
    MALFORMED_PROPOSITION,
}

/** Non-durable result of validating one GroundedReply against one invocation. */
sealed interface GroundedReplyValidationOutcome {
    data class Valid(val reply: GroundedReply) : GroundedReplyValidationOutcome
    data class Invalid(val reason: GroundedReplyValidationFailure) : GroundedReplyValidationOutcome
}

/**
 * R1 structural/provenance validator for [GroundedReply].
 *
 * This object performs no semantic comparison between proposition text and
 * source content. It only checks that references are governed, belong to the
 * exact supplied context, resolved during the current invocation, and remain
 * consistent with provenance facts that are available. It has no state and
 * returns no persistent or authoritative governance result.
 */
internal object GroundedReplyValidator {

    fun validate(
        reply: GroundedReply,
        context: ReasoningContext,
        evidenceResults: List<EvidenceRetrievalResult>,
        memoryCoreResults: List<Pair<RelationshipEndpoint, MemoryCoreRecord?>>,
    ): GroundedReplyValidationOutcome {
        val supplied = context.suppliedGovernedEntries.toSet()
        val resolvedEvidence = evidenceResults
            .filterIsInstance<EvidenceRetrievalResult.Found>()
            .associateBy { it.evidenceArtifactId }
        val resolvedMemory = memoryCoreResults
            .filter { (_, record) -> record != null }
            .associate { (endpoint, record) -> endpoint to requireNotNull(record) }

        reply.propositions.forEach { proposition ->
            val structuralFailure = validatePropositionShape(proposition)
            if (structuralFailure != null) return structuralFailure

            (proposition.supportReferences + proposition.basisReferences).forEach { reference ->
                if (reference !in supplied) return GroundedReplyValidationOutcome.Invalid(
                    GroundedReplyValidationFailure.UNSUPPLIED_CONTEXT_REFERENCE,
                )
                when (val failure = validateReference(reference, resolvedEvidence, resolvedMemory)) {
                    null -> Unit
                    else -> return failure
                }
            }
        }

        return GroundedReplyValidationOutcome.Valid(reply)
    }

    private fun validatePropositionShape(proposition: GroundedProposition): GroundedReplyValidationOutcome.Invalid? = try {
        // GroundedProposition's constructor enforces these invariants. This
        // explicit check protects this boundary if a malformed instance is
        // ever obtained through deserialisation or reflective construction.
        require(proposition.text.isNotBlank())
        require(proposition.supportReferences.none { it is ReasoningContextEntry.PlainText })
        require(proposition.basisReferences.none { it is ReasoningContextEntry.PlainText })
        when (proposition.classification) {
            GroundedPropositionClassification.SUPPORTED_FACT -> require(proposition.supportReferences.isNotEmpty())
            GroundedPropositionClassification.INFERENCE -> {
                require(proposition.supportReferences.isEmpty())
                require(proposition.basisReferences.isNotEmpty())
            }
            GroundedPropositionClassification.NOT_ESTABLISHED ->
                require(proposition.supportReferences.isEmpty() && proposition.basisReferences.isEmpty())
            GroundedPropositionClassification.HUMAN_REVIEW_REQUIRED -> require(proposition.reviewReason != null)
        }
        null
    } catch (_: Exception) {
        GroundedReplyValidationOutcome.Invalid(GroundedReplyValidationFailure.MALFORMED_PROPOSITION)
    }

    private fun validateReference(
        reference: ReasoningContextEntry,
        resolvedEvidence: Map<parker.core.interfaces.EvidenceArtifactId, EvidenceRetrievalResult.Found>,
        resolvedMemory: Map<RelationshipEndpoint, MemoryCoreRecord>,
    ): GroundedReplyValidationOutcome.Invalid? {
        return when (reference) {
            is ReasoningContextEntry.GovernedEvidence -> {
                val found = resolvedEvidence[reference.evidenceArtifactId]
                if (found == null) {
                    GroundedReplyValidationOutcome.Invalid(GroundedReplyValidationFailure.UNRESOLVED_EVIDENCE)
                } else {
                    val expectedHashes = listOfNotNull(
                        reference.sourceSha256,
                        reference.assurance?.sourceSha256,
                    ).distinct()
                    if (expectedHashes.any { it != sha256(found.content) }) {
                        GroundedReplyValidationOutcome.Invalid(GroundedReplyValidationFailure.INCONSISTENT_EVIDENCE_PROVENANCE)
                    } else null
                }
            }
            is ReasoningContextEntry.GovernedMemoryCore -> {
                if (reference.reference !in resolvedMemory) {
                    GroundedReplyValidationOutcome.Invalid(GroundedReplyValidationFailure.UNRESOLVED_MEMORY_CORE_REFERENCE)
                } else null
            }
            is ReasoningContextEntry.GovernedKnowledge -> {
                val evidenceReference = reference.evidenceReference
                if (evidenceReference == null || reference.provenanceReference == null) {
                    GroundedReplyValidationOutcome.Invalid(GroundedReplyValidationFailure.UNRESOLVED_MEMORY_CORE_REFERENCE)
                } else {
                    val endpoint = endpointFor(evidenceReference)
                    val record = resolvedMemory[endpoint]
                    if (record == null) {
                        GroundedReplyValidationOutcome.Invalid(GroundedReplyValidationFailure.UNRESOLVED_MEMORY_CORE_REFERENCE)
                    } else {
                        if (reference.provenanceReference.provenanceId != provenanceIdOf(record)) {
                            GroundedReplyValidationOutcome.Invalid(GroundedReplyValidationFailure.INCONSISTENT_MEMORY_PROVENANCE)
                        } else null
                    }
                }
            }
            is ReasoningContextEntry.GovernedConflict -> null
            is ReasoningContextEntry.PlainText ->
                GroundedReplyValidationOutcome.Invalid(GroundedReplyValidationFailure.UNSUPPLIED_CONTEXT_REFERENCE)
        }
    }

    private fun endpointFor(reference: MemoryCoreRecordReference): RelationshipEndpoint = when (reference) {
        is MemoryCoreRecordReference.ToEntity -> RelationshipEndpoint(RelationshipEndpoint.ENTITY, reference.entityId.value)
        is MemoryCoreRecordReference.ToDocument -> RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, reference.documentId.value)
        is MemoryCoreRecordReference.ToAssertion -> RelationshipEndpoint(RelationshipEndpoint.ASSERTION, reference.assertionId.value)
        is MemoryCoreRecordReference.ToRelationship -> RelationshipEndpoint(RelationshipEndpoint.RELATIONSHIP, reference.relationshipId.value)
    }

    private fun provenanceIdOf(record: MemoryCoreRecord) = when (record) {
        is MemoryCoreRecord.OfEntity -> record.entity.provenanceId
        is MemoryCoreRecord.OfDocument -> record.document.provenanceId
        is MemoryCoreRecord.OfAssertion -> record.assertion.provenanceId
        is MemoryCoreRecord.OfRelationship -> record.relationship.provenanceId
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
