package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.ActionMappingResult
import parker.core.interfaces.CancellationResult
import parker.core.interfaces.EventBus
import parker.core.interfaces.EventType
import parker.core.interfaces.ExecutionLifecycleState
import parker.core.interfaces.ExecutionLifecycleTransitions
import parker.core.interfaces.ExecutionPipeline
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.ExecutionResultStatus
import parker.core.interfaces.ExecutionStatus
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.RequestId
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResultId
import parker.core.interfaces.Tool
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolInvocationBinding
import parker.core.interfaces.ToolRegistry
import parker.core.interfaces.ToolResolution
import parker.core.interfaces.ToolResult
import parker.core.interfaces.ValidationResult

/**
 * Runtime Integration: wires Execution Pipeline -> Permission Engine ->
 * Tool Registry -> Event Bus, per this phase's Priority 4. Every
 * dependency is the already-specified *interface* ([PermissionEngine],
 * [ToolRegistry], [ResourceRegistry], [EventBus]) injected via
 * constructor -- this class invents no policy of its own; it only
 * orchestrates calls to what those interfaces already promise.
 *
 * FORMER HONEST LIMITATION, closed by Unit 11A (see below): `ToolRegistry.resolve`
 * still deliberately returns only a [ToolDescriptor], never a live `Tool`
 * -- see `docs/architecture/tool-registry.md` "Lookup Process" -- but this
 * pipeline no longer stops at that resolution. It now also obtains the
 * actual invocable `Tool` via [ToolInvocationBinding] and calls it. A
 * `SUCCESS` result means a Tool actually ran and reported success, not
 * merely that the right one was found. `IMPLEMENTATION_GAPS.md` #32 is
 * closed by this change.
 *
 * Also deliberately simplified (documented, not silently invented):
 * processing is synchronous within [submit] (no background execution
 * queue), APPROVED_WITH_CONFIRMATION is treated identically to APPROVED
 * (a real confirmation workflow is Chapter 42 territory, out of scope),
 * and no timeout/expiry-during-execution handling exists beyond the
 * up-front `expiresAt` check (IMPLEMENTATION_GAPS.md #33).
 *
 * ## Sprint 1, Unit 11A (ToolInvocationBinding execution wiring)
 *
 * Closes `IMPLEMENTATION_GAPS.md` #32: once [toolRegistry] resolves a
 * [ToolDescriptor], this class now obtains the actual invocable [Tool]
 * bound to it via [toolInvocationBinding] -- the same
 * Execution-Pipeline-only path [ToolInvocationBinding]'s own KDoc
 * documents -- calls [Tool.validate] and only then [Tool.execute], and
 * builds the [ExecutionResult] from the real [ToolResult] returned.
 * Previously this class fabricated an always-`success = true` [ToolResult]
 * itself the moment a descriptor resolved, without ever calling a [Tool];
 * a `SUCCESS` result now means a Tool actually ran, not merely that the
 * right one was found. `PermissionEngine.evaluate` still runs, and its
 * outcome is still branched on, entirely unchanged, strictly *before* this
 * new binding/validate/execute step -- a `DENIED`/`DEFERRED` decision
 * never reaches [toolInvocationBinding] at all. A resolved-but-unbound
 * descriptor, a failed [Tool.validate], and a failed [Tool.execute] all
 * produce `ExecutionResultStatus.FAILED` (AD-015: invalid is not denied),
 * never `DENIED` -- `DENIED` remains exclusively the Permission Engine's
 * own outcome.
 */
class DefaultExecutionPipeline(
    private val resourceRegistry: ResourceRegistry,
    private val actionMapper: ActionMapper,
    private val permissionEngine: PermissionEngine,
    private val toolRegistry: ToolRegistry,
    private val eventBus: EventBus,
    private val toolInvocationBinding: ToolInvocationBinding,
) : ExecutionPipeline {

    private data class Tracked(var state: ExecutionLifecycleState, var lastUpdatedAt: Instant)

    private val mutex = Mutex()
    private val requests = mutableMapOf<RequestId, Tracked>()

    // Trust-sensitive events (InMemoryEventBus.TRUST_SENSITIVE_PREFIXES: "permission.", "execution.")
    // require a non-blank signature. No signing scheme is specified anywhere (IMPLEMENTATION_GAPS.md #26)
    // -- this is the same documented placeholder pattern, applied consistently here.
    private companion object {
        const val PLACEHOLDER_SIGNATURE = "internal-execution-pipeline-authority"
    }

    override suspend fun submit(request: ExecutionRequest): ExecutionResult {
        track(request.requestId, ExecutionLifecycleState.CREATED)
        publishLifecycleEvent("execution.request_received", request)

        val now = Instant.now()
        if (request.expiresAt != null && request.expiresAt.isBefore(now)) {
            transition(request.requestId, ExecutionLifecycleState.CREATED, ExecutionLifecycleState.EXPIRED)
            return terminalResult(request, ExecutionResultStatus.EXPIRED, now)
        }

        // Resolve target Resources to get their ResourceType (ExecutionRequest carries only ResourceIds --
        // action-mapping.md's resource-type matching needs the actual Resource).
        val resolvedResources = request.targetResources.map { it to resourceRegistry.resolve(it) }
        val unresolved = resolvedResources.filter { it.second == null }
        if (unresolved.isNotEmpty()) {
            // IMPLEMENTATION_GAPS.md #31, resolved by the targeted refinement pass: CREATED -> FAILED
            // is now a legal edge, added specifically to represent a validation failure like this one
            // cleanly, without ever reaching PermissionPending.
            transition(request.requestId, ExecutionLifecycleState.CREATED, ExecutionLifecycleState.FAILED)
            return terminalResult(
                request,
                ExecutionResultStatus.FAILED,
                now,
                errors = unresolved.map { "targetResource '${it.first.value}' does not resolve to a registered Resource" },
            )
        }
        val resourceTypes = resolvedResources.mapNotNull { it.second?.resourceType }.toSet()

        val mappingResults = actionMapper.map(request.proposedActions, resourceTypes)
        val failures = mappingResults.filterIsInstance<ActionMappingResult.Failed>()
        if (failures.isNotEmpty()) {
            // Same edge as above (IMPLEMENTATION_GAPS.md #31, resolved): action-mapping.md calls an
            // unresolvable action "Invalid, not Denied" -- CREATED -> FAILED represents that cleanly.
            transition(request.requestId, ExecutionLifecycleState.CREATED, ExecutionLifecycleState.FAILED)
            return terminalResult(
                request,
                ExecutionResultStatus.FAILED,
                now,
                errors = failures.map { "unresolvable proposed action '${it.proposedAction}': ${it.reason}" },
            )
        }

        transition(request.requestId, ExecutionLifecycleState.CREATED, ExecutionLifecycleState.VALIDATED)
        publishLifecycleEvent("permission.requested", request)
        transition(request.requestId, ExecutionLifecycleState.VALIDATED, ExecutionLifecycleState.PERMISSION_PENDING)

        // KNOWN GAP (IMPLEMENTATION_GAPS.md #30): action-mapping.md describes calling
        // PermissionEngine.evaluate once per resolved PermissionAction ("each resolved Permission
        // Action is evaluated as its own PermissionEngine.evaluate call"), but the existing Volume 3
        // interface signature (`evaluate(request: ExecutionRequest): PermissionDecision`, predating
        // that document) has no parameter to say *which* action of possibly several is being
        // evaluated. This implementation calls the interface exactly as it exists today -- once,
        // for the whole request -- since inventing a different signature is out of this phase's
        // authority. See the gap entry for the recommended fix.
        val decision = permissionEngine.evaluate(request)

        return when (decision.decision) {
            PermissionDecisionOutcome.APPROVED, PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION -> {
                // APPROVED_WITH_CONFIRMATION is treated identically to APPROVED here -- a real
                // confirmation workflow (Chapter 42) is out of scope for this phase.
                transition(request.requestId, ExecutionLifecycleState.PERMISSION_PENDING, ExecutionLifecycleState.APPROVED)
                publishLifecycleEvent("permission.granted", request)

                transition(request.requestId, ExecutionLifecycleState.APPROVED, ExecutionLifecycleState.QUEUED)
                transition(request.requestId, ExecutionLifecycleState.QUEUED, ExecutionLifecycleState.EXECUTING)
                publishLifecycleEvent("execution.started", request)

                val allMappings = mappingResults.filterIsInstance<ActionMappingResult.Resolved>().flatMap { it.mappings }
                val actionTypes = allMappings.map { it.resourceType }.toSet()
                val toolResolution = toolRegistry.resolve(decision.action, actionTypes)

                when (toolResolution) {
                    is ToolResolution.Resolved -> executeResolvedTool(request, toolResolution.descriptor, now)
                    is ToolResolution.Failed -> {
                        transition(request.requestId, ExecutionLifecycleState.EXECUTING, ExecutionLifecycleState.FAILED)
                        publishLifecycleEvent("execution.failed", request)
                        terminalResult(
                            request,
                            ExecutionResultStatus.FAILED,
                            now,
                            errors = listOf("no Tool could be resolved for action ${decision.action}: ${toolResolution.reason}"),
                        )
                    }
                }
            }
            PermissionDecisionOutcome.DENIED -> {
                transition(request.requestId, ExecutionLifecycleState.PERMISSION_PENDING, ExecutionLifecycleState.DENIED)
                publishLifecycleEvent("permission.denied", request)
                terminalResult(request, ExecutionResultStatus.DENIED, now)
            }
            PermissionDecisionOutcome.DEFERRED -> {
                transition(request.requestId, ExecutionLifecycleState.PERMISSION_PENDING, ExecutionLifecycleState.DEFERRED)
                terminalResult(request, ExecutionResultStatus.DEFERRED, now)
            }
        }
    }

    override suspend fun cancel(requestId: RequestId): CancellationResult {
        val tracked = mutex.withLock { requests[requestId] }
            ?: return CancellationResult(requestId, cancelled = false, reason = "unknown requestId")

        if (ExecutionLifecycleTransitions.isTerminal(tracked.state)) {
            return CancellationResult(requestId, cancelled = false, reason = "request is already in terminal state ${tracked.state}")
        }
        if (!ExecutionLifecycleTransitions.isValidTransition(tracked.state, ExecutionLifecycleState.CANCELLED)) {
            return CancellationResult(requestId, cancelled = false, reason = "cannot cancel from state ${tracked.state}")
        }

        transition(requestId, tracked.state, ExecutionLifecycleState.CANCELLED)
        return CancellationResult(requestId, cancelled = true)
    }

    override suspend fun status(requestId: RequestId): ExecutionStatus {
        val tracked = mutex.withLock { requests[requestId] }
            ?: throw NoSuchElementException("No tracked ExecutionRequest for requestId '${requestId.value}'")
        return ExecutionStatus(requestId, tracked.state, tracked.lastUpdatedAt)
    }

    private suspend fun track(requestId: RequestId, initial: ExecutionLifecycleState) {
        mutex.withLock { requests[requestId] = Tracked(initial, Instant.now()) }
    }

    private suspend fun transition(requestId: RequestId, from: ExecutionLifecycleState, to: ExecutionLifecycleState) {
        ExecutionLifecycleTransitions.requireValidTransition(from, to)
        mutex.withLock {
            val tracked = requests.getValue(requestId)
            tracked.state = to
            tracked.lastUpdatedAt = Instant.now()
        }
    }

    private suspend fun publishLifecycleEvent(eventType: String, request: ExecutionRequest) {
        eventBus.publish(
            ParkerEvent(
                eventId = "evt-${request.requestId.value}-$eventType",
                publisherPrincipalId = request.principalId,
                eventType = EventType(eventType),
                timestamp = Instant.now(),
                correlationId = request.correlationId,
                payload = mapOf("requestId" to request.requestId.value),
                signature = PLACEHOLDER_SIGNATURE,
            ),
        )
    }

    private fun terminalResult(
        request: ExecutionRequest,
        status: ExecutionResultStatus,
        startedAt: Instant,
        errors: List<String> = emptyList(),
    ): ExecutionResult = ExecutionResult(
        resultId = ResultId("result-${request.requestId.value}"),
        requestId = request.requestId,
        status = status,
        startedAt = startedAt,
        completedAt = Instant.now(),
        errors = errors,
    )

    /**
     * Sprint 1, Unit 11A: the one place [Tool.validate]/[Tool.execute] are
     * ever called from -- see this class's own "Unit 11A" KDoc. Called
     * only after `PermissionEngine.evaluate` has already returned
     * `APPROVED`/`APPROVED_WITH_CONFIRMATION`; nothing here can grant
     * execution authority the Permission Engine did not already grant.
     */
    private suspend fun executeResolvedTool(request: ExecutionRequest, descriptor: ToolDescriptor, startedAt: Instant): ExecutionResult {
        val tool = toolInvocationBinding.invocableFor(descriptor)
        if (tool == null) {
            transition(request.requestId, ExecutionLifecycleState.EXECUTING, ExecutionLifecycleState.FAILED)
            publishLifecycleEvent("execution.failed", request)
            return terminalResult(
                request,
                ExecutionResultStatus.FAILED,
                startedAt,
                errors = listOf(
                    "resolved Tool '${descriptor.toolId}' (${descriptor.version}) has no invocable Tool " +
                        "bound via ToolInvocationBinding",
                ),
            )
        }

        val validation = tool.validate(request)
        if (validation is ValidationResult.Invalid) {
            transition(request.requestId, ExecutionLifecycleState.EXECUTING, ExecutionLifecycleState.FAILED)
            publishLifecycleEvent("execution.failed", request)
            return terminalResult(request, ExecutionResultStatus.FAILED, startedAt, errors = validation.reasons)
        }

        val toolResult = tool.execute(request)
        return if (toolResult.success) {
            transition(request.requestId, ExecutionLifecycleState.EXECUTING, ExecutionLifecycleState.COMPLETED)
            publishLifecycleEvent("execution.completed", request)
            successResult(request, toolResult, startedAt)
        } else {
            transition(request.requestId, ExecutionLifecycleState.EXECUTING, ExecutionLifecycleState.FAILED)
            publishLifecycleEvent("execution.failed", request)
            terminalResult(request, ExecutionResultStatus.FAILED, startedAt, errors = listOfNotNull(toolResult.errorMessage))
        }
    }

    private fun successResult(request: ExecutionRequest, toolResult: ToolResult, startedAt: Instant): ExecutionResult =
        ExecutionResult(
            resultId = ResultId("result-${request.requestId.value}"),
            requestId = request.requestId,
            status = ExecutionResultStatus.SUCCESS,
            startedAt = startedAt,
            completedAt = Instant.now(),
            affectedResources = request.targetResources,
            toolResults = listOf(toolResult),
        )
}
