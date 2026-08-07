**Status:** Genuine Independent Constitutional Review of `docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md`, performed as if by another reviewer, against the four source Contract Designs and the Programme document — not against the Scope Lock's own Section 9 self-check alone. This document does not amend the Scope Lock or any source document. Nothing is staged, committed, or pushed.

# Authorization Purpose Scope Lock — Independent Constitutional Review

## 1. Baseline Confirmation

`git status --short` confirms the Scope Lock and this review are the only new files at review time, alongside the already-known, uncommitted Conversational Memory Admission work. Confirmed all four source Contract Designs, the Programme document, the Memory Retrieval Contract Design, and the Implementation Sequence document are untouched.

---

## 2. Challenge — Does Deferring Precedence (Section 5, Item 4) Silently Weaken the Fail-Closed Freeze in Section 2.4? (Substantive Finding)

This is the sharpest tension in the document, and the task's own required challenge list names "weakening fail-closed behaviour" explicitly, so it receives the closest scrutiny.

Section 2.4 freezes: "an absent or unregistered Authorization Purpose value denies by the same default every other unknown value already denies by." Section 5, item 4 defers the *precedence order* between the verb-phrase discriminator (Policy Rule Collision Clarification) and the Authorization Purpose discriminator entirely, citing the task's own instruction not to freeze "precedence implementation, Kotlin algorithms, internal data structures."

**Pressed directly: does deferring precedence *entirely* leave room for a future implementation to satisfy Section 2.4's letter while violating its spirit?** Consider a plausible, naive precedence design: "if a coarse `(action, resourceType)` rule matches and is `APPROVED`, use it; only consult the Authorization Purpose–specific rule if the coarse rule does not resolve." Under this design, a request with *no* registered Authorization Purpose value (or one absent by construction) could still be approved by the coarse rule — because the coarse rule is checked *first*, not last. This would not violate Section 2.4's literal words (an *unregistered* value does still deny, if the Authorization Purpose dimension is ever actually consulted) but it would defeat the entire purpose of this Programme: exactly the Gap #54 collision (Evidence Intelligence and Knowledge Submission sharing `(READ, MEMORY)`) that motivated Authorization Purpose in the first place could recur if "coarse rule wins by default" is a permissible precedence choice.

**The Scope Lock, as drafted, freezes *that an* unregistered value denies, but does not freeze *that Authorization Purpose must actually be consulted before a coarse rule is allowed to resolve a request its own presence was meant to disambiguate.*** This is not merely the Kotlin algorithm (correctly left open) — it is a constitutional requirement on what *any* precedence algorithm must guarantee, of the identical kind Section 2.4 already freezes for the simpler fail-closed default. Carrier Contract Design §19's own Risk 2 anticipated exactly this failure mode ("becomes difficult to reason about deterministically") and required a future Scope Lock to close it — this Scope Lock's own Section 5, item 4 discloses the deferral but does not supply the one constraint needed to make that deferral safe.

**Required correction:** add a frozen principle — not an algorithm — stating that whatever precedence order is eventually chosen, a coarse `(action, resourceType)` rule may never resolve a request for which a more specific, Authorization-Purpose-aware rule was the one actually meant to govern it; where any doubt or ambiguity exists about which rule governs, the fail-closed default (Section 2.4) applies. This constrains the *outcome space* of the eventual algorithm without specifying the algorithm itself, consistent with every other freeze in this document.

---

## 3. Challenge — Is Section 4 ("What This Scope Lock Additionally Confirms") a Genuine Addition, or a Redundant, Confusing Restatement of Section 2.2/2.3?

Independently re-read Section 4's own two bullets against Section 2.2 and 2.3: "closed value type, never raw `String`" restates Carrier Contract Design §7/§19 already-cited material; "additive and reject-on-conflict registration" is *already stated verbatim* as a bullet within Section 2.3 ("Reject-on-conflict registration"). Section 4 does not freeze anything Section 2 does not already freeze — it restates two of Section 2's own points under a new heading, styled as if it were an additional, separate confirmation.

**This is exactly the shape of confusion the task's own "accidental implementation freezing" and "accidental API freezing" challenges are meant to catch** — not because Section 4 freezes something new, but because a reader skimming section headings could reasonably conclude Section 4 *is* a new freeze beyond Section 2, when it is not, making the document's own boundary between "frozen" and "not frozen" harder to audit than a Scope Lock's own purpose requires.

**Required correction:** remove Section 4 as a standalone section; fold its own two clarifying sentences directly into Section 2.2 and 2.3 respectively, where the underlying freezes already live, so no freeze appears to exist outside Section 2's own enumerated boundary.

---

## 4. Challenge — Second Authorization Model, `ExecutionRequest` Authority

Independently re-checked Section 2.2/2.4 against Carrier Contract Design §8 and Chapter 10 §5/ADR-017 directly (both re-read in full for this review). Every rejected Candidate (B, C, D) in the Carrier Contract Design is correctly cited as the reason no parallel object or context is frozen here. **Confirmed sound.**

---

## 5. Challenge — Plugin Privilege Escalation

Independently re-checked Section 2.3/2.6 against Vocabulary Governance Contract Design §10 and `action-mapping.md`'s own "Plugin Supplied Actions" section (both re-read directly): the ceiling ("MUST NOT register... outside what its own Principal could ever be granted") and the capability-declaration distinction (`ModulePermissionRequirement` vs. Authorization Purpose registration) are both restated accurately, without expanding what either source document actually authorises. **Confirmed sound.**

---

## 6. Challenge — Contradiction With Gap #54 or Programme Governance

Independently re-read Memory Retrieval Contract Design §14/§17/§21/§22 and Programme §4/§7/§8/§9 directly. Every claim in Section 2.5 and the Section 9 self-check's own citations were checked against the actual section content, not assumed from section numbers alone. **Confirmed accurate** — no contradiction found, and the section-number citations in the Scope Lock's own self-check are verified correct against the Programme document's own actual structure.

---

## 7. Findings

**Two required corrections:**

1. Section 2.4's fail-closed freeze, combined with Section 5 item 4's total deferral of precedence, leaves room for a technically-compliant future implementation to let a coarse rule resolve a request Authorization Purpose was meant to disambiguate — defeating this Programme's own purpose without violating any literal freeze. A frozen *principle* (not algorithm) closing this gap is required.
2. Section 4 duplicates Section 2.2/2.3 under a separate heading, risking exactly the "accidental freezing outside the stated boundary" confusion this document exists to prevent. It should be folded into Section 2, not stand alone.

No other required correction was found. The single-authority/no-second-system freeze, the plugin-privilege ceiling, and consistency with Gap #54 and Programme governance were each independently re-derived from primary sources and found sound.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

Two narrow, required corrections (Sections 2 and 3, above): add a frozen precedence-safety principle to Section 2.4/Section 5, and fold Section 4 into Section 2.2/2.3 rather than leaving it as a separate, redundant section. Proceeding to a Defect Confirmation Review after both corrections are applied.

**Post-correction status:** both required corrections were applied to `docs/architecture/AUTHORIZATION_PURPOSE_SCOPE_LOCK.md` — a new frozen precedence-safety principle in Section 2.4, and removal of the redundant former Section 4 with its content folded into Section 2.2/2.3 and all subsequent sections renumbered. See `docs/reviews/AUTHORIZATION_PURPOSE_SCOPE_LOCK_DEFECT_CONFIRMATION_REVIEW.md` for the narrow Defect Confirmation Review, which found both corrections complete and no further defect. The Scope Lock is accepted as of that review.
