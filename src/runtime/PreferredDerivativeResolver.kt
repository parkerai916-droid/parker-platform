package parker.core.runtime

import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.DerivativeOperationalOutcome
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.StructuredDocumentKind
import parker.core.interfaces.OcrAuthorityClassification

sealed interface PreferredDerivativeResolution {
    val evidenceArtifactId: EvidenceArtifactId

    data class Preferred(
        override val evidenceArtifactId: EvidenceArtifactId,
        val derivative: DerivativeCandidateSummary,
        val reason: String,
    ) : PreferredDerivativeResolution

    data class Ambiguous(
        override val evidenceArtifactId: EvidenceArtifactId,
        val candidates: List<DerivativeCandidateSummary>,
        val reason: String,
    ) : PreferredDerivativeResolution

    data class NoUsableDerivative(
        override val evidenceArtifactId: EvidenceArtifactId,
        val reason: String,
    ) : PreferredDerivativeResolution
}

/** Read-only, conservative selection over the complete RA-3A candidate projection. */
class PreferredDerivativeResolver(
    private val discovery: DerivativeGenerationDiscoveryProjection,
) {
    suspend fun resolve(evidenceArtifactId: EvidenceArtifactId): PreferredDerivativeResolution =
        resolve(evidenceArtifactId, discovery.discover(evidenceArtifactId))

    fun resolve(
        evidenceArtifactId: EvidenceArtifactId,
        candidates: List<DerivativeCandidateSummary>,
    ): PreferredDerivativeResolution {
        val exact = candidates.filter { it.rootSourceEvidenceArtifactId == evidenceArtifactId }
        val eligible = exact.filter { candidate ->
            candidate.contentAvailable &&
                candidate.operationalOutcome == DerivativeOperationalOutcome.USABLE &&
                candidate.derivativeKind in GENERAL_DOCUMENT_KINDS &&
                candidate.completenessState in COMPARABLE_COMPLETENESS
        }
        if (eligible.isEmpty()) return PreferredDerivativeResolution.NoUsableDerivative(
            evidenceArtifactId, "No available, usable, general document-level governed derivative exists.",
        )

        // Collapse only candidates whose complete equivalence key was established from the
        // durable payload. Missing keys remain one-candidate classes, preserving fail-closed
        // ambiguity for legacy or incomplete provenance.
        // External OCR is the governed authority for OCR text.  Local OCR remains useful
        // history/diagnostic material, but must not compete with an admitted external result.
        // The two producers use different derivative-kind labels, so normalize both to one
        // representation family before applying the ordinary deterministic selection rules.
        val ocrCandidates = eligible.filter { it.derivativeKind in OCR_DOCUMENT_KINDS }
        val authoritativeOcr = ocrCandidates.filter { it.authority == OcrAuthorityClassification.EXTERNAL_AUTHORITATIVE }
        val authorityFiltered = if (authoritativeOcr.isNotEmpty()) {
            eligible.filterNot { it.derivativeKind in OCR_DOCUMENT_KINDS } + authoritativeOcr
        } else eligible
        val logicalCandidates = authorityFiltered
            .groupBy { it.equivalenceKey ?: "generation:${it.derivativeGenerationId.value}" }
            .values
            .map { equivalent -> equivalent.minWith(compareBy({ it.generatedAt }, { it.derivativeGenerationId.value })) }
        val representationKinds = logicalCandidates.groupBy { representationFamily(it) }
        if (representationKinds.size > 1) {
            return PreferredDerivativeResolution.Ambiguous(
                evidenceArtifactId, eligible,
                "Multiple complete or partial document representation kinds are available and no authoritative cross-kind preference is defined.",
            )
        }

        val sameKind = representationKinds.values.single()
        val complete = sameKind.filter { it.completenessState == DerivativeCompletenessState.ACCOUNTED_FOR }
        val selected = when {
            complete.size == 1 -> complete.single()
            complete.size > 1 -> return PreferredDerivativeResolution.Ambiguous(
                evidenceArtifactId, complete,
                "Multiple complete governed representations of the same kind are available and no authoritative producer preference is defined.",
            )
            sameKind.size == 1 -> sameKind.single()
            else -> return PreferredDerivativeResolution.Ambiguous(
                evidenceArtifactId, sameKind,
                "Multiple governed representations of the same kind are available without a uniquely preferred completeness state.",
            )
        }
        val reason = if (sameKind.size == 1) {
            "Only available, usable general document-level governed derivative."
        } else {
            "Complete governed representation preferred over qualified/incomplete representations of the same kind."
        }
        return PreferredDerivativeResolution.Preferred(evidenceArtifactId, selected, reason)
    }

    private companion object {
        // These are the current document-level payload kinds emitted by DerivativeGenerationCoordinator.
        // Region transcriptions and verification receipts are intentionally excluded as overlays or
        // specialized representations, not discarded from discovery.
        val OCR_DOCUMENT_KINDS = setOf("OCR recognised text", "External transcription recognised text")
        val GENERAL_DOCUMENT_KINDS = setOf(
            "PDF structure", "Searchable PDF literal text", "CSV structure", "EML MIME structure",
            "OCR recognised text", "External transcription recognised text",
        ) + StructuredDocumentKind.entries.mapTo(linkedSetOf()) { "${it.name} structured representation" } + setOf("DOCX OOXML structure")
        val COMPARABLE_COMPLETENESS = setOf(
            DerivativeCompletenessState.ACCOUNTED_FOR,
            DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS,
            DerivativeCompletenessState.KNOWN_INCOMPLETE,
        )

        fun representationFamily(candidate: DerivativeCandidateSummary): String =
            if (candidate.derivativeKind in OCR_DOCUMENT_KINDS) "OCR recognised text" else candidate.derivativeKind
    }
}
