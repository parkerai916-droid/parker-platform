package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AcceptedEvidenceArtifact
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactStorage
import parker.core.interfaces.EvidenceArtifactStorageException
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Evidence Custodian, Implementation Plan Phase 3 (Unit 2) and Phase 4
 * (Unit 3). Behavioural tests of [DefaultEvidenceCustodian] -- the
 * orchestration between [parker.core.interfaces.PermissionEngine] and
 * [EvidenceArtifactStorage], exercised with [FakePermissionEngine] (already
 * established by [DefaultExecutionPipelineTest]) and
 * [InMemoryEvidenceArtifactStorage] (Unit 1). Deliberately does not re-prove
 * anything [InMemoryEvidenceArtifactStorageTest] / [FileSystemEvidenceArtifactStorageTest]
 * already prove about storage's own behaviour (duplicate rejection,
 * identifier safety, exact-byte round trip) -- this suite is about what
 * [DefaultEvidenceCustodian] itself adds: for `accept` (Unit 2), the
 * permission gate, identifier minting, and the candidate/accepted
 * separation; for `retrieve` (Unit 3, added below), the permission gate
 * occurring strictly before any storage read, the explicit
 * Found/NotFound/Rejected distinction, storage-failure propagation, and
 * the observational-only guarantees (no write, no minting, no identifier
 * substitution).
 */
class DefaultEvidenceCustodianTest {

    private val principalId = PrincipalId("principal-1")

    private fun approvingEngine(outcome: PermissionDecisionOutcome = PermissionDecisionOutcome.APPROVED) =
        FakePermissionEngine { request ->
            decision(request, outcome)
        }

    private fun decision(request: ExecutionRequest, outcome: PermissionDecisionOutcome) = PermissionDecision(
        decisionId = DecisionId("decision-1"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = PermissionAction.WRITE,
        decision = outcome,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.now(),
    )

    // --- Successful acceptance ---

    @Test
    fun `an APPROVED decision results in Accepted with the content durably persisted`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.APPROVED))
        val content = "evidence bytes".toByteArray()

        val result = custodian.accept(principalId, CandidateEvidenceArtifact(content))

        val accepted = assertIs<EvidenceAcceptanceResult.Accepted>(result)
        assertContentEquals(content, storage.read(accepted.acceptedEvidenceArtifact.evidenceArtifactId))
    }

    @Test
    fun `an APPROVED_WITH_CONFIRMATION decision also results in Accepted`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(
            storage,
            approvingEngine(PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION),
        )

        val result = custodian.accept(principalId, CandidateEvidenceArtifact("content".toByteArray()))

        assertIs<EvidenceAcceptanceResult.Accepted>(result)
    }

    // --- Rejected acceptance ---

    @Test
    fun `a DENIED decision is Rejected, and nothing is written to storage`() = runTest {
        val storage = FakeEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.DENIED))

        val result = custodian.accept(principalId, CandidateEvidenceArtifact("content".toByteArray()))

        assertIs<EvidenceAcceptanceResult.Rejected>(result)
        assertEquals(0, storage.writeCallCount, "a denied decision must never reach storage.write")
    }

    @Test
    fun `a DEFERRED decision is Rejected, not treated as an approval`() = runTest {
        val storage = FakeEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.DEFERRED))

        val result = custodian.accept(principalId, CandidateEvidenceArtifact("content".toByteArray()))

        assertIs<EvidenceAcceptanceResult.Rejected>(result)
        assertEquals(0, storage.writeCallCount, "a deferred decision must never reach storage.write")
    }

    @Test
    fun `a Rejected result carries a non-blank, informative reason`() = runTest {
        val storage = FakeEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.DENIED))

        val result = assertIs<EvidenceAcceptanceResult.Rejected>(
            custodian.accept(principalId, CandidateEvidenceArtifact("content".toByteArray())),
        )

        assertTrue(result.reason.isNotBlank())
        assertTrue(result.reason.contains(principalId.value), "reason should name the requesting principal")
    }

    // --- Storage failure propagation ---

    @Test
    fun `a storage failure propagates as a thrown exception, not as Rejected`() = runTest {
        val storage = FakeEvidenceArtifactStorage(
            writeBehavior = { _, _ ->
                throw EvidenceArtifactStorageException.StorageIOFailure(
                    "simulated I/O failure",
                    RuntimeException("disk error"),
                )
            },
        )
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.APPROVED))

        assertFailsWith<EvidenceArtifactStorageException.StorageIOFailure> {
            custodian.accept(principalId, CandidateEvidenceArtifact("content".toByteArray()))
        }
    }

    @Test
    fun `an unexpected DuplicateIdentifier from storage propagates unchanged, is not swallowed or reinterpreted`() =
        runTest {
            val storage = FakeEvidenceArtifactStorage(
                writeBehavior = { evidenceArtifactId, _ ->
                    throw EvidenceArtifactStorageException.DuplicateIdentifier(evidenceArtifactId)
                },
            )
            val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.APPROVED))

            assertFailsWith<EvidenceArtifactStorageException.DuplicateIdentifier> {
                custodian.accept(principalId, CandidateEvidenceArtifact("content".toByteArray()))
            }
        }

    // --- Identifier allocation ---

    @Test
    fun `each accepted call mints its own distinct, canonical-form EvidenceArtifactId`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine())

        val first = assertIs<EvidenceAcceptanceResult.Accepted>(
            custodian.accept(principalId, CandidateEvidenceArtifact("first".toByteArray())),
        )
        val second = assertIs<EvidenceAcceptanceResult.Accepted>(
            custodian.accept(principalId, CandidateEvidenceArtifact("second".toByteArray())),
        )

        val firstId = first.acceptedEvidenceArtifact.evidenceArtifactId
        val secondId = second.acceptedEvidenceArtifact.evidenceArtifactId
        assertNotEquals(firstId, secondId, "two separate acceptances must never mint the same identifier")

        // Both identifiers must satisfy storage's own canonical (lowercase-only) safety rule --
        // this call would throw UnsafeIdentifier if it did not.
        storage.read(firstId)
        storage.read(secondId)
    }

    @Test
    fun `a rejected acceptance never allocates an identifier that ends up readable in storage`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.DENIED))

        val result = custodian.accept(principalId, CandidateEvidenceArtifact("content".toByteArray()))

        assertIs<EvidenceAcceptanceResult.Rejected>(result)
        // No identifier was ever surfaced to correlate against -- this is exactly the point:
        // a caller who receives Rejected has no EvidenceArtifactId at all, minted or otherwise.
    }

    // --- Immutability expectations ---

    @Test
    fun `accepting identical content twice produces two independent accepted records, never an update`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine())
        val content = "same bytes".toByteArray()

        val first = assertIs<EvidenceAcceptanceResult.Accepted>(
            custodian.accept(principalId, CandidateEvidenceArtifact(content)),
        )
        val second = assertIs<EvidenceAcceptanceResult.Accepted>(
            custodian.accept(principalId, CandidateEvidenceArtifact(content)),
        )

        assertNotEquals(
            first.acceptedEvidenceArtifact.evidenceArtifactId,
            second.acceptedEvidenceArtifact.evidenceArtifactId,
            "identical content submitted twice must yield two distinct accepted artefacts, not one updated in place",
        )
        assertContentEquals(content, storage.read(first.acceptedEvidenceArtifact.evidenceArtifactId))
        assertContentEquals(content, storage.read(second.acceptedEvidenceArtifact.evidenceArtifactId))
    }

    @Test
    fun `AcceptedEvidenceArtifact exposes no mutator -- both fields are val-only`() {
        val properties = AcceptedEvidenceArtifact::class.java.declaredFields.filter { !it.isSynthetic }
        properties.forEach { field ->
            assertTrue(
                java.lang.reflect.Modifier.isFinal(field.modifiers),
                "AcceptedEvidenceArtifact.${field.name} must be immutable (final)",
            )
        }
    }

    @Test
    fun `CandidateEvidenceArtifact exposes no mutator -- content is val-only`() {
        val contentField = CandidateEvidenceArtifact::class.java.getDeclaredField("content")
        assertTrue(
            java.lang.reflect.Modifier.isFinal(contentField.modifiers),
            "CandidateEvidenceArtifact.content must be immutable (final)",
        )
    }

    // --- Separation between candidate and accepted evidence ---

    @Test
    fun `CandidateEvidenceArtifact carries no identity or acceptance timestamp field`() {
        val fieldNames = CandidateEvidenceArtifact::class.java.declaredFields
            .filter { !it.isSynthetic }
            .map { it.name }

        assertEquals(listOf("content"), fieldNames)
    }

    @Test
    fun `AcceptedEvidenceArtifact carries no content field`() {
        val fieldNames = AcceptedEvidenceArtifact::class.java.declaredFields
            .filter { !it.isSynthetic }
            .map { it.name }
            .toSet()

        assertEquals(setOf("evidenceArtifactId", "acceptedAt"), fieldNames)
    }

    @Test
    fun `two CandidateEvidenceArtifact instances with identical content are equal, with distinct content are not`() {
        val a = CandidateEvidenceArtifact("same".toByteArray())
        val b = CandidateEvidenceArtifact("same".toByteArray())
        val c = CandidateEvidenceArtifact("different".toByteArray())

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
        assertNotEquals(a, c)
    }

    @Test
    fun `accept requires an actual Permission Engine call -- it is not bypassed`() = runTest {
        val engine = approvingEngine()
        val storage = InMemoryEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, engine)

        custodian.accept(principalId, CandidateEvidenceArtifact("content".toByteArray()))

        assertEquals(1, engine.evaluateCallCount)
    }

    // --- Retrieval: approved cases ---

    @Test
    fun `an APPROVED retrieval returns the exact original bytes as Found`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val evidenceArtifactId = EvidenceArtifactId("retrieval-artifact-1")
        val content = "original evidence bytes".toByteArray()
        storage.write(evidenceArtifactId, content)
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.APPROVED))

        val result = custodian.retrieve(principalId, evidenceArtifactId)

        val found = assertIs<EvidenceRetrievalResult.Found>(result)
        assertEquals(evidenceArtifactId, found.evidenceArtifactId)
        assertContentEquals(content, found.content)
    }

    @Test
    fun `an APPROVED_WITH_CONFIRMATION retrieval is also allowed, consistent with Unit 2's acceptance behaviour`() =
        runTest {
            val storage = InMemoryEvidenceArtifactStorage()
            val evidenceArtifactId = EvidenceArtifactId("retrieval-artifact-2")
            storage.write(evidenceArtifactId, "content".toByteArray())
            val custodian = DefaultEvidenceCustodian(
                storage,
                approvingEngine(PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION),
            )

            val result = custodian.retrieve(principalId, evidenceArtifactId)

            assertIs<EvidenceRetrievalResult.Found>(result)
        }

    // --- Retrieval: rejected cases perform no storage read ---

    @Test
    fun `a DENIED retrieval decision is Rejected, and performs no storage read`() = runTest {
        val storage = FakeEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.DENIED))

        val result = custodian.retrieve(principalId, EvidenceArtifactId("some-artifact"))

        assertIs<EvidenceRetrievalResult.Rejected>(result)
        assertEquals(0, storage.readCallCount, "a denied retrieval must never reach storage.read")
    }

    @Test
    fun `a DEFERRED retrieval decision is Rejected, and performs no storage read`() = runTest {
        val storage = FakeEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.DEFERRED))

        val result = custodian.retrieve(principalId, EvidenceArtifactId("some-artifact"))

        assertIs<EvidenceRetrievalResult.Rejected>(result)
        assertEquals(0, storage.readCallCount, "a deferred retrieval must never reach storage.read")
    }

    @Test
    fun `a Rejected retrieval result carries a non-blank, informative reason`() = runTest {
        val storage = FakeEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.DENIED))

        val result = assertIs<EvidenceRetrievalResult.Rejected>(
            custodian.retrieve(principalId, EvidenceArtifactId("some-artifact")),
        )

        assertTrue(result.reason.isNotBlank())
        assertTrue(result.reason.contains(principalId.value), "reason should name the requesting principal")
    }

    // --- Retrieval: missing artefact ---

    @Test
    fun `an authorised retrieval for an identifier nothing was ever written under returns NotFound`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val evidenceArtifactId = EvidenceArtifactId("never-written")
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.APPROVED))

        val result = custodian.retrieve(principalId, evidenceArtifactId)

        val notFound = assertIs<EvidenceRetrievalResult.NotFound>(result)
        assertEquals(evidenceArtifactId, notFound.evidenceArtifactId)
    }

    // --- Retrieval: storage failure propagation ---

    @Test
    fun `a genuine storage failure on read propagates unchanged, is never reported as NotFound`() = runTest {
        val storage = FakeEvidenceArtifactStorage(
            readBehavior = { _ ->
                throw EvidenceArtifactStorageException.StorageIOFailure(
                    "simulated read I/O failure",
                    RuntimeException("disk error"),
                )
            },
        )
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine(PermissionDecisionOutcome.APPROVED))

        assertFailsWith<EvidenceArtifactStorageException.StorageIOFailure> {
            custodian.retrieve(principalId, EvidenceArtifactId("some-artifact"))
        }
    }

    // --- Retrieval: byte immutability ---

    @Test
    fun `mutating Found's returned bytes does not affect a later retrieval of the same artefact`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val evidenceArtifactId = EvidenceArtifactId("retrieval-artifact-mutation")
        val original = "original".toByteArray()
        storage.write(evidenceArtifactId, original)
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine())

        val first = assertIs<EvidenceRetrievalResult.Found>(custodian.retrieve(principalId, evidenceArtifactId))
        first.content[0] = 'X'.code.toByte()

        val second = assertIs<EvidenceRetrievalResult.Found>(custodian.retrieve(principalId, evidenceArtifactId))
        assertContentEquals(original, second.content)
    }

    // --- Retrieval: observational-only guarantees ---

    @Test
    fun `retrieving never calls storage write`() = runTest {
        val storage = FakeEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine())

        custodian.retrieve(principalId, EvidenceArtifactId("some-artifact"))

        assertEquals(0, storage.writeCallCount, "retrieve must never call storage.write")
    }

    @Test
    fun `retrieving does not mint a new identifier -- the returned identifier equals the requested one`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val requestedId = EvidenceArtifactId("caller-supplied-identifier")
        storage.write(requestedId, "content".toByteArray())
        val custodian = DefaultEvidenceCustodian(storage, approvingEngine())

        val result = assertIs<EvidenceRetrievalResult.Found>(custodian.retrieve(principalId, requestedId))

        assertEquals(requestedId, result.evidenceArtifactId, "retrieve must echo back the exact identifier requested")
    }

    @Test
    fun `retrieve requires an actual Permission Engine call -- it is not bypassed`() = runTest {
        val engine = approvingEngine()
        val storage = InMemoryEvidenceArtifactStorage()
        val custodian = DefaultEvidenceCustodian(storage, engine)

        custodian.retrieve(principalId, EvidenceArtifactId("some-artifact"))

        assertEquals(1, engine.evaluateCallCount)
    }

    @Test
    fun `the permission gate occurs before the storage read -- a rejected request never touches storage`() = runTest {
        val callOrder = mutableListOf<String>()
        val storage = FakeEvidenceArtifactStorage(
            readBehavior = {
                callOrder.add("storage.read")
                null
            },
        )
        val engine = FakePermissionEngine { request ->
            callOrder.add("permissionEngine.evaluate")
            decision(request, PermissionDecisionOutcome.DENIED)
        }
        val custodian = DefaultEvidenceCustodian(storage, engine)

        custodian.retrieve(principalId, EvidenceArtifactId("some-artifact"))

        assertEquals(listOf("permissionEngine.evaluate"), callOrder, "storage must never be reached on denial")
    }
}

/**
 * Test-only fake, mirroring [FakePermissionEngine]'s own lambda-configurable
 * shape. Delegates to a real [InMemoryEvidenceArtifactStorage] by default --
 * so tests that only care about call counting or read-back still get real
 * storage semantics -- but allows [write] and, as of Unit 3, [read] to be
 * overridden to simulate a storage-layer fault without needing to
 * reverse-engineer [FileSystemEvidenceArtifactStorage]'s own real failure
 * conditions (permission bits, disk state) just to prove
 * [DefaultEvidenceCustodian] propagates them correctly. [readCallCount],
 * added alongside [readBehavior] for Unit 3, lets retrieval tests prove a
 * denied/deferred decision never reaches storage at all -- the same
 * "no-interaction" proof [writeCallCount] already gave Unit 2's own tests
 * for acceptance.
 */
class FakeEvidenceArtifactStorage(
    private val writeBehavior: (suspend (EvidenceArtifactId, ByteArray) -> Unit)? = null,
    private val readBehavior: (suspend (EvidenceArtifactId) -> ByteArray?)? = null,
) : EvidenceArtifactStorage {

    private val delegate = InMemoryEvidenceArtifactStorage()

    var writeCallCount: Int = 0
        private set

    var readCallCount: Int = 0
        private set

    override suspend fun write(evidenceArtifactId: EvidenceArtifactId, content: ByteArray) {
        writeCallCount++
        if (writeBehavior != null) {
            writeBehavior.invoke(evidenceArtifactId, content)
        } else {
            delegate.write(evidenceArtifactId, content)
        }
    }

    override suspend fun read(evidenceArtifactId: EvidenceArtifactId): ByteArray? {
        readCallCount++
        return if (readBehavior != null) readBehavior.invoke(evidenceArtifactId) else delegate.read(evidenceArtifactId)
    }
}
