package parker.core.interfaces

/**
 * Facts common to any prepared representation submitted to a governed external mechanism,
 * independent of whether the source/representation is paginated (PDF, image) or a non-paginated
 * structured document (for example EML). Deliberately excludes page numbers, DPI, pixel geometry,
 * rotation, crop, and any other page/image-specific field -- those remain on the representation's
 * own concrete provenance type (see FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md §6.1).
 */
interface SubmittedRepresentationSummary {
    val sourceEvidenceArtifactId: EvidenceArtifactId
    val sourceMediaType: String
    val sourceSha256: OcrSha256Digest
    val sourceByteLength: Long
    val representationMediaType: String
    val representationSha256: OcrSha256Digest
    val representationByteLength: Long
    val representationClass: AcquisitionRepresentationClass
    val byteExactCopy: Boolean
    val transformationProfileIdentity: String
}

/** [SubmittedRepresentationSummary] plus the actual bytes to submit to the external mechanism. */
interface ExternallySubmittedRepresentation : SubmittedRepresentationSummary {
    fun content(): ByteArray
}
