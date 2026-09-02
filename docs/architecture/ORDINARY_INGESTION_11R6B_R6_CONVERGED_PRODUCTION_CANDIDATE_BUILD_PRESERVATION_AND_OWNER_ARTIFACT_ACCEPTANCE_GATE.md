# OI11R6B — R6-Converged Production Candidate Build, Preservation and Owner Artifact-Acceptance Gate

Date: 2026-09-02 UTC

## Verdict

**A — R6-CONVERGED PRODUCTION CANDIDATE BUILT AND PRESERVED; OWNER ACCEPTANCE REQUIRED**

This unit built, verified and preserved an exact production candidate from the accepted OI11R6A implementation commit. It did not accept or deploy the candidate, restart production, process the registered Deed, create execution authority, or contact a provider.

## Starting repository state

Before the build:

* host: `parker`;
* repository: `/home/steve/parker-platform`;
* branch: `main`;
* HEAD: `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* upstream: `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* worktree: clean.

The executable candidate contains no commit later than `39fe0e777608c96cba20cec491113e77eee4b8ef`. The documentation commit created by this unit is not part of the executable artifact.

## R6A verification

The authoritative report `ORDINARY_INGESTION_11R6A_PERSISTED_PREPARATION_EXECUTION_BINDING_EXACT_AUTHORIZATION_ENVELOPE_AND_R6_PATH_CONVERGENCE.md` was present with SHA-256:

`9b2c9f49945ca723b64b7f45ecf41ff4cdbdd70d0f190cd75bd5791d3710ae5c`

The report records that persisted corrected preparation is the execution input; the version-2 authorization envelope and execution-time revalidation bind all required security fields; durable one-call and zero-retry enforcement are active; historical codecs remain compatible; raw-before-parse is preserved; the synthetic five-region fake-provider path and derivative provenance passed; and the V8 identity/digest are unchanged. Its focused result was 3 suites, 24 tests, 0 failures, 0 errors and 3 skipped. Its full result was 251 suites, 3,316 tests, 0 failures, 0 errors and 17 skipped. Production-store delta and provider activity were zero.

## Production build and JAR identity

The exact build command was:

```text
PARKER_BUILD_COMMIT=39fe0e777608c96cba20cec491113e77eee4b8ef ./gradlew clean installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g
```

It completed successfully using the established bounded 4 GiB Gradle/Kotlin heap.

* JAR: `build/libs/parker-platform-0.8.0-runtime-complete.jar`
* size: `4,633,615 bytes`
* SHA-256: `d0f52b7d935f568c14d9c7012be43ae7f1ac8d755f522fa03ee34ffe9af530e7`
* embedded `Parker-Source-Commit`: `39fe0e777608c96cba20cec491113e77eee4b8ef`

The installed-distribution JAR had the same size and SHA-256.

## OCI build and identities

The established production image layout was reproduced offline from the exact currently accepted Parker base image and the exact new installation distribution. The governed OCI creation label was fixed to `2026-09-02T00:00:00Z`; the archive index recorded build completion at `2026-09-02T10:57:45Z`.

The successful build used BuildKit with `--network=none`, `--pull=false`, platform `linux/amd64`, OCI output and provenance attestation. Before that successful build, two bounded build-mechanics attempts produced no candidate: a digest-qualified `FROM` caused a denied registry metadata lookup, and a repository-root context omitted ignored build output. No evidence or provider material was involved; the corrected isolated context then built entirely without network access.

Exact identities:

* OCI image/index: `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406`
* platform manifest: `sha256:4dd763cfafdfb8bbff188066caa009aab0c63525127a9c6f86deb2156b6bdf88`
* OCI config: `sha256:91966886b573bcd581e75383aa9d3fbe6eac9c275f057a4bd713b19ab55b9375`
* provenance attestation manifest: `sha256:6042140b565930aa2289e44f62b9629e8d93f10ff4a13bd836447b1a5685322f`
* platform: `linux/amd64`
* OCI revision/source label: `39fe0e777608c96cba20cec491113e77eee4b8ef`
* entry point: `/opt/parker/bin/parker`

The JAR read directly inside the locally loaded candidate had SHA-256 `d0f52b7d935f568c14d9c7012be43ae7f1ac8d755f522fa03ee34ffe9af530e7`, exactly matching the production build output.

## Artifact-local semantic verification

Package inspection of the exact candidate JAR established the governed corrected-preparation service/store classes, persisted-preparation request preparer, versioned exact authorization records/store, authorization guard, attempt/provider-state stores, V8 execution workflow and derivative codecs. Source and bytecode inspection confirmed the R6A-bound preparation, request, Purpose, provider/profile/model, call/retry, raw-state and derivative fields are present in the artifact built above.

A runtime invocation against that exact build returned:

* capability: `ordinary-external-request-region-transcription-v8`;
* digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`.

The artifact also retains `full-page-achromatic-png-preparation-v1`. No production configuration or governed store was supplied to or mutated by artifact-local inspection, and no real provider adapter was called. The candidate has no implementation-bound production V8 acceptance yet.

## Archive preservation and recoverability

The exact OCI candidate was preserved as:

`/mnt/parker-data/parker/replacement-candidates/oi11r6b-r6-converged-39fe0e77-20260902.tar`

* archive SHA-256: `adfbb08a01caedf7359133385d1f618b5913ea4f4bb419d9cec58263336325f4`
* archive size: `1,031,897,088 bytes`
* permissions: `0600`
* owner/group: `steve:steve`

Recovery inspection rehashed the referenced blobs and reproduced the exact index `73c2fda4...`, platform manifest `4dd763cf...`, config `91966886...`, `linux/amd64` platform, embedded revision and entry point. A local load of the platform image reproduced the same platform manifest/config and exact embedded JAR. Archive recoverability: **PASS**. The recovered image was not deployed.

## Production baseline and store accounting

Production was identical before and after R6B:

* container: `8b7c4b9b9f1b374de278e37d2f01c8401bc8ab809516d21135ddebf1e8065d7c`;
* image/index: `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`;
* embedded source: `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`;
* restart count: `0`;
* state: running;
* readiness: `PASS` (`overallReady=true`, no reasons).

Recursive governed-store file counts were unchanged:

| Store | Before | After | Delta |
|---|---:|---:|---:|
| evidence | 29 | 29 | 0 |
| evidence-source manifests | 29 | 29 | 0 |
| corrected preparations | 6 | 6 | 0 |
| capability acceptances | 11 | 11 | 0 |
| execution authorizations | 11 | 11 | 0 |
| attempts | 8 | 8 | 0 |
| provider state | 6 | 6 | 0 |
| derivative generations | 22 | 22 | 0 |
| derivative content | 20 | 20 | 0 |

The replacement-candidate archive is artifact preservation, not governed evidence mutation.

## Registered-Deed and provider boundaries

The registered evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9` and corrected preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f` were not read through an execution path, recreated, modified, authorized, transmitted or transcribed.

Provider accounting:

* OpenAI calls: `0`;
* Claude calls: `0`;
* other external provider calls: `0`;
* retries: `0`;
* external evidence egress: `0`;
* production provider-state delta: `0`.

## Exact owner artifact-acceptance package

**OWNER ARTIFACT ACCEPTANCE REQUIRED**

* Source: `39fe0e777608c96cba20cec491113e77eee4b8ef`
* JAR SHA-256: `d0f52b7d935f568c14d9c7012be43ae7f1ac8d755f522fa03ee34ffe9af530e7`
* OCI image/index: `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406`
* platform manifest: `sha256:4dd763cfafdfb8bbff188066caa009aab0c63525127a9c6f86deb2156b6bdf88`
* OCI config: `sha256:91966886b573bcd581e75383aa9d3fbe6eac9c275f057a4bd713b19ab55b9375`
* archive: `/mnt/parker-data/parker/replacement-candidates/oi11r6b-r6-converged-39fe0e77-20260902.tar`
* archive SHA-256: `adfbb08a01caedf7359133385d1f618b5913ea4f4bb419d9cec58263336325f4`
* archive size: `1,031,897,088 bytes`
* archive protection: `0600`, `steve:steve`
* V8 capability: `ordinary-external-request-region-transcription-v8`
* V8 digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`
* embedded source exact: **PASS**
* archive recoverability: **PASS**
* production unchanged: **PASS**
* provider activity: `0`

The candidate has not been accepted or deployed. Artifact acceptance, exact deployment, implementation-bound V8 production acceptance and any real-document execution remain separate later owner gates.
