**Status:** Unit 2-D Diagnostic Characterisation Execution Evidence Review — **CAMPAIGN SEALED. EVIDENCE CAPTURED.** The single authorized live campaign executed exactly once and completed successfully. This document records the resulting evidence only; it performs no interpretation, remedy selection, or Completion Review. No remedy is selected. Unit 2 remains untouched. Nothing is staged, committed, or pushed.

# Unit 2-D Diagnostic Characterisation — Execution Evidence Review

## 1. Exact execution identity

```text
Campaign ID:        qwen25coder7b-llama32-3b-diagnostic-20260809
Repository commit:  137e1db5ece8cc09d7b36460a7789607962c0d13
                     (implementation verified byte-identical to authorized
                     baseline 705f9f5cf760fe8d9633ecd95b30fd72a37074ba)
Executed via:        ./gradlew reasoningProtocolUnit2DDiagnostic --rerun-tasks
Live test method:    live Unit 2-D diagnostic campaign skips before definition
                     client or configuration construction unless explicit
                     property is enabled()
Execution wall time: 328.101 seconds (~5.5 minutes), reported by JUnit
Gradle task result:  the wrapping :reasoningProtocolUnit2DDiagnostic task
                     reported FAILED overall (one unrelated, expected offline
                     test failed as direct collateral of setting the live
                     environment -- Section 3); the live entry-point test
                     itself, and the campaign it drove, completed with no
                     failure or error.
```

## 2. Exact authorization chain

Scope Lock (`d316032`) → Scope Lock Independent Constitutional Review → Implementation/Execution Plan (`a859ba5`) → Plan Independent Constitutional Review → Plan Correction Independent Constitutional Review → Implementation (`705f9f5`) → Completion Review → Completion Independent Constitutional Review → Implementation Readiness Review → Readiness Independent Constitutional Review → Explicit Execution Approval Review (`137e1db`) → this execution. Every gate the frozen Plan's Section 17 required was satisfied before this campaign ran; no step was skipped or reordered.

## 3. Pre-execution checks (all independently re-verified this session, not reused from the approval review)

```text
REPOSITORY:                    PASS (HEAD=137e1db=origin/main; zero drift vs 705f9f5)
AUTHORIZATION:                 PASS (full chain above, all accepted)
UNIT 2 ARTIFACT INTEGRITY:     PASS (7/7 hashes unchanged)
UNIT 2-D CAMPAIGN DIR ABSENT:  PASS (confirmed before first call)
QWEN IDENTITY:                 PASS (dae161e2...4364, unchanged since Unit 2's own run)
LLAMA IDENTITY:                PASS (a80c4f17...8b72)
OLLAMA RUNTIME IDENTITY:       PASS (container f795e46c5eac, ollama/ollama:latest)
ARTIFACT ROOT:                 PASS (steve:steve, mode 700)
DISK SPACE:                    PASS (3.68 GiB avail vs 2 GiB required)
EXECUTION CONFIGURATION:       PASS (all 15 values resolved from source, none guessed)
24-CALL SCHEDULE:              PASS (compiled-in invariant, 27/28 offline tests passing)
```

One operational note, recorded for the record: the first invocation of `./gradlew reasoningProtocolUnit2DDiagnostic` (without `--rerun-tasks`) returned `UP-TO-DATE` in under a second and made **zero** live calls, because Gradle's task-caching does not treat environment variables as declared inputs to the `Test` task and reused a prior cached result. This was detected immediately (no campaign directory appeared), corrected by re-invoking with `--rerun-tasks`, and is recorded here as an execution-mechanics observation, not a defect in the diagnostic implementation itself.

## 4. Exact invocation

```text
./gradlew reasoningProtocolUnit2DDiagnostic --rerun-tasks --console=plain
```

with the complete environment configuration set out in the Explicit Execution Approval Review's Section 17 (re-verified fresh this session; identical values), except `PARKER_REASONING_EVAL_REPOSITORY_COMMIT`, set to the true current HEAD `137e1db5ece8cc09d7b36460a7789607962c0d13` rather than the review's own citation of `705f9f5` (written before `137e1db` existed) -- justified because the field records the actual commit under which execution occurred, and Phase 1 independently confirmed the two commits are implementation-identical. Run once. Not rerun after completion.

## 5. Campaign artifact inventory

```text
qwen25coder7b-llama32-3b-diagnostic-20260809/
  campaign.lock                    0 bytes
  campaign-definition.txt          3138 bytes, 28 lines
  campaign-identity.txt            204 bytes, 3 lines
  intent.jsonl                     4610 bytes, 24 lines
  manifest.txt                     404 bytes, 13 lines
  campaign.sealed                  65 bytes, 1 line   (campaign.halted: absent)
  artifact-hash-inventory.txt      1038 bytes, 11 lines
  warmup/raw.jsonl                 12194 bytes, 2 lines
  warmup/checkpoint.txt            232 bytes, 2 lines
  production-track/raw.jsonl       107306 bytes, 17 lines
  production-track/checkpoint.txt  1625 bytes, 17 lines
  candidate-track/raw.jsonl        32689 bytes, 5 lines
  candidate-track/checkpoint.txt   545 bytes, 5 lines
```

## 6. Artifact integrity hashes (independently recomputed, matched against `manifest.txt`'s own recorded values)

```text
warmup/raw.jsonl            sha256=d542f0ede35fffb51c3907452afac7c2f2cbfd6533fdad0def34be7c640d5c3d
production-track/raw.jsonl  sha256=568f08f2421dd5a63e949268d2519cae3e0635abc713d437352e141636bcfde7
candidate-track/raw.jsonl   sha256=c12de361ffec2e39ce4e10b2ee0398cadd1b0ea15e30f5ae10dc41e5b789677f
campaign-definition.txt     sha256=c0b65875b45c8710386ea46f2ff00cc7752a151967638e9cd8795df7371a65a2
campaign.sealed content     sha256=bdda3995e06933ddc7b0c6ce050db093b01be6fd04739454447841dde612d057
manifest.txt manifestHash   3faa6883559df115d8f5092ef1d4f990a5e81421c05552ab1e21aea4fea830d2
campaign-identity fingerprint  79b3415a68d5ab26f6ae8ed3620597939f87128647289380ba56688b0d7f824d
```

All three raw-file hashes independently recomputed via `sha256sum` match `manifest.txt`'s own recorded values exactly, byte-for-byte, size-for-size, line-for-line. `campaign.sealed` present; `campaign.halted` absent -- the campaign terminated in the SEALED state, not HALTED.

## 7. Exact call accounting

```text
24 raw observation records total = 2 (warmup) + 17 (production-track) + 5 (candidate-track)
24 intent records (pre-registered before the first call)
24 checkpoint entries (2 + 17 + 5, matching each track's raw file exactly)
```

24 real model-inference calls were made. Zero more, zero fewer. No retry, no duplicate trial ID, no unknown trial ID anywhere in any ledger.

## 8. DQ1 observations (repeatability -- `r01-direct`, `minimal-production-context`, Qwen, 10 attempts)

| # | Actual | Class | Extracted (truncated) |
|---|---|---|---|
| 01 | REPLY | D | "Hello! How can I assist you today?" |
| 02 | REPLY | D | "Thank you for letting me know! I'll keep that in mind..." |
| 03 | REPLY | D | "Hello! How can I assist you today?" |
| 04 | REPLY | D | "I understand that your synthetic test coffee mug is black..." |
| 05 | REPLY | D | "Hello! How can I assist you today? Is there anything..." |
| 06 | REPLY | D | "I remember that your synthetic test coffee mug is black..." |
| 07 | NOACTION | D | "NOACTION" |
| 08 | REPLY | D | "I remember that your synthetic test coffee mug is black..." |
| 09 | REPLY | D | "Hello! How can I assist you today?" |
| 10 | REPLY | D | "Hello! How can I assist you today?" |

10/10 diverge from the expected `REMEMBER`. Not perfectly uniform in output: 9/10 chose `REPLY`, 1/10 (attempt 07) chose `NOACTION`. All 10 have `representationValid = true`. Latencies ranged 0.95s–17.87s (well under the 90s timeout; no timeout or transport failure).

## 9. DQ2 observations (action-class specificity -- minimal context, Qwen, one attempt each)

| Fixture | Expected | Actual | Class | Note |
|---|---|---|---|---|
| `p01-ordinary-fact` | REPLY | REPLY | A | correct, exact/NA fidelity |
| `p06-greeting` | REPLY | REPLY | A | correct, exact/NA fidelity |
| `g01-multistep` | GOAL | REPLY | D | wrong action |
| `n01-heartbeat` | NOACTION | *(none)* | **E** | extracted `"NORESPONSE"` -- an untagged, non-conforming token; `representationValid = false`; parser threw `UnclassifiableModelResponseException` |

## 10. DQ3 observations (context sensitivity -- `r01-direct`, Qwen)

| Profile | Actual | Class | Extracted (truncated) |
|---|---|---|---|
| `mixed-full-production-like` | REPLY | D | "Acknowledged. The synthetic test coffee mug is black. Is there anything else you'd like me to remember?" |
| `conversation-history` | REPLY | D | "Acknowledged." |

Both diverge from `REMEMBER`, identically to the DQ1 minimal-context baseline's majority outcome.

## 11. DQ4 observations (model specificity -- `r01-direct`, minimal context, Llama)

```text
Expected: REMEMBER   Actual: REPLY   Class: D
Extracted: "REPLY: How's your synthetic test coffee mug faring?"
```

Diverges from `REMEMBER`, the same direction as every Qwen observation of the identical fixture.

## 12. DQ5 observations (decision/rendering coupling -- `r01-direct-decision-only`, Qwen, 5 attempts)

| # | Actual | Class | Extracted |
|---|---|---|---|
| 01 | NOACTION | D | `NOACTION` |
| 02 | **REMEMBER** | B | `REMEMBER: black coffee mug` |
| 03 | **REMEMBER** | B | `REMEMBER: SELECTED` |
| 04 | REPLY | D | `REPLY: SELECTED` |
| 05 | REPLY | D | "Thank you for letting me know about your synthetic test coffee mug. Is there anything specific I can help with regarding it?" |

2/5 attempts correctly selected `REMEMBER` (both classified `B`, not `A`, exactly as pre-registered: content fidelity is structurally never `EXACT` under the fixed-placeholder design, so a correct DQ5 selection cannot reach classification `A`). Attempt 03 followed the `SELECTED`-placeholder instruction exactly; attempt 02 selected the correct action but substituted its own brief content (`"black coffee mug"`) rather than the literal placeholder; attempt 04 followed the placeholder format but chose the wrong action; attempt 05 ignored the "no explanation" instruction entirely while still choosing the wrong action. All five parsed without a representation failure.

## 13. DQ6 derived observations (representation independence -- cross-cutting, zero additional calls)

Across all 24 observations: `representationValid = true` for 22, `false` for 2 (the DQ2 `n01-heartbeat` untagged-prose case, classification `E`; and the Llama warm-up case, classification `G`, `extractedResponse = "REPLY:"` -- a blank-content tag that failed `Reply`'s own non-blank-text constructor validation, correctly caught and classified without any parser modification). Semantic-only misses (`D`, 17 of them) occurred with representation fully valid throughout. The two representation failures and the seventeen semantic-only failures did not co-occur on the same trials -- representation validity and semantic correctness varied independently across this campaign's own data, consistent with, and now replicating beyond the single PF01 instance, the separation Unit 2's PF01 first established.

## 14. Representation/parser/transport findings

Zero timeouts (`H`: 0/24). Zero transport/model failures (`I`: 0/24). Two representation-layer events, both handled by the unmodified parser and domain types exactly as designed and offline-tested: an `UnclassifiableModelResponseException` (`n01-heartbeat`'s `"NORESPONSE"`) and an `IllegalArgumentException` from `Reply`'s own `require(text.isNotBlank())` (the Llama warm-up's bare `"REPLY:"`), both correctly routed through `classifyRejected` to classifications `E` and `G` respectively with no crash, no silent data loss, and no special-casing added anywhere. No parser or domain-type modification occurred at any point (confirmed by the unchanged `src/**` diff, re-verified after execution). No harness or infrastructure defect occurred.

## 15. What the evidence establishes

Fully supported by this campaign's own data: under the pinned `qwen2.5-coder:7b` configuration, commit, and minimal-context prompt, the model selected `REPLY` (occasionally `NOACTION`) instead of `REMEMBER` for the exact `R01-direct` instruction in 10 of 10 repeated, independent attempts -- a near-uniform, not merely single-instance, divergence, confirming and substantially strengthening PF01's original finding rather than merely repeating it once. The same divergence occurred under two additional, richer context profiles (DQ3) and under `llama3.2:3b` on the identical fixture (DQ4) -- the miss is not confined to minimal context and is not unique to Qwen. Within the frozen action-family breadth check (DQ2), the miss is not universal: ordinary REPLY-expected fixtures were both handled correctly, while the one GOAL-expected fixture was also missed (also toward REPLY) and the one NOACTION-expected fixture produced a representation failure rather than a semantic one. Under the decision-only candidate variant (DQ5), correct `REMEMBER` selection rose to 2 of 5 attempts, versus 0 of 10 under the joint production task on the same underlying fixture -- a real, evidence-grounded (though small-sample) signal that decision/rendering coupling is a contributing factor, not merely a hypothesis. Representation reliability and semantic reliability are confirmed, independently of each other, across a broader dataset than PF01 alone provided.

## 16. What the evidence does NOT establish

No population-level failure probability for `REMEMBER`, `GOAL`, `REPLY`, or `NOACTION` in general -- ten repeats, or one or two attempts per other cell, is triage evidence, not a statistically powered rate. No claim that Qwen is generally unsuitable, nor that Llama is "better" -- Llama was observed on exactly one attempt of one fixture, confounded by both size and specialization versus Qwen, and it also missed. No claim that the DQ5 decision-only format is a viable production design, that content fidelity as measured for DQ5 means anything (it is structurally non-exact by design), or that any specific remedy -- structured output, prompt rewriting, retry, model replacement, or any other -- is proven, disproven, or even evaluated by this campaign. No claim about production readiness of any kind. No claim about why the DQ3 context variants failed to help (association is not causation, and each was a single attempt). No causal explanation for the two representation-layer misses beyond what is directly recorded.

## 17. Sufficiency for a later Completion/Interpretation Review

All six frozen diagnostic questions now have recorded evidence: DQ1 (10/10 divergent, 9-REPLY/1-NOACTION split), DQ2 (2 correct, 1 wrong, 1 representation failure across the three non-REMEMBER families), DQ3 (2/2 divergent under richer context), DQ4 (1/1 divergent, cross-model), DQ5 (2/5 correct under the decision-only variant), DQ6 (representation and semantic failures confirmed non-co-occurring across 24 real observations). Per the frozen Scope Lock's own exit criteria (completeness and integrity of the pre-registered evidence set, not any particular outcome), this campaign is **diagnostically complete** -- not inconclusive; every cell produced a usable, integrity-verified observation. Whether this evidence is *sufficient to select among remedy families* is an interpretive judgment reserved for the later Completion/Interpretation Review this task does not perform.

## 18. Confirmation: no remedy selected

No production code, harness, parser, or prompt was modified during or after execution. No recommendation of structured output, prompt rewriting, retry, model replacement, or any other remedy family is made anywhere in this document, notwithstanding the genuinely suggestive DQ5 and DQ4 signals recorded in Section 15 -- those are reported as evidence, not adopted as conclusions.

## 19. Confirmation: Unit 2 remained untouched

All seven frozen Unit 2 artifacts independently re-hashed after execution, byte-for-byte identical to every check made throughout this entire programme: `campaign-definition.txt` (`64ce538c...`), `campaign-identity.txt` (`c2f7f56a...`), `stage-0.failed` (`8bb87b7a...`, content `PF01:D`), `intent.jsonl` (`325ca44e...`), `raw.jsonl` (`c635ebcd...`), `checkpoint.txt` (`1cdd4644...`), `manifest.txt` (`4e63cb8a...`). `stage-0.sealed` remains absent. No file under `qwen25coder7b-baseline-20260809/` bears a modification time newer than this task's start. Unit 2-D's campaign lives entirely in a sibling directory and never read Unit 2's state as writable or resumable.

## 20. Repository state

No production, test, or Gradle file changed during this task (Phase 1 confirmed zero drift before execution; nothing was edited during or after). The only new artifact this task produces is this document itself.
