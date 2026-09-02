# OI11R6-R1 — First Governed Real-Document V8 Execution Retry

Date: 2026-09-02 UTC

## Verdict

**C — POST-EGRESS EXECUTION FAILURE**

All pre-egress gates passed and Parker made exactly one authorized OpenAI call with zero retries. OpenAI returned HTTP 200. Parker durably persisted the raw response before parsing, enriched authoritative response provenance, and validated all five V8 regions in Parker page order. Derivative admission did not occur: construction of the version-3 derivative failed because the execution payload supplied the capability processing profile `request-region-fidelity-acquisition-v4` as `providerProfile`, while the version-3 derivative contract correctly requires the authorization-bound provider profile `openai-fidelity-first-transcription-v1`.

The raw provider state, successful V8 assessment, authorization, reservation and attempt ledger remain durably preserved. No invalid derivative was admitted. The single-call budget is consumed. **NO RETRY IS AUTHORIZED.**

## Starting repository and production state

Repository `/home/steve/parker-platform` was on branch `main`. HEAD and upstream were both `55174979be992ee8562044f7f5aa7678dde24fd1`; the worktree was clean.

Production passed the exact identity gate:

| Field | Exact value |
|---|---|
| Container | `9011f1e9e9feaa5d0662c295cc295c19b0dce45fb4e8c2a1dad95fbe12f52fa5` |
| Image/index | `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406` |
| Embedded source | `39fe0e777608c96cba20cec491113e77eee4b8ef` |
| Runtime JAR SHA-256 | `d0f52b7d935f568c14d9c7012be43ae7f1ac8d755f522fa03ee34ffe9af530e7` |
| Started | `2026-09-02T11:25:47.196217995Z` |
| Restart count | `0` |
| Readiness | `PASS` |

## Owner execution authorization boundary

Steven authorized exactly one OpenAI transcription call for the exact registered evidence and persisted preparation, using `openai-fidelity-first-transcription-v1`, model `gpt-5.6-sol`, Purpose `evidence-intelligence.external-transcription`, maximum calls 1, automatic retries 0, and no external reasoning. Claude, other providers, a second call, retries, unrelated evidence, alternate preparation, analysis, summarisation and legal reasoning remained excluded.

## Evidence identity and custody

Canonical custody readback returned:

* evidence: `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`;
* source SHA-256: `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`;
* byte length: `1,887,733`;
* MIME: `application/pdf`;
* page count: `5`.

The governed source bytes independently rehashed to the exact source SHA-256. Neither source nor manifest changed.

## Persisted corrected preparation

The execution path consumed canonical persisted preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`; it did not regenerate from the PDF. The canonical record SHA-256 was `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`. It contained five full-page regions, profile `full-page-achromatic-png-preparation-v1` version 1, deterministic page order `[1,2,3,4,5]`, and region-set digest `bc684a53cb20425580c80664658df3bd6d0515adcefcbe10e827faff87596e56`.

The persisted transport bytes matched the corrected owner-reviewed baseline exactly:

| Page | Transport SHA-256 |
|---:|---|
| 1 | `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c` |
| 2 | `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae` |
| 3 | `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b` |
| 4 | `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705` |
| 5 | `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816` |

No R5F geometry was used and no preparation record or transport file changed.

## Corrected owner-review and R5S closure

The corrected R5R manifest rehashed to `be304dc0cd978bf8ffa55d58b77d89f55aa60c1158f467e47d2882ce82163b99`. Owner acceptance `/home/steve/parker-owner-review/oi11r5r/owner-acceptance.json` rehashed to `f55b70e073f61b329d4410489bf386938e7adf729ee0c8279cbd2e3cf183547f`. It binds the exact evidence, preparation, page order, corrected Page-3/Page-4 bindings and all five transport hashes.

OI11R5S remained authoritative with `5/5 EXACT`, body size `12,202,080 bytes`, zero store delta and zero provider activity at preparation closure.

## V8 capability authority

The canonical evaluator returned `ACCEPTED` before evidence authorization for:

* capability `ordinary-external-request-region-transcription-v8`;
* digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`;
* implementation `39fe0e777608c96cba20cec491113e77eee4b8ef`.

The exact implementation-bound acceptance was record `bc8463b676b3121150af2bbaf86d6aa99feb6eceb24ba60592cf526c00a12d43`, file SHA-256 `c9ecbf23232868275003d73ce71190f4acb4f3eabc1bfa76de4160d5e8b58824`.

## Provider and canonical request gates

The non-executing deployed acquisition projection reconstructed from the persisted preparation and returned `READY`, `NOT_AUTHORISED`, five-page scanned PDF custody, provider OpenAI, model `gpt-5.6-sol`, no existing executable authorization and execution state `NOT_STARTED`.

The exact canonical request identity was:

| Field | Exact value |
|---|---|
| Request regions | `5` |
| Parker order | `[1,2,3,4,5]` |
| Request digest | `2f4f595decb924fd6d252735494dabc85b8e375c4d17e41f952195061e2675a3` |
| Provider body digest | `5a847355cab3217a8b1309ca82dc47f2d38239395e6cdede0108fe85c53f6603` |
| Canonical body size | `12,202,080 bytes` |
| Correlation ID | `preparation-5ed9cc0664ee153d35b2bba5ff2ffad8400f3d1dd6c24ada1961d3d689b4309c` |

These values exactly matched R5S/R5M-R1. The authorization later persisted the same request/body identities, proving request immutability. The request carried only the five accepted transports, required transcription schema/instructions and governed metadata; reasoning was `none` and provider storage was `false`.

## Pre-execution governed-store baseline

| Store | Count | Aggregate digest |
|---|---:|---|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| evidence-source manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| corrected preparations | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` |
| capability acceptances | 12 | `0089970e3bf20a29b845aa0a2820722e47bead49864831d9c0d7873545c18237` |
| execution-authorizations | 11 | `f69eb1cfa4d4ff6438a55e4aa12beeb68a55cd4d5ece17c9fb4b09bb86f24939` |
| attempts | 8 | `842a2457b71df56d8265b1419a37ca2f4e986c483267afee735f0bf344d62e74` |
| provider state | 6 | `980472f44ba44324881755167e7546bfa1d5bb0589db0736c10ac5fcb65cbc5d` |
| derivative generations | 22 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` |
| derivative content | 20 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` |
| evidence audit | 1 | `1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59` |
| document-ingestion audit | 1 | `9349a5cd71c6efbefa1524115900c0769c13e464f08f4bbd5b41f3ac3c42d72e` |

## Fresh exact-envelope authorization

The canonical owner route created fresh version-2 authorization:

* authorization ID/digest: `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97`;
* file SHA-256: `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544`;
* approved at: `2026-09-02T11:43:34.754688654Z`;
* expires at: `2026-09-03T11:43:34.754688654Z`.

Canonical readback bound exact evidence/source, preparation/profile/version, request/body digests, correlation, capability/digest, Purpose `evidence-intelligence.external-transcription`, provider `OpenAI`, provider profile `openai-fidelity-first-transcription-v1`, model `gpt-5.6-sol`, maximum calls 1, retry limit 0 and external reasoning false. Before invocation its state was `NOT_STARTED`, attempt count remained 8 and provider-state count remained 6.

At execution the authorization was durably reserved to execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`. The requested UI attempt identity was `ordinary-attempt-3c2bf685-d6c2-44e0-acf8-0224d92fd976`; the durable fidelity identity uses correlation/attempt identity `preparation-5ed9cc0664ee153d35b2bba5ff2ffad8400f3d1dd6c24ada1961d3d689b4309c`.

## Single provider call and raw-before-parse state

Parker invoked the canonical execution endpoint exactly once. Durable stages were:

1. `AUTHORISED` at `2026-09-02T11:44:08.183941736Z`;
2. `PREFLIGHT_PASSED` at `2026-09-02T11:44:08.218729883Z`;
3. `SOURCE_RETRIEVED` at `2026-09-02T11:44:08.243852657Z`;
4. `REQUEST_PREPARED` at `2026-09-02T11:44:08.268883923Z`;
5. `PROVIDER_ATTEMPT_STARTED` at `2026-09-02T11:44:08.294185575Z`;
6. `PROVIDER_RESPONSE_RECEIVED` at `2026-09-02T11:44:35.775223137Z`.

The exact attempt-ledger file SHA-256 is `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591`.

Provider result:

* OpenAI HTTP status: `200`;
* outer response status/object: `completed` / `response`;
* outer response ID: `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`;
* outer model: `gpt-5.6-sol`;
* provider-state record: `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`;
* provider-state file SHA-256: `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c`;
* raw response length: `18,331 bytes`;
* raw response SHA-256: `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`;
* manifest digest: `d46d3ca4b09e3206ed55fd0ca91193650cfd8dad70db97f59383de1f42865d51`.

The durable provider-state record contains the raw bytes and request/manifest bindings. Only after it existed did Parker parse and create assessment `2b1fbe06...request-region-v8-assessment`, file SHA-256 `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01`. This is raw-before-parse evidence.

## V8 validation, authoritative provenance and ordering

The persisted assessment records outcome `SUCCESS`, raw SHA-256 `4706c24b...`, structured SHA-256 `824fa82a10887e8a877231e9e4995e642e130aa67969338cde7b1eeed052a404`, and exactly five blocks.

* request cardinality: `5`;
* response cardinality: `5`;
* page mapping: `[1,2,3,4,5]`;
* Parker order: `[1,2,3,4,5]`;
* missing/duplicate/unexpected regions: `0`;
* authoritative provider response ID: exact outer ID above;
* authoritative provider model: exact outer model above;
* provider provenance enrichment: `PASS`;
* provider-returned ordinals: retained forensics only.

No transcription content is reproduced in this report.

## Post-egress derivative failure

After successful V8 validation, `OrdinaryRequestRegionV8IngestionWorkflow.payload` constructed the version-3 derivative. It supplied `capability.profile`, whose value is processing/transcription profile `request-region-fidelity-acquisition-v4`, to the derivative's `providerProfile` field. `OrdinaryRegionTranscriptionDerivative` line 57 requires the exact authorization provider profile `openai-fidelity-first-transcription-v1` for representation version 3. The constructor therefore failed closed with `IllegalArgumentException`.

This occurred after raw persistence and V8 validation but before derivative admission. Derivative generation/content counts remained unchanged; no incomplete or invalid derivative exists. The owner HTTP route truthfully returned HTTP 500. The failure is a post-egress R6-path binding defect, not a provider, evidence, preparation, request, response-cardinality or V8-validation failure.

## Post-execution store accounting

| Store | Before | After | Delta | Final aggregate digest |
|---|---:|---:|---:|---|
| evidence | 29 | 29 | 0 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| evidence-source manifests | 29 | 29 | 0 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| corrected preparations | 6 | 6 | 0 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` |
| capability acceptances | 12 | 12 | 0 | `0089970e3bf20a29b845aa0a2820722e47bead49864831d9c0d7873545c18237` |
| execution-authorizations | 11 | 14 | +3 files | `b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6` |
| attempts | 8 | 10 | +2 files | `798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f` |
| provider state | 6 | 8 | +2 files | `be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6` |
| derivative generations | 22 | 22 | 0 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` |
| derivative content | 20 | 20 | 0 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` |
| evidence audit | 1 | 1 | 0 | `1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59` |
| document-ingestion audit | 1 | 1 | 0 | `9349a5cd71c6efbefa1524115900c0769c13e464f08f4bbd5b41f3ac3c42d72e` |

The authorization delta comprises the v2 grant, reservation-events file and lock. The attempt delta comprises the durable ledger and lock. The provider-state delta comprises raw state and assessment. These mutations reconcile exactly with the one authorized execution. Source, preparation, capability and derivative stores are byte-identical.

## Provider accounting and production stability

* OpenAI calls: **`1`**;
* automatic retries: **`0`**;
* manual retries: **`0`**;
* Claude calls: **`0`**;
* other external provider calls: **`0`**;
* external reasoning calls: **`0`**.

Production remained on container `9011f1e9...`, image/index `sha256:73c2fda4...`, source `39fe0e77...`, restart count zero, running state and readiness PASS. V8 capability authority remained `ACCEPTED`. No deployment or restart occurred.

The one authorized call is consumed. Parker must not retry this execution or reuse the reserved authorization. Any remediation requires a new bounded implementation/test/artifact/deployment/implementation-bound-acceptance sequence and a later separate owner execution decision.
