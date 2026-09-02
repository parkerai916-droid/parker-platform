# OI11R6C — Exact Accepted R6-Converged Artifact Deployment and Production Verification

Date: 2026-09-02 UTC

## Verdict

**A — EXACT ACCEPTED R6-CONVERGED ARTIFACT DEPLOYED AND VERIFIED**

The exact owner-accepted OI11R6B artifact was deployed without rebuild or pull. Runtime identity, readiness, corrected-preparation readback, historical compatibility, fail-closed V8 authority and governed-store integrity all passed. No capability acceptance, evidence-specific authority, provider activity or evidence egress occurred.

## Starting repository state

The host was `parker`, repository `/home/steve/parker-platform`, branch `main`. Starting HEAD and upstream were both `7bb24a50bccb61242008e9c2dbc0ab25dc75444e`; the worktree was clean.

## Owner artifact-acceptance verification

The immutable deployment-only acceptance record was verified before production mutation:

* path: `/mnt/parker-data/parker/replacement-candidates/oi11r6b-artifact-acceptance-73c2fda4-v1.json`;
* SHA-256: `cb21cff0a45683f50583f9ddbcdc3f0a5faa1517fa7bddfc98187de73303fdcd`;
* size: `1,739 bytes`;
* permissions: `0600`, `steve:steve`;
* event: `oi11r6b-owner-artifact-acceptance-73c2fda4-v1`;
* accepted at: `2026-09-02T11:13:07Z`.

The record binds exact source, JAR, image/index, platform manifest, config, archive and unchanged V8 capability/digest. It authorizes deployment of this artifact only and explicitly excludes implementation-bound V8 acceptance, Deed/preparation modification, evidence-specific authority, provider execution/egress, retries, external reasoning and transcription execution.

## Artifact identity and recovery verification

The preserved archive remained exact:

| Identity | Exact value |
|---|---|
| Source | `39fe0e777608c96cba20cec491113e77eee4b8ef` |
| JAR SHA-256 | `d0f52b7d935f568c14d9c7012be43ae7f1ac8d755f522fa03ee34ffe9af530e7` |
| OCI image/index | `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406` |
| Linux/amd64 manifest | `sha256:4dd763cfafdfb8bbff188066caa009aab0c63525127a9c6f86deb2156b6bdf88` |
| OCI config | `sha256:91966886b573bcd581e75383aa9d3fbe6eac9c275f057a4bd713b19ab55b9375` |
| Archive | `/mnt/parker-data/parker/replacement-candidates/oi11r6b-r6-converged-39fe0e77-20260902.tar` |
| Archive SHA-256 | `adfbb08a01caedf7359133385d1f618b5913ea4f4bb419d9cec58263336325f4` |
| Archive size | `1,031,897,088 bytes` |
| Archive protection | `0600`, `steve:steve` |

The archive was loaded through local Docker tooling. It resolved to exact accepted index `73c2fda4...`; the index selected accepted platform manifest `4dd763cf...`, config `91966886...`, and source label `39fe0e77...`. A local immutable deployment tag was assigned to that exact loaded index. No image was rebuilt or pulled.

## Pre-deployment production baseline

Production matched the required baseline exactly:

* container: `8b7c4b9b9f1b374de278e37d2f01c8401bc8ab809516d21135ddebf1e8065d7c`;
* image/index: `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`;
* embedded source: `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`;
* started: `2026-09-02T08:57:38.782928781Z`;
* restart count: `0`;
* state: running;
* readiness: `PASS` (`overallReady=true`, empty reasons).

No unexpected baseline difference existed.

## Deployment configuration

The active override preimage was preserved as `/home/steve/.config/parker/docker-compose.fa-a1r.yml.oi11r6c-preimage`, SHA-256 `8e0756c3d9389995c0d56cdea0cd7ce8198c12fd4dd22beb1fd2d9fb74ffa1a4`.

Only four established immutable bindings changed:

* service image to `parker-oi11r6b-r6-converged-39fe0e77@sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406`;
* `PARKER_DEPLOYED_IMMUTABLE_IMAGE_ID` to the same exact index;
* `PARKER_SOURCE_COMMIT` to `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* `PARKER_PRODUCTION_COMMIT` to the same exact source.

The new override SHA-256 is `89f9ed40da1ff04b1af58bd0d8f6e35f943be488f2111e63fde6bb7c8a4109b5`. The final rendered Compose configuration SHA-256 is `021a1507b3cb813d7cc313b966ae98a6f131e17c906521383aa73e165483bbb1`. It resolves the exact artifact/source and retains `/mnt/parker-data/parker/corrected-preparations` mounted read/write at `/data/corrected-preparations`. Provider profile/model, Purpose, credentials, evidence configuration and unrelated Compose settings were unchanged.

## Exact deployment

Only the Parker runtime was recreated using the established Compose stack with exact `PARKER_BUILD_COMMIT`, `--no-build`, `--pull never`, `--no-deps` and `--force-recreate`. No unrelated service was restarted.

The resulting production identity is:

| Field | Exact value |
|---|---|
| Container | `9011f1e9e9feaa5d0662c295cc295c19b0dce45fb4e8c2a1dad95fbe12f52fa5` |
| Image/index | `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406` |
| Embedded source | `39fe0e777608c96cba20cec491113e77eee4b8ef` |
| Runtime JAR SHA-256 | `d0f52b7d935f568c14d9c7012be43ae7f1ac8d755f522fa03ee34ffe9af530e7` |
| Started | `2026-09-02T11:25:47.196217995Z` |
| Restart count | `0` |
| State | running |

## Readiness and corrected-preparation composition

Startup logged `Runtime starting`, `Runtime started`, and the authenticated owner HTTP listener. Repeated canonical readiness diagnostics returned every predicate true, `ordinaryExecutionReady=true`, `overallReady=true`, and no reasons.

The authenticated preparation-only route remains composed; an unauthenticated POST using non-evidence identifier `not-governed-evidence` returned HTTP 401 before custody or preparation handling. No Deed endpoint was invoked.

The corrected-preparation mount is reachable by the runtime. Canonical codec readback, executed inside the deployed runtime against the existing store, returned:

* preparation identity: `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`;
* region-set digest: `bc684a53cb20425580c80664658df3bd6d0515adcefcbe10e827faff87596e56`;
* region count: `5`.

This was read-only. The preparation was not recreated, modified, request-shaped or executed. Store construction and R6A verification retain canonical create-once/conflict behavior.

## Historical compatibility

Startup successfully composed existing evidence, manifest, corrected-preparation, capability, authorization, attempt, provider-state and derivative stores. Every complete sorted file inventory retained the exact pre-deployment count and aggregate digest after deployment. This proves no historical record was rewritten. The canonical corrected-preparation codec additionally read the existing five-region record exactly.

The artifact retains explicitly versioned authorization and derivative codecs: weaker historical authorizations remain readable but cannot satisfy the new exact-envelope requirement; historical derivative versions retain their recorded semantics; unknown versions remain fail-closed. Existing historical capability records were readable by the canonical evaluator.

## V8 identity and implementation-bound authority

The authenticated canonical production evaluator returned:

* capability: `ordinary-external-request-region-transcription-v8`;
* provider: `OpenAI`;
* model: `gpt-5.6-sol`;
* wire version: `8`;
* maximum regions: `32`;
* body maximum: `16,777,216 bytes`;
* batching: `false`;
* disposition: `CAPABILITY_NOT_ACCEPTED`;
* runtime embedded build: `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* accepted promoting build: `null`.

The capability digest remains `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, as verified in the exact accepted artifact. No record exists or was created for this new implementation. The expected fail-closed authority gate therefore passed.

## Governed-store accounting

Counts and aggregate content digests were identical before and after deployment:

| Store | Count | Aggregate digest | Delta |
|---|---:|---|---:|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` | 0 |
| evidence-source manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` | 0 |
| corrected preparations | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` | 0 |
| capability acceptances | 11 | `7965a69748fe8d5009d631ea51372a33e60396d440e4d679693a38caf3ff0910` | 0 |
| execution authorizations | 11 | `f69eb1cfa4d4ff6438a55e4aa12beeb68a55cd4d5ece17c9fb4b09bb86f24939` | 0 |
| attempts | 8 | `842a2457b71df56d8265b1419a37ca2f4e986c483267afee735f0bf344d62e74` | 0 |
| provider state | 6 | `980472f44ba44324881755167e7546bfa1d5bb0589db0736c10ac5fcb65cbc5d` | 0 |
| derivative generations | 22 | `28626778eec53df921b16063635393cd18c6e390ed0df5c321a30bacdf41f322` | 0 |
| derivative content | 20 | `4952e366cae0633922be9b6dcd1204e57d2e3956336c4e5a4d03d3be06ab158e` | 0 |

## Deed, provider and egress boundaries

Registered evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9` was not prepared, modified, authorized, request-shaped, attempted, transmitted or transcribed. Its persisted corrected preparation was only canonically read by identity/metadata for compatibility verification and remained byte-stable.

* OpenAI calls: `0`;
* Claude calls: `0`;
* other external provider calls: `0`;
* retries: `0`;
* external evidence egress: `0`;
* provider-state delta: `0`.

## Final production state

Production runs exact accepted index `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406`, source `39fe0e777608c96cba20cec491113e77eee4b8ef`, with restart count zero and readiness PASS. The corrected-preparation store is composed and readable. Historical state is immutable. V8 identity/digest are unchanged and its evaluator correctly returns `CAPABILITY_NOT_ACCEPTED`. Further progress requires separate explicit owner implementation-bound V8 acceptance.
