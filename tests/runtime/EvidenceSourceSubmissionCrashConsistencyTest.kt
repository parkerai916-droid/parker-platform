package parker.core.runtime

import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import parker.core.interfaces.CandidateEvidenceArtifact
import parker.core.interfaces.DecisionId
import parker.core.interfaces.EvidenceArtifactId
import parker.core.interfaces.EvidenceSourceSubmissionResult
import parker.core.interfaces.ExecutionRequest
import parker.core.interfaces.PermissionAction
import parker.core.interfaces.PermissionDecision
import parker.core.interfaces.PermissionDecisionOutcome
import parker.core.interfaces.PermissionEngine
import parker.core.interfaces.PermissionLevel
import parker.core.interfaces.PrincipalId
import parker.core.interfaces.SourceIdentityReservation
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * AG-1F crash-safe idempotency review. Proves [DefaultEvidenceCustodian.submitSource]'s corrected
 * algorithm (durable `createOrGet` reservation, then idempotent, duplicate-tolerant canonical
 * completion) against the real, file-backed [FileSystemEvidenceArtifactStorage]/
 * [FileSystemEvidenceSourceManifestStorage]/[FileSystemEvidenceSourceIdentityIndex] triple -- never
 * the in-memory fakes, since crash-consistency is specifically about durable state surviving a
 * process restart. The original investigation's crash-window test (this file's prior revision)
 * proved the pre-fix algorithm violated hard idempotency; every test below now proves the
 * corrected algorithm does not.
 */
class EvidenceSourceSubmissionCrashConsistencyTest {

    private val principalId = PrincipalId("principal-1")

    private fun approvingEngine() = FakePermissionEngine { request ->
        PermissionDecision(
            decisionId = DecisionId("decision-1"),
            principalId = request.principalId,
            resourceId = request.targetResources.first(),
            action = PermissionAction.WRITE,
            decision = PermissionDecisionOutcome.APPROVED,
            level = PermissionLevel.AUTOMATIC,
            timestamp = Instant.now(),
        )
    }

    private fun newCustodian(
        artifactRoot: java.nio.file.Path,
        manifestRoot: java.nio.file.Path,
        indexRoot: java.nio.file.Path,
        engine: PermissionEngine = approvingEngine(),
    ): DefaultEvidenceCustodian = DefaultEvidenceCustodian(
        FileSystemEvidenceArtifactStorage(artifactRoot),
        engine,
        FileSystemEvidenceSourceManifestStorage(manifestRoot),
        FileSystemEvidenceSourceIdentityIndex(indexRoot),
    )

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    // ================= A/B: ordinary first/second submission =================

    @Test
    fun `A -- normal first submission is Registered`() = runTest {
        val custodian = newCustodian(
            Files.createTempDirectory("ab-artifact"),
            Files.createTempDirectory("ab-manifest"),
            Files.createTempDirectory("ab-index"),
        )
        val content = "normal first submission".toByteArray()

        val result = custodian.submitSource(principalId, CandidateEvidenceArtifact(content))

        assertIs<EvidenceSourceSubmissionResult.Registered>(result)
    }

    @Test
    fun `B -- normal second identical submission is AlreadyRegistered with the same identity`() = runTest {
        val custodian = newCustodian(
            Files.createTempDirectory("ab-artifact-2"),
            Files.createTempDirectory("ab-manifest-2"),
            Files.createTempDirectory("ab-index-2"),
        )
        val content = "normal second submission".toByteArray()

        val first = assertIs<EvidenceSourceSubmissionResult.Registered>(
            custodian.submitSource(principalId, CandidateEvidenceArtifact(content)),
        )
        val second = assertIs<EvidenceSourceSubmissionResult.AlreadyRegistered>(
            custodian.submitSource(principalId, CandidateEvidenceArtifact(content)),
        )

        assertEquals(first.evidenceArtifactId, second.evidenceArtifactId)
    }

    // ================= C: crash between reservation and canonical completion =================

    @Test
    fun `C -- CRASH RECOVERY -- a reservation with no completed canonical state is completed under the SAME identity, never a second one`() = runTest {
        val artifactRoot = Files.createTempDirectory("crash-recovery-artifact")
        val manifestRoot = Files.createTempDirectory("crash-recovery-manifest")
        val indexRoot = Files.createTempDirectory("crash-recovery-index")
        val content = "identical bytes across a simulated crash".toByteArray()
        val sha256 = sha256Hex(content)

        // Simulate a crash strictly between the reservation (durable) and canonical completion
        // (bytes + manifest, never reached): reserve the hash directly, exactly as submitSource's
        // own first step would, then deliberately never write bytes or a manifest for it.
        val preCrashIndex = FileSystemEvidenceSourceIdentityIndex(indexRoot)
        val x = EvidenceArtifactId("evidence-pre-crash-reservation")
        val reservation = assertIs<SourceIdentityReservation.Created>(preCrashIndex.createOrGet(sha256, x))
        assertEquals(x, reservation.evidenceArtifactId)

        // Confirm the crash precondition: canonical evidence genuinely does not exist yet, even
        // though the reservation is already durable.
        assertEquals(null, FileSystemEvidenceSourceManifestStorage(manifestRoot).read(x))

        // "Restart" -- fresh instances against the identical, already-durable filesystem roots.
        val postCrashCustodian = newCustodian(artifactRoot, manifestRoot, indexRoot)

        val result = postCrashCustodian.submitSource(principalId, CandidateEvidenceArtifact(content))

        // Corrected behaviour: the previously-interrupted reservation is completed under the SAME
        // identity `x` -- never a second, distinct identity.
        val registered = assertIs<EvidenceSourceSubmissionResult.Registered>(result)
        assertEquals(x, registered.evidenceArtifactId, "the pre-existing reservation's identity must be completed, never replaced by a new one")
        assertEquals(sha256, registered.manifest.sha256)

        // A further resubmission now sees fully-complete canonical state and is AlreadyRegistered
        // under that same identity.
        val third = assertIs<EvidenceSourceSubmissionResult.AlreadyRegistered>(
            postCrashCustodian.submitSource(principalId, CandidateEvidenceArtifact(content)),
        )
        assertEquals(x, third.evidenceArtifactId)
    }

    // ================= D: legacy "canonical exists, reservation missing" shape =================

    @Test
    fun `D -- submitSource alone can never produce canonical evidence without first durably reserving its identity`() = runTest {
        val artifactRoot = Files.createTempDirectory("legacy-shape-artifact")
        val manifestRoot = Files.createTempDirectory("legacy-shape-manifest")
        val indexRoot = Files.createTempDirectory("legacy-shape-index")
        val custodian = newCustodian(artifactRoot, manifestRoot, indexRoot)
        val content = "submitSource-originated content".toByteArray()
        val sha256 = sha256Hex(content)

        val result = assertIs<EvidenceSourceSubmissionResult.Registered>(
            custodian.submitSource(principalId, CandidateEvidenceArtifact(content)),
        )

        // Under the corrected algorithm, canonical registration is only ever reached via
        // ensureCanonicalRegistration, which only ever runs after createOrGet has already
        // durably reserved this exact hash for this exact identity -- so the reservation must
        // always be found, naming the same identity, once canonical state exists.
        val index = FileSystemEvidenceSourceIdentityIndex(indexRoot)
        val reservation = assertIs<SourceIdentityReservation.Existing>(
            index.createOrGet(sha256, EvidenceArtifactId("irrelevant-proposal")),
        )
        assertEquals(result.evidenceArtifactId, reservation.evidenceArtifactId)

        // NOTE (reported separately, not a defect this fix must close): EvidenceCustodian.accept()
        // itself remains completely unmodified and untouched by this correction -- it mints an
        // unconditional, fresh identity on every call and never consults or writes to the
        // source-identity index at all, by its own long-standing, frozen, pre-AG-1F contract. A
        // direct accept() caller therefore still produces canonical evidence with no source-identity
        // reservation whatsoever -- this is accept()'s own documented, deliberate behaviour, not the
        // crash-window defect this Unit fixes, and is not exercised by this test.
    }

    // ================= E: multi-instance genuine-thread race =================

    /**
     * Owner-reviewed finding, recorded directly in this test: [FileSystemEvidenceArtifactStorage.write]
     * and [FileSystemEvidenceSourceManifestStorage.write]'s own "duplicate writes are refused"
     * guarantee is enforced only by each class's own private, per-instance `Mutex` -- confirmed by
     * direct experiment (a standalone `Files.move(src, target, ATOMIC_MOVE)` repro, no
     * `REPLACE_EXISTING`, run outside any Mutex) that this JDK/filesystem's `ATOMIC_MOVE` does
     * **not** throw `FileAlreadyExistsException` when the target already exists -- it silently,
     * atomically overwrites. Both storage classes are pre-existing, frozen Evidence Custodian
     * Phase 1 / Document Ingestion primitives, unmodified by, and outside, AG-1F's own authorised
     * scope to change. For AG-1F's own use, this is never a data-integrity hazard: every
     * concurrent completer of the same reservation writes byte-identical content (the same
     * candidate bytes, the same computed manifest fields), so whichever completer's write
     * physically lands last, the final stored bytes and manifest are always correct and
     * identical regardless of which one "won." What it does mean is that more than one
     * concurrent completer of the *same* interrupted or racing reservation may each observe
     * [EvidenceSourceSubmissionResult.Registered] rather than a clean one-Registered/
     * rest-AlreadyRegistered split -- exactly the "where both participate in completing an
     * interrupted reservation, another deterministic clean result that still returns X" outcome
     * the crash-safe idempotency review's own acceptance criteria explicitly allows, never an
     * orphaned identity, a second manifest, or an uncaught exception.
     */
    @Test
    fun `E -- 25-way multi-instance identical-source race converges on exactly one identity, zero exceptions, zero orphans, correct final content`() = runBlocking {
        val artifactRoot = Files.createTempDirectory("concurrency-multi-artifact")
        val manifestRoot = Files.createTempDirectory("concurrency-multi-manifest")
        val indexRoot = Files.createTempDirectory("concurrency-multi-index")
        val content = "concurrent identical bytes, many separate instances".toByteArray()
        val attempts = 25

        // One freshly-constructed DefaultEvidenceCustodian per attempt -- each simulating a
        // separate Parker process racing to submit the same source, all sharing the identical
        // durable storage roots. No in-process mutex exists any more in DefaultEvidenceCustodian
        // -- the filesystem-level createOrGet reservation is the only serialisation point under
        // test here.
        val outcomes = List(attempts) {
            async(Dispatchers.Default) {
                runCatching { newCustodian(artifactRoot, manifestRoot, indexRoot).submitSource(principalId, CandidateEvidenceArtifact(content)) }
            }
        }.awaitAll()

        val succeeded = outcomes.mapNotNull { it.getOrNull() }
        val failed = outcomes.mapNotNull { it.exceptionOrNull() }
        val distinctIds = succeeded.map {
            when (it) {
                is EvidenceSourceSubmissionResult.Registered -> it.evidenceArtifactId
                is EvidenceSourceSubmissionResult.AlreadyRegistered -> it.evidenceArtifactId
                else -> error("unexpected result: $it")
            }
        }.toSet()

        assertTrue(failed.isEmpty(), "no attempt may throw an uncaught exception -- got: $failed")
        assertEquals(attempts, succeeded.size, "every attempt must resolve to a clean result")
        assertEquals(1, distinctIds.size, "all $attempts attempts must converge on exactly one EvidenceArtifactId -- never an orphaned second identity")
        assertTrue(
            succeeded.all { it is EvidenceSourceSubmissionResult.Registered || it is EvidenceSourceSubmissionResult.AlreadyRegistered },
            "every attempt must be a clean Registered or AlreadyRegistered outcome -- never Rejected, HashMismatch, or Conflict for identical valid content",
        )
        assertTrue(succeeded.any { it is EvidenceSourceSubmissionResult.Registered }, "at least one attempt must actually complete registration")

        // Regardless of how many attempts observed Registered, the final durable content under
        // the one shared identity is always exactly the submitted bytes -- never mixed, never
        // corrupted, never a different writer's content winning by accident.
        val winningId = distinctIds.single()
        assertContentEquals(content, FileSystemEvidenceArtifactStorage(artifactRoot).read(winningId))
        val finalManifest = FileSystemEvidenceSourceManifestStorage(manifestRoot).read(winningId)
        assertEquals(sha256Hex(content), finalManifest?.sha256)
    }

    // ================= F: advisory hash mismatch creates nothing =================

    @Test
    fun `F -- an advisory SHA-256 mismatch creates no reservation and writes no evidence`() = runTest {
        val artifactRoot = Files.createTempDirectory("mismatch-artifact")
        val manifestRoot = Files.createTempDirectory("mismatch-manifest")
        val indexRoot = Files.createTempDirectory("mismatch-index")
        val custodian = newCustodian(artifactRoot, manifestRoot, indexRoot)
        val content = "real content".toByteArray()
        val wrongAdvisory = sha256Hex("completely different content".toByteArray())

        val result = custodian.submitSource(principalId, CandidateEvidenceArtifact(content), wrongAdvisory)

        assertIs<EvidenceSourceSubmissionResult.HashMismatch>(result)

        // No reservation was created for the authoritative hash.
        val index = FileSystemEvidenceSourceIdentityIndex(indexRoot)
        val proposedId = EvidenceArtifactId("evidence-should-become-the-reservation")
        val reservation = assertIs<SourceIdentityReservation.Created>(index.createOrGet(sha256Hex(content), proposedId))
        assertEquals(proposedId, reservation.evidenceArtifactId, "a hash mismatch must never have pre-reserved the authoritative hash")
    }

    // ================= G: conflicting canonical state fails closed =================

    @Test
    fun `G -- a reservation pointing at canonical state that does not match fails closed as Conflict, never overwritten, never a new identity`() = runTest {
        val artifactRoot = Files.createTempDirectory("conflict-artifact")
        val manifestRoot = Files.createTempDirectory("conflict-manifest")
        val indexRoot = Files.createTempDirectory("conflict-index")
        val content = "the real submission content".toByteArray()
        val sha256 = sha256Hex(content)
        val conflictingBytes = "unrelated bytes already sitting under the reserved identity".toByteArray()

        // Fabricate a conflicting canonical state: reserve `sha256` for identity `x`, then write
        // DIFFERENT bytes under `x` directly (bypassing submitSource entirely) -- simulating
        // corrupted or tampered storage, never an ordinary resubmission outcome.
        val x = EvidenceArtifactId("evidence-conflict-target")
        val index = FileSystemEvidenceSourceIdentityIndex(indexRoot)
        assertIs<SourceIdentityReservation.Created>(index.createOrGet(sha256, x))
        val rawStorage = FileSystemEvidenceArtifactStorage(artifactRoot)
        rawStorage.write(x, conflictingBytes)

        val custodian = newCustodian(artifactRoot, manifestRoot, indexRoot)
        val result = custodian.submitSource(principalId, CandidateEvidenceArtifact(content))

        val conflict = assertIs<EvidenceSourceSubmissionResult.Conflict>(result)
        assertEquals(x, conflict.evidenceArtifactId)
        assertEquals(sha256, conflict.computedSha256)

        // The conflicting bytes already stored under `x` are untouched.
        assertContentEquals(conflictingBytes, rawStorage.read(x))
        // No manifest was fabricated for `x`.
        assertEquals(null, FileSystemEvidenceSourceManifestStorage(manifestRoot).read(x))
        // No second identity was minted for this hash -- the reservation still names `x` alone.
        val reservation = assertIs<SourceIdentityReservation.Existing>(
            index.createOrGet(sha256, EvidenceArtifactId("irrelevant-proposal")),
        )
        assertEquals(x, reservation.evidenceArtifactId)
    }

    // ================= sanity: different bytes still get different identities =================

    @Test
    fun `different bytes still resolve to different identities, both Registered`() = runTest {
        val custodian = newCustodian(
            Files.createTempDirectory("distinct-artifact"),
            Files.createTempDirectory("distinct-manifest"),
            Files.createTempDirectory("distinct-index"),
        )

        val first = assertIs<EvidenceSourceSubmissionResult.Registered>(
            custodian.submitSource(principalId, CandidateEvidenceArtifact("source A".toByteArray())),
        )
        val second = assertIs<EvidenceSourceSubmissionResult.Registered>(
            custodian.submitSource(principalId, CandidateEvidenceArtifact("source B".toByteArray())),
        )

        assertNotEquals(first.evidenceArtifactId, second.evidenceArtifactId)
    }
}
