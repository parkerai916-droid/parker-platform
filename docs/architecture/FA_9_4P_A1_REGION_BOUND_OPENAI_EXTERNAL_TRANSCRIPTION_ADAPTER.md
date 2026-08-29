# FA.9.4P-A1 Region-Bound OpenAI External Transcription Adapter

**Unit:** FA.9.4P-A1E-R6.4

**Status:** Implemented and offline-verified on Ubuntu. The profile remains `ACCEPTANCE_PENDING`. No provider request, Claude request, credential load from production, acceptance execution, durable response persistence, OCR, comparison, runtime rebuild, production mutation, or deployment occurred.

## 1. Boundary and environment

R6.4 implements only:

`R6.3 RegionTranscriptionRequest -> OpenAI Responses wire request -> exact structured response parsing -> R6.3 validation -> region-bound result plus bounded raw state`

Authoritative work ran on Ubuntu host `parker` from clean base `1dfac5750cd37eeaaa51d5be77114fd5cfc13c20` in isolated workspace `/tmp/parker-fa-9.4p-a1e-r6.4-z72tnz6P`. The R6.3 document digest was verified as `b63d0bf819b0025b71e0e82b99cbb9472a0146ae2fcb0430f261046eaae8de6f`; the provider-neutral schema digest was verified as `672a626bd8a6183ff636a4617d017706897fe658e0036f3693c51d5c0d8bfad1`.

## 2. Adapter and candidate profile

- adapter: `openai-responses-region-transcription-adapter`;
- adapter version: `3.0.0`;
- parser: `openai-region-structured-response-parser` version `1.0.0`;
- OpenAI profile: `openai-region-anchored-transcription-v1`;
- lifecycle: `ACCEPTANCE_PENDING`;
- provider/model: OpenAI / `gpt-5.6-sol`;
- endpoint: `https://api.openai.com/v1/responses`;
- reasoning: `none`;
- storage: `store=false`;
- image format/detail: inline `data:image/png;base64,...`, `detail=original`;
- provider-neutral profile: `region-anchored-fidelity-acquisition-v1`;
- processing profile: `external-transcription.deterministic-source-region-raster-v1`;
- schema: `region-anchored-transcription-schema-v1`, wire version 4.

This is an implementation profile, not an accepted production capability. It is not composed into production and has no authority or live-execution entry point.

## 3. Frozen candidate instruction

Exact UTF-8 instruction (no trailing newline):

> Transcribe only text visibly present in each supplied Parker target region. Preserve exact visible Unicode, spelling, punctuation, capitalization, numbers, dates, identifiers, line breaks, paragraph breaks, repeated spaces, tabs, indentation, and significant emphasis. Bind every block only to its supplied source_region_id and page_number. Report bounded uncertainty instead of guessing. Use NO_VISIBLE_TEXT for no visible text and ILLEGIBLE for unreadable content. Never summarize, interpret, reason, rewrite, correct, normalize, infer, complete, regroup, move text between regions, create region IDs, transcribe surrounding page context, or decide source order. Provider-returned ordinal is forensic metadata only. Return only the strict structured schema.

Instruction SHA-256: `fe65ec1c8784a16f0755d62f47700340ee9745b08e48270cf289bb8e05c5c54c`.

The historical direct-source instruction digest `38e4b87e3a429dac8ed5de91e5e2c94ad3d10cd739db186d41d09ed940b11b88` is not reused or modified.

## 4. Request shape and batching

One Responses API request carries the bounded R6.3 batch of 1–32 regions. This avoids internal multi-call reconciliation while retaining exact one-for-one accounting. Image position is never the only correlation mechanism.

The JSON request contains:

- `model: gpt-5.6-sol`;
- `store: false` and `stream: false`;
- `reasoning: { effort: none }`;
- one user message with the frozen instruction and local request correlation;
- for every target, an explicit manifest containing Parker region ID, page, exact bounds and crop digest, immediately followed by the exact governed PNG data URL;
- optional full-page context preceded by `PAGE_CONTEXT_ONLY`, the target ID/page/bounds, and an explicit prohibition on surrounding transcription;
- strict Responses JSON-schema output with `additionalProperties:false` throughout the R6.3 represented objects.

Custody evidence IDs and unrelated source metadata are not put in provider-visible prose. Native PDF text, OCR, earlier transcriptions, summaries, Memory, Knowledge and analyses are absent. The request uses no `/v1/files` upload.

The OpenAI wire schema is the R6.3 schema directly, not a semantically distinct wrapper. Therefore:

- provider-neutral schema SHA-256: `672a626bd8a6183ff636a4617d017706897fe658e0036f3693c51d5c0d8bfad1`;
- OpenAI wire-schema distinct: no;
- OpenAI wire-schema SHA-256: `672a626bd8a6183ff636a4617d017706897fe658e0036f3693c51d5c0d8bfad1`.

## 5. Parsing, validation and order

The parser requires a bounded successful Responses envelope with response ID, reported model and an assistant `output_text` structured segment. The reported model must exactly match `gpt-5.6-sol`. Refusal content is detected before transcription parsing and becomes `PROVIDER_REFUSAL`, never `ILLEGIBLE` or `NO_VISIBLE_TEXT`.

The exact structured JSON map is passed unchanged to `RegionTranscriptionValidator`. The adapter does not repair or reorder provider output. Validator failures for unexpected properties, unknown/missing/duplicate region IDs, wrong pages, malformed spans, invalid status/text, invalid ordinals and excessive content propagate as explicit failure codes.

On success, exact literal Unicode/whitespace, uncertainties, warnings, observations, region/page identity and provider array order survive into `RegionTranscriptionResult`. Provider ordinal remains forensic metadata only; R6.2 geometry remains the sole source-order authority.

Offline A1 fixtures use exact R6.2 IDs:

- page 2 proposition: `5dfb6c252dd668e7ae9dc1be95c8243c4505916e825ca143a0b02e543a9ab668`;
- page 3 authorization: `e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff`.

The request builder retained both IDs, page bindings, image data, bounds and crop digests. A deliberately reversed response retained page 3 then page 2 in provider order without changing either Parker identity. Synthetic tokens were used; no transcription was performed.

## 6. Raw-state boundary for R6.5

`OpenAiRegionRawResponseState` exposes defensive copies of the bounded raw response body plus:

- response SHA-256;
- response ID and provider-reported model;
- exact parsed structured response map;
- exact structured-segment SHA-256;
- adapter/parser identity through validated provider provenance;
- original provider block order.

`transcribeWithRawState` returns the validated typed result and this state together. The provider-neutral `transcribe` method returns the exact structured candidate for the existing R6.3 boundary. Nothing is durably written in R6.4. R6.5 must atomically persist the raw body, structured segment/map, digests, identities and validated result before any controlled live execution.

## 7. Failure and credential safety

Offline tests cover HTTP authentication/rate/redirect/server errors, timeout, general transport failure, malformed/truncated JSON, missing structured output, model mismatch, provider refusal, schema-invalid output, unknown/duplicate/missing regions, wrong page, invalid uncertainty, incompatible status/text, unexpected properties and excessive output. All fail closed without retry or repair.

The adapter reuses the existing redacted transport request and credential boundary. Tests use only an in-memory sentinel credential and fake transport. The bearer secret never appears in request JSON, outcomes, fixtures, logs or assertions. Construction performs no network action.

## 8. Historical compatibility and exclusions

The historical `openai-responses-adapter` 2.0.0, `openai-fidelity-first-transcription-v1`, schema v3, instruction digest `38e4b87e3a429dac8ed5de91e5e2c94ad3d10cd739db186d41d09ed940b11b88`, and schema digest `7b46bdd6ce615592bb4e7cfee84f5ec5f6fde546d678e13bdad0555f829a3313` are unchanged. No historical request or generation is retrofitted.

No Claude adapter/SDK/credential, local OCR, PDF text extraction, durable persistence, reconstruction, comparison, human review, production authority or deployment is implemented.

## 9. Repository surface and next unit

Reused: R6.3 contracts/schema/validator; R6.1/R6.2 image and region bindings; existing OpenAI credential, redacted bounded transport and offline coroutine/JUnit conventions.

Added:

- `src/runtime/OpenAiRegionTranscriptionAdapter.kt`;
- `tests/runtime/OpenAiRegionTranscriptionAdapterTest.kt`.

Exact unresolved issues are:

1. atomic durable raw/structured/validated provider-state persistence and restart recovery;
2. production authority/profile loading and composition, which remain prohibited until persistence exists;
3. controlled external acceptance after R6.5;
4. downstream geometry-derived reconstruction, comparison and human review;
5. independent Claude/local-OCR adapter evaluation under separate governance.

Recommended next unit: `FA.9.4P-A1E-R6.5 — DURABLE REGION PROVIDER-STATE PERSISTENCE AND RESTART RECOVERY`.
