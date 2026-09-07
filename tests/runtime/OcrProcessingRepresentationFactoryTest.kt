package parker.core.runtime

import java.security.MessageDigest
import java.time.Instant
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*

class OcrProcessingRepresentationFactoryTest {
    private val evidenceId = EvidenceArtifactId("evidence-unit-i")
    private val createdAt = Instant.parse("2026-08-26T01:02:03Z")

    @Test
    fun `PDF is copied byte-exactly with governed provenance and identity`() = runTest {
        val source = byteArrayOf(1, 2, 3, 4)
        val scope = OcrPageScope(listOf(2, 4))
        val representation = created(factory().create(trusted(source, "application/pdf"), scope, scope))

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
    fun `supported images remain byte exact and unavailable page scope is not fabricated`() = runTest {
        listOf("image/jpeg", "image/png", "image/webp").forEach { media ->
            val source = media.toByteArray()
            val provenance = created(factory().create(trusted(source, media))).processingProvenance
            assertEquals(media, provenance.representationMediaType)
            assertNull(provenance.requestedPageScope)
            assertNull(provenance.submittedPageScope)
            assertEquals(digest(source), provenance.representationSha256)
        }
    }

    @Test
    fun `both source and representation ownership boundaries are defensive`() = runTest {
        val source = byteArrayOf(7, 8, 9)
        val expectedDigest = digest(source)
        val trusted = trusted(source, "image/png")
        val representation = created(factory().create(trusted))
        source[0] = 99
        val accessed = representation.bytes()
        accessed[1] = 88

        assertContentEquals(byteArrayOf(7, 8, 9), representation.bytes())
        assertContentEquals(byteArrayOf(99, 8, 9), source)
        assertEquals(3, representation.processingProvenance.representationByteLength)
        assertEquals(expectedDigest, representation.processingProvenance.representationSha256)
    }

    @Test
    fun `STEP 3 -- valid UTF-8 CSV, ASCII and multibyte, is copied byte-exactly with no material transformation`() = runTest {
        listOf(
            "name,value\na,1\n".toByteArray(Charsets.UTF_8),
            "name,value\nÉtoile,1\n".toByteArray(Charsets.UTF_8),
        ).forEach { source ->
            val representation = created(factory().create(trusted(source, "text/csv")))
            assertContentEquals(source, representation.bytes())
            with(representation.processingProvenance) {
                assertEquals(digest(source), sourceManifestSha256)
                assertEquals(digest(source), representationSha256)
                assertEquals(source.size.toLong(), sourceByteLength)
                assertEquals(source.size.toLong(), representationByteLength)
                assertEquals("text/csv", representationMediaType)
                assertTrue(byteExactCopy)
                assertNull(materialTransformation)
            }
        }
    }

    @Test
    fun `STEP 3 -- malformed, incomplete, overlong, and surrogate-encoded UTF-8 CSV fail closed with InvalidTextEncoding`() = runTest {
        val malformedCases = listOf(
            "incomplete two-byte sequence" to byteArrayOf('a'.code.toByte(), 0xC3.toByte()),
            "invalid continuation byte" to byteArrayOf(0xC2.toByte(), 0x20),
            "lone continuation byte with no leading byte" to byteArrayOf('a'.code.toByte(), 0x80.toByte()),
            "overlong two-byte encoding of ASCII '/'" to byteArrayOf(0xC0.toByte(), 0xAF.toByte()),
            "surrogate code point encoded as UTF-8 (CESU-8 style)" to byteArrayOf(0xED.toByte(), 0xA0.toByte(), 0x80.toByte()),
        )
        malformedCases.forEach { (label, source) ->
            assertIs<OcrProcessingRepresentationOutcome.InvalidTextEncoding>(
                factory().create(trusted(source, "text/csv")),
                "expected InvalidTextEncoding for: $label",
            )
        }
    }

    @Test
    fun `unsupported media and invalid source facts fail closed`() = runTest {
        val source = byteArrayOf(1)
        assertIs<OcrProcessingRepresentationOutcome.UnsupportedMedia>(factory().create(trusted(source, "image/gif")))
        assertIs<OcrProcessingRepresentationOutcome.InvalidSourceFacts>(factory().create(trusted(byteArrayOf(), "application/pdf")))
        assertIs<AuthoritativeAcquisitionResolution.ByteLengthMismatch>(resolution(source, "application/pdf", declaredLength = 2))
        assertIs<AuthoritativeAcquisitionResolution.DigestMismatch>(resolution(source, "application/pdf", declaredDigest = "0".repeat(64)))
    }

    @Test
    fun `effective raw limit accepts exactly the limit and rejects over it`() = runTest {
        val bounded = factory(pdfLimit = 3, imageLimit = 2)
        val pdf = byteArrayOf(1, 2, 3)
        val image = byteArrayOf(1, 2, 3)
        assertIs<OcrProcessingRepresentationOutcome.Created>(bounded.create(trusted(pdf, "application/pdf")))
        assertIs<OcrProcessingRepresentationOutcome.BoundsExceeded>(bounded.create(trusted(image, "image/png")))
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

    private fun digest(bytes: ByteArray) = OcrSha256Digest(
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) },
    )
}
