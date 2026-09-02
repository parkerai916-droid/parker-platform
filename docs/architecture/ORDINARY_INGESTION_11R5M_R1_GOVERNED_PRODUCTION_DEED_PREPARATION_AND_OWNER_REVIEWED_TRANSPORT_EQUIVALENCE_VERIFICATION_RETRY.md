# OI11R5M-R1 — Governed Production Deed Preparation and Owner-Reviewed Transport Equivalence Verification Retry

## Verdict

**B — TRANSPORT EQUIVALENCE FAILURE**

The remediated preparation-only operation successfully created and read back a deterministic five-page corrected preparation, and every request/size/persistence gate passed. However, exact owner-reviewed equivalence passed for only pages 1, 2 and 5. Production page 3 equals the transport previously labeled owner-review page 4, while production page 4 equals the transport previously labeled owner-review page 3. Per-page equivalence is therefore 3/5, not the required 5/5. No execution authorization or provider operation was attempted.

## Starting state and remediation history

The host was `parker`, repository `/home/steve/parker-platform`, branch `main`. Starting HEAD and upstream were both exactly `3cbb5f6f62f860e9866275b9759e87943edceacd`; the worktree was clean and `git diff --check` passed.

Original OI11R5M correctly ended `C — PREPARATION OR REQUEST GATE FAILURE` because production lacked a preparation-only operation and durable corrected-preparation store. R5N implemented that bounded capability; R5O built/preserved it; the owner accepted it; R5P deployed it; and R5Q established implementation-bound V8 authority. This retry reached the canonical production preparation path that R5M could not reach.

The original R5M report remains unchanged at `docs/architecture/ORDINARY_INGESTION_11R5M_GOVERNED_PRODUCTION_DEED_PREPARATION_AND_OWNER_REVIEWED_TRANSPORT_EQUIVALENCE_VERIFICATION.md`, SHA-256 `52cc79d2bda9286547cf2d29a6e1740e226a0ee6e5d0347df8612cfae63c16a4`.

## Production identity and V8 authority

| Field | Exact value |
|---|---|
| Container | `8b7c4b9b9f1b374de278e37d2f01c8401bc8ab809516d21135ddebf1e8065d7c` |
| Image/index | `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| Embedded source | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| Started | `2026-09-02T08:57:38.782928781Z` |
| Restart count | 0 |
| Readiness | PASS |

Before preparation, the canonical evaluator returned `ACCEPTED` for capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, and exact implementation `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`. Acceptance record `206d713979522d79d4094699e8eef1c15e0aded801ae8626bf7c74e7629e6f86` remained SHA-256 `a89619c47d424a67942d0ea945387126de8d0515bbd900fa6b54600aefb65382`. This was capability authority only.

## Evidence identity

Canonical custody contained exactly:

| Field | Exact value |
|---|---|
| Evidence ID | `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9` |
| Source SHA-256 | `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e` |
| Source size | 1,887,733 bytes |
| MIME | `application/pdf` |
| Pages | 5 |
| Manifest SHA-256 | `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34` |

The source and manifest matched before mutation.

## Historical R5F preservation

Historical page-1 representation `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e` remains separate. Its geometry file remains SHA-256 `8c8d9949b7fa9308381c3be3915e8ab5f78c4c0575cf30315481a4296565fcb4`; its order file remains `99b594592e18d812da7750e84873071a6ed51604a4a8a17708bbf3cf3ed70e79`, region-set digest `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f`, disposition `SOURCE_ORDER_REVIEW_REQUIRED`. No owner resolution, reinterpretation, corrected promotion or constituent reuse occurred.

## Canonical preparation-only operation

The authenticated operation invoked was exactly:

```text
POST /owner/admin/corrected-preparation/evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9
{"profileId":"full-page-achromatic-png-preparation-v1","profileVersion":"1"}
```

It returned `PREPARED`. The profile was exact version 1. `readbackVerified` was true. Successful completion proves the production chromatic-risk gate returned PASS; no override or fallback was used. The governed transform remained `Y=(77R+150G+29B+128)>>8` and the accepted deterministic PNG encoder produced all transport bytes.

## Geometry, ordering and corrected-preparation identity

| Field | Actual result |
|---|---|
| Preparation identity | `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f` |
| Region-set digest | `bc684a53cb20425580c80664658df3bd6d0515adcefcbe10e827faff87596e56` |
| Source pages | 5 |
| Preparation regions | 5 |
| Request regions | 5 |
| Order | `[1,2,3,4,5]` |
| Bounds | `[0,0,2479,3508)` on every page |
| Coverage | 100%, multiplicity 1 |
| Order state | `DETERMINISTIC_SOURCE_ORDER` on every page |
| R5F constituents | 0 |

The create-once record is `/mnt/parker-data/parker/corrected-preparations/records/85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f.json`, SHA-256 `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`, size 12,202,837 bytes. Five content-addressed transport PNGs were persisted. Canonical codec readback reproduced the stored governed document exactly. No record was overwritten or duplicated.

## Page identities and production transports

| Page | Authoritative representation | Authoritative pixel digest | Preparation ID | Preparation-region ID | PNG bytes | Production transport SHA-256 |
|---:|---|---|---|---|---:|---|
| 1 | `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e` | `0f21b3dd59267b543ab6281c188300105f8a1144feec86503c643f678444081e` | `61c8b55a24885d488d63ad66adf923481ed1368a9123fa67487da6d709bb9f1f` | `d9e87671239b8d7fab1d3c0b6790c250d5f7579b3aec07309fba0caf142ee1c2` | 1,380,662 | `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c` |
| 2 | `669f1af75d9cdd4768305258e4f73de441a5d71342e7e043ed7d7b8276568c39` | `66c355744985e981071d0a6786331f1b8aa721efd25216925fcee9b40c4d7eff` | `6ffcad7cd1b3cf4fcd167e9fcb1d61b813b2a79c876ab0fe7879d40d790b7f56` | `196be4bff8a14b891e1fce7d361d60354502a6bac4aea251dd860d2bf8540040` | 2,236,986 | `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae` |
| 3 | `e65b472cd7fd30d22a470ad6c1fbb22122754443cc1585ba915ecf9e546eeecc` | `de557d9ffe6c421cd8464c78ed172047061b6a6e87556a388cd552ae2bc85395` | `281fad6bd0773a90288970980ebc4219ef6408d5e9727e646ac1cda0e6ce18b7` | `56091794e46942f972316ae5a47170ec5efcc4f5447beba58a2c58b27b753fa3` | 1,459,264 | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` |
| 4 | `8e6751ee97d3c1d66983aef8c2c72c8735714d148db0cde4b54b54b9467e09a8` | `a5ddd17c5da7ae13dcb1cc0e1aeb8661e4dd38d4b08f079b0f7e896caf2020d4` | `804b30113c49b8d296087b6c9964470881e4e257522d687536f0deb65d4873f9` | `c1bc9c7a94cb8882314d506581d942b51e2e9045e971f33a6b4f98668e4c90ea` | 2,084,632 | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` |
| 5 | `eb7ea4a7c78af09554f52ad63fcfe7b122b9bb5b23563a488b269fdd9bf23c44` | `1489a0e74395e441a94b8c5fd1799c05cc9b0f59174f8fa4db31cd2f3e3d4f0b` | `c9887957648f9afeaf80e478c0e48955ff449f2a7c425cac7fda46675364ccc9` | `ef79c2e21276979b69d2221301812b65fcfccb8186880906eb5163a991b7f9ba` | 1,985,949 | `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816` |

## Exact owner-reviewed equivalence failure

| Page | Expected owner-reviewed SHA-256 | Production SHA-256 | Result |
|---:|---|---|---|
| 1 | `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c` | same | MATCH |
| 2 | `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae` | same | MATCH |
| 3 | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` | MISMATCH; equals reviewed page 4 |
| 4 | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` | MISMATCH; equals reviewed page 3 |
| 5 | `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816` | same | MATCH |

Result: `OWNER_REVIEWED_TRANSPORT_EQUIVALENCE = FAILURE (3/5 per-page exact; page 3/page 4 transposed in prior review package)`.

The aggregate set of five transport bytes is identical and aggregate size is exact, but set equality cannot substitute for page-bound equivalence or authoritative order.

### Root classification

Production follows the registered PDF's true page order. Immutable historical representation metadata says:

- representation `e65b472c…`, production page 3, has `source_page: 3` and RGB SHA-256 `3de037da…`;
- representation `8e6751ee…`, production page 4, has `source_page: 4` and RGB SHA-256 `95e8b475…`.

The R5I local acceptance harness function `loadGovernedDeedPages` hard-coded `8e6751ee…` third and `e65b472c…` fourth, then assigned new provenance using `index + 1`. Consequently it relabeled actual source page 4 as review page 3 and actual source page 3 as review page 4. R5I/R5J reports and review filenames inherited that transposition. Production rendering did not reproduce the erroneous fixture labeling.

This is an owner-review fixture/page-binding defect, not visual similarity, compression, chromatic-risk, request-size or production rendering failure. The strict equivalence gate nevertheless requires verdict B.

## Transport and canonical request size

| Gate | Actual | Limit | Result |
|---|---:|---:|---|
| Aggregate PNG | 9,147,493 bytes | 10,875,000 | PASS |
| Aggregate base64 | 12,196,664 characters | 14,500,000 | PASS |
| Canonical UTF-8 body | 12,202,080 bytes | 16,000,000 | PASS |
| Absolute V8 body | 12,202,080 bytes | 16,777,216 | PASS |

The locally constructed canonical request contains five request regions in `[1,2,3,4,5]`, no R5F constituents, no batching, and represents one intended provider call; actual provider calls were zero.

Actual request digest is `2f4f595decb924fd6d252735494dabc85b8e375c4d17e41f952195061e2675a3`. Actual canonical body digest is `5a847355cab3217a8b1309ca82dc47f2d38239395e6cdede0108fe85c53f6603`.

The 52-byte increase from R5I's 12,202,028-byte body is fully explained by the governed correlation ID: local R5I used 24-character `oi11r5i-local-acceptance`; production used deterministic 76-character `preparation-5ed9cc0664ee153d35b2bba5ff2ffad8400f3d1dd6c24ada1961d3d689b4309c`. This legitimate 52-character field difference changes request/body digests and size. It does not explain or cure the page-binding failure.

Structural construction contained only the five governed page images, PDF-page order, registered evidence and corrected-preparation provenance, frozen V8 schema/instructions and required provider metadata. No unrelated evidence, attachment, context, memory, external-reasoning prompt or R5F region was included. No sensitive body content is reproduced here.

## Store accounting and execution boundary

| Governed store | Before | After | Delta |
|---|---:|---:|---:|
| evidence | 29 | 29 | 0 |
| evidence source manifests | 29 | 29 | 0 |
| capability acceptances | 11 | 11 | 0 |
| owner authorizations | 11 | 11 | 0 |
| attempts | 8 | 8 | 0 |
| provider state | 6 | 6 | 0 |
| derivative generations | 22 | 22 | 0 |
| derivative content | 20 | 20 | 0 |
| corrected-preparation files | 0 | 6 | +6 authorized create-once files |

The corrected delta is one canonical record and five content-addressed PNGs, all identified above. Every other store's complete aggregate digest remained unchanged.

No evidence-specific authorization, execution ID, reservation, attempt, provider state, provider response, derivative or transcription was created. OpenAI calls: 0. Claude calls: 0. Other external provider calls: 0. Retries: 0. External provider/evidence egress: 0.

## Production stability and stop state

Production remained the same container, image, source and start time, restart count zero, readiness PASS, and V8 evaluator `ACCEPTED`. No deployment or restart occurred.

The corrected preparation is durably persisted but fails the required owner-reviewed per-page equivalence gate. It is not authorized for provider execution. Further work requires a separately governed correction/review of the page-3/page-4 owner-review binding before any execution decision.
