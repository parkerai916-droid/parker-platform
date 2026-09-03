# Ordinary Ingestion 11R6V-A2 — Governed Human Fidelity Review Durable Storage, Canonical Codec and Narrow Audit

## Status and starting state

**COMPLETE — A2 DURABILITY, CODEC, AND NARROW AUDIT IMPLEMENTATION ONLY**

Host `parker`, branch `main`, starting HEAD and upstream were exactly `58c03abcd96b62f17f10069825982454fd06e680`; the worktree was clean. The frozen Scope Lock SHA-256 was verified as `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`, and the frozen implementation-plan SHA-256 as `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`. A1 implementation identity was exact starting commit `58c03abcd96b62f17f10069825982454fd06e680`.

## Storage-pattern investigation and reuse

The implementation examined `HumanVerificationStorage`, `HumanVerificationRecordCodec`, `FileSystemHumanVerificationStorage`, derivative generation/content storage, `DocumentIngestionAudit`, its filesystem implementation, and Parker's established bounded explicit binary codecs and create-once prepare/publish conventions.

A2 reuses those conventions: explicit magic/version fields, bounded strings and collections, strict enum decoding, trailing-byte rejection, normalized opaque identifiers, staged files, durable channel writes, atomic create-once moves, canonical byte comparison, deterministic ordering, and fail-closed corruption. It does not broaden `DocumentIngestionAudit`, introduce a persistence framework, or alter historical storage semantics.

The sole additive A1 support is an internal persisted-facts reconstruction boundary for `FidelityDiscrepancyLocation`. It invokes the existing private constructor and every A1 validation invariant; it adds no relaxed or alternative domain semantics.

## Canonical codec and integrity

`HumanFidelityReviewRecordCodec` defines representation version 1 with magic `PHFR`. Its explicit canonical payload contains the complete A1 review target, reviewer and review time, artifact provenance digests, coverage and character scopes, formal state, descriptive fidelity, discrepancy identities and exact locations, original substrings and digests, classification, severity, reason/detail, source resolution, cause, pattern associations, supersession, and adjudication reference.

Collections are canonically ordered before encoding, so input iteration order and restart do not affect bytes. The stored envelope binds magic, version, payload length, canonical payload, and SHA-256 integrity over magic + version + payload. Decode bounds the total representation, payload, every string, and every collection before allocation; rejects malformed UTF-8, missing or unknown enum values, unsupported versions, truncation, trailing bytes, and integrity failure; then reconstructs exclusively through validated A1 contracts. It re-derives the review, nested discrepancy, pattern, substring, and source-value identities rather than trusting serialized identifiers.

The audit codec uses the same strict approach with version 1 and magic `PHFA`. Neither codec uses Java/Kotlin native object serialization or persists storage paths.

## Review storage contract and filesystem layout

`HumanFidelityReviewStorage` exposes only `prepare`, `publishPrepared`, `retrieve`, and `listForExactTarget`. It has no update, delete, latest-by-time, winner, correction, or projection operation. Exact duplicate states have explicit deterministic results; same identity with different canonical content fails closed.

The reference filesystem implementation requires an existing writable root and creates only test/reference-local `.tmp` and `.prepared` subdirectories. Canonical names are safe opaque `review-<sha256>.human-fidelity-review-v1` files. Files and directories are restricted to owner access where POSIX permissions are available. Temporary data is forced to durable storage before an atomic move to preparation; publication is an atomic create-once move to the canonical namespace. Existing corrupt state is never overwritten or repaired.

No production directory, mount, configuration, or composition was created. All durability verification used isolated JUnit temporary directories.

## Prepare, publish, query, and recovery semantics

`prepare` durably stages canonical bytes before emitting its PREPARED fact. `publishPrepared` requires the matching PREPARED fact, publishes create-once, and emits PUBLISHED only after canonical publication succeeds. Canonical retrieval requires the matching exact PUBLISHED audit fact. If publication succeeds but audit append fails, readback remains fail-closed with an incomplete-audit result; deterministic publication retry records the missing fact without republishing or changing canonical bytes.

Retrieval supports exact review identity and exact full target (evidence/source/preparation/generation/generation digest/content digest). Results are sorted by review identity. There is deliberately no timestamp authority, fuzzy lookup, evidence-only selection, supersession choice, or conflict resolution in storage.

Clean restart, prepared-but-unpublished isolation, published restart readback, exact duplicate preparation/publication, conflicting same-ID state, unknown identity, corrupted and truncated records, unsupported versions, orphan preparations, and audit-recovery behavior were verified. Recovery never auto-publishes, invents audit success, deletes history, overwrites corruption, or chooses a review by time.

## Narrow human-fidelity governance audit

The dedicated `HumanFidelityGovernanceAudit` is append/create-once and read-only. Its closed A2 vocabulary is `REVIEW_PREPARED`, `REVIEW_PUBLISHED`, and `REVIEW_DUPLICATE_CONFIRMED`; no speculative correction/Sequence B events were added. Each record binds a deterministic event identity, factual event type and outcome, timestamp, actor `PrincipalId`, exact review ID and target, and canonical review-payload SHA-256, with only optional bounded factual detail.

Audit records have their own versioned canonical codec, integrity verification, safe opaque filename, durable write, atomic create-once publication, exact duplicate handling, deterministic per-review ordering, restart durability, and fail-closed corruption/version behavior. They do not mutate review state and contain no generated narrative.

Cross-store atomicity is not claimed. The truthful ordering and explicit incomplete-audit recovery described above prevents a false publication-success claim and prevents unaudited canonical state from becoming readable.

## Supersession, conflict, and historical compatibility

The codec and store preserve validated predecessor and adjudication facts exactly. They do not calculate a winner, traverse a graph, resolve conflicts, apply timestamp precedence, or project effective state; those services remain assigned to later units.

Historical `HumanVerificationRecord` version 1, `HumanVerificationStorage`, `HumanVerificationOutcome`, `DerivativeReviewRegistry`, derivative stores, provider transcription, and R6 execution/attempt/provider-state records were unchanged. No migration ran and the new capability uses a separate, currently uncomposed namespace.

## Exact R6 fixture durability

The isolated fixture binds the exact R6 evidence, source, preparation, generation, generation SHA, content SHA, reviewer and review-artifact provenance. After prepare, publish, and a fresh storage instance, it read back as `HUMAN_REVIEWED_WITH_DISCREPANCY` with descriptive high fidelity, exactly two location-bound MATERIAL `Kellee` occurrences on pages 1 and 5, both resolved by the human reviewer to `Kellec`, one two-member systematic pattern, and UNKNOWN technical cause. Exact page/region/block bindings and source/provider strings survived canonical roundtrip. No real R6S review was imported or recorded.

## Tests

Focused command:

`./gradlew test --tests parker.core.runtime.HumanFidelityReviewRecordCodecTest --tests parker.core.runtime.FileSystemHumanFidelityReviewStorageTest --tests parker.core.runtime.FileSystemHumanFidelityGovernanceAuditTest --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **3 suites, 20 tests, 0 skipped, 0 failures, 0 errors** (codec 8, review storage 7, audit storage 5).

The focused coverage includes deterministic and collection-order-independent encoding, roundtrip, strict malformed/version/count/length/trailing/integrity rejection, nested identity contradictions, non-BMP code-point locations, supersession/adjudication, prepare/publish/restart, duplicate/conflict behavior, exact-target ordering, orphan preparation, non-overwrite, truthful audit sequencing, failed-publication recovery, and audit durability.

Full command:

`./gradlew test --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **255 suites, 3,361 tests, 18 skipped, 0 failures, 0 errors**.

## Production, governed-store, and provider verification

Production remained container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, and runtime JAR SHA-256 `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`. It remained running with restart count zero; the existing startup/readiness state remained PASS with one normal startup and no crash/restart activity.

Before/after governed-store counts and aggregate hashes were identical: evidence 29 (`e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85`), manifests 29 (`ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559`), corrected preparations 6 (`0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d`), capability acceptances 14 (`14eeeb9ceac5361c394c58aeae66ee9b3d6b908717ce06e416c113e0aa97b950`), authorizations 14 (`b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6`), attempts 10 (`798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f`), provider state 8 (`be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6`), derivative generations 23 (`801bdfd3c4b4801ac1981cb48d90ccd1721fdd782d0d54326253de62ecb9b19f`), derivative content 21 (`80eee2bb97f2532b0a1b22e7fbfc6e59c8b8a3d15e98ad24b7969917e5f27435`), evidence audit 1 (`1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59`), and ingestion audit 1 (`3ff333aa80da829ad3454c1d35136a2f24e0997391c1da4a5ef9cc9604429359`). Governed-store delta: **0**.

OpenAI calls, Claude calls, other provider calls, retries, and external evidence egress were **0 / 0 / 0 / 0 / 0**.

## A2 boundary confirmation and verdict

No Authorization Purpose or permission change, authorized recording service, effective-state projector, conflict-resolution service, downstream eligibility integration, owner/Tier A presentation change, `DocumentAnalysisCoordinator` change, production composition/configuration, deployment, production storage root, real R6S review, correction capability, corrected representation, Sequence B implementation, provider operation, or external reasoning was implemented.

**A — GOVERNED HUMAN FIDELITY REVIEW DURABILITY AND NARROW AUDIT IMPLEMENTED AND VERIFIED**
