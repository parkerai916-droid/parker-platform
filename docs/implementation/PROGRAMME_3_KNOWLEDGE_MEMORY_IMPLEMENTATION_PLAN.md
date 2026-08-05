**Status:** Engineering planning only. No Kotlin is implemented, proposed as a diff, or changed by this document. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

# Programme 3 — Knowledge Memory Implementation Plan

Programme: **Programme 3 — Knowledge Memory Implementation Plan.**

**Normative inputs, frozen, not redefined:** `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md`, `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_GOVERNANCE_REVIEW.md`, `docs/reviews/EPISTEMIC_INTEGRITY_CONSTITUTIONAL_REMEDIATION_PLAN.md`, `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`, and `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` (treated as engineering precedent, not as a source of new decisions). This document describes **how** the already-approved Knowledge Memory is built — sequencing, repository impact, migration, testing, and risk handling. It introduces no contract, no field, no lifecycle rule, no permission model, and no constitutional obligation beyond what the Scope Lock and Contract Design Version 2 already froze. Where this plan makes a genuinely new decision (a unit boundary, a phase grouping, an ordering choice), it is disclosed as an implementation-sequencing choice, not presented as an architectural one. Implementation planning remains entirely inside the Scope Lock's own boundary (`PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §3–§4); nothing below expands, narrows, or reinterprets it.

**Terminology note, recorded for audit trail.** "Knowledge Memory" names the Programme 3 subsystem as a whole (the umbrella term the Reconciliation reserves for the Memory Core + renamed-store family). It is not itself a Kotlin identifier. The specific renamed type is `KnowledgeStore`, per `MEMORY_ARCHITECTURE_RECONCILIATION.md` §13's canonical terminology table, restated unchanged in `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` §2 and relied upon without qualification throughout `PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md`. A Unit 1 work order issued against this plan proposed `KnowledgeMemory` as the literal rename target instead of `KnowledgeStore`. This was identified as a contradiction with the frozen terminology before any code was touched, escalated rather than resolved unilaterally, and settled by explicit user decision in favour of the already-ratified name: **the rename target is `KnowledgeStore`, not `KnowledgeMemory`.** Every reference to "Knowledge Memory equivalents" below means the specific type mapping in Unit 1, not a literal `KnowledgeMemory` identifier.

---

## 1. Executive Summary

**Implementation objective:** convert the ten deliverables the Scope Lock already authorised (§5) into a sequence of small, independently buildable, independently testable engineering units, such that Parker compiles and its existing test suite passes after every single unit — never only at the end of the Programme.

**Engineering strategy:** rename before extend, extend before change behaviour, change behaviour before wire permissions, wire permissions before expose retrieval, expose retrieval before verify acceptance. Concretely: the one purely mechanical, zero-behaviour-change unit (the `MemoryStore` → Knowledge Memory rename) is isolated and performed first and alone, so that every risk introduced by later units is a risk in new or extended logic, never entangled with a simultaneous naming change. New public types are added additively to the renamed foundation rather than built as a second, parallel structure. Constitutionally load-bearing logic (promotion weighing, lifecycle ordering, permission evaluation) is implemented and independently tested before the retrieval surface that exposes it is built, so that nothing is ever exposed before it is compliant.

**Expected outcome:** a complete, tested, constitutionally compliant Knowledge Memory that is behaviourally a drop-in-capable replacement for `MemoryStore`'s current production role, verifiably equivalent to it in everything it already did, and additionally compliant with every obligation the Scope Lock names — ready for Programme 4 to adopt, without Programme 3 itself performing that adoption (Scope Lock §4, §9).

---

## 2. Implementation Phases

Four phases. Each phase ends with a working, verifiable platform — no phase depends on a later phase's own work to compile or to pass its own tests.

**Phase 1 — Vocabulary and Type Foundation.** The `MemoryStore` family is renamed to its Knowledge Memory equivalents (`MemoryStore` → `KnowledgeStore`, per the frozen Reconciliation §13 mapping — see the Terminology note above), and the two new foundational value types (the evidential-state representation and the provenance-reference type) are introduced additively. Outcome: `KnowledgeStore` exists under its own name, behaviourally identical to today's `MemoryStore`; the two new value types exist and are independently testable, but nothing yet depends on them.

**Phase 2 — Public Model Construction.** Knowledge Item, Knowledge Promotion, Knowledge Reference, and Knowledge Candidate are built as additive extensions of the renamed record shape from Phase 1, incorporating the two new value types. Outcome: the full public model Contract Design Version 2 §12 describes exists and compiles; existing promotion/retrieval behaviour is unchanged, since nothing yet reads the new fields.

**Phase 3 — Constitutional Behaviour.** The promotion pipeline is extended to satisfy the multi-factor, independence-aware weighing and confidence-sourcing requirements; the knowledge lifecycle (revision ordering, per-revision disclosure, supersession chains, retirement, restoration) is implemented; the Knowledge Memory-side permission evaluation is wired to inherit, and never re-litigate, Memory Core's own. Outcome: every constitutional obligation the Scope Lock names (§6) is implemented and independently tested, still behind the existing production entry points — nothing user-facing has changed yet.

**Phase 4 — Retrieval Surface and Acceptance.** The Knowledge Query/Knowledge Result surface is built, including mandatory staleness disclosure and the determinism guarantee; the full unit set is wired into the composition root additively; the existing `MemoryStore`-descended test suite is verified to remain valid and passing in its renamed form; a full acceptance pass is run against every Scope Lock §6/§7/§9 requirement. Outcome: Knowledge Memory is complete, tested, and ready for Programme 4's own, separately governed cutover — Programme 3 is done.

---

## 3. Engineering Units

Ten units, each scoped to be independently implementable, reviewable, and verifiable.

**Unit 1 — Vocabulary rename.** *Objective:* rename `MemoryStore` → `KnowledgeStore`, `InMemoryMemoryStore` → `InMemoryKnowledgeStore`, `CandidateMemory` → `CandidateKnowledge`, `MemoryRecord` → `KnowledgeRecord`, `MemoryPromotionPolicy` → `KnowledgePromotionPolicy`, `MemoryCategory` → `KnowledgeCategory`, and `MemorySource` → `KnowledgeSource` (the exact mapping fixed by `MEMORY_ARCHITECTURE_RECONCILIATION.md` §13, restated in Contract Design Version 2 §2 — see this document's own Terminology note, above), applied atomically across production code, the composition root, and every affected test file. *Dependencies:* none. *Expected repository impact:* Section 4, below. *Constitutional obligations:* none introduced — this unit is purely mechanical and must produce zero behaviour change. *Verification required:* the full existing test suite passes, unmodified in substance (identifiers renamed only), with no new test required.

**Unit 2 — Evidential-state representation.** *Objective:* introduce the value type expressing Article IV's evidential-state classification, structurally capable of an insufficiently-supported/unresolved outcome (Scope Lock §6). *Dependencies:* Unit 1 (renamed namespace to add this type into). *Expected repository impact:* Section 4. *Constitutional obligations:* Article IV, Article XIII (mandatory expressiveness). *Verification required:* construction tests proving every required outcome, including the unresolved case, is representable; no existing test is affected, since nothing yet references this type.

**Unit 3 — Provenance-reference type.** *Objective:* introduce the minimal, immutable, identifier-only pointer type Contract Design Version 2 §6 defines. *Dependencies:* none beyond Unit 1's renamed namespace. *Expected repository impact:* Section 4. *Constitutional obligations:* Articles VIII/IX (provenance never duplicated, never owned by Knowledge Memory). *Verification required:* tests proving the type carries no provenance field content beyond an identifier, and that it cannot be repointed once constructed; no existing test is affected.

**Unit 4 — Knowledge Item, Knowledge Promotion, Knowledge Reference.** *Objective:* extend the renamed record shape (Unit 1) additively with a Memory Core evidence reference, the evidential-state field (Unit 2), a provenance reference (Unit 3), and a chronologically ordered history slot for Knowledge Promotion/revision disclosure records. *Dependencies:* Units 1, 2, 3. *Expected repository impact:* Section 4. *Constitutional obligations:* Articles VIII/IX (referenced, not copied), Article XVI (history slot present, even before Unit 7 populates it meaningfully). *Verification required:* construction and field-presence tests for the extended shape; the existing renamed test suite (Unit 1) continues to pass unmodified, since the extension is additive.

**Unit 5 — Knowledge Candidate.** *Objective:* define the submission shape as an evolution of the renamed candidate type, explicitly excluding any caller-settable confidence or evidential-state field (Contract Design Version 2 §2, §5 — Amendment 2). *Dependencies:* Units 1, 2. *Expected repository impact:* Section 4. *Constitutional obligations:* Article XV (no self-assigned evidential status). *Verification required:* a test proving a submission carrying either excluded field is rejected as malformed; existing candidate-construction tests continue to pass for the fields that remain.

**Unit 6 — Promotion pipeline.** *Objective:* extend the promotion evaluation to weigh more than one factor, to treat repetition/frequency as contributing no more weight than a single mention when mentions share a common origin, and to source confidence exclusively from Memory Core's own recorded evidence or from this evaluation itself, never from the submission (Contract Design Version 2 §5 — Amendments 1 and 2). *Dependencies:* Units 2, 4, 5. *Expected repository impact:* Section 4. *Constitutional obligations:* Article XI (no undeclared single factor), Article XV (confidence sourcing). *Verification required:* dedicated tests proving no single factor alone determines an outcome absent a documented exception, and proving common-origin repetition does not inflate weight — this is the highest-scrutiny unit in the Programme (Section 7).

**Unit 7 — Knowledge lifecycle.** *Objective:* implement ordered, non-forking revision with a per-revision disclosure record; multi-hop supersession chains; non-deletive, selectively reversible retirement with an explicit restoration event, except where the underlying Memory Core evidence was itself erased (Contract Design Version 2 §3 — Amendments 4 and 5). This unit also fixes, explicitly and disclosedly, the Scope Lock §8 open implementation question — whether concurrently arriving evidence is evaluated jointly or by a defined sequential ordering rule — as a single, consistent, documented choice. *Dependencies:* Units 4, 6. *Expected repository impact:* Section 4. *Constitutional obligations:* Articles XVI, XVII, XVIII. *Verification required:* tests for ordering, non-forking history, per-revision disclosure, multi-hop supersession, retirement/restoration, and the chosen concurrency resolution specifically.

**Unit 8 — Permission-boundary wiring.** *Objective:* implement the Knowledge Memory-side submission evaluation (Evaluation B), gating only the act of submitting a Knowledge Candidate, and wire it so it never re-evaluates Memory Core's own already-settled evidence authorization (Evaluation A) (Contract Design Version 2 §7 — Amendment 8). *Dependencies:* Unit 5; Memory Core's existing, unmodified permission boundary. *Expected repository impact:* Section 4. *Constitutional obligations:* the Parker Constitution's trust-authorises discipline. *Verification required:* a structural test proving Evaluation B holds no dependency capable of re-deciding Evaluation A's outcome, mirroring Memory Core's own precedent test for its analogous guarantee.

*Acceptance-tracking note (added after the fact, not part of this unit's original objective/dependency/verification text above, which is unchanged).* Evaluation B's own concrete constitutional treatment — the governed act's exact boundary, enforcement location, principal-supply mechanism, resource/action disclosure, evaluation order, permission-denial disposition, and CDR-005 Model C self-certification — is now settled by `docs/governance/PROGRAMME_3_UNIT_8_SCOPE_LOCK_CLARIFICATION.md` (Adopted, per its own independent constitutional review, `docs/reviews/PROGRAMME_3_UNIT_8_INDEPENDENT_CONSTITUTIONAL_REVIEW.md`). That Clarification's own Section 13 discloses that `docs/architecture/10-permission-engine.md` and `docs/decisions/CDR-005_CONSTITUTIONAL_ADMISSION_OF_PERMISSIONENGINE_PROPOSAL_CLASSES.md` remain in Draft status pending their own, separately-scoped Final Freeze Verification — a distinct governance task, not performed by this note and not a precondition for Unit 8 to proceed on the Evaluation B dimension.

**Unit 9 — Knowledge Query, Knowledge Result, Knowledge Retrieval.** *Objective:* implement the read interface, including a mandatory staleness disclosure on every result and deterministic, repeatable behaviour for an unchanged store (Contract Design Version 2 §6, §11; Scope Lock §7). *Dependencies:* Units 4, 6, 7. *Expected repository impact:* Section 4. *Constitutional obligations:* Article XIII (staleness never concealed), the Scope Lock's own determinism requirement. *Verification required:* tests proving staleness is present on every result (never omitted), and that an unchanged store returns identical results across repeated identical queries.

**Unit 10 — Composition wiring and regression verification.** *Objective:* wire the completed Knowledge Memory into the composition root additively, exactly as every prior Programme's own composition-root pattern has done (new lines only, no restructuring of existing wiring); confirm the full pre-existing test suite, renamed in Unit 1, still passes; run a full acceptance pass against Scope Lock §6, §7, and §9. *Dependencies:* all prior units. *Expected repository impact:* Section 4. *Constitutional obligations:* all of them, collectively, verified together for the first time as a whole rather than unit by unit. *Verification required:* the full suite (pre-existing plus every unit's own new tests) green; a documented acceptance checklist against each Scope Lock §6 obligation individually.

---

## 4. Repository Impact

No code is proposed. Likely packages, modules, and components affected, by unit:

| Component (current location) | Units touching it | Nature of impact |
| --- | --- | --- |
| `src/interfaces/MemoryStore.kt` | 1, 4, 5 | Renamed (Unit 1); additively extended (Units 4, 5) |
| `src/runtime/InMemoryMemoryStore.kt` | 1, 6, 7, 8 | Renamed (Unit 1); promotion/lifecycle/permission logic extended (Units 6–8) |
| `src/interfaces/MemorySource.kt` | 1, 9 | Renamed (Unit 1); superseded in role by the new retrieval interface (Unit 9), though its renamed form remains what Reasoning Context depends on until Programme 4 |
| A new value-type location alongside `src/interfaces/MemoryCore.kt` (evidential-state and provenance-reference types) | 2, 3 | New, additive types |
| `src/composition/ParkerRuntime.kt` | 1, 8, 10 | One renamed reference (Unit 1); new, additive wiring lines (Units 8, 10) — no existing line altered beyond the rename |
| `tests/runtime/InMemoryMemoryStoreTest.kt` and sibling test files | 1, 4–9 | Renamed, identifiers only (Unit 1); new test files added alongside for each unit's own new behaviour (Units 2–9), none of the renamed suite's own assertions altered |
| `tests/contracts/` (existing `MemoryStore`-related contract tests, if any) | 1 | Renamed, identifiers only |
| `docs/architecture/00-index.md`, `docs/architecture/ARCHITECTURE_HISTORY.md` | 10 | Updated to record Programme 3's completion, mirroring the pattern already used for Epistemic Integrity's own ratification entry — an index/history update, not a code change |

No new top-level module or package is required; Knowledge Memory is implemented within the existing `src/interfaces`/`src/runtime`/`src/composition` structure Memory Core and every prior Programme already use.

---

## 5. Migration Strategy

- **Preserve existing behaviour.** Unit 1's rename is the only unit that touches an already-production-wired path (`MemorySource`, behind `DefaultReasoningContextAssembler`), and it changes names only — no logic, field, or return value changes as part of that unit. Every subsequent unit adds new, additive capability that nothing in the live reasoning path yet consumes; observable behaviour of the current production system is unchanged throughout the entire Programme.
- **Preserve existing tests where possible.** The existing `MemoryStore` test suite is renamed, never rewritten, in Unit 1 — its assertions are unchanged, consistent with the Reconciliation's own finding (§14) that this suite remains valid, unmodified in substance, as Knowledge Memory's own suite. Every later unit adds new test files alongside it; none is asked to change what it already proves.
- **Maintain backward compatibility until replacement is complete.** `DefaultReasoningContextAssembler` continues to depend on exactly the same (renamed) interface, receiving exactly the same information it does today, for the entire duration of Programme 3. Its adoption of the new Knowledge Query/Result surface (Unit 9) is explicitly Programme 4's own act (Scope Lock §4), not performed here.
- **Avoid duplicate sources of truth.** Knowledge Item, Knowledge Promotion, Knowledge Reference, and Knowledge Candidate are built as additive extensions of the single, renamed record shape from Unit 1 (Unit 4, Unit 5) — never as a second, parallel type carrying its own independent copy of what the renamed shape already holds. At no point during the Programme do two independently-writable representations of the same promoted knowledge exist simultaneously.

---

## 6. Verification Strategy

| Unit | Existing tests expected to pass | New tests required | Constitutional behaviour demonstrated |
| --- | --- | --- | --- |
| 1 | Full existing `MemoryStore` suite, under renamed identifiers | None (mechanical unit) | None — establishes the naming foundation only |
| 2 | Unaffected (new, unreferenced type) | Construction tests covering every required evidential-state outcome, including unresolved | Article XIII mandatory expressiveness |
| 3 | Unaffected | Minimality and immutability tests for the reference type | Articles VIII/IX |
| 4 | Renamed suite (Unit 1), unaffected by additive fields | Field-presence and construction tests for the extended shape | Articles VIII/IX, XVI (structural readiness) |
| 5 | Renamed candidate-construction tests | Rejection test for a candidate carrying an excluded field | Article XV |
| 6 | Renamed promotion-policy tests, for behaviour that is unchanged | Multi-factor and common-origin-repetition tests | Article XI, Article XV |
| 7 | Renamed lifecycle-adjacent tests | Ordering, non-forking, per-revision disclosure, supersession-chain, retirement/restoration, and chosen-concurrency-rule tests | Articles XVI, XVII, XVIII |
| 8 | Unaffected Memory Core permission tests | Structural non-re-litigation test | Trust-authorises discipline |
| 9 | Unaffected | Staleness-always-present and determinism/repeatability tests | Article XIII, Scope Lock's determinism requirement |
| 10 | Entire suite, renamed and newly added | A documented acceptance checklist run, not a single test | All Scope Lock §6 obligations, collectively |

Native verification (build and run the full test suite) is possible after every unit — no unit leaves the repository in a state that fails to compile or regresses a previously passing test.

---

## 7. Risk Management

- **Highest implementation risk: the promotion pipeline (Unit 6).** This is the only unit implementing genuinely new evaluative logic rather than an additive structural extension. **Mitigation:** the dedicated multi-factor and common-origin-repetition tests named in Section 6 are treated as blocking, not advisory; Unit 6 is reviewed directly against Contract Design Version 2 §5's own wording line by line before being considered complete, not merely against its own test suite passing.
- **Highest migration risk: Unit 1's rename touching the one genuinely production-wired path.** **Mitigation:** Unit 1 is isolated — no other logic change is bundled into the same unit — so that if anything regresses, the cause is unambiguous; the full pre-existing suite, run immediately after Unit 1 and before any other unit begins, is the acceptance gate for this specific risk.
- **Highest regression risk: the Scope Lock §8 open implementation question (concurrent versus serialized revision evaluation) being resolved inconsistently.** **Mitigation:** Unit 7 fixes this as a single, explicit, documented choice as part of its own definition (Section 3), rather than leaving it to be decided ad hoc by whichever engineer implements a given code path; a dedicated test proves the chosen mechanism is applied consistently.
- **Highest testing risk: treating "compiles and the existing suite passes" as proof of constitutional compliance for new behaviour the existing suite was never written to exercise.** **Mitigation:** Section 6's table names a specific constitutional behaviour each new test must demonstrate, not merely a code path it must execute; Unit 10's acceptance checklist verifies each Scope Lock §6 obligation individually, rather than inferring compliance from the suite's aggregate pass/fail result.

---

## 8. Completion Gates

**Unit complete**, objectively, when: it compiles; every existing test affected by it still passes; every new test named for it in Section 6 passes; no unit later in the sequence was required to reach this state.

**Phase complete**, objectively, when: every unit within the phase is complete by the definition above; a fresh clone of the repository at this point builds and tests successfully with no partially-implemented unit's state present; the phase's own stated outcome (Section 2) is demonstrable by running the relevant tests, not merely asserted.

**Programme complete**, objectively, when: every success criterion `PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` §9 states is met — implementation complete per Section 5 of that document; tests passing, including a dedicated test per constitutional requirement; constitutional behaviour demonstrated via the acceptance checklist (Unit 10), never inferred from naming or compilation; migration from `MemoryStore` complete, with no remaining code path constructing the legacy flat-field shape; and Knowledge Memory verified, through the above, to be ready for Programme 4's own adoption.

---

## 9. Deferred Work

Explicitly reserved for later Programmes, consistent with the Scope Lock's own exclusions (§4) and the Constitutional Remediation Plan's own Programme roadmap (§12):

- **Programme 4 (Reasoning Context):** wiring `DefaultReasoningContextAssembler` (or its successor) onto the new Knowledge Query/Result surface, replacing its current dependency on the renamed-but-unadopted interface; Article V propositional integrity; Article VII burden-of-justification computation at the reasoning layer; residual Article XV enforcement gates.
- **Programme 5 (World Model):** all four World Model conflict remediations the Compliance Audit identified; the World Model side of independence weighing (Article XI), entirely separate from and unblocked by anything in this plan.
- **Programme 6 (Document Intelligence):** OCR/transcription/translation, source-material preservation, and capture-type/contemporaneity work — Knowledge Memory accepts whatever Memory Core presents from this future Programme with no document-specific logic of its own.
- **Programme 7 (Representation Engine):** provenance disclosure, uncertainty disclosure, and assumption disclosure in actual user-facing text; confidence explanation.

No unit in Section 3 performs any work belonging to the above; this plan exists specifically to prevent the scope creep that would result from any of it being pulled forward.

---

## 10. Recommended Implementation Order

1. Unit 1 — vocabulary rename (isolated, verified alone).
2. Unit 2 — evidential-state representation.
3. Unit 3 — provenance-reference type.
4. Unit 4 — Knowledge Item, Knowledge Promotion, Knowledge Reference.
5. Unit 5 — Knowledge Candidate.
6. Unit 6 — promotion pipeline.
7. Unit 7 — knowledge lifecycle.
8. Unit 8 — permission-boundary wiring.
9. Unit 9 — Knowledge Query, Knowledge Result, Knowledge Retrieval.
10. Unit 10 — composition wiring and full regression/acceptance verification.

Each step depends only on steps already completed (Section 3's own dependency list confirms this for every unit); no step requires a later step's work to compile, test, or be reviewed, minimising rollback risk at every point in the sequence.

---

## Final Recommendation

**Ready to begin implementation.**

Every unit in Section 3 traces to a specific Scope Lock deliverable (§5) and constitutional obligation (§6); the one deliberately new sequencing decision this plan makes — isolating the rename as its own, first, zero-behaviour-change unit — directly serves the planning principle of minimising migration risk while keeping Parker buildable throughout. No unit depends on undelivered work from a later Programme, and Section 9 names every item explicitly excluded to prevent scope creep. No completion criterion in Section 8 is subjective. This plan introduces no architecture, no contract, and no constitutional decision beyond what `PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` and `PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` already froze.

---

## Tracking Note (added after the fact — not part of the plan above, which is unchanged)

Programme 4, Evidence Intelligence, Unit 8 ("Runtime Composition") now
composes Knowledge Submission specifically:
`DefaultKnowledgeCandidateEvaluator`, `DefaultKnowledgeSubmission`, and one
long-lived `InMemoryKnowledgeItemPersistence`, into `ParkerRuntime.kt`
(`src/composition/`), with the `WRITE`/`MEMORY` permission mapping for
`DefaultKnowledgeSubmission`'s own disclosed Resource/action registered
against the shared `ResourceRegistry`/`ActionVocabulary`. This satisfies
the Knowledge Submission portion of runtime reachability only.

This is **not** this document's own Unit 8 ("permission-boundary wiring")
or Unit 10 ("composition wiring and full regression/acceptance
verification") — neither is marked complete by this note. Knowledge
Query, Knowledge Result, Knowledge Retrieval (this plan's own Unit 9),
the full migration away from the legacy `MemoryStore` flat-field shape,
and this plan's own dedicated acceptance checklist (Unit 10) remain
entirely distinct, separately scoped, and not begun by Programme 4 Unit
8's work. Promoted `KnowledgeItem`s are, today, durable but unexposed —
no retrieval path onto them exists anywhere in the composed runtime.
