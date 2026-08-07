**Status:** Planning Review for Authorization Purpose Implementation Plan, Unit 2 (`ExecutionRequest` Carrier Extension) only. Governance and planning only — no Kotlin is written by this document. Does not amend the Implementation Plan or the Scope Lock. Nothing is staged, committed, or pushed.

# Authorization Purpose — Unit 2 (`ExecutionRequest` Carrier Extension) — Planning Review

## 1. Scope

Unit 2 only, per `docs/implementation/AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN.md` §4. Unit 1 (Value Type, `AuthorizationPurposeId`) is complete, committed, and treated as authoritative, unmodified. Units 3–6 are not begun.

---

## 2. Independent Verification — Where `ExecutionRequest` Is Actually Defined and Used

**Definition.** `src/contracts/ExecutionRequest.kt` — one `data class ExecutionRequest(...)`, thirteen fields, four already-optional/defaulted trailing fields (`sessionId: String? = null`, `riskEstimate: RiskEstimate? = null`, `expiresAt: Instant? = null`, `metadata: Map<String, String> = emptyMap()`), one `init` block enforcing three `require` checks (non-blank `intent`, non-blank `correlationId`, `expiresAt` after `createdAt` when present). **Exactly one constructor** — no secondary constructor, no companion factory function, confirmed by direct re-read of the full file.

**No builder pattern exists.** Grepped for `ExecutionRequestBuilder`/`fun buildExecutionRequest`/`fun executionRequest(` — the only matches are *private, per-class* helper methods (e.g. `PermissionFilteredMemoryRetrieval.buildExecutionRequest`, `EvidenceRegistrationCoordinator`'s own equivalent), each already using named arguments internally, none shared across classes. No factory to touch beyond the data class itself.

**Every production construction site, enumerated by direct grep, not assumed from the Implementation Plan's own illustrative list:**

`src/composition/ParkerRuntime.kt`, `src/composition/PermissionFilteredMemoryRetrieval.kt`, `src/composition/PermissionGatedMemoryCore.kt`, `src/runtime/DefaultEvidenceCustodian.kt`, `src/runtime/DefaultKnowledgeRetrieval.kt`, `src/runtime/DefaultKnowledgeSubmission.kt`, `src/runtime/DefaultOwnerEvidenceDeletionAuthority.kt`, `src/runtime/EvidenceIntelligenceAcceptanceCoordinator.kt`, `src/runtime/EvidenceIntelligenceInvocationGate.kt`, `src/runtime/EvidenceRegistrationCoordinator.kt`, `src/runtime/InMemoryAgentRuntime.kt`, `src/runtime/MemoryAdmissionCoordinator.kt`, `src/runtime/ResponseDelivery.kt` — **thirteen production sites**, a larger, more complete list than any prior governance document individually enumerated. Ten test files also construct `ExecutionRequest` directly.

**Positional-argument risk, checked directly, not assumed.** Spot-checked every production site's own construction call: all use named arguments (`paramName = value`) exclusively — confirmed for `ResponseDelivery.kt`, `InMemoryAgentRuntime.kt` (two separate construction sites in that one file), and, from this session's own prior direct reads, `PermissionFilteredMemoryRetrieval.kt`, `EvidenceRegistrationCoordinator.kt`, `DefaultKnowledgeRetrieval.kt`, `DefaultKnowledgeSubmission.kt`, `MemoryAdmissionCoordinator.kt`. **A new field's own position in the constructor is therefore irrelevant to every existing caller**, provided it carries a default value (making it optional).

**`copy()`, checked directly.** Grepped for `.copy(` used on any `ExecutionRequest`/`request` value across `src/` and `tests/` — **zero matches**. `ExecutionRequest.copy()` is never called anywhere today. Kotlin's own synthesised `copy()` will handle a new, defaulted field correctly without any code change; a dedicated test is still added (Section 5) as a forward-looking regression guard, since the Implementation Plan's own required test list names this explicitly.

**Propagation, re-confirmed.** `PermissionEngine.evaluate(request: ExecutionRequest): PermissionDecision` is the only place `ExecutionRequest` values are consumed by Trust Framework logic; every value travels by direct reference, unmodified, from construction to that one call — reconfirming Carrier Contract Design §6's own "automatic" propagation finding, independently, for this Unit.

---

## 3. Is the Implementation Plan's Own Illustrative File List Correct?

Implementation Plan §4 named `src/contracts/ExecutionRequest.kt`; `docs/schemas/ExecutionRequest.schema.json`; `docs/specifications/volume-01-core-contracts/ExecutionRequest.md`. **Confirmed correct and complete** — no better or additional location was found. Unlike Unit 1 (where the Implementation Plan's own file estimate was superseded), Unit 2's own estimate withstands independent verification exactly as written.

---

## 4. A Pre-Existing Drift Found in `ExecutionRequest.md`, Disclosed, Not Fixed

`docs/specifications/volume-01-core-contracts/ExecutionRequest.md`'s own "Required Fields" list currently includes `sessionId`, `riskEstimate`, `expiresAt`, and `metadata` — all four of which are actually optional/defaulted in both the Kotlin data class and the JSON schema's own `required` array (which excludes all four). This is a pre-existing inconsistency, not introduced by this Unit and not this Unit's own responsibility to correct (out of scope — Unit 2 adds one field; it does not audit or repair unrelated, already-existing documentation drift). **Consequence for this Unit's own work**: the new field must **not** be added to the "Required Fields" list under this same, already-questionable pattern, since Authorization Purpose is genuinely, deliberately optional (Scope Lock: "Authorization Purpose optional until later Units activate it") and adding it there would compound an existing inaccuracy rather than merely inherit one. A separate, clearly-labelled note is used instead (Section 6, below).

---

## 5. Exact Scope for This Pass

- **`src/contracts/ExecutionRequest.kt`** — add one new, optional, trailing field: `authorizationPurpose: AuthorizationPurposeId? = null`, placed after `metadata` (the last of the four existing optional fields), preserving the established "optional fields added incrementally at the end" pattern. No change to the `init` block — Authorization Purpose requires no construction-time validation beyond what `AuthorizationPurposeId`'s own constructor (Unit 1) already enforces.
- **`docs/schemas/ExecutionRequest.schema.json`** — add `authorizationPurpose` as a new, optional property (nullable string, `["string", "null"]`, mirroring `sessionId`/`expiresAt`'s own existing shape), **not** added to the `required` array.
- **`docs/specifications/volume-01-core-contracts/ExecutionRequest.md`** — a new, small, explicitly-optional note documenting the field, citing the Scope Lock, **not** added to "Required Fields" (Section 4, above).
- **`tests/contracts/ExecutionRequestTest.kt`** — extended, not replaced: add `authorizationPurpose` as a new optional parameter (default `null`) to the file's own existing private `request()` helper; add new test methods for optionality, preservation, equality, and `copy()` — reusing the file's own existing structure and style exactly. No new test file.

## 6. Non-Responsibilities Confirmed Unchanged

No change to `PermissionEngine`, `DefaultPermissionPolicy`, `ActionMapper`, any registry, or `ParkerRuntime.kt`'s own composition. No existing caller is modified to populate the new field. No policy-content decision, Gap #54 work, Knowledge Submission work, or Conversational Retrieval work is touched.

---

## 7. Boundary Review — Determined Not Genuinely Required

No genuine boundary ambiguity was found. The field's own home (`ExecutionRequest`), its own optional/nullable shape, and its own non-interaction with every other Trust Framework component are already fully fixed by Scope Lock §2.2 and confirmed, not merely assumed, by this Review's own independent trace (Section 2). **No Boundary Review performed.**

```
UNIT 2 PLANNING REVIEW COMPLETE — PROCEEDING TO IMPLEMENTATION
```
