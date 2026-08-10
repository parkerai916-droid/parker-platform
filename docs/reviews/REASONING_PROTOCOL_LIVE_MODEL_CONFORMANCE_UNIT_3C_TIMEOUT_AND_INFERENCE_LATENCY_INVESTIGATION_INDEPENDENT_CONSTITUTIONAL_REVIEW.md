**Status:** Independent Constitutional Review of the Unit 3-C Timeout and Inference-Latency Investigation Review — **ACCEPTED WITH QUALIFICATIONS.** This review independently re-derives the latency data from raw artifacts a second, separate time (not merely trusting the Investigation Review's own extraction), independently checks for at least one alternative explanation the Investigation Review did not explicitly rule out, and specifically probes whether the recommended timeout value is genuinely data-derived. No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred.

# Unit 3-C Timeout and Inference-Latency Investigation — Independent Constitutional Review

## 1. Method

Independently re-extracted every `latencyNanos` value from the four preserved raw-evidence files using a fresh Python script, not the Investigation Review's own reported numbers, and independently cross-checked the resulting sorted list, count (N=26), minimum (0.95 s), and maximum (48.57 s) against the Investigation Review's Section 4 table before accepting any of its figures. Independently re-read `defaultOllamaRequestBody`, `LocalHttpModelInferenceClient.infer`, `Unit3CArmLedger`, `Unit3COrchestrationDriver.runWarmups`, and Unit 2-D's own `intentFile` handling directly from source, not from the Investigation Review's own description of them.

## 2. Was cold-start overclaimed?

**No overclaim found, and one alternative explanation independently checked and ruled unlikely.** The Investigation Review already withholds "CONFIRMED" in favor of "STRONGLY SUPPORTED" (Section 3), which this review judges the correct, calibrated level of confidence given the evidence. Independently probed one alternative the Investigation Review does not explicitly rule out: could Attempt 3's timeout instead reflect a malformed or degenerate request body causing Ollama to hang, independent of cold-start? Independently re-read `defaultOllamaRequestBody` (`ModelInferenceClient.kt:102-104`): it produces a simple, correctly-escaped `{"model":...,"prompt":...,"stream":false}` body, identical in shape to every prior successful Unit 2/Unit 2-D call, and Control's own executor is independently confirmed (via `Unit3CLiveEntryPoint.run`'s executor wiring, unaffected by any correction in this programme's history) to use the same `DefaultReasoningPromptBuilder` production prompt-building path Unit 2/2-D's own "production-track" already exercised. This independently rules out a request-shape defect as a competing explanation, strengthening rather than weakening the cold-start hypothesis by elimination — a finding the Investigation Review did not make but which is consistent with, and supports, its own conclusion.

## 3. Is the historical latency evidence genuinely comparable to Unit 3-C, or is this an apples-to-oranges argument?

**Comparable for Control specifically (which is what actually failed); more qualified for Family A/B.** Independently re-read the Unit 3-C Implementation Plan's own text (§ "The control uses the real, unmodified `DefaultReasoningPromptBuilder`... the same production classes Unit 2 and Unit 2-D's own production tracks already exercised, imported and invoked directly, never a hand-copied approximation of the prompt text"). Independently measured Unit 2-D's own production-track prompt length (1,381 characters for the sampled record) as a sanity reference. Since Control is the exact arm that actually failed in Attempt 3, and Control's own prompt-building path is independently confirmed to be the same code Unit 2/2-D's historical data already reflects, the comparability objection is weak for the specific failure this investigation examines. It is **more open** for Family A (decision/render split, shorter per-call prompts) and Family B (a longer candidate-selection prompt) — the Investigation Review's own Section 9 already flags this asymmetry for a different purpose (fairness); this review additionally flags it here as a comparability caveat the timeout-value recommendation should carry forward, since Family B specifically has never had any prompt of its own shape measured for latency at all, historically or in Attempt 3.

## 4. Is the proposed 90,000 ms genuinely data-derived, or convention dressed as analysis?

**Genuinely data-derived, independently re-verified by recomputing the margin ratios myself rather than accepting the Investigation Review's own arithmetic.** Independently computed: 90,000 / 48,568 = **1.853**; 90,000 / 65,396 = **1.376**; 90,000 / 27,990 = **3.215**. All three independently match the Investigation Review's own Section 6 figures. This review specifically checked whether "90,000 ms was historically used" is being used as circular justification (i.e., "it's right because it was used before," with no independent margin analysis) — it is not: the Investigation Review's own derivation explicitly computes the margin over worst-observed cold and warm latency and reports it as a ratio, which is the substance Phase 6 required, not merely an appeal to precedent. The genuine limitation — N=2 cold-start qwen observations — is stated plainly by the Investigation Review itself (Section 6's own "Confidence and limitation" paragraph) rather than concealed, which this review credits directly.

## 5. Does the recommendation preserve fairness, or could it introduce a new asymmetry?

Independently re-derived the arm-order argument (Section 9) from the frozen `UNIT_3C_ARM_ORDER` constant (`CONTROL, FAMILY_A, FAMILY_B, FAMILY_C`, unchanged by any correction in this programme's history) rather than accepting it as stated: confirmed that Control's own 148 calls (3 warm-up + 145 scored) necessarily precede Family A's first call in every execution of `Unit3COrchestrationDriver.run`, independently re-read. This correctly localizes cold-start risk to Control's own warm-up specifically, as the Investigation Review claims. One additional adversarial check performed: does anything in the driver *guarantee* no long gap occurs between arms (which could allow Ollama's keep-alive to expire and re-introduce cold-start risk for Family A/B/C)? Independently confirmed: no such guarantee exists in the current implementation — `runArm` proceeds immediately from one family to the next with no artificial delay, but nothing prevents a *safety-checkpoint halt* or an *operator-side pause* between arms from reintroducing exactly this risk asymmetrically. The Investigation Review's own Section 9 already flags this as "evidence this programme has no current evidence about one way or the other" — independently judged an honest, correctly-hedged statement, not an overclaim.

## 6. Is timeout evidence being confused with transport failure?

**Not confused; the distinction is independently confirmed sound.** Independently re-read Unit 3-C Scope Lock §8's own measurement-invalidating/remedy-performance list a second time, character by character: neither list contains any transport/timeout term. The Investigation Review's Section 7 conclusion — that the frozen distinction simply does not address this case, requiring new reasoning rather than forced categorization — is independently confirmed accurate, not an artifact of selective quotation.

## 7. Are the durability requirements consistent with exact-once semantics?

Independently re-read Unit 2-D's own `intentFile` handling (`ReasoningProtocolDiagnosticCharacterisationTest.kt:971-976`) directly: on a repeat encounter, it *requires* the new intent text to match the previously recorded one (`require(Files.readString(intentFile) == text) { "intent record mismatch" }`) but does not itself gate whether a trial is re-attempted — that remains governed by the raw/checkpoint recovery logic, independently confirmed unchanged and un-referenced by the intent file at all. This independently confirms the Investigation Review's implicit claim (Section 8) that adopting an equivalent intent-before-call record for Unit 3-C would be **additive**, not a replacement for or interference with `Unit3CArmLedger`'s own existing exact-once duplicate-prevention mechanism.

## 8. Does the recommendation change experimental design?

Independently re-read the Investigation Review's Section 11 recommendation in full: it proposes a timeout-value change, a durability addition, and a scoped semantics resolution — none of which touches fixture text, expected actions, repetition counts, the model, the endpoint, or any Family A/B/C mechanism. Independently confirmed via a search of the Investigation Review's own document for any of the five terms the task's own prohibitions name (fixture, repetition, model, mechanism, prompt-content change): none appears in a design-altering context. No experimental-design creep found.

## 9. Is a further governed characterization required before amendment, or does the existing record already suffice?

**Qualification, not rejection.** Independently re-assessed the N=2 cold-start qwen sample size a second time: two independent, real observations, from two different campaigns, both well under 90,000 ms, is judged sufficient to support *proposing* 90,000 ms as a Scope Lock amendment candidate — but this review specifically recommends the eventual amendment task explicitly carry the same stated confidence level ("STRONGLY SUPPORTED, not CONFIRMED") into its own governance record, rather than silently upgrading it to "CONFIRMED" once it is repeated in a new document. This is the basis for this review's own "ACCEPTED WITH QUALIFICATIONS" verdict rather than unqualified "ACCEPTED."

## 10. Blocking defects

None.

## 11. Non-blocking qualifications

1. The confidence level attached to the 90,000 ms recommendation (STRONGLY SUPPORTED, N=2 cold-start observations) must be carried forward verbatim into any future amendment task, not silently strengthened.
2. Family B's own prompt shape has never been latency-measured, historically or in Attempt 3; any future amendment should note this gap explicitly rather than assume Family B's latency profile matches Control's.
3. The scored-trial timeout-semantics question (Investigation Review §7) remains genuinely open; a future durability fix must not resolve it by default/omission (e.g., by silently treating all timeouts as measurement-invalidating without a stated reason) merely because that is simpler to implement.

## 12. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

The Investigation Review's evidence extraction is independently reproduced and confirmed accurate; its cold-start classification is independently confirmed appropriately hedged and strengthened by one additional ruled-out alternative (malformed request body); its 90,000 ms recommendation is independently confirmed genuinely margin-derived rather than conventional; its durability finding is independently confirmed consistent with exact-once semantics; and its recommendation is independently confirmed not to alter any frozen experimental property. The qualifications above (Section 11) must travel with this Investigation Review into any future governance task that acts on it.

## 13. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production, test, or Gradle file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Investigation Review document itself was not modified by this review.
