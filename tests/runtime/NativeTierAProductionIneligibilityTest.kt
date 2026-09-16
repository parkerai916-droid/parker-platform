package parker.core.runtime

import kotlin.test.*
import parker.core.interfaces.*

/**
 * STEP 2 -- OpenAI-first production selection correction
 * (FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md §7.2). Proves CSV's deliberate
 * external-verification policy, EML's governed native structured route, and unchanged PDF/image
 * behaviour.
 */
class NativeTierAProductionIneligibilityTest {
    private val csvMediaType = "text/csv"
    private val emlMediaType = "message/rfc822"

    private fun nativeStructuredSource(mediaType: String) = AcquisitionSource(
        EvidenceArtifactId("synthetic-$mediaType"), "a".repeat(64), 100, mediaType,
        AcquisitionPageCount.Unknown, AcquisitionSourceCharacteristics(
            AcquisitionCharacteristicState.PRESENT, AcquisitionCharacteristicState.ABSENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
        ), HumanAuthorisedCustody.CONFIRMED,
    )

    private val searchablePdfSource = AcquisitionSource(
        EvidenceArtifactId("synthetic-searchable-pdf"), "a".repeat(64), 100, "application/pdf",
        AcquisitionPageCount.Known(1), AcquisitionSourceCharacteristics(
            AcquisitionCharacteristicState.PRESENT, AcquisitionCharacteristicState.ABSENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
        ), HumanAuthorisedCustody.CONFIRMED,
    )

    private val scannedImageSource = AcquisitionSource(
        EvidenceArtifactId("synthetic-scanned-image"), "a".repeat(64), 100, "image/png",
        AcquisitionPageCount.Known(1), AcquisitionSourceCharacteristics(
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.PRESENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
        ), HumanAuthorisedCustody.CONFIRMED,
    )

    @Test fun `native capability keeps CSV external policy but accepts structured EML`() {
        val native = ProductionAcquisitionCapabilityCatalogue.nativeCapability()
        assertEquals(AcquisitionFidelitySuitability.NOT_ACCEPTED, native.fidelitySuitabilityByMediaType[csvMediaType])
        assertEquals(AcquisitionFidelitySuitability.ACCEPTED, native.fidelitySuitabilityByMediaType[emlMediaType])
        assertEquals(AcquisitionFidelitySuitability.ACCEPTED, native.fidelitySuitabilityByMediaType["application/pdf"])
    }

    @Test fun `eligibility evaluation keeps CSV native-ineligible and EML native-eligible`() {
        val native = ProductionAcquisitionCapabilityCatalogue.nativeCapability()
        val csv = assertIs<AcquisitionEligibility.Ineligible>(EvidenceAcquisitionEligibilityEvaluator.evaluate(
            native, nativeStructuredSource(csvMediaType), ExternalEgressAuthorisation.AUTHORISED,
        ))
        assertContains(csv.reasons, AcquisitionEligibilityReason.FIDELITY_NOT_ACCEPTED)
        assertIs<AcquisitionEligibility.Eligible>(EvidenceAcquisitionEligibilityEvaluator.evaluate(
            native, nativeStructuredSource(emlMediaType), ExternalEgressAuthorisation.AUTHORISED,
        ))
    }

    @Test fun `router selects native for structured EML while CSV remains externally governed`() {
        val emlOutcome = DeterministicEvidenceAcquisitionRouter().route(
            nativeStructuredSource(emlMediaType), ProductionAcquisitionCapabilityCatalogue.create().capabilities(),
            ExternalEgressAuthorisation.AUTHORISED,
        )
        assertEquals(ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID,
            assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(emlOutcome).decision.capability.capabilityId)
        val csvOutcome = DeterministicEvidenceAcquisitionRouter().route(
            nativeStructuredSource(csvMediaType), ProductionAcquisitionCapabilityCatalogue.create().capabilities(),
            ExternalEgressAuthorisation.AUTHORISED,
        )
        assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(csvOutcome)
    }

    @Test fun `CSV remains fail closed when its external capability is unavailable`() {
        val outcome = DeterministicEvidenceAcquisitionRouter().route(
            nativeStructuredSource(csvMediaType), ProductionAcquisitionCapabilityCatalogue.create().capabilities(),
            ExternalEgressAuthorisation.AUTHORISED,
        )
        val noSelection = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(outcome)
        assertContains(noSelection.reasons, AcquisitionNoSelectionReason.NO_ELIGIBLE_CAPABILITY)
        assertContains(noSelection.reasons, AcquisitionNoSelectionReason.NO_ACCEPTED_FIDELITY_SUITABLE_CAPABILITY)
    }

    @Test fun `structured EML does not fall back to OCR or external transcription`() {
        val outcome = DeterministicEvidenceAcquisitionRouter().route(
            nativeStructuredSource(emlMediaType), ProductionAcquisitionCapabilityCatalogue.create(
                localOcrAvailability = AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.DISABLED),
            ).capabilities(), ExternalEgressAuthorisation.NOT_AUTHORISED,
        )
        assertEquals(ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID,
            assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(outcome).decision.capability.capabilityId)
    }

    @Test fun `searchable PDF selects the durable native representation path`() {
        val outcome = DeterministicEvidenceAcquisitionRouter().route(
            searchablePdfSource, ProductionAcquisitionCapabilityCatalogue.create().capabilities(), ExternalEgressAuthorisation.AUTHORISED,
        )
        val selected = assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(outcome)
        assertEquals(ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID, selected.decision.capability.capabilityId)
    }

    @Test fun `scanned image still fails closed exactly as before this unit, under real production Local OCR disablement`() {
        // ProductionAcquisitionCapabilityCatalogue.create()'s bare default leaves Local OCR
        // Available (deliberately, for offline router/eligibility tests); only the real
        // production composition root (ParkerRuntime) passes the actual disabled availability.
        // Mirror that real production wiring here rather than the catalogue's permissive default.
        val outcome = DeterministicEvidenceAcquisitionRouter().route(
            scannedImageSource,
            ProductionAcquisitionCapabilityCatalogue.create(
                localOcrAvailability = AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.DISABLED),
            ).capabilities(),
            ExternalEgressAuthorisation.AUTHORISED,
        )
        val noSelection = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(outcome)
        assertContains(noSelection.reasons, AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY)
    }
}
