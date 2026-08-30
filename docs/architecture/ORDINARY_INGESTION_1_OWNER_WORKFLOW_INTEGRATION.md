# ORDINARY-INGESTION-1 owner workflow integration

## Scope and safety

This unit integrates the already-accepted ordinary external region-v5 capability into the existing owner evidence workflow. It does not change capability governance, create an authorization, reserve an execution, invoke a provider, generate a derivative, deploy, or mutate production stores.

## Reconciled gaps

The complete traced gap set was:

1. `LEGACY_UI_READINESS_BINDING`: the owner page emphasized legacy enhanced-transcription readiness rather than the ordinary region-v5 proposal.
2. `BACKEND_DECISION_DEFECT`: generic routing treated absent evidence-specific egress authorization as capability ineligibility, yielding `NO_ELIGIBLE_CAPABILITY` before an owner could review a proposal.
3. `STALE_CACHED_ACCEPTANCE_STATE`: the generic production catalogue projected ordinary acceptance during composition, while the governed ordinary workflow evaluates the acceptance store afresh.
4. `PROPOSAL_PATH_NOT_REGION_V5_AWARE`: ParkerRuntime already exposed a non-executing ordinary proposal, but the owner adapter and HTTP transport did not consume it.
5. `OWNER_AUTHORIZATION_PATH_NOT_REGION_V5_AWARE`: the owner presentation had no explicit proposal/review state before the existing separate authorization boundary.
6. `EXECUTION_ROUTE_NOT_REGION_V5_AWARE`: the UI's generic selected route could expose immediate execution; ordinary region-v5 must never execute without its evidence-bound authorization.

## Correction

The existing `proposeOrdinaryRegionIngestionAsOwner` boundary is now composed into both desktop and HTTP owner adapters. An accepted PDF proposal projects as `PROPOSED`, identifies canonical `ordinary-external-region-transcription-v5`, discloses the transmitted scope, reports egress as `NOT_AUTHORISED`, and directs the owner to a separate review/authorization step. It always reports `executeAvailable=false` and renders no execution control.

The ordinary workflow remains the authority for current acceptance. A missing proposal, non-PDF source, or any disposition other than `ACCEPTED` falls back to the existing fail-closed governed decision. Native searchable text does not suppress an accepted region-v5 PDF proposal.

## Verification

Offline tests cover accepted PDF/no authorization, canonical identity, disclosure, no execution availability, native-searchable-text eligibility, fresh acceptance changes, non-accepted fail-closed behavior, non-PDF behavior, and existing adapter/HTTP regressions. Existing ordinary-ingestion tests retain coverage of temporary authorization stores, fake-provider execution, revocation/attempt linearization, duplicate execution identities, and zero real egress.

## Deployment boundary

The accepted production capability is exact-build bound to promoting build `1694a6a576a8d34a305ab1f3d797b5ab8f8b65d5`. This implementation necessarily produces a different source/runtime build, so the governed evaluator would return `CAPABILITY_NOT_ACCEPTED` for that build. The unit therefore stops after offline verification and source commit/push. No deployment or re-promotion is permitted here.
