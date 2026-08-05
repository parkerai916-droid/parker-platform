package parker.composition

import java.lang.reflect.Field
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEntity
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.CandidateProvenance
import parker.core.interfaces.ContentNature
import parker.core.interfaces.EvidenceAnalysisRequest
import parker.core.interfaces.MemoryCore
import parker.core.interfaces.MemoryRetrieval
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.RelationshipEndpoint
import parker.core.runtime.CommunicationConversationCoordinator
import parker.core.runtime.ConversationReplyCoordinator
import parker.core.runtime.ConversationTurnReasoningCoordinator
import parker.core.runtime.DefaultKnowledgeCandidateEvaluator
import parker.core.runtime.DefaultKnowledgeSubmission
import parker.core.runtime.EvidenceIntelligenceInputResolver
import parker.core.runtime.EvidenceRegistrationOutcome
import parker.core.runtime.InMemoryKnowledgeItemPersistence
import parker.core.runtime.InMemoryMemoryCore
import kotlin.reflect.full.declaredFunctions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Programme 4, Evidence Intelligence, Implementation Unit 8 ("Runtime
 * Composition and Full Verification"). End-to-end tests against the
 * real, fully-wired production graph -- a real [InMemoryMemoryCore], a
 * real [DefaultPermissionEngine][parker.core.runtime.DefaultPermissionEngine]
 * resolving the graph's own newly-registered conventions, and the real
 * `ParkerRuntime.analyseEvidence`/`submitEvidence` entry points -- not
 * fakes, mirroring [ParkerRuntimeEvidenceCustodianIntegrationTest]'s own
 * established style exactly. This suite proves the *wiring* Unit 8 adds;
 * it does not re-prove Units 1-7's own behaviour, already covered by
 * their own unit test suites.
 *
 * No live model server is used, matching
 * [ParkerRuntimeEvidenceCustodianIntegrationTest]'s own precedent --
 * [config]'s `modelEndpointUrl` is deliberately unreachable. Every test
 * below that reaches [ParkerRuntime.analyseEvidence] is therefore
 * constructed so that either (a) nothing resolves for the request, so
 * [parker.core.interfaces.EvidenceIntelligence.analyse] returns before
 * ever invoking a Reasoning Provider (`DefaultEvidenceIntelligence`'s own
 * documented "reasoning is only attempted once at least one input has
 * resolved" rule), or (b) the test deliberately expects the resulting
 * network fault to propagate.
 *
 * Reflection is used only where no public seam exists to observe shared
 * instance identity across the composed graph -- `ParkerRuntime` exposes
 * none of its internal composition by design. [privateField] filters
 * neither static nor synthetic fields itself because it looks up an
 * exact declared field by name, never by iterating and counting.
 */
class ParkerRuntimeEvidenceIntelligenceCompositionTest {

    private val ownerPrincipalId = "user.owner-evidence-intelligence-integration-test"

    private fun config(): ParkerRuntimeConfig = ParkerRuntimeConfig(
        modelEndpointUrl = "http://127.0.0.1:1/api/generate", // deliberately unreachable -- never successfully contacted
        modelName = "test-model",
        ownerPrincipalId = ownerPrincipalId,
        localTextChannelModuleId = "channel.local-text-evidence-intelligence-integration-test",
        evidenceStorageRootPath = Files.createTempDirectory("evidence-intelligence-integration-storage").toString(),
        evidenceDeletionAuditLogPath = Files.createTempDirectory("evidence-intelligence-integration-audit").resolve("audit.log").toString(),
    )

    private fun candidateProvenance() = CandidateProvenance(
        sourceIdentifier = "evidence-intelligence-integration-test-source",
        sourceType = "test",
        acquisitionTime = Instant.parse("2026-01-01T00:00:00Z"),
        contentNature = ContentNature.ORIGINAL,
    )

    private fun <T> Any.privateField(name: String): T {
        val field: Field = this::class.java.declaredFields.first { it.name == name }
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(this) as T
    }

    // ================= Construction and shared identity =================

    @Test
    fun `the complete Evidence Intelligence graph constructs successfully when ParkerRuntime starts`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        runtime.start()

        assertEquals(RuntimeLifecycleState.RUNNING, runtime.state)
        runtime.shutdown()
    }

    @Test
    fun `the same PermissionEngine instance is shared by the acceptance coordinator, the retrieval decorator, and Knowledge Submission`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val runtimePermissionEngine = runtime.privateField<PermissionEngine>("permissionEngine")
        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val acceptancePermissionEngine = acceptanceCoordinator.privateField<PermissionEngine>("permissionEngine")
        val knowledgeSubmission = acceptanceCoordinator.privateField<Any>("knowledgeSubmission")
        val knowledgeSubmissionPermissionEngine = knowledgeSubmission.privateField<PermissionEngine>("permissionEngine")
        val retrievalDecorator = retrievalDecoratorFrom(runtime)
        val retrievalPermissionEngine = retrievalDecorator.privateField<PermissionEngine>("permissionEngine")

        assertSame(runtimePermissionEngine, acceptancePermissionEngine)
        assertSame(runtimePermissionEngine, knowledgeSubmissionPermissionEngine)
        assertSame(runtimePermissionEngine, retrievalPermissionEngine)

        runtime.shutdown()
    }

    @Test
    fun `the same InMemoryMemoryCore backs both the acceptance coordinator's raw write path and the shared retrieval decorator`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val rawMemoryCore = acceptanceCoordinator.privateField<MemoryCore>("memoryCore")
        val retrievalDecorator = retrievalDecoratorFrom(runtime)
        val retrievalDelegate = retrievalDecorator.privateField<MemoryRetrieval>("delegate")

        assertIs<InMemoryMemoryCore>(rawMemoryCore, "PermissionGatedMemoryCore must never be inserted here -- this coordinator already gates its own writes")
        assertSame(rawMemoryCore, retrievalDelegate, "the acceptance coordinator's raw MemoryCore and the retrieval decorator's wrapped delegate must be the one, same instance -- never a parallel Memory Core")

        runtime.shutdown()
    }

    @Test
    fun `EvidenceIntelligenceInputResolver and DefaultKnowledgeCandidateEvaluator receive the same PermissionFilteredMemoryRetrieval instance`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val evidenceIntelligence = runtime.privateField<Any>("evidenceIntelligence")
        val inputResolver = evidenceIntelligence.privateField<Any>("inputResolver")
        val retrievalFromInputResolver = inputResolver.privateField<MemoryRetrieval>("memoryRetrieval")

        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val knowledgeSubmission = acceptanceCoordinator.privateField<Any>("knowledgeSubmission")
        val evaluator = knowledgeSubmission.privateField<Any>("evaluator")
        val retrievalFromEvaluator = evaluator.privateField<MemoryRetrieval>("memoryRetrieval")

        assertIs<PermissionFilteredMemoryRetrieval>(retrievalFromInputResolver)
        assertIs<DefaultKnowledgeCandidateEvaluator>(evaluator)
        assertSame(retrievalFromInputResolver, retrievalFromEvaluator, "one shared decorator, never one per consumer")

        runtime.shutdown()
    }

    @Test
    fun `only one InMemoryKnowledgeItemPersistence instance is reachable from the composed graph`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        val knowledgeSubmission = acceptanceCoordinator.privateField<Any>("knowledgeSubmission")
        val persistence = knowledgeSubmission.privateField<Any>("persistence")

        assertIs<InMemoryKnowledgeItemPersistence>(persistence)
        assertIs<DefaultKnowledgeSubmission>(knowledgeSubmission)

        runtime.shutdown()
    }

    private fun retrievalDecoratorFrom(runtime: ParkerRuntime): PermissionFilteredMemoryRetrieval {
        val evidenceIntelligence = runtime.privateField<Any>("evidenceIntelligence")
        val inputResolver = evidenceIntelligence.privateField<EvidenceIntelligenceInputResolver>("inputResolver")
        return inputResolver.privateField("memoryRetrieval")
    }

    // ================= Invocation denial =================

    @Test
    fun `analyseEvidence for an unregistered principal returns NotAuthorised, never Completed`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()

        val outcome = runtime.analyseEvidence(
            PrincipalId("principal-never-registered"),
            EvidenceAnalysisRequest(analysisKind = "test-analysis", requestingPrincipalId = PrincipalId("principal-never-registered")),
        )

        assertIs<EvidenceIntelligenceInvocationOutcome.NotAuthorised>(outcome)

        runtime.shutdown()
    }

    // ================= Approved invocation =================

    @Test
    fun `analyseEvidence for the registered owner with a reference that resolves to nothing is authorised and completes with an empty acceptance list`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        // EvidenceIntelligenceInputResolver.resolve requires at least one evidenceArtifactId or
        // memoryCoreReferences entry -- a request naming nothing at all is rejected outright
        // (Contract Design Section 11), a different case entirely from a named reference that
        // legitimately fails to resolve. This references a Memory Core Entity that was never
        // created, a structurally valid (non-blank) reference that lawfully resolves to null.
        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "test-analysis",
                requestingPrincipalId = principal,
                memoryCoreReferences = listOf(RelationshipEndpoint(RelationshipEndpoint.ENTITY, "entity-never-created-for-this-test")),
            ),
        )

        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(
            outcome,
            "the invocation gate (EXECUTE/DOCUMENT) must be reachable and approved for a registered, active principal",
        )
        assertTrue(
            completed.acceptanceOutcomes.isEmpty(),
            "the referenced Entity resolves to null (never created), so analyse() returns emptyList() and dispatch() returns emptyList() in turn",
        )

        runtime.shutdown()
    }

    @Test
    fun `analyseEvidence propagates a genuine Reasoning Provider fault unchanged once real input resolves`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)
        val registered = assertIs<EvidenceRegistrationOutcome.Registered>(
            runtime.submitEvidence(
                principal,
                CandidateEvidenceArtifact("evidence intelligence integration content".toByteArray()),
                candidateProvenance(),
                "integration-test-document",
            ),
        )

        assertFailsWith<Exception> {
            runtime.analyseEvidence(
                principal,
                EvidenceAnalysisRequest(
                    analysisKind = "test-analysis",
                    requestingPrincipalId = principal,
                    evidenceArtifactIds = listOf(registered.acceptedEvidenceArtifact.evidenceArtifactId),
                ),
            )
        }

        runtime.shutdown()
    }

    // ================= Retrieval fail-closed behaviour =================

    @Test
    fun `analyseEvidence's Memory Core retrieval remains fail-closed even for a record that genuinely exists in Memory Core, and never bypasses the decorator`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        // Seed a real Entity directly against the raw, reflectively-obtained MemoryCore -- test
        // setup only, deliberately bypassing every gate, mirroring how a real caller never could.
        val acceptanceCoordinator = runtime.privateField<Any>("evidenceIntelligenceAcceptanceCoordinator")
        // Reflected as the concrete InMemoryMemoryCore, not the MemoryCore interface, so this
        // fixture-setup step can call both createProvenance/createEntity (MemoryCore) and getEntity
        // (MemoryRetrieval) on the one, same underlying object.
        val rawMemoryCore = acceptanceCoordinator.privateField<InMemoryMemoryCore>("memoryCore")
        val provenance = rawMemoryCore.createProvenance(principal, candidateProvenance())
        val entity = rawMemoryCore.createEntity(
            principal,
            CandidateEntity(entityType = "person", primaryLabel = "Fail-Closed Retrieval Fixture", provenanceId = provenance.provenanceId),
        )

        // Confirmed to genuinely exist, read directly off the raw core (bypassing any gate).
        assertNotNull(rawMemoryCore.getEntity(principal, entity.entityId))

        // The shared, governed decorator denies it -- Errata 004's own disclosed, structural
        // consequence: targetResources is always empty for this check, so no registration could
        // ever let it resolve to APPROVED.
        val retrievalDecorator = retrievalDecoratorFrom(runtime)
        assertNull(retrievalDecorator.getEntity(principal, entity.entityId), "the governed decorator must deny even a record that genuinely exists")

        // The full analyseEvidence path reaches the identical conclusion -- it never bypasses the
        // decorator to consult rawMemoryCore directly, so referencing this real record analyses to
        // nothing, exactly as if it had never existed.
        val outcome = runtime.analyseEvidence(
            principal,
            EvidenceAnalysisRequest(
                analysisKind = "test-analysis",
                requestingPrincipalId = principal,
                memoryCoreReferences = listOf(RelationshipEndpoint(RelationshipEndpoint.ENTITY, entity.entityId.value)),
            ),
        )
        val completed = assertIs<EvidenceIntelligenceInvocationOutcome.Completed>(outcome)
        assertTrue(completed.acceptanceOutcomes.isEmpty())

        runtime.shutdown()
    }

    // ================= Acceptance legs (composition-level only) =================

    @Test
    fun `submitEvidence composition remains unchanged -- artifact acceptance still reaches the shared Evidence Custodian`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())
        runtime.start()
        val principal = PrincipalId(ownerPrincipalId)

        val outcome = runtime.submitEvidence(
            principal,
            CandidateEvidenceArtifact("regression check content".toByteArray()),
            candidateProvenance(),
            "integration-test-document",
        )

        assertIs<EvidenceRegistrationOutcome.Registered>(outcome)

        runtime.shutdown()
    }

    // ================= Regression safeguards =================

    @Test
    fun `no Evidence Intelligence dependency is reachable from the conversation coordinator chain`() {
        val evidenceIntelligenceTypeNames = setOf(
            "parker.core.interfaces.EvidenceIntelligence",
            "parker.core.runtime.EvidenceIntelligenceAcceptanceCoordinator",
            "parker.core.runtime.EvidenceIntelligenceInputResolver",
            "parker.core.runtime.EvidenceIntelligenceReasoningCoordinator",
        )
        val conversationClasses = listOf(
            ConversationReplyCoordinator::class.java,
            CommunicationConversationCoordinator::class.java,
            ConversationTurnReasoningCoordinator::class.java,
        )

        conversationClasses.forEach { conversationClass ->
            val fieldTypeNames = conversationClass.declaredFields.map { it.type.name }
            assertTrue(
                fieldTypeNames.none { it in evidenceIntelligenceTypeNames },
                "${conversationClass.name} must hold no Evidence Intelligence dependency -- found fields: $fieldTypeNames",
            )
        }
    }

    @Test
    fun `analyseEvidence declares exactly two parameters -- requestingPrincipalId and an already-constructed EvidenceAnalysisRequest`() {
        // Kotlin reflection, not java.lang.reflect, mirroring
        // ParkerRuntimeEvidenceCustodianIntegrationTest's own established precedent for exactly
        // this reason -- a suspend fun compiles with a trailing synthetic Continuation parameter
        // at the JVM level, which java.lang.reflect.Method.parameterCount would wrongly count.
        val function = ParkerRuntime::class.declaredFunctions.single { it.name == "analyseEvidence" }
        val valueParameterTypes = function.parameters
            .filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }
            .map { it.type.classifier }

        assertEquals(listOf(PrincipalId::class, EvidenceAnalysisRequest::class), valueParameterTypes)
    }

    @Test
    fun `analyseEvidence before start() throws NotRunning`() = runTest {
        val runtime = ParkerRuntime(config(), RecordingParkerLogger())

        assertFailsWith<ParkerRuntimeException.NotRunning> {
            runtime.analyseEvidence(
                PrincipalId(ownerPrincipalId),
                EvidenceAnalysisRequest(analysisKind = "test-analysis", requestingPrincipalId = PrincipalId(ownerPrincipalId)),
            )
        }
    }
}
