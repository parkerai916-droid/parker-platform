package parker.core.runtime

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.primaryConstructor
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.ChronologicalLookupQuery
import parker.core.interfaces.DecisionId
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentLookupQuery
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryCoreRecordStatus
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.MetadataLookupQuery
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceLookupQuery
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.RelationshipTraversalQuery
import parker.core.interfaces.SafeKnowledgeResultEntry
import parker.core.interfaces.StalenessDisclosure
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Knowledge Discoverability and Governed Retrieval into Reasoning Context, Implementation Unit 2.
 * Behavioural and structural tests for [DefaultReasoningKnowledgeSource] -- see
 * `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md` Section 7 for
 * the exact, distinct, mandatory test set this suite implements. Every test below proves exactly one
 * behaviour; no test substitutes for another.
 */
class DefaultReasoningKnowledgeSourceTest {

    private val principal = PrincipalId("owner-1")
    private val purpose = AuthorizationPurposeId("knowledge-memory.reasoning-context-retrieval")
    private val now = Instant.parse("2026-06-15T12:00:00Z")
    private val fixedClock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    private val originalLocale: Locale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    // --- Fixtures ---

    private fun toAssertion(id: String) = MemoryCoreRecordReference.ToAssertion(AssertionId(id))
    private fun toEntity(id: String) = MemoryCoreRecordReference.ToEntity(EntityId(id))
    private fun toDocument(id: String) = MemoryCoreRecordReference.ToDocument(DocumentId(id))
    private fun toRelationship(id: String) = MemoryCoreRecordReference.ToRelationship(RelationshipId(id))

    private fun item(
        knowledgeId: KnowledgeId,
        evidenceReference: MemoryCoreRecordReference,
        status: KnowledgeItemStatus = KnowledgeItemStatus.ACTIVE,
        evidentialState: EvidentialState = EvidentialState.UNKNOWN,
        basis: String = "generic promotion basis text",
        occurredAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ): KnowledgeItem {
        val promotion = KnowledgePromotion(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            resultingState = evidentialState,
            occurredAt = occurredAt,
            basis = basis,
        )
        return KnowledgeItem(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            provenanceReference = ProvenanceReference(ProvenanceId("prov-${knowledgeId.value}")),
            evidentialState = evidentialState,
            status = status,
            history = listOf(promotion),
        )
    }

    private fun assertionRecord(
        id: String,
        statement: String,
        status: MemoryCoreRecordStatus = MemoryCoreRecordStatus.ACTIVE,
    ) = Assertion(
        assertionId = AssertionId(id),
        statement = statement,
        provenanceId = ProvenanceId("prov-$id"),
        status = status,
    )

    private fun entityRecord(
        id: String,
        primaryLabel: String,
        aliases: List<String> = emptyList(),
        status: MemoryCoreRecordStatus = MemoryCoreRecordStatus.ACTIVE,
    ) = Entity(
        entityId = EntityId(id),
        entityType = "person",
        primaryLabel = primaryLabel,
        provenanceId = ProvenanceId("prov-$id"),
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        aliases = aliases,
        status = status,
    )

    private fun query(relevance: String, maximumResults: Int = 10, includeRetired: Boolean = false) = KnowledgeRetrievalQuery(
        relevance = relevance,
        correlationId = "corr-1",
        maximumResults = maximumResults,
        includeRetired = includeRetired,
    )

    private fun decision(request: ExecutionRequest, outcome: PermissionDecisionOutcome) = PermissionDecision(
        decisionId = DecisionId("decision-1"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = PermissionAction.READ,
        decision = outcome,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.now(),
    )

    /** Approves every evaluation, act-level and item-level alike. */
    private fun approvingEngine() = FakePermissionEngine { request -> decision(request, PermissionDecisionOutcome.APPROVED) }

    /** Denies every evaluation, act-level and item-level alike. */
    private fun denyingEngine() = FakePermissionEngine { request -> decision(request, PermissionDecisionOutcome.DENIED) }

    /**
     * Approves the act-level gate unconditionally; approves an item-level gate only when its own
     * [ExecutionRequest.intent] names one of [approvedKnowledgeIds] -- every other item-level gate is
     * denied. Distinguishes granularities by comparing [ExecutionRequest.intent] against
     * [DefaultReasoningKnowledgeSource.ACT_LEVEL_INTENT], mirroring
     * `DefaultKnowledgeRetrievalTest`'s own established convention.
     */
    private fun actLevelApprovingEngine(vararg approvedKnowledgeIds: String) = FakePermissionEngine { request ->
        val outcome = when {
            request.intent == DefaultReasoningKnowledgeSource.ACT_LEVEL_INTENT -> PermissionDecisionOutcome.APPROVED
            approvedKnowledgeIds.any { request.intent.contains("'$it'") } -> PermissionDecisionOutcome.APPROVED
            else -> PermissionDecisionOutcome.DENIED
        }
        decision(request, outcome)
    }

    /**
     * A [KnowledgeItemPersistence] spy counting [findAll] invocations, delegating everything else to a
     * real backing [InMemoryKnowledgeItemPersistence].
     */
    private class RecordingKnowledgeItemPersistence(
        private val delegate: KnowledgeItemPersistence = InMemoryKnowledgeItemPersistence(),
    ) : KnowledgeItemPersistence {
        var findAllCallCount: Int = 0
            private set

        override suspend fun store(item: KnowledgeItem): KnowledgeItem = delegate.store(item)
        override suspend fun find(knowledgeId: KnowledgeId): KnowledgeItem? = delegate.find(knowledgeId)
        override suspend fun findAll(): List<KnowledgeItem> {
            findAllCallCount++
            return delegate.findAll()
        }
    }

    private class ThrowingKnowledgeItemPersistence(private val failure: Throwable) : KnowledgeItemPersistence {
        override suspend fun store(item: KnowledgeItem): KnowledgeItem = throw failure
        override suspend fun find(knowledgeId: KnowledgeId): KnowledgeItem? = throw failure
        override suspend fun findAll(): List<KnowledgeItem> = throw failure
    }

    /**
     * A fully controllable [MemoryRetrieval] test double modelling three distinct states per ID:
     * **authorized existing** ([assertions]/[entities] -- a genuine record, disclosed), **denied
     * existing** ([deniedAssertions]/[deniedEntities] -- a genuine, non-null protected record the
     * accessor still withholds, mirroring production `PermissionFilteredMemoryRetrieval`'s own
     * "retrieve unconditionally from the delegate, then gate" nondisclosure exactly), and **missing**
     * (absent from both -- no record exists at all). [getAssertion]/[getEntity] record every requested
     * ID regardless of outcome, so a test can prove genuine dereference was attempted even when the
     * result is `null`. An ID present in both an authorized and a denied collection is not a
     * configuration this test double needs to resolve -- no fixture below does so. Every other method
     * throws immediately on any invocation -- proof that [DefaultReasoningKnowledgeSource] reaches
     * only [getAssertion]/[getEntity], mirroring `DefaultKnowledgeCandidateEvaluatorTest`'s own
     * established `RecordingMemoryRetrieval` spy pattern.
     */
    private class RecordingMemoryRetrieval(
        private val assertions: Map<String, Assertion> = emptyMap(),
        private val entities: Map<String, Entity> = emptyMap(),
        private val deniedAssertions: Map<String, Assertion> = emptyMap(),
        private val deniedEntities: Map<String, Entity> = emptyMap(),
        private val failure: Throwable? = null,
    ) : MemoryRetrieval {
        val getAssertionCalls = mutableListOf<AssertionId>()
        val getEntityCalls = mutableListOf<EntityId>()

        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? {
            getAssertionCalls += assertionId
            failure?.let { throw it }
            // The denied record genuinely exists in this fixture -- withheld here, exactly as
            // PermissionFilteredMemoryRetrieval.getAssertion retrieves first, then gates.
            if (assertionId.value in deniedAssertions) return null
            return assertions[assertionId.value]
        }

        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? {
            getEntityCalls += entityId
            failure?.let { throw it }
            if (entityId.value in deniedEntities) return null
            return entities[entityId.value]
        }

        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document? =
            throw AssertionError("getDocument must never be called by DefaultReasoningKnowledgeSource")

        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? =
            throw AssertionError("getRelationship must never be called by DefaultReasoningKnowledgeSource")

        override suspend fun findEntities(query: EntityLookupQuery): List<Entity> =
            throw AssertionError("findEntities must never be called by DefaultReasoningKnowledgeSource")

        override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> =
            throw AssertionError("findDocuments must never be called by DefaultReasoningKnowledgeSource")

        override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> =
            throw AssertionError("traverseRelationships must never be called by DefaultReasoningKnowledgeSource")

        override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> =
            throw AssertionError("findByTimeRange must never be called by DefaultReasoningKnowledgeSource")

        override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> =
            throw AssertionError("findByMetadata must never be called by DefaultReasoningKnowledgeSource")

        override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> =
            throw AssertionError("findByProvenance must never be called by DefaultReasoningKnowledgeSource")
    }

    private fun source(
        persistence: KnowledgeItemPersistence,
        permissionEngine: FakePermissionEngine,
        evidenceMemoryRetrieval: MemoryRetrieval,
        clock: Clock = fixedClock,
    ) = DefaultReasoningKnowledgeSource(persistence, permissionEngine, evidenceMemoryRetrieval, purpose, clock)

    // --- A. Positive matching ---

    @Test
    fun `an ACTIVE Assertion whose statement contains the query relevance is returned`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "the owner's favourite programming language is Kotlin")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "Kotlin"))

        assertEquals(1, entries.size)
        assertEquals("the owner's favourite programming language is Kotlin", entries.single().content)
    }

    @Test
    fun `an ACTIVE Entity whose primaryLabel contains the query relevance is returned`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toEntity("e1")))
        val evidence = RecordingMemoryRetrieval(entities = mapOf("e1" to entityRecord("e1", "Stellar the dog")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "Stellar"))

        assertEquals(1, entries.size)
        assertEquals("Stellar the dog", entries.single().content)
    }

    @Test
    fun `an ACTIVE Entity whose alias contains the query relevance is returned`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toEntity("e1")))
        val evidence = RecordingMemoryRetrieval(entities = mapOf("e1" to entityRecord("e1", "Stellar", aliases = listOf("Stella", "the good dog"))))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "good dog"))

        assertEquals(1, entries.size)
    }

    @Test
    fun `Entity content joins primaryLabel and every alias in order with the fixed separator`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toEntity("e1")))
        val evidence = RecordingMemoryRetrieval(entities = mapOf("e1" to entityRecord("e1", "Stellar", aliases = listOf("Stella", "Star"))))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "Stellar"))

        assertEquals("Stellar | Stella | Star", entries.single().content)
    }

    // --- B. Authorization ---

    @Test
    fun `act-level denial returns emptyList`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list")))
        val subject = source(persistence, denyingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
    }

    @Test
    fun `act-level denial invokes persistence findAll zero times`() = runTest {
        val persistence = RecordingKnowledgeItemPersistence()
        val evidence = RecordingMemoryRetrieval()
        val subject = source(persistence, denyingEngine(), evidence)

        subject.recall(principal, query(relevance = "grocery"))

        assertEquals(0, persistence.findAllCallCount)
    }

    @Test
    fun `item-level denial with act allowed returns only the approved item`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("approved"), toAssertion("a1")))
        persistence.store(item(KnowledgeId("denied"), toAssertion("a2")))
        val evidence = RecordingMemoryRetrieval(
            assertions = mapOf(
                "a1" to assertionRecord("a1", "grocery list one"),
                "a2" to assertionRecord("a2", "grocery list two"),
            ),
        )
        val subject = source(persistence, actLevelApprovingEngine("approved"), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(1, entries.size)
        assertEquals("grocery list one", entries.single().content)
        assertEquals(
            listOf(AssertionId("a1")),
            evidence.getAssertionCalls,
            "item authorization must precede dereference -- the denied item's own evidence must never be requested",
        )
    }

    @Test
    fun `item-level denial is silent and exposes no denial marker`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("denied"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list")))
        val subject = source(persistence, actLevelApprovingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(
            emptyList<SafeKnowledgeResultEntry>(),
            entries,
            "a denied item must be silently excluded -- no exception, no marker, indistinguishable from a non-match",
        )
        assertEquals(
            emptyList<AssertionId>(),
            evidence.getAssertionCalls,
            "item authorization must precede dereference -- a denied item's own evidence must never be requested at all",
        )
    }

    // --- C. Referenced-evidence proofs ---

    @Test
    fun `a denied Assertion is silently excluded and its content never returned`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val protectedStatement = "grocery list protected-content-marker-7f3a"
        val evidence = RecordingMemoryRetrieval(deniedAssertions = mapOf("a1" to assertionRecord("a1", protectedStatement)))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
        assertEquals(
            listOf(AssertionId("a1")),
            evidence.getAssertionCalls,
            "the exact denied Assertion ID must genuinely be requested, proving a real protected record was consulted",
        )
        assertTrue(
            entries.none { it.content.contains(protectedStatement) },
            "the protected Assertion's own statement must never appear in any returned entry",
        )
    }

    @Test
    fun `a denied Entity is silently excluded and its content never returned`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toEntity("e1")))
        val protectedLabel = "grocery-protected-primary-label-9c2d"
        val protectedAlias = "grocery-protected-alias-4e1b"
        val evidence = RecordingMemoryRetrieval(
            deniedEntities = mapOf("e1" to entityRecord("e1", protectedLabel, aliases = listOf(protectedAlias))),
        )
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
        assertEquals(
            listOf(EntityId("e1")),
            evidence.getEntityCalls,
            "the exact denied Entity ID must genuinely be requested, proving a real protected record was consulted",
        )
        assertTrue(
            entries.none { it.content.contains(protectedLabel) || it.content.contains(protectedAlias) },
            "neither the protected Entity's own primaryLabel nor alias content must ever appear in any returned entry",
        )
    }

    @Test
    fun `a missing or deleted Assertion reference resolves to no result, silently`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("nonexistent")))
        val evidence = RecordingMemoryRetrieval()
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
        assertEquals(
            listOf(AssertionId("nonexistent")),
            evidence.getAssertionCalls,
            "genuine dereference must be attempted for the exact missing ID -- absence, not an earlier filter",
        )
    }

    @Test
    fun `a missing or deleted Entity reference resolves to no result, silently`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toEntity("nonexistent")))
        val evidence = RecordingMemoryRetrieval()
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
        assertEquals(
            listOf(EntityId("nonexistent")),
            evidence.getEntityCalls,
            "genuine dereference must be attempted for the exact missing ID -- absence, not an earlier filter",
        )
    }

    @Test
    fun `a ToDocument reference never matches any query and calls no MemoryRetrieval method`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toDocument("d1")))
        val evidence = RecordingMemoryRetrieval()
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "anything"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
    }

    @Test
    fun `a ToRelationship reference never matches any query and calls no MemoryRetrieval method`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toRelationship("r1")))
        val evidence = RecordingMemoryRetrieval()
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "anything"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
    }

    @Test
    fun `only getAssertion and getEntity are ever reachable, even when ToDocument and ToRelationship candidates exist`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("assertion-item"), toAssertion("a1")))
        persistence.store(item(KnowledgeId("entity-item"), toEntity("e1")))
        persistence.store(item(KnowledgeId("document-item"), toDocument("d1")))
        persistence.store(item(KnowledgeId("relationship-item"), toRelationship("r1")))
        val evidence = RecordingMemoryRetrieval(
            assertions = mapOf("a1" to assertionRecord("a1", "grocery list from assertion")),
            entities = mapOf("e1" to entityRecord("e1", "grocery list from entity")),
        )
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        // RecordingMemoryRetrieval throws on any call beyond getAssertion/getEntity -- reaching this
        // assertion at all, with both candidates resolved, is itself the proof no forbidden method fired.
        assertEquals(2, entries.size)
        assertEquals(listOf(AssertionId("a1")), evidence.getAssertionCalls)
        assertEquals(listOf(EntityId("e1")), evidence.getEntityCalls)
    }

    @Test
    fun `authorized-partial result -- candidate A resolves while candidate B is specifically denied`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("candidate-a"), toAssertion("a1")))
        persistence.store(item(KnowledgeId("candidate-b"), toAssertion("a2")))
        persistence.store(item(KnowledgeId("candidate-c"), toAssertion("a3")))
        val protectedStatementB = "grocery list protected-content-b-3d8f"
        val evidence = RecordingMemoryRetrieval(
            assertions = mapOf(
                "a1" to assertionRecord("a1", "grocery list from A"),
                "a3" to assertionRecord("a3", "grocery list from C"),
            ),
            // candidate B's evidence is specifically denied -- a genuine, existing, non-null Assertion
            // the accessor still withholds -- never missing/non-ACTIVE/unsupported/retired/non-matching.
            deniedAssertions = mapOf("a2" to assertionRecord("a2", protectedStatementB)),
        )
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(
            setOf(AssertionId("a1"), AssertionId("a2"), AssertionId("a3")),
            evidence.getAssertionCalls.toSet(),
            "every candidate's exact evidence ID must genuinely be requested",
        )
        assertEquals(
            listOf("grocery list from A", "grocery list from C"),
            entries.map { it.content },
            "authorized-partial: A and C survive in their own surviving insertion order, B is silently excluded, never fail-whole",
        )
        assertTrue(
            entries.none { it.content.contains(protectedStatementB) },
            "candidate B's protected content must never appear in the result, no denial detail of any kind",
        )
    }

    // --- D. Memory Core record-status gate (distinct from permission denial) ---

    @Test
    fun `a referenced ACTIVE record is included`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list", status = MemoryCoreRecordStatus.ACTIVE)))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(1, entries.size)
    }

    @Test
    fun `a referenced DISPUTED record is excluded`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list", status = MemoryCoreRecordStatus.DISPUTED)))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
    }

    @Test
    fun `a referenced SUPERSEDED record is excluded`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list", status = MemoryCoreRecordStatus.SUPERSEDED)))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
    }

    @Test
    fun `a referenced ARCHIVED record is excluded`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list", status = MemoryCoreRecordStatus.ARCHIVED)))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
    }

    @Test
    fun `a referenced DELETED record is excluded`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list", status = MemoryCoreRecordStatus.DELETED)))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(
            emptyList<SafeKnowledgeResultEntry>(),
            entries,
            "record-status exclusion is a Memory Core gate, never a permission denial -- proven here with an unconditionally approving engine",
        )
    }

    // --- E. KnowledgeItem lifecycle ---

    @Test
    fun `an ACTIVE KnowledgeItem is included by default`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1"), status = KnowledgeItemStatus.ACTIVE))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(1, entries.size)
    }

    @Test
    fun `a RETIRED KnowledgeItem is excluded by default`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1"), status = KnowledgeItemStatus.RETIRED))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(emptyList<SafeKnowledgeResultEntry>(), entries)
    }

    @Test
    fun `a RETIRED KnowledgeItem is included only when includeRetired is true`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1"), status = KnowledgeItemStatus.RETIRED))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery", includeRetired = true))

        assertEquals(1, entries.size)
    }

    // --- F. Matching and normalization ---

    @Test
    fun `CRLF in resolved content becomes a bare LF`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery line one\r\ngrocery line two")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals("grocery line one\ngrocery line two", entries.single().content)
    }

    @Test
    fun `a lone CR in resolved content becomes a bare LF`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery line one\rgrocery line two")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals("grocery line one\ngrocery line two", entries.single().content)
    }

    @Test
    fun `no trimming occurs -- leading and trailing whitespace is preserved`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "  grocery list  ")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals("  grocery list  ", entries.single().content)
    }

    @Test
    fun `no internal whitespace collapse occurs`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery    list    notes")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals("grocery    list    notes", entries.single().content)
    }

    @Test
    fun `a non-BMP Unicode character in resolved content is preserved unchanged`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "grocery list 🎉 celebration")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals("grocery list 🎉 celebration", entries.single().content)
    }

    @Test
    fun `matching remains correct under the Turkish default locale`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("tr"))
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "Team MEETING agenda")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "meeting"))

        assertEquals(
            1,
            entries.size,
            "locale-sensitive case folding would fail this match under Turkish default locale (I/İ, i/ı); " +
                "Char-level, locale-independent folding must not",
        )
    }

    @Test
    fun `generic promotion-basis text containing the query cannot match when resolved content does not`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1"), basis = "grocery shopping reminder"))
        val evidence = RecordingMemoryRetrieval(assertions = mapOf("a1" to assertionRecord("a1", "unrelated budget figures for the quarter")))
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        assertEquals(
            emptyList<SafeKnowledgeResultEntry>(),
            entries,
            "matching must use resolved content only, never KnowledgeItem.history's own generic basis text",
        )
    }

    // --- G. Ordering, bounds, staleness, fault propagation ---

    @Test
    fun `insertion order is preserved through every filter stage`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        persistence.store(item(KnowledgeId("k2"), toAssertion("a2")))
        persistence.store(item(KnowledgeId("k3"), toAssertion("a3")))
        val evidence = RecordingMemoryRetrieval(
            assertions = mapOf(
                "a1" to assertionRecord("a1", "grocery list one"),
                "a2" to assertionRecord("a2", "grocery list two"),
                "a3" to assertionRecord("a3", "grocery list three"),
            ),
        )
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery", maximumResults = 10))

        assertEquals(
            listOf("grocery list one", "grocery list two", "grocery list three"),
            entries.map { it.content },
        )
    }

    @Test
    fun `maximumResults is applied only after every authorization, visibility, and relevance filter`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("first"), toAssertion("a1")))
        persistence.store(item(KnowledgeId("denied-second"), toAssertion("a2")))
        persistence.store(item(KnowledgeId("third"), toAssertion("a3")))
        val evidence = RecordingMemoryRetrieval(
            assertions = mapOf(
                "a1" to assertionRecord("a1", "grocery list one"),
                "a2" to assertionRecord("a2", "grocery list two"),
                "a3" to assertionRecord("a3", "grocery list three"),
            ),
        )
        // item-level denies "denied-second" only -- if bounding happened before permission filtering,
        // a maximumResults of 2 could wrongly admit only "first" (from the raw, unfiltered candidate
        // list); bounding after filtering must instead admit "first" and "third".
        val subject = source(persistence, actLevelApprovingEngine("first", "third"), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery", maximumResults = 2))

        assertEquals(listOf("grocery list one", "grocery list three"), entries.map { it.content })
    }

    @Test
    fun `staleness disclosure mirrors DefaultKnowledgeRetrieval -- POSSIBLY_STALE after thirty days, INDETERMINATE otherwise`() = runTest {
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(
            item(KnowledgeId("stale"), toAssertion("a1"), occurredAt = now.minus(java.time.Duration.ofDays(31))),
        )
        persistence.store(
            item(KnowledgeId("fresh"), toAssertion("a2"), occurredAt = now.minus(java.time.Duration.ofDays(1))),
        )
        val evidence = RecordingMemoryRetrieval(
            assertions = mapOf(
                "a1" to assertionRecord("a1", "grocery list stale"),
                "a2" to assertionRecord("a2", "grocery list fresh"),
            ),
        )
        val subject = source(persistence, approvingEngine(), evidence)

        val entries = subject.recall(principal, query(relevance = "grocery"))

        val byContent = entries.associate { it.content to it.staleness }
        assertEquals(StalenessDisclosure.POSSIBLY_STALE, byContent["grocery list stale"])
        assertEquals(StalenessDisclosure.INDETERMINATE, byContent["grocery list fresh"])
    }

    @Test
    fun `a genuine persistence exception propagates unchanged, uncaught`() = runTest {
        val failure = IllegalStateException("persistence failure")
        val persistence = ThrowingKnowledgeItemPersistence(failure)
        val evidence = RecordingMemoryRetrieval()
        val subject = source(persistence, approvingEngine(), evidence)

        val thrown = assertFailsWith<IllegalStateException> { subject.recall(principal, query(relevance = "grocery")) }
        assertSame(failure, thrown)
    }

    @Test
    fun `a genuine permission-engine exception propagates unchanged, uncaught`() = runTest {
        val failure = IllegalStateException("permission engine failure")
        val persistence = InMemoryKnowledgeItemPersistence()
        val throwingEngine = FakePermissionEngine { throw failure }
        val evidence = RecordingMemoryRetrieval()
        val subject = source(persistence, throwingEngine, evidence)

        val thrown = assertFailsWith<IllegalStateException> { subject.recall(principal, query(relevance = "grocery")) }
        assertSame(failure, thrown)
    }

    @Test
    fun `a genuine evidence-retrieval exception propagates unchanged, uncaught`() = runTest {
        val failure = IllegalStateException("evidence retrieval failure")
        val persistence = InMemoryKnowledgeItemPersistence()
        persistence.store(item(KnowledgeId("k1"), toAssertion("a1")))
        val evidence = RecordingMemoryRetrieval(failure = failure)
        val subject = source(persistence, approvingEngine(), evidence)

        val thrown = assertFailsWith<IllegalStateException> { subject.recall(principal, query(relevance = "grocery")) }
        assertSame(failure, thrown)
    }

    // --- Structural ---

    @Test
    fun `the constructor accepts exactly five dependencies -- persistence, permissionEngine, evidenceMemoryRetrieval, authorizationPurpose, and clock`() {
        // Kotlin reflection, not java.lang.reflect: a defaulted parameter (clock) causes the JVM to
        // carry an additional synthetic bridge constructor, which java.lang.reflect would wrongly count.
        val constructor = DefaultReasoningKnowledgeSource::class.primaryConstructor
        assertEquals(5, constructor?.parameters?.size)
    }

    @Test
    fun `ReasoningKnowledgeSource exposes no mutation operation -- only recall`() {
        // Kotlin reflection, not java.lang.reflect: recall's PrincipalId parameter is a value class,
        // so the JVM method name is mangled (e.g. "recall-fn53kRE") -- KFunction.name is not.
        val functionNames = parker.core.interfaces.ReasoningKnowledgeSource::class.declaredFunctions.map { it.name }.toSet()
        assertEquals(setOf("recall"), functionNames)
    }
}
