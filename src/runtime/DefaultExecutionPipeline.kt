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
import parker.core.interfaces.ToolDescriptor
import parker.core.interfaces.ToolRegistry
import parker.core.interfaces.ToolResolution
import parker.core.interfaces.ToolResult

/**
 * Runtime Integration: wires Execution Pipeline -> Permission Engine ->
 * Tool Registry -> Event Bus, per this phase's Priority 4. Every
 * dependency is the already-specified *interface* ([PermissionEngine],
 * [ToolRegistry], [ResourceRegistry], [EventBus]) injected via
 * constructor -- this class invents no policy of its own; it only
 * orchestrates calls to what those interfaces already promise.
 *
 * IMPORTANT HONEST LIMITATION: no concrete `Tool` implementation exists
 * anywhere in this codebase yet (`ToolRegistry.resolve` deliberately
 * returns a [ToolDescriptor], never a live `Tool` -- see
 * `docs/architecture/tool-registry.md` "Lookup Process"). This pipeline
 * therefore proves the orchestration up to and including tool
 * *resolution*, but stops short of actually invoking `Tool.execute` --
 * there is nothing to invoke yet. A `SUCCESS` result from this pipeline
 * means "every stage up to and including finding the right Tool
 * succeeded," not "a Tool actually ran." Recorded in
 * IMPLEMENTATION_GAPS.md #32.
 *
 * Also deliberately simplified (documented, not silently invented):
 * processing is synchronous within [submit] (no background execution
 * queue), APPROVED_WITH_CONFIRMATION is treated identically to APPROVED
 * (a real confirmation workflow is Chapter 42 territory, out of scope),
 * and no timeout/expiry-during-execution handling exists beyond the
 * up-front `expiresAt` check (IMPLEMENTATION_GAPS.md #33).
 */
class DefaultExecutionPipeline(
    private val resourceRegistry: ResourceRegistry,
    private val actionMapper: ActionMapper,
    private val permissionEngine: PermissionEngine,
    private val toolRegistry: ToolRegistry,
    private val eventBus: EventBus,
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
                    is ToolResolution.Resolved -> {
                        transition(request.requestId, ExecutionLifecycleState.EXECUTING, ExecutionLifecycleState.COMPLETED)
                        publishLifecycleEvent("execution.completed", request)
                        successResult(request, toolResolution.descriptor, now)
                    }
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

    private fun successResult(request: ExecutionRequest, toolDescriptor: ToolDescriptor, startedAt: Instant): ExecutionResult =
        ExecutionResult(
            resultId = ResultId("result-${request.requestId.value}"),
            requestId = request.requestId,
            status = ExecutionResultStatus.SUCCESS,
            startedAt = startedAt,
            completedAt = Instant.now(),
            affectedResources = request.targetResources,
            toolResults = listOf(
                ToolResult(
                    toolId = toolDescriptor.toolId,
                    success = true,
                    output = mapOf("resolvedVersion" to toolDescriptor.version),
                ),
            ),
        )
}
