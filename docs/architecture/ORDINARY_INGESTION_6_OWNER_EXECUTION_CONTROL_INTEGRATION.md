# ORDINARY-INGESTION-6 owner execution control integration

## Outcome

The governed ordinary region-v5 execution boundary is production-wired through a distinct authenticated owner HTTP route and an explicit owner UI control. The exact implementation build was deployed and accepted. The prior evidence-specific authorization is intentionally build-bound and therefore is not valid for the new build; Parker requires a new explicit owner authorization. No production execution, authorization creation, provider attempt, provider call, provider-state record, or derivative was created by this unit.

## Starting state

- Repository HEAD/upstream: `60afa657975ea23d6c9079c2f30cf932590d7797`; clean.
- Production implementation: `ea1d96d656e97c7ed350eeabec5ef279b8ac36bb`.
- Image: `sha256:6fa813f9f4c454652329cfb0ec08399055be360ff959fe1de71f98bb41b7e256`.
- Container: `e11b08cc6955faf29c0274d5fcd10f745563086f28d58a7c1cc2237db1c63b88`; running; restart count 0; HTTP 200; unauthenticated protected boundary 401.
- Capability: `ACCEPTED`; four acceptance records.
- Authorization: `ordinary-auth-09ed59bef8c46e602c56d927e7b2eb5a211ba769a79bc176dbc720de4f055069`, `AVAILABLE`, owner projection `AUTHORISED`; file SHA-256 `228297f66efd9e687808343e37355f18d7d5987c9622df4b7cfa76d504812178`.
- Evidence: `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766`, source SHA-256 `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5`.
- Stores: attempts 4 files; provider-state 2; derivative generations/content 21/19.

## Complete execution trace and gaps

Five production-integration gaps existed before implementation: A `EXECUTION_HTTP_ROUTE_MISSING`, B `EXECUTION_UI_CONTROL_MISSING`, C `OWNER_ADAPTER_EXECUTION_NOT_WIRED`, D `EXECUTION_STATE_NOT_PROJECTED`, and the combined E/F/I confirmation, refresh, and safe error surface. There was no duplicate execution implementation and no legacy control was repurposed.

| Stage | Class/file | Method or route | Implemented before | Wired before | Input / output / mutation | Fail-closed condition | Gap/classification |
|---|---|---:|---:|---:|---|---|---|
| Owner UI | `OwnerEvidenceHttpServer.kt` HTML/JS | acquisition panel | partial | no | read-only decision to explicit action; no mutation until confirmed click | control absent unless canonical eligibility is true | B, E, F, I |
| Owner HTTP | `OwnerEvidenceHttpServer.EvidenceHandler` | `POST /owner/evidence/{id}/execute-region-transcription` | no | no | route-bound evidence ID; execution result | authenticated; empty body only; bounded request | A |
| Runtime adapter | `OwnerUiEvidenceRuntimeAdapter` | `executeOrdinaryRegionTranscription` | no | no | evidence ID to canonical authorization plus server-generated execution/attempt IDs | accepted proposal, `AUTHORISED`, `NOT_STARTED`, non-null backend authorization | C, D |
| Runtime boundary | `ParkerRuntime.kt` | `executeOrdinaryRegionIngestionAsOwner` | yes | no | evidence, authorization, execution, attempt to governed result | runtime running and workflow composed | C |
| Coordinator | `OrdinaryRegionIngestionWorkflow` | `execute` | yes | internal only | validates capability/source then reserves authorization | acceptance/source/authorization/binding mismatch | none |
| Authorization | `FileSystemOrdinaryRegionAuthorizationStore` | `load`, `reserve` | yes | internal | immutable grant plus append-only reservation event | absent, expired, revoked, different execution | none |
| Attempt ledger | `GovernedRegionTranscriptionExecutionCoordinator` | prepare/start | yes | internal | guarded ledger transition | identity conflict or prior attempt state | none |
| Attempt start | coordinator + authorization guard | `durablyStartProviderAttempt` | yes | internal | appends `PROVIDER_ATTEMPT_STARTED` before transport | revoked/conflicting reservation | none |
| Provider transport | coordinator | `transportAfterGuardRelease` | yes | internal | exactly one governed OpenAI request | no automatic retry; recovery uses durable state | none |
| Provider-state | `FileSystemRegionProviderStateStore` | persist raw response | yes | internal | raw response persisted before parse | record/digest conflict or persistence failure | none |
| Validation/order | validator + `RegionSourceOrderReconstructor` | validate/reconstruct | yes | internal | typed result and deterministic Parker source order | malformed/missing/duplicate/unknown regions | none |
| Admission | `OrdinaryRegionDerivativeAdmission` | `admit` | yes | internal | durable derivative content/generation and audit | provenance/content/generation conflict | none |
| Final state | attempt ledger/workflow | terminal transition/result | yes | internal | `ADMITTED` or explicit failure/review disposition | never fabricates success | D/F/I projection only |

## Integration and trust boundary

The new route is `POST /owner/evidence/{evidenceArtifactId}/execute-region-transcription`. The browser supplies only the route-bound evidence identity and an empty body. Any request body, including provider/model/capability overrides, is rejected with HTTP 400. Provider, endpoint, model, adapter, profile, wire, store, rendering, region geometry, capability digest, authorization state, execution state, request body, and provider endpoint remain canonical backend state.

The adapter freshly evaluates the accepted proposal and canonical authorization status, requires execution state `NOT_STARTED`, derives the authorization ID from backend state, generates opaque execution and attempt IDs server-side, and invokes the one existing `ParkerRuntime.executeOrdinaryRegionIngestionAsOwner` boundary. Authorization remains a separate action.

The UI label is `Execute external transcription`. It is distinct from `Process`, `Run enhanced transcription`, `Acquire machine-readable representation`, and generic governed acquisition controls. Rendering, decision refresh, or opening/cancelling confirmation performs no execution.

The execution confirmation states the exact evidence ID, existing evidence-specific authorization, provider OpenAI, literal-transcription purpose, transmission of selected authoritative PDF evidence crops, that the action initiates external processing, authorization reservation/consumption semantics, and possible provider-attempt/provider-state/derivative creation. Its operative sentence is: `This action initiates the authorized external transcription.`

Authorization is reserved inside `OrdinaryRegionIngestionWorkflow.execute` after fresh capability/source resolution and before request preparation. `PROVIDER_ATTEMPT_STARTED` is durably appended under the authorization guard before provider transport. Raw provider response is persisted before parsing. Provider failure does not trigger automatic retry.

## Tests and accepted-semantics diff

Targeted suites passed for the adapter, HTTP server/UI boundary, and ordinary ingestion workflow. Added coverage proves accepted/authorized visibility, unavailable/reserved suppression, canonical backend identities, authentication, browser-override rejection, deliberate confirmation, and safe result refresh. Existing runtime tests continue to cover wrong-source binding, single reservation/provider attempt, durable raw response before parse, deterministic reconstruction/admission, provider failure without retry, races/revocation/recovery, and capability acceptance.

Full verification: 3,257 tests; 0 failures; 0 errors; 9 skipped. `git diff --check`: PASS.

Accepted acquisition semantics changed: **NO**. Provider, endpoint, model, reasoning, adapter/version, profile, wire, schema, rendering, region geometry, limits, batching, `store=false`, retry behavior, provider-state persistence, derivative admission, and authorization lifecycle semantics are unchanged. Changes are limited to owner execution exposure/composition and canonical execution-state projection.

## Implementation, build, deployment, and promotion

- Implementation commit: `72482456531e91ff8eced4cbe073f182ae805126`.
- Built/deployed image: `sha256:c8e0dc7da6b5c167f2259314b06bd838230f048c8786723246aa1b09b5c51dda`.
- Container: `728a00e4ce3e4448873b3016c9c3331e67966602842656fba049dc1e3fea7be6`; running; restart count 0.
- Repository commit = build input = embedded `Parker-Source-Commit` = `PARKER_SOURCE_COMMIT` = `PARKER_PRODUCTION_COMMIT`: exact 40-character equality PASS.
- Only `parker-runtime` was force-recreated using the active base/OpenAI/FA stack plus deployment-local immutable-image override. Ollama, Portainer, Docker, containerd, and storage roots were unchanged.
- Post-deployment: HTTP 200; unauthenticated protected boundary 401; runtime-start log present; no fatal startup error.
- Pre-promotion capability: `CAPABILITY_NOT_ACCEPTED`.
- Promotion POST count: exactly 1. Result: HTTP 201 `CREATED`, record `d26cc25642aceded047b52b61c6f3f3956809c635bc9976da25c898cb1d387ac`.
- Post-promotion without restart: `ACCEPTED`; acceptance records 5.

## Authorization build binding and live owner state

`OrdinaryRegionIngestionWorkflow.authorizationIdentity` hashes evidence ID, source SHA-256, capability digest, `runtimeCommit()`, and owner principal. The persisted old authorization therefore remains immutable but cannot be selected under implementation `7248245...`; the backend derives a different canonical authorization identity. It is not reinterpreted or migrated.

Final Sprint 2 workflow is `PROPOSED`, capability `ACCEPTED`, authorization `NOT_AUTHORISED`, `authorizationAvailable=true`, `executeAvailable=false`, execution `NOT_STARTED`. The production page contains the distinct `Execute external transcription` control and execution-specific confirmation implementation, but the evidence row correctly does not expose the action until Steven explicitly re-authorizes this exact build.

The prior authorization file remains present and byte-identical at SHA-256 `228297f66efd9e687808343e37355f18d7d5987c9622df4b7cfa76d504812178`. Final stores remain: attempts 4, provider-state 2, derivative generations/content 21/19. Production OpenAI calls: 0. Claude calls: 0. No authorization event/reservation file exists.

## Next owner action

OWNER RE-AUTHORIZATION REQUIRED FOR NEW BUILD. Steven must explicitly select `Authorize external transcription` for `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766`. After reviewing the separate execution confirmation, Steven—not a development agent—may later invoke `Execute external transcription`.
