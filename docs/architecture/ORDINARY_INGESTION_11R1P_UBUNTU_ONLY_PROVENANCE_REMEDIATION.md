# OI11R1P Ubuntu-Only Provenance Remediation

## Scope and historical finding

This record remediates the OI11R1 implementation-provenance exception without rewriting history or changing accepted V8 semantics.

Commit `d0e7e117f6bf99357a483957887efd33226bc27f` was committed and pushed from Ubuntu host `parker`, repository `/home/steve/parker-platform`. Its three changed files had first been staged or edited in `C:\Users\steve\AppData\Local\Temp\oi11r1-isolation` and copied to Ubuntu. No Windows Git checkout, WSL checkout, OneDrive checkout, Windows test execution, Windows commit, or Windows push occurred. Production was untouched.

Affected files:

- `scripts/verify-v8-replay-isolated.sh`
- `tests/runtime/OrdinaryRequestRegionV8AcceptanceHarnessTest.kt`
- `tests/runtime/OrdinaryRequestRegionV8ReplayIsolationTest.kt`

Historical commit `d0e7e117` remains unchanged. Its nonconforming staging path is explicitly recorded here.

## Ubuntu-native byte verification

Direct verification on `parker` reproduced the committed files exactly:

| File | SHA-256 | Git blob |
|---|---|---|
| replay script | `09ca8d77f086b3286c33373729fc7d4aeeb6c30d3a81df76f241085fe3f0a521` | `aa1eb39c83f511a36a3944f9c633d5256d5674b6` |
| acceptance harness | `2a90f736a4aef075376f669c25dd3113f8684ddf8b0f8cca16a761b72f9b6775` | `d3e91532412519647e0ee5b533ab5ae09d8efcc7` |
| attribution regression | `6878169a05de77a8b692ac4d6ed8317163b32675fa7584314cff687c5c96e8df` | `c3f7f0809d2c0bf891a20949d9236e1492716ee7` |

## Independent requirement review

| Requirement | Ubuntu-native code finding |
|---|---|
| Fixture-specific attribution | Result path includes the exact A/B/C fixture and the record repeats that fixture. |
| Invocation-specific identity | The required invocation ID is validated, persisted, and compared exactly. |
| Checksum protection | The result envelope carries and verifies `record_sha256` over canonical record bytes. |
| Create-once semantics | Existing result paths fail before execution and `CREATE_NEW` writes prevent replacement. |
| Stale-result rejection | Expected and recorded invocation IDs must be identical. |
| Cross-fixture rejection | Expected and recorded fixture identities must be identical; the regression test exercises directed substitutions and equality makes rejection symmetric. |
| Missing-result rejection | Missing or unreadable fixture results fail closed. |
| Deterministic concurrency prohibition | The wrapper takes a repository-scoped lock before Gradle starts. |
| Nonblocking lock / exit 75 | `flock -n` rejects competition with exit code 75. |
| Provider-free replay | The wrapper fixes the harness action to `REPLAY`; no execute or provider action is selected. |

Review result: PASS. No substantive defect was found.

## Ubuntu-native verification

Fresh isolated replay ran entirely on `parker` from `/home/steve/parker-platform`:

- A: PASS; result SHA-256 `2aa58fdbc6e76bdd0c3c0b373c00098303eb64cf42d9eefefddf1f53f66cf2bb`.
- B: PASS; result SHA-256 `9da548856d2c6d7808422010d2c8e8999b0785a7a52efb05c2e550e0f51539a9`.
- C: PASS; result SHA-256 `6b4884b10fa6e260b9d55fca0e88f9d2c6efc615b6836911273d61d7fde48377`.

Typed evidence reconstruction: PASS, SHA-256 `34ec3c703aacb754c45fa58ddf941d7368e2b4cc2e373cb412eb99c4de30902b`. A/B/C are PASS; the evidence represents three historical provider calls and zero retries. No provider call occurred during remediation.

Cross-attribution regression: PASS for wrong fixture, stale invocation, and missing result. Concurrency verification: the competing invocation was rejected before Gradle, exited 75, and created no colliding result.

Full suite on `parker`: 246 suites, 3,286 tests, 0 failures, 0 errors, 17 skipped. All 246 generated JUnit XML files record hostname `parker`. `git diff --check`: PASS.

## Semantics and production preservation

Accepted V8 semantics were not changed. Capability `ordinary-external-request-region-transcription-v8` retains digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`. Typed evidence was not regenerated or mutated. No historical Git commit was rewritten.

Production remained unchanged:

- image `sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`;
- container `281bba01fa82ddd4a172a424688845ea180a6dfe28eb4ae2aebd9c064ecd68ca`;
- restart count 0;
- governed store counts 4 / 2 / 1 / 21 / 19 / 6 / 5;
- OpenAI calls 0; Claude calls 0; Sprint 2 provider attempts 0.

## Continuation boundary

All future Parker implementation, source editing, testing, building, committing, and deployment continues exclusively in the authoritative Ubuntu Parker environment unless Steven explicitly changes that authority. Missing convenience tooling does not authorize crossing the environment boundary.

Exact next step: OI11R2 V8 durable provider-state and guarded-execution path convergence, Ubuntu-only.
