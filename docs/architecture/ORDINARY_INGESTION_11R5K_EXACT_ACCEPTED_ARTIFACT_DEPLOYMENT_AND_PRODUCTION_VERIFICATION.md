# OI11R5K — Exact Accepted Artifact Deployment and Production Verification

## Disposition

The exact owner-accepted OI11R5J artifact was deployed through Parker's immutable Compose path and passed identity, health, history, store-integrity and bounded stability verification. This unit created deployment authority only. It did not create implementation-bound V8 execution authority and stops before any capability promotion, Deed preparation or provider activity.

## Repository and owner authority

The starting repository was branch `main`, HEAD/upstream `3a8eb5e6ee4a3d6754c679280ec033509b9d2a05`, with a clean worktree and passing `git diff --check`. Steven had visually reviewed the five RGB/grayscale pairs in `/home/steve/parker-owner-review/oi11r5j/` (manifest SHA-256 `25551159e55e948e1b0c6f36742397ce077f30daba4dc2be0bd70735e023b29e`) and explicitly accepted the exact candidate for deployment only.

The accepted identities are:

| Identity | Exact value |
|---|---|
| Source | `fe13047df0dd5f155d6a6921acf7bc85541af26f` |
| JAR SHA-256 | `fd259f70b58843a2bee8955edb98a767b89b0307643d8fe57eb155f22448fe89` |
| OCI image/index | `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f` |
| Linux/amd64 manifest | `sha256:1a0c0fee5dba98c2ff66ccc4718b327b3a561541bd9b1f04a92088d4f260eb53` |
| OCI config | `sha256:1ae20b2d046996ac2a07abc8f624c48ef2f544171080d84a4db5e8d28abc5d6f` |
| Profile | `full-page-achromatic-png-preparation-v1`, version 1 |

## Candidate and archive verification

Before deployment, `/mnt/parker-data/parker/replacement-candidates/oi11r5j-full-page-achromatic-fe13047-20260901.tar` was rechecked as SHA-256 `db3c74049c6868f8bb8aa85223e0e2179b19c28526799ede4bf4e261d2c8cd94`, exactly 908,298,752 bytes, mode 600, owner/group `steve:steve`. Its OCI index resolves to the accepted image/index, whose linux/amd64 descriptor resolves to manifest `1a0c0fee…`; that manifest resolves to config `1ae20b2d…`. The config and local image bind OCI revision `fe13047df0dd5f155d6a6921acf7bc85541af26f`. The candidate JAR itself rehashed to the accepted `fd259f70…` value. No image was rebuilt or pulled.

## Artifact acceptance record

Parker's established immutable JSON mechanism was used. The deployment-only record is `/mnt/parker-data/parker/replacement-candidates/oi11r5k-artifact-acceptance-dea81e5d-v1.json`, SHA-256 `7d2f2e4e5f49068e28ac2f1bf04fa63f477a4bb3cd62f4b7cc9ed51da2c3b8cb`, 1,391 bytes, mode 600, owner/group `steve:steve`. Event identity is `oi11r5k-owner-artifact-acceptance-dea81e5d-v1`; recorded acceptance time is `2026-09-01T13:22:16Z`.

The record binds source, JAR, index, platform manifest, config, archive hash/size and preparation profile. It expressly excludes implementation-bound V8 authority, capability promotion, evidence-specific authority, provider authority/execution and egress authority.

## Pre-deployment production baseline

Production was exactly the expected container `34e0637eaa2b32c3e4c43b3e29c274b3da2d7a59c641cc5a2e25143465ba36b4`, source `9e2a900fee388ebf4787817c24f34a63190b3f0d`, image/index `sha256:c121ea0d5c55c32a8cec9b38eade8ecb5f2d72f5331a8ed761b10fb8cfef0ae4`, running since `2026-09-01T11:29:24.478081554Z`, restart count 0. The non-egress readiness diagnostic returned all predicates true, `ordinaryExecutionReady=true`, `overallReady=true`, and no reasons. Nine capability files were present: six legacy and three V8. The accepted V8 record for `9e2a900…` remained present.

## Compose binding and deployment

The override rollback preimage is `/home/steve/.config/parker/docker-compose.fa-a1r.yml.oi11r5k-preimage`, SHA-256 `f4f05eece9b7f517ad37cbb175ccbc6cd5e63c2567f0b82429fd2d1e6f539aad`. The exact accepted binding has SHA-256 `5257986a23772098b8f3911b42b292d85a37c573385d31233a95d65a6654c6ad`.

The final rendered Compose configuration is `/tmp/oi11r5k-compose-render.yml`, SHA-256 `5fb6a8bed98a367f8fee6b6a0209e643551f0ab5b3dc6fc51dd2ff04be9026b0`. It resolves the service image to `parker-oi11r5j-fe13047@sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f` and all three source/deployment identity environment values to the exact accepted artifact. A preliminary auxiliary `config --quiet` invocation without `PARKER_BUILD_COMMIT` correctly failed closed; it caused no mutation. Validation with the required exact commit passed.

Deployment used only:

```text
PARKER_BUILD_COMMIT=fe13047df0dd5f155d6a6921acf7bc85541af26f docker compose -f /home/steve/parker-platform/docker-compose.yml -f /home/steve/.config/parker/docker-compose.openai-enablement.yml -f /home/steve/.config/parker/docker-compose.fa-a1r.yml up -d --no-build --pull never --no-deps --force-recreate parker
```

No Compose source file was changed and no substitute tag, build or pull was used.

## Running production identity and readiness

Production now runs container `eb5a5bcf74ec26fe09bbb59b9a10a9eb0fd92d02a92cdb5812a18c35c3a4dd0f`, image/index `sha256:dea81e5d8d2a339cd0da407716ac532ca58320b81f8f932d68775cf8b8d0535f`, embedded/configured source `fe13047df0dd5f155d6a6921acf7bc85541af26f`, and JAR SHA-256 `fd259f70b58843a2bee8955edb98a767b89b0307643d8fe57eb155f22448fe89`. It started at `2026-09-01T13:23:54.513307926Z`; status remained running and restart count remained 0.

Startup logged `Runtime starting`, `Runtime started`, then the owner HTTP listener. Repeated non-egress readiness checks returned every predicate true, including store/profile/credential/build-identity checks, `ordinaryExecutionReady=true`, `overallReady=true`, and empty reasons. This diagnostic reports runtime/provider-profile infrastructure health; it is not the implementation-bound V8 evaluator.

## V8 identity and authority state

The artifact retains capability `ordinary-external-request-region-transcription-v8` and digest `c0479979720455d2de3fc9861eeb5dee323a4770bdb15f807af611ad426f9ec0`, as semantically verified for this exact JAR in OI11R5J and confirmed in all three readable production V8 records.

Production contains three historical V8 acceptances bound to C304, D335 and `9e2a900…`; it contains zero acceptances bound to `fe13047df0dd5f155d6a6921acf7bc85541af26f`. Therefore the V8 evaluator's actual state for the newly deployed implementation is `CAPABILITY_NOT_ACCEPTED`, and V8 execution fails closed. R5K did not invoke the promotion route or create/correct authority.

## Historical compatibility and R5F preservation

All six legacy and three V8 capability records remained readable and byte-stable. The existing evidence/manifests, OI11R4F failed provider-state/assessment, OI11R4K successful synthetic attempt/derivative, authorizations, attempts, provider state, derivative generations/content and region acceptance authority remained mounted and readable. Startup and diagnostics successfully composed the same historical stores. The exact artifact's R5I test evidence remains the applicable codec proof for the new preparation types; no corrected governed production preparation record yet exists to read.

The R5F page-1 geometry and order records remain SHA-256 `8c8d9949b7fa9308381c3be3915e8ab5f78c4c0575cf30315481a4296565fcb4` and `99b594592e18d812da7750e84873071a6ed51604a4a8a17708bbf3cf3ed70e79`. They retain region-set digest `4b8571e618e174adc4e8171bdf0fc1ab512e2a4f164abb11925bef93437cc73f` and disposition `SOURCE_ORDER_REVIEW_REQUIRED`. No owner resolution, substitution or rewrite exists.

The registered Deed remains evidence `evidence-a51887d1-1a40-4b68-b340-c60e02e9a8d9`, source digest `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`, with manifest SHA-256 `ec98834d794713ba2842506a9cabb6f200a0c0b19876f6724fc6da17e40c5e34`. It was not rendered, prepared, shaped, authorized or transmitted. The production corrected-preparation count remains zero.

## Store and provider accounting

| Store | Before | After |
|---|---:|---:|
| evidence | 29 | 29 |
| evidence source manifests | 29 | 29 |
| capability acceptances | 9 | 9 |
| owner authorizations | 11 | 11 |
| attempts | 8 | 8 |
| provider state/assessment | 6 | 6 |
| derivative generations | 22 | 22 |
| derivative content | 20 | 20 |
| region acceptance authority | 1 | 1 |
| corrected Deed preparations | 0 | 0 |

The governed runtime-store delta is zero. The only new governed file outside those stores is the explicitly authorized deployment-only artifact-acceptance record. No unexpected file appeared after service recreation, logs contained no provider/attempt/retry activity, and bounded repeated checks showed a stable container, restart count 0, readable stores and readiness PASS.

OpenAI calls: 0. Claude calls: 0. Other external calls: 0. Retries: 0. External egress: 0.

## Stop state

No new implementation-bound capability acceptance, capability promotion, production Deed preparation, provider authorization, provider attempt, transcription or external reasoning occurred. Further progress requires a separate explicit owner decision on V8 production execution authority for implementation `fe13047df0dd5f155d6a6921acf7bc85541af26f`.

UNIT ORDINARY-INGESTION-11R5K COMPLETE — THE EXACT OWNER-ACCEPTED OI11R5J ARTIFACT HAS BEEN DEPLOYED TO PRODUCTION AND VERIFIED FOR EXACT IMAGE AND SOURCE IDENTITY, RUNTIME READINESS, HISTORICAL COMPATIBILITY, STORE INTEGRITY AND STABILITY. THE V8 CAPABILITY IDENTITY AND DIGEST REMAIN UNCHANGED, BUT NO NEW IMPLEMENTATION-BOUND V8 PRODUCTION EXECUTION AUTHORITY HAS BEEN CREATED. THE REGISTERED DEED HAS NOT BEEN PREPARED, AUTHORIZED, TRANSCRIBED OR TRANSMITTED. NO PROVIDER CALL, RETRY OR EXTERNAL EGRESS OCCURRED. FURTHER PROGRESS REQUIRES A SEPARATE EXPLICIT OWNER DECISION ON IMPLEMENTATION-BOUND V8 PRODUCTION AUTHORITY.
