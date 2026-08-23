package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceManifest
import parker.core.interfaces.EvidenceSourceManifestStorage
import parker.core.interfaces.EvidenceSourceManifestStorageException

/**
 * Document Ingestion, Authoritative Source Manifest Foundation
 * Implementation. The test/dev-only [EvidenceSourceManifestStorage]
 * implementation, backed by a plain in-memory map -- mirroring
 * [InMemoryEvidenceArtifactStorage]'s own established shape exactly.
 * Never the production implementation; nothing written here survives
 * process exit.
 */
class InMemoryEvidenceSourceManifestStorage : EvidenceSourceManifestStorage {

    private val mutex = Mutex()
    private val store = mutableMapOf<EvidenceArtifactId, EvidenceSourceManifest>()

    override suspend fun write(manifest: EvidenceSourceManifest) {
        mutex.withLock {
            if (store.containsKey(manifest.evidenceArtifactId)) {
                throw EvidenceSourceManifestStorageException.DuplicateIdentifier(manifest.evidenceArtifactId)
            }
            store[manifest.evidenceArtifactId] = manifest
        }
    }

    override suspend fun read(evidenceArtifactId: EvidenceArtifactId): EvidenceSourceManifest? =
        mutex.withLock { store[evidenceArtifactId] }

    override suspend fun delete(evidenceArtifactId: EvidenceArtifactId): Boolean =
        mutex.withLock { store.remove(evidenceArtifactId) != null }
}
