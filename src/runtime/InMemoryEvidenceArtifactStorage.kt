package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactIdentifierSafety
import parker.core.interfaces.EvidenceArtifactStorage
import parker.core.interfaces.EvidenceArtifactStorageException

/**
 * Evidence Custodian, Implementation Plan Phase 2, Unit 1: the
 * test-only [EvidenceArtifactStorage] implementation, backed by a plain
 * in-memory map -- for fast unit tests elsewhere in this Programme,
 * mirroring this repository's own established `InMemory*` convention
 * ([InMemoryMemoryCore], [InMemoryWorldModel]). This is never the
 * production implementation; nothing written here survives process exit,
 * and this implementation makes no crash-safety or durability claim of
 * any kind.
 *
 * Applies the same identifier-safety rule
 * ([EvidenceArtifactIdentifierSafety]) that [FileSystemEvidenceArtifactStorage]
 * applies, even though this implementation never constructs a filesystem
 * path -- so that which identifiers are accepted does not silently depend
 * on which [EvidenceArtifactStorage] implementation happens to be in use.
 *
 * Copies [ByteArray] content on both write and read: the stored value is
 * never the same array instance a caller passed to [write], and a value
 * returned by [read] is never the same array instance held internally --
 * so a caller mutating an array it passed in, or one it received back,
 * can never affect what this implementation holds or returns on a later
 * call. This mirrors [EvidenceArtifactStorage]'s own documented byte-ownership
 * guarantee, which the filesystem implementation satisfies for a
 * different reason (disk I/O never shares an in-memory reference at all).
 */
class InMemoryEvidenceArtifactStorage : EvidenceArtifactStorage {

    private val mutex = Mutex()
    private val store = mutableMapOf<EvidenceArtifactId, ByteArray>()

    override suspend fun write(evidenceArtifactId: EvidenceArtifactId, content: ByteArray) {
        EvidenceArtifactIdentifierSafety.requireSafe(evidenceArtifactId)

        mutex.withLock {
            if (store.containsKey(evidenceArtifactId)) {
                throw EvidenceArtifactStorageException.DuplicateIdentifier(evidenceArtifactId)
            }
            store[evidenceArtifactId] = content.copyOf()
        }
    }

    override suspend fun read(evidenceArtifactId: EvidenceArtifactId): ByteArray? {
        EvidenceArtifactIdentifierSafety.requireSafe(evidenceArtifactId)

        return mutex.withLock { store[evidenceArtifactId]?.copyOf() }
    }

    /**
     * Implementation Plan Phase 7 ("Deletion workflow"). [MutableMap.remove]
     * already returns the removed value or `null`, giving exactly the
     * `Boolean` this Unit's own [EvidenceArtifactStorage.delete] contract
     * requires. No tombstone of any kind is retained.
     */
    override suspend fun delete(evidenceArtifactId: EvidenceArtifactId): Boolean {
        EvidenceArtifactIdentifierSafety.requireSafe(evidenceArtifactId)

        return mutex.withLock { store.remove(evidenceArtifactId) != null }
    }
}
