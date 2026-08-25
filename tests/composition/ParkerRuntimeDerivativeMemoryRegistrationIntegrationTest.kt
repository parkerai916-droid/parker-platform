package parker.composition

import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KParameter
import kotlin.reflect.full.declaredFunctions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DerivativeMemoryRegistrationOutcome
import parker.core.interfaces.DocumentProcessingStatus
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.OwnerLocalFileIngressOutcome
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADocumentFormat
import parker.core.interfaces.TierADocumentRoutingResult
import parker.core.interfaces.TierAOwnerInvocationOutcome

/**
 * Document Ingestion, Derivative-to-Memory-Core Registration. End-to-end
 * acceptance tests against the real, fully-wired production graph -- a real
 * `DefaultPermissionPolicy` resolving
 * `DerivativeMemoryRegistrationCoordinator.CREATE_PROVENANCE_ACTION_NAME`/
 * `REGISTER_DOCUMENT_ACTION_NAME` against the existing `(WRITE, MEMORY)`
 * pair, and the real `ParkerRuntime.registerDerivativeInMemoryAsOwner` entry
 * point -- mirroring
 * [ParkerRuntimeOwnerLocalFileIngressIntegrationTest]'s own established
 * "prove the wiring itself" style exactly. Behavioural coverage of the
 * governed chain itself (field mapping, content boundary, ineligible-state
 * enforcement) belongs to `DerivativeMemoryRegistrationCoordinatorTest`, not
 * repeated here.
 */
class ParkerRuntimeDerivativeMemoryRegistrationIntegrationTest {

    private val ownerPrincipalId = "user.owner-derivative-memory-registration-integration-test"

    private fun config(): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-derivative-memory-registration-integration-test",
        evidenceStorageRootPath = Files.createTempDirectory("derivative-memory-registration-integration-evidence-storage").toString(),
        evidenceSourceManifestStorageRootPath =
            Files.createTempDirectory("derivative-memory-registration-integration-evidence-manifest").toString(),
        derivativeGenerationStorageRootPath =
            Files.createTempDirectory("derivative-memory-registration-integration-derivative-generation").toString(),
        derivativeContentStorageRootPath =
            Files.createTempDirectory("derivative-memory-registration-integration-derivative-generation-content").toString(),
        savedAnalysisStorageRootPath = Files.createTempDirectory("saved-analysis-storage").toString(),
        documentIngestionAuditLogPath =
            Files.createTempDirectory("derivative-memory-registration-integration-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("derivative-memory-registration-integration-deletion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("derivative-memory-registration-integration-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("derivative-memory-registration-integration-knowledge-items").resolve("items.log").toString(),
    )

    // --- Three visibly separate owner actions: import -> Tier A -> Memory Core registration ---

    @Test
    fun `three separate owner actions -- import, Tier A invocation, and derivative Memory Core registration -- compose correctly`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val fixtureBytes = Files.readAllBytes(Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/06-structured.csv"))
        val sourceDir = Files.createTempDirectory("derivative-memory-registration-source")
        val sourcePath = sourceDir.resolve("ledger.csv")
        Files.write(sourcePath, fixtureBytes)

        // Step 1: one explicit owner action -- import only.
        val importOutcome = runtime.importEvidenceFileAsOwner(sourcePath.toString(), receivedMediaType = "text/csv")
        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(importOutcome)
        val evidenceArtifactId = accepted.acceptedEvidenceArtifact.evidenceArtifactId

        // Step 2: a second, separate, explicit owner action -- Tier A invocation only. Its own
        // return type (TierAOwnerInvocationOutcome.Routed / TierADocumentRoutingResult.Admitted)
        // proves no Memory Core write has occurred yet -- neither variant carries a Provenance or
        // a Document.
        val tierAOutcome = runtime.invokeTierAIngestionAsOwner(evidenceArtifactId)
        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(tierAOutcome)
        val admitted = assertIs<TierADocumentRoutingResult.Admitted>(routed.result)
        assertEquals(TierADocumentFormat.CSV, admitted.format)
        val csv = assertIs<TierADerivativePayload.Csv>(admitted.payload).value
        assertEquals(listOf("id", "name", "reference", "amount", "note"), csv.headers)
        assertEquals(listOf("001", "Alpha, Limited", "00001234", "1234.50", "Exact, quoted value"), csv.rows.first())

        // Step 3: a third, separate, explicit owner action -- the exact, unchanged Admitted result
        // step 2 returned is supplied by the caller, never re-derived, proving identity continuity
        // across three independent calls.
        val registrationOutcome = runtime.registerDerivativeInMemoryAsOwner(admitted)

        val registered = assertIs<DerivativeMemoryRegistrationOutcome.Registered>(registrationOutcome)
        assertEquals(admitted.record.derivativeGenerationId.value, registered.provenance.sourceIdentifier)
        assertEquals(ContentNature.EXTRACTED, registered.provenance.contentNature)
        assertNull(registered.provenance.extractedFrom)
        assertEquals(admitted.record.derivativeKind, registered.document.documentType)
        assertEquals(admitted.record.derivativeGenerationId.value, registered.document.locationReference)
        assertEquals(registered.provenance.provenanceId, registered.document.provenanceId)
        assertEquals(DocumentProcessingStatus.PROCESSED_EXTERNALLY, registered.document.processingStatus)

        // The parser-extracted payload content itself never reached any registered field.
        val distinctiveValue = "Alpha, Limited"
        assertEquals(false, registered.provenance.sourceIdentifier.contains(distinctiveValue))
        assertEquals(false, registered.document.locationReference.contains(distinctiveValue))

        // The evidence import from step 1 remains independently retrievable, untouched by
        // registration -- proving no destructive/rollback interaction across the three actions.
        val retrievedAfterRegistration = runtime.retrieveEvidence(PrincipalId(ownerPrincipalId), evidenceArtifactId)
        val found = assertIs<EvidenceRetrievalResult.Found>(retrievedAfterRegistration)
        assertEquals(true, fixtureBytes.contentEquals(found.content))

        runtime.shutdown()
    }

    // --- No automatic registration merely because Tier A admission succeeded ---

    @Test
    fun `a successful Tier A invocation alone performs no Memory Core registration -- registration remains a separate, explicit third action`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val fixtureBytes = Files.readAllBytes(Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/06-structured.csv"))
        val sourceDir = Files.createTempDirectory("derivative-memory-registration-no-auto-source")
        val sourcePath = sourceDir.resolve("ledger.csv")
        Files.write(sourcePath, fixtureBytes)
        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
            runtime.importEvidenceFileAsOwner(sourcePath.toString(), receivedMediaType = "text/csv"),
        )

        val tierAOutcome = runtime.invokeTierAIngestionAsOwner(accepted.acceptedEvidenceArtifact.evidenceArtifactId)

        // The Tier A outcome type itself is the proof: TierAOwnerInvocationOutcome.Routed wraps a
        // TierADocumentRoutingResult, never a DerivativeMemoryRegistrationOutcome -- there is no
        // field, branch, or code path on this result through which a Provenance or Document could
        // have been produced by this call alone.
        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(tierAOutcome)
        assertIs<TierADocumentRoutingResult.Admitted>(routed.result)

        runtime.shutdown()
    }

    // --- registerDerivativeInMemoryAsOwner is structurally owner-only: no principal parameter exists ---

    @Test
    fun `registerDerivativeInMemoryAsOwner declares no requestingPrincipalId parameter -- structurally always the configured owner`() {
        val function = ParkerRuntime::class.declaredFunctions.single { it.name == "registerDerivativeInMemoryAsOwner" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(
            listOf(TierADocumentRoutingResult.Admitted::class),
            valueParameterTypes,
            "registerDerivativeInMemoryAsOwner must take exactly (admitted) -- no requestingPrincipalId " +
                "parameter, mirroring deleteEvidenceAsOwner's/invokeTierAIngestionAsOwner's/" +
                "importEvidenceFileAsOwner's own structural owner-only pattern exactly, so no caller of " +
                "this method can ever substitute a principal other than the configured owner",
        )
    }

    // --- Lifecycle guard, mirroring the other owner entry points' own existing NotRunning precedent ---

    @Test
    fun `registerDerivativeInMemoryAsOwner before start() throws NotRunning`() = runTest {
        // A genuine Admitted value is obtained from a separate, already-started-then-shutdown
        // runtime, so the guard under test is proven on the never-started runtime specifically --
        // not merely on a runtime that happens to have no eligible derivative yet.
        val fixtureBytes = Files.readAllBytes(Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/06-structured.csv"))
        val helperRuntime = ParkerRuntime(config(), RecordingParkerLogger())
        helperRuntime.start()
        val sourceDir = Files.createTempDirectory("derivative-memory-registration-lifecycle-source")
        val sourcePath = sourceDir.resolve("ledger.csv")
        Files.write(sourcePath, fixtureBytes)
        val accepted = assertIs<OwnerLocalFileIngressOutcome.Accepted>(
            helperRuntime.importEvidenceFileAsOwner(sourcePath.toString(), receivedMediaType = "text/csv"),
        )
        val routed = assertIs<TierAOwnerInvocationOutcome.Routed>(
            helperRuntime.invokeTierAIngestionAsOwner(accepted.acceptedEvidenceArtifact.evidenceArtifactId),
        )
        val admitted = assertIs<TierADocumentRoutingResult.Admitted>(routed.result)
        helperRuntime.shutdown()

        val neverStartedRuntime = ParkerRuntime(config(), RecordingParkerLogger())
        assertFailsWith<ParkerRuntimeException.NotRunning> {
            neverStartedRuntime.registerDerivativeInMemoryAsOwner(admitted)
        }
    }
}
