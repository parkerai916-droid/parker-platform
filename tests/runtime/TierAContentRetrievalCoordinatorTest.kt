package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DerivativeContentEntry
import parker.core.interfaces.DerivativeContentStorage
import parker.core.interfaces.DerivativeContentStorageException
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.DerivativeGenerationTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADerivativePayloadFixtures
import parker.core.interfaces.TierAContentRetrievalOutcome

/**
 * Document Ingestion — Derivative Content Persistence and Retrieval.
 * Governed by `docs/architecture/DOCUMENT_INGESTION_DERIVATIVE_CONTENT_PERSISTENCE_RETRIEVAL_SCOPE_LOCK.md`
 * §10. Proves [TierAContentRetrievalCoordinator] distinguishes every
 * governed retrieval outcome (unknown generation, source/generation
 * mismatch, missing content, corrupt content, unsupported representation
 * version, success) and never fabricates a result -- with hand-written
 * fakes for both dependencies, mirroring [TierAOwnerInvocationCoordinatorTest]'s
 * own established fake-based style.
 */
class TierAContentRetrievalCoordinatorTest {

    private val knownGenerationId = DerivativeGenerationId("generation-known")
    private val knownEvidenceArtifactId = EvidenceArtifactId("source-1")

    private fun record(id: DerivativeGenerationId = knownGenerationId, root: EvidenceArtifactId = knownEvidenceArtifactId) =
        DerivativeGenerationTest.record(id.value).copy(rootSourceEvidenceArtifactId = root, parents = listOf(parker.core.interfaces.DerivativeParentReference.RootEvidenceArtifact(root)))

    private fun contentEntry(id: DerivativeGenerationId = knownGenerationId, root: EvidenceArtifactId = knownEvidenceArtifactId) =
        DerivativeContentEntry(id, root, TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()))

    @Test
    fun `an unknown generation identity returns UnknownGeneration -- content store is never consulted`() = runTest {
        val generationStorage = FakeDerivativeGenerationStorage { null }
        val contentStorage = FakeDerivativeContentStorage(onRetrieve = { throw AssertionError("content store must not be consulted") })
        val coordinator = TierAContentRetrievalCoordinator(generationStorage, contentStorage)

        val outcome = coordinator.retrieve(knownEvidenceArtifactId, knownGenerationId)

        assertEquals(TierAContentRetrievalOutcome.UnknownGeneration(knownGenerationId), outcome)
    }

    @Test
    fun `a source evidence artefact that does not own the generation returns SourceMismatch -- content store is never consulted`() = runTest {
        val generationStorage = FakeDerivativeGenerationStorage { record(root = EvidenceArtifactId("actual-owner")) }
        val contentStorage = FakeDerivativeContentStorage(onRetrieve = { throw AssertionError("content store must not be consulted") })
        val coordinator = TierAContentRetrievalCoordinator(generationStorage, contentStorage)

        val outcome = coordinator.retrieve(EvidenceArtifactId("wrong-caller-supplied-id"), knownGenerationId)

        assertEquals(TierAContentRetrievalOutcome.SourceMismatch(EvidenceArtifactId("wrong-caller-supplied-id"), knownGenerationId), outcome)
    }

    @Test
    fun `a record with no matching content entry returns ContentMissing`() = runTest {
        val generationStorage = FakeDerivativeGenerationStorage { record() }
        val contentStorage = FakeDerivativeContentStorage(onRetrieve = { null })
        val coordinator = TierAContentRetrievalCoordinator(generationStorage, contentStorage)

        val outcome = coordinator.retrieve(knownEvidenceArtifactId, knownGenerationId)

        assertEquals(TierAContentRetrievalOutcome.ContentMissing(knownGenerationId), outcome)
    }

    @Test
    fun `corrupt stored content returns ContentCorrupt, never a partially-decoded payload`() = runTest {
        val generationStorage = FakeDerivativeGenerationStorage { record() }
        val contentStorage = FakeDerivativeContentStorage(
            onRetrieve = { throw DerivativeContentStorageException.CorruptContent(knownGenerationId, "digest mismatch") },
        )
        val coordinator = TierAContentRetrievalCoordinator(generationStorage, contentStorage)

        val outcome = assertIs<TierAContentRetrievalOutcome.ContentCorrupt>(coordinator.retrieve(knownEvidenceArtifactId, knownGenerationId))

        assertEquals(knownGenerationId, outcome.derivativeGenerationId)
        assertEquals("Derivative content 'generation-known' is corrupt: digest mismatch", outcome.reason)
    }

    @Test
    fun `an unsupported representation version returns UnsupportedRepresentationVersion distinctly, not ContentCorrupt`() = runTest {
        val generationStorage = FakeDerivativeGenerationStorage { record() }
        val contentStorage = FakeDerivativeContentStorage(
            onRetrieve = { throw DerivativeContentStorageException.UnsupportedRepresentationVersion(knownGenerationId, 99) },
        )
        val coordinator = TierAContentRetrievalCoordinator(generationStorage, contentStorage)

        val outcome = assertIs<TierAContentRetrievalOutcome.UnsupportedRepresentationVersion>(coordinator.retrieve(knownEvidenceArtifactId, knownGenerationId))

        assertEquals(knownGenerationId, outcome.derivativeGenerationId)
        assertEquals(99, outcome.version)
    }

    @Test
    fun `a matching record and present content returns Retrieved with the record and payload, never re-extracted`() = runTest {
        val storedRecord = record()
        val storedEntry = contentEntry()
        val generationStorage = FakeDerivativeGenerationStorage { storedRecord }
        val contentStorage = FakeDerivativeContentStorage(onRetrieve = { storedEntry })
        val coordinator = TierAContentRetrievalCoordinator(generationStorage, contentStorage)

        val outcome = assertIs<TierAContentRetrievalOutcome.Retrieved>(coordinator.retrieve(knownEvidenceArtifactId, knownGenerationId))

        assertEquals(storedRecord, outcome.record)
        assertEquals(storedEntry.payload, outcome.payload)
    }

    @Test
    fun `the generation store is always queried by the caller-supplied generation identity, never the record's own`() = runTest {
        var queriedWith: DerivativeGenerationId? = null
        val generationStorage = FakeDerivativeGenerationStorage { id -> queriedWith = id; record() }
        val contentStorage = FakeDerivativeContentStorage(onRetrieve = { contentEntry() })
        TierAContentRetrievalCoordinator(generationStorage, contentStorage).retrieve(knownEvidenceArtifactId, knownGenerationId)

        assertEquals(knownGenerationId, queriedWith)
    }
}

private class FakeDerivativeGenerationStorage(
    private val onRetrieve: (DerivativeGenerationId) -> DerivativeGenerationRecord?,
) : DerivativeGenerationStorage {
    override suspend fun prepare(record: DerivativeGenerationRecord) = throw AssertionError("prepare must not be called")
    override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) = throw AssertionError("publishPrepared must not be called")
    override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): DerivativeGenerationRecord? = onRetrieve(derivativeGenerationId)
}

private class FakeDerivativeContentStorage(
    private val onRetrieve: (DerivativeGenerationId) -> DerivativeContentEntry?,
) : DerivativeContentStorage {
    override suspend fun prepare(entry: DerivativeContentEntry) = throw AssertionError("prepare must not be called")
    override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) = throw AssertionError("publishPrepared must not be called")
    override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): DerivativeContentEntry? = onRetrieve(derivativeGenerationId)
}
