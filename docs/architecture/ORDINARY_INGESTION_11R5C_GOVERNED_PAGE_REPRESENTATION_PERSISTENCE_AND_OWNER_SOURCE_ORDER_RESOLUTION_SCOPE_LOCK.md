# OI11R5C — Governed Page Representation Persistence and Owner Source-Order Resolution Scope Lock

## Status and boundary

Draft governance Scope Lock. This unit performs no rendering mutation, representation persistence, owner-order acceptance, authorization, provider execution, or deployment. Provider calls, retries, and egress are `0 / 0 / 0`.

## Authoritative R5B findings

The immutable registered PDF is evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, 1,887,733 bytes, five pages. PDFBox 3.0.7 at deterministic 300 DPI rendered all pages at 2479×3508. Derived regions were 72/3/4/1/12 by page; page 1 returned `SOURCE_ORDER_REVIEW_REQUIRED`. The original R5A shaping failure remains preserved.

## Representation authority and immutability

The original PDF and Evidence Custodian manifest remain authoritative and are never rewritten. A rendered page is a governed derivative representation bound to evidence ID/SHA, page number, renderer identity/version/build, render profile and parameters, output media type/dimensions, representation format/version, digest, and timestamp. A representation is immutable; any governed input change creates a new representation identity. Region geometry is likewise a derivative bound to page representation ID/digest, source/page, region ID, coordinate system, deterministic bounds, derivation profile/version, and order-graph state.

## Ordering and ambiguity

Parker may persist and use automatically derived order only when the existing graph is deterministic and valid. `SOURCE_ORDER_REVIEW_REQUIRED` remains fail-closed: no arbitrary traversal, filesystem order, lexical ID order, provider order, or silent top-to-bottom heuristic is permitted. The owner may resolve ambiguity only by approving a complete explicit ordering over the already-derived immutable region set; geometry and rendering are not changed.

## Owner-resolution record

The required distinct record contains resolution ID, evidence ID/SHA, page, page-representation ID/digest, region-set identity/digest, every region ID exactly once in order, original ambiguity disposition, owner, timestamp, status `OWNER_RESOLVED_SOURCE_ORDER`, format/version, and record digest. Unknown, duplicate, missing, partial, stale, or cross-region-set orders fail closed. Review must show the rendered page with boundaries, stable IDs, positions, and current graph relationships; a bare list of 72 IDs is insufficient.

## Persistence and provenance boundary

The minimum new production state is immutable page-representation content/record, immutable region-geometry record, and either deterministic-order state or the owner-resolution record. These are linked derivatives, not source-evidence records. The chain must remain traceable: source PDF → page → rendering → geometry → derived graph → owner resolution (if needed) → V8 request region. Identical governed inputs may reproduce the same identity; changed inputs require a new generation and renewed owner review.

## V8/provider boundary

The existing V8 capability and digest remain unchanged: `ordinary-external-request-region-transcription-v8` / `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`. Preparation is local, raster-only, and does not require OCR or any provider. Existing 32-region and 16,777,216-byte request bounds remain in force. OpenAI/profile/model/purpose gates remain downstream and separate.

## Future implementation scope (OI11R5D)

The next unit may implement only durable page-rendered representation persistence, durable geometry/order state, ambiguity status, complete owner-order records and validation, integration before existing V8 shaping, and tests. It must not execute the real document. Because implementation identity is governed, the sequence must include tests, replacement artifact build/preservation, explicit artifact acceptance, deployment, and a new implementation-bound V8 capability acceptance before use.

Required tests include deterministic rendering/region identities, immutable persistence, automatic unambiguous order, ambiguity fail-closed behavior, exact complete owner order, duplicate/missing/unknown/stale rejection, source immutability, V8 shaping only after valid resolution, historical readability, and unknown-version fail-closed behavior.

## Explicit exclusions

No production representation or order record is created in OI11R5C. No source mutation, provider call, external egress, OCR-before-OCR pipeline, semantic document understanding, legal analysis, page omission, capability-digest change, or real-document execution is authorized. The registered real evidence remains unchanged.

## Baseline integrity

Host `parker`; repository `/home/steve/parker-platform`; starting HEAD/upstream `2a6146c5d01f492d8ec4dccf42bf2b8e894a1b8a`; production remains running on image `sha256:5ca82f03ce61eade58c60eb4d3783547b4b266f974ed2ac218c09cf43f86075a` with restart count 0. OI11R5D is required before any owner review or OI11R6 authorization.
