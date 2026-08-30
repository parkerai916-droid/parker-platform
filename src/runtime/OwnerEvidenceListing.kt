package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId

data class OwnerRegisteredEvidence(
    val evidenceArtifactId: EvidenceArtifactId,
    val sha256: String,
    val byteLength: Long,
    val mediaType: String?,
    val originalFileName: String?,
)

/**
 * Explicit owner-only discovery of immutable manifest registrations. This is the durable listing
 * mechanism, not a fallback: identities come only from canonical `.manifest` records, and every
 * row is re-read through the manifest codec and the governed Evidence Custodian byte-retrieval
 * boundary before it is returned. Any malformed, missing, denied, or integrity-mismatched entry
 * fails the entire listing closed; it is never silently omitted or repaired.
 */
class FileSystemOwnerEvidenceListing(
    storageRoot: Path,
    private val ownerPrincipalId: PrincipalId,
    private val evidenceCustodian: EvidenceCustodian,
) {
    private val root = storageRoot.toAbsolutePath().normalize()
    private val manifests = FileSystemEvidenceSourceManifestStorage(root)

    init {
        require(Files.isDirectory(root) && Files.isReadable(root))
    }

    suspend fun listRegistered(): List<OwnerRegisteredEvidence> {
        val ids = try {
            Files.list(root).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(SUFFIX) }
                    .map { it.fileName.toString().removeSuffix(SUFFIX) }
                    .sorted()
                    .map(::EvidenceArtifactId)
                    .toList()
            }
        } catch (e: Exception) {
            throw IllegalStateException("durable evidence registration listing unavailable", e)
        }
        return ids.map { id ->
            val manifest = requireNotNull(manifests.read(id)) { "durable evidence registration disappeared during listing" }
            val found = evidenceCustodian.retrieve(ownerPrincipalId, id) as? EvidenceRetrievalResult.Found
                ?: error("durably registered evidence is not retrievable")
            require(found.evidenceArtifactId == id)
            require(found.content.size.toLong() == manifest.byteLength) { "durable evidence byte length mismatch" }
            require(sha256(found.content) == manifest.sha256) { "durable evidence digest mismatch" }
            OwnerRegisteredEvidence(id, manifest.sha256, manifest.byteLength,
                manifest.receivedMediaType, manifest.originalFileName)
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object { const val SUFFIX = ".manifest" }
}
