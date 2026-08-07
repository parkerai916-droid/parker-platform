# Authorization Purpose — Unit 4 (Permission Policy Integration) — Completion Review

## Status

Completion Review for Authorization Purpose Implementation Plan Unit 4 only (`docs/implementation/AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN.md` §6). Unit 5 is not begun. No governance document is amended by this review.

---

## 1. Baseline

Branch `main`, `HEAD` `b12e73f` ("feat: add Authorization Purpose carrier and registry", Units 1–3), tracking `origin/main`. Confirmed independently at Planning Review time (`docs/reviews/AUTHORIZATION_PURPOSE_UNIT_4_PLANNING_REVIEW.md` §0). Pre-existing, unrelated uncommitted work (Conversational Memory Admission, a Programme 3 clarification) present before this Unit began, unchanged by it.

---

## 2. What Was Built

- **`src/runtime/DefaultPermissionPolicy.kt`** (modified) — `PermissionPolicyRule` gains one defaulted field, `authorizationPurpose: AuthorizationPurposeId? = null`. `DefaultPermissionPolicy` gains one defaulted constructor parameter, `authorizationPurposeRegistry: AuthorizationPurposeRegistry? = null`. `evaluate` computes an `effectivePurpose` (the request's own declared purpose, folded to `null` unless the registry confirms it currently active) once per evaluation, and passes it to `ruleOutcomeFor`, which now prefers a rule naming that exact purpose over a coarser rule addressing the same `(action, resourceType)` pair, unconditionally — falling back to the coarse rule, then to the pre-existing "no rule matches → DENIED" default, exactly as Planning Review §3 designed.
- **`tests/runtime/DefaultPermissionPolicyTest.kt`** (modified) — `request()`'s helper gains one defaulted parameter (`authorizationPurpose`); eight new tests added (numbered 10–17 in the file's own comments), covering regression-freeness (two tests: purpose-blind requests unaffected by a purpose-aware rule's presence; the 3-arg, registry-omitted construction shape `ParkerRuntime.kt` still uses), precedence in both restrictiveness directions (two tests, ruling out "most restrictive wins" as an alternative explanation), fail-closed behaviour for unregistered and retired purposes (two tests), the "no coarse fallback" DENIED case (one test), and precedence-safety tested against both rule-list orderings (one test).

**Nothing else was touched.** `git diff --stat` confirms exactly these two files carry this Unit's own changes; every other file in the diff (`ParkerRuntime.kt`, `ConversationReplyCoordinator.kt`, etc.) predates this Unit, confirmed present in the baseline `git status --short` taken before Planning Review began.

---

## 3. Conformance to Unit 4's Own Specification

- **Purpose** — "Extend `DefaultPermissionPolicy`'s own resolution step so Authorization Purpose participates in evaluation, satisfying the Scope Lock's own frozen fail-closed and precedence-safety requirements." Done.
- **Authority** — Scope Lock §2.4, in full: single Permission Engine/Policy (no second class introduced); fail-closed evaluation (absent/unregistered/retired purposes fold into the pre-existing "no rule matches" default, tested); Authorization Purpose participates as an optional, additional matching dimension (implemented); no caller-specific exceptions (the mechanism is keyed on the purpose value alone, never on caller/principal/module identity — confirmed by inspection, no `if caller == X` shape anywhere in the diff); no second authorization system (confined entirely to `ruleOutcomeFor`, one method, one class); precedence safety (Section 3.2/tests 12, 13, 17).
- **Inputs** — Unit 2's own `ExecutionRequest.authorizationPurpose` field (read, not modified); Unit 3's own `AuthorizationPurposeRegistry.isActive` (called, not modified).
- **Outputs** — matches exactly: `evaluate`'s resolution step additionally consults the Authorization Purpose field where present, as an optional, additional matching dimension; a coarse rule never resolves a request a more specific, active-purpose-aware rule was meant to govern (tests 12, 13, 17); an absent or unregistered value denies by the same default every other unknown value already denies by (tests 14, 16); a retired value is treated identically (test 15).
- **Files expected to change** — `src/runtime/DefaultPermissionPolicy.kt`, exactly as named. No refinement needed.
- **Non-responsibilities** — `PermissionEngine`'s own interface untouched (confirmed: `src/interfaces/PermissionEngine.kt` not in the diff); no `PermissionPolicyRule` content added for any real domain (every new rule in this Unit's own tests uses synthetic, test-only action/resource/purpose combinations, e.g. `"household.routine-maintenance"`, never wired to `ParkerRuntime.kt`); the precedence algorithm's own internal data structure was decided (a simple, documented two-step `find`, Planning Review §3.2) without over-specifying beyond the frozen outcome constraint.
- **Completion criteria** — `DefaultPermissionPolicy` compiles and evaluates exactly as before for every request that does not populate `authorizationPurpose` (tests 1–9 unmodified and passing, plus new test 10/11 explicitly proving this in the presence of a purpose-aware rule and in the registry-omitted construction shape); for a request that does populate it, evaluation is deterministic (same rule-matching logic, no randomness or ordering-dependent behaviour — test 17 proves this directly across both rule-list orders), fails closed for an absent/unregistered/ineligible value (tests 14–16), and never lets a coarse rule resolve a request a more specific rule was meant to govern (tests 12, 13, 17).
- **Stop condition** — "if satisfying the precedence-safety constraint reveals that the only workable design requires a second policy-evaluation pass, a second rule table, or any structure resembling a second authority, stop." Not triggered: the design (Planning Review §3.2) is a single, additional `find` inside the same, single `ruleOutcomeFor` method, evaluated once per resolved mapping, within the same single pass `evaluate` already performs. No second pass, no second table, no second authority.

---

## 4. Full Repository Verification

`./gradlew clean test`: **BUILD SUCCESSFUL**, **1969 tests** (up from 1961 before this Unit — exactly the 8 new tests added), **0 failures**, **5 skipped** (pre-existing, unrelated, unchanged count).

---

## 5. Explicit Non-Responsibilities Honoured

No Authorization Purpose value created for any real domain (only test-fixture placeholders, e.g. `"household.routine-maintenance"`, never wired to any real consumer). No vocabulary entry registered outside test fixtures. No change to `ParkerRuntime.kt` (confirmed: not touched by this Unit's own diff — its presence in the overall repository diff predates this Unit). No change to Gap #54 documents, Knowledge Submission, or Evidence Intelligence. No policy content for `memory.retrieve`/`memory.retrieve_document`. No verb-phrase discriminator implemented (Planning Review §2 — explicitly, deliberately excluded, disclosed in `DefaultPermissionPolicy`'s own KDoc).

---

## 6. Files Created

- `docs/reviews/AUTHORIZATION_PURPOSE_UNIT_4_PLANNING_REVIEW.md`
- `docs/reviews/AUTHORIZATION_PURPOSE_UNIT_4_COMPLETION_REVIEW.md` (this document)

## 7. Files Modified

- `src/runtime/DefaultPermissionPolicy.kt`
- `tests/runtime/DefaultPermissionPolicyTest.kt`

---

## Recommended Next Step

Proceed to a genuine Independent Constitutional Review of this Unit's own implementation.
