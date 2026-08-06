# Memory Core — Durability Contract Design

## Status

**Contract design only. Not an amendment, not a Scope Lock, not an Implementation Plan, not an implementation.** No Kotlin is implemented, proposed as a diff, or changed by this document. No schema, file format, serializer, Docker volume, or storage path is fixed by this document except where explicitly stated that governance already requires one. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

**Governing vehicle, determined before drafting, not assumed.** This document is a new, dedicated Contract Design — `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md`, sibling to `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, never an edit to it. Four alternatives were considered and rejected:

- **An errata to `MEMORY_CORE_CONTRACT_DESIGN.md`.** Rejected. Every one of the four existing errata (Errata 001–004) corrected or additively extended something already *inside* Version 1's own frozen scope — a miscount, a missing candidate type, a missing lifecycle operation, a missing principal parameter. Durability is not inside that scope; Scope Lock §14 states outright, as a `SHALL`, that "Memory Core `SHALL` use an in-memory, non-persistent storage implementation for Version 1." Reopening that boundary is a materially different kind of change than any errata this Programme has made, and warrants its own document, not a fifth errata pretending to be the same kind of correction the first four were.
- **A new ADR.** Rejected. `docs/adr/ADR-024-module-event-audit-durability-boundary.md` (Accepted) already is the cross-cutting constitutional authority here — it settles *what* must be durable (Memory Records, Principal records, an Audit log), *what* may remain in-memory (World Model, ordinary working state), and the correct reading of "durable" until a persistence layer exists. This document does not reopen or duplicate that decision; it operationalises it for one named subsystem, exactly the relationship every other domain-specific Contract Design in this repository already has to its own governing CDR/ADR (the OCR Mechanism Contract Design implements CDR-007's classification without itself being a CDR; Evidence Custodian's Contract Design implements CDR-006 the same way).
- **A CDR.** Rejected. The Parker Constitution contains no persistence or durability doctrine to interpret (confirmed by direct search of `docs/architecture/parker-constitution.md` — no match). CDR-005's own Decision Rules reserve a CDR for a domain self-certification that is "genuinely contested, ambiguous, or would require choosing between two or more constitutionally plausible readings." No such contest exists here: ADR-024 already resolved the one constitutional question, and what remains — transaction boundary, recovery ordering, versioning rules, mechanism comparison — is ordinary domain-governance design work.
- **A `docs/reviews/` planning/proposal document instead of a canonical Contract Design.** Rejected for this step specifically. That path is correct when the *enabling constitutional authority* does not yet exist (as when the OCR Mechanism's own Amendment Proposal preceded an amendment authorising a new Evidence Intelligence dependency). Here, the enabling authority (ADR-024) is already Accepted, and two planning reviews (`MEMORY_AND_KNOWLEDGE_RESTART_PERSISTENCE_PLANNING_REVIEW.md`, `MEMORY_AND_KNOWLEDGE_DURABILITY_CONTRACT_DESIGN_PLANNING_REVIEW.md`) are already committed on `main`, satisfying this repository's own planning-first discipline. Proceeding directly to a Contract Design mirrors exactly how `OCR_MECHANISM_CONTRACT_DESIGN.md` was drafted directly once Amendment 2 (its own enabling authority) was already accepted.

This document accepts `docs/adr/ADR-024-module-event-audit-durability-boundary.md`, `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` (as amended by Errata 001–004), and `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` as frozen, normative inputs, and does not reopen, redesign, or reinterpret any of them. It fixes the durability boundary those documents already anticipated but left unresolved.

**Repository baseline confirmed before this document was drafted:** `main` at `41d81fb701d0f1c4d09f4e1bab64bed6a44a3b61`, working tree clean, `origin/main` identical, both prior durability planning reviews confirmed committed.

---

## Context and Constitutional Basis

ADR-024 §D, Rule 13, creates the obligation this document exists to satisfy: "Memory Records... Principal records... and an Audit log" must eventually be durable. Rule 15 fixes the correct present-tense reading until that happens: "'durable' should be read as 'logically durable within process lifetime; physical durability is a reserved seam.'" `IMPLEMENTATION_GAPS.md` #51 tracks the same gap, open, and states plainly that closing it "requires an ADR before persistence... becomes load-bearing" — a requirement ADR-024 has now satisfied at the principle level, leaving the domain-tier design (this document) as the correct next step, not a further ADR.

Memory Core Scope Lock §16, Risk 1, independently names this exact follow-up as required, not optional: "persistence is named in the Out-of-Scope Register... as a required follow-up before Memory Core is relied upon for anything genuinely durable, not an optional enhancement." This document is that follow-up, for Memory Core specifically. It does not address Knowledge Memory, Identity, or any other subsystem ADR-024 names — see §12, below.

**The constitutional principle governing every determination below, restated from the Contract Design's own Failure Behaviour section (§14) and applied one layer deeper:** *an unknown value is never fabricated, defaulted, or inferred without a disclosed inference step, and reasoning does not overwrite history.* Every recovery, corruption, and versioning rule in this document is that same principle, applied to the durable layer rather than the in-memory one.

---

## Relationship to Memory Core Version 1

Memory Core Version 1 is unchanged by this document, in every respect. Memory Core Version 1 remains an in-memory, non-persistent implementation, exactly as currently accepted and exactly as `InMemoryMemoryCore.kt` currently implements it. Scope Lock §14's own `SHALL` — "Memory Core `SHALL` use an in-memory, non-persistent storage implementation for Version 1, and `SHALL NOT` introduce any external database, cloud storage, or network synchronisation dependency" — remains fully authoritative, unamended, and undischarged by anything in this document. No requirement fixed below is satisfied, partially or wholly, by Version 1's own current implementation, and none is required of it.

This document does not supersede, discharge, or begin to satisfy any Version 1 `SHALL`. It fixes requirements for a future durability capability layered beneath Memory Core's own already-frozen `MemoryCore`/`MemoryRetrieval` contracts — a capability that does not yet exist, is not authorised to be built by this document, and remains subject to a later, separate governance stage (a Memory Core Durability Scope Lock, named in the Final Recommendation, below) and a later, separate Implementation Plan before any Kotlin implementing it may be written. Until that future work is itself accepted and implemented, Memory Core continues to operate exactly as Version 1 already specifies: in-memory, non-persistent, and unchanged.

Nothing in this document may be read as reopening, relaxing, or beginning to satisfy Scope Lock §14's own frozen text. Any future document that does relax or amend it must do so explicitly, on its own terms, and is not accomplished implicitly by this one.

---

## 1. Purpose

Fix, without selecting an implementation technology, the durability boundary a future Memory Core Implementation Plan unit must build to: what must survive a process restart, at what granularity, with what atomicity, recovery, corruption-handling, versioning, and concurrency guarantees — so that Memory Core's own already-frozen `MemoryCore`/`MemoryRetrieval` contracts, record shapes, lifecycle rules, and permission boundary are each preserved unchanged, and a caller of either interface cannot observe, from the public contract alone, whether the underlying implementation is in-memory or durable.

---

## 2. Non-Responsibilities

This document does **not**: select a storage technology, file format, serializer, or Kotlin class (§5); design a schema or migration tool (§8); modify `src/interfaces/MemoryCore.kt`'s or `MemoryRetrieval`'s own public shape in any way; introduce a permission check, an owner-authority decision, or any dependency on `PermissionEngine`, `EvidenceCustodian`, or Knowledge Memory into the storage layer (§12); design Docker volumes or `ParkerRuntimeConfig` fields; or resolve Knowledge Memory's, Identity's, World Model's, Conversation History's, or the constitutional Audit log's own durability (§13). Each remains explicitly out of scope, not silently deferred by omission.

---

## 3. Durable Record Scope

Durable, in full: the five existing record kinds exactly as `MEMORY_CORE_CONTRACT_DESIGN.md` §4–§8 (as corrected by Errata 001) already define their fields — `Provenance`, `Entity`, `Document`, `Assertion`, `Relationship` — with no field added, removed, or renamed by this document. Durable in addition: every lifecycle status transition (Contract Design §11), recorded as its own discrete, appended fact — not merely the current `status` value overwritten in place — because Contract Design §12's own "Audit behaviour" already treats each transition as part of the constitutional audit trail ("a future reader reconstructs a record's full history by traversing its `Relationship`s and reading its transition events"); a durability design that persisted only the *current* status and discarded the transition sequence would durably preserve less history than the existing, already-accepted in-memory contract's own audit model already promises. Durable in addition: each of the five per-kind identifier counters' own current value — whether by direct persistence of the counter or by deterministic derivation from the highest persisted identifier of that kind at recovery time is a mechanism choice (§5), not settled here; either is lawful provided the restored value is correct.

**Not separately durable, and not required to be:** nothing beyond what Contract Design §4–§8 and §11 already establish as this Programme's own record and status model. This document introduces no new record kind, no new field, and no new status value.

---

## 4. Atomicity

**One `MemoryCore` contract operation is one atomic durable act.** Confirmed directly from `InMemoryMemoryCore.kt`: every one of its six write operations (`createProvenance`, `createEntity`, `registerDocument`, `createAssertion`, `createRelationship`, `transitionStatus`) performs exactly one record creation or one status mutation under its own single critical section — no operation internally creates or mutates two records as one unit. Durability preserves this exactly: the atomic unit is one record's creation, or one record's status transition, never more.

**No invented multi-record or cross-coordinator transaction.** `EvidenceRegistrationCoordinator.register`'s own two-call sequence (`createProvenance`, then `registerDocument`) is a Runtime-level, coordinator-owned sequence, not a `MemoryCore` transaction, and durability must not invent a new cross-call atomicity spanning it. Doing so would also conflict with Scope Lock §14's own `SHALL`: "no two operations `SHALL` be bundled such that Runtime cannot evaluate a permission decision for one without also authorising the other" — a bundled durable transaction across two independently-gated operations would make exactly that bundling structural.

**Higher-level workflows may lawfully expose durable intermediate states where their accepted outcome models already distinguish them.** `EvidenceRegistrationOutcome` already carries `ProvenanceNotAuthorised` and `DocumentRegistrationNotAuthorised` as named, honest, non-exceptional outcomes — each already documents that the artefact or the `Provenance` "remains genuinely... durably" whatever it already was, even though a later step did not complete. Durability does not alter this; it makes the "durably" in that existing KDoc literally true rather than aspirational. A caller receiving `ProvenanceNotAuthorised` after this document's own design is implemented should be able to trust that the `Provenance` it names has, in fact, survived a restart — this document's own contribution is making that already-accepted intermediate state real, not inventing a new one.

---

## 5. Durability Mechanism Boundary

**This Contract Design fixes required properties; it does not select a mechanism, and no separate ADR is required for that selection.**

Repository precedent settles the tier at which mechanism selection belongs: `FileSystemEvidenceArtifactStorage`'s own KDoc states the filesystem was "the storage mechanism selected for this Unit after comparing it against embedded-database and in-memory-only alternatives" — a decision made at Implementation Plan/Unit level, not in Evidence Custodian's own Contract Design. This document follows the same discipline: it fixes *that* Memory Core's durable layer must provide single-record atomicity (§4), deterministic recovery (§6), the corruption/lifecycle behaviour (§7), and the versioning discipline (§8) below — not *which* concrete technology provides them.

**The append-only-replay direction the two prior planning reviews recommend is assessed here, not selected.** It is a lawful, well-grounded *candidate* direction — it extends two already-reviewed precedents (`FileSystemEvidenceArtifactStorage`'s atomic-write discipline; `FileSystemEvidenceDeletionAudit`'s append-then-force discipline), requires no new runtime dependency (consistent with this repository's own demonstrated bar for adding one — Apache Tika was added only once, with a documented, verified justification), and structurally cannot let Memory Core's own retrieval surface exceed Scope Lock §10's frozen "structural criteria only, no scoring, no ranking" limit, since replay only ever re-exercises `InMemoryMemoryCore`'s own already-constrained write operations. SQLite and other embedded engines remain lawful alternatives this document does not foreclose, each with the trade-offs the prior Durability Contract Design Planning Review already names. **No separate ADR governs this choice** — it is Implementation-Plan-tier engineering discretion, exactly as Evidence Custodian's own storage-technology choice was, informed but not bound by this document's own recommendation.

**What this document does fix, regardless of mechanism:** whichever mechanism is eventually selected must satisfy every requirement in §4, §6, §7, §8, §9, and §10 below in full; none of those requirements is negotiable by mechanism choice.

---

## 6. Recovery

**Deterministic replay.** The same durable state must reconstruct the same in-memory state, in the same order, every time — extending Scope Lock §11's own already-frozen "deterministic behaviour" requirement (currently stated for retrieval only) to the recovery process itself.

**Ordering.** `Provenance` records must be available before any `Entity`, `Document`, `Assertion`, or `Relationship` record that references one — mirroring the creation-time rule `InMemoryMemoryCore.requireExistingProvenance` already enforces, extended to recovery time.

**Referential-integrity validation is required during recovery, not optional.** A reloaded `Entity`/`Document`/`Assertion`/`Relationship` whose `provenanceId` does not resolve, or whose Memory-Core-owned relationship endpoint does not resolve, must cause recovery to fail — never be silently accepted. This is the direct application of the Contract Design's own governing principle (§14 there: "an unknown value is never fabricated, defaulted, or inferred") to the recovery path: silently trusting a broken reference on reload would be exactly the fabrication that principle already forbids.

**Per-record-kind identifier counters** must be restored to (highest persisted identifier of that kind) + 1 — never reset to 1, and never left at a value that could collide with, or leave a gap ambiguous against, an already-persisted identifier.

**Repeated durable record handling.** A creation fact that was durably committed more than once — the expected consequence of an at-least-once durable-write discipline, however the durable mechanism achieves it — must be recognised and idempotently skipped during replay, never treated as a second, conflicting record. A repeated *identifier* carrying *different* content is not a repeated record in this sense; it is corruption (§7).

**Partial durable history handling.** A write that did not complete before an interruption is treated as a write that never happened — discarded during replay, never partially applied — mirroring `FileSystemEvidenceArtifactStorage`'s own existing guarantee that a crash mid-write can only ever orphan an incomplete artefact, never corrupt one already committed.

**No silent empty-store fallback.** If durable state exists but cannot be fully and validly recovered, Memory Core must never silently begin as though no state had ever existed. This is a recovery failure (§7), reported as such, never masked as a fresh, empty store.

---

## 7. Corruption and Lifecycle

**The boundary between a partial, not-yet-complete write and unrecoverable corruption is exactly this:** a write that was interrupted before completing — never fully durably committed — is partial durable history (§6): discarded, not fatal. A malformed, inconsistent, or referentially-broken record that was already durably committed indicates genuine corruption and is unrecoverable under this contract; no partial-repair, best-effort reconstruction, or silent skip-and-continue behaviour is authorised for it.

**Use of the existing `FAILED` lifecycle model.** Unrecoverable corruption during startup must surface through `ParkerRuntime`'s own already-existing `RuntimeLifecycleState.FAILED` transition and its own existing `ParkerRuntimeException` hierarchy (mirroring `DependencyConstructionFailed`'s own "naming the step that failed" pattern) — exactly as any other dependency-construction failure does today. No new status value is added to `MemoryCoreRecordStatus` for this: that enum governs one record's own lifecycle, never the whole-store recovery outcome, and conflating the two would be a category error this document declines to make.

**No platform Safe Mode programme is invented here.** `docs/architecture/48-safe-mode-and-recovery.md` describes an aspirational, currently-unconnected concept; this document does not build the first connection between it and a genuine failure mode. Whether Parker should eventually support a degraded, partial-operation mode remains a separate, later architectural question, deliberately not conflated with this narrower corruption-handling boundary — the same discipline ADR-024's own Reasoning section already applies to keep gap #50 and gap #51 from being solved as though they were one problem.

**Writes are prohibited after incomplete or failed recovery.** This is already structurally true today and this document requires it to remain so: every production entry point on `ParkerRuntime` (`submitEvidence`, `analyseEvidence`, and any future Memory Core write path) already checks `state == RuntimeLifecycleState.RUNNING` before proceeding, throwing `ParkerRuntimeException.NotRunning` otherwise. A Memory Core recovery step that does not complete successfully must be sequenced as one of the construction steps `start()` cannot pass without reaching `FAILED` first — no write path may become reachable while recovery remains incomplete or failed.

---

## 8. Versioning

**Every durably stored record carries an explicit schema-version tag.** No current record needs one — Errata 004 itself confirms "candidate types and stored record types are unchanged... no stored data migration is required" for its own amendment — but this Programme's own history (four errata to date) shows schema evolution is a recurring, expected event, and the tag costs nothing to include now and everything to retrofit later.

**Additive, defaulted evolution is the preferred and presumptive path.** Consistent with `MEMORY_ARCHITECTURE_RECONCILIATION.md` §14's own "additive, defaulted, never breaking" discipline: a new optional field with a documented default requires no migration pass — an absent field on load is simply treated as its own default, exactly as `Provenance.contentNature`'s own "defaults to `UNKNOWN`" precedent already establishes one layer earlier, at submission time.

**Unknown future versions must never be silently interpreted.** If a durably stored record's own version tag is newer than the currently-running code understands (a legitimate scenario — for example, after a rollback to an older Parker build), the current code must not guess at how to read it, silently ignore unrecognised fields, or fail with an unhelpful, undiagnosed parse error. It must be recognised, explicitly, as an unreadable version, and treated as a recovery failure (§7) — the same "never fabricated, defaulted, or inferred without a disclosed inference step" principle applied to forward compatibility specifically.

**Migration authority is explicit.** A genuinely breaking change (a field renamed, removed, or given a new mandatory meaning) requires its own dedicated governance review — an amendment to this document — accepted *before* any migration code is written, mirroring exactly how Errata 004 itself was drafted and accepted before Implementation Plan Unit 10 implemented anything against it. This document does not pre-design that migration mechanism in the absence of a concrete breaking change to migrate, consistent with Scope Lock §15's own "no structure without a concrete need" discipline applied elsewhere in this Programme.

---

## 9. Immutability and Transitions

**Creation facts remain immutable, durably, exactly as they already are in memory.** Contract Design §12 already fixes which fields no operation may ever alter after creation (identifier, Provenance reference, creation timestamp, and each record kind's own "core content"); the durable representation of a creation fact must never be rewritable once committed, for the same reason the in-memory contract already forbids it — this document extends an existing rule to a new layer, it does not invent one.

**Lifecycle transitions are appended, not rewritten**, per §3 and §6, above — the durable log of a record's own history grows only forward.

**Replay must reproduce the same current state and the same history**, not merely the same current state — a mechanism that could reconstruct correct final field values without preserving the sequence of transitions that produced them would satisfy less than what Contract Design §12's own audit-behaviour model already requires of the in-memory contract today.

**No storage-efficiency mechanism may discard constitutionally relevant history.** Should a future mechanism ever introduce any representation optimisation for storage efficiency, it may only ever preserve every creation fact and every transition in some recoverable form — never a lossy summarisation that erases the fact that an intermediate status, dispute, or supersession ever occurred. This is not a new constraint this document invents; it is Contract Design §12's own existing audit-trail guarantee, restated as a durability-layer boundary so a future implementer cannot "optimise" past it.

---

## 10. Concurrency and Ordering

**Required ordering guarantees.** A total order across all writes — not merely a per-kind order — must be preserved by the durable mechanism, because `InMemoryMemoryCore.findByTimeRange`'s own existing, already-frozen tiebreak rule ("a fixed kind order... and then insertion order within that kind") depends on cross-kind insertion order being knowable, not merely reconstructable within one kind alone. A mechanism that durably preserved only a per-kind order would be unable to faithfully reproduce this already-existing, already-tested retrieval guarantee on replay.

**Existing serialised write ordering is preserved, not changed.** `InMemoryMemoryCore`'s own existing single-writer-at-a-time discipline, guarding every store and counter under one serialisation point, remains the correct behavioural model — whatever mechanism provides it; this document requires that a durable write and the corresponding in-memory update occur as one atomic unit from the caller's own perspective, under the same serialised discipline as today — never observably interleaved such that a reader could see an in-memory state not yet durably committed, or a durable state not yet reflected in memory. Whether the mechanism itself commits durably before or after the in-memory update is an implementation-level sequencing choice (§5); the caller-observable atomicity is not.

**No cross-process access is introduced.** Both existing filesystem precedents (`FileSystemEvidenceArtifactStorage`, `FileSystemEvidenceDeletionAudit`) already, explicitly disclose "no cross-process concurrency guarantee... an in-process guard only." This document inherits that same disclaimer rather than attempting to close it now — Parker's own current deployment (`docker-compose.yml`'s single `parker` service) has never contemplated concurrent processes sharing one data directory, and solving a problem with no identified concrete requirement would itself violate Scope Lock's own "burden of proof favours exclusion" principle.

---

## 11. Failure Semantics

Four failure moments, distinguished, not treated as one uniform case:

- **Failure before durable commit** (the mechanism is unreachable, storage is full, or the write otherwise never reaches durable storage). The operation must not report success. Mirroring `InMemoryMemoryCore`'s own existing "no `try`/`catch`, faults propagate" discipline for validation failures, a genuine durability fault propagates as a thrown exception — never returned as a completed `MemoryCoreRecord`, and never silently swallowed.
- **Failure after durable commit but before the in-memory update completes.** The narrow crash window in which the durable fact exists but the original caller never received confirmation. This is acceptable and requires no special handling beyond what §6's own replay guarantee already provides: the record is not lost — it will be replayed on the next successful startup — even though the original call may not have returned successfully. The binding promise this document fixes is narrower and absolute: **a caller must never be told a write failed if it was durably committed, or told it succeeded if it was not.** Whichever commit ordering a future mechanism chooses, the value a completed call actually returns to its caller must be truthful about which of the two occurred.
- **Recovery failure** (durable state exists but cannot be validly reconstructed). Covered in full by §6 and §7 — surfaced through the existing `FAILED` lifecycle state, never a silent empty store.
- **Storage unavailability** (the mechanism itself cannot be reached at all — a missing directory, a permission error). Mirroring `FileSystemEvidenceArtifactStorage`'s own existing "fail fast at construction, not deferred to the first write" discipline (`EvidenceArtifactStorageException.InvalidStorageRoot`, thrown at construction), Memory Core's own durable layer must adopt the identical discipline — a misconfigured or unreachable durability mechanism is detected at startup, before `RUNNING`, never discovered only when the first write is attempted.

**No new caller-facing public result type is invented.** `MemoryCore`'s own existing operations already either return a completed record or throw — Errata 003 already settled, for lifecycle transitions specifically, that a state-precondition violation is "a thrown `IllegalStateException`, not a sealed result type." Durability failures follow the identical, already-established pattern: a thrown exception (a new, specifically-named subtype where diagnosis benefits from one, mirroring `ParkerRuntimeException`'s own "naming the step that failed" convention) — never a new sealed `DurabilityOutcome`-shaped wrapper alongside `MemoryCoreRecord`, which would be a new public type this document has no governing instruction to introduce and the existing contracts do not need.

---

## 12. Runtime Boundary

**Durable storage remains strictly below `MemoryCore`.** No method, no parameter, and no new type is added to `src/interfaces/MemoryCore.kt`'s or `MemoryRetrieval`'s own public shape by this document — durability is an implementation detail of whichever concrete class Runtime composes behind those interfaces, invisible to every caller, exactly as `PermissionGatedMemoryCore`'s own decorator is invisible to a caller holding only a `MemoryCore` reference.

**The Permission Engine, Knowledge Memory, Evidence Custodian, and runtime orchestration remain entirely outside the storage mechanism.** Scope Lock §6 already fixes, absolutely, that "Memory Core never evaluates permissions" — the durable storage layer, sitting strictly beneath `MemoryCore` itself, has no occasion to touch a `PermissionEngine` reference either, and this document introduces none. No dependency on Knowledge Memory or Evidence Custodian is introduced by, or required for, durability — Memory Core's own one-directional dependency rule (Scope Lock §14: "Knowledge Memory `SHALL` depend on Memory Core, never the reverse") is unaffected in either direction by anything this document fixes.

**No direct filesystem authority escapes through `MemoryCore` or `MemoryRetrieval`.** Whatever mechanism is eventually selected, no public field or method may expose a raw file handle, path, or storage-specific identifier to a caller of either interface. A caller cannot determine, from the public contract alone, whether the implementation behind it is in-memory or durable — that is the correct, and only correct, boundary this document fixes for the runtime relationship.

---

## 13. Explicit Exclusions

| Excluded | Reason |
| --- | --- |
| Knowledge Memory durability | A separate Programme, governed by its own Scope Lock; requires its own, separate Contract Design (per the Durability Contract Design Planning Review §4). |
| Legacy `KnowledgeRecord`/`InMemoryKnowledgeStore` reconciliation with the Programme 3 `KnowledgeItem` store | A domain-governance decision about which store is canonical, not a persistence-mechanics question this document is positioned to resolve (Planning Review §5). |
| Identity Service (`InMemoryIdentityService`) durability | ADR-024 Rule 13 names Principal records as an equally-required, sibling durability obligation — sequenced separately by whichever future Contract Design pass takes it up, not addressed here. |
| World Model durability | Permanently, not provisionally, excluded — ADR-024 Rule 14 and `MEMORY_ARCHITECTURE_RECONCILIATION.md` §10 both fix belief transience as part of the World Model's own constitutional definition. |
| Conversation History durability | Not named among ADR-024's own three required items; an open question ADR-024 itself does not resolve, and this document does not resolve it either. |
| The constitutional Audit log ADR-024 §D, Rule 17, separately requires | This document's own durable log for Memory Core's writes is not, by itself, a claim of satisfying the broader "every authorized action leaves a record sufficient to reconstruct" obligation, which spans every subsystem's own actions, not only Memory Core's. |
| Runtime composition, `ParkerRuntime.kt` wiring | Implementation-Plan-tier work, not Contract-Design-tier; this document fixes requirements, not wiring. |
| Docker volume declarations, `ParkerRuntimeConfig` fields | Same tier boundary as runtime composition; deferred to a future Implementation Plan, mirroring exactly how Evidence Custodian's own storage-path configuration was added at its own Runtime Integration phase, not at Contract Design time. |
| Implementation technology, schema files, Kotlin classes, serializers, storage paths | Deferred to a future Implementation Plan (§5, above), unless a future amendment to this document expressly authorises fixing one sooner. |

---

## 14. Repository Reuse

**Reused as precedent, not as code:** `FileSystemEvidenceArtifactStorage`'s own atomic-write discipline (temp file, force, atomic move) and `FileSystemEvidenceDeletionAudit`'s own append-then-force discipline are the two existing, already-reviewed durability patterns this document's own requirements (§4, §6, §7) are modelled on. Neither is a dependency of Memory Core's own durability design in the Kotlin sense — no import, no shared class — only in the sense that a future mechanism satisfying this document's own requirements would be expected to justify any material departure from either pattern's own already-accepted reasoning.

**Not adopted, and explicitly rejected as unnecessary at this stage:** a new caller-facing public result type (§11); a new `MemoryCoreRecordStatus` value representing store-level corruption (§7); a platform Safe Mode programme (§7); a cross-process concurrency mechanism (§10); any migration-tooling selection made in advance of a concrete breaking change (§8).

---

## 15. Verification Requirements

Properties a future Scope Lock and Implementation Plan must be capable of demonstrating, by whatever concrete mechanism they choose, preferring structural verification over source-text pattern matching wherever possible:

- **Single-record atomicity.** No durable write mechanism may commit two records, or a record and a transition, as one unit; each of Memory Core's own six write operations remains independently, individually durable.
- **Deterministic, ordered replay.** The same durable state reconstructs the same in-memory state, in the same total write order, every time.
- **Referential-integrity enforcement on recovery.** A reload with a broken `provenanceId` or a broken Memory-Core-owned relationship endpoint fails recovery; it is never silently accepted.
- **Counter correctness.** Every per-kind identifier counter resumes at (highest persisted identifier of that kind) + 1 after recovery.
- **Partial-write tolerance, corruption intolerance.** A write interrupted before completing is recoverable; a malformed, already-durably-committed record is not, and recovery must fail visibly rather than silently repair or skip it.
- **No silent empty-store fallback.** A durable store that exists but cannot be validly recovered never presents as an empty, fresh store.
- **Version-tag presence and unknown-version rejection.** Every durably stored record carries a schema-version tag; a record whose version the running code does not recognise causes recovery to fail, never a silent best-effort read.
- **Immutability and history preservation under replay.** No replayed state ever exposes a rewritten creation fact or a discarded transition.
- **No public-contract change.** `MemoryCore` and `MemoryRetrieval`'s own method signatures remain exactly as Errata 004 already fixed them; no new public type is introduced by durability work.
- **No dependency reachability.** No type reachable from the durable implementation holds a reference to `PermissionEngine`, `EvidenceCustodian`, or Knowledge Memory's own submission interface.

---

## Final Recommendation

This document fixes the durability boundary ADR-024 already authorised in principle and Memory Core Scope Lock §16 already named as required follow-up, without redesigning, reopening, or reinterpreting the Contract Design, the Scope Lock, or any of the four accepted errata. It selects no storage technology, no schema, and no Kotlin shape, leaving each to a future Implementation Plan exactly as this repository's own Evidence Custodian precedent already establishes as the correct tier for that decision. It resolves no question belonging to Knowledge Memory, Identity, World Model, Conversation History, or the constitutional Audit log, each named explicitly rather than silently omitted.

The next governance stage — a Memory Core Durability Scope Lock — is authorised to begin only after independent constitutional review of this document; neither it nor an Implementation Plan is begun here.

MEMORY CORE DURABILITY CONTRACT DESIGN — DRAFT — AWAITING INDEPENDENT CONSTITUTIONAL REVIEW

---

## Independent Constitutional Review (Performed Before Completion)

Audited as if written by another reviewer, against the governing documents re-read for this task:

- **Does this document implement persistence?** No — no Kotlin, schema, serializer, file format, or storage path is fixed anywhere; every mechanism-level question is explicitly deferred to a future Implementation Plan (§5, §13, §14).
- **Does it preserve the existing `MemoryCore`/`MemoryRetrieval` contracts unchanged?** Yes — confirmed against `src/interfaces/MemoryCore.kt` directly; no method signature, field, or public type is added, removed, or altered by anything in this document (§12).
- **Does it preserve the Permission Boundary (Scope Lock §6)?** Yes — §12 states explicitly that the durable storage layer holds no `PermissionEngine` reference and sits strictly beneath `MemoryCore`, which already holds none.
- **Does it preserve the one-directional dependency rule (Scope Lock §14)?** Yes — §12 confirms no Knowledge Memory or Evidence Custodian dependency is introduced in either direction.
- **Does it preserve immutability (Contract Design §12)?** Yes, and extends it explicitly rather than silently assuming it — §9 restates the existing rule and applies it to the durable layer by name.
- **Does it invent new public surface area where the existing contracts already suffice?** Checked directly against Errata 003's own precedent (thrown exception, not a sealed result, for a state-precondition violation) — §11 follows the same pattern rather than introducing a new result type.
- **Does it select a vehicle without justifying the alternatives?** No — the Status section states and rejects three alternatives (errata, new ADR, CDR) with reasoning specific to this repository's own precedent, before settling on a new Contract Design.
- **Does it resolve any question outside its own named scope?** Checked against §13's own exclusion table — Knowledge Memory, Identity, World Model, Conversation History, and the constitutional Audit log are each named and explicitly left unresolved, not silently folded into this document's own recommendations.
- **Is every requirement traceable to an existing governing clause rather than asserted from preference?** Yes — each of §3 through §12 cites the specific Contract Design section, Scope Lock section, Errata, or ADR-024 rule its own requirement extends, rather than inventing a new rule unmoored from accepted governance.

No genuine defect found requiring correction before completion.
