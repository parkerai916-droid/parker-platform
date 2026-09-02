package parker.core.runtime

import java.security.MessageDigest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceCustodian
import parker.core.interfaces.PrincipalId

data class GovernedPreparationPageResult(
    val pageNumber: Int,
    val authoritativeRepresentationId: String,
    val authoritativePixelDigest: String,
    val preparationId: String,
    val preparationRegionId: String,
    val transportSha256: String,
    val transportByteLength: Int,
    val width: Int,
    val height: Int,
    val orderState: String,
)

data class GovernedCorrectedPreparationResult(
    val evidenceId: String,
    val profileId: String,
    val profileVersion: Int,
    val preparationIdentity: String,
    val regionSetDigest: String,
    val pages: List<GovernedPreparationPageResult>,
    val requestRegionCount: Int,
    val requestDigest: String,
    val requestBodyDigest: String,
    val requestBodyByteLength: Int,
    val aggregatePngByteLength: Int,
    val aggregateBase64Characters: Int,
    val readbackVerified: Boolean,
)

sealed interface GovernedCorrectedPreparationOutcome {
    data class Prepared(val result: GovernedCorrectedPreparationResult) : GovernedCorrectedPreparationOutcome
    data class Rejected(val reason: String) : GovernedCorrectedPreparationOutcome
}

/** Preparation-only authority: custody read -> deterministic preparation -> create-once readback. */
class GovernedCorrectedPreparationService(
    evidenceCustodian: EvidenceCustodian,
    private val owner: PrincipalId,
    private val store: FileSystemFullPageAchromaticPreparationStore,
    private val builder: FullPageAchromaticCanonicalRequestRegionV8Builder =
        FullPageAchromaticCanonicalRequestRegionV8Builder(preparationPersistence = store),
) {
    private val resolver = AuthoritativeAcquisitionSourceResolver(evidenceCustodian)

    suspend fun prepare(
        evidenceId: EvidenceArtifactId,
        profileId: String = FULL_PAGE_ACHROMATIC_PROFILE_ID,
        profileVersion: Int = FULL_PAGE_ACHROMATIC_PROFILE_VERSION,
    ): GovernedCorrectedPreparationOutcome {
        if (profileId != FULL_PAGE_ACHROMATIC_PROFILE_ID || profileVersion != FULL_PAGE_ACHROMATIC_PROFILE_VERSION) {
            return GovernedCorrectedPreparationOutcome.Rejected("UNSUPPORTED_PREPARATION_PROFILE")
        }
        val source = when (val resolved = resolver.resolve(owner, evidenceId)) {
            is AuthoritativeAcquisitionResolution.Verified -> resolved.input
            else -> return GovernedCorrectedPreparationOutcome.Rejected("GOVERNED_EVIDENCE_UNAVAILABLE:${resolved.javaClass.simpleName}")
        }
        if (source.mediaType != "application/pdf") return GovernedCorrectedPreparationOutcome.Rejected("UNSUPPORTED_MEDIA")
        return try {
            val correlationInput = listOf("parker.corrected-preparation.operation.v1", evidenceId.value, source.sha256, profileId, profileVersion.toString()).joinToString("\u0000")
            val correlationId = "preparation-" + MessageDigest.getInstance("SHA-256").digest(correlationInput.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 255) }
            val construction = builder.build(evidenceId, source.sha256, source.mediaType!!, source.bytes(), correlationId)
            val prepared = requireNotNull(construction.achromaticPreparation)
            val readback = store.read(prepared.preparationIdentity)
            require(FullPageAchromaticPreparationCodec.encode(readback) == FullPageAchromaticPreparationCodec.encode(prepared)) {
                "corrected preparation readback mismatch"
            }
            GovernedCorrectedPreparationOutcome.Prepared(
                GovernedCorrectedPreparationResult(
                    evidenceId.value, profileId, profileVersion, prepared.preparationIdentity, prepared.regionSetDigest,
                    prepared.regions.map { region ->
                        GovernedPreparationPageResult(
                            region.provenance.pageNumber, region.provenance.authoritativePageRepresentationId.value,
                            region.provenance.authoritativePixelDigest.value, region.preparationId, region.sourceRegion.id.value,
                            region.provenance.transportSha256, region.provenance.encodedByteLength,
                            region.provenance.authoritativeDimensions.width, region.provenance.authoritativeDimensions.height,
                            region.orderState.disposition,
                        )
                    },
                    construction.request.regions.size, construction.requestBindingSha256, construction.providerBodySha256,
                    construction.providerBody.toByteArray(Charsets.UTF_8).size, construction.aggregateImageBytes,
                    construction.aggregateBase64Characters, true,
                ),
            )
        } catch (e: Exception) {
            GovernedCorrectedPreparationOutcome.Rejected(e.message ?: "CORRECTED_PREPARATION_FAILED")
        }
    }
}
