package parker.core.interfaces

/** Source-bound OCR representation submitted by Hermes after pre-ingestion OCR. */
data class HermesOcrRepresentation(
    val evidenceArtifactId: EvidenceArtifactId,
    val sourceSha256: String,
    val originalFilename: String,
    val originalMediaType: String,
    val processingMethod: HermesProcessingMethod,
    val status: HermesProcessingStatus,
    val recognisedText: String,
    val derivativeContentSha256: String,
    val confidence: Double?,
    val completeness: HermesProcessingCompleteness,
    val warnings: List<String>,
    val issues: List<HermesProcessingIssue>,
    val mechanismVersion: String,
    val modelIdentity: String,
    val modelVersion: String,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$"))) { "sourceSha256 must be a lowercase SHA-256 digest" }
        require(originalFilename.isNotBlank())
        require(originalMediaType == "image/jpeg" || originalMediaType == "image/png" || originalMediaType == "image/webp") {
            "only image OCR representations are accepted"
        }
        require(processingMethod == HermesProcessingMethod.OCR) { "Hermes OCR representation must use OCR processing" }
        require(recognisedText.isNotBlank()) { "recognisedText must not be blank" }
        require(derivativeContentSha256.matches(Regex("^[0-9a-f]{64}$"))) { "derivativeContentSha256 must be a lowercase SHA-256 digest" }
        confidence?.let { require(it in 0.0..1.0) { "confidence must fall within 0..1" } }
        require(mechanismVersion.isNotBlank() && modelIdentity.isNotBlank() && modelVersion.isNotBlank()) {
            "OCR mechanism and model provenance are mandatory"
        }
        require(status == HermesProcessingStatus.PASS || status == HermesProcessingStatus.REVIEW_REQUIRED) {
            "FAILED OCR results cannot submit a representation"
        }
        require(status != HermesProcessingStatus.PASS || completeness == HermesProcessingCompleteness.COMPLETE) {
            "PASS requires complete OCR"
        }
    }
}
