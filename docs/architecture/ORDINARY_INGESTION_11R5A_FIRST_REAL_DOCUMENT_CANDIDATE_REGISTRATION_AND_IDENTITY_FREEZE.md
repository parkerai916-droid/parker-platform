# OI11R5A — First Real-Document Candidate Registration and Identity Freeze

## Disposition

**STOPPED — request-boundary derivation blocker.** The owner-selected PDF was registered exactly once, but the canonical D335 V8 builder cannot derive a request-region set from the registered source under current representations. No authorization, provider call, retry, or derivative was created.

## Source verification and registration

- Host/repository/branch: `parker`, `/home/steve/parker-platform`, `main`
- Starting HEAD/upstream: `66db0fbf27617513cc3f4c20e79d2781d50e8803`
- Original filename: `Deed of Representation Michael Kellec.pdf`
- SHA-256: `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`
- Size: `1,887,733` bytes
- MIME: `application/pdf`
- PDF validity/page count: valid PDF, `5` pages

Exact SHA duplicate search found no existing Evidence Custodian match. Canonical multipart registration returned evidence ID `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`. Registered bytes at `/data/evidence/evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9.evidence` have the identical SHA-256 and size. Its manifest is `/data/evidence-source-manifests/evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9.manifest`, SHA-256 `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34`.

## Acquisition and privacy boundary

Read-only acquisition returned `PROPOSED`: `application/pdf`, 5 pages, scanned/image-only characteristics, V8 capability available, and owner review required. The document contains personal/legal-representation information; any future egress must be limited to explicitly owner-approved pages/regions and OpenAI profile `openai-fidelity-first-transcription-v1`, model `gpt-5.6-sol`, purpose `evidence-intelligence.external-transcription`. No substantive legal analysis was performed.

## Request-boundary derivation

The canonical renderer and region deriver were run offline against the registered bytes. All five pages rendered at 2479×3508 pixels. Derived source-region counts were page 1: 72, page 2: 3, page 3: 4, page 4: 1, page 5: 12. The complete-set shaper rejected the graph because page 1 has an ambiguous source-order graph requiring human order review; the V8 builder therefore raised `request-region shaping failed`. No truthful V8 request-region set or provider request size was produced.

The renderer/deriver are production runtime classes (`DeterministicSourcePageRenderer`, `DeterministicSourceRegionDeriver`), not synthetic-only helpers, and use Apache PDFBox 3.0.7 at the governed 300-DPI raster profile. Evidence Custodian registration itself persists source/manifest metadata only; page representations and geometry are in-memory preparation outputs and are not persisted by registration. The immediate blocker is thus real-document source-order ambiguity, compounded by the absence of a persisted representation/geometry preparation stage. The proposed all-pages boundary (pages 1–5) cannot proceed to owner acceptance until a separately governed preparation/review step resolves this ambiguity without silently omitting pages.

This is not a provider or privacy decision. No workaround, silent page omission, authorization, or provider call is permitted in OI11R5A.

## Store accounting and integrity

Before registration: evidence/manifests `28 / 28`; owner authorizations `11`; attempts `8`; provider state `6`; derivative generations/content `22 / 20`. After registration: evidence/manifests `29 / 29`; all other stores unchanged. This is the single governed Evidence Custodian registration mutation. Production remained on the accepted D335 image, running with restart count `0`; capability/profile state was unchanged. Provider calls, retries, and external egress were `0 / 0 / 0`.

## Next boundary

OI11R6 cannot begin. A separately governed representation-preparation/remediation unit must first make the exact registered source deterministically shapeable, after which the owner must review and accept the exact document and egress boundary. The registered source remains preserved and no second fixture was created.
