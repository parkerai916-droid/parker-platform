# Evidence Intelligence — Unit 3 Constitutional Decision Memorandum

## Status

**Constitutional Decision Memorandum (CDM).** This is not a Constitutional Decision Record, not a governance amendment, not a remediation plan, and not a review. No Kotlin is implemented, proposed, or changed. No governance document, production code, or test is modified. Nothing is staged, committed, or pushed.

This memorandum records, permanently and without re-argument, the constitutional interpretation reached upon completion of `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_REMEDIATION_ANALYSIS.md` and `docs/reviews/EVIDENCE_INTELLIGENCE_UNIT_3_CONSTITUTIONAL_FOLLOW_UP_REVIEW.md` ("the Remediation Analysis" and "the Follow-up Review"). It does not repeat their investigative process. It is written so that a future governance review may cite this document alone rather than reconstruct that process.

---

## 1. Purpose

Unit 3 of the Evidence Intelligence Implementation Plan ("Reasoning Provider Orchestration") could not proceed because `ReasoningProviderRequest.turn: Turn` requires a value only `ConversationEngine` may construct, while Evidence Intelligence's own input shape (`EvidenceAnalysisRequest`) has no `Turn` to supply. The Remediation Analysis surveyed six constitutionally plausible remedies and initially found none dominant. The Follow-up Review re-examined the two strongest candidates against primary constitutional sources and reached a settled interpretation. This memorandum exists to fix that interpretation as a stable reference point, so that neither the underlying question — what a reasoning provider's subject fundamentally is — nor the resulting architectural preference needs to be re-litigated by a future reviewer encountering the same fact pattern.

---

## 2. Constitutional Hierarchy

The interpretation below rests on the following ranked hierarchy, established by each document's own stated status, not asserted here:

```
Parker Constitution                    ("Foundational — highest authority in the Parker architecture")
        ↓
Reasoning Context                      ("Constitutional — subordinate to parker-constitution.md")
        ↓
Reasoning Provider Architecture        (Stage 1 Architecture, PES-001)
Reasoning Provider Contract Design     (Stage 2A Contract Design, PES-001)
        ↓
Conversation Engine Architecture       (Stage 1 Architecture, PES-001)
Conversation Engine Contract Design    (Stage 2A Contract Design, PES-001)
```

`docs/architecture/reasoning-context.md` carries the same "Constitutional" status marker as Epistemic Integrity Amendment No. 1 — a tier immediately subordinate to the Constitution and superior to every ordinary Architecture or Contract Design document. `REASONING_PROVIDER_ARCHITECTURE.md`, `REASONING_PROVIDER_CONTRACT_DESIGN.md`, `19-conversation-engine.md`, and `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` are each PES-001-governed documents (Stage 1 or Stage 2A) — a lower, non-constitutional tier.

Constitutional-tier documents control lower architectural documents wherever wording differs because the Constitution states so directly: "Where any other document, design decision, or implementation appears to conflict with this constitution, this constitution prevails." `reasoning-context.md` occupies the same governing position one level down, by its own status declaration, exactly as Epistemic Integrity Amendment No. 1 does for evidential matters. A Stage 1 or Stage 2A document may specialise, instantiate, or implement what a constitutional-tier document establishes; it may not narrow, redefine, or be read as having silently amended it. Conversation Engine and Reasoning Provider sit at the same, lower tier relative to `reasoning-context.md` — neither has authority over the other, and neither may be read as having settled, on its own, a question `reasoning-context.md` already answered more generally.

---

## 3. Interpretation

### A. Why `ReasoningContext` is the governing authority for reasoning subjects

`reasoning-context.md` is the highest-tier document that defines what a reasoning provider reasons over, and it does so in terms of **a task**: "Reasoning Context is the temporary, task-scoped working set that a reasoning provider actually reasons over. It is assembled specifically for the task at hand." It states its own relationship to the Constitution directly: "This document specialises the cognition stage of the Parker Constitution... by defining precisely what cognition is given to reason over." The Constitution itself, at the tier above, uses only the generic phrase "interprets a request." No document at or above this tier ever names "Turn," "Conversation," or conversational exchange as the subject of reasoning. `reasoning-context.md` is therefore the governing authority: it is both the most specific and the highest-tier statement of what a reasoning subject fundamentally is, and every lower document's treatment of the subject must be read as an instance of "task," not as a competing definition of it.

### B. Why `Turn` is the first concrete reasoning subject, not the universal reasoning subject

`REASONING_PROVIDER_ARCHITECTURE.md` — the document that first ties reasoning to "conversational context" and to a Turn's content — is a Stage 1 Architecture document, one tier below `reasoning-context.md`. Its own Status section discloses why it took this shape: it was produced specifically to close "the highest remaining architectural dependency within the communication/conversation track," i.e., to unblock Conversation Engine, the one caller named at the time it was written. Nothing in that document, or in `REASONING_PROVIDER_CONTRACT_DESIGN.md` beneath it, argues that reasoning is inherently conversational; the narrowing from "task" to "Turn" is presented as an instantiation for a named client, not as a considered restriction on the constitutional-tier concept above it. This reading is independently confirmed by CDR-007, a Constitutional Decision Record superior to both Architecture documents: CDR-007 authorises Evidence Intelligence — a non-conversational subsystem — to orchestrate `ReasoningProvider` without ever mentioning "Turn," and without treating a Turn-shaped subject as a precondition of that authorisation. `Turn` is accordingly interpreted as Conversation Engine's own first concrete realisation of the constitutional-tier "task" concept, not as the universal or exclusive shape a reasoning subject must take.

---

## 4. Architectural Consequence

The constitutional interpretation recorded in Section 3 supports future evolution through generalising the subject `ReasoningProviderRequest` carries (Option 1 of the Remediation Analysis and Follow-up Review), rather than through introducing a dedicated, Evidence-Intelligence-specific reasoning contract (Option 3).

- **Subsystem ownership.** The generalised direction leaves `ReasoningProvider`'s existing ownership, and Conversation Engine's exclusive construction of `Turn`/`Conversation`, completely intact. Evidence Intelligence acquires no dependency on Conversation Engine and no residual claim over any type it does not already own.
- **Reuse.** It reuses the single existing `ReasoningProvider` interface rather than duplicating it, directly satisfying this repository's own stated design discipline: "prefer reuse; prefer composition... avoid duplication... avoid mirrors... avoid shadow models" (Evidence Intelligence Contract Design, Design Rules Compliance).
- **Dependency boundaries.** Evidence Intelligence's dependency table is untouched in structure; only the internal shape of one already-authorised dependency's request type changes.
- **Repository precedent.** The generalised direction is precedented twice, concretely: by `CandidateMemoryCoreRecord` (`src/interfaces/EvidenceIntelligence.kt`) — a closed, behaviour-free selector wrapping existing, unmodified types under a single owner — and by `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §6's additive extension of `ConversationEngine` to a second operation, expressly characterised as "not a reassignment... not a redesign," requiring no fresh Constitutional Decision Record.
- **Architectural convergence.** A single, unified `ReasoningProvider` abstraction remains platform-wide and interchangeable across every caller, consistent with the Constitution's Replaceable Reasoning Providers principle at the caller level, not only the implementation level.

Option 3 was not adopted because it reverses, rather than instantiates, an explicit Constitutional Decision Record clause: CDR-007's Decision block states, "A new Reasoning Provider abstraction specific to Evidence Intelligence — Not adopted." Introducing a dedicated Evidence Intelligence reasoning contract is not an adjacent refinement of that decision; it is the specific candidate the decision already considered and rejected. It also duplicates a contract this repository's own design rules instruct against duplicating, and does not converge — a pattern of one bespoke reasoning contract per subsystem does not scale as further callers appear.

---

## 5. Governance Classification

The interpretation recorded in Section 3 is a **constitutional interpretation, not a constitutional reinterpretation** — it applies what `reasoning-context.md` and the Constitution already establish to a fact pattern (a non-conversational reasoning subject) neither document had previously been tested against. Neither document's own wording changes as a result.

The architectural consequence recorded in Section 4 is a **Contract Design evolution**, not a constitutional change. It operates on the field-level shape `REASONING_PROVIDER_CONTRACT_DESIGN.md` (Stage 2A) fixed for `ReasoningProviderRequest` — exactly the tier at which `Turn`'s role was originally, and only ever, decided. It touches no responsibility, ownership, or trust boundary any Stage 1 Architecture document assigned.

**The Constitution does not change.** `reasoning-context.md` does not change. No operative decision recorded by CDR-007 changes.

---

## 6. CDR Assessment

**Constitutional necessity and governance usefulness are distinct questions and are answered separately.**

**Constitutional necessity: no CDR is required.** No operative clause of the Constitution, `reasoning-context.md`, or CDR-007 is violated by generalising `ReasoningProviderRequest`'s subject. CDR-007 does not mention `Turn` anywhere in its text. Its two "not broadened" clauses are each narrower than a field-shape freeze: one disclaims that CDR-007 itself performs any broadening; the other forbids broadening the abstraction so as to describe Evidence Intelligence's own identity as a Reasoning Provider — a classification guard, not a request-shape freeze. Evidence Intelligence's architectural role, and every ownership, dependency, and exclusion CDR-007 fixes, remain untouched.

**Governance usefulness: a CDR is advisable, not mandatory.** That the Remediation Analysis and the Follow-up Review independently reached different conclusions on this same question is itself evidence the point benefits from a disclosed, citable resolution. CDR-005's own Model C self-certification pattern — self-certify against published criteria, escalate to a full Constitutional Decision Record only if contested — is the proportionate mechanism, mirroring CDR-004's own precedent of confirming, rather than re-deriving, that an existing generic capability already covers a previously-uncovered case.

**Conclusion: unnecessary as a matter of constitutional requirement; at most advisable as an optional, lightweight, self-certifying confirmation.**

---

## 7. Constitutional Findings

1. `ReasoningContext` (`docs/architecture/reasoning-context.md`) is the constitutional authority governing what a reasoning provider reasons over; it defines that scope as a task, not as a Turn or a conversational exchange.
2. `Turn` is Conversation Engine's own first concrete implementation of that constitutional authority, introduced at the Architecture tier for a specific, named dependency — not a universal or exclusive reasoning subject.
3. Evidence Intelligence continues to reuse the existing `ReasoningProvider` abstraction unmodified in identity; its own architectural classification under CDR-007 is unaffected by this interpretation.
4. The preferred future direction — generalising the subject `ReasoningProviderRequest` carries — is achieved through Contract Design evolution, not constitutional reinterpretation.
5. The Constitution, and `reasoning-context.md`, remain unchanged. No operative decision recorded by CDR-007 changes.
6. A Constitutional Decision Record is not constitutionally required for this direction; it is, at most, an optional self-certifying confirmation, escalating to a full CDR only if contested.
7. Future governance reviews concerning Evidence Intelligence Unit 3's Reasoning Provider orchestration should cite this memorandum as the authoritative constitutional interpretation, rather than reconstructing the Remediation Analysis or the Follow-up Review.
8. Nothing in this memorandum alters the ownership of `Turn`, `Conversation`, or `ConversationEngine`. Ownership of all three remains exclusively governed by Conversation Engine's own governance: `CONVERSATION_ENGINE_CONTRACT_DESIGN.md` §3 ("A Turn is never created except as part of this one operation") and `CONVERSATION_CONTINUITY_CONTRACT_DESIGN.md` §12 ("`ConversationEngine` ownership is preserved exactly: it remains the only component that ever constructs a `Conversation` or `Turn`").

---

**No Kotlin implemented. No governance document amended. No production code or test modified. Nothing staged, committed, or pushed.**
