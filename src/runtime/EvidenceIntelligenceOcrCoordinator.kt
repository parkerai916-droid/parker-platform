package parker.core.runtime

import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.OcrMechanism
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.interfaces.PrincipalId

/**
 * OCR Mechanism, Unit 12 ("Runtime Composition"). Governed in full by
 * `docs/architecture/OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`
 * ("the Unit 12 Scope Lock") and
 * `docs/architecture/OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Section 5.A-5.K -- this class is exactly the
 * one new, small, `internal` component that plan authorises
 * (`EvidenceIntelligenceOcrCoordinator`, illustrative name, now adopted
 * verbatim), mirroring [EvidenceIntelligenceReasoningCoordinator]'s own
 * already-accepted shape precisely.
 *
 * ## Exact dependency direction (Implementation Plan Section 5.B)
 *
 * Exactly two dependencies -- [evidenceCustodian] (already frozen, reused,
 * the same instance already threaded to [EvidenceIntelligenceInputResolver])
 * and [ocrMechanism] (already frozen interface) -- **never** `MemoryCore`,
 * **never** `PermissionEngine`, **never** `EvidenceIntelligence` itself,
 * **never** `TierADocumentIngestionRouter`, **never** a reference back to
 * `ParkerRuntime`. The absence of any other constructor parameter is
 * itself the structural guarantee, mirroring every prior coordinator this
 * Programme has designed.
 *
 * ## Source retrieval and manifest-integrity sequence (Implementation Plan
 * Section 5.G)
 *
 * Mirrors [TierAOwnerInvocationCoordinator]'s own already-accepted
 * four-step sequence exactly, substituting the caller's own
 * already-resolved [content] for the first retrieval step -- input
 * resolution has already happened once in [DefaultEvidenceIntelligence.analyse];
 * this coordinator does not retrieve bytes a second time:
 *
 * 1. [EvidenceCustodian.retrieveManifest]. `NotFound`/`Rejected` -> the
 *    artefact is not OCR-eligible for this call; no [OcrMechanism]
 *    invocation occurs.
 * 2. On `Found(manifest)`: verify `manifest.byteLength == content.size` --
 *    mismatch is a distinct, honest failure, no OCR invocation.
 * 3. Verify `sha256(content) == manifest.sha256` -- the expected digest is
 *    always `manifest.sha256`, the custody-established value, **never** a
 *    digest recomputed from `content` and compared to itself (Unit 12
 *    Scope Lock Section 10's own "no digest tautology" requirement).
 * 4. Only on full verification: determine `mediaType` from
 *    `manifest.receivedMediaType` (Implementation Plan Section 5.J) and
 *    construct the [OcrRecognitionRequest]. An artefact whose manifest
 *    carries no `receivedMediaType` at all, or one that is not
 *    image-bearing, is honestly treated as *not* OCR-eligible -- never
 *    fabricated.
 *
 * ## No permission evaluation of its own (Implementation Plan Section 5.H)
 *
 * This class holds no `PermissionEngine` reference of any kind -- the same
 * structural guarantee `OcrMechanism`/`OcrProviderAdapter`/`OcrExecutionSequencer`
 * already hold, extended one tier further. Invocation-level permission
 * gating remains exclusively `ParkerRuntime.analyseEvidence`'s own,
 * already-accepted responsibility (the existing `(EXECUTE, DOCUMENT)`
 * gate), evaluated once, before [DefaultEvidenceIntelligence.analyse] --
 * and therefore this coordinator -- is ever reached.
 *
 * ## Faults are never swallowed
 *
 * No `try`/`catch` of any kind wraps the [ocrMechanism] invocation,
 * mirroring [OcrExecutionSequencer]'s own identical, already-accepted
 * discipline. A genuine fault the adapter itself throws (a malformed
 * bridge response, a process crash) propagates unchanged out of
 * [recognise], all the way to `ParkerRuntime.analyseEvidence`'s own
 * caller -- never caught, never reinterpreted as one of this coordinator's
 * own ordinary outcomes.
 *
 * ## Statelessness
 *
 * No `var`, no mutable collection, no cache of any kind -- only the two
 * constructor-injected dependencies as fields, mirroring every other
 * coordinator this Programme has already accepted.
 */
internal class EvidenceIntelligenceOcrCoordinator(
    evidenceCustodian: EvidenceCustodian,
    private val ocrMechanism: OcrMechanism,
) {
    private val sourceResolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)

    /**
     * Performs the four-step manifest-verified integrity sequence above,
     * then -- only on full verification, and only for an image-bearing
     * media type -- invokes [ocrMechanism] exactly once, no retry, and
     * returns its own, unmodified [OcrRecognitionOutcome], wrapped in
     * [OcrCoordinatorOutcome.Recognised]. Every other case returns a
     * distinct, honest [OcrCoordinatorOutcome] naming exactly which
     * pre-execution check failed -- never collapsed into, or confused
     * with, any [OcrRecognitionOutcome] variant, which begin only once
     * [OcrMechanism.recognise] is actually called (Implementation Plan
     * Section 5.V).
     *
     * @param requestingPrincipalId Passed through unchanged to
     *   [EvidenceCustodian.retrieveManifest] -- the same principal
     *   `EvidenceIntelligenceInputResolver` already used to retrieve
     *   [content] in the first place.
     * @param evidenceArtifactId The already-custodied identity [content]
     *   was retrieved under.
     * @param content The exact bytes `EvidenceIntelligenceInputResolver`
     *   already retrieved for this artefact -- never re-retrieved here.
     */
    suspend fun recognise(
        requestingPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
        content: ByteArray,
    ): OcrCoordinatorOutcome {
        val trusted = when (val resolution = sourceResolver.verifyAlreadyRetrieved(
            requestingPrincipalId, evidenceArtifactId, content,
        )) {
            is AuthoritativeAcquisitionResolution.Verified -> resolution.input
            AuthoritativeAcquisitionResolution.ManifestNotFound -> return OcrCoordinatorOutcome.ManifestNotFound(evidenceArtifactId)
            is AuthoritativeAcquisitionResolution.ManifestRejected -> return OcrCoordinatorOutcome.ManifestRejected(evidenceArtifactId, resolution.reason)
            AuthoritativeAcquisitionResolution.ManifestIdentityMismatch -> return OcrCoordinatorOutcome.ManifestRejected(evidenceArtifactId, "Authoritative manifest identity mismatch")
            is AuthoritativeAcquisitionResolution.ByteLengthMismatch -> return OcrCoordinatorOutcome.ByteLengthMismatch(evidenceArtifactId, resolution.expected, resolution.actual)
            is AuthoritativeAcquisitionResolution.DigestMismatch -> return OcrCoordinatorOutcome.DigestMismatch(evidenceArtifactId)
            AuthoritativeAcquisitionResolution.SourceNotFound,
            is AuthoritativeAcquisitionResolution.SourceRejected,
            -> error("verifyAlreadyRetrieved does not perform source retrieval")
        }

        val mediaType = trusted.mediaType
        if (mediaType == null || !isOcrEligibleMediaType(mediaType)) {
            return OcrCoordinatorOutcome.NotOcrEligible(evidenceArtifactId, mediaType)
        }

        val outcome = ocrMechanism.recognise(
            OcrRecognitionRequest(
                sourceEvidenceId = evidenceArtifactId,
                content = trusted.bytes(),
                mediaType = mediaType,
                pageCount = null,
            ),
        )
        return OcrCoordinatorOutcome.Recognised(outcome)
    }

    /**
     * Illustrative, not frozen (Implementation Plan Section 5.J): `application/pdf`
     * or an image-prefixed media type. This coordinator performs no media-type
     * judgement beyond this eligibility check -- it never re-derives or
     * second-guesses `manifest.receivedMediaType` itself.
     */
    private fun isOcrEligibleMediaType(mediaType: String): Boolean =
        mediaType == "application/pdf" || mediaType.startsWith("image/", ignoreCase = true)
}

/**
 * The coordinator-internal outcome distinction Implementation Plan Section
 * 5.V authorises -- a pre-execution integrity/eligibility rejection,
 * mirroring [TierAOwnerInvocationOutcome]'s own already-accepted shape.
 * `internal`, never public; never collapsed into, or confused with, any
 * [OcrRecognitionOutcome] variant.
 */
internal sealed class OcrCoordinatorOutcome {
    data class ManifestNotFound(val evidenceArtifactId: EvidenceArtifactId) : OcrCoordinatorOutcome()
    data class ManifestRejected(val evidenceArtifactId: EvidenceArtifactId, val reason: String) : OcrCoordinatorOutcome()
    data class ByteLengthMismatch(
        val evidenceArtifactId: EvidenceArtifactId,
        val expectedByteLength: Long,
        val actualByteLength: Long,
    ) : OcrCoordinatorOutcome()
    data class DigestMismatch(val evidenceArtifactId: EvidenceArtifactId) : OcrCoordinatorOutcome()
    data class NotOcrEligible(val evidenceArtifactId: EvidenceArtifactId, val mediaType: String?) : OcrCoordinatorOutcome()
    data class Recognised(val outcome: OcrRecognitionOutcome) : OcrCoordinatorOutcome()
}
