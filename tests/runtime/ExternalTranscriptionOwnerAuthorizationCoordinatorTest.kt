package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import parker.core.interfaces.*

class ExternalTranscriptionOwnerAuthorizationCoordinatorTest {
    private val owner = PrincipalId("owner.auth-test")
    private val evidenceId = EvidenceArtifactId("evidence-auth-1")
    private val otherEvidenceId = EvidenceArtifactId("evidence-auth-2")
    private val sha = "a".repeat(64)
    private val secret = "correct-horse-battery-staple"

    private class FakePermission(private val outcome: PermissionDecisionOutcome) : PermissionEngine {
        var calls = 0
        override suspend fun evaluate(request: ExecutionRequest): PermissionDecision {
            calls++
            return PermissionDecision(DecisionId("dec"), request.principalId, request.targetResources.single(), PermissionAction.EXECUTE, outcome, PermissionLevel.AUTOMATIC, Instant.EPOCH)
        }
        override suspend fun explain(decisionId: DecisionId): PermissionExplanation = error("not used")
    }

    private class FakeVerification(private val expected: String) : OwnerHighAuthorityVerification {
        var calls = 0
        override fun verify(principalId: PrincipalId, purpose: AuthorizationPurposeId, target: ResourceId, presented: OwnerVerificationCredential?): Boolean {
            calls++
            return presented != null && presented.constantTimeEquals(expected.toByteArray())
        }
    }

    private class FakeCustodian(private val manifests: Map<String, EvidenceManifestRetrievalResult>) : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult = error("not used")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult = error("not used")
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult =
            manifests[evidenceArtifactId.value] ?: EvidenceManifestRetrievalResult.NotFound(evidenceArtifactId)
    }

    private fun manifest(id: EvidenceArtifactId, sha256: String = sha) = EvidenceSourceManifest(id, sha256, 10L, "application/pdf")

    private fun coordinator(
        permission: PermissionEngine = FakePermission(PermissionDecisionOutcome.APPROVED),
        verification: OwnerHighAuthorityVerification = FakeVerification(secret),
        purposeActive: Boolean = true,
        custodian: EvidenceCustodian = FakeCustodian(mapOf(evidenceId.value to EvidenceManifestRetrievalResult.Found(manifest(evidenceId)))),
        store: FileSystemExternalTranscriptionAuthorizationStore = FileSystemExternalTranscriptionAuthorizationStore(Files.createTempDirectory("ext-transcription-auth")),
    ): ExternalTranscriptionOwnerAuthorizationCoordinator {
        return ExternalTranscriptionOwnerAuthorizationCoordinator(
            ownerPrincipalId = owner,
            evidenceCustodian = custodian,
            purposes = object : AuthorizationPurposeRegistry {
                override suspend fun register(id: AuthorizationPurposeId): AuthorizationPurposeRegistrationOutcome = error("not used")
                override suspend fun retire(id: AuthorizationPurposeId): AuthorizationPurposeRetirementOutcome = error("not used")
                override suspend fun lookup(id: AuthorizationPurposeId): AuthorizationPurposeEntry? = error("not used")
                override suspend fun isActive(id: AuthorizationPurposeId): Boolean = purposeActive
            },
            permissions = permission,
            ownerVerification = verification,
            store = store,
        )
    }

    @Test
    fun `eligible document with no prior grant reports NOT_AUTHORISED`() = runTest {
        val view = coordinator().status(evidenceId)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, view.disposition)
        assertNull(view.approvedAt)
    }

    @Test
    fun `exact owner confirmation with correct credential creates an exact-target authorization and never invokes a provider`() = runTest {
        val permission = FakePermission(PermissionDecisionOutcome.APPROVED)
        val verification = FakeVerification(secret)
        val c = coordinator(permission = permission, verification = verification)

        val view = c.authorize(evidenceId, OwnerVerificationCredential.presented(secret))

        assertEquals(ExternalTranscriptionAuthorizationDisposition.AUTHORISED, view.disposition)
        assertNotNull(view.approvedAt)
        assertEquals(1, permission.calls)
        assertEquals(1, verification.calls)
        assertEquals(true, c.isAuthorized(evidenceId))
        // No provider/mechanism dependency exists anywhere on this coordinator or its store --
        // structurally, "authorize" cannot invoke a provider.
    }

    @Test
    fun `after authorization the governed decision re-evaluation sees it authorised`() = runTest {
        val c = coordinator()
        c.authorize(evidenceId, OwnerVerificationCredential.presented(secret))
        assertEquals(true, c.isAuthorized(evidenceId))
        val status = c.status(evidenceId)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.AUTHORISED, status.disposition)
    }

    @Test
    fun `wrong evidence target -- unresolvable manifest -- fails closed`() = runTest {
        val c = coordinator(custodian = FakeCustodian(emptyMap()))
        val view = c.authorize(evidenceId, OwnerVerificationCredential.presented(secret))
        assertEquals(ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, view.disposition)
        assertEquals("SOURCE_UNAVAILABLE", view.detail)
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `wrong purpose -- not active -- fails closed`() = runTest {
        val c = coordinator(purposeActive = false)
        val view = c.authorize(evidenceId, OwnerVerificationCredential.presented(secret))
        assertEquals(ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, view.disposition)
        assertEquals("PURPOSE_NOT_ACTIVE", view.detail)
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `paired UI session alone does not bypass high-authority verification -- missing or wrong credential fails closed`() = runTest {
        val permission = FakePermission(PermissionDecisionOutcome.APPROVED)
        val c = coordinator(permission = permission, verification = FakeVerification(secret))

        val missing = c.authorize(evidenceId, null)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, missing.disposition)
        assertEquals("HIGH_AUTHORITY_VERIFICATION_FAILED", missing.detail)

        val wrong = c.authorize(evidenceId, OwnerVerificationCredential.presented("not-the-secret"))
        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, wrong.disposition)
        assertEquals("HIGH_AUTHORITY_VERIFICATION_FAILED", wrong.detail)

        // Permission engine is never even reached when verification fails -- session/permission
        // approval alone was never sufficient.
        assertEquals(0, permission.calls)
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `permission policy denial fails closed even with a correct high-authority credential`() = runTest {
        val c = coordinator(permission = FakePermission(PermissionDecisionOutcome.DENIED))
        val view = c.authorize(evidenceId, OwnerVerificationCredential.presented(secret))
        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, view.disposition)
        assertEquals("PERMISSION_POLICY_DENIED", view.detail)
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `duplicate exact authorization is idempotent -- same target same principal same purpose`() = runTest {
        val store = FileSystemExternalTranscriptionAuthorizationStore(Files.createTempDirectory("ext-transcription-auth-idem"))
        val c = coordinator(store = store)
        val first = c.authorize(evidenceId, OwnerVerificationCredential.presented(secret))
        val second = c.authorize(evidenceId, OwnerVerificationCredential.presented(secret))
        assertEquals(ExternalTranscriptionAuthorizationDisposition.AUTHORISED, first.disposition)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.AUTHORISED, second.disposition)
        assertEquals(first.approvedAt, second.approvedAt)
    }

    @Test
    fun `an authorization for one evidence target never authorises a different target`() = runTest {
        val custodian = FakeCustodian(
            mapOf(
                evidenceId.value to EvidenceManifestRetrievalResult.Found(manifest(evidenceId)),
                otherEvidenceId.value to EvidenceManifestRetrievalResult.Found(manifest(otherEvidenceId)),
            ),
        )
        val store = FileSystemExternalTranscriptionAuthorizationStore(Files.createTempDirectory("ext-transcription-auth-target"))
        val c = coordinator(custodian = custodian, store = store)
        c.authorize(evidenceId, OwnerVerificationCredential.presented(secret))
        assertEquals(true, c.isAuthorized(evidenceId))
        assertEquals(false, c.isAuthorized(otherEvidenceId))
    }

    @Test
    fun `store createOrGet is idempotent for a matching grant and conflicts on a mismatching one`() {
        val store = FileSystemExternalTranscriptionAuthorizationStore(Files.createTempDirectory("ext-transcription-store"))
        val grant = ExternalTranscriptionOwnerAuthorization(evidenceId.value, sha, owner.value, ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE.value, Instant.EPOCH)
        val created = store.createOrGet(grant)
        assert(created is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Created)
        val again = store.createOrGet(grant)
        assert(again is ExternalTranscriptionOwnerAuthorizationStoreOutcome.AlreadyExisted)
        val conflicting = grant.copy(sourceSha256 = "b".repeat(64))
        val conflict = store.createOrGet(conflicting)
        assert(conflict is ExternalTranscriptionOwnerAuthorizationStoreOutcome.Conflict)
    }
}
