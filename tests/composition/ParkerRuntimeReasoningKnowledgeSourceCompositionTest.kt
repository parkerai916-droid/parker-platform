package parker.composition

import java.io.File
import java.lang.reflect.Field
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionMappingResult
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.Assertion
import parker.core.interfaces.AssertionId
import parker.core.interfaces.AuthorizationPurposeId
import parker.core.interfaces.AuthorizationPurposeRegistrationOutcome
import parker.core.interfaces.CandidateAssertion
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ChronologicalLookupQuery
import parker.core.interfaces.ContentNature
import parker.core.interfaces.Document
import parker.core.interfaces.DocumentId
import parker.core.interfaces.DocumentLookupQuery
import parker.core.interfaces.Entity
import parker.core.interfaces.EntityId
import parker.core.interfaces.EntityLookupQuery
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.MemoryCoreRecord
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.MetadataLookupQuery
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceLookupQuery
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.ReasoningKnowledgeSource
import parker.core.interfaces.Relationship
import parker.core.interfaces.RelationshipId
import parker.core.interfaces.RelationshipTraversalQuery
import parker.core.interfaces.RequestId
import parker.core.interfaces.RequestOrigin
import parker.core.interfaces.RequestPriority
import parker.core.interfaces.ResourceType
import parker.core.interfaces.StalenessDisclosure
import parker.core.runtime.ActionMapper
import parker.core.runtime.DefaultKnowledgeSubmission
import parker.core.runtime.DefaultPermissionEngine
import parker.core.runtime.DefaultPermissionPolicy
import parker.core.runtime.DefaultReasoningContextAssembler
import parker.core.runtime.DefaultReasoningKnowledgeSource
import parker.core.runtime.DurableMemoryCore
import parker.core.runtime.InMemoryActionVocabulary
import parker.core.runtime.InMemoryAuthorizationPurposeRegistry
import parker.core.runtime.InMemoryIdentityService
import parker.core.runtime.DurableKnowledgeItemPersistence
import parker.core.runtime.InMemoryResourceRegistry
import parker.core.runtime.PermissionPolicyRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Knowledge Discoverability and Governed Retrieval into Reasoning Context, Implementation Unit 4
 * ("Composition Verification", test-only) -- see
 * `docs/governance/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_IMPLEMENTATION_PLAN.md` Section 9 for
 * the exact, binding, mandatory test set this suite implements. Every test below verifies Unit 3's own
 * already-accepted production composition (`docs/reviews/KNOWLEDGE_DISCOVERABILITY_REASONING_CONTEXT_IMPLEMENTATION_UNIT_3_COMPLETION_REVIEW.md`,
 * `..._INDEPENDENT_CONSTITUTIONAL_REVIEW.md`); none constructs or repairs it. This file creates no
 * production wiring of its own and touches no production file.
 *
 * Mirrors [ParkerRuntimeKnowledgeRetrievalCompositionTest]'s own established structure exactly --
 * `config()`/`privateField`/`composedEngine`/`composedPolicy` helpers reproduced verbatim per that
 * file's own per-file-declaration style, not shared via a common test-utility file.
 *
 * The least-authority Document-denial proof (below) is deliberately built from its own, wholly
 * independent, real production object graph -- never by reflecting into `ParkerRuntime`'s own private
 * `permissionEngine` field or its own construction-local `permissionFilteredMemoryRetrieval`/
 * `reasoningContextMemoryRetrieval` values, both of which remain genuinely inaccessible test-side, by
 * design (Implementation Plan Section 9's own explicit, binding requirement).
 */
class ParkerRuntimeReasoningKnowledgeSourceCompositionTest {

    private val ownerPrincipalId = "user.owner-reasoning-knowledge-source-composition-test"

    private fun config(): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by this suite
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-reasoning-knowledge-source-composition-test",
        evidenceStorageRootPath = Files.createTempDirectory("reasoning-knowledge-source-composition-storage").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("reasoning-knowledge-source-composition-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("reasoning-knowledge-source-composition-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("knowledge-items-test").resolve("items.log").toString(),
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    private fun composedEngine(runtime: ParkerRuntime): PermissionEngine = runtime.privateField("permissionEngine")

    private fun composedPolicy(runtime: ParkerRuntime): DefaultPermissionPolicy = composedEngine(runtime).privateField("policy")

    private fun assemblerFrom(runtime: ParkerRuntime): Any = runtime.privateField("reasoningContextAssembler")

    private fun reasoningKnowledgeSourceFrom(runtime: ParkerRuntime): ReasoningKnowledgeSource = assemblerFrom(runtime).privateField("knowledgeSource")

    /**
     * Creates a genuine, resolvable Assertion in the runtime's own real, shared Memory Core, then
     * directly stores a promoted [KnowledgeItem] referencing it in the runtime's own real, shared
     * [DurableKnowledgeItemPersistence] -- mirroring
     * [ParkerRuntimeKnowledgeRetrievalCompositionTest]'s own established "reflect to the shared
     * persistence, then store directly" convention (lines 186-208 there), extended here with a genuine
     * Memory Core record since, unlike `DefaultKnowledgeRetrieval`, [DefaultReasoningKnowledgeSource]
     * matches only against genuinely dereferenced Assertion/Entity content, never generic
     * promotion-basis text.
     */
    private suspend fun promoteRealAssertion(
        runtime: ParkerRuntime,
        owner: PrincipalId,
        knowledgeId: KnowledgeId,
        statement: String,
        status: KnowledgeItemStatus = KnowledgeItemStatus.ACTIVE,
        occurredAt: Instant = Instant.now(),
    ): AssertionId {
        val memoryCore = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator").privateField<DurableMemoryCore>("memoryCore")
        val provenance = memoryCore.createProvenance(
            owner,
            CandidateProvenance(
                sourceIdentifier = "unit-4-composition-test",
                sourceType = "test-harness",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                contentNature = ContentNature.ORIGINAL,
            ),
        )
        val assertion = memoryCore.createAssertion(
            owner,
            CandidateAssertion(statement = statement, provenanceId = provenance.provenanceId, confidence = 0.9),
        )
        val persistence = runtime.privateField<Any>("knowledgeRetrieval").privateField<DurableKnowledgeItemPersistence>("persistence")
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(assertion.assertionId)
        persistence.store(
            KnowledgeItem(
                knowledgeId = knowledgeId,
                evidenceReference = evidenceReference,
                provenanceReference = ProvenanceReference(provenance.provenanceId),
                evidentialState = EvidentialState.DIRECT_OBSERVATION,
                status = status,
                history = listOf(
                    KnowledgePromotion(
                        knowledgeId = knowledgeId,
                        evidenceReference = evidenceReference,
                        resultingState = EvidentialState.DIRECT_OBSERVATION,
                        occurredAt = occurredAt,
                        basis = "unit-4-composition-test-harness",
                    ),
                ),
            ),
        )
        return assertion.assertionId
    }

    // ================= 1. Construction =================

    @Test
    fun `the composed graph constructs successfully when ParkerRuntime starts`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        runtime.start()

        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)
        runtime.shutdown()
        assertEquals(RuntimeLifecycleState.STOPPED, runtime.state)
    }

    @Test
    fun `the constructed DefaultReasoningContextAssembler holds a genuine DefaultReasoningKnowledgeSource instance`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val reasoningKnowledgeSource = reasoningKnowledgeSourceFrom(runtime)

        assertIs<DefaultReasoningKnowledgeSource>(reasoningKnowledgeSource)

        runtime.shutdown()
    }

    // ================= 2. Shared instances =================

    @Test
    fun `the same DurableKnowledgeItemPersistence instance backs Knowledge Submission, Knowledge Retrieval, and DefaultReasoningKnowledgeSource`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val knowledgeSubmission = acceptanceCoordinator.privateField<Any>("knowledgeSubmission")
        val submissionPersistence = knowledgeSubmission.privateField<Any>("persistence")

        val knowledgeRetrieval = runtime.privateField<Any>("knowledgeRetrieval")
        val retrievalPersistence = knowledgeRetrieval.privateField<Any>("persistence")

        val reasoningPersistence = reasoningKnowledgeSourceFrom(runtime).privateField<Any>("persistence")

        assertIs<DurableKnowledgeItemPersistence>(submissionPersistence)
        assertSame(
            submissionPersistence,
            retrievalPersistence,
            "Knowledge Submission and Knowledge Retrieval must share the one, same persistence instance -- never a parallel one",
        )
        assertSame(
            retrievalPersistence,
            reasoningPersistence,
            "Knowledge Retrieval and DefaultReasoningKnowledgeSource must share the one, same persistence instance -- never a parallel one",
        )

        runtime.shutdown()
    }

    @Test
    fun `the same PermissionEngine instance backs Reasoning Context retrieval as every other gated act in this runtime`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val runtimePermissionEngine = runtime.privateField<PermissionEngine>("permissionEngine")
        val reasoningPermissionEngine = reasoningKnowledgeSourceFrom(runtime).privateField<PermissionEngine>("permissionEngine")

        assertSame(runtimePermissionEngine, reasoningPermissionEngine)

        runtime.shutdown()
    }

    // ================= 3. Positive composed retrieval =================

    @Test
    fun `a genuine promoted matching KnowledgeItem is retrieved through the composed DefaultReasoningKnowledgeSource`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val owner = PrincipalId(ownerPrincipalId)
        promoteRealAssertion(runtime, owner, KnowledgeId("unit-4-positive-k1"), "the owner's favourite programming language is Kotlin")

        val entries = reasoningKnowledgeSourceFrom(runtime).recall(
            owner,
            KnowledgeRetrievalQuery(relevance = "Kotlin", correlationId = "corr-unit-4-positive-retrieval", maximumResults = 10),
        )

        assertEquals(1, entries.size)
        assertEquals("the owner's favourite programming language is Kotlin", entries.single().content)

        runtime.shutdown()
    }

    // ================= 4. Negative authorization =================

    @Test
    fun `an unregistered principal receives emptyList() through the real composed DefaultReasoningKnowledgeSource`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        // This test does not, and may not, require an authorized act whose candidate set contains one
        // policy-approved and one policy-denied item evaluated against the real, composed
        // DefaultPermissionPolicy: no PermissionPolicyRule field, and no field
        // PermissionFilteredMemoryRetrieval.isApproved consults, varies by candidate, resource, or
        // evidence identity, so the real, composed policy structurally cannot produce a mixed per-item
        // outcome for an identical (action, resourceType, Purpose, proposedAction) tuple. Item-level
        // silent exclusion of a denied candidate is proven once, sufficiently, at Unit 2 (its own
        // DefaultReasoningKnowledgeSourceTest.kt) using a controllable FakePermissionEngine -- this
        // test independently proves only the real, composed act-level denial path.
        val entries = reasoningKnowledgeSourceFrom(runtime).recall(
            PrincipalId("principal-never-registered-unit-4"),
            KnowledgeRetrievalQuery(relevance = "anything", correlationId = "corr-unit-4-negative-authorization", maximumResults = 10),
        )

        assertEquals(emptyList(), entries)

        runtime.shutdown()
    }

    // ================= 5. Exact Purpose denial matrix =================

    @Test
    fun `every absent, wrong, inactive, unregistered, or mismatched Purpose fails closed for knowledge_retrieve_for_reasoning_context through the real composed policy`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val engine = composedEngine(runtime)
        val owner = PrincipalId(ownerPrincipalId)
        val registry = composedPolicy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")
        val retiredPurpose = AuthorizationPurposeId("test.unit-4-retired-purpose")
        assertTrue(registry.register(retiredPurpose) is AuthorizationPurposeRegistrationOutcome.Registered)
        assertTrue(registry.retire(retiredPurpose) is parker.core.interfaces.AuthorizationPurposeRetirementOutcome.Retired)
        val unregisteredPurpose = AuthorizationPurposeId("test.unit-4-never-registered-purpose")

        val purposesUnderTest = listOf(
            "absent" to null,
            "wrong/mismatched (candidate-evaluation Purpose)" to AuthorizationPurposeId("knowledge-memory.candidate-evaluation"),
            "wrong/mismatched (Evidence Intelligence Purpose)" to AuthorizationPurposeId("evidence-intelligence.input-resolution"),
            "unregistered" to unregisteredPurpose,
            "inactive/retired" to retiredPurpose,
        )

        purposesUnderTest.forEachIndexed { index, (label, purpose) ->
            val decision = engine.evaluate(
                ExecutionRequest(
                    requestId = RequestId("req-unit-4-purpose-denial-matrix-$index"),
                    principalId = owner,
                    origin = RequestOrigin.TEXT,
                    intent = "Unit 4 Purpose denial matrix check ($label)",
                    targetResources = emptyList(),
                    proposedActions = listOf(DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME),
                    priority = RequestPriority.NORMAL,
                    createdAt = Instant.now(),
                    correlationId = "corr-unit-4-purpose-denial-matrix-$index",
                    authorizationPurpose = purpose,
                ),
            )
            assertEquals(PermissionDecisionOutcome.DENIED, decision.decision, "Purpose case '$label' must fail closed for knowledge.retrieve_for_reasoning_context")
        }

        runtime.shutdown()
    }

    // ================= 6. Least authority: independent Document-denial proof =================

    /**
     * Test-only, local to this file. Records every `getDocument` call and returns [documentResult];
     * every other method throws immediately -- proof this proof's own delegate is reached only via
     * `getDocument`, mirroring `tests/composition/PermissionFilteredMemoryRetrievalTest.kt`'s own
     * established `FakeMemoryRetrieval(documentResult = ...)` convention for supplying that one record
     * only. Never a substitute for the permission-gating behaviour itself, which remains entirely the
     * real, unmodified `PermissionFilteredMemoryRetrieval`.
     */
    private class FakeDocumentOnlyMemoryRetrieval(private val documentResult: Document) : MemoryRetrieval {
        val getDocumentCalls = mutableListOf<Pair<PrincipalId, DocumentId>>()

        override suspend fun getEntity(requestingPrincipalId: PrincipalId, entityId: EntityId): Entity? =
            throw AssertionError("getEntity must never be called by this proof")

        override suspend fun getDocument(requestingPrincipalId: PrincipalId, documentId: DocumentId): Document {
            getDocumentCalls += requestingPrincipalId to documentId
            return documentResult
        }

        override suspend fun getAssertion(requestingPrincipalId: PrincipalId, assertionId: AssertionId): Assertion? =
            throw AssertionError("getAssertion must never be called by this proof")

        override suspend fun getRelationship(requestingPrincipalId: PrincipalId, relationshipId: RelationshipId): Relationship? =
            throw AssertionError("getRelationship must never be called by this proof")

        override suspend fun findEntities(query: EntityLookupQuery): List<Entity> =
            throw AssertionError("findEntities must never be called by this proof")

        override suspend fun findDocuments(query: DocumentLookupQuery): List<Document> =
            throw AssertionError("findDocuments must never be called by this proof")

        override suspend fun traverseRelationships(query: RelationshipTraversalQuery): List<Relationship> =
            throw AssertionError("traverseRelationships must never be called by this proof")

        override suspend fun findByTimeRange(query: ChronologicalLookupQuery): List<MemoryCoreRecord> =
            throw AssertionError("findByTimeRange must never be called by this proof")

        override suspend fun findByMetadata(query: MetadataLookupQuery): List<MemoryCoreRecord> =
            throw AssertionError("findByMetadata must never be called by this proof")

        override suspend fun findByProvenance(query: ProvenanceLookupQuery): List<MemoryCoreRecord> =
            throw AssertionError("findByProvenance must never be called by this proof")
    }

    @Test
    fun `direct policy and purpose-bound-view Document denial proof, built from an independently constructed real production object graph`() = runTest {
        // 1. a real InMemoryAuthorizationPurposeRegistry.
        val purposeRegistry = InMemoryAuthorizationPurposeRegistry()

        // 2. registration and activation, on that registry, of the exact frozen Purpose.
        val reasoningContextPurpose = AuthorizationPurposeId("knowledge-memory.reasoning-context-retrieval")
        assertTrue(purposeRegistry.register(reasoningContextPurpose) is AuthorizationPurposeRegistrationOutcome.Registered)
        assertTrue(purposeRegistry.isActive(reasoningContextPurpose))

        // 3. a real InMemoryActionVocabulary, with exactly one entry registered on it -- transcribing,
        // not duplicating or altering, the identical mapping already registered in ParkerRuntime.kt as
        // a pre-existing Gap #54 Memory Retrieval Operationalisation Unit 2 production registration.
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(
            ActionVocabularyEntry(
                verbPhrase = PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
                mappings = setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)),
            ),
        )
        // Mandatory precondition assertion, before either denial assertion below.
        assertEquals(
            setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)),
            vocabulary.lookup(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME)?.mappings,
            "precondition: the transcribed READ/DOCUMENT mapping must resolve before either denial assertion, or this proof would pass vacuously through UNKNOWN_ACTION",
        )

        // 4. a real ActionMapper, constructed from that same vocabulary.
        val actionMapper = ActionMapper(vocabulary)

        // 5. a real DefaultPermissionPolicy: the pre-existing Gap #54 verb-only DENIED rule for
        // memory.retrieve_document, plus the exact three new Unit 3 PermissionPolicyRule entries,
        // transcribed verbatim -- no Purpose-specific Document approval, no extra rule, no substitute
        // rule.
        val rules = listOf(
            PermissionPolicyRule(
                action = PermissionAction.READ,
                resourceType = ResourceType.DOCUMENT,
                outcome = PermissionDecisionOutcome.DENIED,
                level = PermissionLevel.AUTOMATIC,
                proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
            ),
            PermissionPolicyRule(
                action = PermissionAction.READ,
                resourceType = ResourceType.MEMORY,
                outcome = PermissionDecisionOutcome.DENIED,
                level = PermissionLevel.AUTOMATIC,
                proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
            ),
            PermissionPolicyRule(
                action = PermissionAction.READ,
                resourceType = ResourceType.MEMORY,
                outcome = PermissionDecisionOutcome.APPROVED,
                level = PermissionLevel.AUTOMATIC,
                authorizationPurpose = reasoningContextPurpose,
                proposedAction = DefaultReasoningKnowledgeSource.RETRIEVE_FOR_REASONING_CONTEXT_ACTION_NAME,
            ),
            PermissionPolicyRule(
                action = PermissionAction.READ,
                resourceType = ResourceType.MEMORY,
                outcome = PermissionDecisionOutcome.APPROVED,
                level = PermissionLevel.AUTOMATIC,
                authorizationPurpose = reasoningContextPurpose,
                proposedAction = PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME,
            ),
        )
        val policy = DefaultPermissionPolicy(
            actionMapper = actionMapper,
            resourceRegistry = InMemoryResourceRegistry(),
            rules = rules,
            authorizationPurposeRegistry = purposeRegistry,
            targetlessResourceTypesByProposedAction = mapOf(
                PermissionFilteredMemoryRetrieval.RETRIEVE_ACTION_NAME to setOf(ResourceType.MEMORY),
                PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME to setOf(ResourceType.DOCUMENT),
            ),
        )

        // 6. a real DefaultPermissionEngine, backed by a real InMemoryIdentityService, with a
        // requesting principal registered and transitioned to ACTIVE, then directly confirmed ACTIVE,
        // before either authorization assertion below.
        val identityService = InMemoryIdentityService()
        val principalId = PrincipalId("user.unit-4-document-denial-proof")
        identityService.register(
            Principal(
                principalId = principalId,
                principalType = PrincipalType.USER,
                displayName = "Unit 4 Document Denial Proof Principal",
                owner = null,
                status = PrincipalStatus.CREATED,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                lastSeenAt = Instant.parse("2026-01-01T00:00:00Z"),
            ),
        )
        identityService.updateStatus(principalId, PrincipalStatus.ACTIVE)
        assertEquals(
            PrincipalStatus.ACTIVE,
            identityService.resolve(principalId)?.status,
            "the Document-denial proof's own principal must be directly confirmed ACTIVE before either authorization assertion",
        )
        val permissionEngine = DefaultPermissionEngine(identityService, policy)

        // 7. a real PermissionFilteredMemoryRetrieval whose delegate returns a genuine, well-formed,
        // existing Document value for the requested DocumentId.
        val documentId = DocumentId("unit-4-document-denial-proof-document")
        val genuineDocument = Document(
            documentId = documentId,
            documentType = "pdf",
            locationReference = "/tmp/unit-4-document-denial-proof-document",
            provenanceId = ProvenanceId("prov-unit-4-document-denial-proof"),
            registeredAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        val delegate = FakeDocumentOnlyMemoryRetrieval(genuineDocument)
        val permissionFilteredMemoryRetrieval =
            PermissionFilteredMemoryRetrieval(delegate, permissionEngine)

        // 8. a real purpose-bound view, created through the exact production factory method.
        val purposeBoundView = permissionFilteredMemoryRetrieval.forAuthorizationPurpose(reasoningContextPurpose)

        // 9. direct getDocument(...) returns null -- the delegate's own genuine Document is fetched
        // (FakeDocumentOnlyMemoryRetrieval.getDocument always returns it, unconditionally) but its
        // content is never disclosed to the caller once permission evaluation denies it.
        val disclosedDocument = purposeBoundView.getDocument(principalId, documentId)
        assertNull(disclosedDocument, "the reasoning-context Purpose must never disclose Document content -- least authority, Contract Invariant 13")
        assertEquals(
            listOf(principalId to documentId),
            delegate.getDocumentCalls,
            "the delegate's own genuine Document must be fetched, using the exact active principal and the exact genuine Document ID, exactly once, despite the content never being disclosed",
        )

        // 10. a separate, direct policy.evaluate(...) assertion, using the same confirmed-ACTIVE
        // principal, proving DENIED. ExecutionRequest carries no resourceType field of its own -- only
        // the frozen targetless proposedActions/authorizationPurpose shape
        // PermissionFilteredMemoryRetrieval.buildExecutionRequest's own production construction
        // already supplies for this verb.
        val documentRequest = ExecutionRequest(
            requestId = RequestId("req-unit-4-document-denial-proof"),
            principalId = principalId,
            origin = RequestOrigin.AGENT,
            intent = "Unit 4 Document-denial least-authority proof",
            targetResources = emptyList(),
            proposedActions = listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME),
            priority = RequestPriority.NORMAL,
            createdAt = Instant.now(),
            correlationId = "corr-unit-4-document-denial-proof",
            authorizationPurpose = reasoningContextPurpose,
        )
        val decision = policy.evaluate(documentRequest)
        assertEquals(PermissionDecisionOutcome.DENIED, decision.decision)

        // 11. a separate, direct non-vacuity assertion -- the identical call
        // DefaultPermissionPolicy.evaluate itself makes internally for this targetless verb -- proving
        // item 10's own DENIED result is a genuine Purpose/action rule evaluation, never UNKNOWN_ACTION
        // or RESOURCE_TYPE_MISMATCH.
        val mapped = actionMapper.map(listOf(PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME), setOf(ResourceType.DOCUMENT))
        assertEquals(
            listOf(
                ActionMappingResult.Resolved(
                    PermissionFilteredMemoryRetrieval.RETRIEVE_DOCUMENT_ACTION_NAME,
                    setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT)),
                ),
            ),
            mapped,
        )
    }

    // ================= 6b. Least authority, non-regression =================

    // KNOWLEDGE_CANDIDATE_EVALUATION_PURPOSE's own existing Document-approval behaviour is unaffected.
    // Implementation Plan Section 9 requires this non-regression be confirmed "by running the existing
    // suite unchanged, not by a new assertion in this file" -- the existing candidate-evaluation
    // composition/unit tests already cover this exact behaviour, and the unchanged, passing full
    // Gradle suite (run alongside this file) is the required evidence. No test belongs here.

    // ================= 7. Evidence Intelligence non-widening =================

    @Test
    fun `Evidence Intelligence denial behaviour is unchanged, same runtime, immediately after a successful reasoning-source recall`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val owner = PrincipalId(ownerPrincipalId)
        promoteRealAssertion(runtime, owner, KnowledgeId("unit-4-evidence-intelligence-non-widening"), "the owner enjoys hiking unit 4")

        val entries = reasoningKnowledgeSourceFrom(runtime).recall(
            owner,
            KnowledgeRetrievalQuery(relevance = "hiking", correlationId = "corr-unit-4-evidence-intelligence-non-widening", maximumResults = 10),
        )
        assertEquals(1, entries.size, "precondition: the reasoning-source recall in this same runtime must genuinely succeed before the non-widening check")

        val outcome = runtime.analyseEvidence(
            PrincipalId("principal-never-registered-unit-4-ei"),
            EvidenceAnalysisRequest(analysisKind = "unit-4-non-widening-check", requestingPrincipalId = PrincipalId("principal-never-registered-unit-4-ei")),
        )
        assertIs<EvidenceIntelligenceInvocationOutcome.NotAuthorised>(outcome)

        runtime.shutdown()
    }

    // ================= 8. Lifecycle, staleness, ordering =================

    @Test
    fun `a RETIRED item is excluded by default through the composed reasoning source`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val owner = PrincipalId(ownerPrincipalId)
        promoteRealAssertion(runtime, owner, KnowledgeId("unit-4-retired-default"), "grocery retired unit 4", status = KnowledgeItemStatus.RETIRED)

        val entries = reasoningKnowledgeSourceFrom(runtime).recall(
            owner,
            KnowledgeRetrievalQuery(relevance = "grocery", correlationId = "corr-unit-4-retired-default", maximumResults = 10),
        )

        assertEquals(emptyList(), entries, "a RETIRED item must not appear in an ordinary composed-runtime query by default")

        runtime.shutdown()
    }

    @Test
    fun `a RETIRED item is included when includeRetired = true, through the composed reasoning source`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val owner = PrincipalId(ownerPrincipalId)
        promoteRealAssertion(runtime, owner, KnowledgeId("unit-4-retired-included"), "grocery retired included unit 4", status = KnowledgeItemStatus.RETIRED)

        val entries = reasoningKnowledgeSourceFrom(runtime).recall(
            owner,
            KnowledgeRetrievalQuery(relevance = "grocery", correlationId = "corr-unit-4-retired-included", maximumResults = 10, includeRetired = true),
        )

        assertEquals(1, entries.size)
        assertEquals(KnowledgeItemStatus.RETIRED, entries.single().status, "the explicit opt-in must reach the composed instance and honestly disclose the retired status")

        runtime.shutdown()
    }

    @Test
    fun `staleness disclosure is genuinely computed through the composed reasoning source, using the real system clock`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val owner = PrincipalId(ownerPrincipalId)
        promoteRealAssertion(runtime, owner, KnowledgeId("unit-4-fresh"), "grocery fresh unit 4", occurredAt = Instant.now())
        promoteRealAssertion(runtime, owner, KnowledgeId("unit-4-stale"), "grocery stale unit 4", occurredAt = Instant.now().minus(Duration.ofDays(90)))

        val entries = reasoningKnowledgeSourceFrom(runtime).recall(
            owner,
            KnowledgeRetrievalQuery(relevance = "grocery", correlationId = "corr-unit-4-staleness", maximumResults = 10),
        )

        val stalenessByContent = entries.associate { it.content to it.staleness }
        assertEquals(
            mapOf(
                "grocery fresh unit 4" to StalenessDisclosure.INDETERMINATE,
                "grocery stale unit 4" to StalenessDisclosure.POSSIBLY_STALE,
            ),
            stalenessByContent,
        )

        runtime.shutdown()
    }

    @Test
    fun `deterministic ordering is preserved through the composed reasoning source across repeated calls`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val owner = PrincipalId(ownerPrincipalId)
        promoteRealAssertion(runtime, owner, KnowledgeId("unit-4-order-k3"), "grocery third unit 4")
        promoteRealAssertion(runtime, owner, KnowledgeId("unit-4-order-k1"), "grocery first unit 4")
        promoteRealAssertion(runtime, owner, KnowledgeId("unit-4-order-k2"), "grocery second unit 4")
        val theQuery = KnowledgeRetrievalQuery(relevance = "grocery", correlationId = "corr-unit-4-ordering", maximumResults = 10)

        val first = reasoningKnowledgeSourceFrom(runtime).recall(owner, theQuery)
        val second = reasoningKnowledgeSourceFrom(runtime).recall(owner, theQuery)

        assertEquals(listOf("grocery third unit 4", "grocery first unit 4", "grocery second unit 4"), first.map { it.content })
        assertEquals(first, second, "the same query against unchanged composed state is fully repeatable")

        runtime.shutdown()
    }

    // ================= 9. Structural cutover =================

    @Test
    fun `DefaultReasoningContextAssembler's own declared field types contain no legacy KnowledgeSource -- only the new reasoning source feeds it`() {
        val fieldTypeNames = DefaultReasoningContextAssembler::class.java.declaredFields.map { it.type.name }

        assertTrue(fieldTypeNames.none { it == "parker.core.interfaces.KnowledgeSource" }, "DefaultReasoningContextAssembler: $fieldTypeNames")
        assertTrue(fieldTypeNames.any { it == "parker.core.interfaces.ReasoningKnowledgeSource" }, "DefaultReasoningContextAssembler: $fieldTypeNames")
    }

    @Test
    fun `no production code path in ParkerRuntime_kt constructs InMemoryKnowledgeStore`() {
        val productionSource = File("src/composition/ParkerRuntime.kt").readText()
        val codeLines = productionSource.lines().filter { !it.trim().startsWith("//") }

        assertTrue(codeLines.none { it.contains("InMemoryKnowledgeStore(") }, "a non-comment line in ParkerRuntime.kt still constructs InMemoryKnowledgeStore")
    }

    // ================= 10. Purpose registration =================

    @Test
    fun `REASONING_CONTEXT_RETRIEVAL_PURPOSE is registered and ACTIVE at composition time`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val registry = composedPolicy(runtime).privateField<InMemoryAuthorizationPurposeRegistry>("authorizationPurposeRegistry")

        assertTrue(registry.isActive(AuthorizationPurposeId("knowledge-memory.reasoning-context-retrieval")))

        runtime.shutdown()
    }

    // ================= 11. Non-regression =================

    @Test
    fun `Knowledge Submission's own WRITE MEMORY gate remains unaffected by the new READ MEMORY rules Unit 3 registered`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val knowledgeSubmission = acceptanceCoordinator.privateField<KnowledgeSubmission>("knowledgeSubmission")

        val decision = runtime.privateField<PermissionEngine>("permissionEngine").evaluate(
            ExecutionRequest(
                requestId = RequestId("req-unit-4-submission-non-regression"),
                principalId = principal,
                origin = RequestOrigin.REMOTE_INTERFACE,
                intent = "Unit 4 non-regression probe",
                targetResources = listOf(DefaultKnowledgeSubmission.KNOWLEDGE_SUBMISSION_RESOURCE_ID),
                proposedActions = listOf(DefaultKnowledgeSubmission.SUBMIT_ACTION_NAME),
                priority = RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "corr-unit-4-submission-non-regression",
            ),
        )

        assertEquals(PermissionDecisionOutcome.APPROVED, decision.decision)
        assertIs<DefaultKnowledgeSubmission>(knowledgeSubmission)

        runtime.shutdown()
    }
}
