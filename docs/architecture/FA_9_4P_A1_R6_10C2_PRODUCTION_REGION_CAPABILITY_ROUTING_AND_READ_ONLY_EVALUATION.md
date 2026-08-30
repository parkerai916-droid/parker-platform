# FA.9.4P-A1E-R6.10C2 — Production region capability routing and read-only evaluation

## D-R1 stop finding

R6.10D-R1 safely deployed C1 but stopped before promotion. The canonical
`ProductionAcquisitionCapabilityCatalogue.ordinaryRegionV5Capability` projection existed, yet
`ParkerRuntime` called `ProductionAcquisitionCapabilityCatalogue.create()` without it. The only
acceptance-aware path was ordinary execution; proposal checked PDF custody only, and no authenticated
read-only status surface existed.

## Production registration and canonical identity

Production composition now supplies the existing `ordinaryRegionV5Capability(...)` factory to the
catalogue. No second capability definition was introduced. The projection continues to derive its
configuration identity from `OrdinaryRegionCapabilityIdentity`, preserving the exact OpenAI
Responses operation, gpt-5.6-sol, reasoning none, store false, original detail, adapter 4.0.0,
profile v2, wire v5, schema/instruction/processing identities, PDF scope, 32-region maximum,
16,777,216-byte aggregate bound and no batching. Historical adapter 2/3, profile v1 and wire v4
remain distinct and unchanged.

Registration does not confer acceptance. The catalogue projection is unavailable unless the
fresh evaluator reports accepted, and ordinary execution retains its own mandatory evaluation.

## Read-only evaluation and proposal

`OrdinaryRegionCapabilityStatus` is the bounded owner-visible projection. It includes capability,
provider, operation, model, adapter/version, profile, wire, media, region/body bounds, batching,
disposition, embedded runtime commit and accepted promoting commit when present.

`evaluateOrdinaryRegionCapabilityStatus` delegates to the existing
`OrdinaryRegionCapabilityAcceptanceEvaluator`; it introduces no alternate acceptance rule. Empty,
legacy, corrupt, ambiguous, wrong-capability and wrong-build records remain not accepted. Every
call performs the evaluator's fresh durable lookup. `OrdinaryRegionIngestionWorkflow.capabilityStatus`
uses it directly, and PDF proposals carry the same status without execution or authorization.
`ParkerRuntime.ordinaryRegionCapabilityStatusAsOwner` returns that status or `NOT_CONFIGURED` when
the governed workflow was not composed.

## Authenticated HTTP status

The existing `/owner/admin/region-capability-acceptance` resource now supports authenticated GET
for read-only status and retains C1 authenticated POST for governed creation. GET accepts no body,
performs no mutation and exposes no credential. It has no path to owner authorization, reservation,
revocation, attempt start, provider transport/state or derivative admission.

## Verification and preservation

Focused tests prove catalogue registration and canonical identity, empty-store
`CAPABILITY_NOT_ACCEPTED`, dynamic exact-build `ACCEPTED` without restart, different-build rejection,
legacy/corrupt fail-closed behavior inherited from the evaluator, exact routing metadata, repeated
read-only equality, authenticated GET/401 and unchanged POST behavior. Existing C/C1 tests retain
coverage for typed promotion, concurrency, provider-state recovery, bounds, wire v5, admission and
historical decoding. Provider mechanisms in tests remain fake.

Production remained read-only on container
`30d0b2414ec6ad29cdd92ece2ab72d8bfcdba9c262cd156589b607f49fb82ee7`, image
`sha256:ef821586cd84bce39b63beb2ddfa3c5ff9f89fd8fd8a859a15ce3c9f34a7cb44`, restart count zero.
Acceptance and authorization stores remained empty; attempt, provider-state, assessment, derivative
and audit fingerprints remained unchanged. OpenAI and Claude calls were zero.

## R6.10D-R2

R6.10D-R2 must build and deploy the exact C2 commit, call authenticated GET to prove
`CAPABILITY_NOT_ACCEPTED`, invoke C1 POST once with only capability/build, then call the same GET to
prove dynamic `ACCEPTED` and exact routing metadata without restart, authorization, attempt,
provider state, derivative or provider traffic.
