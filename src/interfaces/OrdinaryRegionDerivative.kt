package parker.core.interfaces

/** Durable, versioned ordinary region-v5 transcription content. Each binding is canonical text
 * produced by Parker, never provider-selected ordering or native PDF text. */
data class OrdinaryRegionTranscriptionDerivative(
    val representationVersion: Int = 1,
    val evidenceArtifactId: String,
    val sourceSha256: String,
    val pageBindings: List<String>,
    val regionBindings: List<String>,
    val transcriptionBlocks: List<String>,
    val providerReturnedOrder: List<String>,
    val parkerSourceOrder: List<String>,
    val provider: String,
    val model: String,
    val adapterId: String,
    val adapterVersion: String,
    val providerProfile: String,
    val wireVersion: Int,
    val schemaSha256: String,
    val instructionSha256: String,
    val processingProfile: String,
    val requestIdentity: String,
    val requestDigest: String,
    val responseIdentity: String,
    val providerStateRecordIdentity: String,
    val capabilityAcceptanceRecordIdentity: String,
    val ownerAuthorizationIdentity: String,
    val executionIdentity: String,
    val attemptIdentity: String,
    val reconstructedContentDigest: String,
    val canonicalGenerationKeyDigest: String,
    val admissionProvenance: String,
) {
    init {
        require(representationVersion == 1)
        require(listOf(sourceSha256, schemaSha256, instructionSha256, requestDigest,
            reconstructedContentDigest, canonicalGenerationKeyDigest).all { SHA256.matches(it) })
        require(pageBindings.isNotEmpty() && regionBindings.isNotEmpty() && transcriptionBlocks.isNotEmpty())
        require(regionBindings.size == parkerSourceOrder.size && providerReturnedOrder.size == parkerSourceOrder.size)
        require(listOf(evidenceArtifactId, provider, model, adapterId, adapterVersion, providerProfile,
            processingProfile, requestIdentity, responseIdentity, providerStateRecordIdentity,
            capabilityAcceptanceRecordIdentity, ownerAuthorizationIdentity, executionIdentity,
            attemptIdentity, admissionProvenance).all { it.isNotBlank() && it.length <= 4096 })
    }

    private companion object { val SHA256 = Regex("^[0-9a-f]{64}$") }
}
