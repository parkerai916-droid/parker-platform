package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.jvmErasure
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.ContentNature
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentProcessingStatus
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Memory Core Durability, Implementation Unit 4 (Replay and Startup
 * Recovery). Behavioural tests for [MemoryCoreRecovery]. Most tests
 * exercise [FakeMemoryCoreDurabilityLog] directly, giving precise
 * control over the exact decoded entry sequence recovery replays --
 * schema-version and malformed-line tests, which depend on genuine
 * text-decoding behaviour, use a real [FileSystemMemoryCoreDurabilityLog]
 * over a `@TempDir` instead.
 */
class MemoryCoreRecoveryTest {

    private val principal = PrincipalId("test-principal")

    private fun provenance(
        id: String,
        sourceIdentifier: String = "source-$id",
        acquisitionTime: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ) = Provenance(
        provenanceId = ProvenanceId(id),
        sourceIdentifier = sourceIdentifier,
        sourceType = "conversation",
        acquisitionTime = acquisitionTime,
        ingestionTime = acquisitionTime.plusSeconds(1),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun entity(
        id: String,
        provenanceId: ProvenanceId,
        status: MemoryCoreRecordStatus = MemoryCoreRecordStatus.ACTIVE,
        createdAt: Instant = Instant.parse("2026-01-01T00:00:02Z"),
    ) = Entity(
        entityId = EntityId(id),
        entityType = "person",
        primaryLabel = "Person $id",
        provenanceId = provenanceId,
        createdAt = createdAt,
        status = status,
    )

    private fun document(id: String, provenanceId: ProvenanceId) = Document(
        documentId = DocumentId(id),
        documentType = "email",
        locationReference = "mailbox://inbox/$id",
        provenanceId = provenanceId,
        registeredAt = Instant.parse("2026-01-01T00:00:03Z"),
        processingStatus = DocumentProcessingStatus.REGISTERED,
    )

    private fun assertion(id: String, provenanceId: ProvenanceId) = Assertion(
        assertionId = AssertionId(id),
        statement = "statement $id",
        provenanceId = provenanceId,
    )

    private fun relationship(
        id: String,
        provenanceId: ProvenanceId,
        fromEndpoint: RelationshipEndpoint,
        toEndpoint: RelationshipEndpoint,
    ) = Relationship(
        relationshipId = RelationshipId(id),
        relationshipType = Relationship.SUPPORTS,
        fromEndpoint = fromEndpoint,
        toEndpoint = toEndpoint,
        directional = true,
        provenanceId = provenanceId,
        createdAt = Instant.parse("2026-01-01T00:00:04Z"),
    )

    private suspend fun fakeLog(vararg entries: DurableMemoryCoreEntry): FakeMemoryCoreDurabilityLog {
        val log = FakeMemoryCoreDurabilityLog()
        entries.forEach { log.append(it) }
        return log
    }

    // ================= Empty log =================

    @Test
    fun `recovering an empty durability log produces a genuinely empty, successfully recovered InMemoryMemoryCore`() = runTest {
        val recovered = MemoryCoreRecovery.recover(FakeMemoryCoreDurabilityLog())

        assertEquals(emptyList(), recovered.findEntities(EntityLookupQuery(principal, maximumResults = 10)))
    }

    // ================= Restoration of each of the five creation record kinds =================

    @Test
    fun `recovery restores a Provenance, confirmed via a dependent Entity's own successful referential check`() = runTest {
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = ent),
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(ent, recovered.getEntity(principal, ent.entityId))
    }

    @Test
    fun `recovery restores a Document with its original identifier, registeredAt, and provenance reference intact`() = runTest {
        val prov = provenance("provenance-1")
        val doc = document("document-1", prov.provenanceId)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.DocumentRegistered(document = doc),
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(doc, recovered.getDocument(principal, doc.documentId))
    }

    @Test
    fun `recovery restores an Assertion with its original identifier and provenance reference intact`() = runTest {
        val prov = provenance("provenance-1")
        val assert = assertion("assertion-1", prov.provenanceId)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.AssertionCreated(assertion = assert),
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(assert, recovered.getAssertion(principal, assert.assertionId))
    }

    @Test
    fun `recovery restores a Relationship with both endpoints, directional flag, and provenance reference intact`() = runTest {
        val prov = provenance("provenance-1")
        val fromEntity = entity("entity-1", prov.provenanceId)
        val toEntity = entity("entity-2", prov.provenanceId)
        val rel = relationship(
            "relationship-1",
            prov.provenanceId,
            RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
            RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-2"),
        )
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = fromEntity),
            DurableMemoryCoreEntry.EntityCreated(entity = toEntity),
            DurableMemoryCoreEntry.RelationshipCreated(relationship = rel),
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(rel, recovered.getRelationship(principal, rel.relationshipId))
    }

    // ================= Identity, timestamp, and provenance preservation =================

    @Test
    fun `recovery never mints a replacement identifier for any restored record`() = runTest {
        val prov = provenance("provenance-77")
        val ent = entity("entity-99", prov.provenanceId)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = ent),
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(EntityId("entity-99"), recovered.getEntity(principal, EntityId("entity-99"))?.entityId)
    }

    @Test
    fun `recovery preserves the original creation timestamp exactly, never substituting the current clock`() = runTest {
        val originalTimestamp = Instant.parse("2020-06-15T08:30:00Z")
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId, createdAt = originalTimestamp)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = ent),
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(originalTimestamp, recovered.getEntity(principal, ent.entityId)?.createdAt)
    }

    // ================= Lifecycle transition replay =================

    @Test
    fun `a single StatusTransitioned entry is replayed, changing the target record's status`() = runTest {
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = ent),
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToEntity(ent.entityId),
                priorStatus = MemoryCoreRecordStatus.ACTIVE,
                targetStatus = MemoryCoreRecordStatus.DISPUTED,
                transitionedAt = Instant.parse("2026-01-01T00:00:05Z"),
            ),
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(MemoryCoreRecordStatus.DISPUTED, recovered.getEntity(principal, ent.entityId)?.status)
    }

    @Test
    fun `multiple transitions on one record are replayed in order, reaching the correct final status -- not collapsed to one step`() = runTest {
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = ent),
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToEntity(ent.entityId),
                priorStatus = MemoryCoreRecordStatus.ACTIVE,
                targetStatus = MemoryCoreRecordStatus.DISPUTED,
                transitionedAt = Instant.parse("2026-01-01T00:00:05Z"),
            ),
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToEntity(ent.entityId),
                priorStatus = MemoryCoreRecordStatus.DISPUTED,
                targetStatus = MemoryCoreRecordStatus.ARCHIVED,
                transitionedAt = Instant.parse("2026-01-01T00:00:06Z"),
            ),
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(MemoryCoreRecordStatus.ARCHIVED, recovered.getEntity(principal, ent.entityId)?.status)
    }

    // ================= Dependency-aware ordering =================

    @Test
    fun `provenance, entities, and a relationship connecting them recover correctly when durable order already satisfies dependency order`() = runTest {
        val prov = provenance("provenance-1")
        val fromEntity = entity("entity-1", prov.provenanceId)
        val toEntity = entity("entity-2", prov.provenanceId)
        val rel = relationship(
            "relationship-1",
            prov.provenanceId,
            RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
            RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-2"),
        )
        // Durable order is exactly original creation order -- provenance, then both entities, then the
        // relationship that depends on all three -- mirroring what InMemoryMemoryCore's own creation-time
        // checks would already have required at original write time.
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = fromEntity),
            DurableMemoryCoreEntry.EntityCreated(entity = toEntity),
            DurableMemoryCoreEntry.RelationshipCreated(relationship = rel),
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(rel, recovered.getRelationship(principal, rel.relationshipId))
        assertEquals(fromEntity, recovered.getEntity(principal, fromEntity.entityId))
        assertEquals(toEntity, recovered.getEntity(principal, toEntity.entityId))
    }

    // ================= Broken references =================

    @Test
    fun `a broken provenance reference on a creation entry fails recovery -- never silently skipped`() = runTest {
        val ent = entity("entity-1", ProvenanceId("provenance-does-not-exist"))
        val log = fakeLog(DurableMemoryCoreEntry.EntityCreated(entity = ent))

        assertFailsWith<MemoryCoreRecoveryException.RestorationFailed> { MemoryCoreRecovery.recover(log) }
    }

    @Test
    fun `a broken relationship endpoint reference fails recovery -- never silently skipped`() = runTest {
        val prov = provenance("provenance-1")
        val rel = relationship(
            "relationship-1",
            prov.provenanceId,
            RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-does-not-exist"),
            RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-also-does-not-exist"),
        )
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.RelationshipCreated(relationship = rel),
        )

        assertFailsWith<MemoryCoreRecoveryException.RestorationFailed> { MemoryCoreRecovery.recover(log) }
    }

    // ================= StatusTransitioned failure modes =================

    @Test
    fun `a StatusTransitioned entry naming a target that was never created fails recovery as a missing transition target`() = runTest {
        val log = fakeLog(
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToEntity(EntityId("entity-never-created")),
                priorStatus = MemoryCoreRecordStatus.ACTIVE,
                targetStatus = MemoryCoreRecordStatus.DISPUTED,
                transitionedAt = Instant.parse("2026-01-01T00:00:05Z"),
            ),
        )

        assertFailsWith<MemoryCoreRecoveryException.MissingTransitionTarget> { MemoryCoreRecovery.recover(log) }
    }

    @Test
    fun `a StatusTransitioned entry whose claimed priorStatus does not match reality fails recovery as a prior-status mismatch`() = runTest {
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId, status = MemoryCoreRecordStatus.ACTIVE)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = ent),
            // Entity is actually ACTIVE, but this entry claims it was ARCHIVED beforehand.
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToEntity(ent.entityId),
                priorStatus = MemoryCoreRecordStatus.ARCHIVED,
                targetStatus = MemoryCoreRecordStatus.DELETED,
                transitionedAt = Instant.parse("2026-01-01T00:00:05Z"),
            ),
        )

        assertFailsWith<MemoryCoreRecoveryException.PriorStatusMismatch> { MemoryCoreRecovery.recover(log) }
    }

    @Test
    fun `a StatusTransitioned entry naming an impossible transition fails recovery, even though priorStatus matches reality`() = runTest {
        val prov = provenance("provenance-1")
        // DELETED is terminal -- no transition out of it exists in the closed table.
        val ent = entity("entity-1", prov.provenanceId, status = MemoryCoreRecordStatus.DELETED)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = ent),
            DurableMemoryCoreEntry.StatusTransitioned(
                reference = MemoryCoreRecordReference.ToEntity(ent.entityId),
                priorStatus = MemoryCoreRecordStatus.DELETED,
                targetStatus = MemoryCoreRecordStatus.ACTIVE,
                transitionedAt = Instant.parse("2026-01-01T00:00:05Z"),
            ),
        )

        assertFailsWith<MemoryCoreRecoveryException.ImpossibleTransition> { MemoryCoreRecovery.recover(log) }
    }

    // ================= Conflicting duplicate identity =================

    @Test
    fun `two creation entries sharing an identifier but carrying different content fail recovery as a conflicting duplicate`() = runTest {
        val prov = provenance("provenance-1")
        val original = entity("entity-1", prov.provenanceId)
        val conflicting = original.copy(primaryLabel = "A Completely Different Label")
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = original),
            DurableMemoryCoreEntry.EntityCreated(entity = conflicting),
        )

        assertFailsWith<MemoryCoreRecoveryException.ConflictingDuplicateIdentity> { MemoryCoreRecovery.recover(log) }
    }

    // ================= Repeated identical record treatment (idempotence) =================

    @Test
    fun `a creation entry durably repeated with byte-identical content is idempotently skipped, not treated as a conflict`() = runTest {
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId)
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = ent),
            DurableMemoryCoreEntry.EntityCreated(entity = ent), // exact duplicate
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(ent, recovered.getEntity(principal, ent.entityId))
    }

    @Test
    fun `a StatusTransitioned entry durably repeated is idempotently skipped, not treated as an impossible self-transition`() = runTest {
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId)
        val transition = DurableMemoryCoreEntry.StatusTransitioned(
            reference = MemoryCoreRecordReference.ToEntity(ent.entityId),
            priorStatus = MemoryCoreRecordStatus.ACTIVE,
            targetStatus = MemoryCoreRecordStatus.DISPUTED,
            transitionedAt = Instant.parse("2026-01-01T00:00:05Z"),
        )
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = ent),
            transition,
            transition, // exact duplicate -- would throw if blindly replayed via transitionStatus twice
        )

        val recovered = MemoryCoreRecovery.recover(log)

        assertEquals(MemoryCoreRecordStatus.DISPUTED, recovered.getEntity(principal, ent.entityId)?.status)
    }

    // ================= Recovery failure exposes no partially writable store =================

    @Test
    fun `recover throws before returning anything on a failing sequence -- no partial InMemoryMemoryCore is ever produced`() = runTest {
        val prov = provenance("provenance-1")
        val goodEntity = entity("entity-1", prov.provenanceId)
        val badEntity = entity("entity-2", ProvenanceId("provenance-does-not-exist"))
        val log = fakeLog(
            DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov),
            DurableMemoryCoreEntry.EntityCreated(entity = goodEntity),
            DurableMemoryCoreEntry.EntityCreated(entity = badEntity),
        )

        // recover() is a single suspend function call with exactly one InMemoryMemoryCore-typed return
        // path (success) and one exception-throwing path (failure) -- there is no third path by which a
        // caller could obtain a reference to the partially-populated instance recover() was building
        // internally when the failure occurred, confirmed here by recover()'s own call site never
        // binding a result when it throws.
        assertFailsWith<MemoryCoreRecoveryException.RestorationFailed> { MemoryCoreRecovery.recover(log) }
    }

    // ================= Schema version and malformed-line handling (real filesystem log) =================

    @Test
    fun `an unsupported schema version anywhere in a real durability log fails recovery, never silently interpreted`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("durability.log")
        val fsLog = FileSystemMemoryCoreDurabilityLog(logFile)
        fsLog.append(DurableMemoryCoreEntry.ProvenanceCreated(provenance = provenance("provenance-1")))
        // Directly corrupt the schema version of the second entry on disk.
        fsLog.append(DurableMemoryCoreEntry.EntityCreated(entity = entity("entity-1", ProvenanceId("provenance-1"))))
        val corrupted = Files.readAllLines(logFile).map { it.replace("schemaVersion=1", "schemaVersion=99") }
        Files.write(logFile, corrupted)

        val freshLog = FileSystemMemoryCoreDurabilityLog(logFile)
        assertFailsWith<MemoryCoreRecoveryException.DurabilityLogUnreadable> { MemoryCoreRecovery.recover(freshLog) }
    }

    @Test
    fun `a malformed non-terminal line in a real durability log fails recovery`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("durability.log")
        Files.write(
            logFile.let { Files.createFile(it) },
            listOf(
                DurableMemoryCoreEntryCodec.encode(DurableMemoryCoreEntry.ProvenanceCreated(provenance = provenance("provenance-1"))),
                "this-line-is-not-a-valid-entry",
                DurableMemoryCoreEntryCodec.encode(DurableMemoryCoreEntry.EntityCreated(entity = entity("entity-1", ProvenanceId("provenance-1")))),
            ),
        )
        val fsLog = FileSystemMemoryCoreDurabilityLog(logFile)

        assertFailsWith<MemoryCoreRecoveryException.DurabilityLogUnreadable> { MemoryCoreRecovery.recover(fsLog) }
    }

    @Test
    fun `an incomplete or malformed final line fails recovery just like any other malformed line -- no leniency is implemented for it`(
        @TempDir tempDir: Path,
    ) = runTest {
        val logFile = tempDir.resolve("durability.log")
        val fsLog = FileSystemMemoryCoreDurabilityLog(logFile)
        fsLog.append(DurableMemoryCoreEntry.ProvenanceCreated(provenance = provenance("provenance-1")))

        // Simulate a genuinely interrupted trailing write: a truncated, incomplete final line appended
        // directly to the file, exactly as a crash mid-append might leave behind.
        Files.write(
            logFile,
            "kind=EntityCreated\tschemaVersion=1\tentityId=partial-and-tr".toByteArray(),
            StandardOpenOption.APPEND,
        )

        val freshLog = FileSystemMemoryCoreDurabilityLog(logFile)

        // This Unit's own governing task explicitly forbids inventing a heuristic to distinguish this
        // case from genuine corruption -- recovery must fail here, exactly as it would for corruption
        // anywhere else in the file, per this Unit's own disclosed, deliberate design choice.
        assertFailsWith<MemoryCoreRecoveryException.DurabilityLogUnreadable> { MemoryCoreRecovery.recover(freshLog) }
    }

    // ================= No public MemoryCore/MemoryRetrieval widening; no prohibited dependency leakage =================

    @Test
    fun `MemoryCoreRecovery and MemoryCoreRecoveryException are internal, never public API surface`() {
        assertEquals(KVisibility.INTERNAL, MemoryCoreRecovery::class.visibility)
        assertEquals(KVisibility.INTERNAL, MemoryCoreRecoveryException::class.visibility)
    }

    @Test
    fun `recover's own public signature references no PermissionEngine, Knowledge Memory, ParkerRuntime, Docker, or filesystem-path type`() {
        val forbiddenSubstrings = listOf(
            "PermissionEngine", "Knowledge", "EvidenceCustodian", "EvidenceIntelligence", "EventBus", "ParkerRuntime", "Docker",
        )
        val forbiddenPrefixes = listOf("java.nio.file", "java.io", "java.sql")

        val recoverFunction = MemoryCoreRecovery::class.declaredFunctions.single { it.name == "recover" }
        val types = recoverFunction.parameters.drop(1).map { it.type.jvmErasure } + recoverFunction.returnType.jvmErasure

        types.forEach { type ->
            val qualifiedName = type.qualifiedName ?: return@forEach
            assertTrue(forbiddenSubstrings.none { qualifiedName.contains(it) }, "found forbidden dependency '$qualifiedName' on MemoryCoreRecovery.recover")
            assertTrue(forbiddenPrefixes.none { qualifiedName.startsWith(it) }, "found forbidden filesystem type '$qualifiedName' on MemoryCoreRecovery.recover")
        }
    }
}
