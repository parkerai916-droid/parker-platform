**Status:** Unit 3-C Controlled Remedy Experiments — Scored-Trial Timeout Semantics Determination — **PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW.** This document resolves the single question the Timeout + Durability Scope Lock Amendment deliberately left open. It contains no implementation, authorizes no live model call, ranks no remedy, and does not reopen Unit 3-D or Unit 4 authority.

# Unit 3-C Controlled Remedy Experiments — Scored-Trial Timeout Semantics Determination

## 1. Baseline and authority

Drafted against committed baseline `0072c01fb773830a7bd951fe6c83ec68a8f36a36`. Amends the Timeout + Durability Scope Lock Amendment §3 (which left scored-trial timeout classification explicitly, bindingly unresolved) and extends the Timeout + Durability Implementation Plan Amendment §5, without altering either document's own text directly — this determination is a further, separate, dated amendment layered on top, exactly as those two documents were themselves layered on top of the original frozen Scope Lock and Plan. Controlling authority: Unit 3-A Reliability Contract Definition Scope Lock (timeout/transport as a distinct operational axis, never scored as a semantic answer); Unit 3-C Scope Lock §16 (the existing measurement-invalidating/remedy-performance distinction, unamended); Unit 3-C Scope Lock §19 (the existing precedent for reporting asymmetries "transparently, never hidden or normalized away" rather than forcing them into a single bucket); the Timeout and Inference Latency Investigation Review and its Independent Constitutional Review.

## 2. The question, precisely bounded

**Only:** what should happen when a measured (scored) Control, Family A, or Family B live model call reaches the governed 90,000 ms timeout? Warm-up timeout semantics (already frozen: measurement-invalidating) are not revisited. Family C (zero model calls) is not addressed. The 90,000 ms value itself is not changed.

## 3. Classification

```text
E — a governed, specifically-reasoned category: dual-purpose treatment, not a single bucket
```

Not A (flatly "remedy-performance evidence," scored identically to semantic correctness), because Unit 3-A's own frozen instruction — independently re-read for this task — states timeout/transport must be **"measured as separate operational axes (H/I), never scored as semantic answers."** Treating a scored-trial timeout as remedy-performance evidence in the same sense as a wrong action or false positive would violate that already-frozen rule; Candidate A is disqualified by existing governance, not merely disfavored.

Not a flat B or C either, because the underlying causes a scored-trial timeout can have are not uniform (Section 4 below) — some genuinely reflect the model/environment being measured, others reflect the measurement apparatus itself failing, and collapsing both into one bucket would either wrongly halt the campaign over ordinary model latency or wrongly launder a real infrastructure failure into comparative evidence.

**The governed classification:** a scored-trial timeout is **its own governed evidence axis** — operational reliability, structurally parallel to, but never merged with, remedy-performance evidence:

- **For stop-condition purposes**, a genuine model-side scored-trial timeout (Section 4, sub-case 1) does **not**, by itself, halt the arm or the campaign — matching how remedy-performance failures already do not halt an arm, because a slow-but-otherwise-reachable model is not evidence the measurement apparatus is broken.
- **For interpretive/comparison purposes**, it is **never** scored alongside, substituted for, or averaged into semantic-correctness, representation-validity, or content-fidelity measures — it is reported as its own distinct metric (completion rate / timeout rate), exactly matching Unit 3-A's own H/I axis framing.
- Sub-cases 2–4 (Section 4) remain governed by the **already-frozen measurement-invalidating category**, unamended — this determination does not create a new stop-condition category for those cases, only applies the existing one with an explicit trigger condition for scored-trial calls specifically.

## 4. Model timeout vs. infrastructure failure

**These four classes must be distinguished, not collapsed, and a re-derivable rule for distinguishing them is frozen here** — grounded in a real, already-observed signal (Attempt 3's own recorded exception type), not invented:

1. **Request transmitted, provider remained reachable, response exceeded the 90-second ceiling.** Governed signal: the call fails with `kotlinx.coroutines.TimeoutCancellationException` — the coroutine-level exception `withTimeout` throws when the client's own budget is exhausted while still awaiting a response, independently confirmed (via Attempt 3's own preserved stack trace) to be thrown only after the HTTP request was successfully sent and a response was being awaited, not before. **This is the operational-reliability axis case (Section 3).**
2. **Transport/network failure.** Governed signal: the call fails with an `IOException`/`java.net.ConnectException`/`java.net.http.HttpConnectTimeoutException`-family exception, thrown by the underlying HTTP client independent of, and typically well before, the governed 90,000 ms budget — indicating the request was never delivered or the connection itself failed. **Measurement-invalidating** (existing category, "harness defect"/"call-accounting ambiguity" in spirit); halts the affected arm, per the existing precedent for every other measurement-invalidating cause (identity mismatch, artifact-integrity violation).
3. **Provider process unavailable/crashed.** Governed signal: connection refused, or no listener on the configured endpoint at all. **Measurement-invalidating**, same treatment as (2). No special campaign-wide pre-check is frozen here — an arm-level halt on the first such failure, followed by the same failure naturally recurring (and halting) on the next arm's own first call if the provider remains down, is judged sufficient without inventing a separate health-check mechanism.
4. **Ambiguous terminal state** (e.g., a crash before either a raw observation or a complete terminal-timeout observation was durably written). Governed signal: absence of a clear, classifiable exception record. **Fails closed**, exactly matching the already-frozen state (D) from the Timeout + Durability Plan Amendment §4 — never silently treated as either (1) or (2)/(3); halts the affected arm pending manual investigation.

These four classes are **not** collapsed merely because they can all surface, at the client's own vantage point, as "a call did not complete" — the exception type actually thrown is the frozen, re-derivable distinguishing signal, independently confirmed available today (Attempt 3 itself already produced case (1)'s own signal) without requiring any new capability.

## 5. Continuation semantics

For a genuine model-side scored-trial timeout (Section 4, case 1) **only that specific trial terminates**, durably recorded as `TIMEOUT`; **the arm continues** to its next registered trial; **the campaign continues** to subsequent arms. Reasoned from fairness, not convenience:

- **"If Control trial X times out, is it fair for Family A/B's own matched trial X to continue?"** Yes — and the premise requires correction: "matched trial X" means the *same fixture*, not the same wall-clock moment. Given the frozen arm order (`CONTROL → FAMILY_A → FAMILY_B → FAMILY_C`, unamended), Family A's own matched-fixture call for the same fixture ID occurs only after all 148 of Control's own calls complete — under different, later, and (per the Investigation Review's own fairness analysis) almost certainly warmer conditions. There is no shared simultaneity to preserve by halting Family A/B in sympathy with a Control-arm event that has already fully resolved (recorded, not repeated) before Family A's own first call is ever issued.
- **"If Family B trial X times out but Control/A completed, should later B trials continue?"** Yes. Halting Family B's own arm over a single isolated, genuine model-side timeout would leave Family B's own dataset artificially truncated relative to Control and Family A, which is a *worse* fairness outcome than recording the one timeout and letting Family B complete its own full, matched 115-trial schedule. A single isolated timeout is not, by itself, evidence that Family B's own remedy mechanism is broken.

For sub-cases 2–4 (measurement-invalidating or ambiguous), continuation semantics are unchanged from the already-frozen general rule: the affected **arm** halts (not the whole campaign, unless the same failure recurs at the next arm's own first call, in which case that arm halts too, by the same rule, without any special cross-arm coordination logic being frozen here).

## 6. Retry semantics

```text
NO AUTOMATIC RETRY
```

Frozen, for the same reasons already established for warm-up timeouts and already frozen in general form by the Timeout + Durability Plan Amendment's own state (C) ("transmitted and timed out... must not be automatically repeated"): the request may have been received and processed by the model even though no response reached the client; an automatic retry would create unequal exposure (a trial that happened to time out once would receive two attempts against the same model state, while every other trial receives exactly one); and exact-once semantics (already amended) exist specifically to make "transmitted timeout" durably distinguishable from "never transmitted," which a silent retry would erase in practice even if the durable record technically still distinguished them. **No exception to no-automatic-retry is created by this determination.** Any future, separately-governed retry policy would require its own explicit justification and its own fresh governance act — not an inference from this document.

## 7. Durable record

The scored-trial timeout observation reuses the already-frozen terminal-timeout-observation schema (Timeout + Durability Plan Amendment §3) in full, with the classification-and-continuation decision now made explicit as part of that same durable record:

At minimum, preserved for every scored-trial timeout: campaign ID; arm; fixture; trial/repetition (`trialSequence`); call ID; expected action; call-start timestamp; elapsed duration; timeout ceiling (`90000`); terminal classification (`TIMEOUT`, plus the Section 4 sub-case: `MODEL_TIMEOUT` / `TRANSPORT_FAILURE` / `PROVIDER_UNAVAILABLE` / `AMBIGUOUS`); model identity and digest; provider/runtime identity; transport-state classification (the exact exception type observed); response-bytes-received (yes/no); `parserResult` and semantic classification **always null**, never fabricated as `GOAL`/`REPLY`/`REMEMBER`/`NOACTION`; the continuation decision actually taken (`ARM_CONTINUED` / `ARM_HALTED`); and checkpoint state. No field here replaces or narrows the prior amendment's own schema — this section only makes explicit that the sub-case classification (Section 4) and the resulting continuation decision (Section 5) are themselves durable, recorded facts, not transient in-memory-only decisions.

## 8. Comparative fairness and Unit 3-D interpretation boundary

Frozen, for a future Unit 3-D task to inherit, not for this document to perform:

- **Is timeout rate itself a comparable reliability metric?** Yes — as its own separate operational metric (completion rate / model-side timeout rate per arm), never merged into or averaged with semantic-correctness scoring, per Section 3's own dual-purpose treatment.
- **Must matched cells be compared symmetrically?** Yes — same fixture, same context profile, same repetition count across arms; any arm's own timeout-affected cells must be reported with their own dedicated visibility, exactly matching the already-frozen Scope Lock §19 precedent for false-positive results ("reported with their own dedicated visibility, never omitted") and for the Family A call-count asymmetry ("must be reported transparently, never hidden or normalized away").
- **Can an arm with more timeouts still be compared on semantic accuracy among completed responses?** Only with an explicit, attached caveat — comparing semantic accuracy among completed responses alone risks survivorship bias if timeout occurrence is not independent of prompt content (e.g., if a remedy's longer or more complex outputs are both more likely to time out and, among those that do complete, different in accuracy from the ones that time out). **Unit 3-D must report completion rate and semantic correctness as two separate, explicitly paired figures, never one presented without the other.**
- **Are infrastructure failures excluded from remedy-performance comparison?** Yes — Section 4's sub-cases 2–4 are measurement-invalidating and must be excluded entirely from any remedy-performance-relevant tally; only sub-case 1 (genuine model-side timeout) enters the separate operational/completion-rate metric.

This section defines the interpretation *boundary* Unit 3-D must respect; it does not perform any comparison, and no remedy family is ranked, preferred, or evaluated by this document.

## 9. Campaign stop / manual-review rule for concentrated scored-trial timeouts

**No non-arbitrary numeric or frequency-based trigger can currently be defined, and this is stated rather than invented one to fill the gap.** Reasoned: every historical qwen latency observation this program possesses (the 26-observation record underlying the 90,000 ms figure itself) comes from a world where the ceiling was already 90,000 ms and zero timeouts occurred — there is no existing baseline "expected" scored-trial timeout rate under the new ceiling to judge a "concentrated pattern" against, and Section 4's own sub-case classification means only some observed timeouts (sub-case 1) would even be candidates for such a trigger in the first place.

**Frozen qualitative principle, mirroring the already-accepted precedent for the existing non-numeric safety checkpoint (Scope Lock §16):** a governed manual-review checkpoint is warranted if genuine, sub-case-1 scored-trial timeouts become frequent enough, within or across arms, to suggest the 90,000 ms ceiling itself may be systematically inadequate — as distinct from an isolated, rare event consistent with ordinary tail latency. **The exact concentration threshold that triggers this checkpoint is not invented here** and must be defined by a future, evidence-informed Plan-level decision (most plausibly once a small number of real scored-trial timeouts, if any, are actually observed under the amended 90,000 ms ceiling, providing the baseline this determination currently lacks) — exactly the same deferral pattern the original Scope Lock already used successfully for the adversarial false-positive checkpoint's own threshold.

## 10. Experimental invariance — independently verified unchanged

This determination changes none of: the 90,000 ms timeout value; the 483-call schedule; any fixture; any expected action; the repetition schedule; the model (`qwen2.5-coder:7b` only) or provider; the Family A decision/render mechanism; the Family B candidate prompt or its SHA-256; the Family C deterministic mechanism or its corrected trace; the artifact root; the 2 GiB disk-space threshold; the existing adversarial-category safety checkpoint (Scope Lock §16, untouched — this determination's own Section 9 principle is a *separate*, timeout-specific checkpoint concept, not a modification of the existing one); downstream isolation; the Unit 3-B remedy-family classifications; Unit 3-D's own reserved comparative-evaluation authority (still not begun — Section 8 defines a boundary for a future Unit 3-D task to respect, not an exercise of that authority); or Unit 4's own reserved production-implementation authority (still not begun).

## 11. Prohibited interpretations

This determination must not be read as: a live-execution authorization (none is granted); an implementation of any of Sections 3–9 (none is performed — a future, separately-governed implementation task must still occur, followed by this programme's own full Completion/Readiness/Approval chain); a claim that cold-start causation for Attempt 3 is proven (unaffected by this document; remains STRONGLY SUPPORTED, NOT CONFIRMED, per the Investigation Review); a resolution that treats Section 4's sub-cases 2–4 identically to sub-case 1 (they remain measurement-invalidating, unchanged from existing governance); a numeric concentration threshold for the Section 9 checkpoint (explicitly not invented); a ranking, comparison, or selection of any remedy family; or an amendment of Unit 3-B, Unit 3-D, or Unit 4 authority.

## 12. Status

```text
PROPOSED — PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
