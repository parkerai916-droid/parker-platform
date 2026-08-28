# Fidelity-First External Transcription — Current Path Assessment

## Status, authority, and classification

**Unit:** FA.9.4P-F. **Assessment date:** 2026-08-28. **Status:** governance/architecture assessment only.

This document authorises no provider request, evidence acquisition, derivative generation, runtime change, profile acceptance, credential use, deployment, retry, fallback, or use of Unit O.5. It preserves Unit O history and the production checkpoint `0dd5f251df0a53df986f0f9b3e4fc65c6308d512`.

Statements are labelled:

- **OFFICIAL CURRENT PROVIDER FACT** — supported by current official OpenAI documentation accessed on 2026-08-28.
- **CURRENT PARKER IMPLEMENTATION FACT** — established by repository inspection at `b7ee69879595bd343163b302fccba1e361687afc`.
- **RECOMMENDATION** — proposed for a later separately authorised implementation or acceptance unit.
- **REQUIRES EMPIRICAL PARKER ACCEPTANCE** — capability exists publicly but Parker has not accepted its fidelity for evidence transcription.

## Official OpenAI sources inspected

All provider claims use official OpenAI documentation only:

- [Models catalogue](https://developers.openai.com/api/docs/models)
- [Model comparison](https://developers.openai.com/api/docs/models/compare)
- [GPT-5.6 Sol](https://developers.openai.com/api/docs/models/gpt-5.6-sol)
- [GPT-5.6 Terra](https://developers.openai.com/api/docs/models/gpt-5.6-terra)
- [GPT-5.6 Luna](https://developers.openai.com/api/docs/models/gpt-5.6-luna)
- [Responses API create reference](https://developers.openai.com/api/reference/cli/resources/responses/methods/create)
- [File inputs](https://developers.openai.com/api/docs/guides/file-inputs)
- [Images and vision](https://developers.openai.com/api/docs/guides/images-vision)
- [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)
- [Data controls](https://developers.openai.com/api/docs/guides/your-data)

Official documentation did not establish a separate PDF page-count ceiling or a dated GPT-5.6 Sol snapshot identifier on the inspected pages. This assessment does not invent either fact.

## Current provider capability

### Model candidates

**OFFICIAL CURRENT PROVIDER FACT.** The current catalogue presents GPT-5.6 Sol as the flagship model, GPT-5.6 Terra as the intelligence/cost balance, and GPT-5.6 Luna for cost-sensitive high-volume work. All three accept image input, support the Responses API and Structured Outputs, expose a 1,050,000-token context window and 128,000 maximum output tokens, and support reasoning efforts `none`, `low`, `medium`, `high`, `xhigh`, and `max`.

| Candidate | Official positioning | Parker assessment |
|---|---|---|
| `gpt-5.6-sol` | Flagship model for complex professional work | **Primary.** Fidelity has priority over price; this is the strongest documented general multimodal candidate. |
| `gpt-5.6-terra` | Balances intelligence and cost | Secondary cost-balanced candidate, only after independent source-class acceptance. |
| `gpt-5.6-luna` | Cost-sensitive, high-volume | Plausible smaller candidate, but not selected merely for price. |

**RECOMMENDATION.** Use `gpt-5.6-sol` with `POST /v1/responses` for the next controlled acceptance. Use `reasoning.effort: "none"` initially: it is the least reasoning mode officially supported and best matches literal non-reasoning transcription. Acceptance must compare `none` with `low` only if `none` materially harms perception or schema compliance; more reasoning is not presumed more faithful.

**REQUIRES EMPIRICAL PARKER ACCEPTANCE.** Vision support and flagship positioning do not prove literal transcription, punctuation, layout, handwriting, table, or uncertainty fidelity.

### Alias and snapshot

**OFFICIAL CURRENT PROVIDER FACT.** OpenAI describes snapshots as the way to lock model behaviour. The inspected GPT-5.6 Sol page lists `gpt-5.6-sol` but exposes no distinct dated snapshot identifier.

**RECOMMENDATION.** Use `gpt-5.6-sol` only for the bounded assessment execution if no dated snapshot is officially available to the Parker project. Record the exact provider-reported model identifier. Before ordinary acceptance, prefer a dated GPT-5.6 Sol snapshot if OpenAI exposes one, repeat acceptance against it, and pin that identifier. A floating alias must not silently inherit acceptance.

## Authoritative PDF and image input

**OFFICIAL CURRENT PROVIDER FACT.** Responses accepts `input_file` as inline Base64 `file_data`, a Files API ID, or an external URL. For PDFs on vision-capable models, OpenAI extracts both text and page images and sends both to the model. PDF page-image detail can be `auto`, `low`, or `high`; for GPT-5.6 and later, `auto` uses `high`, while extracted text remains included. Each file and the combined files in one request must be under 50 MB. PDF text and page images both consume context tokens. The inspected documentation gives no distinct page-count limit or explicit promise that provider output will expose page numbers/order automatically.

**OFFICIAL CURRENT PROVIDER FACT.** Responses accepts direct image inputs. GPT-5.6 Sol/Terra/Luna support `original` detail; official guidance recommends `original` for OCR and fine visual detail. Vision can still make errors, including with small or rotated text and precise spatial relationships. Image requests support up to 512 MB total payload and 1,500 images, subject to model/context and Parker's tighter bounds.

**RECOMMENDATION.** Send the one verified authoritative custody object directly:

`authoritative EvidenceArtifact bytes -> byte-exact request representation -> inline Base64 input_file/input_image -> Responses API`

- PDF: inline `input_file.file_data`, explicit `detail: "high"`.
- Standalone image: inline data URL `input_image`, explicit `detail: "original"`.
- Do not run local/native OCR first and do not use File Search.
- Retain Parker's tighter source/page/output/time bounds; reduce the effective PDF bound to below the official 50 MB per-request ceiling after Base64/request-overhead checks.
- Treat OpenAI's extracted-text-plus-rendered-page processing as provider-side processing of the direct source, not as byte-exact visual presentation or a new authoritative source.

**OFFICIAL CURRENT PROVIDER FACT.** Inline Base64 means `/v1/files` object creation is not required. **RECOMMENDATION.** Keep inline input so there is no separately persistent Files API object or deletion lifecycle. If a later constraint requires `/v1/files`, that is a new privacy/lifecycle decision: set `expires_after` where supported, delete immediately after the governed attempt, and record that Files objects otherwise persist until deleted.

## Structured transcription and uncertainty

**OFFICIAL CURRENT PROVIDER FACT.** Structured Outputs on the Responses API use strict JSON Schema through `text.format`; current GPT-5.6 models support it. Schema adherence guarantees shape, not factual or transcription accuracy. PDF/image input and Structured Outputs can be used in the same Responses request.

**RECOMMENDATION.** Retain one strict response object with:

- document warnings and requested/returned page accounting;
- ordered `pages[]` with `pageNumber` and page outcome;
- ordered `blocks[]` with literal text and a bounded block kind;
- optional normalized source-region coordinates only where the model can report them under an accepted convention;
- uncertainty entries for character, word, block, layout/order, and illegibility;
- explicit `UNREADABLE`, `PARTIALLY_LEGIBLE`, `UNCERTAIN`, `ILLEGIBLE`, `FAILED`, and `NOT_RETURNED` outcomes;
- no provider/model/profile facts supplied by the model payload when those facts are available authoritatively from the response envelope and Parker configuration.

The instruction must prohibit summarisation, paraphrase, grammar correction, legal interpretation, evidential assessment, contextual completion, semantic reconstruction, and guessed unreadable text. It must require omission or explicit uncertainty over fluent invention. Page and block arrays must be ordered, but ordering claims remain subject to source review.

## Privacy, retention, and residency

**OFFICIAL CURRENT PROVIDER FACT.** OpenAI states that API data is not used to train models unless the customer explicitly opts in. Default abuse-monitoring logs may retain customer content for up to 30 days, subject to stated exceptions. Eligible approved customers can use Modified Abuse Monitoring or Zero Data Retention.

**OFFICIAL CURRENT PROVIDER FACT.** `/v1/responses` is listed as having no application-state retention when `store=false`, subject to documented exceptions; default or `store=true` responses have at least 30 days of application-state retention. Background mode temporarily stores response data for polling. Prompt caching can retain encrypted key/value tensors in GPU-local storage for up to 24 hours. Image/file inputs undergo CSAM scanning and flagged material may be retained for manual review even under ZDR/MAM.

**RECOMMENDATION.** Keep `store:false`, foreground execution, no conversation, no background mode, no tools, no File Search, no Files API object, and no extended prompt-caching feature. Before real evidence, verify and record the actual project-level training opt-in, ZDR/MAM status, image/file-input treatment, and regional settings; profile prose is not proof of account configuration.

**OFFICIAL CURRENT PROVIDER FACT.** Data residency is project-configured and eligibility-controlled. US and Europe offer regional storage and processing; Australia, Canada, Japan, India, Singapore, South Korea, the UK, and UAE are documented with regional storage but not regional processing. Non-US regions require approved retention controls; image support in listed non-US regions can require enhanced approval. System data and Structured Output schemas are outside customer-content residency.

**RECOMMENDATION.** Do not select a residency endpoint from geography alone. Before acceptance, the owner must choose an eligible project/region based on evidence privacy requirements and confirmed model/file-input support. The current global endpoint remains historical configuration, not a residency determination.

## Current Parker implementation

**CURRENT PARKER IMPLEMENTATION FACT.** The existing `OpenAiResponsesExternalTranscriptionAdapter` version `1.1.0` already:

- targets exactly `https://api.openai.com/v1/responses`;
- requires `store=false`, foreground/non-streaming operation, and bearer authentication;
- accepts verified PDF/image custody bytes through a byte-exact processing representation;
- sends PDFs as inline Base64 `input_file.file_data` and images as inline data URLs;
- applies `detail:"high"` to images but no explicit PDF detail;
- uses `gpt-4.1-mini` through the historical provider profile;
- uses strict Structured Outputs with `openai-literal-page-transcription-v2`;
- prohibits inference, correction, summarisation, analysis, and guessed unreadable text;
- carries requested/returned pages, page outcomes, warnings, and uncertainty spans;
- parses the provider response, records response ID and provider-reported model, but marks model snapshot `NotExposed`;
- makes one transport call and has no retry/provider-switch loop.

**CURRENT PARKER IMPLEMENTATION FACT.** `ExternalTranscriptionOwnerInvocationCoordinator` verifies custody bytes against the authoritative manifest, creates a byte-exact representation, invokes once, validates structured page accounting/provenance, and admits a new immutable derivative. Existing provider, processing, configuration-digest, page-accounting, uncertainty, fidelity, completeness, and correlation provenance are substantially reusable. Existing exact-generation human-verification records and downstream selection remain suitable for targeted source-vs-transcription review.

**CURRENT PARKER IMPLEMENTATION FACT.** Provider-profile schema v2 hard-codes the literal-v2 profile and byte-exact processing identity, allows the lifecycle `DISABLED`, `CONFIGURATION_READY`, `ACCEPTANCE_PENDING`, `ACCEPTED`, `SUSPENDED`, and ordinary composition fails closed unless `ACCEPTED`. This lifecycle remains suitable and must not be set to `ACCEPTED` before controlled acceptance.

**CURRENT PARKER IMPLEMENTATION FACT.** The durable Unit O attempt ledger has the required monotonic stages, one-attempt guard, checksummed append/replace durability, identity binding, and terminal failure. It is currently acceptance support under `tests/runtime`, not the ordinary production invocation path. Its discipline is suitable, but a new FA execution must use a new immutable FA-specific identity/allocation and must not reuse O.5.

## Minimum future Parker changes

**RECOMMENDATION.** Implement only the following bounded changes before controlled acceptance:

1. Add a new provider-profile/schema version and transcription-profile identity for the current candidate. Keep historical profiles and Unit O records readable and unchanged. Configure `gpt-5.6-sol`, explicit snapshot policy, current review date, current limits, `store=false`, reasoning `none`, and verified privacy/residency facts. Leave lifecycle at `ACCEPTANCE_PENDING`.
2. Update request construction to include `reasoning:{"effort":"none"}`, explicit PDF `detail:"high"`, image `detail:"original"`, and a conservative output-token bound. Continue inline Base64 direct-source input with no Files API object.
3. Version the strict schema/instruction to add ordered blocks, bounded block/order metadata, and character/word/layout uncertainty without weakening the literal boundary. Recompute and govern both digests.
4. Record a pinned dated model snapshot when available; otherwise truthfully record the provider-reported model and the fact that the acceptance used a floating alias.
5. Move or reproduce the Unit O ledger discipline in a production-owned reusable component and bind the separately authorised FA attempt before any network call. Preserve the exact one-request/no-retry semantics.
6. Register one accepted external capability projection and executor behind the governed acquisition workflow only after configuration acceptance. No provider marketplace or multi-provider framework is needed.
7. Expose the exact generation, page/block provenance, uncertainty, provider/model/profile, and review state through the existing review path; no broad UI redesign is required.

## Router correction

**CURRENT PARKER IMPLEMENTATION FACT.** `DeterministicEvidenceAcquisitionRouter.route` immediately narrows searchable-text sources to eligible `DIRECT_NATIVE_EXTRACTION` candidates. Its later tie-breakers prefer authoritative/byte-exact representation and then local execution. `ProductionAcquisitionCapabilityCatalogue.create()` currently registers only native and local OCR unless an external projection is supplied, and ordinary composition supplies none.

**RECOMMENDATION.** Amend `src/runtime/DeterministicEvidenceAcquisitionRouter.kt`, the provider-neutral capability/selection types in `src/interfaces`, `src/runtime/ProductionAcquisitionCapabilityCatalogue.kt`, composition in `src/composition/ParkerRuntime.kt`, and their focused tests so that:

1. eligibility includes source-class-specific empirical fidelity acceptance for the exact capability/configuration;
2. all eligible candidates are first filtered/ranked by demonstrated fidelity suitability;
3. native searchability is only a source characteristic, never an early return;
4. transformation and egress/locality tie-break only among candidates with accepted materially equivalent fidelity;
5. absent/expired/conflicting fidelity evidence produces an explicit no-selection/indeterminate outcome;
6. selection still occurs once, with no execution retry or fallback.

Native extraction remains available only for bounded source classes independently demonstrated faithful. Its failed real-PDF acceptance remains negative evidence; native-PDF remediation does not resume.

## Next controlled acceptance

**RECOMMENDATION.** After the bounded implementation above, create a separately authorised unit: **FA.9.4P-I — GPT-5.6 SOL FIDELITY-FIRST EXTERNAL TRANSCRIPTION IMPLEMENTATION AND CONTROLLED ACCEPTANCE**.

Use one immutable execution authority per exact request and exact owner-approved evidence ID. A minimal representative matrix is:

1. the known searchable PDF that defeated native extraction;
2. one clean scan;
3. one degraded scan;
4. one mixed-layout/table document;
5. one handwriting or mixed-handwriting source, only if separately identified and authorised.

Run no automatic retries, fallback, provider switching, or alias comparison under a single authority. If comparison of `none` and `low`, an alias and snapshot, or Sol and Terra is required, each request needs distinct prior authority and immutable accounting.

Direct human source-vs-transcription review must score literal text, reading order, Unicode/punctuation, numeric/date/identifier fidelity, omissions, inventions, page accounting, layout relationships, tables, handwriting, and uncertainty honesty. Fluency, token completeness, schema validity, and page counts are necessary but not sufficient. Review verifies the derivative against the already selected source; it does not select evidence and does not promote the derivative over the immutable original.

## Determination

**RECOMMENDATION.** GPT-5.6 Sol through foreground `POST /v1/responses`, inline direct authoritative PDF/image bytes, `store:false`, PDF high detail, image original detail, reasoning `none`, and versioned strict Structured Outputs is the one primary current candidate for Parker's next controlled acceptance.

**REQUIRES EMPIRICAL PARKER ACCEPTANCE.** No claim is made yet that GPT-5.6 Sol is sufficiently faithful for any Parker source class. No profile is accepted, no production routing changes, and no provider execution are authorised by this assessment.
