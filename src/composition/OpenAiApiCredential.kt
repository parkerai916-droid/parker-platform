package parker.composition

/**
 * Narrow, non-printing runtime wrapper for the deployment-supplied OpenAI credential.
 * Equality and hashing intentionally retain identity semantics; the value is not serializable.
 */
class OpenAiApiCredential private constructor(private val characters: CharArray) {
    override fun toString(): String = "OpenAiApiCredential([REDACTED])"

    /** Sole future adapter/composition boundary; callers cannot retain the backing character array. */
    internal fun <T> useValue(block: (String) -> T): T = block(String(characters))

    companion object {
        internal fun fromEnvironment(value: String?): OpenAiApiCredential? =
            value?.takeIf { it.isNotBlank() }?.let { OpenAiApiCredential(it.toCharArray()) }
    }
}

sealed interface OpenAiExternalTranscriptionBackendReadiness {
    data object Disabled : OpenAiExternalTranscriptionBackendReadiness
    data class ProfileNotReady(val profileReadiness: OpenAiExternalTranscriptionReadiness) :
        OpenAiExternalTranscriptionBackendReadiness
    data object MissingCredential : OpenAiExternalTranscriptionBackendReadiness
    data object Ready : OpenAiExternalTranscriptionBackendReadiness
}

internal fun externalTranscriptionBackendReadiness(
    profileReadiness: OpenAiExternalTranscriptionReadiness,
    credential: OpenAiApiCredential?,
): OpenAiExternalTranscriptionBackendReadiness = when (profileReadiness) {
    OpenAiExternalTranscriptionReadiness.Disabled -> OpenAiExternalTranscriptionBackendReadiness.Disabled
    is OpenAiExternalTranscriptionReadiness.InvalidProfile,
    is OpenAiExternalTranscriptionReadiness.StaleProfile,
    -> OpenAiExternalTranscriptionBackendReadiness.ProfileNotReady(profileReadiness)
    is OpenAiExternalTranscriptionReadiness.Ready -> if (credential == null) {
        OpenAiExternalTranscriptionBackendReadiness.MissingCredential
    } else {
        OpenAiExternalTranscriptionBackendReadiness.Ready
    }
}
