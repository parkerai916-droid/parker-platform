package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.EvidenceAcceptanceResult
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidenceAnalysisResult
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.EvidenceIntelligence
import parker.core.interfaces.EvidenceManifestRetrievalResult
import parker.core.interfaces.EvidenceRetrievalResult
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ReasoningContext
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.RelationshipEndpoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Evidence Intelligence, Implementation Unit 5 ("The Evidence Intelligence
 * Operation"). Behavioural tests for [DefaultEvidenceIntelligence],
 * demonstrating -- not merely asserting -- that it assembles Units 2-4
 * exactly as `docs/implementation/EVIDENCE_INTELLIGENCE_IMPLEMENTATION_PLAN.md`
 * §8 Unit 5 and
 * `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_5_PLANNING_AND_BOUNDARY_REVIEW.md`
 * require: orchestration and sequencing (resolve, then optionally reason,
 * then convert), partial completion, the documented, permanent
 * `ReasoningContext(emptyList())` limitation, and dependency boundaries
 * (no acceptance interface, no Permission Engine, no runtime composition,
 * no new public type).
 */
class DefaultEvidenceIntelligenceTest {

    private val principalId = PrincipalId("owner-1")

    private fun request(
        evidenceArtifactIds: List<EvidenceArtifactId> = emptyList(),
        memoryCoreReferences: List<RelationshipEndpoint> = emptyList(),
    ) = EvidenceAnalysisRequest(
        analysisKind = "comparison",
        requestingPrincipalId = principalId,
        evidenceArtifactIds = evidenceArtifactIds,
        memoryCoreReferences = memoryCoreReferences,
    )

    private fun resolverOf(evidenceCustodian: EvidenceCustodian) =
        EvidenceIntelligenceInputResolver(evidenceCustodian, FakeMemoryRetrievalForUnit5())

    // ================= Orchestration and sequencing =================

    @Test
    fun `analyse resolves inputs before ever invoking a Reasoning Provider`() = runTest {
        val callOrder = mutableListOf<String>()
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id ->
            callOrder += "resolve"
            EvidenceRetrievalResult.Found(id, byteArrayOf(1))
        }
        val reasoningProvider = FakeReasoningProvider {
            callOrder += "reason"
            ReasoningProviderResponse.NoAction
        }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))

        assertEquals(listOf("resolve", "reason"), callOrder)
    }

    @Test
    fun `analyse rejects a request naming nothing to analyse before any resolution or reasoning occurs`() = runTest {
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, _ -> throw AssertionError("must not be called") }
        val reasoningProvider = FakeReasoningProvider { throw AssertionError("must not be called") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        assertFailsWith<IllegalArgumentException> { evidenceIntelligence.analyse(request()) }
    }

    @Test
    fun `a Reply becomes exactly one TransientOutput labelled model-generated, citing every resolved reference`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val endpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val memoryRetrieval = FakeMemoryRetrievalForUnit5(onGetEntity = { _, _ -> parker.core.interfaces.Entity(
            entityId = parker.core.interfaces.EntityId("entity-1"),
            entityType = "person",
            primaryLabel = "Alex",
            provenanceId = parker.core.interfaces.ProvenanceId("provenance-1"),
            createdAt = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        ) })
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("the two invoices agree") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            EvidenceIntelligenceInputResolver(evidenceCustodian, memoryRetrieval),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(
            request(evidenceArtifactIds = listOf(artifactId), memoryCoreReferences = listOf(endpoint)),
        )

        val output = assertIs<EvidenceAnalysisResult.TransientOutput>(results.single())
        assertEquals("[${AnalyticalOutputDiscipline.MODEL_GENERATED}] the two invoices agree", output.text)
        assertEquals(listOf(artifactId), output.evidenceArtifactReferences)
        assertEquals(listOf(endpoint), output.memoryCoreReferences)
    }

    @Test
    fun `a NoAction response produces an empty result list, never a fabricated TransientOutput`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))

        assertEquals(emptyList(), results)
    }

    @Test
    fun `a Goal response is treated as an implementation-level anomaly, never silently coerced into a result`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Goal("investigate discrepancy") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        assertFailsWith<UnsupportedOperationException> {
            evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))
        }
    }

    // ================= Reasoning Provider optionality =================

    @Test
    fun `with no Reasoning Provider configured, analyse never invokes reasoning and returns an empty list`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            reasoningCoordinator = null,
        )

        val results = evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))

        assertEquals(emptyList(), results)
    }

    // ================= Partial completion =================

    @Test
    fun `with some inputs resolved and others missing, only the resolved subset is cited -- never the missing ones`() = runTest {
        val foundArtifact = EvidenceArtifactId("artifact-found")
        val missingArtifact = EvidenceArtifactId("artifact-missing")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id ->
            if (id == foundArtifact) EvidenceRetrievalResult.Found(id, byteArrayOf(1)) else EvidenceRetrievalResult.NotFound(id)
        }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("partial finding") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(
            request(evidenceArtifactIds = listOf(foundArtifact, missingArtifact)),
        )

        val output = assertIs<EvidenceAnalysisResult.TransientOutput>(results.single())
        assertEquals(listOf(foundArtifact), output.evidenceArtifactReferences)
        assertTrue(missingArtifact !in output.evidenceArtifactReferences)
    }

    @Test
    fun `when every referenced input fails to resolve, analyse returns an empty list and never invokes reasoning`() = runTest {
        val missingArtifact = EvidenceArtifactId("artifact-missing")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.NotFound(id) }
        val reasoningProvider = FakeReasoningProvider { throw AssertionError("must not be called -- nothing resolved to reason about") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(missingArtifact)))

        assertEquals(emptyList(), results)
    }

    @Test
    fun `a missing Memory Core reference is never cited, while a resolved one alongside it still is`() = runTest {
        val foundEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-found")
        val missingEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-missing")
        val entity = parker.core.interfaces.Entity(
            entityId = parker.core.interfaces.EntityId("entity-found"),
            entityType = "person",
            primaryLabel = "Alex",
            provenanceId = parker.core.interfaces.ProvenanceId("provenance-1"),
            createdAt = java.time.Instant.parse("2026-01-01T00:00:00Z"),
        )
        val memoryRetrieval = FakeMemoryRetrievalForUnit5(onGetEntity = { _, id -> if (id.value == "entity-found") entity else null })
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.NotFound(id) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("finding") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            EvidenceIntelligenceInputResolver(evidenceCustodian, memoryRetrieval),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        val results = evidenceIntelligence.analyse(
            request(memoryCoreReferences = listOf(foundEndpoint, missingEndpoint)),
        )

        val output = assertIs<EvidenceAnalysisResult.TransientOutput>(results.single())
        assertEquals(listOf(foundEndpoint), output.memoryCoreReferences)
    }

    // ================= The documented ReasoningContext limitation =================

    @Test
    fun `analyse always supplies an empty ReasoningContext to the Reasoning Provider invocation, never one derived from the request`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val nestedContext = ReasoningContext(listOf("must never reach the Reasoning Provider"))
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.NoAction }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        evidenceIntelligence.analyse(
            EvidenceAnalysisRequest(
                analysisKind = "comparison",
                requestingPrincipalId = principalId,
                evidenceArtifactIds = listOf(artifactId),
                reasoningContext = nestedContext,
            ),
        )

        assertEquals(ReasoningContext(emptyList()), reasoningProvider.lastRequest?.reasoningContext)
    }

    // ================= Dependency boundaries =================

    @Test
    fun `the constructor accepts exactly EvidenceIntelligenceInputResolver, a nullable EvidenceIntelligenceReasoningCoordinator, and a nullable EvidenceIntelligenceOcrCoordinator`() {
        // OCR Mechanism, Unit 12 ("Runtime Composition"): updated from this test's own original,
        // pre-Unit-12 two-parameter assertion, per docs/architecture/OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md
        // Section 5.E's own explicitly-authorised third, nullable constructor parameter -- the same
        // "an environment with no OCR composition remains a structurally valid DefaultEvidenceIntelligence,
        // exactly as one with no reasoning provider already is" nullability discipline the second
        // parameter already established.
        // A default value on the third parameter (Section 5.E's own nullable-with-default
        // discipline) makes Kotlin emit a second, synthetic constructor overload (an extra `int`
        // bitmask + `DefaultConstructorMarker` parameter pair) alongside the real one -- declaredConstructors
        // now returns two entries where it previously returned one; this must select the real,
        // three-Parker-type constructor explicitly rather than assume there is exactly one.
        val constructor = DefaultEvidenceIntelligence::class.java.declaredConstructors
            .single { it.parameterTypes.none { type -> type.simpleName == "DefaultConstructorMarker" } }
        val parameterTypes = constructor.parameterTypes.map { it.simpleName }.toSet()

        assertEquals(
            setOf("EvidenceIntelligenceInputResolver", "EvidenceIntelligenceReasoningCoordinator", "EvidenceIntelligenceOcrCoordinator"),
            parameterTypes,
        )
    }

    @Test
    fun `DefaultEvidenceIntelligence declares no field of any acceptance-interface or PermissionEngine type`() {
        val forbiddenTypeNames = setOf(
            "EvidenceCustodian",
            "MemoryCore",
            "PermissionEngine",
            "ExecutionPipeline",
            "PlannerRuntime",
        )
        val fieldTypeNames = DefaultEvidenceIntelligence::class.java.declaredFields.map { it.type.simpleName }.toSet()

        assertTrue(
            fieldTypeNames.none { it in forbiddenTypeNames },
            "declared fields were: $fieldTypeNames",
        )
    }

    @Test
    fun `DefaultEvidenceIntelligence implements EvidenceIntelligence and no other declared interface`() {
        val implementedInterfaces = DefaultEvidenceIntelligence::class.java.interfaces.map { it.simpleName }

        assertEquals(listOf("EvidenceIntelligence"), implementedInterfaces)
    }

    @Test
    fun `a Reasoning Provider fault propagates unchanged, never caught or converted into a fabricated result`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-1")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, byteArrayOf(1)) }
        val reasoningProvider = FakeReasoningProvider { throw IllegalStateException("model unreachable") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
        )

        assertFailsWith<IllegalStateException> {
            evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))
        }
    }

    // ================= OCR Mechanism, Unit 12 ("Runtime Composition") wiring =================

    private fun ocrRequest(evidenceArtifactIds: List<EvidenceArtifactId>) = EvidenceAnalysisRequest(
        analysisKind = "ocr-transcription",
        requestingPrincipalId = principalId,
        evidenceArtifactIds = evidenceArtifactIds,
    )

    private fun ocrResult(text: String = "recognised text") = parker.core.interfaces.OcrRecognitionResult(
        recognisedText = text,
        fidelity = parker.core.interfaces.TranscriptionFidelity.VERBATIM,
        identity = parker.core.interfaces.OcrRecognitionIdentity(mechanismIdentity = "docling", configurationProfile = "test"),
        recognisedAt = java.time.Instant.EPOCH,
    )

    @Test
    fun `an OCR-eligible analysisKind with a resolved artefact invokes the OcrMechanism and produces a labelled TransientOutput`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-ocr-1")
        val content = "scanned content".toByteArray()
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, content) }
        val ocrMechanism = FakeOcrMechanismForUnit12 { OcrRecognitionOutcome.Recognised(ocrResult("PARKER OCR TEXT")) }
        val ocrCoordinator = EvidenceIntelligenceOcrCoordinator(FakeManifestOnlyEvidenceCustodian(content), ocrMechanism)
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            reasoningCoordinator = null,
            ocrCoordinator = ocrCoordinator,
        )

        val results = evidenceIntelligence.analyse(ocrRequest(listOf(artifactId)))

        assertEquals(1, ocrMechanism.invocationCount)
        assertEquals(1, results.size)
        val transientOutput = assertIs<EvidenceAnalysisResult.TransientOutput>(results.single())
        assertTrue(transientOutput.text.contains("PARKER OCR TEXT"))
        assertTrue(transientOutput.text.startsWith("[${AnalyticalOutputDiscipline.EXTRACTED}]"), "OCR text must be labelled EXTRACTED, never MODEL_GENERATED -- got: ${transientOutput.text}")
        assertEquals(listOf(artifactId), transientOutput.evidenceArtifactReferences)
    }

    @Test
    fun `a non-OCR-eligible analysisKind never invokes the OcrMechanism, even with ocrCoordinator present`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-non-ocr")
        val content = "some content".toByteArray()
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, content) }
        val ocrMechanism = FakeOcrMechanismForUnit12 { throw AssertionError("must not be called for a non-OCR-eligible analysisKind") }
        val ocrCoordinator = EvidenceIntelligenceOcrCoordinator(FakeManifestOnlyEvidenceCustodian(content), ocrMechanism)
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            reasoningCoordinator = null,
            ocrCoordinator = ocrCoordinator,
        )

        val results = evidenceIntelligence.analyse(request(evidenceArtifactIds = listOf(artifactId)))

        assertEquals(0, ocrMechanism.invocationCount)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `a null ocrCoordinator never attempts OCR, even for an OCR-eligible analysisKind -- no regression to the pre-Unit-12 constructor shape`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-no-coordinator")
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, "content".toByteArray()) }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            reasoningCoordinator = null,
            ocrCoordinator = null,
        )

        val results = evidenceIntelligence.analyse(ocrRequest(listOf(artifactId)))

        assertTrue(results.isEmpty(), "with no ocrCoordinator and no reasoningCoordinator, behaviour must be unchanged from before this Unit: an empty list")
    }

    @Test
    fun `a PartialOrDegradedOutput OCR outcome still produces a TransientOutput, carrying the actual partial text`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-partial")
        val content = "content".toByteArray()
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, content) }
        val ocrMechanism = FakeOcrMechanismForUnit12 {
            OcrRecognitionOutcome.PartialOrDegradedOutput(ocrResult("partial text only"), "page 2 unreadable")
        }
        val ocrCoordinator = EvidenceIntelligenceOcrCoordinator(FakeManifestOnlyEvidenceCustodian(content), ocrMechanism)
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            reasoningCoordinator = null,
            ocrCoordinator = ocrCoordinator,
        )

        val results = evidenceIntelligence.analyse(ocrRequest(listOf(artifactId)))

        val transientOutput = assertIs<EvidenceAnalysisResult.TransientOutput>(results.single())
        assertTrue(transientOutput.text.contains("partial text only"), "the actual partial text must be preserved, never discarded")
    }

    @Test
    fun `a non-success OcrRecognitionOutcome contributes nothing to the result list -- silence, never a fabricated success`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-failure")
        val content = "content".toByteArray()
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, content) }
        val ocrMechanism = FakeOcrMechanismForUnit12 { OcrRecognitionOutcome.ProcessingOrDependencyFailure("timed out") }
        val ocrCoordinator = EvidenceIntelligenceOcrCoordinator(FakeManifestOnlyEvidenceCustodian(content), ocrMechanism)
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            reasoningCoordinator = null,
            ocrCoordinator = ocrCoordinator,
        )

        val results = evidenceIntelligence.analyse(ocrRequest(listOf(artifactId)))

        assertTrue(results.isEmpty())
    }

    @Test
    fun `OCR results and reasoning results are both present when both coordinators are wired`() = runTest {
        val artifactId = EvidenceArtifactId("artifact-both")
        val content = "content".toByteArray()
        val evidenceCustodian = FakeEvidenceCustodianForUnit5 { _, id -> EvidenceRetrievalResult.Found(id, content) }
        val ocrMechanism = FakeOcrMechanismForUnit12 { OcrRecognitionOutcome.Recognised(ocrResult("ocr text")) }
        val ocrCoordinator = EvidenceIntelligenceOcrCoordinator(FakeManifestOnlyEvidenceCustodian(content), ocrMechanism)
        val reasoningProvider = FakeReasoningProvider { ReasoningProviderResponse.Reply("reasoning text") }
        val evidenceIntelligence: EvidenceIntelligence = DefaultEvidenceIntelligence(
            resolverOf(evidenceCustodian),
            EvidenceIntelligenceReasoningCoordinator(reasoningProvider),
            ocrCoordinator,
        )

        val results = evidenceIntelligence.analyse(ocrRequest(listOf(artifactId)))

        assertEquals(2, results.size)
        assertTrue(results.any { it is EvidenceAnalysisResult.TransientOutput && it.text.contains("ocr text") })
        assertTrue(results.any { it is EvidenceAnalysisResult.TransientOutput && it.text.contains("reasoning text") })
    }

    // ================= Fakes =================

    private class FakeOcrMechanismForUnit12(
        private val onRecognise: (parker.core.interfaces.OcrRecognitionRequest) -> OcrRecognitionOutcome,
    ) : parker.core.interfaces.OcrMechanism {
        var invocationCount: Int = 0
            private set

        override suspend fun recognise(request: parker.core.interfaces.OcrRecognitionRequest): OcrRecognitionOutcome {
            invocationCount += 1
            return onRecognise(request)
        }
    }

    /** Supplies only [EvidenceCustodian.retrieveManifest] -- a manifest whose sha256/byteLength
     * genuinely match [content] and whose receivedMediaType is OCR-eligible -- so
     * [EvidenceIntelligenceOcrCoordinator]'s own integrity sequence passes without needing the
     * full manifest-construction machinery [EvidenceIntelligenceOcrCoordinatorTest] already
     * exercises directly. [retrieve]/[accept] are never called by this coordinator and throw if
     * reached. */
    private class FakeManifestOnlyEvidenceCustodian(private val content: ByteArray) : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult =
            throw UnsupportedOperationException("must not be called")

        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult =
            throw UnsupportedOperationException("must not be called")

        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult {
            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
            return EvidenceManifestRetrievalResult.Found(
                parker.core.interfaces.EvidenceSourceManifest(
                    evidenceArtifactId = evidenceArtifactId,
                    sha256 = digest,
                    byteLength = content.size.toLong(),
                    receivedMediaType = "application/pdf",
                ),
            )
        }
    }

    private class FakeEvidenceCustodianForUnit5(
        private val onRetrieve: (PrincipalId, EvidenceArtifactId) -> EvidenceRetrievalResult,
    ) : EvidenceCustodian {
        override suspend fun accept(
            requestingPrincipalId: PrincipalId,
            candidate: CandidateEvidenceArtifact,
        ): EvidenceAcceptanceResult = throw UnsupportedOperationException("Unit 5 must never call EvidenceCustodian.accept")

        override suspend fun retrieve(
            requestingPrincipalId: PrincipalId,
            evidenceArtifactId: EvidenceArtifactId,
        ): EvidenceRetrievalResult = onRetrieve(requestingPrincipalId, evidenceArtifactId)

        override suspend fun retrieveManifest(
            requestingPrincipalId: PrincipalId,
            evidenceArtifactId: EvidenceArtifactId,
        ): EvidenceManifestRetrievalResult = throw UnsupportedOperationException("Unit 5 must never call EvidenceCustodian.retrieveManifest")
    }

    private class FakeMemoryRetrievalForUnit5(
        private val onGetEntity: (PrincipalId, parker.core.interfaces.EntityId) -> parker.core.interfaces.Entity? =
            { _, _ -> null },
    ) : parker.core.interfaces.MemoryRetrieval {
        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: parker.core.interfaces.EntityId) =
            onGetEntity(requestingPrincipalId, entityId)

        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: parker.core.interfaces.DocumentId) = null

        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: parker.core.interfaces.AssertionId) = null

        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: parker.core.interfaces.RelationshipId) = null

        override suspend fun findEntities(query: parker.core.interfaces.EntityLookupQuery): List<parker.core.interfaces.Entity> =
            throw UnsupportedOperationException("Unit 5 must never call findEntities")

        override suspend fun findDocuments(query: parker.core.interfaces.DocumentLookupQuery): List<parker.core.interfaces.Document> =
            throw UnsupportedOperationException("Unit 5 must never call findDocuments")

        override suspend fun traverseRelationships(query: parker.core.interfaces.RelationshipTraversalQuery): List<parker.core.interfaces.Relationship> =
            throw UnsupportedOperationException("Unit 5 must never call traverseRelationships")

        override suspend fun findByTimeRange(query: parker.core.interfaces.ChronologicalLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("Unit 5 must never call findByTimeRange")

        override suspend fun findByMetadata(query: parker.core.interfaces.MetadataLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("Unit 5 must never call findByMetadata")

        override suspend fun findByProvenance(query: parker.core.interfaces.ProvenanceLookupQuery): List<MemoryCoreRecord> =
            throw UnsupportedOperationException("Unit 5 must never call findByProvenance")
    }
}
