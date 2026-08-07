package parker.composition

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Sprint 10, Unit 4 acceptance test: `ParkerRuntimeConfigLoader`'s own
 * "missing configuration" / "invalid configuration" production behaviour
 * (task instruction), exercised in isolation from the rest of
 * `ParkerRuntime` -- no runtime component is constructed by this file.
 */
class ParkerRuntimeConfigLoaderTest {

    private fun fullEnvironment(overrides: Map<String, String?> = emptyMap()): Map<String, String> {
        val base = mapOf(
            ParkerRuntimeConfigLoader.KEY_MODEL_ENDPOINT_URL to "http://localhost:11434/api/generate",
            ParkerRuntimeConfigLoader.KEY_MODEL_NAME to "llama3",
            ParkerRuntimeConfigLoader.KEY_MODEL_TIMEOUT_MS to "15000",
            ParkerRuntimeConfigLoader.KEY_OWNER_PRINCIPAL_ID to "user.steven",
            ParkerRuntimeConfigLoader.KEY_OWNER_DISPLAY_NAME to "Steven",
            ParkerRuntimeConfigLoader.KEY_LOCAL_TEXT_CHANNEL_MODULE_ID to "channel.local-text-test",
            ParkerRuntimeConfigLoader.KEY_EVIDENCE_STORAGE_ROOT to Files.createTempDirectory("config-loader-test-evidence-storage").toString(),
            ParkerRuntimeConfigLoader.KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH to
                Files.createTempDirectory("config-loader-test-evidence-audit").resolve("audit.log").toString(),
            ParkerRuntimeConfigLoader.KEY_MEMORY_CORE_DURABILITY_LOG_PATH to
                Files.createTempDirectory("config-loader-test-memory-core").resolve("memory-core.log").toString(),
        )
        val merged = base.toMutableMap()
        overrides.forEach { (key, value) ->
            if (value == null) merged.remove(key) else merged[key] = value
        }
        return merged
    }

    @Test
    fun `every key present loads exactly the supplied values`() {
        val environment = fullEnvironment()
        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals("http://localhost:11434/api/generate", config.modelEndpointUrl)
        assertEquals("llama3", config.modelName)
        assertEquals(15000L, config.modelTimeoutMs)
        assertEquals("user.steven", config.ownerPrincipalId)
        assertEquals("Steven", config.ownerDisplayName)
        assertEquals("channel.local-text-test", config.localTextChannelModuleId)
        assertEquals(environment[ParkerRuntimeConfigLoader.KEY_EVIDENCE_STORAGE_ROOT], config.evidenceStorageRootPath)
        assertEquals(
            environment[ParkerRuntimeConfigLoader.KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH],
            config.evidenceDeletionAuditLogPath,
        )
        assertEquals(
            environment[ParkerRuntimeConfigLoader.KEY_MEMORY_CORE_DURABILITY_LOG_PATH],
            config.memoryCoreDurabilityLogPath,
        )
    }

    @Test
    fun `optional keys absent fall back to their documented defaults`() {
        val environment = fullEnvironment(
            overrides = mapOf(
                ParkerRuntimeConfigLoader.KEY_MODEL_TIMEOUT_MS to null,
                ParkerRuntimeConfigLoader.KEY_OWNER_DISPLAY_NAME to null,
                ParkerRuntimeConfigLoader.KEY_LOCAL_TEXT_CHANNEL_MODULE_ID to null,
            ),
        )

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals(30_000L, config.modelTimeoutMs)
        assertEquals("Owner", config.ownerDisplayName)
        assertEquals("channel.local-text", config.localTextChannelModuleId)
    }

    @Test
    fun `missing PARKER_MODEL_ENDPOINT_URL throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_MODEL_ENDPOINT_URL to null))

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_MODEL_ENDPOINT_URL, thrown.key)
    }

    @Test
    fun `missing PARKER_EVIDENCE_STORAGE_ROOT throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_EVIDENCE_STORAGE_ROOT to null))

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_EVIDENCE_STORAGE_ROOT, thrown.key)
    }

    @Test
    fun `missing PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(
            overrides = mapOf(ParkerRuntimeConfigLoader.KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH to null),
        )

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH, thrown.key)
    }

    @Test
    fun `missing PARKER_MEMORY_CORE_DURABILITY_LOG_PATH throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(
            overrides = mapOf(ParkerRuntimeConfigLoader.KEY_MEMORY_CORE_DURABILITY_LOG_PATH to null),
        )

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_MEMORY_CORE_DURABILITY_LOG_PATH, thrown.key)
    }

    @Test
    fun `missing PARKER_MODEL_NAME throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_MODEL_NAME to null))

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_MODEL_NAME, thrown.key)
    }

    @Test
    fun `missing PARKER_OWNER_PRINCIPAL_ID throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_OWNER_PRINCIPAL_ID to null))

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_OWNER_PRINCIPAL_ID, thrown.key)
    }

    @Test
    fun `a blank required value is treated identically to a missing key`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_MODEL_NAME to "   "))

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_MODEL_NAME, thrown.key)
    }

    @Test
    fun `a non-numeric PARKER_MODEL_TIMEOUT_MS throws InvalidConfiguration naming that key`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_MODEL_TIMEOUT_MS to "not-a-number"))

        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_MODEL_TIMEOUT_MS, thrown.key)
    }

    @Test
    fun `a non-positive PARKER_MODEL_TIMEOUT_MS throws InvalidConfiguration`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_MODEL_TIMEOUT_MS to "0"))

        assertIs<ParkerRuntimeException.InvalidConfiguration>(
            assertFailsWith<ParkerRuntimeException> { ParkerRuntimeConfigLoader.load(environment) },
        )
    }

    @Test
    fun `every ParkerRuntimeException thrown by the loader is a ParkerRuntimeException, never a bare NullPointerException or NumberFormatException`() {
        // Restated explicitly per the task's own "missing configuration" production-behaviour
        // requirement: a caller can catch ParkerRuntimeException alone and handle every
        // configuration fault uniformly.
        val missingEverything = emptyMap<String, String>()
        assertFailsWith<ParkerRuntimeException> { ParkerRuntimeConfigLoader.load(missingEverything) }
    }

    @Test
    fun `PARKER_LOG_LEVEL absent defaults to INFO`() {
        val environment = fullEnvironment()

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals(LogLevel.INFO, config.logLevel)
    }

    @Test
    fun `a blank PARKER_LOG_LEVEL also defaults to INFO`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL to "   "))

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals(LogLevel.INFO, config.logLevel)
    }

    @Test
    fun `an explicit PARKER_LOG_LEVEL of DEBUG loads LogLevel DEBUG`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL to "DEBUG"))

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals(LogLevel.DEBUG, config.logLevel)
    }

    @Test
    fun `an explicit PARKER_LOG_LEVEL of OFF loads LogLevel OFF`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL to "OFF"))

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals(LogLevel.OFF, config.logLevel)
    }

    @Test
    fun `an invalid PARKER_LOG_LEVEL throws InvalidConfiguration naming that key, not a silent fallback`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL to "VERBOSE"))

        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_LOG_LEVEL, thrown.key)
    }
}
