package parker.core.runtime

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*

class GovernedTierADocumentIngestionRouterTest {
    @TempDir lateinit var directory: Path

    @Test fun `frozen seven-fixture matrix selects one governed route with correct authority boundary`() = runTest {
        val storage = FileSystemDerivativeGenerationStorage(directory.resolve("generations").also(Files::createDirectory))
        val auditPath = directory.resolve("audit.log")
        val router = TierADocumentIngestionComposition.create(storage, FileSystemDocumentIngestionAudit(auditPath))
        val admitted = mutableListOf<TierADocumentRoutingResult.Admitted>()
        FIXTURES.forEach { fixture ->
            val bytes = Files.readAllBytes(FIXTURE_ROOT.resolve(fixture.file)); val original = bytes.copyOf()
            assertEquals(fixture.length, bytes.size); assertEquals(fixture.sha256, sha256(bytes))
            val result = router.ingest(context(fixture.file, bytes, fixture.mediaType, fixture.sha256))
            when (fixture.expected) {
                Expected.TIER_B -> assertIs<TierADocumentRoutingResult.RequiresTierB>(result)
                else -> {
                    val success = assertIs<TierADocumentRoutingResult.Admitted>(result)
                    assertEquals(fixture.expected.format, success.format)
                    assertNotNull(storage.retrieve(success.record.derivativeGenerationId))
                    assertEquals(fixture.expected.product, success.record.producerIdentity.pluginIdentity)
                    assertTrue(success.record.warnings.isNotEmpty() || success.record.completenessState == DerivativeCompletenessState.ACCOUNTED_FOR)
                    admitted += success
                }
            }
            assertContentEquals(original, bytes); assertEquals(fixture.sha256, sha256(bytes))
        }
        assertEquals(5, admitted.size); assertEquals(5, admitted.map { it.record.derivativeGenerationId }.distinct().size)
        val auditLines = Files.readAllLines(auditPath)
        assertEquals(10, auditLines.size)
        assertEquals(5, auditLines.count { "stage=ADMISSION_AUTHORISED" in it })
        assertEquals(5, auditLines.count { "stage=ADMITTED" in it })
        assertEquals(1, (admitted.single { it.format == TierADocumentFormat.EML }.payload as TierADerivativePayload.Eml).childSourceCandidateCount)
        assertFalse(Files.exists(directory.resolve("ocr")))
    }

    @Test fun `mechanical content signatures outrank misleading filename and preserve media disagreement`() = runTest {
        val router = router(RecordingStorage(), RecordingAudit())
        val pdf = Files.readAllBytes(FIXTURE_ROOT.resolve("01-searchable-simple.pdf"))
        val pdfResult = assertIs<TierADocumentRoutingResult.Admitted>(router.ingest(context("misleading.csv", pdf, "application/pdf", sha256(pdf))))
        assertEquals(TierADocumentFormat.PDF, pdfResult.format)
        val docx = Files.readAllBytes(FIXTURE_ROOT.resolve("04-structured.docx"))
        assertEquals(TierADocumentFormat.DOCX, assertIs<TierADocumentRoutingResult.Admitted>(router.ingest(context("misleading.pdf", docx, "application/octet-stream", sha256(docx)))).format)
        val eml = Files.readAllBytes(FIXTURE_ROOT.resolve("05-email-with-attachment.eml"))
        val emlResult = assertIs<TierADocumentRoutingResult.Admitted>(router.ingest(context("mail.bin", eml, "application/octet-stream", sha256(eml))))
        assertEquals(TierADocumentFormat.EML, emlResult.format); assertTrue(emlResult.mediaFacts.disagreement)
    }

    @Test fun `ambiguous CSV declared plain text is unsupported and causes no side effects`() = runTest {
        val storage = RecordingStorage(); val audit = RecordingAudit(); val bytes = "a,b\r\n1,2\r\n".toByteArray()
        val result = router(storage, audit).ingest(context("looks.csv", bytes, "text/plain", sha256(bytes)))
        assertIs<TierADocumentRoutingResult.Unsupported>(result); assertEquals(0, storage.prepareCalls); assertTrue(audit.records.isEmpty())
    }

    @Test fun `unknown binary is unsupported while PNG distinctly requires Tier B`() = runTest {
        val storage = RecordingStorage(); val audit = RecordingAudit(); val router = router(storage, audit)
        val binary = byteArrayOf(1, 2, 3, 4)
        assertIs<TierADocumentRoutingResult.Unsupported>(router.ingest(context("x.bin", binary, "application/octet-stream", sha256(binary))))
        val png = Files.readAllBytes(FIXTURE_ROOT.resolve("07-text-image.png"))
        assertIs<TierADocumentRoutingResult.RequiresTierB>(router.ingest(context("x.png", png, "image/png", sha256(png))))
        assertEquals(0, storage.prepareCalls); assertTrue(audit.records.isEmpty())
    }

    @Test fun `near PNG arbitrary ZIP and random eml filename do not false route`() = runTest {
        val router = router(RecordingStorage(), RecordingAudit())
        val nearPng = byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x00)
        assertIs<TierADocumentRoutingResult.Unsupported>(router.ingest(context("fake.pdf", nearPng, "application/octet-stream", sha256(nearPng))))
        val zipOut = ByteArrayOutputStream(); ZipOutputStream(zipOut).use { zip -> zip.putNextEntry(ZipEntry("word/document.xml")); zip.write("x".toByteArray()); zip.closeEntry() }
        val zip = zipOut.toByteArray()
        assertIs<TierADocumentRoutingResult.Unsupported>(router.ingest(context("fake.docx", zip, "application/octet-stream", sha256(zip))))
        val text = "Subject: only one header\r\n\r\nnot an email route".toByteArray()
        assertIs<TierADocumentRoutingResult.Unsupported>(router.ingest(context("fake.eml", text, "text/plain", sha256(text))))
        val png = Files.readAllBytes(FIXTURE_ROOT.resolve("07-text-image.png"))
        assertIs<TierADocumentRoutingResult.RequiresTierB>(router.ingest(context("misleading.pdf", png, "application/octet-stream", sha256(png))))
    }

    @Test fun `each route invokes exactly one specialist and non routes invoke none`() = runTest {
        val routes = SpyRoutes(); val router = GovernedTierADocumentIngestionRouter(routes)
        val csv = "a,b\r\n1,2\r\n".toByteArray(); router.ingest(context("x.csv", csv, "text/csv", sha256(csv)))
        val eml = Files.readAllBytes(FIXTURE_ROOT.resolve("05-email-with-attachment.eml")); router.ingest(context("x.eml", eml, "application/octet-stream", sha256(eml)))
        val docx = Files.readAllBytes(FIXTURE_ROOT.resolve("04-structured.docx")); router.ingest(context("x.bin", docx, "application/octet-stream", sha256(docx)))
        val pdf = Files.readAllBytes(FIXTURE_ROOT.resolve("01-searchable-simple.pdf")); router.ingest(context("x.bin", pdf, "application/octet-stream", sha256(pdf)))
        val png = Files.readAllBytes(FIXTURE_ROOT.resolve("07-text-image.png")); router.ingest(context("x.png", png, "image/png", sha256(png)))
        val unknown = byteArrayOf(1,2,3); router.ingest(context("x.bin", unknown, "application/octet-stream", sha256(unknown)))
        assertEquals(listOf("csv", "eml", "docx", "pdf"), routes.calls)
    }

    @Test fun `source integrity and post-publication audit failure remain distinct`() = runTest {
        val bytes = Files.readAllBytes(FIXTURE_ROOT.resolve("06-structured.csv"))
        assertIs<TierADocumentRoutingResult.SourceIntegrityFailed>(router(RecordingStorage(), RecordingAudit()).ingest(context("x.csv", bytes, "text/csv", "0".repeat(64))))
        val storage = RecordingStorage(); val result = router(storage, RecordingAudit(failAt = 2)).ingest(context("x.csv", bytes, "text/csv", sha256(bytes)))
        val reconciliation = assertIs<TierADocumentRoutingResult.ReconciliationRequired>(result)
        assertEquals(storage.admitted, reconciliation.record); assertEquals(TierADocumentFormat.CSV, reconciliation.format)
    }

    @Test fun `preparation authorization and publication failures retain exact stage`() = runTest {
        val bytes = Files.readAllBytes(FIXTURE_ROOT.resolve("06-structured.csv"))
        assertEquals("PREPARE", assertIs<TierADocumentRoutingResult.AdmissionFailed>(router(RecordingStorage(failPrepare = true), RecordingAudit()).ingest(context("p.csv", bytes, "text/csv", sha256(bytes)))).stage)
        assertEquals("ADMISSION_AUTHORISED", assertIs<TierADocumentRoutingResult.AdmissionFailed>(router(RecordingStorage(), RecordingAudit(1)).ingest(context("a.csv", bytes, "text/csv", sha256(bytes)))).stage)
        assertEquals("PUBLISH", assertIs<TierADocumentRoutingResult.AdmissionFailed>(router(RecordingStorage(failPublish = true), RecordingAudit()).ingest(context("u.csv", bytes, "text/csv", sha256(bytes)))).stage)
    }

    @Test fun `router reprocessing retains both generations and two audit pairs`() = runTest {
        val bytes = Files.readAllBytes(FIXTURE_ROOT.resolve("06-structured.csv")); val original = bytes.copyOf()
        val storage = FileSystemDerivativeGenerationStorage(directory.resolve("repeat-generations").also(Files::createDirectory)); val audit = RecordingAudit(); val router = router(storage, audit)
        val first = assertIs<TierADocumentRoutingResult.Admitted>(router.ingest(context("repeat.csv", bytes, "text/csv", sha256(bytes))))
        val second = assertIs<TierADocumentRoutingResult.Admitted>(router.ingest(context("repeat.csv", bytes, "text/csv", sha256(bytes))))
        assertNotEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        assertNotNull(storage.retrieve(first.record.derivativeGenerationId)); assertNotNull(storage.retrieve(second.record.derivativeGenerationId))
        assertEquals(listOf(DocumentIngestionAuditStage.ADMISSION_AUTHORISED, DocumentIngestionAuditStage.ADMITTED, DocumentIngestionAuditStage.ADMISSION_AUTHORISED, DocumentIngestionAuditStage.ADMITTED), audit.records.map { it.stage })
        assertContentEquals(original, bytes)
    }

    private fun router(storage: DerivativeGenerationStorage, audit: DocumentIngestionAudit) = TierADocumentIngestionComposition.create(storage, audit)
    private fun context(file: String, bytes: ByteArray, media: String, hash: String) = TierADocumentSourceContext(
        EvidenceArtifactId("source-$file"), bytes, hash, media, file, PrincipalId("principal-routing"), "correlation-$file",
    )
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private class RecordingAudit(private val failAt: Int? = null) : DocumentIngestionAudit {
        val records = mutableListOf<DocumentIngestionAuditRecord>(); private var calls = 0
        override suspend fun record(record: DocumentIngestionAuditRecord) { calls++; if (calls == failAt) throw DocumentIngestionAuditException.PersistenceFailure("injected", IOException("injected")); records += record }
    }
    private class RecordingStorage(private val failPrepare: Boolean = false, private val failPublish: Boolean = false) : DerivativeGenerationStorage {
        var prepareCalls = 0; var prepared: DerivativeGenerationRecord? = null; var admitted: DerivativeGenerationRecord? = null
        override suspend fun prepare(record: DerivativeGenerationRecord) { prepareCalls++; if (failPrepare) throw DerivativeGenerationStorageException.PersistenceFailure("injected", IOException("injected")); prepared = record }
        override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) { if (failPublish) throw DerivativeGenerationStorageException.PersistenceFailure("injected", IOException("injected")); admitted = prepared; prepared = null }
        override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId) = admitted?.takeIf { it.derivativeGenerationId == derivativeGenerationId }
    }
    private class SpyRoutes : TierAFormatRoutes {
        val calls = mutableListOf<String>()
        override suspend fun ingestCsv(source: CsvIngestionSource, principal: PrincipalId, correlation: String): DerivativeGenerationCoordinationOutcome { calls += "csv"; return DerivativeGenerationCoordinationOutcome.ExtractionFailed("spy") }
        override suspend fun ingestEml(source: EmlIngestionSource, principal: PrincipalId, correlation: String): EmlDerivativeGenerationCoordinationOutcome { calls += "eml"; return EmlDerivativeGenerationCoordinationOutcome.ExtractionFailed("spy") }
        override suspend fun ingestDocx(source: DocxIngestionSource, principal: PrincipalId, correlation: String): DocxDerivativeGenerationCoordinationOutcome { calls += "docx"; return DocxDerivativeGenerationCoordinationOutcome.ExtractionFailed("spy") }
        override suspend fun ingestPdf(source: PdfIngestionSource, principal: PrincipalId, correlation: String): PdfDerivativeGenerationCoordinationOutcome { calls += "pdf"; return PdfDerivativeGenerationCoordinationOutcome.ExtractionFailed("spy") }
    }
    private data class Fixture(val file: String, val sha256: String, val length: Int, val mediaType: String, val expected: Expected)
    private data class Expected(val format: TierADocumentFormat?, val product: String?) {
        companion object { val TIER_B = Expected(null, null) }
    }
    companion object {
        val FIXTURE_ROOT = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures")
        private val FIXTURES = listOf(
            Fixture("01-searchable-simple.pdf", "7320a9921a3af2b89c56360fd7eea041ed4d2a2146bc4bd540363232e7b8a3db", 75717, "application/pdf", Expected(TierADocumentFormat.PDF, "Apache Tika")),
            Fixture("02-multicolumn-complex.pdf", "d5db7f7497a7032b45776d95c9e712aa1d027830443df760451504f389886c15", 74067, "application/pdf", Expected(TierADocumentFormat.PDF, "Apache Tika")),
            Fixture("03-scanned.pdf", "64039b3200b34c67ce1f993c1cb1f98a115390d6aa25541ceb8a60e674441149", 89913, "application/pdf", Expected.TIER_B),
            Fixture("04-structured.docx", "9cd12baf98a26a8a0edf8d7caeb5a0f821f36d39dbe2e24e16eddbab40e5d49d", 38561, GovernedTierADocumentIngestionRouter.DOCX, Expected(TierADocumentFormat.DOCX, "Apache POI")),
            Fixture("05-email-with-attachment.eml", "9bcb75404cbbe9959f55a2740d33e7b9dfe1ca365bdf5683962d3dfc073c5302", 1039, "message/rfc822", Expected(TierADocumentFormat.EML, "Apache James Mime4j")),
            Fixture("06-structured.csv", "8e560383b8dcdc9a637e20614e46acee7af21596c0ff9d168db83e3ae07120f1", 248, "text/csv", Expected(TierADocumentFormat.CSV, "Apache Commons CSV")),
            Fixture("07-text-image.png", "434e7af5da3312c63b955919742dfefc6ccba7f8ad457ff1152d81554d59db83", 57628, "image/png", Expected.TIER_B),
        )
    }
}
