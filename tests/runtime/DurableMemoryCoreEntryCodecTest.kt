package parker.core.runtime

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.ResourceSensitivity

/**
 * Memory Core Durability, Implementation Unit 3 (Atomic Append). Pure,
 * file-I/O-free round-trip tests for [DurableMemoryCoreEntryCodec] --
 * `encode` then `decode` must reproduce an identical value for every one
 * of the six [DurableMemoryCoreEntry] cases, including adversarial
 * free-text content and every nullable/collection field in both its
 * present and absent form. Filesystem-level behaviour (real appends,
 * real reads, construction validation) is covered separately by
 * [FileSystemMemoryCoreDurabilityLogTest].
 */
class DurableMemoryCoreEntryCodecTest {

    private fun roundTrip(entry: DurableMemoryCoreEntry): DurableMemoryCoreEntry {
        val encoded = DurableMemoryCoreEntryCodec.encode(entry)
        return DurableMemoryCoreEntryCodec.decode(encoded, lineNumber = 1)
    }

    // ================= Provenance: every field, present and absent =================

    @Test
    fun `ProvenanceCreated round-trips with every optional field present`() {
        val entry = DurableMemoryCoreEntry.ProvenanceCreated(
            provenance = Provenance(
                provenanceId = ProvenanceId("provenance-1"),
                sourceIdentifier = "conversation-turn-1",
                sourceType = "conversation",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                ingestionTime = Instant.parse("2026-01-01T00:00:01Z"),
                contentNature = ContentNature.EXTRACTED,
                creator = "Jane Doe",
                creatorPrincipalId = PrincipalId("principal-1"),
                claimedCreationTime = Instant.parse("2025-12-31T00:00:00Z"),
                derivedFrom = listOf(ProvenanceId("provenance-0")),
                extractedFrom = DocumentId("document-1"),
                processingHistory = listOf("extracted", "summarised"),
                integrityInformation = "sha256:abc123",
                confidence = 0.75,
                sensitivity = ResourceSensitivity.FINANCIAL,
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    @Test
    fun `ProvenanceCreated round-trips with every optional field absent`() {
        val entry = DurableMemoryCoreEntry.ProvenanceCreated(
            provenance = Provenance(
                provenanceId = ProvenanceId("provenance-1"),
                sourceIdentifier = "conversation-turn-1",
                sourceType = "conversation",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                ingestionTime = Instant.parse("2026-01-01T00:00:01Z"),
                contentNature = ContentNature.UNKNOWN,
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    // ================= Entity =================

    @Test
    fun `EntityCreated round-trips with aliases, relatedPrincipalId, and metadata present`() {
        val entry = DurableMemoryCoreEntry.EntityCreated(
            entity = Entity(
                entityId = EntityId("entity-1"),
                entityType = "person",
                primaryLabel = "Jane Doe",
                provenanceId = ProvenanceId("provenance-1"),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                aliases = listOf("J. Doe", "Janey"),
                relatedPrincipalId = PrincipalId("principal-1"),
                status = MemoryCoreRecordStatus.DISPUTED,
                metadata = mapOf("origin" to "conversation", "confidence" to "high"),
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    @Test
    fun `EntityCreated round-trips with empty aliases and metadata, and absent relatedPrincipalId`() {
        val entry = DurableMemoryCoreEntry.EntityCreated(
            entity = Entity(
                entityId = EntityId("entity-1"),
                entityType = "person",
                primaryLabel = "Jane Doe",
                provenanceId = ProvenanceId("provenance-1"),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    // ================= Document =================

    @Test
    fun `DocumentRegistered round-trips with integrityHash present and metadata`() {
        val entry = DurableMemoryCoreEntry.DocumentRegistered(
            document = Document(
                documentId = DocumentId("document-1"),
                documentType = "email",
                locationReference = "mailbox://inbox/message-1",
                provenanceId = ProvenanceId("provenance-1"),
                registeredAt = Instant.parse("2026-01-01T00:00:00Z"),
                integrityHash = "sha256:def456",
                processingStatus = DocumentProcessingStatus.PROCESSED_EXTERNALLY,
                status = MemoryCoreRecordStatus.ARCHIVED,
                metadata = mapOf("folder" to "inbox"),
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    @Test
    fun `DocumentRegistered round-trips with integrityHash absent`() {
        val entry = DurableMemoryCoreEntry.DocumentRegistered(
            document = Document(
                documentId = DocumentId("document-1"),
                documentType = "email",
                locationReference = "mailbox://inbox/message-1",
                provenanceId = ProvenanceId("provenance-1"),
                registeredAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    // ================= Assertion =================

    @Test
    fun `AssertionCreated round-trips with confidence and metadata present`() {
        val entry = DurableMemoryCoreEntry.AssertionCreated(
            assertion = Assertion(
                assertionId = AssertionId("assertion-1"),
                statement = "the user prefers window seats",
                provenanceId = ProvenanceId("provenance-1"),
                confidence = 0.9,
                status = MemoryCoreRecordStatus.SUPERSEDED,
                metadata = mapOf("topic" to "travel"),
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    @Test
    fun `AssertionCreated round-trips with confidence absent`() {
        val entry = DurableMemoryCoreEntry.AssertionCreated(
            assertion = Assertion(
                assertionId = AssertionId("assertion-1"),
                statement = "the user prefers window seats",
                provenanceId = ProvenanceId("provenance-1"),
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    // ================= Relationship =================

    @Test
    fun `RelationshipCreated round-trips both endpoints, directional flag, and status`() {
        val entry = DurableMemoryCoreEntry.RelationshipCreated(
            relationship = Relationship(
                relationshipId = RelationshipId("relationship-1"),
                relationshipType = Relationship.SUPPORTS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1"),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.DOCUMENT, "document-1"),
                directional = true,
                provenanceId = ProvenanceId("provenance-1"),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                status = MemoryCoreRecordStatus.ACTIVE,
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    @Test
    fun `RelationshipCreated round-trips a non-directional relationship and an external endpoint kind`() {
        val entry = DurableMemoryCoreEntry.RelationshipCreated(
            relationship = Relationship(
                relationshipId = RelationshipId("relationship-2"),
                relationshipType = Relationship.SAME_AS,
                fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
                toEndpoint = RelationshipEndpoint(RelationshipEndpoint.KNOWLEDGE_RECORD, "knowledge-record-1"),
                directional = false,
                provenanceId = ProvenanceId("provenance-1"),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    // ================= StatusTransitioned =================

    @Test
    fun `StatusTransitioned round-trips for each of the four lifecycle-bearing record kinds`() {
        val transitionedAt = Instant.parse("2026-01-01T00:00:05Z")

        val entries = listOf(
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToEntity(EntityId("entity-1")),
                priorStatus = MemoryCoreRecordStatus.ACTIVE,
                targetStatus = MemoryCoreRecordStatus.DISPUTED,
                transitionedAt = transitionedAt,
            ),
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToDocument(DocumentId("document-1")),
                priorStatus = MemoryCoreRecordStatus.ACTIVE,
                targetStatus = MemoryCoreRecordStatus.ARCHIVED,
                transitionedAt = transitionedAt,
            ),
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToAssertion(AssertionId("assertion-1")),
                priorStatus = MemoryCoreRecordStatus.DISPUTED,
                targetStatus = MemoryCoreRecordStatus.SUPERSEDED,
                transitionedAt = transitionedAt,
            ),
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToRelationship(RelationshipId("relationship-1")),
                priorStatus = MemoryCoreRecordStatus.ARCHIVED,
                targetStatus = MemoryCoreRecordStatus.DELETED,
                transitionedAt = transitionedAt,
            ),
        )

        entries.forEach { entry -> assertEquals(entry, roundTrip(entry)) }
    }

    // ================= Adversarial free-text content =================

    @Test
    fun `free-text fields containing embedded tabs, newlines, and backslashes round-trip exactly`() {
        val entry = DurableMemoryCoreEntry.EntityCreated(
            entity = Entity(
                entityId = EntityId("entity-1"),
                entityType = "person",
                primaryLabel = "Jane\tDoe\nAKA \\Janey\\",
                provenanceId = ProvenanceId("provenance-1"),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                aliases = listOf("alias\twith\ttabs", "alias\nwith\nnewlines"),
                metadata = mapOf("key\twith\ttab" to "value\nwith\nnewline"),
            ),
        )

        val roundTripped = roundTrip(entry) as DurableMemoryCoreEntry.EntityCreated
        assertEquals(entry.entity.primaryLabel, roundTripped.entity.primaryLabel)
        assertEquals(entry.entity.aliases, roundTripped.entity.aliases)
        assertEquals(entry.entity.metadata, roundTripped.entity.metadata)
        assertEquals(entry, roundTripped)
    }

    @Test
    fun `free-text fields containing Unicode content round-trip exactly`() {
        val entry = DurableMemoryCoreEntry.AssertionCreated(
            assertion = Assertion(
                assertionId = AssertionId("assertion-1"),
                statement = "the user's name is 田中 and prefers café seating — emoji: 😀",
                provenanceId = ProvenanceId("provenance-1"),
            ),
        )

        assertEquals(entry, roundTrip(entry))
    }

    @Test
    fun `a metadata value that is an empty string round-trips as an empty string, distinct from an absent key`() {
        // Provenance.creator and every other single nullable free-text field this Programme's record
        // types carry reject a blank-but-present value at construction (forcing null instead) --
        // Entity.metadata carries no such restriction, so it is the one field genuinely capable of
        // holding an empty-string value distinct from the key being absent altogether.
        val entry = DurableMemoryCoreEntry.EntityCreated(
            entity = Entity(
                entityId = EntityId("entity-1"),
                entityType = "person",
                primaryLabel = "Jane Doe",
                provenanceId = ProvenanceId("provenance-1"),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                metadata = mapOf("note" to ""),
            ),
        )

        val roundTripped = roundTrip(entry) as DurableMemoryCoreEntry.EntityCreated
        assertEquals(mapOf("note" to ""), roundTripped.entity.metadata)
        assertEquals(entry, roundTripped)
    }

    // ================= Schema version =================

    @Test
    fun `decode rejects an entry carrying a schema version this code does not recognise`() {
        val entry = DurableMemoryCoreEntry.EntityCreated(
            entity = Entity(
                entityId = EntityId("entity-1"),
                entityType = "person",
                primaryLabel = "Jane Doe",
                provenanceId = ProvenanceId("provenance-1"),
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        val encodedWithFutureVersion = DurableMemoryCoreEntryCodec.encode(entry)
            .replace("schemaVersion=1", "schemaVersion=99")

        assertFailsWith<MemoryCoreDurabilityLogException.UnrecognizedSchemaVersion> {
            DurableMemoryCoreEntryCodec.decode(encodedWithFutureVersion, lineNumber = 1)
        }
    }

    // ================= Malformed input =================

    @Test
    fun `decode rejects a line missing a required field`() {
        assertFailsWith<MemoryCoreDurabilityLogException.MalformedEntry> {
            DurableMemoryCoreEntryCodec.decode("kind=EntityCreated\tschemaVersion=1", lineNumber = 7)
        }
    }

    @Test
    fun `decode rejects a line with an unrecognised entry kind`() {
        assertFailsWith<MemoryCoreDurabilityLogException.MalformedEntry> {
            DurableMemoryCoreEntryCodec.decode("kind=NotARealKind\tschemaVersion=1", lineNumber = 3)
        }
    }

    @Test
    fun `decode rejects a field not in key=value form`() {
        assertFailsWith<MemoryCoreDurabilityLogException.MalformedEntry> {
            DurableMemoryCoreEntryCodec.decode("this-is-not-a-key-value-pair", lineNumber = 2)
        }
    }

    @Test
    fun `decode rejects invalid Base64 in an otherwise well-formed field`() {
        assertFailsWith<MemoryCoreDurabilityLogException.MalformedEntry> {
            DurableMemoryCoreEntryCodec.decode(
                "kind=AssertionCreated\tschemaVersion=1\tassertionId=not-valid-base64!!!\tstatement=YQ==\tprovenanceId=YQ==\tstatus=ACTIVE",
                lineNumber = 5,
            )
        }
    }

    @Test
    fun `MalformedEntry and UnrecognizedSchemaVersion both report the offending line number or kind`() {
        val malformed = assertFailsWith<MemoryCoreDurabilityLogException.MalformedEntry> {
            DurableMemoryCoreEntryCodec.decode("garbage", lineNumber = 42)
        }
        assertEquals(42, malformed.lineNumber)

        val unrecognizedVersion = assertFailsWith<MemoryCoreDurabilityLogException.UnrecognizedSchemaVersion> {
            DurableMemoryCoreEntryCodec.decode("kind=EntityCreated\tschemaVersion=99", lineNumber = 1)
        }
        assertEquals("EntityCreated", unrecognizedVersion.kind)
        assertEquals(99, unrecognizedVersion.schemaVersion)
    }
}
