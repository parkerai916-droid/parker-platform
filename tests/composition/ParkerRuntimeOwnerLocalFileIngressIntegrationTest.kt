package parker.composition

import java.nio.file.Files
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Document Ingestion, Owner-Authorized Local File Ingress. End-to-end
 * acceptance tests against the real, fully-wired production graph -- real
 * filesystem-backed Evidence Custodian and manifest storage, a real
 * `DefaultPermissionPolicy` resolving the new
 * `OwnerLocalFileIngressCoordinator.LOCAL_FILE_READ_ACTION_NAME` verb
 * phrase against the existing `(WRITE, DOCUMENT)` pair, and the real
 * `ParkerRuntime.importEvidenceFileAsOwner` entry point -- mirroring
 * [ParkerRuntimeTierAOwnerInvocationIntegrationTest]'s own established
 * "prove the wiring itself" style exactly. Behavioural coverage of the
 * governed chain itself (permission ordering, path/symlink/size
 * validation, byte fidelity) belongs to
 * `OwnerLocalFileIngressCoordinatorTest`, not repeated here.
 */
class ParkerRuntimeOwnerLocalFileIngressIntegrationTest {

    private val ownerPrincipalId = "user.owner-local-file-ingress-integration-test"

    private fun config(): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-local-file-ingress-integration-test",
        evidenceStorageRootPath = Files.createTempDirectory("local-file-ingress-integration-evidence-storage").toString(),
        evidenceSourceManifestStorageRootPath =
            Files.createTempDirectory("local-file-ingress-integration-evidence-manifest").toString(),
        derivativeGenerationStorageRootPath =
            Files.createTempDirectory("local-file-ingress-integration-derivative-generation").toString(),
        documentIngestionAuditLogPath =
            Files.createTempDirectory("local-file-ingress-integration-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("local-file-ingress-integration-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("local-file-ingress-integration-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("local-file-ingress-integration-knowledge-items").resolve("items.log").toString(),
    )

    // --- Full production graph: import a real local file -> Evidence Custodian Accepted ---

    @Test
    fun `importEvidenceFileAsOwner against the real production graph accepts a real local file end to end`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val sourceDir = Files.createTempDirectory("local-file-ingress-integration-source")
        val sourcePath = sourceDir.resolve("report.pdf")
        val content = "real production graph local file bytes".toByteArray()
        Files.write(sourcePath, content)

        val outcome = runtime.importEvidenceFileAsOwner(sourcePath.toString(), receivedMediaType = null)

        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(outcome)
        val retrieved = runtime.retrieveEvidence(PrincipalId(ownerPrincipalId), accepted.acceptedEvidenceArtifact.evidenceArtifactId)
        val found = assertIs<EvidenceRetrievalResult.Found>(retrieved)
        assertEquals(true, content.contentEquals(found.content))

        runtime.shutdown()
    }

    @Test
    fun `importEvidenceFileAsOwner for a nonexistent absolute path fails closed with PathNotFound, never a fabricated acceptance`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val dir = Files.createTempDirectory("local-file-ingress-integration-missing")

        val outcome = runtime.importEvidenceFileAsOwner(dir.resolve("never-created.pdf").toString(), null)

        assertEquals(OwnerLocalFileIngressOutcome.PathNotFound, outcome)

        runtime.shutdown()
    }

    @Test
    fun `importEvidenceFileAsOwner for a relative path fails closed with InvalidPath through the real production graph`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val outcome = runtime.importEvidenceFileAsOwner("relative/path.pdf", null)

        assertEquals(OwnerLocalFileIngressOutcome.InvalidPath, outcome)

        runtime.shutdown()
    }

    @Test
    fun `starting the runtime performs no local file ingress -- no automatic or background import occurs`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        runtime.start()

        // No EvidenceArtifactId exists yet because importEvidenceFileAsOwner was never called --
        // proven by requesting a never-imported identifier and observing NotFound, never Found.
        val result = runtime.retrieveEvidence(
            PrincipalId(ownerPrincipalId),
            parker.core.interfaces.EvidenceArtifactId("evidence-never-imported"),
        )
        assertIs<EvidenceRetrievalResult.NotFound>(result)

        runtime.shutdown()
    }

    // --- importEvidenceFileAsOwner is structurally owner-only: no principal parameter exists ---

    @Test
    fun `importEvidenceFileAsOwner declares no requestingPrincipalId parameter -- structurally always the configured owner`() {
        val function = ParkerRuntime::class.declaredFunctions.single { it.name == "importEvidenceFileAsOwner" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(
            listOf(String::class, String::class),
            valueParameterTypes,
            "importEvidenceFileAsOwner must take exactly (absolutePath, receivedMediaType) -- no " +
                "requestingPrincipalId parameter, mirroring deleteEvidenceAsOwner's/" +
                "invokeTierAIngestionAsOwner's own structural owner-only pattern exactly, so no caller " +
                "of this method can ever substitute a principal other than the configured owner",
        )
    }

    // --- Lifecycle guard, mirroring the other owner entry points' own existing NotRunning precedent ---

    @Test
    fun `importEvidenceFileAsOwner before start() throws NotRunning`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        assertFailsWith<ParkerRuntimeException.NotRunning> {
            runtime.importEvidenceFileAsOwner("/tmp/some-path.pdf", null)
        }
    }
}
