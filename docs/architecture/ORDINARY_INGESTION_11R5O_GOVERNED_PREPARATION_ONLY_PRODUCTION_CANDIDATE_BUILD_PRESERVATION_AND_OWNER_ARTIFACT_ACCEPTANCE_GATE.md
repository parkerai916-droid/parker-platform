# OI11R5O — Governed Preparation-Only Production Candidate Build, Preservation and Owner Artifact-Acceptance Gate

## Disposition

The exact R5N source was built into a production candidate, the candidate was verified and preserved, and production remained unchanged. The candidate has not been accepted or deployed. All R5O gates passed.

## Starting repository identity

The host was `parker`, repository `/home/steve/parker-platform`, branch `main`. Before the build, HEAD and upstream were both exactly `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`; the worktree was clean and `git diff --check` passed. No substitute source was used.

## R5N test evidence

The committed R5N report, SHA-256 `82e80c210f11030cbcb377147df381bad697cc4163e330643401f966e7709da4`, records:

- focused tests: 169 tests, 0 failures, 0 errors;
- full suite: 251 suites, 3,313 tests, 0 failures, 0 errors, 17 skipped;
- bounded Gradle and Kotlin heap: `-Xmx4g`.

These authoritative results were not reinterpreted. R5O performed the bounded offline production distribution build with:

```text
PARKER_BUILD_COMMIT=a031c92549fd7a3b8c92f6917be0e59b61ca5fde ./gradlew installDist --offline --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g
```

It completed successfully. No test rerun was required.

## Candidate JAR and embedded source

| Field | Exact value |
|---|---|
| JAR | `build/libs/parker-platform-0.8.0-runtime-complete.jar` |
| Size | 4,614,840 bytes |
| SHA-256 | `b398a59855303d5d47b0c2154d2b6ddb31c8784f97b86b6991f08723b929831f` |
| Manifest field | `Parker-Source-Commit: a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |

The JAR inside the candidate image has the same SHA-256. The embedded source is therefore exact and untruncated.

## OCI candidate identities

The established offline Parker layering procedure was used because Dockerfile, Gradle build inputs and runtime tools are unchanged from the exact currently accepted production artifact. The local accepted production image supplied the unchanged runtime layers; the exact newly built install distribution and governed tools directory supplied the application layer. BuildKit used `--network=none`, `--pull=false`, and `linux/amd64`; it did not rebuild, pull, deploy or restart production.

| Field | Exact value |
|---|---|
| Local immutable reference | `parker-oi11r5o-a031c925@sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| OCI image/index | `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| Platform manifest | `sha256:1e7c1b781cc69fb1b42499f31dc05afeaed0975a54a74326b63b7ad9f8a5518f` |
| OCI config | `sha256:157d92a9e1636b4c245de66b3c5f64394dd2db38da1a9e73cc4974aca242e440` |
| Attestation manifest | `sha256:7dac120fcebb327f0444ce57c9e2b31ae661c23c581b5575ff196738b837b69f` |
| Platform | `linux/amd64` |
| Build timestamp | `2026-09-02T08:18:44.516990162Z` |
| Governed label timestamp | `2026-09-02T00:00:00Z` |
| Embedded source | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |

The content digests, not the mutable local tag, are authoritative.

## Artifact semantic verification

JAR class and bytecode inspection established that the candidate contains and composes:

- `GovernedCorrectedPreparationService`, custody-bound through `AuthoritativeAcquisitionSourceResolver`;
- `FileSystemFullPageAchromaticPreparationStore` and `FullPageAchromaticPreparationCodec`;
- create-once, byte-equal replay and conflicting-identity rejection;
- `OwnerEvidenceHttpServer.CorrectedPreparationHandler` at authenticated owner/admin route `/owner/admin/corrected-preparation`;
- `ParkerRuntime.prepareCorrectedEvidenceAsOwner` production composition;
- metadata-only `GovernedCorrectedPreparationResult` and readback verification.

The service accepts only the frozen profile, resolves already-governed custody, rejects unsupported MIME/profile/chromatic risk, persists through the canonical store, reads the persisted record back, and compares canonical codec bytes before returning metadata. It has no provider exchange, execution authorization, attempt, provider-state or derivative dependency.

The exact full-suite evidence plus candidate class inspection confirms retention of the existing authorization gates, capability acceptance/evaluator, attempt controls, provider-state handling, raw-before-parse response flow and derivative-admission controls. No execution behavior was weakened or removed.

## Corrected-preparation store composition

The repository Compose render has SHA-256 `4c3e6e11f57db98cc5e0fc54d195140179ed2911db9f0a29a2e0b79fe1472fb7`. It resolves:

```text
PARKER_CORRECTED_PREPARATION_STORAGE_ROOT=/data/corrected-preparations
/mnt/parker-data/parker/corrected-preparations:/data/corrected-preparations
```

This is the intended durable Parker host location. R5O did not create the directory or write a corrected preparation record.

## Frozen identities

Candidate source and bytecode inspection verified the unchanged capability:

- capability: `ordinary-external-request-region-transcription-v8`;
- digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`;
- maximum regions: 32;
- maximum request body: 16,777,216 bytes;
- batching: false.

The candidate also contains exact profile `full-page-achromatic-png-preparation-v1`, version `1`. Its semantics remain full-page geometry, original dimensions, deterministic `TYPE_BYTE_GRAY` output, formula `Y=(77R+150G+29B+128)>>8`, explicit JDK 17 PNG writer selection, maximum lossless compression, no interlace/ancillary metadata, deterministic ordering, and fail-closed chromatic-risk handling. No alternative profile was substituted.

## Historical compatibility and R5F isolation

The authoritative 3,313-test R5N full suite covers legacy and current codecs and stores. Candidate inspection retained evidence and manifest storage/codecs, six-record legacy capability support, historical V8 acceptance support, authorization and attempt stores, provider-state recovery, derivative generation/content, historical governed page representation/order state, and corrected-preparation codec/readback. This was an offline/static check; production stores were not mounted into or mutated by the candidate.

The historical R5F representation `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e` and page-1 region-set digest `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f` were not read, rewritten, promoted, owner-ordered or reused.

## Preserved archive and recovery verification

| Field | Exact value |
|---|---|
| Archive | `/mnt/parker-data/parker/replacement-candidates/oi11r5o-governed-preparation-only-a031c925-20260902.tar` |
| Size | 990,716,416 bytes |
| SHA-256 | `fdc0ff366b182a2f9ae0fa684c955cf6e2c5264c3cf288831731b85d7e1b6c97` |
| Owner/group | `steve:steve` |
| Permissions | `0600` |

The destination did not exist before preservation, so no archive was overwritten. Direct OCI archive inspection recomputed the exact index, platform-manifest and config digests from the stored blobs and confirmed `linux/amd64` plus the exact embedded-source binding. The archive is sufficient to recover the candidate. It was not loaded into production or deployed.

## Production unchanged and store accounting

Before and after R5O, production was exactly:

| Field | Exact value |
|---|---|
| Container | `eb5a5bcf74ec26fe09bbb59b9a10a9eb0fd92d02a92cdb5812a18c35c3a4dd0f` |
| Image/index | `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f` |
| Embedded source | `fe13047df0dd5f155d6a6921acf7bc85541af26f` |
| Started | `2026-09-01T13:23:54.513307926Z` |
| Restart count | 0 |
| Runtime readiness | PASS (`overallReady=true`, empty reasons) |

There was no Compose mutation, deployment or restart.

| Governed store | Before | After | Delta |
|---|---:|---:|---:|
| evidence | 29 | 29 | 0 |
| evidence source manifests | 29 | 29 | 0 |
| capability acceptances | 10 | 10 | 0 |
| owner authorizations | 11 | 11 | 0 |
| attempts | 8 | 8 | 0 |
| provider state | 6 | 6 | 0 |
| derivative generations | 22 | 22 | 0 |
| derivative content | 20 | 20 | 0 |
| corrected preparations | absent / 0 | absent / 0 | 0 |

## Deed and provider boundary

Registered evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, was not supplied to the candidate or preparation endpoint. It was not prepared, request-shaped, authorized, attempted, transmitted or transcribed. No provider-state or derivative record was created.

Provider accounting is OpenAI calls 0, Claude calls 0, other external provider calls 0, retries 0, external provider/evidence egress 0. No provider authorization, adapter invocation, provider request or transmission of governed evidence occurred. A read-only Docker registry identity lookup against the local tag returned `pull access denied`; it retrieved and published no artifact and carried no Parker source or evidence. The candidate build itself used `--network=none` and `--pull=false`.

## Exact owner artifact-acceptance information and stop state

| Field | Exact value |
|---|---|
| Source commit | `a031c92549fd7a3b8c92f6917be0e59b61ca5fde` |
| JAR SHA-256 | `b398a59855303d5d47b0c2154d2b6ddb31c8784f97b86b6991f08723b929831f` |
| OCI image/index | `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb` |
| Platform manifest | `sha256:1e7c1b781cc69fb1b42499f31dc05afeaed0975a54a74326b63b7ad9f8a5518f` |
| OCI config | `sha256:157d92a9e1636b4c245de66b3c5f64394dd2db38da1a9e73cc4974aca242e440` |
| Preserved archive | `/mnt/parker-data/parker/replacement-candidates/oi11r5o-governed-preparation-only-a031c925-20260902.tar` |
| Archive SHA-256 | `fdc0ff366b182a2f9ae0fa684c955cf6e2c5264c3cf288831731b85d7e1b6c97` |
| Archive size | 990,716,416 bytes |

**THE CANDIDATE HAS NOT BEEN ACCEPTED OR DEPLOYED.** Acceptance would require a separate explicit owner decision binding these exact identities. It would authorize only the separately stated next scope; it would not itself prepare the Deed or authorize provider execution.

R5O stops here for explicit owner acceptance of the exact preserved artifact. No acceptance record has been created.

UNIT ORDINARY-INGESTION-11R5O COMPLETE — A PRODUCTION CANDIDATE CONTAINING THE GOVERNED PREPARATION-ONLY OPERATION AND DURABLE CORRECTED-PREPARATION STORE HAS BEEN BUILT FROM EXACT SOURCE A031C92549FD7A3B8C92F6917BE0E59B61CA5FDE, IDENTIFIED, VERIFIED AND PRESERVED. THE ACCEPTED FULL-PAGE-ACHROMATIC-PNG-PREPARATION-V1 SEMANTICS AND V8 CAPABILITY IDENTITY/DIGEST REMAIN UNCHANGED. CURRENT PRODUCTION REMAINS UNCHANGED. THE REGISTERED DEED HAS NOT BEEN PREPARED, AUTHORIZED, TRANSMITTED OR TRANSCRIBED. NO PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. THE CANDIDATE HAS NOT BEEN ACCEPTED OR DEPLOYED. FURTHER PROGRESS REQUIRES EXPLICIT OWNER ACCEPTANCE OF THE EXACT PRESERVED ARTIFACT.
