package parker.core.runtime

import java.nio.file.Path
import java.time.Instant
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.AssertionId
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DocumentId
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Memory Core Durability, Implementation Unit 6 (Durable Memory Core
 * Decorator). Behavioural tests for [DurableMemoryCore]. Most tests
 * exercise [FakeMemoryCoreDurabilityLog] for precise control and speed;
 * one full end-to-end test uses a real [FileSystemMemoryCoreDurabilityLog]
 * over `@TempDir` to prove the whole stack -- construction, write,
 * restart, read -- genuinely works together.
 */
class DurableMemoryCoreTest {

    private val principal = PrincipalId("test-principal")

    private fun provenance(id: String) = Provenance(
        provenanceId = ProvenanceId(id),
        sourceIdentifier = "conversation-turn-1",
        sourceType = "conversation",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        ingestionTime = Instant.parse("2026-01-01T00:00:01Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun entity(id: String, provenanceId: ProvenanceId) = Entity(
        entityId = EntityId(id),
        entityType = "person",
        primaryLabel = "Person $id",
        provenanceId = provenanceId,
        createdAt = Instant.parse("2026-01-01T00:00:02Z"),
    )

    private val candidateProvenance = CandidateProvenance(
        sourceIdentifier = "conversation-turn-9",
        sourceType = "conversation",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun candidateEntity(provenanceId: ProvenanceId) = CandidateEntity(
        entityType = "person",
        primaryLabel = "New Person",
        provenanceId = provenanceId,
    )

    // Memory Core Durability, Implementation Unit 10 (Verification): candidate helpers for
    // Document, Assertion, and Relationship, mirroring InMemoryMemoryCoreTest's own identically-shaped
    // helpers -- needed here so this file's own restart-cycle tests can cover all five record kinds,
    // not only Provenance and Entity.
    private fun candidateDocument(provenanceId: ProvenanceId) = CandidateDocument(
        documentType = "email",
        locationReference = "mailbox://inbox/message-1",
        provenanceId = provenanceId,
    )

    private fun candidateAssertion(provenanceId: ProvenanceId) = CandidateAssertion(
        statement = "the user prefers window seats",
        provenanceId = provenanceId,
    )

    private fun candidateRelationship(provenanceId: ProvenanceId, fromEndpoint: RelationshipEndpoint, toEndpoint: RelationshipEndpoint) =
        CandidateRelationship(
            relationshipType = Relationship.SUPPORTS,
            fromEndpoint = fromEndpoint,
            toEndpoint = toEndpoint,
            directional = true,
            provenanceId = provenanceId,
        )

    // ================= Construction: successful recovery =================

    @Test
    fun `create over an empty log recovers a genuinely empty, immediately usable instance`() = runTest {
        val durable = DurableMemoryCore.create(FakeMemoryCoreDurabilityLog())

        assertEquals(emptyList(), durable.findEntities(EntityLookupQuery(principal, maximumResults = 10)))
    }

    @Test
    fun `create over a non-empty log recovers every previously-appended record, readable immediately`() = runTest {
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId)
        val log = FakeMemoryCoreDurabilityLog()
        log.append(DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov))
        log.append(DurableMemoryCoreEntry.EntityCreated(entity = ent))

        val durable = DurableMemoryCore.create(log)

        assertEquals(ent, durable.getEntity(principal, ent.entityId))
    }

    // ================= Construction: recovery failure =================

    @Test
    fun `create propagates a recovery failure, and no instance is ever produced`() = runTest {
        val badEntity = entity("entity-1", ProvenanceId("provenance-does-not-exist"))
        val log = FakeMemoryCoreDurabilityLog()
        log.append(DurableMemoryCoreEntry.EntityCreated(entity = badEntity))

        assertFailsWith<MemoryCoreRecoveryException.RestorationFailed> { DurableMemoryCore.create(log) }
    }

    // ================= Successful writes: prepare, append, commit =================

    @Test
    fun `createProvenance durably appends before the record becomes visible in memory`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)

        val created = durable.createProvenance(principal, candidateProvenance)

        assertEquals(1, log.appended.size)
        val appendedEntry = log.appended.single() as DurableMemoryCoreEntry.ProvenanceCreated
        assertEquals(created, appendedEntry.provenance)
    }

    @Test
    fun `createEntity durably appends, then the entity is readable via getEntity`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)

        val created = durable.createEntity(principal, candidateEntity(prov.provenanceId))

        assertEquals(created, durable.getEntity(principal, created.entityId))
        assertTrue(log.appended.any { it is DurableMemoryCoreEntry.EntityCreated && it.entity == created })
    }

    @Test
    fun `createEntity with a broken provenance reference throws and appends nothing -- decorator regression against InMemoryMemoryCore's own behaviour`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)

        assertFailsWith<IllegalArgumentException> {
            durable.createEntity(principal, candidateEntity(ProvenanceId("provenance-does-not-exist")))
        }
        assertEquals(emptyList(), log.appended, "a referential-integrity failure must never reach the durability log")
    }

    // ================= Append failure prevents mutation =================

    @Test
    fun `when append fails, the record never becomes visible in memory -- no mutation occurs`() = runTest {
        val log = FakeMemoryCoreDurabilityLog(
            appendBehavior = { throw MemoryCoreDurabilityLogException.StorageIOFailure("simulated failure", RuntimeException()) },
        )
        val durable = DurableMemoryCore.create(log)

        assertFailsWith<MemoryCoreDurabilityLogException.StorageIOFailure> {
            durable.createProvenance(principal, candidateProvenance)
        }

        // No provenance can be observed to have been created -- confirmed indirectly, since
        // MemoryRetrieval exposes no direct getProvenance: a dependent createEntity naming any
        // identifier this failed call might have minted must still fail with "does not exist",
        // proving nothing was stored under it.
        assertFailsWith<IllegalArgumentException> {
            durable.createEntity(principal, candidateEntity(ProvenanceId("provenance-1")))
        }
    }

    @Test
    fun `append failure during createEntity leaves the entity store completely unaffected`() = runTest {
        var shouldFail = false
        val log = FakeMemoryCoreDurabilityLog(
            appendBehavior = { entry ->
                if (shouldFail && entry is DurableMemoryCoreEntry.EntityCreated) {
                    throw MemoryCoreDurabilityLogException.StorageIOFailure("simulated failure", RuntimeException())
                }
            },
        )
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)
        shouldFail = true

        assertFailsWith<MemoryCoreDurabilityLogException.StorageIOFailure> {
            durable.createEntity(principal, candidateEntity(prov.provenanceId))
        }

        assertEquals(emptyList(), durable.findEntities(EntityLookupQuery(principal, maximumResults = 10)))
    }

    @Test
    fun `an identifier already minted by a failed create is never reused by a later, successful one -- it becomes a permanent gap`() = runTest {
        var shouldFail = true
        val log = FakeMemoryCoreDurabilityLog(
            appendBehavior = { entry ->
                if (shouldFail && entry is DurableMemoryCoreEntry.EntityCreated) {
                    throw MemoryCoreDurabilityLogException.StorageIOFailure("simulated failure", RuntimeException())
                }
            },
        )
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)

        // The first attempt mints "entity-1" internally (InMemoryMemoryCore.prepareEntity), then
        // fails to durably append it -- "entity-1" is now a permanent gap, never stored anywhere.
        assertFailsWith<MemoryCoreDurabilityLogException.StorageIOFailure> {
            durable.createEntity(principal, candidateEntity(prov.provenanceId))
        }

        shouldFail = false
        val created = durable.createEntity(principal, candidateEntity(prov.provenanceId))

        assertEquals(EntityId("entity-2"), created.entityId, "the burned 'entity-1' must never be reissued")
    }

    // ================= Read delegation =================

    @Test
    fun `read operations never touch the durability log`() = runTest {
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId)
        val log = FakeMemoryCoreDurabilityLog()
        log.append(DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov))
        log.append(DurableMemoryCoreEntry.EntityCreated(entity = ent))
        val durable = DurableMemoryCore.create(log)
        val appendedCountAfterRecovery = log.appended.size

        durable.getEntity(principal, ent.entityId)
        durable.findEntities(EntityLookupQuery(principal, maximumResults = 10))
        durable.getDocument(principal, DocumentId("document-does-not-exist"))

        assertEquals(appendedCountAfterRecovery, log.appended.size, "no read operation may ever append to the durability log")
    }

    @Test
    fun `getEntity delegates directly to the recovered InMemoryMemoryCore, returning exactly what it holds`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)
        val created = durable.createEntity(principal, candidateEntity(prov.provenanceId))

        assertEquals(created, durable.getEntity(principal, created.entityId))
        assertNull(durable.getEntity(principal, EntityId("entity-never-created")))
    }

    // ================= Lifecycle persistence =================

    @Test
    fun `transitionStatus appends StatusTransitioned before applying the transition, then the new status is visible`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)
        val created = durable.createEntity(principal, candidateEntity(prov.provenanceId))

        durable.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(created.entityId), MemoryCoreRecordStatus.DISPUTED)

        assertEquals(MemoryCoreRecordStatus.DISPUTED, durable.getEntity(principal, created.entityId)?.status)
        val transitionEntry = log.appended.filterIsInstance<DurableMemoryCoreEntry.StatusTransitioned>().single()
        assertEquals(MemoryCoreRecordStatus.ACTIVE, transitionEntry.priorStatus)
        assertEquals(MemoryCoreRecordStatus.DISPUTED, transitionEntry.targetStatus)
    }

    @Test
    fun `an impossible transition throws and appends nothing -- no duplicate lifecycle logic silently accepts it`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)
        val created = durable.createEntity(principal, candidateEntity(prov.provenanceId))
        durable.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(created.entityId), MemoryCoreRecordStatus.DELETED)
        val appendedCountBeforeAttempt = log.appended.size

        // DELETED is terminal -- no transition out of it exists in the closed table.
        assertFailsWith<IllegalArgumentException> {
            durable.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(created.entityId), MemoryCoreRecordStatus.ACTIVE)
        }

        assertEquals(appendedCountBeforeAttempt, log.appended.size, "an invalid transition must never reach the durability log")
    }

    @Test
    fun `transitioning a nonexistent record throws and appends nothing`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)

        assertFailsWith<NoSuchElementException> {
            durable.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(EntityId("entity-never-created")), MemoryCoreRecordStatus.DISPUTED)
        }
        assertEquals(emptyList(), log.appended)
    }

    // ================= Deterministic behaviour =================

    @Test
    fun `two instances recovered from identical log content produce identical read results`() = runTest {
        val prov = provenance("provenance-1")
        val ent = entity("entity-1", prov.provenanceId)
        val firstLog = FakeMemoryCoreDurabilityLog()
        firstLog.append(DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov))
        firstLog.append(DurableMemoryCoreEntry.EntityCreated(entity = ent))
        val secondLog = FakeMemoryCoreDurabilityLog()
        secondLog.append(DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov))
        secondLog.append(DurableMemoryCoreEntry.EntityCreated(entity = ent))

        val first = DurableMemoryCore.create(firstLog)
        val second = DurableMemoryCore.create(secondLog)

        assertEquals(first.getEntity(principal, ent.entityId), second.getEntity(principal, ent.entityId))
        assertEquals(
            first.findEntities(EntityLookupQuery(principal, maximumResults = 10)),
            second.findEntities(EntityLookupQuery(principal, maximumResults = 10)),
        )
    }

    // ================= Identifier continuity after recovery =================

    @Test
    fun `after recovery, a newly created identifier never collides with a restored one`() = runTest {
        val prov = provenance("provenance-1")
        val restoredEntity = entity("entity-4", prov.provenanceId)
        val log = FakeMemoryCoreDurabilityLog()
        log.append(DurableMemoryCoreEntry.ProvenanceCreated(provenance = prov))
        log.append(DurableMemoryCoreEntry.EntityCreated(entity = restoredEntity))

        val durable = DurableMemoryCore.create(log)
        val newEntity = durable.createEntity(principal, candidateEntity(prov.provenanceId))

        assertEquals(EntityId("entity-5"), newEntity.entityId)
    }

    // ================= Decorator transparency =================

    @Test
    fun `DurableMemoryCore is usable through MemoryCore and MemoryRetrieval exactly like InMemoryMemoryCore, with no observable difference in shape`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable: MemoryCore = DurableMemoryCore.create(log)
        val retrieval: MemoryRetrieval = durable as MemoryRetrieval

        val provenance = durable.createProvenance(principal, candidateProvenance)
        val entity = durable.createEntity(principal, candidateEntity(provenance.provenanceId))

        assertEquals(entity, retrieval.getEntity(principal, entity.entityId))
    }

    @Test
    fun `relationship creation through the decorator enforces the same referential-integrity rules as InMemoryMemoryCore directly`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)

        assertFailsWith<IllegalArgumentException> {
            durable.createRelationship(
                principal,
                CandidateRelationship(
                    relationshipType = "supports",
                    fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-does-not-exist"),
                    toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-also-does-not-exist"),
                    directional = true,
                    provenanceId = prov.provenanceId,
                ),
            )
        }
    }

    // ================= Full end-to-end: real filesystem, construction, write, restart, read =================

    @Test
    fun `a real filesystem-backed decorator survives a full construct-write-restart-read cycle`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("durability.log")

        val first = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))
        val prov = first.createProvenance(principal, candidateProvenance)
        val created = first.createEntity(principal, candidateEntity(prov.provenanceId))

        // A fresh instance, as a new process restarting would construct, over the same log file.
        val second = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))

        assertEquals(created, second.getEntity(principal, created.entityId))
        val nextEntity = second.createEntity(principal, candidateEntity(prov.provenanceId))
        assertEquals(EntityId("entity-2"), nextEntity.entityId, "identifier minting must continue correctly across the restart")
    }

    // Memory Core Durability, Implementation Unit 10 (Verification), item 2: "Write/restart/read
    // tests, all five record kinds." The test above already covers Provenance and Entity through a
    // full DurableMemoryCore-level restart cycle -- the three tests below close the remaining gap for
    // Document, Assertion, and Relationship at this same level (each kind is already covered at the
    // lower MemoryCoreRecovery.recover level by MemoryCoreRecoveryTest.kt, but item 2 names the
    // DurableMemoryCore-wrapping restart cycle specifically, per its own "new DurableMemoryCore
    // wrapping a fresh InMemoryMemoryCore, same durability log" wording).

    @Test
    fun `a Document survives a full construct-write-restart-read cycle through DurableMemoryCore`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("durability.log")

        val first = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))
        val prov = first.createProvenance(principal, candidateProvenance)
        val created = first.registerDocument(principal, candidateDocument(prov.provenanceId))

        val second = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))

        assertEquals(created, second.getDocument(principal, created.documentId))
        val nextDocument = second.registerDocument(principal, candidateDocument(prov.provenanceId))
        assertEquals(DocumentId("document-2"), nextDocument.documentId, "identifier minting must continue correctly across the restart")
    }

    @Test
    fun `an Assertion survives a full construct-write-restart-read cycle through DurableMemoryCore`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("durability.log")

        val first = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))
        val prov = first.createProvenance(principal, candidateProvenance)
        val created = first.createAssertion(principal, candidateAssertion(prov.provenanceId))

        val second = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))

        assertEquals(created, second.getAssertion(principal, created.assertionId))
        val nextAssertion = second.createAssertion(principal, candidateAssertion(prov.provenanceId))
        assertEquals(AssertionId("assertion-2"), nextAssertion.assertionId, "identifier minting must continue correctly across the restart")
    }

    @Test
    fun `a Relationship survives a full construct-write-restart-read cycle through DurableMemoryCore`(@TempDir tempDir: Path) = runTest {
        val logFile = tempDir.resolve("durability.log")

        val first = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))
        val prov = first.createProvenance(principal, candidateProvenance)
        val fromEntity = first.createEntity(principal, candidateEntity(prov.provenanceId))
        val toEntity = first.createEntity(principal, candidateEntity(prov.provenanceId))
        val created = first.createRelationship(
            principal,
            candidateRelationship(
                prov.provenanceId,
                RelationshipEndpoint(RelationshipEndpoint.ENTITY, fromEntity.entityId.value),
                RelationshipEndpoint(RelationshipEndpoint.ENTITY, toEntity.entityId.value),
            ),
        )

        val second = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))

        assertEquals(created, second.getRelationship(principal, created.relationshipId))
        val nextRelationship = second.createRelationship(
            principal,
            candidateRelationship(
                prov.provenanceId,
                RelationshipEndpoint(RelationshipEndpoint.ENTITY, fromEntity.entityId.value),
                RelationshipEndpoint(RelationshipEndpoint.ENTITY, toEntity.entityId.value),
            ),
        )
        assertEquals(RelationshipId("relationship-2"), nextRelationship.relationshipId, "identifier minting must continue correctly across the restart")
    }

    // Memory Core Durability, Implementation Unit 10 (Verification), item 3: "Lifecycle-transition
    // restart test. A multi-step transition sequence survives restart with the correct final status
    // (Unit 6)." Already covered at the MemoryCoreRecovery.recover level by
    // MemoryCoreRecoveryTest.kt's own "multiple transitions on one record are replayed in order,
    // reaching the correct final status" -- this test closes the same property specifically through a
    // full DurableMemoryCore-level restart cycle, mirroring the three tests immediately above.
    @Test
    fun `a multi-step lifecycle transition sequence survives a full DurableMemoryCore restart, reaching the correct final status`(
        @TempDir tempDir: Path,
    ) = runTest {
        val logFile = tempDir.resolve("durability.log")

        val first = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))
        val prov = first.createProvenance(principal, candidateProvenance)
        val created = first.createEntity(principal, candidateEntity(prov.provenanceId))
        first.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(created.entityId), MemoryCoreRecordStatus.DISPUTED)
        first.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(created.entityId), MemoryCoreRecordStatus.ARCHIVED)

        val second = DurableMemoryCore.create(FileSystemMemoryCoreDurabilityLog(logFile))

        assertEquals(
            MemoryCoreRecordStatus.ARCHIVED,
            second.getEntity(principal, created.entityId)?.status,
            "the full two-step transition chain (ACTIVE -> DISPUTED -> ARCHIVED) must be genuinely replayed, not collapsed, and must survive the restart",
        )
    }

    // ================= Memory Core Durability, Implementation Unit 7: concurrency and ordering =================

    @Test
    fun `concurrent write calls never interleave -- durable append order exactly matches in-memory identifier-minting order`() = runTest {
        val writeCount = 12
        // A custom appendBehavior replaces FakeMemoryCoreDurabilityLog's own default recording
        // entirely (its own KDoc: "distinct from what a configured appendBehavior may have done
        // instead") -- this test therefore records observed append order into its own list,
        // after a deliberately reversed delay: a later-minted identifier is given a shorter delay
        // before its own append is recorded, so that -- absent the outer writeMutex -- a later
        // write would be strongly incentivised to durably land before an earlier one. If the
        // resulting observed order is still strictly increasing, the outer mutex is genuinely
        // serialising each write's entire prepare-append-commit body, not merely its append.
        val observedAppendOrder = java.util.concurrent.CopyOnWriteArrayList<Int>()
        val log = FakeMemoryCoreDurabilityLog(
            appendBehavior = { entry ->
                if (entry is DurableMemoryCoreEntry.EntityCreated) {
                    val suffix = entry.entity.entityId.value.removePrefix("entity-").toInt()
                    delay((writeCount - suffix).toLong())
                    observedAppendOrder.add(suffix)
                }
            },
        )
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)

        coroutineScope {
            (1..writeCount).map {
                async { durable.createEntity(principal, candidateEntity(prov.provenanceId)) }
            }.forEach { it.await() }
        }

        assertEquals((1..writeCount).toList(), observedAppendOrder.toList(), "append order must be strictly increasing -- no write's own append may land out of mint order")
    }

    @Test
    fun `concurrent write calls all durably commit exactly once -- no write is lost or duplicated under contention`() = runTest {
        val writeCount = 20
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)

        val created = coroutineScope {
            (1..writeCount).map {
                async { durable.createEntity(principal, candidateEntity(prov.provenanceId)) }
            }.map { it.await() }
        }

        assertEquals(writeCount, created.map { it.entityId }.toSet().size, "every concurrent write must mint a distinct identifier, none lost, none duplicated")
        assertEquals(writeCount, log.appended.count { it is DurableMemoryCoreEntry.EntityCreated })
        created.forEach { entity -> assertEquals(entity, durable.getEntity(principal, entity.entityId)) }
    }

    @Test
    fun `concurrent writes of different kinds -- including lifecycle transitions -- never interleave with each other`() = runTest {
        val log = FakeMemoryCoreDurabilityLog()
        val durable = DurableMemoryCore.create(log)
        val prov = durable.createProvenance(principal, candidateProvenance)
        val entity = durable.createEntity(principal, candidateEntity(prov.provenanceId))
        val appendedCountBeforeConcurrency = log.appended.size

        coroutineScope {
            val transition = async {
                durable.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(entity.entityId), MemoryCoreRecordStatus.DISPUTED)
            }
            val secondEntity = async { durable.createEntity(principal, candidateEntity(prov.provenanceId)) }
            transition.await()
            secondEntity.await()
        }

        assertEquals(appendedCountBeforeConcurrency + 2, log.appended.size, "both concurrent writes must durably commit, none lost")
        assertEquals(MemoryCoreRecordStatus.DISPUTED, durable.getEntity(principal, entity.entityId)?.status)
    }

    // ================= No public API widening; no prohibited dependency leakage =================

    @Test
    fun `DurableMemoryCore is internal, never public API surface`() {
        assertEquals(KVisibility.INTERNAL, DurableMemoryCore::class.visibility)
    }

    @Test
    fun `DurableMemoryCore has no public constructor -- create is the only way to obtain an instance`() {
        val primaryConstructor = DurableMemoryCore::class.primaryConstructor

        assertEquals(KVisibility.PRIVATE, primaryConstructor?.visibility)
    }

    @Test
    fun `create's own public signature references no PermissionEngine, Knowledge Memory, ParkerRuntime, or Docker type`() {
        val forbiddenSubstrings = listOf("PermissionEngine", "Knowledge", "EvidenceCustodian", "EvidenceIntelligence", "EventBus", "ParkerRuntime", "Docker")

        val createFunction = DurableMemoryCore.Companion::class.declaredFunctions.single { it.name == "create" }
        val types = createFunction.parameters.drop(1).map { it.type.jvmErasure } + createFunction.returnType.jvmErasure

        types.forEach { type ->
            val qualifiedName = type.qualifiedName ?: return@forEach
            assertTrue(forbiddenSubstrings.none { qualifiedName.contains(it) }, "found forbidden dependency '$qualifiedName' on DurableMemoryCore.create")
        }
    }
}
