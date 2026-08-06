# Programme 3 — Unit 9.3: Staleness Disclosure — Completion Review

## Status

**Revised.** This document originally recorded a self-performed Independent Constitutional Review that concluded "no genuine defect found." A subsequent, genuine Independent Constitutional Review (`docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`) reached a different, controlling verdict — **REQUIRES REVISION** — and identified two Required Corrections. This revision documents both corrections as applied, including the constitutional design determination Required Correction 1 itself demanded: that `KnowledgeResultEntry.stale: Boolean` be widened to `KnowledgeResultEntry.staleness: StalenessDisclosure`, exercising the extension point Unit 9.1's own KDoc had already, explicitly pre-authorised. The self-review section below is retained, struck through in effect by this revision, and superseded by the material that follows it. Unit 9.3 only is implemented, exactly as `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` §4's own Unit 9.3 entry specifies. Units 9.4 through 9.6 are not begun. No governance document was modified. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD at start:** `883cb6b1bffab7870b887dafbc2c445062631b47`
- **Branch:** `main`
- **Working tree at start:** clean.
- **HEAD at the time of this revision:** unchanged — no commit has occurred between the original implementation and this widening.

---

## Governance Read Before Implementation

Read fresh, in full, before writing any code: `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_IMPLEMENTATION_PLAN.md` (§4, Unit 9.3's own objective, dependencies, repository impact, verification requirements); `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md` (Adopted, §2, §3, §7); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §3 (Amendment 7, staleness's own constitutional definition, re-read exactly). Source read in full: `src/runtime/DefaultKnowledgeRetrieval.kt` and `tests/runtime/DefaultKnowledgeRetrievalTest.kt` (both post-Unit-9.2-acceptance); `src/runtime/DefaultKnowledgeCandidateEvaluator.kt` (as the disclosed-narrower-baseline precedent this Unit's own reasoning mirrors).

---

## The Central Design Problem, Stated Precisely

Contract Design V2 §3 (Amendment 7) defines staleness as a *comparison*: a Knowledge Item is stale when the underlying Memory Core evidence's status has changed since the item's classification was last computed, and Knowledge Memory has not yet re-evaluated. Performing that comparison requires reading Memory Core's current state — exactly the query `DefaultKnowledgeRetrieval` is structurally forbidden from making (Unit 9 Contract Design §3; preserved unchanged by this Unit). This means **no implementation of Unit 9.3, however it is built, can compute the true constitutional definition of staleness while also preserving Memory Core separation.** Every design decision below follows from resolving this tension honestly rather than concealing it.

---

## What Was Implemented (as originally accepted, before widening)

**`src/runtime/DefaultKnowledgeRetrieval.kt`, modified.** `stale = true` (the Unit 9.2 placeholder) is replaced with `stale = isStale(item)`, a private method computing an age-based proxy: an item is disclosed as stale when more than a disclosed, implementation-defined threshold (30 days) has elapsed, per an injected `Clock`, since its own most recent history entry's `occurredAt`. The class gains one new constructor parameter, `clock: Clock = Clock.systemUTC()`, defaulted so every pre-existing Unit 9.2 call site continues to compile and behave unchanged. No other method, field, or dependency was added; `persistence`, `matches`, `retrieve`'s own matching/filtering/bounding logic, and the lifecycle-status non-filtering behaviour are all untouched.

**`tests/runtime/DefaultKnowledgeRetrievalTest.kt`, modified.** The one test that asserted the old placeholder's constant `true` was replaced (it tested behaviour that no longer exists). Seven new tests were added: fresh item discloses not-stale; long-classified item discloses stale; exact-threshold boundary (not yet stale) and one-instant-past-threshold boundary (stale); a mixed batch disclosing each item's own staleness independently; staleness computed from the most recent history entry, not an earlier one; staleness not affecting match/order; and the defaulted-`clock` construction shape remaining valid. The existing determinism test was strengthened to inject a fixed clock explicitly, rather than relying on incidental same-millisecond execution. The constructor-shape structural test was updated to expect two dependencies (`KnowledgeItemPersistence`, `Clock`) instead of one — a legitimate, expected update given this Unit's own authorised new dependency, not a weakening of the test.

**This section is superseded below.** The `Boolean` shape described here is no longer what the repository contains; see "The Widening Determination" and "What Widening Changed," below, for the accepted final shape.

---

## Design Decisions and Reasoning (as originally accepted, before widening)

1. **Age-based elapsed-time proxy, not evidence comparison.** The only genuinely time-based signal Knowledge Memory holds without touching Memory Core is each history event's own `occurredAt`. This is honestly, explicitly disclosed in the code's own KDoc as a *narrower proxy*, not an implementation of the constitutional definition — elapsed time is not evidence that underlying evidence changed status, and the KDoc says so directly, mirroring `DefaultKnowledgeCandidateEvaluator`'s own identical "disclosed narrower baseline, not the full constitutional ideal" precedent.
2. **"Checked at query time," not continuous monitoring.** The Implementation Plan's own Unit 9.3 entry leaves both directions open. Continuous monitoring (subscribing to Memory Core's own published events) would come closer to the true definition, but requires a new dependency and new durable state neither the Contract Design nor the Implementation Plan's own Unit 9.3 entry authorises, and would itself constitute the "runtime wiring" this task's own governing instructions forbid. Computing the proxy at query time, from data already held, requires neither.
3. **An injected `Clock`, defaulted to the real system clock.** Required for deterministic, instant tests (no sleeping, no flakiness); defaulting it preserves every pre-existing Unit 9.2 call site unchanged, and introduces no new production dependency callers must supply.
4. **The one disclosed exception to "preserve deterministic behaviour."** Matching, ordering, and bounding remain fully deterministic, unchanged from Unit 9.2. Staleness disclosure is, necessarily, wall-clock-relative — two calls genuinely separated in real time by more than the threshold may honestly differ in which entries they disclose as stale. This is not a violation of Unit 9.2's own determinism guarantee; it is the same, already-accepted exemption this repository already grants `KnowledgePromotion.occurredAt` (`DefaultKnowledgeCandidateEvaluator`'s own KDoc: "timestamps may legitimately differ across repeated evaluations... does not weaken... the deterministic identity and classification guarantees"), applied here to a second, equally genuine time-based field rather than a new kind of non-determinism.

These four decisions remain correct and are unaffected by the widening below — none concerned the shape of the disclosed value itself.

---

## The Widening Determination

`docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` reached **REQUIRES REVISION**, not the "no defect found" conclusion this document originally recorded. Its Required Correction 1 required an explicit, on-the-record determination of whether `KnowledgeResultEntry.stale` should be widened from `Boolean` to a representation capable of expressing indeterminacy, "reasoning through Section 3 and Section 4's own findings rather than deferring the question again."

**That determination, made explicitly here: widening is constitutionally required.** The reasoning:

1. **Contract Design V2 §3 (Amendment 7) defines staleness as a *confirmed* condition, not an inferred one.** Its own text: "Where the underlying evidence's status changes afterward... the Knowledge Item is stale, and this must never be silently concealed." A `Boolean` has exactly two states. Given this class's structural inability to perform the underlying comparison (see "The Central Design Problem," above), neither `true` nor `false` can ever mean what governance defines "stale" or "not stale" to mean — every `Boolean` value this class could ever produce is, at best, a proxy signal wearing the constitutional term's own clothing.
2. **The false-negative direction is the governing problem, not a symmetric limitation.** A freshly-classified item's `stale = false` asserts freshness this class has no basis to assert — its own evidence could have changed status moments after classification, and this class would have no way to know. Article XIII ("uncertainty must never be concealed") is violated by exactly this asserted-but-unearned `false`, not merely approximated.
3. **`DefaultKnowledgeCandidateEvaluator`'s "disclosed narrower baseline" precedent is not, by itself, sufficient here.** That precedent licenses implementing *fewer* of the constitutional factors and saying so in KDoc. It does not license representing an inferred, unconfirmed signal using a type (`Boolean`) whose own two values are indistinguishable, at the call site, from a confirmed one. The repository's own governing principle — "an unknown value is never fabricated, defaulted, or inferred without a disclosed inference step" — requires the disclosure to reach the point of use, at runtime, not merely exist in source-level KDoc a caller may never read. A `Boolean` cannot carry "this was inferred, not confirmed" as part of its own runtime value; a wider type can.
4. **`DefaultWorldModelUpdatePolicy`'s own age-based `isStillCurrent` is not a counter-precedent.** It governs a different, legitimately age-based epistemic model — World Model belief transience, per ADR-024/Reconciliation §10 — where elapsed time genuinely *is* the governed condition. Contract Design V2 §3 defines Knowledge Memory staleness as evidence-status change, not elapsed time; the naming parallel between the two classes' threshold constants is convention only, and this document (and the class's own KDoc) says so explicitly to prevent the precedent being misread as a claim of conceptual equivalence.
5. **Unit 9.1's own KDoc already pre-authorised exactly this.** Its own text (`src/interfaces/KnowledgeStore.kt`, added during Unit 9.1's own remediation cycle): Unit 9.3 "remains authorised to widen or replace [`stale: Boolean`]... if its own drafting finds a binary signal insufficient to satisfy the Unit 9 Contract Design's own staleness-disclosure guarantee. Any such widening or replacement is... a breaking change to an already-shipped public field -- it requires its own explicit, disclosed contract amendment when it happens, never a silent implementation-time drift introduced without one." This document, together with the interface KDoc change described below, **is** that explicit, disclosed contract amendment.

**Conclusion: a binary signal is insufficient.** Widening is implemented below, as Unit 9.1's own pre-authorised extension point.

---

## What Widening Changed

**`src/interfaces/KnowledgeStore.kt`, modified.** `KnowledgeResultEntry.stale: Boolean` is replaced with `KnowledgeResultEntry.staleness: StalenessDisclosure`, a new four-value enum:

- `CONFIRMED_CURRENT`, `CONFIRMED_STALE` — **reserved**, corresponding to Contract Design V2 §3's own actual, confirmable definition; never assigned by any mechanism this Programme has built as of Unit 9.3, since assigning either would require the Memory Core comparison `DefaultKnowledgeRetrieval` is structurally forbidden from performing.
- `POSSIBLY_STALE` — the honest, disclosed age-based signal Unit 9.3's own mechanism actually assigns: more than a disclosed threshold has elapsed since the item's classification was last computed. Framed explicitly as "may warrant re-verification," never a confirmed-stale claim.
- `INDETERMINATE` — the honest default for every item the age-based signal does not distinguish as unusually old. Deliberately not a claim of freshness — this is the value that replaces the old, unearned `false`.

**`src/runtime/DefaultKnowledgeRetrieval.kt`, modified further.** `isStale(item): Boolean` is replaced with `disclosureFor(item): StalenessDisclosure`, implementing the logic above. The reference-timestamp lookup is also corrected (Finding 4 of the Independent Constitutional Review, addressed alongside Required Correction 1 per that review's own "may reasonably be addressed alongside" text): it now searches only `KnowledgePromotion` history entries (`item.history.filterIsInstance<KnowledgePromotion>().lastOrNull()?.occurredAt`), excluding `KnowledgeRetirement` and `KnowledgeRestoration` entries, since neither represents a genuine reclassification (Unit 7's own established precedent: a retirement or restoration "is a status change, never an evidential classification"). An item with no qualifying `KnowledgePromotion` entry discloses `INDETERMINATE`, not `POSSIBLY_STALE` — there is no reference timestamp to measure elapsed time against, and therefore no basis for either claim. `STALENESS_AGE_THRESHOLD` is renamed `POSSIBLY_STALE_AFTER`, matching `DefaultWorldModelUpdatePolicy.DEFAULT_STALE_AFTER`'s own naming convention (a naming parallel only, disclosed as such in the KDoc — see determination point 4, above). The "Staleness" KDoc section is rewritten in full to satisfy Required Correction 2: it quotes Contract Design V2 §3's governed definition exactly, states plainly that age is not authorised by any governing document as a staleness signal, and identifies the false-negative direction as the more serious of the proxy's two failure modes, not a symmetric limitation.

**`tests/runtime/DefaultKnowledgeRetrievalTest.kt`, modified further.** Every `.stale` assertion is replaced with a `.staleness` assertion against the appropriate `StalenessDisclosure` value. Two new tests verify the retirement/restoration reference-timestamp fix (a `KnowledgeRetirement` and, separately, a `KnowledgeRestoration` following an old promotion must not reset the staleness clock). One new test verifies an item with no `KnowledgePromotion` history entry discloses `INDETERMINATE`.

**`tests/contracts/KnowledgeRetrievalContractsTest.kt`, modified.** Every `KnowledgeResultEntry(..., stale = ...)` construction site is updated to `staleness = StalenessDisclosure.<value>`. The "stale is a non-nullable Boolean" structural test is rewritten to assert `StalenessDisclosure::class` non-nullability instead. One new test asserts `StalenessDisclosure` is closed to exactly its four named values.

---

## Verification Performed (post-widening)

```
$ ./gradlew clean test
BUILD SUCCESSFUL in 46s
5 actionable tasks: 5 executed
```

- **`DefaultKnowledgeRetrievalTest`:** 26/26 passed.
- **`KnowledgeRetrievalContractsTest`:** 19/19 passed, including the new `StalenessDisclosure` closure test.
- **Full repository suite:** 1709 tests, 0 failures, 0 errors, 5 pre-existing skips (up from 1705 at the pre-widening baseline this document originally recorded; +4 new tests, 0 regressions).

---

## Original Self-Performed Independent Constitutional Review (superseded)

Retained verbatim for the record; **superseded by the genuine, separate Independent Constitutional Review's REQUIRES REVISION verdict** and by "The Widening Determination," above. Its "no genuine defect found" conclusion below is incorrect and must not be relied upon.

Audited as if written by another reviewer, against the adopted Contract Design, the Implementation Plan's own Unit 9.3 entry, and the actual compiled code:

- **Does this Unit implement the true constitutional staleness definition?** No, and it does not claim to — the KDoc states this explicitly and repeatedly, distinguishing "elapsed time since classification" from "underlying evidence changed status," and explains precisely why the latter is structurally unreachable without violating Memory Core separation.
- **Does this Unit preserve Memory Core separation?** Yes — no new dependency beyond `Clock` (a JDK time source, not a domain dependency) was added; confirmed no `MemoryRetrieval`/`MemoryCore` reference exists anywhere in the file, and the existing structural test proving this still passes unmodified.
- **Does this Unit introduce ranking, semantic retrieval, permission evaluation, runtime wiring, Reasoning Context integration, or new lifecycle behaviour?** No — `isStale` is a boolean threshold check, not a score; no `PermissionEngine` reference exists; `ParkerRuntime.kt` is untouched (confirmed by `git diff --stat`); no reasoning-provider reference exists; `KnowledgeItem.status` is still never consulted, and the Unit 9.4-reserved lifecycle-shape KDoc section is untouched.
- **Does the `Clock` addition genuinely preserve Unit 9.2's own determinism guarantee?** Matching, ordering, and bounding are byte-for-byte unchanged and remain fully deterministic. Staleness disclosure alone is wall-clock-relative, necessarily, since no genuine time-based signal can be otherwise — this is disclosed as the one exception, not concealed, and grounded in an already-accepted repository precedent for exactly this class of exemption, not a newly invented one.
- **Is the 30-day threshold itself defensible?** It is disclosed as arbitrary and not architecturally significant, mirroring `MEMORY_QUERY_MAXIMUM_RESULTS`'s own identical treatment — this is a legitimate implementation-tier choice the Contract Design's own "or otherwise" latitude authorises, not a constitutional claim about what "stale" truly means.
- **Do the new tests verify governance rather than manufacture it?** The threshold-boundary tests (exact threshold not stale; one instant past is stale) verify the actual `>` comparison this class implements, not an arbitrary assumption; the "most recent history entry governs, not an earlier one" test directly verifies the same principle Unit 9.2's own matching logic already established, applied here to staleness; the "does not affect match/order" test verifies the two concerns remain properly separated. None of these tests merely restate what the implementation happens to do without reference to a stated requirement.
- **Does this Unit modify any governance document?** No — confirmed by `git status`; only `src/runtime/DefaultKnowledgeRetrieval.kt` and its own test file changed.

**This self-review's conclusion — "no genuine defect found requiring correction" — did not survive genuine independent scrutiny.** It examined whether the `Boolean` proxy was an honestly-disclosed *narrower* signal, and correctly found that it was, but never asked the more fundamental question the genuine Independent Constitutional Review went on to ask: whether a `Boolean`, of any disclosed narrowness, is the right *shape* for a signal this uncertain. See "The Widening Determination," above.

---

## Files Modified

- `src/interfaces/KnowledgeStore.kt` (`KnowledgeResultEntry.stale: Boolean` widened to `KnowledgeResultEntry.staleness: StalenessDisclosure`; new `StalenessDisclosure` enum added)
- `src/runtime/DefaultKnowledgeRetrieval.kt` (staleness placeholder replaced with a disclosed age-based proxy; one new constructor parameter, defaulted; subsequently widened to return `StalenessDisclosure`; reference-timestamp lookup corrected to `KnowledgePromotion`-only; "Staleness" KDoc rewritten per Required Correction 2)
- `tests/runtime/DefaultKnowledgeRetrievalTest.kt` (staleness tests updated to `StalenessDisclosure`; two new tests for the retirement/restoration reference-timestamp fix; one new test for the no-`KnowledgePromotion` case)
- `tests/contracts/KnowledgeRetrievalContractsTest.kt` (`KnowledgeResultEntry` construction sites and structural test updated for `staleness: StalenessDisclosure`; one new test asserting the enum's closure)
- `docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md` (this document; revised)
- `docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` (pre-existing, unmodified by this revision — the controlling review this revision responds to)

## Test Results

- `DefaultKnowledgeRetrievalTest`: **26/26 passed**
- `KnowledgeRetrievalContractsTest`: **19/19 passed**
- Full repository suite: **1709 tests, 0 failures, 0 errors, 5 pre-existing skips**

## Constitutional Verdict

```
UNIT 9.3 — COMPLETE. WIDENING APPLIED PER REQUIRED CORRECTION 1.
SEE THE DEFECT CONFIRMATION REVIEW FOR FINAL ACCEPTANCE.
```

## Recommendation

This revision resolves both Required Corrections the genuine Independent Constitutional Review identified. A Defect Confirmation Review (`docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_DEFECT_CONFIRMATION_REVIEW.md`) follows, verifying both corrections were properly applied and that no regression was introduced. Unit 9.3 should be treated as fully accepted only once that Defect Confirmation Review is itself adopted. Unit 9.4 (Retirement and Supersession Retrieval-Shape Decision) and Unit 9.6 (Runtime Composition) both depend on `KnowledgeResultEntry`'s own shape; any code written against the old `stale: Boolean` field before this revision does not exist anywhere in this repository (confirmed by the repository-wide search performed before this widening began), so no downstream remediation is required. Unit 9.5 remains blocked behind its own governance precondition, unaffected by this widening.

---

## Final Git Status

```
$ git status --short
 M src/interfaces/KnowledgeStore.kt
 M src/runtime/DefaultKnowledgeRetrieval.kt
 M tests/contracts/KnowledgeRetrievalContractsTest.kt
 M tests/runtime/DefaultKnowledgeRetrievalTest.kt
?? docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_COMPLETION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_3_STALENESS_DISCLOSURE_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
