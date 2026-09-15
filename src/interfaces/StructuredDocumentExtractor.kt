package parker.core.interfaces

sealed class StructuredDocumentExtractionOutcome {
    data class Extracted(val result: StructuredDocumentRepresentation) : StructuredDocumentExtractionOutcome()
    data class CapabilityUnavailable(val reason: String) : StructuredDocumentExtractionOutcome()
    data class Malformed(val reason: String) : StructuredDocumentExtractionOutcome()
}

fun interface StructuredDocumentExtractor {
    suspend fun extract(sourceBytes: ByteArray, sourceSha256: String, mediaType: String, fileName: String?): StructuredDocumentExtractionOutcome
}
