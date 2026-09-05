package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.composition.HumanFidelityReviewExactTargetRegistrar
import parker.core.interfaces.*

/**
 * HFR Owner UI exposure scope lock amendment
 * (`HUMAN_FIDELITY_REVIEW_OWNER_UI_EXPOSURE_SCOPE_LOCK_AMENDMENT.md`). Behavioural tests for
 * [TierBOcrHumanFidelityReviewCoordinator] -- the shortest governed path from an exact Tier B OCR
 * derivative generation to the existing, unmodified Human Fidelity Review domain. Real,
 * filesystem-backed storage throughout (temp roots), a real [DefaultPermissionEngine] wired
 * exactly like [DefaultGovernedHumanFidelityReviewRecordingServiceTest]'s own
 * `exactTargetPermissionEngine` helper -- never mocked, mirroring every other coordinator test in
 * this file's own established style.
 */
class TierBOcrHumanFidelityReviewCoordinatorTest {
    private val owner = PrincipalId("owner.tierb-hfr-test")
    private val clock = Clock.fixed(Instant.parse("2026-09-05T00:00:00Z"), ZoneOffset.UTC)

    private class Fixture(
        val coordinator: TierBOcrHumanFidelityReviewCoordinator,
        val generationStorage: FileSystemDerivativeGenerationStorage,
        val contentStorage: FileSystemDerivativeContentStorage,
        val reviewStorage: FileSystemHumanFidelityReviewStorage,
        val audit: FileSystemHumanFidelityGovernanceAudit,
        val reviewRoot: Path,
    )

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
        vocabulary.register(ActionVocabularyEntry(
            HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
            setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT)),
        ))
        val rules = listOf(PermissionPolicyRule(
            PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED,
            PermissionLevel.HIGH_ASSURANCE, HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE,
            HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
        ))
        val permissionEngine = DefaultPermissionEngine(
            identities, DefaultPermissionPolicy(ActionMapper(vocabulary), resources, rules, purposeRegistry),
        )
        val permissionPolicy = HumanFidelityReviewRecordingPermissionPolicy(owner, purposeRegistry, permissionEngine, clock)
        val recordingService = DefaultGovernedHumanFidelityReviewRecordingService(permissionPolicy, reviewStorage)
        val registrar = HumanFidelityReviewExactTargetRegistrar(resources, owner, clock::instant)

        val coordinator = TierBOcrHumanFidelityReviewCoordinator(
            generationStorage, contentStorage, recordingService, registrar, projector, owner, clock::instant,
        )
        return Fixture(coordinator, generationStorage, contentStorage, reviewStorage, audit, reviewRoot)
    }

    private fun accounting(page: Int = 1) = OcrPageAccounting(
        OcrPageScope(listOf(page)), OcrPageScope(listOf(page)), OcrPageScope(listOf(page)),
        listOf(OcrPageOutcome(page, OcrPageOutcomeKind.TRANSCRIBED)),
    )

    private fun processing(evidenceArtifactId: EvidenceArtifactId, sourceDigest: String = "a".repeat(64), representationDigest: String = "b".repeat(64)) =
        OcrProcessingProvenance(
            evidenceArtifactId, OcrSha256Digest(sourceDigest), "application/pdf", 10,
            OcrPageScope(listOf(1)), OcrPageScope(listOf(1)), "application/pdf", 10,
            OcrSha256Digest(representationDigest), true, "direct-v1", Instant.EPOCH,
        )

    private fun provider() = OcrProviderProvenance("OpenAI", "adapter", "1.0.0", "profile", "returned-model", OcrModelSnapshot.NotExposed, "response-id")
    private fun producer() = DerivativeProducerIdentity("external", "1.0.0", "profile", "adapter", "1.0.0", "returned-model", null)

    private fun extractedResult(text: String, evidenceArtifactId: EvidenceArtifactId, page: Int = 1) = OcrDerivativeExtractedResult(
        text, TranscriptionFidelity.VERBATIM, OcrDerivativeOutcomeKind.RECOGNISED, null, emptyList(),
        listOf(OcrRecognitionSegment(text, TranscriptionFidelity.VERBATIM, page)),
        producer(), listOf(DerivativeTransformation.OCR, DerivativeTransformation.MODEL_INFERENCE),
        DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, accounting(page), processing(evidenceArtifactId), provider(), Instant.EPOCH,
    )

    private suspend fun admit(
        fixture: Fixture,
        derivativeGenerationId: DerivativeGenerationId,
        evidenceArtifactId: EvidenceArtifactId,
        text: String = "Recognised text.",
    ): DerivativeGenerationRecord {
        val extracted = extractedResult(text, evidenceArtifactId)
        val record = DerivativeGenerationRecord(
            derivativeGenerationId, evidenceArtifactId, listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            "External transcription recognised text", producer(), extracted.transformationHistory, Instant.EPOCH,
            DerivativeContentIdentity.NoCanonicalSerialization, extracted.completenessState, DerivativeOperationalOutcome.USABLE,
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

    private fun discrepancySubmission(text: String, pages: List<Int> = listOf(1)) = TierBHumanFidelityReviewSubmission(
        HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY, pages, "One material discrepancy against the source.",
        listOf(TierBFidelityDiscrepancySubmission(
            1, 0, text.codePointCount(0, text.length).coerceAtMost(3),
            FidelityDiscrepancyClassification.TRANSCRIPTION_DIFFERENCE, FidelityDiscrepancySeverity.MATERIAL,
            "Name misspelled relative to the source",
        )),
    )

    @Test
    fun `exact generation binding resolves and records a governed review through the existing HFR domain`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "exact-binding")
        val evidenceArtifactId = EvidenceArtifactId("evidence-tierb-hfr-a")
        val derivativeGenerationId = DerivativeGenerationId("generation-tierb-hfr-a")
        admit(fixture, derivativeGenerationId, evidenceArtifactId)

        val outcome = assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(
            fixture.coordinator.recordReview(evidenceArtifactId, derivativeGenerationId, passSubmission()),
        )

        val canonical = assertNotNull(fixture.reviewStorage.retrieve(outcome.reviewId))
        assertEquals(evidenceArtifactId, canonical.target.evidenceArtifactId)
        assertEquals(derivativeGenerationId, canonical.target.derivativeGenerationId)
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_PASS, canonical.reviewState)
    }

    @Test
    fun `wrong evidence artefact fails closed with SourceMismatch and records nothing`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "wrong-evidence")
        val realEvidenceArtifactId = EvidenceArtifactId("evidence-tierb-hfr-real")
        val derivativeGenerationId = DerivativeGenerationId("generation-tierb-hfr-real")
        admit(fixture, derivativeGenerationId, realEvidenceArtifactId)

        val wrongEvidenceArtifactId = EvidenceArtifactId("evidence-tierb-hfr-wrong")
        val outcome = assertIs<TierBHumanFidelityReviewRecordingOutcome.TargetResolutionFailed>(
            fixture.coordinator.recordReview(wrongEvidenceArtifactId, derivativeGenerationId, passSubmission()),
        )
        assertEquals(TierBHumanFidelityReviewTargetResolution.SourceMismatch, outcome.resolution)
        assertTrue(fixture.reviewRoot.let { Files.list(it).use { s -> s.filter(Files::isRegularFile).findAny().isEmpty } })
        assertEquals(0, fixture.audit.let { audit -> Files.list(directory.resolve("wrong-evidence/audit")).use { s -> s.filter(Files::isRegularFile).count() } })
    }

    @Test
    fun `unknown generation id fails closed with UnknownGeneration and records nothing`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "unknown-generation")
        val outcome = assertIs<TierBHumanFidelityReviewRecordingOutcome.TargetResolutionFailed>(
            fixture.coordinator.recordReview(EvidenceArtifactId("evidence-none"), DerivativeGenerationId("generation-never-admitted"), passSubmission()),
        )
        assertEquals(TierBHumanFidelityReviewTargetResolution.UnknownGeneration, outcome.resolution)
    }

    @Test
    fun `review persists through the existing HumanFidelityReviewStorage and is retrievable by exact target`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "persistence")
        val evidenceArtifactId = EvidenceArtifactId("evidence-tierb-hfr-persist")
        val derivativeGenerationId = DerivativeGenerationId("generation-tierb-hfr-persist")
        admit(fixture, derivativeGenerationId, evidenceArtifactId)

        val outcome = assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(
            fixture.coordinator.recordReview(evidenceArtifactId, derivativeGenerationId, passSubmission()),
        )
        val projected = assertIs<TierBEffectiveHumanFidelityReviewOutcome.Projected>(
            fixture.coordinator.projectEffectiveReview(evidenceArtifactId, derivativeGenerationId),
        )
        val byTarget = fixture.reviewStorage.listForExactTarget(projected.summary.projection.target)
        assertEquals(1, byTarget.size)
        assertEquals(outcome.reviewId, byTarget.single().reviewId)

        // Recording the identical submission again is idempotent through the existing service --
        // never a second review, never a second audit publication fact.
        val again = assertIs<TierBHumanFidelityReviewRecordingOutcome.AlreadyRecorded>(
            fixture.coordinator.recordReview(evidenceArtifactId, derivativeGenerationId, passSubmission()),
        )
        assertEquals(outcome.reviewId, again.reviewId)
        assertEquals(1, fixture.reviewStorage.listForExactTarget(projected.summary.projection.target).size)
    }

    @Test
    fun `effective review projects HUMAN_REVIEWED_WITH_DISCREPANCY correctly through the existing projector`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "projection")
        val evidenceArtifactId = EvidenceArtifactId("evidence-tierb-hfr-projection")
        val derivativeGenerationId = DerivativeGenerationId("generation-tierb-hfr-projection")
        admit(fixture, derivativeGenerationId, evidenceArtifactId, text = "Kellec attended the meeting.")

        val before = assertIs<TierBEffectiveHumanFidelityReviewOutcome.Projected>(
            fixture.coordinator.projectEffectiveReview(evidenceArtifactId, derivativeGenerationId),
        )
        assertEquals(HumanFidelityReviewState.UNREVIEWED, before.summary.projection.effectiveState)

        assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(
            fixture.coordinator.recordReview(evidenceArtifactId, derivativeGenerationId, discrepancySubmission("Kellec attended the meeting.")),
        )

        val after = assertIs<TierBEffectiveHumanFidelityReviewOutcome.Projected>(
            fixture.coordinator.projectEffectiveReview(evidenceArtifactId, derivativeGenerationId),
        )
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY, after.summary.projection.effectiveState)
        assertEquals(1, after.summary.materialDiscrepancyCount)
    }

    @Test
    fun `one generation's review cannot contaminate another generation's projection`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "isolation")
        val evidenceA = EvidenceArtifactId("evidence-tierb-hfr-isolation-a")
        val generationA = DerivativeGenerationId("generation-tierb-hfr-isolation-a")
        val evidenceB = EvidenceArtifactId("evidence-tierb-hfr-isolation-b")
        val generationB = DerivativeGenerationId("generation-tierb-hfr-isolation-b")
        admit(fixture, generationA, evidenceA, text = "Text for generation A.")
        admit(fixture, generationB, evidenceB, text = "Text for generation B.")

        assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(
            fixture.coordinator.recordReview(evidenceA, generationA, passSubmission()),
        )

        val projectionA = assertIs<TierBEffectiveHumanFidelityReviewOutcome.Projected>(fixture.coordinator.projectEffectiveReview(evidenceA, generationA))
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_PASS, projectionA.summary.projection.effectiveState)

        val projectionB = assertIs<TierBEffectiveHumanFidelityReviewOutcome.Projected>(fixture.coordinator.projectEffectiveReview(evidenceB, generationB))
        assertEquals(HumanFidelityReviewState.UNREVIEWED, projectionB.summary.projection.effectiveState, "generation B must remain unreviewed after only generation A was reviewed")
    }

    @Test
    fun `recording a review never mutates the underlying derivative generation or content bytes`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "no-mutation")
        val evidenceArtifactId = EvidenceArtifactId("evidence-tierb-hfr-no-mutation")
        val derivativeGenerationId = DerivativeGenerationId("generation-tierb-hfr-no-mutation")
        val record = admit(fixture, derivativeGenerationId, evidenceArtifactId)
        val entryBefore = assertNotNull(fixture.contentStorage.retrieve(derivativeGenerationId))
        val generationBytesBefore = DerivativeGenerationRecordCodec.encode(record)
        val contentBytesBefore = DerivativeContentCodec.encode(entryBefore)

        assertIs<TierBHumanFidelityReviewRecordingOutcome.Recorded>(
            fixture.coordinator.recordReview(evidenceArtifactId, derivativeGenerationId, discrepancySubmission("Recognised text.")),
        )

        val recordAfter = assertNotNull(fixture.generationStorage.retrieve(derivativeGenerationId))
        val entryAfter = assertNotNull(fixture.contentStorage.retrieve(derivativeGenerationId))
        assertContentEquals(generationBytesBefore, DerivativeGenerationRecordCodec.encode(recordAfter))
        assertContentEquals(contentBytesBefore, DerivativeContentCodec.encode(entryAfter))
    }

    @Test
    fun `coordinator holds no provider dependency of any kind`() {
        val fieldTypes = TierBOcrHumanFidelityReviewCoordinator::class.java.declaredFields.map { it.type.name }
        assertTrue(fieldTypes.none { "Provider" in it || "Transport" in it || "OpenAi" in it })
    }

    @Test
    fun `invalid submission shapes fail closed with InvalidSubmission and record nothing`(@org.junit.jupiter.api.io.TempDir directory: Path) = runTest {
        val fixture = fixture(directory, "invalid-submission")
        val evidenceArtifactId = EvidenceArtifactId("evidence-tierb-hfr-invalid")
        val derivativeGenerationId = DerivativeGenerationId("generation-tierb-hfr-invalid")
        admit(fixture, derivativeGenerationId, evidenceArtifactId)

        val wrongOutcome = TierBHumanFidelityReviewSubmission(HumanFidelityReviewState.UNREVIEWED, listOf(1), "n/a")
        assertIs<TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission>(
            fixture.coordinator.recordReview(evidenceArtifactId, derivativeGenerationId, wrongOutcome),
        )

        val passWithDiscrepancy = TierBHumanFidelityReviewSubmission(
            HumanFidelityReviewState.HUMAN_REVIEWED_PASS, listOf(1), "n/a",
            listOf(TierBFidelityDiscrepancySubmission(1, 0, 1, FidelityDiscrepancyClassification.TRANSCRIPTION_DIFFERENCE, FidelityDiscrepancySeverity.MINOR, "x")),
        )
        assertIs<TierBHumanFidelityReviewRecordingOutcome.InvalidSubmission>(
            fixture.coordinator.recordReview(evidenceArtifactId, derivativeGenerationId, passWithDiscrepancy),
        )

        val projected = assertIs<TierBEffectiveHumanFidelityReviewOutcome.Projected>(
            fixture.coordinator.projectEffectiveReview(evidenceArtifactId, derivativeGenerationId),
        )
        assertTrue(fixture.reviewStorage.listForExactTarget(projected.summary.projection.target).isEmpty())
    }
}
