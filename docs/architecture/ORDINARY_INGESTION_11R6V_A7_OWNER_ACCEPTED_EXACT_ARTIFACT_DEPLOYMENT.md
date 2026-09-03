# OI11R6V-A7 — Owner-Accepted Exact Artifact Deployment

## Verdict

**A — EXACT OWNER-ACCEPTED HUMAN FIDELITY REVIEW ARTIFACT DEPLOYED AND VERIFIED**

## Owner acceptance and repository gate

Steven Francis McTague explicitly accepted deployment of the exact A6 candidate and creation of only the two human-fidelity governed roots. The pre-deployment repository gate passed: branch `main`, HEAD and upstream `641f69540548e7cff0cf0b87513ad0a12a0dca43`, and a clean worktree. The A6 report was present. The frozen R6T Scope Lock SHA-256 was `f90c9fc654136ea5e92723a1704ad58108ab0f8a9e73a401e8039d3210e4cd2a`; the frozen R6V Implementation Plan SHA-256 was `86f7b27095a6b80b2618556797a877a21b097f76c3c9ba2d91e235b90c395d1d`.

## Accepted candidate identity

- Source commit: `01fd54237227daff7d0b83064825dd004c9fa1f6`
- Runtime JAR SHA-256: `dc04f7c3498607f35b721348087389f7b1c15e9064ed8a98c5e11c765b2b981c`
- Docker image ID and local image/manifest digest: `sha256:51eff5a7060b478ec66b9ad6e42b56b4ec920d142a984b6e5e7e13dce56f89f5`
- OCI config digest: `sha256:5dc4085de9105b471d9bd14e0295e62fa183b047cbb7ac4601281ffd0b83b6ea`

Docker inspection proved the local candidate ID and `org.opencontainers.image.revision` label exact. The candidate JAR previously extracted by A6 at `/tmp/oi11r6v-a6-candidate.jar` re-hashed to the accepted value. No rebuild, pull, source change, or artifact substitution occurred.

## Pre-deployment production and rollback

Production was container `7d51a0c2b3c499cee97818c04c8599351cc03c6b515e5ff0358eaa95dfef62fc`, image `sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, source `d45efbee348b842340616a6a73831ef130086d90`, and runtime JAR SHA-256 `71f154b230a5ce318915f7fdc66b24ad11393c0112e5f76a1a9c289255c3815a`. It was running with restart count zero and the established `Runtime started` readiness state.

The rollback identity was fixed to that exact image. The established rollback procedure is the same four-file Compose invocation used below, with a temporary final override pinning `parker-oi11r6p-readback-compatible-d45efbee@sha256:55b4f29b4e8f30b80528fda075c7936a968ae98a0b6b54e55b536e9fb9d9ac9c`, its three historical identity environment values, and `up -d --no-build --pull never --no-deps --force-recreate parker`. Rollback was not required.

## Governed-root creation

The two roots were absent before mutation. A non-interactive host `sudo install` attempt was rejected before mutation because sudo required a terminal. The successful bounded mechanism used a root user in an isolated, network-disabled container with only `/mnt/parker-data/parker` mounted, asserted both targets absent and non-symlink, then ran `mkdir`, `chown 999:1001`, and `chmod 2775` on exactly:

- `/mnt/parker-data/parker/human-fidelity-reviews`
- `/mnt/parker-data/parker/human-fidelity-review-audit`

Both are real directories on the `/mnt/parker-data` ext4 filesystem, owned UID `999`, group `parker-store-writers`/GID `1001`, and mode `2775`. Runtime UID 999 read/write/traverse checks passed. Runtime startup applied A2's owner-only root initialization mode; the two root modes were subsequently restored to the explicitly owner-authorized `2775` without changing their A2-created internal structure. Final root ownership and modes are exact.

## Exact deployment

The established production Compose stack was used with its base file, fidelity/ordinary-region overlay, OpenAI enablement overlay, and a temporary A7 override. The merged configuration was inspected before deployment and resolved the image to `parker-platform@sha256:51eff5a7060b478ec66b9ad6e42b56b4ec920d142a984b6e5e7e13dce56f89f5`, all three deployed/source/production identity variables to `01fd54237227daff7d0b83064825dd004c9fa1f6` or the accepted digest, and the two exact host mounts.

Deployment used:

```text
PARKER_BUILD_COMMIT=01fd54237227daff7d0b83064825dd004c9fa1f6 docker compose \
  -f docker-compose.yml \
  -f /home/steve/.config/parker/docker-compose.fa-a1r.yml \
  -f /home/steve/.config/parker/docker-compose.openai-enablement.yml \
  -f /tmp/oi11r6v-a7-exact-image.yml \
  up -d --no-build --pull never --no-deps --force-recreate parker
```

Only `parker-runtime` was recreated.

## Running production verification

- Container: `ccf93adcaf7b37e12eb5d8f93c7419d588d713c03881420b49021e5dd8e1b707`
- Image: `sha256:51eff5a7060b478ec66b9ad6e42b56b4ec920d142a984b6e5e7e13dce56f89f5`
- Source label: `01fd54237227daff7d0b83064825dd004c9fa1f6`
- Runtime JAR SHA-256: `dc04f7c3498607f35b721348087389f7b1c15e9064ed8a98c5e11c765b2b981c`
- Status: running
- Readiness: PASS (`Runtime starting` followed by `Runtime started`; owner HTTP listener started normally)
- Restart count: `0`

Both authorized host roots are mounted read-write at `/data/human-fidelity-reviews` and `/data/human-fidelity-review-audit`; runtime access checks passed. A5 composition inspection confirms the configured roots construct `FileSystemHumanFidelityReviewStorage` and its governance audit, register the active `document-ingestion.human-fidelity-review-recording` purpose, compose exact-target registration and owner-bound permission evaluation, and construct `DefaultGovernedHumanFidelityReviewRecordingService`. No public human-review recording route was introduced. No review was written to demonstrate composition.

## New-store baseline

The review root contains only A2's non-semantic empty `.prepared/` and `.tmp/` directories. The audit root contains only its non-semantic empty `.tmp/` directory. Both roots contain zero regular files, therefore zero canonical human-fidelity review records and zero review audit facts. The real R6S review was not recorded and no production R6 review authorization was created.

## Historical R6 preservation

Pre/post aggregate file counts and aggregate hashes were identical:

| Store | Files | Aggregate SHA-256 |
|---|---:|---|
| evidence | 29 | `e5d29f86bc047774082d0beb70f62b81d2b344b8666edfeb1b8d481f4fe27d85` |
| evidence-source-manifests | 29 | `ec2a7dc1aad1efc9bac3763930564da11a0d5674140378e77d4f108228d76559` |
| corrected-preparations | 6 | `0701859977ca979f1dfc64f605e550ee1e963104e445d17c0e361fb5b06b5b3d` |
| capability acceptances | 14 | `14eeeb9ceac5361c394c58aeae66ee9b3d6b908717ce06e416c113e0aa97b950` |
| owner authorizations | 14 | `b9b30ccee935874b8c1ccf397b88dd4b166e87d350a5b1cf42301af43f42c6f6` |
| provider attempts | 10 | `798ed0fd9e4eff2b19085c33f94cb5a495fae9cb4c849659e35b7f1915c8e12f` |
| provider state | 8 | `be1b33109ed42420d260e94c884af0c23a557bda201285feba7c06edc75845d6` |
| derivative generations | 23 | `801bdfd3c4b4801ac1981cb48d90ccd1721fdd782d0d54326253de62ecb9b19f` |
| derivative content | 21 | `80eee2bb97f2532b0a1b22e7fbfc6e59c8b8a3d15e98ad24b7969917e5f27435` |
| document-ingestion audit | 1 | `3ff333aa80da829ad3454c1d35136a2f24e0997391c1da4a5ef9cc9604429359` |

The exact evidence file remains SHA-256 `5d73e6e55d3491e94aa9d6c02a0735572f9840fe8185a71546dba9f2258e237e`. Generation `region-f0df253d73500fef1dd5bbca186632c6be7f0a94faf10310e07cccb8fb673bc6` remains the single immutable R6 generation with generation SHA-256 `9fb18b02db5ac55e5d446cd48ebc619de929c4596f94d2a11fba1a07da71af14` and content SHA-256 `18a6ed08a4729350027d3140dc0f07dd49d32c04aa45f9e3e9558df5d007c4eb`. Preparation `85054cc742813d9b05339d07bce77d8665210b7c6e851fe9470b68a33c9bed8f`, execution `ordinary-exec-3c2bf685-d6c2-44e0-acf8-0224d92fd976`, provider state `2b1fbe06ebee0b7a3fdb618159c6987fa713976d7bfd2732b9048b50f11df3a7`, raw-response digest `4706c24b8b0b83675a8ded1165f316229fa61a92bff4d8fe0a16c1d7d50cfb4a`, response ID `resp_04aa0adc3e021174016a980c0c891487d09764395f58adef7b`, model `gpt-5.6-sol`, and historical budget (maximum one, consumed one, retries zero) remain unchanged.

## Provider and scope accounting

OpenAI calls: `0`; Claude calls: `0`; other provider calls: `0`; retries: `0`; external evidence egress: `0`. No continuation, retranscription, external reasoning, review recording, correction, projector, eligibility, retrieval integration, Gap #54, or Sequence B work occurred.

Remaining Sequence A work is separately authorized canonical R6S review recording, the deferred effective-review/conflict projection, purpose-specific source-confirmed eligibility, retrieval/presentation integration, production verification of those semantics, and R6 closure.
