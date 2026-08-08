package parker.composition

import java.lang.reflect.Modifier
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.ChronologicalLookupQuery
import parker.core.interfaces.ContentNature
import parker.core.interfaces.DecisionId
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentLookupQuery
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.MetadataLookupQuery
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceLookupQuery
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipEndpoint
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.RelationshipTraversalQuery
import parker.core.interfaces.ResourceId
import parker.core.runtime.FakePermissionEngine
import kotlin.reflect.full.primaryConstructor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Programme 2, Memory Core, Implementation Unit 10 ("Runtime
 * composition"). Behavioural tests for [PermissionFilteredMemoryRetrieval]
 * -- see its own KDoc, and `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md`
 * Sections 5, 6, 8, and 9, for the constitutional reasoning this suite
 * verifies exactly. Uses [FakeMemoryRetrieval] (a local, capturing test
 * double, not `InMemoryMemoryCore`) and [FakePermissionEngine]
 * (`tests/runtime/FakePermissionEngine.kt`'s own existing fixture).
 */
class PermissionFilteredMemoryRetrievalTest {

    private val principal = PrincipalId("principal-1")

    /**
     * One [PermissionDecision] per [evaluate][parker.core.interfaces.PermissionEngine.evaluate]
     * call, in invocation order, from [outcomes] -- lets a single fake
     * express "approve the 1st and 3rd candidate record, deny the 2nd,"
     * which no single fixed outcome could, since every
     * [ExecutionRequest] this decorator builds is otherwise
     * indistinguishable per-record (Errata 004 Section 7: `targetResources`
     * is always empty).
     */
    private fun sequencedEngine(
        vararg outcomes: PermissionDecisionOutcome,
        captured: MutableList<ExecutionRequest>? = null,
    ): FakePermissionEngine {
        var index = 0
        return FakePermissionEngine { request ->
            captured?.add(request)
            val outcome = outcomes[index]
            index++
            PermissionDecision(
                decisionId = DecisionId("dec-$index"),
                principalId = request.principalId,
                resourceId = ResourceId("no-target-resource"),
                action = PermissionAction.READ,
                decision = outcome,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.now(),
            )
        }
    }

    private fun fixedEngine(outcome: PermissionDecisionOutcome, onRequest: (ExecutionRequest) -> Unit = {}): FakePermissionEngine =
        FakePermissionEngine { request ->
            onRequest(request)
            PermissionDecision(
                decisionId = DecisionId("dec-1"),
                principalId = request.principalId,
                resourceId = ResourceId("no-target-resource"),
                action = PermissionAction.READ,
                decision = outcome,
                level = PermissionLevel.AUTOMATIC,
                timestamp = Instant.now(),
            )
        }

    private fun entity(id: String) = Entity(
        entityId = EntityId(id),
        entityType = "person",
        primaryLabel = "Jane Doe",
        provenanceId = ProvenanceId("provenance-1"),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun document(id: String) = Document(
        documentId = DocumentId(id),
        documentType = "pdf",
        locationReference = "/tmp/$id",
        provenanceId = ProvenanceId("provenance-1"),
        registeredAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun assertion(id: String) = Assertion(assertionId = AssertionId(id), statement = "a claim", provenanceId = ProvenanceId("provenance-1"))

    private fun relationship(id: String) = Relationship(
        relationshipId = RelationshipId(id),
        relationshipType = Relationship.SUPPORTS,
        fromEndpoint = RelationshipEndpoint(RelationshipEndpoint.ASSERTION, "assertion-1"),
        toEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
        directional = true,
        provenanceId = ProvenanceId("provenance-1"),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    // ================= Direct lookups: getEntity (representative) =================

    @Test
    fun `getEntity returns the authorised record unchanged when approved`() = runTest {
        val e = entity("entity-1")
        val delegate = FakeMemoryRetrieval(entityResult = e)
        val engine = fixedEngine(PermissionDecisionOutcome.APPROVED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val result = filtered.getEntity(principal, EntityId("entity-1"))

        assertEquals(e, result)
    }

    @Test
    fun `getEntity never exposes a denied record`() = runTest {
        val delegate = FakeMemoryRetrieval(entityResult = entity("entity-1"))
        val engine = fixedEngine(PermissionDecisionOutcome.DENIED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        assertNull(filtered.getEntity(principal, EntityId("entity-1")))
    }

    @Test
    fun `getEntity genuine absence remains genuine absence, and evaluates no permission decision`() = runTest {
        val delegate = FakeMemoryRetrieval(entityResult = null)
        val engine = fixedEngine(PermissionDecisionOutcome.APPROVED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        assertNull(filtered.getEntity(principal, EntityId("nonexistent")))
        assertEquals(0, engine.evaluateCallCount)
    }

    @Test
    fun `getEntity propagates the explicit requesting principal, and evaluates READ on the non-Document action`() = runTest {
        var captured: ExecutionRequest? = null
        val delegate = FakeMemoryRetrieval(entityResult = entity("entity-1"))
        val engine = fixedEngine(PermissionDecisionOutcome.APPROVED) { captured = it }
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        filtered.getEntity(principal, EntityId("entity-1"))

        assertEquals(principal, captured!!.principalId)
        assertEquals(emptyList(), captured!!.targetResources)
        assertEquals(listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME), captured!!.proposedActions)
    }

    @Test
    fun `getEntity propagates a genuine exception thrown by the delegate unchanged`() = runTest {
        val fault = IllegalStateException("delegate fault")
        val delegate = FakeMemoryRetrieval(throwOnGetEntity = fault)
        val engine = fixedEngine(PermissionDecisionOutcome.APPROVED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val thrown = assertFailsWith<IllegalStateException> { filtered.getEntity(principal, EntityId("entity-1")) }
        assertSame(fault, thrown)
    }

    @Test
    fun `getEntity propagates a genuine exception thrown by the permission engine unchanged`() = runTest {
        val fault = RuntimeException("permission engine fault")
        val delegate = FakeMemoryRetrieval(entityResult = entity("entity-1"))
        val engine = FakePermissionEngine { throw fault }
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val thrown = assertFailsWith<RuntimeException> { filtered.getEntity(principal, EntityId("entity-1")) }
        assertSame(fault, thrown)
    }

    // ================= Direct lookups: getDocument (Document-specific action name) =================

    @Test
    fun `getDocument evaluates READ on the Document-specific action name`() = runTest {
        var captured: ExecutionRequest? = null
        val delegate = FakeMemoryRetrieval(documentResult = document("document-1"))
        val engine = fixedEngine(PermissionDecisionOutcome.APPROVED) { captured = it }
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val result = filtered.getDocument(principal, DocumentId("document-1"))

        assertEquals(document("document-1"), result)
        assertEquals(listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME), captured!!.proposedActions)
    }

    @Test
    fun `getDocument never exposes a denied record`() = runTest {
        val delegate = FakeMemoryRetrieval(documentResult = document("document-1"))
        val engine = fixedEngine(PermissionDecisionOutcome.DENIED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        assertNull(filtered.getDocument(principal, DocumentId("document-1")))
    }

    // ================= Direct lookups: getAssertion / getRelationship (brief confirmation) =================

    @Test
    fun `getAssertion returns the authorised record and never exposes a denied one`() = runTest {
        val a = assertion("assertion-1")
        val approved = PermissionFilteredMemoryRetrieval(FakeMemoryRetrieval(assertionResult = a), fixedEngine(PermissionDecisionOutcome.APPROVED))
        val denied = PermissionFilteredMemoryRetrieval(FakeMemoryRetrieval(assertionResult = a), fixedEngine(PermissionDecisionOutcome.DENIED))

        assertEquals(a, approved.getAssertion(principal, AssertionId("assertion-1")))
        assertNull(denied.getAssertion(principal, AssertionId("assertion-1")))
    }

    @Test
    fun `getRelationship returns the authorised record and never exposes a denied one`() = runTest {
        val r = relationship("relationship-1")
        val approved = PermissionFilteredMemoryRetrieval(FakeMemoryRetrieval(relationshipResult = r), fixedEngine(PermissionDecisionOutcome.APPROVED))
        val denied = PermissionFilteredMemoryRetrieval(FakeMemoryRetrieval(relationshipResult = r), fixedEngine(PermissionDecisionOutcome.DENIED))

        assertEquals(r, approved.getRelationship(principal, RelationshipId("relationship-1")))
        assertNull(denied.getRelationship(principal, RelationshipId("relationship-1")))
    }

    // ================= Query/list methods: findEntities (representative) =================

    private fun entityQuery() = EntityLookupQuery(requestingPrincipalId = principal, maximumResults = 10)

    @Test
    fun `findEntities keeps only authorised records, in original order, with one evaluation per candidate`() = runTest {
        val e1 = entity("entity-1")
        val e2 = entity("entity-2")
        val e3 = entity("entity-3")
        val delegate = FakeMemoryRetrieval(entitiesResult = listOf(e1, e2, e3))
        val engine = sequencedEngine(
            PermissionDecisionOutcome.APPROVED,
            PermissionDecisionOutcome.DENIED,
            PermissionDecisionOutcome.APPROVED,
        )
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val result = filtered.findEntities(entityQuery())

        assertEquals(listOf(e1, e3), result)
        assertSame(e1, result[0])
        assertSame(e3, result[1])
        assertEquals(3, engine.evaluateCallCount)
    }

    @Test
    fun `findEntities returns an empty list unchanged, evaluating nothing`() = runTest {
        val delegate = FakeMemoryRetrieval(entitiesResult = emptyList())
        val engine = fixedEngine(PermissionDecisionOutcome.APPROVED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        assertEquals(emptyList(), filtered.findEntities(entityQuery()))
        assertEquals(0, engine.evaluateCallCount)
    }

    @Test
    fun `findEntities propagates a genuine exception thrown by the delegate unchanged`() = runTest {
        val fault = IllegalStateException("delegate fault")
        val delegate = FakeMemoryRetrieval(throwOnFindEntities = fault)
        val engine = fixedEngine(PermissionDecisionOutcome.APPROVED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val thrown = assertFailsWith<IllegalStateException> { filtered.findEntities(entityQuery()) }
        assertSame(fault, thrown)
    }

    @Test
    fun `findEntities propagates a genuine exception thrown by the permission engine mid-filter, unchanged`() = runTest {
        val fault = RuntimeException("permission engine fault")
        val delegate = FakeMemoryRetrieval(entitiesResult = listOf(entity("entity-1"), entity("entity-2")))
        val engine = FakePermissionEngine { throw fault }
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val thrown = assertFailsWith<RuntimeException> { filtered.findEntities(entityQuery()) }
        assertSame(fault, thrown)
    }

    @Test
    fun `findEntities evaluates READ on the non-Document action for every candidate`() = runTest {
        val captured = mutableListOf<ExecutionRequest>()
        val delegate = FakeMemoryRetrieval(entitiesResult = listOf(entity("entity-1"), entity("entity-2")))
        val engine = sequencedEngine(PermissionDecisionOutcome.APPROVED, PermissionDecisionOutcome.APPROVED, captured = captured)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        filtered.findEntities(entityQuery())

        assertEquals(2, captured.size)
        captured.forEach { request ->
            assertEquals(listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME), request.proposedActions)
            assertEquals(emptyList(), request.targetResources)
            assertEquals(principal, request.principalId)
        }
    }

    // ================= Query/list methods: the remaining five (routing + action-name confirmation) =================

    @Test
    fun `findDocuments evaluates READ on the Document-specific action, preserving order`() = runTest {
        val d1 = document("document-1")
        val d2 = document("document-2")
        val captured = mutableListOf<ExecutionRequest>()
        val delegate = FakeMemoryRetrieval(documentsResult = listOf(d1, d2))
        val engine = sequencedEngine(PermissionDecisionOutcome.APPROVED, PermissionDecisionOutcome.APPROVED, captured = captured)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val result = filtered.findDocuments(DocumentLookupQuery(requestingPrincipalId = principal, maximumResults = 10))

        assertEquals(listOf(d1, d2), result)
        captured.forEach { assertEquals(listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME), it.proposedActions) }
    }

    @Test
    fun `traverseRelationships keeps only authorised records in original order`() = runTest {
        val r1 = relationship("relationship-1")
        val r2 = relationship("relationship-2")
        val delegate = FakeMemoryRetrieval(relationshipsResult = listOf(r1, r2))
        val engine = sequencedEngine(PermissionDecisionOutcome.DENIED, PermissionDecisionOutcome.APPROVED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val query = RelationshipTraversalQuery(
            requestingPrincipalId = principal,
            maximumResults = 10,
            startingEndpoint = RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-1"),
        )

        assertEquals(listOf(r2), filtered.traverseRelationships(query))
    }

    @Test
    fun `findByTimeRange selects the Document-specific action per record kind within one mixed-kind result set`() = runTest {
        val captured = mutableListOf<ExecutionRequest>()
        val mixed = listOf(
            MemoryCoreRecord.OfEntity(entity("entity-1")),
            MemoryCoreRecord.OfDocument(document("document-1")),
            MemoryCoreRecord.OfAssertion(assertion("assertion-1")),
        )
        val delegate = FakeMemoryRetrieval(recordsResult = mixed)
        val engine = sequencedEngine(
            PermissionDecisionOutcome.APPROVED,
            PermissionDecisionOutcome.APPROVED,
            PermissionDecisionOutcome.APPROVED,
            captured = captured,
        )
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val query = ChronologicalLookupQuery(
            requestingPrincipalId = principal,
            maximumResults = 10,
            from = Instant.parse("2026-01-01T00:00:00Z"),
            to = Instant.parse("2026-01-02T00:00:00Z"),
        )
        val result = filtered.findByTimeRange(query)

        assertEquals(mixed, result)
        assertEquals(
            listOf(
                PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
                PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
                PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
            ),
            captured.map { it.proposedActions.single() },
        )
    }

    @Test
    fun `findByMetadata keeps only authorised records`() = runTest {
        val mixed = listOf(MemoryCoreRecord.OfEntity(entity("entity-1")), MemoryCoreRecord.OfDocument(document("document-1")))
        val delegate = FakeMemoryRetrieval(recordsResult = mixed)
        val engine = sequencedEngine(PermissionDecisionOutcome.APPROVED, PermissionDecisionOutcome.DENIED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val query = MetadataLookupQuery(
            requestingPrincipalId = principal,
            maximumResults = 10,
            recordKind = "entity",
            metadataFilter = mapOf("entityType" to "person"),
        )

        assertEquals(listOf(mixed[0]), filtered.findByMetadata(query))
    }

    @Test
    fun `findByProvenance keeps only authorised records`() = runTest {
        val mixed = listOf(MemoryCoreRecord.OfAssertion(assertion("assertion-1")), MemoryCoreRecord.OfDocument(document("document-1")))
        val delegate = FakeMemoryRetrieval(recordsResult = mixed)
        val engine = sequencedEngine(PermissionDecisionOutcome.DENIED, PermissionDecisionOutcome.APPROVED)
        val filtered = PermissionFilteredMemoryRetrieval(delegate, engine)

        val query = ProvenanceLookupQuery(requestingPrincipalId = principal, maximumResults = 10, recordKind = "assertion", sourceType = "test")

        assertEquals(listOf(mixed[1]), filtered.findByProvenance(query))
    }

    // ================= Structural safeguards =================

    @Test
    fun `a purpose-bound view carries its one exact immutable purpose while the unbound parent remains absent-purpose`() = runTest {
        val purpose = AuthorizationPurposeId("knowledge-memory.candidate-evaluation")
        val captured = mutableListOf<ExecutionRequest>()
        val filtered = PermissionFilteredMemoryRetrieval(
            FakeMemoryRetrieval(entityResult = entity("entity-purpose")),
            fixedEngine(PermissionDecisionOutcome.DENIED) { captured += it },
        )
        val bound = filtered.forAuthorizationPurpose(purpose)

        assertNull(bound.getEntity(principal, EntityId("entity-purpose")))
        assertNull(filtered.getEntity(principal, EntityId("entity-purpose")))

        assertEquals(purpose, captured[0].authorizationPurpose)
        assertNull(captured[1].authorizationPurpose)
    }

    @Test
    fun `two purpose-bound views are distinct immutable carriers over the same parent with no authority dependencies`() {
        val filtered = PermissionFilteredMemoryRetrieval(FakeMemoryRetrieval(), fixedEngine(PermissionDecisionOutcome.DENIED))
        val candidate = filtered.forAuthorizationPurpose(AuthorizationPurposeId("knowledge-memory.candidate-evaluation"))
        val evidence = filtered.forAuthorizationPurpose(AuthorizationPurposeId("evidence-intelligence.input-resolution"))

        assertNotSame(candidate, evidence)
        val candidateFields = candidate::class.java.declaredFields.filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
        val evidenceFields = evidence::class.java.declaredFields.filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
        assertEquals(setOf("parent", "authorizationPurpose"), candidateFields.map { it.name }.toSet())
        assertEquals(setOf("parent", "authorizationPurpose"), evidenceFields.map { it.name }.toSet())
        (candidateFields + evidenceFields).forEach { assertTrue(Modifier.isFinal(it.modifiers), "${it.name} must be immutable") }

        fun Any.field(name: String): Any? = this::class.java.getDeclaredField(name).also { it.isAccessible = true }.get(this)
        assertSame(filtered, candidate.field("parent"))
        assertSame(filtered, evidence.field("parent"))
        // AuthorizationPurposeId is a Kotlin value class and is therefore unboxed to its String
        // representation in this private backing field; request-level behavioral tests above
        // prove it is re-boxed correctly on ExecutionRequest.
        assertEquals("knowledge-memory.candidate-evaluation", candidate.field("authorizationPurpose"))
        assertEquals("evidence-intelligence.input-resolution", evidence.field("authorizationPurpose"))
        assertTrue(candidateFields.none { it.type.name.contains("PermissionEngine") || it.type.name.contains("Registry") })
    }

    @Test
    fun `PermissionFilteredMemoryRetrieval implements only MemoryRetrieval`() {
        val interfaces = PermissionFilteredMemoryRetrieval::class.java.interfaces.toList()
        assertEquals(listOf(MemoryRetrieval::class.java), interfaces)
    }

    @Test
    fun `PermissionFilteredMemoryRetrieval has exactly two constructor parameters -- the delegate and the permission engine`() {
        val constructor = PermissionFilteredMemoryRetrieval::class.primaryConstructor
        assertEquals(2, constructor!!.parameters.size)
    }

    @Test
    fun `PermissionFilteredMemoryRetrieval holds only its two constructor-injected instance fields, both immutable`() {
        // Instance-field filtering, not synthetic-field filtering alone -- a Kotlin companion
        // object's own Companion field and any hoisted const val are static, not synthetic, and
        // must be excluded by Modifier.isStatic, not by isSynthetic (Programme 4 Unit 7's own
        // structural safeguard fix, EvidenceIntelligenceAcceptanceCoordinatorTest.kt).
        val fields = PermissionFilteredMemoryRetrieval::class.java.declaredFields
            .filter { !it.isSynthetic && !Modifier.isStatic(it.modifiers) }
        assertEquals(2, fields.size, "expected exactly delegate/permissionEngine -- found: ${fields.map { it.name }}")
        fields.forEach { field -> assertTrue(Modifier.isFinal(field.modifiers), "${field.name} must be immutable (final)") }
    }
}

/**
 * Test-only fake [MemoryRetrieval], local to this file. Deliberately not
 * `InMemoryMemoryCore` -- an absence/exception-propagation test must
 * control exactly what the delegate returns or throws, independent of
 * any real store's own state.
 */
private class FakeMemoryRetrieval(
    private val entityResult: Entity? = null,
    private val documentResult: Document? = null,
    private val assertionResult: Assertion? = null,
    private val relationshipResult: Relationship? = null,
    private val entitiesResult: List<Entity> = emptyList(),
    private val documentsResult: List<Document> = emptyList(),
    private val relationshipsResult: List<Relationship> = emptyList(),
    private val recordsResult: List<MemoryCoreRecord> = emptyList(),
    private val throwOnGetEntity: RuntimeException? = null,
    private val throwOnFindEntities: RuntimeException? = null,
) : MemoryRetrieval {

    override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? {
        throwOnGetEntity?.let { throw it }
        return entityResult
    }

    override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? = documentResult

    override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? = assertionResult

    override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? = relationshipResult

    override suspend fun findEntities(query: EntityLookupQuery): List<Entity> {
        throwOnFindEntities?.let { throw it }
        return entitiesResult
    }

    override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> = documentsResult

    override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> = relationshipsResult

    override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> = recordsResult

    override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> = recordsResult

    override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> = recordsResult
}
