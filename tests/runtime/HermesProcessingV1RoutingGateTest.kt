package parker.core.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HermesProcessingV1RoutingGateTest {
    @Test
    fun `routing is disabled by default and reachability cannot enable it`() {
        val config = HermesProcessingV1RoutingConfig.fromEnvironment(emptyMap())
        assertEquals(false, config.enabled)
        assertEquals(HermesProcessingV1RouteDecision.DISABLED, config.decide("text/plain"))
    }

    @Test
    fun `enabled route requires all fixed transport configuration`() {
        assertFailsWith<IllegalArgumentException> {
            HermesProcessingV1RoutingConfig.fromEnvironment(mapOf("HERMES_PROCESSING_V1_ENABLED" to "true"))
        }
        val config = HermesProcessingV1RoutingConfig.fromEnvironment(
            mapOf(
                "HERMES_PROCESSING_V1_ENABLED" to "true",
                "HERMES_PROCESSING_V1_ENDPOINT_HOST" to "192.168.178.45",
                "HERMES_PROCESSING_V1_KEY_PATH" to "/run/secrets/hermes-processing-key",
                "HERMES_PROCESSING_V1_KNOWN_HOSTS_PATH" to "/run/secrets/hermes-processing-known-hosts",
            ),
        )
        assertEquals(HermesProcessingV1RouteDecision.REMOTE_NATIVE_OR_STRUCTURED, config.decide("text/plain"))
        assertEquals(HermesProcessingV1RouteDecision.UNSUPPORTED, config.decide("image/png"))
    }

    @Test
    fun `malformed switch value fails closed`() {
        assertFailsWith<IllegalArgumentException> {
            HermesProcessingV1RoutingConfig.fromEnvironment(mapOf("HERMES_PROCESSING_V1_ENABLED" to "yes"))
        }
    }
}
