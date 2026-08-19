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
 * Run explicitly, from a machine with a local QMD checkout provisioned
 * (Windows or otherwise), via:
 * ```
 * .\gradlew.bat qmdRelevanceMechanismLiveAcceptance
 * ```
 * (task registered in `build.gradle.kts`, mirroring the existing
 * `reasoningProtocolLiveModelEvaluation`-family task registrations).
 *
 * Configuration (node executable, QMD source root, tsx CLI, bridge script
 * path, model cache directory) is read from environment variables rather
 * than hard-coded, mirroring `QmdCanonicalMemoryRetrievalExperimentTest.kt`'s
 * own `QMD_TEST_NODE` convention, since these are genuinely
 * environment/deployment-specific paths this Unit does not assume a fixed
 * value for on every machine this test might run on.
 *
 * **Post-Promotion QMD Live-Acceptance Portability Correction.** Every
 * machine-specific value below is read only from its own environment
 * variable, or derived from another explicitly-supplied one -- never a
 * fabricated, machine-specific fallback path (this file previously
 * defaulted `QMD_TEST_TSX_CLI`, `QMD_RELEVANCE_MODEL_CACHE_DIR`, and
 * `QMD_TEST_SOURCE_ROOT` to Steve's own Windows checkout locations, which
 * broke this test's own portability the first time it ran anywhere else).
 * `liveConfiguration()` never searches the filesystem, never guesses a
 * well-known install location, and never downloads or installs anything.
 * A machine with no local QMD checkout provisioned -- expected and lawful,
 * e.g. the Parker Linux server today -- is handled by
 * [assumeLiveQmdPrerequisitesProvisioned], a second, explicit
 * assumption/skip check (the same `assumeTrue` mechanism [LIVE_PROPERTY]
 * itself already uses) that reports precisely which environment variable(s)
 * are missing and skips cleanly, rather than allowing construction to
 * proceed and fail deep inside a subprocess with an opaque path error.
 */
class QmdRelevanceMechanismLiveAcceptanceTest {

    companion object {
        private const val LIVE_PROPERTY = "parker.relevance.qmd.live.enabled"
    }

    // Post-Promotion QMD Live-Acceptance Portability Correction. Both
    // machine-specific values below are resolved once, here, from exactly
    // the environment variables this file's own class KDoc documents --
    // never a fabricated fallback, never filesystem discovery. Shared by
    // [assumeLiveQmdPrerequisitesProvisioned] (the prerequisite check) and
    // [liveConfiguration] (the actual configuration build), so the
    // derivation rule for [tsxCliPath] exists in exactly one place.

    private fun resolvedQmdSourceRoot(): String? =
        System.getenv("QMD_TEST_SOURCE_ROOT")?.takeIf { it.isNotBlank() }

    private fun resolvedTsxCliPath(qmdSourceRoot: String?): String? =
        System.getenv("QMD_TEST_TSX_CLI")?.takeIf { it.isNotBlank() }
            ?: qmdSourceRoot?.let { Path.of(it, "node_modules", "tsx", "dist", "cli.mjs").toString() }

    /**
     * Explicit, diagnosable skip -- never a fabricated path -- when this
     * machine has no local QMD checkout provisioned. Uses the same
     * `assumeTrue` mechanism [LIVE_PROPERTY] itself already uses in every
     * `@Test` below, one further explicit assumption/skip check, matching
     * this repository's own established live-instrument convention rather
     * than introducing a new one. Called as the second line of every
     * `@Test` in this file, immediately after the existing [LIVE_PROPERTY]
     * check.
     */
    private fun assumeLiveQmdPrerequisitesProvisioned() {
        val qmdSourceRoot = resolvedQmdSourceRoot()
        val tsxCliPath = resolvedTsxCliPath(qmdSourceRoot)
        val missing = buildList {
            if (qmdSourceRoot == null) add("QMD_TEST_SOURCE_ROOT (the local QMD installation/checkout root)")
            if (tsxCliPath == null) add("QMD_TEST_TSX_CLI, or QMD_TEST_SOURCE_ROOT (from which it is derived)")
        }
        assumeTrue(
            missing.isEmpty(),
            "Live QMD prerequisites are not provisioned on this machine -- missing: ${missing.joinToString("; ")}. " +
                "This mechanism is never auto-discovered, guessed, or downloaded; provision a local QMD " +
                "checkout and set these environment variable(s) explicitly to run this live acceptance instrument.",
        )
    }

    private fun liveConfiguration(): QmdRelevanceMechanismConfiguration {
        val nodeExecutable = System.getenv("QMD_TEST_NODE")?.takeIf { it.isNotBlank() } ?: "node"
        val bridgeScriptPath = System.getenv("QMD_RELEVANCE_BRIDGE_SCRIPT")?.takeIf { it.isNotBlank() }
            ?: Path.of("tools", "qmd-relevance-bridge.mts").toAbsolutePath().toString()
        val qmdSourceRoot = resolvedQmdSourceRoot()
        val tsxCliPath = resolvedTsxCliPath(qmdSourceRoot)
        // No Steve-specific Windows fallback: `QmdRelevanceMechanismConfiguration.modelCacheDir`
        // already treats `null` as fully lawful -- the production bridge's
        // own local-only, no-download default cache-directory resolution
        // applies (`qmd/src/llm.ts`'s own `DEFAULT_MODEL_CACHE_DIR`, an
        // `XDG_CACHE_HOME`-derived, portable, machine-agnostic location,
        // never a network fetch) -- so no invented machine-specific path is
        // required here at all. An explicit test-only override remains
        // available via the environment variable for whoever genuinely
        // needs one.
        val modelCacheDir = System.getenv("QMD_RELEVANCE_MODEL_CACHE_DIR")?.takeIf { it.isNotBlank() }

        return QmdRelevanceMechanismConfiguration(
            qmdVersion = "2.8.3",
            embeddingModelUri = "hf:ggml-org/embeddinggemma-300M-GGUF/embeddinggemma-300M-Q8_0.gguf",
            vectorDimension = 768,
            nodeExecutablePath = nodeExecutable,
            additionalNodeArguments = listOfNotNull(tsxCliPath),
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
        assumeLiveQmdPrerequisitesProvisioned()

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
        assumeLiveQmdPrerequisitesProvisioned()

        val configuration = liveConfiguration()
        val mechanism = QmdRelevanceMechanism(configuration, ProcessBuilderQmdSubprocessInvoker(configuration))

        val result = mechanism.rank(RelevanceRequest("any query", listOf(candidate("only", "any content"))))

        assertEquals(listOf("only"), result.rankedTokens.map { it.value })
    }

    @Test
    fun `live QMD subprocess returns a successful empty result for zero candidates, without launching a process`() = runTest {
        assumeTrue(System.getProperty(LIVE_PROPERTY) == "true", "Live QMD property absent; no subprocess invoked")
        assumeLiveQmdPrerequisitesProvisioned()

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
        assumeLiveQmdPrerequisitesProvisioned()

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
