# FA.9.4P-A1 Provider-Neutral Region-Bound External Transcription Contract

**Unit:** FA.9.4P-A1E-R6.3

**Status:** Implemented and verified offline on Ubuntu. No provider call, OCR, native text extraction, acceptance execution, persistence, comparison, production mutation, runtime rebuild, or deployment occurred.

## 1. Boundary and purpose

R6.3 adds only:

`R6.2 source regions -> provider-neutral request -> strict region-bound response -> Parker validation`

External high-fidelity visual transcription remains the primary acquisition target. The contract is independent of OpenAI, Claude, and local OCR wire formats. A future adapter translates this Parker contract to a provider API and exposes the exact parsed provider-returned structure without making provider IDs, order, confidence, or semantics authoritative.

Authoritative work ran on Ubuntu host `parker` from clean base commit `6a8e5d4ad4e48ad09b4dc89e8e6ac217f43ac0c5` in isolated workspace `/tmp/parker-fa-9.4p-a1e-r6.3-SlRhz7Ba`. R6.2's governing document digest was verified as `f49073d08d6d272c8f06aaf22bf410fdac12a60fbebe43f71d35400d7ede21b3`.

## 2. Governed identities

- Parker transcription profile: `region-anchored-fidelity-acquisition-v1`;
- schema identity: `region-anchored-transcription-schema-v1`;
- wire version: `4`;
- processing profile: `external-transcription.deterministic-source-region-raster-v1`.

The historical `openai-fidelity-first-transcription-v1`, schema v3, instruction digest `38e4b87e3a429dac8ed5de91e5e2c94ad3d10cd739db186d41d09ed940b11b88`, schema digest `7b46bdd6ce615592bb4e7cfee84f5ec5f6fde546d678e13bdad0555f829a3313`, and adapter 2.0.0 remain unchanged. `openai-region-anchored-transcription-v1` remains proposed until a separate adapter unit governs its instruction, schema translation, transport, and identity.

## 3. Request contract

`RegionTranscriptionRequest` contains a bounded correlation ID, governed profile/schema/version/digest, processing profile, exact literal instruction, and 1–32 immutable targets. Every target binds:

- custody artifact ID and SHA-256;
- R6.1 page representation ID, page number and dimensions;
- Parker `SourceRegionId`, exact half-open bounds and crop digest;
- R6.2 structural class and derivation profile/version;
- exact embedded PNG region bytes and their encoded SHA-256;
- optional exact full-page PNG context.

The image contract verifies defensive byte copying and encoded SHA-256. Region image page ID, bounds and crop digest must exactly equal the target binding. Optional page context must cover `[0,0,pageWidth,pageHeight)` in the same page representation. Its instruction explicitly restricts output to the target region. No OCR, extracted text, Memory, Knowledge, analyses, unrelated evidence, or surrounding-page transcription accompanies a target.

Batching is supported up to 32 regions because bounded batching reduces future transport overhead and allows exact accounting while proving that provider array order is irrelevant. Duplicate request-region IDs are rejected at construction.

## 4. Literal transcription policy

The governed instruction requires only literal visually present text. It prohibits summarisation, interpretation, legal reasoning, rewriting, correction, normalization, inference, completion, semantic regrouping, cross-region movement, captions, and unrelated context text.

Literal strings preserve source-visible Unicode, punctuation, capitalization, numbers, dates, identifiers, line breaks, paragraph breaks, repeated spaces, tabs and indentation. No Unicode normalization is applied: exact provider-returned scalar values are retained, while malformed/unpaired UTF-16 surrogates are rejected. Later comparison may create a separately identified normalized representation, but it cannot replace the forensic literal string.

Uncertainty spans use zero-based Unicode **code-point** indices with a half-open end. This avoids UTF-16/provider ambiguity while remaining deterministic without adding a grapheme library. Each span must fall within the literal text and its `exactSubstring` must equal the indexed text. Combining sequences therefore remain multiple code points; a future grapheme-oriented display may derive presentation mapping without changing canonical indices.

## 5. Response contract

Every `RegionTranscriptionBlock` contains:

- Parker source-region ID and page number;
- literal text or null;
- bounded status;
- uncertainty and warning lists;
- provider-returned ordinal retained only as forensic metadata;
- optional bounded visual observations.

Statuses are `TRANSCRIBED`, `PARTIALLY_TRANSCRIBED`, `ILLEGIBLE`, `NO_VISIBLE_TEXT`, and `UNSUPPORTED_VISUAL_CONTENT`. `TRANSCRIBED` requires text and no uncertainty; partial transcription requires text and uncertainty. Illegible/no-text/unsupported states cannot contain invented text. `NO_VISIBLE_TEXT` is transcription silence, not permission to describe an image.

Uncertainty categories are `ILLEGIBLE`, `AMBIGUOUS_CHARACTER`, `AMBIGUOUS_WORD`, `PARTIALLY_OCCLUDED`, `LOW_CONTRAST`, `HANDWRITING_UNCERTAIN`, `CLIPPED`, and `OTHER_VISUAL_UNCERTAINTY`. Alternatives are bounded and optional. Provider confidence is retained only as a bounded string assertion, never Parker truth. Free-form reasoning is absent.

Optional observations are limited to line/paragraph breaks, list markers, table-cell text, bold, italic, underline, all-caps and visibly enlarged text. They are provider assertions, not source-order or semantic authority. `TABLE_LIKE` regions may return visible bounded cell text or an unsupported/uncertain status; the contract does not flatten tables into invented prose. The uncertainty model already supports future handwriting without creating or consuming an O.5 profile.

## 6. Exact accounting and provider order

Validation requires each requested Parker region exactly once. Unknown, missing, duplicate or provider-created region IDs and page mismatches are rejected. Provider ordinals must be a unique permutation of `1..N`, are preserved in `blocksInProviderOrder`, and are never used to reconstruct source order. R6.2 geometry remains authoritative.

Synthetic A1 fixtures bind page 2's proposition to `5dfb6c252dd668e7ae9dc1be95c8243c4505916e825ca143a0b02e543a9ab668` and page 3's authorization block to `e2c2d8fe0e894fd2b42f53f75c4c7d1304755db1486a29877c01265a6b3e84ff`. Deliberately reversing provider array order preserved both identities. Tokens, not OCR-derived text, were used.

## 7. Schema and validation

The embedded JSON Schema has no arbitrary additional properties. It explicitly requires all top-level, provenance, block, uncertainty and observation fields; bounds block/text/list/string sizes; constrains enums, region-ID patterns, pages, ordinals, schema identity and wire version; and permits null only where governed. The schema SHA-256 is calculated deterministically over the committed UTF-8 schema source.

`RegionTranscriptionValidator` consumes exact parsed structured state as maps, rejects unexpected keys at every represented object boundary, and returns a typed result only after correlation/profile/schema checks, provider-provenance bounds, exact region accounting, page matching, enum/status rules, Unicode scalar validation, code-point span/sub-string checks, ordinal permutation checks, and resource bounds.

Focused synthetic fixtures cover perfect literal text, uncertain supplementary Unicode, illegible/no-text states, multiple and reversed blocks, missing/duplicate/unknown regions, wrong pages, malformed Unicode, invalid spans/status/ordinals, additional fields, invented no-text content, table observations, handwriting-compatible uncertainty, excessive text/blocks, deterministic schema digest, and both A1 bindings.

## 8. Provider provenance and raw-state boundary

The result retains provider, requested model, provider-reported model, response ID, adapter identity/version, and parser identity/version. The adapter-facing outcome exposes the exact parsed structured response map so R6.5 can durably persist it, including returned array order, without reconstructing discarded state. R6.3 performs no durable write.

Future execution authority can bind custody source, page/render profile, region IDs, provider/model/recipient, Parker profile/schema, processing profile, adapter, allowed page context, and attempt count. No authority execution is implemented here.

## 9. Existing and future adapters

The existing OpenAI adapter 2.0.0 is full-source/schema-v3 behavior and is untouched. Region support requires a separately governed adapter 3.x (recommended identity `openai-responses-region-transcription-adapter`, version `3.0.0`) to encode crop/context images, translate wire v4, use the new literal instruction, preserve exact returned structured state, and call this validator before downstream use.

Claude and any accepted local OCR implementation can implement `RegionExternalTranscriptionMechanism` and return the same contract without SDK changes to core. Neither is integrated, selected, called, or authorized by R6.3.

## 10. Repository surface and next prerequisites

Reused: R6.1 page/image identities and dimensions; R6.2 region IDs, geometry, classes and provenance; existing SHA-256, interface/runtime layout, and offline Kotlin/JUnit conventions.

Added:

- `src/interfaces/RegionExternalTranscription.kt`;
- `src/runtime/RegionTranscriptionContract.kt`;
- `tests/runtime/RegionTranscriptionContractTest.kt`.

Unchanged: R6.1/R6.2 geometry; historical OpenAI profiles/schema/adapter; transports; acquisition/admission; production composition; authorities, attempts, generations, derivatives, reviews, analyses, Memory and Knowledge stores.

Exact unresolved issues are:

1. provider-specific adapter 3.x request encoding and exact structured-response parsing;
2. durable raw and parsed provider-state persistence, owned by R6.5;
3. geometry-derived multi-region reconstruction and validation against R6.2 graphs;
4. separately governed provider/model acceptance and disclosure policy;
5. optional grapheme-display mapping without changing canonical code-point spans.

Recommended next unit: `FA.9.4P-A1E-R6.4 — REGION-BOUND EXTERNAL TRANSCRIPTION ADAPTER AND OFFLINE WIRE VERIFICATION`.
