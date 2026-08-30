package parker.core.runtime

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.*

class OrdinaryRegionR69ProductionEvidenceReplayTest {
    @TempDir lateinit var temp: Path
    private val fixture = Path.of("tests/fixtures/r69-production")
    private val authorityFile = "authority-fa-9.4p-a1e-r6.8c1.region-acceptance-authority-v2"
    private val attemptFile = "parker-encoded-execution-fa-9-4p-a1e-r6-8c1--5c4ce3ccfff5eac79fb3f174e62b189f1abfd0e9854ac398967a5b78d08e87e8.fidelity-attempt-ledger"
    private val providerFile = "$R69_PROVIDER_STATE_ID.provider-state"
    private val assessmentFile = "$R69_PROVIDER_STATE_ID.assessment"

    @Test fun `exact production R6_9 records reconstruct complete typed evidence without egress or production writes`() {
        val roots = roots("exact")
        val beforeAttempt = sha(roots.attempt.resolve(attemptFile))
        val beforeProvider = sha(roots.provider.resolve(providerFile))
        val beforeAssessment = sha(roots.provider.resolve(assessmentFile))
        val live = loader(roots).load()
        assertEquals(R69_PROVIDER_RESPONSE_ID, live.providerResponseId)
        assertEquals(R69_REQUEST_DIGEST, live.requestDigest)
        assertEquals(R69_RAW_RESPONSE_DIGEST, live.rawResponseDigest)
        assertEquals(R69_STRUCTURED_STATE_DIGEST, live.structuredStateDigest)
        assertEquals(R69_PROVIDER_RECORD_DIGEST, live.providerStateRecordDigest)
        assertEquals(R69_ASSESSMENT_DIGEST, live.assessmentDigest)
        val commit = "a".repeat(40)
        val acceptance = Files.createDirectories(temp.resolve("acceptance"))
        val authorizations = Files.createDirectories(temp.resolve("authorizations"))
        val outcome = OrdinaryRegionCapabilityAcceptanceCoordinator(
            FileSystemOrdinaryRegionCapabilityAcceptanceStore(acceptance), loader(roots), { commit }, "owner",
            { Instant.parse("2026-08-30T00:00:00Z") },
        ).create(OrdinaryRegionCapabilityPromotionRequest(ORDINARY_REGION_CAPABILITY_ID, commit))
        assertIs<OrdinaryRegionCapabilityPromotionOutcome.Created>(outcome)
        assertEquals(1L, Files.list(acceptance).use { it.count() })
        assertEquals(0L, Files.list(authorizations).use { it.count() })
        assertEquals(beforeAttempt, sha(roots.attempt.resolve(attemptFile)))
        assertEquals(beforeProvider, sha(roots.provider.resolve(providerFile)))
        assertEquals(beforeAssessment, sha(roots.provider.resolve(assessmentFile)))
    }

    @Test fun `all authority attempt provider and assessment mismatches fail closed`() {
        fun fails(name: String, mutate: (Roots) -> Unit) {
            val r = roots(name); mutate(r); assertFails(name) { loader(r).load() }
        }
        fails("wrong-authority") { r ->
            Files.delete(r.authority.resolve(authorityFile))
            val original = FileSystemRegionAcceptanceAuthorityStorageV2(fixture).load(R69_AUTHORITY_ID)!!
            FileSystemRegionAcceptanceAuthorityStorageV2(r.authority).admit(original.copy(authorityId = "authority-wrong"))
        }
        fails("wrong-execution") { r -> rewriteAttempt(r.attempt.resolve(attemptFile), "execution-fa-9.4p-a1e-r6.8c2") }
        fails("wrong-request") { r -> replace(r.provider.resolve(providerFile), R69_REQUEST_DIGEST, "f".repeat(64)) }
        fails("wrong-provider-id") { r -> Files.move(r.provider.resolve(providerFile), r.provider.resolve("${"f".repeat(64)}.provider-state")) }
        fails("wrong-response-id") { r -> mutateRawResponseId(r.provider.resolve(providerFile)) }
        fails("wrong-raw-sha") { r -> replace(r.provider.resolve(providerFile), R69_RAW_RESPONSE_DIGEST, "f".repeat(64)) }
        fails("wrong-structured-sha") { r -> replace(r.provider.resolve(assessmentFile), R69_STRUCTURED_STATE_DIGEST, "f".repeat(64)) }
        fails("missing-provider") { r -> Files.delete(r.provider.resolve(providerFile)) }
        fails("corrupt-provider-checksum") { r -> replace(r.provider.resolve(providerFile), R69_PROVIDER_RECORD_DIGEST, "0".repeat(64)) }
        fails("other-attempt-provider") { r -> replace(r.provider.resolve(providerFile), R69_REQUEST_DIGEST, "e".repeat(64)) }
        fails("missing-assessment") { r -> Files.delete(r.provider.resolve(assessmentFile)) }
        fails("wrong-assessment-identity") { r -> replace(r.provider.resolve(assessmentFile), R69_ASSESSMENT_DIGEST, "f".repeat(64)) }

        val live = loader(roots("governance-negatives")).load()
        val governed = RegionTranscriptionCapabilityAcceptanceEvidenceV1.governed(live)
        assertFails { governed.copy(pointAnchorSemantics = governed.pointAnchorSemantics.copy(reportDigest = "f".repeat(64))) }
        assertFails { governed.copy(pointAnchorSemantics = governed.pointAnchorSemantics.copy(wireVersion = 4)) }
        assertFails { governed.copy(fidelityReview = governed.fidelityReview.copy(classification = OrdinaryRegionFidelityClassification.FAIL_FIDELITY)) }
        assertFails { governed.copy(fidelityReview = governed.fidelityReview.copy(reviewedRegions = 0, exactRegions = 0)) }
    }

    private data class Roots(val authority: Path, val attempt: Path, val provider: Path)
    private fun roots(name: String): Roots {
        val base = Files.createDirectories(temp.resolve(name))
        fun dir(n: String) = Files.createDirectories(base.resolve(n))
        val r = Roots(dir("authority"), dir("attempt"), dir("provider"))
        Files.copy(fixture.resolve(authorityFile), r.authority.resolve(authorityFile))
        Files.copy(fixture.resolve(attemptFile), r.attempt.resolve(attemptFile))
        Files.copy(fixture.resolve(providerFile), r.provider.resolve(providerFile))
        Files.copy(fixture.resolve(assessmentFile), r.provider.resolve(assessmentFile))
        return r
    }
    private fun loader(r: Roots) = DurableOrdinaryRegionR69EvidenceLoader(
        FileSystemRegionProviderStateStore(r.provider), FileSystemRegionAcceptanceAuthorityStorageV2(r.authority),
        FileSystemFidelityFirstAttemptLedger(r.attempt),
    )
    private fun replace(path: Path, old: String, new: String) {
        val text = Files.readString(path); require(old in text); Files.writeString(path, text.replace(old, new))
    }
    private fun rewriteAttempt(path: Path, execution: String) {
        val lines = Files.readAllLines(path).toMutableList(); val parts = lines.first().split('\t').dropLast(1).toMutableList()
        val index = parts.indexOfFirst { it.startsWith("executionId=") }; require(index >= 0); parts[index] = "executionId=$execution"
        val payload = parts.joinToString("\t"); lines[0] = "$payload\tchecksum=${sha(payload.toByteArray())}"
        Files.writeString(path, lines.joinToString("\n", postfix = "\n"))
    }
    @Suppress("UNCHECKED_CAST")
    private fun mutateRawResponseId(path: Path) {
        val top = (RegionJson.parse(Files.readString(path)) as Map<String, Any?>).toMutableMap()
        val record = (top.getValue("record") as Map<String, Any?>).toMutableMap()
        val raw = String(Base64.getDecoder().decode(record.getValue("raw_base64") as String))
            .replace(R69_PROVIDER_RESPONSE_ID, "resp_${"f".repeat(R69_PROVIDER_RESPONSE_ID.length - 5)}")
            .toByteArray()
        record["raw_base64"] = Base64.getEncoder().encodeToString(raw); record["raw_length"] = raw.size
        record["raw_sha256"] = sha(raw); top["record"] = record; top["record_sha256"] = sha(RegionJson.encode(record).toByteArray())
        Files.writeString(path, RegionJson.encode(top))
    }
    private fun sha(path: Path) = sha(Files.readAllBytes(path))
    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
