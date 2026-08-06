# Memory Core Durability Contract Design — Independent Constitutional Review

## Status

**Independent constitutional review only.** The subject document, `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`, was not modified during this review. No persistence was implemented. No Scope Lock or Implementation Plan was created. Nothing is staged, committed, or pushed. This review was performed without relying on the drafting session's own completion report — every citation below was re-verified against the governing document itself or the subject document's own text.

---

## 1. Repository Baseline

- **HEAD:** `41d81fb701d0f1c4d09f4e1bab64bed6a44a3b61` (short `41d81fb`).
- **Branch:** `main`.
- **Remote:** `origin` → `git@github.com:parkerai916-droid/parker-platform.git`; `origin/main` confirmed identical to local `HEAD`.
- **Working tree, before this review began:**
  ```
  ?? docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md
  ```
  Confirmed to match exactly what was expected. No discrepancy.

---

## 2. Authorities Reviewed

Read fresh for this review: `docs/adr/ADR-024-module-event-audit-durability-boundary.md` (§D, Rules 13–17, re-read verbatim); `docs/architecture/IMPLEMENTATION_GAPS.md` #51; `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md` §8, §10, §14; `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` §11, §12, §14; all four Memory Core Errata (001–004, re-read in full); `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §4, §6, §10, §11, §14, §15, §16 (§14's own literal `SHALL` text re-confirmed directly); `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` §7, §11, §13–17; `docs/reviews/MEMORY_AND_KNOWLEDGE_RESTART_PERSISTENCE_PLANNING_REVIEW.md` and `docs/reviews/MEMORY_AND_KNOWLEDGE_DURABILITY_CONTRACT_DESIGN_PLANNING_REVIEW.md` (both already committed on `main`, re-confirmed); `src/interfaces/MemoryCore.kt`; `src/runtime/InMemoryMemoryCore.kt` (re-read in full, 579 lines); `src/runtime/FileSystemEvidenceArtifactStorage.kt` and `src/runtime/FileSystemEvidenceDeletionAudit.kt` (both re-read in full); `docs/architecture/parker-constitution.md` (searched directly for "durable"/"persist"/"restart" — no match, confirmed). The subject document itself was read fresh, in full, independent of the drafting session's own completion report.

---

## 3. Governance Vehicle Review

**The chosen vehicle — a new, dedicated Contract Design under `docs/architecture/`, sibling to the existing one — is lawful.** Tested directly against each alternative the drafter's own Status section already considered, and re-tested independently here:

- **Errata to the existing Contract Design.** Correctly rejected. Every one of Errata 001–004 corrected or additively extended something already inside Version 1's own frozen scope (a miscount, a missing candidate type, a missing lifecycle operation, a missing principal parameter). Durability sits outside that scope entirely — Scope Lock §14 states, verbatim, re-confirmed by direct re-reading: "Memory Core `SHALL` use an in-memory, non-persistent storage implementation for Version 1, and `SHALL NOT` introduce any external database, cloud storage, or network synchronisation dependency." An errata reopening this boundary would be a materially different kind of change than any of the four precedents it would claim to follow.
- **Scope Lock amendment.** Not required as a prerequisite to this document's own existence. This repository's own established sequence, followed without exception across every prior programme this session observed (OCR Mechanism, Evidence Custodian, Knowledge Memory), is Contract Design → Scope Lock → Implementation Plan, in that order — a Contract Design has never in this repository's history been required to wait for its own Scope Lock to exist first. Requiring a Scope Lock amendment before a Contract Design could even be drafted would invert this repository's own sequencing discipline, not honour it. (This is distinct from whether the *document's own content* clearly enough distinguishes itself from Version 1 — see §6, below, which finds a genuine gap on that separate question.)
- **New ADR.** Correctly rejected. ADR-024 already settles the cross-cutting constitutional question (what must be durable, what may remain in-memory) and explicitly reserves implementation to "a future Contract Design pass" — the subject document is exactly that pass, for one named subsystem, not a duplicate or a reopening of ADR-024's own decision.
- **CDR.** Correctly rejected. Direct re-search of the Constitution confirms no persistence doctrine exists there to interpret. CDR-005's own Decision Rules reserve a CDR for a domain self-certification that is genuinely contested or ambiguous — no such contest exists; ADR-024 already resolved the one constitutional question, and everything the subject document adds is ordinary domain-governance elaboration of a principle ADR-024 itself invited a future Contract Design to supply.
- **`docs/reviews/` proposal instead of a canonical Contract Design.** Correctly rejected for this step. The enabling authority (ADR-024) already exists and is Accepted; both required planning reviews are already committed on `main`, satisfying this repository's own planning-first discipline before Contract Design drafting.

**Determination against the four framings the task names:** the subject document lawfully defines a later capability; it does not improperly bypass the frozen Scope Lock (nothing in it purports to change what Version 1's own implementation currently does or is accepted as doing); it does not require an enabling Scope Lock amendment before it may exist as a document. It is correctly staged as design authority preceding a future durability Scope Lock — but see §6, below, for a finding that the document's own text does not state this relationship to Version 1 as explicitly as the governing precedent (and this review's own scrutiny) requires.

---

## 4. ADR-024 Authority Review

ADR-024 §D settles exactly three things, and no more: **the durability obligation** (Rule 13 — Memory Records, Principal records, and an Audit log must eventually be durable); **what may remain in-memory** (Rule 14 — World Model beliefs and ordinary per-request working state); and **the correct present-tense reading of "durable"** until a persistence layer exists (Rule 15). It does not settle a persistence mechanism, recovery semantics, transactional boundaries, or schema evolution — it explicitly defers all of these: "implementation still to be authorised by a future Contract Design pass."

**No proposed decision in the subject document exceeds this authority.** Every requirement in §4 through §12 of the subject document (atomicity, recovery, corruption handling, versioning, immutability, concurrency, failure semantics, runtime boundary) is domain-tier elaboration of a principle ADR-024 itself assigns to "a future Contract Design pass" — not a reopening or duplication of ADR-024's own decision, and not an assumption of authority ADR-024 never granted. This finding holds without qualification.

---

## 5. Version 1 Boundary Review

**Finding: the underlying intent is correct and defensible, but the document does not state it with sufficient explicitness, and this is a genuine gap, not a stylistic preference.**

The subject document's own §1 (Purpose) frames its work as fixing "the durability boundary a future Memory Core Implementation Plan unit must build to," and its own Final Recommendation states the next stage — "a Memory Core Durability Scope Lock" — is a future, separate document. This is consistent with the "future durability capability layered beneath the existing contract" framing the task names as one lawful interpretation. However, at no point does the document contain a standalone, explicit statement that **Version 1 itself remains, unmodified, in-memory and non-persistent exactly as currently accepted; that this document does not discharge, satisfy, or amend Scope Lock §14's own `SHALL`; and that durability becomes binding only once a future, separate governance stage is itself accepted.** The closest existing statements (the vehicle-rejection paragraph in the Status section, and the Final Recommendation's closing lines) each gesture toward this relationship while discussing a different question (why an errata is the wrong vehicle; what comes next) rather than stating it as its own determination.

This matters because Scope Lock §14's own `SHALL` is the single most load-bearing clause this entire document exists in tension with, and the task's own review question 3 specifically tests whether the document "silently rewrites the accepted meaning of Version 1." It does not — but it also does not affirmatively foreclose that misreading with the explicitness this repository's own precedent (for example, the OCR Mechanism Contract Design's repeated, explicit "this document does not reopen X" statements for every neighbouring boundary) consistently provides for exactly this kind of adjacency. This is a required correction (§15, below).

---

## 6. Durable Scope Review

Every item the subject document names as durable — `Provenance`, `Entity`, `Document`, `Assertion`, `Relationship`, lifecycle transition history, identifier counters — is authorised by, and traceable to, an existing governing clause (Contract Design §4–§8 as corrected by Errata 001, for the five record kinds; Contract Design §11–§12 for lifecycle status and its audit treatment). Cross-checked directly against `InMemoryMemoryCore.kt`'s own internal state (one `Mutex`, five `mutableMapOf` stores, five `Long` counters): nothing in the current implementation's own state is omitted from the subject document's own durable-scope list.

**One item is framed more narrowly than strictly necessary.** The identifier-counter requirement (subject document §3, §6) is worded around "counters" specifically — Memory Core's own current sequential-minting implementation choice — rather than the more abstract property a Contract Design should state ("no identifier collision or ambiguity may occur across a restart, however identifiers are minted"). This is defensible as a description of Memory Core's own already-accepted current behaviour, and does not misstate anything, but it is narrower framing than the tier warrants. Noted as a minor observation (§15), not a blocking defect, since the document already explicitly declines to mandate *how* the counter value is restored ("whether by direct persistence of the counter or by deterministic derivation... is a mechanism choice... not settled here").

No item in the subject document's own durable-scope list is implementation detail masquerading as contract-level responsibility — each is a genuine consequence of Contract Design §4–§12's own already-accepted record and lifecycle model, not an invented new requirement.

---

## 7. Atomicity Review

The proposed rule — one `MemoryCore` contract operation is one atomic durable act — is verified directly against `InMemoryMemoryCore.kt`: every one of its six write operations performs exactly one record creation or one status mutation under its own single critical section; none internally creates or mutates two records as one unit. The rule is therefore a description of existing, already-accepted behaviour, not an invention.

Tested against each named concern: **the existing `MemoryCore` contract** — consistent, confirmed above. **Evidence Registration's two-stage outcome model** — consistent; the subject document's own §4 explicitly confirms `EvidenceRegistrationOutcome`'s already-accepted `ProvenanceNotAuthorised`/`DocumentRegistrationNotAuthorised` variants as the lawful vehicle for an honest intermediate state, rather than inventing a new one. **Independent permission gating** — consistent; the subject document correctly identifies that a shared durable transaction spanning two independently-gated operations would itself violate Scope Lock §14's own "no two operations `SHALL` be bundled" requirement, since a rollback of one operation's own durable commit triggered by a later, separately-gated operation's denial would retroactively undo something the first operation's own permission decision had already, independently authorised. **Provenance-before-document ordering** — correctly addressed under Recovery (§6 of the subject document) rather than Atomicity, since it concerns replay sequencing, not single-operation commit boundaries; this separation is correct, not an omission. **Lifecycle transitions** — consistent; explicitly included as one of the two atomic-act kinds.

The design does not promise a cross-record transaction, and does not prohibit a lawful, already-accepted intermediate durable state — both confirmed directly against the document's own text and against `EvidenceRegistrationCoordinator.kt`'s own existing outcome model. No defect found in this section.

---

## 8. Recovery Review

Provenance-first recovery, referential-integrity validation on reload, counter restoration, deterministic replay, and refusal of a silent empty-store fallback are each abstract, property-level requirements appropriately pitched at Contract Design tier — none names a mechanism, and each traces to an existing governing clause (creation-time referential-integrity checks already enforced by `InMemoryMemoryCore.requireExistingProvenance`; Scope Lock §11's own "deterministic behaviour" requirement, extended here to recovery; Contract Design §14's own "never fabricated, defaulted, or inferred" principle, applied to a broken reference on reload).

**Duplicate-entry handling and interrupted-tail handling are pitched too concretely for this tier, and this is a genuine internal inconsistency, not a matter of taste.** Both requirements are phrased in terms — "an already-processed creation fact," "an entry," "a malformed final entry," "the tail" — that presuppose an append-only-log-style mechanism's own specific failure modes. A duplicate identifier arising from an at-least-once durable-write retry, and a truncated final log line, are both consequences of choosing *that* mechanism specifically; a transactional mechanism (SQLite, or any other ACID-compliant embedded engine) would have a different failure mode entirely — a partially-applied transaction, rolled back or committed as a whole, with no direct analog to a "duplicate entry" or an "interrupted tail." This directly contradicts the subject document's own §2 and §5, both of which explicitly state no file format, serializer, or mechanism is selected. The requirement's own underlying *property* (an at-least-once retry must never be treated as two conflicting records; an incomplete write must never be partially applied) is sound and belongs at this tier — only its current wording is mechanism-specific. This is a required correction (§15, below), and is corroborated independently by §11 and §12 of this review, below.

---

## 9. Failure and Corruption Review

**Failure semantics.** The four distinguished moments (before durable commit, after durable commit but before the in-memory update, recovery failure, storage unavailability) are each individually sound and traceable to existing precedent (`InMemoryMemoryCore`'s own "no `try`/`catch`" discipline; `FileSystemEvidenceArtifactStorage`'s own "fail fast at construction" discipline). The existing thrown-exception model can lawfully express all four without misleading a caller, exactly as Errata 003 already established the identical pattern for a state-precondition violation ("thrown `IllegalStateException`, not a sealed result type") — no new caller-facing public type is required, and the subject document correctly declines to invent one.

The binding promise required by the task ("never report failure after a successful durable commit; never report success without a durable commit") is stated correctly and absolutely in the subject document's own §11. **One minor imprecision:** the "failure after durable commit but before the in-memory update completes" paragraph does not distinguish a full process crash (in which the original call never returns at all — there is no caller left to mislead) from an in-flight coroutine cancellation (in which the caller does observe a thrown `CancellationException`, a materially different, currently-unaddressed sub-case). This does not rise to a required correction — the document's own core binding rule remains correct and sufficient either way — but a clarifying sentence distinguishing the two would strengthen it. Noted as a minor observation (§15).

**Corruption and lifecycle.** The reuse of `ParkerRuntime`'s own existing `RuntimeLifecycleState.FAILED` and exception hierarchy is an authorised, precedent-consistent reuse, not an invention. The subject document correctly, explicitly distinguishes whole-store recovery failure from `MemoryCoreRecordStatus`'s own per-record lifecycle field — the two are never conflated, and the document states this distinction directly rather than leaving it implicit. Write-prohibition after failed recovery is adequately specified, citing the existing `state == RuntimeLifecycleState.RUNNING` gating pattern already used by every other production entry point.

**The "recoverable tail damage" versus "unrecoverable corruption" boundary is, again, too implementation-specific for this tier** — the same finding as §8, above, restated here because the task's own review question 9 tests it independently. "Tail" presupposes a sequential, log-structured representation with a definable end; a transactional mechanism has no equivalent concept (a corrupt SQLite file is corrupt as a whole, or verified intact as a whole, via `PRAGMA integrity_check`, with no "tail" to distinguish). This is the same required correction as §8, not a separate one (§15, below, consolidates it).

No Safe Mode architecture is smuggled into the document — confirmed directly; the subject document explicitly declines to build a connection to `docs/architecture/48-safe-mode-and-recovery.md`'s own aspirational concept.

---

## 10. Versioning and Migration Review

Version-tag presence, additive/defaulted evolution as the preferred path, rejection of unknown future versions, migration authority, and compatibility duties are each correctly pitched — property-level requirements, not a wire-format specification. "Every durable entry carries a schema-version tag" is defensible as mechanism-agnostic despite its use of "entry," since a genuine SQL-based analog exists (a `schema_version` column, or `PRAGMA user_version`) with no forced reading toward log-structured storage specifically — unlike "duplicate entry" and "tail" (§8, §9, above), which have no such transactional analog. This wording is included in the same consolidated correction (§15) purely for consistency of vocabulary across the document, not because it independently misleads a reader the way §8's and §9's findings do.

No hidden durable wire-format contract is introduced — the requirement fixes only that a version signal must exist and be honoured, never its representation, location, or type. Migration authority is explicit and correctly gated behind a future, dedicated governance review before any migration code is written, mirroring Errata 004's own precedent directly.

---

## 11. History, Compaction, and Concurrency Review

**Immutability and history.** Creation-fact immutability and append-only transition recording are each direct, correctly-scoped extensions of Contract Design §12's own already-accepted rule, not new inventions. Replay reproducing identical current state *and* history, not merely current state, is correctly required.

**Compaction.** The rule that no compaction may discard constitutionally relevant history is sound in substance — but "compaction" is itself a log-structured-storage-specific term (there is no direct SQLite analog to a compaction pass; the nearest concept, `VACUUM`, does not reclaim space by discarding rows the way log compaction discards superseded log entries). This is the same finding as §8 and §9, above — mechanism-specific vocabulary presented as a general rule — consolidated into the single required correction at §15.

**Concurrency and ordering.** The total-write-order requirement (needed for `findByTimeRange`'s own existing cross-kind tiebreak to replay faithfully) is correctly derived and correctly abstract. **The requirement that "`InMemoryMemoryCore`'s own single `Mutex`... remains the correct model" names a concrete Kotlin/coroutines class (`Mutex`) as the mechanism to preserve — this is itself the same class of error the document's own §2 explicitly forbids ("does not... select... a Kotlin class").** The underlying behavioural guarantee (serialised, single-writer-at-a-time semantics, no observable interleaving between a durable commit and its corresponding in-memory update) is correct and belongs at this tier; naming `Mutex` specifically does not. Read literally, this wording could be misconstrued as prohibiting an equally correct, non-`Mutex`-based serialisation mechanism (an actor, a single-threaded dispatcher, a database's own native locking) that provides the identical guarantee — precisely the failure mode the task's own review question 12 asks this review to test for. This is folded into the same consolidated required correction (§15).

No cross-process access is introduced, and this exclusion is correctly, explicitly reasoned (both existing filesystem precedents already disclose the identical limitation; no concrete multi-process deployment requirement exists to justify solving it now).

---

## 12. Boundary and Exclusion Review

Every dependency the task names (Permission Engine, Knowledge Memory, Evidence Custodian, runtime orchestration, filesystem types in public interfaces, Docker, Identity Service, constitutional audit storage) is confirmed correctly excluded, each traced to a specific section of the subject document (§12 for the first five; §13 for Identity Service, Docker, and the constitutional audit log). No dependency is silently permitted by omission.

Every exclusion in the subject document's own §13 table is correctly placed and correctly reasoned: Knowledge Memory durability and its legacy-store reconciliation are domain-governance questions this document is not positioned to resolve; Identity Service durability is ADR-024's own sibling obligation, sequenced separately, not silently ignored; World Model is permanently, not provisionally, excluded (consistent with ADR-024 Rule 14); Conversation History is an ADR-024-unresolved open question this document correctly declines to resolve unilaterally; the constitutional Audit log is correctly distinguished from this document's own narrower durability log; runtime composition, Docker volume wiring, and concrete storage technology are each correctly deferred to Implementation-Plan tier, mirroring Evidence Custodian's own precedent.

**Nothing among these exclusions must be decided before this Contract Design can be accepted.** Each is either a separate subsystem's own governance question, or a lower-tier decision this repository's own established sequencing (Contract Design → Scope Lock → Implementation Plan) correctly reserves for later.

---

## 13. Findings

Six findings in total: two substantive, requiring correction before acceptance; four minor, not blocking.

| # | Severity | Finding | Section |
| --- | --- | --- | --- |
| 1 | **Substantive** | The document's relationship to Memory Core Version 1 is correct in substance but not stated as an explicit, standalone determination — leaving room for a future reader to misread acceptance of this document as beginning to satisfy or amend Scope Lock §14's own `SHALL`. | §5 (Version 1 Boundary Review) |
| 2 | **Substantive** | Mechanism-specific vocabulary ("duplicate entry," "tail," "compaction," `Mutex`) is used in four places to state requirements the document's own §2 and §5 promise will remain mechanism-neutral, contradicting that promise and risking unintended exclusion of a lawful, non-log-structured alternative mechanism. | §6, §9, §11 |
| 3 | Minor | The "failure after durable commit, before in-memory update" moment does not distinguish a full process crash from an in-flight cancellation, though the core binding rule remains correct either way. | §9 |
| 4 | Minor | "Constitutionally relevant history" is not explicitly defined, though its intent (every creation fact and every transition, without exception) is clear from context. | §11 |
| 5 | Minor | The identifier-counter requirement is framed around Memory Core's own current sequential-counter implementation rather than the more abstract "no identifier collision across a restart, however minted." | §6 |
| 6 | None (confirmed sound) | Atomicity, ADR-024 authority boundary, versioning-tag requirement in substance, corruption/lifecycle reuse of `FAILED`, boundary/exclusion placement — all reviewed and found correct without qualification. | §4, §7, §10, §12 |

---

## 14. Required Corrections

Two corrections are required before this document may be accepted:

1. **Add an explicit, standalone statement of this document's relationship to Memory Core Version 1** — its own subsection, not folded into the vehicle-selection reasoning or the Final Recommendation. It must state plainly that Version 1 remains, unmodified, in-memory and non-persistent exactly as currently accepted; that this document does not discharge, satisfy, or amend Scope Lock §14's own `SHALL`; and that durability becomes binding only once a future, separate governance stage (naming which of "a later Memory Core version," "a layered capability beneath the existing contract," or "a separately governed durability programme" applies, rather than leaving the choice implicit) is itself accepted.
2. **Restate every mechanism-specific requirement in mechanism-neutral, property-level terms.** Specifically: replace "duplicate-entry handling"/"interrupted-tail handling" (§6 of the subject document) with a formulation such as "a write durably committed more than once due to a retry must be idempotently recognised, never treated as two conflicting records" and "a write that did not complete before an interruption must never be partially applied"; replace "compaction" (§9) with "any future storage-efficiency mechanism"; replace the `Mutex`-naming sentence (§10) with a purely behavioural statement — the durable layer must preserve the same serialised, single-writer-at-a-time guarantee `InMemoryMemoryCore`'s own current single-lock discipline provides, whether by continuing to use one or an equivalent serialisation mechanism.

The three minor findings (§13, rows 3–5) are recommended refinements, not required corrections — the document remains internally sound and unambiguous without them, though addressing them would strengthen its clarity.

---

## 15. Constitutional Verdict

```
REQUIRES REVISION
```

The governance vehicle is correctly chosen (§3) and no finding in this review calls it into question. ADR-024's authority is correctly respected throughout (§4). The substance of every determination — durable scope, atomicity, recovery, failure semantics, versioning, immutability, boundary preservation, and exclusions — is sound, traceable to existing governing clauses, and free of invented authority. What blocks acceptance is narrower and specific: one clarity gap on the single most load-bearing boundary this document sits against (Version 1's own frozen scope), and one internally inconsistent use of mechanism-specific vocabulary in a document whose own stated discipline is mechanism neutrality. Both are corrections to this document's own text, not a redirection to a different vehicle and not a prerequisite constitutional decision outstanding elsewhere.

---

## 16. Recommended Next Step

The drafting session (or an equivalent follow-up) should apply the two required corrections in §14 directly to `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`, then request a narrow defect-confirmation review — not a full re-review — verifying only that the two corrections were made correctly and that nothing else in the document was altered, mirroring this repository's own established "narrow correction pass, then defect-confirmation review" pattern already used for the OCR Mechanism Scope Lock and Implementation Plan. Only after that confirmation should the document's own Status be changed from Draft to Accepted, and only then is a Memory Core Durability Scope Lock authorised to begin.

---

## 17. Git Confirmations

- The subject document, `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`, was not modified during this review.
- No persistence was implemented.
- No Scope Lock or Implementation Plan was created.
- Nothing was staged during this review.
- Nothing was committed during this review.
- Nothing was pushed during this review.

## 18. Final Git Status

```
$ git status --short
?? docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md
?? docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Only the draft Contract Design and this review document are uncommitted. Nothing else changed.
