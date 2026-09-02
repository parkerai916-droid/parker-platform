# OI11R5S — Corrected Owner-Reviewed Transport Equivalence and Preparation Closure

## Verdict

**A — REAL-DOCUMENT PREPARATION CLOSED; READY FOR SEPARATE OWNER EXECUTION DECISION**

The immutable governed production preparation is exactly equivalent, page by page, to the corrected OI11R5R baseline explicitly reviewed and accepted by Steven. Canonical readback and local request reconstruction were read-only. No preparation, execution authorization, provider operation or governed-store mutation occurred.

## Starting repository state

The host was `parker`, repository `/home/steve/parker-platform`, branch `main`. Starting HEAD and upstream were both `ed59ce5c0033b7e59d4b6169e01c85769c49c6fe`; the worktree was clean and the initial `git diff --check` passed. The acceptance file was recorded outside Git and did not change repository identity.

## R5R reconciliation and owner acceptance

OI11R5R proved that only the historical R5I/R5J review labels for pages 3 and 4 were transposed. Source PDF order, authoritative page provenance, production preparation order and transport generation were correct. Its report remains SHA-256 `0d4c1f9141dacefa06539c4481520ef9b16a3e31ad97f0c2783edfd3e8d547b7`.

The corrected review manifest at `/home/steve/parker-owner-review/oi11r5r/corrected-review-manifest.json` is SHA-256 `be304dc0cd978bf8ffa55d58b77d89f55aa60c1158f467e47d2882ce82163b99`. The owner-acceptance record at `/home/steve/parker-owner-review/oi11r5r/owner-acceptance.json` is SHA-256 `f55b70e073f61b329d4410489bf386938e7adf729ee0c8279cbd2e3cf183547f`.

The acceptance binds exact evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source PDF SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, corrected manifest, five-page order `[1,2,3,4,5]`, corrected page-3/page-4 bindings and explicit visual acceptance of both corrected RGB/achromatic pairs. It expressly excludes execution authorization, provider execution, transmission, retry, external reasoning and transcription. R5S does not broaden that authority.

## Production identity and stability

Production remained exact throughout:

| Field | Exact value |
|---|---|
| Container | `8b7c4b9b9f1b374de278e37d2f01c8401bc8ab809516d21135ddebf1e8065d7c` |
| OCI image/index | `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| Embedded source | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| Restart count | `0` |
| Readiness | `PASS` |
| V8 evaluator | `ACCEPTED` |

No deployment or restart occurred.

## Canonical preparation readback

The existing create-once record `/data/corrected-preparations/records/85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f.json` remained SHA-256 `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`, size 12,202,837 bytes. Its five content-addressed transport files and record were copied read-only to bounded temporary validation storage. The accepted artifact JAR, SHA-256 `b398a59855303d5d47b0c2154d2b6ddb31c8784f97b86b6991f08723b929831f`, then read that snapshot through `FileSystemFullPageAchromaticPreparationStore.read`, exercising the canonical codec and its exact transport-byte readback checks. No production write operation or preparation endpoint was invoked.

Canonical readback established:

| Property | Result |
|---|---|
| Preparation identity | `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f` |
| Region-set digest | `bc684a53cb20425580c80664658df3bd6d0515adcefcbe10e827faff87596e56` |
| Profile/version | `full-page-achromatic-png-preparation-v1` / `1` |
| Pages / regions | `5 / 5` |
| Bounds | `[0,0,2479,3508)` on every page |
| Page order | `[1,2,3,4,5]` |
| Order state | `DETERMINISTIC_SOURCE_ORDER` on every page |
| R5F constituents | `0` |
| Chromatic-risk gate | previously passed by the immutable admitted preparation |

## Corrected five-page equivalence

Every canonical persisted transport digest equals the corrected owner-accepted digest for the same true source page:

| Page | Authoritative representation | Preparation region | Transport SHA-256 | Result |
|---:|---|---|---|---|
| 1 | `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e` | `d9e87671239b8d7fab1d3c0b6790c250d5f7579b3aec07309fba0caf142ee1c2` | `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c` | MATCH |
| 2 | `669f1af75d9cdd4768305258e4f73de441a5d71342e7e043ed7d7b8276568c39` | `196be4bff8a14b891e1fce7d361d60354502a6bac4aea251dd860d2bf8540040` | `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae` | MATCH |
| 3 | `e65b472cd7fd30d22a470ad6c1fbb22122754443cc1585ba915ecf9e546eeecc` | `56091794e46942f972316ae5a47170ec5efcc4f5447beba58a2c58b27b753fa3` | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` | MATCH |
| 4 | `8e6751ee97d3c1d66983aef8c2c72c8735714d148db0cde4b54b54b9467e09a8` | `c1bc9c7a94cb8882314d506581d942b51e2e9045e971f33a6b4f98668e4c90ea` | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` | MATCH |
| 5 | `eb7ea4a7c78af09554f52ad63fcfe7b122b9bb5b23563a488b269fdd9bf23c44` | `ef79c2e21276979b69d2221301812b65fcfccb8186880906eb5163a991b7f9ba` | `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816` | MATCH |

Result: `5 / 5 EXACT BYTE EQUIVALENCE` and `OWNER_REVIEWED_TRANSPORT_EQUIVALENCE = EXACT`.

## Canonical V8 request reconstruction

The request was reconstructed locally from only the decoded persisted preparation, using the accepted request-region identity algorithm, `RequestRegionV8Request` and `OpenAiRequestRegionV8Codec`. The unchanged deterministic correlation identity from R5M-R1 was retained. No body was transmitted or printed.

| Property | Exact result |
|---|---|
| Regions | `5` |
| Order | `[1,2,3,4,5]` |
| Request-region IDs | `e70de3115d04d3cff1112fe3861dec80618a222605c3be2486d7af8aabf67cff`, `21edabae738a985289c87e2853af52dd5509529cded27b227ab40a64470bc274`, `174d91c745be80ab6cc9f133bba5a3657c56e0d73fe23b490776b079f52512af`, `1d71177d4cb911421ba5854820e306545a2810609c980daa3ce4f18dd283626d`, `b4e89df3a5c84f3b18e6f95e4d14861969b9cdc41f23c186c34cf0f2ef998df9` |
| Aggregate PNG bytes | `9,147,493` |
| Aggregate base64 characters | `12,196,664` |
| Request digest | `2f4f595decb924fd6d252735494dabc85b8e375c4d17e41f952195061e2675a3` |
| Canonical body digest | `5a847355cab3217a8b1309ca82dc47f2d38239395e6cdede0108fe85c53f6603` |
| Canonical body size | `12,202,080 bytes` |

These values exactly reproduce R5M-R1. Correcting human-review labels required no preparation or request mutation. The body passes the 16,000,000-byte governed acceptance threshold and 16,777,216-byte absolute V8 limit; aggregate PNG and base64 values also remain within 10,875,000 bytes and 14,500,000 characters respectively. Hidden batching is zero; the request represents one intended future provider call, while actual R5S provider calls are zero.

Structural inspection of canonical construction verified only the five Deed transport images in PDF order, exact registered-evidence and corrected-preparation provenance, frozen V8 schema/instructions and required governance/provider metadata. It contained no unrelated evidence, memory, attachment, external-reasoning material or R5F diagnostic region.

## Historical preservation

The historical R5F page-1 representation `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e` and region-set digest `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f` remain diagnostic, unordered and unmodified. Historical R5I and R5J packages remain at aggregate inventory digests `246d40f6f3250e0e7c43b24174efd648926aac911c256d81c02ee6d018745bb4` and `a71795b76e367103cbe0429532cfd69801127b3137e454eb14398ff042f34d54` respectively.

The original R5M failure report remains SHA-256 `52cc79d2bda9286547cf2d29a6e1740e226a0ee6e5d0347df8612cfae63c16a4`; the R5M-R1 verdict-B report remains `6fc425800993db54079c5bc589606dc7307afbbc92f3775fd26100a913e7306c`; and the R5R technical reconciliation, corrected manifest and owner acceptance retain the hashes recorded above. The corrected baseline is additive governance history and does not rewrite any earlier artifact.

## Governed-store accounting

Complete file counts and sorted-file inventory digests were identical before and after all R5S verification:

| Store | Count | Aggregate inventory SHA-256 | Delta |
|---|---:|---|---:|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` | 0 |
| evidence source manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` | 0 |
| capability acceptances | 11 | `7965a69748fe8d5009d631ea51372a33e60396d440e4d679693a38caf3ff0910` | 0 |
| execution authorizations | 11 | `f69eb1cfa4d4ff6438a55e4aa12beeb68a55cd4d5ece17c9fb4b09bb86f24939` | 0 |
| attempts | 8 | `842a2457b71df56d8265b1419a37ca2f4e986c483267afee735f0bf344d62e74` | 0 |
| provider state | 6 | `980472f44ba44324881755167e7546bfa1d5bb0589db0736c10ac5fcb65cbc5d` | 0 |
| derivative generations | 22 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` | 0 |
| derivative content | 20 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` | 0 |
| corrected-preparation files | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` | 0 |

No evidence-specific execution authorization, execution ID, attempt, provider state, derivative or transcription was created.

## Provider and egress accounting

OpenAI calls: `0`. Claude calls: `0`. Other external provider calls: `0`. Retries: `0`. External provider/evidence egress: `0`. Provider-state delta: `0`.

## Preparation closure

The closure state is:

* `FIRST_REAL_DOCUMENT_PREPARATION = COMPLETE`
* `OWNER_REVIEWED_TRANSPORT_EQUIVALENCE = EXACT`
* `PROVIDER_EXECUTION_AUTHORITY = NOT GRANTED`

This closes preparation only. The document remains fail-closed pending a separate explicit owner decision on first governed real-document provider execution.

UNIT ORDINARY-INGESTION-11R5S COMPLETE — THE EXISTING GOVERNED PRODUCTION PREPARATION FOR THE REGISTERED FIVE-PAGE DEED HAS BEEN READ BACK WITHOUT MUTATION AND VERIFIED AGAINST THE OWNER-ACCEPTED CORRECTED OI11R5R TRANSPORT BASELINE. ALL FIVE SOURCE-PAGE TRANSPORT REPRESENTATIONS MATCH EXACTLY IN TRUE PDF PAGE ORDER. OWNER_REVIEWED_TRANSPORT_EQUIVALENCE IS EXACT AND FIRST_REAL_DOCUMENT_PREPARATION IS COMPLETE. THE CANONICAL V8 REQUEST HAS BEEN RECONSTRUCTED LOCALLY AND VERIFIED FOR IDENTITY, PROVENANCE, CARDINALITY, ORDER AND SIZE. HISTORICAL R5F, R5I, R5J, R5M, R5M-R1 AND R5R STATE REMAINS IMMUTABLE. NO GOVERNED PRODUCTION DATA WAS MUTATED. NO EXECUTION AUTHORIZATION, PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. THE DOCUMENT IS PREPARED AND VERIFIED BUT REMAINS FAIL-CLOSED PENDING A SEPARATE EXPLICIT OWNER DECISION ON FIRST GOVERNED REAL-DOCUMENT PROVIDER EXECUTION.
