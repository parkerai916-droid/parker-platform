# OI11R5F — Governed Real-Document Representation Preparation and Owner Source-Order Review

## Disposition

Local preparation completed and stopped at the required owner-review gate. No authorization, provider request, transcription, derivative admission or external egress occurred.

## Baseline and source verification

Repository started at HEAD/upstream `f5383795227d313be33547dceb7ad0f6ccdf3149`; production remained on implementation `9e2a900fee388ebf4787817c24f34a63190b3f0d`, image/index `sha256:c121ea0d5c55c32a8cec9b38eade8ecb5f2d72f5331a8ed761b10fb8cfef0ae4`, container `34e0637eaa2b32c3e4c43b3e29c274b3da2d7a59c641cc5a2e25143465ba36b4`, restart count 0 and readiness PASS. The selected source was verified as evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, 1,887,733 bytes, application/pdf, five pages.

## Preparation results

PDFBox `3.0.7` at `300 DPI` rendered all five pages at `2479 × 3508`. Page representation IDs are `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e`, `669f1af75d9cdd4768305258e4f73de441a5d71342e7e043ed7d7b8276568c39`, `8e6751ee97d3c1d66983aef8c2c72c8735714d148db0cde4b54b54b9467e09a8`, `e65b472cd7fd30d22a470ad6c1fbb22122754443cc1585ba915ecf9e546eeecc`, and `eb7ea4a7c78af09554f52ad63fcfe7b122b9bb5b23563a488b269fdd9bf23c44`. Derived region counts were exactly `72 / 3 / 4 / 1 / 12`. Pages 2–5 persisted `DETERMINISTIC_SOURCE_ORDER`; page 1 persisted `SOURCE_ORDER_REVIEW_REQUIRED` over 72 regions. Its region-set digest is `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f`.

Representations, geometry and order records are stored under `/mnt/parker-data/parker/ordinary-ingestion-representations/evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, with five page binaries/metadata files, five geometry files and five order files. All records were read back by the governed store; source and manifest were not rewritten.

## Owner-review surface

The deterministic annotated page-1 review image is `/home/steve/parker-owner-review/oi11r5f/page-1-regions-annotated.png`, SHA-256 `6f98e2770dfd88668fbe7373ae2a22696411354566869bf6ee581b5065598d19`. The structured review manifest is `/home/steve/parker-owner-review/oi11r5f/page-1-regions-review-manifest.json`, SHA-256 `cfd6146895fc3597f4ed31a29bd1a90d94466ececb3a1774ff3caa8c90ba35de`. The image overlays all 72 stable region boundaries and ordinal labels on the exact rendered page; the manifest binds each region ID to geometry and the exact region-set digest.

## Store accounting and boundaries

Before preparation, production counts were evidence 29, manifests 29, capability acceptance 9, owner authorizations 11, attempts 8, provider state 6, derivative generations 22 and derivative content 20; representation stores were empty for this evidence. After preparation, only five page representations, five geometry records and five order-state records exist in the bounded preparation root. Evidence, manifests, owner-order resolutions, authorization, attempts, provider state and derivatives were unchanged. OpenAI, Claude, other external calls and retries were `0 / 0 / 0 / 0`.

## Required owner action

No `OWNER_RESOLVED_SOURCE_ORDER` record was created. Parker’s V8 shaper remains blocked with `SOURCE_ORDER_REVIEW_REQUIRED` until Steven inspects the staged annotated image/manifest and explicitly approves a complete ordering over the exact 72-region set digest above. The next separately governed unit may then persist that owner resolution and proceed only under its own authorization and egress gate.
