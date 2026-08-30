# FA.9.4P-A1E-R6.10B — Governed ordinary external region ingestion

## 1. Decision and scope

This document locks the end-to-end architecture for moving the already-proven OpenAI region-v5
transcription capability from its acceptance-only lane into ordinary, owner-selected evidence
ingestion. It is a design decision only. R6.10B changes no runtime, production configuration,
provider state, acceptance state, or historical evidence.

R6.10 stopped because the generic provider-profile `acceptanceState=ACCEPTED` gates the historical
whole-document adapter rather than the proven region adapter. R6.10A then established that a safe
fix is larger than a lifecycle/catalogue flag: ordinary acquisition hard-codes external egress as
not authorised, registers no external executor, and has no ordinary region reconstruction or
derivative-admission pipeline. This design resolves all of those boundaries together.

The accepted candidate remains exactly:

| Fact | Value |
|---|---|
| Provider / model | OpenAI / `gpt-5.6-sol` |
| Endpoint / operation | Responses API / `POST /v1/responses` |
| Adapter | `openai-responses-region-transcription-adapter` `4.0.0` |
| Provider profile | `openai-region-anchored-transcription-v2` |
| Wire / schema | v5 / `bd5c38f105e90bcb7ba3c3a4c1790ea45f43599f514cc09860b4223d65b527e2` |
| Instruction | `3e1c1c647d011f748fc2cc81cb9e17a4354b0ca879abd28888005ef8d05d71e2` |
| Processing | `external-transcription.deterministic-source-region-raster-v1` |
| Request policy | reasoning `none`, `store=false`, image detail `original`, `REGION_ONLY` |
| Source projection | PDFBox 3.0.7, 300 DPI; Parker deterministic R6.2 region graph |

The evidence is R6.9 execution `execution-fa-9.4p-a1e-r6.8c1`, response
`resp_0007d6aa81587b3e016a92f716feb087d0ae9e005456676627`, R6.9A forensic commit
`07b5b07769e57f5e066b680599143a2de6082ad8`, R6.9B correction commit
`ac6c49115a756d4b5b88ff69e1789b36f54b03e0`, and R6.9C fidelity commit
`b9a964a98edf9803791a046d189a61c2353a44a3` (`PASS_FIDELITY`, 24/24 `EXACT`). The
historical R6.9 `VALIDATION_MALFORMED_SCHEMA` assessment remains immutable.

## 2. Current architecture map

The current ordinary path is `GovernedAcquisitionOwnerWorkflow` →
`DeterministicEvidenceAcquisitionRouter` → `GovernedAcquisitionExecutionCoordinator`. The workflow
resolves an `EvidenceSourceManifest`, projects source characteristics, and routes against
`ProductionAcquisitionCapabilityCatalogue`. The execution coordinator re-resolves authoritative
bytes through `AuthoritativeAcquisitionSourceResolver` and checks the selected executor's exact
`AcquisitionExecutorBinding`.

Three current facts prevent external region use:

1. both ordinary evaluation and execution pass `ExternalEgressAuthorisation.NOT_AUTHORISED`;
2. `ParkerRuntime` registers only `TierANativeAcquisitionExecutor` and
   `LocalOcrAcquisitionExecutor`;
3. `OpenAiRegionTranscriptionAdapter` is reachable only from
   `GovernedRegionTranscriptionExecutionCoordinator` behind a region acceptance authority.

Reusable components already exist: `EvidenceCustodian`, `DeterministicSourcePageRenderer`,
`DeterministicSourceRegionDeriver`, `OpenAiRegionTranscriptionAdapter`,
`FileSystemFidelityFirstAttemptLedger`, `FileSystemRegionProviderStateStore`,
`RegionTranscriptionValidator`, derivative generation/content stores, and document-ingestion audit.
The acceptance reconstructor proves these source components compose, but its acceptance authority
is not ordinary execution authority.

## 3. Three independent governance states

The implementation shall keep these states separate:

- **Deployment readiness:** profile parses, credential is present, stores are durable, and build
  provenance matches. It authorises no egress.
- **Capability acceptance:** a create-once record accepts one exact capability identity. It
  authorises no particular evidence transmission.
- **Owner egress authorisation:** an owner authorises one exact evidence/capability/purpose
  operation. It does not accept a capability or prove deployment readiness.

All three must be satisfied. There is no generic “allow external AI” boolean, and generic OpenAI
provider acceptance cannot imply region-v5 acceptance.

## 4. Capability identity and acceptance contract

Add `RegionV5CapabilityIdentity`, a value object whose canonical fields are every fact in section 1
plus endpoint family, HTTP operation, renderer identity/version/DPI, region-derivation profile and
source-order authority. Canonical UTF-8 field-name order produces `capabilityDigest`. Construction
rejects any unsupported value; the production constant is the only ordinary executable identity.
Historical v4/profile-v1/adapter-3 and whole-document adapter-2 identities remain separately
decodable and cannot satisfy this object.

Add `RegionTranscriptionCapabilityAcceptanceRecord` with:

- record ID and `capabilityDigest`;
- lifecycle exactly `ACCEPTED`;
- the complete canonical capability identity;
- R6.9 authority/execution/response IDs and raw, structured, and complete-state digests;
- R6.9A/B/C commit and report digests;
- fidelity classification `PASS_FIDELITY` and exact count `24/24`;
- promoting source commit, owner reference, and timestamp.

`FileSystemRegionTranscriptionCapabilityAcceptanceStore` uses a dedicated configured root,
canonical encoding, payload checksum, `CREATE_NEW`, file `force(true)`, and directory fsync following
existing authority stores. An existing identical record is idempotently readable; the same record
ID with different bytes is a conflict. Records are never updated or deleted by runtime.

`RegionTranscriptionCapabilityAcceptanceEvaluator` accepts only a valid record whose capability
digest equals the runtime production identity, evidence-chain constants match, lifecycle is
`ACCEPTED`, and promoting commit is an ancestor/equal governed commit according to the configured
build policy. Missing, corrupt, mismatched, pending, or historical records yield a bounded
not-accepted reason. The production record is created only by the later promotion unit, never by
startup.

## 5. Owner external-egress authorisation

Add `ExternalRegionAcquisitionAuthorisation` as a create-once record binding:

- authorisation ID and owner principal;
- evidence artifact ID, exact source SHA-256 and byte length;
- purpose code `ORDINARY_REGION_TRANSCRIPTION`;
- provider `OpenAI`, exact `capabilityDigest`, endpoint operation and context policy;
- transmitted scope `DETERMINISTIC_REGION_CROPS_ONLY` (page context prohibited for this identity);
- issued-at and expires-at timestamps;
- use policy `SINGLE_EXECUTION` and deterministic execution ID;
- disclosure version confirming the owner was told the named provider receives rendered crops.

The default is absence/`NOT_AUTHORISED`. Configuration, acceptance, evidence selection, and prior
acceptance runs never synthesize this record. `FileSystemExternalRegionAcquisitionAuthorisationStore`
is create-once, checksum-protected and fsync-backed. It maintains a create-once consumption marker
bound to authorisation ID, execution ID, request digest and attempt ID. Once consumption or provider
attempt start is durable, the authorisation cannot initiate another provider request.

Owner API changes are two explicit steps:

1. existing acquisition `GET` returns an external-egress proposal with provider, evidence, scope,
   purpose, capability display name and disclosure—not an executable authorisation;
2. authenticated owner `POST .../acquisition/external-region-authorisations` confirms that proposal
   and creates the record. A subsequent `POST .../acquisition` supplies the authorisation ID and
   expected capability ID.

The server derives internal capability and source digests from the current proposal. The owner does
not type wire/schema IDs. Stale source, proposal, capability acceptance, expiry, principal mismatch,
or reuse fails closed. Revocation before consumption is an append-only revocation record; after
provider-attempt start it prevents downstream resumption only when safe and never implies retry.

## 6. Ordinary executor and source pipeline

Add `OrdinaryRegionTranscriptionAcquisitionExecutor`, implementing
`BoundAcquisitionCapabilityExecutor` with an exact region-v5 binding. It is distinct from
`RegionAcceptanceExecutionCoordinator` and accepts no acceptance-authority ID.

Its coordinator performs:

1. revalidate selected capability, accepted-capability record and owner authorisation;
2. use the already re-resolved `AuthoritativeAcquisitionInput` and recheck ID/SHA/length/media;
3. render every PDF page with `DeterministicSourcePageRenderer` at the locked profile;
4. derive each page graph using `DeterministicSourceRegionDeriver`;
5. fail closed on rendering, geometry or ambiguous-order outcomes;
6. construct `RegionTranscriptionRequest` with crops only and source-order list from the graphs;
7. derive canonical request digest, ordinary execution ID and attempt ID;
8. consume the owner authorisation and call the existing governed region execution coordinator;
9. resume only from its durable provider state after a response;
10. validate v5, reconstruct strictly by graph order, build the derivative and admit it.

The existing adapter remains the sole transport/request/parser implementation. Its profile lifecycle
may later permit `ACCEPTANCE_PENDING` only in the acceptance lane and `ACCEPTED` only when the
ordinary coordinator supplies a matching accepted-capability evaluation. The adapter itself does
not decide acceptance. Historical profiles retain their existing constructors and decoding.

## 7. Attempt and retry rules

Reuse the stage discipline of `FileSystemFidelityFirstAttemptLedger` and the exact-response durability
of `FileSystemRegionProviderStateStore`, but namespace ordinary execution identities by purpose and
authorisation. Required durable stages are `PREFLIGHT_PASSED`, `SOURCE_RETRIEVED`,
`REQUEST_PREPARED`, owner-authorisation consumption, `PROVIDER_ATTEMPT_STARTED`, and
`PROVIDER_RESPONSE_RECEIVED`.

The provider-attempt marker is forced before transport. The raw response is forced before parsing or
validation. On restart, an existing response is recovered without transport. Attempt-start without
a durable response is `OUTCOME_UNKNOWN` and never automatically retryable. Timeout or transport
exception after attempt start has the same disposition. A retry requires a separate, explicit owner
retry action producing a new authorisation and execution identity while referencing the prior
unknown/failed attempt; no retry is part of ordinary execution itself.

## 8. Reconstruction and derivative model

Add `RegionTranscriptionDerivative`, with canonical serialization preserving:

- source evidence ID/SHA and media facts;
- page representation IDs, dimensions, renderer identity and pixel digests;
- region IDs, bounds, crop digests, structural classes and geometry profile;
- exact transcription, status, uncertainty spans and visual observations;
- provider-returned ordinal as provenance only;
- Parker source ordinal and graph identity as reconstruction authority;
- provider/model/profile/adapter, schema/wire/instruction and processing identities;
- request/execution/attempt IDs and request digest;
- response ID and provider-state record/reference/digests;
- capability acceptance record/digest and owner authorisation ID;
- validation/reconstruction disposition and normal Parker timestamps.

`RegionSourceOrderReconstructor` consumes the validated result plus the exact graphs used to create
the request. It requires identical region sets, unique IDs, and a complete unambiguous graph; it
orders only by Parker source order. Provider ordinals are never consulted for ordering. Ambiguity,
missing/extra/duplicate regions, or graph drift yields a reviewable failed execution state and no
usable reconstructed derivative.

The authoritative evidence is never replaced. The derivative is a child of
`DerivativeParentReference.RootEvidenceArtifact` with a distinct kind such as
`EXTERNAL_REGION_TRANSCRIPTION_V5`.

## 9. Admission and partial outcomes

Extend the existing `DerivativeGenerationCoordinator` with a typed
`ValidatedRegionTranscriptionAdmission` rather than routing region data through
`OcrStructuredValidationOutcome`. Admission uses the existing derivative-generation storage,
derivative-content storage and `DocumentIngestionAudit`: publish canonical derivative content first,
prepare the generation record, audit authorisation, publish, and audit admission. Producer and
transformation history carry all identities in section 8.

Only a fully validated and deterministically reconstructed result has operational outcome `USABLE`.
Raw/parse/validation/order failures remain in the attempt/provider-state stores and are exposed as a
review status linked to the execution; they are not admitted as authoritative reconstructed content.
If reviewable failed content is later needed, it must be an explicitly non-usable diagnostic
derivative, never silently promoted. This reuses Parker's existing evidence/derivative universe and
does not create evidence, memory, or knowledge directly.

## 10. Catalogue, routing and owner workflow

Add a distinct catalogue ID, `openai-gpt-5.6-sol-region-v5`, projected as available only from a
successful accepted-capability evaluation plus deployment readiness. Its provider configuration is
extended (or wrapped by a region-specific governed configuration value) to bind endpoint operation,
wire, renderer, geometry and acceptance-record digest in addition to existing fields.

The historical whole-document catalogue entry remains distinct. It cannot share configuration
identity or executor binding with region-v5. The router continues to make deterministic technical
selection; external eligibility also requires an explicit authorisation matching the source and
capability. Evaluation without authorisation returns a proposal requiring external egress, not a
selected executable decision. Execution re-routes with the exact verified authorisation and rejects
stale decisions. There is no fallback after the owner selects region-v5.

Minimum owner flow:

1. select authoritative evidence;
2. inspect Parker's acquisition proposal and external disclosure;
3. explicitly authorise one region-v5 acquisition;
4. execute the exact proposal once;
5. view admitted derivative or bounded recovery/review status.

## 11. Runtime composition and configuration

`ParkerRuntime` later adds:

- acceptance-record and owner-authorisation roots/loaders;
- `RegionTranscriptionCapabilityAcceptanceEvaluator`;
- ordinary execution identity/attempt composition using existing ledger and provider-state roots;
- renderer, region deriver and source-order reconstructor;
- `OrdinaryRegionTranscriptionAcquisitionCoordinator` and executor registration;
- typed region derivative admission backed by existing generation/content/audit stores;
- owner operations and HTTP endpoints for proposal, authorisation and execution.

Startup requires exact build provenance, configured writable dedicated roots, existing governed
attempt/provider-state and derivative stores, a valid accepted-capability record, valid profile and
credential. Any missing dependency leaves the capability unavailable; startup never creates
acceptance or owner authorisation.

Minimum configuration adds only dedicated acceptance-record and owner-authorisation roots plus an
explicit ordinary-region feature enablement. The provider credential remains deployment-local.
Feature enablement means “compose if all governance is valid,” not accepted or authorised. The
three states in section 3 remain independent.

## 12. Fail-closed and security rules

Execution rejects missing/corrupt/mismatched authorisation; source ID/SHA drift; capability,
provider/model, adapter/profile, schema/wire, instruction, processing, renderer or geometry mismatch;
missing stores/credential; consumed/revoked/expired authorisation; ambiguous order; and non-durable
attempt/provider state. No mismatch invokes transport.

There is no silent fallback to whole-document OpenAI, native extraction, local OCR, Claude or
Poppler. A different acquisition method requires a new explicit router decision and, where relevant,
new owner action.

Requests retain `store=false`, reasoning `none`, original image detail and region-only crops. No
credential, header or secret enters records, logs or derivatives. The UI names OpenAI and shows the
transmitted scope before authorisation. Request provenance records crop identities/digests without
duplicating secrets or treating provider state as authoritative evidence.

## 13. Historical compatibility

Wire v4 remains decodable; adapter 3.0.0 and profile v1 remain identifiable; whole-document adapter
2.0.0 and its profile remain separately representable. Existing authorities, attempts, provider
state and the R6.9 malformed-schema assessment are untouched. New record schemas use new type and
file suffix identities, so old records are neither rewritten nor reinterpreted as v5 acceptance.

## 14. Complete static execution trace

| Arrow | Concrete resolution | Status |
|---|---|---|
| Owner request → proposal | Existing owner HTTP acquisition route and `GovernedAcquisitionOwnerWorkflow`, extended with external proposal view | Extend |
| Proposal → egress authorisation | New owner POST, `ExternalRegionAcquisitionAuthorisation`, create-once store | New |
| Authorisation → acquisition evaluation | Workflow verifies source/capability binding and passes a verified authorisation, never a boolean | Extend |
| Evaluation → accepted selection | `ProductionAcquisitionCapabilityCatalogue` + acceptance evaluator + existing router | Extend |
| Selection → executor | Existing execution coordinator exact binding; register `OrdinaryRegionTranscriptionAcquisitionExecutor` | Extend/new |
| Executor → custody bytes | Existing coordinator resolution and `AuthoritativeAcquisitionSourceResolver` | Existing |
| Bytes → pages | Existing `DeterministicSourcePageRenderer`, locked PDFBox 3.0.7/300 DPI | Existing |
| Pages → regions/order graph | Existing `DeterministicSourceRegionDeriver` | Existing |
| Regions → request | New ordinary coordinator constructs the existing `RegionTranscriptionRequest` | New |
| Request → durable attempt | Existing ledger semantics, purpose/authorisation-bound identity | Extend |
| Request → adapter | Existing `OpenAiRegionTranscriptionAdapter` under accepted-capability gate | Existing/extend lifecycle |
| Adapter → raw durability | Existing `FileSystemRegionProviderStateStore` before parse | Existing |
| Raw state → validated v5 | Existing parser and `RegionTranscriptionValidator` | Existing |
| Validated blocks → source order | New `RegionSourceOrderReconstructor` using existing R6.2 graphs | New |
| Reconstruction → derivative | New `RegionTranscriptionDerivative` canonical model | New |
| Derivative → admission | Typed extension to existing `DerivativeGenerationCoordinator` and existing stores/audit | Extend |
| Admission → owner result | Existing owner execution view/HTTP response extended with execution/review status | Extend |

Every arrow has an existing component or a named bounded addition; no governance transition is
undefined.

## 15. Implementation plan

Use **one implementation unit** so lifecycle, authorisation, execution, durability, reconstruction,
admission and owner API cannot land in contradictory partial states.

The unit modifies `EvidenceAcquisition` configuration types, `ProductionAcquisitionCapabilityCatalogue`,
`GovernedAcquisitionOwnerWorkflow`, `GovernedAcquisitionIntegration`, `ParkerRuntime`,
`ParkerRuntimeConfig`, `OwnerEvidenceHttpServer`, `OpenAiRegionTranscriptionAdapter`, and
`DerivativeGenerationCoordinator`. It adds capability identity/acceptance storage and evaluator,
owner authorisation storage/coordinator, ordinary region coordinator/executor, source-order
reconstructor, canonical derivative model and focused tests.

Test gates cover canonical/create-once records; capability-specific lifecycle isolation; source and
identity mismatch rejection; owner disclosure/authorisation/expiry/revocation/single-use; offline
ordinary selection and stop-before-transport composition; attempt recovery/no automatic retry;
renderer/geometry/order; v5 validation and R6.9A/B regressions; derivative admission/audit; no
fallback; historical decoding; runtime config; full Ubuntu suite; and `git diff --check`. Fake
transport must assert zero calls for every preflight failure.

Production mutation boundary: implementation and offline verification only, then commit/push. No
production record, configuration, credential, image or container change occurs until a later
explicit promotion/deployment unit. Stop if safe execution would require a generic egress boolean,
historical mutation, acceptance-authority reuse, ambiguous capability binding, non-durable provider
state, automatic retry, or a parallel derivative/evidence store.

The subsequent promotion unit creates the reviewed acceptance record, builds an image from the exact
commit, installs the new roots/configuration, deploys once, and proves provider-free ordinary
composition before any separately owner-authorised ingestion.
