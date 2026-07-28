package parker.composition

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.EventType
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.runtime.InMemoryEventBus
import parker.core.runtime.InMemoryMemoryCore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Programme 2, Memory Core, Implementation Unit 9. Behavioural tests for
 * [EventPublishingMemoryCore] -- the decorator publishing the five frozen
 * `memory.*` events. Wraps a real [InMemoryMemoryCore] (Unit 8) and a real
 * [InMemoryEventBus], subscribing a plain collecting [parker.core.interfaces.EventHandler]
 * to observe exactly what is published, mirroring
 * `InMemoryEventBusTest.kt`'s own established "subscribe and capture"
 * technique rather than introducing a new test double.
 */
class EventPublishingMemoryCoreTest {

    private val principal = PrincipalId("principal-1")

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "conversation-turn-1",
        sourceType = "conversation",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private class Harness {
        val eventBus = InMemoryEventBus()
        val delegate = InMemoryMemoryCore()
        val core = EventPublishingMemoryCore(delegate, eventBus)
        val received = mutableListOf<ParkerEvent>()

        suspend fun subscribeToAll() {
            listOf(
                "memory.entity_created",
                "memory.document_registered",
                "memory.assertion_created",
                "memory.relationship_created",
                "memory.record_status_changed",
            ).forEach { eventType ->
                eventBus.subscribe(EventType(eventType), PrincipalId("test-subscriber")) { received += it }
            }
        }
    }

    // ================= The four creation events =================

    @Test
    fun `createEntity publishes exactly one memory-entity_created event after a successful creation`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())

        val entity = harness.core.createEntity(principal,
            CandidateEntity(entityType = "person", primaryLabel = "Jane Doe", provenanceId = provenance.provenanceId),
        )

        assertEquals(1, harness.received.size)
        val event = harness.received.single()
        assertEquals(EventType("memory.entity_created"), event.eventType)
        assertEquals(entity.entityId.value, event.payload["recordId"])
        assertEquals(RelationshipEndpoint.ENTITY, event.payload["recordKind"])
        assertEquals("ACTIVE", event.payload["status"])
        assertEquals(entity.entityId.value, event.correlationId)
    }

    @Test
    fun `registerDocument publishes exactly one memory-document_registered event`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())

        val document = harness.core.registerDocument(principal,
            CandidateDocument(documentType = "email", locationReference = "mailbox://inbox/1", provenanceId = provenance.provenanceId),
        )

        assertEquals(1, harness.received.size)
        val event = harness.received.single()
        assertEquals(EventType("memory.document_registered"), event.eventType)
        assertEquals(document.documentId.value, event.payload["recordId"])
        assertEquals(RelationshipEndpoint.DOCUMENT, event.payload["recordKind"])
    }

    @Test
    fun `createAssertion publishes exactly one memory-assertion_created event`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())

        val assertion = harness.core.createAssertion(principal,
            CandidateAssertion(statement = "the user prefers window seats", provenanceId = provenance.provenanceId),
        )

        assertEquals(1, harness.received.size)
        val event = harness.received.single()
        assertEquals(EventType("memory.assertion_created"), event.eventType)
        assertEquals(assertion.assertionId.value, event.payload["recordId"])
        assertEquals(RelationshipEndpoint.ASSERTION, event.payload["recordKind"])
    }

    @Test
    fun `createRelationship publishes exactly one memory-relationship_created event`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())
        // Set up the Entity and Document directly on the undecorated delegate -- going through
        // harness.core here would itself publish memory.entity_created/memory.document_registered
        // into the same harness.received list, which is exactly the bug this fixes (those two
        // setup events were being counted against this test's own "exactly one event" assertion).
        val entity = harness.delegate.createEntity(principal,
            CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId),
        )
        val document = harness.delegate.registerDocument(principal,
            CandidateDocument(documentType = "email", locationReference = "mailbox://inbox/1", provenanceId = provenance.provenanceId),
        )

        val relationship = harness.core.createRelationship(principal,
            CandidateRelationship(
                relationshipType = Relationship.SUPPORTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, entity.entityId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, document.documentId.value),
                directional = true,
                provenanceId = provenance.provenanceId,
            ),
        )

        assertEquals(1, harness.received.size)
        val event = harness.received.single()
        assertEquals(EventType("memory.relationship_created"), event.eventType)
        assertEquals(relationship.relationshipId.value, event.payload["recordId"])
        assertEquals(RelationshipEndpoint.RELATIONSHIP, event.payload["recordKind"])
    }

    // ================= memory.record_status_changed =================

    @Test
    fun `transitionStatus publishes exactly one memory-record_status_changed event carrying the new status`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())
        val entity = harness.core.createEntity(principal,
            CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId),
        )

        harness.core.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(entity.entityId), MemoryCoreRecordStatus.DISPUTED)

        // One event from createEntity, one from transitionStatus.
        assertEquals(2, harness.received.size)
        val statusEvent = harness.received.last()
        assertEquals(EventType("memory.record_status_changed"), statusEvent.eventType)
        assertEquals(entity.entityId.value, statusEvent.payload["recordId"])
        assertEquals("DISPUTED", statusEvent.payload["status"])
    }

    // ================= No event for Provenance creation =================

    @Test
    fun `createProvenance publishes no event at all`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()

        harness.core.createProvenance(principal, candidateProvenance())

        assertEquals(emptyList(), harness.received)
    }

    // ================= No event for retrieval =================

    @Test
    fun `retrieval calls on the shared underlying InMemoryMemoryCore publish no event`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())
        val entity = harness.core.createEntity(principal,
            CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId),
        )
        val eventsAfterCreation = harness.received.size

        // EventPublishingMemoryCore wraps MemoryCore only -- MemoryRetrieval is reached directly
        // on the shared delegate, entirely bypassing this decorator, by construction.
        harness.delegate.getEntity(principal, entity.entityId)
        harness.delegate.findEntities(EntityLookupQuery(PrincipalId("test-subscriber"), 10))

        assertEquals(eventsAfterCreation, harness.received.size)
    }

    // ================= No event on failed creation =================

    @Test
    fun `a rejected createEntity -- unknown Provenance -- publishes no event`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()

        assertFailsWith<IllegalArgumentException> {
            harness.core.createEntity(principal,
                CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = ProvenanceId("does-not-exist")),
            )
        }

        assertEquals(emptyList(), harness.received)
    }

    // ================= No event on invalid lifecycle transition =================

    @Test
    fun `an invalid transitionStatus call publishes no memory-record_status_changed event`() = runTest {
        val harness = Harness()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())
        val entity = harness.core.createEntity(principal,
            CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId),
        )
        harness.core.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(entity.entityId), MemoryCoreRecordStatus.DELETED)
        harness.subscribeToAll()

        // DELETED is terminal -- any further transition is invalid.
        assertFailsWith<IllegalArgumentException> {
            harness.core.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(entity.entityId), MemoryCoreRecordStatus.ACTIVE)
        }

        assertEquals(emptyList(), harness.received)
    }

    @Test
    fun `transitionStatus on an unknown reference throws and publishes nothing`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()

        assertFailsWith<NoSuchElementException> {
            harness.core.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(EntityId("does-not-exist")), MemoryCoreRecordStatus.DISPUTED)
        }

        assertEquals(emptyList(), harness.received)
    }

    // ================= Exact frozen event names =================

    @Test
    fun `every published event uses one of the five frozen event names, never a sixth`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())
        val entity = harness.core.createEntity(principal,
            CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId),
        )
        val document = harness.core.registerDocument(principal,
            CandidateDocument(documentType = "email", locationReference = "mailbox://inbox/1", provenanceId = provenance.provenanceId),
        )
        harness.core.createAssertion(principal, CandidateAssertion(statement = "a claim", provenanceId = provenance.provenanceId))
        harness.core.createRelationship(principal,
            CandidateRelationship(
                relationshipType = Relationship.SUPPORTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, entity.entityId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, document.documentId.value),
                directional = true,
                provenanceId = provenance.provenanceId,
            ),
        )
        harness.core.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(entity.entityId), MemoryCoreRecordStatus.DISPUTED)

        val expectedEventTypes = setOf(
            EventType("memory.entity_created"),
            EventType("memory.document_registered"),
            EventType("memory.assertion_created"),
            EventType("memory.relationship_created"),
            EventType("memory.record_status_changed"),
        )
        assertEquals(5, harness.received.size)
        assertTrue(harness.received.all { it.eventType in expectedEventTypes })
    }

    // ================= Deterministic payload shape =================

    @Test
    fun `every published event payload has exactly the three documented keys`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())

        harness.core.createEntity(principal, CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId))

        val event = harness.received.single()
        assertEquals(setOf("recordId", "recordKind", "status"), event.payload.keys)
    }

    @Test
    fun `no event payload carries full record contents`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())

        harness.core.createEntity(principal,
            CandidateEntity(entityType = "person", primaryLabel = "a very specific private label", provenanceId = provenance.provenanceId),
        )

        val event = harness.received.single()
        assertTrue(event.payload.values.none { it.contains("a very specific private label") })
    }

    // ================= No permission evaluation inside event publication =================

    @Test
    fun `EventPublishingMemoryCore's constructor takes only a MemoryCore delegate and an EventBus -- no PermissionEngine`() {
        val constructors = EventPublishingMemoryCore::class.java.constructors

        assertTrue(constructors.any { it.parameterCount == 2 })
    }

    // ================= Successful state change first, event second =================

    @Test
    fun `the returned record already reflects the successful write before the event is inspected`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())

        val entity = harness.core.createEntity(principal,
            CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId),
        )

        assertEquals(MemoryCoreRecordStatus.ACTIVE, entity.status)
        assertEquals(entity.entityId.value, harness.received.single().payload["recordId"])
    }

    // ================= Errata 004: requestingPrincipalId is threaded through but never disclosed =================

    @Test
    fun `the requesting principal never appears anywhere in the published event -- not the payload, not the publisher identity`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val distinctivePrincipal = PrincipalId("a-very-distinctive-requesting-principal")
        val provenance = harness.core.createProvenance(distinctivePrincipal, candidateProvenance())

        harness.core.createEntity(
            distinctivePrincipal,
            CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId),
        )

        val event = harness.received.single()
        assertTrue(event.payload.values.none { it == distinctivePrincipal.value })
        assertNotEquals(distinctivePrincipal, event.publisherPrincipalId)
    }

    @Test
    fun `two otherwise-identical writes from different requesting principals publish events with the same fixed publisher identity`() = runTest {
        val harness = Harness()
        harness.subscribeToAll()
        val provenance = harness.core.createProvenance(principal, candidateProvenance())

        harness.core.createEntity(
            principal,
            CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId),
        )
        harness.core.createEntity(
            PrincipalId("principal-2"),
            CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId),
        )

        assertEquals(2, harness.received.size)
        val publisherIdentities = harness.received.map { it.publisherPrincipalId }.toSet()
        assertEquals(1, publisherIdentities.size, "every event must share the same fixed system publisher identity regardless of requesting principal")
    }
}
