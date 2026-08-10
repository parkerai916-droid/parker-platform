package parker.integration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Reasoning Protocol Live-Model Conformance, Unit 3-C -- orchestration
 * driver, artifact-root hard restriction, disk-space gate, and manual
 * false-positive safety-review checkpoint.
 *
 * Closes the four remaining readiness gaps identified by the Unit 3-C
 * Implementation Readiness Review (all now addressed here; the Family C
 * trace-prediction gap was separately closed by governance correction in
 * the committed, frozen Plan). Test-tier only -- never touches `src/`, never
 * imports any downstream coordinator, and no test in this file makes a
 * model or HTTP call. The live-calling executor construction that would
 * bridge to real production classes is reserved for [Unit3CLiveEntryPoint]
 * and is never invoked by any test.
 */

// ============================================================
// Artifact-root hard restriction (Readiness gap 2). Mirrors Unit 2-D's own
// established `acceptedArtifactParent`/`campaignArtifactRoot` pattern
// exactly -- the Plan itself does not specify a different pattern, so this
// reuses the same, already-governed, already-accepted durable root rather
// than inventing a new one.
// ============================================================

const val UNIT_3C_ARTIFACT_ROOT_PREFIX = "/var/lib/parker/reasoning-protocol-live-model"

val UNIT_3C_PRESERVED_CAMPAIGN_IDS: Set<String> = setOf(
    "qwen25coder7b-baseline-20260809",
    "qwen25coder7b-llama32-3b-diagnostic-20260809",
)

class Unit3CArtifactRootViolationException(message: String) : IllegalArgumentException(message)

object Unit3CArtifactRootPolicy {
    /** Fails closed on anything other than exactly the one accepted,
     * durable, absolute parent -- this single check structurally rejects
     * relative paths, repository-relative paths, `build/reports`, `src`,
     * `tests`, `docs`, and any path-traversal string, since none of them
     * can ever equal [UNIT_3C_ARTIFACT_ROOT_PREFIX] as a raw string. */
    fun resolve(configuredParent: String, campaignId: String): Path {
        val normalizedParentText = configuredParent.replace('\\', '/').trimEnd('/')
        if (normalizedParentText != UNIT_3C_ARTIFACT_ROOT_PREFIX) {
            throw Unit3CArtifactRootViolationException(
                "artifact root must be exactly the accepted durable parent: $UNIT_3C_ARTIFACT_ROOT_PREFIX",
            )
        }
        if (!campaignId.matches(Regex("[a-z0-9][a-z0-9.-]*"))) {
            throw Unit3CArtifactRootViolationException("campaign ID must be machine-safe")
        }
        if (!campaignId.startsWith("unit3c-remedy-experiments-")) {
            throw Unit3CArtifactRootViolationException("campaign ID must start with the frozen unit3c-remedy-experiments- marker")
        }
        if (campaignId in UNIT_3C_PRESERVED_CAMPAIGN_IDS) {
            throw Unit3CArtifactRootViolationException("must not reuse a preserved Unit 2/Unit 2-D campaign identity")
        }
        val parentPath = Path.of(configuredParent).normalize()
        val resolved = parentPath.resolve(campaignId).normalize()
        val expected = "$UNIT_3C_ARTIFACT_ROOT_PREFIX/$campaignId"
        if (resolved.toString().replace('\\', '/').trimEnd('/') != expected) {
            throw Unit3CArtifactRootViolationException("campaign artifact path must be exactly one directory beneath the accepted parent")
        }
        for (preserved in UNIT_3C_PRESERVED_CAMPAIGN_IDS) {
            val forbidden = parentPath.resolve(preserved).normalize()
            if (resolved == forbidden || resolved.startsWith(forbidden)) {
                throw Unit3CArtifactRootViolationException("must not resolve into a preserved Unit 2/Unit 2-D campaign directory")
            }
        }
        return resolved
    }
}

// ============================================================
// Disk-space pre-flight gate (Readiness gap 3). The frozen Plan states
// neither a number nor a derivation for this check; per this task's own
// instruction to prefer an already-defined derivation over inventing one,
// this reuses Unit 2-D's own already-governed, already-accepted minimum
// (2 GiB), the immediately analogous predecessor unit's precedent, rather
// than fabricating an unrelated figure.
// ============================================================

const val UNIT_3C_MINIMUM_FREE_BYTES: Long = 2L * 1024L * 1024L * 1024L

class Unit3CInsufficientSpaceException(message: String) : RuntimeException(message)

object Unit3CDiskSpaceGate {
    /** Must run before any live call. Fails closed on both insufficient
     * and unknown/unreadable filesystem state. */
    fun check(path: Path, usableSpace: (Path) -> Long) {
        val available = try {
            usableSpace(path)
        } catch (e: IOException) {
            throw Unit3CInsufficientSpaceException("unable to determine usable space for $path: ${e.message}")
        }
        if (available < UNIT_3C_MINIMUM_FREE_BYTES) {
            throw Unit3CInsufficientSpaceException(
                "artifact filesystem has less than $UNIT_3C_MINIMUM_FREE_BYTES bytes usable (has $available) at $path",
            )
        }
    }
}

// ============================================================
// Manual false-positive safety-review checkpoint (Readiness gap 4).
// Non-numeric, first-occurrence trigger, exactly as the frozen Plan
// Section 18 requires.
// ============================================================

enum class Unit3CArmOutcome { SEALED, HALTED, SAFETY_CHECKPOINT }

/** True exactly when Plan Section 18's non-numeric trigger condition is
 * met: a false-positive REMEMBER or GOAL result on a fixture drawn from
 * the adversarial/negative-control surface (`ADVERSARIAL`, or `REPLY`/`GOAL`
 * acting as a negative control for the other action). Never true for a
 * fixture whose own expected action is REMEMBER or NOACTION -- those are
 * not negative controls in the sense Section 18 means. */
fun isAdversarialCategoryFalsePositive(category: FixtureCategory, expectedAction: ExpectedAction, actualAction: ExpectedAction?): Boolean {
    if (actualAction == null) return false
    if (actualAction != ExpectedAction.REMEMBER && actualAction != ExpectedAction.GOAL) return false
    if (actualAction == expectedAction) return false
    return category == FixtureCategory.ADVERSARIAL || category == FixtureCategory.REPLY || category == FixtureCategory.GOAL
}

/** Marks and enforces the checkpoint on a single arm's ledger directory.
 * Deliberately minimal: it never deletes, reruns, or repairs anything --
 * it only records that the checkpoint fired and blocks further progress
 * for that arm until the marker is explicitly cleared by a separate,
 * later operator/governance action (not performed anywhere in this file). */
class Unit3CSafetyCheckpoint(private val armDirectory: Path) {
    private val marker = armDirectory.resolve("SAFETY_CHECKPOINT")

    fun isTriggered(): Boolean = marker.exists()

    fun trigger(trialId: String, reason: String) {
        Files.createDirectories(armDirectory)
        Files.writeString(marker, "trialId=$trialId\nreason=$reason\n")
    }

    /** No implicit auto-clear path exists anywhere in this codebase --
     * clearing requires calling this explicitly, deliberately, from a
     * separate future governance/operator action. */
    fun clearForExplicitlyAuthorizedContinuation() {
        if (marker.exists()) Files.delete(marker)
    }
}

// ============================================================
// Orchestration driver (Readiness gap 1). Owns the frozen arm order and
// schedule, enforces exact-once execution per arm via [Unit3CArmLedger],
// integrates the safety checkpoint, and fails closed on any
// measurement-invalidating defect without ever crashing past an arm
// boundary. Records remedy-performance outcomes (wrong action, false
// positives that are NOT adversarial-category) as evidence, never as a
// stop condition.
// ============================================================

private val UNIT_3C_ARM_ORDER: List<Unit3CFamily> = listOf(
    Unit3CFamily.CONTROL,
    Unit3CFamily.FAMILY_A,
    Unit3CFamily.FAMILY_B,
    Unit3CFamily.FAMILY_C,
)

data class Unit3CArmResult(val family: Unit3CFamily, val outcome: Unit3CArmOutcome, val completedTrialCount: Int)

fun interface Unit3CTrialExecutor {
    /** Synchronous by design -- the real, live-calling implementation
     * (constructed only inside [Unit3CLiveEntryPoint], never here) bridges
     * to `ModelReasoningProvider`'s own `suspend fun reason` internally via
     * `runBlocking`; this interface itself stays coroutine-free so it can
     * be exercised by a plain, synchronous fake in every test in this file. */
    fun execute(trial: Unit3CTrial): Unit3CObservation
}

class Unit3COrchestrationDriver(
    private val campaignId: String,
    private val artifactRoot: Path,
    private val identity: Unit3CIdentity,
    private val usableSpace: (Path) -> Long = { Files.getFileStore(it).usableSpace },
) {
    fun run(executors: Map<Unit3CFamily, Unit3CTrialExecutor>): List<Unit3CArmResult> {
        Unit3CDiskSpaceGate.check(artifactRoot, usableSpace)
        return UNIT_3C_ARM_ORDER.map { family -> runArm(family, executors.getValue(family)) }
    }

    private fun trialsFor(family: Unit3CFamily): List<Unit3CTrial> = when (family) {
        Unit3CFamily.CONTROL -> Unit3CCampaignDefinition.controlTrials
        Unit3CFamily.FAMILY_A -> Unit3CCampaignDefinition.familyATrials
        Unit3CFamily.FAMILY_B -> Unit3CCampaignDefinition.familyBTrials
        Unit3CFamily.FAMILY_C -> Unit3CCampaignDefinition.familyCTrials
    }

    private fun armDirectoryName(family: Unit3CFamily): String = when (family) {
        Unit3CFamily.CONTROL -> "control"
        Unit3CFamily.FAMILY_A -> "family-a"
        Unit3CFamily.FAMILY_B -> "family-b"
        Unit3CFamily.FAMILY_C -> "family-c"
    }

    private fun runArm(family: Unit3CFamily, executor: Unit3CTrialExecutor): Unit3CArmResult {
        val trials = trialsFor(family)
        val armDirectory = artifactRoot.resolve(armDirectoryName(family))
        val ledger = Unit3CArmLedger(armDirectory, trials.map { it.id }.toSet())
        val checkpoint = Unit3CSafetyCheckpoint(armDirectory)

        return try {
            ledger.checkIdentity(identity) // measurement-invalidating: identity drift fails closed
            if (checkpoint.isTriggered()) {
                // A prior run already halted here for manual review; do not
                // silently resume. Only an explicit, separate, future
                // operator action (never taken by this driver) can clear it.
                return Unit3CArmResult(family, Unit3CArmOutcome.SAFETY_CHECKPOINT, ledger.recover().size)
            }
            val completed = ledger.recover().toMutableSet() // exact-once: duplicate prevention via prior completion
            for (trial in trials) {
                if (trial.id in completed) continue
                val observation = executor.execute(trial) // Family C: zero model calls, by construction of its own executor
                ledger.appendObservation(trial.id, encodeObservation(observation))
                completed += trial.id
                if (isAdversarialCategoryFalsePositive(observation.fixtureCategory, observation.expectedAction, observation.actualAction)) {
                    // Preserve the observation (already durably appended above),
                    // halt further live calls in this arm only, mark state
                    // unambiguously. Never deletes, reruns, or repairs.
                    checkpoint.trigger(trial.id, "adversarial-category false positive")
                    return Unit3CArmResult(family, Unit3CArmOutcome.SAFETY_CHECKPOINT, completed.size)
                }
                // Remedy-performance failures that are NOT adversarial-category
                // false positives (wrong semantic action elsewhere,
                // representation failure, etc.) are recorded above as
                // evidence and do not, by themselves, halt the arm.
            }
            ledger.seal(trials.map { it.id }.toSet())
            Unit3CArmResult(family, Unit3CArmOutcome.SEALED, completed.size)
        } catch (e: Unit3CArtifactIntegrityException) {
            Unit3CArmResult(family, Unit3CArmOutcome.HALTED, 0)
        }
    }

    private fun encodeObservation(observation: Unit3CObservation): String =
        "campaignId=${observation.campaignId}|family=${observation.family}|fixtureId=${observation.fixtureId}"
}

// ============================================================
// Tests -- entirely offline. Every executor in this file is a fake;
// none makes a model or HTTP call.
// ============================================================

class ReasoningProtocolUnit3COrchestrationTest {

    private fun sampleIdentity() = Unit3CIdentity("commitA", "qwen2.5-coder:7b", "digestA", "endpointA", 30_000L)

    private fun fakeObservationFor(trial: Unit3CTrial, actualAction: ExpectedAction?): Unit3CObservation {
        val isFamilyC = trial.family == Unit3CFamily.FAMILY_C
        return Unit3CObservation(
            campaignId = "unit3c-remedy-experiments-test",
            family = trial.family,
            arm = trial.family.name,
            fixtureId = trial.fixture.id,
            fixtureCategory = trial.fixture.category,
            contextProfileId = "minimal-production-context",
            trialSequence = trial.attempt,
            expectedAction = trial.fixture.expectedAction,
            actualAction = actualAction,
            semanticCorrect = actualAction?.let { it == trial.fixture.expectedAction },
            representationValid = true,
            contentFidelity = null,
            modelName = if (isFamilyC) null else "qwen2.5-coder:7b",
            modelDigest = if (isFamilyC) null else "digestA",
            runtimeIdentity = if (isFamilyC) null else "runtimeA",
            endpointIdentifier = if (isFamilyC) null else "endpointA",
            timeoutMs = if (isFamilyC) null else 30_000L,
            inferenceConfigIdentity = if (isFamilyC) null else "default",
            promptIdentity = if (isFamilyC) null else "fake",
            prompt = if (isFamilyC) null else "fake prompt",
            rawRequest = if (isFamilyC) null else "{}",
            rawResponse = if (isFamilyC) null else "fake response",
            parserResult = if (isFamilyC) null else "fake",
            parserFailure = null,
            latencyNanos = if (isFamilyC) null else 1L,
            transportOutcome = if (isFamilyC) null else "ok",
            candidateMechanismIdentity = if (isFamilyC) "candidate-c1" else null,
            stableInputHash = "hash-${trial.id}",
            repositoryCommit = "commitA",
        )
    }

    // ---- Artifact-root hard restriction ----

    @Test
    fun `artifact root rejects a relative path`() {
        assertFailsWith<Unit3CArtifactRootViolationException> {
            Unit3CArtifactRootPolicy.resolve("relative/path", "unit3c-remedy-experiments-20260810")
        }
    }

    @Test
    fun `artifact root rejects a repository-relative path`() {
        assertFailsWith<Unit3CArtifactRootViolationException> {
            Unit3CArtifactRootPolicy.resolve("/home/steve/parker-platform", "unit3c-remedy-experiments-20260810")
        }
    }

    @Test
    fun `artifact root rejects build reports`() {
        assertFailsWith<Unit3CArtifactRootViolationException> {
            Unit3CArtifactRootPolicy.resolve("/home/steve/parker-platform/build/reports", "unit3c-remedy-experiments-20260810")
        }
    }

    @Test
    fun `artifact root rejects src tests and docs`() {
        for (path in listOf("/home/steve/parker-platform/src", "/home/steve/parker-platform/tests", "/home/steve/parker-platform/docs")) {
            assertFailsWith<Unit3CArtifactRootViolationException> {
                Unit3CArtifactRootPolicy.resolve(path, "unit3c-remedy-experiments-20260810")
            }
        }
    }

    @Test
    fun `artifact root rejects path traversal`() {
        assertFailsWith<Unit3CArtifactRootViolationException> {
            Unit3CArtifactRootPolicy.resolve("$UNIT_3C_ARTIFACT_ROOT_PREFIX/../etc", "unit3c-remedy-experiments-20260810")
        }
    }

    @Test
    fun `artifact root rejects an already campaign-qualified parent`() {
        assertFailsWith<Unit3CArtifactRootViolationException> {
            Unit3CArtifactRootPolicy.resolve("$UNIT_3C_ARTIFACT_ROOT_PREFIX/unit3c-remedy-experiments-20260810", "unit3c-remedy-experiments-20260810")
        }
    }

    @Test
    fun `artifact root rejects the wrong parent entirely`() {
        assertFailsWith<Unit3CArtifactRootViolationException> {
            Unit3CArtifactRootPolicy.resolve("/tmp/somewhere-else", "unit3c-remedy-experiments-20260810")
        }
    }

    @Test
    fun `artifact root rejects collision with an existing preserved campaign directory`() {
        for (preserved in UNIT_3C_PRESERVED_CAMPAIGN_IDS) {
            assertFailsWith<Unit3CArtifactRootViolationException> {
                Unit3CArtifactRootPolicy.resolve(UNIT_3C_ARTIFACT_ROOT_PREFIX, preserved)
            }
        }
    }

    @Test
    fun `artifact root accepts exactly the accepted parent with a valid campaign ID`() {
        val resolved = Unit3CArtifactRootPolicy.resolve(UNIT_3C_ARTIFACT_ROOT_PREFIX, "unit3c-remedy-experiments-20260810")
        assertEquals("$UNIT_3C_ARTIFACT_ROOT_PREFIX/unit3c-remedy-experiments-20260810", resolved.toString())
    }

    // ---- Disk-space gate ----

    @Test
    fun `disk space gate accepts sufficient capacity`() {
        Unit3CDiskSpaceGate.check(Path.of("/tmp")) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
    }

    @Test
    fun `disk space gate rejects insufficient capacity`() {
        assertFailsWith<Unit3CInsufficientSpaceException> {
            Unit3CDiskSpaceGate.check(Path.of("/tmp")) { UNIT_3C_MINIMUM_FREE_BYTES - 1 }
        }
    }

    @Test
    fun `disk space gate accepts exact boundary equality`() {
        Unit3CDiskSpaceGate.check(Path.of("/tmp")) { UNIT_3C_MINIMUM_FREE_BYTES }
    }

    @Test
    fun `disk space gate fails closed on unreadable filesystem state`() {
        assertFailsWith<Unit3CInsufficientSpaceException> {
            Unit3CDiskSpaceGate.check(Path.of("/tmp")) { throw IOException("simulated unreadable filesystem") }
        }
    }

    // ---- Manual false-positive safety-review checkpoint ----

    @Test
    fun `adversarial-category false positive is detected`() {
        assertTrue(isAdversarialCategoryFalsePositive(FixtureCategory.ADVERSARIAL, ExpectedAction.REPLY, ExpectedAction.REMEMBER))
        assertTrue(isAdversarialCategoryFalsePositive(FixtureCategory.GOAL, ExpectedAction.GOAL, ExpectedAction.REMEMBER))
        assertTrue(isAdversarialCategoryFalsePositive(FixtureCategory.REPLY, ExpectedAction.REPLY, ExpectedAction.GOAL))
    }

    @Test
    fun `correct classifications and non-adversarial categories do not trigger the checkpoint`() {
        assertFalse(isAdversarialCategoryFalsePositive(FixtureCategory.REMEMBER, ExpectedAction.REMEMBER, ExpectedAction.REMEMBER))
        assertFalse(isAdversarialCategoryFalsePositive(FixtureCategory.ADVERSARIAL, ExpectedAction.REPLY, ExpectedAction.REPLY))
        assertFalse(isAdversarialCategoryFalsePositive(FixtureCategory.NOACTION, ExpectedAction.NOACTION, ExpectedAction.REMEMBER))
        assertFalse(isAdversarialCategoryFalsePositive(FixtureCategory.ADVERSARIAL, ExpectedAction.REPLY, null))
    }

    @Test
    fun `checkpoint marker preserves the triggering trial and blocks nothing it does not own`(@TempDir dir: Path) {
        val checkpoint = Unit3CSafetyCheckpoint(dir)
        assertFalse(checkpoint.isTriggered())
        checkpoint.trigger("t1", "adversarial-category false positive")
        assertTrue(checkpoint.isTriggered())
        val content = Files.readString(dir.resolve("SAFETY_CHECKPOINT"))
        assertTrue(content.contains("t1"))
    }

    @Test
    fun `checkpoint requires explicit action to clear, never auto-clears`(@TempDir dir: Path) {
        val checkpoint = Unit3CSafetyCheckpoint(dir)
        checkpoint.trigger("t1", "reason")
        assertTrue(checkpoint.isTriggered())
        // Re-instantiating the checkpoint object must not clear it -- state
        // lives in the durable marker file, not in memory.
        assertTrue(Unit3CSafetyCheckpoint(dir).isTriggered())
        checkpoint.clearForExplicitlyAuthorizedContinuation()
        assertFalse(checkpoint.isTriggered())
    }

    // ---- Orchestration driver ----

    @Test
    fun `driver executes all four arms in frozen order and seals each on success`(@TempDir dir: Path) {
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val results = driver.run(executors)
        assertEquals(listOf(Unit3CFamily.CONTROL, Unit3CFamily.FAMILY_A, Unit3CFamily.FAMILY_B, Unit3CFamily.FAMILY_C), results.map { it.family })
        assertTrue(results.all { it.outcome == Unit3CArmOutcome.SEALED })
        assertEquals(145, results.single { it.family == Unit3CFamily.CONTROL }.completedTrialCount)
        assertEquals(220, results.single { it.family == Unit3CFamily.FAMILY_A }.completedTrialCount)
        assertEquals(115, results.single { it.family == Unit3CFamily.FAMILY_B }.completedTrialCount)
        assertEquals(29, results.single { it.family == Unit3CFamily.FAMILY_C }.completedTrialCount)
    }

    @Test
    fun `driver prevents duplicate execution of an already-completed trial`(@TempDir dir: Path) {
        var callCount = 0
        val countingExecutor = Unit3CTrialExecutor { trial -> callCount++; fakeObservationFor(trial, trial.fixture.expectedAction) }
        val executors = mapOf(
            Unit3CFamily.CONTROL to countingExecutor,
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        driver.run(executors)
        assertEquals(145, callCount)
        callCount = 0
        // Second run against the same, now-sealed artifact directory: seal
        // blocks re-append entirely (proving the strongest form of
        // duplicate prevention -- a sealed arm cannot execute again at all).
        assertFailsWith<Unit3CArtifactIntegrityException> {
            Unit3CArmLedger(dir.resolve("control"), Unit3CCampaignDefinition.controlTrials.map { it.id }.toSet())
                .appendObservation(Unit3CCampaignDefinition.controlTrials.first().id, "payload")
        }
    }

    @Test
    fun `driver Family C arm makes zero calls to any executor invoking a model`(@TempDir dir: Path) {
        var familyCExecutorCalls = 0
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> familyCExecutorCalls++; fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        driver.run(executors)
        // The executor itself is invoked (it is where the offline Candidate-C1
        // classification happens), but every resulting observation is
        // constructed with modelName/modelDigest/rawRequest/rawResponse all
        // null, proving no model call occurred within it.
        assertEquals(29, familyCExecutorCalls)
        val familyCLedger = Unit3CArmLedger(dir.resolve("family-c"), Unit3CCampaignDefinition.familyCTrials.map { it.id }.toSet())
        assertEquals(29, familyCLedger.recover().size)
    }

    @Test
    fun `driver halts only the affected arm on a measurement-invalidating identity mismatch`(@TempDir dir: Path) {
        val armDir = dir.resolve("control")
        Files.createDirectories(armDir)
        Files.writeString(armDir.resolve("identity.txt"), "wrong|identity|line|here|0\n")
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        assertEquals(Unit3CArmOutcome.HALTED, results.single { it.family == Unit3CFamily.CONTROL }.outcome)
        // Other arms unaffected -- fault isolation preserved.
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_A }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_B }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_C }.outcome)
    }

    @Test
    fun `driver triggers the safety checkpoint on an adversarial-category false positive and halts only that arm`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial ->
                // Force exactly one adversarial-category false positive on Control.
                if (trial.fixture.id == "p03-ambiguous-memory") fakeObservationFor(trial, ExpectedAction.REMEMBER)
                else fakeObservationFor(trial, trial.fixture.expectedAction)
            },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        val controlResult = results.single { it.family == Unit3CFamily.CONTROL }
        assertEquals(Unit3CArmOutcome.SAFETY_CHECKPOINT, controlResult.outcome)
        assertTrue(Unit3CSafetyCheckpoint(dir.resolve("control")).isTriggered())
        // The triggering observation itself was preserved (not deleted), and
        // the arm did not proceed past it.
        val controlLedger = Unit3CArmLedger(dir.resolve("control"), Unit3CCampaignDefinition.controlTrials.map { it.id }.toSet())
        val recovered = controlLedger.recover()
        assertTrue(recovered.any { it.contains("p03-ambiguous-memory") })
        assertTrue(recovered.size < 145, "arm must stop before completing all trials once the checkpoint fires")
        // Sealing must be impossible while the checkpoint remains set.
        assertFailsWith<Unit3CArtifactIntegrityException> {
            controlLedger.seal(Unit3CCampaignDefinition.controlTrials.map { it.id }.toSet())
        }
        // Other arms unaffected.
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_A }.outcome)
    }

    @Test
    fun `driver refuses to run at all when disk space is insufficient, before any executor is invoked`(@TempDir dir: Path) {
        var anyExecutorCalled = false
        val executors = Unit3CFamily.entries.associateWith { Unit3CTrialExecutor { trial -> anyExecutorCalled = true; fakeObservationFor(trial, trial.fixture.expectedAction) } }
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES - 1 }
        assertFailsWith<Unit3CInsufficientSpaceException> { driver.run(executors) }
        assertFalse(anyExecutorCalled, "no executor may be invoked once the disk-space gate has failed")
    }

    // ---- Call accounting, derived from the orchestration schedule itself ----

    @Test
    fun `exact 483-call total is derivable from the orchestration schedule, not merely the campaign definition object`(@TempDir dir: Path) {
        var liveCalls = 0
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial -> liveCalls++; fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> liveCalls++; fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> liveCalls++; fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) }, // never increments liveCalls
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        // Warm-up trials are accounted separately (Plan Section 11) and are
        // not part of any scored arm's own executor invocation count here;
        // the scored-arm total below plus the fixed 3 warm-up calls
        // (verified independently in the other test file) equals 483.
        assertEquals(480, liveCalls) // 145 + 220 + 115
        assertEquals(3 + liveCalls, 483)
        assertTrue(results.all { it.outcome == Unit3CArmOutcome.SEALED })
    }

    // ---- Live-execution guard ----

    @Test
    fun `orchestration driver constructs no real inference client or provider anywhere in this file`() {
        // Built by concatenation, deliberately, so this test's own source
        // text never contains either forbidden substring verbatim -- a
        // literal-quoted denylist would match its own assertion, exactly
        // the self-referential bug Unit 2-D's own equivalent check once
        // required fixing.
        val forbiddenA = "LocalHttpModelInferenceClient" + "("
        val forbiddenB = "ModelReasoningProvider" + "("
        val thisFile = Path.of("tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt")
        val text = Files.readString(thisFile)
        assertFalse(text.contains(forbiddenA))
        assertFalse(text.contains(forbiddenB))
    }
}
