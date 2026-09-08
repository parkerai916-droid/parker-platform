package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*

class EmlStructuredResultValidatorTest {
    private val evidenceId = EvidenceArtifactId("eml-validator-evidence")
    private val binding = ExternalTranscriptionExecutionBinding("request-1", "attempt-1", "eml-profile-1")

    private suspend fun representation(source: ByteArray = plainEml()): EmlDerivedRepresentation {
        val structural = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result
        val trusted = trusted(source)
        return assertIs<EmlDerivedRepresentationOutcome.Created>(EmlDerivedRepresentationFactory().create(trusted, structural)).representation
    }

    private fun plainEml() = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
        "Content-Type: text/plain; charset=utf-8\r\n\r\nHello body\r\n").toByteArray()

    private suspend fun trusted(bytes: ByteArray): AuthoritativeAcquisitionInput {
        val digest = sha256(bytes)
        val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) = EvidenceAcceptanceResult.Rejected("unused")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = EvidenceRetrievalResult.Found(evidenceId, bytes)
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(evidenceId, digest, bytes.size.toLong(), "message/rfc822"))
            override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?) =
                throw UnsupportedOperationException("not used")
        }
        return assertIs<AuthoritativeAcquisitionResolution.Verified>(
            AuthoritativeAcquisitionSourceResolver(custodian).resolve(PrincipalId("owner"), evidenceId),
        ).input
    }

    private fun candidateFor(representation: EmlDerivedRepresentation, sections: List<EmlVerifiedSectionOutcome>) = EmlStructuredTranscriptionCandidate(
        profileId = binding.profileId, requestId = binding.requestId, attemptId = binding.attemptId,
        sourceEvidenceArtifactId = representation.sourceEvidenceArtifactId,
        submittedRepresentationSha256 = representation.representationSha256,
        processingProfileIdentity = representation.transformationProfileIdentity,
        messageOutcome = EmlMessageOutcomeKind.TRANSCRIBED,
        completenessState = DerivativeCompletenessState.ACCOUNTED_FOR,
        sections = sections,
        recognitionIdentity = OcrRecognitionIdentity("openai-responses", "eml-profile-1", "2.0.0"),
        providerProvenance = OcrProviderProvenance("OpenAI", "openai-responses-adapter", "2.0.0", "eml-profile-1", "model", OcrModelSnapshot.NotExposed, "corr-1"),
        recognisedAt = Instant.EPOCH,
    )

    @Test fun `a fully covered, clean candidate validates`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(representation, listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED)))
        val outcome = EmlStructuredResultValidator().validate(candidate, representation, binding)
        val validated = assertIs<EmlStructuredValidationOutcome.Validated>(outcome)
        assertEquals(listOf(entityId), validated.verifiedSectionIds)
    }

    @Test fun `wrong request ID is rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(representation, listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED))).copy(requestId = "other-request")
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `wrong attempt ID is rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(representation, listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED))).copy(attemptId = "other-attempt")
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `wrong Evidence ID is rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(representation, listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED)))
            .copy(sourceEvidenceArtifactId = EvidenceArtifactId("someone-elses-evidence"))
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `wrong representation digest is rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(representation, listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED)))
            .copy(submittedRepresentationSha256 = OcrSha256Digest("0".repeat(64)))
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `wrong processing profile is rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(representation, listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED)))
            .copy(processingProfileIdentity = "some-other-profile")
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `unknown or invented MIME entity IDs are rejected`() = runTest {
        val representation = representation()
        val candidate = candidateFor(representation, listOf(EmlVerifiedSectionOutcome("9.9.9-invented", OcrPageOutcomeKind.TRANSCRIBED)))
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `omitted required MIME entities are rejected`() = runTest {
        val representation = representation()
        val candidate = candidateFor(representation, emptyList()).copy(sections = emptyList())
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `duplicate section identities are rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(representation, listOf(
            EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED),
            EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED),
        ))
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `malformed response with no sections is rejected`() = runTest {
        val representation = representation()
        val candidate = candidateFor(representation, emptyList())
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `contradictory outcome -- FAILED declared but section clean-transcribed -- is rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(representation, listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED)))
            .copy(messageOutcome = EmlMessageOutcomeKind.FAILED)
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    @Test fun `a section qualification without reason or warnings is rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(representation, listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS)))
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation, binding))
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
