# FA.9.4P-A1E-R6.10C1 — Typed capability-acceptance evidence implementation

## Scope and stop finding

R6.10D stopped after safely deploying R6.10C because `OrdinaryRegionCapabilityAcceptanceRecord`
represented acceptance evidence as an unlabelled `List<String>` and validated only presence and
SHA-256 syntax. `FileSystemOrdinaryRegionCapabilityAcceptanceStore.admit` was the only creation
path; no governed coordinator or authenticated administration boundary reconstructed the accepted
R6.9 chain. Production remained unpromoted.

R6.10C1 is a bounded implementation correction. It performs no deployment, production restart,
provider call, owner authorization, attempt start, derivative creation, or production acceptance.

## Typed evidence and exact validation

`RegionTranscriptionCapabilityAcceptanceEvidenceV1` contains four explicit role-bearing values:

- `R6_9_LIVE_PROVIDER_RESULT`: authority `authority-fa-9.4p-a1e-r6.8c1`, execution
  `execution-fa-9.4p-a1e-r6.8c1`, request digest, response identity, provider-state identity, raw,
  structured and complete-record digests, and the immutable assessment digest.
- `R6_9A_FORENSIC_ANALYSIS`: commit `07b5b07769e57f5e066b680599143a2de6082ad8` and report SHA-256
  `6e3260effc4862bc4eed0a0b2e05aac5a118524f85ae16e4afec1d24e8d93200`.
- `R6_9B_POINT_ANCHOR_SEMANTICS`: commit `ac6c49115a756d4b5b88ff69e1789b36f54b03e0`, report SHA-256
  `2295ea42f7b447806e66d3aac2bec9a1a7875040c0d23aa16cee6d47d013a2dc`, and wire v5.
- `R6_9C_FIDELITY_REVIEW`: commit `b9a964a98edf9803791a046d189a61c2353a44a3`, report SHA-256
  `3245780663d86c486bf8b42d4dded31beb0cf8b00cb6641f8d596ce077fc6d4e`, classification exactly
  `PASS_FIDELITY`, 24 reviewed, 24 exact and zero non-exact discrepancies.

Construction requires the complete four-role set, unique and correctly assigned. Every governed
commit, digest, identity, count, classification and wire binding is compared to its semantic
constant, not merely checked for hash shape. The capability identity additionally binds PDF-only,
32 regions, the 16,777,216-byte aggregate limit and no batching alongside the existing exact
provider/model/adapter/profile/wire/schema/instruction/processing/renderer fields.

## Canonical record and compatibility

Typed records use domain `parker.region-capability-acceptance.v2`. Deterministic ordered framing
includes every typed evidence field, capability digest, exact promoting build commit, accepting
owner and timestamp. Records persist with suffix `.region-capability-acceptance-v2` and retain the
existing checksum, create-once file and directory durability semantics.

The evaluator scans only typed v2 records. A legacy untyped v1 file is preserved but is never
eligible to accept ordinary region-v5. Corrupt, ambiguous, unreadable, wrong-capability and
wrong-build typed state continues to fail closed. Fresh durable lookup remains uncached, so a newly
created valid typed record is observable without restart.

## Governed promotion coordinator and administration

`DurableOrdinaryRegionR69EvidenceLoader` reads the exact accepted provider-state record through
`FileSystemRegionProviderStateStore`, including its newly exposed verified assessment digest, and
extracts the durably retained provider response identity. `OrdinaryRegionCapabilityAcceptanceCoordinator`
accepts only the exact capability ID and one canonical 40-character build commit, requires equality
with the runtime embedded `Parker-Source-Commit`, reconstructs and validates the complete chain,
then uses the typed create-once store. Exact replay returns the existing record.

`ParkerRuntime.createOrdinaryRegionCapabilityAcceptanceAsOwner` is the narrow runtime boundary.
`POST /owner/admin/region-capability-acceptance` exposes it through the existing constant-time bearer
authentication. The bounded JSON body permits exactly `capabilityId` and `promotingBuildCommit`;
caller-supplied evidence, acceptance payloads, owner identities and extra fields are rejected. The
endpoint has no path to authorization, reservation, attempt start, execution, derivatives or
provider transport.

## Verification and production preservation

Focused tests cover exact typed creation, role duplication/misassignment, wrong governed commits
and report digests, non-PASS classification, incorrect counts, incomplete live identity, wrong
capability/build, create-once replay, dynamic reload, legacy-v1 ineligibility, authenticated admin
routing and rejection of caller-supplied evidence. Existing ordinary-ingestion E2E, concurrency,
recovery, bounds, ordering, provider-state, v5 and historical codec tests remain exercised by the
full Ubuntu suite. All transport used by ordinary E2E remains fake.

Production stayed on container `40295a170499b168fc9c5e4fd36532c88e639e6d3c17572d93a70bd923e26765`
and image `sha256:d7bbe9404999110d61b709942b58df329f66d4680b54c4b1f4c66a19c52a0a5b`,
restart count zero. Acceptance and owner-authorization stores remained empty. The R6.9 attempt,
provider-state and assessment fingerprints remained unchanged. OpenAI and Claude calls were zero.

## Resumed R6.10D

R6.10D must build and deploy the exact C1 commit, verify its embedded commit, prove
`CAPABILITY_NOT_ACCEPTED`, invoke the authenticated governed endpoint once for that same build,
and prove dynamic `ACCEPTED` without restart or provider traffic. The old R6.10C image cannot be
promoted by a C1 typed record.
