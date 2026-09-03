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
import parker.core.interfaces.*

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

    @Test
    fun `region retrieval derives the exact six-part target after content retrieval and preserves provider text`() = runTest {
        val payload = regionPayload()
        val storedRecord = record().copy(contentIdentity = DerivativeContentIdentity.Digest("SHA-256", "1".repeat(64)))
        val storedEntry = DerivativeContentEntry(knownGenerationId, knownEvidenceArtifactId, TierADerivativePayload.RegionTranscription(payload))
        var projectedTarget: HumanFidelityReviewTarget? = null
        val projector = EffectiveHumanFidelityReviewProjector { target, purpose ->
            projectedTarget = target
            assertEquals(HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION, purpose)
            EffectiveHumanFidelityReviewProjectionOutcome.Projected(EffectiveHumanFidelityReviewSummary(
                purpose,
                EffectiveHumanFidelityReviewProjection(
                    target, HumanFidelityReviewState.UNREVIEWED, null, emptySet(), emptySet(),
                    SourceConfirmedEligibility(SourceConfirmedEligibilityState.DENIED, SourceConfirmedDenialReason.UNREVIEWED),
                ),
                0, 0, false,
            ))
        }

        val outcome = assertIs<TierAContentRetrievalOutcome.Retrieved>(
            TierAContentRetrievalCoordinator(
                FakeDerivativeGenerationStorage { storedRecord },
                FakeDerivativeContentStorage { storedEntry },
                projector,
            ).retrieve(knownEvidenceArtifactId, knownGenerationId),
        )

        assertEquals(payload.transcriptionBlocks, (outcome.payload as TierADerivativePayload.RegionTranscription).value.transcriptionBlocks)
        assertEquals(knownEvidenceArtifactId, projectedTarget?.evidenceArtifactId)
        assertEquals(payload.sourceSha256, projectedTarget?.sourceSha256?.value)
        assertEquals(payload.preparationIdentity, projectedTarget?.preparationIdentity?.value)
        assertEquals(knownGenerationId, projectedTarget?.derivativeGenerationId)
        assertEquals(sha256(DerivativeGenerationRecordCodec.encode(storedRecord)), projectedTarget?.derivativeGenerationSha256?.value)
        assertEquals(sha256(DerivativeContentCodec.encode(storedEntry)), projectedTarget?.derivativeContentSha256?.value)
    }

    private fun regionPayload() = OrdinaryRegionTranscriptionDerivative(
        representationVersion = 3, capabilityId = ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID,
        capabilityDigest = "2".repeat(64), evidenceArtifactId = knownEvidenceArtifactId.value,
        sourceSha256 = "3".repeat(64), pageBindings = listOf("page-1"), regionBindings = listOf("region-1"),
        transcriptionBlocks = listOf("Michael Gary Kellee"), providerReturnedOrder = listOf("region-1"),
        parkerSourceOrder = listOf("region-1"), provider = "OpenAI", model = "gpt-5.6-sol",
        adapterId = "adapter", adapterVersion = "1", providerProfile = "openai-fidelity-first-transcription-v1", wireVersion = 8,
        schemaSha256 = "4".repeat(64), instructionSha256 = "5".repeat(64), processingProfile = "processing",
        requestIdentity = "request", requestDigest = "6".repeat(64), responseIdentity = "response",
        providerStateRecordIdentity = "provider-state", capabilityAcceptanceRecordIdentity = "acceptance",
        ownerAuthorizationIdentity = "authorization", executionIdentity = "execution", attemptIdentity = "attempt",
        reconstructedContentDigest = "7".repeat(64), canonicalGenerationKeyDigest = "8".repeat(64),
        admissionProvenance = "preserved", preparationIdentity = "9".repeat(64), preparationProfile = "full-page-achromatic-png-preparation-v1",
        preparationProfileVersion = 1, providerBodyDigest = "a".repeat(64), authorizationPurpose = "evidence-intelligence.external-transcription",
        maximumProviderCalls = 1, automaticRetryLimit = 0, externalReasoningAuthorized = false,
    )

    private fun sha256(bytes: ByteArray) = java.security.MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }
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
