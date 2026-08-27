package parker.core.runtime

import parker.core.interfaces.*

/** Truthful conservative projections of Parker's existing native and local mechanisms. */
object ProductionAcquisitionCapabilityCatalogue {
    const val NATIVE_CAPABILITY_ID = "parker-tier-a-native-v1"
    const val LOCAL_OCR_CAPABILITY_ID = "parker-docling-local-ocr-v1"

    fun create(externalCapabilityProjection: EvidenceAcquisitionCapability? = null): GovernedAcquisitionCapabilityRegistry {
        if (externalCapabilityProjection != null) {
            require(externalCapabilityProjection.egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED)
            require(externalCapabilityProjection.providerConfiguration != null)
        }
        return GovernedAcquisitionCapabilityRegistry(
            listOf(nativeCapability(), localOcrCapability()) + listOfNotNull(externalCapabilityProjection),
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
}
