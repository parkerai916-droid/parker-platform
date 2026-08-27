package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DecisionId
import parker.core.interfaces.DerivativeContentEntry
import parker.core.interfaces.DerivativeContentStorage
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeGenerationTest
import parker.core.interfaces.DerivativeParentReference
import parker.core.interfaces.DocumentAnalysisOutcome
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceGenerationSelection
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.OwnerDocumentAnalysisRequest
import parker.core.interfaces.OcrModelSnapshot
import parker.core.interfaces.OcrProviderProvenance
import parker.core.interfaces.OcrPageAccounting
import parker.core.interfaces.OcrPageOutcome
import parker.core.interfaces.OcrPageOutcomeKind
import parker.core.interfaces.OcrPageScope
import parker.core.interfaces.OcrProcessingProvenance
import parker.core.interfaces.OcrSha256Digest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionExplanation
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.TierADerivativePayload
import parker.core.interfaces.TierADerivativePayloadFixtures

/**
 * Minimum Production Document Pipeline — Local Reasoning Implementation.
 * Behavioural tests for [DocumentAnalysisCoordinator]: authorisation,
 * derivative resolution (Tier B first, Tier A fallback), bounds
 * enforcement, and honest, non-leaking failure reporting. Real,
 * filesystem-backed [FileSystemDerivativeGenerationStorage]/
 * [FileSystemDerivativeContentStorage] (temp roots), shared between the two
 * content-retrieval coordinators exactly as production wiring shares them
 * -- mirroring [TierBOcrContentRetrievalCoordinatorTest]'s own established
 * style. Only [PermissionEngine]/[ModelInferenceClient] are faked.
 */
class DocumentAnalysisCoordinatorTest {

    /** The exact, governed Tier A derivativeKind literals `DerivativeGenerationCoordinator`'s own ingestCsv/ingestEml/ingestDocx/ingestPdf admit with (src/runtime/DerivativeGenerationCoordinator.kt) -- fixtures below use these, never an invented value, so the coordinator's own kind-vs-payload validation (§3 of the correction pass) is exercised honestly rather than accidentally bypassed. */
    private companion object {
        const val PDF_KIND = "Searchable PDF literal text"
        const val CSV_KIND = "CSV structure"
        const val EML_KIND = "EML MIME structure"
        const val DOCX_KIND = "DOCX OOXML structure"
    }

    private fun storages(): Pair<FileSystemDerivativeGenerationStorage, DerivativeContentStorage> {
        val generationStorage = FileSystemDerivativeGenerationStorage(Files.createTempDirectory("doc-analysis-generation"))
        val contentStorage = FileSystemDerivativeContentStorage(Files.createTempDirectory("doc-analysis-content"))
        return generationStorage to contentStorage
    }

    private suspend fun admitTierA(
        generationStorage: FileSystemDerivativeGenerationStorage,
        contentStorage: DerivativeContentStorage,
        id: DerivativeGenerationId,
        evidenceArtifactId: EvidenceArtifactId,
        payload: TierADerivativePayload,
        derivativeKind: String,
    ): DerivativeGenerationRecord {
        val record = DerivativeGenerationTest.record(id.value).copy(
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            derivativeKind = derivativeKind,
        )
        contentStorage.prepare(DerivativeContentEntry(id, evidenceArtifactId, payload))
        contentStorage.publishPrepared(id)
        generationStorage.prepare(record)
        generationStorage.publishPrepared(id)
        return record
    }

    private suspend fun admitTierBOcr(
        generationStorage: FileSystemDerivativeGenerationStorage,
        contentStorage: DerivativeContentStorage,
        id: DerivativeGenerationId,
        evidenceArtifactId: EvidenceArtifactId,
        recognisedText: String = TierADerivativePayloadFixtures.ocr().recognisedText,
        providerIdentity: String? = null,
    ): DerivativeGenerationRecord {
        val extracted = TierADerivativePayloadFixtures.ocr().copy(
            recognisedText = recognisedText,
            fidelity = if (providerIdentity == null) TierADerivativePayloadFixtures.ocr().fidelity else parker.core.interfaces.TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
            segments = if (providerIdentity == null) TierADerivativePayloadFixtures.ocr().segments else listOf(
                parker.core.interfaces.OcrRecognitionSegment(
                    recognisedText,
                    parker.core.interfaces.TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
                    1,
                ),
            ),
            providerProvenance = providerIdentity?.let {
                OcrProviderProvenance(it, "adapter", "1.0.0", "literal-v1", "model", OcrModelSnapshot.NotExposed, "correlation-$it")
            },
            pageAccounting = providerIdentity?.let {
                OcrPageAccounting(OcrPageScope(listOf(1)), OcrPageScope(listOf(1)), OcrPageScope(listOf(1)), listOf(OcrPageOutcome(1, OcrPageOutcomeKind.TRANSCRIBED)))
            },
            processingProvenance = providerIdentity?.let {
                OcrProcessingProvenance(
                    evidenceArtifactId, OcrSha256Digest("a".repeat(64)), "application/pdf", 10,
                    OcrPageScope(listOf(1)), OcrPageScope(listOf(1)), "application/pdf", 10,
                    OcrSha256Digest("a".repeat(64)), true, "external-transcription.direct-byte-exact-v1", Instant.EPOCH,
                )
            },
            recognisedAt = providerIdentity?.let { Instant.EPOCH },
        )
        val record = DerivativeGenerationTest.record(id.value).copy(
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
            derivativeKind = "OCR recognised text",
            producerIdentity = extracted.producerIdentity,
            transformationHistory = extracted.transformationHistory,
            completenessState = extracted.completenessState,
            warnings = extracted.warnings,
        )
        contentStorage.prepare(DerivativeContentEntry(id, evidenceArtifactId, TierADerivativePayload.Ocr(extracted)))
        contentStorage.publishPrepared(id)
        generationStorage.prepare(record)
        generationStorage.publishPrepared(id)
        return record
    }

    private class FakePermissionEngine(private val outcome: PermissionDecisionOutcome) : PermissionEngine {
        var evaluateCallCount: Int = 0
            private set

        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
            evaluateCallCount += 1
            return PermissionDecision(
                decisionId = DecisionId("fake-decision"),
                principalId = request.principalId,
                resourceId = request.targetResources.first(),
                action = PermissionAction.EXECUTE,
                decision = outcome,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.EPOCH,
            )
        }

        override suspend fun explain(decisionId: DecisionId): PermissionExplanation =
            throw UnsupportedOperationException("must never be called")
    }

    private class FakeModelInferenceClient(
        private val onInfer: suspend (String) -> String = { "fake analysis response" },
    ) : ModelInferenceClient {
        var invocationCount: Int = 0
            private set
        val receivedPrompts = mutableListOf<String>()

        override suspend fun infer(prompt: String): String {
            invocationCount += 1
            receivedPrompts += prompt
            return onInfer(prompt)
        }
    }

    private fun coordinator(
        generationStorage: FileSystemDerivativeGenerationStorage,
        contentStorage: DerivativeContentStorage,
        permissionEngine: PermissionEngine = FakePermissionEngine(PermissionDecisionOutcome.APPROVED),
        modelInferenceClient: ModelInferenceClient = FakeModelInferenceClient(),
    ) = DocumentAnalysisCoordinator(
        permissionEngine = permissionEngine,
        tierAContentRetrievalCoordinator = TierAContentRetrievalCoordinator(generationStorage, contentStorage),
        tierBOcrContentRetrievalCoordinator = TierBOcrContentRetrievalCoordinator(generationStorage, contentStorage),
        modelInferenceClient = modelInferenceClient,
        promptBuilder = DefaultDocumentAnalysisPromptBuilder(),
        modelTimeoutMs = 30_000L,
    )

    private val owner = PrincipalId("owner-1")

    @Test
    fun `Unit L exact selected OCR generation controls analysed content and provenance despite newer provider-coexisting generations`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidence = EvidenceArtifactId("evidence-with-three-generations")
        val generationA = DerivativeGenerationId("generation-a-local")
        val generationB = DerivativeGenerationId("generation-b-external")
        val generationC = DerivativeGenerationId("generation-c-external-newest")

        // Publication order is deliberate: C is newest. A is local OCR; B and C are separate
        // generations from the same external provider. Selection must remain ID-driven only.
        admitTierBOcr(generationStorage, contentStorage, generationA, evidence, "GENERATION_A_TEXT")
        admitTierBOcr(generationStorage, contentStorage, generationB, evidence, "GENERATION_B_TEXT", "provider-one")
        admitTierBOcr(generationStorage, contentStorage, generationC, evidence, "GENERATION_C_TEXT", "provider-one")

        listOf(
            generationA to "GENERATION_A_TEXT",
            generationB to "GENERATION_B_TEXT",
            generationC to "GENERATION_C_TEXT",
            // Re-select the oldest after both newer generations exist.
            generationA to "GENERATION_A_TEXT",
        ).forEach { (selectedGeneration, selectedMarker) ->
            val model = FakeModelInferenceClient()
            val outcome = coordinator(generationStorage, contentStorage, modelInferenceClient = model).analyse(
                owner,
                OwnerDocumentAnalysisRequest(
                    listOf(EvidenceGenerationSelection(evidence, selectedGeneration, selectedGeneration != generationA)),
                    "Analyse the selected generation",
                ),
            )

            val completed = assertIs<DocumentAnalysisOutcome.Completed>(outcome)
            val prompt = model.receivedPrompts.single()
            assertTrue(selectedMarker in prompt)
            listOf("GENERATION_A_TEXT", "GENERATION_B_TEXT", "GENERATION_C_TEXT")
                .filterNot { it == selectedMarker }
                .forEach { assertFalse(it in prompt, "unselected generation content must not reach analysis") }
            assertEquals(evidence, completed.result.evidenceItems.single().evidenceArtifactId)
            assertEquals(selectedGeneration, completed.result.evidenceItems.single().derivativeGenerationId)
        }
    }

    @Test
    fun `unverified external analysis requires acknowledgement bound to exact pair`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidence = EvidenceArtifactId("evidence-ack")
        val generation = DerivativeGenerationId("generation-ack")
        admitTierBOcr(generationStorage, contentStorage, generation, evidence, "EXTERNAL_TEXT", "OpenAI")
        val model = FakeModelInferenceClient()
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = model)

        assertEquals(
            DocumentAnalysisOutcome.UnverifiedExternalAcknowledgementRequired(evidence, generation),
            coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, generation)), "Analyse")),
        )
        assertEquals(0, model.invocationCount)

        val completed = coord.analyse(
            owner,
            OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, generation, true)), "Analyse"),
        )
        assertIs<DocumentAnalysisOutcome.Completed>(completed)
        assertEquals(1, model.invocationCount)
    }

    @Test
    fun `Unit L wrong evidence pairing and missing generation never substitute a coexisting generation`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidence = EvidenceArtifactId("evidence-real")
        val generation = DerivativeGenerationId("generation-real")
        admitTierBOcr(generationStorage, contentStorage, generation, evidence, "GENERATION_REAL_TEXT")
        val model = FakeModelInferenceClient()
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = model)

        assertEquals(
            DocumentAnalysisOutcome.SourceMismatch(EvidenceArtifactId("evidence-wrong"), generation),
            coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(EvidenceArtifactId("evidence-wrong"), generation)), "Analyse")),
        )
        val missing = DerivativeGenerationId("generation-missing")
        assertEquals(
            DocumentAnalysisOutcome.UnknownGeneration(missing),
            coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidence, missing)), "Analyse")),
        )
        assertEquals(0, model.invocationCount)
    }

    // ================= A =================

    @Test
    fun `A one Tier A document produces a completed analysis result`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-pdf")
        val id = DerivativeGenerationId("gen-pdf")
        admitTierA(generationStorage, contentStorage, id, evidenceArtifactId, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()), PDF_KIND)
        val modelClient = FakeModelInferenceClient(onInfer = { "The document discusses X." })
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = modelClient)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, id)), "Summarise this document"))

        val completed = assertIs<DocumentAnalysisOutcome.Completed>(outcome)
        assertEquals("The document discusses X.", completed.result.analysisText)
        assertEquals(1, modelClient.invocationCount)
        assertEquals(1, completed.result.evidenceItems.size)
    }

    // ================= B =================

    @Test
    fun `B multiple Tier A documents produce one bounded analysis, both documents present in the prompt`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceA = EvidenceArtifactId("evidence-pdf-1")
        val evidenceB = EvidenceArtifactId("evidence-pdf-2")
        val idA = DerivativeGenerationId("gen-pdf-1")
        val idB = DerivativeGenerationId("gen-pdf-2")
        admitTierA(generationStorage, contentStorage, idA, evidenceA, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf("First document text.")), PDF_KIND)
        admitTierA(generationStorage, contentStorage, idB, evidenceB, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf("Second document text.")), PDF_KIND)
        val modelClient = FakeModelInferenceClient()
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = modelClient)

        val outcome = coord.analyse(
            owner,
            OwnerDocumentAnalysisRequest(
                listOf(EvidenceGenerationSelection(evidenceA, idA), EvidenceGenerationSelection(evidenceB, idB)),
                "Compare these documents",
            ),
        )

        assertIs<DocumentAnalysisOutcome.Completed>(outcome)
        assertEquals(1, modelClient.invocationCount)
        val prompt = modelClient.receivedPrompts.single()
        assertTrue("First document text." in prompt)
        assertTrue("Second document text." in prompt)
        assertTrue("\"index\":1" in prompt)
        assertTrue("\"index\":2" in prompt)
    }

    // ================= C =================

    @Test
    fun `C mixed Tier A and Tier B OCR derivatives are both retrieved and analysed together`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceA = EvidenceArtifactId("evidence-csv")
        val evidenceB = EvidenceArtifactId("evidence-ocr")
        val idA = DerivativeGenerationId("gen-csv")
        val idB = DerivativeGenerationId("gen-ocr")
        admitTierA(generationStorage, contentStorage, idA, evidenceA, TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()), CSV_KIND)
        admitTierBOcr(generationStorage, contentStorage, idB, evidenceB)
        val modelClient = FakeModelInferenceClient()
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = modelClient)

        val outcome = coord.analyse(
            owner,
            OwnerDocumentAnalysisRequest(
                listOf(EvidenceGenerationSelection(evidenceA, idA), EvidenceGenerationSelection(evidenceB, idB)),
                "Summarise",
            ),
        )

        val completed = assertIs<DocumentAnalysisOutcome.Completed>(outcome)
        assertEquals(2, completed.result.evidenceItems.size)
        val prompt = modelClient.receivedPrompts.single()
        assertTrue(TierADerivativePayloadFixtures.ocr().recognisedText in prompt)
    }

    // ================= D =================

    @Test
    fun `D exact EvidenceArtifactId and DerivativeGenerationId references survive into the result`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-refs")
        val id = DerivativeGenerationId("gen-refs")
        admitTierA(generationStorage, contentStorage, id, evidenceArtifactId, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()), PDF_KIND)
        val coord = coordinator(generationStorage, contentStorage)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, id)), "Summarise"))

        val completed = assertIs<DocumentAnalysisOutcome.Completed>(outcome)
        val item = completed.result.evidenceItems.single()
        assertEquals(evidenceArtifactId, item.evidenceArtifactId)
        assertEquals(id, item.derivativeGenerationId)
    }

    // ================= E =================

    @Test
    fun `E a source-generation mismatch fails closed with SourceMismatch`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-real-owner")
        val id = DerivativeGenerationId("gen-mismatch")
        admitTierA(generationStorage, contentStorage, id, evidenceArtifactId, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()), PDF_KIND)
        val coord = coordinator(generationStorage, contentStorage)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(EvidenceArtifactId("evidence-wrong"), id)), "Summarise"))

        assertEquals(DocumentAnalysisOutcome.SourceMismatch(EvidenceArtifactId("evidence-wrong"), id), outcome)
    }

    // ================= F =================

    @Test
    fun `F an unknown generation id fails closed with UnknownGeneration`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val coord = coordinator(generationStorage, contentStorage)

        val id = DerivativeGenerationId("never-admitted")
        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(EvidenceArtifactId("evidence-x"), id)), "Summarise"))

        assertEquals(DocumentAnalysisOutcome.UnknownGeneration(id), outcome)
    }

    // ================= G =================

    @Test
    fun `G a generation record with no corresponding content entry fails closed with ContentMissing`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-orphan")
        val id = DerivativeGenerationId("gen-orphan")
        val record = DerivativeGenerationTest.record(id.value).copy(
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
        )
        generationStorage.prepare(record)
        generationStorage.publishPrepared(id)
        // Deliberately never prepare/publish content.
        val coord = coordinator(generationStorage, contentStorage)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, id)), "Summarise"))

        assertEquals(DocumentAnalysisOutcome.ContentMissing(id), outcome)
    }

    // ================= H =================

    @Test
    fun `H a genuinely inconsistent stored record -- no OCR transformation, but Ocr-shaped content -- fails closed with UnsupportedDerivativeKind`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-inconsistent")
        val id = DerivativeGenerationId("gen-inconsistent")
        val extracted = TierADerivativePayloadFixtures.ocr()
        // transformationHistory deliberately does NOT include OCR (DerivativeGenerationTest.record's
        // own default is STRUCTURAL_PARSING only) -- Tier B's own kind check reports
        // WrongDerivativeKind, falling through to Tier A, whose own retrieval performs no kind
        // discrimination and returns the Ocr-shaped payload anyway: a genuine internal
        // inconsistency this coordinator's own defensive branch must catch, never silently accept.
        val record = DerivativeGenerationTest.record(id.value).copy(
            rootSourceEvidenceArtifactId = evidenceArtifactId,
            parents = listOf(DerivativeParentReference.RootEvidenceArtifact(evidenceArtifactId)),
        )
        contentStorage.prepare(DerivativeContentEntry(id, evidenceArtifactId, TierADerivativePayload.Ocr(extracted)))
        contentStorage.publishPrepared(id)
        generationStorage.prepare(record)
        generationStorage.publishPrepared(id)
        val coord = coordinator(generationStorage, contentStorage)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, id)), "Summarise"))

        val unsupported = assertIs<DocumentAnalysisOutcome.UnsupportedDerivativeKind>(outcome)
        assertEquals(id, unsupported.derivativeGenerationId)
    }

    // ================= I =================

    @Test
    fun `I more than the maximum selections fails closed before any derivative retrieval begins`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val coord = coordinator(generationStorage, contentStorage)
        // None of these identities are ever admitted -- if retrieval were attempted first, the
        // outcome would be UnknownGeneration instead, proving the bound is checked first.
        val selections = (1..(DocumentAnalysisCoordinator.MAX_SELECTIONS + 1)).map {
            EvidenceGenerationSelection(EvidenceArtifactId("evidence-$it"), DerivativeGenerationId("gen-$it"))
        }

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(selections, "Summarise"))

        val tooMany = assertIs<DocumentAnalysisOutcome.TooManySelections>(outcome)
        assertEquals(DocumentAnalysisCoordinator.MAX_SELECTIONS + 1, tooMany.requested)
        assertEquals(DocumentAnalysisCoordinator.MAX_SELECTIONS, tooMany.max)
    }

    // ================= Correction pass §2: instruction and complete-prompt bounds =================

    @Test
    fun `an oversized owner instruction fails closed before any derivative retrieval begins`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val modelClient = FakeModelInferenceClient()
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = modelClient)
        val oversizedInstruction = "x".repeat(DocumentAnalysisCoordinator.MAX_INSTRUCTION_CHARACTERS + 1)
        // Never admitted: if retrieval were attempted, the outcome would be UnknownGeneration, not
        // InstructionTooLarge, proving the instruction bound is checked before retrieval.
        val selection = EvidenceGenerationSelection(EvidenceArtifactId("evidence-never-admitted"), DerivativeGenerationId("gen-never-admitted"))

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(selection), oversizedInstruction))

        val tooLarge = assertIs<DocumentAnalysisOutcome.InstructionTooLarge>(outcome)
        assertEquals(oversizedInstruction.length, tooLarge.actualCharacters)
        assertEquals(DocumentAnalysisCoordinator.MAX_INSTRUCTION_CHARACTERS, tooLarge.max)
        assertEquals(0, modelClient.invocationCount)
    }

    @Test
    // A complete assembled prompt exceeding MAX_PROMPT_CHARACTERS fails closed before model
    // invocation, driven by identifier-length overhead the evidence-content/instruction bounds
    // alone do not account for -- proving the prompt is genuinely measured, not merely assumed.
    fun `an oversized complete prompt driven by identifier overhead fails closed before invocation`() = runTest {
        val (generationStorage, contentStorage) = storages()
        // Deliberately small content and a small instruction -- both individually far under their
        // own bounds -- so that only the EvidenceArtifactId's own (unbounded-by-construction)
        // length can be what pushes the complete assembled prompt over MAX_PROMPT_CHARACTERS. This
        // is exactly the "do not merely assume 200,000 + instruction limit equals the final prompt
        // size" case the correction calls out: EvidenceArtifactId/DerivativeGenerationId values are
        // literally interpolated into the prompt, and neither bound above accounts for their size.
        val hugeEvidenceArtifactId = EvidenceArtifactId("evidence-" + "z".repeat(DocumentAnalysisCoordinator.MAX_PROMPT_CHARACTERS))
        val id = DerivativeGenerationId("gen-prompt-overflow")
        admitTierA(generationStorage, contentStorage, id, hugeEvidenceArtifactId, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf("small content")), PDF_KIND)
        val modelClient = FakeModelInferenceClient()
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = modelClient)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(hugeEvidenceArtifactId, id)), "Summarise"))

        val tooLarge = assertIs<DocumentAnalysisOutcome.PromptTooLarge>(outcome)
        assertEquals(DocumentAnalysisCoordinator.MAX_PROMPT_CHARACTERS, tooLarge.max)
        assertEquals(0, modelClient.invocationCount)
    }

    @Test
    // Final correction pass §2: JSON escaping can expand source content well past
    // MAX_TOTAL_CONTENT_CHARACTERS's own (source-only) ceiling -- a package that passes the source
    // ceiling can still, legitimately and intentionally, fail PromptTooLarge once encoded. Content
    // made entirely of the C0 control character U+0001 is the worst realistic case:
    // DefaultDocumentAnalysisPromptBuilder's own jsonEscape renders each one as the six-character
    // \u0001 escape sequence -- a 6x expansion.
    fun `evidence content that stays within the source-content ceiling but requires substantial JSON escaping still exceeds the prompt ceiling and fails closed, never truncated`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-escape-heavy")
        val id = DerivativeGenerationId("gen-escape-heavy")
        // Exactly at MAX_TOTAL_CONTENT_CHARACTERS -- the source-content ceiling is satisfied
        // (not exceeded), proving this is a genuinely different, independent rejection from
        // ContentTooLarge.
        val escapeHeavyText = "\u0001".repeat(DocumentAnalysisCoordinator.MAX_TOTAL_CONTENT_CHARACTERS)
        admitTierA(generationStorage, contentStorage, id, evidenceArtifactId, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf(escapeHeavyText)), PDF_KIND)
        val modelClient = FakeModelInferenceClient()
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = modelClient)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, id)), "Summarise"))

        val tooLarge = assertIs<DocumentAnalysisOutcome.PromptTooLarge>(outcome)
        assertEquals(DocumentAnalysisCoordinator.MAX_PROMPT_CHARACTERS, tooLarge.max)
        assertTrue(
            tooLarge.actualCharacters > DocumentAnalysisCoordinator.MAX_TOTAL_CONTENT_CHARACTERS,
            "the encoded prompt must genuinely exceed the source-content ceiling, proving escaping expansion was actually measured, not assumed away",
        )
        // No model invocation, and the coordinator's own outcome shape (PromptTooLarge, not
        // Completed with a shortened analysisText) is itself the proof that nothing was truncated
        // to force a fit.
        assertEquals(0, modelClient.invocationCount)
    }

    // ================= Correction pass §3: Tier A derivative-kind validation against payload type =================

    private suspend fun mismatchedKindOutcome(
        payload: TierADerivativePayload,
        wrongKind: String,
    ): DocumentAnalysisOutcome {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-kind-mismatch")
        val id = DerivativeGenerationId("gen-kind-mismatch")
        admitTierA(generationStorage, contentStorage, id, evidenceArtifactId, payload, wrongKind)
        val coord = coordinator(generationStorage, contentStorage)
        return coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, id)), "Summarise"))
    }

    @Test
    fun `A PDF payload stored under a mismatched derivativeKind fails closed with UnsupportedDerivativeKind, never silently interpreted as PDF`() = runTest {
        val outcome = mismatchedKindOutcome(TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()), CSV_KIND)
        assertIs<DocumentAnalysisOutcome.UnsupportedDerivativeKind>(outcome)
    }

    @Test
    fun `B CSV payload stored under a mismatched derivativeKind fails closed with UnsupportedDerivativeKind, never silently interpreted as CSV`() = runTest {
        val outcome = mismatchedKindOutcome(TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()), PDF_KIND)
        assertIs<DocumentAnalysisOutcome.UnsupportedDerivativeKind>(outcome)
    }

    @Test
    fun `C EML payload stored under a mismatched derivativeKind fails closed with UnsupportedDerivativeKind, never silently interpreted as EML`() = runTest {
        val outcome = mismatchedKindOutcome(TierADerivativePayload.Eml(TierADerivativePayloadFixtures.eml(), 0), DOCX_KIND)
        assertIs<DocumentAnalysisOutcome.UnsupportedDerivativeKind>(outcome)
    }

    @Test
    fun `D DOCX payload stored under a mismatched derivativeKind fails closed with UnsupportedDerivativeKind, never silently interpreted as DOCX`() = runTest {
        val outcome = mismatchedKindOutcome(TierADerivativePayload.Docx(TierADerivativePayloadFixtures.docx()), EML_KIND)
        assertIs<DocumentAnalysisOutcome.UnsupportedDerivativeKind>(outcome)
    }

    @Test
    fun `E an entirely unsupported (invented) derivativeKind string alongside a genuine Tier A payload fails closed with UnsupportedDerivativeKind`() = runTest {
        val outcome = mismatchedKindOutcome(TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()), "some invented derivative kind")
        assertIs<DocumentAnalysisOutcome.UnsupportedDerivativeKind>(outcome)
    }

    @Test
    fun `F valid PDF, CSV, EML, and DOCX kind-payload combinations all continue to succeed`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val pdfId = DerivativeGenerationId("gen-valid-pdf")
        val csvId = DerivativeGenerationId("gen-valid-csv")
        val emlId = DerivativeGenerationId("gen-valid-eml")
        val docxId = DerivativeGenerationId("gen-valid-docx")
        val pdfEvidence = EvidenceArtifactId("evidence-valid-pdf")
        val csvEvidence = EvidenceArtifactId("evidence-valid-csv")
        val emlEvidence = EvidenceArtifactId("evidence-valid-eml")
        val docxEvidence = EvidenceArtifactId("evidence-valid-docx")
        admitTierA(generationStorage, contentStorage, pdfId, pdfEvidence, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()), PDF_KIND)
        admitTierA(generationStorage, contentStorage, csvId, csvEvidence, TierADerivativePayload.Csv(TierADerivativePayloadFixtures.csv()), CSV_KIND)
        admitTierA(generationStorage, contentStorage, emlId, emlEvidence, TierADerivativePayload.Eml(TierADerivativePayloadFixtures.eml(), 0), EML_KIND)
        admitTierA(generationStorage, contentStorage, docxId, docxEvidence, TierADerivativePayload.Docx(TierADerivativePayloadFixtures.docx()), DOCX_KIND)
        val coord = coordinator(generationStorage, contentStorage)

        val outcome = coord.analyse(
            owner,
            OwnerDocumentAnalysisRequest(
                listOf(
                    EvidenceGenerationSelection(pdfEvidence, pdfId),
                    EvidenceGenerationSelection(csvEvidence, csvId),
                    EvidenceGenerationSelection(emlEvidence, emlId),
                    EvidenceGenerationSelection(docxEvidence, docxId),
                ),
                "Summarise",
            ),
        )

        val completed = assertIs<DocumentAnalysisOutcome.Completed>(outcome)
        assertEquals(4, completed.result.evidenceItems.size)
    }

    // ================= J =================

    @Test
    fun `J total extracted content exceeding the bound fails closed before model invocation`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-big")
        val id = DerivativeGenerationId("gen-big")
        val bigText = "x".repeat(DocumentAnalysisCoordinator.MAX_TOTAL_CONTENT_CHARACTERS + 1)
        admitTierA(generationStorage, contentStorage, id, evidenceArtifactId, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf(bigText)), PDF_KIND)
        val modelClient = FakeModelInferenceClient()
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = modelClient)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, id)), "Summarise"))

        assertIs<DocumentAnalysisOutcome.ContentTooLarge>(outcome)
        assertEquals(0, modelClient.invocationCount)
    }

    // ================= K =================

    @Test
    fun `K an oversized model response is rejected cleanly, never silently truncated`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-small")
        val id = DerivativeGenerationId("gen-small")
        admitTierA(generationStorage, contentStorage, id, evidenceArtifactId, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()), PDF_KIND)
        val hugeResponse = "y".repeat(DocumentAnalysisCoordinator.MAX_RESPONSE_CHARACTERS + 1)
        val modelClient = FakeModelInferenceClient(onInfer = { hugeResponse })
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = modelClient)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, id)), "Summarise"))

        val tooLarge = assertIs<DocumentAnalysisOutcome.ResponseTooLarge>(outcome)
        assertEquals(hugeResponse.length, tooLarge.actualCharacters)
    }

    // ================= L =================

    @Test
    fun `L a Permission Engine denial causes zero derivative retrieval and zero model inference calls`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val modelClient = FakeModelInferenceClient()
        val permissionEngine = FakePermissionEngine(PermissionDecisionOutcome.DENIED)
        val coord = coordinator(generationStorage, contentStorage, permissionEngine = permissionEngine, modelInferenceClient = modelClient)
        // Never admitted: if retrieval were attempted, the outcome would be UnknownGeneration, not
        // NotAuthorised, proving denial short-circuits before any retrieval is even attempted.
        val selection = EvidenceGenerationSelection(EvidenceArtifactId("evidence-never-admitted"), DerivativeGenerationId("gen-never-admitted"))

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(selection), "Summarise"))

        assertIs<DocumentAnalysisOutcome.NotAuthorised>(outcome)
        assertEquals(1, permissionEngine.evaluateCallCount)
        assertEquals(0, modelClient.invocationCount)
    }

    // ================= O =================

    @Test
    fun `O a model inference fault is reported as an honest, non-leaking ModelInvocationFailed`() = runTest {
        val (generationStorage, contentStorage) = storages()
        val evidenceArtifactId = EvidenceArtifactId("evidence-fault")
        val id = DerivativeGenerationId("gen-fault")
        admitTierA(generationStorage, contentStorage, id, evidenceArtifactId, TierADerivativePayload.Pdf(TierADerivativePayloadFixtures.pdf()), PDF_KIND)
        val modelClient = FakeModelInferenceClient(
            onInfer = { throw RuntimeException("connection refused to 10.0.0.5:11434, Authorization: Bearer sk-internal-secret") },
        )
        val coord = coordinator(generationStorage, contentStorage, modelInferenceClient = modelClient)

        val outcome = coord.analyse(owner, OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(evidenceArtifactId, id)), "Summarise"))

        val failed = assertIs<DocumentAnalysisOutcome.ModelInvocationFailed>(outcome)
        assertFalse("sk-internal-secret" in failed.safeMessage)
        assertFalse("10.0.0.5" in failed.safeMessage)
    }

    // ================= T, U, V, W, X =================

    /**
     * Correction pass §6 (structural-test overclaim): this test proves exactly one thing --
     * [DocumentAnalysisCoordinator]'s own declared constructor field TYPES contain no
     * Memory/Knowledge/QMD/RKS/OCR/extraction/external-reasoning-provider type reference, so no
     * such dependency is reachable through this class's *current* constructor shape. It does
     * **not** prove that no future edit to this class, or to any class it is composed with, could
     * ever introduce one -- a reflection test over one class's own field list cannot make a claim
     * about the whole system or about hypothetical future code. The complementary claim -- that the
     * REAL, currently-running production composition wires this coordinator with only durable
     * retrieval coordinators and the one configured local [ModelInferenceClient] -- is proven
     * separately, over the actual constructed [parker.composition.ParkerRuntime] graph, by
     * `OwnerEvidenceHttpServerTest`'s own composition-level structural test.
     */
    @Test
    fun `T U V W X this class's own declared field TYPES contain no Memory, Knowledge, QMD, RKS, OCR-extraction, or external-reasoning-provider type reference`() {
        val fieldTypeNames = DocumentAnalysisCoordinator::class.java.declaredFields.map { it.type.simpleName }.toSet()
        val forbidden = listOf(
            "MemoryCore", "KnowledgeRetrieval", "KnowledgeSubmission", "RelevanceMechanism", "QmdRelevanceMechanism",
            "OcrMechanism", "EvidenceIntelligenceOcrCoordinator", "DerivativeGenerationCoordinator",
            "TierADocumentIngestionRouter", "ReasoningProvider", "EvidenceIntelligence",
        )
        forbidden.forEach { forbiddenType ->
            assertTrue(
                fieldTypeNames.none { it.contains(forbiddenType) },
                "DocumentAnalysisCoordinator's own declared field types must contain no $forbiddenType reference -- found: $fieldTypeNames",
            )
        }
        // The only model-inference seam reachable from this class's own declared fields is the
        // generic ModelInferenceClient interface type -- never a concrete external-provider HTTP
        // client type of any kind. Again: a fact about this class's own field types, not a
        // guarantee about what a caller supplies at runtime (see class doc above).
        assertTrue(fieldTypeNames.none { it.contains("OpenAi") || it.contains("Anthropic") })
    }
}
