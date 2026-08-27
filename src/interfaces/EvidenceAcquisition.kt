package parker.core.interfaces

/** Provider-neutral facts used to describe an evidence-acquisition mechanism. */
enum class EvidenceAcquisitionMechanism {
    DIRECT_NATIVE_EXTRACTION,
    LOCAL_OCR,
    EXTERNAL_TRANSCRIPTION,
    EXTERNAL_VISION_TRANSCRIPTION,
}

enum class AcquisitionRepresentationClass {
    AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY,
    DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION,
}

enum class AcquisitionEgress { LOCAL_ONLY, EXTERNAL_EGRESS_REQUIRED }

enum class AcquisitionCharacteristicState { PRESENT, ABSENT, UNKNOWN }

enum class AcquisitionSourceForm { NATIVE_SEARCHABLE, IMAGE_ONLY_OR_SCANNED, MIXED_TEXT_AND_IMAGE }

data class AcquisitionFidelityCapabilities(
    val literalTranscription: Boolean,
    val nativeTextExtraction: Boolean,
    val ocr: Boolean,
    val handwriting: Boolean,
    val layoutAware: Boolean,
    val tableAware: Boolean,
    val pageAssociation: Boolean,
    val regionAssociation: Boolean,
    val uncertaintyReporting: Boolean,
    val structuredOutput: Boolean,
)

data class AcquisitionSourceCharacteristics(
    val nativeSearchableText: AcquisitionCharacteristicState,
    val imageOnlyOrScanned: AcquisitionCharacteristicState,
    val mixedTextAndImage: AcquisitionCharacteristicState,
    val handwriting: AcquisitionCharacteristicState,
    val complexLayout: AcquisitionCharacteristicState,
    val tables: AcquisitionCharacteristicState,
)

sealed class AcquisitionPageCount {
    data class Known(val value: Int) : AcquisitionPageCount() {
        init { require(value > 0) { "Known page count must be positive" } }
    }
    data object Unknown : AcquisitionPageCount()
}

enum class HumanAuthorisedCustody { CONFIRMED, NOT_CONFIRMED }

data class AcquisitionSource(
    val evidenceArtifactId: EvidenceArtifactId,
    val sha256: String,
    val byteLength: Long,
    val mediaType: String,
    val pageCount: AcquisitionPageCount,
    val characteristics: AcquisitionSourceCharacteristics,
    val humanAuthorisedCustody: HumanAuthorisedCustody,
) {
    init {
        require(sha256.matches(Regex("^[0-9a-f]{64}$"))) { "Source SHA-256 must be lowercase hexadecimal" }
        require(byteLength >= 0) { "Source byte length must not be negative" }
        require(mediaType.isNotBlank()) { "Source media type must not be blank" }
    }
}

data class AcquisitionProviderConfiguration(
    val providerIdentity: String,
    val modelSelectionRule: String,
    val profileIdentity: String,
    val configurationIdentity: String,
    val instructionSha256: String,
    val schemaSha256: String,
    val adapterIdentity: String,
    val adapterVersion: String,
    val processingProfileIdentity: String,
) {
    init {
        listOf(providerIdentity, modelSelectionRule, profileIdentity, configurationIdentity,
            adapterIdentity, adapterVersion, processingProfileIdentity).forEach {
            require(it.isNotBlank()) { "Provider configuration identities must not be blank" }
        }
        require(instructionSha256.matches(Regex("^[0-9a-f]{64}$")))
        require(schemaSha256.matches(Regex("^[0-9a-f]{64}$")))
    }
}

enum class AcquisitionAvailabilityReason {
    DISABLED,
    CONFIGURATION_NOT_ACCEPTED,
    CONFIGURATION_NOT_READY,
}

sealed class AcquisitionAvailability {
    data object Available : AcquisitionAvailability()
    data class Unavailable(val reason: AcquisitionAvailabilityReason) : AcquisitionAvailability()
}

data class AcquisitionOperationalLimits(
    val maximumSourceBytes: Long? = null,
    val maximumPages: Int? = null,
) {
    init {
        require(maximumSourceBytes == null || maximumSourceBytes >= 0)
        require(maximumPages == null || maximumPages > 0)
    }
}

/**
 * Immutable capability facts only. This is neither a registry nor routing authority.
 * Availability projects an existing governed lifecycle/readiness determination; it does not
 * create or mutate provider lifecycle state.
 */
class EvidenceAcquisitionCapability(
    val capabilityId: String,
    val mechanism: EvidenceAcquisitionMechanism,
    supportedMediaTypes: Set<String>,
    supportedSourceForms: Set<AcquisitionSourceForm>,
    val fidelity: AcquisitionFidelityCapabilities,
    supportedRepresentations: Set<AcquisitionRepresentationClass>,
    val egress: AcquisitionEgress,
    val providerConfiguration: AcquisitionProviderConfiguration?,
    val availability: AcquisitionAvailability,
    val limits: AcquisitionOperationalLimits,
) {
    val supportedMediaTypes: Set<String> = supportedMediaTypes.toSet()
    val supportedSourceForms: Set<AcquisitionSourceForm> = supportedSourceForms.toSet()
    val supportedRepresentations: Set<AcquisitionRepresentationClass> = supportedRepresentations.toSet()

    init {
        require(capabilityId.isNotBlank())
        require(this.supportedMediaTypes.isNotEmpty() && this.supportedMediaTypes.none { it.isBlank() })
        require(this.supportedSourceForms.isNotEmpty())
        require(this.supportedRepresentations.isNotEmpty())
        when (mechanism) {
            EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION -> {
                require(fidelity.nativeTextExtraction)
                require(AcquisitionSourceForm.NATIVE_SEARCHABLE in this.supportedSourceForms)
                require(egress == AcquisitionEgress.LOCAL_ONLY && providerConfiguration == null)
            }
            EvidenceAcquisitionMechanism.LOCAL_OCR -> {
                require(fidelity.ocr)
                require(egress == AcquisitionEgress.LOCAL_ONLY && providerConfiguration == null)
            }
            EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION,
            EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION -> {
                require(egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED)
                require(providerConfiguration != null)
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is EvidenceAcquisitionCapability &&
        capabilityId == other.capabilityId && mechanism == other.mechanism &&
        supportedMediaTypes == other.supportedMediaTypes && supportedSourceForms == other.supportedSourceForms &&
        fidelity == other.fidelity && supportedRepresentations == other.supportedRepresentations &&
        egress == other.egress && providerConfiguration == other.providerConfiguration &&
        availability == other.availability && limits == other.limits

    override fun hashCode(): Int = listOf(capabilityId, mechanism, supportedMediaTypes, supportedSourceForms,
        fidelity, supportedRepresentations, egress, providerConfiguration, availability, limits).hashCode()
}

enum class ExternalEgressAuthorisation { AUTHORISED, NOT_AUTHORISED, NOT_REQUIRED }

enum class AcquisitionEligibilityReason {
    CAPABILITY_DISABLED,
    CONFIGURATION_NOT_ACCEPTED,
    CONFIGURATION_NOT_READY,
    HUMAN_AUTHORISED_CUSTODY_NOT_CONFIRMED,
    EXTERNAL_EGRESS_NOT_AUTHORISED,
    UNSUPPORTED_MEDIA_TYPE,
    UNSUPPORTED_SOURCE_FORM,
    SOURCE_TOO_LARGE,
    PAGE_LIMIT_EXCEEDED,
    PAGE_COUNT_UNKNOWN,
    NATIVE_TEXT_CHARACTERISTIC_UNKNOWN,
    NATIVE_TEXT_REQUIRED,
    HANDWRITING_UNSUPPORTED,
    COMPLEX_LAYOUT_UNSUPPORTED,
    TABLES_UNSUPPORTED,
}

sealed class AcquisitionEligibility {
    data object Eligible : AcquisitionEligibility()
    class Ineligible(reasons: Set<AcquisitionEligibilityReason>) : AcquisitionEligibility() {
        val reasons: Set<AcquisitionEligibilityReason> = reasons.toSet()
        override fun equals(other: Any?): Boolean = other is Ineligible && reasons == other.reasons
        override fun hashCode(): Int = reasons.hashCode()
        override fun toString(): String = "Ineligible(reasons=$reasons)"
    }
    class Indeterminate(reasons: Set<AcquisitionEligibilityReason>) : AcquisitionEligibility() {
        val reasons: Set<AcquisitionEligibilityReason> = reasons.toSet()
        override fun equals(other: Any?): Boolean = other is Indeterminate && reasons == other.reasons
        override fun hashCode(): Int = reasons.hashCode()
        override fun toString(): String = "Indeterminate(reasons=$reasons)"
    }
}

/** Pure, deterministic capability/source eligibility projection. It never selects or executes. */
object EvidenceAcquisitionEligibilityEvaluator {
    fun evaluate(
        capability: EvidenceAcquisitionCapability,
        source: AcquisitionSource,
        egressAuthorisation: ExternalEgressAuthorisation,
    ): AcquisitionEligibility {
        val failures = linkedSetOf<AcquisitionEligibilityReason>()
        val unknowns = linkedSetOf<AcquisitionEligibilityReason>()
        (capability.availability as? AcquisitionAvailability.Unavailable)?.let { unavailable ->
            failures += when (unavailable.reason) {
                AcquisitionAvailabilityReason.DISABLED -> AcquisitionEligibilityReason.CAPABILITY_DISABLED
                AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED -> AcquisitionEligibilityReason.CONFIGURATION_NOT_ACCEPTED
                AcquisitionAvailabilityReason.CONFIGURATION_NOT_READY -> AcquisitionEligibilityReason.CONFIGURATION_NOT_READY
            }
        }
        if (source.humanAuthorisedCustody != HumanAuthorisedCustody.CONFIRMED) failures += AcquisitionEligibilityReason.HUMAN_AUTHORISED_CUSTODY_NOT_CONFIRMED
        if (capability.egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED && egressAuthorisation != ExternalEgressAuthorisation.AUTHORISED) {
            failures += AcquisitionEligibilityReason.EXTERNAL_EGRESS_NOT_AUTHORISED
        }
        if (source.mediaType !in capability.supportedMediaTypes) failures += AcquisitionEligibilityReason.UNSUPPORTED_MEDIA_TYPE
        capability.limits.maximumSourceBytes?.let { if (source.byteLength > it) failures += AcquisitionEligibilityReason.SOURCE_TOO_LARGE }
        capability.limits.maximumPages?.let { limit ->
            when (val pages = source.pageCount) {
                is AcquisitionPageCount.Known -> if (pages.value > limit) failures += AcquisitionEligibilityReason.PAGE_LIMIT_EXCEEDED
                AcquisitionPageCount.Unknown -> unknowns += AcquisitionEligibilityReason.PAGE_COUNT_UNKNOWN
            }
        }
        if (capability.mechanism == EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION) {
            when (source.characteristics.nativeSearchableText) {
                AcquisitionCharacteristicState.ABSENT -> failures += AcquisitionEligibilityReason.NATIVE_TEXT_REQUIRED
                AcquisitionCharacteristicState.UNKNOWN -> unknowns += AcquisitionEligibilityReason.NATIVE_TEXT_CHARACTERISTIC_UNKNOWN
                AcquisitionCharacteristicState.PRESENT -> Unit
            }
        }
        if (source.characteristics.imageOnlyOrScanned == AcquisitionCharacteristicState.PRESENT &&
            AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED !in capability.supportedSourceForms) {
            failures += AcquisitionEligibilityReason.UNSUPPORTED_SOURCE_FORM
        }
        if (source.characteristics.mixedTextAndImage == AcquisitionCharacteristicState.PRESENT &&
            AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE !in capability.supportedSourceForms) {
            failures += AcquisitionEligibilityReason.UNSUPPORTED_SOURCE_FORM
        }
        if (source.characteristics.handwriting == AcquisitionCharacteristicState.PRESENT && !capability.fidelity.handwriting) failures += AcquisitionEligibilityReason.HANDWRITING_UNSUPPORTED
        if (source.characteristics.complexLayout == AcquisitionCharacteristicState.PRESENT && !capability.fidelity.layoutAware) failures += AcquisitionEligibilityReason.COMPLEX_LAYOUT_UNSUPPORTED
        if (source.characteristics.tables == AcquisitionCharacteristicState.PRESENT && !capability.fidelity.tableAware) failures += AcquisitionEligibilityReason.TABLES_UNSUPPORTED
        return when {
            failures.isNotEmpty() -> AcquisitionEligibility.Ineligible(failures.toSet())
            unknowns.isNotEmpty() -> AcquisitionEligibility.Indeterminate(unknowns.toSet())
            else -> AcquisitionEligibility.Eligible
        }
    }
}

enum class AcquisitionSelectionReason {
    NATIVE_TEXT_DIRECTLY_AVAILABLE,
    SOURCE_REQUIRES_OCR_OR_TRANSCRIPTION,
    REQUIRED_HANDWRITING_SUPPORT,
    REQUIRED_LAYOUT_SUPPORT,
    REQUIRED_TABLE_SUPPORT,
    STRONGER_SOURCE_RELEVANT_CAPABILITY,
    AVOIDED_UNNECESSARY_TRANSFORMATION,
    AVOIDED_UNNECESSARY_EXTERNAL_EGRESS,
    SOURCE_CHARACTERISTICS_SUPPORTED,
    GOVERNED_CONFIGURATION_ELIGIBLE,
    EXTERNAL_EGRESS_AUTHORISED,
    WITHIN_OPERATIONAL_LIMITS,
}

/** Exact, immutable record of a selection. It has no execution, retry, fallback, or switching API. */
class EvidenceAcquisitionRoutingDecision(
    val source: AcquisitionSource,
    val capability: EvidenceAcquisitionCapability,
    val selectedRepresentation: AcquisitionRepresentationClass,
    selectionReasons: Set<AcquisitionSelectionReason>,
) {
    val selectionReasons: Set<AcquisitionSelectionReason> = selectionReasons.toSet()
    init {
        require(selectedRepresentation in capability.supportedRepresentations)
        require(selectionReasons.isNotEmpty())
    }

    override fun equals(other: Any?): Boolean = other is EvidenceAcquisitionRoutingDecision &&
        source == other.source && capability == other.capability &&
        selectedRepresentation == other.selectedRepresentation && selectionReasons == other.selectionReasons

    override fun hashCode(): Int = listOf(source, capability, selectedRepresentation, selectionReasons).hashCode()
}

enum class AcquisitionNoSelectionReason {
    NO_ELIGIBLE_CAPABILITY,
    UNKNOWN_REQUIRED_CHARACTERISTIC,
    EQUIVALENT_CAPABILITIES_AMBIGUOUS,
    CAPABILITY_DISABLED_OR_NOT_READY,
    EXTERNAL_EGRESS_NOT_AUTHORISED,
    UNSUPPORTED_SOURCE_OR_MEDIA,
    OPERATIONAL_LIMIT_EXCEEDED,
}

sealed class EvidenceAcquisitionRoutingOutcome {
    data class Selected(val decision: EvidenceAcquisitionRoutingDecision) : EvidenceAcquisitionRoutingOutcome()
    class NoEligibleCapability(reasons: Set<AcquisitionNoSelectionReason>) : EvidenceAcquisitionRoutingOutcome() {
        val reasons = reasons.toSet()
        override fun equals(other: Any?): Boolean = other is NoEligibleCapability && reasons == other.reasons
        override fun hashCode(): Int = reasons.hashCode()
    }
    class Indeterminate(reasons: Set<AcquisitionNoSelectionReason>) : EvidenceAcquisitionRoutingOutcome() {
        val reasons = reasons.toSet()
        override fun equals(other: Any?): Boolean = other is Indeterminate && reasons == other.reasons
        override fun hashCode(): Int = reasons.hashCode()
    }
    class Ambiguous(
        capabilityIds: Set<String>,
        reasons: Set<AcquisitionNoSelectionReason>,
    ) : EvidenceAcquisitionRoutingOutcome() {
        val capabilityIds = capabilityIds.toSet()
        val reasons = reasons.toSet()
        override fun equals(other: Any?): Boolean = other is Ambiguous &&
            capabilityIds == other.capabilityIds && reasons == other.reasons
        override fun hashCode(): Int = 31 * capabilityIds.hashCode() + reasons.hashCode()
    }
}
