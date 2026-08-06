package parker.core.interfaces

import java.security.MessageDigest
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.runtime.OcrExecutionSequencer

/**
 * OCR Mechanism, Implementation Unit 10 ("Evidence-Intelligence-Side
 * Contract Sufficiency"). Governed in full by
 * `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract
 * Design"); by `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the
 * Scope Lock"); by `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 10; and by
 * `docs/architecture/EVIDENCE_INTELLIGENCE_CONTRACT_DESIGN.md` ("the
 * Evidence Intelligence Contract Design"), Amendment 2 (the OCR
 * dependency), Section 5 (Outputs), Section 8 (Provenance Model), Section
 * 9 (Confidence Model), and Section 11 (Failure Model).
 *
 * **This Unit adds no production file.** Its own Purpose is explicit:
 * "Confirm, via hand-written fakes only, that a future Evidence-
 * Intelligence-side caller could invoke Units 1-9's own pipeline
 * successfully -- without building that caller, its permission gate, or
 * its trigger logic." Its own Files-expected-to-change field is "New test
 * files only." This file is that hand-written fake and its exercising
 * tests -- mirroring exactly how `EvidenceExtractionCoordinatorTest.kt`
 * already exercised `EvidenceExtractor` from a caller's own perspective
 * before any real coordinator existed, and mirroring
 * `OcrProvenanceDisclosureTest.kt`'s own Unit 8 pattern of building a
 * *hypothetical* downstream artefact from OCR's own disclosure, entirely
 * in test code, never in production.
 *
 * **Pre-implementation finding: no omission exists.** Evidence
 * Intelligence's own Failure Model (Contract Design Section 11) already
 * establishes the controlling principle for every OCR outcome, restated
 * from its own identical treatment of a faulting `ReasoningProvider`
 * invocation: such a fault "is Evidence Intelligence's own concrete
 * implementation's concern to signal, by whatever mechanism it chooses,
 * outside the `EvidenceAnalysisResult` sealed type." Evidence
 * Intelligence therefore needs only to be able to *distinguish* each of
 * `OcrRecognitionOutcome`'s own nine variants -- not a new field, type,
 * or representation from OCR itself -- and Units 1, 6, 7, and 8 already
 * provide every fact this file's own tests below exercise: transcription
 * fidelity and mixed fidelity (Evidence Intelligence Contract Design
 * Section 5's own "which of the three it is -- or which portions are
 * which"), page alignment, confidence, warnings, mechanism identity and
 * version, processing timestamp, and provenance-sufficiency (already
 * proven once, from the pipeline's own side, by
 * `OcrProvenanceDisclosureTest.kt`; proven again here, from a caller's
 * own side, per Unit 10's own Verification requirements). No genuine
 * omission was found; no production file was modified as a result.
 */
class OcrEvidenceIntelligenceContractSufficiencyTest {

    // -- A hand-written fake standing in for "a future Evidence Intelligence caller" --
    // Never referenced from src/; exists only in this test file, exactly as Unit 10's own
    // Constitutional constraints require ("does not touch any real Evidence Intelligence source file").

    private class FakeOcrProviderAdapter(private val respond: suspend (OcrRecognitionRequest) -> OcrRecognitionOutcome) : OcrProviderAdapter {
        override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome = respond(request)
    }

    private fun fakeEvidenceIntelligenceCaller(outcome: OcrRecognitionOutcome): OcrExecutionSequencer =
        OcrExecutionSequencer(FakeOcrProviderAdapter { outcome })

    private fun sampleRequest(pageCount: Int? = null, sourceEvidenceId: String = "evidence-77") = OcrRecognitionRequest(
        sourceEvidenceId = EvidenceArtifactId(sourceEvidenceId),
        content = byteArrayOf(9, 8, 7, 6),
        mediaType = "image/png",
        pageCount = pageCount,
    )

    private fun sampleResult(
        text: String = "recognised text",
        fidelity: TranscriptionFidelity = TranscriptionFidelity.VERBATIM,
        confidence: Double? = 0.75,
        warnings: List<String> = emptyList(),
        segments: List<OcrRecognitionSegment> = emptyList(),
        recognisedAt: Instant = Instant.parse("2026-02-01T12:00:00Z"),
    ) = OcrRecognitionResult(
        recognisedText = text,
        fidelity = fidelity,
        identity = OcrRecognitionIdentity(mechanismIdentity = "mechanism-ei", configurationProfile = "profile-ei", mechanismVersion = "2.0.1"),
        confidence = confidence,
        recognisedAt = recognisedAt,
        warnings = warnings,
        segments = segments,
    )

    // -- Distinguish all fidelity levels --

    @Test
    fun `a fake Evidence Intelligence caller can distinguish all three transcription-fidelity levels from a Recognised outcome`() = runTest {
        TranscriptionFidelity.values().forEach { fidelity ->
            val sequencer = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(fidelity = fidelity)))

            val outcome = sequencer.recognise(sampleRequest())

            val observedFidelity = (outcome as OcrRecognitionOutcome.Recognised).result.fidelity
            assertEquals(fidelity, observedFidelity, "the caller must be able to recover the exact fidelity category from Unit 1's own field")
        }
    }

    // -- Distinguish mixed fidelity --

    @Test
    fun `a fake Evidence Intelligence caller can distinguish mixed-fidelity portions within a single recognition, per Evidence Intelligence Contract Design Section 5`() = runTest {
        val segments = listOf(
            OcrRecognitionSegment(text = "clean passage", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 1),
            OcrRecognitionSegment(text = "standardised passage", fidelity = TranscriptionFidelity.NORMALISED, pageNumber = 1),
            OcrRecognitionSegment(text = "reconstructed passage", fidelity = TranscriptionFidelity.INFERRED_RECONSTRUCTION, pageNumber = 2),
        )
        val sequencer = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(segments = segments)))

        val outcome = sequencer.recognise(sampleRequest(pageCount = 2)) as OcrRecognitionOutcome.Recognised

        val distinctFidelitiesObserved = outcome.result.segments.map { it.fidelity }.toSet()
        assertEquals(
            setOf(TranscriptionFidelity.VERBATIM, TranscriptionFidelity.NORMALISED, TranscriptionFidelity.INFERRED_RECONSTRUCTION),
            distinctFidelitiesObserved,
            "the caller must be able to see which portions are which, satisfying Evidence Intelligence Contract Design Section 5's own 'must make which of the three it is -- or which portions are which -- apparent'",
        )
    }

    // -- Distinguish page ordering --

    @Test
    fun `a fake Evidence Intelligence caller can read page ordering, in order, from a page-aligned recognition`() = runTest {
        val segments = listOf(
            OcrRecognitionSegment(text = "page one", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 1),
            OcrRecognitionSegment(text = "page two", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 2),
            OcrRecognitionSegment(text = "page three", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 3),
        )
        val sequencer = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(segments = segments)))

        val outcome = sequencer.recognise(sampleRequest(pageCount = 3)) as OcrRecognitionOutcome.Recognised

        assertEquals(listOf(1, 2, 3), outcome.result.segments.map { it.pageNumber }, "page ordering must reach the caller unchanged and in order")
    }

    // -- Distinguish unknown page ordering --

    @Test
    fun `a fake Evidence Intelligence caller can distinguish 'no page ordering available' from a genuine, populated page ordering -- never fabricated as page one`() = runTest {
        val sequencer = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(segments = emptyList())))

        val outcome = sequencer.recognise(sampleRequest(pageCount = null)) as OcrRecognitionOutcome.Recognised

        assertTrue(outcome.result.segments.isEmpty(), "an empty segments list must be readable by the caller as 'page ordering genuinely unavailable', never as page one of a real document")
    }

    // -- Distinguish every failure outcome --

    @Test
    fun `a fake Evidence Intelligence caller can exhaustively distinguish every one of the nine OcrRecognitionOutcome variants`() = runTest {
        val cases: List<Pair<String, OcrRecognitionOutcome>> = listOf(
            "Recognised" to OcrRecognitionOutcome.Recognised(sampleResult()),
            "Failed" to OcrRecognitionOutcome.Failed("generic failure"),
            "NotAuthorised" to OcrRecognitionOutcome.NotAuthorised("not authorised"),
            "UnsupportedOrInaccessibleInput" to OcrRecognitionOutcome.UnsupportedOrInaccessibleInput("unsupported input"),
            "NoRecognisableContent" to OcrRecognitionOutcome.NoRecognisableContent("no recognisable content"),
            "PartialOrDegradedOutput" to OcrRecognitionOutcome.PartialOrDegradedOutput(sampleResult(), "degraded"),
            "ValidationRejection" to OcrRecognitionOutcome.ValidationRejection("validation rejection"),
            "ProcessingOrDependencyFailure" to OcrRecognitionOutcome.ProcessingOrDependencyFailure("processing failure"),
            "GenuineImplementationFault" to OcrRecognitionOutcome.GenuineImplementationFault("implementation fault"),
        )

        cases.forEach { (label, outcome) ->
            val sequencer = fakeEvidenceIntelligenceCaller(outcome)
            val observed = sequencer.recognise(sampleRequest())

            // An exhaustive `when` with no `else` branch -- if a tenth variant existed, or if any two of
            // the nine collapsed into a shared representation, this block would fail to compile, not merely
            // fail at runtime. This is the caller's-eye-view proof that every distinction remains reachable.
            val classification: String = when (observed) {
                is OcrRecognitionOutcome.Recognised -> "Recognised"
                is OcrRecognitionOutcome.Failed -> "Failed"
                is OcrRecognitionOutcome.NotAuthorised -> "NotAuthorised"
                is OcrRecognitionOutcome.UnsupportedOrInaccessibleInput -> "UnsupportedOrInaccessibleInput"
                is OcrRecognitionOutcome.NoRecognisableContent -> "NoRecognisableContent"
                is OcrRecognitionOutcome.PartialOrDegradedOutput -> "PartialOrDegradedOutput"
                is OcrRecognitionOutcome.ValidationRejection -> "ValidationRejection"
                is OcrRecognitionOutcome.ProcessingOrDependencyFailure -> "ProcessingOrDependencyFailure"
                is OcrRecognitionOutcome.GenuineImplementationFault -> "GenuineImplementationFault"
            }
            assertEquals(label, classification, "the caller must classify each outcome correctly and distinctly")
        }
    }

    // -- Identify partial output --

    @Test
    fun `a fake Evidence Intelligence caller can identify partial output as distinct from a clean Recognised, while still reading the preserved partial text`() = runTest {
        val partial = sampleResult(text = "only the top half of the page was legible")
        val sequencer = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.PartialOrDegradedOutput(partial, "bottom half illegible"))

        val outcome = sequencer.recognise(sampleRequest())

        assertTrue(outcome is OcrRecognitionOutcome.PartialOrDegradedOutput, "the caller must identify partial/degraded output as its own distinct case")
        assertEquals("only the top half of the page was legible", outcome.partialResult.recognisedText)
    }

    // -- Determine whether recognised text exists --

    @Test
    fun `a fake Evidence Intelligence caller can determine whether recognised text exists at all, without fabricating output for outcomes that carry none`() = runTest {
        val withOutput: List<OcrRecognitionOutcome> = listOf(
            OcrRecognitionOutcome.Recognised(sampleResult()),
            OcrRecognitionOutcome.PartialOrDegradedOutput(sampleResult(), "degraded"),
        )
        val withoutOutput: List<OcrRecognitionOutcome> = listOf(
            OcrRecognitionOutcome.Failed("x"), OcrRecognitionOutcome.NotAuthorised("x"),
            OcrRecognitionOutcome.UnsupportedOrInaccessibleInput("x"), OcrRecognitionOutcome.NoRecognisableContent("x"),
            OcrRecognitionOutcome.ValidationRejection("x"), OcrRecognitionOutcome.ProcessingOrDependencyFailure("x"),
            OcrRecognitionOutcome.GenuineImplementationFault("x"),
        )

        withOutput.forEach { outcome ->
            val text: String? = when (outcome) {
                is OcrRecognitionOutcome.Recognised -> outcome.result.recognisedText
                is OcrRecognitionOutcome.PartialOrDegradedOutput -> outcome.partialResult.recognisedText
                else -> null
            }
            assertTrue(text != null, "${outcome::class.simpleName} must expose recognised text to the caller")
        }
        withoutOutput.forEach { outcome ->
            val text: String? = when (outcome) {
                is OcrRecognitionOutcome.Recognised -> outcome.result.recognisedText
                is OcrRecognitionOutcome.PartialOrDegradedOutput -> outcome.partialResult.recognisedText
                else -> null
            }
            assertNull(text, "${outcome::class.simpleName} must never let the caller fabricate recognised text where none exists")
        }
    }

    // -- Compute downstream provenance (mirrors OcrProvenanceDisclosureTest.kt's own Unit 8 proof, from the caller's own side) --

    @Test
    fun `a fake Evidence Intelligence caller can construct a hypothetical CandidateProvenance entirely from the request it itself holds and the result OCR discloses`() = runTest {
        val request = sampleRequest(sourceEvidenceId = "evidence-99")
        val sequencer = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult()))

        val outcome = sequencer.recognise(request) as OcrRecognitionOutcome.Recognised

        val provenance = CandidateProvenance(
            sourceIdentifier = request.sourceEvidenceId.value,
            sourceType = "ocr-recognition",
            acquisitionTime = outcome.result.recognisedAt,
            contentNature = ContentNature.EXTRACTED,
            creator = "${outcome.result.identity.mechanismIdentity} ${outcome.result.identity.mechanismVersion.orEmpty()}".trim(),
            confidence = outcome.result.confidence,
        )

        assertEquals("evidence-99", provenance.sourceIdentifier)
        assertEquals(outcome.result.recognisedAt, provenance.acquisitionTime)
    }

    // -- Compute downstream integrity hash --

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xFF) }

    @Test
    fun `a fake Evidence Intelligence caller can compute a deterministic downstream integrity hash from recognisedText alone`() = runTest {
        val sequencerA = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(text = "same content")))
        val sequencerB = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(text = "same content")))
        val sequencerC = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(text = "different content")))

        val textA = (sequencerA.recognise(sampleRequest()) as OcrRecognitionOutcome.Recognised).result.recognisedText
        val textB = (sequencerB.recognise(sampleRequest()) as OcrRecognitionOutcome.Recognised).result.recognisedText
        val textC = (sequencerC.recognise(sampleRequest()) as OcrRecognitionOutcome.Recognised).result.recognisedText

        val digestA = sha256Hex(textA.toByteArray(Charsets.UTF_8))
        val digestB = sha256Hex(textB.toByteArray(Charsets.UTF_8))
        val digestC = sha256Hex(textC.toByteArray(Charsets.UTF_8))

        assertEquals(digestA, digestB, "identical recognised text must produce the same downstream digest")
        assertNotEquals(digestA, digestC, "different recognised text must produce a different downstream digest")
    }

    // -- Perform downstream validation (a hypothetical Parker/Evidence-Intelligence-owned policy, never OCR's own) --

    /**
     * A stand-in for a *future*, Parker-owned validation policy -- not
     * authorised, defined, or fixed by this Unit or by OCR's own contract
     * (Scope Lock Section 11: "this Scope Lock... creates no validation
     * policy, threshold, or mechanism of any kind"). Exists only to prove
     * that OCR's own disclosure (confidence, fidelity, warnings) is
     * sufficient *raw material* for some future policy to be built from --
     * the specific threshold below is an arbitrary illustration, not a
     * governance decision, and is never referenced by any production file.
     */
    private fun hypotheticalDownstreamValidation(result: OcrRecognitionResult): Boolean =
        (result.confidence ?: 0.0) >= 0.5 && result.fidelity != TranscriptionFidelity.INFERRED_RECONSTRUCTION

    @Test
    fun `a fake Evidence Intelligence caller can perform a hypothetical downstream validation decision using only OCR's own disclosed confidence and fidelity`() = runTest {
        val highConfidence = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(confidence = 0.9, fidelity = TranscriptionFidelity.VERBATIM)))
        val lowConfidence = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(confidence = 0.1, fidelity = TranscriptionFidelity.VERBATIM)))

        val acceptedResult = (highConfidence.recognise(sampleRequest()) as OcrRecognitionOutcome.Recognised).result
        val rejectedResult = (lowConfidence.recognise(sampleRequest()) as OcrRecognitionOutcome.Recognised).result

        assertTrue(hypotheticalDownstreamValidation(acceptedResult))
        assertFalse(hypotheticalDownstreamValidation(rejectedResult))
    }

    // -- Perform downstream evidence registration (shape sufficiency only, never the real coordinator) --

    @Test
    fun `a fake Evidence Intelligence caller can construct a hypothetical CandidateEvidenceArtifact from recognisedText alone, the shape EvidenceRegistrationCoordinator_register would require`() = runTest {
        val sequencer = fakeEvidenceIntelligenceCaller(OcrRecognitionOutcome.Recognised(sampleResult(text = "text to be registered")))

        val outcome = sequencer.recognise(sampleRequest()) as OcrRecognitionOutcome.Recognised

        val candidateEvidenceArtifact = CandidateEvidenceArtifact(outcome.result.recognisedText.toByteArray(Charsets.UTF_8))

        assertTrue(candidateEvidenceArtifact.content.contentEquals("text to be registered".toByteArray(Charsets.UTF_8)))
    }

    // -- Preserve warnings, confidence, timestamps, mechanism identity/version --

    @Test
    fun `a fake Evidence Intelligence caller receives warnings, confidence, timestamp, and mechanism identity-version unchanged`() = runTest {
        val warnings = listOf("low contrast detected", "possible skew")
        val timestamp = Instant.parse("2026-05-20T08:00:00Z")
        val sequencer = fakeEvidenceIntelligenceCaller(
            OcrRecognitionOutcome.Recognised(sampleResult(confidence = 0.63, warnings = warnings, recognisedAt = timestamp)),
        )

        val outcome = sequencer.recognise(sampleRequest()) as OcrRecognitionOutcome.Recognised

        assertEquals(warnings, outcome.result.warnings, "warnings must reach the caller in original order, unmodified")
        assertEquals(0.63, outcome.result.confidence)
        assertEquals(timestamp, outcome.result.recognisedAt)
        assertEquals("mechanism-ei", outcome.result.identity.mechanismIdentity)
        assertEquals("2.0.1", outcome.result.identity.mechanismVersion)
    }

    // -- OCR itself performs none of the downstream responsibilities demonstrated above --

    /**
     * A compact, Unit-10-scoped confirmation, not a re-derivation of Unit
     * 9's own exhaustive, transitively-computed reachable-type-graph proof
     * (`OcrStructuralIsolationTest.kt`), which already establishes this
     * invariant far more strongly and for every dependency Scope Lock
     * Section 13 excludes. This test exists only to confirm, from Unit
     * 10's own caller-perspective, that none of the specific downstream
     * types this file's own tests construct (`CandidateProvenance`,
     * `CandidateEvidenceArtifact`) are reachable from OCR's own public
     * shape -- i.e. that this file's own hand-written fake is doing work
     * OCR itself structurally cannot do.
     */
    @Test
    fun `no OCR type references CandidateProvenance, CandidateEvidenceArtifact, MemoryCore, or PermissionEngine -- every downstream responsibility demonstrated above belongs to the caller, never to OCR itself`() {
        val excludedQualifiedNames = setOf(
            CandidateProvenance::class.qualifiedName,
            Provenance::class.qualifiedName,
            CandidateEvidenceArtifact::class.qualifiedName,
            MemoryCore::class.qualifiedName,
            PermissionEngine::class.qualifiedName,
        )
        val ocrOwnTypes: List<KClass<*>> = listOf(
            OcrMechanism::class, OcrProviderAdapter::class, OcrRecognitionRequest::class,
            OcrRecognitionIdentity::class, OcrRecognitionSegment::class, OcrRecognitionResult::class,
            OcrRecognitionOutcome::class,
        ) + OcrRecognitionOutcome::class.sealedSubclasses

        ocrOwnTypes.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.mapNotNull { (it.returnType.classifier as? KClass<*>)?.qualifiedName }
            propertyTypeNames.forEach { qualifiedName ->
                assertFalse(
                    excludedQualifiedNames.contains(qualifiedName),
                    "${type.simpleName} must not reference '$qualifiedName' -- provenance construction, evidence " +
                        "registration, Memory Core, and the Permission Engine all remain the caller's own, never " +
                        "OCR's own, responsibility (see OcrStructuralIsolationTest.kt for the exhaustive proof).",
                )
            }
        }
    }
}
