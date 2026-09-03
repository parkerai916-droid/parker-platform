# OI11R6K — Historical-Identity-Corrected Continuation Production Candidate Build, Preservation and Owner Artifact-Acceptance Gate

Date: 2026-09-03 UTC

## Verdict

**A — HISTORICAL-IDENTITY-CORRECTED CONTINUATION PRODUCTION CANDIDATE BUILT AND PRESERVED; OWNER ACCEPTANCE REQUIRED**

An exact production candidate was built from OI11R6J source, inspected, replayed offline against copied R6-R1 state, preserved, reloaded, and verified. It was not accepted or deployed. No continuation, derivative admission, provider call, retry, or external evidence egress occurred.

## Starting state

The repository gate passed:

* host `parker`;
* repository `/home/steve/parker-platform`;
* branch `main`;
* HEAD/upstream `ca15222c9f5edea28e68bbb0099734578fc30c4a`;
* worktree clean.

The executable artifact was completed before this report was created. This report is the only repository change made by R6K and does not enter the executable candidate.

## R6J verification

The authoritative R6J report existed at the required path with SHA-256 `864ad4525be171fa93696c46c8893e941548fa476f1cc818888c10312e3f48be`. It records independently proven root cause, persisted historical identity readback, separate current implementation authority, immutable attempt validation, removal of `DURABLE_PROVIDER_ATTEMPT_REQUIRED` in exact offline replay, exact provider-state and exhausted budget preservation, V8 5/5, provider-profile and derivative-provenance validation, unchanged V8 identity/digest, no remaining known blocker, focused/full tests PASS, production delta zero, and provider calls/retries/egress `0 / 0 / 0`.

R6J full-suite evidence was 251 suites, 3,326 tests, zero failures, zero errors, and 18 skipped.

## Preserved R6-R1 historical state

The candidate remains compatible with these immutable production identities:

| Field | Exact value |
|---|---|
| Authorization | `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97` |
| Authorization record SHA-256 | `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544` |
| Execution | `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976` |
| Attempt ledger SHA-256 | `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591` |
| Historical implementation | `39fe0e777608c96cba20cec491113e77eee4b8ef` |
| Provider-state record | `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7` |
| Provider-state file SHA-256 | `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c` |
| Assessment SHA-256 | `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01` |
| Raw response SHA-256 | `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a` |
| Provider response ID | `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b` |
| Provider model | `gpt-5.6-sol` |

The recorded V8 result remains SUCCESS 5/5 in Parker order `[1,2,3,4,5]`. The derivative remains unadmitted. Provider budget remains maximum 1, consumed 1, retry limit 0. No record was mutated.

## Exact executable source and production JAR

The executable source is exactly `ca15222c9f5edea28e68bbb0099734578fc30c4a`. The established bounded, offline production build was:

```text
PARKER_BUILD_COMMIT=ca15222c9f5edea28e68bbb0099734578fc30c4a ./gradlew clean installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g
```

The build succeeded.

* JAR path: `build/libs/parker-platform-0.8.0-runtime-complete.jar`
* JAR size: `4,653,000 bytes`
* JAR SHA-256: `ce5191a5a04de91c9697acb38043cba6ddfa0c11bfb20f16babeb216804d7137`
* embedded `Parker-Source-Commit`: `ca15222c9f5edea28e68bbb0099734578fc30c4a`

The installed-distribution JAR was byte-identical. Extraction from the recovered OCI candidate reproduced the same size, SHA-256, and embedded source.

## OCI identity

The established isolated BuildKit procedure copied the exact installed distribution and repository tools over the already-local Parker runtime base `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`. BuildKit used `--network=none`, `--pull=false`, platform `linux/amd64`, OCI output, and provenance attestation. No registry or provider was contacted.

* OCI image/index: `sha256:adbb96afdb732a4549661fef08773d1b70a471e5311c804392f5fba26ce1ea4e`
* platform manifest: `sha256:374787378c5096c65231c959754d2ff9c6e694fc03c6dff33741a620f37081ba`
* OCI config: `sha256:fd2e5764f059980e946da8d92b15e0598cae964bdfd6489b7e8888fd855ecd4a`
* attestation manifest: `sha256:0a5987eb5efbfaad241fa95e5a8c18e37732d55cd892b75fab7fea0e24d15508`
* platform: `linux/amd64`
* OCI revision: `ca15222c9f5edea28e68bbb0099734578fc30c4a`
* governed creation label: `2026-09-03T00:00:00Z`
* config creation: `2026-09-03T01:13:00.157150996Z`
* index completion annotation: `2026-09-03T01:13:02Z`
* runtime user: `parker`
* entry point: `/opt/parker/bin/parker`

## Historical/current implementation identity separation

Recovered candidate bytecode proves `recoverPersistedPostEgress` calls `FileSystemFidelityFirstAttemptLedger.readExisting(executionId)`, reads the historical commit from the returned immutable identity, compares the complete ledger identity to a reconstruction differing only in that record-derived commit, and validates provider-state implementation against the historical commit.

Thus:

* historical R6-R1 attempt implementation: `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* current candidate implementation: `ca15222c9f5edea28e68bbb0099734578fc30c4a`;
* historical/current separation: **PASS**.

The current candidate commit is not substituted into the historical attempt. The ledger is read, never opened for creation or rewritten, by continuation recovery.

## Current continuation authority enforcement

Candidate bytecode places `OrdinaryRequestRegionV8AcceptanceEvaluator.evaluate()` at the beginning of `continuePostEgress`. A non-`Accepted` result returns `CAPABILITY_NOT_ACCEPTED` before custody, historical recovery, replay, or admission. Historical acceptance does not imply authority for `ca15222c...`.

No implementation-bound V8 acceptance for `ca15222c9f5edea28e68bbb0099734578fc30c4a` was created in R6K. After a future deployment, the expected state is fail-closed until a separate owner acceptance is recorded.

## Fail-closed contradiction handling

The candidate contains explicit fail-closed handling for:

* missing/not-started historical attempt: `DURABLE_PROVIDER_ATTEMPT_REQUIRED`;
* malformed historical ledger: `HISTORICAL_ATTEMPT_INVALID`;
* full execution/attempt identity disagreement: `HISTORICAL_ATTEMPT_IDENTITY_MISMATCH`;
* attempt/provider-state implementation disagreement: `HISTORICAL_PROVIDER_STATE_IMPLEMENTATION_MISMATCH`;
* provider-state evidence/source/execution/attempt/request/capability disagreement: `RECOVERED_BINDING_MISMATCH`;
* absent current implementation acceptance: `CAPABILITY_NOT_ACCEPTED`.

Synthetic tests cover missing, malformed, mismatched, and unaccepted cases. No path silently repairs persisted identity using current runtime configuration.

## Provider-prohibition verification

Bytecode inspection established that `recoverPersistedPostEgress` performs canonical store readback, identity checks, deterministic parsing/validation, and derivative reconstruction only. It has no call to `durablyStartProviderAttempt`, `transportAfterGuardRelease`, provider exchange, OpenAI transport, Claude, or another provider.

`continuePostEgress` calls recovery and canonical admission; it neither creates a provider attempt nor resets budget. The continuation admission branch performs no historical attempt transition. Provider invocation from continuation is **PROHIBITED**.

## Exact R6-R1 artifact-local offline replay

An isolated source/test harness was copied from exact executable source. Its only harness-local adjustment replaced the prior deployed-continuation test constant with candidate commit `ca15222c9f5edea28e68bbb0099734578fc30c4a`; no application code changed. Eleven exact, read-only copied R6-R1 files supplied authorization/events, attempt ledger, provider state/assessment, corrected-preparation record, and five transport objects.

The exact copied-state test was forced to execute with the fixture path inherited by its test JVM and passed: 1 test, zero skipped, zero failures, zero errors.

| Gate | Result |
|---|---|
| Historical implementation | `39fe0e777608c96cba20cec491113e77eee4b8ef` |
| Current candidate implementation | `ca15222c9f5edea28e68bbb0099734578fc30c4a` |
| Historical attempt | VALID |
| Provider state/raw response | VALID |
| V8 | SUCCESS, 5/5 |
| Parker order | `[1,2,3,4,5]` |
| Provider | `OpenAI` |
| Provider profile | `openai-fidelity-first-transcription-v1` |
| Model | `gpt-5.6-sol` |
| Response ID | `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b` |
| Provider calls required | 0 |
| Retries required | 0 |
| Derivative provenance | VALID |
| Admission eligibility | READY, subject to later deployment/current authority/owner continuation gates |

The broader convergence class also passed 20 executed tests with one deliberately property-gated exact-state test skipped before the separately forced exact run. No provider dependency was supplied to replay.

## Complete continuation artifact audit

| Transition | Candidate component and invariant | Result |
|---|---|---|
| Current implementation authority | current commit evaluated against exact implementation-bound V8 record | PASS / separately gated |
| Historical authorization | versioned exact-envelope canonical store | PASS |
| Historical execution/attempt | read-only ledger decode and full identity validation | PASS |
| Provider state | exact historical implementation/execution/request bindings | PASS |
| Raw replay | persisted raw/assessment only; no transport | PASS |
| V8 validation | five exact regions, Parker order authoritative | 5/5 PASS |
| Provider-profile provenance | exact authorization-bound provider profile | PASS |
| Derivative provenance | evidence through Purpose/capability/provider state | PASS |
| Admission | create-once/idempotent; conflict fail-closed | PASS |
| Historical ledger | no continuation transition/rewrite | PASS |

No known structural blocker remains in the artifact-local continuation path.

## V8 identity and digest

Recovered candidate bytecode reports and enforces:

* capability `ordinary-external-request-region-transcription-v8`;
* digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`.

The identity and digest are unchanged. R6K introduces no new capability semantics or authority.

## Archive preservation and recoverability

The exact candidate is preserved at:

`/mnt/parker-data/parker/replacement-candidates/oi11r6k-historical-identity-corrected-continuation-ca15222c-20260903.tar`

* archive SHA-256: `ed6b5efc984ec4bed2feb254e7fd78b31fd770a90659c6f3bd50530249fe11a4`
* archive size: `1,031,915,520 bytes`
* permissions: `0600`
* owner/group: `steve:steve`

The preserved hash reproduced exactly. `docker load` recovered image/index `sha256:adbb96af...`; inspection reproduced the platform manifest/config, Linux/amd64 platform, revision, runtime user, and entry point. A never-started temporary container yielded exact JAR `ce5191a5...`, size `4,653,000`, and embedded source `ca15222c...`; the temporary container was then removed. Recoverability: **PASS**. The candidate was not started or deployed.

## Current production baseline and governed-store accounting

Production was identical before and after R6K:

* container `f2a9df159d4305528ff89ac26e2dcc8f5e51fc969839f0d91f204976cb6ed542`;
* image/index `sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089`;
* embedded source `e3fec3fac857e7b0e610375d066d524646a1375f`;
* runtime JAR `f4971f223612a1791ca6a013bc3234e03d85b9cfbb7fecc09886b40983861d63`;
* running since `2026-09-02T12:55:25.243280806Z`;
* restart count 0;
* readiness PASS, unchanged from the canonical R6H/R6I verification with no restart or configuration change.

| Store | Before | After | Delta |
|---|---:|---:|---:|
| evidence | 29 | 29 | 0 |
| evidence-source manifests | 29 | 29 | 0 |
| corrected-preparation records | 1 | 1 | 0 |
| corrected-preparation transports | 5 | 5 | 0 |
| capability acceptances | 13 | 13 | 0 |
| execution authorizations | 14 | 14 | 0 |
| attempts | 10 | 10 | 0 |
| provider state/assessments | 8 | 8 | 0 |
| derivative generations | 22 | 22 | 0 |
| derivative content | 20 | 20 | 0 |
| evidence audit | 1 | 1 | 0 |
| document-ingestion audit | 1 | 1 | 0 |

Every aggregate store digest matched before and after. Exact authorization, authorization-event, attempt-ledger, provider-state, and assessment hashes remained unchanged. No generation/content record references the R6-R1 execution. The R6-R1 derivative remains **NOT ADMITTED**.

## Provider accounting

OI11R6K totals:

* OpenAI calls 0;
* Claude calls 0;
* other external provider calls 0;
* retries 0;
* external evidence egress 0;
* production provider-state delta 0.

The historical R6-R1 OpenAI call remains exactly 1 with zero retries. No retry is authorized.

## Exact owner artifact-acceptance package

**OWNER ARTIFACT ACCEPTANCE REQUIRED**

### Source

`ca15222c9f5edea28e68bbb0099734578fc30c4a`

### JAR

SHA-256 `ce5191a5a04de91c9697acb38043cba6ddfa0c11bfb20f16babeb216804d7137`

### OCI

* image/index `sha256:adbb96afdb732a4549661fef08773d1b70a471e5311c804392f5fba26ce1ea4e`
* platform manifest `sha256:374787378c5096c65231c959754d2ff9c6e694fc03c6dff33741a620f37081ba`
* config `sha256:fd2e5764f059980e946da8d92b15e0598cae964bdfd6489b7e8888fd855ecd4a`

### Archive

* path `/mnt/parker-data/parker/replacement-candidates/oi11r6k-historical-identity-corrected-continuation-ca15222c-20260903.tar`
* SHA-256 `ed6b5efc984ec4bed2feb254e7fd78b31fd770a90659c6f3bd50530249fe11a4`
* size `1,031,915,520 bytes`
* permissions `0600`
* owner/group `steve:steve`

### V8 and continuation properties

* capability `ordinary-external-request-region-transcription-v8`;
* capability digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`;
* historical R6-R1 implementation `39fe0e777608c96cba20cec491113e77eee4b8ef`;
* current candidate implementation `ca15222c9f5edea28e68bbb0099734578fc30c4a`;
* historical/current separation PASS;
* durable attempt validation PASS;
* provider-state validation PASS;
* V8 replay 5/5 PASS;
* provider-profile provenance PASS;
* derivative provenance PASS;
* provider invocation from continuation PROHIBITED;
* provider calls required 0;
* complete continuation audit: NO KNOWN STRUCTURAL BLOCKER;
* archive recoverability PASS;
* production unchanged;
* governed-store delta 0;
* provider activity 0.

The candidate has not been owner-accepted or deployed. The required later sequence remains: explicit owner artifact acceptance, exact deployment and production verification, separate implementation-bound V8 acceptance for `ca15222c...`, and a separate owner-governed zero-egress continuation decision.
