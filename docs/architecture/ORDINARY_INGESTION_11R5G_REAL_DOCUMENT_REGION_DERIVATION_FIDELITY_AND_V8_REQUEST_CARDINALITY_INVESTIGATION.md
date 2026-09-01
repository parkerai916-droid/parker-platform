# OI11R5G — Real-Document Region-Derivation Fidelity and V8 Request-Cardinality Investigation

## Scope and starting state

This zero-egress investigation started at `45ad0bd550d23fcd541bd4a9f8c13f650550bed6`, with a clean worktree and equal upstream. Production remained running on implementation `9e2a900fee388ebf4787817c24f34a63190b3f0d`, image `sha256:c121ea0d5c55c32a8cec9b38eade8ecb5f2d72f5331a8ed761b10fb8cfef0ae4`, container `34e0637eaa2b32c3e4c43b3e29c274b3da2d7a59c641cc5a2e25143465ba36b4`, restart count 0. The container was running and readiness remained PASS. Capability evaluation and provider profile were unchanged. No deployment, restart, store mutation, provider authorization, provider call, retry or external egress occurred.

The OI11R5F evidence is immutable: evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, manifest SHA-256 `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34`, five pages rendered by PDFBox 3.0.7 at 300 DPI to 2479×3508 pixels. Page 1 representation is `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e`; its region-set digest is `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f`. The staged review image and manifest remain preserved with SHA-256 values recorded by OI11R5F.

## Owner finding and frozen state

The owner-review finding is that the page-1 geometry is materially over-segmented, overlapping and poorly aligned with meaningful document regions. It is not an evidential rejection. Page 1 remains `SOURCE_ORDER_REVIEW_REQUIRED`; no owner order was created and the frozen region-set digest was not changed.

## Exact derivation pipeline

`src/runtime/DeterministicSourcePageRenderer.kt` renders custody bytes locally with PDFBox and emits an immutable `AuthoritativePageRepresentation`; it performs no OCR or layout extraction. `src/runtime/DeterministicSourceRegionDeriver.kt` then:

1. verifies canonical RGB pixels and page digest;
2. marks a row occupied when at least `minimumDarkPixelsPerRow=2` pixels have a channel below `darkChannelThreshold=245`;
3. groups rows when inter-band blank space is at most `maximumIntraBandBlankRows=2`;
4. splits each band at horizontal gaps greater than `horizontalSplitGapPixels=80`;
5. merges nearby boxes when the vertical gap is at most `maximumInterlineGapPixels=42` and they overlap or are within `alignmentTolerancePixels=80` horizontally;
6. pads by 8 pixels and drops only boxes below 4×4 pixels;
7. classifies by pixel density and constructs overlap/containment/order edges.

The governed profile is `pixel-whitespace-source-regions-v1`, version 1. It uses raster geometry only: no OCR, PDF text objects, contours, semantic layout model, noise filter, overlap pruning, or region consolidation exists in this path.

## Geometry forensics

The persisted geometry files were read without mutation. Diagnostics for page 1 (2479×3508; 8,696,332 total pixels) are:

| Measure | Result |
|---|---:|
| Regions | 72 |
| Intersecting region pairs | 127 |
| Containment pairs | 114 |
| Small regions under 1,000 px | 14 |
| Largest region | 7,778,175 px (89.44% of page area) |
| Sum of region areas | 12,035,097 px |
| Smallest region | 324 px |

The large near-page region and high overlap/containment counts explain the visual review: dark scan/background pixels create candidate bands, while the permissive merge and padding rules create supersets and nested boxes. These are geometry candidates, not reliable semantic text blocks. The 72 IDs therefore cannot truthfully be treated as 72 independently meaningful transcription units without an additional governed selection/consolidation rule.

Pages 2–5 produce 3, 4, 1 and 12 regions respectively. Their files show the same algorithm, including page-sized or near-page-sized boxes and containment pairs where present. The differing counts are explained by page-specific raster darkness, spacing and scan artefacts, not by a different semantic extractor. Page 1 is the most visibly pathological because its raster produces many split bands and overlaps.

## Intended region meaning

The source-region types (`SourceRegion`, `PixelCropBounds`, structural classes and provenance) describe bounded raster geometry and provenance. They are not OCR lines, paragraphs, legal units or evidential selections. A V8 request region is a transport representation containing one or more ordered source regions. The distinction is explicit in `OrdinaryRequestRegionV6.kt`: request bounds are the union of contiguous source-region bounds and constituent source-region identity is retained for provenance.

## Source-region to V8 shaping

`DeterministicCompleteSetRequestRegionShaper.shape` first requires every page graph to be ordered. An ambiguous graph returns `SOURCE_ORDER_REVIEW_REQUIRED` before any request is built. Once ordered, it coalesces contiguous source regions into at most `REQUEST_REGION_MAXIMUM = 32` request regions. It does not filter noise, remove overlap, use page-level regions automatically, or make multiple provider calls. `CompleteRequestRegionSetValidator` requires complete, source-adjacent, Parker-ordered coverage with no duplicate or missing constituent.

For the frozen counts 72/3/4/1/12 (92 source regions), a fully ordered run would not send 92 regions and would not omit regions. The deterministic quota allocator would create exactly 32 contiguous request groups with quotas page 1/page 2/page 3/page 4/page 5 = **24/1/2/1/4**. Today this path cannot reach that stage because page 1 remains ambiguous. There is no batching or multi-request fallback; the one-call V8 boundary is therefore compatible only if one shaped request is within both limits.

## Full-page compatibility and request size

The V8 contract permits a request region to contain one or more source regions and uses the union of their bounds. Thus a full-page region is structurally representable as an upstream preparation choice, but it is not currently produced automatically and would be a preparation-semantics decision, not a V8 schema change. It would preserve page provenance but could transmit background and unrelated page area; that trade-off requires governed preparation design and fidelity review.

Using the actual persisted 2479×3508 PNG representations and the current Java ImageIO PNG encoding, the union crops corresponding to the five pages encoded to:

| Page | Union crop PNG bytes |
|---:|---:|
| 1 | 3,130,233 |
| 2 | 4,580,121 |
| 3 | 4,353,977 |
| 4 | 3,208,414 |
| 5 | 4,288,319 |
| **Total** | **19,561,064** |

Base64 alone is 26,081,420 bytes (plus five `data:image/png;base64,` prefixes, manifests, schema and JSON syntax). Therefore a hypothetical five-page full-page request is **strictly above** the 16,777,216-byte body limit before JSON overhead; it cannot satisfy the frozen one-call/body boundary under current encoding. This is an exact local encoding calculation, not an estimate, and no request was transmitted.

## Strategy comparison

* **Current fine-grained geometry:** preserves local crops but is unsuitable here because raster noise/overlap creates 92 candidates, page-1 ambiguity and poor semantic fidelity.
* **Full-page groups:** simple and page-faithful, but the measured body is over the limit and background transmission is broad.
* **Meaningful-block consolidation:** potentially best fidelity/size trade-off, but requires a new deterministic, source-faithful consolidation/filtering rule and review evidence; none exists today.
* **Multiple requests:** not allowed by the accepted V8 capability (`batching=false`) or R5 one-call scope.

The accepted ingestion principle requires faithful transcription of owner-selected evidence, not silent content selection. Consequently the current deriver is acting as an accidental content/geometry selector rather than a sufficient transcription representation for this scanned PDF.

## Root cause and governance impact

Primary classifications are **F — Combined defect**, comprising **C — Representation-model mismatch** (pixel whitespace candidates are not semantically suitable for this scanned-document class) and **E — Cardinality/shaping defect** (although a ≤32 quota allocator exists, the required truthful consolidation/representation strategy and one-call/body feasibility are not established for this document). A secondary **D — Pipeline composition defect** is present because raw candidate geometry is fed directly to V8 transport grouping without a governed real-PDF consolidation stage.

This is not a provider defect and not a V8 response-schema defect. The immutable evidence, page-rendering provenance and V8 capability identity/digest remain valid. Any remediation would change page/region preparation behavior and implementation identity; it would require a new artifact build, artifact acceptance, deployment and implementation-bound V8 capability acceptance. Existing R5F geometry remains historical diagnostic state and must not be rewritten or reused as corrected geometry. The capability digest should remain unchanged only if the remediation preserves the accepted V8 request/response semantics; that must be re-verified by the next unit.

## Smallest safe remediation recommendation

Do not implement in R5G. The next governed unit should define and test one bounded, deterministic real-PDF preparation strategy that:

* preserves the immutable PDF as authority;
* creates a new representation/region-set identity (never overwriting R5F);
* removes scan/background artefacts or consolidates only under explicit deterministic rules;
* guarantees complete page/source coverage and traceable geometry;
* produces one request of 1–32 regions and ≤16,777,216 encoded bytes;
* retains owner review for any remaining ambiguity;
* preserves raw-before-parse and all existing V8 semantics.

The recommended next unit is **OI11R5H — Governed Real-PDF Region Consolidation and One-Call Request-Fit Implementation Scope/Remediation** (or the repository’s equivalent governed name). It must first decide whether a deterministic meaningful-block derivation or another source-faithful representation is truthful; it must not auto-approve the existing 72-region set. After implementation, the normal build/preserve/accept/deploy/new implementation-bound acceptance sequence is required before any real execution. No owner order should be requested for the current 72-region set until the corrected geometry is established.

## Integrity and accounting

The registered evidence, manifest, R5F representation files, region-set digest, ambiguity state and review artefacts remain unchanged. The observed programme stores remain at the R5F post-state (evidence 29, manifests 29, capability acceptance 9, owner authorizations 11, attempts 8, provider state 6, derivative generations 22, derivative content 20); this unit added no governed records. OpenAI calls 0, Claude calls 0, other external calls 0, retries 0, external egress 0. A temporary local diagnostic compilation attempt encountered the previously observed JVM heap limitation and was removed; it made no repository or production mutation. `git diff --check` passes for this report.

## Verdict

**READY FOR BOUNDED IMPLEMENTATION.** The exact representation and cardinality failure is explained, the current R5F state remains fail-closed forensic evidence, and a bounded remediation path is identified without changing the accepted V8 capability identity or digest at this stage.

UNIT ORDINARY-INGESTION-11R5G COMPLETE — THE REAL-DOCUMENT REGION-DERIVATION AND V8 CARDINALITY FAILURE HAS BEEN FORENSICALLY EXPLAINED. THE EXISTING OI11R5F REGION SET REMAINS IMMUTABLE DIAGNOSTIC EVIDENCE AND HAS NOT BEEN OWNER-ORDERED. A BOUNDED IMPLEMENTATION REMEDIATION HAS BEEN IDENTIFIED WITHOUT CHANGING THE ACCEPTED V8 CAPABILITY IDENTITY OR DIGEST. NO PRODUCTION MUTATION, PROVIDER AUTHORIZATION, PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. IMPLEMENTATION MUST NOT BEGIN UNTIL THE INVESTIGATION RESULT IS REVIEWED.
