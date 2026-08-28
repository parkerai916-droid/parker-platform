package parker.core.runtime

import parker.core.interfaces.*

/**
 * Pure FA.4 selection policy. It consumes bounded facts and returns one decision or an explicit
 * no-selection disposition. It has no dependencies, byte input, environmental lookup, or
 * execution operation.
 */
class DeterministicEvidenceAcquisitionRouter {
    fun route(
        source: AcquisitionSource,
        capabilities: Collection<EvidenceAcquisitionCapability>,
        egressAuthorisation: ExternalEgressAuthorisation,
    ): EvidenceAcquisitionRoutingOutcome {
        require(capabilities.map { it.capabilityId }.distinct().size == capabilities.size) {
            "Acquisition capability identifiers must be unique"
        }

        val evaluations = capabilities.map { capability ->
            capability to EvidenceAcquisitionEligibilityEvaluator.evaluate(capability, source, egressAuthorisation)
        }
        val eligible = evaluations.filter { it.second == AcquisitionEligibility.Eligible }.map { it.first }
        val indeterminate = evaluations.filter { it.second is AcquisitionEligibility.Indeterminate }.map { it.first }

        if (eligible.isEmpty()) {
            return if (indeterminate.isNotEmpty()) {
                EvidenceAcquisitionRoutingOutcome.Indeterminate(setOf(AcquisitionNoSelectionReason.UNKNOWN_REQUIRED_CHARACTERISTIC))
            } else {
                EvidenceAcquisitionRoutingOutcome.NoEligibleCapability(noEligibleReasons(evaluations.map { it.second }))
            }
        }

        val nativeState = source.characteristics.nativeSearchableText
        if (source.mediaType != "application/pdf" && nativeState == AcquisitionCharacteristicState.PRESENT) {
            val native = eligible.filter { it.mechanism == EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION }
            if (native.isNotEmpty()) return selectAmong(source, native, egressAuthorisation,
                setOf(AcquisitionSelectionReason.NATIVE_TEXT_DIRECTLY_AVAILABLE))
        }
        if (nativeState == AcquisitionCharacteristicState.UNKNOWN &&
            (indeterminate.any { it.mechanism == EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION } ||
                eligible.any { it.mechanism == EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION })
        ) return indeterminate()

        if (indeterminate.isNotEmpty()) return indeterminate()
        if (unknownMateriallyAffectsSelection(source, eligible)) return indeterminate()

        val suitable = eligible.filter { isFullySuitable(source, it) }
        if (suitable.isEmpty()) {
            return EvidenceAcquisitionRoutingOutcome.NoEligibleCapability(
                setOf(AcquisitionNoSelectionReason.NO_ELIGIBLE_CAPABILITY, AcquisitionNoSelectionReason.UNSUPPORTED_SOURCE_OR_MEDIA),
            )
        }
        return selectAmong(source, suitable, egressAuthorisation, baseReasons(source))
    }

    private fun selectAmong(
        source: AcquisitionSource,
        candidates: List<EvidenceAcquisitionCapability>,
        egressAuthorisation: ExternalEgressAuthorisation,
        initialReasons: Set<AcquisitionSelectionReason>,
    ): EvidenceAcquisitionRoutingOutcome {
        var survivors = candidates.filter { candidate ->
            candidates.none { other -> other !== candidate && dominates(source, other, candidate) }
        }
        val reasons = initialReasons.toMutableSet()
        if (survivors.size > 1) {
            val direct = survivors.filter { preferredRepresentation(it) == AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY }
            if (direct.isNotEmpty() && direct.size < survivors.size) {
                survivors = direct
                reasons += AcquisitionSelectionReason.AVOIDED_UNNECESSARY_TRANSFORMATION
            }
        }
        if (survivors.size > 1) {
            val local = survivors.filter { it.egress == AcquisitionEgress.LOCAL_ONLY }
            if (local.isNotEmpty() && local.size < survivors.size) {
                survivors = local
                reasons += AcquisitionSelectionReason.AVOIDED_UNNECESSARY_EXTERNAL_EGRESS
            }
        }
        if (survivors.size != 1) {
            return EvidenceAcquisitionRoutingOutcome.Ambiguous(
                survivors.map { it.capabilityId }.toSet(),
                setOf(AcquisitionNoSelectionReason.EQUIVALENT_CAPABILITIES_AMBIGUOUS),
            )
        }
        val selected = survivors.single()
        if (selected.mechanism == EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION &&
            source.characteristics.nativeSearchableText == AcquisitionCharacteristicState.PRESENT) {
            reasons += AcquisitionSelectionReason.NATIVE_TEXT_DIRECTLY_AVAILABLE
        }
        if (candidates.any { it != selected && dominates(source, selected, it) }) {
            reasons += AcquisitionSelectionReason.STRONGER_SOURCE_RELEVANT_CAPABILITY
        }
        reasons += AcquisitionSelectionReason.SOURCE_CHARACTERISTICS_SUPPORTED
        reasons += AcquisitionSelectionReason.FIDELITY_SUITABILITY_ACCEPTED
        reasons += AcquisitionSelectionReason.GOVERNED_CONFIGURATION_ELIGIBLE
        reasons += AcquisitionSelectionReason.WITHIN_OPERATIONAL_LIMITS
        if (selected.egress == AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED &&
            egressAuthorisation == ExternalEgressAuthorisation.AUTHORISED) {
            reasons += AcquisitionSelectionReason.EXTERNAL_EGRESS_AUTHORISED
        }
        return EvidenceAcquisitionRoutingOutcome.Selected(
            EvidenceAcquisitionRoutingDecision(source, selected, preferredRepresentation(selected), reasons),
        )
    }

    private fun preferredRepresentation(capability: EvidenceAcquisitionCapability): AcquisitionRepresentationClass =
        if (AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY in capability.supportedRepresentations) {
            AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY
        } else AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION

    private fun isFullySuitable(source: AcquisitionSource, capability: EvidenceAcquisitionCapability): Boolean {
        val c = source.characteristics
        if ((c.imageOnlyOrScanned == AcquisitionCharacteristicState.PRESENT ||
                c.mixedTextAndImage == AcquisitionCharacteristicState.PRESENT) &&
            capability.mechanism == EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION &&
            c.nativeSearchableText != AcquisitionCharacteristicState.PRESENT) return false
        if (c.imageOnlyOrScanned == AcquisitionCharacteristicState.PRESENT &&
            !capability.fidelity.ocr && !capability.fidelity.literalTranscription) return false
        return true
    }

    private fun unknownMateriallyAffectsSelection(
        source: AcquisitionSource,
        candidates: List<EvidenceAcquisitionCapability>,
    ): Boolean {
        val c = source.characteristics
        fun unknownNotUniversallyCovered(state: AcquisitionCharacteristicState, covered: (EvidenceAcquisitionCapability) -> Boolean) =
            state == AcquisitionCharacteristicState.UNKNOWN && candidates.any { !covered(it) }
        return unknownNotUniversallyCovered(c.handwriting) { it.fidelity.handwriting } ||
            unknownNotUniversallyCovered(c.complexLayout) { it.fidelity.layoutAware } ||
            unknownNotUniversallyCovered(c.tables) { it.fidelity.tableAware } ||
            unknownNotUniversallyCovered(c.imageOnlyOrScanned) { AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED in it.supportedSourceForms } ||
            unknownNotUniversallyCovered(c.mixedTextAndImage) { AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE in it.supportedSourceForms }
    }

    private fun dominates(
        source: AcquisitionSource,
        left: EvidenceAcquisitionCapability,
        right: EvidenceAcquisitionCapability,
    ): Boolean {
        val leftFacts = relevantFacts(source, left)
        val rightFacts = relevantFacts(source, right)
        return leftFacts.containsAll(rightFacts) && leftFacts.size > rightFacts.size
    }

    private fun relevantFacts(source: AcquisitionSource, capability: EvidenceAcquisitionCapability): Set<String> = buildSet {
        val c = source.characteristics
        if (capability.fidelity.literalTranscription) add("LITERAL_TRANSCRIPTION")
        if (capability.fidelity.uncertaintyReporting) add("UNCERTAINTY_REPORTING")
        if (source.pageCount is AcquisitionPageCount.Known && source.pageCount.value > 1 && capability.fidelity.pageAssociation) add("PAGE_ASSOCIATION")
        if (c.nativeSearchableText == AcquisitionCharacteristicState.PRESENT && capability.fidelity.nativeTextExtraction) add("NATIVE_TEXT_EXTRACTION")
        if (c.imageOnlyOrScanned == AcquisitionCharacteristicState.PRESENT && capability.fidelity.ocr) add("OCR")
        if (c.handwriting == AcquisitionCharacteristicState.PRESENT && capability.fidelity.handwriting) add("HANDWRITING")
        if (c.complexLayout == AcquisitionCharacteristicState.PRESENT && capability.fidelity.layoutAware) add("LAYOUT")
        if (c.complexLayout == AcquisitionCharacteristicState.PRESENT && capability.fidelity.regionAssociation) add("REGION_ASSOCIATION")
        if (c.tables == AcquisitionCharacteristicState.PRESENT && capability.fidelity.tableAware) add("TABLES")
        if ((c.tables == AcquisitionCharacteristicState.PRESENT || c.mixedTextAndImage == AcquisitionCharacteristicState.PRESENT) && capability.fidelity.structuredOutput) add("STRUCTURED_OUTPUT")
    }

    private fun baseReasons(source: AcquisitionSource): Set<AcquisitionSelectionReason> = buildSet {
        val c = source.characteristics
        if (c.imageOnlyOrScanned == AcquisitionCharacteristicState.PRESENT || c.nativeSearchableText == AcquisitionCharacteristicState.ABSENT) {
            add(AcquisitionSelectionReason.SOURCE_REQUIRES_OCR_OR_TRANSCRIPTION)
        }
        if (c.handwriting == AcquisitionCharacteristicState.PRESENT) add(AcquisitionSelectionReason.REQUIRED_HANDWRITING_SUPPORT)
        if (c.complexLayout == AcquisitionCharacteristicState.PRESENT) add(AcquisitionSelectionReason.REQUIRED_LAYOUT_SUPPORT)
        if (c.tables == AcquisitionCharacteristicState.PRESENT) add(AcquisitionSelectionReason.REQUIRED_TABLE_SUPPORT)
    }

    private fun indeterminate() = EvidenceAcquisitionRoutingOutcome.Indeterminate(
        setOf(AcquisitionNoSelectionReason.UNKNOWN_REQUIRED_CHARACTERISTIC),
    )

    private fun noEligibleReasons(evaluations: List<AcquisitionEligibility>): Set<AcquisitionNoSelectionReason> = buildSet {
        add(AcquisitionNoSelectionReason.NO_ELIGIBLE_CAPABILITY)
        evaluations.filterIsInstance<AcquisitionEligibility.Ineligible>().flatMap { it.reasons }.forEach { reason ->
            when (reason) {
                AcquisitionEligibilityReason.CAPABILITY_DISABLED,
                AcquisitionEligibilityReason.CONFIGURATION_NOT_ACCEPTED,
                AcquisitionEligibilityReason.CONFIGURATION_NOT_READY,
                -> add(AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY)
                AcquisitionEligibilityReason.FIDELITY_NOT_ACCEPTED -> add(AcquisitionNoSelectionReason.NO_ACCEPTED_FIDELITY_SUITABLE_CAPABILITY)
                AcquisitionEligibilityReason.FIDELITY_UNDETERMINED -> add(AcquisitionNoSelectionReason.FIDELITY_SUITABILITY_UNDETERMINED)
                AcquisitionEligibilityReason.EXTERNAL_EGRESS_NOT_AUTHORISED -> add(AcquisitionNoSelectionReason.EXTERNAL_EGRESS_NOT_AUTHORISED)
                AcquisitionEligibilityReason.SOURCE_TOO_LARGE,
                AcquisitionEligibilityReason.PAGE_LIMIT_EXCEEDED,
                -> add(AcquisitionNoSelectionReason.OPERATIONAL_LIMIT_EXCEEDED)
                else -> add(AcquisitionNoSelectionReason.UNSUPPORTED_SOURCE_OR_MEDIA)
            }
        }
    }
}
