# Ordinary Ingestion 11R6V-A3 — Human Fidelity Review Recording Authorization Purpose and Permission Policy

## Status and starting state

**COMPLETE — A3 AUTHORIZATION PURPOSE AND PERMISSION POLICY ONLY**

Host `parker`, branch `main`, starting HEAD/upstream were exactly `c355ba8ee9254bd1925ada7245e018e568f4616b` and the worktree was clean. The frozen Scope Lock SHA-256 was verified as `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`; the frozen implementation-plan SHA-256 was `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`. Completed A1 and A2 identities were respectively `58c03abcd96b62f17f10069825982454fd06e680` and `c355ba8ee9254bd1925ada7245e018e568f4616b`.

## Existing architecture reused

A3 reuses `AuthorizationPurposeId`, the accepted `InMemoryAuthorizationPurposeRegistry` active/retired lifecycle, `PrincipalId`, `HumanFidelityReviewTarget`, `ExecutionRequest.authorizationPurpose`, `ActionVocabulary`, `ResourceRegistry`, `DefaultPermissionPolicy`, and `DefaultPermissionEngine`. It adds no second identity system, general permission framework, registry alias, wildcard purpose, or production composition.

The generic Permission Engine remains authoritative for active-principal, action, resource, and active-purpose policy. A narrow additive evaluator supplies the frozen review-specific checks that the generic contract cannot express: configured-owner equality and exact full A1 target equality.

## Canonical purpose and meaning

The sole new purpose constant is exactly:

`document-ingestion.human-fidelity-review-recording`

`registerPurpose` delegates to the established registry, so first registration is `Registered`, exact duplicate registration is `AlreadyRegistered`, malformed namespace is rejected by existing behavior, retirement remains one-way, and unknown/retired values are inactive.

The purpose authorizes only an attributable human principal's governed prepare/publication of a completed fidelity review fact for an exact source/provider-transcription target, including coverage, formal state, descriptive fidelity, structured discrepancies, classification, severity, human source resolution, cause, patterns, permitted supersession/adjudication facts, and the narrow A2 audit facts.

## Explicit non-authority

The authority contracts and evaluator contain no review/audit storage dependency and perform no mutation. The purpose grants no evidence replacement/deletion, derivative mutation, correction proposal/acceptance/adjudication, corrected representation, fuzzy/global replacement, retranscription, provider invocation/retry, external OCR/reasoning/verification, Memory/Knowledge promotion, source-confirmed promotion, arbitrary audit mutation, or unrelated ingestion operation.

`document-ingestion.human-transcription-correction` was not registered or introduced in production code. Tests prove the review purpose's only registered action cannot authorize representative external-transcription or correction actions, and that the external-transcription purpose cannot authorize review recording.

## Owner policy and exact target binding

`HumanFidelityReviewRecordingAuthorityScope` binds the configured owner `PrincipalId`, exact purpose, and exact `HumanFidelityReviewTarget`. `HumanFidelityReviewRecordingPermissionRequest` separately carries the target proposed for later A4 publication, forcing exact equality before generic permission evaluation.

The target includes evidence ID, source SHA-256, preparation identity, provider generation ID, generation SHA-256, and content SHA-256. A deterministic SHA-256 over this complete tuple produces an opaque target-specific `ResourceId`. The existing Permission Engine can approve only when that exact resource is explicitly registered and resolves through the review action `human-fidelity-review.record` to `WRITE/DOCUMENT`, under the exact active review purpose. It is therefore not an owner-wide “record anywhere” capability: a different but internally self-consistent target remains denied unless its exact derived resource has separately been registered.

The A3 reference policy checks, in order: configured owner; exact non-null purpose; active purpose registry state; authority/proposed-target equality; then the existing Permission Engine. This structure makes authorization-before-mutation possible for A4. Audit is never treated as authority.

## Fail-closed behavior and historical compatibility

Wrong principal, absent/wrong purpose, unknown/retired purpose, any mismatched target component, unregistered exact-target resource, unknown action, and permission-policy denial return explicit denial. Blank/malformed existing identity primitives reject construction. There is no permissive fallback.

All existing purpose values, registry behavior, permission rules, historical authorization records, HumanVerification semantics, A1 contracts, A2 codecs/stores/audit, provider derivative, R6 execution/attempt/provider state, and provider budget remain unchanged.

## Tests

Focused command:

`./gradlew test --tests parker.core.runtime.HumanFidelityReviewRecordingPermissionPolicyTest --tests parker.core.runtime.AuthorizationPurposeRegistryTest --tests parker.core.runtime.DefaultPermissionPolicyTest --tests parker.core.runtime.AuthorizationPurposeEndToEndVerificationTest --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **4 suites, 53 tests, 0 skipped, 0 failures, 0 errors**. This includes 8 new A3 tests plus all focused registry, policy, and end-to-end Authorization Purpose regression tests.

Full command:

`./gradlew test --no-daemon -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: **256 suites, 3,369 tests, 18 skipped, 0 failures, 0 errors**.

## Production, governed-store, and provider verification

Production remained container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image/index `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, runtime JAR SHA-256 `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`, running with restart count zero and readiness PASS.

Governed counts/hashes remain exactly the established baseline: evidence 29 (`e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85`), manifests 29 (`ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559`), preparations 6 (`0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d`), capability acceptances 14 (`14eeeb9ceac5361c394c58aeae66ee9b3d6b908717ce06e416c113e0aa97b950`), authorizations 14 (`b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6`), attempts 10 (`798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f`), provider state 8 (`be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6`), generations 23 (`801bdfd3c4b4801ac1981cb48d90ccd1721fdd782d0d54326253de62ecb9b19f`), content 21 (`80eee2bb97f2532b0a1b22e7fbfc6e59c8b8a3d15e98ad24b7969917e5f27435`), evidence audit 1 (`1d8b0f78663772b52b5490f6051a92ad453e1ca49365bc5eff8b6c301ae98e59`), and ingestion audit 1 (`3ff333aa80da829ad3454c1d35136a2f24e0997391c1da4a5ef9cc9604429359`). Governed-store delta was **0**.

OpenAI calls, Claude calls, other provider calls, retries, and external evidence egress were **0 / 0 / 0 / 0 / 0**.

## A3 boundary confirmation and verdict

No authorized recording service, review storage composition, effective/conflict projector, source-confirmed eligibility service, retrieval/presentation integration, owner endpoint, production composition/configuration, deployment, production authority or review record, correction purpose/policy/capability, Sequence B work, provider operation, or external reasoning was implemented. A4+ was not begun.

**A — HUMAN FIDELITY REVIEW RECORDING AUTHORITY IMPLEMENTED AND VERIFIED**
