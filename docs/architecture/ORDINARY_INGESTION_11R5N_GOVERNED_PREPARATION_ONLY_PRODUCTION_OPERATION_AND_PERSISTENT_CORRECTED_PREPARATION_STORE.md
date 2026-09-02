# OI11R5N — Governed Preparation-Only Production Operation and Persistent Corrected-Preparation Store

## Disposition and classification

OI11R5N is complete. The change is **implementation/composition completion** under the accepted R5H/R5I design: it exposes and durably composes the already-accepted `full-page-achromatic-png-preparation-v1` version 1 behavior without changing preparation semantics, V8 request/response semantics, authority boundaries or capability identity.

Starting branch was `main`; HEAD and upstream were exactly `c7d16a4168da0ac9c7600ce03d5b25c8e445fe0f`; the worktree was clean.

## R5M failure and root gap

R5M correctly returned `C — PREPARATION OR REQUEST GATE FAILURE`. The artifact contained deterministic preparation and create-once storage classes, but production instantiated `OrdinaryRequestRegionV8RequestPreparer()` with a default builder whose preparation persistence was null. No corrected-preparation root was configured or mounted, and the only runtime route into preparation was inside evidence-authorized execution.

R5N closes only that gap. It does not prepare the registered Deed and does not change provider execution.

## Implementation

Changed implementation files:

- `src/runtime/GovernedCorrectedPreparation.kt`
- `src/runtime/OrdinaryRequestRegionV8TruthfulContract.kt`
- `src/composition/ParkerRuntimeConfig.kt`
- `src/composition/ParkerRuntime.kt`
- `src/composition/Main.kt`
- `src/composition/OwnerEvidenceHttpServer.kt`
- `docker-compose.yml`

Changed test files:

- `tests/runtime/GovernedCorrectedPreparationServiceTest.kt`
- `tests/composition/OwnerEvidenceHttpServerTest.kt`
- `tests/composition/ParkerRuntimeConfigLoaderTest.kt`
- `tests/composition/ParkerRuntimeStartupAndShutdownTest.kt`

### Preparation-only service

`GovernedCorrectedPreparationService` is the single application operation:

```text
governed EvidenceArtifactId
  -> Evidence Custodian source + manifest resolution and byte/digest/length verification
  -> exact full-page-achromatic-png-preparation-v1/version 1 builder
  -> create-once corrected-preparation persistence
  -> canonical codec readback and exact canonical-record comparison
  -> preparation-only identities, provenance, page/order metadata, transport hashes/sizes and local request hashes/sizes
```

The operation receives no authorization ID, execution ID, attempt ID, provider, credential, transport, derivative store or admission port. Its deterministic local correlation identity is derived only from the operation domain, evidence ID, verified source digest and fixed preparation profile/version. It cannot reserve authority or transition an attempt.

The result contains no source/PDF bytes, RGB pixels, PNG bytes or serialized request body. It returns evidence/profile identity, document preparation identity, region-set digest, page count, singleton preparation-region identities, authoritative page/pixel provenance, deterministic source-order state, transport SHA-256/byte lengths, request region count, request/body hashes and sizes, aggregate image/base64 sizes, and explicit readback verification.

### Owner/admin operation

The authenticated production operation is:

```text
POST /owner/admin/corrected-preparation/{evidenceArtifactId}
Authorization: Bearer <existing owner token>
{"profileId":"full-page-achromatic-png-preparation-v1","profileVersion":"1"}
```

The route is bounded by the existing 1,024-byte administrative body limit and the existing constant-time owner bearer-token check. Its schema is exact: unknown fields, including any attempted `executionId`, are rejected before the service is called. It returns metadata only. It neither calls the ordinary authorization/execution routes nor accepts provider or egress fields.

### Durable store and composition

The canonical host root is `/mnt/parker-data/parker/corrected-preparations`, mounted at `/data/corrected-preparations` and configured by `PARKER_CORRECTED_PREPARATION_STORAGE_ROOT`. This is a sibling under Parker's established `/mnt/parker-data/parker/...` production-data hierarchy, not an ad hoc cache and not nested in evidence, derivative or historical representation state.

When ordinary region ingestion is enabled, missing corrected-preparation configuration now fails startup configuration validation. `ParkerRuntime` creates `FileSystemFullPageAchromaticPreparationStore` at that exact configured root and injects it into the preparation-only service. The existing R5I store persists canonical records under `records/{preparationIdentity}.json` and byte-addressed images under `transport/{transportSha256}.png`; identical re-admission is idempotent, conflicting existing bytes fail closed, and readback validates record identity and transport bytes.

The production preparation builder now preserves the underlying rejection reason, including `CHROMATIC_RISK`, rather than collapsing every preparer rejection into a generic message. This changes no acceptance rule or profile identity.

## Fail-closed behavior

The service rejects missing/denied/inconsistent custody state, unsupported MIME, any non-exact profile/version, renderer/preparer failure, chromatic-risk rejection, unavailable/unwritable storage, create-once conflict, codec/readback mismatch, request cardinality failure and request size failure. Exceptions are projected as a rejected preparation outcome; the authenticated route returns a governed conflict response and never falls through to execution.

No alternate profile, JPEG, resizing, OCR segmentation, provider fallback, retry or transport fallback exists.

## R5F isolation and Deed boundary

The new root is disjoint from `ordinary-ingestion-representations` and receives no R5F page/geometry/order dependency. Preparation geometry continues to be one complete page rectangle derived from each authoritative rendered page. The historical page-1 representation `33d341f5f169ea09a6cdeffc50c731a6b9d58e2a646ffb1ac32532bee2afff1e` and region-set digest `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f` were not read, rewritten, ordered, promoted or reused by implementation tests.

The registered Deed `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9` was not submitted to the new service or any local acceptance CLI. All tests used synthetic, non-sensitive PDF/image fixtures. No production corrected-preparation directory or record was created.

## V8 identity and execution isolation

Unchanged:

- capability: `ordinary-external-request-region-transcription-v8`
- digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`
- profile: `full-page-achromatic-png-preparation-v1`, version 1
- V8 schema, instruction, adapter/parser, ordering, response reconstruction and raw-before-parse semantics

The focused test asserts the exact capability ID/digest. The complete suite confirms existing execution behavior and authorization gates remain intact.

Static dependency inspection shows `GovernedCorrectedPreparationService` depends only on Evidence Custodian resolution, the deterministic builder and corrected-preparation store. It has no OpenAI/Claude/provider client, `RequestRegionV8ProviderExchange`, HTTP transport, ledger, authorization store, provider-state store, derivative store, retry loop or external-reasoning dependency. The HTTP handler invokes only this service callback.

## Verification

Focused command:

```text
./gradlew test --tests parker.core.runtime.GovernedCorrectedPreparationServiceTest --tests parker.core.runtime.FullPageAchromaticPreparationTest --tests parker.composition.ParkerRuntimeConfigLoaderTest --tests parker.composition.ParkerRuntimeStartupAndShutdownTest --tests parker.composition.OwnerEvidenceHttpServerTest --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g
```

Result: 169 tests, 0 failures, 0 errors. Coverage includes governed preparation without execution authorization; persistence/readback; idempotent replay; conflict rejection; missing evidence; unsupported profile/MIME; chromatic-risk rejection; deterministic geometry/order; authenticated exact-schema endpoint; rejection of injected execution fields; configuration/startup; historical R5F separation; and frozen V8 identity/digest.

Full command:

```text
./gradlew test --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g
```

Result: 251 suites, 3,313 tests, 0 failures, 0 errors, 17 skipped. The environment was host `parker`, Linux amd64, with bounded Gradle/Kotlin heap `-Xmx4g`.

## Provider and production accounting

OpenAI calls: 0. Claude calls: 0. Other external calls: 0. Retries: 0. External egress: 0.

No production endpoint was invoked and no production configuration was changed. Production remained container `eb5a5bcf74ec26fe09bbb59b9a10a9eb0fd92d02a92cdb5812a18c35c3a4dd0f`, image/index `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f`, source `fe13047df0dd5f155d6a6921acf7bc85541af26f`, running with restart count 0.

Production counts remained evidence 29, manifests 29, capability acceptances 10, owner authorizations 11, attempts 8, provider state 6, derivative generations 22 and derivative content 20. Governed production delta was zero.

## Deployment state and next unit

R5N stops at implementation and tests. No production candidate was built or accepted; no artifact acceptance, deployment, restart, new implementation-bound capability acceptance or R5M retry occurred.

The next unit must build and preserve an exact production candidate from the final R5N commit, verify its semantics and identity, and stop for explicit owner artifact acceptance. Subsequent separately authorized units must deploy the exact accepted artifact, verify the new durable mount/readiness/history, and create new implementation-bound V8 authority. Only then may R5M be retried against the registered Deed.

UNIT ORDINARY-INGESTION-11R5N COMPLETE — PARKER NOW HAS A GOVERNED PREPARATION-ONLY PRODUCTION OPERATION AND DURABLE CREATE-ONCE CORRECTED-PREPARATION STORE, SEPARATE FROM PROVIDER EXECUTION. THE ACCEPTED FULL-PAGE-ACHROMATIC-PNG-PREPARATION-V1 SEMANTICS AND V8 CAPABILITY IDENTITY/DIGEST REMAIN UNCHANGED. HISTORICAL OI11R5F STATE REMAINS IMMUTABLE. THE REGISTERED DEED WAS NOT PREPARED, AUTHORIZED, TRANSMITTED OR TRANSCRIBED. NO PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. THE UNIT STOPS BEFORE ARTIFACT ACCEPTANCE, DEPLOYMENT OR R5M RETRY.
