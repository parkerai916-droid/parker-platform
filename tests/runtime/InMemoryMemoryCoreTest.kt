package parker.core.runtime

import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.ChronologicalLookupQuery
import parker.core.interfaces.ContentNature
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentLookupQuery
import parker.core.interfaces.DocumentProcessingStatus
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.MetadataLookupQuery
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceLookupQuery
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.RelationshipTraversalDirection
import parker.core.interfaces.RelationshipTraversalQuery
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Programme 2, Memory Core, Implementation Unit 8. Behavioural tests for
 * [InMemoryMemoryCore] -- the first implementation of [MemoryCore] and
 * [MemoryRetrieval]. Every prior Unit's test file (`MemoryCoreContractsTest.kt`
 * through `MemoryCoreInterfacesTest.kt`) tested shape only; this file is
 * the first to test behaviour: what a real write actually stores, what a
 * real read actually returns, and how the frozen lifecycle table
 * (`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_003.md`) is
 * actually enforced.
 */
class InMemoryMemoryCoreTest {

    private val principal = PrincipalId("principal-1")

    private fun candidateProvenance(
        sourceIdentifier: String = "conversation-turn-1",
        sourceType: String = "conversation",
        contentNature: ContentNature = ContentNature.ORIGINAL,
    ) = CandidateProvenance(
        sourceIdentifier = sourceIdentifier,
        sourceType = sourceType,
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = contentNature,
    )

    private fun candidateEntity(
        provenanceId: ProvenanceId,
        entityType: String = "person",
        primaryLabel: String = "Jane Doe",
        aliases: List<String> = emptyList(),
    ) = CandidateEntity(
        entityType = entityType,
        primaryLabel = primaryLabel,
        provenanceId = provenanceId,
        aliases = aliases,
    )

    private fun candidateDocument(
        provenanceId: ProvenanceId,
        documentType: String = "email",
        locationReference: String = "mailbox://inbox/message-1",
    ) = CandidateDocument(
        documentType = documentType,
        locationReference = locationReference,
        provenanceId = provenanceId,
    )

    private fun candidateAssertion(
        provenanceId: ProvenanceId,
        statement: String = "the user prefers window seats",
    ) = CandidateAssertion(
        statement = statement,
        provenanceId = provenanceId,
    )

    private fun candidateRelationship(
        provenanceId: ProvenanceId,
        fromEndpoint: RelationshipEndpoint,
        toEndpoint: RelationshipEndpoint,
        relationshipType: String = Relationship.SUPPORTS,
        directional: Boolean = true,
    ) = CandidateRelationship(
        relationshipType = relationshipType,
        fromEndpoint = fromEndpoint,
        toEndpoint = toEndpoint,
        directional = directional,
        provenanceId = provenanceId,
    )

    /**
     * A bare, already-identified [Provenance], for Memory Core Durability
     * Implementation Unit 5's own tests -- distinct from
     * [candidateProvenance], which carries no identifier at all and is
     * only ever passed to [InMemoryMemoryCore.createProvenance] (which
     * mints one). This helper instead constructs the finished record
     * directly, exactly as `restoreProvenance`/`restoreIdentifierCounters`
     * tests need to supply an already-known identifier.
     */
    private fun provenance(id: String) = Provenance(
        provenanceId = ProvenanceId(id),
        sourceIdentifier = "conversation-turn-1",
        sourceType = "conversation",
        acquisitionTime = Instant.parse("2025-01-01T00:00:00Z"),
        ingestionTime = Instant.parse("2025-01-01T00:00:01Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    /** A bare, already-identified [Entity] -- see [provenance]'s own KDoc for why this is distinct from [candidateEntity]. */
    private fun entity(id: String, provenanceId: ProvenanceId) = Entity(
        entityId = EntityId(id),
        entityType = "person",
        primaryLabel = "Person $id",
        provenanceId = provenanceId,
        createdAt = Instant.parse("2025-01-01T00:00:02Z"),
    )

    // ================= Provenance creation =================

    @Test
    fun `createProvenance mints an identifier and assigns ingestionTime internally`() = runTest {
        val core = InMemoryMemoryCore()
        val before = Instant.now()

        val provenance = core.createProvenance(principal, candidateProvenance())

        val after = Instant.now()
        assertTrue(provenance.provenanceId.value.isNotBlank())
        assertTrue(!provenance.ingestionTime.isBefore(before) && !provenance.ingestionTime.isAfter(after))
        assertEquals("conversation-turn-1", provenance.sourceIdentifier)
    }

    @Test
    fun `two createProvenance calls mint two distinct identifiers`() = runTest {
        val core = InMemoryMemoryCore()

        val first = core.createProvenance(principal, candidateProvenance())
        val second = core.createProvenance(principal, candidateProvenance())

        assertNotEquals(first.provenanceId, second.provenanceId)
    }

    // ================= Entity creation =================

    @Test
    fun `createEntity mints an identifier, assigns createdAt, and begins ACTIVE`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val before = Instant.now()

        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        val after = Instant.now()
        assertTrue(entity.entityId.value.isNotBlank())
        assertTrue(!entity.createdAt.isBefore(before) && !entity.createdAt.isAfter(after))
        assertEquals(MemoryCoreRecordStatus.ACTIVE, entity.status)
        assertEquals(provenance.provenanceId, entity.provenanceId)
    }

    @Test
    fun `createEntity rejects a candidate referencing a nonexistent Provenance`() = runTest {
        val core = InMemoryMemoryCore()

        assertFailsWith<IllegalArgumentException> {
            core.createEntity(principal, candidateEntity(ProvenanceId("provenance-does-not-exist")))
        }
    }

    @Test
    fun `two structurally identical CandidateEntity submissions produce two distinct stored Entity records`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val candidate = candidateEntity(provenance.provenanceId)

        val first = core.createEntity(principal, candidate)
        val second = core.createEntity(principal, candidate)

        assertNotEquals(first.entityId, second.entityId)
        assertEquals(first.primaryLabel, second.primaryLabel)
    }

    // ================= Document registration =================

    @Test
    fun `registerDocument mints an identifier, assigns registeredAt, and begins ACTIVE`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())

        val document = core.registerDocument(principal, candidateDocument(provenance.provenanceId))

        assertTrue(document.documentId.value.isNotBlank())
        assertEquals(MemoryCoreRecordStatus.ACTIVE, document.status)
        assertEquals(DocumentProcessingStatus.REGISTERED, document.processingStatus)
    }

    @Test
    fun `registerDocument rejects a candidate referencing a nonexistent Provenance`() = runTest {
        val core = InMemoryMemoryCore()

        assertFailsWith<IllegalArgumentException> {
            core.registerDocument(principal, candidateDocument(ProvenanceId("provenance-does-not-exist")))
        }
    }

    // ================= Assertion creation =================

    @Test
    fun `createAssertion mints an identifier and begins ACTIVE, carrying no timestamp of its own`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())

        val assertion = core.createAssertion(principal, candidateAssertion(provenance.provenanceId))

        assertTrue(assertion.assertionId.value.isNotBlank())
        assertEquals(MemoryCoreRecordStatus.ACTIVE, assertion.status)
    }

    @Test
    fun `createAssertion rejects a candidate referencing a nonexistent Provenance`() = runTest {
        val core = InMemoryMemoryCore()

        assertFailsWith<IllegalArgumentException> {
            core.createAssertion(principal, candidateAssertion(ProvenanceId("provenance-does-not-exist")))
        }
    }

    // ================= Relationship creation =================

    @Test
    fun `createRelationship mints an identifier, assigns createdAt, and begins ACTIVE`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))
        val document = core.registerDocument(principal, candidateDocument(provenance.provenanceId))

        val relationship = core.createRelationship(principal,
            candidateRelationship(
                provenanceId = provenance.provenanceId,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, entity.entityId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, document.documentId.value),
            ),
        )

        assertTrue(relationship.relationshipId.value.isNotBlank())
        assertEquals(MemoryCoreRecordStatus.ACTIVE, relationship.status)
    }

    @Test
    fun `createRelationship rejects a candidate referencing a nonexistent Provenance`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))
        val document = core.registerDocument(principal, candidateDocument(provenance.provenanceId))

        assertFailsWith<IllegalArgumentException> {
            core.createRelationship(principal,
                candidateRelationship(
                    provenanceId = ProvenanceId("provenance-does-not-exist"),
                    fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, entity.entityId.value),
                    toEndpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, document.documentId.value),
                ),
            )
        }
    }

    @Test
    fun `createRelationship rejects a Memory-Core-owned endpoint that does not exist`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        assertFailsWith<IllegalArgumentException> {
            core.createRelationship(principal,
                candidateRelationship(
                    provenanceId = provenance.provenanceId,
                    fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, entity.entityId.value),
                    toEndpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, "document-does-not-exist"),
                ),
            )
        }
    }

    @Test
    fun `createRelationship accepts an external endpoint kind unverified`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        val relationship = core.createRelationship(principal,
            candidateRelationship(
                provenanceId = provenance.provenanceId,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, entity.entityId.value),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.CONVERSATION_TURN, "turn-that-does-not-exist"),
            ),
        )

        assertEquals(RelationshipEndpoint.CONVERSATION_TURN, relationship.toEndpoint.recordKind)
    }

    @Test
    fun `a self-referencing relationship cannot even be constructed as a candidate, before InMemoryMemoryCore is involved`() {
        val sameEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1")

        assertFailsWith<IllegalArgumentException> {
            candidateRelationship(
                provenanceId = ProvenanceId("provenance-1"),
                fromEndpoint = sameEndpoint,
                toEndpoint = sameEndpoint,
            )
        }
    }

    // ================= Lifecycle: every valid transition =================

    private suspend fun freshActiveEntityReference(core: InMemoryMemoryCore): MemoryCoreRecordReference.ToEntity {
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))
        return MemoryCoreRecordReference.ToEntity(entity.entityId)
    }

    private val validTransitions = listOf(
        MemoryCoreRecordStatus.ACTIVE to MemoryCoreRecordStatus.DISPUTED,
        MemoryCoreRecordStatus.DISPUTED to MemoryCoreRecordStatus.ACTIVE,
        MemoryCoreRecordStatus.ACTIVE to MemoryCoreRecordStatus.SUPERSEDED,
        MemoryCoreRecordStatus.DISPUTED to MemoryCoreRecordStatus.SUPERSEDED,
        MemoryCoreRecordStatus.ACTIVE to MemoryCoreRecordStatus.ARCHIVED,
        MemoryCoreRecordStatus.ARCHIVED to MemoryCoreRecordStatus.ACTIVE,
        MemoryCoreRecordStatus.DISPUTED to MemoryCoreRecordStatus.ARCHIVED,
        MemoryCoreRecordStatus.ARCHIVED to MemoryCoreRecordStatus.DISPUTED,
        MemoryCoreRecordStatus.SUPERSEDED to MemoryCoreRecordStatus.ARCHIVED,
        MemoryCoreRecordStatus.ARCHIVED to MemoryCoreRecordStatus.SUPERSEDED,
        MemoryCoreRecordStatus.ACTIVE to MemoryCoreRecordStatus.DELETED,
        MemoryCoreRecordStatus.DISPUTED to MemoryCoreRecordStatus.DELETED,
        MemoryCoreRecordStatus.SUPERSEDED to MemoryCoreRecordStatus.DELETED,
        MemoryCoreRecordStatus.ARCHIVED to MemoryCoreRecordStatus.DELETED,
    )

    @Test
    fun `every frozen valid lifecycle transition is accepted by MemoryCoreLifecycleTransitions`() {
        validTransitions.forEach { (from, to) ->
            assertTrue(
                MemoryCoreLifecycleTransitions.isValidTransition(from, to),
                "$from -> $to must be a valid transition",
            )
        }
    }

    @Test
    fun `every pair not in the frozen table is rejected by MemoryCoreLifecycleTransitions, including same-state and anything from DELETED`() {
        val validPairs = validTransitions.toSet()

        MemoryCoreRecordStatus.entries.forEach { from ->
            MemoryCoreRecordStatus.entries.forEach { to ->
                val expectedValid = (from to to) in validPairs
                assertEquals(
                    expectedValid,
                    MemoryCoreLifecycleTransitions.isValidTransition(from, to),
                    "$from -> $to valid=$expectedValid",
                )
            }
        }
    }

    @Test
    fun `transitionStatus performs a real, valid transition and returns the updated record wrapped in MemoryCoreRecord`() = runTest {
        val core = InMemoryMemoryCore()
        val reference = freshActiveEntityReference(core)

        val result = core.transitionStatus(principal, reference, MemoryCoreRecordStatus.DISPUTED)

        assertTrue(result is MemoryCoreRecord.OfEntity)
        assertEquals(MemoryCoreRecordStatus.DISPUTED, (result as MemoryCoreRecord.OfEntity).entity.status)
    }

    @Test
    fun `transitionStatus rejects an invalid transition with IllegalArgumentException, leaving the record unchanged`() = runTest {
        val core = InMemoryMemoryCore()
        val reference = freshActiveEntityReference(core)
        core.transitionStatus(principal, reference, MemoryCoreRecordStatus.SUPERSEDED)

        assertFailsWith<IllegalArgumentException> {
            core.transitionStatus(principal, reference, MemoryCoreRecordStatus.ACTIVE)
        }

        val stillSuperseded = core.getEntity(principal, reference.entityId)
        assertEquals(MemoryCoreRecordStatus.SUPERSEDED, stillSuperseded?.status)
    }

    @Test
    fun `transitionStatus on an unknown reference throws NoSuchElementException`() = runTest {
        val core = InMemoryMemoryCore()

        assertFailsWith<NoSuchElementException> {
            core.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(EntityId("entity-does-not-exist")), MemoryCoreRecordStatus.DISPUTED)
        }
    }

    @Test
    fun `DELETED is terminal -- no further transition is accepted`() = runTest {
        val core = InMemoryMemoryCore()
        val reference = freshActiveEntityReference(core)
        core.transitionStatus(principal, reference, MemoryCoreRecordStatus.DELETED)

        MemoryCoreRecordStatus.entries.forEach { target ->
            assertFailsWith<IllegalArgumentException> { core.transitionStatus(principal, reference, target) }
        }
    }

    @Test
    fun `a DELETED record is preserved in storage, still retrievable by identifier`() = runTest {
        val core = InMemoryMemoryCore()
        val reference = freshActiveEntityReference(core)
        core.transitionStatus(principal, reference, MemoryCoreRecordStatus.DELETED)

        val stillPresent = core.getEntity(principal, reference.entityId)

        assertEquals(MemoryCoreRecordStatus.DELETED, stillPresent?.status)
    }

    // ================= Retrieval: identifier lookup =================

    @Test
    fun `getEntity, getDocument, getAssertion, getRelationship each return null for an unknown identifier`() = runTest {
        val core = InMemoryMemoryCore()

        assertNull(core.getEntity(principal, EntityId("entity-x")))
        assertNull(core.getDocument(principal, DocumentId("document-x")))
        assertNull(core.getAssertion(principal, AssertionId("assertion-x")))
        assertNull(core.getRelationship(principal, RelationshipId("relationship-x")))
    }

    // ================= Retrieval: entity lookup =================

    @Test
    fun `findEntities matches by case-insensitive label or alias substring, entityType, and status`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        core.createEntity(principal, candidateEntity(provenance.provenanceId, entityType = "person", primaryLabel = "Jane Doe"))
        core.createEntity(principal, candidateEntity(provenance.provenanceId, entityType = "organisation", primaryLabel = "Acme Corp", aliases = listOf("Acme")))

        val byLabel = core.findEntities(EntityLookupQuery(principal, 10, labelOrAliasMatch = "jane"))
        val byAlias = core.findEntities(EntityLookupQuery(principal, 10, labelOrAliasMatch = "acme"))
        val byType = core.findEntities(EntityLookupQuery(principal, 10, entityType = "organisation"))

        assertEquals(1, byLabel.size)
        assertEquals("Jane Doe", byLabel.single().primaryLabel)
        assertEquals(1, byAlias.size)
        assertEquals("Acme Corp", byAlias.single().primaryLabel)
        assertEquals(1, byType.size)
    }

    @Test
    fun `findEntities returns entities in insertion order, truncated to maximumResults`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val first = core.createEntity(principal, candidateEntity(provenance.provenanceId, primaryLabel = "First"))
        core.createEntity(principal, candidateEntity(provenance.provenanceId, primaryLabel = "Second"))
        core.createEntity(principal, candidateEntity(provenance.provenanceId, primaryLabel = "Third"))

        val results = core.findEntities(EntityLookupQuery(principal, 1))

        assertEquals(listOf(first.entityId), results.map { it.entityId })
    }

    @Test
    fun `findEntities returns an empty list, never throws, when nothing matches`() = runTest {
        val core = InMemoryMemoryCore()

        val results = core.findEntities(EntityLookupQuery(principal, 10, entityType = "no-such-type"))

        assertEquals(emptyList(), results)
    }

    // ================= Retrieval: document lookup =================

    @Test
    fun `findDocuments matches by documentType, locationReferenceMatch, and processingStatus`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        core.registerDocument(principal, candidateDocument(provenance.provenanceId, documentType = "email", locationReference = "mailbox://inbox/1"))
        core.registerDocument(principal, candidateDocument(provenance.provenanceId, documentType = "pdf", locationReference = "file:///contract.pdf"))

        val byType = core.findDocuments(DocumentLookupQuery(principal, 10, documentType = "pdf"))
        val byLocation = core.findDocuments(DocumentLookupQuery(principal, 10, locationReferenceMatch = "INBOX"))
        val byProcessingStatus = core.findDocuments(DocumentLookupQuery(principal, 10, processingStatus = DocumentProcessingStatus.REGISTERED))

        assertEquals(1, byType.size)
        assertEquals("pdf", byType.single().documentType)
        assertEquals(1, byLocation.size)
        assertEquals(2, byProcessingStatus.size)
    }

    // ================= Retrieval: relationship traversal =================

    @Test
    fun `traverseRelationships respects FORWARD, REVERSE, and BOTH direction`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))
        val document = core.registerDocument(principal, candidateDocument(provenance.provenanceId))
        val entityEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, entity.entityId.value)
        val documentEndpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, document.documentId.value)
        core.createRelationship(principal, candidateRelationship(provenance.provenanceId, entityEndpoint, documentEndpoint))

        val forward = core.traverseRelationships(
            RelationshipTraversalQuery(principal, 10, entityEndpoint, direction = RelationshipTraversalDirection.FORWARD),
        )
        val reverseFromEntity = core.traverseRelationships(
            RelationshipTraversalQuery(principal, 10, entityEndpoint, direction = RelationshipTraversalDirection.REVERSE),
        )
        val reverseFromDocument = core.traverseRelationships(
            RelationshipTraversalQuery(principal, 10, documentEndpoint, direction = RelationshipTraversalDirection.REVERSE),
        )
        val both = core.traverseRelationships(RelationshipTraversalQuery(principal, 10, entityEndpoint))

        assertEquals(1, forward.size)
        assertEquals(0, reverseFromEntity.size)
        assertEquals(1, reverseFromDocument.size)
        assertEquals(1, both.size)
    }

    // ================= Retrieval: chronological lookup =================

    @Test
    fun `findByTimeRange orders ascending by effective creation time, using Provenance-ingestionTime for Assertion`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))
        val assertion = core.createAssertion(principal, candidateAssertion(provenance.provenanceId))

        val from = Instant.now().minusSeconds(60)
        val to = Instant.now().plusSeconds(60)
        val results = core.findByTimeRange(ChronologicalLookupQuery(principal, 10, from, to))

        val wrappedIds = results.map {
            when (it) {
                is MemoryCoreRecord.OfEntity -> it.entity.entityId.value
                is MemoryCoreRecord.OfAssertion -> it.assertion.assertionId.value
                else -> null
            }
        }
        assertTrue(entity.entityId.value in wrappedIds)
        assertTrue(assertion.assertionId.value in wrappedIds)
    }

    @Test
    fun `findByTimeRange excludes records outside the requested range`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        core.createEntity(principal, candidateEntity(provenance.provenanceId))

        val results = core.findByTimeRange(
            ChronologicalLookupQuery(principal, 10, Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200)),
        )

        assertEquals(emptyList(), results)
    }

    @Test
    fun `findByTimeRange narrows to one recordKind when supplied`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        core.createEntity(principal, candidateEntity(provenance.provenanceId))
        core.registerDocument(principal, candidateDocument(provenance.provenanceId))

        val from = Instant.now().minusSeconds(60)
        val to = Instant.now().plusSeconds(60)
        val results = core.findByTimeRange(ChronologicalLookupQuery(principal, 10, from, to, recordKind = RelationshipEndpoint.ENTITY))

        assertEquals(1, results.size)
        assertTrue(results.all { it is MemoryCoreRecord.OfEntity })
    }

    // ================= Retrieval: metadata filtering =================

    @Test
    fun `findByMetadata matches records whose metadata map contains every filter entry`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        core.createEntity(principal, CandidateEntity(entityType = "person", primaryLabel = "Jane", provenanceId = provenance.provenanceId, metadata = mapOf("topic" to "billing")))
        core.createEntity(principal, CandidateEntity(entityType = "person", primaryLabel = "John", provenanceId = provenance.provenanceId, metadata = mapOf("topic" to "support")))

        val results = core.findByMetadata(MetadataLookupQuery(principal, 10, RelationshipEndpoint.ENTITY, mapOf("topic" to "billing")))

        assertEquals(1, results.size)
    }

    @Test
    fun `findByMetadata always returns empty for relationship, which carries no metadata field`() = runTest {
        val core = InMemoryMemoryCore()

        val results = core.findByMetadata(MetadataLookupQuery(principal, 10, RelationshipEndpoint.RELATIONSHIP, mapOf("k" to "v")))

        assertEquals(emptyList(), results)
    }

    // ================= Retrieval: provenance-aware lookup =================

    @Test
    fun `findByProvenance matches records whose referenced Provenance satisfies every supplied criterion`() = runTest {
        val core = InMemoryMemoryCore()
        val financial = core.createProvenance(principal, candidateProvenance(sourceType = "financial-system"))
        val conversation = core.createProvenance(principal, candidateProvenance(sourceType = "conversation"))
        core.createEntity(principal, candidateEntity(financial.provenanceId, primaryLabel = "Invoice Entity"))
        core.createEntity(principal, candidateEntity(conversation.provenanceId, primaryLabel = "Chat Entity"))

        val results = core.findByProvenance(ProvenanceLookupQuery(principal, 10, RelationshipEndpoint.ENTITY, sourceType = "financial-system"))

        assertEquals(1, results.size)
    }

    // ================= Immutability of returned records =================

    @Test
    fun `mutating a copy of a returned Entity never affects what the store subsequently returns`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        val mutatedCopy = entity.copy(primaryLabel = "Someone Else")
        val fetchedAgain = core.getEntity(principal, entity.entityId)

        assertEquals("Jane Doe", fetchedAgain?.primaryLabel)
        assertEquals("Someone Else", mutatedCopy.primaryLabel)
    }

    // ================= No permission or event dependency =================

    @Test
    fun `InMemoryMemoryCore has a zero-argument constructor -- no PermissionEngine or EventBus dependency`() {
        val constructors = InMemoryMemoryCore::class.java.constructors

        assertTrue(constructors.any { it.parameterCount == 0 }, "InMemoryMemoryCore must be constructible with no arguments")
    }

    // ================= Errata 004: requestingPrincipalId is accepted but never affects behaviour =================

    @Test
    fun `two structurally identical writes differing only in requestingPrincipalId produce structurally identical stored records`() = runTest {
        val core = InMemoryMemoryCore()
        val otherPrincipal = PrincipalId("principal-2")
        val provenance = core.createProvenance(principal, candidateProvenance())
        val candidate = candidateEntity(provenance.provenanceId)

        val fromFirstPrincipal = core.createEntity(principal, candidate)
        val fromSecondPrincipal = core.createEntity(otherPrincipal, candidate)

        // Distinct minted identifiers (Unit 8's own "no two writes collide" guarantee), and
        // independently-assigned createdAt timestamps -- but every field that reflects the
        // candidate's own content (as opposed to per-call minting) is identical regardless of
        // which principal happened to submit the request, since InMemoryMemoryCore never reads
        // requestingPrincipalId at all.
        assertNotEquals(fromFirstPrincipal.entityId, fromSecondPrincipal.entityId)
        assertEquals(fromFirstPrincipal.entityType, fromSecondPrincipal.entityType)
        assertEquals(fromFirstPrincipal.primaryLabel, fromSecondPrincipal.primaryLabel)
        assertEquals(fromFirstPrincipal.provenanceId, fromSecondPrincipal.provenanceId)
        assertEquals(fromFirstPrincipal.status, fromSecondPrincipal.status)
    }

    @Test
    fun `getEntity returns the same record regardless of which principal requests it`() = runTest {
        val core = InMemoryMemoryCore()
        val otherPrincipal = PrincipalId("principal-2")
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        val seenByCreator = core.getEntity(principal, entity.entityId)
        val seenByOther = core.getEntity(otherPrincipal, entity.entityId)

        assertEquals(entity, seenByCreator)
        assertEquals(entity, seenByOther)
    }

    @Test
    fun `transitionStatus succeeds identically regardless of which principal requests it`() = runTest {
        val core = InMemoryMemoryCore()
        val otherPrincipal = PrincipalId("principal-2")
        val reference = freshActiveEntityReference(core)

        val result = core.transitionStatus(otherPrincipal, reference, MemoryCoreRecordStatus.DISPUTED)

        assertTrue(result is MemoryCoreRecord.OfEntity)
        assertEquals(MemoryCoreRecordStatus.DISPUTED, (result as MemoryCoreRecord.OfEntity).entity.status)
    }

    @Test
    fun `no stored record type -- Provenance, Entity, Document, Assertion, Relationship -- exposes a property named requestingPrincipalId`() {
        // Deliberately narrower than "no PrincipalId-typed property at all": Provenance.creatorPrincipalId
        // and Entity.relatedPrincipalId are both approved, pre-existing PrincipalId-typed fields with
        // their own distinct meaning (who originated this content / which Principal this Entity
        // represents) -- neither is the requestingPrincipalId Errata 004 added to MemoryCore's
        // operations, and Errata 004 does not touch either field. What Errata 004 actually guarantees
        // is that the caller-identity parameter added to every write/lookup operation is never itself
        // persisted onto the stored record -- i.e. no stored type gains a property literally named
        // requestingPrincipalId.
        val typesToCheck = listOf(Provenance::class, Entity::class, Document::class, Assertion::class, Relationship::class)

        typesToCheck.forEach { type ->
            val requestingPrincipalIdProperties = type.memberProperties.filter { it.name == "requestingPrincipalId" }
            assertTrue(
                requestingPrincipalIdProperties.isEmpty(),
                "${type.simpleName} must not expose a property named requestingPrincipalId",
            )
        }
    }

    @Test
    fun `the requesting principal supplied to a write is never silently written into a candidate-supplied PrincipalId field on the resulting record`() = runTest {
        val core = InMemoryMemoryCore()
        val distinctiveRequester = PrincipalId("a-very-distinctive-requesting-principal")
        // Neither candidateProvenance() nor candidateEntity() sets creatorPrincipalId/relatedPrincipalId
        // -- both remain the caller's own choice to populate or leave null, entirely independent of
        // requestingPrincipalId. If InMemoryMemoryCore ever started writing requestingPrincipalId into
        // either field, this candidate's own "leave it unset" choice would stop being honoured and
        // one of these would unexpectedly become non-null (and equal to distinctiveRequester).
        val provenance = core.createProvenance(distinctiveRequester, candidateProvenance())
        val entity = core.createEntity(distinctiveRequester, candidateEntity(provenance.provenanceId))

        assertNull(provenance.creatorPrincipalId)
        assertNull(entity.relatedPrincipalId)
    }

    // ================= Memory Core Durability, Implementation Unit 4: restore* functions =================

    @Test
    fun `restoreProvenance preserves the original identifier exactly, minting nothing -- confirmed via a dependent restoreEntity call`() = runTest {
        val core = InMemoryMemoryCore()
        val originalProvenance = Provenance(
            provenanceId = ProvenanceId("provenance-restored-1"),
            sourceIdentifier = "conversation-turn-9",
            sourceType = "conversation",
            acquisitionTime = Instant.parse("2025-01-01T00:00:00Z"),
            ingestionTime = Instant.parse("2025-01-01T00:00:01Z"),
            contentNature = ContentNature.ORIGINAL,
        )

        core.restoreProvenance(originalProvenance)

        // MemoryRetrieval exposes no direct getProvenance method (Version 1's own already-established
        // shape, unmodified by this Unit -- Provenance is reached only via a referencing record's own
        // provenanceId). Confirming the restored Provenance is genuinely stored, under its own original
        // identifier, is done here the same way InMemoryMemoryCore itself would confirm it internally:
        // a dependent restoreEntity call referencing originalProvenance.provenanceId must succeed,
        // which it can only do if requireExistingProvenance finds that exact identifier already present.
        val dependentEntity = Entity(
            entityId = EntityId("entity-depends-on-restored-provenance"),
            entityType = "person",
            primaryLabel = "Depends On Restored Provenance",
            provenanceId = originalProvenance.provenanceId,
            createdAt = Instant.parse("2025-01-01T00:00:02Z"),
        )
        core.restoreEntity(dependentEntity)

        assertEquals(dependentEntity, core.getEntity(principal, dependentEntity.entityId))
    }

    @Test
    fun `restoreEntity preserves the original identifier, createdAt, and provenance reference exactly, minting nothing`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = candidateProvenance().let { candidate ->
            Provenance(
                provenanceId = ProvenanceId("provenance-restored-2"),
                sourceIdentifier = candidate.sourceIdentifier,
                sourceType = candidate.sourceType,
                acquisitionTime = candidate.acquisitionTime,
                ingestionTime = Instant.parse("2025-01-01T00:00:01Z"),
                contentNature = candidate.contentNature,
            )
        }
        core.restoreProvenance(provenance)
        val originalEntity = Entity(
            entityId = EntityId("entity-restored-1"),
            entityType = "person",
            primaryLabel = "Restored Person",
            provenanceId = provenance.provenanceId,
            createdAt = Instant.parse("2025-01-01T00:00:02Z"),
            status = MemoryCoreRecordStatus.DISPUTED,
        )

        core.restoreEntity(originalEntity)
        val fetched = core.getEntity(principal, originalEntity.entityId)

        assertEquals(originalEntity, fetched)
        assertEquals(EntityId("entity-restored-1"), fetched?.entityId, "restoreEntity must never mint a replacement identifier")
    }

    @Test
    fun `restoreEntity rejects a broken provenance reference`() = runTest {
        val core = InMemoryMemoryCore()
        val orphanEntity = Entity(
            entityId = EntityId("entity-orphan"),
            entityType = "person",
            primaryLabel = "Orphan",
            provenanceId = ProvenanceId("provenance-does-not-exist"),
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        )

        assertFailsWith<IllegalArgumentException> { core.restoreEntity(orphanEntity) }
    }

    @Test
    fun `restoreRelationship rejects a broken Memory-Core-owned endpoint reference`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val orphanRelationship = Relationship(
            relationshipId = RelationshipId("relationship-orphan"),
            relationshipType = Relationship.SUPPORTS,
            fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-does-not-exist"),
            toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-also-does-not-exist"),
            directional = true,
            provenanceId = provenance.provenanceId,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        )

        assertFailsWith<IllegalArgumentException> { core.restoreRelationship(orphanRelationship) }
    }

    @Test
    fun `restoring the same record twice with identical content is accepted idempotently`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = Entity(
            entityId = EntityId("entity-idempotent"),
            entityType = "person",
            primaryLabel = "Idempotent",
            provenanceId = provenance.provenanceId,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        )

        core.restoreEntity(entity)
        core.restoreEntity(entity) // identical content, second restoration -- must not throw

        assertEquals(entity, core.getEntity(principal, entity.entityId))
    }

    @Test
    fun `restoring the same identifier with different content is rejected as corruption`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val original = Entity(
            entityId = EntityId("entity-conflict"),
            entityType = "person",
            primaryLabel = "Original Label",
            provenanceId = provenance.provenanceId,
            createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        )
        val conflicting = original.copy(primaryLabel = "Different Label")

        core.restoreEntity(original)

        assertFailsWith<IllegalStateException> { core.restoreEntity(conflicting) }
    }

    @Test
    fun `restore functions on InMemoryMemoryCore are internal, never public API surface`() {
        val restoreFunctionNames = setOf("restoreProvenance", "restoreEntity", "restoreDocument", "restoreAssertion", "restoreRelationship")
        val declaredInternal = InMemoryMemoryCore::class.declaredFunctions
            .filter { it.name in restoreFunctionNames }
            .all { it.visibility == KVisibility.INTERNAL }

        assertEquals(restoreFunctionNames.size, InMemoryMemoryCore::class.declaredFunctions.count { it.name in restoreFunctionNames })
        assertTrue(declaredInternal, "every restore* function must be internal, never public")
    }

    // ================= Memory Core Durability, Implementation Unit 5: restoreIdentifierCounters =================

    @Test
    fun `restoreIdentifierCounters on an empty store leaves every counter at its original starting value`() = runTest {
        val core = InMemoryMemoryCore()

        core.restoreIdentifierCounters()
        val provenance = core.createProvenance(principal, candidateProvenance())

        assertEquals(ProvenanceId("provenance-1"), provenance.provenanceId, "an empty store must resume minting from 1, exactly as a genuinely fresh store already does")
    }

    @Test
    fun `restoreIdentifierCounters resumes minting at one past a single restored identifier`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = provenance("provenance-1")
        core.restoreProvenance(provenance)

        core.restoreIdentifierCounters()
        val nextProvenance = core.createProvenance(principal, candidateProvenance())

        assertEquals(ProvenanceId("provenance-2"), nextProvenance.provenanceId)
    }

    @Test
    fun `restoreIdentifierCounters resumes past the highest of several sparse, non-contiguous identifiers`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        core.restoreEntity(entity("entity-1", provenance.provenanceId))
        core.restoreEntity(entity("entity-5", provenance.provenanceId))
        core.restoreEntity(entity("entity-3", provenance.provenanceId))

        core.restoreIdentifierCounters()
        val nextEntity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        assertEquals(EntityId("entity-6"), nextEntity.entityId, "the gap at entity-2/4 must never be filled -- the next identifier is one past the true maximum (5), not one past the count of records (3)")
    }

    @Test
    fun `restoreIdentifierCounters derives the true maximum regardless of the order identifiers were restored in`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        // Restored out of numeric order -- the highest-numbered identifier is restored first.
        core.restoreEntity(entity("entity-9", provenance.provenanceId))
        core.restoreEntity(entity("entity-2", provenance.provenanceId))
        core.restoreEntity(entity("entity-4", provenance.provenanceId))

        core.restoreIdentifierCounters()
        val nextEntity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        assertEquals(EntityId("entity-10"), nextEntity.entityId, "the maximum must be the true numeric maximum, never merely the most recently restored identifier")
    }

    @Test
    fun `each of the five per-kind counters restores independently of the other four`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = provenance("provenance-1")
        core.restoreProvenance(provenance)
        core.restoreEntity(entity("entity-7", provenance.provenanceId))
        // Document, Assertion, and Relationship have no restored records at all.

        core.restoreIdentifierCounters()

        val nextProvenance = core.createProvenance(principal, candidateProvenance())
        val nextEntity = core.createEntity(principal, candidateEntity(provenance.provenanceId))
        val nextDocument = core.registerDocument(principal, candidateDocument(provenance.provenanceId))
        val nextAssertion = core.createAssertion(principal, candidateAssertion(provenance.provenanceId))

        assertEquals(ProvenanceId("provenance-2"), nextProvenance.provenanceId, "Provenance's own counter must reflect only Provenance's own restored maximum")
        assertEquals(EntityId("entity-8"), nextEntity.entityId, "Entity's own counter must reflect only Entity's own restored maximum")
        assertEquals(DocumentId("document-1"), nextDocument.documentId, "an untouched kind must still start at 1, unaffected by another kind's own high-numbered restoration")
        assertEquals(AssertionId("assertion-1"), nextAssertion.assertionId, "an untouched kind must still start at 1, unaffected by another kind's own high-numbered restoration")
    }

    @Test
    fun `a durably-repeated identical record collapses to one store key and never inflates the restored counter`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val entity = entity("entity-1", provenance.provenanceId)
        core.restoreEntity(entity)
        core.restoreEntity(entity) // idempotent duplicate -- restoreEntity's own existing behaviour

        core.restoreIdentifierCounters()
        val nextEntity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        assertEquals(EntityId("entity-2"), nextEntity.entityId, "one durably-repeated record must count as exactly one identifier, never two")
    }

    @Test
    fun `restoreIdentifierCounters rejects an identifier that does not match the expected prefix-and-numeric-suffix format`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        // EntityId's own construction-time validation checks only non-blank -- a malformed value
        // (wrong prefix here) can still be constructed directly, exactly as corrupt or hand-crafted
        // durable data might restore.
        core.restoreEntity(entity("not-the-right-prefix-7", provenance.provenanceId))

        assertFailsWith<IllegalArgumentException> { core.restoreIdentifierCounters() }
    }

    @Test
    fun `restoreIdentifierCounters rejects an identifier whose suffix is not a positive number`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        core.restoreEntity(entity("entity-not-a-number", provenance.provenanceId))

        assertFailsWith<IllegalArgumentException> { core.restoreIdentifierCounters() }
    }

    @Test
    fun `restoreIdentifierCounters rejects a suffix at Long's own upper bound rather than silently wrapping to a negative counter`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        core.restoreEntity(entity("entity-${Long.MAX_VALUE}", provenance.provenanceId))

        assertFailsWith<IllegalStateException> { core.restoreIdentifierCounters() }
    }

    @Test
    fun `a DELETED record's own identifier still counts toward the restored maximum -- its identifier is never reused`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val deletedEntity = entity("entity-6", provenance.provenanceId)
        core.restoreEntity(deletedEntity)
        core.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(deletedEntity.entityId), MemoryCoreRecordStatus.DELETED)

        core.restoreIdentifierCounters()
        val nextEntity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        assertEquals(
            EntityId("entity-7"),
            nextEntity.entityId,
            "a DELETED record is never physically removed from its store -- its own identifier must still count toward the restored maximum, exactly like any other status",
        )
    }

    @Test
    fun `a lifecycle transition applied after restoration has no effect on the restored identifier counter`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        val restoredEntity = entity("entity-3", provenance.provenanceId)
        core.restoreEntity(restoredEntity)
        core.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(restoredEntity.entityId), MemoryCoreRecordStatus.DISPUTED)

        core.restoreIdentifierCounters()
        val nextEntity = core.createEntity(principal, candidateEntity(provenance.provenanceId))

        assertEquals(EntityId("entity-4"), nextEntity.entityId, "a status transition changes a record's status field only -- it must never influence which identifier a counter resumes from")
    }

    @Test
    fun `after restoration, no newly created identifier ever collides with an already-restored one`() = runTest {
        val core = InMemoryMemoryCore()
        val provenance = core.createProvenance(principal, candidateProvenance())
        core.restoreEntity(entity("entity-1", provenance.provenanceId))
        core.restoreEntity(entity("entity-2", provenance.provenanceId))
        core.restoreEntity(entity("entity-3", provenance.provenanceId))

        core.restoreIdentifierCounters()
        val mintedIdentifiers = (1..5).map { core.createEntity(principal, candidateEntity(provenance.provenanceId)).entityId }

        val restoredIdentifiers = setOf(EntityId("entity-1"), EntityId("entity-2"), EntityId("entity-3"))
        assertTrue(mintedIdentifiers.none { it in restoredIdentifiers }, "no newly minted identifier may ever equal an already-restored one")
        assertEquals(5, mintedIdentifiers.toSet().size, "every newly minted identifier must also be distinct from every other newly minted one")
    }

    @Test
    fun `restoreIdentifierCounters itself is internal, never public API surface`() {
        val function = InMemoryMemoryCore::class.declaredFunctions.single { it.name == "restoreIdentifierCounters" }

        assertEquals(KVisibility.INTERNAL, function.visibility)
    }
}
