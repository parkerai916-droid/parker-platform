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
import parker.core.runtime.DefaultPermissionEngine
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.DefaultReasoningKnowledgeSource
import parker.core.runtime.EvidenceIntelligenceInputResolver
import parker.core.runtime.InMemoryAuthorizationPurposeRegistry
import parker.core.runtime.MemoryAdmissionCoordinator
import parker.core.runtime.PermissionPolicyRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Authorization Purpose Implementation Plan, Unit 5 ("Composition
 * Wiring") and Unit 6 ("End-to-End Verification"). Unit 5's own tests
 * (below, unmodified) prove the already-built Unit 1-4 infrastructure now
 * exists in the composed [ParkerRuntime] graph -- construction and wiring to
 * the one [DefaultPermissionPolicy] instance. Gap #54 Operationalisation
 * Unit 2 now registers exactly two real Purpose values while retaining the
 * original non-adoption guarantee: no consumer propagates either value and
 * no Purpose-specific rule exists. Unit 6's own tests extend
 * this file with the properties that specifically require the *real*
 * composed runtime rather than a test-constructed policy/engine pair:
 * single-instance structure, regression through the *full*
 * `permissionEngine.evaluate` chain (identity resolution + policy, not
 * `DefaultPermissionPolicy.evaluate` alone), and proof that Authorization
 * Purpose infrastructure alone has not accidentally authorised Gap #54's
 * still-blocked `memory.retrieve` path
 * (`docs/reviews/AUTHORIZATION_PURPOSE_UNIT_6_PLANNING_REVIEW.md` §5).
 * Neither Unit re-proves Units 1-4's own behaviour, already covered by
 * their own unit test suites (`AuthorizationPurposeRegistryTest.kt`,
 * `DefaultPermissionPolicyTest.kt`,
 * `AuthorizationPurposeEndToEndVerificationTest.kt`).
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
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("authz-purpose-composition-evidence-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("authz-purpose-composition-evidence-manifest-derivative-generation").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("authz-purpose-composition-evidence-manifest-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("authz-purpose-composition-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("authz-purpose-composition-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("knowledge-items-test").resolve("items.log").toString(),
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    private fun composedEngine(runtime: ParkerRuntime): PermissionEngine = runtime.privateField("permissionEngine")

    private fun composedPolicy(runtime: ParkerRuntime): DefaultPermissionPolicy = composedEngine(runtime).privateField("policy")

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

    // ================= Unit 2 vocabulary registration without consumer adoption =================

    @Test
    fun `exactly the three accepted Memory retrieval Authorization Purpose values are registered, all active`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registry = composedPolicy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val entries = registry.privateField<Map<*, *>>("entries")
        val candidateEvaluationPurpose = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        val evidenceIntelligencePurpose = AuthorizationPurposeId("evidence-intelligence.input-resolution")
        val reasoningContextPurpose = AuthorizationPurposeId("knowledge-memory.reasoning-context-retrieval")

        assertEquals(
            setOf(candidateEvaluationPurpose, evidenceIntelligencePurpose, reasoningContextPurpose),
            entries.keys,
        )
        assertTrue(registry.isActive(candidateEvaluationPurpose))
        assertTrue(registry.isActive(evidenceIntelligencePurpose))
        assertTrue(registry.isActive(reasoningContextPurpose))

        runtime.shutdown()
    }

    @Test
    fun `exactly the four candidate-evaluation and reasoning-context retrieval rules name an authorizationPurpose, partitioned correctly by Purpose`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val rules = composedPolicy(runtime).privateField<List<PermissionPolicyRule>>("rules")
        val candidateEvaluationPurpose = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        val evidenceIntelligencePurpose = AuthorizationPurposeId("evidence-intelligence.input-resolution")
        val reasoningContextPurpose = AuthorizationPurposeId("knowledge-memory.reasoning-context-retrieval")

        assertEquals(4, rules.count { it.authorizationPurpose != null })

        val candidateEvaluationRules = rules.filter { it.authorizationPurpose == candidateEvaluationPurpose }
        assertEquals(2, candidateEvaluationRules.size)
        candidateEvaluationRules.forEach { rule ->
            assertEquals(PermissionDecisionOutcome.APPROVED, rule.outcome)
            assertTrue(
                rule.proposedAction == PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME ||
                    rule.proposedAction == PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
            )
        }

        val reasoningContextRules = rules.filter { it.authorizationPurpose == reasoningContextPurpose }
        assertEquals(2, reasoningContextRules.size)
        reasoningContextRules.forEach { rule ->
            assertEquals(PermissionDecisionOutcome.APPROVED, rule.outcome)
            assertTrue(
                rule.proposedAction == DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME ||
                    rule.proposedAction == PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
            )
        }

        assertTrue(rules.none { it.authorizationPurpose == evidenceIntelligencePurpose }, "Evidence Intelligence must remain with zero Purpose-bound rules")

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

    // ================================================================================
    // Unit 6 ("End-to-End Verification") -- properties requiring the real, composed
    // runtime specifically. See AuthorizationPurposeEndToEndVerificationTest.kt for
    // the precedence-safety/fail-closed matrix, proven through a real, test-constructed
    // DefaultPermissionEngine/DefaultPermissionPolicy pair (the real production rule
    // list here is fixed and cannot carry a purpose-aware rule without an unauthorised
    // production change).
    // ================================================================================

    private val ownerPrincipalId = "user.owner-authz-purpose-composition-test"

    // ================= Single-authority architecture =================

    @Test
    fun `the composed permissionEngine field is the one, real DefaultPermissionEngine implementation`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        assertIs<DefaultPermissionEngine>(composedEngine(runtime), "single-authority architecture requires the one real implementation, never a second or substitute one")

        runtime.shutdown()
    }

    // ================= Regression through the FULL engine (identity resolution + policy) =================

    @Test
    fun `an existing production action still resolves APPROVED through the full, composed permissionEngine (identity plus policy)`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val decision = composedEngine(runtime).evaluate(
            ExecutionRequest(
                requestId = RequestId("req-authz-purpose-unit6-full-engine-regression"),
                principalId = PrincipalId(ownerPrincipalId),
                origin = RequestOrigin.TEXT,
                intent = "Unit 6 full-engine regression check",
                targetResources = listOf(DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID),
                proposedActions = listOf(DefaultEvidenceCustodian.ACCEPT_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "corr-authz-purpose-unit6-full-engine-regression",
            ),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        assertEquals(PermissionLevel.AUTOMATIC, decision.level)

        runtime.shutdown()
    }

    @Test
    fun `a registered, active, but rule-unmatched Authorization Purpose still resolves via the coarse rule through the full, composed permissionEngine`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registry = composedPolicy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val syntheticPurpose = AuthorizationPurposeId("test.unit-6-full-engine-absent-widening-check")
        assertTrue(registry.register(syntheticPurpose) is AuthorizationPurposeRegistrationOutcome.Registered)

        val decision = composedEngine(runtime).evaluate(
            ExecutionRequest(
                requestId = RequestId("req-authz-purpose-unit6-full-engine-regression-2"),
                principalId = PrincipalId(ownerPrincipalId),
                origin = RequestOrigin.TEXT,
                intent = "Unit 6 full-engine regression check with a declared, registered purpose",
                targetResources = listOf(DefaultEvidenceCustodian.EVIDENCE_INTAKE_RESOURCE_ID),
                proposedActions = listOf(DefaultEvidenceCustodian.ACCEPT_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "corr-authz-purpose-unit6-full-engine-regression-2",
                authorizationPurpose = syntheticPurpose,
            ),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        assertEquals(PermissionLevel.AUTOMATIC, decision.level)

        runtime.shutdown()
    }

    @Test
    fun `no consumer class -- including Conversational Memory Admission's own coordinator -- declares a field referencing the Authorization Purpose registry`() {
        // MemoryAdmissionCoordinator was, at this repository's own current baseline, found
        // already wired into the committed ParkerRuntime.kt graph despite its own source file
        // remaining untracked (docs/reviews/AUTHORIZATION_PURPOSE_UNIT_6_PLANNING_REVIEW.md
        // Section 0) -- disclosed there, not corrected here. Because it is genuinely part of the
        // real, composed graph, it is checked here alongside the two consumers Unit 5's own test
        // (above) already checks, for the same "no hidden adoption" reason.
        val coordinatorFields = MemoryAdmissionCoordinator::class.java.declaredFields.map { it.name }

        assertTrue(coordinatorFields.none { it.contains("authorizationPurpose", ignoreCase = true) }, "MemoryAdmissionCoordinator: $coordinatorFields")
    }

    // ================= Gap #54 remains unresolved (mandatory) =================

    @Test
    fun `memory retrieve approves only the exact candidate purpose through the full composed engine`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val engine = composedEngine(runtime)
        val withoutPurpose = engine.evaluate(
            ExecutionRequest(
                requestId = RequestId("req-authz-purpose-unit6-gap54-no-purpose"),
                principalId = PrincipalId(ownerPrincipalId),
                origin = RequestOrigin.TEXT,
                intent = "Gap #54 non-widening check -- no Authorization Purpose declared",
                targetResources = emptyList(),
                proposedActions = listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "corr-authz-purpose-unit6-gap54-no-purpose",
            ),
        )
        assertEquals(PermissionDecisionOutcome.DENIED, withoutPurpose.decision, "memory.retrieve must remain DENIED with no Authorization Purpose declared")

        val registry = composedPolicy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val syntheticPurpose = AuthorizationPurposeId("test.unit-6-gap54-non-widening-check")
        assertTrue(registry.register(syntheticPurpose) is AuthorizationPurposeRegistrationOutcome.Registered)

        val withPurpose = engine.evaluate(
            ExecutionRequest(
                requestId = RequestId("req-authz-purpose-unit6-gap54-with-purpose"),
                principalId = PrincipalId(ownerPrincipalId),
                origin = RequestOrigin.TEXT,
                intent = "Gap #54 non-widening check -- a registered, active Authorization Purpose declared",
                targetResources = emptyList(),
                proposedActions = listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "corr-authz-purpose-unit6-gap54-with-purpose",
                authorizationPurpose = syntheticPurpose,
            ),
        )
        assertEquals(
            PermissionDecisionOutcome.DENIED,
            withPurpose.decision,
            "registration alone must not authorize memory.retrieve without the exact candidate Purpose",
        )

        val withCandidatePurpose = engine.evaluate(
            ExecutionRequest(
                requestId = RequestId("req-gap54-unit4-candidate"),
                principalId = PrincipalId(ownerPrincipalId),
                origin = RequestOrigin.TEXT,
                intent = "Unit 4 exact candidate authority",
                targetResources = emptyList(),
                proposedActions = listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "corr-gap54-unit4-candidate",
                authorizationPurpose = AuthorizationPurposeId("knowledge-memory.candidate-evaluation"),
            ),
        )
        assertEquals(PermissionDecisionOutcome.APPROVED, withCandidatePurpose.decision)
        assertEquals(PermissionLevel.AUTOMATIC, withCandidatePurpose.level)

        runtime.shutdown()
    }
}
