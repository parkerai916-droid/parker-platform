**Status:** Constitutional Decision Record — interpretation only. This is not a governance amendment and modifies no existing governance document. No Kotlin is implemented, proposed, or changed. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed. This record interprets what the existing Constitution already supports; it does not decide what comparison should become.

# CDR-002 — Constitutional Interpretation of "Comparison"

Programme: **Parker Constitutional Decision Record 002.**

This record continues from `docs/decisions/CDR-001_MEMORY_RECORD_COMPARISON_VS_SEMANTIC_RETRIEVAL.md`, which found the apparent contradiction between Memory Record Comparison and Semantic Retrieval unresolved because the Constitution never defines "comparison" precisely. It reviewed CDR-001, `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md`, `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md`, and `docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md` in full — all already read in the audit trail this record continues. It additionally consulted `docs/architecture/33-memory-consolidation.md` (the origin of "repetition"/"frequency"), `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 (the only detailed prior treatment of the comparison mechanism), `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §6 (Article XI restated for Programme 3), and `docs/architecture/epistemic-integrity.md` Article XI (the constitutional source of the independence/common-origin requirement) — each found to materially define repetition, frequency, or comparison.

---

## Question 1 — What Does the Constitution Explicitly Require Comparison to Achieve?

Every constitutional purpose located, each with its supporting text:

1. **Determining what information deserves long-term retention.** `docs/architecture/33-memory-consolidation.md`: "## Purpose — Determines what information deserves long-term retention." Repetition and Frequency are listed directly beneath this purpose statement as two of the six factors serving it.
2. **Serving as one of several jointly-weighed promotion factors**, never a standalone determinant. `docs/architecture/MEMORY_CONTRACT_DESIGN.md`: "repetition and frequency are evaluated by comparing a submission against Memory's own existing records — that comparison is `MemoryPromotionPolicy`'s job, performed during Evaluation." `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §5 restates the same factors under a binding multi-factor weighing constraint.
3. **Feeding the Article XI common-origin check before repetition may be treated as corroboration.** `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §6: "Repetition and frequency must never be treated as independent corroboration without first determining whether repeated mentions share a common origin." This is a downstream constraint on how a comparison's result may be used, not a description of the comparison mechanism itself, but it is a documented purpose comparison exists to serve.

No further purpose is stated anywhere in the reviewed documents. "Recognising related evidence" and "enabling future corroboration" (offered as illustrative examples in the governing task) are not independently supported by any citation found and are not added as additional purposes here.

---

## Question 2 — Does the Constitution Define How Comparison Is Performed?

| Basis | Classification | Support |
| --- | --- | --- |
| Identifiers | Silent | No document connects identifier-based lookup (`MemoryRetrieval`'s "Identifier lookup" mode, `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §10) to the repetition/frequency comparison mechanism anywhere |
| Provenance | Mentioned | `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md`: promotion judgment should be "evaluated against real Entity/Document/Evidence/**Provenance** context" — named as relevant context, not stated as the comparison's specific basis |
| Structure | Mentioned | Memory Core's general "structural criteria only" doctrine (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §10, §11) is pervasive across Memory Core's architecture, but no document explicitly ties it to this specific comparison mechanism |
| Metadata | Mentioned | `MemoryRetrieval`'s "Metadata filtering" mode exists as a general Memory Core capability; no document connects it to repetition/frequency comparison specifically |
| Content | Mentioned | `docs/architecture/MEMORY_CONTRACT_DESIGN.md`: "comparing a submission against Memory's own existing records" — content (the submission and the records) is clearly what is compared, but the comparison technique is never specified |
| Meaning | Silent | No document explicitly requires meaning-based comparison for this mechanism. `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46's "or something like it" suggests, but does not confirm, non-exact matching, and is non-binding. The adjacent "semantic retrieval" exclusion (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §4, §10, §15) is textually confined to a differently-purposed, caller-facing retrieval capability and never mentions promotion, repetition, or frequency (confirmed in CDR-001) |

**No category is Explicitly Required or Explicitly Excluded for this specific mechanism.** The Constitution names *what* is compared (a submission against existing records) without ever specifying *how*.

---

## Question 3 — Does Repetition Require Semantic Understanding?

Every occurrence of "repetition," "repeated," or "frequency" located:

1. `docs/architecture/33-memory-consolidation.md`: bare labels, "Repetition," "Frequency" — no mechanism stated.
2. `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (twice): "repetition and frequency are evaluated by comparing a submission against Memory's own existing records"; "`33-memory-consolidation.md`'s own promotion factors (frequency, repetition) may eventually require comparing a submission against a large or externally-stored population of existing records" — neither specifies identical-record matching or semantic matching.
3. `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §6 / `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §5: "Repetition and frequency must never be treated as independent corroboration without first determining whether repeated mentions share a common origin." This defines a *constraint on use* (a common-origin check must precede treating repetition as corroboration), not the mechanism that identifies "repeated mentions" in the first place.
4. `docs/architecture/epistemic-integrity.md`, Article XI, item 2 (Independence): "Independent corroboration requires that supporting accounts derive from sources whose knowledge of the proposition was not itself derived from one another or from a common upstream source. Repeated accounts shall not be treated as independent corroboration where they derive from the same underlying source." This is the Constitution's own most direct treatment of "repeated" — and it defines repetition's *evidentiary significance* in terms of **source lineage** (a common upstream source), not content meaning. This addresses a downstream question — given two accounts already known to concern the same proposition, are they independent? — not the prior question this record is investigating: how would Memory Core determine that two records concern "the same, or something like it" proposition in the first place?
5. `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46: "has this, or something like it, been said before? how often?" — non-binding, and the most suggestive of non-exact matching among all occurrences found.

**None of these explicitly requires semantic similarity, structural recurrence, or identical records as the comparison mechanism.** Article XI's own treatment of "repeated" is source-lineage-based (a structural, provenance-traceable property) for the *independence* question, but this does not resolve the *detection* question. **The Constitution is silent** on the mechanism by which "repeated" or "frequent" mentions of a proposition would first be identified.

---

## Question 4 — Does "Comparison" Necessarily Imply Semantic Retrieval?

No document reviewed — in this record, in CDR-001, or in any of the seven governing documents — states or implies that comparison *is* semantic retrieval, or that comparison requires semantic retrieval. `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`'s three occurrences of "semantic retrieval" (§4, §10, §15) never once connect to promotion, repetition, frequency, or comparison. **No constitutional document states `comparison == semantic retrieval`.**

---

## Question 5 — Can Comparison Be Constitutionally Satisfied Without Semantic Interpretation?

This is an interpretive question about what is *permitted*, not what is required or desirable, answered from documentary evidence only:

- No document requires semantic comparison (Question 3).
- No document excludes structural comparison for this specific mechanism (Question 2) — the only "semantic" exclusion in frozen governance is scoped to a different capability (retrieval) and never reaches comparison (CDR-001; Question 4).
- Article XI's own treatment of "repeated," where the Constitution does address it directly, reasons in source-lineage terms — a structural, traceable property — not in terms of meaning (Question 3).
- Memory Core's broader, pervasively-stated architectural doctrine favours structural, deterministic operation throughout (`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §10: "structural criteria only"; §11: "Deterministic behaviour... Repeatable retrieval"). This is not stated as governing the comparison mechanism specifically, but it is the consistent character of every other capability Memory Core Scope Lock does define.

**Conclusion: yes, the Constitution currently permits the possibility that comparison could be satisfied through a purely structural operation.** This is a statement of permission, not a determination that a structural approach is required, correct, or the only constitutionally sound path — a semantic approach is not excluded either, for the same reason (silence, not prohibition).

---

## Question 6 — What Constitutional Assumptions Entered Later Reviews?

Neither "similarity" nor "semantic" (in connection with comparison) appears in `docs/architecture/33-memory-consolidation.md`, `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, or `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`. These words entered the audit trail only through this project's own later analysis:

- `docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md`: "a new Memory Core retrieval capability able to compare a candidate against Memory Core's existing record population for **material similarity**"; "a record-similarity search capability."
- `docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md`: "No `MemoryRetrieval` method searches record content for **material similarity** to a proposition."
- `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md`: "a full-text or **statement-similarity search** over `Assertion.statement`."
- `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`: "a record-content **similarity search** capability."

A precise distinction, not previously drawn this sharply: these four documents introduce **"similarity"**, not literally **"semantic."** The word "semantic" itself, applied to comparison, first appears in CDR-001's own analysis, which drew the connection between "similarity" (this project's term) and "semantic retrieval" (Memory Core Scope Lock's term) because both concern non-exact, meaning-adjacent matching. That connection is a reasonable analytical step, not something any frozen governance document states directly — no governance document ever calls Memory Record Comparison "semantic."

---

## Table 1 — Constitutional References to "Comparison"

| Constitutional Reference | Uses "Comparison" | Defined? | Interpretation Supported |
| --- | --- | --- | --- |
| `33-memory-consolidation.md` | No (names factors only) | No | None — silent |
| `MEMORY_CONTRACT_DESIGN.md` | Yes (twice) | No — basis unspecified | Ambiguous/undefined |
| `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §5 | No (names factors, adds weighing constraint) | No | Inherits ambiguity |
| `PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §6 | No ("determining... common origin") | Partial — defines a downstream use-constraint, not the mechanism | Provenance/lineage-oriented for the independence question only |
| `epistemic-integrity.md`, Article XI | No ("repeated accounts," "common upstream source") | Partial — defines "repeated" for independence purposes via source lineage | Structural/provenance-leaning, for the independence question specifically |
| `MEMORY_CORE_SCOPE_LOCK.md` §4, §10, §15 | Uses "semantic retrieval," not "comparison" | Yes — but defines a differently-scoped capability | Structural-only for retrieval; silent on comparison |
| `IMPLEMENTATION_GAPS.md` Gap #46 | Yes | No — illustrative, non-binding | Leans non-exact, not authoritative |
| Remediation Roadmap / Ownership Matrix / Factor Provenance / Constitutional Audit | Yes ("compare," "similarity") | No — introduces new terminology | Not authoritative; later interpretive gloss |

## Table 2 — Capability Support

| Capability | Explicit | Implied | Silent | Excluded |
| --- | --- | --- | --- | --- |
| Structural comparison | No | Yes — consistent with Memory Core's pervasive structural/deterministic doctrine | Yes, for this specific mechanism | No |
| Semantic comparison | No | Weakly — only via Gap #46's non-binding "or something like it" | Yes, in frozen governance itself | No (the adjacent "semantic retrieval" exclusion governs a different capability, per CDR-001) |
| Exact equality | No | No | Yes | No |
| Provenance comparison | No | Yes — `MEMORY_ARCHITECTURE_RECONCILIATION.md`'s "Provenance context"; Article XI's "common upstream source" (for the independence question) | Partially | No |
| Relationship comparison | No | No | Yes | No |
| Document comparison | No | No | Yes | No |

## Table 3 — Later Terminology

| Later Review Term | Present in Original Governance | Introduced Later | Evidence |
| --- | --- | --- | --- |
| "Material similarity" / "similarity search" | No | Yes — `PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md`, `PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md` | Quoted above |
| "Statement-similarity search" | No | Yes — `PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md` | Quoted above |
| "Semantic" (applied to comparison) | No | Yes — first applied to comparison in `CDR-001`'s own analytical connection to Memory Core Scope Lock's distinct "semantic retrieval" term | CDR-001, Question 4 discussion |

---

## Constitutional Interpretation

```
Interpretation C

The Constitution deliberately leaves comparison undefined.
```

This is supported by more than silence alone. `docs/architecture/MEMORY_CONTRACT_DESIGN.md` states directly, of `MemoryPromotionPolicy`: "`33-memory-consolidation.md`'s own promotion factors (frequency, repetition) may eventually require comparing a submission against a large or externally-stored population of existing records, and **a future policy implementation — model-backed, human-in-the-loop, or simply backed by a persistence layer with real I/O — must not be foreclosed**." This is explicit, deliberate open-endedness: the governing document itself anticipates multiple, materially different future comparison mechanisms (including a "model-backed" approach, which could be semantic, and a plain persistence-backed approach, which could be structural) and requires that the interface not foreclose any of them. This is not an oversight or a gap the drafters failed to notice — it is a stated design choice to leave the mechanism open.

---

## Consequence

```
3. The Constitution is presently under-specified.
```

This refines, rather than contradicts, CDR-001's finding. CDR-001 established that the apparent conflict between Memory Record Comparison and Semantic Retrieval could not be resolved and required clarification. This record's closer reading finds no two binding rules that actually collide: Memory Core Scope Lock's semantic-retrieval exclusion is textually confined to a caller-facing retrieval capability and never mentions comparison, promotion, repetition, or frequency; and `MEMORY_CONTRACT_DESIGN.md`'s own text shows the comparison mechanism was deliberately left open rather than silently overlooked. What exists is an absence of specification for one particular mechanism (Memory Record Comparison), sitting near, but never shown to collide with, a specification for a different mechanism (semantic retrieval) that happens to use adjacent vocabulary. Absence of specification, deliberately left open by the governing document's own words, is under-specification — not a standing contradiction between two rules that presently bind and conflict.

---

## Final Report

**Document created:** `docs/decisions/CDR-002_CONSTITUTIONAL_INTERPRETATION_OF_COMPARISON.md` (only file created; no other file modified).

**Constitutional references analysed:** eight (Table 1), spanning `33-memory-consolidation.md`, `MEMORY_CONTRACT_DESIGN.md`, Contract Design V2 §5, Scope Lock §6, Article XI, Memory Core Scope Lock §4/§10/§15, `IMPLEMENTATION_GAPS.md` Gap #46, and the four later review documents.

**Meaning of comparison:** the Constitution states *what* is compared (a submission against Memory Core's existing records) and *why* (supporting the retention/promotion decision and the Article XI independence check) but never states *how* — no basis (identifier, provenance, structure, metadata, content, or meaning) is explicitly required or excluded for this specific mechanism.

**Semantic retrieval relationship:** no document states or implies comparison is, or requires, semantic retrieval; the two terms' apparent overlap was introduced by this project's own later analysis (CDR-001), not by any frozen governance text.

**Later terminology identified:** "material similarity," "similarity search," and "statement-similarity search," all introduced by the Remediation Roadmap, Blocker Ownership Matrix, Knowledge Promotion Factor Provenance, and Constitutional Audit documents, none present in the original constitutional texts.

**Constitutional interpretation selected:** Interpretation C — the Constitution deliberately leaves comparison undefined, directly supported by `MEMORY_CONTRACT_DESIGN.md`'s own "must not be foreclosed" language anticipating multiple future mechanisms.

**Consequence determined:** the Constitution is presently under-specified, not presently self-contradictory.

CDR-002 IDENTIFIES CONSTITUTIONAL UNDER-SPECIFICATION

Confirmed: no production code modified; no tests modified; no governance documents modified; only the new Constitutional Decision Record created; nothing staged; nothing committed; nothing pushed; Unit 7 not started.
