**Status:** Genuine Independent Constitutional Review of `docs/governance/AUTHORIZATION_PURPOSE_VOCABULARY_GOVERNANCE_CONTRACT_DESIGN.md`, performed as if by another reviewer, against the governing documents and the actual, current repository content — not against the Contract Design's own Section 20 self-check alone. This document does not amend the Contract Design, Unit 1, or any other frozen or draft governance document, Kotlin file, or test. Nothing is staged, committed, or pushed.

# Authorization Purpose Vocabulary Governance Contract Design — Independent Constitutional Review

## 1. Baseline Confirmation

`git status --short` confirms the Contract Design and this review are the only new files at review time, alongside the already-known, uncommitted Conversational Memory Admission work. Confirmed Unit 1 (`AUTHORIZATION_PURPOSE_CARRIER_CONTRACT_DESIGN.md`) and every other cited governance document are untouched.

---

## 2. Challenge — Is Section 3's Own Naming-Inconsistency Evidence Accurate? (Substantive Finding)

Section 3 cites two claimed examples of Action Vocabulary naming drift: `MemoryAdmissionCoordinator`'s `"create conversational memory record"`, and a hyphen/underscore mismatch between `EvidenceRegistrationCoordinator.CREATE_PROVENANCE_ACTION_NAME` (`"memory.create-provenance"`) and `PermissionGatedMemoryCore.CREATE_PROVENANCE_ACTION_NAME` (`"memory.create_provenance"`), presented as if both were live, simultaneously-registered entries.

**Independently re-checked**: grepped `ParkerRuntime.kt` directly for `PermissionGatedMemoryCore(` — zero matches. `PermissionGatedMemoryCore` is never constructed in the live composition root, confirming the same dormancy status this governance chain has already, independently established for it in an earlier task (the Resolution Derivation Mechanism Clarification). Its own `CREATE_PROVENANCE_ACTION_NAME` constant is therefore **never passed to `vocabulary.register()`** — there is no live, simultaneously-registered naming collision between it and `EvidenceRegistrationCoordinator`'s own hyphenated name; only `EvidenceRegistrationCoordinator`'s own `"memory.create-provenance"` is actually registered today.

**This does not undermine Section 3's own underlying point** — `MemoryAdmissionCoordinator`'s own `"create conversational memory record"` is independently confirmed live-registered (grepped `ParkerRuntime.kt` line ~960: `verbPhrase = MemoryAdmissionCoordinator.CREATE_CONVERSATIONAL_MEMORY_ACTION_NAME`), and stands on its own as sufficient evidence that naming convention is not mechanically enforced. But citing the dormant `PermissionGatedMemoryCore` pairing as if it were a second, equally-live example overstates the evidence, exactly the kind of overstatement this governance chain has specifically, repeatedly corrected in prior Units (Resolution Derivation Mechanism Clarification, Authorization Context Contract Design).

**Required correction:** Section 3 must disclose that `PermissionGatedMemoryCore` is dormant, not composed into the live runtime, and that the hyphen/underscore mismatch is an in-source inconsistency rather than a live, simultaneously-registered naming collision — while retaining the `MemoryAdmissionCoordinator` example, which is independently confirmed live and sufficient on its own.

---

## 3. Challenge — Section 7's Own Cross-Reference and Sentence Construction

Independently re-read Section 7 ("Immutability") in full: "This is distinct from or *lifecycle status* (Section 9) changing, which does not alter meaning." This sentence does not parse (a stray "or"), and its own cross-reference is wrong: Section 9 is "Can They Be Versioned?"; the section actually discussing lifecycle status (deprecation) is Section 8. **Required correction:** repair the sentence and correct the cross-reference to Section 8.

---

## 4. Challenge — Does Section 14's "Verb-Phrase Pattern, Not Enum Pattern" Conclusion Contradict Unit 1's Own "Closed Value Type" Requirement?

Pressed directly, since this is the kind of subtle inconsistency a careless reader — or a careless author — could introduce across two documents in the same chain. Independently re-read Unit 1 §7/§19: "the field's own value is a small, separately-defined, closed value type... must specify a closed, non-`String` value type (mirroring `PrincipalId`/`ResourceId`)." Independently re-read `Principal.kt`: `PrincipalId`/similar value classes wrap a plain `String` with only a non-blank check — they are structurally distinct Kotlin types, not closed enums of pre-approved values. Unit 1's own "closed" therefore refers to *structural* type-safety (a caller cannot pass an arbitrary raw `String` where an `AuthorizationPurposeId` is expected), not *semantic* closedness of the value space (a fixed, enumerable set decided in advance). Section 14's own "verb-phrase pattern" (registered, growable, string-content, but wrapped in a distinct type) is fully compatible with this — the two documents are describing different layers (Kotlin type structure vs. vocabulary content governance), not competing claims. **Confirmed no contradiction; the two documents are consistent, though neither states the distinction between "structural closedness" and "semantic closedness" as explicitly as this review just did.** Not a required correction — both documents remain individually accurate — but worth noting for a future reader.

---

## 5. Challenge — Are Sections 9 and 15 in Tension Regarding "Redefining a Value in Place"?

Section 9 recommends never redefining an existing value's meaning (mint a new value, deprecate the old); Section 15 defines "giving an already-registered value a new mandatory meaning" as one recognised breaking-change type, gated by governance review. Checked whether these conflict: they do not — Section 15 defines what governance a breaking change requires *if* it occurs; Section 9 is normative guidance that the preferred practice is to avoid ever needing that path by minting new identifiers instead. **Confirmed complementary, not contradictory** — no correction required, though an explicit one-clause cross-reference between them would improve readability. Not required.

---

## 6. Challenge — Is the CDR-005 Threshold (Section 3, 14) Stated Accurately?

Independently re-read CDR-005's own "When a CDR is required" text again: "whenever a domain's self-certification against Chapter 10's criteria is genuinely contested, ambiguous, or would require choosing between two or more constitutionally plausible readings." The Contract Design's own citation and application (ordinary registration does not require CDR-005; only genuine contest does) is confirmed accurate and consistent with how this exact threshold was already, independently applied in both Adopted Memory Retrieval Clarifications. **Confirmed sound.**

---

## 7. Challenge — Is the Breaking-Change Definition (Section 15) Genuinely Sourced, or Invented Under the Guise of Citation?

Independently re-read `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`'s own text: "a genuinely breaking change (a field renamed, removed, or given a new mandatory meaning) requires its own dedicated governance review — an amendment to this document — accepted *before* any migration code is written." The Contract Design's own Section 15 adapts this definition (rename/remove/new-mandatory-meaning) to the vocabulary context precisely, without adding or removing a category beyond what the source already names. **Confirmed genuinely adapted, not invented.**

---

## 8. Findings

**Two required corrections:**

1. Section 3 overstates its own second naming-inconsistency example — the `PermissionGatedMemoryCore` pairing is not a live, simultaneously-registered collision, since `PermissionGatedMemoryCore` is confirmed dormant (never constructed in `ParkerRuntime.kt`). The document's underlying conclusion survives on its first, independently-confirmed-live example (`MemoryAdmissionCoordinator`).
2. Section 7 contains a broken sentence and an incorrect cross-reference (should reference Section 8, not Section 9).

No other required correction was found. The "closed value type" vs. "verb-phrase pattern" consistency across Unit 1 and this Unit, the Sections 9/15 relationship, the CDR-005 threshold, and the breaking-change definition's own sourcing were each independently re-derived from primary sources and found sound.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

Two narrow, required corrections (Sections 2 and 3, above): disclose `PermissionGatedMemoryCore`'s own dormancy in Section 3 rather than presenting it as a live naming collision, and repair Section 7's own broken sentence and cross-reference. Proceeding to a Defect Confirmation Review after both corrections are applied.

**Post-correction status:** both required corrections were applied to `docs/governance/AUTHORIZATION_PURPOSE_VOCABULARY_GOVERNANCE_CONTRACT_DESIGN.md` Sections 3 and 7. See `docs/reviews/AUTHORIZATION_PURPOSE_VOCABULARY_GOVERNANCE_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md` for the narrow Defect Confirmation Review, which found both corrections complete and no further defect. The Contract Design is accepted as of that review.
