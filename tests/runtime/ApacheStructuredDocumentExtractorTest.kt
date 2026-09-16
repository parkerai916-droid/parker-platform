package parker.core.runtime

import java.io.ByteArrayOutputStream
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import parker.core.interfaces.DerivativeContentEntry
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DecisionId
import parker.core.interfaces.DocumentAnalysisOutcome
import parker.core.interfaces.DocxStructuralExtractionOutcome
import parker.core.interfaces.EvidenceGenerationSelection
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.OwnerDocumentAnalysisRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionExplanation
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.StructuredDocumentExtractionOutcome
import parker.core.interfaces.StructuredDocumentKind
import parker.core.interfaces.TierADerivativePayload

class ApacheStructuredDocumentExtractorTest {
    private val extractor = ApacheStructuredDocumentExtractor()
    private val sha = "a".repeat(64)
    private val fixtureRoot = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures")
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `generated DOC fixture preserves paragraph order and exact content`() = runTest {
        val bytes = Files.readAllBytes(fixtureRoot.resolve("08-legacy-parker.doc"))
        val result = assertIs<StructuredDocumentExtractionOutcome.Extracted>(
            extractor.extract(bytes, sha256(bytes), "application/msword", "08-legacy-parker.doc"),
        ).result
        assertEquals(StructuredDocumentKind.DOC, result.kind)
        assertTrue(result.text.contains("PARKER DOC TEST VALUE 43"), result.text)
        assertTrue(result.text.indexOf("PARKER DOC TEST VALUE 43") < result.text.indexOf("SECOND DOC PARAGRAPH"), result.text)
        assertTrue(result.wordBlocks.size >= 2, result.wordBlocks.toString())
    }

    @Test
    fun `generated MSG fixture preserves headers body and attachment relationship`() = runTest {
        val bytes = Files.readAllBytes(fixtureRoot.resolve("09-legacy-parker.msg"))
        val result = assertIs<StructuredDocumentExtractionOutcome.Extracted>(
            extractor.extract(bytes, sha256(bytes), "application/vnd.ms-outlook", "09-legacy-parker.msg"),
        ).result
        assertEquals(StructuredDocumentKind.MSG, result.kind)
        assertEquals("Parker MSG Test", result.subject)
        assertEquals("sender@example.test", result.sender)
        assertTrue(result.recipients.contains("recipient@example.test"), result.recipients.toString())
        assertTrue(result.cc.contains("cc@example.test"), result.cc.toString())
        assertTrue(result.text.contains("PARKER MSG TEST VALUE 45"), result.text)
        assertTrue(result.attachments.any { it.filename == "attachment.txt" }, result.attachments.toString())
    }

    @Test
    fun `DOC and MSG fixtures persist as governed durable structured derivatives`() = runTest {
        listOf(
            Triple("08-legacy-parker.doc", "application/msword", StructuredDocumentKind.DOC),
            Triple("09-legacy-parker.msg", "application/vnd.ms-outlook", StructuredDocumentKind.MSG),
        ).forEach { (name, mediaType, kind) ->
            val bytes = Files.readAllBytes(fixtureRoot.resolve(name))
            val generationStorage = FileSystemDerivativeGenerationStorage(Files.createTempDirectory("structured-generation"))
            val contentStorage = FileSystemDerivativeContentStorage(Files.createTempDirectory("structured-content"))
            val coordinator = DerivativeGenerationCoordinator(
                csvExtractor = ApacheCommonsCsvExtractor(),
                storage = generationStorage,
                audit = FileSystemDocumentIngestionAudit(Files.createTempDirectory("structured-audit").resolve("audit.log")),
                structuredExtractor = extractor,
                contentStorage = contentStorage,
            )
            val outcome = assertIs<StructuredDerivativeGenerationCoordinationOutcome.Admitted>(
                coordinator.ingestStructured(
                    StructuredIngestionSource(EvidenceArtifactId("evidence-$kind"), bytes, sha256(bytes), mediaType, name),
                    PrincipalId("owner-structured-acceptance"), "structured-$kind",
                ),
            )
            assertEquals(kind, outcome.representation.kind)
            assertEquals(outcome.record, generationStorage.retrieve(outcome.record.derivativeGenerationId))
            val persisted = assertNotNull(contentStorage.retrieve(outcome.record.derivativeGenerationId))
            assertEquals(outcome.record.rootSourceEvidenceArtifactId, persisted.rootSourceEvidenceArtifactId)
            assertEquals(outcome.representation, assertIs<TierADerivativePayload.Structured>(persisted.payload).value)
        }
    }

    @Test
    fun `DOC and MSG governed derivatives reach analysis with source structure anchors`() = runTest {
        val permission = object : PermissionEngine {
            override suspend fun evaluate(request: ExecutionRequest) = PermissionDecision(
                DecisionId("structured-analysis-decision"), request.principalId, request.targetResources.first(),
                PermissionAction.EXECUTE, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC, java.time.Instant.EPOCH,
            )
            override suspend fun explain(decisionId: DecisionId): PermissionExplanation = error("not used")
        }
        val model = object : ModelInferenceClient {
            var prompt: String? = null
            override suspend fun infer(prompt: String): String { this.prompt = prompt; return "structured analysis" }
        }
        listOf(
            Triple("08-legacy-parker.doc", "application/msword", "PARKER DOC TEST VALUE 43"),
            Triple("09-legacy-parker.msg", "application/vnd.ms-outlook", "PARKER MSG TEST VALUE 45"),
        ).forEach { (name, mediaType, expectedText) ->
            val bytes = Files.readAllBytes(fixtureRoot.resolve(name))
            val generationStorage = FileSystemDerivativeGenerationStorage(Files.createTempDirectory("analysis-generation"))
            val contentStorage = FileSystemDerivativeContentStorage(Files.createTempDirectory("analysis-content"))
            val admitted = assertIs<StructuredDerivativeGenerationCoordinationOutcome.Admitted>(
                DerivativeGenerationCoordinator(
                    csvExtractor = ApacheCommonsCsvExtractor(), storage = generationStorage,
                    audit = FileSystemDocumentIngestionAudit(Files.createTempDirectory("analysis-audit").resolve("audit.log")),
                    structuredExtractor = extractor, contentStorage = contentStorage,
                ).ingestStructured(
                    StructuredIngestionSource(EvidenceArtifactId("analysis-$name"), bytes, sha256(bytes), mediaType, name),
                    PrincipalId("owner-structured-analysis"), "analysis-$name",
                ),
            )
            val outcome = DocumentAnalysisCoordinator(
                permission, TierAContentRetrievalCoordinator(generationStorage, contentStorage),
                TierBOcrContentRetrievalCoordinator(generationStorage, contentStorage), model,
                DefaultDocumentAnalysisPromptBuilder(), 30_000L,
            ).analyse(
                PrincipalId("owner-structured-analysis"),
                OwnerDocumentAnalysisRequest(listOf(EvidenceGenerationSelection(
                    admitted.record.rootSourceEvidenceArtifactId, admitted.record.derivativeGenerationId,
                )), "What value appears in this document? Quote the supporting text."),
            )
            val completed = assertIs<DocumentAnalysisOutcome.Completed>(outcome)
            val item = completed.result.evidenceItems.single()
            assertTrue(expectedText in item.extractedText)
            assertTrue(expectedText in model.prompt.orEmpty())
            assertEquals(admitted.record.derivativeGenerationId, item.derivativeGenerationId)
            assertTrue(item.citationAnchors.any { expectedText in it.quotedText && it.pageNumber == null })
        }
    }

    @Test
    fun `controlled DOCX fixture preserves the acceptance value and ordered paragraphs`() = runTest {
        val bytes = Files.readAllBytes(fixtureRoot.resolve("10-stage19d.docx"))
        val result = assertIs<DocxStructuralExtractionOutcome.Extracted>(ApachePoiXwpfExtractor().extract(bytes)).result
        val paragraphs = result.paragraphs.map { it.text }
        assertTrue(paragraphs.contains("PARKER DOCX TEST VALUE 46"), paragraphs.toString())
        assertTrue(paragraphs.indexOf("PARKER DOCX TEST VALUE 46") < paragraphs.indexOf("Literal fidelity controls"), paragraphs.toString())
        assertTrue(result.tables.isNotEmpty(), "controlled fixture retains its table structure")
        val reloaded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(
            DerivativeContentEntry(DerivativeGenerationId("docx-method"), EvidenceArtifactId("docx-source"), TierADerivativePayload.Docx(result))
        ))
        assertEquals("STRUCTURED_DOCUMENT_EXTRACTION", assertIs<TierADerivativePayload.Docx>(reloaded.payload).value.extractionMethod)
    }

    @Test
    fun `txt preserves exact quote and line offsets`() = runTest {
        val result = assertIs<StructuredDocumentExtractionOutcome.Extracted>(extractor.extract("PARKER TXT TEST VALUE 42\nsecond".toByteArray(), sha, "text/plain", "fixture.txt")).result
        assertEquals(StructuredDocumentKind.TXT, result.kind)
        assertEquals("PARKER TXT TEST VALUE 42", result.lines.first().text)
        assertEquals("PARKER TXT TEST VALUE 42\nsecond", result.text)
        assertEquals("DIRECT_TEXT_EXTRACTION", result.extractionMethod)
    }

    @Test
    fun `xlsx preserves sheet cell formula and displayed value`() = runTest {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Payments")
            sheet.createRow(3).createCell(1).setCellValue(2250.0)
            sheet.getRow(3).createCell(2).cellFormula = "B4*2"
            val bytes = ByteArrayOutputStream().also { workbook.write(it) }.toByteArray()
            val result = assertIs<StructuredDocumentExtractionOutcome.Extracted>(extractor.extract(bytes, sha, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "fixture.xlsx")).result
            val cells = result.spreadsheetSheets.single().cells
            assertEquals("Payments", cells.first().sheetName)
            assertEquals("B4", cells.first().coordinate)
            assertTrue(cells.any { it.coordinate == "C4" && it.formula == "B4*2" })
            val reloaded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(
                DerivativeContentEntry(DerivativeGenerationId("xlsx-method"), EvidenceArtifactId("xlsx-source"), TierADerivativePayload.Structured(result))
            ))
            assertEquals("STRUCTURED_SPREADSHEET_EXTRACTION", assertIs<TierADerivativePayload.Structured>(reloaded.payload).value.extractionMethod)
        }
    }

    @Test
    fun `xls converges on spreadsheet representation`() = runTest {
        HSSFWorkbook().use { workbook ->
            workbook.createSheet("Payments").createRow(3).createCell(1).setCellValue(1250.0)
            val bytes = ByteArrayOutputStream().also { workbook.write(it) }.toByteArray()
            val result = assertIs<StructuredDocumentExtractionOutcome.Extracted>(extractor.extract(bytes, sha, "application/vnd.ms-excel", "fixture.xls")).result
            assertEquals(StructuredDocumentKind.XLS, result.kind)
            assertEquals("B4", result.spreadsheetSheets.single().cells.single().coordinate)
        }
    }

    @Test
    fun `rtf is bounded text without invented pages`() = runTest {
        val result = assertIs<StructuredDocumentExtractionOutcome.Extracted>(extractor.extract("{\\rtf1\\ansi PARKER RTF TEST VALUE 44}".toByteArray(), sha, "application/rtf", "fixture.rtf")).result
        assertEquals(StructuredDocumentKind.RTF, result.kind)
        assertTrue(result.text.contains("PARKER RTF TEST VALUE 44"))
        assertTrue(result.lines.isNotEmpty())
    }

    @Test
    fun `structured content codec survives restart-shaped round trip`() = runTest {
        val result = assertIs<StructuredDocumentExtractionOutcome.Extracted>(extractor.extract("PARKER TXT TEST VALUE 42".toByteArray(), sha, "text/plain", "fixture.txt")).result
        val entry = DerivativeContentEntry(DerivativeGenerationId("generation-structured"), EvidenceArtifactId("evidence-structured"), TierADerivativePayload.Structured(result))
        val decoded = DerivativeContentCodec.decode(DerivativeContentCodec.encode(entry))
        assertEquals(result, assertIs<TierADerivativePayload.Structured>(decoded.payload).value)
    }

    @Test
    fun `invalid legacy and text inputs fail closed`() = runTest {
        assertIs<StructuredDocumentExtractionOutcome.Malformed>(extractor.extract("not utf8".toByteArray(), sha, "application/rtf", "bad.rtf"))
        assertIs<StructuredDocumentExtractionOutcome.Malformed>(extractor.extract("not ole".toByteArray(), sha, "application/msword", "bad.doc"))
        assertIs<StructuredDocumentExtractionOutcome.Malformed>(extractor.extract("not ole".toByteArray(), sha, "application/vnd.ms-outlook", "bad.msg"))
    }
}
