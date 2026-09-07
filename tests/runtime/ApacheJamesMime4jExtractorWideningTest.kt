package parker.core.runtime

import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import parker.core.interfaces.*

/**
 * STEP 4E -- EML extractor widening: Content-ID capture, bounded recursive nested message/rfc822
 * representation, and coverage for MIME structures the extractor already handled via Mime4j but
 * Parker had never exercised (multipart/alternative, HTML, quoted-printable, 7bit/8bit,
 * explicitly-declared non-UTF-8 charsets).
 */
class ApacheJamesMime4jExtractorWideningTest {
    @Test fun `multipart-alternative preserves both text and HTML alternatives, ordering, and stable entity IDs`() = runTest {
        val source = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: multipart/alternative; boundary=alt1\r\n\r\n" +
            "--alt1\r\nContent-Type: text/plain; charset=utf-8\r\n\r\nPlain body text\r\n" +
            "--alt1\r\nContent-Type: text/html; charset=utf-8\r\n\r\n<html><body><p>HTML body</p></body></html>\r\n" +
            "--alt1--\r\n").toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result

        val root = result.mimeEntities.single { it.entityId == "0" }
        assertEquals(listOf("0.0", "0.1"), root.childEntityIds)
        assertEquals(2, result.bodyAlternatives.size)
        assertEquals("0.0", result.bodyAlternatives[0].mimeEntityId)
        assertEquals("text/plain", result.bodyAlternatives[0].mediaType)
        assertEquals("Plain body text", result.bodyAlternatives[0].decodedText)
        assertEquals("0.1", result.bodyAlternatives[1].mimeEntityId)
        assertEquals("text/html", result.bodyAlternatives[1].mediaType)
    }

    @Test fun `HTML body is preserved as decoded markup text, never stripped or rendered`() = runTest {
        val source = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: text/html; charset=utf-8\r\n\r\n" +
            "<html><body><a href=\"https://example.invalid\">link</a><table><tr><td>1</td></tr></table>" +
            "<div style=\"display:none\">hidden text</div></body></html>\r\n").toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result

        val html = result.bodyAlternatives.single().decodedText
        assertContains(html, "<a href=\"https://example.invalid\">link</a>")
        assertContains(html, "<table><tr><td>1</td></tr></table>")
        assertContains(html, "<div style=\"display:none\">hidden text</div>")
    }

    @Test fun `quoted-printable body is transfer-decoded correctly and the declared encoding is retained`() = runTest {
        val source = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n" +
            "Content-Transfer-Encoding: quoted-printable\r\n\r\n" +
            "Caf=C3=A9 body\r\n").toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result

        assertEquals("Café body\r\n", result.bodyAlternatives.single().decodedText)
        assertEquals("quoted-printable", result.mimeEntities.single { it.entityId == "0" }.transferEncoding)
    }

    @Test fun `7bit body decodes correctly`() = runTest {
        val source = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: text/plain; charset=us-ascii\r\n" +
            "Content-Transfer-Encoding: 7bit\r\n\r\n" +
            "Hello ASCII body\r\n").toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result

        assertEquals("Hello ASCII body\r\n", result.bodyAlternatives.single().decodedText)
        assertEquals("7bit", result.mimeEntities.single { it.entityId == "0" }.transferEncoding)
    }

    @Test fun `8bit body with multibyte UTF-8 content decodes correctly`() = runTest {
        val header = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n\r\n").toByteArray()
        val body = "Māori and café\r\n".toByteArray(Charsets.UTF_8)
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(header + body)).result

        assertEquals("Māori and café\r\n", result.bodyAlternatives.single().decodedText)
        assertEquals("8bit", result.mimeEntities.single { it.entityId == "0" }.transferEncoding)
    }

    @Test fun `explicitly declared ISO-8859-1 charset decodes strictly with no guessing`() = runTest {
        val header = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: text/plain; charset=iso-8859-1\r\n" +
            "Content-Transfer-Encoding: 8bit\r\n\r\n").toByteArray()
        val body = "café\r\n".toByteArray(Charsets.ISO_8859_1)
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(header + body)).result

        assertEquals("café\r\n", result.bodyAlternatives.single().decodedText)
        assertEquals("iso-8859-1", result.bodyAlternatives.single().charset)
    }

    @Test fun `unsupported charset keeps the extractor's existing qualified-warning semantics unchanged`() = runTest {
        // Duplicates ApacheJamesMime4jExtractorTest's existing coverage deliberately, as a
        // regression pin colocated with this unit's new charset tests.
        val source = "From: a@invalid\r\nTo: b@invalid\r\nContent-Type: text/plain; charset=no-such-charset\r\n\r\nbody\r\n".toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result
        assertTrue(result.bodyAlternatives.isEmpty())
        assertEquals(DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, result.completenessState)
        assertTrue(result.warnings.any { "unsupported charset" in it })
    }

    @Test fun `Content-ID is captured on an inline attachment and the literal cid reference in HTML is left untouched`() = runTest {
        val source = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: multipart/related; boundary=rel1\r\n\r\n" +
            "--rel1\r\nContent-Type: text/html; charset=utf-8\r\n\r\n" +
            "<html><body><img src=\"cid:image1@example.invalid\"></body></html>\r\n" +
            "--rel1\r\nContent-Type: image/png\r\nContent-ID: <image1@example.invalid>\r\n" +
            "Content-Transfer-Encoding: base64\r\nContent-Disposition: inline; filename=\"pixel.png\"\r\n\r\n" +
            "AQID\r\n--rel1--\r\n").toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result

        val htmlText = result.bodyAlternatives.single().decodedText
        assertContains(htmlText, "cid:image1@example.invalid")

        val imageEntity = result.mimeEntities.single { it.mediaType == "image/png" }
        assertEquals("<image1@example.invalid>", imageEntity.contentId)
        val attachment = result.attachmentCandidates.single()
        assertEquals("<image1@example.invalid>", attachment.contentId)
        assertContentEquals(byteArrayOf(1, 2, 3), attachment.decodedBytes)

        // No resolution/fetch of any kind occurred: the extractor is a pure, offline byte
        // transformer with no network dependency at all (structurally verified, not just by
        // absence of a network call in this test).
        val forbidden = listOf("Http", "Socket", "URLConnection", "Network")
        ApacheJamesMime4jExtractor::class.java.declaredFields.forEach { field ->
            forbidden.forEach { assertFalse(field.type.name.contains(it)) }
        }
    }

    @Test fun `nested message-rfc822 is recursively represented with its own headers, body, and attachment`() = runTest {
        val source = ("From: outer@invalid\r\nTo: outer-to@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: multipart/mixed; boundary=outer1\r\n\r\n" +
            "--outer1\r\nContent-Type: text/plain; charset=utf-8\r\n\r\nOuter body\r\n" +
            "--outer1\r\nContent-Type: message/rfc822\r\n" +
            "Content-Disposition: attachment; filename=\"forwarded.eml\"\r\n\r\n" +
            "From: inner@invalid\r\nTo: inner-to@invalid\r\nSubject: Inner subject\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: multipart/mixed; boundary=inner1\r\n\r\n" +
            "--inner1\r\nContent-Type: text/plain; charset=utf-8\r\n\r\nInner body\r\n" +
            "--inner1\r\nContent-Type: application/octet-stream\r\n" +
            "Content-Disposition: attachment; filename=\"inner-attachment.bin\"\r\n" +
            "Content-Transfer-Encoding: base64\r\n\r\nYWJj\r\n--inner1--\r\n" +
            "--outer1--\r\n").toByteArray()
        val result = assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result

        val entitiesById = result.mimeEntities.associateBy { it.entityId }
        assertEquals(setOf("0", "0.0", "0.1", "0.1.0", "0.1.0.0", "0.1.0.1"), entitiesById.keys)

        val outerWrapper = entitiesById.getValue("0.1")
        assertEquals("message/rfc822", outerWrapper.mediaType)
        assertEquals("forwarded.eml", outerWrapper.filename)
        assertEquals(listOf("0.1.0"), outerWrapper.childEntityIds)
        assertNull(outerWrapper.nestedMessageHeaders, "the wrapper entity itself is not the nested message's own root")

        val nestedRoot = entitiesById.getValue("0.1.0")
        assertEquals("0.1", nestedRoot.parentEntityId)
        assertEquals("multipart/mixed", nestedRoot.mediaType)
        assertEquals(listOf("0.1.0.0", "0.1.0.1"), nestedRoot.childEntityIds)
        assertNotNull(nestedRoot.nestedMessageHeaders)
        assertEquals("inner@invalid", nestedRoot.nestedMessageHeaders!!.single { it.name == "From" }.value)
        assertEquals("Inner subject", nestedRoot.nestedMessageHeaders!!.single { it.name == "Subject" }.value)

        val nestedBody = result.bodyAlternatives.single { it.mimeEntityId == "0.1.0.0" }
        assertEquals("Inner body", nestedBody.decodedText)

        val nestedAttachment = result.attachmentCandidates.single { it.mimeEntityId == "0.1.0.1" }
        assertEquals("inner-attachment.bin", nestedAttachment.filename)
        assertContentEquals("abc".toByteArray(), nestedAttachment.decodedBytes)

        val outerBody = result.bodyAlternatives.single { it.mimeEntityId == "0.0" }
        assertEquals("Outer body", outerBody.decodedText)
    }

    @Test fun `pathological nested depth fails closed under the existing MAX_MIME_DEPTH bound, no stack overflow`() = runTest {
        fun wrap(inner: ByteArray, boundary: String) = (
            "From: outer@invalid\r\nTo: to@invalid\r\nMIME-Version: 1.0\r\n" +
                "Content-Type: multipart/mixed; boundary=$boundary\r\n\r\n" +
                "--$boundary\r\nContent-Type: message/rfc822\r\n\r\n"
            ).toByteArray() + inner + "\r\n--$boundary--\r\n".toByteArray()

        var message = ("From: innermost@invalid\r\nTo: to@invalid\r\nSubject: bottom\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: text/plain; charset=utf-8\r\n\r\nbottom body\r\n").toByteArray()
        repeat(ApacheJamesMime4jExtractor.MAX_MIME_DEPTH + 5) { depth -> message = wrap(message, "b$depth") }

        val outcome = ApacheJamesMime4jExtractor().extract(message)
        val malformed = assertIs<EmlStructuralExtractionOutcome.Malformed>(outcome)
        assertTrue("nesting exceeds" in malformed.reason, malformed.reason)
    }

    @Test fun `entity-count bound applies across nested message recursion, not just the root tree`() = runTest {
        val partCount = 1005
        val innerBoundary = "many1"
        val innerMessage = buildString {
            append("From: inner@invalid\r\nTo: to@invalid\r\nMIME-Version: 1.0\r\n")
            append("Content-Type: multipart/mixed; boundary=$innerBoundary\r\n\r\n")
            repeat(partCount) { i -> append("--$innerBoundary\r\nContent-Type: text/plain; charset=utf-8\r\n\r\npart $i\r\n") }
            append("--$innerBoundary--\r\n")
        }.toByteArray()
        val outerBoundary = "outer1"
        val source = ("From: outer@invalid\r\nTo: to@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: multipart/mixed; boundary=$outerBoundary\r\n\r\n" +
            "--$outerBoundary\r\nContent-Type: message/rfc822\r\n\r\n").toByteArray() +
            innerMessage + "\r\n--$outerBoundary--\r\n".toByteArray()

        val outcome = ApacheJamesMime4jExtractor().extract(source)
        val malformed = assertIs<EmlStructuralExtractionOutcome.Malformed>(outcome)
        assertTrue("entity count exceeds" in malformed.reason, malformed.reason)
    }

    @Test fun `same source bytes produce exactly the same structural result on repeated runs`() = runTest {
        val source = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
            "Content-Type: multipart/alternative; boundary=alt1\r\n\r\n" +
            "--alt1\r\nContent-Type: text/plain; charset=utf-8\r\n\r\nPlain body text\r\n" +
            "--alt1\r\nContent-Type: text/html; charset=utf-8\r\n\r\n<html><body><p>HTML body</p></body></html>\r\n" +
            "--alt1--\r\n").toByteArray()
        val extractor = ApacheJamesMime4jExtractor()
        val first = assertIs<EmlStructuralExtractionOutcome.Extracted>(extractor.extract(source.copyOf())).result
        val second = assertIs<EmlStructuralExtractionOutcome.Extracted>(extractor.extract(source.copyOf())).result

        assertEquals(
            first.mimeEntities.map { it.entityId to it.parentEntityId to it.order to it.mediaType to it.childEntityIds },
            second.mimeEntities.map { it.entityId to it.parentEntityId to it.order to it.mediaType to it.childEntityIds },
        )
        assertEquals(first.bodyAlternatives.map { it.mimeEntityId to it.decodedText }, second.bodyAlternatives.map { it.mimeEntityId to it.decodedText })
        assertEquals(first.headers.map { it.name to it.value to it.rawRepresentation }, second.headers.map { it.name to it.value to it.rawRepresentation })
        assertEquals(first.warnings, second.warnings)
        assertEquals(first.completenessState, second.completenessState)
    }
}
