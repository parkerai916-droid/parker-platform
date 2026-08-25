# Tier B OCR — Truthful Mandatory Provenance: Implementation Plan

## Status

**Planning document. Not yet accepted, canonical, or implementation-authorising.**
No Kotlin, Python, Docker, `.env`, or governance file is modified by this
document. No dependency is added. No model cache is touched. This plan
resolves the prerequisite named at the close of the accepted
`docs/architecture/DOCUMENT_INGESTION_TIER_B_DURABLE_OCR_DERIVATIVE_CONTENT_SCOPE_LOCK.md`
("the Tier B scope lock") §11: the two mandatory-for-Tier-B provenance
fields, `DerivativeProducerIdentity.modelIdentity`/`modelVersion`, cannot
today be truthfully populated, and durable Tier B admission must fail
closed until they can be. This plan defines, precisely, how a future
implementation unit closes that gap — it does not itself close it.

**This is a correction pass on this plan's own first draft.** Fresh,
empirical re-testing against the real, installed `rapidocr==3.9.2`
package (not merely reading its source) disproved the first draft's
specific claim that `RapidOCR(params={"Rec": {"session": <InferenceSession>}})`
works — it does not; OmegaConf's own config-merge validation rejects an
`InferenceSession` object before it can ever reach `OrtInferSession`. A
corrected, empirically-verified binding mechanism, proven against real
inference output (not merely successful construction), is defined below
(§5-§6). Every other section is re-checked against this correction;
sections whose substance did not need to change are retained.

**Second correction pass (Codex post-implementation review).** After this
plan's own design was implemented, a Codex review of the real code found
three claims below that overstated what the implementation actually does
or was proven to do. Corrected in place, at each cited location, rather
than as a separate addendum, since each is a small, precise wording
correction, not a design change: (1) §6/§18's "no second... filesystem
read... at all" language for Option D described the relationship between
hashing and session-construction correctly, but read in isolation
overstated the total number of file reads per invocation — Docling's own
ordinary pipeline construction always performs one separate, preliminary,
unused read first (§6, §8, below, corrected); (2) §6/§7/§18 described the
fallback as "Option A, retained... as the fallback" — the actual
implemented fallback performs no `model_path` injection of any kind; it
simply leaves RapidOCR's own already-constructed default recognizer
untouched (§6/§7/§18, below, corrected; §13's own description was already
accurate and needed no change); (3) §17's "no network dependency in
either... branch" is corrected to distinguish the verified branch (proven
under real `--network none`) from the fallback branch (pre-existing,
unmodified RapidOCR construction behaviour, which may itself attempt a
network self-heal in some failure states unrelated to this plan's own
mechanism). None of these corrections change the design, the file/surface
plan, or the recommendation — the implementation was not changed as a
result of them, only this document's own description of it.

## 1. Purpose

Determine the smallest truthful mechanism by which Parker's Docling/RapidOCR
OCR path can report a `modelIdentity`/`modelVersion` pair that proves a fact
about the model artifact **actually used** for a specific OCR invocation —
not merely the artifact that was configured, expected, or intended — and
define the implementation, test, and acceptance surface a future unit must
build to make that fact reach `DerivativeGenerationRecord` truthfully.

## 2. Governing authority (fresh-read this pass)

- Tier B scope lock §10-§11: the truth table and the fail-closed rule this
  plan directly answers. §11 names two lawful remedy paths verbatim: (a)
  "a future, separate OCR governance unit extends `OcrRecognitionIdentity`
  with a genuine, distinct model-version field," or (b) a freestanding
  ruling that a mechanism's own version stands in for model version. This
  plan follows path (a) — real, measured fields, never (b)'s aliasing.
- `DOCUMENT_INGESTION_DERIVATIVE_GENERATION_RECORD_SCOPE_LOCK.md` §10: the
  producer-identity requirement this plan satisfies.
- `SOURCE_DERIVATIVE_PROVENANCE_MODEL.md`, Unit 12, the Tier A Content
  Scope Lock, the Programme closures, CDR-006/007/008: re-read; nothing
  conflicts with, or must be reopened by, this plan.

## 3. Implementation inspected fresh, this pass

- `src/interfaces/DerivativeGeneration.kt`: the live `require()` constraint
  this whole plan exists to satisfy, re-confirmed (lines 40-44, 133-139,
  157-169).
- `src/interfaces/OcrMechanism.kt`: `OcrRecognitionIdentity(mechanismIdentity,
  configurationProfile, mechanismVersion? = null)` — exactly three fields.
- `src/runtime/DoclingOcrProviderAdapter.kt`: the dead `;model=` suffix
  hack, re-confirmed.
- `tools/docling-ocr-bridge.py`: `model_identity` never assigned in
  `_real_docling_backend`, re-confirmed.
- **The real, installed `~/docling-venv`'s `rapidocr==3.9.2` package
  source, and its actual runtime behaviour, empirically tested this
  pass** — the primary correction this document makes (§4-§6, below).

## 4. Correction: the first draft's specific claim is disproven

Ran, verbatim, against the real installed package:

```
RapidOCR(params={"Rec.session": <InferenceSession>})
```

Result:

```
UnsupportedValueType: Value 'InferenceSession' is not a supported primitive type
    full_key: Rec.session
```

**Root cause, traced precisely.** `RapidOCR.__init__` never accepts a
nested `{"Rec": {"session": ...}}` dict shape at all — `ParseParams.update_batch`
(`utils/parse_parameters.py`, read in full this pass) expects **flat,
dot-path string keys** (`"Rec.session"`, confirmed by its own
`key_parts = k.split(".")` logic), and every value assigned through it is
merged into an already-typed `omegaconf.DictConfig` via `OmegaConf`'s own
merge machinery — which enforces that config values be "primitive types"
(str/int/float/bool/None/nested containers of these). An
`onnxruntime.InferenceSession` object is rejected at that layer,
**before** it can ever reach `OrtInferSession.__init__`'s own, perfectly
real `cfg.get("session", None)` check (`inference_engine/onnxruntime/main.py`
lines 22-29) — that check itself was never wrong; the *route* to it via
`RapidOCR`'s own high-level convenience constructor was.

## 5. Corrected binding mechanism — empirically verified against real
   inference output, not merely successful construction

**Verified, this pass, in three escalating steps, each with real command
output:**

1. Construct `RapidOCR()` normally (its own default construction, which
   loads its own default Det/Cls/Rec models transiently — see §14 below
   for the one disclosed cost of this).
2. Take the already-resolved `engine.text_rec.cfg` — a real, live
   `omegaconf.DictConfig` — and call its own `_set_flag("allow_objects", True)`.
   This is OmegaConf's own documented mechanism for permitting a
   `DictConfig` to hold an arbitrary Python object as a value (the
   leading underscore names a flag-setter, not an undocumented internal
   hack — OmegaConf itself has no *public*, non-underscore method for
   setting flags on an existing config, and this exact flag is the one
   OmegaConf's own documentation names for holding non-primitive Python
   objects).
3. Assign `rec_cfg.session = <my pre-built InferenceSession>` directly
   (now permitted, since the flag is set) and construct
   `TextRecognizer(rec_cfg)` **directly** — bypassing `RapidOCR.__init__`'s
   own `ParseParams.update_batch` merge-validation entirely, since the
   object is being assigned to an *already-constructed* `DictConfig`
   attribute, never merged in as new input.
4. Replace `engine.text_rec` with this newly-built `TextRecognizer`.

**Empirically confirmed, this pass:**

- `engine.text_rec.session.session is <my session>` — `True` (object
  identity, not equality) — the exact bytes-derived session is what the
  engine actually holds.
- **Real, end-to-end OCR inference through the substituted recognizer,
  against the repository's own `07-text-image.png` bake-off fixture,
  produced text byte-for-byte identical to the unmodified baseline
  engine** (including the Māori text "Te Whanganui-a-Tara" and currency
  formatting) — proving this is not merely a construction-time
  no-exception result but a functionally correct substitution.
- A genuinely different valid ONNX file (the detection model, copied to
  a scratch path, never modifying anything in the real venv) produces a
  **different** SHA-256 digest and, when injected the same way, **fails**
  during recognition (a `ConfigKeyError` surfaced from the
  character-dictionary resolution path, since the wrong model's metadata
  does not match what a Rec model would supply) — proving the substitution
  is genuinely load-bearing, not silently ignored in favour of some other,
  coincidentally-correct default.
- A byte-corrupted copy of the real recognition model is rejected outright
  by `onnxruntime.InferenceSession`'s own construction
  (`InvalidArgument`) — proving a tampered file cannot silently produce a
  usable session at all.

## 6. Re-evaluated design classification

- **Option D (session injection) — classification: A, STILL VIABLE, WITH
  A DIFFERENT (LOWER-LEVEL) INJECTION POINT than first drafted.** The
  exact concrete, demonstrated path is §5 above — `TextRecognizer`
  constructed directly from a `DictConfig` with `allow_objects` set,
  substituted onto `RapidOCR.text_rec` post-construction — never the
  high-level `RapidOCR(params=...)` constructor for this specific field.
  This remains **preferred**: it is architecturally stronger than any
  path-based design, because **for the verified provenance-bearing bytes
  specifically**, the bridge reads them exactly once, into memory, and
  constructs the `InferenceSession` from that **same in-memory bytes
  object** — there is no second, independent filesystem read *between
  hashing and session construction* for a race to occur across, at all,
  regardless of deployment configuration. **Corrected (Codex
  post-implementation review): this is not a claim of only one total
  model-file read per invocation.** Docling's own ordinary pipeline
  construction — which happens unconditionally, before this mechanism
  ever runs, and cannot be skipped without reimplementing Docling/RapidOCR's
  own initialization — already performs one separate, preliminary read of
  its own, building a default `TextRecognizer` that is never used for OCR
  once the verified substitution below succeeds (see §8's own corrected
  discussion). The TOCTOU property this bullet describes concerns only
  the verified bytes' own single read-hash-inject chain, which is where
  the actual security value lies: the digest reported as `modelVersion`
  is provably the digest of the exact bytes recognition then runs on,
  regardless of the preliminary load's own existence.
- **Fallback path — classification: the ordinary, already-shipped
  RapidOCR default construction, unmodified; no injection of any kind.**
  **Corrected (Codex post-implementation review): the implemented
  fallback is not Option A (`model_path` injection).** The bridge never
  performs any substitution at all in the fallback case — it simply
  leaves the preliminary default recognizer described above (the same
  object Docling's own ordinary construction already built) exactly as
  constructed, and OCR proceeds through it. `model_path` injection
  (Option A) was investigated and confirmed technically viable (§7,
  below) but is not what the shipped implementation does; §7 is retained
  for its own empirical record, not as a description of current fallback
  behaviour.
- **Options B/C (read back the effective path/bytes post-init) —
  classification: C, NOT VIABLE**, re-confirmed: `OrtInferSession`'s own
  resolved `model_path` is a local variable, never exposed on `self` or
  on the constructed `InferenceSession`; `onnxruntime.InferenceSession`'s
  own public API (`get_modelmeta`, `get_inputs`, `get_outputs`,
  `get_providers`) exposes no path/bytes accessor.

## 7. Option A — empirical re-confirmation, retained as background (not the shipped fallback)

Ran, this pass: `RapidOCR(params={"Rec.model_path": <str path>})`.

- **Survives OmegaConf**, unlike `session` — a plain string is a
  supported primitive type.
- **Used directly** by `OrtInferSession.__init__`'s own `model_path =
  cfg.get("model_path", None)` branch (checked before its own
  default-resolution/download logic), confirmed by source re-read.
- **A missing/invalid supplied path fails, never silently falls back to
  a default** — `OrtInferSession._verify_model` (`inference_engine/base.py`
  lines 97-106) raises `FileNotFoundError`/`FileExistsError` before any
  `InferenceSession` is ever constructed; there is no code path in
  `OrtInferSession.__init__` that catches this and retries with its own
  default resolution instead.

This remains real and correct as its own empirical finding, retained for
the record. **Corrected (Codex post-implementation review): this is not
what the shipped fallback does.** §13's own resolution — and the actual
implementation — never injects `model_path` (or anything else) in the
fallback case; the fallback is simply the preliminary default recognizer
Docling's own ordinary construction already built, left untouched. This
section's own empirical proof (`model_path` survives OmegaConf, is used
directly, fails closed on a bad path) remains true and was useful in
reaching that design decision, but does not describe current fallback
behaviour. `modelIdentity`/`modelVersion` are never populated outside the
primary, verified session-injection path in §5, exactly as this section
already said.

## 8. Pipeline integration — answered

- Can `RapidOCR` accept an already-constructed `TextRecognizer`? Not
  through its own high-level constructor — but replacing
  `engine.text_rec` **after** normal construction is a plain Python
  attribute assignment, confirmed to be exactly what `RapidOCR.__call__`
  reads at its own single call site (`self.text_rec(rec_input)`,
  `main.py` line 328) — no caching, no re-derivation, no other internal
  reference to the original object survives.
- Does this duplicate detection/classification/orchestration logic? **No.**
  `RapidOCR.__init__` still performs its own, completely unmodified
  Det/Cls construction and its own full `__call__` pipeline orchestration
  (detection → classification → recognition → post-processing). Only the
  **recognition** engine object is substituted; nothing about how
  detection, classification, or box/text assembly work is reimplemented,
  touched, or forked.
- Maintenance/semantic divergence risk: **low and bounded.** The only
  RapidOCR-internal knowledge this design depends on is (a) the shape of
  `cfg` that `TextRecognizer.__init__` expects (`engine_type`,
  `rec_batch_num`, `rec_img_shape`, `session`) and (b) that
  `RapidOCR.__call__` reads `self.text_rec` dynamically. Both were
  verified directly against the pinned `3.9.2` source; a future pin
  upgrade should re-run the same empirical proof (§16, below) as an
  acceptance gate before trusting the mechanism against a new version.
- Preserves current Docling → RapidOCR behaviour: **confirmed** — the
  byte-for-byte-identical recognised text result (§5, above) is the
  direct proof.
- **Preliminary default load vs. the model actually used (added, Codex
  post-implementation review).** Docling's own `RapidOcrModel.__init__`
  unconditionally constructs a complete default RapidOCR engine —
  including a default `TextRecognizer`, built from a separate,
  independent read of the bundled recognition-model file RapidOCR
  performs internally — as part of ordinary pipeline construction,
  before this plan's own mechanism ever runs. No supported Docling/RapidOCR
  hook to suppress this preliminary construction was found; reimplementing
  Docling's own pipeline initialisation to avoid it was considered and
  rejected as complexity exceeding the value of eliminating an *unused*
  load. **The governing invariant this plan actually requires, and
  delivers, is narrower and fully satisfied regardless:** the recognition
  model actually used for the provenance-bearing OCR execution is loaded,
  for that execution, from the exact bytes Parker itself read, hashed,
  and manifest-verified — never that only one total file read occurs.
  Concretely: `reader.text_rec` (the *only* attribute `RapidOCR.__call__`
  reads for recognition, confirmed above) is reassigned to the verified
  recognizer the instant substitution succeeds; the preliminary default
  recognizer becomes unreachable from that point on and is never used for
  OCR. Verified directly (not merely inferred) by a dedicated test proving
  `reader.text_rec` after successful substitution is neither the same
  object as, nor bound to the same session as, the preliminary one.

## 9. Model identity semantics

Unchanged from the first draft, now backed by a working mechanism:
`modelIdentity` names the specific recognition artifact (illustrative,
not frozen: `"rapidocr-onnxruntime:PP-OCRv6_rec_small"`), distinct from
`mechanismIdentity` (the framework, `"docling"`) and `configurationIdentity`
(the profile string).

## 10. Model version semantics

Unchanged: `"sha256:" + <64 lowercase hex digest>` of the exact bytes
used to construct the injected `InferenceSession` — a content-addressed
revision identifier, never a semantic version. Under the corrected
mechanism, this digest is now provably the digest of the exact bytes
recognition actually ran against (§5, above), not merely the digest of
whatever file a separate resolution step was expected to load.

## 11. Manifest verification — simplified under the corrected design

- **Hashing (always performed when the primary path is attempted)**
  proves which bytes were used to build the session that recognition
  then genuinely runs on.
- **Matching `default_models.yaml`'s own SHA-256** for the resolved
  model key remains **required**, not advisory, before the normal,
  bundled `modelIdentity` name may be reported.
- **Corrected simplification**: under the corrected mechanism, a
  manifest mismatch is folded into the **single existing "verification
  failed" branch** (§13, below) that already governs every other
  preparation failure (file missing, unreadable, digest-computation
  error) — it does **not** need its own, third, separately-reported
  state. In every one of these cases the bridge falls back to the
  existing, unmodified, transient-OCR-preserving default path (§13), and
  `modelIdentity`/`modelVersion` are simply never populated. This is
  simpler than the first draft's separate "absent-due-to-mismatch" case
  and equally truthful — mismatch and absence are both, honestly, "no
  verified provenance," never "the normal identity, unverified."

## 12. Mandatory vs. supplementary model artifacts

Unchanged from the first draft: the recognition ("Rec") model is the one
mandatory artifact for this plan's own scope; detection, classification,
Docling-layout, and table-structure models remain supplementary, not
required, not expanded into this plan.

## 13. Failure semantics — resolved (the plan's previously-open question)

**Resolved explicitly, per instruction, in the least-disruptive-to-transient-OCR
direction, while keeping durable admission strictly fail-closed:**

The bridge **always attempts the verified path first** (§5 sequence:
resolve → read bytes → hash → manifest-match → construct session →
inject → recognise). **If, and only if, every step of that sequence
succeeds**, recognition proceeds through the verified, injected session,
and `modelIdentity`/`modelVersion` are populated truthfully.

**If any step fails** (file missing/unreadable, hash computation fails,
manifest entry missing, manifest digest mismatch, `InferenceSession`
construction fails on the read bytes) — the bridge **falls back to
RapidOCR's own normal, completely unmodified default construction**
(today's exact, already-shipped behaviour, unaffected by anything this
plan changes) for that one invocation. Transient OCR **still succeeds**
in this branch, exactly as it does today. `modelIdentity`/`modelVersion`
are **not populated** in this branch — the bridge did not verify which
bytes actually ran, so it reports honestly that it does not know, never
a placeholder, never the normal name unverified.

This is **explicitly distinguishable** internally (the bridge's own
control flow knows, unambiguously, which branch it took) and satisfies
every constraint the governing instruction named: transient OCR is
disrupted only when RapidOCR itself could not produce a result under
either branch (which was already true before this plan, unrelated to
provenance); the durable path never accepts unverified provenance,
because the only way `modelIdentity`/`modelVersion` become non-null is
the fully-verified branch; the fallback never fabricates identity.

**This resolves, cleanly, the specific open question the first draft
left unresolved** (whether a hash-computation failure should cost the
owner a transient OCR result) — it now explicitly does not, in either
design option, because the fallback path exists specifically to absorb
that failure without disrupting transient behaviour.

## 14. Transient OCR compatibility

- `recognisedText`, `fidelity`, `warnings`, `segments`, `confidence`:
  unchanged in both the verified and fallback branches.
- The one disclosed cost of §5's mechanism: the initial `RapidOCR()`
  construction transiently loads its own default Rec session before
  it is replaced (in the verified branch) — a small, bounded, one-time
  extra model load per bridge invocation, not a correctness problem
  (the discarded session is never used for inference). A future
  implementer may choose to accept this (recommended — the cost is a
  few-MB model load, dominated by the rest of the invocation's own
  wall-clock cost) or investigate avoiding it by pre-constructing the
  `Det`/`Cls` config directly and only then building `Rec` once,
  correctly, from the start — not required by this plan.
- No new persistence, no new Memory/Knowledge/QMD/RKS effect, in either
  branch.

## 15. Actual-used proof — corrected design

Directly reuses the empirical method already demonstrated in §5, above,
formalised as a future test: supply a controlled, scratch-path copy of a
genuinely different valid ONNX file (not the model cache, not the venv
itself), inject it via the exact §5 mechanism, and require: (a) the
reported `modelVersion` digest equals an independent `sha256sum` of that
supplied file, never the bundled default's digest; (b) recognition
through that substituted session either produces measurably different
output or fails outright (both outcomes prove genuine substitution,
never silent fallback to the real Rec model); (c) a corrupted-bytes
variant is rejected at `InferenceSession` construction, never silently
producing a usable-but-wrong session.

## 16. Live acceptance test

Unchanged in substance from the first draft: extends the existing real
bridge / real RapidOCR / real `onnxruntime` / `--network none` live test
already established in this repository, asserting `modelIdentity`/
`modelVersion` populated and matching an independently-computed digest of
the real on-disk recognition model inside the same container,
`mechanismIdentity`/`mechanismVersion` independently correct, no path in
response/logs, no network access. **Additionally now requires**: a
second acceptance run against a deliberately-broken/inaccessible model
file (a controlled test fixture, never the production install) proving
the fallback branch (§13, above) still produces a valid transient OCR
result with `modelIdentity`/`modelVersion` absent — proving both branches
of the resolved failure semantics live, not merely the happy path.

## 17. Restart / rebuild / offline semantics

Unchanged from the first draft: `modelVersion` tracks the `rapidocr` pin
specifically; `mechanismVersion` tracks the `docling` pin specifically;
both independent; stable across restart and rebuild from the same pins.
**Corrected (Codex post-implementation review): "no network dependency in
either... branch" overstated what is proven.** The **verified** branch
operates entirely from already-local, already-bundled artifacts and has
been proven to succeed under a real `docker run --network none`
acceptance run. The **fallback** branch is RapidOCR's own ordinary,
pre-existing, unmodified recognizer-construction path — this plan does
not redesign it, and does not claim it is network-independent: RapidOCR's
own `download_file.py` may itself attempt a network self-heal/redownload
in some failure states unrelated to this plan's own mechanism (discovered
during implementation-time testing). The fallback branch is never
provenance-bearing regardless of whether it succeeds or fails, so this
does not affect provenance truthfulness; it is a correction to this
document's own network-independence claim, not a design defect.

## 18. TOCTOU and filesystem-immutability — corrected, honest finding

**Corrects an assumption the task itself invited scrutiny of.** I checked
the actual deployment facts rather than asserting them:

- On the bare host (`~/docling-venv`), the recognition model file is
  `-rw-rw-r--`, owned `steve:steve` — **writable by its own owner and
  group**, not immutable.
- In the built container: `Dockerfile` line 137,
  `COPY --chown=parker:parker --from=docling-build /opt/docling-venv
  /opt/docling-venv`, bakes the venv into an image layer owned by
  `parker:parker`; the container runs as `USER parker` (line 174).
  **`docker-compose.yml` sets no `read_only: true` flag anywhere.**
  Docker's default runtime filesystem is writable, and since the
  `parker` process owns its own venv files, **the running Parker process
  itself has write permission to the very model files it also reads.**

**Honest conclusion: filesystem immutability/read-only deployment is
NOT currently a valid mitigation for Option A's TOCTOU window** — the
premise the task asked me to verify does not hold in the actual current
deployment. This is disclosed here rather than asserted away.

**This is precisely why Option D (§5-§6, above) is preferred, not merely
convenient**: it does not rely on this unconfirmed (and, as checked,
false) deployment assumption at all. Because the verified bytes hashed
and the bytes loaded into the injected session are the literal same
in-memory object, there is no second filesystem access, for the verified
path specifically, for *any* writer — trusted or not — to race against
between hashing and injection. TOCTOU is architecturally eliminated for
the verified provenance-bearing path, independent of container hardening.
**Corrected (Codex post-implementation review): the fallback path is not
Option A.** The shipped fallback performs no `model_path` injection at
all — it is simply Docling's own preliminary default recognizer, already
constructed before this mechanism runs, left untouched. Whatever TOCTOU
exposure exists in *that* recognizer's own construction is entirely
RapidOCR's own ordinary, pre-existing behaviour, unmodified and
unaffected by this plan; it carries no theoretical exposure this plan
introduces, and — as §13 already states — never populates provenance
regardless.

**Separately disclosed, out of this plan's own scope**: Parker's
deployment does not currently run with `read_only: true` container
hardening. This is a genuine, real finding worth a future, separate
deployment-hardening review — this plan does not attempt it, and does
not require it to close the provenance gap, since §5's mechanism does
not depend on it.

## 19. Stronger path-binding alternatives — considered, rejected as
    unnecessary

`/proc/self/fd` re-opening, `memfd_create`, or an immutable exclusive
temp-file copy were all considered as hardening for a *path-based* load.
**Rejected, not because they would not work, but because they are
unnecessary**: §5's mechanism already avoids a path-based load entirely
for the primary, provenance-populating path — there is no path to
harden. Introducing any of these purely to strengthen a path-based
Option A design considered (and not adopted, even as the fallback — §6,
above, corrected) would add real complexity (OS-specific code, additional
failure modes of its own) for a branch that, by design (§13), never
populates provenance and therefore never needs load-bearing integrity
guarantees in the first place. Rejected per the task's own instruction
not to add complexity exceeding security value.

## 20. `OcrRecognitionIdentity` contract change

Unchanged in shape from the first draft:

```
data class OcrRecognitionIdentity(
    val mechanismIdentity: String,
    val configurationProfile: String,
    val mechanismVersion: String? = null,
    val modelIdentity: String? = null,
    val modelVersion: String? = null,
)
```

(Illustrative — not authorised code.) Paired-presence `init` constraint;
non-blank when present; `modelVersion` matches `^sha256:[0-9a-f]{64}$`
exactly when present (lowercase only, full length, no truncation); no
`"unknown"`; backward-compatible defaults; `confidence` untouched.

## 21. Bridge response change

Unchanged in shape from the first draft, with the corrected failure
routing from §13 above governing exactly when the two new JSON fields
are emitted (both, or neither — never one alone); no filesystem path,
manifest path, model bytes, or secret ever crosses the boundary.

## 22. Adapter change

Unchanged from the first draft: direct mapping, removal of the dead
`;model=` suffix hack, defensive rejection of malformed/inconsistent
pairing at the Kotlin `init` boundary, `mechanismIdentity`/`mechanismVersion`/
`configurationProfile` independence preserved.

## 23. File / surface plan (illustrative; none modified by this document)

**MUST CHANGE:** `tools/docling-ocr-bridge.py` (implement §5's verified
path plus §13's fallback, both using only the real, empirically-confirmed
API surface — never the disproven `RapidOCR(params={"Rec": {"session":
...}})` call); `src/interfaces/OcrMechanism.kt`; `src/runtime/DoclingOcrProviderAdapter.kt`.

**MAY CHANGE:** `tests/runtime/DoclingOcrProviderAdapterTest.kt`; bridge-level
Python tests; existing live-acceptance test files extended per §16.

**MUST NOT CHANGE:** Evidence Custodian authority; `DerivativeGenerationRecord`'s
frozen field-shape taxonomy; Memory Core, Knowledge, QMD, RKS;
reasoning-provider boundaries; Tier A behaviour; the existing transient
`analyseEvidence`/`TransientOutput` path's own semantics; `confidence`
handling; `.env`; Docker/compose (including the disclosed-but-out-of-scope
`read_only` hardening question, §18 above); any storage directory; the
not-yet-built durable-generation admission operation itself.

## 24. Test plan

1. `modelIdentity` populated on real, successful, verified recognition.
2. `modelVersion` populated, matching the expected manifest digest.
3. Paired presence enforced.
4. **Actual-loaded-bytes digest proof** (§15, above) — decisive, not a
   static assertion; includes the "different file → different, correctly
   different digest, or clean failure" proof already demonstrated live
   this pass.
5. Manifest-match proof.
6. Manifest-mismatch → falls back, transient OCR still succeeds,
   provenance absent (§13).
7. Missing file → same fallback behaviour.
8. Unreadable file → same fallback behaviour.
9. Malformed bridge response rejected.
10. One-field-only response rejected.
11. Transient OCR text byte-for-byte unchanged (already demonstrated
    live, this pass, for the verified branch; must also be proven for
    the fallback branch).
12. Durable admission blocked when provenance unavailable — exercised
    directly against the existing, already-shipped
    `DerivativeGenerationRecord` constructor.
13. No `"unknown"` anywhere in the new code paths.
14. Mechanism/model identities remain distinct.
15. `--network none` live acceptance, both branches (§16, above).
16. Restart stability.
17. Same package/model reproducibility across a fresh process start.
18. Changed model artifact yields changed provenance (controlled fixture
    only, §15).
19. No path leakage.
20. No token/content leakage.
21. **New, added this pass**: the fallback branch (§13) is reachable and
    correctly distinguishable from the verified branch in a dedicated
    test — not merely inferred from the other tests passing.

## 25. Governance impact

**No governance change required** — unchanged conclusion from the first
draft; the corrected mechanism is a different concrete implementation of
the same lawful remedy class the Tier B scope lock's own §11 already
names.

## 26. Plan defects / unresolved questions (disclosed honestly)

1. Whether `do_table_structure` is active in the current pipeline —
   unresolved, unconfirmed, flagged for the future implementer, as in
   the first draft.
2. The exact literal `modelIdentity` string format remains illustrative,
   not frozen (§9).
3. The one-time wasted default-Rec-model load in §5's mechanism (§14) is
   accepted, not eliminated, by this plan — named honestly as a bounded,
   disclosed cost, not hidden.
4. `_set_flag("allow_objects", True)` is an underscore-prefixed method;
   its *behaviour* is a documented OmegaConf feature, but a future pin
   upgrade of `omegaconf` itself should re-run §5's own empirical proof
   as an acceptance gate (§8, above already names this), since this plan
   does not treat the method's continued existence as permanently
   guaranteed by public API contract.

None of the above undermines this plan's central technical finding
(§5-§6, now proven against real inference output, not merely
construction) or its governance-compliance conclusion (§25).

## 27. Recommendation

READY FOR IMPLEMENTATION
