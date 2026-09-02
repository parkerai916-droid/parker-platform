# OI11R6F — Post-Egress Continuation Production Candidate Build, Preservation and Owner Artifact-Acceptance Gate

Date: 2026-09-02 UTC

## Verdict

**A — POST-EGRESS CONTINUATION PRODUCTION CANDIDATE BUILT AND PRESERVED; OWNER ACCEPTANCE REQUIRED**

An exact production candidate was built from OI11R6E source, inspected semantically, replayed offline against a complete read-only copy of R6-R1 state, preserved, reloaded and verified. It was not accepted or deployed. No production continuation, derivative admission or provider activity occurred.

## Starting state and R6E verification

The repository gate passed:

* host `parker`;
* repository `/home/steve/parker-platform`;
* branch `main`;
* HEAD/upstream `e3fec3fac857e7b0e610375d066d524646a1375f`;
* worktree clean.

The authoritative R6E report existed at the required path with SHA-256 `98adc38928f4b29d44f2bf847bb37cbe6fcc68c77f6827a597b4d4d6b2d82481`. It records the distinct acquisition/preparation/provider profile semantics, authorization-bound provider-profile correction, durable zero-call continuation, fail-closed eligibility, create-once idempotence, exact R6-R1 forensic replay, V8 5/5 validation and ordering, unchanged V8 identity/digest, no known remaining structural blocker, 251 suites / 3,321 tests PASS, production delta zero and provider activity zero.

## Preserved R6-R1 historical state

Production history remained immutable:

* authorization `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97`, file SHA-256 `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544`;
* execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`, ledger SHA-256 `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591`;
* provider state `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`, raw-record SHA-256 `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c`;
* assessment SHA-256 `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01`;
* raw response SHA-256 `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`;
* response ID `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`;
* model `gpt-5.6-sol`;
* historical verdict POST-EGRESS FAILURE; derivative not admitted;
* historical OpenAI calls 1, retries 0.

These hashes were equal before and after R6F. The historical result was not reclassified.

## Exact source and production JAR

The executable candidate contains only source commit `e3fec3fac857e7b0e610375d066d524646a1375f`. The established build command was:

```text
PARKER_BUILD_COMMIT=e3fec3fac857e7b0e610375d066d524646a1375f ./gradlew clean installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g
```

It passed with the bounded 4 GiB heap.

* JAR path: `build/libs/parker-platform-0.8.0-runtime-complete.jar`
* candidate JAR size: `4,652,508 bytes`
* candidate JAR SHA-256: `f4971f223612a1791ca6a013bc3234e03d85b9cfbb7fecc09886b40983861d63`
* embedded manifest source: `e3fec3fac857e7b0e610375d066d524646a1375f`

The installed-distribution JAR copied into the isolated OCI context was byte-identical. Recovery extraction from the final OCI candidate reproduced the same size, SHA-256 and embedded source.

## OCI build and identities

The established R6B offline image procedure was reused. A private isolated context contained the exact installed distribution and repository tools. The already-local accepted Parker runtime base `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` supplied runtime dependencies. BuildKit ran with `--network=none`, `--pull=false`, platform `linux/amd64`, OCI output and provenance attestation. It did not rebuild source inside Docker or contact a registry.

* OCI image/index: `sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089`
* platform manifest: `sha256:3d6655e179e6cd0a639a93d49c3d3b09aa215676035d7afd79dfadf14eaa7b54`
* OCI config: `sha256:03672bc9966ede6940ee200dfa8e2f2c9016c95a78b818f7a6ddde420d52e8da`
* attestation manifest: `sha256:d57d265f69d3d64f90f9ef7da043d8b7008a7c1d0ceb656d2346eb76c609ce41`
* platform: `linux/amd64`
* OCI revision: `e3fec3fac857e7b0e610375d066d524646a1375f`
* governed creation label: `2026-09-03T00:00:00Z`
* OCI config creation: `2026-09-02T12:24:32.046009095Z`
* archive index completion annotation: `2026-09-02T12:24:34Z`
* entry point: `/opt/parker/bin/parker`
* runtime user: `parker`

## Artifact-local semantic and provider-prohibition verification

The recovered candidate JAR contains `RegionTranscriptionContinuationHandler`, exact authorization/store codecs, persisted preparation/provider-state codecs, the recovery-only coordinator, V8 workflow, versioned derivative codec and create-once derivative admission.

Bytecode inspection established:

1. `requestRegionV8DerivativeProviderProfile` reads the exact v2 authorization's `providerProfile` and requires `openai-fidelity-first-transcription-v1`;
2. acquisition profile `request-region-fidelity-acquisition-v4` remains a separate capability/processing value;
3. preparation profile `full-page-achromatic-png-preparation-v1` remains separately authorization/preparation-bound;
4. `continuePostEgress` revalidates current capability acceptance, custody, exact authorization/execution, preparation/request envelope and exact provider-state identity;
5. `recoverPersistedPostEgress` performs only binding checks, canonical provider-state readback, durable attempt-ledger verification and deterministic recovery;
6. that recovery method has no bytecode call to `durablyStartProviderAttempt`, `transportAfterGuardRelease`, provider exchange, OpenAI, Claude or another external adapter;
7. admission remains create-once/idempotent and rejects conflicting generation/content state;
8. historical codec classes remain present and unknown/invalid versions retain fail-closed handling.

An artifact-local runtime probe returned:

* capability `ordinary-external-request-region-transcription-v8`;
* digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`.

No implementation-bound capability acceptance for `e3fec3f...` was created.

## Exact R6-R1 offline compatibility

A complete read-only `/tmp` copy was assembled from the persisted R6-R1 authorization, attempt ledger, provider state/assessment, corrected-preparation record and all five immutable transport PNGs. The property selecting that fixture was explicitly inherited by the Gradle test worker and `cleanTest` forced execution, preventing an up-to-date or assumption skip.

The focused convergence class executed 16 tests, zero skipped, zero failures and zero errors. The exact copied-state test proved:

* exact authorization and reserved execution valid;
* persisted preparation and request identities valid;
* raw provider state/assessment canonical and intact;
* deterministic parse and V8 validation 5/5;
* provider `OpenAI`;
* provider profile `openai-fidelity-first-transcription-v1`;
* model `gpt-5.6-sol`;
* response ID exact;
* Parker order `[1,2,3,4,5]`;
* derivative binding/provenance valid through the local pre-admission boundary;
* provider callback count `0`.

No production derivative was written. No provider was available to or invoked by this replay.

## Archive preservation and recoverability

The exact candidate is preserved at:

`/mnt/parker-data/parker/replacement-candidates/oi11r6f-post-egress-continuation-e3fec3fa-20260903.tar`

* archive SHA-256: `c0e89bb2f71315a816dac4081f6bfb630f8848d975a3ab7c0cc8f4c200ff2527`
* archive size: `1,031,915,008 bytes`
* permissions: `0600`
* owner/group: `steve:steve`

The hash reproduced in the final verification. OCI index, nested platform manifest, config and platform were inspected from the archive. `docker load` recovered exact image/index `sha256:a5f565...`; local inspection reproduced its labels/platform/entry point, and extraction from a never-started temporary container reproduced exact candidate JAR `f4971f...`. Archive recoverability: **PASS**. The image was not deployed or started as production.

## Production baseline and governed-store accounting

Production was identical before and after R6F:

* container `9011f1e9e9feaa5d0662c295cc295c19b0dce45fb4e8c2a1dad95fbe12f52fa5`;
* image/index `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406`;
* embedded source `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* runtime JAR `d0f52b7d935f568c14d9c7012be43ae7f1ac8d755f522fa03ee34ffe9af530e7`;
* state running, restart count 0, readiness PASS.

| Store | Before | After | Delta |
|---|---:|---:|---:|
| evidence | 29 | 29 | 0 |
| evidence-source manifests | 29 | 29 | 0 |
| corrected preparations | 6 | 6 | 0 |
| capability acceptances | 12 | 12 | 0 |
| execution authorizations | 14 | 14 | 0 |
| attempts | 10 | 10 | 0 |
| provider state | 8 | 8 | 0 |
| derivative generations | 22 | 22 | 0 |
| derivative content | 20 | 20 | 0 |
| evidence audit | 1 | 1 | 0 |
| document-ingestion audit | 1 | 1 | 0 |

The replacement-candidate archive is artifact preservation, not governed evidence mutation. R6-R1 derivative generation/content remained absent and no continuation endpoint was invoked.

## Provider accounting

OI11R6F totals:

* OpenAI calls 0;
* Claude calls 0;
* other external provider calls 0;
* retries 0;
* external evidence egress 0;
* production provider-state delta 0.

The historical R6-R1 OpenAI call remains exactly 1 with zero retries. **NO RETRY IS AUTHORIZED.**

## Exact owner artifact-acceptance package

**OWNER ARTIFACT ACCEPTANCE REQUIRED**

### Source

`e3fec3fac857e7b0e610375d066d524646a1375f`

### JAR

SHA-256 `f4971f223612a1791ca6a013bc3234e03d85b9cfbb7fecc09886b40983861d63`

### OCI

* image/index `sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089`
* platform manifest `sha256:3d6655e179e6cd0a639a93d49c3d3b09aa215676035d7afd79dfadf14eaa7b54`
* config `sha256:03672bc9966ede6940ee200dfa8e2f2c9016c95a78b818f7a6ddde420d52e8da`

### Archive

* path `/mnt/parker-data/parker/replacement-candidates/oi11r6f-post-egress-continuation-e3fec3fa-20260903.tar`
* SHA-256 `c0e89bb2f71315a816dac4081f6bfb630f8848d975a3ab7c0cc8f4c200ff2527`
* size `1,031,915,008 bytes`
* protection `0600`, `steve:steve`

### Semantic identity and continuation properties

* V8 capability `ordinary-external-request-region-transcription-v8`;
* V8 digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`;
* provider-profile correction present: PASS;
* persisted-state continuation present: PASS;
* provider invocation impossible from continuation: PASS;
* fail-closed/idempotent/conflict behavior: PASS;
* exact R6-R1 offline replay: PASS, 5/5, provider calls required 0;
* archive recoverability: PASS;
* production unchanged: PASS;
* governed-store delta 0;
* provider activity 0.

The candidate has not been accepted or deployed. Production continuation from R6-R1 remains a separate owner-governed action after artifact acceptance, exact deployment, production verification and any required implementation-bound capability decision.
