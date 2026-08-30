package parker.core.runtime

import parker.core.interfaces.*

/** Truthful conservative projections of Parker's existing native and local mechanisms. */
object ProductionAcquisitionCapabilityCatalogue {
    const val NATIVE_CAPABILITY_ID = "parker-tier-a-native-v1"
    const val LOCAL_OCR_CAPABILITY_ID = "parker-docling-local-ocr-v1"
    const val FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID = "openai-gpt-5.6-sol-fidelity-first-v1"
    const val ORDINARY_REGION_V5_CAPABILITY_ID = ORDINARY_REGION_CAPABILITY_ID

    fun create(externalCapabilityProjection: EvidenceAcquisitionCapability? = fidelityFirstExternalCapability(),
        ordinaryRegionCapabilityProjection: EvidenceAcquisitionCapability? = null): GovernedAcquisitionCapabilityRegistry {
        listOfNotNull(externalCapabilityProjection, ordinaryRegionCapabilityProjection).forEach {
            require(it.egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED)
            require(it.providerConfiguration != null)
        }
        return GovernedAcquisitionCapabilityRegistry(
            listOf(nativeCapability(), localOcrCapability()) + listOfNotNull(externalCapabilityProjection, ordinaryRegionCapabilityProjection),
        )
    }

    fun nativeCapability() = EvidenceAcquisitionCapability(
        NATIVE_CAPABILITY_ID, EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION,
        setOf("text/csv", "message/rfc822", "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
        setOf(AcquisitionSourceForm.NATIVE_SEARCHABLE),
        AcquisitionFidelityCapabilities(false, true, false, false, false, false,
            pageAssociation = true, regionAssociation = false, uncertaintyReporting = false, structuredOutput = true),
        setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
        AcquisitionEgress.LOCAL_ONLY, null, AcquisitionAvailability.Available, AcquisitionOperationalLimits(),
        mapOf(
            "text/csv" to AcquisitionFidelitySuitability.ACCEPTED,
            "message/rfc822" to AcquisitionFidelitySuitability.ACCEPTED,
            "application/pdf" to AcquisitionFidelitySuitability.NOT_ACCEPTED,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to AcquisitionFidelitySuitability.ACCEPTED,
        ),
    )

    fun localOcrCapability() = EvidenceAcquisitionCapability(
        LOCAL_OCR_CAPABILITY_ID, EvidenceAcquisitionMechanism.LOCAL_OCR,
        setOf("application/pdf", "image/jpeg", "image/png", "image/webp"),
        setOf(AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED, AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE),
        AcquisitionFidelityCapabilities(true, false, true, false, false, false,
            pageAssociation = true, regionAssociation = false, uncertaintyReporting = true, structuredOutput = false),
        setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
        AcquisitionEgress.LOCAL_ONLY, null, AcquisitionAvailability.Available, AcquisitionOperationalLimits(),
    )

    fun fidelityFirstExternalCapability() = EvidenceAcquisitionCapability(
        FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID, EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION,
        setOf("application/pdf", "image/jpeg", "image/png", "image/webp"),
        setOf(AcquisitionSourceForm.NATIVE_SEARCHABLE, AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED,
            AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE),
        AcquisitionFidelityCapabilities(true, false, true, true, true, true,
            pageAssociation = true, regionAssociation = true, uncertaintyReporting = true, structuredOutput = true),
        setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
        AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
        AcquisitionProviderConfiguration(
            "OpenAI", "gpt-5.6-sol", "openai-fidelity-first-transcription-v1",
            "openai-fidelity-first-transcription-v1", FIDELITY_FIRST_INSTRUCTION_SHA256,
            FIDELITY_FIRST_SCHEMA_SHA256, "openai-responses-adapter", "2.0.0",
            "external-transcription.direct-authoritative-byte-v1", "none", false, "high", "original",
        ),
        AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED),
        AcquisitionOperationalLimits(ExternalTranscriptionRequest.MAX_SOURCE_BYTES, ExternalTranscriptionRequest.MAX_PAGE_COUNT),
    )

    /** Dynamic acceptance evaluation chooses [accepted] on every projection; no lifecycle snapshot is retained here. */
    fun ordinaryRegionV5Capability(accepted: Boolean) = EvidenceAcquisitionCapability(
        ORDINARY_REGION_V5_CAPABILITY_ID, EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION,
        setOf("application/pdf"), setOf(AcquisitionSourceForm.NATIVE_SEARCHABLE,
            AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED, AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE),
        AcquisitionFidelityCapabilities(true, false, true, true, true, true,
            pageAssociation = true, regionAssociation = true, uncertaintyReporting = true, structuredOutput = true),
        setOf(AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION),
        AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
        AcquisitionProviderConfiguration("OpenAI", OPENAI_REGION_MODEL, OPENAI_REGION_PROFILE_ID,
            OrdinaryRegionCapabilityIdentity().digest(), OPENAI_REGION_INSTRUCTION_SHA256,
            REGION_TRANSCRIPTION_SCHEMA_SHA256, OPENAI_REGION_ADAPTER_ID, OPENAI_REGION_ADAPTER_VERSION,
            REGION_TRANSCRIPTION_PROCESSING_PROFILE, "none", false, "NOT_APPLICABLE", "original"),
        if (accepted) AcquisitionAvailability.Available else
            AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED),
        AcquisitionOperationalLimits(64L * 1024L * 1024L, 200),
    )
}
