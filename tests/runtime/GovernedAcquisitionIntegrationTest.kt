package parker.core.runtime

import java.security.MessageDigest
import kotlin.test.*
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.*

class GovernedAcquisitionIntegrationTest {
    private val owner = PrincipalId("owner")
    private val id = EvidenceArtifactId("synthetic")
    private val bytes = "%PDF-synthetic".toByteArray()
    private val sha = digest(bytes)

    @Test fun `A born digital routes and executes only exact native capability`() = runTest {
        val counters = Counters()
        val result = coordinator(listOf(nativeExec(counters), localExec(counters), externalExec(counters)))
            .execute(owner, source(native = PRESENT, image = ABSENT), AUTHORISED)
        assertIs<GovernedAcquisitionExecutionResult.Admitted>(result)
        assertEquals(listOf(1, 0, 0), listOf(counters.native, counters.local, counters.external))
    }

    @Test fun `B clean scan executes local only and never native or external`() = runTest {
        val counters = Counters()
        val result = coordinator(listOf(nativeExec(counters), localExec(counters), externalExec(counters)))
            .execute(owner, source(), AUTHORISED)
        assertIs<GovernedAcquisitionExecutionResult.Admitted>(result)
        assertEquals(listOf(0, 1, 0), listOf(counters.native, counters.local, counters.external))
    }

    @Test fun `C handwriting executes one exact accepted external fake`() = runTest {
        val counters = Counters()
        val result = coordinator(listOf(nativeExec(counters), localExec(counters), externalExec(counters)))
            .execute(owner, source(handwriting = PRESENT), AUTHORISED)
        val admitted = assertIs<GovernedAcquisitionExecutionResult.Admitted>(result)
        assertEquals("external", admitted.routingProvenance.capabilityId)
        assertEquals(listOf(0, 0, 1), listOf(counters.native, counters.local, counters.external))
    }

    @Test fun `D handwriting without egress performs no execution`() = runTest {
        val counters = Counters()
        val result = coordinator(allExecutors(counters)).execute(owner, source(handwriting = PRESENT), NOT_AUTHORISED)
        assertEquals(ROUTING_NO_ELIGIBLE_CAPABILITY, failed(result).reason)
        assertEquals(0, counters.total())
    }

    @Test fun `E unknown material characteristic returns indeterminate with no execution`() = runTest {
        val counters = Counters()
        val result = coordinator(allExecutors(counters)).execute(owner, source(handwriting = UNKNOWN), AUTHORISED)
        assertEquals(ROUTING_INDETERMINATE, failed(result).reason)
        assertEquals(0, counters.total())
    }

    @Test fun `F equivalent external capabilities return ambiguous with no execution`() = runTest {
        val counters = Counters()
        val caps = listOf(external("external-a"), external("external-b"))
        val execs = caps.map { countingExecutor(it, counters) { counters.external++; admitted() } }
        val result = coordinator(execs, GovernedAcquisitionCapabilityRegistry(caps)).execute(owner, source(), AUTHORISED)
        assertEquals(ROUTING_AMBIGUOUS, failed(result).reason)
        assertEquals(0, counters.total())
    }

    @Test fun `G selected executor missing fails without alternative`() = runTest {
        val counters = Counters()
        val result = coordinator(listOf(externalExec(counters))).execute(owner, source(), AUTHORISED)
        assertEquals(SELECTED_CAPABILITY_UNAVAILABLE, failed(result).reason)
        assertEquals(0, counters.total())
    }

    @Test fun `H selected capability cannot be substituted by mismatched mechanism or configuration`() = runTest {
        val counters = Counters()
        val wrong = object : BoundAcquisitionCapabilityExecutor {
            override val binding = AcquisitionExecutorBinding("local", EXTERNAL_VISION_TRANSCRIPTION, "wrong-config")
            override suspend fun execute(request: GovernedAcquisitionExecutionRequest): BoundAcquisitionExecutorOutcome {
                counters.external++; return admitted()
            }
        }
        val result = coordinator(listOf(wrong, externalExec(counters))).execute(owner, source(), AUTHORISED)
        assertEquals(CAPABILITY_BINDING_MISMATCH, failed(result).reason)
        assertEquals(0, counters.total())
    }

    @Test fun `I stale or substituted source facts fail before acquisition`() = runTest {
        val counters = Counters()
        val stale = source().copy(sha256 = "0".repeat(64))
        val result = coordinator(allExecutors(counters)).execute(owner, stale, AUTHORISED)
        assertEquals(SOURCE_BINDING_MISMATCH, failed(result).reason)
        assertEquals(0, counters.total())
    }

    @Test fun `J selected local exception fails with zero external fallback`() = runTest {
        val counters = Counters()
        val throwingLocal = countingExecutor(local(), counters) { counters.local++; error("sentinel-secret-content") }
        val result = coordinator(listOf(nativeExec(counters), throwingLocal, externalExec(counters)))
            .execute(owner, source(), AUTHORISED)
        assertEquals(ACQUISITION_EXECUTION_FAILED, failed(result).reason)
        assertEquals(listOf(0, 1, 0), listOf(counters.native, counters.local, counters.external))
        assertFalse(result.toString().contains("sentinel-secret-content"))
    }

    @Test fun `K selected external failure has zero local retry or provider switch`() = runTest {
        val counters = Counters()
        val failing = countingExecutor(external(), counters) {
            counters.external++; BoundAcquisitionExecutorOutcome.ExecutionFailed(ACQUISITION_EXECUTION_FAILED)
        }
        val result = coordinator(listOf(nativeExec(counters), localExec(counters), failing))
            .execute(owner, source(handwriting = PRESENT), AUTHORISED)
        assertEquals(ACQUISITION_EXECUTION_FAILED, failed(result).reason)
        assertEquals(listOf(0, 0, 1), listOf(counters.native, counters.local, counters.external))
    }

    @Test fun `L admission failure performs no second acquisition`() = runTest {
        val counters = Counters()
        val admissionFailure = countingExecutor(local(), counters) {
            counters.local++; BoundAcquisitionExecutorOutcome.AdmissionFailed(DERIVATIVE_ADMISSION_FAILED)
        }
        val result = coordinator(listOf(admissionFailure, externalExec(counters))).execute(owner, source(), AUTHORISED)
        assertEquals(DERIVATIVE_ADMISSION_FAILED, failed(result).reason)
        assertEquals(listOf(0, 1, 0), listOf(counters.native, counters.local, counters.external))
    }

    @Test fun `registry rejects duplicate identity preserves external tuple and cannot execute`() {
        assertFailsWith<IllegalArgumentException> { GovernedAcquisitionCapabilityRegistry(listOf(local(), local())) }
        val registry = GovernedAcquisitionCapabilityRegistry(listOf(external()))
        assertEquals("config-external", registry.capability("external")?.providerConfiguration?.configurationIdentity)
        val methods = GovernedAcquisitionCapabilityRegistry::class.java.declaredMethods.map { it.name.lowercase() }
        assertFalse(methods.any { "execute" in it || "transcribe" in it || "recognise" in it })
    }

    @Test fun `registration preserves unavailable state and does not imply acceptance`() {
        val unavailable = external(availability = AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED))
        val registered = GovernedAcquisitionCapabilityRegistry(listOf(unavailable)).capability("external")!!
        assertEquals(unavailable.availability, registered.availability)
    }

    @Test fun `production catalogue is conservative and registry order cannot alter routing`() {
        val registry = ProductionAcquisitionCapabilityCatalogue.create(external())
        val local = registry.capability(ProductionAcquisitionCapabilityCatalogue.LOCAL_OCR_CAPABILITY_ID)!!
        assertFalse(local.fidelity.handwriting)
        assertFalse(local.fidelity.layoutAware)
        assertFalse(local.fidelity.tableAware)
        val forward = DeterministicEvidenceAcquisitionRouter().route(source(), registry.capabilities(), AUTHORISED)
        val reverse = DeterministicEvidenceAcquisitionRouter().route(source(), registry.capabilities().reversed(), AUTHORISED)
        assertEquals(forward, reverse)
    }

    @Test fun `source projector preserves unknowns and never requires bytes or case meaning`() {
        val manifest = EvidenceSourceManifest(id, sha, bytes.size.toLong(), "application/pdf")
        val projected = AcquisitionSourceCharacteristicsProjector.project(manifest)!!
        assertEquals(UNKNOWN, projected.characteristics.nativeSearchableText)
        assertEquals(UNKNOWN, projected.characteristics.handwriting)
        val fields = AcquisitionSourceCharacteristicsProjector::class.java.declaredMethods.flatMap { it.parameterTypes.toList() }
        assertFalse(fields.any { it == ByteArray::class.java })
    }

    @Test fun `routing provenance is bounded and diagnostics contain no bytes transcript credential or sentinel`() = runTest {
        val result = assertIs<GovernedAcquisitionExecutionResult.Admitted>(
            coordinator(allExecutors(Counters())).execute(owner, source(), AUTHORISED),
        )
        val rendered = result.toString()
        listOf("%PDF-synthetic", "Base64", "Authorization", "credential", "transcript-sentinel").forEach {
            assertFalse(rendered.contains(it, ignoreCase = true))
        }
    }

    @Test fun `coordinator cannot construct trusted bytes fallback retry or analysis`() {
        val methods = GovernedAcquisitionExecutionCoordinator::class.java.declaredMethods.map { it.name.lowercase() }
        listOf("fallback", "retry", "analyse", "switchprovider").forEach { forbidden -> assertFalse(methods.any { forbidden in it }) }
        val fields = GovernedAcquisitionExecutionCoordinator::class.java.declaredFields.map { it.type.name }
        assertTrue(fields.any { it.contains("AuthoritativeAcquisitionSourceResolver") })
        assertFalse(fields.any { it.contains("Analysis") || it.contains("Memory") || it.contains("Knowledge") })
    }

    private fun coordinator(
        executors: List<BoundAcquisitionCapabilityExecutor>,
        registry: GovernedAcquisitionCapabilityRegistry = GovernedAcquisitionCapabilityRegistry(listOf(native(), local(), external())),
    ) = GovernedAcquisitionExecutionCoordinator(registry, DeterministicEvidenceAcquisitionRouter(), custodian(), executors)

    private fun allExecutors(c: Counters) = listOf(nativeExec(c), localExec(c), externalExec(c))
    private fun nativeExec(c: Counters) = countingExecutor(native(), c) { c.native++; admitted("native-gen") }
    private fun localExec(c: Counters) = countingExecutor(local(), c) { c.local++; admitted("local-gen") }
    private fun externalExec(c: Counters) = countingExecutor(external(), c) { c.external++; admitted("external-gen") }

    private fun countingExecutor(
        capability: EvidenceAcquisitionCapability,
        counters: Counters,
        action: suspend () -> BoundAcquisitionExecutorOutcome,
    ) = object : BoundAcquisitionCapabilityExecutor {
        override val binding = AcquisitionExecutorBinding(capability.capabilityId, capability.mechanism,
            capability.providerConfiguration?.configurationIdentity)
        override suspend fun execute(request: GovernedAcquisitionExecutionRequest): BoundAcquisitionExecutorOutcome {
            assertEquals(request.decision.source.evidenceArtifactId, request.authoritativeSource.evidenceArtifactId)
            return action()
        }
    }

    private fun admitted(id: String = "generation") =
        BoundAcquisitionExecutorOutcome.Admitted(DerivativeGenerationId(id))
    private fun failed(result: GovernedAcquisitionExecutionResult) = assertIs<GovernedAcquisitionExecutionResult.Failed>(result)

    private fun custodian() = object : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact) = EvidenceAcceptanceResult.Rejected("unused")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) = EvidenceRetrievalResult.Found(id, bytes)
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId) =
            EvidenceManifestRetrievalResult.Found(EvidenceSourceManifest(id, sha, bytes.size.toLong(), "application/pdf"))
    }

    private fun source(
        native: AcquisitionCharacteristicState = ABSENT,
        image: AcquisitionCharacteristicState = PRESENT,
        handwriting: AcquisitionCharacteristicState = ABSENT,
    ) = AcquisitionSource(id, sha, bytes.size.toLong(), "application/pdf", AcquisitionPageCount.Known(1),
        AcquisitionSourceCharacteristics(native, image, ABSENT, handwriting, ABSENT, ABSENT), HumanAuthorisedCustody.CONFIRMED)

    private fun native() = EvidenceAcquisitionCapability("native", DIRECT_NATIVE_EXTRACTION, setOf("application/pdf"),
        setOf(NATIVE_SEARCHABLE), fidelity(native = true, ocr = false), setOf(AUTHORITATIVE), LOCAL_ONLY, null,
        AcquisitionAvailability.Available, AcquisitionOperationalLimits())
    private fun local() = EvidenceAcquisitionCapability("local", LOCAL_OCR, setOf("application/pdf"),
        setOf(IMAGE_ONLY, MIXED), fidelity(), setOf(AUTHORITATIVE), LOCAL_ONLY, null,
        AcquisitionAvailability.Available, AcquisitionOperationalLimits())
    private fun external(id: String = "external", availability: AcquisitionAvailability = AcquisitionAvailability.Available) =
        EvidenceAcquisitionCapability(id, EXTERNAL_VISION_TRANSCRIPTION, setOf("application/pdf"), AcquisitionSourceForm.entries.toSet(),
            fidelity(handwriting = true), setOf(AUTHORITATIVE), EXTERNAL,
            AcquisitionProviderConfiguration("provider", "fixed", "profile-$id", "config-$id", "b".repeat(64),
                "c".repeat(64), "adapter", "1", "external-transcription.direct-byte-exact-v1"), availability,
            AcquisitionOperationalLimits())
    private fun fidelity(native: Boolean = false, ocr: Boolean = true, handwriting: Boolean = false) =
        AcquisitionFidelityCapabilities(true, native, ocr, handwriting, false, false, true, false, true, false)
    private fun digest(value: ByteArray) = MessageDigest.getInstance("SHA-256").digest(value)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    private class Counters(var native: Int = 0, var local: Int = 0, var external: Int = 0) { fun total() = native + local + external }

    private companion object {
        val PRESENT = AcquisitionCharacteristicState.PRESENT; val ABSENT = AcquisitionCharacteristicState.ABSENT; val UNKNOWN = AcquisitionCharacteristicState.UNKNOWN
        val AUTHORISED = ExternalEgressAuthorisation.AUTHORISED; val NOT_AUTHORISED = ExternalEgressAuthorisation.NOT_AUTHORISED
        val DIRECT_NATIVE_EXTRACTION = EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION
        val LOCAL_OCR = EvidenceAcquisitionMechanism.LOCAL_OCR; val EXTERNAL_VISION_TRANSCRIPTION = EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION
        val NATIVE_SEARCHABLE = AcquisitionSourceForm.NATIVE_SEARCHABLE; val IMAGE_ONLY = AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED; val MIXED = AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE
        val AUTHORITATIVE = AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY
        val LOCAL_ONLY = AcquisitionEgress.LOCAL_ONLY; val EXTERNAL = AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED
        val ROUTING_NO_ELIGIBLE_CAPABILITY = AcquisitionExecutionFailureReason.ROUTING_NO_ELIGIBLE_CAPABILITY
        val ROUTING_INDETERMINATE = AcquisitionExecutionFailureReason.ROUTING_INDETERMINATE
        val ROUTING_AMBIGUOUS = AcquisitionExecutionFailureReason.ROUTING_AMBIGUOUS
        val SELECTED_CAPABILITY_UNAVAILABLE = AcquisitionExecutionFailureReason.SELECTED_CAPABILITY_UNAVAILABLE
        val CAPABILITY_BINDING_MISMATCH = AcquisitionExecutionFailureReason.CAPABILITY_BINDING_MISMATCH
        val SOURCE_BINDING_MISMATCH = AcquisitionExecutionFailureReason.SOURCE_BINDING_MISMATCH
        val ACQUISITION_EXECUTION_FAILED = AcquisitionExecutionFailureReason.ACQUISITION_EXECUTION_FAILED
        val DERIVATIVE_ADMISSION_FAILED = AcquisitionExecutionFailureReason.DERIVATIVE_ADMISSION_FAILED
    }
}
