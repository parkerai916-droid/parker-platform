**Status:** Independent Constitutional Review of the Unit 3-C Scored-Trial Timeout Semantics Determination — **ACCEPTED WITH QUALIFICATIONS.** No live model call, no HTTP call, no campaign, and no repository mutation beyond this document occurred.

# Unit 3-C Scored-Trial Timeout Semantics Determination — Independent Constitutional Review

## 1. Method

Independently re-read the Unit 3-A Reliability Contract Definition Scope Lock's own table row directly, before accepting the Determination's own quotation of it: row 35, "Timeout/transport measured as separate operational axes (H/I), never scored as semantic answers," marked **Frozen** (not "unresolved," unlike the adjacent row 36 governing the timeout *value*) — confirming this specific principle was already settled governance, not something the Determination invented to reach its own conclusion. Independently re-read Scope Lock §16 (existing measurement-invalidating/remedy-performance list), §19 (existing asymmetry-disclosure precedent), and the Timeout + Durability Plan Amendment's own state (C)/(D) definitions directly from source.

## 2. Is the timeout classification fair?

Yes, independently re-derived rather than accepted from the Determination's own reasoning: Candidate A (flat remedy-performance treatment) is correctly disqualified by already-frozen governance (Section 1 above), not by the Determination's own preference. The dual-purpose treatment (non-halting for stop-condition purposes, but never merged into semantic scoring for interpretive purposes) is independently confirmed internally consistent — it does not contradict Scope Lock §16's existing rule that remedy-performance failures "do not, by themselves, halt an arm," while also not contradicting Unit 3-A's own "never scored as semantic answers" instruction, because the Determination keeps the two effects (stop-condition behavior vs. scoring treatment) explicitly separate rather than conflating "doesn't halt" with "counts as remedy-performance evidence."

## 3. Is the infrastructure/model distinction real, or a distinction without a difference?

**Real, with one qualification this review specifically adds.** Independently re-confirmed the exception-type basis is genuine: Attempt 3's own preserved stack trace names `kotlinx.coroutines.TimeoutCancellationException` specifically, and this exception type is architecturally distinct from the JDK's own `IOException`/`ConnectException`/`HttpConnectTimeoutException` family, which fire from a different layer of `LocalHttpModelInferenceClient.infer`'s own `HttpClient.sendAsync` call. This is not a distinction invented for this document — it reflects a real difference in what the JVM/coroutines runtime actually reports.

**Qualification independently identified, not raised by the Determination itself:** a `TimeoutCancellationException` firing does not, by itself, *guarantee* the provider "remained reachable throughout" in the strongest sense — a degraded or partially-stalled connection could in principle also manifest as a client-side timeout rather than a hard connection failure, which the exception type alone cannot distinguish from genuine model-side slowness. This review judges the Determination's rule reliable specifically **because** Parker's own Unit 3-C endpoint is `http://127.0.0.1:11434` — a loopback address, not a routed network path — independently re-confirmed via the frozen `PARKER_REASONING_EVAL_ENDPOINT_URL` value used in every prior Approval Review in this programme. Classic network-degradation scenarios (partial packet loss, route flapping) are not realistic threats to a loopback connection between two processes on the same host, which is why this review accepts the rule as sound *for this deployment specifically*, not as a general-purpose transport-failure heuristic that would necessarily hold for a remote endpoint.

## 4. Does the continuation rule bias any arm?

No, independently re-checked: Section 5's own rule ("only that trial terminates; arm continues; campaign continues," for genuine model-side timeouts) is stated once, uniformly, with no arm-specific variant — independently confirmed by re-reading the section for any conditional language naming Control, Family A, or Family B differently. The "matched trial X" simultaneity correction (Determination §5) is independently verified against the frozen arm order: `Unit3COrchestrationDriver`'s own unchanged `UNIT_3C_ARM_ORDER` list guarantees Control's 148 calls complete before Family A's first call, which this review confirms directly resolves the task's own framing concern — there is no shared wall-clock moment across arms for a Control-arm timeout to unfairly "protect" or "expose" a later arm's own matched fixture.

## 5. Do retry semantics preserve exact-once?

Yes. Independently re-confirmed the Determination introduces no exception to the already-frozen "no automatic retry" rule for transmitted-timeout trials, and independently re-checked that this is consistent with, not merely repeated from, the Plan Amendment's own state (C) definition.

## 6. Is durability complete?

Yes, with the sub-case classification and continuation decision correctly identified as themselves durable facts, not transient state — independently re-verified this closes a real gap the Determination itself does not over-claim: without recording *which* sub-case (1–4) was assigned and *what* continuation decision resulted, a future reviewer inspecting only the campaign's own artifacts could not verify the governed rule was actually applied correctly to a given trial, only that some timeout occurred.

## 7. Is Unit 3-D comparability preserved?

Yes, and the survivorship-bias point (Determination §8, "comparing semantic accuracy among completed responses alone risks survivorship bias if timeout occurrence is not independent of prompt content") is independently judged a genuine, non-obvious, correctly-reasoned addition — not a restatement of anything already frozen elsewhere in this programme's governance. Independently checked that the required "completion rate and semantic correctness as two separate, explicitly paired figures" instruction does not itself perform any comparison or state which arm would fare better under it — confirmed neutral.

## 8. Is any remedy advantaged?

No. Independently searched the Determination for any per-family conditional treatment: none found. The dual-purpose classification, the continuation rule, the retry prohibition, the durability schema, and the comparability boundary are all stated in family-agnostic terms throughout.

## 9. Was any arbitrary threshold invented?

No, independently re-checked specifically for smuggled numbers: Section 9 explicitly declines to state a numeric concentration trigger and gives a stated reason (no baseline scored-trial timeout rate exists under the new 90,000 ms ceiling, since all 26 historical observations completed with zero timeouts). Independently confirmed the deferred-threshold pattern genuinely mirrors, rather than merely cites, the original Scope Lock §16's own established precedent for the adversarial false-positive checkpoint.

## 10. Can execution now proceed once implementation is updated?

**Governance-wise, yes — this review independently confirms the specification is now complete enough for an implementation task to proceed against**, covering: classification (§3), the four distinguishable sub-cases and their governed signals (§4), continuation semantics (§5), retry prohibition (§6), the durable record (§7), the Unit 3-D interpretation boundary (§8), and the stop/manual-review principle (§9, with its threshold correctly and explicitly deferred rather than blocking). This determination does not itself unblock *execution* — only a future implementation task, followed by this programme's own full Completion/Readiness/Approval chain, can do that, exactly as the Determination's own §11 states.

## 11. Blocking defects

None.

## 12. Non-blocking qualifications

1. The exception-type-based model/infrastructure distinction (Section 3 of this review) is sound specifically because Unit 3-C's endpoint is loopback-only; any future unit or amendment that changes the endpoint to a genuinely networked address must independently re-derive whether the same rule still holds, rather than assuming it travels unchanged.
2. The Section 9 stop-rule threshold remains genuinely undefined; a future implementation task must not quietly pick a number under time pressure without the same evidence-gathering discipline this entire timeout investigation has otherwise maintained.
3. As with the immediately preceding amendment's own qualifications, the STRONGLY SUPPORTED / NOT CONFIRMED cold-start confidence qualifier and the Family B latency-measurement gap remain outstanding and must continue to travel into any future document that builds on this one.

## 13. Verdict

```text
ACCEPTED WITH QUALIFICATIONS
```

The classification is independently confirmed fair and grounded in already-frozen Unit 3-A governance, not invented; the infrastructure/model distinction is real for this specific (loopback) deployment, with that dependency now made explicit; the continuation rule is independently confirmed unbiased across arms; retry semantics are independently confirmed exact-once-preserving; durability is independently confirmed complete for its stated purpose; Unit 3-D comparability is independently confirmed preserved, with a genuine additional survivorship-bias safeguard; no remedy is advantaged; no arbitrary threshold was invented; and the specification is independently judged complete enough for a future implementation task to proceed against, once that task is separately authorized.

## 14. Confirmation

No model or HTTP call occurred during this review. No campaign was created, resumed, or modified. No production, test, or Gradle file changed. No fixture or Family A/B/C definition was altered. No remedy was selected. The Determination document itself was not modified by this review.
