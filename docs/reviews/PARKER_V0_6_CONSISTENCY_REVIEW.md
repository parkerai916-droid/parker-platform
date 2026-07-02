# Parker v0.6 Consistency Review

Reviewed on `feature/phase-1-core-contracts`. No implementation code was
written or changed to produce this review; only this document is new.

## 1. Executive summary

Phase 1 (Volume 1 Core Contracts) is internally consistent after the last
three rounds of fixes: `ExecutionRequest`, `Principal`, and their supporting
types now agree across prose, JSON Schema, and Kotlin. That work is safe to
review as a closed unit.

**Phase 2 is not safe to start yet**, for a reason bigger than any single
schema mismatch: **there is no `ToolRegistry` interface anywhere in this
repository** — not in Volume 3, not in `src/interfaces`, not in any chapter
or ADR. `IMPLEMENTATION_ORDER.md` requires "Tool Framework" in Phase 2, and
`Chapter 12` describes tools conceptually, but nothing specifies how
`ExecutionPipeline.submit` is supposed to find a `Tool` to dispatch to. This
is the same category of gap as the already-known missing `IdentityService`,
just undiscovered until this pass. Two smaller but real blockers sit next
to it: `EventBus`'s supporting types are almost entirely unspecified, and
nothing defines how `ExecutionRequest.proposedActions` (free text) becomes
a `PermissionDecision.action` (a strict enum) for the Permission Engine to
evaluate.

Beyond those, this pass found one more dangling ADR citation (`ADR-004` in
`Agent.md`, sibling to the `ADR-005` one already fixed in `EventBus.md`),
confirmed the previously-flagged `Resource.sensitivity` Kotlin/schema gap
is still open, and found that the top-level `README.md`/`CHANGELOG.md`
still describe the repository as "v0.4 working draft" despite seven
commits of v0.6 engineering spec work already merged on `main`.

## 2. Critical blockers (block Phase 2)

### 2.1 No `ToolRegistry` interface exists

Searched `docs/architecture`, `docs/adr`, `docs/specifications`, and
`src/interfaces` for "ToolRegistry" — zero matches anywhere. `ResourceRegistry`
has a full Volume 3 interface, a Chapter 8 write-up, and a Kotlin stub;
`Tool` (the individual tool contract) has the same treatment; there is
nothing analogous for *discovering or resolving* tools. `IMPLEMENTATION_ORDER.md`
Phase 2 explicitly requires "Tool Framework," and `ExecutionPipeline.submit`
cannot dispatch to a tool it has no way to look up. This blocks any
concrete `ExecutionPipeline` or `Tool` implementation, not just a
documentation nicety.

### 2.2 `EventBus`'s supporting types are almost entirely unspecified

`EventBus.publish(event: ParkerEvent): PublishResult` and
`subscribe(eventType: EventType, handler: EventHandler): Subscription`
need four types with no specification: `EventType` (should it be an enum?
a string? open or closed?), `EventHandler` (a functional type — what's its
signature?), `Subscription` (does it need a `close()`/`cancel()`?), and
`PublishResult` (success/failure? does it carry a reason?). Only
`ParkerEvent`/`Event` itself has a real JSON Schema
(`docs/schemas/Event.schema.json`) — and even that has no corresponding
Kotlin type yet. `EventBus` cannot be implemented from what exists today.

### 2.3 No mapping from `ExecutionRequest.proposedActions` to `PermissionDecision.action`

`ExecutionRequest.proposedActions` is `List<String>` — free text. The
Permission Engine's actual decision object, `PermissionDecision.action`, is
a closed 10-value enum (`READ, WRITE, DELETE, EXECUTE, EXPORT, SHARE,
CONTROL, NOTIFY, SCHEDULE, SEND_EXTERNAL`). Nothing in Volume 1, Volume 3,
or any ADR states how a request's free-text proposed actions get
translated into the enum value(s) the Permission Engine actually evaluates
against a Resource. Without this, `PermissionEngine.evaluate(request):
PermissionDecision` cannot be implemented deterministically — it's the same
concern raised in the reconciliation report (§8, item 5) but reconfirmed
here as a hard blocker, not just an open question.

### 2.4 `IdentityService` still has no interface

Already known and explicitly deferred in `IMPLEMENTATION_GAPS.md` #1 — repeated
here only because `IMPLEMENTATION_ORDER.md` Phase 1 (the *build-order* Phase 1,
not this task's *contracts* Phase 1 — see §2.5) cannot be considered fully
implementable without it. Not a new finding; listed for completeness of
this review's "Phase 2 readiness" judgment, since Identity underlies
Permission evaluation for every principal type.

### 2.5 "Phase 1" scope conflict, restated

`IMPLEMENTATION_ORDER.md`'s "Phase 1" means three running systems (Resource
Registry, Identity Service, Permission Engine). This project's "Phase 1"
(across the last several tasks) has meant "Volume 1 Core Contracts plus
directly-dependent interfaces" — a different, narrower slice that doesn't
map cleanly onto either `IMPLEMENTATION_ORDER.md`'s Phase 1 or Phase 2.
This was flagged in `docs/architecture/phase1-assessment.md` when first
noticed; restated here because it directly affects what "safe to start
Phase 2" even means — Phase 2 by *this task's* numbering is not the same
scope as `IMPLEMENTATION_ORDER.md` Phase 2, and the two should be
explicitly reconciled (or one abandoned) before further work is planned
against either.

## 3. Non-blocking inconsistencies

1. **`ADR-004` dangling citation in `Agent.md`.** Same defect as the
   already-fixed `ADR-005` in `EventBus.md` — `docs/adr/` has no `ADR-004`
   file (numbering jumps 003→006), but `Agent.md`'s Related section still
   cites it. Not fixed in this pass per this task's "review only" scope.
2. **`Permission.schema.json` vs `PermissionDecision.schema.json` still
   disagree.** Confirmed still present: different `action` enums (one has
   `SEND_EXTERNAL`, the other doesn't) and `Permission.schema.json` has
   `reason`/`requiresConfirmation` properties the other lacks. Recorded as
   a human decision in `IMPLEMENTATION_GAPS.md` #8; still unresolved.
3. **`ExecutionResult.schema.json` is missing `toolResults` and
   `reflectionCandidate`**, both required by the prose `ExecutionResult`
   Contract. Documented in `ExecutionResult-Schema.md` when Volume 2 was
   filled in; not fixed, since only `ExecutionRequest.schema.json` was
   explicitly in scope for a schema fix at the time.
4. **`Resource.md` prose requires `createdAt`, `updatedAt`, `source`, and
   `metadata`; `Resource.schema.json` defines none of them.** Same shape of
   gap as the (now-fixed) `Principal.schema.json` issue, documented in
   `Resource-Schema.md`, not yet fixed.
5. **`src/contracts/Resource.kt` types `sensitivity` as a plain `String`;
   `Resource.schema.json` defines a concrete 9-value enum**
   (`PUBLIC, PERSONAL, HOUSEHOLD, FINANCIAL, MEDICAL, LEGAL,
   SECURITY_SENSITIVE, CREDENTIALS_SECRETS, THIRD_PARTY_PERSONAL_DATA`).
   Recorded in `IMPLEMENTATION_GAPS.md` #10 as a recommended follow-up;
   Kotlin not changed (would be a code fix, out of scope for a docs pass).
6. **Volume 3's original 12 interface specs have no `## Status`/version
   header at all** (`ExecutionPipeline.md`, `PermissionEngine.md`,
   `ResourceRegistry.md`, `Tool.md`, `EventBus.md`, `Agent.md`, `Plugin.md`,
   `MemoryStore.md`, `WorldModel.md`, `ModelManager.md`,
   `NotificationService.md`, `AuditService.md`) — unlike Volume 1's
   contracts, Volume 2's schemas, and the 5 new supporting-type docs added
   this cycle, all of which stamp a version/status. `VOLUME_3_INDEX.md`
   asserts these interfaces are "normative for v0.6 engineering work" as a
   set, but individually nothing marks *which* revision of each is current
   — a minor but real instance of "placeholder reading as final" (checklist
   item 8): there's no way to tell, from the file itself, whether e.g.
   `Tool.md` has been revisited since v0.4 or is unchanged since then.
7. **`README.md` and `CHANGELOG.md` still describe the repository as "v0.4
   working draft."** `git log` shows seven more commits after that
   ("Foundation documents for v0.6" through "Engineering Spec v0.6 - Volume
   3") with no corresponding README/CHANGELOG update. Anyone reading only
   the README would not know Volumes 1–3 exist.
8. **Terminology near-miss, not actual drift:** `RequestOrigin.AGENT`
   (where a request came from) and `PrincipalType.INTERNAL_AGENT` (what
   kind of principal is acting) both use "agent" for related but distinct
   concepts. Likely intentional (an internal agent *is* a valid request
   origin), but worth a one-line clarifying note somewhere since it reads
   like the `AGENT`/`INTERNAL_AGENT` drift already found and fixed in
   `Principal.schema.json`.
9. **`src/README.md` / `tests/README.md`** are one-line "placeholder for
   future" stubs — honestly labeled as such, not a concern, listed only
   for completeness against checklist item 8.
10. **Session, Task, and Workflow lifecycle enums have no diagrams and no
    transition validators** — the same category of gap identified and
    partially addressed for `Principal`/`Resource` (diagrams added,
    validators deferred), but these three schemas are outside Phase 1
    scope entirely and remain fully unaddressed. Not blocking now; will be
    when whatever phase implements Session/Task/Workflow begins.

## 4. Recommended fixes

In rough priority order:

1. Decide and document how `ExecutionRequest.proposedActions` maps to
   `PermissionDecision.action` (§2.3) — this blocks PermissionEngine work
   more directly than any missing type does.
2. Add a `ToolRegistry` interface to Volume 3 (§2.1) — as a specification
   change first, matching how `ResourceRegistry` was done, before any
   Kotlin is written against it.
3. Specify `EventType`, `EventHandler`, `Subscription`, `PublishResult`
   (§2.2) and add a Kotlin `ParkerEvent` type from the existing
   `Event.schema.json`.
4. Fix the `ADR-004` citation in `Agent.md` the same way `ADR-005` was
   fixed in `EventBus.md` (§3.1) — same fix, five-minute job.
5. Resolve the `Permission.schema.json`/`PermissionDecision.schema.json`
   duplication (§3.2) — needs a human decision on which file survives.
6. Add `toolResults`/`reflectionCandidate` to `ExecutionResult.schema.json`
   and `createdAt`/`updatedAt`/`source`/`metadata` to `Resource.schema.json`
   (§3.3, §3.4) — same pattern as the `ExecutionRequest`/`Principal` fixes
   already done.
7. Change `src/contracts/Resource.kt`'s `sensitivity: String` to the real
   9-value enum (§3.5) — a small, contained Kotlin change once someone
   confirms it's wanted.
8. Add a version/status stamp to the 12 un-stamped Volume 3 interface docs
   (§3.6), and update `README.md`/`CHANGELOG.md` to reflect that v0.6 work
   is underway (§3.7).

## 5. Files affected (by this review only)

- `docs/reviews/PARKER_V0_6_CONSISTENCY_REVIEW.md` (new — this document).

No other file was modified to produce this review, per its scope.

## 6. Is Phase 1 safe to review?

**Yes.** `ExecutionRequest`, `Principal`, `Resource`, `PermissionDecision`,
`ExecutionResult`, and the execution lifecycle state machine are now
mutually consistent across prose, schema, and Kotlin (with the two
specific, documented exceptions in §3.4/§3.5, both non-blocking and
already tracked in `IMPLEMENTATION_GAPS.md`). Phase 1 as scoped
(contracts + interfaces, no concrete engines) is a coherent, reviewable
unit.

## 7. Is Phase 2 safe to start?

**No.** §2.1–§2.3 are not documentation nits — they're missing pieces
`ExecutionPipeline`, `Tool`, `EventBus`, and `PermissionEngine`
implementations would need to exist before any of those interfaces could
be implemented correctly. Starting Phase 2 Kotlin work now would mean
inventing a `ToolRegistry` shape, an `EventType`/`EventHandler` shape, and
an action-mapping rule under implementation pressure rather than as a
reviewed specification decision — exactly what Phase 7's "do not invent
missing architecture" rule exists to prevent.

## 8. Go/no-go recommendation

**No-go on Phase 2.** Close §2.1–§2.3 as specification work first (Volume
3 additions + one ADR-level decision on action-mapping), the same way
`IdentityService` is already correctly parked as a deferred decision rather
than an invented stub. Phase 1 itself does not need to be redone — it's
in good shape and the fixes in §4 are small, contained, and independent of
the Phase 2 blockers.
