# Memory Core — Implementation Plan

## Status

Programme: **Programme 2 — Memory Core Implementation Plan.**
Phase: **Final design document before production coding begins.** No
Kotlin is implemented, proposed as a diff, or changed by this document.
Neither `src/` nor `tests/` is touched. Nothing is staged, committed, or
pushed.

**Normative inputs, frozen, not redefined:**
`docs/architecture/MEMORY_CORE_GOVERNANCE_REVIEW.md`,
`docs/architecture/MEMORY_ARCHITECTURE_RECONCILIATION.md`,
`docs/architecture/MEMORY_CORE_CONTRACT_DESIGN.md`, and
`docs/architecture/MEMORY_CORE_SCOPE_LOCK.md`. This document describes
**how** the already-approved Memory Core will be built — sequencing,
repository impact, runtime wiring, testing, and failure handling. It
introduces no contract, no field, no lifecycle rule, no permission
model, and no event beyond what those four documents already froze.
Where this plan makes a genuinely new decision (a class name, a file
location, an internal wrapper pattern), it is disclosed as an
implementation-level choice, not presented as an architectural one.

---

## 1. Executive Summary

This plan sequences Memory Core's build as **nine independently
compilable, independently testable units**, not the twelve this task's
own example suggested — the difference, and the reason for it, is
Section 4's own subject. The strategy is: build the data foundation
first (identifiers, then `Provenance`, since every record type has a
*mandatory* Provenance reference and cannot be meaningfully typed
before it exists), then the four record types, then the two public
interfaces (which cannot be meaningfully signed until the types they
operate on already exist), then one concrete implementation satisfying
both, then event publication as a separately verifiable increment, then
runtime composition behind two thin, Scope-Lock-mandated permission
gates, and finally acceptance verification against all four frozen
documents together. Every unit leaves the repository compiling and every
existing test green; no unit depends on a later one.

---

## 2. Current Repository Assessment

| Component | Disposition | Basis |
| --- | --- | --- |
| `MemoryStore` (interface + `CandidateMemory`/`MemoryRecord`/etc., `src/interfaces/MemoryStore.kt`) | **Left untouched.** | The Reconciliation establishes Memory Core as a new foundation beneath a future-renamed Knowledge Memory; the rename/adaptation itself is explicitly deferred (Scope Lock §15, Out-of-Scope Register). Zero lines of this file change in this plan. |
| `InMemoryMemoryStore` | **Left untouched.** | Same reasoning. Its production wiring in `ParkerRuntime.kt` (as `MemorySource`) is unaffected. |
| Conversation History (`ConversationEngine`/`ConversationHistorySource`/`InMemoryConversationEngine`) | **Left untouched.** | Memory Core references it only by identifier, through `Provenance.sourceIdentifier` (a free-text field, Contract Design §7) — never a structural dependency, never a code change. |
| `ParkerRuntime` composition (`src/composition/ParkerRuntime.kt`) | **Adapted.** | New construction lines are added inside `buildAndRegisterRuntimeGraph()` (Section 7, below); every existing line is preserved unchanged. This is additive, matching every prior Programme's own composition-root pattern (Memory Source Integration, World Model Source Integration, Controlled Agent Run Submission all added lines to this same function without restructuring it). |
| `PermissionEngine` (interface, `DefaultPermissionEngine`, `DefaultPermissionPolicy`) | **Reused, not modified.** | `PermissionEngine.evaluate` is called by new wrapper classes (Section 7); no interface or policy-engine code changes. New `PermissionPolicyRule`/`ActionVocabularyEntry` values are new *data* supplied at composition time, mirroring exactly how `AGENT_RUN_START_VERB_PHRASE` was added for Controlled Agent Run Submission — not a new code path inside `DefaultPermissionEngine` or `DefaultPermissionPolicy` themselves. |
| `EventBus` (interface, `InMemoryEventBus`) | **Reused, not modified.** | `InMemoryMemoryCore` (Section 4, Unit 8) acquires a constructor dependency on it, exactly as `InMemoryTaskManagerRuntime`/`InMemoryAgentRuntime` already do. New `EventType` values are new data, not new `EventBus` code. |
| `AuditService` | **Deferred, untouched.** | Confirmed absent from this repository entirely (no implementation, no wiring, anywhere) by the Governance Review. Memory Core's own auditability requirement is satisfied structurally by `Relationship` records plus `memory.record_status_changed` events (Contract Design §12, Scope Lock §3) — this plan does not implement, wire, or depend on `AuditService` in any way. |
| Existing tests (all of `tests/`) | **Left untouched.** | No existing test file is modified by this plan or by the implementation it sequences. Every new behaviour is covered by new test files only (Section 9). |

---

## 3. Implementation Principles

Every unit in Section 4 is designed to individually satisfy all six:

- **Smallest safe increment.** Each unit adds one coherent piece of
  surface (a set of related types, one interface, one implementation, or
  one integration step) — never a partial type, never half of a
  contract already frozen.
- **Compile after every unit.** No unit leaves the repository in a
  state where `src/` fails to build, even transiently.
- **Tests remain green.** No unit breaks an existing test. Each unit
  that introduces new public surface adds its own tests in the same
  unit, per Scope Lock §12's own frozen minimum test surface.
- **No architectural drift.** No unit may introduce a field, operation,
  event, retrieval mode, or lifecycle transition beyond what the four
  frozen documents already approved. A unit that discovers a need to
  deviate stops and requests a governance revision — it does not
  improvise.
- **No hidden behavioural changes.** No unit alters the behaviour of
  `MemoryStore`, `ConversationEngine`, `PermissionEngine`, `EventBus`,
  or any other existing component as a side effect of adding Memory
  Core alongside them.
- **Deterministic behaviour.** Every new operation behaves exactly as
  Scope Lock §11 requires — same inputs, same stored state, same
  output, every time.

---

## 4. Proposed Implementation Units

**This plan does not adopt the twelve-unit example sequence given in
this task's own brief, and states exactly why.** That sequence places
`Provenance` (its own Unit 7) after `Entity`, `Document`, `Assertion`,
and `Relationship` (Units 3–6) — but Contract Design §7 and Scope Lock §7
both freeze Provenance as a **mandatory** reference on every one of
those four record types. A `ProvenanceId` field cannot be meaningfully
typed on `Entity` before `ProvenanceId` and `Provenance` themselves
exist, so Unit 3 could not compile under the example ordering without
either a forward reference or a temporary placeholder — both of which
violate "compile after every unit" and "no hidden behavioural changes."
The same problem affects placing "Unit 2, MemoryCore write interface"
before any record type exists: an interface whose own methods (for
example, "register an Entity") cannot be meaningfully signed without
`Entity` already existing is not a real, compilable Unit 2.

**Revised, dependency-correct sequence — nine units:**

| # | Unit | Prerequisite units | What it adds |
| --- | --- | --- | --- |
| 1 | Shared identifiers and enumerations | None | `EntityId`, `DocumentId`, `AssertionId`, `ProvenanceId`, `RelationshipId`; the shared lifecycle status type (Scope Lock §8); the five-value `ContentNature` classification (Contract Design §7). |
| 2 | `Provenance` | 1 | The `Provenance` record type and its own construction-time validation (mandatory fields per Scope Lock §7 rejected if absent; optional fields genuinely nullable). |
| 3 | `Entity` | 1, 2 | The `Entity` record type, referencing `Provenance` via a mandatory `provenanceId`. |
| 4 | `Document` | 1, 2 | The `Document` record type, registration-only (Contract Design §5), referencing `Provenance`. |
| 5 | `Assertion` | 1, 2 | The `Assertion` record type, referencing `Provenance`; no embedded support/contradiction fields (Contract Design §6). |
| 6 | `Relationship` | 1, 2, 3, 4, 5 | The `RelationshipEndpoint` shape and the `Relationship` record type, including the seven recognised relationship-type values (Contract Design §8). Sequenced last among the record types because its own tests need real `Entity`/`Document`/`Assertion` records to connect, even though its own field shape has no hard compile-time dependency on them beyond `Provenance`. |
| 7 | `MemoryCore` (write interface) and `MemoryRetrieval` (read interface) | 1–6 | The two public interfaces Contract Design §3/§9 named, now signable against real types: `MemoryCore` exposes creation, amendment, dispute, supersession, archival, and owner-erasure operations for all four record kinds plus the shared status-transition operation; `MemoryRetrieval` exposes the seven frozen retrieval modes. |
| 8 | `InMemoryMemoryCore` | 7 | The one concrete implementation of both interfaces (mirroring `InMemoryMemoryStore`'s own "one class, two interfaces" precedent), including lifecycle/status-transition validation and immutability enforcement (Scope Lock §8/§12) — **not** a separate later unit, since transition validation is one cohesive piece of logic every write operation must already respect correctly from its first working version, and splitting it out later would mean this unit ships an incomplete, incorrect implementation in the interim, violating "tests remain green." Event publication (Section 8, below) is **not** included in this unit — see Unit 9. |
| 9 | Event publication | 8 | Adds an `EventBus` constructor dependency to `InMemoryMemoryCore` and the five publish calls (Contract Design §13, Scope Lock §9) at the exact points Section 8 of this plan specifies. Kept separate from Unit 8, deliberately: Unit 8's own correctness (record creation, provenance enforcement, lifecycle transitions, immutability) is independently verifiable without any `EventBus` involved at all, and testing it that way first isolates any defect to either "the store's own logic" or "the event wiring" — never both at once. |

**Runtime composition and acceptance verification are deliberately not
numbered as Units 10 and 11 within this table.** They are sequenced
next, but they are qualitatively different from Units 1–9 (which each
add new, independently testable production surface) — they are
integration and verification passes over surface that already exists
and is already fully tested in isolation. They are addressed in full in
Sections 7 and 12, respectively, rather than compressed into a single
table row each, since Sections 7 and 12 are where this task's own brief
requires their real substance to live.

---

## 5. Dependency Graph

| Unit | Prerequisites | Outputs | Repository impact | Public contracts affected |
| --- | --- | --- | --- | --- |
| 1 | None | `EntityId`, `DocumentId`, `AssertionId`, `ProvenanceId`, `RelationshipId`, shared status enum, `ContentNature` | New file: `src/interfaces/MemoryCore.kt` (opened) | None existing; five new identifier types, two new enums |
| 2 | 1 | `Provenance` | Same file, extended | `Provenance` (new) |
| 3 | 1, 2 | `Entity` | Same file, extended | `Entity` (new) |
| 4 | 1, 2 | `Document` | Same file, extended | `Document` (new) |
| 5 | 1, 2 | `Assertion` | Same file, extended | `Assertion` (new) |
| 6 | 1, 2, 3, 4, 5 | `RelationshipEndpoint`, `Relationship` | Same file, extended | `Relationship` (new) |
| 7 | 1–6 | `MemoryCore`, `MemoryRetrieval` interfaces | Same file, completed | `MemoryCore`, `MemoryRetrieval` (new) |
| 8 | 7 | `InMemoryMemoryCore` | New file: `src/runtime/InMemoryMemoryCore.kt` | None existing; new concrete class |
| 9 | 8 | Event-publishing `InMemoryMemoryCore` | Same file, extended; five new `EventType` values used (no `EventContracts.kt` change — `EventType` is already an open, string-validated value class) | None existing |
| Runtime composition | 1–9 | Wired, permission-gated Memory Core in the running system | `src/composition/ParkerRuntime.kt` extended; two new files: `src/composition/PermissionGatedMemoryCore.kt`, `src/composition/PermissionFilteredMemoryRetrieval.kt` | None existing; two new decorator classes |
| Acceptance verification | Runtime composition | Confirmation of Scope Lock conformance | No new file; full test suite run, structural review | None |

---

## 6. Repository Impact

- **New packages:** none. Every new file lives in an existing package
  (`parker.core.interfaces`, `parker.core.runtime`, `parker.composition`),
  matching this repository's own established convention that a new
  subsystem is a new *file*, not a new *package*.
- **New interfaces:** `MemoryCore`, `MemoryRetrieval`
  (`src/interfaces/MemoryCore.kt`) — one file for the whole subsystem,
  bundling its own five record types and two identifier-adjacent
  supporting types alongside the two interfaces, mirroring
  `src/interfaces/MemoryStore.kt`'s and `src/interfaces/WorldModel.kt`'s
  own identical "one file per subsystem" precedent exactly, at a larger
  but proportionate scale (seven public contracts and their supporting
  types, against Memory's eight and World Model's six). This file will
  be substantially larger than either existing precedent; this plan
  accepts that as the cost of following established convention rather
  than inventing a new file-splitting scheme with no concrete need
  identified (Scope Lock's own "avoid speculative optimisation"
  principle, applied here to file organisation, not just code).
- **New implementations:** `InMemoryMemoryCore`
  (`src/runtime/InMemoryMemoryCore.kt`, mirroring `InMemoryMemoryStore`'s
  exact naming convention); `PermissionGatedMemoryCore` and
  `PermissionFilteredMemoryRetrieval` (`src/composition/`, Section 7,
  below).
- **Composition changes:** additive lines inside
  `ParkerRuntime.buildAndRegisterRuntimeGraph()` only (Section 7). No
  existing line in that function is altered or reordered relative to
  itself; new lines are inserted following the same pattern every prior
  Programme's own composition work already used.
- **Test additions:** new files only, one roughly per production file
  above, following this repository's own established one-test-file-
  per-production-file convention (`InMemoryMemoryStoreTest.kt` alongside
  `InMemoryMemoryStore.kt`, and so on) — enumerated fully in Section 9.
- **Documentation updates:** none are performed *by* this plan — this
  plan is itself a design document, not an implementation. Once
  implementation actually proceeds, `docs/implementation/IMPLEMENTATION_HISTORY.md`
  and `docs/architecture/IMPLEMENTATION_GAPS.md` will need the same kind
  of entry every prior Programme's own implementation phase has added,
  and `README.md` will eventually need a milestone section mirroring
  "Milestone: Controlled Agent Run Submission" — but per this repository's
  own established Development Method (Governance Review → Contract
  Design → Scope Lock → Approval → **Implementation** → Native
  Verification → **Documentation Reconciliation** → Commit → Push),
  that reconciliation is its own later phase, not a deliverable of this
  Implementation Plan.
- **No unnecessary movement of existing code.** Nothing above relocates,
  renames, or restructures any file that exists today.

---

## 7. Runtime Integration

**Exactly how Runtime will invoke Memory Core**, concretely, not merely
described in principle:

Two thin decorator classes are introduced in `src/composition/`,
mirroring the exact, already-established pattern
`LoggingReasoningProvider` and `LoggingCommunicationIntake` already use
in this same package — a class implementing the same interface as a
wrapped delegate, adding one cross-cutting concern, with `ParkerRuntime.kt`
composing only the wrapper under the interface type, never the raw
delegate directly:

- **`PermissionGatedMemoryCore`**, implementing `MemoryCore`, wrapping a
  real `InMemoryMemoryCore` plus `PermissionEngine` and `ActionMapper`.
  Every write operation (`registerEntity`, `registerDocument`,
  `recordAssertion`, `recordRelationship`, and every status-transition
  operation — amend, dispute, supersede, archive, delete) first resolves
  the proposed action against `ActionVocabularyEntry`s registered for
  Memory Core (new *data*, registered at composition time, mirroring
  `AGENT_RUN_START_VERB_PHRASE`'s own precedent — no new vocabulary
  *mechanism*), then calls `PermissionEngine.evaluate`, and only on
  `APPROVED`/`APPROVED_WITH_CONFIRMATION` delegates to the wrapped
  `InMemoryMemoryCore`. On denial, it returns a rejection outcome to its
  own caller (Section 11) — it never calls the wrapped store at all.
- **`PermissionFilteredMemoryRetrieval`**, implementing `MemoryRetrieval`,
  wrapping the same `InMemoryMemoryCore` plus `PermissionEngine`. Each
  of the seven retrieval modes first calls the wrapped store's own
  structural implementation (Contract Design §9's own disclosed rule:
  `MemoryRetrieval` itself performs no principal-based filtering), then
  evaluates a `READ` permission decision **per returned record**,
  filtering out anything not approved before the result ever reaches the
  caller — never a single, whole-query gate, since a query can
  legitimately match records with different sensitivity levels.

**`ParkerRuntime.kt` composes `InMemoryMemoryCore` once, and exposes it
to the rest of the running system only through these two wrapper types**
— exactly the same discipline already governing `InMemoryMemoryStore`'s
own exposure only as `MemorySource`, never as a raw `MemoryStore`
reference, to any component other than the one place that needs the
fuller interface.

**Confirmed, structurally, not merely asserted:**

- **Permission decisions occur before Memory Core.** `InMemoryMemoryCore`
  itself is never invoked by anything in the composed runtime except the
  two wrapper classes above, each of which performs its own permission
  check strictly before delegating.
- **Memory Core remains permission-neutral.** `InMemoryMemoryCore`'s own
  constructor takes no `PermissionEngine` parameter at all — a
  structural guarantee, not a convention, mirroring the same "the type
  system prevents it" discipline `MemorySource`'s own capability-
  narrowing already relies on.
- **Knowledge Memory remains above Memory Core.** No production line
  introduced by this plan gives `InMemoryMemoryStore` (today's Memory) a
  dependency on `InMemoryMemoryCore`, `PermissionGatedMemoryCore`, or
  `PermissionFilteredMemoryRetrieval`, in either direction. Per Scope
  Lock §15's own Out-of-Scope Register, the future Knowledge Memory read
  boundary onto Memory Core is explicitly deferred — this plan does not
  anticipate or partially build it.

**No production caller is introduced for Memory Core's write path in
this plan**, and this is a deliberate, disclosed choice, not an
omission: Scope Lock's own frozen scope (Sections 3–4) names no Tool,
coordinator, or Document Handling caller as a Version 1 deliverable.
This mirrors exactly today's own Memory: `MemoryStore.remember` is
fully implemented, fully tested, and fully wired into composition, with
zero production callers, because no legitimate caller has been
separately approved yet. `InMemoryMemoryCore`, composed behind its two
permission gates, will be in an identical, deliberately dormant-but-
ready state at the end of this plan's own scope.

---

## 8. Event Integration

Each of the five frozen events (Scope Lock §9) is published from inside
`InMemoryMemoryCore` (Unit 9), never from either wrapper class, and
**only after the corresponding state change has already been committed**
to the store's own internal state — never before, and never on a path
that also fails:

- **`memory.entity_created`** — published after a new `Entity` is
  inserted into the store's own internal map, inside the same
  `Mutex`-guarded block that performed the insertion (mirroring
  `InMemoryAgentRuntime`'s own established "publish after committing
  state" pattern, e.g. `agent.ready` published only after `RunState` is
  already recorded).
- **`memory.document_registered`** — same pattern, after `Document`
  insertion.
- **`memory.assertion_created`** — same pattern, after `Assertion`
  insertion.
- **`memory.relationship_created`** — same pattern, after `Relationship`
  insertion, including for relationships expressing an amendment,
  dispute, or supersession — this is the one event this plan's own
  design (Section 4, Unit 9) treats as carrying the most weight, per
  Contract Design §12's own finding that `Relationship` creation *is*
  how a correction becomes visible to the rest of the runtime.
- **`memory.record_status_changed`** — published after a status
  transition (Scope Lock §8) has been validated and committed, carrying
  the record kind, record identifier, prior status, and new status. Not
  published for the initial `ACTIVE` status a record receives at
  creation (that fact is already carried by the corresponding
  `*_created` event) — only for a genuine transition away from it.

**If any validation step (missing provenance, an invalid transition, an
unverifiable owned-kind relationship endpoint) fails, no event is
published at all** — every one of the five events is emitted strictly
after, and only after, the operation it corresponds to has fully and
successfully committed. A partially-applied write is not a state this
design permits (Section 11) and therefore not a state any event ever
needs to represent.

---

## 9. Testing Strategy

Recommended, organised by kind, per this Unit's own brief:

- **Unit tests** — one test file per production file (Section 6),
  covering each record type's own construction-time validation
  (mandatory fields rejected when absent, per Scope Lock §7) in
  isolation, with no store or interface involved.
- **Contract tests** — structural tests confirming `MemoryCore` and
  `MemoryRetrieval` expose exactly the operations Contract Design §3/§9
  named and no others, and that neither interface's method signatures
  admit a `PermissionEngine` parameter anywhere (Section 6's own
  permission-neutrality guarantee, checked at the interface level, not
  only the implementation level).
- **Runtime tests** — `InMemoryMemoryCoreTest.kt`, covering: creation
  success and provenance-absence rejection for all four record kinds;
  every lifecycle transition Scope Lock §8 defines, and rejection of
  every transition it does not define; immutability (no operation exists
  to alter an immutable field — the same "the guarantee is that no such
  method exists" shape today's `MemoryStore` tests already use);
  referential integrity enforced for owned endpoint kinds, unverified
  for external ones; deterministic, repeatable retrieval across all
  seven modes; and the five events, each published exactly once per
  corresponding successful act, none published on any failure path.
- **Composition tests** — extending `tests/composition/CompositionTestFixtures.kt`'s
  own established pattern: confirm `ParkerRuntime.start()` constructs
  `InMemoryMemoryCore` exactly once, exposes it only through the two
  wrapper types, and that no existing composition test's own assertions
  change as a result (Section 2's "left untouched" claim, verified, not
  only asserted).
- **Negative tests** — every failure mode Section 11 (below) names,
  each with its own dedicated test: missing provenance, duplicate Entity
  (confirmed to succeed, not reject — a negative test in the sense of
  "confirms the *absence* of unwanted deduplication behaviour"), invalid
  lifecycle transition, invalid relationship (self-referencing, blank
  type), unknown reference for an owned endpoint kind, and permission
  denial.
- **Permission boundary tests** — `PermissionGatedMemoryCoreTest.kt` and
  `PermissionFilteredMemoryRetrievalTest.kt`, using a `FakePermissionEngine`
  (mirroring `tests/runtime/FakePermissionEngine.kt`'s own existing
  precedent) to confirm: approval delegates to the wrapped store; denial
  never reaches it; and per-record filtering in
  `PermissionFilteredMemoryRetrieval` correctly narrows a multi-record
  result set rather than gating the whole query at once.
- **Lifecycle tests** — covered within `InMemoryMemoryCoreTest.kt` above;
  called out separately here because Scope Lock §8 is itself the single
  most detailed frozen rule this plan implements, and deserves its own
  named, complete test group, not an incidental subset of general
  runtime tests.
- **Provenance tests** — covered within the unit tests for `Provenance`
  itself (mandatory-field rejection, nullable-field acceptance,
  `contentNature` defaulting to `UNKNOWN` rather than `ORIGINAL`) and
  cross-checked in `InMemoryMemoryCoreTest.kt` (every record type's
  creation genuinely fails without one).

**What remains unchanged:** every test file that exists in this
repository today, without exception. No test in `tests/runtime/`,
`tests/composition/`, or `tests/contracts/` is modified by this plan or
by the implementation it sequences.

---

## 10. Migration Strategy

**No migration is required, and none is performed by this plan.** Memory
Core and today's `MemoryStore` **coexist, unconnected**, in the same
composition root — exactly as Memory and the World Model already coexist
unconnected today, and exactly as the Reconciliation's own accepted
architecture requires (Knowledge Memory depends on Memory Core; nothing
in Version 1 makes that dependency real yet, since the Knowledge Memory
side of it is explicitly deferred, Scope Lock §15).

**No adapter is introduced in this plan.** The Reconciliation (its
Section 14) already recommended that an adapter — reshaping Knowledge
Memory's own submission path to reference real Memory Core records —
should wait until Memory Core itself exists and is real. This plan is
that existence; the adapter itself remains a distinct, later, separately-
scoped unit, not something this plan anticipates or partially builds.

**Existing behaviour is not broken, structurally, not merely by
intention:** since no line in `MemoryStore.kt`, `InMemoryMemoryStore.kt`,
`DefaultMemoryPromotionPolicy.kt`, or `DefaultReasoningContextAssembler.kt`
is touched, there is no code path by which this plan's own work could
alter today's Memory behaviour, tested or otherwise.

---

## 11. Failure Recovery

Each failure mode is handled by the mechanism most consistent with how
this exact repository already distinguishes a genuine caller-side
precondition violation (a hard, thrown failure) from a legitimate,
expected business outcome (a returned, sealed result) — not by a single
uniform policy applied indiscriminately:

| Failure mode | Handling | Precedent followed |
| --- | --- | --- |
| **Missing provenance.** | A hard, construction-time failure (`require`-style precondition), rejecting the submission outright. | `MemoryQuery`'s own existing `init` block validation. |
| **Duplicate Entity.** | **Not a failure at all** — both records are accepted, independently (Contract Design §14). Recorded here to confirm this is deliberate, not an oversight. | Contract Design §14's own explicit determination. |
| **Duplicate Document.** | Same treatment as duplicate Entity. | Same. |
| **Conflicting lifecycle** (a transition attempted from a status that no longer permits it, including a lost race between two concurrent callers). | A hard, thrown failure (`IllegalStateException`, naming the attempted and actual current status), never silently repaired or coerced into a different, "close enough" transition. Concurrent attempts are serialised under the same `Mutex`-per-store discipline `InMemoryMemoryStore`/`InMemoryAgentRuntime` already use; the losing caller sees the real, current status and fails against it honestly. | `ConversationEngine.submitTurn`'s own "thrown, never silently repaired" precedent for a comparable state-mismatch case. |
| **Invalid relationship** (self-referencing endpoint, blank relationship type). | A hard, construction-time failure, same shape as missing provenance. | Same as missing provenance. |
| **Unknown references** (an endpoint of a Memory-Core-owned kind naming a nonexistent record). | A hard, construction-time failure — referential integrity is enforced for owned kinds (Contract Design §8). An endpoint of an *external* kind (Conversation, future Knowledge) is never checked and therefore never fails this way, by design. | Contract Design §8's own referential-integrity rule. |
| **Runtime rejection** (a permission decision is not `APPROVED`/`APPROVED_WITH_CONFIRMATION`). | A returned, sealed outcome from `PermissionGatedMemoryCore` — never a thrown exception, since permission denial is a wholly legitimate, expected outcome a caller must handle gracefully, not a programming error. | `AgentRunCommandResult.Accepted`/`Rejected`'s own precedent for an identical shape of decision. |

---

## 12. Acceptance Sequence

Applied identically to each of Section 4's nine units, and again to
Runtime composition:

1. **Compile.** The unit's own new code, and the whole repository around
   it, builds cleanly.
2. **Tests.** The unit's own new tests (Section 9) pass, and the full
   existing test suite continues to pass unchanged.
3. **Review.** The unit is checked against the specific frozen document
   section it implements (cited unit-by-unit in Section 4/5 above) —
   confirming no field, operation, event, or transition beyond what was
   approved has been introduced.
4. **Then proceed** to the next unit, only once 1–3 are all satisfied for
   the current one.

No unit begins before the unit(s) it depends on (Section 5's own
dependency graph) have each independently passed all three checks.

---

## 13. Completion Criteria

Version 1 is complete when, objectively:

- All seven contracts (Contract Design §3) are implemented exactly as
  designed, with no field, operation, or type beyond what was approved.
- Every test named in Section 9 passes, and no existing test has
  changed.
- `ParkerRuntime.kt` composes `InMemoryMemoryCore` exactly once, exposed
  only through `PermissionGatedMemoryCore`/`PermissionFilteredMemoryRetrieval`
  (Section 7), with `ParkerRuntime.start()` continuing to succeed exactly
  as it does today.
- Every Scope Lock §14 `SHALL` statement is individually true of the
  implementation, verified, not merely assumed.
- No deviation from any of the four frozen documents exists anywhere in
  the implemented code — confirmed by the Section 12 review step
  performed at each unit, not deferred to one single pass at the end.

---

## 14. Deferred Work Register

Governed entirely by `MEMORY_CORE_SCOPE_LOCK.md` §15's own Out-of-Scope
Register, restated here only by reference, not re-litigated: OCR, PDF
parsing, file import, image analysis, Evidence Item, semantic/embedding
retrieval, a `MemoryRetrievalPolicy`-equivalent seam, Knowledge Memory's
own read boundary onto Memory Core, the `MemoryStore` → Knowledge Memory
rename/adaptation, persistent/durable storage, an explicit
`UNCLASSIFIED` `ResourceSensitivity` value, audit-identifier validation
mechanics, World Model integration, the Conversation-absorption-versus-
reference-only open question, workflow integration, network
synchronisation, and cloud storage. This plan adds exactly one further,
implementation-level item to that register: **a production caller for
Memory Core's write path** (Section 7) — not architecturally excluded,
simply not yet approved, mirroring today's own dormant Memory write
path precisely.

---

## 15. Risks

- **Risk: the two permission-gating wrapper classes become the one
  place a bug could silently reopen Scope Lock's own central guarantee**
  (that Memory Core never authorises itself, and nothing reaches it
  ungated). **Mitigation:** Section 9's own dedicated Permission Boundary
  tests exist specifically to make a gating bug a test failure, not a
  silent gap — including a structural test that `InMemoryMemoryCore`
  itself is never reachable from `ParkerRuntime.kt` except through the
  two wrappers.
- **Risk: `src/interfaces/MemoryCore.kt` becomes large enough (seven
  contracts and their supporting types in one file) to be difficult to
  review as a single unit of change.** **Mitigation:** Section 4's own
  unit sequencing already breaks its construction into seven small,
  independently reviewable increments (Units 1–7), even though they
  land in one file — the file's eventual size is not the same as any
  single reviewable change being large.
- **Risk: the `Mutex`-per-store concurrency model `InMemoryMemoryCore`
  inherits from `InMemoryMemoryStore`'s own precedent could become a
  bottleneck once a real, concurrent production caller exists.**
  **Mitigation:** not a Version 1 concern, since Section 7 confirms no
  production caller exists yet; recorded here so a future unit
  introducing the first real caller re-evaluates it against actual,
  concrete concurrency requirements at that time, rather than this plan
  guessing at a requirement that does not yet exist.
- **Risk: five new `EventType` values with no consumer anywhere in the
  running system are easy to introduce inconsistently (a typo in the
  namespaced string, a payload key that drifts between the five)** since
  nothing outside this plan's own new tests exercises them end-to-end.
  **Mitigation:** Section 9's Runtime tests assert the exact `EventType`
  string and payload shape for each of the five events individually,
  making drift a test failure at the moment it is introduced, not a
  silent inconsistency discovered later by a future consumer.
- **Risk: introducing `PermissionPolicyRule` entries for Memory Core's
  own `WRITE`/`READ`/`DELETE` actions at composition time could be
  drafted too broadly** (mirroring the care `ParkerRuntime.kt`'s own
  existing comments already take to explain exactly how narrow its
  `EXECUTE`/`AGENT` rule is). **Mitigation:** the Composition tests
  (Section 9) must assert the new rules approve only what Scope Lock §6
  actually requires, and deny everything else, exactly as the existing
  `DefaultPermissionPolicy` conservative-default-deny behaviour already
  guarantees for every unnamed action/resource-type pair.

---

## 16. Recommendation

Every unit in Section 4 traces to a specific contract, rule, or boundary
already frozen by one of the four normative documents; the one
deviation from this task's own example sequence (Provenance-first, and
the write/read interfaces sequenced after every record type they
operate on, not before) is justified by a concrete, stated compile-order
dependency, not a stylistic preference. No new architecture, technology,
or public contract is introduced anywhere in this plan.

```
READY FOR IMPLEMENTATION
```
