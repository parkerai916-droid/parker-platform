package parker.core.runtime

import parker.core.interfaces.AgentPolicy
import parker.core.interfaces.AgentStepContext
import parker.core.interfaces.AgentStepDecision
import parker.core.interfaces.AgentStepSource

/**
 * Sprint 3, Track C, Unit C2: the deterministic, zero-configuration
 * [AgentStepSource] used everywhere a test needs an [InMemoryAgentRuntime]
 * but does not itself exercise multi-step behaviour. Reproduces Sprint 1
 * Unit 7's exact single-step construction -- propose the Agent Run's own
 * Goal against its own accumulated resource references, then declare
 * `Complete` once that one step has succeeded -- as an explicit, visible
 * [AgentStepSource] rather than runtime-internal behaviour, since Unit C2's
 * constructor change requires every [InMemoryAgentRuntime] construction
 * site to supply one.
 *
 * Every pre-C2 [InMemoryAgentRuntime] construction site
 * (`EventCollectorTest.kt`, `RuntimeLifecycleEventPublicationTest.kt`,
 * `VerticalSliceEndToEndTest.kt`) uses this fixture together with
 * [DEFAULT_AGENT_POLICY], so none of their existing SUCCESS-path
 * assertions (event sequences, `ExecutionRequest` field values,
 * `PermissionEngine` call counts) needed to change to keep compiling.
 * Mirrors [MockTool]/[FakePermissionEngine]'s own "test-only fixture, not
 * `src/runtime`" precedent exactly. `InMemoryAgentRuntimeTest.kt` (Unit
 * C2's own test file) uses its own, differently-parameterised local
 * equivalent instead, since several of its tests deliberately vary the
 * proposed action/target resources per call.
 */
class SingleStepAgentStepSource : AgentStepSource {
    override suspend fun nextStep(context: AgentStepContext): AgentStepDecision =
        if (context.stepNumber == 1) {
            AgentStepDecision.Propose(context.goal, context.resourceReferences)
        } else {
            AgentStepDecision.Complete
        }
}

/**
 * A generous, effectively non-limiting step budget for every pre-C2 test
 * site above, none of which exercise more than the one step
 * [SingleStepAgentStepSource] itself ever proposes -- `maxAgentSteps = 10`
 * is never actually reached by any of them, so it changes no observed
 * behaviour; it only satisfies the new required constructor parameter.
 */
val DEFAULT_AGENT_POLICY = AgentPolicy(maxAgentSteps = 10)
