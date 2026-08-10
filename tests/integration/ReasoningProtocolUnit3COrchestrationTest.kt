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

enum class Unit3CArmOutcome { SEALED, HALTED, SAFETY_CHECKPOINT, CAMPAIGN_HALT_WARMUP_TRANSPORT, NOT_ATTEMPTED_CAMPAIGN_HALTED }

// ============================================================
// Timeout + durability governance (Timeout and Durability Scope Lock
// Amendment / Implementation Plan Amendment; Scored-Trial Timeout
// Semantics Determination). A live call either completes (an
// [Unit3CObservation] is durably appended, unchanged) or terminates via a
// transport-layer exception, in which case NO observation is ever
// fabricated -- the executor throws [Unit3CTransportException] instead of
// returning a result, and the orchestration layer records a separate,
// durable terminal-timeout record. Classification is derived from the
// actual exception type the JVM/coroutines runtime reports, never from a
// message string: [kotlinx.coroutines.TimeoutCancellationException] means
// the request was transmitted and the provider remained reachable while
// the client's own timeout budget elapsed (sub-case 1 of the governed
// determination); any [java.io.IOException] means a transport- or
// provider-level failure (sub-cases 2/3, not distinguishable from each
// other at this layer, per the Determination's own accepted reasoning for
// a loopback-only endpoint); anything else is ambiguous (sub-case 4) and
// fails closed exactly like sub-cases 2/3.
// ============================================================

enum class Unit3CTransportClassification { MODEL_TIMEOUT, TRANSPORT_OR_PROVIDER_FAILURE, AMBIGUOUS }

/** Thrown by a live-calling [Unit3CTrialExecutor] instead of returning an
 * [Unit3CObservation] when no model response was ever received. Never
 * caught and converted into a fabricated GOAL/REPLY/REMEMBER/NOACTION
 * result anywhere in this file. */
class Unit3CTransportException(
    val classification: Unit3CTransportClassification,
    val elapsedNanos: Long,
    val transportDetail: String,
    cause: Throwable,
) : RuntimeException(cause.message, cause)

/** Durable record written before every live model call, strictly before
 * transmission (Timeout + Durability Plan Amendment Section 3's own
 * intent-record schema). */
data class Unit3CIntentRecord(
    val callId: String,
    val campaignId: String,
    val family: Unit3CFamily,
    val fixtureId: String,
    val trialSequence: Int,
    val expectedAction: ExpectedAction,
    val modelName: String,
    val modelDigest: String,
    val repositoryCommit: String,
    val promptIdentity: String,
    val timeoutMs: Long,
    val inferenceConfigIdentity: String,
)

/** Durable terminal-timeout observation (Timeout + Durability Plan
 * Amendment Section 3's own terminal-timeout schema). Written if and only
 * if a call's own intent record exists and no [Unit3CObservation] was
 * durably persisted for that call. `parserResult`/`semanticClassification`
 * do not appear in this record at all -- there is no field to fabricate a
 * GOAL/REPLY/REMEMBER/NOACTION value into. */
data class Unit3CTimeoutRecord(
    val callId: String,
    val campaignId: String,
    val family: Unit3CFamily,
    val fixtureId: String,
    val trialSequence: Int,
    val timeoutMs: Long,
    val elapsedNanos: Long,
    val terminalClassification: Unit3CTransportClassification,
    val responseBytesReceived: Boolean,
    val transportDetail: String,
    val modelName: String,
    val modelDigest: String,
    val inferenceConfigIdentity: String,
    val continuationDecision: String,
    val checkpointStatus: String,
)

/** Structurally derives the same prompt-identity vocabulary
 * [Unit3CLiveEntryPoint]'s own executor-wiring closures already use
 * (`control`, `family-a-decision`, `family-a-render`, `family-b-candidate`),
 * directly from [Unit3CTrial]'s own fields -- so the orchestration layer
 * can populate a durable intent record without depending on the executor
 * to expose its own internal prompt-selection logic. Never called for
 * Family C, which makes no model call and therefore has no prompt identity
 * or intent record at all. */
private fun promptIdentityFor(trial: Unit3CTrial): String = when (trial.family) {
    Unit3CFamily.CONTROL -> "control"
    Unit3CFamily.FAMILY_A -> if (trial.subStep == Unit3CSubStep.DECISION) "family-a-decision" else "family-a-render"
    Unit3CFamily.FAMILY_B -> "family-b-candidate"
    Unit3CFamily.FAMILY_C -> error("Family C makes no model call; no prompt identity applies")
}

private fun buildIntentRecord(trial: Unit3CTrial, campaignId: String, identity: Unit3CIdentity): Unit3CIntentRecord =
    Unit3CIntentRecord(
        callId = trial.id,
        campaignId = campaignId,
        family = trial.family,
        fixtureId = trial.fixture.id,
        trialSequence = trial.attempt,
        expectedAction = trial.fixture.expectedAction,
        modelName = identity.modelName,
        modelDigest = identity.modelDigest,
        repositoryCommit = identity.repositoryCommit,
        promptIdentity = promptIdentityFor(trial),
        timeoutMs = identity.timeoutMs,
        inferenceConfigIdentity = "default-ollama-request-shape",
    )

private fun buildTimeoutRecord(
    trial: Unit3CTrial,
    campaignId: String,
    identity: Unit3CIdentity,
    failure: Unit3CTransportException,
    continuationDecision: String,
    checkpointStatus: String,
): Unit3CTimeoutRecord =
    Unit3CTimeoutRecord(
        callId = trial.id,
        campaignId = campaignId,
        family = trial.family,
        fixtureId = trial.fixture.id,
        trialSequence = trial.attempt,
        timeoutMs = identity.timeoutMs,
        elapsedNanos = failure.elapsedNanos,
        terminalClassification = failure.classification,
        // The client never observes a partial byte count from
        // HttpClient.sendAsync(...).BodyHandlers.ofString() -- a
        // response either fully arrives or the request is cancelled
        // with nothing readable. This is a genuine limitation of the
        // current transport layer, stated here rather than guessed at.
        responseBytesReceived = false,
        transportDetail = failure.transportDetail,
        modelName = identity.modelName,
        modelDigest = identity.modelDigest,
        inferenceConfigIdentity = "default-ollama-request-shape",
        continuationDecision = continuationDecision,
        checkpointStatus = checkpointStatus,
    )

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
        // The campaign-specific artifactRoot does not exist yet on a first
        // run -- nothing creates it before this gate must run (the gate is,
        // by design, the very first check, before any ledger or directory
        // is touched). Files.getFileStore requires an existing path, so the
        // gate is checked against the already-existing, durable *parent*
        // (the same filesystem/mount the campaign directory will be created
        // under) rather than the not-yet-created campaign directory itself.
        val spaceCheckTarget = requireNotNull(artifactRoot.parent) {
            "artifact root must have a parent; got $artifactRoot"
        }
        Unit3CDiskSpaceGate.check(spaceCheckTarget, usableSpace)
        // Sequential with early exit, not an unconditional map: every
        // outcome this driver produced before this task's own governance
        // (SEALED, HALTED, SAFETY_CHECKPOINT) still lets every arm run
        // independently, exactly as already tested. The one new outcome,
        // CAMPAIGN_HALT_WARMUP_TRANSPORT, is the sole trigger for skipping
        // the remaining arms entirely -- a genuine model-side timeout or
        // transport/provider failure during warm-up means the shared live
        // bridge Family A/B/C also depend on could not be verified at all,
        // which is a strictly stronger reason to stop than a per-arm
        // identity mismatch (Scope Lock Amendment Section 3).
        val results = mutableListOf<Unit3CArmResult>()
        var campaignHalted = false
        for (family in UNIT_3C_ARM_ORDER) {
            if (campaignHalted) {
                results += Unit3CArmResult(family, Unit3CArmOutcome.NOT_ATTEMPTED_CAMPAIGN_HALTED, 0)
                continue
            }
            val result = runArm(family, executors.getValue(family))
            results += result
            if (result.outcome == Unit3CArmOutcome.CAMPAIGN_HALT_WARMUP_TRANSPORT) {
                campaignHalted = true
            }
        }
        return results
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

    /** Executes the three frozen, shared, unscored warm-up trials (Plan
     * Section 11's own "Warm-up (shared, unscored)" row) before Control's
     * own scored trials -- and before Family A/B, which share the same
     * live bridge -- verifying the bridge is genuinely reachable before any
     * scored call is attempted. Lives in its own sub-ledger
     * (`control/warmup/`), structurally separate from Control's own scored
     * `control/` ledger, so warm-up calls can never be counted among, or
     * confused with, the 145 scored Control observations. Reuses
     * [Unit3CArmLedger] unchanged, so warm-ups inherit the exact same
     * exact-once, duplicate-prevention, and crash-recovery guarantees
     * already proven for every other ledger in this campaign. A warm-up
     * failure (any [Unit3CArtifactIntegrityException]) halts only the
     * Control arm -- Family A/B/C remain independently fault-isolated,
     * exactly as the frozen Scope Lock's own arm-isolation requirement
     * already establishes for every other measurement-invalidating defect. */
    private fun runWarmups(executor: Unit3CTrialExecutor): Unit3CArmOutcome {
        val trials = Unit3CCampaignDefinition.warmupTrials
        val warmupDirectory = artifactRoot.resolve(armDirectoryName(Unit3CFamily.CONTROL)).resolve("warmup")
        val ledger = Unit3CArmLedger(warmupDirectory, trials.map { it.id }.toSet())
        try {
            ledger.checkIdentity(identity)
            val completed = ledger.recover().toMutableSet()
            for (trial in trials) {
                if (trial.id in completed) continue
                ledger.recordIntent(buildIntentRecord(trial, campaignId, identity))
                val observation = try {
                    executor.execute(trial)
                } catch (e: Unit3CTransportException) {
                    // Timeout + Durability Scope Lock Amendment Section 3: a
                    // timeout during any governed warm-up is measurement-
                    // invalidating and halts the whole campaign, not merely
                    // this arm -- durably recorded first, never silently
                    // retried, never classified as remedy-performance
                    // evidence. Every sub-case (model timeout, transport/
                    // provider failure, ambiguous) receives the same
                    // campaign-halting treatment during warm-up specifically,
                    // because warm-up's own purpose is to verify the shared
                    // bridge Family A/B/C also depend on.
                    ledger.recordTimeout(
                        buildTimeoutRecord(
                            trial, campaignId, identity, e,
                            continuationDecision = "CAMPAIGN_HALTED",
                            checkpointStatus = "NOT_CHECKPOINTED",
                        ),
                    )
                    return Unit3CArmOutcome.CAMPAIGN_HALT_WARMUP_TRANSPORT
                }
                ledger.appendObservation(trial.id, encodeObservation(observation))
                completed += trial.id
            }
            ledger.seal(trials.map { it.id }.toSet())
            return Unit3CArmOutcome.SEALED
        } catch (e: Unit3CArtifactIntegrityException) {
            return Unit3CArmOutcome.HALTED
        }
    }

    private fun runArm(family: Unit3CFamily, executor: Unit3CTrialExecutor): Unit3CArmResult {
        if (family == Unit3CFamily.CONTROL) {
            val warmupOutcome = runWarmups(executor)
            if (warmupOutcome != Unit3CArmOutcome.SEALED) {
                // The shared live bridge could not be verified -- do not
                // proceed to Control's own scored trials at all.
                return Unit3CArmResult(family, warmupOutcome, 0)
            }
        }
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
                if (trial.makesModelCall) {
                    ledger.recordIntent(buildIntentRecord(trial, campaignId, identity))
                }
                val observation = try {
                    executor.execute(trial) // Family C: zero model calls, by construction of its own executor
                } catch (e: Unit3CTransportException) {
                    when (e.classification) {
                        Unit3CTransportClassification.MODEL_TIMEOUT -> {
                            // Scored-Trial Timeout Semantics Determination
                            // Section 5: only this trial terminates; the arm
                            // continues; the campaign continues. Never
                            // scored as a semantic answer, never retried.
                            ledger.recordTimeout(
                                buildTimeoutRecord(
                                    trial, campaignId, identity, e,
                                    continuationDecision = "ARM_CONTINUED",
                                    checkpointStatus = "NOT_CHECKPOINTED",
                                ),
                            )
                            completed += trial.id
                            continue
                        }
                        Unit3CTransportClassification.TRANSPORT_OR_PROVIDER_FAILURE, Unit3CTransportClassification.AMBIGUOUS -> {
                            // Measurement-invalidating (Determination Section
                            // 4, sub-cases 2-4): halts this arm only, per the
                            // same existing precedent as every other
                            // measurement-invalidating cause -- never the
                            // whole campaign for a scored-trial failure.
                            ledger.recordTimeout(
                                buildTimeoutRecord(
                                    trial, campaignId, identity, e,
                                    continuationDecision = "ARM_HALTED",
                                    checkpointStatus = "NOT_CHECKPOINTED",
                                ),
                            )
                            throw Unit3CArtifactIntegrityException(
                                "measurement-invalidating transport failure for trial ${trial.id}: ${e.classification}",
                            )
                        }
                    }
                }
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

    @Test
    fun `driver disk-space gate checks the durable parent, not the not-yet-created campaign directory itself`(@TempDir parent: Path) {
        val notYetCreatedCampaignDir = parent.resolve("unit3c-remedy-experiments-not-yet-created")
        assertFalse(Files.exists(notYetCreatedCampaignDir), "this test's own premise requires the campaign directory to not yet exist")
        var anyExecutorCalled = false
        val executors = Unit3CFamily.entries.associateWith {
            Unit3CTrialExecutor { trial -> anyExecutorCalled = true; fakeObservationFor(trial, trial.fixture.expectedAction) }
        }
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", notYetCreatedCampaignDir, sampleIdentity()) { checkedPath ->
            assertEquals(parent, checkedPath, "the gate must be checked against the existing parent, never the not-yet-created campaign directory")
            UNIT_3C_MINIMUM_FREE_BYTES + 1
        }
        driver.run(executors)
        assertTrue(anyExecutorCalled, "once the disk-space gate correctly passes, execution must proceed")
    }

    @Test
    fun `driver's real default disk-space check succeeds against a not-yet-created campaign directory when its parent exists with sufficient space`(@TempDir parent: Path) {
        // Exercises the real Files.getFileStore(...).usableSpace default
        // (no fake lambda), against a genuinely non-existent child of a
        // real, existing directory -- reproducing, entirely offline, the
        // exact condition that halted the first genuine live-execution
        // attempt (a first-ever campaign directory that does not yet
        // exist), and proving it is now handled correctly.
        val notYetCreatedCampaignDir = parent.resolve("unit3c-remedy-experiments-test")
        assertFalse(Files.exists(notYetCreatedCampaignDir))
        val executors = Unit3CFamily.entries.associateWith {
            Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) }
        }
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", notYetCreatedCampaignDir, sampleIdentity())
        val results = driver.run(executors)
        assertEquals(4, results.size)
        assertTrue(results.all { it.outcome == Unit3CArmOutcome.SEALED }, "all four arms must complete once the disk-space gate correctly passes against the real filesystem")
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
        // 148, not 145: the same Control executor now also serves the 3
        // warm-up trials (Section on warm-up wiring), which is correct --
        // this test only proves no cell is executed *more* than once.
        assertEquals(148, callCount)
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
    fun `exact 483-call total is genuinely executed by the orchestration driver, including the 3 warm-ups`(@TempDir dir: Path) {
        var liveCalls = 0
        var warmupCalls = 0
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial ->
                liveCalls++
                if (trial.fixture.id == "warmup-acknowledgement") warmupCalls++
                fakeObservationFor(trial, trial.fixture.expectedAction)
            },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> liveCalls++; fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> liveCalls++; fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) }, // never increments liveCalls
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        // The driver's own executor-invocation count now genuinely includes
        // the 3 warm-ups (148 = 3 + 145 for Control; 220 for Family A; 115
        // for Family B; 0 for Family C) -- not a constant added after the
        // fact, but the real count of calls the driver actually issued.
        assertEquals(3, warmupCalls)
        assertEquals(483, liveCalls) // 3 + 145 + 220 + 115
        assertTrue(results.all { it.outcome == Unit3CArmOutcome.SEALED })
        // Control's own reported completed-trial count remains 145 (scored
        // trials only) -- the 3 warm-ups are not counted among it, proving
        // warm-ups do not contaminate the scored observation count.
        assertEquals(145, results.single { it.family == Unit3CFamily.CONTROL }.completedTrialCount)
    }

    @Test
    fun `all three warm-ups are present, exactly once, in frozen order, distinguishable from scored observations`(@TempDir dir: Path) {
        val executedWarmupIds = mutableListOf<String>()
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial ->
                if (trial.fixture.id == "warmup-acknowledgement") executedWarmupIds += trial.id
                fakeObservationFor(trial, trial.fixture.expectedAction)
            },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        driver.run(executors)
        assertEquals(Unit3CCampaignDefinition.warmupTrials.map { it.id }, executedWarmupIds, "warm-ups must execute exactly once each, in frozen order")
        // Distinguishable: warm-up observations live in their own directory,
        // structurally separate from Control's own scored raw.jsonl.
        val warmupLedgerDir = dir.resolve("control").resolve("warmup")
        val controlLedgerDir = dir.resolve("control")
        assertTrue(Files.exists(warmupLedgerDir.resolve("raw.jsonl")))
        assertTrue(Files.exists(controlLedgerDir.resolve("raw.jsonl")))
        val warmupRaw = Files.readString(warmupLedgerDir.resolve("raw.jsonl"))
        val controlRaw = Files.readString(controlLedgerDir.resolve("raw.jsonl"))
        assertEquals(3, warmupRaw.lines().count { it.isNotBlank() })
        assertEquals(145, controlRaw.lines().count { it.isNotBlank() })
        assertTrue(controlRaw.lines().none { it.contains("warmup-acknowledgement") }, "scored Control ledger must never contain a warm-up trial ID")
    }

    @Test
    fun `crash recovery cannot duplicate already-completed warm-ups`(@TempDir dir: Path) {
        var warmupCallCount = 0
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial ->
                if (trial.fixture.id == "warmup-acknowledgement") warmupCallCount++
                fakeObservationFor(trial, trial.fixture.expectedAction)
            },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        driver.run(executors)
        assertEquals(3, warmupCallCount)
        // Simulate a second, independent driver instance resuming against
        // the same, already-completed artifact root (a crash-recovery
        // scenario where the process restarts after full completion). The
        // correct, safe behavior is idempotent no-op recovery -- every
        // trial ID is already in the recovered completed-set, so the
        // executor is never invoked again for any of them, proving warm-ups
        // (like every other trial) cannot be silently duplicated.
        val secondDriver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val secondResults = secondDriver.run(
            mapOf(
                Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial -> if (trial.fixture.id == "warmup-acknowledgement") warmupCallCount++; fakeObservationFor(trial, trial.fixture.expectedAction) },
                Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
                Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
                Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
            ),
        )
        assertEquals(3, warmupCallCount, "the executor must not be invoked again for any already-completed warm-up trial")
        assertTrue(secondResults.all { it.outcome == Unit3CArmOutcome.SEALED }, "idempotent recovery over an already-complete campaign must not fail")
    }

    @Test
    fun `a warm-up failure halts only the Control arm, not Family A, B, or C`(@TempDir dir: Path) {
        // Corrupt the warm-up sub-ledger's recorded identity before any run,
        // simulating a measurement-invalidating defect specific to the
        // shared bridge-verification step.
        val warmupDir = dir.resolve("control").resolve("warmup")
        Files.createDirectories(warmupDir)
        Files.writeString(warmupDir.resolve("identity.txt"), "wrong|identity|line|here|0\n")
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        assertEquals(Unit3CArmOutcome.HALTED, results.single { it.family == Unit3CFamily.CONTROL }.outcome)
        assertEquals(0, results.single { it.family == Unit3CFamily.CONTROL }.completedTrialCount)
        // Control's own scored ledger must never have been touched -- the
        // warm-up gate fails before any scored call is attempted.
        assertFalse(Files.exists(dir.resolve("control").resolve("raw.jsonl")))
        // Family A/B/C remain independently fault-isolated and still seal.
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_A }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_B }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_C }.outcome)
    }

    @Test
    fun `campaign cannot silently skip warm-ups -- omitting the CONTROL executor is impossible by type, and warm-ups run unconditionally`(@TempDir dir: Path) {
        var warmupRan = false
        val executors = mapOf(
            Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial ->
                if (trial.fixture.id == "warmup-acknowledgement") warmupRan = true
                fakeObservationFor(trial, trial.fixture.expectedAction)
            },
            Unit3CFamily.FAMILY_A to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_B to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) },
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        driver.run(executors)
        assertTrue(warmupRan, "warm-ups must run unconditionally as part of the Control arm -- there is no flag or configuration path that skips them")
    }

    // ---- Timeout + durability governance ----

    private fun simulatedTransportException(classification: Unit3CTransportClassification): Unit3CTransportException =
        Unit3CTransportException(classification, elapsedNanos = 90_000_000_000L, transportDetail = "SimulatedFailure", cause = RuntimeException("simulated"))

    private fun timeoutOnceExecutor(
        classification: Unit3CTransportClassification,
        matches: (Unit3CTrial) -> Boolean,
        onCall: (Unit3CTrial) -> Unit = {},
    ): Unit3CTrialExecutor = Unit3CTrialExecutor { trial ->
        onCall(trial)
        if (matches(trial)) throw simulatedTransportException(classification)
        fakeObservationFor(trial, trial.fixture.expectedAction)
    }

    private fun allSucceedExecutor(): Unit3CTrialExecutor = Unit3CTrialExecutor { trial -> fakeObservationFor(trial, trial.fixture.expectedAction) }

    private fun isFirstWarmup(trial: Unit3CTrial): Boolean = trial.fixture.id == "warmup-acknowledgement" && trial.attempt == 1
    private fun isFirstControlScored(trial: Unit3CTrial): Boolean = trial.family == Unit3CFamily.CONTROL && trial.fixture.id == "r01-direct" && trial.attempt == 1

    @Test
    fun `a genuine warm-up model timeout halts the whole campaign, not merely the Control arm`(@TempDir dir: Path) {
        var familyAAttempted = false
        val executors = mapOf(
            Unit3CFamily.CONTROL to timeoutOnceExecutor(Unit3CTransportClassification.MODEL_TIMEOUT, ::isFirstWarmup),
            Unit3CFamily.FAMILY_A to timeoutOnceExecutor(Unit3CTransportClassification.MODEL_TIMEOUT, { false }, { familyAAttempted = true }),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        assertEquals(Unit3CArmOutcome.CAMPAIGN_HALT_WARMUP_TRANSPORT, results.single { it.family == Unit3CFamily.CONTROL }.outcome)
        assertEquals(Unit3CArmOutcome.NOT_ATTEMPTED_CAMPAIGN_HALTED, results.single { it.family == Unit3CFamily.FAMILY_A }.outcome)
        assertEquals(Unit3CArmOutcome.NOT_ATTEMPTED_CAMPAIGN_HALTED, results.single { it.family == Unit3CFamily.FAMILY_B }.outcome)
        assertEquals(Unit3CArmOutcome.NOT_ATTEMPTED_CAMPAIGN_HALTED, results.single { it.family == Unit3CFamily.FAMILY_C }.outcome)
        assertFalse(familyAAttempted, "no arm after a campaign-halting warm-up failure may have its own executor invoked at all")
        // Control's own scored ledger must never have been touched.
        assertFalse(Files.exists(dir.resolve("control").resolve("raw.jsonl")))
        // Durable timeout record exists for the warm-up trial, never a fabricated observation.
        val warmupLedger = Unit3CArmLedger(dir.resolve("control").resolve("warmup"), Unit3CCampaignDefinition.warmupTrials.map { it.id }.toSet())
        val warmupTimeoutLine = Files.readString(dir.resolve("control").resolve("warmup").resolve("timeouts.jsonl"))
        assertTrue(warmupTimeoutLine.contains("MODEL_TIMEOUT"))
        assertTrue(warmupTimeoutLine.contains("CAMPAIGN_HALTED"))
        assertFalse(warmupLedger.isSealed())
    }

    @Test
    fun `a genuine warm-up transport failure halts the whole campaign identically to a model timeout`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to timeoutOnceExecutor(Unit3CTransportClassification.TRANSPORT_OR_PROVIDER_FAILURE, ::isFirstWarmup),
            Unit3CFamily.FAMILY_A to allSucceedExecutor(),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        assertEquals(Unit3CArmOutcome.CAMPAIGN_HALT_WARMUP_TRANSPORT, results.single { it.family == Unit3CFamily.CONTROL }.outcome)
        assertTrue(results.filter { it.family != Unit3CFamily.CONTROL }.all { it.outcome == Unit3CArmOutcome.NOT_ATTEMPTED_CAMPAIGN_HALTED })
        val warmupTimeoutLine = Files.readString(dir.resolve("control").resolve("warmup").resolve("timeouts.jsonl"))
        assertTrue(warmupTimeoutLine.contains("TRANSPORT_OR_PROVIDER_FAILURE"))
    }

    @Test
    fun `an ambiguous warm-up terminal state halts the whole campaign and fails closed`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to timeoutOnceExecutor(Unit3CTransportClassification.AMBIGUOUS, ::isFirstWarmup),
            Unit3CFamily.FAMILY_A to allSucceedExecutor(),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        assertEquals(Unit3CArmOutcome.CAMPAIGN_HALT_WARMUP_TRANSPORT, results.single { it.family == Unit3CFamily.CONTROL }.outcome)
        val warmupTimeoutLine = Files.readString(dir.resolve("control").resolve("warmup").resolve("timeouts.jsonl"))
        assertTrue(warmupTimeoutLine.contains("AMBIGUOUS"))
    }

    @Test
    fun `a scored Control-trial model timeout terminates only that trial -- the arm and campaign continue`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to timeoutOnceExecutor(Unit3CTransportClassification.MODEL_TIMEOUT, ::isFirstControlScored),
            Unit3CFamily.FAMILY_A to allSucceedExecutor(),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        // Control still SEALS -- one scored trial resolved as a timeout,
        // the other 144 completed normally, all 145 accounted for.
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.CONTROL }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_A }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_B }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_C }.outcome)
        val controlRaw = Files.readAllLines(dir.resolve("control").resolve("raw.jsonl")).count { it.isNotBlank() }
        assertEquals(144, controlRaw, "144 of Control's 145 scored trials completed normally")
        val timeoutLines = Files.readAllLines(dir.resolve("control").resolve("timeouts.jsonl")).filter { it.isNotBlank() }
        assertEquals(1, timeoutLines.size)
        assertTrue(timeoutLines.single().contains("MODEL_TIMEOUT"))
        assertTrue(timeoutLines.single().contains("ARM_CONTINUED"))
        assertTrue(timeoutLines.single().contains("control/r01-direct/main/01"))
        // No fabricated semantic action anywhere in the timeout record --
        // the type itself has no field capable of holding one.
        for (forbidden in listOf("GOAL", "REPLY", "REMEMBER", "NOACTION")) {
            assertFalse(timeoutLines.single().contains("\"actualAction\":\"$forbidden\""))
        }
    }

    @Test
    fun `a scored Family A-trial model timeout does not halt Family A or block Family B`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to allSucceedExecutor(),
            Unit3CFamily.FAMILY_A to timeoutOnceExecutor(
                Unit3CTransportClassification.MODEL_TIMEOUT,
                matches = { it.family == Unit3CFamily.FAMILY_A && it.fixture.id == "r01-direct" && it.subStep == Unit3CSubStep.DECISION && it.attempt == 1 },
            ),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        assertTrue(results.all { it.outcome == Unit3CArmOutcome.SEALED })
        val familyARaw = Files.readAllLines(dir.resolve("family-a").resolve("raw.jsonl")).count { it.isNotBlank() }
        assertEquals(219, familyARaw, "219 of Family A's 220 scored trials completed normally")
    }

    @Test
    fun `a scored Family B-trial model timeout does not halt Family B`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to allSucceedExecutor(),
            Unit3CFamily.FAMILY_A to allSucceedExecutor(),
            Unit3CFamily.FAMILY_B to timeoutOnceExecutor(
                Unit3CTransportClassification.MODEL_TIMEOUT,
                matches = { it.family == Unit3CFamily.FAMILY_B && it.fixture.id == "r01-direct" && it.attempt == 1 },
            ),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        assertTrue(results.all { it.outcome == Unit3CArmOutcome.SEALED })
        val familyBRaw = Files.readAllLines(dir.resolve("family-b").resolve("raw.jsonl")).count { it.isNotBlank() }
        assertEquals(114, familyBRaw, "114 of Family B's 115 scored trials completed normally")
    }

    @Test
    fun `a scored-trial transport failure is measurement-invalidating and halts only that arm`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to timeoutOnceExecutor(Unit3CTransportClassification.TRANSPORT_OR_PROVIDER_FAILURE, ::isFirstControlScored),
            Unit3CFamily.FAMILY_A to allSucceedExecutor(),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        assertEquals(Unit3CArmOutcome.HALTED, results.single { it.family == Unit3CFamily.CONTROL }.outcome)
        // Other arms unaffected -- scored-trial infrastructure failures halt
        // only the affected arm, never the whole campaign (unlike warm-up).
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_A }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_B }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_C }.outcome)
    }

    @Test
    fun `a scored-trial ambiguous terminal state fails closed and halts only that arm`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to timeoutOnceExecutor(Unit3CTransportClassification.AMBIGUOUS, ::isFirstControlScored),
            Unit3CFamily.FAMILY_A to allSucceedExecutor(),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val results = driver.run(executors)
        assertEquals(Unit3CArmOutcome.HALTED, results.single { it.family == Unit3CFamily.CONTROL }.outcome)
        assertEquals(Unit3CArmOutcome.SEALED, results.single { it.family == Unit3CFamily.FAMILY_A }.outcome)
    }

    @Test
    fun `intent record exists for every scored Control trial before either a raw observation or a timeout record is written`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to timeoutOnceExecutor(Unit3CTransportClassification.MODEL_TIMEOUT, ::isFirstControlScored),
            Unit3CFamily.FAMILY_A to allSucceedExecutor(),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        driver.run(executors)
        val intentIds = Files.readAllLines(dir.resolve("control").resolve("intent.jsonl")).filter { it.isNotBlank() }
        assertEquals(145, intentIds.size, "an intent record exists for every one of Control's 145 scored trials, including the one that timed out")
        val rawIds = Files.readAllLines(dir.resolve("control").resolve("raw.jsonl")).filter { it.isNotBlank() }
        val timeoutIds = Files.readAllLines(dir.resolve("control").resolve("timeouts.jsonl")).filter { it.isNotBlank() }
        assertEquals(145, rawIds.size + timeoutIds.size, "every intent resolves to exactly a raw observation or a timeout record")
    }

    @Test
    fun `Family C never receives an intent record, since it makes no model call`(@TempDir dir: Path) {
        val executors = mapOf(
            Unit3CFamily.CONTROL to allSucceedExecutor(),
            Unit3CFamily.FAMILY_A to allSucceedExecutor(),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val driver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        driver.run(executors)
        assertFalse(Files.exists(dir.resolve("family-c").resolve("intent.jsonl")), "Family C makes zero model calls; no intent record of any kind may exist for it")
    }

    @Test
    fun `a timed-out trial is never automatically retried on crash-recovery resume`(@TempDir dir: Path) {
        var controlExecutorCallCount = 0
        val firstRunExecutors = mapOf(
            Unit3CFamily.CONTROL to timeoutOnceExecutor(Unit3CTransportClassification.MODEL_TIMEOUT, ::isFirstControlScored) { controlExecutorCallCount++ },
            Unit3CFamily.FAMILY_A to allSucceedExecutor(),
            Unit3CFamily.FAMILY_B to allSucceedExecutor(),
            Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
        )
        val firstDriver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        firstDriver.run(executors = firstRunExecutors)
        assertEquals(148, controlExecutorCallCount, "3 warm-ups + 145 scored, including the one that timed out")
        // A second, independent driver instance resumes over the same,
        // already-sealed root -- the timed-out trial must not be re-issued.
        var secondRunControlCallCount = 0
        val secondDriver = Unit3COrchestrationDriver("unit3c-remedy-experiments-test", dir, sampleIdentity()) { UNIT_3C_MINIMUM_FREE_BYTES + 1 }
        val secondResults = secondDriver.run(
            mapOf(
                Unit3CFamily.CONTROL to Unit3CTrialExecutor { trial -> secondRunControlCallCount++; fakeObservationFor(trial, trial.fixture.expectedAction) },
                Unit3CFamily.FAMILY_A to allSucceedExecutor(),
                Unit3CFamily.FAMILY_B to allSucceedExecutor(),
                Unit3CFamily.FAMILY_C to Unit3CTrialExecutor { trial -> fakeObservationFor(trial, null) },
            ),
        )
        assertEquals(0, secondRunControlCallCount, "the executor must not be invoked again for any already-resolved trial, including the timed-out one")
        assertTrue(secondResults.all { it.outcome == Unit3CArmOutcome.SEALED }, "idempotent recovery over an already-fully-resolved campaign must not fail")
    }

    @Test
    fun `an intent record without any resolution is ambiguous and fails closed on recovery`(@TempDir dir: Path) {
        val ledger = Unit3CArmLedger(dir, setOf("t1", "t2"))
        ledger.recordIntent(
            Unit3CIntentRecord(
                callId = "t1", campaignId = "c", family = Unit3CFamily.CONTROL, fixtureId = "f",
                trialSequence = 1, expectedAction = ExpectedAction.REPLY, modelName = "qwen2.5-coder:7b",
                modelDigest = "d", repositoryCommit = "r", promptIdentity = "control", timeoutMs = 90_000L,
                inferenceConfigIdentity = "default-ollama-request-shape",
            ),
        )
        // No raw observation and no timeout record were ever written for
        // "t1" -- an ambiguous terminal transport state (D), which must
        // fail closed, not be silently treated as never-attempted or as
        // complete.
        assertFailsWith<Unit3CArtifactIntegrityException> { ledger.recover() }
    }

    @Test
    fun `never-transmitted trial IDs remain distinguishable from transmitted ones via intent presence`(@TempDir dir: Path) {
        val ledger = Unit3CArmLedger(dir, setOf("t1", "t2"))
        assertFalse(ledger.hasIntent("t1"))
        ledger.recordIntent(
            Unit3CIntentRecord(
                callId = "t1", campaignId = "c", family = Unit3CFamily.CONTROL, fixtureId = "f",
                trialSequence = 1, expectedAction = ExpectedAction.REPLY, modelName = "qwen2.5-coder:7b",
                modelDigest = "d", repositoryCommit = "r", promptIdentity = "control", timeoutMs = 90_000L,
                inferenceConfigIdentity = "default-ollama-request-shape",
            ),
        )
        assertTrue(ledger.hasIntent("t1"))
        assertFalse(ledger.hasIntent("t2"), "state (A), never transmitted, remains distinguishable from state (B)/(C)/(D) for a different trial ID")
    }

    @Test
    fun `intent is durable before the executor is ever invoked -- source order proves no path can transmit first`() {
        val thisFile = Path.of("tests/integration/ReasoningProtocolUnit3COrchestrationTest.kt")
        val text = Files.readString(thisFile)
        for (marker in listOf("private fun runWarmups", "private fun runArm(")) {
            val start = text.indexOf(marker)
            assertTrue(start >= 0, "$marker must exist in this file's own source")
            val end = text.indexOf("\n    private fun ", start + 1).let { if (it < 0) text.indexOf("\n}\n", start) else it }
            val body = text.substring(start, end)
            val intentIndex = body.indexOf("ledger.recordIntent(")
            val executeIndex = body.indexOf(".execute(trial)")
            assertTrue(intentIndex in 0 until executeIndex, "$marker must call ledger.recordIntent(...) before executor.execute(trial)")
        }
    }

    @Test
    fun `timeout record type has no field capable of holding a semantic action, structurally ruling out fabrication`() {
        val record = Unit3CTimeoutRecord(
            callId = "x", campaignId = "c", family = Unit3CFamily.CONTROL, fixtureId = "f", trialSequence = 1,
            timeoutMs = 90_000L, elapsedNanos = 1L, terminalClassification = Unit3CTransportClassification.MODEL_TIMEOUT,
            responseBytesReceived = false, transportDetail = "d", modelName = "qwen2.5-coder:7b", modelDigest = "d",
            inferenceConfigIdentity = "default-ollama-request-shape", continuationDecision = "ARM_CONTINUED", checkpointStatus = "NOT_CHECKPOINTED",
        )
        val fieldNames = Unit3CTimeoutRecord::class.java.declaredFields.map { it.name }
        for (forbidden in listOf("parserResult", "actualAction", "semanticCorrect", "semanticClassification")) {
            assertFalse(forbidden in fieldNames, "Unit3CTimeoutRecord must have no field capable of holding a fabricated semantic result")
        }
        assertEquals(Unit3CTransportClassification.MODEL_TIMEOUT, record.terminalClassification)
    }

    @Test
    fun `483-call schedule and arithmetic remain unaffected by the timeout and durability implementation`() {
        assertEquals(483, Unit3CCampaignDefinition.liveModelCallCount)
        assertEquals(3, Unit3CCampaignDefinition.warmupTrials.size)
        assertEquals(145, Unit3CCampaignDefinition.controlTrials.size)
        assertEquals(220, Unit3CCampaignDefinition.familyATrials.size)
        assertEquals(115, Unit3CCampaignDefinition.familyBTrials.size)
        assertEquals(29, Unit3CCampaignDefinition.familyCTrials.size)
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
