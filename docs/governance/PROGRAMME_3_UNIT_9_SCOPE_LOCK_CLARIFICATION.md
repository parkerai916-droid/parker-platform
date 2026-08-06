**Status:** Narrow governance clarification only. **Adopted.** Independent constitutional review is complete (`docs/reviews/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`): one required correction was identified — disclosure of this document's own precedent basis relative to Unit 7 and Unit 8 — applied, and confirmed by `docs/reviews/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION_DEFECT_CONFIRMATION_REVIEW.md` (READY FOR CLARIFICATION ACCEPTANCE, no regression found). **Domain self-certification under CDR-005 Model C.** No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. This document does not amend `docs/architecture/10-permission-engine.md` ("Chapter 10"), `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md` ("CDR-005"), `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` ("Contract Design V2"), `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` ("the Scope Lock"), or either the Unit 7 or Unit 8 Scope Lock Clarifications, all of which remain frozen and unchanged. It does not reopen Unit 6, 7, or 8, each closed. It does not implement, design, or draft Unit 9's own retrieval interface, query shape, ranking, ordering, or Reasoning Context composition. It does not create a new evaluation tier, and it does not adopt "Evaluation C" as canonical terminology anywhere in its own text — that label is used nowhere below; where this document refers to the classification it performs, it names it by constitutional purpose, never by a nickname. Nothing is staged, committed, or pushed.

# Programme 3 — Unit 9 Scope Lock Clarification

Programme: **Programme 3 — Knowledge Memory, Unit 9 Scope Lock Clarification (Retrieval Permission Classification).**

## 1. Status

See status block above. This document performs exactly one act: the Chapter 10 §10 / CDR-005 domain self-certification classification for Knowledge Retrieval that no prior document has performed. It resolves nothing else about Unit 9 — query shape, ranking, ordering, lifecycle-visibility-in-results, and Reasoning Context composition all remain open, exactly as they were before this document, and are explicitly not addressed here (Section 12).

**Precedent basis for this governance vehicle, stated explicitly.** Scope Lock §5's own Deliverable 9 never names a permission dimension for Retrieval — unlike Deliverable 8, which explicitly named "Permission-boundary wiring" before any Clarification existed, Deliverable 9 is silent on permission entirely. Contract Design V2 never performs a permission classification for Retrieval at any tier. This document therefore performs the *first* constitutional classification of Retrieval — not a resolution of mechanism for an already-recognised act. Unit 7 is the closer precedent for this document's own vehicle choice, because Unit 7 Clarification §13 likewise performed a first-instance Chapter 10/CDR-005 classification, at this same tier, for an act neither Contract Design V2 nor the Scope Lock had previously classified for permission purposes. Unit 8 is not the closer precedent on this specific point: Unit 8 Clarification resolved mechanism (enforcement location, principal supply, resource/action identity, evaluation order, denial disposition) for Evaluation B, a governed act Contract Design V2 §7 (Amendment 8) and Scope Lock Deliverable 8 had already named and recognised before that Clarification was drafted. CDR-005's own Decision Rules distinguish neither positive from negative classifications nor one governance tier from another — they require only that classification occur in "the domain's own governance document," without conditioning that requirement on the classification's own outcome or on which higher-tier document, if any, first named the act. The governance vehicle chosen for this document therefore remains correct regardless of this document's own classification reaching a different, positive outcome from Unit 7's negative one.

---

## 2. Repository Baseline

- **HEAD:** `fabb2124a94eed449095b207efada804dc072ea8` (`fabb212`)
- **Branch:** `main`
- **Remote:** `origin` → `git@github.com:parkerai916-droid/parker-platform.git`; `origin/main` confirmed identical to local `HEAD`.
- **Working tree, confirmed before drafting:** exactly the two expected untracked planning reviews, no discrepancy —
  ```
  ?? docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md
  ?? docs/reviews/PROGRAMME_3_UNIT_9_RETRIEVAL_PERMISSION_EVALUATION_PLANNING_REVIEW.md
  ```
- **Staged changes:** none.

---

## 3. Authorities

Read fresh, in full or in every relevant section, for this document: `docs/architecture/parker-constitution.md` (Architectural Responsibilities; "Cognition proposes, Trust authorises, Runtime executes"; "Uncertainty about trust never defaults to permissiveness"); `docs/architecture/10-permission-engine.md` (Chapter 10, full — §3, §10 read exactly); `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md` (full, including its own Decision Rules and Verification Criteria); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` (§6, §7, §12, §13); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` (§4, §5, §9); `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` (full, §13 exactly); `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md` (full, §5, §12 exactly); `docs/reviews/PROGRAMME_3_UNIT_8_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` (confirmed as the only dedicated independent-review document among Units 6–8; no equivalent standalone document exists for Unit 7, whose own Clarification carries its self-certification directly — noted, not treated as a discrepancy); `docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md` and `docs/reviews/PROGRAMME_3_UNIT_9_RETRIEVAL_PERMISSION_EVALUATION_PLANNING_REVIEW.md` (both this Programme's own immediately preceding work, carried forward without re-litigation); `src/interfaces/KnowledgeStore.kt` (`KnowledgeItem`, lifecycle types); `src/composition/PermissionFilteredMemoryRetrieval.kt` (full, as comparison material only, per Section 9).

---

## 4. Question Presented

Whether Knowledge Retrieval — the act, defined in full by Contract Design V2 §6 and §12 but never yet classified against Chapter 10's admission criteria — is a PermissionEngine proposal class, ordinary internal computation outside PermissionEngine's scope, or a question genuinely contested enough to require CDR escalation rather than ordinary domain self-certification.

---

## 5. Knowledge Retrieval Act

**As already, fully defined by Contract Design V2 §6 and §12 — not redefined here:** a caller issues a Knowledge Query (a task-scoped request for relevant, already-promoted knowledge); Knowledge Memory answers with a Knowledge Result — zero or more `KnowledgeItem`/`KnowledgeReference` entries, each carrying its evidential-state classification, a `ProvenanceReference` to the Memory Core evidence it rests on, and a mandatory staleness disclosure. Knowledge Retrieval performs no write of any kind, and structurally cannot reach Memory Core beyond forwarding an already-issued, minimal, immutable provenance reference (Contract Design V2 §12's own Contract Inventory row for "Knowledge Retrieval (interface)").

**What this document adds, and only this:** a classification of that already-defined act against Chapter 10 §3's admission criteria — nothing about the act's own shape, inputs, or outputs is altered, extended, or newly specified here.

---

## 6. Chapter 10 Admission Test

Chapter 10 §3, read exactly: a proposal is distinguished from ordinary internal computation by whether it carries "a real, external, or state-changing consequence beyond Parker's own internal reasoning — for example, writing, amending, or superseding a durable record; **granting access to a sensitive record or capability**; or otherwise reaching beyond pure interpretation into an effect an owner would recognise as an action taken on their behalf." Ordinary internal computation — "reasoning over already-available information, structural retrieval of non-sensitive records" — does not, by itself, require Permission Engine authorisation.

**A necessary correction before this test can be applied honestly:** Chapter 10 §3 illustrates this test with Memory Core's own precedent, characterising Memory Core Scope Lock §5 as distinguishing a "sensitive `MemoryRetrieval` read" (gated) from "structural, non-semantic retrieval" (its own words: "which is not gated"). This characterisation does not survive a direct check. Memory Core Scope Lock §5's own two clauses concern two different things: "Runtime owns: every `PermissionEngine.evaluate` call required before any `MemoryCore` write and before any sensitive `MemoryRetrieval` read reaches its requester" states *who invokes the Permission Engine and when*; "Memory Core owns: ...structural, non-semantic retrieval" states *what kind of matching algorithm* Memory Core performs (structural, as opposed to embeddings or semantic search) — an entirely different axis. Scope Lock §6, the section immediately following, removes any doubt: "Memory Core never evaluates permissions. Runtime performs all permission decisions before invoking Memory Core. This applies to every operation `MemoryCore` and `MemoryRetrieval` expose, **without exception**." `PermissionFilteredMemoryRetrieval.kt`, read in full, confirms this is what was actually built: every one of its eleven methods evaluates the Permission Engine unconditionally, on every record, with no sensitivity branch anywhere in the class.

**This document records the inconsistency and relies on the corrected reading, without amending Chapter 10 here** — that correction, if made, belongs to Chapter 10's own Final Freeze Verification, not to a Programme 3 Unit Clarification. The test this document actually applies is Chapter 10 §3's own general criterion, checked against the *actual*, built Memory Core precedent (gate every record, unconditionally) rather than Chapter 10's own inaccurate description of it. This document does not, from this correction alone, conclude that all retrieval requires gating merely because Memory Core's own retrieval does — Section 7, below, reasons Knowledge Retrieval's own classification independently, on its own domain act and its own consequence, exactly as Chapter 10 §3's own text requires ("that domain's own specific line is evidence of how one domain applies this general criterion, not a rule this chapter imports wholesale for every domain").

---

## 7. Domain Classification

**Applying Chapter 10's general admission criteria and CDR-005's Model C domain self-certification authority, on Knowledge Retrieval's own domain act, independently of Memory Core's own classification:**

Knowledge Retrieval is classified as a **PermissionEngine proposal class** — a positive classification, disclosed with reasoning per CDR-005's own symmetric documentation requirement, not merely assumed.

**The reasoning:** a Knowledge Result discloses, to a specific requesting principal, the existence and evidential-state classification of a promoted `KnowledgeItem`, together with a `ProvenanceReference` identifying the Memory Core evidence it rests on. This is squarely within Chapter 10 §3's own named example of a proposal-triggering consequence — "granting access to a sensitive record or capability" — because a `KnowledgeItem` is structurally downstream of, and discloses information about, Memory Core evidence that this repository's own only built retrieval-gating precedent treats as requiring authorisation on every single read, without a sensitivity exception (Section 6, above). A caller unable to see certain Memory Core evidence directly, through `PermissionFilteredMemoryRetrieval`'s own per-record gate, would gain an indirect view of that evidence's existence and evidential standing through an ungated Knowledge Result referencing it — precisely the kind of disclosure Chapter 10 §3's criterion exists to catch, whether the disclosure arrives through the underlying record directly or through a downstream summary of it.

**A counter-reading was considered and does not prevail.** One might read Chapter 10 §3's own "reasoning over already-available information" exemption as covering Knowledge Retrieval, on the theory that a `KnowledgeItem` was already gated once, at its own creation (Evaluation A over the underlying evidence, Evaluation B over its submission), and that retrieving it later is merely reasoning over material Parker already holds. This reading does not survive comparison with the one precedent this repository has actually built: Memory Core's own records are, in exactly the same sense, "already available" once created, yet this repository gates every subsequent read of them anyway, without exception (Section 6). If "already created, already authorised once" were sufficient to exempt a later read, Memory Core's own built retrieval gate would not exist. The "already-available information" exemption is better read as covering genuinely non-record-bearing internal computation — reasoning over values already resolved from the current request, exactly as `DefaultReasoningContextAssembler` already does for "Current time," "Current conversation," and similar fields read directly off its own input — not as covering a fresh query against a store of durable, provenance-bearing records.

**This classification is confirmed, not merely suggested, by the categorical distinction Unit 7 Clarification §13 itself already drew to justify its own, opposite conclusion.** Unit 7 classified revision, supersession, retirement, and restoration as *not* requiring gating specifically because "none exposes anything beyond what Promotion (Unit 6) already exposed without Permission Engine gating" — and Promotion itself exposes nothing to an external, requesting principal at all; it is a purely internal act. Retrieval is the first point in Knowledge Memory's own pipeline where content is disclosed to an external, requesting principal. Unit 7's own reasoning for its negative classification is, by its own terms, inapplicable to retrieval — it does not merely fail to support a negative classification here, it affirmatively marks retrieval as the one act in Knowledge Memory's own pipeline its reasoning was never meant to cover.

---

## 8. Permission Ownership

Consistent with Chapter 10 §2, §5, §7, §8, unaffected by this document's own classification:

- **The Permission Engine remains the sole authority for the decision itself**, regardless of this document's own conclusion.
- **Runtime, or Knowledge Retrieval's own eventual implementation, is the operational caller** — which of the two applies depends on Knowledge Retrieval's own eventual shape, not on anything this document decides. Unit 8 Clarification §5 distinguishes the two existing repository precedents exactly: a single, self-contained, one-operation accepting boundary self-gates (`EvidenceCustodian`, `DefaultKnowledgeSubmission`); a multi-operation surface reachable by many independent callers is gated externally by Runtime (`MemoryCore`/`MemoryRetrieval`). Contract Design V2 §12's own description of Knowledge Retrieval — "the single public path through which a Knowledge Query is answered" — reads closer to the first shape, but this document does not decide between them; that determination belongs to Unit 9's own future implementation-facing work, not to this classification.
- **Memory Retrieval cannot be, and is not, the enforcement point** (Section 9, below).
- **Knowledge Retrieval itself does not, and under Memory Core Scope Lock's own generalised principle (Chapter 10 §8) never may, become a second Permission Engine** — whatever mechanism eventually enforces this classification applies the one, shared Permission Engine, exactly as Evaluation A and Evaluation B already do.

---

## 9. Relationship to Memory Core Retrieval

**Knowledge Retrieval does not, and structurally cannot, depend on `PermissionFilteredMemoryRetrieval` (or any `MemoryRetrieval` implementation) to satisfy this obligation.** Confirmed directly from `PermissionFilteredMemoryRetrieval.kt`: every one of its eleven methods operates exclusively over Memory Core's own record kinds (`Entity`, `Document`, `Assertion`, `Relationship`, and the `MemoryCoreRecord` sealed supertype). `KnowledgeItem` is not a `MemoryCoreRecord`; no method on `MemoryRetrieval` accepts one, gates one, or could gate one if it did — its `isApproved` calls are keyed to Memory Core's own `RETRIEVE_ACTION_NAME`/`RETRIEVE_DOCUMENT_ACTION_NAME` action pair, meaningless for a Knowledge-Memory-owned record. This is a type-level, not merely a policy-level, boundary. `PermissionFilteredMemoryRetrieval` is used in this document strictly as *comparison material* establishing what this repository's own built retrieval-gating precedent actually does (Section 6) — never as a dependency Knowledge Retrieval's own classification or eventual implementation may reuse.

Contract Design V2 §12's own narrow authorisation — Memory Core reachable "for forwarded, minimal, immutable provenance references only" — independently forecloses the one way Knowledge Retrieval might otherwise have touched Memory Core's own retrieval surface at all; this document changes nothing about that boundary.

---

## 10. Lifecycle-State Boundary

**Confirmed explicitly: lifecycle state does not, and must not, replace permission evaluation.** No document read for this classification treats a `KnowledgeItem`'s own lifecycle status (`ACTIVE`, `RETIRED`, or any other) as a permission signal, and Chapter 10 §5's "Separation from domain evaluation" guarantee forbids treating it as one: the Permission Engine "never assesses knowledge truth, computes confidence, decides evidential state," and the converse holds equally — a domain's own substantive state is never a substitute for, or determinant of, a permission decision. Unit 7 Clarification §9's own description of retirement as "never implies deletion" and restoration as producing "a new, visible restoration event" concerns visibility *within the item's own history* — an epistemic, audit-trail guarantee — not visibility to a requesting principal via retrieval. Whether a Knowledge Result may include a retired item, and under what disclosure, is a retrieval-shape question this document does not resolve (Section 12); it is, in any case, a question orthogonal to the classification this document does resolve — a retired item, if returned at all, is subject to the same permission classification as any other, never a different one by virtue of its own lifecycle status.

---

## 11. Public Contract Consequences

Without drafting Unit 9's own interface, this classification carries two disclosed, minimum consequences for whatever governance and implementation eventually follow, mirroring precedent this Programme has already established without repeating it here:

- **A requesting principal must be an explicit input to Knowledge Retrieval**, never ambient or inferred — the same treatment Errata 004 already gives Memory Core's own operations and Unit 8 Clarification §6 already gives Knowledge Submission.
- **A permission denial must be expressible as a result distinct from "no matching knowledge exists"** — mirroring Unit 8 Clarification §9's own reasoning for Knowledge Submission's three-way disposition ("a permission denial is constitutionally distinct from a substantive [outcome]... must never be represented, logged, or disclosed as though it were a judgment on [the record's own merit]"), applied here to the distinction between "denied" and "genuinely empty." This document does not name a type, field, or shape for that result — only that a future Unit 9 Contract Design or Scope Lock passage must account for it.

Neither consequence is new architecture; both are the same pattern this Programme has already applied twice, restated here only because this classification makes them applicable to a third act.

---

## 12. Explicit Non-Decisions

This document does **not** resolve, decide, or pre-empt: Knowledge Query's own field shape or "relevance" semantics; ranking, ordering, or scoring of Knowledge Results; whether or how a retired, superseded, or revised item appears in a Knowledge Result, or under what disclosure; the staleness-detection mechanism (Contract Design V2 §3/§6, already reserved to Unit 9's own future work); Reasoning Context's own consumption or composition of Knowledge Retrieval (Scope Lock §4, already allocated to Programme 4); the coexistence or reconciliation of the legacy `KnowledgeSource`/`InMemoryKnowledgeStore` retrieval path and the V2 `KnowledgeItem` store (identified, not resolved, by the preceding planning reviews); any Kotlin interface, type, method signature, resource identifier, or action name for the proposal class this document classifies positively; and whether Knowledge Retrieval's own eventual implementation self-gates (mirroring `DefaultKnowledgeSubmission`) or is externally gated by Runtime (mirroring `PermissionFilteredMemoryRetrieval`) — Section 8 identifies this as open, not settled.

---

## 13. CDR-005 Assessment

**Ordinary domain self-certification is sufficient. Escalation to a new CDR is not required.**

CDR-005's own Decision Rules fix the threshold precisely: a CDR is required "whenever a domain's self-certification against Chapter 10's criteria is genuinely contested, ambiguous, or would require choosing between two or more constitutionally plausible readings"; a domain amendment alone is sufficient "whenever the classification against Chapter 10's published criteria is not genuinely contested." This classification is not genuinely contested: the weight of existing, built authority (Memory Core's own unconditional retrieval gate; Chapter 10 §3's own express naming of "granting access to a sensitive record" as a proposal-triggering example; Unit 7 §13's own reasoning, which affirmatively distinguishes retrieval from every act it classified negatively) resolves cleanly to a positive classification, and the one counter-reading considered (Section 7) fails on direct comparison with the one precedent this repository has actually built, not merely on preference between two equally-supported readings. This mirrors exactly how Unit 6 and Unit 7 each reached confident, disclosed, un-escalated classifications using this same test — this document reaches a different classification than either, but by the same method, at the same confidence, without the genuine two-sided ambiguity CDR-004 was created to resolve.

**Chapter 10 itself is not reopened by this document.** CDR-005's own Decision Rules: Chapter 10 must be amended "only when a constitutional section it itself states — its sole-authority rule, ownership assignments, Runtime relationship, guarantees, boundaries, permission outcomes, failure semantics, or general admission criteria — is materially altered," never merely because a new domain act, cleanly fitting existing criteria, is being recognised. Section 6's own correction is recorded, not enacted, for exactly this reason — it is evidence considered in applying Chapter 10's existing criteria, not a proposed change to them.

---

## 14. Constitutional Self-Certification

Checked directly against the Constitutional Constraints CDR-005 itself states, none reopened or weakened by this classification:

- **Sole authority** — unaffected; this document assigns no authority to any party other than the Permission Engine (Section 8).
- **No bypass** — reinforced, not weakened: a positive classification closes a path (undisclosed retrieval) that would otherwise have let governed knowledge reach a requester without ever passing through the Permission Engine, exactly the risk the no-bypass guarantee exists to prevent.
- **Owner control and Trust Framework ownership** — unaffected; no domain authority is originated or expanded beyond applying Chapter 10's own already-published criteria to a new act.
- **Fail-closed under uncertainty** — honoured, not merely cited: this document resolves what might otherwise have been treated as an unclassified, and therefore ambiguously-gated, act into an explicit, disclosed, positive classification, consistent with "ambiguity about whether an act requires gating must never resolve in favour of treating it as ungated."
- **Modular capability without renegotiating the trust model** — honoured; Chapter 10 is not reopened (Section 13).

---

## 15. Recommendation

Knowledge Retrieval is classified as a PermissionEngine proposal class. A future Unit 9 Contract Design passage or Scope Lock Clarification amendment should record this classification within Contract Design V2 itself (mirroring how Amendment 8 named Evaluation B), and Unit 9's own eventual implementation-facing Clarification — following this document, not replacing it — should resolve the mechanism question Section 8 leaves open (self-gating versus externally-gated) once Unit 9's own concrete shape is designed. Neither step is performed here. This document's own scope is complete: the one classification Chapter 10 §10 and CDR-005 require before Unit 9 may lawfully proceed to implementation on the permission dimension has now been performed and disclosed.

---

## 16. Git Confirmations

- No production code, test, or governance document other than this one was created or modified.
- `docs/architecture/10-permission-engine.md`, `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md`, Contract Design V2, the Scope Lock, and the Unit 7/Unit 8 Clarifications are all unmodified.
- No Kotlin interface, type, method, resource identifier, or action string is named anywhere in this document.
- "Evaluation C" is not used anywhere in this document as canonical terminology.
- Nothing is staged, committed, or pushed.

## 17. Final Git Status

```
$ git status --short
?? docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md
?? docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_RETRIEVAL_PERMISSION_EVALUATION_PLANNING_REVIEW.md
```
