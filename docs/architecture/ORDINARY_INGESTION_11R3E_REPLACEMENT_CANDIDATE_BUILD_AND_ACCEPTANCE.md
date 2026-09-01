# OI11R3E — replacement candidate build, preservation, and acceptance

## Accepted source and verification

The replacement was built from detached clean checkout `/tmp/oi11r3e-exact`
at accepted OI11R2 source commit
`ae63687eb5bea5832ff3c4904920540150da202a`. The full suite passed before the
build: 247 suites, 3,292 tests, 0 failures, 0 errors, 17 skipped; JUnit host
was `parker`. Environment was Ubuntu `parker`, Java 17.0.20, Gradle 8.10,
Docker 29.7.2, buildx 0.36.1.

Build command:

```text
PARKER_BUILD_COMMIT=ae63687eb5bea5832ff3c4904920540150da202a docker compose --env-file /home/steve/parker-platform/.env -f docker-compose.yml build parker
```

The packaged runtime JAR SHA-256 is
`571e455bc6e2ddcabaf75470b8d6d154a0f1187902f67de125733d8f8f659009` and its
manifest contains `Parker-Source-Commit: ae63687eb5bea5832ff3c4904920540150da202a`.

## Concrete artifact identities

The single candidate is image ID/digest
`sha256:0cefbbab93e33cd066e27ce5dc1d35bb8b0e5601323e4d3d88fc39b030e31fc9`.
Its OCI manifest digest is
`sha256:cf9a64d894ff9119d32685a9a1e8e4e8fe33467e865d1a3af5a8ad7c84c603cc`;
OCI config digest is
`sha256:79c6290a8b55ce719aaf37c042a20984f02e915a08beb0cd9692bdd62fe27432`.
The accepted capability is
`ordinary-external-request-region-transcription-v8`, digest
`c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`.

## Preservation and owner acceptance

The image was saved before acceptance to:

`/mnt/parker-data/parker/replacement-candidates/oi11r3e-replacement-ae63687-20260901.tar`

Archive size is 867,012,096 bytes, mode 600, owner `steve:steve`, SHA-256
`f81fc6e3273c5dab12cf7278a64f7bad6d428de2d17f597c12d49310ff17b0af`.
The Docker save manifest and config references were inspected and match the
candidate image.

Steven explicitly accepted the exact source, JAR, image, OCI, capability,
archive, and provisional-evidence identities presented at the owner
checkpoint. Durable acceptance record:

`/mnt/parker-data/parker/replacement-candidates/oi11r3e-artifact-acceptance-0cefbbab93e3-v1.json`

Record SHA-256:
`b4f677d25f4b50ca50852f5e4a0e78f0401744b2afac65a1da8f781281a89e3a`.

## Deployment binding and integrity

Without starting Parker, `PARKER_DEPLOYED_IMMUTABLE_IMAGE_ID` in
`/home/steve/.config/parker/docker-compose.fa-a1r.yml` was advanced from the
old image to the accepted candidate digest. Source and production commits
remain `ae63687eb5bea5832ff3c4904920540150da202a`. A rollback copy was
preserved at `/home/steve/.config/parker/docker-compose.fa-a1r.yml.oi11r3e-preimage`
with SHA-256 `c3ecfe5f507426d84c6a9b763d51bd2bb31fdcd16d48ff0cc52733cd0d9d842b`.
Post-change override SHA-256 is
`0fcf5e6bff80dec3582b1bcf7895456f00d80a200a7250ee117bbd261407fd25`.
Compose rendering passed and resolved all three identity fields exactly.

Production was not deployed, started, restarted, or recreated. The existing
container remained stopped (restart count 11). Persistent store counts remained
`4 / 2 / 1 / 21 / 19 / 6 / 5`; no provider, OCR, transcription, evidence,
memory, knowledge, or provenance mutation occurred. Provider calls and retries
were zero.

## Readiness

The replacement artifact is built, fully verified, immutably preserved,
explicitly accepted by Steven, durably recorded, and bound in deployment
metadata. Production deployment has not occurred. OI11R3 may now be retried
against this exact accepted candidate.
