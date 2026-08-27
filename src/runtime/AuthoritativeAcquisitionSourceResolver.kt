package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.AcquisitionRepresentationClass
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId

/**
 * The internal execution token proving that acquisition input was checked against Evidence
 * Custodian's authoritative manifest. Its implementation is private to this file: callers can
 * consume a verified token but cannot promote arbitrary bytes or caller-supplied facts into one.
 */
internal sealed interface AuthoritativeAcquisitionInput {
    val evidenceArtifactId: EvidenceArtifactId
    val sha256: String
    val byteLength: Long
    val mediaType: String?
    val originalFileName: String?
    val representationClass: AcquisitionRepresentationClass
    fun bytes(): ByteArray
}

private class CustodyVerifiedAcquisitionInput(
    override val evidenceArtifactId: EvidenceArtifactId,
    override val sha256: String,
    override val byteLength: Long,
    override val mediaType: String?,
    override val originalFileName: String?,
    sourceBytes: ByteArray,
) : AuthoritativeAcquisitionInput {
    private val canonicalBytes = sourceBytes.copyOf()

    override val representationClass =
        AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY

    /** Every consumer receives a defensive byte-exact copy; canonical verified state is never exposed. */
    override fun bytes(): ByteArray = canonicalBytes.copyOf()
}

internal sealed interface AuthoritativeAcquisitionResolution {
    data class Verified(val input: AuthoritativeAcquisitionInput) : AuthoritativeAcquisitionResolution
    data object ManifestNotFound : AuthoritativeAcquisitionResolution
    data class ManifestRejected(val reason: String) : AuthoritativeAcquisitionResolution
    data object SourceNotFound : AuthoritativeAcquisitionResolution
    data class SourceRejected(val reason: String) : AuthoritativeAcquisitionResolution
    data object ManifestIdentityMismatch : AuthoritativeAcquisitionResolution
    data class ByteLengthMismatch(val expected: Long, val actual: Long) : AuthoritativeAcquisitionResolution
    data class DigestMismatch(val expected: String, val actual: String) : AuthoritativeAcquisitionResolution
}

/**
 * Sole trusted-construction boundary for ordinary production acquisition. It either resolves the
 * exact custodied bytes itself or verifies bytes already returned by the same Evidence Custodian
 * read path. No derivative store, generation lookup, caller trust flag, or fallback is reachable.
 */
internal class AuthoritativeAcquisitionSourceResolver(
    private val evidenceCustodian: EvidenceCustodian,
) {
    suspend fun resolve(
        principalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): AuthoritativeAcquisitionResolution {
        val manifest = when (val result = evidenceCustodian.retrieveManifest(principalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> result.manifest
            is EvidenceManifestRetrievalResult.NotFound -> return AuthoritativeAcquisitionResolution.ManifestNotFound
            is EvidenceManifestRetrievalResult.Rejected -> return AuthoritativeAcquisitionResolution.ManifestRejected(result.reason)
        }
        if (manifest.evidenceArtifactId != evidenceArtifactId) {
            return AuthoritativeAcquisitionResolution.ManifestIdentityMismatch
        }
        val bytes = when (val result = evidenceCustodian.retrieve(principalId, evidenceArtifactId)) {
            is EvidenceRetrievalResult.Found -> {
                if (result.evidenceArtifactId != evidenceArtifactId) {
                    return AuthoritativeAcquisitionResolution.ManifestIdentityMismatch
                }
                result.content
            }
            is EvidenceRetrievalResult.NotFound -> return AuthoritativeAcquisitionResolution.SourceNotFound
            is EvidenceRetrievalResult.Rejected -> return AuthoritativeAcquisitionResolution.SourceRejected(result.reason)
        }
        return verify(manifest.evidenceArtifactId, manifest.sha256, manifest.byteLength,
            manifest.receivedMediaType, manifest.originalFileName, evidenceArtifactId, bytes)
    }

    /** Preserves the external-transcription boundary's established source-then-manifest read order. */
    suspend fun resolveSourceThenManifest(
        principalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
    ): AuthoritativeAcquisitionResolution {
        val bytes = when (val result = evidenceCustodian.retrieve(principalId, evidenceArtifactId)) {
            is EvidenceRetrievalResult.Found -> {
                if (result.evidenceArtifactId != evidenceArtifactId) {
                    return AuthoritativeAcquisitionResolution.ManifestIdentityMismatch
                }
                result.content
            }
            is EvidenceRetrievalResult.NotFound -> return AuthoritativeAcquisitionResolution.SourceNotFound
            is EvidenceRetrievalResult.Rejected -> return AuthoritativeAcquisitionResolution.SourceRejected(result.reason)
        }
        val manifest = when (val result = evidenceCustodian.retrieveManifest(principalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> result.manifest
            is EvidenceManifestRetrievalResult.NotFound -> return AuthoritativeAcquisitionResolution.ManifestNotFound
            is EvidenceManifestRetrievalResult.Rejected -> return AuthoritativeAcquisitionResolution.ManifestRejected(result.reason)
        }
        return verify(manifest.evidenceArtifactId, manifest.sha256, manifest.byteLength,
            manifest.receivedMediaType, manifest.originalFileName, evidenceArtifactId, bytes)
    }

    suspend fun verifyAlreadyRetrieved(
        principalId: PrincipalId,
        evidenceArtifactId: EvidenceArtifactId,
        retrievedBytes: ByteArray,
    ): AuthoritativeAcquisitionResolution {
        val manifest = when (val result = evidenceCustodian.retrieveManifest(principalId, evidenceArtifactId)) {
            is EvidenceManifestRetrievalResult.Found -> result.manifest
            is EvidenceManifestRetrievalResult.NotFound -> return AuthoritativeAcquisitionResolution.ManifestNotFound
            is EvidenceManifestRetrievalResult.Rejected -> return AuthoritativeAcquisitionResolution.ManifestRejected(result.reason)
        }
        return verify(manifest.evidenceArtifactId, manifest.sha256, manifest.byteLength,
            manifest.receivedMediaType, manifest.originalFileName, evidenceArtifactId, retrievedBytes)
    }

    private fun verify(
        manifestEvidenceArtifactId: EvidenceArtifactId,
        manifestSha256: String,
        manifestByteLength: Long,
        manifestMediaType: String?,
        manifestOriginalFileName: String?,
        requestedEvidenceArtifactId: EvidenceArtifactId,
        bytes: ByteArray,
    ): AuthoritativeAcquisitionResolution {
        if (manifestEvidenceArtifactId != requestedEvidenceArtifactId) {
            return AuthoritativeAcquisitionResolution.ManifestIdentityMismatch
        }
        if (manifestByteLength != bytes.size.toLong()) {
            return AuthoritativeAcquisitionResolution.ByteLengthMismatch(manifestByteLength, bytes.size.toLong())
        }
        val actualDigest = sha256(bytes)
        if (manifestSha256 != actualDigest) {
            return AuthoritativeAcquisitionResolution.DigestMismatch(manifestSha256, actualDigest)
        }
        return AuthoritativeAcquisitionResolution.Verified(
            CustodyVerifiedAcquisitionInput(
                manifestEvidenceArtifactId,
                manifestSha256,
                manifestByteLength,
                manifestMediaType,
                manifestOriginalFileName,
                bytes,
            ),
        )
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
}
