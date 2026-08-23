package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Document Ingestion, Authoritative Source Manifest Foundation
 * Implementation. Validation and shape tests for
 * [EvidenceSourceManifest]/[EvidenceManifestRetrievalResult], governed by
 * `docs/architecture/DOCUMENT_INGESTION_AUTHORITATIVE_SOURCE_MANIFEST_RETRIEVAL_SCOPE_LOCK.md`
 * ("the Scope Lock") Section 6.
 */
class EvidenceSourceManifestTest {

    private val id = EvidenceArtifactId("evidence-1")
    private val validSha256 = "a".repeat(64)

    @Test
    fun `a valid manifest with only required fields constructs successfully`() {
        val manifest = EvidenceSourceManifest(id, validSha256, 10L)

        assertEquals(id, manifest.evidenceArtifactId)
        assertEquals(validSha256, manifest.sha256)
        assertEquals(10L, manifest.byteLength)
        assertNull(manifest.receivedMediaType)
        assertNull(manifest.originalFileName)
    }

    @Test
    fun `sha256 must be exactly 64 lowercase hexadecimal characters`() {
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, "A".repeat(64), 1L) }
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, "a".repeat(63), 1L) }
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, "a".repeat(65), 1L) }
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, "z".repeat(64), 1L) }
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, "", 1L) }
    }

    @Test
    fun `byteLength must not be negative`() {
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, validSha256, -1L) }
    }

    @Test
    fun `byteLength of zero is permitted -- a zero-byte artefact is legitimate`() {
        val manifest = EvidenceSourceManifest(id, validSha256, 0L)
        assertEquals(0L, manifest.byteLength)
    }

    @Test
    fun `receivedMediaType and originalFileName must not be blank if present`() {
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, validSha256, 1L, receivedMediaType = "") }
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, validSha256, 1L, receivedMediaType = "   ") }
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, validSha256, 1L, originalFileName = "") }
        assertFailsWith<IllegalArgumentException> { EvidenceSourceManifest(id, validSha256, 1L, originalFileName = "   ") }
    }

    @Test
    fun `receivedMediaType and originalFileName preserve exact declared literal values, including Unicode`() {
        val manifest = EvidenceSourceManifest(
            evidenceArtifactId = id,
            sha256 = validSha256,
            byteLength = 1L,
            receivedMediaType = "text/csv; charset=utf-8",
            originalFileName = "légal-évidence-🔎.csv",
        )

        assertEquals("text/csv; charset=utf-8", manifest.receivedMediaType)
        assertEquals("légal-évidence-🔎.csv", manifest.originalFileName)
    }

    @Test
    fun `hostile filename metadata remains inert -- a path-traversal-shaped filename is stored literally, not interpreted`() {
        val manifest = EvidenceSourceManifest(
            evidenceArtifactId = id,
            sha256 = validSha256,
            byteLength = 1L,
            originalFileName = "../../etc/passwd",
        )

        // This type has no path-construction behaviour of any kind -- the value is opaque
        // metadata, never used to resolve a filesystem location (Scope Lock Section 11).
        assertEquals("../../etc/passwd", manifest.originalFileName)
    }

    @Test
    fun `EvidenceManifestRetrievalResult Rejected requires a non-blank reason`() {
        assertFailsWith<IllegalArgumentException> { EvidenceManifestRetrievalResult.Rejected(id, "") }
    }

    @Test
    fun `EvidenceCustodian declares no listing, search, or mutation operation for the manifest`() {
        val declaredNames = EvidenceCustodian::class.java.declaredMethods.map { it.name.substringBefore('-') }
        listOf("listManifests", "searchManifests", "updateManifest", "writeManifest", "deleteManifest").forEach { excluded ->
            assertFalse(
                excluded in declaredNames,
                "EvidenceCustodian must not declare '$excluded' -- manifest retrieval is read-only (Scope Lock Section 14)",
            )
        }
    }
}
