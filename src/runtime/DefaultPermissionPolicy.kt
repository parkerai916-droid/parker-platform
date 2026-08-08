package parker.core.runtime

import java.time.Instant
import parker.core.interfaces.ActionMappingResult
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.DecisionId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.ResourceId
import parker.core.interfaces.ResourceRegistry
import parker.core.interfaces.ResourceType

/**
 * A single, fixed policy rule: for this (action, resourceType) pair,
 * produce this outcome at this level. This is the smallest possible
 * policy shape that satisfies
 * `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md`
 * without introducing Role-Based, Attribute-Based, or Capability-based
 * access control (all explicitly out of scope for Sprint 2 Unit A2) --
 * a flat, fixed lookup table, mirroring the same "deterministic table,
 * no runtime editing" shape `InMemoryActionVocabulary`
 * (`src/runtime/ActionMapper.kt`) already uses for action-mapping.
 * Rule content is supplied by the caller, not hardcoded here -- this
 * class is the policy *mechanism* PermissionPolicy.md describes, not a
 * real, shipped policy (`IMPLEMENTATION_GAPS.md` #25's "policy content"
 * concern remains something a caller decides, per PermissionPolicy.md
 * §11's "Policy storage" and "Policy editing" being explicitly out of
 * scope).
 *
 * [authorizationPurpose] (Authorization Purpose Implementation Plan, Unit
 * 4): `null` (the default) means this rule addresses every request
 * reaching its (action, resourceType) pair regardless of Authorization
 * Purpose, identical to today's behaviour. A non-null value narrows the
 * rule to only those requests declaring that exact, currently-active
 * Authorization Purpose -- see [DefaultPermissionPolicy]'s own KDoc for
 * the precedence this creates against a coarser, `null`-purpose rule
 * addressing the same pair.
 *
 * [proposedAction] is an optional exact verb-phrase discriminator. `null`
 * preserves the existing coarse rule. A non-null value matches only the
 * identical verb phrase already resolved by [ActionMapper].
 */
data class PermissionPolicyRule(
    val action: PermissionAction,
    val resourceType: ResourceType,
    val outcome: PermissionDecisionOutcome,
    val level: PermissionLevel,
    val authorizationPurpose: AuthorizationPurposeId? = null,
    val proposedAction: String? = null,
)

/**
 * Sprint 2, Unit A2: implements the policy model
 * `docs/specifications/volume-03-core-interfaces/PermissionPolicy.md`
 * describes. Consulted by [DefaultPermissionEngine] only for a Principal
 * already confirmed `Active` (Sprint 2 Unit A1) -- this class never
 * resolves identity itself, per PermissionPolicy.md's own "Policy
 * Invariants": it never executes actions, never resolves identity, and
 * never modifies Resources. `resourceRegistry.resolve` and
 * `actionMapper.map` are both read-only calls; this class calls neither
 * `ResourceRegistry.register`/`update` nor
 * `ActionVocabulary.register` anywhere.
 *
 * ## Why this class depends on [actionMapper] and [resourceRegistry]
 *
 * `PermissionEngine.evaluate(request: ExecutionRequest): PermissionDecision`'s
 * existing signature (unchanged by this unit, per its own instructions)
 * does not carry the already-resolved `PermissionAction`/`ResourceType`
 * pairs `DefaultExecutionPipeline` computes via `ActionMapper` moments
 * earlier in the same request's lifecycle (`IMPLEMENTATION_GAPS.md` #30,
 * whose own text names "wiring ActionMapper's output into a concrete
 * PermissionEngine.evaluate" as "the natural next step once policy
 * exists" -- this class is that next step). Because
 * `PermissionDecision.action` is read by `DefaultExecutionPipeline` on
 * the `APPROVED` path to resolve a Tool
 * (`toolRegistry.resolve(decision.action, actionTypes)`), an invented or
 * guessed `action` value here could cause an approved request to resolve
 * the wrong Tool, or none. This class therefore re-derives the same
 * `PermissionAction`/`ResourceType` pairs `ActionMapper` and
 * `ResourceRegistry` would already have produced, using those exact
 * dependencies unmodified -- not a second, independently-maintained
 * vocabulary, and not a change to either dependency's own responsibility.
 * This repeats read-only work `DefaultExecutionPipeline` already did a
 * moment earlier for the same request; that repetition is the accepted
 * cost of not being able to change `PermissionEngine`'s signature or
 * `DefaultExecutionPipeline` to pass the already-computed result through.
 *
 * ## "Unknown Action" / "Unknown Resource" / "Unknown Permission" (PermissionPolicy.md §7)
 *
 * By the time a request reaches this class through the real, wired
 * `DefaultExecutionPipeline`, an unresolvable proposed action or an
 * unresolvable target Resource has *already* been rejected as
 * `ExecutionResultStatus.FAILED`, before `PermissionEngine.evaluate` is
 * even called (`action-mapping.md`: "an unresolvable proposed action is
 * Invalid, not Denied"; AD-015). So "Unknown Action → DENIED" here does
 * not mean "fails action-mapping" -- that case cannot reach this class
 * through the pipeline at all. It means: the action and resource
 * resolved successfully, but no [PermissionPolicyRule] addresses that
 * specific pair. This class treats "Unknown Action," "Unknown Resource,"
 * and "Missing policy match" identically -- a single "no applicable rule
 * found" path -- because none of the three names a distinct field this
 * unit's own contracts carry (confirmed with the requester before
 * implementation: no `ExecutionRequest` field represents a "requested
 * permission" distinct from the resolved action/resource pair). If this
 * class is ever called directly, outside the pipeline, with an action or
 * resource that does not resolve at all, it also returns `DENIED` --
 * not because that is a policy decision, but because
 * `PermissionDecisionOutcome` has no fifth "invalid" value for
 * `evaluate` to return; `DENIED` is Section 7's own specified
 * conservative default for exactly this situation.
 *
 * ## Authorization Purpose (Authorization Purpose Implementation Plan, Unit 4)
 *
 * [authorizationPurposeRegistry] is optional and defaults to `null`,
 * additive to this class's own existing two dependencies -- `ParkerRuntime`'s
 * own composition of this class is unmodified by this Unit (out of this
 * Unit's own scope) and continues to omit it, so every currently-composed
 * request behaves exactly as before. Where supplied, `evaluate` reads
 * `request.authorizationPurpose` (Unit 2) and, if non-null, checks
 * `authorizationPurposeRegistry.isActive(...)` (Unit 3) once per
 * evaluation. A [PermissionPolicyRule] naming that exact, currently-active
 * value takes precedence over a coarser rule addressing the same (action,
 * resourceType) pair without one -- never the reverse -- satisfying
 * `AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` §2.4's precedence-safety
 * requirement ("a coarse rule may never resolve a request a more
 * specific, Authorization-Purpose-aware rule was meant to govern"). An
 * absent, unregistered, or retired value can never satisfy a
 * purpose-aware rule -- it falls back to the coarse rule exactly as "no
 * purpose supplied" would, and if no coarse rule exists either, the
 * request denies through this class's own pre-existing "no rule matches"
 * default (§2.4's own "fail-closed... denies by the same default every
 * other unknown value already denies by").
 *
 * ## Targetless derivation and verb-specific rules (Gap #54 Unit 1)
 *
 * [targetlessResourceTypesByProposedAction] is empty by default and is
 * validated against the two-action closed set frozen by the accepted
 * Operationalisation Scope Lock. For a structurally targetless request,
 * each verb is mapped independently with only its own configured types.
 * Requests with real targets continue to use only [resourceRegistry].
 *
 * Applicable rules are ordered by the two optional exact dimensions they
 * declare: Authorization Purpose and verb phrase. One unique maximally
 * specific rule governs. Multiple maximal rules are ambiguous and deny,
 * making selection independent of rule-list ordering.
 */
class DefaultPermissionPolicy(
    private val actionMapper: ActionMapper,
    private val resourceRegistry: ResourceRegistry,
    private val rules: List<PermissionPolicyRule>,
    private val authorizationPurposeRegistry: AuthorizationPurposeRegistry? = null,
    private val targetlessResourceTypesByProposedAction: Map<String, Set<ResourceType>> = emptyMap(),
) {

    init {
        require(
            targetlessResourceTypesByProposedAction.all { (proposedAction, resourceTypes) ->
                GOVERNED_TARGETLESS_RESOURCE_TYPES[proposedAction] == resourceTypes
            },
        ) {
            "Targetless resource derivation is closed to memory.retrieve -> MEMORY and " +
                "memory.retrieve_document -> DOCUMENT"
        }
    }

    suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
        val resourceTypes = request.targetResources
            .mapNotNull { resourceRegistry.resolve(it) }
            .map { it.resourceType }
            .toSet()

        val resolvedMappings = request.proposedActions.flatMap { proposedAction ->
            val typesForAction = if (request.targetResources.isEmpty()) {
                targetlessResourceTypesByProposedAction[proposedAction].orEmpty()
            } else {
                resourceTypes
            }
            actionMapper.map(listOf(proposedAction), typesForAction)
                .filterIsInstance<ActionMappingResult.Resolved>()
                .flatMap { resolved ->
                    resolved.mappings.map { mapping -> ResolvedPolicyMapping(resolved.proposedAction, mapping) }
                }
        }

        if (resolvedMappings.isEmpty()) {
            // No resolved (action, resourceType) pair at all -- see this class's own KDoc
            // ("Unknown Action" / "Unknown Resource") for why this is DENIED, not FAILED.
            return deniedDecision(request)
        }

        // An absent, unregistered, or retired purpose is folded into "no purpose" here --
        // see this class's own KDoc ("Authorization Purpose") for why that is the
        // fail-closed, regression-safe reading of Scope Lock §2.4.
        val effectivePurpose = request.authorizationPurpose?.takeIf { purpose ->
            authorizationPurposeRegistry?.isActive(purpose) == true
        }

        val evaluated = resolvedMappings.map { resolved -> resolved to ruleOutcomeFor(resolved, effectivePurpose) }

        // "Most restrictive wins" when a request's multiple proposed actions resolve to more
        // than one (action, resourceType) pair -- PermissionEngine.evaluate is still called
        // once per whole request (IMPLEMENTATION_GAPS.md #30's already-accepted simplification;
        // this class does not attempt to fix that gap). DENIED is most restrictive, APPROVED is
        // least; ties are broken by picking the first most-restrictive pair encountered.
        val (chosenMapping, chosenOutcome) = evaluated.minByOrNull { (_, outcomeAndLevel) ->
            restrictiveness(outcomeAndLevel.first)
        } ?: return deniedDecision(request)

        return PermissionDecision(
            decisionId = DecisionId("dec-policy-${request.requestId.value}"),
            principalId = request.principalId,
            resourceId = request.targetResources.firstOrNull() ?: ResourceId("no-target-resource"),
            action = chosenMapping.mapping.action,
            decision = chosenOutcome.first,
            level = chosenOutcome.second,
            timestamp = Instant.now(),
        )
    }

    /**
     * Returns (outcome, level) for [resolved] -- `DENIED`/`AUTOMATIC` when no rule matches
     * or when more than one applicable rule has maximal specificity.
     */
    private fun ruleOutcomeFor(
        resolved: ResolvedPolicyMapping,
        purpose: AuthorizationPurposeId?,
    ): Pair<PermissionDecisionOutcome, PermissionLevel> {
        val applicable = rules.filter { rule ->
            rule.action == resolved.mapping.action &&
                rule.resourceType == resolved.mapping.resourceType &&
                (rule.authorizationPurpose == null || rule.authorizationPurpose == purpose) &&
                (rule.proposedAction == null || rule.proposedAction == resolved.proposedAction)
        }
        val maximalSpecificity = applicable.maxOfOrNull(::specificity)
            ?: return PermissionDecisionOutcome.DENIED to PermissionLevel.AUTOMATIC
        val maximalRules = applicable.filter { specificity(it) == maximalSpecificity }
        val rule = maximalRules.singleOrNull()
            ?: return PermissionDecisionOutcome.DENIED to PermissionLevel.AUTOMATIC
        return rule.outcome to rule.level
    }

    private fun specificity(rule: PermissionPolicyRule): Int =
        (if (rule.authorizationPurpose != null) 1 else 0) +
            (if (rule.proposedAction != null) 1 else 0)

    private fun restrictiveness(outcome: PermissionDecisionOutcome): Int = when (outcome) {
        PermissionDecisionOutcome.DENIED -> 0
        PermissionDecisionOutcome.DEFERRED -> 1
        PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION -> 2
        PermissionDecisionOutcome.APPROVED -> 3
    }

    /**
     * Mirrors `DefaultPermissionEngine.deniedDecision`'s own placeholder
     * convention exactly (Sprint 2 Unit A1): `action`/`resourceId` are
     * populated only to satisfy `PermissionDecision`'s shape and carry no
     * policy meaning on a `DENIED` outcome.
     */
    private fun deniedDecision(request: ExecutionRequest): PermissionDecision = PermissionDecision(
        decisionId = DecisionId("dec-policy-${request.requestId.value}"),
        principalId = request.principalId,
        resourceId = request.targetResources.firstOrNull() ?: ResourceId("no-target-resource"),
        action = PermissionAction.EXECUTE,
        decision = PermissionDecisionOutcome.DENIED,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.now(),
    )

    private data class ResolvedPolicyMapping(
        val proposedAction: String,
        val mapping: ActionResourceMapping,
    )

    private companion object {
        val GOVERNED_TARGETLESS_RESOURCE_TYPES: Map<String, Set<ResourceType>> = mapOf(
            "memory.retrieve" to setOf(ResourceType.MEMORY),
            "memory.retrieve_document" to setOf(ResourceType.DOCUMENT),
        )
    }
}
