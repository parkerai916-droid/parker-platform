**Status:** Genuine Independent Constitutional Review of Authorization Purpose Unit 4, performed as if by another reviewer, against Scope Lock §2.4 and the Implementation Plan's own Unit 4 section directly, and against the actual, current file contents — not against the Completion Review's own account alone. This document does not amend any governance document. Nothing is staged, committed, or pushed.

# Authorization Purpose — Unit 4 (Permission Policy Integration) — Independent Constitutional Review

## 1. Baseline and Diff-Shape Re-Verification

Independently re-ran `git diff --stat` — confirmed exactly two files carry this Unit's own changes: `src/runtime/DefaultPermissionPolicy.kt` and `tests/runtime/DefaultPermissionPolicyTest.kt`. Independently confirmed `src/interfaces/PermissionEngine.kt`, `src/runtime/DefaultPermissionEngine.kt`, `src/composition/ParkerRuntime.kt`, `src/runtime/AuthorizationPurposeRegistry.kt`, `src/contracts/AuthorizationPurposeVocabulary.kt`, `src/contracts/Identifiers.kt`, `src/contracts/ExecutionRequest.kt`, and `src/runtime/ActionMapper.kt`/`src/contracts/ActionMapping.kt` all show zero diff attributable to this Unit (every other file appearing in the overall repository diff predates this Unit, per the baseline `git status --short` recorded at Planning Review time).

---

## 2. Challenge — Single Permission Engine / Single Permission Policy

Independently re-read `DefaultPermissionEngine.kt` fresh: `evaluate` resolves identity, then calls `policy.evaluate(request)` once, passing the entire, unmodified `request` through — the same single `DefaultPermissionPolicy` instance `ParkerRuntime.kt` already composes. No second evaluator, no second policy class, no second `PermissionEngine` implementation anywhere in the diff. **Confirmed.**

---

## 3. Challenge — `PermissionEngine`'s Own Interface, Preserved

Independently re-read `src/interfaces/PermissionEngine.kt` — two lines, `evaluate`/`explain`, byte-identical to the version read at Planning Review time; zero diff. Authorization Purpose participation is achieved entirely by reading a field `ExecutionRequest` already carries (Unit 2) — no new parameter was ever needed on the interface, and none was added. **Confirmed.**

---

## 4. Challenge — Regression-Freeness, Traced by Hand Against the Actual Code

Independently hand-traced `evaluate` for a request with `authorizationPurpose == null` (every currently-existing production caller's own state, since no real caller sets it yet): `effectivePurpose = null?.takeIf { ... }` short-circuits to `null` without ever consulting `authorizationPurposeRegistry` — the suspend call is never made. `ruleOutcomeFor(mapping, null)`: `purposeAwareRule = null?.let { ... }` is `null`; `rule = rules.find { ... && it.authorizationPurpose == null }` — **byte-identical** to the pre-Unit-4 predicate (`it.action == mapping.action && it.resourceType == mapping.resourceType`), since every pre-existing rule's own `authorizationPurpose` defaults to `null`. **Confirmed: for every request and rule set that predates this Unit, the evaluated predicate is provably identical to the original, not merely behaviourally similar.** All nine pre-existing tests in `DefaultPermissionPolicyTest.kt` pass unmodified, corroborating this by execution, not only by trace.

Independently checked the registry-omitted construction shape specifically, since `ParkerRuntime.kt:507` constructs `DefaultPermissionPolicy` with exactly three positional arguments today: `authorizationPurposeRegistry` defaults to `null`; `effectivePurpose` then always resolves to `null` regardless of what any request declares (`authorizationPurposeRegistry?.isActive(purpose) == true` short-circuits to `false` on a `null` receiver), so the composed runtime's own behaviour is provably unaffected by this Unit even if a future caller populated `authorizationPurpose` before Unit 5 wires the registry in. **Confirmed — test 11 exercises this exact shape.**

---

## 5. Challenge — Precedence Safety, the Central Constitutional Requirement

Independently re-derived Scope Lock §2.4's own text: *"a coarse `(action, resourceType)` rule may never resolve a request for which a more specific, Authorization-Purpose-aware rule was the one actually meant to govern it."* Independently hand-traced `ruleOutcomeFor` against this: for a resolved mapping and a non-null `purpose`, `purposeAwareRule` is searched **first** and, if found, is returned **without ever consulting the coarse rule** — the coarse `rules.find` on the next line is inside an `?:` that short-circuits once `purposeAwareRule` is non-null. This is unconditional precedence, not restrictiveness-based selection: independently confirmed by tests 12 and 13, which prove the purpose-aware rule governs in **both** restrictiveness directions (a `DENIED` purpose-aware rule overriding an `APPROVED` coarse rule, and the reverse) — ruling out the alternative, incorrect implementation of just re-running the pre-existing "most restrictive wins" logic across the two candidate rules, which would have produced test 13's outcome backwards. Independently confirmed order-independence (test 17): both `[coarse, purposeAware]` and `[purposeAware, coarse]` list orderings produce the identical, correct result, ruling out an implementation that merely happens to prefer whichever rule a naive single `find` over a merged predicate encounters first. **Confirmed, the precedence-safety requirement is genuinely, not nominally, satisfied.**

---

## 6. Challenge — Fail-Closed Behaviour for Absent, Unregistered, and Retired Values

Independently re-derived Scope Lock §2.4's fail-closed clause and cross-checked it against the Implementation Plan's own regression-freeness completion criterion (Section 4 of this task's own Planning Review already resolved this tension in writing — re-verified fresh here rather than re-accepted). Independently hand-traced three cases against the actual code:

- **Unregistered** (test 14): `registry.isActive(purpose)` on a never-registered id — independently re-read `InMemoryAuthorizationPurposeRegistry.isActive`: `entries[id]?.status == ACTIVE`, and `entries[id]` is `null` for an unregistered id, so `null?.status == ACTIVE` is `false`. `effectivePurpose` folds to `null`. Falls back to the coarse rule. **Confirmed correct and matches the test's own outcome.**
- **Retired** (test 15): independently re-traced `InMemoryAuthorizationPurposeRegistry.retire` (Unit 3, unmodified by this Unit) — a retired entry's own `status` becomes `RETIRED`, never removed from the map. `isActive` therefore returns `false` for it, identically to the unregistered case. **Confirmed no special-casing was needed or added — `isActive`'s own pre-existing semantics already produce the correct fold for both cases**, exactly as Unit 3's own KDoc promised Unit 4 it would.
- **No coarse fallback available** (test 16): independently traced the case where the only rule in the table is purpose-aware and the request declares no purpose — `rule` resolves to `null` at both search steps, producing `DENIED`/`AUTOMATIC` via the pre-existing `if (rule != null) ... else DENIED to AUTOMATIC` branch — the same code path, unmodified, that has always produced this default. **Confirmed this is genuinely "the same default every other unknown value already denies by," not a new mechanism dressed up to look like the old one.**

---

## 7. Challenge — No Caller-Specific Exceptions

Independently re-read the full `ruleOutcomeFor`/`evaluate` diff for any `if (caller == ...)`, `if (request.principalId == ...)`, or equivalent shape — none exists. The matching key is `(action, resourceType, authorizationPurpose)` alone, uniformly, for every caller and every rule, present or future. **Confirmed.**

---

## 8. Challenge — Hidden Implementation of Unit 5 or Unit 6

Independently re-confirmed (Section 1) zero diff to `ParkerRuntime.kt`. Independently grepped this Unit's own two changed files for `ParkerRuntime`, `PermissionEngine` (beyond the untouched interface), and any real domain string (`memory.retrieve`, `knowledge.retrieve`, `EvidenceIntelligence`, `KnowledgeSubmission`) — none found; every test rule/purpose in the diff uses synthetic, clearly test-only names (`"household.routine-maintenance"`, `"household.never-registered"`, `"household.retired-purpose"`). **Confirmed no hidden Unit 5 (Composition Wiring) or Unit 6 (End-to-End Verification) work, and no policy-content decision for any real domain.**

---

## 9. Challenge — Was the Verb-Phrase Exclusion (Planning Review §2) Actually Honoured in the Implementation, Not Only Disclosed?

The Planning Review found the governance chain's own language potentially misleading about whether a verb-phrase discriminator already exists in code, and decided Unit 4 would not build one. Independently re-checked the actual diff for any verb-phrase-related addition: `PermissionPolicyRule` gained exactly one new field (`authorizationPurpose`), not two; `evaluate`'s own `.flatMap { it.mappings }` (which discards `ActionMappingResult.Resolved.proposedAction`) is unchanged, confirmed by diff. **Confirmed the disclosed exclusion was actually honoured in the code, not merely stated in a comment.**

---

## 10. Findings

No required correction was found. Single-engine/single-policy preservation, `PermissionEngine` interface preservation, regression-freeness (traced and test-confirmed, including the registry-omitted construction shape), precedence safety (traced and confirmed order-independent and restrictiveness-direction-independent), fail-closed behaviour for absent/unregistered/retired values, the absence of caller-specific exceptions, the absence of any hidden Unit 5/6 work, and the actual (not merely disclosed) exclusion of the verb-phrase discriminator were each independently re-derived from the current code and Scope Lock §2.4's own text, not re-accepted from the Completion Review's own account.

---

## Constitutional Verdict

```
ACCEPTED
```

No required correction. No Defect Confirmation Review is necessary.
