# Memory Core and Knowledge Memory — Durability Contract Design Planning Review

## Status

**Planning review only. No Kotlin, no schema files, no Docker edits, and no governance modifications are made by this document.** This is the lawful next step after `docs/reviews/MEMORY_AND_KNOWLEDGE_RESTART_PERSISTENCE_PLANNING_REVIEW.md`'s own inventory — it determines *direction* (separate or shared contracts, reconciliation approach, transaction boundaries, versioning rules, recovery ordering, corruption handling, and storage-mechanism comparison) so that a future Contract Design amendment has a reasoned basis to draft from. It does not draft that amendment, does not select a final storage technology bindingly, and does not authorise implementation. Nothing is staged, committed, or pushed by this document.

---

## 1. Repository Baseline

- **HEAD:** `b126d74b54402962c506fc4f7aefd3dfc2fcfa0d` (short `b126d74`) — "docs: plan Memory and Knowledge restart persistence," pushed to `origin/main` immediately before this review began.
- **Branch:** `main`.
- **Working tree:** clean.

No discrepancy found.

---

## 2. Authorities Reviewed

- `docs/reviews/MEMORY_AND_KNOWLEDGE_RESTART_PERSISTENCE_PLANNING_REVIEW.md` — this review's own direct predecessor; every finding in that document (store inventory, record shapes, existing precedents, transactional/recovery/schema gaps, Docker pattern) is treated here as given, not re-derived.
- `docs/adr/ADR-024-module-event-audit-durability-boundary.md` (Accepted) — re-confirmed as the controlling authority for what must be durable, what may remain in-memory, and the correct reading of "durable" pending a real persistence layer.
- `docs/architecture/IMPLEMENTATION_GAPS.md` #51 — re-confirmed as the tracked, open gap this line of work addresses.
- `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` §5, §6, §10, §13, §14 — re-confirmed for the frozen retrieval-mode limit (§10: "structural criteria only — no scoring, no ranking"), the one-directional dependency rule (§14: "Knowledge Memory... never Memory Core"), and the independent-per-operation permission-gateability requirement (§14), each directly relevant to the transaction-boundary and mechanism-comparison questions below.
- `docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md` §8, §13, §14 — re-confirmed for the "today's `MemoryStore` becomes Knowledge Memory... renamed, not redesigned" migration framing and the "additive, defaulted, never breaking" extensibility discipline.
- `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN_ERRATA_004.md` — re-confirmed as this Programme's fourth amendment and its own explicit statement that "candidate types and stored record types are unchanged... no stored data migration is required" — i.e., no record-shape migration has ever yet been exercised in this Programme's own history, despite four amendments.
- `src/runtime/InMemoryMemoryCore.kt`, `src/runtime/KnowledgeItemPersistence.kt`, `src/interfaces/KnowledgeStore.kt`, `src/composition/EventPublishingMemoryCore.kt`, `src/composition/PermissionGatedMemoryCore.kt` — re-confirmed exact shape (already read in full across this session's two prior reviews).
- `src/runtime/FileSystemEvidenceArtifactStorage.kt`, `src/runtime/FileSystemEvidenceDeletionAudit.kt` — re-confirmed as this repository's own two existing durable-storage precedents, including `FileSystemEvidenceArtifactStorage`'s own explicit KDoc reasoning for rejecting a database ("adding an embedded database would trade a dependency for a guarantee the filesystem already gives for free") — directly relevant to §11, below.
- `build.gradle.kts` — read fresh. Confirms exactly three production runtime dependencies today: `kotlinx-coroutines-core`, `org.apache.tika:tika-core`, `org.apache.tika:tika-parser-pdf-module`. The Tika dependency's own inline comment states it is "this repository's first third-party (non-kotlinx) production dependency," added only once a concrete, documented capability need (real PDF parsing) was identified and verified against Maven Central during a formal Implementation Plan pass — the precedent this review treats as the model any future database dependency addition would need to follow.
- `docs/architecture/48-safe-mode-and-recovery.md` — re-confirmed (via the prior review) as an existing but aspirational, currently-unconnected concept, relevant to §10, below.

---

## 3. Scope and Objective

Determine, for Memory Core and Knowledge Memory specifically: whether they need separate persistence contracts; how the two existing Knowledge stores are reconciled; the correct atomic transaction boundary; schema versioning and migration rules; startup recovery ordering; corruption and safe-mode behaviour; which storage mechanism best satisfies ADR-024; and what remains explicitly outside this determination's own scope. Each is addressed in its own section below, with a reasoned recommendation — not a binding decision, which remains a future Contract Design amendment's own responsibility.

---

## 4. Whether Memory Core and Knowledge Memory Require Separate Persistence Contracts

**Recommendation: separate persistence contracts, one per Programme, with a shared underlying mechanism permitted but not required.**

The two subsystems are governed by separate Scope Locks (Programme 2, Programme 3), each independently freezing "persistence... deferred to a future, separately-justified storage design" in near-identical language — meaning each Programme's own governance already anticipates resolving this *separately*, mirroring how each Programme received its own Contract Design/Scope Lock/Implementation Plan in the first place rather than one combined document for "Memory." More concretely, Memory Core Scope Lock §14 fixes a one-directional dependency rule as a `SHALL`/`SHALL NOT`: "Knowledge Memory `SHALL` depend on Memory Core, never the reverse." A single, combined persistence contract spanning both risks quietly blurring that direction — for example, by describing a shared transaction or a shared migration sequence that implicitly requires Memory Core's own schema to accommodate Knowledge Memory's needs. Two separate contracts, each owned by its own Programme's own governance, keep that boundary as explicit in the persistence design as it already is in the read/write contract design.

This does **not** mean two separate storage *mechanisms* are required. A single embedded storage engine (whichever §11 recommends) can house both Programmes' own data — in SQLite terms, separate tables within one database file; in append-only-log terms, separate log files or a shared log with a record-kind discriminator — without either Programme's own governance document needing to reference the other's schema. The distinction this review draws is between the **governance artifact** (must be separate, one per Programme, each independently amendable) and the **operational mechanism** (may be shared, for deployment simplicity, without constitutional cost, provided the mechanism itself introduces no Kotlin-level dependency of Memory Core's own code on Knowledge Memory's, or vice versa).

---

## 5. Reconciling the Two Knowledge Stores

Two structurally distinct, both-live, both-in-memory Knowledge stores exist today (per the prior review's own §5.5 finding): `InMemoryKnowledgeStore` (legacy `KnowledgeRecord`, feeding `ReasoningContextAssembler`) and `InMemoryKnowledgeItemPersistence` (Programme 3 V2 `KnowledgeItem`, backing `DefaultKnowledgeSubmission`).

**Recommendation: prioritise durability for the V2 `KnowledgeItem` store; explicitly defer the legacy `KnowledgeRecord` store's own durability pending the domain's own already-anticipated rename/merge.**

Reasoning: the Reconciliation document itself already names the legacy store's own eventual fate — "today's `MemoryStore` becomes Knowledge Memory... renamed... and only then, once Memory Core itself exists, adapted so that Knowledge Memory's submission path constructs or references real Memory Core Provenance/Entity records" — a migration explicitly "scheduled as part of a future Knowledge Memory Contract Design revision," not yet performed. Durably persisting the legacy store's own flat, provenance-free `KnowledgeRecord` shape now would commit a disk format to a structure the domain's own governance already expects to retire, risking exactly the kind of schema churn §7's own migration-rule discussion below is meant to minimise. The V2 `KnowledgeItem` store, by contrast, already carries Memory Core provenance references and an honest evidential-state classification — it is the shape this Programme is actually converging toward, and the one actively growing via `DefaultKnowledgeSubmission`'s own production wiring today.

This recommendation does not resolve the two-store question itself — that remains, as the prior review already found, a domain-governance decision, not a persistence-mechanics one. It only states which store a durability design should treat as primary *if* the domain has not resolved the question first, so that persistence work is not blocked indefinitely waiting for an unrelated rename decision, while also not accidentally pre-empting that decision by durably enshrining the structure most likely to be retired.

---

## 6. Atomic Transaction Boundaries

**Recommendation: the correct atomic unit, for both subsystems, is exactly one record — one create, or one status transition — never a multi-record or cross-coordinator transaction.**

Confirmed directly from `InMemoryMemoryCore.kt`'s own code: every one of its six write operations performs exactly one map insertion or one status mutation under its single `Mutex`; no method internally creates two records as one unit. `EvidenceRegistrationCoordinator.register`'s own two-call sequence (`createProvenance`, then `registerDocument`) is a **Runtime-level**, coordinator-owned sequence, not a Memory Core transaction — and today's own accepted behaviour already tolerates an orphaned `Provenance` if a crash (or, today, a permission denial) occurs between the two calls. A durability design should preserve this exactly, not invent a new cross-call transaction the in-memory implementation never had — doing so would be new scope, not a preservation of existing, already-accepted behaviour, and would contradict Scope Lock §14's own requirement that "no two operations `SHALL` be bundled such that Runtime cannot evaluate a permission decision for one without also authorising the other" (a bundled, cross-call transaction would make exactly that bundling structural).

The same reasoning applies to Knowledge Memory: `DefaultKnowledgeCandidateEvaluator`'s own evaluation is pure computation with no store write; `KnowledgeItemPersistence.store` writes exactly one `KnowledgeItem`. No multi-record atomicity is required there either.

**Consequence for mechanism choice (see §11):** since no multi-record transaction is required, this finding *reduces* rather than increases the case for a full relational-database transaction model — a single-record-durable-write guarantee (the same guarantee `FileSystemEvidenceArtifactStorage`'s own atomic-move-after-force pattern already provides) is sufficient for everything either subsystem's own contract currently requires.

---

## 7. Schema Versioning and Migration Rules

**Finding: no stored-record migration has ever yet been exercised in this Programme's own history**, despite four Contract Design errata — Errata 004 explicitly confirms "candidate types and stored record types are unchanged... no stored data migration is required" for its own (method-signature-only) amendment. This means any migration rule proposed here is precautionary, not a retrofit of an already-solved problem, and should be sized accordingly — simple enough to cost nothing when unused, present enough to exist the day it is finally needed.

**Recommended rules:**

1. **Every persisted record carries an explicit schema-version tag**, even though no current record needs one yet — precisely because this Programme's own history (four amendments so far) shows schema evolution is a recurring, expected event, not a hypothetical one.
2. **The presumptive migration strategy is "additive, defaulted, never breaking"** — already this Programme's own established discipline (Reconciliation §14), and consistent with every actual erratum to date. Under this rule, adding a new *optional* field with a documented default requires **no migration pass at all**: an absent field on load is simply treated as its own default, exactly as `Provenance.contentNature`'s own "defaults to `UNKNOWN`, never `ORIGINAL`" precedent already establishes for a slightly different purpose (a missing value at submission time, not at load time — the same principle, applied one layer earlier).
3. **A genuinely breaking change (renaming or removing a field, changing a mandatory field's meaning) requires an explicit migration pass, treated as rare and exceptional**, gated by its own dedicated governance review *before* any migration code is written — mirroring exactly how Errata 004 itself was drafted and accepted before Unit 10 implemented anything. This review does not attempt to design that migration mechanism in advance of a concrete breaking change actually being proposed, since doing so now would be speculative engineering with no identified need — the same discipline Scope Lock §15's own Out-of-Scope Register already applies elsewhere in this Programme.
4. **Schema-version tagging and migration rules are themselves subsystem-scoped**, per §4, above — Memory Core's own version tags and Knowledge Memory's own are independent counters, never a single, shared version number implying the two must always change in lockstep.

---

## 8. Startup Recovery Ordering

**Recommended order, building directly on the prior review's own §8 findings:**

1. **Memory Core loads first, in full, before Knowledge Memory.** `KnowledgeItem.evidenceReference` points *into* Memory Core; loading Knowledge Memory first would risk validating references against an incomplete Memory Core state, even though today's own `requireGovernedIdentifier` check never actually re-verifies against live Memory Core data (§7 of the prior review) — the safer, forward-compatible ordering costs nothing today and avoids becoming a silent trap the day a future unit *does* add that verification.
2. **Within Memory Core, `Provenance` loads before the other four record kinds.** Every `Entity`/`Document`/`Assertion`/`Relationship` requires an already-existing `provenanceId` at creation time; if a future design chooses to re-verify this invariant on reload (§8 of the prior review named this as an open recovery-requirement decision, not a settled one), `Provenance` must be fully loaded first, unconditionally, regardless of which choice is eventually made — loading it first is compatible with either later decision, whereas loading it last would foreclose the stricter option.
3. **Each of the five per-kind sequence counters resumes only after its own kind's records are fully loaded**, from (highest persisted identifier of that kind) + 1 — never reset to 1, per the prior review's own §5.1 finding.
4. **`ParkerRuntime.start()` does not transition to `RUNNING` until this entire load completes**, exactly mirroring how every other construction step in `buildAndRegisterRuntimeGraph` today already gates `RUNNING` on successful completion — no new lifecycle state is required for this; the existing `STARTING`/`FAILED` pair already covers it (see §9, below).
5. **Knowledge Memory loads only after Memory Core reports itself fully loaded and ready** — per §5, above, this means the V2 `KnowledgeItem` store specifically, if the legacy store's own durability remains deferred.

---

## 9. Corruption and Safe-Mode Behaviour

**Recommendation: extend `ParkerRuntime`'s own existing `FAILED` lifecycle state and exception-wrapping discipline; do not invent a new "Safe Mode" state as part of this contract.**

`docs/architecture/48-safe-mode-and-recovery.md` describes a Safe Mode concept (disable plugins, pause background agents, restrict external execution) triggered by conditions including "corrupted configuration" — but the prior review already found this document has "no tie to today's actual `ParkerRuntime` lifecycle states," and remains aspirational. Rather than have this Contract Design invent the first real connection between that aspirational concept and a genuine, concrete failure mode, this review recommends the narrower, already-proven path: a corrupt or unreadable durable Memory Core/Knowledge Memory store on startup should surface as a new, specifically-named subtype of the existing `ParkerRuntimeException` hierarchy (mirroring `ParkerRuntimeException.DependencyConstructionFailed`'s own existing "naming the step that failed" pattern), causing `start()` to transition to `FAILED` exactly as any other dependency-construction failure does today. This requires no new runtime state, no new lifecycle transition, and no speculative "partial degraded operation" mode this Contract Design has no concrete requirement to design. Whether Parker should eventually support a genuine degraded Safe Mode remains a separate, later architectural question — this review deliberately does not conflate the two, for the same reason ADR-024's own Reasoning section already gives for keeping gap #50 and gap #51 separate: "believing that making [one thing] also [solves another]... is false."

**What "corrupt" means per candidate mechanism**, for §11's own comparison:

- **Append-only log:** a malformed or truncated final line — detectable by validating each line against a strict, fixed format at load time and treating a malformed *final* line as evidence the last write did not complete (mirroring `FileSystemEvidenceArtifactStorage`'s own temp-file-then-atomic-move discipline, which already guarantees this exact failure mode can only ever orphan an incomplete write, never corrupt an already-committed one). A malformed line anywhere *other* than the last one indicates genuine on-disk corruption, not an interrupted write, and should fail startup rather than silently skip the line.
- **SQLite:** the engine's own `PRAGMA integrity_check` provides a built-in, well-understood corruption detector at startup, at the cost of depending on SQLite's own recovery guarantees rather than ones this repository directly controls and can reason about line-by-line.

---

## 10. Storage Mechanism Comparison — SQLite, Append-Only Files, or Another Mechanism

**Recommendation: an append-only durability log per subsystem, replayed through the already-existing `InMemoryMemoryCore`/`InMemoryKnowledgeItemPersistence` logic at startup — not SQLite — unless a concrete future requirement (dataset size, cross-process access, or ad hoc query needs beyond the seven frozen retrieval modes) makes SQLite's own advantages load-bearing.**

**The append-only + replay design, described (not implemented):** a new, thin decorator — mirroring the already-established `EventPublishingMemoryCore`/`PermissionGatedMemoryCore` decorator pattern exactly — sits above `InMemoryMemoryCore` (and, separately, above `InMemoryKnowledgeItemPersistence`) and, on every successful write, additionally appends a durable log entry describing that write (mirroring `FileSystemEvidenceDeletionAudit`'s own append-then-force discipline). At startup, before `RUNNING`, a new replay step reads the entire log and re-issues each recorded write against a fresh `InMemoryMemoryCore` instance, rebuilding exactly the in-memory state that existed before the last shutdown. **This reuses the entirety of `InMemoryMemoryCore`'s own already-tested read-serving logic unchanged** — the durable layer only ever needs to reproduce *writes*, never reimplement any of the seven retrieval modes.

- **For:** zero new runtime dependency (this repository's own explicit, precedent-setting reasoning in `FileSystemEvidenceArtifactStorage`'s own KDoc — "adding an embedded database would trade a dependency for a guarantee the filesystem already gives for free" — applies here without modification); directly extends two already-reviewed, already-trusted precedents (`FileSystemEvidenceArtifactStorage`'s atomic-move discipline for individual writes; `FileSystemEvidenceDeletionAudit`'s append-then-force discipline for the log itself); requires no new schema-definition technology (no DDL, no migration tooling) beyond the versioning rule in §7; structurally guarantees Memory Core can never accidentally gain semantic/ranked retrieval capability through the storage layer, since the replay path only ever re-exercises `InMemoryMemoryCore`'s own already-constrained write operations.
- **Against:** startup cost grows with total historical write volume (a full replay, not an incremental load) unless a future periodic-snapshot mechanism is added — a real, if currently unquantified, scaling concern; no native cross-process concurrency guarantee, inheriting the same "in-process only" limitation both existing `FileSystem*` precedents already disclose (this review treats that as an acceptable inheritance, not a new risk, since Parker's own current deployment model — `docker-compose.yml`'s single `parker` service — has never contemplated multiple concurrent Parker processes sharing one data directory).

**SQLite**, via a JDBC driver (e.g. `org.xerial:sqlite-jdbc`) — the alternative this task explicitly names:

- **For:** native transactions, foreign-key referential integrity (matching Memory Core's own `provenanceId`-must-exist and relationship-endpoint checks precisely), trivial in-place status updates (an `UPDATE` statement, versus the append-only design's own event-sourced reconstruction), a built-in corruption check (`PRAGMA integrity_check`), and — if a future need for ad hoc queries beyond the seven frozen retrieval modes is ever *lawfully* identified (Scope Lock §10 permits none today) — native indexed querying.
- **Against:** a new runtime dependency, requiring the same documented justification-and-verification process this repository's own Tika precedent already establishes as the bar (§2, above) — not a blocker, but real, non-zero governance overhead; introduces SQL as a wholly new technology surface this codebase has never used for storage (schema DDL, a migration-tooling choice, a driver-lifecycle/connection-pooling decision none of which this review resolves); creates a live temptation, not a technical necessity, to let SQL's own query expressiveness quietly grow past Scope Lock §10's frozen "structural criteria only, no scoring, no ranking" limit — a governance-discipline risk specific to this mechanism, since nothing about SQLite itself prevents writing an `ORDER BY`-based "relevance" query, only this Programme's own continued discipline would.

**A third mechanism, named for completeness and not recommended:** an embedded key-value/log-structured store (e.g., RocksDB, LevelDB, H2 in embedded mode) sits between the two above — native transactions and durability like SQLite, but without SQL's own query-expressiveness temptation. Not recommended primarily because it offers no advantage over the append-only design for Memory Core's own actual (non-relational, non-ranked) needs while still introducing an unfamiliar new dependency this repository has no existing experience integrating, unlike SQLite, which is at least an extremely widely-used, well-documented technology if the dependency cost is ever judged worth paying.

**This review's own recommendation is conditional, not absolute:** append-only + replay is the lower-risk, lower-cost starting point given everything currently known (§6's own finding that no multi-record transaction is required removes SQLite's single strongest advantage; §2's own confirmation that no new dependency has been added to this repository except once, for a concretely justified parsing need, sets a high bar any database dependency should also have to clear). If a future concrete requirement emerges — genuinely large data volumes making full-replay startup impractical, a real need for cross-process access, or a lawfully-expanded query requirement — this review's own recommendation should be revisited, not treated as permanently foreclosing SQLite.

---

## 11. Explicitly Outside Scope

- **Identity Service (`InMemoryIdentityService`) durability.** ADR-024 Rule 13 names Principal records as an equally-required, sibling durability obligation — not addressed by this Contract Design, which is scoped to Memory Core and Knowledge Memory only. A future Contract Design pass must decide whether to sequence Identity durability together with or separately from this work.
- **World Model durability.** Permanently, not provisionally, excluded — ADR-024 Rule 14 and the Reconciliation §10 both fix World Model belief transience as part of its own constitutional definition, not a durability gap awaiting closure.
- **Conversation History durability.** Not named by ADR-024 among its own three required items (Memory, Identity, Audit) — an open question ADR-024 itself does not resolve, and this review does not resolve it either.
- **The constitutional Audit log ADR-024 Rule 13/17 separately requires.** The append-only durability log this review recommends (§11, above) is a persistence mechanism for Memory Core/Knowledge Memory's own state — it is **not**, by itself, a claim of satisfying ADR-024's own, broader "every authorized action leaves a record sufficient to reconstruct" Audit obligation, which spans every subsystem's own actions, not only Memory Core/Knowledge Memory writes. A future unit could potentially build the real Audit mechanism atop a similar append-only pattern, but this review does not claim that connection is already made.
- **Agent Runtime, Task Manager, and Planner Runtime state durability.** Not named by ADR-024; a distinct, later durability question.
- **The legacy `KnowledgeRecord`/`InMemoryKnowledgeStore` rename or merge into the V2 `KnowledgeItem` model.** A domain-governance decision (§5, above), not resolved or pre-empted by this review.
- **Binding selection of a storage mechanism.** §10's own recommendation is reasoned, not final — the actual selection remains a future Contract Design/Scope Lock decision.
- **Any schema file, DDL, or on-disk format specification.** Explicitly prohibited by this task's own instruction; §7 names *rules* a schema must follow, never the schema itself.
- **Docker volume declarations, `ParkerRuntimeConfig` fields, or any Kotlin file.** Explicitly prohibited by this task's own instruction.
- **Cross-process concurrency.** Both existing precedents already disclaim it; this review recommends inheriting that disclaimer rather than solving it, absent a concrete multi-process deployment requirement.

---

## 12. Risks

- **Risk: this review's own conditional recommendation (§10) is read as a final, binding technology selection.** Mitigation: stated explicitly, twice (§10's own opening line and closing paragraph), that the recommendation is reasoned and conditional, not a decision this planning review has authority to make final.
- **Risk: a future implementation lets SQL query expressiveness silently exceed Scope Lock §10's frozen retrieval-mode limit, if SQLite is eventually chosen.** Mitigation: named explicitly in §10's own "Against" list for SQLite, so a future implementer inherits the warning rather than discovering the temptation unprompted.
- **Risk: prioritising the V2 `KnowledgeItem` store (§5) is misread as this review deciding the legacy-store question, rather than merely sequencing durability work around it.** Mitigation: §5's own final paragraph states plainly that the two-store question itself remains unresolved and undecided by this review.
- **Risk: the append-only replay design's own startup-cost-grows-with-history property goes unnoticed until a real dataset makes it a problem.** Mitigation: named explicitly in §10's own "Against" list, with a snapshot mechanism named as the future mitigation path, not designed here.
- **Risk: the recommended durability log (§10) is mistaken for the Audit log ADR-024 itself requires**, prematurely closing gap #51's own Audit-specific component. Mitigation: §11, above, states this distinction explicitly and directly.

---

## 13. Independent Review of This Planning Review

- **Did this review select a storage technology, in violation of its own "no schema files" constraint?** No — §10 compares mechanisms and gives a conditional recommendation; no schema, DDL, or file format is specified anywhere in this document.
- **Did it modify Kotlin, Docker, or governance?** No — confirmed by direct inspection; no file outside this new review document was touched.
- **Did it silently resolve the two-Knowledge-store question rather than merely sequencing around it?** Checked directly: §5's own closing paragraph states the question remains open, for the domain's own governance to resolve.
- **Did it conflate the recommended durability log with ADR-024's own separate Audit requirement?** Checked and explicitly distinguished in §11 — a specific self-correction made during drafting once the overlap was noticed.
- **Is every recommendation traceable to a specific finding, rather than asserted from preference?** Yes — the transaction-boundary recommendation (§6) traces to `InMemoryMemoryCore.kt`'s own code; the mechanism recommendation (§10) traces to `FileSystemEvidenceArtifactStorage`'s own KDoc reasoning and the Tika-dependency precedent (§2); the recovery-ordering recommendation (§8) traces to Memory Core's own referential-integrity rule and Knowledge Memory's own cross-store reference.

No genuine defect found requiring correction beyond the Audit-log distinction already folded into §11 during drafting.

---

## 14. Recommendation and Next Governance Step

This review's own findings are ready to inform, but do not themselves constitute, a Memory Core Contract Design amendment (and a parallel Knowledge Memory one, per §4) addressing durability specifically. The correct next governance step is that amendment — drafted separately for each Programme, informed by this review's own recommendations on transaction boundaries (§6), versioning rules (§7), recovery ordering (§8), corruption handling (§9), and mechanism selection (§10) — followed by the same Independent Constitutional Review and Scope Lock revision discipline every other amendment in this repository has already followed.

---

## 15. Confirmation

- No Kotlin, schema file, or Docker file was created or modified.
- No governance document was amended.
- Nothing was staged, committed, or pushed.

## 16. Final Git Status

```
$ git status --short
?? docs/reviews/MEMORY_AND_KNOWLEDGE_DURABILITY_CONTRACT_DESIGN_PLANNING_REVIEW.md
```

Only this new review document is uncommitted.
