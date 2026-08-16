package parker.composition

/**
 * Sprint 10, Unit 4 (Production Composition Root). The runtime's own
 * configuration, field-shaped. Every field here is either required with
 * no invented default (a missing value is a startup failure, not a
 * silently-guessed one) or optional with an explicitly-stated default,
 * never both.
 *
 * @param modelEndpointUrl The real model server's HTTP endpoint, passed
 *   unchanged to `LocalHttpModelInferenceClient`. Required -- this
 *   composition root does not invent a default endpoint, since guessing
 *   one would mean silently pointing production traffic at an address no
 *   one configured.
 * @param modelName Passed unchanged to `LocalHttpModelInferenceClient`.
 *   Required, for the same reason as [modelEndpointUrl].
 * @param modelTimeoutMs Passed unchanged to `ModelReasoningProvider`'s own
 *   `timeoutMs` constructor parameter. Optional -- defaults to
 *   `ModelReasoningProvider`'s own existing default (`30_000L`), restated
 *   explicitly here rather than left to that class's default so this
 *   config's own shape is self-describing.
 * @param ownerPrincipalId The household owner's own [parker.core.interfaces.PrincipalId]
 *   value, registered with `IdentityService` at startup as a `USER`
 *   Principal. Required -- this composition root does not invent an
 *   owner identity.
 * @param ownerDisplayName Optional, defaults to `"Owner"` -- display-only,
 *   carries no trust or routing meaning.
 * @param localTextChannelModuleId The [parker.core.interfaces.ModuleId] value
 *   the Local Text Channel module is registered under. Optional, defaults
 *   to `"channel.local-text"`, matching the value already used throughout
 *   this repository's own existing Local Text Channel tests
 *   (`ResponseDeliveryTest.kt`, `LocalTextChannelDeliverToolTest.kt`).
 * @param evidenceStorageRootPath Evidence Custodian Runtime Integration
 *   (Implementation Plan Phase 10). An already-existing, writable
 *   directory, passed unchanged to `FileSystemEvidenceArtifactStorage`.
 *   Required -- this composition root does not invent a default evidence
 *   storage location, for the same reason [modelEndpointUrl] and
 *   [ownerPrincipalId] are required: guessing one would mean silently
 *   pointing production evidence storage at a location no one configured.
 * @param evidenceDeletionAuditLogPath Evidence Custodian Runtime
 *   Integration (Implementation Plan Phase 10). The exact file path
 *   (not a directory) passed unchanged to `FileSystemEvidenceDeletionAudit`
 *   -- its parent directory must already exist and be writable; the file
 *   itself is created if missing. Required, for the same reason
 *   [evidenceStorageRootPath] is required.
 * @param memoryCoreDurabilityLogPath Memory Core Durability, Unit 8
 *   (Runtime Composition). The exact file path (not a directory) passed
 *   unchanged to `FileSystemMemoryCoreDurabilityLog` -- its parent
 *   directory must already exist and be writable; the file itself is
 *   created if missing, mirroring [evidenceDeletionAuditLogPath]'s own
 *   exact shape. Required, for the same reason [evidenceStorageRootPath]
 *   is required -- this composition root does not invent a default
 *   location for durable Memory Core records.
 * @param logLevel Console display threshold, eventually passed to
 *   `ConsoleParkerLogger`'s own `minLevel` constructor parameter -- a
 *   presentation-layer filter only, never a decision about which events
 *   `RuntimeEventLogger` itself produces (those are unconditional; see
 *   `ConsoleParkerLogger`'s own KDoc). Optional, defaults to
 *   [LogLevel.INFO] here -- this loader has no notion of "interactive"
 *   (it only ever sees an environment map), so this is its own single,
 *   mode-agnostic default. `Main.kt`'s own `resolveEffectiveLogLevel`
 *   resolves the actual, mode-sensitive default (`WARN` for
 *   `--interactive`, `INFO` headless) separately, using this value only
 *   when `PARKER_LOG_LEVEL` was genuinely absent/blank *and* an
 *   explicitly-set value (this field, when the key was present) always
 *   wins regardless of mode.
 */
data class ParkerRuntimeConfig(
    val modelEndpointUrl: String,
    val modelName: String,
    val modelTimeoutMs: Long = 30_000L,
    val ownerPrincipalId: String,
    val ownerDisplayName: String = "Owner",
    val localTextChannelModuleId: String = "channel.local-text",
    val evidenceStorageRootPath: String,
    val evidenceDeletionAuditLogPath: String,
    val memoryCoreDurabilityLogPath: String,
    val knowledgeItemDurabilityLogPath: String,
    val logLevel: LogLevel = LogLevel.INFO,
)

/**
 * Loads a [ParkerRuntimeConfig] from a supplied key/value environment map
 * -- **not** from `System.getenv()` directly inside this function, so a
 * caller (production `main`, or a test) supplies the source explicitly
 * (constructor/parameter injection, not a global lookup, per this Unit's
 * own governing instruction). A real production `main` passes
 * `System.getenv()` itself; this object never reads it on its own
 * initiative.
 *
 * Every required key missing, or present but blank, is reported as one
 * [ParkerRuntimeException.MissingConfiguration] -- "missing configuration"
 * is a named, first-class startup failure this Unit is required to handle
 * (task instruction), not a `NullPointerException` surfacing from deep
 * inside dependency construction.
 */
object ParkerRuntimeConfigLoader {

    const val KEY_MODEL_ENDPOINT_URL = "PARKER_MODEL_ENDPOINT_URL"
    const val KEY_MODEL_NAME = "PARKER_MODEL_NAME"
    const val KEY_MODEL_TIMEOUT_MS = "PARKER_MODEL_TIMEOUT_MS"
    const val KEY_OWNER_PRINCIPAL_ID = "PARKER_OWNER_PRINCIPAL_ID"
    const val KEY_OWNER_DISPLAY_NAME = "PARKER_OWNER_DISPLAY_NAME"
    const val KEY_LOCAL_TEXT_CHANNEL_MODULE_ID = "PARKER_LOCAL_TEXT_CHANNEL_MODULE_ID"
    const val KEY_EVIDENCE_STORAGE_ROOT = "PARKER_EVIDENCE_STORAGE_ROOT"
    const val KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH = "PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH"
    const val KEY_MEMORY_CORE_DURABILITY_LOG_PATH = "PARKER_MEMORY_CORE_DURABILITY_LOG_PATH"
    const val KEY_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH = "PARKER_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH"
    const val KEY_LOG_LEVEL = "PARKER_LOG_LEVEL"

    fun load(environment: Map<String, String>): ParkerRuntimeConfig {
        val modelTimeoutMsRaw = environment[KEY_MODEL_TIMEOUT_MS]?.takeIf { it.isNotBlank() }
        val modelTimeoutMs = if (modelTimeoutMsRaw == null) {
            30_000L
        } else {
            modelTimeoutMsRaw.toLongOrNull()
                ?: throw ParkerRuntimeException.InvalidConfiguration(
                    KEY_MODEL_TIMEOUT_MS,
                    "must be a positive integer number of milliseconds; was '$modelTimeoutMsRaw'",
                )
        }
        if (modelTimeoutMs <= 0) {
            throw ParkerRuntimeException.InvalidConfiguration(
                KEY_MODEL_TIMEOUT_MS,
                "must be a positive integer number of milliseconds; was '$modelTimeoutMsRaw'",
            )
        }

        val logLevelRaw = environment[KEY_LOG_LEVEL]?.takeIf { it.isNotBlank() }
        val logLevel = if (logLevelRaw == null) {
            LogLevel.INFO
        } else {
            try {
                LogLevel.valueOf(logLevelRaw.trim().uppercase())
            } catch (e: IllegalArgumentException) {
                throw ParkerRuntimeException.InvalidConfiguration(
                    KEY_LOG_LEVEL,
                    "must be one of DEBUG, INFO, WARN, ERROR, OFF; was '$logLevelRaw'",
                )
            }
        }

        return ParkerRuntimeConfig(
            modelEndpointUrl = requireKey(environment, KEY_MODEL_ENDPOINT_URL),
            modelName = requireKey(environment, KEY_MODEL_NAME),
            modelTimeoutMs = modelTimeoutMs,
            ownerPrincipalId = requireKey(environment, KEY_OWNER_PRINCIPAL_ID),
            ownerDisplayName = environment[KEY_OWNER_DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: "Owner",
            localTextChannelModuleId = environment[KEY_LOCAL_TEXT_CHANNEL_MODULE_ID]?.takeIf { it.isNotBlank() }
                ?: "channel.local-text",
            evidenceStorageRootPath = requireKey(environment, KEY_EVIDENCE_STORAGE_ROOT),
            evidenceDeletionAuditLogPath = requireKey(environment, KEY_EVIDENCE_DELETION_AUDIT_LOG_PATH),
            memoryCoreDurabilityLogPath = requireKey(environment, KEY_MEMORY_CORE_DURABILITY_LOG_PATH),
            knowledgeItemDurabilityLogPath = requireKey(environment, KEY_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH),
            logLevel = logLevel,
        )
    }

    private fun requireKey(environment: Map<String, String>, key: String): String =
        environment[key]?.takeIf { it.isNotBlank() } ?: throw ParkerRuntimeException.MissingConfiguration(key)
}
