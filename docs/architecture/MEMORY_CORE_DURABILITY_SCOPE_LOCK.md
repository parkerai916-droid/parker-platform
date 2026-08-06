# Memory Core Durability — Scope Lock

## Status

Programme: **Memory Core Durability — Scope Lock.**
Phase: **Final governance document before the Memory Core Durability Implementation Plan.** No Kotlin is implemented, proposed as a diff, or changed by this document. No schema, file format, serializer, Docker volume, or storage path is fixed by this document, except where explicitly stated that an already-accepted document already requires one. Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or pushed.

**This document is binding.** It does not redefine any decision already made. `docs/adr/ADR-024-module-event-audit-durability-boundary.md` (the ADR); `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` as amended by Errata 001–004 (the Contract Design); `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` (the Version 1 Scope Lock); and `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` (the Durability Contract Design) are frozen, normative inputs — this document does not reopen the durability boundary, the mechanism-neutral property requirements, the record shapes, the lifecycle rules, or the permission model any of those four already settled. Its own purpose is narrower and final: fix, without ambiguity, exactly what a Memory Core durability implementation builds, and exactly what it does not. Every capability considered below is marked `IN SCOPE` or `OUT OF SCOPE`. There is no third category. "Future consideration" is not a classification this document uses anywhere.

**Governing-document status of the Durability Contract Design, confirmed before this document was drafted.** `docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_INDEPENDENT_CONSTITUTIONAL_REVIEW.md` found the Durability Contract Design `REQUIRES REVISION` with two Required Corrections; both are verified present in the currently committed text, and `docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md` confirms this directly, word-for-word, against the current text. The Durability Contract Design's own closing status line has not itself been edited to read "Accepted" — that edit is deferred to a future, separately-authorised housekeeping pass, disclosed as such by the Defect Confirmation Review — but its substance is confirmed accepted, and the Defect Confirmation Review's own Recommended Next Step is exactly this document.

**Scope Lock Principle.** A Memory Core durability implementation shall guarantee that Memory Core's own governed state — its five record kinds and their lifecycle transition history — survives a process, VM, host, or power restart, reconstructing exactly the state that existed before the restart. It is not intended to select a storage technology, define a schema, resolve any other subsystem's own durability question, or extend Memory Core's public contract in any way. Where a candidate requirement is plausible, useful, or eventually necessary but is not already fixed by the Durability Contract Design, it is `OUT OF SCOPE` for this document — the burden of proof favours exclusion, not inclusion, throughout.

---

## 1. Executive Summary

This Scope Lock fixes, as binding acceptance criteria, exactly what a Memory Core Durability Implementation Plan must build: durable persistence for Memory Core's own five record kinds (`Provenance`, `Entity`, `Document`, `Assertion`, `Relationship`) and every lifecycle status transition among the four kinds that carry one, such that a process, VM, host, or power restart reconstructs identical current state and identical transition history, with no change to `MemoryCore`'s or `MemoryRetrieval`'s own public shape, no new dependency on `PermissionEngine`, Knowledge Memory, or Evidence Custodian, and no selection of storage technology, schema, or Kotlin implementation type. Mechanism selection remains Implementation-Plan-tier engineering discretion, exactly as the Durability Contract Design §5 already fixed and as `FileSystemEvidenceArtifactStorage`'s own precedent already established for a sibling subsystem. This document introduces no new architecture — it translates properties the Durability Contract Design already fixed into a bounded, binding scope an Implementation Plan can be built and judged against.

---

## 2. Frozen Objectives

A Memory Core durability implementation shall achieve, and shall be judged against, exactly the following — each traced to the Durability Contract Design section that already fixed it:

1. Every one of Memory Core's six write operations (`createProvenance`, `createEntity`, `registerDocument`, `createAssertion`, `createRelationship`, `transitionStatus`) durably commits as exactly one atomic act — never bundled with another operation, never partially applied (Durability Contract Design §4).
2. The same durable state reconstructs the same in-memory state, in the same total write order, every time (§6, §10).
3. `Provenance` records are available before any record that references one, both at creation time (already true) and at recovery time (§6).
4. A reload with a broken `provenanceId` or a broken Memory-Core-owned `Relationship` endpoint fails recovery visibly; it is never silently accepted (§6).
5. Every per-kind identifier counter resumes at (highest persisted identifier of that kind) + 1 after recovery — no reuse, no collision, no ambiguous gap (§6).
6. A write durably committed more than once due to a retry is recognised and idempotently skipped on replay; a write interrupted before completing is discarded on replay, never partially applied (§6).
7. A malformed, already-durably-committed record is genuine corruption and causes recovery to fail visibly — no partial repair, no best-effort reconstruction, no silent skip (§7).
8. No durable state that exists but cannot be validly recovered is ever silently presented as an empty, fresh store (§6, §7).
9. Every durably stored record carries an explicit schema-version tag; a record whose version the running code does not recognise causes recovery to fail, never a silent best-effort read (§8).
10. Creation facts remain immutable durably; lifecycle transitions are appended, never rewritten; replay reproduces the same current state and the same history (§9).
11. No method, parameter, or type is added to `src/interfaces/MemoryCore.kt`'s or `MemoryRetrieval`'s own public shape (§12).
12. No `PermissionEngine`, Knowledge Memory, or Evidence Custodian dependency is introduced into the durable storage layer, in either direction (§12).
13. No raw file handle, path, or storage-specific identifier escapes through `MemoryCore` or `MemoryRetrieval` to any caller (§12).
14. A caller is never told a write failed if it was durably committed, and never told a write succeeded if it was not (§11).

---

## 3. Mandatory Deliverables

| Deliverable | Classification |
| --- | --- |
| Durable persistence for `Provenance` records | **REQUIRED** |
| Durable persistence for `Entity` records | **REQUIRED** |
| Durable persistence for `Document` records | **REQUIRED** |
| Durable persistence for `Assertion` records | **REQUIRED** |
| Durable persistence for `Relationship` records | **REQUIRED** |
| Durable persistence of every lifecycle status transition, as a discrete appended fact (§9) | **REQUIRED** |
| Durable persistence, or deterministic restoration, of each of the five per-kind identifier counters (§3, §6) | **REQUIRED**, mechanism (direct persistence vs. derivation from max persisted identifier) left to the Implementation Plan |
| Deterministic, ordered, referential-integrity-validated startup recovery reconstructing `InMemoryMemoryCore`'s own existing state through governed internal pathways | **REQUIRED** |
| Explicit schema-version tagging on every durably stored record | **REQUIRED** |
| Recovery failure surfaced through the existing `RuntimeLifecycleState.FAILED` state and `ParkerRuntimeException` hierarchy | **REQUIRED** |
| Runtime composition constructing the durable implementation exactly once, preserving the existing `PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval` decorator boundary and the existing composition pattern (no double permission gating) | **REQUIRED** |
| A Docker named volume mapping for the durable storage location, distinguishing container/VM/host/power-loss recovery scope | **REQUIRED** |

All twelve deliverables are `REQUIRED`. A durability implementation that ships persistence for four of the five record kinds and defers the fifth, or that ships persistence without lifecycle-transition history, is not an acceptable interpretation of this Scope Lock — it is a different, smaller scope that would itself require a revision to this document before proceeding.

---

## 4. Explicit Exclusions

Every item below is `OUT OF SCOPE` for a Memory Core durability implementation, with the reason stated directly:

| Excluded capability | Reason |
| --- | --- |
| Selection of a specific storage technology, file format, serializer, or Kotlin class | Durability Contract Design §5 fixes this as Implementation-Plan-tier engineering discretion, mirroring `FileSystemEvidenceArtifactStorage`'s own precedent; this Scope Lock fixes required properties, not a mechanism. |
| Schema/wire-format design beyond "an explicit version tag exists" | Durability Contract Design §8; the concrete representation is an Implementation Plan decision. |
| Any change to `MemoryCore`'s or `MemoryRetrieval`'s own public method signatures, fields, or types | Durability Contract Design §12; Version 1's own already-accepted public contract is frozen and unaffected. |
| Any `PermissionEngine`, `IdentityService`, Knowledge Memory, or Evidence Custodian dependency inside the durable storage layer | Durability Contract Design §12; Memory Core Scope Lock §6's permission boundary is preserved without qualification. |
| Knowledge Memory durability | A separate Programme, its own Scope Lock, its own Contract Design (Durability Contract Design §13). |
| Legacy `KnowledgeRecord`/`InMemoryKnowledgeStore` reconciliation with the Programme 3 `KnowledgeItem` store | A domain-governance decision, not a persistence-mechanics question (Durability Contract Design §13). |
| Identity Service (`InMemoryIdentityService`) durability | ADR-024 Rule 13's sibling obligation, sequenced separately (Durability Contract Design §13). |
| World Model durability | Permanently excluded — ADR-024 Rule 14 and `MEMORY_ARCHITECTURE_RECONCILIATION.md` §10 both fix belief transience as constitutional (Durability Contract Design §13). |
| Conversation History durability | An ADR-024-unresolved open question this document does not resolve (Durability Contract Design §13). |
| The constitutional Audit log ADR-024 §D Rule 17 separately requires | Distinct from, and not satisfied by, Memory Core's own narrower durable write log (Durability Contract Design §13). |
| Backup and replication policy | Not named as a requirement anywhere in the ADR, the Contract Design, or the Durability Contract Design; introducing one now would be an unjustified scope expansion this document's own Scope Lock Principle prohibits. |
| Storage optimisation, indexing, or query performance work beyond correctness | No concrete need identified in any governing document; Durability Contract Design §9 explicitly permits a future storage-efficiency mechanism only under strict history-preservation constraints, and does not require one now. |
| Cross-process concurrent access to the durable store | Durability Contract Design §10 explicitly inherits, rather than closes, both existing filesystem precedents' own disclosed "in-process guard only" limitation; no concrete multi-process deployment requirement exists. |
| Any SQLite adoption, or adoption of any other embedded database, in advance of Implementation Plan-level evidence that the mechanism-neutral requirements below cannot be met safely without one | Durability Contract Design §5 explicitly leaves this an open, unselected alternative; this document selects no mechanism, and neither does the future Implementation Plan without first demonstrating the append-only/replay direction (or any other mechanism-neutral candidate) is insufficient. |

---

## 5. Durable Record Scope

Frozen from Durability Contract Design §3, restated here as binding.

**Durable, in full:** the five existing record kinds exactly as the Contract Design §4–§8 (as corrected by Errata 001) already define their fields — `Provenance`, `Entity`, `Document`, `Assertion`, `Relationship` — with no field added, removed, or renamed by this document or by any Implementation Plan built against it.

**Durable in addition:** every lifecycle status transition (Contract Design §11), recorded as its own discrete, appended fact — never merely the current `status` value overwritten in place.

**Durable in addition:** each of the five per-kind identifier counters' own current value, restored correctly on recovery — whether by direct persistence of the counter or by deterministic derivation from the highest persisted identifier of that kind is a mechanism choice, not fixed here.

**Not separately durable, and not required to be:** nothing beyond what Contract Design §4–§8 and §11 already establish. This document introduces no new record kind, no new field, and no new status value — and forbids any Implementation Plan built against it from introducing one either, without a revision to this Scope Lock first.

---

## 6. Durability Mechanism Boundary

**This Scope Lock fixes required properties. It does not select a mechanism, and no Implementation Plan built against it may select one without first satisfying every property below.**

The append-only-log-plus-replay direction the two prior planning reviews recommend, and which Durability Contract Design §5 assesses as a lawful candidate, remains a candidate only — not selected, not mandated, not foreclosed. SQLite and other embedded engines remain equally lawful alternatives, subject to the same "no new runtime dependency without a documented, verified justification" bar this repository's own Apache Tika precedent already establishes (Durability Contract Design §5). Mechanism selection, including the choice between these and any other candidate, is Implementation-Plan-tier engineering discretion — this Scope Lock neither performs that selection nor authorises the Implementation Plan to skip justifying it.

**What this document fixes, regardless of mechanism:** whichever mechanism is eventually selected must satisfy every requirement in Sections 7 through 13, below, in full. None of those requirements is negotiable by mechanism choice.

---

## 7. Atomicity Rules

Frozen from Durability Contract Design §4.

**One `MemoryCore` contract operation is one atomic durable act.** Each of the six write operations performs exactly one record creation or one status mutation as its own atomic durable unit — never more, never bundled with a second operation.

**No invented multi-record or cross-coordinator transaction.** `EvidenceRegistrationCoordinator.register`'s own two-call sequence (`createProvenance`, then `registerDocument`) remains a Runtime-level, coordinator-owned sequence; no durability implementation may introduce a new cross-call atomicity spanning it. `EvidenceRegistrationOutcome`'s own already-accepted `ProvenanceNotAuthorised`/`DocumentRegistrationNotAuthorised` intermediate outcomes remain the lawful, honest vehicle for the case where the first call durably succeeded and the second did not.

---

## 8. Recovery Rules

Frozen from Durability Contract Design §6.

- **Deterministic replay.** The same durable state must reconstruct the same in-memory state, in the same order, every time.
- **Ordering.** `Provenance` records must be available before any `Entity`, `Document`, `Assertion`, or `Relationship` record that references one.
- **Referential-integrity validation is required during recovery, not optional.** A reloaded record whose `provenanceId` or Memory-Core-owned relationship endpoint does not resolve causes recovery to fail — never silently accepted.
- **Identifier counters restore to (highest persisted identifier of that kind) + 1** — never reset to 1, never left ambiguous against an already-persisted identifier.
- **A write durably committed more than once, due to a retry, is recognised and idempotently skipped during replay** — never treated as a second, conflicting record. A repeated identifier carrying different content is not a repeated record in this sense; it is corruption (Section 9, below).
- **A write that did not complete before an interruption is discarded during replay, never partially applied.**
- **No silent empty-store fallback.** Durable state that exists but cannot be fully and validly recovered is a recovery failure (Section 9, below), never a fresh, empty store.

---

## 9. Corruption and Lifecycle Rules

Frozen from Durability Contract Design §7.

**The boundary between a partial, not-yet-complete write and unrecoverable corruption:** a write interrupted before durably completing is partial durable history (Section 8, above) — discarded, not fatal. A malformed, inconsistent, or referentially-broken record that was already durably committed is genuine corruption and is unrecoverable — no partial-repair, best-effort reconstruction, or silent skip-and-continue is authorised for it.

**Use of the existing `FAILED` lifecycle model.** Unrecoverable corruption during startup surfaces through `ParkerRuntime`'s own existing `RuntimeLifecycleState.FAILED` transition and its own existing `ParkerRuntimeException` hierarchy. No new value is added to `MemoryCoreRecordStatus` for whole-store recovery outcomes.

**No platform Safe Mode programme.** No Implementation Plan built against this Scope Lock may build a connection between Memory Core durability and `docs/architecture/48-safe-mode-and-recovery.md`'s own aspirational, currently-unconnected concept.

**Writes are prohibited after incomplete or failed recovery.** Every production entry point on `ParkerRuntime` already checks `state == RuntimeLifecycleState.RUNNING` before proceeding; a durability implementation must be sequenced as one of the construction steps `start()` cannot pass without reaching `RUNNING`, so no write path becomes reachable while recovery remains incomplete or failed.

---

## 10. Versioning Rules

Frozen from Durability Contract Design §8.

- Every durably stored record carries an explicit schema-version tag.
- Additive, defaulted evolution is the preferred and presumptive path — an absent field on load is treated as its own default, never as a migration trigger.
- An unknown future version tag is never silently interpreted, never silently ignored, and never allowed to fail with an undiagnosed parse error — it is recognised explicitly as an unreadable version and treated as a recovery failure (Section 9, above).
- A genuinely breaking schema change requires its own dedicated governance review — an amendment to the Durability Contract Design — accepted before any migration code is written. No Implementation Plan built against this Scope Lock may pre-design a migration mechanism in the absence of a concrete breaking change to migrate.

---

## 11. Immutability and Transition Rules

Frozen from Durability Contract Design §9.

- Creation facts remain immutable, durably, exactly as they already are in memory — the durable representation of a creation fact is never rewritable once committed.
- Lifecycle transitions are appended, not rewritten — the durable record of a record's own history grows only forward.
- Replay must reproduce the same current state **and** the same history, not merely the same current state.
- No future storage-efficiency mechanism may discard constitutionally relevant history — every creation fact and every transition must remain preserved in some recoverable form; a lossy summarisation erasing the fact that an intermediate status, dispute, or supersession ever occurred is never authorised.

---

## 12. Concurrency and Ordering Rules

Frozen from Durability Contract Design §10.

- **A total order across all writes — not merely a per-kind order — must be preserved**, because `InMemoryMemoryCore.findByTimeRange`'s own existing, already-frozen tiebreak rule depends on cross-kind insertion order being knowable on replay.
- **Existing serialised write ordering is preserved, not changed** — `InMemoryMemoryCore`'s own single-writer-at-a-time discipline remains the correct behavioural model, whatever mechanism provides it. A durable write and its corresponding in-memory update must occur as one atomic unit from the caller's own perspective, under the same serialised discipline as today, never observably interleaved.
- **No cross-process access is introduced.** Both existing filesystem precedents already disclose "no cross-process concurrency guarantee... an in-process guard only"; a durability implementation inherits, not closes, that same disclaimer.

---

## 13. Failure Semantics

Frozen from Durability Contract Design §11. Four failure moments, distinguished, not treated as one uniform case:

- **Failure before durable commit** propagates as a thrown exception — never returned as a completed record, never silently swallowed.
- **Failure after durable commit but before the in-memory update completes** is acceptable and requires no special handling beyond Section 8's own replay guarantee — the record is not lost.
- **Recovery failure** is covered in full by Sections 8 and 9, above — surfaced through the existing `FAILED` lifecycle state, never a silent empty store.
- **Storage unavailability** is detected at construction, before `RUNNING`, mirroring `FileSystemEvidenceArtifactStorage`'s own existing "fail fast at construction" discipline.

**The binding promise, absolute:** a caller must never be told a write failed if it was durably committed, and never told a write succeeded if it was not.

**No new caller-facing public result type is invented.** A durability implementation follows `MemoryCore`'s own existing pattern — a completed record or a thrown exception — exactly as Errata 003 already established for lifecycle-transition state-precondition violations.

---

## 14. Runtime Boundary

Frozen from Durability Contract Design §12.

**Durable storage remains strictly below `MemoryCore`.** No method, parameter, or type is added to `src/interfaces/MemoryCore.kt`'s or `MemoryRetrieval`'s own public shape. Durability is an implementation detail of whichever concrete class Runtime composes behind those interfaces, invisible to every caller.

**The Permission Engine, Knowledge Memory, Evidence Custodian, and runtime orchestration remain entirely outside the storage mechanism.** No dependency on any of them is introduced by, or required for, durability.

**No direct filesystem authority escapes through `MemoryCore` or `MemoryRetrieval`.** No public field or method may expose a raw file handle, path, or storage-specific identifier to a caller of either interface.

**Composition discipline.** A Memory Core durability implementation **shall** be constructed exactly once within `ParkerRuntime.kt`'s own composition graph, **shall** be reachable by every existing consumer through the same decorator boundary that consumer already uses today, and **shall not** introduce double permission gating — a write path already gated internally by its own caller shall not additionally be wrapped by a second, redundant permission-evaluating decorator solely because the underlying `MemoryCore` implementation changed from in-memory to durable. This is a direct consequence of §12's own already-fixed "no `PermissionEngine`... dependency... in the storage layer" requirement, restated for the composition boundary specifically; it fixes no variable name, construction line, or specific decorator-wrapping shape belonging to today's particular composition graph, since that concrete wiring is Implementation-Plan-tier work (Contract Design §13; Version 1 Scope Lock §15's own identical deferral of "composition-ordering in `ParkerRuntime.kt`" to the Implementation Plan), not Scope-Lock-tier. The current, actual state of that composition graph — confirmed by direct reading of `src/composition/ParkerRuntime.kt` as it exists today — is recorded as a disclosed, informational finding in Section 17 (Risks), below, for a future Implementation Plan's own benefit; it is not itself frozen as binding text here.

---

## 15. Verification Scope

Frozen minimum required test surface, restated from Durability Contract Design §15 as binding acceptance requirements, described by behaviour, not implementation:

- Fresh start against no prior durable state produces the same empty Memory Core Version 1 already produces today.
- Write, restart, read round-trips succeed for each of the five record kinds independently.
- A lifecycle transition survives restart, both the resulting current status and the full transition history.
- A broken referential-integrity reference (missing `provenanceId`, missing Memory-Core-owned relationship endpoint) fails recovery visibly on restart.
- Each per-kind identifier counter resumes at max-persisted-identifier-plus-one after restart; no identifier is reused.
- A durably-committed-more-than-once record is idempotently recognised on replay, not duplicated.
- An incomplete, interrupted final write is discarded on replay, not partially applied.
- A genuinely corrupted, already-committed record causes recovery to fail visibly, not silently repaired or skipped.
- A failed recovery leaves every write that was durably committed before the failure unreachable through the failed instance, never exposed as though recovery had partially succeeded.
- Full runtime reconstruction (`ParkerRuntime.start()`) exercises the durable implementation exactly once and reaches `RUNNING` only after recovery completes successfully.
- A Docker-volume-backed restart (container restart, at minimum) demonstrates durable survival end-to-end.
- The full existing repository regression suite passes unchanged, with zero reduction in count, after durability is composed into `ParkerRuntime.kt`.

No verification requirement beyond this list is fixed here; a future Implementation Plan may add mechanism-specific tests but may not omit any of the above without a revision to this Scope Lock.

---

## 16. Out-of-Scope Register

A complete register of deferred capability, collected from every exclusion named across the four frozen documents and this one. Nothing listed here is rejected permanently unless stated as such; each is deferred to a named future scope.

| Deferred capability | Deferred to |
| --- | --- |
| Knowledge Memory durability | A separate Programme, its own Scope Lock and Contract Design |
| Legacy `KnowledgeRecord`/`InMemoryKnowledgeStore` reconciliation | A future domain-governance decision, not this document |
| Identity Service durability | A future, separately-sequenced Contract Design pass under ADR-024 Rule 13 |
| World Model durability | Permanently excluded — not deferred, excluded (ADR-024 Rule 14) |
| Conversation History durability | Left open by ADR-024 itself; not scheduled to any specific future unit |
| The constitutional Audit log (ADR-024 Rule 17) | A separate, broader durability obligation spanning every subsystem, not Memory Core's own narrower write log |
| Storage mechanism selection (append-only-log-plus-replay vs. SQLite vs. any other candidate) | The Memory Core Durability Implementation Plan |
| Schema/wire-format concrete representation | The Memory Core Durability Implementation Plan |
| Docker volume name, mount path, and `ParkerRuntimeConfig` field names | The Memory Core Durability Implementation Plan |
| Backup and replication policy | No future Programme currently named; excluded until a concrete requirement is identified |
| Storage optimisation, indexing, query performance work | No future Programme currently named; excluded until a concrete requirement is identified |
| Cross-process concurrent access to the durable store | No future Programme currently named; excluded until a concrete deployment requirement is identified |

---

## 17. Risks

- **Risk: a future Implementation Plan selects a storage mechanism whose own natural failure modes do not map cleanly onto Sections 8/9's mechanism-neutral vocabulary** (for example, a transactional engine with no "partial write" concept distinct from an all-or-nothing committed/rolled-back transaction). **Mitigation:** Sections 8 and 9 above are already stated in mechanism-neutral, property-level terms — exactly the correction the Durability Contract Design's own Independent Constitutional Review required and its Defect Confirmation Review confirmed applied — so any mechanism capable of satisfying the underlying properties satisfies this Scope Lock, regardless of its own native vocabulary.
- **Risk: an Implementation Plan quietly reintroduces double permission gating**, wrapping the durable implementation in both self-gating callers' own internal checks and a newly-added `PermissionGatedMemoryCore` composition. **Mitigation:** Section 14, above, fixes the abstract "no double permission gating" requirement as binding. As a disclosed, purely informational finding (not itself frozen scope, since concrete composition wiring is Implementation-Plan-tier work): `src/composition/ParkerRuntime.kt`, as it exists today, constructs `InMemoryMemoryCore()` exactly once, exposes it as a raw `MemoryCore` reference directly to `EvidenceRegistrationCoordinator` and to Programme 4's own coordinator (each already gates its own Memory Core writes internally, per the composition's own existing inline reasoning), and separately wraps the same instance in exactly one shared `PermissionFilteredMemoryRetrieval` for every `MemoryRetrieval` consumer; `PermissionGatedMemoryCore` exists in this repository (`src/composition/PermissionGatedMemoryCore.kt`) but is not composed into the current graph. A future Implementation Plan replacing `InMemoryMemoryCore()`'s construction with a durable implementation should verify this description still matches the composition graph at the time it is written, and preserve the same "exactly once, same decorator boundary, no new redundant gate" shape this finding describes — without treating this paragraph itself as fixing the specific variable names or line numbers as permanent scope.
- **Risk: recovery ordering is implemented per-kind rather than as a true total order**, silently breaking `findByTimeRange`'s own existing cross-kind tiebreak on replay without any test catching it, since a per-kind-correct implementation could pass every single-kind test. **Mitigation:** Section 15's verification scope requires the full existing regression suite (which already exercises `findByTimeRange`'s cross-kind ordering) to pass unchanged; an Implementation Plan should additionally be required to demonstrate total-order preservation directly, not merely indirectly through pre-existing tests.
- **Risk: "Implementation-Plan-tier engineering discretion" for mechanism selection is read as licence to skip justifying the choice.** **Mitigation:** Section 6, above, states explicitly that mechanism selection remains subject to this repository's own established "no new runtime dependency without a documented, verified justification" bar; an Implementation Plan selecting SQLite or any other embedded engine must supply that justification, not merely assert convenience.
- **Risk: a durability implementation is composed into `ParkerRuntime.kt` in a way that changes Docker or storage-path handling silently, outside this Scope Lock's own review**, since this task's own governing instruction forbade modifying Docker or `ParkerRuntime` during this planning cycle, meaning no implementation evidence yet exists to check against. **Mitigation:** Section 3's Mandatory Deliverables and Section 15's Verification Scope both require an explicit Docker-volume-backed restart test before completion; this is disclosed here as a residual risk this Scope Lock can fix requirements for but cannot itself verify, since verification requires the very implementation this document precedes.

---

## 18. Acceptance Criteria

- A Memory Core durability implementation **SHALL** durably persist every `Provenance`, `Entity`, `Document`, `Assertion`, and `Relationship` record, exactly as their own current field shapes already define them.
- A Memory Core durability implementation **SHALL** durably persist every lifecycle status transition as a discrete, appended fact — never merely the current status value.
- A Memory Core durability implementation **SHALL** restore each per-kind identifier counter to (highest persisted identifier of that kind) + 1 on recovery, and **SHALL NOT** permit identifier reuse across a restart.
- A Memory Core durability implementation **SHALL** validate referential integrity during recovery and **SHALL** fail recovery visibly, never silently, on a broken reference.
- A Memory Core durability implementation **SHALL** treat an incomplete write as discardable and **SHALL** treat a malformed, already-committed record as unrecoverable corruption — the two **SHALL NOT** be conflated.
- A Memory Core durability implementation **SHALL NOT** silently present a store that exists but cannot be validly recovered as an empty, fresh store.
- A Memory Core durability implementation **SHALL** tag every durably stored record with an explicit schema version and **SHALL** fail recovery, never silently interpret, an unrecognised future version.
- A Memory Core durability implementation **SHALL NOT** add, remove, or alter any method, parameter, or type on `src/interfaces/MemoryCore.kt`'s or `MemoryRetrieval`'s own public shape.
- A Memory Core durability implementation **SHALL NOT** hold, construct, or reference `PermissionEngine`, Knowledge Memory, or `EvidenceCustodian` anywhere within the durable storage layer.
- A Memory Core durability implementation **SHALL NOT** expose a raw file handle, path, or storage-specific identifier through `MemoryCore` or `MemoryRetrieval`.
- A Memory Core durability implementation **SHALL** be constructed exactly once within `ParkerRuntime.kt`'s own composition graph, **SHALL** be reachable by every existing consumer through the same decorator boundary that consumer already uses today, and **SHALL NOT** introduce double permission gating on any write path already gated internally by its own caller.
- A Memory Core durability implementation **SHALL** preserve total write order across all five record kinds, sufficient for `findByTimeRange`'s own existing cross-kind tiebreak to replay faithfully.
- A Memory Core durability implementation **SHALL NOT** permit any write path to become reachable while recovery remains incomplete or failed, and **SHALL** surface a failed recovery through the existing `RuntimeLifecycleState.FAILED` state.
- A Memory Core durability implementation **SHALL NOT** report a write as failed if it was durably committed, and **SHALL NOT** report a write as succeeded if it was not durably committed.
- A Memory Core durability implementation **SHALL** select no storage technology, schema representation, or Kotlin implementation type in this document — that selection **SHALL** occur only in the Memory Core Durability Implementation Plan, subject to this repository's own existing new-dependency justification bar.

---

## 19. Recommendation

Every mandatory deliverable (Section 3) traces directly to a requirement the Durability Contract Design already fixed in full; every exclusion (Section 4) traces to a reason already established across the four frozen documents, not invented here; the mechanism boundary, atomicity, recovery, corruption, versioning, immutability, concurrency, failure-semantics, and runtime-boundary rules (Sections 6 through 14) are restatements of already-frozen decisions, cross-checked in Section 14 against the actual current state of `src/composition/ParkerRuntime.kt`, not merely against precedent. This document introduces no new architecture — it only fixes, without ambiguity, the boundary a Memory Core Durability Implementation Plan must build inside.

```
READY FOR IMPLEMENTATION PLAN
```
