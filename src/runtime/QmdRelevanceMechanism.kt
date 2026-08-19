package parker.core.runtime

import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import parker.core.interfaces.RelevanceCandidateToken
import parker.core.interfaces.RelevanceMechanism
import parker.core.interfaces.RelevanceRequest
import parker.core.interfaces.RelevanceResult

/**
 * Programme 3, Unit 9.7.3 (Local Relevance Mechanism Adapter). The
 * concrete, production [RelevanceMechanism] implementation selected by
 * the Section 13/13.1 mechanism-selection spike
 * (`docs/reviews/PROGRAMME_3_UNIT_9_7_SECTION_13_MECHANISM_SELECTION_SPIKE_EVIDENCE_RECORD.md`,
 * adopted at commit `2a257956b01c93ccbadacfdc4918efe5eb872eeb`): QMD, used
 * strictly as a subordinate, local, request-scoped semantic retrieval
 * mechanism.
 *
 * **Boundary discipline (Contract and Permission Successor §3, §4;
 * Frozen Boundaries #6, #9, #10, #11).** This class declares exactly two
 * constructor dependencies -- [configuration] (an immutable value type)
 * and [invoker] (the entire subprocess-call seam, mirroring
 * `ModelInferenceClient.kt`'s own "the entire model-call seam" precedent)
 * -- and holds no other property. Neither type is, or carries, a
 * `KnowledgeItemPersistence`, `MemoryRetrieval`, `MemoryCore`,
 * `PermissionEngine`, or `KnowledgeStore` reference: architectural
 * inability to reach canonical Parker state is achieved by the same
 * omission technique `DefaultKnowledgeRetrieval` and
 * `RelevanceMechanism` itself already establish elsewhere in this
 * codebase, not by policy or comment (see
 * `tests/runtime/QmdRelevanceMechanismTest.kt`'s own "Contract / boundary"
 * structural proof, section A). [rank] reads only [RelevanceRequest.queryText] and
 * each candidate's [RelevanceCandidateToken]/`content` pair -- never a
 * `KnowledgeId` or any other canonical Parker identifier -- and returns
 * only a [RelevanceResult] built exclusively from the exact tokens
 * [RelevanceRequest] supplied; no content, and no similarity score, ever
 * crosses back out of this class (Successor §3 conditions 4, 6, 7, 8;
 * Frozen Boundary #6).
 *
 * **Disposable state (Frozen Boundary #10).** This class declares no
 * `var`, no class-level cache, and no cross-invocation token map: every
 * [rank] call builds its own request, spawns its own subprocess (via
 * [invoker]), and discards every intermediate value -- including the raw
 * similarity scores the bridge script computes -- once that one call
 * returns or throws. The bridge script itself
 * (`tools/qmd-relevance-bridge.mts`) loads QMD's embedding model fresh
 * inside its own short-lived process and disposes it before exiting; no
 * SQLite file, vector index, or other durable state is created anywhere
 * in this path (contrast with QMD's own `store`/`searchVector` API, which
 * this Unit deliberately does not use -- see the bridge script's own
 * header comment, and Unit 9.7.3 Implication #1 of the spike evidence
 * record).
 *
 * **Local/operator-controlled (Frozen Boundary #11; Successor §3
 * condition 13).** [invoker] never opens a network socket; it launches a
 * local executable ([QmdRelevanceMechanismConfiguration.nodeExecutablePath])
 * against a local script
 * ([QmdRelevanceMechanismConfiguration.bridgeScriptPath]), both explicit,
 * required construction-time values with no implicit default -- mirroring
 * `LocalHttpModelInferenceClient`'s own "a caller must state its own
 * actual local endpoint explicitly" precedent. No candidate content or
 * query text is transmitted anywhere except into that one local
 * subprocess's own stdin file argument.
 *
 * **Local-control precondition, hardened (Windows Verification /
 * Local-Control Hardening Review, Finding 1).** Fresh inspection found
 * that QMD's own `LlamaCpp.resolveModel` calls `node-llama-cpp`'s
 * `resolveModelFile` with its default `download: "auto"` behaviour,
 * meaning a missing embedding model would silently trigger a real
 * network download the first time `embed()` ran -- and that
 * `LLM.modelExists()` cannot catch this in advance, since it
 * unconditionally reports `hf:`-style URIs (this mechanism's own
 * configured model identity) as existing without ever touching the
 * filesystem. `tools/qmd-relevance-bridge.mts` now calls
 * `node-llama-cpp`'s `resolveModelFile` directly with `download: false`
 * *before* constructing `LlamaCpp` or calling `embed()` at all -- a
 * local-filesystem-only check (verified against that function's own
 * implementation) that throws a diagnosable error, surfaced here through
 * the ordinary non-zero-exit path, rather than ever reaching a code path
 * capable of a live download. See that script's own header comment for
 * the full account.
 *
 * **Frozen mechanism identity/version/configuration (Successor §3
 * condition 14; Implementation Plan §12).** [configuration] is an
 * immutable [QmdRelevanceMechanismConfiguration] value, constructed once
 * by Unit 9.7.5's own composition wiring (out of this Unit's scope) and
 * never mutated afterward; a changed value is a disclosed, governed
 * reconfiguration -- never silent drift -- per this same value's own
 * documentation.
 *
 * **Determinism and tie-breaking (Implementation Plan §12; this task's
 * own Phase 2 item 8).** The bridge script deliberately returns an
 * unordered `{token, score}` pair per supplied candidate -- see
 * `tools/qmd-relevance-bridge.mts`'s own header comment for why ordering
 * is not the subprocess's responsibility -- and this class alone performs
 * the final ordering: a stable sort by descending `score`, ties broken by
 * strictly ascending original-supplied-candidate order (never by map
 * iteration order or incidental subprocess/JSON output order). This
 * guarantees identical output ordering for identical input regardless of
 * V8/Node's own sort-stability guarantees, JSON key ordering, or process
 * scheduling.
 *
 * **Failure semantics (Successor §6; this task's own Phase 2 item 9).**
 * Every fault path below propagates as a thrown, distinguishable
 * exception from [rank] -- exactly this codebase's own established
 * "faults propagate unchanged, never absorbed" discipline -- and is never
 * converted into a successful-looking, possibly-empty [RelevanceResult]:
 * subprocess start failure, timeout, non-zero exit, malformed JSON, a
 * missing/extra required field, an unknown returned token, a duplicate
 * returned token, more tokens returned than were supplied, a partial
 * (incomplete) scored set, and an entry carrying an unexpected extra
 * field (e.g. `content`, where token-only output is required) are all
 * detected explicitly and each throws with a distinct, diagnosable
 * message. The sole non-failure path that still avoids launching a
 * subprocess at all is [RelevanceRequest.candidates] being empty, which
 * returns `RelevanceResult(emptyList())` immediately -- a genuine,
 * successful empty result, never itself a failure (Phase 2 item 10).
 *
 * **Known, reviewed, non-blocking operational limitation (Windows
 * Verification / Local-Control Hardening Review, Finding 2).** [rank]'s
 * `withContext(Dispatchers.IO) { invoker.invoke(...) }` wraps a plain
 * blocking call; external coroutine cancellation of the calling
 * coroutine does not itself interrupt an in-flight subprocess wait
 * before [QmdRelevanceMechanismConfiguration.timeoutMillis] elapses on
 * its own. This was checked, deliberately, against every adopted Unit
 * 9.7.3 requirement -- the Implementation Plan, the Contract and
 * Permission Successor, the Frozen Boundaries, and RelevanceMechanism's
 * own contract -- and none require prompt cancellation responsiveness;
 * `suspend` is required only so a mechanism *may* perform in-process
 * computation, never as a cancellation-latency guarantee. No governed
 * property is violated, so no production code was changed for this
 * finding: it is recorded here, deliberately, as a disclosed opportunity
 * for later operational hardening (e.g. registering
 * `invokeOnCancellation` to forcibly destroy the child process), not as
 * a defect blocking this Unit's acceptance.
 */
class QmdRelevanceMechanism(
    private val configuration: QmdRelevanceMechanismConfiguration,
    private val invoker: QmdSubprocessInvoker,
) : RelevanceMechanism {

    override suspend fun rank(request: RelevanceRequest): RelevanceResult {
        if (request.candidates.isEmpty()) {
            // Phase 2 item 10: zero candidates -> successful empty result,
            // without launching QMD at all.
            return RelevanceResult(rankedTokens = emptyList())
        }

        val requestJson = buildRequestJson(request)
        val invocation = withContext(Dispatchers.IO) {
            invoker.invoke(requestJson, configuration.timeoutMillis)
        }

        check(!invocation.timedOut) {
            "QMD relevance mechanism timed out after ${configuration.timeoutMillis}ms waiting for " +
                "the bridge process to complete"
        }
        check(invocation.exitCode == 0) {
            "QMD relevance mechanism bridge process exited with code ${invocation.exitCode}: " +
                invocation.stderr.ifBlank { "(no stderr output)" }
        }

        val parsedScores = parseScoresResponse(invocation.stdout)

        val suppliedTokens = request.candidates.map { it.token.value }
        val suppliedTokenSet = suppliedTokens.toSet()
        val originalIndex = suppliedTokens.withIndex().associate { (index, token) -> token to index }

        val seenTokens = mutableSetOf<String>()
        for (parsed in parsedScores) {
            check(parsed.token in suppliedTokenSet) {
                "QMD relevance mechanism returned an unknown token not present in the supplied " +
                    "candidate set: ${parsed.token}"
            }
            check(seenTokens.add(parsed.token)) {
                "QMD relevance mechanism returned a duplicate token: ${parsed.token}"
            }
        }
        check(parsedScores.size <= suppliedTokens.size) {
            "QMD relevance mechanism returned more scored tokens (${parsedScores.size}) than were " +
                "supplied (${suppliedTokens.size})"
        }
        check(parsedScores.size == suppliedTokens.size) {
            "QMD relevance mechanism returned ${parsedScores.size} scored token(s) but " +
                "${suppliedTokens.size} were supplied -- a partial result is treated as malformed, " +
                "never silently accepted"
        }

        val ranked = parsedScores
            .sortedWith(
                compareByDescending<ParsedCandidateScore> { it.score }
                    .thenBy { originalIndex.getValue(it.token) },
            )
            .map { RelevanceCandidateToken(it.token) }

        return RelevanceResult(rankedTokens = ranked)
    }

    private fun buildRequestJson(request: RelevanceRequest): String {
        val candidatesJson = request.candidates.joinToString(",", prefix = "[", postfix = "]") { candidate ->
            """{"token":${jsonString(candidate.token.value)},"content":${jsonString(candidate.content)}}"""
        }
        return buildString {
            append('{')
            append(""""protocolVersion":${jsonString(configuration.bridgeProtocolVersion)},""")
            append(""""queryText":${jsonString(request.queryText)},""")
            append(""""candidates":$candidatesJson,""")
            append(""""embeddingModelUri":${jsonString(configuration.embeddingModelUri)}""")
            if (configuration.modelCacheDir != null) {
                append(""","modelCacheDir":${jsonString(configuration.modelCacheDir)}""")
            }
            if (configuration.qmdSourceRoot != null) {
                append(""","qmdSourceRoot":${jsonString(configuration.qmdSourceRoot)}""")
            }
            append('}')
        }
    }
}

/**
 * Frozen, disclosed mechanism identity/version/configuration (Successor
 * §3 condition 14; Implementation Plan §12; spike evidence record §17
 * Implication #2, "platform-native binary provisioning must be pinned and
 * confirmed per deployment target"). An immutable `data class`; a changed
 * value anywhere in a running Parker instance can only occur through a
 * disclosed, governed reconstruction of a new [QmdRelevanceMechanism] by
 * Unit 9.7.5's own composition wiring, never through mutation of an
 * existing instance -- there is no setter anywhere on this type.
 *
 * [nodeExecutablePath] and [bridgeScriptPath] carry no default, mirroring
 * `LocalHttpModelInferenceClient`'s own "a caller must state its own
 * actual local endpoint explicitly" precedent -- Unit 9.7.3 does not
 * assume a `node` executable is on `PATH`, nor that
 * `tools/qmd-relevance-bridge.mts` lives at any particular absolute path
 * on a given deployment target.
 *
 * [additionalNodeArguments] exists because `tools/qmd-relevance-bridge.mts`
 * imports QMD's own TypeScript source (`.../qmd/src/llm.ts`, resolved from
 * [qmdSourceRoot] below) directly, which plain `node` cannot execute without
 * a TypeScript-capable loader -- the pre-existing experimental precedent
 * (`tests/composition/QmdCanonicalMemoryRetrievalExperimentTest.kt`)
 * resolves this the same way, by inserting `tsx`'s own CLI entry point as
 * an argument before the target script (`node .../tsx/dist/cli.mjs
 * <script> <args>`), not by changing what `node` itself is. This field is
 * where that same, disclosed, genuinely-required runtime dependency is
 * pinned for Unit 9.7.3's own production bridge, rather than being
 * silently hard-coded inside [ProcessBuilderQmdSubprocessInvoker].
 *
 * [qmdSourceRoot] (Main-Promotion Gate / Production QMD Bridge Portability
 * Correction). The local QMD installation/checkout root -- e.g.
 * `C:\Projects\Parker\qmd` on Steve's own Windows development machine, or
 * whatever equivalent local path a Linux deployment provides -- that
 * `tools/qmd-relevance-bridge.mts` resolves its own `src/llm.ts` and
 * `node_modules/node-llama-cpp/dist/index.js` imports from, dynamically,
 * at the script's own runtime (a static ESM `import ... from "..."` cannot
 * accept a runtime variable; see that script's own header comment for the
 * full account). Before this correction, both of those paths were
 * hard-coded, absolute, Steve-specific Windows literals inside the bridge
 * script itself -- a genuine production portability defect, unrelated to
 * and discovered after Unit 9.7's own closure. Deployment-specific, like
 * [nodeExecutablePath]/[bridgeScriptPath]/[additionalNodeArguments]/
 * [modelCacheDir] above -- never semantic mechanism identity (that remains
 * exactly [mechanismName], [qmdVersion], [embeddingModelUri],
 * [vectorDimension], [similarityMetric], unaffected by this field).
 * Nullable, defaulting to `null`, mirroring [additionalNodeArguments]'s own
 * `tsx` path and [modelCacheDir]'s own reasoning: this composition root
 * does not, and must not, guess or hard-code a QMD installation location.
 * When left unset, the mechanism still constructs successfully, but the
 * real bridge subprocess fails loudly and diagnosably the first time
 * [rank] is genuinely invoked (`tools/qmd-relevance-bridge.mts`'s own
 * "missing or empty qmdSourceRoot" fail-closed check), never a silent
 * empty result and never an on-demand fallback to any default location.
 *
 * [embeddingModelFileSha256], [modelCacheDir], and [qmdSourceRoot] are the
 * fields this configuration cannot always populate confidently in every
 * environment (Implication #2's own disclosed platform-binary caveat) and
 * are therefore nullable, defaulting to `null` -- never silently inferred
 * from mutable environment state that could affect retrieval behaviour
 * (this task's own Phase 2 item 7, "Do not silently infer configuration
 * from mutable environment state if it affects retrieval behaviour").
 */
data class QmdRelevanceMechanismConfiguration(
    val mechanismName: String = "QMD",
    val qmdVersion: String,
    val embeddingModelUri: String,
    val embeddingModelFileSha256: String? = null,
    val vectorDimension: Int,
    val similarityMetric: String = "cosine",
    val bridgeProtocolVersion: String = "1",
    val nodeExecutablePath: String,
    val bridgeScriptPath: String,
    val modelCacheDir: String? = null,
    val additionalNodeArguments: List<String> = emptyList(),
    val timeoutMillis: Long = 30_000,
    val qmdSourceRoot: String? = null,
) {
    init {
        require(mechanismName.isNotBlank()) { "QmdRelevanceMechanismConfiguration.mechanismName must not be blank" }
        require(qmdVersion.isNotBlank()) { "QmdRelevanceMechanismConfiguration.qmdVersion must not be blank" }
        require(embeddingModelUri.isNotBlank()) { "QmdRelevanceMechanismConfiguration.embeddingModelUri must not be blank" }
        require(vectorDimension > 0) { "QmdRelevanceMechanismConfiguration.vectorDimension must be positive" }
        require(similarityMetric.isNotBlank()) { "QmdRelevanceMechanismConfiguration.similarityMetric must not be blank" }
        require(bridgeProtocolVersion.isNotBlank()) { "QmdRelevanceMechanismConfiguration.bridgeProtocolVersion must not be blank" }
        require(nodeExecutablePath.isNotBlank()) { "QmdRelevanceMechanismConfiguration.nodeExecutablePath must not be blank" }
        require(bridgeScriptPath.isNotBlank()) { "QmdRelevanceMechanismConfiguration.bridgeScriptPath must not be blank" }
        require(timeoutMillis > 0) { "QmdRelevanceMechanismConfiguration.timeoutMillis must be positive" }
        require(qmdSourceRoot == null || qmdSourceRoot.isNotBlank()) {
            "QmdRelevanceMechanismConfiguration.qmdSourceRoot must not be blank when supplied -- use null, not \"\", to leave it unset"
        }
    }
}

/**
 * The raw outcome of one subprocess invocation, before [QmdRelevanceMechanism]
 * interprets it. Deliberately dumb: no JSON parsing, no interpretation of
 * [exitCode] happens here -- this type only reports what the operating
 * system reported.
 */
data class QmdSubprocessInvocationResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean,
)

/**
 * The entire subprocess-call seam -- mirroring `ModelInferenceClient`'s
 * own "the entire model-call seam" precedent
 * (`src/runtime/ModelInferenceClient.kt`). [QmdRelevanceMechanism] never
 * knows or cares what is behind this interface: a real
 * [ProcessBuilderQmdSubprocessInvoker] in production, or a fake, in-memory
 * double in this Unit's own pure protocol/validation tests (Phase 6,
 * `tests/runtime/QmdRelevanceMechanismTest.kt`) that never spawns a real
 * process and never depends on QMD's own native binaries being present.
 */
fun interface QmdSubprocessInvoker {
    fun invoke(requestJson: String, timeoutMillis: Long): QmdSubprocessInvocationResult
}

/**
 * Concrete, production [QmdSubprocessInvoker]. Writes [requestJson] to a
 * fresh temporary file (deleted in every path, success or failure --
 * Frozen Boundary #10's disposability applies to this class's own
 * on-disk footprint too, not only to QMD's internal state), launches
 * [nodeExecutablePath] against [bridgeScriptPath] with that file's path
 * as the sole argument, and drains stdout/stderr concurrently on daemon
 * threads while waiting -- avoiding the classic `ProcessBuilder`
 * pipe-buffer deadlock the codebase's own pre-existing experimental
 * precedent (`RealQmdAuthorizedVectorProcessBridge` in
 * `tests/composition/QmdCanonicalMemoryRetrievalExperimentTest.kt`, which
 * reads streams only after an untimed `waitFor()`) does not itself guard
 * against; this Unit's own version both times out and reads
 * concurrently.
 *
 * Never throws: process-start failure is reported as a distinguished
 * `QmdSubprocessInvocationResult(exitCode = -1, ...)` rather than an
 * exception escaping [invoke] itself, so [QmdRelevanceMechanism.rank]
 * alone owns deciding how each fault surfaces (via `check`, per this
 * codebase's own "faults propagate as thrown exceptions" discipline
 * applied at the boundary that actually understands the domain meaning
 * of the fault).
 *
 * Reads [QmdRelevanceMechanismConfiguration.nodeExecutablePath],
 * [QmdRelevanceMechanismConfiguration.additionalNodeArguments], and
 * [QmdRelevanceMechanismConfiguration.bridgeScriptPath] from the same
 * single [configuration] instance [QmdRelevanceMechanism] itself holds --
 * deliberately not a second, separately-supplied copy of those same
 * values -- so there is exactly one frozen source of truth for where the
 * bridge process actually launches from, never two values that could
 * silently drift apart.
 */
class ProcessBuilderQmdSubprocessInvoker(
    private val configuration: QmdRelevanceMechanismConfiguration,
) : QmdSubprocessInvoker {

    override fun invoke(requestJson: String, timeoutMillis: Long): QmdSubprocessInvocationResult {
        val requestFile = Files.createTempFile("parker-qmd-relevance-request-", ".json")
        try {
            Files.writeString(requestFile, requestJson)

            val command = listOf(configuration.nodeExecutablePath) +
                configuration.additionalNodeArguments +
                listOf(configuration.bridgeScriptPath, requestFile.toAbsolutePath().toString())

            val process = try {
                ProcessBuilder(command).start()
            } catch (error: IOException) {
                return QmdSubprocessInvocationResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = "QMD relevance bridge process failed to start: ${error.message}",
                    timedOut = false,
                )
            }

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()
            val stdoutThread = Thread {
                process.inputStream.bufferedReader().forEachLine { stdoutBuilder.append(it).append('\n') }
            }.apply { isDaemon = true }
            val stderrThread = Thread {
                process.errorStream.bufferedReader().forEachLine { stderrBuilder.append(it).append('\n') }
            }.apply { isDaemon = true }
            stdoutThread.start()
            stderrThread.start()

            val finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                stdoutThread.join(1_000)
                stderrThread.join(1_000)
                return QmdSubprocessInvocationResult(
                    exitCode = -1,
                    stdout = stdoutBuilder.toString(),
                    stderr = stderrBuilder.toString(),
                    timedOut = true,
                )
            }

            stdoutThread.join(5_000)
            stderrThread.join(5_000)
            return QmdSubprocessInvocationResult(
                exitCode = process.exitValue(),
                stdout = stdoutBuilder.toString(),
                stderr = stderrBuilder.toString(),
                timedOut = false,
            )
        } finally {
            Files.deleteIfExists(requestFile)
        }
    }
}

private data class ParsedCandidateScore(val token: String, val score: Double)

private val JSON_FIELD_NAME_PATTERN = Regex("\"([A-Za-z0-9_]+)\"\\s*:")
private val JSON_TOKEN_VALUE_PATTERN = Regex("\"token\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
private val JSON_SCORE_VALUE_PATTERN = Regex("\"score\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?(?:[eE][-+]?[0-9]+)?)")

/**
 * Splits the body of a JSON array of flat (non-nested) objects into its
 * individual `{...}` object substrings, respecting string literals (so a
 * `{` or `}` character inside a quoted string is never mistaken for
 * object-boundary punctuation). Limited exactly to this mechanism's own
 * narrow response shape -- entries are never nested objects -- not a
 * general-purpose JSON tokenizer.
 */
private fun splitTopLevelJsonObjects(arrayBody: String): List<String> {
    val objects = mutableListOf<String>()
    var depth = 0
    var start = -1
    var inString = false
    var index = 0
    while (index < arrayBody.length) {
        val character = arrayBody[index]
        if (inString) {
            if (character == '\\') {
                index += 2
                continue
            }
            if (character == '"') inString = false
            index += 1
            continue
        }
        when (character) {
            '"' -> inString = true
            '{' -> {
                if (depth == 0) start = index
                depth += 1
            }
            '}' -> {
                depth -= 1
                if (depth == 0 && start >= 0) {
                    objects.add(arrayBody.substring(start, index + 1))
                    start = -1
                }
            }
        }
        index += 1
    }
    return objects
}

/**
 * Minimal, hand-rolled JSON parser -- limited exactly to this mechanism's
 * own narrow response protocol shape
 * (`{"scores":[{"token":"...","score":...}, ...]}`), not a general-purpose
 * JSON parser, mirroring `ModelInferenceClient.kt`'s own
 * `defaultOllamaResponseBody`'s "minimal, hand-rolled" precedent for this
 * codebase. Deliberately strict, rather than permissive: each entry's own
 * declared field-name set is compared against exactly `{token, score}`
 * (order-independent) -- an entry missing either field, or carrying any
 * additional field (for example, a bridge defect that also returns
 * `content`), is rejected as malformed with a distinct, diagnosable
 * message (this task's own Phase 6-D "missing required field" and
 * "content returned where token-only output is required").
 */
private fun parseScoresResponse(rawJson: String): List<ParsedCandidateScore> {
    val trimmed = rawJson.trim()
    require(trimmed.startsWith("{") && trimmed.endsWith("}")) {
        "malformed QMD relevance bridge response: not a JSON object: $rawJson"
    }

    val key = "\"scores\""
    val keyIndex = trimmed.indexOf(key)
    require(keyIndex >= 0) {
        "malformed QMD relevance bridge response: missing \"scores\" field: $rawJson"
    }

    val arrayStart = trimmed.indexOf('[', keyIndex)
    require(arrayStart >= 0) {
        "malformed QMD relevance bridge response: \"scores\" value is not an array: $rawJson"
    }
    val arrayEnd = trimmed.lastIndexOf(']')
    require(arrayEnd > arrayStart) {
        "malformed QMD relevance bridge response: unterminated scores array: $rawJson"
    }

    val arrayBody = trimmed.substring(arrayStart + 1, arrayEnd).trim()
    if (arrayBody.isEmpty()) return emptyList()

    val objects = splitTopLevelJsonObjects(arrayBody)
    require(objects.isNotEmpty()) {
        "malformed QMD relevance bridge response: no entries found in scores array: $rawJson"
    }

    return objects.map { entry ->
        val fieldNames = JSON_FIELD_NAME_PATTERN.findAll(entry).map { it.groupValues[1] }.toList()
        require(fieldNames.toSet() == setOf("token", "score")) {
            "malformed QMD relevance bridge response: entry declares field(s) $fieldNames, but " +
                "exactly {token, score} are required -- a required field may be missing, or an " +
                "unexpected extra field may be present (e.g. content, where token-only output is " +
                "required): $entry"
        }

        val tokenMatch = JSON_TOKEN_VALUE_PATTERN.find(entry)
            ?: throw IllegalArgumentException(
                "malformed QMD relevance bridge response: entry missing a well-formed token value: $entry",
            )
        val scoreMatch = JSON_SCORE_VALUE_PATTERN.find(entry)
            ?: throw IllegalArgumentException(
                "malformed QMD relevance bridge response: entry missing a well-formed score value: $entry",
            )
        val score = scoreMatch.groupValues[1].toDoubleOrNull()
            ?: throw IllegalArgumentException(
                "malformed QMD relevance bridge response: non-numeric score: $entry",
            )

        ParsedCandidateScore(token = unescapeJsonString(tokenMatch.groupValues[1]), score = score)
    }
}

private fun unescapeJsonString(value: String): String {
    val builder = StringBuilder(value.length)
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (character == '\\' && index + 1 < value.length) {
            when (value[index + 1]) {
                '\\' -> builder.append('\\')
                '"' -> builder.append('"')
                'n' -> builder.append('\n')
                'r' -> builder.append('\r')
                't' -> builder.append('\t')
                else -> builder.append(value[index + 1])
            }
            index += 2
        } else {
            builder.append(character)
            index += 1
        }
    }
    return builder.toString()
}

/**
 * Minimal, hand-rolled JSON string escaping -- the same five sequences
 * `ModelInferenceClient.kt`'s own `jsonEscape` already establishes as this
 * codebase's convention for hand-rolled JSON string production, plus a
 * generic control-character escape for defense-in-depth (candidate
 * content is authorised, Parker-supplied text, not attacker input, but
 * this keeps the produced JSON well-formed even for content containing an
 * unexpected control character).
 */
private fun jsonString(value: String): String = buildString {
    append('"')
    for (character in value) {
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character.code < 0x20) {
                append("\\u")
                append(character.code.toString(16).padStart(4, '0'))
            } else {
                append(character)
            }
        }
    }
    append('"')
}
