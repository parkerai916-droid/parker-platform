# OI11R3A — production build-identity binding investigation

## Result

OI11R3 was not complete because the candidate failed Parker's fail-closed
startup identity gate. No provider call or production store-content mutation
occurred. The failed container was `17b19463f65beb22117185d34010369644717b3666c58b7937ee512941c7fd8b`,
using candidate image `sha256:2475b2765c0f75731d8ada46272d8a55553ea0c85e98dd6ad620da00cf5db836`;
it restarted 11 times and was then stopped.

## Identity path

`Dockerfile` accepts `PARKER_BUILD_COMMIT`; `build.gradle.kts` writes it into
the executable manifest as `Parker-Source-Commit`. `ParkerRuntime` reads that
manifest as its embedded build identity. `ParkerRuntimeConfigLoader` loads
`PARKER_SOURCE_COMMIT`, `PARKER_PRODUCTION_COMMIT`, and
`PARKER_DEPLOYED_IMMUTABLE_IMAGE_ID`; `ParkerRuntime.buildAndRegisterRuntimeGraph`
requires the configured production commit to equal the embedded commit before
composing region execution and throws `InvalidConfiguration` otherwise.

The exact rejection was:

`Invalid Parker Runtime configuration for key 'PARKER_REGION_PROVIDER_STATE_STORAGE_ROOT': region execution requires complete acceptance storage, matching build identity, ready provider profile, and credential`.

The active production override
`/home/steve/.config/parker/docker-compose.fa-a1r.yml` supplied stale
`PARKER_SOURCE_COMMIT=ea1d96d656e97c7ed350eeabec5ef279b8ac36bb`,
`PARKER_PRODUCTION_COMMIT=ea1d96d656e97c7ed350eeabec5ef279b8ac36bb`, and
`PARKER_DEPLOYED_IMMUTABLE_IMAGE_ID=sha256:6fa813f9f4c454652329cfb0ec08399055be360ff959fe1de71f98bb41b7e256`.
Those override-file values take precedence over shell values supplied during
the attempted promotion. The candidate manifest contained
`Parker-Source-Commit: ae63687eb5bea5832ff3c4904920540150da202a`, so startup
correctly rejected it.

The last successfully deployed baseline was container
`281bba01fa82ddd4a172a424688845ea180a6dfe28eb4ae2aebd9c064ecd68ca`, image
`sha256:fdb583d16d99a58d13983046b2ad8b936014ead6b6c22cdf0d670b895b071521`,
with the prior deployment metadata recorded as source/production commit
`def611a8bf8cb6c2297f1d9bf6cd8146a58d4cbc`. Its persistent stores were
`4 / 2 / 1 / 21 / 19 / 6 / 5`.

## Determinism probe

Two non-production builds from commit `ae63687eb5bea5832ff3c4904920540150da202a`
produced image digests `2475b2765c0f75731d8ada46272d8a55553ea0c85e98dd6ad620da00cf5db836`
and `42ada4bdf5e63c0654505e8170aa039b7e26de04bdabcbd1b750d79499c96a9a`.
The packaged manifest in the latter retained the exact source commit. Thus
image-level reproducibility is not established (likely packaging/creation
metadata), but this did not cause the observed rejection: the direct mismatch
was configured production commit `ea1d…` versus embedded commit `ae63687…`.
No build-system change is authorized here.

## Governance and remedy

The check is intentionally fail-closed. Acceptance/build identity is
deployment metadata plus persisted acceptance/authority state, not decorative
configuration; startup must not auto-learn or wildcard a new image.

**Remedy 5 — configuration/deployment defect.** Before retrying OI11R3, the
deployment-local Compose metadata must be explicitly advanced to the exact
candidate source/build commit and promoted immutable image digest, and the
existing governed production acceptance/authority mechanism must create or
advance the corresponding exact-build record. This is an explicit authorized
promotion action, not a startup edit or manual store rewrite. Existing
historical records remain immutable; rollback must restore the prior metadata
and image as one bounded operation.

Retry prerequisites: exact source commit verified; candidate image selected;
override metadata agrees with embedded commit/image; required acceptance and
authority records are explicitly promoted for that exact build; preflight,
tests, mounts, and provider-egress gates pass; then deploy with no volume
deletion and verify startup/readback.

No source changes, acceptance-state changes, deployment retry, provider call,
or production data mutation occurred in OI11R3A.
