# OCR Mechanism — Docling Bridge Script Python-Side Contract / Implementation Plan

## Status

**Draft for owner review. Governance/implementation-planning only — no
Python, Kotlin, or shell code is implemented, proposed as a diff, or
changed by this document. No dependency is added. Docling is not
installed. No model is downloaded. No runtime/cache is provisioned.
No Docker or runtime-composition file is changed. Neither `src/` nor
`tests/` is touched. Nothing is staged, committed, or pushed.**

**Repository baseline confirmed fresh before drafting:** `main` at
`47ef7ba1fdc50a335dbd9676e8ab312837a2559d`, working tree clean.

This document designs, and only designs, the exact Python-side
contract `tools/docling-ocr-bridge.py` (illustrative name, not frozen)
must satisfy so that the already-adopted, already-implemented
`ProcessBuilderDoclingSubprocessInvoker`
(`src/runtime/DoclingOcrProviderAdapter.kt`, adopted commit `47ef7ba`)
can invoke a real Docling installation once one is separately
provisioned. It does not implement the bridge script itself, does not
install Docling, does not download a model, does not provision a
runtime or cache, does not touch `docker-compose.yml` or any other
deployment file, does not begin Unit 12's own composition wiring, and
does not begin Document Ingestion's own Tier B. Every numeric bound
below is reused verbatim from the Docling Authorization's own §6 and
the Adapter Plan's own §13, never re-derived.

---

## 1. Purpose

Determine the exact request/response/exit-code contract a future
`tools/docling-ocr-bridge.py` implementation must satisfy, so that:

1. A future implementation unit can write that script with an exact,
   reviewable specification to build against, rather than inferring
   one from the Kotlin adapter's own source code after the fact.
2. The specification is provably consistent with what
   `DoclingOcrProviderAdapter.kt`/`ProcessBuilderDoclingSubprocessInvoker`
   *actually* parse and expect today — not merely with what the
   Adapter Plan's own illustrative design *anticipated* they would
   need. This document is checked against the real, adopted Kotlin
   source, fresh, not against that plan's own prose alone.
3. A future Python-side dependency-review unit, and a future
   provisioning unit, each have an exact, minimal footprint
   specification to build and evaluate against.
4. A future test-implementation unit has a concrete, non-tautological
   test strategy for the bridge script itself, mirroring the split
   already established for the Kotlin adapter (always-run, mocked-API
   tests versus opt-in, real-Docling `liveModelEvaluation`-tier tests).

This document does not itself perform any of the above.

---

## 2. Authorities inspected fresh (this document)

**Governance (full, fresh-read this drafting pass):**
`docs/architecture/OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md`
("the Docling Authorization", adopted `c5b7aad`);
`docs/architecture/OCR_MECHANISM_DOCLING_CONCRETE_ADAPTER_IMPLEMENTATION_PLAN.md`
("the Adapter Plan", adopted `13d21cf`);
`docs/architecture/OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`
("the Unit 12 Scope Lock", adopted `0be393c`);
`docs/architecture/OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md`
("the Unit 12 Implementation Plan", adopted `9172893`) —
re-confirmed unchanged since each one's own adoption commit
(`git diff HEAD~1 HEAD -- docs/architecture/` returns nothing for any
of the four, confirming no drift since the immediately preceding
turn's own fresh read).

**Production code (full, fresh-read this drafting pass, not assumed
from any prior turn's own report):**

- `src/interfaces/OcrProviderAdapter.kt`, `src/runtime/OcrExecutionSequencer.kt`
  — re-confirmed unchanged since adoption; the frozen contract this
  bridge's own output must ultimately support, once through the
  adapter.
- **`src/runtime/DoclingOcrProviderAdapter.kt` (full, 968 lines,
  adopted `47ef7ba`) — the single, load-bearing source of truth this
  entire document is checked against.** Specifically, fresh-verified:
  `DoclingOcrProviderAdapter.buildRequestJson` (the exact request-file
  JSON shape); `ProcessBuilderDoclingSubprocessInvoker.invoke` (the
  exact `argv`, the exact two offline-mode environment variables
  `applyDoclingOfflineEnvironment` sets, the exact request-file
  temp-file convention); `DoclingOcrProviderAdapter.interpret` (the
  exact exit-code `when` branches — `0`, `-1`, `EXIT_CODE_MISSING_ASSETS
  = 2`, `EXIT_CODE_UNSUPPORTED_INPUT = 3`,
  `EXIT_CODE_RESOURCE_LIMIT_BREACH = 4`, and the `else -> error(...)`
  fall-through for any other non-zero code); `parseBridgeResponse`/
  `parseRecognitionResponse` (the exact, strict, allowed/required field
  sets for each `status` value — `recognised`, `partial`,
  `no_recognisable_content` — verified field-for-field, not
  paraphrased); `BoundedStreamDrain` (the exact stdout/stderr byte
  caps a real bridge script's own output must stay compatible with).
  Every contract fixed below (§5–§8) is copied from this file's own
  actual behaviour, not from the Adapter Plan's own earlier,
  illustrative prose, wherever the two differ in specificity — the
  Adapter Plan sketched the shape; this document freezes the shape the
  adopted code actually implements.
- `tests/runtime/DoclingOcrProviderAdapterTest.kt`,
  `tests/runtime/ProcessBuilderDoclingSubprocessInvokerTest.kt` (full,
  fresh-read) — confirmed every fake-invoker test's own JSON fixtures
  match the contract this document freezes (a divergence here would
  mean the Kotlin side's own tests are already inconsistent with its
  own implementation, which fresh reading confirmed is not the case).
- `tools/qmd-relevance-bridge.mts` (full, 368 lines, fresh-read) — the
  only existing production bridge-script precedent in this repository.
  Load-bearing conventions reused below: exactly one
  `process.stdout.write(JSON.stringify(...))` call, on the sole
  success path, nothing else ever written to stdout; every diagnostic
  and every failure message written to stderr only
  (`console.error(...)`); the request-file-path-as-`argv[1]`
  convention (this script's own `process.argv[2]`, index-shifted only
  because `argv[0]`/`argv[1]` are `node`/the script path itself — the
  Python equivalent is `sys.argv[1]`, `argv[0]` being the script path);
  a local-availability pre-flight check
  (`verifyModelIsLocallyAvailable`) performed *before* any code path
  capable of a live download becomes reachable, mirrored below (§10,
  §11) for Docling's own model-resolution surface; disposing/cleaning
  up its own heavy runtime object in a `finally` block before exiting.
  **One deliberate, disclosed departure from this precedent, not an
  inconsistency:** QMD's own bridge reports every failure through a
  single non-zero exit code (`process.exitCode = 1`, uniformly); the
  Docling bridge cannot do the same, because the already-adopted
  Kotlin adapter's own `interpret()` function requires four
  *structurally distinguishable* exit codes (§8, below) to construct
  the correct `OcrRecognitionOutcome` variant — a requirement QMD's
  own `RelevanceMechanism` contract never had (every QMD-bridge fault
  propagates as one undifferentiated thrown exception on the Kotlin
  side). This document's own exit-code table is therefore Docling
  Authorization/Adapter-Plan-specific, not a QMD-precedent violation.
- `docker-compose.yml` (full, fresh-read) — the exact, already-adopted
  QMD deployment convention (`PARKER_QMD_*` environment variables;
  `/opt/qmd:ro`/`/opt/qmd-models:ro` read-only mounts; the explicit,
  disclosed "this container never installs, builds, downloads, or
  otherwise provisions QMD or its embedding model itself" statement)
  this document's own §11/§20 mirror for Docling, substituting names
  only — re-confirmed no Docling-related entry of any kind exists yet.
- `build.gradle.kts` (fresh, full-file grep for `docling|python`) —
  confirmed no Docling/Python-related entry of any kind exists yet;
  the `liveModelEvaluation` detached source set (`val liveModelEvaluation
  by sourceSets.creating`, lines ~105-118) re-confirmed as the
  precedent §19, below, reuses for a future real-Docling bridge
  acceptance test, exactly as the Adapter Plan's own §23 already
  established for the Kotlin adapter side.
- `tools/README.md` (fresh-read) — confirmed no Python-tooling
  convention of any kind is documented there yet; this document does
  not modify it.
- Fresh repository-wide search (this pass): no `.py` file, no
  `requirements.txt`/`pyproject.toml`/`Pipfile`/`poetry.lock`, and no
  Docling reference of any kind exist anywhere outside `docs/` —
  re-confirmed, consistent with every prior turn's own identical
  finding.

**External knowledge about Docling itself, explicitly flagged as
such — not verified against this repository or against any live
package index, because this document is drafted with no network
access and nothing Docling-related exists in this repository yet.**
Every specific package name, version number, environment-variable
name, license identifier, and API surface named below in §9–§11 is
illustrative and must be independently reconfirmed by whoever actually
implements this contract, against Docling's own current released
documentation and PyPI metadata at that time — mirroring the Adapter
Plan's own §2 identical disclosure verbatim, applied here one tier
further into Docling's own Python-side specifics.

---

## 3. Current state

**Nothing exists yet.** Zero Python files, zero Docling references,
zero Python dependency-declaration files anywhere in this repository
(§2, above). The Kotlin adapter that will invoke this future script
exists and is adopted (`47ef7ba`); the script itself does not.

---

## 4. Bridge architecture overview

```
tools/docling-ocr-bridge.py (illustrative path, mirrors tools/qmd-relevance-bridge.mts)
  reads:  argv[1]  -- absolute path to a request JSON file
  writes: stdout    -- exactly one JSON object, on success only (exit 0)
          stderr    -- diagnostic/error text only, any exit code
  exits:  0 | 2 | 3 | 4 | other-nonzero  (never -1; see §8)
```

**A one-shot, disposable CLI script, not a service, not a module other
Python code imports.** Mirrors `tools/qmd-relevance-bridge.mts`'s own
"fresh, disposable child process for exactly one... call; nothing this
script creates survives past its own single invocation" shape exactly
(Adapter Plan §6, already adopted: "one-shot, disposable subprocess,
not a persistent service"). It holds no state across invocations, no
cache of its own, no background thread, no server socket of any kind.

**No Docling-specific authority may leak outside this one file.**
Mirroring `DoclingOcrProviderAdapter.kt`'s own confinement discipline
one tier further out (Docling Authorization §5): this script is the
*only* file in this repository permitted to `import docling` or any
of its own transitive dependencies. No second Python file, no shared
Python library module, and no Python package of Parker's own is
authorized or implied by this document.

---

## 5. Invocation contract (item 1)

**Exact `argv`, verified against `ProcessBuilderDoclingSubprocessInvoker.invoke`'s
own `command` construction, not re-derived:**

```
[pythonExecutablePath, bridgeScriptPath, requestFilePath]
```

- `argv[0]` — the Python interpreter (`DoclingOcrProviderAdapterConfiguration.pythonExecutablePath`,
  e.g. `python3`, or a venv-specific absolute path — a future
  provisioning unit's own choice, not fixed here).
- `argv[1]` (i.e. this script's own path) — `bridgeScriptPath`, unused
  by the script itself at runtime (Python does not need to know its
  own invocation path to read `sys.argv[1]` for its actual input).
- `sys.argv[1]` (the script's *own* first positional argument,
  distinct from the shell-level `argv[1]` above by exactly one index,
  the same off-by-one every CLI script has relative to its own launch
  command) — the **absolute path to a request JSON file**, written by
  `ProcessBuilderDoclingSubprocessInvoker` to a fresh temp file
  immediately before launch, deleted in that same class's own
  `finally` block immediately after this script exits (§14, below).
  **Required. Missing or empty → this script's own responsibility to
  detect and exit non-zero (illustratively, exit `1`, the unclassified/
  internal-fault code — see §8) with a diagnostic on stderr.**

**No shell.** `ProcessBuilder(command: List<String>)` already
structurally forecloses this on the Kotlin side (verified, §2 above);
this script must not itself invoke a shell, `os.system`, or
`subprocess.run(..., shell=True)` for any purpose (§18, below).

**No arbitrary path authority.** This script never constructs a path
from any value it did not receive verbatim in the request file (§6,
below) or from a small, fixed set of illustrative environment
variables a future provisioning unit may set (§10, §11, below,
mirroring `PARKER_QMD_*`). It never accepts a directory to scan, a
glob pattern, or any other broad filesystem-access primitive.

**Config/environment inputs, exhaustive:**

- The request file's own JSON body (§6, below) — the primary input
  channel.
- Exactly two, fixed-name environment variables the Kotlin side
  already sets on every invocation, verified against
  `applyDoclingOfflineEnvironment`: `HF_HUB_OFFLINE=1`,
  `TRANSFORMERS_OFFLINE=1` (§10, below — this script must not treat
  their *absence* as license to proceed online; see that section's own
  defence-in-depth requirement).
- Whatever ordinary environment variables the Python interpreter and
  Docling's own runtime require to function at all (`PATH`, `HOME`,
  locale variables) — inherited ambient environment, not something
  this script reads or acts on directly, and never anything this
  script treats as authorizing broader filesystem or network access.
- **No other environment variable is read by this script for any
  security- or path-relevant decision.** A future provisioning unit
  may introduce illustrative `PARKER_DOCLING_*` variables at the
  `docker-compose.yml`/`ParkerRuntimeConfig` tier (mirroring
  `PARKER_QMD_*`) that ultimately populate the request file's own
  `modelCacheDir` field — this script itself reads that value only
  from the request file, never directly from its own process
  environment, keeping exactly one source of truth (mirroring
  `ProcessBuilderQmdSubprocessInvoker`'s own "reads from the same
  single configuration instance... so there is exactly one frozen
  source of truth" discipline, Kotlin-side precedent applied here to
  the Python side).

---

## 6. Request file format (reused, not designed here)

**Already fixed by the adopted Kotlin adapter's own
`buildRequestJson` — restated here verbatim as this script's own
required input parsing target, not re-derived:**

```json
{
  "protocolVersion": "1",
  "sourceFilePath": "<absolute path to the source-bytes temp file>",
  "mediaType": "<the OcrRecognitionRequest's own mediaType, unchanged>",
  "configurationProfile": "docling-bridge-v1"
}
```

with one further, optional field, present only when
`DoclingOcrProviderAdapterConfiguration.modelCacheDir` is itself
configured:

```json
  , "modelCacheDir": "<absolute path to the read-only model-cache directory>"
```

**Field-by-field obligations on this script:**

- `protocolVersion` — must be checked against exactly `"1"` (the sole
  value this script may ever expect from this adopted contract
  revision); an unrecognised value is a malformed-request condition,
  exit `1` (unclassified — no more specific exit code applies to a
  request-file-shape defect, since the four named codes in §8 all
  describe *processing* outcomes, not request-parsing defects),
  mirroring `qmd-relevance-bridge.mts`'s own
  `SUPPORTED_PROTOCOL_VERSION` check exactly.
- `sourceFilePath` — the **only** source of the image/PDF bytes to
  recognise. This script opens it **read-only**; never opens it for
  writing, never deletes it (that remains
  `DoclingOcrProviderAdapter.kt`'s own `finally`-block responsibility,
  §14 below), never derives any other path from it (for example, never
  writes a sibling or derived file next to it).
- `mediaType` — trusted, unchanged, exactly as the Kotlin side already
  determined it (the same "pure callee, trusts caller-supplied
  context, never re-derives or second-guesses it" discipline every
  prior OCR Mechanism unit already establishes, Contract Design §4).
  Used only to select the PDF-versus-image processing branch (§12,
  below) — never re-sniffed from the file's own magic bytes, and never
  used to construct any path.
- `configurationProfile` — **read but not required to be acted upon.**
  This script may pass it through to stderr diagnostics or otherwise
  ignore it entirely; it exists for the Kotlin side's own bookkeeping
  (`DoclingOcrProviderAdapterConfiguration.configurationProfile`,
  already adopted), not as an instruction to this script.
- `modelCacheDir` — the fixed, pre-provisioned, **read-only** location
  to resolve Docling's own model files from, when present (§11,
  below). Absent → this script relies on whatever default cache
  location Docling's own current release resolves internally, subject
  to the same offline-mode enforcement (§10, below) regardless.

**Unknown extra fields in the request file are not this script's
concern to reject** — the request file is Parker-generated,
Kotlin-authored input, not adversarial input this script must defend
against the way it must defend against the *response* protocol being
misused downstream (§7, below, is the security-relevant direction);
this script may safely ignore any field it does not recognise here
without weakening any security property, since the request file's own
producer is the already-trusted, already-adopted Kotlin adapter, never
untrusted evidence content.

---

## 7. Response protocol (item 2)

**Exactly one JSON object, written to stdout exactly once, only on
exit code `0`, in UTF-8, with no other stdout output before or after
it — mirroring `qmd-relevance-bridge.mts`'s own single
`process.stdout.write(JSON.stringify(...))` call precisely.** Every
diagnostic, progress message, warning-to-a-human, or library-internal
log line Docling's own dependencies might otherwise print must be
redirected to stderr or suppressed entirely (§13, below) — **no extra
stdout noise** is not a style preference here; a single stray `print()`
anywhere in this script or a transitive dependency's own default
logging configuration would corrupt the one JSON value
`DoclingOcrProviderAdapter.kt`'s own `interpret()` function parses
character-for-character (verified, §2 above: `parseBridgeResponse`
requires `trimmed.startsWith("{") && trimmed.endsWith("}")` over the
*entire* captured stdout buffer).

**Exact JSON schema, verified field-for-field against
`parseRecognitionResponse`/`parseBridgeResponse`, not paraphrased.**
Three, and only three, values of a required `status` field:

### 7.1 `status: "recognised"` — full success

| Field | Required? | Type | Notes |
| --- | --- | --- | --- |
| `status` | required | string | exactly `"recognised"` |
| `recognisedText` | required | string | non-blank; the complete, document-level recognised text |
| `fidelity` | required | string | exactly one of `"VERBATIM"`, `"NORMALISED"`, `"INFERRED_RECONSTRUCTION"` (`parseFidelity` calls `TranscriptionFidelity.valueOf(raw)` — any other value throws) |
| `confidence` | optional | number or `null` | must fall within `0.0..1.0` (enforced by `OcrRecognitionResult`'s own already-frozen `init` block, not re-validated by the parser itself) — omit or `null` when Docling reports no genuine document-level scalar; **never averaged or fabricated from per-region figures** |
| `warnings` | optional | array of strings | defaults to empty when omitted |
| `segments` | optional | array of objects | see §7.4, below; defaults to empty when omitted |
| `mechanismVersion` | optional | string or `null` | Docling's own reported package version |
| `modelIdentity` | optional | string or `null` | which model set was actually loaded |

**No `reason` field is permitted for this status** — `parseRecognitionResponse`'s
own `allowed` set excludes `reason` unless `status == "partial"`; a
`"recognised"` response carrying a `reason` field is rejected as an
unexpected field (verified: `tests/runtime/DoclingOcrProviderAdapterTest.kt`'s
own `` `a full success is never accompanied by an unexpected reason field` ``
test proves this).

### 7.2 `status: "partial"` — degraded but usable

Identical field set to §7.1, **plus** one further required field:

| Field | Required? | Type | Notes |
| --- | --- | --- | --- |
| `reason` | **required** | string | non-blank; an honest, non-blank technical description of what is missing or degraded |

### 7.3 `status: "no_recognisable_content"` — ran cleanly, found nothing

| Field | Required? | Type | Notes |
| --- | --- | --- | --- |
| `status` | required | string | exactly `"no_recognisable_content"` |
| `reason` | required | string | non-blank |

**No other field is permitted** for this status — `recognisedText`,
`fidelity`, and every field §7.1 lists are all rejected as unexpected
if present alongside this status.

### 7.4 `segments[]` entries (nested, within `status: "recognised"`/`"partial"` only)

| Field | Required? | Type | Notes |
| --- | --- | --- | --- |
| `text` | required | string | non-blank |
| `fidelity` | required | string | same three-value enum as §7.1 |
| `pageNumber` | optional | integer or `null` | one-based; when multiple segments carry a `pageNumber`, they must appear in **non-decreasing** order across the array (enforced by `OcrRecognitionResult`'s own already-frozen `init` block) |

**No other field is permitted per segment** — a segment entry carrying
`content`, a bounding box, or any Docling-specific key is rejected
(`parseSegmentsValue`'s own `allowed = setOf("text", "fidelity", "pageNumber")`).

### 7.5 Strictness this script must satisfy exactly

- **Exact JSON, no trailing garbage, no leading BOM.** The whole
  captured stdout buffer, trimmed, must start with `{` and end with
  `}` — a UTF-8 BOM before the opening brace, or any byte after the
  closing brace (even whitespace beyond what `.trim()` strips), risks
  parser rejection; this script must write the JSON object and nothing
  else, terminated by at most a single trailing newline.
- **No duplicate top-level or per-segment keys.** `parseTopLevelFields`
  throws on any repeated key — Python's own `json.dumps` never
  produces this from an ordinary `dict`, so this is naturally satisfied
  by construction, not something this script must separately guard
  against, provided it builds the response as a single `dict` and
  serialises it exactly once via `json.dumps`.
- **UTF-8 throughout, including correct `\uXXXX` escaping for
  non-ASCII text.** Python's own `json.dumps` defaults to
  `ensure_ascii=True`, escaping every non-ASCII character as
  `\uXXXX` — this is fully, correctly supported by the Kotlin side's
  own `unescapeJsonString` function (verified fresh, §2 above; this
  exact support was added during the Kotlin adapter's own
  owner-acceptance review specifically because of this expected
  behaviour). This script may therefore use `json.dumps`'s own default
  `ensure_ascii=True` unmodified — **do not pass
  `ensure_ascii=False`**, since, while the Kotlin parser would still
  accept genuinely raw UTF-8 bytes correctly (`bufferedReader()`
  decodes as UTF-8), staying with the default keeps this script's own
  output pure-ASCII on the wire, the simplest, most conservatively
  compatible choice, and avoids depending on the *process stdout
  encoding* being configured correctly end-to-end across every future
  deployment target.
- **No output larger than this script itself must avoid, independent
  of the Kotlin-side cap.** §13, below, freezes an incremental,
  early-abort accumulation check specifically so this script never
  attempts to serialise (and only then discover is oversized) a
  multi-hundred-megabyte `recognisedText` value.

---

## 8. Exit-code semantics (item 3)

**Verified exhaustively against `DoclingOcrProviderAdapter.interpret`'s
own `when (invocation.exitCode)` block — every branch below
corresponds to exactly one line of that already-adopted function, not
invented independently.**

| Exit code | Meaning | This script's own responsibility | Kotlin-side mapping (already adopted) |
| --- | --- | --- | --- |
| `0` | Bridge executed to completion; stdout carries exactly one well-formed JSON response (§7) | Emit the response, nothing else on stdout | Parses stdout via `parseBridgeResponse` |
| `2` (`EXIT_CODE_MISSING_ASSETS`) | Required runtime/model/cache assets are absent — a pre-flight failure, detected *before* Docling's own recognition work begins | Perform the local-availability pre-flight check (§10, §11, below) before importing/invoking Docling's recognition API; on failure, write a clear diagnostic to stderr and exit `2` | `ProcessingOrDependencyFailure`, reason = stderr text |
| `3` (`EXIT_CODE_UNSUPPORTED_INPUT`) | Docling itself could not process the supplied content — malformed, corrupt, or genuinely unsupported media, discovered only once Docling actually attempts to open/parse it | Catch Docling's own decode/parse-time exception, write a clear diagnostic to stderr, exit `3` | `UnsupportedOrInaccessibleInput`, reason = stderr text |
| `4` (`EXIT_CODE_RESOURCE_LIMIT_BREACH`) | Any of: page count exceeds 200; a page's post-decode pixel dimensions/total pixel count exceed the frozen bound; accumulated or final serialised output exceeds 20 MiB (§13, below) | Detect the specific breach, write a clear diagnostic (naming which bound) to stderr, exit `4` — **never** continue processing past the point of detection | `ProcessingOrDependencyFailure`, reason = stderr text |
| any other non-zero (illustratively `1`) | An unclassified, unexpected internal fault — a genuine Python exception this script did not specifically anticipate, a malformed request file, an unhandled library error | Let it propagate to a top-level `except Exception` handler (mirroring `qmd-relevance-bridge.mts`'s own `.catch(...)`), print the exception message to stderr, exit `1` (or any code other than `0`/`2`/`3`/`4`/`-1`) | `error(...)` — **an uncaught, propagating `IllegalStateException`** on the Kotlin side, mirroring `QmdRelevanceMechanism`'s own `check(invocation.exitCode == 0)` discipline; this is deliberate, not a gap — an unclassified fault is a genuine, unexpected defect, never silently absorbed as a disclosed outcome |
| `-1` | **Reserved. This script must never explicitly exit with this code, and in practice never can:** `-1` is exclusively the JVM-side signal `ProcessBuilderDoclingSubprocessInvoker` itself produces when the process *fails to start at all* (an `IOException` from `ProcessBuilder.start()`, e.g. the interpreter binary not found) — a condition that, by definition, means no Python code in this script ever ran to produce any exit code of its own | N/A — not reachable from within this script | `ProcessingOrDependencyFailure`, reason = stderr text (a distinguished, JVM-constructed message, never sourced from this script) |

**Timeout is explicitly, and entirely, not this script's own
responsibility to detect, enforce, or report via any exit code.**
Verified against `ProcessBuilderDoclingSubprocessInvoker.invoke`
(`Process.waitFor(timeoutMillis, ...)` + `destroyForcibly()` on
expiry) and the Adapter Plan's own §14 ("the single, correct,
governance-consistent enforcement point is inside whatever concrete
`OcrProviderAdapter` implementation" — i.e. the Kotlin tier, not the
bridge). This script may run for as long as genuinely required; if
still running when the JVM-side 15-minute wall-clock ceiling elapses,
it is forcibly terminated (`destroyForcibly()`) regardless of its own
internal state, and any partial stdout already written is discarded by
the Kotlin side (`invocation.timedOut` is checked *before* any attempt
to parse stdout — verified, §2 above). This script must not implement
its own internal timeout, alarm signal, or early-exit-on-elapsed-time
logic of any kind — doing so would be redundant with, and could
conflict with, the JVM-side ceiling that is the sole, governing
enforcement point (Adapter Plan §14, restated).

---

## 9. Docling package/runtime requirements (item 4)

**Illustrative, external-knowledge-based, to be independently
reconfirmed at implementation time (§2, above) — restating and
narrowing the Adapter Plan's own §7 disclosure to the exact facts a
Python-side dependency-review unit will need to verify, never claimed
as already-verified fact here:**

- **Python version.** To be pinned by the future implementer against
  Docling's own current minimum-supported-version statement at
  implementation time — illustratively, Python 3.10+ (Docling's own
  historical minimum has moved over its release history; not verified
  here).
- **Package/version strategy.** The `docling` package itself,
  installed via `pip` into a dedicated virtual environment — never the
  host's system Python (mirroring the isolation discipline this
  repository already applies to Node/`node_modules` for QMD). Exact
  version pin: **not chosen by this document** — a future,
  separate dependency-review unit (§22, below) must supply it,
  verified against Docling's own current PyPI release and this
  script's own actual, tested compatibility with it. This document
  freezes the *contract* the bridge must satisfy regardless of which
  compatible Docling version eventually gets pinned, not a version
  number.
- **Deep-learning backend.** Docling's own recognition/layout models
  historically depend on a backend such as PyTorch; CPU-only inference
  is feasible (slower, not incorrect) and avoids the additional
  driver/hardware dependency surface a GPU requirement would introduce
  (Adapter Plan §7, restated, already adopted as governance fact for
  the Kotlin side's own deployment-footprint description).
- **Native dependencies.** Whatever native libraries Docling's own
  Python dependencies require (image codec libraries, tensor-math
  libraries) are confined entirely to the Python virtual environment;
  the JVM process never loads any of them directly (already adopted,
  Adapter Plan §7).
- **License.** **Not verified here.** Docling and each of its
  transitive dependencies each carry their own license, which a future
  dependency-review unit must independently confirm against the
  actual, current package metadata before installation — this
  document does not assert Docling's own license identifier as
  verified fact, consistent with the "external knowledge... must be
  independently reconfirmed" disclosure (§2, above) applied here
  specifically to license review, which the Adapter Plan's own §25
  already named as required, future, not-yet-performed work.
- **Model/runtime requirements.** Docling's own pretrained
  layout/OCR model checkpoints, pre-downloaded once outside this
  script's own execution path entirely (§11, below); a fixed,
  known, read-only-mounted cache location this script reads from,
  never populates.

---

## 10. Offline/no-network behavior (item 5)

**Defence in depth, mirroring `qmd-relevance-bridge.mts`'s own
"pre-flight check happens before the code path capable of a live
download is ever reachable, never as an after-the-fact catch" —
applied here as two independent layers, neither claimed sufficient
alone (Adapter Plan §8, already adopted, restated):**

1. **Application-level offline-mode flags, set redundantly, at two
   layers.** The Kotlin side already sets `HF_HUB_OFFLINE=1` and
   `TRANSFORMERS_OFFLINE=1` on this script's own process environment
   before launch (verified, §2 above). **This script must not merely
   trust that inheritance** — it must itself, at its own very first
   lines, *before* `import docling` or any transitive import capable
   of triggering model resolution, set the same two environment
   variables again (illustratively,
   `os.environ.setdefault("HF_HUB_OFFLINE", "1")`,
   `os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")` — `setdefault`,
   not overwrite, so an explicit, more specific value is never
   clobbered) — belt-and-braces, so this script remains correctly
   offline-configured even if invoked, in a future test or manual
   diagnostic context, without the Kotlin-side environment already in
   place. If Docling's own current release exposes an explicit
   local-path/offline API distinct from these two environment
   variables (its own release notes, to be confirmed at implementation
   time), that API should be preferred and used identically, before
   any import capable of triggering resolution.
2. **A local-availability pre-flight check, performed before any
   Docling API call capable of triggering a network fetch.** Mirroring
   `verifyModelIsLocallyAvailable`'s own exact shape (§2, above): before
   invoking whichever Docling/Hugging-Face-ecosystem call would
   otherwise resolve model weights, this script checks that the
   expected model files already exist at the effective, resolved cache
   location (§11, below) and, on a miss, exits `2` with a clear
   diagnostic — **never** falls through to a code path capable of an
   on-demand download.

**No automatic model download under any condition.** Consistent with
both layers above; this script contains no code path that, on a cache
miss, silently fetches a model instead of failing.

**No package installation.** This script never invokes `pip`,
`conda`, or any other package manager, under any condition, for any
reason — installing Docling's own package and its dependencies remains
exclusively a separate, future provisioning unit's own responsibility
(§20 item B, below), entirely outside this script's own execution
path.

**No cache mutation.** This script only ever *reads* from the
model-cache directory (§11, below); it never writes, creates, deletes,
or otherwise mutates any file within it, under any condition —
consistent with that directory being mounted read-only at the
deployment tier (a future provisioning unit's own responsibility,
mirroring `docker-compose.yml`'s existing `/opt/qmd-models:ro`
convention exactly).

**Fail if required assets are absent.** Restated from item 2, above —
this is the exact behaviour exit code `2` (§8) exists to represent.

**OS/network denial remains provisioning responsibility, not claimed
as already achieved by this script alone.** Restated, unchanged, from
the Adapter Plan's own §8 (already adopted): the two layers above are
real, additive controls, but neither this script nor
`DoclingOcrProviderAdapter.kt` can *structurally* prove zero network
traffic is possible — the recommended, stronger guarantee (an
OS/container-level outbound-network denial policy) belongs to a
future, separate provisioning/deployment-tier decision (§20 item D,
below), never designed or enforced by this Python-side contract alone.

---

## 11. Model/cache boundary (item 6)

- **Read-only location.** The fixed, pre-provisioned model-cache
  directory this script resolves models from — supplied via the
  request file's own optional `modelCacheDir` field (§6, above) when
  configured, or Docling's own current-release default cache location
  otherwise (subject to the same offline enforcement, §10, above,
  regardless of which location is in effect). Mounted read-only at the
  deployment tier (a future provisioning unit's own responsibility,
  mirroring `/opt/qmd-models:ro`) — this script treats it as read-only
  by construction (§10, above: "no cache mutation"), never merely by
  the filesystem's own permission bits happening to enforce it.
- **Expected files/assets.** Not fixed by this document — depends on
  exactly which Docling pipeline/model configuration a future
  implementer selects (§9, above); that implementer's own dependency
  review (§22, below) must document the exact expected directory
  layout once a concrete Docling version and pipeline configuration
  are chosen.
- **No bridge-side provisioning of any kind.** Restated from §10,
  above — this script never creates the cache directory, never
  populates it, never downloads into it, under any condition.
- **Version/provenance extraction.** Once a model has genuinely been
  loaded (never merely assumed), this script should extract whatever
  model-identity/version fact Docling's own API exposes (illustratively,
  a model-checkpoint name or hash Docling's own pipeline configuration
  reports) and surface it via the response's own optional
  `modelIdentity` field (§7.1, above) — **never fabricated when
  genuinely unavailable; `null`/omitted is the honest disposition in
  that case**, mirroring every other "genuinely available, never
  invented" disclosure this Unit and its predecessors already
  establish.

---

## 12. PDF/image support (item 7)

- **Scanned PDF.** Processed via Docling's own PDF/OCR pipeline,
  page-by-page, each page contributing one `segments[]` entry (§7.4,
  above) carrying its own `pageNumber` (one-based, non-decreasing
  order across the array — already enforced by the Kotlin-side
  `OcrRecognitionResult` type itself, restated here as a constraint
  this script's own segment-ordering must satisfy to avoid a rejected
  response).
- **PNG/JPEG and whatever other image formats Docling's own current
  release genuinely supports.** This script trusts the request file's
  own `mediaType` (§6, above) to select the image-versus-PDF branch;
  it does not itself maintain, or need to maintain, an exhaustive list
  of supported image formats beyond whatever Docling's own API accepts
  — an image format Docling cannot open surfaces naturally as exit
  code `3` (§8, above), the same path any other malformed/unsupported
  input takes.
- **No source mutation.** This script opens `sourceFilePath` (§6,
  above) read-only; it never writes to it, never moves it, never
  deletes it (§14, below — cleanup of that file remains exclusively
  `DoclingOcrProviderAdapter.kt`'s own responsibility, in its own
  `finally` block, entirely outside this script's own execution).
- **No unsupported rich Docling output leakage.** Docling's own API
  may naturally expose tables, headings, document hierarchy, per-word
  bounding boxes, or layout classification labels as part of its
  broader structured-document-conversion capability — **none of these
  may ever appear in this script's own response JSON**, under any
  field name, including smuggled into `warnings` as free text framed
  as a workaround. The response schema (§7, above) has no field to
  carry any of them, and none may be added by this script inventing
  its own extra field — the already-frozen Kotlin-side parser rejects
  any field it does not explicitly expect (§7.5, above), which is
  itself the structural enforcement of this exact prohibition, not
  merely a policy statement this script must remember to honour.

---

## 13. Resource enforcement owned by the bridge (item 8)

**Verified against the Adapter Plan's own already-adopted §13 table,
restated here as the exact behaviour this script itself must
implement (as distinct from the Kotlin-side defensive re-checks
`DoclingOcrProviderAdapter.kt` already performs independently — see
that file's own KDoc, §2 above, for the exact division of labour):**

- **200-page PDF maximum.** Checked immediately after the PDF's own
  document structure is opened (page count becomes knowable at that
  point, and only at that point), *before* any per-page OCR work
  begins on page 201 or beyond. On breach: exit `4` (§8, above),
  stderr names the actual page count and the bound.
- **Post-decode image/pixel checks, two-stage, per page/image.**
  **Stage one, before full decompression:** inspect the page/image's
  own *declared* dimensions (most PDF/image libraries expose
  header-level width/height without fully decoding pixel data) and
  reject outright (exit `4`) if the declared size alone already
  exceeds 10,000×10,000px or 100 megapixels — the genuine
  decompression-bomb defence, since a check performed only *after*
  full rasterisation would already be too late (Adapter Plan §13,
  already adopted, restated verbatim). This is a **second, independent
  bridge-side layer**, in addition to (never a replacement for) the
  Kotlin-side Stage-0 declared-header check `DoclingOcrProviderAdapter.kt`
  already performs for whole-file, image-media-type requests — this
  script's own check additionally covers *each PDF page's own embedded
  raster*, which the Kotlin-side, file-level-only check cannot reach.
  **Stage two, immediately after rasterisation/decode and before OCR
  runs on that page:** re-check the *actual* decoded size as a second,
  independent layer against a declared size that under-reported the
  true content — on breach, exit `4`, aborting with a distinct,
  structured stderr message at whichever stage caught it, **never**
  silently downsampling or cropping to fit.
- **20 MiB serialised-output ceiling.** This script accumulates
  recognised-text byte length **as pages complete**, and aborts early
  (exit `4`) the moment the running total would exceed the bound —
  never waits until the entire document is processed only to discover,
  at final serialisation, that the result is oversized. This is a
  defence-in-depth pairing with `DoclingOcrProviderAdapter.kt`'s own
  independent, already-adopted post-parse re-check
  (`buildRecognitionOutcome`'s own `outputBytes > configuration.maxOutputTextBytes`
  check) — either layer catching the breach is sufficient; neither is
  presumed to be the sole guard.
- **Output must fail, never truncate successfully.** Restated as its
  own explicit requirement, mirroring this task's own governing
  instruction verbatim: at no point may this script silently truncate
  `recognisedText` (or any `segments[].text`) to fit under a bound and
  then report `status: "recognised"`/`"partial"` as though nothing
  were cut. A breach is always, and only, exit `4` — never a shortened
  but nominally successful response.

**Concurrency (exactly one Docling invocation per Parker instance) and
the 15-minute timeout are explicitly, and correctly, *not* this
script's own responsibility** — both are enforced entirely at the
Kotlin tier (the single-permit `Mutex` around the whole invocation,
and `Process.waitFor(timeout)`, respectively; verified, §2 above, and
restated at §8's own timeout paragraph). This script performs no
locking, no queueing, and no internal time-budget logic of its own.

---

## 14. Temp/output behavior (item 9)

- **No arbitrary output files.** This script's only output channel is
  stdout (§7, above) and stderr (§13 of this task's own numbering —
  diagnostics only, §18 below). It never writes a result to any file
  on disk, under any condition.
- **No provider-selected durable path.** This script never chooses,
  names, or creates any file or directory of its own at any location
  it selects — the only path it ever opens is `sourceFilePath`,
  supplied verbatim by the request file (§6, above), read-only. If
  Docling's own internal implementation requires its own transient
  scratch files during processing (for example, an intermediate
  rasterisation buffer), those must be created via the Python
  standard library's own secure temporary-file facility
  (`tempfile.NamedTemporaryFile`/`tempfile.TemporaryDirectory`, which
  select a random, collision-resistant name inside the OS's own
  configured temp directory) and **must be cleaned up by this script
  itself before it exits**, on every code path (success, every failure
  category, and an unclassified exception alike) — mirroring
  `ProcessBuilderQmdSubprocessInvoker`'s and
  `DoclingOcrProviderAdapter.kt`'s own identical "unconditional
  cleanup, every path" discipline, applied here to whatever transient
  scratch state Docling's own library internals may require that this
  document cannot fully anticipate without a real Docling installation
  to inspect.
- **No source path persisted.** `sourceFilePath`'s own string value
  must never appear anywhere in this script's own stdout response
  (§7, above has no field to carry a path, and none may be added,
  §12's own "no unsupported... leakage" restated identically here for
  paths specifically) — it may appear in a stderr diagnostic (stderr
  is not part of the governed response contract, and a path in a
  diagnostic aids operator debugging without creating any durable,
  governed record of it), but never in the one JSON object stdout
  carries.
- **No cache writes.** Restated from §10/§11, above, for completeness
  of this section's own checklist.

---

## 15. Provenance (item 10)

Restated, precisely, from §7.1/§11, above, gathered here as their own
checklist:

- **Docling version** → `mechanismVersion` (§7.1) — Docling's own
  reported package version, captured genuinely (illustratively, its
  own `__version__` attribute or `importlib.metadata.version("docling")`,
  to be confirmed against Docling's own actual current API at
  implementation time), never a hard-coded or guessed string.
- **Model identity/version, where genuinely available** →
  `modelIdentity` (§7.1, §11) — never fabricated when unavailable;
  `null`/omitted is the honest disposition.
- **Configuration identity** — **not this script's own field to
  populate.** `OcrRecognitionIdentity.configurationProfile` is
  assembled entirely on the Kotlin side, from
  `DoclingOcrProviderAdapterConfiguration.configurationProfile` plus
  this script's own optional `modelIdentity` value (verified,
  `buildRecognitionOutcome`'s own
  `"${configuration.configurationProfile};model=${parsed.modelIdentity}"`
  construction, §2 above) — this script contributes `modelIdentity`
  only; it does not, and must not, attempt to construct or emit a
  `configurationProfile` field of its own.
- **Warnings** → `warnings[]` (§7.1) — any genuine, non-fatal condition
  Docling itself discloses (for example, "page 4 partially processed
  due to low scan quality"), passed through as plain text, order
  preserved, never sorted, deduplicated, or fabricated.
- **Confidence, only if truthful** → `confidence` (§7.1) — only when
  Docling reports a genuine, single, document-level scalar in
  `0.0..1.0`; omitted/`null` otherwise; **never averaged or invented
  from per-region figures** if no genuine document-level figure
  exists (restated verbatim from the Adapter Plan's own already-adopted
  §11).

---

## 16. OCR mapping (item 11)

- **Exact text.** `recognisedText` (§7.1) carries the complete,
  document-level text a human reader would recognise — Docling's own
  plain recognised-text output, never its own richer
  markdown/structured export (Docling Authorization §5: "interpreting...
  into recognised text, and nothing more" — already-adopted governance,
  restated).
- **Page association, where genuinely available.** `segments[].pageNumber`
  (§7.4) — populated only when this script can genuinely attribute a
  portion of text to a specific page (naturally true for
  page-by-page-processed PDFs); `null` for a single-image request with
  no page concept of its own, never a fabricated page number.
- **Coordinates — never represented, under any field name, at any
  granularity.** No field in the already-frozen `OcrRecognitionSegment`
  shape exists to carry a bounding box or coordinate of any kind (Unit
  12 Scope Lock §12: "not authorised"), and this document adds none —
  restated as its own explicit prohibition, not merely implied by
  §7.4's own closed field list.
- **Confidence, only where the contract supports it.** Restated from
  §15, above — document-level only, via the single `confidence`
  field; no per-region or per-segment confidence field exists in the
  already-frozen `OcrRecognitionSegment` shape, and none is added
  here.
- **No fabricated structure.** Restated, one final time, from §12's
  own "no unsupported rich Docling output leakage" — tables, headings,
  hierarchy, and layout classification are never represented in any
  form this response protocol carries.

---

## 17. Failure semantics (item 12)

Every case below maps onto exactly one exit code (§8, above) — no case
is handled by any other mechanism, and no case is silently absorbed
into a different one:

| Case | Disposition |
| --- | --- |
| Malformed PDF/image (Docling itself cannot decode it) | Exit `3` |
| Unsupported media type (Docling has no pipeline for it) | Exit `3` |
| Missing models/cache (pre-flight check fails, §10/§11) | Exit `2` |
| Runtime unavailable (Docling package itself fails to import — a provisioning defect, not an input defect) | Exit `2` (the pre-flight check should catch a missing/broken installation before attempting any recognition work; if the import itself fails, that is functionally identical to "required assets absent") |
| Recognition failure — ran cleanly, found nothing usable | Exit `0`, `status: "no_recognisable_content"` (§7.3) |
| Recognition failure — ran, found something, but degraded/incomplete | Exit `0`, `status: "partial"` (§7.2) — the actual partial text is preserved, never discarded |
| Resource-limit rejection (page count / pixel bound / output size) | Exit `4` (§13) |
| Serialization failure (this script's own bug — the response `dict` it constructed is not genuinely JSON-serialisable) | Exit `1` (unclassified — this is an internal defect in this script itself, never a disclosed, expected outcome) |
| Output too large, discovered only at final serialisation despite the incremental check (§13) | Should not occur if the incremental check (§13) is implemented correctly; if it does, exit `4`, not exit `0` with a truncated value |
| Internal exception (any unanticipated Python exception) | Exit `1` (§8's own "unclassified" row), stderr carries the exception's own message |
| Timeout | **Not this script's own case to handle at all** (§8, restated) — the JVM-side ceiling terminates this script from outside; nothing inside this script ever observes or reports this condition |

---

## 18. Security (item 13)

- **No arbitrary imports/plugins from evidence input.** This script's
  own set of imports is fixed at the top of the file, chosen once by
  whoever writes it; nothing in the request file (§6, above) — not
  `mediaType`, not `configurationProfile`, not any other field — may
  ever be used to construct an import name, a plugin identifier, or
  any other dynamically-resolved code-loading target. Docling's own
  internal plugin/extension mechanisms, if it has any, must never be
  driven by any value this script received from the request file.
- **No subprocess chaining unless expressly required by Docling's own
  API.** This script should not itself spawn a further child process
  unless Docling's own library genuinely requires it internally (for
  example, a native helper binary its own Python bindings invoke) —
  and even then, this script has no visibility into, and no
  responsibility for, sanitising an argument vector it never
  constructs itself; that remains entirely Docling's own library
  concern, outside this contract's own reach. This script itself must
  never explicitly launch a second subprocess of its own design.
- **No shell.** Restated from §5, above — `subprocess.run(...,
  shell=True)`, `os.system(...)`, and any other shell-invoking Python
  API are prohibited outright, for any purpose, anywhere in this
  script.
- **No network clients.** This script must import no HTTP client
  library for its own direct use (`requests`, `urllib`, `httpx`, or
  similar) — restated from §10's own "no networking-capable dependency"
  discipline, applied at the Python-import level specifically. Docling's
  own transitive dependencies may themselves be *capable* of network
  access (§10's own disclosed, unresolved limitation, restated) — this
  script's own code never adds a further, independent network
  capability on top of whatever Docling's own runtime already carries.
- **Malicious document handling.** The genuine attack surface here is
  Docling's own parsing libraries, not this script's own code; this
  script's own mitigation is exactly the resource bounds already fixed
  (§13, above), bounding the blast radius any single malicious
  document's own parsing can reach, plus whatever process-level
  isolation a future, separate provisioning unit chooses to add
  (§20 item D, below — a dedicated, low-privilege OS account for this
  script's own process, recommended by the Adapter Plan §18, not
  designed here).
- **stdout/stderr limits compatible with the Kotlin side.** Verified
  against `BoundedStreamDrain` (§2, above): the Kotlin side caps
  buffered stdout at approximately 24 MiB and stderr at approximately
  1 MiB, destroying the subprocess and reporting an honest,
  disclosed failure on breach — never silently truncating and treating
  the truncated bytes as complete. This script's own 20 MiB
  incremental output-accumulation check (§13, above) keeps ordinary
  stdout well under the Kotlin-side cap with comfortable headroom for
  JSON framing overhead; this script's own stderr diagnostics must
  remain terse (a single-line or short, multi-line error message, not
  a full stack trace dump by default) to stay comfortably under the
  smaller, ~1 MiB stderr cap.
- **Source path privacy.** Restated from §14, above — `sourceFilePath`'s
  own string value must never appear in the governed stdout response;
  confined to stderr diagnostics only, where it aids operator
  debugging without creating any durable, governed record.

---

## 19. Test strategy (item 14)

Mirrors the Adapter Plan's own already-adopted §23 split, extended to
the Python side, mirroring `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s
own established convention (§2, above):

**Pure-Python unit tests — no real Docling installation, no model, no
network, always runnable, mirroring how `DoclingOcrProviderAdapterTest.kt`
itself never spawns a real subprocess:**

1. Request-file parsing: valid request → all fields correctly read;
   missing/empty `sys.argv[1]` → exit `1` with a clear diagnostic.
2. `protocolVersion` mismatch → exit `1`, distinct diagnostic naming
   the unsupported version.
3. Response-schema construction: a fake/mocked Docling recognition
   result → the exact `status: "recognised"` JSON this document's §7.1
   freezes, verified byte-for-byte (or via `json.loads` round-trip)
   against the schema, not merely "some JSON came out."
4. Same, for `status: "partial"` (§7.2) and `status: "no_recognisable_content"`
   (§7.3).
5. `segments[]` construction, including the non-decreasing
   `pageNumber` ordering requirement (§7.4) — a test proving this
   script's own page-iteration order genuinely produces non-decreasing
   page numbers, not merely asserting the shape once.
6. Unicode: a fake/mocked recognition result containing non-ASCII
   characters (accented Latin, CJK, emoji outside the Basic
   Multilingual Plane) → the emitted JSON, when decoded by an
   independent JSON parser (Python's own `json.loads`, round-tripped),
   reproduces the exact original string — proving this script's own
   `json.dumps(..., ensure_ascii=True)` usage (§7.5, above) is
   genuinely correct, not merely assumed correct because it is the
   library default.
7. Malformed/mocked-corrupt input → the fake Docling API double raises
   a decode-time exception → this script maps it to exit `3`,
   verified via the test process's own actual exit code (a real
   subprocess invocation of this script under test, or an equivalent
   in-process function-level test if this script's own internal
   structure is factored to support one — a future implementer's own
   choice, not fixed here).
8. Missing model/cache (a mocked or genuinely-absent cache directory,
   pointed at by `modelCacheDir` in a synthetic request file) → exit
   `2`, before any mocked Docling recognition call is ever invoked
   (proving the pre-flight check genuinely runs first, mirroring
   `DoclingOcrProviderAdapterTest.kt`'s own "an oversized source must
   never reach the subprocess boundary" style of proof, one tier
   further in).
9. 200-page limit: a fake/mocked document-open result reporting 201
   pages → exit `4`, before any per-page OCR call is attempted.
10. Pixel limit: a fake/mocked page/image whose declared (Stage one)
    or decoded (Stage two) dimensions exceed the bound → exit `4`,
    with each stage tested independently (a declared-size breach must
    be caught *before* any full-decode call the test double can
    observe was never made; a decoded-size breach, where the declared
    size under-reported the truth, must still be caught at Stage two).
11. Output limit: a fake/mocked multi-page document whose accumulated
    text would exceed 20 MiB by the final page → exit `4`, reached
    before all pages are processed (an early-abort proof, not merely
    a final-size check).
12. JSON exactness: every field-name and allowed-value assertion §7
    freezes, each proven by an independent test that would fail if
    this script ever emitted an extra field, a wrong-cased status
    value, or a field of the wrong type.
13. No stdout contamination: a test capturing this script's own real
    stdout stream end-to-end (for example, invoking it as a genuine
    subprocess under test, exactly as `ProcessBuilderDoclingSubprocessInvokerTest.kt`
    does for the Kotlin invoker) and asserting it contains **only**
    the one JSON line — proving no logging library Docling's own
    dependencies configure by default leaks a stray line onto stdout.
14. Provenance: a fake/mocked Docling result carrying a known version
    string and model identity → both correctly surfaced in
    `mechanismVersion`/`modelIdentity`; a result reporting neither →
    both fields genuinely absent (`null`/omitted), never fabricated.

**`liveModelEvaluation`-equivalent — real Docling, opt-in, gated on
provisioning, mirroring the Kotlin-side split exactly (Adapter Plan
§23, items 16-19, already adopted):**

15. A real, provisioned Docling installation genuinely recognises text
    from the same immutable bake-off fixtures the Kotlin side's own
    equivalent test uses (`03-scanned.pdf`, `07-text-image.png`).
16. A real Docling installation, with the offline-mode environment
    variables deliberately unset and a deliberately missing/renamed
    model-cache directory, fails closed (exit `2`) — never triggers a
    real network download.
17. A real Docling installation genuinely exceeding a very large page
    count (or a deliberately slow synthetic fixture standing in for
    one) is genuinely terminated by the JVM-side timeout when invoked
    through the real Kotlin adapter end-to-end — this is properly an
    integration-level test of the *pair* (script + adapter), not this
    script in isolation, and belongs alongside whichever
    `liveModelEvaluation` task a future implementer registers for it.
18. End-to-end: the real script, invoked through the real, adopted
    `ProcessBuilderDoclingSubprocessInvoker`/`DoclingOcrProviderAdapter`
    pair (no fake invoker anywhere in this one test), against a real,
    provisioned Docling installation, produces a genuinely correct
    `OcrRecognitionOutcome.Recognised` for a known-good fixture — the
    single test that actually exercises this entire contract
    end-to-end, rather than each side's own tests exercising it only
    against a double.

**Full-suite regression requirement, mirrored from the Adapter Plan's
own §23:** the pure-Python unit tests (items 1-14) must be run in an
ordinary, offline, deterministic Python test invocation (a future
implementer's own choice of `pytest`/`unittest`, not fixed here)
before any future implementation of this contract may be declared
code-complete; the `liveModelEvaluation`-equivalent tests (items
15-18) are explicitly not part of that regression requirement, exactly
as their Kotlin-side counterparts already are not.

---

## 20. Provisioning split (item 15)

| # | Item | This document's own authority? |
| --- | --- | --- |
| A | Bridge code (`tools/docling-ocr-bridge.py` itself) | **Designs the contract for; does not write.** A future, separate implementation unit writes the code satisfying §5-§19, above. |
| B | Python/Docling installation | **Not authorized, not performed.** A future, separate provisioning unit's own responsibility (§9, above). |
| C | Model/cache provisioning | **Not authorized, not performed.** Same future provisioning unit, or a further-separated one (§11, above). |
| D | Structural network denial (OS/container-level) | **Not authorized, not performed, not designed.** A recommended, future, separate deployment-tier decision (§10, above) — this document's own two application-level layers are not claimed as a substitute. |
| E | Live acceptance (§19 items 15-18, real Docling) | **Not authorized, not performed.** Requires B and C to already exist; a future, separate test-implementation unit. |
| F | Unit 12 composition (`EvidenceIntelligenceOcrCoordinator`, `DefaultEvidenceIntelligence`'s third constructor parameter) | **Entirely unaffected, unaddressed, not authorized here.** Already, separately designed by the Unit 12 Implementation Plan; this document neither advances nor requires it. |
| G | Document Ingestion Tier B | **Entirely unaffected, unaddressed, not authorized here.** |

**These seven are not, and must never be, silently combined.** A
future implementation turn that writes A must not simultaneously
attempt B, C, D, E, F, or G without each one's own separate proposal
and review — mirroring the Docling Authorization's own §9 identical
warning against combining B-E, extended here to the two further items
(F, G) this document's own task explicitly named.

---

## 21. Implementation impact map (item 16)

| Surface | Classification |
| --- | --- |
| New `tools/docling-ocr-bridge.py` (illustrative name) satisfying §5-§19 | **REQUIRED** — future, separate implementation unit |
| New pure-Python unit tests (§19, items 1-14) | **REQUIRED** — same future unit |
| New `liveModelEvaluation`-equivalent tests (§19, items 15-18) | **CONDITIONAL** — required before this contract may be declared *live*-complete; not runnable, and not required to pass, before provisioning (§20 items B/C) exists |
| A Python dependency-declaration file (`requirements.txt`/`pyproject.toml`, or equivalent) | **REQUIRED** — future, separate implementation unit, coordinated with the dependency review a future unit must also perform (§22, below) |
| Docling `pip` package installation | **FORBIDDEN under this document's own authority** — provisioning-unit work (§20 item B), not performed here |
| Docling model download/provisioning | **FORBIDDEN under this document's own authority** — same (§20 item C) |
| `docker-compose.yml`/`Dockerfile` entries for a Python runtime, Docling venv, or model-cache mount | **REQUIRED, future, separate provisioning-unit implementation** — named precisely by the Adapter Plan's own already-adopted §22, not re-designed here |
| New `ParkerRuntimeConfig` fields (`doclingPythonExecutablePath`, etc.) | **NOT this document's own scope** — already named, illustratively, by the Adapter Plan's own §22; not re-authorized or re-designed here |
| Any change to `OcrMechanism.kt`, `OcrProviderAdapter.kt`, `OcrExecutionSequencer.kt`, `DoclingOcrProviderAdapter.kt`, the Docling Authorization, either Unit 12 document, the Contract Design, or the Scope Lock | **FORBIDDEN** |
| Unit 12's own composition wiring | **NOT this document's own scope** — already, separately designed; unaffected |
| Document Ingestion Tier B routing | **FORBIDDEN under this document's own authority** |
| A second Python bridge script, or Docling-specific logic in any file other than the one this document names | **FORBIDDEN** — mirrors the Kotlin-side single-adapter confinement (§4, above) |

---

## 22. Dependency review (item 17)

**No dependency is added by this document.** For a future
implementation unit to resolve, not answered here:

- **Python package.** `docling` itself and its own full transitive
  dependency tree (illustratively including a deep-learning backend
  and image-handling libraries) — exact package names, exact version
  pins, and a documented upgrade/re-verification policy, none chosen
  here (§9, above).
- **License.** Docling's own license and every transitive dependency's
  own license, individually confirmed against actual, current package
  metadata — not asserted as verified fact anywhere in this document
  (§9, above).
- **Footprint.** Approximate virtual-environment disk size, approximate
  model-file cache size, and the one-time, install-time network
  requirement for the initial model download (performed entirely
  outside any running Parker container's own execution path, mirroring
  `docker-compose.yml`'s own existing QMD convention) — not measured or
  estimated here.
- **Security implications.** Supply-chain exposure of a Python package
  ecosystem this repository has never formally depended on before (the
  Adapter Plan's own §25 already named this as "a new dependency
  surface entirely, unlike QMD's own Node/npm-side dependencies") —
  not evaluated here; a future, separate dependency-review turn must
  supply this evaluation before installation proceeds.
- **Host OS.** The Python interpreter itself, and whatever native
  libraries Docling's own dependencies require at the OS level (image
  codec libraries) — same review requirement, not performed here.
- **JVM side.** **None anticipated.** This document introduces no new
  Kotlin/Gradle dependency of any kind — `DoclingOcrProviderAdapter.kt`
  already exists, already adopted, already satisfies its own side of
  this contract without modification.

**No dependency of any kind is added during this task.**

---

## 23. Required / conditional / optional / forbidden classification

| # | Rule | Class |
| --- | --- | --- |
| 1 | Exactly one JSON object on stdout, only on exit `0`, matching §7 exactly | **R**, once the bridge is implemented |
| 2 | Exit codes `0`/`2`/`3`/`4`/other-non-zero used exactly as §8 fixes; `-1` never emitted | **R** |
| 3 | Offline-mode flags set redundantly by the script itself, before any Docling import | **R** |
| 4 | Local-availability pre-flight check before any model-resolution-capable call | **R** |
| 5 | Every §13 resource bound enforced by the bridge itself, in addition to the Kotlin-side defensive layers | **R** |
| 6 | No source mutation; `sourceFilePath` opened read-only | **R** |
| 7 | No path other than `sourceFilePath`/`modelCacheDir` (both request-file-supplied) ever opened for a security-relevant purpose | **R** |
| 8 | `modelIdentity`/`mechanismVersion` populated whenever genuinely available | **R**, when available; **O**, i.e. honestly absent, otherwise |
| 9 | Exact Docling package/version pin, license confirmation, footprint measurement | **C** — required before installation, not performed here |
| 10 | Exact Docling pipeline/model configuration choice | **O** — left to the future implementer, within this document's own bounds |
| 11 | A dedicated, low-privilege OS account for the bridge subprocess; OS/container-level network denial | **O**, recommended — a future, separate deployment-tier decision |
| 12 | A second Python bridge script, or Docling-specific logic outside the one authorized file | **F** |
| 13 | Any tables/headings/hierarchy/bounding-box/per-region-confidence field in the response protocol | **F** |
| 14 | Package installation, model download, or cache mutation performed by the bridge script itself | **F** |
| 15 | The bridge script implementing its own timeout, concurrency limiting, or queueing | **F** |
| 16 | Document Ingestion Tier B routing implementation under this document's own authority | **F** |

---

## 24. Adversarial review (item 18)

| # | Attack | Result |
| --- | --- | --- |
| 1 | Auto-download of a missing model | Foreclosed — §10's two-layer defence (redundant offline flags set by the script itself; a pre-flight local-availability check before any resolution-capable call) |
| 2 | Network fetch by this script's own code | Foreclosed at the script-authorship level — §18's own "no network clients" import prohibition; **not** foreclosed as a structural guarantee against Docling's own transitive dependencies (§10's own disclosed, unresolved limitation, restated, never claimed solved here) |
| 3 | Cache mutation | Foreclosed — §10/§11/§14, this script never writes to the model-cache directory under any condition, reinforced by a deployment-tier read-only mount (a future, separate provisioning decision) |
| 4 | Arbitrary file read | Foreclosed — §5/§6, the only path this script ever opens for security-relevant purposes is `sourceFilePath`, supplied verbatim by the trusted request file; no directory scan, glob, or broad filesystem primitive exists in this contract |
| 5 | Path escape (a crafted `mediaType` or `configurationProfile` value used to construct a path) | Foreclosed — §6, neither field is ever used to construct any path; only `sourceFilePath`/`modelCacheDir` are ever opened, and both are Parker-generated, never evidence-derived |
| 6 | Shell/subprocess abuse | Foreclosed — §5/§18, no shell invocation of any kind is authorized anywhere in this contract |
| 7 | Provider output injection (a malicious document's own content smuggling extra JSON fields or structure into the response) | Foreclosed — §7.5's own strict schema, enforced on the Kotlin side by `parseRecognitionResponse`'s exact allowed-field-set rejection of anything unexpected; this script's own `recognisedText`/`warnings` values are always emitted as ordinary JSON *string* values via `json.dumps`, never string-concatenated into the surrounding JSON structure, so no document content can ever break out of its own string value to inject a sibling field |
| 8 | Stdout contamination (a stray log line corrupting the one JSON value) | Mitigated, not eliminated by this document alone — §7/§19 item 13 name the requirement and the exact proving test; actually achieving it (for example, configuring every transitive library's own default logging handler to write to stderr, or none at all) is implementation work this document specifies the requirement for but cannot itself perform without real code to configure |
| 9 | Stderr explosion | Mitigated — §18's own "stay comfortably under the ~1 MiB cap" requirement, backstopped independently by the Kotlin-side `BoundedStreamDrain` cap regardless of whether this script's own diagnostics stay terse |
| 10 | Malformed JSON accepted downstream | Not applicable to this script's own authorship (the Kotlin-side parser is what would "accept" it, and that parser is already adopted, already strict, already reviewed in a prior turn) — this document's own obligation is that the script *emits* only well-formed JSON matching §7, which §19's own tests must prove |
| 11 | Unicode corruption | Foreclosed by construction — §7.5's own "use `json.dumps`'s default `ensure_ascii=True`" requirement, paired with the already-adopted, already-fixed Kotlin-side `\uXXXX` decoder (verified fresh, §2 above) — this exact failure mode was found and corrected on the Kotlin side during the immediately preceding owner-acceptance-review turn specifically because of this expected Python-side encoding behaviour |
| 12 | Output truncation presented as success | Foreclosed — §13's own explicit "output must fail, never truncate successfully" requirement, backstopped independently by the Kotlin-side post-parse size re-check |
| 13 | Resource-limit bypass (an attacker-crafted document reporting a small declared size but decoding to something enormous) | Foreclosed by the two-stage check (§13, Stage one/Stage two) — a declared-size lie is caught at Stage two, immediately after decode, before any further per-page work proceeds |
| 14 | Page-count bypass | Foreclosed — §13's own "checked immediately after the PDF's own document structure is opened... before any per-page OCR work begins" requirement |
| 15 | Image decompression bomb | Foreclosed by the same two-stage check as #13 — this is the specific attack that check exists to defeat, restated by name |
| 16 | Provenance fabrication (a fabricated version/model-identity string) | Foreclosed by construction — §15's own "never fabricated when unavailable" requirement for both `mechanismVersion` and `modelIdentity`; both must be read from Docling's own genuine, actual reported values or omitted, never invented by this script's own code |
| 17 | Evidence treated as truth (the bridge's own recognition presented as more than a candidate) | Not applicable to this script's own authority — this script constructs no assertion, proposition, or truth claim of any kind; that boundary is enforced structurally by the entire chain above it (Contract Design §2, §8, restated), and this script has no capability to violate it even in principle, since its only output channel is the fixed JSON schema §7 defines |
| 18 | Memory/Knowledge/Evidence-Intelligence authority leakage | Foreclosed by structural absence — this script holds no dependency, reference, or import capable of reaching any Parker-side authority of any kind; it is a standalone CLI process with exactly one input channel (a request file) and one output channel (stdout/stderr), never linked into, and never capable of calling into, any Parker Kotlin code, JVM process, or Parker-side API |

No item resolves to a blocker for adopting this document. Items 2 and
8 remain honestly, partially disclosed limitations — mitigated by
design, not eliminated by this planning-only document, which cannot
itself enforce a deployment-tier, network-policy, or
library-logging-configuration-specific control without writing code,
exactly as the Adapter Plan's own §26 already disclosed for its own,
analogous items on the Kotlin side.

---

## 25. Citation and cross-reference audit

Fresh-checked, this document, after drafting: every `§N` self-reference
verified against this file's own 27 top-level numbered sections (1
through 27, this one being §25) for correct heading position and
direction, using the same automated audit method this session's own
prior OCR Mechanism documents established. Every external citation to
the Docling Authorization, the Adapter Plan, the Unit 12 Scope
Lock/Implementation Plan, `DoclingOcrProviderAdapter.kt`,
`ProcessBuilderDoclingSubprocessInvoker`, `tools/qmd-relevance-bridge.mts`,
and `docker-compose.yml` was verified against a fresh, full re-read of
each performed in this same drafting pass (§2, above) — not carried
forward from any prior turn's own report without re-verification. Every
JSON field name, exit-code value, and environment-variable name this
document freezes for the *Kotlin-facing* half of the contract (§6-§8)
was checked character-for-character against the actual, adopted
`DoclingOcrProviderAdapter.kt` source, not against the Adapter Plan's
own earlier, illustrative prose — the two are consistent wherever
compared, and this document defers to the adopted code in every place
of any residual ambiguity. Every Docling-packaging or Python-runtime
claim (§9, §11, §22) is explicitly flagged as external knowledge, not a
repository fact. Every illustrative name (`tools/docling-ocr-bridge.py`,
every Python variable/function name mentioned) is explicitly marked
illustrative at first use, mirroring the exact discipline the Adapter
Plan already established.

---

## 26. Conflicts or ambiguities

**None found that block adoption.** One genuine, disclosed design
choice, not a conflict: this document's exit-code table (§8) departs
from `tools/qmd-relevance-bridge.mts`'s own single-exit-code
convention — explained in full at §2, above, as a necessary
consequence of the already-adopted Kotlin adapter's own richer
`OcrRecognitionOutcome` mapping requirement, not an inconsistency this
document failed to reconcile. No decision in §5-§19, above, narrows or
widens anything the Docling Authorization, the Adapter Plan, or either
Unit 12 document already fixed; every numeric bound is reused
verbatim, and every field this script must emit maps onto a field the
already-adopted Kotlin parser already, actually, accepts — verified
fresh against the real source file, not assumed.

---

## 27. Files created/modified

Exactly one —
`docs/architecture/OCR_MECHANISM_DOCLING_BRIDGE_SCRIPT_PYTHON_CONTRACT.md`
(new). No other file is created, modified, staged, committed, or
pushed. No dependency is added. Docling is not installed. No model is
downloaded. No runtime/cache is provisioned. No Docker or
runtime-composition file is changed.

---

## Final Recommendation

**READY FOR OWNER REVIEW.**

This document freezes the exact Python-side contract a future
`tools/docling-ocr-bridge.py` implementation must satisfy, checked —
field for field, exit code for exit code — against the real, already-
adopted `DoclingOcrProviderAdapter.kt`/`ProcessBuilderDoclingSubprocessInvoker`
source, not against aspirational prose. It reuses
`tools/qmd-relevance-bridge.mts`'s own established stdout/stderr,
argv, and local-availability-pre-flight conventions wherever they
transfer, and explains, rather than silently diverges from, the one
place this contract's own requirements (a four-way exit-code
distinction) exceed what that precedent needed. It freezes every
numeric resource bound the Docling Authorization and Adapter Plan
already adopted, without re-deriving or loosening any of them. It
does not implement the bridge, install Docling, download a model,
provision a runtime or cache, touch Docker or runtime composition,
begin Unit 12's own composition wiring, or begin Document Ingestion's
own Tier B — each remains exactly as separately governed as before,
named precisely (§20) so a future implementer knows exactly what
remains to be done, and by whom.
