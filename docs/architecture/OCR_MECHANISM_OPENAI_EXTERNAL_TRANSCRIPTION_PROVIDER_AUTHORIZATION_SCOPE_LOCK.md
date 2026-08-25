# OCR Mechanism — OpenAI External Transcription Provider Authorization Scope Lock

## Status

**Adopted governance.** This document authorises future implementation planning
for one bounded OpenAI external-transcription provider behind the existing
`OcrProviderAdapter`. It does not implement or enable an adapter, network path,
credential, provider configuration, deployment, or live submission.

## 1. Purpose and governed sequence

The sole authorised workflow is:

`owner-selected EvidenceArtifactId`
→ `Permission Engine authorisation`
→ `Evidence Custodian retrieval`
→ `authoritative-manifest verification`
→ `bounded processing representation`
→ `OpenAI OcrProviderAdapter`
→ `validated OcrRecognitionResult`
→ `durable Tier B derivative admission`
→ `separate later analysis`
→ `human review`.

This is provider- and purpose-specific authority, never generic authority for
cloud or AI providers to access Parker evidence.

## 2. Provider and API boundary

OpenAI is authorised only through an approved OpenAI API provider profile and a
single concrete adapter implementing `OcrProviderAdapter`. The initial
implementation plan must use a stateless `POST /v1/responses` transcription
operation with `store=false`, no conversation state, no File Search, no web
search or browsing, no Code Interpreter, no provider-side agents, no MCP, no
secondary retrieval, and no provider tool execution.

The adapter is the sole location permitted to contain OpenAI-specific HTTP,
authentication-header construction, endpoint configuration, model identity,
response parsing, and error mapping. No OpenAI-specific type or vocabulary may
leak into provider-neutral OCR, Evidence Custodian, Memory, Knowledge, or
analysis contracts. No second provider abstraction, generic cloud-provider
registry, or unrestricted network client is authorised.

Structured output establishes a deterministic transport/result shape. It does
not establish transcription truth, fidelity, or completeness.

## 3. Capability only; no Parker authority

OpenAI may only return a candidate transcription of the supplied processing
representation. It receives no authority to accept, identify, retrieve,
enumerate, modify, overwrite, replace, obscure, or delete evidence; write to the
Evidence Store; retain a Parker storage handle; write Memory or Knowledge;
assign evidential state or canonical truth; authorise actions; select other
evidence; perform substantive analysis; or initiate another provider call.

## 4. Explicit owner selection

External transcription is a distinct owner-selected operation. It must not be
triggered merely because OCR is required, Tier A returns `REQUIRES_OCR`, local
OCR fails or performs poorly, analysis is requested, evidence is uploaded or
retrieved, a derivative is missing, Parker predicts better external quality, or
Parker restarts.

Local OCR and external transcription remain separate owner choices. Neither is
an automatic prerequisite or fallback for the other. The owner may select
external transcription directly.

## 5. Permission Engine and Authorization Purpose

External disclosure must pass through Parker's existing sole Permission Engine
before source retrieval or network submission. The operation reuses
`PermissionAction.EXECUTE`, `ResourceType.DOCUMENT`, and the existing Evidence
Intelligence invocation authority, and additionally carries the fresh, active,
registered per-request purpose:

`evidence-intelligence.external-transcription`

Its sole meaning is to submit the bounded processing representation of the one
owner-selected EvidenceArtifact to the authorised OpenAI transcription provider
for faithful transcription and return the result to Parker's governed OCR
admission path.

The purpose is carried on the immutable `ExecutionRequest`, freshly stated for
each call, never inferred from endpoint, provider configuration, media type,
call stack, or prior approval. It must be active in the existing
`AuthorizationPurposeRegistry` and explicitly covered by policy. An absent,
unknown, retired, mismatched, or unauthorised purpose fails closed before
retrieval or egress. No new `PermissionAction`, `ResourceType`, Permission
Engine, or parallel request type is authorised.

## 6. Binding to one selected source

The initial operation processes exactly one governed `EvidenceArtifactId`. That
identifier is carried unchanged through owner selection, authorisation
correlation, custody retrieval, manifest verification, representation
provenance, derivative admission, and audit. The adapter cannot replace it,
select another artefact, or retrieve anything independently.

## 7. Custody-first and manifest verification

Before any external content transfer Parker must:

1. resolve the configured owner structurally;
2. obtain Permission Engine approval for the external purpose;
3. retrieve the selected source through Evidence Custodian;
4. retrieve the authoritative source manifest;
5. verify byte length and SHA-256 against that manifest;
6. validate media, page, file, representation, and resource bounds; and
7. construct only the required processing representation.

Failure at any step causes no submission and no durable derivative. The provider
receives no Parker path, store credential, storage handle, write interface, or
Evidence Custodian reference.

## 8. Processing representation

A processing representation is request-scoped material derived from the
manifest-verified source solely for this call. It is not a replacement source,
new authoritative EvidenceArtifact, canonical content, or independent evidence
object merely because it exists.

### 8.1 Direct PDF/image submission

Where the approved Responses API profile supports the source PDF/image
representation directly, Parker may and should prefer a bounded byte-exact copy
when technically appropriate, avoiding unnecessary rasterisation. Provenance
records source `EvidenceArtifactId`, manifest SHA-256, media type, requested page
scope, byte-exact-copy status, byte length, and representation SHA-256. A
full-source byte-exact copy's digest must equal the manifest digest.

### 8.2 Material transformation

Rasterisation, page extraction, conversion, rotation, scaling, compression,
colour conversion, or cropping must not be described as byte-identical. Each
transformed representation records:

- source `EvidenceArtifactId` and manifest SHA-256;
- source page or inclusive range;
- output media type, byte length, and SHA-256;
- transformation mechanism identity and version;
- material DPI, dimensions, rotation, colour, scaling, crop, and compression
  parameters where applicable;
- creation time and processing-profile identity.

These facts belong to the eventual derivative's processing provenance. A
temporary representation need not become a permanent EvidenceArtifact.
Temporary bytes are bounded, isolated, and removed on success, rejection,
timeout, cancellation, and failure.

## 9. Transcription is not analysis

The provider instruction is limited to faithful transcription. It may identify
text, page association, uncertainty, illegibility, missing recognition, and
warnings. It must not request summary, interpretation, inferred intent,
credibility, legal significance, factual-conflict resolution, wording
improvement, silent substantive correction, translation, speculation to fill
gaps, or substantive analysis.

Where content cannot be read confidently, uncertainty or illegibility is
disclosed rather than guessed. Prompt and structured-output rules are part of
the governed transcription configuration profile.

## 10. Fidelity

`OCR_TRANSCRIPTION_FIDELITY_VERIFICATION_AMENDMENT.md` governs. Ordinary OpenAI
transcription output is `UNVERIFIED_LITERAL_TRANSCRIPTION` unless separately
governed verification establishes another classification for an identified
scope. Provider success, model confidence, fluency, or silence about errors does
not establish `VERBATIM`. Deliberate normalisation is `NORMALISED`; unsupported
reconstruction is `INFERRED_RECONSTRUCTION` and must be disclosed. The provider
should be instructed not to reconstruct illegible content.

## 11. Structured result and page accounting

The future adapter must require a structured result capable of representing
requested page scope, returned page scope, page number, transcription text,
uncertain and illegible content, page outcome, warnings, degradation, and
completeness.

Every requested page resolves to one of:

- `TRANSCRIBED`;
- `TRANSCRIBED_WITH_QUALIFICATIONS`;
- `ILLEGIBLE_OR_NO_RECOGNISABLE_CONTENT`;
- `FAILED`;
- `NOT_RETURNED`.

Returned pages are reconciled against requested and submitted scope. Missing,
failed, partial, unreturned, or unaccounted pages prohibit an unqualified
complete outcome. A usable qualified response may enter the existing
`PARTIAL_OR_DEGRADED` path with mandatory reasons and page accounting.
Malformed or contradictory accounting prevents clean success and, where no
truthful usable partial result can be formed, prevents admission. Request page
count is input scope, never coverage proof.

## 12. Durable provenance and model identity

Provider identity and the exact model identifier reported by OpenAI for the
completed request are mandatory. Where OpenAI exposes an immutable model
snapshot or version applicable to that request, Parker preserves it. Where no
separate snapshot or version is exposed, Parker records that absence truthfully.
Absence of a separate snapshot/version does not by itself prevent durable
admission where the provider-reported model identifier, provider response or
request/response correlation identifier, transcription configuration/profile,
processing-representation provenance, page accounting, and all other mandatory
provenance are present.

Parker never fabricates, infers, substitutes, or manufactures a model
version/snapshot. Missing provider identity, model identifier, required
processing provenance, required page accounting, or another genuinely mandatory
fact fails closed. The adopted Tier B rule requiring a model version for every
model-backed generation is narrowly qualified for this provider only as stated
here; truthful absence is a governed value, never the fabricated string
`unknown`.

A durable external-transcription derivative retains, as applicable:

- authoritative `EvidenceArtifactId` and fresh `DerivativeGenerationId`;
- provider and adapter/mechanism identity and version;
- exact provider-reported model identifier and optional snapshot/version;
- provider response/correlation identifier;
- transcription configuration/profile and creation time;
- recognised text and page-associated segments;
- fidelity and outcome kind;
- requested and returned page coverage and per-page qualifications;
- warnings, degradation, uncertainty, completeness, and operational outcome;
- processing-representation provenance and actual transformations.

Provider confidence remains transient and is neither evidential confidence nor
proof of accuracy.

## 13. Durable generations and analysis selection

The existing Tier B OCR model remains the sole durable destination. Each
successful explicit run mints a fresh `DerivativeGenerationId`. A later
transcription neither overwrites, mutates, silently supersedes, nor becomes
authoritative over an earlier generation, and analysis never switches to it
automatically. Analysis retrieves the exact owner-selected
`EvidenceArtifactId` plus `DerivativeGenerationId`.

The derivative remains subordinate material, never original Evidence, Memory,
Knowledge, or canonical truth merely through admission.

## 14. Failure semantics

Authorisation denial; source or manifest failure; length or digest mismatch;
oversized input or output; unsupported media; timeout; network, authentication,
provider-policy, or provider-content rejection; malformed or incomplete
response; contradictory page accounting; missing pages; mandatory-provenance
failure; cancellation; and genuine implementation fault never produce false
durable success or mutate original evidence.

A usable partial response may be admitted only as `PARTIAL_OR_DEGRADED` with
truthful page and degradation disclosure. No automatic retry is authorised. A
later plan may propose tightly bounded transport retry only if it preserves one
owner invocation, records attempts, avoids duplicate publication, and cannot
create ambiguity about the response that produced the generation.

## 15. Responses API and network boundary

This document narrowly qualifies `OCR_MECHANISM_SCOPE_LOCK.md` §15 and
`OCR_MECHANISM_UNIT_12_RUNTIME_INVOCATION_SCOPE_LOCK.md` §14 only for the
OpenAI operation governed here. Initial outbound access is restricted to the
OpenAI API destination or destinations actually required by the approved
`/v1/responses` profile. General internet access, arbitrary redirects, browsing,
retrieval, tools, agents, MCP, provider-initiated secondary network activity,
and other endpoints are not authorised.

If an additional OpenAI-owned endpoint is later shown necessary, it requires
explicit assessment rather than inference from generic OpenAI authorisation.
Docling/RapidOCR remains local and network-disabled. No other provider or OpenAI
capability receives authority from this qualification.

## 16. Provider-enablement and privacy record

Deployment must not enable the provider until a dated, reviewable profile records
and verifies:

- exact OpenAI API product and credential-free endpoint identity;
- approved model identifier or model-selection rule;
- whether a suitable immutable snapshot exists and whether pinning is selected;
- current file/PDF/image and request limits;
- API data-use and training treatment;
- abuse-monitoring and data-retention treatment;
- whether Zero Data Retention or Modified Abuse Monitoring is actually available
  and enabled for the organisation/project;
- project/account controls affecting retention;
- `store=false` request behaviour and any remaining retention implications;
- applicable provider-side logging;
- authentication and secret-storage configuration;
- material regional or storage considerations;
- verification date, approving owner, and re-verification triggers.

This document claims neither universal Zero Data Retention nor that `store=false`
eliminates every provider retention path. Changeable provider facts belong in
the dated profile, supported by current primary provider materials, not frozen as
timeless constitutional facts.

Material changes to provider terms, product behaviour, endpoint, organisation or
project configuration, model-selection rule, limits, retention, or data use
require re-verification. Failure to establish or renew the profile disables
external submission without fallback.

## 17. Authentication and secrets

The adapter may use only the authenticated API mechanism approved by the provider
profile and Parker secret-management governance. Credentials are adapter-internal
and revocable. No key or token appears in repository files, evidence,
derivatives, prompts, logs, audit content, UI, errors, processing
representations, or URLs. Missing or revoked credentials fail closed.

## 18. Logging and audit

Bounded records may contain Evidence and Derivative Generation identifiers,
authorisation purpose, credential-free provider/model identity, timestamps,
page scope, outcome/error classification, a sanitised correlation or response
identifier, and representation digest/media type.

They must not contain full source bytes, complete transcription text, raw
provider request/response bodies, authentication material, Parker paths, or
unrelated Evidence, Memory, or Knowledge. Owner errors disclose safe
classifications, never secrets, paths, raw stack traces, or document content.

## 19. Separate later analysis

External-transcription authorisation does not authorise external or local
substantive analysis. Only after durable admission may a separately authorised
analysis retrieve the exact selected generation. Its output remains
provider-generated material for human review, does not mutate source or
transcription, establishes no canonical truth, and writes neither Memory nor
Knowledge automatically.

Tier B §33 is qualified only to distinguish this authorised transcription call
from external reasoning-provider submission. Its prohibition on external
reasoning or analysis remains unchanged.

## 20. Local OCR preservation

Docling/RapidOCR remains available, local, network-disabled, and owner-selectable.
It is not required before external transcription. Neither provider is an
automatic fallback for the other. Selection remains an explicit owner action.

## 21. Bounds and provider-specific lower limits

Parker retains its own governed ceilings: no more than the existing governed
page and image-processing limits, one concurrent external transcription
initially, an explicit timeout no greater than the existing OCR ceiling, bounded
output no greater than the existing OCR output ceiling, and bounded temporary
workspace.

For every provider-specific dimension the adapter enforces the lower of Parker's
ceiling and the current approved OpenAI limit in the dated provider profile. The
initial profile must use the currently verified OpenAI PDF/file request limit,
not assume Parker's general 64 MiB upload ceiling is accepted by OpenAI. Exact
changeable OpenAI commercial/API limits are not constitutional facts and are not
hard-coded here.

## 22. Constitutional self-certification

This operation satisfies CDR-005: external disclosure is consequential; one
existing `ExecutionRequest` and Permission Engine remain the sole authority;
`EXECUTE`/`DOCUMENT` are reused; purpose is explicit, registered, request-scoped,
and policy-evaluated; rejection prevents execution; no component self-authorises;
the owner controls provider selection; capability receives no authority; and
audit and fail-closed behaviour are mandatory.

The constitutional tests pass:

- **Evidence authority:** none; Evidence Custodian remains exclusive.
- **Truth authority:** none; structured output and durability establish no truth.
- **Automatic egress:** prohibited structurally and normatively.
- **Generic OpenAI authority:** none; provider, endpoint, purpose, and operation
  are narrow.
- **External analysis:** not authorised.
- **Memory or Knowledge authority:** none.
- **Automatic provider selection:** prohibited.
- **Derivative supersession:** prohibited; exact generation selection required.
- **Original mutation:** structurally unavailable and expressly prohibited.

No Constitution, Permission Engine, Resource Type, action-vocabulary, Evidence
Custodian, Evidence Intelligence, Memory, or Knowledge redesign is required.

## 23. Explicit non-authorities

This document does not authorise generic cloud AI access; another provider;
ChatGPT consumer UI automation; OpenAI as custodian; arbitrary network access;
redirects; browsing, tools, agents, MCP, or secondary retrieval; automatic
egress, provider selection, or fallback; external analysis; summary or
interpretation during transcription; Memory or Knowledge writes; QMD/RKS
indexing; canonical truth or evidential-state assignment; source mutation,
replacement, or deletion; derivative overwrite or supersession; fabricated
provenance; content logging; unverified retention assumptions; deployment,
credentials, runtime configuration, or live submission.

## 24. Implementation gate

Implementation planning may begin after this scope lock and the fidelity
amendment are adopted. Implementation remains separately governed. No unit may
combine implementation planning with provider enablement, secrets, deployment,
or live evidence submission.

## Final determination

**ADOPTED — IMPLEMENTATION NOT AUTHORISED.** The bounded OpenAI transcription
capability is constitutionally compatible and authorised for a later, separate
implementation-planning unit only.
