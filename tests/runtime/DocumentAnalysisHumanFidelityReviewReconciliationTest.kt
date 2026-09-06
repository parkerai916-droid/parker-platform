package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import parker.composition.HumanFidelityReviewExactTargetRegistrar
import parker.core.interfaces.*

/**
 * ANALYSIS-INGESTION-2 -- HFR / Unverified Acknowledgement Reconciliation. Behavioural tests
 * proving [DocumentAnalysisCoordinator]'s existing "unverified external transcription"
 * acknowledgement gate ([DocumentAnalysisOutcome.UnverifiedExternalAcknowledgementRequired]) is
 * waived only for the exact (evidenceArtifactId, derivativeGenerationId) pair whose effective
 * Human Fidelity Review state is [HumanFidelityReviewState.HUMAN_REVIEWED_PASS] -- resolved
 * through the same, unmodified [TierBOcrHumanFidelityReviewCoordinator]/
 * [EffectiveHumanFidelityReviewProjector] the Owner UI's own effective-review presentation already
 * uses, never a new mechanism. Real, filesystem-backed storage throughout (temp roots per test), a
 * real [DefaultPermissionEngine] wired exactly like
 * [TierBOcrHumanFidelityReviewCoordinatorTest]'s own `fixture()` helper for recording reviews --
 * never mocked. Only [DocumentAnalysisCoordinator]'s own [PermissionEngine]/[ModelInferenceClient]
 * are faked, matching [DocumentAnalysisCoordinatorTest]'s own established style.
 */
class DocumentAnalysisHumanFidelityReviewReconciliationTest {
    private val owner = PrincipalId("owner.analysis-hfr-reconciliation-test")
    private val clock = Clock.fixed(Instant.parse("2026-09-06T00:00:00Z"), ZoneOffset.UTC)

    private class Fixture(
        val documentAnalysisCoordinator: DocumentAnalysisCoordinator,
        val hfrCoordinator: TierBOcrHumanFidelityReviewCoordinator,
        val generationStorage: FileSystemDerivativeGenerationStorage,
        val contentStorage: FileSystemDerivativeContentStorage,
        val model: FakeModelInferenceClient,
    )

    private class FakePermissionEngine(private val outcome: PermissionDecisionOutcome) : PermissionEngine {
        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision = PermissionDecision(
            decisionId = DecisionId("fake-decision"),
            principalId = request.principalId,
            resourceId = request.targetResources.first(),
            action = PermissionAction.EXECUTE,
            decision = outcome,
            level = PermissionLevel.AUTOMATIC,
            timestamp = Instant.EPOCH,
        )

        override suspend fun explain(decisionId: DecisionId): PermissionExplanation =
            throw UnsupportedOperationException("must never be called")
    }

    private class FakeModelInferenceClient(
        private val onInfer: suspend (String) -> String = { "fake analysis response" },
    ) : ModelInferenceClient {
        var invocationCount: Int = 0
            private set

        override suspend fun infer(prompt: String): String {
            invocationCount += 1
            return onInfer(prompt)
        }
    }

    /** Mirrors [TierBOcrHumanFidelityReviewCoordinatorTest]'s own `fixture()` real-wiring, plus a [DocumentAnalysisCoordinator] wired to the same HFR coordinator through the narrow [AnalysisEffectiveHumanFidelityReviewResolver] adapter -- exactly the shape [parker.composition.ParkerRuntime] now constructs. */
    private suspend fun fixture(directory: Path, name: String): Fixture {
        val generationStorage = FileSystemDerivativeGenerationStorage(Files.createDirectories(directory.resolve("$name/generations")))
        val contentStorage = FileSystemDerivativeContentStorage(Files.createDirectories(directory.resolve("$name/content")))
        val reviewRoot = Files.createDirectories(directory.resolve("$name/reviews"))
        val auditRoot = Files.createDirectories(directory.resolve("$name/audit"))
        val audit = FileSystemHumanFidelityGovernanceAudit(auditRoot)
        val reviewStorage = FileSystemHumanFidelityReviewStorage(reviewRoot, audit, clock)
        val projector = DefaultEffectiveHumanFidelityReviewProjector(reviewStorage)

        val purposeRegistry = InMemoryAuthorizationPurposeRegistry()
        HumanFidelityReviewRecordingPermissionPolicy.registerPurpose(purposeRegistry)
        val identities = InMemoryIdentityService()
        identities.register(Principal(owner, PrincipalType.USER, "Owner", null, PrincipalStatus.CREATED, clock.instant(), clock.instant()))
        identities.updateStatus(owner, PrincipalStatus.ACTIVE)
        val resources = InMemoryResourceRegistry()
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
                setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT)),
            ),
        )
        val rules = listOf(
            PermissionPolicyRule(
                PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED,
                PermissionLevel.HIGH_ASSURANCE, HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE,
                HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
            ),
        )
        val hfrPermissionEngine = DefaultPermissionEngine(
            identities, DefaultPermissionPolicy(ActionMapper(vocabulary), resources, rules, purposeRegistry),
        )
        val permissionPolicy = HumanFidelityReviewRecordingPermissionPolicy(owner, purposeRegistry, hfrPermissionEngine, clock)
        val recordingService = DefaultGovernedHumanFidelityReviewRecordingService(permissionPolicy, reviewStorage)
        val registrar = HumanFidelityReviewExactTargetRegistrar(resources, owner, clock::instant)

        val hfrCoordinator = TierBOcrHumanFidelityReviewCoordinator(
            generationStorage, contentStorage, recordingService, registrar, projector, owner, clock::instant,
        )

        val model = FakeModelInferenceClient()
        val documentAnalysisCoordinator = DocumentAnalysisCoordinator(
            permissionEngine = FakePermissionEngine(PermissionDecisionOutcome.APPROVED),
            tierAContentRetrievalCoordinator = TierAContentRetrievalCoordinator(generationStorage, contentStorage),
            tierBOcrContentRetrievalCoordinator = TierBOcrContentRetrievalCoordinator(generationStorage, contentStorage),
            modelInferenceClient = model,
            promptBuilder = DefaultDocumentAnalysisPromptBuilder(),
            modelTimeoutMs = 30_000L,
            effectiveHumanFidelityReviewResolver = AnalysisEffectiveHumanFidelityReviewResolver { evidenceArtifactId, derivativeGenerationId ->
                when (val outcome = hfrCoordinator.projectEffectiveReview(evidenceArtifactId, derivativeGenerationId)) {
                    is TierBEffectiveHumanFidelityReviewOutcome.Projected -> outcome.summary.projection.effectiveState
                    is TierBEffectiveHumanFidelityReviewOutcome.TargetResolutionFailed -> null
                    TierBEffectiveHumanFidelityReviewOutcome.FailedClosed -> null
                }
            },
        )
        return Fixture(documentAnalysisCoordinator, hfrCoordinator, generationStorage, contentStorage, model)
    }

    /** An admitted Tier B OCR generation that is simultaneously (a) `UNVERIFIED_LITERAL_TRANSCRIPTION` with non-null provider provenance -- triggering the existing acknowledgement gate -- and (b) resolvable by [TierBOcrHumanFidelityReviewCoordinator] (OCR in its transformation history, non-null processing provenance) -- exactly [DocumentAnalysisCoordinatorTest]'s own `admitTierBOcr(providerIdentity = ...)` shape. */
    private suspend fun admitExternalTranscription(
        fixture: Fixture,
        derivativeGenerationId: DerivativeGenerationId,
        evidenceArtifactId: EvidenceArtifactId,
        recognisedText: String = "Recognised text.",
        providerIdentity: String = "provider-x",
    ): DerivativeGenerationRecord {
        val extracted = TierADerivativePayloadFixtures.ocr().copy(
            recognisedText = recognisedText,
            fidelity = TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
            segments = listOf(OcrRecognitionSegment(recognisedText, TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, 1)),
            providerProvenance = OcrProviderProvenance(
                providerIdentity, "adapter", "1.0.0", "literal-v1", "model", OcrModelSnapshot.NotExposed, "correlation-$providerIdentity",
            ),
            pageAccounting = OcrPageAccounting(
                OcrPageScope(listOf(1)), OcrPageScope(listOf(1)), OcrPageScope(listOf(1)),
                listOf(OcrPageOutcome(1, OcrPageOutcomeKind.TRANSCRIBED)),
            ),
            processingProvenance = OcrProcessingProvenance(
                evidenceArtifactId, OcrSha256Digest("a".repeat(64)), "application/pdf", 10,
                OcrPageScope(listOf(1)), OcrPageScope(listOf(1)), "application/pdf", 10,
                OcrSha256Digest("a".repeat(64)), true, "external-transcription.direct-byte-exact-v1", Instant.EPOCH,
            ),
            recognisedAt = Instant.EPOCH,
        )
        val record = DerivativeGenerationTest.record(derivativeGenerationId.value).copy(
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            derivativeKind = "OCR recognised text",
            producerIdentity = extracted.producerIdentity,
            transformationHistory = extracted.transformationHistory,
            completenessState = extracted.completenessState,
            warnings = extracted.warnings,
        )
        fixture.contentStorage.prepare(DerivativeContentEntry(derivativeGenerationId, evidenceArtifactId, TierADerivativePayload.Ocr(extracted)))
        fixture.contentStorage.publishPrepared(derivativeGenerationId)
        fixture.generationStorage.prepare(record)
        fixture.generationStorage.publishPrepared(derivativeGenerationId)
        return record
    }

    private fun passSubmission(pages: List<Int> = listOf(1)) = TierBHumanFidelityReviewSubmission(
        HumanFidelityReviewState.HUMAN_REVIEWED_PASS, pages, "Verbatim and accurate against the source.",
    )

    @Test
    fun `HUMAN_REVIEWED_PASS exact generation does not require the unverified acknowledgement gate`(
        @org.junit.jupiter.api.io.TempDir directory: Path,
    ) = runTest {
        val fixture = fixture(directory, "pass-waives-gate")
        val evidence = EvidenceArtifactId("evidence-pass-1")
        val generation = DerivativeGenerationId("generation-pass-1")
        admitExternalTranscription(fixture, generation, evidence)
        assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(fixture.hfrCoordinator.recordReview(evidence, generation, passSubmission()))

        val outcome = fixture.documentAnalysisCoordinator.analyse(
            owner,
            OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, generation, acknowledgesUnverifiedExternalTranscription = false)), "Analyse"),
        )
        assertIs<DocumentAnalysisOutcome.Completed>(outcome)
    }

    @Test
    fun `UNREVIEWED machine transcription still requires the existing acknowledgement`(
        @org.junit.jupiter.api.io.TempDir directory: Path,
    ) = runTest {
        val fixture = fixture(directory, "unreviewed-requires-ack")
        val evidence = EvidenceArtifactId("evidence-unreviewed-1")
        val generation = DerivativeGenerationId("generation-unreviewed-1")
        admitExternalTranscription(fixture, generation, evidence)
        // No review recorded for this generation at all.

        val withoutAck = fixture.documentAnalysisCoordinator.analyse(
            owner,
            OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, generation, acknowledgesUnverifiedExternalTranscription = false)), "Analyse"),
        )
        val failed = assertIs<DocumentAnalysisOutcome.UnverifiedExternalAcknowledgementRequired>(withoutAck)
        assertEquals(evidence, failed.evidenceArtifactId)
        assertEquals(generation, failed.derivativeGenerationId)

        val withAck = fixture.documentAnalysisCoordinator.analyse(
            owner,
            OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, generation, acknowledgesUnverifiedExternalTranscription = true)), "Analyse"),
        )
        assertIs<DocumentAnalysisOutcome.Completed>(withAck)
    }

    @Test
    fun `review of sibling generation does not remove the gate`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "sibling-does-not-satisfy")
        val evidence = EvidenceArtifactId("evidence-sibling-1")
        val targetGeneration = DerivativeGenerationId("generation-sibling-target")
        val siblingGeneration = DerivativeGenerationId("generation-sibling-other")
        admitExternalTranscription(fixture, targetGeneration, evidence, recognisedText = "Target text.")
        admitExternalTranscription(fixture, siblingGeneration, evidence, recognisedText = "Sibling text.")
        assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(
            fixture.hfrCoordinator.recordReview(evidence, siblingGeneration, passSubmission()),
        )

        val outcome = fixture.documentAnalysisCoordinator.analyse(
            owner,
            OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, targetGeneration, acknowledgesUnverifiedExternalTranscription = false)), "Analyse"),
        )
        assertIs<DocumentAnalysisOutcome.UnverifiedExternalAcknowledgementRequired>(outcome)
    }

    @Test
    fun `review of different evidence does not remove the gate`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "different-evidence-does-not-satisfy")
        val targetEvidence = EvidenceArtifactId("evidence-target-1")
        val targetGeneration = DerivativeGenerationId("generation-target-1")
        val otherEvidence = EvidenceArtifactId("evidence-other-1")
        val otherGeneration = DerivativeGenerationId("generation-other-1")
        admitExternalTranscription(fixture, targetGeneration, targetEvidence, recognisedText = "Target text.")
        admitExternalTranscription(fixture, otherGeneration, otherEvidence, recognisedText = "Other text.")
        assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(
            fixture.hfrCoordinator.recordReview(otherEvidence, otherGeneration, passSubmission()),
        )

        val outcome = fixture.documentAnalysisCoordinator.analyse(
            owner,
            OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(targetEvidence, targetGeneration, acknowledgesUnverifiedExternalTranscription = false)), "Analyse"),
        )
        assertIs<DocumentAnalysisOutcome.UnverifiedExternalAcknowledgementRequired>(outcome)
    }

    @Test
    fun `HUMAN_REVIEWED_PASS analysis preserves machine provenance, carries effective HFR state, keeps exact binding, and mutates nothing`(
        @org.junit.jupiter.api.io.TempDir directory: Path,
    ) = runTest {
        val fixture = fixture(directory, "pass-preserves-everything")
        val evidence = EvidenceArtifactId("evidence-preserve-1")
        val generation = DerivativeGenerationId("generation-preserve-1")
        admitExternalTranscription(fixture, generation, evidence, recognisedText = "Preserved text.", providerIdentity = "provider-preserve")
        assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(fixture.hfrCoordinator.recordReview(evidence, generation, passSubmission()))

        val beforeContent = fixture.contentStorage.retrieve(generation)
        val beforeGeneration = fixture.generationStorage.retrieve(generation)
        val beforeProjection = fixture.hfrCoordinator.projectEffectiveReview(evidence, generation)

        val outcome = fixture.documentAnalysisCoordinator.analyse(
            owner,
            OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, generation, acknowledgesUnverifiedExternalTranscription = false)), "Analyse"),
        )
        val item = assertIs<DocumentAnalysisOutcome.Completed>(outcome).result.evidenceItems.single()

        // Exact-generation binding remains intact.
        assertEquals(evidence, item.evidenceArtifactId)
        assertEquals(generation, item.derivativeGenerationId)

        // Machine-generated provenance preserved -- never rewritten because of the HFR PASS.
        assertEquals(AnalysisAcquisitionMechanism.EXTERNAL_TRANSCRIPTION, item.assurance.mechanism)
        assertEquals("provider-preserve", item.assurance.providerIdentity)
        assertEquals(TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION, item.assurance.fidelity)

        // The effective HFR PASS fact is carried into the analysis context as a distinct field,
        // never folded into or overwriting the provenance fields asserted above.
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_PASS, item.assurance.effectiveHumanFidelityReviewState)

        // Zero mutation: derivative content and derivative generation record are byte-for-byte/
        // field-for-field identical before and after analysis.
        assertEquals(beforeContent, fixture.contentStorage.retrieve(generation))
        assertEquals(beforeGeneration, fixture.generationStorage.retrieve(generation))

        // The HFR projection itself (no equals() override -- compared field-by-field) is unchanged:
        // no new review or discrepancy was recorded merely by projecting/analysing.
        val beforeSummary = assertIs<TierBEffectiveHumanFidelityReviewOutcome.Projected>(beforeProjection).summary
        val afterSummary = assertIs<TierBEffectiveHumanFidelityReviewOutcome.Projected>(
            fixture.hfrCoordinator.projectEffectiveReview(evidence, generation),
        ).summary
        assertEquals(beforeSummary.projection.effectiveState, afterSummary.projection.effectiveState)
        assertEquals(beforeSummary.projection.applicableReviewIds, afterSummary.projection.applicableReviewIds)
        assertEquals(beforeSummary.projection.discrepancyIds, afterSummary.projection.discrepancyIds)
        assertEquals(beforeSummary.materialDiscrepancyCount, afterSummary.materialDiscrepancyCount)
    }

    @Test
    fun `existing processed-document analysis without an HFR resolver remains unchanged`(
        @org.junit.jupiter.api.io.TempDir directory: Path,
    ) = runTest {
        val fixture = fixture(directory, "no-hfr-resolver-regression")
        // The pre-ANALYSIS-INGESTION-2 shape: no resolver configured at all.
        val coordinatorWithoutResolver = DocumentAnalysisCoordinator(
            permissionEngine = FakePermissionEngine(PermissionDecisionOutcome.APPROVED),
            tierAContentRetrievalCoordinator = TierAContentRetrievalCoordinator(fixture.generationStorage, fixture.contentStorage),
            tierBOcrContentRetrievalCoordinator = TierBOcrContentRetrievalCoordinator(fixture.generationStorage, fixture.contentStorage),
            modelInferenceClient = fixture.model,
            promptBuilder = DefaultDocumentAnalysisPromptBuilder(),
            modelTimeoutMs = 30_000L,
        )
        val evidence = EvidenceArtifactId("evidence-regression-1")
        val generation = DerivativeGenerationId("generation-regression-1")
        admitExternalTranscription(fixture, generation, evidence)
        // Even with a real, durably-recorded PASS review, a coordinator with no resolver configured
        // must still require the acknowledgement -- never assume PASS when the capability itself is
        // absent, exactly matching this coordinator's behaviour before this unit.
        assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(fixture.hfrCoordinator.recordReview(evidence, generation, passSubmission()))

        val withoutAck = coordinatorWithoutResolver.analyse(
            owner,
            OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, generation, acknowledgesUnverifiedExternalTranscription = false)), "Analyse"),
        )
        assertIs<DocumentAnalysisOutcome.UnverifiedExternalAcknowledgementRequired>(withoutAck)

        val withAck = coordinatorWithoutResolver.analyse(
            owner,
            OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, generation, acknowledgesUnverifiedExternalTranscription = true)), "Analyse"),
        )
        val item = assertIs<DocumentAnalysisOutcome.Completed>(withAck).result.evidenceItems.single()
        assertEquals(null, item.assurance.effectiveHumanFidelityReviewState)
    }
}
