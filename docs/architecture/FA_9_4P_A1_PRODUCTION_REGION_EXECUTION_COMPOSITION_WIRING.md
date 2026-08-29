# FA.9.4P-A1E-R6.6A — Production Region Execution Composition Wiring

## Scope

This unit wires the already accepted R6.4 region provider adapter, R6.5 durable provider-state store, and R6.6 governed execution coordinator into `ParkerRuntime`. It does not expose an execution entry point, issue a provider request, execute an acceptance authority, deploy, or alter production configuration.

## Configuration contract

`PARKER_REGION_PROVIDER_STATE_STORAGE_ROOT` is the sole provider-state root input. Absence is represented as `null` and leaves the region execution lane uncomposed. There is no default, `/tmp` selection, repository-local fallback, or implicit directory creation.

When the key is present, startup requires:

- the existing acceptance authority and attempt roots;
- an exact configured production commit matching the embedded build commit;
- a ready external-transcription provider profile; and
- an accepted API credential representation.

The configured provider-state path must already be a writable directory. Any missing prerequisite or unusable path fails startup closed.

## Production composition

The runtime constructs one `FileSystemFidelityFirstAttemptLedger` for the configured attempt root and reuses that instance for both the existing fidelity-first acceptance coordinator and the region execution coordinator. It constructs `FileSystemRegionProviderStateStore` at exactly the configured root, composes `OpenAiRegionTranscriptionAdapter` with the established `JdkOpenAiResponsesTransport`, and injects all three into `GovernedRegionTranscriptionExecutionCoordinator`.

The composed coordinator is private. No ordinary owner method, acquisition executor, router binding, or acceptance bypass is added. A later governed unit must supply the region-specific authority boundary before execution can become reachable.

## Startup and recovery invariants

Composition performs no HTTP request. Startup writes no attempt record, provider response, assessment, or downstream generation. The provider-state store may establish its internal `.tmp` directory at the explicitly configured durable root, but it does not create an evidential record until a separately authorized execution occurs.

The R6.6 coordinator remains the only owner of attempt sequencing and recovery. Existing no-retry behavior, request binding, immutable response persistence, region order, validation, and downstream-resumption semantics are unchanged.

## Future R6.7 deployment input

R6.7 must supply `PARKER_REGION_PROVIDER_STATE_STORAGE_ROOT=/data/external-region-provider-state` and bind the durable host directory `/mnt/parker-data/parker/external-region-provider-state` to that container path. R6.6A does not modify the tracked Compose definition or deployment-local override because neither change is needed for offline application composition, and it does not create either production path.

## Offline verification

Focused composition tests prove exact configuration loading, explicit absence, no fallback, prerequisite failure, exact root reuse, accepted adapter construction, zero startup records, and absence of a public invocation method. The accepted R6.5 and R6.6 suites and the complete repository suite are run offline on Ubuntu/Linux.
