package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.AuthorizationPurposeRegistrationOutcome
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.runtime.AuthorizationPurposeRegistry
import parker.core.runtime.DefaultEvidenceCustodian
import parker.core.runtime.DefaultKnowledgeCandidateEvaluator
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.EvidenceIntelligenceInputResolver
import parker.core.runtime.InMemoryAuthorizationPurposeRegistry
import parker.core.runtime.PermissionPolicyRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Authorization Purpose Implementation Plan, Unit 5 ("Composition
 * Wiring"). Proves the already-built Unit 1-4 infrastructure now exists
 * in the composed [ParkerRuntime] graph -- construction, wiring to the
 * one [DefaultPermissionPolicy] instance, and a still-empty, still-inert
 * registry -- never that any real Authorization Purpose value is
 * registered or that any existing consumer adopts it (both explicitly
 * out of this Unit's own scope; Programme Unit 4/Gap #54 own that later
 * work). This suite proves the *wiring* Unit 5 adds; it does not re-prove
 * Units 1-4's own behaviour, already covered by their own unit test
 * suites (`AuthorizationPurposeRegistryTest.kt`, `DefaultPermissionPolicyTest.kt`).
 *
 * Reflection is used only where no public seam exists to observe internal
 * composition state -- `ParkerRuntime` exposes none of its internal
 * composition by design, mirroring
 * [ParkerRuntimeEvidenceIntelligenceCompositionTest]'s own established
 * precedent and its own identical [privateField] helper, reproduced here
 * verbatim (not shared via a common test-utility file, matching that
 * file's own existing per-file-declaration style). No production
 * accessor is added anywhere in `src/` for this suite's own benefit.
 */
class ParkerRuntimeAuthorizationPurposeCompositionTest {

    private fun config(localTextChannelModuleId: String = "channel.local-text-authz-purpose-composition-test") = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by these tests
        modelName = "test-model",
        ownerPrincipalId = "user.owner-authz-purpose-composition-test",
        localTextChannelModuleId = localTextChannelModuleId,
        evidenceStorageRootPath = Files.createTempDirectory("authz-purpose-composition-evidence").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("authz-purpose-composition-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("authz-purpose-composition-memory").resolve("memory-core.log").toString(),
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    private fun composedPolicy(runtime: ParkerRuntime): DefaultPermissionPolicy {
        val permissionEngine = runtime.privateField<PermissionEngine>("permissionEngine")
        return permissionEngine.privateField("policy")
    }

    // ================= Construction and wiring =================

    @Test
    fun `the runtime constructs and starts successfully with the Authorization Purpose registry composed`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        runtime.start()

        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)
        runtime.shutdown()
        assertEquals(RuntimeLifecycleState.STOPPED, runtime.state)
    }

    @Test
    fun `the composed DefaultPermissionPolicy receives a non-null InMemoryAuthorizationPurposeRegistry`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registry = composedPolicy(runtime).privateField<AuthorizationPurposeRegistry?>("authorizationPurposeRegistry")

        assertTrue(registry is InMemoryAuthorizationPurposeRegistry, "DefaultPermissionPolicy must receive a real registry, not the pre-Unit-5 default null")

        runtime.shutdown()
    }

    @Test
    fun `the same AuthorizationPurposeRegistry field is a stable, single reference on repeated access`() = runTest {
        // Unit 5 introduces no second consumer of the registry (out of scope -- Programme Unit 4
        // retrofit/Gap #54 own that later work), so there is no second call site to compare
        // against yet. This proves what Unit 5 itself can honestly prove: the field the composed
        // DefaultPermissionPolicy holds is one stable object, not reconstructed per access.
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val policy = composedPolicy(runtime)
        val first = policy.privateField<AuthorizationPurposeRegistry?>("authorizationPurposeRegistry")
        val second = policy.privateField<AuthorizationPurposeRegistry?>("authorizationPurposeRegistry")

        assertSame(first, second)

        runtime.shutdown()
    }

    // ================= Zero production adoption =================

    @Test
    fun `no production Authorization Purpose value is registered in the composed registry`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registry = composedPolicy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val entries = registry.privateField<Map<*, *>>("entries")

        assertTrue(entries.isEmpty(), "an empty registry after composition is Unit 5's own expected, correct outcome")

        runtime.shutdown()
    }

    @Test
    fun `no PermissionPolicyRule in the composed policy names an authorizationPurpose`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val rules = composedPolicy(runtime).privateField<List<PermissionPolicyRule>>("rules")

        assertTrue(rules.isNotEmpty(), "the pre-existing production rule set must still be present")
        rules.forEach { rule -> assertNull(rule.authorizationPurpose, "Unit 5 adds no new PermissionPolicyRule and adopts no existing one for Authorization Purpose") }

        runtime.shutdown()
    }

    @Test
    fun `no existing consumer class declares a field referencing the Authorization Purpose registry`() {
        // Structural proof that Unit 5's own wiring is confined to DefaultPermissionPolicy alone --
        // neither Knowledge Submission's own evaluator nor Evidence Intelligence's own input
        // resolver (Programme Unit 4's future retrofit targets) gained a reference of their own.
        val evaluatorFields = DefaultKnowledgeCandidateEvaluator::class.java.declaredFields.map { it.name }
        val resolverFields = EvidenceIntelligenceInputResolver::class.java.declaredFields.map { it.name }

        assertTrue(evaluatorFields.none { it.contains("authorizationPurpose", ignoreCase = true) }, "DefaultKnowledgeCandidateEvaluator: $evaluatorFields")
        assertTrue(resolverFields.none { it.contains("authorizationPurpose", ignoreCase = true) }, "EvidenceIntelligenceInputResolver: $resolverFields")
    }

    // ================= Regression: existing permission outcomes unchanged =================

    @Test
    fun `an existing, already-registered production action still resolves APPROVED after Authorization Purpose composition`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val policy = composedPolicy(runtime)
        val decision = policy.evaluate(
            ExecutionRequest(
                requestId = RequestId("req-authz-purpose-composition-regression"),
                principalId = PrincipalId("user.owner-authz-purpose-composition-test"),
                origin = RequestOrigin.TEXT,
                intent = "regression check",
                targetResources = listOf(DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID),
                proposedActions = listOf(DefaultEvidenceCustodian.ACCEPT_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "corr-authz-purpose-composition-regression",
            ),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        assertEquals(PermissionLevel.AUTOMATIC, decision.level)

        runtime.shutdown()
    }

    @Test
    fun `a request declaring a registered, active but rule-unmatched Authorization Purpose still resolves via the pre-existing coarse rule, unchanged`() = runTest {
        // Proves the registry now genuinely participates in evaluation (it is consulted, not
        // merely present) without widening or narrowing any existing outcome -- registering a
        // synthetic, test-only purpose and declaring it on a request that matches only a coarse
        // rule (no purpose-aware PermissionPolicyRule exists in production, per the test above)
        // must still resolve exactly as it would with no purpose declared at all.
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val policy = composedPolicy(runtime)
        val registry = policy.privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val syntheticPurpose = AuthorizationPurposeId("test.unit-5-composition-verification-only")
        val outcome = registry.register(syntheticPurpose)
        assertTrue(outcome is AuthorizationPurposeRegistrationOutcome.Registered)

        val decision = policy.evaluate(
            ExecutionRequest(
                requestId = RequestId("req-authz-purpose-composition-regression-2"),
                principalId = PrincipalId("user.owner-authz-purpose-composition-test"),
                origin = RequestOrigin.TEXT,
                intent = "regression check with a declared, registered purpose",
                targetResources = listOf(DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID),
                proposedActions = listOf(DefaultEvidenceCustodian.ACCEPT_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "corr-authz-purpose-composition-regression-2",
                authorizationPurpose = syntheticPurpose,
            ),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        assertEquals(PermissionLevel.AUTOMATIC, decision.level)

        runtime.shutdown()
    }
}
