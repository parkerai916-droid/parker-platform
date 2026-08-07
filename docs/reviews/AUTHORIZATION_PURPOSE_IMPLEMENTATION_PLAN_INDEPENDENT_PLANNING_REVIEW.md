**Status:** Genuine Independent Planning Review of `docs/implementation/AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN.md`, performed as if by another reviewer, against `docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` and its own four source governance documents — not against the Implementation Plan's own Section 13 self-check alone. This document does not amend the Implementation Plan, the Scope Lock, or any other governance document, Kotlin file, or test. Nothing is staged, committed, or pushed.

# Authorization Purpose Implementation Plan — Independent Planning Review

## 1. Baseline Confirmation

`git status --short` confirms the Implementation Plan and this review are the only new files at review time, alongside the already-known, uncommitted Conversational Memory Admission work. Confirmed the Scope Lock and every other cited governance document are untouched.

---

## 2. Challenge — Does Unit 6's Own Verification Design Actually Avoid Touching Production Composition Code? (Substantive Finding)

Unit 6's own "Files expected to change" states: "None beyond what Units 1–5 already touched — this unit is verification of the already-built mechanism, not new production code." Its own "Outputs" requires "a single, clearly-synthetic, test-only Authorization Purpose value... registered" to prove the mechanism's fail-closed/precedence-safe behaviour.

**Pressed directly: where does that registration actually happen, given Unit 3's own registration mechanism and Unit 5's own composition wiring?** Unit 5 wires the registry into `ParkerRuntime.kt`'s own real, production composition root. If the synthetic value is registered *there*, Unit 6's own "Files expected to change: None" is false — it would require touching `ParkerRuntime.kt` again, with test-only content mixed into production composition code, a design risk the Plan does not disclose or rule out. If, instead, the synthetic value is registered through a separate, test-tier harness that does not go through `ParkerRuntime.kt`'s own real composition at all, then Unit 6 is not genuinely *end-to-end* in the sense every other verification unit in this repository's own governance history has meant that phrase (for example, `ParkerRuntimeConversationalMemoryAdmissionCompositionTest.kt`, which exercises the real, live `ParkerRuntime`) — a materially weaker guarantee than the unit's own name claims, and the Plan does not disclose this trade-off either.

**This is a genuine planning gap, not a nitpick**: the Plan asserts a property ("Files expected to change: None," "end-to-end") without resolving the design tension that determines whether either claim is actually true. A future implementer could resolve it either way without violating anything explicitly written here, which is exactly the kind of ambiguity a Planning Review exists to close before implementation begins.

**Required correction:** Unit 6 must state explicitly that the synthetic value is registered through **test-tier code only** — never added to `ParkerRuntime.kt`'s own production registration set — and that "end-to-end" means exercising the real, composed `DefaultPermissionPolicy`/registry pairing Unit 5 wires, invoked from a test harness, not that `ParkerRuntime.kt` itself gains any test-only content. "Files expected to change" should be corrected to name new test files explicitly, not "None."

---

## 3. Challenge — Is the Dependency Graph's Own Prose Accurate?

Independently re-checked Unit 5's own "Dependencies" line: "Unit 4 (and transitively, Unit 3)." Independently re-checked Unit 4's own stated dependencies: "Unit 2, Unit 3." Since Unit 5 depends on Unit 4, and Unit 4 depends on both Unit 2 and Unit 3, Unit 5's own transitive dependencies include **both** Unit 2 and Unit 3, not Unit 3 alone. The dependency **graph diagram itself** (Section 2) is drawn correctly (Unit 5 sits below Unit 4, which sits below both Unit 2 and Unit 3) — only the prose sentence restating it is incomplete.

**Required correction:** Unit 5's own "Dependencies" line should read "Unit 4 (and transitively, Units 2 and 3)."

---

## 4. Challenge — Is the Suggested Sequence's Own Rejection (Section 2) Actually Justified, or Asserted?

Independently re-checked each of the four stated divergences against its own cited Scope Lock/Contract Design section: "runtime propagation" against Carrier Contract Design §6's own "Automatic" finding (confirmed, re-read directly in the Carrier Contract Design); "migration" against Scope Lock §3's own direct quotation (confirmed, re-read directly in the Scope Lock); "adoption by Memory Retrieval" against Scope Lock Deferred Decision 8 and this task's own explicit exclusion list (confirmed); "runtime integration" as a duplicate of composition wiring (a structural judgement, not a citation — reasonable, since no source document names a distinct "runtime integration" concern separate from composition). **Confirmed independently justified**, not merely asserted.

---

## 5. Challenge — Accidental Scope Lock Violation: Does Any Unit's Own "Outputs" Exceed What Section 2 of the Scope Lock Freezes?

Checked every unit's own "Outputs" against Scope Lock §2.1–2.6 directly. Unit 4's own precedence-safety outcome ("a coarse rule may never resolve a request a more specific... rule was meant to govern") is quoted, not paraphrased loosely, from Scope Lock §2.4 — confirmed exact. No unit's own "Outputs" states a Kotlin type name, field name, or algorithm beyond what Scope Lock §2 already permits as a behavioural constraint. **Confirmed no violation.**

---

## 6. Challenge — Premature Freezing: Does "Files Expected to Change" Cross Into API Freezing?

Checked whether naming file *locations* (e.g., "a contracts-tier file, alongside `src/contracts/Permission.kt`/`Resource.kt`'s own existing siblings") itself constitutes freezing an API. It does not — a file's own existence and rough location is an organisational fact, not a class or field name, and each unit's own text explicitly disclaims fixing the latter. **Confirmed no premature freezing**, consistent with the Plan's own Section 13 self-check claim.

---

## 7. Challenge — Unit 3's Own Stop Condition (Registration Access Control) — Genuine Question or Overcaution?

Pressed on whether "who is authorised to call the registry's own registration function at runtime" is genuinely unresolved by the Scope Lock, or already implicitly answered by "composition-time registration" (Scope Lock §2.3), the same way Action Vocabulary has never needed a separate access-control mechanism. Independently re-checked Vocabulary Governance Contract Design §10 (Plugin authorship): Plugins register "as part of its Tool registration" — a different, and not necessarily contemporaneous, event from `ParkerRuntime.kt`'s own core composition-time stage (Plugin installation/Tool registration could plausibly occur after initial startup, unlike core domain values). **This confirms the stop condition is a genuine, non-trivial question, not overcaution** — core-value registration timing is settled by convention (composition-time, mirroring Action Vocabulary), but Plugin-value registration timing is not obviously the same moment, and the Plan is correct to flag this as requiring its own resolution rather than assuming it away. **No correction required** — already adequately handled.

---

## 8. Findings

**Two required corrections:**

1. Unit 6's own verification design does not resolve whether the synthetic Authorization Purpose value is registered through production composition code (contradicting "Files expected to change: None") or through a narrower, non-end-to-end test harness (contradicting the unit's own "end-to-end" framing) — a genuine, unresolved design tension, not merely an imprecise description.
2. Unit 5's own "Dependencies" prose omits Unit 2 from its own stated transitive dependency list, though the dependency graph diagram itself is correct.

No other required correction was found. The rejection of "runtime propagation," "migration," "adoption by Memory Retrieval," and "runtime integration" as separate units was independently verified as justified, not asserted; no unit's own Outputs exceeds the Scope Lock's own freeze; no premature API freezing was found; and Unit 3's own stop condition regarding registration access control was independently confirmed to be a genuine, correctly-flagged open question, not overcaution.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

Two narrow, required corrections (Sections 2 and 3, above): resolve Unit 6's own verification-mechanism ambiguity by specifying test-tier-only registration and correcting "Files expected to change," and correct Unit 5's own transitive-dependency prose to include Unit 2. Proceeding to a Defect Confirmation Review after both corrections are applied.

**Post-correction status:** both required corrections were applied to `docs/implementation/AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN.md` Units 5 and 6. See `docs/reviews/AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN_DEFECT_CONFIRMATION_REVIEW.md` for the narrow Defect Confirmation Review, which found both corrections complete and no further defect. The Implementation Plan is accepted as of that review.
