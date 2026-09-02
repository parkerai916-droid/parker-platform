# OI11R5R — Owner-Review Page-Binding Reconciliation and Corrected Transport Equivalence Baseline

## Verdict

**A — REVIEW-BINDING DEFECT PROVED; CORRECTED OWNER REVIEW REQUIRED**

The R5M-R1 failure is strictly confined to the historical R5I/R5J owner-review page binding. Immutable source-page provenance and the persisted production preparation prove that production follows the registered PDF's true page order. The earlier review harness relabeled authoritative pages 3 and 4 in transposed positions. A new review-only package now presents the true page-3/page-4 pairs; owner visual review remains pending.

## Starting repository and R5M-R1 state

The host was `parker`, repository `/home/steve/parker-platform`, branch `main`. Starting HEAD and upstream were both exactly `69c2e40b12008877d1bf9d7ebc2ad6ee691f2f53`; the worktree was clean and `git diff --check` passed.

OI11R5M-R1 persisted a valid five-page preparation but returned `B — TRANSPORT EQUIVALENCE FAILURE` because production page 3 matched bytes historically labeled review page 4, and production page 4 matched bytes historically labeled review page 3. It did not authorize or execute the request. Its 12,202,080-byte canonical request remains historical diagnostic state and was not rebuilt in R5R.

## Production, evidence and preparation identities

Production remained exact throughout:

| Field | Exact value |
|---|---|
| Container | `8b7c4b9b9f1b374de278e37d2f01c8401bc8ab809516d21135ddebf1e8065d7c` |
| OCI image/index | `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| Embedded source | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| Restart count | 0 |
| Readiness | PASS |
| V8 evaluator | `ACCEPTED` for exact implementation |

Registered evidence remained evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, size 1,887,733 bytes, MIME `application/pdf`, five pages, manifest SHA-256 `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34`.

The existing corrected preparation remained immutable:

| Field | Exact value |
|---|---|
| Preparation identity | `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f` |
| Record SHA-256 | `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9` |
| Pages/regions | 5 / 5 |
| Bounds | `[0,0,2479,3508)` each |
| Deterministic order | `[1,2,3,4,5]` |
| Profile | `full-page-achromatic-png-preparation-v1`, version 1 |

No preparation endpoint was invoked. No second preparation was created and no region was reordered.

## Source PDF and authoritative RGB page-order proof

Hashes and embedded provenance, not filenames, establish the true bindings:

| Source PDF page | Authoritative representation ID | Embedded `source_page` | RGB representation SHA-256 | Production transport SHA-256 |
|---:|---|---:|---|---|
| 3 | `e65b472cd7fd30d22a470ad6c1fbb22122754443cc1585ba915ecf9e546eeecc` | 3 | `3de037da9dc9b89fe9007db53b9dda8bf7aaf458791757246f874a79ff384818` | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` |
| 4 | `8e6751ee97d3c1d66983aef8c2c72c8735714d148db0cde4b54b54b9467e09a8` | 4 | `95e8b475bd095b7190c7e7f56cbd404af9bc30b12dfb80d6e32fdbe82d4d7ace` | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` |

Both authoritative metadata records bind the same exact registered evidence/source digest, declare five PDF pages and retain dimensions 2479×3508. Production preparation page 3 references `e65b472c…`; page 4 references `8e6751ee…`. Thus the PDF, authoritative representations and production transport all agree.

## Historical R5I/R5J binding analysis

The immutable R5J files prove the transposition:

| Historical review label | RGB SHA-256 actually placed there | True source page | Grayscale SHA-256 actually placed there | Correct binding |
|---|---|---:|---|---|
| `page-3` | `95e8b475bd095b7190c7e7f56cbd404af9bc30b12dfb80d6e32fdbe82d4d7ace` | 4 | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` | page 4 |
| `page-4` | `3de037da9dc9b89fe9007db53b9dda8bf7aaf458791757246f874a79ff384818` | 3 | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` | page 3 |

The R5I local harness hard-coded representation IDs in the sequence `… 8e6751ee…, e65b472c… …`, then created review provenance using list index plus one. It therefore assigned true source page 4 to review page 3 and true source page 3 to review page 4. No PDF, authoritative representation or production region was transposed.

Historical packages remain byte-stable:

| Package | Files | Aggregate sorted-file inventory SHA-256 | Manifest SHA-256 |
|---|---:|---|---|
| `/home/steve/parker-owner-review/oi11r5i/` | 11 | `246d40f6f3250e0e7c43b24174efd648926aac911c256d81c02ee6d018745bb4` | `cf7f14b384a87d37175557785d9f5d1502a24926862e357d5d2bb6c61e8e6221` |
| `/home/steve/parker-owner-review/oi11r5j/` | 12 | `a71795b76e367103cbe0429532cfd69801127b3137e454eb14398ff042f34d54` | `25551159e55e948e1b0c6f36742397ce077f30daba4dc2be0bd70735e023b29e` |

## Corrected transport baseline

Only the page-to-review binding is corrected; no page content or transport bytes changed.

| Page | Correct transport SHA-256 |
|---:|---|
| 1 | `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c` |
| 2 | `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae` |
| 3 | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` |
| 4 | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` |
| 5 | `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816` |

## Corrected owner-review package

The review-only package is `/home/steve/parker-owner-review/oi11r5r/`. It contains only:

| File | SHA-256 |
|---|---|
| `page-3-authoritative-rgb.png` | `3de037da9dc9b89fe9007db53b9dda8bf7aaf458791757246f874a79ff384818` |
| `page-3-achromatic-transport.png` | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` |
| `page-4-authoritative-rgb.png` | `95e8b475bd095b7190c7e7f56cbd404af9bc30b12dfb80d6e32fdbe82d4d7ace` |
| `page-4-achromatic-transport.png` | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` |
| `corrected-review-manifest.json` | `be304dc0cd978bf8ffa55d58b77d89f55aa60c1158f467e47d2882ce82163b99` |

The package's aggregate sorted-file inventory SHA-256 is `b28f8087c1ab413a0d6efd277dbd96c92508feb8cbf02a6ca2e5804245362c63`. Files are mode 0664, owner/group `steve:steve`.

The manifest format is `parker.owner-review-page-binding-reconciliation.v1`. It binds evidence/source, true source page, authoritative representation ID and RGB/pixel digest, production transport digest, preparation/region identities, full-page bounds, profile/version, deterministic order, historical erroneous label and corrected binding. It explicitly sets `reviewOnly=true`, `ownerReviewStatus=PENDING`, `providerAuthority=false`, and `executionAuthority=false`.

This package is review material outside authoritative evidence and production stores. It neither changes evidential authority nor grants execution authority.

## Governed-store and provider accounting

All counts and complete aggregate inventory digests were identical before and after review staging:

| Store | Before | After | Delta |
|---|---:|---:|---:|
| evidence | 29 | 29 | 0 |
| manifests | 29 | 29 | 0 |
| capability acceptance | 11 | 11 | 0 |
| execution authorizations | 11 | 11 | 0 |
| attempts | 8 | 8 | 0 |
| provider state | 6 | 6 | 0 |
| derivative generations | 22 | 22 | 0 |
| derivative content | 20 | 20 | 0 |
| corrected-preparation files | 6 | 6 | 0 |

The corrected-preparation aggregate inventory digest remained `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d`.

OpenAI calls: 0. Claude calls: 0. Other external provider calls: 0. Retries: 0. External provider/evidence egress: 0. Provider-state delta: 0.

## Production stability and owner-review stop

Production remained the exact same container/image/source/start time, restart count zero, readiness PASS, V8 evaluator `ACCEPTED`. No deployment or restart occurred.

The corrected binding is technically reconciled, but Codex does not infer human acceptance. Steven must inspect the true page-3 and page-4 RGB/grayscale pairs in `/home/steve/parker-owner-review/oi11r5r/` and separately accept the corrected page bindings before any later request-equivalence or execution decision.

UNIT ORDINARY-INGESTION-11R5R COMPLETE — THE OI11R5M-R1 TRANSPORT-EQUIVALENCE FAILURE HAS BEEN PROVEN TO RESULT FROM A HISTORICAL OWNER-REVIEW PAGE-BINDING DEFECT: THE R5I/R5J REVIEW HARNESS TRANSPOSED THE AUTHORITATIVE PAGE-3 AND PAGE-4 BINDINGS, WHILE THE REGISTERED PDF, GOVERNED PRODUCTION PREPARATION AND DETERMINISTIC V8 PAGE ORDER REMAIN CORRECT. THE HISTORICAL R5I/R5J REVIEW ARTIFACTS REMAIN IMMUTABLE. A NEW CORRECTED OWNER-REVIEW PACKAGE AND MANIFEST HAVE BEEN CREATED FOR THE TRUE PAGE-3 AND PAGE-4 BINDINGS. THE EXISTING PRODUCTION CORRECTED PREPARATION WAS NOT MODIFIED OR RECREATED. NO EXECUTION AUTHORIZATION, PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. THE CORRECTED BINDING IS TECHNICALLY RECONCILED BUT REMAINS PENDING SEPARATE OWNER VISUAL REVIEW AND ACCEPTANCE.
