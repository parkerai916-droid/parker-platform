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
 */
data class ParkerRuntimeConfig(
    val modelEndpointUrl: String,
    val modelName: String,
    val modelTimeoutMs: Long = 30_000L,
    val ownerPrincipalId: String,
    val ownerDisplayName: String = "Owner",
    val localTextChannelModuleId: String = "channel.local-text",
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

        return ParkerRuntimeConfig(
            modelEndpointUrl = requireKey(environment, KEY_MODEL_ENDPOINT_URL),
            modelName = requireKey(environment, KEY_MODEL_NAME),
            modelTimeoutMs = modelTimeoutMs,
            ownerPrincipalId = requireKey(environment, KEY_OWNER_PRINCIPAL_ID),
            ownerDisplayName = environment[KEY_OWNER_DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: "Owner",
            localTextChannelModuleId = environment[KEY_LOCAL_TEXT_CHANNEL_MODULE_ID]?.takeIf { it.isNotBlank() }
                ?: "channel.local-text",
        )
    }

    private fun requireKey(environment: Map<String, String>, key: String): String =
        environment[key]?.takeIf { it.isNotBlank() } ?: throw ParkerRuntimeException.MissingConfiguration(key)
}
