package parker.composition

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
            ParkerRuntimeConfigLoader.KEY_EVIDENCE_SOURCE_MANIFEST_STORAGE_ROOT to
                Files.createTempDirectory("config-loader-test-evidence-source-manifest-storage").toString(),
            ParkerRuntimeConfigLoader.KEY_DERIVATIVE_GENERATION_STORAGE_ROOT to
                Files.createTempDirectory("config-loader-test-derivative-generation-storage").toString(),
            ParkerRuntimeConfigLoader.KEY_DERIVATIVE_CONTENT_STORAGE_ROOT to
                Files.createTempDirectory("config-loader-test-derivative-content-storage").toString(),
            ParkerRuntimeConfigLoader.KEY_SAVED_ANALYSIS_STORAGE_ROOT to
                Files.createTempDirectory("config-loader-test-saved-analysis-storage").toString(),
            ParkerRuntimeConfigLoader.KEY_DOCUMENT_INGESTION_AUDIT_LOG_PATH to
                Files.createTempDirectory("config-loader-test-document-ingestion-audit").resolve("audit.log").toString(),
            ParkerRuntimeConfigLoader.KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH to
                Files.createTempDirectory("config-loader-test-evidence-audit").resolve("audit.log").toString(),
            ParkerRuntimeConfigLoader.KEY_MEMORY_CORE_DURABILITY_LOG_PATH to
                Files.createTempDirectory("config-loader-test-memory-core").resolve("memory-core.log").toString(),
            ParkerRuntimeConfigLoader.KEY_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH to
                Files.createTempDirectory("config-loader-test-knowledge-items").resolve("knowledge-items.log").toString(),
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
            environment[ParkerRuntimeConfigLoader.KEY_EVIDENCE_SOURCE_MANIFEST_STORAGE_ROOT],
            config.evidenceSourceManifestStorageRootPath,
        )
        assertEquals(
            environment[ParkerRuntimeConfigLoader.KEY_DERIVATIVE_GENERATION_STORAGE_ROOT],
            config.derivativeGenerationStorageRootPath,
        )
        assertEquals(
            environment[ParkerRuntimeConfigLoader.KEY_DERIVATIVE_CONTENT_STORAGE_ROOT],
            config.derivativeContentStorageRootPath,
        )
        assertEquals(
            environment[ParkerRuntimeConfigLoader.KEY_SAVED_ANALYSIS_STORAGE_ROOT],
            config.savedAnalysisStorageRootPath,
        )
        assertEquals(
            environment[ParkerRuntimeConfigLoader.KEY_DOCUMENT_INGESTION_AUDIT_LOG_PATH],
            config.documentIngestionAuditLogPath,
        )
        assertEquals(
            environment[ParkerRuntimeConfigLoader.KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH],
            config.evidenceDeletionAuditLogPath,
        )
        assertEquals(
            environment[ParkerRuntimeConfigLoader.KEY_MEMORY_CORE_DURABILITY_LOG_PATH],
            config.memoryCoreDurabilityLogPath,
        )
        assertEquals(
            environment[ParkerRuntimeConfigLoader.KEY_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH],
            config.knowledgeItemDurabilityLogPath,
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
        assertEquals(null, config.regionProviderStateStorageRootPath)
    }

    @Test
    fun `region provider state root is loaded exactly and never invented`() {
        val authorityRoot = Files.createTempDirectory("config-region-authority")
        val attemptRoot = Files.createTempDirectory("config-region-attempt")
        val providerRoot = Files.createTempDirectory("config-region-provider")
        val config = ParkerRuntimeConfigLoader.load(
            fullEnvironment(
                mapOf(
                    ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT to authorityRoot.toString(),
                    ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT to attemptRoot.toString(),
                    ParkerRuntimeConfigLoader.KEY_REGION_PROVIDER_STATE_STORAGE_ROOT to providerRoot.toString(),
                    ParkerRuntimeConfigLoader.KEY_PRODUCTION_COMMIT to "a".repeat(40),
                ),
            ),
        )

        assertEquals(providerRoot.toString(), config.regionProviderStateStorageRootPath)
    }

    @Test
    fun `region provider state root without the governed acceptance configuration fails closed`() {
        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(
                fullEnvironment(
                    mapOf(ParkerRuntimeConfigLoader.KEY_REGION_PROVIDER_STATE_STORAGE_ROOT to "/not-a-fallback"),
                ),
            )
        }

        assertEquals(ParkerRuntimeConfigLoader.KEY_REGION_PROVIDER_STATE_STORAGE_ROOT, thrown.key)
    }

    @Test
    fun `region acceptance root loads only with complete immutable deployment binding`() {
        val config = ParkerRuntimeConfigLoader.load(fullEnvironment(mapOf(
            ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT to "/legacy-authorities",
            ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT to "/attempts",
            ParkerRuntimeConfigLoader.KEY_REGION_PROVIDER_STATE_STORAGE_ROOT to "/provider-state",
            ParkerRuntimeConfigLoader.KEY_REGION_ACCEPTANCE_AUTHORITY_STORAGE_ROOT to "/region-authorities",
            ParkerRuntimeConfigLoader.KEY_DEPLOYED_IMMUTABLE_IMAGE_ID to "sha256:" + "b".repeat(64),
            ParkerRuntimeConfigLoader.KEY_SOURCE_COMMIT to "a".repeat(40),
            ParkerRuntimeConfigLoader.KEY_PRODUCTION_COMMIT to "a".repeat(40),
        )))
        assertEquals("/region-authorities", config.regionAcceptanceAuthorityStorageRootPath)
        assertEquals("sha256:" + "b".repeat(64), config.deployedImmutableImageId)
        assertEquals("a".repeat(40), config.sourceCommit)
    }

    @Test
    fun `region acceptance root without image and source identity fails closed`() {
        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(fullEnvironment(mapOf(
                ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT to "/legacy-authorities",
                ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT to "/attempts",
                ParkerRuntimeConfigLoader.KEY_REGION_PROVIDER_STATE_STORAGE_ROOT to "/provider-state",
                ParkerRuntimeConfigLoader.KEY_REGION_ACCEPTANCE_AUTHORITY_STORAGE_ROOT to "/region-authorities",
                ParkerRuntimeConfigLoader.KEY_PRODUCTION_COMMIT to "a".repeat(40),
            )))
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_REGION_ACCEPTANCE_AUTHORITY_STORAGE_ROOT, thrown.key)
    }

    @Test
    fun `mutable image tag is rejected for region acceptance`() {
        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(fullEnvironment(mapOf(ParkerRuntimeConfigLoader.KEY_DEPLOYED_IMMUTABLE_IMAGE_ID to "parker:latest")))
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_DEPLOYED_IMMUTABLE_IMAGE_ID, thrown.key)
    }

    // Programme 3, Unit 9.7.5 (Runtime Composition Wiring).

    @Test
    fun `QMD keys absent fall back to their documented defaults -- node, portable bridge script path, and null tsx-cli-path, model-cache-dir, and source-root`() {
        val environment = fullEnvironment()

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals("node", config.qmdNodeExecutablePath)
        assertTrue(config.qmdBridgeScriptPath.endsWith("tools" + java.io.File.separator + "qmd-relevance-bridge.mts"))
        assertEquals(null, config.qmdTsxCliPath)
        assertEquals(null, config.qmdModelCacheDir)
        assertEquals(120_000L, config.qmdTimeoutMillis)
        assertEquals(null, config.qmdSourceRoot)
    }

    // ui-desktop MODULE_NOT_FOUND regression (post-Unit-9.7.5 owner-UI live acceptance defect):
    // this default is documented ("Optional, defaults to that file's own well-known, portable,
    // repository-relative location resolved to an absolute path at construction time") as
    // resolving relative to the live process's own working directory -- true for the CLI (Docker's
    // own WORKDIR) and for this very test (Gradle's root-project `test` task's own default working
    // directory), but silently false for any entry point whose own working directory is not the
    // repository root, e.g. `ui-desktop`'s `runOwnerUi` JavaExec task before that task's own
    // `workingDir` was fixed to `rootProject.projectDir`. This assertion catches a regression of
    // either side of that assumption -- the resolved path drifting, or this test itself running
    // from the wrong working directory -- by proving the file this default names genuinely exists
    // on disk right now, not merely that the returned string has the expected shape.
    @Test
    fun `the default qmdBridgeScriptPath resolves to a file that genuinely exists on disk from the repository root`() {
        val environment = fullEnvironment()

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertTrue(
            java.io.File(config.qmdBridgeScriptPath).isFile,
            "the default qmdBridgeScriptPath ('${config.qmdBridgeScriptPath}') must resolve to a real, " +
                "existing file when the process working directory is the repository root -- every production " +
                "entry point relying on this default (the CLI, and any future Gradle task) must itself run " +
                "with the repository root as its own working directory for this documented default to be correct",
        )
    }

    @Test
    fun `every QMD key present loads exactly the supplied values`() {
        val environment = fullEnvironment(
            overrides = mapOf(
                ParkerRuntimeConfigLoader.KEY_QMD_NODE_EXECUTABLE_PATH to "C:\\custom\\node.exe",
                ParkerRuntimeConfigLoader.KEY_QMD_BRIDGE_SCRIPT_PATH to "C:\\custom\\bridge.mts",
                ParkerRuntimeConfigLoader.KEY_QMD_TSX_CLI_PATH to "C:\\custom\\tsx\\cli.mjs",
                ParkerRuntimeConfigLoader.KEY_QMD_MODEL_CACHE_DIR to "C:\\custom\\models",
                ParkerRuntimeConfigLoader.KEY_QMD_TIMEOUT_MILLIS to "45000",
                ParkerRuntimeConfigLoader.KEY_QMD_SOURCE_ROOT to "C:\\custom\\qmd",
            ),
        )

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals("C:\\custom\\node.exe", config.qmdNodeExecutablePath)
        assertEquals("C:\\custom\\bridge.mts", config.qmdBridgeScriptPath)
        assertEquals("C:\\custom\\tsx\\cli.mjs", config.qmdTsxCliPath)
        assertEquals("C:\\custom\\models", config.qmdModelCacheDir)
        assertEquals(45_000L, config.qmdTimeoutMillis)
        assertEquals("C:\\custom\\qmd", config.qmdSourceRoot)
    }

    // Main-Promotion Gate / Production QMD Bridge Portability Correction
    // (this Unit's own follow-on).

    @Test
    fun `a blank PARKER_QMD_SOURCE_ROOT falls back to null, same as absent`() {
        val environment = fullEnvironment(
            overrides = mapOf(ParkerRuntimeConfigLoader.KEY_QMD_SOURCE_ROOT to "   "),
        )

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals(null, config.qmdSourceRoot)
    }

    @Test
    fun `a non-numeric PARKER_QMD_TIMEOUT_MILLIS throws InvalidConfiguration naming that key`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_QMD_TIMEOUT_MILLIS to "not-a-number"))

        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_QMD_TIMEOUT_MILLIS, thrown.key)
    }

    @Test
    fun `a non-positive PARKER_QMD_TIMEOUT_MILLIS throws InvalidConfiguration`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_QMD_TIMEOUT_MILLIS to "0"))

        assertIs<ParkerRuntimeException.InvalidConfiguration>(
            assertFailsWith<ParkerRuntimeException> { ParkerRuntimeConfigLoader.load(environment) },
        )
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
    fun `missing PARKER_EVIDENCE_SOURCE_MANIFEST_STORAGE_ROOT throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(
            overrides = mapOf(ParkerRuntimeConfigLoader.KEY_EVIDENCE_SOURCE_MANIFEST_STORAGE_ROOT to null),
        )

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_EVIDENCE_SOURCE_MANIFEST_STORAGE_ROOT, thrown.key)
    }

    @Test
    fun `missing PARKER_DERIVATIVE_GENERATION_STORAGE_ROOT throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(
            overrides = mapOf(ParkerRuntimeConfigLoader.KEY_DERIVATIVE_GENERATION_STORAGE_ROOT to null),
        )

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_DERIVATIVE_GENERATION_STORAGE_ROOT, thrown.key)
    }

    @Test
    fun `missing PARKER_DERIVATIVE_CONTENT_STORAGE_ROOT throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(
            overrides = mapOf(ParkerRuntimeConfigLoader.KEY_DERIVATIVE_CONTENT_STORAGE_ROOT to null),
        )

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_DERIVATIVE_CONTENT_STORAGE_ROOT, thrown.key)
    }

    @Test
    fun `missing PARKER_SAVED_ANALYSIS_STORAGE_ROOT throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(
            overrides = mapOf(ParkerRuntimeConfigLoader.KEY_SAVED_ANALYSIS_STORAGE_ROOT to null),
        )

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_SAVED_ANALYSIS_STORAGE_ROOT, thrown.key)
    }

    @Test
    fun `missing PARKER_DOCUMENT_INGESTION_AUDIT_LOG_PATH throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(
            overrides = mapOf(ParkerRuntimeConfigLoader.KEY_DOCUMENT_INGESTION_AUDIT_LOG_PATH to null),
        )

        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_DOCUMENT_INGESTION_AUDIT_LOG_PATH, thrown.key)
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
    fun `missing PARKER_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH throws MissingConfiguration naming that key`() {
        val environment = fullEnvironment(
            overrides = mapOf(ParkerRuntimeConfigLoader.KEY_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH to null),
        )
        val thrown = assertFailsWith<ParkerRuntimeException.MissingConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH, thrown.key)
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

    // ================= Owner LAN Evidence Upload =================

    @Test
    fun `with neither PARKER_OWNER_HTTP_PORT nor PARKER_OWNER_HTTP_TOKEN set, the owner HTTP feature stays entirely off`() {
        val config = ParkerRuntimeConfigLoader.load(fullEnvironment())

        assertEquals("0.0.0.0", config.ownerHttpBindAddress)
        assertEquals(null, config.ownerHttpPort)
        assertEquals(null, config.ownerHttpToken)
    }

    @Test
    fun `setting only PARKER_OWNER_HTTP_PORT without a token throws InvalidConfiguration`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_PORT to "8080"))

        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_TOKEN, thrown.key)
    }

    @Test
    fun `setting only PARKER_OWNER_HTTP_TOKEN without a port throws InvalidConfiguration`() {
        val environment = fullEnvironment(overrides = mapOf(ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_TOKEN to "secret-token"))

        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_PORT, thrown.key)
    }

    @Test
    fun `setting both PARKER_OWNER_HTTP_PORT and PARKER_OWNER_HTTP_TOKEN enables the feature with those exact values`() {
        val environment = fullEnvironment(
            overrides = mapOf(
                ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_PORT to "8080",
                ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_TOKEN to "secret-token",
            ),
        )

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals(8080, config.ownerHttpPort)
        assertEquals("secret-token", config.ownerHttpToken)
    }

    @Test
    fun `a non-numeric PARKER_OWNER_HTTP_PORT throws InvalidConfiguration naming that key`() {
        val environment = fullEnvironment(
            overrides = mapOf(
                ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_PORT to "not-a-port",
                ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_TOKEN to "secret-token",
            ),
        )

        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(environment)
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_PORT, thrown.key)
    }

    @Test
    fun `a PARKER_OWNER_HTTP_PORT out of the valid TCP port range throws InvalidConfiguration`() {
        val environment = fullEnvironment(
            overrides = mapOf(
                ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_PORT to "99999",
                ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_TOKEN to "secret-token",
            ),
        )

        assertIs<ParkerRuntimeException.InvalidConfiguration>(
            assertFailsWith<ParkerRuntimeException> { ParkerRuntimeConfigLoader.load(environment) },
        )
    }

    @Test
    fun `an explicit PARKER_OWNER_HTTP_BIND_ADDRESS overrides the 0-0-0-0 default`() {
        val environment = fullEnvironment(
            overrides = mapOf(
                ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_BIND_ADDRESS to "127.0.0.1",
                ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_PORT to "8080",
                ParkerRuntimeConfigLoader.KEY_OWNER_HTTP_TOKEN to "secret-token",
            ),
        )

        val config = ParkerRuntimeConfigLoader.load(environment)

        assertEquals("127.0.0.1", config.ownerHttpBindAddress)
    }

    @Test
    fun `external transcription is disabled with no profile path by default`() {
        val config = ParkerRuntimeConfigLoader.load(fullEnvironment())

        assertEquals(false, config.openAiExternalTranscriptionEnabled)
        assertEquals(null, config.openAiExternalTranscriptionProviderProfilePath)
    }

    @Test
    fun `external transcription enablement and credential-free profile path load explicitly`() {
        val config = ParkerRuntimeConfigLoader.load(
            fullEnvironment(
                mapOf(
                    ParkerRuntimeConfigLoader.KEY_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED to "true",
                    ParkerRuntimeConfigLoader.KEY_OPENAI_EXTERNAL_TRANSCRIPTION_PROVIDER_PROFILE_PATH to "/reviewed/profile.properties",
                ),
            ),
        )

        assertEquals(true, config.openAiExternalTranscriptionEnabled)
        assertEquals("/reviewed/profile.properties", config.openAiExternalTranscriptionProviderProfilePath)
    }

    @Test
    fun `invalid external transcription enablement flag fails closed`() {
        val thrown = assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
            ParkerRuntimeConfigLoader.load(
                fullEnvironment(mapOf(ParkerRuntimeConfigLoader.KEY_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED to "yes")),
            )
        }
        assertEquals(ParkerRuntimeConfigLoader.KEY_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED, thrown.key)
    }

    @Test
    fun `OpenAI credential is missing for absent empty or whitespace environment values`() {
        listOf<String?>(null, "", "   ").forEach { value ->
            val config = ParkerRuntimeConfigLoader.load(
                fullEnvironment(mapOf(ParkerRuntimeConfigLoader.KEY_OPENAI_API_KEY to value)),
            )
            assertEquals(null, config.openAiApiCredential)
        }
    }

    @Test
    fun `fake OpenAI credential loads only into the non-printing wrapper`() {
        val sentinel = "unit-g-fake-secret-sentinel"
        val config = ParkerRuntimeConfigLoader.load(
            fullEnvironment(mapOf(ParkerRuntimeConfigLoader.KEY_OPENAI_API_KEY to sentinel)),
        )

        assertTrue(config.openAiApiCredential != null)
        assertTrue(!config.toString().contains(sentinel))
        assertTrue(!config.openAiApiCredential.toString().contains(sentinel))
        assertEquals(sentinel, config.openAiApiCredential?.useValue { it })
    }

    @Test
    fun `ordinary region ingestion loads exact coupled roots only with complete provider and durable configuration`() {
        val authority = Files.createTempDirectory("ordinary-acceptance-authority")
        val attempt = Files.createTempDirectory("ordinary-attempt")
        val provider = Files.createTempDirectory("ordinary-provider-state")
        val capability = Files.createTempDirectory("ordinary-capability-acceptance")
        val authorization = Files.createTempDirectory("ordinary-owner-authorization")
        val config = ParkerRuntimeConfigLoader.load(fullEnvironment(mapOf(
            ParkerRuntimeConfigLoader.KEY_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED to "true",
            ParkerRuntimeConfigLoader.KEY_OPENAI_API_KEY to "offline-config-test-only",
            ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT to authority.toString(),
            ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT to attempt.toString(),
            ParkerRuntimeConfigLoader.KEY_REGION_PROVIDER_STATE_STORAGE_ROOT to provider.toString(),
            ParkerRuntimeConfigLoader.KEY_PRODUCTION_COMMIT to "a".repeat(40),
            ParkerRuntimeConfigLoader.KEY_SOURCE_COMMIT to "a".repeat(40),
            ParkerRuntimeConfigLoader.KEY_ORDINARY_REGION_INGESTION_ENABLED to "true",
            ParkerRuntimeConfigLoader.KEY_ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORAGE_ROOT to capability.toString(),
            ParkerRuntimeConfigLoader.KEY_ORDINARY_REGION_OWNER_AUTHORIZATION_STORAGE_ROOT to authorization.toString(),
        )))
        assertTrue(config.ordinaryRegionIngestionEnabled)
        assertEquals(capability.toString(), config.ordinaryRegionCapabilityAcceptanceStorageRootPath)
        assertEquals(authorization.toString(), config.ordinaryRegionOwnerAuthorizationStorageRootPath)
    }

    @Test
    fun `ordinary region ingestion enablement with any missing coupled dependency fails closed`() {
        val complete = mapOf(
            ParkerRuntimeConfigLoader.KEY_OPENAI_EXTERNAL_TRANSCRIPTION_ENABLED to "true",
            ParkerRuntimeConfigLoader.KEY_OPENAI_API_KEY to "offline-config-test-only",
            ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT to "/acceptance",
            ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT to "/attempt",
            ParkerRuntimeConfigLoader.KEY_REGION_PROVIDER_STATE_STORAGE_ROOT to "/provider",
            ParkerRuntimeConfigLoader.KEY_PRODUCTION_COMMIT to "a".repeat(40),
            ParkerRuntimeConfigLoader.KEY_SOURCE_COMMIT to "a".repeat(40),
            ParkerRuntimeConfigLoader.KEY_ORDINARY_REGION_INGESTION_ENABLED to "true",
            ParkerRuntimeConfigLoader.KEY_ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORAGE_ROOT to "/capability",
            ParkerRuntimeConfigLoader.KEY_ORDINARY_REGION_OWNER_AUTHORIZATION_STORAGE_ROOT to "/authorization",
        )
        listOf(
            ParkerRuntimeConfigLoader.KEY_OPENAI_API_KEY,
            ParkerRuntimeConfigLoader.KEY_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT,
            ParkerRuntimeConfigLoader.KEY_REGION_PROVIDER_STATE_STORAGE_ROOT,
            ParkerRuntimeConfigLoader.KEY_SOURCE_COMMIT,
            ParkerRuntimeConfigLoader.KEY_ORDINARY_REGION_CAPABILITY_ACCEPTANCE_STORAGE_ROOT,
            ParkerRuntimeConfigLoader.KEY_ORDINARY_REGION_OWNER_AUTHORIZATION_STORAGE_ROOT,
        ).forEach { missing ->
            assertFailsWith<ParkerRuntimeException.InvalidConfiguration> {
                ParkerRuntimeConfigLoader.load(fullEnvironment(complete + (missing to null)))
            }
        }
    }
}
