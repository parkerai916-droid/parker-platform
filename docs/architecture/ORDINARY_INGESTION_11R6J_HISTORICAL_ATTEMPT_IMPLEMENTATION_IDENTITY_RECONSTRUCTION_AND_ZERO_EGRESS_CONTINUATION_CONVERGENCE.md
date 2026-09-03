# OI11R6J — Historical Attempt Implementation Identity Reconstruction and Zero-Egress Continuation Convergence

## Verdict

**A — HISTORICAL ATTEMPT IDENTITY CORRECTED AND ZERO-EGRESS CONTINUATION STATICALLY CONVERGED**

The R6I failure was independently reproduced and traced to substitution of the current continuation implementation commit into an immutable historical attempt identity. Parker now reads the historical identity from the canonical durable attempt ledger, validates that identity against the persisted provider state, and evaluates current continuation authority separately. Exact copied-state replay of R6-R1 passes without rewriting production state, creating an attempt, or invoking a provider.

## Starting state

The repository was on branch `main`; HEAD and upstream were both `28a7b3fa565eb73854661fbff133810f8e32d44f`; the worktree was clean. OI11R6J made no deployment or production configuration change.

Production remained container `f2a9df159d4305528ff89ac26e2dcc8f5e51fc969839f0d91f204976cb6ed542`, image/index `sha256:a5f5650889bade28aaa7e0ec6d2f9dcdd7dc255be9087e89cd9259d66d53f089`, embedded source `e3fec3fac857e7b0e610375d066d524646a1375f`, runtime JAR `f4971f223612a1791ca6a013bc3234e03d85b9cfbb7fecc09886b40983861d63`, restart count zero, and readiness PASS. The implementation-bound V8 evaluator remained `ACCEPTED` for the current implementation.

## R6I failure preservation

OI11R6I remains an immutable **B — PRE-ADMISSION CONTINUATION FAILURE**. Its sole production continuation returned HTTP 409, `VALIDATION_FAILED — DURABLE_PROVIDER_ATTEMPT_REQUIRED`, before replay or derivative admission. No report or production record was rewritten.

The preserved R6-R1 identities remain:

| Field | Identity |
|---|---|
| Authorization | `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97` |
| Execution | `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976` |
| Historical implementation | `39fe0e777608c96cba20cec491113e77eee4b8ef` |
| Provider-state record | `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7` |
| Raw response SHA-256 | `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a` |
| Provider response ID | `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b` |
| Provider model | `gpt-5.6-sol` |

V8 remains SUCCESS 5/5 in Parker order `[1,2,3,4,5]`; no derivative exists. The historical budget remains maximum 1, consumed 1, retry limit 0.

## Root-cause investigation

The reported cause is accurate. `OrdinaryRequestRegionV8IngestionWorkflow.continuePostEgress` reconstructs the request from the persisted corrected preparation under the current runtime commit. Before this unit, `GovernedRequestRegionV8ExecutionCoordinator.recoverPersistedPostEgress` passed that reconstructed current-runtime `FidelityFirstExecutionIdentity` to `FileSystemFidelityFirstAttemptLedger.open`.

`open` is a create-or-validate operation. The existing ledger is correctly bound to `39fe0e77...`; the reconstructed identity carried `e3fec3fa...`. Its create-once identity check rejected the conflict. Recovery caught the exception and reduced it to `DURABLE_PROVIDER_ATTEMPT_REQUIRED`, despite the durable attempt being present and having reached `PROVIDER_ATTEMPT_STARTED` and `PROVIDER_RESPONSE_RECEIVED`.

Root-cause classification: **historical identity reconstruction/composition defect**. It was not an absent attempt, corrupt provider response, V8 failure, provider-profile defect, or capability-governance defect.

## Historical and current implementation identities

The corrected invariant is:

* `historicalAttemptImplementation` is the repository commit encoded in the immutable canonical attempt ledger that created the execution;
* `currentContinuationImplementation` is the deployed runtime commit evaluated through the current implementation-bound V8 acceptance gate;
* those values may legitimately differ after a corrective deployment;
* neither value may be substituted for the other.

For exact R6-R1 replay, historical identity is `39fe0e777608c96cba20cec491113e77eee4b8ef`, while current continuation identity is `e3fec3fac857e7b0e610375d066d524646a1375f`.

## Historical source of truth

The canonical source is `FileSystemFidelityFirstAttemptLedger.readExisting(executionId)`. It performs read-only canonical decoding and integrity validation and returns the complete historically persisted `FidelityFirstExecutionIdentity`. Historical implementation is not inferred from the runtime, container, deployment configuration, or current acceptance record.

The provider-state record is a second immutable witness. Continuation requires its `implementationCommit` to equal the historical ledger commit. It also requires exact capability/digest, evidence, source digest, execution, correlation/attempt, request digest, and provider-body digest bindings.

## Current continuation authority

Current authority remains separate and precedes historical recovery. `OrdinaryRequestRegionV8IngestionWorkflow.continuePostEgress` requires `OrdinaryRequestRegionV8AcceptanceEvaluator` to return `Accepted` for the current deployed implementation. Tests prove an acceptance for the historical implementation does not authorize a different current implementation; a distinct exact current acceptance is required.

Production authority remains record `e91808b75afca1f49784a0b69ce2840bc9e74e5b35c3f163730909bb40b59866`, SHA-256 `2133bb08f2e265f7969694c3e9566f6cdf25f61c8dc58d256ddc5b32dd8edf28`, for `e3fec3fa...`.

## Implementation correction

`recoverPersistedPostEgress` now:

1. reads the already-persisted provider state;
2. reads the existing attempt ledger by execution ID without creating or replacing it;
3. rejects a missing, malformed, or not-started historical attempt;
4. reconstructs the expected historical identity by changing only the runtime-derived commit to the ledger's persisted commit;
5. requires exact equality with the full persisted attempt identity;
6. requires provider-state implementation to equal the historical ledger implementation;
7. recovers raw/structured state using the historical identity.

The recovery binding check was strengthened to cover implementation, evidence, source digest, execution, attempt/correlation, capability/digest, request digest, and provider-body digest.

The audit also found and corrected a downstream convergence defect: continuation admission previously attempted ledger completion transitions using the current-runtime identity and silently swallowed the resulting historical identity conflict. The continuation path now performs no attempt-ledger transition. Normal first-pass execution retains its existing completion transitions. Thus continuation can add a create-once derivative without rewriting the historical execution/attempt.

No codec, stored record, execution ID, attempt ID, authorization, provider state, capability, evidence, or preparation was changed.

## Fail-closed contradiction handling

Continuation now produces distinct failures for important boundaries:

* missing/not-started attempt: `DURABLE_PROVIDER_ATTEMPT_REQUIRED`;
* malformed ledger: `HISTORICAL_ATTEMPT_INVALID`;
* any historical execution/attempt identity disagreement: `HISTORICAL_ATTEMPT_IDENTITY_MISMATCH`;
* provider-state historical implementation disagreement: `HISTORICAL_PROVIDER_STATE_IMPLEMENTATION_MISMATCH`;
* any provider-state evidence/source/execution/attempt/request/capability disagreement: `RECOVERED_BINDING_MISMATCH`;
* absent current V8 acceptance: current continuation fails before historical recovery with `CAPABILITY_NOT_ACCEPTED`.

The current runtime commit is never used to repair a historical contradiction.

## Historical compatibility

No schema or codec evolution was required. Existing attempt/execution records continue to decode with their original meaning. `readExisting` uses the established ledger codec and checksum validation; malformed or unknown data remains fail-closed. Historical authorizations, attempts, provider state, assessments, derivatives, and capability records are unchanged.

## Exact R6-R1 offline replay

Eleven exact files were copied read-only from production into a temporary isolated fixture: the authorization and event record, attempt ledger, provider state and assessment, corrected-preparation record, and five transport objects. No production path was writable by the test.

The artifact-local test reconstructed the request under current continuation commit `e3fec3fa...`, recovered the historical attempt as `39fe0e77...`, and produced:

| Gate | Result |
|---|---|
| Current V8 authority | ACCEPTED (independently verified) |
| Durable historical attempt | VALID |
| Historical implementation | `39fe0e777608c96cba20cec491113e77eee4b8ef` |
| Current continuation implementation | `e3fec3fac857e7b0e610375d066d524646a1375f` |
| Provider state/raw identity | VALID |
| Raw replay | VALID |
| V8 | SUCCESS, 5/5 |
| Parker order | `[1,2,3,4,5]` |
| Provider profile | `openai-fidelity-first-transcription-v1` |
| Derivative provenance | VALID |
| Provider calls required | 0 |
| Retry required | 0 |
| Admission eligibility | READY |

The exact provider-state record ID, raw SHA-256, provider response ID, model, and request bindings matched their authoritative values.

## Durable attempt and provider-state validation

The exact immutable ledger validates without rewrite, cloning, or replacement. The former `DURABLE_PROVIDER_ATTEMPT_REQUIRED` result no longer occurs for copied exact R6-R1 state. The ledger bytes remain unchanged during recovery tests, and only one ledger file exists.

Provider state remains bound to the original authorization/execution/attempt. No remapping or new provider-state record is performed. Synthetic contradictions between attempt and provider-state implementations, and between any other full attempt identity fields, fail before replay and before provider invocation.

## Complete continuation convergence audit

| Transition | Authoritative source/component | Enforced invariant and failure behavior | Coverage |
|---|---|---|---|
| Current authority | Current runtime commit + V8 acceptance evaluator | Exact current implementation acceptance required | distinct historical/current acceptance test |
| Authorization readback | Versioned exact-envelope authorization store | Evidence, preparation, request, capability, Purpose, provider/profile/model and budget exact | R6A/R6E suite + exact replay |
| Historical execution/attempt | `readExisting(executionId)` and ledger codec | Full immutable identity, historical commit, started call | new mismatch/malformed/missing tests |
| Provider state | canonical provider-state store | Historical implementation and all execution/request bindings exact | new contradiction tests + exact replay |
| Raw replay | stored raw envelope and assessment | No provider dependency; raw-before-parse ordering retained | exact replay + existing R6E tests |
| V8 validation | `RequestRegionV8StructuredValidator` | Five exact regions; missing/duplicate/unexpected fail closed | exact 5/5 and malformed synthetic tests |
| Provider provenance | authoritative outer envelope + authorization | response ID/model authoritative; provider profile authorization-bound | R6E tests + exact replay |
| Derivative provenance | binder and `OrdinaryRegionTranscriptionDerivative` | Evidence through provider/capability/Purpose bindings complete | existing R6E convergence + exact replay |
| Admission | canonical derivative admission | create-once, idempotent, conflict fail-closed | existing idempotence/conflict tests |
| Historical ledger after continuation | immutable attempt ledger | no continuation transition or rewrite | implementation separation + byte-stability test |

The full remainder audit found no further known structural blocker. Continuation remains unable to invoke a provider, cannot create a new attempt, preserves maximum 1/consumed 1/retry 0, and admits only through canonical create-once derivative storage after deterministic validation.

## Capability and governance impact

This is an implementation/composition correction around historical recovery and continuation admission. It does not change the provider-neutral V8 transcription contract or expand owner authority.

Capability remains `ordinary-external-request-region-transcription-v8`; digest remains `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`. A new artifact, owner artifact acceptance, deployment, and implementation-bound acceptance remain required before another production continuation decision.

## Tests

Focused convergence tests: **21 tests, 0 failures, 0 errors, 1 skipped**. The skip is the deliberately property-gated exact copied-state fixture. The exact copied R6-R1 fixture was then run separately with its isolated root: **1 test, 0 failures, 0 errors, 0 skipped**.

Full `./gradlew test` with bounded heap: **251 suites, 3,326 tests, 0 failures, 0 errors, 18 skipped**. `git diff --check` passed.

Added coverage proves historical/current implementation separation, read-only historical ledger recovery, full identity mismatch rejection, attempt/provider-state mismatch rejection, malformed/missing attempt rejection, separate current acceptance, zero provider calls, exact 5/5 R6-R1 replay, provider provenance, call-budget preservation, and existing create-once admission behavior.

## Production and provider accounting

Read-only post-test production counts and aggregate hashes match the authoritative R6I pre/post baseline for evidence (29), manifests (29), capability acceptances (13), authorizations (14), attempts (10), provider state (8), derivative generations (22), derivative content (20), evidence audit (1), and document-ingestion audit (1). The exact corrected-preparation record remains SHA-256 `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`.

Exact historical hashes remain unchanged:

* authorization events `64070a0d042373164902615edd03a19a4bdf7f602e911d7eecbcec8c31bcb675`;
* authorization `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544`;
* attempt ledger `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591`;
* assessment `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01`;
* raw provider state `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c`.

Production governed-store delta was **0**. OI11R6J made OpenAI calls **0**, Claude calls **0**, other provider calls **0**, retries **0**, and external evidence egress **0**. The historical R6-R1 OpenAI call remains the sole call; no retry is authorized.

## Final decision

`HISTORICAL_ATTEMPT_IDENTITY = CANONICAL_PERSISTED_IDENTITY`

`CURRENT_CONTINUATION_AUTHORITY = SEPARATELY_REQUIRED`

`R6_R1_OFFLINE_CONTINUATION_ELIGIBILITY = READY`

`PRODUCTION_CONTINUATION_EXECUTED = NO`

`PRODUCTION_DERIVATIVE_ADMITTED = NO`

Verdict: **A — HISTORICAL ATTEMPT IDENTITY CORRECTED AND ZERO-EGRESS CONTINUATION STATICALLY CONVERGED**.
