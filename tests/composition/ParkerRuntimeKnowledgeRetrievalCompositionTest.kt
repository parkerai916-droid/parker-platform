package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import kotlin.reflect.full.primaryConstructor
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.EvidentialState
import parker.core.interfaces.KnowledgeId
import parker.core.interfaces.KnowledgeItem
import parker.core.interfaces.KnowledgeItemStatus
import parker.core.interfaces.KnowledgePromotion
import parker.core.interfaces.KnowledgeRetrieval
import parker.core.interfaces.KnowledgeRetrievalDisposition
import parker.core.interfaces.KnowledgeRetrievalQuery
import parker.core.interfaces.KnowledgeSubmission
import parker.core.interfaces.MemoryCoreRecordReference
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.ProvenanceId
import parker.core.interfaces.ProvenanceReference
import parker.core.interfaces.RelevanceMechanism
import parker.core.interfaces.StalenessDisclosure
import parker.core.runtime.CommunicationConversationCoordinator
import parker.core.runtime.ConversationReplyCoordinator
import parker.core.runtime.ConversationTurnReasoningCoordinator
import parker.core.runtime.DefaultKnowledgeRetrieval
import parker.core.runtime.DurableKnowledgeItemPersistence
import parker.core.runtime.QmdRelevanceMechanism
import parker.core.runtime.QmdRelevanceMechanismConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Programme 3, Knowledge Memory, Implementation Unit 9.6 ("Runtime
 * Composition"). End-to-end tests against the real, fully-wired
 * production graph -- a real [DurableKnowledgeItemPersistence], a real
 * `DefaultPermissionEngine`][parker.core.runtime.DefaultPermissionEngine]
 * resolving this graph's own newly-registered `knowledge.retrieve`
 * convention, and the real, composed [DefaultKnowledgeRetrieval] -- not
 * fakes, mirroring [ParkerRuntimeEvidenceIntelligenceCompositionTest]'s
 * own established style exactly. This suite proves the *wiring* Unit 9.6
 * adds; it does not re-prove Units 9.1-9.5's own behaviour, already
 * covered by `DefaultKnowledgeRetrievalTest`'s own 63 tests.
 *
 * Reflection is used only where no public seam exists to observe shared
 * instance identity across the composed graph -- `ParkerRuntime` exposes
 * none of its internal composition by design, exactly as
 * [ParkerRuntimeEvidenceIntelligenceCompositionTest]'s own KDoc already
 * discloses for its own, identical reason. No public `ParkerRuntime`
 * entry point exists for retrieval (Unit 9.6 does not add one -- wiring
 * Knowledge Retrieval to Reasoning Context remains Programme 4's own,
 * separately governed act), so every test below reaches the composed
 * [KnowledgeRetrieval] instance itself via [privateField], then calls its
 * own public [KnowledgeRetrieval.retrieve] operation directly -- the same
 * "reflect to the collaborator, then call its own real public method"
 * discipline the persistence-identity and shared-engine tests in the
 * Evidence Intelligence suite already establish for other collaborators.
 */
class ParkerRuntimeKnowledgeRetrievalCompositionTest {

    private val ownerPrincipalId = "user.owner-knowledge-retrieval-composition-test"

    private fun config(): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never contacted by this suite
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-knowledge-retrieval-composition-test",
        evidenceStorageRootPath = Files.createTempDirectory("knowledge-retrieval-composition-storage").toString(),
        evidenceSourceManifestStorageRootPath = Files.createTempDirectory("knowledge-retrieval-composition-storage-manifest").toString(),
        derivativeGenerationStorageRootPath = Files.createTempDirectory("knowledge-retrieval-composition-storage-manifest-derivative-generation").toString(),
        documentIngestionAuditLogPath = Files.createTempDirectory("knowledge-retrieval-composition-storage-manifest-ingestion-audit").resolve("audit.log").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("knowledge-retrieval-composition-audit").resolve("audit.log").toString(),
        memoryCoreDurabilityLogPath = Files.createTempDirectory("knowledge-retrieval-composition-memory").resolve("memory-core.log").toString(),
        knowledgeItemDurabilityLogPath = Files.createTempDirectory("knowledge-items-test").resolve("items.log").toString(),
    )

    // Programme 3, Unit 9.7.5 (Runtime Composition Wiring). A second config() variant, additive to
    // the pre-existing one above (which every pre-existing test in this file continues to use
    // unmodified): every QMD deployment-specific field is deliberately left at its own portable
    // default (never a real node/tsx/model-cache path), because the tests below either (a) prove
    // pure composition/reflection facts that never invoke RelevanceMechanism.rank() for real, or (b)
    // deliberately point at a non-existent executable to prove fail-loud behaviour without requiring
    // a real, locally-provisioned QMD/node/tsx/model installation in this environment -- mirroring
    // this Unit's own governing task's Phase 7 instruction to prefer pure composition inspection over
    // a live subprocess wherever possible. A genuine, gated live invocation is Phase 8's own separate
    // concern (QmdRelevanceMechanismLiveAcceptanceTest.kt), not this file's.
    private fun configWithUnreachableQmdExecutable(): ParkerRuntimeConfig = config().copy(
        qmdNodeExecutablePath = "parker-unit-9-7-5-composition-test-nonexistent-node-executable",
        // A non-empty (if unused) loader argument, so ProcessBuilderQmdSubprocessInvoker's own
        // pre-flight TypeScript-loader guard does not itself fire first and mask this test's own,
        // different, intended failure (an unreachable node executable) behind an unrelated one.
        qmdTsxCliPath = "parker-unit-9-7-5-composition-test-unused-tsx-cli-path",
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    private fun item(
        knowledgeId: KnowledgeId,
        basis: String,
        occurredAt: Instant,
        status: KnowledgeItemStatus = KnowledgeItemStatus.ACTIVE,
    ): KnowledgeItem {
        val evidenceReference = MemoryCoreRecordReference.ToAssertion(
            parker.core.interfaces.AssertionId("assertion-${knowledgeId.value}"),
        )
        return KnowledgeItem(
            knowledgeId = knowledgeId,
            evidenceReference = evidenceReference,
            provenanceReference = ProvenanceReference(ProvenanceId("prov-${knowledgeId.value}")),
            evidentialState = EvidentialState.UNKNOWN,
            status = status,
            history = listOf(
                KnowledgePromotion(
                    knowledgeId = knowledgeId,
                    evidenceReference = evidenceReference,
                    resultingState = EvidentialState.UNKNOWN,
                    occurredAt = occurredAt,
                    basis = basis,
                ),
            ),
        )
    }

    private fun query(relevance: String, includeRetired: Boolean = false, maximumResults: Int = 10) = KnowledgeRetrievalQuery(
        relevance = relevance,
        correlationId = "corr-composition-test",
        maximumResults = maximumResults,
        includeRetired = includeRetired,
    )

    private fun knowledgeRetrievalFrom(runtime: ParkerRuntime): KnowledgeRetrieval =
        runtime.privateField("knowledgeRetrieval")

    private fun persistenceFrom(knowledgeRetrieval: KnowledgeRetrieval): DurableKnowledgeItemPersistence {
        val persistence = (knowledgeRetrieval as Any).privateField<Any>("persistence")
        return assertIs(persistence)
    }

    // ================= Construction and dependency injection =================

    @Test
    fun `the composed Knowledge Retrieval graph constructs successfully when ParkerRuntime starts`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        runtime.start()

        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)
        runtime.shutdown()
    }

    @Test
    fun `knowledgeRetrieval is reachable from the composed graph as a genuine DefaultKnowledgeRetrieval instance`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val knowledgeRetrieval = runtime.privateField<Any>("knowledgeRetrieval")

        assertIs<DefaultKnowledgeRetrieval>(knowledgeRetrieval)

        runtime.shutdown()
    }

    @Test
    fun `the same DurableKnowledgeItemPersistence instance backs both Knowledge Submission and Knowledge Retrieval`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val knowledgeSubmission = acceptanceCoordinator.privateField<Any>("knowledgeSubmission")
        val submissionPersistence = knowledgeSubmission.privateField<Any>("persistence")

        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val retrievalPersistence = persistenceFrom(knowledgeRetrieval)

        assertIs<DurableKnowledgeItemPersistence>(submissionPersistence)
        assertSame(
            submissionPersistence,
            retrievalPersistence,
            "the write side and the read side must share the one, same persistence instance -- never a parallel one",
        )

        runtime.shutdown()
    }

    @Test
    fun `the same PermissionEngine instance backs Knowledge Retrieval as every other gated act in this runtime`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val runtimePermissionEngine = runtime.privateField<PermissionEngine>("permissionEngine")
        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val retrievalPermissionEngine = (knowledgeRetrieval as Any).privateField<PermissionEngine>("permissionEngine")

        assertSame(runtimePermissionEngine, retrievalPermissionEngine)

        runtime.shutdown()
    }

    // ================= Retrieval available through runtime =================

    @Test
    fun `an item stored via the shared persistence is retrievable through the composed Knowledge Retrieval instance`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val persistence = persistenceFrom(knowledgeRetrieval)
        persistence.store(item(KnowledgeId("composed-k1"), basis = "grocery list", occurredAt = Instant.now()))

        val disposition = knowledgeRetrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(
            disposition,
            "a registered, active owner principal must be authorised through the newly-registered knowledge.retrieve convention",
        )
        assertEquals(1, retrieved.result.entries.size)
        assertEquals(KnowledgeId("composed-k1"), retrieved.result.entries.single().item.knowledgeId)

        runtime.shutdown()
    }

    // ================= Permission path preserved =================

    @Test
    fun `Knowledge Retrieval's permission path is genuinely evaluated -- an unregistered principal receives NotAuthorised, never Retrieved`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val persistence = persistenceFrom(knowledgeRetrieval)
        persistence.store(item(KnowledgeId("composed-k1"), basis = "grocery list", occurredAt = Instant.now()))

        val disposition = knowledgeRetrieval.retrieve(PrincipalId("principal-never-registered"), query(relevance = "grocery"))

        assertIs<KnowledgeRetrievalDisposition.NotAuthorised>(
            disposition,
            "identity resolution must genuinely run through the composed DefaultPermissionEngine -- an unregistered principal is never treated as authorised",
        )

        runtime.shutdown()
    }

    @Test
    fun `Knowledge Retrieval's own act-level and item-level gates both resolve against the real, composed policy -- not a fake`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val permissionEngine = (knowledgeRetrieval as Any).privateField<PermissionEngine>("permissionEngine")
        val persistence = persistenceFrom(knowledgeRetrieval)
        persistence.store(item(KnowledgeId("composed-k1"), basis = "grocery list", occurredAt = Instant.now()))

        val decision = permissionEngine.evaluate(
            parker.core.interfaces.ExecutionRequest(
                requestId = parker.core.interfaces.RequestId("composition-test-probe"),
                principalId = principal,
                origin = parker.core.interfaces.RequestOrigin.REMOTE_INTERFACE,
                intent = "probe",
                targetResources = listOf(DefaultKnowledgeRetrieval.KNOWLEDGE_RETRIEVAL_RESOURCE_ID),
                proposedActions = listOf(DefaultKnowledgeRetrieval.RETRIEVE_ACTION_NAME),
                priority = parker.core.interfaces.RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "composition-test-probe",
            ),
        )

        assertEquals(
            parker.core.interfaces.PermissionDecisionOutcome.APPROVED,
            decision.decision,
            "the newly-registered knowledge.retrieve convention must resolve through the real, composed DefaultPermissionPolicy",
        )

        runtime.shutdown()
    }

    // ================= Lifecycle shaping preserved (Unit 9.4) =================

    @Test
    fun `a RETIRED item is excluded by default through the composed runtime, exactly as Unit 9-4 governs`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val persistence = persistenceFrom(knowledgeRetrieval)
        persistence.store(item(KnowledgeId("composed-retired"), basis = "grocery retired", occurredAt = Instant.now(), status = KnowledgeItemStatus.RETIRED))

        val disposition = knowledgeRetrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(emptyList<Any>(), retrieved.result.entries, "a RETIRED item must not appear in an ordinary composed-runtime query by default")

        runtime.shutdown()
    }

    @Test
    fun `a RETIRED item is included when includeRetired = true, through the composed runtime`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val persistence = persistenceFrom(knowledgeRetrieval)
        persistence.store(item(KnowledgeId("composed-retired"), basis = "grocery retired", occurredAt = Instant.now(), status = KnowledgeItemStatus.RETIRED))

        val disposition = knowledgeRetrieval.retrieve(principal, query(relevance = "grocery", includeRetired = true))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        assertEquals(
            KnowledgeItemStatus.RETIRED,
            retrieved.result.entries.single().item.status,
            "the explicit opt-in must reach the composed instance and honestly disclose the retired status",
        )

        runtime.shutdown()
    }

    // ================= Staleness disclosure preserved (Unit 9.3) =================

    @Test
    fun `staleness disclosure is genuinely computed through the composed runtime, using the real system clock`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val persistence = persistenceFrom(knowledgeRetrieval)
        persistence.store(item(KnowledgeId("composed-fresh"), basis = "grocery fresh", occurredAt = Instant.now()))
        persistence.store(item(KnowledgeId("composed-stale"), basis = "grocery stale", occurredAt = Instant.now().minus(Duration.ofDays(90))))

        val disposition = knowledgeRetrieval.retrieve(principal, query(relevance = "grocery"))

        val retrieved = assertIs<KnowledgeRetrievalDisposition.Retrieved>(disposition)
        val disclosureByItem = retrieved.result.entries.associate { it.item.knowledgeId to it.staleness }
        assertEquals(
            mapOf(
                KnowledgeId("composed-fresh") to StalenessDisclosure.INDETERMINATE,
                KnowledgeId("composed-stale") to StalenessDisclosure.POSSIBLY_STALE,
            ),
            disclosureByItem,
        )

        runtime.shutdown()
    }

    // ================= Deterministic ordering preserved =================

    @Test
    fun `deterministic ordering is preserved through the composed runtime across repeated calls`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val persistence = persistenceFrom(knowledgeRetrieval)
        persistence.store(item(KnowledgeId("composed-k3"), basis = "grocery third", occurredAt = Instant.now()))
        persistence.store(item(KnowledgeId("composed-k1"), basis = "grocery first", occurredAt = Instant.now()))
        persistence.store(item(KnowledgeId("composed-k2"), basis = "grocery second", occurredAt = Instant.now()))
        val theQuery = query(relevance = "grocery")

        val first = knowledgeRetrieval.retrieve(principal, theQuery)
        val second = knowledgeRetrieval.retrieve(principal, theQuery)

        val order = assertIs<KnowledgeRetrievalDisposition.Retrieved>(first).result.entries.map { it.item.knowledgeId }
        assertEquals(listOf(KnowledgeId("composed-k3"), KnowledgeId("composed-k1"), KnowledgeId("composed-k2")), order)
        assertEquals(first, second, "the same query against unchanged composed state is fully repeatable")

        runtime.shutdown()
    }

    // ================= Regression coverage =================

    @Test
    fun `submitEvidence composition remains unchanged by this Unit's own additions`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        val outcome = runtime.submitEvidence(
            principal,
            parker.core.interfaces.CandidateEvidenceArtifact("knowledge retrieval composition regression content".toByteArray()),
            parker.core.interfaces.CandidateProvenance(
                sourceIdentifier = "knowledge-retrieval-composition-regression-source",
                sourceType = "test",
                acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
                contentNature = parker.core.interfaces.ContentNature.ORIGINAL,
            ),
            "integration-test-document",
        )

        assertIs<parker.core.runtime.EvidenceRegistrationOutcome.Registered>(
            outcome,
            "adding the new knowledge.retrieve READ/MEMORY rule must not disturb the existing evidence.accept path",
        )

        runtime.shutdown()
    }

    @Test
    fun `Knowledge Submission's own WRITE MEMORY gate remains unaffected by the new READ MEMORY rule this Unit registers`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val knowledgeSubmission = acceptanceCoordinator.privateField<KnowledgeSubmission>("knowledgeSubmission")

        val decision = runtime.privateField<PermissionEngine>("permissionEngine").evaluate(
            parker.core.interfaces.ExecutionRequest(
                requestId = parker.core.interfaces.RequestId("composition-test-submission-probe"),
                principalId = principal,
                origin = parker.core.interfaces.RequestOrigin.REMOTE_INTERFACE,
                intent = "probe",
                targetResources = listOf(parker.core.runtime.DefaultKnowledgeSubmission.KNOWLEDGE_SUBMISSION_RESOURCE_ID),
                proposedActions = listOf(parker.core.runtime.DefaultKnowledgeSubmission.SUBMIT_ACTION_NAME),
                priority = parker.core.interfaces.RequestPriority.NORMAL,
                createdAt = Instant.now(),
                correlationId = "composition-test-submission-probe",
            ),
        )

        assertEquals(parker.core.interfaces.PermissionDecisionOutcome.APPROVED, decision.decision)
        assertIs<parker.core.runtime.DefaultKnowledgeSubmission>(knowledgeSubmission)

        runtime.shutdown()
    }

    @Test
    fun `no Knowledge Retrieval dependency is reachable from the conversation coordinator chain`() {
        val knowledgeRetrievalTypeNames = setOf(
            "parker.core.interfaces.KnowledgeRetrieval",
            "parker.core.runtime.DefaultKnowledgeRetrieval",
        )
        val conversationClasses = listOf(
            ConversationReplyCoordinator::class.java,
            CommunicationConversationCoordinator::class.java,
            ConversationTurnReasoningCoordinator::class.java,
        )

        conversationClasses.forEach { conversationClass ->
            val fieldTypeNames = conversationClass.declaredFields.map { it.type.name }
            assertTrue(
                fieldTypeNames.none { it in knowledgeRetrievalTypeNames },
                "${conversationClass.name} must hold no Knowledge Retrieval dependency -- found fields: $fieldTypeNames",
            )
        }
    }

    // ================= Programme 3, Unit 9.7.5 (Runtime Composition Wiring) =================
    // Extends this file additively, per the Implementation Plan's own Section 9 Affected Files
    // table and Section 8 Unit 9.7.5 "Tests required" entry -- no pre-existing test above is
    // modified. Confirms the real, composed QmdRelevanceMechanism now backs Knowledge Retrieval
    // where, until this Unit, a temporary fail-closed placeholder did (Task C bounded correction,
    // now superseded). Prefers pure composition/reflection inspection throughout, per this Unit's
    // own governing task's Phase 7 instruction -- a genuine, gated live subprocess invocation
    // remains QmdRelevanceMechanismLiveAcceptanceTest.kt's own separate, Phase 8 concern.

    private fun relevanceMechanismFrom(knowledgeRetrieval: KnowledgeRetrieval): RelevanceMechanism =
        (knowledgeRetrieval as Any).privateField("relevanceMechanism")

    @Test
    fun `the composed Knowledge Retrieval instance is backed by a real QmdRelevanceMechanism, never the former placeholder`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val relevanceMechanism = relevanceMechanismFrom(knowledgeRetrieval)

        assertIs<QmdRelevanceMechanism>(
            relevanceMechanism,
            "ParkerRuntime must compose the real, selected QmdRelevanceMechanism -- the Unit 9.7.4 -> " +
                "Unit 9.7.5 fail-closed placeholder no longer exists anywhere in this composed graph",
        )

        runtime.shutdown()
    }

    @Test
    fun `the composed QmdRelevanceMechanismConfiguration carries exactly the frozen mechanism identity`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val relevanceMechanism = relevanceMechanismFrom(knowledgeRetrievalFrom(runtime))
        val qmdMechanism = assertIs<QmdRelevanceMechanism>(relevanceMechanism)
        val qmdConfiguration = (qmdMechanism as Any).privateField<QmdRelevanceMechanismConfiguration>("configuration")

        assertEquals("QMD", qmdConfiguration.mechanismName)
        assertEquals("2.8.3", qmdConfiguration.qmdVersion)
        assertEquals("hf:ggml-org/embeddinggemma-300M-GGUF/embeddinggemma-300M-Q8_0.gguf", qmdConfiguration.embeddingModelUri)
        assertEquals(768, qmdConfiguration.vectorDimension)
        assertEquals("cosine", qmdConfiguration.similarityMetric)
        assertEquals("1", qmdConfiguration.bridgeProtocolVersion)
        assertNull(
            qmdConfiguration.embeddingModelFileSha256,
            "no adopted governance document freezes a specific model file hash -- this Unit must not invent one",
        )

        runtime.shutdown()
    }

    @Test
    fun `deployment-specific QMD paths flow from ParkerRuntimeConfig into the composed configuration unchanged`() = runTest {
        val deploymentConfig = config().copy(
            qmdNodeExecutablePath = "composition-test-node-path",
            qmdBridgeScriptPath = "composition-test-bridge-script-path",
            qmdTsxCliPath = "composition-test-tsx-cli-path",
            qmdModelCacheDir = "composition-test-model-cache-dir",
            qmdTimeoutMillis = 77_777L,
            qmdSourceRoot = "composition-test-qmd-source-root",
        )
        val runtime = ParkerRuntime(deploymentConfig, RecordingParkerLogger())
        runtime.start()

        val relevanceMechanism = relevanceMechanismFrom(knowledgeRetrievalFrom(runtime))
        val qmdMechanism = assertIs<QmdRelevanceMechanism>(relevanceMechanism)
        val qmdConfiguration = (qmdMechanism as Any).privateField<QmdRelevanceMechanismConfiguration>("configuration")

        assertEquals("composition-test-node-path", qmdConfiguration.nodeExecutablePath)
        assertEquals("composition-test-bridge-script-path", qmdConfiguration.bridgeScriptPath)
        assertEquals(listOf("composition-test-tsx-cli-path"), qmdConfiguration.additionalNodeArguments)
        assertEquals("composition-test-model-cache-dir", qmdConfiguration.modelCacheDir)
        assertEquals(77_777L, qmdConfiguration.timeoutMillis)
        assertEquals("composition-test-qmd-source-root", qmdConfiguration.qmdSourceRoot)

        runtime.shutdown()
    }

    @Test
    fun `qmdSourceRoot left unset in ParkerRuntimeConfig composes through as null, never a guessed default`() = runTest {
        // Main-Promotion Gate / Production QMD Bridge Portability Correction
        // (this Unit's own follow-on): qmdSourceRoot must never be silently
        // inferred (e.g. from this file's own directory, or a well-known
        // developer path) -- absent in ParkerRuntimeConfig must compose
        // through as absent here too.
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val relevanceMechanism = relevanceMechanismFrom(knowledgeRetrievalFrom(runtime))
        val qmdMechanism = assertIs<QmdRelevanceMechanism>(relevanceMechanism)
        val qmdConfiguration = (qmdMechanism as Any).privateField<QmdRelevanceMechanismConfiguration>("configuration")

        assertNull(qmdConfiguration.qmdSourceRoot)

        runtime.shutdown()
    }

    @Test
    fun `no Memory Core, persistence, or PermissionEngine authority reaches QmdRelevanceMechanism through this wiring`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val relevanceMechanism = relevanceMechanismFrom(knowledgeRetrievalFrom(runtime))
        val qmdMechanism = assertIs<QmdRelevanceMechanism>(relevanceMechanism)
        val forbiddenTypeNames = setOf(
            "parker.core.interfaces.KnowledgeItemPersistence",
            "parker.core.runtime.DurableKnowledgeItemPersistence",
            "parker.core.interfaces.PermissionEngine",
            "parker.core.runtime.DefaultPermissionEngine",
            "parker.core.interfaces.MemoryRetrieval",
            "parker.core.interfaces.MemoryCore",
        )
        val declaredFieldTypeNames = QmdRelevanceMechanism::class.java.declaredFields.map { it.type.name }

        assertTrue(
            declaredFieldTypeNames.none { it in forbiddenTypeNames },
            "QmdRelevanceMechanism, as composed by this wiring, must hold no canonical-authority " +
                "dependency -- found fields: $declaredFieldTypeNames",
        )

        runtime.shutdown()
    }

    @Test
    fun `no concrete QMD type leaks into DefaultKnowledgeRetrieval's own constructor contract beyond RelevanceMechanism`() {
        // kotlin.reflect's own primaryConstructor, not raw java.lang.reflect.Class.declaredConstructors
        // -- a Kotlin class with a defaulted constructor parameter (clock, here) may carry an extra,
        // synthetic Java constructor for interop, which would make declaredConstructors.single() throw.
        // This mirrors DefaultKnowledgeRetrievalTest.kt's own established
        // "DefaultKnowledgeRetrieval::class.primaryConstructor" structural-test convention exactly --
        // no live ParkerRuntime needs to be started for this pure type-level check.
        val constructorParameterClassifiers = requireNotNull(DefaultKnowledgeRetrieval::class.primaryConstructor)
            .parameters
            .map { it.type.classifier }

        assertTrue(
            constructorParameterClassifiers.none { (it as? kotlin.reflect.KClass<*>)?.qualifiedName?.contains("Qmd") == true },
            "DefaultKnowledgeRetrieval's own constructor must remain mechanism-neutral -- it must " +
                "declare RelevanceMechanism, never QmdRelevanceMechanism concretely -- found: $constructorParameterClassifiers",
        )
        assertTrue(
            constructorParameterClassifiers.any { it == RelevanceMechanism::class },
            "found: $constructorParameterClassifiers",
        )
    }

    @Test
    fun `missing deployment configuration -- an unreachable node executable -- fails loudly through the real composed runtime, never a silent empty result`() = runTest {
        val runtime = ParkerRuntime(configWithUnreachableQmdExecutable(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val knowledgeRetrieval = knowledgeRetrievalFrom(runtime)
        val persistence = persistenceFrom(knowledgeRetrieval)
        // A non-matching basis, so structural matching finds nothing and the lawful Unit 9.7.2
        // exact-zero-structural-match fallback branch genuinely triggers RelevanceMechanism.rank().
        persistence.store(item(KnowledgeId("composed-unreachable-node"), basis = "unrelated household task", occurredAt = Instant.now()))

        val error = assertFailsWith<IllegalStateException> {
            knowledgeRetrieval.retrieve(principal, query(relevance = "grocery"))
        }
        assertTrue(
            error.message.orEmpty().contains("bridge process failed to start"),
            "a missing/invalid deployment configuration must fail loudly and diagnosably through the " +
                "real composed mechanism -- never a silently successful empty result, and never a " +
                "fallback to a different mechanism: ${error.message}",
        )

        runtime.shutdown()
    }
}
