**Status:** Governance and design only. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed. No language syntax, method signature, or pseudocode appears anywhere below — every contract is described by responsibility, concept, and relationship, in prose, following the same discipline `MEMORY_CORE_CONTRACT_DESIGN.md` applied to Memory Core.

# Programme 3 — Knowledge Memory Contract Design

Programme: **Programme 3 — Knowledge Memory Contract Design.**

This document accepts `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md`'s conclusion (`READY FOR KNOWLEDGE MEMORY CONTRACT DESIGN`) as frozen and does not reopen it. It also treats `docs/architecture/parker-constitution.md`, `user-authorship-and-evidence.md`, `reasoning-context.md`, `epistemic-integrity.md`, `MEMORY_CORE_GOVERNANCE_REVIEW.md`, `MEMORY_CORE_CONTRACT_DESIGN.md`, and `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md` as authoritative and does not contradict any decision they already made. The layering the Governance Review fixed is restated here only as a constraint this design must satisfy:

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

**The constitutional principle governing every contract below:** *Memory Core remains the constitutional source of stored evidence. Knowledge Memory becomes the constitutional source of promoted knowledge. Knowledge Memory exposes knowledge; it never exposes storage mechanics, never duplicates Memory Core's responsibilities, and never owns provenance, identity, or relationships.* Every design choice below is checked against this sentence directly, not merely asserted to satisfy it.

---

## 1. Responsibilities

**Knowledge Memory is responsible for:**

- Evaluating a candidate, submitted with reference to Memory Core evidence, against promotion criteria, and deciding whether it becomes durable, retrievable knowledge.
- Holding the record of what has been promoted, including the evidential-state classification (Article IV) that promotion attaches to it.
- Re-evaluating a promoted item when new Memory Core evidence bearing on the same proposition arrives, and preserving what was previously understood alongside the update.
- Answering task-scoped knowledge queries for Reasoning Context assembly — the only path through which Reasoning Context obtains promoted knowledge.
- Disclosing, honestly, when evidence does not justify a single conclusion — surfacing unresolved competing propositions rather than forcing a premature winner.

**Knowledge Memory is explicitly not responsible for:**

- Identity. It does not mint, assign, or own identifiers for Entities, Documents, or Evidence Items — those remain Memory Core's alone.
- Provenance. It never records, computes, or stores a provenance record of its own; it references Memory Core's.
- Relationships. `SUPPORTS`/`CONTRADICTS`/`DISPUTES`/`AMENDS`/`SUPERSEDES` and any other typed relationship between evidential records remain exclusively Memory Core's record type; Knowledge Memory reads them, never mints them.
- Permission evaluation. It performs no `PermissionEngine` check of its own; it inherits the single Trust boundary Memory Core already enforces (Section 7).
- Storage mechanics of any kind — persistence, indexing, caching, or retrieval optimisation are not this design's concern (Section 13).
- Truth. Promotion is a durability-worthiness judgment, never a verdict that a proposition is true. A promoted Knowledge Item is no more "true" than an unpromoted Memory Core Assertion — it is simply retained.
- Anything belonging to World Model, Conversation History, Document Intelligence, the Planner, or Workflow Engine (Sections 9, 10, 13).

---

## 2. Public Model

Each candidate concept from this Programme's own brief, and one addition this document discloses explicitly, evaluated on its own merits — not assumed to belong merely because it was named.

| Candidate concept | Determination | Reasoning |
| --- | --- | --- |
| **Knowledge Item** | **Belongs.** | The central record of promoted knowledge — a proposition Knowledge Memory has decided is worth durable retention, carrying a reference to the Memory Core evidence it was promoted from and its own evidential-state classification. Without this concept, Knowledge Memory has nothing to expose. |
| **Knowledge Reference** | **Belongs.** | A lightweight, read-oriented handle to a Knowledge Item — an identifier plus enough summary detail to be useful inside an assembled Reasoning Context without requiring a full Knowledge Item fetch for every mention. Mirrors the identifier/record distinction Memory Core already draws for its own record types. |
| **Knowledge Query** | **Belongs.** | The shape of a task-scoped request for relevant promoted knowledge — the sole means by which Reasoning Context assembly asks Knowledge Memory anything (Section 6). |
| **Knowledge Result** | **Belongs.** | The shape of what a Knowledge Query returns — a bounded set of Knowledge References or Items, plus the disclosures Section 11's constitutional guarantees require (unresolved competition, evidential state, provenance references). |
| **Knowledge Promotion** | **Belongs, narrowed.** | Not a caller-invocable operation — promotion has never been caller-facing, in today's architecture or in the Reconciliation's own preserved decision (`MEMORY_ARCHITECTURE_RECONCILIATION.md` §12), and this design does not reopen that. It belongs instead as a disclosed, read-only record attached to a Knowledge Item: what evidence justified promotion, when, and against which criteria — an auditable trace, not an action a caller performs. |
| **Knowledge Revision** | **Does not belong as a standalone concept.** | A separate "Revision" type would duplicate what Memory Core's own `Relationship` record (`AMENDS`/`SUPERSEDES`) already expresses, and this design's own governing principle forbids Knowledge Memory from inventing a parallel relationship mechanism. Revision is instead an aspect of a Knowledge Item's own lifecycle (Section 3), expressed by linking to the Memory-Core-owned relationship that justified it, never by a Knowledge-Memory-native type. |
| **Knowledge Candidate** *(this document's disclosed addition, not named in the original brief)* | **Belongs, and is necessary.** | Something must be the caller-facing submission a promotion evaluation is performed against — mirroring how `MEMORY_CORE_CONTRACT_DESIGN.md` disclosed the `MemoryCore` write interface beyond what its own Governance Review had named. Without an explicit submission concept, "promotion" would have no defined input, leaving a silent gap in the contract surface. |

**Two interface-level contracts follow from the above** (named generically here; Section 12 inventories them): one through which a Knowledge Candidate is submitted, and one through which a Knowledge Query is answered. This mirrors Memory Core's own settled write/read split (`MemoryCore` / `MemoryRetrieval`) and is not a new pattern invented for Knowledge Memory.

---

## 3. Knowledge Lifecycle

Restated at contract-design granularity from the Governance Review's own Section 6, checked here specifically against Epistemic Integrity.

- **Promotion.** A Knowledge Candidate, referencing existing Memory Core evidence, is evaluated against promotion criteria. If it meets them, a Knowledge Item is created, carrying an evidential-state classification (Section 4) and a Knowledge Promotion record disclosing the basis for that decision. A candidate that is not promoted produces no Knowledge Item and no Knowledge Memory record at all — its underlying evidence remains exactly where it already was, inspectable in Memory Core on its own terms. Knowledge Memory does not need to keep a log of rejected candidates to remain constitutionally honest: it never claims completeness, only that what it does hold was genuinely promoted.
- **Revision.** New Memory Core evidence bearing on an already-promoted Knowledge Item triggers re-evaluation. The result is a new evidential-state classification, linked to the Memory Core relationship (typically `SUPPORTS` or `CONTRADICTS`, applied to the underlying Assertions) that justified the change. The prior classification is retained and remains reachable, not overwritten — satisfying Article XVI without Knowledge Memory needing a bespoke history mechanism of its own.
- **Supersession.** When Memory Core marks the underlying Assertion `SUPERSEDED`, the dependent Knowledge Item is re-evaluated against the superseding evidence. The superseded Knowledge Item is retained, marked accordingly, and remains retrievable (Section 6) — it is not deleted, mirroring Memory Core's own non-erasing lifecycle exactly rather than inventing a separate one.
- **Contradiction.** Where Memory Core holds an unresolved `CONTRADICTS`/`DISPUTES` relationship between two Assertions each capable of supporting a promotable proposition, Knowledge Memory must not promote one and silently omit the other. The constitutionally correct outcome is a Knowledge Item (or a pair of Knowledge Items) whose evidential-state classification honestly discloses that the matter is unresolved — never a confident single promotion manufactured by tie-breaking on an incidental promotion factor.
- **Retirement.** A Knowledge Item's underlying evidence may be withdrawn (for example, through Memory Core's owner-requested deletion path). Retirement marks the Knowledge Item as no longer current; it does not erase the historical fact that the promotion once existed, except in the one case Memory Core's own lifecycle already reserves exclusively for owner-requested erasure — Knowledge Memory does not invent a second erasure path more permissive than the one Memory Core already gates carefully.
- **Historical preservation.** No lifecycle transition above is expressed as an in-place mutation. Every transition is a new, linked act — consistent with, and never more permissive than, Memory Core's own inherited correction-not-destruction principle.

---

## 4. Evidential State

- **Where it is stored.** On the Knowledge Item itself, as an attribute of the promoted record — never on the underlying Memory Core evidence. Memory Core records raw evidential facts without classifying what they collectively support; that evaluative classification belongs at the layer that performs evaluation, which is Knowledge Memory's own defining responsibility (Section 1), not Memory Core's.
- **Who assigns it.** Knowledge Memory's own promotion evaluation, at the moment a Knowledge Candidate is promoted. No caller supplies an evidential-state value directly — a Knowledge Candidate proposes evidence for evaluation, it does not propose its own conclusion, exactly as a submitter cannot self-assign truth-status to a Memory Core Assertion.
- **Who may revise it.** Only Knowledge Memory's own re-evaluation process (Section 3, "Revision"), triggered by new Memory Core evidence. Neither Reasoning Context, nor a reasoning provider, nor the World Model may alter a Knowledge Item's evidential-state classification — this is the same separation Article XV requires between what a reasoning provider proposes and what may be constitutionally claimed.
- **Who may read it.** Anything with legitimate, permission-gated read access to the Knowledge Item — principally Reasoning Context assembly (Section 8), and, for disclosure purposes, a future Representation-layer consumer (Section 11's promotion-traceability guarantee exists specifically so this remains possible). No algorithm for computing the classification is specified here; only where it lives, who may change it, and who may see it.

---

## 5. Promotion Boundary

**What qualifies evidence for promotion:**

- The evidence must already exist as a Memory Core record (Entity, Document, or Assertion) with valid, mandatory provenance already attached — Knowledge Memory never promotes evidence that does not yet exist in Memory Core, and never accepts a submission that supplies its own provenance in place of a Memory Core reference.
- The evidence must meet the promotion criteria already established for this layer (repetition, importance, relevance, frequency, confidence, explicit request) — restated here as the boundary's own governing factors, not as an algorithm; how those factors are weighed is implementation, not design.
- The evidence's `ContentNature`, however it is classified (including `UNKNOWN`), is carried forward honestly into the resulting evidential-state classification — an unresolved or unknown content nature is a valid input to promotion, provided the resulting Knowledge Item discloses that status rather than upgrading it silently.

**What never qualifies:**

- Evidence with no Memory Core provenance reference. There is no path by which Knowledge Memory records something Memory Core has not already recorded.
- Evidence whose Memory Core record has been deleted through the owner-requested erasure path. Once gone from Memory Core, it cannot be freshly promoted into Knowledge Memory — retirement (Section 3) governs what happens to a Knowledge Item that already existed before such a deletion, but no new promotion may draw on evidence that no longer exists.
- A caller's direct request to "promote this." Promotion remains, unconditionally, an internal decision made by Knowledge Memory's own evaluation process — a Knowledge Candidate may be submitted, but its promotion is never guaranteed, requested, or forced by the submitter.
- A caller's assertion of an evidential-state classification supplied alongside a candidate. Knowledge Memory computes the classification itself, at promotion or revision time; it never accepts one as given.

---

## 6. Retrieval Contracts

Capabilities only, evaluated individually against whether Reasoning Context genuinely needs them.

| Candidate capability | Determination | Reasoning |
| --- | --- | --- |
| **Retrieve promoted knowledge relevant to a task** | **Belongs — the primary capability.** | This is what a Knowledge Query exists for; without it, Reasoning Context has no way to obtain durable knowledge at all. |
| **Retrieve competing propositions** | **Belongs.** | Where Article IV/VII require disclosing that evidence does not justify exclusivity (Section 3, "Contradiction"), Reasoning Context must be able to receive both sides, not just whichever Knowledge Item happens to exist. |
| **Retrieve contradictory knowledge** | **Folded into the capability above, not a separate one.** | A specific Memory-Core-sourced `CONTRADICTS` relationship is one *cause* of a competing-propositions result, not a materially different query shape. Exposing two near-identical capabilities under different names would itself be a form of duplication this design's governing principle forbids. |
| **Retrieve superseded knowledge** | **Belongs, as a distinct, explicitly historical capability.** | Ordinary task-scoped assembly should return current knowledge by default; a separate, deliberate query for superseded Knowledge Items serves Article XVI's own preservation requirement and audit/disclosure needs, without polluting every ordinary query with historical noise. |
| **Retrieve provenance references** | **Belongs, narrowly.** | A Knowledge Result may include a reference to the Memory Core provenance behind a Knowledge Item — never the provenance content itself. A caller wanting full provenance detail follows that reference to Memory Core's own retrieval surface directly (Section 8) — Knowledge Memory forwards a pointer; it does not reproduce what the pointer points to. |
| **Retrieve evidential history** | **Belongs.** | The trail of evidential-state classifications a Knowledge Item has carried over time (Section 3, "Revision") must be retrievable, not merely retained internally — otherwise Article XVI's preservation guarantee would be unobservable and therefore unverifiable. |

---

## 7. Memory Core Interaction

**Knowledge Memory references Memory Core. Memory Core never depends upon Knowledge Memory.** This is a strict, one-directional dependency, restated here as a binding design constraint, not merely a preference:

- Every Knowledge Item, Knowledge Candidate, and Knowledge Promotion record carries a reference to Memory Core content — never a copy of it. Provenance, identity, and relationship data are read from Memory Core at the moment they are needed and are never duplicated into a Knowledge Memory-owned field.
- Knowledge Memory performs no permission evaluation of its own. It inherits the single Trust boundary Memory Core already enforces at its own read/write surface — a Knowledge Candidate submission is gated exactly once, at Memory Core's boundary, for the evidence it references, plus once at Knowledge Memory's own submission boundary for the act of submitting a candidate itself (mirroring the Reconciliation's own answer to "who may create Knowledge," §12: the same `WRITE` check, applied at Knowledge Memory's own boundary, itself downstream of Memory Core's).
- Memory Core's own contracts, lifecycle, and permission boundary are unaffected by anything in this document. No contract designed here adds a field, event, or obligation to any Memory Core contract.

---

## 8. Reasoning Context Interaction

Reasoning Context consumes Knowledge Memory exclusively through the retrieval capability in Section 6 — issuing a Knowledge Query and receiving a Knowledge Result. **Reasoning Context never bypasses Knowledge Memory to inspect Memory Core directly.** This holds without exception for ordinary task-scoped assembly: no contract designed in this document accepts a Memory Core identifier as an alternate entry point that would let a caller route around Knowledge Memory's own promotion boundary.

The one place a Memory Core reference is genuinely followed outside Knowledge Memory (Section 6, "Retrieve provenance references") is not Reasoning Context bypassing Knowledge Memory — it is Knowledge Memory disclosing a pointer as part of an ordinary Knowledge Result, which a separate, narrower, disclosure-oriented capability (outside Reasoning Context's own assembly role, and outside this document's scope — see Section 13) may later use to fetch full provenance content for explanation or audit purposes. Reasoning Context's own assembly step still only ever asks Knowledge Memory questions; it never asks Memory Core anything itself.

---

## 9. World Model Interaction

Reaffirmed, without modification, from the Governance Review (§7) and the Reconciliation (§10):

```
Knowledge Memory
        \
         v
   Reasoning Context
         ^
        /
World Model
```

Knowledge Memory and the World Model share no dependency in either direction. No contract in this design references, queries, or is queried by any World Model contract. The two meet only downstream, at Reasoning Context, as two independent, sibling inputs into one assembled working set. This document introduces nothing that narrows, widens, or reinterprets that boundary.

---

## 10. Document Intelligence Interaction

No Document Intelligence capability is designed here, referenced here, or assumed by any contract in this document. Future extracted evidence enters Memory Core exactly as any other evidence does — as an Entity, Document, or Assertion record with honestly disclosed `ContentNature` (`EXTRACTED`, `SUMMARISED`, `INFERRED`, or `UNKNOWN`, as appropriate) and mandatory provenance. Once such a record exists in Memory Core, it becomes eligible for Knowledge Memory's ordinary promotion evaluation (Section 5), with no distinct code path, contract, or capability specific to documents. No contract inventoried in Section 12 mentions documents, extraction, OCR, or any Document Intelligence concept by name — this is a deliberate absence, confirming Knowledge Memory has, and needs, no document-specific behaviour at all.

---

## 11. Constitutional Guarantees

The complete list Knowledge Memory's contracts must provide, determined against Epistemic Integrity directly, not merely asserted:

- **Provenance traceable, never owned.** Every Knowledge Item and Knowledge Candidate carries a reference sufficient to reach its originating Memory Core provenance; Knowledge Memory itself never becomes a second source of provenance truth.
- **Evidential state preserved and exclusively Knowledge-Memory-assigned.** No other subsystem may set or silently alter it (Section 4).
- **Historical revision preserved.** Every classification a Knowledge Item has ever carried remains retrievable (Section 6, "evidential history"); none is overwritten in place.
- **Contradiction preserved.** Competing propositions that evidence does not resolve are retrievable as such, never silently collapsed to one (Section 3, "Contradiction"; Section 6).
- **Uncertainty preserved.** An evidential-state classification may itself honestly express "insufficiently supported" or "unresolved" — Knowledge Memory's contracts must never force a more confident classification than the underlying Memory Core evidence justifies.
- **Promotion traceability.** Every Knowledge Item's Knowledge Promotion record discloses the basis on which it was promoted, so that "why does Parker know this" is always answerable, not merely "that Parker knows this."
- **No silent rewriting.** Every lifecycle transition (Section 3) is a new, linked act; nothing in this design permits an in-place mutation of a Knowledge Item's substance.
- **No caller-facing promotion.** A Knowledge Candidate may be submitted; its promotion is never directly invoked, requested, or guaranteed by a caller (Section 5).
- **Single Trust boundary, not a second one.** Knowledge Memory introduces no independent permission-evaluation logic; it inherits Memory Core's, closing off the possibility of a second, weaker gate ever being added later (Section 7).
- **No document-specific leakage.** Nothing about how evidence was acquired — document extraction or otherwise — alters how Knowledge Memory evaluates or classifies it (Section 10).

---

## 12. Contract Inventory

| Contract | Kind | Purpose | Responsibility | Dependencies | Constitutional obligations |
| --- | --- | --- | --- | --- | --- |
| **Knowledge Item** | Record | Represents a single piece of promoted, durable knowledge | Carries a Memory Core evidence reference, an evidential-state classification, a Knowledge Promotion record, and its own lifecycle status | Memory Core (referenced, never copied) | Provenance traceability, evidential-state preservation, historical revision preservation, contradiction preservation |
| **Knowledge Reference** | Read-oriented handle | Lightweight identifier and summary for a Knowledge Item, suitable for inclusion in an assembled Reasoning Context | Points to exactly one Knowledge Item; carries no independent state | Knowledge Item | No silent rewriting (a reference is only ever a pointer, never a divergent copy) |
| **Knowledge Candidate** | Submission record | The caller-facing proposal that evidence be evaluated for promotion | Carries a Memory Core evidence reference; carries no self-asserted evidential-state or provenance | Memory Core (the evidence it references must already exist there) | No caller-facing promotion; promotion boundary (Section 5) |
| **Knowledge Promotion** | Disclosed, read-only record | Documents the basis on which a Knowledge Item was promoted or revised | Attached to exactly one Knowledge Item; never independently created or altered by a caller | Knowledge Item, Memory Core evidence it cites | Promotion traceability, no silent rewriting |
| **Knowledge Query** | Request contract | Expresses a task-scoped request for relevant promoted knowledge | Describes what Reasoning Context is asking for; carries no retrieval logic itself | None (a request shape only) | None directly — governs what a Knowledge Result must be able to answer |
| **Knowledge Result** | Response contract | Expresses what Knowledge Memory returns for a given Knowledge Query | Bundles Knowledge References/Items, evidential-state disclosures, unresolved-competition disclosures, and provenance references | Knowledge Item, Knowledge Reference | Uncertainty preserved, contradiction preserved, provenance traceable |
| **Knowledge Submission (interface)** | Write interface | The single public path through which a Knowledge Candidate is submitted | Performs no promotion decision itself — only accepts a candidate for evaluation | Memory Core's permission boundary (inherited) | No caller-facing promotion, single Trust boundary |
| **Knowledge Retrieval (interface)** | Read interface | The single public path through which a Knowledge Query is answered | Performs no write of any kind; answers strictly from already-promoted Knowledge Items | Memory Core (for forwarded provenance references only) | Reasoning Context never bypasses Knowledge Memory (Section 8) |

**Explicitly not inventoried, and why:** Knowledge Revision (Section 2, folded into Knowledge Item's own lifecycle); any Memory Core record type (Entity, Document, Assertion, Provenance, Relationship — all remain exclusively Memory Core's, referenced not reproduced); any World Model, Conversation History, Document, or Planner-facing contract (Sections 9, 10, 13).

---

## 13. Out of Scope

Explicitly excluded from this design, and from Programme 3 generally, consistent with the Governance Review's own scope boundary (its Section 10):

- Implementation of any kind — no storage technology, data structure, or algorithm is specified anywhere above.
- Persistence. Whether Knowledge Items are held in memory, on disk, or elsewhere is not addressed here.
- Indexing, caching, and retrieval optimisation. Section 6 describes capabilities, never how they are made efficient.
- Vector storage and embeddings. No semantic or similarity-based retrieval mechanism is designed or implied by any contract above.
- OCR and document extraction mechanics. Section 10 fixes only the boundary; the capability itself belongs to a future Document Intelligence Programme.
- Planner integration. Knowledge Memory's relationship to Planning, if any, is not addressed by this document and is not assumed.
- Workflow Engine integration. Not addressed, not assumed.

---

## Final Recommendation

**Recommendation: further governance refinement — a dedicated adversarial Contract Design Review — before Scope Lock.** This document does not recommend proceeding directly to Scope Lock, unlike `MEMORY_CORE_CONTRACT_DESIGN.md`'s own precedent, because this Programme's own task explicitly requires that "no implementation may start until the Contract Design has survived a deliberate adversarial review for contradictions, hidden responsibilities, and constitutional leakage" — a discipline this document treats as binding, not optional, and one this repository has already applied once before, to the Epistemic Integrity amendment itself, before its own ratification.

This document identifies, as a starting brief for that adversarial review rather than as unresolved defects, the seams most likely to reward adversarial scrutiny: whether the double permission gate described in Section 7 (once at Memory Core's boundary, once at Knowledge Memory's own submission boundary) is genuinely two distinct, justified checks or an unintentional, redundant duplication of Memory Core's own Trust enforcement; whether the provenance-reference-forwarding path (Section 6, Section 8) could be exploited as a de facto bypass of Knowledge Memory's own promotion boundary if a future capability treats "fetch the referenced provenance" as equivalent to "fetch the underlying evidence"; whether folding Knowledge Revision into Knowledge Item's own lifecycle (Section 2) adequately expresses supersession chains longer than one step without silently losing intermediate history; and whether Knowledge Memory's own promotion-criteria restatement (Section 5) leaves any factor vague enough to invite an implementation to smuggle in a truth-determination the Constitution reserves against ("storage does not create truth" applies here with equal force to "promotion does not create truth").

None of these four items is asserted here as a defect — each is disclosed as a candidate weak point for the adversarial review to test directly, consistent with this document's own governing discipline of showing reasoning rather than merely asserting conformance.
