package parker.core.runtime

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue
import parker.core.interfaces.DerivativeCompletenessState
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrModelSnapshot
import parker.core.interfaces.OcrPageOutcomeKind
import parker.core.interfaces.OcrPageOutcomeReason
import parker.core.interfaces.OcrPageScope
import parker.core.interfaces.OcrProcessingProvenance
import parker.core.interfaces.OcrProviderProvenance
import parker.core.interfaces.OcrRecognitionIdentity
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrSha256Digest
import parker.core.interfaces.OcrStructuredPageCandidate
import parker.core.interfaces.OcrStructuredTranscriptionCandidate
import parker.core.interfaces.OcrStructuredValidationOutcome
import parker.core.interfaces.OcrUncertaintyKind
import parker.core.interfaces.OcrUncertaintySpan
import parker.core.interfaces.TranscriptionFidelity

class OcrStructuredResultValidatorTest {
    private val validator = OcrStructuredResultValidator()

    @Test
    fun `clean two-page result becomes Recognised and ACCOUNTED_FOR`() {
        val validated = validate(
            candidate(
                pageCandidates = listOf(cleanPage(1, "page one"), cleanPage(2, "page two")),
                declaredReturned = pages(1, 2),
            ),
        )

        assertEquals(DerivativeCompletenessState.ACCOUNTED_FOR, validated.completenessState)
        val outcome = assertIs<OcrRecognitionOutcome.Recognised>(validated.outcome)
        assertEquals("page one\npage two", outcome.result.recognisedText)
        assertEquals(listOf(1, 2), validated.pageAccounting.returnedScope.pageNumbers)
    }

    @Test
    fun `qualified page becomes partial and accounted with qualifications`() {
        val qualified = OcrStructuredPageCandidate(
            2,
            "page two uncertain",
            OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS,
            warnings = listOf("review page two"),
            uncertaintySpans = listOf(OcrUncertaintySpan(2, 9, 18, OcrUncertaintyKind.UNCERTAIN, "uncertain word")),
        )
        val validated = validate(candidate(listOf(cleanPage(1, "page one"), qualified), declaredReturned = pages(1, 2)))

        assertEquals(DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, validated.completenessState)
        val partial = assertIs<OcrRecognitionOutcome.PartialOrDegradedOutput>(validated.outcome)
        assertEquals(qualified.uncertaintySpans, partial.partialResult.pageAccounting?.pageOutcomes?.last()?.uncertaintySpans)
    }

    @Test
    fun `accounted illegible page becomes partial without being called incomplete`() {
        val illegible = OcrStructuredPageCandidate(
            2,
            null,
            OcrPageOutcomeKind.ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT,
            reason = OcrPageOutcomeReason("ILLEGIBLE", "No readable text"),
        )
        val validated = validate(candidate(listOf(cleanPage(1, "page one"), illegible), declaredReturned = pages(1, 2)))

        assertIs<OcrRecognitionOutcome.PartialOrDegradedOutput>(validated.outcome)
        assertEquals(DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, validated.completenessState)
    }

    @Test
    fun `failed page with usable text elsewhere becomes partial and known incomplete`() {
        val failed = OcrStructuredPageCandidate(2, null, OcrPageOutcomeKind.FAILED, OcrPageOutcomeReason("PROVIDER_FAILURE"))
        val validated = validate(candidate(listOf(cleanPage(1, "page one"), failed), declaredReturned = pages(1, 2)))

        assertIs<OcrRecognitionOutcome.PartialOrDegradedOutput>(validated.outcome)
        assertEquals(DerivativeCompletenessState.KNOWN_INCOMPLETE, validated.completenessState)
    }

    @Test
    fun `omitted requested page is synthesized as NOT_RETURNED and known incomplete`() {
        val validated = validate(candidate(listOf(cleanPage(1, "page one")), declaredReturned = pages(1)))

        assertIs<OcrRecognitionOutcome.PartialOrDegradedOutput>(validated.outcome)
        assertEquals(DerivativeCompletenessState.KNOWN_INCOMPLETE, validated.completenessState)
        val missing = validated.pageAccounting.pageOutcomes.single { it.pageNumber == 2 }
        assertEquals(OcrPageOutcomeKind.NOT_RETURNED, missing.outcome)
        assertEquals("VALIDATOR_NOT_RETURNED", missing.reason?.classification)
    }

    @Test
    fun `all pages truthfully illegible maps to NoRecognisableContent`() {
        val illegible = listOf(1, 2).map {
            OcrStructuredPageCandidate(it, null, OcrPageOutcomeKind.ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT, OcrPageOutcomeReason("NO_RECOGNISABLE_CONTENT"))
        }
        val validated = validate(candidate(illegible, declaredReturned = pages(1, 2)))

        assertIs<OcrRecognitionOutcome.NoRecognisableContent>(validated.outcome)
        assertEquals(DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, validated.completenessState)
    }

    @Test
    fun `no text with failed or not-returned page maps to processing failure not no-content`() {
        val failed = OcrStructuredPageCandidate(1, null, OcrPageOutcomeKind.FAILED, OcrPageOutcomeReason("PROVIDER_FAILURE"))
        val validated = validate(candidate(listOf(failed), declaredReturned = pages(1)))

        assertIs<OcrRecognitionOutcome.ProcessingOrDependencyFailure>(validated.outcome)
        assertEquals(DerivativeCompletenessState.KNOWN_INCOMPLETE, validated.completenessState)
    }

    @Test
    fun `duplicate page outcomes reject`() {
        assertRejected(candidate(listOf(cleanPage(1, "a"), cleanPage(1, "b")), declaredReturned = pages(1)))
    }

    @Test
    fun `page candidate construction rejects zero and negative pages`() {
        assertFailsWith<IllegalArgumentException> { cleanPage(0, "x") }
        assertFailsWith<IllegalArgumentException> { cleanPage(-1, "x") }
    }

    @Test
    fun `returned page outside requested scope rejects`() {
        assertRejected(candidate(listOf(cleanPage(1, "a"), cleanPage(3, "outside")), declaredReturned = pages(1, 3)))
    }

    @Test
    fun `transcribed page requires text and submission`() {
        assertRejected(candidate(listOf(OcrStructuredPageCandidate(1, null, OcrPageOutcomeKind.TRANSCRIBED)), declaredReturned = pages(1)))
        assertRejected(
            candidate(
                pageCandidates = listOf(cleanPage(2, "not submitted")),
                submitted = pages(1),
                declaredReturned = pages(2),
            ),
        )
    }

    @Test
    fun `clean page cannot carry uncertainty and qualified page requires a qualification`() {
        val span = OcrUncertaintySpan(1, 0, 1, OcrUncertaintyKind.UNCERTAIN, "uncertain")
        assertRejected(candidate(listOf(cleanPage(1, "a").copy(uncertaintySpans = listOf(span))), declaredReturned = pages(1)))
        assertRejected(
            candidate(
                listOf(OcrStructuredPageCandidate(1, "a", OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS)),
                declaredReturned = pages(1),
            ),
        )
    }

    @Test
    fun `failed and explicit not-returned pages require reasons`() {
        assertRejected(candidate(listOf(OcrStructuredPageCandidate(1, null, OcrPageOutcomeKind.FAILED)), declaredReturned = pages(1)))
        assertRejected(candidate(listOf(OcrStructuredPageCandidate(1, null, OcrPageOutcomeKind.NOT_RETURNED)), declaredReturned = OcrPageScope(emptyList())))
    }

    @Test
    fun `declared returned scope contradiction rejects`() {
        assertRejected(candidate(listOf(cleanPage(1, "a"), cleanPage(2, "b")), declaredReturned = pages(1)))
    }

    @Test
    fun `submitted page scope outside requested scope rejects`() {
        assertRejected(candidate(listOf(cleanPage(1, "a")), submitted = pages(1, 3), declaredReturned = pages(1)))
    }

    @Test
    fun `processing requested and submitted scope contradictions reject`() {
        val candidate = candidate(listOf(cleanPage(1, "a")), declaredReturned = pages(1))
        assertRejected(candidate.copy(processingProvenance = candidate.processingProvenance.copy(requestedPageScope = pages(1))))
        assertRejected(candidate.copy(processingProvenance = candidate.processingProvenance.copy(submittedPageScope = pages(1))))
    }

    @Test
    fun `uncertainty end beyond text and span on failed or not-returned page reject`() {
        val tooLong = OcrUncertaintySpan(1, 0, 5, OcrUncertaintyKind.UNCERTAIN, "too long")
        assertRejected(candidate(listOf(qualifiedPage(1, "abc", listOf(tooLong))), declaredReturned = pages(1)))

        val failedSpan = OcrUncertaintySpan(1, 0, 1, OcrUncertaintyKind.ILLEGIBLE, "not valid on failure")
        val failed = OcrStructuredPageCandidate(1, null, OcrPageOutcomeKind.FAILED, OcrPageOutcomeReason("FAILED"), uncertaintySpans = listOf(failedSpan))
        assertRejected(candidate(listOf(failed), declaredReturned = pages(1)))

        val notReturned = OcrStructuredPageCandidate(1, null, OcrPageOutcomeKind.NOT_RETURNED, OcrPageOutcomeReason("NOT_RETURNED"), uncertaintySpans = listOf(failedSpan))
        assertRejected(candidate(listOf(notReturned), declaredReturned = OcrPageScope(emptyList())))
    }

    @Test
    fun `overlapping uncertainty spans are allowed and preserved exactly`() {
        val spans = listOf(
            OcrUncertaintySpan(1, 0, 3, OcrUncertaintyKind.UNCERTAIN, "first"),
            OcrUncertaintySpan(1, 2, 5, OcrUncertaintyKind.ILLEGIBLE, "overlapping second"),
        )
        val validated = validate(candidate(listOf(qualifiedPage(1, "abcdef", spans)), declaredReturned = pages(1)))
        val partial = assertIs<OcrRecognitionOutcome.PartialOrDegradedOutput>(validated.outcome)

        assertEquals(spans, partial.partialResult.pageAccounting?.pageOutcomes?.single { it.pageNumber == 1 }?.uncertaintySpans)
    }

    @Test
    fun `clean accounting never promotes unverified fidelity to VERBATIM`() {
        val validated = validate(candidate(listOf(cleanPage(1, "a"), cleanPage(2, "b")), declaredReturned = pages(1, 2)))
        val result = assertIs<OcrRecognitionOutcome.Recognised>(validated.outcome).result

        assertEquals(TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, result.fidelity)
    }

    @Test
    fun `validator preserves every supplied governed fidelity without reinterpretation`() {
        TranscriptionFidelity.entries.forEach { fidelity ->
            val validated = validate(candidate(listOf(cleanPage(1, "a"), cleanPage(2, "b")), declaredReturned = pages(1, 2), fidelity = fidelity))
            assertEquals(fidelity, assertIs<OcrRecognitionOutcome.Recognised>(validated.outcome).result.fidelity)
        }
    }

    @Test
    fun `present and not-exposed provider snapshots are accepted and preserved`() {
        listOf<OcrModelSnapshot>(OcrModelSnapshot.Present("snapshot-1"), OcrModelSnapshot.NotExposed).forEach { snapshot ->
            val candidate = candidate(listOf(cleanPage(1, "a"), cleanPage(2, "b")), declaredReturned = pages(1, 2), snapshot = snapshot)
            val result = assertIs<OcrRecognitionOutcome.Recognised>(validate(candidate).outcome).result
            assertEquals(snapshot, result.providerProvenance?.modelSnapshot)
        }
    }

    @Test
    fun `provider contract rejects missing or fabricated model identity before validation`() {
        val valid = provider(OcrModelSnapshot.NotExposed)
        assertFailsWith<IllegalArgumentException> { valid.copy(providerReportedModelIdentifier = "") }
        assertFailsWith<IllegalArgumentException> { valid.copy(providerReportedModelIdentifier = "unknown") }
    }

    @Test
    fun `validator structure has no forbidden authority or side-effect dependency`() {
        val fields = OcrStructuredResultValidator::class.java.declaredFields.map { it.type.name }
        val constructors = OcrStructuredResultValidator::class.java.declaredConstructors.flatMap { it.parameterTypes.map(Class<*>::getName) }
        val forbidden = listOf("OpenAI", "HttpClient", "PermissionEngine", "EvidenceCustodian", "DerivativeContentStorage", "Memory", "Knowledge", "Analysis")
        (fields + constructors).forEach { type -> forbidden.forEach { assertTrue(!type.contains(it), "$type contains forbidden $it") } }
    }

    private fun validate(candidate: OcrStructuredTranscriptionCandidate): OcrStructuredValidationOutcome.Validated =
        assertIs(validator.validate(candidate))

    private fun assertRejected(candidate: OcrStructuredTranscriptionCandidate) {
        assertIs<OcrStructuredValidationOutcome.Rejected>(validator.validate(candidate))
    }

    private fun cleanPage(page: Int, text: String) = OcrStructuredPageCandidate(page, text, OcrPageOutcomeKind.TRANSCRIBED)

    private fun qualifiedPage(page: Int, text: String, spans: List<OcrUncertaintySpan>) = OcrStructuredPageCandidate(
        page,
        text,
        OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS,
        uncertaintySpans = spans,
    )

    private fun pages(vararg values: Int) = OcrPageScope(values.toList())

    private fun candidate(
        pageCandidates: List<OcrStructuredPageCandidate>,
        requested: OcrPageScope = pages(1, 2),
        submitted: OcrPageScope = pages(1, 2),
        declaredReturned: OcrPageScope,
        fidelity: TranscriptionFidelity = TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
        snapshot: OcrModelSnapshot = OcrModelSnapshot.NotExposed,
    ): OcrStructuredTranscriptionCandidate {
        val processing = processing(requested, submitted)
        return OcrStructuredTranscriptionCandidate(
            requestedPageScope = requested,
            submittedPageScope = submitted,
            declaredReturnedPageScope = declaredReturned,
            pages = pageCandidates,
            fidelity = fidelity,
            recognitionIdentity = OcrRecognitionIdentity("structured-transcription", "literal-v1", "1.0.0"),
            providerProvenance = provider(snapshot),
            processingProvenance = processing,
            recognisedAt = Instant.EPOCH,
        )
    }

    private fun provider(snapshot: OcrModelSnapshot) = OcrProviderProvenance(
        providerIdentity = "provider",
        adapterIdentity = "adapter",
        adapterVersion = "1.0.0",
        transcriptionConfigurationProfile = "literal-v1",
        providerReportedModelIdentifier = "provider-model",
        modelSnapshot = snapshot,
        providerCorrelationIdentifier = "correlation-1",
    )

    private fun processing(requested: OcrPageScope, submitted: OcrPageScope) = OcrProcessingProvenance(
        sourceEvidenceArtifactId = EvidenceArtifactId("evidence-1"),
        sourceManifestSha256 = OcrSha256Digest("a".repeat(64)),
        sourceMediaType = "application/pdf",
        sourceByteLength = 100,
        requestedPageScope = requested,
        submittedPageScope = submitted,
        representationMediaType = "application/pdf",
        representationByteLength = 100,
        representationSha256 = OcrSha256Digest("a".repeat(64)),
        byteExactCopy = true,
        processingProfileIdentity = "direct-v1",
        createdAt = Instant.EPOCH,
    )
}
