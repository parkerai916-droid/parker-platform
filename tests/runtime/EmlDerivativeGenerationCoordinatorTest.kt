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

class EmlDerivativeGenerationCoordinatorTest {
    @TempDir lateinit var directory: Path
    private val fixture = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/05-email-with-attachment.eml")

    @Test fun `fixture admission preserves candidate boundary and exact governed sequence`() = runTest {
        val bytes = Files.readAllBytes(fixture)
        val original = bytes.copyOf()
        val trace = mutableListOf<String>()
        val storage = SpyStorage(trace = trace)
        val audit = RecordingAudit(trace = trace)
        val outcome = assertIs<EmlDerivativeGenerationCoordinationOutcome.Admitted>(coordinator(storage, audit, listOf("eml-one"))
            .ingestEml(source(bytes), PRINCIPAL, "eml-fixture"))
        assertContentEquals(original, bytes)
        assertEquals("EML MIME structure", outcome.record.derivativeKind)
        assertIs<DerivativeContentIdentity.NoCanonicalSerialization>(outcome.record.contentIdentity)
        assertEquals(ApacheJamesMime4jExtractor.PRODUCER_IDENTITY, outcome.record.producerIdentity)
        assertEquals(listOf("prepare", "audit:ADMISSION_AUTHORISED", "publish", "audit:ADMITTED"), trace)
        val candidate = outcome.childSourceCandidates.single()
        assertEquals(EvidenceArtifactId("eml-source"), candidate.rootSourceEvidenceArtifactId)
        assertEquals("0.1", candidate.originatingMimeEntityId)
        assertEquals(95, candidate.byteLength)
        assertEquals("3b5a3e7f8d5d7873feedfb2d4c026a73c94308016e82cf4f0f4dbd9eeb740828", candidate.sha256)
        // Candidate bytes are returned only; this coordinator has no EvidenceCustodian dependency or admission call.
        assertEquals(1, storage.publishCalls)
    }

    @Test fun `reprocessing retains independent generations audits and equal attachment identity`() = runTest {
        val bytes = Files.readAllBytes(fixture)
        val root = directory.resolve("generations").also(Files::createDirectory)
        val storage = FileSystemDerivativeGenerationStorage(root)
        val audit = RecordingAudit()
        val coordinator = coordinator(storage, audit, listOf("eml-first", "eml-second"))
        val first = assertIs<EmlDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestEml(source(bytes), PRINCIPAL, "one"))
        val second = assertIs<EmlDerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestEml(source(bytes), PRINCIPAL, "two"))
        assertNotEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        assertNotNull(storage.retrieve(first.record.derivativeGenerationId))
        assertNotNull(storage.retrieve(second.record.derivativeGenerationId))
        assertEquals(first.childSourceCandidates.single().sha256, second.childSourceCandidates.single().sha256)
        assertEquals(4, audit.records.size)
    }

    @Test fun `extractor receives defensive copy and cannot mutate governed source`() = runTest {
        val bytes = "message".toByteArray(); val original = bytes.copyOf()
        val extractor = EmlStructuralExtractor { supplied ->
            supplied[0] = 'X'.code.toByte(); EmlStructuralExtractionOutcome.Malformed("injected")
        }
        val storage = SpyStorage()
        val outcome = coordinator(storage, RecordingAudit(), listOf("unused"), extractor)
            .ingestEml(source(bytes), PRINCIPAL, "copy")
        assertIs<EmlDerivativeGenerationCoordinationOutcome.ExtractionFailed>(outcome)
        assertContentEquals(original, bytes)
        assertEquals(0, storage.prepareCalls)
    }

    @Test fun `source mismatch and extractor failure cannot prepare`() = runTest {
        val storage = SpyStorage()
        val bad = EmlIngestionSource(EvidenceArtifactId("eml-source"), "x".toByteArray(), "0".repeat(64))
        assertIs<EmlDerivativeGenerationCoordinationOutcome.SourceIntegrityFailed>(coordinator(storage, RecordingAudit(), listOf("x")).ingestEml(bad, PRINCIPAL, "c"))
        assertEquals(0, storage.prepareCalls)
    }

    @Test fun `preparation authorization publication and admitted-audit failures retain boundaries`() = runTest {
        val bytes = Files.readAllBytes(fixture)
        assertIs<EmlDerivativeGenerationCoordinationOutcome.PreparationFailed>(coordinator(SpyStorage(failPrepare = true), RecordingAudit(), listOf("p")).ingestEml(source(bytes), PRINCIPAL, "p"))
        val authStorage = SpyStorage()
        assertIs<EmlDerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed>(coordinator(authStorage, RecordingAudit(failAtCall = 1), listOf("a")).ingestEml(source(bytes), PRINCIPAL, "a"))
        assertNull(authStorage.admitted)
        val publishAudit = RecordingAudit(); val publishStorage = SpyStorage(failPublish = true)
        assertIs<EmlDerivativeGenerationCoordinationOutcome.PublicationFailed>(coordinator(publishStorage, publishAudit, listOf("u")).ingestEml(source(bytes), PRINCIPAL, "u"))
        assertEquals(listOf(DocumentIngestionAuditStage.ADMISSION_AUTHORISED), publishAudit.records.map { it.stage })
        val admittedStorage = SpyStorage(); val admittedAudit = RecordingAudit(failAtCall = 2)
        val reconciliation = assertIs<EmlDerivativeGenerationCoordinationOutcome.AdmittedAuditFailed>(coordinator(admittedStorage, admittedAudit, listOf("d")).ingestEml(source(bytes), PRINCIPAL, "d"))
        assertEquals(reconciliation.record, admittedStorage.admitted)
    }

    private fun coordinator(storage: DerivativeGenerationStorage, audit: DocumentIngestionAudit, ids: List<String>,
        extractor: EmlStructuralExtractor = ApacheJamesMime4jExtractor()): DerivativeGenerationCoordinator {
        val iterator = ids.iterator()
        return DerivativeGenerationCoordinator(ApacheCommonsCsvExtractor(), storage, audit,
            idFactory = { DerivativeGenerationId(iterator.next()) }, now = { Instant.parse("2026-08-23T00:00:00Z") }, emlExtractor = extractor)
    }
    private fun source(bytes: ByteArray) = EmlIngestionSource(EvidenceArtifactId("eml-source"), bytes, sha256(bytes))
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private class RecordingAudit(private val failAtCall: Int? = null, private val trace: MutableList<String>? = null) : DocumentIngestionAudit {
        val records = mutableListOf<DocumentIngestionAuditRecord>(); private var calls = 0
        override suspend fun record(record: DocumentIngestionAuditRecord) { calls++; if (calls == failAtCall) throw DocumentIngestionAuditException.PersistenceFailure("injected", IOException("injected")); trace?.add("audit:${record.stage}"); records += record }
    }
    private class SpyStorage(private val failPrepare: Boolean = false, private val failPublish: Boolean = false,
        private val trace: MutableList<String>? = null) : DerivativeGenerationStorage {
        var prepareCalls = 0; var publishCalls = 0; var prepared: DerivativeGenerationRecord? = null; var admitted: DerivativeGenerationRecord? = null
        override suspend fun prepare(record: DerivativeGenerationRecord) { prepareCalls++; if (failPrepare) throw DerivativeGenerationStorageException.PersistenceFailure("injected", IOException("injected")); trace?.add("prepare"); prepared = record }
        override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) { publishCalls++; if (failPublish) throw DerivativeGenerationStorageException.PersistenceFailure("injected", IOException("injected")); trace?.add("publish"); admitted = prepared; prepared = null }
        override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId) = admitted
    }
    companion object { val PRINCIPAL = PrincipalId("principal-eml") }
}
