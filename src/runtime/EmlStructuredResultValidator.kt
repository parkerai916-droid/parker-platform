package parker.core.runtime

import parker.core.interfaces.EmlDerivedRepresentation
import parker.core.interfaces.EmlMessageOutcomeKind
import parker.core.interfaces.EmlStructuredTranscriptionCandidate
import parker.core.interfaces.EmlStructuredValidationOutcome
import parker.core.interfaces.ExternalTranscriptionExecutionBinding
import parker.core.interfaces.OcrPageOutcomeKind

/**
 * Pure deterministic reconciliation of a non-paginated EML verification candidate against the
 * exact representation Parker submitted. No "best effort" acceptance: any contradiction, any
 * omitted required section, any invented section, or any duplicate identity is a hard rejection.
 * This class has no custodian, permission, network, filesystem, provider, UI, store, Memory,
 * Knowledge, or analysis dependency.
 */
class EmlStructuredResultValidator {
    fun validate(
        candidate: EmlStructuredTranscriptionCandidate,
        submittedRepresentation: EmlDerivedRepresentation,
        executionBinding: ExternalTranscriptionExecutionBinding,
    ): EmlStructuredValidationOutcome {
        val provenance = submittedRepresentation.provenance
        if (candidate.requestId != executionBinding.requestId) return rejected("Candidate request ID does not match the execution binding")
        if (candidate.attemptId != executionBinding.attemptId) return rejected("Candidate attempt ID does not match the execution binding")
        if (candidate.profileId != executionBinding.profileId) return rejected("Candidate profile ID does not match the execution binding")
        if (candidate.sourceEvidenceArtifactId != provenance.sourceEvidenceArtifactId) return rejected("Candidate source Evidence ID does not match the submitted representation")
        if (candidate.submittedRepresentationSha256 != provenance.representationSha256) return rejected("Candidate representation digest does not match the submitted representation")
        if (candidate.processingProfileIdentity != provenance.representationGenerationProfileIdentity) return rejected("Candidate processing profile identity does not match the submitted representation")

        if (candidate.sections.isEmpty()) return rejected("Malformed response: no sections were returned")
        val returnedIds = candidate.sections.map { it.mimeEntityId }
        if (returnedIds.size != returnedIds.toSet().size) return rejected("Duplicate section/entity identities are not permitted")
        val knownEntityIds = provenance.mimeEntityOrder.toSet()
        val unknownIds = returnedIds.toSet() - knownEntityIds
        if (unknownIds.isNotEmpty()) return rejected("Response references unknown/invented MIME entity IDs: ${unknownIds.sorted()}")
        val requiredIds = provenance.bodyAlternativesIncluded.toSet()
        val missingIds = requiredIds - returnedIds.toSet()
        if (missingIds.isNotEmpty()) return rejected("Response omits required MIME entities/sections: ${missingIds.sorted()}")

        for (section in candidate.sections) {
            validateSection(section.mimeEntityId, section.outcome, section.reason, section.warnings)?.let { return rejected(it) }
        }

        val outcomesById = candidate.sections.associate { it.mimeEntityId to it.outcome }
        val allRequiredTranscribed = requiredIds.all { id -> outcomesById[id] == OcrPageOutcomeKind.TRANSCRIBED }
        val anyFailedOrMissing = requiredIds.any { id ->
            val outcome = outcomesById[id]
            outcome == OcrPageOutcomeKind.FAILED || outcome == OcrPageOutcomeKind.NOT_RETURNED || outcome == OcrPageOutcomeKind.ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT
        }
        val contradictsOutcome = when (candidate.messageOutcome) {
            EmlMessageOutcomeKind.TRANSCRIBED -> !allRequiredTranscribed
            EmlMessageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS -> allRequiredTranscribed || anyFailedOrMissing
            EmlMessageOutcomeKind.FAILED -> !anyFailedOrMissing
        }
        if (contradictsOutcome) return rejected("Declared message outcome contradicts the actual per-section outcomes")

        return EmlStructuredValidationOutcome.Validated(
            messageOutcome = candidate.messageOutcome,
            completenessState = candidate.completenessState,
            verifiedSectionIds = returnedIds,
            warnings = candidate.warnings,
        )
    }

    private fun validateSection(
        mimeEntityId: String,
        outcome: OcrPageOutcomeKind,
        reason: parker.core.interfaces.OcrPageOutcomeReason?,
        warnings: List<String>,
    ): String? = when (outcome) {
        OcrPageOutcomeKind.TRANSCRIBED -> if (reason != null || warnings.isNotEmpty()) {
            "Clean transcribed section $mimeEntityId carries a qualification"
        } else null
        OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS -> if (reason == null && warnings.isEmpty()) {
            "Qualified-transcribed section $mimeEntityId carries no actual qualification"
        } else null
        OcrPageOutcomeKind.ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT -> if (reason == null && warnings.isEmpty()) {
            "Illegible/no-content section $mimeEntityId requires a qualification"
        } else null
        OcrPageOutcomeKind.FAILED -> if (reason == null) "Failed section $mimeEntityId requires a bounded reason" else null
        OcrPageOutcomeKind.NOT_RETURNED -> if (reason == null) "Not-returned section $mimeEntityId requires a bounded reason" else null
    }

    private fun rejected(reason: String) = EmlStructuredValidationOutcome.Rejected(reason)
}
