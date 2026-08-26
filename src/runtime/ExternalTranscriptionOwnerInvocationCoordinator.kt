package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExternalTranscriptionMechanism
import parker.core.interfaces.ExternalTranscriptionMechanismOutcome
import parker.core.interfaces.ExternalTranscriptionOwnerInvocationOutcome
import parker.core.interfaces.ExternalTranscriptionRequest
import parker.core.interfaces.OcrSha256Digest
import parker.core.interfaces.OcrProcessingRepresentationOutcome
import parker.core.interfaces.OcrStructuredValidationOutcome
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId

/** Owner-bound authorization, custody verification, one external invocation, and pure validation. */
class ExternalTranscriptionOwnerInvocationCoordinator(
    private val ownerPrincipalId: PrincipalId,
    private val permissionEngine: PermissionEngine,
    private val evidenceCustodian: EvidenceCustodian,
    private val externalMechanism: ExternalTranscriptionMechanism,
    private val validator: OcrStructuredResultValidator,
    private val representationFactory: OcrProcessingRepresentationFactory = OcrProcessingRepresentationFactory(),
) {
    suspend fun invoke(evidenceArtifactId: EvidenceArtifactId): ExternalTranscriptionOwnerInvocationOutcome {
        val decision = permissionEngine.evaluate(
            ExternalTranscriptionInvocationGate.buildExecutionRequest(ownerPrincipalId, evidenceArtifactId),
        )
        if (decision.decision != PermissionDecisionOutcome.APPROVED &&
            decision.decision != PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION
        ) return ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised

        val content = when (val retrieval = evidenceCustodian.retrieve(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceRetrievalResult.Found -> retrieval.content
            is EvidenceRetrievalResult.NotFound -> return ExternalTranscriptionOwnerInvocationOutcome.SourceNotFound(evidenceArtifactId)
            is EvidenceRetrievalResult.Rejected -> return ExternalTranscriptionOwnerInvocationOutcome.SourceRetrievalRejected(evidenceArtifactId)
        }
        val manifest = when (val retrieval = evidenceCustodian.retrieveManifest(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> retrieval.manifest
            is EvidenceManifestRetrievalResult.NotFound -> return ExternalTranscriptionOwnerInvocationOutcome.ManifestNotFound(evidenceArtifactId)
            is EvidenceManifestRetrievalResult.Rejected -> return ExternalTranscriptionOwnerInvocationOutcome.ManifestRejected(evidenceArtifactId)
        }
        if (manifest.evidenceArtifactId != evidenceArtifactId) {
            return ExternalTranscriptionOwnerInvocationOutcome.ManifestRejected(evidenceArtifactId)
        }
        if (manifest.byteLength != content.size.toLong()) {
            return ExternalTranscriptionOwnerInvocationOutcome.ByteLengthMismatch(evidenceArtifactId)
        }
        val digest = sha256(content)
        if (manifest.sha256 != digest) return ExternalTranscriptionOwnerInvocationOutcome.DigestMismatch(evidenceArtifactId)
        val mediaType = manifest.receivedMediaType
        if (content.isEmpty() || content.size.toLong() > ExternalTranscriptionRequest.MAX_SOURCE_BYTES ||
            mediaType == null || (mediaType != "application/pdf" && !mediaType.startsWith("image/", ignoreCase = true))
        ) return ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds(evidenceArtifactId)

        val representation = when (val outcome = representationFactory.create(
            sourceEvidenceArtifactId = evidenceArtifactId,
            verifiedSourceBytes = content,
            authoritativeManifestSha256 = OcrSha256Digest(manifest.sha256),
            authoritativeSourceMediaType = mediaType,
            authoritativeSourceByteLength = manifest.byteLength,
        )) {
            is OcrProcessingRepresentationOutcome.Created -> outcome.representation
            else -> return ExternalTranscriptionOwnerInvocationOutcome.UnsupportedOrOutOfBounds(evidenceArtifactId)
        }
        val request = ExternalTranscriptionRequest(
            processingRepresentation = representation,
            maximumPageCount = ExternalTranscriptionRequest.MAX_PAGE_COUNT,
        )
        val candidate = when (val mechanismOutcome = externalMechanism.transcribe(request)) {
            is ExternalTranscriptionMechanismOutcome.Candidate -> mechanismOutcome.candidate
            is ExternalTranscriptionMechanismOutcome.Failure ->
                return ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure(mechanismOutcome.reason)
        }
        val provenance = candidate.processingProvenance
        if (provenance.sourceEvidenceArtifactId != evidenceArtifactId ||
            provenance.sourceManifestSha256.value != manifest.sha256 ||
            provenance.sourceMediaType != mediaType ||
            provenance.sourceByteLength != content.size.toLong() ||
            !provenance.byteExactCopy || provenance.representationMediaType != mediaType ||
            provenance.representationByteLength != content.size.toLong() ||
            provenance.representationSha256.value != manifest.sha256
        ) return ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected("Candidate provenance contradicts the verified source representation")

        return when (val validated = validator.validate(candidate)) {
            is OcrStructuredValidationOutcome.Validated ->
                ExternalTranscriptionOwnerInvocationOutcome.Validated(evidenceArtifactId, validated)
            is OcrStructuredValidationOutcome.Rejected ->
                ExternalTranscriptionOwnerInvocationOutcome.ValidationRejected(validated.outcome.reason)
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
}
