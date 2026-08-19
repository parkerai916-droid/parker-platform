// Programme 3, Unit 9.7.3 (Local Relevance Mechanism Adapter) -- production
// bridge script. Invoked by `QmdRelevanceMechanism`
// (`src/runtime/QmdRelevanceMechanism.kt`) as a fresh, disposable child
// process for exactly one `RelevanceMechanism.rank` call; nothing this
// script creates survives past its own single invocation.
//
// Deliberately does NOT use QMD's own `createStore`/`searchVector` API
// (contrast with the pre-existing experimental precedent,
// `tests/composition/qmd-authorized-vector-bridge.mts`): that API is
// index/store-centric (a SQLite file, `sqlite-vec`'s native `vec0`
// extension, an `INSERT`-then-`searchVec` flow) and risks drifting toward
// a persistent secondary semantic index, which Frozen Boundary #10 and
// Successor §3 condition 10 forbid. This script instead calls QMD's own
// `LLM.embed()` directly -- the same underlying embedding computation
// `store.searchVector` itself calls internally -- and computes cosine
// similarity in plain JavaScript, entirely in-process, with no SQLite
// file and no `sqlite-vec` native dependency at all. This also sidesteps
// the `sqlite-vec-linux-x64` native-binary gap the Section 13/13.1 spike
// disclosed for non-Windows execution environments (spike evidence record
// §5); this script's only native dependency is `node-llama-cpp` itself,
// needed for embedding computation regardless of which QMD API is used.
//
// Applies the exact same query/document formatting QMD's own
// `store.ts` applies before every real embedding call
// (`formatQueryForEmbedding`/`formatDocForEmbedding`, imported directly
// from QMD's own `llm.ts` -- see that file's own doc comments: "Uses
// nomic-style task prefix format for embeddinggemma (default)"), so this
// script's embeddings are computed the same way QMD's own production
// `searchVector` path computes them, not a divergent, unformatted
// embedding this script invented.
//
// Deliberately returns `{"scores": [{"token": "...", "score": ...}, ...]}`
// -- one entry per supplied candidate, in no particular order -- rather
// than a pre-sorted `rankedTokens` array. Final ordering (a stable sort by
// descending score, ties broken by original supplied-candidate order) is
// `QmdRelevanceMechanism.kt`'s own responsibility, not this script's: the
// governed contract already permits a mechanism's internally-computed
// scores to be "parsed/used only inside the adapter and discarded before
// returning" (this Unit's own task instructions, Phase 2 item 3), and
// keeping the final, governed ordering decision in Kotlin means
// `QmdRelevanceMechanismTest.kt` can prove the tie-breaking rule
// deterministically with a fake invoker, never depending on this script's
// own JSON key order or a particular Node/V8 sort-stability guarantee.
//
// Fails loudly, never partially: if ANY candidate's embedding call
// returns `null` (QMD's own `LLM.embed()` returns `null` on an internal
// embedding failure, per `llm.ts`), this script throws and exits
// non-zero, rather than silently omitting that one candidate's score --
// a partial result presented as complete is exactly what Successor §6's
// fail-closed table forbids.
//
// Disposes its own `LlamaCpp` instance in a `finally` block before
// exiting, so the process can terminate promptly rather than being kept
// alive by node-llama-cpp's own native worker threads.
//
// Local/operator-controlled precondition (Frozen Boundary #11; Unit 9.7.3
// Windows Verification / Local-Control Hardening Review, Finding 1).
// Fresh inspection of `qmd/src/llm.ts` (`LlamaCpp.resolveModel`,
// `ensureEmbedModel`) confirmed that `LlamaCpp`'s own model resolution
// calls `node-llama-cpp`'s `resolveModelFile(modelUri, {directory, cli})`
// -- QMD's own doc comment on `resolveModel` says so explicitly:
// "resolveModelFile handles HF URIs and downloads to the cache dir." That
// call site never sets `download: false`, and `resolveModelFile`'s own
// default is `download: "auto"` (per
// `node-llama-cpp/dist/utils/resolveModelFile.d.ts`) -- meaning a missing
// embedding model would silently trigger an on-demand network download
// the very first time `embed()` is called, an uncontrolled network
// dependency Frozen Boundary #11 forbids. `LLM.modelExists()` cannot
// serve as a substitute pre-flight check for this: for any `hf:`-style
// URI (exactly this mechanism's own configured
// `DEFAULT_EMBED_MODEL_URI`), it unconditionally "assumes they exist"
// and returns `{exists: true}` without ever touching the filesystem
// (`llm.ts`, `modelExists`) -- it would never catch a genuine cache miss.
//
// Live Windows Acceptance Failure / Bounded Cache-Resolution Correction
// (this Unit's own follow-up review): the fix below must check the *same*
// cache directory `LlamaCpp` itself will actually use once construction
// proceeds -- not merely whatever directory happens to be supplied on the
// request. Fresh inspection of `qmd/src/llm.ts`'s `LlamaCpp` constructor
// shows it resolves `this.modelCacheDir = config.modelCacheDir ||
// MODEL_CACHE_DIR` (QMD's own exported `DEFAULT_MODEL_CACHE_DIR`, itself
// `$XDG_CACHE_HOME/qmd/models` or `~/.cache/qmd/models`) when no explicit
// `modelCacheDir` is supplied. `resolveModelFile` has an entirely
// different, unrelated default (node-llama-cpp's own `cliModelsDirectory`,
// `~/.node-llama-cpp/models`) when called with `directory: undefined`.
// Calling `resolveModelFile(modelUri, {directory: modelCacheDir,
// download:false})` with `modelCacheDir` left `undefined` therefore checked
// the *wrong* directory relative to what `LlamaCpp` would actually use --
// a genuine defect, distinct from (and in addition to) whichever caller
// supplies `modelCacheDir` on any individual request. The fix: resolve the
// effective cache directory identically to how `LlamaCpp` itself resolves
// it (`modelCacheDir || DEFAULT_MODEL_CACHE_DIR`) before calling
// `resolveModelFile`, so this pre-flight check and the real `embed()` call
// are always guaranteed to agree on which directory holds the model.
//
// The fix below calls `node-llama-cpp`'s own `resolveModelFile` directly,
// with `download: false`, *before* constructing `LlamaCpp` or calling
// `embed()` at all. Reading `resolveModelFile`'s own implementation
// (`node-llama-cpp/dist/utils/resolveModelFile.js`) confirms this path is
// itself local-only up to the point it would otherwise start a download:
// `resolveModelDestination` only parses the URI string, and
// `findMatchingFilesInDirectory` only scans the local cache directory --
// the network-touching `createModelDownloader` call is reached only
// *after* the `download === false` branch has already returned or
// thrown. With `download: false` and no local match, it throws
// `"...and download is disabled"` -- turning a would-be silent download
// into a diagnosable, fail-closed local error, exactly this Finding's own
// required minimum. `verify: true` is deliberately never set here: per
// the same file's own doc comment, verifying an existing file "matches
// the expected size of the remote file" itself requires reaching the
// remote server -- enabling it would reintroduce the exact network
// dependency this precondition exists to rule out. Per-request file-hash
// verification ([QmdRelevanceMechanismConfiguration.embeddingModelFileSha256](
// ../src/runtime/QmdRelevanceMechanism.kt)'s own optional field) is
// deliberately not added here either: hashing a ~300+MB model file on
// every single `rank()` invocation is a real latency/disk-I/O cost this
// Finding's own "minimum necessary" and "do not widen Unit 9.7.3"
// instructions do not justify; it remains a disclosed, out-of-scope
// hardening opportunity for a later unit, not a defect in this one.

import { readFile, stat } from "node:fs/promises";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// Main-Promotion Gate / Production QMD Bridge Portability Correction
// (Programme 3 Unit 9.7.6 follow-on). This script previously imported its
// two QMD implementation dependencies via static ESM imports pinned to
// Steve's own machine (`file:///C:/Projects/Parker/qmd/src/llm.ts`,
// `file:///C:/Projects/Parker/qmd/node_modules/node-llama-cpp/dist/index.js`)
// -- a genuine portability defect, since a static `import ... from "..."`
// specifier cannot use a runtime variable and this script therefore could
// never run against any other QMD installation location, on any machine.
//
// Corrected shape: both modules are now resolved via `await import(...)`
// (a dynamic import, the only ESM mechanism that accepts a runtime-computed
// specifier) from `qmdSourceRoot` -- a new, required field on the request
// this script already reads from its request-file argument, carrying
// exactly the same deployment-specific meaning
// [QmdRelevanceMechanismConfiguration.qmdSourceRoot] documents on the
// Kotlin side (`src/runtime/QmdRelevanceMechanism.kt`): the local QMD
// installation/checkout root, never a hard-coded developer path, never a
// mutable-environment-derived guess, and never anything semantic-identity-
// relevant (that remains the frozen `embeddingModelUri`/`vectorDimension`/
// `similarityMetric` fields, unchanged by this correction).
//
// `resolveLocalQmdModuleUrl` below builds the final module URL with node's
// own `path.resolve` (platform-correct on both Windows and Linux, since
// `node:path` resolves relative to whichever OS actually runs this script)
// and `url.pathToFileURL` (never manual `file://` string concatenation,
// per this task's own Phase 4 instruction) -- which can only ever produce a
// `file:` URL, structurally ruling out a remote import. `assertLocalQmdSourceRoot`
// additionally rejects any `qmdSourceRoot` value that itself already looks
// like a URL/remote scheme (e.g. `https://...`), so a misconfigured remote
// value fails with a clear, specific diagnostic rather than an obscure
// downstream filesystem error. Both required module files are `stat`-checked
// before import, so a missing/misconfigured QMD installation fails loudly
// and diagnostically -- never silently, and never by attempting a download
// or install of any kind (this script has no such capability at all).

const REMOTE_SCHEME_PATTERN = /^[a-zA-Z][a-zA-Z0-9+.-]*:\/\//;

function assertLocalQmdSourceRoot(qmdSourceRoot: string): void {
  if (REMOTE_SCHEME_PATTERN.test(qmdSourceRoot)) {
    throw new Error(
      `qmdSourceRoot must be a local filesystem path, not a URL or remote scheme: ${JSON.stringify(qmdSourceRoot)}. ` +
        `This bridge only ever imports local files -- no remote QMD source is supported.`,
    );
  }
}

async function resolveLocalQmdModuleUrl(qmdSourceRoot: string, ...pathSegments: string[]): Promise<string> {
  const modulePath = resolve(qmdSourceRoot, ...pathSegments);
  try {
    await stat(modulePath);
  } catch (error) {
    const cause = error instanceof Error ? error.message : String(error);
    throw new Error(
      `required QMD module not found at ${JSON.stringify(modulePath)}, resolved from configured ` +
        `qmdSourceRoot ${JSON.stringify(qmdSourceRoot)}. Configure PARKER_QMD_SOURCE_ROOT (or the ` +
        `equivalent QmdRelevanceMechanismConfiguration.qmdSourceRoot value) to point at a local QMD ` +
        `installation containing this file. This bridge never downloads or installs QMD itself. ` +
        `Underlying error: ${cause}`,
    );
  }
  return pathToFileURL(modulePath).href;
}

type CandidateRequest = {
  token: string;
  content: string;
};

type RelevanceBridgeRequest = {
  protocolVersion: string;
  queryText: string;
  candidates: CandidateRequest[];
  embeddingModelUri: string;
  modelCacheDir?: string;
  qmdSourceRoot: string;
};

const SUPPORTED_PROTOCOL_VERSION = "1";

function cosineSimilarity(a: number[], b: number[]): number {
  if (a.length !== b.length) {
    throw new Error(`embedding dimension mismatch: query=${a.length} candidate=${b.length}`);
  }
  let dot = 0;
  let normA = 0;
  let normB = 0;
  for (let i = 0; i < a.length; i += 1) {
    const x = a[i]!;
    const y = b[i]!;
    dot += x * y;
    normA += x * x;
    normB += y * y;
  }
  return dot / (Math.sqrt(normA) * Math.sqrt(normB));
}

function validateRequest(payload: unknown): asserts payload is RelevanceBridgeRequest {
  if (typeof payload !== "object" || payload === null) {
    throw new Error("request is not a JSON object");
  }
  const request = payload as Record<string, unknown>;
  if (request.protocolVersion !== SUPPORTED_PROTOCOL_VERSION) {
    throw new Error(`unsupported protocolVersion: ${JSON.stringify(request.protocolVersion)}`);
  }
  if (typeof request.queryText !== "string" || request.queryText.length === 0) {
    throw new Error("missing or empty queryText");
  }
  if (!Array.isArray(request.candidates) || request.candidates.length === 0) {
    throw new Error("missing or empty candidates array");
  }
  // Required (not merely optional), because the local-availability
  // pre-flight check below needs an explicit model identity to check --
  // it must never silently fall back to node-llama-cpp's own default
  // model resolution for a request that omitted one.
  if (typeof request.embeddingModelUri !== "string" || request.embeddingModelUri.length === 0) {
    throw new Error("missing or empty embeddingModelUri");
  }
  // Required (Main-Promotion Gate / Production QMD Bridge Portability
  // Correction): this script no longer has any hard-coded or default
  // notion of where QMD's own source/runtime lives -- it must never
  // silently guess a location (e.g. this file's own directory, or a
  // well-known developer path) for a request that omitted one.
  if (typeof request.qmdSourceRoot !== "string" || request.qmdSourceRoot.length === 0) {
    throw new Error("missing or empty qmdSourceRoot");
  }
  for (const candidate of request.candidates) {
    if (
      typeof candidate !== "object" ||
      candidate === null ||
      typeof (candidate as Record<string, unknown>).token !== "string" ||
      typeof (candidate as Record<string, unknown>).content !== "string"
    ) {
      throw new Error(`malformed candidate entry, must be {token: string, content: string}: ${JSON.stringify(candidate)}`);
    }
  }
}

/**
 * Fail-closed local-availability precondition (Frozen Boundary #11; see
 * this file's own header comment, Finding 1). Throws a clear,
 * diagnosable error -- never silently proceeding -- if `modelUri` is not
 * already present under the effective model cache directory. Performs no
 * network access itself: `resolveModelFile`'s own `download: false` path
 * is local-filesystem-only (see header comment).
 *
 * The effective directory is computed identically to how `LlamaCpp`
 * itself resolves its own cache directory (`modelCacheDir ||
 * DEFAULT_MODEL_CACHE_DIR`) -- see this file's own header comment, "Live
 * Windows Acceptance Failure / Bounded Cache-Resolution Correction" --
 * so this pre-flight check always agrees with what the subsequent
 * `LlamaCpp` construction and `embed()` call will actually use, rather
 * than falling back to node-llama-cpp's own unrelated default directory
 * when `modelCacheDir` is left unset.
 */
async function verifyModelIsLocallyAvailable(
  modelUri: string,
  modelCacheDir: string | undefined,
  resolveModelFile: (modelUri: string, options: { directory: string; download: false }) => Promise<unknown>,
  defaultModelCacheDir: string,
): Promise<void> {
  const effectiveModelCacheDir = modelCacheDir || defaultModelCacheDir;
  try {
    await resolveModelFile(modelUri, { directory: effectiveModelCacheDir, download: false });
  } catch (error) {
    const cause = error instanceof Error ? error.message : String(error);
    throw new Error(
      `local-control precondition failed: embedding model ${JSON.stringify(modelUri)} is not already ` +
        `present in the local model cache (${JSON.stringify(effectiveModelCacheDir)}). ` +
        `Proceeding would require QMD/node-llama-cpp to perform an on-demand network download, which ` +
        `this mechanism's own governance (Frozen Boundary #11, local/operator-controlled execution) ` +
        `forbids. Pre-fetch this exact model into the local cache before running this mechanism. ` +
        `Underlying resolution error: ${cause}`,
    );
  }
}

async function main(): Promise<void> {
  const requestPath = process.argv[2];
  if (!requestPath) {
    throw new Error("missing required argument: request file path");
  }

  const rawPayload = JSON.parse(await readFile(requestPath, "utf8"));
  validateRequest(rawPayload);
  const request = rawPayload;

  assertLocalQmdSourceRoot(request.qmdSourceRoot);
  const llmModuleUrl = await resolveLocalQmdModuleUrl(request.qmdSourceRoot, "src", "llm.ts");
  const nodeLlamaCppModuleUrl = await resolveLocalQmdModuleUrl(
    request.qmdSourceRoot,
    "node_modules",
    "node-llama-cpp",
    "dist",
    "index.js",
  );
  // Dynamic imports, resolved entirely from the local, operator-configured
  // `qmdSourceRoot` -- see this file's own header comment. `as any`: a
  // dynamic import specifier that is not a compile-time string literal
  // cannot be statically type-checked by tsx's transpile-only execution
  // (no separate typecheck step runs this file; see build.gradle.kts, this
  // script is never Kotlin-/Gradle-compiled), exactly as before this
  // correction when these same values arrived via static imports that tsx
  // also only transpiled, never type-checked, at run time.
  const { DEFAULT_MODEL_CACHE_DIR, LlamaCpp, formatDocForEmbedding, formatQueryForEmbedding } =
    (await import(llmModuleUrl)) as any;
  const { resolveModelFile } = (await import(nodeLlamaCppModuleUrl)) as any;

  await verifyModelIsLocallyAvailable(request.embeddingModelUri, request.modelCacheDir, resolveModelFile, DEFAULT_MODEL_CACHE_DIR);

  const llm = new LlamaCpp({
    embedModel: request.embeddingModelUri,
    modelCacheDir: request.modelCacheDir,
  });

  try {
    const modelUri = request.embeddingModelUri;

    const queryEmbedding = await llm.embed(formatQueryForEmbedding(request.queryText, modelUri));
    if (!queryEmbedding) {
      throw new Error("query embedding failed (QMD LLM.embed returned null)");
    }

    const scores: { token: string; score: number }[] = [];
    for (const candidate of request.candidates) {
      const candidateEmbedding = await llm.embed(formatDocForEmbedding(candidate.content, undefined, modelUri));
      if (!candidateEmbedding) {
        throw new Error(`candidate embedding failed for token ${JSON.stringify(candidate.token)} (QMD LLM.embed returned null)`);
      }
      scores.push({
        token: candidate.token,
        score: cosineSimilarity(queryEmbedding.embedding, candidateEmbedding.embedding),
      });
    }

    process.stdout.write(JSON.stringify({ scores }));
  } finally {
    await llm.dispose();
  }
}

void main().catch((error: unknown) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
