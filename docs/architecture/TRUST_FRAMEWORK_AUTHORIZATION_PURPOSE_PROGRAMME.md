# Trust Framework Authorization Purpose Programme

**Status:** Planned (Future Programme)
**Classification:** Constitutional Evolution — Trust Framework
**Priority:** This Programme's own first step — the carrier/representation design pass (Section 9) — may begin now; no further governance beyond the already-Adopted Contract Design is required to start it. Scope Lock for Authorization Purpose itself may not begin until that pass completes. Gap #54's own Scope Lock (`docs/governance/TRUST_FRAMEWORK_MEMORY_RETRIEVAL_CONTRACT_DESIGN.md`) cannot complete until this Programme delivers Authorization Purpose as an evaluable capability.

This document is planning only. It records future architectural direction so the work is not lost, re-derived, or improvised later. It does not itself design a mechanism, does not authorise a Scope Lock or Implementation Plan, does not write or propose Kotlin, and does not modify `docs/governance/TRUST_FRAMEWORK_AUTHORIZATION_CONTEXT_CONTRACT_DESIGN.md` (the "Contract Design," Adopted) or any other frozen or draft governance document — it is built on the Contract Design's own conclusions, cites them, and carries them forward into roadmap form, without amending the source. Nothing here is staged, committed, or pushed.

---

## 1. Purpose — The Constitutional Deficiency

Parker's Trust Framework can today answer: *may Principal P perform Action A on Resource R?* It cannot answer: *may Principal P perform Action A on Resource R for internal purpose X, but not for internal purpose Y?*

This is not a hypothetical gap. The Contract Design's own independent audit found it already, concretely manifesting: Knowledge Submission's own candidate evaluator and Evidence Intelligence's own input resolver are two constitutionally distinct, independently-governed consumers that propose the *identical* action (`memory.retrieve`) against the *identical* resource shape, through the *identical* shared decorator, carrying the *same* honestly-propagated principal. Nothing in the Trust Framework as it exists today can tell them apart — not because a mechanism was implemented incorrectly, but because no dimension of `ExecutionRequest` was ever designed to answer that question. `principalId` answers who is accountable. `proposedActions` answers what kind of act. `targetResources` answers what is acted upon. None of the three was ever meant to answer, and none can honestly be repurposed to answer, *for what governed reason, on behalf of which internal purpose,* an act is being proposed.

The Contract Design also found, and this Programme treats as settled rather than re-litigating, that no existing concept already fills this gap: not the ad hoc `system.*` Principal-naming convention (ungoverned, inconsistent, and independently confirmed non-functional for at least one live consumer today), and not Principal Delegation via `owner` (governed, but answers a different question — who backs whose authority — and cannot supply a second identity on the single `ExecutionRequest.principalId` field without displacing genuine accountability).

**This Programme exists to carry the Contract Design's own conceptual conclusion — that a new, closed, governed dimension called Authorization Purpose is constitutionally necessary — into a sequenced, scoped body of future work**, without prejudging the mechanism by which it is eventually built.

---

## 2. The Four Dimensions of Authorization

The Trust Framework, once this Programme completes, distinguishes four independent questions about every permission request, not three:

| Dimension | Question answered | Governed by |
|---|---|---|
| **Principal** | Who is accountable for this act? | `IdentityService`, `Principal.md` |
| **Action** | What kind of act is being proposed? | Action Vocabulary, `action-mapping.md` |
| **Resource** | What is being acted upon? | Resource Registry, Chapter 8 |
| **Authorization Purpose** | For what governed reason, on behalf of which internal purpose, is this act being proposed? | *This Programme* |

Each dimension is deliberately caller-agnostic and orthogonal to the other three. None substitutes for, overlaps with, or absorbs another:

- Authorization Purpose never identifies *who* is accountable — that remains Principal's exclusive question, and Principal continues to be propagated exactly as it is today, unweakened, on every request.
- Authorization Purpose never re-describes *what kind* of act is proposed — two requests sharing the identical Authorization Purpose may propose entirely different actions, and two requests sharing the identical action may carry entirely different Authorization Purposes. The two axes vary independently.
- Authorization Purpose never names *what is acted upon* — it says nothing about a Resource's own type or identity, and introduces no new `ResourceType` or Resource Registry entry.

---

## 3. What Authorization Purpose Is

- **A closed, governed vocabulary — never free text, never ambient, never inferred.** Authorization Purpose values are registered, deliberately and individually, the same disciplined way an Action Vocabulary entry is registered today: a fixed, deterministic, reviewed set of named purposes (for example, in the shape `"knowledge-memory.candidate-evaluation"`, `"evidence-intelligence.input-resolution"`), never a caller-supplied arbitrary string and never derived from call-stack, thread-local, or other implicit context. An unregistered or absent value, where one is required, denies — it does not default to permissive.
- **Evaluated by the single, existing Permission Policy — never a second authorization system.** Authorization Purpose is consulted by the same `PermissionEngine`/`DefaultPermissionPolicy` that already evaluates every `ExecutionRequest` today, as an additional, optional matching dimension alongside `(PermissionAction, ResourceType)` and the verb-phrase discriminator already adopted for the Policy Rule Collision Clarification. It is not a second `PermissionEngine`, not a second policy instance, not a parallel decision authority, and does not change `PermissionEngine`'s own public interface. Chapter 10's sole-authority guarantee — no path from proposal to execution bypasses the Permission Engine — is preserved exactly, not narrowed and not duplicated.
- **A purpose classification, never an identity classification.** Authorization Purpose names *why* an act is constitutionally distinct from another act sharing the same action and resource — a governed reason, at the same conceptual altitude as an Action Vocabulary entry — never *which Kotlin class, module, or component* happens to be calling. A candidate Purpose value that would only ever be judged by asking "which code calls this" rather than "what governed reason does this represent" is not a lawful Authorization Purpose value, regardless of how it is spelled.
- **Additive to Principal, never a substitute for it.** A request carries its own real, accountable Principal *and* its own Authorization Purpose, simultaneously, on the same request — the two are independent fields (or independent inputs, however the eventual carrier mechanism represents them; Section 9), never a single field forced to serve both purposes.

---

## 4. Relationship to Gap #54

Gap #54 (`docs/architecture/IMPLEMENTATION_GAPS.md`) is this Programme's own originating trigger, and remains blocked on it. The Trust Framework Memory Retrieval Contract Design and its two adopted Clarifications (Policy Rule Collision; Resolution Derivation Mechanism) resolved every prerequisite reachable without a new authorization dimension: how `memory.retrieve` and `knowledge.retrieve` can lawfully receive independent policy outcomes despite sharing a resolved `(READ, MEMORY)` pair, and how that pair can be derived at all when `targetResources` is deliberately empty. Both leave one prerequisite standing, disclosed by the Resolution Derivation Mechanism Clarification's own Section 7 and carried forward as the Authorization Context Contract Design's own motivating case: **Evidence Intelligence and Knowledge Submission share the identical verb phrase, the identical resource shape, and the identical shared decorator instance, and no dimension of the Trust Framework as it exists today can distinguish them.**

**Gap #54's own Scope Lock cannot lawfully complete until Authorization Purpose exists as an evaluable Trust Framework capability.** Any Scope Lock that approved `memory.retrieve` before that distinction is available would, unavoidably, approve it identically for Evidence Intelligence and Knowledge Submission at once — directly contradicting the Contract Design's own already-accepted requirement that Evidence Intelligence's fail-closed retrieval survive Gap #54's resolution unless a future, separate, explicitly-reasoned decision changes that. This Programme is therefore a hard dependency of Gap #54's own completion, not a parallel, optional enhancement to it.

---

## 5. Affected Subsystems

**Current, already-live subsystems requiring an Authorization Purpose assignment once this Programme delivers a carrier mechanism:**

- `DefaultKnowledgeCandidateEvaluator` (Knowledge Memory, Knowledge Submission's own candidate evaluation) — currently substitutes a fixed, ungoverned `system.knowledge-memory` identity in place of genuine consumer distinction.
- `EvidenceIntelligenceInputResolver` (Evidence Intelligence's own input resolution) — currently propagates the real accountable principal correctly, but has no way to declare its own distinct purpose.

**Current, source-present but not-yet-live subsystems that would need one before composition:**

- `DefaultKnowledgeRevisionEvaluator` (Knowledge Memory, revision/supersession evaluation) — already shares `system.knowledge-memory` with the candidate evaluator in source, though not yet composed into the live runtime; would collide identically the moment it is wired in, absent this Programme's own resolution.
- `PermissionGatedMemoryCore`'s own write-side decorator, if and when composed live.

**Future subsystems this Programme's own general design must accommodate without a bespoke mechanism per consumer:**

- Future Conversational Retrieval (the Parker Conversational Memory Bridge's own paused, second unit).
- Future Memory Maintenance.
- Future World Model retrieval.
- Any future Memory Core consumer, and — by the Contract Design's own general construction — any future consumer of any Trust-Framework-gated boundary that shares an action/resource pair with another, unrelated consumer.

---

## 6. Programme Goals

- **Name the missing dimension, governed and closed.** Establish Authorization Purpose as a first-class, registered Trust Framework vocabulary, held to the same discipline as the Action Vocabulary — deterministic, reviewed, no silent overwrite, no free text.
- **Preserve every existing Trust Framework guarantee.** Sole authority, fail-closed default, no ambient authority, unweakened Principal semantics, no caller-specific exceptions — every constitutional requirement the Contract Design already fixed (its own Section 7) carries forward as this Programme's own binding constraint, not a preference.
- **Resolve Gap #54's own remaining prerequisite.** Give Evidence Intelligence and Knowledge Submission a lawful way to receive independent policy outcomes for the identical act, without either regressing to a fixed system identity or losing real-principal propagation.
- **Generalise beyond Memory Core.** Deliver a mechanism any future Trust-Framework-gated consumer can adopt the same way, not a special case wired to one decorator.
- **Retire the ad hoc `system.*` substitution pattern where it is actually load-bearing for consumer distinction**, replacing an ungoverned, inconsistent, partially-broken convention with a deliberate, reviewed one — without touching the parts of that convention (root system identities such as `system.parker`) that are genuinely, correctly performing ordinary Principal accountability, not consumer distinction.

---

## 7. Non-Responsibilities

This Programme does not:

- Design, select, or freeze the carrier/representation mechanism (Section 9) — that is later, dedicated governance work, not this document's own subject.
- Amend `ExecutionRequest`'s schema, `PermissionEngine`'s interface, `ActionVocabulary`, or `ResourceRegistry` — any such amendment is a future unit's own, separately authorised responsibility, should the carrier design require it.
- Retrofit any existing consumer (`DefaultKnowledgeCandidateEvaluator`, `EvidenceIntelligenceInputResolver`, or any other) with an actual Purpose value — that is implementation work, reserved for a future Scope Lock and Implementation Plan this document does not authorise.
- Decide the actual policy content of any future rule (whether, or under what confirmation level, any specific Authorization Purpose should be approved for any specific action/resource pair) — a policy-content decision for whichever future unit eventually builds the mechanism.
- Reopen, amend, or supersede the Contract Design, the Policy Rule Collision Clarification, or the Resolution Derivation Mechanism Clarification — each remains Adopted and unmodified.
- Resolve Gap #54 itself — it removes Gap #54's own remaining blocking prerequisite once complete, but Gap #54's own Scope Lock and Implementation Plan remain separate, later work.

---

## 8. Dependencies and Sequencing

- **Depends on:** the Contract Design (Adopted) as its own sole conceptual authority; no other new governance is required to begin the carrier-design pass named in Section 9.
- **Blocks:** Gap #54's own Scope Lock (Section 4) cannot complete without this Programme's own output. The Parker Conversational Memory Bridge's own second unit (conversational retrieval) continues to wait on Gap #54, and therefore transitively on this Programme.
- **Does not block:** any work unrelated to Trust Framework permission evaluation for consumers that do not currently collide (the vast majority of the system). This Programme is a targeted, not a repository-wide, dependency.
- **Sequencing within this Programme:** the carrier/representation design pass (Section 9) must complete, and reach its own Contract-Design-tier decision, before any Scope Lock for Authorization Purpose itself may begin. Retrofitting existing consumers (Section 5) cannot begin before that Scope Lock completes.

---

## 9. Carrier/API Shape — Unresolved, Reserved for Later Governance

**The Contract Design deliberately did not decide, and this Programme deliberately does not decide, how Authorization Purpose is actually carried on a permission request.** At least three shapes remain open, none selected or preferred here:

- A new `ExecutionRequest` field — the most direct representation, but the most consequential: `ExecutionRequest` is governed as canonical and immutable-after-validation (ADR-017, ADR-018), backed by `ExecutionRequest.schema.json` and `docs/specifications/volume-01-core-contracts/ExecutionRequest.md`, and touches every existing caller in the system whether or not each caller ever populates a non-default value.
- A companion parameter carried alongside, but outside, `ExecutionRequest` — avoids the schema amendment above, but requires a `PermissionEngine.evaluate` signature change, an equally significant decision.
- Some other, narrower mechanism not yet identified.

This question — and the closely related questions of who governs the Purpose vocabulary's own registration discipline, whether an absent value defaults to deny or to today's caller-agnostic behaviour during any transition, and how existing consumers are retrofitted — is explicitly reserved for a dedicated, future Contract Design of its own, following the same Planning Review → Boundary Review → Contract Design discipline already applied throughout this governance chain. **No Scope Lock for Authorization Purpose may lawfully begin until that carrier question is resolved.**

---

## 10. Likely Future Units (Not Designed Here)

Named for continuity and future planning only — none of the following is scoped, sequenced in detail, or designed by this document. Each would require its own Planning Review and Contract Design before any implementation begins.

- **Unit 1 — Carrier/Representation Design.** Resolves Section 9's own open question: how Authorization Purpose is actually carried on a request, and what (if any) amendment to `ExecutionRequest`, `PermissionEngine`, or neither, that requires.
- **Unit 2 — Authorization Purpose Vocabulary Governance.** Defines the registration mechanism and review discipline for Authorization Purpose values themselves — analogous to, and possibly reusing, the Action Vocabulary's own registration model.
- **Unit 3 — Permission Policy Extension.** Extends `DefaultPermissionPolicy`'s own matching to consult Authorization Purpose as an additional discriminator, building on the same mechanism the Policy Rule Collision Clarification already introduced for the verb-phrase discriminator.
- **Unit 4 — Existing Consumer Retrofit.** Assigns real Authorization Purpose values to `DefaultKnowledgeCandidateEvaluator`, `EvidenceIntelligenceInputResolver`, `DefaultKnowledgeRevisionEvaluator` (if composed live by then), and any other existing Trust-Framework-facing consumer identified by that time.
- **Unit 5 — Gap #54 Policy-Content Resolution.** The actual, separate decision of what outcome `memory.retrieve`/`memory.retrieve_document` should receive per distinguishable Authorization Purpose — the step that finally, safely closes Gap #54's own live symptom.
- **Unit 6 — `system.*` Convention Retirement (where applicable).** Reviews each existing ad hoc `system.*` identity currently substituting for consumer distinction, and retires that substitution in favour of a real Authorization Purpose value wherever Unit 4 has already supplied one — without touching root system identities that correctly represent ordinary Principal accountability rather than consumer distinction.

This list is not committed, not ordered as a schedule, and not exhaustive — it exists so this work is not lost or re-derived from scratch when a future planning pass takes it up.

---

## 11. Risks Carried Forward from the Contract Design

Not re-analysed here — the Contract Design's own Section 20 already names them, and this Programme treats them as still-live constraints on every future unit above: Authorization Purpose drifting into raw component/class identity; the carrier mechanism being resolved by quietly weakening `ExecutionRequest`'s own guarantees rather than through deliberate amendment; the vocabulary becoming a second, informally-governed classification system; and this document's own existence being misread as licensing immediate retrofitting. None of the future units named in Section 10 is authorised to proceed without re-affirming each mitigation the Contract Design already stated.

---

## 12. Relationship to Existing Roadmap

This Programme is a Trust Framework constitutional evolution, not a feature addition — it belongs beside, not inside, the ordinary Programme sequence (Memory Core, Knowledge Memory, Evidence Intelligence) whose own consumers are precisely what motivate it. It is sequenced ahead of, and blocking, Gap #54's own Scope Lock specifically (Section 4, Section 8) — not a general prerequisite for unrelated work elsewhere in the roadmap. This ordering is a planning statement, not a scheduling commitment; it does not change when any other currently-planned unit begins.

```
TRUST FRAMEWORK AUTHORIZATION PURPOSE PROGRAMME — PLANNING DOCUMENT COMPLETE, PENDING INDEPENDENT CONSTITUTIONAL REVIEW
```
