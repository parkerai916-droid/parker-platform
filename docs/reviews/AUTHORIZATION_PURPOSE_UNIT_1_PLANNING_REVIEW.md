**Status:** Planning Review for Authorization Purpose Implementation Plan, Unit 1 (Authorization Purpose Value Type) only. Governance and planning only at this point — no Kotlin is written by this document. This review does not amend the Implementation Plan or the Scope Lock. Nothing is staged, committed, or pushed.

# Authorization Purpose — Unit 1 (Value Type) — Planning Review

## 1. Scope

Unit 1 only, per `docs/implementation/AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN.md` §3, exactly as accepted. Units 2–6 are not begun.

## 2. Re-Read Fresh

Implementation Plan §3 (Unit 1): Purpose, Authority (Scope Lock §2.2 — "a distinct, closed value type, never a raw `String`"), Outputs (a new, standalone value type, structurally analogous to `PrincipalId`/`ResourceId`), Files expected to change ("a contracts-tier file, alongside `src/contracts/Permission.kt`/`Resource.kt`'s own existing siblings" — a location estimate, not a freeze), Dependencies (none), Non-responsibilities (no `ExecutionRequest` change, no registry, no value content decided), Completion criteria, Stop condition.

## 3. Existing Precedent, Traced Fresh

`PrincipalId`/`ResourceId`/`RequestId`/`DecisionId`/`ResultId` are not actually siblings of `Permission.kt`/`Resource.kt` as the Implementation Plan's own Files field estimated — they are all defined together in one dedicated file, `src/contracts/Identifiers.kt`, each an identical `@JvmInline value class(val value: String)` with a single `require(value.isNotBlank())` constructor check, no per-type KDoc, sharing one file-level KDoc: "Typed identifiers for Parker's core contracts (Volume 1). Plain value classes (zero runtime overhead) rather than bare String everywhere, so e.g. a PrincipalId can't be accidentally passed where a ResourceId is expected." A dedicated test file, `tests/contracts/IdentifiersTest.kt`, exercises all five together: equality, inequality, and blank-rejection, in three shared test methods, not one file/method per type.

**This is a more precise, better-fitting location than the Implementation Plan's own estimate** (which named `Permission.kt`/`Resource.kt` as illustrative siblings, not a fixed requirement — Implementation Plan §3: "the specific file and type name are not fixed by this Plan"). Using the actual, established shared-identifiers file and its own established test file is more consistent with existing convention than creating a new file would be, and touches strictly fewer files.

## 4. Naming

Not frozen by any governance document, but used illustratively and consistently across the Carrier Contract Design, the Vocabulary Governance Contract Design, and the Implementation Plan itself: `AuthorizationPurposeId`. Adopted here as the actual name — an ordinary implementation-tier naming decision, not a retroactive governance freeze, since every governance document that used this name explicitly disclaimed it as illustrative only.

## 5. Boundary Review — Determined Not Genuinely Required

Checked against this task's own "only if genuinely required" instruction. A Boundary Review resolves genuine ambiguity about which existing architectural boundary a new capability crosses or belongs to. Unit 1 has none: Scope Lock §2.2 already fixes the shape ("closed, non-`String` value type"); Implementation Plan §3 already fixes zero dependencies and explicit non-responsibilities (no `ExecutionRequest`, no registry, no value content); the unit touches one existing, already-governed file pattern (`Identifiers.kt`) with no interaction with `PermissionEngine`, `DefaultPermissionPolicy`, `ActionMapper`, `ResourceRegistry`, or any other Trust Framework component. There is no boundary question left to resolve. **No Boundary Review performed.**

## 6. Exact Scope for This Pass

- Add one new value class, `AuthorizationPurposeId`, to `src/contracts/Identifiers.kt`, following the file's own established pattern exactly (blank-value rejection only — no naming-structure/namespace validation, per Implementation Plan §3's own explicit stop condition reserving that to Unit 3).
- Extend `tests/contracts/IdentifiersTest.kt`'s own three existing test methods to also cover `AuthorizationPurposeId`, mirroring the file's own established style exactly — no new test file.
- Touch nothing else. No `ExecutionRequest`, no `DefaultPermissionPolicy`, no composition root, no registry.

## 7. Confirmation Against Unit 1's Own Completion Criteria and Stop Condition

Completion criteria ("a distinct, non-`String` value type exists... with no other file depending on it yet") and stop condition (do not decide vocabulary content-validation rules) are both directly achievable by the scope in Section 6 above, with no discovered need to diverge.

```
UNIT 1 PLANNING REVIEW COMPLETE — PROCEEDING TO IMPLEMENTATION
```
