# ORDINARY-INGESTION-9 deterministic request-region coalescing implementation

## Outcome and baseline

Hybrid deterministic complete-set request-region coalescing is implemented as a parallel,
acceptance-pending v6 capability. It is not composed into production routing. Source regions remain
unchanged evidential geometry; every v6 request region is a subordinate transport representation with
an ordered, nonempty constituent source-region set.

Starting HEAD/upstream was `68e2be93efcb91885dfdf27fae14057af61ec9f0`, clean. The governing OI8
report SHA-256 was verified as `f60b228c37cd1763367957bc500a3a02326eaa9ac4e28b07916b9e38c1002a35`.
Production remained implementation `def611a8bf8cb6c2297f1d9bf6cd8146a58d4cbc`, image
`sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`, container
`281bba01fa82ddd4a172a424688845ea180a6dfe28eb4ae2aebd9c064ecd68ca`.

Implementation commit: `87764757a35a6df8d2491a0fe69608bafee5bca0`.

## Capability and model

The new capability is `ordinary-external-request-region-transcription-v6`, initially
`ACCEPTANCE_PENDING`. It has a deterministic identity digest over every governed surface and does not
consult or inherit the region-v5 acceptance store.

| Surface | v6 identity |
|---|---|
| Profile | `request-region-anchored-fidelity-acquisition-v2` |
| Schema | `request-region-anchored-transcription-schema-v2` |
| Wire | 6 |
| Processing | `external-transcription.deterministic-complete-set-request-region-v2` |
| Shaping | `complete-set-request-region-shaping-v1` |
| Adapter | `openai-responses-request-region-transcription-adapter` / `5.0.0` |
| Parser | `openai-request-region-structured-response-parser` / `1.0.0` |
| Provider/model | OpenAI / `gpt-5.6-sol` |
| Request/body bounds | 32 / 16,777,216 UTF-8 bytes |

`RequestRegion` retains evidence ID/SHA, page representation and number, page dimensions, union
bounds, canonical crop digest/image, structural class, derivation profile, and the full ordered
`SourceRegion` constituents. Thus the proof chain is request region -> constituent IDs and exact
source geometry -> canonical rendered page -> authoritative evidence. Singleton and coalesced modes
use the same model. Provider text is bound only to a request region; derivative blocks retain ordered
constituent IDs but do not fabricate constituent-level text.

## Exact algorithm and invariants

The shaper runs only after unchanged PDFBox 300-DPI rendering, source-region derivation, and graph
construction.

1. Reject more than 32 nonempty pages, page/graph mismatch, ambiguous/not-supported order, cycles,
   or an empty complete set.
2. At <=32 source regions, allocate one request region per source region.
3. Above 32, allocate quota one per nonempty page. Repeatedly assign the next quota to the page with
   maximum `sourceCount/currentQuota`; ties are page number then page representation ID, until 32.
4. Partition each page's Parker-ordered source regions into balanced consecutive intervals using
   `[floor(i*n/q), floor((i+1)*n/q))`.
5. Use the constituent-bounds union as the request crop. Never merge across pages. Retain inter-region
   pixels as crop context, not as new source regions.
6. Validate an exact ordered partition of the complete source-ID set: no missing, duplicate, unknown,
   altered, wrong-page, cross-page, non-adjacent, out-of-order, or bounds-inconsistent constituent.
7. Independently enforce 1..32 request regions and the exact 16-MiB encoded-body limit before any
   future reservation or provider attempt.

The request-region identity is a length-prefixed SHA-256 domain over
`parker.complete-region-set-group.identity.v1`, shaping version, page identity/number, source
derivation profile/version, union bounds, union crop digest, member count, and ordered member IDs.
There are no clocks, randomness, provider values, extracted text, or runtime-dependent inputs.

The strict v6 provider schema returns `request_region_id`, never a falsely promoted source-region ID.
The manifest includes request ID, page, union bounds/digest and ordered constituent IDs. Validation
requires the complete requested result set with unique IDs/pages and ordinals. Reconstruction ignores
provider order and emits blocks in Parker request order. Provider-state binding includes capability,
request digest, v6 adapter identity, request IDs and all memberships. Derivative binding preserves
request-level transcription plus membership; it never decomposes text among constituents.

## Sprint 2 proof

Authoritative evidence `evidence-4c6f2ee8-2f62-47be-bd7a-946c744b2766`, SHA-256
`ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5`, was read through an isolated
temporary copy only.

- source pages/counts: 3, `16/14/6`;
- source regions: 36, unchanged;
- request-region quotas/count: `14/12/6`, total 32;
- constituent coverage: 36/36; missing 0; duplicate 0;
- exact v6 body: 1,169,528 bytes, below 16 MiB;
- request digest: `b2f50f15d0f80eb66590a160de6e4a910609545214d89f04b67f66c671190968`;
- encoded request/manifest digest: `11baaf1818581d3e50c1a6a3069e52407eacb9e06a52d4131d8e484a14af9851`;
- three additional complete reconstructions produced identical identities, membership, geometry,
  order, request digest and manifest digest: PASS.

## Corpus regression

The production implementation processed the same 24 registered PDFs provider-free. It reproduced
the OI8 decision exactly:

- before: 15/24 eligible (62.5%);
- after v6 shaping: 17/24 eligible (70.83%);
- two remain `SOURCE_ORDER_REVIEW_REQUIRED`;
- five 63-region PDFs shape to 32 but remain above the body bound at 19,778,617 bytes;
- no successfully shaped document exceeds 32 request regions.

The temporary corpus copy was deleted after verification. No document contents were reported.

## Verification

Targeted `OrdinaryRequestRegionV6Test`: 8 tests passed. Composite and property-style coverage proves
1->1, 32->32, 33->32, 36->32, deterministic quota/ties/identity/geometry/order, exact membership,
same-page adjacency, fail-closed missing/duplicate/unknown/wrong-page/cross-page/non-adjacent/bounds,
ambiguous order, >32 nonempty pages, provider missing/duplicate/unknown/wrong-page results,
provider-order non-authority, provider-state membership, request-level derivative provenance, and no
provider transport. The retained production fixture proves the exact Sprint 2 and corpus cases and is
skipped unless an explicit read-only corpus fixture exists.

Full suite: 240 suites, 3,270 tests, 0 failures, 0 errors, 10 skipped. Region-v5 codecs, R6.9 replay,
authorization/execution, OI7B prepare-before-reserve, durable provider state, acceptance and ordinary
routing tests remain green. `git diff --check`: PASS.

## Acceptance-boundary delta

Changed semantic surfaces requiring new acceptance:

- distinct source-region/request-region model;
- versioned request-region identity and shaping mode;
- ordered constituent membership and complete-set validator;
- request union geometry and crop identity;
- v6 capability/profile/schema/wire/processing/instruction/adapter/parser identities;
- request manifest/digest and provider-state binding;
- request-region structured result validator and Parker-order reconstruction;
- request-level derivative blocks with constituent provenance.

Unchanged surfaces:

- authoritative evidence and custody-byte verification;
- PDFBox renderer, 300 DPI, pixel format and source-page identities;
- deterministic source-region geometry, IDs, bounds, graphs and ambiguity dispositions;
- provider/model/endpoint intent, original image detail, reasoning `none`, `store=false`;
- one request, single attempt, no batching/retry/truncation/omission;
- maximum 32 provider request regions and 16-MiB body bound;
- provider-state-before-assessment durability ordering;
- explicit owner authorization, exact-build acceptance and prepare-before-reserve separation;
- every historical region-v5 acceptance, authorization, provider state and derivative interpretation.

## Exact OI10 fidelity-acceptance plan

OI10 must create a new, build-bound v6 acceptance authority; it must not reuse R6.9 acceptance.

Fixtures:

1. A <=32 singleton fixture with known literal truth and visual anchors, proving the wrapper does not
   regress the accepted fine-region path.
2. Sprint 2 `36->32`, with the exact request digest/memberships above and human-reviewed literal truth
   for every request crop.
3. Source-order-sensitive multi-column/adjacent material whose expected Parker request and constituent
   order is locked before execution.
4. Mixed structural groups (TEXT_LIKE/TABLE_LIKE/MIXED and a long union crop), including uncertainty,
   paragraph/line anchors and inter-region whitespace.

Before any call, lock source SHA/page digests, request IDs, ordered memberships, bounds/crop/image
digests, request/manifest digest, expected literal Unicode and observation anchors. Review provider
output against the authoritative crop pixels and expected truth, not native PDF text. Pass only with
complete unique result coverage, exact page/ID binding, no omitted/added/reordered text, compliant
uncertainty/anchors, provider/model/schema provenance, Parker-order reconstruction, durable raw and
structured state, deterministic replay, and no constituent-level provenance fabrication.

Provider-call maximum: one call per governed fixture, maximum four total. No retry. Any transport
uncertainty, schema mismatch, fidelity discrepancy, missing result, identity/provenance mismatch or
durability ambiguity fails closed and stops promotion. Promotion/deployment remain separate later
units after OI10 evidence is reviewed and accepted.

## Safety and next step

OpenAI calls: 0. Claude calls: 0. Production governed-store mutations: 0. No image was built, no
Compose file changed, no container restarted, no capability accepted/promoted, and no evidence
authorized/executed. The exact next step is OI10: offline preparation plus the bounded, governed v6
fidelity-acceptance execution described above; do not deploy or ordinary-route v6 yet.
