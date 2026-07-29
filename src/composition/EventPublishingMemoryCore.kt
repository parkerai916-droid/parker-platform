package parker.composition

import java.time.Instant
import java.util.UUID
import parker.core.interfaces.Assertion
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.Document
import parker.core.interfaces.Entity
import parker.core.interfaces.EventBus
import parker.core.interfaces.EventType
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.ParkerEvent
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint

/**
 * Programme 2, Memory Core, Implementation Unit 9. A transparent,
 * delegating [MemoryCore] decorator that publishes the five frozen
 * `memory.*` events (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`
 * Section 13; `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` Section 9) --
 * [MemoryCore] is an interface, so this composition-root class wraps it
 * via delegation exactly as [LoggingReasoningProvider] wraps
 * `ReasoningProvider` and [LoggingCommunicationIntake] wraps
 * `CommunicationIntake`, without modifying `InMemoryMemoryCore` (Unit 8)
 * or the frozen [MemoryCore] contract itself. Wraps [MemoryCore] only --
 * `MemoryRetrieval` needs no decorator here, since retrieval publishes
 * nothing (Scope Lock Section 9; this Unit's own "Retrieval publishes
 * nothing" boundary), and a caller holding only a `MemoryRetrieval`-typed
 * reference is structurally unable to reach any of this class's
 * operations at all -- the same capability-narrowing precedent already
 * established by [InMemoryConversationEngine]'s split between
 * `ConversationEngine` and `ConversationHistorySource`.
 *
 * ## Event/behaviour boundary
 *
 * Every operation calls the wrapped [delegate] first and returns exactly
 * what it returns -- this class never alters the created or transitioned
 * record, and never catches an exception the delegate throws. A creation
 * or transition that fails (the delegate throws) therefore never reaches
 * the `publish` call at all -- the event is a strict *consequence* of an
 * already-successful state change, never a precondition or side input to
 * one, satisfying "event publication must not alter Memory Core
 * behaviour" structurally rather than by convention.
 *
 * ## Why `createProvenance` publishes nothing
 *
 * `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`'s frozen five-event set
 * is `memory.entity_created`, `memory.document_registered`,
 * `memory.assertion_created`, `memory.relationship_created`,
 * `memory.record_status_changed` -- no `memory.provenance_created` event
 * exists, and this Unit's own scope explicitly excludes adding one.
 * [createProvenance] is therefore a zero-logic delegate, exactly as
 * [KnowledgeSource.recall] is a zero-logic delegate to
 * [parker.core.interfaces.KnowledgeStore.retrieve] in [InMemoryKnowledgeStore].
 *
 * ## Payload shape
 *
 * Every published event's `payload` carries exactly three entries --
 * `recordId`, `recordKind` (one of [RelationshipEndpoint]'s own
 * Memory-Core-owned kind constants), and `status` (the record's
 * resulting [MemoryCoreRecordStatus], always `ACTIVE` for the four
 * creation events and the transition's own target status for
 * `memory.record_status_changed`) -- never the full record. This mirrors
 * [DefaultExecutionPipeline.publishLifecycleEvent]'s own established
 * "identifying fields only, never full content" payload convention.
 * [correlationId] is the affected record's own newly-minted identifier --
 * Memory Core's write operations carry no independent correlation
 * identifier of their own to reuse (no `Candidate*` type carries one),
 * so the record's own identifier is the only value already guaranteed
 * present and unique enough to serve that role.
 *
 * ## Publisher identity
 *
 * [publisherPrincipalId] is always [MEMORY_CORE_SYSTEM_PRINCIPAL_ID], a
 * fixed system identity -- mirroring
 * `InMemoryTaskManagerRuntime.TASK_MANAGER_RUNTIME_PRINCIPAL_ID`'s own,
 * already-established precedent for exactly this situation. Following
 * `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`, every
 * [MemoryCore] write operation now does accept a `requestingPrincipalId`
 * parameter -- but this class deliberately does not use it as
 * [publisherPrincipalId], and does not add it to the event `payload`
 * either. Publishing an event about an already-completed,
 * already-authorised state change attributes the event to the system
 * component that performed the write (Memory Core itself), not to
 * whichever principal happened to request it -- the same distinction
 * `InMemoryTaskManagerRuntime` already draws between the runtime's own
 * publisher identity and the principal whose proposal it is acting on.
 * Errata 004 §8's own non-disclosure reasoning for direct lookups applies
 * here too: identity of the requester is deliberately not surfaced
 * through this decorator's output.
 *
 * ## No permission evaluation here
 *
 * This class holds no [parker.core.interfaces.PermissionEngine]
 * reference and evaluates no permission decision -- publishing an event
 * about an already-completed, already-authorised state change is not
 * itself a new authorisation decision. Permission evaluation for the
 * write itself remains entirely upstream of this decorator (a future
 * `PermissionGatedMemoryCore`, not built by this Unit, composed outside
 * this class if both concerns are ever combined).
 */
class EventPublishingMemoryCore(
    private val delegate: MemoryCore,
    private val eventBus: EventBus,
) : MemoryCore {

    private companion object {
        val MEMORY_CORE_SYSTEM_PRINCIPAL_ID = PrincipalId("system.memory-core")
    }

    override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance =
        delegate.createProvenance(requestingPrincipalId, candidate)

    override suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity {
        val entity = delegate.createEntity(requestingPrincipalId, candidate)
        publish("memory.entity_created", entity.entityId.value, RelationshipEndpoint.ENTITY, entity.status)
        return entity
    }

    override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document {
        val document = delegate.registerDocument(requestingPrincipalId, candidate)
        publish("memory.document_registered", document.documentId.value, RelationshipEndpoint.DOCUMENT, document.status)
        return document
    }

    override suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion {
        val assertion = delegate.createAssertion(requestingPrincipalId, candidate)
        publish("memory.assertion_created", assertion.assertionId.value, RelationshipEndpoint.ASSERTION, assertion.status)
        return assertion
    }

    override suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship {
        val relationship = delegate.createRelationship(requestingPrincipalId, candidate)
        publish(
            "memory.relationship_created",
            relationship.relationshipId.value,
            RelationshipEndpoint.RELATIONSHIP,
            relationship.status,
        )
        return relationship
    }

    override suspend fun transitionStatus(
        requestingPrincipalId: PrincipalId,
        reference: MemoryCoreRecordReference,
        targetStatus: MemoryCoreRecordStatus,
    ): MemoryCoreRecord {
        // Nothing is published if this throws (unknown reference, or a transition the frozen
        // lifecycle table does not permit) -- the delegate's own exception propagates unchanged,
        // exactly as LoggingReasoningProvider.reason lets ReasoningProvider's own exception through.
        val updated = delegate.transitionStatus(requestingPrincipalId, reference, targetStatus)
        val (recordId, recordKind, status) = when (updated) {
            is MemoryCoreRecord.OfEntity -> Triple(updated.entity.entityId.value, RelationshipEndpoint.ENTITY, updated.entity.status)
            is MemoryCoreRecord.OfDocument -> Triple(updated.document.documentId.value, RelationshipEndpoint.DOCUMENT, updated.document.status)
            is MemoryCoreRecord.OfAssertion -> Triple(updated.assertion.assertionId.value, RelationshipEndpoint.ASSERTION, updated.assertion.status)
            is MemoryCoreRecord.OfRelationship ->
                Triple(updated.relationship.relationshipId.value, RelationshipEndpoint.RELATIONSHIP, updated.relationship.status)
        }
        publish("memory.record_status_changed", recordId, recordKind, status)
        return updated
    }

    private suspend fun publish(eventType: String, recordId: String, recordKind: String, status: MemoryCoreRecordStatus) {
        eventBus.publish(
            ParkerEvent(
                eventId = "evt-$recordId-$eventType-${UUID.randomUUID()}",
                publisherPrincipalId = MEMORY_CORE_SYSTEM_PRINCIPAL_ID,
                eventType = EventType(eventType),
                timestamp = Instant.now(),
                correlationId = recordId,
                payload = mapOf(
                    "recordId" to recordId,
                    "recordKind" to recordKind,
                    "status" to status.name,
                ),
            ),
        )
    }
}
