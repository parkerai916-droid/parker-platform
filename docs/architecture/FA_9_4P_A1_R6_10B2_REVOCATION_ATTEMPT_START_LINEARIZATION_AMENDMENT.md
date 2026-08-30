# FA.9.4P-A1E-R6.10B2 — Revocation/attempt-start linearization

## 1. Status and precedence

This amendment supplements the R6.10B ordinary external region-ingestion architecture and
supersedes only the concurrent revocation/attempt-start portion of R6.10B1. All unaffected R6.10B
and R6.10B1 rules remain governing. This is a design decision only; it changes no runtime or
production state.

## 2. Confirmed race

R6.10B1 required a final revocation check followed by durable `PROVIDER_ATTEMPT_STARTED`, but those
operations were independently synchronized. Revocation could commit after the check and before the
ledger transition, allowing transport after a pre-attempt revocation. The correction is a shared,
authorization-scoped serialization guard, not a distributed transaction or multi-store commit.

## 3. Authorization-scoped guard

Every authorization ID deterministically maps to one filesystem-backed lock file within the
authorization store. `FileChannel.lock()` provides an exclusive inter-process guard on Linux. The
lock file is not evidence and need not survive a process crash; durable authorization, revocation,
reservation and attempt-ledger records decide recovery.

The authorization store exposes a bounded operation equivalent to:

```text
withExclusiveAuthorizationGuard(authorizationId) { guardedView -> ... }
```

All writers for the same authorization—initial reservation, conflicting reservation checks,
revocation, and provider-attempt linearization—must use it. Unrelated authorization IDs use
different lock files. Paths use a validated filesystem-safe digest of the governed ID.

Mandatory lock order is:

1. authorization-scoped guard;
2. attempt-ledger execution lock/store operation;
3. any later provider-state operation after the authorization guard is released.

No code may acquire the authorization guard while holding the attempt-ledger or provider-state
lock. Provider transport always occurs after releasing the guard.

## 4. Linearization protocol

For authorization A reserved to execution E, the ordinary coordinator holds A's guard continuously
while it:

1. loads and verifies A and its create-once reservation;
2. verifies owner, evidence ID/SHA, capability, provider, purpose, execution, request and attempt;
3. checks that no pre-attempt revocation exists;
4. reads E's ledger and rejects an existing attempt-start/later state under no-retry rules;
5. durably transitions E from `REQUEST_PREPARED` to `PROVIDER_ATTEMPT_STARTED`;
6. re-reads/accepts the successful durable transition.

That successful ledger write is the linearization point and canonical “egress may have begun” fact.
The guard is then released and only then may transport execute. Authorization-side consumed state
remains an optional idempotent projection.

Revocation of A obtains the same guard, then inspects the exact reserved execution's ledger. With no
attempt-start it durably writes pre-attempt revocation and wins. With attempt-start or later, the
attempt won; revocation is recorded as post-attempt and cannot undo egress or permit another call.

### Winner semantics

- **Revocation wins:** its durable record commits while holding the guard. Later execution observes
  it and cannot write attempt-start. Provider calls: zero.
- **Attempt-start wins:** the ledger transition commits while execution holds the guard. Later
  revocation observes it and is post-attempt. The first call remains authorized; no second call is
  permitted.

There is one total order per authorization and no state where both operations predate the other.

## 5. Coordinator boundary change

Current `GovernedRegionTranscriptionExecutionCoordinator.execute` advances pre-attempt stages,
writes attempt-start, and invokes `mechanism.transcribe` in one method. It must be narrowly split or
extended without duplicating stage logic:

- `prepare(binding)` performs binding checks, provider-state recovery check, and idempotent
  pre-attempt advancement through `REQUEST_PREPARED`, returning a bounded prepared token bound to
  the full execution/request identity;
- `startPrepared(prepared, guardedStart)` invokes the caller-supplied guarded transition boundary;
  that boundary holds the authorization guard while revalidating and calling the ledger's single
  existing attempt-start transition;
- after the guarded transition returns success and the guard is released, the coordinator invokes
  the existing mechanism once and retains its existing provider-state/recovery behavior.

An equivalent callback shape is acceptable, but the ordinary coordinator must be able to hold the
authorization guard through the successful durable ledger transition. The token is unforgeable in
scope, identity checked, and cannot bypass the existing started-attempt guard. Acceptance-lane callers
may retain a compatibility `execute` wrapper using a no-authorization guarded-start implementation;
ordinary execution uses the explicit guarded seam.

## 6. Crash and concurrency matrix

| Case | Durable result | Recovery and egress rule |
|---|---|---|
| Crash before guard | no new state | same execution resumes normally |
| Crash under guard before attempt-start | reservation/pre-attempt state only; OS releases lock | same execution may resume unless revocation later wins |
| Crash after durable attempt-start while guard held | attempt-start exists; OS releases lock | effectively consumed; no automatic retry |
| Crash after guard release before transport | attempt-start exists | egress classified may-have-occurred; no retry |
| Crash during/after transport before response persistence | attempt-start exists | unknown outcome; no retry |
| Concurrent revocation gets guard first | revocation durable, no attempt-start | execution denied; zero transport |
| Execution gets guard and commits start first | attempt-start durable | later revocation is post-attempt; at most first transport |
| Execution gets guard after prior revocation | revocation observed | no attempt-start and no transport |

Unresolved safety states: **0**.

## 7. Expiry, reservation, and downstream recovery

R6.10B1 expiry remains unchanged: expiry is checked before initial reservation, while the same
validly reserved execution survives later wall-clock expiry. Expiry therefore does not race for the
attempt-start linearization point and never releases authorization to another execution.

Reservation and conflicting reservation writers use the same authorization guard. The identical
execution reservation is idempotent; a second execution or changed binding fails closed.

After attempt-start, revocation never permits further egress. If provider response state is durable,
the existing provider-state-first recovery path may parse, validate, reconstruct, admit and present
locally without holding the authorization guard. If no response is durable, outcome is unknown and
automatic retry remains prohibited—even when transport did not actually begin after the conservative
attempt-start marker.

## 8. Static implementation trace

The future authorization store can use the same Linux `FileChannel.lock()` convention already used
by `FileSystemFidelityFirstAttemptLedger`, with a separate lock per authorization. The ordinary
coordinator acquires that guard first. Its guarded callback calls the existing ledger transition,
which acquires the execution lock second, atomically replaces/fsyncs the ledger, and returns. The
ordinary coordinator releases the authorization guard before calling `RegionExternalTranscriptionMechanism`.

Revocation follows the same authorization-then-ledger order. Provider-state persistence remains
inside the adapter after transport and therefore never precedes the authorization guard. Existing
raw-response-first persistence and provider-state-first restart recovery remain unchanged.

The required runtime refactor is bounded to exposing the pre-attempt/guarded-start seam in
`GovernedRegionTranscriptionExecutionCoordinator`; it does not require a broader routing,
transcription, storage, or distributed-transaction redesign.

## 9. R6.10C readiness

R6.10C shall implement the shared inter-process authorization guard, enforce the mandatory lock
order in reservation/revocation/attempt-start paths, and test both winner schedules plus every crash
case above. It must prove zero transport when revocation wins, at most one transport when attempt
wins, guard release before transport, and no automatic retry after the durable marker.

The concurrency race is closed and R6.10C is ready to implement under R6.10B, R6.10B1, and this
amendment.
