# Sprint 4 Track A Unit A3 — Post-Implementation Review

## Status

Unit:
Sprint 4, Track A, Unit A3 — Memory Runtime Implementation

Design basis:
`docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md` (Unit A1, reviewed and
accepted) and `docs/architecture/MEMORY_CONTRACT_DESIGN.md` (Unit A2,
reviewed and accepted, including its own revision pass) — both now
authoritative for this Unit.

Commit:
pending (this review is written before commit, per PES-001, matching
Unit C2/D2's own precedent)

Android Studio Tests:
Not yet run by a human in Android Studio (PES-001: test execution and
commit remain Human authority; no working Kotlin/Gradle toolchain was
available in this session's sandbox either — confirmed unavailable again
during this Unit). Static count only: the prior suite stood at 315/315;
`tests/contracts/MemoryContractsTest.kt` adds 19, `tests/runtime/DefaultMemoryPromotionPolicyTest.kt`
adds 10, and `tests/runtime/InMemoryMemoryStoreTest.kt` adds 16
(`tests/runtime/FakeMemoryPromotionPolicy.kt` is a fixture, contributing
no tests of its own) — a net addition of +45. If every existing and new
test passes unchanged, the expected total is 360/360. This figure is an
arithmetic projection from the source, not a verified run.

Date reviewed:
2026-07-06

Performed **before** commit, matching Unit C2/D2's own precedent — Unit
A3 is a Level 2 (Behavioural Implementation) unit under PES-001
(`docs/architecture/PARKER_ENGINEERING_STANDARD.md`) Chapter 4, and this
Post-Implementation Review is written as part of Engineering Validation
(Stage 9), before Stage 10/11 (commit, human approval).

---

## 1. Does the implementation match the Unit A1 architecture?

Yes. Every responsibility, boundary, and lifecycle stage
`docs/architecture/MEMORY_RUNTIME_ARCHITECTURE.md` assigns to Memory is
implemented exactly, and nothing beyond it:

- **Purpose and responsibilities (A1 §§1–2).** `InMemoryMemoryStore`
  stores and retrieves durable, promoted knowledge only. It has no
  method that reads or writes current world state, no method that
  reasons or proposes a plan, no method that executes a tool, and no
  method that grants or evaluates permission — confirmed by the absence
  of any such reference anywhere in `src/runtime/InMemoryMemoryStore.kt`
  or `src/runtime/DefaultMemoryPromotionPolicy.kt`.
- **Memory Categories (A1 §3).** `MemoryCategory` is the single,
  five-value closed enum A1 named (`EPISODIC`, `SEMANTIC`, `PROCEDURAL`,
  `USER_PREFERENCES`, `RELATIONSHIPS`), exactly as Unit A2 §4 determined
  it should be shaped — no category-specific type hierarchy was
  introduced.
- **Lifecycle (A1 §4).** Observation → Candidate Memory → Evaluation →
  Promotion → Long-term Memory → Retrieval is implemented end to end:
  `CandidateMemory` is the Candidate Memory stage; `MemoryPromotionPolicy.evaluate`
  is Evaluation; `MemoryPromotionDecision.Promote` causes
  `InMemoryMemoryStore.remember` to construct and store a `MemoryRecord`
  (Promotion → Long-term Memory); `retrieve` is Retrieval, implemented as
  the minimal, deterministic matching A1 §4's own retrieval bullet
  anticipated ("ranking strategy is implementation-defined"). Ownership
  of Candidate Memory (A1 §5's explicit clarification — "Memory owns
  Candidate Memory from the moment it is submitted") is implemented
  exactly: a `CandidateMemory` never exists inside `InMemoryMemoryStore`
  before `remember` is called, and every code path from that call
  onward treats it as Memory's own.
- **Ownership table (A1 §5).** Promotion, Retrieval, and Deletion are
  all performed solely by `InMemoryMemoryStore`; no other class in this
  codebase constructs a `MemoryRecord` or removes one. Consolidation and
  Retention are not implemented in this Unit (they remain a deferred
  seam, per Unit A2 §11), so their ownership assignment (also to Memory)
  is not yet exercised by any code, which is consistent with, not a
  violation of, A1's own table.
- **Input Sources (A1 §6).** `CandidateMemory.sourceSubsystem` is a
  free-text field, exactly as A1 required ("illustrative... not closed by
  architectural necessity") — no closed enum of sources was introduced,
  and nothing in this Unit's code assumes a fixed, closed set of
  submitters.
- **Interaction with the Planner Runtime, the World Model, and the rest
  of the Runtime (A1 §§8–10).** None of these integrations exist in this
  Unit's code at all — no import of, or reference to, `PlannerRuntime`,
  `AgentRunCommandChannel`, `WorldModel`, `PermissionEngine`, or
  `IdentityService` appears anywhere in `InMemoryMemoryStore.kt` or
  `DefaultMemoryPromotionPolicy.kt`. This is correct for this Unit's own
  explicit scope ("Do not implement... Planner integration, Agent
  Runtime integration, Permission Engine integration") — A1 describes how
  those systems *may* consult Memory in the future; it does not require
  this Unit to wire that consultation up.
- **Constitutional Boundaries (A1 §11).** See §6 below.

## 2. Does the implementation match the Unit A2 contract design?

Yes, field by field, including Unit A2's own revision pass:

- **`MemoryId`.** The established identifier pattern exactly — a
  `@JvmInline value class` wrapping a blank-rejecting `String` — per
  Unit A2 §1's explicit "yes."
- **`CandidateMemory`.** Every field Unit A2 §2 named is present
  (`knowledgePayload`, `proposedCategory`, `sourceSubsystem`,
  `correlationId`, `originatingPrincipalId`, `confidence`,
  `explicitlyRequested`) plus `sensitive`, carried forward per Unit A2
  §3's requirement that `MemoryRecord` carry a sensitivity indicator;
  none of the fields Unit A2 §2 said it must *not* carry (a promotion
  decision, a self-reported repetition/frequency figure, a ranking
  score) were added.
- **`MemoryRecord`.** Every field group Unit A2 §3 separated is present:
  required identity (`memoryId`), required metadata (`category`,
  provenance, `promotedAt`, `sensitive`), optional metadata
  (`confidence`, `relatedMemoryIds`), knowledge payload
  (`knowledgePayload`), and history (`history`). The type is named
  `MemoryRecord`, not `Memory`, per Unit A2 §3's naming clarification.
- **`MemoryCategory`.** A single enum, per Unit A2 §4's explicit
  determination against a five-type hierarchy.
- **`MemoryPromotionDecision`.** Exactly the two-variant sealed shape
  Unit A2 §5 designed — `Promote`/`Reject`, `Reject.reason` a non-blank,
  free-text `String`, not a closed enum — with Unit A2's own reasoning
  (individual evaluation, not batch competition; multi-factor judgement,
  not a structural rule) reflected unchanged.
- **`MemoryPromotionPolicy`.** A `suspend fun evaluate(candidate, memoryId): MemoryPromotionDecision`
  interface, exactly per Unit A2 §6, including the revision's explicit
  "SHALL determine whether a `CandidateMemory` becomes a `MemoryRecord`"
  wording (present verbatim in the interface's KDoc) and the
  Memory-internal-only invocation rule (confirmed by §5 below).
- **`MemoryQuery`.** Every required member Unit A2 §7 named is present,
  including `maximumResults` (Unit A2's own revision), validated as a
  required, positive bound.
- **`MemoryStore`.** Implemented directly by `InMemoryMemoryStore` — no
  separate `MemoryRuntime` interface exists, per Unit A2 §9's central
  minimalism determination. The `promote`-is-not-a-caller-operation
  architectural decision (Unit A2 §9's revision) is implemented exactly:
  see §5 below for the dedicated confirmation.

## 3. Were any excluded contracts implemented?

No. `CandidateMemoryId`, `MemoryQueryResult`, `MemoryRuntime`, and
`MemoryObservation` do not appear anywhere in `src/` or `tests/` — a
targeted search for each name across both directories confirms zero
matches beyond this review and the two design documents' own prose. See
the Traceability Review (§8) for the complete accounting.

## 4. Were any deferred seams implemented without justification?

No. `MemoryRetrievalPolicy` and the combined retention/consolidation
seam are not implemented as separate types or interfaces anywhere in
this Unit. `retrieve`'s minimal, deterministic matching (Principal
scope, category narrowing, case-insensitive substring match, most-
recently-promoted-first ordering, `maximumResults` truncation) is
hard-coded directly inside `InMemoryMemoryStore`, exactly as Unit A2 §8
anticipated a first implementation would do ("a first, minimal
`MemoryStore` implementation can retrieve by a simple, fixed strategy...
with no pluggable seam at all"). Consolidation and Retention have no
code path at all in this Unit — no method, no field, no scheduled
sweep — consistent with this Unit's own "do not implement autonomous
background consolidation... autonomous retention sweeps" instruction.

No deferred seam was judged "absolutely required" during this Unit's
implementation, so no stop-and-report was triggered.

## 5. The `MemoryStore` boundary: is promotion ever caller-facing?

No, confirmed both structurally and behaviourally:

- **Structurally**, `MemoryStore` has exactly three members —
  `remember`, `retrieve`, `forget` — confirmed by
  `InMemoryMemoryStoreTest`'s `MemoryStore exposes no external promote
  operation` test, which inspects the interface via reflection and
  asserts `"promote"` is absent from its member function names.
- **Behaviourally**, `MemoryPromotionPolicy` is reachable only from
  inside `InMemoryMemoryStore.remember`; no test, and no other production
  class, calls `MemoryPromotionPolicy.evaluate` directly except through
  a `MemoryStore`. `InMemoryMemoryStoreTest`'s `MemoryPromotionPolicy is
  consulted internally by InMemoryMemoryStore, exactly once per
  submission` test proves the internal call happens exactly once per
  `remember` call, using an injected `FakeMemoryPromotionPolicy`; a
  second test proves the store's promotion outcome is *entirely*
  governed by whatever policy is injected — even an explicitly-requested,
  maximum-confidence candidate is rejected when the injected fake always
  rejects, proving no hard-coded promotion logic exists inside
  `InMemoryMemoryStore` itself outside the policy seam.
- The original stub's `promote(memoryId: MemoryId): Memory` operation is
  gone. `remember` expresses submission, Evaluation, and Promotion as
  one caller-facing step returning `MemoryPromotionDecision`, which can
  represent both a promoted and a rejected outcome, exactly per Unit A2
  §9's revision.
- The original stub's `forget(memoryId): ForgetResult` is now
  `forget(memoryId): Boolean` — `ForgetResult` was never one of Unit A2's
  eight approved contracts, and this Unit declined to shape a ninth,
  unauthorised one to preserve it. This is the one place this Unit's own
  instructions explicitly authorised updating the stub, and it is
  recorded here rather than left unexplained.

## 6. Constitutional Constraints

Checked individually, against the running code, not merely asserted:

- **Memory stores knowledge.** `InMemoryMemoryStore` stores and returns
  `MemoryRecord`s; it performs no other kind of operation.
- **Memory never plans.** No type or method in this Unit generates,
  evaluates, or selects anything resembling a Plan Candidate; no
  reference to `PlanDecision`, `PlanCandidate`, or `PlannerRuntime`
  exists anywhere in this Unit's code.
- **Memory never authorises.** `CandidateMemory.sensitive` and
  `MemoryRecord.sensitive` are carried as plain flags; nothing in this
  Unit reads them to grant, deny, or filter access — that evaluation is
  left entirely to a Permission Engine this Unit does not integrate
  with, per its own explicit scope.
- **Memory never executes.** No reference to `Tool`,
  `ToolInvocationBinding`, or `ExecutionRequest` exists anywhere in this
  Unit's code.
- **Memory never reacts autonomously to events.** No `EventBus`
  dependency exists anywhere in this Unit — `InMemoryMemoryStore`'s
  constructor takes only a `MemoryPromotionPolicy`. Every operation
  (`remember`, `retrieve`, `forget`) is invoked explicitly by a caller;
  none is triggered by a subscription, a timer, or any other autonomous
  mechanism.
- **Memory provides knowledge only.** Every public operation either
  accepts knowledge (`remember`) or returns it (`retrieve`); none issues
  an instruction to, or mutates the state of, any other subsystem.
- **Memory never modifies its own contents except through explicit
  Runtime operations governed by Memory policy (Unit A2's own added
  principle).** The only paths by which a `MemoryRecord` is created or
  removed are `remember` (governed by `MemoryPromotionPolicy`) and
  `forget` (an explicit, caller-invoked operation). No background
  thread, scheduled task, or event subscription exists in this Unit that
  could promote, rewrite, summarise, or delete a record unattended.

## 7. Are tests sufficient?

Sufficient against the explicit list this Unit's own instructions
required: `MemoryId` validation (`MemoryContractsTest`); `CandidateMemory`
validation (blank payload/source/correlation id, confidence range,
every `MemoryCategory` value); `MemoryRecord` validation (the same set of
checks, directly on the record type, not merely inherited from
`CandidateMemory`); `MemoryCategory` use (both in validation and in
`InMemoryMemoryStoreTest`'s category-narrowing retrieval test); promotion
approved and promotion rejected (both the pure-policy level in
`DefaultMemoryPromotionPolicyTest` and the end-to-end level in
`InMemoryMemoryStoreTest`); the explicit-request promotion factor
(dedicated tests at both levels); the confidence-based promotion factor
(dedicated tests, including an exact-threshold boundary case and a
custom-threshold case); a rejected candidate never becoming retrievable,
and a promoted one always being retrievable; `maximumResults` being
respected (including a query that would otherwise "look unbounded");
deterministic retrieval (two consecutive `retrieve` calls with identical
input asserted equal, plus an explicit most-recently-promoted-first
ordering assertion); forgetting removing a record from retrieval;
forgetting being auditable (`wasForgotten`, a class-specific inspection
method); forgetting a nonexistent `MemoryId` being handled safely
(`false`, not an exception); `MemoryStore` exposing no external
`promote` operation (a reflection-based structural test); `MemoryPromotionPolicy`
being invoked internally, exactly once per submission, and entirely
governing the outcome (both proven via `FakeMemoryPromotionPolicy`); and
a scope-discipline structural test confirming `InMemoryMemoryStore`'s
single-argument construction, mirroring `InMemoryIdentityServiceTest`'s
own identical pattern for proving the absence of a Planner/Agent
Runtime/Permission Engine/Event Bus dependency.

One limitation, disclosed rather than hidden: the "confidence-based
promotion factor if implemented" instruction is satisfied (it is
implemented, and tested at both an exact-threshold boundary and a
comfortably-above value), but the four remaining `33-memory-consolidation.md`
factors (repetition, non-explicit user importance, goal relevance,
frequency) have no tests, because they have no implementation — see
`IMPLEMENTATION_GAPS.md` #46. Android Studio itself has not yet been run
against this suite by a human (see Status above); this review's
test-sufficiency finding is a static read of the test source, not a
confirmed green run.

## 8. Traceability Review

Every public Memory type introduced or modified by this Unit, and its
determination in `MEMORY_CONTRACT_DESIGN.md` (Unit A2):

| Type | Unit A2 determination | Implementation matches? |
| --- | --- | --- |
| `MemoryId` | Required (§1) | Yes — established identifier pattern |
| `CandidateMemory` | Required (§2) | Yes — field set matches exactly |
| `MemoryRecord` | Required (§3), renamed from `Memory` | Yes — renamed as recommended |
| `MemoryCategory` | Required (§4), single enum | Yes — five-value enum, no hierarchy |
| `MemoryPromotionDecision` | Required (§5) | Yes — two-variant sealed shape |
| `MemoryPromotionPolicy` | Required (§6) | Yes — suspend seam, internal-only |
| `MemoryQuery` | Required (§7), with `maximumResults` | Yes — including the revision |
| `MemoryStore` | Required (§9), Memory's one interface | Yes — three operations, `promote` removed |
| `CandidateMemoryId` | Excluded (§1) | Not implemented |
| `MemoryQueryResult` | Excluded (§7) | Not implemented |
| `MemoryRuntime` | Excluded (§9) | Not implemented |
| `MemoryObservation` | Excluded (§10) | Not implemented |
| `MemoryRetrievalPolicy` | Deferred (§8) | Not implemented |
| Retention/consolidation seam | Deferred (§11) | Not implemented |

Two implementation-level types exist that Unit A2 did not itself name as
public contracts, and both are addressed explicitly rather than left
unremarked:

- **`DefaultMemoryPromotionPolicy`** — a concrete class implementing the
  approved `MemoryPromotionPolicy` interface, exactly mirroring
  `DefaultPlanDecision`'s and `DefaultPermissionPolicy`'s own precedent
  of a named default implementation of an approved seam. This is not a
  new public *contract*; it is the expected, unremarkable implementation
  of one.
- **`InMemoryMemoryStore`** — the concrete class implementing the
  approved `MemoryStore` interface, exactly mirroring
  `InMemoryIdentityService`/`InMemoryToolRegistry`'s own precedent.
  Likewise not a new contract.
- **`InMemoryMemoryStore.wasForgotten`** — a class-specific public
  method, not part of the `MemoryStore` interface. This mirrors an
  already-established, already-accepted pattern in this codebase
  (`InMemoryPlannerRuntime.getSessionStatus`, `InMemoryTaskManagerRuntime.getTask`,
  `InMemoryAgentRuntime.getAgentRun`) — an observability method on a
  concrete class, outside its formal interface, not a new public
  contract requiring its own architectural authorisation.

**No public contract introduced by this Unit is unauthorised.** Every
required contract traces to `MEMORY_CONTRACT_DESIGN.md`; every excluded
and deferred item remains unimplemented; the two concrete
implementation classes and one class-specific inspection method all
follow an already-established, already-accepted codebase pattern rather
than introducing a new kind of public surface.

## 9. Process Note

Matching Unit C2/D2's own precedent, this review was written before this
unit's own commit, as part of Engineering Validation (PES-001 Stage 9),
ahead of Stage 10/11 (human commit and approval).

## 10. Was the Architecture Validated?

Unit A3 implemented, rather than changed, the already-accepted Unit A1
architecture and Unit A2 contract design. Neither document was modified.
No Architecture Decision (AD-011 Context Is Reference-Based, AD-012
Memory and World Model Are Context Providers, AD-013 Specifications
Define Contracts) required revision. The one stub-versus-design conflict
this Unit was explicitly authorised to resolve (`src/interfaces/MemoryStore.kt`'s
original `promote`/`Memory`/`ForgetResult` shapes) was resolved exactly
as Unit A2 §9 required, and is recorded in full in
`docs/implementation/IMPLEMENTATION_HISTORY.md`'s own entry for this
Unit, not merely in this review.

## 11. Remaining Gaps

- `IMPLEMENTATION_GAPS.md` #46 (new, logged alongside this review) —
  `DefaultMemoryPromotionPolicy` implements only 2 of
  `33-memory-consolidation.md`'s 6 named promotion factors; the
  remaining four require a way to compare a submission against Memory's
  own existing records that neither Unit A2 nor this Unit's own
  instructions authorise a shape for.
- `MemoryRetrievalPolicy` and the combined retention/consolidation seam
  remain deferred, unimplemented, per Unit A2's own determination —
  not a defect of this Unit, a documented, intentional scope boundary.
- No embeddings, vector database, storage engine, persistence layer, or
  Android API exists anywhere in this Unit's code, per its own explicit
  scope; Memory's durable storage today is process-memory only, and does
  not survive a restart.
- Android Studio has not yet been run against this suite by a human; the
  360/360 figure in this review's Status section is a static projection,
  not a confirmed result.

## 12. Decision

**Proceed to human test verification.** The implementation matches the
accepted Unit A1 architecture and Unit A2 contract design in full, adds
no excluded contract, implements no deferred seam without justification,
introduces no unauthorised public contract, and Memory has gained no
planning, authorisation, execution, or event autonomy anywhere in this
Unit's code. The one stub-versus-design conflict was resolved as
instructed and explained here and in `IMPLEMENTATION_HISTORY.md`. This
unit is complete pending a human running the test suite in Android
Studio and, if it passes, committing — no further design or
implementation work is recommended before that commit.
