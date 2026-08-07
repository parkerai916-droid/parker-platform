**Status:** Implementation Plan. First implementation-tier document for Authorization Purpose. No Kotlin is implemented, proposed as a diff, or changed by this document. No test is written or modified. This document does not amend `docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` ("the Scope Lock," Adopted) or any of the four governance documents it freezes — it translates their already-frozen decisions into a build sequence, redesigning nothing. It does not implement Gap #54, Knowledge Submission, or Conversational Retrieval, and it does not make any Authorization Purpose policy-content decision. Nothing is staged, committed, or pushed.

# Trust Framework — Authorization Purpose — Implementation Plan

Programme: **Trust Framework Authorization Purpose Programme — build phase, covering the mechanism-level portion of Programme Units 1–3** (Carrier, Vocabulary Governance, Permission Policy Extension) **as already frozen by the Scope Lock — excluding Programme Unit 4 (Existing Consumer Retrofit) and Unit 5 (Gap #54 Policy-Content Resolution), neither of which this document authorises or begins.**

---

## 1. Purpose

Translate the Scope Lock's own frozen objectives (§2.1–2.6) into the smallest lawful sequence of implementation units, each independently completable and independently verifiable, without redesigning any frozen decision and without freezing any Kotlin API this Programme's own governance chain deliberately left open (Scope Lock §3).

---

## 2. Deriving the Unit Sequence Independently From the Scope Lock

The suggested sequence in this task's own prompt (value type → `ExecutionRequest` extension → vocabulary registration → policy integration → composition wiring → runtime propagation → verification → migration → adoption by Memory Retrieval → runtime integration) is **not assumed correct** and is checked line by line against the Scope Lock directly:

- **"Runtime propagation" is not a separate unit.** Carrier Contract Design §6 (cited by Scope Lock §2.2) found propagation "automatic — travels with the request through every layer already reading `ExecutionRequest`" once the field exists. There is nothing to separately build; it is a consequence of the `ExecutionRequest` extension unit, not its own unit.
- **"Migration" is not a unit this Plan authorises.** Scope Lock §3, restating Vocabulary Governance Contract Design §15 directly: "Migration code or a migration mechanism... none is authorised or needed until a genuinely breaking change is proposed." Every unit below is additive (a new optional field, a new registry, a new optional policy-matching dimension) — none proposes a breaking change, so no migration unit is derived.
- **"Adoption by Memory Retrieval" is not a unit this Plan builds.** It is Programme Unit 4 (Existing Consumer Retrofit) and touches Scope Lock Deferred Decision 8 — assigning real Authorization Purpose values to `DefaultKnowledgeCandidateEvaluator`/`EvidenceIntelligenceInputResolver`. This task's own explicit exclusions ("do not implement Gap #54... Knowledge Submission... Conversational Retrieval") place this outside this Plan's own boundary. It is named in Section 8 ("Relationship to Future Work") as a forward pointer only, not scheduled here.
- **"Runtime integration" duplicates "Composition wiring".** No separate concern was found in the Scope Lock distinguishing the two; they are merged into one unit (Unit 5, below).
- **A unit the suggested sequence omits, but the Scope Lock requires: end-to-end Verification, scoped narrowly enough to avoid becoming Gap #54 implementation.** Retained as its own unit (Unit 6) precisely because it must prove the assembled mechanism honestly, using a synthetic registration, never a real domain's own retrofit (Section 6, Unit 6, below, states this constraint explicitly).

**Independently derived sequence, six units:**

1. Authorization Purpose Value Type
2. `ExecutionRequest` Carrier Extension
3. Vocabulary Registry
4. Permission Policy Integration
5. Composition Wiring
6. End-to-End Verification

**Dependency graph:**

```
Unit 1 (Value Type)
    |
    +---------------------+
    v                      v
Unit 2 (ExecutionRequest)  Unit 3 (Vocabulary Registry)
    |                      |
    +----------+-----------+
               v
      Unit 4 (Permission Policy Integration)
               |
               v
      Unit 5 (Composition Wiring)
               |
               v
      Unit 6 (End-to-End Verification)
```

Unit 4 depends on **both** Unit 2 and Unit 3 — `DefaultPermissionPolicy` cannot correctly implement the Scope Lock §2.4 fail-closed default ("an absent or unregistered Authorization Purpose value denies") without a way to read the value off `ExecutionRequest` (Unit 2) *and* a way to check whether it is registered (Unit 3). Units 2 and 3 do not depend on each other and may proceed in either order once Unit 1 exists.

---

## 3. Unit 1 — Authorization Purpose Value Type

**Purpose.** Define the closed, distinct Kotlin value type Authorization Purpose values are held in.

**Authority.** Scope Lock §2.2: "the eventual value type must be a distinct, closed value type, never a raw `String` directly on `ExecutionRequest`" (Carrier Contract Design §7, §19 Risk 1).

**Inputs.** None beyond the Scope Lock's own constraint.

**Outputs.** A new, standalone value type, structurally analogous to `PrincipalId`/`ResourceId`'s own existing `@JvmInline value class` pattern (cited as precedent, not frozen as the required shape — the exact Kotlin shape remains Scope Lock Deferred Decision 1).

**Files expected to change.** A contracts-tier file, alongside `src/contracts/Permission.kt`/`src/contracts/Resource.kt`'s own existing siblings — the specific file and type name are not fixed by this Plan (no Kotlin API freezing).

**Dependencies.** None — this is the sequence's own root unit.

**Non-responsibilities.** Does not touch `ExecutionRequest` (Unit 2); does not build a registry (Unit 3); does not decide what any specific value's own string content will be (that is domain-owned, Vocabulary Governance Contract Design §5, and out of this Plan's scope entirely).

**Completion criteria.** A distinct, non-`String` value type exists, satisfying Scope Lock §2.2's own constraint, with no other file depending on it yet.

**Stop condition.** If building this type reveals a need to also decide the vocabulary's own content-validation rules (e.g., whether a value's own string must match the naming convention), stop — that is Unit 3's own responsibility (Vocabulary Governance Contract Design §12), not this unit's.

---

## 4. Unit 2 — `ExecutionRequest` Carrier Extension

**Purpose.** Add Authorization Purpose to `ExecutionRequest` as a new, optional field.

**Authority.** Scope Lock §2.2: "Authorization Purpose is carried by `ExecutionRequest`. It is part of the request itself" (Carrier Contract Design §7, §10).

**Inputs.** Unit 1's own value type.

**Outputs.** `ExecutionRequest` gains one new, optional field of Unit 1's own type. `PermissionEngine.evaluate`'s own public signature is **not** touched (Scope Lock §2.2: "`PermissionEngine` remains unchanged as the single authority").

**Files expected to change.** `src/contracts/ExecutionRequest.kt`; `docs/schemas/ExecutionRequest.schema.json`; `docs/specifications/volume-01-core-contracts/ExecutionRequest.md` (Carrier Contract Design §18's own migration considerations, cited by Scope Lock §3).

**Dependencies.** Unit 1.

**Non-responsibilities.** Does not populate the new field for any existing caller (that is Programme Unit 4, out of scope); does not change any existing caller's own behaviour, since the field is optional/defaulted (Carrier Contract Design §18: "every existing caller that never sets it behaves exactly as today"); does not touch `PermissionEngine`'s or `DefaultPermissionPolicy`'s own logic (Unit 4).

**Completion criteria.** `ExecutionRequest` compiles with the new field; every existing production caller continues to compile and behave unchanged without modification; the schema and specification document accurately describe the new field.

**Stop condition.** If adding this field reveals that any existing caller's own behaviour changes without that caller being deliberately modified, stop — the field was not implemented as additive/optional, contradicting Scope Lock §2.2 and Carrier Contract Design §18 directly.

---

## 5. Unit 3 — Vocabulary Registry

**Purpose.** Build the registration mechanism the closed Authorization Purpose vocabulary is governed by.

**Authority.** Scope Lock §2.3, in full — closed vocabulary, domain ownership, composition-time registration, namespacing, immutable identifiers, retirement without deletion, Plugin governance, reject-on-conflict registration.

**Inputs.** Unit 1's own value type.

**Outputs.** A registration/lookup capability, analogous to but administratively separate from `InMemoryActionVocabulary` (Vocabulary Governance Contract Design §6, §13): registration is additive and reject-on-conflict (Scope Lock §2.3); a registered value's own meaning is immutable (§2.3); a mechanism exists to mark a value ineligible for new policy authorship without deleting it (retirement, §2.3); registration-time naming-structure validation exists (Scope Lock Deferred Decision 7, resolved here as part of this unit's own scope, since Vocabulary Governance Contract Design §12 recommended exactly this be built rather than left to convention alone).

**Files expected to change.** A new runtime-tier file, alongside `src/runtime/ActionMapper.kt`'s own `InMemoryActionVocabulary` sibling — exact file/class name not fixed by this Plan.

**Dependencies.** Unit 1.

**Non-responsibilities.** Does not register any actual domain value (Programme Unit 4, out of scope); does not decide whether the vocabulary becomes a formal `docs/schemas/`-tier artifact (Scope Lock Deferred Decision 6, left open); does not touch `ActionVocabulary` itself (Vocabulary Governance Contract Design §13: "parallel and orthogonal," administratively separate).

**Completion criteria.** A registration function exists that is additive, reject-on-conflict, validates naming structure at registration time, and supports marking a value retired without deleting it; a lookup function exists that `DefaultPermissionPolicy` (Unit 4) can use to determine whether a value is currently registered and eligible for new policy authorship.

**Stop condition.** If building this registry reveals a need to decide *who* is authorised to call its own registration function at runtime (an access-control question on the registry itself, distinct from the vocabulary's own content governance), stop and report — this is a new question the Scope Lock does not address and would require its own, narrow governance pass before proceeding.

---

## 6. Unit 4 — Permission Policy Integration

**Purpose.** Extend `DefaultPermissionPolicy`'s own resolution step so Authorization Purpose participates in evaluation, satisfying the Scope Lock's own frozen fail-closed and precedence-safety requirements.

**Authority.** Scope Lock §2.4, in full — single Permission Engine/Policy, fail-closed evaluation, Authorization Purpose participates in evaluation, no caller-specific exceptions, no second authorization system, precedence safety.

**Inputs.** Unit 2's own field (to read); Unit 3's own lookup capability (to check registration/eligibility).

**Outputs.** `DefaultPermissionPolicy.evaluate`'s own resolution step additionally consults the Authorization Purpose field, where present, as an optional, additional matching dimension alongside the already-Adopted verb-phrase discriminator (Carrier Contract Design §12) — implementing a precedence rule that satisfies Scope Lock §2.4's own frozen outcome constraint: a coarse `(action, resourceType)` rule may never resolve a request a more specific, Authorization-Purpose-aware rule was meant to govern; ambiguity defaults to deny. An absent or unregistered value denies by the same default every other unknown value already denies by.

**Files expected to change.** `src/runtime/DefaultPermissionPolicy.kt`.

**Dependencies.** Unit 2, Unit 3.

**Non-responsibilities.** Does not change `PermissionEngine`'s own public interface (Scope Lock §2.2); does not add any `PermissionPolicyRule` content for any real domain (that is the still-undecided policy-content question, Scope Lock Deferred Decision 9 — explicitly not this unit's, or this Plan's, responsibility); does not decide the exact precedence *algorithm*'s own internal data structure beyond satisfying the frozen outcome constraint (Scope Lock §2.4/§4 item 4 — the algorithm itself remains open, only its required behaviour is fixed).

**Completion criteria.** `DefaultPermissionPolicy` compiles and evaluates exactly as it does today for every request that does not populate the new field (regression-free); for a request that does populate it, evaluation is deterministic, fails closed for an absent/unregistered/ineligible value, and never lets a coarse rule resolve a request a more specific rule was meant to govern.

**Stop condition.** If satisfying the precedence-safety constraint reveals that the only workable design requires a second policy-evaluation pass, a second rule table, or any structure resembling a second authority, stop — Scope Lock §2.4 forecloses this directly ("no second authorization system"), and the design must be reconsidered within the single, existing resolution step, not escalated into a new one.

---

## 7. Unit 5 — Composition Wiring

**Purpose.** Wire Unit 3's own registry and Unit 4's own extended policy together at the runtime's own composition root.

**Authority.** Scope Lock §2.3 ("composition-time registration... at the same composition stage `ActionVocabulary`/`ResourceRegistry` entries already are").

**Inputs.** Unit 3's own registry; Unit 4's own extended `DefaultPermissionPolicy`.

**Outputs.** The registry is constructed once, at the same composition stage as `ActionVocabulary`/`ResourceRegistry`, and supplied to wherever Unit 4's own extended policy needs it. **No domain-specific Authorization Purpose value is registered by this unit** — this wires the infrastructure only, exactly as Unit 3/Unit 4 built it, without adopting it for any real consumer.

**Files expected to change.** `src/composition/ParkerRuntime.kt`.

**Dependencies.** Unit 4 (and transitively, Units 2 and 3).

**Non-responsibilities.** Does not register `DefaultKnowledgeCandidateEvaluator`'s, `EvidenceIntelligenceInputResolver`'s, or any other existing consumer's own Authorization Purpose value (Programme Unit 4, out of scope); does not add any new `PermissionPolicyRule`.

**Completion criteria.** `ParkerRuntime.kt` constructs the registry and wires it to the extended policy; the full existing test suite continues to pass unchanged, since no existing caller populates the new field and no new rule content exists for anything to match against.

**Stop condition.** If wiring reveals that the registry or the extended policy cannot be constructed without also deciding a real domain's own Authorization Purpose value, stop — this would mean Unit 3/Unit 4 were not built generally enough, contradicting Scope Lock §2.4's own "uniformly available... no caller-specific exceptions" requirement.

---

## 8. Unit 6 — End-to-End Verification

**Purpose.** Prove the fully composed mechanism behaves correctly — fail-closed by default, precedence-safe, no regression — without adopting Authorization Purpose for any real production consumer.

**Authority.** Scope Lock §6 ("Acceptance Criteria"): a future Implementation Plan must build only what is frozen, in the shape frozen, without introducing anything Section 2 does not already describe.

**Inputs.** The fully composed runtime from Unit 5.

**Outputs.** Verification, using a single, clearly-synthetic, test-only Authorization Purpose value (for example, one explicitly named and namespaced to make clear it is not a real domain's own value) registered **solely through test-tier code, never added to `ParkerRuntime.kt`'s own production registration set** — the synthetic value must never appear in the live composition root's own real startup sequence, only within test scaffolding that exercises the real `DefaultPermissionPolicy`/registry pairing Unit 5 wires. "End-to-end," for this unit, means invoking that real, composed pairing from a test harness — not that `ParkerRuntime.kt` itself gains any test-only content, and not a narrower substitute that avoids the real composed objects entirely. Proof required: an absent value denies, an unregistered value denies, a registered-but-ineligible (retired) value denies, a registered-and-eligible value participates correctly in the precedence-safe resolution Unit 4 built, and every existing, unmodified caller's own behaviour is unchanged.

**Files expected to change.** New test files only — none of Units 1–5's own production files are modified again by this unit. (This document does not design the test code itself, per this task's own "no tests" instruction; this unit records *what* must be verified and *where* the synthetic registration may lawfully live, not the test code's own content.)

**Dependencies.** Unit 5.

**Non-responsibilities.** Does not register any real domain's own value; does not decide or verify any policy-content outcome for `memory.retrieve` or any other real action (Scope Lock Deferred Decision 9); does not constitute, and must not be read as constituting, Gap #54 implementation, Knowledge Submission implementation, or Conversational Retrieval implementation.

**Completion criteria.** Every property named in "Outputs," above, is demonstrated true of the composed mechanism using only the synthetic value; no real production consumer's own behaviour changes as a result.

**Stop condition.** If verifying this mechanism is found to require registering a real domain's own Authorization Purpose value to be convincing, stop — that would mean verification has silently become Programme Unit 4 (retrofit) or Gap #54 implementation, both explicitly out of this Plan's own scope, and must be reported rather than proceeded past.

---

## 9. Explicit Exclusions (Restated)

This Implementation Plan does not, anywhere in Units 1–6: design a Kotlin class's own exact shape beyond the constraints Scope Lock §2.2–2.4 already freeze; select a field name; define a registry's own concrete API; define a serialization format; define a storage mechanism; implement Gap #54's own policy-content decision; implement any part of Knowledge Submission's own retrofit; implement any part of Conversational Retrieval.

---

## 10. Relationship to Future Work

Not scheduled by this Plan, named here only for continuity: Programme Unit 4 (Existing Consumer Retrofit — assigning real Authorization Purpose values to `DefaultKnowledgeCandidateEvaluator`, `EvidenceIntelligenceInputResolver`, and others); Programme Unit 5 (Gap #54 Policy-Content Resolution — whether `memory.retrieve`/`memory.retrieve_document` receive an `APPROVED` rule); Programme Unit 6 (`system.*` Convention Retirement). Each requires its own, later, separately-authorised Implementation Plan, building on the mechanism Units 1–6 above deliver.

---

## 11. Risks

- **Risk: Unit 6's own verification quietly becomes real-consumer retrofit.** **Mitigation:** Unit 6's own stop condition names this directly and requires reporting, not proceeding, if it occurs.
- **Risk: a unit's own "Files expected to change" is read as freezing a class or field name.** **Mitigation:** every unit's own Non-responsibilities/Outputs explicitly states that exact naming remains open; only file *locations* are indicated, consistent with `src/contracts/Permission.kt`/`Resource.kt`'s own established co-location convention for sibling contract types.
- **Risk: Unit 4's own precedence-safety obligation is satisfied by a structure that is, in substance, a second authority.** **Mitigation:** Unit 4's own stop condition requires halting and reconsidering rather than proceeding if this shape is the only one found to work.
- **Risk: this Plan's own six-unit sequence is read as a committed schedule rather than a dependency-ordered plan.** **Mitigation:** Section 2's own dependency graph states only ordering constraints; nothing here commits to timing.

---

## 12. Recommendation

Proceed unit by unit, in the dependency order Section 2 derives, each unit's own completion criteria satisfied and its own stop condition checked before the next begins. This document does not authorise any unit's own Kotlin to be written — that remains for whichever later, separately-authorised step actually begins implementation, unit by unit, following this Plan.

---

## 13. Independent Planning Review Self-Check

- **Sequencing independently derived, not assumed?** Yes — Section 2 checks the suggested sequence against the Scope Lock directly and diverges from it in four stated places (propagation, migration, adoption, runtime integration), each with its own citation.
- **Dependency correctness?** Yes — Unit 4's own dual dependency on Units 2 and 3 is derived from the fail-closed requirement specifically, not asserted.
- **Implementation boundaries?** Yes — every unit's own Non-responsibilities names what it must not do, cross-checked against the Scope Lock's own Deferred Decisions list.
- **Accidental governance change?** No — every freeze cited is restated, not altered; no unit redesigns a Scope Lock §2 decision.
- **Accidental Scope Lock violation?** No — Unit 4/5/6 each explicitly forbid adding real policy content or real consumer adoption.
- **Premature implementation freezing?** No — no unit fixes a class, field, or API name; each states only file locations and required behaviour.

```
AUTHORIZATION PURPOSE IMPLEMENTATION PLAN — DRAFT COMPLETE, PENDING INDEPENDENT PLANNING REVIEW
```
