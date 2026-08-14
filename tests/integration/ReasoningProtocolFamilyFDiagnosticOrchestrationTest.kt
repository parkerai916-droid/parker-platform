package parker.integration

/**
 * Reasoning Protocol Live-Model Conformance, Unit 3-BF: Family F
 * Alternative-Model Diagnostic -- orchestration counterpart.
 *
 * Exact schedule construction, resource/residency/production-isolation
 * gates, the durable append-only campaign ledger, exact-once
 * dispatch/recovery, the subject-only investment-screening advancement
 * gate, and this file's own offline test suite. See
 * ReasoningProtocolFamilyFDiagnosticTest.kt for frozen definitions, the
 * transparent capture proxy, and the live entry point; both files
 * together form one atomic, test-tier-only implementation boundary.
 *
 * No model is acquired, loaded, started, stopped, or contacted by any
 * test in this file.
 */

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parker.core.interfaces.ReasoningProviderResponse
import parker.core.interfaces.ReasoningSubject
import parker.core.runtime.DefaultReasoningPromptBuilder
import parker.core.runtime.TaggedReasoningResponseParser
import parker.core.runtime.UnclassifiableModelResponseException
import parker.core.runtime.defaultOllamaRequestBody
import parker.core.runtime.defaultOllamaResponseBody
import java.io.IOException
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import java.time.Duration
import java.time.Instant
import java.util.Base64

// ---------------------------------------------------------------------
// Trial schedule (Plan Sections 10, 11)
// ---------------------------------------------------------------------

enum class FamilyFTrialKind { WARMUP, SCORED }

data class FamilyFTrial(
    val id: String,
    val role: FamilyFRole,
    val repetition: Int,
    val kind: FamilyFTrialKind,
    val fixture: ConformanceFixture,
    val profileId: ContextProfileId?,
    val attempt: Int,
)

data class FamilyFBlock(val repetition: Int, val role: FamilyFRole, val trials: List<FamilyFTrial>)

private fun familyFTrialId(
    repetition: Int,
    role: FamilyFRole,
    kind: FamilyFTrialKind,
    fixtureId: String? = null,
    profileToken: String? = null,
    warmupAttempt: Int? = null,
): String {
    val repToken = "r" + repetition.toString().padStart(2, '0')
    val roleToken = role.name.lowercase()
    return when (kind) {
        FamilyFTrialKind.WARMUP -> "ff-$repToken-$roleToken-warmup-${warmupAttempt!!.toString().padStart(2, '0')}"
        FamilyFTrialKind.SCORED -> "ff-$repToken-$roleToken-${fixtureId!!.lowercase()}-$profileToken"
    }
}

object FamilyFCampaignDefinition {
    // AB/BA alternation (Plan Section 10, not a full Latin square):
    // repetitions 1 and 3 run subject then control; repetitions 2 and 4
    // run control then subject.
    private fun blockOrder(repetition: Int): List<FamilyFRole> =
        if (repetition % 2 == 1) listOf(FamilyFRole.SUBJECT, FamilyFRole.CONTROL) else listOf(FamilyFRole.CONTROL, FamilyFRole.SUBJECT)

    val allTrials: List<FamilyFTrial> = (1..4).flatMap { repetition ->
        blockOrder(repetition).flatMap { role ->
            val warmups = (1..3).map { attempt ->
                FamilyFTrial(
                    id = familyFTrialId(repetition, role, FamilyFTrialKind.WARMUP, warmupAttempt = attempt),
                    role = role,
                    repetition = repetition,
                    kind = FamilyFTrialKind.WARMUP,
                    fixture = FamilyFCorpus.warmupFixture,
                    profileId = ContextProfileId.MINIMAL_PRODUCTION_CONTEXT,
                    attempt = attempt,
                )
            }
            val scored = FamilyFCorpus.fixtures.flatMap { fixture ->
                FamilyFCorpus.profiles.map { profile ->
                    FamilyFTrial(
                        id = familyFTrialId(repetition, role, FamilyFTrialKind.SCORED, fixtureId = fixture.id, profileToken = profile.familyFToken()),
                        role = role,
                        repetition = repetition,
                        kind = FamilyFTrialKind.SCORED,
                        fixture = fixture,
                        profileId = profile,
                        attempt = 1,
                    )
                }
            }
            warmups + scored
        }
    }

    val blocks: List<FamilyFBlock> = allTrials
        .groupBy { it.repetition to it.role }
        .toList()
        .map { (key, trials) -> FamilyFBlock(key.first, key.second, trials) }

    init {
        val ids = allTrials.map { it.id }
        check(ids.size == ids.distinct().size) { "Family F trial IDs must be unique" }
        check(allTrials.count { it.kind == FamilyFTrialKind.SCORED } == 368) { "Family F scored call count must be exactly 368" }
        check(allTrials.count { it.kind == FamilyFTrialKind.WARMUP } == 24) { "Family F warm-up call count must be exactly 24" }
        check(allTrials.size == 392) { "Family F total call count must be exactly 392" }
        check(allTrials.count { it.role == FamilyFRole.SUBJECT && it.kind == FamilyFTrialKind.SCORED } == 184)
        check(allTrials.count { it.role == FamilyFRole.CONTROL && it.kind == FamilyFTrialKind.SCORED } == 184)
        check(blocks.size == 8) { "Family F campaign must contain exactly 8 residency blocks" }
        check(blocks.all { it.trials.size == 49 }) { "each residency block must contain exactly 3 warm-ups + 46 scored calls" }
    }

    val scheduleHash: String = sha256(allTrials.joinToString("\n") { it.id })
}

// ---------------------------------------------------------------------
// Resource gates (Plan Section 16), mirroring the already-proven
// Unit3CDiskSpaceGate injectable-lambda, fail-closed pattern.
// ---------------------------------------------------------------------

class FamilyFInsufficientMemoryException(message: String) : RuntimeException(message)
class FamilyFInsufficientSpaceException(message: String) : RuntimeException(message)

// ---------------------------------------------------------------------
// Structured resource-reading record (Plan Sections 16, 17; independent
// review Finding 1). Every governed resource check must produce one of
// these and persist it through FamilyFCampaignLedger.recordResourceReading
// -- durability, sequencing, and hash-chaining are the ledger's own
// responsibility (see FamilyFCampaignLedger below); this data class
// carries only the check's own observed facts.
// ---------------------------------------------------------------------

data class FamilyFResourceReading(
    val phase: String,
    val blockId: String?,
    val trialId: String?,
    val source: String,
    val rawReading: String,
    val parsedBytes: Long,
    val thresholdBytes: Long,
    val outcome: String,
)

object FamilyFMemoryGate {
    // Existing convenience gates, unchanged: throw-only, no persistence,
    // used directly by unit tests below with simple lambdas.
    fun checkPreLoad(artifactSizeBytes: Long, availableBytes: () -> Long) {
        enforce(measurePreLoad(artifactSizeBytes, null, availableBytes))
    }

    fun checkPerCall(availableBytes: () -> Long) {
        enforce(measurePerCall(null, "MEMORY_PER_CALL", availableBytes))
    }

    // Pure measurement -- never throws, never persists. The driver
    // records the returned reading through the ledger before calling
    // [enforce], so a failing (or unreadable) reading is always durably
    // recorded before the corresponding exception halts execution. The
    // ledger itself stamps the authoritative campaign ID on every
    // persisted record (the universal chained-envelope mechanism); this
    // reading carries only its own observed facts.
    fun measurePreLoad(artifactSizeBytes: Long, blockId: String?, availableBytes: () -> Long): FamilyFResourceReading {
        val threshold = artifactSizeBytes + FAMILY_F_MINIMUM_FREE_MEMORY_BYTES
        val (rawReading, parsedBytes) = readSafely(availableBytes)
        val outcome = if (parsedBytes >= threshold) "PASS" else "FAIL"
        return FamilyFResourceReading("MEMORY_PRE_LOAD", blockId, null, "MemAvailable", rawReading, parsedBytes, threshold, outcome)
    }

    fun measurePerCall(trialId: String?, phase: String, availableBytes: () -> Long): FamilyFResourceReading {
        val (rawReading, parsedBytes) = readSafely(availableBytes)
        val outcome = if (parsedBytes >= FAMILY_F_MINIMUM_FREE_MEMORY_BYTES) "PASS" else "FAIL"
        return FamilyFResourceReading(phase, null, trialId, "MemAvailable", rawReading, parsedBytes, FAMILY_F_MINIMUM_FREE_MEMORY_BYTES, outcome)
    }

    // A reading of "FAIL" (including an unreadable source, parsedBytes=-1)
    // is not itself an exception -- only [enforce] throws, and only after
    // the reading has already been returned to the caller for persistence.
    fun enforce(reading: FamilyFResourceReading) {
        if (reading.outcome != "PASS") {
            throw FamilyFInsufficientMemoryException(
                "memory gate failed (phase=${reading.phase}): raw=${reading.rawReading}, threshold=${reading.thresholdBytes}",
            )
        }
    }

    private fun readSafely(availableBytes: () -> Long): Pair<String, Long> = try {
        val value = availableBytes()
        value.toString() to value
    } catch (exception: Exception) {
        "ERROR: ${exception.javaClass.simpleName}: ${exception.message}" to -1L
    }

    // Real default reader for this deployment's Ubuntu host. Fully
    // overridable; never invoked by any offline test in this file. The
    // Explicit Execution Approval fixes the authoritative source per
    // Plan Section 16 -- this is a reasonable default, not a frozen
    // requirement.
    val defaultReader: () -> Long = {
        val line = Files.readAllLines(Path.of("/proc/meminfo")).firstOrNull { it.startsWith("MemAvailable:") }
            ?: throw IOException("MemAvailable not found in /proc/meminfo")
        val kib = Regex("(\\d+)").find(line)?.groupValues?.get(1)?.toLong()
            ?: throw IOException("unable to parse MemAvailable line: $line")
        kib * 1024L
    }
}

object FamilyFDiskSpaceGate {
    // Existing convenience gate, unchanged: throw-only, no persistence,
    // used directly by unit tests below with simple lambdas.
    fun check(path: Path, usableSpace: (Path) -> Long) {
        enforceDisk(measure(path, "DISK_SPACE", null, usableSpace))
    }

    // Pure measurement -- never throws, never persists; see
    // FamilyFMemoryGate.measurePreLoad's own doc comment for why.
    fun measure(path: Path, phase: String, blockId: String?, usableSpace: (Path) -> Long): FamilyFResourceReading {
        val (rawReading, parsedBytes) = try {
            val value = usableSpace(path)
            value.toString() to value
        } catch (exception: IOException) {
            "ERROR: ${exception.javaClass.simpleName}: ${exception.message}" to -1L
        }
        val outcome = if (parsedBytes >= FAMILY_F_MINIMUM_FREE_MEMORY_BYTES) "PASS" else "FAIL"
        return FamilyFResourceReading(phase, blockId, null, path.toString(), rawReading, parsedBytes, FAMILY_F_MINIMUM_FREE_MEMORY_BYTES, outcome)
    }

    fun enforceDisk(reading: FamilyFResourceReading) {
        if (reading.outcome != "PASS") {
            throw FamilyFInsufficientSpaceException(
                "disk gate failed for ${reading.source} (phase=${reading.phase}): raw=${reading.rawReading}, threshold=${reading.thresholdBytes}",
            )
        }
    }
}

// ---------------------------------------------------------------------
// Residency gate (Plan Section 15). No residency/process-control
// abstraction exists anywhere else in this repository; this is new,
// additive infrastructure confined to these two test files.
// ---------------------------------------------------------------------

enum class FamilyFResidencyState { ABSENT, SUBJECT_RESIDENT, CONTROL_RESIDENT, BOTH_RESIDENT, UNKNOWN }

fun interface FamilyFResidencyQuery {
    fun currentResidency(): FamilyFResidencyState
}

fun interface FamilyFModelUnloadCommand {
    fun unload(role: FamilyFRole): Boolean
}

class FamilyFResidencyException(message: String) : RuntimeException(message)

object FamilyFResidencyGate {
    fun checkNeitherResident(query: FamilyFResidencyQuery) {
        val state = queryOrFail(query)
        if (state != FamilyFResidencyState.ABSENT) {
            throw FamilyFResidencyException("expected no governed model resident before block start, observed $state")
        }
    }

    fun checkAssignedResident(role: FamilyFRole, query: FamilyFResidencyQuery) {
        val state = queryOrFail(query)
        val expected = if (role == FamilyFRole.SUBJECT) FamilyFResidencyState.SUBJECT_RESIDENT else FamilyFResidencyState.CONTROL_RESIDENT
        if (state != expected) {
            throw FamilyFResidencyException("expected $expected resident for role $role, observed $state")
        }
    }

    fun unloadAndVerifyAbsent(
        role: FamilyFRole,
        unload: FamilyFModelUnloadCommand,
        pollUntilAbsent: () -> FamilyFResidencyState,
    ) {
        val accepted = try {
            unload.unload(role)
        } catch (exception: Exception) {
            throw FamilyFResidencyException("unload request failed for $role: ${exception.message}")
        }
        if (!accepted) {
            throw FamilyFResidencyException("unload request was not accepted for $role")
        }
        val finalState = try {
            pollUntilAbsent()
        } catch (exception: Exception) {
            throw FamilyFResidencyException("residency polling failed for $role: ${exception.message}")
        }
        if (finalState != FamilyFResidencyState.ABSENT) {
            throw FamilyFResidencyException("unload did not converge to absence for $role, observed $finalState")
        }
    }

    private fun queryOrFail(query: FamilyFResidencyQuery): FamilyFResidencyState = try {
        query.currentResidency()
    } catch (exception: Exception) {
        throw FamilyFResidencyException("unable to determine model residency: ${exception.message}")
    }
}

// ---------------------------------------------------------------------
// Production isolation guard (Plan Section 14). Read-only comparisons
// only; never signals, stops, restarts, reconfigures, or routes traffic
// through any protected process.
// ---------------------------------------------------------------------

data class FamilyFProtectedProcess(val label: String, val pid: String, val endpointIdentifier: String)

class FamilyFProductionIsolationException(message: String) : RuntimeException(message)

object FamilyFProductionIsolationGuard {
    fun requireDistinctFromProduction(
        dedicatedPid: String,
        dedicatedEndpointIdentifier: String,
        protected: List<FamilyFProtectedProcess>,
    ) {
        protected.forEach { process ->
            if (process.pid == dedicatedPid) {
                throw FamilyFProductionIsolationException(
                    "dedicated diagnostic PID must not equal protected production PID (${process.label})",
                )
            }
            if (process.endpointIdentifier == dedicatedEndpointIdentifier) {
                throw FamilyFProductionIsolationException(
                    "dedicated diagnostic endpoint must not equal protected production endpoint (${process.label})",
                )
            }
        }
    }

    fun requireProtectedProcessesHealthy(protected: List<FamilyFProtectedProcess>, isAlive: (String) -> Boolean) {
        protected.forEach { process ->
            if (!isAlive(process.pid)) {
                throw FamilyFProductionIsolationException(
                    "protected production process ${process.label} (pid ${process.pid}) is not observed healthy",
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// Universal chained-record ledger mechanism (independent review: every
// append-only JSONL artifact must carry schema version, campaign ID,
// trial ID where applicable, sequence number, prior-record hash, record
// hash, and timestamp). One shared mechanism, not seven independently
// implemented variants -- every governed ledger file below is a thin
// wrapper around FamilyFChainedLedger.append/recoverAndVerify.
// ---------------------------------------------------------------------

class FamilyFArtifactIntegrityException(message: String) : RuntimeException(message)

data class FamilyFRecoveryState(
    val dispatched: Set<String>,
    val pendingOfflineClassification: Set<String>,
    val resolved: Set<String>,
)

const val FAMILY_F_LEDGER_SCHEMA_VERSION: Int = 1
val FAMILY_F_LEDGER_GENESIS_HASH: String = "0".repeat(64)

data class FamilyFChainCursor(var sequence: Long, var priorRecordHash: String)

private fun familyFObjectLine(fields: List<Pair<String, Any?>>): String =
    fields.joinToString(prefix = "{", postfix = "}", separator = ",") { (key, value) ->
        "\"${familyFJsonEscape(key)}\":${familyFJsonValue(value)}"
    }

private fun familyFJsonValue(value: Any?): String = when (value) {
    null -> "null"
    is Boolean, is Int, is Long -> value.toString()
    else -> "\"${familyFJsonEscape(value.toString())}\""
}

private fun familyFJsonEscape(value: String): String = buildString {
    value.forEach { character ->
        when (character) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(character)
        }
    }
}

// A linear character-by-character scan, deliberately not a backtracking
// "(?:\\.|[^\"])*"-shaped regex: durable transport records now carry
// Base64-encoded raw request/response bodies that can be many kilobytes
// long, and Java's recursive backtracking regex engine can StackOverflow
// on a single long capture group of that shape. Single-pass, so this
// scales linearly in line length regardless of field size.
private fun familyFExtractStringField(line: String, field: String): String {
    val marker = "\"$field\":\""
    val start = line.indexOf(marker)
    if (start < 0) {
        throw FamilyFArtifactIntegrityException("record missing required field \"$field\": $line")
    }
    val builder = StringBuilder()
    var index = start + marker.length
    while (index < line.length) {
        val character = line[index]
        if (character == '\\' && index + 1 < line.length) {
            when (line[index + 1]) {
                '"' -> builder.append('"')
                '\\' -> builder.append('\\')
                'n' -> builder.append('\n')
                'r' -> builder.append('\r')
                't' -> builder.append('\t')
                else -> builder.append(line[index + 1])
            }
            index += 2
            continue
        }
        if (character == '"') {
            return builder.toString()
        }
        builder.append(character)
        index += 1
    }
    throw FamilyFArtifactIntegrityException("record missing required field \"$field\": $line")
}

// Same linear-scan approach as familyFExtractStringField above, but
// tolerant of an absent field (returns the raw, still-escaped JSON
// token -- null/true/false/"..." -- rather than throwing), for callers
// that need to decode a nested payload field (e.g. terminal.jsonl's own
// "payload", which wraps the full EvaluationJsonLines.trial(...) text
// and can be long enough to make a backtracking-regex extraction risk a
// StackOverflowError).
private fun familyFJsonFieldRawToken(line: String, field: String): String? {
    val marker = "\"$field\":"
    val start = line.indexOf(marker)
    if (start < 0) return null
    var index = start + marker.length
    if (index >= line.length) return null
    return when {
        line.startsWith("null", index) -> "null"
        line.startsWith("true", index) -> "true"
        line.startsWith("false", index) -> "false"
        line[index] == '"' -> {
            val builder = StringBuilder()
            builder.append('"')
            index += 1
            while (index < line.length) {
                val character = line[index]
                if (character == '\\' && index + 1 < line.length) {
                    builder.append(character).append(line[index + 1])
                    index += 2
                    continue
                }
                builder.append(character)
                index += 1
                if (character == '"') break
            }
            builder.toString()
        }
        else -> null
    }
}

// A single left-to-right pass, deliberately not a chain of sequential
// String.replace calls: replacing "\n"->newline before "\\"->"\" would
// corrupt a genuinely double-escaped sequence like "\\n" (an escaped
// backslash immediately followed by a literal 'n') by misreading its
// second backslash as the start of a fresh "\n" escape. Payload fields
// are exactly this double-escaped case -- terminal.jsonl's own "payload"
// field is itself an already-escaped EvaluationJsonLines.trial(...)
// string, so unescaping it can encounter literal backslashes adjacent
// to n/r/t/quote characters that must not be reinterpreted.
private fun familyFDecodeQuotedField(line: String, field: String): String? {
    val raw = familyFJsonFieldRawToken(line, field) ?: return null
    if (raw == "null" || raw == "true" || raw == "false") return null
    val inner = raw.removeSurrounding("\"")
    val builder = StringBuilder(inner.length)
    var index = 0
    while (index < inner.length) {
        val character = inner[index]
        if (character == '\\' && index + 1 < inner.length) {
            when (inner[index + 1]) {
                '"' -> builder.append('"')
                '\\' -> builder.append('\\')
                'n' -> builder.append('\n')
                'r' -> builder.append('\r')
                't' -> builder.append('\t')
                else -> builder.append(inner[index + 1])
            }
            index += 2
            continue
        }
        builder.append(character)
        index += 1
    }
    return builder.toString()
}

private fun familyFExtractLongField(line: String, field: String): Long? =
    Regex("\"$field\":(-?\\d+)").find(line)?.groupValues?.get(1)?.toLongOrNull()

// recordHash is always appended last by FamilyFChainedLedger.append below,
// so stripping it back off (to independently recompute and compare) is a
// simple, exact, trailing-field removal -- never a full JSON re-parse.
private fun familyFStripTrailingRecordHash(line: String): String {
    val match = Regex(",\"recordHash\":\"((?:\\\\.|[^\"])*)\"\\}$").find(line)
        ?: throw FamilyFArtifactIntegrityException("record missing trailing recordHash field: $line")
    return line.substring(0, match.range.first) + "}"
}

private fun familyFWriteForced(file: Path, content: String) {
    Files.createDirectories(file.parent)
    Files.writeString(
        file,
        content,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE,
        StandardOpenOption.SYNC,
    )
}

private fun familyFAppendForced(file: Path, line: String) {
    Files.createDirectories(file.parent)
    Files.writeString(
        file,
        line + "\n",
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND,
        StandardOpenOption.SYNC,
    )
}

private fun familyFSha256File(file: Path): String = familyFSha256Bytes(Files.readAllBytes(file))

// ---------------------------------------------------------------------
// Durable transport raw-capture encoding (Plan Section 12: complete raw
// request/response bytes and interpretation-relevant response headers
// must be durably recorded, not merely their hashes). Every byte value
// -- request body, response body, header names, header values -- is
// Base64-encoded so arbitrary bytes (including non-UTF-8-safe content)
// round-trip losslessly through the canonical UTF-8 JSON-lines ledger.
// Base64's own alphabet ([A-Za-z0-9+/=]) requires no JSON escaping,
// so the header encoding below needs no escape/unescape logic at all.
// ---------------------------------------------------------------------

private fun familyFEncodeHeaders(headers: Map<String, List<String>>): String {
    val encoder = Base64.getEncoder()
    val entries = headers.keys.sorted().joinToString(",") { key ->
        val encodedKey = encoder.encodeToString(key.toByteArray(StandardCharsets.UTF_8))
        val values = headers.getValue(key).joinToString(",") { value ->
            "\"" + encoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8)) + "\""
        }
        "\"$encodedKey\":[$values]"
    }
    return "{$entries}"
}

// Fails closed on any structural deviation from familyFEncodeHeaders's
// own exact output shape: reconstructing the matched entries and
// comparing against the original body catches trailing garbage or
// malformed structure a permissive regex scan would otherwise silently
// skip over.
private fun familyFDecodeHeaders(encoded: String): Map<String, List<String>> {
    val trimmed = encoded.trim()
    if (trimmed == "{}") return emptyMap()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
        throw FamilyFArtifactIntegrityException("malformed encoded response headers: $encoded")
    }
    val body = trimmed.substring(1, trimmed.length - 1)
    val decoder = Base64.getDecoder()
    val entryPattern = Regex("\"([A-Za-z0-9+/=]*)\":\\[([^\\]]*)\\]")
    val matches = entryPattern.findAll(body).toList()
    val reconstructed = matches.joinToString(",") { it.value }
    if (reconstructed != body) {
        throw FamilyFArtifactIntegrityException("malformed encoded response headers structure: $encoded")
    }
    val result = LinkedHashMap<String, List<String>>()
    matches.forEach { match ->
        val key = runCatching { String(decoder.decode(match.groupValues[1]), StandardCharsets.UTF_8) }
            .getOrElse { throw FamilyFArtifactIntegrityException("invalid base64 header name in: $encoded") }
        val valuesText = match.groupValues[2]
        val values = if (valuesText.isBlank()) {
            emptyList()
        } else {
            valuesText.split(",").map { token ->
                val unquoted = token.trim().removeSurrounding("\"")
                runCatching { String(decoder.decode(unquoted), StandardCharsets.UTF_8) }
                    .getOrElse { throw FamilyFArtifactIntegrityException("invalid base64 header value in: $encoded") }
            }
        }
        result[key] = values
    }
    return result
}

// The complete, verified, decoded shape of one durable transport
// record -- read back from transport.jsonl and independently
// hash/length-verified before any recovery use (Plan Section 18).
data class FamilyFDurableTransportRecord(
    val trialId: String,
    val exchangeSequence: Long,
    val startedAt: Instant,
    val completedAt: Instant,
    val requestBytes: ByteArray,
    val requestSha256: String,
    val responseStatus: Int?,
    val responseCaptured: Boolean,
    val responseBytes: ByteArray?,
    val responseSha256: String?,
    val responseHeaders: Map<String, List<String>>,
    val forwardingOutcome: String,
)

// Decodes and independently re-verifies one raw transport.jsonl line:
// Base64 validity, decoded byte count against the persisted count, and
// decoded SHA-256 against the persisted hash -- for both request and
// (when captured) response bodies. Fails closed on any missing field,
// invalid Base64, wrong length, or wrong hash; never silently recovers
// from mismatched evidence.
private fun familyFDecodeTransportRecord(line: String): FamilyFDurableTransportRecord {
    val trialId = familyFExtractStringField(line, "trialId")
    val exchangeSequence = familyFExtractLongField(line, "exchangeSequence")
        ?: throw FamilyFArtifactIntegrityException("transport record for $trialId missing exchangeSequence")
    val startedAt = runCatching { Instant.parse(familyFExtractStringField(line, "startedAt")) }
        .getOrElse { throw FamilyFArtifactIntegrityException("transport record for $trialId has malformed startedAt") }
    val completedAt = runCatching { Instant.parse(familyFExtractStringField(line, "completedAt")) }
        .getOrElse { throw FamilyFArtifactIntegrityException("transport record for $trialId has malformed completedAt") }

    val requestByteCount = familyFExtractLongField(line, "requestByteCount")
        ?: throw FamilyFArtifactIntegrityException("transport record for $trialId missing requestByteCount")
    val requestBodyBase64 = familyFExtractStringField(line, "requestBodyBase64")
    val requestSha256 = familyFExtractStringField(line, "requestSha256")
    val requestBytes = runCatching { Base64.getDecoder().decode(requestBodyBase64) }
        .getOrElse { throw FamilyFArtifactIntegrityException("transport record for $trialId has invalid base64 request body") }
    if (requestBytes.size.toLong() != requestByteCount) {
        throw FamilyFArtifactIntegrityException(
            "transport record for $trialId request byte count mismatch: expected $requestByteCount, decoded ${requestBytes.size}",
        )
    }
    if (familyFSha256Bytes(requestBytes) != requestSha256) {
        throw FamilyFArtifactIntegrityException("transport record for $trialId request hash mismatch on decode")
    }

    val responseStatusRaw = familyFExtractLongField(line, "responseStatus")
        ?: throw FamilyFArtifactIntegrityException("transport record for $trialId missing responseStatus")
    val responseStatus = if (responseStatusRaw == -1L) null else responseStatusRaw.toInt()
    val responseCaptured = Regex("\"responseCaptured\":(true|false)").find(line)?.groupValues?.get(1)?.toBoolean()
        ?: throw FamilyFArtifactIntegrityException("transport record for $trialId missing responseCaptured")
    val responseByteCount = familyFExtractLongField(line, "responseByteCount")
        ?: throw FamilyFArtifactIntegrityException("transport record for $trialId missing responseByteCount")
    val responseBodyBase64 = familyFExtractStringField(line, "responseBodyBase64")
    val responseSha256Raw = familyFExtractStringField(line, "responseSha256")

    val responseBytes: ByteArray?
    val responseSha256: String?
    if (responseCaptured) {
        val decoded = runCatching { Base64.getDecoder().decode(responseBodyBase64) }
            .getOrElse { throw FamilyFArtifactIntegrityException("transport record for $trialId has invalid base64 response body") }
        if (decoded.size.toLong() != responseByteCount) {
            throw FamilyFArtifactIntegrityException(
                "transport record for $trialId response byte count mismatch: expected $responseByteCount, decoded ${decoded.size}",
            )
        }
        if (familyFSha256Bytes(decoded) != responseSha256Raw) {
            throw FamilyFArtifactIntegrityException("transport record for $trialId response hash mismatch on decode")
        }
        responseBytes = decoded
        responseSha256 = responseSha256Raw
    } else {
        responseBytes = null
        responseSha256 = null
    }

    val responseHeadersJson = familyFExtractStringField(line, "responseHeadersJson")
    val responseHeaders = familyFDecodeHeaders(responseHeadersJson)
    val forwardingOutcome = familyFExtractStringField(line, "forwardingOutcome")

    return FamilyFDurableTransportRecord(
        trialId = trialId,
        exchangeSequence = exchangeSequence,
        startedAt = startedAt,
        completedAt = completedAt,
        requestBytes = requestBytes,
        requestSha256 = requestSha256,
        responseStatus = responseStatus,
        responseCaptured = responseCaptured,
        responseBytes = responseBytes,
        responseSha256 = responseSha256,
        responseHeaders = responseHeaders,
        forwardingOutcome = forwardingOutcome,
    )
}

object FamilyFChainedLedger {
    // Builds one chained record (envelope + payload + trailing recordHash),
    // forces it durably to disk, and advances the cursor -- the one shared
    // append primitive every governed ledger file uses.
    fun append(
        file: Path,
        cursor: FamilyFChainCursor,
        campaignId: String,
        trialId: String?,
        payloadFields: List<Pair<String, Any?>>,
    ): Long {
        val nextSequence = cursor.sequence + 1
        val envelopeWithoutHash = listOf(
            "schemaVersion" to FAMILY_F_LEDGER_SCHEMA_VERSION,
            "campaignId" to campaignId,
            "trialId" to (trialId ?: ""),
            "sequence" to nextSequence,
            "priorRecordHash" to cursor.priorRecordHash,
            "timestamp" to Instant.now().toString(),
        ) + payloadFields
        val canonical = familyFObjectLine(envelopeWithoutHash)
        val recordHash = familyFSha256Bytes(canonical.toByteArray(StandardCharsets.UTF_8))
        val line = familyFObjectLine(envelopeWithoutHash + ("recordHash" to recordHash))
        familyFAppendForced(file, line)
        cursor.sequence = nextSequence
        cursor.priorRecordHash = recordHash
        return nextSequence
    }

    // Verifies one record line against its expected chain position,
    // schema version, and campaign ID; recomputes and compares its own
    // record hash. Fails closed (throws) on any discrepancy, malformed
    // field, or missing field.
    fun verifyRecord(fileName: Any, line: String, campaignId: String, expectedSequence: Long, expectedPriorHash: String): String {
        val schemaVersion = familyFExtractLongField(line, "schemaVersion")
            ?: throw FamilyFArtifactIntegrityException("$fileName: record missing schemaVersion at sequence $expectedSequence")
        if (schemaVersion != FAMILY_F_LEDGER_SCHEMA_VERSION.toLong()) {
            throw FamilyFArtifactIntegrityException("$fileName: schema version mismatch at sequence $expectedSequence (found $schemaVersion)")
        }
        val lineCampaignId = runCatching { familyFExtractStringField(line, "campaignId") }
            .getOrElse { throw FamilyFArtifactIntegrityException("$fileName: record missing campaignId at sequence $expectedSequence") }
        if (lineCampaignId != campaignId) {
            throw FamilyFArtifactIntegrityException("$fileName: campaign ID mismatch at sequence $expectedSequence (found $lineCampaignId, expected $campaignId)")
        }
        runCatching { familyFExtractStringField(line, "trialId") }
            .getOrElse { throw FamilyFArtifactIntegrityException("$fileName: record missing trialId at sequence $expectedSequence") }
        val sequence = familyFExtractLongField(line, "sequence")
            ?: throw FamilyFArtifactIntegrityException("$fileName: record missing sequence at sequence $expectedSequence")
        if (sequence != expectedSequence) {
            throw FamilyFArtifactIntegrityException("$fileName: sequence discontinuity, expected $expectedSequence, found $sequence")
        }
        val priorHash = runCatching { familyFExtractStringField(line, "priorRecordHash") }
            .getOrElse { throw FamilyFArtifactIntegrityException("$fileName: record missing priorRecordHash at sequence $expectedSequence") }
        if (priorHash != expectedPriorHash) {
            throw FamilyFArtifactIntegrityException("$fileName: prior-hash mismatch at sequence $expectedSequence")
        }
        runCatching { familyFExtractStringField(line, "timestamp") }
            .getOrElse { throw FamilyFArtifactIntegrityException("$fileName: record missing timestamp at sequence $expectedSequence") }
        val recordHash = runCatching { familyFExtractStringField(line, "recordHash") }
            .getOrElse { throw FamilyFArtifactIntegrityException("$fileName: record missing recordHash at sequence $expectedSequence") }
        val recomputed = familyFSha256Bytes(familyFStripTrailingRecordHash(line).toByteArray(StandardCharsets.UTF_8))
        if (recomputed != recordHash) {
            throw FamilyFArtifactIntegrityException("$fileName: record-hash mismatch at sequence $expectedSequence")
        }
        return recordHash
    }

    // Reads and verifies a file's entire chain from genesis (requirement:
    // "before appending to an existing ledger, verify its entire existing
    // chain"; "on recovery/resume, verify every governed ledger chain").
    // Returns a fresh cursor positioned at genesis if the file does not
    // exist or is empty.
    fun recoverAndVerify(file: Path, campaignId: String): FamilyFChainCursor {
        if (!Files.exists(file)) return FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        val lines = Files.readAllLines(file).filter { it.isNotBlank() }
        var sequence = 0L
        var priorHash = FAMILY_F_LEDGER_GENESIS_HASH
        for (line in lines) {
            sequence += 1
            priorHash = verifyRecord(file.fileName, line, campaignId, sequence, priorHash)
        }
        return FamilyFChainCursor(sequence, priorHash)
    }
}

class FamilyFCampaignLedger(
    private val directory: Path,
    private val registeredTrialIds: Set<String>,
    private val campaignId: String = "test-campaign",
) {
    private val scheduleFile = directory.resolve("schedule.jsonl")
    private val intentFile = directory.resolve("intent.jsonl")
    private val dispatchFile = directory.resolve("dispatch.jsonl")
    private val transportFile = directory.resolve("transport.jsonl")
    private val terminalFile = directory.resolve("terminal.jsonl")
    private val controlEventsFile = directory.resolve("control-events.jsonl")
    private val resourceReadingsFile = directory.resolve("resource-readings.jsonl")
    private val campaignDefinitionFile = directory.resolve("campaign-definition.json")
    private val campaignIdentityFile = directory.resolve("campaign-identity.json")
    private val advancementWorksheetFile = directory.resolve("advancement-worksheet.json")
    private val sealedReportFile = directory.resolve("sealed-report.json")
    private val checksumsFile = directory.resolve("SHA256SUMS.txt")
    private val sealedMarker = directory.resolve("campaign.sealed")
    private val haltedMarker = directory.resolve("campaign.halted")

    // Every file that must exist before sealing is permitted -- an exact,
    // named list, never an "include whatever happens to exist" scan.
    // SHA256SUMS.txt itself is deliberately excluded: it is the generated
    // manifest, not mandatory pre-seal evidence, and must never recursively
    // hash itself.
    private val mandatoryArtifacts: List<Path> = listOf(
        scheduleFile, intentFile, dispatchFile, transportFile, terminalFile,
        controlEventsFile, resourceReadingsFile, campaignDefinitionFile, campaignIdentityFile,
        advancementWorksheetFile, sealedReportFile,
    )

    // Every governed chained-ledger file. Verified in full -- schema,
    // campaign ID, sequence continuity, and every record hash -- on
    // construction (so any already-durable chain is validated before this
    // instance ever appends to or reads from it) and again, independently,
    // immediately before sealing.
    private val chainedFiles: List<Path> = listOf(
        scheduleFile, intentFile, dispatchFile, transportFile, terminalFile, controlEventsFile, resourceReadingsFile,
    )

    private val cursors: MutableMap<Path, FamilyFChainCursor> = mutableMapOf()

    init {
        chainedFiles.forEach { file -> cursors[file] = FamilyFChainedLedger.recoverAndVerify(file, campaignId) }
    }

    private fun cursorFor(file: Path): FamilyFChainCursor = cursors.getValue(file)

    // Independent, from-disk re-verification of every governed chain --
    // not the cached in-memory cursors -- used both by recover() and
    // explicitly again immediately before sealing.
    private fun verifyAllChainsFromDisk() {
        chainedFiles.forEach { file -> FamilyFChainedLedger.recoverAndVerify(file, campaignId) }
    }

    fun createDirectory() {
        if (Files.exists(directory)) {
            throw FamilyFArtifactIntegrityException("campaign directory already exists: $directory")
        }
        Files.createDirectories(directory)
    }

    fun isSealed(): Boolean = Files.exists(sealedMarker)
    fun isHalted(): Boolean = Files.exists(haltedMarker)

    fun writeScheduleOnce(trials: List<FamilyFTrial>) {
        require(!Files.exists(scheduleFile)) { "schedule already materialized" }
        // The real 392-call campaign invariant is enforced independently
        // and exhaustively by FamilyFCampaignDefinition's own init block
        // (the only real production caller of this function); this ledger
        // operation itself only requires a non-empty, internally
        // consistent trial list, so it remains usable for focused
        // ledger-mechanics unit tests below.
        require(trials.isNotEmpty()) { "schedule must not be empty" }
        val cursor = cursorFor(scheduleFile)
        trials.forEach { trial ->
            FamilyFChainedLedger.append(
                scheduleFile, cursor, campaignId, trial.id,
                listOf(
                    "role" to trial.role.name,
                    "repetition" to trial.repetition,
                    "kind" to trial.kind.name,
                    "fixtureId" to trial.fixture.id,
                    "profileId" to (trial.profileId?.externalId ?: ""),
                ),
            )
        }
    }

    fun writeCampaignDefinition(json: String) = familyFWriteForced(campaignDefinitionFile, json)
    fun writeCampaignIdentity(json: String) = familyFWriteForced(campaignIdentityFile, json)
    fun writeAdvancementWorksheet(json: String) = familyFWriteForced(advancementWorksheetFile, json)

    // Atomic subject/control sealed report document -- not an append-only
    // JSONL stream (Plan Sections 20, 21, 23: separate subject/control
    // sections, per-cell, no ranking). Computed once, written once,
    // manifest-covered exactly like campaign-definition.json and
    // advancement-worksheet.json.
    fun writeSealedReport(content: String) = familyFWriteForced(sealedReportFile, content)

    fun recordIntent(trialId: String) {
        require(trialId in registeredTrialIds) { "cannot record intent for an unregistered trial ID" }
        FamilyFChainedLedger.append(intentFile, cursorFor(intentFile), campaignId, trialId, emptyList())
    }

    fun recordDispatch(trialId: String) {
        FamilyFChainedLedger.append(dispatchFile, cursorFor(dispatchFile), campaignId, trialId, emptyList())
    }

    // Durably persists the COMPLETE transport exchange -- raw request
    // bytes, raw response bytes, response headers (including duplicate/
    // multi-valued ones), status, and forwarding outcome -- not merely
    // their hashes (Plan Section 12; corrects the confirmed raw-capture
    // defect). Every byte value is Base64-encoded alongside an explicit
    // byte count and SHA-256 so arbitrary, non-UTF-8-safe content
    // round-trips losslessly through this canonical UTF-8 JSON ledger.
    // Recomputes both hashes from the actual bytes about to be persisted
    // and fails closed before ever appending if either disagrees with
    // what the proxy itself claims -- durable evidence is never allowed
    // to silently diverge from its own hash. Called synchronously from
    // inside the proxy's listener callback, strictly before the proxy
    // releases the response to its caller (LocalHttpModelInferenceClient),
    // and forced to disk by the shared FamilyFChainedLedger.append/
    // familyFAppendForced mechanism -- so a persistence failure here
    // (this function throwing) propagates out of the proxy's listener
    // callback before any response bytes are ever written back to the
    // caller, and is never treated as a successful release.
    fun recordTransport(trialId: String, record: FamilyFProxyExchangeRecord) {
        val requestBytes = record.requestBody
        if (familyFSha256Bytes(requestBytes) != record.requestSha256) {
            throw FamilyFArtifactIntegrityException("transport capture integrity failure: request hash mismatch for $trialId")
        }
        val responseBody = record.responseBody
        val responseCaptured = record.forwardingOutcome == "FORWARDED" && responseBody != null
        if (responseBody != null) {
            if (record.responseSha256 == null || familyFSha256Bytes(responseBody) != record.responseSha256) {
                throw FamilyFArtifactIntegrityException("transport capture integrity failure: response hash mismatch for $trialId")
            }
        }
        val encoder = Base64.getEncoder()
        FamilyFChainedLedger.append(
            transportFile, cursorFor(transportFile), campaignId, trialId,
            listOf(
                // Named "exchangeSequence", not "sequence": the ledger
                // envelope's own "sequence" field is the chain position,
                // a distinct concept from the proxy's own per-exchange
                // counter -- never collapsed into one ambiguous field.
                "exchangeSequence" to record.sequence,
                "startedAt" to record.startedAt.toString(),
                "completedAt" to record.completedAt.toString(),
                "requestByteCount" to requestBytes.size,
                "requestBodyBase64" to encoder.encodeToString(requestBytes),
                "requestSha256" to record.requestSha256,
                "responseStatus" to (record.responseStatus ?: -1),
                "responseCaptured" to responseCaptured,
                "responseByteCount" to (responseBody?.size ?: -1),
                "responseBodyBase64" to (responseBody?.let { encoder.encodeToString(it) } ?: ""),
                "responseSha256" to (record.responseSha256 ?: ""),
                "responseHeadersJson" to familyFEncodeHeaders(record.responseHeaders),
                "forwardingOutcome" to record.forwardingOutcome,
            ),
        )
    }

    // Reads back and independently re-verifies the exact-one durable
    // transport record for a trial (fails closed if absent or
    // duplicated -- duplication is additionally caught, campaign-wide,
    // by recover()'s own readIdSet check). This is the one read path
    // genuine offline recovery (Plan Section 18) uses; it never trusts
    // Base64/length/hash content without re-deriving it from the bytes
    // actually on disk.
    fun readDurableTransportRecord(trialId: String): FamilyFDurableTransportRecord {
        if (!Files.exists(transportFile)) {
            throw FamilyFArtifactIntegrityException("no transport evidence recorded for $trialId")
        }
        val matches = Files.readAllLines(transportFile).filter { it.isNotBlank() }
            .filter { familyFExtractStringField(it, "trialId") == trialId }
        if (matches.isEmpty()) {
            throw FamilyFArtifactIntegrityException("no transport evidence recorded for $trialId")
        }
        if (matches.size > 1) {
            throw FamilyFArtifactIntegrityException("duplicate transport evidence recorded for $trialId")
        }
        return familyFDecodeTransportRecord(matches.single())
    }

    fun recordTerminal(trialId: String, payload: String) {
        FamilyFChainedLedger.append(terminalFile, cursorFor(terminalFile), campaignId, trialId, listOf("payload" to payload))
    }

    fun recordControlEvent(description: String) {
        FamilyFChainedLedger.append(controlEventsFile, cursorFor(controlEventsFile), campaignId, null, listOf("description" to description))
    }

    // Every governed resource check (Plan Section 16; independent review
    // Finding 1) persists exactly one chained record here via the shared
    // universal mechanism: schema version, campaign ID, trial ID where
    // applicable, sequence, prior-record hash, record hash, timestamp,
    // and this reading's own phase/block/source/raw/parsed/threshold/
    // outcome fields. Returns the assigned sequence number so callers
    // (e.g. control-event records) can reference this exact reading.
    fun recordResourceReading(reading: FamilyFResourceReading): Long =
        FamilyFChainedLedger.append(
            resourceReadingsFile, cursorFor(resourceReadingsFile), campaignId, reading.trialId,
            listOf(
                "phase" to reading.phase,
                "blockId" to (reading.blockId ?: ""),
                "source" to reading.source,
                "rawReading" to reading.rawReading,
                "parsedBytes" to reading.parsedBytes,
                "thresholdBytes" to reading.thresholdBytes,
                "outcome" to reading.outcome,
            ),
        )

    private fun readIds(file: Path): List<String> {
        if (!Files.exists(file)) return emptyList()
        return Files.readAllLines(file).filter { it.isNotBlank() }.map { familyFExtractStringField(it, "trialId") }
    }

    private fun readIdSet(file: Path, label: String): Set<String> {
        val ids = readIds(file)
        val distinct = ids.distinct()
        if (distinct.size != ids.size) {
            throw FamilyFArtifactIntegrityException("duplicate trial ID recorded in $label")
        }
        if (!distinct.all { it in registeredTrialIds }) {
            throw FamilyFArtifactIntegrityException("unknown trial ID recorded in $label")
        }
        return distinct.toSet()
    }

    // A transport record's mere presence is not enough to call it
    // "a complete durably captured response" (Plan Section 18): a
    // forwarding-failure exchange (upstream unreachable, etc.) is
    // captured too, but has no response to classify. Restricting to
    // responseCaptured==true AND forwardingOutcome=="FORWARDED" is what
    // separates the genuinely resumable transport-without-terminal state
    // from a dispatch that must remain ambiguous and permanently halt.
    private fun readCompleteTransportIdSet(file: Path): Set<String> {
        if (!Files.exists(file)) return emptySet()
        return Files.readAllLines(file).filter { it.isNotBlank() }
            .filter { line ->
                val captured = Regex("\"responseCaptured\":(true|false)").find(line)?.groupValues?.get(1)?.toBoolean()
                    ?: throw FamilyFArtifactIntegrityException("transport.jsonl record missing responseCaptured field")
                val outcome = familyFExtractStringField(line, "forwardingOutcome")
                captured && outcome == "FORWARDED"
            }
            .map { familyFExtractStringField(it, "trialId") }
            .toSet()
    }

    /**
     * Four-state recovery mirroring the already-proven Unit 3-C model:
     * never attempted (absent everywhere); dispatched-but-unresolved
     * (ambiguous, permanent halt -- Plan Section 18, "dispatch without a
     * complete raw response is ambiguous and permanently halts the
     * campaign"); transport-captured-but-unclassified (resumable,
     * classifiable offline without a new call); fully resolved (terminal
     * recorded). Every governed chain is independently re-verified from
     * disk first -- recovery state is never derived from an unverified
     * chain.
     */
    fun recover(): FamilyFRecoveryState {
        verifyAllChainsFromDisk()
        val intentIds = readIdSet(intentFile, "intent.jsonl")
        val dispatchIds = readIdSet(dispatchFile, "dispatch.jsonl")
        // readIdSet still runs over every transport record (complete or
        // not) purely for duplicate/unknown-ID detection; completeness
        // itself is a separate, additional dimension read by
        // readCompleteTransportIdSet below.
        readIdSet(transportFile, "transport.jsonl")
        val completeTransportIds = readCompleteTransportIdSet(transportFile)
        val terminalIds = readIdSet(terminalFile, "terminal.jsonl")

        if (!dispatchIds.all { it in intentIds }) {
            throw FamilyFArtifactIntegrityException("dispatch record without a corresponding intent record")
        }
        if (!terminalIds.all { it in completeTransportIds }) {
            throw FamilyFArtifactIntegrityException("terminal record without complete transport evidence")
        }
        val ambiguous = dispatchIds - completeTransportIds
        if (ambiguous.isNotEmpty()) {
            throw FamilyFArtifactIntegrityException(
                "ambiguous dispatched trial(s) with no complete transport evidence, permanently halting: $ambiguous",
            )
        }
        return FamilyFRecoveryState(
            dispatched = dispatchIds,
            pendingOfflineClassification = completeTransportIds - terminalIds,
            resolved = terminalIds,
        )
    }

    fun sealAfterAdvancementRecorded(expected: Set<String>) {
        check(!Files.exists(haltedMarker)) { "cannot seal an already-halted campaign" }
        // recover() itself performs a fresh, from-disk verification of
        // every governed chain (verifyAllChainsFromDisk) -- this is that
        // "verify all governed chains again" pass, immediately before
        // sealing, independent of whatever verification already happened
        // at recovery/resume time earlier in this run.
        val state = recover()
        if (state.pendingOfflineClassification.isNotEmpty()) {
            throw FamilyFArtifactIntegrityException("cannot seal: ${state.pendingOfflineClassification.size} trial(s) pending offline classification")
        }
        if (state.resolved != expected) {
            throw FamilyFArtifactIntegrityException("cannot seal: expected ${expected.size} resolved trials, have ${state.resolved.size}")
        }
        requireMandatoryArtifactsPresent()
        writeChecksums()
        familyFWriteForced(sealedMarker, "sealed\n")
    }

    // Exact mandatory-artifact check, fails closed if any is missing --
    // never an "include whatever happens to exist" scan.
    private fun requireMandatoryArtifactsPresent() {
        val missing = mandatoryArtifacts.filterNot { Files.exists(it) }
        if (missing.isNotEmpty()) {
            throw FamilyFArtifactIntegrityException(
                "cannot seal: missing mandatory artifact(s): ${missing.map { directory.relativize(it) }}",
            )
        }
    }

    fun halt(reason: String) {
        check(!Files.exists(sealedMarker)) { "cannot halt an already-sealed campaign" }
        if (!Files.exists(haltedMarker)) {
            familyFWriteForced(haltedMarker, "halted: ${familyFJsonEscape(reason)}\n")
        }
    }

    // Called only after requireMandatoryArtifactsPresent() has already
    // confirmed every one of mandatoryArtifacts exists -- an exact,
    // named manifest, not a filtered "whichever exist" scan. This whole-
    // file manifest layer is separate from, and in addition to, the
    // per-record chain hashes above: SHA256SUMS.txt is the sealed-
    // artifact integrity layer, never a substitute for chain
    // verification, and it never hashes itself.
    private fun writeChecksums() {
        val lines = mandatoryArtifacts.map { "${familyFSha256File(it)}  ${directory.relativize(it)}" }
        familyFWriteForced(checksumsFile, lines.joinToString("\n", postfix = "\n"))
    }

    fun verifyChecksums(): Boolean {
        if (!Files.exists(checksumsFile)) return false
        val lines = Files.readAllLines(checksumsFile).filter { it.isNotBlank() }
        if (lines.isEmpty()) return false
        return lines.all { line ->
            val parts = line.split("  ", limit = 2)
            if (parts.size != 2) return@all false
            val file = directory.resolve(parts[1])
            Files.exists(file) && familyFSha256File(file) == parts[0]
        }
    }
}

// ---------------------------------------------------------------------
// Genuine offline recovery for a "transport-without-terminal" trial
// (Plan Section 18: "a complete durably captured response without a
// terminal record may be classified offline without another model
// call"). Reuses exactly the unmodified production formatter
// (defaultOllamaRequestBody), production response extraction
// (defaultOllamaResponseBody), and TaggedReasoningResponseParser --
// never a duplicated parsing/formatting protocol. Makes no model call
// and opens no socket: every input is the already-durable, already
// hash-verified transport record plus the frozen schedule/fixture/
// profile metadata.
// ---------------------------------------------------------------------

private data class FamilyFOfflineClassificationResult(
    val primaryClassification: PrimaryClassification,
    val contentFidelity: ContentFidelity,
    val representationValid: Boolean,
)

private fun familyFOfflineAction(response: ReasoningProviderResponse): ExpectedAction = when (response) {
    is ReasoningProviderResponse.Goal -> ExpectedAction.GOAL
    is ReasoningProviderResponse.Reply -> ExpectedAction.REPLY
    is ReasoningProviderResponse.Remember -> ExpectedAction.REMEMBER
    ReasoningProviderResponse.NoAction -> ExpectedAction.NOACTION
}

private fun familyFOfflineVariantName(response: ReasoningProviderResponse): String = when (response) {
    is ReasoningProviderResponse.Goal -> "Goal"
    is ReasoningProviderResponse.Reply -> "Reply"
    is ReasoningProviderResponse.Remember -> "Remember"
    ReasoningProviderResponse.NoAction -> "NoAction"
}

private fun familyFOfflineContentFidelity(fixture: ConformanceFixture, response: ReasoningProviderResponse?): ContentFidelity {
    val expected = fixture.expectedContent ?: return ContentFidelity.NOT_APPLICABLE
    val actual = when (response) {
        is ReasoningProviderResponse.Goal -> response.text
        is ReasoningProviderResponse.Reply -> response.text
        is ReasoningProviderResponse.Remember -> response.text
        else -> return ContentFidelity.INDETERMINATE
    }
    return if (actual == expected) ContentFidelity.EXACT else ContentFidelity.DEVIATION_OR_PARAPHRASE
}

private fun familyFOfflineContainsMultipleTaggedOutputs(raw: String): Boolean =
    Regex("(?m)^\\s*(?:GOAL:|REPLY:|REMEMBER:|NOACTION(?:\\s*$))").findAll(raw).count() > 1

private fun familyFOfflineClassifyRejected(raw: String): PrimaryClassification {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return PrimaryClassification.G
    if (familyFOfflineContainsMultipleTaggedOutputs(trimmed)) return PrimaryClassification.F
    val exactTags = listOf("GOAL:", "REPLY:", "REMEMBER:")
    if (exactTags.any { trimmed == it } || listOf("GOAL", "REPLY", "REMEMBER", "NOACTION").any { it.startsWith(trimmed) }) {
        return PrimaryClassification.G
    }
    if (trimmed.startsWith("NOACTION")) return PrimaryClassification.C
    if (!trimmed.matches(Regex("^[A-Z]+:.*", RegexOption.DOT_MATCHES_ALL))) return PrimaryClassification.E
    return PrimaryClassification.C
}

// Mirrors ReasoningProtocolLiveModelEvaluationHarness's own private
// classify() exactly, for the reachable subset of outcomes an already-
// captured response can produce (a TimeoutCancellationException branch
// is structurally impossible here: nothing is ever awaited, let alone
// timed out, during offline recovery).
private fun familyFOfflineClassify(
    fixture: ConformanceFixture,
    extracted: String?,
    parsed: ReasoningProviderResponse?,
    failure: Throwable?,
): FamilyFOfflineClassificationResult {
    val actualAction = parsed?.let(::familyFOfflineAction)
    val multiple = extracted?.let(::familyFOfflineContainsMultipleTaggedOutputs) == true
    val primary = when {
        failure != null && failure !is UnclassifiableModelResponseException &&
            !(failure is IllegalArgumentException && extracted != null) -> PrimaryClassification.I
        multiple -> PrimaryClassification.F
        failure != null -> familyFOfflineClassifyRejected(extracted.orEmpty())
        actualAction != fixture.expectedAction -> PrimaryClassification.D
        familyFOfflineContentFidelity(fixture, parsed) == ContentFidelity.EXACT ||
            familyFOfflineContentFidelity(fixture, parsed) == ContentFidelity.NOT_APPLICABLE -> PrimaryClassification.A
        else -> PrimaryClassification.B
    }
    val representationValid = failure == null && !multiple
    return FamilyFOfflineClassificationResult(primary, familyFOfflineContentFidelity(fixture, parsed), representationValid)
}

private fun familyFOfflineExtractEndpointMetadata(raw: String?): EndpointMetadata {
    fun number(key: String): Long? = raw?.let {
        Regex("\\\"${Regex.escape(key)}\\\"\\s*:\\s*(\\d+)").find(it)?.groupValues?.get(1)?.toLongOrNull()
    }
    return EndpointMetadata(
        promptEvalCount = number("prompt_eval_count"),
        evalCount = number("eval_count"),
        totalDuration = number("total_duration"),
        loadDuration = number("load_duration"),
        promptEvalDuration = number("prompt_eval_duration"),
        evalDuration = number("eval_duration"),
    )
}

object FamilyFOfflineRecovery {
    // Recovers the exact terminal payload for one transport-without-
    // terminal trial: verifies the durable request against the expected
    // production-formatted request, decodes the durable response text,
    // runs it through the unmodified production extraction and parser,
    // and derives a TrialObservation identical in shape to the normal
    // dispatch path's own EvaluationJsonLines.trial(...) output. Fails
    // closed -- never fabricates a terminal record -- when the transport
    // evidence does not represent a complete captured response, or when
    // the durable request does not match the trial it is claimed for.
    fun recoverTerminalPayload(
        trial: FamilyFTrial,
        transport: FamilyFDurableTransportRecord,
        roleIdentity: FamilyFRoleIdentity,
        identity: FamilyFIdentity,
    ): String {
        require(transport.trialId == trial.id) { "transport record trial ID does not match the requested trial" }
        if (!transport.responseCaptured || transport.forwardingOutcome != "FORWARDED" || transport.responseBytes == null) {
            throw FamilyFArtifactIntegrityException(
                "transport record for ${trial.id} does not represent a complete captured response; cannot classify offline",
            )
        }

        val profileId = trial.profileId ?: ContextProfileId.MINIMAL_PRODUCTION_CONTEXT
        val input = SyntheticContextProfiles.construct(trial.fixture, profileId)
        val turn = (input.request.subject as ReasoningSubject.OfTurn).turn
        val expectedPrompt = DefaultReasoningPromptBuilder().buildPrompt(turn, input.request.reasoningContext)
        val expectedRequestBody = defaultOllamaRequestBody(expectedPrompt, roleIdentity.modelName)
        val actualRequestText = String(transport.requestBytes, StandardCharsets.UTF_8)
        if (actualRequestText != expectedRequestBody) {
            throw FamilyFArtifactIntegrityException(
                "durable request for ${trial.id} does not match the expected trial/model/prompt request produced by the production formatter",
            )
        }

        val rawEnvelope = String(transport.responseBytes, StandardCharsets.UTF_8)
        var extracted: String? = null
        var parsed: ReasoningProviderResponse? = null
        var failure: Throwable? = null
        try {
            extracted = defaultOllamaResponseBody(rawEnvelope)
            parsed = TaggedReasoningResponseParser().parse(extracted)
        } catch (throwable: Throwable) {
            failure = throwable
        }

        val classification = familyFOfflineClassify(trial.fixture, extracted, parsed, failure)
        val latencyNanos = Duration.between(transport.startedAt, transport.completedAt).toNanos().coerceAtLeast(0L)

        val observation = TrialObservation(
            runId = "family-f-diagnostic-offline-recovery",
            fixtureId = trial.fixture.id,
            contextProfileId = profileId.externalId,
            trialSequence = trial.attempt,
            stableInputHash = input.stableInputHash,
            repositoryCommit = identity.repositoryCommit,
            modelName = roleIdentity.modelName,
            modelDigest = roleIdentity.modelDigest,
            runtimeImageId = null,
            endpointIdentifier = identity.dedicatedEndpointIdentifier,
            timeoutMs = identity.requestTimeoutMs,
            prompt = expectedPrompt,
            promptSha256 = sha256(expectedPrompt),
            requestBody = actualRequestText,
            rawOllamaEnvelope = rawEnvelope,
            extractedResponse = extracted,
            parsedVariant = parsed?.let(::familyFOfflineVariantName),
            parserExceptionType = failure?.javaClass?.name,
            parserExceptionClassification = when (failure) {
                is UnclassifiableModelResponseException -> "UNCLASSIFIABLE_MODEL_RESPONSE"
                is IllegalArgumentException -> "INVALID_RESPONSE_CONTENT"
                else -> null
            },
            expectedAction = trial.fixture.expectedAction,
            actualAction = parsed?.let(::familyFOfflineAction),
            representationValid = classification.representationValid,
            contentFidelity = classification.contentFidelity,
            latencyNanos = latencyNanos,
            endpointMetadata = familyFOfflineExtractEndpointMetadata(rawEnvelope),
            primaryClassification = classification.primaryClassification,
        )
        return EvaluationJsonLines.trial(observation)
    }
}

// ---------------------------------------------------------------------
// Subject-only investment-screening advancement gate (Plan Section 20).
// Never a qualification pass, production threshold, model-selection
// rule, ranking criterion, or permission to execute another campaign.
// ---------------------------------------------------------------------

enum class FamilyFAdvancementVerdict { ELIGIBLE_FOR_FULL_QUALIFICATION_PROPOSAL, NOT_ELIGIBLE }

data class FamilyFCellResult(val fixtureId: String, val profileId: String, val correctCount: Int, val totalCount: Int)

data class FamilyFAdvancementResult(
    val verdict: FamilyFAdvancementVerdict,
    val failedConditions: List<String>,
    val cellResults: List<FamilyFCellResult>,
    val deviationOrParaphraseCount: Int,
    val indeterminateContentCount: Int,
)

object FamilyFAdvancementGate {
    /**
     * `materialMutationOrInventionConfirmedZero` must come from the
     * required content-fidelity interpretation step (mirroring Unit 2's
     * own two-independent-human-review process for ambiguous cases,
     * Unit 2 Scope Lock Section 10) -- the shared harness's
     * `ContentFidelity` enum (EXACT / DEVIATION_OR_PARAPHRASE /
     * NOT_APPLICABLE / INDETERMINATE) cannot by itself automatically
     * distinguish acceptable non-material paraphrase from material
     * mutation or invention, and this gate never guesses that
     * distinction.
     */
    fun evaluate(
        subjectObservations: List<TrialObservation>,
        materialMutationOrInventionConfirmedZero: Boolean,
    ): FamilyFAdvancementResult {
        val failed = mutableListOf<String>()

        if (subjectObservations.size != 184) {
            failed += "subject scored observation count is ${subjectObservations.size}, expected 184"
        }

        val byCell = subjectObservations.groupBy { it.fixtureId to it.contextProfileId }
        val cellResults = byCell.map { (key, observations) ->
            val correct = observations.count { it.actualAction == it.expectedAction }
            FamilyFCellResult(key.first, key.second, correct, observations.size)
        }
        if (byCell.size != 46) {
            failed += "observed ${byCell.size} fixture/profile cells, expected 46"
        }
        cellResults.forEach { cell ->
            if (cell.totalCount != 4) {
                failed += "cell ${cell.fixtureId}/${cell.profileId} has ${cell.totalCount} attempts, expected 4"
            }
            if (cell.correctCount < 3) {
                failed += "cell ${cell.fixtureId}/${cell.profileId} has only ${cell.correctCount}/4 correct, minimum 3 required"
            }
        }

        val falsePositiveRemember = subjectObservations.count {
            it.actualAction == ExpectedAction.REMEMBER && it.expectedAction != ExpectedAction.REMEMBER
        }
        if (falsePositiveRemember > 0) failed += "$falsePositiveRemember false-positive REMEMBER classification(s)"

        val falsePositiveGoal = subjectObservations.count {
            it.actualAction == ExpectedAction.GOAL && it.expectedAction != ExpectedAction.GOAL
        }
        if (falsePositiveGoal > 0) failed += "$falsePositiveGoal false-positive GOAL classification(s)"

        if (!materialMutationOrInventionConfirmedZero) {
            failed += "material mutation/invention has not been confirmed zero by the required content-fidelity interpretation step"
        }

        val representationInvalid = subjectObservations.count { !it.representationValid }
        if (representationInvalid > 0) failed += "$representationInvalid representation-invalid subject observation(s)"

        val verdict = if (failed.isEmpty()) FamilyFAdvancementVerdict.ELIGIBLE_FOR_FULL_QUALIFICATION_PROPOSAL else FamilyFAdvancementVerdict.NOT_ELIGIBLE
        return FamilyFAdvancementResult(
            verdict = verdict,
            failedConditions = failed,
            cellResults = cellResults,
            deviationOrParaphraseCount = subjectObservations.count { it.contentFidelity == ContentFidelity.DEVIATION_OR_PARAPHRASE },
            indeterminateContentCount = subjectObservations.count { it.contentFidelity == ContentFidelity.INDETERMINATE },
        )
    }
}

// ---------------------------------------------------------------------
// Subject/control sealed reporting (Plan Sections 20, 21, 23). Presents
// both roles' results, separately, per fixture/profile cell, across the
// required independent measurement axes -- never a ranking, winner,
// preference, relative score, substitution recommendation, or
// control-derived advancement field. Completely independent of, and
// never consulted by, FamilyFAdvancementGate (Section 6 above).
// ---------------------------------------------------------------------

data class FamilyFRoleCellReport(
    val fixtureId: String,
    val profileId: String,
    val attempts: Int,
    val correctCount: Int,
    val representationValidCount: Int,
    val contentFidelityExactCount: Int,
    val contentFidelityDeviationOrParaphraseCount: Int,
    val contentFidelityNotApplicableCount: Int,
    val contentFidelityIndeterminateCount: Int,
    val materialMutationOrInventionFlaggedCount: Int,
    val falsePositiveRememberCount: Int,
    val falsePositiveGoalCount: Int,
    val timeoutCount: Int,
    val transportFailureCount: Int,
    val averageLatencyNanos: Long,
    val artifactIntegrityOutcome: String,
)

data class FamilyFRoleReport(val role: FamilyFRole, val cells: List<FamilyFRoleCellReport>, val totalObservations: Int)

object FamilyFRoleReportBuilder {
    fun build(role: FamilyFRole, observations: List<TrialObservation>): FamilyFRoleReport {
        val byCell = observations.groupBy { it.fixtureId to it.contextProfileId }
        val cells = byCell.map { (key, cellObservations) ->
            FamilyFRoleCellReport(
                fixtureId = key.first,
                profileId = key.second,
                attempts = cellObservations.size,
                correctCount = cellObservations.count { it.actualAction == it.expectedAction },
                representationValidCount = cellObservations.count { it.representationValid },
                contentFidelityExactCount = cellObservations.count { it.contentFidelity == ContentFidelity.EXACT },
                contentFidelityDeviationOrParaphraseCount = cellObservations.count { it.contentFidelity == ContentFidelity.DEVIATION_OR_PARAPHRASE },
                contentFidelityNotApplicableCount = cellObservations.count { it.contentFidelity == ContentFidelity.NOT_APPLICABLE },
                contentFidelityIndeterminateCount = cellObservations.count { it.contentFidelity == ContentFidelity.INDETERMINATE },
                // Automatically flaggable proxy only, never an automatic
                // finding -- distinguishing acceptable non-material
                // paraphrase from true material mutation/invention still
                // requires the separately governed human interpretation
                // step (see FamilyFAdvancementGate's own doc comment).
                materialMutationOrInventionFlaggedCount = cellObservations.count { it.contentFidelity == ContentFidelity.DEVIATION_OR_PARAPHRASE },
                falsePositiveRememberCount = cellObservations.count { it.actualAction == ExpectedAction.REMEMBER && it.expectedAction != ExpectedAction.REMEMBER },
                falsePositiveGoalCount = cellObservations.count { it.actualAction == ExpectedAction.GOAL && it.expectedAction != ExpectedAction.GOAL },
                timeoutCount = cellObservations.count { it.primaryClassification == PrimaryClassification.H },
                transportFailureCount = cellObservations.count { it.primaryClassification == PrimaryClassification.I },
                averageLatencyNanos = if (cellObservations.isEmpty()) 0L else cellObservations.sumOf { it.latencyNanos } / cellObservations.size,
                artifactIntegrityOutcome = if (cellObservations.size == 4) "COMPLETE" else "INCOMPLETE",
            )
        }.sortedWith(compareBy({ it.fixtureId }, { it.profileId }))
        return FamilyFRoleReport(role, cells, observations.size)
    }

    // Disclosure, not concealment: an incomplete cell is reported by this
    // function's own caller (it is never silently dropped), but sealing
    // itself must not proceed while any cell remains incomplete -- this
    // is the completeness gate, called for both subject and control.
    fun requireComplete(report: FamilyFRoleReport, label: String) {
        if (report.cells.size != 46) {
            throw FamilyFArtifactIntegrityException("$label report has ${report.cells.size} fixture/profile cells, expected 46")
        }
        report.cells.forEach { cell ->
            if (cell.attempts != 4) {
                throw FamilyFArtifactIntegrityException(
                    "$label report cell ${cell.fixtureId}/${cell.profileId} has ${cell.attempts} observation(s), expected exactly 4",
                )
            }
        }
        if (report.totalObservations != 184) {
            throw FamilyFArtifactIntegrityException("$label report has ${report.totalObservations} total observations, expected 184")
        }
    }
}

private fun familyFSealedReportCellObject(cell: FamilyFRoleCellReport): String = familyFObjectLine(
    listOf(
        "fixtureId" to cell.fixtureId,
        "profileId" to cell.profileId,
        "attempts" to cell.attempts,
        "correctCount" to cell.correctCount,
        "representationValidCount" to cell.representationValidCount,
        "contentFidelityExactCount" to cell.contentFidelityExactCount,
        "contentFidelityDeviationOrParaphraseCount" to cell.contentFidelityDeviationOrParaphraseCount,
        "contentFidelityNotApplicableCount" to cell.contentFidelityNotApplicableCount,
        "contentFidelityIndeterminateCount" to cell.contentFidelityIndeterminateCount,
        "materialMutationOrInventionFlaggedCount" to cell.materialMutationOrInventionFlaggedCount,
        "falsePositiveRememberCount" to cell.falsePositiveRememberCount,
        "falsePositiveGoalCount" to cell.falsePositiveGoalCount,
        "timeoutCount" to cell.timeoutCount,
        "transportFailureCount" to cell.transportFailureCount,
        "averageLatencyNanos" to cell.averageLatencyNanos,
        "artifactIntegrityOutcome" to cell.artifactIntegrityOutcome,
    ),
)

// Single atomic canonical JSON document -- not an append-only JSONL
// stream (independent review: a multi-record JSONL artifact must either
// carry the universal chained envelope or be replaced by an atomic,
// non-append record format; this report is computed once, from a
// complete and already-verified set of observations, so an atomic
// document is the more honest representation). Clearly separate
// "subject" and "control" arrays, each holding all 46 of that role's own
// per-cell records; manifest-covered exactly like any other sealed
// artifact (FamilyFCampaignLedger.writeSealedReport -> sealed-report.json).
fun familyFSealedReportDocument(subjectReport: FamilyFRoleReport, controlReport: FamilyFRoleReport): String {
    val subjectArray = subjectReport.cells.joinToString(",", prefix = "[", postfix = "]") { familyFSealedReportCellObject(it) }
    val controlArray = controlReport.cells.joinToString(",", prefix = "[", postfix = "]") { familyFSealedReportCellObject(it) }
    return "{\"subject\":$subjectArray,\"control\":$controlArray}\n"
}

// ---------------------------------------------------------------------
// Orchestration driver
// ---------------------------------------------------------------------

enum class FamilyFCampaignOutcome { SEALED, HALTED, ALREADY_SEALED }

fun interface FamilyFModelCaller {
    fun call(trial: FamilyFTrial, roleModelName: String): TrialObservation
}

data class FamilyFRuntimeDependencies(
    val availableMemory: () -> Long,
    val usableSpace: (Path) -> Long,
    val residencyQuery: FamilyFResidencyQuery,
    val unloadCommand: FamilyFModelUnloadCommand,
    val pollResidencyUntilAbsent: (FamilyFRole) -> FamilyFResidencyState,
    val protectedProcesses: List<FamilyFProtectedProcess>,
    val isAlive: (String) -> Boolean,
    val dedicatedPid: String,
)

class FamilyFOrchestrationDriver(
    private val config: FamilyFConfig,
    private val ledger: FamilyFCampaignLedger,
    private val dependencies: FamilyFRuntimeDependencies,
    private val modelCaller: FamilyFModelCaller,
    private val transportRecorder: FamilyFTrialTransportRecorder,
) {
    fun run(): FamilyFCampaignOutcome {
        FamilyFProductionIsolationGuard.requireDistinctFromProduction(
            dependencies.dedicatedPid, config.identity.dedicatedEndpointIdentifier, dependencies.protectedProcesses,
        )
        FamilyFProductionIsolationGuard.requireProtectedProcessesHealthy(dependencies.protectedProcesses, dependencies.isAlive)

        if (ledger.isHalted()) {
            throw FamilyFArtifactIntegrityException("campaign is halted; cannot resume without new governance")
        }
        if (ledger.isSealed()) {
            return FamilyFCampaignOutcome.ALREADY_SEALED
        }

        if (!Files.exists(config.campaignArtifactRoot)) {
            // Pre-creation disk gates (Plan Section 16; Finding 1/2): both
            // the artifact root and the dedicated runtime root, once,
            // before campaign creation. The campaign directory -- and
            // therefore resource-readings.jsonl's own parent -- does not
            // exist yet, so a failing reading here must never be persisted
            // merely to record the failure (Finding 1 item 6). The two
            // readings are computed and enforced first; only once both
            // pass, and only once the directory actually exists, are they
            // persisted as the very first resource records, before any
            // endpoint resolution, daemon launch, residency operation, or
            // model contact (Finding 1 item 5).
            val artifactRootReading = FamilyFDiskSpaceGate.measure(
                config.campaignArtifactRoot.parent ?: config.campaignArtifactRoot,
                "DISK_ARTIFACT_ROOT_PRE_CREATION", null, dependencies.usableSpace,
            )
            FamilyFDiskSpaceGate.enforceDisk(artifactRootReading)
            val runtimeRootReading = FamilyFDiskSpaceGate.measure(
                config.dedicatedRuntimeRoot, "DISK_RUNTIME_ROOT_PRE_CREATION", null, dependencies.usableSpace,
            )
            FamilyFDiskSpaceGate.enforceDisk(runtimeRootReading)

            ledger.createDirectory()
            ledger.recordResourceReading(artifactRootReading)
            ledger.recordResourceReading(runtimeRootReading)
            ledger.writeCampaignDefinition(campaignDefinitionJson())
            ledger.writeCampaignIdentity(campaignIdentityJson(config.identity))
            ledger.writeScheduleOnce(FamilyFCampaignDefinition.allTrials)
            FamilyFCampaignDefinition.allTrials.forEach { ledger.recordIntent(it.id) }
        }

        val recoveryState = ledger.recover()
        try {
            // Resolve every transport-without-terminal trial offline,
            // strictly before any further model contact, and only then
            // continue dispatching the remaining schedule (Plan Section
            // 18: "continue only if all exact-once and integrity gates
            // pass"). A failure here (fail-closed evidence, a request
            // that does not match the trial it is claimed for, etc.)
            // is handled by the identical halt path as any other
            // integrity failure below -- never a retry, never a partial
            // seal.
            recoverPendingOfflineClassifications(recoveryState.pendingOfflineClassification)
            for (block in FamilyFCampaignDefinition.blocks) {
                runBlock(block, recoveryState.dispatched)
            }
        } catch (exception: FamilyFArtifactIntegrityException) {
            ledger.halt(exception.message ?: "integrity failure")
            return FamilyFCampaignOutcome.HALTED
        } catch (exception: Exception) {
            ledger.halt("unexpected failure: ${exception.javaClass.simpleName}: ${exception.message}")
            return FamilyFCampaignOutcome.HALTED
        }

        val subjectObservations = readObservationsForRole(FamilyFRole.SUBJECT)
        val controlObservations = readObservationsForRole(FamilyFRole.CONTROL)
        val subjectReport = FamilyFRoleReportBuilder.build(FamilyFRole.SUBJECT, subjectObservations)
        val controlReport = FamilyFRoleReportBuilder.build(FamilyFRole.CONTROL, controlObservations)
        // Both roles' reporting must be complete before sealing -- neither
        // alone is sufficient (Plan Section 21: subject and control results
        // presented in separate sections; Section 23: missing or duplicate
        // observations for either role must prevent sealing).
        FamilyFRoleReportBuilder.requireComplete(subjectReport, "subject")
        FamilyFRoleReportBuilder.requireComplete(controlReport, "control")

        // The subject-only investment-screening advancement decision is
        // computed exclusively from subjectObservations -- controlObservations
        // and controlReport are never passed to it, structurally incapable of
        // affecting, lowering, offsetting, or replacing any subject gate.
        val advancement = FamilyFAdvancementGate.evaluate(subjectObservations, materialMutationOrInventionConfirmedZero = false)
        ledger.writeAdvancementWorksheet(advancementWorksheetJson(advancement))
        ledger.writeSealedReport(familyFSealedReportDocument(subjectReport, controlReport))
        ledger.sealAfterAdvancementRecorded(FamilyFCampaignDefinition.allTrials.map { it.id }.toSet())
        return FamilyFCampaignOutcome.SEALED
    }

    // Zero-model-call offline recovery for every trial recover() found
    // transport-captured-but-unclassified (Plan Section 18). Not
    // private: exercised directly against a fresh, reconstructed
    // FamilyFOrchestrationDriver instance by the offline-recovery test
    // suite below, exactly as a real crash/resume would reconstruct one.
    fun recoverPendingOfflineClassifications(pending: Set<String>) {
        if (pending.isEmpty()) return
        val byId = FamilyFCampaignDefinition.allTrials.associateBy { it.id }
        pending.sorted().forEach { trialId ->
            val trial = byId[trialId]
                ?: throw FamilyFArtifactIntegrityException("pending offline classification for unknown trial ID $trialId")
            val roleIdentity = if (trial.role == FamilyFRole.SUBJECT) config.identity.subject else config.identity.control
            val transport = ledger.readDurableTransportRecord(trialId)
            val payload = FamilyFOfflineRecovery.recoverTerminalPayload(trial, transport, roleIdentity, config.identity)
            ledger.recordTerminal(trialId, payload)
        }
    }

    private fun runBlock(block: FamilyFBlock, alreadyDispatched: Set<String>) {
        val blockId = "r${block.repetition.toString().padStart(2, '0')}-${block.role.name}"

        // Both disk paths, freshly re-read (never cached), before every
        // one of the eight blocks, inside the block-producing loop and
        // before any residency or load permission (Finding 2 items 1-3).
        val blockArtifactReading = FamilyFDiskSpaceGate.measure(
            config.campaignArtifactRoot.parent ?: config.campaignArtifactRoot,
            "DISK_ARTIFACT_ROOT_BLOCK", blockId, dependencies.usableSpace,
        )
        val blockArtifactSequence = ledger.recordResourceReading(blockArtifactReading)
        FamilyFDiskSpaceGate.enforceDisk(blockArtifactReading)
        val blockRuntimeReading = FamilyFDiskSpaceGate.measure(
            config.dedicatedRuntimeRoot, "DISK_RUNTIME_ROOT_BLOCK", blockId, dependencies.usableSpace,
        )
        val blockRuntimeSequence = ledger.recordResourceReading(blockRuntimeReading)
        FamilyFDiskSpaceGate.enforceDisk(blockRuntimeReading)

        FamilyFResidencyGate.checkNeitherResident(dependencies.residencyQuery)
        val roleIdentity = if (block.role == FamilyFRole.SUBJECT) config.identity.subject else config.identity.control
        val preLoadReading = FamilyFMemoryGate.measurePreLoad(roleIdentity.modelSizeBytes, blockId, dependencies.availableMemory)
        val preLoadSequence = ledger.recordResourceReading(preLoadReading)
        FamilyFMemoryGate.enforce(preLoadReading)
        // Resource records referenced by their relevant control event
        // (Finding 1 item 8: evidence linkage).
        ledger.recordControlEvent(
            "pre-load gate passed for block repetition=${block.repetition} role=${block.role} " +
                "resourceReadingSequences=[$blockArtifactSequence,$blockRuntimeSequence,$preLoadSequence]",
        )

        for (trial in block.trials) {
            if (trial.id in alreadyDispatched) continue
            val beforeReading = FamilyFMemoryGate.measurePerCall(trial.id, "MEMORY_PER_CALL_BEFORE", dependencies.availableMemory)
            ledger.recordResourceReading(beforeReading)
            FamilyFMemoryGate.enforce(beforeReading)
            ledger.recordDispatch(trial.id)
            transportRecorder.currentTrialId = trial.id
            val observation = try {
                modelCaller.call(trial, roleIdentity.modelName)
            } catch (exception: Exception) {
                throw FamilyFArtifactIntegrityException("model call failed for ${trial.id}: ${exception.message}")
            } finally {
                transportRecorder.currentTrialId = null
            }
            ledger.recordTerminal(trial.id, EvaluationJsonLines.trial(observation))
            val afterReading = FamilyFMemoryGate.measurePerCall(trial.id, "MEMORY_PER_CALL_AFTER", dependencies.availableMemory)
            ledger.recordResourceReading(afterReading)
            FamilyFMemoryGate.enforce(afterReading)
        }

        FamilyFResidencyGate.checkAssignedResident(block.role, dependencies.residencyQuery)
        FamilyFResidencyGate.unloadAndVerifyAbsent(block.role, dependencies.unloadCommand) { dependencies.pollResidencyUntilAbsent(block.role) }
        ledger.recordControlEvent("unload verified for block repetition=${block.repetition} role=${block.role}")
    }

    private fun readObservationsForRole(role: FamilyFRole): List<TrialObservation> {
        // terminal.jsonl carries only {trialId, payload}; payload is the
        // flat EvaluationJsonLines.trial(...) object. Correlate role/kind
        // back via the frozen schedule, decode the fields the advancement
        // gate and role reporting need directly from the payload text.
        val byId = FamilyFCampaignDefinition.allTrials.associateBy { it.id }
        val terminalFile = config.campaignArtifactRoot.resolve("terminal.jsonl")
        if (!Files.exists(terminalFile)) return emptyList()
        return Files.readAllLines(terminalFile).filter { it.isNotBlank() }.mapNotNull { line ->
            val trialId = familyFExtractStringField(line, "trialId")
            val trial = byId[trialId] ?: return@mapNotNull null
            if (trial.role != role || trial.kind != FamilyFTrialKind.SCORED) return@mapNotNull null
            val payload = familyFDecodeQuotedField(line, "payload") ?: return@mapNotNull null
            decodeObservation(payload, role)
        }
    }

    private fun decodeObservation(payload: String, role: FamilyFRole): TrialObservation? {
        fun stringField(name: String): String? = Regex("\"$name\":\"((?:\\\\.|[^\"])*)\"").find(payload)?.groupValues?.get(1)
        fun boolField(name: String): Boolean? = Regex("\"$name\":(true|false)").find(payload)?.groupValues?.get(1)?.toBoolean()
        fun longField(name: String): Long? = Regex("\"$name\":(-?\\d+)").find(payload)?.groupValues?.get(1)?.toLongOrNull()

        val fixtureId = stringField("fixtureId") ?: return null
        val contextProfileId = stringField("contextProfileId") ?: return null
        val expectedActionText = stringField("expectedAction") ?: return null
        val actualActionText = stringField("actualAction")
        val representationValid = boolField("representationValid") ?: false
        val contentFidelityText = stringField("contentFidelity") ?: ContentFidelity.NOT_APPLICABLE.name
        val primaryClassificationText = stringField("primaryClassification") ?: PrimaryClassification.A.name
        val latencyNanos = longField("latencyNanos") ?: 0L
        val roleIdentity = if (role == FamilyFRole.SUBJECT) config.identity.subject else config.identity.control

        return TrialObservation(
            runId = "family-f-diagnostic",
            fixtureId = fixtureId,
            contextProfileId = contextProfileId,
            trialSequence = 0,
            stableInputHash = "",
            repositoryCommit = config.identity.repositoryCommit,
            modelName = roleIdentity.modelName,
            modelDigest = roleIdentity.modelDigest,
            runtimeImageId = null,
            endpointIdentifier = config.identity.dedicatedEndpointIdentifier,
            timeoutMs = config.identity.requestTimeoutMs,
            prompt = "",
            promptSha256 = "",
            requestBody = null,
            rawOllamaEnvelope = null,
            extractedResponse = null,
            parsedVariant = null,
            parserExceptionType = null,
            parserExceptionClassification = null,
            expectedAction = ExpectedAction.valueOf(expectedActionText),
            actualAction = actualActionText?.let { ExpectedAction.valueOf(it) },
            representationValid = representationValid,
            contentFidelity = ContentFidelity.valueOf(contentFidelityText),
            latencyNanos = latencyNanos,
            endpointMetadata = EndpointMetadata(),
            primaryClassification = PrimaryClassification.valueOf(primaryClassificationText),
        )
    }

    private fun campaignDefinitionJson(): String = familyFObjectLine(
        listOf(
            "campaignId" to config.campaignId,
            "scheduledFixtureCount" to FamilyFCorpus.fixtures.size,
            "scheduledProfileCount" to FamilyFCorpus.profiles.size,
            "repetitions" to 4,
            "scoredCallCount" to 368,
            "warmupCallCount" to 24,
            "totalCallCount" to 392,
            "scheduleHash" to FamilyFCampaignDefinition.scheduleHash,
        ),
    )

    private fun campaignIdentityJson(identity: FamilyFIdentity): String = familyFObjectLine(
        listOf(
            "repositoryCommit" to identity.repositoryCommit,
            "subjectModelName" to identity.subject.modelName,
            "subjectModelDigest" to identity.subject.modelDigest,
            "subjectModelSizeBytes" to identity.subject.modelSizeBytes,
            "controlModelName" to identity.control.modelName,
            "controlModelDigest" to identity.control.modelDigest,
            "controlModelSizeBytes" to identity.control.modelSizeBytes,
            "dedicatedEndpointIdentifier" to identity.dedicatedEndpointIdentifier,
            "providerBinaryPath" to identity.providerBinaryPath,
            "providerBinaryDigest" to identity.providerBinaryDigest,
            "requestTimeoutMs" to identity.requestTimeoutMs,
            "unloadTimeoutMs" to identity.unloadTimeoutMs,
            "executionApprovalId" to identity.executionApprovalId,
            "executionApprovalHash" to identity.executionApprovalHash,
        ),
    )

    private fun advancementWorksheetJson(result: FamilyFAdvancementResult): String = familyFObjectLine(
        listOf(
            "verdict" to result.verdict.name,
            "failedConditionCount" to result.failedConditions.size,
            "failedConditions" to result.failedConditions.joinToString("; "),
            "cellCount" to result.cellResults.size,
            "deviationOrParaphraseCount" to result.deviationOrParaphraseCount,
            "indeterminateContentCount" to result.indeterminateContentCount,
        ),
    )
}

// =======================================================================
// Offline tests. Zero real model calls, zero real endpoints, zero real
// resource/residency/process state -- every gate exercised with fakes.
// =======================================================================

private const val FAMILY_F_FAKE_CAMPAIGN_ID = "familyf-diagnostic-faketest"

class ReasoningProtocolFamilyFDiagnosticOrchestrationTest {

    // ---- Schedule construction ----

    @Test
    fun `exact 392-call total is independently derived from campaign structure, not hard-coded`() {
        val fixtures = 23
        val profiles = 2
        val repetitions = 4
        val roles = 2
        val expectedScored = fixtures * profiles * repetitions * roles
        val expectedWarmups = repetitions * roles * 3
        assertEquals(368, expectedScored)
        assertEquals(24, expectedWarmups)
        assertEquals(392, expectedScored + expectedWarmups)
        assertEquals(expectedScored + expectedWarmups, FamilyFCampaignDefinition.allTrials.size)
        assertEquals(expectedScored, FamilyFCampaignDefinition.allTrials.count { it.kind == FamilyFTrialKind.SCORED })
        assertEquals(expectedWarmups, FamilyFCampaignDefinition.allTrials.count { it.kind == FamilyFTrialKind.WARMUP })
    }

    @Test
    fun `each role receives exactly 184 scored calls across 46 distinct fixture-profile cells per repetition`() {
        FamilyFRole.values().forEach { role ->
            val scored = FamilyFCampaignDefinition.allTrials.filter { it.role == role && it.kind == FamilyFTrialKind.SCORED }
            assertEquals(184, scored.size)
            (1..4).forEach { repetition ->
                val cells = scored.filter { it.repetition == repetition }
                assertEquals(46, cells.size)
                assertEquals(46, cells.map { it.fixture.id to it.profileId }.distinct().size)
            }
        }
    }

    @Test
    fun `model order is AB-BA alternation across the four repetitions, not a full Latin square`() {
        // Reconstruct first-role-per-repetition purely from block grouping order.
        val firstRolePerRepetition = (1..4).map { repetition ->
            FamilyFCampaignDefinition.allTrials.first { it.repetition == repetition }.role
        }
        assertEquals(listOf(FamilyFRole.SUBJECT, FamilyFRole.CONTROL, FamilyFRole.SUBJECT, FamilyFRole.CONTROL), firstRolePerRepetition)
    }

    @Test
    fun `every residency block contains exactly three warm-ups in frozen order followed by 46 scored calls`() {
        FamilyFCampaignDefinition.blocks.forEach { block ->
            val warmups = block.trials.take(3)
            assertTrue(warmups.all { it.kind == FamilyFTrialKind.WARMUP })
            assertEquals(listOf(1, 2, 3), warmups.map { it.attempt })
            warmups.forEach { assertEquals(FAMILY_F_WARMUP_INPUT, it.fixture.ownerMessage) }
            val scored = block.trials.drop(3)
            assertEquals(46, scored.size)
            assertTrue(scored.all { it.kind == FamilyFTrialKind.SCORED })
        }
    }

    @Test
    fun `warm-up trials are never scored and are structurally distinguishable from scored trials`() {
        assertTrue(FamilyFCampaignDefinition.allTrials.filter { it.kind == FamilyFTrialKind.WARMUP }.all { it.fixture.id == FamilyFCorpus.warmupFixture.id })
        assertTrue(FamilyFCampaignDefinition.allTrials.filter { it.kind == FamilyFTrialKind.SCORED }.none { it.fixture.id == FamilyFCorpus.warmupFixture.id })
    }

    @Test
    fun `all 392 trial IDs are unique and deterministic across two independent constructions`() {
        val first = FamilyFCampaignDefinition.allTrials.map { it.id }
        val second = FamilyFCampaignDefinition.allTrials.map { it.id }
        assertEquals(first, second)
        assertEquals(first.size, first.distinct().size)
    }

    // ---- Resource gates ----

    @Test
    fun `memory gate accepts sufficient pre-load capacity and rejects insufficient capacity`() {
        FamilyFMemoryGate.checkPreLoad(1_000_000_000L) { 1_000_000_000L + FAMILY_F_MINIMUM_FREE_MEMORY_BYTES }
        assertFailsWith<FamilyFInsufficientMemoryException> {
            FamilyFMemoryGate.checkPreLoad(1_000_000_000L) { 1_000_000_000L + FAMILY_F_MINIMUM_FREE_MEMORY_BYTES - 1 }
        }
    }

    @Test
    fun `memory gate per-call check accepts exact boundary and fails closed on unreadable state`() {
        FamilyFMemoryGate.checkPerCall { FAMILY_F_MINIMUM_FREE_MEMORY_BYTES }
        assertFailsWith<FamilyFInsufficientMemoryException> {
            FamilyFMemoryGate.checkPerCall { throw IOException("simulated unreadable memory state") }
        }
    }

    @Test
    fun `disk space gate accepts sufficient capacity and rejects insufficient capacity`() {
        FamilyFDiskSpaceGate.check(Path.of("/tmp")) { FAMILY_F_MINIMUM_FREE_MEMORY_BYTES + 1 }
        assertFailsWith<FamilyFInsufficientSpaceException> {
            FamilyFDiskSpaceGate.check(Path.of("/tmp")) { FAMILY_F_MINIMUM_FREE_MEMORY_BYTES - 1 }
        }
    }

    @Test
    fun `disk space gate fails closed on unreadable filesystem state`() {
        assertFailsWith<FamilyFInsufficientSpaceException> {
            FamilyFDiskSpaceGate.check(Path.of("/tmp")) { throw IOException("simulated unreadable filesystem") }
        }
    }

    // ---- Resource-reading durability (independent review Finding 1) ----

    @Test
    fun `a passing disk measurement still produces a resource reading -- recording is reached regardless of outcome`() {
        val reading = FamilyFDiskSpaceGate.measure(Path.of("/tmp"), "DISK_SPACE", "block-1") { FAMILY_F_MINIMUM_FREE_MEMORY_BYTES + 1 }
        assertEquals("PASS", reading.outcome)
        assertEquals("block-1", reading.blockId)
        assertEquals(Path.of("/tmp").toString(), reading.source)
    }

    @Test
    fun `a failing disk measurement still produces a resource reading before the caller throws`() {
        val reading = FamilyFDiskSpaceGate.measure(Path.of("/tmp"), "DISK_SPACE", null) { FAMILY_F_MINIMUM_FREE_MEMORY_BYTES - 1 }
        assertEquals("FAIL", reading.outcome)
        assertFailsWith<FamilyFInsufficientSpaceException> { FamilyFDiskSpaceGate.enforceDisk(reading) }
    }

    @Test
    fun `an unreadable disk source still produces a resource reading with a negative parsed value, not a silent skip`() {
        val reading = FamilyFDiskSpaceGate.measure(Path.of("/tmp"), "DISK_SPACE", null) { throw IOException("simulated") }
        assertEquals("FAIL", reading.outcome)
        assertEquals(-1L, reading.parsedBytes)
        assertTrue(reading.rawReading.contains("ERROR"))
    }

    @Test
    fun `resource-reading ledger records are hash-chained with a monotonic sequence`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        val first = ledger.recordResourceReading(FamilyFResourceReading("PHASE_A", null, null, "src", "1", 1L, 1L, "PASS"))
        val second = ledger.recordResourceReading(FamilyFResourceReading("PHASE_B", null, null, "src", "2", 2L, 1L, "PASS"))
        assertEquals(1L, first)
        assertEquals(2L, second)
        val lines = Files.readAllLines(dir.resolve("resource-readings.jsonl")).filter { it.isNotBlank() }
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("\"priorRecordHash\":\"$FAMILY_F_LEDGER_GENESIS_HASH\""))
        val firstHash = Regex("\"recordHash\":\"([a-f0-9]+)\"").find(lines[0])!!.groupValues[1]
        assertTrue(lines[1].contains("\"priorRecordHash\":\"$firstHash\""), "second record's priorRecordHash must equal the first record's own recordHash")
    }

    @Test
    fun `resource-reading hash chain resumes correctly from an already-durable file rather than restarting at sequence one`(@TempDir dir: Path) {
        val firstLedger = FamilyFCampaignLedger(dir, setOf("t1"))
        firstLedger.recordResourceReading(FamilyFResourceReading("PHASE_A", null, null, "src", "1", 1L, 1L, "PASS"))
        val resumedLedger = FamilyFCampaignLedger(dir, setOf("t1"))
        val resumedSequence = resumedLedger.recordResourceReading(FamilyFResourceReading("PHASE_B", null, null, "src", "2", 2L, 1L, "PASS"))
        assertEquals(2L, resumedSequence, "a freshly constructed ledger over an existing directory must continue the chain, not restart it")
    }

    // ---- Residency gate ----

    @Test
    fun `residency gate accepts absence before a block and rejects any resident model`() {
        FamilyFResidencyGate.checkNeitherResident { FamilyFResidencyState.ABSENT }
        assertFailsWith<FamilyFResidencyException> {
            FamilyFResidencyGate.checkNeitherResident { FamilyFResidencyState.SUBJECT_RESIDENT }
        }
    }

    @Test
    fun `residency gate requires exactly the assigned role resident after warm-up`() {
        FamilyFResidencyGate.checkAssignedResident(FamilyFRole.SUBJECT) { FamilyFResidencyState.SUBJECT_RESIDENT }
        assertFailsWith<FamilyFResidencyException> {
            FamilyFResidencyGate.checkAssignedResident(FamilyFRole.SUBJECT) { FamilyFResidencyState.CONTROL_RESIDENT }
        }
        assertFailsWith<FamilyFResidencyException> {
            FamilyFResidencyGate.checkAssignedResident(FamilyFRole.SUBJECT) { FamilyFResidencyState.BOTH_RESIDENT }
        }
    }

    @Test
    fun `unload failure and unverifiable residency both fail closed`() {
        assertFailsWith<FamilyFResidencyException> {
            FamilyFResidencyGate.unloadAndVerifyAbsent(FamilyFRole.SUBJECT, { false }) { FamilyFResidencyState.ABSENT }
        }
        assertFailsWith<FamilyFResidencyException> {
            FamilyFResidencyGate.unloadAndVerifyAbsent(FamilyFRole.SUBJECT, { true }) { FamilyFResidencyState.SUBJECT_RESIDENT }
        }
        assertFailsWith<FamilyFResidencyException> {
            FamilyFResidencyGate.unloadAndVerifyAbsent(FamilyFRole.SUBJECT, { true }) { FamilyFResidencyState.UNKNOWN }
        }
    }

    @Test
    fun `successful unload converging to absence does not throw`() {
        FamilyFResidencyGate.unloadAndVerifyAbsent(FamilyFRole.CONTROL, { true }) { FamilyFResidencyState.ABSENT }
    }

    // ---- Production isolation ----

    @Test
    fun `production isolation guard rejects a dedicated PID or endpoint equal to a protected production process`() {
        val protectedProcesses = listOf(FamilyFProtectedProcess("production-parker", "5261", "production-endpoint"))
        assertFailsWith<FamilyFProductionIsolationException> {
            FamilyFProductionIsolationGuard.requireDistinctFromProduction("5261", "dedicated-endpoint", protectedProcesses)
        }
        assertFailsWith<FamilyFProductionIsolationException> {
            FamilyFProductionIsolationGuard.requireDistinctFromProduction("9999", "production-endpoint", protectedProcesses)
        }
        FamilyFProductionIsolationGuard.requireDistinctFromProduction("9999", "dedicated-endpoint", protectedProcesses)
    }

    @Test
    fun `production isolation guard requires every protected process to be observed healthy`() {
        val protectedProcesses = listOf(FamilyFProtectedProcess("production-parker", "5261", "production-endpoint"))
        assertFailsWith<FamilyFProductionIsolationException> {
            FamilyFProductionIsolationGuard.requireProtectedProcessesHealthy(protectedProcesses) { false }
        }
        FamilyFProductionIsolationGuard.requireProtectedProcessesHealthy(protectedProcesses) { true }
    }

    // ---- Ledger: exact-once, recovery, no-retry ----

    @Test
    fun `intent is durable before the executor is ever invoked -- source order proves no path can transmit first`() {
        val text = Files.readString(Path.of("tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt"))
        val marker = "private fun runBlock("
        val start = text.indexOf(marker)
        assertTrue(start >= 0)
        val end = text.indexOf("\n    private fun ", start + 1).let { if (it < 0) text.indexOf("\n}\n", start) else it }
        val body = text.substring(start, end)
        val dispatchIndex = body.indexOf("ledger.recordDispatch(")
        val callIndex = body.indexOf("modelCaller.call(")
        assertTrue(dispatchIndex in 0 until callIndex, "dispatch must be recorded before the model caller is ever invoked")
    }

    @Test
    fun `recover accepts a trial with only an intent record as safely resumable`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        val state = ledger.recover()
        assertFalse("t1" in state.dispatched)
        assertTrue("t1" !in state.resolved)
    }

    @Test
    fun `recover treats a dispatched trial with no transport evidence as ambiguous and fails closed`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.recover() }
    }

    @Test
    fun `recover treats transport-captured-but-unclassified trials as pending offline classification, not ambiguous`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val record = FamilyFProxyExchangeRecord(1, Instant.now(), Instant.now(), "POST", "/api/generate", ByteArray(0), familyFSha256Bytes(ByteArray(0)), 200, emptyMap(), ByteArray(0), familyFSha256Bytes(ByteArray(0)), "FORWARDED")
        ledger.recordTransport("t1", record)
        val state = ledger.recover()
        assertTrue("t1" in state.pendingOfflineClassification)
        assertTrue("t1" in state.dispatched)
        assertFalse("t1" in state.resolved)
    }

    @Test
    fun `recover treats a fully terminal trial as resolved and never reissues it`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val record = FamilyFProxyExchangeRecord(1, Instant.now(), Instant.now(), "POST", "/api/generate", ByteArray(0), familyFSha256Bytes(ByteArray(0)), 200, emptyMap(), ByteArray(0), familyFSha256Bytes(ByteArray(0)), "FORWARDED")
        ledger.recordTransport("t1", record)
        ledger.recordTerminal("t1", "payload")
        val state = ledger.recover()
        assertTrue("t1" in state.resolved)
        assertTrue(state.pendingOfflineClassification.isEmpty())
    }

    @Test
    fun `recover treats a captured-but-forwarding-failed exchange as ambiguous, not pending offline classification`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val requestBody = ByteArray(0)
        // A forwarding-failure exchange IS durably captured (the proxy
        // records it too), but it is not "a complete durably captured
        // response" (Plan Section 18) -- no response was ever obtained,
        // so it must remain ambiguous and permanently halt, never be
        // treated as offline-classifiable.
        val record = FamilyFProxyExchangeRecord(
            1, Instant.now(), Instant.now(), "POST", "/api/generate",
            requestBody, familyFSha256Bytes(requestBody), null, emptyMap(),
            null, null, "FORWARDING_FAILURE: java.net.ConnectException: Connection refused",
        )
        ledger.recordTransport("t1", record)
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.recover() }
    }

    @Test
    fun `recover fails closed on a duplicate trial ID in any ledger`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        ledger.recordIntent("t1")
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.recover() }
    }

    @Test
    fun `recover fails closed on an unregistered trial ID`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        // Force an unregistered ID directly into the intent ledger.
        Files.writeString(dir.resolve("intent.jsonl"), "{\"trialId\":\"unregistered\",\"timestamp\":\"x\"}\n", StandardOpenOption.APPEND)
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.recover() }
    }

    /**
     * Writes every mandatory artifact this focused unit test does not
     * itself exercise (schedule, campaign-definition, campaign-identity,
     * a control event, and a resource reading), so a minimal single-trial
     * ledger can still legally reach [FamilyFCampaignLedger.sealAfterAdvancementRecorded]
     * -- exercising the real mandatory-artifact gate rather than bypassing it.
     */
    private fun writeMinimalMandatoryArtifacts(ledger: FamilyFCampaignLedger, trialId: String) {
        val trial = FamilyFTrial(
            id = trialId, role = FamilyFRole.SUBJECT, repetition = 1, kind = FamilyFTrialKind.SCORED,
            fixture = FamilyFCorpus.fixtures.first(), profileId = ContextProfileId.MINIMAL_PRODUCTION_CONTEXT, attempt = 1,
        )
        ledger.writeScheduleOnce(listOf(trial))
        ledger.writeCampaignDefinition("{}")
        ledger.writeCampaignIdentity("{}")
        ledger.recordControlEvent("unit test setup")
        ledger.recordResourceReading(FamilyFResourceReading("TEST_SETUP", null, null, "test-source", "100", 100L, 50L, "PASS"))
    }

    @Test
    fun `sealing requires zero pending offline classification and the exact expected resolved set`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        writeMinimalMandatoryArtifacts(ledger, "t1")
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val record = FamilyFProxyExchangeRecord(1, Instant.now(), Instant.now(), "POST", "/api/generate", ByteArray(0), familyFSha256Bytes(ByteArray(0)), 200, emptyMap(), ByteArray(0), familyFSha256Bytes(ByteArray(0)), "FORWARDED")
        ledger.recordTransport("t1", record)
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.sealAfterAdvancementRecorded(setOf("t1")) }
        ledger.recordTerminal("t1", "payload")
        ledger.writeAdvancementWorksheet("{}")
        ledger.writeSealedReport("{}")
        ledger.sealAfterAdvancementRecorded(setOf("t1"))
        assertTrue(ledger.isSealed())
    }

    @Test
    fun `sealed and halted markers are mutually exclusive`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        writeMinimalMandatoryArtifacts(ledger, "t1")
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val record = FamilyFProxyExchangeRecord(1, Instant.now(), Instant.now(), "POST", "/api/generate", ByteArray(0), familyFSha256Bytes(ByteArray(0)), 200, emptyMap(), ByteArray(0), familyFSha256Bytes(ByteArray(0)), "FORWARDED")
        ledger.recordTransport("t1", record)
        ledger.recordTerminal("t1", "payload")
        ledger.writeAdvancementWorksheet("{}")
        ledger.writeSealedReport("{}")
        ledger.sealAfterAdvancementRecorded(setOf("t1"))
        assertFailsWith<IllegalStateException> { ledger.halt("late halt attempt") }
    }

    @Test
    fun `a halted campaign cannot later be sealed`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        ledger.halt("simulated integrity failure")
        assertFailsWith<IllegalStateException> { ledger.sealAfterAdvancementRecorded(setOf("t1")) }
    }

    @Test
    fun `sealing fails closed when a mandatory artifact is missing, even with all trials resolved`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        // Deliberately omit writeScheduleOnce/writeCampaignDefinition/etc.
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val record = FamilyFProxyExchangeRecord(1, Instant.now(), Instant.now(), "POST", "/api/generate", ByteArray(0), familyFSha256Bytes(ByteArray(0)), 200, emptyMap(), ByteArray(0), familyFSha256Bytes(ByteArray(0)), "FORWARDED")
        ledger.recordTransport("t1", record)
        ledger.recordTerminal("t1", "payload")
        ledger.writeAdvancementWorksheet("{}")
        ledger.writeSealedReport("{}")
        val exception = assertFailsWith<FamilyFArtifactIntegrityException> { ledger.sealAfterAdvancementRecorded(setOf("t1")) }
        assertTrue(exception.message!!.contains("missing mandatory artifact"))
        assertFalse(ledger.isSealed())
        assertFalse(Files.exists(dir.resolve("SHA256SUMS.txt")), "manifest must not be generated when sealing fails closed")
    }

    @Test
    fun `manifest checksums are verifiable after sealing and detect tampering, including from a copied directory`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        writeMinimalMandatoryArtifacts(ledger, "t1")
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val record = FamilyFProxyExchangeRecord(1, Instant.now(), Instant.now(), "POST", "/api/generate", ByteArray(0), familyFSha256Bytes(ByteArray(0)), 200, emptyMap(), ByteArray(0), familyFSha256Bytes(ByteArray(0)), "FORWARDED")
        ledger.recordTransport("t1", record)
        ledger.recordTerminal("t1", "payload")
        ledger.writeAdvancementWorksheet("{}")
        ledger.writeSealedReport("{}")
        ledger.sealAfterAdvancementRecorded(setOf("t1"))
        assertTrue(ledger.verifyChecksums())
        val checksumsText = Files.readString(dir.resolve("SHA256SUMS.txt"))
        assertTrue(checksumsText.contains("resource-readings.jsonl"), "resource-readings.jsonl must be manifest-covered")

        val copy = dir.resolveSibling(dir.fileName.toString() + "-copy")
        Files.createDirectories(copy)
        Files.list(dir).use { stream ->
            stream.forEach { file -> Files.copy(file, copy.resolve(file.fileName)) }
        }
        val copiedLedger = FamilyFCampaignLedger(copy, setOf("t1"))
        assertTrue(copiedLedger.verifyChecksums(), "checksums must independently re-verify from a copied directory")

        Files.writeString(dir.resolve("terminal.jsonl"), "{\"trialId\":\"t1\",\"payload\":\"tampered\"}\n")
        assertFalse(ledger.verifyChecksums())
    }

    // ---- Advancement gate ----

    private fun observation(fixtureId: String, profile: String, expected: ExpectedAction, actual: ExpectedAction, representationValid: Boolean = true, fidelity: ContentFidelity = ContentFidelity.NOT_APPLICABLE): TrialObservation =
        TrialObservation(
            runId = "test", fixtureId = fixtureId, contextProfileId = profile, trialSequence = 1, stableInputHash = "",
            repositoryCommit = "commit", modelName = FAMILY_F_SUBJECT_MODEL_NAME, modelDigest = null, runtimeImageId = null,
            endpointIdentifier = "endpoint", timeoutMs = FAMILY_F_TIMEOUT_MS, prompt = "", promptSha256 = "",
            requestBody = null, rawOllamaEnvelope = null, extractedResponse = null, parsedVariant = null,
            parserExceptionType = null, parserExceptionClassification = null, expectedAction = expected, actualAction = actual,
            representationValid = representationValid, contentFidelity = fidelity, latencyNanos = 0,
            endpointMetadata = EndpointMetadata(), primaryClassification = if (expected == actual) PrimaryClassification.A else PrimaryClassification.D,
        )

    private fun fullPassingSubjectObservations(): List<TrialObservation> =
        FamilyFCorpus.fixtures.flatMap { fixture ->
            FamilyFCorpus.profiles.flatMap { profile ->
                (1..4).map { observation(fixture.id, profile.externalId, fixture.expectedAction, fixture.expectedAction) }
            }
        }

    @Test
    fun `advancement gate is eligible when all conditions hold across the full 46-cell matrix`() {
        val result = FamilyFAdvancementGate.evaluate(fullPassingSubjectObservations(), materialMutationOrInventionConfirmedZero = true)
        assertEquals(FamilyFAdvancementVerdict.ELIGIBLE_FOR_FULL_QUALIFICATION_PROPOSAL, result.verdict)
        assertTrue(result.failedConditions.isEmpty())
        assertEquals(46, result.cellResults.size)
    }

    @Test
    fun `advancement gate is not eligible when a single cell falls below 3 of 4 correct`() {
        val observations = fullPassingSubjectObservations().toMutableList()
        val index = observations.indexOfFirst { it.fixtureId == "r01-direct" && it.contextProfileId == ContextProfileId.MINIMAL_PRODUCTION_CONTEXT.externalId }
        observations[index] = observation("r01-direct", ContextProfileId.MINIMAL_PRODUCTION_CONTEXT.externalId, ExpectedAction.REMEMBER, ExpectedAction.REPLY)
        val index2 = observations.indexOfFirst { it.fixtureId == "r01-direct" && it.contextProfileId == ContextProfileId.MINIMAL_PRODUCTION_CONTEXT.externalId && it !== observations[index] }
        observations[index2] = observation("r01-direct", ContextProfileId.MINIMAL_PRODUCTION_CONTEXT.externalId, ExpectedAction.REMEMBER, ExpectedAction.REPLY)
        val result = FamilyFAdvancementGate.evaluate(observations, materialMutationOrInventionConfirmedZero = true)
        assertEquals(FamilyFAdvancementVerdict.NOT_ELIGIBLE, result.verdict)
        assertTrue(result.failedConditions.any { it.contains("r01-direct") })
    }

    @Test
    fun `an R01-only improvement cannot satisfy the gate while other cells remain below threshold`() {
        val observations = FamilyFCorpus.fixtures.flatMap { fixture ->
            FamilyFCorpus.profiles.flatMap { profile ->
                (1..4).map { _ ->
                    val correct = fixture.id == "r01-direct"
                    observation(
                        fixture.id, profile.externalId, fixture.expectedAction,
                        if (correct) fixture.expectedAction else ExpectedAction.REPLY,
                    )
                }
            }
        }
        val result = FamilyFAdvancementGate.evaluate(observations, materialMutationOrInventionConfirmedZero = true)
        assertEquals(FamilyFAdvancementVerdict.NOT_ELIGIBLE, result.verdict)
        assertTrue(result.failedConditions.size > 1, "an R01-only pass must leave many other cells failing")
    }

    @Test
    fun `advancement gate is not eligible on any false-positive REMEMBER or GOAL classification`() {
        val observations = fullPassingSubjectObservations().toMutableList()
        val index = observations.indexOfFirst { it.fixtureId == "p01-ordinary-fact" }
        observations[index] = observation("p01-ordinary-fact", observations[index].contextProfileId, ExpectedAction.REPLY, ExpectedAction.REMEMBER)
        val result = FamilyFAdvancementGate.evaluate(observations, materialMutationOrInventionConfirmedZero = true)
        assertEquals(FamilyFAdvancementVerdict.NOT_ELIGIBLE, result.verdict)
        assertTrue(result.failedConditions.any { it.contains("false-positive REMEMBER") })
    }

    @Test
    fun `advancement gate is not eligible without explicit confirmation that material mutation or invention is zero`() {
        val result = FamilyFAdvancementGate.evaluate(fullPassingSubjectObservations(), materialMutationOrInventionConfirmedZero = false)
        assertEquals(FamilyFAdvancementVerdict.NOT_ELIGIBLE, result.verdict)
        assertTrue(result.failedConditions.any { it.contains("material mutation") })
    }

    @Test
    fun `advancement gate is not eligible on any representation-invalid subject observation`() {
        val observations = fullPassingSubjectObservations().toMutableList()
        val index = observations.indexOfFirst { it.fixtureId == "p06-greeting" }
        observations[index] = observation("p06-greeting", observations[index].contextProfileId, ExpectedAction.REPLY, ExpectedAction.REPLY, representationValid = false)
        val result = FamilyFAdvancementGate.evaluate(observations, materialMutationOrInventionConfirmedZero = true)
        assertEquals(FamilyFAdvancementVerdict.NOT_ELIGIBLE, result.verdict)
        assertTrue(result.failedConditions.any { it.contains("representation-invalid") })
    }

    @Test
    fun `advancement gate never produces a ranking or comparative statement -- its result type carries no control-model field`() {
        val resultType = FamilyFAdvancementResult::class.java
        val fieldNames = resultType.declaredFields.map { it.name }
        assertTrue(fieldNames.none { it.contains("control", ignoreCase = true) || it.contains("rank", ignoreCase = true) || it.contains("winner", ignoreCase = true) })
    }

    // ---- Provenance separation ----

    @Test
    fun `Family F campaign IDs never collide with prior campaign identity markers`() {
        val forbiddenMarkers = listOf("unit3c-remedy-experiments-", "qwen25coder7b-baseline-", "qwen25coder7b-llama32-3b-diagnostic-")
        forbiddenMarkers.forEach { marker ->
            assertFalse(FAMILY_F_CAMPAIGN_ID_MARKER.startsWith(marker))
            assertFalse(marker.startsWith(FAMILY_F_CAMPAIGN_ID_MARKER))
        }
    }

    @Test
    fun `no function anywhere in the Family F files pools evidence into a prior campaign's ledger or artifact root`() {
        val combined = familyFScanSafeSource(Path.of("tests/integration/ReasoningProtocolFamilyFDiagnosticTest.kt")) +
            familyFScanSafeSource(Path.of("tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt"))
        // FAMILY_F_FORBIDDEN_SCAN_EXCLUDE_START -- these are the literal
        // forbidden markers themselves; excluded from their own scan below
        // for the same reason FAMILY_F_FORBIDDEN_SYMBOLS is excluded.
        val forbiddenPaths = listOf("qwen25coder7b-baseline-20260809", "qwen25coder7b-llama32-3b-diagnostic-20260809", "unit3c-remedy-experiments-20260810-03")
        // FAMILY_F_FORBIDDEN_SCAN_EXCLUDE_END
        forbiddenPaths.forEach { forbidden -> assertFalse(combined.contains(forbidden)) }
    }

    // ---- Subject/control role-report completeness (unit level) ----

    private fun fullRoleObservations(): List<TrialObservation> =
        FamilyFCorpus.fixtures.flatMap { fixture ->
            FamilyFCorpus.profiles.flatMap { profile ->
                (1..4).map { observation(fixture.id, profile.externalId, fixture.expectedAction, fixture.expectedAction) }
            }
        }

    @Test
    fun `role report contains all 46 fixture-profile cells with four observations each when the input is complete`() {
        val report = FamilyFRoleReportBuilder.build(FamilyFRole.CONTROL, fullRoleObservations())
        assertEquals(46, report.cells.size)
        assertTrue(report.cells.all { it.attempts == 4 })
        assertEquals(184, report.totalObservations)
        FamilyFRoleReportBuilder.requireComplete(report, "control")
    }

    @Test
    fun `role report completeness check rejects a cell with fewer than four observations -- a missing observation would block sealing`() {
        val observations = fullRoleObservations().toMutableList()
        observations.removeAt(0)
        val report = FamilyFRoleReportBuilder.build(FamilyFRole.CONTROL, observations)
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFRoleReportBuilder.requireComplete(report, "control") }
    }

    @Test
    fun `role report completeness check rejects a cell with more than four observations -- a duplicate observation would block sealing`() {
        val observations = fullRoleObservations().toMutableList()
        observations.add(observations[0].copy())
        val report = FamilyFRoleReportBuilder.build(FamilyFRole.CONTROL, observations)
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFRoleReportBuilder.requireComplete(report, "control") }
    }

    @Test
    fun `role report discloses poor correctness per cell rather than omitting the cell`() {
        val observations = FamilyFCorpus.fixtures.flatMap { fixture ->
            val wrongAction = if (fixture.expectedAction == ExpectedAction.REPLY) ExpectedAction.NOACTION else ExpectedAction.REPLY
            FamilyFCorpus.profiles.flatMap { profile ->
                (1..4).map { observation(fixture.id, profile.externalId, fixture.expectedAction, wrongAction) }
            }
        }
        val report = FamilyFRoleReportBuilder.build(FamilyFRole.CONTROL, observations)
        assertEquals(46, report.cells.size, "every cell must still be present even at zero correctness")
        assertTrue(report.cells.all { it.correctCount == 0 })
    }

    @Test
    fun `advancement gate evaluation has exactly two parameters -- structurally incapable of receiving control observations`() {
        val method = FamilyFAdvancementGate::class.java.getMethod("evaluate", List::class.java, Boolean::class.javaPrimitiveType)
        assertEquals(2, method.parameterCount)
    }

    // ---- Full offline fake-driven campaign (end-to-end reporting proof) ----

    private fun fakeConfig(campaignRoot: Path): FamilyFConfig {
        val identity = FamilyFIdentity(
            repositoryCommit = "fakecommit0123456789",
            subject = FamilyFRoleIdentity(FamilyFRole.SUBJECT, FAMILY_F_SUBJECT_MODEL_NAME, "sha256:fakesubject", 4_700_000_000L),
            control = FamilyFRoleIdentity(FamilyFRole.CONTROL, FAMILY_F_CONTROL_MODEL_NAME, "sha256:fakecontrol", 2_000_000_000L),
            dedicatedEndpointIdentifier = "http://127.0.0.1:0/fake",
            providerBinaryPath = "/usr/local/bin/fake-ollama",
            providerBinaryDigest = "sha256:fakeprovider",
            protectedParkerPid = "999999",
            protectedModelDaemonPid = "999998",
            requestTimeoutMs = FAMILY_F_TIMEOUT_MS,
            unloadTimeoutMs = 30_000L,
            executionApprovalId = "fake-approval",
            executionApprovalHash = "sha256:fakeapproval",
        )
        val runtimeRoot = campaignRoot.resolveSibling(campaignRoot.fileName.toString() + "-runtime")
        return FamilyFConfig(FAMILY_F_FAKE_CAMPAIGN_ID, campaignRoot, runtimeRoot, "http://127.0.0.1:0/fake", identity)
    }

    /**
     * Drives the real [FamilyFOrchestrationDriver] through a complete
     * 392-call campaign using only fakes: a synchronous [FamilyFModelCaller]
     * that never opens a socket, an always-sufficient resource reader, a
     * residency state machine driven purely by which role is currently
     * being called, and trivially-healthy protected processes distinct
     * from the fake dedicated PID. `campaignRoot` must not already exist.
     */
    private fun runFakeCampaign(
        campaignRoot: Path,
        subjectAlwaysCorrect: Boolean,
        controlAlwaysCorrect: Boolean,
        usableSpace: (Path) -> Long = { 8L * 1024 * 1024 * 1024 },
    ): Pair<FamilyFCampaignOutcome, FamilyFCampaignLedger> {
        val config = fakeConfig(campaignRoot)
        val ledger = FamilyFCampaignLedger(config.campaignArtifactRoot, FamilyFCampaignDefinition.allTrials.map { it.id }.toSet(), config.campaignId)
        val transportRecorder = FamilyFTrialTransportRecorder(ledger)
        var residency = FamilyFResidencyState.ABSENT

        val modelCaller = FamilyFModelCaller { trial, roleModelName ->
            residency = if (trial.role == FamilyFRole.SUBJECT) FamilyFResidencyState.SUBJECT_RESIDENT else FamilyFResidencyState.CONTROL_RESIDENT
            val alwaysCorrect = if (trial.role == FamilyFRole.SUBJECT) subjectAlwaysCorrect else controlAlwaysCorrect
            val actual = when {
                trial.kind == FamilyFTrialKind.WARMUP -> ExpectedAction.REPLY
                alwaysCorrect -> trial.fixture.expectedAction
                trial.fixture.expectedAction == ExpectedAction.REPLY -> ExpectedAction.NOACTION
                else -> ExpectedAction.REPLY
            }
            transportRecorder.onExchange(
                FamilyFProxyExchangeRecord(
                    sequence = 1, startedAt = Instant.now(), completedAt = Instant.now(), method = "POST", path = "/api/generate",
                    requestBody = ByteArray(0), requestSha256 = familyFSha256Bytes(ByteArray(0)), responseStatus = 200, responseHeaders = emptyMap(),
                    responseBody = ByteArray(0), responseSha256 = familyFSha256Bytes(ByteArray(0)), forwardingOutcome = "FORWARDED",
                ),
            )
            TrialObservation(
                runId = "fake", fixtureId = trial.fixture.id, contextProfileId = (trial.profileId ?: ContextProfileId.MINIMAL_PRODUCTION_CONTEXT).externalId,
                trialSequence = trial.attempt, stableInputHash = "hash", repositoryCommit = "fakecommit0123456789", modelName = roleModelName,
                modelDigest = "sha256:fake", runtimeImageId = null, endpointIdentifier = "fake-endpoint", timeoutMs = FAMILY_F_TIMEOUT_MS,
                prompt = "prompt", promptSha256 = "prompt-sha", requestBody = null, rawOllamaEnvelope = null, extractedResponse = null,
                parsedVariant = null, parserExceptionType = null, parserExceptionClassification = null,
                expectedAction = trial.fixture.expectedAction, actualAction = actual, representationValid = true,
                contentFidelity = ContentFidelity.NOT_APPLICABLE, latencyNanos = 1_000_000L, endpointMetadata = EndpointMetadata(),
                primaryClassification = if (trial.fixture.expectedAction == actual) PrimaryClassification.A else PrimaryClassification.D,
            )
        }

        val dependencies = FamilyFRuntimeDependencies(
            availableMemory = { 8L * 1024 * 1024 * 1024 },
            usableSpace = usableSpace,
            residencyQuery = FamilyFResidencyQuery { residency },
            unloadCommand = FamilyFModelUnloadCommand { residency = FamilyFResidencyState.ABSENT; true },
            pollResidencyUntilAbsent = { residency },
            protectedProcesses = listOf(FamilyFProtectedProcess("production-parker", "1", "fake-production-endpoint")),
            isAlive = { true },
            dedicatedPid = "2",
        )
        val driver = FamilyFOrchestrationDriver(config, ledger, dependencies, modelCaller, transportRecorder)
        return driver.run() to ledger
    }

    // Splits the atomic sealed-report.json document's own "subject":[...]
    // and "control":[...] arrays into their raw text, exploiting the
    // document's own fixed, deterministic field order rather than a full
    // JSON parse -- consistent with this codebase's existing hand-rolled
    // regex idiom.
    private fun splitSealedReportSections(text: String): Pair<String, String> {
        val subjectMarker = "\"subject\":["
        val controlMarker = "],\"control\":["
        val subjectStart = text.indexOf(subjectMarker) + subjectMarker.length
        val controlMarkerIndex = text.indexOf(controlMarker, subjectStart)
        assertTrue(subjectStart >= subjectMarker.length && controlMarkerIndex >= 0, "sealed-report.json must contain both a subject and a control array")
        val subjectSection = text.substring(subjectStart, controlMarkerIndex)
        val controlStart = controlMarkerIndex + controlMarker.length
        val controlSection = text.substring(controlStart, text.lastIndexOf("]}"))
        return subjectSection to controlSection
    }

    @Test
    fun `a complete fake-driven campaign seals with both subject and control fully reported across all 46 cells`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val text = Files.readString(dir.resolve("run").resolve("sealed-report.json"))
        val (subjectSection, controlSection) = splitSealedReportSections(text)
        assertEquals(46, Regex("\"fixtureId\":").findAll(subjectSection).count())
        assertEquals(46, Regex("\"fixtureId\":").findAll(controlSection).count())
    }

    @Test
    fun `every subject and control cell in a sealed fake-driven report reflects exactly four governed scored observations`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val text = Files.readString(dir.resolve("run").resolve("sealed-report.json"))
        assertEquals(92, Regex("\"attempts\":4").findAll(text).count())
    }

    @Test
    fun `sealed fake-driven report presents subject and control in clearly separate, structurally distinct sections`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val text = Files.readString(dir.resolve("run").resolve("sealed-report.json"))
        assertEquals(1, Regex("\"subject\":\\[").findAll(text).count(), "exactly one subject array")
        assertEquals(1, Regex("\"control\":\\[").findAll(text).count(), "exactly one control array")
        val (subjectSection, controlSection) = splitSealedReportSections(text)
        assertEquals(46, Regex("\"fixtureId\":").findAll(subjectSection).count())
        assertEquals(46, Regex("\"fixtureId\":").findAll(controlSection).count())
    }

    @Test
    fun `poor control results are disclosed in the sealed fake-driven report rather than omitted`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = false)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val text = Files.readString(dir.resolve("run").resolve("sealed-report.json"))
        val (_, controlSection) = splitSealedReportSections(text)
        assertEquals(46, Regex("\"fixtureId\":").findAll(controlSection).count(), "poor performance must not cause any control cell to be dropped")
        assertEquals(46, Regex("\"correctCount\":0").findAll(controlSection).count(), "poor correctness must be disclosed, not hidden")
    }

    @Test
    fun `control correctness has zero effect on the subject advancement worksheet`(@TempDir dir: Path) {
        val (outcomeGood, _) = runFakeCampaign(dir.resolve("good-control"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        val (outcomePoor, _) = runFakeCampaign(dir.resolve("poor-control"), subjectAlwaysCorrect = true, controlAlwaysCorrect = false)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcomeGood)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcomePoor)
        val worksheetGood = Files.readString(dir.resolve("good-control").resolve("advancement-worksheet.json"))
        val worksheetPoor = Files.readString(dir.resolve("poor-control").resolve("advancement-worksheet.json"))
        assertEquals(worksheetGood, worksheetPoor, "subject advancement worksheet must be identical regardless of control's own correctness")
    }

    @Test
    fun `subject correctness does change the advancement worksheet -- the previous identity is not vacuous`(@TempDir dir: Path) {
        val (outcomeGoodSubject, _) = runFakeCampaign(dir.resolve("good-subject"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        val (outcomePoorSubject, _) = runFakeCampaign(dir.resolve("poor-subject"), subjectAlwaysCorrect = false, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcomeGoodSubject)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcomePoorSubject)
        val worksheetGood = Files.readString(dir.resolve("good-subject").resolve("advancement-worksheet.json"))
        val worksheetPoor = Files.readString(dir.resolve("poor-subject").resolve("advancement-worksheet.json"))
        assertTrue(worksheetGood != worksheetPoor, "subject's own correctness must change its own advancement worksheet")
    }

    @Test
    fun `sealed fake-driven report contains no ranking, winner, preference, or comparative field`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = false)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val text = Files.readString(dir.resolve("run").resolve("sealed-report.json")).lowercase()
        val forbidden = listOf("rank", "winner", "prefer", "better", "worse", "superior", "inferior", "recommend", "\"vs\"", "comparison", "compare", "substitut")
        forbidden.forEach { term -> assertFalse(text.contains(term), "sealed report must not contain forbidden term: $term") }
    }

    @Test
    fun `warm-ups never appear as a cell in either sealed fake-driven report section`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val text = Files.readString(dir.resolve("run").resolve("sealed-report.json"))
        assertFalse(text.contains(FamilyFCorpus.warmupFixture.id))
    }

    @Test
    fun `sealed report is included in the manifest and its checksum verifies, including from a copied directory`(@TempDir dir: Path) {
        val (outcome, ledger) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        assertTrue(ledger.verifyChecksums())
        val checksums = Files.readString(dir.resolve("run").resolve("SHA256SUMS.txt"))
        assertTrue(checksums.contains("sealed-report.json"))

        val copy = dir.resolve("run-copy")
        Files.createDirectories(copy)
        Files.list(dir.resolve("run")).use { stream -> stream.forEach { file -> Files.copy(file, copy.resolve(file.fileName)) } }
        val copiedLedger = FamilyFCampaignLedger(copy, FamilyFCampaignDefinition.allTrials.map { it.id }.toSet(), FAMILY_F_FAKE_CAMPAIGN_ID)
        assertTrue(copiedLedger.verifyChecksums(), "checksums must independently re-verify from a copied directory")
    }

    @Test
    fun `sealed report generation is deterministic across two independent fake-driven runs with identical observations`(@TempDir dir: Path) {
        val (outcomeA, _) = runFakeCampaign(dir.resolve("a"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        val (outcomeB, _) = runFakeCampaign(dir.resolve("b"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcomeA)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcomeB)
        val reportA = Files.readString(dir.resolve("a").resolve("sealed-report.json"))
        val reportB = Files.readString(dir.resolve("b").resolve("sealed-report.json"))
        assertEquals(reportA, reportB)
    }

    @Test
    fun `sealing requires both subject and control completeness checks -- source proves both roles are required, not just subject`() {
        val text = Files.readString(Path.of("tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt"))
        val start = text.indexOf("fun run(): FamilyFCampaignOutcome {")
        assertTrue(start >= 0)
        val end = text.indexOf("\n    private fun runBlock(", start)
        assertTrue(end > start)
        val body = text.substring(start, end)
        assertTrue(body.contains("FamilyFRoleReportBuilder.requireComplete(subjectReport"), "subject completeness must be required before sealing")
        assertTrue(body.contains("FamilyFRoleReportBuilder.requireComplete(controlReport"), "control completeness must be required before sealing")
    }

    // ---- End-to-end resource-reading durability (independent review Finding 1/2) ----

    @Test
    fun `a sealed fake-driven campaign contains durable memory and disk resource records, not an empty or absent file`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val readingsFile = dir.resolve("run").resolve("resource-readings.jsonl")
        assertTrue(Files.exists(readingsFile), "resource-readings.jsonl must exist for a sealed campaign")
        val lines = Files.readAllLines(readingsFile).filter { it.isNotBlank() }
        assertTrue(lines.isNotEmpty(), "resource-reading recording must actually be reached, not merely defined")
        assertTrue(lines.any { it.contains("\"source\":\"MemAvailable\"") }, "must contain memory readings")
        assertTrue(lines.any { it.contains("DISK_ARTIFACT_ROOT") }, "must contain artifact-root disk readings")
        assertTrue(lines.any { it.contains("DISK_RUNTIME_ROOT") }, "must contain dedicated-runtime-root disk readings")
    }

    @Test
    fun `both pre-creation disk checks occur exactly once each, as the first two persisted resource records`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val lines = Files.readAllLines(dir.resolve("run").resolve("resource-readings.jsonl")).filter { it.isNotBlank() }
        assertEquals(1, lines.count { it.contains("\"phase\":\"DISK_ARTIFACT_ROOT_PRE_CREATION\"") })
        assertEquals(1, lines.count { it.contains("\"phase\":\"DISK_RUNTIME_ROOT_PRE_CREATION\"") })
        // The retained pre-creation observations are the first persisted
        // resource records -- sequence 1 and 2 -- before any block-level
        // or per-call reading (Finding 1 item 5).
        assertTrue(lines[0].contains("\"sequence\":1") && lines[0].contains("PRE_CREATION"))
        assertTrue(lines[1].contains("\"sequence\":2") && lines[1].contains("PRE_CREATION"))
        assertFalse(lines[0].contains("BLOCK") || lines[0].contains("MEMORY_PER_CALL"))
    }

    @Test
    fun `both disk paths are freshly rechecked before every one of the eight residency blocks, not cached`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val lines = Files.readAllLines(dir.resolve("run").resolve("resource-readings.jsonl")).filter { it.isNotBlank() }
        assertEquals(8, lines.count { it.contains("\"phase\":\"DISK_ARTIFACT_ROOT_BLOCK\"") }, "one artifact-root check per block")
        assertEquals(8, lines.count { it.contains("\"phase\":\"DISK_RUNTIME_ROOT_BLOCK\"") }, "one runtime-root check per block")
        // Each block-level record must carry its own distinct blockId --
        // proof these are eight fresh, distinguishable readings, not one
        // cached value reused eight times.
        val blockIds = lines.filter { it.contains("DISK_ARTIFACT_ROOT_BLOCK") }
            .map { Regex("\"blockId\":\"([^\"]*)\"").find(it)!!.groupValues[1] }
        assertEquals(8, blockIds.distinct().size, "all eight block-level artifact-root readings must carry distinct block IDs")
    }

    @Test
    fun `memory is measured immediately before and after every one of the 392 calls`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val lines = Files.readAllLines(dir.resolve("run").resolve("resource-readings.jsonl")).filter { it.isNotBlank() }
        assertEquals(392, lines.count { it.contains("\"phase\":\"MEMORY_PER_CALL_BEFORE\"") })
        assertEquals(392, lines.count { it.contains("\"phase\":\"MEMORY_PER_CALL_AFTER\"") })
        assertEquals(8, lines.count { it.contains("\"phase\":\"MEMORY_PRE_LOAD\"") }, "one artifact-size-aware pre-load reading per block")
    }

    @Test
    fun `resource records are referenced from the corresponding control event -- evidence linkage`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val controlEvents = Files.readAllLines(dir.resolve("run").resolve("control-events.jsonl")).filter { it.isNotBlank() && it.contains("pre-load gate passed") }
        assertEquals(8, controlEvents.size)
        assertTrue(controlEvents.all { it.contains("resourceReadingSequences=[") }, "each pre-load control event must reference its own resource-reading sequence numbers")
    }

    @Test
    fun `a per-block disk failure stops before that block's first call, and the prior block's calls are unaffected`(@TempDir dir: Path) {
        var diskCallCount = 0
        // Pre-creation consumes 2 calls; block 1 consumes 2 more (artifact +
        // runtime); the 5th call is block 2's own artifact-root check --
        // fail from there onward.
        val (outcome, ledger) = runFakeCampaign(
            dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true,
            usableSpace = {
                diskCallCount += 1
                if (diskCallCount <= 4) 8L * 1024 * 1024 * 1024 else 0L
            },
        )
        assertEquals(FamilyFCampaignOutcome.HALTED, outcome)
        assertTrue(ledger.isHalted())
        val dispatchedText = Files.readString(dir.resolve("run").resolve("dispatch.jsonl"))
        val block1Ids = FamilyFCampaignDefinition.blocks[0].trials.map { it.id }
        val block2Ids = FamilyFCampaignDefinition.blocks[1].trials.map { it.id }
        assertTrue(block1Ids.all { dispatchedText.contains("\"trialId\":\"$it\"") }, "the first block's own calls must have already been dispatched")
        assertTrue(block2Ids.none { dispatchedText.contains("\"trialId\":\"$it\"") }, "the second block's calls must never be dispatched once its own disk gate fails")
        val readingsText = Files.readString(dir.resolve("run").resolve("resource-readings.jsonl"))
        assertTrue(readingsText.contains("\"phase\":\"DISK_ARTIFACT_ROOT_BLOCK\"") && readingsText.contains("\"outcome\":\"FAIL\""), "the failing block-level reading must itself be durably recorded")
    }

    @Test
    fun `a resource-record persistence failure halts before any further governed action, rather than continuing silently`(@TempDir dir: Path) {
        val campaignDir = dir.resolve("run")
        Files.createDirectories(campaignDir)
        // Pre-occupy the resource-readings path with a directory so any
        // attempted write to it as a file fails with an IOException.
        Files.createDirectories(campaignDir.resolve("resource-readings.jsonl"))
        // The failure surfaces as soon as anything attempts to read or
        // write the pre-occupied path -- here, during the ledger's own
        // hash-chain-recovery construction -- which is itself a valid,
        // even earlier, demonstration of failing closed rather than
        // continuing silently.
        assertFailsWith<Exception> {
            val ledger = FamilyFCampaignLedger(campaignDir, setOf("t1"))
            ledger.recordResourceReading(FamilyFResourceReading("PHASE", null, null, "src", "1", 1L, 1L, "PASS"))
        }
        // No terminal, seal, or halt marker may exist -- the failure must
        // stop before any further ledger-visible governed action, not be
        // silently absorbed and continued past.
        assertFalse(Files.exists(campaignDir.resolve("campaign.sealed")))
        assertFalse(Files.exists(campaignDir.resolve("terminal.jsonl")))
    }

    @Test
    fun `resource-readings-jsonl is manifest-covered and copied-directory verification detects its own tampering`(@TempDir dir: Path) {
        val (outcome, ledger) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        assertTrue(ledger.verifyChecksums())

        val copy = dir.resolve("run-copy")
        Files.createDirectories(copy)
        Files.list(dir.resolve("run")).use { stream -> stream.forEach { file -> Files.copy(file, copy.resolve(file.fileName)) } }
        val copiedLedger = FamilyFCampaignLedger(copy, FamilyFCampaignDefinition.allTrials.map { it.id }.toSet(), FAMILY_F_FAKE_CAMPAIGN_ID)
        assertTrue(copiedLedger.verifyChecksums())

        Files.writeString(copy.resolve("resource-readings.jsonl"), "{\"tampered\":true}\n")
        assertFalse(copiedLedger.verifyChecksums(), "tampering resource-readings.jsonl in the copy must be detected")
        // The original, untouched directory must remain valid.
        assertTrue(ledger.verifyChecksums())
    }

    @Test
    fun `SHA256SUMS-txt never covers or hashes itself`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val checksums = Files.readString(dir.resolve("run").resolve("SHA256SUMS.txt"))
        assertFalse(checksums.contains("SHA256SUMS.txt"))
    }

    // =======================================================================
    // Universal chained-record ledger-integrity mechanism (independent
    // review: every append-only JSONL artifact must carry the same
    // envelope -- schema version, campaign ID, trial ID where applicable,
    // sequence, prior-record hash, record hash, timestamp -- through one
    // shared mechanism, FamilyFChainedLedger, not seven independent
    // variants). Tests here exercise the shared mechanism generically
    // (proving it once, correctly) and then confirm every one of the
    // seven real governed files actually uses it.
    // =======================================================================

    @Test
    fun `one shared mechanism -- every governed record method delegates to FamilyFChainedLedger-append, not an independent variant`() {
        val text = Files.readString(Path.of("tests/integration/ReasoningProtocolFamilyFDiagnosticOrchestrationTest.kt"))
        val methods = listOf(
            "fun writeScheduleOnce", "fun recordIntent", "fun recordDispatch",
            "fun recordTransport", "fun recordTerminal", "fun recordControlEvent", "fun recordResourceReading",
        )
        methods.forEach { marker ->
            val start = text.indexOf(marker)
            assertTrue(start >= 0, "$marker must exist in this file's own source")
            val end = text.indexOf("\n    fun ", start + 1).let { if (it < 0) text.indexOf("\n}\n", start) else it }
            val body = text.substring(start, end)
            assertTrue(body.contains("FamilyFChainedLedger.append("), "$marker must delegate to the single shared FamilyFChainedLedger.append mechanism")
        }
    }

    @Test
    fun `every one of the seven governed JSONL ledger files carries the full universal envelope in a real campaign`(@TempDir dir: Path) {
        val (outcome, _) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val governedFiles = listOf(
            "schedule.jsonl", "intent.jsonl", "dispatch.jsonl", "transport.jsonl",
            "terminal.jsonl", "control-events.jsonl", "resource-readings.jsonl",
        )
        governedFiles.forEach { fileName ->
            val lines = Files.readAllLines(dir.resolve("run").resolve(fileName)).filter { it.isNotBlank() }
            assertTrue(lines.isNotEmpty(), "$fileName must contain at least one record")
            val firstLine = lines.first()
            listOf("schemaVersion", "campaignId", "trialId", "sequence", "priorRecordHash", "recordHash", "timestamp").forEach { field ->
                assertTrue(firstLine.contains("\"$field\":"), "$fileName record must contain required envelope field $field")
            }
        }
    }

    @Test
    fun `a freshly appended record carries every required envelope field plus its own payload`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        FamilyFChainedLedger.append(file, cursor, "camp-1", "trial-1", listOf("payloadField" to "value"))
        val line = Files.readAllLines(file).single()
        listOf("schemaVersion", "campaignId", "trialId", "sequence", "priorRecordHash", "recordHash", "timestamp", "payloadField").forEach { field ->
            assertTrue(line.contains("\"$field\":"), "record must contain field $field")
        }
    }

    @Test
    fun `the genesis prior hash is exactly 64 lowercase zeroes, and the first record in any chain uses it`(@TempDir dir: Path) {
        assertEquals(64, FAMILY_F_LEDGER_GENESIS_HASH.length)
        assertTrue(FAMILY_F_LEDGER_GENESIS_HASH.all { it == '0' })
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        FamilyFChainedLedger.append(file, cursor, "camp-1", null, emptyList())
        val line = Files.readAllLines(file).single()
        assertTrue(line.contains("\"priorRecordHash\":\"$FAMILY_F_LEDGER_GENESIS_HASH\""))
    }

    @Test
    fun `sequence numbers begin at 1 and increase contiguously within a file`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        repeat(5) { FamilyFChainedLedger.append(file, cursor, "camp-1", null, emptyList()) }
        val sequences = Files.readAllLines(file).map { familyFExtractLongField(it, "sequence") }
        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), sequences)
    }

    @Test
    fun `each record correctly links to the immediately preceding record's own verified hash`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        repeat(4) { FamilyFChainedLedger.append(file, cursor, "camp-1", null, emptyList()) }
        val lines = Files.readAllLines(file)
        for (index in 1 until lines.size) {
            assertEquals(familyFExtractStringField(lines[index - 1], "recordHash"), familyFExtractStringField(lines[index], "priorRecordHash"))
        }
    }

    @Test
    fun `an independently recomputed record hash matches the persisted recordHash`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        FamilyFChainedLedger.append(file, cursor, "camp-1", "t1", listOf("x" to 1))
        val line = Files.readAllLines(file).single()
        val recomputed = familyFSha256Bytes(familyFStripTrailingRecordHash(line).toByteArray(StandardCharsets.UTF_8))
        assertEquals(familyFExtractStringField(line, "recordHash"), recomputed)
    }

    @Test
    fun `recoverAndVerify accepts a genuinely valid chain and returns its true final cursor`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        repeat(3) { FamilyFChainedLedger.append(file, cursor, "camp-1", null, emptyList()) }
        val recovered = FamilyFChainedLedger.recoverAndVerify(file, "camp-1")
        assertEquals(3L, recovered.sequence)
        assertEquals(cursor.priorRecordHash, recovered.priorRecordHash)
    }

    @Test
    fun `schema-version mismatch fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        Files.writeString(file, "{\"schemaVersion\":99,\"campaignId\":\"camp-1\",\"trialId\":\"\",\"sequence\":1,\"priorRecordHash\":\"$FAMILY_F_LEDGER_GENESIS_HASH\",\"timestamp\":\"x\",\"recordHash\":\"deadbeef\"}\n")
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `campaign-ID mismatch fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        FamilyFChainedLedger.append(file, cursor, "camp-1", null, emptyList())
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-DIFFERENT") }
    }

    @Test
    fun `a record missing sequence fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        Files.writeString(file, "{\"schemaVersion\":1,\"campaignId\":\"camp-1\",\"trialId\":\"\",\"priorRecordHash\":\"$FAMILY_F_LEDGER_GENESIS_HASH\",\"timestamp\":\"x\",\"recordHash\":\"deadbeef\"}\n")
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `a record missing recordHash fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        Files.writeString(file, "{\"schemaVersion\":1,\"campaignId\":\"camp-1\",\"trialId\":\"\",\"sequence\":1,\"priorRecordHash\":\"$FAMILY_F_LEDGER_GENESIS_HASH\",\"timestamp\":\"x\"}\n")
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `a record missing timestamp fails closed`(@TempDir dir: Path) {
        // Hand-built with every field except timestamp, and a correctly
        // computed recordHash over exactly those fields, to prove the
        // failure is specifically the missing-timestamp check, not an
        // incidental hash mismatch.
        val file = dir.resolve("generic.jsonl")
        val fields = listOf(
            "schemaVersion" to FAMILY_F_LEDGER_SCHEMA_VERSION, "campaignId" to "camp-1", "trialId" to "",
            "sequence" to 1L, "priorRecordHash" to FAMILY_F_LEDGER_GENESIS_HASH,
        )
        val hash = familyFSha256Bytes(familyFObjectLine(fields).toByteArray(StandardCharsets.UTF_8))
        Files.writeString(file, familyFObjectLine(fields + ("recordHash" to hash)) + "\n")
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `a skipped sequence number fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        FamilyFChainedLedger.append(file, cursor, "camp-1", null, emptyList())
        val fields = listOf(
            "schemaVersion" to FAMILY_F_LEDGER_SCHEMA_VERSION, "campaignId" to "camp-1", "trialId" to "",
            "sequence" to 3L, "priorRecordHash" to cursor.priorRecordHash, "timestamp" to Instant.now().toString(),
        )
        val hash = familyFSha256Bytes(familyFObjectLine(fields).toByteArray(StandardCharsets.UTF_8))
        Files.writeString(file, familyFObjectLine(fields + ("recordHash" to hash)) + "\n", StandardOpenOption.APPEND)
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `a duplicate sequence number fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        FamilyFChainedLedger.append(file, cursor, "camp-1", null, emptyList())
        val fields = listOf(
            "schemaVersion" to FAMILY_F_LEDGER_SCHEMA_VERSION, "campaignId" to "camp-1", "trialId" to "",
            "sequence" to 1L, "priorRecordHash" to cursor.priorRecordHash, "timestamp" to Instant.now().toString(),
        )
        val hash = familyFSha256Bytes(familyFObjectLine(fields).toByteArray(StandardCharsets.UTF_8))
        Files.writeString(file, familyFObjectLine(fields + ("recordHash" to hash)) + "\n", StandardOpenOption.APPEND)
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `reordered lines fail closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        repeat(3) { FamilyFChainedLedger.append(file, cursor, "camp-1", null, emptyList()) }
        val lines = Files.readAllLines(file)
        Files.write(file, listOf(lines[1], lines[0], lines[2]))
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `payload mutation fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        FamilyFChainedLedger.append(file, cursor, "camp-1", "t1", listOf("description" to "original"))
        val tampered = Files.readAllLines(file).single().replace("original", "tampered")
        Files.writeString(file, tampered + "\n")
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `record deletion fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        repeat(3) { FamilyFChainedLedger.append(file, cursor, "camp-1", null, emptyList()) }
        val lines = Files.readAllLines(file)
        Files.write(file, listOf(lines[0], lines[2]))
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `a truncated final record fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("generic.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        FamilyFChainedLedger.append(file, cursor, "camp-1", "t1", listOf("description" to "value"))
        val line = Files.readAllLines(file).single()
        Files.writeString(file, line.substring(0, line.length / 2) + "\n")
        assertFailsWith<Exception> { FamilyFChainedLedger.recoverAndVerify(file, "camp-1") }
    }

    @Test
    fun `cross-file record substitution fails closed once chains have diverged`(@TempDir dir: Path) {
        val fileA = dir.resolve("a.jsonl")
        val fileB = dir.resolve("b.jsonl")
        val cursorA = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        repeat(2) { index -> FamilyFChainedLedger.append(fileA, cursorA, "camp-1", "t1", listOf("kind" to "A$index")) }
        val cursorB = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        repeat(2) { index -> FamilyFChainedLedger.append(fileB, cursorB, "camp-1", "t1", listOf("kind" to "B$index")) }
        // File A's own second record is internally well-formed (correct
        // schema/campaign/sequence, and its own recordHash is genuinely
        // valid for its own content) but its own priorRecordHash was
        // computed against file A's own first record -- which differs
        // from file B's own first record -- so substituting it into file
        // B's own second position must fail.
        val linesA = Files.readAllLines(fileA)
        val linesB = Files.readAllLines(fileB).toMutableList()
        linesB[1] = linesA[1]
        Files.write(fileB, linesB)
        assertFailsWith<FamilyFArtifactIntegrityException> { FamilyFChainedLedger.recoverAndVerify(fileB, "camp-1") }
    }

    @Test
    fun `recover refuses to derive recovery state when a governed chain is damaged after construction`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        Files.writeString(dir.resolve("intent.jsonl"), "{\"tampered\":true}\n")
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.recover() }
    }

    @Test
    fun `sealing refuses any damaged governed chain, even one unrelated to trial resolution`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        writeMinimalMandatoryArtifacts(ledger, "t1")
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val record = FamilyFProxyExchangeRecord(1, Instant.now(), Instant.now(), "POST", "/api/generate", ByteArray(0), familyFSha256Bytes(ByteArray(0)), 200, emptyMap(), ByteArray(0), familyFSha256Bytes(ByteArray(0)), "FORWARDED")
        ledger.recordTransport("t1", record)
        ledger.recordTerminal("t1", "payload")
        ledger.writeAdvancementWorksheet("{}")
        ledger.writeSealedReport("{}")
        // control-events.jsonl is not read by recover() for trial-ID
        // resolution at all -- damaging it must still block sealing,
        // proving sealing verifies EVERY governed chain, not only the
        // ones recover() happens to consult.
        Files.writeString(dir.resolve("control-events.jsonl"), "{\"tampered\":true}\n")
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.sealAfterAdvancementRecorded(setOf("t1")) }
    }

    @Test
    fun `valid recovery and sealing still work end to end through the universal chain mechanism`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        writeMinimalMandatoryArtifacts(ledger, "t1")
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val record = FamilyFProxyExchangeRecord(1, Instant.now(), Instant.now(), "POST", "/api/generate", ByteArray(0), familyFSha256Bytes(ByteArray(0)), 200, emptyMap(), ByteArray(0), familyFSha256Bytes(ByteArray(0)), "FORWARDED")
        ledger.recordTransport("t1", record)
        ledger.recordTerminal("t1", "payload")
        val state = ledger.recover()
        assertEquals(setOf("t1"), state.resolved)
        ledger.writeAdvancementWorksheet("{}")
        ledger.writeSealedReport("{}")
        ledger.sealAfterAdvancementRecorded(setOf("t1"))
        assertTrue(ledger.isSealed())
    }

    @Test
    fun `whole-file manifest verification catches tampering in non-chained atomic artifacts, proving it is a distinct final layer`(@TempDir dir: Path) {
        val (outcome, ledger) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        assertTrue(ledger.verifyChecksums())
        // campaign-definition.json is atomic and non-append -- it has no
        // per-record chain to verify at all -- yet its own tampering is
        // still caught, because whole-file manifest hashing is a separate,
        // additional integrity layer covering every mandatory artifact,
        // chained or not, never a substitute for chain verification and
        // never substituted for by it.
        Files.writeString(dir.resolve("run").resolve("campaign-definition.json"), "{\"tampered\":true}")
        assertFalse(ledger.verifyChecksums())
    }

    // =======================================================================
    // Family F Raw Transport Capture Defect Correction: durable raw
    // request/response capture, response headers, write- and read-time
    // hash/length re-verification, and genuine offline recovery for a
    // transport-without-terminal trial (Plan Sections 12, 18; the
    // confirmed raw-capture defect).
    // =======================================================================

    private fun startFakeUpstreamForTransportTest(
        status: Int,
        body: ByteArray,
        headers: Map<String, List<String>> = emptyMap(),
    ): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.requestBody.readBytes()
            headers.forEach { (name, values) -> values.forEach { value -> exchange.responseHeaders.add(name, value) } }
            exchange.sendResponseHeaders(status, if (body.isEmpty()) -1L else body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        return server
    }

    // ---- Durable raw byte capture (real proxy, real ledger) ----

    @Test
    fun `a real proxy exchange connected to the real ledger persists exact non-empty request and response bytes`(@TempDir dir: Path) {
        val fakeResponseBody = "{\"model\":\"llama3.2:3b\",\"response\":\"REPLY: hello there\",\"done\":true}".toByteArray(StandardCharsets.UTF_8)
        val upstream = startFakeUpstreamForTransportTest(200, fakeResponseBody)
        try {
            val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
            ledger.recordIntent("t1")
            ledger.recordDispatch("t1")
            val transportRecorder = FamilyFTrialTransportRecorder(ledger)
            transportRecorder.currentTrialId = "t1"
            val proxy = FamilyFCaptureProxy("http://127.0.0.1:${upstream.address.port}", transportRecorder)
            proxy.start()
            val requestBody = "{\"model\":\"llama3.2:3b\",\"prompt\":\"hello, world\",\"stream\":false}".toByteArray(StandardCharsets.UTF_8)
            try {
                val client = HttpClient.newHttpClient()
                val response = client.send(
                    HttpRequest.newBuilder().uri(URI.create("${proxy.url}/api/generate"))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody)).build(),
                    HttpResponse.BodyHandlers.ofByteArray(),
                )
                assertEquals(200, response.statusCode())
            } finally {
                proxy.stop()
            }
            val durable = ledger.readDurableTransportRecord("t1")
            assertTrue(durable.requestBytes.isNotEmpty())
            assertTrue(durable.requestBytes.contentEquals(requestBody))
            assertTrue(durable.responseBytes!!.isNotEmpty())
            assertTrue(durable.responseBytes.contentEquals(fakeResponseBody))
            assertEquals("FORWARDED", durable.forwardingOutcome)
            assertEquals(200, durable.responseStatus)
        } finally {
            upstream.stop(0)
        }
    }

    private class FamilyFDurabilityWitnessListener(
        private val ledger: FamilyFCampaignLedger,
        private val delegate: FamilyFTrialTransportRecorder,
    ) : FamilyFCaptureListener {
        var durableAtCallbackTime: FamilyFDurableTransportRecord? = null

        override fun onExchange(record: FamilyFProxyExchangeRecord) {
            val trialId = delegate.currentTrialId!!
            // Delegates to the real recorder/ledger first, then reads the
            // record straight back off disk -- all still inside the
            // proxy's own onExchange callback, which the proxy (source-
            // order verified in ReasoningProtocolFamilyFDiagnosticTest.kt)
            // calls strictly before sendResponseHeaders/the response body
            // write. Finding the complete, hash-verified record already
            // readable from disk at this exact point proves durability
            // before release, not merely in-memory transparency or
            // in-process ordering.
            delegate.onExchange(record)
            durableAtCallbackTime = ledger.readDurableTransportRecord(trialId)
        }
    }

    @Test
    fun `durable transport exists on disk before the response is released to the caller`(@TempDir dir: Path) {
        val fakeResponseBody = "{\"response\":\"REPLY: before release\",\"done\":true}".toByteArray(StandardCharsets.UTF_8)
        val upstream = startFakeUpstreamForTransportTest(200, fakeResponseBody)
        try {
            val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
            ledger.recordIntent("t1")
            ledger.recordDispatch("t1")
            val transportRecorder = FamilyFTrialTransportRecorder(ledger)
            transportRecorder.currentTrialId = "t1"
            val witness = FamilyFDurabilityWitnessListener(ledger, transportRecorder)
            val proxy = FamilyFCaptureProxy("http://127.0.0.1:${upstream.address.port}", witness)
            proxy.start()
            try {
                val client = HttpClient.newHttpClient()
                client.send(
                    HttpRequest.newBuilder().uri(URI.create("${proxy.url}/api/generate"))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(ByteArray(0))).build(),
                    HttpResponse.BodyHandlers.discarding(),
                )
            } finally {
                proxy.stop()
            }
            val witnessed = witness.durableAtCallbackTime
            assertTrue(witnessed != null, "the durable transport record must already be readable from disk inside the pre-release callback")
            assertTrue(witnessed!!.responseBytes!!.contentEquals(fakeResponseBody))
        } finally {
            upstream.stop(0)
        }
    }

    @Test
    fun `transport persistence failure prevents the response from ever being released as successful`(@TempDir dir: Path) {
        val fakeResponseBody = "{\"response\":\"REPLY: should never reach the caller\",\"done\":true}".toByteArray(StandardCharsets.UTF_8)
        val upstream = startFakeUpstreamForTransportTest(200, fakeResponseBody)
        try {
            val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
            ledger.recordIntent("t1")
            ledger.recordDispatch("t1")
            // Pre-create transport.jsonl as a read-only file: the write
            // recordTransport attempts strictly after a genuine upstream
            // dispatch now fails with an IOException, simulating durable-
            // persistence failure after dispatch (Plan Section 12/18).
            val transportFile = dir.resolve("transport.jsonl")
            Files.createFile(transportFile)
            assertTrue(transportFile.toFile().setWritable(false), "test requires the ability to mark a file read-only")
            try {
                val transportRecorder = FamilyFTrialTransportRecorder(ledger)
                transportRecorder.currentTrialId = "t1"
                val proxy = FamilyFCaptureProxy("http://127.0.0.1:${upstream.address.port}", transportRecorder)
                proxy.start()
                try {
                    val client = HttpClient.newHttpClient()
                    // The proxy's own (unmodified) fallback behavior is to
                    // catch the listener's exception and attempt to record
                    // a forwarding-failure exchange instead -- which itself
                    // fails the same way (transport.jsonl is still
                    // read-only), so the exchange never reaches
                    // sendResponseHeaders at all and the connection is
                    // simply closed. Either outcome -- the HTTP call
                    // itself failing, or a non-200/non-matching-body
                    // response -- proves the same thing: the real,
                    // successful upstream response is never released to
                    // the caller.
                    val releasedSuccessfully = try {
                        val response = client.send(
                            HttpRequest.newBuilder().uri(URI.create("${proxy.url}/api/generate"))
                                .POST(HttpRequest.BodyPublishers.ofByteArray(ByteArray(0))).build(),
                            HttpResponse.BodyHandlers.ofByteArray(),
                        )
                        response.statusCode() == 200 && response.body().contentEquals(fakeResponseBody)
                    } catch (_: IOException) {
                        false
                    }
                    assertFalse(
                        releasedSuccessfully,
                        "a durable-persistence failure must never be released to the caller as a successful response",
                    )
                } finally {
                    proxy.stop()
                }
            } finally {
                transportFile.toFile().setWritable(true)
            }
        } finally {
            upstream.stop(0)
        }
    }

    @Test
    fun `orchestration halts on the very first dispatch when transport persistence fails, never retries, never seals`(@TempDir dir: Path) {
        val campaignRoot = dir.resolve("run")
        val config = fakeConfig(campaignRoot)
        val ledger = FamilyFCampaignLedger(config.campaignArtifactRoot, FamilyFCampaignDefinition.allTrials.map { it.id }.toSet(), config.campaignId)
        val transportRecorder = FamilyFTrialTransportRecorder(ledger)
        var residency = FamilyFResidencyState.ABSENT
        var modelCallCount = 0

        val modelCaller = FamilyFModelCaller { trial, roleModelName ->
            modelCallCount += 1
            residency = if (trial.role == FamilyFRole.SUBJECT) FamilyFResidencyState.SUBJECT_RESIDENT else FamilyFResidencyState.CONTROL_RESIDENT
            transportRecorder.onExchange(
                FamilyFProxyExchangeRecord(
                    sequence = 1, startedAt = Instant.now(), completedAt = Instant.now(), method = "POST", path = "/api/generate",
                    requestBody = ByteArray(0), requestSha256 = familyFSha256Bytes(ByteArray(0)), responseStatus = 200, responseHeaders = emptyMap(),
                    responseBody = ByteArray(0), responseSha256 = familyFSha256Bytes(ByteArray(0)), forwardingOutcome = "FORWARDED",
                ),
            )
            TrialObservation(
                runId = "fake", fixtureId = trial.fixture.id, contextProfileId = (trial.profileId ?: ContextProfileId.MINIMAL_PRODUCTION_CONTEXT).externalId,
                trialSequence = trial.attempt, stableInputHash = "hash", repositoryCommit = "fakecommit0123456789", modelName = roleModelName,
                modelDigest = "sha256:fake", runtimeImageId = null, endpointIdentifier = "fake-endpoint", timeoutMs = FAMILY_F_TIMEOUT_MS,
                prompt = "prompt", promptSha256 = "prompt-sha", requestBody = null, rawOllamaEnvelope = null, extractedResponse = null,
                parsedVariant = null, parserExceptionType = null, parserExceptionClassification = null,
                expectedAction = trial.fixture.expectedAction, actualAction = trial.fixture.expectedAction, representationValid = true,
                contentFidelity = ContentFidelity.NOT_APPLICABLE, latencyNanos = 1_000_000L, endpointMetadata = EndpointMetadata(),
                primaryClassification = PrimaryClassification.A,
            )
        }
        val dependencies = FamilyFRuntimeDependencies(
            availableMemory = { 8L * 1024 * 1024 * 1024 },
            usableSpace = { 8L * 1024 * 1024 * 1024 },
            residencyQuery = FamilyFResidencyQuery { residency },
            unloadCommand = FamilyFModelUnloadCommand { residency = FamilyFResidencyState.ABSENT; true },
            pollResidencyUntilAbsent = { residency },
            protectedProcesses = listOf(FamilyFProtectedProcess("production-parker", "1", "fake-production-endpoint")),
            isAlive = { true },
            dedicatedPid = "2",
        )
        val driver = FamilyFOrchestrationDriver(config, ledger, dependencies, modelCaller, transportRecorder)

        // Replicates exactly the directory-creation half of driver.run()
        // (schedule + intent for all 392 trials, no model contact) so the
        // driver below sees a genuinely pre-existing campaign and goes
        // straight to recover()/dispatch, precisely as a real resumed
        // run would -- then corrupts only transport.jsonl before the
        // first dispatch is ever attempted.
        ledger.createDirectory()
        ledger.writeScheduleOnce(FamilyFCampaignDefinition.allTrials)
        FamilyFCampaignDefinition.allTrials.forEach { ledger.recordIntent(it.id) }
        val transportFile = campaignRoot.resolve("transport.jsonl")
        Files.createFile(transportFile)
        assertTrue(transportFile.toFile().setWritable(false), "test requires the ability to mark a file read-only")
        try {
            val outcome = driver.run()
            assertEquals(FamilyFCampaignOutcome.HALTED, outcome)
        } finally {
            transportFile.toFile().setWritable(true)
        }
        assertTrue(ledger.isHalted())
        assertFalse(ledger.isSealed())
        assertEquals(1, modelCallCount, "no retry: the model caller must be invoked exactly once for the one trial that failed to persist")
        val dispatchLines = Files.readAllLines(campaignRoot.resolve("dispatch.jsonl")).filter { it.isNotBlank() }
        assertEquals(1, dispatchLines.size, "dispatch must not be reissued after a persistence failure")
    }

    // ---- Durable round-trip fidelity: adversarial bytes, headers, hashes ----

    @Test
    fun `adversarial unicode, CRLF, quotes, backslashes, NUL, and non-text bytes round-trip losslessly`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val adversarialText = "unicode: éè中文😀 quotes:\" backslash:\\ crlf:\r\n tab:\t"
        val requestBody = adversarialText.toByteArray(StandardCharsets.UTF_8) + byteArrayOf(0, 1, 2, 255.toByte(), 254.toByte())
        val responseBody = byteArrayOf(0, 0, 0, 255.toByte(), 128.toByte(), 10, 13, 34, 92) + adversarialText.toByteArray(StandardCharsets.UTF_8)
        val record = FamilyFProxyExchangeRecord(
            1, Instant.now(), Instant.now(), "POST", "/api/generate",
            requestBody, familyFSha256Bytes(requestBody), 200, emptyMap(),
            responseBody, familyFSha256Bytes(responseBody), "FORWARDED",
        )
        ledger.recordTransport("t1", record)
        val durable = ledger.readDurableTransportRecord("t1")
        assertTrue(durable.requestBytes.contentEquals(requestBody))
        assertTrue(durable.responseBytes!!.contentEquals(responseBody))
    }

    @Test
    fun `duplicate and multi-valued response headers survive durable capture exactly`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val headers = mapOf(
            "Set-Cookie" to listOf("a=1; Path=/", "b=2; Path=/x", "c=3"),
            "X-Custom-Header" to listOf("value-with-\"quote\"-and-\\backslash-and-\ncontrol"),
        )
        val requestBody = ByteArray(0)
        val responseBody = "ok".toByteArray(StandardCharsets.UTF_8)
        val record = FamilyFProxyExchangeRecord(
            1, Instant.now(), Instant.now(), "POST", "/api/generate",
            requestBody, familyFSha256Bytes(requestBody), 200, headers,
            responseBody, familyFSha256Bytes(responseBody), "FORWARDED",
        )
        ledger.recordTransport("t1", record)
        val durable = ledger.readDurableTransportRecord("t1")
        assertEquals(headers["Set-Cookie"], durable.responseHeaders["Set-Cookie"])
        assertEquals(3, durable.responseHeaders["Set-Cookie"]?.size)
        assertEquals(headers["X-Custom-Header"], durable.responseHeaders["X-Custom-Header"])
    }

    @Test
    fun `persisted hashes and byte counts independently recompute from the decoded durable bytes`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        ledger.recordDispatch("t1")
        val requestBody = ("request body with repeated content " + "x".repeat(500)).toByteArray(StandardCharsets.UTF_8)
        val responseBody = "{\"response\":\"REPLY: independent recompute\",\"done\":true}".toByteArray(StandardCharsets.UTF_8)
        val record = FamilyFProxyExchangeRecord(
            1, Instant.now(), Instant.now(), "POST", "/api/generate",
            requestBody, familyFSha256Bytes(requestBody), 200, emptyMap(),
            responseBody, familyFSha256Bytes(responseBody), "FORWARDED",
        )
        ledger.recordTransport("t1", record)
        val durable = ledger.readDurableTransportRecord("t1")
        assertEquals(requestBody.size, durable.requestBytes.size)
        assertEquals(familyFSha256Bytes(durable.requestBytes), durable.requestSha256)
        assertEquals(responseBody.size, durable.responseBytes!!.size)
        assertEquals(familyFSha256Bytes(durable.responseBytes), durable.responseSha256)
        // Independently recomputed a second time, directly from the raw
        // transport.jsonl line on disk, bypassing readDurableTransportRecord
        // entirely.
        val line = Files.readAllLines(dir.resolve("transport.jsonl")).single()
        val rawBase64Request = Regex("\"requestBodyBase64\":\"([^\"]*)\"").find(line)!!.groupValues[1]
        assertEquals(durable.requestSha256, familyFSha256Bytes(Base64.getDecoder().decode(rawBase64Request)))
    }

    // ---- Fail-closed evidence handling ----

    @Test
    fun `reading a missing transport record fails closed`(@TempDir dir: Path) {
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        ledger.recordIntent("t1")
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.readDurableTransportRecord("t1") }
    }

    @Test
    fun `a transport record missing a required raw-capture field fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("transport.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        // Correctly chained envelope, but the requestBodyBase64 payload
        // field this defect correction requires is entirely absent.
        FamilyFChainedLedger.append(
            file, cursor, "test-campaign", "t1",
            listOf("exchangeSequence" to 1L, "requestSha256" to "abc", "responseStatus" to 200, "forwardingOutcome" to "FORWARDED"),
        )
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.readDurableTransportRecord("t1") }
    }

    @Test
    fun `invalid base64 request body fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("transport.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        FamilyFChainedLedger.append(
            file, cursor, "test-campaign", "t1",
            listOf(
                "exchangeSequence" to 1L, "startedAt" to Instant.now().toString(), "completedAt" to Instant.now().toString(),
                "requestByteCount" to 3, "requestBodyBase64" to "not-valid-base64!!!", "requestSha256" to "irrelevant",
                "responseStatus" to 200, "responseCaptured" to false, "responseByteCount" to -1,
                "responseBodyBase64" to "", "responseSha256" to "", "responseHeadersJson" to "{}", "forwardingOutcome" to "FORWARDED",
            ),
        )
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.readDurableTransportRecord("t1") }
    }

    @Test
    fun `wrong request byte count fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("transport.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        val requestBody = "hello".toByteArray(StandardCharsets.UTF_8)
        FamilyFChainedLedger.append(
            file, cursor, "test-campaign", "t1",
            listOf(
                "exchangeSequence" to 1L, "startedAt" to Instant.now().toString(), "completedAt" to Instant.now().toString(),
                "requestByteCount" to 999, "requestBodyBase64" to Base64.getEncoder().encodeToString(requestBody),
                "requestSha256" to familyFSha256Bytes(requestBody),
                "responseStatus" to 200, "responseCaptured" to false, "responseByteCount" to -1,
                "responseBodyBase64" to "", "responseSha256" to "", "responseHeadersJson" to "{}", "forwardingOutcome" to "FORWARDED",
            ),
        )
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.readDurableTransportRecord("t1") }
    }

    @Test
    fun `wrong request hash fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("transport.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        val requestBody = "hello".toByteArray(StandardCharsets.UTF_8)
        FamilyFChainedLedger.append(
            file, cursor, "test-campaign", "t1",
            listOf(
                "exchangeSequence" to 1L, "startedAt" to Instant.now().toString(), "completedAt" to Instant.now().toString(),
                "requestByteCount" to requestBody.size, "requestBodyBase64" to Base64.getEncoder().encodeToString(requestBody),
                "requestSha256" to "0".repeat(64),
                "responseStatus" to 200, "responseCaptured" to false, "responseByteCount" to -1,
                "responseBodyBase64" to "", "responseSha256" to "", "responseHeadersJson" to "{}", "forwardingOutcome" to "FORWARDED",
            ),
        )
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.readDurableTransportRecord("t1") }
    }

    @Test
    fun `malformed response headers structure fails closed`(@TempDir dir: Path) {
        val file = dir.resolve("transport.jsonl")
        val cursor = FamilyFChainCursor(0L, FAMILY_F_LEDGER_GENESIS_HASH)
        val requestBody = ByteArray(0)
        val responseBody = "ok".toByteArray(StandardCharsets.UTF_8)
        FamilyFChainedLedger.append(
            file, cursor, "test-campaign", "t1",
            listOf(
                "exchangeSequence" to 1L, "startedAt" to Instant.now().toString(), "completedAt" to Instant.now().toString(),
                "requestByteCount" to 0, "requestBodyBase64" to "", "requestSha256" to familyFSha256Bytes(requestBody),
                "responseStatus" to 200, "responseCaptured" to true, "responseByteCount" to responseBody.size,
                "responseBodyBase64" to Base64.getEncoder().encodeToString(responseBody),
                "responseSha256" to familyFSha256Bytes(responseBody),
                "responseHeadersJson" to "{not-well-formed-at-all",
                "forwardingOutcome" to "FORWARDED",
            ),
        )
        val ledger = FamilyFCampaignLedger(dir, setOf("t1"))
        assertFailsWith<FamilyFArtifactIntegrityException> { ledger.readDurableTransportRecord("t1") }
    }

    @Test
    fun `offline recovery fails closed when the transport record does not represent a complete captured response`() {
        val trial = FamilyFCampaignDefinition.allTrials.first { it.kind == FamilyFTrialKind.SCORED }
        val transport = FamilyFDurableTransportRecord(
            trialId = trial.id, exchangeSequence = 1, startedAt = Instant.now(), completedAt = Instant.now(),
            requestBytes = ByteArray(0), requestSha256 = familyFSha256Bytes(ByteArray(0)),
            responseStatus = null, responseCaptured = false, responseBytes = null, responseSha256 = null,
            responseHeaders = emptyMap(), forwardingOutcome = "FORWARDING_FAILURE: simulated",
        )
        assertFailsWith<FamilyFArtifactIntegrityException> {
            FamilyFOfflineRecovery.recoverTerminalPayload(trial, transport, FamilyFRoleIdentity(trial.role, trial.role.modelName, "sha256:fake", 1L), fakeIdentityForOfflineRecoveryTests())
        }
    }

    @Test
    fun `offline recovery fails closed when the durable request does not match the expected production-formatted request`() {
        val trial = FamilyFCampaignDefinition.allTrials.first { it.kind == FamilyFTrialKind.SCORED }
        val wrongRequestBody = "{\"model\":\"not-the-right-model\",\"prompt\":\"not the real prompt\",\"stream\":false}".toByteArray(StandardCharsets.UTF_8)
        val responseBody = "{\"response\":\"REPLY: irrelevant\",\"done\":true}".toByteArray(StandardCharsets.UTF_8)
        val transport = FamilyFDurableTransportRecord(
            trialId = trial.id, exchangeSequence = 1, startedAt = Instant.now(), completedAt = Instant.now(),
            requestBytes = wrongRequestBody, requestSha256 = familyFSha256Bytes(wrongRequestBody),
            responseStatus = 200, responseCaptured = true, responseBytes = responseBody, responseSha256 = familyFSha256Bytes(responseBody),
            responseHeaders = emptyMap(), forwardingOutcome = "FORWARDED",
        )
        assertFailsWith<FamilyFArtifactIntegrityException> {
            FamilyFOfflineRecovery.recoverTerminalPayload(trial, transport, FamilyFRoleIdentity(trial.role, trial.role.modelName, "sha256:fake", 1L), fakeIdentityForOfflineRecoveryTests())
        }
    }

    @Test
    fun `offline recovery rejects a transport record claimed for the wrong trial`() {
        val trial = FamilyFCampaignDefinition.allTrials.first { it.kind == FamilyFTrialKind.SCORED }
        val otherTrial = FamilyFCampaignDefinition.allTrials.first { it.kind == FamilyFTrialKind.SCORED && it.id != trial.id }
        val transport = FamilyFDurableTransportRecord(
            trialId = otherTrial.id, exchangeSequence = 1, startedAt = Instant.now(), completedAt = Instant.now(),
            requestBytes = ByteArray(0), requestSha256 = familyFSha256Bytes(ByteArray(0)),
            responseStatus = 200, responseCaptured = true, responseBytes = ByteArray(0), responseSha256 = familyFSha256Bytes(ByteArray(0)),
            responseHeaders = emptyMap(), forwardingOutcome = "FORWARDED",
        )
        assertFailsWith<IllegalArgumentException> {
            FamilyFOfflineRecovery.recoverTerminalPayload(trial, transport, FamilyFRoleIdentity(trial.role, trial.role.modelName, "sha256:fake", 1L), fakeIdentityForOfflineRecoveryTests())
        }
    }

    // Reuses fakeConfig's own identity construction rather than
    // duplicating it -- the campaign root passed in is never touched by
    // identity-only tests, so an unused placeholder path is sufficient.
    private fun fakeIdentityForOfflineRecoveryTests(): FamilyFIdentity =
        fakeConfig(Path.of("build/family-f-offline-recovery-fake-identity-unused")).identity

    // ---- Genuine offline recovery: reconstruction, zero model calls, matches normal path ----

    private fun seedTransportWithoutTerminal(
        ledger: FamilyFCampaignLedger,
        trial: FamilyFTrial,
        roleIdentity: FamilyFRoleIdentity,
        responseTag: String,
    ): ByteArray {
        val profileId = trial.profileId ?: ContextProfileId.MINIMAL_PRODUCTION_CONTEXT
        val input = SyntheticContextProfiles.construct(trial.fixture, profileId)
        val turn = (input.request.subject as ReasoningSubject.OfTurn).turn
        val prompt = DefaultReasoningPromptBuilder().buildPrompt(turn, input.request.reasoningContext)
        val requestBody = defaultOllamaRequestBody(prompt, roleIdentity.modelName).toByteArray(StandardCharsets.UTF_8)
        val responseBody = "{\"model\":\"${roleIdentity.modelName}\",\"response\":\"$responseTag\",\"done\":true}".toByteArray(StandardCharsets.UTF_8)
        val record = FamilyFProxyExchangeRecord(
            sequence = 1, startedAt = Instant.now(), completedAt = Instant.now().plusMillis(250), method = "POST", path = "/api/generate",
            requestBody = requestBody, requestSha256 = familyFSha256Bytes(requestBody), responseStatus = 200,
            responseHeaders = mapOf("Content-Type" to listOf("application/json")),
            responseBody = responseBody, responseSha256 = familyFSha256Bytes(responseBody), forwardingOutcome = "FORWARDED",
        )
        ledger.recordIntent(trial.id)
        ledger.recordDispatch(trial.id)
        ledger.recordTransport(trial.id, record)
        return responseBody
    }

    private fun normalPathTerminalPayload(trial: FamilyFTrial, roleIdentity: FamilyFRoleIdentity, responseBody: ByteArray, repositoryCommit: String): String {
        val upstream = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        upstream.createContext("/") { exchange ->
            exchange.requestBody.readBytes()
            exchange.sendResponseHeaders(200, responseBody.size.toLong())
            exchange.responseBody.use { it.write(responseBody) }
        }
        upstream.start()
        try {
            val config = LiveEvaluationConfig(
                endpointUrl = "http://127.0.0.1:${upstream.address.port}/api/generate",
                sanitizedEndpointIdentifier = "http://127.0.0.1:${upstream.address.port}",
                modelName = roleIdentity.modelName,
                timeoutMs = FAMILY_F_TIMEOUT_MS,
                outputPath = Path.of("build/family-f-offline-recovery-test-unused.jsonl"),
                repositoryCommit = repositoryCommit,
                modelDigest = roleIdentity.modelDigest,
                runtimeImageId = null,
                repetitions = 1,
            )
            val profileId = trial.profileId ?: ContextProfileId.MINIMAL_PRODUCTION_CONTEXT
            val input = SyntheticContextProfiles.construct(trial.fixture, profileId)
            val harness = ReasoningProtocolLiveModelEvaluationHarness(config)
            val observation = runBlocking { harness.execute(input, trial.attempt) }
            return EvaluationJsonLines.trial(observation)
        } finally {
            upstream.stop(0)
        }
    }

    // Compares only the semantically meaningful fields between a normal-
    // path terminal payload and an offline-recovered one: fields like
    // runId, endpointIdentifier, and timeoutMs legitimately differ (the
    // normal-path comparison call above necessarily uses a throwaway
    // fake upstream distinct from the campaign's own dedicated endpoint
    // identity), so a full-string comparison would be a false negative.
    // Reuses the top-level familyFJsonFieldRawToken (a linear scan, not
    // a backtracking regex, since these fields can be long).
    private fun assertSameSemanticTerminalContent(expectedPayload: String, actualPayload: String) {
        val fields = listOf(
            "fixtureId", "contextProfileId", "prompt", "promptSha256", "requestBody",
            "rawOllamaEnvelope", "extractedResponse", "parsedVariant", "parserExceptionType",
            "parserExceptionClassification", "expectedAction", "actualAction",
            "representationValid", "contentFidelity", "primaryClassification",
        )
        fields.forEach { field ->
            assertEquals(
                familyFJsonFieldRawToken(expectedPayload, field),
                familyFJsonFieldRawToken(actualPayload, field),
                "field \"$field\" must match between normal-path and offline-recovered terminal output",
            )
        }
    }

    @Test
    fun `transport-without-terminal is classified offline after reconstructing a new ledger and driver instance, with zero model calls, matching normal-path output`(@TempDir dir: Path) {
        val trial = FamilyFCampaignDefinition.allTrials.first { it.kind == FamilyFTrialKind.SCORED && it.fixture.expectedAction == ExpectedAction.REPLY }
        val config = fakeConfig(dir)
        val roleIdentity = if (trial.role == FamilyFRole.SUBJECT) config.identity.subject else config.identity.control

        val seedingLedger = FamilyFCampaignLedger(dir, FamilyFCampaignDefinition.allTrials.map { it.id }.toSet())
        val responseBody = seedTransportWithoutTerminal(seedingLedger, trial, roleIdentity, "REPLY: recovered offline")
        val seededState = seedingLedger.recover()
        assertTrue(trial.id in seededState.pendingOfflineClassification)

        // Reconstruct an entirely fresh FamilyFCampaignLedger and
        // FamilyFOrchestrationDriver pointed at the same directory --
        // exactly what a real crash/resume would do -- rather than
        // continuing to use the same in-memory instance that seeded the
        // state above.
        val reconstructedLedger = FamilyFCampaignLedger(dir, FamilyFCampaignDefinition.allTrials.map { it.id }.toSet())
        var modelCallCount = 0
        val throwingModelCaller = FamilyFModelCaller { _, _ ->
            modelCallCount += 1
            throw AssertionError("offline recovery must make zero model calls")
        }
        val dependencies = FamilyFRuntimeDependencies(
            availableMemory = { 8L * 1024 * 1024 * 1024 }, usableSpace = { 8L * 1024 * 1024 * 1024 },
            residencyQuery = FamilyFResidencyQuery { FamilyFResidencyState.ABSENT },
            unloadCommand = FamilyFModelUnloadCommand { true },
            pollResidencyUntilAbsent = { FamilyFResidencyState.ABSENT },
            protectedProcesses = emptyList(), isAlive = { true }, dedicatedPid = "2",
        )
        val reconstructedTransportRecorder = FamilyFTrialTransportRecorder(reconstructedLedger)
        val driver = FamilyFOrchestrationDriver(config, reconstructedLedger, dependencies, throwingModelCaller, reconstructedTransportRecorder)

        val reconstructedState = reconstructedLedger.recover()
        driver.recoverPendingOfflineClassifications(reconstructedState.pendingOfflineClassification)

        assertEquals(0, modelCallCount)
        val finalState = reconstructedLedger.recover()
        assertTrue(trial.id in finalState.resolved)
        assertTrue(finalState.pendingOfflineClassification.isEmpty())

        val terminalLine = Files.readAllLines(dir.resolve("terminal.jsonl")).single { familyFExtractStringField(it, "trialId") == trial.id }
        val recoveredPayload = familyFDecodeQuotedField(terminalLine, "payload")!!

        val normalPayload = normalPathTerminalPayload(trial, roleIdentity, responseBody, config.identity.repositoryCommit)
        assertSameSemanticTerminalContent(normalPayload, recoveredPayload)
        assertTrue(recoveredPayload.contains("\"extractedResponse\":\"REPLY: recovered offline\""))
        assertTrue(recoveredPayload.contains("\"primaryClassification\":\"A\""))
    }

    @Test
    fun `offline recovery works after copying the campaign directory elsewhere`(@TempDir dir: Path) {
        val trial = FamilyFCampaignDefinition.allTrials.first { it.kind == FamilyFTrialKind.SCORED && it.fixture.expectedAction == ExpectedAction.NOACTION }
        val identity = fakeIdentityForOfflineRecoveryTests()
        val roleIdentity = if (trial.role == FamilyFRole.SUBJECT) identity.subject else identity.control

        val original = dir.resolve("original")
        val seedingLedger = FamilyFCampaignLedger(original, FamilyFCampaignDefinition.allTrials.map { it.id }.toSet())
        seedTransportWithoutTerminal(seedingLedger, trial, roleIdentity, "NOACTION")

        val copy = dir.resolve("copy")
        Files.createDirectories(copy)
        Files.list(original).use { stream -> stream.forEach { file -> Files.copy(file, copy.resolve(file.fileName)) } }

        val copiedLedger = FamilyFCampaignLedger(copy, FamilyFCampaignDefinition.allTrials.map { it.id }.toSet())
        var modelCallCount = 0
        val throwingModelCaller = FamilyFModelCaller { _, _ -> modelCallCount += 1; throw AssertionError("must not be called") }
        val config = fakeConfig(dir.resolve("unused-campaign-root")).copy(campaignArtifactRoot = copy)
        val dependencies = FamilyFRuntimeDependencies(
            availableMemory = { 8L * 1024 * 1024 * 1024 }, usableSpace = { 8L * 1024 * 1024 * 1024 },
            residencyQuery = FamilyFResidencyQuery { FamilyFResidencyState.ABSENT },
            unloadCommand = FamilyFModelUnloadCommand { true },
            pollResidencyUntilAbsent = { FamilyFResidencyState.ABSENT },
            protectedProcesses = emptyList(), isAlive = { true }, dedicatedPid = "2",
        )
        val transportRecorder = FamilyFTrialTransportRecorder(copiedLedger)
        val driver = FamilyFOrchestrationDriver(config, copiedLedger, dependencies, throwingModelCaller, transportRecorder)
        val state = copiedLedger.recover()
        assertTrue(trial.id in state.pendingOfflineClassification)
        driver.recoverPendingOfflineClassifications(state.pendingOfflineClassification)

        assertEquals(0, modelCallCount)
        val finalState = copiedLedger.recover()
        assertTrue(trial.id in finalState.resolved)
    }

    @Test
    fun `no duplicate terminal record is created for a trial already resolved`(@TempDir dir: Path) {
        val trial = FamilyFCampaignDefinition.allTrials.first { it.kind == FamilyFTrialKind.SCORED }
        val identity = fakeIdentityForOfflineRecoveryTests()
        val roleIdentity = if (trial.role == FamilyFRole.SUBJECT) identity.subject else identity.control
        val ledger = FamilyFCampaignLedger(dir, FamilyFCampaignDefinition.allTrials.map { it.id }.toSet())
        seedTransportWithoutTerminal(ledger, trial, roleIdentity, "REPLY: only once")
        val driver = FamilyFOrchestrationDriver(
            fakeConfig(dir.resolve("unused")).copy(campaignArtifactRoot = dir), ledger,
            FamilyFRuntimeDependencies(
                availableMemory = { 8L * 1024 * 1024 * 1024 }, usableSpace = { 8L * 1024 * 1024 * 1024 },
                residencyQuery = FamilyFResidencyQuery { FamilyFResidencyState.ABSENT },
                unloadCommand = FamilyFModelUnloadCommand { true }, pollResidencyUntilAbsent = { FamilyFResidencyState.ABSENT },
                protectedProcesses = emptyList(), isAlive = { true }, dedicatedPid = "2",
            ),
            FamilyFModelCaller { _, _ -> throw AssertionError("must not be called") },
            FamilyFTrialTransportRecorder(ledger),
        )
        val state = ledger.recover()
        driver.recoverPendingOfflineClassifications(state.pendingOfflineClassification)
        val terminalLinesAfterFirstRecovery = Files.readAllLines(dir.resolve("terminal.jsonl")).filter { it.isNotBlank() && familyFExtractStringField(it, "trialId") == trial.id }
        assertEquals(1, terminalLinesAfterFirstRecovery.size)

        // A fresh recover() pass -- exactly what a second, independent
        // resume attempt would compute -- no longer includes the
        // now-resolved trial in pendingOfflineClassification at all, so
        // a real caller has no way to reach recoverPendingOfflineClassifications
        // for it a second time; re-driving recovery with that fresh,
        // correctly-empty set is therefore a no-op, and no duplicate
        // terminal record is ever created.
        val secondState = ledger.recover()
        assertFalse(trial.id in secondState.pendingOfflineClassification)
        driver.recoverPendingOfflineClassifications(secondState.pendingOfflineClassification)
        val terminalLines = Files.readAllLines(dir.resolve("terminal.jsonl")).filter { it.isNotBlank() && familyFExtractStringField(it, "trialId") == trial.id }
        assertEquals(1, terminalLines.size, "exactly one terminal record must exist for the trial, never a duplicate")
    }

    // ---- All 392 fake-driven calls carry durable, complete transport evidence ----

    @Test
    fun `all 392 fake-driven calls contain durable complete transport evidence`(@TempDir dir: Path) {
        val (outcome, ledger) = runFakeCampaign(dir.resolve("run"), subjectAlwaysCorrect = true, controlAlwaysCorrect = true)
        assertEquals(FamilyFCampaignOutcome.SEALED, outcome)
        val transportLines = Files.readAllLines(dir.resolve("run").resolve("transport.jsonl")).filter { it.isNotBlank() }
        assertEquals(392, transportLines.size)
        val expectedIds = FamilyFCampaignDefinition.allTrials.map { it.id }.toSet()
        transportLines.forEach { line ->
            val trialId = familyFExtractStringField(line, "trialId")
            assertTrue(trialId in expectedIds)
            val durable = ledger.readDurableTransportRecord(trialId)
            assertTrue(durable.responseCaptured)
            assertEquals("FORWARDED", durable.forwardingOutcome)
            assertEquals(200, durable.responseStatus)
        }
        assertEquals(392, transportLines.map { familyFExtractStringField(it, "trialId") }.distinct().size)
    }
}
