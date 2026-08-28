package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class UnitOAcceptanceAttemptLedgerTest {
    @TempDir lateinit var root: Path

    @Test fun `normal fake success is durable monotonic and restart recoverable`() {
        val tracker = tracker()
        tracker.authorized(); tracker.preflightPassed(); tracker.sourceRetrieved(); tracker.requestPrepared()
        tracker.providerAttemptStarting(); tracker.providerResponseReceived(); tracker.generationAdmitted(); tracker.terminalSuccess()
        val recovered = ledger().open(identity())
        assertEquals(listOf(
            AcceptanceAttemptStage.AUTHORIZED, AcceptanceAttemptStage.PREFLIGHT_PASSED,
            AcceptanceAttemptStage.SOURCE_RETRIEVED, AcceptanceAttemptStage.REQUEST_PREPARED,
            AcceptanceAttemptStage.PROVIDER_ATTEMPT_STARTED, AcceptanceAttemptStage.PROVIDER_RESPONSE_RECEIVED,
            AcceptanceAttemptStage.GENERATION_ADMITTED, AcceptanceAttemptStage.TERMINAL_SUCCESS,
        ), recovered.entries.map { it.stage })
        assertTrue(recovered.providerAttemptStarted); assertTrue(recovered.providerResponseReceived)
        assertFails { ledger().requireExecutionAvailable(identity()) }
    }

    @Test fun `preflight failure records bounded terminal failure without attempt`() {
        val tracker = tracker(); tracker.authorized()
        tracker.terminalFailure(IllegalArgumentException("SECRET_SENTINEL source transcript credential"), "PREFLIGHT_FAILURE")
        val recovered = tracker.snapshot()
        assertEquals(listOf(AcceptanceAttemptStage.AUTHORIZED, AcceptanceAttemptStage.TERMINAL_FAILURE), recovered.entries.map { it.stage })
        assertFalse(recovered.providerAttemptStarted)
        val raw = Files.readString(record())
        assertFalse(raw.contains("SECRET_SENTINEL")); assertFalse(raw.contains("source transcript credential"))
        assertTrue(raw.contains("exceptionClass=IllegalArgumentException"))
    }

    @Test fun `marker durability failure prevents fake send`() {
        var forces = 0
        val failing = object : AcceptanceLedgerDurability {
            override fun forceFile(channel: java.nio.channels.FileChannel) {
                forces++
                if (forces == 5) throw IllegalStateException("SECRET_FSYNC_SENTINEL")
                channel.force(true)
            }
        }
        val tracker = UnitOAcceptanceAttemptTracker(
            FileSystemAcceptanceAttemptLedger(
                root,
                durability = failing,
                directoryDurability = DirectoryDurabilityRequirement.NOT_APPLICABLE_ON_THIS_FILESYSTEM,
            ),
            identity(),
        )
        tracker.authorized(); tracker.preflightPassed(); tracker.sourceRetrieved(); tracker.requestPrepared()
        var sends = 0
        assertFails { tracker.providerAttemptStarting(); sends++ }
        assertEquals(0, sends)
        assertFalse(ledger().open(identity()).providerAttemptStarted)
        assertFalse(Files.readString(record()).contains("SECRET_FSYNC_SENTINEL"))
    }

    @Test fun `crash after attempt marker is conservatively consumed and blocks restart`() {
        val tracker = tracker(); tracker.authorized(); tracker.preflightPassed(); tracker.sourceRetrieved(); tracker.requestPrepared()
        tracker.providerAttemptStarting()
        assertTrue(ledger().open(identity()).providerAttemptStarted)
        assertFails { ledger().requireExecutionAvailable(identity()) }
    }

    @Test fun `send and post-response failures preserve truthful latest stages without retry`() {
        val sendTracker = tracker("send-failure")
        sendTracker.authorized(); sendTracker.preflightPassed(); sendTracker.sourceRetrieved(); sendTracker.requestPrepared(); sendTracker.providerAttemptStarting()
        sendTracker.terminalFailure(IllegalArgumentException("SECRET_SEND"), "FAKE_SEND_FAILURE")
        assertEquals(AcceptanceAttemptStage.TERMINAL_FAILURE, sendTracker.snapshot().latest)
        assertFalse(sendTracker.snapshot().providerResponseReceived)

        val parseTracker = tracker("parse-failure")
        parseTracker.authorized(); parseTracker.preflightPassed(); parseTracker.sourceRetrieved(); parseTracker.requestPrepared()
        parseTracker.providerAttemptStarting(); parseTracker.providerResponseReceived()
        parseTracker.terminalFailure(IllegalArgumentException("SECRET_BODY"), "RESPONSE_PARSE_FAILURE")
        assertTrue(parseTracker.snapshot().providerResponseReceived)
        assertFalse(Files.readString(record("parse-failure")).contains("SECRET_BODY"))
    }

    @Test fun `admission and terminal result failures retain exact durable stage`() {
        val admission = tracker("admission-failure")
        admission.authorized(); admission.preflightPassed(); admission.sourceRetrieved(); admission.requestPrepared()
        admission.providerAttemptStarting(); admission.providerResponseReceived()
        admission.terminalFailure(IllegalStateException("SECRET"), "ADMISSION_FAILURE")
        assertEquals(AcceptanceAttemptStage.PROVIDER_RESPONSE_RECEIVED, admission.snapshot().entries.dropLast(1).last().stage)

        val result = tracker("result-failure")
        result.authorized(); result.preflightPassed(); result.sourceRetrieved(); result.requestPrepared()
        result.providerAttemptStarting(); result.providerResponseReceived(); result.generationAdmitted()
        result.terminalFailure(IllegalStateException("SECRET"), "TERMINAL_RESULT_FAILURE")
        assertEquals(AcceptanceAttemptStage.GENERATION_ADMITTED, result.snapshot().entries.dropLast(1).last().stage)
    }

    @Test fun `injected unexpected failures retain the exact latest durable stage`() {
        data class Scenario(val name: String, val durableStages: List<AcceptanceAttemptStage>, val expected: AcceptanceAttemptStage)
        val scenarios = listOf(
            Scenario("authorization", emptyList(), AcceptanceAttemptStage.AUTHORIZED),
            Scenario("source-retrieval", listOf(AcceptanceAttemptStage.PREFLIGHT_PASSED), AcceptanceAttemptStage.PREFLIGHT_PASSED),
            Scenario("representation", listOf(AcceptanceAttemptStage.PREFLIGHT_PASSED, AcceptanceAttemptStage.SOURCE_RETRIEVED), AcceptanceAttemptStage.SOURCE_RETRIEVED),
            Scenario("request-preparation", listOf(AcceptanceAttemptStage.PREFLIGHT_PASSED, AcceptanceAttemptStage.SOURCE_RETRIEVED), AcceptanceAttemptStage.SOURCE_RETRIEVED),
            Scenario("before-marker", listOf(AcceptanceAttemptStage.PREFLIGHT_PASSED, AcceptanceAttemptStage.SOURCE_RETRIEVED, AcceptanceAttemptStage.REQUEST_PREPARED), AcceptanceAttemptStage.REQUEST_PREPARED),
            Scenario("after-marker", listOf(AcceptanceAttemptStage.PREFLIGHT_PASSED, AcceptanceAttemptStage.SOURCE_RETRIEVED, AcceptanceAttemptStage.REQUEST_PREPARED, AcceptanceAttemptStage.PROVIDER_ATTEMPT_STARTED), AcceptanceAttemptStage.PROVIDER_ATTEMPT_STARTED),
            Scenario("response-receipt", listOf(AcceptanceAttemptStage.PREFLIGHT_PASSED, AcceptanceAttemptStage.SOURCE_RETRIEVED, AcceptanceAttemptStage.REQUEST_PREPARED, AcceptanceAttemptStage.PROVIDER_ATTEMPT_STARTED), AcceptanceAttemptStage.PROVIDER_ATTEMPT_STARTED),
            Scenario("validation", listOf(AcceptanceAttemptStage.PREFLIGHT_PASSED, AcceptanceAttemptStage.SOURCE_RETRIEVED, AcceptanceAttemptStage.REQUEST_PREPARED, AcceptanceAttemptStage.PROVIDER_ATTEMPT_STARTED, AcceptanceAttemptStage.PROVIDER_RESPONSE_RECEIVED), AcceptanceAttemptStage.PROVIDER_RESPONSE_RECEIVED),
        )
        scenarios.forEachIndexed { index, scenario ->
            val tracker = tracker("injected-$index")
            tracker.authorized()
            scenario.durableStages.forEach { stage -> ledger().transition(tracker.identity, stage) }
            tracker.terminalFailure(IllegalArgumentException("SECRET_${scenario.name}"), "INJECTED_FAILURE")
            val snapshot = tracker.snapshot()
            assertEquals(scenario.expected, snapshot.entries.dropLast(1).last().stage, scenario.name)
            assertEquals(AcceptanceAttemptStage.TERMINAL_FAILURE, snapshot.latest)
            assertFalse(Files.readString(record("injected-$index")).contains("SECRET_"))
        }
    }

    @Test fun `corrupt truncated skipped duplicate and conflicting identity ledgers fail closed`() {
        val tracker = tracker(); tracker.authorized()
        val valid = Files.readString(record())
        Files.writeString(record(), valid.dropLast(1))
        assertFails { ledger().open(identity()) }

        Files.delete(record()); tracker().authorized()
        Files.writeString(record(), Files.readString(record()) + "kind=STAGE\tstage=BAD\tchecksum=bad\n")
        assertFails { ledger().open(identity()) }

        Files.delete(record()); val duplicate = tracker(); duplicate.authorized(); duplicate.preflightPassed()
        val lines = Files.readAllLines(record()); Files.write(record(), lines + lines.last())
        assertFails { ledger().open(identity()) }

        Files.delete(record()); val backward = tracker(); backward.authorized(); backward.preflightPassed()
        val backwardLines = Files.readAllLines(record()); Files.write(record(), backwardLines + backwardLines[1])
        assertFails { ledger().open(identity()) }

        Files.delete(record()); tracker().authorized()
        assertFails { ledger().transition(identity(), AcceptanceAttemptStage.SOURCE_RETRIEVED) }
        assertFails { ledger().open(identity().copy(allocation = "wrong-allocation")) }
        assertFails { ledger().open(identity().copy(evidenceArtifactId = "evidence-wrong")) }
        assertFails { ledger().open(identity().copy(repositoryCommit = "b".repeat(40))) }
        assertFails { ledger().open(identity().copy(profileId = "wrong-profile")) }
        assertFails { AcceptanceExecutionId("../corrupt") }
    }

    private fun tracker(id: String = "unit-o4r-test") = UnitOAcceptanceAttemptTracker(ledger(), identity(id))
    private fun ledger() = FileSystemAcceptanceAttemptLedger(
        root,
        now = { Instant.parse("2026-08-27T00:00:00Z") },
        directoryDurability = DirectoryDurabilityRequirement.NOT_APPLICABLE_ON_THIS_FILESYSTEM,
    )
    private fun record(id: String = "unit-o4r-test") = root.resolve("$id.attempt-ledger")
    private fun identity(id: String = "unit-o4r-test") = AcceptanceExecutionIdentity(
        AcceptanceExecutionId(id), "UNIT_O4R", "CLEAN_LITERAL_V2", 2, "evidence-test",
        "a".repeat(64), 123, "application/pdf", "a".repeat(40), "OpenAI", "gpt-4.1-mini",
        "literal-v2", "b".repeat(64), "c".repeat(64), "byte-exact-v1", "1.1.0",
    )
}

class UnitOAcceptanceAttemptLedgerLinuxDurabilityTest {
    @TempDir lateinit var root: Path

    @Test fun `system filesystem provides required containing-directory durability`() {
        val capabilityFailure = runCatching {
            AcceptanceLedgerDurability.SYSTEM.forceDirectory(root)
        }.exceptionOrNull()
        org.junit.jupiter.api.Assumptions.assumeTrue(
            capabilityFailure == null,
            "NOT APPLICABLE ON THIS FILESYSTEM: containing-directory fsync is unavailable (${capabilityFailure?.javaClass?.simpleName})",
        )
        val identity = AcceptanceExecutionIdentity(
            AcceptanceExecutionId("linux-directory-durability"), "UNIT_O4R", "FAKE", 1, "evidence-fake",
            "a".repeat(64), 1, "application/pdf", "a".repeat(40), "Fake", "fixed", "fake",
            "b".repeat(64), "c".repeat(64), "byte-exact", "1",
        )
        val tracker = UnitOAcceptanceAttemptTracker(FileSystemAcceptanceAttemptLedger(root), identity)
        tracker.authorized(); tracker.preflightPassed(); tracker.sourceRetrieved(); tracker.requestPrepared()
        tracker.providerAttemptStarting(); tracker.providerResponseReceived(); tracker.generationAdmitted(); tracker.terminalSuccess()
        assertEquals(AcceptanceAttemptStage.TERMINAL_SUCCESS, FileSystemAcceptanceAttemptLedger(root).open(identity).latest)
    }
}

class UnitOAcceptanceAttemptLedgerMountTest {
    @Test fun `uid 999 writes and recovers durable ledger on mounted root`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("parker.unitO.ledger.mount.enabled") == "true")
        assertEquals("999", System.getProperty("user.name"))
        val root = Path.of(requireNotNull(System.getenv("PARKER_UNIT_O_LEDGER_MOUNT_ROOT")))
        val identity = AcceptanceExecutionIdentity(AcceptanceExecutionId("uid-999-mount-test"), "UNIT_O4R", "FAKE", 1,
            "evidence-fake", "a".repeat(64), 1, "application/pdf", "a".repeat(40), "Fake", "fixed",
            "fake", "b".repeat(64), "c".repeat(64), "byte-exact", "1")
        val tracker = UnitOAcceptanceAttemptTracker(FileSystemAcceptanceAttemptLedger(root), identity)
        tracker.authorized(); tracker.preflightPassed(); tracker.sourceRetrieved(); tracker.requestPrepared()
        tracker.providerAttemptStarting(); tracker.providerResponseReceived(); tracker.generationAdmitted(); tracker.terminalSuccess()
        assertEquals(AcceptanceAttemptStage.TERMINAL_SUCCESS, FileSystemAcceptanceAttemptLedger(root).open(identity).latest)
    }
}

object UnitOAcceptanceAttemptLedgerMountProbe {
    @JvmStatic fun main(args: Array<String>) {
        val processStatus = Files.readString(Path.of("/proc/self/status"))
        check(processStatus.lineSequence().any { it.matches(Regex("Uid:\\s+999\\s+999\\s+999\\s+999")) })
        check(processStatus.lineSequence().any { it.matches(Regex("Gid:\\s+999\\s+999\\s+999\\s+999")) })
        val readOnlyManifest = Path.of(requireNotNull(System.getenv("PARKER_UNIT_O_FAKE_MANIFEST_ROOT")))
        val readOnlyEvidence = Path.of(requireNotNull(System.getenv("PARKER_UNIT_O_FAKE_EVIDENCE_ROOT")))
        check(Files.isReadable(readOnlyManifest) && !Files.isWritable(readOnlyManifest))
        check(Files.isReadable(readOnlyEvidence) && !Files.isWritable(readOnlyEvidence))
        listOf("PARKER_UNIT_O_FAKE_GENERATION_ROOT", "PARKER_UNIT_O_FAKE_CONTENT_ROOT", "PARKER_UNIT_O_RESULT_ROOT")
            .forEach { check(Files.isWritable(Path.of(requireNotNull(System.getenv(it))))) }
        val root = Path.of(requireNotNull(System.getenv("PARKER_UNIT_O_LEDGER_MOUNT_ROOT")))
        val identity = AcceptanceExecutionIdentity(AcceptanceExecutionId("uid-999-mount-probe"), "UNIT_O4R", "FAKE", 1,
            "evidence-fake", "a".repeat(64), 1, "application/pdf", "a".repeat(40), "Fake", "fixed",
            "fake", "b".repeat(64), "c".repeat(64), "byte-exact", "1")
        val tracker = UnitOAcceptanceAttemptTracker(FileSystemAcceptanceAttemptLedger(root), identity)
        tracker.authorized(); tracker.preflightPassed(); tracker.sourceRetrieved(); tracker.requestPrepared()
        tracker.providerAttemptStarting(); tracker.providerResponseReceived(); tracker.generationAdmitted(); tracker.terminalSuccess()
        check(FileSystemAcceptanceAttemptLedger(root).open(identity).latest == AcceptanceAttemptStage.TERMINAL_SUCCESS)
        println("UNIT_O_ATTEMPT_LEDGER_UID_999_MOUNT_OK")
    }
}
