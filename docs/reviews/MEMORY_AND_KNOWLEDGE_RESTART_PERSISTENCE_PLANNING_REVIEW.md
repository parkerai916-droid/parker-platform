# Memory Core and Knowledge Memory — Durable, Restart-Safe Persistence Planning Review

## Status

**Planning review only. No Kotlin implemented, proposed as a diff, or changed by this document. No governance document amended.** This document inventories the current state and identifies what a future Contract Design/Scope Lock/Implementation Plan revision would need to address — it does not draft that revision, does not select a storage technology, and does not authorise implementation to begin. Nothing is staged, committed, or pushed.

---

## 1. Repository Baseline

- **HEAD:** `0db4b737d3112b9f74762c0fc79cefeeaf905ae9` (short `0db4b73`) — "docs: record Memory Core write gateway decision."
- **Branch:** `main`.
- **Working tree:** clean.

No discrepancy found.

---

## 2. Authorities Reviewed

- `docs/architecture/MEMORY_CORE_SCOPE_LOCK.md` — §4 (Explicit Exclusions), §14 (Acceptance Criteria), §15 (Out-of-Scope Register), §16 (Risks) read in full/re-confirmed; the governing text explicitly anticipates this exact review (§16, Risk 1, quoted below).
- `docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md` — §15 (Runtime Responsibilities), record-shape sections previously read in full this session.
- `docs/implementation/MEMORY_CORE_IMPLEMENTATION_PLAN.md` — §11 (Failure Recovery — a different sense of "recovery" than this review's own concern; see §10, below), §15 (Risks), §17 (Unit 10 Acceptance Tracking).
- `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_SCOPE_LOCK.md` and `docs/governance/PROGRAMME_3_KNOWLEDGE_MEMORY_CONTRACT_DESIGN_V2.md` — persistence-exclusion clauses read directly.
- `docs/implementation/PROGRAMME_3_KNOWLEDGE_MEMORY_IMPLEMENTATION_PLAN.md` — including its own post-hoc "Tracking Note" recording that promoted `KnowledgeItem`s are "durable but unexposed" today (a usage of "durable" this review deliberately does not inherit — see §5.4).
- `docs/reviews/MEMORY_CORE_WRITE_GATEWAY_ARCHITECTURAL_DECISION_REVIEW.md` — this session's own immediately prior review, re-confirming the current, exact composition-time shape of `ParkerRuntime.kt`.
- **`docs/adr/ADR-024-module-event-audit-durability-boundary.md`** (Status: Accepted) — read in full. This is the single most directly relevant authority found: an already-accepted ADR whose own Section D ("Audit and Durability Boundary," Rules 13–17) settles, in advance of this review, exactly which stores must eventually be durable, which may remain in-memory, and the precise, correct reading of "durable" until a persistence layer exists. Quoted extensively in §11, below.
- **`docs/architecture/IMPLEMENTATION_GAPS.md`, item #51** ("Persistence / durability / audit boundary is not structurally defined") — the existing, open, tracked gap this exact review addresses. Status at time of writing: "Open, logged for tracking. Strategic architecture gap — not a defect in the current in-memory reference implementation... No action required to close now; requires an ADR before persistence or durable audit becomes load-bearing."
- `docs/reviews/ARCHITECTURE_V2_INDEPENDENT_AUDIT_TRIAGE.md`, Finding 4 — cited by Gap #51 as the origin of its own reasoning; not separately re-litigated here.
- `docs/architecture/EVIDENCE_ARTIFACT_STORAGE_MAPPING.md` — the existing governance note for `FileSystemEvidenceArtifactStorage`'s own identifier-to-filename mapping, the closest existing precedent for what a Memory Core/Knowledge Memory equivalent design note would look like.
- `docs/architecture/50-deployment-architecture.md` and `docs/architecture/48-safe-mode-and-recovery.md` — checked and found materially stale (the former describes an original Android-device deployment target with "local databases," not the actual Docker/JVM deployment this repository now has; the latter describes an aspirational Safe Mode concept with no cross-reference to `ParkerRuntime`'s own actual lifecycle states). Neither governs this review's own subject matter; noted so a future reader does not mistake either for current, binding deployment guidance.
- `src/runtime/InMemoryMemoryCore.kt`, `src/runtime/KnowledgeItemPersistence.kt`, `src/interfaces/KnowledgeStore.kt` (record shapes) — read in full.
- `src/runtime/FileSystemEvidenceArtifactStorage.kt`, `src/runtime/FileSystemEvidenceDeletionAudit.kt` — read in full, as this repository's own only two existing durable-storage precedents.
- `src/composition/ParkerRuntimeConfig.kt`, `Dockerfile`, `docker-compose.yml` — read in full.
- Every `InMemory*` class construction site confirmed directly against `src/composition/ParkerRuntime.kt` (1319 lines, read in full this session).

---

## 3. Scope and Objective

This review inventories what already exists and what a future durability design would need to address for **Memory Core** (`Entity`, `Document`, `Assertion`, `Relationship`, `Provenance`) and **Knowledge Memory** (both the legacy `KnowledgeRecord` store and the Programme 3 V2 `KnowledgeItem` persistence seam). Every other in-memory production store is inventoried too, for context and comparison, but this review's own transactional/recovery/schema/Docker analysis is scoped to the two named subsystems, per the task's own framing.

**Governance already anticipates this review, in terms that make it required follow-up, not speculative enhancement.** Scope Lock §16, Risk 1, quoted in full because it is the direct authority for this review's own existence:

> "Risk: Version 1's in-memory-only storage undermines Memory Core's own claim to be a 'system of record.' A record that does not survive a process restart is a weaker guarantee than 'system of record' implies, especially for the evidence-intelligence use case that motivated this Programme... Mitigation: the implementation's own documentation must state this limitation explicitly, not by omission... persistence is named in the Out-of-Scope Register above as a **required follow-up** before Memory Core is relied upon for anything genuinely durable, not an optional enhancement."

Scope Lock §15's own Out-of-Scope Register lists "Persistent / durable storage (relational, object/file, graph)," deferred to "a future, separately-justified storage design, once a concrete durability requirement is identified" — this review is the first step toward identifying that requirement, not the design itself.

**This is not a novel concern this review is the first to raise.** `IMPLEMENTATION_GAPS.md` #51 already tracks it, repository-wide, as an open strategic gap, and ADR-024 (Accepted) already settles the boundary question in principle — this review's own contribution is the concrete inventory ADR-024 itself did not attempt (it "implements no persistence or audit storage... each [gap] remains open, with its implementation still to be authorised by a future Contract Design pass").

---

## 4. Complete In-Memory Production Store Inventory

Every class in `src/runtime/` whose name begins `InMemory`, confirmed against its actual construction site (or absence of one) in `ParkerRuntime.kt`:

| Store | Interface(s) | Internal state | Live in `ParkerRuntime.kt`? | Lost on restart? |
| --- | --- | --- | --- | --- |
| `InMemoryMemoryCore` | `MemoryCore`, `MemoryRetrieval` | One `Mutex`; five `mutableMapOf` stores (`provenanceStore`, `entityStore`, `documentStore`, `assertionStore`, `relationshipStore`); five monotonic `Long` sequence counters | Yes (line 604) | **Yes — in scope for this review** |
| `InMemoryKnowledgeStore` | `KnowledgeStore`, `KnowledgeSource` | One `Mutex`; `mutableMapOf<KnowledgeId, KnowledgeRecord>`; a separate forgotten-identifier audit set | Yes (line 358, as `memorySource: KnowledgeSource` feeding the Reasoning Context Assembler) | **Yes — in scope for this review** (the legacy Knowledge Memory store) |
| `InMemoryKnowledgeItemPersistence` | `KnowledgeItemPersistence` (`internal`) | One `Mutex`; `mutableMapOf<KnowledgeId, KnowledgeItem>` | Yes (line 742, backing `DefaultKnowledgeSubmission`) | **Yes — in scope for this review** (the Programme 3 V2 promoted-item store) |
| `InMemoryConversationEngine` | `ConversationEngine`, `ConversationHistorySource` | Conversation turns and continuity state (not re-examined in depth here — out of this review's own named scope) | Yes (line 348) | Yes, but out of scope |
| `InMemoryWorldModel` | `WorldModel`, `WorldModelSource` | Current beliefs, confidence-scored | Yes (line 372) | Yes, but out of scope (and constitutionally excluded from ever depending on Memory Core in either direction — Reconciliation §10) |
| `InMemoryEventBus` | `EventBus` | In-process subscriber list; no event history retained | Yes (line 339) | N/A — this store never held anything meant to outlive a single delivery; nothing to persist |
| `InMemoryIdentityService` | `IdentityService` | Registered Principals | Yes (line 340) | Reconstructed fresh at every startup from `registerSystemIdentities`/config (`ownerPrincipalId`) — not user data, not in scope |
| `InMemoryResourceRegistry` | `ResourceRegistry` | Registered Resources | Yes (line 333) | Reconstructed fresh at every startup from fixed, hardcoded registration calls — not in scope |
| `InMemoryToolRegistry` | `ToolRegistry` | Registered Tools | Yes (line 336) | Reconstructed fresh at every startup — not in scope |
| `InMemoryModuleRegistry` | `ModuleRegistry` | Registered/enabled Modules | Yes (line 337) | Reconstructed fresh at every startup — not in scope |
| `InMemoryActionVocabulary` | `ActionVocabulary` | Registered `ActionVocabularyEntry`s | Yes (line 334) | Reconstructed fresh at every startup — not in scope |
| `InMemoryToolInvocationBinding` | `ToolInvocationBinding` | Bound Tool implementations | Yes (line 338) | Reconstructed fresh at every startup — not in scope |
| `InMemoryAgentRuntime` | `AgentRunCommandChannel`, `AgentRunExecutionTrigger` | Agent Run state | Yes (line 673) | Yes, but out of scope for this review (a distinct future durability question — Agent Run history) |
| `InMemoryTaskManagerRuntime` | (Task Manager's own interface) | Task state | Yes (line 696) | Yes, but out of scope |
| `InMemoryPlannerRuntime` | (Planner's own interface) | Planning session state | Yes (line 697) | Yes, but out of scope |

Two further classes hold a `MemoryRetrieval` dependency but are **not constructed anywhere in `ParkerRuntime.kt`** — `EvidenceExtractionCoordinator` and `DefaultKnowledgeRevisionEvaluator` — confirmed dormant, consistent with `PermissionGatedMemoryCore`'s own already-documented dormant state (this session's immediately prior review). Their existence does not change this review's own inventory, since they hold no state of their own to persist; they are readers, not stores.

Two further **stores** exist but are dormant/test-only, confirmed not constructed anywhere in `ParkerRuntime.kt`:

- **`InMemoryEvidenceArtifactStorage`** (`EvidenceArtifactStorage`) — an in-memory alternative to `FileSystemEvidenceArtifactStorage`, referenced only from tests. Notable precisely because it shows this repository already has direct experience replacing an in-memory implementation with a durable one for exactly this interface — the nearest possible precedent for what a Memory Core equivalent migration would look like in shape, even though the interface itself (single-blob, write-once) is much simpler than Memory Core's own.
- **`InMemoryDerivativeReviewRegistry`** (`DerivativeReviewRegistry`) — holds derivative-review history per evidence artifact (`mutableMapOf<EvidenceArtifactId, MutableList<DerivativeReviewRecord>>`), referenced only from its own test file. No durable counterpart exists for this interface at all today, unlike `EvidenceArtifactStorage`'s own dual in-memory/filesystem pair.

**The clean dividing line for this review's own scope:** registries/vocabularies/bindings (`IdentityService`, `ResourceRegistry`, `ToolRegistry`, `ModuleRegistry`, `ActionVocabulary`, `ToolInvocationBinding`) are all **rebuilt identically, deterministically, from code and configuration at every startup** — losing them on restart is not data loss, since nothing user- or world-derived was ever in them. Memory Core, Knowledge Memory, Conversation History, World Model, and Agent/Task/Planner runtime state are all **genuinely user- or world-derived** — losing them on restart is real, irreversible data loss. This review addresses only the first two of that second group (Memory Core, Knowledge Memory), exactly as scoped.

---

## 5. Records That Must Survive Restart

### 5.1 Memory Core

Five record kinds, each keyed by its own identifier value class, each carrying a mandatory `MemoryCoreRecordStatus`:

| Record | Identifier | Mandatory fields | Notable nullable/optional fields |
| --- | --- | --- | --- |
| `Provenance` | `ProvenanceId` | `sourceIdentifier`, `sourceType`, `acquisitionTime`, `ingestionTime`, `contentNature` | `creator`, `creatorPrincipalId`, `claimedCreationTime`, `derivedFrom` (list), `extractedFrom`, `processingHistory` (list), `integrityInformation`, `confidence`, `sensitivity` |
| `Entity` | `EntityId` | `entityType`, `primaryLabel`, `provenanceId`, `createdAt` | `aliases` (list, additive-only), `relatedPrincipalId`, `metadata` (map) |
| `Document` | `DocumentId` | `documentType`, `locationReference`, `provenanceId`, `registeredAt` | `integrityHash`, `processingStatus`, `metadata` (map) |
| `Assertion` | `AssertionId` | `statement`, `provenanceId` | `confidence`, `metadata` (map) |
| `Relationship` | `RelationshipId` | `relationshipType`, `fromEndpoint`, `toEndpoint`, `directional`, `provenanceId`, `createdAt` | — |

Every record additionally carries `MemoryCoreRecordStatus` (`ACTIVE`/`DISPUTED`/`SUPERSEDED`/`ARCHIVED`/`DELETED`), mutated only via `transitionStatus` (Scope Lock §8's own frozen lifecycle) — a durable store must persist status transitions as they occur, not only initial creation, since a `DELETED` record is never removed from the store (Scope Lock §7: correction is always a new, linked record; nothing is ever physically erased except through the distinct, owner-only Evidence Custodian deletion path, which is unrelated to Memory Core's own status field).

**Identifier minting is sequential and per-kind** (`"provenance-1"`, `"entity-1"`, ...), from an in-process `Long` counter starting at 1. A durable design must resume each counter from the highest previously-persisted value of its own kind, not restart at 1 — restarting at 1 would either collide with an already-persisted identifier or silently orphan the counter's own uniqueness guarantee, depending on load order.

### 5.2 Knowledge Memory (legacy) — `InMemoryKnowledgeStore`

`KnowledgeRecord`: `memoryId` (`KnowledgeId`), `category`, `sourceSubsystem`, `correlationId`, `promotedAt`, `knowledgePayload`, plus optional `originatingPrincipalId`, `confidence`, `sensitive` (boolean), `relatedMemoryIds` (list), `history` (list of strings). This store additionally retains a **separate, content-free "forgotten identifier" audit set** — every `KnowledgeId` ever forgotten, so a durable design must persist this set too, distinctly from the live records, to preserve the "never recycled" identifier guarantee across a restart.

### 5.3 Knowledge Memory (Programme 3 V2) — `InMemoryKnowledgeItemPersistence`

`KnowledgeItem`: `knowledgeId` (`KnowledgeId`), `evidenceReference` (`MemoryCoreRecordReference` — a reference *into* Memory Core, meaning Knowledge Memory's own durable store has a cross-store referential dependency any recovery design must sequence correctly, loading Memory Core before validating or trusting these references), `provenanceReference`, `evidentialState`, `status` (`KnowledgeItemStatus`, defaulted `ACTIVE`), `history` (list of `KnowledgeLifecycleEvent`).

### 5.4 A terminology note this review deliberately corrects

The Programme 3 Implementation Plan's own post-hoc tracking note states promoted `KnowledgeItem`s are "today, durable but unexposed." **This review uses "durable" strictly to mean "survives process restart."** In that note's own context, "durable" means only "held indefinitely in an in-process map for the life of the running process" — which is exactly the gap this review exists to close. No `KnowledgeItem`, and no Memory Core record, is durable in this review's own sense today; all are lost the instant the JVM process ends.

### 5.5 Two parallel, currently un-reconciled Knowledge stores

**Observed, not resolved here:** `InMemoryKnowledgeStore` (legacy, feeds `ReasoningContextAssembler`) and `InMemoryKnowledgeItemPersistence` (Programme 3 V2, backs `DefaultKnowledgeSubmission`) are two structurally distinct, both-live, both-in-memory stores holding two structurally distinct record types (`KnowledgeRecord` vs. `KnowledgeItem`), with no retrieval path from the second back into Reasoning Context yet (the Programme 3 Implementation Plan's own Unit 9, "Knowledge Query/Result/Retrieval," is explicitly unbuilt). **A durability design for "Knowledge Memory" must therefore decide whether it durably persists one store, both stores separately, or waits for the Reconciliation's own planned rename/merge to occur first** — this is a real, load-bearing planning question this review surfaces but does not answer, since resolving it is an architectural decision for the domain's own governance, not a persistence-mechanics question this review's own scope covers.

---

## 6. Existing Storage Precedents

This repository already has exactly two production, disk-durable storage implementations, both in Evidence Custodian, both readable in full this session:

- **`FileSystemEvidenceArtifactStorage`** — write-once binary content. Pattern: write to a uniquely-named temp file inside a `.tmp` subdirectory of the storage root, `FileChannel.force` it to guarantee the write has reached durable storage (not merely an OS buffer), then `Files.move` with `StandardCopyOption.ATOMIC_MOVE` into its final, identifier-derived path. A crash mid-write can only ever orphan a temp file; it can never corrupt content already readable under a real identifier. Guarded by an in-process `Mutex` (explicitly disclosed as providing no cross-process guarantee). No hash/integrity verification at this layer (disclosed as correctly excluded — nothing in the governing Contract Design requires one here).
- **`FileSystemEvidenceDeletionAudit`** — an append-only, tab-separated-value log, one line per record, each line durably `force`d before the call returns. Never truncates or rewrites a previous line. No read/query capability is exposed by design (a human inspects the file directly).

**Both share the same disclosed limitations**, directly relevant to any future Memory Core/Knowledge Memory persistence design: in-process-only concurrency guarantees (no cross-process coordination); no protection against OS/administrator-level access; fail-fast construction-time validation of the storage location (existence, directory-ness, writability) rather than deferred, first-write validation.

**Neither precedent solves Memory Core's own harder problem.** Both existing stores are single-record, single-write, no-update, no-relationship, no-cross-reference designs — write once, read by exact identifier, optionally append. Memory Core needs: five interrelated record kinds, in-place status-field transitions on existing records (never content mutation, but a durable design must still support rewriting *a* file/record if a flat, per-record-file model like `FileSystemEvidenceArtifactStorage`'s own is reused), and mandatory foreign-key-style validation (`provenanceId` must already exist) that this repository's own existing precedents never needed to solve durably. This is the central, correctly-identified gap between "an existing pattern to extend" and "genuinely new storage-design work," consistent with Scope Lock §16's own framing ("a future, separately-justified storage design").

---

## 7. Transactional Boundaries

**Today's actual boundary, precisely:** one `Mutex` per store class (`InMemoryMemoryCore`, `InMemoryKnowledgeStore`, `InMemoryKnowledgeItemPersistence` each hold their own, independent `Mutex`) — guaranteeing in-process serialisation of every operation *within* one store, but **no transactional boundary spans two stores**. Concretely:

- `EvidenceRegistrationCoordinator.register` calls `MemoryCore.createProvenance`, then (a separate call, separately permission-gated, separately mutex-acquired) `MemoryCore.registerDocument`. If a crash occurs between the two, the `Provenance` record persists (once durable) with no `Document` ever referencing it — an orphan, not a corrupted state, but not "transactional" in the ACID sense either.
- Memory Core's own five stores share **one** `Mutex` today (a single lock across all five maps) — a durable redesign could preserve this (a single, coarse-grained write transaction per operation) or split it per store; either changes today's actual concurrency behaviour and must be a deliberate design decision, not an incidental side effect of adding a disk-backed layer.
- Knowledge Memory's `evidenceReference` (into Memory Core) is validated only by `requireGovernedIdentifier`'s own **defensive, non-Memory-Core-reading** check (confirms the identifier value class itself is non-blank, never verifies the referenced record still exists) — meaning even today, in-memory, there is no cross-store transactional guarantee between Memory Core and Knowledge Memory; a durable design does not need to invent a *new* cross-store guarantee, only to preserve this already-accepted, already-disclosed lack of one.

**No existing precedent in this repository demonstrates a multi-file or multi-store atomic transaction.** `FileSystemEvidenceArtifactStorage` and `FileSystemEvidenceDeletionAudit` are both single-file-per-operation designs; neither has ever needed to coordinate two durable writes as one atomic unit. This is a genuine gap a future design must close for Memory Core specifically (at minimum: `createProvenance` and `registerDocument`, called independently by `EvidenceRegistrationCoordinator` today, would need either an accepted "the provenance-then-document sequence may leave an orphaned Provenance on crash, and this is acceptable" determination, mirroring today's own in-memory behaviour exactly, or a genuinely new cross-write transactional mechanism this repository has no existing pattern for).

---

## 8. Recovery Requirements

Distinguishing what "recovery" already means in this repository's governing text from what this review means by it:

- **Implementation Plan §11 ("Failure Recovery") is about *request-level* error handling** (a rejected precondition, a denied permission, an invalid lifecycle transition) — already fully specified, already implemented, unaffected by durability.
- **This review's own "recovery" is *process-level***: what must happen between process start and the moment Memory Core/Knowledge Memory are first ready to serve a request, if their own state must be reloaded from disk.

Requirements a future design must address, none of which any existing precedent in this repository currently answers:

1. **Load-before-serve.** `ParkerRuntime.start()` must not transition to `RUNNING` until every persisted Memory Core and Knowledge Memory record has been reloaded into the equivalent in-memory structure — otherwise a request arriving immediately after startup could observe an incomplete store. No existing `stage(...)` block in `ParkerRuntime.kt` performs a bulk load of anything today; every existing `InMemory*` construction is a zero-argument or config-only constructor, never a "read everything back in" step.
2. **Sequence-counter recovery** (§5.1, above) — each of Memory Core's five per-kind counters must resume from (highest persisted identifier of that kind) + 1, not from 1.
3. **Partial-write detection.** A crash during a Memory Core write (mid-creation, mid-transition) must leave the store recoverable to a state consistent with *some* well-defined point — either the write never happened (preferred, mirroring `FileSystemEvidenceArtifactStorage`'s own temp-file-then-atomic-move discipline) or it fully happened; a design must never load a record recognisable as "half-written."
4. **Referential-integrity recovery.** Memory Core's own creation-time checks (a candidate's `provenanceId` must already exist; a Memory-Core-owned relationship endpoint must already exist) are enforced today only at the moment of creation, in memory. On reload from disk, a design must decide whether to re-verify these invariants across the entire reloaded data set (expensive, but catches corruption) or trust the disk state unconditionally (cheaper, but assumes the write path already enforced correctness, which is only true if every write genuinely went through `InMemoryMemoryCore`'s own logic and was never corrupted at rest).
5. **Knowledge Memory's cross-store dependency on reload order.** Since a `KnowledgeItem.evidenceReference` points into Memory Core, and Knowledge Memory's own current implementation never verifies that reference against live Memory Core state (§7, above), reload order between the two stores is not *forced* to be sequenced for correctness today — but a future design that *does* choose to add reload-time verification would need Memory Core fully reloaded first.
6. **Corrupt-file handling.** Neither existing precedent (`FileSystemEvidenceArtifactStorage`/`FileSystemEvidenceDeletionAudit`) defines what happens if a file on disk is truncated, unreadable, or fails a not-yet-existing integrity check at *read* time (only write-time durability is addressed today) — a genuinely new design question for Memory Core, which would need this answered for every one of five record kinds plus two Knowledge stores.

---

## 9. Schema Migration Needs

No record type in Memory Core or Knowledge Memory carries a schema version field today. Every record is a plain Kotlin `data class`; both Errata 002/003/004 already demonstrate this domain's own governance process **can** and **does** amend record shapes (Errata 004 added a leading `requestingPrincipalId` parameter to eleven methods; earlier errata amended field-level shapes) — meaning schema evolution is already a real, recurring fact of this Programme's own history, not a hypothetical future concern.

**No existing precedent in this repository defines how an on-disk record format would be migrated across such a change.** `FileSystemEvidenceArtifactStorage` stores opaque bytes (no internal schema to migrate); `FileSystemEvidenceDeletionAudit`'s own tab-separated format is fixed-shape and undocumented for forward/backward compatibility. A future Memory Core/Knowledge Memory persistence design must decide, at minimum: whether stored records carry an explicit format-version tag; whether a future schema change (a new optional field, following this Programme's own established "additive, defaulted, never breaking" precedent — `MEMORY_ARCHITECTURE_RECONCILIATION.md` §14, citing the same discipline `MEMORY_CONTRACT_DESIGN.md` already established) requires a migration pass over already-persisted records, or can be handled by treating an absent field as its own documented default at load time (the lower-risk option, and the one most consistent with every additive change this Programme has made so far — every Errata to date has *added* a parameter/field, never removed or repurposed one).

---

## 10. Docker Volume Requirements

**An existing, exact precedent already exists and should be extended, not reinvented** (`Dockerfile` + `docker-compose.yml`, both read in full):

- Two named Docker volumes today: `evidence-storage` (mounted at `/data/evidence`) and `evidence-audit` (mounted at `/data/evidence-audit`), both declared in `docker-compose.yml`, both referenced via environment variables (`PARKER_EVIDENCE_STORAGE_ROOT`, `PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH`) that `ParkerRuntimeConfigLoader` reads with no invented default — a missing value is a startup failure, never a silently-guessed path.
- The `Dockerfile` itself creates both mount-point directories and `chown`s them to the non-root `parker` user *before* `USER parker` takes effect, specifically so a freshly-initialised named volume (which Docker seeds from the image's own directory content on first `docker compose up`) comes up already correctly owned.

**A future Memory Core/Knowledge Memory durability design would need, at minimum:** one or more new named volumes (e.g., `memory-core-storage`, `knowledge-memory-storage`, or one combined `memory-storage` volume, depending on the domain's own eventual file-layout decision — not decided here), new `ParkerRuntimeConfig` fields (mirroring `evidenceStorageRootPath`'s own required-with-no-default pattern), corresponding `Dockerfile` mount-point creation/`chown` lines, and corresponding `docker-compose.yml` volume declarations and environment variables. No new pattern needs to be invented — the existing Evidence Custodian precedent is directly reusable in shape, pending only the domain's own decision about file layout (§9, above) and whether Memory Core's five record kinds and two Knowledge stores share one volume or several.

---

## 11. Constitutional and Governance Boundaries

**The governing boundary already exists, is already accepted, and already settles the principle — only the implementation remains unauthorised.** ADR-024 ("Module, Event, Audit, and Durability Boundary," Status: Accepted), Section D, Rules 13–17, quoted directly:

> "13. **What must eventually be durable:** Memory Records (`MemoryStore.md` already calls Memory 'durable long-term knowledge' — the word itself creates the obligation), Principal records (`InMemoryIdentityService` is the trust foundation every other subsystem's identity claims rest on), and an Audit log satisfying the Constitution's 'every authorized action leaves a record sufficient to reconstruct' guarantee.
> 14. **What is allowed to remain in-memory:** World Model beliefs (transience is part of `WorldModel.md`'s own definition...), and any subsystem's internal, per-request working state that is not itself one of the three items in Rule 13.
> 15. **Memory MAY NOT be treated as durable, in the sense a caller can rely on across a process restart, before a real persistence layer exists and is verified.**... 'durable' should be read as 'logically durable within process lifetime; physical durability is a reserved seam,' until a persistence-boundary ADR and implementation exist.
> 16. **What must be true before a real user module can rely on Memory:** a real, tested persistence backing that survives process restart.
> 17. **What must be true before an audit-reconstruction claim is made:** a real, durable Audit mechanism — not `InMemoryEventBus` alone..."

This directly, independently corroborates §4's own dividing line (registries/vocabularies rebuilt at startup vs. genuinely user/world-derived state) and confirms World Model's own correct exclusion from durability scope by an authority separate from, and prior to, this review. It also broadens the *ultimate* durability obligation one step beyond this review's own named scope: **Principal records (`InMemoryIdentityService`) are an equally-named, sibling durability requirement**, sequenced separately from Memory Core/Knowledge Memory by whichever future Contract Design pass takes this up — not resolved or scoped further here, since this review's own task is Memory Core and Knowledge Memory specifically.

**ADR-024 itself explicitly declines to implement anything, and explicitly reserves the next step for elsewhere:** "This ADR does not close any of the three gaps [#47, #50, #51]... Gap #51 — this ADR settles what must eventually be durable, what may remain in-memory, and the two preconditions..., but implements no persistence or audit storage... implementation still to be authorised by a future Contract Design pass." `IMPLEMENTATION_GAPS.md` #51 independently confirms the same: "No action required to close now; requires an ADR before persistence or durable audit becomes load-bearing" — a requirement ADR-024 has now satisfied at the *principle* level, leaving the Contract Design pass as the correct, and only remaining, next governance step.

- **Persistence remains explicitly, currently out of scope for both Programmes' own Version 1** (Memory Core Scope Lock §4, §15; Knowledge Memory Scope Lock, mirroring identical language). This review does not change that — it is preparatory inventory, not an implementation authorisation, and does not itself constitute the "future Contract Design pass" ADR-024 and Gap #51 both call for.
- **No CDR appears to govern storage technology selection specifically**, and none of the reasoning above suggests one is required. A future storage design would need, at minimum, a Contract Design/Scope Lock revision for Memory Core and (separately, per §5.5) a domain decision for Knowledge Memory's own two-store question — mirroring how Evidence Custodian's own storage design (`FileSystemEvidenceArtifactStorage`) was itself an ordinary Implementation Plan Unit, never a CDR. A CDR would only become necessary if the technology choice itself became genuinely constitutionally contested, which "which file format" is not the kind of question CDR-001 through CDR-005 were written to resolve.
- **Knowledge Memory's own two-store ambiguity (§5.5, above) is itself a domain-governance question**, not a persistence-mechanics one — a future durability design should not be the vehicle that silently resolves it.

---

## 12. Risks

- **Risk: treating this review's own inventory as a green light to begin implementation.** Mitigation: this document is explicit, in its own Status section and throughout, that it authorises nothing; a Contract Design/Scope Lock revision remains a separate, required governance step.
- **Risk: a future design reuses `FileSystemEvidenceArtifactStorage`'s own write-once pattern uncritically for Memory Core**, which needs in-place status updates on existing records (never true write-once) — a real risk given how directly reusable the *rest* of that precedent is. Mitigation: §6 and §7, above, name this gap explicitly, so a future design does not discover it only after committing to the wrong file-layout strategy.
- **Risk: the two parallel Knowledge stores (§5.5) are durably persisted independently, cementing today's unreconciled duplication rather than prompting the domain's own overdue merge decision.** Mitigation: named explicitly here, for the domain's own governance to weigh before a persistence design is drafted, not decided by this review.
- **Risk: cross-store transactional expectations are assumed rather than decided.** Mitigation: §7, above, states plainly that today's actual behaviour already tolerates an orphaned `Provenance` on crash between two writes, and that a durable design's honest baseline is to preserve that same tolerance unless a future governance stage deliberately decides otherwise.
- **Risk: schema versioning is deferred so long that a real Errata-driven field change (which this Programme has already done four times) arrives before any migration mechanism exists**, forcing an ad hoc, undocumented fix under time pressure. Mitigation: §9, above, flags this as a live, not hypothetical, risk given this Programme's own documented history of amendment.
- **Risk: a future durability design treats Memory Core/Knowledge Memory persistence as the whole of Gap #51, overlooking that ADR-024 names Principal records (`InMemoryIdentityService`) as an equally-required, sibling durability obligation.** Mitigation: named explicitly in §11, above, so a future Contract Design pass scopes its own work deliberately (Memory Core and Knowledge Memory together, Identity separately, or all three at once) rather than by omission.

---

## 13. Independent Review of This Planning Review

- **Did this review implement anything?** No — no `src/` or `tests/` file was created or modified; no `docker-compose.yml`/`Dockerfile` change was made.
- **Did it modify governance?** No — no Scope Lock, Contract Design, Implementation Plan, or CDR was edited.
- **Did it select a storage technology?** No — §6 and §9 identify what a design would need to decide; neither selects a file format, database, or serialization library.
- **Did it silently resolve an open architectural question?** Checked directly: §5.5's two-parallel-store finding and §7's cross-store transactional question are both explicitly flagged as open, not resolved, matching this task's own "inventory... do not implement" framing.
- **Is the inventory complete against the task's own six named categories?** Yes: every currently in-memory production store (§4); every record that must survive restart (§5); existing storage precedents (§6); transactional boundaries (§7); recovery requirements (§8); schema migration needs (§9); Docker volume requirements (§10) — all seven of the task's own named categories (six plus Docker) are addressed with a dedicated section each.
- **Did this review present the durability question as though it had never been considered before?** Corrected during drafting: an initial pass grounded this review only in Memory Core's own Scope Lock (§16, Risk 1). A broader search surfaced `docs/adr/ADR-024-module-event-audit-durability-boundary.md` (Accepted) and `IMPLEMENTATION_GAPS.md` #51, both already, independently settling the same boundary at the principle level. The document was revised (§2, §11) to cite both directly rather than let the review imply it was breaking new ground where accepted governance already exists.

One genuine gap was found and corrected before completion: the initial draft did not cite the repository's own already-accepted ADR on this exact question. §2 and §11 were revised to incorporate it directly, and §4's inventory table and §12's risk list were both extended once the ADR's own broader scope (Principal records as a sibling durability obligation) came to light.

---

## 14. Confirmation

- Nothing was implemented. No production code was written or modified.
- No governance document was amended.
- Nothing was staged, committed, or pushed.

## 15. Final Git Status

```
$ git status --short
?? docs/reviews/MEMORY_AND_KNOWLEDGE_RESTART_PERSISTENCE_PLANNING_REVIEW.md
```

Only this review document is uncommitted.
