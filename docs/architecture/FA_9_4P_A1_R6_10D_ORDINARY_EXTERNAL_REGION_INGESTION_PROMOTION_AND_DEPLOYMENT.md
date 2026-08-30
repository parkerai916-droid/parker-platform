# FA.9.4P-A1E-R6.10D — Ordinary external region ingestion promotion and deployment

## Final outcome

R6.10D-R3 deployed the exact R6.10C3 build and created the single governed typed region-v5 capability acceptance from verified preserved R6.9 evidence. The dynamic evaluator changed from `CAPABILITY_NOT_ACCEPTED` to `ACCEPTED` without restart. No evidence authorization, ingestion execution, derivative, provider attempt, OpenAI call, Claude call, or automatic retry occurred. R6.10 is closed.

## Deployment history

### R6.10D initial attempt

The exact C implementation was deployed. Promotion safely stopped because its acceptance mechanism used an insufficiently typed/unverifiable R6.9 evidence chain and had no production acceptance-administration entry point. No provider execution occurred.

### R6.10C1

C1 added the role-validated R6.9 evidence chain, deterministic `parker.region-capability-acceptance.v2` record, exact-build equality, governed acceptance coordinator, and authenticated owner promotion POST.

### R6.10D-R1

The exact C1 build was deployed. Promotion safely stopped because ordinary region-v5 was not registered in the production capability projection and no non-executing production acceptance-status evaluation existed. No promotion or provider execution occurred.

### R6.10C2

C2 added canonical production region-v5 registration, shared dynamic acceptance evaluation, authenticated read-only GET, proposal acceptance status, and exact routing metadata.

### R6.10D-R2

The exact C2 build was deployed and passed pre-promotion checks. Its single authorized POST returned HTTP 409 `R6_9_PROVIDER_EVIDENCE_UNAVAILABLE_OR_CORRUPT`; it was not retried. It created no acceptance or owner authorization and made no provider call.

### R6.10C3

C3 traced all 12 reconstruction stages and collected three defects before implementation: `HISTORICAL_CODEC_COMPATIBILITY_DEFECT`, `AUTHORITY_TO_ATTEMPT_LINKAGE_DEFECT`, and `ATTEMPT_TO_PROVIDER_STATE_LINKAGE_DEFECT`. It proved the preserved evidence intact; retained the historical v4 `provider_response_id=null`, `PROVIDER_RESPONSE_RECEIVED`, and `VALIDATION_MALFORMED_SCHEMA` facts; recovered the exact response ID from the cryptographically verified raw Responses envelope; and bound authority → attempt → provider state exactly. Its exact production-record offline replay passed, the typed evidence chain was complete, temporary acceptance creation was exactly one, all negative tests failed closed, and provider calls were zero.

### Pre-R6.10D-R3 infrastructure maintenance

Docker persistent data was migrated from `/var/lib/docker` to `/mnt/parker-data/docker`, and containerd persistent data from `/var/lib/containerd` to `/mnt/parker-data/containerd`. Parker governed stores remained independently bind-mounted under `/mnt/parker-data/parker`; Ollama remained under `/mnt/parker-data/ollama`. This infrastructure-only migration performed no provider execution, capability promotion, owner evidence authorization, or Parker evidence mutation.

Before D-R3, Docker and containerd were active, Parker was running with HTTP 200 and RestartCount 0 on the unchanged C2 image, and the migrated storage roots were exact. Root capacity was 61 GB total / 30 GB used / 29 GB available (51%); `/mnt/parker-data` was 738 GB total / 34 GB used / 667 GB available (5%).

## R6.10D-R3 source and governance gates

- Starting `HEAD` and upstream: `1694a6a576a8d34a305ab1f3d797b5ab8f8b65d5`.
- Starting worktree: clean.
- C3 report SHA-256: `14d18882aae1d420f795a0c9b8701c04350ad269b449f330ce0a46e0f492573b`.
- C2/C1/C report hashes: `0adf307d…`, `5971ddd1…`, `9d838b65…` (exact).
- B/B1/B2/B3/B4/B5 hashes: `dfa19963…`, `fd1ca786…`, `b72aefab…`, `4b9fedd1…`, `f74c9def…`, `a5c4f990…` (exact).
- Docker data-root: `/mnt/parker-data/docker`.
- containerd persistent root: `/mnt/parker-data/containerd`; runtime state remained `/run/containerd`.
- `/mnt/parker-data`: mounted ext4 from `/dev/sdb1`.
- Credentials: present, never exposed.

## Initial production and preservation baseline

- Container: `17ecdcfa2138ddba5baae9bcb70bdc39c71fc9e5ddfc37e28e35c9c75b3daf1e`.
- Image: `sha256:e94f0ccc9d978ce83f3c71f5ed3afe045af48e8331b6ea9d65cdc4402abbf0cc`.
- Embedded commit: `5daa344503d8702019b3f398b0507988ec585fde`.
- Status/restarts: running / 0.
- HTTP: root 200; unauthenticated owner endpoint 401.
- Mounts: 15 writable / 4 read-only.
- Capability: `CAPABILITY_NOT_ACCEPTED`.
- Counts: attempts 4; provider-state files 2; authorities 1; acceptances 0; authorizations 0.
- Attempt fingerprint: `e6e3a6eba4a2033e58c23a06c39aed95fc1a7c880cd3e568ee0ae01d7806741d`.
- Provider-state fingerprint: `edd6b3fcf357edfcfce0e6825e2b8734462fb2654f6816255d8e6305b080b4c9`.
- Assessment file fingerprint: `62a5b9a266db3cf4702eb5583f61f8a2f23d96e4954371b2750ddf07ebb50ae8`.
- Derivative content/generation governed fingerprints: `521050570f8b6de018603d2e1bcb65ffc3e3d64b0a55e4605dede4999d8050ba` / `55176ee0c863edca19834419c96161b42c90d9aa5f828eccc7ffeee093ff9c25`.
- Independent sorted file-hash snapshots: content `8b82cb415377e9f07645bbd29f838cbcf9185fbb218919b4db53b11e88449990`; generation `4e78867831463adeba0ccb7fb728736458738de2a3057c59dd449a3e10f040a9`.
- Audit fingerprint: `4f585b3d81e51c720a7dd72ff6a79219b701dd2deba231736243297ec406406e`.

Initial byte capacities were root 65,401,753,600 total / 31,580,401,664 used / 30,981,234,688 available, and Parker data 791,515,963,392 total / 35,645,837,312 used / 715,588,136,960 available.

## Exact C3 build and deployment

The clean C3 commit was built with:

```text
PARKER_BUILD_COMMIT=1694a6a576a8d34a305ab1f3d797b5ab8f8b65d5 docker compose -f /home/steve/parker-platform/docker-compose.yml -f /home/steve/.config/parker/docker-compose.openai-enablement.yml -f /home/steve/.config/parker/docker-compose.fa-a1r.yml build parker
```

Build result: success. Immutable image ID: `sha256:e14234a82a41bbe534c83704d5251506edf458cc29b78d303243fd67ded2ce3e`. Embedded `Parker-Source-Commit`: exact C3 `1694a6a576a8d34a305ab1f3d797b5ab8f8b65d5`.

Post-build byte capacities were root 65,401,753,600 total / 31,580,479,488 used / 30,981,156,864 available, and Parker data 791,515,963,392 total / 43,673,075,712 used / 707,560,898,560 available.

Only deployment-local image/source/production commit references were updated. Parker alone was recreated using `up -d --no-deps --no-build --pull=never --force-recreate parker` with the three authoritative Compose files in order.

New production:

- Container: `61439ad811f203b47ddca99588a314e17abe7a7eef261aa1546bfadcd65892ca`.
- Image: `sha256:e14234a82a41bbe534c83704d5251506edf458cc29b78d303243fd67ded2ce3e`.
- Embedded/runtime commit: exact C3.
- Status/restarts: running / 0.
- HTTP: 200 / unauthenticated owner 401.
- Mounts: 15 writable / 4 read-only.
- Startup error/exception matches: 0.
- Automatic acceptance/authorization after deployment: 0 / 0.

Post-deployment byte capacities were root 65,401,753,600 total / 31,580,499,968 used / 30,981,136,384 available, and Parker data 791,515,963,392 total / 43,581,370,368 used / 707,652,603,904 available.

## Pre-promotion evaluation

Authenticated GET returned HTTP 200 and `CAPABILITY_NOT_ACCEPTED`, runtime commit exact C3, and null accepted commit. Routing was exact: OpenAI; `POST /v1/responses`; `gpt-5.6-sol`; reasoning none; store false; detail original; adapter `openai-responses-region-transcription-adapter` 4.0.0; profile `openai-region-anchored-transcription-v2`; wire v5; `application/pdf`; maximum 32 regions; aggregate request maximum 16,777,216 UTF-8 bytes; batching false.

The GET was read-only. Acceptance/authorization/reservation/revocation and new ordinary-execution counts remained zero. Attempt/provider/assessment/derivative/audit fingerprints were unchanged. Provider calls and retries were zero.

## Single governed promotion

Exactly one authenticated POST was issued. Its request contained only `capabilityId` and `promotingBuildCommit`; no historical evidence or owner evidence authorization was caller-supplied.

Result: HTTP 201 `CREATED`.

- Framing/domain: `parker.region-capability-acceptance.v2`.
- File: `1ef37f99850d3367fe39cd94c18262318edf043836bd546dff239131bbd14ce6.region-capability-acceptance-v2`.
- Record ID/identity: `1ef37f99850d3367fe39cd94c18262318edf043836bd546dff239131bbd14ce6`.
- File SHA-256: `58f803531959888dc24397ad62ed6b64d9ed0997e0ca1ef7785c20e3bfe410f3`.
- Canonical payload checksum: `38308ff66d5e6bfbaf6934e01a2468d431371fde6a70ab5eb3269ddbe33f83db`.
- Capability ID: `ordinary-external-region-transcription-v5`.
- Capability digest: `9b404e8dfc4f0ffa3067fcffb00c39e6bd739050f173418de740239a1dc94103`.
- Promoting commit: exact C3.
- Exact-build equality against runtime embedded commit: PASS.

Internally reconstructed live evidence:

- authority ID/record identity: `authority-fa-9.4p-a1e-r6.8c1` / `5497f12e65d6e7a4d795cfec22ee3aa99c40eb00a8fc2e9f76835b5cfb2d23c9`;
- authority → attempt linkage: PASS, execution `execution-fa-9.4p-a1e-r6.8c1`;
- attempt → provider-state linkage: PASS;
- request digest: `1a691388478370add9bae4e920fb1071369efa543057403727b422e9000a3d36`;
- provider-state identity: `31b997b2a5208ab120fa483778bca9f1ec270c994b7937b3e5dd765db2bfabcd`;
- raw SHA: `500863d65c7f9ca69a66b2ffef3ef8a42b7033903cf1b5a5bd774d9f0decd87f`;
- structured SHA: `7031179aa4267fdc12a50a429eef184e4ecfb2efb3ae993b6a5527ecf9f4c476`;
- complete provider-record SHA: `ad2542015546250bfe0640e5c31636bb6401a20d537d95db04248c81883ad135`;
- response ID recovered from verified raw envelope: `resp_0007d6aa81587b3e016a92f716feb087d0ae9e005456676627` (PASS);
- assessment evidential identity: `39fbc01c7cf831ebf5fc0751cfcca73310bc1a1a1846508ff46d64c61bd09da7`, deliberately distinct from file fingerprint `62a5b9…`.

Typed roles were exactly `R6_9_LIVE_PROVIDER_RESULT`, `R6_9A_FORENSIC_ANALYSIS`, `R6_9B_POINT_ANCHOR_SEMANTICS`, and `R6_9C_FIDELITY_REVIEW`. R6.9A bound commit/report `07b5b077…` / `6e3260ef…`; R6.9B bound `ac6c4911…` / `2295ea42…` / wire 5; R6.9C bound `b9a964a9…` / `32457806…` / `PASS_FIDELITY` with 24 reviewed, 24 exact, 0 discrepancies.

## Dynamic acceptance and preservation

Restart count immediately before and after promotion was 0. No restart or redeployment followed the POST. The next authenticated GET returned HTTP 200, `ACCEPTED`, exact C3 runtime commit, and exact C3 accepted promoting commit with unchanged routing metadata.

Final durable counts were attempts 4, provider-state files 2, authorities 1, capability acceptances exactly 1, and owner authorizations 0. Reservations, revocations, and ordinary executions created by D-R3 were all 0. OpenAI calls, Claude calls, automatic retries, new provider attempts, and new provider-state records were all 0.

All historical fingerprints remained byte-for-byte unchanged. The before/after independent derivative snapshots remained content `8b82cb41…` and generation `4e788678…`, confirming the governed derivative fingerprints `521050…` and `55176e…` were preserved. Audit remained `4f585b3d…`.

## Final production and storage health

- Parker: running; restart count 0; startup errors 0.
- Root HTTP 200; unauthenticated owner endpoint 401.
- Exact C3 image and embedded/runtime commit remain deployed.
- Capability: `ACCEPTED`; acceptance records 1; owner authorizations 0.
- Docker data-root: `/mnt/parker-data/docker`.
- containerd persistent root: `/mnt/parker-data/containerd`.
- Root capacity: 65,401,753,600 total / 31,580,512,256 used / 30,981,124,096 available (51%).
- Parker data capacity: 791,515,963,392 total / 43,581,374,464 used / 707,652,599,808 available (6%).

No unexpected production observation occurred. The first migrated-cache build was slower because it populated pinned image dependencies in the accepted Docker data-root; it completed successfully without cleanup or production mutation.

## Closure

R6.10 is closed. Production is ready for the separately authorized first ordinary owner-selected document-ingestion workflow. Capability acceptance alone authorized no evidence and transmitted nothing.
