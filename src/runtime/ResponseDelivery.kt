package parker.core.runtime

import parker.core.interfaces.ExecutionPipeline
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.ExecutionResult
import parker.core.interfaces.OutboundParkerResponse
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResourceType
import java.time.Instant

/**
 * The well-known `ExecutionRequest.metadata` key carrying an
 * [OutboundParkerResponse]'s text, per
 * `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 3 and
 * `docs/architecture/ADR-025_RESPONSE_DELIVERY_CONTENT_CARRIER.md`'s own
 * Stage 4 deferral -- fixed by
 * `docs/implementation/RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md` Section
 * 5, Decision 1. Exposed as a top-level constant, not an inline literal,
 * so a future "deliver" Tool implementation (Contract Design Section 4,
 * Deferred Item 2) can read `request.metadata[RESPONSE_TEXT_METADATA_KEY]`
 * without redefining or guessing the key.
 */
const val RESPONSE_TEXT_METADATA_KEY = "response.text"

/**
 * Sprint 7, Unit C4. Implements
 * `docs/architecture/RESPONSE_DELIVERY_CONTRACT_DESIGN.md` Section 1
 * exactly, per `docs/implementation/RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md`
 * (Scope Locked). A concrete, non-interface-backed class (Contract Design
 * Decision 1) -- no swappable seam exists for this component; the
 * mechanism is already fixed in full by `COMMUNICATION_CONTRACT_DESIGN.md`
 * Section 7, and Decision 2's own Resource-location logic is a
 * deterministic structural lookup, not a policy seam.
 *
 * Delivers an already-authorised [OutboundParkerResponse] by locating its
 * channel's own backing Resource (Contract Design Decision 2; formalised
 * by `docs/architecture/ADR-026_MODULE_RESOURCE_OWNERSHIP_CONVENTION.md`)
 * and submitting one [ExecutionRequest] through the injected
 * [ExecutionPipeline] -- exactly as any other request, with no exception
 * carved out for having originated as a Reasoning Provider's `Reply`.
 * Never authorises anything itself; never decides whether a response
 * should be sent; never constructs the content of a response.
 *
 * **Stateless (Plan Section 4a).** The only fields are the two
 * constructor-injected dependencies below -- no `var`, no mutable
 * collection, no cache of any prior response, request, or result. Each
 * call to [deliver] is fully independent of every other call.
 *
 * **Response pass-through (Plan Section 4b).** [deliver] never mutates or
 * reinterprets [response], never constructs a new [OutboundParkerResponse],
 * and never branches on `response.text`'s own content -- it is read
 * exactly once, to be carried unchanged into the constructed
 * [ExecutionRequest]'s `metadata`.
 *
 * **Exception propagation, not recovery (Plan Section 4c).** Neither call
 * below ([ResourceRegistry.listByOwner], [ExecutionPipeline.submit]) is
 * wrapped in a `try`/`catch`. Any exception either throws propagates to
 * this class's own caller unchanged. [GatedOutcome.NotAccepted] is
 * constructed only for the two structural Resource-location outcomes
 * named below (Contract Design Section 5) -- never to catch and
 * repackage a thrown exception.
 */
class ResponseDelivery(
    private val resourceRegistry: ResourceRegistry,
    private val executionPipeline: ExecutionPipeline,
) {

    /**
     * Given an already-constructed [OutboundParkerResponse]: locates the
     * response's own channel's backing Resource via
     * `ResourceRegistry.listByOwner(PrincipalId(response.channelId.value))`,
     * filtered to Resources of type [ResourceType.TOOL] (Contract Design
     * Decision 2). If the result is not exactly one match, delivery stops
     * here -- no [ExecutionRequest] is constructed, and this returns
     * [GatedOutcome.NotAccepted] with a `reason` naming which case
     * occurred (Contract Design Section 5). If exactly one match, this
     * constructs one [ExecutionRequest] (field values fixed by
     * `RESPONSE_DELIVERY_IMPLEMENTATION_PLAN.md` Section 5) and submits it
     * to [ExecutionPipeline.submit], wrapping the resulting
     * [ExecutionResult] in [GatedOutcome.Produced] unchanged.
     */
    suspend fun deliver(response: OutboundParkerResponse): GatedOutcome<ExecutionResult> {
        val candidates = resourceRegistry.listByOwner(PrincipalId(response.channelId.value))
            .filter { it.resourceType == ResourceType.TOOL }

        val target = when {
            candidates.isEmpty() -> return GatedOutcome.NotAccepted(
                "no channel Resource found for channelId '${response.channelId.value}'",
            )
            candidates.size > 1 -> return GatedOutcome.NotAccepted(
                "ambiguous: ${candidates.size} channel Resources found for channelId '${response.channelId.value}'",
            )
            else -> candidates.single()
        }

        val request = ExecutionRequest(
            requestId = RequestId("deliver-response-${response.correlationId.value}"),
            principalId = response.senderPrincipalId,
            origin = RequestOrigin.TEXT,
            intent = "deliver response",
            targetResources = listOf(target.resourceId),
            proposedActions = listOf("notify owner"),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = response.correlationId.value,
            metadata = mapOf(RESPONSE_TEXT_METADATA_KEY to response.text),
        )

        val result = executionPipeline.submit(request)
        return GatedOutcome.Produced(result)
    }
}
