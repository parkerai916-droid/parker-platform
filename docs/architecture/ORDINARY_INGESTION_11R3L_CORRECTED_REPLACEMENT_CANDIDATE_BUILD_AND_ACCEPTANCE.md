# OI11R3L — Corrected Replacement Candidate Build and Acceptance

## Result

PASS — the corrected replacement candidate was built from the exact source
commit `854910c4cb695f2b74db2b1b4d0779e7b58676c6`, verified, preserved, and
explicitly accepted by Steven. Production was not deployed.

## Source and historical lineage

The fresh archive-based exact-source checkout was created from
`854910c4cb695f2b74db2b1b4d0779e7b58676c6` (40 characters), without changing
`main`. The earlier OI11R3K candidate remains preserved as rejected diagnostic
evidence because its manifest contained a 39-character truncated identity.
That historical artifact was not overwritten or reused.

## Verification and build

Focused readiness, startup/composition, provider-profile, and credential tests
passed. The full suite passed with 248 suites, 3,298 tests, 0 failures, 0
errors, and 17 skipped; JUnit hostnames were `parker`.

The build identity was derived mechanically from `git rev-parse --verify
854910c^{commit}` and supplied through `PARKER_BUILD_COMMIT`. The candidate JAR
manifest contains the identical 40-character `Parker-Source-Commit`.

JAR SHA-256: `a90ef15cca0dce392e9bae156df1214b83ad663e9c9fe3b7a0ad0699d2dd4831`

Image ID/digest: `sha256:9268d5d1685f6760cc6daea7fb40000c437584ec2721156a40143266530a3ec7`

OCI manifest: `sha256:6f0fd3e95f01357a41e2b5218444b574c84526b6d4a9a2a5bc5368665a170d68`

OCI config: `sha256:d7144d5c4b346ad382ddd95c02e40bd5df583bb466f37b87ea8b538c9a4d098e`

Static inspection confirmed `RuntimeReadinessDiagnostic`, shared readiness
evaluation, ParkerRuntime routing, and ordinary-execution readiness reporting
inside the candidate. The non-egress diagnostic reported every predicate PASS,
`ordinaryExecutionReady=true`, `overallReady=true`, and no reasons.

## Governance identities

Capability: `ordinary-external-request-region-transcription-v8`

Capability digest: `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`

Provider profile: `openai-fidelity-first-transcription-v1`, state `ACCEPTED`,
SHA-256 `3038538d53b98595631c76325062688b40c449d512bb94cae17be2e7f0d6e956`.

## Preservation and acceptance

The candidate was preserved before acceptance at:

`/mnt/parker-data/parker/replacement-candidates/oi11r3l-replacement-854910c-20260901.tar`

Archive SHA-256: `4bdd245e2bc9cdeba600e8c204ad70cc6aeda8076483c0bb65ef881feb5f276c`

Archive size: 867,029,504 bytes; mode 600; owner `steve:steve`.

Provisional evidence SHA-256:
`cbe4a122d09880802bfe89d104c37c626affe0c08a43c9d101c2e2eb44c6b1bc`.

Steven explicitly accepted the exact source, executable, image, OCI,
capability, provider-profile, and archive identities. The durable acceptance
record is:

`/mnt/parker-data/parker/replacement-candidates/oi11r3l-artifact-acceptance-9268d5d1685f-v1.json`

Acceptance-record SHA-256:
`64031c616057053bd1971c9b23a3063cffbe8e3e752134aeafeec41d0aac038c`.

## Deployment binding and integrity

The deployment-local override was minimally advanced to bind
`PARKER_DEPLOYED_IMMUTABLE_IMAGE_ID` to the accepted image and both source and
production commit fields to the exact 40-character source identity. A
rollback preimage was preserved at:

`/home/steve/.config/parker/docker-compose.fa-a1r.yml.oi11r3l-preimage`

Preimage SHA-256:
`0fcf5e6bff80dec3582b1bcf7895456f00d80a200a7250ee117bbd261407fd25`.

Post-change override SHA-256:
`8dc3c540bfccd7ec739bf00fbb53843f58b409d9168f3b782a6699fe876817f2`.

Static Compose rendering passed. No service was started, recreated, or
restarted.

## Production and egress integrity

Production remains stopped (container
`afcbbcd2140272304287279eae53b4fbb2f69bdabbefaf743feeb8754347dffe`, restart
count 8). Store counts remain `4 / 2 / 1 / 21 / 19 / 6 / 5`. No evidence,
provider-state, transcription, memory, knowledge, or provenance mutation
occurred. OpenAI calls: 0; Claude calls: 0; external provider calls: 0;
retries: 0.

## Readiness

The corrected artifact is accepted and preserved, but not deployed. The next
step is to retry OI11R3 using this exact accepted artifact and its bound
identities; no other rebuild or artifact substitution is authorized.
