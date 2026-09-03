# OI11R6N — Governed Zero-Egress Continuation Retry of Preserved R6-R1 Result and First Real-Document Derivative Admission

## Verdict

**C — POST-ADMISSION VERIFICATION FAILURE**

The canonical zero-egress continuation admitted exactly one derivative and explicitly reported `providerInvoked=false`. The immediately required canonical HTTP readback then failed with HTTP 500 because the owner runtime adapter rejects ordinary request-region transcription through the historical Tier A presentation path. The admitted records remain preserved and were not deleted or rewritten. No continuation retry or provider action was attempted.

## Starting repository state and owner authorization

Branch was `main`; HEAD and upstream were both `a693bcec3cb9798569ba4e869aa7bde65bb16cdc`; the worktree was clean.

Steven authorized one governed zero-egress continuation of preserved execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`, using only its canonical persisted authorization, attempt, consumed budget, provider state and evidence/preparation bindings. The authority permitted exactly one create-once derivative admission after all eligibility gates passed. It did not authorize a provider call, retry, new authorization, execution, attempt or provider-state record.

## Production identity

Production was and remains:

| Field | Exact value |
|---|---|
| Container | `aae0bf09790510cdb6d2e47a7dfeb25e79bc7f4b86236e7e3121fb5fa66f3149` |
| OCI image/index | `sha256:adbb96afdb732a4549661fef08773d1b70a471e5311c804392f5fba26ce1ea4e` |
| Platform manifest | `sha256:374787378c5096c65231c959754d2ff9c6e694fc03c6dff33741a620f37081ba` |
| OCI config | `sha256:fd2e5764f059980e946da8d92b15e0598cae964bdfd6489b7e8888fd855ecd4a` |
| Embedded source | `ca15222c9f5edea28e68bbb0099734578fc30c4a` |
| Runtime JAR SHA-256 | `ce5191a5a04de91c9697acb38043cba6ddfa0c11bfb20f16babeb216804d7137` |
| Restart count/readiness | `0` / `PASS` |

## R6M implementation-bound V8 acceptance

Exactly one applicable record was found:

* record ID `bb67c84688f34d76fc6d2f86946ebd1d1f38c14f4f9bbe91259d0e458715ce4d`;
* path `/data/region-transcription-capability-acceptances/bb67c84688f34d76fc6d2f86946ebd1d1f38c14f4f9bbe91259d0e458715ce4d.request-region-v8-capability-acceptance-v1`;
* file SHA-256 `ee39f493cbcde459f3a2ca7ef4368df848122b0d3738c389d69bf6e0c66d884c`;
* state accepted;
* capability `ordinary-external-request-region-transcription-v8`;
* digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`;
* implementation `ca15222c9f5edea28e68bbb0099734578fc30c4a`.

The canonical evaluator returned `ACCEPTED` for that exact runtime implementation.

## Historical/current implementation separation

The immutable historical R6-R1 attempt identifies implementation `39fe0e777608c96cba20cec491113e77eee4b8ef`. Current continuation authority identifies implementation `ca15222c9f5edea28e68bbb0099734578fc30c4a`. Canonical continuation eligibility accepted these as distinct governed identities; `DURABLE_PROVIDER_ATTEMPT_REQUIRED` did not recur.

## Evidence, preparation and owner-reviewed baseline

Evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9` remained exact: source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, manifest SHA-256 `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34`, MIME `application/pdf`, size `1,887,733 bytes`, five pages.

Canonical persisted preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f` was read without mutation. It retained profile `full-page-achromatic-png-preparation-v1`, version 1, five regions and page order `[1,2,3,4,5]`. Record SHA-256 was `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`.

Transport SHA-256 values matched the corrected owner-reviewed baseline in order:

1. `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c`
2. `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae`
3. `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b`
4. `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705`
5. `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816`

Owner acceptance SHA-256 was `f55b70e073f61b329d4410489bf386938e7adf729ee0c8279cbd2e3cf183547f`; corrected manifest SHA-256 was `be304dc0cd978bf8ffa55d58b77d89f55aa60c1158f467e47d2882ce82163b99`.

## Preserved authorization, execution, attempt and provider state

Authorization `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97` canonically bound the exact evidence, source, preparation, request, capability/digest, Purpose `evidence-intelligence.external-transcription`, provider `OpenAI`, provider profile `openai-fidelity-first-transcription-v1`, model `gpt-5.6-sol`, maximum one call and retry limit zero.

Execution was `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`. Its immutable attempt ledger identified historical implementation `39fe0e77...`, request `2f4f595decb924fd6d252735494dabc85b8e375c4d17e41f952195061e2675a3`, attempt `preparation-5ed9cc0664ee153d35b2bba5ff2ffad8400f3d1dd6c24ada1961d3d689b4309c`, and stages through `PROVIDER_RESPONSE_RECEIVED`.

Provider state remained `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`, raw response SHA-256 `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`, response ID `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`, model `gpt-5.6-sol`, historical HTTP status 200, raw-before-parse preserved, and V8 assessment `SUCCESS` 5/5 in Parker order `[1,2,3,4,5]`.

The call budget before continuation was maximum 1, consumed 1, retry 0. No derivative or conflict existed for the execution.

## Pre-continuation store snapshot

| Store | Count | Aggregate digest |
|---|---:|---|
| Evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| Evidence manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| Corrected preparations | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` |
| Capability acceptances | 14 | `14eeeb9ceac5361c394c58aeae66ee9b3d6b908717ce06e416c113e0aa97b950` |
| Execution authorizations | 14 | `b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6` |
| Attempt files | 10 | `798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f` |
| Provider state | 8 | `be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6` |
| Derivative generations | 22 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` |
| Derivative content | 20 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` |
| Evidence audit | 1 | `1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59` |
| Document-ingestion audit | 1 | `9349a5cd71c6efbefa1524115900c0769c13e464f08f4bbd5b41f3ac3c42d72e` |

## Canonical continuation, eligibility and replay

The authenticated canonical `POST /owner/admin/region-transcription-continuation` route was invoked exactly once with only the exact evidence, authorization, execution and provider-state identities. It returned HTTP 200:

```text
status=ADMITTED
detail=admitted
derivativeGenerationId=region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6
providerInvoked=false
```

This proves current capability authority, authorization/execution relationship, historical durable attempt, exhausted call budget, provider-state binding, evidence/preparation/request/capability/Purpose/provider/profile/model bindings, and pre-admission create-once eligibility passed. The historical-identity defect reported by R6I did not recur.

Persisted raw-state replay completed without provider access, retained authoritative response ID/model, V8 5/5, and Parker order `[1,2,3,4,5]`. The admitted generation metadata identifies provider profile `openai-fidelity-first-transcription-v1`, adapter `openai-responses-request-region-transcription-adapter` version `7.0.0`, and model `gpt-5.6-sol`; acquisition and preparation profiles remained separate.

## Derivative admission identities

Exactly one generation and one content file were created:

| Field | Exact value |
|---|---|
| Generation ID/content ID | `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6` |
| Generation path | `/data/derivative-generations/region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6.derivative` |
| Generation size/SHA-256 | `626 bytes` / `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14` |
| Content path | `/data/derivative-content/region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6.content` |
| Content size/SHA-256 | `12,254 bytes` / `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb` |
| Generation codec/version | `PDGR` / `1` |
| Admission timestamp | `2026-09-03T03:39:50.820228571Z` |
| Audit stage timestamp | `ADMITTED` / `2026-09-03T03:39:50.846610491Z` |

No raw transcription content is reproduced here.

## Canonical post-admission readback failure

Immediately after admission, authenticated canonical GET
`/owner/evidence/evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9/content/region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`
returned HTTP 500.

The bounded runtime diagnostic was:

```text
java.lang.IllegalStateException:
Ordinary region transcription uses its governed owner-result projection,
not historical Tier A presentation
```

The exception originates at `OwnerUiEvidenceRuntimeAdapter.toOwnerContent` and propagates through `retrieveTierAExtractedContent` and `OwnerEvidenceHttpServer.handleRetrieveContent`. Thus persistence succeeded, but the only exposed canonical content route selected the historical Tier A presentation adapter, which intentionally rejects this ordinary request-region derivative kind. The required canonical readback gate did not pass. The 26-byte error response had SHA-256 `3e44db91f017b569d3e65eacc818326c8adb28750d716764bc303bcf3b7463fd`.

No alternative direct-store presentation, ad hoc decoder, second continuation or deletion was attempted. This is a post-admission verification failure, not grounds to rewrite the admitted state.

## Historical failure and provider-budget preservation

The R6-R1 and R6I failure histories, authorization, event record, attempt ledger, provider state and assessment remain byte-identical. Their post-operation SHA-256 values are respectively `48f4e540...`, `64070a0d...`, `bff24b6c...`, `c3f1ea29...`, and `bace0583...` as captured before continuation.

Budget remains maximum 1, consumed 1, retry limit 0. R6N created no authorization, execution, attempt or provider-state record and made no provider call.

## Governed-store accounting

| Store | Before | After | Delta |
|---|---:|---:|---:|
| Evidence | 29 | 29 | 0 |
| Evidence manifests | 29 | 29 | 0 |
| Corrected preparations | 6 | 6 | 0 |
| Capability acceptances | 14 | 14 | 0 |
| Execution authorizations | 14 | 14 | 0 |
| Attempt files | 10 | 10 | 0 |
| Provider state | 8 | 8 | 0 |
| Derivative generations | 22 | 23 | **+1** |
| Derivative content | 20 | 21 | **+1** |
| Evidence audit | 1 | 1 | 0 |
| Document-ingestion audit | 1 | 1 | 0 files; one canonical append |

The document-ingestion audit aggregate digest changed from `9349a5cd71c6efbefa1524115900c0769c13e464f08f4bbd5b41f3ac3c42d72e` to `3ff333aa80da829ad3454c1d35136a2f24e0997391c1da4a5ef9cc9604429359` solely because the canonical `ADMITTED` audit entry was appended for the exact execution/generation. Derivative aggregate digests changed to `801bdfd3c4b4801ac1981cb48d90ccd1721fdd782d0d54326253de62ecb9b19f` and `80eee2bb97f2532b0a1b22e7fbfc6e59c8b8a3d15e98ad24b7969917e5f27435`. All other aggregate digests were unchanged.

## Idempotence status

Filesystem identity checks found exactly one generation and one content record for the admitted ID. Source and R6E/R6J tests establish create-once conflict handling, but the unit stopped at the mandatory post-admission verification failure. A second mutating continuation was not used to demonstrate idempotence.

## Production stability and provider accounting

Production remains on the same image, source and JAR with restart count zero and readiness PASS. There was no deployment or restart.

R6N activity: OpenAI `0`, Claude `0`, other providers `0`, retries `0`, external evidence egress `0`, new provider attempts `0`, and new provider-state records `0`. Historical R6-R1 OpenAI activity remains exactly one consumed call.

## Final verdict

**C — POST-ADMISSION VERIFICATION FAILURE.**

Exactly one derivative was admitted and is preserved, but canonical owner readback failed at the ordinary-region-versus-historical-Tier-A presentation boundary. No further continuation, mutation, provider action or external reasoning is authorized in this unit.
