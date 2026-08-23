package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADocumentIngestionRouter
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierADocumentSourceContext
import parker.core.interfaces.TierAMediaFacts
import parker.core.interfaces.TierAOwnerInvocationOutcome

/**
 * Document Ingestion, Owner-Facing Tier A Runtime Invocation Boundary.
 * Behavioural tests for [TierAOwnerInvocationCoordinator] -- the
 * governed chain manifest retrieval -> byte retrieval -> byte-length
 * verification -> SHA-256 verification -> exactly one router call,
 * governed by
 * `docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
 * Section 21. Precondition-failure and pass-through behaviour is proven
 * with hand-written fakes/spies; realistic admission behaviour is proven
 * against the immutable seven-fixture bake-off corpus through the real
 * [DefaultEvidenceCustodian] and the real, unchanged Tier A router
 * (`TierADocumentIngestionComposition.create`), never against real
 * evidence.
 */
class TierAOwnerInvocationCoordinatorTest {

    private val ownerPrincipalId = PrincipalId("owner-1")
    private val correlationValue = "correlation-1"
    private val evidenceArtifactId = EvidenceArtifactId("evidence-1")

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    private fun manifest(
        id: EvidenceArtifactId = evidenceArtifactId,
        content: ByteArray = "content".toByteArray(),
        receivedMediaType: String? = null,
        originalFileName: String? = null,
    ) = parker.core.interfaces.EvidenceSourceManifest(id, sha256Hex(content), content.size.toLong(), receivedMediaType, originalFileName)

    // --- Precondition failures: manifest retrieval ---

    @Test
    fun `a rejected manifest retrieval returns ManifestRetrievalRejected -- no byte retrieval, no router call`() = runTest {
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Rejected(id, "denied") },
            onRetrieve = { _, _ -> throw AssertionError("byte retrieval must not be reached") },
        )
        val router = SpyTierADocumentIngestionRouter()
        val coordinator = TierAOwnerInvocationCoordinator(custodian, router)

        val result = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        val rejected = assertIs<TierAOwnerInvocationOutcome.ManifestRetrievalRejected>(result)
        assertEquals(evidenceArtifactId, rejected.evidenceArtifactId)
        assertEquals("denied", rejected.reason)
        assertEquals(0, router.callCount)
    }

    @Test
    fun `a NotFound manifest retrieval returns ManifestNotFound -- no byte retrieval, no router call, never fabricated`() = runTest {
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.NotFound(id) },
            onRetrieve = { _, _ -> throw AssertionError("byte retrieval must not be reached") },
        )
        val router = SpyTierADocumentIngestionRouter()
        val coordinator = TierAOwnerInvocationCoordinator(custodian, router)

        val result = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        assertEquals(TierAOwnerInvocationOutcome.ManifestNotFound(evidenceArtifactId), result)
        assertEquals(0, router.callCount)
    }

    // --- Precondition failures: source-byte retrieval ---

    @Test
    fun `a rejected source-byte retrieval returns SourceRetrievalRejected -- no router call`() = runTest {
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(manifest(id)) },
            onRetrieve = { _, id -> EvidenceRetrievalResult.Rejected(id, "denied") },
        )
        val router = SpyTierADocumentIngestionRouter()
        val coordinator = TierAOwnerInvocationCoordinator(custodian, router)

        val result = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        val rejected = assertIs<TierAOwnerInvocationOutcome.SourceRetrievalRejected>(result)
        assertEquals("denied", rejected.reason)
        assertEquals(0, router.callCount)
    }

    @Test
    fun `a NotFound source-byte retrieval returns SourceNotFound -- no router call`() = runTest {
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(manifest(id)) },
            onRetrieve = { _, id -> EvidenceRetrievalResult.NotFound(id) },
        )
        val router = SpyTierADocumentIngestionRouter()
        val coordinator = TierAOwnerInvocationCoordinator(custodian, router)

        val result = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        assertEquals(TierAOwnerInvocationOutcome.SourceNotFound(evidenceArtifactId), result)
        assertEquals(0, router.callCount)
    }

    // --- Precondition failures: integrity verification ---

    @Test
    fun `a byte-length mismatch fails closed before the router is ever called`() = runTest {
        val realContent = "seven!!".toByteArray() // 7 bytes
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(manifest(id, content = realContent).copy(byteLength = 999L)) },
            onRetrieve = { _, id -> EvidenceRetrievalResult.Found(id, realContent) },
        )
        val router = SpyTierADocumentIngestionRouter()
        val coordinator = TierAOwnerInvocationCoordinator(custodian, router)

        val result = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        val mismatch = assertIs<TierAOwnerInvocationOutcome.ByteLengthMismatch>(result)
        assertEquals(999L, mismatch.expectedByteLength)
        assertEquals(7L, mismatch.actualByteLength)
        assertEquals(0, router.callCount)
    }

    @Test
    fun `a SHA-256 mismatch fails closed before the router is ever called -- no digest tautology`() = runTest {
        val realContent = "actual bytes".toByteArray()
        val bogusManifest = manifest(content = realContent).copy(sha256 = "0".repeat(64))
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(bogusManifest.copy(evidenceArtifactId = id)) },
            onRetrieve = { _, id -> EvidenceRetrievalResult.Found(id, realContent) },
        )
        val router = SpyTierADocumentIngestionRouter()
        val coordinator = TierAOwnerInvocationCoordinator(custodian, router)

        val result = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        val mismatch = assertIs<TierAOwnerInvocationOutcome.DigestMismatch>(result)
        assertEquals("0".repeat(64), mismatch.expectedSha256, "expected digest must be the manifest's own persisted value, never recomputed from the retrieved bytes")
        assertEquals(sha256Hex(realContent), mismatch.actualSha256)
        assertEquals(0, router.callCount)
    }

    // --- Identity, no divergence ---

    @Test
    fun `manifest and byte retrieval are both called with exactly the same requested identity`() = runTest {
        val calls = mutableListOf<EvidenceArtifactId>()
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> calls += id; EvidenceManifestRetrievalResult.Found(manifest(id)) },
            onRetrieve = { _, id -> calls += id; EvidenceRetrievalResult.Found(id, "content".toByteArray()) },
        )
        val coordinator = TierAOwnerInvocationCoordinator(custodian, SpyTierADocumentIngestionRouter())

        coordinator.invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        assertEquals(listOf(evidenceArtifactId, evidenceArtifactId), calls)
    }

    // --- Successful path: context construction and single router invocation ---

    @Test
    fun `on successful verification the router is invoked exactly once with a truthfully constructed context`() = runTest {
        val content = "verified content".toByteArray()
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(manifest(id, content, "text/csv", "ledger.csv")) },
            onRetrieve = { _, id -> EvidenceRetrievalResult.Found(id, content) },
        )
        val router = SpyTierADocumentIngestionRouter()
        val coordinator = TierAOwnerInvocationCoordinator(custodian, router)

        val result = coordinator.invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        assertEquals(1, router.callCount, "router must be invoked exactly once")
        val context = router.lastContext!!
        assertEquals(evidenceArtifactId, context.evidenceArtifactId)
        assertContentEquals(content, context.content)
        assertEquals(sha256Hex(content), context.expectedSha256)
        assertEquals("text/csv", context.receivedMediaType)
        assertEquals("ledger.csv", context.originalFileName)
        assertEquals(ownerPrincipalId, context.requestingPrincipalId)
        assertEquals(correlationValue, context.correlationValue)
        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(result)
        assertEquals(router.stubResult, routed.result)
    }

    @Test
    fun `absent received media type remains absent in the constructed context -- never fabricated`() = runTest {
        val content = "content".toByteArray()
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(manifest(id, content, receivedMediaType = null)) },
            onRetrieve = { _, id -> EvidenceRetrievalResult.Found(id, content) },
        )
        val router = SpyTierADocumentIngestionRouter()
        TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        assertNull(router.lastContext!!.receivedMediaType)
    }

    @Test
    fun `absent original filename remains absent in the constructed context`() = runTest {
        val content = "content".toByteArray()
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(manifest(id, content, originalFileName = null)) },
            onRetrieve = { _, id -> EvidenceRetrievalResult.Found(id, content) },
        )
        val router = SpyTierADocumentIngestionRouter()
        TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        assertNull(router.lastContext!!.originalFileName)
    }

    @Test
    fun `a hostile path-traversal-shaped filename is passed through as opaque metadata, never interpreted`() = runTest {
        val content = "content".toByteArray()
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(manifest(id, content, originalFileName = "../../etc/passwd")) },
            onRetrieve = { _, id -> EvidenceRetrievalResult.Found(id, content) },
        )
        val router = SpyTierADocumentIngestionRouter()
        TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        assertEquals("../../etc/passwd", router.lastContext!!.originalFileName)
    }

    @Test
    fun `source bytes reaching the router are byte-identical to what was retrieved -- no mutation`() = runTest {
        val content = "immutable source bytes".toByteArray()
        val original = content.copyOf()
        val custodian = FakeEvidenceCustodianForTierA(
            onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(manifest(id, content)) },
            onRetrieve = { _, id -> EvidenceRetrievalResult.Found(id, content) },
        )
        val router = SpyTierADocumentIngestionRouter()
        TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)

        assertContentEquals(original, content)
        assertContentEquals(original, router.lastContext!!.content)
    }

    // --- Router result variants are preserved unchanged, never collapsed ---

    @Test
    fun `every router outcome variant is preserved unchanged inside Routed, never collapsed or reinterpreted`() = runTest {
        val facts = TierAMediaFacts(null, null, null, false)
        val variants = listOf<TierADocumentRoutingResult>(
            TierADocumentRoutingResult.RequiresTierB("scanned", facts),
            TierADocumentRoutingResult.Unsupported("no route", facts),
            TierADocumentRoutingResult.ExtractionFailed(parker.core.interfaces.TierADocumentFormat.CSV, "malformed", facts),
        )
        variants.forEach { variant ->
            val content = "content".toByteArray()
            val custodian = FakeEvidenceCustodianForTierA(
                onRetrieveManifest = { _, id -> EvidenceManifestRetrievalResult.Found(manifest(id, content)) },
                onRetrieve = { _, id -> EvidenceRetrievalResult.Found(id, content) },
            )
            val router = SpyTierADocumentIngestionRouter(stubResult = variant)
            val result = TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, evidenceArtifactId, correlationValue)
            assertEquals(variant, assertIs<TierAOwnerInvocationOutcome.Routed>(result).result)
        }
    }

    // --- Structural dependency shape: no Memory Core, no Evidence Intelligence, no OCR, no Tier B ---

    @Test
    fun `TierAOwnerInvocationCoordinator declares exactly two constructor dependencies -- EvidenceCustodian and the Tier A router only`() {
        val constructor = TierAOwnerInvocationCoordinator::class.primaryConstructor!!
        val parameterTypes = constructor.parameters.map { it.type.classifier }

        assertEquals(
            setOf(EvidenceCustodian::class, TierADocumentIngestionRouter::class),
            parameterTypes.toSet(),
            "TierAOwnerInvocationCoordinator must depend on exactly EvidenceCustodian and " +
                "TierADocumentIngestionRouter -- no MemoryCore, KnowledgeStore, EvidenceIntelligence, " +
                "OcrMechanism, or PermissionEngine reference of its own",
        )
        assertEquals(2, parameterTypes.size)
    }

    // --- Realistic end-to-end admission, against the immutable bake-off fixtures ---

    @Test
    fun `authorized owner invocation of a searchable PDF fixture is admitted end to end`() = runTest {
        val (custodian, id) = acceptedFixture("01-searchable-simple.pdf", "application/pdf")
        val router = TierADocumentIngestionComposition.create(FileSystemDerivativeGenerationStorage(tempDir()), FileSystemDocumentIngestionAudit(tempDir().resolve("audit.log")))

        val result = TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, id, correlationValue)

        val admitted = assertIs<TierADocumentRoutingResult.Admitted>(assertIs<TierAOwnerInvocationOutcome.Routed>(result).result)
        assertEquals(parker.core.interfaces.TierADocumentFormat.PDF, admitted.format)
    }

    @Test
    fun `authorized owner invocation of a CSV fixture with authoritative text-csv is admitted end to end`() = runTest {
        val (custodian, id) = acceptedFixture("06-structured.csv", "text/csv")
        val router = TierADocumentIngestionComposition.create(FileSystemDerivativeGenerationStorage(tempDir()), FileSystemDocumentIngestionAudit(tempDir().resolve("audit.log")))

        val result = TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, id, correlationValue)

        val admitted = assertIs<TierADocumentRoutingResult.Admitted>(assertIs<TierAOwnerInvocationOutcome.Routed>(result).result)
        assertEquals(parker.core.interfaces.TierADocumentFormat.CSV, admitted.format)
    }

    @Test
    fun `authorized owner invocation of an EML fixture is admitted end to end`() = runTest {
        val (custodian, id) = acceptedFixture("05-email-with-attachment.eml", "message/rfc822")
        val router = TierADocumentIngestionComposition.create(FileSystemDerivativeGenerationStorage(tempDir()), FileSystemDocumentIngestionAudit(tempDir().resolve("audit.log")))

        val result = TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, id, correlationValue)

        val admitted = assertIs<TierADocumentRoutingResult.Admitted>(assertIs<TierAOwnerInvocationOutcome.Routed>(result).result)
        assertEquals(parker.core.interfaces.TierADocumentFormat.EML, admitted.format)
    }

    @Test
    fun `authorized owner invocation of a DOCX fixture is admitted end to end`() = runTest {
        val (custodian, id) = acceptedFixture("04-structured.docx", GovernedTierADocumentIngestionRouter.DOCX)
        val router = TierADocumentIngestionComposition.create(FileSystemDerivativeGenerationStorage(tempDir()), FileSystemDocumentIngestionAudit(tempDir().resolve("audit.log")))

        val result = TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, id, correlationValue)

        val admitted = assertIs<TierADocumentRoutingResult.Admitted>(assertIs<TierAOwnerInvocationOutcome.Routed>(result).result)
        assertEquals(parker.core.interfaces.TierADocumentFormat.DOCX, admitted.format)
    }

    @Test
    fun `a scanned PDF fixture requires Tier B -- no OCR is invoked`() = runTest {
        val (custodian, id) = acceptedFixture("03-scanned.pdf", "application/pdf")
        val router = TierADocumentIngestionComposition.create(FileSystemDerivativeGenerationStorage(tempDir()), FileSystemDocumentIngestionAudit(tempDir().resolve("audit.log")))

        val result = TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, id, correlationValue)

        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(result).result)
    }

    @Test
    fun `a PNG fixture requires Tier B -- no OCR is invoked`() = runTest {
        val (custodian, id) = acceptedFixture("07-text-image.png", "image/png")
        val router = TierADocumentIngestionComposition.create(FileSystemDerivativeGenerationStorage(tempDir()), FileSystemDocumentIngestionAudit(tempDir().resolve("audit.log")))

        val result = TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, id, correlationValue)

        assertIs<TierADocumentRoutingResult.RequiresTierB>(assertIs<TierAOwnerInvocationOutcome.Routed>(result).result)
    }

    @Test
    fun `an unsupported binary is reported Unsupported`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val manifestStorage = InMemoryEvidenceSourceManifestStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingPermissionEngine(), manifestStorage)
        val bytes = byteArrayOf(1, 2, 3, 4)
        val accepted = assertIs<EvidenceAcceptanceResult.Accepted>(
            custodian.accept(ownerPrincipalId, CandidateEvidenceArtifact(bytes, "application/octet-stream", "x.bin")),
        )
        val router = TierADocumentIngestionComposition.create(FileSystemDerivativeGenerationStorage(tempDir()), FileSystemDocumentIngestionAudit(tempDir().resolve("audit.log")))

        val result = TierAOwnerInvocationCoordinator(custodian, router).invoke(ownerPrincipalId, accepted.acceptedEvidenceArtifact.evidenceArtifactId, correlationValue)

        assertIs<TierADocumentRoutingResult.Unsupported>(assertIs<TierAOwnerInvocationOutcome.Routed>(result).result)
    }

    @Test
    fun `reprocessing the same authoritative source produces a distinct generation and preserves the prior one`() = runTest {
        val (custodian, id) = acceptedFixture("06-structured.csv", "text/csv")
        val storage = FileSystemDerivativeGenerationStorage(tempDir())
        val router = TierADocumentIngestionComposition.create(storage, FileSystemDocumentIngestionAudit(tempDir().resolve("audit.log")))
        val coordinator = TierAOwnerInvocationCoordinator(custodian, router)

        val first = assertIs<TierADocumentRoutingResult.Admitted>(assertIs<TierAOwnerInvocationOutcome.Routed>(coordinator.invoke(ownerPrincipalId, id, "correlation-first")).result)
        val second = assertIs<TierADocumentRoutingResult.Admitted>(assertIs<TierAOwnerInvocationOutcome.Routed>(coordinator.invoke(ownerPrincipalId, id, "correlation-second")).result)

        assertNotEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        assertTrue(storage.retrieve(first.record.derivativeGenerationId) != null, "the prior generation must remain retrievable, never overwritten")
        assertTrue(storage.retrieve(second.record.derivativeGenerationId) != null)
    }

    // --- Test helpers ---

    private fun tempDir(): Path = Files.createTempDirectory("tier-a-owner-invocation-test")

    private suspend fun acceptedFixture(fileName: String, receivedMediaType: String): Pair<EvidenceCustodian, EvidenceArtifactId> {
        val storage = InMemoryEvidenceArtifactStorage()
        val manifestStorage = InMemoryEvidenceSourceManifestStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingPermissionEngine(), manifestStorage)
        val bytes = Files.readAllBytes(FIXTURE_ROOT.resolve(fileName))
        val accepted = assertIs<EvidenceAcceptanceResult.Accepted>(
            custodian.accept(ownerPrincipalId, CandidateEvidenceArtifact(bytes, receivedMediaType, fileName)),
        )
        return custodian to accepted.acceptedEvidenceArtifact.evidenceArtifactId
    }

    private companion object {
        val FIXTURE_ROOT: Path = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures")
    }
}

/** Test-only fake mirroring [FakeEvidenceArtifactStorage]'s own configurable-lambda shape, adapted for the full [EvidenceCustodian] surface this coordinator depends on. */
private class FakeEvidenceCustodianForTierA(
    private val onRetrieveManifest: suspend (PrincipalId, EvidenceArtifactId) -> EvidenceManifestRetrievalResult,
    private val onRetrieve: suspend (PrincipalId, EvidenceArtifactId) -> EvidenceRetrievalResult,
) : EvidenceCustodian {
    override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) =
        throw AssertionError("accept must not be called by TierAOwnerInvocationCoordinator")

    override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
        onRetrieve(requestingPrincipalId, evidenceArtifactId)

    override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
        onRetrieveManifest(requestingPrincipalId, evidenceArtifactId)
}

/** Records call count and the last [TierADocumentSourceContext] it was invoked with; returns a caller-configured stub result. */
private class SpyTierADocumentIngestionRouter(
    val stubResult: TierADocumentRoutingResult = TierADocumentRoutingResult.Unsupported("stub", TierAMediaFacts(null, null, null, false)),
) : TierADocumentIngestionRouter {
    var callCount = 0
        private set
    var lastContext: TierADocumentSourceContext? = null
        private set

    override suspend fun ingest(source: TierADocumentSourceContext): TierADocumentRoutingResult {
        callCount++
        lastContext = source
        return stubResult
    }
}

/** Builds an approving [FakePermissionEngine] -- for tests exercising real [DefaultEvidenceCustodian]/router wiring, not permission logic itself. */
private fun approvingPermissionEngine() = FakePermissionEngine { request ->
    parker.core.interfaces.PermissionDecision(
        decisionId = parker.core.interfaces.DecisionId("decision-${request.requestId.value}"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = parker.core.interfaces.PermissionAction.READ,
        decision = parker.core.interfaces.PermissionDecisionOutcome.APPROVED,
        level = parker.core.interfaces.PermissionLevel.AUTOMATIC,
        timestamp = java.time.Instant.now(),
    )
}
