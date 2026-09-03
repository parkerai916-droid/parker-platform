# OI11R6I — Governed Zero-Egress Continuation of Preserved R6-R1 Provider Result and Derivative Admission

## Verdict

**B — PRE-ADMISSION CONTINUATION FAILURE**

The sole canonical continuation invocation stopped before raw-state replay and derivative construction with HTTP 409, disposition `VALIDATION_FAILED`, detail `DURABLE_PROVIDER_ATTEMPT_REQUIRED`, and `providerInvoked=false`. No derivative was admitted. OI11R6I provider calls, retries and external egress were `0 / 0 / 0`. No retry or alternative admission path was attempted.

## Starting repository state

The repository was on branch `main`; HEAD and upstream were both `db3b2469b73d3fd5348a66a741d8b1a531588f06`; the worktree was clean.

## Explicit owner continuation authorization

Steven explicitly authorized one zero-egress continuation of preserved execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`, using only its existing canonical authorization, execution/attempt ledger, provider state, raw response, and validated evidence/preparation/request bindings. Derivative admission was authorized only after all continuation gates passed. No provider call, retry, new authorization, execution, attempt, or evidence egress was authorized.

## Production identity and capability authority

Production identity was exact before and after the attempt:

| Field | Exact value |
|---|---|
| Container | `f2a9df159d4305528ff89ac26e2dcc8f5e51fc969839f0d91f204976cb6ed542` |
| Image/index | `sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089` |
| Embedded source | `e3fec3fac857e7b0e610375d066d524646a1375f` |
| Runtime JAR SHA-256 | `f4971f223612a1791ca6a013bc3234e03d85b9cfbb7fecc09886b40983861d63` |
| Started | `2026-09-02T12:55:25.243280806Z` |
| Restart count | `0` |

The canonical readiness diagnostic returned all predicates true, `ordinaryExecutionReady=true`, `overallReady=true`, and no reasons: readiness **PASS**.

Implementation-bound V8 acceptance record `e91808b75afca1f49784a0b69ce2840bc9e74e5b35c3f163730909bb40b59866` had exact file SHA-256 `2133bb08f2e265f7969694c3e9566f6cdf25f61c8dc58d256ddc5b32dd8edf28` and bound capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, to implementation `e3fec3fa...`. The authenticated evaluator returned `ACCEPTED`.

## Evidence, preparation, and owner-reviewed transport baseline

Canonical custody readback verified evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, size `1,887,733 bytes`, MIME `application/pdf`, five pages, and manifest SHA-256 `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34`. Source and manifest were not modified.

The canonical corrected-preparation record was identity `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, file SHA-256 `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`, profile `full-page-achromatic-png-preparation-v1` version 1, five regions in order `[1,2,3,4,5]`. It was read only and not regenerated.

The five transport objects matched the corrected owner-reviewed baseline exactly:

1. `17dbc36d7df4db281d8052bac6ef5a14a5dc04d6ead57413efd707ea3a49504c`
2. `4de7ea456db529899be8d9ad714a7e3318af70c3a2c68b4c55bd8f75656e1dae`
3. `c481b51bd68d93bd0346237c038c7d4768a4174fa2d42922a30fb4e5630a578b`
4. `b0c37fa3032e545d80eeac5da371862586115f8304c41e44c11a63603a723705`
5. `3b4f992240518ddbb872fddd65f58c130629c8affd1d0f36d783100c4d1a9816`

Owner acceptance `/home/steve/parker-owner-review/oi11r5r/owner-acceptance.json` had SHA-256 `f55b70e073f61b329d4410489bf386938e7adf729ee0c8279cbd2e3cf183547f`; corrected manifest SHA-256 was `be304dc0cd978bf8ffa55d58b77d89f55aa60c1158f467e47d2882ce82163b99`.

## Preserved authorization, execution, provider state, and budget

Canonical authorization `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97` exactly bound the evidence/source, preparation/profile version, request digest `2f4f595decb924fd6d252735494dabc85b8e375c4d17e41f952195061e2675a3`, capability/digest, Purpose `evidence-intelligence.external-transcription`, provider `OpenAI`, provider profile `openai-fidelity-first-transcription-v1`, model `gpt-5.6-sol`, maximum one provider call, zero retries, and no external reasoning.

The preserved execution was `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`. Its ledger records `PROVIDER_ATTEMPT_STARTED` followed by `PROVIDER_RESPONSE_RECEIVED`, accurately preserving the historical R6-R1 post-egress failure boundary. Maximum calls remained 1, consumed calls 1, retry limit 0.

Provider-state record `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7` bound the exact evidence, source, capability, execution and request. It records HTTP 200 and raw SHA-256 `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`. The separate persisted assessment remained `SUCCESS`, five blocks ordered `[1,2,3,4,5]`, authoritative response ID `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`, and model `gpt-5.6-sol`. Raw state remained immutable.

## Pre-continuation derivative and store state

A complete search found no generation or content record bound to the R6-R1 execution and no conflicting derivative. The complete pre-snapshot was:

| Store | Count | Aggregate digest |
|---|---:|---|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| evidence manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| corrected preparations | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` |
| capability acceptances | 13 | `e183ca8ed3cbf1542f6bbebc20bb101c9092245b3319a31721afdf814f3b0ead` |
| execution authorizations | 14 | `b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6` |
| attempts | 10 | `798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f` |
| provider state | 8 | `be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6` |
| derivative generations | 22 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` |
| derivative content | 20 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` |
| evidence audit | 1 | `1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59` |
| document-ingestion audit | 1 | `9349a5cd71c6efbefa1524115900c0769c13e464f08f4bbd5b41f3ac3c42d72e` |

## Canonical continuation operation and exact failure

The authenticated canonical `POST /owner/admin/region-transcription-continuation` operation was invoked exactly once with only the exact evidence, authorization, execution and provider-state identities. It returned:

* HTTP `409`;
* status `VALIDATION_FAILED`;
* detail `DURABLE_PROVIDER_ATTEMPT_REQUIRED`;
* derivative generation ID `null`;
* `providerInvoked=false`.

The operation stopped during mandatory eligibility recovery, before deterministic raw-state replay, corrected provider-profile provenance construction, derivative validation, or admission.

## Failure analysis

The failure is an execution-identity convergence defect, not absent historical attempt evidence. The R6-R1 ledger visibly and canonically contains `PROVIDER_ATTEMPT_STARTED` and `PROVIDER_RESPONSE_RECEIVED` for the exact execution. However:

1. the continuation preparer reconstructs `FidelityFirstExecutionIdentity` using the currently deployed implementation commit `e3fec3fac857e7b0e610375d066d524646a1375f`;
2. the immutable R6-R1 ledger identity is correctly bound to its execution-time implementation `39fe0e777608c96cba20cec491113e77eee4b8ef`;
3. `recoverPersistedPostEgress` opens the ledger using the newly reconstructed current-implementation identity;
4. canonical ledger identity validation rejects that mismatch;
5. recovery fail-closes as `DURABLE_PROVIDER_ATTEMPT_REQUIRED`.

Thus the deployed continuation path cannot presently continue an historical provider result across the implementation transition it was designed to remediate. Resolving this requires a separately scoped implementation/governance unit that preserves execution-time identity from canonical persisted state rather than substituting the continuation runtime commit. OI11R6I did not modify code or bypass this gate.

## Replay, provenance, admission, and readback results

Canonical deterministic replay was **not reached in this invocation**. The pre-existing persisted assessment remains independently `SUCCESS` 5/5 with Parker order `[1,2,3,4,5]`, but it was not sufficient to bypass the failed continuation eligibility gate.

Provider-profile provenance construction, derivative validation, admission, and canonical derivative readback were therefore not performed. Derivative generation/content identities remain absent. No invalid or partial derivative was created.

## Historical preservation and governed-store accounting

Every post-attempt count and aggregate digest exactly matched the pre-snapshot above. Delta was zero for evidence, manifests, corrected preparations, capability acceptances, authorizations, attempts, provider state, derivative generations/content, and both audit stores.

The exact historical file hashes also remained unchanged:

* authorization events: `64070a0d042373164902615edd03a19a4bdf7f602e911d7eecbcec8c31bcb675`;
* authorization: `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544`;
* execution ledger: `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591`;
* assessment: `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01`;
* raw provider-state record: `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c`;
* corrected preparation record: `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`.

The original R6-R1 post-egress failure remains historical truth. OI11R6I added no production record.

## Call-budget, idempotence, production, and provider accounting

Provider budget remains maximum 1, consumed 1, retry limit 0. OI11R6I created no authorization, execution, attempt or provider state. OpenAI calls `0`, Claude calls `0`, other external calls `0`, retries `0`, external evidence egress `0`.

No derivative exists, so post-admission idempotence verification was inapplicable. The canonical continuation's fail-closed result itself created no duplicate or conflicting admission.

Production remains on the same container, image, source and JAR with restart count zero and readiness PASS. No deployment or restart occurred.

## Mandatory stop

OI11R6I stops at the pre-admission continuation eligibility failure. Provider calls were `0`, retries were `0`, external evidence egress was `0`, and no invalid derivative was admitted. No retry is authorized.
