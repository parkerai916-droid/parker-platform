package parker.core.interfaces

import java.time.Instant
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.runtime.OcrExecutionSequencer

/**
 * OCR Mechanism, Implementation Unit 11 ("Verification"). Governed in
 * full by `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md`; by
 * `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md`; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md` ("the
 * Implementation Plan") Unit 11, whose own Responsibilities are to "run,
 * and confirm passing, the full suite" Units 1-10 already established,
 * consolidated against the cross-cutting categories its own Section 9
 * (Verification Strategy) and Section 10 (Structural Safeguards) name.
 *
 * **Why this file exists, given Unit 11's own text authorises a new
 * verification suite only "if the repository's own convention calls for
 * one."** This task's own governing instruction asks, explicitly, that
 * eight properties -- provider-neutral abstraction, execution sequencing,
 * the input boundary, the output model, the failure model,
 * provenance-supporting disclosure, structural isolation, and Evidence
 * Intelligence contract sufficiency -- be verified "collectively rather
 * than individually." Units 1-10's own nine test files each already prove
 * their own property in isolation, exhaustively; none of them proves that
 * all eight compose correctly together, in one realistic scenario, at
 * once. That collective composition is this file's own, sole, genuinely
 * new contribution -- it is not a re-derivation of any single-property
 * proof Units 1-10 already completed, each of which is cited rather than
 * repeated.
 *
 * **This file adds no production capability of its own.** Every property
 * it exercises was already true before this file existed; this file only
 * demonstrates that those already-true properties hold simultaneously,
 * and records, once, the closed set of facts (the nine
 * `OcrRecognitionOutcome` variants; which prior verification files exist)
 * every earlier unit's own test suite implicitly depends on remaining
 * stable.
 */
class OcrProgrammeVerificationTest {

    private class FakeOcrProviderAdapter(private val respond: suspend (OcrRecognitionRequest) -> OcrRecognitionOutcome) : OcrProviderAdapter {
        var invocationCount = 0
            private set
        override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome {
            invocationCount += 1
            return respond(request)
        }
    }

    // -- Collective verification: all eight required properties, composed in one realistic scenario --

    @Test
    fun `all eight required properties compose correctly at once, in a single successful recognition`() = runTest {
        // -- Input boundary: only already-retrieved content, no retrieval path of any kind (Unit 4) --
        val request = OcrRecognitionRequest(
            sourceEvidenceId = EvidenceArtifactId("evidence-programme-1"),
            content = byteArrayOf(1, 2, 3, 4, 5),
            mediaType = "image/png",
            pageCount = 2,
        )

        val segments = listOf(
            OcrRecognitionSegment(text = "page one, clean", fidelity = TranscriptionFidelity.VERBATIM, pageNumber = 1),
            OcrRecognitionSegment(text = "page two, reconstructed", fidelity = TranscriptionFidelity.INFERRED_RECONSTRUCTION, pageNumber = 2),
        )
        val result = OcrRecognitionResult(
            recognisedText = "page one, clean\npage two, reconstructed",
            fidelity = TranscriptionFidelity.VERBATIM,
            identity = OcrRecognitionIdentity(mechanismIdentity = "mechanism-programme", configurationProfile = "profile-programme", mechanismVersion = "3.1.4"),
            confidence = 0.82,
            recognisedAt = Instant.parse("2026-06-01T00:00:00Z"),
            warnings = listOf("page two required reconstruction"),
            segments = segments,
        )
        val expectedOutcome = OcrRecognitionOutcome.Recognised(result)

        // -- Provider-neutral abstraction (Unit 2): a hand-written fake, no concrete provider named anywhere --
        val adapter = FakeOcrProviderAdapter { expectedOutcome }

        // -- Execution sequencing (Unit 3): OcrMechanism -> OcrExecutionSequencer -> OcrProviderAdapter, no alternate path --
        val sequencer: OcrMechanism = OcrExecutionSequencer(adapter)
        val outcome = sequencer.recognise(request)

        assertSame(expectedOutcome, outcome, "the sequencer must relay the adapter's own outcome unchanged")
        assertEquals(1, adapter.invocationCount, "exactly one invocation -- no retry, no batching")

        // -- Evidence Intelligence contract sufficiency (Unit 10): exhaustive, no-else classification, before any smart cast narrows `outcome` below --
        val classification: String = when (outcome) {
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
        assertEquals("Recognised", classification)

        // -- Output model (Unit 6): fidelity, mixed-fidelity segments, page ordering, confidence, warnings, identity --
        val recognised = outcome as OcrRecognitionOutcome.Recognised
        assertEquals(TranscriptionFidelity.VERBATIM, recognised.result.fidelity)
        assertEquals(setOf(TranscriptionFidelity.VERBATIM, TranscriptionFidelity.INFERRED_RECONSTRUCTION), recognised.result.segments.map { it.fidelity }.toSet())
        assertEquals(listOf(1, 2), recognised.result.segments.map { it.pageNumber })
        assertEquals(0.82, recognised.result.confidence)
        assertEquals(listOf("page two required reconstruction"), recognised.result.warnings)
        assertEquals("mechanism-programme", recognised.result.identity.mechanismIdentity)

        // -- Provenance-supporting disclosure (Unit 8): a hypothetical CandidateProvenance, from this same outcome --
        val provenance = CandidateProvenance(
            sourceIdentifier = request.sourceEvidenceId.value,
            sourceType = "ocr-recognition",
            acquisitionTime = recognised.result.recognisedAt,
            contentNature = ContentNature.EXTRACTED,
            creator = "${recognised.result.identity.mechanismIdentity} ${recognised.result.identity.mechanismVersion.orEmpty()}".trim(),
            confidence = recognised.result.confidence,
        )
        assertEquals("evidence-programme-1", provenance.sourceIdentifier)

        // -- Structural isolation (Unit 9): nothing this scenario touched reaches a governed downstream type --
        assertNoProhibitedTypeReachableFrom(OcrRecognitionOutcome::class)
    }

    @Test
    fun `the failure model also composes -- a failure outcome remains distinguishable and structurally isolated`() = runTest {
        val request = OcrRecognitionRequest(EvidenceArtifactId("evidence-programme-2"), byteArrayOf(9, 9), "image/png")
        val adapter = FakeOcrProviderAdapter { OcrRecognitionOutcome.ProcessingOrDependencyFailure("a required processing step was unavailable") }
        val sequencer: OcrMechanism = OcrExecutionSequencer(adapter)

        val outcome = sequencer.recognise(request)

        assertTrue(outcome is OcrRecognitionOutcome.ProcessingOrDependencyFailure)
        assertEquals("a required processing step was unavailable", outcome.reason)
        assertNoProhibitedTypeReachableFrom(OcrRecognitionOutcome::class)
    }

    private fun assertNoProhibitedTypeReachableFrom(root: KClass<*>) {
        val excludedQualifiedNames = setOf(
            Provenance::class.qualifiedName, CandidateProvenance::class.qualifiedName,
            CandidateEvidenceArtifact::class.qualifiedName, MemoryCore::class.qualifiedName,
            KnowledgeSubmission::class.qualifiedName, PermissionEngine::class.qualifiedName,
            EvidenceCustodian::class.qualifiedName, EvidenceIntelligence::class.qualifiedName,
        )
        val typesToCheck = listOf(root) + root.sealedSubclasses
        typesToCheck.forEach { type ->
            val propertyTypeNames = type.declaredMemberProperties.mapNotNull { (it.returnType.classifier as? KClass<*>)?.qualifiedName }
            propertyTypeNames.forEach { qualifiedName ->
                assertFalse(excludedQualifiedNames.contains(qualifiedName), "${type.simpleName} must not reference '$qualifiedName'")
            }
        }
    }

    // -- Regression consolidation: the closed set every prior unit's own suite depends on remaining stable --

    @Test
    fun `regression -- OcrRecognitionOutcome retains exactly the same nine variants Units 1-10 collectively rely on`() {
        val expected = setOf(
            "Recognised", "Failed", "NotAuthorised", "UnsupportedOrInaccessibleInput", "NoRecognisableContent",
            "PartialOrDegradedOutput", "ValidationRejection", "ProcessingOrDependencyFailure", "GenuineImplementationFault",
        )
        assertEquals(expected, OcrRecognitionOutcome::class.sealedSubclasses.map { it.simpleName }.toSet())
    }

    @Test
    fun `regression -- every OCR verification file created across Units 1-10 is still present`() {
        val expectedFiles = listOf(
            "tests/contracts/OcrMechanismScopeTest.kt",
            "tests/contracts/OcrProviderAdapterScopeTest.kt",
            "tests/contracts/OcrInputContractTest.kt",
            "tests/contracts/OcrOutputModelTest.kt",
            "tests/contracts/OcrFailureHandlingTest.kt",
            "tests/runtime/OcrExecutionSequencerTest.kt",
            "tests/runtime/OcrExecutionPipelineTest.kt",
            "tests/contracts/OcrProvenanceDisclosureTest.kt",
            "tests/contracts/OcrStructuralIsolationTest.kt",
            "tests/contracts/OcrEvidenceIntelligenceContractSufficiencyTest.kt",
        )
        val missing = expectedFiles.filterNot { java.io.File(it).exists() }
        assertTrue(missing.isEmpty(), "the following Units 1-10 verification files are missing: $missing")
    }

    @Test
    fun `regression -- every OCR production file created across Units 1-3 is still present, and no fourth production file has appeared`() {
        val expectedFiles = setOf(
            "src/interfaces/OcrMechanism.kt",
            "src/interfaces/OcrProviderAdapter.kt",
            "src/runtime/OcrExecutionSequencer.kt",
        )
        val actualFiles = listOf(java.io.File("src/interfaces"), java.io.File("src/runtime"))
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" && it.name.startsWith("Ocr") } }
            .map { it.path.replace('\\', '/') }
            .toSet()

        assertEquals(expectedFiles, actualFiles, "the OCR mechanism's own production surface must remain exactly Units 1-3's own three files -- no Unit 12 runtime-composition file, and no concrete provider file, has appeared")
    }
}
