package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.*

class DefaultGovernedHumanFidelityReviewRecordingServiceTest {
    @TempDir lateinit var directory: Path
    private val owner = HumanFidelityReviewFixture.reviewer
    private val clock = Clock.fixed(Instant.parse("2026-09-03T04:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `authorized exact R6 fixture records reads back and duplicates idempotently`() = runTest {
        val fixture = fixture("success")
        val service = DefaultGovernedHumanFidelityReviewRecordingService(fixture.authority, fixture.storage)
        val review = HumanFidelityReviewFixture.review()

        assertEquals(GovernedHumanFidelityReviewRecordingResult.Recorded(review.reviewId), service.record(request(review)))
        val read = assertNotNull(fixture.storage.retrieve(review.reviewId))
        assertContentEquals(HumanFidelityReviewRecordCodec.encode(review), HumanFidelityReviewRecordCodec.encode(read))
        assertEquals(2, read.discrepancyOccurrences.size)
        assertEquals(1, read.systematicPatterns.size)
        assertTrue(read.discrepancyOccurrences.all { it.severity == FidelityDiscrepancySeverity.MATERIAL })
        assertTrue(read.discrepancyOccurrences.all { it.causeAssessment.state == FidelityCauseState.UNKNOWN })
        assertEquals(setOf("Kellec"), read.discrepancyOccurrences.map { occurrence ->
            (occurrence.sourceResolution as HumanSourceResolution.ResolvedAgainstSource).assertedSourceValue
        }.toSet())

        assertEquals(GovernedHumanFidelityReviewRecordingResult.AlreadyRecorded(review.reviewId), service.record(request(review)))
        assertEquals(1, fixture.storage.listForExactTarget(review.target).size)
        assertEquals(3, fixture.audit.listForReview(review.reviewId).size)
        assertEquals(GovernedHumanFidelityReviewRecordingResult.AlreadyRecorded(review.reviewId), service.record(request(review)))
        assertEquals(3, fixture.audit.listForReview(review.reviewId).size)
    }

    @Test
    fun `authorization is observed before any storage call`() = runTest {
        val events = mutableListOf<String>()
        val fixture = fixture("ordering")
        val authority = HumanFidelityReviewRecordingPermissionEvaluator {
            events += "authority"
            HumanFidelityReviewRecordingPermissionResult.Authorized
        }
        val storage = ObservingStorage(fixture.storage) { events += it }
        val result = DefaultGovernedHumanFidelityReviewRecordingService(authority, storage)
            .record(request(HumanFidelityReviewFixture.review()))
        assertTrue(result is GovernedHumanFidelityReviewRecordingResult.Recorded)
        assertEquals(listOf("authority", "prepare", "publish", "retrieve"), events)
    }

    @Test
    fun `wrong principal purpose and target are denied with zero review and audit mutation`() = runTest {
        val cases = listOf(
            request(authority = authority().copy(principalId = PrincipalId("owner.wrong"))),
            request(authority = authority().copy(authorizationPurpose = AuthorizationPurposeId("evidence-intelligence.external-transcription"))),
            request(authority = authority().copy(target = HumanFidelityReviewFixture.target.copy(sourceSha256 = OcrSha256Digest("1".repeat(64))))),
        )
        cases.forEachIndexed { index, request ->
            val fixture = fixture("deny-$index")
            val result = DefaultGovernedHumanFidelityReviewRecordingService(fixture.authority, fixture.storage).record(request)
            assertTrue(result is GovernedHumanFidelityReviewRecordingResult.AuthorizationDenied)
            assertZeroMutation(fixture, request.review)
        }
    }

    @Test
    fun `permission denial and exception produce zero mutation`() = runTest {
        val review = HumanFidelityReviewFixture.review()
        listOf<HumanFidelityReviewRecordingPermissionEvaluator>(
            HumanFidelityReviewRecordingPermissionEvaluator {
                HumanFidelityReviewRecordingPermissionResult.Denied(HumanFidelityReviewRecordingDenialReason.PERMISSION_POLICY_DENIED)
            },
            HumanFidelityReviewRecordingPermissionEvaluator { throw IllegalStateException("authority unavailable") },
        ).forEachIndexed { index, evaluator ->
            val fixture = fixture("authority-failure-$index")
            val result = DefaultGovernedHumanFidelityReviewRecordingService(evaluator, fixture.storage).record(request(review))
            if (index == 0) assertTrue(result is GovernedHumanFidelityReviewRecordingResult.AuthorizationDenied)
            else assertEquals(failure(GovernedHumanFidelityReviewRecordingFailureReason.AUTHORITY_EVALUATION_FAILED), result)
            assertZeroMutation(fixture, review)
        }
    }

    @Test
    fun `prepare and publication failures never report success`() = runTest {
        val review = HumanFidelityReviewFixture.review()
        val fixture = fixture("operation-failures")
        val prepareFailure = FailingStorage(fixture.storage, failPrepare = true)
        assertEquals(failure(GovernedHumanFidelityReviewRecordingFailureReason.STORAGE_OPERATION_FAILED),
            DefaultGovernedHumanFidelityReviewRecordingService(allowing(), prepareFailure).record(request(review)))
        assertZeroMutation(fixture, review)

        val publishFixture = fixture("publish-failure")
        val publishFailure = FailingStorage(publishFixture.storage, failPublish = true)
        assertEquals(failure(GovernedHumanFidelityReviewRecordingFailureReason.STORAGE_OPERATION_FAILED),
            DefaultGovernedHumanFidelityReviewRecordingService(allowing(), publishFailure).record(request(review)))
        assertNull(publishFixture.storage.retrieve(review.reviewId))
        assertTrue(publishFixture.audit.listForReview(review.reviewId).none { it.eventType == HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED })
    }

    @Test
    fun `publication audit failure remains fail closed and deterministic A2 retry converges`() = runTest {
        val root = Files.createDirectories(directory.resolve("recovery/reviews"))
        val auditRoot = Files.createDirectories(directory.resolve("recovery/audit"))
        val durableAudit = FileSystemHumanFidelityGovernanceAudit(auditRoot)
        val review = HumanFidelityReviewFixture.review()
        val initiallyFailing = FileSystemHumanFidelityReviewStorage(root, FailingPublishedAudit(durableAudit), clock)
        val first = DefaultGovernedHumanFidelityReviewRecordingService(allowing(), initiallyFailing).record(request(review))
        assertEquals(failure(GovernedHumanFidelityReviewRecordingFailureReason.STORAGE_OPERATION_FAILED), first)
        assertTrue(durableAudit.listForReview(review.reviewId).none { it.eventType == HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED })

        val recovered = FileSystemHumanFidelityReviewStorage(root, durableAudit, clock)
        val second = DefaultGovernedHumanFidelityReviewRecordingService(allowing(), recovered).record(request(review))
        assertEquals(GovernedHumanFidelityReviewRecordingResult.AlreadyRecorded(review.reviewId), second)
        assertNotNull(recovered.retrieve(review.reviewId))
        assertEquals(1, recovered.listForExactTarget(review.target).size)
    }

    @Test
    fun `missing mismatched and corrupt canonical readback fail closed`() = runTest {
        val review = HumanFidelityReviewFixture.review()
        val missing = ScriptedStorage(readback = null)
        assertEquals(failure(GovernedHumanFidelityReviewRecordingFailureReason.CANONICAL_READBACK_MISSING),
            DefaultGovernedHumanFidelityReviewRecordingService(allowing(), missing).record(request(review)))

        val differentSameId = HumanFidelityReviewFixture.review(reason = "A different material discrepancy reason")
        assertEquals(review.reviewId, differentSameId.reviewId)
        val mismatch = ScriptedStorage(readback = differentSameId)
        assertEquals(failure(GovernedHumanFidelityReviewRecordingFailureReason.CANONICAL_READBACK_MISMATCH),
            DefaultGovernedHumanFidelityReviewRecordingService(allowing(), mismatch).record(request(review)))

        val corrupt = ScriptedStorage(readFailure = HumanFidelityReviewStorageException.CorruptRecord(review.reviewId, "test"))
        assertEquals(failure(GovernedHumanFidelityReviewRecordingFailureReason.STORAGE_OPERATION_FAILED),
            DefaultGovernedHumanFidelityReviewRecordingService(allowing(), corrupt).record(request(review)))
    }

    @Test
    fun `same identity different canonical content fails without rewriting published review`() = runTest {
        val fixture = fixture("conflict")
        val service = DefaultGovernedHumanFidelityReviewRecordingService(allowing(), fixture.storage)
        val original = HumanFidelityReviewFixture.review()
        val conflict = HumanFidelityReviewFixture.review(reason = "Different facts under the same deterministic review identity")
        assertTrue(service.record(request(original)) is GovernedHumanFidelityReviewRecordingResult.Recorded)
        assertEquals(failure(GovernedHumanFidelityReviewRecordingFailureReason.STORAGE_OPERATION_FAILED), service.record(request(conflict)))
        assertContentEquals(HumanFidelityReviewRecordCodec.encode(original),
            HumanFidelityReviewRecordCodec.encode(assertNotNull(fixture.storage.retrieve(original.reviewId))))
    }

    @Test
    fun `minimum service has no provider projector correction or query dependency`() {
        val fields = DefaultGovernedHumanFidelityReviewRecordingService::class.java.declaredFields.map { it.type.simpleName }.toSet()
        assertEquals(setOf("HumanFidelityReviewRecordingPermissionEvaluator", "HumanFidelityReviewStorage"), fields)
        assertEquals(setOf("record"), GovernedHumanFidelityReviewRecordingService::class.members
            .filter { it.isAbstract }.map { it.name }.toSet())
    }

    private suspend fun fixture(name: String): Fixture {
        val reviewRoot = Files.createDirectories(directory.resolve("$name/reviews"))
        val auditRoot = Files.createDirectories(directory.resolve("$name/audit"))
        val audit = FileSystemHumanFidelityGovernanceAudit(auditRoot)
        val storage = FileSystemHumanFidelityReviewStorage(reviewRoot, audit, clock)
        val registry = InMemoryAuthorizationPurposeRegistry()
        HumanFidelityReviewRecordingPermissionPolicy.registerPurpose(registry)
        val authority = HumanFidelityReviewRecordingPermissionPolicy(owner, registry, exactTargetPermissionEngine(registry), clock)
        return Fixture(storage, audit, authority, reviewRoot)
    }

    private suspend fun exactTargetPermissionEngine(registry: AuthorizationPurposeRegistry): PermissionEngine {
        val identities = InMemoryIdentityService()
        identities.register(Principal(
            owner, PrincipalType.USER, "Owner", null, PrincipalStatus.CREATED, clock.instant(), clock.instant(),
        ))
        identities.updateStatus(owner, PrincipalStatus.ACTIVE)
        val resources = InMemoryResourceRegistry()
        resources.register(Resource(
            HumanFidelityReviewRecordingPermissionPolicy.resourceIdFor(HumanFidelityReviewFixture.target),
            ResourceType.DOCUMENT, "Exact R6 human fidelity review target", owner,
            ResourceSensitivity.LEGAL, ResourceLifecycleState.AVAILABLE,
            clock.instant(), clock.instant(), "isolated-test",
        ))
        val vocabulary = InMemoryActionVocabulary()
        vocabulary.register(ActionVocabularyEntry(
            HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
            setOf(ActionResourceMapping(PermissionAction.WRITE, ResourceType.DOCUMENT)),
        ))
        val rules = listOf(PermissionPolicyRule(
            PermissionAction.WRITE, ResourceType.DOCUMENT, PermissionDecisionOutcome.APPROVED,
            PermissionLevel.HIGH_ASSURANCE, HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE,
            HumanFidelityReviewRecordingPermissionPolicy.RECORD_ACTION_NAME,
        ))
        return DefaultPermissionEngine(
            identities, DefaultPermissionPolicy(ActionMapper(vocabulary), resources, rules, registry),
        )
    }

    private fun allowing() = HumanFidelityReviewRecordingPermissionEvaluator {
        HumanFidelityReviewRecordingPermissionResult.Authorized
    }

    private fun request(
        review: HumanFidelityReviewRecord = HumanFidelityReviewFixture.review(),
        authority: HumanFidelityReviewRecordingAuthorityScope = authority(),
    ) = GovernedHumanFidelityReviewRecordingRequest(review, authority)

    private fun authority() = HumanFidelityReviewRecordingAuthorityScope(
        owner, HUMAN_FIDELITY_REVIEW_RECORDING_PURPOSE, HumanFidelityReviewFixture.target,
    )

    private suspend fun assertZeroMutation(fixture: Fixture, review: HumanFidelityReviewRecord) {
        assertTrue(fixture.storage.listForExactTarget(review.target).isEmpty())
        assertTrue(fixture.audit.listForReview(review.reviewId).isEmpty())
        assertTrue(Files.list(fixture.reviewRoot.resolve(".prepared")).use { it.findAny().isEmpty })
    }

    private fun failure(reason: GovernedHumanFidelityReviewRecordingFailureReason) =
        GovernedHumanFidelityReviewRecordingResult.Failure(reason)

    private data class Fixture(
        val storage: FileSystemHumanFidelityReviewStorage,
        val audit: FileSystemHumanFidelityGovernanceAudit,
        val authority: HumanFidelityReviewRecordingPermissionPolicy,
        val reviewRoot: Path,
    )

    private class ObservingStorage(
        private val delegate: HumanFidelityReviewStorage,
        private val observed: (String) -> Unit,
    ) : HumanFidelityReviewStorage by delegate {
        override suspend fun prepare(record: HumanFidelityReviewRecord) = delegate.prepare(record).also { observed("prepare") }
        override suspend fun publishPrepared(reviewId: HumanFidelityReviewId) = delegate.publishPrepared(reviewId).also { observed("publish") }
        override suspend fun retrieve(reviewId: HumanFidelityReviewId) = delegate.retrieve(reviewId).also { observed("retrieve") }
    }

    private class FailingStorage(
        private val delegate: HumanFidelityReviewStorage,
        private val failPrepare: Boolean = false,
        private val failPublish: Boolean = false,
    ) : HumanFidelityReviewStorage by delegate {
        override suspend fun prepare(record: HumanFidelityReviewRecord): HumanFidelityReviewPreparationResult {
            if (failPrepare) error("prepare failed")
            return delegate.prepare(record)
        }
        override suspend fun publishPrepared(reviewId: HumanFidelityReviewId): HumanFidelityReviewPublicationResult {
            if (failPublish) error("publish failed")
            return delegate.publishPrepared(reviewId)
        }
    }

    private class ScriptedStorage(
        private val readback: HumanFidelityReviewRecord? = HumanFidelityReviewFixture.review(),
        private val readFailure: RuntimeException? = null,
    ) : HumanFidelityReviewStorage {
        override suspend fun prepare(record: HumanFidelityReviewRecord) = HumanFidelityReviewPreparationResult.Prepared
        override suspend fun publishPrepared(reviewId: HumanFidelityReviewId) = HumanFidelityReviewPublicationResult.Published
        override suspend fun retrieve(reviewId: HumanFidelityReviewId): HumanFidelityReviewRecord? {
            readFailure?.let { throw it }
            return readback
        }
        override suspend fun listForExactTarget(target: HumanFidelityReviewTarget) = emptyList<HumanFidelityReviewRecord>()
    }

    private class FailingPublishedAudit(
        private val delegate: HumanFidelityGovernanceAudit,
    ) : HumanFidelityGovernanceAudit by delegate {
        override suspend fun append(record: HumanFidelityGovernanceAuditRecord): HumanFidelityGovernanceAuditAppendResult {
            if (record.eventType == HumanFidelityGovernanceAuditEventType.REVIEW_PUBLISHED) error("audit unavailable")
            return delegate.append(record)
        }
    }
}
