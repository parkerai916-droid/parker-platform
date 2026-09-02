# OI11R5L — V8 Implementation-Bound Production Capability Acceptance

## Disposition and owner authority

Steven explicitly accepted production execution authority for unchanged capability `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, on exact implementation `fe13047df0dd5f155d6a6921acf7bc85541af26f` and image/index `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f`. Parker canonically recorded that implementation-bound authority. This acceptance contains no evidence-specific, provider-call, retry, transcription or egress authority.

## Starting repository and production identity

Repository branch was `main`; HEAD and upstream were both exactly `3763b7298183fc55aa8a432b12c2a0a795ad4070`; the worktree was clean and `git diff --check` passed.

Before acceptance, production was exactly:

| Identity | Exact value |
|---|---|
| Container | `eb5a5bcf74ec26fe09bbb59b9a10a9eb0fd92d02a92cdb5812a18c35c3a4dd0f` |
| Image/index | `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f` |
| Source/build | `fe13047df0dd5f155d6a6921acf7bc85541af26f` |
| JAR SHA-256 | `fd259f70b58843a2bee8955edb98a767b89b0307643d8fe57eb155f22448fe89` |
| Started | `2026-09-01T13:23:54.513307926Z` |
| Restart count | 0 |

The non-egress runtime diagnostic returned every predicate true, including exact build-identity match, readable required stores, `ordinaryExecutionReady=true`, `overallReady=true`, and empty reasons.

## Artifact acceptance verification

The canonical R5K deployment-only artifact acceptance remained `/mnt/parker-data/parker/replacement-candidates/oi11r5k-artifact-acceptance-dea81e5d-v1.json`, SHA-256 `7d2f2e4e5f49068e28ac2f1bf04fa63f477a4bb3cd62f4b7cc9ed51da2c3b8cb`. Its source, JAR, OCI index, platform manifest, config, archive and preparation-profile fields bind the exact running artifact. Its excluded-authority list correctly excluded implementation-bound V8 authority until this separate owner decision.

## V8 identity and pre-acceptance fail-closed proof

The authenticated, read-only canonical evaluator (`GET /owner/admin/region-capability-acceptance`) returned:

```json
{"capabilityId":"ordinary-external-request-region-transcription-v8","provider":"OpenAI","operation":"POST /v1/responses","model":"gpt-5.6-sol","adapterId":"openai-responses-request-region-transcription-adapter","adapterVersion":"7.0.0","profile":"request-region-fidelity-acquisition-v4","wireVersion":8,"mediaType":"application/pdf","maximumRegions":32,"aggregateRequestBodyMaximumBytes":16777216,"batching":false,"disposition":"CAPABILITY_NOT_ACCEPTED","runtimeEmbeddedBuildCommit":"fe13047df0dd5f155d6a6921acf7bc85541af26f","acceptedPromotingBuildCommit":null}
```

This runtime diagnostic independently established the unchanged V8 transport semantics exposed by the exact artifact and the required fail-closed state. R5I changed preparation and provenance only; it did not alter the accepted V8 capability identity/digest or request/response semantics.

## Historical acceptance inventory

Before the operation the capability store contained nine records: six immutable legacy `.region-capability-acceptance-v2` records and three immutable V8 records for C304, D335 and `9e2a900…`. Their exact pre/post SHA-256 values were unchanged:

| Record ID | SHA-256 |
|---|---|
| `1c08c6e5…` | `413c8a888724d2a35d245fdb82a2d09921791acd0f2492c56645d17ddd093ce8` |
| `1ef37f99…` | `58f803531959888dc24397ad62ed6b64d9ed0997e0ca1ef7785c20e3bfe410f3` |
| `22610872…` | `5514bc04430659c724055c601c4bc6589a0b640d79f93fdf2757696fed499b60` |
| `5303c27e…` | `2830d939048196f5918bb6cc4bad3140cb5a5390958bb1aebb3c2d45604345af` |
| `d26cc256…` | `6592794a85e37b88507afb1df76e5fa29010ca9ab89761a49fe1a0ad4ccbad42` |
| `e29b1097…` | `58a36c9c1de8087ea99cd6877a6c202e502a591630d8d0826616ff3e1b4b5d90` |
| V8 `85af5c7b…` (D335) | `b3ec36185e40a1a5940726d1311e6d5351890bb4d14791b97f218ae10f61aca5` |
| V8 `c3efa482…` (C304) | `2704d826896002c077c1eeb8d323c100d313546ef5a477eacb73deb0d15eb03e` |
| V8 `9fe94406…` (`9e2a900…`) | `943cf8abf75597b0507d4fa5eaf3679eb558964a76ffde0911e37c156b8d11af` |

No historical record was rewritten, superseded or deleted.

## Canonical acceptance operation and record

The authenticated localhost-only canonical route received exactly:

```json
{"capabilityId":"ordinary-external-request-region-transcription-v8","promotingBuildCommit":"fe13047df0dd5f155d6a6921acf7bc85541af26f"}
```

The canonical route accepts no evidence ID, provider authorization, provider execution, caller-supplied digest override or external-reasoning authority. Its coordinator reconstructs the frozen V8 identity and digest. It returned `CREATED` exactly once:

| Field | Exact value |
|---|---|
| Record ID/digest | `ab14b21a53d1e2ddccb7c0dfbacb0189e6ec6ccbde46c1384467f58fdf062d76` |
| Path | `/data/region-transcription-capability-acceptances/ab14b21a53d1e2ddccb7c0dfbacb0189e6ec6ccbde46c1384467f58fdf062d76.request-region-v8-capability-acceptance-v1` |
| File SHA-256 | `4291d3e429274712b343500d5538661b0dcd0f7d42b539e57efaa5bb3edeaa3a` |
| Capability | `ordinary-external-request-region-transcription-v8` |
| Capability digest | `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0` |
| Implementation | `fe13047df0dd5f155d6a6921acf7bc85541af26f` |
| Accepted by | `user.steve` |
| Accepted at | `2026-09-02T07:18:13.318904669Z` |
| Governed evidence ID | `acceptance-evidence-ordinary-ingestion-10r7-v8-fidelity` |
| Governed evidence SHA-256 | `34ec3c703aacb754c45fa58ddf941d7368e2b4cc2e373cb412eb99c4de30902b` |

The record is additional immutable history, not a mutation of an earlier acceptance.

## Post-acceptance evaluator

The same canonical authenticated GET returned:

```json
{"capabilityId":"ordinary-external-request-region-transcription-v8","provider":"OpenAI","operation":"POST /v1/responses","model":"gpt-5.6-sol","adapterId":"openai-responses-request-region-transcription-adapter","adapterVersion":"7.0.0","profile":"request-region-fidelity-acquisition-v4","wireVersion":8,"mediaType":"application/pdf","maximumRegions":32,"aggregateRequestBodyMaximumBytes":16777216,"batching":false,"disposition":"ACCEPTED","runtimeEmbeddedBuildCommit":"fe13047df0dd5f155d6a6921acf7bc85541af26f","acceptedPromotingBuildCommit":"fe13047df0dd5f155d6a6921acf7bc85541af26f"}
```

This is capability authority only. Provider readiness is not evidence-specific authorization, and no such authorization was created.

## Store integrity and Deed boundary

| Store | Before | After | Delta |
|---|---:|---:|---:|
| evidence | 29 | 29 | 0 |
| evidence source manifests | 29 | 29 | 0 |
| capability acceptances | 9 | 10 | +1 |
| owner authorizations | 11 | 11 | 0 |
| attempts | 8 | 8 | 0 |
| provider state/assessment | 6 | 6 | 0 |
| derivative generations | 22 | 22 | 0 |
| derivative content | 20 | 20 | 0 |
| region acceptance authority | 1 | 1 | 0 |
| corrected Deed preparations | 0 | 0 | 0 |

The only production mutation was the one authorized capability record. Registered evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, was not prepared, rendered through the new production path, shaped into a V8 request, authorized, executed or transmitted. No grayscale derivative, evidence-specific authority, attempt, provider state, transcription or derivative was created.

## Stability, provider accounting and stop state

Production remained the same running container, image, source and start time with restart count 0. Repeated runtime readiness remained PASS, the canonical evaluator remained `ACCEPTED`, stores remained readable, and no background execution appeared in logs or stores. No deployment, rebuild or restart occurred.

OpenAI calls: 0. Claude calls: 0. Other external calls: 0. Retries: 0. External egress: 0.

Further progress requires a separately scoped governed production preparation of the registered Deed. This unit supplies no permission for provider execution.

UNIT ORDINARY-INGESTION-11R5L COMPLETE — THE OWNER HAS ACCEPTED AND PARKER HAS CANONICALLY RECORDED IMPLEMENTATION-BOUND PRODUCTION EXECUTION AUTHORITY FOR THE UNCHANGED ORDINARY-EXTERNAL-REQUEST-REGION-TRANSCRIPTION-V8 CAPABILITY AND DIGEST ON EXACT IMPLEMENTATION FE13047DF0DD5F155D6A6921ACF7BC85541AF26F. THE CANONICAL EVALUATOR RETURNS ACCEPTED. ALL HISTORICAL CAPABILITY ACCEPTANCE RECORDS REMAIN IMMUTABLE, PRODUCTION REMAINS ON THE EXACT OWNER-ACCEPTED ARTIFACT WITH RESTART COUNT ZERO, AND NO EVIDENCE-SPECIFIC AUTHORITY HAS BEEN CREATED. THE REGISTERED DEED HAS NOT BEEN PREPARED OR TRANSMITTED. NO PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. FURTHER PROGRESS REQUIRES A SEPARATELY SCOPED GOVERNED PRODUCTION PREPARATION OF THE REGISTERED DEED BEFORE ANY PROVIDER EXECUTION MAY BE CONSIDERED.
