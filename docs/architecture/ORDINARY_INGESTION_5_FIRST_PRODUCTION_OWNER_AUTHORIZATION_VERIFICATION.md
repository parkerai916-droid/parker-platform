# ORDINARY-INGESTION-5 First Production Owner Authorization Verification

## Read-only baseline

- Repository HEAD/upstream: `ceb5f2ae435644257a666c6aec58cbee96266331`; worktree clean.
- Production implementation: `ea1d96d656e97c7ed350eeabec5ef279b8ac36bb`.
- Production image: `sha256:6fa813f9f4c454652329cfb0ec08399055be360ff959fe1de71f98bb41b7e256`.
- Container: `e11b08cc6955faf29c0274d5fcd10f745563086f28d58a7c1cc2237db1c63b88`, running, restart count 0.
- Root HTTP 200; unauthenticated protected owner boundary HTTP 401.
- Region-v5 capability: `ACCEPTED`; accepted commit equals the running implementation commit. Capability digest: `9b404e8dfc4f0ffa3067fcffb00c39e6bd739050f173418de740239a1dc94103`. Acceptance-file count: 4.

No POST, PUT, PATCH, or DELETE request was issued. There was no build, deployment, restart, promotion, authorization mutation, execution, provider call, or evidence/derivative mutation.

## Evidence custody

Evidence `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766` remains durably registered. Its authoritative bytes exist at the governed evidence mount, have length 124,027, media type `application/pdf`, and SHA-256 `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5`. The SHA is unchanged. The historical manifest filename remains the server-side legacy upload name and was not rewritten.

## Authorization record

Exactly one semantic authorization record exists. The store also contains one expected zero-byte cross-process lock file; that lock is not an authorization record.

- Authorization ID: `ordinary-auth-09ed59bef8c46e602c56d927e7b2eb5a211ba769a79bc176dbc720de4f055069`.
- Authorization file SHA-256: `228297f66efd9e687808343e37355f18d7d5987c9622df4b7cfa76d504812178`.
- Canonical payload checksum: `66f5184724051524c6901db8fa15ce63908aac4bae1ecc7f67eda1792b91c27d`.
- Evidence ID: exact Sprint 2 ID.
- Source SHA: exact Sprint 2 SHA.
- Capability digest: exact accepted ordinary region-v5 digest.
- Provider: `OpenAI`.
- Purpose: `literal transcription`.
- Transmitted scope: `Selected authoritative PDF evidence crops`.
- Disclosure: selected authoritative PDF evidence crops will be transmitted to OpenAI for literal transcription.
- Approved by: production owner principal.
- Approved at: `2026-08-30T12:28:03.542998650Z`.
- Expires at: `2026-08-31T12:28:03.542998650Z`.

The record binds only the Sprint 2 evidence. It does not bind the CV or any other evidence. No execution identity is stored because no reservation event exists. No authorization event file exists.

## Binding classification

| Semantic | Classification | Evidence |
|---|---|---|
| Evidence ID | PERSISTED DIRECTLY | authorization grant |
| Source SHA-256 | PERSISTED DIRECTLY | authorization grant |
| Capability digest | PERSISTED DIRECTLY | authorization grant |
| Provider OpenAI | PERSISTED DIRECTLY | authorization grant |
| Purpose literal transcription | PERSISTED DIRECTLY | authorization grant |
| PDF-crop transmission scope/disclosure | PERSISTED DIRECTLY | authorization grant |
| Owner, approval, expiry | PERSISTED DIRECTLY | authorization grant |
| Capability ID | DERIVED FROM BOUND CAPABILITY IDENTITY | canonical digest resolves to ordinary region-v5 |
| Model `gpt-5.6-sol` | DERIVED FROM BOUND CAPABILITY IDENTITY | immutable capability identity |
| Profile `openai-region-anchored-transcription-v2` | DERIVED FROM BOUND CAPABILITY IDENTITY | immutable capability identity |
| Adapter/version `openai-responses-region-transcription-adapter` / `4.0.0` | DERIVED FROM BOUND CAPABILITY IDENTITY | immutable capability identity |
| Wire v5, `store=false`, renderer, geometry, bounds, batching | DERIVED FROM BOUND CAPABILITY IDENTITY | immutable capability identity |
| Runtime/build commit | DERIVED FROM AUTHORIZATION IDENTITY AND LIVE EXACT-BUILD ACCEPTANCE | deterministic authorization identity includes runtime commit; not a separate grant field |
| Execution ID | NOT STORED | no reservation event |
| Attempt ID/provider response | NOT STORED | execution has not started |

## Lifecycle and governed-store verification

Canonical storage state is `AVAILABLE`; owner presentation is `AUTHORISED`. It is not reserved and not consumed. There is no reservation event, no revocation event, and no new `PROVIDER_ATTEMPT_STARTED` associated with this authorization.

- Attempt-store raw file count: 4, comprising two historical ledger records plus their two zero-byte locks. Both ledger records and their `PROVIDER_ATTEMPT_STARTED` events predate this authorization; neither contains its authorization ID or evidence ID.
- Provider-state count: 2 historical files (one provider-state record and its assessment), both last modified on 2026-08-29 and unrelated to this authorization.
- Derivatives: 21 generation records / 19 content records, unchanged.
- Region acceptance authorities: 1, unchanged.
- Capability acceptances: 4, unchanged.

Parker's governed evidence shows no execution attempt, no new provider-attempt-start event, no provider-state, no derivative, and no post-authorization provider/execution log signal. Therefore Parker did not initiate governed OpenAI execution from this authorization. This does not claim abstract external-network impossibility; it is the conclusion supported by Parker's authoritative attempt boundary and durable stores. Claude calls: 0.

## Owner workflow read-back

Authenticated read-only `GET /owner/evidence/evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766/acquisition` returned:

- status `PROPOSED`;
- capability availability `READY`;
- egress authorization `AUTHORISED`;
- next step `SEPARATE_EXECUTION_ACTION_REQUIRED`;
- `authorizationAvailable: false`;
- `executeAvailable: false`;
- the exact authorization ID and expiry.

`executeAvailable: false` is literal: the proposal projection deliberately never exposes the generic selected-acquisition execute action. Authorization did not automatically enable or invoke execution.

## Execution eligibility and controls

The internal governed ordinary execution path is:

1. `OrdinaryRegionIngestionWorkflow.execute` freshly requires exact-build capability acceptance.
2. It resolves and verifies the exact authoritative source.
3. It reserves the supplied evidence-specific authorization to one execution ID.
4. It verifies authorization evidence/source/capability/provider binding.
5. It prepares the fixed region-v5 request.
6. Under the authorization guard, it durably records `PROVIDER_ATTEMPT_STARTED`.
7. Only after guard release can it call OpenAI, persist provider-state, validate/reconstruct, and admit a derivative.

The production runtime entry point is `ParkerRuntime.executeOrdinaryRegionIngestionAsOwner(evidenceArtifactId, authorizationId, executionId, attemptId)`. It can reserve the authorization and reach `PROVIDER_ATTEMPT_STARTED`, but it is **not yet wired** to `OwnerEvidenceOperations`, an HTTP route, or an enabled owner UI control.

### Current controls

- **Acquire machine-readable representation**: enabled read-only review control. It calls authenticated `GET /owner/evidence/{id}/acquisition` through `loadAcquisitionDecision`. It does not reserve, execute, or reach `PROVIDER_ATTEMPT_STARTED`.
- **Execute selected acquisition**: generic action rendered only for `SELECTED && executeAvailable`; this ordinary proposal is `PROPOSED` with `executeAvailable: false`, so the button does not exist. Its route would be `POST /owner/evidence/{id}/acquire`, mapped to `executeGovernedAcquisitionAsOwner`, a separate generic workflow that evaluates external egress as `NOT_AUTHORISED`; it is not the ordinary authorization execution boundary.
- **Process**: disabled legacy/manual Tier A local document-ingestion control. Its route is `POST /owner/evidence/{id}/process`; it does not execute ordinary external region-v5 transcription.
- **Run enhanced transcription**: disabled legacy/manual compatibility control. Its route is `POST /owner/evidence/{id}/transcribe-external`, mapped to the separate legacy external-transcription coordinator; it is unrelated to the ordinary region-v5 authorization and must not be used.

## Required execution map

```text
OWNER AUTHORIZATION
  ↓
AVAILABLE in store / AUTHORISED in owner projection
  ↓
Exact owner UI execution control: NOT YET PRODUCTION-WIRED
  ↓
Exact ordinary HTTP execution route: NOT YET PRODUCTION-WIRED
  ↓
ParkerRuntime.executeOrdinaryRegionIngestionAsOwner (implemented, internal only)
  ↓
OrdinaryRegionIngestionWorkflow.execute
  ↓
authorization reservation
  ↓
PROVIDER_ATTEMPT_STARTED
  ↓
OpenAI request
  ↓
provider-state persistence
  ↓
validation and source-order reconstruction
  ↓
derivative admission
```

The exact governed execution boundary has been identified, as has the missing production UI/HTTP wiring. Steven must not use `Acquire`, `Process`, generic `/acquire`, or `Run enhanced transcription` to attempt the first ordinary transcription.

## Final zero-mutation comparison

After all reads, evidence SHA, authorization count and file SHA, absence of authorization events, attempt count/fingerprint, provider-state count/fingerprint, authority count, derivative counts/fingerprints, and capability-acceptance count were unchanged. The read-only verification itself created no governed mutation.

## Verdict

UNIT ORDINARY-INGESTION-5 COMPLETE — FIRST PRODUCTION EVIDENCE-SPECIFIC OWNER AUTHORIZATION VERIFIED AS DURABLE, EXACTLY SCOPED, AND UNCONSUMED; AUTHORIZATION CAUSED NO PROVIDER ATTEMPT, PROVIDER-STATE, OR DERIVATIVE MUTATION; EXACT GOVERNED EXECUTION CONTROL IDENTIFIED FOR THE FIRST ORDINARY TRANSCRIPTION

## Exact next owner action

Do not click an execution-looking control. No production owner UI execution control currently reaches the identified ordinary region-v5 runtime boundary. The next unit must explicitly govern and wire that existing boundary before Steven can make the next execution decision.
