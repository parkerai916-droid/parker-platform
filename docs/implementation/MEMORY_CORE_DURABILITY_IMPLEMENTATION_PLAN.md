# Memory Core Durability — Implementation Plan

## Status

**Implementation planning only. No Kotlin is implemented, proposed as a diff, or changed by this document.** Neither `src/` nor `tests/` nor `Dockerfile` nor `docker-compose.yml` is touched. Nothing is staged, committed, or pushed.

**Governing, frozen inputs — not reopened, redesigned, or reinterpreted by this document:** ADR-024; `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` as amended by Errata 001–004; `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`; `docs/architecture/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN.md` ("the Contract Design"); `docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md` ("the Scope Lock").

**Governance sufficiency, confirmed before drafting — see Section 2, below, for the full determination.** The Contract Design's two Required Corrections and the Scope Lock's one Required Correction are each confirmed applied, by `docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md` and `docs/reviews/MEMORY_CORE_DURABILITY_SCOPE_LOCK_DEFECT_CONFIRMATION_REVIEW.md` respectively. No implementation-critical matter was found unresolved by either document; this plan proceeds on that basis.

---

## 1. Executive Summary

This plan translates the Scope Lock's own fixed properties into ten discrete, ordered, independently completable implementation units, plus the one mechanism selection the Scope Lock and Contract Design both explicitly reserve for this tier (Section 4, below): a filesystem-backed, single, totally-ordered append-only durability log, replayed at startup through `InMemoryMemoryCore`'s own existing, unmodified read-and-validation logic via a new, thin decorator (`DurableMemoryCore`), extending `FileSystemEvidenceArtifactStorage`'s atomic-write discipline and `FileSystemEvidenceDeletionAudit`'s append-then-force discipline. `InMemoryMemoryCore.kt` itself is not rewritten; it gains a small, disclosed, `internal`-only extension (Section 5, below) that no external caller can reach and that leaves its existing public contract, and every one of its existing tests, unaffected. No storage technology beyond the local filesystem is added; no new runtime dependency is introduced. Runtime composition and Docker volume mapping are the last two units, exactly as the Scope Lock's own tier discipline requires.

---

## 2. Governance Sufficiency Determination

Verified directly, not assumed, before any unit below was drafted:

- **The Contract Design's Required Corrections.** Both are confirmed present in the current committed text by `docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md`, itself checked word-for-word against the current text, not against a prior grep alone.
- **The Scope Lock's Required Correction.** Confirmed applied by `docs/reviews/MEMORY_CORE_DURABILITY_SCOPE_LOCK_DEFECT_CONFIRMATION_REVIEW.md`, correcting a tier-boundary defect (concrete `ParkerRuntime.kt` composition facts frozen as Scope-Lock-tier binding text) found by the Scope Lock's own genuine Independent Constitutional Review.
- **No implementation-critical matter was found unresolved by either document.** Every requirement Sections 6 through 15, below, build against traces to an already-fixed Contract Design or Scope Lock clause, cited by section number throughout. Where a genuine choice remains open (the durability mechanism itself), both governing documents explicitly, and consistently, name this Implementation Plan as the correct tier to resolve it (Contract Design §5: "Implementation-Plan/Unit level, not in Evidence Custodian's own Contract Design"; Scope Lock §6: "Implementation-Plan-tier engineering discretion") — this is not a gap in governance; it is governance correctly deferring a decision to the tier built to make it, exactly as `FileSystemEvidenceArtifactStorage`'s own precedent already established for a sibling subsystem.
- **No stop condition is triggered.** This plan did not encounter a requirement it could not trace to already-accepted authority, and did not need to invent policy to proceed. Where a specific stop condition exists for an individual unit below (should implementation itself later discover a gap this planning pass could not have found), it is stated in that unit's own "Stop Condition" field.

**Determination: governance is sufficient. This plan proceeds.**

---

## 3. Current Repository Assessment

- **`src/runtime/InMemoryMemoryCore.kt`** (579 lines): one `Mutex`; five `mutableMapOf` stores; five independent `Long` sequence counters starting at 1; six write operations, each performing exactly one map insertion or one status mutation under the single mutex; referential-integrity checks (`requireExistingProvenance`, `requireResolvableEndpoint`) enforced only at creation time; `findByTimeRange`'s own fixed-kind-order-then-insertion-order tiebreak; the `internal` (not `private`) `MemoryCoreLifecycleTransitions` closed transition table — already `internal` specifically so code outside its own file, in the same module, can exercise it directly, the same visibility precedent Section 5, below, extends.
- **`src/interfaces/MemoryCore.kt`** (1217 lines): `MemoryCore` and `MemoryRetrieval`, each already frozen by Errata 004 with a leading `requestingPrincipalId` parameter on every method; five record types, five candidate types, two sealed wrapper types (`MemoryCoreRecord`, `MemoryCoreRecordReference`). No change to this file is proposed by any unit below.
- **`src/runtime/FileSystemEvidenceArtifactStorage.kt`** and **`src/runtime/FileSystemEvidenceDeletionAudit.kt`**: the two extended precedents. The former's temp-file/force/atomic-move write discipline and construction-time storage-root validation; the latter's append-then-`FileChannel.force` discipline, tab-separated fixed-field-order line format, and construction-time parent-directory validation. Neither exposes a query/replay capability — Memory Core durability needs one neither precedent had reason to build, disclosed as a deliberate, justified departure in Section 6 (Unit 2), below.
- **`src/composition/ParkerRuntime.kt`**: constructs `InMemoryMemoryCore()` exactly once today, exposes it as a raw `MemoryCore` reference to `EvidenceRegistrationCoordinator` and a Programme 4 coordinator (each self-gates its own writes), and wraps it in exactly one shared `PermissionFilteredMemoryRetrieval`. `PermissionGatedMemoryCore` exists in the repository but is not composed into the current graph.
- **`src/composition/ParkerRuntimeConfig.kt`**: the exact precedent this plan's own new configuration field (Section 6, Unit 8) mirrors — `evidenceStorageRootPath: String`, sourced via `KEY_EVIDENCE_STORAGE_ROOT` and `requireKey(environment, ...)`, no invented default.
- **`Dockerfile`** / **`docker-compose.yml`**: two existing named volumes (`evidence-storage`, `evidence-audit`), each declared in `docker-compose.yml`, each mounted to a fixed in-container path, each created and `chown`ed to the non-root `parker` user in `Dockerfile` before `USER parker`.
- **No prior durability implementation exists anywhere in this repository for any subsystem** beyond Evidence Custodian's own artifact storage and deletion audit — this plan is Memory Core's first.

---

## 4. Mechanism Selection

**This section makes the one mechanism decision both the Contract Design (§5) and the Scope Lock (§6) explicitly reserve for Implementation-Plan tier. It is a decision, not a further deferral — a future unit may not reopen it without a revision to this plan.**

**Selected: a filesystem-backed, single, totally-ordered append-only durability log, replayed through `InMemoryMemoryCore`'s own existing logic via a new decorator, `DurableMemoryCore`.**

**Reasoning, weighed against the two candidates the prior planning review already compared:**

- **No new runtime dependency.** This repository's own established bar (Apache Tika: added once, with a documented, verified justification) is not met by SQLite today — no concrete requirement has been demonstrated that the filesystem-plus-replay direction cannot satisfy safely. Per Section 13, below (Explicit Non-Responsibilities, Unit-crossing), SQLite adoption remains available to a *future* plan revision if implementation evidence, not speculation, later proves this direction insufficient — but no such evidence exists today, and none is invented here.
- **Reuses `InMemoryMemoryCore`'s own already-accepted, already-tested logic entirely unchanged for every read and for every validation rule**, rather than reimplementing referential-integrity checks, lifecycle-transition validation, or retrieval ordering a second time against a different storage substrate. This structurally eliminates an entire class of defect (the durable and in-memory implementations disagreeing about what is valid) that a from-scratch SQL schema would have to re-earn through its own, separate test suite.
- **Total write order falls out for free.** `findByTimeRange`'s own cross-kind tiebreak (Scope Lock §12) requires a single total order across all five record kinds. A *single* log file, written in true call order, satisfies this trivially, by construction — no separate global sequence needs to be reconstructed from five per-kind logs or five per-kind tables.
- **Extends, rather than replaces, two already-reviewed precedents.** `FileSystemEvidenceArtifactStorage`'s atomic-write discipline and `FileSystemEvidenceDeletionAudit`'s append-then-force discipline are both already-accepted, already-tested patterns this repository's own governance has twice endorsed; this decision is the third application of the same discipline, not a new one.
- **Structurally cannot exceed Scope Lock §10's "structural criteria only" retrieval limit**, since replay only ever re-exercises `InMemoryMemoryCore`'s own already-constrained operations — a property a SQL-backed implementation would have to establish separately, by discipline rather than by structure, since SQL's own `ORDER BY`/`WHERE` vocabulary makes it easy to accidentally add a ranking or scoring capability Memory Core's own contract forbids.

**What this selection does not do.** It does not foreclose a future SQLite adoption, does not claim the filesystem direction is superior in the abstract, and does not relieve Unit 3 (Section 6, below) of the obligation to design and test a correct, round-trip-safe encoding for every field this Programme's five record types actually carry, including arbitrary caller-supplied free text.

---

## 5. Design Resolution — Identifier Restoration Without Public-Contract Reuse

**This is the single most load-bearing design decision this plan makes, and it is stated explicitly here because getting it wrong would silently violate Scope Lock Objective 5 (stable identifiers, never reused) without any obviously failing test.**

`InMemoryMemoryCore`'s five `create*` operations each mint a new identifier internally (`"provenance-${nextProvenanceSequence++}"`, and so on) — deliberately, per Errata 002: an identifier is "never accepted from a caller." **This means replay cannot call `createEntity`, `createProvenance`, `registerDocument`, `createAssertion`, or `createRelationship` again during recovery** — doing so would mint a *new* identifier for what is actually a *restoration* of an existing one, immediately violating identifier stability across a restart.

**`transitionStatus` has no equivalent problem and is replayed via its existing public method, unchanged.** It mints no identifier; it looks up an existing record by an already-known reference and applies an already-validated transition. Replaying it during recovery, in original order, against an already-restored record, reproduces the exact same resulting state the original call produced — no new method is needed for lifecycle-transition replay.

**Required, disclosed, `internal`-only extension to `InMemoryMemoryCore.kt`:** five new `internal` (not `public`, not reachable through the `MemoryCore` interface, and not part of Errata 004's frozen public shape) restoration functions — one per creatable record kind (`internal suspend fun restoreProvenance(provenance: Provenance)`, and correspondingly for `Entity`, `Document`, `Assertion`, `Relationship`) — each performing exactly the same referential-integrity validation its public `create*` counterpart already performs (Contract Design §6's own recovery requirement: "referential-integrity validation is required during recovery, not optional"), but accepting the record's own already-known identifier directly rather than minting one, and not itself advancing the corresponding sequence counter (Unit 5, below, restores each counter once, after all restoration calls for that kind complete, to `(highest restored identifier of that kind) + 1` — simpler and less error-prone than incrementing counters one call at a time during a replay loop). This mirrors the exact precedent already set by `MemoryCoreLifecycleTransitions` being kept `internal`, not `private`, specifically so code in the same module — there, a test file; here, `DurableMemoryCore` — can reach it directly without it ever becoming part of the public `MemoryCore` contract. **No existing public method signature on `InMemoryMemoryCore` changes. No existing test is affected. `InMemoryMemoryCoreTest.kt`'s own existing assertions about `create*`'s identifier-minting behaviour remain true and untouched** — only new tests, exercising the five new `internal` functions directly (as `InMemoryMemoryCoreTest.kt` already does for `MemoryCoreLifecycleTransitions`), are added.

---

## 6. Implementation Units

### Unit 1 — Durable Record Format Boundary

- **Purpose.** Define the internal, versioned, on-disk representation of a durable act — one entry per `InMemoryMemoryCore` write operation — sufficient to reconstruct the exact record or transition that produced it.
- **Governing authority.** Contract Design §3 (durable record scope, no field added/removed/renamed), §8 (explicit schema-version tag, additive/defaulted evolution, unknown-version rejection); Scope Lock §5, §10.
- **Inputs.** The five record types and `MemoryCoreRecordStatus`/`MemoryCoreRecordReference` shapes, exactly as `src/interfaces/MemoryCore.kt` currently defines them; no field is added, removed, or renamed.
- **Outputs.** A sealed `DurableMemoryCoreEntry` type with six cases — one per write operation kind (`ProvenanceCreated`, `EntityCreated`, `DocumentRegistered`, `AssertionCreated`, `RelationshipCreated`, `StatusTransitioned`) — each carrying an explicit `schemaVersion: Int` field and the full field payload its corresponding record or transition needs; a deterministic, round-trip-safe line-oriented text encoding for each case, extending `FileSystemEvidenceDeletionAudit`'s own tab-separated `key=value` convention with an explicit escaping rule for embedded tab/newline/backslash characters in free-text fields (`Entity.primaryLabel`, `Assertion.statement`, and any other caller-supplied string field) — a genuine, disclosed departure from the deletion-audit precedent's own "no field value may itself contain a tab or newline" assumption, required because Memory Core's own record types, unlike a deletion-audit entry, carry arbitrary caller-supplied text.
- **Production files expected to change.** New: `src/runtime/DurableMemoryCoreEntry.kt` (the sealed type, its encode/decode functions, the escaping scheme).
- **Test files expected to change.** New: `tests/runtime/DurableMemoryCoreEntryTest.kt` — round-trip encode/decode for every one of the six cases, including adversarial free-text inputs (embedded tabs, newlines, backslashes, empty strings, Unicode) and an explicit "unrecognised future schema version fails deterministically, never silently" test.
- **Dependencies.** None beyond `kotlinx-coroutines-core` (already present) and the JDK standard library. No new runtime dependency.
- **Explicit non-responsibilities.** Does not implement file I/O (Unit 2). Does not implement replay ordering or recovery logic (Unit 4). Does not change any field on any existing record type.
- **Completion criteria.** Every one of the six entry cases round-trips exactly (encode then decode reproduces an identical value, including every nested `List`/`Map` field) under property-style and adversarial-input tests; an entry carrying an unrecognised `schemaVersion` fails decoding with a specific, disclosed exception rather than a generic parse error.
- **Stop condition.** If a round-trip-safe encoding cannot be constructed without a new runtime dependency for some field this Programme's record types actually carry, stop and request a Contract Design or Scope Lock revision naming the specific field and the dependency required — do not silently add a dependency without the justification bar Section 4, above, already names.

### Unit 2 — Durable Storage Interface

- **Purpose.** The internal-only persistence seam: append an entry durably; read back every previously-appended entry, in order, at startup.
- **Governing authority.** Contract Design §12 (no filesystem authority escapes through `MemoryCore`/`MemoryRetrieval`); Scope Lock §14 (durable storage strictly below `MemoryCore`).
- **Inputs.** `DurableMemoryCoreEntry` (Unit 1); a single file `Path`, supplied by the caller at construction, never guessed or defaulted, mirroring `FileSystemEvidenceDeletionAudit`'s own construction-time discipline exactly.
- **Outputs.** A single concrete class, `FileSystemMemoryCoreDurabilityLog` — deliberately not split into an interface plus one implementation the way `EvidenceArtifactStorage`/`FileSystemEvidenceArtifactStorage` are: no second implementation need has been identified anywhere in the four governing documents, and introducing an unused abstraction now would itself violate the Scope Lock's own "no structure without a concrete need" discipline. Two operations: `suspend fun append(entry: DurableMemoryCoreEntry)` (open in append mode, write one encoded line, `FileChannel.force`, mirroring `FileSystemEvidenceDeletionAudit.record` exactly) and `suspend fun readAll(): List<DurableMemoryCoreEntry>` (read the file line by line, decode each line, in file order — the one capability neither existing precedent needed, disclosed as a deliberate, required departure, not an oversight). Construction-time validation of the parent directory (exists, is a directory, is writable) mirrors `FileSystemEvidenceDeletionAudit`'s own `init` block exactly; the log file itself is created empty if absent, preserved unchanged if a prior process's own log already exists.
- **Production files expected to change.** New: `src/runtime/FileSystemMemoryCoreDurabilityLog.kt`.
- **Test files expected to change.** New: `tests/runtime/FileSystemMemoryCoreDurabilityLogTest.kt` — construction-time validation (missing/non-writable/non-directory parent, mirroring `FileSystemEvidenceDeletionAuditTest.kt`'s own existing cases); append-then-`readAll` round-trip for a sequence of mixed entry kinds; `readAll` against an empty, freshly-created log returns an empty list; `readAll` preserves file order exactly.
- **Dependencies.** `DurableMemoryCoreEntry` (Unit 1). No dependency on `InMemoryMemoryCore`, `PermissionEngine`, Knowledge Memory, or `EvidenceCustodian`.
- **Explicit non-responsibilities.** Does not validate an entry's own referential integrity (Unit 4's responsibility, applied during replay, not during storage). Does not detect or classify corruption beyond a line failing to decode (Unit 4 classifies partial-write-vs-corruption; this Unit only reports a decode failure as such). Does not know about `InMemoryMemoryCore`, `MemoryCore`, or `MemoryRetrieval` at all — it operates purely on `DurableMemoryCoreEntry` values.
- **Completion criteria.** Every test above passes; `FileSystemMemoryCoreDurabilityLog` has no import from `src/interfaces/MemoryCore.kt` beyond `DurableMemoryCoreEntry`'s own dependency on the record types it encodes (Unit 1), and no import from `src/composition/`.
- **Stop condition.** None identified — this Unit's scope is fully determined by Units 1 and the two already-reviewed filesystem precedents.

### Unit 3 — Append and Atomicity Behaviour

- **Purpose.** Guarantee that each of `InMemoryMemoryCore`'s six write operations durably commits as exactly one atomic act, with the caller-observable failure semantics Contract Design §11 fixes.
- **Governing authority.** Contract Design §4 (one operation, one atomic durable act; no invented multi-record transaction), §11 (four distinguished failure moments; binding truthfulness promise).
- **Inputs.** `FileSystemMemoryCoreDurabilityLog` (Unit 2); `InMemoryMemoryCore` (unmodified for this Unit's own purposes — its six public write operations, called after a successful durable append).
- **Outputs.** The write-path half of `DurableMemoryCore` (the other half, read delegation, needs no new logic and is completed in this same Unit for cohesion, mirroring `InMemoryMemoryCore`'s own Implementation Plan precedent of not splitting one cohesively-correct class across two units): for each of the six `MemoryCore` operations, `DurableMemoryCore` first constructs and durably appends the corresponding `DurableMemoryCoreEntry` (Unit 1) via `FileSystemMemoryCoreDurabilityLog.append` (Unit 2) — a genuine durability fault at this step propagates as a thrown exception, never silently swallowed, never returned as a completed record — and only then invokes the corresponding `InMemoryMemoryCore` operation to update in-memory state and obtain the value returned to the caller. A crash between these two steps leaves a durably-committed fact whose in-memory reflection did not complete before the caller could be told: exactly the "acceptable" window Contract Design §11 already names, recoverable on the next successful startup (Unit 4). Every `MemoryRetrieval` method on `DurableMemoryCore` delegates directly, unchanged, to the wrapped `InMemoryMemoryCore` instance — no new logic, since Version 1's own read behaviour is already correct and already tested.
- **Production files expected to change.** New: `src/runtime/DurableMemoryCore.kt` (write-path and read-delegation only for this Unit; recovery logic is Unit 4).
- **Test files expected to change.** New: `tests/runtime/DurableMemoryCoreTest.kt` — each of the six write operations durably appends before the in-memory store reflects the change (verified via a test double `FileSystemMemoryCoreDurabilityLog` or a real one plus direct file inspection); a durability-log failure (for example, a non-writable log file) causes the operation to throw and leaves `InMemoryMemoryCore`'s own state unchanged; every `MemoryRetrieval` method returns exactly what the wrapped `InMemoryMemoryCore` instance would return directly, for identical calls.
- **Dependencies.** Units 1, 2; `InMemoryMemoryCore` (existing, unmodified for this Unit).
- **Explicit non-responsibilities.** Does not implement startup recovery/replay (Unit 4). Does not restore identifiers or lifecycle history from a prior run (Units 5, 6). Does not perform any permission check (none of `DurableMemoryCore`'s own code holds a `PermissionEngine` reference, mirroring `InMemoryMemoryCore`'s own identical omission).
- **Completion criteria.** All tests above pass; `DurableMemoryCore` implements `MemoryCore` and `MemoryRetrieval` with no additional public method beyond what those two interfaces already require (no new public surface area, per Contract Design §12).
- **Stop condition.** If a durability fault cannot be distinguished from an ordinary validation failure (for example, `IllegalArgumentException` from a candidate's own `init` block) using the existing exception hierarchy alone, stop and request a Contract Design revision naming the specific new exception type required — do not silently reuse an existing exception type for a semantically different failure without disclosure.

### Unit 4 — Replay and Startup Recovery

- **Purpose.** Reconstruct `InMemoryMemoryCore`'s own complete state — every record, every lifecycle transition, in original order — from a durability log written by a prior process, through governed internal pathways, never by direct field manipulation.
- **Governing authority.** Contract Design §6 (deterministic replay; Provenance-first ordering; referential-integrity validation; repeated-record idempotence; partial-write discard; no silent empty-store fallback); Scope Lock §8.
- **Inputs.** `FileSystemMemoryCoreDurabilityLog.readAll()` (Unit 2); a fresh `InMemoryMemoryCore` instance; the five new `internal restore*` functions (Section 5, above).
- **Outputs.** `DurableMemoryCore.recover(): RecoveryResult` (or an equivalent suspend function invoked once, before the instance is handed to Runtime composition — Unit 8 fixes exactly where in `ParkerRuntime.start()` this call occurs): reads every entry via `readAll()`, in file order (already the true total write order, by construction of Unit 2's single-log design); for each `*Created`/`*Registered` entry, calls the corresponding `internal restore*` function (Section 5), which re-runs the same referential-integrity check its public `create*` counterpart already performs — a broken `provenanceId` or a broken Memory-Core-owned `Relationship` endpoint on replay throws, exactly as Contract Design §6 requires, never silently accepted; for each `StatusTransitioned` entry, calls the existing public `transitionStatus` unchanged. A decode failure on the *last* entry in the file (Unit 1's own encode/decode contract makes this distinguishable from a decode failure on any earlier entry) is treated as an interrupted, never-completed write — discarded, recovery proceeds with every entry before it. A decode failure on any entry that is *not* the last is genuine corruption — Contract Design §7's "malformed, already-durably-committed record... unrecoverable" — recovery fails. A referential-integrity failure during a `restore*` call is likewise genuine corruption, never discarded regardless of position, since (unlike a decode failure) it can only occur once an entry has already been fully, successfully decoded. A `readAll()` returning zero entries against a log file that exists (freshly created, never yet written to) is a legitimate empty store, not a failure — the same "fresh start" case Unit 10's own first verification test exercises. **No case in this Unit's own logic silently begins as though a genuinely present but invalid durable state had never existed** — every failure path above is a thrown, specifically-named exception, never a caught-and-ignored fallback to an empty `InMemoryMemoryCore`.
- **Production files expected to change.** `src/runtime/DurableMemoryCore.kt` (extended with recovery logic); `src/runtime/InMemoryMemoryCore.kt` (the five `internal restore*` functions, Section 5, above).
- **Test files expected to change.** `tests/runtime/DurableMemoryCoreTest.kt` (extended); `tests/runtime/InMemoryMemoryCoreTest.kt` (extended, exercising the five new `internal restore*` functions directly, mirroring its own existing direct exercise of `MemoryCoreLifecycleTransitions`).
- **Dependencies.** Units 1, 2, 3; Section 5's design resolution.
- **Explicit non-responsibilities.** Does not restore identifier counters (Unit 5 — a separate, explicit step after all `restore*` calls complete). Does not surface failure through `RuntimeLifecycleState` directly (Unit 8 — this Unit throws; Runtime composition catches and transitions state). Does not implement any storage-efficiency or compaction mechanism.
- **Completion criteria.** Fresh-start recovery against an empty log reproduces empty `InMemoryMemoryCore` state; write-then-restart-then-read round-trips correctly for all five record kinds and for a lifecycle transition; a broken reference injected into a durability log fails recovery with a specific exception; a truncated final entry is discarded silently (recovery succeeds using every entry before it) while a truncated *non-final* entry fails recovery; a durability log containing the same entry twice (simulating an at-least-once retry) recovers correctly with no duplicate record.
- **Stop condition.** If a corruption case is discovered during implementation that cannot be classified as either "partial final write" or "genuine corruption" using position-in-file alone, stop and request a Contract Design revision — do not invent a third classification silently.

### Unit 5 — Identifier Restoration

- **Purpose.** Restore each of the five per-kind identifier counters to (highest restored identifier of that kind) + 1, exactly once, after Unit 4's own replay completes.
- **Governing authority.** Contract Design §3, §6 ("either by direct persistence of the counter or by deterministic derivation from the highest persisted identifier... is a mechanism choice"); Scope Lock §5, §8, Objective 5.
- **Inputs.** The fully-restored `InMemoryMemoryCore` instance from Unit 4 (every record already present, each carrying its own original identifier).
- **Outputs.** A narrow, disclosed `internal` extension to `InMemoryMemoryCore` — one function per kind (or one function taking a kind-and-value pair five times) directly setting `nextProvenanceSequence`, `nextEntitySequence`, and so on, to `(maximum numeric suffix among restored identifiers of that kind) + 1`, or to `1` if no record of that kind was restored (the correct value for a genuinely fresh store, consistent with the existing, unmodified starting value). Derivation, not separate persistence, is selected — the counter value is fully computable from the already-restored records themselves, so no additional field needs to be added to `DurableMemoryCoreEntry` (Unit 1) to carry it separately, avoiding a second, redundant source of truth that could itself drift out of sync with the records it counts.
- **Production files expected to change.** `src/runtime/InMemoryMemoryCore.kt` (the counter-restoration `internal` function(s), added alongside the five `restore*` functions from Section 5/Unit 4).
- **Test files expected to change.** `tests/runtime/InMemoryMemoryCoreTest.kt` (extended): after restoring records with non-contiguous or out-of-order-looking identifiers (still each unique, still each internally consistent with this repository's own `"kind-N"` minting format), the next `create*` call for that kind mints exactly (previous maximum) + 1, never a collision, never a gap-driven reuse of an already-used value.
- **Dependencies.** Unit 4.
- **Explicit non-responsibilities.** Does not persist a counter value directly as its own durable fact — deliberately, per the reasoning above. Does not change the minting format (`"kind-N"`) itself.
- **Completion criteria.** After a full recover-then-create cycle in a test, the newly created record's identifier is provably greater than every restored identifier of the same kind, and no collision occurs across repeated restart-then-create cycles in the same test.
- **Stop condition.** None identified — this Unit's own scope is fully determined by Unit 4's output and the existing, unmodified minting format.

### Unit 6 — Lifecycle Recovery

- **Purpose.** Confirm that a record's complete durable transition history, not merely its final current status, survives a restart — and that write-once record identity and lawful status transitions are preserved throughout.
- **Governing authority.** Contract Design §9 (immutability and transitions; replay reproduces same current state *and* same history); Scope Lock §11.
- **Inputs.** `StatusTransitioned` entries (Unit 1), replayed via the existing public `transitionStatus` (Section 5, above — no new method needed).
- **Outputs.** No new production code beyond what Units 1 through 4 already provide — `transitionStatus`'s own existing `MemoryCoreLifecycleTransitions.requireValidTransition` check already re-validates every replayed transition against the same closed transition table a live call would, so an invalid transition sequence recorded by corruption or a defect elsewhere could not silently replay into an invalid final state; it would throw, correctly surfacing as recovery failure (Unit 4). This Unit's own deliverable is verification, not new production surface.
- **Production files expected to change.** None beyond Unit 4's own listed changes.
- **Test files expected to change.** `tests/runtime/DurableMemoryCoreTest.kt` (extended): a record created, then transitioned through two or more lawful status changes (for example `ACTIVE → DISPUTED → ARCHIVED`), survives a restart with both its final `ARCHIVED` status and (verified indirectly, since `MemoryCore`'s own public contract exposes only current status, never a transition-history query — Contract Design §9's "same... history" guarantee is about durable *storage*, not a new public retrieval mode) the fact that replaying the same durability log twice in a row from a fresh `InMemoryMemoryCore` produces bit-for-bit identical final state both times, proving the replay is deterministic and the full sequence, not merely the final value, was genuinely replayed rather than collapsed.
- **Dependencies.** Units 1, 3, 4.
- **Explicit non-responsibilities.** Does not add a new public method exposing transition history directly — no such method exists on `MemoryRetrieval` today (Contract Design §9's own already-accepted "audit by traversing Relationships and reading transition events" model), and none is authorised by anything this plan builds against.
- **Completion criteria.** The two-restart-determinism test above passes; an invalid transition sequence artificially injected into a durability log (for a test only, never producible by a correctly-functioning `DurableMemoryCore`) fails recovery via the existing `IllegalStateException` path, not silently.
- **Stop condition.** None identified.

### Unit 7 — Concurrency and Ordering

- **Purpose.** Preserve `InMemoryMemoryCore`'s own existing single-writer-at-a-time discipline across the addition of a durable append step, with no observable interleaving between a durable commit and its corresponding in-memory update.
- **Governing authority.** Contract Design §10; Scope Lock §12.
- **Inputs.** `InMemoryMemoryCore`'s own existing `Mutex`; `FileSystemMemoryCoreDurabilityLog`'s own append operation (Unit 2), itself already single-writer-serialised via its own internal guard, mirroring `FileSystemEvidenceDeletionAudit`'s own identical pattern.
- **Outputs.** `DurableMemoryCore`'s own write path (Unit 3) performs the durable append and the corresponding `InMemoryMemoryCore` call as one serialised unit from a concurrent caller's own perspective — achieved by a `DurableMemoryCore`-level `Mutex` guarding the append-then-delegate sequence as a whole, distinct from (and outer to) `InMemoryMemoryCore`'s own internal `Mutex` and `FileSystemMemoryCoreDurabilityLog`'s own internal guard, so that two concurrent callers can never observe one call's durable commit interleaved with a different call's in-memory update. This is stated behaviourally, not by naming `Mutex` as a requirement (Scope Lock §12 forbids naming a concrete mechanism as the fixed requirement) — `Mutex` is this Unit's own chosen means of satisfying the already-fixed behavioural guarantee, freely replaceable by an equally correct alternative without a governance revision, exactly as the Scope Lock's own corrected wording already anticipates.
- **Production files expected to change.** `src/runtime/DurableMemoryCore.kt` (the outer serialisation guard, added in the same pass as Unit 3's own write-path logic — listed as a separate Unit here only because the Scope Lock's own Section 12 names it as a distinct required property to verify, not because it is separable production code).
- **Test files expected to change.** `tests/runtime/DurableMemoryCoreTest.kt` (extended): two concurrent write calls against the same `DurableMemoryCore` instance never produce an interleaved durability-log entry (verified by inspecting the log file's own resulting content for well-formed, non-interleaved lines) and never leave `InMemoryMemoryCore`'s own in-memory state observably ahead of or behind the durability log's own content.
- **Dependencies.** Units 2, 3.
- **Explicit non-responsibilities.** Does not introduce or guarantee cross-process concurrency (Contract Design §10 explicitly inherits, not closes, this limitation). Does not change `InMemoryMemoryCore`'s own internal `Mutex` in any way.
- **Completion criteria.** The concurrent-write test above passes reliably (no flaking) under this repository's own existing coroutine test-execution conventions.
- **Stop condition.** None identified.

### Unit 8 — Runtime Composition

- **Purpose.** Construct the durable implementation exactly once within `ParkerRuntime.kt`'s own composition graph, reachable by every existing consumer through the same decorator boundary it already uses today, with no double permission gating.
- **Governing authority.** Contract Design §12; Scope Lock §14, Section 18 Acceptance Criteria.
- **Inputs.** `DurableMemoryCore` (Units 1–7, complete); `ParkerRuntimeConfig`'s own existing `evidenceStorageRootPath` field as the direct structural precedent for the new configuration field this Unit adds.
- **Outputs.** In `ParkerRuntimeConfig.kt`: a new required field, `memoryCoreDurabilityLogPath: String`, sourced via a new `KEY_MEMORY_CORE_DURABILITY_LOG_PATH` constant and `requireKey(environment, KEY_MEMORY_CORE_DURABILITY_LOG_PATH)` — no invented default, mirroring `evidenceStorageRootPath`'s own identical pattern exactly. In `ParkerRuntime.kt`: the existing line `val inMemoryMemoryCore = InMemoryMemoryCore()` is replaced by construction of `FileSystemMemoryCoreDurabilityLog` (the new configuration field's path, resolved to a `Path`) and `DurableMemoryCore` wrapping a fresh `InMemoryMemoryCore()`; a call to `DurableMemoryCore.recover()` (Unit 4) occurs during `ParkerRuntime.start()`, before the lifecycle state may reach `RUNNING` — sequenced as one of the construction steps `start()` cannot pass without reaching `FAILED` first (mirroring `DependencyConstructionFailed`'s own existing "naming the step that failed" pattern) — never during a synchronous constructor, since recovery is a suspend operation and every existing precedent for a load-before-`RUNNING` step in this repository already uses `start()`, not construction, for exactly this reason. Every existing reference to `inMemoryMemoryCore` (the raw `MemoryCore` exposure to `EvidenceRegistrationCoordinator` and the Programme 4 coordinator; the single shared `PermissionFilteredMemoryRetrieval` wrapping) is updated to reference the new `durableMemoryCore` value instead, preserving the exact same decorator shape and the exact same "no `PermissionGatedMemoryCore` wrapper, since each caller already self-gates" reasoning the current composition already discloses inline — no new `PermissionGatedMemoryCore` composition is introduced.
- **Production files expected to change.** `src/composition/ParkerRuntimeConfig.kt`; `src/composition/ParkerRuntime.kt`.
- **Test files expected to change.** New or extended composition test file (mirroring `tests/composition/ParkerRuntimeKnowledgeRetrievalCompositionTest.kt`'s own established shape): `durableMemoryCore` is constructed exactly once; the same instance is reachable, unwrapped, from both self-gating callers; the same instance is wrapped by exactly one `PermissionFilteredMemoryRetrieval`; `ParkerRuntime.start()` does not reach `RUNNING` if `DurableMemoryCore.recover()` throws, and does reach `FAILED` instead; a full regression run of the existing `EvidenceRegistrationCoordinator`/Programme 4 composition tests still passes unchanged against the new composed instance.
- **Dependencies.** Units 1–7, complete and independently tested.
- **Explicit non-responsibilities.** Does not add a `PermissionGatedMemoryCore` wrapper. Does not change any method signature on `MemoryCore`/`MemoryRetrieval`. Does not touch Docker (Unit 9).
- **Completion criteria.** The full existing repository regression suite passes unchanged in count; the new composition tests above pass; `git diff -- src/composition/ParkerRuntime.kt` shows no line altered or reordered beyond the one construction-site replacement and the reference updates it necessitates.
- **Stop condition.** If preserving "no double permission gating" while composing the durable implementation is found, during implementation, to require a change to `EvidenceRegistrationCoordinator`'s or the Programme 4 coordinator's own internal gating logic, stop and request a Scope Lock revision — this plan authorises no change to either coordinator's own existing permission-gating behaviour.

### Unit 9 — Container Durability

- **Purpose.** Map the durable storage location to a Docker named volume, and distinguish what each of container restart, VM restart, Proxmox/host restart, and full power-loss recovery actually requires and actually provides.
- **Governing authority.** Contract Design §13 (Docker volume declarations deferred to Implementation Plan); the existing `evidence-storage`/`evidence-audit` named-volume precedent in `docker-compose.yml` and `Dockerfile`.
- **Inputs.** `ParkerRuntimeConfig.memoryCoreDurabilityLogPath` (Unit 8); the existing two-named-volume pattern.
- **Outputs.** In `docker-compose.yml`: a new named volume, `memory-core-durability`, mounted at a fixed in-container path (for example `/data/memory-core`), declared in the `volumes:` top-level block alongside the existing two, with a new environment variable (`PARKER_MEMORY_CORE_DURABILITY_LOG_PATH`, pointing at a file inside that mount, e.g. `/data/memory-core/durability.log`) supplied to the `parker` service, mirroring `PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH`'s own identical shape (a file path inside a directory-mounted volume, not the directory itself). In `Dockerfile`: the new mount-point directory is created and `chown`ed to `parker` in the same `RUN mkdir -p ... && chown -R parker:parker /opt/parker /data` line the existing two directories already share, requiring only that the new path be added to the existing `mkdir -p` argument list — no new `RUN` step.
  - **Container restart** (the `parker` container stops and starts again, host and volume unchanged): the named volume's own content survives natively via Docker's own volume persistence; `DurableMemoryCore.recover()` (Unit 4) reconstructs state from it on the next `start()`. This is the primary case this plan's own tests (Unit 10) exercise directly.
  - **VM restart / Proxmox or host restart** (the host the container runs on restarts, `docker compose up` or an equivalent restart policy brings the container back): identical to container restart from Memory Core's own point of view, *provided* the named volume's own backing storage (a Docker-managed directory on the host filesystem, by default) survives the host restart — which it does, by Docker's own standard behaviour, without this plan needing to add anything further; `restart: unless-stopped` (already present in `docker-compose.yml`) already governs whether the container itself restarts automatically after a host reboot, orthogonally to durability.
  - **Full power-loss recovery** (the host loses power ungracefully, mid-write): the same recovery path as container restart, with one added, disclosed distinction — an ungraceful power loss can leave the *last* durability-log entry genuinely incomplete (the OS-level write or `fsync` that would have completed it never happened), which is exactly the "partial, not-yet-complete write" case Unit 4 already discards, not a new case this Unit needs to handle separately. This plan does not claim, and does not need, any guarantee stronger than what `FileChannel.force`'s own documented `fsync`-equivalent behaviour already provides for a completed write.
- **Production files expected to change.** `docker-compose.yml`; `Dockerfile`.
- **Test files expected to change.** None directly (Docker configuration has no Kotlin test file); verified instead by Unit 10's own Docker-restart verification step.
- **Dependencies.** Unit 8.
- **Explicit non-responsibilities.** Does not select or configure any backup or replication policy for the named volume — none is governed anywhere in the four frozen documents, and none is invented here (Scope Lock §4). Does not address multi-host or multi-container deployment — no such deployment exists in `docker-compose.yml` today.
- **Completion criteria.** `docker compose config` (or equivalent static validation) accepts the modified `docker-compose.yml` without error; the new volume, mount, and environment variable follow the existing two-volume pattern with no structural deviation; `Dockerfile`'s own single `chown` step continues to cover every mounted path, including the new one.
- **Stop condition.** None identified — this Unit's own scope is fully determined by the existing two-volume precedent and Unit 8's own configuration field.

### Unit 10 — Verification

- **Purpose.** Demonstrate, not merely assert, that every property Sections 6 (Units 1–9) above fix actually holds, before this Programme's own durability work is considered complete.
- **Governing authority.** Contract Design §15; Scope Lock §15, Section 18 Acceptance Criteria.
- **Inputs.** Every production file listed across Units 1–9, complete.
- **Outputs.** The following tests, each independently named and independently passing (several already listed under their own originating Unit above; consolidated here as the complete required verification surface, per the Scope Lock's own Section 15):
  1. **Fresh-start test.** No prior durability log exists; `DurableMemoryCore.recover()` produces the same empty state `InMemoryMemoryCore()` alone already produces.
  2. **Write/restart/read tests, all five record kinds.** Each record kind, created, survives an instance-level restart (new `DurableMemoryCore` wrapping a fresh `InMemoryMemoryCore`, same durability log) with every field intact.
  3. **Lifecycle-transition restart test.** A multi-step transition sequence survives restart with the correct final status (Unit 6).
  4. **Referential-integrity recovery test.** A broken `provenanceId` or relationship endpoint, injected into a durability log for test purposes, fails recovery visibly (Unit 4).
  5. **Identifier max+1 tests, all five kinds.** Post-recovery creation never collides with a restored identifier (Unit 5).
  6. **Repeated-record idempotence test.** A durability log containing the same entry twice recovers to the same state as containing it once (Unit 4).
  7. **Incomplete-terminal-data recovery test.** A truncated final entry is discarded; recovery succeeds using every entry before it (Unit 4).
  8. **Non-terminal corruption failure test.** A truncated or malformed *non-final* entry fails recovery visibly (Unit 4).
  9. **Failed recovery leaves writes unreachable test.** After a recovery failure, no write made it into a readable, servable state through the failed instance — confirmed by asserting the failed `DurableMemoryCore` never reaches a state from which any `MemoryRetrieval` call could be made (Unit 8's own `start()`-gating ensures no caller ever holds such an instance in practice; this test confirms the underlying object-level guarantee directly).
  10. **Full runtime reconstruction test.** `ParkerRuntime.start()`, exercised end-to-end against a pre-populated durability log, reaches `RUNNING` with fully restored Memory Core state, and every existing consumer (`EvidenceRegistrationCoordinator`, the Programme 4 coordinator, `PermissionFilteredMemoryRetrieval`) observes the restored data through its own existing call path (Unit 8).
  11. **Docker-volume restart test.** A container-level restart (`docker compose restart parker`, or an equivalent build-and-restart cycle run manually against the modified `Dockerfile`/`docker-compose.yml`) demonstrates durable survival end-to-end, verifying Unit 9's own container-restart case directly, not merely by static configuration inspection.
  12. **Full repository regression suite.** `./gradlew clean test` passes with the same or greater test count as the pre-durability baseline, zero new failures, zero reduction in existing coverage.
- **Production files expected to change.** None — this Unit is test-only.
- **Test files expected to change.** Every test file listed under Units 1–9, above, collected and confirmed complete against this Unit's own twelve-item list; any gap found between what a prior Unit's own "Test files expected to change" already covers and this list is closed here, not deferred further.
- **Dependencies.** Units 1–9, complete.
- **Explicit non-responsibilities.** Does not add new production behaviour of any kind. Does not test Knowledge Memory, Identity, World Model, Conversation History, or the constitutional Audit log's own durability — each remains out of scope (Section 15, below).
- **Completion criteria.** All twelve items pass. This is the completion criterion for the Memory Core Durability Programme as a whole, not merely for this Unit.
- **Stop condition.** If any of the twelve items cannot be made to pass without a change to a governing document's own already-fixed requirement, stop and request the specific revision needed, naming which item and which requirement — do not weaken the test to make it pass.

---

## 7. Dependency Graph

| Unit | Prerequisites | Outputs | Files touched |
| --- | --- | --- | --- |
| 1 | None | `DurableMemoryCoreEntry` | New: `src/runtime/DurableMemoryCoreEntry.kt` |
| 2 | 1 | `FileSystemMemoryCoreDurabilityLog` | New: `src/runtime/FileSystemMemoryCoreDurabilityLog.kt` |
| 3 | 1, 2 | `DurableMemoryCore` (write path, read delegation) | New: `src/runtime/DurableMemoryCore.kt` |
| 4 | 1, 2, 3 | `DurableMemoryCore.recover()`; `InMemoryMemoryCore` internal `restore*` | `src/runtime/DurableMemoryCore.kt` extended; `src/runtime/InMemoryMemoryCore.kt` extended |
| 5 | 4 | Counter restoration | `src/runtime/InMemoryMemoryCore.kt` further extended |
| 6 | 1, 3, 4 | Lifecycle-history verification (no new production code) | None beyond Unit 4 |
| 7 | 2, 3 | Outer serialisation guard | `src/runtime/DurableMemoryCore.kt` further extended |
| 8 | 1–7 | Runtime composition | `src/composition/ParkerRuntimeConfig.kt`, `src/composition/ParkerRuntime.kt` |
| 9 | 8 | Docker volume mapping | `Dockerfile`, `docker-compose.yml` |
| 10 | 1–9 | Full verification | Test files only, across all prior units |

---

## 8. Repository Impact

- **New files:** `src/runtime/DurableMemoryCoreEntry.kt`, `src/runtime/FileSystemMemoryCoreDurabilityLog.kt`, `src/runtime/DurableMemoryCore.kt`, plus the corresponding four new test files (`tests/runtime/DurableMemoryCoreEntryTest.kt`, `tests/runtime/FileSystemMemoryCoreDurabilityLogTest.kt`, `tests/runtime/DurableMemoryCoreTest.kt`, a new composition test file under `tests/composition/`).
- **Modified files:** `src/runtime/InMemoryMemoryCore.kt` (five `internal restore*` functions plus counter-restoration functions — additive only; no existing method signature changes); `tests/runtime/InMemoryMemoryCoreTest.kt` (additive only); `src/composition/ParkerRuntimeConfig.kt` (one new required field); `src/composition/ParkerRuntime.kt` (one construction-site replacement plus reference updates, additive-pattern-following per Unit 8); `Dockerfile`, `docker-compose.yml` (one new volume/mount/env-var, following the existing two-volume pattern exactly).
- **No new package.** Every new file lives in `parker.core.runtime`, the same package `InMemoryMemoryCore`, `FileSystemEvidenceArtifactStorage`, and `FileSystemEvidenceDeletionAudit` already occupy.
- **No new runtime dependency.** `build.gradle.kts` is not touched by any unit above.
- **No change to `src/interfaces/MemoryCore.kt`.** Confirmed by every unit's own "explicit non-responsibilities" field, above — no unit lists this file among its production changes.
- **No unnecessary movement of existing code.** Nothing above relocates, renames, or restructures any file that exists today.

---

## 9. Failure Recovery

Consolidated from each unit's own failure-handling logic, above, into one end-to-end picture:

- **A durability fault during a live write** (Unit 3) propagates as a thrown exception before the caller is told anything; `InMemoryMemoryCore`'s own state is left unchanged.
- **A crash between a durable commit and its in-memory reflection** (Unit 3) is recoverable on the next successful startup (Unit 4) — never lost, never double-counted.
- **A genuinely corrupted, already-committed record** (Units 4, 6) fails recovery visibly, surfaced through `RuntimeLifecycleState.FAILED` (Unit 8) — no partial repair, no silent skip.
- **An incomplete final write** (Unit 4) is discarded, recovery proceeds using every entry before it.
- **A recovery failure of any kind** (Unit 4) leaves no write reachable through the failed instance (Unit 10, item 9) and prevents `ParkerRuntime.start()` from reaching `RUNNING` (Unit 8) — no write path becomes reachable while recovery remains incomplete or failed, exactly as every other production entry point's existing `state == RuntimeLifecycleState.RUNNING` gate already enforces today.

---

## 10. Acceptance Sequence

1. Units 1 and 2 implemented and independently tested (pure encoding and pure file I/O, no dependency on each other's own correctness beyond the interface between them).
2. Unit 3 implemented and tested against a real `FileSystemMemoryCoreDurabilityLog` and a real `InMemoryMemoryCore`.
3. Units 4 and 5 implemented together (replay and counter restoration are tightly coupled, per Unit 5's own "after all restoration calls complete" dependency) and tested.
4. Unit 6 verified (no new production code; verification only) against the completed Unit 4.
5. Unit 7 implemented and tested; full `DurableMemoryCore` now feature-complete in isolation.
6. Unit 8 implemented; full existing regression suite re-run to confirm zero regression in every other composed subsystem.
7. Unit 9 implemented; Docker configuration validated statically.
8. Unit 10 executed in full, including the Docker-volume restart test, which requires Units 8 and 9 both complete.
9. Documentation reconciliation (`IMPLEMENTATION_HISTORY.md`, `IMPLEMENTATION_GAPS.md` gap #51 closure entry) — a later, separate phase of this repository's own established Development Method, not a deliverable of this plan itself, mirroring exactly how the Version 1 Memory Core Implementation Plan (§6) treats the same phase.

---

## 11. Completion Criteria

The Memory Core Durability Programme is complete when, and only when:

- Every one of Units 1 through 10's own individual completion criteria, above, is met.
- The full repository regression suite passes with zero reduction in count.
- `docs/architecture/IMPLEMENTATION_GAPS.md` gap #51 can be closed, citing this plan's own completed units as the resolution.
- No item in Section 12 (Deferred Work Register), below, has been silently implemented beyond what this plan actually authorises.

---

## 12. Deferred Work Register

Every item below remains explicitly out of scope for this Programme, mirroring the Scope Lock's own Section 16 register:

| Deferred capability | Reason |
| --- | --- |
| Knowledge Memory durability | A separate Programme, its own Scope Lock and Contract Design. |
| Legacy `KnowledgeRecord`/`InMemoryKnowledgeStore` reconciliation | A domain-governance decision, not addressed by this plan. |
| Identity Service durability | ADR-024 Rule 13's sibling obligation, sequenced separately. |
| World Model durability | Permanently excluded, not deferred. |
| Conversation History durability | An ADR-024-unresolved open question this plan does not resolve. |
| The constitutional Audit log (ADR-024 Rule 17) | A separate, broader obligation spanning every subsystem. |
| Backup and replication policy | Not governed anywhere in the four frozen documents; not invented here. |
| Storage optimisation, indexing, query performance work | No concrete need identified. |
| Cross-process concurrent access | No concrete deployment requirement identified. |
| SQLite, or any other embedded database, adoption | Section 4, above, selects the filesystem-plus-replay mechanism on the evidence available today; a future plan revision may revisit this only with implementation evidence, not speculation, that the mechanism-neutral requirements Sections 6 through 15 of the Scope Lock fix cannot be met safely otherwise. |

---

## 13. Risks

- **Risk: the escaping scheme Unit 1 designs for free-text fields contains an undiscovered edge case** (an input that does not round-trip correctly). **Mitigation:** Unit 1's own completion criteria require adversarial-input round-trip tests, not merely happy-path ones, before this Unit is considered complete.
- **Risk: the `internal restore*` functions added to `InMemoryMemoryCore.kt` (Section 5; Units 4, 5) drift out of sync with their public `create*` counterparts over time**, if a future, unrelated change to a `create*` method's own validation logic is not mirrored into its `restore*` counterpart. **Mitigation:** disclosed here explicitly as a maintenance obligation for any future change to `InMemoryMemoryCore.kt`'s own creation logic, not solved structurally by this plan — a future refactor consolidating the shared validation logic between each `create*`/`restore*` pair is a reasonable follow-up but is not required for this Programme's own completion criteria and is not designed here without a concrete defect motivating it.
- **Risk: a future, unrelated `ParkerRuntime.kt` refactor changes composition variable names or introduces a legitimate second `PermissionFilteredMemoryRetrieval` consumer**, and Unit 8's own tests, if written too literally against today's specific names, would then fail for a reason unrelated to durability. **Mitigation:** Unit 8's own tests should assert the *behavioural* properties (exactly one construction; reachable through the existing decorator boundary; no double gating) rather than asserting specific variable names, exactly mirroring the Scope Lock's own corrected Section 14 wording (Section 2, above).
- **Risk: the single-log-file design (Section 4) grows without bound over a long-lived deployment**, since no compaction or snapshot mechanism is included. **Mitigation:** explicitly accepted for this Programme — Contract Design §9 permits, but does not require, a future storage-efficiency mechanism, and no concrete requirement for one has been identified yet; startup cost growing with total historical write volume is a disclosed, known trade-off of the selected mechanism (Section 4), not a defect.
- **Risk: the Docker-volume restart test (Unit 10, item 11) is the one verification item this plan cannot itself execute**, since it requires an actual running Docker environment this planning cycle's own instructions forbid touching. **Mitigation:** disclosed directly; this is a required, not optional, item for Programme completion, to be executed once Units 8 and 9 are actually implemented, not skipped or downgraded to "recommended."

---

## 14. Self-Review

Performed before this document was presented as complete, checking each item the governing task required:

- **No architectural decision was invented.** Every requirement in Sections 6 through 13 traces to a specific Contract Design or Scope Lock section, cited throughout. The one genuine decision this plan makes on its own authority — mechanism selection (Section 4) — is a decision both governing documents explicitly name this tier as the correct place to make, not an invention of new authority.
- **Mechanism neutrality was preserved** at the Contract Design and Scope Lock tiers — neither document is edited by this plan, and this plan's own mechanism selection does not retroactively narrow either document's own text, which remains, correctly, mechanism-neutral for any future revision to reconsider.
- **Every unit traces to accepted authority** — confirmed above and within each unit's own "Governing authority" field.
- **Memory Core Version 1 remains unchanged.** No unit modifies any field on any existing record type, any method signature on `MemoryCore` or `MemoryRetrieval`, or any existing public behaviour of `InMemoryMemoryCore`'s own `create*`/`transitionStatus`/read operations. The five new `internal restore*` functions are additive and unreachable through any public interface.
- **No Knowledge Memory implementation leaked into the plan.** No unit references `KnowledgeItem`, `KnowledgeStore`, `KnowledgeRecord`, or any Knowledge-Memory-owned type; Section 12 names Knowledge Memory durability explicitly as deferred.
- **Runtime and Docker composition are last.** Units 8 and 9 are the ninth and tenth units in dependency order, following seven units of independently-testable production and test surface.
- **Recovery failure cannot silently start an empty writable store.** Unit 4's own logic distinguishes a genuinely empty fresh store (zero entries, log file present but never written to) from a recovery failure (any decode or referential-integrity failure not on the final entry); Unit 8 gates `RUNNING` on `recover()`'s own success; Unit 10 item 9 tests this directly.
- **No production code or test was changed.** This document is the only file this task cycle's drafting of it touched; confirmed in Section 15, below.

---

## 15. Recommendation

Every mandatory unit traces directly to a requirement the Contract Design or the Scope Lock already fixed in full; the one mechanism decision this plan makes (Section 4) is made at the tier both governing documents name as correct for it, with reasoning grounded in this repository's own established precedent and dependency-justification bar. This plan introduces no new architecture beyond what implementing an already-fixed boundary requires — it sequences, files, and tests that boundary into ten discrete, independently completable units.

```
READY FOR IMPLEMENTATION
```

---

## Final Verification

```
$ wc -l docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md
357 docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md
```

```
$ grep '^## ' docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md
## Status
## 1. Executive Summary
## 2. Governance Sufficiency Determination
## 3. Current Repository Assessment
## 4. Mechanism Selection
## 5. Design Resolution — Identifier Restoration Without Public-Contract Reuse
## 6. Implementation Units
## 7. Dependency Graph
## 8. Repository Impact
## 9. Failure Recovery
## 10. Acceptance Sequence
## 11. Completion Criteria
## 12. Deferred Work Register
## 13. Risks
## 14. Self-Review
## 15. Recommendation
## Final Verification
```

```
$ git status --short
?? docs/architecture/MEMORY_CORE_DURABILITY_SCOPE_LOCK.md
?? docs/implementation/MEMORY_CORE_DURABILITY_IMPLEMENTATION_PLAN.md
?? docs/reviews/MEMORY_CORE_DURABILITY_CONTRACT_DESIGN_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_SCOPE_LOCK_DEFECT_CONFIRMATION_REVIEW.md
?? docs/reviews/MEMORY_CORE_DURABILITY_SCOPE_LOCK_INDEPENDENT_CONSTITUTIONAL_REVIEW.md
```

Nothing staged, committed, or pushed.
