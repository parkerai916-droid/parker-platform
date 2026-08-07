**Status:** Planning Review only. No Kotlin is implemented, proposed as a diff, or changed by this document. Repository baseline independently re-verified below, not assumed from the task's own claim. This document does not amend any governance document.

# Authorization Purpose — Unit 4 (Permission Policy Integration) — Planning Review

## 0. Baseline, Independently Re-Verified

`git log --oneline -10`: `HEAD` is `b12e73f "feat: add Authorization Purpose carrier and registry"`, tracking `origin/main`, working tree clean of any Unit 1–3 artefact (`git status --short` shows only pre-existing, unrelated uncommitted work — Conversational Memory Admission and a Programme 3 clarification — none of it touched by this Unit). `AuthorizationPurposeId` (`src/contracts/Identifiers.kt:68`), `ExecutionRequest.authorizationPurpose` (`src/contracts/ExecutionRequest.kt:74`), and `AuthorizationPurposeRegistry`/`InMemoryAuthorizationPurposeRegistry` (`src/runtime/AuthorizationPurposeRegistry.kt`) all confirmed present and committed. The task's own claim "Units 1–3 are complete, reviewed, committed and pushed" is **confirmed true**, independently, not merely accepted.

---

## 1. Existing Permission Framework — Independently Reviewed

### 1.1 Where `DefaultPermissionPolicy` evaluates rules

`src/runtime/DefaultPermissionPolicy.kt`, `evaluate(request: ExecutionRequest): PermissionDecision`:

1. Resolves `request.targetResources` to `ResourceType`s via `resourceRegistry.resolve` (read-only).
2. Resolves `request.proposedActions` via `actionMapper.map(...)`, keeps only `ActionMappingResult.Resolved`, and flattens every resolved mapping's own `mappings: Set<ActionResourceMapping>` into one list — **`ActionMappingResult.Resolved.proposedAction` (the originating verb phrase) is discarded at this exact point** (`.flatMap { it.mappings }`, line 112–113).
3. If no mapping resolved at all → `deniedDecision(request)` (fail-closed default).
4. Otherwise, each resolved `(action, resourceType)` mapping is looked up in `ruleOutcomeFor(mapping)` (lines 122, 145–148): `rules.find { it.action == mapping.action && it.resourceType == mapping.resourceType }` — the **sole** matching key today is the `(PermissionAction, ResourceType)` pair. No rule matches → `DENIED`/`AUTOMATIC` (fail-closed default, same mechanism as step 3).
5. If a request's multiple proposed actions resolve to more than one `(action, resourceType)` pair, `evaluate` picks the single **most restrictive** `(outcome, level)` across all of them (`restrictiveness`, lines 129–131, 150–155) and returns exactly one `PermissionDecision` for the whole request — an already-accepted simplification (`IMPLEMENTATION_GAPS.md` #30) this Unit does not touch.

This is the entirety of where `DefaultPermissionPolicy` evaluates rules — one method, one matching step, one rule list, one engine.

### 1.2 Where verb-phrase discrimination currently occurs — checked, not assumed

**It does not currently occur anywhere in code.** This required independent correction of an assumption embedded in this task's own governing chain (see Section 2, below) — the task's own prompt asks to "identify where verb-phrase discrimination currently occurs," phrased as if it already exists; it does not.

- `PermissionPolicyRule` (`src/runtime/DefaultPermissionPolicy.kt:33–38`) carries no verb-phrase field — only `action: PermissionAction`, `resourceType: ResourceType`, `outcome`, `level`.
- `ruleOutcomeFor`'s own match predicate never inspects a verb phrase.
- `ActionMapper.mapOne` (`src/runtime/ActionMapper.kt:71–81`) *does* already produce `ActionMappingResult.Resolved(proposedAction, applicable)`, carrying the verb phrase — but `DefaultPermissionPolicy.evaluate`'s own `.flatMap { it.mappings }` throws it away a moment later, one line after receiving it.

### 1.3 Where Authorization Purpose should participate

The only place `DefaultPermissionPolicy` performs rule matching is `ruleOutcomeFor` (Section 1.1, step 4). Authorization Purpose participation belongs there, and only there: as a second, optional, additional matching dimension read directly from `request.authorizationPurpose` (already present on every request since Unit 2), consulted against Unit 3's own `AuthorizationPurposeRegistry.isActive(...)` to determine eligibility, exactly as the Implementation Plan's own Unit 4 section (`docs/implementation/AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN.md` §6) specifies.

---

## 2. A Genuine Discrepancy Found, and Resolved

The Authorization Purpose governance chain's own language, read literally, implies the verb-phrase discriminator is a **precondition already in place** for Unit 4 to extend "alongside":

- Carrier Contract Design §12: "`DefaultPermissionPolicy`'s own resolution step — **already extended once by the Adopted Policy Rule Collision Clarification** to optionally match on the specific verb phrase — gains a second, optional, additional matching dimension."
- Scope Lock §2.4: "the same 'extend the single, existing resolution step' pattern already used for the Policy Rule Collision Clarification's own verb-phrase discriminator."
- Implementation Plan §6 (Unit 4 Outputs): "an additional, optional matching dimension alongside the already-Adopted verb-phrase discriminator."

**Independently verified, and found materially misleading if read as "implemented in code":** `docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_POLICY_RULE_COLLISION_CLARIFICATION.md`'s own Status line states explicitly: *"No Kotlin is implemented, proposed as a diff, or changed by this document."* Its own Section 6 ("Required Follow-On Changes"), item 1: *"A Scope Lock and Implementation Plan for the `DefaultPermissionPolicy`/`PermissionPolicyRule` extension itself — **not authorised or begun by this document**."* "Adopted" describes the governance **mechanism selection** (a decision that verb-phrase-specific rules, if built, should take precedence over coarser ones) — it does not describe a Kotlin state. No code implementing it exists (Section 1.2, above, confirms this directly against the actual file).

**This is exactly the kind of discrepancy this task's own instruction — "do not assume the illustrative implementation locations are correct" — exists to catch**, generalised from file-location to a load-bearing factual premise about the codebase's own current state.

**Resolution, not left ambiguous:** the Collision Clarification document is itself titled "Gap #54 — Policy Rule Collision Clarification" and its own Section 6 names the verb-phrase mechanism's own Scope Lock/Implementation Plan as future Gap #54 work. This task's own explicit instruction lists **"modify Gap #54"** among Unit 4's non-responsibilities. Building the verb-phrase discriminator is therefore **doubly foreclosed** for this Unit — not authorised by the Collision Clarification itself, and separately excluded by this task's own stated scope. **Unit 4 implements only the Authorization Purpose matching dimension.** No verb-phrase field is added to `PermissionPolicyRule`; no verb phrase is read out of `ActionMappingResult.Resolved` by this Unit.

This narrows, rather than complicates, the Scope Lock §2.4 precedence-safety principle for this Unit's own purposes: with no verb-phrase discriminator in existence, "precedence between the verb-phrase and Authorization-Purpose discriminators" (Scope Lock §4 item 4) is not yet a live question — there is only one new dimension being added, against the one already-existing coarse `(action, resourceType)` dimension. The precedence-safety principle itself — *"a coarse `(action, resourceType)` rule may never resolve a request for which a more specific, Authorization-Purpose-aware rule was the one actually meant to govern it"* — is fully implemented as a two-tier precedence (Authorization-Purpose-aware rule over coarse rule) rather than a three-tier one. Should the verb-phrase discriminator ever be separately built (Gap #54's own future track), reconciling the two into one combined precedence order is that future work's own problem to solve, not silently pre-solved here.

---

## 3. Design for the Authorization Purpose Matching Dimension

### 3.1 `PermissionPolicyRule` extension

Add one new, defaulted field: `val authorizationPurpose: AuthorizationPurposeId? = null`. `null` means "no Authorization-Purpose requirement — matches any request regardless of what it carries," identical in spirit to the Collision Clarification's own "no discriminator = matches any verb phrase" precedent (§3.2 of that document) — the same general shape Scope Lock §2.4 itself names as reusable ("the same... pattern"), applied here to a different field, independently, since the field it was originally described against does not exist in code. Defaulted, trailing position — every existing positional and named construction of `PermissionPolicyRule` (`ParkerRuntime.kt`, `DefaultPermissionPolicyTest.kt`) continues to compile unchanged.

### 3.2 Matching precedence

For a resolved `(action, resourceType)` mapping:

1. If `request.authorizationPurpose` is non-null **and** `authorizationPurposeRegistry.isActive(request.authorizationPurpose)` is true, look for a rule matching `(action, resourceType, authorizationPurpose = request.authorizationPurpose)`. If found, it governs — unconditionally, regardless of whether a coarser rule for the same `(action, resourceType)` also exists, and regardless of whether the purpose-aware rule is more or less restrictive than that coarser rule. This is the literal implementation of the precedence-safety principle: a purpose-aware rule, once eligible to match, is never silently bypassed in favour of a coarser one.
2. Otherwise (no purpose supplied, purpose not currently active/registered, or no purpose-aware rule exists for this pair), fall back to the existing coarse match: a rule for `(action, resourceType, authorizationPurpose = null)`.
3. If neither matches, `DENIED`/`AUTOMATIC` — the existing, unchanged "no rule matches" default.

### 3.3 Fail-closed semantics for an absent/unregistered/retired value — derived, not guessed

Scope Lock §2.4: *"an absent or unregistered Authorization Purpose value denies by the same default every other unknown value already denies by."* Read together with the Completion Criteria's own explicit regression-freeness requirement (*"evaluates exactly as it does today for every request that does not populate the new field"*), this cannot mean "any request with a null/unregistered/retired purpose is denied outright regardless of rule matches" — every existing production caller leaves the field null today, and such a rule would deny every one of them the instant any purpose-aware rule existed anywhere in the table, which is neither stated nor implied, and would make the regression-freeness criterion unsatisfiable in general. The textually faithful, regression-consistent reading: an absent, unregistered, or retired value simply **cannot ever satisfy a purpose-aware rule match** — folding into Section 3.2 step 2 exactly like "no purpose supplied" — so it "denies by the same default every other unknown value already denies by" precisely in the sense that it feeds into the same, pre-existing "no rule matches → DENIED" mechanism (Section 1.1 step 4's existing default), not a new, separate, request-wide deny mechanism.

**A retired purpose is treated identically to an unregistered one** — Unit 3's own `isActive()` already returns `false` uniformly for both (`InMemoryAuthorizationPurposeRegistry.isActive`), matching Unit 3's own KDoc ("retirement... marks a value ineligible for new policy authorship"); Unit 4 needs, and uses, no other Unit 3 method.

### 3.4 Dependency wiring — additive, `ParkerRuntime.kt` untouched

`DefaultPermissionPolicy` gains one new, defaulted constructor parameter: `authorizationPurposeRegistry: AuthorizationPurposeRegistry? = null`. `ParkerRuntime.kt` constructs `DefaultPermissionPolicy` today (`ParkerRuntime.kt:507`) with three positional arguments; a defaulted trailing fourth parameter leaves that call site compiling, and behaving, identically — satisfying this task's own explicit "do not modify ParkerRuntime composition." When the registry is `null` (the composed runtime's own current state, unchanged by this Unit), every request behaves exactly as today: no purpose-aware rule can ever be supplied by `ParkerRuntime.kt`'s own currently-fixed rule list (Unit 4 adds no real domain rule, per its own non-responsibilities), so the new matching branch is structurally unreachable in production until a later, separately-authorised Unit wires both the registry and real rules. When a registry **is** supplied (as every new test in this Unit will do), `evaluate` calls `authorizationPurposeRegistry.isActive(...)` once, suspend, before rule matching — no nested lock, no repeated calls per mapping.

### 3.5 `PermissionEngine` interface

Untouched. `evaluate(request: ExecutionRequest): PermissionDecision`'s own signature is unchanged — `ExecutionRequest` already carries `authorizationPurpose` (Unit 2); no new parameter is needed on the interface itself. Satisfies Scope Lock §2.2/§2.4's "existing PermissionEngine interface" and "single Permission Engine" requirements directly.

---

## 4. Exact Scope for This Pass

**Modify:** `src/runtime/DefaultPermissionPolicy.kt` only — `PermissionPolicyRule` gains one defaulted field; `DefaultPermissionPolicy` gains one defaulted constructor parameter; `ruleOutcomeFor` (or its replacement) implements Section 3.2's precedence.

**Do not modify:** `PermissionEngine.kt`, `ActionMapper.kt`, `ActionMapping.kt`, `ExecutionRequest.kt`, `ParkerRuntime.kt`, `AuthorizationPurposeRegistry.kt`, `AuthorizationPurposeVocabulary.kt`, `Identifiers.kt`, any Gap #54/Knowledge Submission/Evidence Intelligence file.

**New tests:** `tests/runtime/DefaultPermissionPolicyTest.kt` (extended) or a new, dedicated test file — proving: (a) behaviour without Authorization Purpose is byte-for-byte unchanged (every existing test in that file continues to pass unmodified, plus new tests using the 3-arg construction form to confirm the defaulted registry parameter changes nothing); (b) a purpose-aware rule takes precedence over a coarser rule addressing the same pair, for a request declaring the matching, active purpose; (c) fail-closed: absent, unregistered, and retired purpose values each fail to satisfy a purpose-aware rule and fall back to whatever the coarse rule (or absence of one) dictates; (d) a coarse rule cannot silently resolve a request whose purpose matches a more specific rule (the precedence-safety guarantee, tested directly, not only inferred from (b)).

**No real domain value, no vocabulary registration, no `ParkerRuntime.kt` change, no Gap #54/Knowledge Submission/Evidence Intelligence/`memory.retrieve` policy content** — confirmed excluded by Section 4 above and re-checked against this task's own explicit "Do not" list before implementation begins.

---

## 5. Boundary Review

**Not genuinely required.** The one substantive ambiguity this Unit faced — how "fail-closed" and "regression-free" interact when a purpose-aware rule and an absent/invalid purpose value coexist — was resolved by direct textual derivation from already-frozen Scope Lock and Implementation Plan language (Section 3.3, above), not by an open design choice needing external adjudication. The verb-phrase discrepancy (Section 2) was a factual-premise correction, not a boundary requiring a decision between competing lawful designs — the Collision Clarification's own text and this task's own "do not modify Gap #54" instruction resolve it identically and without tension.

---

## 6. Non-Responsibilities Confirmed Unchanged

No Authorization Purpose value created. No vocabulary entry registered. No `ParkerRuntime.kt` change. No Gap #54 work (verb-phrase discriminator excluded, per Section 2). No Knowledge Submission, Evidence Intelligence, or `memory.retrieve` policy-content change. Unit 5 (Composition Wiring) and Unit 6 (End-to-End Verification) not begun.

```
UNIT 4 PLANNING REVIEW COMPLETE — PROCEEDING TO IMPLEMENTATION
```
