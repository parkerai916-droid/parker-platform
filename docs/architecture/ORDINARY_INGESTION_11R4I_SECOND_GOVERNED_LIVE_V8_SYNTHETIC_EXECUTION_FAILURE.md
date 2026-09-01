# OI11R4I — Second Governed Live V8 Synthetic Execution (Pre-Egress Failure)

Date: 2026-09-01

## Disposition

OI11R4I stopped pre-egress. The canonical authorization endpoint returned `UNAVAILABLE` with `CAPABILITY_NOT_ACCEPTED`; no provider transport was attempted.

## Starting state

- Host: `parker`
- Repository: `/home/steve/parker-platform`
- Branch: `main`
- HEAD/upstream: `2d7079a018f457f29bb0b4106ebea37fa20f097d`
- Worktree: clean before this report
- Container: `c3b739bdec37e594b63fe35bad43c4be545f75568db7b431b81b747e1b3c85b6`
- Container state: running; restart count `0`
- Running image: `sha256:5ca82f03ce61eade58c60eb4d3783547b4b266f974ed2ac218c09cf43f86075a`

The container labels and environment identify source/build/production commit `d33518e85604083d620be08be4a4f001d7be3187`, not the `c304fde…` identity bound by the persisted V8 acceptance record.

## Frozen source verification

The governed evidence object was read-only verified at `/data/evidence/evidence-84d85f99-3a94-4101-86b2-8b8aa9aef0ae.evidence`:

- Evidence ID: `evidence-84d85f99-3a94-4101-86b2-8b8aa9aef0ae`
- SHA-256: `f9ce327166b00c28d0ce50334bad256c03f2ff9c74e6b045592f53f8dce03a89`
- Size: `1,406` bytes
- Page count: `2` (from its governed manifest)

## Pre-egress checks

The non-provider readiness endpoint reported `READY`, but acquisition for this evidence reported `NO_ELIGIBLE_CAPABILITY`. The canonical authorization request was:

`POST /owner/evidence/evidence-84d85f99-3a94-4101-86b2-8b8aa9aef0ae/authorize-region-transcription`

Response: `status=UNAVAILABLE`, `detail=CAPABILITY_NOT_ACCEPTED`, `authorizationId=null`, `executionStarted=false`.

The persisted V8 acceptance record was read-only verified as record `c3efa482db11d5ecea93ddf9b5cce1fb01793ed25f0577a62a054bf783a8a125`, family `.request-region-v8-capability-acceptance-v1`, capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, and implementation binding `c304fdeff6bd89f96e8397ef4192e9f83b41cb93`.

Thus the required production capability acceptance is not applicable to the currently running artifact identity. This is a deployment/artifact convergence blocker, not a provider response result.

## Store and egress accounting

Before authorization, counts were:

`evidence=28; manifests=28; owner-authorizations=8; attempts=6; provider-state=4; derivative-generations=21; derivative-content=19; capability-acceptances=7`.

After the rejected authorization request, the same counts remained unchanged. No new execution identity, authorization reservation, attempt, provider-state record, derivative, or audit record was created. The existing OI11R4F history remains untouched.

Provider calls: OpenAI `0`; Claude `0`; other external `0`; retries `0`.

## Root cause and next unit

Primary classification: **pre-egress deployment/artifact identity convergence failure**. The running container is not the accepted OI11R4D/C304 artifact expected by the OI11R4I starting gate, and the canonical evaluator correctly fails closed against the mismatched implementation binding.

No provider call or retry is authorized in this unit. The smallest next governed unit must reconcile/deploy the already accepted C304 artifact (or otherwise establish a newly governed artifact/acceptance binding) before a new, separately identified live synthetic execution unit can be considered. OI11R4F remains immutable historical failure evidence.
