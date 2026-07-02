package parker.core.runtime

import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionMappingFailureReason
import parker.core.interfaces.ActionMappingResult
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.ResourceType
import parker.core.interfaces.VocabularyRegistrationOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Proves the behaviours docs/architecture/action-mapping.md requires:
 * the Planner-owned vocabulary table, unknown-action handling (Invalid,
 * not Denied), ambiguous-action prevention, multiple-action handling, and
 * composite actions.
 */
class ActionMapperTest {

    private fun readCalendarEntry() = ActionVocabularyEntry(
        verbPhrase = "read calendar",
        mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR)),
    )

    private fun moveToArchiveEntry() = ActionVocabularyEntry(
        verbPhrase = "move document to archive",
        mappings = setOf(
            ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT),
            ActionResourceMapping(PermissionAction.DELETE, ResourceType.DOCUMENT),
        ),
    )

    @Test
    fun `an unresolvable proposed action maps to UNKNOWN_ACTION, not a permission denial`() = runTest {
        val mapper = ActionMapper(InMemoryActionVocabulary())
        val results = mapper.map(listOf("do something nonsensical"), setOf(ResourceType.CALENDAR))

        assertEquals(1, results.size)
        val failed = assertIs<ActionMappingResult.Failed>(results.single())
        assertEquals(ActionMappingFailureReason.UNKNOWN_ACTION, failed.reason)
    }

    @Test
    fun `a resolvable action against the wrong target resource type maps to RESOURCE_TYPE_MISMATCH`() = runTest {
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(readCalendarEntry())
        val mapper = ActionMapper(vocabulary)

        val results = mapper.map(listOf("read calendar"), setOf(ResourceType.DOCUMENT))
        val failed = assertIs<ActionMappingResult.Failed>(results.single())
        assertEquals(ActionMappingFailureReason.RESOURCE_TYPE_MISMATCH, failed.reason)
    }

    @Test
    fun `a resolvable action against a matching target resource type resolves`() = runTest {
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(readCalendarEntry())
        val mapper = ActionMapper(vocabulary)

        val results = mapper.map(listOf("read calendar"), setOf(ResourceType.CALENDAR))
        val resolved = assertIs<ActionMappingResult.Resolved>(results.single())
        assertEquals(setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.CALENDAR)), resolved.mappings)
    }

    @Test
    fun `composite actions resolve to more than one action-resource pair`() = runTest {
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(moveToArchiveEntry())
        val mapper = ActionMapper(vocabulary)

        val results = mapper.map(listOf("move document to archive"), setOf(ResourceType.DOCUMENT))
        val resolved = assertIs<ActionMappingResult.Resolved>(results.single())
        assertEquals(2, resolved.mappings.size)
    }

    @Test
    fun `multiple proposed actions are each resolved independently`() = runTest {
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(readCalendarEntry())
        val mapper = ActionMapper(vocabulary)

        val results = mapper.map(
            listOf("read calendar", "do something nonsensical"),
            setOf(ResourceType.CALENDAR),
        )

        assertEquals(2, results.size)
        assertIs<ActionMappingResult.Resolved>(results[0])
        assertIs<ActionMappingResult.Failed>(results[1])
    }

    @Test
    fun `a request only counts as fully resolved if every proposed action resolved`() = runTest {
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(readCalendarEntry())
        val mapper = ActionMapper(vocabulary)

        assertTrue(mapper.allResolved(listOf("read calendar"), setOf(ResourceType.CALENDAR)))
        assertEquals(
            false,
            mapper.allResolved(listOf("read calendar", "do something nonsensical"), setOf(ResourceType.CALENDAR)),
        )
    }

    // --- Ambiguous-action handling (prevented by construction, not resolved by a tie-break rule) ---

    @Test
    fun `registering a second entry under the same verbPhrase with different mappings is rejected`() = runTest {
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(readCalendarEntry())

        val conflicting = readCalendarEntry().copy(
            mappings = setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.CALENDAR)),
        )
        val outcome = vocabulary.register(conflicting)

        assertIs<VocabularyRegistrationOutcome.Rejected>(outcome)
    }

    @Test
    fun `re-registering an identical vocabulary entry is an idempotent no-op`() = runTest {
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(readCalendarEntry())
        val outcome = vocabulary.register(readCalendarEntry())

        assertIs<VocabularyRegistrationOutcome.AlreadyRegistered>(outcome)
    }
}
