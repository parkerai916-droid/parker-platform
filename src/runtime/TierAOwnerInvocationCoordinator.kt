package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
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
    private val evidenceCustodian: EvidenceCustodian,
    private val tierADocumentIngestionRouter: TierADocumentIngestionRouter,
) {
    suspend fun invoke(
        ownerPrincipalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
        correlationValue: String,
    ): TierAOwnerInvocationOutcome {
        val manifest = when (val manifestResult = evidenceCustodian.retrieveManifest(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> manifestResult.manifest
            is EvidenceManifestRetrievalResult.NotFound ->
                return TierAOwnerInvocationOutcome.ManifestNotFound(evidenceArtifactId)
            is EvidenceManifestRetrievalResult.Rejected ->
                return TierAOwnerInvocationOutcome.ManifestRetrievalRejected(evidenceArtifactId, manifestResult.reason)
        }

        val content = when (val retrievalResult = evidenceCustodian.retrieve(ownerPrincipalId, evidenceArtifactId)) {
            is EvidenceRetrievalResult.Found -> retrievalResult.content
            is EvidenceRetrievalResult.NotFound ->
                return TierAOwnerInvocationOutcome.SourceNotFound(evidenceArtifactId)
            is EvidenceRetrievalResult.Rejected ->
                return TierAOwnerInvocationOutcome.SourceRetrievalRejected(evidenceArtifactId, retrievalResult.reason)
        }

        val actualByteLength = content.size.toLong()
        if (actualByteLength != manifest.byteLength) {
            return TierAOwnerInvocationOutcome.ByteLengthMismatch(evidenceArtifactId, manifest.byteLength, actualByteLength)
        }

        val actualSha256 = sha256Hex(content)
        if (actualSha256 != manifest.sha256) {
            return TierAOwnerInvocationOutcome.DigestMismatch(evidenceArtifactId, manifest.sha256, actualSha256)
        }

        val context = TierADocumentSourceContext(
            evidenceArtifactId = evidenceArtifactId,
            content = content,
            expectedSha256 = manifest.sha256,
            receivedMediaType = manifest.receivedMediaType,
            originalFileName = manifest.originalFileName,
            requestingPrincipalId = ownerPrincipalId,
            correlationValue = correlationValue,
        )

        return TierAOwnerInvocationOutcome.Routed(tierADocumentIngestionRouter.ingest(context))
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }
}
