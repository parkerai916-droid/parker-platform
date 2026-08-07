**Status:** Planning Review for Authorization Purpose Implementation Plan, Unit 3 (Vocabulary Registry) only. Governance and planning only — no Kotlin is written by this document. Does not amend the Implementation Plan or the Scope Lock. Nothing is staged, committed, or pushed.

# Authorization Purpose — Unit 3 (Vocabulary Registry) — Planning Review

## 1. Scope and Baseline

Unit 3 only, per `docs/implementation/AUTHORIZATION_PURPOSE_IMPLEMENTATION_PLAN.md` §5. Units 1–2 are complete and accepted (Unit 1: `AuthorizationPurposeId`; Unit 2: `ExecutionRequest.authorizationPurpose`). `git log` confirms `HEAD` at `493b69a` (Unit 1 committed); Unit 2's own work remains uncommitted in the working tree, consistent with this session's own established pattern of the user committing accepted work at their own discretion between tasks — not a discrepancy requiring action. Units 4–6 are not begun.

---

## 2. Independent Verification — Existing Action Vocabulary Implementation

Re-read `src/runtime/ActionMapper.kt` and `src/contracts/ActionMapping.kt` fresh, in full.

- **Split convention, confirmed precisely**: data/result *contracts* (`ActionResourceMapping`, `ActionVocabularyEntry`, `ActionMappingFailureReason`, `ActionMappingResult`, `VocabularyRegistrationOutcome`) live in `src/contracts/ActionMapping.kt`; the *interface and implementation* (`ActionVocabulary`, `InMemoryActionVocabulary`, `ActionMapper`) live in `src/runtime/ActionMapper.kt`. This is a two-file split, not the Implementation Plan's own illustrative single "new runtime-tier file."
- **Registration lifecycle**: `InMemoryActionVocabulary.register(entry)` — `mutex.withLock`, keyed by `verbPhrase` in a `mutableMapOf`. Unregistered key → insert, `Registered`. Same key, *identical* entry → `AlreadyRegistered` (idempotent no-op). Same key, *different* entry → `Rejected(reason)`. **No `update`/`unregister`/`deprecate` method exists.**
- **Duplicate detection**: structural equality of the whole `ActionVocabularyEntry` value (Kotlin data class `==`), not a separate conflict-detection field.
- **Namespacing**: `action-mapping.md`'s own "Plugin Supplied Actions": `plugin:<pluginId>:<action>` — colon-separated, confirmed by direct citation (`plugin:<pluginId>:run irrigation cycle`). This is the syntax **already frozen** by `docs/governance/AUTHORIZATION_PURPOSE_VOCABULARY_GOVERNANCE_CONTRACT_DESIGN.md` §12 for Authorization Purpose specifically: `<domain>.<purpose>` for core values, `plugin:<pluginId>:<purpose>` for Plugin-supplied ones.
- **A second, different, already-existing convention was found and deliberately not followed**: `EventType` (`src/contracts/EventContracts.kt`, re-read fresh) uses `plugin:<pluginId>.<event>` — a **dot** after the Plugin ID, not a second colon, per its own governing `EventType.md`. This confirms two mutually-inconsistent plugin-namespace conventions already coexist in this codebase, each correctly following its own governing specification. **Authorization Purpose follows Vocabulary Governance Contract Design §12's own already-frozen choice (colon-separated, matching Action Vocabulary) — not `EventType`'s own different one** — since §12 is itself already-Adopted governance directly on point, sourced from `action-mapping.md` specifically, not a stylistic preference this Unit is free to pick either way.
- **Plugin registration**: no separate method — Plugins call the *same* `register` function core code uses; differentiation is by namespace convention alone, never a distinct code path. Mirrored here identically (Section 5).
- **Retirement handling**: **does not exist for Action Vocabulary at all** — confirmed by the complete absence of any such method. Authorization Purpose's own retirement requirement (Scope Lock §2.3) has no existing Action Vocabulary precedent to mirror; it is modelled instead on Contract Design V2 §3's "retirement never implies deletion" philosophy, already applied by `docs/governance/AUTHORIZATION_PURPOSE_VOCABULARY_GOVERNANCE_CONTRACT_DESIGN.md` §8/§17 to this exact vocabulary.
- **Naming validation**: Action Vocabulary itself performs **none** — `ActionVocabularyEntry`'s own `init` only checks non-blank. Authorization Purpose's own registration-time naming validation (Unit 1's own deferred responsibility, Scope Lock Deferred Decision 7) has no Action Vocabulary precedent either; `EventType`'s own constructor-embedded `require('.' in value)` is the closest *style* precedent for "validate a namespace shape with a `require`," even though its own *location* (constructor vs. registry) and *exact syntax* (dot vs. colon for Plugins) both differ from what this Unit must build.
- **Composition-time registration model**: confirmed, by direct re-read of `ParkerRuntime.kt`, that every `ActionVocabulary`/`ResourceRegistry` entry is registered exactly once, during `ParkerRuntime.kt`'s own `start()` sequence — never at any other time, and never conditionally. This Unit's own registry is built to support the identical pattern (Unit 5's own later responsibility to actually call it there).

---

## 3. Is the Implementation Plan's Own Illustrative File Location Correct?

**Not quite — refined, with disclosure, mirroring Unit 1's own precedent.** The Implementation Plan named "a new runtime-tier file, alongside `src/runtime/ActionMapper.kt`'s own `InMemoryActionVocabulary` sibling," implying one new file. Section 2's own fresh trace shows the *actual*, established convention is a **two-file split**: contracts in `src/contracts/`, interface+implementation in `src/runtime/`. Using two files, mirroring this exactly, is more consistent with existing convention than combining everything into one file would be — the same kind of location refinement Unit 1 made (`Identifiers.kt` over the Plan's own `Permission.kt`/`Resource.kt` guess), not a departure from intent.

**Chosen locations:**
- `src/contracts/AuthorizationPurposeVocabulary.kt` — `AuthorizationPurposeStatus` (enum: `ACTIVE`, `RETIRED`), `AuthorizationPurposeEntry` (data class: `id`, `status`), `AuthorizationPurposeRegistrationOutcome` and `AuthorizationPurposeRetirementOutcome` (sealed classes, mirroring `VocabularyRegistrationOutcome`'s own three-variant shape).
- `src/runtime/AuthorizationPurposeRegistry.kt` — the `AuthorizationPurposeRegistry` interface and its `InMemoryAuthorizationPurposeRegistry` implementation, mirroring `ActionVocabulary`/`InMemoryActionVocabulary`'s own shape, `Mutex`-protected, `suspend fun`-based.

---

## 4. Design Decisions Made, and Why

- **No "content conflict" case, unlike Action Vocabulary.** `ActionVocabularyEntry` carries a payload (`mappings`) that can genuinely differ between two registrations of the same key; `AuthorizationPurposeId` carries no separate payload — the identifier *is* the value. Re-registering an already-**active** id is therefore an idempotent no-op (`AlreadyRegistered`), mirroring Action Vocabulary's own identical-entry case exactly.
- **Re-registering an already-*retired* id is rejected, not silently reactivated.** Not decided by any prior document verbatim, but a direct, necessary consequence of Scope Lock §2.3's own "immutable identifiers... a value's own meaning cannot change" and "retirement without deletion": treating retirement as reversible via ordinary re-registration would make it not really retirement. `register()` therefore checks status, not merely presence.
- **Namespace validation lives in the registry's own `register()` function, not in `AuthorizationPurposeId`'s own constructor.** Required directly by Unit 1's own already-accepted stop condition and this Unit's own "Outputs" (Implementation Plan §5).
- **No runtime access-control mechanism on the registry itself.** Checked against Unit 3's own stop condition ("who is authorised to call its own registration function at runtime... stop and report"). Confirmed this is **not** triggered: `ActionVocabulary`/`InMemoryActionVocabulary` has no access-control mechanism of its own either — access is governed entirely by who holds a reference to the instance, decided at composition time (Unit 5's own, later responsibility), never by the registry class itself. Building this registry with the identical, already-accepted shape does not newly raise the question Unit 3's own stop condition anticipates; it is answered the same way it already is for every other registry in this codebase.
- **A Plugin's own "never exceeding what its own Principal could ever be granted" ceiling (Scope Lock §2.3) is a governance-process discipline, not a runtime check this registry performs.** The registry has no dependency on `PermissionEngine`/`Principal` (explicitly out of this Unit's own scope) and cannot evaluate what a Principal could be granted. This ceiling is enforced the same way it already is for Action Vocabulary's own identical Plugin ceiling (`action-mapping.md`) — by whoever reviews and accepts a Plugin's own registration request at the governance tier, not by code.

---

## 5. Exact Scope for This Pass

- `src/contracts/AuthorizationPurposeVocabulary.kt` (new) — `AuthorizationPurposeStatus`, `AuthorizationPurposeEntry`, `AuthorizationPurposeRegistrationOutcome`, `AuthorizationPurposeRetirementOutcome`.
- `src/runtime/AuthorizationPurposeRegistry.kt` (new) — `AuthorizationPurposeRegistry` interface (`register`, `retire`, `lookup`, `isActive`); `InMemoryAuthorizationPurposeRegistry` implementation, `Mutex`-protected, reject-on-conflict, namespace-validating at registration time.
- A dedicated test file, mirroring the file split: `tests/runtime/AuthorizationPurposeRegistryTest.kt` — no existing test file to reuse, since no prior test file covers vocabulary-shaped registries other than `ActionMapperTest`/equivalent, which this Unit does not touch (Non-responsibilities: "does not touch `ActionVocabulary` itself").
- Touch nothing else. No `DefaultPermissionPolicy`, no `ParkerRuntime.kt`, no `ExecutionRequest`, no real domain value.

## 6. Non-Responsibilities Confirmed Unchanged

No change to `PermissionEngine`, `DefaultPermissionPolicy`, `ActionMapper`/`ActionVocabulary`, `ExecutionRequest`, or `ParkerRuntime.kt`'s own composition. No real domain's own Authorization Purpose value is registered anywhere. No Gap #54, Knowledge Submission, or Conversational Retrieval work.

---

## 7. Boundary Review — Determined Not Genuinely Required

No genuine boundary ambiguity was found. The registry's own shape, its own separation from `ActionVocabulary`, its own namespace syntax, and its own retirement semantics are all already fixed by Scope Lock §2.3 and Vocabulary Governance Contract Design §5–§17, confirmed rather than merely assumed by this Review's own independent trace (Section 2). The one genuine open question this Unit's own stop condition anticipated (registration access control) was checked directly and found already answered by existing precedent (Section 4), not a new boundary requiring escalation. **No Boundary Review performed.**

```
UNIT 3 PLANNING REVIEW COMPLETE — PROCEEDING TO IMPLEMENTATION
```
