# OI11R5Q — V8 Implementation-Bound Production Capability Acceptance

## Verdict

**A — V8 IMPLEMENTATION-BOUND PRODUCTION CAPABILITY ACCEPTED**

Steven explicitly accepted the unchanged V8 capability on exact deployed implementation `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`. Parker created exactly one canonical implementation-bound acceptance record. The evaluator now returns `ACCEPTED`; no evidence-specific authority or provider activity occurred.

## Starting repository state

The host was `parker`, repository `/home/steve/parker-platform`, branch `main`. Starting HEAD and upstream were both exactly `502e2953c54e37e9e6ab959e9d68a49eec3f22fe`; the worktree was clean and `git diff --check` passed.

## Explicit owner acceptance boundary

The owner accepted production execution authority for capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, on implementation `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` and OCI index `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`.

This is implementation-bound capability authority only. It does not authorize the registered Deed's preparation, evidence-specific execution, provider transmission, retry, transcription or external reasoning, and it does not change capability semantics or identity.

## Exact production identity

Before mutation, production was exact:

| Field | Exact value |
|---|---|
| Container | `8b7c4b9b9f1b374de278e37d2f01c8401bc8ab809516d21135ddebf1e8065d7c` |
| Image/index | `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| Embedded source | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| Started | `2026-09-02T08:57:38.782928781Z` |
| Restart count | 0 |
| Status | running |
| Readiness | PASS (`overallReady=true`, empty reasons) |

No identity mismatch existed.

## V8 identity and pre-acceptance evaluator

The canonical runtime evaluator exposed capability `ordinary-external-request-region-transcription-v8`, unchanged digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, wire version 8, 32-region maximum, 16,777,216-byte body maximum and batching false.

Before mutation it returned:

| Field | Exact value |
|---|---|
| Disposition | `CAPABILITY_NOT_ACCEPTED` |
| Runtime embedded build | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| Accepted promoting build | null |

This proved the exact deployment was fail-closed before recording the owner's decision.

## Historical acceptance inventory

The store contained ten records: six immutable legacy capability records and four immutable V8 implementation-bound records.

| Record ID | Family/binding | File SHA-256 |
|---|---|---|
| `1c08c6e5bce06505dc114970fb36a9e59f664326688f280b46403e5265bec01b` | legacy v2 | `413c8a888724d2a35d245fdb82a2d09921791acd0f2492c56645d17ddd093ce8` |
| `1ef37f99850d3367fe39cd94c18262318edf043836bd546dff239131bbd14ce6` | legacy v2 | `58f803531959888dc24397ad62ed6b64d9ed0997e0ca1ef7785c20e3bfe410f3` |
| `226108722a77de027e3ad15226704009aa5f1efb7effd6f35a3c31075a8c58c2` | legacy v2 | `5514bc04430659c724055c601c4bc6589a0b640d79f93fdf2757696fed499b60` |
| `5303c27eb6cb098a740a1ac5c182d994dc1293e29ef8e1e5ca1516d751580a21` | legacy v2 | `2830d939048196f5918bb6cc4bad3140cb5a5390958bb1aebb3c2d45604345af` |
| `d26cc25642aceded047b52b61c6f3f3956809c635bc9976da25c898cb1d387ac` | legacy v2 | `6592794a85e37b88507afb1df76e5fa29010ca9ab89761a49fe1a0ad4ccbad42` |
| `e29b10970d63890a260e49c42f3d26103b7aece397d43e122d8557b209a63da3` | legacy v2 | `58a36c9c1de8087ea99cd6877a6c202e502a591630d8d0826616ff3e1b4b5d90` |
| `c3efa482db11d5ecea93ddf9b5cce1fb01793ed25f0577a62a054bf783a8a125` | V8, `c304fdeff6bd89f96e8397ef4192e9f83b41cb93` | `2704d826896002c077c1eeb8d323c100d313546ef5a477eacb73deb0d15eb03e` |
| `85af5c7b87fe9c7fe5f71a403039aff2e7c5db65e5c598471354ed4796d87d1c` | V8, `d33518e85604083d620be08be4a4f001d7be3187` | `b3ec36185e40a1a5940726d1311e6d5351890bb4d14791b97f218ae10f61aca5` |
| `9fe944069ed6a664e75850247b113d9cc1db3f7f52c866ef5abfa7c746cf0915` | V8, `9e2a900fee388ebf4787817c24f34a63190b3f0d` | `943cf8abf75597b0507d4fa5eaf3679eb558964a76ffde0911e37c156b8d11af` |
| `ab14b21a53d1e2ddccb7c0dfbacb0189e6ec6ccbde46c1384467f58fdf062d76` | V8, `fe13047df0dd5f155d6a6921acf7bc85541af26f` | `4291d3e429274712b343500d5538661b0dcd0f7d42b539e57efaa5bb3edeaa3a` |

The pre-mutation aggregate capability-store inventory digest was `3d0ce2eeb4d2642c74dc049e98b2330db84aadce51cfd7ae5b3b17e95a921843`.

## Canonical acceptance operation and result

The authenticated localhost-only canonical route `POST /owner/admin/region-capability-acceptance` received exactly:

```json
{"capabilityId":"ordinary-external-request-region-transcription-v8","promotingBuildCommit":"a031c92549fd7a3b8c92f6917be0e59b61ca5fde"}
```

No evidence ID, provider authorization, provider execution permission, request payload, retry or external-reasoning field was supplied. The route returned `CREATED` exactly once.

## New canonical record

| Field | Exact value |
|---|---|
| Record ID/digest | `206d713979522d79d4094699e8eef1c15e0aded801ae8626bf7c74e7629e6f86` |
| Path | `/data/region-transcription-capability-acceptances/206d713979522d79d4094699e8eef1c15e0aded801ae8626bf7c74e7629e6f86.request-region-v8-capability-acceptance-v1` |
| File SHA-256 | `a89619c47d424a67942d0ea945387126de8d0515bbd900fa6b54600aefb65382` |
| Size | 453 bytes |
| Capability | `ordinary-external-request-region-transcription-v8` |
| Capability digest | `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0` |
| Implementation/promoting commit | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| Accepted by | `user.steve` |
| Accepted at | `2026-09-02T09:06:31.678918535Z` |
| Acceptance evidence | `acceptance-evidence-ordinary-ingestion-10r7-v8-fidelity` |
| Acceptance evidence SHA-256 | `34ec3c703aacb754c45fa58ddf941d7368e2b4cc2e373cb412eb99c4de30902b` |

Canonical record readback reproduced every exact owner-accepted field.

## Post-acceptance evaluator

The same authenticated canonical evaluator returned:

| Field | Exact value |
|---|---|
| Disposition | `ACCEPTED` |
| Capability | `ordinary-external-request-region-transcription-v8` |
| Runtime embedded build | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| Accepted promoting build | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |

Provider readiness was not interpreted as evidence-specific authorization.

## Historical immutability and store accounting

All ten historical files were rehashed after mutation and every SHA-256 remained identical to the inventory above. The new record is additive; no historical record was overwritten, superseded, deleted or reinterpreted. Post-mutation capability-store count is 11 and aggregate inventory digest is `7965a69748fe8d5009d631ea51372a33e60396d440e4d679693a38caf3ff0910`.

| Governed store | Before | After | Delta | Aggregate digest unchanged |
|---|---:|---:|---:|---|
| evidence | 29 | 29 | 0 | yes |
| evidence source manifests | 29 | 29 | 0 | yes |
| capability acceptances | 10 | 11 | +1 | expected additive change |
| owner authorizations | 11 | 11 | 0 | yes |
| attempts | 8 | 8 | 0 | yes |
| provider state | 6 | 6 | 0 | yes |
| derivative generations | 22 | 22 | 0 | yes |
| derivative content | 20 | 20 | 0 | yes |
| corrected preparations | 0 | 0 | 0 | yes |

The unchanged non-capability aggregate digests were independently compared before and after. The capability store changed by exactly the single authorized record.

## Deed and provider boundary

Registered evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, was not passed to corrected preparation, request construction, authorization or execution. Corrected-preparation count remains zero. No execution ID, attempt, provider state, response or derivative was created.

OpenAI calls: 0. Claude calls: 0. Other external provider calls: 0. Retries: 0. External provider/evidence egress: 0. Provider-state delta: 0.

## Production stability and stop state

After acceptance production remained the same container, image, source and start time with restart count zero. Repeated canonical readiness returned PASS. Logs showed no background execution. There was no deployment or restart.

R5Q stops before any real-document preparation, evidence-specific authorization or provider execution.

UNIT ORDINARY-INGESTION-11R5Q COMPLETE — THE OWNER HAS EXPLICITLY ACCEPTED THE UNCHANGED ORDINARY-EXTERNAL-REQUEST-REGION-TRANSCRIPTION-V8 CAPABILITY AND DIGEST C0479979720455D2DE3FC9861EEB5DEE323A4770BDB15F807AF611AD426F9EC0 FOR THE EXACT DEPLOYED IMPLEMENTATION A031C92549FD7A3B8C92F6917BE0E59B61CA5FDE. EXACTLY ONE NEW CANONICAL IMPLEMENTATION-BOUND ACCEPTANCE RECORD WAS CREATED, HISTORICAL ACCEPTANCE RECORDS REMAIN IMMUTABLE, AND THE PRODUCTION EVALUATOR NOW RETURNS ACCEPTED. THE REGISTERED DEED REMAINS UNPREPARED AND UNAUTHORIZED FOR PROVIDER EXECUTION. NO PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. PRODUCTION REMAINS STABLE. THE UNIT STOPS BEFORE ANY REAL-DOCUMENT PREPARATION OR EXECUTION.
