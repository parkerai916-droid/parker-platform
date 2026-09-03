# Ordinary Ingestion 11R6V-A4-MIN — Minimum Governed Human Fidelity Review Recording Service

## Status and starting state

**COMPLETE — MINIMUM RECORDING APPLICATION BOUNDARY ONLY**

Host `parker`, branch `main`, starting HEAD/upstream were exactly `8bfb6847204735cb416f654357bd153d183b0640`; the worktree was clean. The frozen R6T Scope Lock SHA-256 was verified as `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`, and the frozen R6V implementation-plan SHA-256 as `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`. Completed A1, A2, and A3 identities were respectively `58c03abcd96b62f17f10069825982454fd06e680`, `c355ba8ee9254bd1925ada7245e018e568f4616b`, and `8bfb6847204735cb416f654357bd153d183b0640`.

## Service boundary

`GovernedHumanFidelityReviewRecordingRequest` carries exactly one existing immutable `HumanFidelityReviewRecord` and one existing `HumanFidelityReviewRecordingAuthorityScope`. The small result model distinguishes `Recorded`, `AlreadyRecorded`, `AuthorizationDenied`, and a typed fail-closed `Failure` reason.

`DefaultGovernedHumanFidelityReviewRecordingService` has exactly two dependencies: `HumanFidelityReviewRecordingPermissionEvaluator` and `HumanFidelityReviewStorage`. It has no provider, audit-store, query, projector, correction, derivative, or production-composition dependency. A2 remains the sole owner of storage and audit mutation semantics.

## Authorization-before-mutation proof

The service first compares the authority target with the review's complete A1 target. A mismatch returns `AuthorizationDenied(TARGET_MISMATCH)` without touching storage. It then passes the same two exact targets to the A3 evaluator. An explicit denial returns the governing A3 denial reason; an evaluator exception returns `AUTHORITY_EVALUATION_FAILED`. Only `Authorized` reaches `storage.prepare`.

An observing integration test records the exact call order as `authority`, `prepare`, `publish`, `retrieve`. Wrong principal, wrong purpose, wrong target, explicit permission denial, and permission-evaluator exception each leave the canonical review list, prepared directory, and human-fidelity audit list empty.

## Prepare, publish, and canonical readback

After authorization, the service uses A2 unchanged:

1. `prepare(review)` performs create-once preparation and truthful PREPARED audit handling;
2. unless preparation reports an already-published exact record, `publishPrepared(reviewId)` performs A2 publication and PUBLISHED audit handling;
3. `retrieve(reviewId)` performs mandatory canonical readback, including A2's complete-audit and integrity checks; and
4. the service compares the exact version-1 canonical encoding of readback and request.

Success is impossible before readback. Missing readback returns `CANONICAL_READBACK_MISSING`; corrupt/unreadable storage returns `STORAGE_OPERATION_FAILED`; differing canonical bytes return `CANONICAL_READBACK_MISMATCH`.

## Duplicate and failure behavior

An exact existing publication is revalidated by A2, including required audit facts, and then canonically read back. It returns `AlreadyRecorded`; repeated exact duplicates leave one review and one deterministic set of three A2 facts (PREPARED, PUBLISHED, DUPLICATE_CONFIRMED). Same review ID with different canonical facts fails without replacing the published bytes.

Prepare failure creates neither review nor audit state. Publication failure does not report success and creates no PUBLISHED fact. If canonical publication succeeds but its PUBLISHED audit append initially fails, the service returns failure; A2 readback remains closed. A later authorized deterministic retry uses A2's existing recovery path to append only the missing audit fact, read back the unchanged canonical review, and return `AlreadyRecorded`. No delete, overwrite, rollback, or new recovery mechanism was introduced.

## Exact isolated R6 fixture

The test fixture binds the exact R6 evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source SHA `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, provider generation `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6`, generation SHA `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14`, and content SHA `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`.

The isolated authority uses the real A3 chain: registered active purpose, active owner principal, deterministic exact-target resource registration, action vocabulary, purpose-aware `DefaultPermissionPolicy`, and `DefaultPermissionEngine`. In temporary A2 filesystem storage, the review recorded and canonically round-tripped with `HUMAN_REVIEWED_WITH_DISCREPANCY`, descriptive high overall fidelity, exactly two MATERIAL page-1/page-5 `Kellee` occurrences, one systematic pattern, UNKNOWN technical cause, and human source resolution `Kellec`. Exact replay returned `AlreadyRecorded`. No production R6S state was written.

## Tests

Focused command:

`./gradlew test --tests parker.core.runtime.DefaultGovernedHumanFidelityReviewRecordingServiceTest --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **1 suite, 9 tests, 0 skipped, 0 failures, 0 errors**. The suite covers authorization ordering and denial/exception zero-mutation, R6 recording/readback, exact duplicate behavior, same-ID conflict, prepare/publication failure, incomplete publication-audit recovery, missing/mismatched/corrupt readback, and structural absence of provider/projector/correction dependencies.

Full command:

`./gradlew test --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **257 suites, 3,378 tests, 18 skipped, 0 failures, 0 errors**.

## Production, governed-store, and provider verification

Production remained container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, runtime JAR SHA-256 `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`, running with restart count zero and readiness PASS.

All governed-store counts and aggregate hashes remained at the established baseline: evidence 29 (`e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85`), manifests 29 (`ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559`), preparations 6 (`0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d`), capability acceptances 14 (`14eeeb9ceac5361c394c58aeae66ee9b3d6b908717ce06e416c113e0aa97b950`), authorizations 14 (`b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6`), attempts 10 (`798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f`), provider state 8 (`be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6`), generations 23 (`801bdfd3c4b4801ac1981cb48d90ccd1721fdd782d0d54326253de62ecb9b19f`), content 21 (`80eee2bb97f2532b0a1b22e7fbfc6e59c8b8a3d15e98ad24b7969917e5f27435`), evidence audit 1 (`1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59`), and ingestion audit 1 (`3ff333aa80da829ad3454c1d35136a2f24e0997391c1da4a5ef9cc9604429359`). Production governed-store delta: **0**.

OpenAI calls, Claude calls, other provider calls, retries, and external evidence egress were **0 / 0 / 0 / 0 / 0**.

## Deferred work and verdict

Explicitly deferred: every effective-review projector; multi-review conflict resolution; supersession graph traversal and cycles; competing-successor/adjudication interpretation; coverage merging; timestamp/lexical winner logic; source-confirmed eligibility; read-only query/presentation integration; A5+ production composition; deployment; real R6S recording; correction purpose/proposal/acceptance/representation; provider activity; and all Sequence B work.

**A — MINIMUM GOVERNED HUMAN FIDELITY REVIEW RECORDING SERVICE IMPLEMENTED AND VERIFIED**
