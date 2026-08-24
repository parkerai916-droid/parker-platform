package parker.composition

import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADocumentFormat
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome
import parker.core.runtime.EvidenceRegistrationOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Document Ingestion, Owner-Facing Tier A Runtime Invocation Boundary.
 * End-to-end acceptance tests against the real, fully-wired production
 * graph -- real filesystem-backed Evidence Custodian, manifest, derivative
 * generation, and ingestion audit storage, and the real
 * `ParkerRuntime.invokeTierAIngestionAsOwner` entry point -- mirroring
 * [ParkerRuntimeEvidenceCustodianIntegrationTest]'s own established
 * "prove the wiring itself" style exactly. Behavioural coverage of the
 * governed chain itself (manifest/byte retrieval, integrity verification,
 * router pass-through) belongs to `TierAOwnerInvocationCoordinatorTest`,
 * not repeated here.
 */
class ParkerRuntimeTierAOwnerInvocationIntegrationTest {

    private val ownerPrincipalId = "user.owner-tier-a-integration-test"

    private fun config(): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-tier-a-integration-test",
        evidenceStorageRootPath = Files.createTempDirectory("tier-a-integration-evidence-storage").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("tier-a-integration-evidence-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("tier-a-integration-derivative-generation").toString(),
        derivativeContentStorageRootPath = Files.createTempDirectory("tier-a-integration-derivative-generation-content").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("tier-a-integration-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("tier-a-integration-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("tier-a-integration-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("tier-a-integration-knowledge-items").resolve("items.log").toString(),
    )

    private val fixtureRoot: Path = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures")

    // --- Full production graph: submit -> invoke Tier A as owner -> admitted ---

    @Test
    fun `invokeTierAIngestionAsOwner against the real production graph admits a governed CSV fixture end to end`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principalId = PrincipalId(ownerPrincipalId)
        val bytes = Files.readAllBytes(fixtureRoot.resolve("06-structured.csv"))
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(
                requestingPrincipalId = principalId,
                candidateEvidenceArtifact = CandidateEvidenceArtifact(bytes, "text/csv", "06-structured.csv"),
                candidateProvenance = candidateProvenance(),
                documentType = "integration-test-document",
            ),
        )

        val outcome = runtime.invokeTierAIngestionAsOwner(registered.acceptedEvidenceArtifact.evidenceArtifactId)

        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(outcome)
        val admitted = assertIs<TierADocumentRoutingResult.Admitted>(routed.result)
        assertEquals(TierADocumentFormat.CSV, admitted.format)

        runtime.shutdown()
    }

    @Test
    fun `invokeTierAIngestionAsOwner for an artifact never submitted fails closed with ManifestNotFound, never a fabricated route`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val outcome = runtime.invokeTierAIngestionAsOwner(EvidenceArtifactId("evidence-never-submitted"))

        assertEquals(TierAOwnerInvocationOutcome.ManifestNotFound(EvidenceArtifactId("evidence-never-submitted")), outcome)

        runtime.shutdown()
    }

    // --- invokeTierAIngestionAsOwner is structurally owner-only: no principal parameter exists ---

    @Test
    fun `invokeTierAIngestionAsOwner declares no requestingPrincipalId parameter -- structurally always the configured owner`() {
        val function = ParkerRuntime::class.declaredFunctions.single { it.name == "invokeTierAIngestionAsOwner" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(
            listOf(EvidenceArtifactId::class),
            valueParameterTypes,
            "invokeTierAIngestionAsOwner must take exactly one parameter (the target identifier) -- no " +
                "requestingPrincipalId parameter, mirroring deleteEvidenceAsOwner's own structural " +
                "owner-only pattern exactly, so no caller of this method can ever substitute a " +
                "principal other than the configured owner",
        )
    }

    // --- Lifecycle guard, mirroring deleteEvidenceAsOwner's own existing NotRunning precedent ---

    @Test
    fun `invokeTierAIngestionAsOwner before start() throws NotRunning`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        assertFailsWith<ParkerRuntimeException.NotRunning> {
            runtime.invokeTierAIngestionAsOwner(EvidenceArtifactId("some-artifact"))
        }
    }

    private fun candidateProvenance() = parker.core.interfaces.CandidateProvenance(
        sourceIdentifier = "integration-test-source",
        sourceType = "test",
        acquisitionTime = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = parker.core.interfaces.ContentNature.ORIGINAL,
    )
}
