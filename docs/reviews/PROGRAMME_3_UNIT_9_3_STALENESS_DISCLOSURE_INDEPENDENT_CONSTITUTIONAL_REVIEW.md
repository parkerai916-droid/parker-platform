# Programme 3 Unit 9.3 — Staleness Disclosure — Independent Constitutional Review

## Status

**Independent constitutional review only.** No production code or test was modified during this review. Unit 9.4, Unit 9.5, and Unit 9.6 were not begun. Nothing is staged, committed, or pushed. This review does not rely on `docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md`'s own account — every claim below was independently re-derived from primary governance text and the actual code, read fresh, including one directly relevant repository precedent the Completion Review never cited.

---

## Repository Baseline

- **HEAD:** `883cb6b1bffab7870b887dafbc2c445062631b47`
- **Branch:** `main`
- **Remote:** `origin/main` confirmed identical to local `HEAD`.
- **Working tree, confirmed before this review began:**
  ```
   M src/runtime/DefaultKnowledgeRetrieval.kt
   M tests/runtime/DefaultKnowledgeRetrievalTest.kt
  ?? docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md
  ```
  Exactly as expected. No discrepancy.
- **Staged changes:** none.

---

## Authorities Reviewed

Read fresh for this review: `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted, §2, §3, §7); `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` (§4, Unit 9.3's own entry, read exactly); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §3 (Amendment 7, the staleness definition itself, re-read word for word); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`; `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (Adopted); the Unit 9.1 and Unit 9.2 accepted implementations and their own Independent Constitutional Reviews; `src/runtime/DefaultKnowledgeRetrieval.kt` and `tests/runtime/DefaultKnowledgeRetrievalTest.kt` (both current, in full); `docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md`; `src/interfaces/KnowledgeStore.kt` (`KnowledgeLifecycleEvent`, `KnowledgePromotion`, `KnowledgeRetirement`, `KnowledgeRestoration`). Repository precedent search performed directly, not assumed: `src/runtime/DefaultWorldModelUpdatePolicy.kt`, found via direct grep for `Duration.of`/`Clock` usage across `src/` — a genuinely on-point precedent for age-based, query-time-checked currency, not cited anywhere in the Completion Review.

---

## 1. Governing Definition Review

Contract Design V2 §3 (Amendment 7), read exactly: "A Knowledge Item's classification reflects the state of its underlying Memory Core evidence at the moment that classification was computed. **Where the underlying evidence's status changes afterward** (for example, becomes disputed) **before Knowledge Memory has re-evaluated and produced a new classification, the Knowledge Item is stale.**"

The governing condition is unambiguous: **underlying evidence-status change, un-reconciled by re-evaluation.** It is not elapsed time, not classification age, and not merely "a lifecycle event occurred." An item classified a year ago whose evidence has never changed status is not stale under this definition, however long ago it was classified. An item classified one hour ago whose evidence changed status fifty-nine minutes ago is stale under this definition, however recently it was classified. Time is present in the definition only as a *reference point* ("at the moment... computed"), never as the *triggering condition*. This review does not substitute implementation convenience for this meaning; it is stated here exactly as written, before evaluating whether the implementation matches it.

---

## 2. Threshold Authority Review

Stated plainly, per this review's own instruction: **no accepted governance document authorises a 30-day threshold, any age threshold, time-since-classification as a proxy, or the use of the latest history event as the reference timestamp.** The only latitude any governing document grants is mechanism-*timing*, not signal-*choice*: Contract Design §11 and the Implementation Plan §4 both defer "the specific staleness-detection mechanism (continuous monitoring, checked at query time, or otherwise)" to implementation. "Checked at query time... or otherwise" authorises *when* a check runs; it does not authorise *what the check measures*, and nowhere does any governing document suggest elapsed time is an acceptable substitute for the evidence-change condition Section 1, above, establishes as the actual governed meaning. This is a genuine gap between what was built and what was authorised, examined for lawfulness in Section 3.

---

## 3. Proxy Lawfulness Review

**The implementation discloses its own limitation honestly, in KDoc — but the disclosure understates the proxy's actual failure severity, and the failure mode itself is not merely narrower than the ideal, it is materially misaligned with it.**

Checked directly against the code: `isStale` computes `Duration.between(lastClassifiedAt, clock.instant()) > STALENESS_AGE_THRESHOLD`. This creates both false positives and false negatives, systematically, not as rare edge cases:

- **False positive:** an item classified 90 days ago whose evidence has never changed status is disclosed `stale = true` — but per Section 1's own governing definition, it is not stale.
- **False negative, and the more serious of the two:** an item classified one day ago whose underlying evidence became disputed twenty-three hours ago is disclosed `stale = false` — but per Section 1's own governing definition, it *is* stale, and Contract Design V2 §3's own closing clause exists specifically to prevent exactly this outcome: "this must never be silently concealed." A caller trusting a `stale = false` disclosure for a just-classified item has no way to know the disclosure could be wrong in the one direction the constitutional text most directly warns against.

The code's own KDoc does disclose both directions of this mismatch ("a long-unrevisited item may still be perfectly current, and a freshly-classified item's own evidence could in principle already have changed status moments later") — this is not concealment. But disclosure alone does not resolve the deeper question: for the large majority of items (anything under 30 days old, which will be nearly everything in a system this young), the proxy **collapses genuine uncertainty into a definitive `false`** — not because evidence is known to be unchanged, but because the mechanism has no way to know either way and defaults silently to the less alarming answer for the entire window. This is the precise failure mode Review Question 3 names: "collapses 'unknown' into... 'not stale.'" A richer representation, capable of expressing "indeterminate" as its own state distinct from both "known current" and "known stale," would not have this property. Section 4 examines whether building one was within Unit 9.3's own authority.

---

## 4. Unit 9.1 Contract Interaction

Unit 9.1's own accepted KDoc (added during its own remediation cycle) states precisely: "Unit 9.3... remains authorised to widen or replace it [`Boolean`]... if its own drafting finds a binary signal insufficient to satisfy the Unit 9 Contract Design's own staleness-disclosure guarantee... Any such widening or replacement is... a breaking change to an already-shipped public field -- it requires its own explicit, disclosed contract amendment when it happens, never a silent implementation-time drift introduced without one."

Applying Section 3's own finding to this text directly: a binary signal *is* insufficient here — not merely imprecise, but structurally incapable of expressing the one honest answer (genuinely indeterminate) that applies to the overwhelming majority of items under this mechanism. Unit 9.1 did not merely permit widening as an abstract possibility; it named the exact condition ("finds a binary signal insufficient") that this review's own analysis confirms is met. Unit 9.3 should, at minimum, have explicitly confronted this question and documented its own answer — whichever answer it reached — rather than keeping `Boolean` without discussing the alternative Unit 9.1 itself flagged. Given the severity of the false-negative case identified in Section 3, the better-supported answer was very likely to introduce a genuine indeterminate state, not retain a two-value type that cannot express one. This is not, however, a case requiring a *new* governance decision from outside Unit 9.3's own scope — Unit 9.1 already pre-authorised exactly this widening, through exactly this mechanism (a disclosed contract amendment performed by Unit 9.3 itself). The authority already exists; it was not exercised, or even visibly weighed.

---

## 5. Dependency Boundary Review

True staleness computation, per Section 1's own governing definition, requires reading Memory Core's current evidence status — via `MemoryRetrieval` at minimum, and no other approved source in this repository substitutes for it (Evidence Custodian and event history were both considered and neither carries the specific "has this evidence's own status changed" signal directly). Confirmed directly: `DefaultKnowledgeRetrieval` holds no such dependency, and Contract Design §3's own Non-Responsibilities forbid introducing one for this class. Unit 9.3 is correctly **not** authorised to introduce a Memory Core dependency. Given Section 2's finding that no governance authorises age as a substitute signal either, the lawful outcome, per this review's own named options, is **an indeterminate result** (honestly disclosing "cannot be determined without exceeding this Unit's own dependency boundary") — not a proxy dressed as a definitive Boolean answering a question the mechanism cannot actually answer.

---

## 6. Determinism Review

Three claims, distinguished precisely as this review's own instruction requires:

- **Deterministic execution for a fixed clock** — confirmed true, directly, by the test suite's own repeated-call assertions using an injected `Clock.fixed`.
- **Wall-clock-relative output in production** — confirmed true and honestly disclosed; two calls separated by more than the threshold may differ, exactly as `KnowledgePromotion.occurredAt`'s own already-accepted precedent tolerates for a genuinely time-based field.
- **Semantic correctness of the staleness decision** — **not established, and Sections 3–5 above show good reason to doubt it.** A computation can be perfectly deterministic and perfectly punctual while still answering a question other than the one governance actually asks. Determinism was never the concern this review needed to resolve; whether the deterministic answer is the *right* answer is the concern, and it is not.

---

## 7. History Timestamp Review

`isStale` reads `item.history.lastOrNull()?.occurredAt` — the most recent `KnowledgeLifecycleEvent` regardless of its own kind. `KnowledgePromotion` and a future `KnowledgeRevisionEvaluator`-produced revision both represent a genuine re-classification, and using their own `occurredAt` as the staleness reference point is defensible within the age-based framework Section 3 already questions on other grounds. `KnowledgeRetirement` and `KnowledgeRestoration`, by contrast, are Unit 7's own established "status change, never an evidential classification" — their own `occurredAt` does not represent a new classification being computed at all. The current implementation does not distinguish these cases: were the most recent event ever a retirement or restoration (structurally possible, though currently unreachable in production since Unit 7's own evaluators remain unwired), the staleness clock would reset against an event that never re-evaluated anything. This is a real, if currently dormant, imprecision — not an urgent defect, but one that should be disclosed rather than left for a future reader to discover once Unit 7's own lifecycle evaluators are eventually composed.

---

## 8. Threshold Boundary Review

Neither the "30 days" figure nor the strict-greater-than boundary (exactly-at-threshold treated as not-yet-stale) carries any constitutional or contractual authority — confirmed directly in Section 2. Both are internally consistent, correctly tested implementation choices, but the code's own KDoc characterisation ("not architecturally significant... may change freely") somewhat understates the actual position established by this review: it is not merely that the specific number is unimportant, but that *no governance authorises using age as the staleness signal at all* (Section 2), which is a materially stronger statement than "the number is arbitrary."

---

## 9. Unit Boundary Review

Confirmed directly, by source inspection and by `git diff --stat` (only `src/runtime/DefaultKnowledgeRetrieval.kt` and its own test file changed): no lifecycle-status filtering was introduced (the Unit 9.2-era "absence of policy" KDoc section is untouched); no `PermissionEngine` reference exists anywhere; `ParkerRuntime.kt` is untouched; no reference to `ReasoningContext` or any reasoning provider exists anywhere; `isStale` is a boolean threshold comparison, never a score, and no ranking-shaped declaration was added; matching remains the same case-insensitive substring test Unit 9.2 already established, with no semantic component introduced. No defect found in this section.

---

## 10. Test Quality Review

The seven new tests are internally sound — each verifies a real property of the code as written (the `>` boundary, the most-recent-entry selection, non-interference with matching/ordering, the defaulted-constructor shape), and none is vacuous in the narrow sense of asserting a tautology. But per this review's own framing, they **validate the implementation's own internal arithmetic, not an authorised governance contract** — no test, and no test *could*, construct a scenario in which "the underlying evidence's status changed" (Section 1's own actual governed condition), because the class has no way to observe that condition at all. The suite therefore freezes the proxy's own behaviour faithfully, which is a legitimate and useful thing for a test suite to do, but it should not be read, and is not currently framed, as evidence that the proxy correctly implements Contract Design V2 §3. No test exercises or even names the indeterminate case Sections 3–4 identify as missing from the type itself — a gap in coverage that follows directly from the same gap in the type.

---

## Findings

| # | Severity | Finding |
| --- | --- | --- |
| 1 | **Substantive** | The age-based proxy measures a condition materially different from the governed staleness definition (evidence-status change), producing a systematic false-negative risk for recently-classified items — the direction Article XIII / Contract Design V2 §3 most directly warns against concealing. |
| 2 | Substantive | Unit 9.1 explicitly pre-authorised widening `Boolean` when found insufficient; this condition is met, and the widening (with its own required disclosed-amendment documentation) was not performed or visibly weighed. |
| 3 | Moderate | No governance authorises age as a staleness signal at all (only mechanism-timing latitude); the code's own KDoc frames this as "the specific threshold is arbitrary," understating that the signal choice itself lacks authority. |
| 4 | Moderate | Retirement and restoration events are not distinguished from promotion/revision events when selecting the staleness reference timestamp — currently dormant, since Unit 7's own evaluators are unwired, but undisclosed as a forward-looking limitation. |
| 5 | Minor | No test names or exercises the "indeterminate" case the type cannot currently express, a direct consequence of Finding 1/2. |
| 6 | None (confirmed sound) | Unit boundary discipline (no Unit 9.4–9.6 work), determinism-for-a-fixed-clock, and Memory Core dependency boundary are each independently verified and found correct. |

---

## Required Corrections

Two corrections are required before acceptance; the first is substantive, not documentation-only:

1. **Determine, explicitly and on the record, whether `KnowledgeResultEntry.stale` should be widened from `Boolean` to a representation capable of expressing a genuine indeterminate state**, exercising the authority Unit 9.1's own KDoc already grants, and perform that determination as the "explicit, disclosed contract amendment" Unit 9.1 requires for it — reasoning through Section 3 and Section 4's own findings rather than deferring the question again. If the determination concludes widening is warranted, implement it; if it concludes the existing `Boolean` should be retained despite the false-negative risk, state that conclusion and its reasoning explicitly, so a future reader is not left to independently rediscover this review's own analysis.
2. **Rewrite the "Staleness" KDoc section to state plainly, without softening, that age is not authorised by any governing document as a staleness signal**, that the governed condition is evidence-status change (quoted exactly), and that the false-negative direction (recently-classified items whose evidence may have already changed) is the more serious of the proxy's two failure modes, not a symmetric limitation.

Findings 4 and 5 are recommended clarifications, not required corrections — both are real but non-blocking, and may reasonably be addressed alongside Correction 1 rather than separately.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

The mechanism *direction* Unit 9.3 chose — query-time computation from data already held, no Memory Core dependency — remains within the Implementation Plan's own "or otherwise" latitude and is not itself an authority violation; Unit boundary discipline, determinism-for-a-fixed-clock, and the dependency boundary are all independently confirmed sound. What blocks acceptance is substantive, not cosmetic: the specific signal chosen (age) does not measure the governed condition (evidence-status change) with anything close to the reliability its own Boolean disclosure implies, the resulting false-negative risk runs directly counter to Contract Design V2 §3's own closing purpose, and Unit 9.1 already granted, but Unit 9.3 did not exercise or visibly consider, the authority to widen the representation specifically to address exactly this situation. This is corrigible within Unit 9.3's own already-granted authority — it does not require a new, external governance decision — but it is not a documentation-only gap, and does not qualify for acceptance as built.

---

## Recommended Next Step

Perform Required Correction 1 as a genuine design determination, not a documentation pass — decide, with reasoning on the record, whether `stale: Boolean` should become a three-state (or otherwise richer) disclosure, exercising Unit 9.1's own pre-granted widening authority. Apply Required Correction 2 regardless of that outcome. Re-run the full test suite, update or add tests to match whatever representation is settled on (including, if retained, an explicit test or KDoc statement naming the indeterminate case the current type cannot express), and produce a Defect Confirmation Review verifying both corrections before treating Unit 9.3 as accepted. Only after that confirmation should Unit 9.4 begin.

---

## Git Confirmations

- No production code or test was modified during this review.
- Unit 9.4, Unit 9.5, and Unit 9.6 were not begun.
- Nothing was staged during this review.
- Nothing was committed during this review.
- Nothing was pushed during this review.

## Final Git Status

```
$ git status --short
 M src/runtime/DefaultKnowledgeRetrieval.kt
 M tests/runtime/DefaultKnowledgeRetrievalTest.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```
