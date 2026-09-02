# OI11R6D — V8 Implementation-Bound Production Capability Acceptance

Date: 2026-09-02 UTC

## Verdict

**A — V8 IMPLEMENTATION-BOUND PRODUCTION CAPABILITY ACCEPTED**

Steven explicitly accepted the unchanged V8 capability for exact deployed R6-converged implementation `39fe0e777608c96cba20cec491113e77eee4b8ef`. Parker created exactly one canonical additive acceptance record. The evaluator changed from `CAPABILITY_NOT_ACCEPTED` to `ACCEPTED`; every historical acceptance record remained byte-identical and every other governed store remained unchanged.

## Starting repository state

The host was `parker`, repository `/home/steve/parker-platform`, branch `main`. Starting HEAD and upstream were both `151951b49ef3a9e9cc2694d4d08041c78cab0690`; the worktree was clean.

## Explicit owner acceptance boundary

The authorized proposition was limited to:

* capability: `ordinary-external-request-region-transcription-v8`;
* capability digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`;
* implementation: `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* deployed image/index: `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406`.

The acceptance did not authorize Deed preparation/modification, corrected-preparation modification, evidence-specific authority, provider execution, retry, evidence egress, external reasoning or transcription execution. None occurred.

## Production identity gate

Before mutation, production matched exactly:

| Field | Exact value |
|---|---|
| Container | `9011f1e9e9feaa5d0662c295cc295c19b0dce45fb4e8c2a1dad95fbe12f52fa5` |
| Image/index | `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406` |
| Embedded source | `39fe0e777608c96cba20cec491113e77eee4b8ef` |
| Runtime JAR SHA-256 | `d0f52b7d935f568c14d9c7012be43ae7f1ac8d755f522fa03ee34ffe9af530e7` |
| Started | `2026-09-02T11:25:47.196217995Z` |
| Restart count | `0` |
| State | running |
| Readiness | `PASS` |

The canonical readiness diagnostic returned every predicate true, `ordinaryExecutionReady=true`, `overallReady=true`, and no reasons.

## Exact capability and pre-acceptance evaluator

The authenticated canonical production evaluator exposed unchanged V8 identity and transport limits:

* capability: `ordinary-external-request-region-transcription-v8`;
* digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`;
* runtime implementation: `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* wire version: `8`;
* maximum regions: `32`;
* aggregate body maximum: `16,777,216 bytes`;
* batching: `false`;
* disposition: `CAPABILITY_NOT_ACCEPTED`;
* accepted promoting build: `null`.

This proved the new implementation remained fail-closed immediately before the owner decision was recorded.

## Historical acceptance inventory

The pre-mutation store contained 11 records: six legacy records and five historical V8 implementation-bound records.

| Record ID | Family / implementation binding | File SHA-256 |
|---|---|---|
| `1c08c6e5bce06505dc114970fb36a9e59f664326688f280b46403e5265bec01b` | legacy v2 / `3ae55f492e10f18b9dca0846114bd80458680fe6` | `413c8a888724d2a35d245fdb82a2d09921791acd0f2492c56645d17ddd093ce8` |
| `1ef37f99850d3367fe39cd94c18262318edf043836bd546dff239131bbd14ce6` | legacy v2 / `1694a6a576a8d34a305ab1f3d797b5ab8f8b65d5` | `58f803531959888dc24397ad62ed6b64d9ed0997e0ca1ef7785c20e3bfe410f3` |
| `226108722a77de027e3ad15226704009aa5f1efb7effd6f35a3c31075a8c58c2` | legacy v2 / `def611a8bf8cb6c2297f1d9bf6cd8146a58d4cbc` | `5514bc04430659c724055c601c4bc6589a0b640d79f93fdf2757696fed499b60` |
| `5303c27eb6cb098a740a1ac5c182d994dc1293e29ef8e1e5ca1516d751580a21` | legacy v2 / `363201a2233d29240571781ced0e78dfbc6680e1` | `2830d939048196f5918bb6cc4bad3140cb5a5390958bb1aebb3c2d45604345af` |
| `d26cc25642aceded047b52b61c6f3f3956809c635bc9976da25c898cb1d387ac` | legacy v2 / `72482456531e91ff8eced4cbe073f182ae805126` | `6592794a85e37b88507afb1df76e5fa29010ca9ab89761a49fe1a0ad4ccbad42` |
| `e29b10970d63890a260e49c42f3d26103b7aece397d43e122d8557b209a63da3` | legacy v2 / `ea1d96d656e97c7ed350eeabec5ef279b8ac36bb` | `58a36c9c1de8087ea99cd6877a6c202e502a591630d8d0826616ff3e1b4b5d90` |
| `c3efa482db11d5ecea93ddf9b5cce1fb01793ed25f0577a62a054bf783a8a125` | V8 / `c304fdeff6bd89f96e8397ef4192e9f83b41cb93` | `2704d826896002c077c1eeb8d323c100d313546ef5a477eacb73deb0d15eb03e` |
| `85af5c7b87fe9c7fe5f71a403039aff2e7c5db65e5c598471354ed4796d87d1c` | V8 / `d33518e85604083d620be08be4a4f001d7be3187` | `b3ec36185e40a1a5940726d1311e6d5351890bb4d14791b97f218ae10f61aca5` |
| `9fe944069ed6a664e75850247b113d9cc1db3f7f52c866ef5abfa7c746cf0915` | V8 / `9e2a900fee388ebf4787817c24f34a63190b3f0d` | `943cf8abf75597b0507d4fa5eaf3679eb558964a76ffde0911e37c156b8d11af` |
| `ab14b21a53d1e2ddccb7c0dfbacb0189e6ec6ccbde46c1384467f58fdf062d76` | V8 / `fe13047df0dd5f155d6a6921acf7bc85541af26f` | `4291d3e429274712b343500d5538661b0dcd0f7d42b539e57efaa5bb3edeaa3a` |
| `206d713979522d79d4094699e8eef1c15e0aded801ae8626bf7c74e7629e6f86` | V8 / `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` | `a89619c47d424a67942d0ea945387126de8d0515bbd900fa6b54600aefb65382` |

The aggregate sorted historical inventory digest was `7965a69748fe8d5009d631ea51372a33e60396d440e4d679693a38caf3ff0910`.

## Canonical acceptance operation

The authenticated localhost canonical route `POST /owner/admin/region-capability-acceptance` received exactly:

```json
{"capabilityId":"ordinary-external-request-region-transcription-v8","promotingBuildCommit":"39fe0e777608c96cba20cec491113e77eee4b8ef"}
```

No evidence ID, preparation, execution authorization, provider instruction, retry permission or external-reasoning authority was supplied. The route returned HTTP 201 and `CREATED` exactly once.

## New canonical record

| Field | Exact value |
|---|---|
| Record ID/digest | `bc8463b676b3121150af2bbaf86d6aa99feb6eceb24ba60592cf526c00a12d43` |
| Path | `/data/region-transcription-capability-acceptances/bc8463b676b3121150af2bbaf86d6aa99feb6eceb24ba60592cf526c00a12d43.request-region-v8-capability-acceptance-v1` |
| File SHA-256 | `c9ecbf23232868275003d73ce71190f4acb4f3eabc1bfa76de4160d5e8b58824` |
| Size | `453 bytes` |
| Capability | `ordinary-external-request-region-transcription-v8` |
| Capability digest | `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0` |
| Implementation | `39fe0e777608c96cba20cec491113e77eee4b8ef` |
| Accepted by | `user.steve` |
| Accepted at | `2026-09-02T11:34:25.306565204Z` |
| Canonical result | `CREATED` |

Canonical identity, persisted content and owner-authorized fields matched exactly.

## Post-acceptance evaluator

The canonical evaluator then returned:

* disposition: `ACCEPTED`;
* runtime embedded build: `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* accepted promoting build: `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* capability identity and transport limits unchanged.

## Historical immutability

All 11 historical record hashes were recomputed after creation and remained exactly equal to the pre-mutation inventory above. Their aggregate digest excluding the new record remained `7965a69748fe8d5009d631ea51372a33e60396d440e4d679693a38caf3ff0910`. The new record was additive; no existing record was overwritten, replaced, deleted or reinterpreted.

## Governed-store accounting

| Store | Before | After | Delta | Before/after aggregate digest |
|---|---:|---:|---:|---|
| evidence | 29 | 29 | 0 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| evidence-source manifests | 29 | 29 | 0 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| corrected preparations | 6 | 6 | 0 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` |
| capability acceptances | 11 | 12 | **+1** | `7965a69748fe8d5009d631ea51372a33e60396d440e4d679693a38caf3ff0910` → `0089970e3bf20a29b845aa0a2820722e47bead49864831d9c0d7873545c18237` |
| execution authorizations | 11 | 11 | 0 | `f69eb1cfa4d4ff6438a55e4aa12beeb68a55cd4d5ece17c9fb4b09bb86f24939` |
| attempts | 8 | 8 | 0 | `842a2457b71df56d8265b1419a37ca2f4e986c483267afee735f0bf344d62e74` |
| provider state | 6 | 6 | 0 | `980472f44ba44324881755167e7546bfa1d5bb0589db0736c10ac5fcb65cbc5d` |
| derivative generations | 22 | 22 | 0 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` |
| derivative content | 20 | 20 | 0 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` |

Exactly the permitted capability-store mutation occurred.

## Deed, provider and production boundaries

Registered evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9` and corrected preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f` were not prepared, modified, authorized, executed, transmitted or transcribed.

* OpenAI calls: `0`;
* Claude calls: `0`;
* other external provider calls: `0`;
* retries: `0`;
* external evidence egress: `0`;
* provider-state delta: `0`.

Production remained on the same container, exact image/source and runtime JAR, with restart count zero, running state and readiness PASS. No deployment or restart occurred.
