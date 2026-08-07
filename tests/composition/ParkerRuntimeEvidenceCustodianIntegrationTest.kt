package parker.composition

import java.nio.file.Files
import java.time.Instant
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceDeletionResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.PrincipalId
import parker.core.runtime.EvidenceRegistrationOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Evidence Custodian, Implementation Plan Phase 10 ("Runtime Integration").
 * End-to-end acceptance tests against the real, fully-wired production
 * graph -- real [FileSystemEvidenceArtifactStorage][parker.core.runtime.FileSystemEvidenceArtifactStorage]
 * and [FileSystemEvidenceDeletionAudit][parker.core.runtime.FileSystemEvidenceDeletionAudit]
 * backed by real temporary directories, a real [DefaultPermissionPolicy][parker.core.runtime.DefaultPermissionPolicy]
 * resolving the five disclosed Evidence Custodian conventions this Unit
 * registered, and the real `ParkerRuntime.submitEvidence`/`retrieveEvidence`/
 * `deleteEvidenceAsOwner` entry points -- not `FakePermissionEngine` or
 * `InMemoryEvidenceArtifactStorage`, which every unit-level Evidence
 * Custodian test suite already exercises. This suite proves the wiring
 * itself, not behaviour those suites already prove.
 *
 * No live model server is used -- these tests never call
 * `submitOwnerMessage`, so [ParkerRuntimeConfig.modelEndpointUrl] is
 * deliberately unreachable, mirroring [ParkerRuntimeStartupAndShutdownTest]'s
 * own precedent.
 */
class ParkerRuntimeEvidenceCustodianIntegrationTest {

    private val ownerPrincipalId = "user.owner-evidence-integration-test"

    private fun config(): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-evidence-integration-test",
        evidenceStorageRootPath = Files.createTempDirectory("evidence-integration-storage").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("evidence-integration-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("evidence-integration-memory").resolve("memory-core.log").toString(),
    )

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "integration-test-source",
        sourceType = "test",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    // --- Full production graph: accept -> register -> retrieve -> delete -> retrieve ---

    @Test
    fun `submitEvidence against the real production graph durably persists content and registers it with Memory Core`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principalId = PrincipalId(ownerPrincipalId)
        val content = "real production graph evidence bytes".toByteArray()

        val outcome = runtime.submitEvidence(
            requestingPrincipalId = principalId,
            candidateEvidenceArtifact = CandidateEvidenceArtifact(content),
            candidateProvenance = candidateProvenance(),
            documentType = "integration-test-document",
        )

        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(outcome)
        assertEquals(
            registered.acceptedEvidenceArtifact.evidenceArtifactId.value,
            registered.document.locationReference,
            "the registered Document's locationReference must be exactly the accepted artefact's own identifier",
        )

        runtime.shutdown()
    }

    @Test
    fun `retrieveEvidence against the real production graph returns exactly what was submitted`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principalId = PrincipalId(ownerPrincipalId)
        val content = "round trip content".toByteArray()
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(principalId, CandidateEvidenceArtifact(content), candidateProvenance(), "integration-test-document"),
        )

        val result = runtime.retrieveEvidence(principalId, registered.acceptedEvidenceArtifact.evidenceArtifactId)

        val found = assertIs<EvidenceRetrievalResult.Found>(result)
        assertTrue(content.contentEquals(found.content))

        runtime.shutdown()
    }

    @Test
    fun `deleteEvidenceAsOwner against the real production graph removes the artefact and durably audits it`() = runTest {
        val auditLogPath = Files.createTempDirectory("evidence-integration-audit-explicit").resolve("audit.log")
        val config = config().copy(evidenceDeletionAuditLogPath = auditLogPath.toString())
        val runtime = ParkerRuntime(config, RecordingParkerLogger())
        runtime.start()
        val principalId = PrincipalId(ownerPrincipalId)
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(
                principalId,
                CandidateEvidenceArtifact("to be deleted".toByteArray()),
                candidateProvenance(),
                "integration-test-document",
            ),
        )
        val evidenceArtifactId = registered.acceptedEvidenceArtifact.evidenceArtifactId

        val deletionResult = runtime.deleteEvidenceAsOwner(evidenceArtifactId)

        assertIs<EvidenceDeletionResult.Deleted>(deletionResult)
        assertIs<EvidenceRetrievalResult.NotFound>(runtime.retrieveEvidence(principalId, evidenceArtifactId))

        val auditLines = Files.readAllLines(auditLogPath)
        assertEquals(2, auditLines.size, "exactly one AUTHORISED and one COMPLETED record for this one deletion")
        assertTrue(auditLines[0].contains("stage=AUTHORISED"))
        assertTrue(auditLines[1].contains("stage=COMPLETED"))
        assertTrue(auditLines.all { it.contains("evidenceArtifactId=${evidenceArtifactId.value}") })

        runtime.shutdown()
    }

    @Test
    fun `deleting an evidence artefact that was never submitted returns NotFound against the real production graph`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val result = runtime.deleteEvidenceAsOwner(EvidenceArtifactId("evidence-never-submitted"))

        assertIs<EvidenceDeletionResult.NotFound>(result)

        runtime.shutdown()
    }

    // --- deleteEvidenceAsOwner is structurally owner-only: no principal parameter exists ---

    @Test
    fun `deleteEvidenceAsOwner declares no requestingPrincipalId parameter -- structurally always the configured owner`() {
        val function = ParkerRuntime::class.declaredFunctions.single { it.name == "deleteEvidenceAsOwner" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(
            listOf(EvidenceArtifactId::class),
            valueParameterTypes,
            "deleteEvidenceAsOwner must take exactly one parameter (the target identifier) -- no " +
                "requestingPrincipalId parameter, unlike submitEvidence/retrieveEvidence, so no caller " +
                "of this method can ever substitute a principal other than the configured owner " +
                "(Phase 7 Boundary Clarification Section 3; Controlled Agent Run Submission's own " +
                "call-site-structure precedent)",
        )
    }

    // --- Lifecycle guards, mirroring submitOwnerMessage's own existing NotRunning precedent ---

    @Test
    fun `submitEvidence before start() throws NotRunning`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        assertFailsWith<ParkerRuntimeException.NotRunning> {
            runtime.submitEvidence(
                PrincipalId(ownerPrincipalId),
                CandidateEvidenceArtifact("content".toByteArray()),
                candidateProvenance(),
                "integration-test-document",
            )
        }
    }

    @Test
    fun `retrieveEvidence before start() throws NotRunning`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        assertFailsWith<ParkerRuntimeException.NotRunning> {
            runtime.retrieveEvidence(PrincipalId(ownerPrincipalId), EvidenceArtifactId("some-artifact"))
        }
    }

    @Test
    fun `deleteEvidenceAsOwner before start() throws NotRunning`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        assertFailsWith<ParkerRuntimeException.NotRunning> {
            runtime.deleteEvidenceAsOwner(EvidenceArtifactId("some-artifact"))
        }
    }
}
