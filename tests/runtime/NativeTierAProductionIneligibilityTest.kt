package parker.core.runtime

import kotlin.test.*
import parker.core.interfaces.*

/**
 * STEP 2 -- OpenAI-first production selection correction
 * (FIDELITY_PRESERVING_EVIDENCE_ACQUISITION_SCOPE_LOCK.md §7.2). Proves Native Tier A is
 * production-ineligible for CSV/EML/DOCX, that no other capability silently fills the gap yet,
 * and that PDF/image behaviour is unchanged.
 */
class NativeTierAProductionIneligibilityTest {
    private val nativeStructuredMediaTypes = listOf(
        "text/csv", "message/rfc822", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    )

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

    @Test fun `native capability reports NOT_ACCEPTED fidelity suitability for CSV, EML, and DOCX`() {
        val native = ProductionAcquisitionCapabilityCatalogue.nativeCapability()
        nativeStructuredMediaTypes.forEach { mediaType ->
            assertEquals(
                AcquisitionFidelitySuitability.NOT_ACCEPTED, native.fidelitySuitabilityByMediaType[mediaType],
                "expected NOT_ACCEPTED for $mediaType",
            )
        }
        assertEquals(AcquisitionFidelitySuitability.NOT_ACCEPTED, native.fidelitySuitabilityByMediaType["application/pdf"])
    }

    @Test fun `eligibility evaluation returns FIDELITY_NOT_ACCEPTED for native on CSV, EML, and DOCX`() {
        val native = ProductionAcquisitionCapabilityCatalogue.nativeCapability()
        nativeStructuredMediaTypes.forEach { mediaType ->
            val outcome = EvidenceAcquisitionEligibilityEvaluator.evaluate(
                native, nativeStructuredSource(mediaType), ExternalEgressAuthorisation.AUTHORISED,
            )
            val ineligible = assertIs<AcquisitionEligibility.Ineligible>(outcome, "expected ineligible for $mediaType")
            assertContains(ineligible.reasons, AcquisitionEligibilityReason.FIDELITY_NOT_ACCEPTED)
        }
    }

    @Test fun `router does not select native for CSV, EML, or DOCX even though nativeSearchableText is PRESENT`() {
        nativeStructuredMediaTypes.forEach { mediaType ->
            val outcome = DeterministicEvidenceAcquisitionRouter().route(
                nativeStructuredSource(mediaType),
                ProductionAcquisitionCapabilityCatalogue.create().capabilities(),
                ExternalEgressAuthorisation.AUTHORISED,
            )
            if (outcome is EvidenceAcquisitionRoutingOutcome.Selected) {
                assertNotEquals(
                    ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID,
                    outcome.decision.capability.capabilityId,
                    "native must not be selected for $mediaType",
                )
            }
        }
    }

    @Test fun `CSV, EML, and DOCX fail closed with no eligible capability under current production registration`() {
        nativeStructuredMediaTypes.forEach { mediaType ->
            val outcome = DeterministicEvidenceAcquisitionRouter().route(
                nativeStructuredSource(mediaType),
                ProductionAcquisitionCapabilityCatalogue.create().capabilities(),
                ExternalEgressAuthorisation.AUTHORISED,
            )
            val noSelection = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(outcome, "expected fail-closed for $mediaType")
            assertContains(noSelection.reasons, AcquisitionNoSelectionReason.NO_ELIGIBLE_CAPABILITY)
            assertContains(noSelection.reasons, AcquisitionNoSelectionReason.NO_ACCEPTED_FIDELITY_SUITABLE_CAPABILITY)
        }
    }

    @Test fun `no silent fallback to native, local OCR, or any unrelated capability for CSV, EML, or DOCX`() {
        nativeStructuredMediaTypes.forEach { mediaType ->
            val outcome = DeterministicEvidenceAcquisitionRouter().route(
                nativeStructuredSource(mediaType),
                ProductionAcquisitionCapabilityCatalogue.create().capabilities(),
                ExternalEgressAuthorisation.AUTHORISED,
            )
            assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(outcome, "expected no selection at all for $mediaType")
        }
    }

    @Test fun `searchable PDF still fails closed exactly as before this unit`() {
        val outcome = DeterministicEvidenceAcquisitionRouter().route(
            searchablePdfSource, ProductionAcquisitionCapabilityCatalogue.create().capabilities(), ExternalEgressAuthorisation.AUTHORISED,
        )
        val noSelection = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(outcome)
        assertContains(noSelection.reasons, AcquisitionNoSelectionReason.NO_ACCEPTED_FIDELITY_SUITABLE_CAPABILITY)
        assertContains(noSelection.reasons, AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY)
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
