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

class DocxDerivativeGenerationCoordinatorTest {
    @TempDir lateinit var directory: Path

    @Test fun `fixture completes atomic admission and source remains immutable`() = runTest {
        val bytes = Files.readAllBytes(ApachePoiXwpfExtractorTest.FIXTURE); val original = bytes.copyOf(); val trace = mutableListOf<String>()
        val storage = SpyStorage(trace = trace); val audit = RecordingAudit(trace = trace)
        val outcome = assertIs<DocxDerivativeGenerationCoordinationOutcome.Admitted>(coordinator(storage, audit, listOf("docx-one")).ingestDocx(source(bytes), PRINCIPAL, "fixture"))
        assertContentEquals(original, bytes)
        assertEquals("DOCX OOXML structure", outcome.record.derivativeKind)
        assertIs<DerivativeContentIdentity.NoCanonicalSerialization>(outcome.record.contentIdentity)
        assertEquals(ApachePoiXwpfExtractor.PRODUCER_IDENTITY, outcome.record.producerIdentity)
        assertEquals(listOf("prepare", "audit:ADMISSION_AUTHORISED", "publish", "audit:ADMITTED"), trace)
    }

    @Test fun `reprocessing retains distinct generations and audit histories without source rewrite`() = runTest {
        val bytes = Files.readAllBytes(ApachePoiXwpfExtractorTest.FIXTURE); val original = bytes.copyOf()
        val storage = FileSystemDerivativeGenerationStorage(directory.resolve("generations").also(Files::createDirectory)); val audit = RecordingAudit()
        val coordinator = coordinator(storage, audit, listOf("docx-first", "docx-second"))
        val first = assertIs<DocxDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestDocx(source(bytes), PRINCIPAL, "first"))
        val second = assertIs<DocxDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestDocx(source(bytes), PRINCIPAL, "second"))
        assertNotEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        assertNotNull(storage.retrieve(first.record.derivativeGenerationId)); assertNotNull(storage.retrieve(second.record.derivativeGenerationId))
        assertEquals(4, audit.records.size); assertContentEquals(original, bytes)
    }

    @Test fun `digest mismatch and extractor mutation cannot reach preparation`() = runTest {
        val storage = SpyStorage(); val bytes = "docx".toByteArray(); val original = bytes.copyOf()
        val bad = DocxIngestionSource(EvidenceArtifactId("docx-source"), bytes, "0".repeat(64))
        assertIs<DocxDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed>(coordinator(storage, RecordingAudit(), listOf("x")).ingestDocx(bad, PRINCIPAL, "bad"))
        val mutator = DocxStructuralExtractor { supplied -> supplied[0] = 0; DocxStructuralExtractionOutcome.Malformed("injected") }
        assertIs<DocxDerivativeGenerationCoordinationOutcome.ExtractionFailed>(coordinator(storage, RecordingAudit(), listOf("y"), mutator).ingestDocx(source(bytes), PRINCIPAL, "copy"))
        assertContentEquals(original, bytes); assertEquals(0, storage.prepareCalls)
    }

    @Test fun `all persistence and audit failures retain established boundaries`() = runTest {
        val bytes = Files.readAllBytes(ApachePoiXwpfExtractorTest.FIXTURE)
        assertIs<DocxDerivativeGenerationCoordinationOutcome.PreparationFailed>(coordinator(SpyStorage(failPrepare = true), RecordingAudit(), listOf("p")).ingestDocx(source(bytes), PRINCIPAL, "p"))
        val authStorage = SpyStorage(); assertIs<DocxDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed>(coordinator(authStorage, RecordingAudit(1), listOf("a")).ingestDocx(source(bytes), PRINCIPAL, "a")); assertNull(authStorage.admitted)
        val pubAudit = RecordingAudit(); assertIs<DocxDerivativeGenerationCoordinationOutcome.PublicationFailed>(coordinator(SpyStorage(failPublish = true), pubAudit, listOf("u")).ingestDocx(source(bytes), PRINCIPAL, "u")); assertEquals(listOf(DocumentIngestionAuditStage.ADMISSION_AUTHORISED), pubAudit.records.map { it.stage })
        val admittedStorage = SpyStorage(); val reconciliation = assertIs<DocxDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed>(coordinator(admittedStorage, RecordingAudit(2), listOf("d")).ingestDocx(source(bytes), PRINCIPAL, "d")); assertEquals(reconciliation.record, admittedStorage.admitted)
    }

    private fun coordinator(storage: DerivativeGenerationStorage, audit: DocumentIngestionAudit, ids: List<String>, extractor: DocxStructuralExtractor = ApachePoiXwpfExtractor()): DerivativeGenerationCoordinator {
        val iterator = ids.iterator(); return DerivativeGenerationCoordinator(ApacheCommonsCsvExtractor(), storage, audit,
            idFactory = { DerivativeGenerationId(iterator.next()) }, now = { Instant.parse("2026-08-23T00:00:00Z") },
            emlExtractor = ApacheJamesMime4jExtractor(), docxExtractor = extractor)
    }
    private fun source(bytes: ByteArray) = DocxIngestionSource(EvidenceArtifactId("docx-source"), bytes, sha256(bytes))
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
    companion object { val PRINCIPAL = PrincipalId("principal-docx") }
}
