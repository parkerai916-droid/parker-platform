package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DocumentId
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.runtime.DurableMemoryCore
import parker.core.runtime.EvidenceIntelligenceAcceptanceCoordinator
import parker.core.runtime.EvidenceRegistrationCoordinator
import parker.core.runtime.EvidenceRegistrationOutcome
import parker.core.runtime.FileSystemMemoryCoreDurabilityLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Memory Core Durability, Implementation Unit 8 ("Runtime Composition").
 * Proves the composition this Unit adds -- `FileSystemMemoryCoreDurabilityLog`
 * -> `DurableMemoryCore.create` -> shared by `EvidenceRegistrationCoordinator`,
 * `EvidenceIntelligenceAcceptanceCoordinator`, and the one, shared
 * `PermissionFilteredMemoryRetrieval` -- not Units 1-7's own already-verified
 * internal behaviour (durability-log atomicity, recovery replay, identifier
 * restoration, write ordering), each already covered by its own dedicated
 * unit test suite under `tests/runtime`.
 *
 * Reflection is used only where no public seam exists to observe shared
 * instance identity across the composed graph, mirroring
 * [ParkerRuntimeEvidenceIntelligenceCompositionTest]'s own established
 * precedent for this exact reason.
 */
class ParkerRuntimeMemoryCoreDurabilityCompositionTest {

    private val ownerPrincipalId = "user.owner-memory-core-durability-composition-test"

    private fun config(memoryCoreDurabilityLogPath: String? = null) = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by this suite
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-memory-core-durability-composition-test",
        evidenceStorageRootPath = Files.createTempDirectory("memory-core-durability-composition-evidence-storage").toString(),
        evidenceDeletionAuditLogPath =
            Files.createTempDirectory("memory-core-durability-composition-evidence-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = memoryCoreDurabilityLogPath
            ?: Files.createTempDirectory("memory-core-durability-composition-memory").resolve("memory-core.log").toString(),
    )

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "memory-core-durability-composition-test-source",
        sourceType = "test",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    // ================= Composition: one DurableMemoryCore, no double gating =================

    @Test
    fun `EvidenceRegistrationCoordinator and EvidenceIntelligenceAcceptanceCoordinator receive the same raw DurableMemoryCore instance`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registrationCoordinator = runtime.privateField<EvidenceRegistrationCoordinator>("evidenceRegistrationCoordinator")
        val registrationMemoryCore = registrationCoordinator.privateField<MemoryCore>("memoryCore")

        val acceptanceCoordinator = runtime.privateField<EvidenceIntelligenceAcceptanceCoordinator>("evidenceIntelligenceAcceptanceCoordinator")
        val acceptanceMemoryCore = acceptanceCoordinator.privateField<MemoryCore>("memoryCore")

        assertIs<DurableMemoryCore>(registrationMemoryCore, "the raw InMemoryMemoryCore must never reach a caller directly once Unit 8 composes DurableMemoryCore")
        assertSame(registrationMemoryCore, acceptanceMemoryCore, "one Memory Core, never a parallel one -- both self-gating coordinators must share the identical durable instance")

        runtime.shutdown()
    }

    @Test
    fun `neither self-gating coordinator's raw MemoryCore is a PermissionGatedMemoryCore -- no double gating`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registrationCoordinator = runtime.privateField<EvidenceRegistrationCoordinator>("evidenceRegistrationCoordinator")
        val registrationMemoryCore = registrationCoordinator.privateField<MemoryCore>("memoryCore")

        // DurableMemoryCore itself, not a further PermissionGatedMemoryCore wrapper around it --
        // Memory Core Durability Unit 8's own disclosed Planning Review finding, reaffirming the
        // Implementation Plan's own Unit 8 text, Scope Lock Section 18's binding "SHALL NOT
        // introduce double permission gating on any write path already gated internally by its
        // own caller," and this composition root's own pre-existing inline comments.
        assertIs<DurableMemoryCore>(registrationMemoryCore)

        runtime.shutdown()
    }

    @Test
    fun `the shared PermissionFilteredMemoryRetrieval wraps the same DurableMemoryCore instance the self-gating coordinators hold, never the raw recovered delegate`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registrationCoordinator = runtime.privateField<EvidenceRegistrationCoordinator>("evidenceRegistrationCoordinator")
        // Reflected as the concrete DurableMemoryCore, not the MemoryCore interface, so it shares
        // a real common type with retrievalDelegate below (MemoryRetrieval, which DurableMemoryCore
        // also implements) for assertSame's own type inference -- MemoryCore and MemoryRetrieval
        // are themselves unrelated sibling interfaces.
        val rawMemoryCore = registrationCoordinator.privateField<DurableMemoryCore>("memoryCore")

        val evidenceIntelligence = runtime.privateField<Any>("evidenceIntelligence")
        val inputResolver = evidenceIntelligence.privateField<Any>("inputResolver")
        val retrievalDecorator = inputResolver.privateField<PermissionFilteredMemoryRetrieval>("memoryRetrieval")
        val retrievalDelegate = retrievalDecorator.privateField<MemoryRetrieval>("delegate")

        assertSame(rawMemoryCore, retrievalDelegate, "retrieval must be wired through the durable decorator itself, not a second reference to the recovered in-memory delegate DurableMemoryCore holds privately")

        runtime.shutdown()
    }

    // ================= Behavioural verification: durable commit survives a fresh recovery =================

    @Test
    fun `an Entity created through the running graph is recovered by a second, independent ParkerRuntime pointed at the same durability log`() = runTest {
        val sharedLogPath = Files.createTempDirectory("memory-core-durability-composition-shared").resolve("memory-core.log").toString()
        val principal = PrincipalId(ownerPrincipalId)

        val firstRuntime = ParkerRuntime(config(memoryCoreDurabilityLogPath = sharedLogPath), RecordingParkerLogger())
        firstRuntime.start()
        val firstMemoryCore = firstRuntime
            .privateField<EvidenceRegistrationCoordinator>("evidenceRegistrationCoordinator")
            .privateField<DurableMemoryCore>("memoryCore")
        val provenance = firstMemoryCore.createProvenance(principal, candidateProvenance())
        val entity = firstMemoryCore.createEntity(
            principal,
            CandidateEntity(entityType = "person", primaryLabel = "Durability Composition Fixture", provenanceId = provenance.provenanceId),
        )
        firstRuntime.shutdown()

        val secondRuntime = ParkerRuntime(config(memoryCoreDurabilityLogPath = sharedLogPath), RecordingParkerLogger())
        secondRuntime.start()
        val secondMemoryCore = secondRuntime
            .privateField<EvidenceRegistrationCoordinator>("evidenceRegistrationCoordinator")
            .privateField<DurableMemoryCore>("memoryCore")

        val recovered = secondMemoryCore.getEntity(principal, entity.entityId)
        assertNotNull(recovered, "the second, independently-started runtime must recover the first runtime's own durably committed Entity from the shared log")
        assertEquals(entity.entityId, recovered.entityId)
        assertEquals("Durability Composition Fixture", recovered.primaryLabel)

        secondRuntime.shutdown()
    }

    // Memory Core Durability, Implementation Unit 10 (Verification), item 10: "Full runtime
    // reconstruction test. ParkerRuntime.start(), exercised end-to-end against a pre-populated
    // durability log, reaches RUNNING with fully restored Memory Core state, and every existing
    // consumer (EvidenceRegistrationCoordinator, the Programme 4 coordinator,
    // PermissionFilteredMemoryRetrieval) observes the restored data through its own existing call
    // path." The test above already proves genuine file-backed recovery through one reflectively-
    // obtained reference; this test goes further, checking each of the three named consumers' own
    // distinct field independently, plus one genuine public-API exercise (submitEvidence) proving the
    // write side continues correctly atop restored state, not merely that reflection can observe it.
    @Test
    fun `ParkerRuntime start() against a pre-populated durability log reaches RUNNING, and each of the three named consumers observes the restored state through its own reference`() = runTest {
        val logPath = Files.createTempDirectory("memory-core-durability-composition-prepopulated").resolve("memory-core.log")
        val principal = PrincipalId(ownerPrincipalId)

        // Pre-populated independently of ParkerRuntime entirely -- a bare DurableMemoryCore over the
        // real filesystem log, never a live runtime -- exactly the "a durability log already exists,
        // from some prior process" scenario this item names, not merely a second lap of a runtime this
        // same test constructed.
        val seed = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logPath))
        val seededProvenance = seed.createProvenance(principal, candidateProvenance())
        val seededEntity = seed.createEntity(
            principal,
            CandidateEntity(entityType = "person", primaryLabel = "Pre-Populated Entity", provenanceId = seededProvenance.provenanceId),
        )
        val seededDocument = seed.registerDocument(
            principal,
            CandidateDocument(documentType = "email", locationReference = "mailbox://inbox/message-1", provenanceId = seededProvenance.provenanceId),
        )
        val seededAssertion = seed.createAssertion(
            principal,
            CandidateAssertion(statement = "pre-populated assertion", provenanceId = seededProvenance.provenanceId),
        )

        val runtime = ParkerRuntime(config(memoryCoreDurabilityLogPath = logPath.toString()), RecordingParkerLogger())
        runtime.start()

        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state, "start() must reach RUNNING against a genuinely pre-populated log, not merely an empty one")

        // Consumer 1: EvidenceRegistrationCoordinator's own memoryCore field, queried directly.
        val registrationMemoryCore = runtime
            .privateField<EvidenceRegistrationCoordinator>("evidenceRegistrationCoordinator")
            .privateField<DurableMemoryCore>("memoryCore")
        assertEquals(seededEntity, registrationMemoryCore.getEntity(principal, seededEntity.entityId), "EvidenceRegistrationCoordinator's own MemoryCore reference must observe the pre-populated Entity")

        // Consumer 2: EvidenceIntelligenceAcceptanceCoordinator's own memoryCore field, queried directly.
        val acceptanceMemoryCore = runtime
            .privateField<EvidenceIntelligenceAcceptanceCoordinator>("evidenceIntelligenceAcceptanceCoordinator")
            .privateField<DurableMemoryCore>("memoryCore")
        assertEquals(seededDocument, acceptanceMemoryCore.getDocument(principal, seededDocument.documentId), "EvidenceIntelligenceAcceptanceCoordinator's own MemoryCore reference must observe the pre-populated Document")

        // Consumer 3: the shared PermissionFilteredMemoryRetrieval's own delegate field, queried
        // directly -- bypassing the decorator's own deliberate, permanent fail-closed retrieval gate
        // (Errata 004; already independently verified elsewhere), exactly as
        // ParkerRuntimeEvidenceIntelligenceCompositionTest's own established precedent does to confirm
        // a record genuinely exists before separately demonstrating the wrapper's own denial.
        val evidenceIntelligence = runtime.privateField<Any>("evidenceIntelligence")
        val inputResolver = evidenceIntelligence.privateField<Any>("inputResolver")
        val retrievalDecorator = inputResolver.privateField<PermissionFilteredMemoryRetrieval>("memoryRetrieval")
        val retrievalDelegate = retrievalDecorator.privateField<DurableMemoryCore>("delegate")
        assertEquals(seededAssertion, retrievalDelegate.getAssertion(principal, seededAssertion.assertionId), "the shared PermissionFilteredMemoryRetrieval's own delegate must observe the pre-populated Assertion")

        // Genuine public-API exercise: EvidenceRegistrationCoordinator's own real call path
        // (submitEvidence), proving the write side continues correctly atop restored identifier
        // counters -- the newly minted Provenance/Document must not collide with the pre-populated
        // provenance-1/document-1.
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(
                principal,
                CandidateEvidenceArtifact("post-restart submission content".toByteArray()),
                candidateProvenance(),
                "post-restart-document",
            ),
        )
        assertEquals(ProvenanceId("provenance-2"), registered.provenance.provenanceId, "a post-restart write must continue minting past the pre-populated Provenance, never colliding with it")
        assertEquals(DocumentId("document-2"), registered.document.documentId, "a post-restart write must continue minting past the pre-populated Document, never colliding with it")

        runtime.shutdown()
    }

    // ================= Runtime failure verification: no silent empty-store fallback =================

    @Test
    fun `start() aborts to FAILED, naming Memory Core recovery, when the durability log fixture is intentionally malformed -- no empty in-memory replacement is ever produced`() = runTest {
        // An isolated test fixture only: one line of genuine garbage, never produced by any real
        // append() call, written directly to a fresh log file before ParkerRuntime is even
        // constructed -- proving recovery failure aborts startup rather than falling back to a
        // fresh, empty Memory Core.
        val corruptLogPath = Files.createTempDirectory("memory-core-durability-composition-corrupt").resolve("memory-core.log")
        Files.writeString(corruptLogPath, "this is not a valid Memory Core durability log entry\n")

        val runtime = ParkerRuntime(config(memoryCoreDurabilityLogPath = corruptLogPath.toString()), RecordingParkerLogger())

        val thrown = assertFailsWith<ParkerRuntimeException.DependencyConstructionFailed> { runtime.start() }
        assertEquals("Memory Core recovery", thrown.component)
        assertEquals(RuntimeLifecycleState.FAILED, runtime.state)

        // shutdown() remains callable after a failed start(), mirroring
        // ParkerRuntimeStartupAndShutdownTest's own identical precedent for every other
        // dependency-construction failure -- no component this composition root already
        // constructed (before the failing step) is left in a state shutdown() cannot handle.
        runtime.shutdown()
        assertEquals(RuntimeLifecycleState.STOPPED, runtime.state)
    }

    @Test
    fun `the filesystem durability log construction step itself fails cleanly, naming its own step, when its parent directory does not exist`() = runTest {
        val nonExistentParent = Files.createTempDirectory("memory-core-durability-composition-missing-parent")
            .resolve("does-not-exist")
            .resolve("memory-core.log")

        val runtime = ParkerRuntime(config(memoryCoreDurabilityLogPath = nonExistentParent.toString()), RecordingParkerLogger())

        val thrown = assertFailsWith<ParkerRuntimeException.DependencyConstructionFailed> { runtime.start() }
        assertEquals("Memory Core durability log construction", thrown.component)
        assertEquals(RuntimeLifecycleState.FAILED, runtime.state)
    }
}
