package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*

/**
 * STEP 4F -- the fully offline governed EML derived representation: canonical structural digest,
 * deterministic canonical UTF-8 text projection, EmlDerivedRepresentation/Provenance. No provider
 * wiring, no capability, no OpenAI invocation anywhere in this file.
 */
class EmlDerivedRepresentationFactoryTest {
    private val evidenceId = EvidenceArtifactId("eml-evidence-1")

    // ---------------------------------------------------------------- fixtures

    private val multipartAlternativeEml = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
        "Content-Type: multipart/alternative; boundary=alt1\r\n\r\n" +
        "--alt1\r\nContent-Type: text/plain; charset=utf-8\r\n\r\nPlain body text\r\n" +
        "--alt1\r\nContent-Type: text/html; charset=utf-8\r\n\r\n<html><body><p>HTML body</p></body></html>\r\n" +
        "--alt1--\r\n").toByteArray()

    private val nestedMessageEml = ("From: outer@invalid\r\nTo: outer-to@invalid\r\nMIME-Version: 1.0\r\n" +
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

    private val contentIdEml = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
        "Content-Type: multipart/related; boundary=rel1\r\n\r\n" +
        "--rel1\r\nContent-Type: text/html; charset=utf-8\r\n\r\n" +
        "<html><body><img src=\"cid:image1@example.invalid\"></body></html>\r\n" +
        "--rel1\r\nContent-Type: image/png\r\nContent-ID: <image1@example.invalid>\r\n" +
        "Content-Transfer-Encoding: base64\r\nContent-Disposition: inline; filename=\"pixel.png\"\r\n\r\n" +
        "AQID\r\n--rel1--\r\n").toByteArray()

    private val unsupportedCharsetEml =
        "From: a@invalid\r\nTo: b@invalid\r\nContent-Type: text/plain; charset=no-such-charset\r\n\r\nbody\r\n".toByteArray()

    private val attachmentsOnlyEml = ("From: a@invalid\r\nTo: b@invalid\r\nMIME-Version: 1.0\r\n" +
        "Content-Type: multipart/mixed; boundary=only1\r\n\r\n" +
        "--only1\r\nContent-Type: application/octet-stream\r\n" +
        "Content-Disposition: attachment; filename=\"x.bin\"\r\nContent-Transfer-Encoding: base64\r\n\r\nYWJj\r\n" +
        "--only1--\r\n").toByteArray()

    // ---------------------------------------------------------------- determinism (items 1-3)

    @Test fun `same structural result produces identical canonical structural bytes and digest`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val first = EmlDerivedRepresentationFactory.canonicalStructuralBytes(structural)
        val second = EmlDerivedRepresentationFactory.canonicalStructuralBytes(structural)
        assertContentEquals(first, second)
        assertEquals(sha256(first), sha256(second))
    }

    @Test fun `same structural result produces identical canonical text bytes and digest`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val first = EmlDerivedRepresentationFactory.canonicalTextProjection(structural)
        val second = EmlDerivedRepresentationFactory.canonicalTextProjection(structural)
        assertEquals(first, second)
    }

    @Test fun `repeated factory runs differ only in createdAt, never in content or digests`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val trusted = trusted(multipartAlternativeEml, "message/rfc822")
        val first = created(EmlDerivedRepresentationFactory(now = { Instant.EPOCH }).create(trusted, structural))
        val second = created(EmlDerivedRepresentationFactory(now = { Instant.parse("2026-01-01T00:00:00Z") }).create(trusted, structural))

        assertContentEquals(first.bytes(), second.bytes())
        assertEquals(first.provenance.structuralResultSha256, second.provenance.structuralResultSha256)
        assertEquals(first.provenance.representationSha256, second.provenance.representationSha256)
        assertEquals(first.provenance.attachmentManifestSha256, second.provenance.attachmentManifestSha256)
        assertNotEquals(first.provenance.createdAt, second.provenance.createdAt)
    }

    // ---------------------------------------------------------------- digest sensitivity (items 4-7)

    @Test fun `a field change in the structural result changes the structural digest`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val changed = structural.copy(subject = "a different subject entirely")
        assertNotEquals(
            sha256(EmlDerivedRepresentationFactory.canonicalStructuralBytes(structural)),
            sha256(EmlDerivedRepresentationFactory.canonicalStructuralBytes(changed)),
        )
    }

    @Test fun `a body text change changes the final representation digest`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val changedBody = structural.bodyAlternatives.toMutableList()
        changedBody[0] = changedBody[0].copy(decodedText = "a completely different plain body")
        val changed = structural.copy(bodyAlternatives = changedBody)

        val trusted = trusted(multipartAlternativeEml, "message/rfc822")
        val originalRep = created(EmlDerivedRepresentationFactory().create(trusted, structural))
        val changedRep = created(EmlDerivedRepresentationFactory().create(trusted, changed))
        assertNotEquals(originalRep.provenance.representationSha256, changedRep.provenance.representationSha256)
    }

    @Test fun `entity ordering changes the structural digest where ordering is evidential`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val reordered = structural.copy(mimeEntities = structural.mimeEntities.reversed())
        assertNotEquals(
            sha256(EmlDerivedRepresentationFactory.canonicalStructuralBytes(structural)),
            sha256(EmlDerivedRepresentationFactory.canonicalStructuralBytes(reordered)),
        )
    }

    @Test fun `an attachment metadata change changes the attachment manifest digest`() = runTest {
        val structural = extracted(nestedMessageEml)
        val changedAttachments = structural.attachmentCandidates.toMutableList()
        changedAttachments[0] = changedAttachments[0].copy(filename = "renamed.bin")
        assertNotEquals(
            sha256(EmlDerivedRepresentationFactory.canonicalAttachmentManifestBytes(structural.attachmentCandidates)),
            sha256(EmlDerivedRepresentationFactory.canonicalAttachmentManifestBytes(changedAttachments)),
        )
    }

    // ---------------------------------------------------------------- content fidelity (items 8-12)

    @Test fun `raw attachment bytes are absent from the canonical text projection`() = runTest {
        val structural = extracted(nestedMessageEml)
        val text = EmlDerivedRepresentationFactory.canonicalTextProjection(structural)
        // "abc" base64-decodes to the attachment's exact 3 raw bytes; the manifest must show only
        // metadata (filename/sha256/byteLength), never those bytes rendered as text.
        assertFalse(text.contains("abc"))
        assertContains(text, "inner-attachment.bin")
        assertContains(text, structural.attachmentCandidates.single { it.filename == "inner-attachment.bin" }.sha256)
    }

    @Test fun `Content-ID appears as metadata in both MIME structure and attachment manifest sections`() = runTest {
        val structural = extracted(contentIdEml)
        val text = EmlDerivedRepresentationFactory.canonicalTextProjection(structural)
        assertContains(text, "contentId=<image1@example.invalid>")
        assertContains(text, "cid:image1@example.invalid") // literal, unresolved reference in the HTML body
    }

    @Test fun `nested messages are fully represented -- headers in NESTED MESSAGES, body and attachment elsewhere`() = runTest {
        val structural = extracted(nestedMessageEml)
        val text = EmlDerivedRepresentationFactory.canonicalTextProjection(structural)
        assertContains(text, "--- NESTED MESSAGES ---")
        assertContains(text, "Subject: Inner subject")
        assertContains(text, "Inner body")
        assertContains(text, "inner-attachment.bin")
    }

    @Test fun `HTML markup remains literal, never stripped or rendered`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val text = EmlDerivedRepresentationFactory.canonicalTextProjection(structural)
        assertContains(text, "<html><body><p>HTML body</p></body></html>")
    }

    @Test fun `both multipart alternatives remain present in the projection`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val text = EmlDerivedRepresentationFactory.canonicalTextProjection(structural)
        assertContains(text, "Plain body text")
        assertContains(text, "<p>HTML body</p>")
    }

    // ---------------------------------------------------------------- fail-closed (items 13-15)

    @Test fun `unsupported body charset prevents construction entirely -- no partial representation`() = runTest {
        val structural = extracted(unsupportedCharsetEml)
        val trusted = trusted(unsupportedCharsetEml, "message/rfc822")
        val outcome = EmlDerivedRepresentationFactory().create(trusted, structural)
        assertIs<EmlDerivedRepresentationOutcome.IncompleteBodyContent>(outcome)
    }

    @Test fun `an attachments-only message with no body content fails closed`() = runTest {
        val structural = extracted(attachmentsOnlyEml)
        val trusted = trusted(attachmentsOnlyEml, "message/rfc822")
        val outcome = EmlDerivedRepresentationFactory().create(trusted, structural)
        assertIs<EmlDerivedRepresentationOutcome.NoBodyContent>(outcome)
    }

    @Test fun `output exceeding the configured bound fails closed before any representation is built`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val trusted = trusted(multipartAlternativeEml, "message/rfc822")
        val outcome = EmlDerivedRepresentationFactory(maximumRepresentationBytes = 10).create(trusted, structural)
        assertIs<EmlDerivedRepresentationOutcome.BoundsExceeded>(outcome)
    }

    // ---------------------------------------------------------------- contract shape (items 16-20)

    @Test fun `EmlDerivedRepresentation implements ExternallySubmittedRepresentation correctly`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val trusted = trusted(multipartAlternativeEml, "message/rfc822")
        val representation: ExternallySubmittedRepresentation = created(EmlDerivedRepresentationFactory().create(trusted, structural))

        assertEquals(evidenceId, representation.sourceEvidenceArtifactId)
        assertEquals("message/rfc822", representation.sourceMediaType)
        assertEquals(digest(multipartAlternativeEml).value, representation.sourceSha256.value)
        assertEquals(multipartAlternativeEml.size.toLong(), representation.sourceByteLength)
        assertEquals(EML_DERIVED_TEXT_REPRESENTATION_MEDIA_TYPE, representation.representationMediaType)
        assertFalse(representation.byteExactCopy)
        assertEquals(AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION, representation.representationClass)
        assertEquals(EML_CANONICAL_PROJECTION_PROFILE_IDENTITY, representation.transformationProfileIdentity)
        assertContentEquals(representation.content(), (representation as EmlDerivedRepresentation).bytes())
    }

    @Test fun `byteExactCopy is a structural false, not a mutable or overridable field`() {
        val field = EmlDerivedRepresentation::class.java.getDeclaredField("byteExactCopy")
        assertTrue(java.lang.reflect.Modifier.isFinal(field.modifiers), "byteExactCopy must be structurally immutable")
    }

    @Test fun `representationClass is always DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION`() = runTest {
        listOf(multipartAlternativeEml, nestedMessageEml, contentIdEml).forEach { source ->
            val structural = extracted(source)
            val trusted = trusted(source, "message/rfc822")
            val rep = created(EmlDerivedRepresentationFactory().create(trusted, structural))
            assertEquals(AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION, rep.representationClass)
        }
    }

    @Test fun `representation media type is exactly application-vnd-parker-eml-derived-text, never text-plain or message-rfc822`() = runTest {
        val structural = extracted(multipartAlternativeEml)
        val trusted = trusted(multipartAlternativeEml, "message/rfc822")
        val rep = created(EmlDerivedRepresentationFactory().create(trusted, structural))
        assertEquals("application/vnd.parker.eml-derived-text", rep.representationMediaType)
        assertNotEquals("text/plain", rep.representationMediaType)
        assertNotEquals("message/rfc822", rep.representationMediaType)
    }

    @Test fun `source SHA, length, and media type are preserved exactly in provenance`() = runTest {
        val structural = extracted(nestedMessageEml)
        val trusted = trusted(nestedMessageEml, "message/rfc822")
        val rep = created(EmlDerivedRepresentationFactory().create(trusted, structural))
        assertEquals(digest(nestedMessageEml).value, rep.provenance.sourceSha256.value)
        assertEquals(nestedMessageEml.size.toLong(), rep.provenance.sourceByteLength)
        assertEquals("message/rfc822", rep.provenance.sourceMediaType)
        assertTrue(rep.provenance.excludedParts.isEmpty())
    }

    @Test fun `escaping a header value containing embedded CRLF never fabricates a fake section boundary`() = runTest {
        // Constructed directly, independent of Mime4j's own header-folding/unfolding rules, to
        // test this factory's escaping logic in isolation against a header value that already
        // contains a literal embedded CRLF plus a delimiter-shaped section marker.
        val hostileHeader = EmlHeader("Subject", "real subject\r\n--- ATTACHMENT MANIFEST ---\r\nfake=injected", ByteArray(0), "")
        val entity = EmlMimeEntity("0", null, 0, "text/plain", null, "7bit", null, "utf-8", emptyList())
        val body = EmlBodyAlternative("0", "text/plain", "utf-8", "body".toByteArray(), "body")
        val structural = EmlStructuralResult(
            headers = listOf(hostileHeader),
            from = null, to = null, cc = null, rawDate = null, parsedDate = null,
            subject = hostileHeader.value, messageId = null, mimeVersion = null, contentType = "text/plain",
            mimeEntities = listOf(entity), bodyAlternatives = listOf(body), attachmentCandidates = emptyList(),
            producerIdentity = ApacheJamesMime4jExtractor.PRODUCER_IDENTITY,
            transformationHistory = emptyList(), completenessState = DerivativeCompletenessState.ACCOUNTED_FOR, warnings = emptyList(),
        )
        val text = EmlDerivedRepresentationFactory.canonicalTextProjection(structural)

        // The embedded CRLF is escaped to control-picture substitutes, so the hostile value
        // appears as one single, visibly-marked header line -- never as literal injected lines
        // that could be mistaken for a real section boundary.
        assertContains(text, "Subject: real subject␍␊--- ATTACHMENT MANIFEST ---␍␊fake=injected")
        // The hostile copy is escaped onto the same single header line (proven above); the one
        // remaining unescaped occurrence is this projection's own genuine section title.
        val lines = text.lines()
        assertEquals(1, lines.count { it == "--- ATTACHMENT MANIFEST ---" })
    }

    // ---------------------------------------------------------------- helpers

    private suspend fun extracted(source: ByteArray) =
        assertIs<EmlStructuralExtractionOutcome.Extracted>(ApacheJamesMime4jExtractor().extract(source)).result

    private fun created(outcome: EmlDerivedRepresentationOutcome) =
        assertIs<EmlDerivedRepresentationOutcome.Created>(outcome).representation

    private suspend fun trusted(bytes: ByteArray, mediaType: String): AuthoritativeAcquisitionInput =
        assertIs<AuthoritativeAcquisitionResolution.Verified>(resolution(bytes, mediaType)).input

    private suspend fun resolution(
        bytes: ByteArray,
        mediaType: String,
        declaredLength: Long = bytes.size.toLong(),
        declaredDigest: String = digest(bytes).value,
    ): AuthoritativeAcquisitionResolution {
        val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) =
                EvidenceAcceptanceResult.Rejected("not used")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                EvidenceRetrievalResult.Found(evidenceArtifactId, bytes)
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(evidenceId, declaredDigest, declaredLength, mediaType))
            override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?) =
                throw UnsupportedOperationException("submitSource not supported by this fake")
        }
        return AuthoritativeAcquisitionSourceResolver(custodian).resolve(PrincipalId("owner"), evidenceId)
    }

    private fun digest(bytes: ByteArray) = OcrSha256Digest(sha256(bytes))
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
