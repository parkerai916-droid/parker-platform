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

    @Test fun `STEP 3 -- text-csv is externally supported in the production capability catalogue, and only text-csv was added`() {
        val external = ProductionAcquisitionCapabilityCatalogue.fidelityFirstExternalCapability()
        assertContains(external.supportedMediaTypes, "text/csv")
        assertEquals(AcquisitionFidelitySuitability.ACCEPTED, external.fidelitySuitabilityByMediaType["text/csv"])
        assertFalse("message/rfc822" in external.supportedMediaTypes)
        assertFalse("application/vnd.openxmlformats-officedocument.wordprocessingml.document" in external.supportedMediaTypes)
        assertFalse("text/plain" in external.supportedMediaTypes)
    }

    @Test fun `STEP 3 -- router selects external OpenAI for eligible UTF-8 CSV once the external capability is Available`() {
        val csvSource = AcquisitionSource(
            // Known(1), matching GovernedAcquisitionOwnerWorkflow.projectTechnicalFacts's real
            // production characterisation of text/csv (Step 3): one undivided source, submitted
            // as a single atomic unit for operational page-limit bounding.
            EvidenceArtifactId("synthetic-csv"), "a".repeat(64), 100, "text/csv",
            AcquisitionPageCount.Known(1), AcquisitionSourceCharacteristics(
                AcquisitionCharacteristicState.PRESENT, AcquisitionCharacteristicState.ABSENT,
                AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
                AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
            ), HumanAuthorisedCustody.CONFIRMED,
        )
        val external = ProductionAcquisitionCapabilityCatalogue.fidelityFirstExternalCapability().let { pending ->
            EvidenceAcquisitionCapability(
                pending.capabilityId, pending.mechanism, pending.supportedMediaTypes, pending.supportedSourceForms,
                pending.fidelity, pending.supportedRepresentations, pending.egress, pending.providerConfiguration,
                AcquisitionAvailability.Available, pending.limits, pending.fidelitySuitabilityByMediaType,
            )
        }
        val outcome = DeterministicEvidenceAcquisitionRouter().route(
            csvSource, ProductionAcquisitionCapabilityCatalogue.create(external).capabilities(), ExternalEgressAuthorisation.AUTHORISED,
        )
        val selected = assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(outcome)
        assertEquals(ProductionAcquisitionCapabilityCatalogue.FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID, selected.decision.capability.capabilityId)
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

    @Test fun `STEP 4G -- the EML capability is distinct, unavailable by default, and never widens the fidelity-first capability`() {
        val eml = ProductionAcquisitionCapabilityCatalogue.emlDerivedTextExternalCapability()
        assertEquals(ProductionAcquisitionCapabilityCatalogue.EML_DERIVED_TEXT_EXTERNAL_CAPABILITY_ID, eml.capabilityId)
        assertNotEquals(ProductionAcquisitionCapabilityCatalogue.FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID, eml.capabilityId)
        assertEquals(setOf("message/rfc822"), eml.supportedMediaTypes)
        assertEquals(setOf(AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION), eml.supportedRepresentations)
        assertEquals(AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED), eml.availability)
        assertNull(eml.limits.maximumPages, "EML has no page concept -- none may be fabricated")

        val fidelityFirst = ProductionAcquisitionCapabilityCatalogue.fidelityFirstExternalCapability()
        assertFalse("message/rfc822" in fidelityFirst.supportedMediaTypes, "registering EML must never widen the existing PDF/image/CSV capability")
    }

    @Test fun `STEP 4G -- router selects the EML capability for message-rfc822 once it is Available, native remains untouched`() {
        val emlSource = AcquisitionSource(
            EvidenceArtifactId("synthetic-eml"), "a".repeat(64), 100, "message/rfc822",
            AcquisitionPageCount.Unknown, AcquisitionSourceCharacteristics(
                AcquisitionCharacteristicState.PRESENT, AcquisitionCharacteristicState.ABSENT,
                AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
                AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
            ), HumanAuthorisedCustody.CONFIRMED,
        )
        val emlTemplate = ProductionAcquisitionCapabilityCatalogue.emlDerivedTextExternalCapability()
        val emlAvailable = EvidenceAcquisitionCapability(
            emlTemplate.capabilityId, emlTemplate.mechanism, emlTemplate.supportedMediaTypes, emlTemplate.supportedSourceForms,
            emlTemplate.fidelity, emlTemplate.supportedRepresentations, emlTemplate.egress, emlTemplate.providerConfiguration,
            AcquisitionAvailability.Available, emlTemplate.limits, emlTemplate.fidelitySuitabilityByMediaType,
        )
        val outcome = DeterministicEvidenceAcquisitionRouter().route(
            emlSource,
            ProductionAcquisitionCapabilityCatalogue.create(emlExternalCapabilityProjection = emlAvailable).capabilities(),
            ExternalEgressAuthorisation.AUTHORISED,
        )
        val selected = assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(outcome)
        assertEquals(ProductionAcquisitionCapabilityCatalogue.EML_DERIVED_TEXT_EXTERNAL_CAPABILITY_ID, selected.decision.capability.capabilityId)
        assertNotEquals(ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID, selected.decision.capability.capabilityId)
    }

    private val emlSource = AcquisitionSource(
        EvidenceArtifactId("synthetic-eml"), "a".repeat(64), 100, "message/rfc822",
        AcquisitionPageCount.Unknown, AcquisitionSourceCharacteristics(
            AcquisitionCharacteristicState.PRESENT, AcquisitionCharacteristicState.ABSENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
            AcquisitionCharacteristicState.ABSENT, AcquisitionCharacteristicState.ABSENT,
        ), HumanAuthorisedCustody.CONFIRMED,
    )

    private fun available(template: EvidenceAcquisitionCapability) = EvidenceAcquisitionCapability(
        template.capabilityId, template.mechanism, template.supportedMediaTypes, template.supportedSourceForms,
        template.fidelity, template.supportedRepresentations, template.egress, template.providerConfiguration,
        AcquisitionAvailability.Available, template.limits, template.fidelitySuitabilityByMediaType,
    )

    // Three narrow acceptance extensions -- the two accepted profiles project two independent
    // capabilities in the same registered catalogue; neither's acceptance leaks into the other.
    @Test fun `accepting fidelity-first alone leaves the EML capability at its own unavailable default`() {
        val fidelityFirstAvailable = available(ProductionAcquisitionCapabilityCatalogue.fidelityFirstExternalCapability())
        val catalogue = ProductionAcquisitionCapabilityCatalogue.create(
            externalCapabilityProjection = fidelityFirstAvailable,
            emlExternalCapabilityProjection = ProductionAcquisitionCapabilityCatalogue.emlDerivedTextExternalCapability(),
        )
        val eml = catalogue.capabilities().single { it.capabilityId == ProductionAcquisitionCapabilityCatalogue.EML_DERIVED_TEXT_EXTERNAL_CAPABILITY_ID }
        assertEquals(AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED), eml.availability)

        val outcome = DeterministicEvidenceAcquisitionRouter().route(emlSource, catalogue.capabilities(), ExternalEgressAuthorisation.AUTHORISED)
        val noSelection = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(outcome)
        assertContains(noSelection.reasons, AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY)
    }

    @Test fun `accepting EML alone leaves the fidelity-first capability at its own unavailable default`() {
        val emlAvailable = available(ProductionAcquisitionCapabilityCatalogue.emlDerivedTextExternalCapability())
        val catalogue = ProductionAcquisitionCapabilityCatalogue.create(emlExternalCapabilityProjection = emlAvailable)
        val fidelityFirst = catalogue.capabilities().single { it.capabilityId == ProductionAcquisitionCapabilityCatalogue.FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID }
        assertEquals(AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED), fidelityFirst.availability)

        val outcome = DeterministicEvidenceAcquisitionRouter().route(source, catalogue.capabilities(), ExternalEgressAuthorisation.AUTHORISED)
        val noSelection = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(outcome)
        assertContains(noSelection.reasons, AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY)
    }

    @Test fun `fidelity-first and EML accepted profiles coexist -- each routes independently for its own media type`() {
        val fidelityFirstAvailable = available(ProductionAcquisitionCapabilityCatalogue.fidelityFirstExternalCapability())
        val emlAvailable = available(ProductionAcquisitionCapabilityCatalogue.emlDerivedTextExternalCapability())
        val catalogue = ProductionAcquisitionCapabilityCatalogue.create(
            externalCapabilityProjection = fidelityFirstAvailable,
            emlExternalCapabilityProjection = emlAvailable,
        )

        val pdfOutcome = DeterministicEvidenceAcquisitionRouter().route(source, catalogue.capabilities(), ExternalEgressAuthorisation.AUTHORISED)
        assertEquals(
            ProductionAcquisitionCapabilityCatalogue.FIDELITY_FIRST_EXTERNAL_CAPABILITY_ID,
            assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(pdfOutcome).decision.capability.capabilityId,
        )

        val emlOutcome = DeterministicEvidenceAcquisitionRouter().route(emlSource, catalogue.capabilities(), ExternalEgressAuthorisation.AUTHORISED)
        assertEquals(
            ProductionAcquisitionCapabilityCatalogue.EML_DERIVED_TEXT_EXTERNAL_CAPABILITY_ID,
            assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(emlOutcome).decision.capability.capabilityId,
        )

        // No side-effect on native's own per-media-type fidelity suitability from accepting either
        // or both externals -- native remains NOT_ACCEPTED for every media type regardless.
        val native = catalogue.capabilities().single { it.capabilityId == ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID }
        assertEquals(AcquisitionFidelitySuitability.NOT_ACCEPTED, native.fidelitySuitabilityByMediaType["message/rfc822"])
        assertEquals(AcquisitionFidelitySuitability.NOT_ACCEPTED, native.fidelitySuitabilityByMediaType["application/pdf"])
    }

    // Three narrow acceptance extensions -- projectExternalTranscriptionCapability (the shared,
    // extracted acceptance-gate ParkerRuntime now uses for both the fidelity-first and EML
    // capabilities) must project Available only for Ready+ACCEPTED, and leave every other
    // readiness/acceptance-state combination at the template's own existing availability.
    @Test fun `projectExternalTranscriptionCapability gates on Ready and ACCEPTED only`() {
        val template = ProductionAcquisitionCapabilityCatalogue.emlDerivedTextExternalCapability()
        assertEquals(template, parker.composition.projectExternalTranscriptionCapability(template, parker.composition.OpenAiExternalTranscriptionReadiness.Disabled))
        assertEquals(template, parker.composition.projectExternalTranscriptionCapability(template, parker.composition.OpenAiExternalTranscriptionReadiness.InvalidProfile("missing/invalid EML profile")))
        assertEquals(template, parker.composition.projectExternalTranscriptionCapability(template, parker.composition.OpenAiExternalTranscriptionReadiness.StaleProfile(java.time.LocalDate.parse("2026-01-01"))))

        val basisProfile = ProfileFixtures.eml(parker.composition.ExternalTranscriptionAcceptanceState.ACCEPTANCE_PENDING)
        val pendingReady = parker.composition.OpenAiExternalTranscriptionReadiness.Ready(basisProfile, ProfileFixtures.LIMITS)
        assertEquals(AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED),
            parker.composition.projectExternalTranscriptionCapability(template, pendingReady).availability)

        val suspendedReady = parker.composition.OpenAiExternalTranscriptionReadiness.Ready(
            ProfileFixtures.eml(parker.composition.ExternalTranscriptionAcceptanceState.SUSPENDED), ProfileFixtures.LIMITS,
        )
        assertEquals(AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED),
            parker.composition.projectExternalTranscriptionCapability(template, suspendedReady).availability)

        val acceptedReady = parker.composition.OpenAiExternalTranscriptionReadiness.Ready(
            ProfileFixtures.eml(parker.composition.ExternalTranscriptionAcceptanceState.ACCEPTED), ProfileFixtures.LIMITS,
        )
        val projected = parker.composition.projectExternalTranscriptionCapability(template, acceptedReady)
        assertEquals(AcquisitionAvailability.Available, projected.availability)
        assertEquals(template.capabilityId, projected.capabilityId)
    }

    private object ProfileFixtures {
        val LIMITS = parker.composition.OpenAiExternalTranscriptionEffectiveLimits(
            maximumPdfBytes = 1, maximumImageBytes = 1, maximumOutputBytes = 1, timeoutMillis = 1,
        )
        fun eml(state: parker.composition.ExternalTranscriptionAcceptanceState) = parker.composition.OpenAiExternalTranscriptionProviderProfile(
            schemaVersion = "4", providerIdentity = "OpenAI", apiProductPath = "/v1/responses", store = false,
            modelSelectionRule = "gpt-5.6-sol", modelSnapshotPolicy = "RECORD_PRESENT_OR_NOT_EXPOSED",
            maximumPdfBytes = 1, maximumImageBytes = 1, maximumOutputBytes = 1, timeoutMillis = 1,
            allowedNetworkDestination = "https://api.openai.com", retentionTreatment = "x", dataUseTrainingTreatment = "x",
            zdrMamStatus = "x", projectAccountStatus = "x", projectAccountControls = "x",
            authenticationMechanism = "BEARER_API_CREDENTIAL", requestLoggingConsiderations = "x", regionalStorageConsiderations = "x",
            verifiedOn = java.time.LocalDate.parse("2026-01-01"), approvingOwnerReference = "x",
            nextReviewDate = java.time.LocalDate.parse("2027-01-01"), verificationReferences = listOf("x"), reverificationTriggers = listOf("x"),
            transcriptionProfileId = parker.core.runtime.EML_TRANSCRIPTION_PROFILE_ID,
            instructionSha256 = parker.core.runtime.EML_VERIFICATION_INSTRUCTION_SHA256,
            structuredSchemaSha256 = parker.core.runtime.EML_VERIFICATION_SCHEMA_SHA256,
            processingProfileIdentity = parker.core.runtime.EML_PROCESSING_PROFILE_IDENTITY,
            acceptanceState = state, reasoningEffort = "none", pdfDetail = "NOT_APPLICABLE", imageDetail = "NOT_APPLICABLE",
        )
    }
}
