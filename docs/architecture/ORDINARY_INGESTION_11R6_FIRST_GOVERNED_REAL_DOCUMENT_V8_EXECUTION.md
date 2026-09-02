# OI11R6 — First Governed Real-Document V8 Execution

## Verdict

**B — PRE-EGRESS EXECUTION GATE FAILURE**

OpenAI calls: `0`. Retries: `0`. Claude calls: `0`. Other external calls: `0`. External evidence egress: `0`.

The owner authorized one exact OpenAI V8 transcription request, but the deployed canonical authorization/execution path cannot bind or consume that exact persisted preparation and request. Parker therefore failed closed before evidence-specific authorization and before any provider call.

## Starting repository and owner authority

The host was `parker`, repository `/home/steve/parker-platform`, branch `main`. HEAD and upstream were both exactly `87f6330c56ec2026731fb720611bab5aa731f6bc`; the worktree was clean and `git diff --check` passed.

Steven authorized one OpenAI call, zero retries and no external reasoning for exact evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, persisted preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, unchanged V8 capability/digest and the exact R5S request. No broader authority was inferred.

## Production identity and readiness

The production identity gate passed:

| Field | Exact value |
|---|---|
| Container | `8b7c4b9b9f1b374de278e37d2f01c8401bc8ab809516d21135ddebf1e8065d7c` |
| OCI image/index | `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| Embedded source | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| Started | `2026-09-02T08:57:38.782928781Z` |
| Runtime state | running |
| Restart count | `0` |
| Canonical readiness | `PASS` (`overallReady=true`, empty reasons) |
| V8 evaluator | `ACCEPTED` |

The readiness diagnostic confirmed provider profile presence, structural validity, acceptance, non-staleness, credential presence and ordinary execution readiness without exposing credential material.

## Evidence, preparation and owner-review gates

The registered evidence remains exact: size 1,887,733 bytes, MIME `application/pdf`, five pages, manifest SHA-256 `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34`.

The immutable corrected-preparation record remains SHA-256 `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`. R5S canonically read back preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, profile `full-page-achromatic-png-preparation-v1` version 1, five full-page regions with bounds `[0,0,2479,3508)`, deterministic order `[1,2,3,4,5]`, and zero R5F constituents.

The corrected R5R manifest remains SHA-256 `be304dc0cd978bf8ffa55d58b77d89f55aa60c1158f467e47d2882ce82163b99`; `/home/steve/parker-owner-review/oi11r5r/owner-acceptance.json` remains SHA-256 `f55b70e073f61b329d4410489bf386938e7adf729ee0c8279cbd2e3cf183547f`. The R5S closure report remains SHA-256 `ef793102a0e9fc37f7063787a6c3b2424679086b551d41af90bbd937ae99099d` and records `5 / 5 EXACT BYTE EQUIVALENCE` for the owner-accepted transports:

1. `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c`
2. `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae`
3. `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b`
4. `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705`
5. `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816`

No preparation was regenerated in R6.

## V8 and canonical request gates

Production retains capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, implementation `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`, provider `OpenAI`, model `gpt-5.6-sol`, reasoning `none`, storage false, maximum 32 regions, maximum body 16,777,216 bytes and no batching. Acceptance record `206d713979522d79d4094699e8eef1c15e0aded801ae8626bf7c74e7629e6f86` remains SHA-256 `a89619c47d424a67942d0ea945387126de8d0515bbd900fa6b54600aefb65382`.

The authoritative R5S reconstruction records exactly five regions in `[1,2,3,4,5]`, aggregate PNG 9,147,493 bytes, aggregate base64 12,196,664 characters, request digest `2f4f595decb924fd6d252735494dabc85b8e375c4d17e41f952195061e2675a3`, canonical body digest `5a847355cab3217a8b1309ca82dc47f2d38239395e6cdede0108fe85c53f6603`, and body size 12,202,080 bytes. The structural boundary contains only the five accepted transports, required provenance, frozen V8 transcription instructions/schema and required provider/governance metadata. It contains no unrelated evidence, memory, attachment, external reasoning instruction or R5F region.

## Exact pre-egress failure boundary

Static inspection of the exact deployed source proves two incompatible production contracts:

1. Production composition creates `OrdinaryRequestRegionV8IngestionWorkflow` with a default `OrdinaryRequestRegionV8RequestPreparer()`. Although the corrected-preparation store and preparation-only service are separately composed, neither is injected into the execution preparer.
2. `OrdinaryRequestRegionV8RequestPreparer.prepare` calls `FullPageAchromaticCanonicalRequestRegionV8Builder.build` with the custody source PDF bytes. That builder renders and prepares the PDF afresh. It does not canonically read preparation `85054…` or prove it is consuming the already-persisted bytes immediately before egress.
3. The canonical `OrdinaryRequestRegionV8OwnerAuthorization` schema binds only authorization ID, evidence ID, source SHA-256, capability ID/digest, provider, approving owner and validity timestamps. It has no fields for preparation identity, request digest, Authorization Purpose, provider profile, model, one-call maximum or zero-retry constraint.
4. The authenticated authorization endpoint accepts no bounded proposition fields and calls that schema directly. Consequently Parker cannot create the Section 15 authorization that must bind the exact preparation and request digest without changing implementation/governance first.

Calling the authorization endpoint would have created a materially broader/different record than the exact owner proposition. Calling execution afterward would have regenerated preparation, contrary to Sections 1, 7 and 12. The mismatch was discovered before authorization. No canonical mutation or provider operation was attempted.

Required remediation is a separately governed implementation unit that makes the execution preparer consume canonical persisted corrected-preparation state and extends or introduces an authorization contract binding at least the exact preparation identity and request digest, together with the remaining frozen execution limits. That implementation must follow normal test, artifact acceptance, deployment and implementation-bound capability-authority gates before R6 can be retried.

## Pre/post governed-store accounting

The complete production inventory was identical before and after R6 inspection:

| Store | Count | Aggregate inventory SHA-256 | Delta |
|---|---:|---|---:|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` | 0 |
| evidence manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` | 0 |
| corrected preparations | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` | 0 |
| capability acceptances | 11 | `7965a69748fe8d5009d631ea51372a33e60396d440e4d679693a38caf3ff0910` | 0 |
| execution authorizations | 11 | `f69eb1cfa4d4ff6438a55e4aa12beeb68a55cd4d5ece17c9fb4b09bb86f24939` | 0 |
| attempts | 8 | `842a2457b71df56d8265b1419a37ca2f4e986c483267afee735f0bf344d62e74` | 0 |
| provider state | 6 | `980472f44ba44324881755167e7546bfa1d5bb0589db0736c10ac5fcb65cbc5d` | 0 |
| derivative generations | 22 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` | 0 |
| derivative content | 20 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` | 0 |
| document-ingestion audit | 1 | `9349a5cd71c6efbefa1524115900c0769c13e464f08f4bbd5b41f3ac3c42d72e` | 0 |
| evidence audit | 1 | `1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59` | 0 |

There is no R6 authorization ID, execution ID, attempt ID, raw provider-state identity or derivative identity because execution stopped before authorization.

## Historical integrity, provider accounting and stability

Source evidence, source manifest, corrected preparation, capability records and all R5F/R5I/R5J/R5M/R5M-R1/R5R/R5S history remain immutable. Production remains the same running container/image/source with restart count zero and readiness PASS. No deployment or restart occurred.

OpenAI calls: `0`. Claude calls: `0`. Other external calls: `0`. Retries: `0`. External evidence egress: `0`. Provider-state delta: `0`.

UNIT ORDINARY-INGESTION-11R6 STOPPED BEFORE EGRESS — VERDICT B: THE EXACT OWNER-AUTHORIZED REQUEST COULD NOT BE CANONICALLY AUTHORIZED OR EXECUTED BECAUSE THE DEPLOYED V8 EXECUTION PATH REGENERATES PREPARATION FROM SOURCE INSTEAD OF CONSUMING THE IMMUTABLE PERSISTED CORRECTED PREPARATION, AND THE CANONICAL EVIDENCE AUTHORIZATION RECORD DOES NOT BIND THE REQUIRED PREPARATION IDENTITY OR REQUEST DIGEST. NO EVIDENCE-SPECIFIC AUTHORIZATION, EXECUTION, ATTEMPT OR PROVIDER STATE WAS CREATED. OPENAI CALLS: 0. RETRIES: 0. EXTERNAL EGRESS: 0. PRODUCTION AND ALL GOVERNED STORES REMAIN UNCHANGED. A SEPARATELY GOVERNED IMPLEMENTATION AND CONVERGENCE UNIT IS REQUIRED BEFORE THIS FIRST REAL-DOCUMENT EXECUTION MAY BE RETRIED.
