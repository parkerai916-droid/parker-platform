package parker.integration

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import parker.core.interfaces.RelevanceCandidate
import parker.core.interfaces.RelevanceCandidateToken
import parker.core.interfaces.RelevanceRequest
import parker.core.runtime.ProcessBuilderQmdSubprocessInvoker
import parker.core.runtime.QmdRelevanceMechanism
import parker.core.runtime.QmdRelevanceMechanismConfiguration

/**
 * Programme 3, Unit 9.7.3 (Local Relevance Mechanism Adapter). This
 * Unit's own required "genuine live integration test" (this task's own
 * Phase 6/Phase 9): runs the real, production `QmdRelevanceMechanism`
 * against a real QMD embedding-model subprocess, on the Windows
 * development environment, over the exact accepted six-candidate
 * emergency-vet fixture the Section 13/13.1 spike evidence record
 * documents.
 *
 * Lives under `tests/integration`, this repository's own pre-existing
 * detached `liveModelEvaluation` source set (`build.gradle.kts`) --
 * deliberately NOT part of the ordinary `test`/`check`/`build` lifecycle,
 * mirroring every existing `ReasoningProtocol*` live instrument already
 * registered there. Gated by [LIVE_PROPERTY]: with the property absent
 * (every ordinary `./gradlew test` run, and this specific Claude/device-
 * bridge execution environment, which this task's own governance
 * discloses lacks the native QMD binaries this test requires), every test
 * below is skipped via `assumeTrue`, not failed -- exactly the same
 * pattern `ReasoningProtocolBaselineCharacterisationTest.kt`,
 * `ReasoningProtocolDiagnosticCharacterisationTest.kt`, and
 * `ReasoningProtocolFamilyFDiagnosticTest.kt` already establish for this
 * codebase's own gated live-model instruments.
 *
 * Run explicitly, from the Windows development environment, via:
 * ```
 * .\gradlew.bat qmdRelevanceMechanismLiveAcceptance
 * ```
 * (task registered in `build.gradle.kts`, mirroring the existing
 * `reasoningProtocolLiveModelEvaluation`-family task registrations).
 *
 * Configuration (node executable, bridge script path, model cache
 * directory) is read from environment variables rather than hard-coded,
 * mirroring `QmdCanonicalMemoryRetrievalExperimentTest.kt`'s own
 * `QMD_TEST_NODE` convention, since these are genuinely
 * environment/deployment-specific paths this Unit does not assume a
 * fixed value for on every machine this test might run on.
 */
class QmdRelevanceMechanismLiveAcceptanceTest {

    companion object {
        private const val LIVE_PROPERTY = "parker.relevance.qmd.live.enabled"
    }

    private fun liveConfiguration(): QmdRelevanceMechanismConfiguration {
        val nodeExecutable = System.getenv("QMD_TEST_NODE")?.takeIf { it.isNotBlank() } ?: "node"
        val tsxCliPath = System.getenv("QMD_TEST_TSX_CLI")?.takeIf { it.isNotBlank() }
            ?: "C:\\Projects\\Parker\\qmd\\node_modules\\tsx\\dist\\cli.mjs"
        val bridgeScriptPath = System.getenv("QMD_RELEVANCE_BRIDGE_SCRIPT")?.takeIf { it.isNotBlank() }
            ?: Path.of("tools", "qmd-relevance-bridge.mts").toAbsolutePath().toString()
        // Live Windows Acceptance Failure / Bounded Cache-Resolution
        // Correction (this Unit's own follow-up review): falls back to the
        // actual, confirmed-existing QMD model cache directory on this
        // development environment (`config/index.yml`'s own collection
        // root's sibling `cache/qmd/models`, populated via QMD's own
        // portable `XDG_CACHE_HOME`-derived default -- see
        // `qmd/src/llm.ts`'s `MODEL_CACHE_DIR`) when the environment
        // variable is not set, mirroring the exact fallback pattern already
        // used above for `tsxCliPath`. This is a local development test
        // fixture default, not production configuration: `QmdRelevanceMechanism`
        // and `QmdRelevanceMechanismConfiguration` themselves remain wholly
        // unaware of this path and receive it only via this test's own
        // externally-supplied configuration value, exactly as they would
        // receive any other deployment-specific location at composition
        // time.
        val modelCacheDir = System.getenv("QMD_RELEVANCE_MODEL_CACHE_DIR")?.takeIf { it.isNotBlank() }
            ?: "C:\\Projects\\Parker\\qmd-parker-experiment\\cache\\qmd\\models"
        // Main-Promotion Gate / Production QMD Bridge Portability Correction
        // (this Unit's own follow-on): `tools/qmd-relevance-bridge.mts` no
        // longer hard-codes its own QMD source-root import location -- it
        // now requires `qmdSourceRoot` on every request and resolves its
        // `src/llm.ts` and `node_modules/node-llama-cpp/dist/index.js`
        // imports from it dynamically (see that script's own header comment,
        // and `QmdRelevanceMechanismConfiguration.qmdSourceRoot`'s own
        // KDoc). Without this, the corrected bridge throws "missing or empty
        // qmdSourceRoot" and this test's own live subprocess calls would
        // fail -- this fallback default is the exact same local QMD checkout
        // root (`C:\Projects\Parker\qmd`) both of the bridge script's own
        // previously hard-coded import paths already pointed into, so this
        // test's own real Windows behaviour is unchanged by the correction,
        // mirroring the identical env-var-with-local-fallback pattern
        // already used above for `tsxCliPath`.
        val qmdSourceRoot = System.getenv("QMD_TEST_SOURCE_ROOT")?.takeIf { it.isNotBlank() }
            ?: "C:\\Projects\\Parker\\qmd"

        return QmdRelevanceMechanismConfiguration(
            qmdVersion = "2.8.3",
            embeddingModelUri = "hf:ggml-org/embeddinggemma-300M-GGUF/embeddinggemma-300M-Q8_0.gguf",
            vectorDimension = 768,
            nodeExecutablePath = nodeExecutable,
            additionalNodeArguments = listOf(tsxCliPath),
            bridgeScriptPath = bridgeScriptPath,
            modelCacheDir = modelCacheDir,
            timeoutMillis = 120_000,
            qmdSourceRoot = qmdSourceRoot,
        )
    }

    private fun candidate(token: String, content: String) = RelevanceCandidate(RelevanceCandidateToken(token), content)

    private val fixtureRequest = RelevanceRequest(
        "Which animal clinic did I tell you to use in an emergency?",
        listOf(
            candidate("candidate-1", "the owner's synthetic emergency vet is Harbour Animal Clinic"),
            candidate("candidate-2", "the owner's synthetic regular vet is Riverside Veterinary Centre"),
            candidate("candidate-3", "the owner's synthetic dog groomer is Central City Grooming"),
            candidate("candidate-4", "the owner's synthetic emergency plumber is Wellington Rapid Plumbing"),
            candidate("candidate-5", "the owner's synthetic preferred pharmacy is Harbour Pharmacy"),
            candidate("candidate-6", "the owner's synthetic favourite hiking trail is Widow's Peak Ridge"),
        ),
    )

    @Test
    fun `live QMD subprocess ranks the accepted six-candidate emergency-vet fixture with candidate-1 first, repeated three times`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live QMD property absent; no subprocess invoked")

        val configuration = liveConfiguration()
        val mechanism = QmdRelevanceMechanism(configuration, ProcessBuilderQmdSubprocessInvoker(configuration))

        val timings = mutableListOf<Long>()
        val rankings = mutableListOf<List<String>>()
        repeat(3) {
            val start = System.nanoTime()
            val result = mechanism.rank(fixtureRequest)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000
            timings += elapsedMs
            rankings += result.rankedTokens.map { it.value }
        }

        println("QMD live relevance acceptance -- latencies (ms): $timings")
        println("QMD live relevance acceptance -- rankings: $rankings")

        rankings.forEach { ranking ->
            assertEquals("candidate-1", ranking.first(), "the emergency-vet target must rank first, per the adopted spike evidence")
            assertEquals(6, ranking.size)
            assertEquals(ranking.toSet(), fixtureRequest.candidates.map { it.token.value }.toSet())
        }

        // Repetition stability: identical ranking across all three repeated,
        // frozen-configuration invocations (this task's own Phase 9 requirement).
        assertTrue(rankings.all { it == rankings.first() }, "repeated live invocations under frozen configuration must produce an identical ranking")
    }

    @Test
    fun `live QMD subprocess handles a single candidate deterministically`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live QMD property absent; no subprocess invoked")

        val configuration = liveConfiguration()
        val mechanism = QmdRelevanceMechanism(configuration, ProcessBuilderQmdSubprocessInvoker(configuration))

        val result = mechanism.rank(RelevanceRequest("any query", listOf(candidate("only", "any content"))))

        assertEquals(listOf("only"), result.rankedTokens.map { it.value })
    }

    @Test
    fun `live QMD subprocess returns a successful empty result for zero candidates, without launching a process`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live QMD property absent; no subprocess invoked")

        val configuration = liveConfiguration()
        val mechanism = QmdRelevanceMechanism(configuration, ProcessBuilderQmdSubprocessInvoker(configuration))

        val result = mechanism.rank(RelevanceRequest("q", emptyList()))

        assertTrue(result.rankedTokens.isEmpty())
    }

    @Test
    fun `a missing local embedding model fails closed, without triggering an on-demand network download`() = runTest {
        // Windows Verification / Local-Control Hardening Review, Finding 1.
        // Points modelCacheDir at a fresh, guaranteed-empty temporary
        // directory instead of the real cache, so `tools/qmd-relevance-bridge.mts`'s
        // own pre-flight `resolveModelFile(..., {download: false})` check
        // must find no local match. Proves the mechanism throws a
        // diagnosable failure -- surfaced through the exact same non-zero-exit
        // path `QmdRelevanceMechanismTest.kt`'s own offline tests already
        // cover -- rather than silently attempting a live download or
        // hanging indefinitely.
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live QMD property absent; no subprocess invoked")

        val emptyCacheDir = Files.createTempDirectory("parker-qmd-relevance-empty-model-cache-")
        try {
            val configuration = liveConfiguration().copy(modelCacheDir = emptyCacheDir.toAbsolutePath().toString())
            val mechanism = QmdRelevanceMechanism(configuration, ProcessBuilderQmdSubprocessInvoker(configuration))

            val error = kotlin.test.assertFailsWith<IllegalStateException> {
                mechanism.rank(RelevanceRequest("q", listOf(candidate("only", "content"))))
            }
            assertTrue(
                error.message!!.contains("local-control precondition failed"),
                "expected the bridge script's own fail-closed local-availability message, got: ${error.message}",
            )
        } finally {
            emptyCacheDir.toFile().deleteRecursively()
        }
    }
}
