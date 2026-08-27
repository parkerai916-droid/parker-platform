package parker.core.interfaces

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EvidenceAcquisitionTest {
    private val sha = "a".repeat(64)
    private val characteristics = AcquisitionSourceCharacteristics(
        nativeSearchableText = AcquisitionCharacteristicState.ABSENT,
        imageOnlyOrScanned = AcquisitionCharacteristicState.PRESENT,
        mixedTextAndImage = AcquisitionCharacteristicState.ABSENT,
        handwriting = AcquisitionCharacteristicState.ABSENT,
        complexLayout = AcquisitionCharacteristicState.ABSENT,
        tables = AcquisitionCharacteristicState.ABSENT,
    )
    private val fidelity = AcquisitionFidelityCapabilities(
        literalTranscription = true,
        nativeTextExtraction = false,
        ocr = true,
        handwriting = false,
        layoutAware = false,
        tableAware = false,
        pageAssociation = true,
        regionAssociation = true,
        uncertaintyReporting = true,
        structuredOutput = true,
    )

    private fun source(
        mediaType: String = "application/pdf",
        byteLength: Long = 100,
        pageCount: AcquisitionPageCount = AcquisitionPageCount.Known(2),
        traits: AcquisitionSourceCharacteristics = characteristics,
        custody: HumanAuthorisedCustody = HumanAuthorisedCustody.CONFIRMED,
    ) = AcquisitionSource(EvidenceArtifactId("evidence-fa2"), sha, byteLength, mediaType, pageCount, traits, custody)

    private fun localCapability(
        availability: AcquisitionAvailability = AcquisitionAvailability.Available,
        limits: AcquisitionOperationalLimits = AcquisitionOperationalLimits(maximumSourceBytes = 1_000, maximumPages = 10),
    ) = EvidenceAcquisitionCapability(
        capabilityId = "local-ocr-v1",
        mechanism = EvidenceAcquisitionMechanism.LOCAL_OCR,
        supportedMediaTypes = setOf("application/pdf", "image/png"),
        supportedSourceForms = setOf(AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED, AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE),
        fidelity = fidelity,
        supportedRepresentations = setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
        egress = AcquisitionEgress.LOCAL_ONLY,
        providerConfiguration = null,
        availability = availability,
        limits = limits,
    )

    private fun externalCapability(availability: AcquisitionAvailability = AcquisitionAvailability.Available) =
        EvidenceAcquisitionCapability(
            capabilityId = "external-vision-v1",
            mechanism = EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION,
            supportedMediaTypes = setOf("application/pdf", "image/png"),
            supportedSourceForms = AcquisitionSourceForm.entries.toSet(),
            fidelity = fidelity.copy(handwriting = true, layoutAware = true, tableAware = true),
            supportedRepresentations = setOf(
                AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY,
                AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION,
            ),
            egress = AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED,
            providerConfiguration = AcquisitionProviderConfiguration(
                "provider", "fixed-model", "profile-v1", "config-v1", "b".repeat(64), "c".repeat(64),
                "adapter", "1", "processing-v1",
            ),
            availability = availability,
            limits = AcquisitionOperationalLimits(1_000, 10),
        )

    @Test
    fun `A capability descriptions distinguish direct local and external mechanisms`() {
        val local = localCapability()
        val external = externalCapability()
        val native = EvidenceAcquisitionCapability(
            "native", EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION, setOf("application/pdf"),
            setOf(AcquisitionSourceForm.NATIVE_SEARCHABLE), fidelity.copy(nativeTextExtraction = true, ocr = false),
            setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
            AcquisitionEgress.LOCAL_ONLY, null, AcquisitionAvailability.Available, AcquisitionOperationalLimits(),
        )
        assertEquals(EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION, native.mechanism)
        assertEquals(null, native.providerConfiguration)
        assertEquals(EvidenceAcquisitionMechanism.LOCAL_OCR, local.mechanism)
        assertEquals(AcquisitionEgress.LOCAL_ONLY, local.egress)
        assertEquals(EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION, external.mechanism)
        assertEquals(AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED, external.egress)
        assertTrue(external.fidelity.handwriting)
    }

    @Test
    fun `B contradictory capability combinations are rejected`() {
        assertFailsWith<IllegalArgumentException> {
            EvidenceAcquisitionCapability("bad", EvidenceAcquisitionMechanism.LOCAL_OCR, setOf("image/png"), setOf(AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED), fidelity,
                setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
                AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED, externalCapability().providerConfiguration,
                AcquisitionAvailability.Available, AcquisitionOperationalLimits())
        }
        assertFailsWith<IllegalArgumentException> {
            EvidenceAcquisitionCapability("bad", EvidenceAcquisitionMechanism.EXTERNAL_TRANSCRIPTION, setOf("image/png"), setOf(AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED), fidelity,
                setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
                AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED, null, AcquisitionAvailability.Available, AcquisitionOperationalLimits())
        }
    }

    @Test
    fun `C eligibility is deterministic and uses bounded reasons`() {
        val first = EvidenceAcquisitionEligibilityEvaluator.evaluate(localCapability(), source(), ExternalEgressAuthorisation.NOT_REQUIRED)
        val second = EvidenceAcquisitionEligibilityEvaluator.evaluate(localCapability(), source(), ExternalEgressAuthorisation.NOT_REQUIRED)
        assertEquals(first, second)
        assertEquals(AcquisitionEligibility.Eligible, first)

        val oversized = assertIs<AcquisitionEligibility.Ineligible>(
            EvidenceAcquisitionEligibilityEvaluator.evaluate(localCapability(), source(byteLength = 1_001), ExternalEgressAuthorisation.NOT_REQUIRED),
        )
        assertEquals(setOf(AcquisitionEligibilityReason.SOURCE_TOO_LARGE), oversized.reasons)

        val unsupported = assertIs<AcquisitionEligibility.Ineligible>(
            EvidenceAcquisitionEligibilityEvaluator.evaluate(localCapability(), source(mediaType = "text/plain"), ExternalEgressAuthorisation.NOT_REQUIRED),
        )
        assertTrue(AcquisitionEligibilityReason.UNSUPPORTED_MEDIA_TYPE in unsupported.reasons)

        val handwriting = characteristics.copy(handwriting = AcquisitionCharacteristicState.PRESENT)
        val handwritingResult = assertIs<AcquisitionEligibility.Ineligible>(
            EvidenceAcquisitionEligibilityEvaluator.evaluate(localCapability(), source(traits = handwriting), ExternalEgressAuthorisation.NOT_REQUIRED),
        )
        assertTrue(AcquisitionEligibilityReason.HANDWRITING_UNSUPPORTED in handwritingResult.reasons)
    }

    @Test
    fun `D unknown source facts remain indeterminate rather than inferred`() {
        val direct = EvidenceAcquisitionCapability(
            "native-v1", EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION, setOf("application/pdf"), setOf(AcquisitionSourceForm.NATIVE_SEARCHABLE),
            fidelity.copy(nativeTextExtraction = true, ocr = false),
            setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
            AcquisitionEgress.LOCAL_ONLY, null, AcquisitionAvailability.Available, AcquisitionOperationalLimits(),
        )
        val unknown = characteristics.copy(
            nativeSearchableText = AcquisitionCharacteristicState.UNKNOWN,
            imageOnlyOrScanned = AcquisitionCharacteristicState.ABSENT,
        )
        assertEquals(
            AcquisitionEligibility.Indeterminate(setOf(AcquisitionEligibilityReason.NATIVE_TEXT_CHARACTERISTIC_UNKNOWN)),
            EvidenceAcquisitionEligibilityEvaluator.evaluate(direct, source(traits = unknown), ExternalEgressAuthorisation.NOT_REQUIRED),
        )
    }

    @Test
    fun `E unaccepted external profile cannot be eligible and egress needs explicit authorisation`() {
        val unavailable = externalCapability(
            AcquisitionAvailability.Unavailable(AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED),
        )
        val result = assertIs<AcquisitionEligibility.Ineligible>(
            EvidenceAcquisitionEligibilityEvaluator.evaluate(unavailable, source(), ExternalEgressAuthorisation.NOT_AUTHORISED),
        )
        assertTrue(AcquisitionEligibilityReason.CONFIGURATION_NOT_ACCEPTED in result.reasons)
        assertTrue(AcquisitionEligibilityReason.EXTERNAL_EGRESS_NOT_AUTHORISED in result.reasons)
    }

    @Test
    fun `F routing decision binds exact source capability configuration and representation`() {
        val capability = externalCapability()
        val evidence = source()
        val decision = EvidenceAcquisitionRoutingDecision(
            evidence, capability, AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION,
            setOf(AcquisitionSelectionReason.SOURCE_CHARACTERISTICS_SUPPORTED, AcquisitionSelectionReason.GOVERNED_CONFIGURATION_ELIGIBLE),
        )
        assertEquals(evidence.evidenceArtifactId, decision.source.evidenceArtifactId)
        assertEquals(sha, decision.source.sha256)
        assertEquals("external-vision-v1", decision.capability.capabilityId)
        assertEquals("config-v1", decision.capability.providerConfiguration?.configurationIdentity)
        assertFailsWith<IllegalArgumentException> {
            EvidenceAcquisitionRoutingDecision(evidence, localCapability(),
                AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION,
                setOf(AcquisitionSelectionReason.SOURCE_CHARACTERISTICS_SUPPORTED))
        }
    }

    @Test
    fun `G public contracts contain no execution retry fallback ranking or quality score API`() {
        val types = listOf(EvidenceAcquisitionCapability::class.java, EvidenceAcquisitionRoutingDecision::class.java,
            EvidenceAcquisitionEligibilityEvaluator::class.java)
        val names = types.flatMap { type -> type.declaredMethods.map { it.name.lowercase() } }
        listOf("execute", "retry", "fallback", "switchprovider", "rank", "qualityscore").forEach { forbidden ->
            assertFalse(names.any { forbidden in it }, "Forbidden FA-2 authority found: $forbidden")
        }
    }

    @Test
    fun `H evaluating and recording contracts causes no acquisition OCR evidence or analysis invocation`() {
        var invocations = 0
        val result = EvidenceAcquisitionEligibilityEvaluator.evaluate(localCapability(), source(), ExternalEgressAuthorisation.NOT_REQUIRED)
        EvidenceAcquisitionRoutingDecision(source(), localCapability(),
            AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY,
            setOf(AcquisitionSelectionReason.WITHIN_OPERATIONAL_LIMITS))
        assertEquals(AcquisitionEligibility.Eligible, result)
        assertEquals(0, invocations)
    }

    @Test
    fun `B source characteristics preserve known and unknown facts and reject invalid source facts`() {
        val facts = characteristics.copy(
            nativeSearchableText = AcquisitionCharacteristicState.UNKNOWN,
            tables = AcquisitionCharacteristicState.PRESENT,
        )
        val value = source(traits = facts, pageCount = AcquisitionPageCount.Unknown)
        assertEquals(AcquisitionCharacteristicState.UNKNOWN, value.characteristics.nativeSearchableText)
        assertEquals(AcquisitionCharacteristicState.PRESENT, value.characteristics.tables)
        assertEquals(AcquisitionPageCount.Unknown, value.pageCount)
        assertFailsWith<IllegalArgumentException> {
            AcquisitionSource(EvidenceArtifactId("e"), "bad", 1, "application/pdf", AcquisitionPageCount.Unknown, facts, HumanAuthorisedCustody.CONFIRMED)
        }
        assertFailsWith<IllegalArgumentException> {
            AcquisitionSource(EvidenceArtifactId("e"), sha, -1, "application/pdf", AcquisitionPageCount.Unknown, facts, HumanAuthorisedCustody.CONFIRMED)
        }
        assertFailsWith<IllegalArgumentException> {
            AcquisitionSource(EvidenceArtifactId("e"), sha, 1, " ", AcquisitionPageCount.Unknown, facts, HumanAuthorisedCustody.CONFIRMED)
        }
        val forbiddenNames = AcquisitionSourceCharacteristics::class.java.declaredFields.map { it.name.lowercase() }
        listOf("relevance", "credibility", "probative", "legal", "meaning").forEach { forbidden ->
            assertFalse(forbiddenNames.any { forbidden in it })
        }
    }

    @Test
    fun `D provider-neutral external capability contains no OpenAI construction requirement`() {
        val capability = externalCapability()
        assertEquals("provider", capability.providerConfiguration?.providerIdentity)
        assertFalse(capability.javaClass.name.lowercase().contains("openai"))
    }

    @Test
    fun `E processing lineage admits only authoritative or directly derived representations`() {
        assertEquals(
            setOf("AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY", "DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION"),
            AcquisitionRepresentationClass.entries.map { it.name }.toSet(),
        )
        assertFalse(AcquisitionRepresentationClass.entries.any {
            it.name.contains("OCR_OUTPUT") || it.name.contains("TRANSCRIPTION_OUTPUT") || it.name.contains("DERIVATIVE_GENERATION")
        })
    }

    @Test
    fun `G contracts are structurally isolated from substantive platform domains`() {
        val sourceText = java.io.File("src/interfaces/EvidenceAcquisition.kt").readText()
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            .lines().joinToString("\n") { it.substringBefore("//") }
        listOf("MemoryCore", "Knowledge", "Analysis", "Reasoning", "Conversation", "SavedAnalysis", "Rks").forEach { forbidden ->
            assertFalse(Regex("\\b$forbidden\\b", RegexOption.IGNORE_CASE).containsMatchIn(sourceText),
                "FA.2 contract source must not depend on $forbidden")
        }
    }

    @Test
    fun `capability collection inputs are defensively copied`() {
        val media = mutableSetOf("application/pdf")
        val representations = mutableSetOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY)
        val forms = mutableSetOf(AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED)
        val capability = EvidenceAcquisitionCapability("local", EvidenceAcquisitionMechanism.LOCAL_OCR, media, forms, fidelity,
            representations, AcquisitionEgress.LOCAL_ONLY, null, AcquisitionAvailability.Available, AcquisitionOperationalLimits())
        media.clear()
        forms.clear()
        representations.clear()
        assertEquals(setOf("application/pdf"), capability.supportedMediaTypes)
        assertEquals(setOf(AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED), capability.supportedSourceForms)
        assertEquals(setOf(AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY), capability.supportedRepresentations)
    }
}
