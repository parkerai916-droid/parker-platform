# FA.9.4P-A1 region-aware execution composition and no-retry recovery

**Status:** Implemented and verified offline on Ubuntu. Production remains `ACCEPTANCE_PENDING`; no provider call, authority creation, deployment, production-store mutation, OCR, comparison, or O.5 consumption occurred.

## Composition and authority

`GovernedRegionTranscriptionExecutionCoordinator` composes the accepted R6.3 request, existing `FileSystemFidelityFirstAttemptLedger`, provider-neutral `RegionExternalTranscriptionMechanism`, R6.5 `FileSystemRegionProviderStateStore`, and R6.3 validator. The request is the immutable handoff from R6.1/R6.2 preparation: every target binds authoritative evidence/source SHA-256, page representation, page number, deterministic SourceRegionId, crop/image digest, derivation profile and optional full-page context. Parker's supplied `sourceRegionOrder` remains authoritative; provider array order is retained only as provider evidence.

The existing attempt ledger remains the sole authority for which execution stage was reached. The provider-state store remains the sole authority for what response was received. No parallel ledger or response format is introduced.

## Exact binding and sequence

The ledger identity's immutable `requestId` must equal the R6.5 canonical request digest. That digest binds source/page/ordered targets/crops/context, provider/model, adapter/version, transcription and processing profiles, schema/digest, literal instruction digest, reasoning, store and image detail. Correlation must equal the governed attempt ID. The remaining ledger fields independently bind execution, source, repository commit, provider profile and OpenAI instruction identity. A mismatch fails before transport.

Execution order is:

1. validate exact identity and source/region bindings;
2. derive and compare the R6.5 request digest;
3. check the provider-state store;
4. check the existing attempt ledger;
5. recover an existing response without transmission;
6. refuse automatic retry if `PROVIDER_ATTEMPT_STARTED` already exists;
7. durably advance `PREFLIGHT_PASSED`, `SOURCE_RETRIEVED`, `REQUEST_PREPARED`;
8. durably force `PROVIDER_ATTEMPT_STARTED` immediately before invoking the injected mechanism;
9. require the R6.5 response record before treating any provider return as known;
10. durably record `PROVIDER_RESPONSE_RECEIVED`;
11. validate recovered structured state and expose bounded downstream continuation.

Marker persistence failure prevents transport. A timeout/exception after the marker and without response returns unknown/consumed state. Response persistence failure never becomes retry permission.

## Restart matrix

- **A — no marker/no response:** the one first attempt may proceed.
- **B — marker/no response:** `ATTEMPT_OUTCOME_UNKNOWN`; automatic retransmission forbidden.
- **C — raw response:** `RAW_RESPONSE_RECOVERED`; no retransmission.
- **D — raw plus parse failure:** `PARSE_FAILURE_RECOVERED`; raw fact retained.
- **E — raw/structured plus validation failure:** `VALIDATION_FAILURE_RECOVERED`.
- **F — valid state/downstream interrupted:** `DOWNSTREAM_RESUMABLE` with validated region result and Parker source order.

Fresh ledger/store/coordinator instances make the same decision. Concurrent invocations share the ledger's filesystem lock; at most one can durably cross attempt start. Existing response lookup occurs before attempt lookup and is repeated after competing marker transitions.

## Configuration, security and future deployment

Future production roots are `/data/external-region-provider-state` in the container and `/mnt/parker-data/parker/external-region-provider-state` on the host. `RegionProviderStateRootConfiguration` fails closed when region execution is enabled without an explicit existing writable root; there is no `/tmp`, working-directory or home fallback. R6.6 does not create the production root or modify Compose.

Durable request, ledger and provider-state artefacts exclude credentials and Authorization material. Tests use synthetic data, injected mechanisms and JUnit-owned bounded directories only. The rotated credential must remain merely `PRESENT`; any future live region-aware execution requires a separately governed acceptance authority binding production commit, source/page/regions, provider/model/adapter/profile/schema/instruction/context, maximum one attempt and recipient. No ordinary owner lane is enabled by this unit.

## Next unit

The next bounded unit should specify and offline-verify the exact region-aware acceptance authority and production composition changes, including the new persistent mount and service-account controls. Credential rotation verification, deployment and any live provider call remain separate explicit gates.
