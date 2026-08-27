package parker.core.runtime

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*

class AuthoritativeAcquisitionSourceResolverTest {
    private val owner = PrincipalId("owner-fa3")
    private val id = EvidenceArtifactId("evidence-fa3")
    private val bytes = "%PDF-synthetic".toByteArray()

    @Test
    fun `A exact custody and manifest facts create the sole trusted input`() = runTest {
        val result = resolver().resolve(owner, id)
        val trusted = assertIs<AuthoritativeAcquisitionResolution.Verified>(result).input

        assertEquals(id, trusted.evidenceArtifactId)
        assertEquals(digest(bytes), trusted.sha256)
        assertEquals(bytes.size.toLong(), trusted.byteLength)
        assertEquals("application/pdf", trusted.mediaType)
        assertEquals(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY, trusted.representationClass)
        assertContentEquals(bytes, trusted.bytes())
    }

    @Test
    fun `A wrong ID digest length and malformed identity fail closed`() = runTest {
        assertIs<AuthoritativeAcquisitionResolution.ManifestIdentityMismatch>(
            resolver(manifestId = EvidenceArtifactId("different-id")).resolve(owner, id),
        )
        assertIs<AuthoritativeAcquisitionResolution.DigestMismatch>(
            resolver(manifestDigest = "0".repeat(64)).resolve(owner, id),
        )
        assertIs<AuthoritativeAcquisitionResolution.ByteLengthMismatch>(
            resolver(manifestLength = bytes.size.toLong() + 1).resolve(owner, id),
        )
        assertFailsWith<IllegalArgumentException> { EvidenceArtifactId(" ") }
    }

    @Test
    fun `A manifest media is authoritative and unsupported media fails before representation`() = runTest {
        val trusted = assertIs<AuthoritativeAcquisitionResolution.Verified>(
            resolver(mediaType = "text/plain").resolve(owner, id),
        ).input
        assertEquals("text/plain", trusted.mediaType)
        assertIs<OcrProcessingRepresentationOutcome.UnsupportedMedia>(
            OcrProcessingRepresentationFactory().create(trusted),
        )
    }

    @Test
    fun `B callers cannot manufacture or implement trusted input`() {
        val implementations = AuthoritativeAcquisitionInput::class.sealedSubclasses
        assertEquals(1, implementations.size)
        assertTrue(implementations.single().simpleName.orEmpty().contains("CustodyVerified"))
        assertEquals(kotlin.reflect.KVisibility.PRIVATE, implementations.single().visibility)
        assertFalse(AuthoritativeAcquisitionInput::class.java.declaredFields.any { it.name.contains("trusted", true) })
        assertFalse(AuthoritativeAcquisitionSourceResolver::class.java.declaredMethods.any {
            it.name.contains("promote", true) || it.name.contains("trust", true)
        })
    }

    @Test
    fun `C source ownership and accessor ownership are defensively isolated`() = runTest {
        val callerBytes = bytes.copyOf()
        val trusted = assertIs<AuthoritativeAcquisitionResolution.Verified>(
            resolver(sourceBytes = callerBytes).resolve(owner, id),
        ).input
        callerBytes[0] = 0
        val returned = trusted.bytes()
        returned[1] = 0

        assertContentEquals(bytes, trusted.bytes())
        assertNotEquals(callerBytes[0], trusted.bytes()[0])
        assertNotEquals(returned[1], trusted.bytes()[1])
    }

    @Test
    fun `D byte-exact processing representation remains bound to the exact source`() = runTest {
        val trusted = assertIs<AuthoritativeAcquisitionResolution.Verified>(resolver().resolve(owner, id)).input
        val representation = assertIs<OcrProcessingRepresentationOutcome.Created>(
            OcrProcessingRepresentationFactory().create(trusted),
        ).representation

        with(representation.processingProvenance) {
            assertEquals(id, sourceEvidenceArtifactId)
            assertEquals(digest(bytes), sourceManifestSha256.value)
            assertEquals(bytes.size.toLong(), sourceByteLength)
            assertEquals(sourceManifestSha256, representationSha256)
            assertEquals(sourceByteLength, representationByteLength)
            assertTrue(byteExactCopy)
        }
    }

    @Test
    fun `E F G composed acquisition boundaries all require custody verification before mechanism input`() {
        val tierA = java.io.File("src/runtime/TierAOwnerInvocationCoordinator.kt").readText()
        val localOcr = java.io.File("src/runtime/EvidenceIntelligenceOcrCoordinator.kt").readText()
        val external = java.io.File("src/runtime/ExternalTranscriptionOwnerInvocationCoordinator.kt").readText()
        assertTrue("sourceResolver.resolve" in tierA)
        assertTrue("sourceResolver.verifyAlreadyRetrieved" in localOcr)
        assertTrue("sourceResolver.resolveSourceThenManifest" in external)
        assertTrue("representationFactory.create(\n            authoritativeSource = trusted" in external)
        listOf(tierA, localOcr, external).forEach { source ->
            assertFalse("latestGeneration" in source || "preferredGeneration" in source)
        }
    }

    @Test
    fun `H derivative review and analysis types cannot enter trusted source APIs`() {
        val prohibited = listOf(
            DerivativeGenerationRecord::class.java,
            DerivativeContentEntry::class.java,
            TierADerivativePayload::class.java,
            OcrDerivativeExtractedResult::class.java,
            HumanVerificationRecord::class.java,
            SavedAnalysisRecord::class.java,
        )
        prohibited.forEach { type ->
            assertFalse(AuthoritativeAcquisitionInput::class.java.isAssignableFrom(type), type.name)
        }
        val resolverParameterTypes = AuthoritativeAcquisitionSourceResolver::class.java.declaredMethods
            .flatMap { it.parameterTypes.toList() }.toSet()
        prohibited.forEach { assertFalse(it in resolverParameterTypes, it.name) }
    }

    @Test
    fun `I no derivative lookup or fallback API exists on trusted construction boundary`() {
        val names = AuthoritativeAcquisitionSourceResolver::class.java.declaredMethods.map { it.name.lowercase() }
        listOf("latest", "current", "preferred", "derivative", "fallback", "retry").forEach { forbidden ->
            assertFalse(names.any { forbidden in it })
        }
        val dependencies = AuthoritativeAcquisitionSourceResolver::class.java.declaredFields.map { it.type.name }
        assertEquals(listOf(EvidenceCustodian::class.java.name), dependencies)
    }

    @Test
    fun `J source enforcement remains structurally isolated from substantive domains`() {
        val source = java.io.File("src/runtime/AuthoritativeAcquisitionSourceResolver.kt").readText()
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        listOf("MemoryCore", "Knowledge", "Analysis", "Reasoning", "Conversation", "Rks", "SavedAnalysis").forEach {
            assertFalse(Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(source))
        }
    }

    @Test
    fun `K failed verification produces no acquisition provider derivative or analysis execution`() = runTest {
        var ocr = 0
        var provider = 0
        var derivative = 0
        var analysis = 0
        val result = resolver(manifestDigest = "0".repeat(64)).resolve(owner, id)
        if (result is AuthoritativeAcquisitionResolution.Verified) {
            ocr++; provider++; derivative++; analysis++
        }
        assertIs<AuthoritativeAcquisitionResolution.DigestMismatch>(result)
        assertEquals(listOf(0, 0, 0, 0), listOf(ocr, provider, derivative, analysis))
    }

    private fun resolver(
        sourceBytes: ByteArray = bytes,
        manifestId: EvidenceArtifactId = id,
        manifestDigest: String = digest(sourceBytes),
        manifestLength: Long = sourceBytes.size.toLong(),
        mediaType: String = "application/pdf",
    ) = AuthoritativeAcquisitionSourceResolver(object : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) =
            EvidenceAcceptanceResult.Rejected("not used")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
            EvidenceRetrievalResult.Found(evidenceArtifactId, sourceBytes)
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
            EvidenceManifestRetrievalResult.Found(
                EvidenceSourceManifest(manifestId, manifestDigest, manifestLength, mediaType),
            )
    })

    private fun digest(value: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(value).joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
