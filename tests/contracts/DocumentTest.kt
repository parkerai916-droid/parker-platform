package parker.core.interfaces

import java.time.Instant
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Programme 2, Memory Core, Implementation Unit 4. Construction-time and
 * structural validation tests for [Document]
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` Section 5,
 * `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`). Pure data-shape
 * validation only, mirroring `EntityTest.kt`'s own established scope --
 * no `Assertion`, `Relationship`, `MemoryCore`, or `MemoryRetrieval`
 * exists yet; this file tests [Document] alone, composed only with the
 * already-implemented [Provenance] it references.
 */
class DocumentTest {

    private val registeredAt = Instant.parse("2026-01-01T00:00:00Z")

    private fun minimalDocument(
        documentId: DocumentId = DocumentId("document-1"),
        documentType: String = "email",
        locationReference: String = "mailbox://inbox/message-1",
        provenanceId: ProvenanceId = ProvenanceId("provenance-1"),
        registeredAt: Instant = this.registeredAt,
    ) = Document(
        documentId = documentId,
        documentType = documentType,
        locationReference = locationReference,
        provenanceId = provenanceId,
        registeredAt = registeredAt,
    )

    // --- Successful construction ---

    @Test
    fun `a Document can be constructed with only mandatory fields`() {
        val document = minimalDocument()

        assertEquals(DocumentId("document-1"), document.documentId)
        assertEquals("email", document.documentType)
        assertEquals("mailbox://inbox/message-1", document.locationReference)
        assertEquals(ProvenanceId("provenance-1"), document.provenanceId)
        assertEquals(registeredAt, document.registeredAt)
    }

    @Test
    fun `a Document can be constructed with every optional field supplied`() {
        val document = Document(
            documentId = DocumentId("document-2"),
            documentType = "pdf",
            locationReference = "file:///documents/contract.pdf",
            provenanceId = ProvenanceId("provenance-2"),
            registeredAt = registeredAt,
            integrityHash = "sha256:def456",
            processingStatus = DocumentProcessingStatus.PROCESSED_EXTERNALLY,
            status = MemoryCoreRecordStatus.ARCHIVED,
            metadata = mapOf("pages" to "12"),
        )

        assertEquals("sha256:def456", document.integrityHash)
        assertEquals(DocumentProcessingStatus.PROCESSED_EXTERNALLY, document.processingStatus)
        assertEquals(MemoryCoreRecordStatus.ARCHIVED, document.status)
        assertEquals(mapOf("pages" to "12"), document.metadata)
    }

    // --- Required-field validation ---

    @Test
    fun `a blank documentType is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalDocument(documentType = "") }
        assertFailsWith<IllegalArgumentException> { minimalDocument(documentType = "   ") }
    }

    @Test
    fun `a blank locationReference is rejected`() {
        assertFailsWith<IllegalArgumentException> { minimalDocument(locationReference = "") }
        assertFailsWith<IllegalArgumentException> { minimalDocument(locationReference = "   ") }
    }

    @Test
    fun `a blank integrityHash is rejected when present`() {
        assertFailsWith<IllegalArgumentException> { minimalDocument().copy(integrityHash = "") }
    }

    // --- Registration only -- never fetches or validates the location ---

    @Test
    fun `an unreachable or nonsensical locationReference is accepted without any fetch attempt`() {
        val document = minimalDocument(locationReference = "not-a-real-scheme://this-does-not-exist/nowhere")

        assertEquals("not-a-real-scheme://this-does-not-exist/nowhere", document.locationReference)
    }

    // --- Unknown-value / default handling ---

    @Test
    fun `optional fields genuinely accept absence, and status-shaped fields default sensibly`() {
        val document = minimalDocument()

        assertNull(document.integrityHash)
        assertEquals(emptyMap(), document.metadata)
        assertEquals(DocumentProcessingStatus.REGISTERED, document.processingStatus)
        assertEquals(MemoryCoreRecordStatus.ACTIVE, document.status)
    }

    @Test
    fun `a Document can be constructed in every DocumentProcessingStatus value`() {
        DocumentProcessingStatus.entries.forEach { processingStatus ->
            val document = minimalDocument().copy(processingStatus = processingStatus)
            assertEquals(processingStatus, document.processingStatus)
        }
    }

    @Test
    fun `a Document can be constructed directly in every MemoryCoreRecordStatus, not only ACTIVE`() {
        MemoryCoreRecordStatus.entries.forEach { status ->
            val document = minimalDocument().copy(status = status)
            assertEquals(status, document.status)
        }
    }

    @Test
    fun `processingStatus and status vary independently of each other`() {
        val document = minimalDocument().copy(
            processingStatus = DocumentProcessingStatus.PROCESSING_FAILED,
            status = MemoryCoreRecordStatus.ACTIVE,
        )

        assertEquals(DocumentProcessingStatus.PROCESSING_FAILED, document.processingStatus)
        assertEquals(MemoryCoreRecordStatus.ACTIVE, document.status)
    }

    // --- No embedded relationship field ---

    @Test
    fun `Document exposes exactly the nine frozen fields, and no embedded relationship field`() {
        val expectedFields = setOf(
            "documentId",
            "documentType",
            "locationReference",
            "provenanceId",
            "registeredAt",
            "integrityHash",
            "processingStatus",
            "status",
            "metadata",
        )

        assertEquals(expectedFields, Document::class.memberProperties.map { it.name }.toSet())
    }

    // --- Equality ---

    @Test
    fun `two Document records with identical fields are equal`() {
        assertEquals(minimalDocument(), minimalDocument())
    }

    @Test
    fun `two Document records differing in one field are not equal`() {
        assertNotEquals(minimalDocument(), minimalDocument(documentType = "pdf"))
        assertNotEquals(minimalDocument(), minimalDocument().copy(processingStatus = DocumentProcessingStatus.PROCESSING_REQUESTED))
    }

    // --- Immutability expectations ---

    @Test
    fun `Document exposes no mutable (var) property`() {
        val mutableProperties = Document::class.memberProperties.filterIsInstance<KMutableProperty1<*, *>>()
        assertTrue(
            mutableProperties.isEmpty(),
            "Document must expose only immutable (val) properties, found: ${mutableProperties.map { it.name }}",
        )
    }

    @Test
    fun `copy() produces a distinct instance without mutating the original`() {
        val original = minimalDocument()
        val copy = original.copy(documentType = "pdf")

        assertEquals("email", original.documentType)
        assertEquals("pdf", copy.documentType)
        assertNotEquals(original, copy)
    }
}
