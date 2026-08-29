# FA.9.4P-A1 Deterministic Source-Region Geometry and Order Graph

**Unit:** FA.9.4P-A1E-R6.2
**Status:** Implemented and verified on Ubuntu in an isolated workspace. No OCR, text extraction, provider request, acceptance execution, production mutation, runtime rebuild, or deployment occurred.

## 1. Purpose and boundary

R6.2 implements only this governed transformation:

`authoritative evidence -> deterministic R6.1 page -> deterministic source regions -> geometry/order graph`

The result supplies exact, source-bound visual regions for a later external high-fidelity transcription mechanism. It does not understand or transcribe document content. The derivation consumes only the canonical RGB pixels and provenance of the R6.1 page representation; it does not inspect PDF text objects, fonts, OCR output, language, provider output, or semantic meaning.

Authoritative verification ran on Ubuntu host `parker` from base commit `4fc9bfe6da904554af59b5c2f2f52266bf358f1d` in isolated workspace `/tmp/parker-fa-9.4p-a1e-r6.2-OtFkZYeS`. The production checkout and stores remained outside the implementation workspace.

## 2. Region contract

Each `SourceRegion` binds:

- the custody `EvidenceArtifactId` and source SHA-256;
- the R6.1 `PageRepresentationId`, page number, dimensions, and canonical page-pixel digest;
- exact integer half-open pixel bounds `[x0,y0,x1,y1)`;
- the derivation profile ID/version;
- a minimal pixel-derived structural class;
- the canonical R6.1 crop-pixel digest;
- a deterministic `SourceRegionId`.

Bounds are validated by the existing `PixelCropBounds` and crop primitive: both dimensions must be positive, non-negative, and within the page. Exact processing-representation pixels are authoritative. Normalized coordinates are intentionally deferred: a future provider crop and its region share the same versioned R6.1 raster, so scale-independent coordinates add no current binding value and cannot replace exact pixels.

## 3. Identity and crop binding

Region identity is SHA-256 over length-prefixed canonical fields under domain `parker.source-region.identity.v1`: source artifact and digest, page representation, page number and dimensions, exact bounds, derivation profile/version, structural class, and crop digest. It excludes timestamps, randomness, paths, OCR, provider text, and provider IDs.

The deriver invokes the R6.1 deterministic crop primitive for every region. Consequently the region records the digest of the exact canonical RGB pixels selected by its page representation and bounds. Focused tests demonstrate that re-cropping the same representation at the recorded bounds yields the same digest, and ten repeated derivations yield identical region counts, identities, bounds, crop digests, classes, edges, and ambiguity state.

## 4. Derivation algorithm and granularity

The version-1 profile is `pixel-whitespace-source-regions-v1`.

1. Recompute and verify the canonical page-pixel digest.
2. Mark raster rows containing the bounded minimum number of dark pixels.
3. Form occupied row bands with a governed blank-row tolerance.
4. Split each band at governed horizontal whitespace gaps.
5. Merge vertically adjacent, horizontally overlapping or aligned line boxes within the governed interline gap.
6. Apply fixed padding, clamp to page bounds, reject undersized boxes, and enforce the maximum region count.
7. Crop, classify, identify, sort canonically, reject identity collisions, and derive graph relationships.

This produces visual line groups/structural blocks: larger than words or characters, but smaller than a whole document. It retains enough local layout context for bounded future transcription. A later request may send the exact region crop and coordinates plus the full page as optional context, while requiring output to bind to Parker's region ID.

Structural classes are limited to `TEXT_LIKE`, `IMAGE_LIKE`, `TABLE_LIKE`, `RULE_OR_SEPARATOR`, `MIXED`, and `UNKNOWN`. Classification uses only pixel density and sustained horizontal/vertical occupancy. It does not assert actual text, headings, legal propositions, signature meaning, or importance.

## 5. Geometry order graph

`SourceRegionOrderGraph` records provider-independent `BEFORE`, `CONTAINS`, and `COLUMN_PEER` edges and one of `UNAMBIGUOUS`, `HUMAN_ORDER_REQUIRED`, or `NOT_YET_SUPPORTED`.

- Vertically separated regions with horizontal overlap receive a top-to-bottom `BEFORE` edge.
- Containment is recorded without inventing reading order.
- Horizontally separated regions with vertical overlap are `COLUMN_PEER`; the implementation does not flatten columns into naive global y-order or impose a traversal unsupported by geometry.
- Overlapping, non-contained region bounds produce `HUMAN_ORDER_REQUIRED`.

Tables remain bounded `TABLE_LIKE` regions; cell traversal is deferred. Authorization/signature blocks, headers, footers, separators, and mixed-media blocks remain spatially present and are never silently discarded or semantically relocated.

## 6. Failure and resource boundary

Fail-closed outcomes represent invalid page bytes/pixel shape, page digest mismatch, invalid geometry, crop digest mismatch, identity collision, excessive region count, derivation nondeterminism, and unsupported layout. Ambiguous traversal is represented on the graph as human review required rather than guessed.

The default profile bounds work to 1,000 regions per page, at least two dark pixels per occupied row, minimum 4 x 4 pixel regions, finite whitespace/alignment tolerances, and the R6.1 page byte/dimension/pixel limits. The implementation performs deterministic row/column scans and bounded crop creation; it adds no connected-component explosion or unbounded semantic analysis.

## 7. Verification evidence

The synthetic focused matrix covers single-column prose, emphasized insertion, two columns, a table grid, authorization/signature geometry, header/footer, mixed image-like pixels, large whitespace, overlapping derived bounds, ambiguous competing order, thin separators, small blocks, invalid bounds, source/crop binding, digest validation, and region-count limits. The focused Ubuntu test gate passed.

The read-only A1 witness was `evidence-0275472f-535a-4cf1-b30d-f45ac7684743`, SHA-256 `7373ad403b4fae5bf5c777deb8524eaa3ba38594ce9fabfa8fcbce22fbd33182`, length 111,122 bytes, three pages. Each page was rendered at the governed R6.1 300-DPI profile and independently derived ten times.

| Page | Regions | Ambiguity | Demonstrated relationship |
|---:|---:|---|---|
| 1 | 5 | `UNAMBIGUOUS` | deterministic page-region graph |
| 2 | 15 | `UNAMBIGUOUS` | emphasized proposition region `[235,951,2178,1208)` occurs after its preceding prose region and before its following prose region |
| 3 | 4 | `UNAMBIGUOUS` | authorization region `[236,107,2252,1295)` occurs before closing prose |

The source raster geometry, not the failed GPT transcription, is the regression oracle. The page-2 and page-3 relationship assertions both passed. No OCR or transcription was used to locate or classify the asserted regions.

## 8. Repository surface and production boundary

Reused: R6.1 page representation, provenance, canonical pixel digests, deterministic crop primitive, evidence identity, and Kotlin/JUnit conventions.

Added:

- `src/interfaces/SourceRegionGeometry.kt`;
- `src/runtime/DeterministicSourceRegionDeriver.kt`;
- `tests/runtime/DeterministicSourceRegionDeriverTest.kt`;
- `tests/runtime/R62A1GeometryEvaluationTest.kt`.

Unchanged: acquisition routing, OCR and transcription adapters, provider schemas, admission/persistence, authorities, attempts, generations, derivatives, reviews, analyses, Memory, Knowledge, UI, runtime composition, production image/container, and stores.

## 9. R6.3 prerequisites and unresolved issues

R6.3 may construct a provider-neutral request containing `region_id`, page number, exact region bounds, region digest, and region image, optionally accompanied by full-page context. Returned text must bind to Parker's region ID, and geometry-aware admission must preserve this graph rather than trusting provider array order.

Exact unresolved issues are:

1. reliable table cell traversal remains deferred/human-required;
2. ambiguous multi-column traversal remains human-required unless a separately versioned profile demonstrates a unique traversal;
3. durable page/region/crop/graph persistence and replay are not implemented;
4. future external request/response schema, validation, comparison, and human review are not implemented;
5. broad source-class and cross-build geometry reproducibility require their own acceptance evidence.

Recommended next unit: `FA.9.4P-A1E-R6.3 — PROVIDER-NEUTRAL REGION-BOUND EXTERNAL TRANSCRIPTION CONTRACT`.
