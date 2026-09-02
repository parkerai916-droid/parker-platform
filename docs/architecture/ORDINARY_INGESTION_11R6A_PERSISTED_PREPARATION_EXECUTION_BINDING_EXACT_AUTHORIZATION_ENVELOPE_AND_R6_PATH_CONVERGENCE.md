# OI11R6A — Persisted-Preparation Execution Binding, Exact Authorization Envelope and R6 Path Convergence

## Verdict

**A — R6 EXECUTION PATH IMPLEMENTED AND STATICALLY CONVERGED**

The two OI11R6 pre-egress defects are corrected. V8 execution now constructs its request exclusively from canonical readback of the selected durable corrected preparation. A version-2 exact authorization envelope commits to and enforces the complete execution boundary. The downstream raw-state, validation and derivative path was traced and exercised with synthetic five-page/fake-provider fixtures. No further known structural blocker remains.

## Starting state and R6 root causes

The repository began on branch `main`, with HEAD/upstream both `45df58f0a64af947139cb69206e96987b929d146` and a clean worktree. OI11R6 remains immutable verdict-B evidence: production execution regenerated preparation from source, while its version-1 evidence authorization lacked preparation/request/Purpose/provider-profile/model/budget bindings.

Production remained container `8b7c4b9b9f1b374de278e37d2f01c8401bc8ab809516d21135ddebf1e8065d7c`, image `sha256:d33a5a47f8a540bf11375c2fd373d5bf3257f36da5f0f4afb444bbf3ce46f9cb`, source `a031c92549fd7a3b8c92f6917be0e59b61ca5fde`, running with restart count zero. R6A did not deploy, restart or modify production configuration.

## Implementation surface

Changed implementation files:

* `src/runtime/FullPageAchromaticPreparation.kt`
* `src/runtime/OrdinaryRequestRegionV6.kt`
* `src/runtime/OrdinaryRequestRegionV8TruthfulContract.kt`
* `src/runtime/OrdinaryRequestRegionV8Execution.kt`
* `src/composition/ParkerRuntime.kt`
* `src/interfaces/OrdinaryRegionDerivative.kt`
* `src/runtime/DerivativeContentCodec.kt`
* `src/runtime/OrdinaryRegionIngestion.kt`

Changed focused-test files:

* `tests/runtime/FullPageAchromaticPreparationTest.kt`
* `tests/runtime/OrdinaryRequestRegionV8ExecutionConvergenceTest.kt`

## Persisted-preparation execution correction

`FileSystemFullPageAchromaticPreparationStore.findExact` resolves one and only one preparation for exact evidence, source digest and profile/version. Every candidate is decoded through the canonical codec and its content-addressed transport bytes are re-read and compared. Missing, malformed, conflicting, mismatched or incomplete state fails closed.

`FullPageAchromaticCanonicalRequestRegionV8Builder.buildPersisted` constructs complete V8 request regions directly from the decoded preparation regions. It preserves page representation ID, source provenance, bounds, region identity, crop digest, transport digest/bytes and deterministic order. It does not render source pages, invoke the preparer, inspect PDF bytes or consult R5F geometry.

`OrdinaryRequestRegionV8RequestPreparer` now requires the corrected-preparation store. Both authorization preflight and execution read the persisted preparation; the source-rendering `builder.build(...)` path is absent from this execution preparer. Production composition injects the same durable corrected-preparation store used by the preparation-only service. No regeneration fallback exists.

## Exact authorization envelope and identity

Authorization format 2 binds:

| Boundary | Bound fields |
|---|---|
| Evidence | evidence ID, source SHA-256 |
| Preparation | identity, `full-page-achromatic-png-preparation-v1`, version 1 |
| Request | request digest, provider-body digest, correlation/request identity |
| Capability | capability ID and digest |
| Purpose | `ExternalTranscriptionInvocationGate.AUTHORIZATION_PURPOSE`, resolving to `evidence-intelligence.external-transcription` |
| Provider | OpenAI, `openai-fidelity-first-transcription-v1`, `gpt-5.6-sol` |
| Budget | maximum calls 1, automatic retries 0 |
| Reasoning | external reasoning false |
| Governance | approving owner and validity interval |

The canonical authorization identity uses domain `parker.request-region-v8.exact-owner-authorization.v2` and length-prefixed hashing over every security-relevant bound field and approving owner. Individual changes to evidence, source, preparation, request, capability, Purpose, provider, profile, model, call limit or retry limit invalidate construction or produce a different identity.

The owner/admin authorization operation remains metadata-free from the browser: Parker derives the exact envelope from governed custody, persisted preparation, canonical request and frozen capability/provider configuration. Client-supplied substitutions are impossible.

## Historical authorization compatibility

The authorization codec reads historical `.request-region-v8-owner-authorization-v1` files exactly as version-1 records. Version 2 uses a distinct filename/codec and complete field set. Version-1 grants retain null exact-envelope fields and are explicitly rejected by V8 execution with `EXACT_AUTHORIZATION_REQUIRED`; they are never reinterpreted or promoted. Unknown versions have no accepted path and fail closed.

## Execution-time validation and immutable request

Immediately before reservation/provider-attempt creation, execution reconstructs from the authorized preparation and authorized correlation identity, then requires exact equality for preparation identity/profile/version, request digest, body digest, correlation ID, capability/digest, Purpose, provider/profile/model, call maximum, retry limit and reasoning prohibition.

Region reorder, transport-byte/digest change, preparation substitution or digest-affecting metadata change changes canonical request/body identity and fails this comparison before the provider boundary. The request authorized is therefore the request executed; rebuilding a merely similar request is insufficient.

## Call and retry budget

Format 2 admits only `maximumProviderCalls=1` and `automaticRetryLimit=0`. The durable authorization reservation permits one execution identity. The durable attempt ledger records `PROVIDER_ATTEMPT_STARTED` before guard release. Re-entry with durable provider state recovers without transport; re-entry after an indeterminate/no-state started attempt fails closed. Fake-provider tests proved both successful and malformed first responses cause exactly one transport call across repeated execution attempts. No automatic or manual retry path is introduced.

## Raw-before-parse, validation and five-page fake-provider result

The downstream order remains:

`provider response → persistReceived(raw) → parse/enrich → V8 validate → recordAssessment → derivative bind → admission`

`OpenAiRequestRegionV8ProviderExchange` persists status, raw bytes, raw SHA-256, request/body/manifest binding and provider-state identity before parsing. A malformed response remains recoverable raw evidence and produces no valid derivative.

A synthetic five-page corrected preparation was persisted and canonically read back. `buildPersisted` produced exactly five request regions in `[1,2,3,4,5]`, using identical transport bytes. A controlled fake OpenAI envelope returned five blocks in reversed provider order and non-authoritative ordinals. Validation enriched the response ID/model from the authoritative envelope, retained provider order for forensics, and reconstructed Parker order `[1,2,3,4,5]`. Existing focused cases also reject malformed JSON, missing envelope ID, conflicting envelope/structured provenance, model conflict, missing/duplicate/unexpected region and invalid cardinality.

## Derivative provenance and historical codec compatibility

Exact-envelope derivative representation version 3 adds durable:

* preparation identity/profile/version;
* provider-body digest;
* Authorization Purpose;
* maximum calls and retry limit;
* external-reasoning prohibition.

The existing derivative already binds evidence/source, page and region/transport identities, request digest, authorization, execution/attempt, provider-state/raw identity, response identity, provider/model, capability/digest and capability acceptance. Version 3 therefore contains every R6-required provenance link. Its codec round-trip is exact and producer identity is `3.0.0`.

Historical derivative representations 1 and 2 retain their existing decoding and producer semantics; neither gains fields it never persisted. Unknown versions fail closed.

## Complete R6 convergence audit

| Transition | Component / persisted record | Enforced invariant and failure | Verification |
|---|---|---|---|
| Owner decision → authorization preflight | `OrdinaryRequestRegionV8IngestionWorkflow`, custody resolver | exact governed PDF, accepted capability, persisted preparation required | focused exact-envelope/store tests |
| Preparation lookup → readback | `FileSystemFullPageAchromaticPreparationStore` record + transport PNGs | codec, identity, evidence/source/profile, transport bytes, unique match | persisted five-page test |
| Readback → request | `buildPersisted`, V8 codec | exact geometry/order/bytes, size and cardinality; no renderer/preparer fallback | five-page construction test |
| Request → authorization | authorization v2 codec | all envelope fields identity-bound; create-once | mutation matrix and v1/v2 readback tests |
| Authorization → execution | workflow exact revalidation + authorization guard | every actual field equals authorized field; v1 rejected | constructor mutation matrix and static trace |
| Execution → provider boundary | attempt ledger + execution coordinator | one persistent attempt, zero retry, recovery without second call | success/failure re-entry fake tests |
| Provider return → raw state | provider-state v1 store | raw persisted before parse and hash/bindings recoverable | raw-before-parse tests |
| Raw → parse/validate | exchange, envelope enrichment, V8 validator | 5/5 IDs/pages/provenance; envelope authoritative; ordinals forensic | five-page and malformed matrix |
| Valid result → Parker order | derivative binder | request order authoritative, complete set only | reversed provider-order test |
| Provenance → admission | derivative v3 codec and admission | complete envelope/source/provider provenance persists; create-once admission | v3 round-trip plus admission suite |

No additional predictable structural blocker was found.

## Capability identity impact

This is execution composition, authorization and durable-provenance hardening around the provider-neutral contract. The V8 schema, instructions, request semantics, provider validation, raw-before-parse behavior and response reconstruction are unchanged.

Capability remains `ordinary-external-request-region-transcription-v8`; digest remains `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`. Tests assert the exact digest. A new implementation-bound acceptance will nevertheless be required after eventual artifact acceptance/deployment.

## Tests

Focused command:

`./gradlew test --tests 'parker.core.runtime.OrdinaryRequestRegionV8ExecutionConvergenceTest' --tests 'parker.core.runtime.FullPageAchromaticPreparationTest' --tests 'parker.core.runtime.OrdinaryRequestRegionV8TruthfulContractTest' -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: 3 suites, 24 tests, 0 failures, 0 errors, 3 skipped.

Full command:

`./gradlew test -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g`

Result: 251 suites, 3,316 tests, 0 failures, 0 errors, 17 skipped. The bounded 4 GiB Gradle/Kotlin heap matched prior Parker verification practice.

## Production and provider accounting

Production store counts remained exactly: evidence 29; manifests 29; corrected-preparation files 6; capability acceptances 11; execution authorization files 11; attempts 8; provider-state files 6; derivative generations 22; derivative content 20. Delta for every production store was zero.

The registered Deed was not read into tests, prepared, authorized, executed or transmitted. OpenAI calls: 0. Claude calls: 0. Other external calls: 0. Retries: 0. External evidence egress: 0.

No artifact was accepted or deployed, production remained unchanged, and no implementation-bound production authority was created.

UNIT ORDINARY-INGESTION-11R6A COMPLETE — THE REAL-DOCUMENT V8 EXECUTION PATH NOW CONSUMES THE CANONICAL PERSISTED CORRECTED PREPARATION RATHER THAN REGENERATING PREPARATION FROM SOURCE, AND THE EVIDENCE-SPECIFIC AUTHORIZATION ENVELOPE BINDS AND ENFORCES THE EXACT PREPARATION, REQUEST, CAPABILITY, AUTHORIZATION PURPOSE, PROVIDER, PROFILE, MODEL, PROVIDER-CALL LIMIT AND RETRY LIMIT. EXECUTION-TIME REVALIDATION FAILS CLOSED ON ANY BOUNDARY MISMATCH. THE COMPLETE R6 PATH HAS BEEN TRACED THROUGH PROVIDER INVOCATION, RAW-BEFORE-PARSE PERSISTENCE, V8 VALIDATION, PROVENANCE AND DERIVATIVE ADMISSION USING CONTROLLED FAKE-PROVIDER TESTS, WITH NO REMAINING KNOWN STRUCTURAL BLOCKER. HISTORICAL RECORDS REMAIN COMPATIBLE AND IMMUTABLE. THE V8 CAPABILITY IDENTITY AND DIGEST REMAIN UNCHANGED. NO PRODUCTION DEPLOYMENT, REAL-EVIDENCE EXECUTION, PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. A SEPARATE ARTIFACT BUILD, OWNER ACCEPTANCE, DEPLOYMENT AND IMPLEMENTATION-BOUND CAPABILITY ACCEPTANCE ARE REQUIRED BEFORE OI11R6 MAY BE RETRIED.
