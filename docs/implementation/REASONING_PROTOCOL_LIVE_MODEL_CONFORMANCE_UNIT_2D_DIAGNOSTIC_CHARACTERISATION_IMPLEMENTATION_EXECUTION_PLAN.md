**Status:** Unit 2-D Diagnostic Characterisation Implementation/Execution Plan — **PROPOSED, PENDING ITS OWN INDEPENDENT CONSTITUTIONAL REVIEW.** Governance only against committed baseline `d316032` (the frozen Unit 2-D Scope Lock). No implementation, live model call, or campaign creation is authorized by this document. Implementation requires an accepted Independent Constitutional Review of this Plan; live execution additionally requires an Implementation Readiness Review, its own Independent Constitutional Review, and explicit execution approval.

# Reasoning Protocol Live-Model Conformance Unit 2-D — Diagnostic Characterisation Implementation/Execution Plan

## 1. Constitutional status and authority

The frozen Unit 2-D Scope Lock (`docs/architecture/REASONING_PROTOCOL_LIVE_MODEL_CONFORMANCE_UNIT_2D_DIAGNOSTIC_CHARACTERISATION_SCOPE_LOCK.md`), accepted with qualifications by its Independent Constitutional Review, is controlling. This Plan translates that Scope Lock into an executable procedure; it does not amend, broaden, narrow, or reinterpret any Scope Lock provision. Where this Plan is silent, the Scope Lock governs. Where any conflict is found between this Plan and the Scope Lock, the Scope Lock prevails and this Plan is defective on that point.

This Plan itself authorizes nothing beyond planning. No implementation file may be created or modified on the strength of this document alone; that requires this Plan's own Independent Constitutional Review to accept it first (Section 17).

The existing failed Unit 2 campaign (`qwen25coder7b-baseline-20260809`, artifact root `/var/lib/parker/reasoning-protocol-live-model/qwen25coder7b-baseline-20260809/`) is preserved evidence. Nothing in this Plan reads, writes, resumes, copies from, or otherwise touches it. Every artifact this Plan defines lives in a new, sibling location (Section 8).

## 2. Diagnostic questions mapped to concrete evidence

All fixture text, profile definitions, and model identities below are reused verbatim from the frozen Scope Lock (§7, §9, §10) and independently re-confirmed against the actual frozen Unit 2 corpus text in `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt` during this Plan's drafting. No fixture text is invented here.

### DQ1 — Repeatability

- Fixture: `R01-direct` — `Remember that my synthetic test coffee mug is black.`
- Context profile: `minimal-production-context`
- Model: `qwen2.5-coder:7b`
- Attempts: 10
- Expected action: `REMEMBER`
- Evidence recorded: one `TrialObservation`-shaped record per attempt (prompt, `promptSha256`, raw Ollama envelope, extracted response, parsed action, `representationValid`, `primaryClassification`, `contentFidelity`, latency/endpoint metadata), append-and-flush per attempt.
- Permitted interpretation: whether the ten outcomes are uniform (near-deterministic) or materially mixed (stochastic contribution), per Scope Lock §21 rules 1–2.
- Prohibited interpretation: any population-level failure rate; any claim that REMEMBER fails "in general"; any claim about production behavior beyond this exact fixture/profile/model/commit combination.

### DQ2 — Action-class specificity

- Fixtures, one attempt each, all under `minimal-production-context`, model `qwen2.5-coder:7b`:
  - `P01-ordinary-fact` — `My synthetic desk token is amber.` — expected `REPLY`
  - `P06-greeting` — `Hello Parker.` — expected `REPLY`
  - `G01-multistep` — `Create a three-step checklist for inspecting the synthetic blue test bench.` — expected `GOAL`
  - `N01-heartbeat` — `Synthetic system heartbeat event: no response and no action are required.` — expected `NOACTION`
- Evidence recorded: one observation per fixture, same shape as DQ1.
- Permitted interpretation: whether any of the three non-REMEMBER action families shows a comparable single-attempt semantic miss to the one already observed for REMEMBER at PF01, as a coarse breadth signal only.
- Prohibited interpretation: any failure-rate claim for any action family; any claim that a family is "reliable" from a single correct observation, or "unreliable" from a single miss.

### DQ3 — Context sensitivity

- Fixture: `R01-direct`, model `qwen2.5-coder:7b`, one attempt each under:
  - `mixed-full-production-like`
  - `conversation-history`
- Expected action: `REMEMBER`.
- Evidence recorded: one observation per profile, same shape as DQ1.
- Permitted interpretation: a weak, directional signal only, interpretable solely jointly with DQ1's variability (Scope Lock §10, §21 rule 7).
- Prohibited interpretation: any causal "context caused/fixed this" claim from a single attempt in isolation; any claim independent of the DQ1 baseline.

### DQ4 — Model specificity

- Fixture: `R01-direct`, context profile `minimal-production-context`, model `llama3.2:3b`, one attempt.
- Expected action: `REMEMBER`.
- Evidence recorded: one observation, same shape as DQ1.
- Permitted interpretation: a directional model-specificity signal, read jointly with DQ1, per Scope Lock §21 rules 3–4.
- Prohibited interpretation: "Llama is better," "switch models," any general reliability claim about either model, any conclusion independent of the stated size/specialization confound (Scope Lock §9).

### DQ5 — Decision/rendering coupling

- Fixture: `R01-direct`, under a single, clearly labeled, non-production decision-only variant (Section 6), model `qwen2.5-coder:7b`, 5 attempts.
- Expected action: `REMEMBER` (as a category-only judgment; no content is requested or evaluated).
- Evidence recorded: one observation per attempt, same shape as DQ1, with the record's prompt/track field explicitly marked `candidate-decision-only`, never `production`. The response text itself is a fixed, deliberately content-free placeholder (`"SELECTED"`, Section 6); `contentFidelity` is therefore expected to show `DEVIATION_OR_PARAPHRASE` for every successfully parsed record by design, and is not evidence of anything for this DQ.
- Permitted interpretation: whether accuracy under the decision-only task differs materially from DQ1's joint-task baseline, as suggestive (not dispositive) evidence about coupling, Qwen-only, per Scope Lock §21 rule 6.
- Prohibited interpretation: "the fix," any production-design viability claim, any claim generalizing beyond `qwen2.5-coder:7b`, any reading of DQ5's content fidelity as a finding.

### DQ6 — Representation independence

- No dedicated fixture or call. Cross-cutting analysis over all 22 non-warm-up observations collected under DQ1–DQ5 (10 + 4 + 2 + 1 + 5).
- Evidence recorded: a derived summary in the interpretation worksheet (Section 12) comparing each observation's `representationValid` against its `primaryClassification`/semantic-match outcome.
- Permitted interpretation: whether representation validity and semantic correctness vary independently across this unit's own data, reinforcing the Unit 1/2-established separation.
- Prohibited interpretation: any representation-failure rate; any claim that representation problems are a significant driver of overall unreliability.

## 3. Exact live-call schedule

Reconstructed directly from Scope Lock §18, not invented:

```text
DQ1 repeatability   1 cell  × 10 attempts = 10
DQ2 breadth         4 cells ×  1 attempt  =  4
DQ3 context         2 cells ×  1 attempt  =  2
DQ4 model            1 cell ×  1 attempt  =  1
DQ5 decision-only    1 cell ×  5 attempts =  5
Warm-up              2 cells ×  1 attempt  =  2
                                    TOTAL  = 24
```

10 + 4 + 2 + 1 + 5 + 2 = **24**. This total, and every component within it, is copied from the frozen Scope Lock's own §18 table; nothing here is a new invention, and nothing is added (no retries, no exploratory probes, no extra comparison cells).

### Warm-up fixture — frozen

The proposed version of this Plan prior to its Independent Constitutional Review scheduled two warm-up calls without freezing their exact text. Corrected here by reusing, verbatim, the already-reviewed and already-accepted Unit 2 warm-up fixture rather than inventing a new one:

```text
Owner input:      Synthetic warm-up request: reply with a brief acknowledgement.
Context profile:  minimal-production-context
Expected action:  REPLY (observed, not scored — matching Unit 2's own convention that
                   warm-up output is "observed but not scored")
```

Run once against each model (Qwen, Llama), both within the fixed 24-call total (Section 3 table). Synthetic, non-consequential, and identical in kind to every other fixture in this Plan — it exercises only `ModelReasoningProvider.reason` and terminates at the recorded observation (Section 11); it does not touch Memory, Goal creation, Knowledge Submission, tools, planning, or execution. Its two records are stored in their own dedicated, non-evidentiary path (Section 8) and are excluded from DQ1–DQ6 analysis entirely, exactly as Unit 2 excluded its own warm-up calls from scoring.

### Deterministic execution ordering

All Qwen-only work is scheduled first, in DQ order, followed by the single Llama block — one model switch, at the very end, rather than interleaving:

```text
01  WARMUP-QWEN     / warmup-acknowledgement        / minimal-production-context   / qwen  / 01
02–11  DQ1           / R01-direct                    / minimal-production-context   / qwen  / 01..10
12  DQ2              / P01-ordinary-fact              / minimal-production-context   / qwen  / 01
13  DQ2              / P06-greeting                   / minimal-production-context   / qwen  / 01
14  DQ2              / G01-multistep                  / minimal-production-context   / qwen  / 01
15  DQ2              / N01-heartbeat                  / minimal-production-context   / qwen  / 01
16  DQ3              / R01-direct                     / mixed-full-production-like   / qwen  / 01
17  DQ3              / R01-direct                     / conversation-history         / qwen  / 01
18–22  DQ5           / R01-direct-decision-only       / minimal-production-context   / qwen  / 01..05
23  WARMUP-LLAMA     / warmup-acknowledgement         / minimal-production-context   / llama / 01
24  DQ4              / R01-direct                     / minimal-production-context   / llama / 01
```

This order is fixed at driver startup as part of the campaign definition, exactly as Unit 2's schedule was frozen before any call. It is not re-derived from results, not reordered based on interim outcomes, and not extended. Trial IDs follow the deterministic pattern `<campaign-id>/<DQ-or-warmup-id>/<fixture-id>/<profile-or-variant-id>/<model-short>/<NN>`.

## 4. Model identity

Both `qwen2.5-coder:7b` and `llama3.2:3b` must have their exact digest pinned and verified before any DQ or warm-up call, using the same proven technique Unit 2's already-reviewed, defect-corrected identity extraction established: query `/api/tags`, require exactly one object whose `name`/`model` field matches the configured model string, require a full 64-hex-character `digest` field, and fail closed on zero or multiple matches or a missing/malformed digest. Unit 2's own implementation of this technique (`OllamaIdentityEvidence.exactModelDigest`, `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt`) is declared `private` within Unit 2's own file and is therefore not importable; this Plan requires the same technique to be **reimplemented**, not imported, inside Unit 2-D's own new file (Section 15, Unit B), so that no Unit 2 file is touched. `/api/show` is additionally queried per model and its response hashed, mirroring Unit 2's cross-check convention, so that both the `/api/tags` digest and the `/api/show` evidence are captured and recorded.

No identity call (`/api/tags`, `/api/show`, or `ollama list`/`ollama show`) is made during this planning task. This is a specification of what execution-time code must do, not an action performed now.

## 5. Repository/runtime identity

Planning-time assumption: repository commit `d316032`, endpoint `http://127.0.0.1:11434/api/generate` (matching Unit 2's precedent), evaluation timeout `90,000 ms` (Scope Lock §17, reusing Unit 2's convention), and the Ubuntu Parker host used throughout this session are all expected to still apply at execution time.

Execution-time capture requirement, distinct from the above: the runbook must independently re-verify all of this immediately before warm-up, using the same read-only operator checks Unit 2's own Implementation/Execution Plan §4 already established (`git branch --show-current`, `git rev-parse HEAD`, `git status --short`, `java -version`, `./gradlew --version`, `ollama list`, `ollama show qwen2.5-coder:7b`, `ollama show llama3.2:3b`, `docker inspect` where used, `df -h` on the artifact filesystem) — reused as a technique, not by modifying Unit 2's file. If any identity cannot be pinned, or differs from what this Plan assumed, execution stops before warm-up; this Plan's planning-time assumptions are never silently trusted over execution-time reality.

## 6. Prompt controls

**DQ1–DQ4 (production track).** The byte-identical output of `parker.core.runtime.DefaultReasoningPromptBuilder().buildPrompt(turn, reasoningContext)` is used, unmodified, called directly from production code — no test double, no copied template. This is the same production class Unit 1 and Unit 2 both already relied on unchanged.

**DQ5 (candidate track, exactly one variant, Qwen-only) — corrected.** The version of this section proposed prior to this Plan's Independent Constitutional Review asked for the bare category word with "no colon," which `TaggedReasoningResponseParser` cannot recognize for `GOAL`, `REPLY`, or `REMEMBER` (only exact `NOACTION` has no colon requirement) — confirmed as a blocking defect by direct re-inspection of `src/runtime/ReasoningResponseParser.kt` during correction. A second, related problem was found while designing the fix: `ReasoningProviderResponse.Goal`/`Reply`/`Remember` each `require(text.isNotBlank())` in their own constructors (`src/interfaces/ReasoningProvider.kt`), so simply restoring the colon with nothing after it (`"GOAL:"`) would not merely fail to construct cleanly — `TaggedReasoningResponseParser`'s own `classifyRejected` helper treats any bare, colon-terminated tag as `PrimaryClassification.G` (blank/partial) regardless of *which* tag it was, collapsing `GOAL:`, `REPLY:`, and `REMEMBER:` into one indistinguishable bucket and destroying exactly the semantic-category information DQ5 exists to measure. Neither the bare-word form nor the bare-tag form is usable without modifying production types this Plan is forbidden to touch.

**What DQ5 manipulates:** only whether the model must compose genuine response content after selecting a category. **What it holds constant:** the same four-way action set, the same category-selection guidance text verbatim (the existing `SELECTION_GUIDANCE` wording, reused as reference text, not by importing the private constant), the same colon-tagged syntax the production parser already recognizes, and the same fixture/context/model/commit as DQ1.

```text
Respond with exactly one of the following: GOAL:, REPLY:, REMEMBER:, or NOACTION.

If your answer is GOAL:, REPLY:, or REMEMBER:, write the tag followed by exactly the
single word "SELECTED" and nothing else — no explanation, no restated fact, no
additional sentence.

If your answer is NOACTION, write exactly NOACTION and nothing else.

[the same REPLY/GOAL/REMEMBER/NOACTION selection-guidance paragraphs, reused verbatim]
```

**Why the output remains parseable by the unchanged production parser:** `"GOAL: SELECTED"`, `"REPLY: SELECTED"`, and `"REMEMBER: SELECTED"` each satisfy `trimmed.startsWith("<TAG>:")` exactly as production output does, and `trimmed.removePrefix(<TAG>).trim()` yields `"SELECTED"` — non-blank, so `Goal("SELECTED")`/`Reply("SELECTED")`/`Remember("SELECTED")` construct without throwing. Exact `NOACTION` is unchanged from production behavior. `TaggedReasoningResponseParser`, `Goal`, `Reply`, `Remember`, and `NoAction` are used entirely unmodified; no permissive parser, no schema, and no structured/constrained output is introduced anywhere in this design.

**Why this remains diagnostic rather than a proposed production prompt:** the template never appears in, and is never merged toward, `DefaultReasoningPromptBuilder`; it is defined once, inside Unit 2-D's own new file, never inside `ReasoningPromptBuilder.kt`. Every record produced under it is permanently labeled `candidate-decision-only`, stored under a visibly separate path from the production-track records (Section 8), and is never described in any report as characterizing "the Parker protocol." The placeholder word `"SELECTED"` is deliberately fixed and content-free — chosen specifically so that no genuine composition is required, and so it can never be mistaken for a proposed real response.

**Permitted conclusions:** whether semantic selection accuracy under this narrower, decision-only task differs materially from DQ1's joint decide-and-render baseline — suggestive (not dispositive) evidence about decision/rendering coupling (hypothesis I), Qwen-only, per Scope Lock §21 rule 6.

**Prohibited conclusions:** that the placeholder-word format is a viable production design; that any remedy is proven or disproven; any claim generalizing beyond `qwen2.5-coder:7b`; and — new to this correction — any reading of DQ5's content fidelity as evidence of anything. Because `"SELECTED"` is a fixed, deliberately content-free placeholder rather than an attempt at the real fact, `contentFidelity` will trivially and uninformatively show `DEVIATION_OR_PARAPHRASE` for every successfully parsed DQ5 observation; the interpretation worksheet (Section 12) must record this as an expected, non-evidentiary artifact of the design, never as a finding.

**Proof of which prompt was sent.** Both tracks reuse Unit 1's existing transparent-capture pattern (`TransportCapture`-equivalent): the actual HTTP request body is captured at the transport boundary, and the driver asserts — as a hard-stop check, not a soft warning — that the captured prompt equals byte-for-byte either the live `DefaultReasoningPromptBuilder` output (DQ1–DQ4) or the fixed, corrected DQ5 template above (DQ5), exactly mirroring the existing harness's own `check(capture.prompt == null || capture.prompt == expectedPrompt)` assertion. `promptSha256` is recorded per observation in both tracks.

## 7. Inference controls

The production request shape is reused unchanged, by direct import of the existing, already-reviewed production function `defaultOllamaRequestBody` (`src/runtime/ModelInferenceClient.kt`) — a public production seam already relied on unmodified by both Unit 1 and Unit 2, and reused the same way here: `{"model", "prompt", "stream": false}` only. No `temperature`, `seed`, `top_p`, `top_k`, `format`, or grammar constraint is added anywhere in this Plan, for either model, for either track. No temperature sweep, no seed experiment, no sampling-policy experiment, and no hidden retry exists anywhere in this design; `ModelReasoningProvider`'s own no-retry, no-repair contract is reused unmodified.

**Interpretive limitation, restated precisely for execution-time documentation:** because no sampling parameters are pinned, DQ1's ten outcomes reflect whatever Ollama's effective default sampling behavior is at execution time — itself unmeasured and potentially version-dependent. No DQ1 result may be characterized as a fixed, controllable rate; no repeatability finding may be assumed to transfer to a differently configured or differently versioned deployment; and no claim may be made about *why* variation occurs (sampling-driven versus genuine model uncertainty), since the two are conflated by this unpinned state. This limitation must be reproduced verbatim in the interpretation worksheet (Section 12), not merely asserted once here.

## 8. Artifact architecture

A wholly new, sibling campaign namespace, never inside `qwen25coder7b-baseline-20260809`'s directory.

**Corrected artifact-count accounting.** The version of this section proposed prior to this Plan's Independent Constitutional Review labeled `production-track/raw.jsonl` as holding "DQ1-DQ4 observations (19 records)" — 19 did not reconcile with DQ1+DQ2+DQ3+DQ4's own stated counts (10+4+2+1 = 17), because the two warm-up records were silently, and inconsistently, folded into that figure without being said to be. Corrected by giving warm-ups their own explicit, separate, non-evidentiary path rather than folding them into either substantive track — this does not change the frozen 24-call schedule (Section 3), which is unaffected; it only makes explicit where each of the 24 calls' resulting records is stored:

```text
live inference calls (Section 3, fixed by Scope Lock §18) ............ 24
  warm-up calls (non-evidentiary, excluded from all DQ analysis) ....   2
  DQ1 repeatability calls ..............................................10
  DQ2 breadth calls ..................................................... 4
  DQ3 context calls ..................................................... 2
  DQ4 model-comparison call ............................................. 1
  DQ5 decision-only calls ............................................... 5
                                                            (2+10+4+2+1+5 = 24)

raw observation records — exactly one per live call, same total ...... 24
  warmup/raw.jsonl ....................................................... 2
  production-track/raw.jsonl (DQ1+DQ2+DQ3+DQ4 only) .....................17
  candidate-track/raw.jsonl (DQ5 only) .................................... 5
                                                               (2+17+5 = 24)

intent records — one unified pre-registration of all planned trial IDs,
written once before any call, matching Unit 2's own single intent.jsonl
convention (not split by track) ....................................... 24

checkpoints — one append-only completion ledger per raw-evidence path,
so each ledger's count matches its own raw file exactly:
  warmup/checkpoint.txt ................................................... 2
  production-track/checkpoint.txt ........................................17
  candidate-track/checkpoint.txt ............................................ 5

manifests — one unified manifest.txt, covering the hash/size/line-count of
all three raw files plus overall campaign state (not one manifest per
track; proportionate to this unit's much smaller scale relative to Unit 2's
per-batch manifests) ..................................................... 1

campaign-level metadata records that are not per-call at all:
  campaign-definition.txt (the frozen schedule + hashes) .................. 1
  campaign-identity.txt (commit, both model digests, endpoint, etc.) ...... 1
  campaign.lock ............................................................ 1
  campaign.sealed OR campaign.halted (mutually exclusive terminal marker) . 1
  interpretation-worksheet.md (post-hoc, derived, not raw evidence) ....... 1
  artifact-hash-inventory.txt (covers every file above) ................... 1
```

No use of the word "record" in this Plan is ambiguous as to which of the above it refers to: "raw observation record" always means one of the 24 per-call entries across the three `raw.jsonl` files; "intent record" always means one of the 24 pre-registered entries in the single `intent.jsonl`; no other file is a "record" in this Plan's vocabulary.

```text
Parent: /var/lib/parker/reasoning-protocol-live-model/
New campaign identity pattern: reasoning-protocol-unit-2d-diagnostic-<yyyymmdd>
  (the literal substring "diagnostic" is mandatory in the identity; the exact date
   is fixed only at execution time, not by this Plan)

<campaign-id>/
  campaign.lock
  campaign-definition.txt       -- the frozen 24-cell schedule (Section 3) + fixture/profile/
                                    variant-template hashes; SHA-256 becomes campaignDefinitionHash
  campaign-identity.txt         -- commit, both model name/digest pairs, /api/show hashes,
                                    endpoint, runtime/container identity, timeout
  intent.jsonl                  -- all 24 planned trial IDs, written before any call
  warmup/raw.jsonl               -- 2 non-evidentiary warm-up observations, excluded from DQ1-DQ6
  warmup/checkpoint.txt
  production-track/raw.jsonl    -- DQ1+DQ2+DQ3+DQ4 observations only (17 records)
  production-track/checkpoint.txt
  candidate-track/raw.jsonl     -- DQ5 observations only (5 records), permanently separate path
  candidate-track/checkpoint.txt
  manifest.txt                  -- hash/size/line-count of all three raw artifacts, running state
  campaign.sealed                -- written only when all 24 scheduled calls complete AND every
                                    hard-stop check has passed throughout; presence means
                                    "data collection complete with integrity intact," never
                                    "results were favorable"
  campaign.halted                -- written only on a hard stop (Section 10), carrying a reason
                                    code; mutually exclusive with campaign.sealed; blocks any
                                    further call until a Unit-2-D-scoped Defect Confirmation
                                    Review accepts a correction
  interpretation-worksheet.md    -- populated only after campaign.sealed exists; applies
                                    Section 12's pre-registered rules to the actual results
  artifact-hash-inventory.txt    -- SHA-256/size/line-count of every file above
```

**Authoritative:** `campaign-definition.txt`, `campaign-identity.txt`, all three `raw.jsonl` files, all three `checkpoint.txt` files, `manifest.txt`, and whichever of `campaign.sealed`/`campaign.halted` is present are byte-level ground truth. `interpretation-worksheet.md` is derived commentary, always traceable to specific raw records, never itself primary evidence.

**Isolation guard, required as an execution-time assertion, not merely a naming convention:** the driver must assert, before any call, that its resolved artifact root is neither equal to nor nested inside `/var/lib/parker/reasoning-protocol-live-model/qwen25coder7b-baseline-20260809/`, and must refuse to start if that assertion fails. This is a mandatory implementation requirement (Section 15, Unit B), not an assumption this Plan is content to leave implicit.

None of these artifacts, directories, or files are created by this Plan. This is a specification for future implementation and execution only.

## 9. Classification and observation

Reused unchanged, by direct import from Unit 1's public harness types (`tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`): `TrialObservation`, `PrimaryClassification` (`A`–`I`), `ContentFidelity`, `representationValid`, `EvaluationJsonLines`. No new classification value is added; none of the existing nine `PrimaryClassification` values is redefined.

Mapping preserved exactly as already established and independently re-verified this session:
- semantic action-selection failure → `PrimaryClassification.D` (`actualAction != expectedAction`);
- representation failure → `C`/`E`/`F`/`G` (rejected/untagged/multiple/blank, per `classifyRejected`);
- parser failure → surfaced as `representationValid = false` plus the applicable rejection classification above; the parser itself is not modified;
- timeout → `H`;
- transport/model failure → `I`;
- content-fidelity deviation → the existing `ContentFidelity` enum (`EXACT`/`DEVIATION_OR_PARAPHRASE`/`NOT_APPLICABLE`/`INDETERMINATE`);
- context-sensitive drift → **not** a new per-trial field; computed only at the interpretation-worksheet level (Section 12) by comparing DQ3's two context-profile observations against the DQ1 baseline distribution, exactly as Scope Lock §10 already requires joint reading rather than an automatic field;
- repeatability finding → likewise computed only at the interpretation-worksheet level, over DQ1's ten raw records; not a new per-trial field.

No classification is redefined to make any DQ easier to close. A trial that is malformed, times out, or fails transport is recorded exactly as such and remains a completed observation, exactly as Unit 2's Implementation/Execution Plan §13 already established for its own campaign ("timeout, malformed output, wrong action, and transport failure remain completed observations").

## 10. Stop conditions

Translated exactly from Scope Lock §20, with no broadening or narrowing:

**Hard stop — halts the entire campaign immediately, writes `campaign.halted` with a reason code, and forbids further calls without a new Defect Confirmation Review:**
- any measurement/harness defect (capture failure, parser misbehaving outside its documented contract, evidence loss);
- any repository commit, model/digest, endpoint, timeout, or runtime-identity mismatch (including the Section 5 execution-time re-verification failing, or the Section 8 isolation-guard assertion failing);
- any artifact-integrity failure (hash mismatch, corrupted or duplicate record, failed append/flush);
- any unexpected consequential action — a parsed `Remember` or `Goal` reaching any live downstream execution path (Section 11) — a safety boundary, not a diagnostic outcome.

**Not a stop condition — recorded as data, campaign proceeds to the next scheduled cell:**
- a semantic failure (`D`) on any fixture, including all ten of DQ1's repeats coming back divergent from the expected action;
- an unexpected representation failure (malformed/untagged/timeout/transport) on a single trial.

**Why this differs from Unit 2's Stage 0 gate, restated precisely:** Unit 2's Stage 0 halts on the first non-A/B observation because it exists to protect entry into a 3,900-trial statistical commitment whose validity depends on a proven instrument — the cost of proceeding on a broken instrument there is large and the entire purpose of that gate is pass/fail screening before a large downstream commitment. Unit 2-D has no such downstream statistical commitment; its entire purpose is characterizing failure, including uniformly repeated failure, at a scale where continuing *is* the evidence. A semantic-failure auto-halt here would be self-defeating: it would terminate data collection on exactly the phenomenon DQ1 and DQ2 exist to observe. This does not weaken Unit 2's own historical fail-closed determination in any way — Unit 2's gate remains untouched, governing only Unit 2's own campaign; Unit 2-D defines its own, separately justified gate for a differently structured unit with different stakes.

## 11. Downstream isolation

The driver calls only `ModelReasoningProvider.reason(request)` and records the returned `ReasoningProviderResponse` — exactly the same, already-verified-isolated pattern Unit 1's harness and Unit 2's driver both use. It is a mandatory implementation requirement (verifiable by a source-scan test, Section 15) that Unit 2-D's new file contains no import of, or reference to, any of: `MemoryAdmissionCoordinator`, `DefaultKnowledgeSubmission`, `AuthorizationPurposeRegistry`, `ConversationReplyCoordinator`, `PlannerRuntime`, any tool registry or tool-invocation binding, or `ParkerRuntime`/any composition-root wiring. This was independently re-verified this session, by direct grep, to be true of the existing Unit 1 harness and Unit 2 driver (zero such imports found); Unit 2-D's new file must be held to the identical standard. No parsed `Remember` or `Goal` response can therefore reach Memory admission, Goal creation, Knowledge Submission, planning/execution, tools, or any owner-facing consequential action — the evaluation terminates at the recorded observation, exactly as it does today.

## 12. Interpretation worksheet

Defined now, before any observation exists, as a template the future implementation must produce and the future execution must populate — not populated here. It must include, for each of the six named traps, an explicit line stating the finding is bounded exactly as follows:

- **DQ1's ten repeats → prohibited from becoming a population failure rate.** Only "uniform" or "materially mixed" characterizations are permitted (Scope Lock §21 rules 1–2; Section 7's sampling-state limitation restated verbatim).
- **The one DQ4 Llama comparison → prohibited from becoming a model benchmark.** Only a directional, confound-disclosed signal is permitted (Scope Lock §21 rules 3–4).
- **DQ5 → prohibited from becoming a remedy recommendation.** Only a suggestive, Qwen-only coupling signal is permitted; the worksheet must explicitly record that no production-viability judgment was made (Scope Lock §21 rule 6, Remedy Firewall Section 16).
- **DQ3's context observations → prohibited from becoming causal proof.** Permitted only as weak signal, read jointly with DQ1 (Scope Lock §21 rule 7).
- **Representation validity → prohibited from being read as semantic correctness.** The worksheet must report the two axes as separate fields for every one of the 22 non-warm-up observations, never combined into one pass/fail (Sections 9, and Scope Lock §§12–14).
- **Diagnostic completion → prohibited from being read as production readiness.** The worksheet must state explicitly, regardless of results, that `campaign.sealed` means evidence collection completed with integrity, and nothing about deployment suitability.

## 13. Exit states

Outcome-neutral, named, and defined now:

- **`CAMPAIGN_COMPLETE_INTEGRITY_VERIFIED`** — all 24 scheduled calls completed, `campaign.sealed` present, every artifact hash-verified. This state is reached regardless of what DQ1–DQ6 actually show.
- **`DIAGNOSTICALLY COMPLETE — INCONCLUSIVE`** — a named sub-state of the above, used when the interpretation worksheet finds the collected evidence does not clearly support any single hypothesis for one or more DQs (for example, a DQ1 split near 5/10). This is an explicitly valid, successful completion state under Scope Lock §22's own text ("that insufficiency is itself a valid, recordable finding") — it does not block exit and does not require re-running or expanding the campaign.
- **`CAMPAIGN_HALTED_INSTRUMENT_DEFECT`** — a hard stop occurred (Section 10); requires its own Defect Confirmation Review before any correction or resumption is considered; not treated as diagnostic evidence about the model.

No exit state requires any particular substantive result. `CAMPAIGN_COMPLETE_INTEGRITY_VERIFIED` (with or without the `INCONCLUSIVE` qualifier) is success; only `CAMPAIGN_HALTED_INSTRUMENT_DEFECT` is not.

## 14. Verification workflow

1. **Deterministic pre-execution verification** — offline unit tests, no live call, proving: exact fixture text/actions/counts match Section 2; exact 24-call schedule and ordering match Section 3; deterministic, unique trial IDs; isolation-guard assertion (Section 8) fails closed against a simulated Unit-2-path collision; hard-stop triggers (Section 10) individually simulated and proven to halt; not-a-stop-condition cases individually simulated and proven to continue; prompt-capture-equals-expected assertions for both tracks (Section 6); resume/duplicate-ID rejection.
2. **Targeted implementation verification** — the new Unit 2-D test class run in isolation, against the offline suite above.
3. **Full repository verification** — ordinary `./gradlew test`/`check`/`build` remain detached and unaffected, exactly as Unit 1's and Unit 2's own task-isolation convention already requires; the new Gradle task is filtered to the new class alone and gated by its own JVM property.
4. **Pre-live readiness verification** — a dedicated Implementation Readiness Review, examining the actual implementation diff (not this Plan's prose), and its own Independent Constitutional Review, exactly mirroring Unit 2's gate sequence.
5. **Execution evidence verification** — after any live run, independent hash/size/line-count re-verification of every artifact against `manifest.txt` and `artifact-hash-inventory.txt`.
6. **Artifact-integrity verification** — confirmation that `campaign.sealed` or `campaign.halted` (never both) is present and consistent with the raw record count.
7. **Post-execution Independent Constitutional Review** — the final gate before any evidence is handed toward a Remedy Selection Review; examines the interpretation worksheet against Section 12's pre-registered rules for overclaiming.

## 15. Implementation decomposition

Exactly two new files — deliberately mirroring Unit 2's own Scope Lock precedent of authorizing exactly two files, proportionate to a smaller unit:

**Unit A — `build.gradle.kts`.**
- Files affected: `build.gradle.kts` only.
- Responsibility: one new detached Gradle task (for example `reasoningProtocolUnit2DDiagnostic`), using the existing `liveModelEvaluation` output/runtime classpath, JUnit Platform filtering to the single new test class, and a new JVM property (for example `parker.reasoning.diagnostic.enabled=true`) supplied only by that task. Must not attach to `test`, `check`, `build`, or `assemble`.
- Tests required: a build-level assertion (or an existing-pattern equivalent to Unit 2's own detachment tests) that ordinary lifecycle tasks do not include the new class and that the new task requires its own property.
- Constitutional boundary: exactly Scope Lock's own reuse-not-duplication principle; no other Gradle change (no new dependency, plugin, or source set).

**Unit B — `tests/integration/ReasoningProtocolDiagnosticCharacterisationTest.kt`** (new file; final name subject to the Implementation Readiness Review's own naming check against repository convention).
- Files affected: this one new file only. Zero changes to `tests/integration/ReasoningProtocolLiveModelEvaluationHarness.kt`, `tests/integration/ReasoningProtocolBaselineCharacterisationTest.kt`, or any `src/` file.
- Responsibility: the five-fixture corpus (Section 2, text reimplemented verbatim as reference material, not imported from Unit 2's private `BaselineCorpus`); the 24-cell schedule and deterministic IDs (Section 3); the DQ5 decision-only prompt template (Section 6); a reimplementation of Unit 2's proven identity-verification technique (Section 4), file-private to this new file; the campaign driver (intent/raw/checkpoint/manifest/`campaign.sealed`-or-`campaign.halted` state machine, Section 8); the isolation guard (Section 8); the stop-condition logic (Section 10); offline deterministic tests (Section 14, item 1); and the live entry point, gated by Unit A's JVM property.
- Tests required: everything enumerated under Section 14 item 1, plus a source-scan or equivalent architectural test proving the absence of any Memory/Goal/Planner/tool/composition import (Section 11).
- Constitutional boundary: imports only public production types (`DefaultReasoningPromptBuilder`, `ModelReasoningProvider`, `LocalHttpModelInferenceClient`, `defaultOllamaRequestBody`, `TaggedReasoningResponseParser`) and public Unit 1 harness types (`ConformanceFixture`, `ContextProfileId`, `SyntheticContextProfiles`, `TrialObservation`, `PrimaryClassification`, `ContentFidelity`, `EvaluationJsonLines`) unchanged; never imports from, and never modifies, any Unit 2-owned file.

A third, shared "identity verification utility" file was considered and rejected: extracting Unit 2's file-private identity technique into a new shared/public utility would itself be a third implementation file not authorized by anything in the frozen Scope Lock, and would expand implementation surface for no benefit over a small, file-private reimplementation inside Unit B. Minimal duplication of a already-proven, narrow technique is preferred here over creating new shared surface.

## 16. Remedy firewall

This unit selects, recommends, prototypes, or implements none of the following, restating Scope Lock §23 together with this task's explicit additions as one operative list:

structured/schema-constrained output; prompt rewriting (beyond the one firewalled, non-production DQ5 diagnostic variant defined in Section 6, which is never treated as a candidate for deployment); deterministic intent routing; retry; semantic repair; fallback; model replacement; a larger model; ensembling; confidence thresholds; parser relaxation; protocol weakening; temperature/seed changes; memory-policy changes; goal-policy changes; production reasoning changes.

All of these remain candidate categories for a future, separately governed Unit 3 only. Nothing in Sections 2–15 constitutes a recommendation of any of them, regardless of what any future execution under this Plan shows.

## 17. Execution authorization

```text
LIVE EXECUTION IS NOT AUTHORIZED BY THIS PLAN.
```

Before any implementation file is created: this Plan requires its own accepted Independent Constitutional Review (produced alongside this document; see the companion review). Before any live call: the completed implementation must undergo an Implementation Readiness Review and its own Independent Constitutional Review, conducted against the actual implementation diff, exactly as Unit 2's was. Explicit execution approval is required after all of the above, and only then. No step in this sequence may be skipped, combined, or inferred from this Plan's acceptance alone.

## 18. Disposition

```text
PROPOSED
```

Implementation and live execution remain prohibited pending this Plan's own Independent Constitutional Review and the full gate sequence in Section 17.
