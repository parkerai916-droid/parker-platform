package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import parker.core.interfaces.*
import parker.core.runtime.*

class ParkerRuntimeHumanFidelityReviewCompositionTest {
    @Test
    fun `exact R6 review converges through production composition and survives recomposition`() = runTest {
        val roots = Roots.create()
        val first = runtime(roots)
        first.start()
        val service = first.service()
        first.registrar().register(HumanFidelityReviewFixture.target)
        val review = HumanFidelityReviewFixture.review()
        val authority = exactAuthority()

        assertEquals(
            GovernedHumanFidelityReviewRecordingResult.Recorded(review.reviewId),
            service.record(GovernedHumanFidelityReviewRecordingRequest(review, authority)),
        )
        val canonical = service.storage().retrieve(review.reviewId)
        assertNotNull(canonical)
        assertContentEquals(HumanFidelityReviewRecordCodec.encode(review), HumanFidelityReviewRecordCodec.encode(canonical))
        assertEquals(2, canonical.discrepancyOccurrences.size)
        assertEquals(1, canonical.systematicPatterns.size)
        assertTrue(canonical.discrepancyOccurrences.all { it.severity == FidelityDiscrepancySeverity.MATERIAL })
        assertTrue(canonical.discrepancyOccurrences.all { it.causeAssessment.state == FidelityCauseState.UNKNOWN })
        assertTrue(canonical.discrepancyOccurrences.all {
            (it.sourceResolution as HumanSourceResolution.ResolvedAgainstSource).assertedSourceValue == "Kellec"
        })
        assertEquals(
            GovernedHumanFidelityReviewRecordingResult.AlreadyRecorded(review.reviewId),
            service.record(GovernedHumanFidelityReviewRecordingRequest(review, authority)),
        )
        assertEquals(3, service.audit().listForReview(review.reviewId).size)
        val projected = assertIs<EffectiveHumanFidelityReviewProjectionOutcome.Projected>(
            first.projector().project(
                review.target,
                HumanFidelityEligibilityUse.SOURCE_CONFIRMED_WHOLE_GENERATION,
            ),
        ).summary
        assertEquals(HumanFidelityReviewState.HUMAN_REVIEWED_WITH_DISCREPANCY, projected.projection.effectiveState)
        assertEquals(
            SourceConfirmedEligibility(
                SourceConfirmedEligibilityState.DENIED,
                SourceConfirmedDenialReason.MATERIAL_DISCREPANCY,
            ),
            projected.projection.eligibility,
        )
        first.shutdown()

        val second = runtime(roots)
        second.start()
        second.registrar().register(HumanFidelityReviewFixture.target)
        val recomposedService = second.service()
        assertContentEquals(
            HumanFidelityReviewRecordCodec.encode(review),
            HumanFidelityReviewRecordCodec.encode(requireNotNull(recomposedService.storage().retrieve(review.reviewId))),
        )
        assertEquals(
            GovernedHumanFidelityReviewRecordingResult.AlreadyRecorded(review.reviewId),
            recomposedService.record(GovernedHumanFidelityReviewRecordingRequest(review, authority)),
        )
        second.shutdown()
    }

    @Test
    fun `purpose owner action and exact target are production composed and fail closed`() = runTest {
        val roots = Roots.create()
        val runtime = runtime(roots)
        runtime.start()
        val service = runtime.service()
        val review = HumanFidelityReviewFixture.review()

        assertTrue(runtime.purposeRegistry().isActive(HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE))
        assertNotNull(runtime.actionVocabulary().lookup(HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME))
        runtime.registrar().register(review.target)

        val deniedAuthorities = listOf(
            exactAuthority().copy(principalId = PrincipalId("owner.not-configured")),
            exactAuthority().copy(authorizationPurpose = null),
            exactAuthority().copy(authorizationPurpose = AuthorizationPurposeId("evidence-intelligence.external-transcription")),
            exactAuthority().copy(target = review.target.copy(evidenceArtifactId = EvidenceArtifactId("evidence-${"1".repeat(64)}"))),
            exactAuthority().copy(target = review.target.copy(sourceSha256 = OcrSha256Digest("2".repeat(64)))),
            exactAuthority().copy(target = review.target.copy(preparationIdentity = OcrSha256Digest("3".repeat(64)))),
            exactAuthority().copy(target = review.target.copy(derivativeGenerationId = DerivativeGenerationId("region-${"4".repeat(64)}"))),
            exactAuthority().copy(target = review.target.copy(derivativeGenerationSha256 = OcrSha256Digest("5".repeat(64)))),
            exactAuthority().copy(target = review.target.copy(derivativeContentSha256 = OcrSha256Digest("6".repeat(64)))),
        )
        deniedAuthorities.forEach { authority ->
            assertIs<GovernedHumanFidelityReviewRecordingResult.AuthorizationDenied>(
                service.record(GovernedHumanFidelityReviewRecordingRequest(review, authority)),
            )
            assertEquals(0, roots.reviewFacts())
            assertEquals(0, roots.auditFacts())
        }

        runtime.purposeRegistry().retire(HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE)
        assertIs<GovernedHumanFidelityReviewRecordingResult.AuthorizationDenied>(
            service.record(GovernedHumanFidelityReviewRecordingRequest(review, exactAuthority())),
        )
        assertEquals(0, roots.reviewFacts())
        assertEquals(0, roots.auditFacts())
        runtime.shutdown()
    }

    @Test
    fun `missing or unusable persistent roots fail closed without fallback`() = runTest {
        val missing = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(loaderEnvironment().minus(
                ParkerRuntimeConfigLoader.KEY_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT,
            ))
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT, missing.key)

        val roots = Roots.create()
        val unusable = roots.base.resolve("missing-review-root")
        val runtime = ParkerRuntime(config(roots).copy(humanFidelityReviewStorageRootPath = unusable.toString()), RecordingParkerLogger())
        val failure = assertFailsWith<ParkerRuntimeException.DependencyConstructionFailed> { runtime.start() }
        assertEquals("Human fidelity review storage construction", failure.component)
        assertFalse(Files.exists(unusable))
        assertEquals(0, roots.reviewFacts())
        assertEquals(0, roots.auditFacts())
    }

    @Test
    fun `composed capability has no provider correction projector or public write route`() {
        val serviceFields = DefaultGovernedHumanFidelityReviewRecordingService::class.java.declaredFields.map { it.type.name }
        assertTrue(serviceFields.all { "Provider" !in it && "Correction" !in it && "Projection" !in it })
        assertFalse(ParkerRuntime::class.java.methods.any {
            it.name.contains("HumanFidelity", ignoreCase = true) || it.name.contains("ReviewRecording", ignoreCase = true)
        })
    }

    private fun runtime(roots: Roots) = ParkerRuntime(config(roots), RecordingParkerLogger())

    private fun config(roots: Roots) = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate",
        modelName = "offline-test-model",
        ownerPrincipalId = HumanFidelityReviewFixture.reviewer.value,
        localTextChannelModuleId = "channel.a5-human-fidelity-test",
        evidenceStorageRootPath = roots.directory("evidence").toString(),
        evidenceDeletionAuditLogPath = roots.directory("evidence-audit").resolve("audit.log").toString(),
        evidenceSourceManifestStorageRootPath = roots.directory("manifests").toString(),
        derivativeGenerationStorageRootPath = roots.directory("generations").toString(),
        derivativeContentStorageRootPath = roots.directory("content").toString(),
        savedAnalysisStorageRootPath = roots.directory("analyses").toString(),
        documentIngestionAuditLogPath = roots.directory("ingestion-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = roots.directory("memory").resolve("memory.log").toString(),
        knowledgeItemDurabilityLogPath = roots.directory("knowledge").resolve("knowledge.log").toString(),
        humanFidelityReviewStorageRootPath = roots.reviews.toString(),
        humanFidelityGovernanceAuditStorageRootPath = roots.audit.toString(),
    )

    private fun loaderEnvironment(): Map<String, String> {
        val roots = Roots.create()
        val config = config(roots)
        return mapOf(
            ParkerRuntimeConfigLoader.KEY_MODEL_ENDPOINT_URL to config.modelEndpointUrl,
            ParkerRuntimeConfigLoader.KEY_MODEL_NAME to config.modelName,
            ParkerRuntimeConfigLoader.KEY_OWNER_PRINCIPAL_ID to config.ownerPrincipalId,
            ParkerRuntimeConfigLoader.KEY_EVIDENCE_STORAGE_ROOT to config.evidenceStorageRootPath,
            ParkerRuntimeConfigLoader.KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH to config.evidenceDeletionAuditLogPath,
            ParkerRuntimeConfigLoader.KEY_EVIDENCE_SOURCE_MANIFEST_STORAGE_ROOT to config.evidenceSourceManifestStorageRootPath,
            ParkerRuntimeConfigLoader.KEY_DERIVATIVE_GENERATION_STORAGE_ROOT to config.derivativeGenerationStorageRootPath,
            ParkerRuntimeConfigLoader.KEY_DERIVATIVE_CONTENT_STORAGE_ROOT to config.derivativeContentStorageRootPath,
            ParkerRuntimeConfigLoader.KEY_SAVED_ANALYSIS_STORAGE_ROOT to config.savedAnalysisStorageRootPath,
            ParkerRuntimeConfigLoader.KEY_DOCUMENT_INGESTION_AUDIT_LOG_PATH to config.documentIngestionAuditLogPath,
            ParkerRuntimeConfigLoader.KEY_MEMORY_CORE_DURABILITY_LOG_PATH to config.memoryCoreDurabilityLogPath,
            ParkerRuntimeConfigLoader.KEY_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH to config.knowledgeItemDurabilityLogPath,
            ParkerRuntimeConfigLoader.KEY_HUMAN_FIDELITY_REVIEW_STORAGE_ROOT to requireNotNull(config.humanFidelityReviewStorageRootPath),
            ParkerRuntimeConfigLoader.KEY_HUMAN_FIDELITY_GOVERNANCE_AUDIT_STORAGE_ROOT to requireNotNull(config.humanFidelityGovernanceAuditStorageRootPath),
        )
    }

    private fun exactAuthority() = HumanFidelityReviewRecordingAuthorityScope(
        HumanFidelityReviewFixture.reviewer,
        HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE,
        HumanFidelityReviewFixture.target,
    )

    private fun ParkerRuntime.service(): DefaultGovernedHumanFidelityReviewRecordingService =
        requireNotNull(privateField<GovernedHumanFidelityReviewRecordingService?>("humanFidelityReviewRecordingService"))
            as DefaultGovernedHumanFidelityReviewRecordingService

    private fun ParkerRuntime.registrar(): HumanFidelityReviewExactTargetRegistrar =
        requireNotNull(privateField("humanFidelityReviewExactTargetRegistrar"))

    private fun ParkerRuntime.projector(): EffectiveHumanFidelityReviewProjector =
        requireNotNull(privateField("effectiveHumanFidelityReviewProjector"))

    private fun ParkerRuntime.purposeRegistry(): AuthorizationPurposeRegistry =
        permissionPolicy().privateField("authorizationPurposeRegistry")

    private fun ParkerRuntime.actionVocabulary(): ActionVocabulary =
        permissionPolicy().privateField<ActionMapper>("actionMapper").privateField("vocabulary")

    private fun ParkerRuntime.permissionPolicy(): DefaultPermissionPolicy =
        privateField<PermissionEngine>("permissionEngine").privateField("policy")

    private fun DefaultGovernedHumanFidelityReviewRecordingService.storage(): HumanFidelityReviewStorage =
        privateField("storage")

    private fun DefaultGovernedHumanFidelityReviewRecordingService.audit(): HumanFidelityGovernanceAudit =
        storage().privateField("audit")

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    private data class Roots(val base: Path, val reviews: Path, val audit: Path) {
        fun directory(name: String): Path = base.resolve(name).also(Files::createDirectories)
        fun reviewFacts(): Long = Files.list(reviews).use { paths -> paths.filter(Files::isRegularFile).count() }
        fun auditFacts(): Long = Files.list(audit).use { paths -> paths.filter(Files::isRegularFile).count() }

        companion object {
            fun create(): Roots {
                val base = Files.createTempDirectory("a5-human-fidelity-composition")
                return Roots(base, base.resolve("reviews").also(Files::createDirectories),
                    base.resolve("audit").also(Files::createDirectories))
            }
        }
    }
}
