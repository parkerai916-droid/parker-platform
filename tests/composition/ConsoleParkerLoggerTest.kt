package parker.composition

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Focused tests for [ConsoleParkerLogger]'s own `minLevel` console display
 * threshold (`PARKER_LOG_LEVEL`) and its stdout/stderr separation
 * (interactive-console refinement task) -- [System.out]/[System.err] are
 * redirected around each test so assertions read real, written console
 * output, not a [ParkerLogger] test double that could hide a filtering or
 * stream-routing bug this class's own [ConsoleParkerLogger.log]
 * introduces.
 *
 * **All levels now write to `System.err` only.** Previously `DEBUG`/`INFO`
 * wrote to `System.out`; every test below that checks a *visible* line now
 * checks [errCapture], not [outCapture] -- [outCapture] is asserted empty
 * throughout, proving `ConsoleParkerLogger` never writes to `System.out`
 * at all, which is what reserves stdout for `runInteractiveConsole`'s own
 * banner/prompts/spinner/reply.
 */
class ConsoleParkerLoggerTest {

    private val originalOut = System.out
    private val originalErr = System.err
    private lateinit var outCapture: ByteArrayOutputStream
    private lateinit var errCapture: ByteArrayOutputStream

    private val fixedClock = { Instant.parse("2026-01-01T00:00:00Z") }

    @BeforeTest
    fun redirectStreams() {
        outCapture = ByteArrayOutputStream()
        errCapture = ByteArrayOutputStream()
        System.setOut(PrintStream(outCapture))
        System.setErr(PrintStream(errCapture))
    }

    @AfterTest
    fun restoreStreams() {
        System.setOut(originalOut)
        System.setErr(originalErr)
    }

    @Test
    fun `constructed with no explicit minLevel, DEBUG is suppressed -- the default is INFO`() {
        val logger = ConsoleParkerLogger("test-component", clock = fixedClock)

        logger.debug("a debug line")

        assertTrue(errCapture.toString().isEmpty(), "expected no console output for a suppressed DEBUG line")
    }

    @Test
    fun `constructed with an explicit DEBUG minLevel, a DEBUG line is emitted`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.DEBUG, clock = fixedClock)

        logger.debug("a debug line")

        assertTrue(errCapture.toString().contains("[DEBUG] [test-component] a debug line"))
    }

    @Test
    fun `at INFO minLevel, a DEBUG line is suppressed`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.INFO, clock = fixedClock)

        logger.debug("execution request received (eventType=execution.request_received)")

        assertTrue(errCapture.toString().isEmpty())
    }

    @Test
    fun `at DEBUG minLevel, a DEBUG line is emitted`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.DEBUG, clock = fixedClock)

        logger.debug("execution request received (eventType=execution.request_received)")

        assertTrue(errCapture.toString().contains("execution request received"))
    }

    @Test
    fun `at INFO minLevel, WARN and ERROR lines remain visible`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.INFO, clock = fixedClock)

        logger.warn("Execution denied (eventType=permission.denied)")
        logger.error("Reasoning Provider timed out")

        val errOutput = errCapture.toString()
        assertTrue(errOutput.contains("[WARN] [test-component] Execution denied"))
        assertTrue(errOutput.contains("[ERROR] [test-component] Reasoning Provider timed out"))
    }

    @Test
    fun `at INFO minLevel, an INFO line remains visible`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.INFO, clock = fixedClock)

        logger.info("Reply delivered")

        assertTrue(errCapture.toString().contains("[INFO] [test-component] Reply delivered"))
    }

    @Test
    fun `at ERROR minLevel, WARN is suppressed but ERROR remains visible`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.ERROR, clock = fixedClock)

        logger.warn("suppressed warning")
        logger.error("visible error")

        val errOutput = errCapture.toString()
        assertTrue("suppressed warning" !in errOutput)
        assertTrue(errOutput.contains("visible error"))
    }

    @Test
    fun `at WARN minLevel, DEBUG and INFO lines are both suppressed`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.WARN, clock = fixedClock)

        logger.debug("suppressed debug")
        logger.info("suppressed info")

        assertTrue(errCapture.toString().isEmpty())
    }

    @Test
    fun `at WARN minLevel, WARN and ERROR lines remain visible`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.WARN, clock = fixedClock)

        logger.warn("a warning")
        logger.error("an error")

        val errOutput = errCapture.toString()
        assertTrue(errOutput.contains("[WARN] [test-component] a warning"))
        assertTrue(errOutput.contains("[ERROR] [test-component] an error"))
    }

    @Test
    fun `at OFF minLevel, nothing is written at all, not even ERROR`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.OFF, clock = fixedClock)

        logger.debug("x")
        logger.info("x")
        logger.warn("x")
        logger.error("x")

        assertTrue(outCapture.toString().isEmpty())
        assertTrue(errCapture.toString().isEmpty())
    }

    @Test
    fun `ConsoleParkerLogger never writes to stdout, at any level`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.DEBUG, clock = fixedClock)

        logger.debug("x")
        logger.info("x")
        logger.warn("x")
        logger.error("x")

        assertTrue(outCapture.toString().isEmpty(), "ConsoleParkerLogger must never write to System.out")
        assertTrue(errCapture.toString().isNotEmpty())
    }

    @Test
    fun `with no spinnerLineGuard supplied, logging behaves exactly as before -- no crash, no behaviour change`() {
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.WARN, clock = fixedClock)

        logger.warn("a warning")

        assertTrue(errCapture.toString().contains("a warning"))
    }

    @Test
    fun `a spinnerLineGuard's clear action fires only for a log line that actually passes the level filter`() {
        var clearCalls = 0
        val guard = SpinnerLineGuard()
        guard.activate { clearCalls++ }
        val logger = ConsoleParkerLogger("test-component", minLevel = LogLevel.WARN, clock = fixedClock, spinnerLineGuard = guard)

        logger.debug("suppressed -- below WARN threshold")
        assertEquals(0, clearCalls, "a suppressed line must not trigger a spinner-line clear")

        logger.warn("a warning")
        assertEquals(1, clearCalls)

        logger.error("an error")
        assertEquals(2, clearCalls)
    }
}
