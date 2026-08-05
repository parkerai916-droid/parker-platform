package parker.core.runtime

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.OcrProviderAdapter
import parker.core.interfaces.OcrRecognitionIdentity
import parker.core.interfaces.OcrRecognitionOutcome
import parker.core.interfaces.OcrRecognitionRequest
import parker.core.interfaces.OcrRecognitionResult
import parker.core.interfaces.TranscriptionFidelity

/**
 * OCR Mechanism, Implementation Unit 5 ("OCR Execution Pipeline"). Governed
 * in full by
 * `docs/architecture/OCR_MECHANISM_CONTRACT_DESIGN.md` ("the Contract
 * Design") Section 3, Section 5; by
 * `docs/architecture/OCR_MECHANISM_SCOPE_LOCK.md` ("the Scope Lock")
 * Section 4, Section 6, Section 13; and by
 * `docs/implementation/OCR_MECHANISM_IMPLEMENTATION_PLAN.md`
 * ("the Implementation Plan") Unit 5.
 *
 * **This Unit adds no production file.** Implementation Plan Unit 5's own
 * Files-expected-to-change field is "None beyond Units 1-4's own files,
 * unless the pipeline itself requires a thin, dependency-free composition
 * point among them" -- no such point is needed: [OcrExecutionSequencer]
 * (Unit 3) already takes an [OcrProviderAdapter] (Unit 2) directly and
 * already implements `OcrMechanism` (Unit 1) end to end. "Complete the
 * end-to-end path" (Unit 5's own Purpose) is therefore a verification
 * responsibility -- proving the already-built pipeline behaves correctly
 * and deterministically across realistic scenarios -- not a new
 * construction responsibility. This file is that verification.
 *
 * **On "each of the three fidelity categories" and "page-aligned
 * output."** These remain fully testable: [TranscriptionFidelity] has all
 * three categories today, and [OcrRecognitionRequest.pageCount] already
 * exists. What Unit 1's own shape does *not* yet provide is a page-aligned
 * *representation* of recognised text -- that refinement is explicitly
 * Implementation Plan Unit 6's own, later responsibility ("Page-aligned
 * representation... is deferred to Implementation Plan Unit 6's own
 * refinement of this shape," `OcrMechanism.kt`'s own KDoc). This suite
 * therefore confirms a request's own page scope reaches the adapter and
 * may be reflected in its output, without asserting a page-aligned text
 * *format* Unit 1 does not yet define.
 *
 * **On "each of the seven failure distinctions."** As already established
 * in `OcrExecutionSequencerTest.kt`'s own KDoc: `OcrRecognitionOutcome`
 * settled, after two independent constitutional review cycles, at exactly
 * two variants (`Recognised`, `Failed(reason)`) -- Scope Lock Section 10
 * forbids representing the seven distinctions as any named, coded, or
 * enum-like list in this Unit's own tier. This suite exercises `Failed`
 * with several distinct, honest reasons -- the literal requirement's own
 * intent ("a distinct, non-fabricated outcome," never "silently upgraded
 * to a plausible-looking result") applied to the shape as it now, lawfully,
 * exists -- and does not claim to satisfy Unit 7's own, later, concrete
 * non-collapse verification.
 */
class OcrExecutionPipelineTest {

    private class FakeOcrProviderAdapter(private val respond: suspend (OcrRecognitionRequest) -> OcrRecognitionOutcome) : OcrProviderAdapter {
        var invocationCount: Int = 0
            private set
        val receivedRequests: MutableList<OcrRecognitionRequest> = mutableListOf()

        override suspend fun recognise(request: OcrRecognitionRequest): OcrRecognitionOutcome {
            invocationCount += 1
            receivedRequests += request
            return respond(request)
        }
    }

    private fun request(pageCount: Int? = null, sourceEvidenceId: String = "evidence-1") = OcrRecognitionRequest(
        sourceEvidenceId = EvidenceArtifactId(sourceEvidenceId),
        content = byteArrayOf(1, 2, 3, 4),
        mediaType = "image/png",
        pageCount = pageCount,
    )

    private fun result(
        text: String = "recognised text",
        fidelity: TranscriptionFidelity = TranscriptionFidelity.VERBATIM,
    ) = OcrRecognitionResult(
        recognisedText = text,
        fidelity = fidelity,
        identity = OcrRecognitionIdentity(mechanismIdentity = "fake-pipeline-provider", configurationProfile = "default"),
        recognisedAt = Instant.EPOCH,
    )

    // -- Successful recognition, each of the three fidelity categories -----

    @Test
    fun `a VERBATIM recognition completes the full request-to-result pipeline unchanged`() = runTest {
        val expected = OcrRecognitionOutcome.Recognised(result(fidelity = TranscriptionFidelity.VERBATIM))
        val sequencer = OcrExecutionSequencer(FakeOcrProviderAdapter { expected })

        val actual = sequencer.recognise(request())

        assertSame(expected, actual)
        assertEquals(TranscriptionFidelity.VERBATIM, (actual as OcrRecognitionOutcome.Recognised).result.fidelity)
    }

    @Test
    fun `a NORMALISED recognition completes the full request-to-result pipeline unchanged`() = runTest {
        val expected = OcrRecognitionOutcome.Recognised(result(fidelity = TranscriptionFidelity.NORMALISED))
        val sequencer = OcrExecutionSequencer(FakeOcrProviderAdapter { expected })

        val actual = sequencer.recognise(request())

        assertSame(expected, actual)
        assertEquals(TranscriptionFidelity.NORMALISED, (actual as OcrRecognitionOutcome.Recognised).result.fidelity)
    }

    @Test
    fun `an INFERRED_RECONSTRUCTION recognition completes the full request-to-result pipeline unchanged`() = runTest {
        val expected = OcrRecognitionOutcome.Recognised(result(fidelity = TranscriptionFidelity.INFERRED_RECONSTRUCTION))
        val sequencer = OcrExecutionSequencer(FakeOcrProviderAdapter { expected })

        val actual = sequencer.recognise(request())

        assertSame(expected, actual)
        assertEquals(TranscriptionFidelity.INFERRED_RECONSTRUCTION, (actual as OcrRecognitionOutcome.Recognised).result.fidelity)
    }

    // -- Page/document scope reaches the adapter -----------------------------

    @Test
    fun `a request carrying a page count reaches the adapter unchanged, and the adapter's own output may reflect it`() = runTest {
        val pagedRequest = request(pageCount = 3)
        val adapter = FakeOcrProviderAdapter { received ->
            OcrRecognitionOutcome.Recognised(result(text = "page count was ${received.pageCount}"))
        }
        val sequencer = OcrExecutionSequencer(adapter)

        val actual = sequencer.recognise(pagedRequest)

        assertEquals(3, adapter.receivedRequests.single().pageCount, "the adapter must receive the exact page count the caller supplied")
        assertEquals("page count was 3", (actual as OcrRecognitionOutcome.Recognised).result.recognisedText)
    }

    @Test
    fun `a request with no page count reaches the adapter as genuinely unknown, never defaulted to zero`() = runTest {
        val adapter = FakeOcrProviderAdapter { OcrRecognitionOutcome.Recognised(result()) }
        val sequencer = OcrExecutionSequencer(adapter)

        sequencer.recognise(request(pageCount = null))

        assertEquals(null, adapter.receivedRequests.single().pageCount, "an unknown page count must remain null, never fabricated as zero")
    }

    // -- Failure disclosure: distinct, non-fabricated outcomes, provisional per Unit 7's own later work --

    @Test
    fun `distinct failure reasons pass through the full pipeline unchanged and are never collapsed into one another`() = runTest {
        val reasons = listOf(
            "the supplied content could not be decoded",
            "no usable text was found in the supplied image",
            "the recognition engine reported an internal fault",
        )

        reasons.forEach { reason ->
            val sequencer = OcrExecutionSequencer(FakeOcrProviderAdapter { OcrRecognitionOutcome.Failed(reason) })

            val actual = sequencer.recognise(request())

            assertTrue(actual is OcrRecognitionOutcome.Failed, "every failure must surface as Failed, never silently upgraded to Recognised")
            assertEquals(reason, actual.reason, "each distinct failure reason must reach the caller unchanged, never collapsed into a generic message")
        }
    }

    @Test
    fun `a failure is never silently reported as an empty or fabricated success`() = runTest {
        val sequencer = OcrExecutionSequencer(FakeOcrProviderAdapter { OcrRecognitionOutcome.Failed("nothing could be recognised") })

        val actual = sequencer.recognise(request())

        assertTrue(actual !is OcrRecognitionOutcome.Recognised, "a genuine failure must never be represented as a successful, empty, or fabricated recognition")
    }

    // -- Determinism: the same fixed input, adapter, and configuration reproduce the same disclosure --

    @Test
    fun `repeated calls with the same fixed request against the same fake adapter reproduce the same disclosure every time`() = runTest {
        val expected = OcrRecognitionOutcome.Recognised(result(text = "stable output"))
        val sequencer = OcrExecutionSequencer(FakeOcrProviderAdapter { expected })
        val fixedRequest = request()

        val first = sequencer.recognise(fixedRequest)
        val second = sequencer.recognise(fixedRequest)
        val third = sequencer.recognise(fixedRequest)

        assertSame(expected, first)
        assertSame(expected, second)
        assertSame(expected, third)
    }

    // -- No retries, no batching: each distinct request produces exactly one adapter invocation --

    @Test
    fun `three distinct requests invoke the adapter exactly three times, never batched, never combined`() = runTest {
        val adapter = FakeOcrProviderAdapter { OcrRecognitionOutcome.Recognised(result()) }
        val sequencer = OcrExecutionSequencer(adapter)

        sequencer.recognise(request(sourceEvidenceId = "evidence-1"))
        sequencer.recognise(request(sourceEvidenceId = "evidence-2"))
        sequencer.recognise(request(sourceEvidenceId = "evidence-3"))

        assertEquals(3, adapter.invocationCount)
        assertEquals(
            listOf("evidence-1", "evidence-2", "evidence-3"),
            adapter.receivedRequests.map { it.sourceEvidenceId.value },
            "each request must reach the adapter individually, in order, never merged into a single batched call",
        )
    }

    // -- Provider neutrality preserved end to end: the pipeline is indifferent to which adapter is plugged in --

    @Test
    fun `the pipeline behaves identically regardless of which interchangeable fake adapter is plugged in`() = runTest {
        val outcomeA = OcrRecognitionOutcome.Recognised(result(text = "from provider A"))
        val outcomeB = OcrRecognitionOutcome.Recognised(result(text = "from provider B"))

        val sequencerA = OcrExecutionSequencer(FakeOcrProviderAdapter { outcomeA })
        val sequencerB = OcrExecutionSequencer(FakeOcrProviderAdapter { outcomeB })

        val resultA = sequencerA.recognise(request())
        val resultB = sequencerB.recognise(request())

        // The same OcrExecutionSequencer/OcrMechanism contract served both calls above; only the
        // plugged-in adapter differed, and each call returned exactly what its own adapter produced --
        // the substitutability Scope Lock Section 14 requires, demonstrated end to end.
        assertSame(outcomeA, resultA)
        assertSame(outcomeB, resultB)
    }
}
