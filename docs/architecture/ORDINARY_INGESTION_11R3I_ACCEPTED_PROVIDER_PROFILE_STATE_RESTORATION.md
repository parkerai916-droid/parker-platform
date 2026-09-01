# Ordinary Ingestion OI11R3I — Accepted Provider Profile State Restoration

## Starting state

- Host: `parker`
- Repository: `/home/steve/parker-platform`
- Branch: `main`
- HEAD/upstream: `4281393ea9f878a5c51c3ad7ffaeacc407178143`
- Worktree: clean
- Production: stopped; container `ae25e09f07498f502650e75fd47278a3de6bfac9f48e043cddb966a216c8e1dc`, restart count `7`
- Accepted image: `sha256:0cefbbab93e33cd066e27ce5dc1d35bb8b0e5601323e4d3d88fc39b030e31fc9`
- Store counts: `4 / 2 / 1 / 21 / 19 / 6 / 5`

## Profile and authority

The exact profile is `/home/steve/.config/parker/openai-external-transcription.properties`, identity `openai-fidelity-first-transcription-v1`, provider `OpenAI`. Its restored pre-transition digest was `a1c0d3998bd865e64758cb71e8f30221f50e61555dd96f4f66749bfd64542b40` and state `ACCEPTANCE_PENDING`. Steven's prior explicit acceptance of this same identity and non-secret configuration remained applicable; no material profile field had changed.

The rollback copy remained intact at `/mnt/parker-data/parker/provider-profile-backups/oi11r3f-openai-fidelity-first-transcription-v1-20260901.acceptance-pending.properties` with the same digest.

## Diagnostics and mutation

Before mutation, the authoritative non-egress diagnostic reported all storage,
identity, profile-structure, non-stale, and credential predicates PASS, with
`providerProfileAccepted=false`, reason `PROFILE_NOT_ACCEPTED`, and ordinary
execution readiness blocked by lifecycle.

The sole authorized mutation was:

```diff
-acceptanceState=ACCEPTANCE_PENDING
+acceptanceState=ACCEPTED
```

The resulting profile digest is the previously accepted value:
`3038538d53b98595631c76325062688b40c449d512bb94cae17be2e7f0d6e956`.
Provider, model, credential location, capability-related configuration,
processing identity, and all other fields are unchanged.

After mutation, the diagnostic reported:

```text
acceptanceStoragePresent=true
acceptanceStorageComplete=true
providerStateStoragePresent=true
buildIdentityPresent=true
buildIdentityMatches=true
providerProfilePresent=true
providerProfileStructurallyValid=true
providerProfileAccepted=true
providerProfileNonStale=true
providerProfileReady=true
credentialPresent=true
credentialStructurallyValid=true
ordinaryExecutionReady=true
overallReady=true
reasons={}
```

## Composition versus ordinary execution

`overallReady` is the existing region-composition readiness decision. It may be
true while the profile is pending because Parker composes the acceptance lane
without granting ordinary provider execution. `ordinaryExecutionReady` adds
the lifecycle acceptance predicate and is the relevant readiness result for
ordinary execution. Before restoration it was false; after restoration it is
true. No authorization or execution is granted by diagnostics alone.

## Fail-closed verification

The existing evaluator still rejects invalid, stale, missing, or malformed
profiles; `externalTranscriptionBackendReadiness` still requires
`ACCEPTED` plus a valid credential for ordinary execution. The production guard
continues to consume the shared structured evaluation and retains its existing
fail-closed behavior.

## Integrity and egress

Production was not started, recreated, or deployed. The container remained
stopped at restart count `7`; the accepted image was unchanged. Store counts
before and after remained `4 / 2 / 1 / 21 / 19 / 6 / 5`. OpenAI, Claude,
external-provider, OCR/transcription, and retry counts were all zero. No
evidence, provider-state, memory, knowledge, or provenance content changed.

Focused diagnostic tests and the full suite passed (248 suites, 3,298 tests,
0 failures, 0 errors; JUnit hostnames `parker`). `git diff --check` passed.

## OI11R3 status

The provider-profile readiness prerequisite is now satisfied statically for the
exact accepted artifact. OI11R3 remains a separate deployment unit and was not
retried here.

