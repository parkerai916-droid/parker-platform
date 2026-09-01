# OI11R4J — V8 Capability Acceptance Rebinding to Corrected Implementation

## Status

Investigation complete; owner acceptance required before authority mutation.

## Starting state

- Host/repository: `parker`, `/home/steve/parker-platform`
- Branch and HEAD/upstream: `main`, `bac9151443842fd17ab24df55d12d2f704ca3217`
- Production container: `c3b739bdec37e594b63fe35bad43c4be545f75568db7b431b81b747e1b3c85b6`, running, restart count `0`
- Running artifact/source: image `sha256:5ca82f03ce61eade58c60eb4d3783547b4b266f974ed2ac218c09cf43f86075a`, source/build `d33518e85604083d620be08be4a4f001d7be3187`
- Provider calls/retries/external egress: `0 / 0 / 0`

The existing V8 acceptance record remains present and is bound to implementation commit `c304fdeff6bd89f96e8397ef4192e9f83b41cb93`. No D335-bound record exists.

## Acceptance semantics

The V8 record schema includes capability identity, capability digest, implementation commit, fixed acceptance-evidence identity/digest, accepting owner, timestamp, and deterministic record identity. `OrdinaryRequestRegionV8AcceptanceEvaluator.findExact` requires the runtime embedded commit and exact V8 digest to match one record. Therefore implementation binding is intentionally part of execution authority; each corrected implementation commit requires its own record. The C304 record remains immutable historical authority and is not rewritten.

The coordinator is deterministic: it requires the exact V8 capability identity and runtime commit equality, returns an existing exact record when present, otherwise creates one record in the `.request-region-v8-capability-acceptance-v1` family. Unknown identities, mismatches, and conflicts remain blocked. Multiple records may coexist for distinct implementation commits; the evaluator selects the exact runtime commit.

V8 identity and digest, request semantics, provenance semantics, and validation semantics are unchanged. The D335 change is implementation-only (the OI11R4H adapter correction), so no capability amendment or new digest is required.

Capability acceptance is provider-neutral. Provider profile, model, Authorization Purpose, credentials, permission, and execution authorization remain separate gates.

## Canonical rebinding path

The authenticated `POST /owner/admin/region-capability-acceptance` route parses exactly governed capability identities and 40-character commits, then delegates to the existing `OrdinaryRequestRegionV8CapabilityAcceptanceCoordinator`. No record was created in this investigation. The production evaluator currently reports `CAPABILITY_NOT_ACCEPTED`, runtime commit D335, and no accepted promoting commit.

The exact owner proposition is: accept V8 capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, for implementation `d33518e85604083d620be08be4a4f001d7be3187`, using the existing fixed acceptance-evidence identity/digest and coordinator. This would create exactly one additional D335-bound V8 record, preserve the C304 record, and authorize consideration of the capability only; it would not execute a provider call or alter provider/profile/purpose governance.

## Verification and preservation

Six legacy capability records remain present and unchanged. The OI11R4F failed execution and OI11R4I pre-egress failure remain immutable. No deployment, rebuild, restart, profile mutation, capability mutation, or provider activity occurred.

Expected post-acceptance authority state is six legacy records plus the existing C304 V8 record and one new D335 V8 record. Until explicit owner acceptance is provided, production remains correctly fail-closed for D335.

## Next action

Steven must explicitly accept execution authority for the exact V8 capability/digest on D335. Only then may the canonical route be invoked. After successful rebinding, a new separately governed live synthetic execution unit (not a retry of OI11R4I) is required.
