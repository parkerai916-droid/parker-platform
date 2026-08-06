# Programme 3 — Unit 9: Retrieval Permission Evaluation — Planning Review

## Status

**Governance-first Planning Review only.** No production code, test, governance document, Contract Design, Scope Lock, Implementation Plan, or runtime composition file was modified. Nothing is staged, committed, or pushed. **"Evaluation C" is used throughout this document only as the task's own convenience label for the unresolved question — its use here is not a finding that a third, formally distinct evaluation tier is architecturally required.** This review's central discipline, per its own instruction, is to test that label against repository authority before adopting it, not to assume it.

---

## 1. Repository Baseline

- **HEAD:** `fabb2124a94eed449095b207efada804dc072ea8` (`fabb212`)
- **Branch:** `main`
- **Working tree:** one pre-existing untracked file from the immediately preceding review (`docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md`), not yet committed by the user; no other change
- **Staged changes:** none

---

## 2. Governing Documents Reviewed

Read fresh, in full, for this review: `docs/architecture/10-permission-engine.md` ("Chapter 10," full — §3 Proposal Abstraction, §5 Constitutional Guarantees, §6 Boundaries, §7 Runtime Relationship, §8 Domain Consumer Relationship, §10 Extensibility); `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §5 (Runtime Boundary) and §6 (Permission Boundary), read exactly, word for word; `docs/governance/PROGRAMME_3_UNIT_7_SCOPE_LOCK_CLARIFICATION.md` §13 (Permission-Boundary Self-Certification, in full); `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md` §5 and §12 (re-examined for its own classification method, not merely its conclusion); `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §6, §12; `src/composition/PermissionFilteredMemoryRetrieval.kt` (full — code, not description); this review's own immediately preceding planning review (`PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md`), for continuity of findings already established there.

---

## 3. Is Retrieval a Permission-Sensitive Act, in General? (Chapter 10 §3's Own Test)

Chapter 10 §3 states the general, already-existing, already-frozen-in-principle test (Chapter 10 itself remains Draft, but this is the same test already relied upon, without further invention, for both Evaluation B and the Unit 7 lifecycle acts):

> "A proposal is distinguished from ordinary internal computation by whether it carries a real, external, or state-changing consequence beyond Parker's own internal reasoning — for example, writing, amending, or superseding a durable record; **granting access to a sensitive record or capability**; or otherwise reaching beyond pure interpretation into an effect an owner would recognise as an action taken on their behalf. Ordinary internal computation — reasoning over already-available information, **structural retrieval of non-sensitive records**... — does not, by itself, constitute a proposal and does not require Permission Engine authorisation."

Chapter 10 §3 then illustrates this test using Memory Core's own precedent, characterising Memory Core Scope Lock §5 as drawing a line between a "sensitive `MemoryRetrieval` read" (gated) and "structural, non-semantic retrieval" (**"which is not gated"**, Chapter 10's own words).

**A direct check against Memory Core Scope Lock's own text and against the actual code finds this characterisation does not hold up as stated:**

- Scope Lock §5 itself, read exactly: "**Runtime owns:** every `PermissionEngine.evaluate` call required before any `MemoryCore` write and before any sensitive `MemoryRetrieval` read reaches its requester... **Memory Core owns:** ...structural, non-semantic retrieval." These are two separate clauses about two separate things — the first is about *who* invokes the Permission Engine and *when*; the second is about *what kind of matching algorithm* Memory Core itself performs (structural/non-semantic, as opposed to embeddings or semantic search). Nothing in §5's own text states that "structural" retrieval is thereby exempt from gating.
- Scope Lock §6, the very next section, states without the qualification Chapter 10 attributes to §5: "**Memory Core never evaluates permissions. Runtime performs all permission decisions before invoking Memory Core.** This applies to every operation `MemoryCore` and `MemoryRetrieval` expose, **without exception**."
- `PermissionFilteredMemoryRetrieval.kt`, read in full, confirms Scope Lock §6's "without exception" is what was actually built: every one of its eleven methods — all four direct lookups and all seven query-based methods — calls `isApproved()` (via `PermissionEngine.evaluate`) unconditionally, on every record, every time. No "sensitivity" classification, flag, or branch exists anywhere in this class. Structural, non-semantic retrieval is gated, exhaustively, in the one place this repository has actually built retrieval-side gating.

**Finding:** Chapter 10 §3's own worked example is not accurate against the document it cites. Its general test (real/external/state-changing consequence, "granting access to a sensitive record," vs. "ordinary internal computation") remains the correct, already-established test to apply — that much is sound and not in question. But its own illustration of how that test resolves for Memory Core is wrong on the facts, and this matters directly here: it means Chapter 10 §3 cannot be read as already having decided, by cross-domain analogy, that "structural retrieval" is categorically exempt from gating. The actual, built precedent this repository has for retrieval is the opposite: gate every record, every time, unconditionally.

---

## 4. Does Retrieval Inherit Memory Retrieval Filtering?

**No — not merely as a matter of unaddressed policy, but as a matter of type-level structural impossibility, confirmed directly from the code.**

`PermissionFilteredMemoryRetrieval` implements `MemoryRetrieval`, and every method on it operates exclusively over Memory Core's own record kinds (`Entity`, `Document`, `Assertion`, `Relationship`, and the `MemoryCoreRecord` sealed supertype). `KnowledgeItem` — the record kind Knowledge Retrieval would answer a Knowledge Query with — is not a `MemoryCoreRecord`, is not a Memory Core record kind at all, and passes through none of `PermissionFilteredMemoryRetrieval`'s eleven methods. There is no mechanism by which "reusing" or "inheriting" `PermissionFilteredMemoryRetrieval`'s own filtering could apply to a `KnowledgeItem` — the class has no method that accepts one, and could not gate one if it did, since its `isApproved` calls are keyed to Memory Core's own `RETRIEVE_ACTION_NAME`/`RETRIEVE_DOCUMENT_ACTION_NAME` action pair, not anything Knowledge-Memory-scoped.

**Consequence:** whatever permission treatment Knowledge Retrieval requires, if any, cannot be satisfied by depending on `MemoryRetrieval`'s existing filtering and calling the question closed. Contract Design V2 §12's own narrow authorisation — Memory Core reachable "for forwarded, minimal, immutable provenance references only" — already forecloses the one way this might otherwise have worked (Knowledge Retrieval re-running its own Memory Core queries through the existing gate); that path was already ruled out by the preceding planning review's own Section 7 (Dependency Analysis), independently of this review's own retrieval-permission question.

---

## 5. Does Lifecycle State Alone Determine Visibility?

**No document treats lifecycle status as a permission-visibility mechanism, and Chapter 10's own architecture forbids conflating the two.** Chapter 10 §5's "Separation from domain evaluation" guarantee states the Permission Engine "never assesses knowledge truth, computes confidence, decides evidential state" — the converse also holds throughout every document read for this review: a `KnowledgeItem`'s own lifecycle status (`ACTIVE`/`RETIRED`/etc., Unit 7's own domain) is never treated anywhere as a substitute for, or determinant of, a permission decision. These are established as orthogonal axes, not overlapping ones. Unit 7 Clarification §9's own retirement/restoration text discusses retirement as "never implies deletion" and restoration as producing "a new, visible restoration event" — but "visible" there means visible *in the item's own history*, an epistemic/audit-trail guarantee, not a statement about which requesting principal may retrieve the item at all.

---

## 6. Do Retired, Restored, or Revised Items Affect Retrieval Decisions?

**Not a permission question — a retrieval-shape question, and one this review confirms remains genuinely unsettled, consistent with the preceding planning review's own finding.** Contract Design V2 §12's Knowledge Result definition bundles "Knowledge References/Items, evidential-state disclosures... and a mandatory staleness disclosure" — it does not state whether a Knowledge Result may include a `RETIRED` item by default, must exclude one, or must include one with an explicit disclosure. No document read for this review resolves this. It is a real, disclosed gap (already flagged in the preceding review's Section 6, "Lifecycle visibility... Not settled") — but it is a question about *what a Knowledge Result contains*, not about *whether retrieving it requires authorisation*, and this review does not conflate the two.

---

## 7. Ownership: Where Would Retrieval Permissions Belong?

Applying Chapter 10's own, already-established ownership architecture (§2, §7, §8), independent of whether gating is ultimately required at all:

- **Permission Engine** — remains the sole authority for *the decision itself*, unconditionally, regardless of how this question resolves (Chapter 10 §5, sole-authority guarantee). This is not in question.
- **Memory Retrieval** — structurally barred from being the enforcement point, both because it cannot reach `KnowledgeItem` at all (Section 4, above) and because Memory Core Scope Lock §6/§14's own prohibition on `MemoryCore`/`MemoryRetrieval` holding a `PermissionEngine` reference is specific to that interface — it is not a rule Chapter 10 §8 extends to bar *every* domain class from self-gating (Knowledge Submission's own `DefaultKnowledgeSubmission` holds `PermissionEngine` directly, precisely because it is a single, self-contained, accepting-shaped boundary, not a multi-operation surface — Unit 8 Clarification §5's own precedent comparison).
- **Knowledge Retrieval or Runtime** — genuinely open, and dependent on a fact not yet knowable: which of the two existing repository precedents Knowledge Retrieval's own eventual shape resembles. Unit 8 Clarification §5 distinguishes them exactly: a single, self-contained, one-operation accepting boundary self-gates (`EvidenceCustodian`, `DefaultKnowledgeSubmission`); a multi-operation surface reachable by many independent callers is gated externally by Runtime (`MemoryCore`/`MemoryRetrieval`, via `PermissionFilteredMemoryRetrieval`). Knowledge Retrieval's own Contract Design V2 §12 description — "the single public path through which a Knowledge Query is answered" — reads closer to the first shape (one operation, one query method) than the second, but this review does not decide the question; it identifies which existing precedent-pair the eventual decision must choose between.

---

## 8. Is "Evaluation C" Genuinely Missing Governance, or Does Repository Authority Already Imply the Answer?

**Neither cleanly.** The honest finding, held to the standard the task itself sets ("repository authority takes precedence over attractive terminology"), is narrower than either extreme:

**What repository authority already, genuinely settles, without need for any new invention:**

1. The *test* to apply is not missing — Chapter 10 §3's general admission criterion is already established, already relied upon twice (Evaluation B, and the Unit 7 lifecycle acts' negative classification), and requires no new governance to exist as a test.
2. The *procedure* for applying it is not missing — Chapter 10 §10 already fixes it: "Domains explicitly classify consequential acts against Chapter 10's criteria... every newly introduced act... receives an explicit, disclosed classification in the domain's own document, citing the Chapter 10 criterion relied upon... Neither a positive nor a negative classification is asserted or omitted silently." This is not a hypothetical mechanism this review is proposing — it is the same mechanism Unit 6 (Promotion, negative), Unit 7 §13 (four lifecycle acts, negative, explicitly reasoned), and Unit 8 §12 (Submission, positive) each already used.
3. Memory Retrieval cannot be reused to answer the question by default (Section 4) — that path is closed, cleanly, on structural grounds.
4. Lifecycle status is not a substitute for a permission answer (Section 5) — that path is also closed.

**What repository authority genuinely does not yet settle:**

Whether Knowledge Retrieval itself — the act of disclosing a `KnowledgeItem`, with its evidential-state classification and provenance reference, to a requesting principal — is, under Chapter 10 §3's own test, "ordinary internal computation" or "granting access to a sensitive record." **No Programme 3 document has performed this classification for Retrieval, positively or negatively, the way it has been performed for every other consequential Knowledge Memory act.** This is confirmed by direct search: Contract Design V2 §6/§12 describes Knowledge Retrieval's *shape* in detail but is silent on its permission classification; the Scope Lock's own Deliverable 9 and §4 exclusions describe scope and sequencing but not permission treatment; no Unit 9 Scope Lock Clarification exists (consistent with Unit 9 not yet being implemented, per the preceding review).

Two considerations weigh on how that missing classification is likely to resolve, without this review resolving it: retrieval is the *first* point in Knowledge Memory's own pipeline where content becomes visible to an external, requesting principal — categorically different from Promotion and the four lifecycle acts, none of which involve external disclosure at all (Unit 7 §13's own reasoning is explicit that its four acts are non-gated *because* "none exposes anything beyond what Promotion already exposed," and Promotion itself exposes nothing externally). And a `KnowledgeItem` carries a `ProvenanceReference` to Memory Core evidence that this repository's own only built retrieval-gating precedent (Section 3, above) treats as requiring gating unconditionally, not selectively. Both considerations point toward disclosure at the retrieval boundary being the kind of act Chapter 10 §3 names as a proposal ("granting access to a sensitive record") — but pointing toward an answer is not the same as the classification having been performed and disclosed, which is what Chapter 10 §10 actually requires before implementation, and what has not yet happened here.

**Conclusion: "Evaluation C" is not a governance void requiring invention. It is a missing instance of an already-existing, already-used governance procedure.** The correct next lawful step is not to design a new evaluation tier, but to perform, for Retrieval, the same explicit, disclosed, reasoned classification exercise Unit 6, Unit 7 §13, and Unit 8 §12 already performed for their own acts — reaching whatever answer that exercise actually supports, positive or negative, and disclosing the reasoning either way, exactly as CDR-005's symmetric documentation requirement demands.

---

## 9. Findings Summary

| # | Finding | Kind |
| --- | --- | --- |
| 1 | Chapter 10 §3's own characterisation of Memory Core Scope Lock §5 ("structural retrieval... not gated") is inconsistent with Scope Lock §6's own unqualified text and with the actual, built `PermissionFilteredMemoryRetrieval` code, both of which gate every record unconditionally | Documentary inaccuracy in a still-Draft document, not a defect in this review |
| 2 | Knowledge Retrieval cannot inherit Memory Retrieval's own filtering — structurally impossible, since `KnowledgeItem` is not a `MemoryCoreRecord` and passes through none of `PermissionFilteredMemoryRetrieval`'s methods | Confirmed, closed question |
| 3 | Lifecycle status and permission are established as orthogonal concerns everywhere this review looked; lifecycle status does not determine retrieval visibility | Confirmed, closed question |
| 4 | Whether a Knowledge Result includes retired/superseded items by default is unsettled — a retrieval-shape gap, not a permission gap | Open, but correctly scoped (contract-shape, not permission) |
| 5 | No Programme 3 document has performed Chapter 10 §10's required domain self-certification classification for Retrieval | **The one genuine governance gap this review confirms** |
| 6 | Which of the two existing self-gating/externally-gated precedents Knowledge Retrieval's own eventual shape resembles is not yet determinable | Open, dependent on Unit 9's own future design, not a governance gap |

---

## 10. Recommendation

Do not draft a new "Evaluation C" mechanism, interface, or Contract Design amendment. The correct, minimal, lawful next step — mirroring exactly how Unit 6, Unit 7, and Unit 8 each closed the identical procedural requirement for their own acts — is a narrow domain self-certification classification for Knowledge Retrieval against Chapter 10 §3's criteria, performed and disclosed under CDR-005's Model C procedure, reaching and stating whichever answer (gated or not gated) that exercise actually supports. This is a documentation task with an already-established template (Unit 7 Clarification §13 is the closest structural precedent, being itself a negative classification reached through the same method), not new architecture, and it does not require Unit 9's own implementation to begin first — exactly as Unit 8's own classification (Clarification, adopted) preceded `DefaultKnowledgeSubmission`'s implementation, not the reverse.

---

## Independent Constitutional Review

- **Did the review invent governance?** No. Every requirement or test applied (Chapter 10 §3's admission criterion, §10's classification procedure, Scope Lock §6's "without exception" gating rule) is a direct citation, checked against primary text, not an assertion introduced here.
- **Did it assume implementation gaps?** No — the central finding (no classification exists for Retrieval) was confirmed by direct search across every governance document read, not inferred from silence alone; Section 3's finding about Chapter 10 §3's own inaccuracy was confirmed by direct, word-for-word comparison against Scope Lock §5/§6 and the actual code, not assumed.
- **Did it mistake runtime composition for implementation, or confuse Memory Retrieval with Knowledge Retrieval?** No — Section 4 treats the two as structurally distinct by direct code inspection, and this review does not address runtime composition at all, since no implementation exists yet to compose.
- **Did it propose implementation outside existing authority?** No — Section 10's recommendation is a documentation/classification step using an already-three-times-used procedure, not a new mechanism, an interface shape, or a permission model. This review deliberately does not name a resource identifier, action name, or Kotlin type for whatever classification eventually results, consistent with the task's own instruction not to invent Evaluation C.
- **Did the review adopt "Evaluation C" as though it were settled architecture?** No — Section 8 explicitly concludes the opposite: the label names an open question, not a decided mechanism, and Sections 1 and 8 both state this outcome directly rather than leaving it implied.

---

## Final Git Status

```
$ git status --short
?? docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_PLANNING_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_RETRIEVAL_PERMISSION_EVALUATION_PLANNING_REVIEW.md
```
