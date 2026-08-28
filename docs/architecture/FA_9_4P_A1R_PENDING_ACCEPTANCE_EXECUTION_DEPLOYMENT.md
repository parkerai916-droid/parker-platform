# FA.9.4P-A1R pending-acceptance execution deployment requirements

This unit adds code and offline proofs only. It does not deploy, create a real authority, read real evidence, or contact OpenAI.

## Production identity

The production JAR manifest must contain `Parker-Source-Commit`, baked by the Gradle `jar` task from `PARKER_BUILD_COMMIT`. The Docker build must therefore receive the exact clean source commit as the `PARKER_BUILD_COMMIT` build argument. Runtime configuration must also provide the same value as `PARKER_PRODUCTION_COMMIT`. The acceptance lane is not composed unless the configured and embedded identities are equal and both are exact lowercase 40-character Git commits.

## Persistent stores

Before a later deployment, create these SSD-backed host directories as the runtime container UID/GID and make them owner-readable and owner-writable:

- `/mnt/parker-data/parker/external-transcription-acceptance-authorities`
- `/mnt/parker-data/parker/external-transcription-attempts`

Mount them read-write at:

- `/data/external-transcription-acceptance-authorities`
- `/data/external-transcription-attempts`

Set these non-secret environment values:

- `PARKER_FIDELITY_FIRST_ACCEPTANCE_AUTHORITY_STORAGE_ROOT=/data/external-transcription-acceptance-authorities`
- `PARKER_FIDELITY_FIRST_ATTEMPT_STORAGE_ROOT=/data/external-transcription-attempts`
- `PARKER_PRODUCTION_COMMIT=<exact deployed commit>`

All three must be configured together. The authority store uses create-once records with bounded fields and a SHA-256 checksum. The attempt store uses locked, forced, atomically replaced ledgers. A missing, unreadable, unwritable, corrupt, conflicting, or unmounted store fails closed before provider transmission.

## Operational boundary

The existing bearer-authenticated owner server exposes only:

`POST /owner/evidence/acceptance-executions/{authorityId}`

The body is ignored. Source IDs, model names, profiles, configuration, and attempt identities cannot be supplied by the caller; the server resolves them from the immutable authority. Ordinary `transcribe-external` remains blocked while the profile is `ACCEPTANCE_PENDING`.

The next deployment unit must build from a clean pushed commit, mount both new stores, set the three non-secret values, retain the existing secret injection without displaying it, verify ordinary execution remains blocked, and verify the acceptance endpoint reports a missing authority without making a provider request. Only a later separately authorised unit may create a real authority or execute it.
