package parker.core.runtime

import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADocumentIngestionRouter
import parker.core.interfaces.TierADocumentSourceContext
import parker.core.interfaces.TierAOwnerInvocationOutcome

/**
 * Document Ingestion, Owner-Facing Tier A Runtime Invocation Boundary.
 * Sequences [EvidenceCustodian] (manifest and byte retrieval) and the
 * existing, unchanged [TierADocumentIngestionRouter] -- two
 * independently-governed subsystems -- earning the "Coordinator" name
 * exactly as [EvidenceRegistrationCoordinator] does for the identical
 * reason (sequencing across two separate constitutional domains that
 * must never call each other directly).
 *
 * **Exactly two dependencies, no `PermissionEngine` of its own.**
 * [EvidenceCustodian.retrieveManifest] and [EvidenceCustodian.retrieve]
 * each already gate themselves internally -- this class introduces no
 * new Permission Engine evaluation, no new `PermissionAction`, and no
 * new `ResourceType`, exactly as
 * `docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
 * Section 17 requires ("Tier A invocation itself: requires no new
 * permission gate distinct from the two reads above").
 *
 * ## Sequence
 *
 * 1. [EvidenceCustodian.retrieveManifest] -- anything other than
 *    [EvidenceManifestRetrievalResult.Found] returns immediately; no
 *    byte retrieval, no router call.
 * 2. [EvidenceCustodian.retrieve] -- anything other than
 *    [EvidenceRetrievalResult.Found] returns immediately; no router
 *    call.
 * 3. Byte-length verification: retrieved content's own size against the
 *    manifest's [parker.core.interfaces.EvidenceSourceManifest.byteLength].
 *    A mismatch returns immediately; no digest computation, no router
 *    call.
 * 4. SHA-256 verification: computed from the retrieved bytes, compared
 *    against the manifest's own persisted, independently-established
 *    digest -- never the reverse, and never a digest computed from these
 *    same bytes treated as its own "expected" value (Scope Lock Section
 *    7). A mismatch returns immediately; no router call.
 * 5. [TierADocumentSourceContext] is constructed only now, from the
 *    manifest's own [parker.core.interfaces.EvidenceSourceManifest.receivedMediaType]/
 *    [parker.core.interfaces.EvidenceSourceManifest.originalFileName] --
 *    never inferred, never caller-overridden.
 * 6. [TierADocumentIngestionRouter.ingest] is called exactly once. Its
 *    result is returned unchanged, wrapped in
 *    [TierAOwnerInvocationOutcome.Routed] -- this class never calls a
 *    specialist directly, never retries with a different mechanism, and
 *    never invokes OCR or Tier B.
 *
 * No `try`/`catch` appears anywhere in [invoke] -- a genuine fault from
 * either dependency propagates unchanged, mirroring every other
 * coordinator in this repository's own "faults are never swallowed"
 * discipline.
 */
class TierAOwnerInvocationCoordinator(
    evidenceCustodian: EvidenceCustodian,
    private val tierADocumentIngestionRouter: TierADocumentIngestionRouter,
) {
    private val sourceResolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)

    suspend fun invoke(
        ownerPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
        correlationValue: String,
    ): TierAOwnerInvocationOutcome {
        val trusted = when (val resolution = sourceResolver.resolve(ownerPrincipalId, evidenceArtifactId)) {
            is AuthoritativeAcquisitionResolution.Verified -> resolution.input
            AuthoritativeAcquisitionResolution.ManifestNotFound -> return TierAOwnerInvocationOutcome.ManifestNotFound(evidenceArtifactId)
            is AuthoritativeAcquisitionResolution.ManifestRejected ->
                return TierAOwnerInvocationOutcome.ManifestRetrievalRejected(evidenceArtifactId, resolution.reason)
            AuthoritativeAcquisitionResolution.ManifestIdentityMismatch ->
                return TierAOwnerInvocationOutcome.ManifestRetrievalRejected(evidenceArtifactId, "Authoritative manifest identity mismatch")
            AuthoritativeAcquisitionResolution.SourceNotFound -> return TierAOwnerInvocationOutcome.SourceNotFound(evidenceArtifactId)
            is AuthoritativeAcquisitionResolution.SourceRejected ->
                return TierAOwnerInvocationOutcome.SourceRetrievalRejected(evidenceArtifactId, resolution.reason)
            is AuthoritativeAcquisitionResolution.ByteLengthMismatch ->
                return TierAOwnerInvocationOutcome.ByteLengthMismatch(evidenceArtifactId, resolution.expected, resolution.actual)
            is AuthoritativeAcquisitionResolution.DigestMismatch ->
                return TierAOwnerInvocationOutcome.DigestMismatch(evidenceArtifactId, resolution.expected, resolution.actual)
        }

        val context = TierADocumentSourceContext(
            evidenceArtifactId = evidenceArtifactId,
            content = trusted.bytes(),
            expectedSha256 = trusted.sha256,
            receivedMediaType = trusted.mediaType,
            originalFileName = trusted.originalFileName,
            requestingPrincipalId = ownerPrincipalId,
            correlationValue = correlationValue,
        )

        return TierAOwnerInvocationOutcome.Routed(tierADocumentIngestionRouter.ingest(context))
    }
}
