package parker.core.interfaces

/**
 * Action Mapping contracts (`docs/architecture/action-mapping.md`),
 * closing consistency review §2.3: the deterministic bridge between
 * `ExecutionRequest.proposedActions` (free text) and
 * `PermissionDecision.action` (the closed [PermissionAction] enum).
 */

/** One (action, resourceType) pair a vocabulary entry maps a proposed action onto. */
data class ActionResourceMapping(
    val action: PermissionAction,
    val resourceType: ResourceType,
)

/**
 * A single entry in the Planner-owned action vocabulary table
 * (action-mapping.md "Transformation Rules"). [mappings] is a set, not a
 * single pair, to support composite actions (action-mapping.md
 * "Composite Actions") -- e.g. "move document to archive" needing both a
 * READ and a DELETE/WRITE pair.
 */
data class ActionVocabularyEntry(
    val verbPhrase: String,
    val mappings: Set<ActionResourceMapping>,
) {
    init {
        require(verbPhrase.isNotBlank()) { "ActionVocabularyEntry.verbPhrase must not be blank" }
        require(mappings.isNotEmpty()) { "ActionVocabularyEntry.mappings must not be empty" }
    }
}

/**
 * action-mapping.md "Unknown Actions": an unresolvable proposed action is
 * Invalid, not Denied -- there was never a well-formed decision to deny.
 * [RESOURCE_TYPE_MISMATCH] is the "Validation" section's rule: a mapped
 * action whose declared resourceTypes don't match any of the request's
 * actual target Resource types is equally invalid, not merely unapproved.
 */
enum class ActionMappingFailureReason {
    UNKNOWN_ACTION,
    RESOURCE_TYPE_MISMATCH,
}

/** Result of resolving a single proposed-action string against the vocabulary and a request's target Resource types. */
sealed class ActionMappingResult {
    data class Resolved(val proposedAction: String, val mappings: Set<ActionResourceMapping>) : ActionMappingResult()
    data class Failed(val proposedAction: String, val reason: ActionMappingFailureReason) : ActionMappingResult()
}

/**
 * Result of registering an [ActionVocabularyEntry]. action-mapping.md's
 * "Future Extensibility" section leaves ambiguous-mapping tie-breaking as
 * an open question (mirrors tool-registry.md's `TOOL_AMBIGUOUS` open
 * question). This implementation sidesteps needing a tie-break rule by
 * construction: one vocabulary entry per `verbPhrase`, and registering a
 * second entry under a `verbPhrase` that already has a *different* set of
 * mappings is rejected outright rather than silently overwritten or
 * allowed to coexist ambiguously. Re-registering an identical entry is an
 * idempotent no-op, matching the Tool Registry's established pattern for
 * the same shape of situation.
 */
sealed class VocabularyRegistrationOutcome {
    data class Registered(val verbPhrase: String) : VocabularyRegistrationOutcome()
    data class AlreadyRegistered(val verbPhrase: String) : VocabularyRegistrationOutcome()
    data class Rejected(val reason: String) : VocabularyRegistrationOutcome()
}
