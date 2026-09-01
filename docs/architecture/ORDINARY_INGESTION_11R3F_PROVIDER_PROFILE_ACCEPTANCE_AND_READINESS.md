# Ordinary Ingestion OI11R3F — Provider Profile Acceptance and Readiness

## Verdict

PASS — the exact OpenAI provider profile was explicitly accepted by Steven and
advanced from `ACCEPTANCE_PENDING` to `ACCEPTED`. Production was not started,
restarted, or deployed, and no provider request occurred.

## Starting state

- Host: `parker`
- Repository: `/home/steve/parker-platform`
- Branch: `main`
- HEAD/upstream: `9625a50ae6cb08c1c97f641c4e1f6c860d3d085c`
- Worktree: clean
- Accepted artifact: `sha256:0cefbbab93e33cd066e27ce5dc1d35bb8b0e5601323e4d3d88fc39b030e31fc9`
- Production container remained stopped (`eda413b055c2713c7db97070280a25c23c357df46048d3ab983e06b8966e34c4`), restart count `9`.

## Provider profile

- Path: `/home/steve/.config/parker/openai-external-transcription.properties`
- Mounted storage root: `/run/parker-config`
- Identity: `openai-fidelity-first-transcription-v1`
- Provider: `OpenAI`
- Schema: structurally valid provider-profile schema
- Owner/mode: `steve:steve`, `0644`
- Prior SHA-256: `a1c0d3998bd865e64758cb71e8f30221f50e61555dd96f4f66749bfd64542b40`
- Prior lifecycle: `ACCEPTANCE_PENDING`
- New SHA-256: `3038538d53b98595631c76325062688b40c449d512bb94cae17be2e7f0d6e956`
- New lifecycle: `ACCEPTED`

The profile's provider, model selection, credential location, capability-
related fields, processing identity, limits, and review metadata were retained.
Only the lifecycle field changed.

## Governance and runtime rule

`OpenAiExternalTranscriptionProviderReadinessEvaluator` performs structural and
review-date validation. `externalTranscriptionBackendReadiness` requires the
profile lifecycle to be `ACCEPTED` and a structurally valid credential before
reporting backend `Ready`. `ParkerRuntime.buildAndRegisterRuntimeGraph` then
requires a ready profile, credential, complete acceptance storage, and matching
build identity; otherwise it throws the existing fail-closed
`InvalidConfiguration` exception.

The prior `ACCEPTANCE_PENDING` state was therefore correctly non-ready. This
unit advanced the accepted profile state, not the readiness rule. Pending,
malformed, stale, missing, or otherwise unaccepted profiles remain rejected.

## Authority and transition

Steven explicitly authorized this exact profile, path, identity, and prior
digest to transition to `ACCEPTED`, while excluding arbitrary future profiles,
wildcards, credential changes, provider execution, and readiness bypasses.
No dedicated profile-transition command was present; the profile is deployment-
local operational state, so the minimal authorized mutation was applied
directly to its single `acceptanceState` property.

Rollback evidence was preserved before mutation at:

`/mnt/parker-data/parker/provider-profile-backups/oi11r3f-openai-fidelity-first-transcription-v1-20260901.acceptance-pending.properties`

Rollback SHA-256:
`a1c0d3998bd865e64758cb71e8f30221f50e61555dd96f4f66749bfd64542b40`

The pre/post diff is exactly:

```diff
-acceptanceState=ACCEPTANCE_PENDING
+acceptanceState=ACCEPTED
```

## Verification

- Focused existing tests passed:
  `OpenAiExternalTranscriptionProviderProfileTest` and
  `OpenAiApiCredentialTest`.
- The resulting profile parses with lifecycle `ACCEPTED`.
- Provider identity remains `OpenAI`.
- Profile identity remains `openai-fidelity-first-transcription-v1`.
- Production image/source/artifact bindings were not changed.
- Production remained stopped; no container recreation occurred in this unit.
- Store counts remained `4 / 2 / 1 / 21 / 19 / 6 / 5`.
- OpenAI calls: `0`; Claude calls: `0`; external provider calls: `0`; retries: `0`.
- No evidence, provider-state, memory, knowledge, or provenance content changed.
- No credentials or secret values were exposed or committed.

## OI11R3 readiness

The provider-profile prerequisite that previously blocked startup is now
satisfied statically for this exact profile. OI11R3 remains a separate unit;
this unit did not start or deploy Parker and did not perform provider execution.

## Repository change

Only this governance report was added. No source, tests, build files, Compose
files, or provider credentials were changed in Git.

