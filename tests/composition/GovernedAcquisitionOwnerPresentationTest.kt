package parker.composition

import kotlin.test.*
import parker.core.interfaces.*
import parker.core.runtime.*
import parker.ui.*
import kotlinx.coroutines.test.runTest
import java.security.MessageDigest

class GovernedAcquisitionOwnerPresentationTest {
    private val id = EvidenceArtifactId("synthetic-fa6")
    private val sourceFacts = AcquisitionSource(
        id, "a".repeat(64), 100, "application/pdf", AcquisitionPageCount.Known(1),
        AcquisitionSourceCharacteristics(ABSENT, PRESENT, ABSENT, ABSENT, ABSENT, ABSENT),
        HumanAuthorisedCustody.CONFIRMED,
    )

    @Test fun `B clean scan presents local OCR and technical selection without quality claim`() {
        val view = selectedView(sourceFacts, listOf(local(), external()))
        assertEquals("Local OCR", view.capability.mechanismLabel)
        assertFalse(view.capability.externalEgressRequired)
        assertTrue(view.capability.selectionReasons.contains("AVOIDED_UNNECESSARY_EXTERNAL_EGRESS"))
        assertFalse(view.explanation.contains("preferred", true))
    }

    @Test fun `C handwriting accepted external discloses provider model profile egress and exact capability`() {
        val handwriting = sourceFacts.copy(characteristics = sourceFacts.characteristics.copy(handwriting = PRESENT))
        val view = selectedView(handwriting, listOf(local(), external()))
        assertEquals("External transcription", view.capability.mechanismLabel)
        assertEquals("provider", view.capability.provider); assertEquals("fixed-model", view.capability.modelRule)
        assertEquals("profile", view.capability.profile); assertTrue(view.capability.externalEgressRequired)
        assertEquals("external", view.capability.capabilityId)
    }

    @Test fun `D handwriting blocked external shows no eligible and never fabricates local substitute`() {
        val handwriting = sourceFacts.copy(characteristics = sourceFacts.characteristics.copy(handwriting = PRESENT))
        val view = decision(handwriting, listOf(local(), external(AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED))))
        assertIs<OwnerAcquisitionDecisionView.NoEligible>(view)
        assertFalse(view.toString().contains("Local OCR"))
    }

    @Test fun `E unknown characteristic remains visible and has no execution projection`() {
        val unknown = sourceFacts.copy(characteristics = sourceFacts.characteristics.copy(handwriting = UNKNOWN))
        val view = assertIs<OwnerAcquisitionDecisionView.Indeterminate>(decision(unknown, listOf(local(), external())))
        assertEquals("UNKNOWN", view.source.handwriting)
    }

    @Test fun `F equivalent capabilities remain ambiguous without arbitrary action`() {
        val view = assertIs<OwnerAcquisitionDecisionView.Ambiguous>(decision(sourceFacts, listOf(external(id = "a"), external(id = "b"))))
        assertEquals(listOf("a", "b"), view.capabilityIds)
    }

    @Test fun `G acceptance pending external is blocked and unavailable`() {
        val handwriting = sourceFacts.copy(characteristics = sourceFacts.characteristics.copy(handwriting = PRESENT))
        val view = assertIs<OwnerAcquisitionDecisionView.NoEligible>(decision(
            handwriting, listOf(external(AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED))),
        ))
        assertTrue(view.reasons.contains("CAPABILITY_DISABLED_OR_NOT_READY"))
    }

    @Test fun `J successful result retains exact identity fidelity completeness review and source`() {
        val decision = selected(sourceFacts, listOf(local()))
        val result = GovernedAcquisitionOwnerExecution.Executed(
            sourceFacts, decision,
            GovernedAcquisitionExecutionResult.Admitted(
                AcquisitionRoutingProvenance(id, sourceFacts.sha256, "local", LOCAL_OCR, null,
                    AUTHORITATIVE, false, decision.selectionReasons),
                DerivativeGenerationId("generation-exact"), TranscriptionFidelity.UNVERIFIED_LITERAL_TRANSCRIPTION,
                DerivativeCompletenessState.ACCOUNTED_FOR_WITH_QUALIFICATIONS, null,
            ),
        )
        val view = assertIs<OwnerAcquisitionExecutionView.Admitted>(projectGovernedExecution(result))
        assertEquals("generation-exact", view.derivativeGenerationId.value)
        assertEquals("UNVERIFIED_LITERAL_TRANSCRIPTION", view.fidelity)
        assertEquals("ACCOUNTED_FOR_WITH_QUALIFICATIONS", view.completeness)
        assertEquals("UNREVIEWED", view.humanReviewState); assertEquals(id.value, view.evidenceArtifactId)
    }

    @Test fun `L review failed remains visible and M acquisition failure stays bounded without alternate`() {
        val admitted = OwnerAcquisitionExecutionView.Admitted(
            DerivativeGenerationId("g"), id.value, capabilityViewForTest(local()), "UNVERIFIED_LITERAL_TRANSCRIPTION",
            "ACCOUNTED_FOR", "REVIEW_FAILED",
        )
        assertEquals("REVIEW_FAILED", admitted.humanReviewState)
        val decision = selected(sourceFacts, listOf(local()))
        val failed = projectGovernedExecution(GovernedAcquisitionOwnerExecution.Executed(
            sourceFacts, decision, GovernedAcquisitionExecutionResult.Failed(
                AcquisitionExecutionFailureReason.ACQUISITION_EXECUTION_FAILED,
            ),
        ))
        assertEquals("ACQUISITION_EXECUTION_FAILED", assertIs<OwnerAcquisitionExecutionView.Failed>(failed).reason)
    }

    @Test fun `I authoritative source mismatch fails before selected mechanism invocation`() = runTest {
        var invocations = 0
        val sourceBytes = "name,value\na,1\n".toByteArray()
        val staleManifest = EvidenceSourceManifest(id, "a".repeat(64), sourceBytes.size.toLong(), "text/csv")
        val currentManifest = EvidenceSourceManifest(id, MessageDigest.getInstance("SHA-256").digest(sourceBytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }, sourceBytes.size.toLong(), "text/csv")
        var manifestCalls = 0
        val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) = EvidenceAcceptanceResult.Rejected("unused")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = EvidenceRetrievalResult.Found(id, sourceBytes)
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                EvidenceManifestRetrievalResult.Found(if (manifestCalls++ == 0) staleManifest else currentManifest)
        }
        val registry = ProductionAcquisitionCapabilityCatalogue.create()
        val router = DeterministicEvidenceAcquisitionRouter()
        val executor = object : BoundAcquisitionCapabilityExecutor {
            override val binding = AcquisitionExecutorBinding(ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID, DIRECT_NATIVE_EXTRACTION, null)
            override suspend fun execute(request: GovernedAcquisitionExecutionRequest): BoundAcquisitionExecutorOutcome {
                invocations++; return BoundAcquisitionExecutorOutcome.Admitted(DerivativeGenerationId("must-not-run"))
            }
        }
        val workflow = GovernedAcquisitionOwnerWorkflow(
            PrincipalId("owner"), custodian, registry, router,
            GovernedAcquisitionExecutionCoordinator(registry, router, custodian, listOf(executor)),
        )
        val result = assertIs<GovernedAcquisitionOwnerExecution.Executed>(
            workflow.execute(id, ProductionAcquisitionCapabilityCatalogue.NATIVE_CAPABILITY_ID),
        )
        assertEquals(AcquisitionExecutionFailureReason.SOURCE_BINDING_MISMATCH,
            assertIs<GovernedAcquisitionExecutionResult.Failed>(result.result).reason)
        assertEquals(0, invocations)
    }

    // UI-INGESTION-5: GovernedAcquisitionOwnerWorkflow.evaluate now threads a dynamic, per-evidence-
    // artifact externalEgressAuthorised check instead of always ExternalEgressAuthorisation.NOT_AUTHORISED.
    @Test fun `external egress authorization is dynamic and bound to the exact evidence artifact -- not a blanket grant`() = runTest {
        val authorisedId = EvidenceArtifactId("authorised-image")
        val otherId = EvidenceArtifactId("other-image")
        fun custodianFor(id: EvidenceArtifactId) = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) = EvidenceAcceptanceResult.Rejected("unused")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = error("not used")
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(id, "a".repeat(64), 10L, "image/jpeg"))
        }
        val externalImageCapability = EvidenceAcquisitionCapability(
            "external-image", EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION, setOf("image/jpeg"), setOf(IMAGE_ONLY),
            AcquisitionFidelityCapabilities(true, false, true, true, true, true, true, true, true, true),
            setOf(AUTHORITATIVE), AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
            AcquisitionProviderConfiguration("provider", "fixed-model", "profile", "config", "b".repeat(64), "c".repeat(64), "adapter", "1", "external-transcription.direct-byte-exact-v1"),
            AcquisitionAvailability.Available, AcquisitionOperationalLimits(),
        )
        val registry = GovernedAcquisitionCapabilityRegistry(listOf(externalImageCapability))
        val router = DeterministicEvidenceAcquisitionRouter()
        val executionCoordinator = GovernedAcquisitionExecutionCoordinator(registry, router, custodianFor(authorisedId), emptyList())

        fun workflowFor(id: EvidenceArtifactId) = GovernedAcquisitionOwnerWorkflow(
            PrincipalId("owner"), custodianFor(id), registry, router, executionCoordinator,
            externalEgressAuthorised = { it == authorisedId },
        )

        val authorisedResult = workflowFor(authorisedId).evaluate(authorisedId)
        val evaluated = assertIs<GovernedAcquisitionOwnerEvaluation.Evaluated>(authorisedResult)
        assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(evaluated.routing)

        val otherResult = workflowFor(otherId).evaluate(otherId)
        val otherEvaluated = assertIs<GovernedAcquisitionOwnerEvaluation.Evaluated>(otherResult)
        val notSelected = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(otherEvaluated.routing)
        assertTrue(notSelected.reasons.contains(AcquisitionNoSelectionReason.EXTERNAL_EGRESS_NOT_AUTHORISED))
    }

    @Test fun `omitting externalEgressAuthorised preserves the prior always-NOT_AUTHORISED default`() = runTest {
        val custodian = object : EvidenceCustodian {
            override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) = EvidenceAcceptanceResult.Rejected("unused")
            override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = error("not used")
            override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
                EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(id, "a".repeat(64), 10L, "image/jpeg"))
        }
        val externalImageCapability = EvidenceAcquisitionCapability(
            "external-image", EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION, setOf("image/jpeg"), setOf(IMAGE_ONLY),
            AcquisitionFidelityCapabilities(true, false, true, true, true, true, true, true, true, true),
            setOf(AUTHORITATIVE), AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
            AcquisitionProviderConfiguration("provider", "fixed-model", "profile", "config", "b".repeat(64), "c".repeat(64), "adapter", "1", "external-transcription.direct-byte-exact-v1"),
            AcquisitionAvailability.Available, AcquisitionOperationalLimits(),
        )
        val registry = GovernedAcquisitionCapabilityRegistry(listOf(externalImageCapability))
        val router = DeterministicEvidenceAcquisitionRouter()
        val workflow = GovernedAcquisitionOwnerWorkflow(
            PrincipalId("owner"), custodian, registry, router,
            GovernedAcquisitionExecutionCoordinator(registry, router, custodian, emptyList()),
        )
        val evaluated = assertIs<GovernedAcquisitionOwnerEvaluation.Evaluated>(workflow.evaluate(id))
        val notSelected = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(evaluated.routing)
        assertTrue(notSelected.reasons.contains(AcquisitionNoSelectionReason.EXTERNAL_EGRESS_NOT_AUTHORISED))
    }

    @Test fun `presentation and bounded results leak no sentinel content credential body or header`() {
        val rendered = decision(sourceFacts, listOf(local())).toString()
        listOf("secret-sentinel", "Authorization", "Bearer", "Base64", "%PDF-source-content").forEach {
            assertFalse(rendered.contains(it, true))
        }
    }

    private fun decision(source: AcquisitionSource, capabilities: List<EvidenceAcquisitionCapability>): OwnerAcquisitionDecisionView =
        projectGovernedDecision(GovernedAcquisitionOwnerEvaluation.Evaluated(
            source, DeterministicEvidenceAcquisitionRouter().route(source, capabilities, ExternalEgressAuthorisation.AUTHORISED),
        ))
    private fun selectedView(source: AcquisitionSource, capabilities: List<EvidenceAcquisitionCapability>) =
        assertIs<OwnerAcquisitionDecisionView.Selected>(decision(source, capabilities))
    private fun selected(source: AcquisitionSource, capabilities: List<EvidenceAcquisitionCapability>) =
        assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(
            DeterministicEvidenceAcquisitionRouter().route(source, capabilities, ExternalEgressAuthorisation.AUTHORISED),
        ).decision
    private fun capabilityViewForTest(cap: EvidenceAcquisitionCapability) = OwnerAcquisitionCapabilityView(
        cap.capabilityId, "Local OCR", "LOCAL", false, null, null, null,
        "Authoritative source or byte-exact copy", "READY", listOf("SOURCE_CHARACTERISTICS_SUPPORTED"),
    )
    private fun local() = EvidenceAcquisitionCapability(
        "local", LOCAL_OCR, setOf("application/pdf"), setOf(IMAGE_ONLY),
        AcquisitionFidelityCapabilities(true, false, true, false, false, false, true, false, true, false),
        setOf(AUTHORITATIVE), AcquisitionEgress.LOCAL_ONLY, null, AcquisitionAvailability.Available, AcquisitionOperationalLimits(),
    )
    private fun external(availability: AcquisitionAvailability = AcquisitionAvailability.Available, id: String = "external") = EvidenceAcquisitionCapability(
        id, EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION, setOf("application/pdf"), setOf(IMAGE_ONLY),
        AcquisitionFidelityCapabilities(true, false, true, true, true, true, true, true, true, true),
        setOf(AUTHORITATIVE), AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
        AcquisitionProviderConfiguration("provider", "fixed-model", "profile", "config-$id", "b".repeat(64), "c".repeat(64), "adapter", "1", "external-transcription.direct-byte-exact-v1"),
        availability, AcquisitionOperationalLimits(),
    )
    private companion object {
        val PRESENT = AcquisitionCharacteristicState.PRESENT; val ABSENT = AcquisitionCharacteristicState.ABSENT; val UNKNOWN = AcquisitionCharacteristicState.UNKNOWN
        val LOCAL_OCR = EvidenceAcquisitionMechanism.LOCAL_OCR; val IMAGE_ONLY = AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED
        val DIRECT_NATIVE_EXTRACTION = EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION
        val AUTHORITATIVE = AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY
    }
}
