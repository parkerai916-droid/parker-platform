# FA.9.4P-A1E-R6.10B1 — Owner authorization/provider-attempt crash recovery

## 1. Status and supersession

This amendment corrects only the authorization consumption, expiry, revocation, and crash-recovery
semantics in
`FA_9_4P_A1_R6_10B_ORDINARY_EXTERNAL_REGION_INGESTION_ARCHITECTURE.md`. R6.10B remains governing
for capability acceptance, routing, rendering, region geometry, provider-state persistence,
reconstruction, derivatives, admission, runtime composition, security, and owner workflow.

The amendment is architecture only. It changes no runtime or production state.

## 2. Confirmed defect

R6.10B placed an irreversible owner-authorization consumption marker before the existing
`GovernedRegionTranscriptionExecutionCoordinator` durably writes `PROVIDER_ATTEMPT_STARTED`. Those
facts live in separate stores and have no transaction. A crash between them would leave transport
definitely unstarted while the authorization was unusable, stranding the exact authorized
execution. Treating that state as retryable by another execution would instead create duplicate
egress entitlement.

The correction is not a distributed transaction. Reservation binds authorization to one execution;
the attempt ledger remains authoritative for whether transmission may have started.

## 3. Durable and effective states

The authorization store has these durable states:

1. `AVAILABLE`: valid grant, not reserved, revoked, or expired. It may be reserved once.
2. `RESERVED_FOR_EXECUTION`: create-once reservation binds authorization ID, owner, evidence ID,
   source SHA-256, capability digest, purpose, provider, exact execution ID, request digest, and
   attempt ID. Repeating the identical reservation is idempotent. Any differing binding conflicts.
3. optional `CONSUMED` projection: an idempotent audit/reconciliation marker. It is never the
   canonical provider-safety fact.

Effective state is derived from the reservation and the exact execution's durable attempt ledger:

- no `PROVIDER_ATTEMPT_STARTED` or later provider stage: `RESERVED_FOR_EXECUTION`;
- `PROVIDER_ATTEMPT_STARTED` or any later provider stage: `CONSUMED_BY_PROVIDER_ATTEMPT`.

The optional authorization-side consumed marker may be written after observing the ledger and may
be repaired idempotently after restart. Its absence cannot make a started attempt retryable, and its
presence must name the same authorization, execution, request and attempt.

## 4. Reservation, expiry, and revocation

Initial reservation checks owner, evidence, SHA, capability, purpose, provider, disclosure,
revocation, and expiry. It uses create-once/fsync storage. The same complete binding can be loaded or
reserved again by the same execution; a different execution or changed binding is denied.

Expiry is checked before initial reservation. Expiry after a valid reservation does not revoke the
bound execution and never frees the authorization for another execution. The same execution may
resume under its durable stage. Expiry never permits another provider attempt after attempt-start.

Revocation is append-only:

- while `AVAILABLE`, it prevents reservation;
- while reserved but before `PROVIDER_ATTEMPT_STARTED`, it makes the execution egress-ineligible;
  local preflight artifacts remain auditable, but transport is forbidden;
- after `PROVIDER_ATTEMPT_STARTED`, it cannot undo possible transmission, permit retry, or authorize
  new egress. If a response is durable, parsing, validation, reconstruction, admission and owner
  presentation may continue because they are local-only. If no response is durable, the outcome
  remains unknown and non-retriable automatically.

The final reservation-validity check immediately before attempt-start reads the reservation,
revocation record, exact execution identity and ledger. It permits only the same reserved execution
with no attempt-start fact. The ledger transition is then forced before transport.

## 5. Attempt-ledger authority and retry boundary

`FileSystemFidelityFirstAttemptLedger` answers “could external transmission have begun?” Its
checksum-protected sequence and single `PROVIDER_ATTEMPT_STARTED` stage are canonical. The
authorization store answers “was owner authority granted, and to which exact execution?” These
facts are complementary.

No transport occurs before a forced `PROVIDER_ATTEMPT_STARTED`. Once that stage exists, the existing
coordinator's `providerAttemptStarted` guard blocks another transport when no response exists; an
existing provider response is recovered before that guard and causes local-only resumption.

A consumed authorization never authorizes another attempt. A deliberate later attempt requires a
new explicit authorization, new execution/attempt identity, and an explicit subsequent-attempt
governance action referencing the prior attempt. Until such a feature exists, Parker reports that a
new governed retry action is required. There is no automatic retry.

## 6. Corrected coordinator sequence

The future ordinary coordinator shall execute:

1. validate `AVAILABLE` authorization and reserve it to the exact deterministic execution;
2. perform ordinary acquisition preflight and authoritative source resolution;
3. render pages and derive regions deterministically;
4. construct the request and persist through `REQUEST_PREPARED` idempotently;
5. re-read the reservation, revocation state, request digest and attempt ledger;
6. if still reserved and unstarted, force `PROVIDER_ATTEMPT_STARTED`;
7. derive effective authorization state as consumed;
8. invoke transport once;
9. persist raw provider response before parsing and persist the response ledger stage;
10. resume parsing, validation, reconstruction and admission locally.

Implementation shall expose the final reservation check at the ordinary coordinator boundary and
then use the existing ledger transition/transport ordering. It need not atomically update the
authorization store. A crash before attempt-start is resumable by the same execution. A crash after
attempt-start is never automatically externally resumable.

## 7. Crash/restart matrix

| Crash point | Authorization durable state | Attempt stage | Transport fact | Same execution | Provider transport permitted | Local downstream | New authorization |
|---|---|---|---|---|---|---|---|
| Before reservation | `AVAILABLE` | none | definitely not started | may reserve if still valid | yes, after reservation/preflight | no response work | no |
| After reservation, before preflight | reserved to E | `AUTHORISED` | definitely not started | resumes E | yes after checks | preflight only | no |
| After preflight, before source | reserved to E | `PREFLIGHT_PASSED` | definitely not started | resumes E | yes after checks | preflight/source work | no |
| After source, before request | reserved to E | `SOURCE_RETRIEVED` | definitely not started | resumes E | yes after checks | request preparation | no |
| After request prepared | reserved to E | `REQUEST_PREPARED` | definitely not started | resumes E idempotently | yes after final reservation check | preparation only | no |
| After attempt-start, before transport | effectively consumed | `PROVIDER_ATTEMPT_STARTED` | may have started by safety rule | cannot repeat transport | no | none unless response appears | required for any future governed attempt |
| During/after transport, before response persistence | effectively consumed | `PROVIDER_ATTEMPT_STARTED` | may have started | recovery reports unknown | no | none | required for future governed attempt |
| After raw response persistence, before response stage | effectively consumed | `PROVIDER_ATTEMPT_STARTED` | responded durably | recovers response | no | yes | no |
| After response stage, before assessment | effectively consumed | `PROVIDER_RESPONSE_RECEIVED` | responded durably | recovers response | no | parse/assessment may resume | no |
| After assessment, before reconstruction | effectively consumed | response received | responded durably | resumes locally | no | yes | no |
| After reconstruction, before admission | effectively consumed | response received | responded durably | resumes locally | no | yes | no |
| After admission | effectively consumed | generation admitted/later | responded and admitted | reports existing outcome | no | result presentation | no |

For every definitely-unstarted reserved state, exact execution E has a legal resume path. For every
may-have-started state, no automatic second provider call exists.

## 8. Trace against current Parker stores

- `FileSystemFidelityFirstAttemptLedger.advancePreAttempt` already reconstructs
  `PREFLIGHT_PASSED`, `SOURCE_RETRIEVED`, and `REQUEST_PREPARED` idempotently and refuses them after
  provider start.
- `transition(... PROVIDER_ATTEMPT_STARTED)` is checksum-protected, locked and forced. Decoding
  requires ordered stages and permits at most one start marker.
- `GovernedRegionTranscriptionExecutionCoordinator` checks durable provider state first. Therefore a
  persisted response resumes locally without transport.
- With no response, the coordinator reads the ledger; a start marker returns
  `ATTEMPT_STARTED_WITHOUT_DURABLE_RESPONSE`, preventing a duplicate call.
- Before the start marker, its pre-attempt advances are idempotent, so the same exact execution can
  resume from reservation and prepared request.
- `FileSystemRegionProviderStateStore.persistReceived` creates and fsyncs raw state before adapter
  parsing/assessment. `readFor` binds recovery to the exact request digest. Assessment is a separate
  immutable sidecar, allowing restart from raw response.

The future authorization store needs only create-once grant, reservation, revocation, and optional
consumption-projection records. The existing attempt and provider-state stores provide the two
canonical safety facts. No cross-store atomic operation is required.

## 9. Impact on R6.10C

R6.10C shall implement `AVAILABLE` and create-once `RESERVED_FOR_EXECUTION`, plus an effective-state
evaluator that joins reservation with the exact ledger snapshot. Ordinary execution must accept an
already reserved identical execution before attempt-start, reject other executions, perform the
final revocation/binding check, and let the ledger marker define consumption. Tests must inject
crashes at every matrix boundary, prove same-execution resume before start, zero duplicate transport
after start, and local-only recovery after durable response.

This amendment also replaces R6.10B's ambiguous statement that post-attempt revocation may stop
downstream work “when safe”: durable-response downstream work is explicitly permitted because it is
local-only. Unknown outcomes remain blocked and non-retriable automatically.

Unresolved safety states after this amendment: **0**.
