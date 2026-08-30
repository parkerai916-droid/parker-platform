# ORDINARY-INGESTION-2R1 exact build provenance recovery

## Outcome

Production was restored on implementation commit `363201a2233d29240571781ced0e78dfbc6680e1` and immutable image `sha256:f6e674e0f56f405de55ea73ba97850aaf8ec9c38727fbc924d5cdf08166f2da1`. The runtime is stable with zero restarts and reports `CAPABILITY_NOT_ACCEPTED`, while the authenticated governed promotion boundary is available. No promotion POST, authorization, or provider operation occurred.

## Starting incident and repository

- HEAD/upstream: `363201a2233d29240571781ced0e78dfbc6680e1`; clean.
- Failed OI2R image: `sha256:27b150d275a112675aca39f8c788bbec825f6941eb4f46e46c9de06882e39015`.
- Incorrect embedded identity: `363201ae52f1ccfab6e53a8199a3701caf09e020`.
- Failed production container: `4501e6f80909bbe294360d2e1c1c70f5d4c2da6cdabc90a21a111ec32c395194`, deliberately stopped at restart count 38 during recovery.
- Exact intended implementation: `363201a2233d29240571781ced0e78dfbc6680e1`.
- Source code changed in R1: **NO**.

## Identity-source audit

Sixteen field-level identity sources were inspected. Historical documentation and test constants were non-executable and not defects.

| Source | Location / field | Observed before correction | Expected | Use | Stale | Action |
|---|---|---|---|---|---|---|
| Git | repository `HEAD` | `363201a2233...` | same | source | no | authoritative input |
| Build shell | `PARKER_BUILD_COMMIT` | previously mistyped `363201ae52f1...` | `363201a2233...` | build | yes | derive from `git rev-parse HEAD` |
| Base Compose | `build.args.PARKER_BUILD_COMMIT` | shell interpolation | exact HEAD | build | no | retained |
| Dockerfile | `ARG PARKER_BUILD_COMMIT` | build input | exact HEAD | build | no | retained |
| Dockerfile | build-stage `ENV PARKER_BUILD_COMMIT` | build input | exact HEAD | build | no | retained |
| Gradle | JAR `Parker-Source-Commit` | build input | exact HEAD | runtime provenance | no | verified from JAR |
| FA override | deployed image ID | C3 image `e14234a...` | recovery image | runtime | yes | superseded by R1 override |
| FA override | source commit | C3 `1694a6a...` | exact HEAD | runtime | yes | superseded by R1 override |
| FA override | production commit | C3 `1694a6a...` | exact HEAD | runtime | yes | superseded by R1 override |
| Rejected R override | image ID | rejected `27b150d...` | recovery image | runtime candidate | yes | not used |
| Rejected R override | source commit | mistyped `363201ae...` | exact HEAD | runtime candidate | yes | not used |
| Rejected R override | production commit | mistyped `363201ae...` | exact HEAD | runtime candidate | yes | not used |
| R1 override | image ID | `f6e674e...` | same | runtime | no | active |
| R1 override | source commit | `363201a2233...` | same | runtime | no | active |
| R1 override | production commit | `363201a2233...` | same | runtime | no | active |
| Runtime JAR | embedded source commit | `363201a2233...` | same | runtime equality | no | verified |

The rejected image embedded the wrong commit because a manually transcribed, syntactically valid but nonexistent 40-character value was supplied as `PARKER_BUILD_COMMIT`. The FA override independently pinned C3 runtime values. Neither originated in production source or acceptance state.

## Deployment-local correction

The active stack was:

1. `/home/steve/parker-platform/docker-compose.yml`
2. `/home/steve/.config/parker/docker-compose.openai-enablement.yml`
3. `/home/steve/.config/parker/docker-compose.fa-a1r.yml`
4. `/tmp/docker-compose.oi2r1.yml`

The fourth, deployment-only file overrides the immutable image, source commit, and production commit with the exact recovery values. It contains no secrets and is not committed. Existing mounts and Docker/containerd configuration were unchanged.

The build used `PARKER_BUILD_COMMIT="$(git rev-parse HEAD)"` after asserting HEAD equals the required commit, with a no-cache Compose build. Before deployment:

- repository source = `363201a2233d29240571781ced0e78dfbc6680e1`;
- build input = `363201a2233d29240571781ced0e78dfbc6680e1`;
- embedded JAR identity = `363201a2233d29240571781ced0e78dfbc6680e1`;
- provenance equality: **PASS**.

Deployment used `up -d --no-deps --no-build --pull=never --force-recreate parker` with the four-file stack.

## Recovered production

- Container: `399dcd70c0a31aae0c6b9d01aaa9347760fc779d0b0bcf63705ee6f05057639a`.
- Image: `sha256:f6e674e0f56f405de55ea73ba97850aaf8ec9c38727fbc924d5cdf08166f2da1`.
- Embedded commit: exact recovery implementation.
- Stable state: running, restart count 0 after the observation interval.
- Root HTTP: 200.
- Unauthenticated admin GET: 401.
- Authenticated admin GET: 200, `CAPABILITY_NOT_ACCEPTED`, accepted commit `null`.
- Startup logs: `Runtime started`; prior provider-state configuration fatal error absent.
- Promotion boundary: composed and reachable through authenticated GET; POST invocation count 0.
- Fixture workflow: `NO_ELIGIBLE_CAPABILITY`, `executeAvailable=false`, native searchable text `PRESENT`; therefore non-executable before acceptance.

## Preservation and zero-egress proof

- Existing C3 acceptance count: 1.
- Existing acceptance SHA-256: `58f803531959888dc24397ad62ed6b64d9ed0997e0ca1ef7785c20e3bfe410f3` (unchanged).
- Owner authorizations: 0.
- Attempt files: 4 (unchanged).
- Provider-state files: 2 (unchanged).
- Region authorities: 1 (unchanged).
- Derivative generations/content: 21/19 (unchanged).
- Fixture evidence SHA-256: `ce8bd4b53d8b007026575974014e71f648f045bf3970b0e984605cf842a7b4a5` (unchanged).
- OpenAI calls: 0; Claude calls: 0; promotion POSTs: 0.
- Disk: 738 GB total, 652 GB available, 7% used.

## Verification baseline and next boundary

The unchanged implementation/test-lock commit had already passed 3,248 tests across 236 suites with 0 failures, 0 errors, and 9 skips; `git diff --check` passed. R1 introduced no source changes.

The next separately authorized unit is `ORDINARY-INGESTION-2C`: one-shot governed acceptance of the running recovery build, followed by live read-only proposal verification. This recovery did not perform that action.
