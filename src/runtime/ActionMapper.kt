package parker.core.runtime

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import parker.core.interfaces.ActionMappingFailureReason
import parker.core.interfaces.ActionMappingResult
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.ResourceType
import parker.core.interfaces.VocabularyRegistrationOutcome

/** The Planner-owned action vocabulary table (action-mapping.md "Transformation Rules"). */
interface ActionVocabulary {
    suspend fun register(entry: ActionVocabularyEntry): VocabularyRegistrationOutcome
    suspend fun lookup(verbPhrase: String): ActionVocabularyEntry?
}

/**
 * In-memory vocabulary keyed by exact `verbPhrase` string match.
 * action-mapping.md deliberately leaves natural-language-to-vocabulary
 * matching to the Planner/Chapter 20 (out of scope here) -- this table
 * only does the deterministic half: an already-chosen verbPhrase string
 * to its [ActionResourceMapping] set.
 */
class InMemoryActionVocabulary : ActionVocabulary {

    private val mutex = Mutex()
    private val entries = mutableMapOf<String, ActionVocabularyEntry>()

    override suspend fun register(entry: ActionVocabularyEntry): VocabularyRegistrationOutcome = mutex.withLock {
        when (val existing = entries[entry.verbPhrase]) {
            null -> {
                entries[entry.verbPhrase] = entry
                VocabularyRegistrationOutcome.Registered(entry.verbPhrase)
            }
            entry -> VocabularyRegistrationOutcome.AlreadyRegistered(entry.verbPhrase)
            else -> VocabularyRegistrationOutcome.Rejected(
                "verbPhrase '${entry.verbPhrase}' is already registered with different mappings " +
                    "(existing=${existing.mappings}, attempted=${entry.mappings})",
            )
        }
    }

    override suspend fun lookup(verbPhrase: String): ActionVocabularyEntry? = mutex.withLock {
        entries[verbPhrase]
    }
}

/**
 * Resolves `ExecutionRequest.proposedActions` against an [ActionVocabulary],
 * per action-mapping.md. Deliberately does NOT evaluate Permission (no
 * policy exists to evaluate against yet) -- this only performs the
 * mapping step that sits *before* `PermissionEngine.evaluate` in the
 * documented process (`ExecutionRequest -> Intent -> Planner -> Proposed
 * Actions -> Permission Actions -> Permission Engine -> Decision`); wiring
 * this into a concrete `PermissionEngine.evaluate` with real authorisation
 * policy is out of this phase's scope and recorded in
 * IMPLEMENTATION_GAPS.md.
 */
class ActionMapper(private val vocabulary: ActionVocabulary) {

    /**
     * Maps each proposed action independently (action-mapping.md
     * "Multiple Actions": each resolved action is its own evaluation,
     * there is no merged batch object). A single proposed action can
     * resolve to more than one [ActionResourceMapping] (action-mapping.md
     * "Composite Actions").
     */
    suspend fun map(proposedActions: List<String>, targetResourceTypes: Set<ResourceType>): List<ActionMappingResult> =
        proposedActions.map { proposed -> mapOne(proposed, targetResourceTypes) }

    private suspend fun mapOne(proposedAction: String, targetResourceTypes: Set<ResourceType>): ActionMappingResult {
        val entry = vocabulary.lookup(proposedAction)
            ?: return ActionMappingResult.Failed(proposedAction, ActionMappingFailureReason.UNKNOWN_ACTION)

        val applicable = entry.mappings.filter { it.resourceType in targetResourceTypes }.toSet()
        return if (applicable.isEmpty()) {
            ActionMappingResult.Failed(proposedAction, ActionMappingFailureReason.RESOURCE_TYPE_MISMATCH)
        } else {
            ActionMappingResult.Resolved(proposedAction, applicable)
        }
    }

    /**
     * action-mapping.md "Multiple Actions": the overall request may only
     * proceed if every resolved proposed action succeeded -- a single
     * unresolvable action invalidates the whole request rather than
     * silently executing a narrower subset.
     */
    suspend fun allResolved(proposedActions: List<String>, targetResourceTypes: Set<ResourceType>): Boolean =
        map(proposedActions, targetResourceTypes).all { it is ActionMappingResult.Resolved }
}
