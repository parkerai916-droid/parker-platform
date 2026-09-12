package parker.core.interfaces

/** Precision of a source pointer actually established by governed provenance. */
enum class AnalysisReferencePrecision { DOCUMENT, PAGE, REGION }

enum class AnalysisReferenceAuthority { MACHINE_DERIVED, OWNER_AUTHORIZED_CORRECTION }

data class AnalysisEvidenceReference(
    val evidenceArtifactId: EvidenceArtifactId,
    val derivativeGenerationId: DerivativeGenerationId,
    val sourceSha256: String,
    val originalFilename: String?,
    val precision: AnalysisReferencePrecision,
    val pageNumber: Int? = null,
    val regionId: String? = null,
    val authority: AnalysisReferenceAuthority = AnalysisReferenceAuthority.MACHINE_DERIVED,
    val correctionId: HermesPreIngestionCorrectionId? = null,
    val correctionScope: String? = null,
) {
    init {
        require(sourceSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(precision == AnalysisReferencePrecision.DOCUMENT || pageNumber != null)
        require(precision != AnalysisReferencePrecision.REGION || !regionId.isNullOrBlank())
        require(precision == AnalysisReferencePrecision.DOCUMENT || pageNumber!! >= 1)
        require((authority == AnalysisReferenceAuthority.OWNER_AUTHORIZED_CORRECTION) == (correctionId != null))
        require(correctionId == null || correctionScope == "ISSUE")
    }
}

data class StructuredAnalysisFinding(
    val text: String,
    val supportReferences: List<AnalysisEvidenceReference>,
)

data class StructuredAnalysisContraryEvidence(
    val text: String,
    val references: List<AnalysisEvidenceReference>,
)

data class StructuredAnalysisUncertainty(
    val text: String,
    val references: List<AnalysisEvidenceReference>,
)

data class StructuredAnalysisEvidenceGap(
    val text: String,
    val relatedEvidenceArtifactIds: List<EvidenceArtifactId> = emptyList(),
)

data class StructuredAnalysisResult(
    val answer: String,
    val findings: List<StructuredAnalysisFinding>,
    val contraryEvidence: List<StructuredAnalysisContraryEvidence>,
    val uncertainties: List<StructuredAnalysisUncertainty>,
    val evidenceGaps: List<StructuredAnalysisEvidenceGap>,
    val conclusion: String,
) {
    init {
        require(answer.isNotBlank() && answer.length <= 16_000)
        require(conclusion.isNotBlank() && conclusion.length <= 16_000)
        require(findings.size <= 100 && contraryEvidence.size <= 100 && uncertainties.size <= 100 && evidenceGaps.size <= 100)
        require(findings.all { it.text.isNotBlank() && it.text.length <= 8_000 && it.supportReferences.size <= 100 })
        require(contraryEvidence.all { it.text.isNotBlank() && it.text.length <= 8_000 && it.references.size <= 100 })
        require(uncertainties.all { it.text.isNotBlank() && it.text.length <= 8_000 && it.references.size <= 100 })
        require(evidenceGaps.all { it.text.isNotBlank() && it.text.length <= 8_000 && it.relatedEvidenceArtifactIds.size <= 100 })
    }
}
