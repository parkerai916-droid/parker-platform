# OCR Mechanism — Docling Concrete Adapter Implementation Plan

## Status

**Draft for owner review. Governance/implementation-planning only — no
Kotlin, Python, or shell code is implemented, proposed as a diff, or
changed by this document. No dependency is added. Docling is not
installed. No model is downloaded. No runtime/cache is provisioned.
Neither `src/` nor `tests/` is touched. No fixture is added or
modified. No `docker-compose.yml`, `Dockerfile`, or deployment
configuration is changed. Nothing is staged, committed, or pushed.**

**Repository baseline confirmed fresh before drafting:** `main` at
`9172893353298d8452be7518569346fbb98343cd`, working tree clean.

This document designs, and only designs, the concrete
`DoclingOcrProviderAdapter` implementation
`docs/architecture/OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md`
("the Docling Authorization") authorized to be proposed. It does not
reopen provider selection, does not reopen the Docling Authorization's
own boundaries, does not redesign Unit 12's own composition wiring
(`docs/architecture/OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md`,
`docs/architecture/OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md`), and
does not begin Document Ingestion Tier B. Every numeric bound below is
reused verbatim from the Docling Authorization's own §6, never
re-derived.

---

## 1. Purpose

Determine the minimum concrete adapter architecture a future
implementation unit would build, so that:

1. `DoclingOcrProviderAdapter` (illustrative name) can be proposed with
   an exact, reviewable process model, input/output mapping,
   provenance shape, resource-enforcement plan, failure taxonomy, and
   security posture.
2. A future, separate provisioning unit has an exact, minimal
   deployment-footprint specification to build against.
3. A future test-implementation unit has a concrete, non-tautological
   test strategy, split correctly between fake-adapter contract tests
   (always run) and real-Docling live-acceptance tests (opt-in, gated
   on provisioning).

This document does not itself perform any of the above.

---

## 2. Authorities inspected fresh (this document)

**Already fully inspected in the three immediately preceding turns,
re-confirmed unchanged by fresh `git log`/`git diff --stat` before this
document's own drafting (no content drift found in any of the six):**
`OCR_MECHANISM_DOCLING_PROVIDER_AUTHORIZATION_SCOPE_LOCK.md` (`c5b7aad`),
`OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md` (`0be393c`),
`OCR_MECHANISM_UNIT_12_IMPLEMENTATION_PLAN.md` (`9172893`, the
corrected version), `OCR_MECHANISM_CONTRACT_DESIGN.md`,
`OCR_MECHANISM_SCOPE_LOCK.md`, `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`,
`DOCUMENT_INGESTION_ROUTING_AND_COMPLETENESS_POLICY.md`,
`DOCUMENT_INGESTION_CANONICAL_GOVERNANCE_ALIGNMENT.md`, CDR-007, CDR-008.

**Freshly inspected for the first time in this document, not carried
forward from any prior turn:**

- `src/interfaces/OcrProviderAdapter.kt`, `src/interfaces/OcrMechanism.kt`,
  `src/runtime/OcrExecutionSequencer.kt` — re-confirmed unchanged (§2 of
  the Unit 12 Implementation Plan's own fresh read, itself re-confirmed
  by `git diff --stat` returning nothing for any of the three).
- **`src/runtime/QmdRelevanceMechanism.kt` (full, 660 lines) — the
  single most load-bearing precedent this document relies on.** An
  already-accepted, production, local-subprocess-backed capability
  mechanism (Programme 3 Unit 9.7.3), structurally analogous to what
  `DoclingOcrProviderAdapter` must become: `QmdRelevanceMechanism`
  (implements `RelevanceMechanism`, two dependencies —
  `QmdRelevanceMechanismConfiguration`, `QmdSubprocessInvoker`);
  `QmdSubprocessInvoker` (a `fun interface`, "the entire subprocess-call
  seam," swappable between a real invoker and an in-memory test fake);
  `ProcessBuilderQmdSubprocessInvoker` (the real, production invoker) —
  writes a request to a fresh temp file (deleted in `finally`,
  unconditionally), launches `ProcessBuilder(command: List<String>)`
  (never a shell string), drains stdout/stderr concurrently on daemon
  threads (avoiding pipe-buffer deadlock), enforces timeout via
  `Process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)` +
  `destroyForcibly()` on expiry, never throws from `invoke()` itself
  (a distinguished result object instead, letting the calling
  mechanism decide how each fault surfaces); a minimal, hand-rolled,
  deliberately strict JSON parser (no external JSON library
  dependency), rejecting any response with a missing or unexpected
  field. Every one of these decisions is reused, by direct analogy,
  for `DoclingOcrProviderAdapter` below (§5-§14).
- `tools/qmd-relevance-bridge.mts` (header comment, full) — the
  production bridge-script precedent: one-shot, disposable per
  invocation; explicit local/offline-model-resolution pre-flight check
  (`resolveModelFile(..., { download: false })`) performed *before*
  any code path capable of a live download is reachable; fails loudly,
  never partially.
- `docker-compose.yml` (QMD section, full) — the exact, already-accepted
  deployment convention this document's own §18 mirrors: fixed
  in-container paths; a host-owned, read-only-mounted model-cache
  volume (`/opt/qmd-models:ro`); explicit, disclosed statement that
  "this container never installs, builds, downloads, or otherwise
  provisions QMD or its embedding model itself: both remain wholly
  owned and maintained on the host."
- `src/composition/ParkerRuntimeConfig.kt` (QMD-related KDoc, targeted
  read) — confirmed the exact configuration-threading convention:
  `qmdNodeExecutablePath`, `qmdBridgeScriptPath`, `qmdTsxCliPath`,
  `qmdModelCacheDir` as explicit, individually-documented constructor
  parameters, each mapped from a `PARKER_QMD_*` environment variable
  one layer further out (not itself re-read in full here — its own
  QMD precedent is sufficient to fix the naming convention this plan
  reuses).
- `tests/integration/QmdRelevanceMechanismLiveAcceptanceTest.kt` — **not
  in the ordinary `test` source set.** Confirmed via `build.gradle.kts`
  (`val liveModelEvaluation by sourceSets.creating`, lines ~105-214):
  QMD's own real-subprocess acceptance tests live in a dedicated,
  separate Gradle source set with its own dedicated test tasks, never
  run as part of `./gradlew test`, run only when explicitly invoked —
  the exact, already-accepted precedent this plan's own §19 test
  strategy reuses for "real local Docling adapter tests once
  provisioned."
- Fresh repository-wide search (this pass): no Python file, no
  `requirements.txt`/`pyproject.toml`/`Pipfile`, and no Docling
  reference of any kind exist anywhere outside `docs/` — re-confirmed,
  consistent with every prior turn's own identical finding.
- `scripts/run-owner-ui.sh` — confirmed the repository's only other
  runtime-launcher script; not directly relevant to a bridge-script
  design, noted only to confirm no existing Python-launcher convention
  exists to instead reuse.

**External knowledge about Docling itself, explicitly flagged as such
— not verified against this repository, because nothing Docling-related
exists in it yet.** Docling (an open-source, Python-distributed
document-conversion/layout/OCR toolkit) is, as of this document's own
drafting, characteristically: installed via `pip`; dependent on a
deep-learning backend (historically PyTorch) and its own pretrained
layout/table/OCR model checkpoints; capable of CPU-only inference
(slower than GPU, not incorrect); historically resolves its own model
weights through the Hugging Face Hub ecosystem, which **fetches from
the network by default on first use** unless explicitly configured
for offline/local-only resolution (`HF_HUB_OFFLINE`/`TRANSFORMERS_OFFLINE`-style
environment variables, or an equivalent explicit local-path
configuration, depending on Docling's exact release). **Every specific
version number, exact package name, exact environment-variable name,
and exact model-checkpoint identity named below is illustrative and
must be independently reconfirmed by whoever actually implements this
plan, against Docling's own current released documentation at that
time** — this document freezes the *architecture* Docling's own
well-established shape requires, not a pinned, potentially-stale
version manifest.

---

## 3. Current Docling implementation state

**Nothing exists yet.** Zero Docling-related files, dependencies, or
configuration anywhere in this repository (§2, above). The Docling
Authorization exists (governance only); Unit 12's own composition
design exists (planning only); no code implementing either has been
written.

---

## 4. Adapter architecture — overview

`DoclingOcrProviderAdapter` (implements `OcrProviderAdapter`, the
already-frozen, unmodified interface) mirrors `QmdRelevanceMechanism`'s
own already-accepted shape exactly, substituted for Docling/OCR in
place of QMD/relevance:

```
DoclingOcrProviderAdapter (implements OcrProviderAdapter)
  ├─ configuration: DoclingOcrProviderAdapterConfiguration (immutable)
  └─ invoker: DoclingSubprocessInvoker (fun interface — "the entire subprocess-call seam")
       └─ real production: ProcessBuilderDoclingSubprocessInvoker
       └─ test-only fake: hand-written, in-memory, never spawns a process
```

Every Kotlin/Python name below is **illustrative, not frozen** —
Contract Design §11's own "exact Kotlin names... remain a future
Implementation Plan's own responsibility" discipline, applied
identically to the Python bridge script's own name.

---

## 5. Exact adapter boundary (item 1)

**Unchanged from the already-frozen contract — this plan introduces no
widening.**

- **What Parker passes in:** exactly `OcrRecognitionRequest`
  (`sourceEvidenceId: EvidenceArtifactId`, `content: ByteArray`,
  `mediaType: String`, `pageCount: Int?`) — already-retrieved,
  already-manifest-verified bytes only (Unit 12 Implementation Plan
  §5.G), never a path, never a caller-supplied confidence or
  evidential value.
- **What the adapter returns:** exactly `OcrRecognitionOutcome` — one
  of the nine already-frozen variants (§13, below).
- **What it must never expose:** any Docling-specific type, exception
  class, JSON shape, model identifier format, or file-path value, into
  any type outside `DoclingOcrProviderAdapter.kt` and its own
  co-located, `internal`/`private` helper types — mirroring
  `TikaEvidenceExtractor`'s own confinement discipline exactly, per
  the Docling Authorization's own §5. `OcrRecognitionIdentity.mechanismIdentity`
  is the sole, deliberate exception: the Docling Authorization's own
  §6 requires this field, specifically, to name Docling itself ("the
  first time this field is ever populated with a real, non-illustrative
  value") — reconciled explicitly, not silently, with
  `OcrMechanism.kt`'s own KDoc phrase "a provider-neutral label, never
  a concrete engine name": that phrase governs the *type definition's*
  own documentation (the field must not be typed or explained in terms
  of one specific engine), not the *runtime value* an authorized
  adapter populates — the field's entire purpose (Provenance Model §2's
  own "plugin, parser/mechanism and adapter identities... required")
  is defeated if an authorized adapter cannot honestly name itself.

---

## 6. Process/runtime model (item 2)

**Local subprocess, one Python bridge process per `recognise` call —
not in-process JVM, not a persistent local service.** Chosen on
governance-consistency grounds, not convenience:

- **In-process JVM library — rejected.** No JVM/Kotlin binding for
  Docling exists (Docling is a pure-Python distribution); embedding a
  Python interpreter inside the JVM process (for example, via GraalPy
  or a JNI bridge) would introduce a materially new, unprecedented
  runtime-embedding technology this repository has never used anywhere,
  a far larger and less-reviewed surface than a subprocess boundary
  Parker already has one accepted precedent for.
- **Persistent local service (a long-running Docling server process or
  local socket) — rejected.** Would require a new lifecycle/background-process
  concept — the Unit 12 Scope Lock's own "no new lifecycle/background
  behaviour" boundary (restated at Docling Authorization §6, "No
  automatic invocation") would require its own separate authorization
  for exactly this kind of standing process, not yet obtained; it also
  breaks the "disposable state" discipline `QmdRelevanceMechanism`'s
  own Frozen Boundary #10 already established for a directly
  comparable, model-heavy local mechanism, and introduces a
  longer-lived attack surface (a listening socket/held file
  descriptors) a one-shot process does not have.
- **Python worker — adopted, as a one-shot subprocess.** Exactly
  mirrors `QmdRelevanceMechanism`/`ProcessBuilderQmdSubprocessInvoker`/`tools/qmd-relevance-bridge.mts`'s
  own already-accepted shape (§2, above), substituting a Python
  interpreter and a Docling-invoking script for Node and a QMD-invoking
  script. **Disclosed trade-off, not hidden:** a fresh process means
  Docling's own model weights are loaded from disk on every single
  invocation, a real, non-trivial cost this document does not deny —
  accepted because (a) concurrency is already bounded to exactly one
  invocation at a time (§10, below), so no warm-pool contention
  question exists; (b) the 15-minute timeout (§10, below) already
  budgets generously for this; (c) it avoids every risk a persistent
  process would introduce, listed above; (d) it is the same trade-off
  this repository has already, knowingly, accepted for QMD's own
  comparable, model-loading-heavy mechanism.

---

## 7. Docling packaging/runtime requirements (item 3)

**Illustrative, external-knowledge-based, to be reconfirmed at
implementation time (§2, above):**

- Python interpreter, version to be pinned by the future implementer
  against Docling's own current minimum-supported-version statement.
- The `docling` package and its own transitive dependencies (a
  deep-learning backend, image-handling libraries, and Docling's own
  pretrained-model packages), installed into a dedicated Python virtual
  environment — never the host's system Python — mirroring the
  isolation discipline this repository already applies to every other
  externally-sourced runtime dependency.
- CPU-only inference is feasible (slower, not incorrect) — no GPU
  requirement for correctness; a future provisioning unit may choose
  CPU-only specifically to avoid the additional driver/hardware
  dependency surface a GPU requirement would introduce, consistent
  with this plan's own minimal-footprint discipline.
- No JVM-side native library requirement (the JVM never loads Docling
  directly, §6, above); whatever native libraries Docling's own Python
  dependencies require (image codecs, tensor-math libraries) are
  confined entirely to the Python virtual environment/subprocess, never
  the JVM process.
- Model/cache directories: a fixed, pre-populated, read-only-mounted
  directory (§9, below) — never created, written to, or populated by
  the adapter or the bridge script themselves.
- Startup behaviour: model loading at bridge-script start is the
  expected, accepted heavy cost (§6, above).
- **Network activity by default: yes, unless explicitly disabled** —
  Docling's own model-resolution layer, left at its own defaults,
  characteristically attempts a network fetch the first time a given
  model is needed and not already locally cached. This is the single
  most consequential packaging fact driving §8, below.

---

## 8. No-network enforcement (item 4)

**Defence in depth — application-level configuration plus, where the
deployment permits it, OS/network-level denial — because
application-level configuration alone cannot be proven sufficient by
this document.**

- **How runtime execution is guaranteed offline (application level):**
  the bridge script must set Docling's own offline/local-only
  resolution mode (illustratively, `HF_HUB_OFFLINE=1`/`TRANSFORMERS_OFFLINE=1`-equivalent
  environment variables, or Docling's own explicit local-path model
  loading API, whichever its current release actually exposes) *before*
  importing or invoking any Docling code capable of triggering model
  resolution — mirroring `tools/qmd-relevance-bridge.mts`'s own
  `resolveModelFile(..., { download: false })` pre-flight discipline
  exactly: the check happens before the code path capable of a live
  download is ever reachable, never as an after-the-fact catch.
- **How a successful, persisted download is structurally prevented as
  a second, independent layer (provisioning-adjacent) — corrected for
  precision, not merely restated:** the model-cache directory Docling
  reads from is mounted read-only (§9, below). This does **not**, by
  itself, prevent an outbound network *request* from being attempted —
  a read-only mount blocks only the *write* half of a fetch (persisting
  a downloaded file to that directory); it does not block the HTTP
  request itself, and some Hugging-Hub-style client libraries may still
  use fetched bytes in memory even when unable to cache them to disk.
  **The genuine guarantee against the request itself being attempted at
  all rests on the application-level offline-mode configuration above,
  or, more strongly, on the recommended OS/network-level denial below**
  — this read-only mount is a real, additive hardening layer against
  cache *corruption/tampering* specifically (§18, below), not an
  independent proof that zero network traffic can occur. Stating this
  precisely, rather than implying the mount alone forecloses any
  request, is the correction this review pass makes.
- **What happens if required assets are absent:** the bridge script
  performs its own pre-flight existence check against the expected,
  fixed model-cache path *before* invoking Docling's own model-loading
  code — mirroring the QMD bridge's own identical discipline — and, on
  a missing asset, exits non-zero with a structured, diagnosable
  message (§16, below) rather than ever reaching a code path capable
  of a live download.
- **Recommended, not code-enforced, deployment-tier hardening (§18,
  below):** running the bridge subprocess under an OS/container
  network policy that denies outbound network access outright (for
  example, a dedicated network namespace, or the same "no network
  access" posture `docker-compose.yml`'s own existing QMD section
  already achieves by never exposing the container to any port
  Docling could reach a remote host through) — named here as a
  provisioning-unit recommendation, not designed or enforced by this
  Kotlin/Python-code-level plan, and explicitly not claimed as already
  sufficient by application-level configuration alone.

---

## 9. Model/cache provisioning boundary (item 5)

**Mirrors `docker-compose.yml`'s own QMD section exactly, substituting
Docling's own equivalents:**

- **What must exist before adapter execution:** the Python virtual
  environment with Docling installed; Docling's own pretrained model
  files, pre-downloaded once, outside this adapter's own execution
  path entirely, and cached at a fixed, known filesystem location
  (illustratively, `/opt/docling-models`, mirroring `/opt/qmd-models`).
- **What this adapter may read:** that fixed model-cache directory
  (read-only), and the one, fresh, Parker-controlled temporary file it
  writes for the current invocation's own source bytes (§12, below).
- **What it may never download or mutate automatically:** the model
  cache directory, the Python virtual environment itself, and the
  Docling package installation — all mounted or installed read-only
  from the adapter's own runtime perspective, exactly mirroring the
  QMD precedent's own `:ro` mount.
- **What remains a separate provisioning unit:** the actual
  installation of the Python interpreter, the virtual environment, the
  Docling package, and the one-time (network-requiring) model download
  itself — explicitly, entirely outside this document's own authority
  (§20, below) and outside the running Parker container's own
  execution path, mirroring `docker-compose.yml`'s own explicit,
  already-adopted statement: *"This container never installs, builds,
  downloads, or otherwise provisions QMD or its embedding model
  itself: both remain wholly owned and maintained on the host."* The
  same sentence, substituting Docling for QMD, is the exact boundary
  this plan freezes and does not cross.

---

## 10. Input mapping (item 6)

- **Scanned PDFs, PNG/JPEG image evidence:** both already-authorized
  media types (Unit 12 Implementation Plan §5.J's own illustrative
  `application/pdf`/`image/*` eligibility convention); the adapter
  itself performs no media-type judgement of its own — it receives
  whatever `mediaType` the coordinator already determined eligible.
- **Already-custodied bytes only:** `OcrRecognitionRequest.content`,
  never independently fetched, retrieved, or re-read by this adapter —
  the same "pure callee" discipline every prior OCR Mechanism unit
  already established, unchanged.
- **Temporary-file semantics:** Docling's own Python API most naturally
  operates on a filesystem path (or an in-memory stream, depending on
  its current release — the future implementer confirms which);
  because the bridge boundary is a subprocess, the safest, most
  Docling-version-independent choice is to write `content` to one
  fresh, Parker-controlled temporary file before invoking the bridge
  script, mirroring `ProcessBuilderQmdSubprocessInvoker`'s own
  `Files.createTempFile`/`Files.writeString` pattern, extended here to
  binary content rather than a JSON request body.
- **Secure cleanup:** the temporary source-bytes file is deleted in a
  `finally` block, unconditionally, on every path — success, every
  failure category (§13, below), and timeout alike — mirroring the QMD
  precedent's own identical `finally { Files.deleteIfExists(...) }`
  discipline exactly.
- **No source mutation:** the adapter writes a *copy* of already-retrieved
  bytes to its own private temporary file; it never references,
  reopens, or touches the original custodied artefact's own storage
  location at any point — custody itself remains untouched by
  construction, since this adapter holds no `EvidenceCustodian`
  reference of any kind (Docling Authorization §6, restated).

---

## 11. Output mapping (item 7)

**Only fields Docling can truthfully provide — nothing fabricated,
nothing smuggled in from Docling's own broader structured-conversion
output:**

| `OcrRecognitionResult` field | Source | Notes |
| --- | --- | --- |
| `recognisedText` | Docling's own plain recognised-text output | Never Docling's own richer markdown/structured export — that capability is explicitly out of scope (Docling Authorization §5: "interpreting... into recognised text, and nothing more") |
| `fidelity` | A construction-time judgement the bridge script/adapter makes, not a value Docling itself labels | Illustrative default: `VERBATIM` for direct character-level recognition, downgraded to `NORMALISED`/`INFERRED_RECONSTRUCTION` per Docling's own confidence signal where genuinely available and low; the exact rule is implementation-plan-level judgement, not frozen here, since it depends on Docling's own actual confidence-reporting shape at implementation time |
| `identity` | `OcrRecognitionIdentity(mechanismIdentity = "docling", configurationProfile = <bridge protocol version + pipeline configuration>, mechanismVersion = Docling's own reported package version)` | §5's own reconciliation applies |
| `confidence` | Only if Docling reports a genuine, single, document-level scalar in `0.0..1.0`; `null` otherwise | Never averaged or fabricated from per-region figures if no genuine document-level figure exists |
| `recognisedAt` | `Instant.now()` at the point the bridge script/adapter completes | Never a Docling-internal timestamp |
| `warnings` | Any non-fatal condition Docling itself discloses (for example, "page N partially processed") | Passed through as plain text, order preserved |
| `segments` | One `OcrRecognitionSegment` per page/region Docling naturally processes, each carrying its own `text`/`fidelity`/`pageNumber` | Maps naturally, since Docling already processes multi-page PDFs page-by-page |

**Explicitly never mapped, discarded regardless of whether Docling's
own API surfaces it:** coordinates/bounding boxes (no field exists in
the already-frozen `OcrRecognitionSegment` shape to carry one, and
none is added — Unit 12 Scope Lock §12's own "not authorised"); tables,
headings, hierarchy, or any other structured-document-conversion
output (Docling Authorization §5); per-word confidence maps; layout
classification labels. Any such data Docling's own API returns is
read, and immediately discarded, by the bridge script or the adapter —
never forwarded into any `OcrRecognitionResult` field, and never
smuggled into `warnings` as a workaround.

---

## 12. Provenance (item 8)

- **Docling identity:** `mechanismIdentity = "docling"` (§5, above).
- **Docling version:** captured from Docling's own reported package
  version (illustratively, its own `__version__` attribute), included
  in the bridge script's own JSON response, folded into
  `mechanismVersion`.
- **Adapter version:** `OcrRecognitionIdentity` has no dedicated field
  for a Parker-owned adapter/bridge-script version distinct from the
  provider's own version — folded into `configurationProfile`
  (illustratively, a string such as `"docling-bridge-v1"`), since that
  field's own already-frozen purpose ("a named tag identifying the
  specific configuration in force") already accommodates it; not a new
  field, no contract widening.
- **Model identity/version:** Docling's own underlying model checkpoint
  identity (which layout/OCR model set was actually loaded), folded
  into `configurationProfile` alongside the adapter/bridge-script
  version, in whatever compact, structured-but-still-a-single-string
  form the future implementer finds clearest — mirroring
  `ExtractionIdentity`'s own "one structured record" precedent applied
  within the already-frozen three-flat-string-field shape, not a
  reason to widen it.
- **Configuration identity:** the bridge protocol version plus whatever
  Docling pipeline options were actually selected (CPU-only, which
  model set) — same field as above.
- **Transformations, warnings, execution timestamps, genuine
  confidence:** already covered in §11, above; nothing further to add
  here. Whatever future, separate `CandidateProvenance`/`CandidateAssertion`
  construction eventually reads this disclosure (Unit 12 Implementation
  Plan §5.Q) is not designed by this document — only required to remain
  honestly possible from the fields this adapter honestly populates.

---

## 13. Resource enforcement — exact layer per bound (item 9)

| Bound (Docling Authorization §6, verbatim) | Enforcing layer |
| --- | --- |
| 64 MiB maximum source bytes | Primarily the coordinator tier (`EvidenceIntelligenceOcrCoordinator`, Unit 12 Implementation Plan §5.G/N), using the manifest's own `byteLength`, *before* the adapter is ever invoked; the adapter/bridge script may defensively re-check the temp file's own size as a second, independent layer, never the sole one |
| 200 PDF pages | Inside the bridge script (Python) — page count is only knowable once Docling begins reading the document's own structure; checked immediately after document open, before any per-page OCR work begins, aborting with a distinct, structured error on breach |
| 10,000 × 10,000 px / 100 MP | **Two-stage, not single-stage — corrected during this review pass.** Stage one, *before* full decompression: the bridge script inspects the image/page's own *declared* dimensions (most image/PDF libraries expose header-level width/height without fully decoding pixel data) and rejects outright if the declared size alone already exceeds the bound — the genuine decompression-bomb defence, since checking only *after* full rasterisation would already be too late (the memory exhaustion the bound exists to prevent happens *during* decode itself). Stage two, immediately after rasterisation/decode and before OCR runs on that page, re-checks the *actual* decoded size as a second, independent layer against a declared size that under-reported the true content — aborting with a distinct, structured error at whichever stage catches it, never silently downsampling or cropping |
| 20 MiB recognised-text output | Two independent layers: the bridge script accumulates recognised-text byte length as pages complete and aborts early on breach (defense in depth); the Kotlin adapter additionally caps how many bytes of subprocess stdout it will ever buffer (§14, below — the same cap doubles as a security control) |
| 15-minute independent wall-clock timeout | `ProcessBuilderDoclingSubprocessInvoker`'s own `Process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)`, mirroring `ProcessBuilderQmdSubprocessInvoker` exactly — **never** page-count-reconciled (§14, below) |
| Exactly 1 concurrent Docling invocation per Parker instance | A single-permit `kotlinx.coroutines.sync.Mutex`, held for the duration of one `invoke()` call, acquired by `EvidenceIntelligenceOcrCoordinator` (or the adapter itself — an implementation-level choice) before invoking, released in a `finally` block |

---

## 14. Timeout mechanics (item 10)

**Mirrors `ProcessBuilderQmdSubprocessInvoker` exactly, with one
disclosed, honest caveat this precedent did not need to address:**

1. `process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)` returns
   `false` on expiry.
2. `process.destroyForcibly()` is called immediately — the JVM's own
   forcible-termination signal, not the gentler `destroy()`, which a
   hung or misbehaving process could ignore.
3. Any stdout/stderr already buffered on the concurrent daemon-thread
   readers is drained with a short, bounded `join(...)`, mirroring the
   QMD precedent's own identical shape.
4. A distinguished, `timedOut = true` result is returned; the Kotlin
   adapter maps this to `OcrRecognitionOutcome.ProcessingOrDependencyFailure`
   with an honest, non-blank "timed out after {N}ms" reason — never a
   silently truncated `Recognised`, matching the Unit 12 Implementation
   Plan's own already-fixed mapping exactly.
5. The temporary source-bytes file is still deleted in `finally`,
   regardless of the timeout (§10, above).

**Disclosed caveat, not silently assumed away: `destroyForcibly()`
terminates the direct child process (the Python interpreter) but does
not, by itself, guarantee termination of any further descendant
processes that interpreter may itself have spawned** (Python's own
deep-learning stack has a real, known propensity for worker-process
forking — for example, data-loading or multiprocessing pools). **No
zombie process** is only fully guaranteed if the bridge script itself
avoids spawning such descendants, or if the invoker additionally
destroys the *entire process group/tree* rather than only the direct
child — a hardening technique (for example, launching via a
process-group-creating wrapper and signalling the group, or using
`ProcessHandle.descendants()` on Java 9+ to walk and destroy the whole
tree) this document *recommends*, but does not mandate as a frozen,
single mechanism, since the correct technique depends on the exact
process topology Docling's own current release actually creates —
confirmed by the future implementer, not assumed here. This is named
explicitly as a required verification step for that future unit
(§20/§22, below), mirroring `QmdRelevanceMechanism`'s own established
discipline of disclosing a known, non-blocking operational limitation
rather than silently omitting it.

---

## 15. Concurrency-one enforcement (item 11)

- **Exact strategy:** a single-permit `Mutex` (or equivalently, a
  `Semaphore(1)`), held around the entire invocation (temp-file write
  through subprocess completion/timeout through cleanup) — not merely
  around the `ProcessBuilder.start()` call itself.
- **Behaviour on concurrent attempts:** a second, concurrent
  OCR-eligible `analyseEvidence` call simply suspends on the mutex
  until the first invocation completes — ordinary, predictable FIFO
  suspension, never a silently dropped or silently queued-forever
  request. **Disclosed trade-off:** if the second caller's own
  15-minute timeout budget is measured from the moment it *attempted*
  invocation rather than from the moment it actually *acquired* the
  mutex and began, a long first invocation could consume most or all
  of a queued second caller's own budget through no fault of Docling's
  own execution time. This document recommends starting each
  invocation's own timeout clock only once the mutex is actually
  acquired — an implementation-level choice, not re-litigated here
  further.
- **Restart behaviour:** the mutex is an ordinary in-memory object,
  freshly constructed on every `ParkerRuntime` startup — no persistent
  lock file, no cross-process-restart deadlock risk. The
  concurrency-one guarantee is scoped to one running Parker instance's
  own lifetime, exactly as the Docling Authorization's own §6 already
  frames it ("per Parker instance"). A Parker restart while an
  invocation is in flight orphans that one subprocess to the OS
  (mitigated by the timeout, §14, above, on the *next* attempt's own
  fresh mutex, though the orphaned process itself is not this
  document's own concurrency control's responsibility to clean up —
  a disclosed, narrow, non-blocking gap named for the future
  implementer's own awareness).

---

## 16. Temporary-file handling (item 12)

- **Creation location:** a dedicated, explicitly-configured
  Docling-temp-file root (illustratively,
  `doclingTempDir: String? = null`, mirroring
  `QmdRelevanceMechanismConfiguration.modelCacheDir`'s own nullable,
  explicit-or-default-to-JVM-temp shape) — preferred over the bare
  JVM system temp directory for tighter, Parker-controlled permissions
  and cleanup auditability, though falling back to the JVM default
  remains a lawful, simpler starting choice for a first implementation.
- **Permissions:** created owner-only-readable/writable (a stricter
  requirement than the QMD precedent needed, since OCR input is raw
  evidence content, materially more sensitive than QMD's own candidate
  text) — an explicit, new requirement this plan adds, not present in
  the QMD precedent it otherwise mirrors.
- **Filename/path privacy:** `Files.createTempFile`'s own default
  random-suffix naming is sufficient — the evidence artefact's own
  identifier or original filename must never appear in the temp
  file's own name (mirroring the Manifest Retrieval Scope Lock's own
  "filename is metadata, never a storage path" discipline, restated
  here for a different boundary).
- **Cleanup on success/failure/timeout:** unconditional, `finally`-block
  deletion on every path, mirroring §10/§14, above.
- **No durable source path leakage:** the temp file's own path is never
  echoed into `OcrRecognitionResult`/`OcrRecognitionOutcome` — no field
  exists to carry one, and none is added (§5, above).

---

## 17. Failure semantics (item 13)

Every case below maps onto one of the nine already-frozen
`OcrRecognitionOutcome` variants (Unit 1/7, unmodified) or an ordinary
thrown exception — never a new variant, never collapsed into another:

| Case | Disposition |
| --- | --- |
| Runtime unavailable (Python/venv missing) | Subprocess start failure, distinguished `exitCode = -1` result (mirroring QMD's own precedent) → `OcrRecognitionOutcome.ProcessingOrDependencyFailure` |
| Model/cache missing | Bridge script's own pre-flight check (§8, above) → structured non-zero exit → `ProcessingOrDependencyFailure` |
| Unsupported source | `OcrRecognitionOutcome.UnsupportedOrInaccessibleInput` |
| Malformed source (corrupt PDF/image) | Same — Docling itself fails to decode it |
| Provider startup failure | `ProcessingOrDependencyFailure` |
| Recognition failure (ran, found nothing usable) | `NoRecognisableContent` (empty) or `PartialOrDegradedOutput` (partial, carrying the actual partial result, never discarded) |
| Timeout | `ProcessingOrDependencyFailure`, honest "timed out" reason (§14, above) |
| Resource rejection (page/pixel/output breach) | `ProcessingOrDependencyFailure`, naming the specific bound breached |
| Malformed provider output (bridge JSON does not match the expected shape) | **A thrown exception, propagating unchanged** — mirroring `QmdRelevanceMechanism`'s own `require(...)`-throws discipline exactly; a malformed bridge response is a Parker-owned implementation defect, never a legitimate Docling disclosure. An implementer may instead choose to catch and represent this as `GenuineImplementationFault` if preferred (that variant's own KDoc explicitly permits either choice) — but never silently discarded |
| Output too large | `ProcessingOrDependencyFailure` |
| Process crash (non-zero exit, no timeout, no structured error) | An uncaught, propagating exception, mirroring QMD's own `check(invocation.exitCode == 0)` discipline — unless the bridge script's own structured-error convention (recommended, §20 below) classifies it, in which case the matching variant above applies instead |
| Cleanup failure (temp-file deletion itself fails) | Caught and logged, never masks or replaces the primary result/exception — mirrors `ParkerRuntime.shutdown`'s own established "best-effort, not fail-fast" cleanup discipline |

`NotAuthorised` and `ValidationRejection` remain, as for every prior
OCR Mechanism unit, never constructed by any code path this plan
designs.

---

## 18. Security review (item 14)

- **Command injection:** structurally foreclosed — `ProcessBuilder(command:
  List<String>)`, never a shell string, mirroring QMD's own identical
  protection; no shell is ever invoked to parse a combined command.
- **Path injection:** all paths (temp file, model cache, bridge script)
  are Parker-generated or Parker-configured constants — never derived
  from evidence-supplied strings (media type, any filename).
- **Environment-variable injection:** the subprocess's own environment
  is explicitly, minimally constructed (fixed, Parker-set constants
  including the offline-mode flags, §8 above) — never populated from
  evidence content.
- **Untrusted metadata:** `mediaType` and any filename-adjacent value
  are treated as opaque, display-only strings only, never interpreted
  as a path, command, or format specifier.
- **Malicious PDFs/images:** the real attack surface is Docling's own
  parsing libraries; mitigated by the already-established size/page/pixel
  bounds (§13, above, bounding the attack's own resource envelope),
  subprocess isolation itself (a compromised Python process cannot
  directly corrupt the JVM's own memory), and a recommended,
  deployment-tier, dedicated low-privilege OS user for the bridge
  process (§20, below) — not code-enforced by this Kotlin/Python layer
  alone.
- **Subprocess isolation:** recommended, dedicated, minimal-privilege
  OS account, distinct from the account running the main Parker JVM
  process — a deployment recommendation (§20, below), not designed in
  code here.
- **Model-file tampering / cache tampering:** foreclosed at the mount
  level — the model-cache directory is read-only from the adapter's
  own perspective (§9, above); genuine integrity verification of the
  model files themselves (checksums) is a provisioning-unit concern,
  named as a recommendation, not designed here.
- **stdout/stderr limits — a genuinely new consideration beyond the
  QMD precedent, disclosed explicitly:** QMD's own stdout is tiny
  (token/score JSON); Docling's own stdout, carrying full recognised
  document text, could legitimately be large, and a compromised or
  malfunctioning subprocess could produce an even larger, runaway
  stream. The Kotlin-side concurrent reader must impose a hard cap on
  buffered bytes — illustratively, a stdout cap slightly above the
  already-established 20 MiB output bound (accommodating JSON
  framing overhead) and a separate, materially smaller stderr cap
  (illustratively, 1 MiB, since stderr should only ever carry
  diagnostic text) — aborting the read (and, if not already complete,
  destroying the subprocess) on breach, never silently growing an
  unbounded JVM-heap buffer.

---

## 19. Adapter result semantics (item 15)

**Maps onto the existing `OcrProviderAdapter`/`OcrMechanism` contracts
without widening them.** No genuine incompatibility was found during
this design pass — every input this plan needs (`sourceEvidenceId`,
`content`, `mediaType`, `pageCount`) and every output category this
plan needs (recognised text, fidelity, identity, confidence,
timestamp, warnings, segments, and the nine-way failure taxonomy) is
already present in the already-frozen Unit 1/6/7 shapes (§2, above,
confirmed by fresh re-read). No new public type, no new field, and no
new `OcrRecognitionOutcome` variant is proposed anywhere in this
document.

---

## 20. No authority widening (item 16)

Confirmed, one by one, each by structural absence of the capability
required to violate it — mirroring this session's own "structural
impossibility, not policy" discipline throughout:

- **Write Memory Core:** foreclosed — `DoclingOcrProviderAdapter` holds
  no `MemoryCore` reference, directly or transitively; only
  `EvidenceIntelligenceOcrCoordinator`'s own already-designed, separate
  mapping step (Unit 12 Implementation Plan §5.R/X) ever reaches
  `MemoryCore`, and only through the existing, gated
  `CandidateRecordProduced` leg.
- **Create Knowledge:** foreclosed — same reasoning, §5.Y.
- **Create `DerivativeGenerationRecord`:** foreclosed — no dependency
  on `DerivativeGenerationStorage`/`DocumentIngestionAudit` anywhere in
  this design (§5.AA); Tier B output never enters that pipeline at
  all, restated from the Docling Authorization's own §6.
- **Invoke Evidence Intelligence independently:** foreclosed — this
  adapter is invoked only by `OcrExecutionSequencer`, itself invoked
  only by the coordinator, itself invoked only from inside
  `DefaultEvidenceIntelligence.analyse`; nothing in this design self-triggers.
- **Decide evidential truth:** foreclosed — recognised text is, at
  most, a candidate disclosure (Contract Design §2, §8); this adapter
  constructs no assertion, proposition, or truth claim of any kind.
- **Perform review approval:** foreclosed — no dependency on
  `DerivativeReviewRegistry` anywhere in this design.
- **Access QMD/RKS:** foreclosed — no dependency of any kind; this
  design adds no retrieval or indexing capability.
- **Modify Evidence Custodian:** foreclosed — this adapter and its own
  coordinator only ever call `EvidenceCustodian.retrieveManifest`/`retrieve`
  (both read-only); no `accept`, `delete`, or any write-shaped call
  exists anywhere in this design.

---

## 21. Reprocessing (item 17)

**Stateless by construction, mirroring `QmdRelevanceMechanism`'s own
Frozen-Boundary-#10-style "disposable state" precedent exactly.** A
fresh subprocess per call, no memoisation, no cache of any kind held
by the adapter itself. No deduplication and no silent overwrite —
matches Unit 12 Implementation Plan §5.W's own already-fixed behaviour
without modification: two separate, explicit OCR-eligible
`analyseEvidence` calls against the same source each independently
retrieve, verify, and invoke a fresh Docling subprocess.

---

## 22. Deployment footprint (item 18)

**Named precisely; not performed.** A future, separate provisioning
unit will need to install/configure, on the Parker Ubuntu VM:

1. A Python interpreter (version to be confirmed against Docling's own
   current requirement at implementation time).
2. A dedicated Python virtual environment containing the `docling`
   package and its own transitive dependencies, at a fixed path
   (illustratively, `/opt/docling`, mirroring `/opt/qmd`).
3. Docling's own pretrained model files, pre-downloaded once (the one
   genuinely network-requiring step in this entire footprint,
   performed outside any running Parker container's own execution
   path), cached at a fixed path (illustratively, `/opt/docling-models`),
   mounted read-only into Parker's own runtime environment
   (`docker-compose.yml`'s own `:ro` mount convention, mirrored).
4. `tools/docling-ocr-bridge.py` (illustrative name), packaged into
   the Parker image/deployment alongside the existing
   `tools/qmd-relevance-bridge.mts`, following the same `tools/`
   directory convention.
5. New `docker-compose.yml` environment-variable entries, mirroring
   the exact `PARKER_QMD_*` naming convention: illustratively,
   `PARKER_DOCLING_PYTHON_EXECUTABLE_PATH`,
   `PARKER_DOCLING_BRIDGE_SCRIPT_PATH`, `PARKER_DOCLING_MODEL_CACHE_DIR`,
   `PARKER_DOCLING_VENV_PATH`.
6. New `ParkerRuntimeConfig` constructor parameters, mirroring
   `qmdNodeExecutablePath`/`qmdBridgeScriptPath`/`qmdModelCacheDir`
   exactly: illustratively, `doclingPythonExecutablePath`,
   `doclingBridgeScriptPath`, `doclingModelCacheDir`.
7. Recommended, not mandated by this document: a dedicated,
   minimal-privilege OS user for the bridge subprocess (§18, above);
   an OS/container-level outbound-network denial policy (§8, above).

None of the above is installed, configured, or added by this document.

---

## 23. Test strategy (item 19)

Mirrors the Unit 12 Implementation Plan's own §7 (29 items), extended
with the Docling-adapter-specific items this task names, and split
correctly between the ordinary `test` source set (fake adapter only,
always run) and the existing `liveModelEvaluation` source set (real
Docling, opt-in, gated on provisioning) — exactly the split
`QmdRelevanceMechanismLiveAcceptanceTest.kt` already establishes.

**Ordinary `test` source set — fake `DoclingSubprocessInvoker` only,
never spawns a real process, mirroring `QmdRelevanceMechanismTest.kt`'s
own established shape:**

1. Contract test: valid request → fake returns a well-formed
   `Recognised`-shaped JSON → adapter produces the expected
   `OcrRecognitionOutcome.Recognised`.
2. Scanned PDF fixture (`03-scanned.pdf`, reused, immutable,
   already-proven `RequiresTierB`-producing): prove the correct temp
   file/argument construction, never real Docling invocation.
3. PNG image fixture (`07-text-image.png`, reused): same shape.
4. Corrupt source: fake invoker simulates Docling's own decode
   failure → `UnsupportedOrInaccessibleInput`.
5. Oversized source: fake simulates the coordinator-tier 64 MiB
   rejection (composition-level, per §13, above) — never reaches the
   adapter at all; a second test simulates a bridge-script-tier
   page/pixel/output rejection reaching the adapter directly.
6. Timeout: fake invoker returns `timedOut = true` → `ProcessingOrDependencyFailure`,
   honest reason, never `Recognised`.
7. No-network proof: structural, reflection-based test proving
   `DoclingOcrProviderAdapter` and its own configuration/invoker types
   hold no networking-capable dependency of any kind (mirroring Unit
   9's own closed reachable-type-graph proof, extended to this new
   adapter).
8. Missing model/cache: fake invoker simulates the bridge script's own
   pre-flight-check failure exit → `ProcessingOrDependencyFailure`,
   distinct reason.
9. Malformed output: fake invoker returns JSON missing a required
   field, or carrying an unexpected extra one → the adapter's own
   hand-rolled parser throws, propagating unchanged (§17, above),
   mirroring `QmdRelevanceMechanismTest.kt`'s own equivalent tests
   exactly.
10. Concurrency-one: a structural test proving `DoclingOcrProviderAdapter`/`EvidenceIntelligenceOcrCoordinator`
    hold, and correctly acquire/release, exactly one mutex/semaphore
    permit around invocation — mirroring the honest limitation §15,
    above, already discloses (genuine concurrent-contention timing is
    not provable by a fast, deterministic unit test; the structural
    presence and correct acquire/release ordering is).
11. Provenance: fake invoker supplies a populated identity/version
    payload; prove it is carried, unaltered, into `OcrRecognitionIdentity`.
12. Cleanup: prove the temp source-bytes file is deleted after a
    successful invocation, after a simulated failure, and after a
    simulated timeout — three separate tests, mirroring §16's own
    three-path requirement.
13. No source mutation: prove the retrieved `content` `ByteArray`
    passed into the request remains byte-identical after the call
    completes (mirroring `TierAOwnerInvocationCoordinatorTest`'s own
    "source bytes reaching the router are byte-identical... no
    mutation" precedent).
14. No Memory/Knowledge/`DerivativeGenerationRecord` side effects: a
    structural test proving `DoclingOcrProviderAdapter` holds no
    dependency capable of any of the three (§20, above).
15. Process-argument-list proof: a spy `DoclingSubprocessInvoker`
    capturing the exact command list a real invoker would pass to
    `ProcessBuilder`, proving it is a `List<String>` never joined into
    a shell string, and that no evidence-supplied value (media type,
    filename) appears inside it unescaped/uninterpreted.

**`liveModelEvaluation` source set — real Docling, opt-in, gated on
provisioning, mirroring `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s
own established shape and skip discipline exactly (a new, dedicated
Gradle task, illustratively `doclingOcrProviderAdapterLiveAcceptance`,
within the same, already-existing `liveModelEvaluation` source set —
not a new source set):**

16. A real, provisioned Docling subprocess correctly recognises text
    from `03-scanned.pdf` and `07-text-image.png`.
17. A real Docling subprocess, launched with the offline-mode
    environment variables deliberately *unset* and a deliberately
    *missing* model-cache directory, fails closed with a diagnosable
    error — never triggers a real network download (mirroring
    `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own "a missing
    local embedding model fails closed, without triggering an
    on-demand network download" test exactly, substituted for
    Docling).
18. A real Docling subprocess genuinely exceeding the 15-minute
    timeout (or a deliberately-slow test double standing in for one,
    where a genuine 15-minute wait is impractical for routine test
    runs) is genuinely terminated, and no process remains running
    afterward (verified via `ProcessHandle`/OS-level inspection).
19. A real Docling subprocess run twice concurrently (deliberately
    racing two invocations) proves the mutex genuinely serialises
    them, never running two Docling processes' own heavy model-loading
    simultaneously.

**Full-suite regression requirement:** `./gradlew test --no-daemon
--rerun-tasks` must pass in full (ordinary source set only, with only
the same pre-existing, environment-conditional skips already documented
this session) before any future implementation of this plan may be
declared complete; the `liveModelEvaluation`-source-set tests are
explicitly not part of that regression requirement, exactly as QMD's
own equivalent tests already are not.

---

## 24. Implementation impact map (item 20)

| Surface | Classification |
| --- | --- |
| New `DoclingOcrProviderAdapter.kt` (`OcrProviderAdapter` implementation) | **REQUIRED** — future implementation |
| New `DoclingOcrProviderAdapterConfiguration` (immutable, mirroring `QmdRelevanceMechanismConfiguration`) | **REQUIRED** — future implementation |
| New `DoclingSubprocessInvoker` (`fun interface`) + `ProcessBuilderDoclingSubprocessInvoker` | **REQUIRED** — future implementation |
| New `tools/docling-ocr-bridge.py` (illustrative name) | **REQUIRED** — future implementation |
| New `ParkerRuntimeConfig` fields (§22, above) | **REQUIRED** — future implementation, coordinated with Unit 12's own composition wiring |
| New `docker-compose.yml`/`Dockerfile` entries (§22, above) | **REQUIRED** — future, separate provisioning-unit implementation |
| Docling `pip` package installation | **FORBIDDEN under this plan's own authority** — provisioning-unit work, not performed here |
| Docling model download/provisioning | **FORBIDDEN under this plan's own authority** — same |
| Ordinary `test`-source-set unit/structural tests (§23, items 1-15) | **REQUIRED** — future implementation |
| `liveModelEvaluation`-source-set tests (§23, items 16-19) | **CONDITIONAL** — required before this unit may be declared *live*-complete; not runnable, and not required to pass, before provisioning exists |
| New `OcrRecognitionOutcome`/`OcrRecognitionRequest`/`OcrRecognitionResult` field or variant | **FORBIDDEN** — none required (§19, above) |
| New `PermissionAction`/`ResourceType`/`Resource`/`ActionVocabulary` entry | **FORBIDDEN** — unaffected by this plan, confirmed by Unit 12 Scope Lock §5 |
| Any change to `OcrMechanism.kt`, `OcrProviderAdapter.kt`, `OcrExecutionSequencer.kt`, the Docling Authorization, either Unit 12 document, the Contract Design, or the Scope Lock | **FORBIDDEN** |
| Unit 12's own composition wiring (`EvidenceIntelligenceOcrCoordinator`, `DefaultEvidenceIntelligence`'s third parameter) | **NOT this plan's own scope** — already, separately designed (Unit 12 Implementation Plan); this plan supplies the adapter that composition wiring would hold, not the wiring itself |
| Document Ingestion Tier B routing | **FORBIDDEN under this plan's own authority** |
| Full ordinary-source-set regression run | **REQUIRED**, before future implementation may be declared complete |

---

## 25. Dependency review (item 21)

**No dependency is added by this document.** For the future
implementation this plan authorizes to be proposed, the following
dependency questions must each be independently answered by that
future unit, not answered here:

- **JVM side:** no new JVM/Gradle dependency is anticipated — the
  adapter's own JSON parsing follows `QmdRelevanceMechanism`'s own
  hand-rolled, deliberately-narrow, dependency-free convention (§2,
  above), not a general-purpose JSON library; `ProcessBuilder` and
  `kotlinx.coroutines.sync.Mutex` are both already present in this
  repository's own existing dependency graph.
- **Python side (a new dependency surface entirely, unlike QMD's own
  Node/npm-side dependencies, which this repository has never had to
  formally review before):** the `docling` package itself, and its own
  transitive dependencies (a deep-learning backend, image-handling
  libraries) — for each, a future dependency-review turn must supply
  exact package name and version-pinning strategy; license (Docling
  itself and each transitive dependency); security implications
  (supply-chain exposure of a Python package ecosystem this repository
  has not previously depended on); deployment implications (virtual
  environment size, model-file storage size, install-time network
  requirement for the one-time model download); and confirmation that
  it lives in the Python virtual environment, never the JVM or the
  host OS's own system Python.
- **Host OS side:** the Python interpreter itself, and whatever native
  libraries Docling's own Python dependencies require at the OS level
  (for example, image-codec libraries) — same review requirement.

**No dependency of any kind is added during this task.**

---

## 26. Adversarial review (item 22)

| # | Attack | Result |
| --- | --- | --- |
| 1 | Implicit network fetch | **Mitigated, not fully foreclosed by code alone, honestly disclosed** — §8: the application-level offline-mode pre-flight check is the primary control, but is not proven sufficient by itself (Phase 5's own instruction); the read-only model-cache mount blocks only the *write*/persistence half of a fetch, not the request itself; the only genuine request-level guarantee is the *recommended*, not mandated, OS/network-level denial (§8/§18) |
| 2 | Model auto-download | Same reasoning as #1 — a successful, persisted auto-download is structurally prevented by the read-only mount (§9); the network *request* itself is not fully foreclosed by this plan's own code-level controls alone |
| 3 | Subprocess escape | Mitigated, not eliminated — §18: `ProcessBuilder(List<String>)` forecloses shell-based escape; genuine sandboxing (a dedicated low-privilege OS user, network denial) is a recommended, disclosed, deployment-tier hardening step, not code-enforced by this plan alone |
| 4 | Command injection | Foreclosed — §18, `ProcessBuilder(List<String>)`, no shell |
| 5 | Arbitrary path use | Foreclosed — §18, every path is Parker-generated/configured, never evidence-derived |
| 6 | Source mutation | Foreclosed — §10, a copy is written to a private temp file; the original custodied artefact is never touched |
| 7 | Output fabrication | Foreclosed — §11/§17: only fields Docling can truthfully provide are mapped; every non-success outcome maps to a distinct, honest failure variant, never a fabricated `Recognised` |
| 8 | Confidence fabrication | Foreclosed — §11: `confidence` is `null` unless Docling genuinely reports a document-level scalar; never averaged or invented |
| 9 | Unbounded stdout/stderr | Foreclosed — §18, explicit stdout/stderr byte caps, a genuinely new consideration this plan adds beyond the QMD precedent |
| 10 | Timeout escape | Foreclosed — §14, `waitFor(timeout)` + `destroyForcibly()`, JVM-native, independent of Python's own cooperative behaviour |
| 11 | Process leak (zombie process) | Partially foreclosed, honestly disclosed — §14: the direct child is reliably terminated; a full process-*tree* kill is recommended but not mandated as one frozen mechanism, pending confirmation of Docling's own actual process topology |
| 12 | Temp-file leak | Foreclosed — §10/§16, unconditional `finally`-block cleanup on every path |
| 13 | Model/cache corruption | Foreclosed — §9/§18, read-only mount; genuine tamper-evidence (checksums) is a recommended provisioning-unit concern |
| 14 | Concurrency bypass | Foreclosed — §15, single-permit mutex held for the entire invocation, not merely the process-start call |
| 15 | Side-effect authority creep | Foreclosed — §20, item-by-item structural confirmation |
| 16 | Memory/Knowledge writes | Foreclosed — §20 |
| 17 | Evidence Intelligence bypass | Foreclosed — §20; this adapter is reachable only through the already-designed, unchanged invocation chain |
| 18 | `DerivativeGenerationRecord` creation | Foreclosed — §20 |
| 19 | Provider-specific authority leakage | Foreclosed — §5, confinement discipline, `mechanismIdentity` reconciliation explicitly addressed, not silently permitted to widen further |
| 20 | Dependency creep | Foreclosed — §25: no dependency is added by this document; every future dependency question is named, not answered, here |
| 21 | Decompression bombs (a small file expanding into an enormous in-memory bitmap during rasterisation) | **Corrected during this review pass** — §13's pixel-bound row now specifies a declared-size, *pre*-decompression check as the genuine defence, not merely a post-decode check that would already be too late; found and fixed as a real defect, not present in the original drafting |

No item resolves to a blocker for adopting this document. Items 1, 2,
3, and 11 remain honestly, partially disclosed limitations — mitigated
by design, not eliminated by this planning-only document, which cannot
itself enforce a deployment-tier, network-policy, or
process-topology-specific control
without writing code.

---

## 27. Governance classification (item 23)

**The concrete adapter implementation can proceed under already-adopted
authority once this plan is accepted — for the code itself. Runtime
provisioning remains separately, additionally gated before that code
can be functionally exercised in production or live-tested.**

Precisely:

- Provider authorization: adopted (Docling Authorization).
- Invocation-authority/composition design: adopted (Unit 12 Scope
  Lock/Implementation Plan).
- Concrete adapter *design*: supplied by this document.
- Once this document is itself accepted, a future implementation unit
  may write `DoclingOcrProviderAdapter` and its own ordinary-source-set
  tests (§23, items 1-15) **without** requiring Docling to be installed
  or provisioned — the fake-invoker test discipline this plan
  specifies makes the code itself fully buildable and testable in its
  absence, exactly as `QmdRelevanceMechanism`'s own equivalent tests
  already do not require a real QMD checkout to pass.
- What that same future unit **cannot** do without a further, separate
  provisioning step: run the `liveModelEvaluation`-source-set tests
  (§23, items 16-19), or exercise the adapter against genuine Docling
  output in production — both require the deployment footprint §22,
  above, names but does not perform.
- **No further narrow governance blocker remains for the adapter's own
  code-level implementation.** Provisioning itself is not a governance
  question this document, or any prior OCR Mechanism document, treats
  as requiring its own Scope Lock — it is ordinary, disclosed,
  operational/deployment work, named precisely (§22, above) so a
  future implementer or operator knows exactly what to do, but not
  gated behind a further governance-adoption cycle the way provider
  *selection* was.

---

## 28. Citation and cross-reference audit

Fresh-checked, this document, after drafting: every `§N` self-reference
verified against this file's own 30 top-level numbered sections (this
one being §28); every external citation to the Docling Authorization,
the Unit 12 Scope Lock, the Unit 12 Implementation Plan, and
`QmdRelevanceMechanism.kt`/`tools/qmd-relevance-bridge.mts`/`docker-compose.yml`/`build.gradle.kts`
verified against a fresh read of each performed in this same drafting
pass (§2, above) — not carried forward from any prior turn's own report
without re-verification. Every Docling-packaging claim is explicitly
flagged as external knowledge, not a repository fact, per §2's own
disclosure. Every illustrative name (`DoclingOcrProviderAdapter`,
`DoclingOcrProviderAdapterConfiguration`, `DoclingSubprocessInvoker`,
`ProcessBuilderDoclingSubprocessInvoker`, `tools/docling-ocr-bridge.py`,
every `PARKER_DOCLING_*` environment variable, every
`ParkerRuntimeConfig` field name) is explicitly marked illustrative at
first use, mirroring the exact discipline the two prior OCR Mechanism
implementation-planning documents this session already established.

---

## 29. Conflicts or ambiguities

**None found.** Every architectural decision in §6-§21, above, either
reuses an already-accepted mechanism unchanged (the `OcrProviderAdapter`
boundary, the nine-way failure taxonomy, the resource bounds) or
mirrors an existing, already-accepted precedent by direct, disclosed
analogy (`QmdRelevanceMechanism`'s own subprocess/configuration/invoker
shape, `docker-compose.yml`'s own QMD deployment convention, the
`liveModelEvaluation` source set). No decision in this document narrows
or widens anything the Docling Authorization or either Unit 12 document
already fixed.

---

## 30. Files created/modified

Exactly one — `docs/architecture/OCR_MECHANISM_DOCLING_CONCRETE_ADAPTER_IMPLEMENTATION_PLAN.md`
(new). No other file is created, modified, staged, committed, or
pushed. No dependency is added. Docling is not installed. No model is
downloaded. No runtime/cache is provisioned.

---

## Final Recommendation

**READY FOR OWNER REVIEW.**

This plan designs the smallest, most precedent-consistent concrete
`DoclingOcrProviderAdapter` architecture available: a one-shot local
Python subprocess, mirroring `QmdRelevanceMechanism`'s own
already-accepted shape field-for-field, with every numeric bound the
Docling Authorization froze mapped to an exact enforcing layer, an
honest, non-widened mapping onto the already-frozen
`OcrProviderAdapter`/`OcrMechanism` contracts, explicit no-network and
no-authority-widening confirmation, and a test strategy correctly split
between always-run fake-adapter tests and opt-in, provisioning-gated
live tests. It names, precisely, a deployment footprint it does not
build. It confirms the adapter's own code-level implementation may
proceed once this document is accepted, without waiting on Docling's
own runtime provisioning — while honestly disclosing that runtime
provisioning, live testing, and production execution each still
require that separate, later, operational step. It does not implement,
install, download, provision, or authorize Unit 12 composition or
Document Ingestion Tier B — each remains exactly as separately governed
as before.
