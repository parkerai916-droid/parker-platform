package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*
import kotlin.test.*

class DefaultGovernedHumanCorrectionServiceTest {
    @TempDir lateinit var directory: Path
    private val owner = HumanFidelityReviewFixture.reviewer
    private val now = Instant.parse("2026-09-04T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun `exact R6 correction converges create-once and remains durable after restart`() = runTest {
        val fixture = fixture("r6")
        val request = request(fixture.review)
        val created = assertIs<GovernedHumanCorrectionResult.Created>(fixture.service.create(request)).representation
        assertEquals(2, created.proposals.size)
        assertEquals("Execution page 1 — Michael Gary Kellec", literalText(created.correctedTranscriptionBlocks[0]))
        assertEquals("Execution page 5 — Michael Gary Kellec", literalText(created.correctedTranscriptionBlocks[4]))
        assertEquals(fixture.provider.transcriptionBlocks.slice(1..3), created.correctedTranscriptionBlocks.slice(1..3))
        assertEquals("Execution page 1 — Michael Gary Kellee", literalText(fixture.provider.transcriptionBlocks[0]))
        assertEquals("Execution page 5 — Michael Gary Kellee", literalText(fixture.provider.transcriptionBlocks[4]))
        assertEquals(envelopeWithoutLiteral(fixture.provider.transcriptionBlocks[0]), envelopeWithoutLiteral(created.correctedTranscriptionBlocks[0]))
        assertEquals(envelopeWithoutLiteral(fixture.provider.transcriptionBlocks[4]), envelopeWithoutLiteral(created.correctedTranscriptionBlocks[4]))
        assertEquals(2, fixture.audit.listForRepresentation(created.derivativeGenerationId).size)
        assertIs<GovernedHumanCorrectionResult.AlreadyCreated>(fixture.service.create(request))

        val restartedStorage = FileSystemHumanCorrectedRepresentationStorage(fixture.correctedRoot)
        val restarted = assertNotNull(restartedStorage.retrieve(created.derivativeGenerationId))
        assertContentEquals(HumanCorrectedRepresentationCodec.encode(created), HumanCorrectedRepresentationCodec.encode(restarted))
        val eligibility = DefaultHumanCorrectedRepresentationEligibilityEvaluator(fixture.reviews, fixture.projector).evaluate(restarted)
        assertEquals(SourceConfirmedEligibilityState.ALLOWED, eligibility.state)
        assertEquals(2, fixture.reviews.retrieve(fixture.review.reviewId)!!.discrepancyOccurrences.size)
    }

    @Test
    fun `Unicode code-point ranges apply to literal text rather than the V8 envelope`() {
        val literal = "😀 Michael Gary Kellee remains"
        val start = literal.codePoints().toArray().indexOfFirst { it == 'K'.code }
        val corrected = replaceExactCodePointRange(literal, start, start + 6, "Kellee", "Kellec")
        assertEquals("😀 Michael Gary Kellec remains", corrected)
        assertFails { replaceExactCodePointRange(literal, start, start + 6, "Kellec", "Kellec") }

        val envelope = envelope("a".repeat(64), 1, literal)
        assertNotEquals("Kellee", envelope.codePoints().toArray().let { points ->
            String(points, start, 6)
        })
    }

    @Test
    fun `wrong literal provider substring fails closed without corrected state`() = runTest {
        val fixture = fixture("wrong-literal")
        val wrong = fixture.provider.copy(transcriptionBlocks = fixture.provider.transcriptionBlocks.toMutableList().also {
            it[0] = envelope((11).toString().padStart(64, '0'), 1, "Execution page 1 — Michael Gary Kelley")
        })
        val service = DefaultGovernedHumanCorrectionService(allow(), fixture.reviews, fixture.projector,
            HumanCorrectionProviderResolver { ResolvedProviderTranscription(it, wrong) }, fixture.corrected, fixture.audit)
        assertEquals(GovernedHumanCorrectionResult.Failed(GovernedHumanCorrectionFailureReason.INVALID_CORRECTION),
            service.create(request(fixture.review)))
        assertEquals(0, Files.list(fixture.correctedRoot).use { it.filter(Files::isRegularFile).count() })
    }

    @Test
    fun `authority denial and exception precede every review correction and audit mutation`() = runTest {
        val fixture = fixture("denials")
        val denied = DefaultGovernedHumanCorrectionService(
            HumanCorrectionPermissionEvaluator { _, _, _ -> HumanCorrectionPermissionResult.Denied(HumanCorrectionDenialReason.WRONG_PRINCIPAL) },
            fixture.reviews, fixture.projector, HumanCorrectionProviderResolver { throw AssertionError("provider resolved before authority") },
            fixture.corrected, fixture.audit,
        )
        assertIs<GovernedHumanCorrectionResult.AuthorizationDenied>(denied.create(request(fixture.review)))
        val exceptional = DefaultGovernedHumanCorrectionService(
            HumanCorrectionPermissionEvaluator { _, _, _ -> throw IllegalStateException("unavailable") }, fixture.reviews, fixture.projector,
            HumanCorrectionProviderResolver { throw AssertionError("provider resolved before authority") }, fixture.corrected, fixture.audit,
        )
        assertEquals(GovernedHumanCorrectionResult.Failed(GovernedHumanCorrectionFailureReason.AUTHORITY_EVALUATION_FAILED),
            exceptional.create(request(fixture.review)))
        assertEquals(0, Files.list(fixture.correctedRoot).use { it.filter(Files::isRegularFile).count() })
        assertTrue(fixture.audit.listForRepresentation(DerivativeGenerationId("human-corrected-" + "0".repeat(64))).isEmpty())
    }

    @Test
    fun `wrong target review values and incomplete material resolution fail closed`() = runTest {
        val cases = listOf<(GovernedHumanCorrectionRequest) -> GovernedHumanCorrectionRequest>(
            { it.copy(target = it.target.copy(sourceSha256 = OcrSha256Digest("1".repeat(64)))) },
            { it.copy(reviewId = HumanFidelityReviewId("review-" + "1".repeat(64))) },
            { it.copy(proposals = it.proposals.dropLast(1), acceptance = acceptance(it.reviewId, it.target, it.proposals.dropLast(1))) },
        )
        cases.forEachIndexed { index, mutate ->
            val fixture = fixture("invalid-$index")
            val result = fixture.service.create(mutate(request(fixture.review)))
            assertTrue(result is GovernedHumanCorrectionResult.Failed)
            assertEquals(0, Files.list(fixture.correctedRoot).use { it.filter(Files::isRegularFile).count() })
        }
    }

    @Test
    fun `storage and canonical readback failures do not report success`() = runTest {
        val fixture = fixture("failures")
        val failing = object : HumanCorrectedRepresentationStorage {
            override suspend fun prepare(representation: HumanCorrectedRegionTranscription) = HumanCorrectedRepresentationPrepareResult.Prepared
            override suspend fun publishPrepared(id: DerivativeGenerationId) { throw IllegalStateException("publish") }
            override suspend fun retrieve(id: DerivativeGenerationId): HumanCorrectedRegionTranscription? = null
        }
        val service = DefaultGovernedHumanCorrectionService(allow(), fixture.reviews, fixture.projector,
            HumanCorrectionProviderResolver { ResolvedProviderTranscription(it, fixture.provider) }, failing, fixture.audit)
        assertEquals(GovernedHumanCorrectionResult.Failed(GovernedHumanCorrectionFailureReason.STORAGE_FAILURE),
            service.create(request(fixture.review)))

        val missing = object : HumanCorrectedRepresentationStorage by fixture.corrected {
            override suspend fun retrieve(id: DerivativeGenerationId): HumanCorrectedRegionTranscription? = null
        }
        assertEquals(GovernedHumanCorrectionResult.Failed(GovernedHumanCorrectionFailureReason.CANONICAL_READBACK_FAILED),
            DefaultGovernedHumanCorrectionService(allow(), fixture.reviews, fixture.projector,
                HumanCorrectionProviderResolver { ResolvedProviderTranscription(it, fixture.provider) }, missing, fixture.audit)
                .create(request(fixture.review)))
    }

    @Test
    fun `tampered corrected representation and conflicting duplicate fail closed`() = runTest {
        val fixture = fixture("tamper")
        val created = assertIs<GovernedHumanCorrectionResult.Created>(fixture.service.create(request(fixture.review))).representation
        val file = fixture.correctedRoot.resolve("${created.derivativeGenerationId.value}.human-corrected-v1")
        Files.write(file, Files.readAllBytes(file).copyOf(20))
        assertFails { FileSystemHumanCorrectedRepresentationStorage(fixture.correctedRoot).retrieve(created.derivativeGenerationId) }
        assertIs<GovernedHumanCorrectionResult.Failed>(fixture.service.create(request(fixture.review)))
    }

    private suspend fun fixture(name: String): Fixture {
        val review = HumanFidelityReviewFixture.review()
        val reviewRoot = Files.createDirectories(directory.resolve("$name/reviews"))
        val reviewAudit = FileSystemHumanFidelityGovernanceAudit(Files.createDirectories(directory.resolve("$name/review-audit")))
        val reviews = FileSystemHumanFidelityReviewStorage(reviewRoot, reviewAudit, clock)
        reviews.prepare(review); reviews.publishPrepared(review.reviewId)
        val correctedRoot = Files.createDirectories(directory.resolve("$name/corrected"))
        val corrected = FileSystemHumanCorrectedRepresentationStorage(correctedRoot)
        val audit = FileSystemHumanCorrectionAudit(Files.createDirectories(directory.resolve("$name/correction-audit")))
        val projector = DefaultEffectiveHumanFidelityReviewProjector(reviews)
        val provider = provider(review)
        val service = DefaultGovernedHumanCorrectionService(allow(), reviews, projector,
            HumanCorrectionProviderResolver { target -> if (target == review.target) ResolvedProviderTranscription(target, provider) else null },
            corrected, audit)
        return Fixture(review, reviews, projector, provider, corrected, audit, correctedRoot, service)
    }

    private fun request(review: HumanFidelityReviewRecord): GovernedHumanCorrectionRequest {
        val proposals = review.discrepancyOccurrences.map { discrepancy ->
            val resolution = discrepancy.sourceResolution as HumanSourceResolution.ResolvedAgainstSource
            val id = HumanTranscriptionCorrectionProposal.deriveId(review.reviewId, discrepancy.discrepancyId, review.target,
                discrepancy.location.originalProviderSubstring, resolution.assertedSourceValue, owner, now, "Accept exact source value")
            HumanTranscriptionCorrectionProposal(id, review.reviewId, discrepancy.discrepancyId, review.target,
                discrepancy.location.originalProviderSubstring, resolution.assertedSourceValue, owner, now, "Accept exact source value")
        }
        return GovernedHumanCorrectionRequest(review.target, review.reviewId, proposals,
            acceptance(review.reviewId, review.target, proposals),
            HumanCorrectionAuthorityScope(owner, HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE, review.target, review.reviewId))
    }

    private fun acceptance(reviewId: HumanFidelityReviewId, target: HumanFidelityReviewTarget,
                           proposals: List<HumanTranscriptionCorrectionProposal>): HumanTranscriptionCorrectionAcceptance {
        val ids = proposals.map { it.proposalId }
        val id = HumanTranscriptionCorrectionAcceptance.deriveId(reviewId, target, ids, owner, now)
        return HumanTranscriptionCorrectionAcceptance(id, reviewId, target, ids, owner, now, HUMAN_TRANSCRIPTION_CORRECTION_PURPOSE)
    }

    private fun provider(review: HumanFidelityReviewRecord): OrdinaryRegionTranscriptionDerivative {
        val regionIds = (1..5).map { (it + 10).toString().padStart(64, '0') }
        val blocks = (1..5).map { envelope(regionIds[it - 1], it, "Execution page $it — Michael Gary Kellee") }
        return OrdinaryRegionTranscriptionDerivative(
            representationVersion=3, capabilityId=ORDINARY_REQUEST_REGION_V8_CAPABILITY_ID, capabilityDigest="2".repeat(64),
            evidenceArtifactId=review.target.evidenceArtifactId.value, sourceSha256=review.target.sourceSha256.value,
            pageBindings=(1..5).map { "page-$it" }, regionBindings=regionIds.map { "$it|crop|image|constituents" },
            transcriptionBlocks=blocks, providerReturnedOrder=(1..5).map { "region-$it" }, parkerSourceOrder=(1..5).map { "region-$it" },
            provider="OpenAI", model="gpt-5.6-sol", adapterId="adapter", adapterVersion="1",
            providerProfile="openai-fidelity-first-transcription-v1", wireVersion=8, schemaSha256="3".repeat(64),
            instructionSha256="4".repeat(64), processingProfile="processing", requestIdentity="request", requestDigest="5".repeat(64),
            responseIdentity="response", providerStateRecordIdentity="state", capabilityAcceptanceRecordIdentity="capability",
            ownerAuthorizationIdentity="authorization", executionIdentity="execution", attemptIdentity="attempt",
            reconstructedContentDigest="6".repeat(64), canonicalGenerationKeyDigest="7".repeat(64), admissionProvenance="preserved",
            preparationIdentity=review.target.preparationIdentity.value, preparationProfile="full-page-achromatic-png-preparation-v1",
            preparationProfileVersion=1, providerBodyDigest="8".repeat(64), authorizationPurpose="evidence-intelligence.external-transcription",
            maximumProviderCalls=1, automaticRetryLimit=0, externalReasoningAuthorized=false,
        )
    }

    private fun envelope(regionId: String, page: Int, literal: String) =
        listOf(regionId, page.toString(), literal, "TRANSCRIBED", "", "").joinToString("\u001f")

    private fun literalText(envelope: String) = envelope.split('\u001f', limit = 6)[2]

    private fun envelopeWithoutLiteral(envelope: String) = envelope.split('\u001f', limit = 6).let {
        listOf(it[0], it[1], it[3], it[4], it[5])
    }

    private fun allow() = HumanCorrectionPermissionEvaluator { _, _, _ -> HumanCorrectionPermissionResult.Authorized }

    private data class Fixture(
        val review: HumanFidelityReviewRecord,
        val reviews: HumanFidelityReviewStorage,
        val projector: EffectiveHumanFidelityReviewProjector,
        val provider: OrdinaryRegionTranscriptionDerivative,
        val corrected: HumanCorrectedRepresentationStorage,
        val audit: HumanCorrectionAudit,
        val correctedRoot: Path,
        val service: GovernedHumanCorrectionService,
    )
}
