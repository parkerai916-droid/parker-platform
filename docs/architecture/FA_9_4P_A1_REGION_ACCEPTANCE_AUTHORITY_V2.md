# FA.9.4P-A1E-R6.8A1 — Region acceptance authority v2

## R6.8C stop finding

R6.8C correctly stopped before creating an authority. The v1 manifest recorded the digest of the
provider-neutral region request instruction while the execution identity recorded the distinct
OpenAI adapter instruction. It also omitted the provider-specific profile, reasoning setting,
request operation, and a bounded structural purpose. The low-level create-once store was the only
creation operation, so a caller would have needed to assemble trusted page, region, and provider
facts itself.

## Explicit versioning

The corrected format is `parker.region-transcription-acceptance-authority.v2`. V1 classes, canonical
domain, decoder, file suffix, and store behavior remain unchanged. V2 uses its own canonical domain,
type, decoder, checksum envelope, and `.region-acceptance-authority-v2` suffix. Production currently
contains no v1 region-authority records, so no migration exists or is needed. The older
`FidelityFirstAcceptanceAuthority` is also unchanged.

## Complete request and provider surface

V2 keeps the instruction layers distinct:

- `request.provider_neutral_instruction_sha256` binds the provider-neutral request instruction;
- `adapter.provider_instruction_sha256` binds the provider-facing OpenAI adapter instruction.

It additionally binds provider, Responses API endpoint family and exact endpoint, operation
`POST /v1/responses`, model, reasoning, store flag, adapter and version, provider-neutral profile,
provider-specific profile, processing profile, schema identity/version/digest, image detail, page
representations, region geometry/crops/classes, deterministic ordering, context policy, source, and
all deployment identities. The existing R6 request digest transitively binds the neutral request
instruction, images, model/adapter constants, reasoning, store, schema, and request profiles. The
existing execution identity separately binds the provider-specific profile and adapter instruction.
V2 requires both identities to agree with the complete current provider surface.

## Purpose

`RegionAcceptancePurposeCode.CONTROLLED_LIVE_FIDELITY_ACCEPTANCE` is the sole accepted v2 purpose.
Its detail is generated internally from the reconstructed page and region counts. The execution
bridge recomputes that exact purpose and rejects a count/detail mismatch. This authority cannot be
used as ordinary owner execution, retry authority, analysis authority, or comparison transcription.

## Governed creation boundary

`RegionTranscriptionAcceptanceAuthorityCreationCoordinator` accepts only bounded identifiers,
evidence identity, programme unit, authoriser, and time. It resolves current custody bytes and
internally reconstructs pages, regions, order, request, context, deployment, and provider facts. It
then builds the v2 manifest, checks that neither the existing R6.6 ledger nor R6.5 provider store has
state for the reconstructed identity, and calls the create-once v2 store exactly once.

Creation never receives a manifest, page, region, provider configuration, or execution binding from
the caller. It cannot call a provider, advance the attempt ledger, write provider state, invoke the
execution bridge, or consume the authority.

## Creation and execution consistency

Creation and execution share `CustodyRegionAcceptanceReconstructor`,
`RegionAcceptanceProviderSurface`, and `RegionAcceptanceManifestV2Factory`. At execution time the
bridge re-resolves custody bytes and rebuilds the same v2 manifest. Only byte-equivalent canonical
facts and digest permit delegation to `GovernedRegionTranscriptionExecutionCoordinator`. R6.6 owns
the attempt marker and no-retry behavior exactly as before; A1E-R6.8A1 changes neither.

Runtime composition remains inert. It constructs the explicit acceptance-administration creation
boundary and v2 execution bridge but calls neither on startup. A later deployment must install this
version before a resumed R6.8C creates the single reviewed A1 authority; R6.9 remains the separately
authorized one-shot provider execution and human-fidelity-review unit.
