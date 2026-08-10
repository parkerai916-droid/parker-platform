**Status:** Unit 3-C Timeout and Inference-Latency Investigation Review — **INVESTIGATION COMPLETE. FINDING: 30,000 ms is empirically too short (Finding B), primarily driven by cold-start latency.** This is investigation and governance analysis only. No campaign was run, no code was modified, no timeout was changed, and zero new `/api/generate` calls were made — all evidence below is derived from artifacts, source, and governance documents already committed before this task began.

# Unit 3-C Timeout and Inference-Latency Investigation Review

## 1. Baseline

`HEAD` = `origin/main` = `3a7d59d8493066fbf5514ecc17432ee7d508f325`, clean, independently re-confirmed at task start. Governance read fresh: Unit 3-A Reliability Contract Scope Lock, Unit 3-C Scope Lock, corrected Implementation/Execution Plan, the full Completion/Readiness review chain, Approval Review 4, the Execution Evidence Review (Attempts 1–3), its Independent Constitutional Review, the current `Unit3CDiskSpaceGate`/`Unit3COrchestrationDriver`/`Unit3CArmLedger` implementation, `LocalHttpModelInferenceClient`/`ParkerRuntimeConfig` (production timeout path), and the preserved Unit 2/Unit 2-D raw evidence.

## 2. Attempt 3 reconstruction

From primary evidence (the committed Execution Evidence Review's Attempt 3 section, the JUnit XML report, and the campaign directory's own surviving `identity.txt`, all independently re-read for this task):

- **Request start:** inferred from the JUnit testcase's own `timestamp="2026-08-10T06:28:22"` and the campaign directory's file timestamps (`Aug 10 06:28`).
- **Timeout firing:** testcase `time="30.861"` seconds — consistent with a genuine ~30-second wait for an HTTP response followed by client-side cancellation overhead, not an instant connection failure.
- **Whether model load began:** `ps aux`, captured immediately after the failure (preserved in the prior task's own record), showed the Ollama `llama-server` subprocess for `qwen2.5-coder:7b` running since the exact same wall-clock time as the campaign, consuming 88.9–160% CPU and ~75% of system memory — consistent with active model loading and/or inference, not an idle or crashed process.
- **Whether model generation began:** cannot be determined from client-side evidence alone. The client's `HttpClient.sendAsync` call was cancelled (`future.cancel(true)`, `LocalHttpModelInferenceClient.infer`) on timeout; no Ollama-side request-completion log was captured by this programme (no server-side timing endpoint was queried, and none is authorized for retroactive capture in this task).
- **Whether Ollama timing metadata survived:** no. The client never received a response body, so no `eval_duration`/`load_duration`/`total_duration` fields (Ollama's own `/api/generate` response metadata) were ever captured. This is a genuine evidentiary gap — Phase 8 addresses it as a durability question.
- **Whether the model process remained active after abandonment:** yes, independently confirmed at the time — the `llama-server` process was still running, still consuming heavy CPU, after the client had already given up and the Gradle task had already reported failure. By the start of *this* task, that process had exited (only the base `ollama serve` supervisor remains), consistent with either the request eventually completing/failing server-side or the model being unloaded after Ollama's own idle keep-alive elapsed.
- **Classification of the specific cause:** **STRONGLY SUPPORTED, not CONFIRMED**, that this was a cold-start load exceeding the 30-second timeout, rather than prompt evaluation, token generation, or transport delay in isolation. This classification does not rest on CPU usage alone (Phase 2's own explicit caution) — see Section 3.

## 3. Cold vs. warm evidence

**Why cold-start is the leading explanation, independently reasoned from multiple lines of evidence, not asserted from CPU alone:**

1. **Timing of last prior qwen inference call.** The most recent real inference call to `qwen2.5-coder:7b` anywhere in this programme's history before Attempt 3 belongs to Unit 2-D's own campaign, timestamped `Aug 9`. Attempt 3 ran on `Aug 10`. Ollama's default idle keep-alive unloads an unused model well within that gap; no intervening call of any kind (only read-only `/api/tags`/`/api/show` metadata calls, which do not load model weights) occurred in between.
2. **Process start time coincidence.** The `llama-server` subprocess's own start time exactly matched the campaign's own start time (`06:28`), consistent with Ollama spawning a fresh worker process specifically to serve this request because none was already resident.
3. **Historical precedent for this exact model.** Two independent, real, historical cold-start observations exist for `qwen2.5-coder:7b` on this same hardware (Section 4): `39.92 s` (Unit 2's own first warm-up call) and `48.57 s` (Unit 2-D's own first qwen warm-up call). Both are dramatically higher than every other observed *warm* qwen call (max `27.99 s`) and both would independently have exceeded a 30-second timeout too, had one been in effect at the time.
4. **Sustained post-abandonment CPU activity**, offered only as corroborating, not primary, evidence per Phase 2's own explicit caution against over-relying on it alone.

**A. Cold-load latency estimate:** not directly measured for Attempt 3 itself (the call never completed). By analogy to the two historical qwen cold-start observations, **plausibly in the 40–65 second range** on this hardware — both historical values fall in that band, and Attempt 3's own timeout at 30 s is fully consistent with (not contradicted by) a true latency in that range.

**B. Prompt-evaluation latency estimate:** cannot be isolated from load latency for Attempt 3 (no partial timing survived). From the historical warm-call record (Section 4), prompt evaluation for comparable minimal-production-context prompts appears to contribute somewhere within the `1–20 s` range depending on context size, with `mixed-full-production-like`/`conversation-history` contexts trending toward the higher end (`18–28 s` observed).

**C. Generation latency estimate:** likewise not isolable from the other two components for Attempt 3. No component-level breakdown is available anywhere in this programme's preserved evidence (no run captured Ollama's own `load_duration`/`prompt_eval_duration`/`eval_duration` response fields).

**D. Total request latency:** unknown and unbounded above `30.861 s` (the point of client abandonment) for Attempt 3 specifically. The historical record bounds a *plausible* total at up to `65.40 s` (the single worst observed cold-start, for `llama3.2:3b`, on the same hardware) to `48.57 s` (the worst observed *qwen*-specific cold-start).

**Where exact decomposition cannot be proven, this is stated explicitly:** components A–C above cannot be separated for Attempt 3 with the evidence this programme currently preserves. Only the aggregate (D) is bounded, and only from below.

## 4. Historical latency evidence table

All values independently re-extracted from the `latencyNanos` field of the preserved, sealed, hash-verified raw evidence files under `/var/lib/parker/reasoning-protocol-live-model/`. No timing value below is invented or estimated without saying so explicitly.

| Source | Prompt/context class | Cold/warm | Latency | Timeout configured | Completed? | Confidence/limitations |
|---|---|---|---|---|---|---|
| Unit 2 (`cbecaf3`), STAGE-0, WARMUP-01 | synthetic warm-up | **Cold** (first call of campaign) | 39.92 s | 90,000 ms | Yes | Direct measurement |
| Unit 2, STAGE-0, WARMUP-02 | synthetic warm-up | Warm | 2.37 s | 90,000 ms | Yes | Direct measurement |
| Unit 2, STAGE-0, WARMUP-03 | synthetic warm-up | Warm | 2.16 s | 90,000 ms | Yes | Direct measurement |
| Unit 2, STAGE-0, PF01 (R01-direct) | minimal-production-context | Warm | 22.50 s | 90,000 ms | Yes (semantic mismatch, not a transport issue — see Stage-0 Failure Review) | Direct measurement |
| Unit 2-D (`137e1db5`), warmup, qwen | synthetic warm-up | **Cold** (first qwen call of campaign) | 48.57 s | 90,000 ms | Yes | Direct measurement |
| Unit 2-D, warmup, llama | synthetic warm-up | **Cold** (first llama call of campaign) | 65.40 s | 90,000 ms | Yes | Direct measurement; different model, offered as an upper-bound reference only |
| Unit 2-D, production-track, DQ1 r01-direct ×10 (minimal-production-context, qwen) | minimal-production-context | Warm | 17.87, 5.22, 3.27, 6.35, 6.33, 5.55, 0.95, 6.82, 3.13, 3.12 s | 90,000 ms | Yes (all 10) | Direct measurement |
| Unit 2-D, production-track, DQ2 (4 fixture categories, minimal-production-context, qwen) | minimal-production-context | Warm | 19.28, 17.10, 18.80, 16.08 s | 90,000 ms | Yes (all 4) | Direct measurement |
| Unit 2-D, production-track, DQ3, mixed-full-production-like (qwen) | mixed-full-production-like | Warm | **27.99 s** | 90,000 ms | Yes | Direct measurement — the single highest warm qwen value observed anywhere in this programme |
| Unit 2-D, production-track, DQ3, conversation-history (qwen) | conversation-history | Warm | 18.58 s | 90,000 ms | Yes | Direct measurement |
| Unit 2-D, candidate-track ×5 (qwen) | minimal-production-context | Warm | 17.09, 2.03, 1.97, 1.55, 6.14 s | 90,000 ms | Yes (all 5) | Direct measurement |
| **Unit 3-C Attempt 3** (`db9c612`), Control warm-up trial 1 | synthetic warm-up | **Plausibly cold** (Section 3) | **> 30.861 s, true value unknown** | **30,000 ms** | **No — timed out** | Client-side timeout only; no server-side completion time captured |
| Historical production runtime override | (unspecified prompts, outside this repository's own preserved evidence) | Unspecified | Unspecified | Raised from 30,000 ms to 90,000 ms "because qwen2.5-coder:7b was taking longer under production-like prompts" (Unit 3-A Scope Lock §4 row 36; Programme Planning Review §12) | N/A | **Referenced only** — no specific latency numbers for this historical incident survive in this repository's own evidence; treated strictly as corroborating context, not a quantified data point |

**Aggregate, qwen2.5-coder:7b only, this hardware:** 26 total historical observations (4 Unit 2 + 22 Unit 2-D), all completed under a 90,000 ms timeout. 2 of 26 (7.7%) were cold-start, both exceeding 30 s (39.92 s, 48.57 s) but well under 90 s (44%, 54% of the 90 s ceiling respectively). 24 of 26 (92.3%) were warm, ranging 0.95–27.99 s, with the single maximum (27.99 s) reaching 93.3% of the *current* Unit 3-C 30,000 ms ceiling even while fully warm.

No timing value in this table is invented; every cell is either a direct measurement independently re-extracted from a sealed, hash-verified artifact, or explicitly marked unspecified/unknown.

## 5. Is 30,000 ms a constitutionally defensible Unit 3-C timeout?

```text
B — 30,000 ms is empirically too short for the governed model/hardware
```

**Not chosen merely because one call timed out.** The full preserved record independently supports this finding on at least two separate grounds: (1) both of the only two historical cold-start observations for this exact model on this exact hardware (39.92 s, 48.57 s) would themselves have exceeded 30,000 ms had that ceiling been in effect at the time, entirely independent of Attempt 3; (2) even restricting to *warm* calls only, the single highest observed warm latency (27.99 s, a `mixed-full-production-like` context) leaves only 6.7% headroom under the current ceiling, meaning the 30-second value was never demonstrated to have adequate margin even for the case it was implicitly assumed to cover. Independently confirmed from the Unit 3-C Scope Lock's own text (§4, referencing `ModelReasoningProvider`'s `30_000` ms *default*) that 30,000 ms was never derived from any percentile or margin analysis for Unit 3-C's own purposes — it was inherited unexamined from Parker's production default, and the Scope Lock itself explicitly lists "timeout value" as Unit 3-A dimension **#12**, "unresolved," pending exactly this kind of evidence-based determination.

## 6. Timeout derivation method

Per the frozen requirement already on record (Unit 3-A Scope Lock §4 row 36; Programme Planning Review §12): *"Production timeout value must derive from measured percentiles, explicit margin, and maximum."* Applying that method to the table in Section 4:

- **Worst observed warm latency (qwen):** 27.99 s.
- **Worst observed cold-start latency (qwen):** 48.57 s.
- **Worst observed cold-start latency (any model, same hardware/harness):** 65.40 s (llama, offered as an upper-bound sanity check, not as direct qwen evidence).
- **The already-used historical ceiling (90,000 ms)** sits at **1.85×** the worst observed qwen cold-start and **1.38×** the worst observed cold-start of any model — i.e., **not an arbitrary round number**, but a value that, when checked against this programme's own preserved evidence, already carries a real, quantifiable margin over every cold-start observation on record, and a still-larger margin (3.2×) over the worst observed warm call.

**Recommendation, if defensible:** reusing **90,000 ms** is the most evidence-grounded candidate available without new data collection, specifically because it is not a fresh proposal — it is the literal value this programme's own prior units (Unit 2, Unit 2-D) already used, on this same hardware, with this same model, across 26 real completed calls, with zero timeouts. This satisfies the "worst observed legitimate cold-start latency + bounded margin" method from Phase 6's own list of acceptable approaches, using data that already exists rather than data that would need to be gathered.

**Confidence and limitation, stated plainly:** this is **STRONGLY SUPPORTED, not CONFIRMED**. Two specific gaps remain: (1) only **2** qwen cold-start observations exist in the entire historical record — a thin base for a hard ceiling, even though both happen to fall comfortably under 90 s; (2) Unit 2/Unit 2-D's prompts, while drawn from the same production prompt-builder family, are not byte-identical to Unit 3-C's own Family A/B/C wrapped prompts, which may differ modestly in length/complexity. A percentile-based ceiling in the formal statistical sense is not reliable at N=24–26; the analysis above uses **maximum-observed-plus-margin**, an explicitly weaker but honestly-stated method, consistent with Phase 6's own instruction to say so when precise decomposition or a large-N percentile cannot be proven.

## 7. Timeout-semantics determination

Independently reasoned from the frozen Unit 3-C Scope Lock's own measurement-invalidating/remedy-performance distinction (§8): that distinction **does not name transport/timeout failures in either list**. Measurement-invalidating failures are exclusively identity/integrity/harness/accounting/corruption issues; remedy-performance failures are exclusively *semantic-output* issues (wrong action, false positive, representation, fidelity) — a timeout produces no output to classify into either category.

```text
D — classify some timeouts as measurement-invalidating and others as performance evidence
```

Specifically recommended, not merely listed among options:

- **A timeout during a warm-up trial** (before any scored/comparative measurement has begun for that arm) is closer in spirit to a harness/environment-readiness failure than to remedy performance — the warm-up's entire governed purpose (per the already-corrected warm-up orchestration defect) is to establish a stable, warm baseline *before* measurement; a warm-up failing to establish that baseline indicates the environment was not ready to measure anything yet, not that a remedy performed poorly. This should be treated as **measurement-invalidating** for that arm.
- **A timeout during a scored trial**, occurring after that arm's own warm-up has already sealed (meaning the model is independently known to already be warm), is a different situation: it could reflect genuine prompt-complexity-driven latency (arguably legitimate remedy-relevant evidence, especially if one family's prompts are systematically longer) or a transient infrastructure blip unrelated to the remedy at all. **This programme's current evidence is insufficient to assign this case confidently to either category** — doing so without further data risks either concealing a real reliability signal (if forced into "measurement-invalidating") or contaminating comparative evidence with infrastructure noise (if forced into "remedy-performance"). This is recorded as an open governance question, not resolved here (Section 12).

`TimeoutCancellationException` does **not** automatically belong to either frozen category by construction — this was independently checked against the Scope Lock's own text, not assumed.

## 8. Durability finding

```text
DEFECT CONFIRMED
```

Independently compared against Unit 2-D's own already-governed, already-accepted precedent, not invented for this task: Unit 2-D's `DiagnosticCampaignRunner` writes a durable `intent.jsonl` record — `{trialId, identity}` — **before** issuing each live call, independently confirmed present in the preserved, sealed Unit 2-D campaign (24 intent records, exactly matching its 24 completed raw records). This is precisely the mechanism that would let a future reviewer distinguish "a trial was never attempted" from "a trial was attempted but never completed" using only durable, campaign-scoped evidence.

`Unit3CArmLedger` (Unit 3-C's own ledger, independently re-read in full for this task) has no equivalent: `checkIdentity` writes `identity.txt` once, before the *first* call of an arm, but no per-trial intent record is written before each individual call, and `appendObservation` writes to `raw.jsonl` only **after** a call completes. Attempt 3 is now direct, live proof of the consequence: `identity.txt` exists and correctly proves the ledger and driver reached the point of attempting the call, but **no durable, campaign-scoped record anywhere states that the first warm-up trial specifically was attempted** — that fact survives only in an ephemeral Gradle test log and JUnit XML report outside the campaign directory, not in anything `Unit3CArmLedger.recover()` could ever read back.

Answering the specific sub-questions:

- **Should an intent record exist before the model call?** Yes — Unit 2-D's own precedent already establishes this as governable, implementable, and low-cost.
- **Should a timed-out request produce a durable terminal observation?** This is a related but separate question from intent recording; Section 7's own unresolved classification (measurement-invalidating vs. remedy-performance for scored-trial timeouts) must be settled before deciding what a "terminal" timeout record should even claim.
- **Should raw transport-failure metadata be persisted?** Independently judged: yes, would materially help future investigations exactly like this one — Attempt 3's own component-level latency question (Section 3) is unanswerable specifically because no such metadata was ever captured.
- **Should the ledger checkpoint it?** Not resolved here — depends on the Section 7 classification decision.
- **Should the campaign remain resumable, halted, or sealed after this kind of failure?** Attempt 3's own campaign state (Execution Evidence Review, Attempt 3) is currently none of these cleanly — it is a crashed, uncategorized, non-sealed, non-halted state, precisely because no code path converts a raw transport exception into any of the driver's own governed outcomes (`SEALED`/`HALTED`/`SAFETY_CHECKPOINT`). This is the same underlying gap already noted, independently, in the Execution Evidence Review's own Attempt 3 §B12 and its Independent Constitutional Review's own §28 — this task's independent re-derivation confirms, rather than merely repeats, that finding.
- **Does the current implementation lose constitutionally relevant evidence when a timeout escapes?** Yes, confirmed: the component-level latency breakdown (Section 3, items A–C) is permanently unrecoverable for Attempt 3 specifically because no intent-before-call or raw-transport-metadata durability existed to capture it.

## 9. Fairness / experimental validity implications

What Unit 3-C must preserve to avoid biasing a future Unit 3-D comparison, independently reasoned from the frozen arm order (`CONTROL → FAMILY_A → FAMILY_B → FAMILY_C`) and the warm-up mechanism's own placement:

- **Cold-start risk is structurally concentrated at the very start of the campaign, not spread evenly across arms.** Control's own warm-up runs first, before any of Control's 145 scored trials, and Control's 145 scored trials all run before Family A begins — meaning that by the time Family A, B, or C issue their first call, the model has already answered at least 148 prior calls and is almost certainly warm. **A single cold-start event at campaign start therefore threatens Control specifically (and only its first warm-up call), not Family A/B/C symmetrically**, unless a long enough pause (e.g., a safety-checkpoint halt, or simply wall-clock gaps between arms) allows Ollama's own idle keep-alive to unload the model between arms — a scenario this programme has no current evidence about one way or the other, since no full campaign has ever run long enough to observe it.
- **If any future timeout-handling change treats scored-trial timeouts as remedy-performance evidence** (Section 7's open question), **prompt-length/complexity differences between families must be distinguished from genuine unreliability** before such evidence is used comparatively: Family B's own candidate prompt (the fuller `FAMILY_B_CANDIDATE_SELECTION_GUIDANCE`) is longer than Control's own default prompt, and the historical record already shows `mixed-full-production-like`/`conversation-history` contexts trending toward higher warm latency (18–28 s) than `minimal-production-context` (1–20 s) — a longer or more complex prompt taking longer to process is not, by itself, evidence of unreliability, and conflating the two would bias Unit 3-D's later comparison against verbose-but-otherwise-sound remedies.
- **Whatever timeout value and handling policy is eventually adopted must apply uniformly across Control and every Family A/B/C arm** — this is already a frozen Scope Lock requirement (§ "the current production inference configuration is held identical across the control arm and every Family A/B/C arm... the timeout value... unless a future, separately-governed act states an affirmative reason to vary it") and nothing in this investigation's own findings suggests varying it per-arm; any future amendment must change the timeout (and/or its semantics) identically for all four arms, or explicitly, separately justify why not.
- **Timeout itself may be a legitimate reliability outcome worth recording**, distinct from "the experiment failed" — this is consistent with the Unit 3-A Reliability Contract's own frozen instruction that timeout/transport be "measured as separate operational axes (H/I), never scored as semantic answers." Any future durability fix should preserve this distinction, not collapse a timeout into either a false "semantic wrong answer" or a silently-discarded non-event.

## 10. Governance impact

This investigation's findings do not themselves amend anything. They establish, for a future, separately-authorized governance task to act on:

- Unit 3-A dimension **#12** (timeout value) now has real, evidence-based material available to resolve it, where previously none existed.
- The Unit 3-C Scope Lock's own timeout provision (§4) explicitly anticipates and permits exactly this kind of amendment ("unless a future, separately-governed act states an affirmative reason to vary it") — this investigation constitutes that affirmative reason, but does not itself constitute the governed act.
- The durability gap (Section 8) is independent of the timeout-value question and would need its own Scope Lock/Plan amendment (or an explicit decision that it is out of scope) regardless of what timeout value is eventually chosen.
- The timeout-semantics question (Section 7) is a precondition for a complete durability fix, not an independent afterthought — a fix that records "timed out" without also deciding whether that record is measurement-invalidating or remedy-performance evidence would only partially close the gap.

## 11. Exact recommended next step

```text
D (primary) combined with B — a small, separately-authorized, narrowly-scoped governance amendment task
```

Specifically recommended, not merely selected from the list: **not** a full fresh latency-characterization campaign (Phase 10 option D taken in isolation) — the existing historical evidence (Section 4) is judged strong enough, on its own, to support a timeout-value amendment to 90,000 ms without first running a new characterization campaign, given that value's own real, multi-campaign, zero-timeout track record on this exact hardware and model. The recommended next step is a combined **Scope Lock / Implementation Plan amendment task** (option B) that: (1) proposes raising Unit 3-C's timeout, grounded explicitly in Section 4's table and Section 6's derivation; (2) proposes the intent-before-call durability correction (Section 8), adopting Unit 2-D's own already-governed pattern; (3) resolves or explicitly further defers the timeout-semantics question (Section 7) for scored-trial timeouts specifically (warm-up timeouts can be resolved now, per Section 7's own reasoning); and (4) passes through this programme's own full Scope Lock amendment → ICR → Plan amendment → ICR → implementation → Completion/Readiness/Approval chain, exactly as every prior Unit 3-C correction has, before any further live-execution attempt. This task does not perform any part of that amendment.

## 12. Prohibited interpretations

This investigation must not be read as: authorization to change the Unit 3-C timeout (no governed act has occurred; the number 90,000 ms above is a recommendation with stated confidence and stated limitations, not a new frozen value); authorization to modify `Unit3CDiskSpaceGate`, `Unit3COrchestrationDriver`, `Unit3CArmLedger`, or any other production/test code; a claim that Attempt 3's specific failure is proven, rather than strongly supported, to be cold-start-caused (Section 2 explicitly withholds "CONFIRMED"); a claim that the historical "90-second production override" incident is itself quantified by this repository's own evidence (it is referenced only, per the Unit 3-A Scope Lock's own framing, as "evidence, not new authority"); a resolution of the scored-trial timeout-semantics question (Section 7 leaves it explicitly open); a claim that Family A, B, or C's own reliability has been measured in any way (no observations of any kind exist for Unit 3-C beyond the single failed warm-up attempt); or authorization to begin Unit 3-D, select a remedy, or run any further campaign.
