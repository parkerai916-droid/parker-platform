package parker.core.runtime

import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.GroundedProposition
import parker.core.interfaces.GroundedPropositionClassification
import parker.core.interfaces.GroundedReply
import parker.core.interfaces.GroundedReviewReason
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningContextEntry
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.SourceRegionId
import parker.core.interfaces.KnownConflictResolutionState
import parker.core.interfaces.KnownConflictType

/** Report-only indication for fallback observability not present in R0. */
enum class GroundingReportFallbackObservation {
    NOT_REPRESENTED,
}

/** Whether the non-durable grounding validation accepted the reply. */
data class GroundingReportValidation(
    val valid: Boolean,
    val failure: GroundedReplyValidationFailure? = null,
) {
    init {
        require(valid == (failure == null)) {
            "valid reports must not carry a failure and invalid reports must carry one"
        }
    }
}

/** Explicit report-level distinction between absent and inconsistent provenance. */
enum class GroundingReportProvenanceStatus {
    AVAILABLE,
    UNAVAILABLE,
    INCONSISTENT,
}

/** Existing governed identity/provenance projected for inspection. */
data class GroundingReportProvenance(
    val status: GroundingReportProvenanceStatus,
    val evidenceArtifactId: EvidenceArtifactId? = null,
    val sourceSha256: String? = null,
    val derivativeGenerationId: DerivativeGenerationId? = null,
    val pageNumber: Int? = null,
    val sourceRegionId: SourceRegionId? = null,
    val knowledgeId: KnowledgeId? = null,
    val knowledgeProvenanceId: ProvenanceId? = null,
    val memoryCoreReference: RelationshipEndpoint? = null,
)

/** One exact entry in the immutable context supplied to cognition. */
data class GroundingReportContextEntry(
    val position: Int,
    val entry: ReasoningContextEntry,
    val provenance: GroundingReportProvenance,
)

/** A structural mapping from a proposition reference to its supplied entry. */
data class GroundingReportReferenceMapping(
    val reference: ReasoningContextEntry,
    val suppliedContextEntry: GroundingReportContextEntry?,
)

/** Report-only view of the exact semantic-support check for one proposition. */
data class GroundingReportSemanticSupport(
    val status: ExactStructuredClaimSupportStatus,
    val supportClass: String? = null,
    val checkedValue: String? = null,
    val failure: ExactStructuredClaimSupportFailure? = null,
)

/** Report-only outcome of the deterministic exact-support stage. */
data class GroundingReportSemanticValidation(
    val status: ExactStructuredClaimSupportStatus,
    val failure: ExactStructuredClaimSupportFailure? = null,
)

data class GroundingReportConflict(
    val conflictId: String,
    val conflictType: KnownConflictType,
    val resolutionState: KnownConflictResolutionState,
    val participants: List<RelationshipEndpoint>,
    val disclosedByReply: Boolean,
)

data class GroundingReportConflictEnforcement(
    val valid: Boolean,
    val failure: KnownConflictEnforcementFailure? = null,
)

/** One proposition and its report-only support/basis mappings. */
data class GroundingReportProposition(
    val text: String,
    val classification: GroundedPropositionClassification,
    val supportMappings: List<GroundingReportReferenceMapping>,
    val inferenceMappings: List<GroundingReportReferenceMapping>,
    val reviewReason: GroundedReviewReason?,
    val semanticSupport: GroundingReportSemanticSupport,
)

/**
 * Complete, read-only projection of one strict-evidence reasoning invocation.
 * It contains no newly assigned identity and is never persisted.
 */
data class GroundingReport(
    val request: EvidenceAnalysisRequest,
    val requestedEvidence: List<EvidenceArtifactId>,
    val resolvedEvidence: List<EvidenceArtifactId>,
    val requestedMemoryCore: List<RelationshipEndpoint>,
    val resolvedMemoryCore: List<RelationshipEndpoint>,
    val suppliedContext: List<GroundingReportContextEntry>,
    val plainTextContext: List<GroundingReportContextEntry>,
    val propositions: List<GroundingReportProposition>,
    val validation: GroundingReportValidation,
    val semanticValidation: GroundingReportSemanticValidation,
    val knownConflicts: List<GroundingReportConflict> = emptyList(),
    val conflictEnforcement: GroundingReportConflictEnforcement = GroundingReportConflictEnforcement(true),
    val semanticFallback: GroundingReportFallbackObservation = GroundingReportFallbackObservation.NOT_REPRESENTED,
)

/** Stateless projection seam for inspecting one invocation's grounding path. */
internal object GroundingReportProjection {

    fun project(
        request: EvidenceAnalysisRequest,
        reply: GroundedReply,
        context: ReasoningContext,
        evidenceResults: List<EvidenceRetrievalResult>,
        memoryCoreResults: List<Pair<RelationshipEndpoint, MemoryCoreRecord?>>,
        semanticSupport: ExactStructuredClaimSupportValidationOutcome? = null,
        knownConflicts: List<KnownConflictProjection> = emptyList(),
        conflictEnforcement: KnownConflictEnforcementOutcome = KnownConflictEnforcementOutcome.Valid,
    ): GroundingReport {
        val validationOutcome = GroundedReplyValidator.validate(reply, context, evidenceResults, memoryCoreResults)
        val failure = (validationOutcome as? GroundedReplyValidationOutcome.Invalid)?.reason
        val reportEntries = context.typedEntries.mapIndexed { position, entry ->
            GroundingReportContextEntry(position, entry, provenance(entry, memoryCoreResults, failure))
        }
        val suppliedByEntry = reportEntries.filter { it.entry !is ReasoningContextEntry.PlainText }
        val byReference = suppliedByEntry.associateBy { it.entry }
        val semanticChecks = semanticSupport?.checks?.associateBy { it.propositionIndex }.orEmpty()

        return GroundingReport(
            request = request,
            requestedEvidence = request.evidenceArtifactIds.toList(),
            resolvedEvidence = evidenceResults.filterIsInstance<EvidenceRetrievalResult.Found>()
                .map { it.evidenceArtifactId },
            requestedMemoryCore = request.memoryCoreReferences.toList(),
            resolvedMemoryCore = memoryCoreResults.filter { it.second != null }.map { it.first },
            suppliedContext = suppliedByEntry,
            plainTextContext = reportEntries.filter { it.entry is ReasoningContextEntry.PlainText },
            propositions = reply.propositions.mapIndexed { propositionIndex, proposition ->
                GroundingReportProposition(
                    text = proposition.text,
                    classification = proposition.classification,
                    supportMappings = mappings(proposition.supportReferences, byReference),
                    inferenceMappings = mappings(proposition.basisReferences, byReference),
                    reviewReason = proposition.reviewReason,
                    semanticSupport = semanticChecks[propositionIndex]?.let {
                        GroundingReportSemanticSupport(it.status, it.supportClass, it.checkedValue, it.failure)
                    } ?: GroundingReportSemanticSupport(ExactStructuredClaimSupportStatus.NOT_APPLICABLE),
                )
            },
            validation = GroundingReportValidation(failure == null, failure),
            semanticValidation = semanticValidation(semanticSupport),
            knownConflicts = knownConflicts.map { conflict ->
                GroundingReportConflict(
                    conflictId = conflict.conflictId,
                    conflictType = conflict.conflictType,
                    resolutionState = conflict.resolutionState,
                    participants = conflict.participants,
                    disclosedByReply = KnownConflictEnforcer.disclosed(reply),
                )
            },
            conflictEnforcement = when (conflictEnforcement) {
                KnownConflictEnforcementOutcome.Valid -> GroundingReportConflictEnforcement(true)
                is KnownConflictEnforcementOutcome.Invalid -> GroundingReportConflictEnforcement(false, conflictEnforcement.reason)
            },
        )
    }

    private fun semanticValidation(
        outcome: ExactStructuredClaimSupportValidationOutcome?,
    ): GroundingReportSemanticValidation = when (outcome) {
        null -> GroundingReportSemanticValidation(ExactStructuredClaimSupportStatus.NOT_APPLICABLE)
        is ExactStructuredClaimSupportValidationOutcome.ExactlySupported ->
            GroundingReportSemanticValidation(ExactStructuredClaimSupportStatus.EXACTLY_SUPPORTED)
        is ExactStructuredClaimSupportValidationOutcome.HumanReviewRequired ->
            GroundingReportSemanticValidation(ExactStructuredClaimSupportStatus.HUMAN_REVIEW_REQUIRED, outcome.reason)
        is ExactStructuredClaimSupportValidationOutcome.Invalid ->
            GroundingReportSemanticValidation(ExactStructuredClaimSupportStatus.INVALID, outcome.reason)
        is ExactStructuredClaimSupportValidationOutcome.NotApplicable ->
            GroundingReportSemanticValidation(ExactStructuredClaimSupportStatus.NOT_APPLICABLE)
    }

    private fun mappings(
        references: List<ReasoningContextEntry>,
        byReference: Map<ReasoningContextEntry, GroundingReportContextEntry>,
    ) = references.map { reference -> GroundingReportReferenceMapping(reference, byReference[reference]) }

    private fun provenance(
        entry: ReasoningContextEntry,
        memoryCoreResults: List<Pair<RelationshipEndpoint, MemoryCoreRecord? >>,
        failure: GroundedReplyValidationFailure?,
    ): GroundingReportProvenance = when (entry) {
        is ReasoningContextEntry.PlainText -> GroundingReportProvenance(GroundingReportProvenanceStatus.UNAVAILABLE)
        is ReasoningContextEntry.GovernedEvidence -> {
            val inconsistent = failure == GroundedReplyValidationFailure.INCONSISTENT_EVIDENCE_PROVENANCE
            GroundingReportProvenance(
                status = if (inconsistent) GroundingReportProvenanceStatus.INCONSISTENT
                else if (entry.sourceSha256 == null && entry.assurance?.sourceSha256 == null && entry.derivativeGenerationId == null && entry.pageNumber == null && entry.sourceRegionId == null)
                    GroundingReportProvenanceStatus.UNAVAILABLE else GroundingReportProvenanceStatus.AVAILABLE,
                evidenceArtifactId = entry.evidenceArtifactId,
                sourceSha256 = entry.sourceSha256 ?: entry.assurance?.sourceSha256,
                derivativeGenerationId = entry.derivativeGenerationId,
                pageNumber = entry.pageNumber,
                sourceRegionId = entry.sourceRegionId,
            )
        }
        is ReasoningContextEntry.GovernedKnowledge -> {
            val endpoint = entry.evidenceReference?.let(::endpointFor)
            val inconsistent = failure == GroundedReplyValidationFailure.INCONSISTENT_MEMORY_PROVENANCE
            GroundingReportProvenance(
                status = if (inconsistent) GroundingReportProvenanceStatus.INCONSISTENT
                else if (entry.evidenceReference == null || entry.provenanceReference == null) GroundingReportProvenanceStatus.UNAVAILABLE
                else GroundingReportProvenanceStatus.AVAILABLE,
                knowledgeId = entry.knowledgeId,
                knowledgeProvenanceId = entry.provenanceReference?.provenanceId,
                memoryCoreReference = endpoint,
            )
        }
        is ReasoningContextEntry.GovernedMemoryCore -> GroundingReportProvenance(
            status = if (memoryCoreResults.any { it.first == entry.reference && it.second != null })
                GroundingReportProvenanceStatus.AVAILABLE else GroundingReportProvenanceStatus.UNAVAILABLE,
            memoryCoreReference = entry.reference,
        )
        is ReasoningContextEntry.GovernedConflict -> GroundingReportProvenance(
            status = GroundingReportProvenanceStatus.AVAILABLE,
            memoryCoreReference = RelationshipEndpoint(RelationshipEndpoint.RELATIONSHIP, entry.conflictId),
        )
    }

    private fun endpointFor(reference: MemoryCoreRecordReference): RelationshipEndpoint = when (reference) {
        is MemoryCoreRecordReference.ToEntity -> RelationshipEndpoint(RelationshipEndpoint.ENTITY, reference.entityId.value)
        is MemoryCoreRecordReference.ToDocument -> RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, reference.documentId.value)
        is MemoryCoreRecordReference.ToAssertion -> RelationshipEndpoint(RelationshipEndpoint.ASSERTION, reference.assertionId.value)
        is MemoryCoreRecordReference.ToRelationship -> RelationshipEndpoint(RelationshipEndpoint.RELATIONSHIP, reference.relationshipId.value)
    }
}
