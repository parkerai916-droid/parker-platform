package parker.core.runtime

import java.time.Instant
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.ContentNature
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentProcessingStatus
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId

/**
 * Memory Core Durability, Implementation Unit 1 (Durable Record Format
 * Boundary). Structural tests for [DurableMemoryCoreEntry] only -- pure
 * data-shape validation, mirroring `MemoryCoreInterfacesTest.kt`'s and
 * `MemoryCoreCandidatesTest.kt`'s own established "shape only" scope for
 * a governing task's own first Unit. No persistence, no append log, no
 * replay, no file I/O, and no [InMemoryMemoryCore] dependency exists
 * anywhere in this file -- each of those is a later Unit's own
 * responsibility, and none is needed to test a pure, in-memory data
 * model.
 */
class DurableMemoryCoreEntryTest {

    private val provenance = Provenance(
        provenanceId = ProvenanceId("provenance-1"),
        sourceIdentifier = "conversation-turn-1",
        sourceType = "conversation",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        ingestionTime = Instant.parse("2026-01-01T00:00:01Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private val entity = Entity(
        entityId = EntityId("entity-1"),
        entityType = "person",
        primaryLabel = "Jane Doe",
        provenanceId = provenance.provenanceId,
        createdAt = Instant.parse("2026-01-01T00:00:02Z"),
    )

    private val document = Document(
        documentId = DocumentId("document-1"),
        documentType = "email",
        locationReference = "mailbox://inbox/message-1",
        provenanceId = provenance.provenanceId,
        registeredAt = Instant.parse("2026-01-01T00:00:03Z"),
        processingStatus = DocumentProcessingStatus.REGISTERED,
    )

    private val assertion = Assertion(
        assertionId = AssertionId("assertion-1"),
        statement = "the user prefers window seats",
        provenanceId = provenance.provenanceId,
    )

    private val relationship = Relationship(
        relationshipId = RelationshipId("relationship-1"),
        relationshipType = Relationship.SUPPORTS,
        fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1"),
        toEndpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, "document-1"),
        directional = true,
        provenanceId = provenance.provenanceId,
        createdAt = Instant.parse("2026-01-01T00:00:04Z"),
    )

    // ================= Exactly six cases =================

    @Test
    fun `DurableMemoryCoreEntry has exactly the six expected cases -- one per MemoryCore write operation`() {
        val expectedNames = setOf(
            "ProvenanceCreated",
            "EntityCreated",
            "DocumentRegistered",
            "AssertionCreated",
            "RelationshipCreated",
            "StatusTransitioned",
        )

        assertEquals(
            expectedNames,
            DurableMemoryCoreEntry::class.sealedSubclasses.mapNotNull { it.simpleName }.toSet(),
        )
    }

    // ================= Per-kind construction: identifiers, timestamps, provenance links preserved =================

    @Test
    fun `ProvenanceCreated preserves the wrapped Provenance's own identifier, timestamps, and content nature unchanged`() {
        val durableEntry = DurableMemoryCoreEntry.ProvenanceCreated(provenance = provenance)

        assertEquals(provenance.provenanceId, durableEntry.provenance.provenanceId)
        assertEquals(provenance.acquisitionTime, durableEntry.provenance.acquisitionTime)
        assertEquals(provenance.ingestionTime, durableEntry.provenance.ingestionTime)
        assertEquals(provenance.contentNature, durableEntry.provenance.contentNature)
        assertEquals(provenance, durableEntry.provenance)
    }

    @Test
    fun `EntityCreated preserves the wrapped Entity's own identifier, createdAt, and provenance link unchanged`() {
        val durableEntry = DurableMemoryCoreEntry.EntityCreated(entity = entity)

        assertEquals(entity.entityId, durableEntry.entity.entityId)
        assertEquals(entity.createdAt, durableEntry.entity.createdAt)
        assertEquals(provenance.provenanceId, durableEntry.entity.provenanceId)
        assertEquals(entity, durableEntry.entity)
    }

    @Test
    fun `DocumentRegistered preserves the wrapped Document's own identifier, registeredAt, and provenance link unchanged`() {
        val durableEntry = DurableMemoryCoreEntry.DocumentRegistered(document = document)

        assertEquals(document.documentId, durableEntry.document.documentId)
        assertEquals(document.registeredAt, durableEntry.document.registeredAt)
        assertEquals(provenance.provenanceId, durableEntry.document.provenanceId)
        assertEquals(document, durableEntry.document)
    }

    @Test
    fun `AssertionCreated preserves the wrapped Assertion's own identifier and provenance link unchanged`() {
        val durableEntry = DurableMemoryCoreEntry.AssertionCreated(assertion = assertion)

        assertEquals(assertion.assertionId, durableEntry.assertion.assertionId)
        assertEquals(provenance.provenanceId, durableEntry.assertion.provenanceId)
        assertEquals(assertion, durableEntry.assertion)
    }

    @Test
    fun `RelationshipCreated preserves the wrapped Relationship's own identifier, endpoints, createdAt, and provenance link unchanged`() {
        val durableEntry = DurableMemoryCoreEntry.RelationshipCreated(relationship = relationship)

        assertEquals(relationship.relationshipId, durableEntry.relationship.relationshipId)
        assertEquals(relationship.fromEndpoint, durableEntry.relationship.fromEndpoint)
        assertEquals(relationship.toEndpoint, durableEntry.relationship.toEndpoint)
        assertEquals(relationship.createdAt, durableEntry.relationship.createdAt)
        assertEquals(provenance.provenanceId, durableEntry.relationship.provenanceId)
        assertEquals(relationship, durableEntry.relationship)
    }

    // ================= Lifecycle transition representation =================

    @Test
    fun `StatusTransitioned preserves the target record reference, prior and target status, and its own transition timestamp`() {
        val transitionedAt = Instant.parse("2026-01-01T00:00:05Z")

        val durableEntry = DurableMemoryCoreEntry.StatusTransitioned(
            reference = MemoryCoreRecordReference.ToEntity(entity.entityId),
            priorStatus = MemoryCoreRecordStatus.ACTIVE,
            targetStatus = MemoryCoreRecordStatus.DISPUTED,
            transitionedAt = transitionedAt,
        )

        assertEquals(MemoryCoreRecordReference.ToEntity(entity.entityId), durableEntry.reference)
        assertEquals(MemoryCoreRecordStatus.ACTIVE, durableEntry.priorStatus)
        assertEquals(MemoryCoreRecordStatus.DISPUTED, durableEntry.targetStatus)
        assertEquals(transitionedAt, durableEntry.transitionedAt)
    }

    @Test
    fun `StatusTransitioned distinguishes prior and target status -- both are independently preserved, never collapsed to one`() {
        val durableEntry = DurableMemoryCoreEntry.StatusTransitioned(
            reference = MemoryCoreRecordReference.ToAssertion(assertion.assertionId),
            priorStatus = MemoryCoreRecordStatus.DISPUTED,
            targetStatus = MemoryCoreRecordStatus.ARCHIVED,
            transitionedAt = Instant.parse("2026-01-01T00:00:06Z"),
        )

        assertNotEquals(durableEntry.priorStatus, durableEntry.targetStatus)
    }

    // ================= Schema version presence and evolution =================

    @Test
    fun `every case defaults its schemaVersion to the single CURRENT_SCHEMA_VERSION source of truth`() {
        assertEquals(1, DurableMemoryCoreEntry.CURRENT_SCHEMA_VERSION)

        val expectedVersion = DurableMemoryCoreEntry.CURRENT_SCHEMA_VERSION

        assertEquals(expectedVersion, DurableMemoryCoreEntry.ProvenanceCreated(provenance = provenance).schemaVersion)
        assertEquals(expectedVersion, DurableMemoryCoreEntry.EntityCreated(entity = entity).schemaVersion)
        assertEquals(expectedVersion, DurableMemoryCoreEntry.DocumentRegistered(document = document).schemaVersion)
        assertEquals(expectedVersion, DurableMemoryCoreEntry.AssertionCreated(assertion = assertion).schemaVersion)
        assertEquals(expectedVersion, DurableMemoryCoreEntry.RelationshipCreated(relationship = relationship).schemaVersion)
        assertEquals(
            expectedVersion,
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToEntity(entity.entityId),
                priorStatus = MemoryCoreRecordStatus.ACTIVE,
                targetStatus = MemoryCoreRecordStatus.DISPUTED,
                transitionedAt = Instant.parse("2026-01-01T00:00:05Z"),
            ).schemaVersion,
        )
    }

    @Test
    fun `schemaVersion can be set explicitly, distinct from the current default`() {
        val durableEntry = DurableMemoryCoreEntry.EntityCreated(schemaVersion = 2, entity = entity)

        assertEquals(2, durableEntry.schemaVersion)
        assertNotEquals(DurableMemoryCoreEntry.CURRENT_SCHEMA_VERSION, durableEntry.schemaVersion)
    }

    @Test
    fun `two otherwise-identical entries differing only in schemaVersion are not equal -- version genuinely participates in identity`() {
        val versionOne = DurableMemoryCoreEntry.EntityCreated(schemaVersion = 1, entity = entity)
        val versionTwo = DurableMemoryCoreEntry.EntityCreated(schemaVersion = 2, entity = entity)

        assertNotEquals(versionOne, versionTwo)
    }

    // ================= Equality =================

    @Test
    fun `entries wrapping equal records with equal schema versions are equal`() {
        assertEquals(
            DurableMemoryCoreEntry.EntityCreated(entity = entity),
            DurableMemoryCoreEntry.EntityCreated(entity = entity),
        )
        assertEquals(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = provenance),
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = provenance),
        )
    }

    @Test
    fun `entries of different cases are never equal, even if constructed from related data`() {
        assertNotEquals<DurableMemoryCoreEntry>(
            DurableMemoryCoreEntry.EntityCreated(entity = entity),
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToEntity(entity.entityId),
                priorStatus = MemoryCoreRecordStatus.ACTIVE,
                targetStatus = MemoryCoreRecordStatus.DISPUTED,
                transitionedAt = Instant.parse("2026-01-01T00:00:05Z"),
            ),
        )
    }

    @Test
    fun `copy() produces a distinct instance without mutating the original`() {
        val original = DurableMemoryCoreEntry.StatusTransitioned(
            reference = MemoryCoreRecordReference.ToEntity(entity.entityId),
            priorStatus = MemoryCoreRecordStatus.ACTIVE,
            targetStatus = MemoryCoreRecordStatus.DISPUTED,
            transitionedAt = Instant.parse("2026-01-01T00:00:05Z"),
        )
        val copy = original.copy(targetStatus = MemoryCoreRecordStatus.ARCHIVED)

        assertEquals(MemoryCoreRecordStatus.DISPUTED, original.targetStatus)
        assertEquals(MemoryCoreRecordStatus.ARCHIVED, copy.targetStatus)
        assertNotEquals(original, copy)
    }

    // ================= Immutability =================

    @Test
    fun `DurableMemoryCoreEntry and every one of its six cases exposes only immutable (val) properties`() {
        val typesToCheck = listOf(
            DurableMemoryCoreEntry.ProvenanceCreated::class,
            DurableMemoryCoreEntry.EntityCreated::class,
            DurableMemoryCoreEntry.DocumentRegistered::class,
            DurableMemoryCoreEntry.AssertionCreated::class,
            DurableMemoryCoreEntry.RelationshipCreated::class,
            DurableMemoryCoreEntry.StatusTransitioned::class,
        )

        typesToCheck.forEach { type ->
            val mutableProperties = type.memberProperties.filterIsInstance<KMutableProperty1<*, *>>()
            assertTrue(
                mutableProperties.isEmpty(),
                "${type.simpleName} must expose only immutable (val) properties, found: ${mutableProperties.map { it.name }}",
            )
        }
    }

    // ================= Internal, not public -- remains internal to the durability subsystem =================

    @Test
    fun `DurableMemoryCoreEntry and every one of its six cases is internal, never public API surface`() {
        val typesToCheck = listOf(
            DurableMemoryCoreEntry::class,
            DurableMemoryCoreEntry.ProvenanceCreated::class,
            DurableMemoryCoreEntry.EntityCreated::class,
            DurableMemoryCoreEntry.DocumentRegistered::class,
            DurableMemoryCoreEntry.AssertionCreated::class,
            DurableMemoryCoreEntry.RelationshipCreated::class,
            DurableMemoryCoreEntry.StatusTransitioned::class,
        )

        typesToCheck.forEach { type ->
            assertEquals(
                KVisibility.INTERNAL,
                type.visibility,
                "${type.simpleName} must be internal, never part of any public, caller-facing contract",
            )
        }
    }

    // ================= Absence of runtime, filesystem, or serialization dependencies =================

    @Test
    fun `no case's primary constructor accepts a filesystem, serialization, or storage-technology type`() {
        val forbiddenQualifiedNamePrefixes = listOf(
            "java.nio.file",
            "java.io",
            "java.sql",
            "javax.sql",
        )
        val forbiddenQualifiedNameSubstrings = listOf(
            "Json", "json", "Sqlite", "sqlite", "Serializer", "Serializable", "Parser", "Codec",
        )

        val typesToCheck = listOf(
            DurableMemoryCoreEntry.ProvenanceCreated::class,
            DurableMemoryCoreEntry.EntityCreated::class,
            DurableMemoryCoreEntry.DocumentRegistered::class,
            DurableMemoryCoreEntry.AssertionCreated::class,
            DurableMemoryCoreEntry.RelationshipCreated::class,
            DurableMemoryCoreEntry.StatusTransitioned::class,
        )

        typesToCheck.forEach { type ->
            val parameterTypeNames = type.primaryConstructor!!.parameters.mapNotNull {
                (it.type.classifier as? kotlin.reflect.KClass<*>)?.qualifiedName
            }

            parameterTypeNames.forEach { qualifiedName ->
                assertTrue(
                    forbiddenQualifiedNamePrefixes.none { qualifiedName.startsWith(it) },
                    "${type.simpleName} constructor parameter type '$qualifiedName' looks like a filesystem/storage dependency",
                )
                assertTrue(
                    forbiddenQualifiedNameSubstrings.none { qualifiedName.contains(it) },
                    "${type.simpleName} constructor parameter type '$qualifiedName' looks like a serialization dependency",
                )
            }
        }
    }

    @Test
    fun `no case declares a suspend function -- this Unit performs no input or output of any kind`() {
        val typesToCheck = listOf(
            DurableMemoryCoreEntry.ProvenanceCreated::class,
            DurableMemoryCoreEntry.EntityCreated::class,
            DurableMemoryCoreEntry.DocumentRegistered::class,
            DurableMemoryCoreEntry.AssertionCreated::class,
            DurableMemoryCoreEntry.RelationshipCreated::class,
            DurableMemoryCoreEntry.StatusTransitioned::class,
        )

        typesToCheck.forEach { type ->
            val suspendFunctions = type.members.filter { it.isSuspend }
            assertTrue(
                suspendFunctions.isEmpty(),
                "${type.simpleName} must declare no suspend function -- Unit 1 performs no persistence, no append log, no replay",
            )
        }
    }
}
