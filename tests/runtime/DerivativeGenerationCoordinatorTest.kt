package parker.core.runtime

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.CsvStructuralExtractionOutcome
import parker.core.interfaces.CsvStructuralExtractor
import parker.core.interfaces.DerivativeGenerationId
import parker.core.interfaces.DerivativeGenerationRecord
import parker.core.interfaces.DerivativeGenerationStorage
import parker.core.interfaces.DerivativeGenerationStorageException
import parker.core.interfaces.DocumentIngestionAudit
import parker.core.interfaces.DocumentIngestionAuditException
import parker.core.interfaces.DocumentIngestionAuditRecord
import parker.core.interfaces.DocumentIngestionAuditStage
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.PrincipalId

class DerivativeGenerationCoordinatorTest {
    @TempDir lateinit var directory: Path

    @Test
    fun `fixture 06 completes governed sequence and preserves source and provenance`() = runTest {
        val bytes = Files.readAllBytes(ApacheCommonsCsvExtractorTest.FIXTURE)
        val original = bytes.copyOf()
        val storage = FileSystemDerivativeGenerationStorage(directory.resolve("generations").also { Files.createDirectory(it) })
        val auditLog = directory.resolve("ingestion-audit.log")
        val coordinator = coordinator(storage, FileSystemDocumentIngestionAudit(auditLog), listOf("generation-fixture"))
        val outcome = assertIs<DerivativeGenerationCoordinationOutcome.Admitted>(
            coordinator.ingestCsv(source(bytes), PRINCIPAL, "fixture-correlation"),
        )
        assertEquals(ApacheCommonsCsvExtractorTest.EXPECTED_ROWS, outcome.csvStructure.rows)
        assertContentEquals(original, bytes)
        val stored = assertNotNull(storage.retrieve(outcome.record.derivativeGenerationId))
        assertEquals(EvidenceArtifactId("fixture-source"), stored.rootSourceEvidenceArtifactId)
        assertEquals(ApacheCommonsCsvExtractor.PRODUCER_IDENTITY, stored.producerIdentity)
        assertEquals(ApacheCommonsCsvExtractor.TRANSFORMATIONS, stored.transformationHistory)
        val auditLines = Files.readAllLines(auditLog)
        assertEquals(2, auditLines.size)
        assertEquals(listOf("ADMISSION_AUTHORISED", "ADMITTED"), auditLines.map { field(it, "stage") })
        assertEquals(listOf("fixture-correlation", "fixture-correlation"), auditLines.map { decodedField(it, "correlationValue") })
        assertEquals(listOf("generation-fixture", "generation-fixture"), auditLines.map { decodedField(it, "derivativeGenerationId") })
    }

    @Test
    fun `reprocessing creates distinct retained generations and audit histories`() = runTest {
        val bytes = Files.readAllBytes(ApacheCommonsCsvExtractorTest.FIXTURE)
        val storage = FileSystemDerivativeGenerationStorage(directory.resolve("generations").also { Files.createDirectory(it) })
        val audit = RecordingAudit()
        val coordinator = coordinator(storage, audit, listOf("generation-one", "generation-two"))
        val first = assertIs<DerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestCsv(source(bytes), PRINCIPAL, "attempt-one"))
        val second = assertIs<DerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestCsv(source(bytes), PRINCIPAL, "attempt-two"))
        assertNotEquals(first.record.derivativeGenerationId, second.record.derivativeGenerationId)
        assertEquals(first.record, storage.retrieve(first.record.derivativeGenerationId))
        assertEquals(second.record, storage.retrieve(second.record.derivativeGenerationId))
        assertEquals(4, audit.records.size)
    }

    @Test
    fun `extractor failure leaves no generation or audit`() = runTest {
        val storage = SpyStorage()
        val audit = RecordingAudit()
        val extractor = CsvStructuralExtractor { CsvStructuralExtractionOutcome.Malformed("malformed") }
        val outcome = DerivativeGenerationCoordinator(extractor, storage, audit).ingestCsv(source("a".toByteArray()), PRINCIPAL, "c")
        assertIs<DerivativeGenerationCoordinationOutcome.ExtractionFailed>(outcome)
        assertEquals(0, storage.prepareCalls)
        assertEquals(emptyList(), audit.records)
    }

    @Test
    fun `preparation failure writes no admission audit`() = runTest {
        val storage = SpyStorage(failPrepare = true)
        val audit = RecordingAudit()
        val outcome = coordinator(storage, audit, listOf("generation-pfail")).ingestCsv(validSyntheticSource(), PRINCIPAL, "c")
        assertIs<DerivativeGenerationCoordinationOutcome.PreparationFailed>(outcome)
        assertEquals(0, storage.publishCalls)
        assertEquals(emptyList(), audit.records)
    }

    @Test
    fun `authorisation audit failure leaves prepared record unpublished`() = runTest {
        val storage = SpyStorage()
        val audit = RecordingAudit(failAtCall = 1)
        val outcome = coordinator(storage, audit, listOf("generation-afail")).ingestCsv(validSyntheticSource(), PRINCIPAL, "c")
        assertIs<DerivativeGenerationCoordinationOutcome.AuthorisationAuditFailed>(outcome)
        assertEquals(1, storage.prepareCalls)
        assertEquals(0, storage.publishCalls)
        assertEquals(null, storage.admitted)
    }

    @Test
    fun `publication failure retains authorization fact but creates no admitted outcome audit`() = runTest {
        val storage = SpyStorage(failPublish = true)
        val audit = RecordingAudit()
        val outcome = coordinator(storage, audit, listOf("generation-pubfail")).ingestCsv(validSyntheticSource(), PRINCIPAL, "c")
        assertIs<DerivativeGenerationCoordinationOutcome.PublicationFailed>(outcome)
        assertEquals(listOf(DocumentIngestionAuditStage.ADMISSION_AUTHORISED), audit.records.map { it.stage })
        assertEquals(null, storage.admitted)
    }

    @Test
    fun `admitted audit failure returns reconciliation outcome with authorization-backed admitted record`() = runTest {
        val storage = SpyStorage()
        val audit = RecordingAudit(failAtCall = 2)
        val outcome = assertIs<DerivativeGenerationCoordinationOutcome.AdmittedAuditFailed>(
            coordinator(storage, audit, listOf("generation-postfail")).ingestCsv(validSyntheticSource(), PRINCIPAL, "c"),
        )
        assertEquals(outcome.record, storage.admitted)
        assertEquals(listOf(DocumentIngestionAuditStage.ADMISSION_AUTHORISED), audit.records.map { it.stage })
    }

    @Test
    fun `invalid source integrity stops before extraction and persistence`() = runTest {
        val storage = SpyStorage()
        val audit = RecordingAudit()
        val source = CsvIngestionSource(EvidenceArtifactId("source"), "a,b".toByteArray(), "0".repeat(64))
        val outcome = coordinator(storage, audit, listOf("generation-integrity")).ingestCsv(source, PRINCIPAL, "c")
        assertIs<DerivativeGenerationCoordinationOutcome.SourceIntegrityFailed>(outcome)
        assertEquals(0, storage.prepareCalls)
    }

    @Test
    fun `successful coordinator order is prepare authorize publish admitted`() = runTest {
        val trace = mutableListOf<String>()
        val storage = SpyStorage(trace = trace)
        val audit = RecordingAudit(trace = trace)
        val outcome = coordinator(storage, audit, listOf("generation-order")).ingestCsv(validSyntheticSource(), PRINCIPAL, "c")
        assertIs<DerivativeGenerationCoordinationOutcome.Admitted>(outcome)
        assertEquals(listOf("prepare", "audit:ADMISSION_AUTHORISED", "publish", "audit:ADMITTED"), trace)
    }

    @Test
    fun `duplicate generated identity cannot overwrite first admitted generation`() = runTest {
        val bytes = Files.readAllBytes(ApacheCommonsCsvExtractorTest.FIXTURE)
        val storage = FileSystemDerivativeGenerationStorage(directory.resolve("generations").also { Files.createDirectory(it) })
        val audit = RecordingAudit()
        val coordinator = coordinator(storage, audit, listOf("generation-duplicate", "generation-duplicate"))
        val first = assertIs<DerivativeGenerationCoordinationOutcome.Admitted>(coordinator.ingestCsv(source(bytes), PRINCIPAL, "first"))
        assertIs<DerivativeGenerationCoordinationOutcome.PreparationFailed>(coordinator.ingestCsv(source(bytes), PRINCIPAL, "second"))
        assertEquals(first.record, storage.retrieve(first.record.derivativeGenerationId))
        assertEquals(2, audit.records.size)
    }

    @Test
    fun `extractor receives defensive copy and cannot mutate source bytes`() = runTest {
        val bytes = "a,b\r\n1,2\r\n".toByteArray()
        val original = bytes.copyOf()
        val extractor = CsvStructuralExtractor { supplied ->
            supplied[0] = 'X'.code.toByte()
            CsvStructuralExtractionOutcome.Malformed("injected")
        }
        val storage = SpyStorage()
        val outcome = DerivativeGenerationCoordinator(extractor, storage, RecordingAudit())
            .ingestCsv(source(bytes), PRINCIPAL, "copy-test")
        assertIs<DerivativeGenerationCoordinationOutcome.ExtractionFailed>(outcome)
        assertContentEquals(original, bytes)
        assertEquals(0, storage.prepareCalls)
    }

    private fun coordinator(storage: DerivativeGenerationStorage, audit: DocumentIngestionAudit, ids: List<String>): DerivativeGenerationCoordinator {
        val iterator = ids.iterator()
        return DerivativeGenerationCoordinator(
            ApacheCommonsCsvExtractor(), storage, audit,
            idFactory = { DerivativeGenerationId(iterator.next()) },
            now = { Instant.parse("2026-08-23T00:00:00Z") },
        )
    }

    private fun source(bytes: ByteArray) = CsvIngestionSource(EvidenceArtifactId("fixture-source"), bytes, sha256(bytes))
    private fun validSyntheticSource() = source("a,b\r\n1,2\r\n".toByteArray())
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private class RecordingAudit(private val failAtCall: Int? = null, private val trace: MutableList<String>? = null) : DocumentIngestionAudit {
        val records = mutableListOf<DocumentIngestionAuditRecord>()
        private var calls = 0
        override suspend fun record(record: DocumentIngestionAuditRecord) {
            calls++
            if (calls == failAtCall) throw DocumentIngestionAuditException.PersistenceFailure("injected audit failure", IOException("injected"))
            trace?.add("audit:${record.stage.name}")
            records += record
        }
    }

    private class SpyStorage(
        private val failPrepare: Boolean = false,
        private val failPublish: Boolean = false,
        private val trace: MutableList<String>? = null,
    ) : DerivativeGenerationStorage {
        var prepareCalls = 0
        var publishCalls = 0
        var prepared: DerivativeGenerationRecord? = null
        var admitted: DerivativeGenerationRecord? = null
        override suspend fun prepare(record: DerivativeGenerationRecord) {
            prepareCalls++
            if (failPrepare) throw DerivativeGenerationStorageException.PersistenceFailure("injected prepare failure", IOException("injected"))
            trace?.add("prepare")
            prepared = record
        }
        override suspend fun publishPrepared(derivativeGenerationId: DerivativeGenerationId) {
            publishCalls++
            if (failPublish) throw DerivativeGenerationStorageException.PersistenceFailure("injected publish failure", IOException("injected"))
            trace?.add("publish")
            admitted = prepared
            prepared = null
        }
        override suspend fun retrieve(derivativeGenerationId: DerivativeGenerationId): DerivativeGenerationRecord? = admitted
    }

    companion object { val PRINCIPAL = PrincipalId("principal-1") }

    private fun field(line: String, name: String): String =
        line.split('\t').single { it.startsWith("$name=") }.substringAfter('=')

    private fun decodedField(line: String, name: String): String =
        String(Base64.getUrlDecoder().decode(field(line, name)), StandardCharsets.UTF_8)
}
