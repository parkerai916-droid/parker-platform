**Status:** Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review — **REFRESHED (sixth refresh). READY.** Every prior refresh was superseded by a live-execution attempt or a subsequent review finding a gap the prior refresh's own "proof" had not actually covered. Attempt 5 (the fifth refresh's own eventual live execution) found exactly such a gap: the fifth refresh's Section 13 table ends at "durable artifacts," treating `Unit3CArmLedger`'s own sealing/exact-once logic as the last link — it never separately traced whether the *payload written into* that durable artifact was itself complete. It was not (Sections 16–18 below). This refresh adds that missing link explicitly and does not repeat the same omission. This document does not authorize execution.

# Unit 3-C Controlled Remedy Experiments — Implementation Readiness Review

## 1. Question

Is the corrected implementation genuinely ready for a future, fresh Explicit Execution Approval Review? This refresh does not assume any prior "READY" verdict — it re-derives readiness fresh, dimension by dimension, with the timeout value, warm-up campaign-halt semantics, scored-trial continuation semantics, and intent/terminal durability now added as their own explicit, individually-proven dimensions, exactly the discipline every prior refresh's own failure (warm-ups counted but unrun; no trigger caller; disk-space gate omitted from the path table) has repeatedly shown is required.

## 2. Executable timeout

**Ready.** `UNIT_3C_TIMEOUT_MS = 90_000L`, independently re-confirmed the active value `Unit3CConfigLoader.load` enforces via `require(live.timeoutMs == UNIT_3C_TIMEOUT_MS)`. Every fixture-driven test environment (`completeUnit3CEnvironment()`, the campaign-ID-rejection test's own environment) supplies `"90000"`, re-verified consistent with the constant — a mismatch here would have caused those tests to fail at the timeout check before ever reaching their own intended assertion, and they do not.

## 3. Model identity / campaign identity / artifact root / disk space

**Ready, unchanged from the fourth refresh.** None of these dimensions is touched by this task's diff; independently re-confirmed via `git diff` showing no hunk in `Unit3CConfigLoader`, `Unit3CArtifactRootPolicy`, or the disk-space gate's own target-path logic.

## 4. Intent durability

**Ready — proven, not assumed.** `ledger.recordIntent(...)` is called, synchronously, immediately before `executor.execute(trial)`, in both `runWarmups` and `runArm`, for every trial with `makesModelCall == true`. Proven via: (a) direct source reading of both call sites; (b) a dedicated source-scan test independently confirming the textual ordering within each function's own body; (c) a dedicated coverage test confirming exactly 145 intent records exist for Control's own 145 scored trials, including the one that times out in that test; (d) a dedicated test confirming Family C, which makes no model call, receives zero intent records of any kind.

## 5. Terminal durability

**Ready.** `Unit3CTimeoutRecord` is written whenever `Unit3CTransportException` is caught, for every classification, in both warm-up and scored-trial paths. Contains no field capable of holding a parser result or semantic classification — independently provable at the type level via reflection (a dedicated test does exactly this), not only by inspecting the code that populates it.

## 6. Exact-once (four-state)

**Ready.** `Unit3CArmLedger.recover()` independently tracks intent/raw/timeout IDs, resolves state (B)+(C) as "do not retry," and fails closed on state (D) (an intent with neither a raw nor a timeout resolution). A dedicated crash-recovery test proves a timed-out trial is not re-issued by a second, independent driver instance. A dedicated test proves state (A) remains distinguishable from (B)/(C)/(D) via `hasIntent`. A dedicated test proves state (D) fails closed on `recover()` directly. Backward compatibility with four pre-existing standalone ledger tests (which call `appendObservation` without ever calling `recordIntent`) is independently re-confirmed intact, because the intent-before-call *policy* lives at the orchestration call-site level, not inside the ledger's own generic, policy-agnostic API.

## 7. Warm-up behavior

**Ready — and now covers the specific failure mode Attempt 3 actually hit, not only the pre-existing identity-mismatch case.** A genuine model-side timeout, transport/provider failure, or ambiguous terminal state during any of the three warm-up trials now durably records the timeout and returns `Unit3CArmOutcome.CAMPAIGN_HALT_WARMUP_TRANSPORT`, which `Unit3COrchestrationDriver.run()`'s own loop is independently confirmed to detect and act on: every subsequent arm is marked `NOT_ATTEMPTED_CAMPAIGN_HALTED` without its own executor ever being invoked. Three dedicated tests (one per classification) independently prove this, including a direct assertion that Family A's own executor is never called.

## 8. Scored-trial behavior

**Ready.** A genuine model-side timeout on a scored Control/Family A/Family B trial durably records the timeout and continues to the next trial, the arm, and the campaign — independently proven per-arm via three dedicated tests. A transport/provider failure or ambiguous state on a scored trial durably records the timeout and halts the affected arm only (reusing the pre-existing, unmodified isolation guarantee every other measurement-invalidating cause already has) — independently proven via two dedicated tests, each confirming every other arm still seals.

## 9. Infrastructure/transport handling

**Ready, with an honestly-stated limitation carried forward, not concealed.** Classification is derived from actual exception type (`TimeoutCancellationException` vs. `IOException` vs. anything else), never a message string. Sub-cases 2 (transport failure) and 3 (provider unavailable) are not distinguishable from each other at this layer and share one classification value (`TRANSPORT_OR_PROVIDER_FAILURE`) — this is not a gap relative to what was governed; the Scored-Trial Timeout Semantics Determination's own Section 4 already accepted this collapse as reasonable specifically for Unit 3-C's loopback-only endpoint.

## 10. Live trigger / Gradle filtering / downstream isolation

**Ready, unchanged.** None of these is touched by this task's diff; independently re-confirmed via `git diff --stat -- build.gradle.kts` (empty) and a fresh re-run of the existing trigger/isolation tests, all still passing.

## 11. Safety checkpoint

**Ready, unaffected.** The adversarial-category false-positive checkpoint is untouched; independently re-confirmed its own existing tests still pass and that the new timeout-handling code path is entirely separate from (runs before, in the same loop, but is a structurally distinct branch from) the checkpoint-triggering branch.

## 12. No campaign mutation

**Ready, independently re-verified specifically for this task.** Attempt 3's own preserved campaign directory (`unit3c-remedy-experiments-20260810/control/warmup/identity.txt`) was re-hashed before and after this task's entire implementation and test-running process: `56af7ca3fa84b1e3c6aca3d4fdd2a23f5884cc5a0ff5a7b574fe2d663d62c9c8`, unchanged. No new campaign directory exists anywhere under the artifact root.

## 13. Full path, re-proven a fifth time, with every link this refresh is aware could be silently omitted

| Link | Evidence |
|---|---|
| Detached Gradle task, structurally isolated | Unchanged; fresh `./gradlew test` contains zero Unit 3-C result files |
| Live trigger reachable, gated | Unchanged; trigger test still reports `skipped` under offline runs |
| Config validation (now against 90,000 ms) | `Unit3CConfigLoader.load`'s own timeout check, re-verified against the new constant |
| Artifact-root validation | Unchanged |
| Disk-space gate (against the durable parent) | Unchanged |
| Driver construction, all four family executors wired | Unchanged |
| **Intent recorded before every live call** | New this refresh — Section 4 |
| Warm-ups execute, and a warm-up transport failure halts the whole campaign | New this refresh — Section 7 |
| Scored trials execute, and a scored-trial model timeout does not halt the arm | New this refresh — Section 8 |
| **Terminal timeout record durably written, never a fabricated semantic action** | New this refresh — Section 5 |
| Exact-once recovery correctly resolves all four states on any restart | New this refresh — Section 6 |
| Durable artifacts | Unchanged, `Unit3CArmLedger`'s own core sealing logic untouched |

## 14. The residual note, carried forward, honestly unchanged in substance

The real, live-calling HTTP request itself remains structurally unexercised by any test in this codebase, exactly as every prior refresh has stated — no test bound by a no-live-calls constraint can exercise it. What was new in the fifth refresh: the code path that would run *immediately after* that HTTP call fails (timeout/transport handling) is independently proven correct entirely offline, using fakes that raise the exact exception types the real production code would raise. Attempt 5 subsequently exercised the real HTTP call for the first time and reached exactly this handling code, genuinely — but the fifth refresh's own readiness table stopped one link short of where the evidence actually needed to be interpretable (Sections 15–18 below), which is why "READY" alone was not sufficient to make Attempt 5's own evidence usable for its intended purpose.

## 15. Sixth refresh — the observation-durability link, traced explicitly

The fifth refresh's Section 13 table's last row, "Durable artifacts | Unchanged, `Unit3CArmLedger`'s own core sealing logic untouched," is accurate as far as it goes but incomplete as a readiness claim: it verifies that a `raw.jsonl` line is written, exactly once, for the correct trial ID, and never duplicated or lost — the *ledger mechanics* — without separately asking whether the *content of that line* carries the fields a future reader needs. It did not, discovered independently by the Unit 3-C Evidence Completeness and Durability Determination Review after Attempt 5's own genuine live execution. This refresh traces the full chain explicitly, as this task's own governing instruction requires:

**provider result → `Unit3CObservation` → `encodeObservation` → durable `raw.jsonl` → later evidence interpretation**

1. **Provider result → `Unit3CObservation`:** `buildModelInvokingExecutor`/`buildFamilyCExecutor` construct a fully-populated object for every completed trial — independently re-confirmed correct field-by-field (Observation Durability Defect Confirmation Review, Section 2). Unaffected by this refresh's own correction; unchanged.
2. **`Unit3CObservation` → `encodeObservation`:** previously discarded ~23 of ~26 already-computed, already-governed fields at this exact step (the defect). Now serializes every field the accepted Plan Section 16 schema requires that `Unit3CObservation` already computes, excluding only `prompt`/`rawRequest`/`rawResponse` per the Determination's own accepted minimum-evidence analysis (Observation Durability Defect Confirmation Review, Sections 6–7).
3. **`encodeObservation` → durable `raw.jsonl`:** `Unit3CArmLedger.appendObservation`'s own outer format, checkpoint timing, and duplicate-prevention are untouched; only the `payload` string's own content changed. Independently proven via a test that reads the written line back from disk (not from any in-memory reference) and decodes it (Observation Durability Defect Confirmation Review, Section 10, test 2).
4. **Durable `raw.jsonl` → later evidence interpretation:** proven via a full, real `Unit3COrchestrationDriver.run()` execution whose durably-written `raw.jsonl` is read from disk after the run completes and shown to contain the real `actualAction` for the exact trial ID that triggers the safety checkpoint — the specific case the Evidence Completeness Determination could previously only narrow by logical deduction from the checkpoint's own trigger condition, now directly, definitively readable (Observation Durability Defect Confirmation Review, Section 10, test 5).

**Ready.** Every link in this chain is now independently traced and independently tested, not merely asserted to exist because the surrounding ledger mechanics were already correct.

## 16. Nullability and no-fabrication, re-verified specifically for the corrected encoding

**Ready.** `semanticCorrect` is durably `null` whenever `actualAction` is durably `null` (a dedicated round-trip test constructs exactly this case — an unclassifiable-response observation — and asserts both fields decode to `null`, and that `parserFailure`'s own real message decodes correctly rather than being dropped or replaced). Family C's model-call-only fields (`modelName`, `modelDigest`, `runtimeIdentity`, `endpointIdentifier`, `timeoutMs`, `inferenceConfigIdentity`, `promptIdentity`, `parserResult`, `latencyNanos`, `transportOutcome`) durably decode to `null`, matching `Unit3CObservation`'s own constructor invariant, not merely omitted from the schema. No completed-observation encoding path can fabricate a `GOAL`/`REPLY`/`REMEMBER`/`NOACTION` value the object itself does not already hold — the encoder only ever reads `observation.actualAction?.name`, never a default or inferred value.

## 17. `contentFidelity` — readiness-relevant boundary, not silently resolved

**Ready, with the same boundary the Evidence Completeness Determination and its own Independent Constitutional Review already drew, not narrowed or widened here.** `contentFidelity` durably serializes as `null` for every trial today because it is never computed — this refresh's own correction does not change that; it only stops silently dropping the (always-null) field. A future reader inspecting a corrected `raw.jsonl` will see `"contentFidelity":null` for every record and must not mistake that for "no fixture defined `expectedContent`" — it means "not computed," a distinct, still-open, correctly out-of-scope condition. Any future Unit 3-D task consuming this evidence must be aware of this distinction; it is not resolved by readiness for a future Unit 3-C campaign.

## 18. Attempt 5 and prior-campaign integrity, re-verified for this refresh

**Ready.** Attempt 3's own preserved directory (`unit3c-remedy-experiments-20260810/control/warmup/identity.txt`) remains hash-unchanged from every prior refresh's own citation (`56af7ca3fa84b1e3c6aca3d4fdd2a23f5884cc5a0ff5a7b574fe2d663d62c9c8`). Attempt 5's own directory (`unit3c-remedy-experiments-20260810-02/`, 26 files) independently re-confirmed to carry file modification times entirely predating this task's session, with no write of any kind issued against `/var/lib/parker/reasoning-protocol-live-model` at any point during this task. This refresh's own correction has no retroactive effect on either preserved campaign — it changes only the code a *future* campaign would run.

## 19. Readiness determination

```text
READY
```

All dimensions independently re-derived as ready, including the observation-durability chain (Sections 15–18) that no prior refresh traced as its own explicit link — the fifth refresh's own table ended at ledger mechanics, which is exactly the gap Attempt 5's own genuine execution exposed. The one residual note (Section 14) remains, unchanged in kind from every prior refresh: only the live HTTP call's own real-world success or failure is unverified by any offline test; every gate, every caller, every timeout-handling branch, and now the full provider-result-to-durable-evidence chain is independently proven to exist, to be reachable, and to behave as governed — including that a future evidence reader can genuinely interpret what gets written, which was not true before this refresh.
