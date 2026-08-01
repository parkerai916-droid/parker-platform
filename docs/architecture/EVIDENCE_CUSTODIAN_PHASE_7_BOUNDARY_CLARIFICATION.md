# Evidence Custodian — Phase 7 Boundary Clarification

## Status

**Final. Approved governance clarification.** Adopted 1 August 2026,
following a read-only Planning Review and two rounds of refinement. Does
not reopen `docs/decisions/CDR-006_CONSTITUTIONAL_CLASSIFICATION_OF_ORIGINAL_EVIDENCE_CUSTODY_AND_IMMUTABILITY.md`
("CDR-006"), `docs/architecture/EVIDENCE_ARTIFACT_CONTRACT_DESIGN.md`
("the Contract Design"), `docs/architecture/EVIDENCE_CUSTODIAN_SCOPE_LOCK.md`
("the Scope Lock"), or `docs/architecture/EVIDENCE_CUSTODIAN_IMPLEMENTATION_PLAN.md`
("the Implementation Plan") — all four remain frozen and unmodified by
this document, exactly as ratified in
`docs/reviews/EVIDENCE_CUSTODIAN_CDR-006_FINAL_FREEZE_REVIEW.md`. This
document alters no constitutional doctrine, responsibility, exclusion,
or boundary any of those four already fixed. Its purpose is narrower and
final: resolve, before any Kotlin is written, the four boundary
questions that Phase 7 (Deletion Workflow) left genuinely open — the
smallest lawful audit mechanism, whether a coordinator is required, the
exact ordering that prevents an unaudited successful deletion, and the
structural shape of owner-only enforcement.

Programme: **Evidence Custodian — Phase 7 (Deletion Workflow), Boundary
Clarification.**

No Kotlin is implemented, proposed as a diff, or changed by this
document. No test is modified. Nothing is staged, committed, or pushed
by this document — that remains Steven's own action, following his own
verification.

---

## 1. Determinations

1. **Phase 7 is Custodian-only.** Deletion ends physical custody within
   the Evidence Custodian alone. It never transitions a Memory Core
   `Document` or `Provenance` record. Cross-subsystem reconciliation is
   deferred to a future, separately governed coordinator, because no
   reliable artefact-to-Document mapping exists today — Memory Core's
   `Document.locationReference` is an opaque string Memory Core "never
   fetches, opens, or validates," and there is no indexed, reverse
   lookup from an `EvidenceArtifactId` back to whichever `Document`(s),
   if any, reference it.
2. **No storage tombstone.** Deleting an absent or already-deleted
   artefact produces `NotFound`. The storage layer retains no deletion
   marker and no copy of the original content. The durable audit record
   defined in Section 4 is the sole retained trace that a deletion
   occurred.
3. **Owner-only deletion is structurally enforced**, not enforced by
   policy content alone. Deletion is not exposed on `EvidenceCustodian`
   — the interface ordinary consumers (retrieval callers, future
   acceptance callers) already hold. It lives on a separate, narrow
   contract surface no such consumer is ever given a reference to.
   Permission Engine authorisation remains mandatory throughout, but is
   not the sole structural protection.
4. **A single, minimal, durable audit mechanism satisfies the
   constitutional word "audited."** No general-purpose audit subsystem
   and no `AuditService` implementation is authorised or required by
   Phase 7.
5. **A successful deletion can never exist without durable evidence that
   it was authorised**, closed against process termination, not merely
   against an in-process thrown exception.

---

## 2. Smallest Constitutional Audit Mechanism

`src/interfaces/AuditService.kt` exists but has zero implementations
anywhere in this repository — no `Default*`/`InMemory*` class, no
defined persistence technology, no caller. Building its first
implementation as a side effect of Phase 7 would mean designing general,
platform-wide audit infrastructure — a decision disproportionate to,
and not authorised by, anything reviewed for this clarification.

**Determination:** one narrow interface, `EvidenceDeletionAudit`, with
exactly one method, and one production implementation,
`FileSystemEvidenceDeletionAudit`, reusing the fsync-on-write,
append-only discipline `FileSystemEvidenceArtifactStorage` already
established. No query capability, no general record taxonomy, and no
second, in-memory production implementation — durability is this port's
entire purpose, so an in-memory implementation would defeat it. A
test-only `FakeEvidenceDeletionAudit` (in `tests/runtime/`, following
this repository's existing `Fake*` convention) is sufficient for
exercising the deletion authority's own gating logic without real disk
I/O.

Whether this narrow port is later subsumed into a future, properly
governed, platform-wide `AuditService` is explicitly deferred and not
decided here.

---

## 3. Why No Coordinator

"Coordinator" in this repository's own vocabulary
(`EvidenceRegistrationCoordinator`, `GoalPlanningHandoffCoordinator`,
`ReplyDeliveryCoordinator`, `ConversationReplyCoordinator`) names one
specific, recurring shape: a runtime-layer class sequencing calls
**across two or more independently-governed subsystems**, each with its
own Contract Design and Scope Lock. `EvidenceRegistrationCoordinator`
earns that name because it sequences Evidence Custodian *and* Memory
Core — two separate constitutional domains that must never call each
other directly.

Phase 7, per Determination 1, has no second domain. Deletion sequences
exactly: a Permission Engine check, and calls to two of its own
dependencies (`EvidenceArtifactStorage`, and the narrow
`EvidenceDeletionAudit` port serving the Custodian's own frozen
obligation, not an independently governed subsystem). This is the same
shape `DefaultEvidenceCustodian.accept` already has today — a permission
check, then a call to its own `storage` dependency — and nothing calls
that a coordinator.

**Conclusion:** no coordinator is architecturally justified. A single,
direct implementation, named consistently with every other `Default*`
class in this repository, satisfies every requirement in Section 1.

---

## 4. Contract Surfaces

- **`EvidenceCustodian` is unchanged.** No `delete` method, no new
  dependency, no modification of any kind to
  `src/runtime/DefaultEvidenceCustodian.kt`.
- **New interface, `OwnerEvidenceDeletionAuthority`** — one operation,
  `deleteAsOwner(requestingPrincipalId, evidenceArtifactId):
  EvidenceDeletionResult`. A structurally separate capability from
  `EvidenceCustodian`; nothing holding only an `EvidenceCustodian`
  reference can reach it.
- **New sealed result, `EvidenceDeletionResult`** — `Deleted`,
  `NotFound`, `Rejected(reason)`. No `reason`/`justification` parameter
  anywhere in `deleteAsOwner`'s own signature — there is no code path by
  which a caller can label a deletion "optimisation" and have it treated
  differently from an ordinary request.
- **New class, `DefaultOwnerEvidenceDeletionAuthority`** — the sole
  implementation of `OwnerEvidenceDeletionAuthority`. Exactly three
  dependencies: `EvidenceArtifactStorage`, `PermissionEngine`,
  `EvidenceDeletionAudit`. No `EvidenceCustodian` reference and no
  `MemoryCore` reference — structurally absent, not merely unused,
  mirroring `EvidenceRegistrationCoordinator`'s own "absence of any
  other constructor parameter is itself the structural guarantee"
  convention.
- **New interface, `EvidenceDeletionAudit`** — one method,
  `record(record: EvidenceDeletionAuditRecord)`.
- **New enum, `EvidenceDeletionAuditStage`** — exactly two values,
  `AUTHORISED` and `COMPLETED`. No third value. No `FAILED`,
  `REQUESTED`, `NOT_FOUND`, or any other stage is introduced.
- **New record, `EvidenceDeletionAuditRecord`** — one flat, non-sealed
  type: `deletionRequestId` (minted once per `deleteAsOwner` call,
  correlating the two appends of one attempt), `evidenceArtifactId`,
  `requestingPrincipalId`, `recordedAt`, `stage: EvidenceDeletionAuditStage`.
  Exactly one record type exists; the `stage` field, not a second type,
  distinguishes the two points in one attempt's lifecycle at which it is
  durably written.
- **`EvidenceArtifactStorage` gains one new primitive**,
  `delete(evidenceArtifactId): Boolean` — ungated, exactly like `write`/`read`;
  physically removes content, no tombstone.

---

## 5. The Authorised Caller and the Owner Deletion Request Boundary

`OwnerEvidenceDeletionAuthority` is not wired into anything by this
document — no `ParkerRuntime` change is proposed or authorised here.
Phase 7's obligation is to make the type narrow enough that Runtime
Integration (Phase 10) has no choice but to wire it narrowly: only
whatever component that later, separately governed unit designates as
the owner-facing entry point may ever hold a reference to it — never
`ConversationReplyCoordinator`, `GoalPlanningHandoffCoordinator`,
`PlannerRuntime`, or anything reachable from a reasoning provider's own
proposal. Permission Engine authorisation (Section 6) remains mandatory
regardless of this structural narrowing; one is not a substitute for the
other, per Determination 3.

---

## 6. Permission Evaluation Order and Audit Ordering

Sequence, in order, for every `deleteAsOwner` call:

1. **Permission Engine evaluation**, first — mirroring `accept`/`retrieve`
   exactly.
2. **Not approved** → return `Rejected`. No storage access, no audit
   write, no side effect of any kind.
3. **Approved** → mint `deletionRequestId`; durably write
   `EvidenceDeletionAuditRecord(deletionRequestId, evidenceArtifactId,
   requestingPrincipalId, recordedAt, stage = AUTHORISED)`. **This write
   is a hard precondition of physical deletion.** If it fails,
   `EvidenceArtifactStorage.delete` is never called and `deleteAsOwner`
   fails with a thrown fault. This is what closes the crash-window
   failure mode Determination 5 requires closed: by the time physical
   deletion could possibly be attempted, durable evidence that it was
   authorised already, unconditionally, exists — not merely once an
   in-process exception handler has a chance to run.
4. Call `EvidenceArtifactStorage.delete(evidenceArtifactId)`.
5. **Nothing was present** → return `NotFound`. No second audit write —
   only the `AUTHORISED` record exists for this attempt, honestly
   reflecting "authorised, attempted, nothing there."
6. **Deletion succeeded** → durably write a second
   `EvidenceDeletionAuditRecord`, same `deletionRequestId`, `stage =
   COMPLETED`. Only once this second write itself durably succeeds does
   `deleteAsOwner` return `Deleted`.

The single `EvidenceDeletionAuditRecord` type is written twice per
successful deletion — once before the irreversible physical action,
once after — distinguished only by its `stage` field. No second type,
and no additional stage value, is introduced.

---

## 7. Failure Semantics

- **Permission denied:** `Rejected`. No side effect of any kind.
- **`AUTHORISED`-record persistence fails:** the entire `deleteAsOwner`
  call fails with a thrown fault. `EvidenceArtifactStorage.delete` is
  never reached. No deletion occurs without durable evidence of
  authorisation ever having existed.
- **Nothing present:** `NotFound`. Only the `AUTHORISED` record exists
  for this attempt; no `COMPLETED` record is written, since nothing
  completed.
- **`EvidenceArtifactStorage.delete` throws a genuine I/O fault:**
  propagates to the caller unchanged. Only the `AUTHORISED` record
  exists for this attempt — an honest reflection of an authorised,
  attempted, but unsuccessful deletion.
- **Deletion succeeds, but the `COMPLETED`-record write then fails:**
  the underlying bytes are already gone and this cannot be undone
  (Section 8). The write failure propagates to the caller as a thrown
  fault, and `deleteAsOwner` never returns `Deleted` in this case. This
  is the one disclosed, irreducible residual case — but it is
  categorically smaller than the failure mode this clarification exists
  to close: durable evidence that the deletion was *authorised* already
  exists (written in step 3, before the irreversible action), so a
  successful deletion can never exist with zero durable trace of
  authorisation. What may be missing in this narrow residual case is
  durable confirmation of definite completion, not evidence of
  authorisation.

---

## 8. Atomicity / Compensation Rule

No rollback or compensation mechanism exists for any failure mode above
— there is no "restore a deleted original" capability, and building one
is not authorised by anything reviewed for this clarification. Atomicity
is achieved by definition and by ordering, not by cross-resource
transaction: `deleteAsOwner` defines "successful deletion" as "physical
removal *and* durable `COMPLETED` audit both confirmed" — so the
caller-visible contract never permits reporting `Deleted` without a
durable `COMPLETED` record behind it, and never permits physical
deletion to proceed without a durable `AUTHORISED` record already having
been confirmed ahead of it.

---

## 9. Boundary Between Phase 7 and Phase 8

Phase 7 ships the structurally separated capability and its minimal,
two-stage audit trail. Phase 8 verifies and enforces, without adding new
production capability:

- Extend `EvidenceCustodianScopeTest.kt`-style verification to confirm
  `EvidenceCustodian` still declares only `accept`/`retrieve`, and that
  `OwnerEvidenceDeletionAuthority` remains structurally separate, with
  no `MemoryCore`-reachable dependency anywhere in
  `DefaultOwnerEvidenceDeletionAuthority`.
- Confirm no code path anywhere invokes `deleteAsOwner` for any purpose
  other than a genuine, permission-evaluated owner request.
- Confirm nothing in the eventually-wired production graph (Phase 10)
  gives a reasoning-provider- or Evidence-Intelligence-reachable
  component a usable reference to `OwnerEvidenceDeletionAuthority` — the
  one part of this verification Phase 7 cannot fully close on its own,
  since the production graph does not exist until Phase 10.

---

## 10. How This Clarification Prevents Each Named Failure Mode

| Must prevent | Mechanism |
| --- | --- |
| Reasoning-provider deletion | `OwnerEvidenceDeletionAuthority` is a separate type from `EvidenceCustodian`; nothing reasoning-reachable is ever given a reference to it |
| Ordinary subsystem deletion | `EvidenceCustodian` gains no delete method |
| Optimisation-motivated deletion | No `reason` parameter exists anywhere in `deleteAsOwner`'s signature |
| Deletion without durable audit | `EvidenceArtifactStorage.delete` cannot be called until a durable `AUTHORISED` record is confirmed; `deleteAsOwner` cannot return `Deleted` unless a durable `COMPLETED` record is confirmed |
| Memory Core calls from inside Evidence Custodian | `DefaultOwnerEvidenceDeletionAuthority`'s constructor holds no `MemoryCore` reference |
| Retention of original bytes after successful deletion | `EvidenceArtifactStorage.delete` physically removes content; no tombstone anywhere |

---

## 11. Proposed Implementation File List

**Production:**
- `src/interfaces/EvidenceCustodian.kt` — add `OwnerEvidenceDeletionAuthority`, `EvidenceDeletionResult`. `EvidenceCustodian` itself unchanged.
- `src/interfaces/EvidenceArtifactStorage.kt` — add `delete`.
- New file: `src/interfaces/EvidenceDeletionAudit.kt` — `EvidenceDeletionAudit`, `EvidenceDeletionAuditStage`, `EvidenceDeletionAuditRecord`.
- New file: `src/runtime/DefaultOwnerEvidenceDeletionAuthority.kt`.
- New file: `src/runtime/FileSystemEvidenceDeletionAudit.kt`.
- `src/runtime/FileSystemEvidenceArtifactStorage.kt` — implement `delete`.
- `src/runtime/InMemoryEvidenceArtifactStorage.kt` — implement `delete`.
- `src/runtime/DefaultEvidenceCustodian.kt` — **no change.**

**Tests:**
- `tests/contracts/EvidenceCustodianScopeTest.kt` — extend to assert `EvidenceCustodian` still declares only `accept`/`retrieve`, and that `OwnerEvidenceDeletionAuthority` is a distinct type not implemented by `DefaultEvidenceCustodian`.
- `tests/runtime/FileSystemEvidenceArtifactStorageTest.kt`, `tests/runtime/InMemoryEvidenceArtifactStorageTest.kt` — add `delete` cases.
- New file: `tests/runtime/FakeEvidenceDeletionAudit.kt` — test fixture.
- New file: `tests/runtime/DefaultOwnerEvidenceDeletionAuthorityTest.kt` — denied (no side effect), not-found (`AUTHORISED` record only), storage failure (propagates, `AUTHORISED` record only), success (`AUTHORISED` then `COMPLETED`, `Deleted` returned), `AUTHORISED`-write failure (blocks physical deletion entirely), `COMPLETED`-write failure after successful physical deletion (fault propagates, `Deleted` never returned), no-`MemoryCore`/no-`EvidenceCustodian` dependency confirmed by constructor shape.
- New file: `tests/runtime/FileSystemEvidenceDeletionAuditTest.kt` — durability across a real temp directory, append-only behaviour, both stages independently retrievable by `deletionRequestId`.

**Not touched:** `EvidenceRegistrationCoordinator.kt` and its tests; `MemoryCore.kt`; CDR-006; the Contract Design; the Scope Lock; the Implementation Plan; the Final Freeze Review; `ParkerRuntime.kt`; `README.md`; `CHANGELOG.md`.

---

## Final Recommendation

This Boundary Clarification is accepted and final. It authorises Phase 7
implementation to proceed against the contract shape, ordering, and file
list fixed in Sections 4–7 and 11 above. It does not itself write any
Kotlin, does not itself begin implementation, and does not alter any
decision fixed by CDR-006, the Contract Design, the Scope Lock, or the
Implementation Plan.

EVIDENCE CUSTODIAN PHASE 7 BOUNDARY CLARIFICATION — ACCEPTED — FINAL

Confirmed: no Kotlin implemented; no test modified; CDR-006, the
Evidence Artifact Contract Design, the Evidence Custodian Scope Lock,
the Evidence Custodian Implementation Plan, and the CDR-006 Final Freeze
Review all unmodified by this document; nothing staged; nothing
committed; nothing pushed. Verification, staging, commit, and push
remain Steven's own responsibility, to be completed before Phase 7
implementation begins.
