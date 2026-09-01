# OI11R5H — Governed Real-PDF Source-Faithful Preparation and One-Call Request-Fit Scope Lock

## Verdict and scope

**A — READY FOR OWNER ACCEPTANCE.** R5I shall implement one bounded strategy: `full-page-achromatic-png-preparation-v1`, version 1. Each authoritative 300-DPI page becomes exactly one full-page preparation region and one V8 request region, in PDF page order. The transport image is a deterministic full-resolution 8-bit grayscale PNG derived from, but never substituted for, the authoritative RGB page. This yields five regions for the registered five-page Deed, geometrically proves complete coverage, removes all internal tile boundaries and ordering ambiguity, and has measured one-call feasibility.

This is a governance and architectural scope lock only. It adds no production implementation, deployment, restart, production-store record, owner source-order resolution, provider authorization, provider call, retry, derivative or transcription. R5I must remain fail-closed until its quality and exact-body gates pass; this document is not authority to execute R5I or to transmit the Deed.

## Authoritative R5G findings and immutable diagnostic state

OI11R5G is authoritative and is not reopened. `pixel-whitespace-source-regions-v1` uses raster dark-pixel detection, whitespace grouping, horizontal splitting, permissive geometric merging, padding, minimal size filtering, and overlap/containment/order graph construction. It uses no OCR, semantic or PDF-object layout, meaningful-content classification, noise suppression, overlap pruning, or semantic consolidation.

The selected Deed produced `72 / 3 / 4 / 1 / 12` regions, 92 total. Page 1 has 127 intersecting pairs, 114 containment pairs, and a largest region covering 89.44% of the page; it remains `SOURCE_ORDER_REVIEW_REQUIRED`. The frozen root classification is **F — Combined defect**, comprising **C — Representation-model mismatch** and **E — Cardinality/shaping defect**, with secondary **D — Pipeline composition defect**.

The following state remains immutable historical diagnostic evidence and shall not be modified, deleted, reordered, owner-resolved, reused, or reinterpreted as corrected geometry:

| Item | Frozen value |
|---|---|
| Evidence | `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9` |
| Source SHA-256 | `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e` |
| Page-1 representation | `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e` |
| Page-1 region-set digest | `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f` |

Corrected preparation creates new identities and source-order state alongside this state.

## Preparation principles and representation layers

For owner-selected evidence, Parker's real-document preparation stage produces a faithful, bounded visual representation for transcription. It is not an evidential relevance filter and must not silently decide which source content deserves transcription. It preserves immutable source linkage, complete intended page coverage, page identity, deterministic geometry, source-to-request traceability, ordering provenance, and genuine uncertainty. Background may be suppressed or omitted only by a deterministic rule proven not to remove meaningful visible source information.

Three layers are distinct:

1. **Authoritative page representation** — the immutable deterministic 300-DPI RGB rendering of a PDF page, with canonical pixel and encoded identities. It remains the visual authority.
2. **Preparation region** — a deterministic bounded subdivision used to prepare faithful input. Under the selected profile it is the full half-open page rectangle `[0,0,width,height)`.
3. **V8 request region** — the provider transport unit. Under this bounded profile each preparation region maps one-to-one to a request region, but that is a profile choice rather than a general identity rule. The model permits future many-to-one or one-to-many mappings only through separately governed work.

Every transition retains explicit references; transport bytes never acquire evidential authority over the source or authoritative page.

## Candidate assessment

| Candidate | Fidelity and coverage | Geometry/order | One-call fit | Disposition |
|---|---|---|---|---|
| A — cleaned meaningful blocks | Could save bytes, but proving that noise removal never removes faint print, handwriting or signatures requires content discrimination that is not available. | Non-overlap is possible; block reading order can remain ambiguous. | Plausible, not demonstrated. | Rejected: proof burden and silent-omission risk are too high. |
| B — deterministic page tiling | Geometrically complete and independent of semantic classification. One full-page tile eliminates boundary clipping and duplicate overlap. | Five disjoint per-page regions; intrinsic PDF page order. | Demonstrated with full-resolution grayscale PNG transport and guarded by exact-body gates. | **Selected.** |
| C — layout/tiling hybrid | Fallback can preserve coverage, but layout confidence adds a second decision surface and identity path without benefit for this five-page case. | More complicated ordering and fallback proofs. | Plausible. | Rejected as unnecessary scope and variability. |
| D — corrected current mechanism | R5G proves raw scan artefacts, containment and permissive merging make it an unreliable selector. Filtering/consolidation would still need to decide what matters. | Risks recreating pathological graphs and false review. | Unproven. | Rejected; the R5F set is diagnostic only. |
| Five full-page RGB PNGs | Pixel-exact and complete. | Ideal. | R5G measured 19,561,064 raw PNG bytes and 26,081,420 base64 bytes before JSON. | Rejected solely for frozen body overflow. |
| JPEG / resolution reduction | Likely smaller. | Geometry/order can be deterministic. | Likely. | Rejected for v1: lossy artefacts or reduced small-text resolution need a larger quality study; JPEG would also require an explicit codec/media-type decision. |

No OCR-generated text, PDF text-object extraction, semantic label, or transcription result participates in preparation. A future open-source visual layout mechanism may be evaluated only in a separate scope lock, using geometry/confidence rather than generated text as evidential truth and retaining complete tiling fallback.

## Selected deterministic tiling and complete-coverage semantics

For each authoritative page, R5I derives one preparation rectangle exactly equal to its canonical pixel extent. Empty, nearly blank, noisy, handwritten, signed, margin, header and footer areas are included without classification. Page count comes from the already governed authoritative renderer. A page may not be dropped because it appears blank.

Complete source coverage means, for every page pixel coordinate `(x,y)` in `[0,width) × [0,height)`, exactly one preparation region contains it; the union equals the page rectangle and the intersection of distinct preparation regions is empty. Across the document, represented page numbers must equal exactly `1..authoritativePageCount`. The implementation shall validate these set/area invariants from integer half-open bounds before persistence and again before V8 shaping.

This selected instance uses no overlap and has no internal boundary, so it cannot clip or duplicate text at a tile edge. The general tiling ideas of fixed-height strips or overlap are not selected: non-overlap can bisect text, while overlap can cause duplicate transcription and reconstruction ambiguity. If a future document cannot fit using one tile per page under this profile, R5I fails closed; it does not silently increase tiling, lower resolution, change encoding or batch.

The preparation-region order and Parker request order are ascending PDF page number. There is one region per page, so no within-page ordering decision exists. Request-region IDs do not control order. Provider ordinals remain forensic-only.

## Transport encoding and fidelity floor

The immutable authority remains the PDF plus the 300-DPI, 2479×3508, 8-bit RGB page representation. R5I performs no resize, crop, threshold, denoise, sharpening, contrast adjustment, binarization, palette quantization, OCR or lossy compression. It maps each unsigned sRGB pixel to one 8-bit sample with the frozen integer rule `Y = (77R + 150G + 29B + 128) >> 8` (coefficients sum to 256; round-half-up before division; result range `0..255`). R5I then uses the JDK 17 PNG writer with an explicitly selected provider class, `TYPE_BYTE_GRAY`, explicit maximum lossless compression, no ancillary metadata, no interlace, and byte-repeatability tests. The provider class, JDK artifact identity and all write parameters are profile inputs. PNG is lossless with respect to the derived grayscale samples.

Grayscale is a transport transformation, not a claim that color lacks evidential meaning. Its provenance records the source pixel digest, formula/version, output dimensions/color model, encoder identity/parameters, encoded length and SHA-256. R5I must fail closed for this profile when a deterministic chromatic-risk gate or the required visual acceptance fixtures show that distinct visible material collapses or becomes less readable. There is no automatic JPEG, lower-DPI, thresholded, denoised or multi-call fallback.

The quality floor is: unchanged 300-DPI dimensions; no alpha or spatial resampling; all grayscale values `0..255`; deterministic repeat output; and human-verifiable readability/no-loss acceptance for clean, faint, dense, handwritten, signed, mixed and chromatic-risk fixtures at original pixel scale. A chromatic-risk fixture must include equal-or-near-equal-luminance distinguishable colors and must cause the profile to reject rather than erase the distinction. The registered Deed must receive bounded local side-by-side review against authoritative RGB pages before artifact acceptance. Request-size compliance cannot override any quality failure.

The current codec emits `data:image/png;base64,` with `detail=original`; grayscale PNG therefore uses the already supported V8 image representation. Lower-resolution PNG, JPEG and other media types are outside R5I.

## Measured one-call feasibility and safety margin

R5G's five full-page Java ImageIO RGB PNGs were 19,561,064 bytes raw and 26,081,420 bytes base64, excluding JSON, and therefore fail. During this zero-egress unit, a local Java/ImageIO feasibility measurement decoded the five preserved authoritative pages and encoded full-resolution `TYPE_BYTE_GRAY` PNGs with the current JDK conversion/default lossless encoder. It did not persist governed state or transmit bytes:

| Page | Grayscale PNG bytes |
|---:|---:|
| 1 | 1,518,617 |
| 2 | 2,463,807 |
| 3 | 2,263,345 |
| 4 | 1,591,418 |
| 5 | 2,176,951 |
| **Total** | **10,014,138** |

The exact aggregate base64 length for that feasibility encoding is 13,352,184 bytes. This is a conservative feasibility observation, not an identity fixture or R5I acceptance result: R5I's frozen integer conversion and explicit encoder must independently pass the image thresholds, construct the real canonical V8 request, and measure exact UTF-8 bytes.

R5I freezes both limits:

* aggregate encoded image binary: at most **10,875,000 bytes**;
* aggregate base64 characters: at most **14,500,000 bytes**;
* canonical final serialized UTF-8 request body: at most **16,000,000 bytes**;
* absolute V8 contract limit: **16,777,216 bytes**.

Thus at least 1,500,000 bytes are reserved beyond base64 for manifests, instructions, schema, JSON syntax, data-URI prefixes and deterministic serialization, and the internal final-body ceiling retains a further 777,216-byte margin below the contract maximum. Both prepared-image gates and both final-body gates must pass; the exact final serialized body is authoritative. No estimate or pre-base64 size alone can authorize transport.

## Ordering and owner-review semantics

The order is `pageNumber ASC`, with the sole region on each page. The preparation manifest, request manifest and reconstruction must contain that same order. `SOURCE_ORDER_REVIEW_REQUIRED` is valid only when source-faithful preparation regions are themselves valid and more than one plausible reading order remains. It must never repair invalid or pathological machine geometry.

For this profile, the geometry creates no reading-order ambiguity, so the new state is `DETERMINISTIC_SOURCE_ORDER` if all invariants pass. No owner resolution is created for R5F. If a future selected page subdivision produces genuine ambiguity, preparation stops with `SOURCE_ORDER_REVIEW_REQUIRED`; it cannot invent an order or delegate ordering to the provider.

## Deterministic identity and provenance

R5I shall use profile `full-page-achromatic-png-preparation-v1`, version `1`. Domain-separated, length-delimited canonical SHA-256 identities shall be used; concatenation without framing, filesystem order, timestamps, environment paths and encoder discovery order are forbidden inputs.

The preparation identity binds: evidence ID and source SHA-256; page number/count; authoritative page representation ID and canonical pixel digest; authoritative dimensions; profile ID/version; every frozen preparation and encoding parameter; exact full-page bounds; ordering rule/version; transport transformation ID/version; transport color model/dimensions; transport byte length and SHA-256. The region-set digest binds the ordered list of preparation-region identities and bounds plus the preparation identity. Each V8 request-region identity additionally binds its ordered constituent preparation-region identity, exact source bounds, source crop digest, and transport digest according to the existing request identity rules as extended only to carry this provenance.

Every record remains traceable to source evidence, source digest, page, authoritative page identity, exact source pixel bounds, profile/version/parameters, ordering inputs, and transport transform/encoding. The new representation/preparation identity, region-set digest and source-order record are create-once alongside R5F. Collision with, overwrite of, or mutation of an existing record fails closed.

## V8 compatibility and governance impact

The selected strategy can feed the existing schema without changing accepted semantics:

* capability remains `ordinary-external-request-region-transcription-v8`;
* digest remains `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`;
* a request region remains a bounded visual transport representation with Parker-issued identity, page, source bounds and ordered constituent provenance;
* the five complete full-page preparation regions are valid constituents and produce five request regions; no one-to-one rule is added to V8 itself;
* Parker's page/request order remains authoritative, and provider-returned ordinals remain forensic-only;
* response validation by issued request identity, ordered reconstruction, literal/uncertainty semantics, and raw-response-before-parse durability remain unchanged;
* the request remains one call, no batching and no retry.

The grayscale PNG is a deterministic transport rendering with explicit authoritative-source provenance; it does not change the provider's duty to transcribe visible content or the response contract. R5I changes implementation/preparation identity and therefore requires a new build, artifact acceptance, deployment and implementation-bound acceptance for the same capability/digest. If implementation discovers that V8 cannot carry truthful transformation provenance, that a codec/media-type change is required, or that response reconstruction needs boundary deduplication or changed semantics, R5I must stop and return **B — GOVERNANCE CHANGE REQUIRED**. It may not revise the digest implicitly.

## Minimum fidelity and acceptance fixtures

Automated fixtures should be synthetic and non-sensitive. The minimum set covers: clean print; faint print; dense paragraphs; headings; all four page margins; handwriting; signatures; scan noise/background; text placed at every potential block/tile boundary (and proof that the selected full-page region has no internal boundary); mixed print/handwriting; blank and nearly blank pages; multi-page order; and the chromatic-risk rejection case described above.

Each fixture asserts source/page linkage, exact full-page bounds, pixel coverage multiplicity exactly one, unchanged dimensions, deterministic grayscale samples and PNG bytes/digests across repeated runs, readable comparison at original scale, deterministic order, unique identities, and exact body accounting. Tests must demonstrate that blank/noisy areas remain covered, faint strokes and signature strokes remain visible, and chromatic collapse fails closed.

The registered Deed is a bounded local real-world acceptance fixture only and must never be externally transmitted during these gates.

## Registered Deed acceptance matrix

R5I must record the following local evidence before it can be proposed for artifact acceptance:

| Criterion | Required measurement |
|---|---|
| All five pages | Manifest page set equals exactly `1..5`; five authoritative-page links, five preparation regions and five request regions. |
| Determinism | Two clean constructions have byte-identical manifests, geometries, order, identities, transport PNGs and canonical body digest. |
| No pathological graph | Each page has one valid full-page rectangle; document intersections/containments between distinct same-page regions are `0 / 0`; no R5F region is a constituent. |
| Complete coverage | Per-page union equals `[0,0,2479,3508)`, summed area equals 8,696,332 pixels, and every pixel has multiplicity one. |
| Order | Request pages and reconstruction order equal `[1,2,3,4,5]`; state is `DETERMINISTIC_SOURCE_ORDER`. |
| Region bound | Exact request-region count is 5, within `1..32`. |
| Body bound | Binary/base64 gates pass; exact canonical UTF-8 body is `≤16,000,000` and therefore `≤16,777,216`. |
| No omission | Page/area equality, blank-area inclusion and visual overlay show no crop, mask, threshold, denoise or excluded region. |
| Fidelity | Side-by-side full-scale review of every RGB authority and grayscale transport plus fixture suite passes for faint print, handwriting, signatures and mixed material; chromatic-risk gate passes or fails closed. |
| Provider-free | Builder/codec validation stops before transport; calls, retries and egress remain zero. |

Failure of any row blocks the profile. No automatic parameter search or degradation is permitted.

## Exact R5I implementation boundary

R5I may add narrowly named preparation interfaces/types for profile/parameters, full-page preparation region, transport transformation provenance, preparation manifest/identity and validation outcomes. It may add one deterministic full-page preparer and one fixed grayscale PNG encoder, integrate create-once persistence beside the existing page representation records, and adapt V8 shaping so the five new preparation regions become normal request-region constituents with exact provenance. It shall compute canonical identities/digests, deterministic page order, all fidelity/coverage/size gates, and expose `SOURCE_ORDER_REVIEW_REQUIRED` only under the semantics above.

Tests are limited to profile/parameter validation; pixel conversion and encoding determinism; coverage/order/identity/provenance; create-once persistence/read-back/conflict behavior; V8 five-region shaping, codec, exact body and response reconstruction compatibility; immutable R5F coexistence; fixture fidelity; and the registered Deed's local acceptance harness. Test-only inspection artefacts must stay outside governed production stores.

R5I must not become a general layout programme. It must not change the PDF renderer or authoritative 300-DPI RGB page bytes/identities; `pixel-whitespace-source-regions-v1` or R5F records; source evidence/manifest; V8 capability ID/digest, instruction, schema, parser, validator, response reconstruction, raw-before-parse sequence, provider/model/profile, 32-region/16,777,216-byte limits, batching/retry behavior; owner authorization; execution/provider-state/derivative stores; provider adapters beyond accepting the already supported PNG bytes from the new preparation path; production configuration; or deployment state. It must not add OCR, PDF text extraction, semantic layout, scan-noise content removal, JPEG, resizing, hidden batching or provider work.

## Later production-convergence sequence

The mandatory later sequence is:

1. R5I implementation and tests;
2. corrected artifact build;
3. preservation;
4. explicit owner artifact acceptance;
5. deployment;
6. readiness verification;
7. new implementation-bound V8 capability acceptance;
8. evaluator `ACCEPTED`;
9. regenerate the Deed with the new preparation profile as new governed state;
10. inspect corrected preparation locally;
11. only then reconsider first real provider execution.

No gate may be combined, inferred or bypassed.

## Production integrity and zero-egress accounting

Read-only inspection during R5H confirmed production remains container `34e0637eaa2b32c3e4c43b3e29c274b3da2d7a59c641cc5a2e25143465ba36b4`, implementation `9e2a900fee388ebf4787817c24f34a63190b3f0d`, image `sha256:c121ea0d5c55c32a8cec9b38eade8ecb5f2d72f5331a8ed761b10fb8cfef0ae4`, running with restart count `0`. The bounded in-container diagnostic returned `ordinaryExecutionReady=true`, `overallReady=true`, and no reasons: readiness **PASS**. Its exact implementation-bound V8 acceptance remains present, so evaluator state remains **ACCEPTED**. R5H caused no deployment, restart or production-store mutation.

Final accounting: OpenAI `0`; Claude `0`; other external `0`; retries `0`; external egress `0`.

UNIT ORDINARY-INGESTION-11R5H COMPLETE — A BOUNDED, DETERMINISTIC AND SOURCE-FAITHFUL REAL-PDF PREPARATION STRATEGY HAS BEEN DEFINED FOR OWNER REVIEW. THE STRATEGY PRESERVES IMMUTABLE SOURCE AUTHORITY, COMPLETE PAGE COVERAGE, TRACEABLE GEOMETRY, DETERMINISTIC ORDERING AND THE FROZEN ONE-CALL V8 REQUEST LIMITS WITHOUT REUSING OR OWNER-ORDERING THE UNSUITABLE OI11R5F REGION SET. NO PRODUCTION IMPLEMENTATION, PRODUCTION MUTATION, PROVIDER AUTHORIZATION, PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. R5I IMPLEMENTATION MUST NOT BEGIN UNTIL THIS SCOPE LOCK IS EXPLICITLY ACCEPTED BY THE OWNER.
