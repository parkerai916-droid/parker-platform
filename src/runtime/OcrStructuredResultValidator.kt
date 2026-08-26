package parker.core.runtime

import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.OcrPageAccounting
import parker.core.interfaces.OcrPageOutcome
import parker.core.interfaces.OcrPageOutcomeKind
import parker.core.interfaces.OcrPageOutcomeReason
import parker.core.interfaces.OcrPageScope
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionResult
import parker.core.interfaces.OcrRecognitionSegment
import parker.core.interfaces.OcrStructuredPageCandidate
import parker.core.interfaces.OcrStructuredTranscriptionCandidate
import parker.core.interfaces.OcrStructuredValidationOutcome

/**
 * Pure deterministic reconciliation of provider-neutral structured transcription facts.
 * Structural validity and complete page accounting never establish source fidelity or truth.
 * This class has no custodian, permission, network, filesystem, provider, UI, store, Memory,
 * Knowledge, or analysis dependency.
 */
class OcrStructuredResultValidator {
    fun validate(candidate: OcrStructuredTranscriptionCandidate): OcrStructuredValidationOutcome {
        val requested = candidate.requestedPageScope.pageNumbers.toSet()
        val submitted = candidate.submittedPageScope.pageNumbers.toSet()
        if (requested.isEmpty()) return rejected("Requested page scope is empty and cannot be reconciled")
        if (!requested.containsAll(submitted)) return rejected("Submitted page scope contains a page outside requested scope")
        if (candidate.processingProvenance.requestedPageScope != candidate.requestedPageScope) {
            return rejected("Processing provenance requested page scope contradicts the candidate")
        }
        if (candidate.processingProvenance.submittedPageScope != candidate.submittedPageScope) {
            return rejected("Processing provenance submitted page scope contradicts the candidate")
        }

        val duplicates = candidate.pages.groupingBy { it.pageNumber }.eachCount().filterValues { it > 1 }.keys
        if (duplicates.isNotEmpty()) return rejected("Duplicate page outcomes are not permitted: ${duplicates.sorted()}")

        for (page in candidate.pages) {
            if (page.pageNumber !in requested) return rejected("Returned page ${page.pageNumber} is outside requested scope")
            validatePage(page, submitted)?.let { return rejected(it) }
        }

        val explicitByPage = candidate.pages.associateBy { it.pageNumber }
        val finalPages = candidate.requestedPageScope.pageNumbers.map { pageNumber ->
            explicitByPage[pageNumber] ?: OcrStructuredPageCandidate(
                pageNumber = pageNumber,
                text = null,
                outcome = OcrPageOutcomeKind.NOT_RETURNED,
                reason = OcrPageOutcomeReason(
                    classification = "VALIDATOR_NOT_RETURNED",
                    detail = "No page-associated outcome was returned for the requested page",
                ),
            )
        }

        val derivedReturnedScope = OcrPageScope(
            candidate.pages
                .filter { it.outcome != OcrPageOutcomeKind.NOT_RETURNED }
                .map { it.pageNumber },
        )
        if (derivedReturnedScope != candidate.declaredReturnedPageScope) {
            return rejected("Declared returned page scope contradicts actual returned page outcomes")
        }

        for (page in finalPages) {
            validateUncertainty(page)?.let { return rejected(it) }
        }

        val pageAccounting = OcrPageAccounting(
            requestedScope = candidate.requestedPageScope,
            submittedScope = candidate.submittedPageScope,
            returnedScope = derivedReturnedScope,
            pageOutcomes = finalPages.map { page ->
                OcrPageOutcome(page.pageNumber, page.outcome, page.reason, page.warnings, page.uncertaintySpans)
            },
        )
        val completeness = deriveCompleteness(finalPages)
        val usablePages = finalPages.filter { !it.text.isNullOrBlank() }
        val outcome = deriveOutcome(candidate, finalPages, usablePages, pageAccounting)
        return OcrStructuredValidationOutcome.Validated(outcome, completeness, pageAccounting)
    }

    private fun validatePage(page: OcrStructuredPageCandidate, submitted: Set<Int>): String? = when (page.outcome) {
        OcrPageOutcomeKind.TRANSCRIBED -> when {
            page.pageNumber !in submitted -> "Page ${page.pageNumber} is transcribed but was not submitted"
            page.text == null -> "Page ${page.pageNumber} is transcribed without page-associated text"
            page.reason != null || page.warnings.isNotEmpty() || page.uncertaintySpans.isNotEmpty() ->
                "Clean transcribed page ${page.pageNumber} carries a qualification"
            else -> null
        }
        OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS -> when {
            page.pageNumber !in submitted -> "Page ${page.pageNumber} is qualified-transcribed but was not submitted"
            page.text == null -> "Qualified-transcribed page ${page.pageNumber} has no page-associated text"
            page.reason == null && page.warnings.isEmpty() && page.uncertaintySpans.isEmpty() ->
                "Qualified-transcribed page ${page.pageNumber} carries no actual qualification"
            else -> null
        }
        OcrPageOutcomeKind.ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT -> when {
            page.pageNumber !in submitted -> "Illegible page ${page.pageNumber} was not submitted"
            page.text != null -> "Illegible/no-content page ${page.pageNumber} must not carry usable text"
            page.reason == null && page.warnings.isEmpty() -> "Illegible/no-content page ${page.pageNumber} requires a qualification"
            else -> null
        }
        OcrPageOutcomeKind.FAILED -> when {
            page.pageNumber !in submitted -> "Failed page ${page.pageNumber} was not submitted"
            page.text != null -> "Failed page ${page.pageNumber} must not carry usable text"
            page.reason == null -> "Failed page ${page.pageNumber} requires a bounded reason"
            else -> null
        }
        OcrPageOutcomeKind.NOT_RETURNED -> when {
            page.text != null -> "Not-returned page ${page.pageNumber} must not carry usable text"
            page.reason == null -> "Explicit not-returned page ${page.pageNumber} requires a bounded reason"
            else -> null
        }
    }

    private fun validateUncertainty(page: OcrStructuredPageCandidate): String? {
        if (page.uncertaintySpans.isEmpty()) return null
        if (page.outcome == OcrPageOutcomeKind.FAILED || page.outcome == OcrPageOutcomeKind.NOT_RETURNED) {
            return "Page ${page.pageNumber} carries uncertainty despite outcome ${page.outcome}"
        }
        val text = page.text ?: return "Page ${page.pageNumber} carries uncertainty without returned text"
        page.uncertaintySpans.forEach { span ->
            if (span.pageNumber != page.pageNumber) return "Uncertainty span references a different page"
            if (span.endOffsetExclusive > text.length) return "Uncertainty span extends beyond page ${page.pageNumber} text"
        }
        // Overlap is deliberately allowed: independent disclosures may qualify the same characters.
        return null
    }

    private fun deriveCompleteness(pages: List<OcrStructuredPageCandidate>): DerivativeCompletenessState = when {
        pages.any { it.outcome == OcrPageOutcomeKind.FAILED || it.outcome == OcrPageOutcomeKind.NOT_RETURNED } ->
            DerivativeCompletenessState.KNOWN_INCOMPLETE
        pages.all { it.outcome == OcrPageOutcomeKind.TRANSCRIBED } -> DerivativeCompletenessState.ACCOUNTED_FOR
        else -> DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS
    }

    private fun deriveOutcome(
        candidate: OcrStructuredTranscriptionCandidate,
        pages: List<OcrStructuredPageCandidate>,
        usablePages: List<OcrStructuredPageCandidate>,
        pageAccounting: OcrPageAccounting,
    ): OcrRecognitionOutcome {
        if (usablePages.isEmpty()) {
            return if (pages.all { it.outcome == OcrPageOutcomeKind.ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT }) {
                OcrRecognitionOutcome.NoRecognisableContent("Every requested page was accounted for but contained no recognisable content")
            } else {
                OcrRecognitionOutcome.ProcessingOrDependencyFailure("No usable transcription text was returned; one or more pages failed or were not returned")
            }
        }

        val recognisedText = usablePages.joinToString("\n") { it.text!! }
        val result = OcrRecognitionResult(
            recognisedText = recognisedText,
            fidelity = candidate.fidelity,
            identity = candidate.recognitionIdentity,
            recognisedAt = candidate.recognisedAt,
            warnings = candidate.warnings + pages.flatMap { it.warnings },
            segments = usablePages.map { page -> OcrRecognitionSegment(page.text!!, candidate.fidelity, page.pageNumber) },
            pageAccounting = pageAccounting,
            processingProvenance = candidate.processingProvenance,
            providerProvenance = candidate.providerProvenance,
        )
        return if (pages.all { it.outcome == OcrPageOutcomeKind.TRANSCRIBED }) {
            OcrRecognitionOutcome.Recognised(result)
        } else {
            val degraded = pages.filter { it.outcome != OcrPageOutcomeKind.TRANSCRIBED }
                .joinToString(", ") { "page ${it.pageNumber}=${it.outcome.name}" }
            OcrRecognitionOutcome.PartialOrDegradedOutput(result, "Structured transcription is partial or degraded: $degraded")
        }
    }

    private fun rejected(reason: String) = OcrStructuredValidationOutcome.Rejected(
        OcrRecognitionOutcome.ValidationRejection(reason),
    )
}
