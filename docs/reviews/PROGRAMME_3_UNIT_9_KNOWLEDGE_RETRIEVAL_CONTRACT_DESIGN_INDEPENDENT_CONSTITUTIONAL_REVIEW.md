# Programme 3 Unit 9 Knowledge Retrieval Contract Design — Independent Constitutional Review

## Status

**Independent constitutional review only.** The subject document, `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`, was not modified during this review. Unit 9 was not implemented. No Scope Lock, Implementation Plan, Kotlin interface, test, or runtime wiring was created. Nothing is staged, committed, or pushed. This review does not rely on the drafting session's own completion report — every citation below was re-verified against primary text, the subject document's own line numbers, or the actual code.

---

## Repository Baseline

- **HEAD:** `b04710cf822cb2755996e43cb212376932f69a14` (`b04710c`), matching the commit named in the governing task exactly.
- **Branch:** `main`
- **Remote:** `origin` → `git@github.com:parkerai916-droid/parker-platform.git`; `origin/main` confirmed identical to local `HEAD`.
- **Working tree, confirmed before this review began:**
  ```
  ?? docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md
  ```
  Exactly the one expected file. No discrepancy.
- **Staged changes:** none.

---

## Authorities Reviewed

Read fresh, in full or in every relevant section, for this review: `docs/architecture/parker-constitution.md`; `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` (full, all 16 sections including the Phase 1 Amendment 5 extension); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` (full, all 11 sections); `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` (§3 Unit 9, §4, §5, §9, Tracking Note — previously read in full this session, re-confirmed); `docs/governance/PROGRAMME_3_UNIT_9_SCOPE_LOCK_CLARIFICATION.md` (Adopted, full, including its own precedent-basis paragraph); its Independent Constitutional Review and Defect Confirmation Review (both full); `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md` (full, previously read this session, re-confirmed); `docs/architecture/10-permission-engine.md` (full, previously read this session, re-confirmed); `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` and `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md` (both full, as precedent); the subject document, `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`, in full; `src/interfaces/KnowledgeStore.kt` (`KnowledgeItemStatus` enum and `KnowledgeItem` data class, re-confirmed by direct grep: exactly two status values, `ACTIVE` and `RETIRED`); `src/interfaces/KnowledgeSource.kt`; `src/runtime/InMemoryKnowledgeStore.kt`; `src/runtime/KnowledgeItemPersistence.kt`; `src/runtime/DefaultReasoningContextAssembler.kt`; `src/composition/ParkerRuntime.kt` (Knowledge-related wiring, re-confirmed against the prior planning reviews' own findings).

---

## Governance Vehicle Review

**A dedicated Unit 9 Contract Design is a lawful vehicle, elaborating an already-authorised capability rather than bypassing or improperly amending anything — but the subject document leaves one real tension with its own cited precedent undisclosed.**

It does not improperly amend Contract Design V2 — confirmed directly; every V2 citation in the subject document is quotation or restatement, and no V2 section's own text is altered. It does not bypass the Scope Lock — Scope Lock §5 Deliverable 9 already authorises "the Knowledge Query/Knowledge Result surface," and this document elaborates that surface's own contract-level detail without adding, removing, or reweighting any Scope Lock deliverable. It correctly elaborates an already-authorised capability rather than requiring a Scope Lock amendment: Scope Lock §8's own precedent (the concurrent-revision-ordering question left open at Scope Lock tier, resolved only that "it must be resolved consistently during implementation") establishes that this Programme already tolerates mechanism-level questions surviving past Scope Lock into later work — the subject document's own deferrals (ordering rule, staleness-detection mechanism, enforcement mechanism) are consistent with, not a departure from, that established practice.

Should it instead be a Scope Lock Clarification? No — a Clarification's own established purpose in this Programme (Unit 7, Unit 8, Unit 9's own permission Clarification) is narrow ambiguity-resolution within an existing boundary, not the level of contract-tier elaboration (query/result shape properties, ordering guarantees, a five-outcome error model) this document performs. The precedent set by `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` — a dedicated, sibling Contract Design elaborating a specific capability's contract-tier detail beyond what its own parent Contract Design fixed — is the closer, correctly-followed analogy.

**One real tension is left undisclosed.** The Unit 9 Clarification's own Section 15 (Recommendation) states: "A future Unit 9 Contract Design passage or Scope Lock Clarification amendment should record this classification within Contract Design V2 itself (mirroring how Amendment 8 named Evaluation B)." Read plainly, this names V2 amendment as the Clarification's own expected next step, mirroring exactly how Amendment 8 recorded Evaluation B inside Contract Design V2's own text rather than in a sibling document. The subject document takes a third path — a wholly separate, sibling Contract Design, with Contract Design V2 left untouched — consistent with the governing task's own explicit instruction ("Do not amend Contract Design V2"), but the subject document never states this, never acknowledges the Clarification's own stated expectation, and never explains why a sibling document satisfies that expectation as well as, or in place of, the V2 amendment the Clarification's own text anticipated. This is not a defect that makes the vehicle wrong — the governing task's own constraint controls, and CDR-005's Decision Rules (already tested for the Clarification itself) do not require the classification-recording step to occur in any one specific document — but a careful reader comparing this document against the Clarification it cites would reasonably ask why the expected V2 amendment never happened, and the document should say so rather than leave the question unaddressed.

---

## Purpose and Boundary Review

Section 1 distinguishes Knowledge Retrieval from Knowledge Submission, Promotion, Lifecycle, Reasoning Context Assembly, Memory Retrieval, and Evidence Retrieval (folded into Memory Retrieval, correctly, as the same capability under Memory Core's own vocabulary), and Section 3 separately excludes semantic reasoning. No overlap or authority drift was found in any of these six distinctions — each is reasoned from a specific governing document and does not claim any adjacent act's own responsibility.

**One naming gap, minor.** The governing task's own review checklist names "Knowledge Candidate Evaluation" as a distinction Section 1 must draw. The subject document addresses the same underlying act only under the label "Promotion" (Contract Design V2's own preferred term) and never uses the phrase "Knowledge Candidate Evaluation" or cross-references `DefaultKnowledgeCandidateEvaluator`/Unit 6 by name. The substance is present and correctly reasoned; only the explicit terminological bridge between "Promotion" and "Knowledge Candidate Evaluation" (the same act, two names in different documents) is missing. This is a clarity improvement, not a substantive gap.

---

## Public Contract Review

Section 4 remains at the properties-and-guarantees level throughout; direct search confirms no Kotlin type, method signature, field name, or interface declaration is fixed anywhere in the document, satisfying the governing task's own "without freezing Kotlin" requirement. Requesting principal, empty-result behaviour, denial behaviour, failure behaviour, and deterministic expectations are each addressed, in Sections 4, 5, 8, and 9 respectively, with sufficient precision to bind a future Implementation Plan without prescribing its shape.

**One genuine gap: query identity.** The governing task's own checklist separately names "query identity" alongside "requesting principal" as something to verify. Section 4's own Knowledge Query bullet describes only the query's *content* (structural matching criteria); nothing in the subject document addresses whether a Knowledge Query itself must carry a correlation or request identifier for auditability — the same property every other permission-evaluated act in this repository already carries (`ExecutionRequest.requestId`/`correlationId`; the legacy `KnowledgeQuery`'s own existing `correlationId` field; `DefaultKnowledgeSubmission`'s own minted request identifier). Given Section 5 already establishes retrieval as a PermissionEngine proposal class, and Chapter 10 §5's own "Traceability and auditability" guarantee requires "a record sufficient to reconstruct what was proposed, what was authorized... and what was executed" for every authorised action, this omission is a real, fillable gap, not merely a style preference.

---

## Permission Boundary Review

Confirmed directly: the Unit 9 Clarification's classification is treated as settled throughout (Section 5's own opening line: "Settled, not reopened"), never re-examined or re-derived. "Evaluation C" does not appear anywhere in the subject document as canonical terminology — confirmed by direct search; the one appearance (Section 5's third bullet) is an explicit disclaimer, mirroring the Clarification's own discipline. Policy ownership is correctly assigned to the Permission Engine alone (Section 3: "the *content* of any permission decision... is exclusively the Permission Engine's own responsibility"). Explicit principal handling is preserved (Section 5, first bullet). Self-authorisation is prevented — nothing in the document suggests Knowledge Retrieval decides its own authorisation outcome; Section 5's third bullet leaves only the *enforcement location* question open, never the *decision content* itself.

**One completeness gap, minor.** The subject document explicitly defers the enforcement-mechanism question (self-gating versus externally-gated) to a future Clarification (Section 5, third bullet; Section 11's exclusion table), but never separately names concrete resource/action representation as an equally deferred item — a distinct question the Unit 8 Clarification's own §7 treated as its own dedicated section, separate from §5's enforcement-location question. The subject document's silence on this specific point is defensible by the same reasoning that supports deferring enforcement location, but is not stated, leaving an asymmetry between what is explicitly named as deferred and what is merely never mentioned.

---

## Lifecycle and Revision Review

**Confirmed directly against the code:** `KnowledgeItemStatus` carries exactly two values, `ACTIVE` and `RETIRED`; the subject document's Section 6 states this precisely and does not invent a third status value or a new event kind — "revised" and "restored" are correctly identified as event kinds within history, not status values. Visibility is distinguished from permission explicitly (Section 6's closing paragraph). Lifecycle evaluator ownership is preserved — Section 6 attributes every lifecycle act to the (unnamed) lifecycle process, never to Retrieval itself, and Retrieval's own role is framed strictly as disclosure. No revision-selection policy is invented — Section 6 explicitly defers whether prior classifications surface on an ordinary query. Restoration semantics are not extended beyond existing authority — Section 6's "Restored" bullet restates only what Contract Design V2 §3 and the Unit 7 Clarification §9 already fix.

**One genuine gap: supersession is not addressed at all.** The governing task's own Review Question 6 separately asks whether retrieval of "superseded items" is sufficiently defined, and warns against silently assuming "latest" without defining authority or ordering. Contract Design V2 §3 treats supersession as a named, distinct lifecycle sub-concept — "supersession chains of arbitrary length are supported... the full chain — not merely the most recent hop — remains transitively retrievable as part of the Knowledge Item's evidential history" — with its own explicit multi-hop-chain retrievability requirement. Section 6 of the subject document addresses only Active, Revised, Retired, and Restored; it never mentions supersession, never states whether a superseded classification is subsumed under "Revised" (which Contract Design V2 §3 itself supports — "the dependent Knowledge Item is re-evaluated... exactly as an ordinary revision") or requires separate treatment, and never confirms multi-hop chain retrievability is preserved by anything this document fixes. This is a real, fillable gap, not an invented "latest" assumption — the document's actual "current classification reflects the most recent revision" language (Section 6, "Revised" bullet) is soundly grounded in Contract Design V2's own non-forking sequence model, not an unsupported assumption — but supersession's own silence is a genuine omission the governing task specifically asked this review to catch.

---

## Ordering and Ranking Review

Section 8 defines deterministic behaviour precisely, grounded directly in Scope Lock §7's own binding determinism requirement. No ranking algorithm is invented anywhere; Sections 2, 3, and 8 each independently and consistently exclude scored, weighted, or semantic ranking. Ordering is explicitly distinguished from ranking (Section 8's own "Ranking is not ordering" clause). No semantic relevance is promised — the same clause forecloses it directly. No indexing or caching is forced — Section 11 explicitly excludes both. No defect found in this section's own substance.

---

## Provenance Review

Section 7 confirms retrieval exposes only already-existing provenance references, never constructs new Memory Core provenance (explicit: "Knowledge Retrieval does not generate, mint, or construct a provenance reference"), never redefines provenance ownership (explicit cross-reference to Contract Design V2 §6's ownership rule), and creates no dependency on a Memory Core write (retrieval only forwards, never writes). Section 7 does not explicitly state that provenance disclosure implies neither truth nor evidential weight — a minor omission, but a low-risk one, since Contract Design V2 §1 and §5 already establish, repository-wide and independently of this document, that promotion and provenance are never truth or weight determinations; a bare identifier-only reference (as Section 7 itself defines it) has no mechanical capacity to imply either. Not flagged as a required correction.

---

## Error Model Review

Section 9's five outcomes (invalid query, unavailable data, permission denial, empty result, implementation failure) are each precisely distinguished, and the binding requirement ("no two of them may ever be represented identically to a caller") is sufficient to prevent a caller from confusing denial with emptiness — directly satisfying the governing task's own stated concern.

**A real, disclosable tension with an existing non-disclosure duty is not addressed.** `MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` §8 establishes, for Memory Core's own direct-lookup methods, the opposite convention: "`Entity?`/`Document?`/`Assertion?`/`Relationship?` already collapse 'no such record' and 'record exists, not shown' into the same, indistinguishable signal... doing so would itself leak which case occurred." Section 9 of the subject document requires the reverse for Knowledge Retrieval — permission denial must remain distinguishable from an empty result, never collapsed. On examination, this is not a genuine contradiction: Memory Core's own rule operates at per-record granularity (preventing an existence-oracle attack against a specific identifier), while Knowledge Retrieval's own classification (the Unit 9 Clarification) gates the *act* of retrieval as a whole, not any individual record's existence — a caller denied at the act level learns nothing about the store's contents, only about their own already-known permission status. The reasoning holds, but the subject document never states it; a careful reader comparing the two established conventions side by side could reasonably perceive an unexplained inconsistency, which the document should pre-empt rather than leave for a reader to resolve unaided.

---

## Legacy/V2 Boundary Review

Confirmed: the subject document does not resolve the legacy-store migration question (Section 11 explicitly excludes it, citing the prior planning review by name); does not accidentally make the legacy `InMemoryKnowledgeStore`/`KnowledgeSource` path authoritative (neither is named anywhere outside the exclusion table); does not assume V2 composition before governance authorises it (Section 10 and Section 11 both defer all runtime composition); does not treat runtime wiring as Contract Design (Section 10 explicitly assigns wiring to Runtime/Implementation Plan tier). No defect found.

---

## Reasoning Context and Runtime Boundary Review

Confirmed: the subject document does not compose results into reasoning, define prompt assembly, decide conversational invocation, or make Retrieval responsible for model context selection — Section 1 and Section 3 both explicitly and consistently exclude Reasoning Context composition, and Section 10's own "Runtime owns" list contains nothing resembling any of these four. No storage technology, cache, index, database, serializer, or runtime composition path is selected anywhere — Section 11's own exclusion table names each explicitly. No defect found.

---

## Findings

| # | Severity | Finding | Location |
| --- | --- | --- | --- |
| 1 | **Substantive, mechanical** | Approximately fourteen internal `(Section N)` cross-references point to the wrong section, consistently off by one, throughout Sections 2, 3, 4, 5, and the Explicit Exclusions table. Full list in Required Corrections. | Sections 2, 3, 4, 5, 11 |
| 2 | Moderate | The Unit 9 Clarification's own Recommendation named a future V2 amendment as an expected next step; the subject document takes a different path without disclosing or defending the divergence. | Status block, "Governing vehicle" |
| 3 | Moderate | No provision for Knowledge Query carrying a correlation/request identifier for auditability, despite this being the established pattern for every other permission-evaluated act in this repository. | Section 4 |
| 4 | Moderate | Supersession and superseded-item retrievability are never addressed, despite Contract Design V2 §3's own named, distinct treatment of supersession with its own multi-hop-chain requirement. | Section 6 |
| 5 | Minor | The Error Model's denial/emptiness distinguishability requirement is not reconciled against Memory Core's own opposite direct-lookup non-disclosure convention (Errata 004 §8) — not a genuine contradiction, but unexplained. | Section 9 |
| 6 | Minor | Resource/action representation is not separately named as deferred, unlike the enforcement-mechanism question, though the same reasoning would support deferring it. | Section 5 |
| 7 | Minor | "Knowledge Candidate Evaluation" is not named by that term (the same act is covered under "Promotion"). | Section 1 |

---

## Required Corrections

Four corrections are required before acceptance:

1. **Fix the broken cross-references.** Verified against the document's own actual section numbering (1 Purpose, 2 Responsibilities, 3 Non-Responsibilities, 4 Public Contract, 5 Permission Boundary, 6 Lifecycle Behaviour, 7 Provenance, 8 Ordering, 9 Error Model, 10 Runtime Responsibilities, 11 Explicit Exclusions, 12 Constitutional Constraints), the following references are wrong and must be corrected:
   - Section 2, "Filtering" bullet: "(Section 9, below)" → should reference Section 3 or Section 8 (ranking exclusion / ordering), not Section 9 (Error Model).
   - Section 2, "Ordering" bullet: "see Section 9" → should be Section 8.
   - Section 2, "Ranking, if any" bullet: "Section 9's own deterministic-ordering guarantee" → should be Section 8; "(Contract Design V2 §13; Section 4, below)" → should be Section 3.
   - Section 2, "Provenance disclosure" bullet: "see Section 8" → should be Section 7.
   - Section 2, "Lifecycle visibility..." bullet: "see Section 7" → should be Section 6.
   - Section 3, "Persistence implementation" bullet: "(Section 12)" → should be Section 11.
   - Section 4, Knowledge Result bullet: "(Section 10)" (denial/failure confusability) → should be Section 9; "(Section 9)" (order preservation) → should be Section 8.
   - Section 4, retrieval interface bullet: "a requesting principal (Section 6)" → should be Section 5; "a distinguishable non-result outcome (Section 10)" → should be Section 9.
   - Section 5, second bullet: "(Section 10)" → should be Section 9.
   - Section 6, "Revised" bullet: "(Section 12)" → should be Section 11.
   - Section 6, "Retired" bullet: "(Section 12)" → should be Section 11.
   - Section 11 table, "The specific ordering rule" row: "(Section 9)" → should be Section 8.
2. **Add a sentence to the "Governing vehicle" paragraph disclosing the Unit 9 Clarification's own stated expectation of a future V2 amendment, and stating explicitly why this document proceeds as a sibling Contract Design instead** (per the governing task's own explicit constraint), consistent with Finding 2.
3. **Add a query-identity requirement to Section 4's Knowledge Query bullet**, stating that a Knowledge Query must be capable of carrying whatever correlation identifier this repository's own established auditability discipline (Chapter 10 §5) already requires of any PermissionEngine proposal, without naming a field or type (Finding 3).
4. **Add supersession to Section 6**, stating explicitly whether superseded classifications are subsumed under "Revised" (as Contract Design V2 §3's own text supports) and confirming that multi-hop supersession chains remain retrievable, consistent with Contract Design V2 §3's own requirement (Finding 4).

Findings 5–7 are recommended clarifications, not required corrections — the document remains substantively sound and unambiguous without them.

---

## Constitutional Verdict

```
REQUIRES REVISION
```

The document's substantive reasoning — the purpose and boundary distinctions, the permission-boundary treatment of the Unit 9 Clarification as settled, the lifecycle model's fidelity to the actual two-value status enum, the ordering and error-model guarantees, and the explicit exclusions — is sound throughout and was independently verified against primary sources and the actual code without finding a constitutional defect. What blocks acceptance is a concrete, objectively verifiable mechanical defect (fourteen broken internal cross-references, spanning five sections) together with three moderate content gaps (an undisclosed vehicle-precedent tension, a missing query-identity requirement, and an unaddressed supersession case) — each fixable by narrow, additive correction, none requiring a different governance vehicle or a change to any conclusion this review confirmed sound.

---

## Recommended Next Step

Apply the four required corrections directly to the subject document, then request a narrow defect-confirmation review — not a full re-review — verifying only that the corrections were made correctly and that nothing else was altered, mirroring this repository's own established "narrow correction pass, then defect-confirmation review" pattern already used for the Memory Core Durability Contract Design and the Unit 9 Scope Lock Clarification. Only after that confirmation should the document's own Status move from Draft to Accepted, and only then is a Unit 9 Implementation Plan passage authorised to begin.

---

## Git Confirmations

- The subject document was not modified during this review.
- Unit 9 was not implemented.
- No Scope Lock, Implementation Plan, Kotlin interface, test, or runtime wiring was created.
- Nothing was staged during this review.
- Nothing was committed during this review.
- Nothing was pushed during this review.

## Final Git Status

```
$ git status --short
?? docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md
?? docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```
