package parker.core.runtime

import parker.core.interfaces.*

/** Truthful conservative projections of Parker's existing native and local mechanisms. */
object ProductionAcquisitionCapabilityCatalogue {
    const val NATIVE_CAPABILITY_ID = "parker-tier-a-native-v1"
    const val LOCAL_OCR_CAPABILITY_ID = "parker-docling-local-ocr-v1"
    const val FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID = "openai-gpt-5.6-sol-fidelity-first-v1"
    const val EML_DERIVED_TEXT_EXTERNAL_CAPABILITY_ID = "openai-gpt-5.6-sol-eml-derived-text-v1"
    const val ORDINARY_REGION_V5_CAPABILITY_ID = ORDINARY_REGION_CAPABILITY_ID

    fun create(externalCapabilityProjection: EvidenceAcquisitionCapability? = fidelityFirstExternalCapability(),
        ordinaryRegionCapabilityProjection: EvidenceAcquisitionCapability? = null,
        // REAL-DOCUMENT-2E: defaults to Available so every existing direct caller of this catalogue
        // (offline router/eligibility tests, synthetic acceptance, historical coverage) is
        // unaffected. The one real production composition root (ParkerRuntime) passes the owner's
        // governed production eligibility decision explicitly instead of relying on this default.
        localOcrAvailability: AcquisitionAvailability = AcquisitionAvailability.Available,
        // STEP 4G: a distinct capability record, never a widening of externalCapabilityProjection
        // -- EML uses its own representation class, representation media type, processing profile,
        // response schema, and acceptance evidence. Defaults to null (not registered), matching
        // ordinaryRegionCapabilityProjection's own default, so every existing direct caller of
        // this catalogue (offline router/eligibility tests, historical coverage, including tests
        // that assert an exact external-capability count) is unaffected; the one real production
        // composition root (ParkerRuntime) passes it explicitly.
        emlExternalCapabilityProjection: EvidenceAcquisitionCapability? = null,
    ): GovernedAcquisitionCapabilityRegistry {
        listOfNotNull(externalCapabilityProjection, ordinaryRegionCapabilityProjection, emlExternalCapabilityProjection).forEach {
            require(it.egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED)
            require(it.providerConfiguration != null)
        }
        return GovernedAcquisitionCapabilityRegistry(
            listOf(nativeCapability(), localOcrCapability(localOcrAvailability)) +
                listOfNotNull(externalCapabilityProjection, ordinaryRegionCapabilityProjection, emlExternalCapabilityProjection),
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
            // Not production-selected per the OpenAI-first production selection correction
            // (FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md §7.2). The extractor remains
            // implemented and tested; only production eligibility is withdrawn here.
            "text/csv" to AcquisitionFidelitySuitability.NOT_ACCEPTED,
            "message/rfc822" to AcquisitionFidelitySuitability.NOT_ACCEPTED,
            "application/pdf" to AcquisitionFidelitySuitability.NOT_ACCEPTED,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to AcquisitionFidelitySuitability.NOT_ACCEPTED,
        ),
    )

    fun localOcrCapability(availability: AcquisitionAvailability = AcquisitionAvailability.Available) = EvidenceAcquisitionCapability(
        LOCAL_OCR_CAPABILITY_ID, EvidenceAcquisitionMechanism.LOCAL_OCR,
        setOf("application/pdf", "image/jpeg", "image/png", "image/webp"),
        setOf(AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED, AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE),
        AcquisitionFidelityCapabilities(true, false, true, false, false, false,
            pageAssociation = true, regionAssociation = false, uncertaintyReporting = true, structuredOutput = false),
        setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
        AcquisitionEgress.LOCAL_ONLY, null, availability, AcquisitionOperationalLimits(),
    )

    fun fidelityFirstExternalCapability() = EvidenceAcquisitionCapability(
        FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID, EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION,
        // text/csv per the OpenAI-first production selection correction (FIDELITY_PRESERVING_
        // EVIDENCE_ACQUISITION_SCOPE_LOCK.md §7.2, Step 3): narrow, proven transport only --
        // message/rfc822, DOCX, and text/plain are deliberately not added here.
        setOf("application/pdf", "image/jpeg", "image/png", "image/webp", "text/csv"),
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

    /**
     * STEP 4G -- the distinct EML external capability (FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md
     * §6.1). Never a widening of [fidelityFirstExternalCapability]: separate source media type,
     * representation class (`DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION`, never byte-exact),
     * representation media type, processing profile, instruction/schema digests, and acceptance --
     * reusing only the same provider/model/adapter implementation. No page limit is set: EML has
     * no page concept, so none is fabricated (unlike CSV/PDF/image, which reuse `Known(1)`/real
     * page counts).
     */
    fun emlDerivedTextExternalCapability() = EvidenceAcquisitionCapability(
        EML_DERIVED_TEXT_EXTERNAL_CAPABILITY_ID, EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION,
        setOf("message/rfc822"),
        setOf(AcquisitionSourceForm.NATIVE_SEARCHABLE),
        AcquisitionFidelityCapabilities(true, false, false, false, false, false,
            pageAssociation = false, regionAssociation = false, uncertaintyReporting = true, structuredOutput = true),
        setOf(AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION),
        AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
        AcquisitionProviderConfiguration(
            "OpenAI", "gpt-5.6-sol", EML_TRANSCRIPTION_PROFILE_ID,
            EML_TRANSCRIPTION_PROFILE_ID, EML_VERIFICATION_INSTRUCTION_SHA256,
            EML_VERIFICATION_SCHEMA_SHA256, "openai-responses-adapter", "2.0.0",
            EML_PROCESSING_PROFILE_IDENTITY, "none", false, "NOT_APPLICABLE", "NOT_APPLICABLE",
        ),
        AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED),
        AcquisitionOperationalLimits(ExternalTranscriptionRequest.MAX_SOURCE_BYTES, null),
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

    fun ordinaryRequestRegionV8Capability(accepted:Boolean):EvidenceAcquisitionCapability {
        val c=OrdinaryRequestRegionV8CapabilityIdentity()
        return EvidenceAcquisitionCapability(c.capabilityId,EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION,setOf("application/pdf"),
            setOf(AcquisitionSourceForm.NATIVE_SEARCHABLE,AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED,AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE),
            AcquisitionFidelityCapabilities(true,false,true,true,true,true,pageAssociation=true,regionAssociation=true,uncertaintyReporting=true,structuredOutput=true),
            setOf(AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION),AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
            AcquisitionProviderConfiguration(c.provider,c.model,c.profile,c.capabilityDigest,c.instructionSha256,c.schemaSha256,c.adapterId,c.adapterVersion,c.processing,c.reasoning,c.store,"NOT_APPLICABLE","original"),
            if(accepted)AcquisitionAvailability.Available else AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED),
            AcquisitionOperationalLimits(64L*1024L*1024L,200))
    }
}
