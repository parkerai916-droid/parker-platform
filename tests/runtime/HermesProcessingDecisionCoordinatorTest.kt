package parker.core.runtime

import java.time.Instant
import kotlin.reflect.full.declaredFunctions
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.ActionResourceMapping
import parker.core.interfaces.ActionVocabularyEntry
import parker.core.interfaces.HermesProcessingCorrection
import parker.core.interfaces.HermesProcessingFailure
import parker.core.interfaces.HermesProcessingFailureKind
import parker.core.interfaces.HermesProcessingHumanDecisionType
import parker.core.interfaces.HermesProcessingIssue
import parker.core.interfaces.HermesProcessingIssueKind
import parker.core.interfaces.HermesProcessingMethod
import parker.core.interfaces.HermesProcessingResult
import parker.core.interfaces.HermesProcessingResultRegistry
import parker.core.interfaces.HermesProcessingStatus
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.Principal
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.PrincipalStatus
import parker.core.interfaces.PrincipalType
import parker.core.interfaces.Resource
import parker.core.interfaces.ResourceLifecycleState
import parker.core.interfaces.ResourceSensitivity
import parker.core.interfaces.ResourceType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Hermes Exception Decision Backend, Task 4. Unit-level coverage of
 * [HermesProcessingDecisionCoordinator] -- the one production seam through which an Owner decision
 * is authorised and recorded. Mirrors [AgentGatewayEvidenceProjectionTest]'s own "hand-built
 * environment carrying production's own rule shape" discipline.
 */
class HermesProcessingDecisionCoordinatorTest {

    private val ownerPrincipalId = PrincipalId("user.owner-hermes-decision-coordinator-test")
    private val hermesPrincipalId = PrincipalId("agent.hermes-ingestion-operator")
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun result(sha256: String = "a".repeat(64), batchId: String = "bulk-abc", status: HermesProcessingStatus, failure: HermesProcessingFailure? = null, issues: List<HermesProcessingIssue> = emptyList()) =
        HermesProcessingResult(sha256, batchId, status, setOf(HermesProcessingMethod.OCR), issues = issues, failure = failure)

    private class Environment(
        val identityService: InMemoryIdentityService,
        val processingResults: HermesProcessingResultRegistry,
        val decisions: InMemoryHermesProcessingDecisionRegistry,
        val coordinator: HermesProcessingDecisionCoordinator,
    )

    /** Reproduces production's own rule shape exactly: the coarse (WRITE, DOCUMENT)/(READ, DOCUMENT) approvals every Owner verb in this repository already relies on -- no principal-aware rule exists anywhere in this policy mechanism. */
    private suspend fun buildEnvironment(caseDisplayNameForBatch: suspend (String) -> String? = { null }): Environment {
        val identityService = InMemoryIdentityService()
        identityService.register(Principal(ownerPrincipalId, PrincipalType.USER, "Owner", null, PrincipalStatus.CREATED, now, now))
        identityService.register(Principal(hermesPrincipalId, PrincipalType.EXTERNAL_AGENT, "Hermes", ownerPrincipalId, PrincipalStatus.CREATED, now, now))

        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(ActionVocabularyEntry(HermesProcessingDecisionCoordinator.DECISION_RECORD_ACTION_NAME, setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT))))
        vocabulary.register(ActionVocabularyEntry(HermesProcessingDecisionCoordinator.REVIEW_LIST_ACTION_NAME, setOf(ActionResourceMapping(PermissionAction.READ, ResourceType.DOCUMENT))))

        val resourceRegistry = InMemoryResourceRegistry()
        listOf(HermesProcessingDecisionCoordinator.DECISION_RESOURCE_ID, HermesProcessingDecisionCoordinator.REVIEW_RESOURCE_ID).forEach { id ->
            resourceRegistry.register(
                Resource(
                    resourceId = id, resourceType = ResourceType.DOCUMENT, displayName = id.value,
                    ownerPrincipalId = ownerPrincipalId, sensitivity = ResourceSensitivity.PUBLIC,
                    lifecycleState = ResourceLifecycleState.REGISTERED, createdAt = now, updatedAt = now, source = "test",
                ),
            )
        }
        val rules = listOf(
            PermissionPolicyRule(PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC),
            PermissionPolicyRule(PermissionAction.READ, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED, PermissionLevel.AUTOMATIC),
        )
        val policy = DefaultPermissionPolicy(ActionMapper(vocabulary), resourceRegistry, rules)
        val engine = DefaultPermissionEngine(identityService, policy)
        val processingResults = InMemoryHermesProcessingResultRegistry()
        val decisions = InMemoryHermesProcessingDecisionRegistry()
        val coordinator = HermesProcessingDecisionCoordinator(engine, processingResults, decisions, caseDisplayNameForBatch)
        return Environment(identityService, processingResults, decisions, coordinator)
    }

    @Test
    fun `an ACTIVE owner recording ACCEPT against a REVIEW_REQUIRED result succeeds and is retained`() = runTest {
        val env = buildEnvironment()
        env.identityService.updateStatus(ownerPrincipalId, PrincipalStatus.ACTIVE)
        env.processingResults.record(result(status = HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(HermesProcessingIssue(HermesProcessingIssueKind.MISSING_CONTENT, "missing"))))

        val outcome = env.coordinator.recordDecision(ownerPrincipalId, "bulk-abc", "a".repeat(64), HermesProcessingHumanDecisionType.ACCEPT, "fine", null)

        val recorded = assertIs<HermesProcessingDecisionOutcome.Recorded>(outcome)
        assertEquals(ownerPrincipalId, recorded.decision.decidedBy)
        assertEquals(HermesProcessingHumanDecisionType.ACCEPT, env.decisions.latest("bulk-abc", "a".repeat(64))?.decision)
    }

    @Test
    fun `a CREATED (not yet ACTIVE) principal is Denied, and nothing is recorded -- the same PermissionEngine gate every Owner-only entry point in this codebase relies on`() = runTest {
        val env = buildEnvironment()
        env.processingResults.record(result(status = HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(HermesProcessingIssue(HermesProcessingIssueKind.MISSING_CONTENT, "missing"))))

        val outcome = env.coordinator.recordDecision(ownerPrincipalId, "bulk-abc", "a".repeat(64), HermesProcessingHumanDecisionType.ACCEPT, null, null)

        assertIs<HermesProcessingDecisionOutcome.Denied>(outcome)
        assertEquals(null, env.decisions.latest("bulk-abc", "a".repeat(64)))
    }

    @Test
    fun `no stored HermesProcessingResult for the exact key resolves UnknownProcessingResult, and nothing is recorded`() = runTest {
        val env = buildEnvironment()
        env.identityService.updateStatus(ownerPrincipalId, PrincipalStatus.ACTIVE)

        val outcome = env.coordinator.recordDecision(ownerPrincipalId, "bulk-never-processed", "a".repeat(64), HermesProcessingHumanDecisionType.REJECT, null, null)

        assertIs<HermesProcessingDecisionOutcome.UnknownProcessingResult>(outcome)
        assertEquals(null, env.decisions.latest("bulk-never-processed", "a".repeat(64)))
    }

    @Test
    fun `ACCEPT against a FAILED result is InvalidDecision, and nothing is recorded`() = runTest {
        val env = buildEnvironment()
        env.identityService.updateStatus(ownerPrincipalId, PrincipalStatus.ACTIVE)
        env.processingResults.record(result(status = HermesProcessingStatus.FAILED, failure = HermesProcessingFailure(HermesProcessingFailureKind.ENCRYPTED_SOURCE)))

        val outcome = env.coordinator.recordDecision(ownerPrincipalId, "bulk-abc", "a".repeat(64), HermesProcessingHumanDecisionType.ACCEPT, null, null)

        assertIs<HermesProcessingDecisionOutcome.InvalidDecision>(outcome)
        assertEquals(null, env.decisions.latest("bulk-abc", "a".repeat(64)))
    }

    @Test
    fun `CORRECT naming an out-of-range issueIndex is InvalidDecision, and nothing is recorded`() = runTest {
        val env = buildEnvironment()
        env.identityService.updateStatus(ownerPrincipalId, PrincipalStatus.ACTIVE)
        env.processingResults.record(result(status = HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(HermesProcessingIssue(HermesProcessingIssueKind.MISSING_CONTENT, "missing"))))

        val outcome = env.coordinator.recordDecision(
            ownerPrincipalId, "bulk-abc", "a".repeat(64), HermesProcessingHumanDecisionType.CORRECT, null,
            HermesProcessingCorrection(3, "corrected", "reason"),
        )

        assertIs<HermesProcessingDecisionOutcome.InvalidDecision>(outcome)
        assertEquals(null, env.decisions.latest("bulk-abc", "a".repeat(64)))
    }

    @Test
    fun `CORRECT against a FAILED result with no issues is structurally InvalidDecision -- no special-cased rule needed`() = runTest {
        val env = buildEnvironment()
        env.identityService.updateStatus(ownerPrincipalId, PrincipalStatus.ACTIVE)
        env.processingResults.record(result(status = HermesProcessingStatus.FAILED, failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE)))

        val outcome = env.coordinator.recordDecision(
            ownerPrincipalId, "bulk-abc", "a".repeat(64), HermesProcessingHumanDecisionType.CORRECT, null,
            HermesProcessingCorrection(0, "corrected", "reason"),
        )

        assertIs<HermesProcessingDecisionOutcome.InvalidDecision>(outcome)
    }

    @Test
    fun `REPROCESS and REJECT are always permitted regardless of machine status`() = runTest {
        val env = buildEnvironment()
        env.identityService.updateStatus(ownerPrincipalId, PrincipalStatus.ACTIVE)
        env.processingResults.record(result(status = HermesProcessingStatus.FAILED, failure = HermesProcessingFailure(HermesProcessingFailureKind.CORRUPT_SOURCE)))

        val reprocess = env.coordinator.recordDecision(ownerPrincipalId, "bulk-abc", "a".repeat(64), HermesProcessingHumanDecisionType.REPROCESS, null, null)
        val reject = env.coordinator.recordDecision(ownerPrincipalId, "bulk-abc", "a".repeat(64), HermesProcessingHumanDecisionType.REJECT, null, null)

        assertIs<HermesProcessingDecisionOutcome.Recorded>(reprocess)
        assertIs<HermesProcessingDecisionOutcome.Recorded>(reject)
        assertEquals(2, env.decisions.history("bulk-abc", "a".repeat(64)).size, "append-only: both decisions retained, neither overwrote the other")
    }

    @Test
    fun `listPendingForOwner is Denied for a CREATED principal and Found for an ACTIVE one`() = runTest {
        val env = buildEnvironment()
        env.processingResults.record(result(status = HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(HermesProcessingIssue(HermesProcessingIssueKind.MISSING_CONTENT, "missing"))))

        val denied = env.coordinator.listPendingForOwner(ownerPrincipalId)
        env.identityService.updateStatus(ownerPrincipalId, PrincipalStatus.ACTIVE)
        val found = env.coordinator.listPendingForOwner(ownerPrincipalId)

        assertIs<HermesProcessingReviewListOutcome.Denied>(denied)
        val foundList = assertIs<HermesProcessingReviewListOutcome.Found>(found)
        assertEquals(1, foundList.items.size)
    }

    @Test
    fun `this policy mechanism has no per-principal matching capability -- an ACTIVE Hermes principal would also be approved by the coarse rule, which is exactly why production never supplies it here`() = runTest {
        val env = buildEnvironment()
        env.identityService.updateStatus(hermesPrincipalId, PrincipalStatus.ACTIVE)
        env.processingResults.record(result(status = HermesProcessingStatus.REVIEW_REQUIRED, issues = listOf(HermesProcessingIssue(HermesProcessingIssueKind.MISSING_CONTENT, "missing"))))

        // This call is only reachable in a test, by directly invoking the coordinator with
        // Hermes's own PrincipalId -- ParkerRuntime.recordHermesProcessingDecisionAsOwner (the sole
        // production caller) has no parameter through which any caller, including Hermes's own HTTP
        // surface (AgentGatewayHttpServer/AgentGatewayEvidenceProjection), could ever substitute it.
        val outcome = env.coordinator.recordDecision(hermesPrincipalId, "bulk-abc", "a".repeat(64), HermesProcessingHumanDecisionType.ACCEPT, null, null)

        assertIs<HermesProcessingDecisionOutcome.Recorded>(outcome, "documents the known, disclosed caveat DefaultOwnerEvidenceDeletionAuthority's own KDoc already establishes for this codebase's policy mechanism")
    }

    @Test
    fun `ParkerRuntime's owner-facing methods take no requestingPrincipalId parameter -- the actual structural guarantee Hermes cannot write a decision`() {
        val recordFunction = parker.composition.ParkerRuntime::class.declaredFunctions.single { it.name == "recordHermesProcessingDecisionAsOwner" }
        val reviewFunction = parker.composition.ParkerRuntime::class.declaredFunctions.single { it.name == "listHermesProcessingReviewAsOwner" }
        val recordParameterTypes = recordFunction.parameters.filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }.map { it.type.classifier }
        val reviewParameterTypes = reviewFunction.parameters.filter { it.kind == kotlin.reflect.KParameter.Kind.VALUE }.map { it.type.classifier }
        assertTrue(PrincipalId::class !in recordParameterTypes, "recordHermesProcessingDecisionAsOwner must accept no caller-supplied PrincipalId")
        assertTrue(reviewParameterTypes.isEmpty(), "listHermesProcessingReviewAsOwner must accept no parameters at all")
    }
}
