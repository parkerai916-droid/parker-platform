package parker.core.runtime
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import parker.core.interfaces.EvidenceArtifactId

class UnitOMetadataPreflightTest {
    private val selected = EvidenceArtifactId("selected")
    private val other = EvidenceArtifactId("other")
    private val good = UnitOAuthoritativeManifestFacts(selected, "a".repeat(64), 100, "application/pdf")
    @Test fun `exact ID returns only its own valid metadata without enumeration`() = runTest {
        val reads = mutableListOf<EvidenceArtifactId>()
        val result = bridge { id -> reads += id; good }.preflight(selected)
        assertTrue(result.eligible); assertEquals(good, result.metadata); assertEquals(listOf(selected), reads)
        assertFalse(UnitOManifestMetadataReader::class.java.methods.any { it.name.contains("list", true) })
        assertFalse(UnitOAuthoritativeMetadataBridge::class.java.methods.any { it.returnType == ByteArray::class.java })
    }
    @Test fun `wrong ID fails closed before manifest access`() = runTest {
        var reads = 0
        assertEquals(UnitOMetadataEligibility.NOT_SELECTED, bridge { reads++; good }.preflight(other).eligibility)
        assertEquals(0, reads)
    }
    @Test fun `missing and identity mismatch fail closed`() = runTest {
        assertEquals(UnitOMetadataEligibility.MANIFEST_MISSING, bridge { null }.preflight(selected).eligibility)
        assertEquals(UnitOMetadataEligibility.IDENTITY_MISMATCH, bridge { good.copy(evidenceArtifactId = other) }.preflight(selected).eligibility)
    }
    @Test fun `non PDF zero length and invalid digest fail`() = runTest {
        assertEquals(UnitOMetadataEligibility.MEDIA_TYPE_NOT_PDF, bridge { good.copy(declaredMediaType = "image/png") }.preflight(selected).eligibility)
        assertEquals(UnitOMetadataEligibility.ZERO_LENGTH, bridge { good.copy(byteLength = 0) }.preflight(selected).eligibility)
        assertEquals(UnitOMetadataEligibility.INVALID_SHA256, bridge { good.copy(sha256 = "SECRET_CONTENT_SENTINEL") }.preflight(selected).eligibility)
    }
    @Test fun `Parker and provider bounds fail independently`() = runTest {
        assertEquals(UnitOMetadataEligibility.OVER_PARKER_LIMIT, bridge(50, 200) { good }.preflight(selected).eligibility)
        assertEquals(UnitOMetadataEligibility.OVER_PROVIDER_LIMIT, bridge(200, 50) { good }.preflight(selected).eligibility)
    }
    @Test fun `bridge dependencies cannot invoke custody writes bytes provider OCR or analysis`() {
        val dependencies = UnitOAuthoritativeMetadataBridge::class.java.declaredFields.map { it.type.name }
        listOf("EvidenceSourceManifestStorage", "EvidenceArtifactStorage", "EvidenceCustodian", "ExternalTranscription",
            "Ocr", "Analysis", "Derivative").forEach { forbidden -> assertTrue(dependencies.none { it.contains(forbidden) }) }
    }
    @Test fun `bounded diagnostic contains no secret or content sentinel`() = runTest {
        val diagnostic = bridge { good.copy(sha256 = "SECRET_CONTENT_SENTINEL") }.preflight(selected).safeDiagnostic()
        assertFalse(diagnostic.contains("SECRET_CONTENT_SENTINEL"))
        assertEquals("EVIDENCE_ID=selected ELIGIBILITY=INVALID_SHA256", diagnostic)
    }
    private fun bridge(parker: Long = 1024, provider: Long = 1024,
        read: suspend (EvidenceArtifactId) -> UnitOAuthoritativeManifestFacts?,
    ) = UnitOAuthoritativeMetadataBridge(setOf(selected), UnitOManifestMetadataReader(read), parker, provider)
}
