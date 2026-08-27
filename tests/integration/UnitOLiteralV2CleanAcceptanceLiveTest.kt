package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Instant
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import parker.composition.*
import parker.core.interfaces.*

/** Explicitly invoked detached real-document instrument; never part of test/check/build/offline verification. */
class UnitOLiteralV2CleanAcceptanceLiveTest {
    @Test fun `one authorised locked clean request uses production custody validation and durable admission`() = runTest {
        val env = System.getenv()
        val commit = required(env, "PARKER_UNIT_O4R_REPOSITORY_COMMIT")
        val profilePath = required(env, "PARKER_UNIT_O4R_PROVIDER_PROFILE_PATH")
        val resultPath = Path.of(required(env, "PARKER_UNIT_O4R_RESULT_PATH"))
        val authorizationPath = Path.of(required(env, "PARKER_UNIT_O4R_AUTHORIZATION_RECORD_PATH"))
        val evidenceId = EvidenceArtifactId(UnitOLiteralV2CleanLock.EVIDENCE_ID)
        val manifestRoot = Path.of(required(env, "PARKER_UNIT_O4R_MANIFEST_ROOT"))
        val evidenceRoot = Path.of(required(env, "PARKER_UNIT_O4R_EVIDENCE_ROOT"))
        val generationRoot = Path.of(required(env, "PARKER_UNIT_O4R_GENERATION_ROOT"))
        val contentRoot = Path.of(required(env, "PARKER_UNIT_O4R_CONTENT_ROOT"))
        val ledgerRoot = Path.of(required(env, "PARKER_UNIT_O4R_ATTEMPT_LEDGER_ROOT"))
        val identity = AcceptanceExecutionIdentity(
            AcceptanceExecutionId("unit-o4r-clean-literal-v2-request-2"),
            UnitOLiteralV2CleanLock.STAGE, UnitOLiteralV2CleanLock.ALLOCATION,
            UnitOLiteralV2CleanLock.REQUEST_ORDINAL, evidenceId.value,
            UnitOLiteralV2CleanLock.SOURCE_SHA256, UnitOLiteralV2CleanLock.SOURCE_BYTES,
            UnitOLiteralV2CleanLock.MEDIA_TYPE, commit, UnitOLiteralV2CleanLock.PROVIDER,
            UnitOLiteralV2CleanLock.MODEL_RULE, UnitOLiteralV2CleanLock.PROFILE,
            UnitOLiteralV2CleanLock.INSTRUCTION_SHA256, UnitOLiteralV2CleanLock.SCHEMA_SHA256,
            UnitOLiteralV2CleanLock.PROCESSING_PROFILE, UnitOLiteralV2CleanLock.ADAPTER_VERSION,
        )
        val tracker = UnitOAcceptanceAttemptTracker(FileSystemAcceptanceAttemptLedger(ledgerRoot), identity)
        tracker.authorized()

        try {
            val readiness = OpenAiExternalTranscriptionProviderReadinessEvaluator().evaluate(true, profilePath)
            val ready = assertIs<OpenAiExternalTranscriptionReadiness.Ready>(readiness)
            val credentialReady = OpenAiLiveAcceptanceBridge.credentialStructurallyReady(env["PARKER_OPENAI_API_KEY"])
            val authorization = readUnitOLiteralV2Authorization(authorizationPath)
            val permission = AcceptancePermission(evidenceId)
            val custodian = DefaultEvidenceCustodian(FileSystemEvidenceArtifactStorage(evidenceRoot), permission,
                FileSystemEvidenceSourceManifestStorage(manifestRoot))
            val adapter = OpenAiLiveAcceptanceBridge.createAcceptance(ready, tracker) { }
            val generations = FileSystemDerivativeGenerationStorage(generationRoot)
            val contents = FileSystemDerivativeContentStorage(contentRoot)
            val admission = DerivativeGenerationCoordinator(CsvStructuralExtractor { error("CSV unreachable") }, generations,
                DocumentIngestionAudit { }, now = { Instant.now() }, contentStorage = contents)
            val coordinator = ExternalTranscriptionOwnerInvocationCoordinator(PrincipalId("owner.unit-o4r.acceptance"), permission,
                custodian, adapter.mechanism, OcrStructuredResultValidator(), admission, invocationObserver = tracker)
            val boundary = UnitOLiteralV2CleanAcceptanceBoundary(UnitOManifestMetadataReader { id ->
                UnitOReadOnlyManifestAcceptanceBridge.read(manifestRoot, id)?.let {
                    UnitOAuthoritativeManifestFacts(it.evidenceArtifactId, it.sha256, it.byteLength, it.mediaType)
                }
            }, coordinator::invoke)
            val input = UnitOLiteralV2AcceptancePreflightInput(
                System.getProperty("parker.unitO4r.literalV2.acceptance.enabled") == "true", authorization, evidenceId, commit,
                readiness, credentialReady, resultPath,
            )
            val preflight = boundary.preflight(input)
            require(preflight.ready) { "UNIT_O4R_PREFLIGHT_FAILED=${preflight.problems.joinToString(",")}" }
            tracker.preflightPassed()
            assertEquals(0, adapter.state.calls)

            val outcome = boundary.execute(input)
            val admitted = outcome as? ExternalTranscriptionOwnerInvocationOutcome.Admitted
            val extracted = admitted?.extracted
            val provider = extracted?.providerProvenance
            val accounting = extracted?.pageAccounting
            val result = UnitOLiteralV2AcceptanceResult(
                status = if (admitted != null) "ADMITTED_PENDING_HUMAN_REVIEW" else safeOutcome(outcome), commit = commit,
                returnedModel = provider?.providerReportedModelIdentifier, responseId = provider?.providerCorrelationIdentifier,
                generationId = admitted?.record?.derivativeGenerationId?.value,
                requestedPages = accounting?.requestedScope?.pageNumbers?.joinToString(","),
                returnedPages = accounting?.returnedScope?.pageNumbers?.joinToString(","),
                fidelity = extracted?.fidelity?.name, completeness = extracted?.completenessState?.name,
                requestCount = adapter.state.calls,
            )
            assertEquals(1, adapter.state.calls)
            if (admitted == null) {
                writeTerminalResult(resultPath, result.render())
                error("literal-v2 CLEAN acceptance failed: ${safeOutcome(outcome)}")
            }
            assertEquals(UnitOLiteralV2CleanLock.PROFILE,
                (provider?.transcriptionConfiguration as? OcrTranscriptionConfiguration.DigestedConfiguration)?.profileId)
            val restored = TierBOcrContentRetrievalCoordinator(FileSystemDerivativeGenerationStorage(generationRoot),
                FileSystemDerivativeContentStorage(contentRoot)).retrieve(evidenceId, admitted.record.derivativeGenerationId)
            assertIs<TierBOcrContentRetrievalOutcome.Retrieved>(restored)
            writeTerminalResult(resultPath, result.render())
            tracker.terminalSuccess()
        } catch (error: Throwable) {
            tracker.terminalFailure(error)
            throw error
        }
    }

    private class AcceptancePermission(private val id: EvidenceArtifactId) : PermissionEngine {
        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
            require(request.metadata[ExternalTranscriptionInvocationGate.EVIDENCE_ARTIFACT_ID_METADATA_KEY] == id.value)
            return PermissionDecision(DecisionId("unit-o4r-explicit"), request.principalId, request.targetResources.single(),
                PermissionAction.EXECUTE, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC, Instant.now())
        }
        override suspend fun explain(decisionId: DecisionId): PermissionExplanation = error("unreachable")
    }
    private fun required(env: Map<String, String>, name: String) = requireNotNull(env[name]?.takeIf { it.isNotBlank() }) { "$name is required" }
    private fun writeTerminalResult(path: Path, text: String) {
        require(Files.isRegularFile(path) && Files.isWritable(path))
        val temporary = Files.createTempFile(path.parent, ".unit-o4r-", ".tmp")
        Files.writeString(temporary, text)
        Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }
    private fun safeOutcome(outcome: ExternalTranscriptionOwnerInvocationOutcome): String = when (outcome) {
        is ExternalTranscriptionOwnerInvocationOutcome.Admitted -> "ADMITTED"
        is ExternalTranscriptionOwnerInvocationOutcome.MechanismFailure -> "MECHANISM_FAILURE:${outcome.reason}"
        is ExternalTranscriptionOwnerInvocationOutcome.ReconciliationRequired -> "RECONCILIATION_REQUIRED"
        ExternalTranscriptionOwnerInvocationOutcome.NotAuthorised -> "NOT_AUTHORISED"
        else -> outcome::class.simpleName?.uppercase() ?: "FAILED"
    }
}
