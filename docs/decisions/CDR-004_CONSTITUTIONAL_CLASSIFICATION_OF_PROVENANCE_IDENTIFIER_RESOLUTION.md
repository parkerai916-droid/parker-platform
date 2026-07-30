**Status:** Constitutional Decision Record. This record persists, as the
canonical repository decision, the constitutional classification already
adopted for Provenance Identifier Resolution. It does not reopen
constitutional analysis and does not reconsider Models A, B, or C — that
analysis was already performed and concluded; this record makes its
outcome canonical, following the same repository pattern established by
`docs/decisions/CDR-001_MEMORY_RECORD_COMPARISON_VS_SEMANTIC_RETRIEVAL.md`
and `docs/decisions/CDR-002_CONSTITUTIONAL_INTERPRETATION_OF_COMPARISON.md`.
No Kotlin is implemented, proposed, or changed. Neither `src/` nor
`tests/` is touched. Nothing is staged, committed, or pushed.

# CDR-004 — Constitutional Classification of Provenance Identifier Resolution

Programme: **Parker Constitutional Decision Record 004.**

This record was triggered when an independent constitutional review of
Amendment 2 ("Provenance Lookup by Identifier," then drafted as Section
19 of `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`) found the draft's
central assumption — that identifier-based Provenance resolution was
already covered by Section 10's existing "Identifier lookup" mode —
unproven, and identified specific, on-point contrary evidence in
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`. This record reviewed
`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`,
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`,
`docs/architecture/MEMORY_CONTRACT_DESIGN.md` (legacy),
`docs/reviews/PROGRAMME_3_CONSTITUTIONAL_AUDIT.md`,
`docs/governance/PROGRAMME_3_CONSTITUTIONAL_REMEDIATION_ROADMAP.md`,
`docs/reviews/PROGRAMME_3_BLOCKER_OWNERSHIP_MATRIX.md`, Section 18 of
Memory Core Scope Lock (Amendment 1), and CDR-001, CDR-002, and CDR-003
in full.

---

## Constitutional Question

What constitutional classification should be assigned to Provenance
Identifier Resolution: an existing retrieval mode (Model A), a
specialised capability within an existing mode requiring its own
guarantees (Model B), or a constitutionally distinct retrieval mode
(Model C)?

## Repository Evidence (Summary)

- `MEMORY_CORE_CONTRACT_DESIGN.md` §9's "Identifier lookup" description
  ("given an identifier and its record kind, return that one record or
  nothing") is generic and does not itself exclude Provenance.
- `MEMORY_CORE_CONTRACT_DESIGN.md` §18's Contract Summary table states
  Provenance's read path as "via referencing record" — distinct from
  Entity/Document/Assertion's "retrieve" and Relationship's "traverse."
- `MEMORY_CORE_CONTRACT_DESIGN.md` §8's "record kind" enumeration for
  Relationship endpoints (`entity, document, assertion, relationship,
  conversation-turn, knowledge-record`) excludes `provenance`.
- `MEMORY_CORE_CONTRACT_DESIGN.md` §§2–3 list Provenance among the five
  record kinds Memory Core structurally owns, on equal footing with
  Entity/Document/Assertion/Relationship — supporting parity of kind,
  not parity of current retrieval-path treatment.
- The Programme 3 Constitutional Audit and Blocker Ownership Matrix both
  independently classify this gap as "missing governance," not an
  intentional, permanent exclusion (unlike semantic retrieval).
- Amendment 1 (Section 18) set a precedent of keeping a genuinely new
  capability outside the seven-mode inventory entirely, rather than
  editing Section 10's frozen text — informative, but not directly
  transferable, since Provenance Lookup is a retrieval operation in a
  way Memory Record Comparison was not.

Full evidence citations and per-document classification are recorded in
the review that produced this decision and are not repeated in full
here.

## Candidate Models

- **Model A — Already an existing retrieval mode.** Rejected. Weakly
  supported: contradicted by `MEMORY_CORE_CONTRACT_DESIGN.md` §18's and
  §8's specific, on-point evidence that Provenance was not, in practice,
  treated as covered by mode 1.
- **Model B — A specialised capability within the existing Identifier
  lookup mode; the retrieval-mode inventory (count and names) unchanged;
  the capability requires its own constitutional guarantees.** Adopted.
  Reconciles the contrary evidence honestly (acknowledges Provenance was
  not previously covered) without requiring Section 10's frozen
  "exactly seven" text, or Contract Design's own frozen §9/§18 text, to
  be touched. Rests on the distinction that Section 10's "exactly
  seven... everything else excluded" language governs operation
  *shapes* (ruling out ranked/semantic retrieval as additional shapes),
  not a permanent freeze on which record kinds an existing, generically
  defined shape may address.
- **Model C — A constitutionally distinct retrieval mode; the inventory
  must change.** Rejected. The most literal reading of the contrary
  evidence, but disproportionate to what that evidence shows: nothing in
  reviewed governance states Provenance's exclusion from direct lookup
  was a deliberate, permanent architectural boundary (unlike semantic
  retrieval's explicit, repeated "must never be reintroduced" language).
  Treating an omission as though it required expanding a frozen,
  numbered inventory imposes a heavier, more disruptive constitutional
  act than the evidence demands, and breaks the additive-only pattern
  every amendment in this programme has otherwise followed.

## Decision

```
Model A — Rejected
Model B — Adopted
Model C — Rejected
```

Model B is preferred because it best preserves determinism and
auditability (guarantees are stated explicitly rather than assumed to
already exist), trust-first architecture (it does not assert a textual
claim the evidence does not support), governance simplicity (it avoids
reopening previously frozen, untouched numbered-section text for what
the Audit and Blocker Matrix both independently characterise as an
omission rather than a deliberate exclusion), and future extensibility
(it establishes a reusable, lower-friction pattern for closing
comparable gaps in other generically defined modes).

## Constitutional Consequences

Provenance Identifier Resolution is classified as a specialised
capability within the existing Identifier lookup mode (Section 10, mode
1). The seven-mode inventory is unchanged in count and name. Amendment 2
was revised, following this decision, to state explicitly that it
*extends* mode 1's previously undocumented scope — never that this
coverage already existed — and to supply the guarantees that extension
requires.

## Relationship to Amendment 2

This decision is implemented in `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`
Section 19 ("Amendment 2 — Provenance Lookup by Identifier"), which
adopts Model B by name in its own Status paragraph and Constitutional
Consistency Check (§19.8), and which has since completed independent
constitutional review, revision, final review, correction, and Final
Freeze Verification (Ready to Freeze). Section 19 is not modified,
reopened, or reinterpreted by this record.

---

## Final Report

**Document created:** `docs/decisions/CDR-004_CONSTITUTIONAL_CLASSIFICATION_OF_PROVENANCE_IDENTIFIER_RESOLUTION.md`
(alongside CDR-003's own canonical record, created in the same task; no
other file modified).

**Decision persisted:** Model B — Canonical, specialised capability
within Identifier lookup mode — Adopted; Models A and C rejected.

**Binding implementation:** `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`
Section 19 (Amendment 2).

CDR-004 CANONICAL DECISION RECORDED — MODEL B ADOPTED

Confirmed: no production code modified; no tests modified; no existing
governance document or amendment modified, including Amendment 1
(Section 18) and Amendment 2 (Section 19); only this new canonical
decision record created; nothing staged; nothing committed; nothing
pushed; Amendment 3 not started; Unit 7 not started.
