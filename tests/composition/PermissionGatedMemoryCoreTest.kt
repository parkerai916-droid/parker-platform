package parker.composition

import java.lang.reflect.Modifier
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateDocument
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.CandidateRelationship
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DecisionId
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.Provenance
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.ResourceId
import parker.core.runtime.FakePermissionEngine
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Programme 2, Memory Core, Implementation Unit 10 ("Runtime
 * composition"). Behavioural tests for [PermissionGatedMemoryCore] --
 * see its own KDoc, and `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`
 * Sections 7 and 9, for the constitutional reasoning this suite verifies
 * exactly. Uses [FakeMemoryCore] (a local, capturing test double, not
 * `InMemoryMemoryCore`, so denial/exception paths can be proven never to
 * reach it) and [FakePermissionEngine] (`tests/runtime/FakePermissionEngine.kt`'s
 * own existing fixture).
 */
class PermissionGatedMemoryCoreTest {

    private val principal = PrincipalId("principal-1")

    private fun engineReturning(outcome: PermissionDecisionOutcome, onRequest: (ExecutionRequest) -> Unit = {}): FakePermissionEngine =
        FakePermissionEngine { request ->
            onRequest(request)
            PermissionDecision(
                decisionId = DecisionId("dec-1"),
                principalId = request.principalId,
                resourceId = request.targetResources.firstOrNull() ?: ResourceId("no-target-resource"),
                action = PermissionAction.WRITE,
                decision = outcome,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.now(),
            )
        }

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "source-1",
        sourceType = "test",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun provenance() = Provenance(
        provenanceId = ProvenanceId("provenance-1"),
        sourceIdentifier = "source-1",
        sourceType = "test",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        ingestionTime = Instant.parse("2026-01-01T00:00:01Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun candidateEntity() = CandidateEntity(entityType = "person", primaryLabel = "Jane Doe", provenanceId = ProvenanceId("provenance-1"))

    private fun entity() = Entity(
        entityId = EntityId("entity-1"),
        entityType = "person",
        primaryLabel = "Jane Doe",
        provenanceId = ProvenanceId("provenance-1"),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun candidateDocument() = CandidateDocument(documentType = "pdf", locationReference = "/tmp/doc-1", provenanceId = ProvenanceId("provenance-1"))

    private fun document() = Document(
        documentId = DocumentId("document-1"),
        documentType = "pdf",
        locationReference = "/tmp/doc-1",
        provenanceId = ProvenanceId("provenance-1"),
        registeredAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun candidateAssertion() = CandidateAssertion(statement = "the sky is blue", provenanceId = ProvenanceId("provenance-1"))

    private fun assertion() = Assertion(assertionId = AssertionId("assertion-1"), statement = "the sky is blue", provenanceId = ProvenanceId("provenance-1"))

    private fun candidateRelationship() = CandidateRelationship(
        relationshipType = Relationship.SUPPORTS,
        fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1"),
        toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
        directional = true,
        provenanceId = ProvenanceId("provenance-1"),
    )

    private fun relationship() = Relationship(
        relationshipId = RelationshipId("relationship-1"),
        relationshipType = Relationship.SUPPORTS,
        fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1"),
        toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
        directional = true,
        provenanceId = ProvenanceId("provenance-1"),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    // ================= createProvenance =================

    @Test
    fun `createProvenance delegates exactly once and returns the wrapped result unchanged when approved`() = runTest {
        val delegate = FakeMemoryCore(provenanceResult = provenance())
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        val candidate = candidateProvenance()
        val result = gated.createProvenance(principal, candidate)

        assertEquals(1, delegate.createProvenanceCallCount)
        assertEquals(1, engine.evaluateCallCount)
        assertEquals(principal, delegate.lastPrincipal)
        assertSame(candidate, delegate.lastCandidateProvenance)
        assertEquals(provenance(), result)
    }

    @Test
    fun `createProvenance never delegates and throws when denied`() = runTest {
        val delegate = FakeMemoryCore(provenanceResult = provenance())
        val engine = engineReturning(PermissionDecisionOutcome.DENIED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        assertFailsWith<MemoryCoreWriteDeniedException> { gated.createProvenance(principal, candidateProvenance()) }
        assertEquals(0, delegate.createProvenanceCallCount)
    }

    // ================= createEntity (the representative operation for cross-cutting behaviour) =================

    @Test
    fun `createEntity delegates exactly once and returns the wrapped result unchanged when approved`() = runTest {
        val delegate = FakeMemoryCore(entityResult = entity())
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        val candidate = candidateEntity()
        val result = gated.createEntity(principal, candidate)

        assertEquals(1, delegate.createEntityCallCount)
        assertEquals(1, engine.evaluateCallCount)
        assertEquals(principal, delegate.lastPrincipal)
        assertSame(candidate, delegate.lastCandidateEntity)
        assertEquals(entity(), result)
    }

    @Test
    fun `createEntity delegates exactly once when APPROVED_WITH_CONFIRMATION`() = runTest {
        val delegate = FakeMemoryCore(entityResult = entity())
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        gated.createEntity(principal, candidateEntity())

        assertEquals(1, delegate.createEntityCallCount)
    }

    @Test
    fun `createEntity never delegates and throws when denied, and never retries`() = runTest {
        val delegate = FakeMemoryCore(entityResult = entity())
        val engine = engineReturning(PermissionDecisionOutcome.DENIED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        assertFailsWith<MemoryCoreWriteDeniedException> { gated.createEntity(principal, candidateEntity()) }
        assertEquals(0, delegate.createEntityCallCount)
        assertEquals(1, engine.evaluateCallCount)
    }

    @Test
    fun `createEntity never delegates and throws when deferred`() = runTest {
        val delegate = FakeMemoryCore(entityResult = entity())
        val engine = engineReturning(PermissionDecisionOutcome.DEFERRED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        assertFailsWith<MemoryCoreWriteDeniedException> { gated.createEntity(principal, candidateEntity()) }
        assertEquals(0, delegate.createEntityCallCount)
    }

    @Test
    fun `createEntity request always names an empty targetResources list, per Errata 004 Section 7`() = runTest {
        var captured: ExecutionRequest? = null
        val delegate = FakeMemoryCore(entityResult = entity())
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED) { captured = it }
        val gated = PermissionGatedMemoryCore(delegate, engine)

        gated.createEntity(principal, candidateEntity())

        assertEquals(emptyList(), captured!!.targetResources)
        assertEquals(listOf(PermissionGatedMemoryCore.CREATE_ENTITY_ACTION_NAME), captured!!.proposedActions)
        assertEquals(principal, captured!!.principalId)
    }

    @Test
    fun `createEntity propagates a genuine exception thrown by the wrapped delegate unchanged`() = runTest {
        val fault = IllegalStateException("delegate fault")
        val delegate = FakeMemoryCore(entityResult = entity(), throwOnEntity = fault)
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        val thrown = assertFailsWith<IllegalStateException> { gated.createEntity(principal, candidateEntity()) }
        assertSame(fault, thrown)
    }

    @Test
    fun `createEntity propagates a genuine exception thrown by the permission engine unchanged, never delegating`() = runTest {
        val fault = RuntimeException("permission engine fault")
        val delegate = FakeMemoryCore(entityResult = entity())
        val engine = FakePermissionEngine { throw fault }
        val gated = PermissionGatedMemoryCore(delegate, engine)

        val thrown = assertFailsWith<RuntimeException> { gated.createEntity(principal, candidateEntity()) }
        assertSame(fault, thrown)
        assertEquals(0, delegate.createEntityCallCount)
    }

    // ================= registerDocument =================

    @Test
    fun `registerDocument delegates exactly once and returns the wrapped result unchanged when approved`() = runTest {
        val delegate = FakeMemoryCore(documentResult = document())
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        val candidate = candidateDocument()
        val result = gated.registerDocument(principal, candidate)

        assertEquals(1, delegate.registerDocumentCallCount)
        assertSame(candidate, delegate.lastCandidateDocument)
        assertEquals(document(), result)
    }

    @Test
    fun `registerDocument never delegates and throws when denied`() = runTest {
        val delegate = FakeMemoryCore(documentResult = document())
        val engine = engineReturning(PermissionDecisionOutcome.DENIED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        assertFailsWith<MemoryCoreWriteDeniedException> { gated.registerDocument(principal, candidateDocument()) }
        assertEquals(0, delegate.registerDocumentCallCount)
    }

    // ================= createAssertion =================

    @Test
    fun `createAssertion delegates exactly once and returns the wrapped result unchanged when approved`() = runTest {
        val delegate = FakeMemoryCore(assertionResult = assertion())
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        val candidate = candidateAssertion()
        val result = gated.createAssertion(principal, candidate)

        assertEquals(1, delegate.createAssertionCallCount)
        assertSame(candidate, delegate.lastCandidateAssertion)
        assertEquals(assertion(), result)
    }

    @Test
    fun `createAssertion never delegates and throws when denied`() = runTest {
        val delegate = FakeMemoryCore(assertionResult = assertion())
        val engine = engineReturning(PermissionDecisionOutcome.DENIED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        assertFailsWith<MemoryCoreWriteDeniedException> { gated.createAssertion(principal, candidateAssertion()) }
        assertEquals(0, delegate.createAssertionCallCount)
    }

    // ================= createRelationship =================

    @Test
    fun `createRelationship delegates exactly once and returns the wrapped result unchanged when approved`() = runTest {
        val delegate = FakeMemoryCore(relationshipResult = relationship())
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        val candidate = candidateRelationship()
        val result = gated.createRelationship(principal, candidate)

        assertEquals(1, delegate.createRelationshipCallCount)
        assertSame(candidate, delegate.lastCandidateRelationship)
        assertEquals(relationship(), result)
    }

    @Test
    fun `createRelationship never delegates and throws when denied`() = runTest {
        val delegate = FakeMemoryCore(relationshipResult = relationship())
        val engine = engineReturning(PermissionDecisionOutcome.DENIED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        assertFailsWith<MemoryCoreWriteDeniedException> { gated.createRelationship(principal, candidateRelationship()) }
        assertEquals(0, delegate.createRelationshipCallCount)
    }

    // ================= transitionStatus =================

    @Test
    fun `transitionStatus to a non-DELETED target uses the transition action name and delegates once when approved`() = runTest {
        var captured: ExecutionRequest? = null
        val transitioned = MemoryCoreRecord.OfEntity(entity().copy(status = MemoryCoreRecordStatus.ARCHIVED))
        val delegate = FakeMemoryCore(transitionResult = transitioned)
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED) { captured = it }
        val gated = PermissionGatedMemoryCore(delegate, engine)

        val reference = MemoryCoreRecordReference.ToEntity(EntityId("entity-1"))
        val result = gated.transitionStatus(principal, reference, MemoryCoreRecordStatus.ARCHIVED)

        assertEquals(1, delegate.transitionStatusCallCount)
        assertEquals(listOf(PermissionGatedMemoryCore.TRANSITION_STATUS_ACTION_NAME), captured!!.proposedActions)
        assertEquals(transitioned, result)
    }

    @Test
    fun `transitionStatus to DELETED uses the delete action name`() = runTest {
        var captured: ExecutionRequest? = null
        val deleted = MemoryCoreRecord.OfEntity(entity().copy(status = MemoryCoreRecordStatus.DELETED))
        val delegate = FakeMemoryCore(transitionResult = deleted)
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED) { captured = it }
        val gated = PermissionGatedMemoryCore(delegate, engine)

        gated.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(EntityId("entity-1")), MemoryCoreRecordStatus.DELETED)

        assertEquals(listOf(PermissionGatedMemoryCore.DELETE_RECORD_ACTION_NAME), captured!!.proposedActions)
    }

    @Test
    fun `transitionStatus never delegates and throws when denied`() = runTest {
        val delegate = FakeMemoryCore(transitionResult = MemoryCoreRecord.OfEntity(entity()))
        val engine = engineReturning(PermissionDecisionOutcome.DENIED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        assertFailsWith<MemoryCoreWriteDeniedException> {
            gated.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(EntityId("entity-1")), MemoryCoreRecordStatus.ARCHIVED)
        }
        assertEquals(0, delegate.transitionStatusCallCount)
    }

    @Test
    fun `transitionStatus propagates a genuine lifecycle exception thrown by the delegate unchanged`() = runTest {
        val fault = IllegalStateException("invalid transition")
        val delegate = FakeMemoryCore(transitionResult = MemoryCoreRecord.OfEntity(entity()), throwOnTransition = fault)
        val engine = engineReturning(PermissionDecisionOutcome.APPROVED)
        val gated = PermissionGatedMemoryCore(delegate, engine)

        val thrown = assertFailsWith<IllegalStateException> {
            gated.transitionStatus(principal, MemoryCoreRecordReference.ToEntity(EntityId("entity-1")), MemoryCoreRecordStatus.ARCHIVED)
        }
        assertSame(fault, thrown)
    }

    // ================= Structural safeguards =================

    @Test
    fun `PermissionGatedMemoryCore implements only MemoryCore`() {
        val interfaces = PermissionGatedMemoryCore::class.java.interfaces.toList()
        assertEquals(listOf(MemoryCore::class.java), interfaces)
    }

    @Test
    fun `PermissionGatedMemoryCore has exactly two constructor parameters -- the delegate and the permission engine`() {
        val constructor = PermissionGatedMemoryCore::class.primaryConstructor
        assertEquals(2, constructor!!.parameters.size)
    }

    @Test
    fun `PermissionGatedMemoryCore holds only its two constructor-injected instance fields, both immutable`() {
        // javap -v confirms a Kotlin class's own Companion reference and any companion-hosted
        // const val (hoisted onto the outer class as a static field) are declared static, not
        // synthetic -- filtering on !isSynthetic alone under-counts nothing here, but filtering
        // must also exclude static fields, exactly as the Programme 4 Unit 7 structural safeguard
        // fix already established (`EvidenceIntelligenceAcceptanceCoordinatorTest.kt`).
        val fields = PermissionGatedMemoryCore::class.java.declaredFields
            .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
        assertEquals(2, fields.size, "expected exactly delegate/permissionEngine -- found: ${fields.map { it.name }}")
        fields.forEach { field -> assertTrue(Modifier.isFinal(field.modifiers), "${field.name} must be immutable (final)") }
    }
}

/**
 * Test-only fake [MemoryCore], local to this file. Deliberately not
 * `InMemoryMemoryCore` -- a denial/exception-propagation test must be
 * able to prove the delegate was *never called*, which requires a
 * delegate that records call counts, not one holding real state.
 */
private class FakeMemoryCore(
    private val provenanceResult: Provenance? = null,
    private val entityResult: Entity? = null,
    private val documentResult: Document? = null,
    private val assertionResult: Assertion? = null,
    private val relationshipResult: Relationship? = null,
    private val transitionResult: MemoryCoreRecord? = null,
    private val throwOnEntity: RuntimeException? = null,
    private val throwOnTransition: RuntimeException? = null,
) : MemoryCore {

    var createProvenanceCallCount = 0
    var createEntityCallCount = 0
    var registerDocumentCallCount = 0
    var createAssertionCallCount = 0
    var createRelationshipCallCount = 0
    var transitionStatusCallCount = 0

    var lastPrincipal: PrincipalId? = null
    var lastCandidateProvenance: CandidateProvenance? = null
    var lastCandidateEntity: CandidateEntity? = null
    var lastCandidateDocument: CandidateDocument? = null
    var lastCandidateAssertion: CandidateAssertion? = null
    var lastCandidateRelationship: CandidateRelationship? = null

    override suspend fun createProvenance(requestingPrincipalId: PrincipalId, candidate: CandidateProvenance): Provenance {
        createProvenanceCallCount++
        lastPrincipal = requestingPrincipalId
        lastCandidateProvenance = candidate
        return provenanceResult!!
    }

    override suspend fun createEntity(requestingPrincipalId: PrincipalId, candidate: CandidateEntity): Entity {
        createEntityCallCount++
        lastPrincipal = requestingPrincipalId
        lastCandidateEntity = candidate
        throwOnEntity?.let { throw it }
        return entityResult!!
    }

    override suspend fun registerDocument(requestingPrincipalId: PrincipalId, candidate: CandidateDocument): Document {
        registerDocumentCallCount++
        lastPrincipal = requestingPrincipalId
        lastCandidateDocument = candidate
        return documentResult!!
    }

    override suspend fun createAssertion(requestingPrincipalId: PrincipalId, candidate: CandidateAssertion): Assertion {
        createAssertionCallCount++
        lastPrincipal = requestingPrincipalId
        lastCandidateAssertion = candidate
        return assertionResult!!
    }

    override suspend fun createRelationship(requestingPrincipalId: PrincipalId, candidate: CandidateRelationship): Relationship {
        createRelationshipCallCount++
        lastPrincipal = requestingPrincipalId
        lastCandidateRelationship = candidate
        return relationshipResult!!
    }

    override suspend fun transitionStatus(
        requestingPrincipalId: PrincipalId,
        reference: MemoryCoreRecordReference,
        targetStatus: MemoryCoreRecordStatus,
    ): MemoryCoreRecord {
        transitionStatusCallCount++
        lastPrincipal = requestingPrincipalId
        throwOnTransition?.let { throw it }
        return transitionResult!!
    }
}
