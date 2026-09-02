# OI11R6G — Exact Accepted Post-Egress Continuation Artifact Deployment and Production Verification

## Verdict

**A — EXACT ACCEPTED POST-EGRESS CONTINUATION ARTIFACT DEPLOYED AND VERIFIED**

The exact owner-accepted OI11R6F artifact was deployed without rebuild, pull, tag substitution, continuation invocation, derivative admission or provider activity. Runtime identity, readiness, provider-free continuation composition, historical R6-R1 readback, fail-closed V8 authority and governed-store integrity all passed.

## Starting repository state

The repository was on branch `main`; HEAD and upstream were both `72a2002eb1b8a455e111386d59ead556fe21dd57`; the worktree was clean. No executable source was changed in this unit.

## Owner artifact-acceptance verification

The canonical acceptance record `/mnt/parker-data/parker/replacement-candidates/oi11r6f-artifact-acceptance-a5f56508-v1.json` was verified as:

| Field | Exact value |
|---|---|
| SHA-256 | `58d3156cc28507e2403099b4dfc6a1829ba5cf6c18df3c5f8f5a535a39d13828` |
| Size | `2,892 bytes` |
| Protection | `0600`, `steve:steve` |
| Event | `oi11r6f-owner-artifact-acceptance-a5f56508-v1` |
| Type | `exact-production-artifact-deployment-only` |
| Accepted at | `2026-09-02T12:41:37Z` |

The record binds the exact source, JAR, OCI index, platform manifest, config and archive below, together with the unchanged V8 capability identity/digest. It grants deployment of this exact artifact only. Its exclusions expressly cover implementation-bound capability acceptance, continuation, derivative admission, new authorization/execution/attempt state, provider activity, retry, egress, external reasoning and mutation of the Deed, corrected preparation or preserved R6-R1 state.

## Artifact identity and recoverability

| Field | Exact value |
|---|---|
| Executable source | `e3fec3fac857e7b0e610375d066d524646a1375f` |
| JAR SHA-256 | `f4971f223612a1791ca6a013bc3234e03d85b9cfbb7fecc09886b40983861d63` |
| OCI image/index | `sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089` |
| Linux/amd64 manifest | `sha256:3d6655e179e6cd0a639a93d49c3d3b09aa215676035d7afd79dfadf14eaa7b54` |
| OCI config | `sha256:03672bc9966ede6940ee200dfa8e2f2c9016c95a78b818f7a6ddde420d52e8da` |
| Archive | `/mnt/parker-data/parker/replacement-candidates/oi11r6f-post-egress-continuation-e3fec3fa-20260903.tar` |
| Archive SHA-256 | `c0e89bb2f71315a816dac4081f6bfb630f8848d975a3ab7c0cc8f4c200ff2527` |
| Archive size/protection | `1,031,915,008 bytes`; `0600`, `steve:steve` |

Direct OCI archive inspection proved index `a5f56508...` selects platform manifest `3d6655e1...`, which selects config `03672bc9...`; the config reports Linux/amd64 and embedded revision `e3fec3fa...`. The locally loaded immutable image resolved to the same index. Recoverability therefore remains **PASS**. No build or network pull occurred.

## Pre-deployment production baseline

Production exactly matched the required baseline: container `9011f1e9e9feaa5d0662c295cc295c19b0dce45fb4e8c2a1dad95fbe12f52fa5`, index `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406`, embedded source `39fe0e777608c96cba20cec491113e77eee4b8ef`, runtime JAR `d0f52b7d935f568c14d9c7012be43ae7f1ac8d755f522fa03ee34ffe9af530e7`, restart count zero and readiness PASS.

## Preserved R6-R1 historical state

Before deployment, the preserved state remained exact:

* authorization `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97`;
* execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`;
* provider state `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`;
* raw response SHA-256 `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`;
* authoritative response ID `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b` and model `gpt-5.6-sol`;
* V8 assessment `SUCCESS`, five blocks in Parker page order `[1,2,3,4,5]`;
* historical OpenAI calls `1`, retries `0`, derivative not admitted.

The authorization record hash was `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544`; its event record hash was `64070a0d042373164902615edd03a19a4bdf7f602e911d7eecbcec8c31bcb675`; the execution ledger hash was `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591`; provider-state and assessment file hashes were `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c` and `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01`. All remained identical after deployment.

## Deployment configuration

The active override preimage was preserved at `/home/steve/.config/parker/docker-compose.fa-a1r.yml.oi11r6g-preimage`, SHA-256 `89f9ed40da1ff04b1af58bd0d8f6e35f943be488f2111e63fde6bb7c8a4109b5`.

Only four established immutable bindings changed:

* service image to `parker-oi11r6f-post-egress-continuation-e3fec3fa@sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089`;
* `PARKER_DEPLOYED_IMMUTABLE_IMAGE_ID` to that index;
* `PARKER_SOURCE_COMMIT` to `e3fec3fac857e7b0e610375d066d524646a1375f`;
* `PARKER_PRODUCTION_COMMIT` to the same source.

The override SHA-256 is `b49eedf8434536a0f65152313d74718187780ca5cdd01f5da63ff9d2a6ecae4f`. The rendered Compose SHA-256 is `10930c4f70370da08285510e84f9ffff1ad526ef4768fc105aae26b048d8d7c7`; it retains `/mnt/parker-data/parker/corrected-preparations` mounted at `/data/corrected-preparations`. Provider, model, Purpose, credentials, stores and unrelated configuration were unchanged.

## Deployed production identity and readiness

Only service `parker` was recreated with `--no-build --pull never --no-deps --force-recreate`. The resulting identity is:

| Field | Exact value |
|---|---|
| Container | `f2a9df159d4305528ff89ac26e2dcc8f5e51fc969839f0d91f204976cb6ed542` |
| Image/index | `sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089` |
| Embedded source | `e3fec3fac857e7b0e610375d066d524646a1375f` |
| Runtime JAR | `f4971f223612a1791ca6a013bc3234e03d85b9cfbb7fecc09886b40983861d63` |
| Started | `2026-09-02T12:55:25.243280806Z` |
| Restart count | `0` |

Startup completed normally. The canonical non-egress diagnostic returned every predicate true, including build identity, stores, provider-profile readiness and credential structure; `ordinaryExecutionReady=true`, `overallReady=true`, and reasons were empty. Readiness is **PASS**.

## Continuation composition and provider prohibition

The deployed exact JAR contains the authenticated owner/admin `RegionTranscriptionContinuationHandler`; exact-source production composition routes it to `continueOrdinaryRegionPostEgressAsOwner`. Static inspection verifies the operation accepts only evidence, authorization, execution and provider-state identities, reads the exact authorization and persisted preparation/provider state, revalidates all bindings, deterministically recovers the prior V8 result and performs create-once derivative admission. It has no transport invocation and its response explicitly reports `providerInvoked=false`.

Eligibility remains fail closed for missing/corrupt state, wrong authorization format, evidence/execution/provider-state mismatch, preparation/request mismatch, non-successful validation or derivative conflict. It does not create an attempt, reset the durable one-call budget or alter the zero-retry boundary. The continuation endpoint was **not invoked** in this unit.

## Profile semantic verification

The deployed exact implementation retains separate canonical meanings:

* acquisition/request-region processing profile: `request-region-fidelity-acquisition-v4`;
* corrected-preparation profile: `full-page-achromatic-png-preparation-v1`;
* external provider profile: `openai-fidelity-first-transcription-v1`.

Derivative construction calls the authorization-bound provider-profile resolver and separately retains preparation and acquisition processing provenance. Contradictory authorization/provider facts remain fail closed. No profile field was mutated.

## Historical readback and derivative non-admission

Canonical startup/readback accepted the existing versioned authorization, execution ledger, raw provider state and assessment. Metadata-only assessment readback reproduced the exact raw digest, response ID/model, `SUCCESS`, five blocks and order `[1,2,3,4,5]`. A complete search of derivative generation/content stores found zero records bound to execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976` before and after deployment. The historical R6-R1 failure remains immutable and no derivative was admitted.

## V8 identity and implementation-bound evaluator

The capability remains `ordinary-external-request-region-transcription-v8`, digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`.

The authenticated canonical evaluator reported runtime source `e3fec3fac857e7b0e610375d066d524646a1375f`, accepted promoting build `null`, and disposition `CAPABILITY_NOT_ACCEPTED`. This is the required fail-closed state: no implementation-bound record for the new source was created.

## Governed-store accounting

Complete recursive inventories were identical before and after deployment:

| Store | Count | Aggregate digest | Delta |
|---|---:|---|---:|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` | 0 |
| evidence manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` | 0 |
| corrected preparations | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` | 0 |
| capability acceptances | 12 | `0089970e3bf20a29b845aa0a2820722e47bead49864831d9c0d7873545c18237` | 0 |
| execution authorizations | 14 | `b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6` | 0 |
| attempts | 10 | `798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f` | 0 |
| provider state | 8 | `be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6` | 0 |
| derivative generations | 22 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` | 0 |
| derivative content | 20 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` | 0 |
| evidence audit | 1 | `1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59` | 0 |
| document-ingestion audit | 1 | `9349a5cd71c6efbefa1524115900c0769c13e464f08f4bbd5b41f3ac3c42d72e` | 0 |

No historical record hash changed. Deployment metadata/configuration is not governed evidence mutation.

## Provider accounting and final production state

OI11R6G activity was OpenAI `0`, Claude `0`, other providers `0`, retries `0`, external evidence egress `0`, provider-state delta `0`. The historical R6-R1 OpenAI call remains exactly one; no retry is authorized.

Production ends on exact index `a5f56508...`, source `e3fec3fa...`, JAR `f4971f22...`, restart count zero and readiness PASS. Continuation is composed and provider-free; R6-R1 state is readable and unchanged; its derivative is not admitted; V8 identity/digest are unchanged; the evaluator is `CAPABILITY_NOT_ACCEPTED`; governed-store delta and provider activity are zero.

Further progress requires a separate explicit owner decision before implementation-bound V8 authority may be created for source `e3fec3fac857e7b0e610375d066d524646a1375f`.
