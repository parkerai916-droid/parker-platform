**Status:** Genuine Independent Constitutional Review of Authorization Purpose Unit 2, performed as if by another reviewer, against the accepted Implementation Plan and Scope Lock directly, and against the actual, current file contents — not against the Completion Review's own account alone. This document does not amend the Implementation Plan, the Scope Lock, or any other governance document. Nothing is staged, committed, or pushed.

# Authorization Purpose — Unit 2 (`ExecutionRequest` Carrier Extension) — Independent Constitutional Review

## 1. Baseline and Diff-Shape Re-Verification

Independently re-ran `git diff --stat` — confirmed exactly four files touched by this Unit (`docs/schemas/ExecutionRequest.schema.json` +6, `docs/specifications/volume-01-core-contracts/ExecutionRequest.md` +3, `src/contracts/ExecutionRequest.kt` +12, `tests/contracts/ExecutionRequestTest.kt` +36), matching the Completion Review's own account exactly. Every other pending change in the working tree (`ParkerRuntime.kt` and the Conversational Memory Admission files) independently confirmed pre-existing and untouched by this Unit — none of it was opened or edited during this task. Independently re-ran the JSON schema through a parser: valid.

---

## 2. Challenge — Was the Planning Review's Own "Files Expected to Change" Claim Actually Complete? (Substantive Finding)

The Planning Review's own Section 3 stated the Implementation Plan's file list was "confirmed correct and complete." **Independently re-searched for every specification document mentioning `ExecutionRequest`**, more broadly than the Planning Review's own check: `find docs/specifications -iname "*ExecutionRequest*"` returns **three** files, not two — `docs/specifications/volume-01-core-contracts/ExecutionRequest.md` (updated by this Unit), `docs/schemas/ExecutionRequest.schema.json` (updated by this Unit), and **`docs/specifications/volume-02-core-schemas/ExecutionRequest-Schema.md` — not updated, and not named by the Implementation Plan, the Planning Review, or the Completion Review.**

Independently read this third document in full: it is a "human-readable summary" of the same JSON schema ("if the two ever disagree, the JSON Schema wins (ADR-019)"), carrying its own, separately-maintained "Required Fields" and "Optional Fields" lists — the latter currently reading "sessionId, riskEstimate, expiresAt, metadata," now stale, since `authorizationPurpose` was added to the actual JSON schema and to `ExecutionRequest.kt` without a corresponding update here. Unlike the Volume 1 prose document, this Volume 2 document's own Required/Optional split already correctly matches the JSON schema's own `required` array — it is the more accurate of the two prose documents, and leaving it stale is a genuine, newly-introduced inconsistency, not a pre-existing one being disclosed and left alone (contrast Planning Review §4, which correctly disclosed a *pre-existing* drift in the Volume 1 document without perpetuating it — this is different: a *new* drift, introduced by this Unit's own incomplete file search).

**This is not a breaking change** (adding an optional field, confirmed non-breaking per this same document's own "Versioning" section, which reserves version bumps/ADRs for breaking changes only) — so no schema version bump or ADR is required, only the "Optional Fields" list itself.

**Required correction:** add `authorizationPurpose` to `docs/specifications/volume-02-core-schemas/ExecutionRequest-Schema.md`'s own "Optional Fields" list, mirroring the Volume 1 document's own now-correct treatment.

---

## 3. Challenge — Accidental API Expansion Beyond One Field?

Independently re-read `src/contracts/ExecutionRequest.kt` in full: exactly one new field, one new KDoc paragraph, no other change to the class's own shape, `init` block, or any other declaration in the file. **Confirmed no expansion beyond what Unit 2 authorises.**

---

## 4. Challenge — Accidental `PermissionPolicy`/`PermissionEngine`/Runtime Wiring/Vocabulary Implementation?

Independently re-read `src/interfaces/PermissionEngine.kt` and `src/runtime/DefaultPermissionPolicy.kt` directly — both confirmed byte-for-byte unchanged (zero diff). Independently confirmed no new file was created for a registry or vocabulary mechanism. `git diff --stat` confirms `ParkerRuntime.kt`'s own diff pre-dates this Unit entirely (Conversational Memory Admission work), not touched by anything in this task. **Confirmed no accidental implementation of any later Unit.**

---

## 5. Challenge — Immutability, Backward Compatibility, Fail-Closed Preservation

Independently re-read the corrected `ExecutionRequest.kt`: the class remains a `val`-only data class with no mutator, ADR-018's own guarantee undisturbed. Independently re-checked every one of the thirteen production `ExecutionRequest` construction sites the Planning Review enumerated — `git diff` shows zero change to any of them, confirming complete backward compatibility, not merely asserted. Fail-closed behaviour is not this Unit's own responsibility to implement (Unit 4), and nothing here creates a path by which an absent field could be mistaken for an approval — the field is simply inert until Unit 4 exists, exactly as planned.

---

## 6. Challenge — Propagation Correctness and Hidden Boundary Crossing

Independently re-confirmed `PermissionEngine.evaluate(request: ExecutionRequest)` is the sole consumption point, unchanged; the new field travels by ordinary object reference, requiring no new propagation code, consistent with the Planning Review's own re-derivation of Carrier Contract Design §6's "automatic" finding. No boundary crossing into `DefaultPermissionPolicy`, `ActionMapper`, `ResourceRegistry`, or any registry was found anywhere in the diff.

---

## 7. Challenge — Test Design: Does the Synthetic Test Value Risk Being Mistaken for a Real, Registered Value?

Checked the literal string used in the new tests, `"test.example-purpose"` — prefixed `test.`, distinct in shape from every real domain-style example named elsewhere in this governance chain (`knowledge-memory.candidate-evaluation`, `evidence-intelligence.input-resolution`). **Confirmed sufficiently distinguishable**; no risk of being mistaken for a real, registered value or of constituting a policy-content decision.

---

## 8. Findings

**One required correction:** `docs/specifications/volume-02-core-schemas/ExecutionRequest-Schema.md` — a third specification document describing the same schema, missed by the Implementation Plan's own file list and by the Planning Review's own "complete" claim — must be updated to list `authorizationPurpose` under "Optional Fields," to avoid a newly-introduced (not merely pre-existing) documentation inconsistency.

No other required correction was found. API scope, absence of accidental Trust Framework/runtime/vocabulary implementation, immutability, backward compatibility, propagation, and test-value clarity were each independently re-derived from primary sources and found sound.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

One narrow, required correction (Section 2, above): update the Volume 2 schema summary document's own "Optional Fields" list. Proceeding to a Defect Confirmation Review after the correction is applied.

**Post-correction status:** the required correction was applied to `docs/specifications/volume-02-core-schemas/ExecutionRequest-Schema.md`. See `docs/reviews/AUTHORIZATION_PURPOSE_UNIT_2_DEFECT_CONFIRMATION_REVIEW.md` for the narrow Defect Confirmation Review, which found the correction complete, re-ran the full test suite successfully, and found no further defect. Authorization Purpose Unit 2 is accepted as of that review.
