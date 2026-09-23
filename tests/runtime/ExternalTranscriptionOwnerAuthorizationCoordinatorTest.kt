package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import parker.core.interfaces.*
import parker.core.runtime.FileSystemCaseGovernanceAudit
import org.junit.jupiter.api.io.TempDir

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
        override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?): EvidenceSourceSubmissionResult =
            throw UnsupportedOperationException("submitSource not supported by this fake")
    }

    private class SwitchingCustodian(var current: EvidenceSourceManifest) : EvidenceCustodian {
        override suspend fun accept(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact): EvidenceAcceptanceResult = error("not used")
        override suspend fun retrieve(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceRetrievalResult = error("not used")
        override suspend fun retrieveManifest(requestingPrincipalId: PrincipalId, evidenceArtifactId: EvidenceArtifactId): EvidenceManifestRetrievalResult =
            if (current.evidenceArtifactId == evidenceArtifactId) EvidenceManifestRetrievalResult.Found(current)
            else EvidenceManifestRetrievalResult.NotFound(evidenceArtifactId)
        override suspend fun submitSource(requestingPrincipalId: PrincipalId, candidate: CandidateEvidenceArtifact, advisorySha256: String?): EvidenceSourceSubmissionResult =
            throw UnsupportedOperationException("submitSource not supported by this fake")
    }

    private class ThrowingStore : ExternalTranscriptionOwnerAuthorizationStore {
        override fun loadIfPresent(evidenceArtifactId: String): ExternalTranscriptionOwnerAuthorization? = null
        override fun createOrGet(grant: ExternalTranscriptionOwnerAuthorization): ExternalTranscriptionOwnerAuthorizationStoreOutcome =
            error("injected authorization-store failure")
    }

    private fun manifest(id: EvidenceArtifactId, sha256: String = sha) = EvidenceSourceManifest(id, sha256, 10L, "application/pdf")

    private fun coordinator(
        permission: PermissionEngine = FakePermission(PermissionDecisionOutcome.APPROVED),
        verification: OwnerHighAuthorityVerification = FakeVerification(secret),
        purposeActive: Boolean = true,
        custodian: EvidenceCustodian = FakeCustodian(mapOf(evidenceId.value to EvidenceManifestRetrievalResult.Found(manifest(evidenceId)))),
        store: ExternalTranscriptionOwnerAuthorizationStore = FileSystemExternalTranscriptionAuthorizationStore(Files.createTempDirectory("ext-transcription-auth")),
        auditReader: CaseGovernanceAuditReader? = null,
        standingPolicy: FileSystemStandingExternalTranscriptionPolicyStore? = null,
        governanceAudit: CaseGovernanceAudit? = null,
        proofStore: InMemoryOwnerUnlockProofStore? = null,
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
            auditReader = auditReader,
            standingPolicy = standingPolicy,
            governanceAudit = governanceAudit,
            ownerUnlockProofStore = proofStore,
        )
    }

    private fun proofScope(principalId: PrincipalId = owner, id: EvidenceArtifactId = evidenceId, sourceSha: String = sha) =
        OwnerUnlockProofScope(
            principalId = principalId,
            evidenceArtifactId = id,
            sourceSha256 = OcrSha256Digest(sourceSha),
            authorizationPurpose = OWNER_UNLOCK_EXTERNAL_TRANSCRIPTION_PURPOSE,
            operation = OwnerUnlockProofOperation.EXTERNAL_TRANSCRIPTION_AUTHORIZATION,
        )

    private fun issuedProof(store: InMemoryOwnerUnlockProofStore, scope: OwnerUnlockProofScope = proofScope()): OwnerUnlockProofId {
        val result = store.issue(OwnerPinVerificationResult.VERIFIED, scope)
        return (result as OwnerUnlockProofIssueResult.Issued).proof.id
    }

    @Test
    fun `eligible document with no prior grant reports NOT_AUTHORISED`() = runTest {
        val view = coordinator().status(evidenceId)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, view.disposition)
        assertNull(view.approvedAt)
    }

    // UI-INGESTION-5D regression: the read-only status/gate paths -- the ones "Process document"
    // and its governed re-evaluation actually call -- must never themselves invoke high-authority
    // verification. Only an explicit authorize() (an owner-submitted credential) may. If this ever
    // regresses, a bare "Process document" click would again be able to reach
    // HIGH_AUTHORITY_VERIFICATION_FAILED before any credential was ever presented.
    @Test
    fun `status and isAuthorized never invoke high-authority verification -- only an explicit authorize call may`() = runTest {
        val verification = FakeVerification(secret)
        val c = coordinator(verification = verification)

        c.status(evidenceId)
        assertEquals(false, c.isAuthorized(evidenceId))
        assertEquals(0, verification.calls, "a read-only status/gate check must never call the verifier")

        c.authorize(evidenceId, OwnerVerificationCredential.presented(secret))
        assertEquals(1, verification.calls, "only the explicit authorize() call may invoke the verifier")
    }

    // UI-INGESTION-5D regression: "Process document" (governed decision evaluation) for a document
    // that has never had an authorization attempt must present the plain "not yet authorised"
    // state -- never the string HIGH_AUTHORITY_VERIFICATION_FAILED, which is meaningful only after
    // an owner has actually submitted a credential that failed to verify.
    @Test
    fun `an evidence target with no authorization attempt never reports HIGH_AUTHORITY_VERIFICATION_FAILED`() = runTest {
        val c = coordinator()
        val status = c.status(evidenceId)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, status.disposition)
        assertNull(status.detail)
        assertEquals(false, c.isAuthorized(evidenceId))
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
    fun `verified Owner PIN proof converges on the exact authorization coordinator`() = runTest {
        val proofStore = InMemoryOwnerUnlockProofStore()
        val c = coordinator(proofStore = proofStore)
        val proofId = issuedProof(proofStore)

        val view = c.authorizeWithUnlockProof(evidenceId, proofId)

        assertEquals(ExternalTranscriptionAuthorizationDisposition.AUTHORISED, view.disposition)
        assertEquals(true, c.isAuthorized(evidenceId))
        assertEquals(OwnerUnlockProofConsumeResult.REPLAY_REJECTED, proofStore.consume(proofId, proofScope()))
    }

    @Test
    fun `proof-backed authorization consumes before createOrGet failure and cannot be retried`() = runTest {
        val proofStore = InMemoryOwnerUnlockProofStore()
        val c = coordinator(store = ThrowingStore(), proofStore = proofStore)
        val proofId = issuedProof(proofStore)

        val failed = c.authorizeWithUnlockProof(evidenceId, proofId)

        assertEquals(ExternalTranscriptionAuthorizationDisposition.UNAVAILABLE, failed.disposition)
        assertEquals("AUTHORIZATION_STORE_UNAVAILABLE", failed.detail)
        assertEquals(OwnerUnlockProofConsumeResult.REPLAY_REJECTED, proofStore.consume(proofId, proofScope()))
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `proof is bound to the current trusted source SHA and burns when source changes`() = runTest {
        val proofStore = InMemoryOwnerUnlockProofStore()
        val custodian = SwitchingCustodian(manifest(evidenceId, sha))
        val c = coordinator(custodian = custodian, proofStore = proofStore)
        val proofId = issuedProof(proofStore)
        custodian.current = manifest(evidenceId, "b".repeat(64))

        val view = c.authorizeWithUnlockProof(evidenceId, proofId)

        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, view.disposition)
        assertEquals("PROOF_INVALID", view.detail)
        assertEquals(OwnerUnlockProofConsumeResult.REPLAY_REJECTED, proofStore.consume(proofId, proofScope()))
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `proof rejects wrong evidence and principal without creating authorization`() = runTest {
        val proofStore = InMemoryOwnerUnlockProofStore()
        val c = coordinator(proofStore = proofStore)
        val wrongEvidence = issuedProof(proofStore, proofScope(id = otherEvidenceId))

        val evidenceView = c.authorizeWithUnlockProof(evidenceId, wrongEvidence)

        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, evidenceView.disposition)
        assertEquals("PROOF_INVALID", evidenceView.detail)
        assertEquals(false, c.isAuthorized(evidenceId))

        val wrongPrincipal = issuedProof(proofStore, proofScope(principalId = PrincipalId("another-owner")))
        val principalView = c.authorizeWithUnlockProof(evidenceId, wrongPrincipal)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, principalView.disposition)
        assertEquals("PROOF_INVALID", principalView.detail)
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `permission denial happens before proof consumption and does not create authorization`() = runTest {
        val proofStore = InMemoryOwnerUnlockProofStore()
        val c = coordinator(
            permission = FakePermission(PermissionDecisionOutcome.DENIED),
            proofStore = proofStore,
        )
        val proofId = issuedProof(proofStore)

        val view = c.authorizeWithUnlockProof(evidenceId, proofId)

        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, view.disposition)
        assertEquals("PERMISSION_POLICY_DENIED", view.detail)
        assertEquals(OwnerUnlockProofConsumeResult.CONSUMED, proofStore.consume(proofId, proofScope()))
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `expired and replayed proofs fail closed while exact existing authorization remains idempotent`() = runTest {
        val proofStore = InMemoryOwnerUnlockProofStore()
        val c = coordinator(proofStore = proofStore)
        val firstProof = issuedProof(proofStore)
        val first = c.authorizeWithUnlockProof(evidenceId, firstProof)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.AUTHORISED, first.disposition)

        val replay = c.authorizeWithUnlockProof(evidenceId, firstProof)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.NOT_AUTHORISED, replay.disposition)
        assertEquals("PROOF_REPLAYED", replay.detail)

        val secondProof = issuedProof(proofStore)
        val second = c.authorizeWithUnlockProof(evidenceId, secondProof)
        assertEquals(ExternalTranscriptionAuthorizationDisposition.AUTHORISED, second.disposition)
        assertEquals(first.approvedAt, second.approvedAt)
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

    @Test
    fun `batch authority uses the same high-authority verifier and derives the normal evidence grant idempotently`() = runTest {
        val verification = FakeVerification(secret)
        val store = FileSystemExternalTranscriptionAuthorizationStore(Files.createTempDirectory("ext-transcription-batch"))
        val c = coordinator(store = store, verification = verification)
        assertEquals(
            ExternalTranscriptionBatchAuthorizationOutcome.Authorised,
            c.authorizeBatch("bulk-authorized", CaseId("case-authorized"), OwnerVerificationCredential.presented(secret)),
        )
        assertEquals(
            ExternalTranscriptionBatchAuthorizationOutcome.Rejected("HIGH_AUTHORITY_VERIFICATION_FAILED"),
            c.authorizeBatch("bulk-authorized", CaseId("case-authorized"), OwnerVerificationCredential.presented("wrong")),
        )
        assertTrue(c.deriveEvidenceAuthorizationFromOwnerAuthorisedBatch(evidenceId))
        assertTrue(c.deriveEvidenceAuthorizationFromOwnerAuthorisedBatch(evidenceId))
        assertTrue(c.isAuthorized(evidenceId))
    }

    @Test
    fun `derivation audit failure occurs before grant persistence`() = runTest {
        val c = coordinator()
        assertFailsWith<IllegalStateException> {
            c.deriveEvidenceAuthorizationFromOwnerAuthorisedBatch(evidenceId) {
                error("injected derivation audit failure")
            }
        }
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `authorization-store failure after derivation preparation has no completed derivation audit`() = runTest {
        val auditFile = Files.createTempDirectory("ext-transcription-audit").resolve("audit.log")
        val audit = FileSystemCaseGovernanceAudit(auditFile)
        val policyRoot = Files.createTempDirectory("ext-transcription-policy")
        val policy = FileSystemStandingExternalTranscriptionPolicyStore(policyRoot, audit)
        val c = coordinator(store = ThrowingStore(), auditReader = audit, standingPolicy = policy, governanceAudit = audit)
        assertTrue(c.establishStandingExternalTranscriptionPolicy(OwnerVerificationCredential.presented(secret)))
        assertFailsWith<IllegalStateException> {
            c.deriveEvidenceAuthorizationFromOwnerAuthorisedBatch(
                "bulk-deadbeef", evidenceId,
                beforePersist = { audit.record(CaseGovernanceAuditRecord(
                    CaseGovernanceAuditEventType.INGESTION_BATCH_EXTERNAL_TRANSCRIPTION_DERIVATION_PREPARED,
                    CaseId("case-derived"), evidenceArtifactId = evidenceId, actorPrincipalId = owner,
                    recordedAt = Instant.EPOCH, batchId = "bulk-deadbeef", externalTranscriptionAuthorised = true,
                    authorizationPurpose = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE.value,
                    policyVersion = STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION,
                )) },
            )
        }
        assertTrue(Files.readAllLines(auditFile).none { it.contains("INGESTION_BATCH_EXTERNAL_TRANSCRIPTION_DERIVED") })
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `derived grant remains unusable when completion audit fails after grant persistence`(@TempDir directory: java.nio.file.Path) = runTest {
        val auditFile = directory.resolve("audit.log")
        val backing = FileSystemCaseGovernanceAudit(auditFile)
        val failingAudit = object : CaseGovernanceAudit {
            override suspend fun record(record: CaseGovernanceAuditRecord) {
                if (record.eventType == CaseGovernanceAuditEventType.INGESTION_BATCH_EXTERNAL_TRANSCRIPTION_DERIVED) {
                    error("injected completion audit failure")
                }
                backing.record(record)
            }
        }
        val policyRoot = directory.resolve("policy").also { Files.createDirectories(it) }
        val policy = FileSystemStandingExternalTranscriptionPolicyStore(policyRoot, backing)
        val c = coordinator(store = FileSystemExternalTranscriptionAuthorizationStore(directory.resolve("grants").also { Files.createDirectories(it) }), auditReader = backing, standingPolicy = policy, governanceAudit = backing)
        assertTrue(c.establishStandingExternalTranscriptionPolicy(OwnerVerificationCredential.presented(secret)))
        assertFailsWith<IllegalStateException> {
            c.deriveEvidenceAuthorizationFromOwnerAuthorisedBatch("bulk-cafebabe", evidenceId,
                beforePersist = { failingAudit.record(CaseGovernanceAuditRecord(
                    CaseGovernanceAuditEventType.INGESTION_BATCH_EXTERNAL_TRANSCRIPTION_DERIVATION_PREPARED,
                    CaseId("case-derived"), evidenceArtifactId = evidenceId, actorPrincipalId = owner,
                    recordedAt = Instant.EPOCH, batchId = "bulk-cafebabe", externalTranscriptionAuthorised = true,
                    authorizationPurpose = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE.value,
                    policyVersion = STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION,
                )) },
                afterPersist = { failingAudit.record(CaseGovernanceAuditRecord(
                    CaseGovernanceAuditEventType.INGESTION_BATCH_EXTERNAL_TRANSCRIPTION_DERIVED,
                    CaseId("case-derived"), evidenceArtifactId = evidenceId, actorPrincipalId = owner,
                    recordedAt = Instant.EPOCH, batchId = "bulk-cafebabe", externalTranscriptionAuthorised = true,
                    authorizationPurpose = ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE.value,
                    policyVersion = STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION,
                )) },
            )
        }
        assertEquals(false, c.isAuthorized(evidenceId))
    }

    @Test
    fun `standing policy is explicit durable owner authority and gates batch derivation`(@TempDir directory: java.nio.file.Path) = runTest {
        val auditFile = directory.resolve("case-audit.log")
        val audit = FileSystemCaseGovernanceAudit(auditFile)
        Files.createDirectories(directory.resolve("policy"))
        val policyStore = FileSystemStandingExternalTranscriptionPolicyStore(directory.resolve("policy"), audit)
        val c = coordinator(auditReader = audit, standingPolicy = policyStore, governanceAudit = audit)
        assertEquals(false, c.deriveEvidenceAuthorizationFromOwnerAuthorisedBatch("bulk-deadbeef", evidenceId))
        assertTrue(c.establishStandingExternalTranscriptionPolicy(OwnerVerificationCredential.presented(secret)))
        assertNotNull(FileSystemStandingExternalTranscriptionPolicyStore(directory.resolve("policy"), FileSystemCaseGovernanceAudit(auditFile)).load())
        val restartedPolicy = FileSystemStandingExternalTranscriptionPolicyStore(directory.resolve("policy"), FileSystemCaseGovernanceAudit(auditFile))
        val restarted = coordinator(auditReader = audit, standingPolicy = restartedPolicy, governanceAudit = audit)
        assertTrue(restarted.deriveEvidenceAuthorizationFromOwnerAuthorisedBatch(
            "bulk-deadbeef", evidenceId,
            beforePersist = {},
            afterPersist = { grant -> audit.record(CaseGovernanceAuditRecord(
                CaseGovernanceAuditEventType.INGESTION_BATCH_EXTERNAL_TRANSCRIPTION_DERIVED,
                CaseId("case-derived"), evidenceArtifactId = evidenceId, actorPrincipalId = owner,
                recordedAt = Instant.EPOCH, batchId = "bulk-deadbeef", externalTranscriptionAuthorised = true,
                authorizationPurpose = grant.purpose, policyVersion = STANDING_EXTERNAL_TRANSCRIPTION_POLICY_VERSION,
            )) },
        ))
    }

    @Test
    fun `standing policy establishment is idempotent for the exact completed policy`(@TempDir directory: java.nio.file.Path) = runTest {
        val auditFile = directory.resolve("audit.log")
        val audit = FileSystemCaseGovernanceAudit(auditFile)
        val store = FileSystemStandingExternalTranscriptionPolicyStore(directory.resolve("policy").also { Files.createDirectories(it) }, audit)
        val policy = StandingExternalTranscriptionPolicy(owner, ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE, Instant.parse("2026-01-01T00:00:00Z"))

        assertTrue(store.establish(policy, audit))
        assertTrue(store.establish(policy.copy(approvedAt = Instant.parse("2026-02-01T00:00:00Z")), audit))
        assertEquals(policy, store.load())
        assertEquals(1, Files.readAllLines(auditFile).count { it.contains("eventType=STANDING_EXTERNAL_TRANSCRIPTION_POLICY_AUTHORISED") })
    }

    @Test
    fun `owner retry repairs an active policy whose completion audit was interrupted without replacing its identity`(@TempDir directory: java.nio.file.Path) = runTest {
        val auditFile = directory.resolve("audit.log")
        val backing = FileSystemCaseGovernanceAudit(auditFile)
        val failing = object : CaseGovernanceAudit {
            var failCompletion = true
            override suspend fun record(record: CaseGovernanceAuditRecord) {
                if (record.eventType == CaseGovernanceAuditEventType.STANDING_EXTERNAL_TRANSCRIPTION_POLICY_AUTHORISED && failCompletion) {
                    failCompletion = false
                    error("injected completion-audit failure")
                }
                backing.record(record)
            }
        }
        val store = FileSystemStandingExternalTranscriptionPolicyStore(directory.resolve("policy").also { Files.createDirectories(it) }, backing)
        val original = StandingExternalTranscriptionPolicy(owner, ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE, Instant.parse("2026-03-01T00:00:00Z"))

        assertFailsWith<IllegalStateException> { store.establish(original, failing) }
        assertNull(store.load())
        val retry = original.copy(approvedAt = Instant.parse("2026-04-01T00:00:00Z"))
        assertTrue(store.establish(retry, backing))
        assertEquals(original, store.load())
        assertEquals(original.policyId, store.load()?.policyId)
    }

    @Test
    fun `incompatible standing policy cannot replace an existing active policy`(@TempDir directory: java.nio.file.Path) = runTest {
        val audit = FileSystemCaseGovernanceAudit(directory.resolve("audit.log"))
        val store = FileSystemStandingExternalTranscriptionPolicyStore(directory.resolve("policy").also { Files.createDirectories(it) }, audit)
        val original = StandingExternalTranscriptionPolicy(owner, ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE, Instant.parse("2026-05-01T00:00:00Z"))
        assertTrue(store.establish(original, audit))
        assertEquals(false, store.establish(original.copy(ownerPrincipalId = PrincipalId("other-owner")), audit))
        assertEquals(original, store.load())
    }

    @Test
    fun `completed audit for a different policy identity cannot activate the current policy`(@TempDir directory: java.nio.file.Path) = runTest {
        val auditFile = directory.resolve("audit.log")
        val backing = FileSystemCaseGovernanceAudit(auditFile)
        val failing = object : CaseGovernanceAudit {
            override suspend fun record(record: CaseGovernanceAuditRecord) {
                if (record.eventType == CaseGovernanceAuditEventType.STANDING_EXTERNAL_TRANSCRIPTION_POLICY_AUTHORISED) error("completion interrupted")
                backing.record(record)
            }
        }
        val store = FileSystemStandingExternalTranscriptionPolicyStore(directory.resolve("policy").also { Files.createDirectories(it) }, backing)
        val current = StandingExternalTranscriptionPolicy(owner, ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE, Instant.parse("2026-06-01T00:00:00Z"))
        val stale = current.copy(approvedAt = Instant.parse("2026-07-01T00:00:00Z"))

        assertFailsWith<IllegalStateException> { store.establish(current, failing) }
        backing.record(CaseGovernanceAuditRecord(
            CaseGovernanceAuditEventType.STANDING_EXTERNAL_TRANSCRIPTION_POLICY_AUTHORISED,
            null, actorPrincipalId = owner, recordedAt = stale.approvedAt,
            externalTranscriptionAuthorised = true, authorizationPurpose = stale.authorizationPurpose.value,
            policyVersion = stale.policyVersion, policyId = stale.policyId,
        ))
        assertNull(store.load())
    }
}
