**Status: PROGRAMME COMPLETE.** This is the Reasoning Context, Bounded Semantic Relevance
("RKS") Completion Review. It records the completion of RKS.1 through RKS.6 (implemented,
verified, and manually accepted in one bounded working session against the adopted
`REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`
and `REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`), together
with two narrow, bounded production-launch corrections discovered only during the owner-facing
UI's own live manual acceptance, and the manual acceptance result itself. It authorises no new
implementation; it closes the programme this repository's own governance had already adopted and
left blocked only on Unit 9.7.1/Unit 9.7.3 existing.

# Reasoning Context — Bounded Semantic Relevance — Completion Review

Repository: `parker-platform`. Branch `main`. Baseline HEAD at the start of this work:
`3d0526c7ff82b2886fca90953ce4d29b90e78243` ("Add explicit owner persistence directive
recognition"), equal to `origin/main`.

## 1. Programme Purpose

RKS exists to close the Live-Reasoning Integration gate the adopted Programme 3, Unit 9.7
Implementation Plan's own Section 8 named as a blocked, out-of-scope dependency: Unit 9.7 wired
Bounded Semantic Relevance into `DefaultKnowledgeRetrieval` only; `DefaultReasoningKnowledgeSource`
-- the actual, sole, live-path surface feeding `DefaultReasoningContextAssembler` and, from there,
the real model prompt -- remained governed by literal, case-insensitive substring matching alone.
This is the same limitation this repository's own operational memory-persistence acceptance work
had already disclosed and deliberately declined to paper over with an ad-hoc token-overlap
workaround, in favour of implementing the already-adopted, already-governed RKS programme instead.

## 2. Authoritative Governance

- `docs/governance/REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_CONTRACT_AND_PERMISSION_SUCCESSOR.md`
  (Adopted) -- the complete authorised processing sequence, the opaque request-token boundary,
  shared Unit 9.7 mechanism reuse, canonical re-resolution, the three-check Pre-disclosure
  re-verification, the split fail-closed table, and the explicit, binding disclaimer that this
  document's own adoption does not by itself "fix semantic recall."
- `docs/governance/REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_IMPLEMENTATION_PLAN.md`
  (Adopted) -- the RKS.1-RKS.6 unit decomposition, dependency ordering (RKS.1/RKS.2 require only
  Unit 9.7.1; RKS.3 is a join requiring both RKS.2 and Unit 9.7.3), frozen boundaries, gap
  analysis, and verification matrix this Completion Review checks off.
- `docs/reviews/PROGRAMME_3_UNIT_9_7_BOUNDED_SEMANTIC_RELEVANCE_COMPLETION_REVIEW.md` (Programme
  Complete) -- confirms Unit 9.7.1 (Relevance Contract Types) and Unit 9.7.3 (Local Relevance
  Mechanism Adapter, the concrete `QmdRelevanceMechanism`) both already exist and are already
  wired into `DefaultKnowledgeRetrieval` in production, satisfying RKS's own only two genuine
  dependencies before this work began.
- `docs/decisions/CDR-008_MEMORY_CORE_DOWNSTREAM_RELEVANCE_BOUNDARY.md` (Accepted, Canonical,
  Frozen) -- unaffected; Memory Core's own interface is untouched by this work.
- The operational memory-persistence acceptance thread that preceded and motivated this
  programme's implementation (CLI controlled acceptance, then owner-UI manual acceptance) --
  referenced here as the origin of the demonstrated defect this programme corrects, not
  re-litigated.

## 3. What Was Already True, And What This Work Closed

Confirmed by fresh inspection before any code was written: neither RKS.1 through RKS.6 existed in
`src/`/`tests/` anywhere (`DefaultReasoningKnowledgeSource.kt` still implemented only the original,
frozen ten-step literal-substring algorithm), while Unit 9.7.1 (`src/interfaces/RelevanceMechanism.kt`)
and Unit 9.7.3 (`src/runtime/QmdRelevanceMechanism.kt`) were both already complete, accepted, and
wired into `DefaultKnowledgeRetrieval` in `src/composition/ParkerRuntime.kt`. RKS's own dependency
gate was therefore already satisfied; no further governance cycle was required before implementation
could begin, and none was initiated.

## 4. Demonstrated Defect This Programme Corrects

The realistic owner-UI acceptance case that motivated this work, precisely:

1. Owner submitted, through the real production owner UI: `Remember that the Parker UI persistence
   test codeword is cedar-orbit-5924.` -- durably admitted into Memory Core (Assertion) and
   Knowledge Memory (Knowledge Item), both confirmed via direct, independent decode of the
   production durability logs.
2. Parker was fully stopped and restarted from a fresh runtime instance. Canonical recovery was
   independently verified (durability logs intact, hash-chain unbroken, process reached Ready).
3. The owner asked, in ordinary natural phrasing, not literally repeating the remembered statement's
   own wording: `What is the Parker UI persistence test codeword?`
4. `DefaultReasoningKnowledgeSource.recall`'s then-only relevance test --
   `content.contains(query.relevance, ignoreCase = true)`, where `relevance` is the owner's entire
   message text verbatim -- returned **zero** candidates: the stored assertion's content did not
   contain the full question sentence as a literal substring. This is not a paraphrase-recall
   limitation in the ordinary sense; it is structural -- a question and its own answering statement
   essentially never share full-sentence text, so this mechanism could not have recalled almost any
   naturally-phrased owner question, independent of QMD or any semantic capability at all.
5. With zero grounding in the assembled `ReasoningContext`, the local model (`qwen2.5-coder:7b`)
   did not refuse or disclose uncertainty -- it fabricated a confident, wrong answer: **"PARK."**

Forensic trace confirmed, independently, that every other boundary in the persistence pipeline
(explicit owner directive recognition, Memory Core admission, Knowledge Item promotion, durability
writes, clean shutdown, fresh-runtime canonical recovery) was already correct. The first, and only,
failed boundary was retrieval -- specifically the literal-substring relevance test -- confirming this
was the correct, narrowly-scoped target for RKS, not a persistence or recovery defect and not a
reason to reopen any earlier-accepted boundary.

## 5. RKS.1-RKS.6 Implementation Summary

- **RKS.1 (Shared Contract Compatibility Gate).** Confirmed, not assumed: Unit 9.7.1's
  `RelevanceCandidateToken`/`RelevanceCandidate`/`RelevanceRequest`/`RelevanceResult`/
  `RelevanceMechanism` types are directly consumable from `parker.core.runtime` unchanged, with zero
  modification and zero extension. No file impact, per the adopted plan's own Section 9 definition
  of this unit.
- **RKS.2 (Fallback Trigger and Closed Candidate Set).** `DefaultReasoningKnowledgeSource.recall`'s
  steps 7-8 restructured: the dereference pass now retains an item-to-content pairing (`dereferenced`)
  instead of discarding it once the current item's own structural test completes. One or more
  structural matches continue to produce today's exact, unmodified result; Bounded Relevance
  Computation runs only on a genuine, successful, zero-structural-match outcome over a non-empty
  dereferenced set.
- **RKS.3 (Mechanism Invocation and Token Minting).** `resolveSemanticFallback` mints one opaque,
  request-scoped `RelevanceCandidateToken` per candidate, invokes the shared `relevanceMechanism`
  with only the query text and the token-to-content mapping -- never a `KnowledgeId`,
  `MemoryCoreRecordReference`, `evidentialState`, `status`, or `StalenessDisclosure`.
- **RKS.4 (Three-Check Pre-Disclosure Re-Verification).** `resolveSemanticResult` validates every
  returned token fail-closed (unknown, duplicate, or excess token is a thrown integrity fault, never
  silently repaired), then performs three fresh checks per surviving candidate before any
  disclosure: (A) `KnowledgeItemPersistence.find`, (B) a fresh `PermissionEngine.evaluate` for the
  identical item-level intent, and (C) a fresh, second Memory Core dereference -- the check this
  surface needs beyond Unit 9.7's own two-check pattern, since this class discloses live content,
  not merely a `basis` string. Disclosed content always comes from check C, never from
  `RelevanceCandidate.content` and never from the Pre-computation snapshot.
- **RKS.5 (Runtime Composition Wiring).** `src/composition/ParkerRuntime.kt` passes the identical
  `relevanceMechanism` instance already constructed for `DefaultKnowledgeRetrieval` into
  `DefaultReasoningKnowledgeSource`'s own constructor -- one shared `QmdRelevanceMechanism` object,
  the same frozen identity/version/configuration, backing both surfaces. Verified directly, not by
  construction alone: `ParkerRuntimeReasoningKnowledgeSourceCompositionTest.kt` asserts
  `assertSame` between the two surfaces' own reflected `relevanceMechanism` fields.
- **RKS.6 (Live-Reasoning End-to-End Verification).** Automated: the existing `aadd596`-lineage
  fixture in `ParkerRuntimeReasoningContextIntegrationTest.kt` (the six-memory "emergency vet"
  paraphrase case, matching the Unit 9.7 mechanism-selection spike's own accepted discrimination
  test) converted from a negative ("does not recall") control into a positive proof that the real,
  live QMD mechanism now recalls the correct memory and discriminates it from five distractors,
  including the "emergency plumber" token-overlap trap the spike specifically proved QMD gets right
  and a naive comparator does not. Live-QMD-gated (`assumeTrue`), mirroring
  `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own established portability discipline -- skipped,
  not failed, in any environment without a local QMD checkout provisioned. **Manual: Steve's own
  real owner-UI acceptance (Section 8, below) is the authoritative, real-runtime instance of this
  same proof, and is the evidence this Completion Review actually relies on for closure**, per the
  adopted plan's own Section 18(B) requirement that programme-level closure needs the real,
  fully-composed `ParkerRuntime`'s own live proof, not code compiling or the mechanism merely being
  wired.

## 6. Two Bounded Production-Launch Corrections, Discovered Only During Manual UI Acceptance

Both were launch/configuration defects in the owner-facing UI's own entry point, not in RKS/QMD
architecture, and neither was anticipated by the CLI-only controlled acceptance that preceded this
programme (the CLI runs inside Docker, whose own `WORKDIR` and `docker-compose.yml` environment
already satisfied both assumptions correctly).

### 6.1 Working-directory-dependent bridge resolution

**Failure observed:** `Error: Cannot find module '/home/steve/parker-platform/ui-desktop/tools/qmd-relevance-bridge.mts', code: 'MODULE_NOT_FOUND'`, reached from `QmdRelevanceMechanism.rank`.

**Root cause:** `QmdRelevanceMechanismConfiguration`'s default `bridgeScriptPath` is explicitly
documented as resolved repository-relative, from the live process's own working directory, at
construction time. `ui-desktop/build.gradle.kts`'s `runOwnerUi` `JavaExec` task never set
`workingDir`, so Gradle defaulted it to the `ui-desktop/` subproject directory rather than the
repository root -- true of no other production entry point (the CLI's own Docker `WORKDIR` and the
root project's own `test` task both already satisfy the documented assumption).

**Correction:** `ui-desktop/build.gradle.kts`, `runOwnerUi` task -- `workingDir =
rootProject.projectDir`, plus a `doFirst` guard that fails the task immediately, with a clear
diagnostic, if that assumption is ever violated again. No Kotlin production file changed; no
path-resolution algorithm changed. Regression coverage: a new test in
`ParkerRuntimeConfigLoaderTest.kt` proving the default `qmdBridgeScriptPath` resolves to a file that
genuinely exists on disk, not merely a string of the expected shape.

### 6.2 Missing TypeScript loader for the `.mts` bridge

**Failure observed:** `TypeError [ERR_UNKNOWN_FILE_EXTENSION]: Unknown file extension ".mts"`,
reached from the same call site, after the path defect above was corrected.

**Root cause:** the governed bridge script is a `.mts` file; plain `node` cannot execute it without
a TypeScript-capable loader (`tsx`) named in `additionalNodeArguments`, itself derived only from
`PARKER_QMD_TSX_CLI_PATH`. The production launch command supplied `PARKER_QMD_SOURCE_ROOT` but had
omitted this independent, separately-required variable -- `docker-compose.yml`'s own CLI path
already sets both explicitly, so this gap was specific to the bare-JVM UI launch, not to any code
path.

**Correction, in two parts, deliberately not conflated:**
- *Operational:* the production UI launch command was corrected to also supply
  `PARKER_QMD_TSX_CLI_PATH=/home/steve/qmd/node_modules/tsx/dist/cli.mjs` -- the existing, already-governed
  tsx CLI entry point this host's own QMD checkout already provides, exactly as
  `QmdRelevanceMechanismLiveAcceptanceTest.kt`'s own `liveConfiguration()` and `docker-compose.yml`
  already derive/declare it. No new configuration surface was invented.
- *Defensive:* `ProcessBuilderQmdSubprocessInvoker.invoke` (`src/runtime/QmdRelevanceMechanism.kt`)
  gained a narrow pre-flight check: if `bridgeScriptPath` ends in `.mts`/`.cts`/`.ts` and
  `additionalNodeArguments` is empty, `invoke` reports this through the identical
  `QmdSubprocessInvocationResult(exitCode = -1, ...)` channel this class already uses for
  process-start failure -- never a new failure shape, never a thrown exception escaping `invoke`
  itself, never a change to what command line a correctly configured deployment ends up running.
  This was deliberately **not** placed in `QmdRelevanceMechanismConfiguration`'s own `init` block:
  that configuration is constructed unconditionally by every `ParkerRuntime.start()` call, including
  every composition test that never reaches the QMD fallback at all, so an eager check there would
  have made `additionalNodeArguments`/the tsx path a de facto required value for constructing any
  `ParkerRuntime` anywhere -- exactly the "genuinely has no portable, machine-agnostic default"
  problem `QmdRelevanceMechanismConfiguration`'s own KDoc already, deliberately, declines to solve
  by invented derivation. Placing the check at the concrete, production-only invoker instead keeps
  it lazy (only evaluated when a real subprocess is actually about to be spawned) and scoped to
  exactly the demonstrated defect. Regression coverage: new
  `tests/runtime/ProcessBuilderQmdSubprocessInvokerTest.kt` (5 cases), proving the guard fires for
  `.mts`/`.cts`/`.ts` with no loader and, just as importantly, proving it does **not** fire when a
  loader is configured or the script is not TypeScript.

Neither correction touches `DefaultReasoningKnowledgeSource`, `DefaultKnowledgeRetrieval`,
`RelevanceMechanism`, `QmdRelevanceMechanism.rank`'s own token/score parsing or ordering, or the
shared-instance wiring RKS.5 established.

## 7. Governance Properties Re-Confirmed By Fresh Inspection At Closeout

Every property below was checked directly against the final diff, not assumed carried over from
drafting:

- QMD remains subordinate, search-only relevance infrastructure -- `RelevanceMechanism.rank`
  returns only an ordering over opaque tokens it was given; it is structurally incapable of
  returning content, a `KnowledgeId`, or any write.
- Memory Core and Knowledge Items remain sole canonical authority -- every disclosed
  `SafeKnowledgeResultEntry`, on both the structural and fallback paths, is built from a fresh
  `KnowledgeItemPersistence`/Memory Core read, never from mechanism output or a stale snapshot.
- QMD cannot create, modify, delete, promote, or redefine canonical memory -- no write-capable
  dependency of any kind is reachable from `RelevanceMechanism`, `QmdRelevanceMechanism`, or
  `ProcessBuilderQmdSubprocessInvoker`.
- `DefaultReasoningKnowledgeSource` uses Bounded Relevance Computation only at the already-authorised
  fallback boundary -- confirmed by the `when` expression's own ordering in `recall`: structural
  match first, fallback only on a genuine, non-empty-set, zero-match outcome.
- Structural retrieval remains ahead of bounded relevance, exactly as the adopted contract requires.
- Fail-closed candidate-token minting/resolution is intact -- unknown, duplicate, and excess-token
  cases each throw, unchanged from RKS.3/RKS.4's own design.
- Pre-disclosure re-verification (three checks, A/B/C) is intact and unchanged.
- The same single production `QmdRelevanceMechanism` instance backs both `DefaultKnowledgeRetrieval`
  and `DefaultReasoningKnowledgeSource` -- one construction site in `ParkerRuntime.kt`, verified by a
  same-instance composition test, not merely by inspection.
- No duplicate relevance mechanism and no UI-specific retrieval architecture exists anywhere in the
  diff -- the `ui-desktop` changes are exclusively Gradle launch configuration (`workingDir`, a
  `doFirst` diagnostic guard); zero Kotlin production logic was added under `ui-desktop/`.
- The UI working-directory correction is limited exactly to making the already-documented
  repository-relative bridge assumption true for this one entry point -- no path-resolution
  algorithm in `ParkerRuntimeConfig`/`QmdRelevanceMechanismConfiguration` was changed.
- The TypeScript pre-flight guard changes no governed QMD mechanism semantics -- it intercepts only
  before a doomed process would otherwise be spawned, reuses the exact existing failure-result
  shape, and never alters the command line a correctly configured deployment runs.

No discrepancy was found. Nothing in this section required stopping to report a defect against
governance itself.

## 8. Manual Owner-UI Acceptance -- The Authoritative Live Proof

Performed by Steve, personally, through the real, production-composed owner UI (`:ui-desktop:runOwnerUi`),
against the real production SSD-backed durability stores (`/mnt/parker-data/parker/{memory-core,knowledge-items}`),
the real local Ollama/`qwen2.5-coder:7b` model, and the real, locally-provisioned QMD 2.8.3 checkout
and embedding-model cache on this host -- not a fake, stub, or simulated component anywhere in the
path.

1. Owner submitted, through the real UI: `Remember that the Parker UI persistence test codeword is
   cedar-orbit-5924.` Parker acknowledged. This memory was durably written before this session's own
   work began and was **not** resubmitted at any point during RKS implementation or the two
   corrections above.
2. Parker was stopped and a genuinely fresh runtime instance was started, more than once, across the
   session -- including once after each of the two corrections in Section 6.
3. In the final, corrected runtime, Steve asked, through the real UI, using natural phrasing that
   does not literally repeat the remembered statement's own wording: `What is the Parker UI
   persistence test codeword?`
4. Parker responded: **"The Parker UI persistence test codeword is cedar-orbit-5924."** The UI
   remained Ready throughout; no crash, no fabricated content, no ungrounded guess.

This result is the real, same-runtime, live proof the adopted RKS Implementation Plan's own Section
16 item 11 and Section 18(B) require -- inspected here as the owner-visible outcome of exactly that
mechanism, not inferred from a lesser signal.

### 8.1 What this acceptance proves

The complete chain: explicit owner `REMEMBER` recognition -> canonical Memory Core/Knowledge Item
persistence -> durable write -> full stop -> fresh runtime -> canonical recovery -> literal
structural retrieval (correctly finding nothing for this naturally-phrased question) -> Bounded
Relevance Computation fallback, using the real, shared `QmdRelevanceMechanism` -> fail-closed token
resolution -> three-check Pre-disclosure re-verification against live canonical state -> correct
content included in the real assembled `ReasoningContext` -> correct, grounded, owner-visible
response -- all through the real owner-facing UI runtime, not the CLI, not a test fixture.

### 8.2 What this acceptance does not prove, and does not claim

- It does not prove semantic recall for questions unrelated to the one exercised phrasing, nor for
  every conceivable paraphrase shape; RKS.6's own automated six-memory discrimination fixture, not
  this one manual question, is what establishes the mechanism's own discrimination behaviour in
  breadth (Section 5, above).
- It does not establish document ingestion, evidence-derived knowledge, or any capability outside
  the explicit-owner-`REMEMBER` -> recall path already governed here.
- It does not weaken, and should not be read as re-opening, any earlier-accepted persistence,
  authorization, or durability boundary -- all were independently re-confirmed correct, not
  re-tested by this acceptance's own success.
- It relies on this specific host's own local QMD/tsx provisioning (Section 9, below); it is not a
  claim that Bounded Relevance Computation now requires no deployment-specific configuration.

## 9. Residual Operational Requirement -- Local QMD Provisioning For Bare-JVM/UI Operation

Unchanged by this work, restated for operational clarity: any runtime that can reach Bounded
Relevance Computation's fallback branch -- which now means any production `ParkerRuntime`, since the
fallback is unconditionally wired -- requires a genuinely local, already-provisioned QMD 2.8.3
checkout and embedding-model cache, plus an explicit TypeScript loader path, to avoid a fail-closed
mechanism-unavailable outcome the first time a query needs it:

- `PARKER_QMD_SOURCE_ROOT` -- the local QMD checkout root (this host: `/home/steve/qmd`).
- `PARKER_QMD_TSX_CLI_PATH` -- the tsx CLI entry point (this host: `/home/steve/qmd/node_modules/tsx/dist/cli.mjs`).
- The embedding model itself, resolved by QMD's own default, portable, `XDG_CACHE_HOME`-derived
  cache location (this host: `/home/steve/.cache/qmd/models`) unless `PARKER_QMD_MODEL_CACHE_DIR`
  overrides it.

The Docker/CLI production composition (`docker-compose.yml`) already sets the equivalent values
correctly and was not part of either defect. The bare-JVM `:ui-desktop:runOwnerUi` launch path now
also requires them to be supplied explicitly on each invocation; no default derivation was
introduced (Section 6.2, above, explains why one was deliberately not added).

## 10. Verification

Full regression suite result recorded at closeout (see the closeout report accompanying this
document's own commit for the exact, final count) -- zero failures, zero errors, matching the
pre-existing live-QMD-gated skip count (5 pre-existing plus the 2 RKS.6 live-QMD-gated automated
tests, unchanged in number by the two corrections in Section 6). Targeted suites covering
`DefaultReasoningKnowledgeSource`, `QmdRelevanceMechanism`, `ProcessBuilderQmdSubprocessInvoker`, the
Reasoning Knowledge Source and Knowledge Retrieval composition graphs, the Reasoning Context
integration fixtures, and `ParkerRuntimeConfigLoader` all pass independently.

## 11. Final Verdict

**PROGRAMME COMPLETE.** RKS.1 through RKS.6 are each independently verified against their own
adopted completion criteria; no later unit weakened an earlier one's accepted property; both
production-launch corrections discovered during manual acceptance are bounded, narrowly scoped, and
independently regression-tested; the real, live, same-runtime owner-UI acceptance (Section 8)
satisfies the adopted Implementation Plan's own Section 18(B) programme-level closure requirement.
No genuine defect or unresolved contradiction was found during this review.

```text
REASONING KNOWLEDGE SOURCE BOUNDED SEMANTIC RELEVANCE -- PROGRAMME COMPLETE
OPERATIONAL MEMORY PERSISTENCE (OWNER-FACING UI) -- ACCEPTED
```
