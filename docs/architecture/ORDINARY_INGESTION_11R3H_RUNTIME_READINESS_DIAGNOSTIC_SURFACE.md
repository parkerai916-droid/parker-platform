# Ordinary Ingestion OI11R3H — Runtime Readiness Diagnostic Surface

## A. Starting state

- Host: `parker`
- Repository: `/home/steve/parker-platform`
- Branch: `main`
- HEAD/upstream: `eb296b539d1667907287b1948981ef3d46ae114a`
- Worktree: clean before implementation
- Production: stopped, candidate image `sha256:0cefbbab93e33cd066e27ce5dc1d35bb8b0e5601323e4d3d88fc39b030e31fc9`
- Provider profile: `openai-fidelity-first-transcription-v1`, `ACCEPTANCE_PENDING`, SHA-256 `a1c0d3998bd865e64758cb71e8f30221f50e61555dd96f4f66749bfd64542b40`

## B. Previous observability gap

The production exception at `ParkerRuntime.buildAndRegisterRuntimeGraph`:

`region execution requires complete acceptance storage, matching build identity, ready provider profile, and credential`

collapsed several independent conditions into one message. OI11R3G could not
identify the false condition without starting production.

## C. Authoritative predicate map

`RuntimeReadinessDiagnostic` evaluates the same inputs consumed by the region
composition guard:

- acceptance authority/attempt storage present and complete;
- provider-state storage present;
- configured build identity present and equal to embedded runtime identity;
- provider profile present and structurally valid;
- profile accepted and non-stale;
- provider profile readiness;
- credential present and structurally valid;
- overall composition readiness.

Pending lifecycle remains visible separately from composition readiness: the
acceptance lane may compose while ordinary provider execution remains gated by
the existing `ACCEPTED` lifecycle requirement.

## D. Implementation

Added `src/composition/RuntimeReadinessDiagnostic.kt` containing the structured
non-secret result, shared manifest identity lookup, and bounded
`RuntimeReadinessDiagnosticCli` entry point. `ParkerRuntime` now consumes the
diagnostic's `overallReady` for the region guard while retaining its existing
acceptance-lane semantics. No provider or persistence code was changed.

Added `tests/composition/RuntimeReadinessDiagnosticTest.kt` covering accepted,
pending, missing-credential, build-mismatch, invalid-profile, and secret
non-disclosure cases.

## E. Startup equivalence and secret safety

The production guard uses `RuntimeReadinessDiagnostic.fromEvaluated` over the
already computed profile readiness and the same configuration/identity values;
diagnostics do not implement a second approximate readiness rule. The CLI only
reports booleans and bounded reason codes. Credentials are represented solely
as presence/structural-validity booleans and never serialized.

## F. Verification

Focused diagnostic tests passed. Full suite passed:

- 248 suites
- 3,298 tests
- 0 failures
- 0 errors
- JUnit XML hostnames: `parker`

`git diff --check` passed.

The production diagnostic was invoked without starting Parker by loading the
stopped candidate's environment, translating only container paths to their
host-mounted equivalents, and supplying the verified candidate embedded source
identity. Result:

```text
acceptanceStoragePresent=true
acceptanceStorageComplete=true
providerStateStoragePresent=true
buildIdentityPresent=true
buildIdentityMatches=true
providerProfilePresent=true
providerProfileStructurallyValid=true
providerProfileAccepted=false
providerProfileNonStale=true
providerProfileReady=true
credentialPresent=true
credentialStructurallyValid=true
overallReady=true
reasons={providerProfileAccepted=PROFILE_NOT_ACCEPTED}
```

Thus the exact false predicate is `providerProfileAccepted`; it is the
intentional restored `ACCEPTANCE_PENDING` execution-authorization state. The
composition predicates are true and no startup mutation was attempted.

## G. Integrity and egress

Production remained stopped at restart count `7`, with image identity unchanged.
Store counts remained `4 / 2 / 1 / 21 / 19 / 6 / 5`. OpenAI, Claude, external
provider, OCR/transcription, and retry counts were all zero. No profile,
acceptance, credential, provider-state, evidence, memory, knowledge, or
provenance state was mutated.

## H. Remedy and retry status

The smallest next remedy is a separately governed provider-profile acceptance
operation (or an explicit existing acceptance authority record) if ordinary
execution is to be enabled. This diagnostic does not advance that state.

`OI11R3 RETRY NOT AUTHORIZED` remains in force. The diagnostic is programme
instrumentation with a reusable operational-assurance shape; it grants no
execution authority.

