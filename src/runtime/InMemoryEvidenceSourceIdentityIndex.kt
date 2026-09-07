package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceIdentityIndex
import parker.core.interfaces.SourceIdentityReservation

/**
 * Parker Agent Gateway, AG-1F. In-memory [EvidenceSourceIdentityIndex] --
 * mirrors [InMemoryEvidenceSourceManifestStorage]'s own shape exactly
 * (a `Mutex`-guarded map, no durability). [DefaultEvidenceCustodian]'s own
 * default dependency, for tests and any composition that does not need
 * durability across restarts. Only ever meaningful within a single process
 * (an in-memory map cannot be shared cross-process), so the `Mutex` here is
 * a complete correctness mechanism for this implementation, not merely an
 * optimisation the way it is for [FileSystemEvidenceSourceIdentityIndex]'s
 * own filesystem-atomic `createOrGet`.
 */
class InMemoryEvidenceSourceIdentityIndex : EvidenceSourceIdentityIndex {

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, EvidenceArtifactId>()

    override suspend fun createOrGet(sha256: String, proposedEvidenceArtifactId: EvidenceArtifactId): SourceIdentityReservation =
        mutex.withLock {
            val existing = entries[sha256]
            if (existing != null) {
                SourceIdentityReservation.Existing(existing)
            } else {
                entries[sha256] = proposedEvidenceArtifactId
                SourceIdentityReservation.Created(proposedEvidenceArtifactId)
            }
        }
}
