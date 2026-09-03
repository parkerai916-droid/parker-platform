# OI11R6M — Historical-Identity-Corrected Continuation Implementation-Bound V8 Production Capability Acceptance

## Verdict

**A — HISTORICAL-IDENTITY-CORRECTED CONTINUATION IMPLEMENTATION-BOUND V8 PRODUCTION CAPABILITY ACCEPTED**

Exactly one owner-authorized implementation-bound acceptance was created through Parker's canonical production route. The evaluator moved from `CAPABILITY_NOT_ACCEPTED` to `ACCEPTED`; all historical and R6-R1 state remained immutable; no continuation, derivative admission, provider activity, retry, egress, deployment or restart occurred.

## Starting repository state

Branch was `main`; HEAD and upstream were both `7f020328e071720351676be8791ea81e801eb273`; the worktree was clean.

## Explicit owner acceptance

Steven explicitly accepted unchanged capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, for exact deployed implementation `ca15222c9f5edea28e68bbb0099734578fc30c4a` only.

This is implementation-bound capability authority. It does not authorize continuation, derivative admission, new authorization/execution/attempt state, provider activity, retry, evidence egress, external reasoning, or mutation/reinterpretation of preserved R6-R1 state. The historical provider budget remains maximum one, consumed one, retry limit zero. **NO RETRY IS AUTHORIZED.**

## Exact production artifact identity

| Field | Exact value |
|---|---|
| Container | `aae0bf09790510cdb6d2e47a7dfeb25e79bc7f4b86236e7e3121fb5fa66f3149` |
| OCI image/index | `sha256:adbb96afdb732a4549661fef08773d1b70a471e5311c804392f5fba26ce1ea4e` |
| Platform manifest | `sha256:374787378c5096c65231c959754d2ff9c6e694fc03c6dff33741a620f37081ba` |
| OCI config | `sha256:fd2e5764f059980e946da8d92b15e0598cae964bdfd6489b7e8888fd855ecd4a` |
| Embedded source | `ca15222c9f5edea28e68bbb0099734578fc30c4a` |
| Runtime JAR SHA-256 | `ce5191a5a04de91c9697acb38043cba6ddfa0c11bfb20f16babeb216804d7137` |
| Archive SHA-256 | `ed6b5efc984ec4bed2feb254e7fd78b31fd770a90659c6f3bd50530249fe11a4` |
| Artifact-acceptance record SHA-256 | `1566341ed674d8ed5c230bdf4588a78f0295301aec4e77a3cbbf22ff5e7d3329` |
| Restart count/readiness | `0` / `PASS` |

The archive path was `/mnt/parker-data/parker/replacement-candidates/oi11r6k-historical-identity-corrected-continuation-ca15222c-20260903.tar`; the acceptance path was `/mnt/parker-data/parker/replacement-candidates/oi11r6k-artifact-acceptance-adbb96af-v1.json`. Both hashes matched exactly.

## Historical/current identity separation

Production retains two distinct identities:

* immutable R6-R1 execution/attempt implementation: `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* current continuation implementation: `ca15222c9f5edea28e68bbb0099734578fc30c4a`.

Exact-source and deployed-artifact inspection confirms historical validation derives the former from canonical persisted attempt state. Current capability authority binds only the latter. No current runtime identity is substituted into historical state.

## Continuation provider prohibition

The deployed continuation route remains composed to the persisted-state-only workflow. It has no provider transport dependency or invocation edge, cannot create a new provider attempt, and cannot reset or extend the consumed provider budget. It retains fail-closed eligibility/conflict checks and create-once admission semantics. The continuation endpoint was not invoked.

## V8 identity and pre-acceptance evaluator

Capability identity and digest were exact and unchanged. Before mutation, the authenticated canonical evaluator reported runtime implementation `ca15222c9f5edea28e68bbb0099734578fc30c4a`, accepted promoting implementation `null`, and disposition `CAPABILITY_NOT_ACCEPTED`. No matching record already existed.

## Historical capability-acceptance inventory

The pre-mutation store contained exactly 13 records: six legacy v2 records and seven implementation-bound V8 records.

| Record ID | Family / implementation | File SHA-256 |
|---|---|---|
| `1c08c6e5bce06505dc114970fb36a9e59f664326688f280b46403e5265bec01b` | legacy / `3ae55f492e10f18b9dca0846114bd80458680fe6` | `413c8a888724d2a35d245fdb82a2d09921791acd0f2492c56645d17ddd093ce8` |
| `1ef37f99850d3367fe39cd94c18262318edf043836bd546dff239131bbd14ce6` | legacy / `1694a6a576a8d34a305ab1f3d797b5ab8f8b65d5` | `58f803531959888dc24397ad62ed6b64d9ed0997e0ca1ef7785c20e3bfe410f3` |
| `226108722a77de027e3ad15226704009aa5f1efb7effd6f35a3c31075a8c58c2` | legacy / `def611a8bf8cb6c2297f1d9bf6cd8146a58d4cbc` | `5514bc04430659c724055c601c4bc6589a0b640d79f93fdf2757696fed499b60` |
| `5303c27eb6cb098a740a1ac5c182d994dc1293e29ef8e1e5ca1516d751580a21` | legacy / `363201a2233d29240571781ced0e78dfbc6680e1` | `2830d939048196f5918bb6cc4bad3140cb5a5390958bb1aebb3c2d45604345af` |
| `d26cc25642aceded047b52b61c6f3f3956809c635bc9976da25c898cb1d387ac` | legacy / `72482456531e91ff8eced4cbe073f182ae805126` | `6592794a85e37b88507afb1df76e5fa29010ca9ab89761a49fe1a0ad4ccbad42` |
| `e29b10970d63890a260e49c42f3d26103b7aece397d43e122d8557b209a63da3` | legacy / `ea1d96d656e97c7ed350eeabec5ef279b8ac36bb` | `58a36c9c1de8087ea99cd6877a6c202e502a591630d8d0826616ff3e1b4b5d90` |
| `c3efa482db11d5ecea93ddf9b5cce1fb01793ed25f0577a62a054bf783a8a125` | V8 / `c304fdeff6bd89f96e8397ef4192e9f83b41cb93` | `2704d826896002c077c1eeb8d323c100d313546ef5a477eacb73deb0d15eb03e` |
| `85af5c7b87fe9c7fe5f71a403039aff2e7c5db65e5c598471354ed4796d87d1c` | V8 / `d33518e85604083d620be08be4a4f001d7be3187` | `b3ec36185e40a1a5940726d1311e6d5351890bb4d14791b97f218ae10f61aca5` |
| `9fe944069ed6a664e75850247b113d9cc1db3f7f52c866ef5abfa7c746cf0915` | V8 / `9e2a900fee388ebf4787817c24f34a63190b3f0d` | `943cf8abf75597b0507d4fa5eaf3679eb558964a76ffde0911e37c156b8d11af` |
| `ab14b21a53d1e2ddccb7c0dfbacb0189e6ec6ccbde46c1384467f58fdf062d76` | V8 / `fe13047df0dd5f155d6a6921acf7bc85541af26f` | `4291d3e429274712b343500d5538661b0dcd0f7d42b539e57efaa5bb3edeaa3a` |
| `206d713979522d79d4094699e8eef1c15e0aded801ae8626bf7c74e7629e6f86` | V8 / `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` | `a89619c47d424a67942d0ea945387126de8d0515bbd900fa6b54600aefb65382` |
| `bc8463b676b3121150af2bbaf86d6aa99feb6eceb24ba60592cf526c00a12d43` | V8 / `39fe0e777608c96cba20cec491113e77eee4b8ef` | `c9ecbf23232868275003d73ce71190f4acb4f3eabc1bfa76de4160d5e8b58824` |
| `e91808b75afca1f49784a0b69ce2840bc9e74e5b35c3f163730909bb40b59866` | V8 / `e3fec3fac857e7b0e610375d066d524646a1375f` | `2133bb08f2e265f7969694c3e9566f6cdf25f61c8dc58d256ddc5b32dd8edf28` |

The sorted pre-inventory aggregate digest was `e183ca8ed3cbf1542f6bbebc20bb101c9092245b3319a31721afdf814f3b0ead`.

## Canonical acceptance operation and new record

The authenticated localhost canonical route `POST /owner/admin/region-capability-acceptance` received exactly:

```json
{"capabilityId":"ordinary-external-request-region-transcription-v8","promotingBuildCommit":"ca15222c9f5edea28e68bbb0099734578fc30c4a"}
```

It returned HTTP 201 and `CREATED` exactly once.

| Field | Exact value |
|---|---|
| Record ID/digest | `bb67c84688f34d76fc6d2f86946ebd1d1f38c14f4f9bbe91259d0e458715ce4d` |
| Path | `/data/region-transcription-capability-acceptances/bb67c84688f34d76fc6d2f86946ebd1d1f38c14f4f9bbe91259d0e458715ce4d.request-region-v8-capability-acceptance-v1` |
| File SHA-256 | `ee39f493cbcde459f3a2ca7ef4368df848122b0d3738c389d69bf6e0c66d884c` |
| Size/protection | `453 bytes`; `0644`, `parker:parker` |
| Capability | `ordinary-external-request-region-transcription-v8` |
| Capability digest | `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0` |
| Implementation | `ca15222c9f5edea28e68bbb0099734578fc30c4a` |
| State/result | accepted / `CREATED` |
| Accepted by | `user.steve` |
| Accepted at | `2026-09-03T03:33:42.203068743Z` |

## Post-acceptance evaluator

The canonical evaluator now reports disposition `ACCEPTED`, runtime implementation `ca15222c9f5edea28e68bbb0099734578fc30c4a`, and accepted promoting implementation `ca15222c9f5edea28e68bbb0099734578fc30c4a`.

## Preserved R6-R1 state

Authorization `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97`, execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`, attempt ledger, provider state `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`, raw digest `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`, response ID `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`, model `gpt-5.6-sol`, and historical V8 success 5/5 remain exact.

The authorization, attempt ledger, provider-state and V8-assessment file hashes remained respectively `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544`, `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591`, `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c`, and `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01`. The budget remains one authorized, one consumed, zero retries. No derivative record references the execution.

## Governed-store accounting

| Store | Before | After | Delta |
|---|---:|---:|---:|
| Evidence | 29 | 29 | 0 |
| Evidence manifests | 29 | 29 | 0 |
| Corrected preparations | 6 | 6 | 0 |
| Capability acceptances | 13 | 14 | **+1** |
| Execution authorizations | 14 | 14 | 0 |
| Attempt files | 10 | 10 | 0 |
| Provider state | 8 | 8 | 0 |
| Derivative generations | 22 | 22 | 0 |
| Derivative content | 20 | 20 | 0 |
| Evidence audit | 1 | 1 | 0 |
| Document-ingestion audit | 1 | 1 | 0 |

Every non-capability aggregate digest remained identical. The capability-store aggregate digest changed only from `e183ca8ed3cbf1542f6bbebc20bb101c9092245b3319a31721afdf814f3b0ead` to `14eeeb9ceac5361c394c58aeae66ee9b3d6b908717ce06e416c113e0aa97b950` due to the one new record.

## Historical acceptance immutability

All 13 pre-existing record SHA-256 values matched the pre-mutation inventory after creation. In particular, the prior `e3fec3fa...` record remained `2133bb08f2e265f7969694c3e9566f6cdf25f61c8dc58d256ddc5b32dd8edf28`. The mutation was additive only.

## Provider/egress accounting and production stability

OI11R6M activity: OpenAI `0`, Claude `0`, other providers `0`, retries `0`, external evidence egress `0`, new attempts `0`, and provider-state delta `0`. Historical R6-R1 OpenAI activity remains exactly one consumed call.

Production remains container `aae0bf09...` on exact index `adbb96af...`, source `ca15222c...`, JAR `ce5191a5...`, restart count zero and readiness PASS. No deployment or restart occurred.

## Final verdict

**A — HISTORICAL-IDENTITY-CORRECTED CONTINUATION IMPLEMENTATION-BOUND V8 PRODUCTION CAPABILITY ACCEPTED.**

A separate owner-governed zero-egress continuation decision is still required before the preserved R6-R1 result may be admitted as a derivative.
