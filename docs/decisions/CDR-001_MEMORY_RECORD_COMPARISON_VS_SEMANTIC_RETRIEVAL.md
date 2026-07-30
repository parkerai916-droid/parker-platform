**Status:** Constitutional Decision Record. This is not a governance amendment and modifies no existing governance document. No Kotlin is implemented, proposed, or changed. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed. This record answers the questions posed to it; it does not resolve the ambiguity it finds.

# CDR-001 — Memory Record Comparison vs Semantic Retrieval

Programme: **Parker Constitutional Decision Record 001.**

This record was triggered by `PHASE 1 - AMENDMENT 1 BLOCKED` (the attempt to draft the Memory Record Comparison amendment to Memory Core Scope Lock), which found that Memory Core Scope Lock permanently excludes "semantic retrieval... of any kind," while Programme 3's own governance requires a comparison capability whose relationship to that exclusion was never tested precisely. This record reviewed `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md`, `docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md`, `docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`, and `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md` in full (all already read during the audit trail this record continues), plus `docs/architecture/33-memory-consolidation.md` and `docs/architecture/IMPLEMENTATION_GAPS.md` (Gap #46), because both materially discuss comparison and were the original source of the "repetition/frequency" language every later document inherits.

---

## Question 1 — What Is Memory Record Comparison?

Every constitutional description located:

- `docs/architecture/33-memory-consolidation.md`: names "Repetition" and "Frequency" as two of six promotion factors, with no operational definition beyond the label.
- `docs/architecture/MEMORY_CONTRACT_DESIGN.md`: "repetition and frequency are evaluated by comparing a submission against Memory's own existing records — that comparison is `MemoryPromotionPolicy`'s job, performed during Evaluation, not something the submitter can assert about itself." A second passage: "`33-memory-consolidation.md`'s own promotion factors (frequency, repetition) may eventually require comparing a submission against a large or externally-stored population of existing records."
- `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 (an engineering gap log, not itself frozen governance, but the most detailed treatment of this concept in the repository): frames the question as "has this, or something like it, been said before? how often? how does it relate to what else Memory already holds?"
- `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §5: inherits the same factor labels without further operational definition.

**Constitutional purpose:** to support Knowledge Promotion's repetition and frequency factors (Chapter 33; Contract Design V2 §5), and, by extension, Article XI's independence requirement (repeated mentions must not be treated as corroboration without determining common origin — `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §6). No other purpose is named anywhere in the reviewed documents.

**Constitutional inputs:** one submitted candidate (a `CandidateMemory`/`CandidateKnowledge`, or, under Programme 3's later model, a `KnowledgeCandidate`'s referenced evidence) and "a population of Memory's own existing records." No document specifies which record types populate that comparison beyond "Memory's own existing records" generically.

**Constitutional outputs:** never specified beyond the two illustrative questions Gap #46 poses ("has this... been said before? how often?"). No document states whether the output is a boolean, a count, a list, or something else.

**Intended consumers:** Knowledge Memory's own promotion evaluator (`MemoryPromotionPolicy`, per `docs/architecture/MEMORY_CONTRACT_DESIGN.md`) exclusively. No other consumer is named anywhere.

**Nothing beyond the above is invented here.** In particular, no document specifies the comparison's *basis* (what makes two records "the same, or something like it") — this absence is the subject of Question 2.

---

## Question 2 — Is Memory Record Comparison Purely Structural?

No document reviewed enumerates identifiers, provenance, entity identity, relationships, exact content, hashes, or metadata as the specific basis for this comparison. `docs/architecture/MEMORY_CONTRACT_DESIGN.md`'s own text ("comparing a submission against Memory's own existing records") never specifies which fields are compared or how.

Governance also does not affirmatively authorise comparison of *meaning* — no document uses the word "semantic," "meaning," or "embedding" anywhere in connection with Memory Record Comparison. The only suggestive language is Gap #46's "or something like it," which implies something beyond exact-string matching but does not name a mechanism, and which appears in an engineering gap log rather than in frozen constitutional governance itself.

**Conclusion: governance neither affirmatively authorises a purely structural basis, nor affirmatively authorises comparison of meaning.** Both readings are underdetermined by the actual text. This is not a case where structural comparison is clearly permitted and semantic comparison is clearly excluded (or vice versa) — the governing texts simply never reach this level of specificity.

---

## Question 3 — If Comparison of Meaning Is Required, Which Subsystem Owns It?

**No constitutional owner currently exists** for meaning-based (semantic) similarity determination specifically. `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §15 defers "Semantic / embedding-based retrieval" to "a future, separately-justified read layer alongside `MemoryRetrieval`" — naming a future layer, not a future owner. No document assigns semantic comparison to Memory Core, Knowledge Memory, Reasoning Context, Retrieval, World Model, or any other named subsystem. This record does not assign one.

The *non-semantic* aspect — the decision to weigh repetition/frequency during promotion — is owned by Knowledge Memory (`MemoryPromotionPolicy`, per `docs/architecture/MEMORY_CONTRACT_DESIGN.md`, and Knowledge Memory generally per `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §5: "Knowledge Memory owns: evaluation and promotion decisions over Memory Core content"). This is the *weighing* responsibility, not the underlying similarity-detection mechanism, and is not in question here.

---

## Question 4 — Which Governance Documents Use "Comparison" to Mean Semantic Similarity?

| Document | Occurrence | Classification | Support |
| --- | --- | --- | --- |
| `docs/architecture/MEMORY_CONTRACT_DESIGN.md` | "repetition and frequency are evaluated by comparing a submission against Memory's own existing records" | Ambiguous | No basis specified; could be exact-match or meaning-based |
| `docs/architecture/MEMORY_CONTRACT_DESIGN.md` | "...may eventually require comparing a submission against a large or externally-stored population of existing records" | Ambiguous | Same reasoning; "large... population" implies scale, not mechanism |
| `docs/architecture/IMPLEMENTATION_GAPS.md` Gap #46 | "has this, or something like it, been said before?" | Leans semantic, but not conclusive | "or something like it" suggests non-exact matching; this document is an engineering gap log, not frozen governance, so its weight is lower than Contract Design or Scope Lock text |
| `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §5 | Names "repetition," "frequency" without further definition | Ambiguous (inherited) | Adds a binding multi-factor weighing constraint but no comparison-basis definition |
| `docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md` | "a new Memory Core retrieval capability able to compare a candidate against Memory Core's existing record population for **material similarity**" | Leans semantic | "Similarity" is this record's own prior document's word choice, not present verbatim in Chapter 33, Contract Design V2, or `MEMORY_CONTRACT_DESIGN.md` |
| `docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md` | "No `MemoryRetrieval` method searches record content for material similarity to a proposition" | Leans semantic | Same self-referential origin as above |
| `docs/reviews/PROGRAMME_3_KNOWLEDGE_PROMOTION_FACTOR_PROVENANCE.md` | "a full-text or statement-similarity search over `Assertion.statement`" | Leans semantic | Same self-referential origin |
| `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §4, §10, §15 | "Semantic retrieval," "semantic / embedding-based retrieval" | **Structural — i.e., clearly and consistently about a different capability** | Every one of these occurrences ties "semantic retrieval" to caller-facing, ranked/scored retrieval; none mentions promotion, repetition, frequency, or Knowledge Memory anywhere |

**A material finding:** the frozen constitutional texts that actually govern Memory Record Comparison (Chapter 33, `MEMORY_CONTRACT_DESIGN.md`, Contract Design V2 §5) never use the word "semantic," "similarity," or "meaning" at all. That language enters the audit trail only through this project's own later analysis documents (the Remediation Roadmap, the Blocker Ownership Matrix, the Knowledge Promotion Factor Provenance document) and, more weakly, through the non-binding Gap #46 engineering log. Memory Core Scope Lock's own use of "semantic retrieval" is confined, consistently and without exception, to a differently-purposed, caller-facing retrieval capability. It is a documented possibility that the apparent contradiction was sharpened by this project's own prior interpretive word choices ("similarity search") rather than being fully present in the original frozen governance texts — but Gap #46's independent, earlier "or something like it" phrasing means this cannot be attributed entirely to this project's own recent drafting either.

---

## Question 5 — Does the Constitutional Remediation Roadmap Require Revision?

The Roadmap's own wording ("a record-content similarity search capability") is not verbatim-grounded in Chapter 33, Contract Design V2, or `MEMORY_CONTRACT_DESIGN.md`, none of which use "similarity." This is the third of the three options offered: **the Roadmap uses ambiguous terminology.** This finding does not, by itself, resolve whether Amendment 1 remains correctly scoped or attempts to amend an excluded capability — both remain genuinely undetermined until the underlying question (whether repetition/frequency comparison constitutionally requires meaning-based similarity) is settled. The Roadmap's own wording is a contributing factor to the ambiguity being surfaced as a contradiction, not proof that a contradiction actually exists in the original governance, and not proof that it does not.

---

## Table 1 — Memory Record Comparison Meaning

| Document | Memory Record Comparison Meaning | Evidence |
| --- | --- | --- |
| `33-memory-consolidation.md` | Names "Repetition" and "Frequency" as promotion factors; no operational definition | "## Promotion Factors — Repetition ... Frequency" |
| `MEMORY_CONTRACT_DESIGN.md` | Comparison of a submitted candidate against Memory's existing record population, performed by the promotion policy during Evaluation; basis unspecified | "repetition and frequency are evaluated by comparing a submission against Memory's own existing records — that comparison is `MemoryPromotionPolicy`'s job" |
| `IMPLEMENTATION_GAPS.md` (Gap #46) | A check for whether a materially similar proposition has been previously recorded, and how often; basis unspecified but suggestive of non-exact matching | "has this, or something like it, been said before? how often?" |
| `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §5 | Inherits the same factor labels without further operational definition | "the promotion criteria already established for this layer (repetition, importance, relevance, frequency, confidence, explicit request)" |
| `PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md` | Introduces "similarity search" language not present in the above | "a new Memory Core retrieval capability able to compare a candidate against Memory Core's existing record population for material similarity" |

## Table 2 — Semantic Retrieval Meaning

| Document | Semantic Retrieval Meaning | Evidence |
| --- | --- | --- |
| `MEMORY_CORE_SCOPE_LOCK.md` §4 | A caller-facing retrieval capability, permanently excluded, with no concrete need identified | "Semantic retrieval | Explicitly and repeatedly excluded... it remains a future, separately-justified layer, not an extension of `MemoryRetrieval`" |
| `MEMORY_CORE_SCOPE_LOCK.md` §10 | Retrieval matched or ranked by relevance/meaning rather than structural criteria; the exclusion most likely to be reintroduced under a different name | "Every one of the seven modes above returns records matching structural criteria only — no scoring, no ranking... no relevance judgment of any kind" |
| `MEMORY_CORE_SCOPE_LOCK.md` §15 | A future, separately-justified read layer, alongside embeddings/vector databases and a deferred ranking seam | "Semantic / embedding-based retrieval | A future, separately-justified read layer alongside `MemoryRetrieval`" |
| `MEMORY_CONTRACT_DESIGN.md` | Relevance/ordering computed at retrieval time via a deferred `MemoryRetrievalPolicy` seam — a different concept from promotion-time comparison | "the seam by which relevance and ordering are computed when `MemoryStore.retrieve` is called" |

## Table 3 — Ambiguous Use of "Comparison"

| Document | Uses Comparison Ambiguously | Yes / No | Explanation |
| --- | --- | --- | --- |
| `33-memory-consolidation.md` | Yes | Names factors with no operational definition — ambiguous by silence |
| `MEMORY_CONTRACT_DESIGN.md` | Yes | "Comparing... existing records" specified twice, basis never stated either time |
| `IMPLEMENTATION_GAPS.md` | Yes | "Or something like it" suggests, but does not confirm, non-exact matching; lower governance weight (engineering log, not frozen governance) |
| `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §5 | Yes | Inherits the same unspecified factors |
| `PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md` | Yes | Introduces "similarity" language absent from the originating documents, potentially sharpening rather than reflecting the original ambiguity |
| `MEMORY_CORE_SCOPE_LOCK.md` (§4, §10, §15) | No | Consistently and unambiguously ties "semantic retrieval" to caller-facing ranked/scored retrieval; never once connects it to promotion, repetition, or frequency |

## Table 4 — Constitutional Capability Ownership

| Constitutional Capability | Constitutional Owner | Status |
| --- | --- | --- |
| Promotion-time weighing of repetition/frequency against Memory's existing records | Knowledge Memory (`MemoryPromotionPolicy`) | Owned; mechanism unspecified |
| Underlying record-population query capability needed to perform that weighing | Memory Core (by elimination — Knowledge Memory holds no independent copy of Memory Core's records) | Owned in principle; no specific method authorised |
| Meaning-based (semantic) similarity determination, if this comparison requires it | No constitutional owner currently exists | Undetermined whether required at all; no owner named even as a future matter |
| Caller-facing semantic/ranked retrieval (Memory Core Scope Lock's own excluded concept) | No constitutional owner currently exists (deferred, unnamed) | Permanently out of scope for Memory Core Version 1; no future owner named |

---

## Decision

```
Option C

The Constitution is presently ambiguous and requires clarification before Amendment 1 may proceed.
```

Neither Option A nor Option B is supportable without inventing a resolution the reviewed texts do not themselves provide. Option A (structurally distinct) would require asserting that "comparing a submission against Memory's own existing records" can be fully satisfied by identifier/hash/metadata-level matching alone — a reading no document states, and one that sits uneasily against Gap #46's own "or something like it" language. Option B (the same concept under another name) would require asserting that repetition/frequency comparison necessarily requires meaning-based similarity — equally unstated, and contradicted by the observation that Memory Core Scope Lock's own "semantic retrieval" language never once connects to promotion, repetition, or frequency anywhere it appears. The honest conclusion, supported by Question 4's citation table, is that frozen governance simply never specified this comparison's basis precisely enough to answer the question either way, and that this project's own subsequent audit trail (not frozen governance) introduced the "similarity" framing that made the apparent conflict with Memory Core Scope Lock's semantic-retrieval exclusion visible.

---

## Final Report

**Document created:** `docs/decisions/CDR-001_MEMORY_RECORD_COMPARISON_VS_SEMANTIC_RETRIEVAL.md` (only file created; no other file modified).

**Questions answered:** all five.

**Documents reviewed:** the seven named in the governing task, plus `docs/architecture/33-memory-consolidation.md` and `docs/architecture/IMPLEMENTATION_GAPS.md` (Gap #46), both found to materially discuss comparison and required to trace the six-factor list to its origin.

**Ambiguous terminology identified:** "comparison" (Chapter 33, `MEMORY_CONTRACT_DESIGN.md`, Contract Design V2 §5 — never specifies a basis) and "similarity" (introduced by this project's own Remediation Roadmap, Blocker Ownership Matrix, and Knowledge Promotion Factor Provenance documents, not present in the originating governance).

**Recommendation selected:** Option C.

CDR-001 ESTABLISHES CONSTITUTIONAL AMBIGUITY

Confirmed: no production code modified; no tests modified; no governance documents modified; only the new Constitutional Decision Record created; nothing staged; nothing committed; nothing pushed; Unit 7 not started.
