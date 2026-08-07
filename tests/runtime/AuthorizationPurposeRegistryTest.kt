package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.AuthorizationPurposeRegistrationOutcome
import parker.core.interfaces.AuthorizationPurposeRetirementOutcome
import parker.core.interfaces.AuthorizationPurposeStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthorizationPurposeRegistryTest {

    private fun registry() = InMemoryAuthorizationPurposeRegistry()

    // -- Successful registration --

    @Test
    fun `a domain-namespaced value registers successfully`() = runTest {
        val outcome = registry().register(AuthorizationPurposeId("knowledge-memory.candidate-evaluation"))

        assertEquals(
            AuthorizationPurposeRegistrationOutcome.Registered(AuthorizationPurposeId("knowledge-memory.candidate-evaluation")),
            outcome,
        )
    }

    @Test
    fun `a plugin-namespaced value registers successfully`() = runTest {
        val outcome = registry().register(AuthorizationPurposeId("plugin:irrigation-plugin:schedule-adjustment"))

        assertEquals(
            AuthorizationPurposeRegistrationOutcome.Registered(AuthorizationPurposeId("plugin:irrigation-plugin:schedule-adjustment")),
            outcome,
        )
    }

    @Test
    fun `a newly registered value is active and reachable via lookup`() = runTest {
        val reg = registry()
        val id = AuthorizationPurposeId("evidence-intelligence.input-resolution")
        reg.register(id)

        assertEquals(AuthorizationPurposeStatus.ACTIVE, reg.lookup(id)?.status)
        assertTrue(reg.isActive(id))
    }

    // -- Duplicate rejection --

    @Test
    fun `re-registering an already-active value is idempotent, not an error`() = runTest {
        val reg = registry()
        val id = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        reg.register(id)

        val second = reg.register(id)

        assertEquals(AuthorizationPurposeRegistrationOutcome.AlreadyRegistered(id), second)
    }

    @Test
    fun `re-registering a retired value is rejected, never silently reactivated`() = runTest {
        val reg = registry()
        val id = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        reg.register(id)
        reg.retire(id)

        val outcome = reg.register(id)

        assertTrue(outcome is AuthorizationPurposeRegistrationOutcome.Rejected)
        assertEquals(AuthorizationPurposeStatus.RETIRED, reg.lookup(id)?.status)
    }

    // -- Namespace validation --

    @Test
    fun `a value with no namespace separator is rejected`() = runTest {
        val outcome = registry().register(AuthorizationPurposeId("no-namespace-at-all"))

        assertTrue(outcome is AuthorizationPurposeRegistrationOutcome.Rejected)
    }

    @Test
    fun `a domain-namespaced value with a blank domain segment is rejected`() = runTest {
        val outcome = registry().register(AuthorizationPurposeId(".purpose-only"))

        assertTrue(outcome is AuthorizationPurposeRegistrationOutcome.Rejected)
    }

    @Test
    fun `a domain-namespaced value with a blank purpose segment is rejected`() = runTest {
        val outcome = registry().register(AuthorizationPurposeId("domain-only."))

        assertTrue(outcome is AuthorizationPurposeRegistrationOutcome.Rejected)
    }

    @Test
    fun `a plugin-namespaced value missing the second colon is rejected`() = runTest {
        val outcome = registry().register(AuthorizationPurposeId("plugin:irrigation-plugin.schedule-adjustment"))

        assertTrue(outcome is AuthorizationPurposeRegistrationOutcome.Rejected)
    }

    @Test
    fun `a plugin-namespaced value with a blank pluginId is rejected`() = runTest {
        val outcome = registry().register(AuthorizationPurposeId("plugin::schedule-adjustment"))

        assertTrue(outcome is AuthorizationPurposeRegistrationOutcome.Rejected)
    }

    @Test
    fun `a plugin-namespaced value with a blank purpose is rejected`() = runTest {
        val outcome = registry().register(AuthorizationPurposeId("plugin:irrigation-plugin:"))

        assertTrue(outcome is AuthorizationPurposeRegistrationOutcome.Rejected)
    }

    @Test
    fun `a rejected registration does not register the value`() = runTest {
        val reg = registry()
        val id = AuthorizationPurposeId("no-namespace-at-all")

        reg.register(id)

        assertNull(reg.lookup(id))
        assertTrue(!reg.isActive(id))
    }

    // -- Lookup --

    @Test
    fun `lookup of a never-registered value returns null`() = runTest {
        assertNull(registry().lookup(AuthorizationPurposeId("knowledge-memory.never-registered")))
    }

    @Test
    fun `isActive of a never-registered value is false`() = runTest {
        assertTrue(!registry().isActive(AuthorizationPurposeId("knowledge-memory.never-registered")))
    }

    // -- Retirement behaviour --

    @Test
    fun `retiring an active value succeeds and marks it retired, never deleted`() = runTest {
        val reg = registry()
        val id = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        reg.register(id)

        val outcome = reg.retire(id)

        assertEquals(AuthorizationPurposeRetirementOutcome.Retired(id), outcome)
        assertEquals(AuthorizationPurposeStatus.RETIRED, reg.lookup(id)?.status)
    }

    @Test
    fun `a retired value is no longer active but remains looked up`() = runTest {
        val reg = registry()
        val id = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        reg.register(id)
        reg.retire(id)

        assertTrue(!reg.isActive(id))
        assertEquals(id, reg.lookup(id)?.id)
    }

    @Test
    fun `retiring an already-retired value is AlreadyRetired, not an error`() = runTest {
        val reg = registry()
        val id = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        reg.register(id)
        reg.retire(id)

        val second = reg.retire(id)

        assertEquals(AuthorizationPurposeRetirementOutcome.AlreadyRetired(id), second)
    }

    @Test
    fun `retiring a never-registered value is rejected`() = runTest {
        val outcome = registry().retire(AuthorizationPurposeId("knowledge-memory.never-registered"))

        assertTrue(outcome is AuthorizationPurposeRetirementOutcome.Rejected)
    }

    // -- Immutability --

    @Test
    fun `no mechanism exists to change a registered value's own meaning short of retirement`() = runTest {
        val reg = registry()
        val id = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        reg.register(id)

        // The only state transition available is retire() -- confirmed structurally by this
        // registry's own public interface carrying no update/replace method of any kind.
        val beforeRetire = reg.lookup(id)
        assertEquals(AuthorizationPurposeStatus.ACTIVE, beforeRetire?.status)

        reg.retire(id)
        val afterRetire = reg.lookup(id)
        assertEquals(id, afterRetire?.id)
        assertEquals(AuthorizationPurposeStatus.RETIRED, afterRetire?.status)
    }
}
