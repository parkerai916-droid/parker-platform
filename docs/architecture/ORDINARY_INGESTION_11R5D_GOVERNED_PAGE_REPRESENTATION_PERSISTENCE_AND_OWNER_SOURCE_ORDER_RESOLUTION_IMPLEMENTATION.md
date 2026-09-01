# OI11R5D — Governed Page Representation Persistence and Owner Source-Order Resolution Implementation

## Disposition

Implementation complete; provider-free verification passed. The page-1 region graph remains `SOURCE_ORDER_REVIEW_REQUIRED`; no owner order was fabricated.

## Traceability and scope

Implemented from OI11R5C (SHA-256 `cda3c216c10785132518ceefdb25123d906c7ec62b2fd4d912556199204b4c86`) at source HEAD `d45d9e84011238d3a047496927eaaee6db0ec620`. The immutable source remains Evidence Custodian artifact `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9` with SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e` and manifest SHA-256 `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34`.

## Implementation

`GovernedPageRepresentationPersistence` defines the write-once page, geometry and order-state boundary. `FileSystemGovernedPageRepresentationPersistence` stores immutable encoded page bytes plus provenance metadata, geometry records and order records using atomic creation. `InMemoryGovernedPageRepresentationPersistence` supports isolated tests. Page identities continue to be deterministically derived by `DeterministicSourcePageRenderer`; `SourceRegionSetIdentity` deterministically binds the exact region set.

`SourceRegionOrderState` persists either `DETERMINISTIC_SOURCE_ORDER` or `SOURCE_ORDER_REVIEW_REQUIRED`. `OwnerSourceOrderValidator` accepts only a complete, duplicate-free ordering over the exact region set and emits a distinct `OWNER_RESOLVED_SOURCE_ORDER` record. `SourceRegionOrderGraph.withOwnerOrder` supplies that order to shaping without changing geometry. The V8 shaper accepts owner order only through its explicit optional map; absent resolution remains fail-closed.

## Candidate verification

The existing PDFBox 3.0.7 / 300 DPI renderer and raster region deriver remain local and unchanged. The five-page candidate renders at the governed 2479×3508 dimensions; deterministic ordering is retained where unambiguous and page 1 remains blocked pending owner review. No OCR or remote service is used.

## Tests

Focused persistence/owner-order tests passed. Full `./gradlew test` passed: 3,304 tests, 17 skipped, 0 failures, 0 errors. `git diff --check` passed.

## Governance and preservation

V8 capability identity and digest are unchanged (`ordinary-external-request-region-transcription-v8` / `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`). Source evidence, manifests and historical OI11R5A/B/C and OI11R4 records are not rewritten. No provider authorization, provider state, derivative, representation or owner-order record was created for the real candidate during this unit; tests use isolated in-memory state only. Provider calls/retries/external egress were `0 / 0 / 0`.

Because implementation identity changes, the next governed sequence must build and preserve a replacement artifact, obtain explicit artifact acceptance, deploy it, create implementation-bound V8 acceptance for the new commit, and verify evaluator acceptance before any real-document preparation or execution. OI11R5E must provide the owner-facing review of page 1's 72 regions; only then may a separately governed real-document execution proceed.
