# OI11R6E — Provider-Profile Derivative Provenance Correction and Persisted Post-Egress Continuation Convergence

## Verdict

**A — PROVIDER-PROFILE DEFECT CORRECTED AND ZERO-EGRESS POST-EGRESS CONTINUATION STATICALLY CONVERGED**

The R6-R1 failure was reproduced from code and persisted records, corrected at its semantic source, and replayed offline from an exact copy of the durable R6-R1 state. No production state was mutated and no provider path was invoked.

## Starting state and preserved R6-R1 truth

Repository branch `main`, HEAD and upstream were exactly `cb385d87b8784261c459558eb5114900669df630`; the worktree was clean. R6-R1 remains verdict C, not a successful execution. Its immutable boundary is:

* authorization `ff286fdcc38a35aefed16201724c00d8a9930e2f73c08206571295a664127f97`;
* execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`;
* provider state `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`;
* raw SHA-256 `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`;
* response `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`, model `gpt-5.6-sol`;
* V8 assessment SUCCESS, 5/5, Parker order `[1,2,3,4,5]`;
* derivative not admitted; historical OpenAI calls 1, retries 0.

The critical production file hashes remained: authorization `48f4e5405fd298df9c492cd2cab95f65ea9abdfe2546cb951de5f0c9f0cd5544`, attempt ledger `bff24b6c97ecd5382e514a0dc57f1f28c40509c0d483ba553d2db4d03b5d7591`, raw provider record `c3f1ea29c9f1b4e76b886a33fdfd7a84400a4b96250d9899905493c844c9620c`, assessment `bace05830d9a2872dbcbb78d3bc73b192dcfb179925def756fc6684604b87d01`, and corrected preparation record `fe1111c75ee0307f755e03ac479dbb51ac7b9af7a4eeed715b9e6d1fffc18ae9`.

## Exact failure reproduction, taxonomy and root cause

The authorization v2 record canonically binds external provider profile `openai-fidelity-first-transcription-v1`. The version-3 derivative contract requires that exact value. However, `OrdinaryRequestRegionV8IngestionWorkflow.payload` passed `capability.profile`, whose value is `request-region-fidelity-acquisition-v4`, into `OrdinaryRegionTranscriptionDerivative.providerProfile`. Construction therefore failed closed before admission.

The profiles are distinct:

| Concept | Canonical value | Provenance location |
|---|---|---|
| acquisition/request-region processing | `request-region-fidelity-acquisition-v4` | V8 capability processing/acquisition configuration |
| corrected preparation | `full-page-achromatic-png-preparation-v1`, version 1 | persisted preparation and exact authorization envelope |
| external provider | `openai-fidelity-first-transcription-v1` | exact authorization envelope |

Root-cause classification is **A — derivative constructor mapping defect**. The persisted authorization, provider state, codec and authoritative provider envelope were correct. The defect was the final constructor mapping, not string corruption or provider behavior.

## Implementation correction and contradiction handling

The derivative provider profile now comes only from the exact version-2 authorization through `requestRegionV8DerivativeProviderProfile`. It requires the accepted external-provider profile. Acquisition profile remains in its existing processing/acquisition field; preparation identity/profile/version remain separate derivative provenance fields. Provider `OpenAI`, model `gpt-5.6-sol`, response ID, capability/digest and Authorization Purpose retain their existing authoritative sources.

No independently persisted attempt-level external-provider-profile field exists. The erroneous call-site parameter was derived, not historical truth, so removing it is safe. A malformed or contradictory authorization fails in its constructor/codec or exact execution-envelope revalidation; it is not overwritten. No derivative codec change was required. Historical derivative versions retain their recorded meaning and readback.

Changed implementation surfaces are:

* `src/runtime/OrdinaryRequestRegionV8Execution.kt`: authoritative provider-profile mapping, recovery-only coordinator operation, exact continuation eligibility, and shared validated admission;
* `src/runtime/OrdinaryRegionIngestion.kt`: provider-free continuation port;
* `src/composition/ParkerRuntime.kt`: narrow runtime continuation entry point;
* `src/composition/OwnerEvidenceHttpServer.kt`: authenticated `POST /owner/admin/region-transcription-continuation` operation accepting only evidence, authorization, execution and provider-state identities;
* `src/composition/Main.kt`: composition wiring;
* `tests/runtime/OrdinaryRequestRegionV8ExecutionConvergenceTest.kt`: profile, recovery, exact replay and idempotence coverage.

## Persisted-state replay and continuation operation

An exact read-only copy of the R6-R1 authorization, attempt ledger, raw provider state, assessment and corrected preparation was placed under `/tmp` for the bounded forensic test. The test reconstructed the canonical request from preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, then invoked only `recoverPersistedPostEgress`. It proved:

* authorization/request/preparation bindings intact;
* raw and assessment state canonical and intact;
* request digest `2f4f595decb924fd6d252735494dabc85b8e375c4d17e41f952195061e2675a3` and body digest `5a847355cab3217a8b1309ca82dc47f2d38239395e6cdede0108fe85c53f6603` unchanged;
* deterministic V8 parse and validation 5/5;
* Parker page order `[1,2,3,4,5]`;
* exact outer response ID/model enrichment;
* external provider profile `openai-fidelity-first-transcription-v1`;
* provider callback count 0;
* derivative binding/provenance valid through the pre-admission boundary.

The recovery-only coordinator method has no call to attempt-start or transport. It requires an exact raw provider-state match plus a durable attempt ledger whose provider-attempt marker is already consumed. Missing raw state, missing durable attempt, binding mismatch, malformed state or failed V8 validation fails closed.

The separately governed production continuation operation after a later artifact/deployment gate is:

`POST /owner/admin/region-transcription-continuation`

Its authenticated request must contain exactly `evidenceId`, `authorizationId`, `executionId`, and `providerStateId`. It reloads all canonical state server-side and can only parse, validate and attempt create-once derivative admission. It cannot invoke a provider, create an authorization, create an execution or reset call budget.

## Eligibility, idempotence and call budget

Continuation requires an exact v2 authorization, its original reserved execution, non-revoked state, exact evidence/source/preparation/request/capability/Purpose/provider/profile/model/budget binding, durable provider-attempt marker, exact provider-state identity, and successful deterministic recovery. The existing derivative admission remains create-once: identical replay returns the canonical existing admission; conflicting content/provenance fails closed. Tests proved identical double admission creates no duplicate and conflicting same-key content is rejected.

For R6-R1, consumed calls remain 1 of 1 and retry limit remains 0. Continuation neither consumes nor resets these values. Any transport callback during recovery is a test failure.

## Complete post-egress convergence audit

| Transition | Component / authoritative source | Enforced invariant and failure |
|---|---|---|
| authorization → execution | v2 authorization store + reserved snapshot | exact envelope/execution; mismatch or revocation fails |
| execution → provider state | attempt ledger + V8 provider-state store | one started attempt and exact state identity required |
| raw readback → parse | provider-state codec | raw exists before assessment; malformed state fails |
| parse → enrichment | V8 envelope adapter | outer response ID/model fill nulls; contradiction fails |
| enrichment → validation | V8 structured validator | exact five-region identities/cardinality/provenance |
| validation → ordering | V8 derivative binder | Parker request order authoritative; provider ordinal forensic-only |
| profile resolution | exact authorization | provider profile is authorization-bound, never acquisition/preparation-derived |
| provenance → admission | v3 derivative + create-once admission | complete provenance required; conflict fails; exact replay idempotent |

No further known structural blocker was found in this path.

## Capability and governance impact

This is provenance composition and safe continuation of an already-authorized, already-consumed, durably persisted provider result. It does not alter the provider-neutral V8 transcription contract:

* capability `ordinary-external-request-region-transcription-v8`;
* digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`.

Production continuation still requires a separately accepted artifact, deployment, any required implementation-bound acceptance, and explicit governance for the exact continuation. This unit creates none of those authorities.

## Verification

Focused suite: 16 tests, zero failures/errors, including the exact copied-state R6-R1 offline acceptance test. Full bounded-heap command `./gradlew test -Dorg.gradle.jvmargs=-Xmx4g -Dkotlin.daemon.jvm.options=-Xmx4g` passed 251 suites / 3,321 tests, zero failures, zero errors, 18 skipped. `git diff --check` passed.

## Production and provider accounting

Production remained container `9011f1e9e9feaa5d0662c295cc295c19b0dce45fb4e8c2a1dad95fbe12f52fa5`, image `sha256:73c2fda48df7d846a6c59a39864f75905be9de7176cdb31cc330cda6558a7406`, running with restart count 0. The authenticated evaluator remained `ACCEPTED` for source `39fe0e777608c96cba20cec491113e77eee4b8ef` and the unchanged V8 identity/digest. Physical store-file counts remained evidence 29, manifests 29, corrected-preparation records 1, capability acceptances 12, authorization files 9, attempt ledgers 5, provider-state files 8, derivative generations 22 and derivative content 20. The corresponding governed logical state and all R6-R1 historical records remained unchanged.

OI11R6E activity: OpenAI 0, Claude 0, other providers 0, retries 0, external evidence egress 0, production derivative admission 0, governed production-store delta 0. The historical R6-R1 OpenAI total remains exactly 1 with zero retries. **NO RETRY IS AUTHORIZED.**

## Stop state

The implementation and offline convergence work are complete. No deployment, production continuation or production derivative admission occurred. The next work is a separate candidate build/preservation and owner artifact-acceptance gate, followed by deployment and any required implementation-bound capability acceptance before a separately governed continuation from the preserved R6-R1 state.
