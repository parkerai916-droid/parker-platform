package parker.core.runtime

import kotlin.test.*
import parker.core.interfaces.*

class DeterministicEvidenceAcquisitionRouterTest {
    private val router = DeterministicEvidenceAcquisitionRouter()
    private val sha = "a".repeat(64)

    @Test fun `born-digital native source selects direct extraction over local and external OCR`() {
        val result = selected(route(source(native = PRESENT, image = ABSENT), listOf(native(), local(), external())))
        assertEquals(DIRECT_NATIVE_EXTRACTION, result.capability.mechanism)
        assertTrue(NATIVE_TEXT_DIRECTLY_AVAILABLE in result.selectionReasons)
    }

    @Test fun `clean scanned local and external equivalence avoids unnecessary egress`() {
        val result = selected(route(source(), listOf(external(), local())))
        assertEquals("local", result.capability.capabilityId)
        assertTrue(AVOIDED_UNNECESSARY_EXTERNAL_EGRESS in result.selectionReasons)
    }

    @Test fun `handwriting selects capable external over incapable local`() {
        val result = selected(route(source(handwriting = PRESENT), listOf(local(), external())))
        assertEquals("external", result.capability.capabilityId)
        assertTrue(REQUIRED_HANDWRITING_SUPPORT in result.selectionReasons)
    }

    @Test fun `handwriting with prohibited egress does not select inadequate local`() {
        assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(
            route(source(handwriting = PRESENT), listOf(local(), external()), NOT_AUTHORISED),
        )
    }

    @Test fun `unknown handwriting remains indeterminate when capability choice depends on it`() {
        assertIs<EvidenceAcquisitionRoutingOutcome.Indeterminate>(
            route(source(handwriting = UNKNOWN), listOf(local(), external())),
        )
    }

    @Test fun `complex layout selects the only layout-capable mechanism`() {
        val result = selected(route(source(layout = PRESENT), listOf(local(), external())))
        assertEquals("external", result.capability.capabilityId)
        assertTrue(REQUIRED_LAYOUT_SUPPORT in result.selectionReasons)
    }

    @Test fun `table requirement selects the only table-capable mechanism`() {
        val result = selected(route(source(tables = PRESENT), listOf(local(), external())))
        assertEquals("external", result.capability.capabilityId)
        assertTrue(REQUIRED_TABLE_SUPPORT in result.selectionReasons)
    }

    @Test fun `equivalent external capabilities are ambiguous and provider name cannot break tie`() {
        val z = external("z-provider", provider = "Zulu")
        val a = external("a-provider", provider = "Alpha")
        val result = assertIs<EvidenceAcquisitionRoutingOutcome.Ambiguous>(route(source(), listOf(z, a)))
        assertEquals(setOf("z-provider", "a-provider"), result.capabilityIds)
        assertEquals(setOf(AcquisitionNoSelectionReason.EQUIVALENT_CAPABILITIES_AMBIGUOUS), result.reasons)
    }

    @Test fun `all candidate permutations and repeated evaluations produce equal result`() {
        val candidates = listOf(native(), local(), external())
        val source = source(native = PRESENT, image = ABSENT)
        val expected = route(source, candidates)
        permutations(candidates).forEach { assertEquals(expected, route(source, it)) }
        assertEquals(expected, route(source, candidates))
    }

    @Test fun `acceptance pending suspended and disabled external projections are excluded`() {
        listOf(
            AcquisitionAvailabilityReason.CONFIGURATION_NOT_ACCEPTED,
            AcquisitionAvailabilityReason.CONFIGURATION_NOT_READY,
            AcquisitionAvailabilityReason.DISABLED,
        ).forEach { reason ->
            val result = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(
                route(source(), listOf(external(availability = AcquisitionAvailability.Unavailable(reason)))),
            )
            assertTrue(AcquisitionNoSelectionReason.CAPABILITY_DISABLED_OR_NOT_READY in result.reasons)
        }
    }

    @Test fun `source byte and page limits exclude bounded candidates`() {
        val byteResult = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(
            route(source(bytes = 101), listOf(local(maxBytes = 100))),
        )
        assertTrue(AcquisitionNoSelectionReason.OPERATIONAL_LIMIT_EXCEEDED in byteResult.reasons)
        val pageResult = assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(
            route(source(pages = AcquisitionPageCount.Known(3)), listOf(local(maxPages = 2))),
        )
        assertTrue(AcquisitionNoSelectionReason.OPERATIONAL_LIMIT_EXCEEDED in pageResult.reasons)
    }

    @Test fun `unknown page count is indeterminate when capability has a page bound`() {
        assertIs<EvidenceAcquisitionRoutingOutcome.Indeterminate>(
            route(source(pages = AcquisitionPageCount.Unknown), listOf(local(maxPages = 2))),
        )
    }

    @Test fun `native text unknown is indeterminate when native extraction could be selected`() {
        assertIs<EvidenceAcquisitionRoutingOutcome.Indeterminate>(
            route(source(native = UNKNOWN, image = ABSENT), listOf(native(), local(), external())),
        )
    }

    @Test fun `no OCR capability produces no eligible capability`() {
        assertIs<EvidenceAcquisitionRoutingOutcome.NoEligibleCapability>(route(source(), listOf(native())))
    }

    @Test fun `accepted external-only OCR may be selected when egress is authorised`() {
        assertEquals("external", selected(route(source(), listOf(external()))).capability.capabilityId)
    }

    @Test fun `equivalent local capability avoids unnecessary transformation`() {
        val direct = local("direct-representation")
        val transformed = local("transformed-representation", representations = setOf(DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION))
        val result = selected(route(source(), listOf(transformed, direct)))
        assertEquals("direct-representation", result.capability.capabilityId)
        assertTrue(AVOIDED_UNNECESSARY_TRANSFORMATION in result.selectionReasons)
    }

    @Test fun `selected decision binds exact source capability representation egress and configuration`() {
        val source = source(handwriting = PRESENT)
        val decision = selected(route(source, listOf(external())))
        assertEquals(source, decision.source)
        assertEquals("external", decision.capability.capabilityId)
        assertEquals("config-external", decision.capability.providerConfiguration?.configurationIdentity)
        assertEquals(EXTERNAL_EGRESS_REQUIRED, decision.capability.egress)
        assertEquals(AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY, decision.selectedRepresentation)
        assertTrue(decision.selectionReasons.isNotEmpty())
    }

    @Test fun `unknown facts are harmless only when every survivor safely covers them`() {
        val allCover = listOf(external("one"), external("two"))
        assertIs<EvidenceAcquisitionRoutingOutcome.Ambiguous>(route(source(handwriting = UNKNOWN), allCover))
    }

    @Test fun `router has no quality score enum provider ordering case semantics or execution surface`() {
        val sourceText = java.io.File("src/runtime/DeterministicEvidenceAcquisitionRouter.kt").readText()
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        listOf("qualityScore", "providerRank", "ordinal", "sortedBy", "random", "UUID", "currentTime",
            "filename", "title", "credibility", "relevance", "legal", "party").forEach {
            assertFalse(Regex("\\b$it\\b", RegexOption.IGNORE_CASE).containsMatchIn(sourceText), it)
        }
        val methods = DeterministicEvidenceAcquisitionRouter::class.java.declaredMethods.map { it.name.lowercase() }
        listOf("execute", "extract", "recognise", "transcribe", "admit", "analyse", "retry", "fallback").forEach { forbidden ->
            assertFalse(methods.any { forbidden in it }, forbidden)
        }
    }

    @Test fun `router receives no bytes and has no execution or platform subsystem dependencies`() {
        assertTrue(DeterministicEvidenceAcquisitionRouter::class.java.declaredFields.isEmpty())
        val types = DeterministicEvidenceAcquisitionRouter::class.java.declaredMethods.flatMap {
            it.parameterTypes.toList() + it.returnType
        }
        assertFalse(types.any { it == ByteArray::class.java })
        val forbidden = listOf("EvidenceCustodian", "OcrMechanism", "ExternalTranscription", "Transport", "Analysis",
            "Memory", "Knowledge", "Rks", "Conversation", "OwnerEvidenceHttp")
        types.forEach { type -> forbidden.forEach { assertFalse(type.name.contains(it), type.name) } }
    }

    @Test fun `routing construction and evaluation invoke no provider OCR derivative or analysis`() {
        var provider = 0; var ocr = 0; var derivative = 0; var analysis = 0
        val result = route(source(), listOf(local(), external()))
        assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(result)
        assertEquals(listOf(0, 0, 0, 0), listOf(provider, ocr, derivative, analysis))
    }

    private fun route(
        source: AcquisitionSource,
        capabilities: List<EvidenceAcquisitionCapability>,
        egress: ExternalEgressAuthorisation = AUTHORISED,
    ) = router.route(source, capabilities, egress)

    private fun selected(outcome: EvidenceAcquisitionRoutingOutcome) =
        assertIs<EvidenceAcquisitionRoutingOutcome.Selected>(outcome).decision

    private fun source(
        native: AcquisitionCharacteristicState = ABSENT,
        image: AcquisitionCharacteristicState = PRESENT,
        mixed: AcquisitionCharacteristicState = ABSENT,
        handwriting: AcquisitionCharacteristicState = ABSENT,
        layout: AcquisitionCharacteristicState = ABSENT,
        tables: AcquisitionCharacteristicState = ABSENT,
        bytes: Long = 50,
        pages: AcquisitionPageCount = AcquisitionPageCount.Known(1),
    ) = AcquisitionSource(EvidenceArtifactId("synthetic-source"), sha, bytes, "application/pdf", pages,
        AcquisitionSourceCharacteristics(native, image, mixed, handwriting, layout, tables), HumanAuthorisedCustody.CONFIRMED)

    private fun native(id: String = "native") = EvidenceAcquisitionCapability(
        id, DIRECT_NATIVE_EXTRACTION, setOf("application/pdf"), setOf(NATIVE_SEARCHABLE),
        fidelity(native = true, ocr = false), setOf(AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
        LOCAL_ONLY, null, AcquisitionAvailability.Available, AcquisitionOperationalLimits(1_000, null),
    )

    private fun local(
        id: String = "local",
        maxBytes: Long? = 1_000,
        maxPages: Int? = null,
        representations: Set<AcquisitionRepresentationClass> = setOf(AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
    ) = EvidenceAcquisitionCapability(
        id, LOCAL_OCR, setOf("application/pdf"), setOf(IMAGE_ONLY_OR_SCANNED, MIXED_TEXT_AND_IMAGE),
        fidelity(), representations, LOCAL_ONLY, null, AcquisitionAvailability.Available,
        AcquisitionOperationalLimits(maxBytes, maxPages),
    )

    private fun external(
        id: String = "external",
        provider: String = "provider",
        availability: AcquisitionAvailability = AcquisitionAvailability.Available,
    ) = EvidenceAcquisitionCapability(
        id, EXTERNAL_VISION_TRANSCRIPTION, setOf("application/pdf"), AcquisitionSourceForm.entries.toSet(),
        fidelity(handwriting = true, layout = true, tables = true), setOf(AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY),
        EXTERNAL_EGRESS_REQUIRED,
        AcquisitionProviderConfiguration(provider, "fixed", "profile-$id", "config-$id", "b".repeat(64),
            "c".repeat(64), "adapter", "1", "processing"),
        availability, AcquisitionOperationalLimits(1_000, null),
    )

    private fun fidelity(
        native: Boolean = false,
        ocr: Boolean = true,
        handwriting: Boolean = false,
        layout: Boolean = false,
        tables: Boolean = false,
    ) = AcquisitionFidelityCapabilities(true, native, ocr, handwriting, layout, tables,
        pageAssociation = true, regionAssociation = layout, uncertaintyReporting = true, structuredOutput = tables)

    private fun <T> permutations(values: List<T>): List<List<T>> =
        if (values.size <= 1) listOf(values) else values.indices.flatMap { index ->
            permutations(values.filterIndexed { candidate, _ -> candidate != index }).map { listOf(values[index]) + it }
        }

    private companion object {
        val PRESENT = AcquisitionCharacteristicState.PRESENT
        val ABSENT = AcquisitionCharacteristicState.ABSENT
        val UNKNOWN = AcquisitionCharacteristicState.UNKNOWN
        val DIRECT_NATIVE_EXTRACTION = EvidenceAcquisitionMechanism.DIRECT_NATIVE_EXTRACTION
        val LOCAL_OCR = EvidenceAcquisitionMechanism.LOCAL_OCR
        val EXTERNAL_VISION_TRANSCRIPTION = EvidenceAcquisitionMechanism.EXTERNAL_VISION_TRANSCRIPTION
        val NATIVE_SEARCHABLE = AcquisitionSourceForm.NATIVE_SEARCHABLE
        val IMAGE_ONLY_OR_SCANNED = AcquisitionSourceForm.IMAGE_ONLY_OR_SCANNED
        val MIXED_TEXT_AND_IMAGE = AcquisitionSourceForm.MIXED_TEXT_AND_IMAGE
        val AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY = AcquisitionRepresentationClass.AUTHORITATIVE_SOURCE_OR_BYTE_EXACT_COPY
        val DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION = AcquisitionRepresentationClass.DIRECTLY_DERIVED_TRANSFORMED_REPRESENTATION
        val LOCAL_ONLY = AcquisitionEgress.LOCAL_ONLY
        val EXTERNAL_EGRESS_REQUIRED = AcquisitionEgress.EXTERNAL_EGRESS_REQUIRED
        val AUTHORISED = ExternalEgressAuthorisation.AUTHORISED
        val NOT_AUTHORISED = ExternalEgressAuthorisation.NOT_AUTHORISED
        val NATIVE_TEXT_DIRECTLY_AVAILABLE = AcquisitionSelectionReason.NATIVE_TEXT_DIRECTLY_AVAILABLE
        val REQUIRED_HANDWRITING_SUPPORT = AcquisitionSelectionReason.REQUIRED_HANDWRITING_SUPPORT
        val REQUIRED_LAYOUT_SUPPORT = AcquisitionSelectionReason.REQUIRED_LAYOUT_SUPPORT
        val REQUIRED_TABLE_SUPPORT = AcquisitionSelectionReason.REQUIRED_TABLE_SUPPORT
        val AVOIDED_UNNECESSARY_TRANSFORMATION = AcquisitionSelectionReason.AVOIDED_UNNECESSARY_TRANSFORMATION
        val AVOIDED_UNNECESSARY_EXTERNAL_EGRESS = AcquisitionSelectionReason.AVOIDED_UNNECESSARY_EXTERNAL_EGRESS
    }
}
