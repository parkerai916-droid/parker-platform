package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*

/**
 * EML TRANSPORT-BINDING CORRECTION: this validator no longer receives or checks profile/request/
 * attempt identity, Evidence ID, representation digest, or processing profile identity -- those
 * six values are no longer part of the model-generated candidate at all (moved to Responses API
 * request/response metadata, verified in OpenAiResponsesExternalTranscriptionAdapter's
 * requireEmlResponseMetadataBinding before the candidate is ever parsed). This file therefore no
 * longer contains "wrong request/attempt ID"/"wrong Evidence ID"/"wrong digest"/"wrong processing
 * profile" tests -- the equivalent coverage now lives in EmlExternalTranscriptionAdapterTest's
 * transport-binding tests, at the layer that actually performs those checks now.
 */
class EmlStructuredResultValidatorTest {
    private val evidenceId = EvidenceArtifactId("eml-validator-evidence")

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

    private fun candidateFor(sections: List<EmlVerifiedSectionOutcome>) = EmlStructuredTranscriptionCandidate(
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
        val candidate = candidateFor(listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED)))
        val outcome = EmlStructuredResultValidator().validate(candidate, representation)
        val validated = assertIs<EmlStructuredValidationOutcome.Validated>(outcome)
        assertEquals(listOf(entityId), validated.verifiedSectionIds)
    }

    @Test fun `unknown or invented MIME entity IDs are rejected`() = runTest {
        val representation = representation()
        val candidate = candidateFor(listOf(EmlVerifiedSectionOutcome("9.9.9-invented", OcrPageOutcomeKind.TRANSCRIBED)))
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation))
    }

    @Test fun `omitted required MIME entities are rejected`() = runTest {
        val representation = representation()
        val candidate = candidateFor(emptyList()).copy(sections = emptyList())
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation))
    }

    @Test fun `duplicate section identities are rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(listOf(
            EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED),
            EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED),
        ))
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation))
    }

    @Test fun `malformed response with no sections is rejected`() = runTest {
        val representation = representation()
        val candidate = candidateFor(emptyList())
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation))
    }

    @Test fun `contradictory outcome -- FAILED declared but section clean-transcribed -- is rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED)))
            .copy(messageOutcome = EmlMessageOutcomeKind.FAILED)
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation))
    }

    @Test fun `a section qualification without reason or warnings is rejected`() = runTest {
        val representation = representation()
        val entityId = representation.provenance.bodyAlternativesIncluded.single()
        val candidate = candidateFor(listOf(EmlVerifiedSectionOutcome(entityId, OcrPageOutcomeKind.TRANSCRIBED_WITH_QUALIFICATIONS)))
        assertIs<EmlStructuredValidationOutcome.Rejected>(EmlStructuredResultValidator().validate(candidate, representation))
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
