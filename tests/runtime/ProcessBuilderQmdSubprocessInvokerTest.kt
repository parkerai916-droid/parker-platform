package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression coverage for the owner-UI live acceptance defect where a deployment configured
 * `PARKER_QMD_SOURCE_ROOT` without also configuring `PARKER_QMD_TSX_CLI_PATH`: the composed
 * `QmdRelevanceMechanismConfiguration.bridgeScriptPath` defaults to a `.mts` file, which plain
 * `node` cannot execute (`ERR_UNKNOWN_FILE_EXTENSION`) without a TypeScript-capable loader named in
 * `additionalNodeArguments`. That failure previously surfaced only the first time Bounded Relevance
 * Computation's fallback genuinely fired, several layers inside a real Node subprocess crash, far
 * removed from its own true cause.
 *
 * Every test here runs offline, deterministically, with no real Node process, no QMD installation,
 * and no native binary of any kind -- the pre-flight check this suite verifies is structural
 * (`bridgeScriptPath`'s own extension plus whether `additionalNodeArguments` is empty), decided
 * entirely before [ProcessBuilderQmdSubprocessInvoker.invoke] ever constructs a real
 * `ProcessBuilder`. Tests proving the guard does *not* fire use a deliberately non-existent
 * `nodeExecutablePath` so that a real attempt to launch a process is itself observable (as an
 * ordinary "process failed to start" `IOException`, a genuinely different failure shape from this
 * guard's own) without depending on any real `node`/`tsx` binary actually being present.
 */
class ProcessBuilderQmdSubprocessInvokerTest {

    private fun configuration(
        bridgeScriptPath: String = "tools/qmd-relevance-bridge.mts",
        nodeExecutablePath: String = "node",
        additionalNodeArguments: List<String> = emptyList(),
    ): QmdRelevanceMechanismConfiguration = QmdRelevanceMechanismConfiguration(
        qmdVersion = "2.8.3",
        embeddingModelUri = "hf:ggml-org/embeddinggemma-300M-GGUF/embeddinggemma-300M-Q8_0.gguf",
        vectorDimension = 768,
        nodeExecutablePath = nodeExecutablePath,
        bridgeScriptPath = bridgeScriptPath,
        additionalNodeArguments = additionalNodeArguments,
    )

    @Test
    fun `a mts bridge script with no additionalNodeArguments fails closed before any process is spawned, with a clear diagnostic`() {
        val invoker = ProcessBuilderQmdSubprocessInvoker(configuration(bridgeScriptPath = "tools/qmd-relevance-bridge.mts"))

        val result = invoker.invoke("{}", timeoutMillis = 1_000)

        assertEquals(-1, result.exitCode)
        assertFalse(result.timedOut)
        assertEquals("", result.stdout)
        assertTrue(
            "TypeScript" in result.stderr && "PARKER_QMD_TSX_CLI_PATH" in result.stderr,
            "the diagnostic must name both the defect (a TypeScript module with no loader) and the " +
                "governed remedy (PARKER_QMD_TSX_CLI_PATH): ${result.stderr}",
        )
    }

    @Test
    fun `a cts bridge script with no additionalNodeArguments also fails closed`() {
        val invoker = ProcessBuilderQmdSubprocessInvoker(configuration(bridgeScriptPath = "tools/example-bridge.cts"))

        val result = invoker.invoke("{}", timeoutMillis = 1_000)

        assertEquals(-1, result.exitCode)
    }

    @Test
    fun `a ts bridge script with no additionalNodeArguments also fails closed`() {
        val invoker = ProcessBuilderQmdSubprocessInvoker(configuration(bridgeScriptPath = "tools/example-bridge.ts"))

        val result = invoker.invoke("{}", timeoutMillis = 1_000)

        assertEquals(-1, result.exitCode)
    }

    @Test
    fun `a mts bridge script WITH a configured loader argument does not trigger this guard -- it reaches real process launch`() {
        // A deliberately non-existent node executable: if the guard wrongly fired, the failure
        // would carry this test's own TypeScript-loader message; since it does not, the invoker
        // must instead have reached ProcessBuilder itself and observed a genuine start failure.
        val invoker = ProcessBuilderQmdSubprocessInvoker(
            configuration(
                bridgeScriptPath = "tools/qmd-relevance-bridge.mts",
                nodeExecutablePath = "/nonexistent/node-binary-does-not-exist-9f3a",
                additionalNodeArguments = listOf("/home/steve/qmd/node_modules/tsx/dist/cli.mjs"),
            ),
        )

        val result = invoker.invoke("{}", timeoutMillis = 1_000)

        assertEquals(-1, result.exitCode)
        assertTrue(
            "failed to start" in result.stderr,
            "a configured loader must let the invoker reach the real process-start attempt, never this " +
                "guard's own TypeScript-loader diagnostic: ${result.stderr}",
        )
        assertFalse("TypeScript" in result.stderr)
    }

    @Test
    fun `a non-TypeScript bridge script with no additionalNodeArguments does not trigger this guard either`() {
        val invoker = ProcessBuilderQmdSubprocessInvoker(
            configuration(
                bridgeScriptPath = "tools/qmd-relevance-bridge.mjs",
                nodeExecutablePath = "/nonexistent/node-binary-does-not-exist-9f3a",
            ),
        )

        val result = invoker.invoke("{}", timeoutMillis = 1_000)

        assertEquals(-1, result.exitCode)
        assertTrue("failed to start" in result.stderr)
        assertFalse("TypeScript" in result.stderr)
    }
}
