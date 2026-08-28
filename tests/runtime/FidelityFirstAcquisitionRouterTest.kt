package parker.core.runtime

import kotlin.test.*
import parker.core.interfaces.*

class FidelityFirstAcquisitionRouterTest {
    private val source = AcquisitionSource(
        EvidenceArtifactId("synthetic-searchable-pdf"), "a".repeat(64), 100, "application/pdf",
        AcquisitionPageCount.Known(1), AcquisitionSourceCharacteristics(
            AcquisitionCharacteristicState.PRESENT, AcquisitionCharacteristicState.ABSENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
        ), HumanAuthorisedCustody.CONFIRMED,
    )

    @Test fun `searchable PDF does not early-select source-inaccurate native extraction`() {
        val outcome = DeterministicEvidenceAcquisitionRouter().route(
            source, ProductionAcquisitionCapabilityCatalogue.create().capabilities(), ExternalEgressAuthorisation.AUTHORISED,
        )
        val noSelection = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(outcome)
        assertContains(noSelection.reasons, AcquisitionNoSelectionReason.NO_ACCEPTED_FIDELITY_SUITABLE_CAPABILITY)
        assertContains(noSelection.reasons, AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY)
    }

    @Test fun `accepted lifecycle projection makes fidelity-first external capability primary`() {
        val external = ProductionAcquisitionCapabilityCatalogue.fidelityFirstExternalCapability().let { pending ->
            EvidenceAcquisitionCapability(
                pending.capabilityId, pending.mechanism, pending.supportedMediaTypes, pending.supportedSourceForms,
                pending.fidelity, pending.supportedRepresentations, pending.egress, pending.providerConfiguration,
                AcquisitionAvailability.Available, pending.limits, pending.fidelitySuitabilityByMediaType,
            )
        }
        val outcome = DeterministicEvidenceAcquisitionRouter().route(
            source, ProductionAcquisitionCapabilityCatalogue.create(external).capabilities(), ExternalEgressAuthorisation.AUTHORISED,
        )
        val selected = assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(outcome)
        assertEquals(ProductionAcquisitionCapabilityCatalogue.FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID, selected.decision.capability.capabilityId)
        assertContains(selected.decision.selectionReasons, AcquisitionSelectionReason.FIDELITY_SUITABILITY_ACCEPTED)
    }

    @Test fun `catalogue registers exactly one external capability and it is not executable before acceptance`() {
        val external = ProductionAcquisitionCapabilityCatalogue.create().capabilities().filter {
            it.mechanism == EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION ||
                it.mechanism == EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION
        }
        assertEquals(1, external.size)
        assertEquals(AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED), external.single().availability)
        assertEquals("none", external.single().providerConfiguration?.reasoningEffort)
        assertEquals("original", external.single().providerConfiguration?.imageDetail)
    }
}
