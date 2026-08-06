# Programme 3 Unit 9 Knowledge Retrieval Contract Design — Defect Confirmation Review

## Status

**Defect confirmation review only — not a new Independent Constitutional Review.** This review does not re-examine the architecture, reconsider any constitutional conclusion, or reconsider the governance vehicle. It confirms only whether the four required corrections identified by `docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` were correctly implemented, and that no regression was introduced. No file was modified during this review. Nothing is staged, committed, or pushed.

---

## Repository Baseline

- **HEAD:** `b04710cf822cb2755996e43cb212376932f69a14` (`b04710c`)
- **Branch:** `main`
- **Staged changes:** none.

---

## Defect Reviewed

The Independent Constitutional Review's four required corrections to `docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md`:

1. Fix all broken internal `(Section N)` cross-references identified in the review.
2. Disclose, in the "Governing vehicle" paragraph, the Unit 9 Clarification's own recommendation of a future Contract Design V2 amendment, and explain why this document proceeds instead as a separate sibling Contract Design.
3. Add a contract-level query-identity requirement to Section 4 (Public Contract), stated at constitutional-purpose level only.
4. Add explicit supersession treatment to Section 6 (Lifecycle Behaviour), satisfying Contract Design V2 §3's multi-hop-chain retrievability requirement without inventing a new lifecycle state or selecting a "latest only" policy.

---

## Verification

Each of the four corrections was confirmed applied by direct re-inspection of the corrected document:

1. **Cross-references.** All `Section N` references in the document — including one additional stale reference discovered during verification (the Non-Responsibilities' provenance-forwarding mention, corrected from Section 8 to Section 7) — now resolve to their intended section. No stale off-by-one reference remains.
2. **Governance-vehicle disclosure.** The "Governing vehicle" block now states the Unit 9 Clarification's own expectation of a future V2 amendment, and distinguishes amendment-of-V2, sibling elaboration, preservation of V2's frozen text, and non-reopening of the Clarification, exactly as required.
3. **Query identity.** Section 4's Knowledge Query bullet now requires an explicit correlation identifier for auditability and traceability, with no field name, type, format, or generation mechanism named.
4. **Supersession treatment.** Section 6 now carries a "Superseded" bullet confirming no new status or event kind, non-erasure, multi-hop chain retrievability, and explicit non-selection of a "latest only" policy.

---

## Regression Check

Confirmed unchanged, by direct re-inspection of the corrected document's remaining sections:

- **Constitutional classification** (Section 5, Permission Boundary) — unchanged: the Unit 9 Clarification's classification remains "Settled, not reopened."
- **Governance vehicle** — unchanged: still a dedicated, sibling Contract Design; the correction adds disclosure, not a different vehicle.
- **Lifecycle model and ownership** (Section 6) — unchanged in substance: the Active/Revised/Retired/Restored treatment is untouched; only the new Superseded bullet was added between Revised and Retired.
- **Ordering, Provenance, Error Model, Runtime Responsibilities** (Sections 7, 8, 9, 10) — unchanged.
- **Explicit Exclusions and Constitutional Constraints** (Sections 11, 12) — unchanged.
- **Recommendation** (Final Recommendation section) — substantive reasoning unchanged.

**No regression found.** All four corrections are additive and precisely scoped; no other section's substantive text differs from the version the Independent Constitutional Review examined.

---

## Verdict

```
READY FOR CONTRACT DESIGN ACCEPTANCE
```

---

## Recommended Next Step

The Contract Design's own status header should be updated to reflect adoption, citing this defect confirmation review as the disclosed basis — mirroring the precedent already followed for the Unit 9 Scope Lock Clarification's own adoption-status update.

---

## Git Confirmation

- No file was modified during this review.
- Nothing was staged during this review.
- Nothing was committed during this review.
- Nothing was pushed during this review.

## Final Git Status

```
$ git status --short
?? docs/governance/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN.md
?? docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/PROGRAMME_3_UNIT_9_KNOWLEDGE_RETRIEVAL_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```
