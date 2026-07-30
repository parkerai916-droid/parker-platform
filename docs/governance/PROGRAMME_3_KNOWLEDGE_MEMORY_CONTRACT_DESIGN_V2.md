**Status:** Governance and design only. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed. No language syntax, method signature, or pseudocode appears anywhere below.

# Programme 3 — Knowledge Memory Contract Design (Version 2)

Programme: **Programme 3 — Knowledge Memory Contract Design, Revision 2.**

This document supersedes `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN.md` ("Version 1"). It is not a redesign. Version 1's architecture, layering, public model, and section structure are retained in full except where `docs/reviews/PROGRAMME_3_KNOWLEDGE_MEMORY_ADVERSARIAL_CONTRACT_REVIEW.md` ("the Adversarial Review") identified a required amendment. The Adversarial Review is treated as authoritative regarding the eight weaknesses it identified; this document resolves each one and changes nothing else. `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md`, `docs/architecture/epistemic-integrity.md`, `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, and `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md` remain authoritative and are not contradicted by anything below. No constitutional requirement is weakened anywhere in this revision — every change strengthens precision, narrows implementation freedom, or corrects an internal inconsistency; none loosens a guarantee Version 1 already made.

The layering constraint is unchanged from Version 1:

```
Conversation History
        |
        v
   Memory Core
        |
        v
 Knowledge Memory
        |
        v
Reasoning Context
        ^
        |
   World Model
```

**The constitutional principle governing every contract below, unchanged:** *Memory Core remains the constitutional source of stored evidence. Knowledge Memory becomes the constitutional source of promoted knowledge. Knowledge Memory exposes knowledge; it never exposes storage mechanics, never duplicates Memory Core's responsibilities, and never owns provenance, identity, or relationships.*

---

## 1. Responsibilities

**Knowledge Memory is responsible for:**

- Evaluating a candidate, submitted with reference to Memory Core evidence, against promotion criteria, and deciding whether it becomes durable, retrievable knowledge.
- Holding the record of what has been promoted, including the evidential-state classification (Article IV) that promotion attaches to it.
- Re-evaluating a promoted item when new Memory Core evidence bearing on the same proposition arrives, and preserving what was previously understood alongside the update.
- Answering task-scoped knowledge queries for Reasoning Context assembly — the only path through which Reasoning Context obtains promoted knowledge.
- Disclosing, honestly, when evidence does not justify a single conclusion — surfacing unresolved competing propositions rather than forcing a premature winner.

**Knowledge Memory is explicitly not responsible for:**

- Identity, provenance, or relationships — unchanged from Version 1; these remain exclusively Memory Core's.
- Permission evaluation of its own devising (Section 7, revised).
- Storage mechanics of any kind (Section 13).
- **Truth.** *(Revised — Amendment 1.)* Promotion is a durability-worthiness judgment, never a verdict that a proposition is true. Knowledge Memory classifies a promoted proposition according to the constitutional evidential-state taxonomy (Article IV) — how well the available Memory Core evidence supports it — and this classification is never itself a determination of factual truth. A Knowledge Item classified at a strong evidential state is not thereby certified true; it is disclosed as well-supported by specific evidence, and remains open to revision exactly as Article XVI requires.
- Anything belonging to World Model, Conversation History, Document Intelligence, the Planner, or Workflow Engine (Sections 9, 10, 13).

---

## 2. Public Model

Unchanged from Version 1 except the Knowledge Candidate row, revised for Amendment 2.

| Candidate concept | Determination | Reasoning |
| --- | --- | --- |
| **Knowledge Item** | Belongs. | Unchanged from Version 1. |
| **Knowledge Reference** | Belongs. | Unchanged from Version 1; its shape is now bound by Section 6's revised minimality/immutability rule. |
| **Knowledge Query** | Belongs. | Unchanged from Version 1. |
| **Knowledge Result** | Belongs. | Unchanged from Version 1; now additionally required to carry a staleness disclosure (Section 3, Section 11 — Amendment 7). |
| **Knowledge Promotion** | Belongs, narrowed. | Unchanged in kind from Version 1, but now created at every revision, not only at initial promotion (Section 3 — Amendment 4). |
| **Knowledge Revision** | Does not belong as a standalone concept. | Unchanged from Version 1 — folded into Knowledge Item's own lifecycle, now with explicit ordering rules (Section 3 — Amendment 4). |
| **Knowledge Candidate** | Belongs, and is necessary. | *(Revised — Amendment 2.)* Carries a reference to existing Memory Core evidence and nothing else evidential — explicitly, **a Knowledge Candidate carries no caller-settable confidence value and no caller-settable evidential-state value.** Both are computed exclusively by Knowledge Memory itself (Section 4, Section 5). A submission proposing its own confidence or evidential classification is malformed and must be rejected as such, not silently accepted and overridden. |

---

## 3. Knowledge Lifecycle

Substantially revised for Amendments 1, 2, 4, 5, and 7.

- **Promotion.** A Knowledge Candidate, referencing existing Memory Core evidence, is evaluated against promotion criteria (Section 5). If it meets them, a Knowledge Item is created, carrying an evidential-state classification and a Knowledge Promotion record disclosing the basis for that decision. *(Revised — Amendment 1.)* This classification expresses how well the cited Memory Core evidence supports the proposition; it is never read, represented, or implemented as a truth determination. *(Revised — Amendment 2.)* Any confidence value contributing to the classification is sourced only from Memory Core's own recorded evidence (for example, a referenced Assertion's own recorded confidence, where present) or from Knowledge Memory's own independent evaluation performed at this moment — never from the submitter. Where Memory Core's own recorded confidence is absent and Knowledge Memory's own evaluation cannot establish one from the available evidence, the resulting classification must express that absence honestly (Section 4) — it must never manufacture a numeric confidence value to fill the gap. A candidate that is not promoted produces no Knowledge Item and no Knowledge Memory record at all; its underlying evidence remains exactly where it already was, inspectable in Memory Core on its own terms.
- **Revision.** *(Revised — Amendment 4.)* New Memory Core evidence bearing on an already-promoted Knowledge Item triggers re-evaluation. Each Knowledge Item's classification history is a single, chronologically ordered, non-forking sequence — it never branches. Where more than one piece of new evidence arrives for the same Knowledge Item within a short interval, both are incorporated into the same re-evaluation and serialized into one next classification, by a consistent, disclosed ordering rule (the specific rule — for example, ordering by evidence acceptance time — is a Scope Lock decision, not fixed here; what is fixed here is that exactly one such rule must exist and be applied consistently, never left to vary by implementation whim or by race condition). Every revision — not only the initial promotion — produces its own Knowledge Promotion-kind disclosure record, stating the basis for that specific change. A prior classification, and the disclosure record that justified it, both remain retained and reachable (Section 6) — a revision never overwrites the disclosure that preceded it.
- **Supersession.** *(Revised — Amendment 4.)* When Memory Core marks the underlying Assertion `SUPERSEDED`, the dependent Knowledge Item is re-evaluated against the superseding evidence, exactly as an ordinary revision. Supersession chains of arbitrary length are supported: each hop links to the Memory Core relationship that justified it, and the full chain — not merely the most recent hop — remains transitively retrievable as part of the Knowledge Item's evidential history (Section 6). The superseded classification is retained, marked accordingly, and remains retrievable; it is not deleted.
- **Contradiction.** Unchanged from Version 1: where Memory Core holds an unresolved `CONTRADICTS`/`DISPUTES` relationship between two Assertions each capable of supporting a promotable proposition, Knowledge Memory must not promote one and silently omit the other. The constitutionally correct outcome is a Knowledge Item (or a pair of Knowledge Items) whose evidential-state classification honestly discloses that the matter is unresolved. *(Reinforced by Amendment 1's revised Section 5:* a promotion-weighting rule that permitted a single factor to break a tie between contradictory evidence would itself violate this requirement — Section 5 now forbids exactly that.)
- **Retirement.** *(Revised — Amendment 5.)* Retirement marks a Knowledge Item as no longer current. **Retirement never implies deletion** — the Knowledge Item record, its full classification history, and every disclosure record attached to it remain retained and retrievable regardless of retirement. Retirement is reversible: where a Knowledge Item was retired because re-evaluation concluded it was no longer sufficiently supported, and new Memory Core evidence subsequently re-establishes support for the same proposition, Knowledge Memory does not reactivate or roll back the retired record in place. Instead, it appends a new **restoration** event to the same Knowledge Item's history — a fourth kind of lifecycle event alongside promotion, revision, and retirement — so that the full sequence (promoted → retired → restored) remains visible, never silently collapsed to "currently active" with no trace that retirement ever occurred. One case is not reversible in this way: where retirement resulted from the underlying Memory Core evidence itself being permanently erased through the owner-requested deletion path, there is no evidence left to restore from, and no restoration event may be recorded against that Knowledge Item. Support for the same proposition, if it re-emerges from new evidence, is expressed as a **new promotion** of a new Knowledge Item — never as a restoration of the one whose evidentiary basis was erased. This distinction preserves the same non-erasure discipline the Constitution requires of Memory Core's own deletion boundary (Section 7), without inventing a second, more permissive erasure path at the Knowledge Memory layer.
- **Staleness.** *(New subsection — Amendment 7.)* A Knowledge Item's classification reflects the state of its underlying Memory Core evidence at the moment that classification was computed. Where the underlying evidence's status changes afterward (for example, becomes disputed) before Knowledge Memory has re-evaluated and produced a new classification, the Knowledge Item is **stale**, and this must never be silently concealed. Every Knowledge Result that includes a Knowledge Item or Knowledge Reference must carry an explicit staleness disclosure for it — present and populated on every result, never an optional field a caller must remember to check, and never inferred from its own absence. How staleness is detected (continuous monitoring, checked at query time, or otherwise) is left to Scope Lock and implementation; that it must always be disclosed, on every result, is fixed here.
- **Historical preservation.** Unchanged from Version 1, reinforced by the above: no lifecycle event — promotion, revision, supersession, retirement, or restoration — is ever expressed as an in-place mutation. Every event is a new, linked, appended act.

---

## 4. Evidential State

- **Where it is stored.** Unchanged from Version 1: on the Knowledge Item itself, never on the underlying Memory Core evidence.
- **Who assigns it.** *(Revised — Amendments 1 and 2.)* Knowledge Memory's own promotion or revision evaluation, and no one else. This classification is an assessment of evidential support, never a truth determination (Section 1, Section 3). Any confidence component of the classification is sourced exclusively from Memory Core's own recorded evidence or from Knowledge Memory's own independent evaluation — never accepted as a value a submitter declares (Section 2). Where neither source can establish a confidence value, the classification must express that absence explicitly (see below) rather than defaulting to an invented figure.
- **Who may revise it.** Unchanged from Version 1: only Knowledge Memory's own re-evaluation process, triggered by new Memory Core evidence, following Section 3's ordering rules.
- **Who may read it.** Unchanged from Version 1: anything with legitimate, permission-gated read access to the Knowledge Item.
- **Mandatory expressiveness.** *(Revised — Amendment 3.)* Permissive language is removed. Whatever evidential-state representation is designed at Scope Lock, it **must** be capable of expressing an "insufficiently supported" or "unresolved" outcome — this is a binding requirement of this Contract Design, not an option the representation may or may not include. A representation limited to a bare graduated-confidence scalar, with no way to express that a proposition's support is genuinely inconclusive, does not satisfy this Contract Design and may not be treated as compliant with it.

---

## 5. Promotion Boundary

**What qualifies evidence for promotion**, revised for Amendments 1 and 2:

- The evidence must already exist as a Memory Core record with valid, mandatory provenance already attached — unchanged from Version 1.
- The evidence must meet the promotion criteria already established for this layer (repetition, importance, relevance, frequency, confidence, explicit request). *(Revised — Amendment 1.)* These factors' weighing is no longer left unconstrained. Weighing **must** consider more than one factor — no single factor may, by itself, determine promotion or the resulting evidential-state classification, absent an express, documented governing-rule exception stated and justified at Scope Lock (mirroring Article XI §1's own conditions for such an exception). In particular, **repetition and frequency must never be treated as independent corroboration merely because a proposition was mentioned more than once** — before repetition or frequency may contribute weight, the evaluation must first determine whether the repeated mentions share a common origin; mentions sharing a common origin contribute no more weight than a single mention would.
- The evidence's `ContentNature`, however classified (including `UNKNOWN`), is carried forward honestly into the resulting evidential-state classification — unchanged from Version 1.

**What never qualifies:**

- Evidence with no Memory Core provenance reference — unchanged.
- Evidence whose Memory Core record has been deleted through the owner-requested erasure path — unchanged (Section 3, "Retirement").
- A caller's direct request to "promote this" — unchanged; promotion remains internal.
- *(Revised — Amendment 2.)* **A caller-declared confidence or evidential-state value accompanying a Knowledge Candidate.** A Knowledge Candidate carrying either is malformed (Section 2) and must be rejected on that basis; Knowledge Memory computes both itself, from the sources Section 4 defines, and never from the submission.

**Reaffirmed, not merely restated:** promotion never constitutes a truth determination. A Knowledge Item's evidential-state classification expresses how well Memory Core evidence, weighed as required above, supports a proposition at the time of classification — never whether the proposition is true.

---

## 6. Retrieval Contracts

Unchanged from Version 1 except the two rows below.

| Candidate capability | Determination | Reasoning |
| --- | --- | --- |
| **Retrieve promoted knowledge relevant to a task** | Belongs. | Unchanged. |
| **Retrieve competing propositions** | Belongs. | Unchanged; reinforced by Section 5's revised weighing constraint, which prevents this capability's input from ever being pre-collapsed by a single-factor promotion decision. |
| **Retrieve superseded knowledge** | Belongs. | Unchanged; now explicitly returns full multi-hop chains (Section 3). |
| **Retrieve provenance references** | Belongs, narrowly, now tightly bound. | *(Revised — Amendment 6.)* A provenance reference returned by this capability is defined minimally and immutably (see below); it is never a copy of provenance content. |
| **Retrieve evidential history** | Belongs. | Unchanged; now explicitly ordered, non-forking, and inclusive of every revision's own disclosure record (Section 3). |
| **Staleness disclosure** | *(New — Amendment 7.)* Not a separate, opt-in query capability. | Staleness is not something a caller retrieves by asking a distinct question — it is a mandatory component of every Knowledge Result and every provenance-bearing response this interface returns (Section 3). Treating it as a separate, optional capability would recreate exactly the "caller must remember to check" risk the Adversarial Review identified; it is instead specified as a property every relevant response always carries. |

**Provenance reference — minimum immutable characteristics.** *(New — Amendment 6.)* A provenance reference contains only the identifier necessary to address the underlying Memory Core provenance record, and nothing else — no provenance field content (source type, acquisition time, creator, or any other provenance attribute) may be embedded within it under any circumstance. Once issued, a provenance reference is immutable: it addresses the same target for as long as it exists and may never be silently repointed to a different provenance record. Ownership of provenance remains with Memory Core regardless of how the reference is used or how often it is the practical path by which a caller encounters provenance; nothing in this design implies or permits an ownership transfer to Knowledge Memory. Memory Core's own retrieval surface remains directly reachable by any caller needing full provenance detail — a provenance reference is a pointer a caller may follow there, never a substitute for going there.

---

## 7. Memory Core Interaction

**Knowledge Memory references Memory Core. Memory Core never depends upon Knowledge Memory.** Unchanged as a governing statement. The permission-boundary description beneath it is revised for Amendment 8.

*(Revised — Amendment 8.)* Two distinct permission evaluations exist, each with a non-overlapping responsibility, sharing one enforcement mechanism (the Permission Engine) rather than constituting two independent boundaries:

- **Evaluation A**, at Memory Core's own boundary, gates whether the referenced evidence may exist in Memory Core at all — creation, amendment, dispute, or a sensitive read of that evidence. This evaluation is settled at the time the evidence itself was created, amended, or read in Memory Core, and is never re-evaluated by anything at the Knowledge Memory layer.
- **Evaluation B**, at Knowledge Memory's own submission boundary, gates only the act of submitting a Knowledge Candidate that references already-recorded evidence. This is a fresh evaluation of the submission act itself — it never re-litigates Evaluation A's already-settled outcome, and it must not be implemented as a repeated check of the same authorization Evaluation A already granted. A caller may hold authorization to record evidence in Memory Core without holding authorization to submit Knowledge Candidates, and the reverse is structurally impossible (a Knowledge Candidate cannot exist without evidence Evaluation A already approved) — these are two genuinely different acts, not one act checked twice.

This replaces Version 1's "single Trust boundary, not a second one" language, which the Adversarial Review correctly identified as inaccurate against the design's own description. The corrected guarantee is: **one enforcement mechanism, invoked at two distinct, non-overlapping, non-re-litigating evaluation points** — never a single boundary, and never two boundaries capable of producing a contradictory outcome for the same act, because no act is ever subject to both evaluations.

- Every Knowledge Item, Knowledge Candidate, and Knowledge Promotion record carries a reference to Memory Core content — never a copy — unchanged from Version 1.
- Memory Core's own contracts, lifecycle, and permission boundary are unaffected by anything in this document — unchanged.

---

## 8. Reasoning Context Interaction

Unchanged from Version 1. Reasoning Context consumes Knowledge Memory exclusively through the retrieval capability in Section 6 — issuing a Knowledge Query and receiving a Knowledge Result, which now always carries the staleness disclosure Section 3 requires. Reasoning Context never bypasses Knowledge Memory to inspect Memory Core directly; the one place a Memory Core reference is followed outside Knowledge Memory (a provenance reference, Section 6) is Knowledge Memory disclosing a pointer, not Reasoning Context routing around Knowledge Memory's own promotion boundary.

*(Note, not a formal amendment.)* The Adversarial Review's Section 2 separately observed that "relevant" in Knowledge Query's own description is not tightly defined, and flagged a risk that an implementation could read Knowledge Memory as performing task-interpretation rather than structural matching against caller-supplied criteria. This was not among the Review's eight required amendments (its Section 12), and this document does not resolve it — it is disclosed here explicitly, and again in Section 15, as a known, deliberately out-of-scope item for this revision, rather than silently fixed or silently ignored.

---

## 9. World Model Interaction

Unchanged from Version 1.

```
Knowledge Memory
        \
         v
   Reasoning Context
         ^
        /
World Model
```

No contract in this design references, queries, or is queried by any World Model contract.

---

## 10. Document Intelligence Interaction

Unchanged from Version 1. No Document Intelligence capability is designed, referenced, or assumed by any contract in this document.

---

## 11. Constitutional Guarantees

Revised for Amendments 1, 3, 6, 7, and 8; unchanged guarantees restated for completeness.

- **Provenance traceable, never owned, minimal, and immutable.** *(Revised — Amendment 6.)* Every Knowledge Item and Knowledge Candidate carries a reference sufficient to reach its originating Memory Core provenance; the reference itself contains no provenance content and, once issued, is never repointed.
- **Evidential state preserved and exclusively Knowledge-Memory-assigned, never a truth determination.** *(Revised — Amendment 1.)* No other subsystem may set or silently alter it; Knowledge Memory itself never treats it as a certification of fact.
- **Historical revision preserved, ordered, and non-forking.** *(Revised — Amendment 4.)* Every classification a Knowledge Item has ever carried, and the disclosure record that justified each one, remains retrievable in a single chronological sequence.
- **Contradiction preserved.** Unchanged.
- **Uncertainty preserved, mandatorily expressible.** *(Revised — Amendment 3.)* An evidential-state classification must be capable of honestly expressing "insufficiently supported" or "unresolved" — this is a required property of the representation, not an optional one.
- **Promotion traceability, refreshed at every revision.** *(Revised — Amendment 4.)* Every Knowledge Item's disclosure record states the basis for its current classification specifically, not only the basis for its original promotion.
- **No silent rewriting.** Unchanged; reinforced by Section 3's restoration rule (a reversal is itself a new, visible event, never an erasure of the retirement that preceded it).
- **No caller-facing promotion, and no caller-supplied confidence or evidential state.** *(Revised — Amendment 2.)* A Knowledge Candidate may be submitted; neither its promotion, nor the confidence or evidential-state values eventually attached to it, are ever supplied or guaranteed by the submitter.
- **Two non-overlapping permission evaluations, one enforcement mechanism.** *(Revised — Amendment 8, replacing Version 1's "single Trust boundary" language.)* No act is ever subject to both evaluations; no divergence between them is possible because their responsibilities do not overlap.
- **No document-specific leakage.** Unchanged.
- **Staleness always disclosed.** *(New — Amendment 7.)* No Knowledge Result may omit or imply staleness status; a caller is never required to separately ask whether returned knowledge remains current.

---

## 12. Contract Inventory

Revised for Amendments 1, 2, 4, 6, and 7 in the rows noted; unchanged rows restated for completeness.

| Contract | Kind | Purpose | Responsibility | Dependencies | Constitutional obligations |
| --- | --- | --- | --- | --- | --- |
| **Knowledge Item** | Record | Represents a single piece of promoted, durable knowledge | Carries a Memory Core evidence reference, an evidential-state classification (never a truth determination), a chronologically ordered, non-forking history of Knowledge Promotion/revision/retirement/restoration disclosure records, and its own current lifecycle status | Memory Core (referenced, never copied) | Provenance traceability, evidential-state preservation, historical revision preservation (ordered, non-forking), contradiction preservation, staleness disclosure |
| **Knowledge Reference** | Read-oriented handle | Lightweight identifier and summary for a Knowledge Item | Points to exactly one Knowledge Item; carries no independent state | Knowledge Item | No silent rewriting |
| **Knowledge Candidate** | Submission record | The caller-facing proposal that evidence be evaluated for promotion | Carries a Memory Core evidence reference only — *(revised, Amendment 2)* **no caller-settable confidence or evidential-state field of any kind** | Memory Core (the evidence it references must already exist there) | No caller-facing promotion; no caller-supplied confidence/evidential state; promotion boundary (Section 5) |
| **Knowledge Promotion** | Disclosed, read-only record | Documents the basis for a Knowledge Item's classification | *(Revised, Amendment 4)* One such record exists per promotion **and per subsequent revision** — never only at initial promotion; each is attached to exactly one classification event and never independently altered afterward | Knowledge Item, Memory Core evidence it cites | Promotion traceability (refreshed per revision), no silent rewriting |
| **Knowledge Query** | Request contract | Expresses a task-scoped request for relevant promoted knowledge | Describes what Reasoning Context is asking for; carries no retrieval logic itself | None | None directly |
| **Knowledge Result** | Response contract | Expresses what Knowledge Memory returns for a given Knowledge Query | Bundles Knowledge References/Items, evidential-state disclosures, unresolved-competition disclosures, provenance references, and — *(new, Amendment 7)* **a mandatory staleness disclosure for every included item** | Knowledge Item, Knowledge Reference | Uncertainty preserved, contradiction preserved, provenance traceable, staleness always disclosed |
| **Knowledge Submission (interface)** | Write interface | The single public path through which a Knowledge Candidate is submitted | Performs no promotion decision itself | Memory Core's permission boundary (Evaluation A, inherited); its own submission-act evaluation (Evaluation B, Section 7) | No caller-facing promotion; two non-overlapping permission evaluations |
| **Knowledge Retrieval (interface)** | Read interface | The single public path through which a Knowledge Query is answered | Performs no write of any kind; answers strictly from already-promoted Knowledge Items, always with staleness disclosure | Memory Core (for forwarded, minimal, immutable provenance references only) | Reasoning Context never bypasses Knowledge Memory; staleness always disclosed |

**Explicitly not inventoried, and why:** unchanged from Version 1 — Knowledge Revision (folded into Knowledge Item's own lifecycle); any Memory Core record type; any World Model, Conversation History, Document, or Planner-facing contract.

---

## 13. Out of Scope

Unchanged from Version 1: implementation, persistence, indexing, caching, optimisation, vector storage, embeddings, OCR/document extraction mechanics, Planner integration, Workflow Engine integration. Additionally, and consistent with this scope: the specific serialization rule for concurrently-arriving revisions (Section 3), the specific mechanism for staleness detection (Section 3, Section 6), and the specific governing-rule exception procedure for single-factor promotion weighing (Section 5) are all Scope Lock or implementation decisions — this document fixes that each must exist and be applied consistently, not how.

---

## 14. Amendment Validation

| # | Review finding addressed | Contract section(s) updated | Constitutional article(s) satisfied | Ambiguity removed |
| --- | --- | --- | --- | --- |
| 1 | Unconstrained promotion-factor weighing permitted single-factor promotion and reproduction of the World Model's own CT-EI-48 defect | §1, §3 (Promotion), §5, §11 | Article XI §1 (no undeclared single factor), Article IV (evidential-state classification, not truth) | "How factors are weighed is implementation, not design" replaced with a binding multi-factor, independence-aware requirement; promotion explicitly disclaimed as never a truth determination |
| 2 | Unsourced "confidence" factor risked submitter self-certification | §2, §3 (Promotion), §4, §5, §11, §12 (Knowledge Candidate, Knowledge Item) | Article XV (no subsystem determines its own evidential status), Article VII | Knowledge Candidate now explicitly carries no caller-settable confidence or evidential-state field; source of confidence fixed to Memory Core's own record or Knowledge Memory's own evaluation, with explicit handling for the case neither can establish one |
| 3 | Permissive "may express unresolved" left uncertainty-expressibility optional | §4, §11 | Article XIII (no false precision; uncertainty must remain expressible) | "May" replaced with a binding "must be capable of expressing an insufficiently-supported/unresolved outcome," stated as a Contract Design requirement, not an option |
| 4 | Unordered/forkable revision history; single, non-refreshed disclosure record | §2, §3 (Revision, Supersession), §11, §12 (Knowledge Promotion) | Article XVI (revision without erasure; honest record of what was known when) | History fixed as single, chronologically ordered, non-forking; concurrent triggers required to serialize by one consistent rule; every revision now produces its own disclosure record |
| 5 | Undefined retirement reversibility | §1, §3 (Retirement) | Articles XVI/XVII | Retirement explicitly never implies deletion; reversible via a new, visible "restoration" event except where the underlying evidence was itself erased, in which case a new promotion (not restoration) is the only path, mirroring Memory Core's own erasure boundary |
| 6 | Undefined reference shape permitted hidden copies, mutable references, implicit ownership transfer | §2, §6, §11, §12 | Articles VIII/IX (provenance remains Memory Core's exclusive, non-duplicated record) | Provenance reference now defined as identifier-only, immutable once issued, with an explicit statement that ownership never transfers and Memory Core's own retrieval surface remains directly reachable |
| 7 | Knowledge could silently outlive the validity of its underlying evidence | §3 (Staleness, new), §6, §11, §12 (Knowledge Result) | Article XIII (uncertainty/changed status must not be concealed) | Staleness disclosure made a mandatory, always-present component of every Knowledge Result, not a separate capability a caller must remember to invoke |
| 8 | "Single Trust boundary" claim contradicted the design's own description of two checks; divergence/re-litigation risk under policy drift | §7, §11, §12 | Parker Constitution's trust-authorises discipline (predictable, non-contradictory authorization) | Two evaluations explicitly named (Evaluation A: evidence: Evaluation B: submission act), declared non-overlapping and non-re-litigating; guarantee language corrected from "single boundary" to "one enforcement mechanism, two non-overlapping evaluation points" |

---

## 15. Regression Review

A genuine self-check against all eight original findings, performed after drafting the above — not asserted without re-examination.

1. **Promotion weighting** — resolved at the Contract Design level: the binding multi-factor, independence-aware requirement is now present, and the truth/classification distinction is stated explicitly in three separate sections (1, 3, 5), not once. The specific weighting algorithm remains, appropriately, a Scope Lock matter — the *constraint* on it, which is what the Adversarial Review actually required, is now binding.
2. **Confidence sourcing** — resolved, including the residual case the Adversarial Review did not itself raise but that this revision's own drafting surfaced: what happens when neither Memory Core nor Knowledge Memory's own evaluation can establish a confidence value. Section 3 and Section 4 now both require the resulting classification to express that absence rather than inventing a figure, closing what would otherwise have been a new gap introduced by this very fix.
3. **Uncertainty expression** — resolved; mandatory language now governs both Section 4 and Section 11.
4. **Revision history** — resolved architecturally (ordering, non-forking, per-revision disclosure, multi-hop supersession). One item is intentionally left open, not overlooked: the specific tie-breaking rule for concurrently-arriving evidence is deferred to Scope Lock, consistent with this document's own discipline of fixing requirements rather than mechanisms — disclosed here rather than left implicit.
5. **Retirement reversibility** — resolved cleanly; the erasure-caused exception was reasoned through explicitly rather than left as a loose end, and mirrors Memory Core's own existing erasure boundary rather than inventing a new one.
6. **Provenance reference** — resolved; all four sub-concerns the Adversarial Review raised (hidden copies, staleness, mutability, implicit ownership transfer) are addressed individually in Section 6's new definition.
7. **Staleness** — resolved; deliberately specified as a disclosure requirement rather than a detection mechanism, keeping this document architectural rather than implementation-prescriptive, per this Programme's own constraints.
8. **Permission boundary** — resolved; the internal contradiction is corrected, and the two evaluations are now defined with genuinely non-overlapping responsibilities rather than merely being asserted not to overlap.

**One item is knowingly not resolved by this revision, and is disclosed rather than concealed:** the Adversarial Review's Section 2 observation that Knowledge Query's own "relevant" wording could be read as authorizing task-interpretation inside Knowledge Memory, rather than structural matching against caller-supplied criteria, was not among the Review's eight required amendments and remains open. It should be carried forward as a candidate finding for the next adversarial pass or for Scope Lock's own attention, not treated as resolved by anything in this document.

No other residual weakness was found on re-examination of the original eight.

---

## Final Recommendation

```
READY FOR FINAL ADVERSARIAL CONFIRMATION
```

All eight required amendments are incorporated, each traced individually in Section 14 to the specific review finding it resolves, the section changed, the Article(s) satisfied, and the ambiguity removed. Section 15's regression review found no unresolved item among the original eight, and surfaced and closed one further gap (the confidence-absence handling in Amendment 2) that emerged only while drafting the fix itself — evidence this revision was genuinely re-examined, not merely asserted complete. Consistent with this Programme's own established discipline — that the author of a fix is not the right party to certify it — this document does not declare itself `READY FOR SCOPE LOCK` outright. It recommends a further, independent adversarial pass to confirm the eight resolutions actually hold under the same hostile reading the first review applied, and to assess the one knowingly-deferred item (Section 8's Reasoning Context "relevance" wording) on its own merits before Scope Lock begins.

---

## 16. Constitutional Remediation Programme — Phase 1 Amendment 5 (Explicit Request Clarification)

**Status.** This section is a governance amendment, added under the
Parker Constitutional Remediation Programme, Phase 1, Amendment 5. It is
additive only. It does not modify, reopen, or reinterpret Sections 1–15
or the Final Recommendation above, including this document's own,
internal "Amendment 5" (Section 3, Retirement) — a distinct, unrelated,
prior revision to this same document, using this document's own internal
amendment-numbering scheme. To avoid any confusion between the two, this
section is never referred to below as bare "Amendment 5" without the
"Phase 1" qualifier, and the Remediation Programme's own Amendment 5 is
this Section 16 alone.

### 16.1 Constitutional Ambiguity

Section 2's Knowledge Candidate row states that a Knowledge Candidate
"carries a reference to existing Memory Core evidence and nothing else
evidential — explicitly, a Knowledge Candidate carries no caller-settable
confidence value and no caller-settable evidential-state value." This
text is genuinely ambiguous between two readings: (a) "nothing else
evidential" is exhaustively cashed out by the two named examples
(confidence, evidential-state) — meaning only caller-asserted
evidential-weight or evidential-classification content is excluded; or
(b) "nothing else evidential" categorically excludes every field beyond
the bare evidence reference, with confidence and evidential-state named
only as the two most important instances. Until resolved, whether a
Knowledge Candidate may lawfully carry an explicit-request indication —
which its legacy predecessor, `CandidateMemory`, already did — cannot be
determined.

### 16.2 Constitutional Intent

Reading (a) is the correct reading of existing constitutional intent,
for three reasons drawn directly from repository governance:

1. The sentence's own construction — a general clause followed by
   "explicitly" and two named examples — introduces confidence and
   evidential-state as the specific content being excluded, not as
   illustrative instances of a broader, unstated category.
2. The stated reason for the exclusion is that "Knowledge Memory
   computes both itself" (Section 2) and that no subsystem may
   "determine the final evidential status of their own outputs" (Article
   XV). This reasoning applies to claims about evidential weight or
   classification specifically. An explicit-request indication is not a
   claim about evidential weight, confidence, or evidential-state — it
   is a factual claim about the circumstances of submission (whether a
   user directly asked Parker to remember something), architecturally
   distinct from an evidential judgment in the same way Section 1
   already distinguishes promotion (a durability-worthiness judgment)
   from truth.
3. Decisively: Section 5 itself names "explicit request" as one of the
   six required promotion factors Knowledge Memory must weigh. Reading
   (b) would make that named factor permanently unusable by
   construction, since no Knowledge Candidate could ever carry the fact
   Section 5 requires Knowledge Memory to weigh — an internal
   contradiction between Section 2 and Section 5 that reading (a)
   avoids entirely.
4. The legacy `docs/architecture/MEMORY_CONTRACT_DESIGN.md` describes
   the explicit-request flag as "architecturally different **evidence**
   than Parker noticing a pattern on its own," which could superficially
   be read as supporting reading (b). This reflects that document's own,
   older terminology, predating the evidential-state taxonomy (Article
   IV) this Programme later adopted, not Contract Design V2's own
   controlled vocabulary. Contract Design V2 §5 itself draws the
   relevant distinction: it labels the six named factors "promotion
   criteria" and reserves the word "evidence" exclusively for the
   underlying Memory Core record being evaluated against them — never
   for the criteria themselves. Read against Contract Design V2's own,
   current vocabulary, the legacy document's word choice does not
   establish that explicit request is "evidential" within the meaning
   of Section 2.

This clarification changes no existing constitutional intent; it removes
an ambiguity in wording that already, correctly, permitted an
explicit-request indication.

### 16.3 Constitutional Guarantees

1. **Permitted, non-evidential field.** A Knowledge Candidate may carry
   an indication of whether its submission was made in direct response
   to an explicit user request. This is not "evidential" content within
   the meaning of Section 2's existing exclusion, and carrying it does
   not render a Knowledge Candidate malformed.
2. **No evidential authority created.** An explicit-request indication
   never contributes to, substitutes for, or overrides the confidence or
   evidential-state values Knowledge Memory computes exclusively under
   Sections 2 and 4. Knowledge Memory's exclusive authority over those
   two values is entirely unchanged by this section.
3. **No single-factor promotion.** The presence of an explicit-request
   indication does not, by itself, determine promotion. It remains one
   of the six factors Section 5's existing multi-factor, independence-
   aware weighing discipline governs, subject to the same prohibition on
   any single factor determining promotion absent an express, documented
   governing-rule exception.
4. **Honest absence.** Where a submission was not made in direct
   response to an explicit request, or where this cannot be determined,
   that fact must be represented honestly — never defaulted to "explicit
   request" and never fabricated in either direction.
5. **Technology independence.** No specific representation (a boolean, an
   enumeration, a timestamped record, or otherwise) is named, required,
   or precluded by this section; any representation satisfying the
   guarantees above is permitted.

### 16.4 Constitutional Boundaries

An explicit-request indication does not:

- constitute evidence of the truth of the underlying proposition;
- contribute, numerically or otherwise, to a computed confidence value;
- contribute to, or substitute for, an evidential-state classification;
- by itself determine promotion (Section 5's multi-factor discipline
  applies unchanged);
- grant a caller any override authority over Knowledge Memory's own
  computed values (Section 2's existing malformation rule for
  caller-declared confidence or evidential-state remains entirely
  unchanged and unaffected by this section).

### 16.5 Ownership and Consumers

Ownership is unchanged: the Knowledge Candidate contract slot belongs to
Knowledge Memory, exactly as Section 2 already establishes; the value
carried in that slot is supplied by whichever subsystem constructs the
candidate on the submitter's behalf. The only authorised consumer of an
explicit-request indication is Knowledge Memory's own promotion
evaluator (Section 5) — the same consumer already established for the
other five named promotion factors. No new consumer is authorised by
this section. Where an explicit-request indication contributes to a
promotion decision, the same promotion evaluator remains the party
responsible for the promotion-basis disclosure Section 3 already
requires — this section introduces no separate disclosure mechanism and
does not alter Section 3's existing requirement in any way.

### 16.6 Failure Semantics

At the constitutional level only: where a submission's explicit-request
status is genuinely unknown to the subsystem constructing the Knowledge
Candidate, that uncertainty must be represented honestly, consistent
with Guarantee 4 above. No exception, return type, status code, or API
is defined by this section.

### 16.7 Extensibility

A future implementation of this indication may use any suitable
representation, provided every guarantee in Section 16.3 continues to
hold. No specific technology, data shape, or interface is named,
required, or ruled out by this section.

### 16.8 Constitutional Consistency Check

This section has been checked against Sections 1–15 of this document
(including this document's own internal Amendment 5, Section 3
Retirement, confirmed unrelated and untouched), `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`
(Sections 1–19, including Amendments 1 and 2, both frozen and unaffected),
`docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`, `docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md`,
and `docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md`.
No inconsistency was found. This section resolves the ambiguity the
Blocker Ownership Matrix and Remediation Roadmap both identified in
Section 2, using the narrow-clarification pattern both documents
recommended, without altering Section 2's own text or any other
previously settled decision in this document.

```
PHASE 1 AMENDMENT 5 — EXPLICIT REQUEST CLARIFICATION — ADOPTED
```
