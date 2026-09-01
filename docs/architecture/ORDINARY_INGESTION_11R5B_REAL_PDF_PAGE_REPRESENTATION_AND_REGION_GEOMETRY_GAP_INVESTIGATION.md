# OI11R5B — Real-PDF Page Representation and Region-Geometry Gap Investigation

## Verdict

**GOVERNANCE DECISION REQUIRED.** The exact gap is proven, but persisting page representations and resolving real-document source-order ambiguity requires an explicit bounded governance decision before implementation.

## Starting state and preserved evidence

Host `parker`; repository `/home/steve/parker-platform`; branch `main`; starting HEAD/upstream `5a1d451063def1da771c3e3c6b9ec8c3ed2be473`; worktree clean. Production remained running on D335 image `sha256:5ca82f03ce61eade58c60eb4d3783547b4b266f974ed2ac218c09cf43f86075a`, restart count 0, readiness READY, capability ACCEPTED. Provider calls/retries/external egress were `0 / 0 / 0`.

The registered owner-selected source remains immutable: evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, 1,887,733 bytes, PDF, 5 pages. OI11R5A’s `request-region shaping failed` remains historical evidence.

## Synthetic and real path comparison

The successful synthetic path and the real path both use production classes: `DeterministicSourcePageRenderer` (Apache PDFBox 3.0.7, 300 DPI), `DeterministicSourceRegionDeriver`, `DeterministicCompleteSetRequestRegionShaper`, and `CanonicalRequestRegionV8Builder`. Rendering and geometry are generated in memory; Evidence Custodian registration persists only source/manifest metadata, not page-rendered representations or geometry records.

| Concern | OI11R4K synthetic | Registered real PDF | Gap |
|---|---|---|---|
| Source/manifest | exact source and manifest | exact source and manifest | none |
| Page rendering | production renderer, in-memory | production renderer, in-memory | no durable representation stage |
| Dimensions | governed raster provenance | 2479×3508 each page | none |
| Derived regions | bounded and shapeable | page counts 72/3/4/1/12 | page 1 order graph ambiguous |
| Region geometry | deterministic pixel bounds | deterministic pixel bounds produced | ambiguity requires human order review |
| V8 shaping | succeeds | rejects with `request-region shaping failed` | shaper fail-closed on ambiguity |

## Exact preconditions and failure

V8 requires page identity/count, rendered pixel representation, dimensions, source digest linkage, source-region IDs/bounds, deterministic ordering, and a complete set within 32 request regions and 16,777,216-byte body bound. The renderer produced all five pages and the deriver produced regions, but page 1’s graph was not unambiguous; `DeterministicCompleteSetRequestRegionShaper` therefore returns `SOURCE_ORDER_REVIEW_REQUIRED`, which the V8 builder reports as `request-region shaping failed`. No request-region IDs were authorized for egress and no request body size was computed.

## Existing capability inventory

The renderer, deriver, cropper, page/region provenance types, and V8 shaper are production-capable and tested. OCR is not required for this preparation: rendering is raster-only and geometry is derived from pixel whitespace; no external OCR or provider call is involved. Existing Tier-A/Tier-B OCR components produce unrelated document derivatives and do not currently persist the V8 page/region representation contract. No PDFBox dependency addition is required (PDFBox 3.0.7 is already present).

## Authority and determinism boundary

The original PDF remains authoritative. Rendered pages and crops are deterministic, hash-linked derivatives requiring renderer identity/version/build, profile (300 DPI), dimensions, page number, source SHA, representation digest, and transformation parameters. They may be regenerated but each persisted generation must retain its own provenance. V8 must consume only representations whose source and geometry links verify.

The current geometry model is raster whitespace line/block detection, not semantic OCR. It can yield many regions and can mark overlapping order as `HUMAN_ORDER_REQUIRED`; silently collapsing or omitting page-1 regions would weaken source fidelity.

## Five-page feasibility

All five pages render successfully at 2479×3508. The page count is within the 1–32 page/request bound, but the derived region totals (72, 3, 4, 1, 12) trigger the page-1 ambiguity gate before a bounded request can be formed. Feasibility is therefore conditional on governed order resolution; no provider request size is truthfully available yet.

## Root cause and smallest remediation

Primary classification: **B — Existing renderer exists but lacks governed representation persistence/geometry integration**, with a secondary real-document source-order ambiguity. The smallest remediation is a bounded representation-preparation capability for registered PDFs that persists renderer/page/region provenance and introduces an explicit owner-review/ordering decision for ambiguous graphs, then feeds the existing V8 shaper without changing capability identity or digest. It must not become full document understanding or legal analysis.

Governance classification: **G3 — Existing representation type exists but codec/store support is required**, plus a bounded governance decision on human order resolution. Capability identity/digest remain unchanged; only implementation commit and implementation-bound artifact/capability acceptance would change after implementation.

## Integrity and next unit

No production representation, authorization, attempt, provider-state, derivative, or capability record was created in OI11R5B. The registered source remains unchanged. Provider calls/retries/egress remain `0 / 0 / 0`; production was not restarted or redeployed.

The exact next unit should govern the representation persistence and ambiguity-review mechanism for this evidence, then rerun deterministic shaping. Only after a valid bounded region set exists may an owner review and accept an OI11R6 real-document transaction.
