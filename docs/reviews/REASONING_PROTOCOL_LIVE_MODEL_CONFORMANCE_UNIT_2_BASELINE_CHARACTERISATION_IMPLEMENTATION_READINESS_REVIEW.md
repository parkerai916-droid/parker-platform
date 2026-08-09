**Status:** Unit 2 Baseline Characterisation Implementation Readiness Review — **PASS**. Implementation-only review against committed baseline `b34f8d0`, the six accepted Unit 2 governance artefacts, the PASS Boundary Review, the committed Unit 1 instrument, and the verification evidence recorded below. No Stage 0 or scored campaign call has executed. Nothing is staged, committed, or pushed.

# Reasoning Protocol Live-Model Conformance — Unit 2 Baseline Characterisation Implementation Readiness Review

## 1. Reviewed authority and surface

The review read and applied the accepted Planning Review, Planning Independent Constitutional Review, Scope Lock, Scope Lock Independent Constitutional Review, Implementation/Execution Plan, Implementation Plan Independent Constitutional Review, and the PASS Boundary Review. It also re-read `build.gradle.kts`, `ReasoningProtocolLiveModelEvaluationHarness.kt`, `ReasoningProtocolLiveModelConformanceTest.kt`, and the accepted Unit 1 governance/reviews.

The implementation surface is exactly the two authorized files:

1. `build.gradle.kts`; and
2. `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt`.

No `src/**`, Unit 1 harness/test, dependency, plugin, settings, `.gitignore`, production configuration, prompt, parser, transport, model, sampling, schema, retry, or repair surface changed. The six already-accepted Unit 2 governance files remain untracked and unchanged by implementation.

## 2. Detached task and live-execution guard — PASS

`reasoningProtocolBaselineCharacterisation` reuses `liveModelEvaluation` output/runtime classpaths, filters exactly to `parker.integration.ReasoningProtocolBaselineCharacterisationTest`, sets only `parker.reasoning.baseline.enabled=true`, and has no lifecycle dependency. The live test first requires that property and then a complete explicit Unit 2 campaign configuration before constructing the definition, Unit 1 harness, HTTP identity preflight, or endpoint client.

Dry-run evidence:

- `test check build assemble --dry-run`: contains no `reasoningProtocolBaselineCharacterisation` or `liveModelEvaluation` task;
- `reasoningProtocolBaselineCharacterisation --dry-run`: contains the detached evaluation compilation and only the dedicated task; and
- the executed Unit 2 task skipped its live method because complete campaign configuration was absent.

No Ollama or other external endpoint call occurred during implementation or verification.

## 3. Frozen corpus, contexts, schedule, and hash — PASS

The driver freezes all 23 governed fixtures with their exact owner text, expected actions and canonical content where deterministic; the distribution is 3 REMEMBER, 13 REPLY, 5 GOAL, and 2 NOACTION. It freezes the exact twelve sentinels and reuses all nine Unit 1 `ContextProfileId`/`SyntheticContextProfiles` definitions rather than copying prompt logic.

Offline construction proves:

- Stage 0: 3 warm-ups + 8 preflight = 11 unscored calls;
- Stage 1: 23 × 2 × 30 = 1,380 scored trials;
- Stage 2: 12 × 7 × 30 = 2,520 scored trials;
- total: 3,900 scored trials and 3,911 unique endpoint-call identities;
- every scored cell contains exactly 30 attempts; and
- external trial IDs preserve the frozen fixture IDs and `<campaign>/<stage>/<fixture>/<profile>/<01..30>` identity shape.

The entire schedule is materialized before any executor is supplied. Canonical serialization covers fixture definitions, actions/content, profile identities/order, sentinels, stage membership, attempts, 90,000 ms timeout, request-format identity, every ordered trial, and batch identity. SHA-256 is stable across repeated offline construction. No live result can select or mutate the campaign.

## 4. Batch design and operator control — PASS

The exact fourteen scored batches are present:

- Stage 1: `S1-B01`–`S1-B05`, 10/10/10/10/6 cells;
- Stage 2: `S2-B01`–`S2-B09`, 10/10/10/10/10/10/10/10/4 cells; and
- maximum 10 cells / 300 calls.

Batch selection is a mandatory exact operator parameter; unknown or ambiguous selection blocks. Earlier batches must be complete, sealed, hash-valid and unambiguous before a later batch may run. Execution is a single serial loop with progress events, no worker, prefetch, concurrency, retry, background execution, or automatic “all remaining” mode.

## 5. Exact-once ledger and resume — PASS

The call boundary is:

1. exclusive campaign `FileChannel` lock;
2. append pre-call intent;
3. `FileChannel.force(true)` intent;
4. one serial Unit 1 execution;
5. append full `EvaluationJsonLines.trial` record with deterministic trial ID;
6. `FileChannel.force(true)` raw evidence;
7. atomic checkpoint update; and
8. only then select another trial.

Recovery verifies manifest identity/hash, raw SHA-256/size/line count, prior-manifest chain, known/unique ordered IDs, checkpoint/raw agreement, and intent/raw agreement. Valid raw without checkpoint is adopted and checkpointed without execution. Checkpoint without raw, duplicate/unknown raw, duplicate/unknown intent, non-prefix completion, and intent-only state block. Timeout, transport failure, malformed output, wrong action, and consequential false positives are complete raw records and are never selected again. The implementation expressly does not claim atomicity between endpoint execution and local storage.

## 6. Identity freeze and Ollama evidence — PASS

Exact fingerprint comparison covers repository commit, exact model name, full digest plus `/api/show` SHA-256 evidence, sanitized endpoint, 90,000 ms timeout, production request identity, Ubuntu/runtime identity, optional container identity, corpus hash, profile hash, campaign-definition hash, and manifest version. Each batch additionally verifies the exact prior-manifest hash. No fuzzy comparison exists.

The later live preflight support queries the configured Ollama origin at `GET /api/tags`, resolves a digest only from the exact configured model entry, obtains `POST /api/show` evidence for that model, and hashes the complete show response. Missing or mismatched immutable identity blocks. These methods were not invoked during this implementation phase.

## 7. Stage 0 and fail-closed transition — PASS

`W01`–`W03` use the frozen acknowledgement fixture. `PF01`–`PF08` use the exact governed fixture/profile pairs and are held in Stage 0 outside scored aggregation. Stage 0 must complete with acceptable typed protocol evidence, checkpoint and manifest integrity before its identity-bound seal is written. An adverse preflight is durably preserved as `PREFLIGHT_FAILED`; continuation and scored execution remain blocked. Stage 1 additionally requires both explicit Stage-0 and scored-execution approvals.

## 8. Consequential false-positive pause — PASS

Unexpected parsed REMEMBER or GOAL is evaluated only after raw force and checkpoint. The driver then forces separate consequential evidence, seals/hash-manifests the batch as `PAUSED_CONSEQUENTIAL_FALSE_POSITIVE`, and returns before another executor call. Offline verification proves one call only in the pause case. No automatic resume or rerun exists.

## 9. Artifacts, deterministic reports, and statistics — PASS

The driver requires the accepted `/var/lib/parker/reasoning-protocol-live-model/<campaign-id>/` hierarchy and rejects less than 2 GiB usable space. Offline tests inject temporary directories and never write the server root.

Implemented deterministic evidence includes intent/raw JSONL, atomic checkpoints, batch/campaign identity manifests, SHA-256/size/line-count inventory, prior-manifest chaining, deterministic JSON/CSV/Markdown summaries, four-action confusion matrices, per-action Wilson metrics with integer counts, per-context data, latency quantiles, token totals, H/I operational counts, repeatability, context drift, consequential events, and a content-fidelity worksheet. Complete-campaign reports regenerate from preserved raw observations rather than live results or mutable in-memory-only state.

Wilson calculations use exactly `z = 1.959963984540054` and the frozen formula. Tests cover a known 15/30 interval while preserving integer numerator/denominator.

## 10. Fidelity, drift, and repeatability boundaries — PASS

Only exact canonical content and exact NOACTION can close automatically as exact. Missing typed content is not assessable. Paraphrase/deviation and ordinary semantic Reply/Goal content are emitted to a blinded worksheet with opaque output IDs and empty reviewer-1, reviewer-2, resolution, adjudicator and timestamp fields. Raw records are immutable; no model grading exists.

Context reporting deterministically preserves `STABLE_CORRECT`, `STABLE_INCORRECT`, `CONTEXT_ASSOCIATED_DEGRADATION`, `CONTEXT_ASSOCIATED_IMPROVEMENT`, and `MIXED_INCONCLUSIVE`, using the 10-point stability rule, 20-point effect rule, and non-overlapping Wilson requirement without causal wording. Repeatability independently reports action, representation, byte-content and fidelity-category stability, including stable incorrect.

## 11. Verification evidence

| Verification | Result |
|---|---|
| Unit 2 detached deterministic task | **PASS** — 13 tests, 0 failures, 1 skipped live entry |
| Unit 1 detached class | **PASS** — 16 tests, 0 failures, 1 skipped live smoke |
| Ordinary `test` suite | **QUALIFIED PASS** — 2,015 tests, 1 known unrelated Windows-only failure, 8 skipped |
| Known failure | `OcrStructuralIsolationTest.kt:338`, path-separator assumption; unrelated and unchanged |
| Lifecycle dry runs | **PASS** — Unit 2 task absent from `test`, `check`, `build`, `assemble` |
| `git diff --check` | **PASS** (Git emitted only the existing Windows LF/CRLF advisory for `build.gradle.kts`) |
| Live calls | **ZERO** |
| Stage 0 / scored calls | **NOT EXECUTED** |

The OCR failure is an unrelated environmental/portability observation. It neither compiles nor executes the detached Unit 2 source set and produced no repository modification.

## 12. Verdict

```text
PASS
```

The implemented driver is ready to become the committed Unit 2 campaign instrument. Corrective action: **NONE**. This verdict does not authorize Stage 0 or scored execution; implementation must first receive the independent constitutional readiness review, be committed/pushed with a clean `HEAD == origin/main`, and then receive separate explicit live-execution approval.
