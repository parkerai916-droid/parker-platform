package parker.core.interfaces

/** Durable, explicitly capability-bound ordinary request-region transcription content. */
data class OrdinaryRegionTranscriptionDerivative(
    val representationVersion: Int = 1,
    val capabilityId: String = "ordinary-external-region-transcription-v5",
    val capabilityDigest: String = "historical-v5-capability-digest-not-persisted",
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
    val preparationIdentity: String? = null,
    val preparationProfile: String? = null,
    val preparationProfileVersion: Int? = null,
    val providerBodyDigest: String? = null,
    val authorizationPurpose: String? = null,
    val maximumProviderCalls: Int? = null,
    val automaticRetryLimit: Int? = null,
    val externalReasoningAuthorized: Boolean? = null,
) {
    init {
        require(representationVersion in 1..3)
        if (representationVersion >= 2) require(capabilityDigest.matches(SHA256))
        require(listOf(sourceSha256, schemaSha256, instructionSha256, requestDigest,
            reconstructedContentDigest, canonicalGenerationKeyDigest).all { SHA256.matches(it) })
        require(pageBindings.isNotEmpty() && regionBindings.isNotEmpty() && transcriptionBlocks.isNotEmpty())
        require(regionBindings.size == parkerSourceOrder.size && providerReturnedOrder.size == parkerSourceOrder.size)
        require(listOf(capabilityId, capabilityDigest, evidenceArtifactId, provider, model, adapterId, adapterVersion, providerProfile,
            processingProfile, requestIdentity, responseIdentity, providerStateRecordIdentity,
            capabilityAcceptanceRecordIdentity, ownerAuthorizationIdentity, executionIdentity,
            attemptIdentity, admissionProvenance).all { it.isNotBlank() && it.length <= 4096 })
        if (representationVersion == 3) {
            require(capabilityId == "ordinary-external-request-region-transcription-v8" && provider == "OpenAI" && model == "gpt-5.6-sol")
            require(providerProfile == "openai-fidelity-first-transcription-v1")
            require(preparationIdentity?.matches(SHA256) == true && providerBodyDigest?.matches(SHA256) == true)
            require(!preparationProfile.isNullOrBlank() && preparationProfileVersion == 1)
            require(authorizationPurpose == "evidence-intelligence.external-transcription")
            require(maximumProviderCalls == 1 && automaticRetryLimit == 0 && externalReasoningAuthorized == false)
        }
    }

    private companion object { val SHA256 = Regex("^[0-9a-f]{64}$") }
}
