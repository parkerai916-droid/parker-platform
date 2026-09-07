package parker.core.runtime

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceIdentityIndex
import parker.core.interfaces.SourceIdentityReservation

/**
 * Parker Agent Gateway, AG-1F (Section 8, Section 16), corrected by the crash-safe idempotency
 * review. The durable, production [EvidenceSourceIdentityIndex] -- one file per authoritative
 * SHA-256, named directly by the hash itself (already a safe, fixed-length, lowercase-hex
 * filename; no additional digest of the key is needed, unlike
 * [FileSystemExternalTranscriptionAuthorizationStore]'s own digest-of-key step for its own,
 * differently-shaped `evidenceArtifactId` key). Uses the same `CREATE_NEW` atomic-create primitive
 * that class's own `createOrGet` already establishes as this subsystem's template for exactly this
 * shape of idempotent claim -- opened via [FileChannel] rather than [Files.newByteChannel] so the
 * reservation write can be forced durable before being treated as complete (see "Durability,"
 * below -- the crash-safe idempotency review's own required correction).
 *
 * ## Durability
 *
 * [createOrGet]'s atomic-create write is followed by `FileChannel.force(true)` before the
 * reservation is reported as [SourceIdentityReservation.Created] -- mirroring
 * [FileSystemEvidenceArtifactStorage.write]/[FileSystemEvidenceSourceManifestStorage.write]'s own
 * existing durability discipline, which this class's original version lacked (a genuine gap
 * identified by the crash-safe idempotency review, distinct from the crash window itself).
 *
 * ## Cross-process correctness, not merely an in-process guard
 *
 * Unlike [InMemoryEvidenceSourceIdentityIndex] (correct only within one process), [createOrGet]'s
 * `CREATE_NEW` atomic file creation is the actual cross-process serialisation point: exactly one
 * of any number of concurrent processes racing to reserve the same [sha256] -- on this host,
 * against this storage root -- ever receives [SourceIdentityReservation.Created]; every other
 * racer, regardless of timing, receives [SourceIdentityReservation.Existing] naming the winner's
 * identity. [DefaultEvidenceCustodian] relies on this property directly and does not itself
 * serialise calls to this method with any in-process lock.
 *
 * @param storageRoot Its parent must already exist and be writable,
 *   validated once at construction, mirroring
 *   [FileSystemEvidenceDeletionAudit]'s own "fail fast at construction"
 *   discipline.
 */
class FileSystemEvidenceSourceIdentityIndex(storageRoot: Path) : EvidenceSourceIdentityIndex {

    private val root = storageRoot.toAbsolutePath().normalize()

    init {
        require(Files.isDirectory(root)) { "EvidenceSourceIdentityIndex storage root does not exist or is not a directory: '$root'" }
        require(Files.isWritable(root)) { "EvidenceSourceIdentityIndex storage root is not writable: '$root'" }
    }

    override suspend fun createOrGet(sha256: String, proposedEvidenceArtifactId: EvidenceArtifactId): SourceIdentityReservation {
        val path = pathFor(sha256)
        return try {
            FileChannel.open(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE).use { channel ->
                val buffer = ByteBuffer.wrap(proposedEvidenceArtifactId.value.toByteArray(StandardCharsets.UTF_8))
                while (buffer.hasRemaining()) channel.write(buffer)
                channel.force(true)
            }
            SourceIdentityReservation.Created(proposedEvidenceArtifactId)
        } catch (e: FileAlreadyExistsException) {
            val existing = Files.readString(path, StandardCharsets.UTF_8).trim()
            SourceIdentityReservation.Existing(EvidenceArtifactId(existing))
        }
    }

    private fun pathFor(sha256: String): Path {
        require(sha256.matches(SHA256_PATTERN)) { "EvidenceSourceIdentityIndex key must be a 64-character lowercase hex SHA-256" }
        return root.resolve("$sha256.source-identity-v1").normalize().also {
            require(it.parent == root) { "computed source-identity index path escaped storage root" }
        }
    }

    private companion object {
        val SHA256_PATTERN = Regex("^[0-9a-f]{64}$")
    }
}
