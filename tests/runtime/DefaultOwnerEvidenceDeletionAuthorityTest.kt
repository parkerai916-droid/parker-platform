package parker.core.runtime

import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceArtifactStorage
import parker.core.interfaces.EvidenceArtifactStorageException
import parker.core.interfaces.EvidenceDeletionAudit
import parker.core.interfaces.EvidenceDeletionAuditException
import parker.core.interfaces.EvidenceDeletionAuditStage
import parker.core.interfaces.EvidenceDeletionResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Evidence Custodian, Implementation Plan Phase 7 ("Deletion workflow").
 * Behavioural tests of [DefaultOwnerEvidenceDeletionAuthority], governed
 * by `docs/architecture/EVIDENCE_CUSTODIAN_PHASE_7_BOUNDARY_CLARIFICATION.md`
 * ("the Boundary Clarification") Sections 3-7. Exercised with
 * [FakePermissionEngine], [InMemoryEvidenceArtifactStorage] /
 * [FakeEvidenceArtifactStorage], and [FakeEvidenceDeletionAudit] --
 * deliberately does not re-prove anything
 * [InMemoryEvidenceArtifactStorageTest] / [FileSystemEvidenceArtifactStorageTest]
 * already prove about storage's own `delete` behaviour. This suite is
 * about what this class itself adds: the permission gate, the
 * AUTHORISED-before-physical-deletion ordering, the
 * COMPLETED-required-for-Deleted rule, and the absence of any
 * `MemoryCore`/`EvidenceCustodian` dependency.
 */
class DefaultOwnerEvidenceDeletionAuthorityTest {

    private val principalId = PrincipalId("principal-1")

    private fun approvingEngine(outcome: PermissionDecisionOutcome = PermissionDecisionOutcome.APPROVED) =
        FakePermissionEngine { request -> decision(request, outcome) }

    private fun decision(request: ExecutionRequest, outcome: PermissionDecisionOutcome) = PermissionDecision(
        decisionId = DecisionId("decision-1"),
        principalId = request.principalId,
        resourceId = request.targetResources.first(),
        action = PermissionAction.DELETE,
        decision = outcome,
        level = PermissionLevel.AUTOMATIC,
        timestamp = Instant.now(),
    )

    // --- Permission denied ---

    @Test
    fun `a DENIED decision is Rejected, with no storage access and no audit write`() = runTest {
        val storage = FakeEvidenceArtifactStorage()
        val audit = FakeEvidenceDeletionAudit()
        val authority = DefaultOwnerEvidenceDeletionAuthority(
            storage,
            approvingEngine(PermissionDecisionOutcome.DENIED),
            audit,
        )

        val result = authority.deleteAsOwner(principalId, EvidenceArtifactId("artifact-1"))

        assertIs<EvidenceDeletionResult.Rejected>(result)
        assertEquals(0, storage.deleteCallCount, "a denied request must never reach storage")
        assertTrue(audit.records.isEmpty(), "a denied request must produce no audit record")
    }

    // --- Successful deletion ---

    @Test
    fun `an APPROVED decision for an existing artefact deletes it and writes AUTHORISED then COMPLETED`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        storage.write(EvidenceArtifactId("artifact-1"), "content".toByteArray())
        val audit = FakeEvidenceDeletionAudit()
        val authority = DefaultOwnerEvidenceDeletionAuthority(storage, approvingEngine(), audit)

        val result = authority.deleteAsOwner(principalId, EvidenceArtifactId("artifact-1"))

        assertIs<EvidenceDeletionResult.Deleted>(result)
        assertNull(storage.read(EvidenceArtifactId("artifact-1")))
        assertEquals(2, audit.records.size)
        assertEquals(EvidenceDeletionAuditStage.AUTHORISED, audit.records[0].stage)
        assertEquals(EvidenceDeletionAuditStage.COMPLETED, audit.records[1].stage)
        assertEquals(
            audit.records[0].deletionRequestId,
            audit.records[1].deletionRequestId,
            "both records of one attempt must share one deletionRequestId",
        )
    }

    @Test
    fun `an APPROVED_WITH_CONFIRMATION decision also results in Deleted`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        storage.write(EvidenceArtifactId("artifact-1"), "content".toByteArray())
        val audit = FakeEvidenceDeletionAudit()
        val authority = DefaultOwnerEvidenceDeletionAuthority(
            storage,
            approvingEngine(PermissionDecisionOutcome.APPROVED_WITH_CONFIRMATION),
            audit,
        )

        val result = authority.deleteAsOwner(principalId, EvidenceArtifactId("artifact-1"))

        assertIs<EvidenceDeletionResult.Deleted>(result)
    }

    // --- Not found ---

    @Test
    fun `an APPROVED decision for a nonexistent artefact returns NotFound and writes only AUTHORISED`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        val audit = FakeEvidenceDeletionAudit()
        val authority = DefaultOwnerEvidenceDeletionAuthority(storage, approvingEngine(), audit)

        val result = authority.deleteAsOwner(principalId, EvidenceArtifactId("never-written"))

        assertIs<EvidenceDeletionResult.NotFound>(result)
        assertEquals(1, audit.records.size, "a not-found outcome produces only the AUTHORISED record")
        assertEquals(EvidenceDeletionAuditStage.AUTHORISED, audit.records.single().stage)
    }

    // --- Storage failure ---

    @Test
    fun `a genuine storage fault propagates, leaving only the AUTHORISED record`() = runTest {
        val storage = FakeEvidenceArtifactStorage(
            deleteBehavior = { throw EvidenceArtifactStorageException.StorageIOFailure("simulated", IOException()) },
        )
        val audit = FakeEvidenceDeletionAudit()
        val authority = DefaultOwnerEvidenceDeletionAuthority(storage, approvingEngine(), audit)

        assertFailsWith<EvidenceArtifactStorageException.StorageIOFailure> {
            authority.deleteAsOwner(principalId, EvidenceArtifactId("artifact-1"))
        }

        assertEquals(1, audit.records.size)
        assertEquals(EvidenceDeletionAuditStage.AUTHORISED, audit.records.single().stage)
    }

    // --- Audit ordering: an AUTHORISED-write failure blocks physical deletion entirely ---

    @Test
    fun `if the AUTHORISED audit write fails, physical deletion is never attempted`() = runTest {
        val storage = FakeEvidenceArtifactStorage()
        val audit = FakeEvidenceDeletionAudit(
            recordBehavior = { throw EvidenceDeletionAuditException.PersistenceFailure("simulated", IOException()) },
        )
        val authority = DefaultOwnerEvidenceDeletionAuthority(storage, approvingEngine(), audit)

        assertFailsWith<EvidenceDeletionAuditException.PersistenceFailure> {
            authority.deleteAsOwner(principalId, EvidenceArtifactId("artifact-1"))
        }

        assertEquals(
            0,
            storage.deleteCallCount,
            "storage.delete must never be reached if the AUTHORISED record cannot be durably written",
        )
    }

    // --- Audit ordering: a COMPLETED-write failure after successful physical deletion ---

    @Test
    fun `if the COMPLETED audit write fails after a successful physical deletion, Deleted is never returned`() = runTest {
        val storage = InMemoryEvidenceArtifactStorage()
        storage.write(EvidenceArtifactId("artifact-1"), "content".toByteArray())
        var recordCallCount = 0
        val audit = FakeEvidenceDeletionAudit(
            recordBehavior = { record ->
                recordCallCount++
                if (record.stage == EvidenceDeletionAuditStage.COMPLETED) {
                    throw EvidenceDeletionAuditException.PersistenceFailure("simulated", IOException())
                }
            },
        )
        val authority = DefaultOwnerEvidenceDeletionAuthority(storage, approvingEngine(), audit)

        assertFailsWith<EvidenceDeletionAuditException.PersistenceFailure> {
            authority.deleteAsOwner(principalId, EvidenceArtifactId("artifact-1"))
        }

        // The underlying bytes are already, irreversibly gone (Boundary Clarification Section 8 --
        // no rollback exists) even though the caller was never told Deleted. This confirms the one
        // disclosed residual case, not a bug: a caller can never observe Deleted without a durable
        // COMPLETED record, even though physical deletion itself already succeeded.
        assertNull(storage.read(EvidenceArtifactId("artifact-1")))
        assertEquals(2, recordCallCount, "both the AUTHORISED and COMPLETED writes must have been attempted")
    }

    // --- Structural dependency shape (Boundary Clarification Section 4) ---

    @Test
    fun `DefaultOwnerEvidenceDeletionAuthority declares exactly three constructor dependencies -- no MemoryCore, no EvidenceCustodian`() {
        val constructor = DefaultOwnerEvidenceDeletionAuthority::class.constructors.single()
        val parameterTypes = constructor.parameters.map { it.type.classifier }

        assertEquals(3, parameterTypes.size)
        assertEquals(
            setOf(EvidenceArtifactStorage::class, PermissionEngine::class, EvidenceDeletionAudit::class),
            parameterTypes.toSet(),
            "DefaultOwnerEvidenceDeletionAuthority must depend on exactly EvidenceArtifactStorage, " +
                "PermissionEngine, and EvidenceDeletionAudit -- no MemoryCore, no EvidenceCustodian " +
                "(Boundary Clarification Section 4)",
        )
    }
}
