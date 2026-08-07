**Status:** Genuine Independent Constitutional Review of Authorization Purpose Unit 1, performed as if by another reviewer, against the accepted Implementation Plan and Scope Lock directly, and against the actual, current file contents — not against the Completion Review's own account alone. This document does not amend the Implementation Plan, the Scope Lock, or any other governance document. Nothing is staged, committed, or pushed.

# Authorization Purpose — Unit 1 (Value Type) — Independent Constitutional Review

## 1. Baseline and Diff-Shape Re-Verification

Independently re-ran `git diff --stat` — confirmed exactly two files changed by this Unit (`src/contracts/Identifiers.kt`, +16; `tests/contracts/IdentifiersTest.kt`, +4), matching the Completion Review's own account exactly. Confirmed, via `git status --short`, every other pending change in the working tree predates this Unit and is untouched by it.

---

## 2. Challenge — Does the Implementation Conform to Unit 1's Own Frozen Specification?

Independently re-read Implementation Plan §3 and Scope Lock §2.2 directly, then independently re-read `src/contracts/Identifiers.kt`'s own current content. `AuthorizationPurposeId` is a `@JvmInline value class(val value: String)` with a single `require(value.isNotBlank())` check — structurally identical to `PrincipalId`/`ResourceId`, satisfying "distinct, closed value type, never a raw `String`" exactly. No other file was touched; `ExecutionRequest`, `PermissionEngine`, `DefaultPermissionPolicy`, and `ParkerRuntime.kt` are confirmed untouched by direct re-check of the diff. **Confirmed conformant.**

---

## 3. Challenge — Was the File-Location Deviation From the Implementation Plan's Own Estimate Justified?

The Implementation Plan's own Files field named `Permission.kt`/`Resource.kt` as illustrative siblings; the Planning Review instead used `Identifiers.kt`, the actual shared file. Independently re-read `Identifiers.kt` directly — confirmed it is, in fact, where `PrincipalId`/`ResourceId`/`RequestId`/`DecisionId`/`ResultId` are all actually defined, not `Permission.kt`/`Resource.kt`. **Confirmed the deviation is a correction toward greater accuracy, not a departure from intent** — the Implementation Plan's own text explicitly called its file estimate illustrative ("the specific file and type name are not fixed by this Plan"), so using the more precise, actually-correct location is compliant, not a violation.

---

## 4. Challenge — Is Deferring All Naming/Content Validation to Unit 3 Actually Correct, Given a Closer Precedent Exists That Does Otherwise? (Substantive Finding)

Pressed directly, since Unit 1's own stop condition explicitly defers "whether a value's own string must match the naming convention" to Unit 3, and this deferral deserves scrutiny beyond restating it.

**Independently searched for closer conceptual precedents than `PrincipalId`/`ResourceId`** (both freely-assigned instance identifiers, unlike Authorization Purpose's own closed-vocabulary, namespaced nature) — found `EventType` (`src/contracts/EventContracts.kt`), read directly: `@JvmInline value class EventType(val value: String)`, whose own constructor enforces **both** a non-blank check **and** a namespace-structure check: `require('.' in value) { "EventType '$value' must be namespaced as <domain>.<event> or plugin:<pluginId>.<event>" }`. `EventType` is a materially closer structural analogue to Authorization Purpose than `PrincipalId`/`ResourceId` are — a namespaced, closed-vocabulary-style value, not a freely-assigned instance identifier — and it embeds its own naming-structure validation directly in the value-class constructor, contrary to the separation Unit 1/Unit 3 (value type vs. registry) currently plan.

**This does not mean Unit 1's own implementation is wrong.** The Implementation Plan is already-accepted, frozen governance for this build phase, and its own stop condition explicitly, deliberately reserves naming validation to Unit 3 — a considered separation-of-concerns choice (value type vs. registry), not an oversight, and this task's own instruction is to follow that Plan exactly, not to substitute a different pattern found elsewhere in the codebase merely because it exists. **Correctly, Unit 1 did not embed namespace validation, and must not be changed to do so now.**

**What is missing is disclosure, not correctness**: neither the KDoc nor the Completion Review records that a closer, contrary precedent (`EventType`) exists and was found, for the benefit of whoever designs Unit 3 next — who will need to decide, with this precedent in view, whether Authorization Purpose's own naming validation should also move into the value-class constructor (mirroring `EventType`) or remain a separate, registry-level concern (the current plan). Leaving this undisclosed risks the same "lost, re-derived, or improvised later" outcome this Programme's own governance chain has repeatedly taken care to avoid.

**Required correction:** add a brief, disclosed note — in `AuthorizationPurposeId`'s own KDoc and/or the Completion Review — recording the `EventType` precedent as a consideration for Unit 3's own future design, without changing Unit 1's own implementation.

---

## 5. Challenge — Is `AuthorizationPurposeId`'s Own Name Well-Chosen?

Checked directly: `EventType`'s own naming convention (no "-Id" suffix, since it classifies a *kind* of thing, not a specific instance) is arguably a marginally closer fit for a namespaced, closed-vocabulary value than the "-Id" suffix `PrincipalId`/`ResourceId` use for freely-assigned instance identifiers. However, `AuthorizationPurposeId` was used consistently and repeatedly, as the same illustrative name, across four prior governance documents in this chain (Carrier Contract Design, Vocabulary Governance Contract Design, the Scope Lock's own citations of the Carrier Contract Design, and the Implementation Plan itself) — a strong, repeated signal within this Programme's own paper trail that outweighs a marginal, cross-codebase stylistic preference. **Not a required correction** — the chosen name remains well-justified and consistent with its own governance chain's own established usage.

---

## 6. Challenge — Does Placing This Type in a File Whose Own KDoc Says "Volume 1 Core Contracts" Overclaim Specification Status?

Checked directly: `Identifiers.kt`'s own file-level KDoc scopes itself to "Volume 1 core contracts," and Authorization Purpose has no corresponding `docs/specifications/volume-01-core-contracts/` entry yet (that remains Unit 2's own, later responsibility, when the field is actually added to `ExecutionRequest`). Checked whether this constitutes overclaiming: `AuthorizationPurposeId`'s own new, per-type KDoc cites its actual authority precisely (`AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` §2.2), never claiming Volume-1-specification status for itself. **Confirmed no overclaim** — the type-level KDoc's own precise citation is sufficient disclosure; no correction required.

---

## 7. Challenge — Full Repository Verification, Independently Re-Confirmed

Independently re-ran `./gradlew clean test` — confirmed `BUILD SUCCESSFUL`, 1938 tests, 0 failures, 5 pre-existing skips, matching the Completion Review's own reported figures exactly.

---

## 8. Findings

**One required correction:** disclose the `EventType` precedent (namespace validation embedded in the value-class constructor, contrary to Unit 1/Unit 3's own current separation of concerns) as a forward-looking note for Unit 3's own future design, without altering Unit 1's own implementation, which correctly follows the already-accepted Implementation Plan's own explicit deferral.

No other required correction was found. Conformance to Unit 1's own specification, the justified file-location correction, the naming choice, and the "Volume 1 core contracts" scope question were each independently re-derived from primary sources and found sound.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

One narrow, required correction (Section 4, above): disclose the `EventType` precedent for Unit 3's own future benefit. Proceeding to a Defect Confirmation Review after the correction is applied.

**Post-correction status:** the required correction was applied to `src/contracts/Identifiers.kt`'s own `AuthorizationPurposeId` KDoc. See `docs/reviews/AUTHORIZATION_PURPOSE_UNIT_1_DEFECT_CONFIRMATION_REVIEW.md` for the narrow Defect Confirmation Review, which found the correction complete, recompiled and re-ran the full test suite successfully, and found no further defect. Authorization Purpose Unit 1 is accepted as of that review.
