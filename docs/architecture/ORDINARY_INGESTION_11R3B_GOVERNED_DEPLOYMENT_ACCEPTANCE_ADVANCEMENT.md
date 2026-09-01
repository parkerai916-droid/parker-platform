# OI11R3B — governed deployment-acceptance advancement

## Starting state

Execution remained on Ubuntu host `parker` in `/home/steve/parker-platform`,
branch `main`, at HEAD/upstream
`6237d9676112e73c363a671f0962c6e311953087`; the worktree was clean. The
OI11R3A report hash was verified as
`533bd5408c820d0f1604462341e16afd0c787f6e5fac9c9a951988de3d3ce9d4`.

The Parker container was already stopped from the failed OI11R3 attempt
(container `17b19463f65beb22117185d34010369644717b3666c58b7937ee512941c7fd8b`,
restart count 11). OI11R3B did not start, restart, recreate, or deploy it.

## Acceptance target and authority

The independently established target is the accepted OI11R2 source identity
`ae63687eb5bea5832ff3c4904920540150da202a`, confirmed by the OI11R2 and
OI11R3A reports, Git history, and the candidate JAR manifest. The deployment
override is operational state, not source-controlled repository state. Steven's
unit instruction is the explicit authority to advance it. Repository search
found no dedicated promotion command for this override, so the minimum direct
operational update was authorized.

## Mutation and rollback evidence

Override: `/home/steve/.config/parker/docker-compose.fa-a1r.yml` (owner
`steve:steve`, mode `664`). Pre-change SHA-256:
`6f9324c6928323e87a9ead5b78c2dcd53d4a1c0923e9bc936d512dd442432bf2`.
A preserved rollback copy is
`/home/steve/.config/parker/docker-compose.fa-a1r.yml.oi11r3b-prechange`,
with the same SHA-256.

Only these two coordinated fields changed:

```text
PARKER_SOURCE_COMMIT:     ea1d96d656e97c7ed350eeabec5ef279b8ac36bb -> ae63687eb5bea5832ff3c4904920540150da202a
PARKER_PRODUCTION_COMMIT: ea1d96d656e97c7ed350eeabec5ef279b8ac36bb -> ae63687eb5bea5832ff3c4904920540150da202a
```

Post-change SHA-256 is
`c3ecfe5f507426d84c6a9b763d51bd2bb31fdcd16d48ff0cc52733cd0d9d842b`.
The image digest, mounts, provider configuration, secrets, topology, and
restart policy were unchanged.

## Static verification and integrity

The full existing Compose configuration rendered successfully with the
updated override; it resolves both commit fields exactly to `ae63687…`.
The Parker runtime configuration-loader tests passed. Existing code still
requires exact commit equality and rejects missing or malformed identities;
the trust rule was not changed or weakened.

No service was started. The container remained exited with restart count 11.
Persistent store counts remained the recorded baseline `4 / 2 / 1 / 21 / 19 /
6 / 5`; no provider state, transcription, evidence, memory, or knowledge
content was written. OpenAI, Claude, external-provider calls, and retries were
zero. `git diff --check` passed.

All acceptance-state prerequisites for retrying OI11R3 are now satisfied:
the deployment source/build identity is explicitly aligned to the accepted
OI11R2 source. OI11R3 itself must perform the later image selection and
deployment verification; it was not retried here.
