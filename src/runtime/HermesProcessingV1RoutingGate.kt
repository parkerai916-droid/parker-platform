package parker.core.runtime

/** Parker-side decision: availability never enables this route by itself. */
enum class HermesProcessingV1RouteDecision {
    DISABLED,
    REMOTE_NATIVE_OR_STRUCTURED,
    UNSUPPORTED,
}

data class HermesProcessingV1RoutingConfig(
    val enabled: Boolean = false,
    val endpointHost: String? = null,
    val keyPath: String? = null,
    val knownHostsPath: String? = null,
) {
    fun validateForEnabledRoute() {
        if (!enabled) return
        require(!endpointHost.isNullOrBlank()) { "Hermes v1 endpoint host is required when routing is enabled" }
        require(!keyPath.isNullOrBlank()) { "Hermes v1 key path is required when routing is enabled" }
        require(!knownHostsPath.isNullOrBlank()) { "Hermes v1 known-hosts path is required when routing is enabled" }
    }

    fun decide(mediaType: String): HermesProcessingV1RouteDecision {
        if (!enabled) return HermesProcessingV1RouteDecision.DISABLED
        return if (HermesV1ClientMethodSelection.forMediaType(mediaType) == null) {
            HermesProcessingV1RouteDecision.UNSUPPORTED
        } else {
            HermesProcessingV1RouteDecision.REMOTE_NATIVE_OR_STRUCTURED
        }
    }

    companion object {
        const val KEY_ENABLED = "HERMES_PROCESSING_V1_ENABLED"
        const val KEY_ENDPOINT_HOST = "HERMES_PROCESSING_V1_ENDPOINT_HOST"
        const val KEY_KEY_PATH = "HERMES_PROCESSING_V1_KEY_PATH"
        const val KEY_KNOWN_HOSTS_PATH = "HERMES_PROCESSING_V1_KNOWN_HOSTS_PATH"

        fun fromEnvironment(environment: Map<String, String>): HermesProcessingV1RoutingConfig {
            val raw = environment[KEY_ENABLED]?.trim()?.lowercase()
            val enabled = when (raw) {
                null, "", "false" -> false
                "true" -> true
                else -> throw IllegalArgumentException("$KEY_ENABLED must be true or false")
            }
            return HermesProcessingV1RoutingConfig(
                enabled,
                environment[KEY_ENDPOINT_HOST]?.takeIf { it.isNotBlank() },
                environment[KEY_KEY_PATH]?.takeIf { it.isNotBlank() },
                environment[KEY_KNOWN_HOSTS_PATH]?.takeIf { it.isNotBlank() },
            ).also { it.validateForEnabledRoute() }
        }
    }
}
