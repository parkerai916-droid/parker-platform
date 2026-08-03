package parker.composition

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Focused tests for [resolveEffectiveLogLevel] -- `PARKER_LOG_LEVEL`'s own
 * mode-sensitive default resolution (interactive-console refinement
 * task). Exercised directly, not via [main] itself: `main` calls
 * `exitProcess` and constructs a real [ParkerRuntime], neither of which
 * belongs in a unit test of a pure decision function.
 */
class MainTest {

    @Test
    fun `headless (interactive = false) with no PARKER_LOG_LEVEL set defaults to INFO`() {
        val effective = resolveEffectiveLogLevel(
            environment = emptyMap(),
            interactive = false,
            configuredLogLevel = LogLevel.INFO,
        )

        assertEquals(LogLevel.INFO, effective)
    }

    @Test
    fun `interactive with no PARKER_LOG_LEVEL set defaults to WARN`() {
        val effective = resolveEffectiveLogLevel(
            environment = emptyMap(),
            interactive = true,
            configuredLogLevel = LogLevel.INFO,
        )

        assertEquals(LogLevel.WARN, effective)
    }

    @Test
    fun `a blank PARKER_LOG_LEVEL is treated as absent -- interactive still defaults to WARN`() {
        val effective = resolveEffectiveLogLevel(
            environment = mapOf(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL to "   "),
            interactive = true,
            configuredLogLevel = LogLevel.INFO,
        )

        assertEquals(LogLevel.WARN, effective)
    }

    @Test
    fun `an explicit PARKER_LOG_LEVEL of DEBUG overrides the interactive WARN default`() {
        val effective = resolveEffectiveLogLevel(
            environment = mapOf(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL to "DEBUG"),
            interactive = true,
            configuredLogLevel = LogLevel.DEBUG,
        )

        assertEquals(LogLevel.DEBUG, effective)
    }

    @Test
    fun `an explicit PARKER_LOG_LEVEL of DEBUG overrides the headless INFO default`() {
        val effective = resolveEffectiveLogLevel(
            environment = mapOf(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL to "DEBUG"),
            interactive = false,
            configuredLogLevel = LogLevel.DEBUG,
        )

        assertEquals(LogLevel.DEBUG, effective)
    }

    @Test
    fun `an explicit PARKER_LOG_LEVEL equal to the interactive default (WARN) still counts as explicit -- not accidentally re-derived`() {
        val effective = resolveEffectiveLogLevel(
            environment = mapOf(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL to "WARN"),
            interactive = true,
            configuredLogLevel = LogLevel.WARN,
        )

        assertEquals(LogLevel.WARN, effective)
    }

    @Test
    fun `an explicit PARKER_LOG_LEVEL of INFO in interactive mode wins over the WARN default -- explicit always wins`() {
        val effective = resolveEffectiveLogLevel(
            environment = mapOf(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL to "INFO"),
            interactive = true,
            configuredLogLevel = LogLevel.INFO,
        )

        assertEquals(LogLevel.INFO, effective)
    }
}
