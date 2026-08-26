package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import kotlin.test.*
import parker.core.interfaces.*

class OcrProcessingRepresentationFactoryTest {
    private val evidenceId = EvidenceArtifactId("evidence-unit-i")
    private val createdAt = Instant.parse("2026-08-26T01:02:03Z")

    @Test
    fun `PDF is copied byte-exactly with governed provenance and identity`() {
        val source = byteArrayOf(1, 2, 3, 4)
        val scope = OcrPageScope(listOf(2, 4))
        val representation = created(factory().create(evidenceId, source, digest(source), "application/pdf", 4, scope, scope))

        assertContentEquals(source, representation.bytes())
        assertEquals(evidenceId, representation.processingProvenance.sourceEvidenceArtifactId)
        assertEquals(4, representation.byteLength)
        with(representation.processingProvenance) {
            assertEquals(digest(source), representationSha256)
            assertEquals(sourceManifestSha256, representationSha256)
            assertEquals(sourceByteLength, representationByteLength)
            assertEquals("application/pdf", sourceMediaType)
            assertEquals(sourceMediaType, representationMediaType)
            assertEquals(scope, requestedPageScope)
            assertEquals(scope, submittedPageScope)
            assertTrue(byteExactCopy)
            assertNull(materialTransformation)
            assertEquals(OcrProcessingRepresentationFactory.PROCESSING_PROFILE_IDENTITY, processingProfileIdentity)
            assertEquals(createdAt, this.createdAt)
        }
    }

    @Test
    fun `supported images remain byte exact and unavailable page scope is not fabricated`() {
        listOf("image/jpeg", "image/png", "image/webp").forEach { media ->
            val source = media.toByteArray()
            val provenance = created(factory().create(evidenceId, source, digest(source), media, source.size.toLong())).processingProvenance
            assertEquals(media, provenance.representationMediaType)
            assertNull(provenance.requestedPageScope)
            assertNull(provenance.submittedPageScope)
            assertEquals(digest(source), provenance.representationSha256)
        }
    }

    @Test
    fun `both source and representation ownership boundaries are defensive`() {
        val source = byteArrayOf(7, 8, 9)
        val expectedDigest = digest(source)
        val representation = created(factory().create(evidenceId, source, expectedDigest, "image/png", 3))
        source[0] = 99
        val accessed = representation.bytes()
        accessed[1] = 88

        assertContentEquals(byteArrayOf(7, 8, 9), representation.bytes())
        assertContentEquals(byteArrayOf(99, 8, 9), source)
        assertEquals(3, representation.processingProvenance.representationByteLength)
        assertEquals(expectedDigest, representation.processingProvenance.representationSha256)
    }

    @Test
    fun `unsupported media and invalid source facts fail closed`() {
        val source = byteArrayOf(1)
        assertIs<OcrProcessingRepresentationOutcome.UnsupportedMedia>(factory().create(evidenceId, source, digest(source), "image/gif", 1))
        assertIs<OcrProcessingRepresentationOutcome.InvalidSourceFacts>(factory().create(evidenceId, byteArrayOf(), digest(source), "application/pdf", 0))
        assertIs<OcrProcessingRepresentationOutcome.SourceLengthMismatch>(factory().create(evidenceId, source, digest(source), "application/pdf", 2))
        assertIs<OcrProcessingRepresentationOutcome.DigestMismatch>(factory().create(evidenceId, source, OcrSha256Digest("0".repeat(64)), "application/pdf", 1))
    }

    @Test
    fun `effective raw limit accepts exactly the limit and rejects over it`() {
        val bounded = factory(pdfLimit = 3, imageLimit = 2)
        val pdf = byteArrayOf(1, 2, 3)
        val image = byteArrayOf(1, 2, 3)
        assertIs<OcrProcessingRepresentationOutcome.Created>(bounded.create(evidenceId, pdf, digest(pdf), "application/pdf", 3))
        assertIs<OcrProcessingRepresentationOutcome.BoundsExceeded>(bounded.create(evidenceId, image, digest(image), "image/png", 3))
    }

    @Test
    fun `factory is structurally representation-only`() {
        val types = OcrProcessingRepresentationFactory::class.java.declaredFields.map { it.type.name }
        val forbidden = listOf("OpenAI", "Http", "Credential", "EvidenceCustodian", "PermissionEngine", "Storage", "Analysis", "Memory", "Knowledge", "Docling", "OwnerUi")
        types.forEach { type -> forbidden.forEach { assertFalse(type.contains(it), "$type contains $it") } }
        val methods = OcrProcessingRepresentationFactory::class.java.declaredMethods.map { it.name.lowercase() }
        listOf("retrieve", "enumerate", "write", "transcribe", "analyse", "selectprovider", "selectmodel").forEach { forbiddenName ->
            assertTrue(methods.none { forbiddenName in it })
        }
    }

    private fun factory(pdfLimit: Long = 1024, imageLimit: Long = 1024) =
        OcrProcessingRepresentationFactory(OcrProcessingRepresentationLimits(pdfLimit, imageLimit)) { createdAt }

    private fun created(outcome: OcrProcessingRepresentationOutcome) =
        assertIs<OcrProcessingRepresentationOutcome.Created>(outcome).representation

    private fun digest(bytes: ByteArray) = OcrSha256Digest(
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) },
    )
}
