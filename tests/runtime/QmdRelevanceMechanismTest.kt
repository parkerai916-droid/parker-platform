package parker.core.runtime

import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.RelevanceCandidate
import parker.core.interfaces.RelevanceCandidateToken
import parker.core.interfaces.RelevanceMechanism
import parker.core.interfaces.RelevanceRequest

/**
 * Programme 3, Unit 9.7.3 (Local Relevance Mechanism Adapter). Pure
 * protocol/validation tests over `QmdRelevanceMechanism.kt` -- every test
 * in this file runs offline, deterministically, with no real subprocess,
 * no QMD installation, and no native binary of any kind, via
 * [FakeQmdSubprocessInvoker]. The one genuine live-QMD acceptance test
 * this Unit also requires (this task's own Phase 6, "retain at least one
 * genuine live integration test") lives separately, in
 * `tests/integration/QmdRelevanceMechanismLiveAcceptanceTest.kt`, under
 * the repository's own pre-existing `liveModelEvaluation` detached source
 * set (`build.gradle.kts`) -- explicit opt-in, never part of this
 * ordinary `test` task, mirroring the `ReasoningProtocol*` live
 * instruments already established there.
 */
class QmdRelevanceMechanismTest {

    private fun configuration(timeoutMillis: Long = 30_000): QmdRelevanceMechanismConfiguration =
        QmdRelevanceMechanismConfiguration(
            qmdVersion = "2.8.3",
            embeddingModelUri = "hf:ggml-org/embeddinggemma-300M-GGUF/embeddinggemma-300M-Q8_0.gguf",
            vectorDimension = 768,
            nodeExecutablePath = "node",
            bridgeScriptPath = "tools/qmd-relevance-bridge.mts",
            timeoutMillis = timeoutMillis,
        )

    private class FakeQmdSubprocessInvoker(
        private val respond: (requestJson: String, timeoutMillis: Long) -> QmdSubprocessInvocationResult,
    ) : QmdSubprocessInvoker {
        var lastRequestJson: String? = null
            private set
        var invocationCount: Int = 0
            private set

        override fun invoke(requestJson: String, timeoutMillis: Long): QmdSubprocessInvocationResult {
            lastRequestJson = requestJson
            invocationCount += 1
            return respond(requestJson, timeoutMillis)
        }
    }

    private fun success(scoresJson: String) =
        QmdSubprocessInvocationResult(exitCode = 0, stdout = """{"scores":[$scoresJson]}""", stderr = "", timedOut = false)

    private fun candidate(token: String, content: String) = RelevanceCandidate(RelevanceCandidateToken(token), content)

    // ---- A. Contract / boundary ----

    @Test
    fun `QmdRelevanceMechanism implements RelevanceMechanism unchanged`() {
        val mechanism: RelevanceMechanism = QmdRelevanceMechanism(configuration(), FakeQmdSubprocessInvoker { _, _ -> success("""{"token":"t1","score":1.0}""") })
        assertTrue(mechanism is RelevanceMechanism)
    }

    @Test
    fun `QmdRelevanceMechanism declares only configuration and invoker, no canonical Parker dependency`() {
        val properties = QmdRelevanceMechanism::class.declaredMemberProperties.map { it.name }.toSet()
        assertEquals(setOf("configuration", "invoker"), properties)
    }

    @Test
    fun `QmdRelevanceMechanism constructor accepts no PermissionEngine, persistence, or Memory Core type`() {
        val constructor = QmdRelevanceMechanism::class.primaryConstructor
        assertTrue(constructor != null)
        val disallowedSimpleNames = setOf(
            "PermissionEngine",
            "DefaultPermissionEngine",
            "KnowledgeItemPersistence",
            "MemoryRetrieval",
            "MemoryCore",
            "KnowledgeStore",
        )
        constructor!!.parameters.forEach { parameter ->
            val typeName = parameter.type.classifier?.toString() ?: ""
            disallowedSimpleNames.forEach { disallowed ->
                assertTrue(!typeName.contains(disallowed), "constructor parameter ${parameter.name} must not reference $disallowed")
            }
        }
    }

    @Test
    fun `QmdRelevanceMechanismConfiguration and QmdSubprocessInvoker carry no KnowledgeId-typed field`() {
        QmdRelevanceMechanismConfiguration::class.declaredMemberProperties.forEach { property ->
            assertTrue(property.returnType.toString() != "parker.core.interfaces.KnowledgeId")
        }
    }

    // ---- B. Request serialization ----

    @Test
    fun `request JSON carries only queryText and token, content per candidate -- no KnowledgeId`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> success("""{"token":"t1","score":1.0}""") }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val request = RelevanceRequest("Which animal clinic?", listOf(candidate("t1", "the owner's synthetic emergency vet is Harbour Animal Clinic")))

        mechanism.rank(request)

        val requestJson = invoker.lastRequestJson!!
        assertTrue(requestJson.contains("\"queryText\":\"Which animal clinic?\""))
        assertTrue(requestJson.contains("\"token\":\"t1\""))
        assertTrue(requestJson.contains("\"content\":\"the owner's synthetic emergency vet is Harbour Animal Clinic\""))
        assertTrue(!requestJson.contains("KnowledgeId"))
        assertTrue(!requestJson.contains("knowledgeId"))
    }

    @Test
    fun `request candidate ordering is deterministic -- same as RelevanceRequest supplied order`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> success("""{"token":"a","score":1.0},{"token":"b","score":1.0}""") }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val request = RelevanceRequest("q", listOf(candidate("a", "content a"), candidate("b", "content b")))

        mechanism.rank(request)

        val requestJson = invoker.lastRequestJson!!
        assertTrue(requestJson.indexOf("\"a\"") < requestJson.indexOf("\"b\""))
    }

    // ---- C. Response parsing ----

    @Test
    fun `full ordering is returned in descending score order`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ ->
            success("""{"token":"low","score":0.1},{"token":"high","score":0.9},{"token":"mid","score":0.5}""")
        }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val request = RelevanceRequest(
            "q",
            listOf(candidate("low", "c1"), candidate("high", "c2"), candidate("mid", "c3")),
        )

        val result = mechanism.rank(request)

        assertEquals(listOf("high", "mid", "low"), result.rankedTokens.map { it.value })
    }

    @Test
    fun `a valid subset is not produced by this adapter -- every supplied candidate must be scored`() = runTest {
        // This adapter's own design choice (documented in QmdRelevanceMechanism.kt):
        // it always scores the full supplied set. A bridge response scoring
        // fewer candidates than were supplied is treated as a malformed,
        // partial result -- never silently accepted as a valid subset.
        val invoker = FakeQmdSubprocessInvoker { _, _ -> success("""{"token":"a","score":1.0}""") }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val request = RelevanceRequest("q", listOf(candidate("a", "content a"), candidate("b", "content b")))

        val error = assertFailsWith<IllegalStateException> { mechanism.rank(request) }
        assertTrue(error.message!!.contains("partial result"))
    }

    // ---- D. Integrity / failure ----

    @Test
    fun `malformed JSON response fails loudly`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> QmdSubprocessInvocationResult(0, "not json at all", "", false) }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        assertFailsWith<IllegalArgumentException> {
            mechanism.rank(RelevanceRequest("q", listOf(candidate("a", "content"))))
        }
    }

    @Test
    fun `missing scores field fails loudly`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> QmdSubprocessInvocationResult(0, """{"somethingElse":[]}""", "", false) }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val error = assertFailsWith<IllegalArgumentException> {
            mechanism.rank(RelevanceRequest("q", listOf(candidate("a", "content"))))
        }
        assertTrue(error.message!!.contains("scores"))
    }

    @Test
    fun `unknown returned token fails loudly`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> success("""{"token":"not-a-real-candidate","score":1.0}""") }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val error = assertFailsWith<IllegalStateException> {
            mechanism.rank(RelevanceRequest("q", listOf(candidate("a", "content"))))
        }
        assertTrue(error.message!!.contains("unknown token"))
    }

    @Test
    fun `duplicate returned token fails loudly`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> success("""{"token":"a","score":1.0},{"token":"a","score":0.5}""") }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val error = assertFailsWith<IllegalStateException> {
            mechanism.rank(RelevanceRequest("q", listOf(candidate("a", "content"))))
        }
        assertTrue(error.message!!.contains("duplicate"))
    }

    @Test
    fun `too many returned tokens fails loudly`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ ->
            success("""{"token":"a","score":1.0},{"token":"unexpected-extra","score":0.5}""")
        }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        assertFailsWith<IllegalStateException> {
            mechanism.rank(RelevanceRequest("q", listOf(candidate("a", "content"))))
        }
    }

    @Test
    fun `content returned instead of token-only output fails loudly`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ ->
            QmdSubprocessInvocationResult(
                0,
                """{"scores":[{"token":"a","content":"leaked content","score":1.0}]}""",
                "",
                false,
            )
        }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val error = assertFailsWith<IllegalArgumentException> {
            mechanism.rank(RelevanceRequest("q", listOf(candidate("a", "content"))))
        }
        assertTrue(error.message!!.contains("extra field"))
    }

    @Test
    fun `non-zero process exit code fails loudly, surfacing stderr`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> QmdSubprocessInvocationResult(1, "", "boom", false) }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val error = assertFailsWith<IllegalStateException> {
            mechanism.rank(RelevanceRequest("q", listOf(candidate("a", "content"))))
        }
        assertTrue(error.message!!.contains("boom"))
    }

    @Test
    fun `timeout fails loudly and never returns a result`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> QmdSubprocessInvocationResult(-1, "", "", timedOut = true) }
        val mechanism = QmdRelevanceMechanism(configuration(timeoutMillis = 5_000), invoker)
        val error = assertFailsWith<IllegalStateException> {
            mechanism.rank(RelevanceRequest("q", listOf(candidate("a", "content"))))
        }
        assertTrue(error.message!!.contains("timed out"))
    }

    @Test
    fun `process unavailable -- start failure surfaces as a thrown failure, never an empty success`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ ->
            QmdSubprocessInvocationResult(-1, "", "QMD relevance bridge process failed to start: No such file or directory", false)
        }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val error = assertFailsWith<IllegalStateException> {
            mechanism.rank(RelevanceRequest("q", listOf(candidate("a", "content"))))
        }
        assertTrue(error.message!!.contains("failed to start"))
    }

    // ---- E. Determinism ----

    @Test
    fun `repeated frozen input returns an identical RelevanceResult`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ ->
            success("""{"token":"a","score":0.5858313981584177},{"token":"b","score":0.413659729701786}""")
        }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val request = RelevanceRequest("q", listOf(candidate("a", "c1"), candidate("b", "c2")))

        val first = mechanism.rank(request)
        val second = mechanism.rank(request)
        val third = mechanism.rank(request)

        assertEquals(first, second)
        assertEquals(first, third)
    }

    @Test
    fun `deterministic tie-breaking -- equal scores resolve by original supplied order, not incidental output order`() = runTest {
        // Bridge script returns the tie in *reverse* supplied order; the
        // adapter must still resolve the tie by the original request order
        // (a before b), never by the order the JSON happened to list them in.
        val invoker = FakeQmdSubprocessInvoker { _, _ -> success("""{"token":"b","score":0.5},{"token":"a","score":0.5}""") }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val request = RelevanceRequest("q", listOf(candidate("a", "c1"), candidate("b", "c2")))

        val result = mechanism.rank(request)

        assertEquals(listOf("a", "b"), result.rankedTokens.map { it.value })
    }

    // ---- F. Zero candidates ----

    @Test
    fun `zero candidates returns a successful empty result without launching a subprocess`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> success("""{"token":"never","score":1.0}""") }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)

        val result = mechanism.rank(RelevanceRequest("q", emptyList()))

        assertTrue(result.rankedTokens.isEmpty())
        assertEquals(0, invoker.invocationCount)
    }

    // ---- G. One candidate ----

    @Test
    fun `single candidate is handled deterministically and safely`() = runTest {
        val invoker = FakeQmdSubprocessInvoker { _, _ -> success("""{"token":"only","score":0.42}""") }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)

        val result = mechanism.rank(RelevanceRequest("q", listOf(candidate("only", "content"))))

        assertEquals(listOf("only"), result.rankedTokens.map { it.value })
    }

    // ---- H. Selected semantic fixture (accepted spike evidence, reproduced through the real adapter) ----

    @Test
    fun `accepted six-candidate emergency-vet fixture ranks candidate-1 first, matching the adopted spike evidence`() = runTest {
        // Scores below are the exact accepted values from
        // docs/reviews/PROGRAMME_3_UNIT_9_7_SECTION_13_MECHANISM_SELECTION_SPIKE_EVIDENCE_RECORD.md
        // Section 9 -- this test proves QmdRelevanceMechanism's own
        // request/response/ordering pipeline reproduces the adopted ranking,
        // not merely that the spike's own disposable test file did.
        val invoker = FakeQmdSubprocessInvoker { _, _ ->
            success(
                """{"token":"candidate-1","score":0.5858313981584177},""" +
                    """{"token":"candidate-2","score":0.413659729701786},""" +
                    """{"token":"candidate-3","score":0.2925},""" +
                    """{"token":"candidate-4","score":0.2664},""" +
                    """{"token":"candidate-5","score":0.2130},""" +
                    """{"token":"candidate-6","score":0.0741}""",
            )
        }
        val mechanism = QmdRelevanceMechanism(configuration(), invoker)
        val request = RelevanceRequest(
            "Which animal clinic did I tell you to use in an emergency?",
            listOf(
                candidate("candidate-1", "emergency vet -- Harbour Animal Clinic"),
                candidate("candidate-2", "regular vet -- Riverside Veterinary Centre"),
                candidate("candidate-3", "dog groomer -- Central City Grooming"),
                candidate("candidate-4", "emergency plumber -- Wellington Rapid Plumbing"),
                candidate("candidate-5", "pharmacy -- Harbour Pharmacy"),
                candidate("candidate-6", "hiking trail -- Widow's Peak Ridge"),
            ),
        )

        val result = mechanism.rank(request)

        assertEquals(
            listOf("candidate-1", "candidate-2", "candidate-3", "candidate-4", "candidate-5", "candidate-6"),
            result.rankedTokens.map { it.value },
        )
    }
}
