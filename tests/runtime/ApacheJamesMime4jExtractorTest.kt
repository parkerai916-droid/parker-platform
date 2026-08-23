package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import parker.core.interfaces.*

class ApacheJamesMime4jExtractorTest {
    private val fixture = Path.of("tests/fixtures/document-ingestion-bakeoff/fixtures/05-email-with-attachment.eml")

    @Test fun `fixture 05 preserves headers MIME hierarchy body and exact attachment bytes`() = runTest {
        val source = Files.readAllBytes(fixture)
        assertEquals(1039, source.size)
        assertEquals("9bcb75404cbbe9959f55a2740d33e7b9dfe1ca365bdf5683962d3dfc073c5302", sha256(source))
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result
        assertEquals("synthetic.sender@example.invalid", result.from)
        assertEquals("synthetic.recipient@example.invalid", result.to)
        assertEquals("synthetic.audit@example.invalid", result.cc)
        assertEquals("Fri, 21 Aug 2026 10:15:00 +1200", result.rawDate)
        assertEquals(Instant.parse("2026-08-20T22:15:00Z"), result.parsedDate)
        assertTrue(DerivativeTransformation.DATE_PARSING in result.transformationHistory)
        assertEquals("PARKER-FIXTURE-2026-005 — Synthetic evidence message", result.subject)
        val rawSubject = result.headers.single { it.name.equals("Subject", true) }
        assertEquals("Subject: PARKER-FIXTURE-2026-005 =?utf-8?b?4oCU?= Synthetic evidence message", rawSubject.rawRepresentation)
        assertContentEquals(rawSubject.rawRepresentation.toByteArray(Charsets.ISO_8859_1), rawSubject.rawBytes)
        assertEquals("<parker-fixture-2026-005@example.invalid>", result.messageId)
        assertEquals(3, result.mimeEntities.size)
        assertEquals(listOf("0.0", "0.1"), result.mimeEntities.single { it.entityId == "0" }.childEntityIds)
        assertEquals("0", result.mimeEntities.single { it.entityId == "0.1" }.parentEntityId)
        val expectedBody = "PARKER-FIXTURE-2026-005\r\nEMAIL BODY CONTROL: MAIL-77105\r\nReference: 000205\r\nUnicode: Māori and café\r\nThis body is synthetic and contains no real case data.\r\n"
        assertEquals(expectedBody, result.bodyAlternatives.single().decodedText)
        val attachment = result.attachmentCandidates.single()
        assertEquals("synthetic-control-005.txt", attachment.filename)
        assertEquals("text/plain", attachment.declaredMimeType)
        assertEquals("attachment", attachment.disposition)
        assertEquals("base64", attachment.transferEncoding)
        assertNull(attachment.charset)
        assertEquals(95, attachment.byteLength)
        assertEquals("3b5a3e7f8d5d7873feedfb2d4c026a73c94308016e82cf4f0f4dbd9eeb740828", attachment.sha256)
        assertEquals(attachment.sha256, sha256(attachment.decodedBytes))
        assertTrue(result.warnings.any { "no declared charset" in it })
    }

    @Test fun `hostile attachment filename remains inert metadata`() = runTest {
        val source = ("From: a@invalid\r\n" +
            "To: b@invalid\r\n" +
            "MIME-Version: 1.0\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-Disposition: attachment; filename=\"../../evil.txt\"\r\n" +
            "Content-Transfer-Encoding: base64\r\n\r\nYWJj\r\n").toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result
        assertEquals("../../evil.txt", result.attachmentCandidates.single().filename)
        assertContentEquals("abc".toByteArray(), result.attachmentCandidates.single().decodedBytes)
    }

    @Test fun `Windows-like hostile filename also remains exact inert metadata`() = runTest {
        val source = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: application/octet-stream\r\n" +
            "Content-Disposition: attachment; filename=\"..\\..\\evil.txt\"\r\n" +
            "Content-Transfer-Encoding: base64\r\n\r\nYWJj\r\n").toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result
        assertEquals("..\\..\\evil.txt", result.attachmentCandidates.single().filename)
        assertContentEquals("abc".toByteArray(), result.attachmentCandidates.single().decodedBytes)
    }

    @Test fun `oversized source fails explicitly before parsing`() = runTest {
        val source = ByteArray(ApacheJamesMime4jExtractor.MAX_MESSAGE_BYTES + 1)
        val malformed = assertIs<EmlStructuralExtractionOutcome.Malformed>(ApacheJamesMime4jExtractor().extract(source))
        assertTrue("adapter limit" in malformed.reason)
    }

    @Test fun `invalid body charset is qualified and not fabricated as text`() = runTest {
        val source = "From: a@invalid\r\nTo: b@invalid\r\nContent-Type: text/plain; charset=no-such-charset\r\n\r\nbody\r\n".toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result
        assertTrue(result.bodyAlternatives.isEmpty())
        assertEquals(DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, result.completenessState)
        assertTrue(result.warnings.any { "unsupported charset" in it })
    }

    @Test fun `malformed transfer encoding is never represented as complete`() = runTest {
        val source = "From: a@invalid\r\nTo: b@invalid\r\nContent-Type: application/octet-stream\r\nContent-Disposition: attachment; filename=x\r\nContent-Transfer-Encoding: base64\r\n\r\n%%%not-base64%%%\r\n".toByteArray()
        val outcome = ApacheJamesMime4jExtractor().extract(source)
        when (outcome) {
            is EmlStructuralExtractionOutcome.Malformed -> assertTrue(outcome.reason.isNotBlank())
            is EmlStructuralExtractionOutcome.Extracted -> assertNotEquals(DerivativeCompletenessState.ACCOUNTED_FOR, outcome.result.completenessState)
        }
    }

    @Test fun `truncated multipart boundary is malformed rather than fabricated complete`() = runTest {
        val source = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: multipart/mixed; boundary=x\r\n\r\n--x\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n\r\nbody\r\n").toByteArray()
        assertIs<EmlStructuralExtractionOutcome.Malformed>(ApacheJamesMime4jExtractor().extract(source))
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
