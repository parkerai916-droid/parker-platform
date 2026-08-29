# FA.9.4P-A1E-R6.8A — Region-specific acceptance authority

## Decision

R6.8 stopped because its one-shot authority was the legacy document-level identity: it could bind a
source and provider profile but could not bind the R6 page raster, region geometry, crop pixels,
source-order graph, context policy, or immutable deployed image. Executing R6 with that record would
have allowed a materially different acquisition surface to pass under the same authority.

Region-bound external transcription remains `ACCEPTANCE_PENDING`. A provider attempt is legal only
through the region acceptance coordinator and only after a separately stored, create-once R6
authority has been loaded and matched against facts reconstructed at invocation time.

The legacy `FidelityFirstAcceptanceAuthority` format and coordinator are unchanged. Region
authorities use a distinct schema, file suffix, storage root, checksum, and record identity. The two
formats cannot be confused or substituted.

The legacy type remains unchanged because it is durable governed history and remains the decoder for
existing document-level records. Expanding its fixed field count would make old records ambiguous or
unreadable; it therefore cannot authorize the region lane.

## Canonical authority

`RegionAcceptanceManifest` is a lexicographically ordered list of uniquely named facts encoded with
explicit byte lengths. Its SHA-256 digest binds the entire plan. Required facts cover:

- authoritative evidence identity, digest, byte length, and media type;
- source, build, embedded-runtime commits, and immutable deployed image ID;
- every rendered page representation and canonical pixel digest;
- every derived region ID, page, dimensions, crop bounds/digest, structural class, and derivation
  profile;
- the complete deterministic source order graph and ambiguity state;
- region-only/full-page context policy and all context-image digests;
- correlation, profile, processing, instruction, schema, endpoint, wire, model, adapter, image-detail,
  storage, and one-attempt settings.

Authorities are written with `CREATE_NEW`, forced to durable storage, and never overwritten. The
payload checksum detects corruption; the record identity detects envelope substitution.

## Invocation ordering

The public acceptance command accepts only an authority ID. It performs these gates in order:

1. load and verify the dedicated authority record;
2. require lifecycle `ACCEPTANCE_PENDING`;
3. compare current source/build/runtime commit and immutable image identities;
4. reconstruct custody bytes, page representations, region geometry/order, request images, and
   effective provider configuration;
5. compare both the canonical manifest and digest to the authority;
6. compare the reconstructed execution identity;
7. pass the reconstructed binding to `GovernedRegionTranscriptionExecutionCoordinator`.

No caller-provided source, geometry, request, or provider override is accepted. A missing or corrupt
authority, configuration drift, reconstruction failure, or fact mismatch stops before the R6.6
coordinator and therefore before its attempt ledger can mark `PROVIDER_ATTEMPT_STARTED`.

R6.6 remains responsible for durable attempt ordering and the R6.5 provider-response boundary. No
retry path is added.

The region execution identity is reconstructed separately from the old document identity. Its
request ID is the existing R6.5 canonical request digest, its attempt ID is the request correlation
ID, and its remaining source/provider fields come from the exact current acquisition surface. That
identity is passed unchanged to the existing R6.6 ledger and coordinator.

## Runtime configuration

The deployment-only keys are:

- `PARKER_REGION_ACCEPTANCE_AUTHORITY_STORAGE_ROOT`
- `PARKER_DEPLOYED_IMMUTABLE_IMAGE_ID`
- `PARKER_SOURCE_COMMIT`

They complement the existing production/runtime commit, attempt-ledger, and region-provider-state
roots. If the region authority root is configured without the complete immutable deployment and
durability binding, startup fails closed. Merely configuring the roots performs no execution.

Preferred future production paths are
`/mnt/parker-data/parker/region-transcription-acceptance-authorities` on the host and
`/data/region-transcription-acceptance-authorities` in the container. R6.8A does not create, mount,
or deploy either path.

The governed continuation is deliberately split: R6.8B deploys the authority-capable inert runtime;
R6.8C creates and independently reviews the exact A1 authority; R6.9 invokes it exactly once; human
fidelity review decides the result. None of those later steps is implied by this implementation.

## Operational boundary

R6.8A is offline implementation and verification only. It performs no provider request, authority
execution, deployment, restart, production-store mutation, or lifecycle transition. A later unit
must create the exact one-shot authority from independently reviewed canonical facts and separately
authorize deployment and invocation.
