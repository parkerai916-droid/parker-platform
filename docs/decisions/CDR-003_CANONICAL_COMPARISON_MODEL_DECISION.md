**Status:** Constitutional Decision Record. This record persists, as the
canonical repository decision, the outcome already reached by
`docs/studies/CDR-003_COMPARISON_MODEL_EVALUATION.md` (the "Study"). It
does not reopen, re-derive, or repeat the Study's own analysis, and does
not reconsider Models A, B, or C. The Study remains unchanged and
remains the supporting analysis this record relies on; this record is
what makes that analysis's outcome a canonical Constitutional Decision
Record, following the same repository pattern already established by
`docs/decisions/CDR-001_MEMORY_RECORD_COMPARISON_VS_SEMANTIC_RETRIEVAL.md`
and `docs/decisions/CDR-002_CONSTITUTIONAL_INTERPRETATION_OF_COMPARISON.md`.
No Kotlin is implemented, proposed, or changed. Neither `src/` nor
`tests/` is touched. Nothing is staged, committed, or pushed.

# CDR-003 — Canonical Comparison Model Decision

Programme: **Parker Constitutional Decision Record 003.**

This record follows `docs/decisions/CDR-001_MEMORY_RECORD_COMPARISON_VS_SEMANTIC_RETRIEVAL.md`
(finding no proven contradiction between Memory Record Comparison and
Memory Core Scope Lock's semantic-retrieval exclusion, but identifying
constitutional ambiguity requiring clarification) and
`docs/decisions/CDR-002_CONSTITUTIONAL_INTERPRETATION_OF_COMPARISON.md`
(finding that the Constitution deliberately leaves the comparison
mechanism undefined, per `docs/architecture/MEMORY_CONTRACT_DESIGN.md`'s
own "must not be foreclosed" language). `docs/studies/CDR-003_COMPARISON_MODEL_EVALUATION.md`
subsequently evaluated three candidate constitutional models against
that finding and recommended one. This record adopts that
recommendation as canonical.

---

## Models Evaluated (per the Study)

- **Model A — Structural Comparison.** Comparison limited to
  identifiers, provenance, exact content, hashes, and metadata already
  present as constitutional facts. Highest simplicity and auditability;
  lowest long-term flexibility (a hard detection ceiling for paraphrased
  repetition).
- **Model B — Canonical Record Comparison.** Comparison performed
  against a constitutional representation of knowledge, with only the
  guarantees a mechanism must uphold fixed constitutionally; the
  mechanism itself (structural, rule-based, model-assisted,
  human-assisted) is left open and may change over time without
  altering constitutional behaviour.
- **Model C — Semantic Comparison.** Comparison explicitly evaluates
  meaning; ownership would need to relocate outside Memory Core, since
  Memory Core's own frozen exclusions do not permit it to own
  meaning-evaluation. Rejected in the Study on constitutional grounds
  before reaching implementation considerations.

## Adopted Decision

```
Model B — Canonical Record Comparison — Adopted
```

Model B was adopted because it is the direct architectural expression of
language already present in frozen governance (`MEMORY_CONTRACT_DESIGN.md`'s
"must not be foreclosed" passage), confirmed by CDR-002 as a deliberate,
not accidental, omission; because it binds every present and future
comparison mechanism to the same fixed guarantees rather than trusting
any one mechanism's internal workings; and because it permits Parker's
comparison quality to improve over time without repeatedly reopening
governance, unlike Model A's permanent detection ceiling. Model A was
not rejected for being harder to build — implementation difficulty was
the Study's lowest-weighted criterion — but because its long-term
flexibility and future-compatibility were the weakest of the three.
Model C was rejected on constitutional grounds specifically: it requires
relocating an assigned responsibility, raises an unresolved Article XV
tension, and threatens the determinism guarantee Memory Core's
architecture relies on throughout. Full reasoning, the comparative
table, and the five stress-test scenarios supporting this decision are
recorded in the Study and are not repeated here.

## Constitutional Guarantees Adopted

The Study's nine constitutional guarantees for Model B — Determinism, No
mutation, No promotion/no classification, Disclosed basis, Independence
preserved, Comparison is not contradiction resolution, No ownership
transfer, No technology commitment, and Disclosed asymmetry (never
concealed) — are the guarantees this decision adopts. Their full text is
recorded in the Study's own "Constitutional Guarantees Model B Must
Provide" section and is not repeated here.

## Relationship to Amendment 1

These nine guarantees were subsequently reproduced verbatim in
`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` Section 18 ("Amendment 1 —
Memory Record Comparison"), which this decision record's adoption of
Model B directly authorised. Section 18 is frozen, committed
(`b3f7090`), and is not modified, reopened, or reinterpreted by this
record.

---

## Final Report

**Document created:** `docs/decisions/CDR-003_CANONICAL_COMPARISON_MODEL_DECISION.md`
(only file created by this task alongside CDR-004's own canonical
record; no other file modified).

**Source study:** `docs/studies/CDR-003_COMPARISON_MODEL_EVALUATION.md`,
unchanged.

**Decision persisted:** Model B — Canonical Record Comparison — Adopted.

**Binding implementation:** `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`
Section 18 (Amendment 1, frozen, committed `b3f7090`).

CDR-003 CANONICAL DECISION RECORDED — MODEL B ADOPTED

Confirmed: no production code modified; no tests modified; no existing
governance document or amendment modified; the Study
(`docs/studies/CDR-003_COMPARISON_MODEL_EVALUATION.md`) unchanged; only
this new canonical decision record created; nothing staged; nothing
committed; nothing pushed; Amendment 3 not started; Unit 7 not started.
