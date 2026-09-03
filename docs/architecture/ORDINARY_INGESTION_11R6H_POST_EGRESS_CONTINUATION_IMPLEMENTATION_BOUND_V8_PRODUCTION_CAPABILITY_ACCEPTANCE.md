# OI11R6H — Post-Egress Continuation Implementation-Bound V8 Production Capability Acceptance

## Verdict

**A — POST-EGRESS CONTINUATION IMPLEMENTATION-BOUND V8 PRODUCTION CAPABILITY ACCEPTED**

The owner-authorized implementation-bound acceptance was recorded through Parker's canonical production route. Exactly one capability-acceptance record was added. The evaluator changed from `CAPABILITY_NOT_ACCEPTED` to `ACCEPTED`; all historical and R6-R1 state remained immutable; no continuation, derivative admission, provider activity, retry, egress, deployment or restart occurred.

## Starting repository state

The repository was on branch `main`. HEAD and upstream were both `615ca14cbb7071f13ba40c0f3a387903dfa57593`; the worktree was clean.

## Explicit owner acceptance

Steven explicitly accepted unchanged capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, for exact deployed implementation `e3fec3fac857e7b0e610375d066d524646a1375f`, OCI index `sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089`, and runtime JAR `f4971f223612a1791ca6a013bc3234e03d85b9cfbb7fecc09886b40983861d63`.

This acceptance is implementation-bound only. It does not authorize continuation, derivative admission, another evidence authorization/execution/attempt, provider activity, retry, evidence egress, external reasoning, or mutation of the Deed, corrected preparation, or preserved R6-R1 state. The historical R6-R1 budget remains maximum one call, one consumed, retry limit zero. **NO RETRY IS AUTHORIZED.**

## Production identity and stability

Production passed every gate before and after acceptance:

| Field | Exact value |
|---|---|
| Container | `f2a9df159d4305528ff89ac26e2dcc8f5e51fc969839f0d91f204976cb6ed542` |
| Image/index | `sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089` |
| Embedded source | `e3fec3fac857e7b0e610375d066d524646a1375f` |
| Runtime JAR SHA-256 | `f4971f223612a1791ca6a013bc3234e03d85b9cfbb7fecc09886b40983861d63` |
| Started | `2026-09-02T12:55:25.243280806Z` |
| Restart count | `0` |

The canonical diagnostic returned every predicate true, `ordinaryExecutionReady=true`, `overallReady=true`, and empty reasons: readiness **PASS**. No deployment or restart occurred.

## Continuation composition

The same exact deployed runtime verified in OI11R6G remains running. Its authenticated owner/admin post-egress continuation route is composed to the governed workflow that consumes persisted authorization, execution, preparation and provider state. Static production composition retains provider-free deterministic replay, authorization-bound provider-profile mapping, create-once/idempotent derivative admission and fail-closed conflict/eligibility handling. The continuation route was not invoked. No provider dependency, attempt creation, call-budget reset or retry is reachable through that operation.

## V8 identity and pre-acceptance evaluator

The deployed V8 capability identity remained exactly:

* capability: `ordinary-external-request-region-transcription-v8`;
* digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`;
* implementation: `e3fec3fac857e7b0e610375d066d524646a1375f`.

Before mutation, the authenticated canonical evaluator returned runtime source `e3fec3fa...`, accepted promoting build `null`, and disposition `CAPABILITY_NOT_ACCEPTED`. No matching record already existed.

## Historical acceptance inventory

The pre-mutation store contained exactly 12 records: six legacy v2 and six implementation-bound V8 records.

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

The complete sorted pre-inventory aggregate digest was `0089970e3bf20a29b845aa0a2820722e47bead49864831d9c0d7873545c18237`. In particular, the R6D acceptance remained exact.

## Canonical acceptance operation and new record

The authenticated localhost canonical route `POST /owner/admin/region-capability-acceptance` received exactly:

```json
{"capabilityId":"ordinary-external-request-region-transcription-v8","promotingBuildCommit":"e3fec3fac857e7b0e610375d066d524646a1375f"}
```

It returned HTTP 201 and `CREATED` exactly once. No continuation, evidence, preparation, provider, execution, retry or reasoning field was supplied.

| Field | Exact value |
|---|---|
| Record ID/digest | `e91808b75afca1f49784a0b69ce2840bc9e74e5b35c3f163730909bb40b59866` |
| Path | `/data/region-transcription-capability-acceptances/e91808b75afca1f49784a0b69ce2840bc9e74e5b35c3f163730909bb40b59866.request-region-v8-capability-acceptance-v1` |
| File SHA-256 | `2133bb08f2e265f7969694c3e9566f6cdf25f61c8dc58d256ddc5b32dd8edf28` |
| Size/protection | `453 bytes`; `0644`, `parker:parker` |
| Capability | `ordinary-external-request-region-transcription-v8` |
| Capability digest | `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0` |
| Implementation | `e3fec3fac857e7b0e610375d066d524646a1375f` |
| State/result | accepted / `CREATED` |
| Accepted by | `user.steve` |
| Accepted at | `2026-09-03T00:43:23.736721801Z` |

Canonical record identity and persisted fields exactly match the owner-authorized boundary.

## Post-acceptance evaluator and historical immutability

The authenticated evaluator subsequently returned disposition `ACCEPTED`, runtime source `e3fec3fac857e7b0e610375d066d524646a1375f`, and accepted promoting build equal to that exact source.

All 12 pre-existing record hashes were rechecked and remained byte-identical. The post-inventory contains exactly 13 records with aggregate digest `e183ca8ed3cbf1542f6bbebc20bb101c9092245b3319a31721afdf814f3b0ead`; the sole new file is `e91808b...`. The operation was additive only.

## Preserved R6-R1 state

The historical R6-R1 state remains:

* authorization `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97`;
* execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`;
* provider state `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`;
* raw SHA-256 `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`;
* response ID `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`, model `gpt-5.6-sol`;
* V8 `SUCCESS`, 5/5, Parker order `[1,2,3,4,5]`;
* maximum calls `1`, consumed `1`, retry limit `0`;
* derivative **NOT ADMITTED**.

Authorization/event, execution-ledger, assessment and provider-state file hashes remained respectively `48f4e540...`, `64070a0d...`, `bff24b6c...`, `bace0583...`, and `c3f1ea29...`. Searches of derivative generation/content found zero records bound to the R6-R1 execution before and after acceptance.

## Governed-store accounting

| Store | Before | After | Aggregate digest after | Delta |
|---|---:|---:|---|---:|
| evidence | 29 | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` | 0 |
| evidence manifests | 29 | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` | 0 |
| corrected preparations | 6 | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` | 0 |
| capability acceptances | 12 | 13 | `e183ca8ed3cbf1542f6bbebc20bb101c9092245b3319a31721afdf814f3b0ead` | **+1** |
| execution authorizations | 14 | 14 | `b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6` | 0 |
| attempts | 10 | 10 | `798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f` | 0 |
| provider state | 8 | 8 | `be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6` | 0 |
| derivative generations | 22 | 22 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` | 0 |
| derivative content | 20 | 20 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` | 0 |
| evidence audit | 1 | 1 | `1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59` | 0 |
| document-ingestion audit | 1 | 1 | `9349a5cd71c6efbefa1524115900c0769c13e464f08f4bbd5b41f3ac3c42d72e` | 0 |

Only the authorized acceptance store changed.

## Provider and egress accounting

OI11R6H activity: OpenAI `0`, Claude `0`, other providers `0`, retries `0`, external evidence egress `0`, provider-state delta `0`. The historical R6-R1 OpenAI total remains exactly one. No continuation, new authorization, execution, attempt or derivative admission occurred.

## Completion state

Production remains stable on exact index `a5f56508...`, source `e3fec3fa...`, JAR `f4971f22...`, restart count zero and readiness PASS. The V8 evaluator is now `ACCEPTED`. A separate owner-governed zero-egress continuation decision is required before the preserved R6-R1 result may be admitted as a derivative.
