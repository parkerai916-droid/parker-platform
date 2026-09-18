package parker.core.runtime

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*

class PdfDerivativeGenerationCoordinatorTest {
    @TempDir lateinit var directory: Path

    @Test fun `fixture 01 follows atomic sequence with immutable source`() = runTest {
        val bytes = Files.readAllBytes(TikaPdfStructuralExtractorTest.FIXTURE_01); val original = bytes.copyOf(); val trace = mutableListOf<String>()
        val storage = SpyStorage(trace = trace); val outcome = assertIs<PdfDerivativeGenerationCoordinationOutcome.Admitted>(coordinator(storage, RecordingAudit(trace = trace), listOf("pdf-one")).ingestPdf(source(bytes), PRINCIPAL, "fixture"))
        assertContentEquals(original, bytes); assertEquals("Searchable PDF literal text", outcome.record.derivativeKind)
        assertIs<DerivativeContentIdentity.NoCanonicalSerialization>(outcome.record.contentIdentity)
        assertEquals(listOf("prepare", "audit:ADMISSION_AUTHORISED", "publish", "audit:ADMITTED"), trace)
    }

    @Test fun `scanned fixture requires Tier B without persistence or audit`() = runTest {
        val bytes = Files.readAllBytes(TikaPdfStructuralExtractorTest.FIXTURE_03); val storage = SpyStorage(); val audit = RecordingAudit()
        assertIs<PdfDerivativeGenerationCoordinationOutcome.RequiresTierB>(coordinator(storage, audit, listOf("unused")).ingestPdf(source(bytes), PRINCIPAL, "scan"))
        assertEquals(0, storage.prepareCalls); assertTrue(audit.records.isEmpty())
    }

    @Test fun `reprocessing retains independent generations and histories`() = runTest {
        val bytes = Files.readAllBytes(TikaPdfStructuralExtractorTest.FIXTURE_01); val original = bytes.copyOf()
        val storage = FileSystemDerivativeGenerationStorage(directory.resolve("generations").also(Files::createDirectory)); val audit = RecordingAudit(); val coordinator = coordinator(storage, audit, listOf("pdf-a", "pdf-b"))
        val first = assertIs<PdfDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestPdf(source(bytes), PRINCIPAL, "a")); val second = assertIs<PdfDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestPdf(source(bytes), PRINCIPAL, "b"))
        assertNotEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId); assertNotNull(storage.retrieve(first.record.derivativeGenerationId)); assertNotNull(storage.retrieve(second.record.derivativeGenerationId)); assertEquals(4, audit.records.size); assertContentEquals(original, bytes)
    }

    @Test fun `ordinary searchable PDF reprocessing is idempotent when durable stores support discovery`() = runTest {
        val bytes = Files.readAllBytes(TikaPdfStructuralExtractorTest.FIXTURE_01)
        val generationsRoot = directory.resolve("idempotent-generations").also(Files::createDirectory)
        val contentsRoot = directory.resolve("idempotent-content").also(Files::createDirectory)
        val storage = FileSystemDerivativeGenerationStorage(generationsRoot)
        val contents = FileSystemDerivativeContentStorage(contentsRoot)
        val audit = RecordingAudit()
        val coordinator = coordinator(storage, audit, listOf("unused-a", "unused-b"), contentStorage = contents)

        val first = assertIs<PdfDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestPdf(source(bytes), PRINCIPAL, "idempotent-a"))
        val second = assertIs<PdfDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestPdf(source(bytes), PRINCIPAL, "idempotent-b"))

        assertEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        assertEquals(1, storage.findGenerationsForEvidence(EvidenceArtifactId("pdf-source")).size)
        assertEquals(2, audit.records.size)
        assertNotNull(contents.retrieve(first.record.derivativeGenerationId))
    }

    @Test fun `whole-document fallback remains idempotent without page segments`() = runTest {
        val bytes = Files.readAllBytes(TikaPdfStructuralExtractorTest.FIXTURE_01)
        val generationsRoot = directory.resolve("whole-generations").also(Files::createDirectory)
        val contentsRoot = directory.resolve("whole-content").also(Files::createDirectory)
        val storage = FileSystemDerivativeGenerationStorage(generationsRoot)
        val contents = FileSystemDerivativeContentStorage(contentsRoot)
        val audit = RecordingAudit()
        val wholeDocumentExtractor = PdfStructuralExtractor {
            val extracted = assertIs<PdfStructuralExtractionOutcome.Extracted>(TikaPdfStructuralExtractor().extract(it)).result
            PdfStructuralExtractionOutcome.Extracted(extracted.copy(
                pageTextAssociationAvailable = false,
                pageTextSegments = emptyList(),
                producerIdentity = TikaPdfStructuralExtractor.PRODUCER_IDENTITY,
            ))
        }
        val coordinator = coordinator(storage, audit, listOf("unused-a", "unused-b"), wholeDocumentExtractor, contents)

        val first = assertIs<PdfDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestPdf(source(bytes), PRINCIPAL, "whole-a"))
        val second = assertIs<PdfDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestPdf(source(bytes), PRINCIPAL, "whole-b"))

        assertEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        assertTrue(first.pdfStructure.pageTextSegments.isEmpty())
        assertEquals(1, storage.findGenerationsForEvidence(EvidenceArtifactId("pdf-source")).size)
        assertNotNull(storage.retrieve(first.record.derivativeGenerationId))
    }

    @Test fun `integrity extractor persistence and audit failures preserve boundaries`() = runTest {
        val fixture = Files.readAllBytes(TikaPdfStructuralExtractorTest.FIXTURE_01); val storage = SpyStorage()
        assertIs<PdfDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed>(coordinator(storage, RecordingAudit(), listOf("i")).ingestPdf(PdfIngestionSource(EvidenceArtifactId("pdf-source"), fixture, "0".repeat(64)), PRINCIPAL, "i"))
        val mutating = PdfStructuralExtractor { supplied -> supplied[0] = 0; PdfStructuralExtractionOutcome.Malformed("injected") }; val original = fixture.copyOf()
        assertIs<PdfDerivativeGenerationCoordinationOutcome.ExtractionFailed>(coordinator(storage, RecordingAudit(), listOf("e"), mutating).ingestPdf(source(fixture), PRINCIPAL, "e")); assertContentEquals(original, fixture)
        assertIs<PdfDerivativeGenerationCoordinationOutcome.PreparationFailed>(coordinator(SpyStorage(failPrepare = true), RecordingAudit(), listOf("p")).ingestPdf(source(fixture), PRINCIPAL, "p"))
        val authStorage = SpyStorage(); assertIs<PdfDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed>(coordinator(authStorage, RecordingAudit(1), listOf("a")).ingestPdf(source(fixture), PRINCIPAL, "a")); assertNull(authStorage.admitted)
        val pubAudit = RecordingAudit(); assertIs<PdfDerivativeGenerationCoordinationOutcome.PublicationFailed>(coordinator(SpyStorage(failPublish = true), pubAudit, listOf("u")).ingestPdf(source(fixture), PRINCIPAL, "u")); assertEquals(listOf(DocumentIngestionAuditStage.ADMISSION_AUTHORISED), pubAudit.records.map { it.stage })
        val admittedStorage = SpyStorage(); val reconciled = assertIs<PdfDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed>(coordinator(admittedStorage, RecordingAudit(2), listOf("d")).ingestPdf(source(fixture), PRINCIPAL, "d")); assertEquals(reconciled.record, admittedStorage.admitted)
    }

    private fun coordinator(storage: DerivativeGenerationStorage, audit: DocumentIngestionAudit, ids: List<String>, extractor: PdfStructuralExtractor = TikaPdfStructuralExtractor(), contentStorage: DerivativeContentStorage? = null): DerivativeGenerationCoordinator {
        val iterator = ids.iterator(); return DerivativeGenerationCoordinator(ApacheCommonsCsvExtractor(), storage, audit,
            idFactory = { DerivativeGenerationId(iterator.next()) }, now = { Instant.parse("2026-08-23T00:00:00Z") },
            emlExtractor = ApacheJamesMime4jExtractor(), docxExtractor = ApachePoiXwpfExtractor(), pdfExtractor = extractor,
            contentStorage = contentStorage)
    }
    private fun source(bytes: ByteArray) = PdfIngestionSource(EvidenceArtifactId("pdf-source"), bytes, sha256(bytes))
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private class RecordingAudit(private val failAt: Int? = null, private val trace: MutableList<String>? = null) : DocumentIngestionAudit {
        val records = mutableListOf<DocumentIngestionAuditRecord>(); private var calls = 0
        override suspend fun record(record: DocumentIngestionAuditRecord) { calls++; if (calls == failAt) throw DocumentIngestionAuditException.PersistenceFailure("injected", IOException("injected")); trace?.add("audit:${record.stage}"); records += record }
    }
    private class SpyStorage(private val failPrepare: Boolean = false, private val failPublish: Boolean = false, private val trace: MutableList<String>? = null) : DerivativeGenerationStorage {
        var prepareCalls = 0; var prepared: DerivativeGenerationRecord? = null; var admitted: DerivativeGenerationRecord? = null
        override suspend fun prepare(record: DerivativeGenerationRecord) { prepareCalls++; if (failPrepare) throw DerivativeGenerationStorageException.PersistenceFailure("injected", IOException("injected")); trace?.add("prepare"); prepared = record }
        override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) { if (failPublish) throw DerivativeGenerationStorageException.PersistenceFailure("injected", IOException("injected")); trace?.add("publish"); admitted = prepared; prepared = null }
        override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId) = admitted
    }
    companion object { val PRINCIPAL = PrincipalId("principal-pdf") }
}
